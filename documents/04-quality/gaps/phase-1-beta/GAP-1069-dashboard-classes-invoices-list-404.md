# GAP-1069: Dashboard widget gọi GET /api/v1/classes + /api/v1/invoices list → 404 (FE↔BE contract drift)

**Status:** 🔵 OPEN
**Priority:** 🟡 P2
**Domain:** Mixed (kiteclass-frontend dashboard + kiteclass-core ClassController/InvoiceController)
**Found:** 2026-06-08 (KC-1 G2 — dashboard OWNER, 2/6 widget 404)
**Affects:** Dashboard summary widgets KiteClass (Lớp học + Hóa đơn); liên quan KC-3 (classes) + KC-7 (invoices)

## Problem

OWNER dashboard (`owner@skyedu.vn`, tenant resolve OK) gọi 6 list endpoint; **4 trả 200, 2 trả 404**:

| Endpoint | Kết quả |
|---|---|
| `GET /api/v1/teachers?page=0&size=1` | ✅ 200 |
| `GET /api/v1/students?page=0&size=1` | ✅ 200 |
| `GET /api/v1/courses?page=0&size=1` | ✅ 200 |
| `GET /api/v1/classes?page=0&size=1` | 🔴 404 `RESOURCE_NOT_FOUND` |
| `GET /api/v1/invoices?page=0&size=1` | 🔴 404 `RESOURCE_NOT_FOUND` |

`InvoiceController` CÓ `@RequestMapping("/api/v1/invoices")` (line 37) nhưng **không có GET-list method** (paging root) → 404 (chỉ có GET /{id} hoặc POST). `ClassController` tương tự (request route tới core OK, error shape là app-level `success:false RESOURCE_NOT_FOUND` → reached core nhưng no handler). FE dashboard (`lib/api/invoices.ts:25` gọi `'/api/v1/invoices'` với params) kỳ vọng list endpoint mà BE chưa expose ở root.

Non-blocking KC-1 (tenant settings). Là drift contract FE gọi vs BE expose — thuộc scope KC-3/KC-7 verify.

## Proposed Fix

Xác định canonical: hoặc BE thêm GET-list `/api/v1/classes` + `/api/v1/invoices` (paging) cho dashboard, hoặc FE dashboard gọi đúng endpoint hiện có (vd `/api/v1/classes` có thể nằm dưới course scope). Cross-check `api-contract.md` của 2 domain (per api-contract-change-caller-sweep.md).

## Acceptance Criteria

- [ ] Xác định canonical list endpoint cho classes + invoices
- [ ] FE dashboard widget Lớp học + Hóa đơn → 200 (hoặc empty-state đúng, không 404)
- [ ] api-contract.md đồng bộ controller ↔ FE caller

## Related

- Discovered in: KC-1 G2 walk 2026-06-08 (dashboard widget)
- GAP-1068 (tenant resolution — sister finding cùng walk, đã reframe)
- Scope: KC-3 (class) + KC-7 (invoice) flow verify
