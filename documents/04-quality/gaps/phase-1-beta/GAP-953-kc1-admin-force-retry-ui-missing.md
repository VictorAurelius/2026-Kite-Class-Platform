# GAP-953: Admin "force retry" UI cho FAILED instance không tồn tại

**Status:** 🔵 OPEN
**Priority:** 🔴 P0
**Domain:** Mixed
**Found:** 2026-06-04 (Wave flow-kh3 KC-1 pre-walk audit — 3-agent outside-in consensus)
**Affects:** KC-1 (Admin platform ops surface) — persona P4 admin intervene
**Defer-to:** After Wave flow-kh3 finish

## Problem

UC-PROV-05 "Retry After Failure" — actor: "Admin / scheduled retry (future)". `lifecycle.retry()` exists nhưng KHÔNG có HTTP endpoint exposed (`grep -rn "/admin/.*retry\|@PostMapping.*retry" kitehub/kitehub-platform/src/main/java --include='*.java'` = 0). Admin platform thấy 5 instances FAILED 24h cuối → muốn manual retry → KHÔNG có button trong `/admin/tenants` → phải SSH vào RDS `UPDATE status='INITIALIZING'` (nguy hiểm + audit invisible). Surfaced: persona Finding 4.1.

## Proposed Fix

Thêm BE endpoint `POST /api/v1/admin/tenants/{id}/retry-provisioning` + FE button trong `/admin/tenants/{id}` detail page. Audit log `TENANT_PROVISIONING_RETRY_TRIGGERED` cho mỗi click. Role guard PLATFORM_ADMIN only.

## Acceptance Criteria

- [ ] `POST /api/v1/admin/tenants/{id}/retry-provisioning` returns 200 cho admin role + invokes `lifecycle.retry()`
- [ ] 403 cho non-admin role
- [ ] FE button visible trong admin tenant detail; click → retry triggered + UI updates
- [ ] Audit log row written mỗi retry

## Related

- Discovered in: 3-agent outside-in audit 2026-06-04
- Audit artifact: persona-review/2026-06-04-pre-walk-kc1-tenant-provisioning.md Finding 4.1
- Flow Verification Campaign §4 row KC-1
