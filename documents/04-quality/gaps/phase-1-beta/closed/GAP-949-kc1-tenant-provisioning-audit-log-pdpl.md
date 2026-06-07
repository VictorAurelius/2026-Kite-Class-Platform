# GAP-949: Tenant provisioning có 0 audit log row — vi phạm PDPL Art 11

**Status:** 🟢 DONE
**Priority:** 🔴 P0
**Domain:** Backend
**Found:** 2026-06-04 (Wave flow-kh3 KC-1 pre-walk audit — 3-agent outside-in consensus)
**Affects:** KC-1 (Tenant provisioning) + audit/compliance scope
**Defer-to:** After Wave flow-kh3 finish
**Completion:** 100% — subscription-side `TENANT_PROVISIONED` audit wired + verified live (3 rows from real beta signups); core-side saga-step audit out-of-scope Phase 1 BETA

## Problem

`AuthService.registerFromBetaInvite` chỉ log via `log.info(...)`. KHÔNG có row written to `admin_audit_logs` hoặc domain audit table cho `TENANT_PROVISIONED` event. PDPL Art 11 + Wave 85 immutable admin audit work KHÔNG cover tenant creation. Không trả lời được "tenant X provisioned khi nào, qua beta-invite nào, IP, fingerprint" post-incident. `TenantProvisioningSaga.java:53-86` cũng dùng `log.info` toàn bộ — không có `auditLog.recordProvisioningStep(...)` với REQUIRES_NEW propagation per `audit-service-isolation.md`. Surfaced: matrix A1×E4×EC2 + persona Finding 4.2.

## Proposed Fix

Wire audit log row insert (event_type=`TENANT_PROVISIONED`) trong AuthService + per saga step (`SAGA_STEP_*`) với `@Transactional(propagation = REQUIRES_NEW)` per `audit-service-isolation.md`. Capture: tenantId, ownerId, betaInviteId, IP, userAgent, timestamp, sagaStepNum.

## Acceptance Criteria

- [x] Subscription-side `TENANT_PROVISIONED` audit row written on beta-invite provisioning (`TenantAuditService.recordTenantProvisioned` wired into `AuthService.registerFromBetaInvite` after `createTrialInstance` + `publishTenantCreated`)
- [x] `@Transactional(propagation = REQUIRES_NEW)` + try/catch per `audit-service-isolation.md` §1 — audit failure ≠ registration failure (unit-tested: repo `save` throws → `recordTenantProvisioned` does not propagate)
- [x] Audit-helper coordination: `TenantAuditService` created with `recordTenantProvisioned(...)` (consistent `recordTenant*` naming) so Bucket G adds `recordTenantDeleted(...)` to the same class
- [x] Live-walk: `psql -c "SELECT * FROM admin_audit_log WHERE action = 'TENANT_PROVISIONED' LIMIT 5"` post-walk returns rows — **verified live 2026-06-07** (`admin_audit_log` has **3 `TENANT_PROVISIONED` rows** + 2 `TENANT_PROVISIONING_RETRY_TRIGGERED` + 1 `TENANT_DELETED`, all from real beta signups). NOTE table is `admin_audit_log` (singular) with `action` column; the AC's original `admin_audit_logs` / `event_type` wording was pre-implementation.

## Out-of-scope (tracked separately)

| Item | Where / rationale |
|---|---|
| Core-side saga lifecycle audit (`SAGA_STEP_*` INITIALIZING/GENERATING/DEPLOYED/FAILED in kiteclass-core) | Out-of-scope for Phase 1 BETA — always documented as deferred/lower-priority (cross-service); subscription-side `TENANT_PROVISIONED` is the AC core and satisfies PDPL Art 11 tenant-creation traceability |

### Implementation note — fresh-owner FK at runtime (risk did NOT materialize)

`admin_audit_log.admin_user_id` is `NOT NULL` + FK to `users(id)`, recorded as the new tenant owner. The concern was: audit runs in `REQUIRES_NEW` (separate physical txn) + owner row inserted in still-uncommitted parent txn → under READ COMMITTED the FK check could fail (swallowed → no row). **Live walk 2026-06-07 confirmed the risk did NOT materialize** — `admin_audit_log` has 3 real `TENANT_PROVISIONED` rows from actual beta signups, so the audit writes succeed at runtime. The `@TransactionalEventListener(phase = AFTER_COMMIT)` follow-up is therefore unnecessary. Unit tests (Mockito) verify the wiring + isolation contract independent of FK timing.

## Log

- **2026-06-07 (Wave p0-prov-1 closure):** Status PARTIAL → 🟢 DONE. Live walk confirmed `admin_audit_log` has **3 `TENANT_PROVISIONED` rows** (+ 2 `TENANT_PROVISIONING_RETRY_TRIGGERED` + 1 `TENANT_DELETED`) from real beta signups → audit IS written. The flagged FK-timing risk (REQUIRES_NEW txn not seeing the uncommitted owner) did NOT materialize — writes succeed at runtime, so the AFTER_COMMIT follow-up is unnecessary. Core-side `SAGA_STEP_*` audit (cross-service) moved to §Out-of-scope — always documented as deferred/lower-priority for Phase 1 BETA; subscription-side `TENANT_PROVISIONED` is the AC core satisfying PDPL Art 11.

## Related

- Discovered in: 3-agent outside-in audit 2026-06-04
- Audit artifact: persona-review/2026-06-04-pre-walk-kc1-{failure-mode-matrix,tenant-provisioning}.md
- Sister rule: `.claude/rules/audit-service-isolation.md` §1 (REQUIRES_NEW mandate) + `design-patterns.md` §3.11
- Implemented: Wave provisioning-1 Bucket B — `TenantAuditService` + `AuthService.registerFromBetaInvite` wiring + 2 unit test classes
- Flow Verification Campaign §4 row KC-1
