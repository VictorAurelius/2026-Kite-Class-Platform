---
title: Wave audit-stale-sweep-1 — Phase 1 BETA P0 stale-CSV state-check
status: active
created: 2026-05-26
updated: 2026-05-26
audience: dev
tag_primary: audit-stale-sweep
tags_secondary: [phase-1-beta, csv-hygiene, meta]
waves: [audit-stale-sweep-1]
gaps: [GAP-751]
---

# Wave audit-stale-sweep-1 — Phase 1 BETA P0 stale-CSV state-check

## 1. Scope + motivation

Per session-handoff `2026-05-26-wave-br-7-shipped-audit-stale-sweep-queued.md` §"Sequencing chốt":
> Wave audit-stale-sweep (~2h coordinator inline) — state-check all 44 active Phase 1 BETA P0 (expected eliminate ~9-18 stale CSV rows)

**Trigger pattern (Wave br-7 evidence):** 4/5 buckets state-check phát hiện code ĐÃ shipped Wave 5 era nhưng GAP CSV stale OPEN P0 (GAP-215/216/217/218). Per `gap-done-discipline.md` §2 recurrence + `feedback_outside_in_recurring_miss.md` — stale-OPEN CSV pattern recurring → systemic CSV hygiene debt.

**Scope:** State-check each active P0 gap trong Phase 1 BETA → for each, decide:
- ✅ **DONE** (code shipped + AC met) → flip CSV → git mv to `phase-1-beta/closed/`
- 🟡 **STAY PARTIAL** (work in progress, AC partial) → keep status + update progress %
- 🟢 **REFINE** (scope changed, AC needs update) → file follow-up gap
- ❌ **STILL OPEN** (no work yet) → keep status (BUT verify scope still relevant)

## 2. Methodology (per audit-to-gap-pipeline.md §2.5-§2.8 state-check family)

Per `audit-to-gap-pipeline.md` §2.6 wave-plan state-check + §2.7 decision-doc state-check:

For each gap:
1. **Read AC** từ gap file
2. **Grep code/tests** for fix evidence (symbol references / test names / config keys / migration file names)
3. **Cross-reference** với recent git log (`git log --grep="GAP-XXX"`) + wave-history.jsonl
4. **Verdict** per §1 4-option matrix
5. **Document** trong audit artifact (single batch artifact per `output-review-mandate.md` §3 Quality audit row)

## 3. Tiered execution

Per progress %, prioritize high-likelihood-stale first:

| Tier | Progress band | Count | Rationale |
|---|---|---|---|
| **Tier 1** | ≥80% | 15 gaps | Highest likelihood of stale-DONE |
| **Tier 2** | 50-79% | 10 gaps | Medium likelihood — likely PARTIAL stays |
| **Tier 3** | <50% | 13 gaps | Lower likelihood — most truly OPEN |

Active scope (per `bash scripts/query-gaps.sh P0 "" phase-1-beta` 2026-05-26):
- **38 active gaps** (handoff said "44" — approximate, actual count 38)
- Distribution: 15 Tier 1 + 10 Tier 2 + 13 Tier 3

## 4. Out of scope

- Phase 1.5+ / Phase 2 / Phase 3 gaps (only Phase 1 BETA scope)
- P1/P2 gaps (only P0 scope — focused stale-sweep)
- Code changes (state-check + CSV hygiene only; if state-check surfaces actionable code bug → file follow-up gap, không fix in this wave)
- Outside-in audit (per `outside-in-coverage-trigger.md` §4 exception "Wave 100% internal scope — ops/refactor/tech debt")

## 5. Deliverables

1. **Audit artifact:** `documents/04-quality/audits/quality/2026-05-26-wave-audit-stale-sweep-1-phase-1-beta-p0-state-check.md`
2. **CSV flips:** N rows in `documents/04-quality/gaps/gap-status.csv` (expected 9-18 per handoff projection)
3. **File moves:** `phase-1-beta/<gap>.md` → `phase-1-beta/closed/<gap>.md` for each DONE flip (per `gap-folder-organization.md` + `gap-done-discipline.md` §2)
4. **Follow-up gaps:** N new gap files for any actionable scope refinements discovered
5. **5-target sync** per `post-merge-sync-completeness.md` §2:
   - gap-status.csv (Target 1)
   - ROADMAP.md §🎯 Current Status Snapshot (Target 2)
   - wave-history.jsonl (Target 3)
   - MEMORY.md index (Target 4 — if new memory entries)
   - session-handoff note (Target 5)

## 6. Success criteria

- [ ] All 38 active P0 state-checked with explicit verdict in audit artifact
- [ ] Each DONE flip backed by evidence (commit / PR / test / config grep)
- [ ] Each STAY PARTIAL backed by reason (what % remaining)
- [ ] Single audit artifact + single CSV PR (atomic state-sweep)
- [ ] PR docs-only → auto-merge per `docs-only-pr-auto-merge.md` v1.0.2

## 7. Cross-link

- Session-handoff: `documents/03-planning/session-handoffs/2026-05-26-wave-br-7-shipped-audit-stale-sweep-queued.md`
- Wave br-7 pattern: `documents/03-planning/waves/wave-2026-05-25-beta-readiness-7-document-performance-cluster.md`
- Wave numbering: `.claude/rules/wave-tag-numbering-convention.md` v1.0.0 (`tag_primary: audit-stale-sweep` + counter 1)
- Source-of-truth: `documents/04-quality/gaps/gap-status.csv` (Phase 4 per `gap-architecture-v2.md`)

## 8. Log

- **2026-05-26:** Wave plan created. Source = session-handoff `2026-05-26-wave-br-7-shipped-audit-stale-sweep-queued.md` Sequencing chốt block. Approach = coordinator-inline (no agent spawn — state-check is small per-gap, parallelism overhead > work; per `agent-model-opus-default.md` §3 exception "scope ngắn không phù hợp agent spawn"). Tiered execution Tier 1 → Tier 2 → Tier 3.
