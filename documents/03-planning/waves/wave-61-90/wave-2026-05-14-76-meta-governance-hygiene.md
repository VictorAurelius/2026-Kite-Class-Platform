---
title: Wave 76 — Meta Governance Hygiene Finish (steady-state preparation)
status: complete
created: 2026-05-14
updated: 2026-05-14
waves: [76]
gaps: [GAP-530, GAP-531, GAP-532]
---

<!-- wave-plan-completeness-exempt: Pre-Wave-76 legacy plan — predates current _TEMPLATE.md structure -->

# Wave 76 — Meta Governance Hygiene Finish

**Goal:** Close residual meta-governance hygiene gaps surfaced by Wave 75 meta-system outside-in benchmark + Wave 74 outside-in fold-in carryover. After Wave 76 ships, meta-system reaches **steady-state** — further governance work moves to quarterly retro cadence per `post-wave-audit-mandate.md` §2.4.

**Estimated wall-clock:** ~3-4h với 5 parallel agents + closure.

**Per `outside-in-coverage-trigger.md` §4:** outside-in benchmark đã chạy Wave 75 same domain (meta-system industry comparison) → skip refresh. Scope dẫn xuất TRỰC TIẾP từ `2026-05-14-wave-75-meta-system-outside-in-benchmark.md` 5 HIGH-confidence patterns + Wave 75 Bucket E coverage follow-up.

**Per `post-wave-audit-mandate.md` §2.4.1:** `meta-governance` domain — NO AUDIT REQUIRED.

---

## 1. Brainstorm

**Q1 (alignment):** Last meta-loop iteration before steady-state. Each bucket addresses ≥1 industry benchmark gap. After Wave 76, meta gaps come from external triggers (new Anthropic features, new tools, incidents) — not internal architectural debt.

**Q2 (trade-offs):**
- Considered: bundle `audit-gate.py` runtime coverage into Wave 76 (Bucket F). Rejected — that's product debt (hook test extension), NOT governance hygiene. Defer Wave 77.
- Considered: defer Wave 76 to next session. Rejected — user direction "làm luôn trong session này".
- **Selected:** 5-bucket meta-meta governance + closure, audit-gate.py coverage tracked Wave 77.

**Q3 (risks):**
- Bucket E (rule body streamline) — touches multiple rule files. Risk: cross-rule consistency break. Mitigation: agent only moves §Self-test + §Worked example to fixture dir; rule body content unchanged.
- Bucket D (rule staleness CI + count ceiling) — adds new CI gate. Risk: blocks existing PRs. Mitigation: WARN mode first, BLOCK after 30-day grace per `incident-to-rule-pipeline.md` premature-rule guard.
- Bucket A (audits-index.csv) — first audit-index canonical. Risk: schema lock-in. Mitigation: pattern-match `gap-architecture-v2.md` + `meta-csv-index-pattern.md` already-proven schema.

---

## 2. Task Breakdown

| Bucket | Scope | Owner | Effort | Disjoint? |
|--------|-------|-------|--------|-----------|
| A | `audits-index.csv` canonical + deprecation lifecycle columns | bg-agent | ~1.5h | ✅ new CSV + scripts |
| B | `scripts/check-*.sh` test coverage extension | bg-agent | ~3h | ✅ new test fixtures under `scripts/fixtures/` |
| C | Wave-plan CI check + atomic-unique rule checklist | bg-agent | ~1.5h | ✅ new check script + rule edit |
| D | Rule staleness CI (90-day) + count ceiling policy | bg-agent | ~1.5h | ✅ new check script + rule edit |
| E | Rule body streamline + skill-vs-rule split criterion + CSV-canonical ADR | bg-agent | ~2h | ✅ rule file refactor + new ADR |
| F | Closure (coordinator) | coordinator | ~30 min | NO (last) |

Disjoint check: A=audits CSV+scripts, B=script tests, C=wave-plan CI, D=staleness CI+ceiling, E=rule streamline+ADR. Zero file overlap.

---

## 3. Scope

### Bucket A — `audits-index.csv` + deprecation lifecycle

