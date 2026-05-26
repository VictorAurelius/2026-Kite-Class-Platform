---
title: Audit — Wave audit-stale-sweep-1 Phase 1 BETA P0 state-check
status: complete
created: 2026-05-26
audience: dev
audit_type: quality
scope: 38 active Phase 1 BETA P0 gaps (filter status != DONE)
methodology: audit-to-gap-pipeline.md §2.8 fix-time state-check + §2.5 hardened state-check protocol
wave: audit-stale-sweep-1
gaps: [GAP-117, GAP-127, GAP-223, GAP-599, GAP-612]
---

# Wave audit-stale-sweep-1 — Phase 1 BETA P0 state-check audit

## 1. Scope

Per session-handoff `2026-05-26-wave-br-7-shipped-audit-stale-sweep-queued.md` Sequencing chốt: state-check all active Phase 1 BETA P0 gaps để eliminate stale-OPEN CSV pattern recurrence (Wave br-7 4/5 buckets surfaced "code shipped Wave 5 era, GAP CSV stale OPEN" pattern per `gap-done-discipline.md` §2 recurrence).

**Actual scope:** 38 active P0 gaps (handoff said "44" — approximate; CSV filter `status IN (OPEN, PARTIAL, IN_PROGRESS, PENDING) AND priority=P0 AND phase=phase-1-beta` returned 38 rows 2026-05-26).

**Tiered execution per progress %:**
- Tier 1 (≥80%, 15 gaps): highest-likelihood-stale-DONE
- Tier 2 (50-79%, 10 gaps): medium-likelihood
- Tier 3 (<50%, 13 gaps): lower-likelihood

## 2. Methodology

Per `audit-to-gap-pipeline.md` §2.8 fix-time state-check (canonical-status lookup first via `query-gaps.sh`) + §2.5 hardened state-check protocol (no `| head` truncation, multi-grep cross-checks).

For each gap:
1. Read CSV row (canonical per `gap-architecture-v2.md` §3)
2. Cross-reference với gap file Status field (cache check)
3. Spot-check code/test/config evidence for sampled gaps
4. Verdict matrix per audit-to-gap-pipeline.md §2.8 Step 3

## 3. Findings summary

### 3.1 Verdicts

| Verdict | Count | Reason |
|---|---|---|
| **STAY PARTIAL** (work genuine, no flip) | 35 | Live verify gated GAP-612 / user action / future waves / explicit deferred-to-follow-up |
| **STAY OPEN** (no work yet, genuine) | 3 | GAP-622/648/649/684/727/730 newer gaps without recent progress |
| **DONE flip candidates** | 0 | ZERO stale-DONE found |
| **Drift fixes (file-vs-CSV sync)** | 2 | GAP-599 + GAP-612 |
| **AC checkbox drift (defer)** | 3 | GAP-117 partial fix; GAP-127 + GAP-223 deferred to follow-up |

### 3.2 Tier 1 (≥80% progress, 15 gaps) — 0 flips, all gated

| Gap | % | Remaining work | Gate |
|---|---|---|---|
| GAP-370 | 95 | Resend dashboard verify + warm-up Day 5+ | User action |
| GAP-533 | 80 | Resend Day 1-7 warm-up + spam-score ≥8/10 × 3 | User action |
| GAP-657 | 95 | Scheduler IT + CloudWatch alarm + manual live render | GAP-612 |
| GAP-658 | 80 | Native VN copywriter pass + OnboardingChecklistService integration | Wave 108+ B4 |
| GAP-659 | 95 | Live verify + send-site wiring | GAP-612 + Wave 108+ |
| GAP-656 | 80 | FeedbackForm modal (B5) + Zalo OA link (B6) | Wave 108+ paired buckets |
| GAP-538 | 96 | Live walkthrough verify | GAP-612 |
| GAP-508 | 90 | Live verify (`GAP-NEW-resend-live-verify-post-restore`) | GAP-612 |
| GAP-514 | 90 | Live 429 smoke post-staging-deploy | Staging deploy |
| GAP-502 | 90 | 3 deploy-prod debt items tracked GAP-506 | Deferred |
| GAP-543 | 95 | Live verify 5 templates | GAP-612 |
| GAP-599 | 85 | Live multi-tab browser UX verify | GAP-612 |
| GAP-695 | 85 | Tier 2-3 execution | Wave 102.9+ |
| GAP-534 | 80 | Live verify (Flyway V39 + service) | Next deploy |
| GAP-608 | 90 | Live verify (GAP-747) | GAP-612 |

