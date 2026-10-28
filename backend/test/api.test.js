import { after, before, beforeEach, describe, test } from "node:test"
import assert from "node:assert/strict"
import { createServer } from "node:http"
import { createApp } from "../src/app.js"
import { openDatabase, resetDatabase } from "../src/db.js"

const NOW = new Date(2026, 9, 25, 10, 0, 0).getTime()
const HOUR = 3_600_000
const DAY = 24 * HOUR

let db
let server
let baseUrl
let clock = NOW

before(async () => {
  db = openDatabase(":memory:")
  resetDatabase(db, NOW)
  server = createServer(createApp(db, { clock: () => clock, allowReset: true }))
  await new Promise((resolve) => server.listen(0, "127.0.0.1", resolve))
  baseUrl = `http://127.0.0.1:${server.address().port}`
})

after(() => server.close())

async function call(method, path, { token, body } = {}) {
  const response = await fetch(baseUrl + path, {
    method,
    headers: {
      ...(token ? { Authorization: `Bearer ${token}` } : {}),
      ...(body ? { "Content-Type": "application/json" } : {}),
    },
    body: body ? JSON.stringify(body) : undefined,
  })
  const text = await response.text()
  const isJson = response.headers.get("content-type")?.includes("json")
  return { status: response.status, body: isJson ? JSON.parse(text) : text, headers: response.headers }
}

async function login(username = "ba22-041", password = "usth2025") {
  const { status, body } = await call("POST", "/api/v1/auth/login", { body: { username, password } })
  assert.equal(status, 200)
  return body.token
}

describe("auth", () => {
  test("rejects wrong password", async () => {
    const { status, body } = await call("POST", "/api/v1/auth/login", { body: { username: "ba22-041", password: "nope" } })
    assert.equal(status, 401)
    assert.equal(body.error.code, "invalid_credentials")
  })

  test("protects endpoints without a token", async () => {
    const { status } = await call("GET", "/api/v1/courses")
    assert.equal(status, 401)
  })

  test("login is case-insensitive and returns the profile with a role", async () => {
    const { status, body } = await call("POST", "/api/v1/auth/login", { body: { username: "BA22-041", password: "usth2025" } })
    assert.equal(status, 200)
    assert.equal(body.profile.fullName, "Hoang An")
    assert.equal(body.profile.role, "STUDENT")
    assert.equal(body.profile.programName, "Information & Communication Technology")
    assert.equal(body.profile.semester, "Semester 1")
    assert.equal(body.profile.academicYear, "2026–2027")

    const teacher = await call("POST", "/api/v1/auth/login", { body: { username: "gv-minh", password: "usth2025" } })
    assert.equal(teacher.body.profile.role, "TEACHER")
  })

  test("logout invalidates the token", async () => {
    const token = await login()
    assert.equal((await call("POST", "/api/v1/auth/logout", { token })).status, 200)
    assert.equal((await call("GET", "/api/v1/me", { token })).status, 401)
  })

  test("each role only reaches its own endpoints", async () => {
    const student = await login()
    const teacher = await login("gv-minh")
    assert.equal((await call("GET", "/api/v1/teacher/bootstrap", { token: student })).status, 403)
    const forbidden = await call("GET", "/api/v1/bootstrap", { token: teacher })
    assert.equal(forbidden.status, 403)
    assert.equal(forbidden.body.error.code, "forbidden")
    assert.equal((await call("GET", "/api/v1/me", { token: teacher })).body.role, "TEACHER")
  })
})

