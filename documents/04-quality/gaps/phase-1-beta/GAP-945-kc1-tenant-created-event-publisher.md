# GAP-945: KC saga not wired — kitehub-subscription thiếu `tenant.created` publisher

**Status:** 🟡 PARTIAL
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

**STILL OPEN (PARTIAL — KC-1 flow completion, KHÔNG phải B–G blocker):**

- 🔴 **Live KC-1 full-stack walk** per `feature-ship-runtime-walk-mandate.md` §2 — beta signup → KC tenant provisioned end-to-end trên local Docker stack (subscription + core + rabbit + postgres). Round-trip IT mock saga → real saga execute-to-completion chưa walk-verified. Pre-walk persona simulation per `pre-walk-persona-simulation-mandate.md` required trước.
- 🔴 **AC #3 status transition** — subscription `Instance.status` INITIALIZING → DEPLOYED cần core→subscription callback (saga tạo core `FrontendInstance`; chưa có event flow ngược flip subscription `Instance.status`). Confirm/file trong live walk.
- 🔴 Real `provisionInfrastructure` (vẫn log-only stub) = coupled GAP-946 remaining.

## Live-walk re-confirmation (KC-1 closure walk — 2026-06-07)

KC-1 closure walk (GAP-953 retry path) re-confirmed the saga end-to-end on the shared stack: re-publish `tenant.created` → kiteclass-core `TenantCreatedEventConsumer` → `TenantProvisioningSaga.provision()` → FAILED→INITIALIZING→GENERATING→**DEPLOYED** (kiteclass-core `frontend_instances.status`), no slug collision. AC #1 (publisher) + AC #2 (`@RabbitListener` consumer) grep-verified present.

**AC #3 still PARTIAL (kept):** the *subscription-side* `Instance.status` did NOT transition — it stayed `TRIAL` throughout the walk (no kiteclass-core→subscription callback flips `instances.status`). The provisioning "DEPLOYED" state lives on the kiteclass-core `FrontendInstance`, not the kitehub `Instance`. Plus real `provisionInfrastructure` is still a stub (coupled GAP-946). So GAP-945 stays 🟡 PARTIAL pending the status-callback decision + real infra (GAP-946).

## Problem

`AuthService.registerFromBetaInvite:218` (kitehub-subscription) gọi `instanceService.createTrialInstance(...)` synchronously và KHÔNG enqueue `tenant.created` event (no outbox enqueue / no `rabbitTemplate.convertAndSend`). `TenantProvisioningSaga` trong kiteclass-core tồn tại như orphan code — không có `@RabbitListener(queues = "tenant.created.queue")` consumer. Hệ quả: KC tenant DB không được tạo, `Instance.status` stuck `INITIALIZING` mãi mãi, Owner login vào `kc-<slug>.kitehub.me/admin` → 404 hoặc spinner forever. Surfaced cross-audit: persona simulation Finding 1.2 + failure-mode matrix A1×E5×EC2 + A1×E8×EC2.

## Proposed Fix

Thêm outbox enqueue `tenant.created` trong `AuthService.registerFromBetaInvite` sau commit tx; wire `@RabbitListener` consumer trong kiteclass-core invoking `TenantProvisioningSaga.handle(event)`.

## Acceptance Criteria

- [ ] `grep -rn "tenant.created\|TenantCreatedEvent" kitehub/kitehub-subscription/src/main/java` ≥1 publisher hit
- [ ] `grep -rn "@RabbitListener.*tenant" kiteclass/kiteclass-core/src/main/java` ≥1 consumer hit
- [ ] Post-beta-signup walk: `Instance.status` transitions INITIALIZING → DEPLOYED trong <30s

## Related

- Discovered in: 3-agent outside-in audit 2026-06-04 (persona / matrix consensus)
- Audit artifact: `documents/04-quality/audits/persona-review/2026-06-04-pre-walk-kc1-{tenant-provisioning,failure-mode-matrix}.md`
- Sister gaps: GAP-925 (consumer wire-format Wave flow-kh1) — sibling outbox dispatcher work
- Flow Verification Campaign §4 row KC-1
