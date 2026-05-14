---
title: Wave 75 Closure — Meta Hook Coverage Finish
status: complete
created: 2026-05-14
phase: post-wave-75
wave: 75
gaps_closed: [GAP-529]
prs: ["#1325", "#1326", "#1327", "#1328", "#1329", "#1330"]
---

# Wave 75 Closure — Meta Hook Coverage Finish

## Scope

Đóng Wave 75 — Meta Hook Coverage Finish. 5 bucket song song + 1 outside-in benchmark + 1 closure (this audit). User direction: "hoàn chỉnh meta triệt để".

## Wave outcome — 7 artifacts shipped

| # | Item | PR / SHA | Result |
|---|---|---|---|
| Plan | Wave 75 plan | #1325 → `6ce63005` | 5-bucket scope locked |
| **A** | GAP-529 fix — `_has_trailer_in_pr` per-PR helper | #1330 → `164cb977` | 4 callers migrated, 25 pre-tool-guard tests (+5 regression), GAP-529 DONE |
| **B** | Hook Review skill v1.1.0 | #1328 → `f4cddd50` | 13-point rubric (+5 new) + 15 EC entries (+6 new) + 5 sharpened |
| **C** | Hook ordering empirical | #1326 → `9ad7308c` | **SAFE — close concern.** Outside-in benchmark Wave 74 INACCURATE (pre-tool-guard PreToolUse vs audit-gate PostToolUse). Real multi-hook same-event = audit-gate + post-tool-guard PostToolUse, Anthropic docs "parallel + dedup" → safe. |
| **D** | Concurrent race investigation | #1327 → `19070899` | **SAFE @ N=5.** State-writes catalogued; pr-logs/*.json disjoint per agent; other hooks stateless or graceful. |
| **E** | Coverage baseline | #1329 → `8b341833` | **50.2% overall.** audit-gate.py **28.2%** (LOWEST — `_on_pr_merge_impl` 0% covered runtime path). 5 follow-up gaps proposed. |
| **Meta benchmark** | Outside-in OSS comparison | (artifact only — no PR) | `documents/04-quality/audits/meta/2026-05-14-wave-75-meta-system-outside-in-benchmark.md`. **Verdict: 67% industry coverage FULL + 17% PARTIAL + 33% MISS. NOT over-engineered. Missing pruning hygiene at scale.** |
| **F** | Closure (this PR) | (TBD) | audit artifact + Wave 76 plan stub + wave-history + cleanup |

## Test counts post-Wave-75

| Hook | Post-W74 tests | Post-W75 tests | Delta |
|---|---:|---:|---:|
| `audit-gate.py` | 23 | 23 | 0 |
| `pre-tool-guard.py` | 21 | 25 | +4 (GAP-529 regression) |
| `inject-rule-digest.py` | 20 | 20 | 0 |
| `post-tool-guard.py` | 16 | 16 | 0 |
| `stop-handoff-check.py` | 13 | 13 | 0 |
| `test-hook-ordering.py` (NEW) | 0 | 11 | +11 |
| `test-concurrent-fire.py` (NEW) | 0 | 7 | +7 |
| **Total** | **93** | **115** | **+22** |

## Critical findings — outside-in benchmark feedback loop

**2 of 3 Wave 74 outside-in CRITICAL claims EMPIRICALLY REFUTED:**

| Wave 74 outside-in claim | Wave 75 empirical | Confidence-weighting lesson |
|---|---|---|
| Hook ordering = CRITICAL untested risk | SAFE — wrong premise (different events listed); real multi-hook same-event is parallel-safe per Anthropic | Outside-in finding **MAY BE WRONG** — verify empirically before treating as P0 |
| Concurrent race = CRITICAL untested risk | SAFE — pr-logs files disjoint per agent; other hooks stateless | Same lesson |
| Coverage measurement = CRITICAL gap | REAL — 50.2% overall, audit-gate 28% is genuine concern | Some outside-in findings ARE real; can't blanket dismiss |

**Codified lesson:** Apply Wave 75 C+D "verify-before-treat-as-P0" discipline to ALL outside-in findings. Confidence-weight by empirical test results.

## Outside-in benchmark — META-system findings

(From `2026-05-14-wave-75-meta-system-outside-in-benchmark.md`)

### 5 HIGH-confidence NEW patterns to adopt (Wave 76 candidates)

