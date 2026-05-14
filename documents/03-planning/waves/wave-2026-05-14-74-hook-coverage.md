---
title: Wave 74 — Hook Coverage (rubric + audit-gate tests + edge cases)
status: complete
created: 2026-05-14
updated: 2026-05-14
waves: [74]
gaps: [GAP-529]
---

<!-- wave-plan-completeness-exempt: Pre-Wave-76 legacy plan — predates current _TEMPLATE.md structure -->

# Wave 74 — Hook Coverage

**Goal:** Close coverage gap for 6 hooks shipped Wave 73 B (PR #1318). Currently: 779-line `audit-gate.py` has 0 unit tests; `post-tool-guard.py` + `stop-handoff-check.py` có ít tests (3-7/rule); KHÔNG có meta-rubric review hook. User-flagged 2026-05-14: "có meta để review hook chưa? tôi lo lắng về coverage để giảm risk."

**Estimated wall-clock:** ~2-3h với 3 parallel agents (vs ~6-8h serial).

**Per `outside-in-coverage-trigger.md` v1.0.0 check:** N/A — meta-governance internal scope, no user-facing surface. Outside-in skipped per §4 row "Wave 100% internal scope".

**Per `post-wave-audit-mandate.md` §2.4.1 domain registry:** `meta-governance` — NO AUDIT REQUIRED (governance is its own quality gate).

---

## 1. Brainstorm

**Q1 (alignment):** Hook reliability serves ALL future PRs that rely on deterministic enforcement (admin-merge / concurrent-mutation / terraform-retry / aws-tier3 / audit-cadence / docs-sync). Force-multiplier per `meta-gap-priority.md` §3.

**Q2 (trade-offs):**
- Considered: skip skill, just add tests (rejected: no rubric → future hook edits inconsistent quality)
- Considered: defer to follow-up gaps (rejected: gaps stale fast post-wave per user observation 2026-05-14)
- Considered: heavy formal coverage measurement với `coverage.py` (deferred Phase 2 — overkill cho v1)
- **Selected:** 3 parallel buckets — rubric skill + audit-gate tests + edge-case tests cho sparse-coverage hooks

**Q3 (risks):**
- Risk: New tests catch false positives → BLOCK chain reveals hook bugs. Mitigation: each test có positive + negative cases; CI green required before merge.
- Risk: Skill rubric too prescriptive → constrains future hook design. Mitigation: rubric là checklist không phải mandate; override mechanism per `rule-change-process.md` §8.
- Risk: audit-gate.py refactor needed to make testable. Mitigation: only add tests, no refactor in this wave; if untestable surfaces → file follow-up gap.

---

## 2. Task Breakdown

| Bucket | Scope | Owner | Effort | Disjoint? |
|--------|-------|-------|--------|-----------|
| A | `.claude/skills/quality/hook-review/SKILL.md` + 8-point rubric + reference docs | bg-agent A | ~45 min | ✅ new files only |
| B | `audit-gate.py` test suite (`test-audit-gate.py` ~15-20 tests) | bg-agent B | ~1.5h | ✅ new test file only |
| C | Edge-case tests for `post-tool-guard.py` + `stop-handoff-check.py` (+ ~5 tests each) | bg-agent C | ~45 min | ✅ existing test files only |
| D | Closure: wave plan `status:complete` + coverage audit artifact + wave-history append | coordinator | ~15 min | NO (last) |

Disjoint check: agents touch DISJOINT file sets (A=`skills/quality/hook-review/**`; B=`hooks/tests/test-audit-gate.py` NEW; C=`hooks/tests/test-post-tool-guard.py` + `test-stop-handoff-check.py` EXISTING). Zero overlap.

---

## 3. Scope

| # | Bucket | Files (glob) | Spawn order |
|:-:|--------|--------------|:-----------:|
| 1 | **A** Hook Review Skill | `.claude/skills/quality/hook-review/SKILL.md` (NEW) + `reference/rubric-checklist.md` (NEW) + `reference/edge-case-catalog.md` (NEW) + index entry in `.claude/skills/_README-skills-index.md` | parallel |
| 2 | **B** audit-gate.py tests | `.claude/hooks/tests/test-audit-gate.py` (NEW) + fixtures under `tests/fixtures/audit-gate/` | parallel |
| 3 | **C** Edge tests | `.claude/hooks/tests/test-post-tool-guard.py` (EXTEND) + `test-stop-handoff-check.py` (EXTEND) | parallel |
| 4 | **D** Closure | wave plan + `documents/04-quality/audits/meta/2026-05-14-wave-74-hook-coverage.md` (NEW) + `wave-history.jsonl` append | LAST |

### Bucket A — Hook Review Skill

**8-point rubric** (proposed scope cho agent — agent finalize):

1. **Event matcher correctness** — `matcher: "..."` regex covers đủ tool calls intended; không miss Edit/Write khi PostToolUse rule mong đợi
2. **BLOCK vs WARN gradient** — khi nào dùng `decision="block"` (hard fail, user must override) vs systemMessage warn (advisory)
3. **Override trailer recognition** — regex parser handles trailing whitespace + multi-line body + case sensitivity edge cases
4. **Fail-safe degradation** — hook crash / exception / missing file → silent allow (KHÔNG silently block legitimate work)
5. **`settings.local.json` wiring verification** — hook file tồn tại nhưng KHÔNG wired ≡ 0% enforcement
6. **False-positive testing** — commit message containing rule's banned keyword (e.g. "gh pr merge --admin" trong PR body) PHẢI NOT trigger BLOCK
7. **Idempotency** — hook chạy 2 lần liên tiếp cùng input → cùng output (no side effects)
8. **Performance budget** — hook hoàn thành <500ms cho typical input (PreToolUse blocks user action)

Skill structure per `skill-conventions.md`:
- `SKILL.md` <100 lines với description trigger keywords ("hook review", "review hook", "kiểm tra hook")
- `reference/rubric-checklist.md` — full 8-point detailed checklist
- `reference/edge-case-catalog.md` — known edge cases (commit body containing keywords, trailing whitespace trailer, etc.)
- Gotchas section: integration với `settings.local.json`, JSON I/O format, environment variables

### Bucket B — audit-gate.py test suite

Test targets (priority order):

1. `is_docs_only(files)` — 4 tests: pure docs / pure code / mixed / empty
2. `has_audit_override(pr, info)` — 4 tests: trailer present / absent / malformed / multi-line body
3. `has_domain_milestone_defer(pr, files)` — 3 tests: valid domain + path match / unknown domain (typo) / path outside domain
4. `has_domain_milestone_audit(pr)` — 2 tests: trailer with reports / trailer without reports
5. `check_gap_doc_drift(pr, info, files)` — 2 tests: gap status flip with doc sync / without
6. `compute_score(checklist)` — 2 tests: all-checked / half-checked
7. `detect_pr_merge(cmd)` — 2 tests: positive / negative pattern
8. AUDIT_RULES pattern matching — 2 tests: file match triggers required audit / no match silent

Total ~21 tests targeting key decision logic. Coverage: focus on branches where hook makes DECISION (BLOCK vs ALLOW vs WARN).

Fixtures under `.claude/hooks/tests/fixtures/audit-gate/`:
- `pr-info-docs-only.json`, `pr-info-mixed.json`, `pr-info-code-only.json`
- `commit-body-with-trailer.txt`, `commit-body-no-trailer.txt`
- `gap-file-flipped.md`, `gap-file-open.md`

### Bucket C — Edge tests cho post-tool-guard + stop-handoff-check

**post-tool-guard.py** — add ~5 tests:
1. `check_status_csv_sync` — gap status MD changed but CSV NOT changed → WARN
2. CSV changed but status field unchanged → no WARN (idempotent re-sync)
3. Multiple gap files changed in one commit → all caught
4. `check_release_retry_pattern` — 3 consecutive same-gate retries → WARN
5. Mixed gate retries (different gates per attempt) → no WARN

**stop-handoff-check.py** — add ~5 tests:
1. Transcript có "DONE" + §2 checklist → no WARN
2. Transcript có "DONE" no §2 checklist → WARN
3. Transcript có "PARTIAL" no §2 checklist → no WARN (only DONE flips need checklist)
4. Multi-gap DONE flips → checklist required per gap OR shared
5. Empty transcript → silent allow (no error)

### Bucket D — Closure

- Audit artifact: `documents/04-quality/audits/meta/2026-05-14-wave-74-hook-coverage.md` với before/after coverage table + per-hook test count + risk assessment delta
- Wave plan frontmatter `status: complete`
- `wave-history.jsonl` append per `.claude/skills/quality/wave-pack-planner/data/` schema
- `bash scripts/prune-merged-worktrees.sh --yes` per `post-wave-cleanup.md`

---

## 4. State-Check Evidence (per `audit-to-gap-pipeline.md` §2.6)

| Symbol | Type | Verification | Verdict |
|--------|------|----------------------|---------|
| `.claude/hooks/audit-gate.py` (779 lines) | hook script | `wc -l .claude/hooks/audit-gate.py` → 779 | ✅ exists |
| `.claude/hooks/post-tool-guard.py` (164 lines) | hook script | `wc -l ...` → 164 | ✅ exists |
| `.claude/hooks/stop-handoff-check.py` (142 lines) | hook script | `wc -l ...` → 142 | ✅ exists |
| `.claude/hooks/tests/` folder | test dir | `ls -d ...` | ✅ exists |
| `.claude/hooks/tests/fixtures/` folder | fixture dir | `ls -d ...` | ✅ exists |
| `.claude/skills/quality/hook-review/SKILL.md` | new skill | `ls ...` → 0 | 🆕 to-be-created (Bucket A) |
| `.claude/skills/quality/hook-review/reference/rubric-checklist.md` | new ref doc | n/a | 🆕 (Bucket A) |
| `.claude/hooks/tests/test-audit-gate.py` | new test file | `ls ...` → 0 | 🆕 to-be-created (Bucket B) |
| `documents/04-quality/audits/meta/2026-05-14-wave-74-hook-coverage.md` | audit artifact | n/a | 🆕 to-be-created (Bucket D) |
| `AUDIT_RULES` constant in audit-gate.py | symbol | `grep -n "^AUDIT_RULES" .claude/hooks/audit-gate.py` → line 22 | ✅ exists |
| `is_docs_only`, `has_audit_override`, `has_domain_milestone_defer`, `compute_score` functions | symbols | grep'd in audit-gate.py at lines 406/426/445/357 | ✅ exists |

Zero absent symbols. Bucket A/B/C/D scope all verified or marked 🆕 to-be-created.

---

## 5. Verification Gates (per bucket)

| Bucket | Local verify command | CI gate |
|--------|---------------------|---------|
| A | `bash scripts/check-skill-conventions.sh` (existing CI script) | `skill-conventions` |
| B | `python3 .claude/hooks/tests/test-audit-gate.py` (all tests PASS) | `script-quality` Ruff + manual run trong CI |
| C | `python3 .claude/hooks/tests/test-post-tool-guard.py && python3 .claude/hooks/tests/test-stop-handoff-check.py` | `script-quality` |
| D | All above + `bash scripts/check-rules-index-csv.sh` + `bash scripts/check-rule-frontmatter.sh` | All CI gates |

---

## 6. Agent Spawn Pattern

Per `agent-background-spawn-default.md` + `feedback_parallel_agent_strategy.md`:
- 3 buckets A/B/C spawn parallel với `run_in_background: true`
- Worktree isolation (`isolation: worktree`)
- RELATIVE paths in agent prompts
- Coordinator merges sequentially after all 3 complete
- Closure (Bucket D) coordinator-direct (no agent)

**Concurrency:** 3 agents OK (< 5 cap per parallel-strategy rule #9).

**Model selection:** All buckets Opus 4.7 full (touches `.claude/` infrastructure — HIGH stake).

---

## 7. Closure Protocol

Per `gap-done-discipline.md` + `feedback_post_merge_doc_sync.md` + `post-merge-sync-completeness.md` + `post-wave-cleanup.md`:

- Each bucket PR self-contained với tests passing
- Wave plan `status: complete` flip in Bucket D closure PR
- Audit artifact ships Bucket D
- `wave-history.jsonl` append
- `bash scripts/prune-merged-worktrees.sh --yes` post-merge

---

## 8. Out-of-scope (track separately)

| Item | Where |
|------|-------|
| Formal `coverage.py` measurement of hook line/branch % | Future Wave 75+ (Phase 2) — overkill cho v1 |
| Refactor `audit-gate.py` into smaller modules | Future — only if testing surfaces untestable code |
| End-to-end integration test (hook fired by real Claude Code) | Future — current scope = unit test |
| Track production false-positive/false-negative rates | Future — needs telemetry plumbing |

---

## 9. Log

- **2026-05-14** (complete): Wave 74 SHIPPED. 4 PRs merged: #1320 plan, #1321 Bucket B audit-gate.py 23 tests, #1322 Bucket A hook-review skill + 8-point rubric + 9 EC entries, #1323 Bucket C 17 edge tests. Outside-in benchmark artifact filed surfacing 32% coverage gap vs industry (5 HIGH-conf additions + 5 sharpening + 3 CRITICAL classes). Bucket C empirical discovered hook trailer scope bug → GAP-529 P1 filed. Closure (this PR): audit artifact + GAP-529 + plan flip + wave-history append + worktree prune. Outside-in audit was added MID-wave after user flagged miss of `outside-in-coverage-trigger.md` §3 — fold-in strategy: HIGH-conf Wave 75 quick follow-up; CRITICAL classes Wave 75 dedicated gaps. Hook trailer scope bug severity P1 (not P0) confirmed post-verify: main HEAD `45efea57` does NOT have stale trailer → hook BLOCKS correctly trên main; bug per-branch-derivation only. Per `incident-to-rule-pipeline.md` 5-stage applied to both rule miss + hook bug.
- **2026-05-14** (draft): Plan created in response to user-flagged miss after Wave 73 closure: "có meta để review hook chưa? tôi hiểu là đã test hook rồi? nhưng tôi lo lắng về coverage của nó để giảm risk." Per `incident-to-rule-pipeline.md` 5-stage applied: Detect ✓ (user-flagged) → Classify ✓ (no rule covers; `script-review-checklist.md` generic không cover hook-specific concerns; audit-gate.py 779 lines 0 unit tests) → Rule+Enforce (this wave delivers skill + tests; rule itself optional Phase 2) → Self-Test (Bucket D audit artifact) → Retro Log (Bucket D + wave-history). Solo-dev MINOR per `rule-change-process.md` §5 — adds previously-uncovered hook governance + closes risk gap.
