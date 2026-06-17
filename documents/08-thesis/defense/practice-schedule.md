---
title: Lịch tập bảo vệ — 2 buổi dry-run + danh sách kiểm tra trước bảo vệ
chapter: defense
audience: dev
status: draft
created: 2026-05-23
last-reviewed: 2026-05-23
---

# Lịch tập bảo vệ — 2 buổi dry-run trước bảo vệ

**Mục tiêu:** chuẩn bị tâm lý + canh giờ + luồng bảo vệ qua 2 buổi dry-run thực tế (T-3 tuần và T-2 tuần trước cửa sổ bảo vệ), cộng danh sách kiểm tra trước bảo vệ T-1 ngày.

**Cửa sổ bảo vệ dự kiến:** 15/08/2026 → 15/10/2026 (theo `thesis-info.md`).

Giả sử ngày bảo vệ cụ thể là **T-day**, lịch dry-run như sau:

| Buổi | Thời điểm | Mục tiêu chính | Người tham gia đề xuất |
|---|---|---|---|
| **Buổi 1** | T-day − 3 tuần | Toàn bộ deck + đi qua demo + canh giờ | GVHD (TS. Nguyễn Đức Dư) + 1 người phản biện |
| **Buổi 2** | T-day − 2 tuần | Luyện Q&A + tinh chỉnh slide sau Buổi 1 | 1 người phản biện + tự quay audio |
| **Kiểm tra trước bảo vệ** | T-day − 1 ngày | Danh sách kiểm tra cuối + hậu cần | Cá nhân |

---

## Buổi 1 — Toàn bộ deck + đi qua demo (T-3 tuần)

### Chuẩn bị trước Buổi 1 (1 tuần trước Buổi 1)

- [ ] Liên hệ GVHD đặt lịch — khung 90 phút cuối ngày trong tuần (Thứ 2-Thứ 6 16:00-18:00 ưu tiên)
- [ ] Liên hệ 1 người phản biện (đề xuất: bạn cùng lớp K63 ngành CNTT đã qua vòng bảo vệ trước, hoặc đàn anh đã hỗ trợ debug code) — khung trùng GVHD
- [ ] Gửi trước cho GVHD + người phản biện:
  - Deck Reveal.js dạng PDF (xuất qua `decktape` hoặc `chrome --headless --print-to-pdf`)
  - Link repository GitHub (chế độ chỉ-đọc cho người phản biện ngoài team)
  - Báo cáo chính bản PDF (phiên bản mới nhất)
- [ ] Đặt phòng họp UTC hoặc quán cafe phù hợp — có máy chiếu + Wi-Fi ổn định

### Buổi 1 — Lịch trình chi tiết (90 phút)

| Khung giờ | Hoạt động | Thời lượng | Ghi chú |
|---|---|---|---|
| 00:00-00:05 | Chuẩn bị laptop + máy chiếu + Wi-Fi | 5 phút | Kiểm thử slide deck + Reveal.js render Mermaid |
| 00:05-00:35 | **Đi qua slide deck** — 40 slide tiếng Việt | 30 phút | Canh mỗi slide ≤ 45 giây, đo bằng đồng hồ bấm giây |
| 00:35-00:50 | **Demo trực tiếp 15 phút** theo `defense-demo-script.md` | 15 phút | Bám sát canh giờ 6 pha |
| 00:50-01:10 | **Vòng phản hồi 1** — GVHD + người phản biện ghi chú | 20 phút | Ghi chú bằng giấy + quay audio |
| 01:10-01:30 | **Luyện Q&A nhẹ** — 5 câu ngẫu nhiên từ 20 câu trong tờ Q&A | 20 phút | Luyện trả lời ≤ 30 giây/câu |

### Mục tiêu cụ thể Buổi 1

1. **Canh giờ thực tế:** deck 30 phút + demo 15 phút = 45 phút tổng (Q&A bảo vệ thường thêm 15-20 phút sau). Nếu vượt 50 phút → phải cắt nội dung.

