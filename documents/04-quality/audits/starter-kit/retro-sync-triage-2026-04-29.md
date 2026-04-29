---
title: Starter-Kit Retro-Sync Triage Report
status: triage
audit_date: 2026-04-29
source: GAP-195 Phase 2a (Wave Meta Phase-2 Cleanup, Cluster 7 Agent C)
script: scripts/starter-kit-diff.sh
remote_repo: github.com/VictorAurelius/claude-starter-kit
remote_version: 2.2.0
local_kit_version: none (no .claude/starter-kit/ mirror in this project)
followup_gap: GAP-262
runbook: documents/05-guides/starter-kit-retro-sync.md
---

# Starter-Kit Retro-Sync Triage Report — 2026-04-29

> Phase 2a deliverable for GAP-195. Phase 2b (cross-repo upstream PR work) tracked in GAP-262.
> This is the **triage** report — it identifies what should land upstream. The actual upstream PR(s) are out of scope here.

---

## 1. Summary

`scripts/starter-kit-diff.sh` ran successfully on 2026-04-29 against remote `github.com/VictorAurelius/claude-starter-kit@main` (VERSION 2.2.0). The local project has **no `.claude/starter-kit/` mirror folder** — comparison was made directly between the project's `.claude/rules/` + `.claude/skills/` and the remote's matching trees. As a result, "MODIFIED" detection fired on zero files (the remote tree at `.claude/rules/` does not overlap local rule names; same for skills). All deltas surface as either NEW (local) or PROJECT-SPECIFIC.

### Bucket counts

| Bucket | Count | Notes |
|--------|------:|-------|
| 🆕 NEW (local) — candidate for upstream | **110** | 9 rules + 101 skill files |
| 🔒 PROJECT-SPECIFIC — omit | **48** | Files referencing kitehub/kiteclass/ai-branding markers |
| ✏️ MODIFIED (both sides differ) | **0** | No overlap by filename — remote tree is independent skeleton |
| 🆕 NEW (remote) — adopt back? | **0** | Script's `find` emits empty list because remote rule/skill names don't collide with local |

### Filter notes

The diff script's heuristic for "project-specific" (filename or first 30 lines mention `kitehub`/`kiteclass`/`ai-branding`/`smart-quiz`) had **false negatives** on a handful of files that ARE project-specific despite not matching the regex. Most notable manual reclassifications captured in §3 below.

### Total candidates after manual reclassification

- **Truly generic and ready for upstream import:** ~85 files (estimate — see §2 Top-N + §4 open questions for the boundary cases)
- **Recommended for first upstream PR (Top-N):** **9 files** (rules-only, conservative scope) — see §2

---

## 2. Recommended Top-N for First Upstream PR (Phase 2b)

The first retro-sync PR should be **rules-only** to keep review tractable per the runbook §6 ("one PR per category"). Skills follow in PR 2 + PR 3 once rules land. The 9 rules below all pass the 4-question triage checklist; they are also the meta-governance layer that future projects most benefit from inheriting.

