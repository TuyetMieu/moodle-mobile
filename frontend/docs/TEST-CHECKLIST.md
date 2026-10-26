# Checklist test tay – luồng chính (F-32)

## Chuẩn bị

1. Đặt `VITE_USE_MOCK=true` trong `.env.development`, sau đó chạy `pnpm dev`.
2. Mở http://localhost:8443 trong Chrome hoặc Edge.
3. Bật DevTools (F12) → chế độ điện thoại (Ctrl+Shift+M, ví dụ iPhone 12 Pro, 390×844) và tab **Network**.
4. Chạy các ca theo thứ tự. Ca sau dùng trạng thái của ca trước, trừ TC10.

Ghi chú: ở chế độ mock, số liệu trong cột "Kết quả lần chạy" có thể khác nếu ngày giờ máy khác, vì có bài sẽ thành quá hạn. Cần kiểm tra **chiều thay đổi** (tăng/giảm 1, % tăng), không phải con số tuyệt đối.

## Các ca test

| ID | Luồng | Các bước | Kết quả mong đợi |
|---|---|---|---|
| TC01 | Mở app | Mở trang. | Hiện "Đang tải dữ liệu…" rồi tới Home có mục "Up next". Network có 3 request `courses`, `assignments`, `timetable`. Console không có lỗi. |
| TC02 | Mark as done | Ghi lại số **To do** ở Home. Bấm thẻ To do → chọn "Mid-term practical" → **Mark as done**. | Nút đổi thành "Marked as complete". Toast "Web Development: …% complete · … tasks left." **hiện trên modal**. Đóng modal: To do giảm 1. |
| TC03 | Số liệu Course | Vào **Courses**, xem thẻ Web Application Development, rồi mở môn đó. | % trên thẻ tăng so với trước TC02. Vòng tiến độ trong trang môn hiện đúng % mới. |
| TC04 | Số liệu Tracker | Vào **Tracker**. | "Overall completion" tăng so với trước TC02. |
| TC05 | Mất mạng | Network → **Offline**. Bấm thẻ To do → chọn "Quiz 05" → **Mark as done**. Chuyển lại **No throttling**, bấm **Thử lại** trên toast. | Banner "Mất kết nối…" hiện ở đầu màn hình. My classwork vẫn hiện danh sách cũ kèm cảnh báo. Mark as done: nút trở lại "Mark as done" và toast đỏ có nút **Thử lại** (bấm được, không bị modal che). Có mạng lại: banner ẩn; Thử lại → "Marked as complete", To do giảm 1. |
| TC06 | Lọc classwork | Mở My classwork, lần lượt bấm Due soon → Done → To do, rồi bấm môn "Molecular Biology". | Mỗi lần đổi tab hoặc môn đều có request `assignments` mới. Số hàng trong danh sách khớp với số trên tab. Lọc môn chỉ còn bài của Molecular Biology. |
| TC07 | Chuyển tuần timetable | Vào **Lịch học**. Bấm **Tuần sau**, rồi **Tuần trước**. Bấm Tuần sau rồi **Hôm nay**. Chuyển sang **Tháng**, bấm **Tháng sau**. | Khoảng ngày ở thẻ đầu đổi theo tuần và quay lại đúng tuần cũ. Nút Hôm nay đưa về tuần hiện tại và bị mờ. Chế độ tháng có chấm màu theo môn. |
| TC08 | Mở Course từ timetable | Chọn một ngày có chấm (có lịch), chạm vào một buổi học. | Mở trang của đúng môn đó. |
| TC09 | Tải report | Tracker → nút tải (View grade report) → **Download sample report**. | Tải về file `USTH-semester-2-sample-report.csv`, có dòng tiêu đề và 3 dòng môn. Toast "Sample grade report downloaded." |
| TC10 | Reload trang | Nhấn F5. | Không màn trắng; Home hiện lại. Ở chế độ mock, các bài đã Mark as done quay về như ban đầu vì mock không lưu. Với backend thật, trạng thái phải được giữ. |

## Kết quả lần chạy

- **Ngày chạy:** 26/10/2026
- **Môi trường:** chế độ mock, Edge headless, khung hình 390×844
- **Cách chạy:** tự động hoá các bước trên bằng Playwright

| ID | Kết quả | Ghi nhận |
|---|---|---|
| TC01 | ✅ Pass | Có đủ 3 request mock, console sạch. |
| TC02 | ✅ Pass | To do 6 → 5; toast hiện trên modal. |
| TC03 | ✅ Pass | Web Application Development 80% → 90%. |
| TC04 | ✅ Pass | Overall completion 79% → 83%. |
| TC05 | ✅ Pass | Banner hiện; classwork offline vẫn có 5 bài; rollback và toast có Thử lại; khi có mạng, Thử lại thành công (To do 5 → 4). |
| TC06 | ✅ Pass | To do 4 · Due soon 2 · Done 3; lọc Molecular Biology còn 2 bài. |
| TC07 | ✅ Pass | 26/10–01/11 → 02/11–08/11 và quay lại; chế độ tháng có chấm màu. |
| TC08 | ✅ Pass | Chạm buổi học mở trang Molecular Biology. |
| TC09 | ✅ Pass | CSV có 3 môn. |
| TC10 | ✅ Pass | Reload không lỗi; To do về 6 (giới hạn đã biết của mock). |

### Lỗi phát hiện khi chạy và đã sửa

1. **Toast bị modal che, nút "Thử lại" không bấm được (F-16, F-31).** Modal dùng `<dialog>.showModal()`: dialog nằm trên cùng và làm mọi thứ ngoài nó thành *inert*. Toast và banner "Mất kết nối" vì thế bị che và không bấm được.
   - **Sửa:** toast và banner giờ được render bên trong dialog đang mở dưới dạng `popover="manual"` (component `TopLayer` trong `App.tsx`).
2. **My classwork thành ngõ cụt khi offline (F-15, F-31).** Trước đây chỉ hiện "Couldn't load assignments", nên không mở được bài nào.
   - **Sửa:** khi lỗi, danh sách dùng dữ liệu đã tải lúc khởi động và hiện cảnh báo kèm nút Try again, giống màn Timetable.
