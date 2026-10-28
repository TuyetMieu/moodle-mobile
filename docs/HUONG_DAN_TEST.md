# Hướng dẫn test: giao diện app và API backend

Hệ thống gồm hai phần, mỗi phần test bằng công cụ khác nhau:

| Phần | Là gì | Chạy ở đâu | Test bằng |
|---|---|---|---|
| **Giao diện (UI)** | App Android (Kotlin + Jetpack Compose), thư mục `app/` | Emulator hoặc điện thoại thật | Android Studio + emulator, bấm tay theo checklist ở mục 3 |
| **Backend (API)** | REST API Node.js + SQLite, thư mục `backend/` | Máy tính, cổng `8080` | `npm test`, `curl`, Postman / Thunder Client (mục 4–5) |

> Không có bản web. Trình duyệt chỉ mở được `http://localhost:8080/health`; mọi API khác cần token (header `Authorization`), nên phải dùng curl/Postman.

---

## 1. Chuẩn bị

### Backend
```bash
cd backend
npm run reset      # seed lại dữ liệu mẫu (deadline tính lại theo giờ hiện tại)
npm start          # http://localhost:8080
```
Server in ra địa chỉ cho máy này, emulator (`10.0.2.2:8080`) và điện thoại cùng Wi‑Fi (`192.168.x.x:8080`).

> Các con số mong đợi trong tài liệu này (điểm, số bài, số thông báo) đúng **ngay sau `npm run reset`**.
> Sau khi bạn bấm thử (đánh dấu xong, tạo bài, đổi thang điểm...) số sẽ khác — chạy `npm run reset` để về lại ban đầu.
> Deadline được tính tương đối lúc seed, nên để quá 1 ngày nhiều bài sẽ thành "Quá hạn" — cũng chạy lại `npm run reset`.

### App
- Mở thư mục gốc repo bằng Android Studio → chọn emulator → Run.
- Hoặc dòng lệnh (cần `JAVA_HOME` trỏ tới JDK của Android Studio, VD `C:\Program Files\Android\Android Studio\jbr`):
  ```bash
  ./gradlew :app:installDebug
  ```
- Trên emulator app tự dùng `http://10.0.2.2:8080`. Điện thoại thật: bấm dòng **"Máy chủ: …"** ở màn đăng nhập và nhập địa chỉ LAN.

### Tài khoản demo (mật khẩu chung `usth2025`)

| Tài khoản | Vai trò | Dùng để test |
|---|---|---|
| `BA22-041` | Sinh viên ngành ICT (Hoang An), 3 môn | Luồng sinh viên chính |
| `BA22-087` | Sinh viên ngành SA (Tran Minh Chau), 2 môn | Dữ liệu tách theo tài khoản, tin theo ngành |
| `gv-minh` | Giáo viên ngành ICT, dạy **Web** (có bonus +1) | Luồng giáo viên chính |
| `gv-linh` | Giáo viên ngành BIO, dạy **Molecular Biology** (không bonus) | Môn không có bonus |
| `gv-quang` | Giáo viên ngành SA, dạy **Earth & Space** (bonus +2) | |

Nút **"Xem thử với dữ liệu mẫu"** ở màn đăng nhập chạy app **không cần backend** (chỉ luồng sinh viên, dữ liệu trong `app/src/main/assets/sample_bootstrap.json`).

---

## 2. App gọi API nào, ở đâu

App sinh viên **không gọi API riêng cho từng màn**: khi mở app / kéo xuống làm mới, app gọi **một lần** `GET /api/v1/bootstrap` rồi mọi màn vẽ từ dữ liệu đó. Chỉ các thao tác thay đổi dữ liệu mới gọi API riêng.

### Sinh viên

