---
title: Defense Practice Schedule — 2 buổi dry-run + pre-defense checklist
chapter: defense
audience: thesis
status: draft
created: 2026-05-23
last-reviewed: 2026-05-23
---

# Defense Practice Schedule — 2 buổi dry-run pre-defense

**Mục tiêu:** chuẩn bị tâm lý + timing + flow defense qua 2 buổi dry-run thực tế (T-3 tuần và T-2 tuần trước defense window), cộng pre-defense checklist T-1 ngày.

**Defense window dự kiến:** 15/08/2026 → 15/10/2026 (per `thesis-info.md`).

Giả sử defense ngày cụ thể là **T-day**, lịch dry-run như sau:

| Buổi | Timing | Mục tiêu chính | Audience đề xuất |
|---|---|---|---|
| **Buổi 1** | T-day − 3 tuần | Full deck + demo walkthrough + timing | GVHD (TS. Nguyễn Đức Dư) + 1 peer reviewer |
| **Buổi 2** | T-day − 2 tuần | Q&A drill + tinh chỉnh slide sau Buổi 1 | 1 peer + tự record audio |
| **Pre-defense check** | T-day − 1 ngày | Final checklist + logistics | Cá nhân |

---

## Buổi 1 — Full Deck + Demo Walkthrough (T-3 tuần)

### Pre-Buổi 1 setup (1 tuần trước Buổi 1)

- [ ] Liên hệ GVHD đặt lịch — slot 90 phút cuối ngày trong tuần (Mon-Fri 16:00-18:00 ưu tiên)
- [ ] Liên hệ 1 peer reviewer (đề xuất: bạn cùng lớp K63 ngành CNTT đã pass defense round trước, hoặc senior đã hỗ trợ debug code) — slot trùng GVHD
- [ ] Gửi trước cho GVHD + peer reviewer:
  - PDF Reveal.js deck (export qua `decktape` hoặc `chrome --headless --print-to-pdf`)
  - Link repository GitHub (read-only mode cho reviewer ngoài team)
  - Báo cáo chính bản PDF (latest version)
- [ ] Đặt phòng họp UTC hoặc cafe phù hợp — có projector + Wi-Fi ổn định

### Buổi 1 — Agenda chi tiết (90 phút)

| Slot | Hoạt động | Thời lượng | Note |
|---|---|---|---|
| 00:00-00:05 | Setup laptop + projector + Wi-Fi | 5 phút | Test slide deck + Reveal.js render Mermaid |
| 00:05-00:35 | **Slide deck walkthrough** — 40 slide tiếng Việt | 30 phút | Time mỗi slide ≤ 45 giây, đo bằng đồng hồ bấm giây |
| 00:35-00:50 | **Live demo 15 phút** theo `defense-demo-script.md` | 15 phút | Bám sát timing 6 phase |
| 00:50-01:10 | **Feedback round 1** — GVHD + peer note | 20 phút | Take note bằng giấy + audio recording |
| 01:10-01:30 | **Q&A drill light** — 5 câu random từ 20 Q&A sheet | 20 phút | Practice trả lời ≤ 30 giây/câu |

### Mục tiêu cụ thể Buổi 1

1. **Timing realistic:** deck 30 phút + demo 15 phút = 45 phút tổng (Q&A defense thường thêm 15-20 phút sau). Nếu vượt 50 phút → phải cắt nội dung.

2. **Identify slide nào dư thừa:** slide nào lặp lại nội dung slide khác → loại bỏ. Slide quá nặng text → reformat thành bullet ngắn.

3. **Demo fluency:** đi qua 6 phase mượt mà, không gặp surprise bug. Nếu Step nào bug → fix ngay trong tuần T-3 → T-2.

4. **Get honest feedback:** GVHD + peer phải critique thẳng thắn. Hỏi cụ thể:
   - "Phần nào confusing nhất cho hội đồng không chuyên?"
   - "Slide nào em nói quá nhanh / quá chậm?"
   - "Demo bug nào em cần fix?"
   - "Câu Q&A nào em trả lời chưa convincing?"

### Buổi 1 deliverables (post-meeting)

- [ ] List feedback đầy đủ (note + audio transcript)
- [ ] Action plan T-3 → T-2: top 5 slide cần fix + top 3 demo step cần debug + top 5 Q&A response cần rewrite
- [ ] Schedule Buổi 2 với 1 peer (không cần GVHD lần 2 trừ khi có concern major)

---

