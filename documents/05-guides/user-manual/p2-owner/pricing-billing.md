---
persona: p2-center-owner
topic: pricing-billing
last-updated: 2026-05-15
version: v0.9.0-beta
effort_minutes: 6
---

# Bảng giá + Thanh toán — Hướng dẫn cho Chủ trung tâm

> 📅 Cập nhật lần cuối: **2026-05-15** · Phiên bản KiteHub: **v0.9.0-beta** · Đọc khoảng **6 phút**

## TL;DR

Sky Education đang dùng Beta FREE 6 tháng đầu. Sau 14/11/2026 cần chọn tier PRO hoặc PREMIUM. Hoá đơn được tự tạo cuối tháng, chị Hằng duyệt + thanh toán qua VNPay/MoMo/chuyển khoản.

- 💰 **Tier hiện tại:** BETA_FREE_6M (đến 14/11/2026)
- 📅 **Hoá đơn:** Tạo tự động ngày 1 mỗi tháng
- 💳 **Phương thức thanh toán:** VNPay · MoMo · Chuyển khoản ngân hàng
- 🔔 **Reminder:** Email + SMS 3 ngày trước hạn

---

## 1. Bảng giá KiteHub

<!-- Screenshot placeholder pending B+C merge: capture pricing-billing-step-1.png — 1440×900 vi-VN — show /billing/plans table 4 cột (FREE / PRO 990k / PREMIUM 2.5tr / ENTERPRISE) + nút "Upgrade tier" mũi tên đỏ -->

| Tier | Giá/tháng | Số học sinh | Tính năng |
|---|---|---|---|
| **FREE** | 0đ | ≤30 hs | Cơ bản (3 module) |
| **PRO** | 990.000đ | ≤150 hs | Full module + branding |
| **PREMIUM** | 2.500.000đ | ≤500 hs | Full + AI suggest + multi-branch |
| **ENTERPRISE** | Liên hệ | >500 hs | Custom + SLA + dedicated support |
| **BETA_FREE_6M** | 0đ | ≤500 hs | Full PRO/PREMIUM trong 6 tháng đầu |

Sky Education hiện đang dùng `BETA_FREE_6M` — đến 14/11/2026 sẽ tự động hỏi chị chọn tier.

### 1.1 So sánh PRO vs PREMIUM

| Tính năng | PRO | PREMIUM |
|---|:---:|:---:|
| Số học sinh | ≤150 | ≤500 |
| Số giáo viên | ≤10 | ≤30 |
| Branding logo + màu | ✅ | ✅ |
| AI tạo logo từ tên | ❌ | ✅ |
| Multi-branch (nhiều cơ sở) | ❌ | ✅ |
| Export PDF reports | ✅ | ✅ |
| Email transactional VN/EN | ✅ | ✅ |
| SLA 99.5% uptime | ✅ | ✅ |
| Hotline 24/7 | ❌ | ✅ |

Khuyến nghị: Sky Education 120 hs → PRO đủ. Khi tăng tới 200 hs → PREMIUM.

---

## 2. Xem hoá đơn pending

<!-- Screenshot placeholder pending B+C merge: capture pricing-billing-step-2.png — 1440×900 vi-VN — show /billing/invoices list với 3 invoice rows (Tháng 5/2026 1.500.000đ PENDING / Tháng 4/2026 1.500.000đ PAID / Tháng 3 PAID) + nút "Duyệt + thanh toán" mũi tên đỏ -->

URL: `/billing/invoices`

| Cột | Ý nghĩa |
|---|---|
| Mã hoá đơn | `INV-2026-05-001` |
| Tháng | `Tháng 5/2026` |
| Số tiền | `1.500.000đ` (PRO 990k + 510k phí extra học sinh) |
| Hạn thanh toán | `15/05/2026` |
| Status | `PENDING` / `PAID` / `OVERDUE` |
| Action | Nút "Duyệt + Thanh toán" |

Click hoá đơn → mở `/billing/invoices/{id}` xem chi tiết:
- Số hoá đơn + MST trung tâm + MST KiteHub
- Bảng kê (mã, mô tả, đơn giá, số lượng, thành tiền)
- Phương thức thanh toán đã chọn
- File PDF tải về cho kế toán

---

## 3. Thanh toán hoá đơn

### 3.1 Chọn phương thức

Click **Duyệt + Thanh toán** trong list → modal hiện 3 lựa chọn:

| Phương thức | Phí | Thời gian giao dịch |
|---|---|---|
| **VNPay (thẻ ATM/Credit)** | 0đ | Tức thì |
| **MoMo** | 0đ | Tức thì |
| **Chuyển khoản ngân hàng** | 0đ | T+1 ngày (manual verify) |

### 3.2 VNPay

<!-- Screenshot placeholder pending B+C merge: capture pricing-billing-step-3.png — 1440×900 vi-VN — show VNPay payment redirect với QR code + hoá đơn 1.500.000đ + mũi tên đỏ chỉ vào QR -->

Click **VNPay** → redirect tới cổng VNPay → chọn ngân hàng → nhập OTP → done.

Sau ~10 giây, KiteHub nhận webhook + update hoá đơn `status=PAID` + gửi email xác nhận chị Hằng.

### 3.3 MoMo

Tương tự VNPay nhưng dùng app MoMo. Chị quét QR trên màn hình hoặc đăng nhập app web.

### 3.4 Chuyển khoản

Khi chọn chuyển khoản, hệ thống hiển thị:
- Số tài khoản KiteHub: `9988-7766-5544` Vietcombank
- Số tiền chính xác: `1.500.000đ`
- Nội dung CK: `KH-INV-2026-05-001-SkyEdu` (verbatim)

Chị chuyển → ngày sau hệ thống verify + update `PAID`.

---

## 4. Phê duyệt hoá đơn cho giáo viên/cộng tác viên

Khi giáo viên submit invoice (lương + phụ cấp + bồi dưỡng), invoice vào `/billing/invoices?type=INTERNAL` chờ chị duyệt.

<!-- Screenshot placeholder pending B+C merge: capture pricing-billing-step-4.png — 1440×900 vi-VN — show internal invoice approve view "Cô Phạm Thị Mai - Tháng 5 lương 12.000.000đ + phụ cấp 800.000đ" + nút Duyệt mũi tên đỏ -->

Detail:
- Họ tên: Phạm Thị Mai
- Vị trí: Giáo viên Anh ngữ
- Lương cơ bản: 8.000.000đ
- Số buổi dạy: 24 buổi × 200.000đ = 4.800.000đ
- Phụ cấp: 800.000đ
- **Tổng cộng: 13.600.000đ**

Chị click **Duyệt** → trigger payment qua tài khoản giáo viên đã đăng ký. Anh Tâm KHÔNG có quyền duyệt invoice nội bộ — chỉ chị OWNER.

---

## 5. Báo cáo doanh thu hàng tháng

URL: `/billing/reports?period=monthly`

<!-- Screenshot placeholder pending B+C merge: capture pricing-billing-step-5.png — 1440×900 vi-VN — show monthly revenue chart 12 tháng + table breakdown (Học phí thu / Lương chi / Lợi nhuận thuần) + nút Export Excel mũi tên đỏ -->

Hiển thị:
- Tổng doanh thu (học phí thu được)
- Chi phí (lương GV, vận hành, KiteHub fee)
- Lợi nhuận thuần
- Trend 12 tháng line chart

Export Excel/PDF cho kế toán: click nút **Export** → chọn format → tải file về.

---

## 6. Troubleshooting

| Triệu chứng | Khả năng | Hành động |
|---|---|---|
| VNPay redirect lỗi "Giao dịch không thành công" | Hết tiền / OTP sai / Bank decline | Thử lại với card khác, hoặc dùng MoMo/chuyển khoản |
| Đã chuyển khoản nhưng hoá đơn vẫn PENDING sau 2 ngày | Manual verify chậm hoặc nội dung CK sai | Email support@kitehub.me kèm screenshot biên lai |
| Duyệt invoice nội bộ nhưng giáo viên không nhận tiền | Sai số tài khoản đã đăng ký | Kiểm tra trong `/team/{teacher-id}/banking` |
| Quên duyệt invoice → trễ trả lương | Setup auto-reminder 3 ngày trước hạn | Bật notification email trong `/settings/notifications` |

---

## 7. Liên kết

- [Tổng quan Chủ trung tâm](index.md)
- [Mời nhân viên](invite-staff.md)
- [Tuỳ chỉnh logo](branding.md)
- Bảng giá public: [/pricing](/pricing)

---

## 🆘 Cần hỗ trợ?

- 📧 Email: [support@kitehub.me](mailto:support@kitehub.me)
- 💬 Zalo OA: zalo.me/kitehub (đang triển khai)
- 📞 Hotline: 1900-xxxx (giờ hành chính)
- 📊 Trạng thái beta: [/beta-status](/beta-status)