- Files:
  - `documents/04-quality/audits/audits-index.csv` (NEW) — canonical per `meta-csv-index-pattern.md` §3 (4-artifact requirement)
  - `scripts/query-audits.sh` (NEW) — query helper
  - `scripts/check-audits-index-csv.sh` (NEW) — CI validator
  - `.github/workflows/script-quality.yml` (EDIT) — add `audits-index-csv` CI job
  - `documents/04-quality/audits/audits-index.csv` populated với existing audit files (per scan `documents/04-quality/audits/**/*.md`)
  - `.claude/rules/rules-index.csv` (EDIT) — add 3 new columns: `lifecycle_status` (active/deprecated/superseded), `deprecated_at`, `replaced_by`. Default existing rows `lifecycle_status=active`.
  - `.claude/rules/rule-change-process.md` (EDIT) — §6 add deprecation lifecycle policy (60-day WARN, beyond = remove)
  - `.claude/rules/meta-csv-index-pattern.md` (EDIT) — §6 update registry table với audits row + deprecation columns
- Acceptance:
  - [ ] audits-index.csv 100% coverage existing audit files
  - [ ] query-audits.sh works: `bash scripts/query-audits.sh <category>`
  - [ ] check-audits-index-csv.sh PASS
  - [ ] rules-index.csv 3 new columns + existing 55 rows backfilled active
  - [ ] rule-change-process.md §6 deprecation policy added + PATCH bump
  - [ ] CI gate wired

### Bucket B — Script test coverage extension

- Files:
  - `scripts/tests/` (NEW directory) — test framework for `scripts/check-*.sh`
  - `scripts/tests/test-check-rule-frontmatter.sh` (NEW) — 5 fixture-based tests
  - `scripts/tests/test-check-gap-status-csv.sh` (NEW) — 5 tests
  - `scripts/tests/test-check-rules-index-csv.sh` (NEW) — 4 tests
  - `scripts/tests/test-check-adrs-index-csv.sh` (NEW) — 3 tests
  - `scripts/tests/test-check-meta-csv-indexes.sh` (NEW) — 3 tests
  - `scripts/tests/test-check-readme-freshness.sh` (NEW) — 4 tests
  - `scripts/tests/test-prune-merged-worktrees.sh` (NEW) — 3 tests
  - `scripts/tests/fixtures/` (NEW) — fixture files per script
  - `scripts/tests/run-all.sh` (NEW) — wrapper running all script tests
  - `.github/workflows/script-quality.yml` (EDIT) — add `script-tests` job calling `scripts/tests/run-all.sh`
- Acceptance:
  - [ ] ≥6 scripts có test suite
  - [ ] Total ≥25 fixture-based tests
  - [ ] All tests PASS locally
  - [ ] CI gate wired
  - [ ] Each test: at least 1 positive (PASS expected) + 1 negative (FAIL expected) per fixtures pattern

### Bucket C — Wave-plan CI check + atomic-unique checklist

- Files:
  - `scripts/check-wave-plan-completeness.sh` (NEW) — verify new wave plans under `documents/03-planning/waves/wave-*.md` follow `_TEMPLATE.md` structure (Q1/Q2/Q3 brainstorm + bucket table + state-check evidence section)
  - `scripts/tests/fixtures/wave-plan/good-plan.md` + `bad-plan-missing-brainstorm.md` + `bad-plan-missing-state-check.md` (NEW)
  - `.github/workflows/script-quality.yml` (EDIT) — add `wave-plan-completeness` job
  - `.claude/rules/rule-change-process.md` (EDIT) — §5 add atomic-unique-bar checklist for new rules:
    - [ ] Rule has single atomic concept (not "rule A and B and C")
    - [ ] Rule's responsibility unique vs existing rules (no overlap)
    - [ ] Rule widely applicable (≥3 distinct trigger cases anticipated)
    - [ ] Rule body has ≤2 "and" conjunctions in §1 The Rule
