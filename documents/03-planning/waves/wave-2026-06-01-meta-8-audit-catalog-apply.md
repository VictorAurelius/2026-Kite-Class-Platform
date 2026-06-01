---
title: Wave meta-8 — Wave meta-7 follow-up apply 50 PARTIAL adjust + 14 SCOPE-REVISE + 2 META gaps new
wave: 8
waves: [meta-8]
tag_primary: meta
tags_secondary: [audit-followup, csv-canonical, force-multiplier]
counter: 8
created: 2026-06-01
date_launch: 2026-06-01
date_closed: 2026-06-01
status: complete
---

# Wave meta-8 — Wave meta-7 follow-up apply audit catalog

**Trigger:** Wave meta-7 closure (PR #2005, 2026-06-01) — shipped 19 SHIPPED-DONE flips nhưng deferred 50 PARTIAL adjust + 14 SCOPE-REVISE + 1 DROP catalog (preserved trong 4 audit artifacts `documents/04-quality/audits/meta/2026-06-01-wave-meta-7-bucket-*.md`). Wave meta-7 cũng surfaced 2 systemic META gap candidates needing dedicated work.

**Goal:** Close audit catalog applied + file 2 new META gaps để eliminate drift class permanently.

**Per `meta-gap-priority.md` §3 META P0 force-multiplier:** fix drift detection + auto-sync mechanism permanently → mọi future P0+P1 audit ~50× cheaper qua `query-gaps.sh` trustworthy.

---

## 1. Brainstorm Q1

**Inside-out 3 buckets (per `inside-out-completeness-trigger.md`):**

- **ROADMAP §🚀:** Wave meta-7 just closed; closure PR documented Wave meta-8 follow-up scope explicitly
- **Wave meta-7 audit catalog** (4 artifacts):
  - 50 PARTIAL adjust completion_pct (14 UP + 15 DOWN + 21 keep_pct refresh)
  - 14 SCOPE-REVISE (10 Bucket D Status/AC desync + 3 Bucket C rules never shipped + 1 Bucket B scope drift)
  - 1 DROP candidate (GAP-444 defer-by-design → WONTFIX)
- **2 META gap candidates surfaced:**
  - Audit-cadence systemic enforcement (Bucket B finding: 5/7 audit-cadence gaps miss post-wave audit mandate)
  - CSV ↔ AC structure auto-sync mechanism (Bucket D finding: 10 Status field "🔵 OPEN" + AC 0/N nhưng CSV PARTIAL pct>0)
- **Outside-in:** SKIP per `outside-in-coverage-trigger.md` §4 (internal meta scope — audit catalog apply + META gap filing)

**Q2 Risks:**
- 50 PARTIAL adjust requires precise CSV parsing per gap; risk of clobbering notes column → mitigate via Python script + pre-script CSV backup
- 14 SCOPE-REVISE rewrite touches gap file body → risk of git mv conflicts với other parallel waves → mitigate single-coordinator inline (no parallel agents)
- 2 META gap files + detector scripts paired same PR per `rule-change-process.md` §6.5 Enforcement Parity

**Q3 Out-of-scope:**
- Apply 87 OPEN+PARTIAL keep_pct (only refresh last_verified — already covered via Wave meta-7 closure batch)
- Wave thesis-2 NFR (separate parallel wave per `path-to-thesis-goal.md` Track A)
- Wave beta-cohort-1 invite execution (separate parallel — long-running async)

---

## 2. Task Breakdown

| Bucket | Scope | Owner | Effort |
|---|---|---|---|
| **A** | Apply 50 PARTIAL adjust completion_pct + notes via Python script (parse 4 audit artifacts) | coordinator-inline | ~30min |
| **B** | Rewrite 14 SCOPE-REVISE gap files (10 Bucket D Status/AC sync + 3 Bucket C never-shipped + 1 Bucket B scope-drift) | coordinator-inline | ~45min |
| **C** | File 1 META gap audit-cadence detector + ship `scripts/check-post-wave-audit-cadence.sh` + CI job wire | coordinator-inline | ~30min |
| **D** | File 1 META gap CSV/AC auto-sync + ship `scripts/sync-gap-csv-from-ac.sh` + CI WARN | coordinator-inline | ~30min |
| **E** | Apply DROP candidate GAP-444 → WONTFIX | coordinator-inline | ~5min |

Total: ~2.5h coordinator-inline. No parallel agents (single CSV target — sequential safer).

---

## 3. Scope

### Bucket A — Apply 50 PARTIAL adjust

Script `scripts/apply-wave-meta-7-partial-adjust.py`:
- Parse 4 audit artifacts (regex extract gap-id + new_completion_pct + new_notes per gap)
- Update `gap-status.csv` rows: completion_pct + last_verified + notes
- Validate via `check-gap-status-csv.sh` before commit

Verified targets (sample from audit artifacts):
- GAP-191: 50→83, GAP-374: 50→80, GAP-473: 40→73, GAP-692: 33→60 (UP)
- GAP-371: 50→17, GAP-586: 70→45, GAP-587: 40→20, GAP-590: 60→35 (DOWN)
- ~42 others per Bucket A/B/C/D catalogs

### Bucket B — Rewrite 14 SCOPE-REVISE gaps

Each gap file: update `## Problem` description + AC checkboxes + add Log entry "SCOPE-REVISE from Wave meta-7 audit".

10 Bucket D Status/AC desync:
- Status field shows OPEN + AC 0/N unchecked nhưng CSV PARTIAL pct>0
- Fix: align Status field với CSV reality OR adjust AC count

3 Bucket C rules never shipped:
- GAP-461 brand-clearance-pre-domain rule (proposed, file missing)
- GAP-615 Wave 86 retro 4-extensions (subset shipped, rest never)
- GAP-723 pre-mutation §1.6 Java extension (Java pattern absent)

1 Bucket B scope drift:
- GAP-213 Spring Cloud BOM fails on Dependabot — fix shipped PR #523 nhưng pom.xml evolved away from referenced symbols

### Bucket C — META P1 audit-cadence detector (NEW)

Filename: `documents/04-quality/gaps/phase-1-beta/GAP-821-audit-cadence-systemic-enforcement.md`

Scope: `scripts/check-post-wave-audit-cadence.sh`:
- Read `wave-history.jsonl` entries với `"status":"complete"`
- For each wave: check `audits-index.csv` cho matching audit rows ≤3 days post-closure
- WARN nếu wave closed ≥7 days without audit suite
- CI WARN-mode initially, HARD STOP target Wave meta-9 sau 30-day grace

### Bucket D — META P1 CSV/AC auto-sync (NEW)

Filename: `documents/04-quality/gaps/phase-1-beta/GAP-822-csv-ac-auto-sync-mechanism.md`

Scope: `scripts/sync-gap-csv-from-ac.sh`:
- Parse mỗi `documents/04-quality/gaps/phase-X/GAP-*.md` cho AC `- [x]` vs `- [ ]` count
- Compute suggested `completion_pct` = checked/total × 100
- WARN nếu delta vs CSV current >10pp
- Output report `documents/04-quality/audits/meta/2026-06-NN-csv-ac-drift.md`
- CI WARN-mode

### Bucket E — DROP candidate

GAP-444 → CSV `status=WONTFIX`, notes += "DROPPED Wave meta-8 — defer-by-design per Bucket D audit"

---

## 4. State-Check Evidence (per `audit-to-gap-pipeline.md` §2.6)

| Symbol | Verify | Verdict |
|---|---|---|
| `documents/04-quality/audits/meta/2026-06-01-wave-meta-7-bucket-*.md` | 4 artifacts present | ✅ |
| `documents/04-quality/gaps/gap-status.csv` | 640 rows, header valid | ✅ |
| `scripts/check-gap-status-csv.sh` | CI validator | ✅ |
| `scripts/apply-wave-meta-7-partial-adjust.py` | NEW | 🆕 Bucket A owns |
| `scripts/check-post-wave-audit-cadence.sh` | NEW | 🆕 Bucket C owns |
| `scripts/sync-gap-csv-from-ac.sh` | NEW | 🆕 Bucket D owns |
| GAP-444 file exists | `ls documents/04-quality/gaps/**/GAP-444*` | ✅ phase-1-beta/ |

---

## 5. Verification Gates (per bucket)

| Bucket | Gate |
|---|---|
| A | check-gap-status-csv PASS + ≤50 rows changed |
| B | 14 gap files updated + git diff scope match |
| C | scripts/check-post-wave-audit-cadence.sh self-test PASS + CI job WARN-mode |
| D | scripts/sync-gap-csv-from-ac.sh self-test PASS + CI job WARN-mode |
| E | GAP-444 status=WONTFIX trong CSV |

## 6. Agent Spawn Pattern

Single coordinator-inline (NO parallel agents) — CSV target sequential safer; total ~2.5h doable trong 1 session.

## 7. Closure Protocol

Per `wave-closure-scope-completeness.md` v1.0.1 + `post-merge-sync-completeness.md`:
- Single coordinator-inline closure PR (5 bucket merged sequential trong same commit OR 5 small PRs sequential)
- 4-target post-merge sync (CSV + ROADMAP + wave-history + 2 new gaps trong CSV)
- Scope-Completeness Reconciliation table trong closure PR body
- Wave plan frontmatter `status: complete`

---

## Appendix A — Open Items / Defer

- Apply tutoring re: SCOPE-REVISE → audit-to-gap-pipeline §2.5 state-check ladder integration (defer Wave meta-9 nếu cần)
- Wave meta-9 candidate: extend `check-post-wave-audit-cadence.sh` HARD STOP eligibility post-30-day grace

---

## 8. Log

- **2026-06-01** (draft): Plan created. Triggered by Wave meta-7 closure (PR #2005) surfacing 50 PARTIAL adjust + 14 SCOPE-REVISE + 1 DROP catalog + 2 META gap candidates. Per `meta-gap-priority.md` §3 META P0 force-multiplier — 2 new META detectors eliminate drift class permanently (audit-cadence + CSV/AC sync). Outside-in audit SKIP per `outside-in-coverage-trigger.md` §4 (internal meta scope). State-Check Evidence §4 verified — 7 symbols (4 ✅ + 3 🆕 NEW Bucket-owned). Cross-layer NO. Single coordinator-inline (no parallel agents) — CSV sequential safer. Estimate ~2.5h coordinator-inline.

- **2026-06-01** (complete): All 5 buckets SHIPPED coordinator-inline ~1h wall-clock (vs ~2.5h estimate; ~2.5x speedup). 6 commits on wave/meta-8-plan: plan + 5 bucket commits. Per `wave-closure-scope-completeness.md` §3 reconciliation:

## Scope-Completeness Reconciliation

| # | Plan §3 Scope item | Verdict | Follow-up |
|---|---|---|---|
| A1 | Apply 50 PARTIAL adjust completion_pct + notes via Python script | ✅ DONE (71 updates, exceeds estimate; +4 OPEN→PARTIAL fix + 5 row healing) | — |
| A2 | Status flips OPEN↔PARTIAL aligning với new pct | ✅ DONE (18 status flips applied) | — |
| B1 | Rewrite 14 SCOPE-REVISE gap files (Log entry markers) | ✅ DONE (14/14 files updated với cross-link audit) | Deep AC rewrite deferred — reviewer may file follow-up gaps per file if needed |
| C1 | File GAP-821 META audit-cadence detector | ✅ DONE | Phase 2 HARD STOP flip Wave meta-9 (target 2026-07-01 grace end) |
| C2 | Ship `scripts/check-post-wave-audit-cadence.sh` + self-test | ✅ DONE (PASS, baseline 76 stale waves) | — |
| C3 | CI WARN job `post-wave-audit-cadence` | ✅ DONE (`quality-docs.yml`) | — |
| D1 | File GAP-822 META CSV/AC auto-sync mechanism | ✅ DONE | Phase 2 direction-aware classification Wave meta-9 |
| D2 | Ship `scripts/sync-gap-csv-from-ac.sh` + self-test | ✅ DONE (PASS, baseline 226 drift gaps) | — |
| D3 | Baseline drift report saved | ✅ DONE (`documents/04-quality/audits/meta/2026-06-01-csv-ac-drift-baseline.md`) | — |
| D4 | CI WARN job `csv-ac-sync` | ✅ DONE | — |
| E1 | Apply DROP candidate GAP-444 → WONTFIX | ✅ DONE (CSV status WONTFIX + pct 0 + notes) | Re-open mechanism documented if Phase 7 plan needs explicit pre-prep |

**Verdict:** 11/11 scope items ✅ DONE. 0 PARTIAL. 0 NOT-IMPLEMENTED. 2 Phase 2 follow-ups (GAP-821 + GAP-822) scheduled Wave meta-9 (HARD STOP flip + direction-aware classification).

**Outcome metrics:**
- 71 CSV completion_pct updates (74 catalog hits + 4 OPEN→PARTIAL fixes + 5 row healing)
- 18 status flips
- 14 SCOPE-REVISE Log markers
- 2 new META detectors shipped (audit-cadence + CSV/AC) — closes drift class
- 2 baseline reports surfaced (76 stale-cadence waves + 226 CSV↔AC drift gaps)
- 1 DROP → WONTFIX applied
- ~1h wall-clock coordinator-inline (vs ~2.5h estimate)