| # | File path | Bucket | 4-Q checklist (Generalize / Stable / No project paths / Battle-tested) | Priority | Rationale |
|---|-----------|--------|:---------------------------------------------------:|:--------:|-----------|
| 1 | `.claude/rules/rule-change-process.md` | NEW (local) | ✅ ✅ ✅ ✅ | 🔴 P0 | The "rule about rules". Without it, every other rule below would lack a governance backbone in a downstream project. Battle-tested ≥10 versions/log entries. Pure meta — zero project references. |
| 2 | `.claude/rules/output-review-mandate.md` | NEW (local) | ✅ ✅ ⚠️ ✅ | 🔴 P0 | Master mandate behind every review standard. Some §3 matrix rows mention specific GAP IDs — must scrub to `<example: GAP-XXX>` placeholders before upstream PR. v1.1.4, ≥10 log entries, ~190+ days battle-tested. |
| 3 | `.claude/rules/skill-conventions.md` | NEW (local) | ✅ ✅ ✅ ✅ | 🔴 P0 | Already lives in remote conceptually (sync rule + version-management section); local version has Anthropic-internal best-practices expanded. Lighter scrub: `kiteclass` mentions in UI Audit Workflow section need rewrite to project-agnostic example. |
| 4 | `.claude/rules/audit-to-gap-pipeline.md` | NEW (local) | ✅ ✅ ✅ ✅ | 🔴 P0 | Generic pipeline (Issue → Gap Check → Gap File → Memory → Fix PR). Step 2.5 state-check pattern is portable. v1.0.0 + Step 2.5 added 2026-04-20 — well-tested. |
| 5 | `.claude/rules/meta-gap-priority.md` | NEW (local) | ✅ ✅ ✅ ✅ | 🟠 P1 | "Meta gaps before feature gaps" force-multiplier rule — generalizes perfectly. Examples cite specific GAP IDs but rationale is universal. Light scrub of example table. |
| 6 | `.claude/rules/gap-done-discipline.md` | NEW (local) | ✅ ✅ ✅ ✅ | 🟠 P1 | "DONE means done" rule — banned-phrase list + PARTIAL exit ramp + override trailer. Closes universal silent-deferral failure mode. v1.0 paired with check-docs.sh Rule 13 — but Rule 13 implementation lives in skills, separate PR. |
| 7 | `.claude/rules/incident-to-rule-pipeline.md` | NEW (local) | ✅ ✅ ✅ ✅ | 🟠 P1 | 5-stage pipeline turning user-flagged misses into permanent guards. Pure governance, zero project specificity. |
| 8 | `.claude/rules/mcp-first-with-fallback.md` | NEW (local) | ✅ ✅ ✅ ✅ | 🟡 P2 | Tool-selection rule — MCP-first, CLI fallback. Universal. References specific MCP servers (GitHub, Postgres, Playwright) but as examples, not project-bound. |
| 9 | `.claude/rules/docs-folder-structure.md` | NEW (local) | ✅ ✅ ✅ ✅ | 🟡 P2 | Generic README-per-top-level-folder rule for `documents/`. Template + ownership matrix can be lifted as-is. |

### What is intentionally NOT in the Top-N for PR 1

- `audit-to-gap-pipeline.md` is included even though its naming uses generic "audit/gap" terms; included because the pipeline IS generic.
- `planning-docs-structure.md` — flagged 🔒 PROJECT-SPECIFIC by script (correctly — it cites `documents/03-planning/` specific folder names; would need rewrite to placeholders).
- `post-wave-audit-mandate.md` — flagged 🔒 (correctly — heavily references project-specific audit categories like "ui-review /128", "ops /100"; portable concept but would need significant scrub).
- `business-logic-review.md`, `logs-format-standard.md`, `design-patterns.md`, `ai-branding-guidelines.md` — flagged 🔒 (correctly — heavy project-specific references; staying local).
- All 101 NEW (local) skill files — defer to PR 2 + PR 3 after rules merge upstream. Skills also need scoring-guide normalization (some reference specific scores like /128 that would need conversion to /100 generic).

---

## 3. Skip List — Project-Specific Items

The script identified 48 files as 🔒 PROJECT-SPECIFIC. Spot-check confirms heuristic largely correct. Items NOT to include in any upstream PR:

### 3.1 Rules (6 files, all correct skip)

| File | Reason |
|------|--------|
| `rules/ai-branding-guidelines.md` | KiteHub Branding feature-specific |
| `rules/business-logic-review.md` | Cites `documents/01-business/`, KiteClass tier names |
| `rules/design-patterns.md` | Heavy KiteHub/KiteClass code references; outbox patterns project-tuned |
| `rules/logs-format-standard.md` | Vietnamese law citations (PDPL, ND-13/2023), kite-specific service list |
| `rules/planning-docs-structure.md` | `documents/03-planning/` folder structure specific |
| `rules/post-wave-audit-mandate.md` | Cites "ui-review /128", project audit categories |

### 3.2 Skills (42 files, mostly correct skip)

