# GAP-954: PDPL Art 23 tenant DELETE cascade incomplete — orphan data

**Status:** 🟢 DONE (2026-06-07 — live cascade walk PASS)
**Priority:** 🔴 P0
**Domain:** Compliance
**Found:** 2026-06-04 (Wave flow-kh3 KC-1 pre-walk audit — 3-agent outside-in consensus)
**Affects:** KC-1 (Tenant lifecycle DELETE) + PDPL compliance
**Defer-to:** After Wave flow-kh3 finish

## Current State (verified 2026-06-06 — Wave provisioning-1 Bucket G)

State-check phát hiện hạ tầng cascade ĐÃ TỒN TẠI một phần (gap over-claim "incomplete"):
- `InstancePurgeService` (kitehub-subscription) đã có DB drop + S3 backup delete + 30d retention (`PURGE_RETENTION_DAYS=30`) + `SKIPPED_NO_BACKUP` safety gate.
- `InstanceStatus` (kitehub-platform) đã có SUSPENDED/DELETED/PURGED.

Bucket G ship phần DELTA còn thiếu:
- kiteclass-core `FrontendInstanceStatus` thêm SUSPENDED + DELETED (terminal) + transitions; `FrontendInstance.suspendedAt/deletedAt`; `InstanceLifecycleService.suspend/reactivate/softDelete`; Flyway `V91` (CHECK constraint + 2 cột).
- `InstancePurgeService` cascade mở rộng: MinIO/S3 logo+branding prefix purge (`BackupStorageService.deleteByPrefix("instances/{id}/")`) + DNS/custom-domain clear (`DomainService.removeCustomDomain`) + `TENANT_DELETED` audit (`TenantAuditService`, REQUIRES_NEW).

## Problem

Per `tenant-provisioning/rules.md` BR-PROV-005 + `instance-lifecycle/rules.md`: lifecycle FSM chỉ cover NOT_STARTED → INITIALIZING → GENERATING → DEPLOYED → REGENERATING → FAILED loop. KHÔNG cover SUSPEND/DELETE state-machine. Khi Owner xoá tenant trial cũ → click "Đóng trung tâm A" → BE call DELETE → orphan data (DB row, MinIO bucket, DNS record, S3 logo). 6 tháng sau bị PDPL audit hỏi "retain dữ liệu sau xoá account?". Surfaced: persona Finding 2.3.

## Proposed Fix

Extend FSM với SUSPENDED + DELETED states + transitions (per benchmark §A row 8: soft-delete 30d → permanent). Implement cascade: tenant DB drop / MinIO bucket purge / DNS record delete / S3 logo delete + audit log `TENANT_DELETED` event. Wave deferred to off-boarding scope per GAP-201 partial.

## Acceptance Criteria

- [x] FSM định nghĩa transition DEPLOYED → SUSPENDED → DELETED + grace 30d — `FrontendInstanceStatus` + V91; unit-tested (`FrontendInstanceStatusTest`, `InstanceLifecycleServiceTest`)
- [x] Cascade delete xoá: DB + MinIO bucket (prefix) + DNS record + S3 logo — `InstancePurgeService` cascade; unit-tested (`InstancePurgeServiceTest.Pdpl23Cascade`). ⚠️ live-walk on prod-equivalent stack pending (xem Remaining)
- [x] PDPL Art 23 retention policy documented + tested — `off-boarding/rules.md` §7b + 30d retention unit test
- [x] Audit row `TENANT_DELETED` written — `TenantAuditService.recordTenantDeleted` (REQUIRES_NEW); unit-tested

## Walk evidence (per feature-ship-runtime-walk-mandate.md §3 — 2026-06-07)

Live cascade walk against shared Docker stack (kitehub-subscription :8081 + Postgres + MinIO), disposable tenant `kc1walk` (`5f233e29-...`), admin actor `00000000-...-099`:

- **Soft delete** `DELETE /api/platform/instances/{id}` → HTTP 204; `instances.status = DELETED`, `deleted = t`.
- **Safety gate:** inserted a COMPLETED `backup_records` row (walk aid); a separate test confirmed `SKIPPED_NO_BACKUP` when no backup exists.
- **Purge** `DELETE /api/platform/instances/{id}/purge` (with `X-User-Id` admin) → HTTP 200, `PurgeResult.status = SUCCESS`: `databaseDropped=true`, `backupFilesDeleted=1`, `dnsRecordCleared=true`, `tenantDeletedAuditWritten=true`, `s3ObjectsDeleted=0`.
- **Final state:** `instances.status = PURGED`; `backup_records.status = DELETED`.
- **TENANT_DELETED audit:** `admin_audit_log` row `action=TENANT_DELETED`, `admin_user_id=00000000-...-099`, payload `{databaseDropped,backupFilesDeleted,s3ObjectsDeleted,dnsRecordCleared,purgedAt}`, `success=t`.

**Bugs surfaced + fixed this PR:**
- **Bug #2 (V65):** purge always failed in production. `BackupStatus` enum has `DELETED` but the `chk_backup_records_status` CHECK constraint (V59) had `RESTORED` instead — so step-4 "mark backup DELETED" CHECK-violated → 409. Since purge REQUIRES a COMPLETED backup, EVERY real purge failed. Fix: migration `V65__backup_records_status_add_deleted.sql` widens the constraint to include `DELETED`.
- **Bug #3 (actor + audit isolation):** purge passed a null actor → `SYSTEM_ACTOR` zero-UUID → FK violation `fk_admin_audit_log_user` (admin_user_id NOT NULL + FK to users) → REQUIRES_NEW txn marked rollback-only → `UnexpectedRollbackException` corrupted the result to FAILED even though the cascade committed (the `audit-service-isolation.md` §2 anti-pattern). Fix: (a) `DELETE /{id}/purge` now forwards `X-User-Id` → `adminPurge(id, actorId)` → `recordTenantDeleted(actorId)`; (b) executePurge wraps the audit call in its own try/catch so an audit failure can never flip an already-committed purge to FAILED.

**Deferred (not local-walkable, noted not failed):**
- Actual Cloudflare DNS record removal — `DomainService.removeCustomDomain` is invoked (control-plane), but the real Cloudflare API call is not exercisable on the local stack. DEFER.
- MinIO objects deleted = 0 because the test tenant has no branding assets; the `deleteByPrefix("instances/{id}/")` call-path is exercised (returns 0). For a tenant with assets, prefix purge is unit-tested.
- FrontendInstance SUSPENDED→DELETED FSM (V91) is service-level (`InstanceLifecycleService.suspend/softDelete`) with NO REST endpoint and NO cross-service purge consumer in kiteclass-core; it is unit-tested + the V91 CHECK constraint (verified to include SUSPENDED/DELETED). FE-side cascade is a separate kiteclass-core admin op, not driven by the kitehub purge event.

## Related

- Discovered in: 3-agent outside-in audit 2026-06-04
- Audit artifact: persona-review/2026-06-04-pre-walk-kc1-tenant-provisioning.md Finding 2.3
- Sister: GAP-201 (Tenant Off-boarding Runbook PARTIAL 50%)
- Flow Verification Campaign §4 row KC-1
- Implemented: Wave provisioning-1 Bucket G (PR pending)

## Log

- **2026-06-07** KC-1 closure walk: live soft-delete + purge cascade PASS on shared stack (DELETED → PURGED + backup DELETED + TENANT_DELETED audit). Surfaced + fixed Bug #2 (V65 backup-status constraint missing DELETED → purge always failed in prod) + Bug #3 (purge actor not propagated → FK violation + audit-isolation rollback corrupting result). Flipped 🟢 DONE; git mv → `closed/`. Per `feature-ship-runtime-walk-mandate.md` §3.
- **2026-06-06** (Wave provisioning-1 Bucket G): Rescope OPEN → PARTIAL 90% per state-check (infra đã tồn tại một phần). Ship DELTA: kiteclass-core FSM SUSPENDED/DELETED + V91 migration + InstancePurgeService cascade (MinIO prefix purge + DNS clear + TENANT_DELETED audit) + PDPL Art 23 doc (`off-boarding/rules.md` §7b) + unit tests (both modules `./mvnw test` PASS). Remaining: live cascade walk on prod-equivalent stack (no Docker in agent worktree).
