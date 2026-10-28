# Backend Moodle USTH — Giải thích code & tài liệu API

Backend nằm trong `backend/`: Node.js ≥ 22.13, chỉ dùng module có sẵn (`node:http`, `node:sqlite`, `node:crypto`), **không cần `npm install`**.
Tài liệu này giải thích từng file và liệt kê **toàn bộ endpoint** kèm request/response. Cách chạy xem `backend/README.md`, schema bảng xem `CHUC_NANG_VA_CSDL.md`.

> **Lưu ý (v2):** tài liệu này mô tả phiên bản đầu (thang 10, thành phần điểm cố định, chỉ có sinh viên).
> Từ v2: thang 20, thành phần điểm do giáo viên đặt, bonus, máy chấm, vai trò giáo viên và thông báo 2 luồng —
> xem `docs/NANG_CAP_V2.md` và bảng endpoint trong `backend/README.md` (nguồn cập nhật nhất).

---

## 1. Cấu trúc & luồng một request

```
backend/
├── src/server.js     khởi động HTTP server, seed lần đầu, in URL
├── src/app.js        bảng route, middleware xác thực, parse JSON, xử lý lỗi, log
├── src/services.js   toàn bộ nghiệp vụ + truy vấn SQL (trả về object JSON)
├── src/auth.js       băm mật khẩu (scrypt), tạo/xoá/kiểm tra token phiên
├── src/db.js         schema SQLite, reset, transaction
├── src/seed.js       dữ liệu mô phỏng
├── scripts/export-sample.js   xuất bootstrap ra assets của app (chế độ xem thử)
└── test/api.test.js  16 test tích hợp
```

```
HTTP request
   │
   ▼
server.js  createServer(app)
   │
   ▼
app.js handle(req, res)
   ├─ OPTIONS ............................. 204 (CORS preflight)
   ├─ khớp path với bảng route ............ không khớp → 404 not_found
   ├─ khớp method ......................... không khớp → 405 method_not_allowed
   ├─ route.auth ? authenticate() ......... không có/hết hạn → 401 unauthorized
   ├─ params từ :param (decodeURIComponent)
   ├─ body = POST ? readJson() : null ..... >64 KB → 413, sai JSON → 400
   ├─ handler(ctx) → services.js → SQLite
   └─ send JSON 200  |  HttpError → {error:{code,message}}  |  lỗi khác → 500
```

`ctx` truyền cho handler: `{ db, user, params, query, body, now }`. Handler **không bao giờ gọi `Date.now()`**; `now` đến từ `clock()` được tiêm vào `createApp`, nên test dựng đồng hồ giả được.

---

## 2. Giải thích từng file

### 2.1. `src/server.js`

| Bước | Chi tiết |
|---|---|
| Cấu hình | `PORT` (8080), `HOST` (`0.0.0.0`), `DB_FILE` (mặc định `backend/data/moodle-usth.db`), `ALLOW_RESET = NODE_ENV !== "production"` |
| Seed | Nếu có `--reset`, `--reset-only` hoặc DB chưa có `meta.seeded_at` → `resetDatabase(db)`. `--reset-only` seed xong thì thoát (`npm run reset`). |
| Cảnh báo | Dữ liệu seed > 1 ngày → in cảnh báo vì deadline là *tương đối* so với lúc seed. |
| Lắng nghe | In URL localhost, emulator `10.0.2.2`, và các IPv4 LAN (cho điện thoại thật), cùng tài khoản demo. |
| Tắt máy | `SIGINT`/`SIGTERM` → `db.close()`. |

### 2.2. `src/app.js`

**Danh sách API.** `makeRoutes(allowReset)` trả về mảng object `{ method, path, auth, run(ctx) }`. `path` có thể chứa tham số `:id`. `auth` là `true` với mọi API trừ `/health`, `/auth/login` và `/dev/reset`.

**Chọn API.** Vòng `for` qua danh sách, `matchPath(pattern, path)` tách cả hai theo `"/"` rồi so từng đoạn (đoạn bắt đầu bằng `:` là tham số) — không dùng regex. Có path khớp nhưng sai method → 405; không path nào khớp → 404.

