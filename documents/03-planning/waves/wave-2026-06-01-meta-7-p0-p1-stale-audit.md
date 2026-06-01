---
title: Wave meta-7 — Full P0+P1 stale-status audit (172 gaps)
wave: 7
waves: [meta-7]
tag_primary: meta
tags_secondary: [audit, csv-canonical, force-multiplier]
counter: 7
created: 2026-06-01
date_launch: 2026-06-01
status: draft
---

# Wave meta-7 — Full P0+P1 stale-status audit (172 gaps)

**Trigger:** GAP-791/792 stale-OPEN drift discovery 2026-06-01 — code fix shipped PR #1937 nhưng CSV vẫn báo `status=OPEN completion_pct=0`. User-flagged "state-check toàn bộ P0 P1 gaps để cập nhật đúng index". Pattern class = canonical CSV ↔ code drift across 172 active P0+P1 gaps.

**Goal:** Verify mỗi P0+P1 OPEN/PARTIAL gap có CSV status match thực tế code; flip stale-OPEN → DONE; điều chỉnh completion_pct cho PARTIAL inaccurate; file follow-up nếu sister sites lộ ra.

**META P0 force-multiplier per `meta-gap-priority.md` §3** — 1 audit pass eliminate drift → Phase 1 BETA gate signal trustworthy + future state-check ~50× cheaper qua `query-gaps.sh`.

---

## 1. Brainstorm Q1 — Inside-out + Outside-in

**Inside-out 4 buckets per `inside-out-completeness-trigger.md` §3:**

- **ROADMAP §🚀:** Wave tenant-domain-1 vừa close 2026-06-01 với 4 PARTIAL gap; GAP-817 audit deadline 2026-06-04
- **inside-out-queue.md:** 5 queued items (Premium plan / Feedback channel / Email content / Manual / Live verify) — không match meta scope
- **Audit-surfaced:** GAP-791/792 drift discovery (cùng class với 172 candidate gaps); pattern recurrence likely
- **Outside-in NEW:** SKIP per `outside-in-coverage-trigger.md` §4 — scope internal meta audit (không user-facing); existing audit-to-gap-pipeline.md §2.8 covers methodology

**Q2 Risks:**
- 172 gaps × manual state-check = too large for 1 wave nếu mỗi gap deep-grep → mitigate qua classification taxonomy (Bucket 0) + 4 parallel agents
- Agent output format drift → mitigate qua Foundation bucket shipping CSV batch-update script template
- False-flip risk (gap report DONE nhưng AC chưa met) → mitigate qua `gap-done-discipline.md` §2 strict check trong taxonomy

**Q3 Out-of-scope:**
- phase-1.5-paid + phase-2 + phase-3 gaps (defer Wave meta-8+)
- New gap filing (chỉ state-check existing rows)
- Code fix (chỉ verify code-state; fixes ship separate waves)

---

## 2. Task Breakdown

| Bucket | Owner | Scope | Effort |
|---|---|---|---|
| **0 Foundation** | sequential FIRST | Classification taxonomy doc + CSV batch-update helper script + agent prompt template | ~30min |
| **A** | parallel | P0 OPEN (17) + P0 PARTIAL (30) = 47 gaps | ~45min |
| **B** | parallel | P1 OPEN phase-1-beta first 38 gaps | ~45min |
| **C** | parallel | P1 OPEN phase-1-beta remaining 38 + n/a 7 = 45 gaps | ~45min |
| **D** | parallel | P1 PARTIAL phase-1-beta 52 + n/a 2 = 54 gaps | ~45min |

Total: ~45min wall-clock (parallel) + ~30min coordinator merge = ~1.5h. Per `agent-model-opus-default.md` v1.0.0 — all spawn `model: "opus"`. Per `agent-background-spawn-default.md` v1.0.1 — all `run_in_background: true`.

---

## 3. Scope

### Bucket 0 — Foundation (sequential, MERGE FIRST)

- **Output:** `documents/04-quality/audits/meta/2026-06-01-wave-meta-7-classification-taxonomy.md`
- **Classification taxonomy:**
  - `SHIPPED→DONE`: code fix shipped + AC met → flip CSV status=DONE completion_pct=100 + git mv file → `phase-X/closed/`
  - `PARTIAL→adjust_pct`: code partial; adjust completion_pct match reality (20/40/60/80%); update notes column
  - `OPEN→keep`: no code fix shipped yet; CSV correct
  - `SCOPE-REVISE`: gap description outdated/misdiagnosis; flag for next session re-write
  - `DROP`: gap genuinely obsolete (feature deprecated / superseded); file rationale + status=WONTFIX
- **Helper script:** `scripts/audit-stale-gap-status.sh` — takes gap-id list + verdict per gap, batch-applies CSV update (sed in-place) + git mv files
- **Agent prompt template:** standardized format Bucket A-D agents output:

```markdown
## GAP-NNN verdict: <SHIPPED-DONE | PARTIAL→pct=NN | OPEN→keep | SCOPE-REVISE | DROP>
- Evidence: <grep output / commit ref / IT test ref>
- AC status: <N/M checkboxes met per gap file>
- New completion_pct: <0-100>
- New notes: <≤80 chars>
```

