# GAP-1005: InvoiceController thiếu @PreAuthorize toàn bộ endpoint (OWASP A01)

**Status:** 🔵 OPEN
**Priority:** 🟠 P1
**Domain:** Backend
**Found:** 2026-06-05 (KC-7 invoice→payment G1 walk)
**Affects:** `InvoiceController` (kiteclass-core)

## Problem

KC-7 G1 walk: `GET /api/v1/invoices/28` trả **HTTP 200 không cần role** (không header `X-User-Roles`). `InvoiceController` KHÔNG có `@PreAuthorize` trên BẤT KỲ method nào — kể cả mutation tài chính `mark-paid`, `cancel`, `apply-adjustment`. Trái ngược `PaymentRecordController` (đã có `@PreAuthorize hasAnyRole`).

Cross-tenant read được RLS bảo vệ (walk: GET invoice tenant khác → 404), nhưng **không có role gate** → bất kỳ role nào lọt gateway (kể cả thấp) đều gọi được mark-paid/cancel/adjustment trong cùng tenant. Defense-in-depth gap (OWASP A01 broken access control).

Lưu ý: gap này chỉ thực sự gate được SAU khi GAP-1003 (auth bridge) đã land — nay đã có, nên `@PreAuthorize hasAnyRole` trên InvoiceController sẽ hoạt động.

## Root Cause

InvoiceController ship không kèm `@PreAuthorize` (khác convention PaymentRecordController). Trước GAP-1003 thì `hasRole` dù sao cũng dead-deny; nay bridge đã có → cần thêm guard cho đúng.

## Proposed Fix

Thêm `@PreAuthorize("hasAnyRole('TEACHER','ADMIN','OWNER','PLATFORM_ADMIN')")` (read) + `hasAnyRole('ADMIN','OWNER')` (mark-paid/cancel/adjustment — chỉ owner/admin sửa tài chính) trên từng method `InvoiceController`. Thêm 403 IT (role thấp → mark-paid bị chặn).

## Acceptance Criteria

- [ ] Mỗi endpoint InvoiceController có `@PreAuthorize` đúng tier (read vs financial-mutation)
- [ ] IT: STUDENT/PARENT/no-role → mark-paid/cancel → 403; OWNER → 200/201
- [ ] api-contract.md ghi authz per endpoint

## Related

- Discovered in: KC-7 G1 walk artifact `documents/04-quality/audits/persona-review/2026-06-05-pre-walk-kc7-invoice-payment.md` §G1 (#8)
- Depends on: GAP-1003 (auth bridge — DONE, hasRole nay enforce được)
- Pattern: GAP-637 (admin v1 controllers @PreAuthorize missing — same class, kitehub side)
