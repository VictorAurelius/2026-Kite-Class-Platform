# GAP-945: KC saga not wired — kitehub-subscription thiếu `tenant.created` publisher

**Status:** 🟢 DONE
**Priority:** 🔴 P0
**Domain:** Backend
**Found:** 2026-06-04 (Wave flow-kh3 KC-1 pre-walk audit — 3-agent outside-in consensus)
**Affects:** KC-1 (Tenant provisioning) — KH-2b → KC-1 chain critical path
**Defer-to:** After Wave flow-kh3 finish (per user direction 2026-06-04)

## Resolution (Wave provisioning-1 Bucket A — 2026-06-06)

**Saga wiring SHIPPED + verified** (keystone unblock cho buckets B–G — saga contract frozen + reachable):

- ✅ **Publisher:** `AuthService.registerFromBetaInvite` → `publishTenantCreated(instance)` → `SubscriptionEventEmitter.emit(instanceId, "TENANT_CREATED", "tenant.created", json)` (outbox-backed + fast-path). Payload `{tenantId, slug, audience, tone}` build qua `SubscriptionEventEmitter.escape(...)`.
- ✅ **Consumer:** NEW `TenantCreatedEventConsumer` (`@RabbitListener(queues="tenant.created.queue")`) → deserialize → `TenantProvisioningSaga.provision(event)`. Ack-on-failure (saga compensate nội bộ; retry admin-driven GAP-953, không poison-loop broker).
- ✅ **Topology:** `RabbitConfig` declare DirectExchange `email.exchange` + queue `tenant.created.queue` + binding (routing key `tenant.created`). `TenantCreatedEvent` thêm `@Jacksonized` để Jackson deserialize immutable `@Value` payload.
- ✅ **Verified:** `TenantCreatedEventConsumerTest` 3/3 + `AuthServiceTenantCreatedPublishTest` 1/1 (CI guards) + `TenantCreatedSagaWiringIT` 1/1 (Testcontainers RabbitMQ round-trip — publish→broker→consumer→saga.provision, raw-UTF8 GAP-925 shape confirmed). `TenantProvisioningSagaTest` 5/5 (@Jacksonized non-breaking).

**RESOLVED (Wave p0-prov-1 closure — 2026-06-07):**

- ✅ **Live KC-1 full-stack walk** — saga verified end-to-end on shared stack (publisher + `@RabbitListener` consumer + saga → kiteclass-core `FrontendInstance` DEPLOYED; no slug collision).
- ✅ **AC #3 status transition** — reframed + closed (see §AC #3 reframe above): subscription-side `Instance.status` PENDING → TRIAL on `tenant.deployed` is wired (`TenantDeployedEventConsumer` → `InstanceService.markProvisioned`) + executes live; provisioning DEPLOYED state lives on kiteclass-core `FrontendInstance`.
- 🟡 Real `provisionInfrastructure` (branded-frontend infra, still log-only stub) — scope-split to **GAP-1055** (Phase 1.5); not a Phase 1 BETA blocker (tenant DB provisioned by subscription-side `DatabaseProvisioningService`).

## Live-walk re-confirmation (KC-1 closure walk — 2026-06-07)

KC-1 closure walk (GAP-953 retry path) re-confirmed the saga end-to-end on the shared stack: re-publish `tenant.created` → kiteclass-core `TenantCreatedEventConsumer` → `TenantProvisioningSaga.provision()` → FAILED→INITIALIZING→GENERATING→**DEPLOYED** (kiteclass-core `frontend_instances.status`), no slug collision. AC #1 (publisher) + AC #2 (`@RabbitListener` consumer) grep-verified present.

## AC #3 reframe + closure (Wave p0-prov-1 — 2026-06-07)

AC #3 was mis-framed as a subscription-side `Instance.status` "INITIALIZING → DEPLOYED" transition. The provisioning "DEPLOYED" state correctly lives on the **kiteclass-core `FrontendInstance`** (verified live: FAILED→INITIALIZING→GENERATING→DEPLOYED), not on the kitehub `Instance`. The actual subscription-side transition IS wired + verified: `TenantDeployedEventConsumer` → `InstanceService.markProvisioned` flips the kitehub `Instance` **PENDING → TRIAL** on the `tenant.deployed` event (mirrors `activatePendingInstance`, sets trial timestamps). Executes live: a PENDING instance flips to TRIAL; a TRIAL test instance correctly no-ops (the earlier "stays TRIAL" observation was a TRIAL test instance — a correct no-op, not a gap). Real `provisionInfrastructure` (branded-frontend infra) remains a kiteclass-core stub — scope-split to GAP-1055 (Phase 1.5), not a Phase 1 BETA blocker (tenant DB provisioned by subscription-side `DatabaseProvisioningService`).

