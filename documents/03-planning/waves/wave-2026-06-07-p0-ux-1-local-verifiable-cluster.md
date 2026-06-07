---
title: Wave p0-ux-1 — local-verifiable UX cluster (batch invoice + onboarding sample-data + mobile-first admin)
wave: 1
tag_primary: p0-ux
tags_secondary: [flow-campaign, p0, beta-prep, frontend]
counter: 1
date_launch: 2026-06-07
created: 2026-06-07
updated: 2026-06-07
waves: [p0-ux-1]
status: complete
gaps: [GAP-297, GAP-950, GAP-951]
---

# Wave p0-ux-1 — local-verifiable UX cluster

**Goal:** Đóng 3 P0 UX gap fully live-verifiable trên local stack (GAP-297 batch invoice email-live + GAP-950 onboarding sample-data + GAP-951 mobile-first admin). Defer vendor-gated GAP-063/286 (Zalo/SMS cần Zalo OA Business account + SMS provider contract = real-user action). Phase 1 BETA P0 23 → 20.

## 1. Brainstorm

**Trigger:** User chọn UX/feature cluster (option "1") 2026-06-07; AskUserQuestion chốt scope "Local-verifiable trọn vẹn" (3 gap walk được hết local; defer vendor-gated 063/286).

**Q1 alignment:** UX/feature cluster phục vụ non-tech Owner persona (onboarding wizard, mobile admin, batch invoice) — Wave 11 outside-in persona audit (2026-06-02) + persona 2026-05-14/15 (≤30 ngày, cover per `outside-in-coverage-trigger.md` §4).

**Vendor-gating insight:** GAP-063/286 live Zalo/SMS = real-user action (vendor account) per `feedback_real_user_action_not_a_gap.md` — defer Phase 1.5. GAP-297 email path KHÔNG cần GAP-063 (email adapter ship Wave 18a, live OK).

**State-check discoveries (per `audit-to-gap-pipeline.md` §2.8):**
- **GAP-950 scope-revise:** `OnboardingWizard.tsx` (5-step) ĐÃ tồn tại + wired trong `app/dashboard/page.tsx:132` (localStorage progress). Gap premise "find onboarding likely empty" = stale. AC#1 (wizard renders) ✅ đã có. Remaining: AC#3 sample-data import (greenfield) + AC#2 DB-progress (cân nhắc — localStorage đủ?) + AC#4 mobile (= GAP-951).
- **GAP-297:** no existing batch endpoint (grep trống) → greenfield BE.
- `ZaloOaNotificationService.java` đã tồn tại (GAP-063 infra một phần — out of scope wave này).

## 2. Task Breakdown

**Phase 1 — parallel (2 Opus agent, module rời nhau):**
- Bucket A (kiteclass-core BE): GAP-297 BE — batch-generate/confirm + idempotency + pro-rata + outbox InvoiceCreated + IT.
- Bucket B (kiteclass-frontend): GAP-951 responsive — collapsible sidebar <768px + responsive pages + Playwright mobile spec.

**Phase 2 — after Phase 1 merge (FE features on responsive+BE foundation):**
- Bucket C: GAP-950 sample-data import (kiteclass-core endpoint + FE button) + wizard scope-revise verify.
- Bucket D: GAP-297 FE — batch button + preview drawer + email dispatch, consume Bucket A.

**Phase 3 — coordinator walks (per `feature-ship-runtime-walk-mandate.md`):** GAP-297 (batch → email MailHog), GAP-950 (sample-data → dashboard populated), GAP-951 (mobile viewport) → flip DONE.

## 3. Scope

| Bucket | Gap | Module | Walk class |
|---|---|---|---|
| A | GAP-297 BE | `kiteclass-core/module/invoice` | greenfield BE — IT-verifiable |
| B | GAP-951 | `kiteclass-frontend` layout+pages | user-facing — mobile walk |
| C | GAP-950 | `kiteclass-core` + `kiteclass-frontend` | user-facing — wizard+sample-data walk |
| D | GAP-297 FE | `kiteclass-frontend` billing | user-facing — batch walk |

**Scope boundary:** Defer GAP-063/286 (vendor-gated Zalo/SMS, Phase 1.5). GAP-950 AC#2 DB-progress = decide in Bucket C (localStorage may suffice Phase 1 BETA). Zalo/SMS dispatch trong GAP-297 = wire abstraction only (email is live channel).

## 4. State-Check Evidence

| Target | Check | Result |
|---|---|---|
| GAP-950 wizard exists? | `grep OnboardingWizard app/dashboard/page.tsx` | ✅ wired line 132 (localStorage) — scope-revise |
| GAP-297 batch endpoint? | `grep batch-generate kiteclass-core` | 🆕 none — greenfield |
| invoice module | `find module/invoice` | ✅ controller/service/entity/event/scheduler exist |
| Enrollment entity | `find Enrollment*.java` | ✅ entity + repository exist |
| Playwright config | `find playwright.config.ts` | ✅ exists (GAP-951 extends) |
| sample-data endpoint | `grep sampleData kiteclass-core` | 🆕 none — greenfield (Bucket C) |

## 5. Verification Gates

