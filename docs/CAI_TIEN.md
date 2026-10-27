# Đợt cải tiến — 26/10/2026

> Ghi chú 27/10: sau đợt này code đã được **đơn giản hoá** (xem `DON_GIAN_HOA.md`) — một số tên file/hàm nhắc dưới đây đã đổi: `DeadlineReminders` + `ReminderScheduler` → `ReminderHelper`; `setRemindersEnabled` → `setRemindersOn`; `matchesQuery` → `matchesSearch`; `createLoginLimiter` → `lockedForMs/recordFailedLogin/clearFailedLogins`. Tính năng giữ nguyên.

Tài liệu ghi lại **mọi thay đổi** trong đợt cải tiến này: vì sao làm, sửa file nào, hoạt động ra sao, đã kiểm tra thế nào và còn giới hạn gì.
Mô tả chức năng tổng thể: `CHUC_NANG_VA_CSDL.md` · giải thích code: `CODE_WALKTHROUGH.md` · API: `BACKEND_API.md`.

## Tóm tắt

| # | Phần | Thay đổi | Loại |
|---|---|---|---|
| A1 | App | **Nhắc hạn nộp bằng thông báo hệ thống** (24 giờ và 2 giờ trước hạn), bật/tắt ở Profile, chạm vào mở thẳng bài | Tính năng mới |
| A2 | App | Tự đồng bộ khi quay lại app sau 5 phút; hiện "đồng bộ lúc …"; báo lỗi khi làm mới thất bại | Cải tiến |
| A3 | App | Ô tìm kiếm trong "My classwork" | Tính năng mới |
| A4 | App | Không sao lưu token, cache và lịch nhắc lên cloud / sang máy mới | Bảo mật |
| B1 | Backend | URL có `%` sai → 400 `invalid_path` thay vì 500 | Sửa lỗi |
| B2 | Backend | Sai mật khẩu 5 lần / 15 phút → 429 `too_many_attempts` | Bảo mật |
| B3 | Backend | Dọn phiên hết hạn khi đăng nhập | Dọn dẹp |
| B4 | Backend | `bootstrap` không còn truy vấn lặp cho từng môn | Hiệu năng |

Không thêm thư viện nào (app và backend). Cấu trúc payload API không đổi, nên `app/src/main/assets/sample_bootstrap.json` không cần xuất lại.

---

## A1. Nhắc hạn nộp

**Vấn đề.** App chỉ nhắc deadline khi người dùng mở app (badge chuông, thẻ đếm ngược). Không mở app thì không biết bài sắp hết hạn.

**Cách hoạt động.**
1. Ở **Profile** có thẻ *Nhắc hạn nộp* với công tắc, mặc định **tắt** (không tự xin quyền khi mở app). Bật lên trên Android 13+ thì app xin quyền `POST_NOTIFICATIONS`; nếu từ chối, công tắc vẫn tắt và có snackbar hướng dẫn bật trong Cài đặt.
2. Bật xong, app lập lịch cho mỗi bài **chưa hoàn thành, chưa quá hạn**: một lời nhắc lúc còn 24 giờ, một lúc còn 2 giờ. Mốc đã trôi qua thì bỏ (bài còn 20 giờ chỉ được nhắc ở mốc 2 giờ). Giữ tối đa 24 lời nhắc gần nhất. Snackbar báo số lời nhắc vừa đặt.
3. Lịch được **đặt lại tự động** mỗi khi dữ liệu đổi: tải/làm mới, *Mark as done*, *Undo*. Đánh dấu xong một bài thì lời nhắc của bài đó biến mất. Đăng xuất hoặc phiên hết hạn thì huỷ toàn bộ.
4. Tới giờ, hệ thống hiện thông báo kênh **"Nhắc hạn nộp"** (mức ưu tiên cao), ví dụ:
   - Tiêu đề: `Còn 1 giờ 59 phút: Final project proposal`
   - Nội dung: `Web Application Development · Hạn nộp 27 Oct, 05:13`