| Thao tác trên app | Ở đâu trên UI | API backend | Code app |
|---|---|---|---|
| Đăng nhập | Màn đăng nhập → **Đăng nhập** | `POST /api/v1/auth/login` | `MoodleApi.login` |
| Tải toàn bộ dữ liệu | Mở app, kéo xuống để làm mới, chạm banner "Ngoại tuyến" | `GET /api/v1/bootstrap` | `MoodleRepository.load` |
| Đánh dấu xong / bỏ đánh dấu | Bấm một bài → dialog chi tiết → **Mark as done** / **Undo** | `POST` / `DELETE /api/v1/assignments/:id/completion` | `MoodleApi.setCompleted` |
| Đọc một thông báo | Chuông → bấm một tin | `POST /api/v1/notifications/:id/read` | `MoodleApi.markNotificationRead` |
| Đọc tất cả | Chuông → **Đánh dấu đã đọc tất cả** (theo tab đang mở) | `POST /api/v1/notifications/read-all` | `MoodleApi.markAllNotificationsRead` |
| Tải báo cáo điểm CSV | Tab **Tiến độ** → icon tải ↓ → dialog → **Download grade report (CSV)** | `GET /api/v1/reports/grades.csv` | `MoodleApi.downloadGradeReport` |
| Đăng xuất | Tab **Hồ sơ** → **Đăng xuất** | `POST /api/v1/auth/logout` | `MoodleApi.logout` |
| Đồng bộ nền + thông báo hệ thống | Tự chạy ~15 phút/lần khi đã đăng nhập (kể cả khi đóng app) | `GET /api/v1/bootstrap` | `reminders/SyncWorker.kt` |

### Giáo viên

| Thao tác trên app | Ở đâu trên UI | API backend |
|---|---|---|
| Tải dữ liệu lớp dạy | Mở app / kéo xuống làm mới | `GET /api/v1/teacher/bootstrap` |
| Lưu thang điểm, bật/tắt bonus | **Lớp dạy** → chọn lớp → tab **Thang điểm** → **Lưu thang điểm** | `PUT /api/v1/teacher/courses/:id/grading` |
| Tạo bài tập | Tab **Bài tập** → **Bài tập mới** → **Tạo bài tập** | `POST /api/v1/teacher/courses/:id/assignments` |
| Sửa bài / đổi deadline | Tab **Bài tập** → **Sửa** → **Lưu** | `PUT /api/v1/teacher/assignments/:id` |
| Xoá bài | **Sửa** → **Xoá** | `DELETE /api/v1/teacher/assignments/:id` |
| Chấm ngay | Tab **Bài tập** → **Chấm ngay (n)** | `POST /api/v1/teacher/assignments/:id/auto-grade` |
| Đăng tin (lớp / ngành / toàn trường) | Tab **Bảng tin** của lớp → **Đăng tin cho lớp**, hoặc tab **Đăng tin** | `POST /api/v1/teacher/announcements` |
| Xoá tin | Nút **Xoá** dưới mỗi tin | `DELETE /api/v1/teacher/announcements/:id` |

### API có sẵn nhưng app không gọi (chỉ để test bằng curl/Postman)
`GET /health`, `/api/v1/me`, `/summary`, `/semester`, `/courses`, `/courses/:id`, `/assignments?filter=todo|urgent|done&courseId=`, `/assignments/:id`, `/timetable`, `/notifications`, `/teacher/courses/:id`, `POST /api/v1/dev/reset`.

---

## 3. Test giao diện (bấm tay trên emulator)

Đánh dấu ✅ khi đúng. Số liệu mong đợi tính ngay sau `npm run reset`.

### 3.1. Đăng nhập — tài khoản `BA22-041`
| # | Bước | Mong đợi |
|---|---|---|
| 1 | Nhập sai mật khẩu → Đăng nhập | Hiện "Tên đăng nhập hoặc mật khẩu không đúng" |
| 2 | Sai 5 lần liên tiếp | Lần thứ 6 báo nhập sai quá nhiều lần (khoá 15 phút; restart server để mở khoá) |
| 3 | Đúng mật khẩu | Vào tab **Trang chủ**, lời chào "Chào An" |