### Bucket A — P0 stale-status audit (47 gaps)

- Scope: 17 P0 OPEN + 30 P0 PARTIAL phase-1-beta
- Acceptance: each gap classified per Foundation taxonomy; output table appended to Bucket A audit artifact `documents/04-quality/audits/meta/2026-06-01-wave-meta-7-bucket-a-p0.md`
- CSV apply: deferred to coordinator merge step (avoid concurrent CSV writes)

### Bucket B — P1 OPEN first half (38 gaps)

- Scope: P1 OPEN phase-1-beta sorted by GAP-id, first 38
- Output: `documents/04-quality/audits/meta/2026-06-01-wave-meta-7-bucket-b-p1-open-1.md`

### Bucket C — P1 OPEN remainder + n/a (45 gaps)

- Scope: P1 OPEN phase-1-beta last 38 + P1 OPEN n/a 7
- Output: `documents/04-quality/audits/meta/2026-06-01-wave-meta-7-bucket-c-p1-open-2.md`

### Bucket D — P1 PARTIAL audit (54 gaps)

- Scope: P1 PARTIAL phase-1-beta 52 + n/a 2 — verify completion_pct accuracy
- Output: `documents/04-quality/audits/meta/2026-06-01-wave-meta-7-bucket-d-p1-partial.md`

---

## 4. State-Check Evidence (per `audit-to-gap-pipeline.md` §2.6)

| Symbol | Type | Verify | Result | Verdict |
|---|---|---|---|---|
| `scripts/query-gaps.sh` | helper script | `ls scripts/query-gaps.sh` | exists | ✅ |
| `documents/04-quality/gaps/gap-status.csv` | canonical CSV | header line check | OK | ✅ |
| `scripts/check-gap-status-csv.sh` | CI validator | exists | OK | ✅ |
| `scripts/check-gap-folder-location.sh` | CI validator | exists | OK | ✅ |
| `scripts/audit-stale-gap-status.sh` | NEW batch update | — | — | 🆕 to-be-created Bucket 0 |
| `documents/04-quality/audits/meta/` | output folder | `ls documents/04-quality/audits/meta/` | exists | ✅ |

No aspirational symbols. Foundation bucket owns 🆕 creation.

---

## 5. Verification Gates (per bucket)

| Bucket | Gate |
|---|---|
| 0 | Taxonomy doc shipped + helper script unit-tested + 1 dry-run sample PASS |
| A-D | Audit artifact shipped với verdict table cho mỗi gap-id in scope |
| Coordinator merge | Apply CSV updates via Bucket 0 script; git mv DONE files to `phase-X/closed/`; run `check-gap-status-csv.sh` + `check-gap-folder-location.sh` local CI |

---

## 6. Agent Spawn Pattern

**Per `agent-background-spawn-default.md` v1.0.1 + `agent-model-opus-default.md` v1.0.0:**

```
1. Bucket 0 Foundation: coordinator-inline (no agent — direct edit + script ship)
2. After Bucket 0 merge to main:
   Spawn 4 background agents in single message:
   - Agent A: P0 47 gaps, isolation=worktree, model=opus, run_in_background=true
   - Agent B: P1 OPEN first 38, isolation=worktree, model=opus, run_in_background=true
   - Agent C: P1 OPEN second 45, isolation=worktree, model=opus, run_in_background=true
   - Agent D: P1 PARTIAL 54, isolation=worktree, model=opus, run_in_background=true
3. Wait for 4 notifications → coordinator merge each bucket PR sequentially
4. Coordinator runs Bucket 0 helper script on merged audit artifacts → batch CSV updates + git mv → single closure PR
```

---

## 7. Closure Protocol

Per `gap-done-discipline.md` + `wave-closure-scope-completeness.md` + `post-merge-sync-completeness.md` + `post-wave-cleanup.md`:

- Each bucket PR ships audit artifact (no gap state change inside bucket PR)
- Coordinator closure PR: apply CSV updates + git mv DONE files + Scope-Completeness Reconciliation table in PR body
- ROADMAP §🚀 Next Action updated với drift findings count
- `wave-history.jsonl` append với metrics + drift class identified
- `bash scripts/prune-merged-worktrees.sh --yes` post-merge
- Per `wave-closure-scope-completeness.md` v1.0.1 §3 — Scope-Completeness Reconciliation in CLOSURE PR BODY (NOT in wave plan file — learned từ Wave tenant-domain-1 placement miss)

---

## 8. Log

- **2026-06-01** (draft): Plan created. Triggered by GAP-791/792 stale-OPEN drift discovery during Wave security-1 scoping. Pattern likely recurs across 172 P0+P1 gaps. META P0 force-multiplier — 1 audit pass eliminate drift permanently + future state-check via `query-gaps.sh` trustworthy. Outside-in audit SKIP per `outside-in-coverage-trigger.md` §4 (internal meta scope). State-Check Evidence §4 verified — 5 symbols ✅ exists / 1 🆕 to-be-created Bucket 0. Cross-layer NO (pure docs/CSV audit). Wave-pack pattern: max 4 parallel agents under 5-agent ceiling. Foundation bucket sequential FIRST để standardize agent output format. Estimate ~1.5h wall-clock (parallel) — confirm post-execution.