5. **Chạm thông báo** → app mở (hoặc đưa lên trước nếu đang chạy) và hiện thẳng *Assignment details* của bài đó.
6. **Khởi động lại máy / cập nhật app**: Android xoá mọi báo thức; receiver nhận `BOOT_COMPLETED` / `MY_PACKAGE_REPLACED` và đặt lại từ kế hoạch đã lưu, không cần mở app.

**Quyết định kỹ thuật.**
- Dùng `AlarmManager.setAndAllowWhileIdle` thay vì WorkManager (không thêm thư viện) và thay vì báo thức chính xác: `SCHEDULE_EXACT_ALARM` bị tắt mặc định trên Android 13+, còn `USE_EXACT_ALARM` chỉ dành cho app đồng hồ/lịch theo chính sách Google Play.
- Đổi lại, từ Android 12 hệ thống được giao báo thức không chính xác **muộn tới ~1 giờ** (`dumpsys alarm` hiện `window=+1h`). Vì vậy câu "Còn … giờ … phút" được **tính lúc thông báo hiện**, không ghi cứng lúc đặt lịch; tới lúc đó đã quá hạn thì không hiện gì. Lỗi này được phát hiện khi chạy thật trên emulator và đã sửa trước khi hoàn tất.
- Kế hoạch nhắc lưu ở SharedPreferences `moodle_reminders` (JSON) để huỷ đúng báo thức cũ (AlarmManager không cho liệt kê) và để khôi phục sau reboot mà không phải đọc lại dữ liệu bài tập (cache có thể cũ hơn trạng thái "đã xong").
- Mỗi bài dùng chung một id thông báo, nên lời nhắc 2 giờ **thay thế** lời nhắc 24 giờ thay vì xếp chồng.

**File.**
| File | Thay đổi |
|---|---|
| `app/.../reminders/DeadlineReminders.kt` | **Mới.** Logic thuần: `plan()`, `remainingLabel()`, `notificationTitle/Body()`, `toJson/fromJson()` |
| `app/.../reminders/ReminderScheduler.kt` | **Mới.** `enabled`, `sync()`, `restore()`, `cancelAll()`, `canPostNotifications()` |
| `app/.../reminders/ReminderReceiver.kt` | **Mới.** Hiện thông báo; đặt lại lịch sau reboot/cập nhật |
| `app/src/main/res/drawable/ic_stat_deadline.xml` | **Mới.** Icon đồng hồ đơn sắc cho thanh trạng thái |
| `AndroidManifest.xml` | Quyền `POST_NOTIFICATIONS`, `RECEIVE_BOOT_COMPLETED`; khai báo receiver; `MainActivity` → `launchMode="singleTop"` |
| `ui/MainViewModel.kt` | `remindersEnabled`, `setRemindersEnabled()`, `openAssignment()`, `showMessage()`; theo dõi `_data` để `sync`; huỷ khi logout/401 |
| `MainActivity.kt` | `by viewModels()`, đọc extra `assignment_id` ở `onCreate`/`onNewIntent` |
| `ui/screens/ProfileScreen.kt` | Thẻ *Nhắc hạn nộp* + xin quyền + cảnh báo khi thông báo bị tắt trong Cài đặt |

## A2. Đồng bộ thông minh hơn

**Vấn đề.**
- Mở lại app sau vài giờ vẫn thấy dữ liệu cũ cho tới khi tự kéo để làm mới; nhãn "sắp hạn" có thể trễ tới 30 giây vì đồng hồ chỉ cập nhật theo nhịp.
- Không biết dữ liệu đang xem là của lúc nào, nhất là khi ngoại tuyến.
- Kéo để làm mới bị lỗi (server 500, mất mạng mà không có cache) khi đã có dữ liệu trên màn hình thì **lỗi bị nuốt im lặng**.