### 3.2. Trang chủ (sinh viên)
| Ở đâu | Kiểm tra | Mong đợi |
|---|---|---|
| Ô "Góc học tập" | 3 ô Cần làm / Sắp hết hạn / Vừa xong | **10 / 2 / 8**; bấm mỗi ô mở danh sách bài đã lọc |
| Thẻ tối "Hạn nộp gần nhất" | Tên bài, đồng hồ đếm ngược | "Final project proposal", còn ~6 giờ 42 phút, chạy từng giây; "15% điểm môn · Xem bài" |
| "Ưu tiên của bạn" | Danh sách xếp hạng 1, 2, 3… | Bài chưa xong, chưa quá hạn, **deadline gần nhất đứng đầu**: 1. Final project proposal, 2. Lab 04 · DNA replication… |
| Chuông góc phải | Số đỏ | **6** (số thông báo chưa đọc) |

### 3.3. Thông báo (chuông)
| Kiểm tra | Mong đợi |
|---|---|
| Tab **Ngành của bạn** | 2 tin ngành ICT: "ICT Career Day 2026" (sự kiện, có ngày giờ + địa điểm), "USTH Hackathon…" |
| Tab **Thầy cô & toàn trường** | Điểm mới Lab 03 (18 / 20), tin của lớp Web, tin toàn trường (lịch thi, open lecture, thư viện) |
| Bấm một tin | Chấm đỏ mất, số trên chuông giảm 1 |
| **Đánh dấu đã đọc tất cả** | Badge của tab về 0 |
| Đăng nhập `BA22-087` | Tab ngành chỉ có "Visit to the satellite ground station" (ngành SA), **không** thấy tin ICT |

### 3.4. Môn học → chọn **Web Application Development**
| Tab | Mong đợi |
|---|---|
| Đầu trang | 4 cần làm, 8/12 đã xong, 4 tín chỉ, vòng tiến độ 67% |
| **Bài tập** | Bài nhóm theo thành phần: Attendance 10% · Mid-term & labs 30% · Final project 60% · nhóm Bonus "tối đa +1" |
| **Thang điểm** | Attendance 16.3/20; Mid-term & labs 18.0/20 · 33% đã chấm; Final project chưa chấm; Bonus +0 / tối đa +1; **Điểm môn 17.6 / 20** |
| **Bảng tin** | Tin ghim "A little reminder for this week" đứng đầu |
| Nút ⋯ góc phải | Dialog thông tin môn: lịch học, tài liệu theo tuần |

Các môn khác: Molecular Biology **16.3/20** (không có bonus), Earth & Space Science **17.9/20** (bonus tối đa +2).

### 3.5. Chi tiết bài + đánh dấu xong
| Bước | Mong đợi |
|---|---|
| Bấm "Mid-term practical" | Thành phần Mid-term & labs (30%), bài chiếm 20% điểm môn |
| **Mark as done** | Snackbar báo tiến độ môn; Trang chủ: Cần làm 10 → 9 |
| **Undo** | Quay lại như cũ |
| Mở "Lab 03 · React components" (đã chấm 18/20) | Nút ở trạng thái đã chấm, **không** bỏ đánh dấu được |

### 3.6. Lịch học, Tiến độ, Hồ sơ
| Tab | Kiểm tra | Mong đợi |
|---|---|---|
| **Lịch học** | Chuyển Tuần / Tháng, ‹ › đổi tuần | 6 buổi/tuần; nhãn Lý thuyết / Thực hành; bấm buổi học mở môn |
| **Tiến độ** | Điểm trung bình, thẻ từng môn | Điểm TB học kỳ **17.3 / 20** (trọng số tín chỉ) |
| **Tiến độ** → icon ↓ | Dialog báo cáo điểm → tải CSV | Snackbar "Đã tải báo cáo điểm…", file nằm trong thư mục Downloads |
| **Hồ sơ** → Ngôn ngữ | Chọn Tiếng Việt / English / Français | Toàn app đổi ngôn ngữ ngay (màn hình tải lại) |
| **Hồ sơ** → Giao diện | Hệ thống / Sáng / Tối | Đổi nền tối/sáng, chữ vẫn đọc rõ |
| **Hồ sơ** → Nhắc hạn nộp | Bật công tắc, chọn mốc (3 ngày, 1 ngày… 30 phút) | Snackbar báo số lời nhắc đã đặt; phải chọn ít nhất 1 mốc |

