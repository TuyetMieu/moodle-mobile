const HOUR = 60 * 60 * 1000
const DAY = 24 * HOUR
const RECENT_DONE_WINDOW = 14 * DAY

export const CATEGORIES = {
  ATTENDANCE: { label: "Attendance", weight: 10 },
  MIDTERM_LABS: { label: "Mid-term & labs", weight: 30 },
  FINAL_PROJECT: { label: "Final project", weight: 60 },
}

export class HttpError extends Error {
  constructor(status, code, message, headers = {}) {
    super(message)
    this.status = status
    this.code = code
    this.headers = headers
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

/**
 * Điểm trung bình có trọng số của các bài ĐÃ CHẤM: Σ(điểm × % đóng góp) / Σ(% đóng góp).
 * Chưa có bài nào được chấm → null.
 */
function weightedAverage(assignments) {
  let totalWeight = 0
  let totalPoints = 0
  for (const a of assignments) {
    if (a.score == null) continue // chưa chấm thì bỏ qua
    totalWeight += a.contribution
    totalPoints += a.score * a.contribution
  }
  if (totalWeight === 0) return null
  return round1(totalPoints / totalWeight)
}

/** Điểm tích luỹ của môn. */
function gradeOf(assignments) {
  return weightedAverage(assignments)
}

/** Điểm và trạng thái chấm của từng thành phần: Attendance, Mid-term & labs, Final project. */
function gradeComponents(assignments) {
  const result = []
  for (const key of Object.keys(CATEGORIES)) {
    const category = CATEGORIES[key]
    let gradedWeight = 0 // tổng % của các bài đã chấm
    let publishedWeight = 0 // tổng % của mọi bài thuộc thành phần này
    const items = []
    for (const a of assignments) {
      if (a.category !== key) continue
      items.push(a)
      publishedWeight += a.contribution
      if (a.score != null) gradedWeight += a.contribution
    }

    let status = "IN_PROGRESS"
    if (gradedWeight === 0) status = "NOT_GRADED"
    else if (gradedWeight >= category.weight) status = "GRADED"

    result.push({
      key: key,
      label: category.label,
      weight: category.weight,
      score: weightedAverage(items),
      status: status,
      gradedWeight: gradedWeight,
      publishedWeight: publishedWeight,
    })
  }
  return result
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
  const summary = learningSummary(db, user.id, now)

  // Điểm TB học kỳ = TB điểm các môn, có trọng số theo tín chỉ (bỏ môn chưa có điểm).
  let enrolledCredits = 0
  let gradedCredits = 0
  let gradePoints = 0
  for (const c of courses) {
    enrolledCredits += c.credits
    if (c.grade != null) {
      gradedCredits += c.credits
      gradePoints += c.grade * c.credits
    }
  }
  return {
    label: semester.label,
    academicYear: semester.academicYear,
    startsAt: semester.startsAt,
    endsAt: semester.endsAt,
    creditsEarned: user.credits_earned,
    creditsMilestone: user.credits_milestone,
    enrolledCredits: enrolledCredits,
    coursesCount: courses.length,
    averageGrade: gradedCredits === 0 ? null : round1(gradePoints / gradedCredits),
    completion: { done: summary.done, total: summary.total, progress: summary.progress },
  }
}

/** Một ô CSV: nếu có dấu phẩy, dấu " hoặc xuống dòng thì bọc trong "..." và nhân đôi dấu ". */
function csvCell(value) {
  const text = value == null ? "" : String(value)
  if (text.includes(",") || text.includes('"') || text.includes("\n")) {
    return '"' + text.replaceAll('"', '""') + '"'
  }
  return text
}

export function gradeReportCsv(db, user, now) {
  const semester = currentSemester(db)
  const rows = []
  rows.push(["Student", "Student ID", "Semester", "Subject", "Course code", "Credits", "Completed tasks", "Total tasks", "Accumulated grade"])
  for (const c of listCourses(db, user.id, now)) {
    const grade = c.grade == null ? "" : c.grade
    rows.push([user.full_name, user.student_id, semester.label + " " + semester.academicYear,
      c.title, c.code, c.credits, c.done, c.total, grade])
  }

  // Mỗi dòng: các ô nối bằng dấu phẩy. Dòng cách nhau bằng \r\n (chuẩn CSV, Excel đọc được).
  let csv = "\ufeff" // ký tự BOM ở đầu file để Excel đọc đúng tiếng Việt (UTF-8)
  for (const row of rows) {
    const cells = []
    for (const value of row) cells.push(csvCell(value))
    csv += cells.join(",") + "\r\n"
  }
  return csv
}

/** Một lần gọi trả về đủ dữ liệu cho app khi mở / kéo để làm mới. */
export function bootstrap(db, user, now) {
  // Lấy bài tập một lần rồi chia theo môn, thay vì truy vấn lại cho từng môn.
  const assignments = listAssignments(db, user.id, now)
  return {
    serverTime: now,
    profile: profileOf(db, user),
    semester: semesterOverview(db, user, now),
    summary: learningSummary(db, user.id, now),
    courses: enrolledCourseRows(db, user.id).map((row) => {
      const own = assignments.filter((a) => a.courseId === row.id)
      return {
        ...toCourse(row, own),
        gradeComponents: gradeComponents(own),
        announcements: listAnnouncements(db, row.id),
        resources: listResources(db, row.id),
      }
    }),
    assignments,
    timetable: timetable(db, user.id),
    notifications: listNotifications(db, user.id),
  }
}