**Cách hoạt động.**
- `MainActivity.onResume` → `viewModel.onAppResumed()`: cập nhật đồng hồ ngay; nếu dữ liệu cũ hơn **5 phút** hoặc đang ngoại tuyến thì tự làm mới ở chế độ `quiet` (không hiện lại snackbar "ngoại tuyến" nếu trước đó đã ngoại tuyến, không báo lỗi). Chế độ xem thử không tự làm mới.
- Thời điểm đồng bộ được lưu (`SessionStore.lastSyncedAtMs`) và hiện ở banner ngoại tuyến (`Ngoại tuyến · dữ liệu lúc 14:05 hôm nay. Chạm để thử lại.`) và thẻ trạng thái ở Profile.
- Làm mới thất bại khi đã có dữ liệu → giữ nguyên màn hình và hiện lỗi bằng snackbar (`onLoadFailed`).

**File.** `data/local/SessionStore.kt` (`lastSyncedAtMs`), `data/repository/MoodleRepository.kt` (`LoadResult.syncedAtMs`), `ui/MainViewModel.kt` (`LoadUiState.lastSyncedAtMs`, `refresh(initial, quiet)`, `onLoadFailed`, `onAppResumed`, `STALE_AFTER_MS`), `ui/Formatters.kt` (`syncedAt`), `MainActivity.kt` (banner, `onResume`), `ui/screens/ProfileScreen.kt`.

## A3. Tìm kiếm trong "My classwork"

- Ô tìm kiếm phía trên các tab, khớp **tên bài, tên môn đầy đủ hoặc tên ngắn**, không phân biệt hoa/thường, có nút xoá. Kết hợp với bộ lọc trạng thái và môn; số đếm trên tab tính theo từ khoá. Không có kết quả → lời nhắn "No assignments match “…”".
- Cố ý **không** khớp tên thành phần điểm: thử trên máy cho thấy "Mid-term & labs" làm từ khoá "lab" trả về cả quiz và bài đọc.
- File: `ui/dialogs/ClassworkListDialog.kt` (`Assignment.matchesQuery`, `OutlinedTextField`).

## A4. Không sao lưu dữ liệu phiên

`android:allowBackup="true"` trước đây sao lưu cả `moodle_session.xml` (token) và `bootstrap_cache.json` lên Google Drive; khôi phục sang máy khác sẽ dùng lại token cũ. Nay loại trừ `moodle_session.xml`, `moodle_reminders.xml` và `bootstrap_cache.json` ở cả `res/xml/backup_rules.xml` (Android ≤ 11) và `res/xml/data_extraction_rules.xml` (Android 12+, cả cloud backup và device transfer). Hệ quả: khôi phục app sang máy mới phải đăng nhập lại — đúng ý đồ.

## B1–B4. Backend

| # | Thay đổi | File |
|---|---|---|
| B1 | `decodeParam()` bọc `decodeURIComponent`; lỗi → `400 invalid_path` | `src/app.js` |
| B2 | `createLoginLimiter({ maxFailures: 5, windowMs: 15 phút })` đếm lần sai theo username; đủ 5 lần → khoá tới hết cửa sổ, trả `429 too_many_attempts` kèm header `Retry-After`; đăng nhập đúng xoá bộ đếm. `HttpError` nhận thêm `headers`. App hiển thị sẵn message của server nên không cần sửa app | `src/auth.js`, `src/app.js`, `src/services.js` |
| B3 | `createSession` xoá các phiên `expires_at <= now` trước khi tạo phiên mới | `src/auth.js` |
| B4 | `bootstrap` gọi `listAssignments` một lần rồi chia theo môn (trước đây thêm một truy vấn cho mỗi môn) | `src/services.js` |

---

## Kiểm thử