**`readJson(req)`** gom chunk, đếm byte, vượt `MAX_BODY_BYTES = 64 KB` thì reject 413 và `req.destroy()`. Body rỗng → `null`; body không phải JSON → 400 `invalid_json`. Chỉ được gọi cho method `POST` (DELETE/GET không đọc body).

**`send(res, status, body, headers)`**: `string` → `text/plain`, còn lại → `application/json; charset=utf-8`. Luôn kèm `Access-Control-Allow-Origin: *`, `Access-Control-Allow-Headers: Authorization, Content-Type`, `Access-Control-Allow-Methods: GET, POST, DELETE, OPTIONS`, `Cache-Control: no-store`. Header truyền thêm ghi đè mặc định (đó là cách CSV đặt `text/csv`).

**Response tuỳ biến.** Handler trả `{ status, headers, body }` (đủ cả `status` kiểu số, `body` và `headers`) thì `app.js` dùng nguyên văn; mọi giá trị khác được trả như JSON 200. Hiện chỉ route CSV dùng.

**Log** mỗi request ở `finally`: `GET /api/v1/bootstrap → 200 (7 ms)`.

**Tham số đường dẫn** được giải mã bằng `decodeParam()`: chuỗi `%` sai (VD `/courses/%E0%A4%A`) → `400 invalid_path` thay vì để `decodeURIComponent` ném lỗi thành 500.

**Tuỳ chọn của `createApp(db, opts)`**: `clock` (đồng hồ, test tiêm đồng hồ giả), `allowReset` (bật `/dev/reset`), `log`. Mỗi lần gọi `createApp` tạo một `Map` `failedLogins` riêng để đếm lần đăng nhập sai, truyền vào API qua `ctx`. `HttpError` có thể mang `headers` (VD `Retry-After` của lỗi 429) và `app.js` gửi kèm chúng.

### 2.3. `src/auth.js`

| Hàm | Mô tả |
|---|---|
| `hashPassword(pw)` | salt 16 byte ngẫu nhiên (hex) + `scryptSync(pw, salt, 32)`; lưu `"salt:hash"` |
| `verifyPassword(pw, stored)` | tách `salt:hash`, băm lại cùng độ dài, so bằng `timingSafeEqual` |
| `createSession(db, userId, now)` | xoá các phiên đã hết hạn (`expires_at <= now`), rồi tạo token = `randomBytes(32).toString("base64url")`, ghi `sessions`, hạn `now + 30 ngày` |
| `lockedForMs(failedLogins, username, now)` / `recordFailedLogin(...)` / `clearFailedLogins(...)` | Chặn dò mật khẩu. `failedLogins` là `Map` username → `{ count, firstAt }` (dùng `Map` vì khoá là chữ người dùng gõ; object `{}` thường có thể bị phá bởi `"__proto__"`). Sai 5 lần trong 15 phút → `lockedForMs` trả số ms còn bị khoá; đăng nhập đúng → `clearFailedLogins` |
| `deleteSession(db, token)` | xoá token (logout) |
| `authenticate(db, header, now)` | regex `^Bearer\s+(.+)$` (không phân biệt hoa/thường chữ "Bearer"), truy vấn `sessions ⨝ users WHERE token=? AND expires_at > ?`; trả về hàng `users` kèm trường `token`, hoặc `null` |

### 2.4. `src/db.js`

- `openDatabase(file)`: tạo thư mục chứa DB nếu cần (trừ `:memory:`), `new DatabaseSync`, bật `PRAGMA foreign_keys = ON; journal_mode = WAL`, chạy `SCHEMA` (12 bảng + 3 index, đều `IF NOT EXISTS`).
- `isSeeded` / `seededAt`: đọc `meta.seeded_at`.
- `resetDatabase(db, now)`: trong **một transaction** xoá mọi bảng theo thứ tự con→cha (`TABLES`), gọi `seed(db, now)`, ghi `seeded_at`. Vì xoá cả `sessions`, mọi token cũ **mất hiệu lực** sau reset.
- `transaction(db, work)`: `BEGIN` → `work()` → `COMMIT`, lỗi thì `ROLLBACK` rồi ném lại.

### 2.5. `src/services.js` — nghiệp vụ

