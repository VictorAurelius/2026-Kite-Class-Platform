---
title: Wave p0-1 — Phase 1 BETA P0 local-verifiable cluster (3 disjoint buckets)
status: draft
tag_primary: p0
tags_secondary: [provisioning-1, beta-readiness]
counter: 1
date_launch: 2026-06-07
created: 2026-06-07
updated: 2026-06-07
gaps: [GAP-882, GAP-946, GAP-948]
waves: [p0-1]
---

# Wave p0-1 — Phase 1 BETA P0 local-verifiable cluster

> Per user directive 2026-06-07 "P0 gap statistics should be prioritized over G2" ([[project-p0-priority-over-g2]]). Parallel Opus agents (per `wave-pack-planner` + `agent-model-opus-default.md`). Plan merge qua PR TRƯỚC khi spawn (per `feedback_wave_plan_through_pr.md`).

## 1. Brainstorm

**Scope:** 3 P0 Phase 1 BETA gaps có genuine implementation work + file-disjoint → an toàn parallel.

**Vì sao 3 gap này (không phải P0 khác):**
- Loại **GAP-975/976** (payment): state-check 2026-06-07 — V63/V64 + txnRef + SePay Apikey + IdempotencyService **đã code-complete** → coordinator verify-walk, không cần agent.
- Loại **GAP-952** (saga compensation): app-level + tests đã shipped; residual = CloudWatch live-apply (AWS-gated, stack STOPPED).
- Loại **GAP-885** (RLS coverage): migration-heavy đụng kiteclass-core Flyway → collide Bucket A → tách `wave-p0-2`.
- Loại **GAP-610/793/502/608/...**: near-done verify-walk hoặc AWS-gated.

**Risk/edge:** migration-version collision nếu 2 agent cùng thêm Flyway 1 service → mitigate: chỉ Bucket A thêm migration (kiteclass-core); B + C code-only (kitehub-subscription, file-disjoint).

**Outside-in trigger:** KHÔNG fire — bug-fix có root-cause cụ thể (per `outside-in-coverage-trigger.md` §4 exception). **Pre-walk persona sim:** KHÔNG bắt buộc — backend integrity fix, không phải user-facing flow walk mới (per `pre-walk-persona-simulation-mandate.md` §2 out-of-scope); verify = G3 runtime walk sau fix.

## 2. Task Breakdown

- Bucket A (GAP-882): canonical UPPERCASE enum + Flyway CHECK update + lowercase→UPPERCASE backfill (`invoices.status` + `invoice_items.item_type`) + IT round-trip.
- Bucket B (GAP-946): rethrow `DatabaseProvisioningException` / validate `databaseUrl != "pending"` → saga fail-loud + instance FAILED + test.
- Bucket C (GAP-948): wire existing `EmailServiceClient.sendTenantReadyEmail` vào outbox sau saga DEPLOYED + DLQ visibility + test.
- Coordinator post: octopus-merge → rebuild kiteclass-core + kitehub-subscription → CI → G3 walk → flip DONE.

## 3. Scope

3 disjoint buckets, parallel Opus agents:

| Bucket | Gap | Scope (files) | Migration | Owner | Walk class |
|---|---|---|---|---|---|
| **A** | GAP-882 (P0, 90%) | `kiteclass-core` invoice: `module/invoice/entity/Invoice.java` + `common/constant/InvoiceStatus.java` + new Flyway | ✅ kiteclass-core (sole Flyway writer) | Opus, worktree | backend integrity — pre-walk N/A |
| **B** | GAP-946 (P0, 40%) | `kitehub-subscription`: `service/DatabaseProvisioningService.java` + `service/InstanceService.java` | ❌ none | Opus, worktree | backend integrity — pre-walk N/A |
| **C** | GAP-948 (P0, 60%) | `kitehub-subscription`: `client/EmailServiceClient.java` (wire existing `sendTenantReadyEmail`) + outbox after DEPLOYED + DLQ | ❌ none | Opus, worktree | backend integrity — pre-walk N/A |

**Disjoint proof:** A = kiteclass-core (separate service). B vs C = same service `kitehub-subscription` NHƯNG file-disjoint (DatabaseProvisioningService/InstanceService vs EmailServiceClient/outbox-wire) → octopus-merge clean. Chỉ A thêm migration → zero Flyway version collision.