## Buổi 2 — Q&A Drill + Polish (T-2 tuần)

### Pre-Buổi 2 setup (1 tuần sau Buổi 1)

- [ ] Apply feedback Buổi 1: fix slide + fix demo + rewrite Q&A response sheet
- [ ] Liên hệ peer reviewer (cùng người Buổi 1 hoặc khác) — slot 60 phút
- [ ] Chuẩn bị **20 câu Q&A từ `defense-qa-response-sheet.md`** + 5 câu bất ngờ (peer tự nghĩ thêm)
- [ ] Setup môi trường: laptop + headphones + microphone để record audio Q&A drill

### Buổi 2 — Agenda chi tiết (60 phút)

| Slot | Hoạt động | Thời lượng | Note |
|---|---|---|---|
| 00:00-00:05 | Setup + audio record start | 5 phút | OBS Studio hoặc QuickTime audio-only |
| 00:05-00:30 | **Q&A drill round 1** — 10 câu từ 4 archetype | 25 phút | Peer đọc câu, em trả lời ≤ 30 giây, peer time bằng đồng hồ |
| 00:30-00:50 | **Q&A drill round 2** — 10 câu còn lại + 5 câu bất ngờ peer tự nghĩ | 20 phút | Đo timing + nhịp độ + sự tự tin |
| 00:50-00:60 | **Self-review audio** — nghe lại + critique | 10 phút | Identify câu nào ngập ngừng / mơ hồ / sai evidence |

### Mục tiêu cụ thể Buổi 2

1. **Drill 20 Q&A × 3 lần:** mỗi câu trả lời được ≤ 30 giây cô đọng, cite evidence cụ thể.

2. **Identify câu yếu nhất:** câu nào em trả lời ngập ngừng, mơ hồ, hoặc evidence sai → rewrite response sheet trước Buổi 3 nếu có.

3. **Stress test với câu bất ngờ:** 5 câu peer tự nghĩ kiểm tra khả năng ứng phó situation không chuẩn bị trước. Cần áp dụng template 4 phần trong `defense-qa-response-sheet.md` §Quy trình ứng phó.

4. **Build confidence:** sau Buổi 2 em phải tự tin cho mọi câu trong 20 Q&A — không phải học thuộc, mà hiểu rõ evidence và có thể giải thích linh hoạt.

### Buổi 2 deliverables (post-meeting)

- [ ] Audio recording Q&A drill — playback trong tuần T-2 → T-1 vài lần để self-coach
- [ ] Final response sheet update sau drill (commit changes vào repo)
- [ ] Confidence rating self-assessment per archetype:
  - Architecture: __/10
  - NFR/Database: __/10
  - Business/Compliance: __/10
  - Process/Future: __/10
- [ ] Nếu bất kỳ archetype < 7/10 → schedule mini-drill bổ sung trong tuần T-2 → T-1

---

## Tuần T-2 → T-1 — Self-coaching

Trong tuần này, không cần meeting external — focus self-coaching:

### Hàng ngày (15-30 phút/ngày)

- **Sáng:** đọc 1 archetype Q&A response sheet (5 câu) tiếng nhỏ, time mình bằng đồng hồ
- **Chiều:** nghe lại audio recording Buổi 2 1 lần
- **Tối:** review slide deck 1 lần — nhớ flow + transition

### Cuối tuần (60-90 phút)

- **Thứ 7:** full deck + demo walkthrough trước gương / camera laptop — quay video xem lại
- **Chủ nhật:** Q&A drill 20 câu solo (đặt câu hỏi cho mình + trả lời) + nghỉ ngơi

### Anti-patterns tránh trong tuần T-2 → T-1

- ❌ Cố gắng thêm slide mới giờ chót — slide deck phải lock sau Buổi 2
- ❌ Học thuộc Q&A như văn mẫu — phải hiểu evidence để trả lời linh hoạt
- ❌ Stress eating / thiếu ngủ → ảnh hưởng performance defense day
- ❌ Drill liên tục 4 giờ/ngày → burnout
- ✅ Phân bổ 30 phút/ngày × 7 ngày > 4 giờ/ngày × 1-2 ngày

---

## Pre-Defense Checklist (T-1 ngày)

### Logistics (sáng T-1)

