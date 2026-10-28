import { DatabaseSync } from "node:sqlite"
import { mkdirSync } from "node:fs"
import { dirname } from "node:path"
import { seed } from "./seed.js"

/** Tăng số này mỗi khi đổi schema: DB cũ sẽ được xoá và seed lại (dữ liệu chỉ là dữ liệu mẫu). */
const SCHEMA_VERSION = 2

const SCHEMA = `
CREATE TABLE IF NOT EXISTS meta (
  key   TEXT PRIMARY KEY,
  value TEXT NOT NULL
);

-- Ngành học. Sinh viên và giáo viên đều thuộc một ngành.
CREATE TABLE IF NOT EXISTS programs (
  code TEXT PRIMARY KEY,
  name TEXT NOT NULL
);

CREATE TABLE IF NOT EXISTS users (
  id                INTEGER PRIMARY KEY,
  username          TEXT NOT NULL UNIQUE,
  password_hash     TEXT NOT NULL,
  role              TEXT NOT NULL CHECK (role IN ('STUDENT', 'TEACHER')),
  full_name         TEXT NOT NULL,
  short_name        TEXT NOT NULL,
  initials          TEXT NOT NULL,
  member_code       TEXT NOT NULL,              -- mã sinh viên hoặc mã cán bộ
  program_code      TEXT NOT NULL REFERENCES programs(code),
  cohort            TEXT NOT NULL,
  credits_earned    INTEGER NOT NULL DEFAULT 0,
  credits_milestone INTEGER NOT NULL DEFAULT 0
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
  id            TEXT PRIMARY KEY,
  semester_id   TEXT NOT NULL REFERENCES semesters(id),
  teacher_id    INTEGER NOT NULL REFERENCES users(id),
  title         TEXT NOT NULL,
  short_title   TEXT NOT NULL,
  code          TEXT NOT NULL,
  image         TEXT NOT NULL,
  credits       INTEGER NOT NULL,
  tone          TEXT NOT NULL,
  summary       TEXT NOT NULL,
  bonus_enabled INTEGER NOT NULL DEFAULT 0,    -- giáo viên có cho điểm bonus không
  bonus_max     REAL NOT NULL DEFAULT 0        -- bonus tối đa cộng vào điểm môn (thang 20)
);

-- Thành phần điểm của từng môn (Attendance, Midterm, Final...), do giáo viên đặt. Tổng weight = 100.
CREATE TABLE IF NOT EXISTS grade_components (
  id        TEXT PRIMARY KEY,
  course_id TEXT NOT NULL REFERENCES courses(id) ON DELETE CASCADE,
  name      TEXT NOT NULL,
  weight    INTEGER NOT NULL CHECK (weight BETWEEN 1 AND 100),
  position  INTEGER NOT NULL
);

CREATE TABLE IF NOT EXISTS enrollments (
  user_id   INTEGER NOT NULL REFERENCES users(id) ON DELETE CASCADE,
  course_id TEXT NOT NULL REFERENCES courses(id) ON DELETE CASCADE,
  PRIMARY KEY (user_id, course_id)
);

-- Bài tập thuộc một thành phần điểm, hoặc là bài bonus (component_id NULL, is_bonus = 1).
-- weight: trọng số tương đối trong thành phần (VD proposal 25, final demo 75).
CREATE TABLE IF NOT EXISTS assignments (
  id           TEXT PRIMARY KEY,
  course_id    TEXT NOT NULL REFERENCES courses(id) ON DELETE CASCADE,
  component_id TEXT REFERENCES grade_components(id),
  is_bonus     INTEGER NOT NULL DEFAULT 0,
  title        TEXT NOT NULL,
  description  TEXT NOT NULL,
  weight       REAL NOT NULL DEFAULT 1 CHECK (weight > 0),
  due_at       INTEGER NOT NULL,
  created_at   INTEGER NOT NULL,
  updated_at   INTEGER NOT NULL,
  CHECK ((is_bonus = 1 AND component_id IS NULL) OR (is_bonus = 0 AND component_id IS NOT NULL))
);

-- Điểm thang 20. graded_by: 'AUTO' (máy chấm) hoặc 'TEACHER' (dữ liệu cũ đã chấm tay).
CREATE TABLE IF NOT EXISTS submissions (
  user_id       INTEGER NOT NULL REFERENCES users(id) ON DELETE CASCADE,
  assignment_id TEXT NOT NULL REFERENCES assignments(id) ON DELETE CASCADE,
  completed_at  INTEGER NOT NULL,
  score         REAL CHECK (score IS NULL OR score BETWEEN 0 AND 20),
  feedback      TEXT,
  graded_at     INTEGER,
  graded_by     TEXT CHECK (graded_by IS NULL OR graded_by IN ('AUTO', 'TEACHER')),
  PRIMARY KEY (user_id, assignment_id)
);

CREATE TABLE IF NOT EXISTS class_sessions (
  id         TEXT PRIMARY KEY,
  course_id  TEXT NOT NULL REFERENCES courses(id) ON DELETE CASCADE,
  weekday    INTEGER NOT NULL CHECK (weekday BETWEEN 0 AND 6),
  start_time TEXT NOT NULL,
  end_time   TEXT NOT NULL,
  kind       TEXT NOT NULL CHECK (kind IN ('LECTURE', 'LAB')),
  room       TEXT NOT NULL
);

-- Bảng tin của lớp (hiện ở tab Stream).
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

-- Thông báo riêng cho từng sinh viên. data: JSON tham số để app tự dịch câu chữ (VD điểm, deadline cũ/mới).
CREATE TABLE IF NOT EXISTS notifications (
  id            TEXT PRIMARY KEY,
  user_id       INTEGER NOT NULL REFERENCES users(id) ON DELETE CASCADE,
  kind          TEXT NOT NULL CHECK (kind IN ('GRADE', 'ANNOUNCEMENT', 'ASSIGNMENT_NEW', 'DEADLINE_CHANGED')),
  title         TEXT NOT NULL,
  body          TEXT NOT NULL,
  created_at    INTEGER NOT NULL,
  read_at       INTEGER,
  assignment_id TEXT REFERENCES assignments(id) ON DELETE SET NULL,
  course_id     TEXT REFERENCES courses(id) ON DELETE SET NULL,
  data          TEXT
);

-- Tin gửi cho cả ngành (audience = PROGRAM) hoặc toàn trường (audience = ALL).
CREATE TABLE IF NOT EXISTS bulletins (
  id           TEXT PRIMARY KEY,
  audience     TEXT NOT NULL CHECK (audience IN ('PROGRAM', 'ALL')),
  program_code TEXT REFERENCES programs(code),
  kind         TEXT NOT NULL CHECK (kind IN ('EVENT', 'NOTICE')),
  title        TEXT NOT NULL,
  body         TEXT NOT NULL,
  author_id    INTEGER REFERENCES users(id) ON DELETE SET NULL,
  author_name  TEXT NOT NULL,
  event_at     INTEGER,
  location     TEXT,
  created_at   INTEGER NOT NULL,
  CHECK ((audience = 'PROGRAM') = (program_code IS NOT NULL))
);

CREATE TABLE IF NOT EXISTS bulletin_reads (
  user_id     INTEGER NOT NULL REFERENCES users(id) ON DELETE CASCADE,
  bulletin_id TEXT NOT NULL REFERENCES bulletins(id) ON DELETE CASCADE,
  read_at     INTEGER NOT NULL,
  PRIMARY KEY (user_id, bulletin_id)
);

CREATE INDEX IF NOT EXISTS idx_assignments_course ON assignments(course_id);
CREATE INDEX IF NOT EXISTS idx_components_course ON grade_components(course_id);
CREATE INDEX IF NOT EXISTS idx_notifications_user ON notifications(user_id);
CREATE INDEX IF NOT EXISTS idx_resources_course ON course_resources(course_id);
CREATE INDEX IF NOT EXISTS idx_bulletins_audience ON bulletins(audience, program_code);
`

