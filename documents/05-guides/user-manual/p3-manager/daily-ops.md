---
persona: p3-center-manager
topic: daily-ops
last-updated: 2026-05-16
version: v0.9.0-beta
effort_minutes: 6
---

# Daily Operations — Việc hàng ngày của Quản lý

> 📅 Cập nhật lần cuối: **2026-05-16** · Phiên bản KiteHub: **v0.9.0-beta** · Đọc khoảng **6 phút**

## TL;DR

Trang này hướng dẫn anh Tâm (Quản lý Sky Education) chu trình làm việc hàng ngày trên KiteHub. Mỗi sáng anh Tâm có 4 việc chính (~30 phút).

- 📋 **8:00** Xem dashboard + duyệt request giáo viên xin nghỉ
- 🎓 **8:30** Kiểm tra lớp học hôm nay (giáo viên có mặt? phòng đủ?)
- ✏️ **Trong ngày** Chấm công + nhập điểm + báo phụ huynh
- 📊 **Cuối ngày** Xuất báo cáo + chốt sổ ngày

---

## 1. Buổi sáng — Mở Dashboard

<!-- Screenshot placeholder: capture daily-ops-step-1.png — 1440×900 vi-VN — show /dashboard 7:50 sáng với 4 KPI cards (Hoá đơn pending 8 / Đơn xin nghỉ 3 / Chấm công hôm qua 92% / Học sinh mới tuần 5) + alert badge đỏ trên "Đơn xin nghỉ". -->

Anh Tâm mở browser truy cập `https://kitehub.me/dashboard`. Dashboard Manager hiển thị:

| KPI Card | Hành động |
|---|---|
| 📋 **Hoá đơn pending: 8** | Forward sang chị Hằng (Manager không duyệt billing) |
| 🏖️ **Đơn xin nghỉ: 3** | Review từng đơn → approve / reject (xem §2) |
| ✅ **Chấm công hôm qua: 92%** | Nếu <90% → contact giáo viên hỏi lý do |
| 👥 **Học sinh mới tuần: 5** | Verify thông tin + assign vào lớp phù hợp |

**Mẹo:** Set bookmark `/dashboard` + mở mỗi sáng 7:50 → 30 phút trước lớp đầu tiên (8:30).

---

## 2. Duyệt đơn xin nghỉ giáo viên

<!-- Screenshot placeholder: capture daily-ops-step-2.png — 1440×900 vi-VN — show /requests/leaves với danh sách 3 đơn (Phạm Thị Mai - 17/05 / Lê Hoàng Quân - 20/05 / Nguyễn Văn An - 22/05) + nút "Duyệt" + nút "Từ chối" + viền vàng khoanh nút "Duyệt". -->

Click card **"Đơn xin nghỉ"** → `/requests/leaves`. Danh sách hiển thị:

| Giáo viên | Ngày nghỉ | Lý do | Lớp ảnh hưởng | Đề xuất GV thay thế |
|---|---|---|---|---|
| Phạm Thị Mai | `Thứ Bảy, 17/05/2026` | Khám sức khoẻ định kỳ | Lớp Anh ngữ 4B | Nguyễn Văn An (free 14:00-15:30) |
| Lê Hoàng Quân | `Thứ Ba, 20/05/2026` | Việc gia đình | Lớp Toán 9A1 | Phạm Thị Mai |
| Nguyễn Văn An | `Thứ Năm, 22/05/2026` | Bận họp phụ huynh | Lớp Anh ngữ 5A1 | — (cần tìm thêm) |

Mỗi đơn 3 hành động:
- ✅ **Duyệt + auto-assign GV thay thế** (nếu KiteHub đã suggest)
- ✅ **Duyệt + chỉ huỷ buổi học** (báo phụ huynh nghỉ bù)
- ❌ **Từ chối** + ghi lý do (giáo viên nhận email)

Click **"Duyệt"** → đơn chuyển sang `APPROVED` → email tự động báo giáo viên + phụ huynh.

---

## 3. Kiểm tra lớp học hôm nay

