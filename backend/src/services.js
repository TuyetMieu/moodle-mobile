import { MAX_SCORE, contributionOf, courseGrade } from "./grading.js"

const HOUR = 60 * 60 * 1000
const DAY = 24 * HOUR
const RECENT_DONE_WINDOW = 14 * DAY

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

export function currentSemester(db) {
  const row = db.prepare("SELECT * FROM semesters ORDER BY starts_at DESC LIMIT 1").get()
  return { id: row.id, label: row.label, academicYear: row.academic_year, startsAt: row.starts_at, endsAt: row.ends_at }
}

export function profileOf(db, user) {
  const semester = currentSemester(db)
  const program = db.prepare("SELECT name FROM programs WHERE code = ?").get(user.program_code)
  return {
    id: user.id,
    username: user.username,
    role: user.role,
    fullName: user.full_name,
    shortName: user.short_name,
    initials: user.initials,
    memberCode: user.member_code,
    programCode: user.program_code,
    programName: program ? program.name : user.program_code,
    cohort: user.cohort,
    academicYear: semester.academicYear,
    semester: semester.label,
  }
}

// ------------------------------------------------------- thành phần điểm

/** Thành phần điểm của các môn: Map courseId → [{ id, name, weight, position }]. */
export function componentsByCourse(db, courseIds) {
  const map = new Map(courseIds.map((id) => [id, []]))
  if (courseIds.length === 0) return map
  const marks = courseIds.map(() => "?").join(",")
  const rows = db.prepare(`SELECT * FROM grade_components WHERE course_id IN (${marks}) ORDER BY position`).all(...courseIds)
  for (const row of rows) {
    map.get(row.course_id).push({ id: row.id, name: row.name, weight: row.weight, position: row.position })
  }
  return map
}

/** Tổng trọng số bài trong từng thành phần (để tính % đóng góp): Map componentId → tổng weight. */
export function componentTotals(db, courseIds) {
  const map = new Map()
  if (courseIds.length === 0) return map
  const marks = courseIds.map(() => "?").join(",")
  const rows = db.prepare(`
    SELECT component_id, SUM(weight) AS total FROM assignments
    WHERE course_id IN (${marks}) AND is_bonus = 0 GROUP BY component_id
  `).all(...courseIds)
  for (const row of rows) map.set(row.component_id, row.total)
  return map
}

// ------------------------------------------------------------ assignments

const ASSIGNMENT_SELECT = `
  SELECT a.*, c.title AS course_title, c.short_title AS course_short_title, c.bonus_max,
         g.name AS component_name, g.weight AS component_weight,
         s.completed_at, s.score, s.feedback, s.graded_by
  FROM assignments a
  JOIN courses c ON c.id = a.course_id
  JOIN enrollments e ON e.course_id = a.course_id AND e.user_id = :userId
  LEFT JOIN grade_components g ON g.id = a.component_id
  LEFT JOIN submissions s ON s.assignment_id = a.id AND s.user_id = :userId
`

function toAssignment(row, now, totals) {
  const isBonus = row.is_bonus === 1
  const assignment = {
    id: row.id,
    courseId: row.course_id,
    courseTitle: row.course_title,
    courseShortTitle: row.course_short_title,
    title: row.title,
    description: row.description,
    componentId: row.component_id ?? null,
    componentName: row.component_name ?? null,
    componentWeight: row.component_weight ?? null,
    isBonus,
    bonusMax: isBonus ? row.bonus_max : null,
    weight: row.weight,
    contribution: contributionOf(
      { isBonus, weight: row.weight },
      row.component_id ? { weight: row.component_weight } : null,
      totals.get(row.component_id) ?? 0,
    ),
    dueAt: row.due_at,
    createdAt: row.created_at,
    updatedAt: row.updated_at,
    completed: row.completed_at != null,
    completedAt: row.completed_at ?? null,
    late: row.completed_at != null && row.completed_at > row.due_at,
    score: row.score ?? null,
    maxScore: MAX_SCORE,
    feedback: row.feedback ?? null,
    gradedBy: row.graded_by ?? null,
  }
  assignment.status = assignmentStatus(assignment, now)
  return assignment
}

