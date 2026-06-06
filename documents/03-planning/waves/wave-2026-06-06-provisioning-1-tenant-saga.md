---
title: Wave provisioning-1 — tenant-provisioning KC saga cluster (8 P0)
status: in-progress
created: 2026-06-06
updated: 2026-06-06
waves: [provisioning-1]
wave: 1
tag_primary: provisioning
tags_secondary: [saga, tenant, kc-1, pdpl, audit, email, rabbitmq]
counter: 1
date_launch: 2026-06-06
gaps: [GAP-945, GAP-946, GAP-947, GAP-948, GAP-949, GAP-952, GAP-953, GAP-954]
---

# Wave provisioning-1 — tenant-provisioning KC saga cluster

**Mục tiêu:** Đóng 8 P0 từ KC-1 tenant-provisioning pre-walk audit (2026-06-04). Cluster lớn, net-new + partial, span 3 service (kitehub-subscription + kiteclass-core + kitehub-email). **Multi-phase / multi-session epic** — plan PR-first per `feedback_wave_plan_through_pr`.

## 1. Brainstorm

KC-1 (Owner đăng ký từ beta invite → tenant KiteClass được provision) hiện **ĐỨT**: `AuthService.registerFromBetaInvite` gọi `createTrialInstance` đồng bộ, KHÔNG publish `tenant.created`; `TenantProvisioningSaga` (kiteclass-core) là **orphan code** không có `@RabbitListener` → KC tenant không bao giờ tạo, Instance kẹt INITIALIZING. Đây là keystone — không fix thì 7 gap còn lại vô nghĩa.

**Fix-time state-check (per `audit-to-gap-pipeline.md` §2.5)** — Explore agent map code thực tế. 4 gap **over-claim "missing entirely"** (rescope PARTIAL):

| Gap | Gap claim | Code thực tế | Rescope |
|-----|-----------|--------------|---------|
| GAP-948 | "EmailServiceClient chỉ có 2 method" | ~20 send methods (sendWelcomeEmail:479 + provider routing Resend/SES đủ) | **PARTIAL** — chỉ thêm 1 method + wire saga DEPLOYED |
| GAP-949 | "0 audit infra" | AdminAuditLog + AdminAuditAspect + ITs đủ | **PARTIAL** — provisioning chưa write vào |
| GAP-953 | "no retry" | `lifecycle.retry()` + internal endpoint `/api/v1/instances/{id}/retry` đã có | **PARTIAL** — thiếu admin endpoint + guard + audit + FE |
| GAP-954 | "cascade incomplete" | InstancePurgeService (DB drop + S3 backup + 30d retention) + SUSPENDED/DELETED/PURGED FSM đã có | **PARTIAL** — thiếu MinIO/DNS/logo cascade + kiteclass FrontendInstance delete states + TENANT_DELETED audit |

GAP-946 cũng partial: `database.lifecycle.enabled=true` đã set ở `application-production.yml:57` — real bug là silent-swallow **3 sites** (`InstanceService:170-176/250-267/329-333`, gap chỉ nêu 1 → cross-flow sweep per `cross-flow-bug-class-sweep.md`).

## 2. Task Breakdown — dependency-ordered buckets

```
FOUNDATION (Phase 1, this session — keystone, must ship first):
  Bucket A — GAP-945 + GAP-946 (coupled, same saga/InstanceService files)
    - Freeze tenant.created event contract (TenantCreatedEvent DTO + routing key + queue)
    - Publisher: registerFromBetaInvite → enqueue via SubscriptionOutboxDispatcher (reuse outbox infra)
    - Consumer: @RabbitListener in kiteclass-core wiring orphan TenantProvisioningSaga
    - GAP-946: remove 3 silent-swallow sites → rethrow → Instance FAILED; verify databaseUrl != 'pending'

PARALLEL (Phase 2 — after foundation contract frozen):
  Bucket B — GAP-949 provisioning audit (AdminAuditLog REQUIRES_NEW; shared dep for C/D)
  Bucket C — GAP-948 tenant-ready email (new EmailServiceClient method + saga DEPLOYED hook + DLQ)
  Bucket D — GAP-952 compensation alert + @Scheduled stuck-sweep + CloudWatch SNS
  Bucket E — GAP-953 admin force-retry endpoint + PLATFORM_ADMIN guard + audit + FE button

INDEPENDENT (Phase 3 — no saga-contract dep, any order):
  Bucket F — GAP-947 TenantSettings entity + table + controller + 3-layer docs + Năm học auto-fill
  Bucket G — GAP-954 DELETE cascade: kiteclass FrontendInstance SUSPENDED/DELETED states +
             MinIO/DNS/logo cascade + TENANT_DELETED audit + PDPL Art 23 retention doc
```

## 3. Scope (per bucket — symbols verified by state-check)

