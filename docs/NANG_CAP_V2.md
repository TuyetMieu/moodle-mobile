# Đợt nâng cấp v2: giáo viên, thang 20, thông báo, dark mode, 3 ngôn ngữ

Tài liệu này tóm tắt đợt nâng cấp v2. Chi tiết API xem `backend/README.md`.

## Tóm tắt

| Hạng mục | Kết quả |
|---|---|
| Phân luồng người dùng | Hai vai trò `STUDENT` / `TEACHER`. Đăng nhập xong, app tự chuyển sang giao diện tương ứng. |
| Thang điểm | Thang **20**. Thành phần điểm (Attendance, Midterm, Final…) và trọng số % do **giáo viên đặt** cho từng môn. |
| Bonus | Là cài đặt của giáo viên: bật/tắt cho từng môn, đặt mức tối đa (0.5–5). Bonus cộng thẳng vào điểm môn, tối đa 20. |
| Chấm điểm | Máy chấm tự động khi bài đã nộp và quá hạn. Giáo viên có thể bấm "Chấm ngay". |
| Ưu tiên | Trang chủ có "Ưu tiên của bạn": bài chưa xong, chưa quá hạn, xếp theo deadline gần nhất của sinh viên đó. |
| Thông báo | Hai luồng: **Ngành của bạn** và **Thầy cô & toàn trường**. Có thông báo hệ thống chạy nền và mốc nhắc deadline tuỳ chọn. |
| Giao diện | Dark mode (Hệ thống / Sáng / Tối), chọn ngôn ngữ **Tiếng Việt / English / Français**. |

## 1. Phân quyền

- Bảng `users` có thêm cột `role` (`STUDENT` | `TEACHER`) và `program_code` (ngành: ICT, SA, BIO).
- Route khai báo `role`. Gọi sai vai trò sẽ nhận `403 forbidden`.
- Phía app:
  - `SessionStore.role` lưu vai trò.
  - `MoodleAppRoot` chọn `SignedInApp` (sinh viên) hoặc `TeacherApp` (giáo viên).
  - Hai luồng dùng chung `MainViewModel` cho đăng nhập và cài đặt.

### Giáo viên làm được gì

Có 3 tab: **Lớp dạy**, **Đăng tin**, **Hồ sơ**. Trang một môn có 4 tab:

1. **Bài tập**
   - Tạo, sửa, xoá bài. Mỗi bài thuộc một thành phần điểm, hoặc là bài bonus.
   - Đặt trọng số trong thành phần và hạn nộp (chọn ngày, rồi chọn giờ).
   - Xem thống kê đã nộp / đã chấm / điểm trung bình, và bấm "Chấm ngay".
2. **Thang điểm**
   - Thêm, đổi tên, đổi % hoặc xoá thành phần. Tổng phải bằng 100%.
   - Không xoá được thành phần còn bài tập.
   - Bật/tắt bonus và chỉnh bonus tối đa.
3. **Sinh viên**
   - Điểm từng sinh viên (máy tính), bonus, tiến độ, số bài chưa nộp.
   - Giáo viên **không nhập điểm tay**, đúng như yêu cầu.
4. **Bảng tin**: đăng và xoá tin của lớp.

Tab **Đăng tin** gửi được tới 3 nhóm đối tượng:

| Đối tượng | Sinh viên nhận ở đâu |
|---|---|
| Một lớp | Bảng tin của lớp, và thông báo `ANNOUNCEMENT` cho từng sinh viên |
| Ngành của giáo viên | Tab "Ngành của bạn" của sinh viên ngành đó |
| Toàn trường | Tab "Thầy cô & toàn trường" |

Mỗi tin có loại *Thông báo* hoặc *Sự kiện*. Sự kiện có thêm ngày giờ và địa điểm.

## 2. Điểm thang 20

Công thức nằm ở `backend/src/grading.js`, có test riêng `backend/test/grading.test.js`.

```
điểm thành phần = Σ(điểm bài × weight bài) / Σ(weight các bài ĐÃ CHẤM trong thành phần)
điểm cơ bản     = Σ(điểm thành phần × % thành phần) / Σ(% các thành phần đã có điểm)
bonus           = (TB điểm các bài bonus đã chấm / 20) × bonus tối đa     (môn không bật bonus → null)
điểm môn        = min(20, điểm cơ bản + bonus)
```

- **% đóng góp của một bài** = % thành phần × weight bài / tổng weight của thành phần.
  - Ví dụ: proposal có weight 25, final demo có weight 75, thành phần Final project chiếm 60%.
  - Khi đó proposal đóng góp 15%, demo đóng góp 45%.
- **Máy chấm** (`autoGrade`) chạy trước mỗi request đã đăng nhập, trong cùng transaction với request.
  - Nó chấm mọi bài **đã nộp, đã quá hạn, chưa có điểm**.
  - Điểm nằm trong khoảng 11–19.5, bước 0.5. Điểm cố định theo cặp (sinh viên, bài), nên chạy lại vẫn ra cùng điểm.
  - Nộp muộn bị trừ 2 điểm.
  - Mỗi bài được chấm tạo một thông báo `GRADE`.
- Sinh viên vẫn không bỏ đánh dấu được bài đã có điểm (lỗi 409).

## 3. Ưu tiên deadline

`LearningRules.priorities()` lấy các bài chưa xong và chưa quá hạn, rồi sắp xếp theo `dueAt` tăng dần.