**Hằng số:** `DAY`, `RECENT_DONE_WINDOW = 14 ngày`, `CATEGORIES` (`ATTENDANCE` 10, `MIDTERM_LABS` 30, `FINAL_PROJECT` 60). `HttpError(status, code, message)` là lỗi mà `app.js` biến thành response.

**Trạng thái bài** `assignmentStatus`: `DONE` (đã đánh dấu) → `OVERDUE` (`dueAt ≤ now`) → `URGENT` (còn ≤ 24h) → `UPCOMING`. Được tính **tại thời điểm gọi** từ `now`, không lưu trong DB.

**Truy vấn gốc `ASSIGNMENT_SELECT`:**

```sql
FROM assignments a
JOIN courses c       ON c.id = a.course_id
JOIN enrollments e   ON e.course_id = a.course_id AND e.user_id = :userId   -- chỉ môn đã đăng ký
LEFT JOIN submissions s ON s.assignment_id = a.id AND s.user_id = :userId   -- bài đã xong/điểm của riêng user
```

Đây là cơ chế *cách ly dữ liệu*: không có cột "owner" ở bảng bài, mà lọc qua `enrollments` và `submissions`.

| Hàm | Việc làm |
|---|---|
| `profileOf` | Hồ sơ + học kỳ hiện tại (`semesters` có `starts_at` lớn nhất) |
| `listAssignments(db, userId, now, {courseId, filter})` | Lấy bài (có thể theo môn), map sang `toAssignment`, rồi lọc `done` / `urgent` / `todo`. `filter` sai → 400 `invalid_filter`. |
| `getAssignment` | 404 `assignment_not_found` nếu không tồn tại **hoặc** không thuộc môn user đăng ký |
| `markCompleted` | `INSERT INTO submissions` nếu chưa xong; **idempotent** (gọi lại vẫn 200) |
| `unmarkCompleted` | Có `score` → 409 `already_graded`; ngược lại `DELETE`. Gọi trên bài chưa xong vẫn trả 200 (không làm gì). |
| `listCourses` / `getCourse` | Môn đã đăng ký kèm `done/total/pending/progress/grade`; chi tiết thêm `gradeComponents`, `sessions`, `announcements`, `resources`. Môn không đăng ký → 404 `course_not_found`. |
| `gradeOf` | `Σ(score·contribution) / Σ(contribution)` trên bài đã chấm, làm tròn 1 chữ số; chưa có bài chấm → `null` |
| `gradeComponents` | Với từng thành phần: `score`, `gradedWeight`, `publishedWeight`, `status` (`NOT_GRADED` nếu `gradedWeight = 0`; `GRADED` nếu `gradedWeight ≥ weight` của thành phần; còn lại `IN_PROGRESS`) |
| `timetable` | Buổi học của các môn đã đăng ký + mốc đầu/cuối học kỳ |
| `listNotifications` | Thông báo của user, mới nhất trước, kèm `unread` |
| `markNotificationRead` | `UPDATE ... SET read_at = COALESCE(read_at, ?) WHERE id=? AND user_id=?`; không có hàng nào đổi → 404 `notification_not_found` (cũng là cách chặn đọc thông báo người khác). Trả về cả danh sách mới. |
| `learningSummary` | `pending`, `urgent`, `recentDone`, `done`, `total`, `progress` |
| `semesterOverview` | Tín chỉ, số môn, điểm TB **có trọng số theo tín chỉ** (bỏ môn chưa có điểm), tiến độ |
| `gradeReportCsv` | CSV UTF‑8 có BOM, escape `"`/`,`/xuống dòng, dòng cách nhau bằng `\r\n` |
| `bootstrap` | Gộp mọi thứ app cần trong một payload. Lấy danh sách bài **một lần** rồi chia theo môn để tính `done/total/grade/gradeComponents` (trước đây gọi lại `getCourse` cho từng môn) |

### 2.6. `src/seed.js`

Dữ liệu mô phỏng, **deadline tính theo giờ so với `now`** (âm = đã qua hạn):