**Migration-version protocol (optimization):** ONLY Bucket A adds Flyway (kiteclass-core) → sole writer reads `ls .../db/migration/ | tail` at spawn (no pre-assignment). B + C add ZERO migrations.

## 4. State-Check Evidence

Per `audit-to-gap-pipeline.md` §2.6 — every code-symbol reference verified:

| Symbol | Bucket | Grep verdict |
|---|---|---|
| `InvoiceStatus` enum | A | ✅ `kiteclass-core/.../common/constant/InvoiceStatus.java` |
| `Invoice` entity (`invoices.status`) | A | ✅ `kiteclass-core/.../module/invoice/entity/Invoice.java` |
| invoice-status CHECK migration | A | 🆕 to-be-created (Bucket A owns; no existing invoice-status Flyway; V12 = create-extended only) |
| `DatabaseProvisioningService` | B | ✅ `kitehub-subscription/.../service/DatabaseProvisioningService.java` |
| `InstanceService` | B | ✅ `kitehub-subscription/.../service/InstanceService.java` |
| `EmailServiceClient.sendTenantReadyEmail` | C | ✅ exists `EmailServiceClient.java:534` (GAP-948 = WIRE not create) |
| `SubscriptionOutboxRepository` (outbox wire) | C | ✅ `kitehub-subscription/.../outbox/SubscriptionOutboxRepository.java` |

Drift caught + excluded: GAP-975/976 `V63/V64` already exist (V63/V64/V65 present) + txnRef/SePay/idempotency code-complete → not implementation work.

## 5. Verification Gates

- **G1 (per bucket):** `./mvnw -pl <module> test` PASS local (Testcontainers, strict-warnings); run tests not just compile per `api-contract-change-caller-sweep.md`.
- **Integration:** octopus-merge 3 branches → rebuild kiteclass-core + kitehub-subscription → `Test Core Service` + subscription CI green.
- **G3 runtime walk (coordinator, post-merge):** invoice SENT/REFUNDED persist via :9000; saga fail→FAILED; saga DEPLOYED→tenant-ready email in MailHog. Per `feature-ship-runtime-walk-mandate.md` §3 + `pre-handoff-self-test-completeness.md` §3 before DONE flip.

## 6. Agent Spawn Pattern

- 3 Opus worktree agents (per `agent-model-opus-default.md` Opus + isolation worktree + `agent-background-spawn-default.md` background).
- Spawn **serial OR small-batch** — 3-parallel hit server rate-limit 2026-06-07 first attempt; fall back to 1-at-a-time if rate-limited.
- Each agent STRICTLY within its bucket files (§3); no cross-bucket edits → octopus-merge clean.
- Commit on `fix/gap-<id>-<slug>` (no push); coordinator collects + octopus-merges.

## 7. Closure Protocol

- Octopus-merge → rebuild → CI green → G3 walk PASS → flip GAP-882/946/948 DONE + git mv `closed/` + CSV sync (per `gap-done-discipline.md` + `gap-folder-organization.md` + `post-merge-sync-completeness.md`).
- Run `bash scripts/prune-merged-worktrees.sh --yes` (per `post-wave-cleanup.md`).
- **Scope-Completeness Reconciliation** (per `wave-closure-scope-completeness.md` §3): table mapping Bucket A/B/C → ✅ DONE / 🟡 PARTIAL / ❌ at closure PR.
- Expected Phase 1 BETA P0: 29 → 26.
- **Out-of-wave (tracked):** `wave-p0-2` GAP-885 RLS (migration-heavy both services). Coordinator verify-walk (near-done): GAP-975/976/610. AWS-gated: GAP-793/502/608/533/567/566/572/117/756/648 + GAP-952 CloudWatch live-apply.

## 8. Log

- **2026-06-07:** Plan created (wave-pack optimized, 3 disjoint buckets). State-check (§4) caught GAP-975/976 code-complete + GAP-948 method-exists (wire-only) + GAP-885 migration-collision → excluded/tracked. First 3-parallel agent spawn attempt hit server rate-limit → §6 fallback serial documented. v2: section names aligned to `_TEMPLATE.md` §1-§8 (wave-plan-completeness CI).
