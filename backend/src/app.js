import {
  authenticate, clearFailedLogins, createSession, deleteSession, lockedForMs, recordFailedLogin, verifyPassword,
} from "./auth.js"
import { resetDatabase, seededAt } from "./db.js"
import {
  HttpError, bootstrap, getAssignment, getCourse, gradeReportCsv, learningSummary, listAssignments,
  listCourses, listNotifications, markCompleted, markNotificationRead, profileOf, semesterOverview,
  timetable, unmarkCompleted,
} from "./services.js"

const MAX_BODY_BYTES = 64 * 1024

// ------------------------------------------------------------------ các API

/**
 * Danh sách API. Mỗi API gồm:
 * - method, path: "/api/v1/courses/:id" — phần ":id" là tham số, đọc bằng params.id
 * - auth: true nếu cần đăng nhập (header Authorization: Bearer <token>)
 * - run(ctx): hàm xử lý, ctx = { db, user, params, query, body, now, failedLogins }.
 *   Trả về một object → server gửi JSON. Muốn gửi kiểu khác (CSV) thì trả về { status, headers, body }.
 */
function makeRoutes(allowReset) {
  const routes = [
    {
      method: "GET", path: "/health", auth: false,
      run(ctx) {
        return { status: "ok", serverTime: ctx.now, seededAt: seededAt(ctx.db) }
      },
    },
    {
      method: "POST", path: "/api/v1/auth/login", auth: false,
      run(ctx) {
        return login(ctx)
      },
    },
    {
      method: "POST", path: "/api/v1/auth/logout", auth: true,
      run(ctx) {
        deleteSession(ctx.db, ctx.user.token)
        return { ok: true }
      },
    },
    {
      method: "GET", path: "/api/v1/me", auth: true,
      run(ctx) {
        return profileOf(ctx.db, ctx.user)
      },
    },
    {
      method: "GET", path: "/api/v1/bootstrap", auth: true,
      run(ctx) {
        return bootstrap(ctx.db, ctx.user, ctx.now)
      },
    },
    {
      method: "GET", path: "/api/v1/summary", auth: true,
      run(ctx) {
        return learningSummary(ctx.db, ctx.user.id, ctx.now)
      },
    },
    {
      method: "GET", path: "/api/v1/semester", auth: true,
      run(ctx) {
        return semesterOverview(ctx.db, ctx.user, ctx.now)
      },
    },
    {
      method: "GET", path: "/api/v1/courses", auth: true,
      run(ctx) {
        return { items: listCourses(ctx.db, ctx.user.id, ctx.now) }
      },
    },
    {
      method: "GET", path: "/api/v1/courses/:id", auth: true,
      run(ctx) {
        return getCourse(ctx.db, ctx.user.id, ctx.params.id, ctx.now)
      },
    },
    {
      method: "GET", path: "/api/v1/assignments", auth: true,
      run(ctx) {
        const options = {
          courseId: ctx.query.get("courseId") || undefined,
          filter: ctx.query.get("filter") || undefined,
        }
        return { items: listAssignments(ctx.db, ctx.user.id, ctx.now, options) }
      },
    },
    {
      method: "GET", path: "/api/v1/assignments/:id", auth: true,
      run(ctx) {
        return getAssignment(ctx.db, ctx.user.id, ctx.params.id, ctx.now)
      },
    },
    {
      method: "POST", path: "/api/v1/assignments/:id/completion", auth: true,
      run(ctx) {
        return markCompleted(ctx.db, ctx.user.id, ctx.params.id, ctx.now)
      },
    },
    {
      method: "DELETE", path: "/api/v1/assignments/:id/completion", auth: true,
      run(ctx) {
        return unmarkCompleted(ctx.db, ctx.user.id, ctx.params.id, ctx.now)
      },
    },
    {
      method: "GET", path: "/api/v1/timetable", auth: true,
      run(ctx) {
        return timetable(ctx.db, ctx.user.id)
      },
    },
    {
      method: "GET", path: "/api/v1/notifications", auth: true,
      run(ctx) {
        return listNotifications(ctx.db, ctx.user.id)
      },
    },
    {
      method: "POST", path: "/api/v1/notifications/:id/read", auth: true,
      run(ctx) {
        return markNotificationRead(ctx.db, ctx.user.id, ctx.params.id, ctx.now)
      },
    },
    {
      method: "GET", path: "/api/v1/reports/grades.csv", auth: true,
      run(ctx) {
        const profile = profileOf(ctx.db, ctx.user)
        // VD: "USTH-Semester-1-2026-2027-grade-report.csv" (đổi khoảng trắng và dấu – thành "-")
        let filename = "USTH-" + profile.semester + "-" + profile.academicYear + "-grade-report.csv"
        filename = filename.replaceAll(" ", "-").replaceAll("–", "-")
        return {
          status: 200,
          headers: {
            "Content-Type": "text/csv; charset=utf-8",
            "Content-Disposition": 'attachment; filename="' + filename + '"',
          },
          body: gradeReportCsv(ctx.db, ctx.user, ctx.now),
        }
      },
    },
  ]

  // API xoá và tạo lại dữ liệu mẫu — chỉ bật khi không chạy production.
  if (allowReset) {
    routes.push({
      method: "POST", path: "/api/v1/dev/reset", auth: false,
      run(ctx) {
        resetDatabase(ctx.db, ctx.now)
        return { ok: true, seededAt: ctx.now }
      },
    })
  }
  return routes
}