- [ ] Confirm timing + phòng defense với khoa CNTT — gọi điện xác nhận lần cuối
- [ ] Print 3 bản cứng báo cáo chính (cho 3 thành viên hội đồng thường gặp)
- [ ] Print 1 bản cứng Q&A response sheet (backup khi quên evidence)
- [ ] Print 1 bản cứng `defense-demo-script.md` (backup khi quên flow demo)
- [ ] USB backup: báo cáo PDF + slide deck PDF + backup demo recording MP4
- [ ] Mang theo: laptop chính + laptop dự phòng (nếu có) + sạc + chuột + adapter HDMI/USB-C

### Technical (chiều T-1)

- [ ] Test slide deck trên laptop chính — full screen Reveal.js render OK
- [ ] Test demo walkthrough end-to-end 1 lần — tất cả 6 phase fluent
- [ ] Test backup recording play trên laptop chính
- [ ] Verify production stack health all GREEN qua CloudWatch
- [ ] Backup database snapshot pre-defense — phòng khi demo accidentally corrupt data
- [ ] Charge laptop 100% + mang theo cable sạc

### Tâm lý (tối T-1)

- [ ] Ăn nhẹ tối — không ăn quá no
- [ ] Đi ngủ trước 22:30 — đảm bảo 7-8 giờ ngủ
- [ ] Set alarm 6:30 sáng T-day
- [ ] Chuẩn bị quần áo formal sẵn — sơ mi + quần tây + giày tây
- [ ] KHÔNG drill thêm Q&A T-1 tối — relax xem phim / đọc sách nhẹ

---

## Defense Day Checklist (T-day)

### Sáng T-day

- [ ] Dậy 6:30 — ăn sáng đầy đủ (cơm/bún + protein)
- [ ] Đến phòng defense **trước 30 phút** so với schedule
- [ ] Setup laptop + projector + Wi-Fi tại phòng — test 1 lần
- [ ] Mở browser tabs pre-loaded theo `defense-demo-script.md` §Phase 0 setup
- [ ] Đặt giấy + bút note câu hỏi hội đồng
- [ ] Uống nước, thư giãn 10 phút trước khi vào phòng

### Trong defense (40-60 phút thường)

- [ ] **5 phút đầu** — present slide intro (slide 1-6) — tốc độ vừa, hơi chậm rãi vì hội đồng mới làm quen
- [ ] **25 phút tiếp** — slide content (slide 7-38) — tốc độ chuẩn, 40-45 giây/slide
- [ ] **15 phút demo** — theo `defense-demo-script.md` 6 phase
- [ ] **Slide 39-40 + Q&A** — mời câu hỏi, áp dụng template `defense-qa-response-sheet.md`
- [ ] Take note câu hỏi hội đồng bằng giấy — phòng khi quên detail
- [ ] Nếu không biết câu → áp dụng "Em xin tiếp thu" template

### Sau defense (T-day chiều/tối)

- [ ] Cảm ơn hội đồng + GVHD bằng tin nhắn formal
- [ ] Take note feedback hội đồng — bổ sung vào báo cáo final version nếu cần
- [ ] Nghỉ ngơi — defense xong là milestone lớn, không cần code thêm gì tối nay

---

## Contingency Plans

### Nếu Buổi 1 fail (timing > 50 phút hoặc demo bug critical)

- Schedule **Buổi 1.5** tuần T-2.5 với chỉ GVHD (60 phút)
- Focus: cắt nội dung dư thừa + fix demo bug
- Push Buổi 2 lùi 3-4 ngày để có thời gian apply feedback

### Nếu defense window thay đổi (khoa đẩy sớm hoặc trễ)

- Đẩy sớm > 2 tuần: gộp Buổi 1 + Buổi 2 thành 1 buổi intensive 3 giờ
- Đẩy trễ > 4 tuần: schedule thêm Buổi 3 polish vào T-1 tuần

### Nếu GVHD bận / peer không available

- Buổi 1: thay GVHD bằng senior alumni đã pass defense + 1 peer
- Buổi 2: thay peer bằng AI-driven mock interview (record voice + tự nghe lại)

### Nếu defense day technical fail (laptop hỏng, projector không hoạt động)

- Switch laptop dự phòng
- Nếu projector hỏng → present trên màn hình laptop directly (zoom font size)
- Nếu network hỏng → switch backup recording cho demo

---

## Log

- **2026-05-23 (Wave thesis-1 Bucket C):** File tạo cho defense preparation. 2 buổi dry-run (T-3 tuần Full deck + T-2 tuần Q&A drill) + pre-defense checklist T-1 ngày + defense day checklist + contingency plans.