2. **Xác định slide nào dư thừa:** slide nào lặp lại nội dung slide khác → loại bỏ. Slide quá nặng chữ → định dạng lại thành gạch đầu dòng ngắn.

3. **Trôi chảy khi demo:** đi qua 6 pha mượt mà, không gặp lỗi bất ngờ. Nếu Bước nào lỗi → fix ngay trong tuần T-3 → T-2.

4. **Lấy phản hồi thẳng thắn:** GVHD + người phản biện phải nhận xét thẳng thắn. Hỏi cụ thể:
   - "Phần nào confusing nhất cho hội đồng không chuyên?"
   - "Slide nào em nói quá nhanh / quá chậm?"
   - "Demo bug nào em cần fix?"
   - "Câu Q&A nào em trả lời chưa convincing?"

### Sản phẩm bàn giao Buổi 1 (sau buổi họp)

- [ ] Danh sách phản hồi đầy đủ (ghi chú + bản gỡ băng audio)
- [ ] Kế hoạch hành động T-3 → T-2: top 5 slide cần fix + top 3 bước demo cần debug + top 5 câu trả lời Q&A cần viết lại
- [ ] Lên lịch Buổi 2 với 1 người phản biện (không cần GVHD lần 2 trừ khi có lo ngại lớn)

---

## Buổi 2 — Luyện Q&A + tinh chỉnh (T-2 tuần)

### Chuẩn bị trước Buổi 2 (1 tuần sau Buổi 1)

- [ ] Áp dụng phản hồi Buổi 1: fix slide + fix demo + viết lại tờ trả lời Q&A
- [ ] Liên hệ người phản biện (cùng người Buổi 1 hoặc khác) — khung 60 phút
- [ ] Chuẩn bị **20 câu Q&A từ `defense-qa-response-sheet.md`** + 5 câu bất ngờ (người phản biện tự nghĩ thêm)
- [ ] Chuẩn bị môi trường: laptop + tai nghe + microphone để quay audio luyện Q&A

### Buổi 2 — Lịch trình chi tiết (60 phút)

| Khung giờ | Hoạt động | Thời lượng | Ghi chú |
|---|---|---|---|
| 00:00-00:05 | Chuẩn bị + bắt đầu quay audio | 5 phút | OBS Studio hoặc QuickTime chỉ-audio |
| 00:05-00:30 | **Luyện Q&A vòng 1** — 10 câu từ 4 nhóm | 25 phút | Người phản biện đọc câu, em trả lời ≤ 30 giây, người phản biện canh giờ bằng đồng hồ |
| 00:30-00:50 | **Luyện Q&A vòng 2** — 10 câu còn lại + 5 câu bất ngờ người phản biện tự nghĩ | 20 phút | Đo canh giờ + nhịp độ + sự tự tin |
| 00:50-00:60 | **Tự đánh giá audio** — nghe lại + nhận xét | 10 phút | Xác định câu nào ngập ngừng / mơ hồ / sai bằng chứng |

### Mục tiêu cụ thể Buổi 2

1. **Luyện 20 Q&A × 3 lần:** mỗi câu trả lời được ≤ 30 giây cô đọng, dẫn bằng chứng cụ thể.

2. **Xác định câu yếu nhất:** câu nào em trả lời ngập ngừng, mơ hồ, hoặc bằng chứng sai → viết lại tờ trả lời trước Buổi 3 nếu có.

3. **Kiểm thử áp lực với câu bất ngờ:** 5 câu người phản biện tự nghĩ kiểm tra khả năng ứng phó tình huống không chuẩn bị trước. Cần áp dụng mẫu 4 phần trong `defense-qa-response-sheet.md` §Quy trình ứng phó.

4. **Xây dựng sự tự tin:** sau Buổi 2 em phải tự tin cho mọi câu trong 20 Q&A — không phải học thuộc, mà hiểu rõ bằng chứng và có thể giải thích linh hoạt.

