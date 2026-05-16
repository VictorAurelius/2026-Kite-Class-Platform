---
persona: p2-center-owner
topic: first-class
last-updated: 2026-05-16
version: v0.9.0-beta
effort_minutes: 5
---

# Tạo lớp học đầu tiên

> 📅 Cập nhật lần cuối: **2026-05-16** · Phiên bản KiteHub: **v0.9.0-beta** · Đọc khoảng **5 phút**

## TL;DR

Trang này hướng dẫn chị Hằng tạo lớp học đầu tiên sau onboarding wizard. Mục tiêu **end-to-end ≤5 phút** từ form đến lớp xuất hiện trong dashboard.

- 📚 **Bước 1:** Vào `/classes` → click "Tạo lớp mới"
- 📝 **Bước 2:** Điền thông tin lớp (tên, môn, level, sĩ số)
- 🗓️ **Bước 3:** Cấu hình lịch học (thứ + giờ + ngày khai giảng)
- 👨‍🏫 **Bước 4:** Gán giáo viên (Nguyễn Văn An / Phạm Thị Mai)
- 💰 **Bước 5:** Đặt giá khoá học (1.500.000đ / khoá)
- ✅ **Bước 6:** Save → lớp xuất hiện trong danh sách `/classes`

---

## 1. Truy cập trang Quản lý lớp

<!-- Screenshot placeholder: capture first-class-step-1.png — 1440×900 vi-VN — show /classes empty state với hero "Chưa có lớp nào — Tạo lớp đầu tiên" + nút CTA "Tạo lớp mới" + mũi tên đỏ chỉ vào nút + số bước "1" overlay. -->

Từ Dashboard → sidebar click **"Lớp học"** hoặc truy cập `/classes`.

Lần đầu chưa có lớp, trang hiển thị **empty state**:

- 🎓 Icon lớp học lớn
- 📝 Tiêu đề: "Chưa có lớp nào — Hãy tạo lớp đầu tiên!"
- 🔘 Nút CTA primary: **"Tạo lớp mới"**

Click nút **"Tạo lớp mới"** → modal/dialog form mở.

---

## 2. Điền thông tin lớp

<!-- Screenshot placeholder: capture first-class-step-2.png — 1440×900 vi-VN — show modal "Tạo lớp mới" với form 4 trường (Tên lớp / Môn / Level / Sĩ số tối đa) + sample data "Lớp Anh ngữ 5A1" / "English" / "Pre-Intermediate" / "20" + viền vàng khoanh field "Tên lớp" + số bước "2". -->

Form yêu cầu 4 thông tin cơ bản:

| Trường | Ví dụ | Ghi chú |
|---|---|---|
| **Tên lớp** | Lớp Anh ngữ 5A1 | Nên có môn + level + nhóm (vd `5A1` = lớp 5, nhóm A, slot 1) |
| **Môn học** | English / Toán / Vật lý | Dropdown từ danh sách môn đã cấu hình trong `/settings/subjects` |
| **Level / Cấp độ** | Pre-Intermediate | Beginner / Elementary / Pre-Intermediate / Intermediate / Upper / Advanced |
| **Sĩ số tối đa** | 20 | Số học sinh tối đa — KiteHub auto-warn khi đầy 90% |

Click **"Tiếp theo"** chuyển sang Bước 3 (lịch học).

---

## 3. Cấu hình lịch học

<!-- Screenshot placeholder: capture first-class-step-3.png — 1440×900 vi-VN — show modal step 2 với calendar widget chọn ngày khai giảng + checkboxes 7 ngày trong tuần (T2/T4/T6 checked) + time picker (18:00-19:30) + sample data. -->

Bước 3 cấu hình **lịch học định kỳ**:

| Trường | Ví dụ |
|---|---|
| **Ngày khai giảng** | `Thứ Hai, 02/06/2026` |
| **Ngày kết thúc** | `Thứ Sáu, 28/08/2026` (3 tháng = 1 khoá) |
| **Các ngày trong tuần** | ☑ Thứ 2 ☑ Thứ 4 ☑ Thứ 6 (T3/T5/T7/CN bỏ trống) |
| **Giờ bắt đầu** | `18:00` |
| **Giờ kết thúc** | `19:30` |
| **Phòng học** | Phòng 201 (tuỳ chọn) |

**Hệ thống auto-tính:** 3 buổi/tuần × ~12 tuần = **36 buổi/khoá**.

Click **"Tiếp theo"** chuyển sang Bước 4 (giáo viên).

---

## 4. Gán giáo viên

<!-- Screenshot placeholder: capture first-class-step-4.png — 1440×900 vi-VN — show modal step 3 với multi-select dropdown chọn giáo viên (Nguyễn Văn An — đã chọn / Phạm Thị Mai — gợi ý) + avatar + workload bar "An: 12/20 giờ/tuần". -->

Form hiển thị danh sách giáo viên đã mời (qua [invite-staff](invite-staff.md)):

| Giáo viên | Workload hiện tại | Phù hợp |
|---|---|---|
| Nguyễn Văn An | 12/20 giờ/tuần | ✅ Còn slot |
| Phạm Thị Mai | 18/20 giờ/tuần | ⚠️ Gần đầy |
| Lê Hoàng Quân | 20/20 giờ/tuần | ❌ Đầy — cảnh báo đỏ |

