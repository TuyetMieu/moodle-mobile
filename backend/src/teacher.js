// Nghiệp vụ của giáo viên: xem lớp mình dạy, cấu hình thang điểm, tạo/sửa bài tập, đăng tin.
// Điểm từng sinh viên do máy chấm (xem grading.js), giáo viên không nhập tay.

import { randomBytes } from "node:crypto"
import { MAX_SCORE, autoGrade, contributionOf, courseGrade } from "./grading.js"
import {
  HttpError, componentTotals, componentsByCourse, currentSemester, listAnnouncements, listResources, profileOf, toSession,
} from "./services.js"

const MAX_COMPONENTS = 8
const MAX_TITLE = 120
const MAX_BODY = 2000

const newId = (prefix) => `${prefix}-${randomBytes(6).toString("hex")}`
const round1 = (value) => Math.round(value * 10) / 10

// ------------------------------------------------------------------ kiểm tra dữ liệu gửi lên

function text(value, field, max, { optional = false } = {}) {
  const result = typeof value === "string" ? value.trim() : ""
  if (result === "" && !optional) throw new HttpError(400, "invalid_" + field, `Thiếu ${field}`)
  if (result.length > max) throw new HttpError(400, "invalid_" + field, `${field} dài quá ${max} ký tự`)
  return result
}

function number(value, field, min, max) {
  if (typeof value !== "number" || !Number.isFinite(value) || value < min || value > max) {
    throw new HttpError(400, "invalid_" + field, `${field} phải là số từ ${min} đến ${max}`)
  }
  return value
}

// ------------------------------------------------------------------ đọc dữ liệu

/** Môn của giáo viên, 404 nếu không có hoặc không phải môn mình dạy. */
function ownCourse(db, teacher, courseId) {
  const row = db.prepare("SELECT * FROM courses WHERE id = ? AND teacher_id = ?").get(courseId, teacher.id)
  if (!row) throw new HttpError(404, "course_not_found", "Không tìm thấy môn học bạn phụ trách")
  return row
}

function ownAssignment(db, teacher, assignmentId) {
  const row = db.prepare(`
    SELECT a.* FROM assignments a JOIN courses c ON c.id = a.course_id WHERE a.id = ? AND c.teacher_id = ?
  `).get(assignmentId, teacher.id)
  if (!row) throw new HttpError(404, "assignment_not_found", "Không tìm thấy bài tập trong môn bạn phụ trách")
  return row
}

function studentsOf(db, courseId) {
  return db.prepare(`
    SELECT u.*, p.name AS program_name FROM users u JOIN enrollments e ON e.user_id = u.id
    JOIN programs p ON p.code = u.program_code
    WHERE e.course_id = ? ORDER BY u.full_name
  `).all(courseId)
}

/** Toàn bộ dữ liệu một môn nhìn từ phía giáo viên. */
function teachingCourse(db, row, now) {
  const components = componentsByCourse(db, [row.id]).get(row.id)
  const totals = componentTotals(db, [row.id])
  const students = studentsOf(db, row.id)
  const assignmentRows = db.prepare("SELECT * FROM assignments WHERE course_id = ? ORDER BY due_at").all(row.id)
  const submissions = db.prepare(`
    SELECT s.* FROM submissions s JOIN assignments a ON a.id = s.assignment_id WHERE a.course_id = ?
  `).all(row.id)

  const assignments = assignmentRows.map((a) => {
    const subs = submissions.filter((s) => s.assignment_id === a.id)
    const graded = subs.filter((s) => s.score != null)
    const component = components.find((c) => c.id === a.component_id)
    return {
      id: a.id,
      title: a.title,
      description: a.description,
      componentId: a.component_id ?? null,
      componentName: component ? component.name : null,
      isBonus: a.is_bonus === 1,
      weight: a.weight,
      contribution: contributionOf({ isBonus: a.is_bonus === 1, weight: a.weight }, component, totals.get(a.component_id) ?? 0),
      dueAt: a.due_at,
      createdAt: a.created_at,
      updatedAt: a.updated_at,
      students: students.length,
      submitted: subs.length,
      graded: graded.length,
      averageScore: graded.length === 0 ? null : round1(graded.reduce((sum, s) => sum + s.score, 0) / graded.length),
    }
  })

  const studentItems = students.map((student) => {
    const own = assignmentRows.map((a) => {
      const sub = submissions.find((s) => s.assignment_id === a.id && s.user_id === student.id)
      return {
        componentId: a.component_id,
        isBonus: a.is_bonus === 1,
        weight: a.weight,
        score: sub ? sub.score : null,
        completed: Boolean(sub),
        overdue: a.due_at <= now,
      }
    })
    const grade = courseGrade(components, own, { bonusEnabled: row.bonus_enabled === 1, bonusMax: row.bonus_max })
    return {
      id: student.id,
      fullName: student.full_name,
      initials: student.initials,
      memberCode: student.member_code,
      programName: student.program_name,
      done: own.filter((a) => a.completed).length,
      total: own.length,
      missing: own.filter((a) => !a.completed && a.overdue).length,
      grade: grade.grade,
      baseGrade: grade.baseGrade,
      bonus: grade.bonus,
    }
  })

  const graded = studentItems.filter((s) => s.grade != null)
  return {
    id: row.id,
    title: row.title,
    shortTitle: row.short_title,
    code: row.code,
    image: row.image,
    tone: row.tone,
    credits: row.credits,
    summary: row.summary,
    maxScore: MAX_SCORE,
    bonusEnabled: row.bonus_enabled === 1,
    bonusMax: row.bonus_max,
    components: components.map((c) => ({
      ...c,
      tasks: assignmentRows.filter((a) => a.component_id === c.id).length,
    })),
    assignments,
    students: studentItems,
    averageGrade: graded.length === 0 ? null : round1(graded.reduce((sum, s) => sum + s.grade, 0) / graded.length),
    sessions: db.prepare("SELECT * FROM class_sessions WHERE course_id = ? ORDER BY weekday, start_time").all(row.id).map(toSession),
    announcements: listAnnouncements(db, row.id),
    resources: listResources(db, row.id),
  }
}

