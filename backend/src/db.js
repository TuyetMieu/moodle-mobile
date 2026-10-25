import { DatabaseSync } from "node:sqlite"
import { mkdirSync } from "node:fs"
import { dirname } from "node:path"
import { seed } from "./seed.js"

const SCHEMA = `
CREATE TABLE IF NOT EXISTS meta (
  key   TEXT PRIMARY KEY,
  value TEXT NOT NULL
);

CREATE TABLE IF NOT EXISTS users (
  id                INTEGER PRIMARY KEY,
  username          TEXT NOT NULL UNIQUE,
  password_hash     TEXT NOT NULL,
  full_name         TEXT NOT NULL,
  short_name        TEXT NOT NULL,
  initials          TEXT NOT NULL,
  student_id        TEXT NOT NULL,
  program           TEXT NOT NULL,
  cohort            TEXT NOT NULL,
  credits_earned    INTEGER NOT NULL,
  credits_milestone INTEGER NOT NULL
);

CREATE TABLE IF NOT EXISTS sessions (
  token      TEXT PRIMARY KEY,
  user_id    INTEGER NOT NULL REFERENCES users(id) ON DELETE CASCADE,
  created_at INTEGER NOT NULL,
  expires_at INTEGER NOT NULL
);

CREATE TABLE IF NOT EXISTS semesters (
  id            TEXT PRIMARY KEY,
  label         TEXT NOT NULL,
  academic_year TEXT NOT NULL,
  starts_at     INTEGER NOT NULL,
  ends_at       INTEGER NOT NULL
);

CREATE TABLE IF NOT EXISTS courses (
  id               TEXT PRIMARY KEY,
  semester_id      TEXT NOT NULL REFERENCES semesters(id),
  title            TEXT NOT NULL,
  short_title      TEXT NOT NULL,
  code             TEXT NOT NULL,
  teacher          TEXT NOT NULL,
  teacher_initials TEXT NOT NULL,
  image            TEXT NOT NULL,
  credits          INTEGER NOT NULL,
  tone             TEXT NOT NULL,
  summary          TEXT NOT NULL
);

CREATE TABLE IF NOT EXISTS enrollments (
  user_id   INTEGER NOT NULL REFERENCES users(id) ON DELETE CASCADE,
  course_id TEXT NOT NULL REFERENCES courses(id) ON DELETE CASCADE,
  PRIMARY KEY (user_id, course_id)
);

CREATE TABLE IF NOT EXISTS assignments (
  id           TEXT PRIMARY KEY,
  course_id    TEXT NOT NULL REFERENCES courses(id) ON DELETE CASCADE,
  title        TEXT NOT NULL,
  category     TEXT NOT NULL CHECK (category IN ('ATTENDANCE', 'MIDTERM_LABS', 'FINAL_PROJECT')),
  contribution INTEGER NOT NULL,
  description  TEXT NOT NULL,
  due_at       INTEGER NOT NULL
);

CREATE TABLE IF NOT EXISTS submissions (
  user_id       INTEGER NOT NULL REFERENCES users(id) ON DELETE CASCADE,
  assignment_id TEXT NOT NULL REFERENCES assignments(id) ON DELETE CASCADE,
  completed_at  INTEGER NOT NULL,
  score         REAL,
  feedback      TEXT,
  PRIMARY KEY (user_id, assignment_id)
);

CREATE TABLE IF NOT EXISTS class_sessions (
  id         TEXT PRIMARY KEY,
  course_id  TEXT NOT NULL REFERENCES courses(id) ON DELETE CASCADE,
  weekday    INTEGER NOT NULL CHECK (weekday BETWEEN 0 AND 6),
  start_time TEXT NOT NULL,
  end_time   TEXT NOT NULL,
  kind       TEXT NOT NULL,
  room       TEXT NOT NULL
);

CREATE TABLE IF NOT EXISTS announcements (
  id        TEXT PRIMARY KEY,
  course_id TEXT NOT NULL REFERENCES courses(id) ON DELETE CASCADE,
  kind      TEXT NOT NULL CHECK (kind IN ('ANNOUNCEMENT', 'RESOURCE')),
  title     TEXT NOT NULL,
  body      TEXT NOT NULL,
  posted_at INTEGER NOT NULL,
  pinned    INTEGER NOT NULL DEFAULT 0
);

CREATE TABLE IF NOT EXISTS course_resources (
  id           TEXT PRIMARY KEY,
  course_id    TEXT NOT NULL REFERENCES courses(id) ON DELETE CASCADE,
  week         INTEGER NOT NULL,
  title        TEXT NOT NULL,
  kind         TEXT NOT NULL CHECK (kind IN ('SLIDES', 'NOTES', 'EXERCISE', 'DATASET')),
  file_type    TEXT NOT NULL,
  size_kb      INTEGER NOT NULL,
  published_at INTEGER NOT NULL
);

CREATE TABLE IF NOT EXISTS notifications (
  id            TEXT PRIMARY KEY,
  user_id       INTEGER NOT NULL REFERENCES users(id) ON DELETE CASCADE,
  kind          TEXT NOT NULL CHECK (kind IN ('GRADE', 'MESSAGE')),
  title         TEXT NOT NULL,
  body          TEXT NOT NULL,
  created_at    INTEGER NOT NULL,
  read_at       INTEGER,
  assignment_id TEXT REFERENCES assignments(id) ON DELETE SET NULL,
  course_id     TEXT REFERENCES courses(id) ON DELETE SET NULL
);

CREATE INDEX IF NOT EXISTS idx_assignments_course ON assignments(course_id);
CREATE INDEX IF NOT EXISTS idx_notifications_user ON notifications(user_id);
CREATE INDEX IF NOT EXISTS idx_resources_course ON course_resources(course_id);
`

const TABLES = [
  "notifications", "course_resources", "announcements", "class_sessions", "submissions", "assignments",
  "enrollments", "courses", "semesters", "sessions", "users", "meta",
]

export function openDatabase(file) {
  if (file !== ":memory:") mkdirSync(dirname(file), { recursive: true })
  const db = new DatabaseSync(file)
  db.exec("PRAGMA foreign_keys = ON; PRAGMA journal_mode = WAL;")
  db.exec(SCHEMA)
  return db
}

export function isSeeded(db) {
  return Boolean(db.prepare("SELECT value FROM meta WHERE key = 'seeded_at'").get())
}

export function seededAt(db) {
  const row = db.prepare("SELECT value FROM meta WHERE key = 'seeded_at'").get()
  return row ? Number(row.value) : null
}

/** Xoá toàn bộ dữ liệu và seed lại; mốc deadline được tính tương đối so với `now`. */
export function resetDatabase(db, now = Date.now()) {
  transaction(db, () => {
    for (const table of TABLES) db.exec(`DELETE FROM ${table}`)
    seed(db, now)
    db.prepare("INSERT INTO meta (key, value) VALUES ('seeded_at', ?)").run(String(now))
  })
}

export function transaction(db, work) {
  db.exec("BEGIN")
  try {
    const result = work()
    db.exec("COMMIT")
    return result
  } catch (error) {
    db.exec("ROLLBACK")
    throw error
  }
}