- **Học kỳ**: suy từ tháng của `now` (`academicTerm`): tháng ≥ 9 → Semester 1; tháng 1 → Semester 1 của năm trước; còn lại → Semester 2. Bắt đầu 9 tuần trước đầu tuần hiện tại, kết thúc 9 tuần sau.
- **Người dùng**: `ba22-041` (Hoang An, 18/24 tín chỉ, 3 môn) và `ba22-087` (Tran Minh Chau, 15/24, 2 môn `web` + `earth`); mật khẩu `usth2025`.
- **Môn**: `web` (ICT 3103, 4 TC), `bio` (BIO 2201, 3 TC), `earth` (ESS 2102, 3 TC).
- **Bài tập**: 10 bài chính (`proposal` còn ~6h42m, `lab` 20h, `midterm` 72h…) + 19 buổi check-in (`web` 7, `bio` 6, `earth` 6), mỗi buổi 1%, hạn lùi dần từng tuần.
- **Đã chấm cho An**: `lab3` 9.0, `earth-lab1` 9.0, `earth-lab2` 9.5, `earth-quiz` 8.5 + điểm các buổi check-in. Chau có tiến độ khác (đã nộp `proposal`, chấm `lab3` 8.0…).
- **Lịch học**: 6 buổi/tuần (thứ 2–6). **Bảng tin**: mỗi môn 1 thông báo ghim + 1 thông báo tài liệu tuần 9. **Tài liệu**: 3–4 file/môn. **Thông báo**: 3 cho An (2 chưa đọc), 1 cho Chau.

---

## 3. Quy ước API

| Mục | Giá trị |
|---|---|
| Base URL | `http://localhost:8080` (emulator: `http://10.0.2.2:8080`) |
| Tiền tố | `/api/v1` (trừ `/health`) |
| Xác thực | Header `Authorization: Bearer <token>` (token lấy từ `/auth/login`, sống 30 ngày) |
| Content-Type | Request có body: `application/json`. Response: JSON UTF‑8 (CSV là `text/csv`) |
| Thời gian | **epoch milliseconds** (số nguyên) ở mọi trường `...At`, `serverTime`, mốc học kỳ |
| Điểm | `0–10`, làm tròn 1 chữ số; `null` nếu chưa có |
| Lỗi | `{ "error": { "code": "...", "message": "..." } }` |

### Bảng mã lỗi

| HTTP | `code` | Khi nào |
|---|---|---|
| 400 | `missing_credentials` | Thiếu username hoặc password khi login |
| 400 | `invalid_json` | Body POST không phải JSON hợp lệ |
| 400 | `invalid_filter` | `filter` không thuộc `todo`/`urgent`/`done` |
| 400 | `invalid_path` | Tham số đường dẫn chứa chuỗi `%` không giải mã được |
| 401 | `invalid_credentials` | Sai mã SV hoặc mật khẩu |
| 401 | `unauthorized` | Thiếu/sai/hết hạn token |
| 404 | `not_found` | Đường dẫn không tồn tại |
| 404 | `course_not_found` | Môn không tồn tại hoặc chưa đăng ký |
| 404 | `assignment_not_found` | Bài không tồn tại hoặc không thuộc môn đã đăng ký |
| 404 | `notification_not_found` | Thông báo không tồn tại hoặc của người khác |
| 405 | `method_not_allowed` | Đúng path, sai method |
| 409 | `already_graded` | Bỏ đánh dấu bài đã có điểm |
| 413 | `payload_too_large` | Body > 64 KB |
| 429 | `too_many_attempts` | Sai mật khẩu 5 lần trong 15 phút cho cùng mã SV; có header `Retry-After` (giây) |
| 500 | `internal_error` | Lỗi không lường trước |

---

## 4. Danh sách endpoint

Tóm tắt (🔒 = cần token):

