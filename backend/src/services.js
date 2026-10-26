const HOUR = 60 * 60 * 1000
const DAY = 24 * HOUR
const RECENT_DONE_WINDOW = 14 * DAY

export const CATEGORIES = {
  ATTENDANCE: { label: "Attendance", weight: 10 },
  MIDTERM_LABS: { label: "Mid-term & labs", weight: 30 },
  FINAL_PROJECT: { label: "Final project", weight: 60 },
}

export class HttpError extends Error {
  constructor(status, code, message) {
    super(message)
    this.status = status
    this.code = code
  }
}

const round1 = (value) => Math.round(value * 10) / 10

/** Cùng quy tắc với app: xong → DONE, quá hạn → OVERDUE, còn ≤ 24h → URGENT. */
export function assignmentStatus(assignment, now) {
  if (assignment.completed) return "DONE"
  if (assignment.dueAt <= now) return "OVERDUE"
  return assignment.dueAt - now <= DAY ? "URGENT" : "UPCOMING"
}

// ---------------------------------------------------------------- profile

export function profileOf(db, user) {
  const semester = currentSemester(db)
  return {
    id: user.id,
    username: user.username,
    fullName: user.full_name,
    shortName: user.short_name,
    initials: user.initials,
    studentId: user.student_id,
    program: user.program,
    cohort: user.cohort,
    academicYear: semester.academicYear,
    semester: semester.label,
  }
}

function currentSemester(db) {
  const row = db.prepare("SELECT * FROM semesters ORDER BY starts_at DESC LIMIT 1").get()
  return { id: row.id, label: row.label, academicYear: row.academic_year, startsAt: row.starts_at, endsAt: row.ends_at }
}

// ------------------------------------------------------------ assignments

function toAssignment(row, now) {
  const category = CATEGORIES[row.category]
  const assignment = {
    id: row.id,
    courseId: row.course_id,
    courseTitle: row.course_title,
    courseShortTitle: row.course_short_title,
    title: row.title,
    category: row.category,
    categoryLabel: category.label,
    categoryWeight: category.weight,
    contribution: row.contribution,
    description: row.description,
    dueAt: row.due_at,
    completed: row.completed_at != null,
    completedAt: row.completed_at ?? null,
    score: row.score ?? null,
    maxScore: 10,
    feedback: row.feedback ?? null,
  }
  assignment.status = assignmentStatus(assignment, now)
  return assignment
}

const ASSIGNMENT_SELECT = `
  SELECT a.*, c.title AS course_title, c.short_title AS course_short_title,
         s.completed_at, s.score, s.feedback
  FROM assignments a
  JOIN courses c ON c.id = a.course_id
  JOIN enrollments e ON e.course_id = a.course_id AND e.user_id = :userId
  LEFT JOIN submissions s ON s.assignment_id = a.id AND s.user_id = :userId
`

export function listAssignments(db, userId, now, { courseId, filter } = {}) {
  const rows = courseId
    ? db.prepare(`${ASSIGNMENT_SELECT} WHERE a.course_id = :courseId ORDER BY a.due_at`).all({ userId, courseId })
    : db.prepare(`${ASSIGNMENT_SELECT} ORDER BY a.due_at`).all({ userId })
  const items = rows.map((row) => toAssignment(row, now))
  if (!filter) return items
  if (filter === "done") return items.filter((a) => a.status === "DONE").sort((a, b) => b.completedAt - a.completedAt)
  if (filter === "urgent") return items.filter((a) => a.status === "URGENT" || a.status === "OVERDUE")
  if (filter === "todo") return items.filter((a) => a.status !== "DONE")
  throw new HttpError(400, "invalid_filter", "filter phải là todo, urgent hoặc done")
}

export function getAssignment(db, userId, id, now) {
  const row = db.prepare(`${ASSIGNMENT_SELECT} WHERE a.id = :id`).get({ userId, id })
  if (!row) throw new HttpError(404, "assignment_not_found", "Không tìm thấy bài tập")
  return toAssignment(row, now)
}

export function markCompleted(db, userId, id, now) {
  const assignment = getAssignment(db, userId, id, now)
  if (!assignment.completed) {
    db.prepare("INSERT INTO submissions (user_id, assignment_id, completed_at) VALUES (?, ?, ?)").run(userId, id, now)
  }
  return getAssignment(db, userId, id, now)
}

