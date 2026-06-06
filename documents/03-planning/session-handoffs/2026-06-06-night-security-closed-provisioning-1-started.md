# Session handoff — 2026-06-06 night

**Scope:** Closed P0 security cluster (Bucket B IDOR) → started tenant-provisioning epic (wave provisioning-1).

## Shipped this session (all merged, 0 open PRs)

| PR | What | Result |
|----|------|--------|
| #2208 | session-handoff doc (evening) | merged |
| #2207 | security-2 Bucket C — GAP-1025/1035 @PreAuthorize authz | merged |
| #2209 | security-2 Bucket B — GAP-1015/1019/1023 cross-tenant IDOR ownership binding | merged → **7/7 P0 security cluster CLOSED** |
| #2210 | provisioning-1 **wave plan** (8-gap KC saga epic, dependency-ordered + §2.5 rescope) | merged |
| #2211 | provisioning-1 Bucket A Phase 1a — GAP-946 fail-fast DB provisioning (kill 3 silent-swallow sites) | merged → GAP-946 PARTIAL 40% |

**Gaps:** GAP-1015/1019/1023 DONE · GAP-946 PARTIAL · GAP-1044 filed (stale `*IT` not CI-run).

## Key technical context for pickup

- **Security IDOR fix pattern (reusable):** `TenantOwnershipGuard` (per service) — admin bypass via SecurityContext authority, throws `AccessDeniedException`→403. Gateway `TenantHeaderGuardFilter` ALREADY injects trusted `X-Tenant-Id` (no gateway change needed for tenant binding).
- **GAP-925 publish pattern:** when publishing JSON to a queue consumed by `@RabbitListener(String)`, build AMQP `Message` with raw UTF-8 bytes + `CONTENT_TYPE_JSON` (avoid Jackson double-encode). See `SubscriptionEventEmitter:99-111`.
- **`*IT` not run in CI:** no maven-failsafe plugin → CI `mvn test` (surefire default) excludes `*IT.java`. Regression guards must be `*Test.java`. (GAP-1044 tracks this.)

## NEXT SESSION — GAP-945 saga foundation (keystone)

Plan: `documents/03-planning/waves/wave-2026-06-06-provisioning-1-tenant-saga.md`.

**Bucket A remaining (GAP-945):** wire the orphan `TenantProvisioningSaga` (kiteclass-core) — it has NO `@RabbitListener` so KC tenants never get created (Instance stuck INITIALIZING).
1. **Publisher (kitehub-subscription):** `AuthService.registerFromBetaInvite` (~:261, after `createTrialInstance`) → emit `TenantCreatedEvent` JSON via `SubscriptionEventEmitter.emit(instanceId, "TENANT_CREATED", "tenant.created", payloadJson)` (outbox reliability + fast-path → `email.exchange` topic). Map: tenantId=instance id, slug=instance.slug, audience/tone=default.
2. **Consumer (kiteclass-core):** declare `tenant.created.queue` bound to `email.exchange` routing `tenant.created` in `RabbitConfig`; new `@RabbitListener(queues="tenant.created.queue")` consumer → deserialize `TenantCreatedEvent` → `saga.provision(event)`. Mirror `ClassRescheduledEmailConsumer` pattern.
3. **Verify:** Testcontainers RabbitMQ publish→consume round-trip; ideally live KC-1 walk on local Docker stack (pre-walk persona simulation per `pre-walk-persona-simulation-mandate.md`).
4. Also: GAP-946 remaining — `TenantProvisioningSaga.provisionInfrastructure:83-86` real (not stub) + async FAILED/retry.

**Then Phase 2/3 = WAVE-PACK PARALLEL opportunity (4-5 Opus agents):** Bucket B GAP-949 audit (ship first, shared dep) → C GAP-948 email · D GAP-952 compensation alert · E GAP-953 admin retry · F GAP-947 TenantSettings entity · G GAP-954 DELETE cascade. (B-G largely disjoint once saga contract frozen.)

## Other P0 backlog (phase-1-beta)
~13 P0 OPEN + ~19 PARTIAL remain. Notable: GAP-942 (SUB-20 started_at/expires_at NOT NULL blocks PENDING — latent), GAP-1043 (reschedule past-date), GAP-1044 (stale *IT CI strategy).
