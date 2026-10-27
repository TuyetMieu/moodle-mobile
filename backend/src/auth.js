import { randomBytes, scryptSync, timingSafeEqual } from "node:crypto"

const TOKEN_TTL_MS = 30 * 24 * 60 * 60 * 1000

export function hashPassword(password) {
  const salt = randomBytes(16).toString("hex")
  const hash = scryptSync(password, salt, 32).toString("hex")
  return `${salt}:${hash}`
}

export function verifyPassword(password, stored) {
  const [salt, hash] = stored.split(":")
  if (!salt || !hash) return false
  const expected = Buffer.from(hash, "hex")
  const actual = scryptSync(password, salt, expected.length)
  return timingSafeEqual(expected, actual)
}

export function createSession(db, userId, now = Date.now()) {
  // Dọn phiên đã hết hạn để bảng sessions không phình mãi.
  db.prepare("DELETE FROM sessions WHERE expires_at <= ?").run(now)
  const token = randomBytes(32).toString("base64url")
  db.prepare("INSERT INTO sessions (token, user_id, created_at, expires_at) VALUES (?, ?, ?, ?)")
    .run(token, userId, now, now + TOKEN_TTL_MS)
  return { token, expiresAt: now + TOKEN_TTL_MS }
}

export function deleteSession(db, token) {
  db.prepare("DELETE FROM sessions WHERE token = ?").run(token)
}

// ------------------------------------------------------------------ chặn dò mật khẩu
// Đếm số lần đăng nhập sai của từng mã sinh viên. Sai 5 lần trong 15 phút → khoá tới hết 15 phút.
// failedLogins là một Map: "ba22-041" → { count: 2, firstAt: <thời điểm sai lần đầu> }.
// Dùng Map (không dùng object {}) vì khoá là chữ người dùng gõ vào, VD "__proto__" sẽ phá object thường.

const MAX_FAILED_LOGINS = 5
const LOCK_WINDOW_MS = 15 * 60 * 1000

/** Còn bị khoá bao nhiêu ms (0 = được phép đăng nhập). */
export function lockedForMs(failedLogins, username, now) {
  const entry = failedLogins.get(username)
  if (!entry) return 0
  if (now - entry.firstAt >= LOCK_WINDOW_MS) {
    failedLogins.delete(username) // đã qua 15 phút → xoá, đếm lại từ đầu
    return 0
  }
  if (entry.count < MAX_FAILED_LOGINS) return 0
  return entry.firstAt + LOCK_WINDOW_MS - now
}

export function recordFailedLogin(failedLogins, username, now) {
  const entry = failedLogins.get(username)
  if (!entry || now - entry.firstAt >= LOCK_WINDOW_MS) {
    failedLogins.set(username, { count: 1, firstAt: now })
  } else {
    entry.count = entry.count + 1
  }
}

export function clearFailedLogins(failedLogins, username) {
  failedLogins.delete(username)
}

/** Trả về user gắn với header `Authorization: Bearer <token>`, hoặc null. */
export function authenticate(db, authorization, now = Date.now()) {
  const match = /^Bearer\s+(.+)$/i.exec(authorization ?? "")
  if (!match) return null
  const row = db.prepare(`
    SELECT u.*, s.token FROM sessions s JOIN users u ON u.id = s.user_id
    WHERE s.token = ? AND s.expires_at > ?
  `).get(match[1], now)
  return row ?? null
}
