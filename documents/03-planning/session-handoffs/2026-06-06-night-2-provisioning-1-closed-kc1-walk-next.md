# Session handoff — 2026-06-06 night-2

**Scope:** Wave provisioning-1 (tenant-provisioning KC saga, 8 P0) — ALL 8 buckets code-merged + closure shipped. NEXT = KC-1 live full-stack walk to flip 8 gaps PARTIAL → DONE.

## Shipped this session (9 PRs merged, 0 open, main = 90a43c23)

Executed via dependency-ordered DAG + parallel Opus worktree agents (~6.5x vs serial):

| PR | Bucket | Gap | Status |
|----|--------|-----|--------|
| #2214 | A keystone — `tenant.created` publisher + `TenantCreatedEventConsumer` wiring orphan `TenantProvisioningSaga` + RabbitConfig binding + `@Jacksonized` + Testcontainers round-trip IT | GAP-945 | 🟡 70% |
| #2211 | A 1a — fail-fast DB provisioning (prior session) | GAP-946 | 🟡 40% |
| #2216 | B — `TENANT_PROVISIONED` audit (`TenantAuditService` REQUIRES_NEW) | GAP-949 | 🟡 75% |
| #2219 | C — tenant-ready email (core consumer → `tenant.deployed` → subscription `TenantDeployedEventConsumer` resolves owner → `sendTenantReadyEmail`) | GAP-948 | 🟡 60% |
| #2217 | D — compensation alert + `ProvisioningStuckSweep` @Scheduled + CloudWatch metric-filter alarm→SNS (IaC) | GAP-952 | 🟡 60% |
| #2218 | E — admin force-retry `POST /api/platform/admin/instances/{id}/retry-provisioning` + PLATFORM_ADMIN guard + audit + gateway route + FE | GAP-953 | 🟡 80% |
| #2213 | F — `TenantSettings` entity + V90 + GET/PUT + Năm học auto-fill + 3-layer docs | GAP-947 | 🟡 90% |
| #2215 | G — PDPL Art 23 DELETE cascade: FSM SUSPENDED/DELETED + V91 + MinIO/DNS/logo cascade + `TENANT_DELETED` audit | GAP-954 | 🟡 90% |
| #2220 | closure — reconciliation §7.1 + wave-history + ROADMAP + CSV sync | — | — |

Merge order A→F→B→G→D→C→E→closure. Manual resolves: V90↔V91 migration collision; `TenantAuditService` 3-method consolidation (recordTenantProvisioned + recordTenantDeleted + recordTenantRetryRequested in one class, across G+E rebases); gap-status.csv 3-way. Rebased G+E local-verified before force-push per `admin-merge-discipline.md` §2.

## NEXT SESSION — KC-1 live full-stack walk (flips all 8 gaps → DONE)

All 8 gaps are 🟡 PARTIAL: code shipped + unit/IT-tested + CI-gated, but each defers live verification per `feature-ship-runtime-walk-mandate.md` §1. The **single cross-cutting gate** is one KC-1 walk.

### Prerequisites (DO FIRST)
1. **Rebuild stale services** — running `kitehub-subscription` + `kiteclass-core` containers are ~6h old (pre-merge); they do NOT contain today's saga code. Per `pre-walk-static-audit-bundle.md`:
   ```
   bash kitehub/scripts/rebuild.sh kitehub-subscription
   bash kitehub/scripts/rebuild.sh kiteclass-core
   bash kitehub/scripts/rebuild.sh kitehub-email   # C added TenantDeployedEventConsumer
   ```
2. **Read pre-walk persona artifact** — `documents/04-quality/audits/persona-review/2026-06-06-pre-walk-kc1-provisioning.md` (spawned this session per `pre-walk-persona-simulation-mandate.md`; 8-12 failure modes + HIGH-confidence batch-fix list). Batch-fix HIGH-confidence findings BEFORE walking.

### Walk path (persona: Owner from beta invite, then PLATFORM_ADMIN)
beta signup → `tenant.created` → saga provisions KC `FrontendInstance` → `tenant.deployed` → tenant-ready email (check MailHog :8025) → owner login → TenantSettings GET/PUT (Năm học/timezone) → admin retry on a forced-FAILED tenant → admin delete (PDPL cascade: DB + MinIO + DNS + `TENANT_DELETED` audit). Catalog-then-batch per `feature-ship-runtime-walk-mandate.md` §3.4 (don't rebuild per-bug mid-walk).

### Per-gap follow-ups to confirm/fix during walk (from reconciliation §7.1)
- **GAP-945:** subscription `Instance.status` INITIALIZING→DEPLOYED needs a core→subscription callback — saga creates a core `FrontendInstance` but no event flows back to flip subscription `Instance.status`. Likely a real gap to wire (new `tenant.deployed` consumer on subscription side already exists for email — could also flip status).
- **GAP-949:** audit `admin_user_id` NOT NULL + FK to `users(id)` written REQUIRES_NEW while owner uncommitted (parent txn) → under READ COMMITTED FK may fail → audit row silently dropped. If walk confirms no row, move call to `@TransactionalEventListener(AFTER_COMMIT)` (allowed by `audit-service-isolation.md` §3 event-boundary).
- **GAP-953:** admin retry RE-PUBLISHES `tenant.created` → confirm `saga.provision`/`lifecycle.initiate` idempotent on re-provision (no duplicate FrontendInstance / no throw).
- **GAP-952:** CloudWatch live-apply + fault-injection blocked on AWS restore (GAP-612); @Scheduled stuck-sweep should run locally — verify.
- **GAP-947:** jsonb `themeConfig` Testcontainers IT (per `postgres-specific-type-testcontainers.md`) deferred — add at walk.

### Closure after walk
Flip the 8 gaps DONE in gap-status.csv + git mv to `phase-1-beta/closed/` per `gap-folder-organization.md` §3.3; flip wave plan frontmatter `status: complete`; update wave-history + ROADMAP; KC-1 row in Flow Verification Campaign §4 → ✅ (G1 portion).

## Stack state (2026-06-06 night-2)
All 13 containers UP healthy (subscription/core 6h old → rebuild; rest 37h). AWS stack STOPPED (cost-save). Local Docker has full stack incl rabbitmq + postgres + minio + mailhog + redis.

## Context note
Session reached 71% context (Opus 4.8 1M) after the 8-bucket parallel wave + closure → walk deferred to fresh session per `session-end-context-check.md` §3 (heavy multi-round debugging phase needs full context room).
