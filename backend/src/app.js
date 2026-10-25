import { authenticate, createSession, deleteSession, verifyPassword } from "./auth.js"
import { resetDatabase, seededAt } from "./db.js"
import {
  HttpError, bootstrap, getAssignment, getCourse, gradeReportCsv, learningSummary, listAssignments,
  listCourses, listNotifications, markCompleted, markNotificationRead, profileOf, semesterOverview,
  timetable, unmarkCompleted,
} from "./services.js"

const MAX_BODY_BYTES = 64 * 1024

/**
 * Bảng route: [method, pattern, handler, { auth }]. Pattern dùng `:param`.
 * Handler nhận ctx = { db, user, params, query, body, now } và trả về object JSON
 * hoặc { status, headers, body } khi cần tuỳ biến response.
 */
function routes({ allowReset }) {
  const table = [
    ["GET", "/health", ({ db, now }) => ({ status: "ok", serverTime: now, seededAt: seededAt(db) }), { auth: false }],

    ["POST", "/api/v1/auth/login", ({ db, body, now }) => {
      const username = String(body?.username ?? "").trim().toLowerCase()
      const password = String(body?.password ?? "")
      if (!username || !password) throw new HttpError(400, "missing_credentials", "Vui lòng nhập mã sinh viên và mật khẩu")
      const user = db.prepare("SELECT * FROM users WHERE username = ?").get(username)
      if (!user || !verifyPassword(password, user.password_hash)) {
        throw new HttpError(401, "invalid_credentials", "Mã sinh viên hoặc mật khẩu không đúng")
      }
      const session = createSession(db, user.id, now)
      return { token: session.token, expiresAt: session.expiresAt, profile: profileOf(db, user) }
    }, { auth: false }],

    ["POST", "/api/v1/auth/logout", ({ db, user }) => {
      deleteSession(db, user.token)
      return { ok: true }
    }],

    ["GET", "/api/v1/me", ({ db, user }) => profileOf(db, user)],
    ["GET", "/api/v1/bootstrap", ({ db, user, now }) => bootstrap(db, user, now)],
    ["GET", "/api/v1/summary", ({ db, user, now }) => learningSummary(db, user.id, now)],
    ["GET", "/api/v1/semester", ({ db, user, now }) => semesterOverview(db, user, now)],

    ["GET", "/api/v1/courses", ({ db, user, now }) => ({ items: listCourses(db, user.id, now) })],
    ["GET", "/api/v1/courses/:id", ({ db, user, params, now }) => getCourse(db, user.id, params.id, now)],

    ["GET", "/api/v1/assignments", ({ db, user, query, now }) => ({
      items: listAssignments(db, user.id, now, {
        courseId: query.get("courseId") || undefined,
        filter: query.get("filter") || undefined,
      }),
    })],
    ["GET", "/api/v1/assignments/:id", ({ db, user, params, now }) => getAssignment(db, user.id, params.id, now)],
    ["POST", "/api/v1/assignments/:id/completion", ({ db, user, params, now }) => markCompleted(db, user.id, params.id, now)],
    ["DELETE", "/api/v1/assignments/:id/completion", ({ db, user, params, now }) => unmarkCompleted(db, user.id, params.id, now)],

    ["GET", "/api/v1/timetable", ({ db, user }) => timetable(db, user.id)],

    ["GET", "/api/v1/notifications", ({ db, user }) => listNotifications(db, user.id)],
    ["POST", "/api/v1/notifications/:id/read", ({ db, user, params, now }) => markNotificationRead(db, user.id, params.id, now)],

    ["GET", "/api/v1/reports/grades.csv", ({ db, user, now }) => {
      const profile = profileOf(db, user)
      const filename = `USTH-${profile.semester}-${profile.academicYear}-grade-report.csv`.replace(/[\s–]+/g, "-")
      return {
        status: 200,
        headers: {
          "Content-Type": "text/csv; charset=utf-8",
          "Content-Disposition": `attachment; filename="${filename}"`,
        },
        body: gradeReportCsv(db, user, now),
      }
    }],
  ]
  if (allowReset) {
    table.push(["POST", "/api/v1/dev/reset", ({ db, now }) => {
      resetDatabase(db, now)
      return { ok: true, seededAt: now }
    }, { auth: false }])
  }
  return table.map(([method, pattern, handler, options]) => ({
    method,
    handler,
    auth: options?.auth ?? true,
    keys: [...pattern.matchAll(/:(\w+)/g)].map((m) => m[1]),
    regex: new RegExp(`^${pattern.replace(/:(\w+)/g, "([^/]+)")}$`),
  }))
}

