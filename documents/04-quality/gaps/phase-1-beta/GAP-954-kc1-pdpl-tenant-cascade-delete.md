# GAP-954: PDPL Art 23 tenant DELETE cascade incomplete — orphan data

**Status:** 🟡 PARTIAL (90% — Wave provisioning-1 Bucket G)
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
- kiteclass-core `FrontendInstanceStatus` thêm SUSPENDED + DELETED (terminal) + transitions; `FrontendInstance.suspendedAt/deletedAt`; `InstanceLifecycleService.suspend/reactivate/softDelete`; Flyway `V90` (CHECK constraint + 2 cột).
- `InstancePurgeService` cascade mở rộng: MinIO/S3 logo+branding prefix purge (`BackupStorageService.deleteByPrefix("instances/{id}/")`) + DNS/custom-domain clear (`DomainService.removeCustomDomain`) + `TENANT_DELETED` audit (`TenantAuditService`, REQUIRES_NEW).

## Problem

Per `tenant-provisioning/rules.md` BR-PROV-005 + `instance-lifecycle/rules.md`: lifecycle FSM chỉ cover NOT_STARTED → INITIALIZING → GENERATING → DEPLOYED → REGENERATING → FAILED loop. KHÔNG cover SUSPEND/DELETE state-machine. Khi Owner xoá tenant trial cũ → click "Đóng trung tâm A" → BE call DELETE → orphan data (DB row, MinIO bucket, DNS record, S3 logo). 6 tháng sau bị PDPL audit hỏi "retain dữ liệu sau xoá account?". Surfaced: persona Finding 2.3.

## Proposed Fix

Extend FSM với SUSPENDED + DELETED states + transitions (per benchmark §A row 8: soft-delete 30d → permanent). Implement cascade: tenant DB drop / MinIO bucket purge / DNS record delete / S3 logo delete + audit log `TENANT_DELETED` event. Wave deferred to off-boarding scope per GAP-201 partial.

## Acceptance Criteria

- [x] FSM định nghĩa transition DEPLOYED → SUSPENDED → DELETED + grace 30d — `FrontendInstanceStatus` + V90; unit-tested (`FrontendInstanceStatusTest`, `InstanceLifecycleServiceTest`)
- [x] Cascade delete xoá: DB + MinIO bucket (prefix) + DNS record + S3 logo — `InstancePurgeService` cascade; unit-tested (`InstancePurgeServiceTest.Pdpl23Cascade`). ⚠️ live-walk on prod-equivalent stack pending (xem Remaining)
- [x] PDPL Art 23 retention policy documented + tested — `off-boarding/rules.md` §7b + 30d retention unit test
- [x] Audit row `TENANT_DELETED` written — `TenantAuditService.recordTenantDeleted` (REQUIRES_NEW); unit-tested

## Remaining (PARTIAL → DONE)

- Live cascade walk trên prod-equivalent stack (Postgres + MinIO + real DNS) per `feature-ship-runtime-walk-mandate.md` — agent không có Docker stack up. Unit tests (Mockito) verify orchestration + safety gate; live walk xác minh actual S3 prefix delete + DNS provider state + audit row. Flip DONE sau live walk.

## Related

- Discovered in: 3-agent outside-in audit 2026-06-04
- Audit artifact: persona-review/2026-06-04-pre-walk-kc1-tenant-provisioning.md Finding 2.3
- Sister: GAP-201 (Tenant Off-boarding Runbook PARTIAL 50%)
- Flow Verification Campaign §4 row KC-1
- Implemented: Wave provisioning-1 Bucket G (PR pending)

## Log

- **2026-06-06** (Wave provisioning-1 Bucket G): Rescope OPEN → PARTIAL 90% per state-check (infra đã tồn tại một phần). Ship DELTA: kiteclass-core FSM SUSPENDED/DELETED + V90 migration + InstancePurgeService cascade (MinIO prefix purge + DNS clear + TENANT_DELETED audit) + PDPL Art 23 doc (`off-boarding/rules.md` §7b) + unit tests (both modules `./mvnw test` PASS). Remaining: live cascade walk on prod-equivalent stack (no Docker in agent worktree).