function enrolledCourseIds(db, userId) {
  return db.prepare("SELECT course_id FROM enrollments WHERE user_id = ?").all(userId).map((r) => r.course_id)
}

export function listAssignments(db, userId, now, { courseId, filter } = {}) {
  const totals = componentTotals(db, enrolledCourseIds(db, userId))
  const rows = courseId
    ? db.prepare(`${ASSIGNMENT_SELECT} WHERE a.course_id = :courseId ORDER BY a.due_at`).all({ userId, courseId })
    : db.prepare(`${ASSIGNMENT_SELECT} ORDER BY a.due_at`).all({ userId })
  const items = rows.map((row) => toAssignment(row, now, totals))
  if (!filter) return items
  if (filter === "done") return items.filter((a) => a.status === "DONE").sort((a, b) => b.completedAt - a.completedAt)
  if (filter === "urgent") return items.filter((a) => a.status === "URGENT" || a.status === "OVERDUE")
  if (filter === "todo") return items.filter((a) => a.status !== "DONE")
  throw new HttpError(400, "invalid_filter", "filter phải là todo, urgent hoặc done")
}

export function getAssignment(db, userId, id, now) {
  const row = db.prepare(`${ASSIGNMENT_SELECT} WHERE a.id = :id`).get({ userId, id })
  if (!row) throw new HttpError(404, "assignment_not_found", "Không tìm thấy bài tập")
  return toAssignment(row, now, componentTotals(db, [row.course_id]))
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

const COURSE_SELECT = `
  SELECT c.*, t.full_name AS teacher_name, t.initials AS teacher_initials
  FROM courses c JOIN users t ON t.id = c.teacher_id
`

function enrolledCourseRows(db, userId) {
  return db.prepare(`
    ${COURSE_SELECT} JOIN enrollments e ON e.course_id = c.id
    WHERE e.user_id = ? ORDER BY c.rowid
  `).all(userId)
}

/** Môn học kèm tiến độ và điểm của một sinh viên. assignments = các bài của môn này. */
export function toCourse(row, assignments, components) {
  const done = assignments.filter((a) => a.completed).length
  const total = assignments.length
  const result = courseGrade(components, assignments, { bonusEnabled: row.bonus_enabled === 1, bonusMax: row.bonus_max })
  return {
    id: row.id,
    title: row.title,
    shortTitle: row.short_title,
    code: row.code,
    teacher: row.teacher_name,
    teacherInitials: row.teacher_initials,
    image: row.image,
    credits: row.credits,
    tone: row.tone,
    summary: row.summary,
    done,
    total,
    pending: total - done,
    progress: total === 0 ? 0 : Math.round((done / total) * 100),
    maxScore: MAX_SCORE,
    grade: result.grade,
    baseGrade: result.baseGrade,
    bonusEnabled: row.bonus_enabled === 1,
    bonusMax: row.bonus_max,
    bonus: result.bonus,
    gradeComponents: result.components,
  }
}

export function listCourses(db, userId, now) {
  const rows = enrolledCourseRows(db, userId)
  const assignments = listAssignments(db, userId, now)
  const components = componentsByCourse(db, rows.map((r) => r.id))
  return rows.map((row) => toCourse(row, assignments.filter((a) => a.courseId === row.id), components.get(row.id)))
}

export function getCourse(db, userId, id, now) {
  const row = enrolledCourseRows(db, userId).find((c) => c.id === id)
  if (!row) throw new HttpError(404, "course_not_found", "Không tìm thấy môn học hoặc bạn chưa đăng ký môn này")
  const assignments = listAssignments(db, userId, now, { courseId: id })
  return {
    ...toCourse(row, assignments, componentsByCourse(db, [id]).get(id)),
    sessions: db.prepare("SELECT * FROM class_sessions WHERE course_id = ? ORDER BY weekday, start_time").all(id).map(toSession),
    announcements: listAnnouncements(db, id),
    resources: listResources(db, id),
  }
}

export function listResources(db, courseId) {
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

export function listAnnouncements(db, courseId) {
  return db.prepare(`
    SELECT a.*, t.full_name AS teacher_name, t.initials AS teacher_initials
    FROM announcements a JOIN courses c ON c.id = a.course_id JOIN users t ON t.id = c.teacher_id
    WHERE a.course_id = ? ORDER BY a.pinned DESC, a.posted_at DESC
  `).all(courseId).map((row) => ({
    id: row.id,
    courseId: row.course_id,
    kind: row.kind,
    title: row.title,
    body: row.body,
    author: row.teacher_name,
    authorInitials: row.teacher_initials,
    postedAt: row.posted_at,
    pinned: row.pinned === 1,
  }))
}

// -------------------------------------------------------------- timetable

export function toSession(row) {
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
// Hai luồng:
// - PROGRAM: tin/sự kiện dành cho ngành của sinh viên (bulletins.audience = PROGRAM).
// - GENERAL: việc thầy cô làm (điểm mới, bài mới, đổi deadline, tin của lớp) và tin gửi toàn trường (audience = ALL).

function parseData(text) {
  if (!text) return null
  try {
    return JSON.parse(text)
  } catch (error) {
    return null
  }
}

export function listNotifications(db, user) {
  const personal = db.prepare(`
    SELECT n.*, t.full_name AS author FROM notifications n
    LEFT JOIN courses c ON c.id = n.course_id LEFT JOIN users t ON t.id = c.teacher_id
    WHERE n.user_id = ?
  `).all(user.id).map((row) => ({
    id: row.id,
    channel: "GENERAL",
    kind: row.kind,
    title: row.title,
    body: row.body,
    createdAt: row.created_at,
    read: row.read_at != null,
    assignmentId: row.assignment_id ?? null,
    courseId: row.course_id ?? null,
    author: row.author ?? null,
    eventAt: null,
    location: null,
    data: parseData(row.data),
  }))

  const bulletins = db.prepare(`
    SELECT b.*, r.read_at FROM bulletins b
    LEFT JOIN bulletin_reads r ON r.bulletin_id = b.id AND r.user_id = :userId
    WHERE b.audience = 'ALL' OR (b.audience = 'PROGRAM' AND b.program_code = :program)
  `).all({ userId: user.id, program: user.program_code }).map((row) => ({
    id: row.id,
    channel: row.audience === "PROGRAM" ? "PROGRAM" : "GENERAL",
    kind: row.kind,
    title: row.title,
    body: row.body,
    createdAt: row.created_at,
    read: row.read_at != null,
    assignmentId: null,
    courseId: null,
    author: row.author_name,
    eventAt: row.event_at ?? null,
    location: row.location ?? null,
    data: null,
  }))

  const items = [...personal, ...bulletins].sort((a, b) => b.createdAt - a.createdAt)
  const unreadOf = (channel) => items.filter((n) => !n.read && (channel == null || n.channel === channel)).length
  return {
    items,
    unread: unreadOf(null),
    unreadByChannel: { PROGRAM: unreadOf("PROGRAM"), GENERAL: unreadOf("GENERAL") },
  }
}

function visibleBulletin(db, user, id) {
  return db.prepare(`
    SELECT id FROM bulletins WHERE id = ? AND (audience = 'ALL' OR (audience = 'PROGRAM' AND program_code = ?))
  `).get(id, user.program_code)
}

export function markNotificationRead(db, user, id, now) {
  if (visibleBulletin(db, user, id)) {
    db.prepare("INSERT OR IGNORE INTO bulletin_reads (user_id, bulletin_id, read_at) VALUES (?, ?, ?)").run(user.id, id, now)
    return listNotifications(db, user)
  }
  const result = db.prepare("UPDATE notifications SET read_at = COALESCE(read_at, ?) WHERE id = ? AND user_id = ?").run(now, id, user.id)
  if (result.changes === 0) throw new HttpError(404, "notification_not_found", "Không tìm thấy thông báo")
  return listNotifications(db, user)
}

/** Đánh dấu đã đọc tất cả (hoặc chỉ một luồng: PROGRAM / GENERAL). */
export function markAllNotificationsRead(db, user, channel, now) {
  if (channel != null && channel !== "PROGRAM" && channel !== "GENERAL") {
    throw new HttpError(400, "invalid_channel", "channel phải là PROGRAM hoặc GENERAL")
  }
  if (channel !== "PROGRAM") {
    db.prepare("UPDATE notifications SET read_at = ? WHERE user_id = ? AND read_at IS NULL").run(now, user.id)
  }
  const audiences = channel === "PROGRAM" ? ["PROGRAM"] : channel === "GENERAL" ? ["ALL"] : ["PROGRAM", "ALL"]
  for (const audience of audiences) {
    db.prepare(`
      INSERT OR IGNORE INTO bulletin_reads (user_id, bulletin_id, read_at)
      SELECT ?, id, ? FROM bulletins WHERE audience = ? AND (audience = 'ALL' OR program_code = ?)
    `).run(user.id, now, audience, user.program_code)
  }
  return listNotifications(db, user)
}

// ------------------------------------------------------ summary & semester

export function learningSummary(db, userId, now, assignments = listAssignments(db, userId, now)) {
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

export function semesterOverview(db, user, now, courses = listCourses(db, user.id, now), summary = learningSummary(db, user.id, now)) {
  const semester = currentSemester(db)

  // Điểm TB học kỳ = TB điểm các môn (thang 20), có trọng số theo tín chỉ (bỏ môn chưa có điểm).
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
    enrolledCredits,
    coursesCount: courses.length,
    maxScore: MAX_SCORE,
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
  rows.push(["Student", "Student ID", "Semester", "Subject", "Course code", "Credits", "Completed tasks", "Total tasks",
    "Weighted grade (/20)", "Bonus", "Accumulated grade (/20)"])
  for (const c of listCourses(db, user.id, now)) {
    rows.push([user.full_name, user.member_code, semester.label + " " + semester.academicYear,
      c.title, c.code, c.credits, c.done, c.total, c.baseGrade ?? "", c.bonusEnabled ? c.bonus : "", c.grade ?? ""])
  }

  // Mỗi dòng: các ô nối bằng dấu phẩy. Dòng cách nhau bằng \r\n (chuẩn CSV, Excel đọc được).
  let csv = "﻿" // ký tự BOM ở đầu file để Excel đọc đúng tiếng Việt (UTF-8)
  for (const row of rows) {
    const cells = []
    for (const value of row) cells.push(csvCell(value))
    csv += cells.join(",") + "\r\n"
  }
  return csv
}

/** Một lần gọi trả về đủ dữ liệu cho app sinh viên khi mở / kéo để làm mới. */
export function bootstrap(db, user, now) {
  // Lấy bài tập một lần rồi chia theo môn, thay vì truy vấn lại cho từng môn.
  const assignments = listAssignments(db, user.id, now)
  const rows = enrolledCourseRows(db, user.id)
  const components = componentsByCourse(db, rows.map((r) => r.id))
  const courses = rows.map((row) => ({
    ...toCourse(row, assignments.filter((a) => a.courseId === row.id), components.get(row.id)),
    announcements: listAnnouncements(db, row.id),
    resources: listResources(db, row.id),
  }))
  const summary = learningSummary(db, user.id, now, assignments)
  return {
    serverTime: now,
    profile: profileOf(db, user),
    semester: semesterOverview(db, user, now, courses, summary),
    summary,
    courses,
    assignments,
    timetable: timetable(db, user.id),
    notifications: listNotifications(db, user),
  }
}