| # | Method | Path | 🔒 | Mục đích |
|---|---|---|---|---|
| 1 | GET | `/health` | | Kiểm tra server |
| 2 | POST | `/api/v1/auth/login` | | Đăng nhập |
| 3 | POST | `/api/v1/auth/logout` | ✔ | Đăng xuất |
| 4 | GET | `/api/v1/me` | ✔ | Hồ sơ sinh viên |
| 5 | GET | `/api/v1/bootstrap` | ✔ | **Toàn bộ dữ liệu cho app** |
| 6 | GET | `/api/v1/summary` | ✔ | Số liệu học tập tổng hợp |
| 7 | GET | `/api/v1/semester` | ✔ | Tổng quan học kỳ |
| 8 | GET | `/api/v1/courses` | ✔ | Danh sách môn |
| 9 | GET | `/api/v1/courses/:id` | ✔ | Chi tiết môn |
| 10 | GET | `/api/v1/assignments` | ✔ | Danh sách bài (lọc được) |
| 11 | GET | `/api/v1/assignments/:id` | ✔ | Chi tiết bài |
| 12 | POST | `/api/v1/assignments/:id/completion` | ✔ | Đánh dấu hoàn thành |
| 13 | DELETE | `/api/v1/assignments/:id/completion` | ✔ | Bỏ đánh dấu |
| 14 | GET | `/api/v1/timetable` | ✔ | Thời khoá biểu |
| 15 | GET | `/api/v1/notifications` | ✔ | Thông báo |
| 16 | POST | `/api/v1/notifications/:id/read` | ✔ | Đánh dấu đã đọc |
| 17 | GET | `/api/v1/reports/grades.csv` | ✔ | Báo cáo điểm CSV |
| 18 | POST | `/api/v1/dev/reset` | | Seed lại (chỉ khi không production) |

Mọi endpoint có token còn có thể trả `401 unauthorized`; các ví dụ dưới đây không nhắc lại.

---

### 4.1. `GET /health`
Không cần token. Dùng để kiểm tra server sống và dữ liệu seed lúc nào.

```json
{ "status": "ok", "serverTime": 1790000000000, "seededAt": 1789990000000 }
```

### 4.2. `POST /api/v1/auth/login`
Body: `{ "username": "BA22-041", "password": "usth2025" }` (username được `trim().toLowerCase()`, nên không phân biệt hoa/thường).

Thành công `200`:
```json
{
  "token": "n3F...base64url",
  "expiresAt": 1792592000000,
  "profile": {
    "id": 1, "username": "ba22-041", "fullName": "Hoang An", "shortName": "An", "initials": "HA",
    "studentId": "BA22-041", "program": "Information & Communication Technology",
    "cohort": "USTH · Class of 2026", "academicYear": "2026–2027", "semester": "Semester 1"
  }
}
```
Lỗi: `400 missing_credentials`, `401 invalid_credentials` (cùng một thông báo cho sai user lẫn sai mật khẩu),
`429 too_many_attempts` khi đã sai 5 lần trong 15 phút: tài khoản bị khoá tới hết cửa sổ 15 phút **kể cả khi nhập đúng**, response có header `Retry-After: <giây>` và message dạng "…thử lại sau 12 phút." Đăng nhập đúng (khi chưa bị khoá) sẽ xoá bộ đếm. Bộ đếm nằm trong bộ nhớ nên restart server là mất.

### 4.3. `POST /api/v1/auth/logout`
Xoá token đang dùng. Trả `{ "ok": true }`. Dùng lại token đó sau đó → `401`.

### 4.4. `GET /api/v1/me`
Trả đúng object `profile` như ở login.

### 4.5. `GET /api/v1/bootstrap`
**Một lần gọi lấy mọi thứ app cần** (app gọi khi mở và khi kéo để làm mới).

```jsonc
{
  "serverTime": 1790000000000,
  "profile":   { /* như /me */ },
  "semester":  { /* như /semester */ },
  "summary":   { /* như /summary */ },
  "courses": [ {
      /* các trường như /courses (item) */
      "gradeComponents": [ /* xem 4.9 */ ],
      "announcements":   [ /* xem 4.9 */ ],
      "resources":       [ /* xem 4.9 */ ]
  } ],
  "assignments":   [ /* như /assignments → items */ ],
  "timetable":     { /* như /timetable */ },
  "notifications": { "items": [ ... ], "unread": 2 }
}
```
Lưu ý: `courses[]` trong bootstrap **không có** `sessions` (lịch học nằm ở `timetable`). `serverTime` giúp app dời mốc thời gian của dữ liệu mẫu.

### 4.6. `GET /api/v1/summary`
```json
{ "pending": 6, "urgent": 2, "recentDone": 5, "done": 23, "total": 29, "progress": 79 }
```
`urgent` = bài chưa xong có trạng thái `URGENT` hoặc `OVERDUE`; `recentDone` = bài hoàn thành trong 14 ngày gần nhất; `progress` = `done/total` làm tròn %.