### Sản phẩm bàn giao Buổi 2 (sau buổi họp)

- [ ] Bản quay audio luyện Q&A — phát lại trong tuần T-2 → T-1 vài lần để tự rèn
- [ ] Cập nhật tờ trả lời cuối sau khi luyện (commit thay đổi vào repo)
- [ ] Tự đánh giá mức độ tự tin theo từng nhóm:
  - Kiến trúc: __/10
  - NFR/Cơ sở dữ liệu: __/10
  - Nghiệp vụ/Tuân thủ: __/10
  - Quy trình/Tương lai: __/10
- [ ] Nếu bất kỳ nhóm nào < 7/10 → lên lịch buổi luyện nhỏ bổ sung trong tuần T-2 → T-1

---

## Tuần T-2 → T-1 — Tự rèn luyện

Trong tuần này, không cần họp với người ngoài — tập trung tự rèn luyện:

### Hàng ngày (15-30 phút/ngày)

- **Sáng:** đọc 1 nhóm trong tờ trả lời Q&A (5 câu) tiếng nhỏ, tự canh giờ bằng đồng hồ
- **Chiều:** nghe lại bản quay audio Buổi 2 1 lần
- **Tối:** xem lại slide deck 1 lần — nhớ luồng + chuyển cảnh

### Cuối tuần (60-90 phút)

- **Thứ 7:** toàn bộ deck + đi qua demo trước gương / camera laptop — quay video xem lại
- **Chủ nhật:** luyện Q&A 20 câu một mình (đặt câu hỏi cho mình + trả lời) + nghỉ ngơi

### Lỗi cần tránh trong tuần T-2 → T-1

- ❌ Cố gắng thêm slide mới giờ chót — slide deck phải khóa sau Buổi 2
- ❌ Học thuộc Q&A như văn mẫu — phải hiểu bằng chứng để trả lời linh hoạt
- ❌ Ăn uống do căng thẳng / thiếu ngủ → ảnh hưởng phong độ ngày bảo vệ
- ❌ Luyện liên tục 4 giờ/ngày → kiệt sức
- ✅ Phân bổ 30 phút/ngày × 7 ngày > 4 giờ/ngày × 1-2 ngày

---

## Danh sách kiểm tra trước bảo vệ (T-1 ngày)

### Hậu cần (sáng T-1)

- [ ] Xác nhận thời gian + phòng bảo vệ với khoa CNTT — gọi điện xác nhận lần cuối
- [ ] In 3 bản cứng báo cáo chính (cho 3 thành viên hội đồng thường gặp)
- [ ] In 1 bản cứng tờ trả lời Q&A (dự phòng khi quên bằng chứng)
- [ ] In 1 bản cứng `defense-demo-script.md` (dự phòng khi quên luồng demo)
- [ ] USB dự phòng: báo cáo PDF + slide deck PDF + bản ghi demo dự phòng MP4
- [ ] Mang theo: laptop chính + laptop dự phòng (nếu có) + sạc + chuột + bộ chuyển đổi HDMI/USB-C

### Kỹ thuật (chiều T-1)

- [ ] Kiểm thử slide deck trên laptop chính — toàn màn hình Reveal.js render OK
- [ ] Kiểm thử đi qua demo end-to-end 1 lần — tất cả 6 pha trôi chảy
- [ ] Kiểm thử phát bản ghi dự phòng trên laptop chính
- [ ] Xác minh sức khỏe stack production toàn bộ GREEN qua CloudWatch
- [ ] Sao lưu snapshot database trước bảo vệ — phòng khi demo vô tình làm hỏng dữ liệu
- [ ] Sạc laptop 100% + mang theo cáp sạc

### Tâm lý (tối T-1)