### 3.7. Giáo viên — tài khoản `gv-minh`
| Ở đâu | Bước | Mong đợi |
|---|---|---|
| **Lớp dạy** | Mở app | 1 lớp, 2 sinh viên; thẻ Web: "Bonus · tối đa +1", **Điểm TB lớp 16.7 / 20** |
| Lớp Web → **Sinh viên** | Xem danh sách | Hoang An **17.6**, Tran Minh Chau **15.8** (chỉ xem, không sửa) |
| **Thang điểm** | Đổi Attendance 10 → 20 | "Tổng: 110%" màu đỏ, nút Lưu bị khoá |
| | Đổi Attendance 20, Mid-term & labs 20, thêm "Quiz" 10, Final 50 → Lưu | Lưu được; đăng nhập lại `BA22-041` → điểm Web đổi thành **17.1** |
| | Xoá thành phần đang có bài (thùng rác) | Không bấm được, có dòng "n bài tập thuộc thành phần này" |
| | Tắt bonus | Không tắt được vì đang có 1 bài bonus |
| **Bài tập** | **Bài tập mới** → nhập tiêu đề → **Tạo bài tập** | Bài mới xuất hiện; `BA22-041` thấy thông báo "Bài tập mới" và bài nằm trong "Ưu tiên của bạn" |
| | **Sửa** → đổi hạn nộp → Lưu | Sinh viên nhận thông báo "Đổi hạn nộp" (hạn cũ → hạn mới) |
| | `BA22-041` đánh dấu xong bài đó → giáo viên bấm **Chấm ngay (1)** | Snackbar "Máy đã chấm 1 bài"; sinh viên nhận "Có điểm mới", điểm x/20 |
| | **Sửa** bài "Lab 03" | Không có nút Xoá (bài đã có điểm) |
| **Đăng tin** | Chọn **Ngành ICT** → loại Sự kiện → nhập → Đăng | `BA22-041` thấy ở tab "Ngành của bạn"; `BA22-087` (ngành SA) **không** thấy |
| | Chọn **Toàn trường** → Đăng | Cả hai sinh viên thấy ở tab "Thầy cô & toàn trường" |
| Lớp → **Bảng tin** | **Đăng tin cho lớp** | Tin lên bảng tin môn Web; sinh viên của lớp nhận thông báo |

### 3.8. Trường hợp lỗi / mạng
| Bước | Mong đợi |
|---|---|
| Đang đăng nhập sinh viên, tắt backend → kéo xuống làm mới | Banner đỏ "Ngoại tuyến · dữ liệu lúc …", vẫn xem được dữ liệu cũ |
| Bật lại backend → chạm banner | Banner mất, dữ liệu mới |
| `npm run reset` khi đang đăng nhập → làm mới | Về màn đăng nhập "Phiên đăng nhập đã hết hạn" (reset xoá mọi phiên) |
| Đăng nhập giáo viên rồi gọi API sinh viên (mục 5) | `403 forbidden` |

---

## 4. Test backend tự động

```bash
cd backend
npm test
```
Chạy bằng `node:test` (có sẵn trong Node, không cần cài gì), dùng DB tạm trong bộ nhớ nên **không đụng dữ liệu thật**:
- `test/api.test.js` — gọi HTTP thật tới từng API: đăng nhập, phân quyền, điểm, thông báo 2 luồng, giáo viên tạo bài/đổi thang điểm/chấm ngay/đăng tin, khoá đăng nhập sai…
- `test/grading.test.js` — công thức điểm thang 20, bonus, chặn ở 20, máy chấm.

