---
persona: p3-center-manager
topic: reports
last-updated: 2026-05-15
version: v0.9.0-beta
effort_minutes: 4
---

# Báo cáo — Read-only access cho Manager

> 📅 Cập nhật lần cuối: **2026-05-15** · Phiên bản KiteHub: **v0.9.0-beta** · Đọc khoảng **4 phút**

## TL;DR

Anh Tâm dùng `/reports` để gửi số liệu cho chị Hằng cuối tuần/tháng. Read-only access — không sửa được data nhưng có thể export Excel/PDF.

- 📊 **4 loại:** Doanh thu · Chấm công · Chất lượng GV · Học sinh
- 📅 **Khung thời gian:** Tuần · Tháng · Quý · Năm · Custom
- 📥 **Export:** Excel (XLSX) · PDF · CSV
- ❌ **KHÔNG sửa được:** Số liệu live từ DB, đảm bảo tính nhất quán

---

## 1. Doanh thu

<!-- Screenshot placeholder pending B+C merge: capture reports-step-1.png — 1440×900 vi-VN — show /reports/revenue chart 30 ngày + table breakdown + nút Export Excel mũi tên đỏ -->

URL: `/reports/revenue`

Filter: Tuần / Tháng / Quý / Năm / Custom range

Hiển thị line chart + table breakdown:

| Tháng | Học phí thu | Học sinh | TB/hs | YoY |
|---|---|---|---|---|
| 5/2026 | 180.000.000đ | 120 | 1.500.000đ | +5% |
| 4/2026 | 171.000.000đ | 115 | 1.487.000đ | +3% |

Click **Export Excel** → file `bao-cao-doanh-thu-2026-05.xlsx` tải về.

---

## 2. Chấm công

URL: `/reports/attendance`

<!-- Screenshot placeholder pending B+C merge: capture reports-step-2.png — 1440×900 vi-VN — show heatmap 7 ngày × tất cả lớp + warning red cell -->

| Lớp | Buổi | Có mặt | Vắng phép | Vắng không phép | Tỷ lệ |
|---|---|---|---|---|---|
| Anh ngữ 5A1 | 12 | 11 | 1 | 0 | 92% |
| Toán 9B | 8 | 7 | 1 | 0 | 88% |
| Khoa học STEM | 10 | 5 | 3 | 2 | 50% ⚠️ |

Lớp <70% → highlight đỏ, anh Tâm cần tìm hiểu lý do.

---

## 3. Chất lượng giáo viên

URL: `/reports/teacher-performance`

<!-- Screenshot placeholder pending B+C merge: capture reports-step-3.png — 1440×900 vi-VN — show ranking table 5 GV với 4 metric -->

| Cô/Thầy | Số lớp tháng | Tỷ lệ chấm công | Đánh giá PH (⭐) | Doanh thu |
|---|---|---|---|---|
| Cô Phạm Thị Mai | 15 | 98% | 4.8⭐ | 45tr |
| Cô Lê Thị Lan | 12 | 95% | 4.6⭐ | 36tr |

---

## 4. Học sinh

URL: `/reports/students`

3 sub-section:
- **Đăng ký mới** (theo tuần/tháng) — số + nguồn + conversion rate
- **Nghỉ học (churn)** — số + lý do + lifetime value
- **Top progress** — điểm kiểm tra cao nhất

---

## 5. Custom builder

URL: `/reports/custom`

Kéo-thả dimension + metric:
- **Dimensions:** Thời gian / Lớp / Giáo viên / Cấp lớp / Môn học
- **Metrics:** Doanh thu / Số học sinh / Chấm công / Đánh giá

---

## 6. Permissions

| Action | Anh Tâm (STAFF) | Chị Hằng (OWNER) |
|---|:---:|:---:|
| Xem doanh thu thô | ✅ | ✅ |
| Xem doanh thu thuần (profit) | ❌ | ✅ |
| Xem chấm công | ✅ | ✅ |
| Xem GV performance | ✅ | ✅ |
| Export Excel/PDF | ✅ | ✅ |
| Sửa số liệu | ❌ | ❌ |

---

## 7. Liên kết

- [Tổng quan Manager](index.md)
- [Vận hành hàng ngày](daily-operations.md)
- [Quyền hạn STAFF](permissions.md)

---

## 🆘 Cần hỗ trợ?

- 📧 Email: [support@kitehub.me](mailto:support@kitehub.me)
- 📞 Hotline: 1900-xxxx
- 📊 Trạng thái beta: [/beta-status](/beta-status)