describe("learning data", () => {
  test("courses expose progress and grades on a 20-point scale", async () => {
    const token = await login()
    const { body } = await call("GET", "/api/v1/courses", { token })
    const byId = Object.fromEntries(body.items.map((c) => [c.id, c]))
    assert.deepEqual([byId.web.done, byId.web.total, byId.web.progress], [8, 12, 67])
    assert.deepEqual([byId.bio.done, byId.bio.total, byId.bio.progress], [6, 10, 60])
    assert.deepEqual([byId.earth.done, byId.earth.total, byId.earth.progress], [9, 11, 82])
    assert.equal(byId.web.maxScore, 20)
    assert.equal(byId.bio.grade, 16.3)
    assert.equal(byId.earth.grade, 17.9)
    // Bonus: môn bật bonus nhưng chưa có bài bonus nào được chấm → 0; môn không bật → null.
    assert.deepEqual([byId.web.bonusEnabled, byId.web.bonusMax, byId.web.bonus], [true, 1, 0])
    assert.deepEqual([byId.bio.bonusEnabled, byId.bio.bonus], [false, null])
  })

  test("summary counts pending and urgent work", async () => {
    const token = await login()
    const { body } = await call("GET", "/api/v1/summary", { token })
    assert.equal(body.pending, 10)
    assert.equal(body.urgent, 2) // proposal (~6.7h) và Lab 04 (20h)
  })

  test("assignment filters", async () => {
    const token = await login()
    const urgent = await call("GET", "/api/v1/assignments?filter=urgent", { token })
    assert.deepEqual(urgent.body.items.map((a) => a.id), ["proposal", "lab"])
    const web = await call("GET", "/api/v1/assignments?courseId=web&filter=todo", { token })
    assert.deepEqual(web.body.items.map((a) => a.id), ["proposal", "midterm", "web-bonus", "web-demo"])
    assert.equal((await call("GET", "/api/v1/assignments?filter=bogus", { token })).status, 400)
  })

  test("assignments carry their grade component and contribution", async () => {
    const token = await login()
    const proposal = (await call("GET", "/api/v1/assignments/proposal", { token })).body
    assert.deepEqual([proposal.componentName, proposal.componentWeight, proposal.contribution, proposal.maxScore],
      ["Final project", 60, 15, 20])
    const bonus = (await call("GET", "/api/v1/assignments/web-bonus", { token })).body
    assert.deepEqual([bonus.isBonus, bonus.componentId, bonus.contribution, bonus.bonusMax], [true, null, null, 1])
  })

  test("course detail exposes grade components, sessions and stream", async () => {
    const token = await login()
    const { body } = await call("GET", "/api/v1/courses/web", { token })
    assert.deepEqual(body.gradeComponents.map((c) => [c.name, c.weight, c.status]), [
      ["Attendance", 10, "GRADED"],
      ["Mid-term & labs", 30, "IN_PROGRESS"],
      ["Final project", 60, "NOT_GRADED"],
    ])
    assert.equal(body.teacher, "Dr. Nguyen Minh")
    assert.equal(body.sessions.length, 2)
    assert.equal(body.sessions[0].kind, "LECTURE")
    assert.equal(body.announcements[0].pinned, true)
    assert.equal(body.resources.length, 4)
    assert.equal(body.resources[0].week, 9)
  })

  test("students only see their own courses", async () => {
    const token = await login("ba22-087")
    assert.equal((await call("GET", "/api/v1/courses/bio", { token })).status, 404)
    const { body } = await call("GET", "/api/v1/courses", { token })
    assert.deepEqual(body.items.map((c) => c.id), ["web", "earth"])
  })
})

describe("completion", () => {
  test("mark done, then undo", async () => {
    const token = await login()
    const done = await call("POST", "/api/v1/assignments/midterm/completion", { token })
    assert.equal(done.body.status, "DONE")
    assert.equal((await call("GET", "/api/v1/courses/web", { token })).body.done, 9)

    const undone = await call("DELETE", "/api/v1/assignments/midterm/completion", { token })
    assert.equal(undone.body.status, "UPCOMING")
  })

  test("graded work cannot be un-marked", async () => {
    const token = await login()
    const { status, body } = await call("DELETE", "/api/v1/assignments/lab3/completion", { token })
    assert.equal(status, 409)
    assert.equal(body.error.code, "already_graded")
  })

  test("status follows the clock", async () => {
    const token = await login()
    clock = NOW + 8 * HOUR
    const { body } = await call("GET", "/api/v1/assignments/proposal", { token })
    assert.equal(body.status, "OVERDUE")
    clock = NOW
  })
})

