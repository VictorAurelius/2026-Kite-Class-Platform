---
title: Wave 75 — Meta Hook Coverage Finish (close residual from Wave 74 + GAP-529)
status: complete
created: 2026-05-14
updated: 2026-05-14
waves: [75]
gaps: [GAP-529]
---

<!-- wave-plan-completeness-exempt: Pre-Wave-76 legacy plan — predates current _TEMPLATE.md structure -->

# Wave 75 — Meta Hook Coverage Finish

**Goal:** Close all residual meta-governance hook coverage gaps surfaced by Wave 74 outside-in benchmark + GAP-529. After Wave 75 ships, meta-governance for hooks is COMPLETE (rule + skill + tests + ordering + race + coverage measurement all addressed).

**Estimated wall-clock:** ~2h với 5 parallel agents (~1.5h critical path + ~30 min closure).

**Per `outside-in-coverage-trigger.md` §4 row "User đã trải qua outside-in ≤30 ngày":** outside-in benchmark đã ship Wave 74 (~1h trước) — không cần refresh; Wave 75 scope dẫn xuất TRỰC TIẾP từ outside-in artifact `2026-05-14-wave-74-outside-in-benchmark.md`.

**Per `post-wave-audit-mandate.md` §2.4.1:** `meta-governance` domain — NO AUDIT REQUIRED.

---

## 1. Brainstorm

**Q1 (alignment):** Final meta-fix wave. Each bucket addresses ONE outside-in finding class. After Wave 75, hooks coverage saturates against the standard discovered Wave 74.

**Q2 (trade-offs):** Considered narrowing to A+B+C only (defer D race + E coverage). Rejected per user direction "hoàn chỉnh meta triệt để". 5 buckets parallel within max-5 cap acceptable.

**Q3 (risks):** Bucket A code change touches both `pre-tool-guard.py` + `audit-gate.py` — moderate scope; existing test suites catch regression. Bucket C/D empirical tests may surface real production bugs → file follow-up Wave 76 if needed. Bucket E `coverage.py` setup may discover untestable code in `audit-gate.py` 779-line orchestrator (`_on_pr_merge_impl`) — document, don't refactor in this wave.

---

## 2. Task Breakdown

| Bucket | Scope | Owner | Effort | Disjoint? |
|--------|-------|-------|--------|-----------|
| A | GAP-529 fix: `_has_trailer_in_pr(pr_num, trailer)` shared helper + migrate callers | bg-agent | ~1h | ✅ touches hook source code only |
| B | Skill extension: fold 5 HIGH-conf + 5 sharpening from outside-in artifact into `hook-review/reference/` | bg-agent | ~30 min | ✅ docs only |
| C | Hook ordering empirical test + audit artifact | bg-agent | ~45 min | ✅ test fixtures + audit doc |
| D | Concurrent race investigation + audit artifact (focus: `audit-gate.py` writes `pr-logs/*.json`) | bg-agent | ~45 min | ✅ test fixtures + audit doc |
| E | `coverage.py` setup + baseline measurement of 6 hooks | bg-agent | ~45 min | ✅ tooling + baseline artifact |
| F | Closure (coordinator-direct) — audit artifact + wave-history + cleanup | coordinator | ~15 min | NO (last) |

Disjoint check: A=hook source, B=skill docs, C=test+ordering-audit doc, D=test+race-audit doc, E=tooling+coverage-baseline doc. Zero overlap.

---

## 3. Scope

### Bucket A — GAP-529 fix (per-PR trailer scoping)

- Files:
  - `.claude/hooks/pre-tool-guard.py` — replace `_has_trailer(trailer)` with `_has_trailer_in_pr(pr_num, trailer)`; extract PR number from `gh pr merge <N>` regex; fallback to legacy `_commit_body()` if no PR num
  - `.claude/hooks/audit-gate.py` — migrate `has_audit_override` + `has_domain_milestone_defer` to per-PR scoping (call `gh pr view <N> --json body` via existing `gh_run` helper)
  - `.claude/hooks/tests/test-pre-tool-guard.py` — refactor existing `test_admin_merge_blocked` to stub `_commit_body` / `_has_trailer_in_pr` (no HEAD env dep); upgrade bonus test to assert NOW-correct behavior
  - `.claude/rules/admin-merge-discipline.md` §4 — clarify "trailer applied to PR body" (current text says "SQUASH commit" — same intent, different mechanism)