Patterns that correctly skip:
- `skills/quality/{audit-name}/SKILL.md` files — most reference project-specific scoring (/128, /100 with kite-bound categories)
- `skills/quality/business-logic-audit/data/eval-fixtures/*.md` — fixtures encode specific KiteClass rules
- `skills/quality/security-audit/data/eval-fixtures/*.md` — same
- `skills/quality/email-template-review/reference/sample-data.md` — KiteHub branding samples
- `skills/backend/backend-standards.md` — Spring Boot + KiteHub package conventions
- `skills/document-generation/{type}/SKILL.md` — wired to project paths
- `skills/quality/ui-review/SKILL.md` — KiteClass + KiteHub frontends as audit targets
- `skills/quality/wave-pack-planner/reference/file-overlap-algorithm.md` — embeds project file maps
- `skills/quick-reference/task-breakdown-examples.md` + `task-breakdown-formula.md` — examples drawn from KiteClass features
- `skills/reference/{architecture-overview, cross-service-data-strategy, email-service, plantuml-diagrams, ui-template-guide}.md` — project-bound

Spot-check found NO obvious false-positives in 🔒 list (heuristic ran clean for skip side).

### 3.3 Heuristic false-NEGATIVES (script said NEW but reviewer flags caution)

These were marked "NEW (local) — candidate for remote PR" but on inspection have project references that need scrubbing or downscoping before upstream import. Not blockers — flagged for Phase 2b cleanup:

| File | Concern | Action for Phase 2b |
|------|---------|---------------------|
| `skills/_README-skills-index.md` | Index references project skills + project-specific paths | Rebuild as generic template index, or split into project-bound section + portable section |
| `skills/quality/ops-readiness-audit/SKILL.md` (root) — script flagged candidate but content cites kite services | Likely should be 🔒 | Re-triage in Phase 2b; defer |
| `skills/quality/wave-pack-planner/SKILL.md` | References Wave Obs 5x speedup specific to this repo | Generic concept — rewrite intro to remove repo-specific stat |
| `skills/quality/marketing-legal-review/reference/compliance-checklist.md` | Vietnamese-law-specific (PDPL, Consumer Protection) | Keep VN focus OR generalize to "your-jurisdiction-specific" — open question §4 |
| `skills/devops/terraform-cloud-deploy/reference/{aws-deploy,oracle-cloud-deploy}.md` | Mentions kite stack, ports | Light scrub — replace with placeholder `<your-app>` |

---

## 4. Open Questions for User

### Q1 — Localization scope of marketing/legal review skill
`marketing-legal-review/` is rich and battle-tested but **Vietnam-PDPL/Consumer-Protection-primary**. Options for upstream:
- **A.** Keep as-is, label "Vietnam-focused starter; replace per your jurisdiction"
- **B.** Strip jurisdiction-specific rules, ship as generic checklist with TODO markers per jurisdiction
- **C.** Skip entirely from upstream — keep local only
**Recommendation:** B (most useful for downstream projects). Ask user for confirmation before Phase 2b.

### Q2 — Skills-index pattern
`_README-skills-index.md` is the navigation backbone for the kit. Should upstream:
- **A.** Ship a GENERIC template skills-index that downstream projects fill in
- **B.** Ship the project's index as-is and let downstream forks edit
- **C.** Skip (each project authors their own)
**Recommendation:** A — provide a clean template with category headings + placeholder rows.

### Q3 — Rules vs Skills package split
Current Top-N is rules-only (PR 1). Should Phase 2b open:
- **A.** PR 1 (rules) + PR 2 (skills:core+workflow) + PR 3 (skills:quality+reference) as runbook §6 suggests
- **B.** Single PR everything (faster but harder review)
- **C.** Just PR 1 to start, gauge maintainer response, scope PR 2+ accordingly
**Recommendation:** C — incremental. Start with 9 rules, see review velocity, then plan skills batches.