describe("notifications", () => {
  test("split into the student's program and general (teachers + whole school)", async () => {
    const an = (await call("GET", "/api/v1/notifications", { token: await login() })).body
    assert.equal(an.unread, 6)
    assert.deepEqual(an.unreadByChannel, { PROGRAM: 2, GENERAL: 4 })
    const program = an.items.filter((n) => n.channel === "PROGRAM").map((n) => n.id)
    assert.deepEqual(program.sort(), ["b-ict-career", "b-ict-hackathon"])
    const career = an.items.find((n) => n.id === "b-ict-career")
    assert.equal(career.kind, "EVENT")
    assert.ok(career.eventAt > NOW)
    assert.equal(career.location, "Hall A · Building A21")
    const grade = an.items.find((n) => n.id === "n-grade-lab3")
    assert.deepEqual(grade.data, { score: 18, maxScore: 20 })

    // Sinh viên ngành SA không thấy tin của ngành ICT.
    const chau = (await call("GET", "/api/v1/notifications", { token: await login("ba22-087") })).body
    assert.deepEqual(chau.items.filter((n) => n.channel === "PROGRAM").map((n) => n.id), ["b-sa-visit"])
  })

  test("mark one read, then a whole channel", async () => {
    const token = await login()
    const one = await call("POST", "/api/v1/notifications/n-msg-web/read", { token })
    assert.equal(one.body.unread, 5)
    const bulletin = await call("POST", "/api/v1/notifications/b-all-exams/read", { token })
    assert.equal(bulletin.body.unreadByChannel.GENERAL, 2)
    const program = await call("POST", "/api/v1/notifications/read-all", { token, body: { channel: "PROGRAM" } })
    assert.deepEqual(program.body.unreadByChannel, { PROGRAM: 0, GENERAL: 2 })
    assert.equal((await call("POST", "/api/v1/notifications/b-sa-visit/read", { token })).status, 404) // tin ngành khác
    assert.equal((await call("POST", "/api/v1/notifications/read-all", { token, body: { channel: "X" } })).status, 400)
    const all = await call("POST", "/api/v1/notifications/read-all", { token })
    assert.equal(all.body.unread, 0)
  })
})

describe("misc", () => {
  test("bootstrap bundles everything the app needs", async () => {
    const token = await login()
    const { body } = await call("GET", "/api/v1/bootstrap", { token })
    for (const key of ["profile", "semester", "summary", "courses", "assignments", "timetable", "notifications"]) {
      assert.ok(body[key], `missing ${key}`)
    }
    assert.equal(body.profile.programCode, "ICT")
    assert.equal(body.semester.enrolledCredits, 10)
    assert.equal(body.semester.maxScore, 20)
    assert.equal(body.timetable.sessions.length, 6)
  })

  test("grade report is a CSV download", async () => {
    const token = await login()
    const { status, body, headers } = await call("GET", "/api/v1/reports/grades.csv", { token })
    assert.equal(status, 200)
    assert.match(headers.get("content-disposition"), /attachment; filename=".*\.csv"/)
    assert.match(body, /Molecular Biology,BIO 2201,3,6,10,16\.3,,16\.3/)
  })

  test("unknown routes return JSON errors", async () => {
    const { status, body } = await call("GET", "/api/v1/nope")
    assert.equal(status, 404)
    assert.equal(body.error.code, "not_found")
  })

  test("malformed path parameters are a 400, not a crash", async () => {
    const token = await login()
    const { status, body } = await call("GET", "/api/v1/courses/%E0%A4%A", { token })
    assert.equal(status, 400)
    assert.equal(body.error.code, "invalid_path")
  })

  test("bootstrap course data matches the course endpoints", async () => {
    const token = await login()
    const { body } = await call("GET", "/api/v1/bootstrap", { token })
    for (const course of body.courses) {
      const detail = (await call("GET", `/api/v1/courses/${course.id}`, { token })).body
      assert.deepEqual(course.gradeComponents, detail.gradeComponents)
      assert.deepEqual([course.done, course.total, course.grade], [detail.done, detail.total, detail.grade])
    }
  })

  test("expired sessions are purged on the next login", async () => {
    await login()
    clock = NOW + 31 * 24 * HOUR
    await login()
    const stale = db.prepare("SELECT COUNT(*) AS n FROM sessions WHERE expires_at <= ?").get(clock).n
    assert.equal(stale, 0)
    clock = NOW
  })
})

