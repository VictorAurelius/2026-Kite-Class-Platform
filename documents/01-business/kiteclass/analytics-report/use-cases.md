---
audience: mixed
---

# Analytics Report — Use Cases

**Domain:** analytics-report
**Source:** GAP-775

## UC-RPT-001 — Chủ trung tâm xem báo cáo doanh thu tháng

**Actor:** P2 Center Owner (role `ADMIN`)

**Tiền điều kiện:**
- Owner đã đăng nhập, request mang `X-Tenant-Id` của trung tâm.
- Có ít nhất 0 khoản thanh toán (báo cáo vẫn trả về series zero-fill nếu rỗng).

**Luồng chính:**
1. Owner mở dashboard → component "Doanh thu tháng".
2. FE gọi `GET /api/v1/reports/revenue?months=12`.
3. Server clamp `months` về `[1, 36]`, tính cửa sổ `[đầu tháng cũ nhất, đầu tháng kế tiếp)`.
4. Server `SUM(amount)` các payment `COMPLETED` GROUP BY tháng của `completedAt` (tenant filter tự động).
5. Server zero-fill các tháng trống → trả về series 12 điểm (cũ → mới) + `totalRevenue`.
6. FE render KPI card (`totalRevenue` → `15.000.000đ`) + chart 12 tháng.

**Luồng phụ / lỗi:**
- **E1 — Không phải admin:** request từ TEACHER/PARENT/STUDENT → `@PreAuthorize` chặn → HTTP 403, service không chạy.
- **E2 — DB rỗng:** không có payment COMPLETED → series toàn 0, `totalRevenue = 0` (không lỗi).
- **E3 — `months` ngoài khoảng:** `months=0` → clamp 1; `months=100` → clamp 36.

**Hậu điều kiện:** Owner thấy doanh thu thực của trung tâm mình (không lẫn tenant khác — BR-RPT-SCOPE-001).

**FE behavior:** VND format `1.500.000đ`, nhãn tiếng Việt "Doanh thu tháng", chart trục X = tháng `YYYY-MM`.

---

## UC-RPT-002 — Chủ trung tâm xem báo cáo tỷ lệ điểm danh

**Actor:** P2 Center Owner (role `ADMIN`)

**Tiền điều kiện:** như UC-RPT-001.

**Luồng chính:**
1. Owner mở dashboard → component "Tỷ lệ điểm danh".
2. FE gọi `GET /api/v1/reports/attendance?months=12`.
3. Server `COUNT(PRESENT)` + `COUNT(*)` GROUP BY tháng của `markedDate` (tenant filter tự động, `deleted = false`).
4. Server tính `presentRate` mỗi tháng (HALF_UP 1 chữ số) + `overallPresentRate` toàn cửa sổ.
5. Server zero-fill các tháng trống (rate = 0, counts = 0).
6. FE render KPI card (`overallPresentRate` → `92,5%`) + chart 12 tháng + fraction "37/40 buổi".

**Luồng phụ / lỗi:**
- **E1 — Không phải admin:** HTTP 403, service không chạy (BR-RPT-AUTHZ-003).
- **E2 — Tháng total=0:** rate = 0, không chia cho 0 (BR-RPT-ATT-004).
- **E3 — DB rỗng:** `overallPresentRate = 0`, series toàn 0.

**Hậu điều kiện:** Owner thấy tỷ lệ điểm danh trung tâm mình theo tháng.

**FE behavior:** phần trăm `92,5%` (dấu phẩy thập phân VN), nhãn tiếng Việt "Tỷ lệ điểm danh", hiển thị kèm `presentCount/totalCount`.
