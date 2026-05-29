# GAP-809 — FE↔BE contract drift: /api/v1/classes + /api/v1/invoices flat-list 404

**Status:** (canonical: gap-status.csv) OPEN 2026-05-29
**Priority:** P2 · **Domain:** Mixed · **Phase:** phase-1-beta

## Problem

FE gọi flat-list endpoints `GET /api/v1/classes?page=` + `GET /api/v1/invoices?page=` nhưng BE KHÔNG expose chúng:
- `ClassController`: chỉ `GET /api/v1/courses/{courseId}/classes` (course-scoped) + `GET /api/v1/classes/{classId}` — KHÔNG có flat `GET /api/v1/classes`.
- `InvoiceController` (`@RequestMapping /api/v1/invoices`): chỉ `GET /{id}`, `/{id}/items`, `/student/{studentId}`, `/overdue` — KHÔNG có `GET /` list-all.

→ Cả 2 trả 404. Surfaced 2026-05-29 demo-trio walk (dashboard widget calls). Sau khi fix FE crash-guard (GAP-807 envelope+theme guards), các 404 này được handle graceful (dashboard không crash) nhưng page `/classes` (course-scoped, OK by design) + `/billing` widgets có thể empty.

Sister class của GAP-802 BE↔FE contract drift detection.

## Acceptance Criteria

- [ ] Quyết định: thêm BE flat-list endpoints (`GET /api/v1/classes`, `GET /api/v1/invoices` với tenant scope) HOẶC sửa FE bỏ flat-list calls (dùng course-scoped + student-scoped)
- [ ] FE handle 404 graceful trên các list widgets (no console error spam)
- [ ] `check-be-fe-url-contract.sh` (GAP-802) detect được class này

## Log

- **2026-05-29 (OPEN):** Filed từ demo-trio walk. Direct core :8088 confirm 404 RESOURCE_NOT_FOUND cho cả 2 (không phải gateway routing). See audits/rst-html/2026-05-29-demo-trio-walk-findings.md.
