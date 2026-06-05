# GAP-1007: Parent portal role-collision IDOR (defense-in-depth)

**Status:** 🔵 OPEN
**Priority:** 🟡 P2
**Domain:** Backend
**Found:** 2026-06-05 (Wave flow-kc8 KC-8 pre-walk persona sim FM#8)
**Affects:** `kiteclass-core` `AuthorizationBean.hasAccessToChild` + `kitehub-gateway` route config

## Problem

`@authz.hasAccessToChild(#childId)` (guard mọi parent facet) check `parents.id == X-User-Reference-Id` + link tới child, NHƯNG **không verify caller có role PARENT**. Gateway `/api/v1/parent/**` rơi vào catch-all `/api/v1/**` — `JwtAuthenticationGatewayFilter` authenticate + inject headers nhưng **không role-gate per-route**.

Kịch bản collision: 1 TEACHER/STUDENT user có `users.reference_id` (BIGINT) trùng số với 1 `parents.id` đã link tới child → craft request `/api/v1/parent/children/{childId}/...` → `existsByParentIdAndStudentIdAndDeletedFalse(refId, childId)` match → access. Xác suất thấp (collision 2 ID space riêng) nhưng unbounded (cả 2 là BIGINT sequence). Defense hiện chỉ dựa gateway routing (which không role-gate).

Đây là defense-in-depth gap, KHÔNG phải live leak phổ biến (cần collision cụ thể).

## Proposed Fix

(1) Thêm `@PreAuthorize("hasRole('PARENT')")` (hoặc compose với hasAccessToChild) trên parent controllers, HOẶC (2) thêm role check trong `hasAccessToChild` (verify authority chứa ROLE_PARENT trước khi check link), HOẶC (3) gateway role-gate `/api/v1/parent/**` → PARENT only. Chọn 1, nhất quán 6 controller.

## Acceptance Criteria

- [ ] Non-PARENT user (TEACHER/STUDENT) với reference_id trùng parents.id → 403 khi gọi parent facet.
- [ ] PARENT happy-path unaffected.

## Related

- Discovered in: Wave flow-kc8 KC-8 pre-walk persona sim FM#8
- Cross-flow: applies mọi parent facet (6 controller), không chỉ 2 vừa fix @PreAuthorize