export function unmarkCompleted(db, userId, id, now) {
  const assignment = getAssignment(db, userId, id, now)
  if (assignment.score != null) {
    throw new HttpError(409, "already_graded", "Bài đã được chấm điểm, không thể bỏ đánh dấu hoàn thành")
  }
  db.prepare("DELETE FROM submissions WHERE user_id = ? AND assignment_id = ?").run(userId, id)
  return getAssignment(db, userId, id, now)
}

// ---------------------------------------------------------------- courses

/** Điểm tích luỹ = trung bình có trọng số (theo % đóng góp) của các bài đã chấm. */
function gradeOf(assignments) {
  const graded = assignments.filter((a) => a.score != null)
  const weight = graded.reduce((sum, a) => sum + a.contribution, 0)
  if (weight === 0) return null
  return round1(graded.reduce((sum, a) => sum + a.score * a.contribution, 0) / weight)
}

function gradeComponents(assignments) {
  return Object.entries(CATEGORIES).map(([key, category]) => {
    const items = assignments.filter((a) => a.category === key)
    const graded = items.filter((a) => a.score != null)
    const gradedWeight = graded.reduce((sum, a) => sum + a.contribution, 0)
    const publishedWeight = items.reduce((sum, a) => sum + a.contribution, 0)
    const score = gradedWeight > 0 ? round1(graded.reduce((sum, a) => sum + a.score * a.contribution, 0) / gradedWeight) : null
    const status = gradedWeight === 0
      ? "NOT_GRADED"
      : gradedWeight >= category.weight ? "GRADED" : "IN_PROGRESS"
    return { key, label: category.label, weight: category.weight, score, status, gradedWeight, publishedWeight }
  })
}

function toCourse(row, assignments) {
  const done = assignments.filter((a) => a.completed).length
  const total = assignments.length
  return {
    id: row.id,
    title: row.title,
    shortTitle: row.short_title,
    code: row.code,
    teacher: row.teacher,
    teacherInitials: row.teacher_initials,
    image: row.image,
    credits: row.credits,
    tone: row.tone,
    summary: row.summary,
    done,
    total,
    pending: total - done,
    progress: total === 0 ? 0 : Math.round((done / total) * 100),
    grade: gradeOf(assignments),
  }
}

function enrolledCourseRows(db, userId) {
  return db.prepare(`
    SELECT c.* FROM courses c JOIN enrollments e ON e.course_id = c.id
    WHERE e.user_id = ? ORDER BY c.rowid
  `).all(userId)
}

export function listCourses(db, userId, now) {
  const assignments = listAssignments(db, userId, now)
  return enrolledCourseRows(db, userId).map((row) => toCourse(row, assignments.filter((a) => a.courseId === row.id)))
}

export function getCourse(db, userId, id, now) {
  const row = enrolledCourseRows(db, userId).find((c) => c.id === id)
  if (!row) throw new HttpError(404, "course_not_found", "Không tìm thấy môn học hoặc bạn chưa đăng ký môn này")
  const assignments = listAssignments(db, userId, now, { courseId: id })
  return {
    ...toCourse(row, assignments),
    gradeComponents: gradeComponents(assignments),
    sessions: db.prepare("SELECT * FROM class_sessions WHERE course_id = ? ORDER BY weekday, start_time").all(id).map(toSession),
    announcements: listAnnouncements(db, id),
    resources: listResources(db, id),
  }
}

function listResources(db, courseId) {
  return db.prepare("SELECT * FROM course_resources WHERE course_id = ? ORDER BY week DESC, rowid").all(courseId).map((row) => ({
    id: row.id,
    courseId: row.course_id,
    week: row.week,
    title: row.title,
    kind: row.kind,
    fileType: row.file_type,
    sizeKb: row.size_kb,
    publishedAt: row.published_at,
  }))
}

function listAnnouncements(db, courseId) {
  return db.prepare(`
    SELECT a.*, c.teacher, c.teacher_initials FROM announcements a JOIN courses c ON c.id = a.course_id
    WHERE a.course_id = ? ORDER BY a.pinned DESC, a.posted_at DESC
  `).all(courseId).map((row) => ({
    id: row.id,
    courseId: row.course_id,
    kind: row.kind,
    title: row.title,
    body: row.body,
    author: row.teacher,
    authorInitials: row.teacher_initials,
    postedAt: row.posted_at,
    pinned: row.pinned === 1,
  }))
}

// -------------------------------------------------------------- timetable

function toSession(row) {
  return {
    id: row.id,
    courseId: row.course_id,
    weekday: row.weekday,
    start: row.start_time,
    end: row.end_time,
    kind: row.kind,
    room: row.room,
  }
}

