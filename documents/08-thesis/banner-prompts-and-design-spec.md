---
title: Banner AI prompts + Landing design spec (wave-thesis-4)
audience: dev
last-updated: 2026-05-30
---

# Banner AI Prompts + Landing Design Spec

Tham chiếu 2 landing chuyên nghiệp: [mshoajunior.edu.vn](https://mshoajunior.edu.vn/) + [anhngumshoa.com](https://www.anhngumshoa.com/).

---

## Phần 1 — Chuẩn design rút ra từ 2 trang mẫu (để nâng template)

| Yếu tố | 2 trang mẫu làm gì | Template hiện tại | Cần cải thiện |
|---|---|---|---|
| **Hero** | Full-width, nền gradient xanh→trắng (hoặc xanh→tím), slogan UPPERCASE lớn, ảnh người/trẻ tích hợp, CTA cam/vàng nổi bật | Banner card cột phải + text trái | Hero full-width có gradient overlay + ảnh người làm nền/bên cạnh + CTA cam to |
| **Counter** | Số TO bold (5 năm / 300 GV / 50k HV), animate đếm lên | Stats card tĩnh nhỏ | Phóng to số, thêm animate count-up |
| **CTA** | Cam/vàng, UPPERCASE, rải nhiều nơi (hero + giữa + cuối) | 1 CTA "học thử" | CTA cam nổi bật + lặp ≥2 vị trí |
| **Card** | Bo góc 8-12px, shadow nhẹ, ảnh tròn GV | Card cơ bản | Tăng shadow + bo góc + ảnh GV tròn viền |
| **Spacing** | Padding section 40-60px, nhiều breathing room | Padding py-16 (~64px) OK | Giữ; thêm divider rõ (đã có zebra) |
| **Typography** | Montserrat/Roboto bold, H1 28-48px | Be Vietnam Pro | Tăng cỡ H1 hero + weight bold |
| **Trust signals** | Badge "Top đầu VN", logo media, đối tác | Chưa có | Thêm badge cam khẩu hiệu + trust row |
| **Màu CTA** | Cam #F97316 / vàng tương phản nền xanh | accent theo theme | Thêm CTA accent cam tương phản |

**Lưu ý GV độc lập (khác trung tâm):** KHÔNG bịa "50k học viên / 300 GV". Counter dùng số thật khiêm tốn (9 năm KN / 500 HS đã dạy / 95% đạt mục tiêu — đã seed). Giữ chuyên nghiệp nhưng trung thực quy mô cá nhân.

---

## Phần 2 — 2 Prompt tạo banner cho ChatGPT Plus (DALL-E / GPT-4o image)

**Cách dùng:** Upload ảnh chân dung GV (`documents/08-thesis/portrait/<tên GV>.png`) lên ChatGPT Plus, dán prompt tương ứng. Ảnh ra dùng làm hero banner (`hero_image_url`). Tỉ lệ khuyến nghị **16:9 ngang** (hoặc 1.9:1 khớp container hero hiện tại).

### Prompt A — "Người giơ tay quyết tâm" (hero tự tin)

```
Tạo một ảnh banner ngang tỉ lệ 16:9 cho trang chủ website giáo dục, phong cách
chuyên nghiệp hiện đại giống các trung tâm tiếng Anh hàng đầu Việt Nam.

Dùng GƯƠNG MẶT trong ảnh chân dung tôi vừa tải lên làm nhân vật chính (giữ đúng
khuôn mặt, giới tính, độ tuổi). Nhân vật mặc trang phục công sở lịch sự (áo sơ mi/
blazer), GIƠ MỘT TAY LÊN với biểu cảm TỰ TIN, QUYẾT TÂM, nụ cười thân thiện truyền
cảm hứng — tư thế như đang động viên "Bạn làm được!".

Bố cục: nhân vật đặt bên PHẢI khung hình, chừa khoảng trống bên TRÁI để đặt chữ slogan.
Nền gradient xanh dương đậm sang xanh nhạt/trắng, điểm thêm các hoạ tiết đồ hoạ giáo dục
mờ (sách, bảng, biểu tượng tốt nghiệp, đường nét bay bổng). Ánh sáng studio sáng, sạch.
Độ phân giải cao, sắc nét, phong cách banner marketing giáo dục.

Tông màu chủ đạo: [MÀU GV] (xem bảng dưới).
```

### Prompt B — "Thầy/cô đứng sau 3 học sinh vui vẻ" (ấm áp, lớp học)

```
Tạo một ảnh banner ngang tỉ lệ 16:9 cho trang chủ website giáo dục, phong cách ấm áp
chuyên nghiệp giống banner các trung tâm giáo dục uy tín Việt Nam.

Dùng GƯƠNG MẶT trong ảnh chân dung tôi vừa tải lên làm [GIÁO VIÊN] (giữ đúng khuôn mặt,
giới tính, độ tuổi). [Giáo viên] ĐỨNG PHÍA SAU, đặt tay lên vai 3 HỌC SINH [ĐỘ TUỔI]
đang VUI VẺ TƯƠI CƯỜI, các em mặc đồng phục/áo gọn gàng, biểu cảm hào hứng tự tin,
một vài em giơ ngón tay cái hoặc ôm sách vở.

Bố cục: nhóm người căn giữa-phải, chừa khoảng trống bên trái cho chữ. Nền lớp học sáng
sủa hiện đại (bảng, bàn ghế mờ phía sau) HOẶC nền gradient xanh nhẹ. Ánh sáng tự nhiên
ấm áp. Độ phân giải cao, chân thực, truyền cảm giác tin cậy và niềm vui học tập.

Tông màu chủ đạo: [MÀU GV] (xem bảng dưới).
```

### Bảng tham số per giảng viên

| GV | Portrait file | [ĐỘ TUỔI] học sinh | [MÀU GV] | Môn |
|---|---|---|---|---|
| **Cô Đỗ Lan Khánh** | `Đỗ Lan Khánh - THPT - Pháp Luật và Đời Sống.png` | học sinh THPT (16-18 tuổi) | xanh navy + vàng gold | Pháp luật & GDCD |
| **Cô Nguyễn Thị Hà** | `Nguyễn Thị Hà - Tiểu Học - Toán Học.png` | học sinh tiểu học (7-10 tuổi) | xanh dương tươi | Toán Tiểu học |
| **Thầy Nguyễn Đình Nhì** | `Nguyễn Đình Nhì - THCS - Hóa Học.png` | học sinh THCS (12-15 tuổi) | xanh lá | Hóa học |

**Gợi ý:** Prompt A hợp hero "quyết tâm/luyện thi" (cô Khánh THPT, thầy Nhì THCS luyện thi vào 10); Prompt B hợp "ấm áp/trẻ nhỏ" (cô Hà tiểu học). Có thể tạo cả 2 kiểu cho mỗi GV rồi chọn.

**Sau khi có ảnh:** lưu vào `documents/08-thesis/portrait/banners/<slug>.png` (ghi đè 3 banner cũ) → copy sang `kiteclass-frontend/public/demo-banners/` → landing tự dùng (hero_image_url đã trỏ sẵn).

---

## Phần 3 — Component cần fix (capture + UI review)

Sau khi nâng template theo Phần 1, capture qua headless (`verify-landing.mjs`) + review per-component. Checklist:

- [ ] Hero: gradient full-width + slogan lớn + CTA cam (hiện banner card nhỏ cột phải)
- [ ] CTA accent cam tương phản (hiện theo theme primary)
- [ ] Counter animate + phóng to số
- [ ] Card shadow + bo góc rõ hơn
- [ ] Trust badge cam (khẩu hiệu / cam kết)
- [ ] Typography H1 hero to + bold hơn
- [ ] Section divider rõ (đã có zebra — kiểm tra đủ tương phản)
