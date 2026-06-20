---
audience: mixed
---

# Analytics Report (Báo cáo dashboard Chủ trung tâm) — Business Rules

**Domain:** analytics-report
**Source:** GAP-775 (Wave 106 RST Mảng B11)
**Persona:** P2 Center Owner (primary) — Chủ trung tâm xem báo cáo doanh thu + tỷ lệ điểm danh
**Depends on:** payment-invoice (Payment entity ✅), period-attendance / attendance (Attendance entity ✅), multi-tenancy (tenant filter ✅)

## Scope

Phase 1 BETA — báo cáo analytics tổng hợp toàn tenant cho dashboard Chủ trung tâm:
- **Doanh thu tháng** — tổng tiền các khoản thanh toán `COMPLETED` theo từng tháng.
- **Tỷ lệ điểm danh** — tỷ lệ buổi `PRESENT` / tổng số buổi điểm danh theo từng tháng.

Phân biệt với domain `report-card` (bảng điểm PDF K-12 per-student, Phase 3). Domain này = analytics tổng hợp đọc-only, KHÔNG sinh PDF.

## Rules

### Phạm vi & cô lập tenant (BR-RPT-SCOPE)

| ID | Rule | Enforcement |
|----|------|-------------|
| BR-RPT-SCOPE-001 | Báo cáo cô lập theo tenant — chỉ tổng hợp dữ liệu của tenant hiện tại | Hibernate `tenantFilter` tự động trên `payments.instance_id` + `attendance.instance_id` (không cần filter thủ công trong JPQL) |
| BR-RPT-SCOPE-002 | Bản ghi soft-deleted bị loại khỏi tổng hợp | Mọi query có điều kiện `deleted = false` |
| BR-RPT-SCOPE-003 | Cửa sổ báo cáo = N tháng gần nhất tính đến tháng hiện tại (bao gồm) | `months` param, default 12 |
| BR-RPT-SCOPE-004 | `months` clamp về khoảng `[1, 36]` server-side — guard chống full-table scan vô hạn | `ReportServiceImpl.clampMonths()` (`MAX_MONTHS = 36`) |

### Doanh thu (BR-RPT-REV)

| ID | Rule | Enforcement |
|----|------|-------------|
| BR-RPT-REV-001 | Doanh thu = `SUM(amount)` chỉ các payment trạng thái `COMPLETED` | JPQL filter `paymentStatus = COMPLETED` |
| BR-RPT-REV-002 | Gom nhóm theo tháng của `completedAt` (thời điểm thanh toán hoàn tất) | `GROUP BY YEAR(completedAt), MONTH(completedAt)` |
| BR-RPT-REV-003 | Tháng không có doanh thu → zero-fill (amount = 0), KHÔNG bỏ qua | Service dense-fill toàn bộ cửa sổ |
| BR-RPT-REV-004 | `totalRevenue` = tổng amount toàn cửa sổ | Service cộng dồn |
| BR-RPT-REV-005 | Đơn vị tiền = VND thô (không đơn vị phụ); FE render `1.500.000đ` | per `vn-localization-audit-checklist.md` §1 |

### Điểm danh (BR-RPT-ATT)

| ID | Rule | Enforcement |
|----|------|-------------|
| BR-RPT-ATT-001 | Tỷ lệ điểm danh = `COUNT(PRESENT) / COUNT(*)` theo tháng | JPQL `SUM(CASE WHEN status=PRESENT...) , COUNT(a)` |
| BR-RPT-ATT-002 | Gom nhóm theo tháng của `markedDate` | `GROUP BY YEAR(markedDate), MONTH(markedDate)` |
| BR-RPT-ATT-003 | Tỷ lệ là phần trăm `[0, 100]`, làm tròn 1 chữ số thập phân (HALF_UP) | `ReportServiceImpl.rate()` dùng `BigDecimal.setScale(1, HALF_UP)` |
| BR-RPT-ATT-004 | Tháng không có buổi điểm danh (total=0) → rate = 0, KHÔNG chia cho 0 | Service guard `total <= 0 → 0.0` |
| BR-RPT-ATT-005 | `overallPresentRate` = tổng PRESENT / tổng total toàn cửa sổ (không phải trung bình các tỷ lệ tháng) | Service cộng dồn rồi mới chia |
| BR-RPT-ATT-006 | Trạng thái tính PRESENT chỉ là `PRESENT`; `LATE`/`EXCUSED`/`MAKEUP`/`ABSENT` KHÔNG tính present | enum `AttendanceStatus.PRESENT` literal |