| Loại | Kết quả |
|---|---|
| Backend `npm test` | **21/21 pass** (16 cũ + 5 mới: `invalid_path`, bootstrap khớp `/courses/:id`, dọn phiên hết hạn, khoá sau 5 lần sai rồi mở sau 15 phút, đăng nhập đúng xoá bộ đếm) |
| App `./gradlew :app:testDebugUnitTest` | **12/12 pass** (`LearningRulesTest` 4, `DeadlineRemindersTest` 7, `ExampleUnitTest` 1) |
| `./gradlew :app:assembleDebug` | Build thành công; cảnh báo duy nhất (`Locale` deprecated ở `TimetableScreen.kt`) có từ trước |
| `./gradlew :app:lintDebug` | Không có lỗi; không có cảnh báo mới trong các file đã sửa |

**Chạy thật trên emulator** `Medium_Phone_API_37.0` (Android API 37) với backend cục bộ:

| Kịch bản | Kết quả |
|---|---|
| Token cũ sau khi reset DB | Về màn đăng nhập với "Phiên đăng nhập đã hết hạn" |
| Đăng nhập `BA22-041` | Vào Home, số liệu đúng |
| Profile | Thẻ *Nhắc hạn nộp*; "Dữ liệu lấy từ http://10.0.2.2:8080 lúc 22:34 hôm nay" |
| Bật nhắc | Snackbar "10 lời nhắc"; `dumpsys alarm` có 10 báo thức `DEADLINE_REMINDER`; bài còn ~6 giờ chỉ có mốc 2 giờ |
| Cập nhật APK khi đang bật | Sau cài đặt vẫn đủ 10 báo thức mà không mở app (`MY_PACKAGE_REPLACED` → `restore`) |
| Bài thử hạn sau 2 giờ 1 phút | Thông báo hiện "Còn 1 giờ 59 phút: Reminder test · Quick quiz" (hệ thống giao muộn ~70 giây — câu chữ vẫn đúng) |
| Chạm thông báo | Mở thẳng *Assignment details* của đúng bài |
| *Mark as done* bài đó | Lời nhắc của bài biến mất khỏi kế hoạch; vẫn đúng 10 báo thức (không nhân đôi) |
| Tìm "lab" | To do 1 (Lab 04), Done 3 (các bài lab) |

Bài thử đã được xoá khỏi DB sau khi kiểm tra; emulator chạy với `-no-snapshot-save` nên snapshot của AVD không bị ghi đè.

**Chưa kiểm tra trên máy:** khôi phục lịch sau khi khởi động lại máy thật (`BOOT_COMPLETED` — cùng đường code `restore()` với trường hợp cập nhật app đã thử), luồng từ chối quyền thông báo, và lỗi 429 trên giao diện app (đã có test API; app hiển thị message của server như mọi lỗi đăng nhập khác).

## Giới hạn còn lại

- Lời nhắc có thể đến muộn tới ~1 giờ trên Android 12+ (đổi lấy việc không cần quyền báo thức chính xác). Nhãn thời gian luôn đúng tại lúc hiện.
- Mốc nhắc cố định 24h/2h, chưa cho người dùng tự chọn.
- Kế hoạch nhắc phản ánh dữ liệu lần đồng bộ gần nhất; nếu bài được đánh dấu xong trên thiết bị khác thì lời nhắc chỉ bị huỷ ở lần đồng bộ sau.
- Bộ đếm đăng nhập sai nằm trong bộ nhớ, theo username (không theo IP), mất khi restart server; ai đó có thể cố tình nhập sai để khoá tạm tài khoản người khác 15 phút.
- Token vẫn lưu dạng thường trong SharedPreferences (đã loại khỏi backup, chưa mã hoá).

## Thử nhanh

```bash
cd backend && npm start                       # backend
./gradlew :app:installDebug                   # cài app lên emulator
```
Đăng nhập `BA22-041 / usth2025` → Profile → bật *Nhắc hạn nộp* → kiểm tra lịch: `adb shell dumpsys alarm | grep DEADLINE_REMINDER`.
Muốn thấy thông báo ngay: thêm một bài có hạn sau ~2 giờ 1 phút vào DB, kéo để làm mới, rồi đưa app về nền và đợi khoảng 1–2 phút.
