# GAP-1306: `resolveTenantIdForRole` dùng `findFirst()` không `ORDER BY` → JWT `tenantId` non-deterministic cho owner nhiều instance

**Status:** 🔵 OPEN
**Priority:** 🟠 P1
**Domain:** Backend
**Found:** 2026-06-14 (SSO hardening GAP-1138 — `feature/sso-hardening-e2e`; root-cause của GAP-1305 workaround)
**Affects:** `kitehub/kitehub-subscription/.../service/AuthService.java:846-856` (`resolveTenantIdForRole`); mọi JWT KiteHub mint cho role `OWNER` (login `/api/auth/login` + SSO exchange `/api/v1/auth/sso/exchange`)

## Problem

`AuthService.resolveTenantIdForRole(userId, role)` enrich JWT claim `tenantId` cho role `OWNER` bằng:

```java
if ("OWNER".equals(role)) {
    return instanceRepository.findByOwnerIdAndDeletedFalse(userId).stream()
        .findFirst()                       // ← KHÔNG ORDER BY
        .map(Instance::getId)
        .orElse(null);
}
```

`findByOwnerIdAndDeletedFalse(userId)` trả về `List<Instance>` KHÔNG có `ORDER BY` → thứ tự do Postgres quyết định (heap/index scan order, có thể đổi giữa các lần query, sau VACUUM, hay khi plan thay đổi). Với owner sở hữu **>1 non-deleted instance**, `.findFirst()` chọn instance non-deterministic → JWT claim `tenantId` có thể trỏ tenant SAI giữa các lần login/SSO.

Javadoc tại `AuthService.java:838-840` nêu rõ invariant Phase 1 BETA "1 user → 1 tenant" và thừa nhận "first one wins, which is safe because beta-signup is gated to a single tenant per owner" — nhưng đây là giả định, KHÔNG được DB constraint enforce. Thực tế đã vi phạm: `owner.test@test.vn` cố ý sở hữu 2 instance (verified GAP-1305 §State-check 2026-06-14: lúc emit `aaaabbbb`, lúc `22003e3c`).

**GAP-1305 chỉ WORKAROUND vấn đề này**: seed 1 dedicated single-instance owner (`sso.owner@skytest.test` → đúng 1 instance → `findFirst()` deterministic) để SSO G1/G2 walk chạy được. Root cause (`findFirst()` không `ORDER BY` + invariant không enforce) vẫn chưa fix → production owner có >1 instance vẫn có thể land SAI tenant qua SSO (cross-tenant data exposure risk).

## Proposed Fix

Một trong hai (đề xuất ưu tiên (1), high-level — chi tiết để fix PR quyết):

1. **Deterministic ORDER BY** — thêm `ORDER BY created_at ASC` (instance cũ nhất = primary, ổn định) hoặc cờ `is_primary` rõ ràng vào repository query (`findByOwnerIdAndDeletedFalseOrderByCreatedAtAsc` hoặc `@Query` tường minh), thay `.findFirst()`. Làm `tenantId` claim deterministic kể cả owner nhiều instance.
2. **Enforce invariant** — nếu Phase 1 BETA thật sự "1 user → 1 tenant", thêm DB constraint (unique partial index trên `instances(owner_id) WHERE deleted = false`) + reject/flag owner vi phạm; SSO exchange trả lỗi tường minh thay vì âm thầm chọn bừa.

Cập nhật javadoc `AuthService.java:838` cho khớp behavior đã chọn. Cân nhắc test đa-instance (owner 2 instance → claim ổn định qua N lần mint).

## Acceptance Criteria

- [ ] `resolveTenantIdForRole` cho `OWNER` trả `tenantId` deterministic kể cả khi owner sở hữu ≥2 non-deleted instance (ORDER BY tường minh HOẶC invariant enforced).
- [ ] Test bao phủ owner-nhiều-instance: mint JWT N lần → `tenantId` claim không đổi.
- [ ] Javadoc `AuthService.java:838` đồng bộ với behavior thực tế (không còn dựa giả định "beta-signup gates single tenant").
- [ ] GAP-1305 dedicated-seed workaround có thể gỡ (hoặc giữ làm test fixture nhưng không còn là điều kiện đúng-đắn của SSO).

## Related

- Discovered in: PR `feature/sso-hardening-e2e` (GAP-1138 SSO hardening — Bucket C); root-cause investigation từ GAP-1305 §State-check finding.
- GAP-1305 (workaround đã ship — single-instance dedicated owner seed cho SSO walk determinism).
- GAP-1138 (cross-product SSO KH→KC — consumer của JWT `tenantId` claim qua exchange).
- GAP-531 (follow-up per-role tenant lookup — STAFF/TEACHER/PARENT/STUDENT; cùng `resolveTenantIdForRole` scope).
