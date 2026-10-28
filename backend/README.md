# Moodle USTH – Backend

REST API cho app Android `moodle-usth-mobile-app`. Dữ liệu là dữ liệu mô phỏng (3 môn học, bài tập,
thời khóa biểu, thông báo, 2 sinh viên và 3 giáo viên), lưu bằng SQLite có sẵn trong Node.js nên **không cần `npm install`**.

Khi đổi schema, `db.js` tăng `SCHEMA_VERSION` (lưu ở `PRAGMA user_version`): DB cũ sẽ tự bị xoá và seed lại.

## Chạy

Yêu cầu Node.js ≥ 22.13 (đã thử với Node 24).

```bash
cd backend
npm start          # http://localhost:8080 — lần đầu tự seed dữ liệu vào data/moodle-usth.db
npm run dev        # tự restart khi sửa code
npm run reset      # seed lại: deadline được tính lại tương đối với thời điểm hiện tại
npm test           # test API (node:test)
npm run export-sample   # ghi lại app/src/main/assets/sample_bootstrap.json cho "Chế độ xem thử"
```

Deadline được seed tương đối so với lúc seed (bài gần nhất còn ~6h42m). Sau khoảng một ngày nhiều bài
sẽ chuyển sang *Overdue* — chạy `npm run reset` để làm mới. Server cũng in cảnh báo khi dữ liệu đã cũ.

Biến môi trường: `PORT` (mặc định 8080), `HOST` (0.0.0.0), `DB_FILE`, `NODE_ENV=production` (tắt `/dev/reset`).

### Tài khoản demo

| Tên đăng nhập | Mật khẩu | Vai trò | Ghi chú |
|---|---|---|---|
| `BA22-041` | `usth2025` | Sinh viên | Hoang An – ngành ICT, 3 môn |
| `BA22-087` | `usth2025` | Sinh viên | Tran Minh Chau – ngành SA, 2 môn, tiến độ khác |
| `gv-minh` | `usth2025` | Giáo viên | Dr. Nguyen Minh – ngành ICT, dạy Web Application Development (có bonus +1) |
| `gv-linh` | `usth2025` | Giáo viên | Dr. Tran Linh – ngành BIO, dạy Molecular Biology (không bonus) |
| `gv-quang` | `usth2025` | Giáo viên | Dr. Le Quang – ngành SA, dạy Earth & Space Science (có bonus +2) |

### Kết nối từ app

- **Android emulator**: mặc định `http://10.0.2.2:8080` (đã cấu hình trong `BuildConfig.API_BASE_URL`).
- **Điện thoại thật** (cùng Wi‑Fi): dùng địa chỉ LAN mà server in ra khi khởi động, nhập ở mục *Máy chủ*
  trên màn đăng nhập, hoặc đặt `moodle.apiUrl=http://192.168.x.x:8080` trong `local.properties`.
  Có thể cần mở cổng 8080 trên Windows Firewall.
- Android 17 (API 37) yêu cầu quyền **ACCESS_LOCAL_NETWORK** để app gọi tới địa chỉ mạng cục bộ; app tự
  xin quyền này khi bấm *Đăng nhập*. Nếu lỡ từ chối: Cài đặt › Ứng dụng › moodle-usth-mobile-app › Quyền.

## API

Mọi endpoint (trừ `/health`, `/auth/login`) cần header `Authorization: Bearer <token>`.
Lỗi luôn có dạng `{ "error": { "code": "...", "message": "..." } }`. Thời gian là epoch milliseconds.

| Method | Path | Mô tả |
|---|---|---|
| GET | `/health` | Kiểm tra server |
| POST | `/api/v1/auth/login` | `{ username, password }` → `{ token, expiresAt, profile }` (token sống 30 ngày) |
| POST | `/api/v1/auth/logout` | Huỷ token hiện tại |
| GET | `/api/v1/me` | Hồ sơ sinh viên |
| GET | `/api/v1/bootstrap` | **Một lần gọi lấy toàn bộ dữ liệu** app cần: profile, semester, summary, courses (kèm gradeComponents + announcements), assignments, timetable, notifications |
| GET | `/api/v1/summary` | Số bài to do / due soon / recent done, tiến độ chung |
| GET | `/api/v1/semester` | Tín chỉ, điểm trung bình (trọng số tín chỉ), tiến độ học kỳ |
| GET | `/api/v1/courses` | Danh sách môn kèm `done/total/progress/grade` |
| GET | `/api/v1/courses/:id` | Chi tiết môn: thành phần điểm, lịch học, bảng tin |
| GET | `/api/v1/assignments?courseId=&filter=todo\|urgent\|done` | Bài tập kèm trạng thái của sinh viên |
| GET | `/api/v1/assignments/:id` | Chi tiết bài tập |
| POST | `/api/v1/assignments/:id/completion` | Đánh dấu hoàn thành |
| DELETE | `/api/v1/assignments/:id/completion` | Bỏ đánh dấu (409 nếu bài đã được chấm) |
| GET | `/api/v1/timetable` | Lịch học hằng tuần + ngày bắt đầu/kết thúc học kỳ |
| GET | `/api/v1/notifications` | Thông báo 2 luồng: `PROGRAM` (tin ngành) và `GENERAL` (thầy cô + toàn trường); `unread`, `unreadByChannel` |
| POST | `/api/v1/notifications/:id/read` | Đánh dấu đã đọc (thông báo riêng hoặc tin ngành/toàn trường) |
| POST | `/api/v1/notifications/read-all` | `{ channel? }` — đánh dấu đã đọc cả một luồng (bỏ trống = tất cả) |
| GET | `/api/v1/reports/grades.csv` | Tải báo cáo điểm CSV (UTF‑8 BOM, mở được bằng Excel) |
| POST | `/api/v1/dev/reset` | Seed lại dữ liệu (chỉ khi không phải production) |