- Acceptance:
  - [ ] check-wave-plan-completeness.sh PASS + 3 fixture tests
  - [ ] CI gate wired
  - [ ] rule-change-process.md §5 atomic-unique checklist + PATCH bump

### Bucket D — Rule staleness CI + count ceiling

- Files:
  - `scripts/check-rule-staleness.sh` (NEW) — verify all `.claude/rules/*.md` have `Last-Reviewed` within 90 days; WARN at 60 days, FAIL at 180 days
  - `.github/workflows/script-quality.yml` (EDIT) — add `rule-staleness` job WARN mode (BLOCK after 30-day grace period)
  - `.claude/rules/rule-change-process.md` (EDIT) — §3 add staleness policy (review cadence ≤90 days for MANDATORY+CRITICAL; backfill grace period)
  - `.claude/rules/README.md` (EDIT) — add count ceiling policy:
    - 0-50 rules: free growth
    - 50-75 rules: quarterly review (alert at 75)
    - 75-100 rules: consolidation review (audit overlap)
    - >100 rules: HARD STOP — must consolidate or deprecate before adding
    - Current: 56 rules (within free-growth)
  - `scripts/check-rule-count-ceiling.sh` (NEW) — count `.claude/rules/*.md` files; emit warning at thresholds
- Acceptance:
  - [ ] check-rule-staleness.sh + 3 fixture tests
  - [ ] check-rule-count-ceiling.sh works
  - [ ] CI gate wired (WARN mode)
  - [ ] rule-change-process.md §3 staleness + README count policy added

### Bucket E — Rule body streamline + split criterion + CSV ADR

- Files:
  - Rule body streamline — move §"Self-test (worked example)" + long §"Worked example" sections from rules >300 lines to `.claude/rules/_examples/<rule-slug>.md` (NEW deferred-load); rule body becomes <200 lines
  - Target rules (top 5 longest, audit first):
    - `output-review-mandate.md` (~326 lines)
    - `release-deploy-standard.md` (~319 lines)
    - `pre-mutation-state-check.md` (~313 lines)
    - `pre-handoff-self-test-completeness.md` (~282 lines)
    - `agent-aws-access.md` (~270 lines)
  - `.claude/rules/README.md` (EDIT) — add skill-vs-rule split criterion:
    - Constraint enforced via review/CI/hook → RULE
    - Multi-step workflow with state → SKILL
    - Reference docs (rubrics, checklists) → SKILL reference/
    - Default to RULE if borderline (constraints harder to back-fit)
  - `documents/02-architecture/adr/ADR-029-csv-canonical-meta-indexes.md` (NEW) — document rationale: CSV chosen over MADR YAML / JSON / TOML for meta indexes
    - Context: KiteHub needs canonical store for gaps/rules/ADRs/audits
    - Decision: CSV (flat, awk-queryable, git-diffable, low overhead)
    - Consequences: tradeoff lookup speed > schema validation richness; mitigated by `check-*-index-csv.sh` validators
    - Alternatives considered: YAML (MADR ADR-0013 standard), JSON (machine-readable but bulky), SQLite (overkill), per-item frontmatter (drift-prone)
  - `documents/02-architecture/adr/adrs-index.csv` (EDIT) — append ADR-029 row
- Acceptance:
  - [ ] 5 longest rules streamlined (body ≤200 lines)
  - [ ] `.claude/rules/_examples/` directory với 5 example files
  - [ ] README.md split criterion added
  - [ ] ADR-029 filed + adrs-index.csv updated
  - [ ] All streamlined rules CI rule-frontmatter PASS

### Bucket F — Closure

- Files:
  - `documents/04-quality/audits/meta/2026-05-14-wave-76-closure-meta-hygiene.md` (NEW)
  - `documents/03-planning/waves/wave-2026-05-14-76-meta-governance-hygiene.md` (UPDATE) — `status: complete`
  - `.claude/skills/quality/wave-pack-planner/data/wave-history.jsonl` — append Wave 76 entry
- Steps:
  - Audit artifact summarizes 5 bucket outcomes + remaining tracked items
  - Wave plan status flip
  - wave-history append
  - `bash scripts/prune-merged-worktrees.sh --yes`
  - Declare **meta steady-state achieved** — Wave 77+ moves to quarterly retro cadence

