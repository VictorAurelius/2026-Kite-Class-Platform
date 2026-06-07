---
title: Wave p0-1 — Phase 1 BETA P0 local-verifiable cluster (3 disjoint buckets)
status: draft
tag_primary: p0
tags_secondary: [provisioning-1, beta-readiness]
created: 2026-06-07
updated: 2026-06-07
gaps: [GAP-882, GAP-946, GAP-948]
waves: [p0-1]
---

# Wave p0-1 — Phase 1 BETA P0 local-verifiable cluster

> Per user directive 2026-06-07 "P0 gap statistics should be prioritized over G2" ([[project-p0-priority-over-g2]]). Wave này attack cluster P0 local-verifiable có **genuine implementation work** + **disjoint files** → parallel Opus agents (per `wave-pack-planner` + `agent-model-opus-default.md`). Plan merge qua PR TRƯỚC khi spawn (per `feedback_wave_plan_through_pr.md`).

## 1. Brainstorm (Superpowers — quick)

**Scope:** 3 P0 Phase 1 BETA gaps có remaining-implementation work + file-disjoint → an toàn parallel.

**Vì sao 3 gap này (không phải gap P0 khác):**
- Loại **GAP-975/976** (payment): state-check 2026-06-07 cho thấy V63/V64 + txnRef + SePay Apikey + IdempotencyService **đã code-complete** → coordinator verify-walk, không cần agent.
- Loại **GAP-952** (saga compensation): app-level + tests đã shipped; residual = CloudWatch live-apply (AWS-gated, stack STOPPED).
- Loại **GAP-885** (RLS coverage): migration-heavy đụng kiteclass-core Flyway → **collide** với Bucket A → tách `wave-p0-2` riêng.
- Loại **GAP-610/793/502/608/533/...**: hoặc near-done verify-walk hoặc AWS-gated.

**Risk/edge:** migration-version collision nếu 2 agent cùng thêm Flyway 1 service → mitigate: **chỉ Bucket A thêm migration** (kiteclass-core); B + C code-only (kitehub-subscription, file-disjoint).

**Outside-in trigger:** KHÔNG fire — đây là bug-fix có root-cause cụ thể (per `outside-in-coverage-trigger.md` §4 exception "Gap fix cụ thể đã có root cause").

**Pre-walk persona sim:** KHÔNG bắt buộc — không phải user-facing flow walk mới; saga/invoice là backend integrity fix (per `pre-walk-persona-simulation-mandate.md` §2 out-of-scope). Verify = G3 runtime walk sau fix.

## 2. Buckets (3 disjoint, parallel Opus agents)

| Bucket | Gap | Scope (files) | Migration | Agent |
|---|---|---|---|---|
| **A** | GAP-882 (P0, 90%) | `kiteclass-core` invoice: `module/invoice/entity/Invoice.java` + `common/constant/InvoiceStatus.java` + new Flyway | ✅ kiteclass-core (owns Flyway this wave) | Opus, worktree |
| **B** | GAP-946 (P0, 40%) | `kitehub-subscription`: `service/DatabaseProvisioningService.java` + `service/InstanceService.java` | ❌ none | Opus, worktree |
| **C** | GAP-948 (P0, 60%) | `kitehub-subscription`: `client/EmailServiceClient.java` (wire existing `sendTenantReadyEmail`) + outbox after DEPLOYED + DLQ visibility | ❌ none | Opus, worktree |

**Disjoint proof:** A = kiteclass-core (separate service). B vs C = same service `kitehub-subscription` NHƯNG file-disjoint (DatabaseProvisioningService/InstanceService vs EmailServiceClient/outbox-wire) → octopus-merge clean. Chỉ A thêm migration → zero kiteclass-core/kitehub Flyway version collision.

### Bucket A — GAP-882 invoice status enum↔CHECK drift
- Canonical enum UPPERCASE per `design-patterns.md` §3.12. New Flyway migration: update CHECK constraint cho `invoices.status` + `invoice_items.item_type` + backfill lowercase→UPPERCASE. Per §3.12 entity↔migration↔mapper triad atomic.
- **AC:** persist SENT/REFUNDED không 500; CHECK accepts canonical set; existing rows backfilled; IT round-trip.