### 4.7. `GET /api/v1/semester`
```json
{
  "label": "Semester 1", "academicYear": "2026–2027",
  "startsAt": 1784000000000, "endsAt": 1795000000000,
  "creditsEarned": 18, "creditsMilestone": 24,
  "enrolledCredits": 10, "coursesCount": 3,
  "averageGrade": 8.6,
  "completion": { "done": 23, "total": 29, "progress": 79 }
}
```
`averageGrade` là `null` nếu chưa môn nào có điểm. `creditsEarned/creditsMilestone` lấy thẳng từ bảng `users`.

### 4.8. `GET /api/v1/courses`
```json
{ "items": [ {
    "id": "web", "title": "Web Application Development", "shortTitle": "Web Development",
    "code": "ICT 3103", "teacher": "Dr. Nguyen Minh", "teacherInitials": "NM",
    "image": "development", "credits": 4, "tone": "blue", "summary": "Build modern, ...",
    "done": 8, "total": 10, "pending": 2, "progress": 80, "grade": 8.6
} ] }
```
`image`/`tone` chỉ là *tên*; app tự ánh xạ sang ảnh và màu cục bộ. `grade` là `null` khi chưa có bài nào được chấm.

### 4.9. `GET /api/v1/courses/:id`
Trả các trường như 4.8 **cộng thêm**:

```jsonc
{
  "gradeComponents": [
    { "key": "ATTENDANCE", "label": "Attendance", "weight": 10, "score": 8.1,
      "status": "IN_PROGRESS", "gradedWeight": 7, "publishedWeight": 7 }
    // MIDTERM_LABS (30), FINAL_PROJECT (60)
  ],
  "sessions": [
    { "id": "web-lecture", "courseId": "web", "weekday": 2, "start": "09:00", "end": "11:00",
      "kind": "Lý thuyết", "room": "A · 402" }
  ],
  "announcements": [
    { "id": "web-reminder", "courseId": "web", "kind": "ANNOUNCEMENT", "title": "A little reminder for this week",
      "body": "...", "author": "Dr. Nguyen Minh", "authorInitials": "NM", "postedAt": 1790000000000, "pinned": true }
  ],
  "resources": [
    { "id": "web-res-1", "courseId": "web", "week": 9, "title": "Week 09 · State management & data fetching",
      "kind": "SLIDES", "fileType": "PDF", "sizeKb": 2450, "publishedAt": 1789480000000 }
  ]
}
```
- `status` của thành phần: `NOT_GRADED` / `IN_PROGRESS` / `GRADED`.
- `weekday`: `0` = Chủ nhật … `6` = Thứ bảy; `start`/`end` là `"HH:mm"` giờ địa phương.
- `announcements` sắp xếp ghim trước rồi mới nhất trước; `resources` sắp xếp tuần giảm dần.
- `announcements[].kind`: `ANNOUNCEMENT` | `RESOURCE`. `resources[].kind`: `SLIDES` | `NOTES` | `EXERCISE` | `DATASET`.

Lỗi: `404 course_not_found`.

### 4.10. `GET /api/v1/assignments`
Query tuỳ chọn:

| Tham số | Giá trị | Ý nghĩa |
|---|---|---|
| `courseId` | id môn | Chỉ bài của môn đó (môn không đăng ký → danh sách rỗng, **không** 404) |
| `filter` | `todo` | Bài chưa xong (mọi trạng thái ≠ `DONE`), sắp theo hạn |
| | `urgent` | `URGENT` hoặc `OVERDUE` (chưa xong), sắp theo hạn |
| | `done` | Bài đã xong, **mới hoàn thành trước** |
| (không có) | | Tất cả, sắp theo `dueAt` tăng dần |

Ví dụ: `GET /api/v1/assignments?courseId=web&filter=todo`