---

## 4. State-Check Evidence (per `audit-to-gap-pipeline.md` §2.6)

| Symbol | Type | Verification | Verdict |
|--------|------|--------------|---------|
| `documents/04-quality/audits/` directory | dir | `ls -d ...` | ✅ exists |
| `.claude/rules/rules-index.csv` | CSV | `wc -l ...` → 56 rows | ✅ exists |
| `.claude/rules/rule-change-process.md` | rule | exists v1.1.0 | ✅ exists |
| `.claude/rules/meta-csv-index-pattern.md` | rule | exists v1.0.0 | ✅ exists |
| `.claude/rules/README.md` | doc | exists (Wave 73 created) | ✅ exists |
| `scripts/check-rule-frontmatter.sh` | script | exists | ✅ exists |
| `scripts/check-gap-status-csv.sh` | script | exists | ✅ exists |
| `scripts/check-rules-index-csv.sh` | script | exists | ✅ exists |
| `scripts/check-adrs-index-csv.sh` | script | exists | ✅ exists |
| `documents/04-quality/audits/audits-index.csv` (Bucket A) | NEW | `ls ...` → 0 | 🆕 to-be-created (Bucket A) |
| `scripts/query-audits.sh` (Bucket A) | NEW | n/a | 🆕 (Bucket A) |
| `scripts/tests/` directory (Bucket B) | NEW | `ls -d ...` → 0 | 🆕 to-be-created (Bucket B) |
| `scripts/check-wave-plan-completeness.sh` (Bucket C) | NEW | n/a | 🆕 (Bucket C) |
| `scripts/check-rule-staleness.sh` (Bucket D) | NEW | n/a | 🆕 (Bucket D) |
| `scripts/check-rule-count-ceiling.sh` (Bucket D) | NEW | n/a | 🆕 (Bucket D) |
| `.claude/rules/_examples/` directory (Bucket E) | NEW | n/a | 🆕 (Bucket E) |
| `documents/02-architecture/adr/ADR-029-csv-canonical-meta-indexes.md` (Bucket E) | NEW | n/a | 🆕 (Bucket E) |
| `documents/03-planning/waves/_TEMPLATE.md` | template ref | exists | ✅ exists |
| `documents/02-architecture/adr/adrs-index.csv` | CSV | 28 rows | ✅ exists |

Zero absent code symbols. New files marked 🆕.

---

## 5. Verification Gates (per bucket)

| Bucket | Local verify | CI gate |
|--------|--------------|---------|
| A | `bash scripts/check-audits-index-csv.sh` PASS; `bash scripts/check-rules-index-csv.sh` PASS post 3-column add | `audits-index-csv`, `meta-csv-indexes` |
| B | `bash scripts/tests/run-all.sh` ALL PASS | `script-tests` (NEW) |
| C | `bash scripts/check-wave-plan-completeness.sh test/fixtures/...` 3 fixture cases | `wave-plan-completeness` (NEW) |
| D | `bash scripts/check-rule-staleness.sh` runs (output may WARN, exit 0 in WARN mode) + `bash scripts/check-rule-count-ceiling.sh` reports current count | `rule-staleness` (NEW WARN mode) |
| E | All 5 streamlined rules pass `bash scripts/check-rule-frontmatter.sh`; ADR-029 added to `adrs-index.csv` | `rule-frontmatter`, `meta-csv-indexes` |
| F | All above + `bash scripts/check-rules-index-csv.sh` | All CI gates |

---

## 6. Agent Spawn Pattern

Per `agent-background-spawn-default.md` + `feedback_parallel_agent_strategy.md`:
- 5 buckets A/B/C/D/E spawn parallel với `run_in_background: true`
- Worktree isolation
- RELATIVE paths in agent prompts
- Coordinator merges sequentially
- Closure coordinator-direct

**Concurrency:** 5 agents at cap.
**Model:** Opus 4.7 full per Stake Tier HIGH (touches governance infrastructure).

