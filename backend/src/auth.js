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
  const token = randomBytes(32).toString("base64url")
  db.prepare("INSERT INTO sessions (token, user_id, created_at, expires_at) VALUES (?, ?, ?, ?)")
    .run(token, userId, now, now + TOKEN_TTL_MS)
  return { token, expiresAt: now + TOKEN_TTL_MS }
}

export function deleteSession(db, token) {
  db.prepare("DELETE FROM sessions WHERE token = ?").run(token)
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