function send(res, status, body, headers = {}) {
  const isText = typeof body === "string"
  const payload = isText ? body : JSON.stringify(body)
  res.writeHead(status, {
    "Content-Type": isText ? "text/plain; charset=utf-8" : "application/json; charset=utf-8",
    "Access-Control-Allow-Origin": "*",
    "Access-Control-Allow-Headers": "Authorization, Content-Type",
    "Access-Control-Allow-Methods": "GET, POST, DELETE, OPTIONS",
    "Cache-Control": "no-store",
    ...headers,
  })
  res.end(payload)
}

function readJson(req) {
  return new Promise((resolve, reject) => {
    let size = 0
    const chunks = []
    req.on("data", (chunk) => {
      size += chunk.length
      if (size > MAX_BODY_BYTES) {
        reject(new HttpError(413, "payload_too_large", "Request body quá lớn"))
        req.destroy()
      } else chunks.push(chunk)
    })
    req.on("end", () => {
      if (chunks.length === 0) return resolve(null)
      try {
        resolve(JSON.parse(Buffer.concat(chunks).toString("utf8")))
      } catch {
        reject(new HttpError(400, "invalid_json", "Body không phải JSON hợp lệ"))
      }
    })
    req.on("error", reject)
  })
}

export function createApp(db, { clock = () => Date.now(), allowReset = false, log = () => {} } = {}) {
  const table = routes({ allowReset })

  return async function handle(req, res) {
    const startedAt = Date.now()
    const url = new URL(req.url, "http://localhost")
    let status = 500
    try {
      if (req.method === "OPTIONS") {
        status = 204
        return send(res, 204, "")
      }
      const candidates = table.filter((route) => route.regex.test(url.pathname))
      if (candidates.length === 0) throw new HttpError(404, "not_found", `Không có endpoint ${url.pathname}`)
      const route = candidates.find((r) => r.method === req.method)
      if (!route) throw new HttpError(405, "method_not_allowed", `${req.method} không được hỗ trợ cho ${url.pathname}`)

      const now = clock()
      const user = route.auth ? authenticate(db, req.headers.authorization, now) : null
      if (route.auth && !user) throw new HttpError(401, "unauthorized", "Phiên đăng nhập không hợp lệ hoặc đã hết hạn")

      const match = route.regex.exec(url.pathname)
      const params = Object.fromEntries(route.keys.map((key, i) => [key, decodeURIComponent(match[i + 1])]))
      const body = req.method === "POST" ? await readJson(req) : null
      const result = await route.handler({ db, user, params, query: url.searchParams, body, now })

      if (result && typeof result.status === "number" && "body" in result && result.headers) {
        status = result.status
        return send(res, result.status, result.body, result.headers)
      }
      status = 200
      return send(res, 200, result)
    } catch (error) {
      if (error instanceof HttpError) {
        status = error.status
        return send(res, error.status, { error: { code: error.code, message: error.message } })
      }
      console.error(error)
      return send(res, 500, { error: { code: "internal_error", message: "Lỗi máy chủ" } })
    } finally {
      log(`${req.method} ${url.pathname} → ${status} (${Date.now() - startedAt} ms)`)
    }
  }
}