- Acceptance:
  - [ ] `_has_trailer_in_pr` shared helper added
  - [ ] All 3 pre-tool-guard rule functions migrated (admin-merge + aws-tier3 + terraform-retry)
  - [ ] audit-gate.py 2 trailer detector functions migrated
  - [ ] Existing 21 pre-tool-guard tests + 23 audit-gate tests still PASS post-migration
  - [ ] New regression test: clean PR body → BLOCK; trailer in PR body → ALLOW (independent of HEAD)
  - [ ] GAP-529 status flip → DONE in CSV
  - [ ] rule §4 admin-merge-discipline.md PATCH bump

### Bucket B — Skill extension

Fold-in từ `2026-05-14-wave-74-outside-in-benchmark.md` §Recommendations Nhóm 1+2:

- Files:
  - `.claude/skills/quality/hook-review/reference/rubric-checklist.md` — extend with 5 new sections:
    - Point 9: Exit code matrix + `exit 1` trap callout
    - Point 10: True-positive + true-negative fixture parity per BLOCK condition (ESLint mandate)
    - Point 11: Stdin malformed JSON handling test
    - Point 12: JSON stdout contract schema compliance
    - Point 13: Hardware-pinned performance baseline (cold-start vs steady-state)
  - Sharpen existing 5 points với enumerations:
    - Point 1 (event matcher): enumerate 4 Anthropic event types
    - Point 2 (BLOCK vs WARN): exit-code-to-decision mapping table
    - Point 4 (fail-safe): enumerate 6 cases (missing dep, malformed input, timeout, permission, env unset, working dir)
    - Point 5 (wiring): settings precedence chain (project → project.local → user)
    - Test annotation: Semgrep-style `# ruleid:` / `# ok:` / `# todo:` fixture conventions
  - `.claude/skills/quality/hook-review/reference/edge-case-catalog.md` — add EC entries for new points (EC-010 through EC-015)
  - `.claude/skills/quality/hook-review/SKILL.md` — bump version to 1.1.0; note rubric grew 8 → 13 points
- Acceptance:
  - [ ] 5 new rubric points added
  - [ ] 5 existing points sharpened với enumerations
  - [ ] 6 new EC entries
  - [ ] SKILL.md frontmatter version 1.1.0
  - [ ] `bash scripts/check-skill-conventions.sh` PASS

### Bucket C — Hook ordering empirical test

- Files:
  - `documents/04-quality/audits/meta/2026-05-14-wave-75-hook-ordering-empirical.md` — investigation artifact
  - `.claude/hooks/tests/test-hook-ordering.py` (NEW) — synthetic test verifying behavior when 2 hooks listen Bash event
- Investigation:
  - Read `settings.local.json` — `pre-tool-guard.py` + `audit-gate.py` both listen `Bash` PreToolUse?
  - Empirical: trigger a Bash command, log hook fire order (timestamps + hook responses)
  - Document Anthropic behavior: serial? parallel? short-circuit on first BLOCK?
  - If a hook mutate state (audit-gate.py logs to pr-logs) before another hook checks (pre-tool-guard.py checks command) → ordering matters
- Acceptance:
  - [ ] Empirical findings documented (Anthropic behavior verified)
  - [ ] Test fixture demonstrates ordering behavior
  - [ ] Recommendations: if ordering matters → ship as Wave 76 fix candidate; if not → close concern
  - [ ] Cross-link from `hook-review/reference/rubric-checklist.md` if new rubric point warranted

### Bucket D — Concurrent race investigation

- Files:
  - `documents/04-quality/audits/meta/2026-05-14-wave-75-concurrent-race.md` — investigation artifact
  - `.claude/hooks/tests/test-concurrent-fire.py` (NEW) — synthetic test invoking hook N times in parallel
- Investigation:
  - Identify state-mutating hooks: `audit-gate.py` writes `documents/03-planning/pr-logs/PR-NNN.json`, possibly other state files
  - `inject-rule-digest.py` may cache; verify
  - Wave-pack agent pattern: N agents tool-call concurrently → N hook fires; if all writing same file → race
  - Simulate: parallel subprocess invocation; verify last-write-wins / lock / corruption
- Acceptance:
  - [ ] State-mutating writes catalogued per hook
  - [ ] Empirical race test executed; results documented
  - [ ] If race confirmed → file follow-up Wave 76 gap với fix recommendation (file locks / atomic writes / per-PR-namespaced paths)
  - [ ] If race NOT confirmed (e.g., hooks already idempotent) → close concern

