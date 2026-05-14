---
title: Wave 73 — Meta Context Optimization (auto-load tier + hook coverage)
status: complete
created: 2026-05-14
updated: 2026-05-14
waves: [73]
gaps: [GAP-528]
---

<!-- wave-plan-completeness-exempt: Pre-Wave-76 legacy plan — predates current _TEMPLATE.md structure -->

# Wave 73 — Meta Context Optimization

**Goal:** Giảm base context auto-load mỗi session từ ~250k → ~100k tokens (saved ~150k, ~45% context window) qua native `paths:` frontmatter path-scoping + deterministic hook coverage cho rules không có natural path trigger.

**Trigger:** User-flagged 2026-05-14 — `/start-session` tốn ~34% context (~347k tokens) trong session bình thường. Inventory measure: 54 `.claude/rules/*.md` auto-load chiếm ~237k tokens (68% của base context). Per `meta-gap-priority.md` §3 Meta-P0 force multiplier — every future session benefits permanently.

**Estimated wall-clock:** ~5-6h with 4 parallel agents (vs ~12-15h serial).

**Per `outside-in-coverage-trigger.md` v1.0.0 check:** N/A — wave 100% internal tooling/governance scope (no user-facing surface). Outside-in (persona simulation / external benchmark) skipped per §4 row "Wave 100% internal scope".

---

## 1. Brainstorm

**Q1 (alignment):** Serves ALL future sessions across ALL personas. Force-multiplier nature = highest leverage per `meta-gap-priority.md` §3.

