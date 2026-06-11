# GAP-1169: ParentPaymentController.currentTenantId() trả nil-UUID stub — chưa resolve tenant thật

**Status:** 🔵 OPEN
**Priority:** 🟡 P2
**Domain:** Backend
**Found:** 2026-06-11 (TODO classification — IDE diagnostics review)
**Affects:** `kiteclass-core` parent payment module (`ParentPaymentController`)

## Problem

`ParentPaymentController.currentTenantId()` (line ~207) trả về **nil-UUID stub** thay vì tenant thật:

```java
private String currentTenantId() {
    // TODO Wave 106: use TenantContext.getCurrentTenant() when stable helper exists.
    return "00000000-0000-0000-0000-000000000000";
}
```

Javadoc ghi: "stub returns nil-UUID; production wiring uses TenantContext when available across modules." Nghĩa là parent-payment queries hiện scope theo nil-UUID tenant — nếu endpoint được dùng trong Phase 1 BETA, dữ liệu sẽ scope sai (nil-tenant → rỗng/fail-closed thay vì tenant đúng của phụ huynh).

Đây là **deferred-có-chủ-đích** chờ "stable TenantContext helper" (TODO gắn Wave 106), nhưng chưa có gap track → file để không sót.

## Root Cause

`TenantContext.getCurrentTenant()` chưa được wire ổn định cross-module cho luồng parent-payment (parent portal resolve tenant qua đâu chưa chốt). Stub nil-UUID là placeholder.

## Proposed Fix

Thay nil-UUID stub bằng resolve tenant thật qua `TenantContext.getCurrentTenant()` (đã dùng ở các module khác — vd `CourseServiceImpl` `@CacheEvict` key dùng `TenantContext.getCurrentTenant()`), HOẶC qua header `X-Tenant-Id` per `BaseEntity` tenant filter convention. Verify parent-payment queries scope đúng tenant của phụ huynh đăng nhập.

## Acceptance Criteria

- [ ] `currentTenantId()` resolve tenant thật (không còn nil-UUID hardcode)
- [ ] Parent-payment endpoint scope đúng tenant — IT verify 1 phụ huynh chỉ thấy payment của tenant mình
- [ ] Cross-tenant isolation: phụ huynh tenant A không thấy payment tenant B

## Related

- Discovered in: TODO classification 2026-06-11 (IDE diagnostic `ParentPaymentController.java:207`)
- Tenant-context model: GAP-790 / GAP-795 (X-User-Id UUID, DONE) — luồng này cần helper tương tự cho tenant
- Parent portal: GAP-1007 (parent portal IDOR / hasAccessToChild)