- [ ] Ăn nhẹ tối — không ăn quá no
- [ ] Đi ngủ trước 22:30 — đảm bảo 7-8 giờ ngủ
- [ ] Đặt báo thức 6:30 sáng T-day
- [ ] Chuẩn bị quần áo trang trọng sẵn — sơ mi + quần tây + giày tây
- [ ] KHÔNG luyện thêm Q&A tối T-1 — thư giãn xem phim / đọc sách nhẹ

---

## Danh sách kiểm tra ngày bảo vệ (T-day)

### Sáng T-day

- [ ] Dậy 6:30 — ăn sáng đầy đủ (cơm/bún + protein)
- [ ] Đến phòng bảo vệ **trước 30 phút** so với lịch
- [ ] Chuẩn bị laptop + máy chiếu + Wi-Fi tại phòng — kiểm thử 1 lần
- [ ] Mở tab browser nạp sẵn theo `defense-demo-script.md` §Pha 0 chuẩn bị
- [ ] Đặt giấy + bút ghi chú câu hỏi hội đồng
- [ ] Uống nước, thư giãn 10 phút trước khi vào phòng

### Trong buổi bảo vệ (40-60 phút thường)

- [ ] **5 phút đầu** — trình bày slide intro (slide 1-6) — tốc độ vừa, hơi chậm rãi vì hội đồng mới làm quen
- [ ] **25 phút tiếp** — nội dung slide (slide 7-38) — tốc độ chuẩn, 40-45 giây/slide
- [ ] **15 phút demo** — theo `defense-demo-script.md` 6 pha
- [ ] **Slide 39-40 + Q&A** — mời câu hỏi, áp dụng mẫu `defense-qa-response-sheet.md`
- [ ] Ghi chú câu hỏi hội đồng bằng giấy — phòng khi quên chi tiết
- [ ] Nếu không biết câu → áp dụng mẫu "Em xin tiếp thu"

### Sau buổi bảo vệ (T-day chiều/tối)

- [ ] Cảm ơn hội đồng + GVHD bằng tin nhắn trang trọng
- [ ] Ghi chú phản hồi hội đồng — bổ sung vào báo cáo phiên bản cuối nếu cần
- [ ] Nghỉ ngơi — bảo vệ xong là cột mốc lớn, không cần code thêm gì tối nay

---

## Phương án dự phòng

### Nếu Buổi 1 lỗi (canh giờ > 50 phút hoặc demo lỗi nghiêm trọng)

- Lên lịch **Buổi 1.5** tuần T-2.5 với chỉ GVHD (60 phút)
- Tập trung: cắt nội dung dư thừa + fix lỗi demo
- Đẩy Buổi 2 lùi 3-4 ngày để có thời gian áp dụng phản hồi

### Nếu cửa sổ bảo vệ thay đổi (khoa đẩy sớm hoặc trễ)

- Đẩy sớm > 2 tuần: gộp Buổi 1 + Buổi 2 thành 1 buổi cường độ cao 3 giờ
- Đẩy trễ > 4 tuần: lên lịch thêm Buổi 3 tinh chỉnh vào T-1 tuần

### Nếu GVHD bận / người phản biện không sẵn sàng

- Buổi 1: thay GVHD bằng cựu sinh viên đàn anh đã qua bảo vệ + 1 người phản biện
- Buổi 2: thay người phản biện bằng phỏng vấn thử do AI dẫn (quay giọng + tự nghe lại)

### Nếu ngày bảo vệ gặp sự cố kỹ thuật (laptop hỏng, máy chiếu không hoạt động)

- Chuyển sang laptop dự phòng
- Nếu máy chiếu hỏng → trình bày trực tiếp trên màn hình laptop (phóng to cỡ chữ)
- Nếu mạng hỏng → chuyển sang bản ghi dự phòng cho demo

---

## Log

- **2026-05-23 (Wave thesis-1 Bucket C):** File tạo cho defense preparation. 2 buổi dry-run (T-3 tuần Full deck + T-2 tuần Q&A drill) + pre-defense checklist T-1 ngày + defense day checklist + contingency plans.
