# GAP-953: Admin "force retry" UI cho FAILED instance không tồn tại

**Status:** 🟢 DONE (2026-06-07 — live walk PASS)
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

- [x] `POST /api/platform/admin/instances/{id}/retry-provisioning` returns 200 cho admin role + re-trigger provisioning (re-publish `tenant.created` → kiteclass-core saga → `InstanceLifecycleService.retry()` path)
- [x] 403 cho non-admin role (verified `AdminTenantProvisioningControllerIntegrationTest`: anonymous 401 / OWNER 403 / STAFF 403 / PLATFORM_ADMIN 200)
- [x] FE button visible trong admin instance detail (`/admin/instances/[id]`); click → confirm dialog → retry triggered + UI invalidates
- [x] Audit log row written mỗi retry (`TenantAuditService.recordTenantRetryRequested` → `TENANT_PROVISIONING_RETRY_TRIGGERED`, REQUIRES_NEW)
- [x] **Live walk** (RST end-to-end trên shared stack: admin retry → DB `admin_audit_log` row + saga re-run) — PASS 2026-06-07 (see Walk evidence)

## Walk evidence (per feature-ship-runtime-walk-mandate.md §3 + pre-handoff-self-test-completeness.md §2.4 — 2026-06-07)

Live walk against shared Docker stack (kitehub-subscription :8081), instance `kc1walk3` (`b40eb7b0-...`), admin actor `00000000-...-099` (`X-User-Id` + `X-User-Roles: PLATFORM_ADMIN`):

- **Setup:** flipped FE instance id=5 (slug `kc1-walk3-school`) → `FAILED`, `retry_count=1` in `kiteclass_shared.frontend_instances`.
- **POST** `/api/platform/admin/instances/{id}/retry-provisioning` → HTTP 200.
- **Saga path verified (no slug collision):** kiteclass-core log `[saga] retrying FAILED instance ... id=5` → `lifecycle.retry()` (FAILED → INITIALIZING) → outbox `instance.generating` → `instance.deployed`. Final `frontend_instances.status = DEPLOYED`. NO "Slug already in use" thrown (routed via `findActiveBySlug` retry path, not `initiate`).
- **Idempotent-skip path also verified:** retry on an already-DEPLOYED instance → log `[saga] instance exists in status DEPLOYED — idempotent skip (no re-provision)`, no re-provision.
- **Audit row:** `admin_audit_log` row `action=TENANT_PROVISIONING_RETRY_TRIGGERED`, `admin_user_id=00000000-...-099`, `target_entity_id=b40eb7b0-...`, payload with reason, `success=t`.
- **retry_count note:** stayed `1` after successful retry — correct by design: `retry()` transitions FAILED→INITIALIZING WITHOUT incrementing; the counter is bumped only on re-failure (`markFailed`), and `MAX_RETRIES` is checked before retry. No bug.

No code changes needed for GAP-953 (walked clean).

## Rescope (per wave plan §1, audit-to-gap-pipeline §2.5 fix-time state-check)

Gap over-claimed "no retry" — `lifecycle.retry()` (kiteclass-core `InstanceLifecycleService:129`) + internal endpoint `POST /api/v1/instances/{id}/retry` (kiteclass-core `InstanceController:109`) **đã tồn tại**. Bucket E delta = ADMIN endpoint + guard + audit + FE button. Path adjusted từ proposed `/api/v1/admin/tenants/{id}/...` → `/api/platform/admin/instances/{id}/retry-provisioning` để khớp routing convention thực tế (gateway `/api/platform/admin/instances/{id}/...` explicit subscription routes + existing FE `/admin/instances/[id]` page; "tenant" = kitehub `Instance`).

Architecture: kitehub-subscription publishes `tenant.created` (GAP-945 keystone) → kiteclass-core `TenantProvisioningSaga` consumes. Admin retry = re-publish `tenant.created` (same keystone mechanism) → saga re-drives provisioning. Subscription cannot call kiteclass-core `lifecycle.retry()` cross-service directly.

## Related

- Discovered in: 3-agent outside-in audit 2026-06-04
- Audit artifact: persona-review/2026-06-04-pre-walk-kc1-tenant-provisioning.md Finding 4.1
- Flow Verification Campaign §4 row KC-1
- Implemented: Wave provisioning-1 Bucket E (kitehub-subscription `AdminTenantProvisioningController` + `AdminTenantProvisioningService` + `TenantAuditService.recordTenantRetryRequested` + gateway route + FE `useRetryProvisioning` hook + button/dialog)

## Log

- **2026-06-07** KC-1 closure walk: live retry walk PASS on shared stack — HTTP 200, saga `retrying FAILED instance` (no slug collision), final DEPLOYED, audit row written with real admin actor; idempotent-skip path also verified. Flipped 🟢 DONE; git mv → `closed/`. Per `feature-ship-runtime-walk-mandate.md` §3.
- **2026-06-06** (Wave provisioning-1 Bucket E): PARTIAL — shipped admin retry endpoint `POST /api/platform/admin/instances/{id}/retry-provisioning` (PLATFORM_ADMIN guard) + audit (`recordTenantRetryRequested` reuses Bucket B `TenantAuditService`) + gateway route + FE button/dialog on `/admin/instances/[id]`. BE tests: service 3 + controller unit 4 + controller authz integration 4 (401/403/403/200) PASS. FE production build PASS. Live RST walk deferred → stays PARTIAL per `feature-ship-runtime-walk-mandate.md` §1 + `pre-handoff-self-test-completeness.md` §2.4 admin-flow.