Có thể chọn **1-2 giáo viên** (chính + trợ giảng). Nguyễn Văn An là chính → click avatar để select.

KiteHub auto-validate: nếu lịch trùng giáo viên khác → **báo conflict + suggest đổi giờ**.

Click **"Tiếp theo"** chuyển sang Bước 5 (giá).

---

## 5. Đặt giá khoá học

<!-- Screenshot placeholder: capture first-class-step-5.png — 1440×900 vi-VN — show modal step 4 với input giá VND + preview "1.500.000 ₫" + dropdown payment plan (full / split 2 / split 3) + sample data. -->

Bước 5 cấu hình **giá + payment plan**:

| Trường | Ví dụ |
|---|---|
| **Học phí cả khoá** | `1.500.000đ` (1.5 triệu / 36 buổi ≈ 42k/buổi) |
| **Payment plan** | ☑ Trả 1 lần (giảm 5%) ☑ Trả 2 lần ☐ Trả 3 lần |
| **Hạn đóng học phí** | 7 ngày sau ngày khai giảng |
| **Phí huỷ** | 20% học phí nếu huỷ trong tuần đầu, 50% sau đó |

**Hiển thị giá**:
- Format VND: `1.500.000đ` hoặc `1.500.000 ₫`
- Học sinh đăng ký thấy giá này khi vào `https://sky-education.kiteclass.com/classes/anh-ngu-5a1`

Click **"Lưu lớp"** → save + chuyển về `/classes`.

---

## 6. Verify lớp xuất hiện trong danh sách

<!-- Screenshot placeholder: capture first-class-step-6.png — 1440×900 vi-VN — show /classes với card lớp mới tạo "Lớp Anh ngữ 5A1" + badge "Mở đăng ký" + thông tin (giáo viên / sĩ số 0/20 / khai giảng 02/06) + mũi tên đỏ chỉ vào card. -->

Quay lại `/classes`, lớp mới hiển thị dưới dạng card:

- 🎓 **Lớp Anh ngữ 5A1** · Pre-Intermediate
- 👨‍🏫 GV: Nguyễn Văn An
- 📅 T2/T4/T6 · 18:00-19:30 · KG `02/06/2026`
- 👥 Sĩ số: 0/20 (chưa có học sinh đăng ký)
- 💰 1.500.000đ / khoá
- 🟢 Badge: **"Mở đăng ký"**

Học sinh + phụ huynh truy cập `https://sky-education.kiteclass.com/classes/anh-ngu-5a1` → đăng ký + thanh toán → tự động vào danh sách lớp.

---

## 7. Thời gian thực tế

| Bước | Thời gian ước tính |
|---|---|
| Bước 1 (truy cập + click CTA) | 15 giây |
| Bước 2 (thông tin lớp) | 1 phút |
| Bước 3 (lịch học) | 1 phút |
| Bước 4 (gán giáo viên) | 30 giây |
| Bước 5 (giá + payment plan) | 1 phút |
| Bước 6 (verify) | 15 giây |
| **Tổng** | **~4 phút** |

Mục tiêu **end-to-end ≤5 phút** đạt được nếu chị đã có sẵn thông tin (tên môn / giá / giáo viên). Nếu chưa cấu hình môn → cần thêm ~3 phút cho `/settings/subjects` trước.

---

## 8. Bước tiếp theo

Sau khi có lớp đầu tiên:

- 🎨 [AI Branding](branding.md) — Tạo logo + theme cho trang tenant
- 👥 [Mời học sinh](invite-staff.md#student) — Gửi link đăng ký lớp cho phụ huynh
- 📊 [Báo cáo doanh thu](../p3-manager/reports.md) — Tracking học phí thu được
- ⚙️ [Cấu hình môn học](settings.md#subjects) — Thêm môn vào dropdown

---

## 9. Troubleshooting

| Vấn đề | Cách khắc phục |
|---|---|
| Không thấy nút "Tạo lớp mới" | Kiểm tra role — chỉ OWNER + STAFF có quyền (xem [permissions](../p3-manager/permissions.md)) |
| Conflict lịch giáo viên | Đổi giờ hoặc chọn giáo viên khác — hệ thống tự suggest |
| Lưu xong không thấy lớp | F5 refresh / clear cache / báo support nếu kéo dài >30 giây |
| Học phí không hiển thị VND đúng | Kiểm tra `/settings/billing` — locale phải là `vi-VN` |
| Giáo viên báo "trùng lịch" sai | Verify lịch ở `/team/{teacher-id}/schedule` — có thể stale cache |

---

## 🆘 Cần hỗ trợ?

- 📧 Email: [support@kitehub.me](mailto:support@kitehub.me)
- 💬 Zalo OA: zalo.me/kitehub (đang triển khai — Phase 1.5)
- 📞 Hotline beta: 1900-xxxx (giờ hành chính)
- 🐛 Báo lỗi trang này: [mailto:support@kitehub.me?subject=Lỗi /help/p2-owner/first-class](mailto:support@kitehub.me?subject=L%E1%BB%97i%20%2Fhelp%2Fp2-owner%2Ffirst-class)
- 📊 Trạng thái beta: [/beta-status](/beta-status)
