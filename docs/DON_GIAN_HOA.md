# Đơn giản hoá code — 27/10/2026

Mục tiêu: viết lại code sao cho **người mới học Android/Kotlin đọc hiểu được**, bỏ các kỹ thuật nâng cao,
**không đổi tính năng, giao diện hay API**. Toàn bộ test vẫn pass và app đã được chạy thử lại trên emulator.

Bản sao lưu trước khi sửa (app/src, backend, docs) nằm ở scratchpad của phiên làm việc:
`C:\Users\admin\AppData\Local\Temp\claude\d--EDU-test-desgin\88b70317-6d28-46ad-9d3f-528a236500ca\scratchpad\backup-truoc-don-gian-hoa`
(thư mục tạm — nên commit hoặc copy ra chỗ khác nếu muốn giữ lâu).

---

## 1. App Android — đã đổi gì

| Trước (khó) | Sau (đơn giản) | File |
|---|---|---|
| `MutableStateFlow` + `asStateFlow` + `stateIn` + `combine`; màn hình dùng `collectAsState()` (~40 chỗ) | `var x by mutableStateOf(...)` trong ViewModel; màn hình đọc thẳng `viewModel.x`. Dữ liệu suy ra là thuộc tính có `get()` (VD `val assignments get() = data?.assignments ?: emptyList()`) | `MainViewModel.kt`, mọi màn hình/dialog |
| `Channel` + `receiveAsFlow` cho snackbar | Biến `message: String?`; `MainActivity` hiện xong thì đặt lại `null` | `MainViewModel.kt`, `MainActivity.kt` |
| `sealed class DialogState` (7 loại) | `openDialog("assignment", id)`; `dialogName` + `dialogArg` là chuỗi | `MainViewModel.kt`, `MainActivity.kt`, các màn hình |
| `AuthState`, `LoginUiState`, `LoadUiState` | Biến riêng: `isLoggedIn`, `isLoggingIn`, `loginError`, `isLoading`, `isRefreshing`, `isOffline`, `loadError`, `lastSyncedAtMs` | `MainViewModel.kt` |
| `pendingAssignmentIds: Set<String>` | `savingAssignmentId: String?` | `MainViewModel.kt`, `AssignmentDetailDialog.kt` |
| Collector `_data.collectLatest { reminders.sync(...) }` | Hàm `setNewData()`: đổi `data` rồi gọi `ReminderHelper.update(...)` | `MainViewModel.kt` |
| `CompositionLocalProvider(LocalSnackbarHostState …)` | `object AppSnackbar { val state = SnackbarHostState() }` dùng chung | `AppModal.kt`, `MainActivity.kt` |
| `MoodleApi(baseUrl = { … }, token = { … })` (truyền lambda) | `MoodleApi(session)` đọc `session.baseUrl`, `session.token` | `MoodleApi.kt`, `MoodleRepository.kt` |
| Extension function `JSONArray.objects()`, `JSONObject.optLongOrNull()`, `String.encoded()`, `Assignment.matchesQuery()`, `ClassSession.hours()` | Hàm thường: `toList(array)`, `getLongOrNull(json, key)`, `encode(text)`, `matchesSearch(assignment, query)`, `sessionHours(session)` | `JsonMapper.kt`, `MoodleApi.kt`, `ClassworkListDialog.kt`, `TimetableScreen.kt` |
| `runCatching { Enum.valueOf(...) }.getOrDefault(...)`, `.let { }`, `?.let(::f)`, tham chiếu hàm `::course` | `Enum.entries.find { it.name == ... } ?: mặc định`, `if (x != null) ... else ...`, `.map { course(it) }` | nhiều file |
| `Calendar.getInstance().apply { ... }`, `chunked(7)`, `flatMap`, `sumOf`, `mapNotNull`, `partition`, destructuring `val (a, b) = ...` | Biến tạm + gọi từng lệnh, vòng `for`, `subList`, hàm `totalHoursOf()`, `courseTones()`, `totalCredits()`, `if/else` gán `var` | `TimetableScreen.kt`, `CoursesScreen.kt`, `ClassworkListDialog.kt`, `ProfileScreen.kt`, `GradeWeightsCard.kt` |
| `prefs.edit { … }` (KTX) | `prefs.edit().putString(...).apply()` | `SessionStore.kt`, `ReminderHelper.kt` |
| 3 file nhắc hạn nộp (`DeadlineReminders`, `ReminderScheduler` class, `ReminderReceiver`) | 2 file: `ReminderHelper` (object gồm hàm thường) + `ReminderReceiver` | `reminders/` |
| Hàm cục bộ + `remember(key)` trong ProfileScreen | `if/else` ngay trong `onCheckedChange` | `ProfileScreen.kt` |

**Giữ nguyên:** toàn bộ layout Compose (Row/Column/Text/Card… — vốn đã là cách người mới viết), màu sắc, chữ trên giao diện,
`LearningRules`, `Models.kt`, theme, thứ tự màn hình.

## 2. Backend — đã đổi gì

| Trước | Sau | File |
|---|---|---|
| Bảng route dạng mảng `[method, pattern, handler, {auth}]`, biên dịch pattern thành `RegExp`, `matchAll` | Mảng object `{ method, path, auth, run(ctx) }`; hàm `matchPath()` tách `"/"` rồi so từng đoạn bằng vòng `for` | `app.js` |
| Handler viết dạng arrow + destructuring `({ db, user, now }) => …` | `run(ctx) { return ...(ctx.db, ctx.user.id, ctx.now) }` | `app.js` |
| `createLoginLimiter()` trả về object có closure | 3 hàm thường `lockedForMs / recordFailedLogin / clearFailedLogins` nhận một `Map` | `auth.js`, `app.js` |
| `reduce(...)` lồng nhau để tính điểm, tín chỉ | Vòng `for` với biến cộng dồn; hàm chung `weightedAverage()` | `services.js` |
| CSV dùng spread `...`, `map().join()` lồng nhau, regex escape | Vòng `for` + hàm `csvCell()` dùng `includes/replaceAll` | `services.js` |
| `Object.values(networkInterfaces()).flat().filter().map()` | Hai vòng `for` | `server.js` |