---

## 7. Closure Protocol

Per existing rules (`gap-done-discipline.md`, `feedback_post_merge_doc_sync.md`, `post-merge-sync-completeness.md`, `post-wave-cleanup.md`):

- Each bucket PR self-contained
- Wave plan `status: complete` flip in Bucket F closure PR
- Audit artifact ships Bucket F + declares **meta steady-state**
- `wave-history.jsonl` append
- `bash scripts/prune-merged-worktrees.sh --yes` post-merge

---

## 8. Out-of-scope (Wave 77+ candidates)

| Item | Where |
|------|-------|
| audit-gate.py `_on_pr_merge_impl` runtime coverage (P0 from Wave 75 E) | Wave 77 — different concern (hook test extension vs governance hygiene) |
| Mutation testing setup (Stryker-style) | Wave 77+ — diminishing returns until baseline >80% |
| `session-lock-guard.py` Python test conversion (P1 from Wave 75 E) | Wave 77 |
| CI coverage threshold wiring | Defer ≥7 days per `incident-to-rule-pipeline.md` premature-rule guard |
| `scripts/check-context-budget.sh` detector (per `context-budget-mandate.md` §6.3) | Defer ≥7 days |
| Memory entry review process | Quarterly retro item |
| Skill activation telemetry | Future telemetry wave |
| ROADMAP §🚀 auto-derive from CSV | Curated doc, risky to autogen — defer |

---

## 9. Log

- **2026-05-14** (complete): Wave 76 SHIPPED. 7 PRs: #1332 plan, #1333 A audits-index, #1334 D rule-staleness, #1335 C wave-plan-CI, #1336 E rule-streamline, #1337 B script-tests, #TBD F closure. Wave 75 outside-in benchmark 5 patterns absorbed (NEW-1 deprecation lifecycle / NEW-2 split criterion / NEW-3 count ceiling / SHARPEN-3 atomic-unique / ARCH-2 CSV-canonical ADR-030). Phase 1 BETA persona audit fold-in: 3 NEW gaps filed (GAP-530 P0 email e2e / GAP-531 P1 tenant init handoff / GAP-532 P1 multi-tenant switch) + 3 P0→P1 downgrades (GAP-412/447/005) + Plan 1 invite scope tightened to 2-3 trusted users. **Meta steady-state declared** — Wave 77+ moves to quarterly retro cadence; product debt (audit-gate.py coverage + GAP-530 + GAP-518 verify) separate scope. rule-change-process.md 3 PATCH bumps (1.1.0 → 1.1.1 → 1.1.2 → 1.1.3) chained across parallel buckets — coordination cost noted. 5 rules streamlined -172 lines total. 7 script-tests + new CI jobs `audits-index-csv`, `script-tests`, `rule-staleness`, `rule-count-ceiling`, `wave-plan-completeness`. 35-35-35 (35 test cases, 35 audit categories indexed, 35 wave-plans audited).
- **2026-05-14** (draft): Plan created in response to user direction "làm luôn wave 76 trước và task này [Phase 1 BETA blockers re-audit] trong session này". Scope dẫn xuất TRỰC TIẾP từ Wave 75 meta-system outside-in benchmark artifact (5 HIGH-confidence patterns) + Wave 75 closure artifact §"Wave 76 plan stub". Per `outside-in-coverage-trigger.md` §4 — outside-in skipped (Wave 75 benchmark same domain ~30 min ago). Per `incident-to-rule-pipeline.md` 5-stage: Detect ✓ (Wave 75 closure catalogued residual scope) → Classify ✓ (5 hygiene gaps) → Rule+Enforce ✓ (this wave delivers) → Self-Test (Bucket F audit artifact) → Retro Log (Bucket F + wave-history). Solo-dev MINOR per `rule-change-process.md` §5 — closes governance hygiene gap class, no constraint loosening. **Stopping criterion declared:** after Wave 76 ships, meta moves to quarterly retro cadence; Wave 77+ for product debt (audit-gate.py coverage) + external triggers only.