Các endpoint trên (trừ `/me`, `/auth/*`) chỉ dành cho **sinh viên**; giáo viên gọi sẽ nhận `403 forbidden`.

#### Giáo viên (`role = TEACHER`)

| Method | Path | Mô tả |
|---|---|---|
| GET | `/api/v1/teacher/bootstrap` | Hồ sơ, học kỳ, các môn đang dạy (thang điểm, bài tập + thống kê nộp/chấm, sinh viên + điểm, bảng tin), tin đã đăng |
| GET | `/api/v1/teacher/courses/:id` | Một môn đang dạy |
| PUT | `/api/v1/teacher/courses/:id/grading` | `{ components: [{ id?, name, weight }], bonusEnabled, bonusMax }` — tổng weight = 100 |
| POST | `/api/v1/teacher/courses/:id/assignments` | `{ title, description, componentId \| isBonus: true, weight, dueAt }` → báo `ASSIGNMENT_NEW` cho lớp |
| PUT | `/api/v1/teacher/assignments/:id` | Sửa bài; đổi `dueAt` → báo `DEADLINE_CHANGED` cho lớp |
| DELETE | `/api/v1/teacher/assignments/:id` | Xoá bài (409 nếu đã có điểm) |
| POST | `/api/v1/teacher/assignments/:id/auto-grade` | Máy chấm ngay các bài đã nộp chưa có điểm → `{ graded, course }` |
| POST | `/api/v1/teacher/announcements` | `{ audience: COURSE \| PROGRAM \| ALL, courseId?, kind: NOTICE \| EVENT, title, body, eventAt?, location?, pinned? }` |
| DELETE | `/api/v1/teacher/announcements/:id` | Xoá tin đã đăng (tin ngành/toàn trường hoặc tin của lớp mình) |

### Quy tắc nghiệp vụ

- **Trạng thái bài tập**: đã đánh dấu → `DONE`; quá hạn → `OVERDUE`; còn ≤ 24h → `URGENT`; còn lại `UPCOMING`.
  App dùng cùng quy tắc (`LearningRules.kt`) để đồng hồ đếm ngược cập nhật mà không phải gọi lại API.
- **Thang điểm 20.** Mỗi môn có các **thành phần điểm do giáo viên đặt** (bảng `grade_components`, VD
  Attendance 10% · Mid-term & labs 30% · Final project 60%; tổng = 100%). Mỗi bài thuộc một thành phần và có
  `weight` tương đối trong thành phần; `contribution` = weight thành phần × phần của bài (VD proposal 25/100 × 60% = 15%).
- **Điểm môn** = Σ(điểm thành phần × %) / Σ(% các thành phần đã có điểm) **+ bonus**, tối đa 20, làm tròn 1 chữ số.
  Điểm thành phần = trung bình có trọng số các bài đã chấm.
- **Bonus** là cài đặt của giáo viên (`bonus_enabled`, `bonus_max`): bonus = điểm TB các bài bonus / 20 × `bonus_max`.
  Môn không bật bonus → `bonus = null`.
- **Máy chấm**: trước mỗi request đã đăng nhập, mọi bài đã nộp và đã quá hạn mà chưa có điểm được chấm tự động
  (11–19.5, cố định theo sinh viên + bài; nộp muộn −2) và sinh viên nhận thông báo `GRADE`. Xem `src/grading.js`.
- **Điểm trung bình học kỳ** = trung bình điểm môn (thang 20) có trọng số theo tín chỉ.
- **Thông báo**: `PROGRAM` = tin/sự kiện cho ngành của sinh viên (bảng `bulletins`, audience PROGRAM);
  `GENERAL` = thông báo riêng (điểm, bài mới, đổi deadline, tin của lớp) + tin toàn trường (audience ALL).
- **Recent done** = số bài hoàn thành trong 14 ngày gần nhất.

## Cấu trúc

```
backend/
├── src/
│   ├── server.js     # khởi động HTTP server, seed lần đầu
│   ├── app.js        # router, auth middleware, xử lý lỗi/JSON
│   ├── services.js   # nghiệp vụ sinh viên: trạng thái, tiến độ, điểm, thông báo, CSV, bootstrap
│   ├── teacher.js    # nghiệp vụ giáo viên: thang điểm, bài tập, đăng tin
│   ├── grading.js    # công thức điểm thang 20 + bonus, máy chấm tự động
│   ├── auth.js       # băm mật khẩu (scrypt), token phiên
│   ├── db.js         # schema SQLite, reset/transaction
│   └── seed.js       # dữ liệu mô phỏng
├── scripts/export-sample.js
└── test/            # api.test.js (HTTP), grading.test.js (công thức điểm)
```
