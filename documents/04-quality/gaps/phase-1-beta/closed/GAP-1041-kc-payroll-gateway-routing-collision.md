# GAP-1041: Payroll endpoints shadowed by gateway `/api/v1/admin/**` → kitehub-admin (404)

**Status:** 🟢 DONE
**Priority:** 🔴 P0
**Domain:** Backend (gateway)
**Found:** 2026-06-06 (KC-12 reschedule/payroll G1 walk, FM-1/headline)
**Affects:** `kitehub-gateway` route `kitehub-admin-v1` (application.yml:551) shadows `PayrollController` (`/api/v1/admin/payroll/**`, kiteclass-core)

## Problem

KC-12 G1 walk: payroll endpoints **unreachable qua gateway** — route nhầm tới kitehub-admin (không có payroll) → 404.

**Walk evidence:**
```
GET :9000/api/v1/admin/payroll/periods (ADMIN token) → 404 "Endpoint not found: GET /api/v1/admin/payroll/periods"
GET :8080/api/v1/admin/payroll/periods (direct kiteclass-core, ADMIN headers) → 200 {content:[],...}  ← backend OK
```

`PayrollController` (kiteclass-core) maps `/api/v1/admin/payroll/{configs,periods,periods/{id}}`. Gateway route `kitehub-admin-v1` (`application.yml:551`, `Path=/api/v1/admin/**` → `kitehub-admin:8080`) first-match-wins nuốt `/api/v1/admin/payroll/**` → kitehub-admin (KH-9 admin console, không có payroll) → 404. Toàn bộ payroll feature dead qua gateway.

## Root Cause

Route `kitehub-admin-v1` predicate `Path=/api/v1/admin/**` quá rộng — bắt mọi `/api/v1/admin/*` kể cả `/api/v1/admin/payroll` của kiteclass-core. Cùng class với GAP-1034 (kitehub-branding-v1 `/api/v1/branding/**` shadows kiteclass branding) + GAP-1031 (platform-email broad route). **Recurrence #3** của gateway broad-predicate problem → xem meta GAP-1043.

## Proposed Fix

**Option A (recommended):** Thêm explicit route `kiteclass-payroll` (`Path=/api/v1/admin/payroll/**` → kiteclass-core) khai báo **TRƯỚC** `kitehub-admin-v1` (giống pattern `kitehub-admin-beta-requests-v1` :525 đã "Must precede kitehub-admin-v1 catch-all").

**Option B:** Thu hẹp `kitehub-admin-v1` predicate xuống đúng paths kitehub-admin own (dashboard/instances/audit-logs) thay vì catch-all `/**`.

⚠️ Sau fix, payroll cross-tenant leak (GAP-1039 sister — payroll repos filter-only) sẽ activate live → fix cùng security-1.

## Acceptance Criteria

- [x] `GET :9000/api/v1/admin/payroll/periods` (ADMIN) → 200 (was 404)
- [x] `/configs` + `/periods/{id}` reachable qua gateway (route /api/v1/admin/payroll/**)
- [x] kitehub-admin KH-9 endpoints vẫn route đúng (admin instances PLATFORM_ADMIN → 200)
- [x] Payroll cross-tenant → tracked GAP-1039 sweep (filter-only repos, security-1 Bucket D)

## Related

- Discovered in: KC-12 G1 walk (Wave flow-kc12), pre-walk FM-1
- **Recurrence #3** routing collision: GAP-1031 (KH-10 email) + GAP-1034 (KC-10 branding) + this. → META GAP-1043 (gateway route-predicate audit)
- Existing precedent fix: `kitehub-admin-beta-requests-v1` (:525) + `kitehub-admin-impersonate` (:539) already declared before catch-all — same pattern needed for payroll
- Payroll cross-tenant (GAP-1039 sister) latent behind this shadow

## Closure (Wave security-1, 2026-06-06)

**Fix shipped:** added `kiteclass-payroll` route (`/api/v1/admin/payroll/**` → kiteclass-core, TenantResolver) to `kitehub-gateway/.../application.yml` BEFORE `kitehub-admin-v1` catch-all — mirrors existing `kitehub-admin-beta-requests-v1` / `kitehub-admin-impersonate` precedent (specific admin subpaths declared before /api/v1/admin/** catch-all).

**Re-walk evidence (live gateway :9000 post-rebuild):**
- ADMIN-role `GET :9000/api/v1/admin/payroll/periods` → **200** (was 404 "Endpoint not found").
- No regression: kitehub-admin KH-9 admin-instances (PLATFORM_ADMIN) → 200; gateway-route audit back to 4 findings (= main baseline).

**AC:** payroll reachable 200 ✅, kitehub-admin no-regression ✅. Payroll cross-tenant leak (repos filter-only, sister GAP-1039) tracked for security-1 Bucket D (data-layer). Routing collision recurrence #3 systemic audit → GAP-1042.
