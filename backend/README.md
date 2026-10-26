# Moodle USTH – Backend

REST API cho app Android `moodle-usth-mobile-app`. Dữ liệu là dữ liệu mô phỏng (3 môn học, bài tập,
thời khóa biểu, thông báo), lưu bằng SQLite có sẵn trong Node.js nên **không cần `npm install`**.

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

| Mã sinh viên | Mật khẩu | Ghi chú |
|---|---|---|
| `BA22-041` | `usth2025` | Hoang An – 3 môn, khớp với bản thiết kế |
| `BA22-087` | `usth2025` | Tran Minh Chau – 2 môn, tiến độ khác (để thấy dữ liệu tách theo tài khoản) |

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
| GET | `/api/v1/notifications` | Thông báo từ lớp học (`unread` = số chưa đọc) |
| POST | `/api/v1/notifications/:id/read` | Đánh dấu đã đọc |
| GET | `/api/v1/reports/grades.csv` | Tải báo cáo điểm CSV (UTF‑8 BOM, mở được bằng Excel) |
| POST | `/api/v1/dev/reset` | Seed lại dữ liệu (chỉ khi không phải production) |

### Quy tắc nghiệp vụ

- **Trạng thái bài tập**: đã đánh dấu → `DONE`; quá hạn → `OVERDUE`; còn ≤ 24h → `URGENT`; còn lại `UPCOMING`.
  App dùng cùng quy tắc (`LearningRules.kt`) để đồng hồ đếm ngược cập nhật mà không phải gọi lại API.
- **Thành phần điểm** mỗi môn: Attendance 10% · Mid-term & labs 30% · Final project 60%. Mỗi bài có
  `contribution` = % đóng góp vào điểm môn (VD: proposal 15%, mỗi buổi điểm danh 1%).
- **Điểm tích luỹ môn** = trung bình có trọng số (theo `contribution`) của các bài đã chấm, làm tròn 1 chữ số.
- **Điểm trung bình học kỳ** = trung bình điểm môn có trọng số theo tín chỉ.
- **Recent done** = số bài hoàn thành trong 14 ngày gần nhất.

## Cấu trúc

```
backend/
├── src/
│   ├── server.js     # khởi động HTTP server, seed lần đầu
│   ├── app.js        # router, auth middleware, xử lý lỗi/JSON
│   ├── services.js   # nghiệp vụ: trạng thái, tiến độ, điểm, CSV, bootstrap
│   ├── auth.js       # băm mật khẩu (scrypt), token phiên
│   ├── db.js         # schema SQLite, reset/transaction
│   └── seed.js       # dữ liệu mô phỏng
├── scripts/export-sample.js
└── test/api.test.js
```