/** POST /api/v1/auth/login */
function login(ctx) {
  const body = ctx.body || {}
  const username = String(body.username || "").trim().toLowerCase()
  const password = String(body.password || "")
  if (username === "" || password === "") {
    throw new HttpError(400, "missing_credentials", "Vui lòng nhập mã sinh viên và mật khẩu")
  }

  // Sai quá 5 lần trong 15 phút → tạm khoá.
  const waitMs = lockedForMs(ctx.failedLogins, username, ctx.now)
  if (waitMs > 0) {
    const minutes = Math.ceil(waitMs / 60000)
    const headers = { "Retry-After": String(Math.ceil(waitMs / 1000)) }
    throw new HttpError(429, "too_many_attempts", "Bạn đã nhập sai quá nhiều lần. Vui lòng thử lại sau " + minutes + " phút.", headers)
  }

  const user = ctx.db.prepare("SELECT * FROM users WHERE username = ?").get(username)
  if (!user || !verifyPassword(password, user.password_hash)) {
    recordFailedLogin(ctx.failedLogins, username, ctx.now)
    throw new HttpError(401, "invalid_credentials", "Mã sinh viên hoặc mật khẩu không đúng")
  }

  clearFailedLogins(ctx.failedLogins, username)
  const session = createSession(ctx.db, user.id, ctx.now)
  return { token: session.token, expiresAt: session.expiresAt, profile: profileOf(ctx.db, user) }
}

// ------------------------------------------------------------------ hàm hỗ trợ

/**
 * So khớp đường dẫn với mẫu.
 * matchPath("/api/v1/courses/:id", "/api/v1/courses/web") → { id: "web" }
 * matchPath("/api/v1/courses/:id", "/api/v1/me")          → null (không khớp)
 */
function matchPath(pattern, path) {
  const patternParts = pattern.split("/")
  const pathParts = path.split("/")
  if (patternParts.length !== pathParts.length) return null

  const params = {}
  for (let i = 0; i < patternParts.length; i++) {
    if (patternParts[i].startsWith(":")) {
      const name = patternParts[i].substring(1)
      params[name] = pathParts[i]
    } else if (patternParts[i] !== pathParts[i]) {
      return null
    }
  }
  return params
}

/** "%20" → " ". Chuỗi % sai (VD "%E0%A4%A") → lỗi 400 thay vì làm sập request. */
function decodeParam(value) {
  try {
    return decodeURIComponent(value)
  } catch (error) {
    throw new HttpError(400, "invalid_path", "Đường dẫn chứa ký tự không hợp lệ")
  }
}