### Bucket B — GAP-946 stub-mode silent DB exception swallow
- `DatabaseProvisioningService`: rethrow `DatabaseProvisioningException` (stop saga, mark instance FAILED) thay vì silent swallow; validate `databaseUrl != "pending"` post-provision. OR wire `database.lifecycle.enabled` flag explicit.
- **AC:** provision fail → saga stops + instance FAILED (không silent DEPLOYED-with-pending-url); IT.

### Bucket C — GAP-948 tenant-ready email wire
- `sendTenantReadyEmail` đã tồn tại (EmailServiceClient:534) → wire vào outbox sau saga `DEPLOYED` (Vietnamese template + Resend) + DLQ visibility cho 3-retry exhausted.
- **AC:** saga DEPLOYED → tenant-ready email enqueued (MailHog verify local); DLQ visible khi exhausted; IT.

## 3. State-Check Evidence (per `audit-to-gap-pipeline.md` §2.6)

| Symbol | Bucket | Grep verdict |
|---|---|---|
| `InvoiceStatus` enum | A | ✅ `kiteclass-core/.../common/constant/InvoiceStatus.java` |
| `Invoice` entity (`invoices.status`) | A | ✅ `kiteclass-core/.../module/invoice/entity/Invoice.java` |
| invoice-status CHECK migration | A | 🆕 to-be-created (Bucket A owns; no existing invoice-status Flyway; V12 = create-extended only) |
| `DatabaseProvisioningService` | B | ✅ `kitehub-subscription/.../service/DatabaseProvisioningService.java` |
| `InstanceService` | B | ✅ `kitehub-subscription/.../service/InstanceService.java` |
| `EmailServiceClient.sendTenantReadyEmail` | C | ✅ exists `EmailServiceClient.java:534` (GAP-948 = WIRE not create) |
| `SubscriptionOutboxRepository` (outbox wire) | C | ✅ `kitehub-subscription/.../outbox/SubscriptionOutboxRepository.java` |

Drift caught by state-check (excluded from wave): GAP-975/976 `V63/V64` already exist (V63/V64/V65 present) + txnRef/SePay/idempotency code-complete → not implementation work.

## 4. Migration-version protocol (optimization — collision avoidance)
- ONLY Bucket A adds Flyway, in `kiteclass-core` → next free `V[N]` (agent reads `ls .../db/migration/ | tail` at spawn, no pre-assignment needed since sole writer).
- B + C add ZERO migrations → no kitehub-subscription Flyway contention.

## 5. Verification gates
- **G1 (per bucket):** `./mvnw -pl <module> test` PASS local (Testcontainers, strict-warnings); per `api-contract-change-caller-sweep.md` run tests not just compile.
- **Integration:** octopus-merge 3 branches → rebuild kiteclass-core + kitehub-subscription → full `Test Core Service` + subscription CI green.
- **G3 runtime walk (coordinator, post-merge):** invoice SENT/REFUNDED persist via :9000; saga fail→FAILED; saga DEPLOYED→tenant-ready email in MailHog. Per `feature-ship-runtime-walk-mandate.md` §3 + `pre-handoff-self-test-completeness.md` §3 before DONE flip.
- **Post-wave:** flip GAP-882/946/948 DONE + git mv closed/ + CSV sync (per `gap-done-discipline.md` + `gap-folder-organization.md` + `post-merge-sync-completeness.md`).

## 6. Execution order
P0 (this plan PR merge) → spawn 3 Opus worktree agents parallel → collect + octopus-merge → rebuild + CI → G3 walk → flip DONE. Expected Phase 1 BETA P0: 29 → 26.

## 7. Out-of-wave (tracked)
- `wave-p0-2`: GAP-885 RLS coverage (migration-heavy both services).
- Coordinator verify-walk (near-done, no agent): GAP-975, GAP-976, GAP-610 (95%), GAP-882... ; GAP-952 (AWS-gated CloudWatch live-apply).
- AWS-gated bucket (needs `bash scripts/aws/start-stack.sh`): GAP-793/502/608/533/567/566/572/117/756/648.