### Bucket E — Coverage measurement setup

- Files:
  - `.coveragerc` (NEW) hoặc extend existing — config for `coverage.py` tracking `.claude/hooks/*.py`
  - `.claude/hooks/tests/run-coverage.sh` (NEW) — wrapper script running 4 test suites under coverage
  - `documents/04-quality/audits/meta/2026-05-14-wave-75-hook-coverage-baseline.md` — baseline measurement artifact
- Steps:
  - Install `coverage` if not present (verify Ruff/CI already includes)
  - Run 4 hook test suites under coverage
  - Report line + branch coverage per hook
  - Identify untested branches (especially in `audit-gate.py` 779 lines)
  - Baseline metric: total coverage % + per-hook %
- Acceptance:
  - [ ] `.coveragerc` config valid
  - [ ] All 4 test suites run under coverage without error
  - [ ] Baseline report shows per-hook line + branch %
  - [ ] Top 5 untested branches identified (file follow-up Wave 76 gap if % is too low)
  - [ ] Cross-link from `hook-review/reference/rubric-checklist.md` point 13 (perf baseline) → coverage baseline

### Bucket F — Closure

- Files:
  - `documents/04-quality/audits/meta/2026-05-14-wave-75-closure-meta-finish.md` (NEW)
  - `documents/03-planning/waves/wave-2026-05-14-75-meta-finish.md` (UPDATE) — `status: complete`
  - `.claude/skills/quality/wave-pack-planner/data/wave-history.jsonl` — append Wave 75 entry
  - `documents/04-quality/gaps/gap-status.csv` — GAP-529 status update if A merged
- Steps:
  - Audit artifact summarizes 5 bucket outcomes + any Wave 76 follow-up gaps
  - Wave plan status flip
  - wave-history append
  - `bash scripts/prune-merged-worktrees.sh --yes`

---

## 4. State-Check Evidence (per `audit-to-gap-pipeline.md` §2.6)

| Symbol | Type | Verification | Verdict |
|--------|------|--------------|---------|
| `.claude/hooks/pre-tool-guard.py` `_has_trailer`, `_commit_body` | function refs | `grep -n "_has_trailer\|_commit_body" .claude/hooks/pre-tool-guard.py` → lines 35-50 | ✅ exists |
| `.claude/hooks/audit-gate.py` `has_audit_override`, `has_domain_milestone_defer` | function refs | `grep -n "def has_audit_override\|def has_domain_milestone_defer" .claude/hooks/audit-gate.py` → lines 426, 445 | ✅ exists |
| `.claude/hooks/tests/test-pre-tool-guard.py` `test_admin_merge_blocked` | test ref | line 38 | ✅ exists |
| GAP-529 file | gap | `documents/04-quality/gaps/GAP-529-hook-trailer-scope-bug.md` | ✅ exists (filed Wave 74) |
| Outside-in benchmark artifact | reference | `documents/04-quality/audits/meta/2026-05-14-wave-74-outside-in-benchmark.md` | ✅ exists |
| `.claude/skills/quality/hook-review/reference/rubric-checklist.md` | extend target | shipped Wave 74 #1322 | ✅ exists |
| `.claude/hooks/tests/test-hook-ordering.py` (Bucket C) | NEW | `ls ...` → 0 | 🆕 to-be-created (Bucket C) |
| `.claude/hooks/tests/test-concurrent-fire.py` (Bucket D) | NEW | `ls ...` → 0 | 🆕 to-be-created (Bucket D) |
| `.coveragerc` (Bucket E) | NEW | `ls .coveragerc` → 0 | 🆕 to-be-created (Bucket E) |
| 5 audit artifacts under `documents/04-quality/audits/meta/2026-05-14-wave-75-*` | NEW | n/a | 🆕 to-be-created |
| `gh pr view` CLI | tool | `gh pr view --help` | ✅ available |
| `coverage` Python package | dep | check | 🟡 verify pre-Bucket E |

Zero absent code symbols. New files marked 🆕.

---

## 5. Verification Gates (per bucket)

