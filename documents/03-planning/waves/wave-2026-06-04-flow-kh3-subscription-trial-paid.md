---
title: Wave flow-kh3 — Subscription create + trial→paid migration
status: draft
created: 2026-06-04
updated: 2026-06-04
waves: [flow-kh3]
tag_primary: flow
tags_secondary: [kh3, subscription, billing, trial-to-paid, campaign]
counter: 1
gaps: []
campaign: flow-verification-campaign
---

# Wave flow-kh3 — Subscription create + trial→paid migration

**Goal:** Walk end-to-end flow KH-3 (Owner create subscription on trial tenant → 14-day countdown → manual VietQR upgrade → admin confirm payment → status flip PAID) trên production-equivalent stack, đạt **G1 PASS**. Chain với KH-1 + KH-2c (Owner tenant đã có từ Wave flow-kh1 closure).

**Trigger:** Flow KH-3 next-in-chain per `flow-verification-campaign.md` §3 dependency graph (KH-2c owner login + onboarding → KH-3 subscription). Phase 1 BETA gate — Owner trial tenant phải convert được paid để validate revenue path.

**Status:** 🟡 DRAFT stub — scope TBD tại session start. Full §3 expansion happens khi session pick wave này; current stub satisfies `check-wave-plan-completeness.sh` structural mandate.

---

## 1. Brainstorm

**Q1 (alignment):** Persona `Owner (tenant)` (P2 — center owner persona reused từ Wave flow-kh1) + `PlatformAdmin` (payment confirmer, KH-2a evidence reused). Domain `subscription` + `billing` + `email` (renewal notification). Downstream cho mọi paid-tier feature (KC-* phase-1.5+).

**Q2 (trade-offs):** Walk full E2E chain (create subscription → trial state → manual VietQR upgrade → admin confirm → PAID flip) cùng wave thay vì split:
- State-continuous flow (token + DB state + admin manual step), không tách clean per sub-step
- Single-agent campaign-loop per Wave flow-kh1/kh2 pattern proven
- Chain với KH-1+KH-2c verify Owner journey complete

**Q3 (risks):**
- **VietQR provider integration**: recent commit `ac54a419` gate tier upgrade behind manual VietQR confirm — provider config state TBD verify
- **Trial countdown timer**: 14-day BR-SUB needs DB-side scheduler hoặc check-on-request — implementation path TBD state-check
- **Admin confirm UX**: admin có UI button confirm payment? hay chỉ DB UPDATE? state-check khi pick wave
- **Email notification**: trial expiry + payment confirm emails — kitehub-email channel ready từ Wave flow-kh1 walk
- **Phase 1 BETA scope**: K-12 P5 không scope; chỉ medium-center tier per release-1 plan

**Inside-out completeness check (per `inside-out-completeness-trigger.md`):** Deferred to session start — sẽ pull ROADMAP §🚀 + `inside-out-queue.md` + CSV `subscription` domain gaps trước khi lock §3 Scope.

**Outside-in audit (per `outside-in-coverage-trigger.md`):** Deferred to session start. Architecture-decision keywords trong gap filing (vd "VietQR processor integration" / "trial scheduler engine") sẽ fire outside-in audit nếu apply.

---

## 2. Task Breakdown

| Bucket | Scope | Owner | Effort | Disjoint? |
|--------|-------|-------|--------|-----------|
| 0 (Pre-walk) | Spawn Opus pre-walk persona simulation per `pre-walk-persona-simulation-mandate.md` §1 — `Owner trial creates subscription → expects trial UI → upgrade prompt → VietQR → admin confirm → PAID flip`; return ≥5 failure modes | Coordinator | ~5-10 min agent + ~30 min batch-fix | n/a — single agent |
| A | Loop walk + catalog (subscription create → trial → upgrade → confirm → PAID) | claude (session-pick) | 30-60 min | n/a — state-continuous |
| B | Batch-fix blocker (nếu lòi mid-walk) | claude | 10-30 min/cycle | n/a |
| C | Re-walk + G1 verdict + G2 handoff MD per `g2-handoff-md-mandate.md` | claude | 15-30 min | n/a |

Single-agent campaign-loop per Wave flow-kh1/kh2 protocol. Bucket-level expansion deferred — sẽ scope tại session start khi state-check reveals concrete scope items.

---

## 3. Scope

**TBD at session start.** Required reading before lock scope:

- `documents/01-business/kitehub/subscription/rules.md` — BR-SUB-* values + config keys
- `documents/01-business/kitehub/subscription/use-cases.md` — trial-to-paid use case
- `documents/01-business/kitehub/subscription/api-contract.md` — endpoint shape + DTOs
- Recent commit `ac54a419 feat(subscription): gate tier upgrade behind manual VietQR payment confirm` — implementation entry point
- `documents/01-business/kitehub/billing/*` — VietQR provider config
- `documents/03-planning/inside-out-queue.md` — user-flagged subscription scope items

**Dependency check:** Owner tenant từ KH-1+KH-2c chain ✅ (g2test-an-8 exists per Wave flow-kh1 closure handoff `documents/03-planning/session-handoffs/2026-06-04-flow-kh1-closure-kh3-prep.md`).

**Wave scope completeness reconciliation table** (per `wave-closure-scope-completeness.md` §3): defer to closure PR.

---

## 4. State-Check Evidence

**TBD at session start.** Required state-check rows per `audit-to-gap-pipeline.md` §2.6 wave-plan pre-flight + `contract-first-for-cross-layer.md` §3.2 api-contract row (cross-layer wave likely — FE upgrade button + BE subscription endpoint + admin confirm).

Minimum row checklist:
- `documents/01-business/kitehub/subscription/api-contract.md` (api-contract for cross-layer scope)
- `kitehub/kitehub-subscription/**/SubscriptionService.java` (BE entry point)
- `kitehub-frontend/src/app/(owner)/subscription/**` (FE entry point — path TBD verify)
- `documents/04-quality/gaps/gap-status.csv` query for OPEN subscription gaps

---

## 5. Verification Gates

| Gate | Owner | Criteria | Status |
|---|---|---|---|
| G1 — agent runtime walk | Claude | Subscription create on Owner trial tenant + trial countdown reaches 14d (or simulate via DB UPDATE) + manual VietQR upgrade trigger + admin confirm → status flip PAID + email notification sent (MailHog verify) | ⬜ |
| G2 — human local test | User | Login as Owner (g2test-an-8 từ KH-1) → walk subscription state → simulate expiry → confirm payment flow per G2 handoff recipe MD | ⬜ |
| G3 — production parity | Claude + User | Production: VietQR provider config reachable + admin confirm via real workflow + SES email delivery + DB PAID state persisted | ⬜ |

G2 handoff MD recipe per `g2-handoff-md-mandate.md` §3 — ship same PR as G1 PASS flip.

---

## 6. Agent Spawn Pattern

**Single-agent campaign-loop** per Wave flow-kh1/kh2 proven pattern:
- KHÔNG parallel-spawn (state-continuous flow không split clean)
- Pre-walk Opus agent BACKGROUND per `agent-background-spawn-default.md` §1 + `agent-model-opus-default.md` §1 (5-10 min reasoning) → coordinator prep walk runbook parallel
- Walk + batch-fix + re-walk execute sequential trong session

---

## 7. Closure Protocol

Per `feedback_post_merge_doc_sync.md` 4-target sync + `post-wave-cleanup.md` + `g2-handoff-md-mandate.md`:

1. Flip `gap-status.csv` rows DONE cho gaps closed wave này (if any) + git mv → `phase-1-beta/closed/`
2. ROADMAP §🎯 Current Status Snapshot — add Wave flow-kh3 closure entry
3. `wave-history.jsonl` append entry per `wave-tag-numbering-convention.md` §2.5 new schema (tag_primary=flow, counter=3 inside flow tag — re-verify counter at closure time)
4. Wave plan frontmatter `status: draft → complete` flip + Scope-Completeness Reconciliation table per `wave-closure-scope-completeness.md` §3
5. Campaign §4 row flip → `🔄 walk-pass-pending-human` (G1 ✅) + ship G2 handoff MD recipe per `g2-handoff-md-mandate.md` §3
6. Session-handoff note `documents/03-planning/session-handoffs/YYYY-MM-DD-flow-kh3-closure.md`
7. `bash scripts/prune-merged-worktrees.sh --yes` per `post-wave-cleanup.md` §2

---

## 8. Log

- **2026-06-04 (plan stub ship):** Filed at Wave flow-kh1 closure session-end as KH-3 = next-in-chain per campaign §3 dependency graph (KH-2c → KH-3). Owner tenant exists từ KH-1 closure (g2test-an-8). Subscription module ready per recent commit `ac54a419` (manual VietQR gate shipped). Plan stub satisfies `check-wave-plan-completeness.sh` structural mandate (8 sections + 4 frontmatter fields). Full §3 Scope + §4 State-Check + bucket-level expansion happens tại session start khi pick wave này — `/start-session` → state-check `subscription/` BE+FE+docs → inside-out + outside-in audit per triggers → lock scope → walk G1.