Per bucket (Phase 3 walks):
- **GAP-297:** IT 60 enrollments → batch-generate 60 preview + batch-confirm 60 rows + 60 outbox events + idempotency (re-run no dup) + email dispatch MailHog.
- **GAP-950:** sample-data import → dashboard shows 1 class + 3 students + 1 teacher; wizard renders 5 steps.
- **GAP-951:** Playwright `mobile-admin.spec.ts` PASS 375×667; sidebar collapsed <768px; breakpoint grep ≥50.

## 6. Agent Spawn Pattern

- Phase 1: 2 Opus agent (`agent-model-opus-default.md`) background (`agent-background-spawn-default.md`) worktree-isolated, disjoint modules (kiteclass-core BE vs kiteclass-frontend responsive).
- Phase 2: FE features after Phase 1 merge (avoid FE-convergent collision per Wave g2-blockers lesson).
- Phase 3: coordinator-driven walks (shared stack, sequential).

## 7. Closure Protocol

- 3 gap → DONE + git-mv `phase-1-beta/closed/` per `gap-folder-organization.md` (after Phase 3 walk-verify).
- GAP-950 scope-revise documented (wizard pre-existing; sample-data = real deliverable).
- gap-status.csv 3 rows DONE per `meta-csv-index-pattern.md`.
- flow-verification-campaign §4 KC-1 row (GAP-950/951) sync.
- Defer note: GAP-063/286 Phase 1.5 vendor-gated; GAP-297 Zalo/SMS dispatch Phase 1.5.

## 9. Scope-Completeness Reconciliation

Per `wave-closure-scope-completeness.md` §3 — mọi item trong §3 Scope categorize tại closure.

| # | Plan §3 Scope item | Verdict | Follow-up |
|---|---|---|---|
| 1 | Bucket A — GAP-297 BE batch-generate/confirm + idempotency + pro-rata + outbox InvoiceCreated + IT | ✅ DONE | — (V93 migration + BatchInvoiceGenerationIT 4/4; live walk gateway prorated 1.3M + idempotency) |
| 2 | Bucket B — GAP-951 responsive collapsible sidebar <768px + responsive pages + Playwright mobile spec | ✅ DONE | — (44px targets + 7 page headers + mobile-admin.spec.ts 5/5; breakpoint grep 67) |
| 3 | Bucket C — GAP-950 sample-data import + wizard scope-revise verify | ✅ DONE | — (POST /api/v1/onboarding/sample-data + OnboardingSampleDataIT 2/2; live walk HTTP 201 dashboard populated; wizard pre-existing) |
| 4 | Bucket D — GAP-297 FE batch button + preview drawer + email dispatch | ✅ DONE | — (batch-invoice-drawer.tsx preview→confirm + email channel live Wave 18a) |

**Deferred (existing gaps — not new follow-ups):**

| Deferred item | Verdict | Tracked |
|---|---|---|
| GAP-297 Zalo/SMS notification dispatch | ❌ NOT-IMPLEMENTED (out-of-scope Phase 1.5) | Existing [GAP-063](../../04-quality/gaps/phase-1-beta/GAP-063-sms-zalo-notification-integration.md) — vendor-gated (Zalo OA Business account + SMS provider contract = real-user action). Email is the live channel Phase 1 BETA. |
| Vendor-gated Zalo/SMS live integration | ❌ NOT-IMPLEMENTED (out-of-scope Phase 1.5) | Existing GAP-063 / GAP-286 — vendor-gated, no new gap needed |
| GAP-950 AC#2 DB `onboarding_progress` table | ❌ NOT-IMPLEMENTED (out-of-scope — judged unnecessary) | localStorage acceptable Phase 1 BETA (one-time single-device onboarding); documented in GAP-950 closure, not pending |

All 4 §3 Scope buckets ✅ DONE. Deferred items map to existing gaps (GAP-063/286) or documented out-of-scope decisions (GAP-950 AC#2) — zero orphan items.

## 8. Log

- **2026-06-07:** Wave tạo. Scope "Local-verifiable trọn vẹn" per AskUserQuestion. GAP-950 scope-revise (wizard pre-existing). Vendor-gated 063/286 defer Phase 1.5. Walk-convergent FE → Phase 1 disjoint (BE + responsive) → Phase 2 FE features → Phase 3 walks.
- **2026-06-07 (closure):** Wave SHIPPED — status draft → complete. 3 gap (GAP-297/950/951) flipped DONE + git-mv `phase-1-beta/closed/` after coordinator live walks (gateway :9000, tenant sky-education). GAP-297 live: batch-generate prorated 1.3M + batch-confirm idempotency (createdCount 1 then 0). GAP-950 live: sample-data HTTP 201 dashboard populated. GAP-951: mobile-admin.spec.ts 5/5 PASS. GAP-950 + GAP-951 scope-revised (wizard + sidebar pre-existing; real deliverables = sample-data import + 44px touch targets/responsive headers). Phase 1 BETA P0 23 → 20. Deferred GAP-063/286 (existing gaps, vendor-gated Phase 1.5) + GAP-297 Zalo/SMS dispatch + GAP-950 AC#2 DB-progress (localStorage acceptable) — §9 reconciliation, zero orphan.