| Bucket | Local verify | CI gate |
|--------|--------------|---------|
| A | All 4 hook test suites PASS post-migration; `ruff check .claude/hooks/` clean; `bash scripts/check-gap-status-csv.sh` (GAP-529 row update) | `script-quality` Ruff + `gap-status-csv` |
| B | `bash scripts/check-skill-conventions.sh` PASS | `skill-conventions` |
| C | `python3 .claude/hooks/tests/test-hook-ordering.py` (synthetic test PASS) | `script-quality` Ruff |
| D | `python3 .claude/hooks/tests/test-concurrent-fire.py` PASS | `script-quality` Ruff |
| E | `bash .claude/hooks/tests/run-coverage.sh` produces report; `.coveragerc` valid syntax | `script-quality` Ruff |
| F | All above + `bash scripts/check-rule-frontmatter.sh` | All CI gates |

---

## 6. Agent Spawn Pattern

Per `agent-background-spawn-default.md` + `feedback_parallel_agent_strategy.md`:
- 5 buckets A/B/C/D/E spawn parallel với `run_in_background: true`
- Worktree isolation (`isolation: worktree`)
- RELATIVE paths in agent prompts
- Coordinator merges sequentially after all 5 complete
- Closure (Bucket F) coordinator-direct

**Concurrency:** 5 agents (at cap per parallel-strategy rule #9).
**Model:** All Opus 4.7 full per Stake Tier HIGH (touches `.claude/hooks/` + governance docs).

---

## 7. Closure Protocol

Per `gap-done-discipline.md` + `feedback_post_merge_doc_sync.md` + `post-merge-sync-completeness.md` + `post-wave-cleanup.md`:

- Each bucket PR self-contained
- Bucket A migration: GAP-529 flip to DONE in CSV (canonical per `gap-architecture-v2.md`)
- Wave plan `status: complete` flip in Bucket F closure PR
- Audit artifact ships Bucket F
- `wave-history.jsonl` append
- `bash scripts/prune-merged-worktrees.sh --yes` post-merge

---

## 8. Out-of-scope (track separately)

| Item | Where |
|------|-------|
| Mutation testing (Stryker-style) | Future Wave 76+ if coverage % too low |
| Hook framework abstraction (pre-commit / Lefthook adapter) | Future major refactor |
| Refactor `audit-gate.py` 779-line orchestrator into modules | Defer indefinitely — only if Bucket E surfaces untestable code |
| Cross-hook integration test (real Claude Code lifecycle) | Future — current scope = unit test |
| Per-hook performance budget enforcement | Future — needs telemetry |

---

## 9. Log

- **2026-05-14** (complete): Wave 75 SHIPPED. 6 PRs merged: #1325 plan, #1326 Bucket C ordering (SAFE), #1327 Bucket D race (SAFE), #1328 Bucket B skill v1.1.0, #1329 Bucket E coverage 50.2%, #1330 Bucket A GAP-529 fix. Meta outside-in benchmark artifact filed (67% industry coverage). Closure (this PR): audit artifact `2026-05-14-wave-75-closure-meta-finish.md` + Wave 76 plan stub + wave-history append. GAP-529 closed. 2 of 3 Wave 74 outside-in CRITICAL claims empirically REFUTED (ordering + race SAFE); coverage 28% confirmed real concern. Codified lesson: outside-in findings need empirical verification before P0 treatment. Wave 76 candidate scope: 5-bucket meta-meta governance hygiene (audits-index + script tests + wave-plan CI + rule staleness + rule body streamline) + benchmark fold-in (deprecation lifecycle + split criterion + count ceiling + atomic-unique + CSV-canonical ADR). audit-gate.py runtime coverage P0 follow-up filed separately (Wave 77 candidate, different concern).
- **2026-05-14** (draft): Plan created in response to user direction "vậy là meta vẫn chưa fix xong... làm luôn trong session này, mục tiêu là hoàn chỉnh meta" after Wave 74 closure. Scope dẫn xuất TRỰC TIẾP từ Wave 74 outside-in benchmark artifact + GAP-529 + Bucket C empirical findings. Per `outside-in-coverage-trigger.md` §4 row "User đã trải qua outside-in ≤30 ngày" — outside-in skipped (just ran 1h earlier same domain). Per `incident-to-rule-pipeline.md` 5-stage: Detect ✓ (Wave 74 audit catalogued residual scope) → Classify ✓ (5 distinct findings) → Rule+Enforce ✓ (this wave delivers) → Self-Test (Bucket F audit artifact) → Retro Log (Bucket F + wave-history). Solo-dev MINOR per `rule-change-process.md` §5 — extends meta-governance coverage, no constraint loosening.
