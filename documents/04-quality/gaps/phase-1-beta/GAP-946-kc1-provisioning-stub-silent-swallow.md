# GAP-946: KC-1 provisioning stub mode + silent DB exception swallow

**Status:** 🟡 PARTIAL
**Priority:** 🔴 P0
**Domain:** Backend
**Found:** 2026-06-04 (Wave flow-kh3 KC-1 pre-walk audit — 3-agent outside-in consensus)
**Affects:** KC-1 (Tenant provisioning) — `kitehub-subscription` DatabaseProvisioningService + InstanceService
**Defer-to:** After Wave flow-kh3 finish

## Current State (verified 2026-06-06, Wave provisioning-1 Bucket A)

- ✅ **Silent-swallow FIXED** (data-corruption part): 3 `catch(Exception){log;/*Continue*/}` sites in `InstanceService` (createTrialInstance:170 / activatePendingInstance:249 / registerInstance:328) removed → `provisionDatabase` failure now propagates so the `@Transactional` creation rolls back instead of persisting a row with `databaseUrl='pending'`. `provisionDatabase` only throws in `lifecycleEnabled` (prod) mode; stub/local simulates success → fail-fast never triggers in tests. Regression test `InstanceServiceTest.shouldPropagateWhenDatabaseProvisioningFails`. (subscription `InstanceStatus` has no FAILED value → fix is rollback-propagate, not status-flip.)
- ✅ `database.lifecycle.enabled=true` already set `application-production.yml:57` (AC #1 satisfied pre-gap).
- ✅ **Saga now reachable** (GAP-945 Bucket A wiring 2026-06-06): `TenantProvisioningSaga.provision()` is wired to `tenant.created.queue` via `TenantCreatedEventConsumer` — saga executes on beta signup (was orphan). So when `provisionInfrastructure` becomes real, it will actually run.
- 🔴 **STILL OPEN** (the GAP-946 core remaining): `TenantProvisioningSaga.provisionInfrastructure` (kiteclass-core:83-86) still log-only stub — real infra provisioning (DB schema / MinIO bucket / DNS) + async FAILED state-machine/retry not yet implemented. Saga wiring done (GAP-945); this is the infra-execution delta.

## Problem

`TenantProvisioningSaga.provisionInfrastructure` (kiteclass-core line 83-86) chỉ log "infrastructure provisioning stub"; `DatabaseProvisioningService.lifecycleEnabled=false` mặc định → DB không được tạo thực. `InstanceService:170-176` wrap `databaseProvisioningService.provisionDatabase(saved.getId())` trong `catch (Exception e) { log.error(...); /* Continue */ }`. Instance row lưu với `databaseUrl="pending"`, exception swallowed. User thấy "tenant ready" nhưng DB không tồn tại; KC-2+ subsequent flows fail với errors khó hiểu. Surfaced: persona Finding 1.1 + matrix A1×E1×EC2.

## Proposed Fix

Wire `database.lifecycle.enabled=true` cho real DB provisioning OR rethrow `DatabaseProvisioningException` (stop saga, mark instance FAILED) thay vì silent swallow. Validate `databaseUrl != "pending"` post-provision.

## Acceptance Criteria

- [x] `grep "database.lifecycle.enabled"` returns true cho prod profile (`application-production.yml:57`)
- [x] Provisioning exception KHÔNG còn silent-swallow → propagate → `@Transactional` rollback (no row persisted with `databaseUrl='pending'` on failure). Unit-verified `InstanceServiceTest.shouldPropagateWhenDatabaseProvisioningFails`.
- [ ] ~~exception → status FAILED~~ reframe: subscription `InstanceStatus` has no FAILED → async FAILED + retry is the saga path (GAP-945). Synchronous path = rollback-propagate (done).
- [ ] Saga `provisionInfrastructure` real (not stub) + live walk 0 `pending` rows post-walk — coupled GAP-945 (saga wiring)

## Related

- Discovered in: 3-agent outside-in audit 2026-06-04
- Audit artifact: persona-review/2026-06-04-pre-walk-kc1-{tenant-provisioning,failure-mode-matrix}.md
- Flow Verification Campaign §4 row KC-1