/** Tin giáo viên đã đăng cho ngành / toàn trường. */
function postsOf(db, teacher) {
  return db.prepare("SELECT * FROM bulletins WHERE author_id = ? ORDER BY created_at DESC").all(teacher.id).map((row) => ({
    id: row.id,
    audience: row.audience,
    programCode: row.program_code ?? null,
    kind: row.kind,
    title: row.title,
    body: row.body,
    eventAt: row.event_at ?? null,
    location: row.location ?? null,
    createdAt: row.created_at,
  }))
}

export function teacherBootstrap(db, teacher, now) {
  const semester = currentSemester(db)
  const rows = db.prepare("SELECT * FROM courses WHERE teacher_id = ? ORDER BY rowid").all(teacher.id)
  return {
    serverTime: now,
    profile: profileOf(db, teacher),
    semester: { label: semester.label, academicYear: semester.academicYear, startsAt: semester.startsAt, endsAt: semester.endsAt },
    courses: rows.map((row) => teachingCourse(db, row, now)),
    posts: postsOf(db, teacher),
  }
}

export function getTeachingCourse(db, teacher, courseId, now) {
  return teachingCourse(db, ownCourse(db, teacher, courseId), now)
}

// ------------------------------------------------------------------ thang điểm

/**
 * Thay toàn bộ thang điểm của môn.
 * body: { components: [{ id?, name, weight }], bonusEnabled, bonusMax }
 * - Tổng weight phải bằng 100, tên không trùng.
 * - Thành phần có id cũ được giữ (đổi tên/trọng số), không có id → tạo mới, thành phần cũ không còn trong danh sách → xoá
 *   (409 nếu còn bài tập thuộc thành phần đó).
 */