<!-- Screenshot placeholder: capture daily-ops-step-3.png — 1440×900 vi-VN — show /classes/today với 5 lớp hôm nay (8:00 / 10:00 / 14:00 / 16:00 / 18:00) + status indicator (xanh = OK / đỏ = thiếu giáo viên) + sample data. -->

Truy cập `/classes/today` (hoặc click "Lớp hôm nay" trong Dashboard):

| Giờ | Lớp | Giáo viên | Phòng | Sĩ số | Status |
|---|---|---|---|---|---|
| 08:00 | Lớp Anh ngữ 3A1 | Phạm Thị Mai | P101 | 15/20 | 🟢 OK |
| 10:00 | Lớp Toán 6B2 | Lê Hoàng Quân | P102 | 18/20 | 🟢 OK |
| 14:00 | Lớp Anh ngữ 4B | (thiếu — Mai nghỉ) | P101 | 12/15 | 🔴 Cần xử lý |
| 16:00 | Lớp Toán 9A1 | Lê Hoàng Quân | P102 | 8/15 | 🟡 Sĩ số thấp |
| 18:00 | Lớp Anh ngữ 5A1 | Nguyễn Văn An | P201 | 19/20 | 🟢 OK |

**Status 🔴:** Click lớp → assign giáo viên dạy thay → email báo phụ huynh.
**Status 🟡:** Sĩ số <50% — báo chị Hằng để cân nhắc gộp lớp.

---

## 4. Chấm công trong giờ học

<!-- Screenshot placeholder: capture daily-ops-step-4.png — 375×812 vi-VN MOBILE — show /classes/{id}/attendance trên mobile với danh sách 15 học sinh + checkbox "Có mặt" / "Vắng" / "Đi muộn" + sample VN names + viền vàng khoanh học sinh "Trần Thị Hằng" (đi muộn). -->

Giáo viên (hoặc Manager nếu cần) mở mobile app KiteHub → vào lớp đang dạy → màn hình chấm công:

- 📱 **Mobile-first** — list học sinh ≥44px tap target
- ✅ **3 trạng thái**: Có mặt / Vắng có phép / Vắng không phép / Đi muộn
- 📸 **Optional photo proof** — chụp ảnh lớp học (cho phụ huynh xem)
- ⏱️ **Sau 15 phút giờ học** — auto-warn nếu chưa chấm công

Click **"Lưu chấm công"** → push notification đến phụ huynh học sinh vắng.

---

## 5. Nhập điểm + nhận xét cuối buổi

<!-- Screenshot placeholder: capture daily-ops-step-5.png — 1440×900 vi-VN — show /classes/{id}/grades với inline editor 15 student × 3 columns (Điểm chuyên cần / Điểm bài tập / Nhận xét) + auto-save indicator + sample VN sample data. -->

Cuối buổi học (~30 phút sau giờ kết thúc), giáo viên / Manager mở `/classes/{id}/grades`:

| Học sinh | Chuyên cần (0-10) | Bài tập (0-10) | Nhận xét |
|---|---|---|---|
| Trần Thị Hồng | 10 | 9 | Tích cực phát biểu |
| Nguyễn Văn Bình | 8 | 7 | Cần luyện thêm phát âm |
| Phạm Thị Mai | 10 | 10 | Xuất sắc — đề nghị lên lớp cao hơn |

**Tính năng**:
- 💾 Auto-save sau mỗi 5 giây (no need click Save)
- 📤 Sau khi save xong → click "Gửi báo cáo phụ huynh" → email + SMS (tuỳ gói)
- 📊 Điểm tổng hợp tự động sang `/reports/students/{id}` cho tháng

---

## 6. Cuối ngày — Báo cáo + chốt sổ

<!-- Screenshot placeholder: capture daily-ops-step-6.png — 1440×900 vi-VN — show /reports/daily với chart (5 lớp đã dạy / 95% chấm công / 75 lượt học sinh / 0 incident) + nút "Xuất Excel" + nút "Gửi chị Hằng" + sample data. -->

Cuối ngày (~21:00), anh Tâm vào `/reports/daily`:

| Chỉ số | Giá trị |
|---|---|
| Lớp đã dạy | 5/5 (100%) |
| Tỷ lệ chấm công | 75/80 = 93.75% |
| Giáo viên có mặt đủ | 4/4 (Nguyễn Văn An dạy thay Phạm Thị Mai) |
| Incident / báo lỗi | 0 |
| Doanh thu phát sinh (học phí mới) | `4.500.000đ` (3 đăng ký mới) |

Click:
- 📥 **"Xuất Excel"** — download file `bao-cao-ngay-16-05-2026.xlsx` (gửi đính kèm email cho chị Hằng)
- 📤 **"Gửi báo cáo chị Hằng"** — auto-email summary qua Resend
- 💤 **"Đóng cửa trung tâm"** — toggle tắt đèn (smart device tuỳ chọn — Phase 2)

---

## 7. Workflow tóm tắt

```
07:50 → Mở /dashboard
08:00 → Duyệt 3 đơn xin nghỉ (5 phút)
08:10 → Check /classes/today, xử lý lớp 🔴 14:00 (10 phút)
08:30 → Quan sát lớp đầu tiên (chấm công nếu cần)
...trong ngày
12:00 → Mid-day check: tỷ lệ chấm công sáng OK?
18:30 → Kiểm tra lớp 18:00 chấm công đúng giờ
21:00 → /reports/daily → xuất Excel → gửi chị Hằng
21:15 → Done. Đăng xuất.
```

**Tổng cộng:** ~30 phút làm việc tích cực + ~3 giờ giám sát thụ động (trong giờ lớp).

---

## 8. Escalation matrix — khi nào báo chị Hằng?

| Tình huống | Báo ngay? | Kênh |
|---|---|---|
| Giáo viên xin nghỉ đột xuất, không tìm được người thay | ✅ Ngay | Zalo + gọi |
| Phụ huynh phàn nàn về chất lượng giảng dạy | ✅ Trong ngày | Zalo + email |
| Sĩ số lớp <50% kéo dài >2 tuần | ⚠️ Cuối tuần | Báo cáo tuần |
| Hệ thống KiteHub lỗi >30 phút | ✅ Ngay | Gọi + ticket support@kitehub.me |
| Doanh thu giảm >20% tháng | ✅ Cuối tháng | Báo cáo tháng |
| Học sinh mới đăng ký rất tốt | ❌ Defer | Đưa vào báo cáo tuần |

---

## 9. Troubleshooting

| Vấn đề | Cách khắc phục |
|---|---|
| Dashboard load chậm (>5 giây) | F5 / clear cache / check status.kitehub.me |
| Chấm công không lưu trên mobile | Kiểm tra kết nối — KiteHub offline mode sync khi có mạng lại |
| Email phụ huynh không gửi được | Verify SES domain `kitehub.me` → check Resend dashboard |
| Excel xuất bị lỗi font tiếng Việt | Mở bằng Excel ≥2019 — Google Sheets render kém font Việt |
| Mất Internet giữa chừng | KiteHub PWA hoạt động offline 24h — sync lại khi có mạng |

---

## 10. Bước tiếp theo

- 📅 [Daily Operations chi tiết hơn](daily-operations.md)
- 🛡️ [Permissions matrix](permissions.md)
- 📊 [Báo cáo tuần / tháng](reports.md)
- 📧 [Mời thêm giáo viên](../p2-owner/invite-staff.md)

---

## 🆘 Cần hỗ trợ?

- 📧 Email: [support@kitehub.me](mailto:support@kitehub.me)
- 💬 Zalo OA: zalo.me/kitehub (đang triển khai — Phase 1.5)
- 📞 Hotline beta: 1900-xxxx (giờ hành chính)
- 🐛 Báo lỗi trang này: [mailto:support@kitehub.me?subject=Lỗi /help/p3-manager/daily-ops](mailto:support@kitehub.me?subject=L%E1%BB%97i%20%2Fhelp%2Fp3-manager%2Fdaily-ops)
- 📊 Trạng thái beta: [/beta-status](/beta-status)