/** Gửi response. body là chữ → text/plain, còn lại → JSON. */
function send(res, status, body, extraHeaders) {
  const isText = typeof body === "string"
  const headers = {
    "Content-Type": isText ? "text/plain; charset=utf-8" : "application/json; charset=utf-8",
    "Access-Control-Allow-Origin": "*",
    "Access-Control-Allow-Headers": "Authorization, Content-Type",
    "Access-Control-Allow-Methods": "GET, POST, DELETE, OPTIONS",
    "Cache-Control": "no-store",
  }
  // Header riêng (VD Content-Type của CSV) ghi đè header mặc định.
  if (extraHeaders) {
    for (const key in extraHeaders) headers[key] = extraHeaders[key]
  }
  res.writeHead(status, headers)
  res.end(isText ? body : JSON.stringify(body))
}

/**
 * Đọc body JSON của request. Node nhận body thành từng mảnh ("data"), hết thì báo "end",
 * nên phải gói trong Promise để dùng được `await`.
 */
function readJson(req) {
  return new Promise((resolve, reject) => {
    let size = 0
    const chunks = []
    req.on("data", (chunk) => {
      size += chunk.length
      if (size > MAX_BODY_BYTES) {
        reject(new HttpError(413, "payload_too_large", "Request body quá lớn"))
        req.destroy()
      } else {
        chunks.push(chunk)
      }
    })
    req.on("end", () => {
      if (chunks.length === 0) {
        resolve(null)
        return
      }
      try {
        resolve(JSON.parse(Buffer.concat(chunks).toString("utf8")))
      } catch (error) {
        reject(new HttpError(400, "invalid_json", "Body không phải JSON hợp lệ"))
      }
    })
    req.on("error", reject)
  })
}

// ------------------------------------------------------------------ tạo app

/**
 * Tạo hàm xử lý request cho http.createServer.
 * options.clock: hàm trả về giờ hiện tại (test truyền đồng hồ giả vào).
 * options.allowReset: bật API /api/v1/dev/reset.
 * options.log: hàm ghi log mỗi request.
 */
export function createApp(db, options = {}) {
  const clock = options.clock || Date.now
  const log = options.log || function () {}
  const routes = makeRoutes(options.allowReset === true)
  const failedLogins = new Map() // đếm số lần đăng nhập sai, xem auth.js

  return async function handle(req, res) {
    const startedAt = Date.now()
    const url = new URL(req.url, "http://localhost")
    let status = 500

    try {
      // Trình duyệt hỏi trước khi gọi API (CORS) → trả lời "được".
      if (req.method === "OPTIONS") {
        status = 204
        send(res, 204, "")
        return
      }

      // 1. Tìm API có đường dẫn khớp.
      let foundPath = false
      let route = null
      let params = null
      for (const r of routes) {
        const match = matchPath(r.path, url.pathname)
        if (match === null) continue
        foundPath = true
        if (r.method === req.method) {
          route = r
          params = match
          break
        }
      }
      if (!foundPath) throw new HttpError(404, "not_found", "Không có endpoint " + url.pathname)
      if (route === null) throw new HttpError(405, "method_not_allowed", req.method + " không được hỗ trợ cho " + url.pathname)

      // 2. Kiểm tra đăng nhập.
      const now = clock()
      let user = null
      if (route.auth) {
        user = authenticate(db, req.headers.authorization, now)
        if (!user) throw new HttpError(401, "unauthorized", "Phiên đăng nhập không hợp lệ hoặc đã hết hạn")
      }

      // 3. Giải mã tham số trên đường dẫn và đọc body.
      for (const name in params) params[name] = decodeParam(params[name])
      let body = null
      if (req.method === "POST") body = await readJson(req)

      // 4. Chạy API.
      const result = await route.run({ db, user, params, query: url.searchParams, body, now, failedLogins })

      // API muốn tự chọn status/header (VD tải file CSV)
      if (result && typeof result.status === "number" && result.headers && "body" in result) {
        status = result.status
        send(res, result.status, result.body, result.headers)
        return
      }
      status = 200
      send(res, 200, result)
    } catch (error) {
      if (error instanceof HttpError) {
        status = error.status
        send(res, error.status, { error: { code: error.code, message: error.message } }, error.headers)
        return
      }
      console.error(error)
      send(res, 500, { error: { code: "internal_error", message: "Lỗi máy chủ" } })
    } finally {
      log(req.method + " " + url.pathname + " → " + status + " (" + (Date.now() - startedAt) + " ms)")
    }
  }
}