**Q2 (trade-offs):**
- Considered: convert all 40 MANDATORY rules → skills (rejected: 8-12h refactor, breaks `rules-index.csv` canonical pattern, less granular than `paths:` frontmatter)
- Considered: compress rules (remove §Log + §Self-test) — defer to Phase 2 (Wave 74+) as separate concern; current wave focuses on LOAD mechanism
- Considered: `claudeMdExcludes` blacklist 40 paths — rejected: hard to maintain, no auto-trigger when needed
- **Selected:** Native `paths:` frontmatter path-scoping (per Anthropic [docs](https://code.claude.com/docs/en/memory) — rules with `paths:` only load when Claude reads matching files) + hook coverage for rules với no natural path trigger

**Q3 (risks):**
- Risk: Wrong `paths:` glob → rule skipped when needed. Mitigation: per-rule review pass + native Anthropic feature is well-tested + `rules-index.csv` `path_trigger` column documents intent
- Risk: 10 MANDATORY rules cannot path-scope cleanly (vd `admin-merge-discipline` triggered by `gh pr merge --admin` Bash, not file read) → covered by Bucket B hooks (deterministic enforcement)
- Risk: User-driven hooks BLOCK false-positive → ship with override trailers per existing patterns; can downgrade BLOCK→WARN
- Risk: Bucket 0 pilot fails (Anthropic feature works differently than expected) → STOP per `release-fix-retry-budget.md` §3 (decision flow at retry #2)

---

## 2. Task Breakdown

| Bucket | Scope | Owner | Effort | Disjoint? |
|--------|-------|-------|--------|-----------|
| 0 | Pilot + index column update | bg-agent | ~1h | NO (sequential foundation) |
| A | Path-scope ~30 MANDATORY rules | 4 bg-agents | ~3-4h (parallel) | ✅ per-file disjoint |
| B | 8 hooks deterministic enforcement | 1-2 bg-agents | ~4-5h | ✅ per-hook disjoint |
| C | UserPromptSubmit dynamic inject | 1 bg-agent | ~2h | ✅ new file |
| D | Governance + meta rules | 1 bg-agent | ~2h | ✅ new files mostly |
| E | Verification + wave closure | coordinator | ~1h | NO (last) |

Disjoint check: Bucket A agents partition by rule-name alphabetical (each agent owns 6-8 specific rule files). Bucket B agents partition by hook-event (PreToolUse / PostToolUse / Stop). Buckets A/B/C/D have NO file overlap.

---

## 3. Scope

**Stake tier:** HIGH (touches `.claude/` infrastructure used every session) → model: **Opus 4.7 full** for all agents.
**Cross-layer?** NO (no FE+BE; pure `.claude/` + `documents/` scope) → SKIP Bucket 0 Foundation (cross-layer api-contract). Bucket 0 here = pilot test foundation (different concept).

> **Gap referencing convention** (per `gap-architecture-v2.md`): no specific gaps closed by this wave. New gap files MAY be created during execution (vd `GAP-525-meta-context-optimization`) — coordinator decides per `audit-to-gap-pipeline.md` §3.

| # | Bucket | Files (glob) | Spawn order |
|:-:|--------|--------------|:-----------:|
| 0 | **Foundation** | `.claude/rules/rules-index.csv` (add `path_trigger` column) + 1 pilot rule + `.claude/rules/README.md` (CREATE if missing) + `.claude/hooks/instructions-loaded-debug.sh` (optional) | MERGE FIRST |
| 1 | **A1** | 6 rules: audit-to-gap-pipeline, audit-skill-rubric-business-logic-audit, audit-skill-rubric-quality-audit, audit-skill-rubric-ui-review, audit-skill-rubric-performance-audit, audit-skill-rubric-api-contract-audit | parallel after Bucket 0 |
| 2 | **A2** | 6 rules: audit-skill-rubric-ops-readiness-audit, business-logic-review, audit-to-gap-pipeline (already in A1, swap), gap-architecture-v2, post-merge-sync-completeness, gap-done-discipline, planning-docs-structure | parallel |
| 3 | **A3** | 6 rules: docs-folder-structure, docs-only-pr-auto-merge, dev-readable-doc-language, test-artifact-format-standard, deployment-naming-convention, readme-content-discipline | parallel |
| 4 | **A4** | 6 rules: pre-launch-auth-hardening-checklist, pre-launch-secrets-hardening-checklist, pre-launch-dependency-hardening-checklist, pre-launch-infra-hardening-checklist, pre-launch-owasp-rest-hardening-checklist, pre-handoff-self-test-completeness | parallel |
| 5 | **A5** | 6 rules: ai-branding-guidelines, design-patterns, design-layer-coverage, contract-first-for-cross-layer, post-wave-audit-mandate, post-wave-cleanup, mcp-first-with-fallback | parallel |
| 6 | **B** | `.claude/hooks/audit-gate.py` (extend) + new PreToolUse hook + new Stop hook + `.claude/settings.local.json` (wire events) + tests | parallel |
| 7 | **C** | `.claude/hooks/inject-rule-digest.py` NEW + `.claude/settings.local.json` (UserPromptSubmit) + tests | parallel |
| 8 | **D** | `.claude/rules/context-budget-mandate.md` NEW + `output-review-mandate.md` (§3 row update) + memory entry + CLAUDE.md (Skills Reference table update) + `meta-csv-index-pattern.md` (rules-index column extension) | parallel |
| 9 | **E** | `documents/04-quality/audits/meta/2026-05-14-wave-73-context-budget-baseline.md` + ROADMAP update + wave plan `status:complete` flip + `wave-history.jsonl` append | LAST |

### Bucket 0 — Foundation (Pilot + rules-index extension)

- Files:
  - `.claude/rules/rules-index.csv` — add `path_trigger` column (column 7), pre-fill values for all 54 rows based on rule scope
  - `.claude/rules/aws-sg-description-ascii.md` — pilot rule, add `paths:` frontmatter `["infrastructure/**/*.tf"]`
  - `.claude/rules/README.md` — CREATE if missing, document tier convention (CRITICAL auto-load, MANDATORY path-scoped or hook-covered)
  - `.claude/hooks/instructions-loaded-debug.sh` — OPTIONAL helper to log which instructions load per session (per Anthropic `InstructionsLoaded` hook event)
- Tests: manual fresh session start → check pilot rule does NOT appear in load list unless touching `.tf` file
- Acceptance:
  - rules-index.csv `path_trigger` column populated for all 54 rules
  - Pilot rule path-scoped successfully (verified empirically)
  - README.md documents tier convention
- Spawn order: **MERGE FIRST** (other buckets depend on rules-index.csv updated schema + pilot validation)

### Bucket A — Path-scope 30 MANDATORY rules

(5 sub-buckets A1-A5, each takes ~6 rules)

- Per rule:
  - Read rule file (especially §Applies-to in frontmatter + scope sections)
  - Add `paths:` frontmatter (preserve other frontmatter fields verbatim)
  - Cross-reference `rules-index.csv` `path_trigger` column for consistency
  - Verify rule still parseable (frontmatter YAML valid)
- Tests: fresh session start (per Bucket E) measures auto-load list shrinkage
- Acceptance:
  - ~30 MANDATORY rules have `paths:` frontmatter
  - 10 MANDATORY rules without natural path-trigger documented in README + flagged for Bucket B hook coverage
  - All paths use RELATIVE patterns from repo root
  - rules-index.csv `path_trigger` column matches each rule's actual `paths:` value

**Path-trigger mapping (proposal — agents finalize per rule):**

| Rule | Proposed `paths:` glob |
|------|------------------------|
| audit-to-gap-pipeline | `documents/04-quality/gaps/**/*.md` + `documents/04-quality/audits/**` |
| audit-skill-rubric-business-logic-audit | `documents/04-quality/audits/business-logic/**` |
| audit-skill-rubric-quality-audit | `documents/04-quality/audits/quality/**` |
| audit-skill-rubric-ui-review | `documents/04-quality/audits/ui/**` |
| audit-skill-rubric-performance-audit | `documents/04-quality/audits/performance/**` |
| audit-skill-rubric-api-contract-audit | `documents/04-quality/audits/api-contract/**` |
| audit-skill-rubric-ops-readiness-audit | `documents/04-quality/audits/ops-readiness/**` |
| business-logic-review | `documents/01-business/**/rules.md` |
| gap-architecture-v2 | `documents/04-quality/gaps/**` + `**/*.csv` |
| post-merge-sync-completeness | `documents/04-quality/gaps/**` |
| gap-done-discipline | `documents/04-quality/gaps/**` |
| planning-docs-structure | `documents/03-planning/**/*.md` |
| docs-folder-structure | `documents/**/*.md` (broad — review if too wide) |
| docs-only-pr-auto-merge | (CRITICAL) — keep auto-load |
| dev-readable-doc-language | `documents/**/*.md` |
| test-artifact-format-standard | `documents/05-guides/operations/acceptance-tests/**` + `**/*.csv` |
| deployment-naming-convention | `documents/05-guides/{deploy,operations,account-prep}/**` + `infrastructure/**` |
| readme-content-discipline | `README.md` (root only) |
| pre-launch-auth-hardening-checklist | `kitehub*/src/**/auth/**` + `*.git-tag` (no glob; trigger via Bucket B hook for tag) |
| pre-launch-secrets-hardening-checklist | `application*.yml` + `docker-compose*.yml` + `infrastructure/**` |
| pre-launch-dependency-hardening-checklist | `pom.xml` + `package.json` + `pnpm-lock.yaml` |
| pre-launch-infra-hardening-checklist | `Dockerfile*` + `infrastructure/**` |
| pre-launch-owasp-rest-hardening-checklist | `**/*Controller.java` + `application*.yml` |
| pre-handoff-self-test-completeness | (CRITICAL) — keep auto-load |
| ai-branding-guidelines | `kitehub-branding/**` |
| design-patterns | `**/*.java` + `**/*.tsx` + `**/*.ts` |
| design-layer-coverage | `documents/02-architecture/design-system/**` + `ui_kits/**` |
| contract-first-for-cross-layer | `documents/03-planning/waves/**` |
| post-wave-audit-mandate | `documents/03-planning/waves/**` + `documents/04-quality/audits/**` |
| post-wave-cleanup | `documents/03-planning/waves/**` |
| mcp-first-with-fallback | (CRITICAL) — keep auto-load |

### Bucket B — 8 Hooks (deterministic enforcement)

- Files:
  - `.claude/hooks/audit-gate.py` — extend with new AUDIT_RULES entries + new PreToolUse rules
  - `.claude/hooks/pre-tool-guard.py` — NEW PreToolUse hook (matcher Bash/Edit/Write)
  - `.claude/hooks/stop-handoff-check.py` — NEW Stop hook for `pre-handoff-self-test-completeness`
  - `.claude/settings.local.json` — wire PreToolUse + Stop events
  - `.claude/hooks/tests/` — fixtures + asserts per hook
- 8 hooks (1 hook can cover multiple rules):
  1. `admin-merge-discipline` — PreToolUse Bash, match `gh pr merge.*--admin`, BLOCK + verify trailer
  2. `agent-aws-access` Tier 3 — PreToolUse Bash, match `aws (create-|delete-|put-|update-|modify-|terminate-)`, BLOCK + override trailer
  3. `aws-sg-description-ascii` — PreToolUse Edit/Write, grep non-ASCII in `description` fields when path matches `infrastructure/**/*.tf`
  4. `terraform-apply-retry-reconfirm` — PreToolUse Bash, detect consecutive `terraform apply` < 5min apart
  5. `concurrent-production-mutation-ops` — PreToolUse Bash, check `gh run list --status in_progress` for overlap
  6. `post-merge-sync-completeness` — PostToolUse Bash, on `git commit\|gh pr merge` check CSV row sync
  7. `release-fix-retry-budget` — PostToolUse Bash, scan recent commits for retry pattern
  8. `pre-handoff-self-test-completeness` — Stop hook, scan response for "DONE" claim without §2 checklist
- Tests: each hook fixture + assert (positive + negative + override case)
- Acceptance:
  - 8 hooks wired with tests passing
  - Override trailer mechanisms documented in each rule (already exist for most)
  - No false-positive on existing PR patterns

### Bucket C — UserPromptSubmit dynamic inject

- Files:
  - `.claude/hooks/inject-rule-digest.py` — NEW UserPromptSubmit hook
  - `.claude/settings.local.json` — wire UserPromptSubmit event
  - `.claude/hooks/data/keyword-rule-map.json` — keyword → rule path mapping
- Logic:
  - Scan user prompt for keywords (`audit`, `deploy`, `merge`, `gap closure`, `release tag`, `terraform`, `AWS`, etc.)
  - Read matching rule file (snippet, ~first 30 lines = §1 The Rule + §3 Anti-patterns)
  - Inject via `additionalContext` field
  - Try/catch fallback to no inject if errors (silent degradation)
- Tests: prompt fixtures with various keywords → assert correct rule injected
- Acceptance:
  - Hook fires on UserPromptSubmit, injects relevant snippet
  - Zero injection if no keyword match (saves tokens default case)
  - Fallback graceful on errors

### Bucket D — Governance + meta rules

- Files:
  - `.claude/rules/context-budget-mandate.md` — NEW rule
  - `.claude/rules/output-review-mandate.md` — §3 add row "Context budget" tracking review standard
  - `.claude/rules/meta-csv-index-pattern.md` — update §4 schema convention if `path_trigger` column adds new pattern
  - `~/.claude/projects/.../memory/feedback_meta_context_optimization.md` — memory entry
  - `~/.claude/projects/.../memory/MEMORY.md` — index entry
  - `CLAUDE.md` — update §Skills Reference to mention rules tier
  - `.claude/rules/rules-index.csv` — add new rule row
- New rule `context-budget-mandate.md` content:
  - **Rule:** Base auto-load context per session MUST be <100k tokens. Rules adding to base load MUST use `paths:` frontmatter unless justified.
  - Per-check: `paths:` present? If absent, justification trong rule §"Auto-load justification" section?
  - Enforcement: PR template checkbox + reviewer-checklist + `bash scripts/check-context-budget.sh` (future detector deferred per `incident-to-rule-pipeline.md` premature-rule guard)
  - Worked self-test: this very wave's measurements
- Acceptance:
  - New rule shipped with built-in enforcement per `rule-change-process.md` §6.5
  - All cross-link updates present
  - Memory entry indexed in MEMORY.md

### Bucket E — Verification + wave closure

- Files:
  - `documents/04-quality/audits/meta/2026-05-14-wave-73-context-budget-baseline.md` — NEW (CREATE folder if missing)
  - `documents/04-quality/gaps/ROADMAP.md` — §🚀 Next Action update
  - `documents/03-planning/waves/wave-2026-05-14-73-meta-context-optimization.md` — frontmatter `status: complete`
  - `.claude/skills/quality/wave-pack-planner/data/wave-history.jsonl` — append entry per Rule 15
- Steps:
  - User runs fresh `/start-session` → reports new base context tokens
  - Compare load list before vs after (use `InstructionsLoaded` hook log if Bucket 0 enabled it, else manual diff)
  - Document baseline in audit file
  - Wave closure PR with all sync targets per `post-merge-sync-completeness.md` §2
- Acceptance:
  - Base context dropped from ~250k → ≤120k (target: ~100k)
  - All sync targets updated (CSV, ROADMAP, wave-history, MEMORY)
  - `bash scripts/prune-merged-worktrees.sh --yes` ran per `post-wave-cleanup.md`

---

## 4. State-Check Evidence (per `audit-to-gap-pipeline.md` §2.6)

| Symbol | Type | Verification command | Evidence | Verdict |
|--------|------|----------------------|----------|---------|
| `.claude/rules/*.md` 54 files | rule files | `ls .claude/rules/*.md \| wc -l` | 54 | ✅ exists |
| `paths:` frontmatter feature | Anthropic native feature | `https://code.claude.com/docs/en/memory` §"Path-specific rules" | docs confirmed 2026-05-14 | ✅ native |
| `claudeMdExcludes` setting | Anthropic native | docs §"Exclude specific CLAUDE.md files" | confirmed | ✅ native (alternative not used in this wave) |
| `InstructionsLoaded` hook event | Anthropic native | docs §"Troubleshoot memory issues" Tip block | confirmed | ✅ native |
| `.claude/rules/rules-index.csv` | CSV index | `ls .claude/rules/rules-index.csv` | exists, 54 rows + header | ✅ exists |
| `path_trigger` column in rules-index.csv | new column | `head -3 .claude/rules/rules-index.csv \| awk -F, '{print NF}'` | currently 6 cols | 🆕 to-be-created (Bucket 0) |
| `.claude/hooks/audit-gate.py` | hook script | `wc -l .claude/hooks/audit-gate.py` | 779 lines | ✅ exists (extend in Bucket B) |
| `.claude/hooks/pre-tool-guard.py` | new hook | `ls .claude/hooks/pre-tool-guard.py` | 0 matches | 🆕 to-be-created (Bucket B) |
| `.claude/hooks/stop-handoff-check.py` | new hook | `ls .claude/hooks/stop-handoff-check.py` | 0 matches | 🆕 to-be-created (Bucket B) |
| `.claude/hooks/inject-rule-digest.py` | new hook | `ls .claude/hooks/inject-rule-digest.py` | 0 matches | 🆕 to-be-created (Bucket C) |
| `.claude/rules/context-budget-mandate.md` | new rule | `ls .claude/rules/context-budget-mandate.md` | 0 matches | 🆕 to-be-created (Bucket D) |
| `.claude/rules/README.md` | folder README | `ls .claude/rules/README.md` | 0 matches | 🆕 to-be-created (Bucket 0) |
| `documents/04-quality/audits/meta/` | new audit subfolder | `ls -d documents/04-quality/audits/meta` | 0 | 🆕 to-be-created (Bucket E) |
| `feedback_meta_context_optimization.md` | new memory entry | `ls ~/.claude/projects/-home-nguyenvankiet-projects-2026-Kite-Class-Platform/memory/feedback_meta_context_optimization.md` | 0 (memory dir empty) | 🆕 to-be-created (Bucket D) |

Banned shortcuts: zero `| head` truncation in evidence — full enumeration above.

---

## 5. Verification Gates (per bucket)

| Bucket | Local verify command | CI gate |
|--------|---------------------|---------|
| 0 | `python3 -c "import csv; r=list(csv.reader(open('.claude/rules/rules-index.csv'))); assert len(r[0])==7; print('OK', len(r)-1, 'rows')"` | `meta-csv-indexes` (existing) |
| A1-A5 | Per agent: `python3 -c "import yaml; [yaml.safe_load(open(f).read().split('---')[1]) for f in <rule-files>]"` (frontmatter parses) | `rule-frontmatter` (existing) |
| B | `python3 -m pytest .claude/hooks/tests/` + `python3 .claude/hooks/audit-gate.py < /dev/null` (smoke) | `script-quality` (existing audit-gate test) |
| C | `python3 .claude/hooks/inject-rule-digest.py < /tmp/test-prompt.json` | none yet (new hook) |
| D | `bash scripts/check-rule-frontmatter.sh` + `bash scripts/check-meta-csv-indexes.sh` | `rule-frontmatter`, `meta-csv-indexes` |
| E | Manual: fresh `/start-session` from user, report new context size | none |

---

## 6. Agent Spawn Pattern

Per `feedback_parallel_agent_strategy.md` + `agent-background-spawn-default.md`:
- All buckets spawned with `run_in_background: true`
- Worktree isolation (`isolation: worktree`) for parallel safety
- RELATIVE paths in agent prompts per `feedback_worktree_absolute_path_contamination.md`
- Coordinator merges sequentially after all background completions
- Order: Bucket 0 ships + merges → THEN spawn A1-A5 + B + C + D in parallel (7 agents) → THEN Bucket E coordinator

**Concurrency cap:** 5 agents max per `feedback_parallel_agent_strategy.md` rule #9. Stagger A1-A5 + B in 2 waves if needed (A1-A5 first wave, B+C+D second wave).

**Model selection:** All buckets use Opus 4.7 full per Stake Tier HIGH (touches `.claude/` infrastructure).

---

## 7. Closure Protocol

Per `gap-done-discipline.md` + `feedback_post_merge_doc_sync.md` + `post-merge-sync-completeness.md` + `post-wave-cleanup.md`:

- Each bucket PR updates affected files + tests
- ROADMAP §🚀 Next Action updated in closure PR (Bucket E)
- Wave plan frontmatter `status: complete` flip in closure PR
- `wave-history.jsonl` append in closure PR (Rule 15 enforcement)
- `gap-status.csv` row updates if any gaps closed/created
- MEMORY.md index entry update for new memory entry
- Run `bash scripts/prune-merged-worktrees.sh --yes` before closure PR
- Per `release-fix-retry-budget.md`: if Bucket 0 pilot fails 2nd retry, STOP wave and re-evaluate

---

## 8. Risk Matrix (paired w/ §1 Q3)

| Risk | Likelihood | Impact | Mitigation | Bucket owner |
|------|:----------:|:------:|------------|:------------:|
| `paths:` glob too narrow → rule never loads | LOW | MEDIUM | rules-index.csv `path_trigger` review pass + Bucket E verification compares load list | A1-A5 |
| `paths:` glob too broad → no savings | LOW | LOW | per-rule scope analysis (frontmatter §Applies-to) | A1-A5 |
| Hook BLOCK false-positive | MEDIUM | MEDIUM | Override trailers per existing rule patterns; downgrade BLOCK→WARN if pattern emerges | B |
| UserPromptSubmit hook breaks sessions | LOW | HIGH | Try/catch fallback to no inject; comprehensive fixture tests | C |
| Rule conflict (path-scoped rule overrides CRITICAL) | LOW | MEDIUM | Per `rule-change-process.md` §5 cross-check during review | A1-A5 + D |
| Bucket 0 pilot fails | LOW | HIGH | STOP per `release-fix-retry-budget.md` §3; revert Bucket 0 commit | 0 |
| Anthropic feature works differently than expected | LOW | HIGH | Bucket 0 IS the empirical test; abort wave if doesn't behave as docs claim | 0 |

---

## 9. Out-of-scope (track separately)

| Item | Where |
|------|-------|
| Compress rules (remove §Log + §Self-test from CRITICAL rules to save more) | Future Wave 74 (Phase 2) |
| Convert rules → skills (heavier refactor) | Defer indefinitely; only if `paths:` proves insufficient |
| Migrate `.claude/skills/` similar tier optimization | Out of scope (skills load on-demand already per Anthropic docs §"For task-specific instructions...use skills") |
| Optimize `documents/03-planning/session-handoffs/` auto-load behavior | Not auto-loaded; out of scope |

---

## 10. Log

- **2026-05-14** (miss-fix): User-flagged `/start-session` vẫn load ~182k thay vì target ~88k. Investigation surfaced 13 MANDATORY rules + folder README **không có row trong §3 Scope path-trigger table** (line 105-139) → vẫn auto-load base context dù scope rule có natural path trigger. Root cause: §3 Scope Bucket E mandate "Manual: fresh /start-session from user, report new context size" KHÔNG chạy trước closure → vi phạm `pre-handoff-self-test-completeness.md` §2.2. Fix PR ships: 11 MANDATORY rules path-scoped (agent-aws-access, agent-background-spawn-default, context-budget-mandate, logs-format-standard, meta-csv-index-pattern, production-env-config-registry, session-currentdate-check, skill-conventions, terraform-apply-retry-reconfirm, terraform-partial-backend-public-repo, third-party-platform-automation-discovery) + folder README + 2 cross-cut rules (agent-action-bias, mcp-first-with-fallback) given `## Auto-load justification` section per `context-budget-mandate.md` §3.2 + `rules-index.csv` `path_trigger` column synced for 13 rows + audit artifact `documents/04-quality/audits/meta/2026-05-14-wave-73-miss-fix-baseline.md` ships the missed Bucket E measurement. Estimated additional savings: ~33.6k tokens × every session × forever. Reviewer: @nguyenvankiet (solo-dev PATCH self-approves per `rule-change-process.md` §5 across 14 rule files — all additive frontmatter / section, no constraint change). Per `incident-to-rule-pipeline.md` 5-stage applied to this miss: Detect ✓ (user-flagged) → Classify ✓ (no rule miss; instead Bucket E self-test miss) → Rule+Enforce ✓ (fix shipped paired with audit artifact filling missed baseline measurement per §6.5 Enforcement Parity) → Self-Test ✓ (post-merge user runs `/start-session` → confirm projection ~142-162k actual) → Retro Log ✓ (this entry).
- **2026-05-14** (complete): Wave 73 SHIPPED. Bucket 0 (PR #1309) + Bucket A1-A5 (#1310/1311/1312/1313/1314) + Bucket C UserPromptSubmit hook (PR #1315) + Bucket D context-budget-mandate (PR #1316) all merged to main. ~30 MANDATORY rules path-scoped via Anthropic native `paths:` frontmatter. Estimated base context savings: ~150k tokens (~237k → ~87k rules alone). Bucket B (8 deterministic enforcement hooks) deferred to GAP-528 — agent worktree self-deadlocked by wiring `pre-tool-guard.py` in `settings.local.json` BEFORE script existed; lesson-learned codified as "stub-first pattern" trong GAP-528 acceptance criteria. Bucket E closure: this entry + ROADMAP entry + wave-history.jsonl + worktree pruning. UserPromptSubmit hook (Bucket C) live-verified this session — injected rule digests for `release-deploy-standard`, `admin-merge-discipline`, `outside-in-coverage-trigger`, `release-fix-retry-budget`, `docs-only-pr-auto-merge` keywords correctly.
- **2026-05-14** (draft): Plan created. Per `incident-to-rule-pipeline.md` 5-stage Detect (user-flagged ~34% context per /start-session) → Classify (no rule mandates context budget; force-multiplier per `meta-gap-priority.md`) → Rule+Enforce ✓ (this wave + Bucket D `context-budget-mandate.md` paired same wave per `rule-change-process.md` §6.5) → Self-Test (Bucket E baseline measurement) → Retro Log (this entry + Bucket E closure update). Per `outside-in-coverage-trigger.md` v1.0.0 — internal scope, outside-in N/A documented in §1. Per `meta-gap-priority.md` Meta-P0 priority — every future session benefits permanently.
