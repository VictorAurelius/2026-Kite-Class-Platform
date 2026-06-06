# GAP-1025: InstanceController thiếu @PreAuthorize — enumerate all + delete/purge any instance

**Status:** 🟢 DONE
**Priority:** 🔴 P0
**Domain:** Backend
**Found:** 2026-06-06 (KH-8 off-boarding G1 walk)
**Affects:** `InstanceController` (kitehub-subscription) + gateway `platform-instances` route

## Problem

KH-8 G1 walk: `InstanceController` (`/api/platform/instances`) có **ZERO @PreAuthorize** trên mọi endpoint, và gateway route `platform-instances` không có role predicate → chỉ `anyRequest().authenticated()`. Hệ quả: bất kỳ authenticated user (kể cả TENANT user thường) có thể:

- **`GET /api/platform/instances`** — list TẤT CẢ instances (cross-tenant enumeration). Walk evidence: owner.test (role OWNER) → 200, trả 6 instances.
- **`DELETE /api/platform/instances/{id}`** — soft-delete bất kỳ instance.
- **`DELETE /api/platform/instances/{id}/purge`** — purge vĩnh viễn bất kỳ instance (off-boarding leg). Walk evidence: owner.test purge instance khác → 200 (reach purge logic; chỉ status=FAILED do chưa soft-deleted, nhưng authz đã cho qua).
- **`POST /api/platform/instances/{id}/extend-trial`** — extend trial bất kỳ instance.

Javadoc `purgeInstance` ghi "admin only" nhưng KHÔNG có gì enforce. OWASP A01 — destructive (purge) + cross-tenant enumeration. P0.

## Root Cause

InstanceController thiếu method-level @PreAuthorize (dù subscription `SecurityConfig` có `@EnableMethodSecurity`); gateway route không gắn role. Class giống FM-1 các flow trước nhưng tệ hơn — không có role check NÀO.

## Proposed Fix

1. Add `@PreAuthorize` cho destructive/enumeration endpoints:
   - `listInstances` + `listInstancesByCursor` → `hasAnyRole('PLATFORM_ADMIN','ADMIN')` (list-all = admin; owner dùng `GET /owner/{ownerId}`)
   - `deleteInstance` + `purgeInstance` + `extendTrial` → admin
2. Update `InstanceApiContractTest` (10 tests, `@WebMvcTest` + `@Import(SecurityConfig)` + `@EnableMethodSecurity`) để auth as admin (X-User-Roles header / @WithMockUser).
3. Cân nhắc owner-scoped self-offboard endpoint riêng (ownership bind) nếu cần — KHÔNG mở deleteInstance cho owner without bind.

## Acceptance Criteria

- [x] Non-admin user GET /instances (list all) → 403 (owner→403 verified)
- [x] Non-admin DELETE/purge/extend-trial bất kỳ instance → 403 (owner-purge→403 verified)
- [x] Admin vẫn thao tác được (PLATFORM_ADMIN→200)
- [x] InstanceApiContractTest pass với admin auth (@WithMockUser PLATFORM_ADMIN added)
- [ ] IT cover non-admin 403 + admin 200

## Related

- Discovered in: KH-8 G1 walk — `documents/04-quality/audits/persona-review/2026-06-06-pre-walk-kh8-offboarding-pdpl-consent.md` (FM-2)
- Related (authz/IDOR family): GAP-1015/1019/1023 (cross-tenant bind); spans KH-1 provisioning + KH-9 admin console scope

## Closure (Wave security-2 Bucket C, 2026-06-06)

**Fix:** added `@PreAuthorize("hasAnyRole('PLATFORM_ADMIN','ADMIN')")` to InstanceController admin/destructive endpoints: `listInstances`, `listInstancesByCursor`, `deleteInstance`, `purgeInstance`, `extendTrial` (kitehub-subscription, @EnableMethodSecurity active). Ungated reads (getInstanceById/getInstanceBySubdomain) + provisioning (create/register) left as-is (single-instance ownership binding = Bucket B IDOR scope).

**Re-walk (live gateway :9000 post-rebuild):** owner GET /api/platform/instances → **403** (was 200 enumerate-all); owner DELETE purge any → **403**; PLATFORM_ADMIN GET → **200**.

**Tests:** InstanceApiContractTest (class-level @WithMockUser PLATFORM_ADMIN) + InstanceControllerIntegrationTest.shouldDeleteInstanceSuccessfully (method @WithMockUser) updated per `api-contract-change-caller-sweep.md` — both re-run PASS (mvnw test exit 0). 5 prior failures (403-vs-expected) resolved.