/** Thứ tự xoá: bảng con trước, bảng cha sau. Gồm cả bảng của schema cũ. */
const TABLES = [
  "bulletin_reads", "bulletins", "notifications", "course_resources", "announcements", "class_sessions",
  "submissions", "assignments", "enrollments", "grade_components", "courses", "semesters", "sessions",
  "users", "programs", "meta",
]

export function openDatabase(file) {
  if (file !== ":memory:") mkdirSync(dirname(file), { recursive: true })
  const db = new DatabaseSync(file)
  db.exec("PRAGMA foreign_keys = ON; PRAGMA journal_mode = WAL;")

  // Schema cũ (khác phiên bản) → xoá hết bảng, tạo lại. Server sẽ tự seed vì meta trống.
  const version = db.prepare("PRAGMA user_version").get().user_version
  if (version !== SCHEMA_VERSION) {
    db.exec("PRAGMA foreign_keys = OFF;")
    for (const table of TABLES) db.exec(`DROP TABLE IF EXISTS ${table}`)
    db.exec("PRAGMA foreign_keys = ON;")
    db.exec(`PRAGMA user_version = ${SCHEMA_VERSION}`)
  }
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

/** DB đang trong transaction — gọi lồng nhau thì chạy luôn trong transaction ngoài (SQLite không hỗ trợ BEGIN lồng). */
const openTransactions = new WeakSet()

export function transaction(db, work) {
  if (openTransactions.has(db)) return work()
  openTransactions.add(db)
  try {
    return runTransaction(db, work)
  } finally {
    openTransactions.delete(db)
  }
}

function runTransaction(db, work) {
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