export function updateGrading(db, teacher, courseId, body, now) {
  const course = ownCourse(db, teacher, courseId)
  const input = Array.isArray(body?.components) ? body.components : null
  if (!input || input.length === 0 || input.length > MAX_COMPONENTS) {
    throw new HttpError(400, "invalid_components", `Cần từ 1 đến ${MAX_COMPONENTS} thành phần điểm`)
  }

  const existing = componentsByCourse(db, [courseId]).get(courseId)
  const components = input.map((item, index) => ({
    id: typeof item?.id === "string" && existing.some((c) => c.id === item.id) ? item.id : null,
    name: text(item?.name, "name", 40),
    weight: number(item?.weight, "weight", 1, 100),
    position: index,
  }))
  if (components.some((c) => !Number.isInteger(c.weight))) {
    throw new HttpError(400, "invalid_weight", "Trọng số phải là số nguyên (%)")
  }
  const total = components.reduce((sum, c) => sum + c.weight, 0)
  if (total !== 100) throw new HttpError(400, "weights_must_sum_100", `Tổng trọng số phải bằng 100% (hiện là ${total}%)`)
  const names = new Set(components.map((c) => c.name.toLowerCase()))
  if (names.size !== components.length) throw new HttpError(400, "duplicate_component", "Tên thành phần điểm bị trùng")

  const bonusEnabled = body?.bonusEnabled === true
  const bonusMax = bonusEnabled ? number(body?.bonusMax, "bonusMax", 0.5, 5) : 0

  const removed = existing.filter((c) => !components.some((n) => n.id === c.id))
  for (const component of removed) {
    const used = db.prepare("SELECT COUNT(*) AS n FROM assignments WHERE component_id = ?").get(component.id).n
    if (used > 0) {
      throw new HttpError(409, "component_in_use", `Thành phần “${component.name}” còn ${used} bài tập, hãy chuyển hoặc xoá bài trước`)
    }
  }
  if (!bonusEnabled) {
    const bonusTasks = db.prepare("SELECT COUNT(*) AS n FROM assignments WHERE course_id = ? AND is_bonus = 1").get(courseId).n
    if (bonusTasks > 0) throw new HttpError(409, "bonus_in_use", `Môn còn ${bonusTasks} bài bonus, hãy xoá bài bonus trước khi tắt bonus`)
  }

  for (const component of removed) db.prepare("DELETE FROM grade_components WHERE id = ?").run(component.id)
  for (const c of components) {
    if (c.id) {
      db.prepare("UPDATE grade_components SET name = ?, weight = ?, position = ? WHERE id = ?").run(c.name, c.weight, c.position, c.id)
    } else {
      db.prepare("INSERT INTO grade_components (id, course_id, name, weight, position) VALUES (?, ?, ?, ?, ?)")
        .run(newId(courseId), courseId, c.name, c.weight, c.position)
    }
  }
  db.prepare("UPDATE courses SET bonus_enabled = ?, bonus_max = ? WHERE id = ?").run(bonusEnabled ? 1 : 0, bonusMax, course.id)
  return getTeachingCourse(db, teacher, courseId, now)
}

// ------------------------------------------------------------------ bài tập

/** Đọc và kiểm tra nội dung bài tập gửi lên (dùng chung cho tạo mới và sửa). */
function assignmentInput(db, course, body) {
  const isBonus = body?.isBonus === true
  let componentId = null
  if (isBonus) {
    if (course.bonus_enabled !== 1) throw new HttpError(400, "bonus_disabled", "Môn này chưa bật điểm bonus")
  } else {
    componentId = typeof body?.componentId === "string" ? body.componentId : ""
    const component = db.prepare("SELECT id FROM grade_components WHERE id = ? AND course_id = ?").get(componentId, course.id)
    if (!component) throw new HttpError(400, "invalid_componentId", "Thành phần điểm không thuộc môn này")
  }
  return {
    title: text(body?.title, "title", MAX_TITLE),
    description: text(body?.description, "description", MAX_BODY, { optional: true }),
    weight: body?.weight == null ? 1 : number(body.weight, "weight", 0.5, 100),
    dueAt: number(body?.dueAt, "dueAt", 0, Number.MAX_SAFE_INTEGER),
    isBonus,
    componentId,
  }
}

/** Gửi thông báo cho mọi sinh viên của môn. */
function notifyStudents(db, courseId, { kind, title, body, assignmentId = null, data = null }, now) {
  const insert = db.prepare(`
    INSERT INTO notifications (id, user_id, kind, title, body, created_at, read_at, assignment_id, course_id, data)
    VALUES (?, ?, ?, ?, ?, ?, NULL, ?, ?, ?)
  `)
  for (const student of studentsOf(db, courseId)) {
    insert.run(newId("n"), student.id, kind, title, body, now, assignmentId, courseId, data == null ? null : JSON.stringify(data))
  }
}

export function createAssignment(db, teacher, courseId, body, now) {
  const course = ownCourse(db, teacher, courseId)
  const input = assignmentInput(db, course, body)
  if (input.dueAt <= now) throw new HttpError(400, "invalid_dueAt", "Hạn nộp phải ở tương lai")
  const id = newId(courseId)
  db.prepare(`
    INSERT INTO assignments (id, course_id, component_id, is_bonus, title, description, weight, due_at, created_at, updated_at)
    VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
  `).run(id, courseId, input.componentId, input.isBonus ? 1 : 0, input.title, input.description, input.weight, input.dueAt, now, now)
  notifyStudents(db, courseId, {
    kind: "ASSIGNMENT_NEW", title: "New assignment", body: input.title, assignmentId: id, data: { dueAt: input.dueAt },
  }, now)
  return getTeachingCourse(db, teacher, courseId, now)
}