### Phân quyền (BR-RPT-AUTHZ)

| ID | Rule | Enforcement |
|----|------|-------------|
| BR-RPT-AUTHZ-001 | Báo cáo tổng hợp toàn tenant (tài chính + vận hành) → chỉ Owner/admin xem được | `@PreAuthorize("hasRole('ADMIN')")` trên cả 2 endpoint (OWASP A01 role gate) |
| BR-RPT-AUTHZ-002 | Đây là role gate, KHÔNG phải per-resource gate — vì "tài nguyên" là toàn tenant (không có scope class/student để thu hẹp) | per `pre-launch-owasp-rest-hardening-checklist.md` §2.1 |
| BR-RPT-AUTHZ-003 | User không phải admin → 403, service KHÔNG được gọi (defense in depth) | Spring Security `@PreAuthorize` chặn trước khi vào controller body |

## Config keys

Không có config key động — `MAX_MONTHS = 36` là hằng số code-level (guard, không phải business tunable). Nếu cần tunable trong tương lai → chuyển sang `kiteclass.report.max-months` property.

## Out-of-scope (Phase 1.5+ follow-up)

- Báo cáo theo quý/năm (chỉ có tháng ở Phase 1).
- Drill-down per-class / per-teacher revenue + attendance.
- Export CSV/Excel báo cáo (document-generation infra đã có nhưng chưa wire cho analytics).
- So sánh kỳ trước (MoM/YoY growth %).

## Five-attribute review per `business-logic-review.md` §2

Analytics-report rule values (COMPLETED-only revenue, PRESENT-only attendance rate, 36-month clamp, Owner/admin role gate) carry **business meaning** — a Center Owner cares whether revenue counts only settled payments and how attendance is computed. Pure-engineering bits (month clamp guard) are noted as such.

- **Source:** GAP-775 (Wave 106 RST Mảng B11) — derived from P2 Center Owner persona need (revenue + attendance overview). Definitions (COMPLETED-only revenue, PRESENT-only rate, zero-fill months) = informed gut + standard reporting convention; no internal analytics A/B data yet.
- **Rationale:** Revenue = `COMPLETED` payments only (BR-RPT-REV-001) so unsettled/pending money is not overstated; attendance rate = `PRESENT` / total with `LATE`/`EXCUSED`/`MAKEUP` excluded (BR-RPT-ATT-006) to give a strict present-rate; the 36-month clamp (BR-RPT-SCOPE-004) is an engineering full-table-scan guard, not a business limit; aggregate tenant-wide financial+operational data → role-gated to Owner/admin (OWASP A01).
- **Reviewer:** @nguyenvankiet (acting Product Owner, solo-dev, 2026-06-21). Revenue/attendance definitions warrant Product-Owner + (future) center-owner stakeholder sign-off — queued GAP-156 AC-D.
- **Compliance check:** **Considered (self-assessed, counsel pending GAP-156 AC-D)** — per `documents/00-brd/compliance-checklist.md` L1: the aggregate report is read-only, tenant-isolated (BR-RPT-SCOPE-001), and role-gated (BR-RPT-AUTHZ-001) so no per-student PII is exposed to unauthorized roles. **Nghị định 13/2023/NĐ-CP (PDPL)** applies if CSV/Excel export is added (Phase 1.5+, out-of-scope) — aggregate-data export would need a PII-minimization review then. No counsel verification yet.
- **Review cadence:** **Quarterly** (Phase 1 BETA feature, validate definitions fit) → Annual once stable. **Next review:** 2026-09-21. Event triggers: revenue-definition change (e.g., counting PROCESSING), export feature added (PDPL re-review), persona complaint on attendance semantics.
