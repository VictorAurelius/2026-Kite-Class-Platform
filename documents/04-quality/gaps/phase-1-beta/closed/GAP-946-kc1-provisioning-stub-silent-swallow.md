# GAP-946: KC-1 provisioning stub mode + silent DB exception swallow

**Status:** 🟢 DONE (Phase 1 BETA scope — defensive fail-loud + DB provisioning verified; real branded-frontend infra → GAP-1055 Phase 1.5)
**Priority:** 🔴 P0
**Domain:** Backend
**Found:** 2026-06-04 (Wave flow-kh3 KC-1 pre-walk audit — 3-agent outside-in consensus)
**Affects:** KC-1 (Tenant provisioning) — `kitehub-subscription` DatabaseProvisioningService + InstanceService
**Defer-to:** After Wave flow-kh3 finish

## Current State (verified 2026-06-06, Wave provisioning-1 Bucket A)

- ✅ **Silent-swallow FIXED** (data-corruption part): 3 `catch(Exception){log;/*Continue*/}` sites in `InstanceService` (createTrialInstance:170 / activatePendingInstance:249 / registerInstance:328) removed → `provisionDatabase` failure now propagates so the `@Transactional` creation rolls back instead of persisting a row with `databaseUrl='pending'`. `provisionDatabase` only throws in `lifecycleEnabled` (prod) mode; stub/local simulates success → fail-fast never triggers in tests. Regression test `InstanceServiceTest.shouldPropagateWhenDatabaseProvisioningFails`. (subscription `InstanceStatus` has no FAILED value → fix is rollback-propagate, not status-flip.)
- ✅ `database.lifecycle.enabled=true` already set `application-production.yml:57` (AC #1 satisfied pre-gap).
- ✅ **Saga now reachable** (GAP-945 Bucket A wiring 2026-06-06): `TenantProvisioningSaga.provision()` is wired to `tenant.created.queue` via `TenantCreatedEventConsumer` — saga executes on beta signup (was orphan). So when `provisionInfrastructure` becomes real, it will actually run.
- 🔴 **STILL OPEN** (the GAP-946 core remaining): `TenantProvisioningSaga.provisionInfrastructure` (kiteclass-core:136-139) still log-only stub — real infra provisioning (DB schema / MinIO bucket / DNS) + async FAILED state-machine/retry not yet implemented. Saga wiring done (GAP-945); this is the infra-execution delta.

## Fix-time state-check (per audit-to-gap-pipeline.md §2.8, Wave p0-1 Bucket B 2026-06-07)

Gap age: 3 days; drift-class. Empirical verification trước khi fix:

- **Tenant DB provisioning thật ĐÃ hoạt động** qua subscription-side: `DatabaseProvisioningService.provisionDatabase()` (lifecycleEnabled prod, `application-production.yml:57`) tạo physical DB + chạy migration + cập nhật `databaseUrl` "pending"→real + save (`DatabaseProvisioningService.java:80-93`). Saga stub `provisionInfrastructure` là layer branded-frontend-instance riêng (javadoc: "responsibility of a separate ops service"), KHÔNG phải tenant DB.
- **Silent-swallow + propagate**: ✅ đã done (3 call site `InstanceService` gọi `provisionDatabase` trực tiếp, `@Transactional` class-level rollback).
- **FAILED state machine + compensation + retry**: ✅ đã có (`TenantProvisioningSaga.compensate`→`markFailed`, `retry()`, `FrontendInstanceStatus.FAILED`, GAP-952 alarm, `ProvisioningStuckSweep`).

**Verdict (§2.8 matrix): Symptom partially present → scope-revise.** Plan Bucket B "fail-loud fix" gần như đã done; delta thật còn lại = (a) defensive post-provision validation (small, shipped này) + (b) `provisionInfrastructure` real DB-schema/MinIO/DNS (large kiteclass-core task, tách wave riêng). Gap giữ **PARTIAL** per `gap-done-discipline.md` §3.

## Defensive hardening shipped (Wave p0-1 Bucket B, 2026-06-07)

`InstanceService.assertDatabaseProvisioned(instance)` — sau mỗi `provisionDatabase` call (3 site: createTrialInstance / activatePendingInstance / registerInstance), assert `databaseUrl != null/empty/"pending"`, else throw `IllegalStateException` → `@Transactional` rollback. Defense-in-depth chống provisionDatabase silent no-op để lại row half-provisioned (`databaseUrl='pending'`) mà KC-2+ flow gặp lỗi khó hiểu. Test: `InstanceServiceTest.shouldFailLoudWhenDatabaseUrlStillPendingAfterProvision`.

**Remaining (tách wave riêng):** `provisionInfrastructure` real implementation. Saga stub acceptable Phase 1 BETA (tenant DB do subscription provision; DNS/MinIO defer).

## Problem

`TenantProvisioningSaga.provisionInfrastructure` (kiteclass-core line 83-86) chỉ log "infrastructure provisioning stub"; `DatabaseProvisioningService.lifecycleEnabled=false` mặc định → DB không được tạo thực. `InstanceService:170-176` wrap `databaseProvisioningService.provisionDatabase(saved.getId())` trong `catch (Exception e) { log.error(...); /* Continue */ }`. Instance row lưu với `databaseUrl="pending"`, exception swallowed. User thấy "tenant ready" nhưng DB không tồn tại; KC-2+ subsequent flows fail với errors khó hiểu. Surfaced: persona Finding 1.1 + matrix A1×E1×EC2.

## Proposed Fix

Wire `database.lifecycle.enabled=true` cho real DB provisioning OR rethrow `DatabaseProvisioningException` (stop saga, mark instance FAILED) thay vì silent swallow. Validate `databaseUrl != "pending"` post-provision.

## Acceptance Criteria

- [x] `grep "database.lifecycle.enabled"` returns true cho prod profile (`application-production.yml:57`)
- [x] Provisioning exception KHÔNG còn silent-swallow → propagate → `@Transactional` rollback (no row persisted with `databaseUrl='pending'` on failure). Unit-verified `InstanceServiceTest.shouldPropagateWhenDatabaseProvisioningFails`.
- [x] Defensive `assertDatabaseProvisioned(instance)` post-provision validation shipped (3 `InstanceService` sites: createTrialInstance / activatePendingInstance / registerInstance + test `shouldFailLoudWhenDatabaseUrlStillPendingAfterProvision`)
- [x] Live walk: 0 half-provisioned instances post-walk — **verified live 2026-06-07** (0/9 instances have `database_url` null/'pending'; provisioning completes: audit rows + saga DEPLOYED + tenant-ready email)

## Scope-split (Phase 1.5 → GAP-1055)

| Item | Where / rationale |
|---|---|
| ~~exception → subscription `InstanceStatus` FAILED~~ | Reframe: subscription `InstanceStatus` has no FAILED value → async FAILED + retry IS the saga path (GAP-945, wired). Synchronous path = rollback-propagate (done above). |
| Saga `provisionInfrastructure` real implementation (branded-frontend per-tenant DB schema / MinIO bucket / DNS) | **GAP-1055 (Phase 1.5)** — Saga stub acceptable Phase 1 BETA per gap §"Defensive hardening shipped": tenant DB is provisioned by subscription-side `DatabaseProvisioningService`; branded-frontend infra layer deferred to Phase 1.5 paid multi-tenant frontend isolation. |

## Log

- **2026-06-07 (Wave p0-prov-1 closure):** Status PARTIAL → 🟢 DONE for Phase 1 BETA scope. Defensive `assertDatabaseProvisioned()` shipped (3 InstanceService sites + test). Fix-time state-check (`audit-to-gap-pipeline.md` §2.8): silent-swallow already propagate-fixed + FAILED state machine already in saga path (GAP-945); residual delta = (a) defensive post-provision validation (shipped) + (b) real `provisionInfrastructure` branded-frontend infra (large kiteclass-core task). Live walk verified **0/9 instances half-provisioned** (no `database_url` null/'pending'); provisioning completes (audit rows + saga DEPLOYED + tenant-ready email). Real `provisionInfrastructure` (log-only stub) scope-split → **GAP-1055** (Phase 1.5); acceptable Phase 1 BETA because tenant DB is provisioned by subscription-side `DatabaseProvisioningService`.

## Related

- Discovered in: 3-agent outside-in audit 2026-06-04
- Audit artifact: persona-review/2026-06-04-pre-walk-kc1-{tenant-provisioning,failure-mode-matrix}.md
- Scope-split: GAP-1055 (Phase 1.5 — real `provisionInfrastructure` branded-frontend infra)
- Sister: GAP-945 (saga wiring — saga now reachable)
- Flow Verification Campaign §4 row KC-1