- Bài quá hạn được tách riêng (`overdue()`). Trang chủ hiện chúng thành một dòng cảnh báo.
- Thẻ đếm ngược dùng `nextDeadline()`: lấy deadline gần nhất. Nếu không còn bài nào sắp tới thì lấy bài vừa quá hạn.

## 4. Thông báo

### Hai luồng trong app

| Luồng | Nguồn |
|---|---|
| `PROGRAM` | Bảng `bulletins` với `audience = PROGRAM`, lọc theo ngành của sinh viên |
| `GENERAL` | Thông báo riêng (`GRADE`, `ASSIGNMENT_NEW`, `DEADLINE_CHANGED`, `ANNOUNCEMENT`) và `bulletins` toàn trường |

- Dialog thông báo có 2 tab, mỗi tab có badge số chưa đọc và nút "Đánh dấu đã đọc tất cả" (`POST /notifications/read-all`).
- Với thông báo do server tự sinh, server chỉ gửi **tham số** trong `data` (điểm, deadline cũ/mới). App tự ghép câu theo ngôn ngữ đang chọn trong `reminders/NotificationTexts.kt`.

### Thông báo hệ thống chạy nền

- `reminders/SyncWorker.kt` (WorkManager) chạy khoảng 15 phút một lần, kể cả khi app đã đóng. Mỗi lần chạy nó làm 3 việc:
  1. Tải dữ liệu mới và lưu cache.
  2. Đặt lại lịch nhắc deadline. Nhờ vậy, khi giáo viên đổi hạn nộp, lời nhắc cũng đổi theo.
  3. Hiện thông báo cho tin mới chưa đọc, chia vào 2 kênh Android: "Tin của ngành" và "Thầy cô & toàn trường".
- Lần đồng bộ đầu tiên không báo dồn các tin cũ. Khi app đang mở và vừa tải dữ liệu, các tin đó được coi là đã báo (`UpdateNotifier.markSeen`).
- Không cần Firebase: app tự hỏi server.

### Cài đặt trong Hồ sơ

- Bật/tắt nhắc deadline.
- Chọn nhiều mốc nhắc: 3 ngày, 1 ngày, 12 giờ, 6 giờ, 2 giờ, 1 giờ, 30 phút. Mặc định là 1 ngày và 2 giờ.
- Bật/tắt riêng thông báo của từng luồng.

## 5. Dark mode và ngôn ngữ

- **Màu**: `ui/theme/Color.kt` khai báo `AppColors` cho chế độ sáng và tối. Các tên cũ (`TextPrimary`, `SurfaceLight`, `AccentGreenLight`…) giờ là getter `@Composable` đọc màu từ `LocalAppColors`, nên tự đổi theo theme. Nền thẻ dùng `CardSurface` thay cho `Color.White`.
- **Ngôn ngữ**:
  - `values/` là tiếng Anh (mặc định), `values-vi/` là tiếng Việt, `values-fr/` là tiếng Pháp.
  - Chuỗi chia theo nhóm: `strings.xml` (dùng chung), `strings_home.xml`, `strings_course.xml`, `strings_profile.xml`, `strings_teacher.xml`.
  - `LocaleHelper` áp ngôn ngữ trong `MainActivity.attachBaseContext`. Đổi ngôn ngữ sẽ `recreate()` Activity.
  - ViewModel, Receiver và Worker lấy chuỗi qua `LocaleHelper.localized(context)`.
- **Lỗi từ server** được dịch theo mã lỗi (`ui/ErrorTexts.kt`). Mã lỗi lạ thì giữ nguyên câu server gửi.

## 6. Tài khoản demo

Mật khẩu chung: `usth2025`.

| Tài khoản | Vai trò | Ghi chú |
|---|---|---|
| `BA22-041` | Sinh viên ngành ICT | Học 3 môn |
| `BA22-087` | Sinh viên ngành SA | Học 2 môn, nên thấy tin ngành SA chứ không thấy tin ngành ICT |
| `gv-minh` | Giáo viên | Dạy Web (có bonus +1) |
| `gv-linh` | Giáo viên | Dạy Molecular Biology (không bonus) |
| `gv-quang` | Giáo viên | Dạy Earth & Space (có bonus +2) |

### Kịch bản demo nhanh

1. Đăng nhập `gv-minh`, mở **Web**, vào tab **Bài tập**, tạo "Lab 05" với hạn 2 ngày tới.
2. Đăng xuất, đăng nhập `BA22-041`.
   - Chuông thông báo hiện "Bài tập mới · Web Development".
   - Bài Lab 05 xuất hiện trong "Ưu tiên của bạn".
3. Sinh viên mở Lab 05 và bấm *Mark as done*.
4. Quay lại tài khoản giáo viên, vào tab **Bài tập**, bấm "Chấm ngay (1)".
5. Sinh viên nhận thông báo "Có điểm mới". Điểm hiện dạng x/20 và điểm môn được tính lại.
6. Giáo viên vào **Thang điểm**, đổi % hoặc tắt bonus. Điểm của sinh viên đổi theo ngay.

## 7. Giới hạn còn lại

- Máy chấm chỉ mô phỏng: điểm sinh từ mã băm, không đọc nội dung bài nộp.
- Thông báo nền phụ thuộc WorkManager, chạy tối thiểu 15 phút một lần. Android có thể giãn thêm khi máy tiết kiệm pin.
- Nội dung do người dùng nhập (tên môn, mô tả bài, tin đăng) không được dịch; chỉ giao diện được dịch.
- Không có chế độ xem thử cho giáo viên; nút *Xem thử* chỉ dành cho sinh viên.