### Q4 — Local kit mirror
Project has no `.claude/starter-kit/` mirror folder. Runbook §7 ("Local-Side Updates") assumes one. Should we:
- **A.** Create `.claude/starter-kit/` as a sparse mirror after first upstream PR merges (sync downstream)
- **B.** Treat the local rules+skills tree as the canonical source and skip the mirror entirely
- **C.** Mirror selectively (only generic items)
**Recommendation:** A — once Phase 2b ships, sync local mirror so future per-change syncs (per `skill-conventions.md §Remote Repo Sync`) have a target.

### Q5 — Coordination with remote VERSION 2.2.0
Remote is at v2.2.0 (last release 2026-04-04). Importing 9 rules = MINOR bump → v2.3.0. Adding ~85 generic skills later = either staged MINORs (v2.4.0, v2.5.0) or a single MAJOR if any restructuring required. Confirm appetite for cadence in Phase 2b.

---

## 5. Next Steps — Phase 2b Tracked in GAP-262

This triage report is the Phase 2a output. The actual upstream PR work is Phase 2b, tracked in **`documents/04-quality/gaps/GAP-262-starter-kit-upstream-retro-sync-pr.md`** (filed alongside this report).

GAP-262 acceptance criteria summary:
- [ ] Open PR 1 on `VictorAurelius/claude-starter-kit` with the 9 rules from §2 (with scrubbing per §3.3 + §4 user decisions)
- [ ] Bump remote `VERSION` 2.2.0 → 2.3.0 (MINOR per `skill-conventions.md §Starter-Kit Version Management`)
- [ ] Add `CHANGELOG.md` entry per runbook §5 format
- [ ] Confirm sync back to local: create `.claude/starter-kit/` mirror at v2.3.0 (per Q4 decision)
- [ ] (Optional, deferred) PR 2 + PR 3 for skills batches

GAP-262 stays 🔵 OPEN until user decisions on Q1–Q5 land + the upstream PR is opened.

GAP-195 stays 🟡 PARTIAL until GAP-262 closes (full DONE = upstream PR landed + local mirror in sync).

---

## 6. Raw Diff Output (collapsible — for archival)

<details>
<summary>Click to expand: full output of `scripts/starter-kit-diff.sh` run 2026-04-29T04:08:16+00:00</summary>