API (đường dẫn, request, response, mã lỗi) **không đổi một chữ** — `npm test` (21 test, dùng `fetch` thật) vẫn pass nguyên.

## 3. Những thứ cố ý GIỮ LẠI dù hơi "khó" — và vì sao

| Thứ | Vì sao không bỏ được |
|---|---|
| Coroutine (`viewModelScope.launch`, `suspend`, `withContext(Dispatchers.IO)`) | Android **cấm** gọi mạng trên luồng giao diện; đây là cách tối thiểu |
| `AndroidViewModel` + `by viewModels()` | Dữ liệu phải sống qua xoay màn hình; Activity cần ViewModel để xử lý mở app từ thông báo |
| `Map` cho bộ đếm đăng nhập sai | Object `{}` thường bị phá nếu ai đó gõ mã SV là `"__proto__"` — đổi sang `{}` sẽ mở lỗ hổng |
| `Promise` trong `readJson` (backend) | Node đọc body thành từng mảnh theo sự kiện; phải gói Promise để dùng `await` |
| scrypt + `timingSafeEqual` | Băm mật khẩu an toàn; bỏ đi là giảm bảo mật |
| `AlarmManager` / `PendingIntent` / `BroadcastReceiver` | Cách duy nhất để hiện thông báo khi app đang tắt (tính năng nhắc hạn nộp) |
| `object Dates` trong lịch học | Đã viết lại bằng lệnh từng dòng; logic tính tuần/tháng tự nó cần ~10 hàm nhỏ |

Nói thẳng: một người học Android **1 ngày** chưa thể tự viết app có mạng, coroutine, Compose và thông báo hệ thống.
Code hiện tại là mức đơn giản nhất mà vẫn giữ đủ tính năng; mỗi khái niệm còn lại đều có comment tiếng Việt giải thích ngay tại chỗ.

## 4. Đánh đổi

- `MoodleRepository.load()` nay ghi file cache và đọc `assets/sample_bootstrap.json` ngay trên luồng chính (trước đây chạy trên `Dispatchers.IO`) cho gọn code. File chỉ vài chục KB nên không thấy giật; nếu dữ liệu lớn lên nên bọc lại bằng `withContext(Dispatchers.IO)`.
- `ReminderHelper.update()` (đặt ~24 báo thức) cũng chạy trên luồng chính — vài mili-giây.
- Tên dialog là chuỗi: gõ sai tên thì trình biên dịch không báo (sealed class trước đây thì có). Danh sách 7 tên nằm trong comment của `dialogName`.
- Lint có thêm 7 cảnh báo `UseKtx` (gợi ý dùng lại `prefs.edit { }`) — cố ý, chỉ là gợi ý phong cách.
- Lỗi mạng nay được bắt bằng `catch (error: Exception)` (trước là `IOException`), nên lỗi JSON lạ từ server cũng hiện thành thông báo thay vì làm app dừng.

## 5. Kiểm tra

| Loại | Kết quả |
|---|---|
| `cd backend && npm test` | **21/21 pass** |
| `./gradlew :app:testDebugUnitTest` | **12/12 pass** (test nhắc hạn nộp viết lại theo tên hàm mới, cùng nội dung kiểm tra) |
| `./gradlew :app:assembleDebug` / `:app:lintDebug` | Thành công; không lỗi; thêm 7 cảnh báo `UseKtx` (xem trên) |
| `server.js` | Khởi động, in địa chỉ LAN, `/health` trả 200; CSV vẫn có BOM UTF‑8 |
| Emulator (API 37) | Token cũ → về màn đăng nhập ✔ · đăng nhập `BA22-041` → Home ✔ · View task → dialog ✔ · Mark as done → "Completed", nút Undo, snackbar **trong dialog**, chuông ở nền giảm 2 → 1 ✔ · Undo ✔ · Profile "đồng bộ lúc 09:34 hôm nay" ✔ · tắt/bật nhắc: báo thức 9 → 0 → 9 ✔ · Lịch học tháng 10/2026: lưới bắt đầu thứ Năm, 26 buổi · 52 giờ ✔ · Recent done → tab Done, nhóm "GẦN ĐÂY · 14 NGÀY (8)" ✔ · logcat không có crash |

## 6. Đọc code theo thứ tự nào (cho người mới)

1. `data/model/Models.kt` — dữ liệu trông như thế nào (`Assignment`, `Course`, `AppData`).
2. `ui/MainViewModel.kt` — mọi biến state và mọi việc app làm; đọc comment từng nhóm.
3. `MainActivity.kt` — đăng nhập hay chưa → màn nào; thanh tab; dialog nào đang mở.
4. `ui/screens/HomeScreen.kt` — một màn hình điển hình: đọc `viewModel.x`, bấm thì gọi `viewModel.hàm()`.
5. `data/repository/MoodleRepository.kt` → `data/remote/MoodleApi.kt` → `JsonMapper.kt` — dữ liệu đi từ server vào app.
6. `data/repository/LearningRules.kt` — quy tắc "sắp hạn / quá hạn / tiến độ".
7. `reminders/ReminderHelper.kt` + `ReminderReceiver.kt` — nhắc hạn nộp.
8. Backend: `backend/src/app.js` (danh sách API) → `services.js` (tính toán) → `db.js` (bảng).