```json
{ "items": [ {
    "id": "proposal", "courseId": "web",
    "courseTitle": "Web Application Development", "courseShortTitle": "Web Development",
    "title": "Final project proposal",
    "category": "FINAL_PROJECT", "categoryLabel": "Final project", "categoryWeight": 60,
    "contribution": 15, "description": "Outline your web application idea, ...",
    "dueAt": 1790024138000, "completed": false, "completedAt": null,
    "score": null, "maxScore": 10, "feedback": null,
    "status": "URGENT"
} ] }
```
`status`: `DONE` | `OVERDUE` | `URGENT` | `UPCOMING`. `contribution` là % đóng góp vào điểm môn; `categoryWeight` là trọng số của thành phần. Lỗi: `400 invalid_filter`.

### 4.11. `GET /api/v1/assignments/:id`
Trả một object bài (cùng cấu trúc phần tử ở 4.10). Lỗi: `404 assignment_not_found`.

### 4.12. `POST /api/v1/assignments/:id/completion`
Không cần body. Đánh dấu hoàn thành (`completed_at = now`, `score = NULL`) và trả bài đã cập nhật (`status: "DONE"`). Gọi lại khi đã xong vẫn `200`, không tạo bản ghi trùng. Bài quá hạn vẫn đánh dấu được.

### 4.13. `DELETE /api/v1/assignments/:id/completion`
Bỏ đánh dấu và trả bài đã cập nhật (trạng thái quay về `UPCOMING`/`URGENT`/`OVERDUE` theo hạn).
Lỗi: `409 already_graded` nếu bài đã có điểm; `404 assignment_not_found`.

### 4.14. `GET /api/v1/timetable`
```json
{
  "semesterStartsAt": 1784000000000, "semesterEndsAt": 1795000000000,
  "sessions": [ { "id": "bio-lecture", "courseId": "bio", "weekday": 1, "start": "08:00", "end": "10:00",
                  "kind": "Lý thuyết", "room": "A · 305" } ]
}
```
Server chỉ trả **mẫu lặp hằng tuần**; app tự bung ra từng ngày và giới hạn trong `[semesterStartsAt, semesterEndsAt]`. Sắp theo `weekday`, `start`.

### 4.15. `GET /api/v1/notifications`
```json
{ "items": [ {
    "id": "n-grade-lab3", "kind": "GRADE", "title": "A new grade to feel good about",
    "body": "Lab 03 · React components · 9.0 / 10", "createdAt": 1789992800000,
    "read": false, "assignmentId": "lab3", "courseId": "web"
} ], "unread": 2 }
```
`kind`: `GRADE` | `MESSAGE`. `assignmentId`/`courseId` là `null` nếu không liên quan. Sắp theo `createdAt` giảm dần.

### 4.16. `POST /api/v1/notifications/:id/read`
Không cần body. Đặt `read_at = now` (nếu chưa đọc) và trả **toàn bộ danh sách mới** như 4.15 (`unread` giảm). Lỗi: `404 notification_not_found`.

### 4.17. `GET /api/v1/reports/grades.csv`
Trả file CSV (không phải JSON):

```
Content-Type: text/csv; charset=utf-8
Content-Disposition: attachment; filename="USTH-Semester-1-2026-2027-grade-report.csv"
```
Cột: `Student, Student ID, Semester, Subject, Course code, Credits, Completed tasks, Total tasks, Accumulated grade`, mỗi môn một dòng, ví dụ `Hoang An,BA22-041,Semester 1 2026–2027,Molecular Biology,BIO 2201,3,6,9,8.2`. File có BOM UTF‑8 để Excel mở đúng tiếng Việt/ký tự đặc biệt, dòng kết thúc `\r\n`.

### 4.18. `POST /api/v1/dev/reset`
**Không cần token.** Xoá toàn bộ dữ liệu và seed lại theo thời điểm hiện tại. Trả `{ "ok": true, "seededAt": 1790000000000 }`. Route này **chỉ tồn tại** khi `NODE_ENV !== "production"`; ở production sẽ ra `404 not_found`. Mọi token đang dùng bị vô hiệu.

---

## 5. Ví dụ gọi bằng curl

Trên Windows PowerShell dùng `curl.exe` (không phải alias `curl`) hoặc chạy trong Git Bash.