Mong đợi: `tests 37 · pass 37 · fail 0`.

Test của app (logic, không cần emulator):
```bash
./gradlew :app:testDebugUnitTest   # LearningRulesTest (ưu tiên deadline, thời gian còn lại), DeadlineRemindersTest (mốc nhắc)
./gradlew :app:lintDebug           # kiểm tra lỗi Android (chuỗi dịch, quyền...)
```

---

## 5. Test API bằng tay

### Công cụ
- **curl** — có sẵn trong Git Bash. Trên PowerShell gõ `curl.exe` (vì `curl` trong PowerShell là lệnh khác), hoặc dùng `Invoke-RestMethod` (ví dụ cuối mục).
- **Postman** hoặc **Thunder Client** (extension VS Code) — tiện xem JSON. Tạo request, tab *Headers* thêm `Authorization: Bearer <token>`, tab *Body* chọn *raw / JSON*.
- **Trình duyệt** — chỉ mở được `http://localhost:8080/health`.

Mọi lỗi trả về dạng `{ "error": { "code": "...", "message": "..." } }`. Thời gian là epoch milliseconds.

### Kịch bản curl (Git Bash)
```bash
B=http://localhost:8080
curl $B/health

# 1. Đăng nhập sinh viên, lấy token
curl -s -X POST $B/api/v1/auth/login -H "Content-Type: application/json" \
  -d '{"username":"ba22-041","password":"usth2025"}'
# → copy giá trị "token" vào biến:
S=<token sinh viên>

# 2. Dữ liệu sinh viên
curl -s $B/api/v1/bootstrap      -H "Authorization: Bearer $S"   # toàn bộ dữ liệu app dùng
curl -s $B/api/v1/courses        -H "Authorization: Bearer $S"   # grade: web 17.6, bio 16.3, earth 17.9
curl -s $B/api/v1/summary        -H "Authorization: Bearer $S"   # pending 10, urgent 2
curl -s "$B/api/v1/assignments?filter=urgent" -H "Authorization: Bearer $S"   # proposal, lab
curl -s $B/api/v1/notifications  -H "Authorization: Bearer $S"   # unread 6, unreadByChannel {PROGRAM 2, GENERAL 4}

# 3. Thao tác sinh viên
curl -s -X POST   $B/api/v1/assignments/midterm/completion -H "Authorization: Bearer $S"   # status DONE
curl -s -X DELETE $B/api/v1/assignments/midterm/completion -H "Authorization: Bearer $S"   # status UPCOMING
curl -s -X DELETE $B/api/v1/assignments/lab3/completion    -H "Authorization: Bearer $S"   # 409 already_graded
curl -s -X POST $B/api/v1/notifications/read-all -H "Authorization: Bearer $S" \
  -H "Content-Type: application/json" -d '{"channel":"PROGRAM"}'
curl -s $B/api/v1/reports/grades.csv -H "Authorization: Bearer $S" -o grades.csv

# 4. Giáo viên
curl -s -X POST $B/api/v1/auth/login -H "Content-Type: application/json" \
  -d '{"username":"gv-minh","password":"usth2025"}'
T=<token giáo viên>
curl -s $B/api/v1/teacher/bootstrap -H "Authorization: Bearer $T"
curl -s $B/api/v1/bootstrap -H "Authorization: Bearer $T"          # 403 forbidden (API của sinh viên)

#    Tạo bài (dueAt = epoch ms trong tương lai; ở đây là 3 ngày nữa)
DUE=$(( $(date +%s) * 1000 + 3 * 86400000 ))
curl -s -X POST $B/api/v1/teacher/courses/web/assignments -H "Authorization: Bearer $T" \
  -H "Content-Type: application/json" \
  -d "{\"title\":\"Lab 05\",\"description\":\"Unit tests\",\"componentId\":\"web-labs\",\"weight\":10,\"dueAt\":$DUE}"

#    Đổi thang điểm (tổng phải = 100, nếu không → 400 weights_must_sum_100)
curl -s -X PUT $B/api/v1/teacher/courses/web/grading -H "Authorization: Bearer $T" \
  -H "Content-Type: application/json" \
  -d '{"components":[{"id":"web-att","name":"Attendance","weight":20},{"id":"web-labs","name":"Mid-term & labs","weight":20},{"id":"web-final","name":"Final project","weight":60}],"bonusEnabled":true,"bonusMax":1}'

#    Máy chấm ngay bài midterm (sau khi sinh viên đã đánh dấu xong)
curl -s -X POST $B/api/v1/teacher/assignments/midterm/auto-grade -H "Authorization: Bearer $T"   # → { graded, course }

#    Đăng tin cho ngành (sự kiện) / toàn trường
curl -s -X POST $B/api/v1/teacher/announcements -H "Authorization: Bearer $T" -H "Content-Type: application/json" \
  -d "{\"audience\":\"PROGRAM\",\"kind\":\"EVENT\",\"title\":\"ICT meetup\",\"body\":\"Pizza & demos\",\"eventAt\":$DUE,\"location\":\"A21\"}"
curl -s -X POST $B/api/v1/teacher/announcements -H "Authorization: Bearer $T" -H "Content-Type: application/json" \
  -d '{"audience":"ALL","kind":"NOTICE","title":"Campus closed Monday","body":"Public holiday"}'

# 5. Seed lại dữ liệu (chỉ khi không chạy production)
curl -s -X POST $B/api/v1/dev/reset
```