## Problem

`AuthService.registerFromBetaInvite:218` (kitehub-subscription) gọi `instanceService.createTrialInstance(...)` synchronously và KHÔNG enqueue `tenant.created` event (no outbox enqueue / no `rabbitTemplate.convertAndSend`). `TenantProvisioningSaga` trong kiteclass-core tồn tại như orphan code — không có `@RabbitListener(queues = "tenant.created.queue")` consumer. Hệ quả: KC tenant DB không được tạo, `Instance.status` stuck `INITIALIZING` mãi mãi, Owner login vào `kc-<slug>.kitehub.me/admin` → 404 hoặc spinner forever. Surfaced cross-audit: persona simulation Finding 1.2 + failure-mode matrix A1×E5×EC2 + A1×E8×EC2.

## Proposed Fix

Thêm outbox enqueue `tenant.created` trong `AuthService.registerFromBetaInvite` sau commit tx; wire `@RabbitListener` consumer trong kiteclass-core invoking `TenantProvisioningSaga.handle(event)`.

## Acceptance Criteria

- [x] `grep -rn "tenant.created\|TenantCreatedEvent" kitehub/kitehub-subscription/src/main/java` ≥1 publisher hit — **verified** (`AuthService.registerFromBetaInvite` → `publishTenantCreated` → `SubscriptionEventEmitter.emit`)
- [x] `grep -rn "@RabbitListener.*tenant" kiteclass/kiteclass-core/src/main/java` ≥1 consumer hit — **verified** (`TenantCreatedEventConsumer` @RabbitListener tenant.created.queue → `TenantProvisioningSaga.provision`)
- [x] **(reframed)** Post-`tenant.deployed` walk: subscription-side `Instance.status` flips PENDING → TRIAL (wired `TenantDeployedEventConsumer` → `InstanceService.markProvisioned`, verified live 2026-06-07); kiteclass-core `FrontendInstance` carries the DEPLOYED state (FAILED→INITIALIZING→GENERATING→DEPLOYED verified live). Original "subscription Instance INITIALIZING → DEPLOYED" framing was incorrect — see §AC #3 reframe.

## Log

- **2026-06-07 (Wave p0-prov-1 closure):** Status PARTIAL → 🟢 DONE. Saga wiring live-confirmed end-to-end (publisher + `@RabbitListener` consumer + saga → kiteclass-core `FrontendInstance` DEPLOYED, no slug collision). AC#3 reframed: subscription-side `Instance.status` PENDING → TRIAL on `tenant.deployed` IS wired (`TenantDeployedEventConsumer` → `InstanceService.markProvisioned`, mirrors `activatePendingInstance` + sets trial timestamps) + executes live (a PENDING instance flips to TRIAL; a TRIAL test instance correctly no-ops — the earlier "stays TRIAL" observation was a TRIAL test instance, a correct no-op, not a gap). The provisioning DEPLOYED state lives on the kiteclass-core `FrontendInstance`, not the kitehub `Instance`. Real `provisionInfrastructure` (branded-frontend infra log-only stub) scope-split → GAP-1055 (Phase 1.5); not a Phase 1 BETA blocker (tenant DB provisioned by subscription-side `DatabaseProvisioningService`).

## Related

- Discovered in: 3-agent outside-in audit 2026-06-04 (persona / matrix consensus)
- Audit artifact: `documents/04-quality/audits/persona-review/2026-06-04-pre-walk-kc1-{tenant-provisioning,failure-mode-matrix}.md`
- Sister gaps: GAP-925 (consumer wire-format Wave flow-kh1) — sibling outbox dispatcher work
- Scope-split: GAP-1055 (Phase 1.5 — real `provisionInfrastructure` branded-frontend infra) + GAP-946 (defensive provisioning hardening)
- Flow Verification Campaign §4 row KC-1