1. **NEW-1: Rule deprecation lifecycle** — add `status` + `replaced_by` + `deprecated_at` columns to `rules-index.csv`. 60-day warn, beyond = remove. Source: Clippy deprecation lane + OpenLogic 2026.
2. **NEW-2: Skill-vs-rule split criterion** — codified in `.claude/rules/README.md` (constraint = rule, multi-step workflow = skill, default rule if borderline).
3. **NEW-3: Pruning hygiene + count ceiling** — 50-75 quarterly review, 75-100 consolidation review, >100 hard stop. KiteHub at 56 — actionable next quarter.
4. **SHARPEN-3: Atomic-unique bar in rule-change-process.md** — checklist (atomic, unique, widely applicable, ≤2 "and"s). Source: ESLint rule guidelines.
5. **ARCH-2: ADR documenting CSV-canonical choice** — MADR ADR-0013 chose YAML; KiteHub chose CSV (contrarian). Document rationale.

### 3 streamline candidates

1. **STREAMLINE-1: Rule body length** — industry rules <100 lines; KiteHub often 200-500+. Move §Self-Test + worked examples to fixture files. **Already in user-confirmed Wave 76 scope.**
2. STREAMLINE-2: §Relationship cap at 5 entries.
3. STREAMLINE-3: Memory entry → rule body migration (continue pattern).

### Where KiteHub is AHEAD of industry (defend, don't retreat)

1. Path-scope empirical measurement (Wave 73 ~75% savings) — published savings data unique
2. 5-stage `incident-to-rule-pipeline` as codified process — Google SRE has culture, not formal pipeline
3. Same-PR enforcement parity (§6.5) — ESLint accepts rules with no impl; KiteHub mandates same-PR
4. Stage 4 self-test on ORIGINATING incident — stronger empirical grounding

## Wave 76 plan stub (per user direction "full 5-bucket meta-meta")

Plan to be drafted in dedicated wave-76 plan PR. Tentative scope (5 bucket — at concurrency cap):

| Bucket | Scope (combined original + benchmark fold-in) | Effort |
|---|---|---|
| **A** | `audits-index.csv` canonical + NEW-1 deprecation lifecycle columns (both CSV schema work) | ~1.5h |
| **B** | `scripts/check-*.sh` test coverage extension (per Wave 75 E follow-up) | ~3h |
| **C** | Wave-plan CI check + SHARPEN-3 atomic-unique checklist | ~1.5h |
| **D** | Rule staleness enforcement (90-day Last-Reviewed CI check) + NEW-3 count ceiling policy | ~1.5h |
| **E** | Rule body streamline (>300-line rules) + NEW-2 split criterion + ARCH-2 CSV-canonical ADR | ~2h |
| **F** | Closure (coordinator) | ~30 min |

**NOT in Wave 76 scope** (separate concern, file Wave 77 or dedicated gap):
- audit-gate.py `_on_pr_merge_impl` runtime coverage (P0 from Wave 75 E follow-ups) — separate "hook test extension" wave, not "meta-meta governance"

## Closure protocol completed

- [x] Audit artifact (this file)
- [x] GAP-529 status flip → DONE in CSV (Bucket A done)
- [x] Hook trailer scope bug fixed (Bucket A — 4 callers migrated)
- [x] Outside-in 2/3 CRITICAL claims refuted (Bucket C + D)
- [x] Coverage baseline measured (Bucket E)
- [x] Meta-system industry benchmark filed (outside-in)
- [ ] Wave 75 plan frontmatter `status: complete`
- [ ] `wave-history.jsonl` append
- [ ] `bash scripts/prune-merged-worktrees.sh --yes` cleanup
- [ ] PR with all closure + Wave 76 plan-stub references

## Recommendations

1. **Ship this closure PR** — docs-only, auto-merge per `docs-only-pr-auto-merge.md` after CI green
2. **Wave 76 plan PR** — dedicated next session OR continuation; absorb 5 buckets per user direction + benchmark fold-in (5 NEW patterns)
3. **audit-gate.py coverage** — separate from Wave 76; file as P0 standalone gap OR Wave 77 — different concern than governance hygiene
4. **Lesson codified**: Outside-in findings need empirical verification before treating as P0 (Wave 75 C+D refuted 2/3) — fold into `hook-review/reference/rubric-checklist.md` point 10 (true-positive + true-negative fixture parity) implicitly OR explicit new rubric point

## References

- Wave plan: `documents/03-planning/waves/wave-2026-05-14-75-meta-finish.md`
- Wave 75 audit artifacts:
  - Bucket C hook ordering: `2026-05-14-wave-75-hook-ordering-empirical.md`
  - Bucket D concurrent race: `2026-05-14-wave-75-concurrent-race.md`
  - Bucket E coverage baseline: `2026-05-14-wave-75-hook-coverage-baseline.md`
  - Meta benchmark: `2026-05-14-wave-75-meta-system-outside-in-benchmark.md`
- GAP-529 (closed Bucket A): `documents/04-quality/gaps/GAP-529-hook-trailer-scope-bug.md`
- Predecessor Wave 74 audit: `2026-05-14-wave-74-closure-coverage-audit.md`
