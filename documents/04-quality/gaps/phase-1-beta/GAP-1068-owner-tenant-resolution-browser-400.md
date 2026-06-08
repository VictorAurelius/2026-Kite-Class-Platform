# GAP-1068: OWNER browser 400 — wrong-credential (không phải code bug); residual users.tenant_id ↔ instances.owner_id drift

**Status:** 🟡 PARTIAL
**Priority:** 🟢 P3
**Domain:** Backend (test-data consistency)
**Found:** 2026-06-08 (KC-1 G2 — OWNER login → dashboard → mọi call KiteClass 400)
**Affects:** Tenant resolution cho OWNER browser path; test-data link consistency `kitehub.users.tenant_id` ↔ `kitehub.instances.owner_id`

## Problem

**Reframed (chẩn đoán ban đầu SAI — tưởng P0 code bug).** Thực tế:

- Code Wave 104 (GAP-704) **ĐÚNG**: `TokenService.resolveTenantIdForRole` cho OWNER dùng `instanceRepository.findByOwnerIdAndDeletedFalse(userId)` → nhúng `tenantId` claim (= instance.id) vào JWT. Gateway `TenantResolverGatewayFilterFactory` §85 đọc claim → resolve → set X-Tenant-Id → core 200.
- **400 mọi call là do đăng nhập SAI owner.** Tenant sky-education (instance `e8ff87e1`) có 2 user liên kết LỆCH:
  - `owner@skyedu.vn` (`3c659096`) = `instances.owner_id` ✅ canonical → JWT có tenantId claim → **dashboard 200**.
  - `owner.sky@test.vn` (`c2a4b159`) = `users.tenant_id=e8ff87e1` (forward) NHƯNG KHÔNG phải `instances.owner_id` → `findByOwnerId` null → JWT thiếu claim → **400**.
- Recipe KC-1 gốc dùng `owner@skyedu.vn` (đúng); coordinator "sửa" nhầm sang `owner.sky@test.vn` (theo users.tenant_id) → tự gây 400. Đã revert recipe.

**Verify (2026-06-08):** login `owner@skyedu.vn` → JWT `"tenantId":"e8ff87e1..."`; `/teachers` `/students` `/courses` → 200 không gắn header tay.

**Residual thật (P3):** forward link `users.tenant_id` và reverse link `instances.owner_id` không nhất quán cho 2 user sky. Nếu một OWNER hợp lệ chỉ có `users.tenant_id` mà không phải `instances.owner_id` → sẽ 400. Seed/provisioning nên set cả 2 link nhất quán. Scope drift: 30 instance, 0 null_owner, 1 orphan_owner.

## Proposed Fix (P3, không gấp)

(1) Seed/provisioning đảm bảo `instances.owner_id` ↔ `users.tenant_id` nhất quán khi tạo tenant. (2) (Tùy chọn) `resolveTenantIdForRole` fallback đọc `users.tenant_id` nếu `findByOwnerId` rỗng (defense-in-depth) — cân nhắc rủi ro multi-owner. (3) Cleanup 1 orphan_owner instance.

## Acceptance Criteria

- [x] Root cause xác định = wrong credential (instances.owner_id canonical), không phải code
- [x] Verify login owner đúng → dashboard 200 (no manual header)
- [ ] (P3) Seed consistency users.tenant_id ↔ instances.owner_id + cleanup orphan

## Related

- Discovered in: KC-1 G2 walk 2026-06-08
- GAP-704 / Wave 104 Bucket A (JWT tenantId enrichment — đúng, không regress)
- GAP-1069 (classes/invoices 404 — issue riêng cùng walk)
- Lesson: chọn owner theo `instances.owner_id` (canonical cho JWT), KHÔNG theo `users.tenant_id`