```bash
BASE=http://localhost:8080

# 1. Đăng nhập, lấy token
TOKEN=$(curl -s -X POST $BASE/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"BA22-041","password":"usth2025"}' | node -pe "JSON.parse(require('fs').readFileSync(0)).token")

# 2. Lấy toàn bộ dữ liệu
curl -s $BASE/api/v1/bootstrap -H "Authorization: Bearer $TOKEN"

# 3. Bài sắp hạn
curl -s "$BASE/api/v1/assignments?filter=urgent" -H "Authorization: Bearer $TOKEN"

# 4. Đánh dấu hoàn thành rồi bỏ đánh dấu
curl -s -X POST   $BASE/api/v1/assignments/midterm/completion -H "Authorization: Bearer $TOKEN"
curl -s -X DELETE $BASE/api/v1/assignments/midterm/completion -H "Authorization: Bearer $TOKEN"

# 5. Tải báo cáo điểm
curl -s -OJ $BASE/api/v1/reports/grades.csv -H "Authorization: Bearer $TOKEN"

# 6. Làm mới dữ liệu khi deadline đã cũ
curl -s -X POST $BASE/api/v1/dev/reset
```

---

## 6. Kiểm thử

`cd backend && npm test` (`node --test`, **21 test**). `test/api.test.js` mở DB `:memory:`, `resetDatabase(db, NOW)` với `NOW` cố định (25/10/2026 10:00), khởi động server thật ở cổng ngẫu nhiên và gọi bằng `fetch`. Biến `clock` được đổi trong test để kiểm tra trạng thái theo thời gian (`proposal` thành `OVERDUE` sau 8 giờ).

| Nhóm | Test |
|---|---|
| auth | sai mật khẩu → 401; không token → 401; login không phân biệt hoa/thường; logout vô hiệu token |
| learning data | tiến độ môn khớp thiết kế (8/10, 6/9, 9/10); `summary` (6 pending, 2 urgent); bộ lọc assignments và `invalid_filter`; chi tiết môn; cách ly dữ liệu giữa hai sinh viên |
| completion | mark rồi undo; bài đã chấm không bỏ đánh dấu được (409); trạng thái đổi theo đồng hồ |
| misc | đánh dấu đã đọc thông báo; bootstrap đủ khoá; CSV tải về; route lạ → JSON 404; `%` sai → 400 `invalid_path`; dữ liệu môn trong bootstrap khớp từng `/courses/:id`; phiên hết hạn bị dọn ở lần đăng nhập sau |
| login throttling | sai 5 lần → 429 kèm `Retry-After`, hết 15 phút thì đăng nhập lại được; đăng nhập đúng xoá bộ đếm (chạy cuối vì khoá tạm `ba22-087`) |

---

## 7. Lưu ý khi phát triển & giới hạn hiện tại

1. **Deadline tương đối**: sau ~1 ngày nhiều bài thành `OVERDUE`; chạy `npm run reset`. Sau reset cần đăng nhập lại.
2. **Đổi payload bootstrap** → chạy `npm run export-sample` để cập nhật `app/src/main/assets/sample_bootstrap.json`, và sửa `JsonMapper.kt`/`Models.kt` bên app.
3. **Chặn dò mật khẩu chỉ theo username**, lưu trong bộ nhớ: không chặn theo IP, mất khi restart, và nhiều instance server sẽ không chia sẻ bộ đếm. Đủ cho demo; khi triển khai thật nên chuyển sang lưu chung (Redis/DB) và thêm giới hạn theo IP. Mặt trái của khoá theo username: người khác có thể cố tình nhập sai để khoá tạm tài khoản của một sinh viên trong 15 phút.
4. CORS mở `*` và `/dev/reset` không cần xác thực — chỉ phù hợp môi trường dev/demo, cần đóng lại trước khi công khai.
5. **Điểm và nộp bài là dữ liệu seed**: backend chưa có endpoint giảng viên chấm điểm hay nộp file; "đánh dấu hoàn thành" chỉ để sinh viên tự theo dõi, đúng như dòng nhắc trong app ("hãy nộp bài chính thức trên USTH Moodle").
6. `Content-Type` của request `POST` không được kiểm tra; server luôn parse body bằng `JSON.parse`.

Đã sửa trong đợt cải tiến (xem `docs/CAI_TIEN.md`): `%` sai trong URL trả 400 thay vì 500; phiên hết hạn được dọn khi đăng nhập; `bootstrap` không còn truy vấn lặp cho từng môn; có giới hạn đăng nhập sai.