describe("teacher", () => {
  // Mỗi test bắt đầu từ dữ liệu seed sạch (reset xoá cả phiên đăng nhập nên phải đăng nhập lại).
  beforeEach(() => {
    clock = NOW
    resetDatabase(db, NOW)
  })

  test("sees only the courses they teach, with students and auto grades", async () => {
    const token = await login("gv-minh")
    const { body } = await call("GET", "/api/v1/teacher/bootstrap", { token })
    assert.deepEqual(body.courses.map((c) => c.id), ["web"])
    const web = body.courses[0]
    assert.deepEqual(web.students.map((s) => [s.fullName, s.grade]), [["Hoang An", 17.6], ["Tran Minh Chau", 15.8]])
    assert.deepEqual(web.components.map((c) => [c.name, c.weight]), [["Attendance", 10], ["Mid-term & labs", 30], ["Final project", 60]])
    const proposal = web.assignments.find((a) => a.id === "proposal")
    assert.deepEqual([proposal.students, proposal.submitted, proposal.graded], [2, 1, 0])
    assert.equal((await call("GET", "/api/v1/teacher/courses/bio", { token })).status, 404)
  })

  test("grade scheme must add up to 100% and keep components that are in use", async () => {
    const token = await login("gv-minh")
    const put = (body) => call("PUT", "/api/v1/teacher/courses/web/grading", { token, body })
    const bad = await put({ components: [{ id: "web-att", name: "Attendance", weight: 10 }], bonusEnabled: false })
    assert.equal(bad.status, 400)
    assert.equal(bad.body.error.code, "weights_must_sum_100")

    const dropFinal = await put({
      components: [{ id: "web-att", name: "Attendance", weight: 40 }, { id: "web-labs", name: "Labs", weight: 60 }],
      bonusEnabled: true, bonusMax: 1,
    })
    assert.equal(dropFinal.status, 409)
    assert.equal(dropFinal.body.error.code, "component_in_use")

    const noBonus = await put({
      components: [{ id: "web-att", name: "Attendance", weight: 10 }, { id: "web-labs", name: "Labs", weight: 30 },
        { id: "web-final", name: "Final project", weight: 60 }],
      bonusEnabled: false,
    })
    assert.equal(noBonus.status, 409)
    assert.equal(noBonus.body.error.code, "bonus_in_use")
  })

  test("changing weights changes the students' grades; new components can be added", async () => {
    const token = await login("gv-minh")
    const { status, body } = await call("PUT", "/api/v1/teacher/courses/web/grading", {
      token,
      body: {
        components: [
          { id: "web-att", name: "Attendance", weight: 20 },
          { id: "web-labs", name: "Mid-term & labs", weight: 20 },
          { name: "Bonus quizzes", weight: 10 },
          { id: "web-final", name: "Final project", weight: 50 },
        ],
        bonusEnabled: true,
        bonusMax: 2,
      },
    })
    assert.equal(status, 200)
    assert.deepEqual(body.components.map((c) => c.weight), [20, 20, 10, 50])
    assert.equal(body.bonusMax, 2)

    const web = (await call("GET", "/api/v1/courses/web", { token: await login() })).body
    // (16.29 × 20 + 18 × 20) / 40 = 17.1
    assert.equal(web.grade, 17.1)
    assert.equal(web.gradeComponents[2].status, "NOT_GRADED")
  })

  test("new assignments and deadline changes notify the class", async () => {
    const token = await login("gv-minh")
    const create = (body) => call("POST", "/api/v1/teacher/courses/web/assignments", { token, body })
    assert.equal((await create({ title: "Late", componentId: "web-labs", dueAt: NOW - HOUR })).status, 400)
    assert.equal((await create({ title: "Wrong course", componentId: "bio-labs", dueAt: NOW + DAY })).status, 400)

    const created = await create({ title: "Lab 05 · Testing", description: "Write unit tests", componentId: "web-labs", weight: 10, dueAt: NOW + 3 * DAY })
    assert.equal(created.status, 200)
    const lab5 = created.body.assignments.find((a) => a.title === "Lab 05 · Testing")
    assert.equal(lab5.contribution, 7.5) // 30% × 10 / (20 + 10 + 10)

    const chau = await login("ba22-087")
    let feed = (await call("GET", "/api/v1/notifications", { token: chau })).body.items
    assert.ok(feed.some((n) => n.kind === "ASSIGNMENT_NEW" && n.assignmentId === lab5.id))

    const moved = await call("PUT", `/api/v1/teacher/assignments/${lab5.id}`, {
      token, body: { title: lab5.title, description: "", componentId: "web-labs", weight: 10, dueAt: NOW + 5 * DAY },
    })
    assert.equal(moved.status, 200)
    feed = (await call("GET", "/api/v1/notifications", { token: chau })).body.items
    const change = feed.find((n) => n.kind === "DEADLINE_CHANGED")
    assert.deepEqual(change.data, { oldDueAt: NOW + 3 * DAY, dueAt: NOW + 5 * DAY })
    assert.equal(change.channel, "GENERAL")

    assert.equal((await call("DELETE", `/api/v1/teacher/assignments/${lab5.id}`, { token })).status, 200)
    assert.equal((await call("DELETE", "/api/v1/teacher/assignments/lab3", { token })).status, 409)
  })

  test("the machine grades submissions after the deadline, or right away on request", async () => {
    const student = await login()
    await call("POST", "/api/v1/assignments/midterm/completion", { token: student })
    await call("POST", "/api/v1/assignments/web-bonus/completion", { token: student })

    const teacher = await login("gv-minh")
    const now = await call("POST", "/api/v1/teacher/assignments/midterm/auto-grade", { token: teacher })
    assert.equal(now.body.graded, 1)
    const midterm = (await call("GET", "/api/v1/assignments/midterm", { token: student })).body
    assert.ok(midterm.score >= 11 && midterm.score <= 19.5)
    assert.equal(midterm.gradedBy, "AUTO")

    // Bài bonus: máy chấm sau khi qua hạn (10 ngày), bonus của môn > 0.
    clock = NOW + 11 * DAY
    const bonus = (await call("GET", "/api/v1/assignments/web-bonus", { token: student })).body
    assert.equal(bonus.gradedBy, "AUTO")
    const web = (await call("GET", "/api/v1/courses/web", { token: student })).body
    assert.ok(web.bonus > 0 && web.bonus <= 1)
    assert.equal(web.grade, Math.min(20, Math.round((web.baseGrade + web.bonus) * 10) / 10))
    const feed = (await call("GET", "/api/v1/notifications", { token: student })).body.items
    assert.ok(feed.some((n) => n.kind === "GRADE" && n.assignmentId === "web-bonus"))
  })

  test("posts reach the class, the teacher's program or the whole school", async () => {
    const token = await login("gv-minh")
    const post = (body) => call("POST", "/api/v1/teacher/announcements", { token, body })
    assert.equal((await post({ audience: "COURSE", courseId: "bio", title: "x", body: "y" })).status, 404)
    assert.equal((await post({ audience: "PROGRAM", kind: "EVENT", title: "x", body: "y" })).status, 400) // thiếu eventAt

    const course = await post({ audience: "COURSE", courseId: "web", title: "Room change", body: "Thursday lab moves to A · 405" })
    assert.equal(course.status, 200)
    assert.ok(course.body.course.announcements.some((a) => a.title === "Room change" && !a.pinned))
    await post({ audience: "PROGRAM", kind: "EVENT", title: "ICT meetup", body: "Pizza & demos", eventAt: NOW + 2 * DAY, location: "A21" })
    await post({ audience: "ALL", title: "Campus closed Monday", body: "Public holiday" })

    const an = (await call("GET", "/api/v1/notifications", { token: await login() })).body.items
    assert.ok(an.some((n) => n.kind === "ANNOUNCEMENT" && n.title === "Room change" && n.courseId === "web"))
    assert.ok(an.some((n) => n.channel === "PROGRAM" && n.title === "ICT meetup" && n.author === "Dr. Nguyen Minh"))
    assert.ok(an.some((n) => n.channel === "GENERAL" && n.title === "Campus closed Monday"))

    const chau = (await call("GET", "/api/v1/notifications", { token: await login("ba22-087") })).body.items
    assert.ok(!chau.some((n) => n.title === "ICT meetup")) // ngành SA
    assert.ok(chau.some((n) => n.title === "Campus closed Monday"))

    const posts = (await call("GET", "/api/v1/teacher/bootstrap", { token })).body.posts
    assert.equal(posts.length, 3) // 2 tin vừa đăng + tin hackathon trong seed
    assert.equal((await call("DELETE", `/api/v1/teacher/announcements/${posts[0].id}`, { token })).status, 200)
  })

  test("dev reset works inside the request transaction", async () => {
    assert.equal((await call("POST", "/api/v1/dev/reset")).status, 200)
  })
})

// Chạy cuối cùng vì khoá tạm tài khoản ba22-087.
describe("login throttling", () => {
  test("locks an account after 5 wrong passwords, then unlocks after the window", async () => {
    const attempt = (password) => call("POST", "/api/v1/auth/login", { body: { username: "ba22-087", password } })
    for (let i = 0; i < 5; i++) assert.equal((await attempt("wrong")).status, 401)

    const locked = await attempt("usth2025")
    assert.equal(locked.status, 429)
    assert.equal(locked.body.error.code, "too_many_attempts")
    assert.ok(Number(locked.headers.get("retry-after")) > 0)

    clock = NOW + 16 * 60 * 1000
    assert.equal((await attempt("usth2025")).status, 200)
    clock = NOW
  })

  test("a successful login resets the failure count", async () => {
    const attempt = (password) => call("POST", "/api/v1/auth/login", { body: { username: "ba22-041", password } })
    for (let i = 0; i < 4; i++) await attempt("wrong")
    assert.equal((await attempt("usth2025")).status, 200)
    for (let i = 0; i < 4; i++) await attempt("wrong")
    assert.equal((await attempt("usth2025")).status, 200)
  })
})