```markdown
# Starter-Kit Diff Report

- **Generated:** 2026-04-29T04:08:16+00:00
- **Remote:** https://github.com/VictorAurelius/claude-starter-kit.git
- **Remote VERSION:** 2.2.0
- **Local kit copy VERSION:** none
- **Project root:** /home/nguyenvankiet/projects/2026-Kite-Class-Platform/.claude/worktrees/agent-a3ada478
- **Category filter:** all

Triage legend:
- 🆕 NEW (local) — exists locally, missing remote → candidate for retro-sync
- 🆕 NEW (remote) — exists remote, missing locally → import candidate
- ✏️ MODIFIED — exists both sides, content differs → merge decision needed
- 🔒 PROJECT-SPECIFIC — local only, but tagged project-specific → omit from sync


## Section: `.claude/rules/`

### 🆕 NEW (local only) + 🔒 PROJECT-SPECIFIC

- 🔒 `rules/ai-branding-guidelines.md` — project-specific, omit from sync
- 🆕 `rules/audit-to-gap-pipeline.md` — candidate for remote PR
- 🔒 `rules/business-logic-review.md` — project-specific, omit from sync
- 🔒 `rules/design-patterns.md` — project-specific, omit from sync
- 🆕 `rules/docs-folder-structure.md` — candidate for remote PR
- 🆕 `rules/gap-done-discipline.md` — candidate for remote PR
- 🆕 `rules/incident-to-rule-pipeline.md` — candidate for remote PR
- 🔒 `rules/logs-format-standard.md` — project-specific, omit from sync
- 🆕 `rules/mcp-first-with-fallback.md` — candidate for remote PR
- 🆕 `rules/meta-gap-priority.md` — candidate for remote PR
- 🆕 `rules/output-review-mandate.md` — candidate for remote PR
- 🔒 `rules/planning-docs-structure.md` — project-specific, omit from sync
- 🔒 `rules/post-wave-audit-mandate.md` — project-specific, omit from sync
- 🆕 `rules/rule-change-process.md` — candidate for remote PR
- 🆕 `rules/skill-conventions.md` — candidate for remote PR

## Section: `.claude/skills/`

### 🆕 NEW (local only) + 🔒 PROJECT-SPECIFIC

- 🆕 `skills/_README-skills-index.md` — candidate for remote PR
- 🔒 `skills/backend/backend-standards.md` — project-specific, omit from sync
- 🔒 `skills/core/brainstorming-methodology.md` — project-specific, omit from sync
- 🔒 `skills/core/systematic-debugging.md` — project-specific, omit from sync
- 🆕 `skills/core/task-breakdown-guide.md` — candidate for remote PR
- 🆕 `skills/core/tdd-enforcement.md` — candidate for remote PR
- 🆕 `skills/core/two-stage-code-review.md` — candidate for remote PR
- 🆕 `skills/devops/devops-standards.md` — candidate for remote PR
- 🆕 `skills/devops/terraform-cloud-deploy/SKILL.md` — candidate for remote PR
- 🆕 `skills/devops/terraform-cloud-deploy/reference/aws-deploy.md` — candidate for remote PR
- 🆕 `skills/devops/terraform-cloud-deploy/reference/oracle-cloud-deploy.md` — candidate for remote PR
- 🆕 `skills/devops/terraform-cloud-deploy/reference/output-templates.md` — candidate for remote PR
- 🆕 `skills/devops/terraform-cloud-deploy/reference/terraform-review.md` — candidate for remote PR
- 🔒 `skills/document-generation/excel/SKILL.md` — project-specific, omit from sync
- 🆕 `skills/document-generation/excel/reference/excel-formula-patterns.md` — candidate for remote PR
- 🔒 `skills/document-generation/pdf/SKILL.md` — project-specific, omit from sync
- 🆕 `skills/document-generation/pdf/reference/pdf-block-types.md` — candidate for remote PR
- 🆕 `skills/document-generation/pdf/reference/pdf-cover-styles.md` — candidate for remote PR
- 🔒 `skills/document-generation/word/SKILL.md` — project-specific, omit from sync
- 🆕 `skills/document-generation/word/reference/docx-3-pipelines.md` — candidate for remote PR
- 🆕 `skills/frontend/frontend-standards.md` — candidate for remote PR
- 🆕 `skills/frontend/ui-template-guide.md` — candidate for remote PR
- 🔒 `skills/quality-audit/SKILL.md` — project-specific, omit from sync
- 🔒 `skills/quality/ai-branding-quality-gate/SKILL.md` — project-specific, omit from sync
- 🔒 `skills/quality/api-contract-audit/SKILL.md` — project-specific, omit from sync
- 🆕 `skills/quality/api-contract-audit/reference/scoring-guide.md` — candidate for remote PR
- 🔒 `skills/quality/business-gap-check.md` — project-specific, omit from sync
- 🔒 `skills/quality/business-logic-audit/SKILL.md` — project-specific, omit from sync
- 🔒 `skills/quality/business-logic-audit/data/eval-fixtures/bad-rule-not-implemented.md` — project-specific, omit from sync
- 🔒 `skills/quality/business-logic-audit/data/eval-fixtures/edge-config-key-renamed.md` — project-specific, omit from sync
- 🔒 `skills/quality/business-logic-audit/data/eval-fixtures/good.md` — project-specific, omit from sync
- 🆕 `skills/quality/business-logic-audit/reference/scoring-guide.md` — candidate for remote PR
- 🔒 `skills/quality/cross-app-consistency.md` — project-specific, omit from sync
- 🔒 `skills/quality/design-pattern-audit/SKILL.md` — project-specific, omit from sync
- 🔒 `skills/quality/design-pattern-audit/reference/anti-pattern-detectors.md` — project-specific, omit from sync
- 🆕 `skills/quality/design-pattern-audit/reference/scoring-guide.md` — candidate for remote PR
- 🔒 `skills/quality/email-template-review/SKILL.md` — project-specific, omit from sync
- 🔒 `skills/quality/email-template-review/reference/checklist.md` — project-specific, omit from sync
- 🔒 `skills/quality/email-template-review/reference/sample-data.md` — project-specific, omit from sync
- 🆕 `skills/quality/gap-review/SKILL.md` — candidate for remote PR
- 🆕 `skills/quality/gap-review/reference/checklist.md` — candidate for remote PR
- 🆕 `skills/quality/marketing-legal-review/SKILL.md` — candidate for remote PR
- 🆕 `skills/quality/marketing-legal-review/reference/compliance-checklist.md` — candidate for remote PR
- 🆕 `skills/quality/marketing-legal-review/reference/workflow.md` — candidate for remote PR
- 🆕 `skills/quality/migration-review-checklist.md` — candidate for remote PR
- 🆕 `skills/quality/ops-readiness-audit/SKILL.md` — candidate for remote PR
- 🆕 `skills/quality/ops-readiness-audit/reference/scoring-guide.md` — candidate for remote PR
- 🔒 `skills/quality/performance-audit/SKILL.md` — project-specific, omit from sync
- 🆕 `skills/quality/performance-audit/reference/scoring-guide.md` — candidate for remote PR
- 🆕 `skills/quality/persona-based-business-review.md` — candidate for remote PR
- 🆕 `skills/quality/pre-flight-check.md` — candidate for remote PR
- 🆕 `skills/quality/rework-audit/SKILL.md` — candidate for remote PR
- 🆕 `skills/quality/rework-audit/reference/heuristics.md` — candidate for remote PR
- 🆕 `skills/quality/rework-audit/reference/scoring-rubric.md` — candidate for remote PR
- 🆕 `skills/quality/rule-review/SKILL.md` — candidate for remote PR
- 🔒 `skills/quality/script-review-checklist.md` — project-specific, omit from sync
- 🔒 `skills/quality/security-audit/SKILL.md` — project-specific, omit from sync
- 🔒 `skills/quality/security-audit/data/eval-fixtures/bad-secret-in-config.md` — project-specific, omit from sync
- 🔒 `skills/quality/security-audit/data/eval-fixtures/edge-transitive-cve.md` — project-specific, omit from sync
- 🔒 `skills/quality/security-audit/data/eval-fixtures/good.md` — project-specific, omit from sync
- 🆕 `skills/quality/security-audit/reference/scoring-guide.md` — candidate for remote PR
- 🆕 `skills/quality/simulation-gap-finder.md` — candidate for remote PR
- 🆕 `skills/quality/skill-conventions-check/data/fixtures/bad-description-style.md` — candidate for remote PR
- 🆕 `skills/quality/skill-conventions-check/data/fixtures/bad-no-gotchas.md` — candidate for remote PR
- 🆕 `skills/quality/skill-conventions-check/data/fixtures/good.md` — candidate for remote PR
- 🔒 `skills/quality/ui-review/SKILL.md` — project-specific, omit from sync
- 🆕 `skills/quality/wave-pack-planner/SKILL.md` — candidate for remote PR
- 🆕 `skills/quality/wave-pack-planner/assets/agents/docs-only-agent.md` — candidate for remote PR
- 🔒 `skills/quality/wave-pack-planner/assets/agents/feature-tdd-agent.md` — project-specific, omit from sync
- 🆕 `skills/quality/wave-pack-planner/assets/agents/p3-cleanup-agent.md` — candidate for remote PR
- 🆕 `skills/quality/wave-pack-planner/assets/agents/test-only-agent.md` — candidate for remote PR
- 🆕 `skills/quality/wave-pack-planner/assets/agents/wave-coordinator-agent.md` — candidate for remote PR
- 🆕 `skills/quality/wave-pack-planner/data/self-test-result.md` — candidate for remote PR
- 🆕 `skills/quality/wave-pack-planner/reference/agent-spawning-template.md` — candidate for remote PR
- 🆕 `skills/quality/wave-pack-planner/reference/background-loop-fleet.md` — candidate for remote PR
- 🆕 `skills/quality/wave-pack-planner/reference/cluster-pattern.md` — candidate for remote PR
- 🔒 `skills/quality/wave-pack-planner/reference/file-overlap-algorithm.md` — project-specific, omit from sync
- 🆕 `skills/quality/wave-pack-planner/reference/retrospective-checklist.md` — candidate for remote PR
- 🆕 `skills/quality/wave-pack-planner/reference/wave-plan-template.md` — candidate for remote PR
- 🆕 `skills/quality/wave-pack-planner/scripts/analyze-overlap.sh` — candidate for remote PR
- 🆕 `skills/quick-reference/brainstorming-question-templates.md` — candidate for remote PR
- 🆕 `skills/quick-reference/brainstorming-trade-off-matrix.md` — candidate for remote PR
- 🆕 `skills/quick-reference/design-decision-documentation.md` — candidate for remote PR
- 🆕 `skills/quick-reference/quick-brainstorm-template.md` — candidate for remote PR
- 🆕 `skills/quick-reference/review-checklists.md` — candidate for remote PR
- 🆕 `skills/quick-reference/review-stage-decision-tree.md` — candidate for remote PR
- 🆕 `skills/quick-reference/review-template.md` — candidate for remote PR
- 🆕 `skills/quick-reference/systematic-debugging-4phases.md` — candidate for remote PR
- 🆕 `skills/quick-reference/systematic-debugging-checklist.md` — candidate for remote PR
- 🔒 `skills/quick-reference/task-breakdown-examples.md` — project-specific, omit from sync
- 🔒 `skills/quick-reference/task-breakdown-formula.md` — project-specific, omit from sync
- 🆕 `skills/quick-reference/tdd-git-hook.md` — candidate for remote PR
- 🆕 `skills/quick-reference/tdd-phases.md` — candidate for remote PR
- 🆕 `skills/quick-reference/tdd-workflow-diagram.md` — candidate for remote PR
- 🔒 `skills/reference/architecture-overview.md` — project-specific, omit from sync
- 🆕 `skills/reference/business-docs-3-layer.md` — candidate for remote PR
- 🔒 `skills/reference/cross-service-data-strategy.md` — project-specific, omit from sync
- 🆕 `skills/reference/design-pattern-advisor.md` — candidate for remote PR
- 🆕 `skills/reference/diagrams.md` — candidate for remote PR
- 🔒 `skills/reference/email-service.md` — project-specific, omit from sync
- 🆕 `skills/reference/ide-setup.md` — candidate for remote PR
- 🔒 `skills/reference/plantuml-diagrams.md` — project-specific, omit from sync
- 🆕 `skills/reference/project-structure.md` — candidate for remote PR
- 🆕 `skills/reference/service-docs-standard.md` — candidate for remote PR
- 🔒 `skills/reference/ui-template-guide.md` — project-specific, omit from sync
- 🔒 `skills/testing/testing-standards.md` — project-specific, omit from sync
- 🆕 `skills/workflow/check-pr/SKILL.md` — candidate for remote PR
- 🆕 `skills/workflow/ci-failure-triage.md` — candidate for remote PR
- 🆕 `skills/workflow/continue/SKILL.md` — candidate for remote PR
- 🔒 `skills/workflow/development-workflow/SKILL.md` — project-specific, omit from sync
- 🆕 `skills/workflow/development-workflow/reference/branching-strategy.md` — candidate for remote PR
- 🆕 `skills/workflow/development-workflow/reference/checklist.md` — candidate for remote PR
- 🆕 `skills/workflow/development-workflow/reference/commit-messages.md` — candidate for remote PR
- 🆕 `skills/workflow/development-workflow/reference/phases-1-2-3.md` — candidate for remote PR
- 🔒 `skills/workflow/development-workflow/reference/phases-4-5-6-7.md` — project-specific, omit from sync
- 🆕 `skills/workflow/development-workflow/reference/pull-request-process.md` — candidate for remote PR
- 🆕 `skills/workflow/development-workflow/reference/release-and-hotfix.md` — candidate for remote PR
- 🆕 `skills/workflow/development-workflow/reference/troubleshooting.md` — candidate for remote PR
- 🆕 `skills/workflow/docs-freshness/SKILL.md` — candidate for remote PR
- 🆕 `skills/workflow/fix-pr/SKILL.md` — candidate for remote PR
- 🆕 `skills/workflow/gap-to-pr-converter.md` — candidate for remote PR
- 🆕 `skills/workflow/gap-triage.md` — candidate for remote PR
- 🆕 `skills/workflow/pr-health.md` — candidate for remote PR
- 🆕 `skills/workflow/priority-pr-planning/SKILL.md` — candidate for remote PR
- 🆕 `skills/workflow/priority-pr-planning/reference/compliance-checklist.md` — candidate for remote PR
- 🔒 `skills/workflow/priority-pr-planning/reference/core-principle.md` — project-specific, omit from sync
- 🆕 `skills/workflow/priority-pr-planning/reference/example-good-vs-bad.md` — candidate for remote PR
- 🆕 `skills/workflow/priority-pr-planning/reference/execution-workflow.md` — candidate for remote PR
- 🆕 `skills/workflow/priority-pr-planning/reference/pitfalls.md` — candidate for remote PR
- 🆕 `skills/workflow/priority-pr-planning/reference/plan-template.md` — candidate for remote PR
- 🆕 `skills/workflow/priority-pr-planning/reference/validation-checklist.md` — candidate for remote PR
- 🆕 `skills/workflow/priority-pr-planning/reference/when-to-create.md` — candidate for remote PR
- 🔒 `skills/workflow/quality-plan/SKILL.md` — project-specific, omit from sync
- 🆕 `skills/workflow/repo-status/SKILL.md` — candidate for remote PR
- 🆕 `skills/workflow/repo-status/reference/level-definitions.md` — candidate for remote PR
- 🆕 `skills/workflow/session-docs-check/SKILL.md` — candidate for remote PR
- 🔒 `skills/workflow/session-docs-check/reference/doc-rules-matrix.md` — project-specific, omit from sync
- 🆕 `skills/workflow/session-docs-check/scripts/check-docs.sh` — candidate for remote PR
- 🆕 `skills/workflow/start-pr/SKILL.md` — candidate for remote PR
- 🆕 `skills/workflow/start-session/SKILL.md` — candidate for remote PR
- 🆕 `skills/workflow/start-session/reference/context-template.md` — candidate for remote PR
- 🆕 `skills/workflow/start-session/scripts/collect-state.sh` — candidate for remote PR
- 🆕 `skills/workflow/wave-completion-check.md` — candidate for remote PR

## Summary

Review the triaged diff above and:
1. For 🆕 NEW (local) non-project-specific: candidate for PR to https://github.com/VictorAurelius/claude-starter-kit.git
2. For 🆕 NEW (remote): decide if project should adopt
3. For ✏️ MODIFIED: run `diff -u` locally to inspect
4. For 🔒 PROJECT-SPECIFIC: document in this project only; do not sync

See documents/05-guides/starter-kit-retro-sync.md for the triage + PR runbook.
```

</details>

---

## Log

- **2026-04-29 (Phase 2b PR 1 OPENED):** Top-N 9 rules shipped to upstream PR https://github.com/VictorAurelius/claude-starter-kit/pull/10. Q1-Q5 user decisions: B/A/C/A/staged-MINORs (see GAP-262 Log). Light scrubbing applied per §3.3 to 3 rules (output-review-mandate, skill-conventions, meta-gap-priority).
- **2026-04-29:** Triage report created as Phase 2a deliverable for GAP-195 (Wave Meta Phase-2 Cleanup, Cluster 7 Agent C). Diff script ran clean (exit 0); 110 NEW (local) candidates / 48 PROJECT-SPECIFIC / 0 MODIFIED / 0 NEW (remote). Top 9 rules selected for first upstream PR; skills batches deferred. Phase 2b tracked in GAP-262. Files modified by this work: this report (NEW), GAP-262 (NEW), runbook (Log entry append), GAP-195 (Log entry append + AC annotations).