- **Bucket A (Foundation):** `kitehub-subscription .../service/AuthService.java:229-261` (publish), `SubscriptionOutboxDispatcher`/`SubscriptionEventEmitter` (reuse), NEW `TenantCreatedEvent` contract; `kiteclass-core .../module/provisioning/TenantProvisioningSaga.java:49-109` (wire @RabbitListener + real provisionInfrastructure:83-86); `InstanceService.java:170-176/250-267/329-333` (kill 3 swallow → FAILED).
- **Bucket B:** `AuthService.registerFromBetaInvite` + `TenantProvisioningSaga` steps → AdminAuditLog `TENANT_PROVISIONED` (REQUIRES_NEW per `audit-service-isolation.md`).
- **Bucket C:** `EmailServiceClient` new `sendTenantReadyEmail` + VN template + saga DEPLOYED hook + 3-retry DLQ alert.
- **Bucket D:** `TenantProvisioningSaga.compensate():102-109` + CloudWatch metric `tenant_provisioning_compensation_failed` + SNS alarm + `@Scheduled provisioning-stuck-sweep`.
- **Bucket E:** NEW `POST /api/v1/admin/tenants/{id}/retry-provisioning` (kitehub-subscription admin) + PLATFORM_ADMIN guard + audit + FE `/admin/tenants/{id}` button (Mixed — admin tenant page net-new).
- **Bucket F:** NEW `TenantSettings` entity + `V<N>__create_tenant_settings.sql` + repo/service/controller + DTO + `GET/PUT /api/v1/tenants/{id}/settings` + 3-layer business docs + Năm học auto-compute (Sep–May VN K-12).
- **Bucket G:** `kiteclass-core FrontendInstanceStatus` + SUSPENDED/DELETED states; MinIO bucket + DNS record + S3 logo cascade (extend `InstancePurgeService`); `TENANT_DELETED` audit; PDPL Art 23 retention doc + test.

## 4. State-Check Evidence (per `audit-to-gap-pipeline.md` §2.6)

| Symbol | Verify | Verdict |
|--------|--------|---------|
| `TenantProvisioningSaga` (orphan, no listener) | core provisioning pkg:49-109 | ✅ exists, unreachable |
| `tenant.created` publisher in subscription | grep → 0 (only JPA index false-pos) | 🆕 to-be-created (Bucket A) |
| `@RabbitListener.*tenant` in core | grep → 0 (only class.rescheduled) | 🆕 to-be-created (Bucket A) |
| `SubscriptionOutboxDispatcher`/`EventEmitter` (reuse for publish) | subscription | ✅ exists |
| `InstanceService` 3 swallow sites | :170-176/250-267/329-333 | ✅ confirmed (gap named 1) |
| `database.lifecycle.enabled` prod | application-production.yml:57 | ✅ already true |
| `TenantSettings` entity | grep → 0 | 🆕 to-be-created (Bucket F) |
| `EmailServiceClient` send infra | sendWelcomeEmail:479 + ~20 methods | ✅ exists (Bucket C small) |
| `AdminAuditLog` + aspect | subscription audit pkg | ✅ exists (Bucket B wire) |
| `lifecycle.retry()` + internal endpoint | InstanceLifecycleService:129 + InstanceController:109 | ✅ exists (Bucket E admin delta) |
| `InstancePurgeService` cascade + SUSPENDED/DELETED/PURGED | InstanceStatus enum + InstancePurgeService:93-177 | ✅ exists (Bucket G delta = MinIO/DNS/logo/kiteclass-FSM/audit) |

Gaps filed 2026-06-04 (KC-1 pre-walk audit). 4 gaps rescoped PARTIAL per §1 table (gap files updated when bucket executes).

## 5. Verification Gates

Per bucket: TDD + `./mvnw test` (run not compile per `api-contract-change-caller-sweep.md`). Cross-service saga (Bucket A) needs RabbitMQ → Testcontainers IT for publish→consume round-trip. Live walk per `feature-ship-runtime-walk-mandate.md` §2 (KC-1 = user-facing provisioning flow) when local Docker stack up; pre-walk persona simulation per `pre-walk-persona-simulation-mandate.md` before any walk.

## 6. Agent Spawn Pattern

- **Foundation (Bucket A):** solo coordinator — cross-service saga contract needs single-author consistency (freeze event DTO + routing key first; the highest-risk integration point).
- **Phase 2/3 (Buckets B-G):** wave-eligible (≥3 disjoint once contract frozen) → spawn Opus parallel agents per `agent-model-opus-default.md` + `agent-background-spawn-default.md`, worktree-isolated. Bucket B (audit) ships before C/D/E (shared audit dep).

## 7. Closure Protocol

Per `wave-closure-scope-completeness.md` §3 — reconciliation table at closure mapping all 8 gaps ✅/🟡/❌. Sync gap-status.csv + wave-history + ROADMAP. This is a multi-phase wave; each phase's PR is a checkpoint, full closure when all 8 buckets reconcile.

## 8. Log

- **2026-06-06:** Wave plan created (PR-first). Explore agent mapped 8-gap cluster + state-check corrected 4 over-claiming gaps (948/949/953/954 → PARTIAL; infra exists). Dependency: Foundation A (GAP-945 saga contract + 946 fail-fast) keystone → Phase 2 parallel (B/C/D/E) → Phase 3 independent (F/G). Foundation impl starts this session.