### PowerShell (Invoke-RestMethod)
```powershell
$B = "http://localhost:8080"
$login = Invoke-RestMethod -Method Post "$B/api/v1/auth/login" -ContentType "application/json" `
  -Body '{"username":"ba22-041","password":"usth2025"}'
$H = @{ Authorization = "Bearer $($login.token)" }
Invoke-RestMethod "$B/api/v1/courses" -Headers $H | ConvertTo-Json -Depth 5
```

### Mã lỗi hay gặp
| HTTP | `error.code` | Khi nào |
|---|---|---|
| 400 | `missing_credentials`, `invalid_filter`, `weights_must_sum_100`, `invalid_dueAt`, `invalid_audience`… | Dữ liệu gửi lên sai |
| 401 | `invalid_credentials` / `unauthorized` | Sai mật khẩu / thiếu hoặc hết hạn token |
| 403 | `forbidden` | Sai vai trò (giáo viên gọi API sinh viên hoặc ngược lại) |
| 404 | `not_found`, `course_not_found`, `assignment_not_found`… | Không có hoặc không thuộc quyền của bạn |
| 409 | `already_graded`, `component_in_use`, `bonus_in_use`, `assignment_graded` | Thao tác bị chặn theo quy tắc nghiệp vụ |
| 429 | `too_many_attempts` | Sai mật khẩu 5 lần trong 15 phút |

---

## 6. Xem dữ liệu trong database

DB nằm ở `backend/data/moodle-usth.db` (SQLite). Mở bằng **DB Browser for SQLite** hoặc extension **SQLite Viewer** của VS Code để kiểm tra sau mỗi thao tác, VD:
- `submissions` — bài đã nộp, điểm (`score`), ai chấm (`graded_by = AUTO`)
- `grade_components` — thang điểm giáo viên đặt
- `notifications`, `bulletins`, `bulletin_reads` — thông báo và trạng thái đã đọc

Tài liệu liên quan: `backend/README.md` (danh sách API đầy đủ), `docs/NANG_CAP_V2.md` (công thức điểm, thông báo, phân quyền).