export function timetable(db, userId) {
  const semester = currentSemester(db)
  const sessions = db.prepare(`
    SELECT s.* FROM class_sessions s JOIN enrollments e ON e.course_id = s.course_id
    WHERE e.user_id = ? ORDER BY s.weekday, s.start_time
  `).all(userId).map(toSession)
  return { semesterStartsAt: semester.startsAt, semesterEndsAt: semester.endsAt, sessions }
}

// ---------------------------------------------------------- notifications

export function listNotifications(db, userId) {
  const items = db.prepare("SELECT * FROM notifications WHERE user_id = ? ORDER BY created_at DESC").all(userId).map((row) => ({
    id: row.id,
    kind: row.kind,
    title: row.title,
    body: row.body,
    createdAt: row.created_at,
    read: row.read_at != null,
    assignmentId: row.assignment_id ?? null,
    courseId: row.course_id ?? null,
  }))
  return { items, unread: items.filter((n) => !n.read).length }
}

export function markNotificationRead(db, userId, id, now) {
  const result = db.prepare("UPDATE notifications SET read_at = COALESCE(read_at, ?) WHERE id = ? AND user_id = ?").run(now, id, userId)
  if (result.changes === 0) throw new HttpError(404, "notification_not_found", "Không tìm thấy thông báo")
  return listNotifications(db, userId)
}

// ------------------------------------------------------ summary & semester

export function learningSummary(db, userId, now) {
  const assignments = listAssignments(db, userId, now)
  const pending = assignments.filter((a) => a.status !== "DONE")
  const done = assignments.length - pending.length
  return {
    pending: pending.length,
    urgent: pending.filter((a) => a.status === "URGENT" || a.status === "OVERDUE").length,
    recentDone: assignments.filter((a) => a.status === "DONE" && now - a.completedAt <= RECENT_DONE_WINDOW).length,
    done,
    total: assignments.length,
    progress: assignments.length === 0 ? 0 : Math.round((done / assignments.length) * 100),
  }
}

export function semesterOverview(db, user, now) {
  const semester = currentSemester(db)
  const courses = listCourses(db, user.id, now)
  const graded = courses.filter((c) => c.grade != null)
  const gradedCredits = graded.reduce((sum, c) => sum + c.credits, 0)
  const summary = learningSummary(db, user.id, now)
  return {
    label: semester.label,
    academicYear: semester.academicYear,
    startsAt: semester.startsAt,
    endsAt: semester.endsAt,
    creditsEarned: user.credits_earned,
    creditsMilestone: user.credits_milestone,
    enrolledCredits: courses.reduce((sum, c) => sum + c.credits, 0),
    coursesCount: courses.length,
    averageGrade: gradedCredits === 0 ? null : round1(graded.reduce((sum, c) => sum + c.grade * c.credits, 0) / gradedCredits),
    completion: { done: summary.done, total: summary.total, progress: summary.progress },
  }
}

export function gradeReportCsv(db, user, now) {
  const semester = currentSemester(db)
  const escape = (value) => {
    const text = value == null ? "" : String(value)
    return /[",\n]/.test(text) ? `"${text.replace(/"/g, '""')}"` : text
  }
  const rows = [
    ["Student", "Student ID", "Semester", "Subject", "Course code", "Credits", "Completed tasks", "Total tasks", "Accumulated grade"],
    ...listCourses(db, user.id, now).map((c) => [
      user.full_name, user.student_id, `${semester.label} ${semester.academicYear}`,
      c.title, c.code, c.credits, c.done, c.total, c.grade ?? "",
    ]),
  ]
  // BOM để Excel đọc đúng UTF-8.
  return "﻿" + rows.map((row) => row.map(escape).join(",")).join("\r\n") + "\r\n"
}

/** Một lần gọi trả về đủ dữ liệu cho app khi mở / kéo để làm mới. */
export function bootstrap(db, user, now) {
  return {
    serverTime: now,
    profile: profileOf(db, user),
    semester: semesterOverview(db, user, now),
    summary: learningSummary(db, user.id, now),
    courses: listCourses(db, user.id, now).map((course) => ({
      ...course,
      gradeComponents: getCourse(db, user.id, course.id, now).gradeComponents,
      announcements: listAnnouncements(db, course.id),
      resources: listResources(db, course.id),
    })),
    assignments: listAssignments(db, user.id, now),
    timetable: timetable(db, user.id),
    notifications: listNotifications(db, user.id),
  }
}
