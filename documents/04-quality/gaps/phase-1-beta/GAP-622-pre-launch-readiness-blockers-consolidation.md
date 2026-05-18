# GAP-622: Pre-launch readiness blockers consolidation (PDPL deadline + AWS GAP-612 + Phase 1 BETA gate score)

**Status:** 🔵 OPEN
**Priority:** 🔴 P0
**Domain:** Meta
**Found:** 2026-05-18 (Wave 92 closure meta-improvements audit per `wave-closure-scope-completeness.md` §3)
**Affects:** Phase 1 BETA launch readiness — 3 critical-path blockers cần consolidated tracking

## Problem

Phase 1 BETA launch readiness có 3 blockers đang scattered across multiple tracking surfaces:

| Blocker | Hiện tracked ở đâu | Deadline | Risk class |
|---|---|---|---|
| **GAP-612 AWS account suspension** | Standalone gap file + ROADMAP §Pending + wave-history Wave 91 followup | 2026-06-01 deletion hard cutoff (~14 ngày) | 🔴 CATASTROPHIC — account loss = lose CloudTrail history + EIP + Secrets + RDS |
| **PDPL 2023 deadline** | CLAUDE.md §CURRENT PHASE + release-1-plan-2026.md §1 + GAP-353 cluster (multi-gaps) | 2026-07-01 (~7 tuần) | 🔴 LEGAL — compliance requirement Vietnam |
| **Phase 1 BETA gate score ≥80** | ROADMAP §🎯 Status Snapshot + post-wave audit reports (ops-readiness 75/100 last) | Soft gate before invite Phase 2 | 🟧 HIGH — quality gate |

Hiện không có **consolidated dashboard / runbook** liệt kê đầy đủ + cross-reference. Future Claude session post-/clear hoặc dev sau gap về cần đọc 4+ docs để aggregate state.

## Root Cause

Blockers được file/track tại nhiều thời điểm khác nhau (GAP-612 filed Wave 91, PDPL queue từ Wave 23, BETA gate threshold from release-1 plan). Mỗi blocker có scope discipline riêng nhưng không có aggregation layer.

Per `wave-closure-scope-completeness.md` v1.0.0 (just shipped 2026-05-18) §3 reconciliation table — wave-level scope completeness. Đây là **phase-level scope completeness** parallel concern: Phase 1 BETA gate scope phải có dashboard.

## Proposed Fix

### Phase 1: Create `documents/03-planning/roadmap/phase-1-beta-launch-readiness-dashboard.md`

Single dashboard doc với:

- **Blocker matrix** — 3 blockers + status + deadline + dependency
- **Current score path** — Phase 1 BETA gate ≥80 score tracking (ops-readiness latest + quality-audit latest + security latest)
- **Critical path timeline** — D-day backward planning
- **Decision tree** — vd "If GAP-612 silent >D+7, evaluate AWS account migration"

### Phase 2: Cross-reference all 3 blocker gaps + ROADMAP §Pending

- GAP-612 add cross-link to dashboard
- PDPL gaps (GAP-353 cluster) add cross-link
- ROADMAP §Pending update to point dashboard

### Phase 3: Weekly cadence review

Per `post-wave-audit-mandate.md` parallel pattern — dashboard refresh weekly (hoặc khi blocker state change).

## Acceptance Criteria

- [ ] Dashboard doc shipped tại `documents/03-planning/roadmap/phase-1-beta-launch-readiness-dashboard.md`
- [ ] 3 blockers consolidated với current state + deadline + decision tree
- [ ] Cross-references added trong GAP-612 + PDPL gaps + ROADMAP §Pending
- [ ] Weekly cadence convention documented (vd "Refresh mỗi thứ Hai")
- [ ] Status flip DONE khi Phase 1 BETA gate ≥80 verified + all 3 blockers resolved hoặc properly tracked

## Related

- GAP-612 — AWS account suspension (P0 BLOCKER)
- GAP-353 cluster — PDPL implementation (multi-gap)
- ROADMAP §🎯 Status Snapshot + §Pending list
- Wave 91 plan §Trigger — GAP-612 surfaced
- Rule: `wave-closure-scope-completeness.md` v1.0.0 (parallel scope discipline pattern)
- Release plan: `documents/03-planning/roadmap/release-1-plan-2026.md` §1.7 deadlines

## Log

- **2026-05-18 (filed):** Filed by Wave 92 closure meta-improvements audit. Top 3 improvement areas surfaced 2026-05-18 session: pre-launch readiness blockers consolidation = #3 priority (P0 — time-bound, deadline-driven). Per user 2026-05-18 decision "File 3 gap files TOP 3 + defer execution" — execution defer Wave 94+ post-release-2-plan-lock. Tracking-only filing này tránh silent loss per `wave-closure-scope-completeness.md` recursion.