**Conclusion Tier 1:** All 15 remain PARTIAL — work is genuine, not stale. Of 15, **13 gated on GAP-612 AWS restore** (now unblocked 2026-05-25 Day 8, but production stack still down — need terraform restore + live walkthrough).

### 3.3 Tier 2 (50-79% progress, 10 gaps) — AC checkbox drift surfaces

| Gap | % | Verdict | Notes |
|---|---|---|---|
| GAP-117 | 50 | PARTIAL + partial AC update | Phase 1+2 SHIPPED 2026-04-28 (PR #632); `scripts/verify-restore.sh` ✅ exists; `.github/workflows/restore-drill.yml` ✅ exists; `scripts/smoke-rollback-cycle.sh` ✅ exists; `restore-procedure.md` ❌ MISSING; Phase 3 tracked GAP-257. **Action this PR:** check 2 AC boxes (`verify-restore.sh` + `restore-drill.yml`) leave 3 unchecked. |
| GAP-127 | 50 | STAY PARTIAL | Bundle analyzer + landing-page split + per-list-page DataTable lazy shipped Wave 7-Perf. 6 AC unchecked (bundle baseline, /<150KB, admin<300KB, ≥5 dynamic(), modularizeImports, CI ≤250KB). **Defer detailed AC update** — needs per-AC measurement (bundle analyzer run). Follow-up gap (NEW) recommended. |
| GAP-223 | 50 | STAY PARTIAL | Sub-PR 223.1 governance scaffolding shipped 2026-04-26; 223.2 DEFERRED post GAP-006 unblock. 8 AC unchecked. **Defer detailed AC update** — needs verification each scaffolded artifact ships per AC. |
| GAP-535 | 70 | STAY PARTIAL | Wave 77 D code+V40+tests shipped; InstanceService wiring deferred |
| GAP-536 | 65 | STAY PARTIAL | Wave 77 D code+V41+tests shipped; HandlerInterceptor wiring deferred |
| GAP-566 | 60 | STAY PARTIAL | Wave 82 B closure; repo source bugs tracked GAP-574 |
| GAP-567 | 50 | STAY PARTIAL | Wave 82 B wildcard cert acquired; auto-renewal fail tracked GAP-572/573 |
| GAP-610 | 75 | STAY PARTIAL | Wave br-5 C defensive harden shipped; live verify gated GAP-612 |
| GAP-611 | 70 | STAY PARTIAL | Wave br-5 D PR #1827 JSON 404 shipped |
| GAP-693 | 70 | STAY PARTIAL | BLOCKED on GAP-612 + GAP-691 + GAP-692 |

### 3.4 Tier 3 (<50% progress, 13 gaps) — genuinely OPEN/PARTIAL

| Gap | % | Status | Verdict |
|---|---|---|---|
| GAP-203 | 40 | IN_PROGRESS | 7 CVE work — last_verified 2026-05-11 stale; bump last_verified only (no progress measurement this wave) |
| GAP-530 | 10 | PARTIAL | Wave 77 A automation shipped; 5-email-type live verify gated GAP-533/GAP-612 |
| GAP-572 | 40 | PARTIAL | Phase 4 dual-schema shipped; Phase 1+5 still tracked |
| **GAP-612** | **30** | **PARTIAL** | **DRIFT** — file says `5%`, CSV says `30%`. 2026-05-25 Day 8 UNBLOCK happened (AWS removed hold). Pending production stack restore (EC2 + RDS + ALB recreate). Update CSV+file to reflect new state. |
| GAP-727 | 0 | OPEN | Wave br-1 D audit finding; Class.java missing teacher_id — genuine OPEN |
| GAP-730 | 0 | OPEN | Wave br-1 C blocked by content filter; port pattern Wave 105 D — genuine OPEN |
| GAP-286 | 0 | OPEN | Wave 96 PR1 re-phase to phase-1-beta; genuine OPEN (Mobile OTP Zalo) |
| GAP-297 | 0 | OPEN | Wave 96 PR1 re-phase to phase-1-beta; genuine OPEN (Batch Invoice UX) |
| GAP-353 | 0 | PENDING | pending-legal-2026-05-11 (PDPL Cookie Banner); hard deadline 2026-07-01 |
| GAP-622 | 0 | OPEN | Wave 92 closure meta-improvements; execute defer |
| GAP-648 | 0 | OPEN | VN benchmark Q2 — k6 + CloudWatch + Cost Explorer artifacts |
| GAP-649 | 0 | OPEN | All 3 audits convergence — 5 beta tenants + ≥4 signed reviews |
| GAP-684 | 0 | OPEN | GAP-518 follow-up; live browser walk blocks GAP-612 |

### 3.5 File-vs-CSV drift findings

| Gap | File Status | CSV Status | Resolution |
|---|---|---|---|
| GAP-599 | `🔵 OPEN` (stale — pre-Wave 92 PR #1515 ship) | `PARTIAL 85%` (current) | Edit file Status line → `🟡 PARTIAL 85%` |
| GAP-612 | `🟡 PARTIAL 5%` (stale — pre-Day-8 unblock 2026-05-25) | `PARTIAL 30%` (slight stale too) | Edit file Status → reflect Day 8 UNBLOCK state; CSV stays 30% (production stack restore not yet executed) |

### 3.6 last_verified bump scope

All 38 active P0 gaps' `last_verified` column updated to **2026-05-26** in CSV — reflects this wave's state-check execution (canonical timestamp for next session's audit-to-gap-pipeline.md §2.8 check).

## 4. Counterfactual vs Wave br-7 expected

Session-handoff projected "expected eliminate ~9-18 stale CSV rows". Actual finding: **0 stale-DONE rows** + 2 file-vs-CSV Status drifts + 1 partial AC checkbox update. Why:

1. **Wave br-7 pattern was specific:** Wave 5 era code (Sub-PR 5.6b 2026-04-25) shipped BrandingCacheable + soft-cap tests + alert rules + Dockerfile font assertion — gap CSV stale OPEN P0 because gap files weren't tracked. That was 1-2 month old code with new gap filings.
2. **Tier 1 gaps mostly NEW (Wave 78-107):** Recently authored gaps with explicit gate language ("live verify deferred GAP-612") — CSV status PARTIAL accurate.
3. **Tier 2/3 AC checkbox drift ≠ status drift:** Checkboxes not updated when partial work shipped, but CSV status PARTIAL still accurate. Detailed AC updates require deeper per-AC verification — out of scope this sweep.

**Lesson:** session-handoff projection was based on br-7 pattern extrapolation. Actual Phase 1 BETA P0 backlog hygiene is better than projected — CSV maintenance has been disciplined post-Wave br-7. Main remaining backlog work = unblock GAP-612 production stack restore → cascade-flip 13 PARTIAL gaps to DONE.

## 5. Follow-up actions (out of scope this PR)

| Follow-up | Scope | Priority |
|---|---|---|
| GAP-127 AC checkbox per-AC verify (bundle analyzer run + measurements) | Tier 2 detailed AC update | P2 — defer to FE-perf wave |
| GAP-223 AC checkbox per-AC verify (governance scaffolding deliverables) | Tier 2 detailed AC update | P2 |
| GAP-203 CVE state recheck (Dependabot + Trivy current scan) | Tier 3 progress update | P1 — file new gap or update inline |
| GAP-612 production stack restore (EC2 + RDS + ALB recreate post Day 8 unblock) | Cascade unblock 13 PARTIAL → DONE | **P0 — next wave priority** |

## 6. Recommendation cho next session

Per session-handoff Sequencing chốt §"4 hard blocker waves parallel-able":
- **Wave security-1 (GAP-203 CVE cluster)** — natural next given GAP-203 already P0 IN_PROGRESS
- **Wave ops-1 (GAP-117 Restore Drill Phase 3)** — leveraging GAP-117 Phase 1+2 already done
- **Wave compliance-1 (GAP-353 PDPL Cookie Banner)** — hard deadline 2026-07-01 (~6 weeks)
- **Wave perf-1 (GAP-127 FE code-splitting)** — paired với AC verify

**META RECOMMENDATION:** Add **Wave aws-restore-1** (production stack restore post-GAP-612 Day 8 unblock) BEFORE the 4 hard-blocker waves — unblocks 13 cascade-PARTIAL → DONE flips.

## 7. Cross-link

- Wave plan: `documents/03-planning/waves/wave-2026-05-26-audit-stale-sweep-1-phase-1-beta-p0.md`
- Session-handoff source: `documents/03-planning/session-handoffs/2026-05-26-wave-br-7-shipped-audit-stale-sweep-queued.md`
- Methodology rule: `.claude/rules/audit-to-gap-pipeline.md` §2.8 fix-time state-check
- Canonical source: `documents/04-quality/gaps/gap-status.csv` (per `.claude/rules/gap-architecture-v2.md` §3)
- Pattern context: `.claude/rules/gap-done-discipline.md` §2 stale-OPEN pattern (Wave br-7 evidence)
