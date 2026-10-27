import { after, before, describe, test } from "node:test"
import assert from "node:assert/strict"
import { createServer } from "node:http"
import { createApp } from "../src/app.js"
import { openDatabase, resetDatabase } from "../src/db.js"

const NOW = new Date(2026, 9, 25, 10, 0, 0).getTime()
const HOUR = 3_600_000

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

  test("login is case-insensitive and returns the profile", async () => {
    const { status, body } = await call("POST", "/api/v1/auth/login", { body: { username: "BA22-041", password: "usth2025" } })
    assert.equal(status, 200)
    assert.equal(body.profile.fullName, "Hoang An")
    assert.equal(body.profile.semester, "Semester 1")
    assert.equal(body.profile.academicYear, "2026–2027")
  })

  test("logout invalidates the token", async () => {
    const token = await login()
    assert.equal((await call("POST", "/api/v1/auth/logout", { token })).status, 200)
    assert.equal((await call("GET", "/api/v1/me", { token })).status, 401)
  })
})

describe("learning data", () => {
  test("courses match the design's progress numbers", async () => {
    const token = await login()
    const { body } = await call("GET", "/api/v1/courses", { token })
    const byId = Object.fromEntries(body.items.map((c) => [c.id, c]))
    assert.deepEqual([byId.web.done, byId.web.total, byId.web.progress], [8, 10, 80])
    assert.deepEqual([byId.bio.done, byId.bio.total, byId.bio.progress], [6, 9, 67])
    assert.deepEqual([byId.earth.done, byId.earth.total, byId.earth.progress], [9, 10, 90])
    assert.equal(byId.bio.grade, 8.2)
    assert.equal(byId.earth.grade, 9.0)
  })

  test("summary counts pending and urgent work", async () => {
    const token = await login()
    const { body } = await call("GET", "/api/v1/summary", { token })
    assert.equal(body.pending, 6)
    assert.equal(body.urgent, 2) // proposal (~6.7h) và Lab 04 (20h)
  })

  test("assignment filters", async () => {
    const token = await login()
    const urgent = await call("GET", "/api/v1/assignments?filter=urgent", { token })
    assert.deepEqual(urgent.body.items.map((a) => a.id), ["proposal", "lab"])
    const web = await call("GET", "/api/v1/assignments?courseId=web&filter=todo", { token })
    assert.deepEqual(web.body.items.map((a) => a.id), ["proposal", "midterm"])
    assert.equal((await call("GET", "/api/v1/assignments?filter=bogus", { token })).status, 400)
  })

  test("course detail exposes grade components, sessions and stream", async () => {
    const token = await login()
    const { body } = await call("GET", "/api/v1/courses/web", { token })
    assert.deepEqual(body.gradeComponents.map((c) => [c.key, c.weight, c.status]), [
      ["ATTENDANCE", 10, "IN_PROGRESS"],
      ["MIDTERM_LABS", 30, "IN_PROGRESS"],
      ["FINAL_PROJECT", 60, "NOT_GRADED"],
    ])
    assert.equal(body.sessions.length, 2)
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

describe("misc", () => {
  test("notifications can be marked read", async () => {
    const token = await login()
    const before = await call("GET", "/api/v1/notifications", { token })
    assert.equal(before.body.unread, 2)
    const after = await call("POST", "/api/v1/notifications/n-msg-web/read", { token })
    assert.equal(after.body.unread, 1)
  })

  test("bootstrap bundles everything the app needs", async () => {
    const token = await login()
    const { body } = await call("GET", "/api/v1/bootstrap", { token })
    for (const key of ["profile", "semester", "summary", "courses", "assignments", "timetable", "notifications"]) {
      assert.ok(body[key], `missing ${key}`)
    }
    assert.equal(body.semester.enrolledCredits, 10)
    assert.equal(body.timetable.sessions.length, 6)
  })

  test("grade report is a CSV download", async () => {
    const token = await login()
    const { status, body, headers } = await call("GET", "/api/v1/reports/grades.csv", { token })
    assert.equal(status, 200)
    assert.match(headers.get("content-disposition"), /attachment; filename=".*\.csv"/)
    assert.match(body, /Molecular Biology,BIO 2201,3,6,9,8\.2/)
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