export function updateAssignment(db, teacher, assignmentId, body, now) {
  const old = ownAssignment(db, teacher, assignmentId)
  const course = ownCourse(db, teacher, old.course_id)
  const input = assignmentInput(db, course, body)
  db.prepare(`
    UPDATE assignments SET component_id = ?, is_bonus = ?, title = ?, description = ?, weight = ?, due_at = ?, updated_at = ?
    WHERE id = ?
  `).run(input.componentId, input.isBonus ? 1 : 0, input.title, input.description, input.weight, input.dueAt, now, assignmentId)
  if (input.dueAt !== old.due_at) {
    notifyStudents(db, course.id, {
      kind: "DEADLINE_CHANGED", title: "Deadline changed", body: input.title, assignmentId,
      data: { oldDueAt: old.due_at, dueAt: input.dueAt },
    }, now)
  }
  return getTeachingCourse(db, teacher, course.id, now)
}

export function deleteAssignment(db, teacher, assignmentId, now) {
  const assignment = ownAssignment(db, teacher, assignmentId)
  const graded = db.prepare("SELECT COUNT(*) AS n FROM submissions WHERE assignment_id = ? AND score IS NOT NULL").get(assignmentId).n
  if (graded > 0) throw new HttpError(409, "assignment_graded", `Bài đã có ${graded} điểm, không thể xoá`)
  db.prepare("DELETE FROM assignments WHERE id = ?").run(assignmentId)
  return getTeachingCourse(db, teacher, assignment.course_id, now)
}

/** Giáo viên bấm "Chấm tự động ngay": máy chấm mọi bài đã nộp chưa có điểm, kể cả khi chưa tới hạn. */
export function autoGradeNow(db, teacher, assignmentId, now) {
  const assignment = ownAssignment(db, teacher, assignmentId)
  const graded = autoGrade(db, now, { assignmentId, force: true })
  return { graded, course: getTeachingCourse(db, teacher, assignment.course_id, now) }
}

// ------------------------------------------------------------------ đăng tin

/**
 * body: { audience: COURSE | PROGRAM | ALL, courseId?, kind: NOTICE | EVENT, title, body, eventAt?, location?, pinned? }
 * - COURSE: lên bảng tin (Stream) của lớp và gửi thông báo cho sinh viên của lớp.
 * - PROGRAM: tin cho cả ngành của giáo viên. ALL: tin cho toàn trường.
 */
export function postAnnouncement(db, teacher, body, now) {
  const audience = body?.audience
  if (!["COURSE", "PROGRAM", "ALL"].includes(audience)) {
    throw new HttpError(400, "invalid_audience", "audience phải là COURSE, PROGRAM hoặc ALL")
  }
  const kind = body?.kind === "EVENT" ? "EVENT" : "NOTICE"
  const title = text(body?.title, "title", MAX_TITLE)
  const message = text(body?.body, "body", MAX_BODY)
  let eventAt = null
  let location = null
  if (kind === "EVENT") {
    eventAt = number(body?.eventAt, "eventAt", now, Number.MAX_SAFE_INTEGER)
    location = text(body?.location, "location", 80, { optional: true }) || null
  }

  if (audience === "COURSE") {
    const course = ownCourse(db, teacher, typeof body?.courseId === "string" ? body.courseId : "")
    const id = newId("ann")
    db.prepare("INSERT INTO announcements (id, course_id, kind, title, body, posted_at, pinned) VALUES (?, ?, 'ANNOUNCEMENT', ?, ?, ?, ?)")
      .run(id, course.id, title, message, now, body?.pinned === true ? 1 : 0)
    notifyStudents(db, course.id, { kind: "ANNOUNCEMENT", title, body: message }, now)
    return { id, audience, course: getTeachingCourse(db, teacher, course.id, now), posts: postsOf(db, teacher) }
  }

  const id = newId("b")
  db.prepare(`
    INSERT INTO bulletins (id, audience, program_code, kind, title, body, author_id, author_name, event_at, location, created_at)
    VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
  `).run(id, audience, audience === "PROGRAM" ? teacher.program_code : null, kind, title, message, teacher.id, teacher.full_name,
    eventAt, location, now)
  return { id, audience, course: null, posts: postsOf(db, teacher) }
}

export function deletePost(db, teacher, id, now) {
  const result = db.prepare("DELETE FROM bulletins WHERE id = ? AND author_id = ?").run(id, teacher.id)
  if (result.changes === 0) {
    // Có thể là tin của lớp.
    const announcement = db.prepare(`
      SELECT a.id, a.course_id FROM announcements a JOIN courses c ON c.id = a.course_id WHERE a.id = ? AND c.teacher_id = ?
    `).get(id, teacher.id)
    if (!announcement) throw new HttpError(404, "post_not_found", "Không tìm thấy tin")
    db.prepare("DELETE FROM announcements WHERE id = ?").run(id)
  }
  return teacherBootstrap(db, teacher, now)
}
