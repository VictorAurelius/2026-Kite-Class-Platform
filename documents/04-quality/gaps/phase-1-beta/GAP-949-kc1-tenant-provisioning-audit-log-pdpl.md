# GAP-949: Tenant provisioning có 0 audit log row — vi phạm PDPL Art 11

**Status:** 🟡 PARTIAL
**Priority:** 🔴 P0
**Domain:** Backend
**Found:** 2026-06-04 (Wave flow-kh3 KC-1 pre-walk audit — 3-agent outside-in consensus)
**Affects:** KC-1 (Tenant provisioning) + audit/compliance scope
**Defer-to:** After Wave flow-kh3 finish
**Completion:** 75% — subscription-side `TENANT_PROVISIONED` audit wired (Wave provisioning-1 Bucket B); live-walk verify + optional core-side saga-step audit remaining

## Problem

`AuthService.registerFromBetaInvite` chỉ log via `log.info(...)`. KHÔNG có row written to `admin_audit_logs` hoặc domain audit table cho `TENANT_PROVISIONED` event. PDPL Art 11 + Wave 85 immutable admin audit work KHÔNG cover tenant creation. Không trả lời được "tenant X provisioned khi nào, qua beta-invite nào, IP, fingerprint" post-incident. `TenantProvisioningSaga.java:53-86` cũng dùng `log.info` toàn bộ — không có `auditLog.recordProvisioningStep(...)` với REQUIRES_NEW propagation per `audit-service-isolation.md`. Surfaced: matrix A1×E4×EC2 + persona Finding 4.2.

## Proposed Fix

Wire audit log row insert (event_type=`TENANT_PROVISIONED`) trong AuthService + per saga step (`SAGA_STEP_*`) với `@Transactional(propagation = REQUIRES_NEW)` per `audit-service-isolation.md`. Capture: tenantId, ownerId, betaInviteId, IP, userAgent, timestamp, sagaStepNum.

## Acceptance Criteria

- [x] Subscription-side `TENANT_PROVISIONED` audit row written on beta-invite provisioning (`TenantAuditService.recordTenantProvisioned` wired into `AuthService.registerFromBetaInvite` after `createTrialInstance` + `publishTenantCreated`)
- [x] `@Transactional(propagation = REQUIRES_NEW)` + try/catch per `audit-service-isolation.md` §1 — audit failure ≠ registration failure (unit-tested: repo `save` throws → `recordTenantProvisioned` does not propagate)
- [x] Audit-helper coordination: `TenantAuditService` created with `recordTenantProvisioned(...)` (consistent `recordTenant*` naming) so Bucket G adds `recordTenantDeleted(...)` to the same class
- [ ] Live-walk: `psql -c "SELECT * FROM admin_audit_log WHERE action = 'TENANT_PROVISIONED' LIMIT 5"` post-walk returns rows — PARTIAL per `feature-ship-runtime-walk-mandate.md` (live walk pending). NOTE table is `admin_audit_log` (singular) with `action` column (entity has no `event_type`); the AC's original `admin_audit_logs` / `event_type` wording was pre-implementation.
- [ ] Core-side saga lifecycle audit (`SAGA_STEP_*` INITIALIZING/GENERATING/DEPLOYED/FAILED in kiteclass-core) — deferred (cross-service, lower priority; subscription-side `TENANT_PROVISIONED` is the AC core)

### Implementation note — fresh-owner FK at runtime

`admin_audit_log.admin_user_id` is `NOT NULL` + FK to `users(id)`, recorded as the new tenant owner. Because the audit runs in `REQUIRES_NEW` (separate physical txn) and the owner row is inserted in the still-uncommitted parent registration txn, the owner may not be visible to the audit txn under READ COMMITTED → FK check could fail at runtime (swallowed → no row). This is flagged for live-walk verification; if confirmed, the follow-up is to move the audit to an `@TransactionalEventListener(phase = AFTER_COMMIT)` (still REQUIRES_NEW per `audit-service-isolation.md` §3 event-boundary exemption) so the committed owner is visible. Unit tests (Mockito) verify the wiring + isolation contract independent of FK timing.

## Related

- Discovered in: 3-agent outside-in audit 2026-06-04
- Audit artifact: persona-review/2026-06-04-pre-walk-kc1-{failure-mode-matrix,tenant-provisioning}.md
- Sister rule: `.claude/rules/audit-service-isolation.md` §1 (REQUIRES_NEW mandate) + `design-patterns.md` §3.11
- Implemented: Wave provisioning-1 Bucket B — `TenantAuditService` + `AuthService.registerFromBetaInvite` wiring + 2 unit test classes
- Flow Verification Campaign §4 row KC-1
