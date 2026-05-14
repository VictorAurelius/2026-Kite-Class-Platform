---
title: Wave Meta-Governance 1 — close 8 ecosystem-audit meta gaps (Phase 0 README cleanup + 2 parallel agents)
status: complete
created: 2026-04-28
updated: 2026-04-28
waves: [meta-gov-1]
gaps: [GAP-249, GAP-250, GAP-251, GAP-252, GAP-253, GAP-254, GAP-255, GAP-256]
---

<!-- wave-plan-completeness-exempt: Pre-Wave-76 legacy plan — predates current _TEMPLATE.md structure -->

# Wave Meta-Governance 1

**Status:** 🟢 DONE 2026-04-28 — 6 PRs merged in sequence (#607 foundation → #608 Move 1 → #609 Move 2 → #610 Sub-PR C → #611 retro → #612 GAP-255). **7/8 wave gaps DONE** (GAP-249/250/251/252/253/254/255); 1 gap **gated** (GAP-256 — eligible after GAP-255 active ≥7d per `incident-to-rule-pipeline.md` premature-rule guard, timer 2026-04-28). Total wall-clock ~2-3h. Per `gap-done-discipline.md` §2 — Status flip clean across all closing PRs (no banned phrases in any Log entry).

## 1. Goal

Close 8 meta-governance gaps surfaced by the 2026-04-28 ecosystem audit + external best-practice review (top skill repos: anthropics/skills, obra/superpowers, trailofbits/skills, awesome-skills/code-review-skill, sennaBruno/claude-skills). Tighten enforcement parity per `incident-to-rule-pipeline.md` §6.5 — every governance rule ships with a detection layer in same PR. After this wave: rule frontmatter is CI-enforced, skills convention is CI-lintable, eval fixtures pilot lands on the two highest-stakes audit skills, severity rubric adds a positive-recognition tier, and 3 critical-staleness READMEs are rewritten with freshness CI tracked as follow-up.

**Bundled in foundation PR** (per user "Rộng" scope decision 2026-04-28): Phase 0 README cleanup — root + kiteclass + kitehub READMEs rewritten / fixed, plus 2 follow-up gaps (GAP-255 freshness CI, GAP-256 read-first rule). README cleanup is meta-governance (doc quality is meta) and aligns with the wave's enforcement-parity theme.

## Phase 0 — README cleanup (foundation PR, parent does inline)

Bundled into foundation PR (not delegated to Agent A or B). Parent (this session) does these inline because they're small, creative-design tasks and disjoint from agent scope:

- **Root `README.md`** — full redesign (pixel-art KITE logo Variant B with frame, badges, sections inspired by ComposioHQ/agent-orchestrator + tirth8205/code-review-graph reference READMEs)
- **`kiteclass/README.md`** — light-fix Spring Boot 3.5.11→3.5.14, `Last Updated` 2026-02-27→2026-04-28, drop archived-architecture link
- **`kitehub/README.md`** — moderate-fix Spring Boot 3.5.11→3.5.14, `Last Updated` 2026-03-09→2026-04-28, service status table 🚧 PR 4.X→✅ Live
- **GAP-255** filed (README freshness CI — P2 Meta, paired with GAP-249/250 pattern but for docs)
- **GAP-256** filed (rule "read README before grep" for AI navigation — P2 Meta, conditional on GAP-255 active for ≥7d)

These ship in the foundation PR alongside the 6 original gap files + wave plan + ROADMAP update.

## 2. Wave-eligibility verification (Step 0)

| Q | Answer |
|---|--------|
| ≥3 sub-tasks? | ✅ YES — 6 gaps, naturally 2 disjoint clusters (rule-frontmatter + skill-conventions) |
| Disjoint files? | ✅ YES — Agent A owns `.claude/rules/*.md` + 1 new shellscript + 1 workflow job; Agent B owns `.claude/skills/**` + different shellscript + skill files |
| Self-contained build? | ✅ YES — each agent runs own shellcheck + script self-test on own files |

→ Wave-eligible. Spawn 2 agents.

## 3. Scope split (Agent A vs Agent B)

### Agent A — Move 1 (rule frontmatter discipline)

**Branch:** `feature/wave-meta-gov-1-A-rule-frontmatter`
**Gaps:** GAP-249 (bulk backfill) + GAP-250 (CI gate)
**Files (exclusive):**
- `.claude/rules/audit-to-gap-pipeline.md` (frontmatter only — Last-Reviewed + Reviewer-Approver)
- `.claude/rules/docs-folder-structure.md` (full frontmatter backfill)
- `.claude/rules/logs-format-standard.md` (full frontmatter backfill)
- `.claude/rules/mcp-first-with-fallback.md` (full frontmatter backfill)
- `.claude/rules/meta-gap-priority.md` (full frontmatter backfill)
- `.claude/rules/planning-docs-structure.md` (full frontmatter backfill)
- `.claude/rules/post-wave-audit-mandate.md` (full frontmatter backfill)
- `.claude/rules/skill-conventions.md` (full frontmatter backfill)
- `scripts/check-rule-frontmatter.sh` (NEW)
- `.github/workflows/script-quality.yml` (add `rule-frontmatter` job)
- GAP-249 + GAP-250 files (own AC checkboxes + Log entry)

**Out-of-bounds:** any `.claude/skills/**` file, `_README-skills-index.md`, any audit skill, `core/two-stage-code-review.md`, GAP-251/252/253/254.

**Acceptance:**
- All 8 rules have Version + Last-Reviewed + Reviewer-Approver + Applies-to + dated Log entry
- `scripts/check-rule-frontmatter.sh` exists, executable, shellcheck-clean
- Self-test fixture quoted in PR description (PASS on backfilled rules; FAIL on synthetic missing-Version fixture)
- New CI job `rule-frontmatter` triggers on `.claude/rules/**.md` PR changes; green on main post-merge

### Agent B — Move 2 (skills convention enforcement)

**Branch:** `feature/wave-meta-gov-1-B-skill-conventions`
**Gaps:** GAP-251 (CI lint script) + GAP-252 (index refresh) + GAP-253 (eval fixtures pilot) + GAP-254 (severity rubric)
**Files (exclusive):**
- `scripts/check-skill-conventions.sh` (NEW)
- `.claude/skills/_README-skills-index.md` (rewrite — all 27 SKILL.md listed)
- `.claude/skills/quality/business-logic-audit/data/eval-fixtures/good.md` (NEW)
- `.claude/skills/quality/business-logic-audit/data/eval-fixtures/bad-rule-not-implemented.md` (NEW)
- `.claude/skills/quality/business-logic-audit/data/eval-fixtures/edge-config-key-renamed.md` (NEW)
- `.claude/skills/quality/business-logic-audit/SKILL.md` (add `## Eval Fixtures` section linking to dir)
- `.claude/skills/quality/security-audit/data/eval-fixtures/good.md` (NEW)
- `.claude/skills/quality/security-audit/data/eval-fixtures/bad-secret-in-config.md` (NEW)
- `.claude/skills/quality/security-audit/data/eval-fixtures/edge-transitive-cve.md` (NEW)
- `.claude/skills/quality/security-audit/SKILL.md` (add `## Eval Fixtures` section)
- `.claude/skills/core/two-stage-code-review.md` (add `## Severity Rubric` section + Log entry)
- `.claude/skills/quality/skill-conventions-check/data/fixtures/good.md` (NEW — for GAP-251 self-test)
- `.claude/skills/quality/skill-conventions-check/data/fixtures/bad-no-gotchas.md` (NEW)
- `.claude/skills/quality/skill-conventions-check/data/fixtures/bad-description-style.md` (NEW)
- GAP-251 + GAP-252 + GAP-253 + GAP-254 files (own AC checkboxes + Log entries)

**Out-of-bounds:** any `.claude/rules/**` file, `scripts/check-rule-frontmatter.sh`, `.github/workflows/script-quality.yml` (Agent A's), GAP-249/250.

**Acceptance:**
- `scripts/check-skill-conventions.sh` exists, executable, shellcheck-clean
- Validates: frontmatter, description trigger style, gotchas section, body line count (≤500), folder structure
- Includes optional WARN for audit skills missing eval fixtures (GAP-253 hook)
- Includes index drift sub-check (GAP-252 hook)
- 3 fixtures committed with PASS/FAIL evidence in PR description
- `_README-skills-index.md` rewritten — all 27 SKILL.md listed; `**Updated:** 2026-04-28`
- 6 audit-eval fixtures committed (3 per skill × 2 skills) with expected-output headers
- `core/two-stage-code-review.md` has `## Severity Rubric` 5-tier section + Log entry

## 4. Sequencing & merge order

Per `feedback_parallel_agent_strategy.md` rule #5 (sequence merges by blast radius):

1. **Agent A merges first.** Smaller blast radius (8 frontmatter PATCH + 1 script + 1 workflow job). Foundation PR's `check-rule-frontmatter` CI must be green before B merges so the script-quality.yml chain stays clean.
2. **Agent B merges second.** Larger surface area (10+ new files, 1 skill body edit) but no overlap with A's files.

If A fails CI → A retries until green; B does NOT merge until A is green.

## 5. Sub-PR C (deferred — post-A+B merge)

Small follow-up PR (~5 min) wires `check-skill-conventions.sh` into CI:
- Either extend `script-quality.yml` with a `skill-conventions` job
- Or create `meta-quality.yml` if YAML conflicts with A's earlier change

Done separately to avoid YAML merge conflict between Agent A (which touches script-quality.yml first) and Agent B's CI wiring. Documented in GAP-251 §Out-of-scope.

## 6. AC for wave (gate to flip wave to DONE per `gap-done-discipline.md`)

- [ ] All 6 gaps DONE OR PARTIAL with documented follow-up
- [ ] Both agent PRs CI green pre-merge
- [ ] Both new scripts shellcheck-clean (no -S error)
- [ ] `scripts/check-rule-frontmatter.sh` passes on main post-merge
- [ ] `scripts/check-skill-conventions.sh` ready for Sub-PR C wire-up
- [ ] No banned phrases (per `gap-done-discipline.md` §2) in any DONE-flip Log entry
- [ ] ROADMAP updated by parent post-merge
- [ ] No agent touched out-of-bounds files (rule #1)

## 7. Risk / rollback

| Risk | Mitigation |
|------|------------|
| Agent A's CI fails on backfill PR (unexpected rule already non-conformant) | Script supports `--strict` flag; default WARN mode lets PR merge with warnings; flip strict on next iteration |
| Agent B's body-line-count check too strict (some grandfathered skills exceed 500) | Document exemption list in script header; flag in agent summary |
| YAML conflict on `script-quality.yml` despite sequencing | A merges first locks YAML; B's only YAML touch is Sub-PR C deferred |
| Severity rubric perceived as too long | ≤80 lines cap on `core/two-stage-code-review.md` addition; review before merge |
| Eval fixtures use unredacted real-incident data | Synthetic scaffolds only (per GAP-253 §Out-of-scope) |

**Rollback:** all changes are additive (new scripts) or PATCH-style (frontmatter). Rollback = revert 2-3 PRs in reverse merge order. No data migrations, no schema changes, no production impact.

## 8. References

- `feedback_parallel_agent_strategy.md` — 6 hard rules + worktree isolation + merge sequencing
- `feedback_wave_plan_through_pr.md` — wave plan PR-first (incident 2026-04-26 do NOT push directly to main)
- `audit-to-gap-pipeline.md` Step 2.5 — state-check verified before filing all 6 gaps
- `incident-to-rule-pipeline.md` §6.5 — Enforcement Parity Mandate (rule + detection same PR)
- `gap-done-discipline.md` §2 — DONE-flip criteria (banned-phrase scan, AC checkbox check)
- `meta-gap-priority.md` §3 — meta gaps boost Feature gaps; this wave is fully Meta-tier
- `output-review-mandate.md` §3 rows: "Rules docs (meta)", "Skills (meta)" both touched

## 9. Out of scope (this wave)

- CODEOWNERS configuration (separate gap)
- Husky pre-commit hooks for rule frontmatter (deferred, GAP-250 §Out-of-scope)
- Cascading severity rubric to 13 audit skills (file as GAP-255 follow-up)
- Retro-fitting eval fixtures to 11 other audit skills (separate follow-up)
- Auto-regenerating skills index via cron (manual + CI WARN sufficient)

## 10. Closure log

- **2026-04-28 (Follow-up GAP-255 SHIPPED #612):** README freshness CI added — `scripts/check-readme-freshness.sh` (~225 LOC, shellcheck-clean) + 5 self-test fixtures + new `readme-freshness` job in `script-quality.yml`. Baseline 4 PASS / 42 WARN / 0 FAIL across 46 READMEs. `output-review-mandate.md` bumped v1.1.1 → v1.1.2 with new §3 matrix row "README freshness" + Skills (meta) row PARTIAL → DONE flip. **2 bugs caught + fixed during dev** (regex `^\*\*Last...\*\*` rejecting project convention; YAML colon-space breaking workflow parse). 5 stale worktrees from prior waves cleaned post-merge.
- **2026-04-28 (Retro #611):** Wave plan status `active` → `complete` + ROADMAP closure log entry. Per `feedback_post_merge_doc_sync.md` — final sub-PR should bump status; if missed, ROADMAP/wave-plan drift creates phantom "active" waves.
- **2026-04-28 (DONE):** All 4 PRs merged. PR #607 (foundation: 8 gap files + wave plan + ROADMAP + 3 READMEs redesigned with pixel-art KITE logo) → PR #608 (Move 1 Agent A: 8-rule frontmatter backfill + `scripts/check-rule-frontmatter.sh` + new `rule-frontmatter` CI job; bonus catch: detector flagged `output-review-mandate.md` missing `Applies-to` field, fixed in same PR with PATCH bump 1.1.0→1.1.1) → PR #609 (Move 2 Agent B: `scripts/check-skill-conventions.sh` 456 LOC + 21-skill grandfathered-exemption list + 3 self-test fixtures + 6 audit eval fixtures + skills index refresh + 5-tier severity rubric on `two-stage-code-review.md`) → PR #610 (Sub-PR C: `skill-conventions` CI job wired, GAP-251 PARTIAL→DONE).
- **Honest deviations:** Agent B reported 21 grandfathered skills exempted from skill-conventions check (cleanup deferred, target empty list by Wave 9). Agent A bonus catch indicates GAP-249 audit list was incomplete — reinforces value of CI gate (G-3.3 force-multiplier worked first run).
- **Counts:** 90 OPEN → 92 OPEN (wave closed 6 + filed 2 follow-ups; net +2 — that's correct for a wave that surfaces follow-up work).
- **Next:** GAP-255 (README freshness CI) shippable any time as small standalone PR. GAP-256 (read-first rule) gated on GAP-255 active ≥7d per `incident-to-rule-pipeline.md` premature-rule guard.
