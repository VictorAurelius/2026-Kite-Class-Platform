---
name: gap-to-pr
description: "Dùng khi user nói 'convert gap X thành PR', 'start PR cho gap Y', 'gap này thành wave', 'từ gap tạo PR', 'fix gaps'. Input: gap ID (GAP-XXX). Output: branch name + PR template + task breakdown + dependency check."
user-invocable: true
---

# Skill: Gap → PR/Wave Converter

**Version:** 1.0
**Created:** 2026-04-14
**Purpose:** Transform a gap file (`GAP-XXX.md`) into actionable PR/wave với proper structure.

**Tool preference:** GitHub MCP for PR creation if connected; `gh pr create` fallback. See `.claude/rules/mcp-first-with-fallback.md`.

---

## Usage

```
/gap-to-pr GAP-007
/gap-to-pr GAP-011 --wave
/gap-to-pr GAP-007,GAP-008,GAP-009 --wave wave/core-pipeline
```

Modes:
- **PR mode** (single gap, small scope): `gap-to-pr GAP-XXX`
- **Wave mode** (multiple related gaps): `gap-to-pr GAP-X,GAP-Y --wave NAME`

---

## Process

### Step 1: Load Gap + Dependencies

```bash
# Read gap file
cat documents/04-quality/gaps/GAP-XXX-*.md

# Check dependencies section
# Cross-reference với ROADMAP.md để tìm epic + sprint context
```

Extract:
- Title, description, priority
- Dependencies (blocked by / blocks)
- Acceptance criteria
- Proposed fix scope
- Related files

### Step 2: Dependency Validation

Before starting work:
- [ ] All "blocked by" gaps have status `DONE` hoặc `IN_PROGRESS`?
- [ ] This gap trong current sprint (check ROADMAP)?
- [ ] Related code files exist?
- [ ] Business docs prerequisites done (per GAP-016)?

**Block:** nếu dependency chưa xong → stop, fix dependencies first.

### Step 2.5: State-Check — Is Gap Scope Still Accurate? (BẮT BUỘC)

Between the time the gap was filed and now, code may have moved. Re-verify before building the PR:

- If gap has `## Current State (verified YYYY-MM-DD)` section → grep same paths again; if state diverged, update the gap file first, then resume.
- If gap has NO Current State section (old-format gap) → perform a one-shot state-check per `.claude/rules/audit-to-gap-pipeline.md` Step 2.5. Options:
  - Fully implemented → close the gap as DONE in same PR; do NOT build redundant work.
  - Partial → shrink PR scope to the delta; update gap to 🟡 PARTIAL.
  - Still missing → proceed.
- Record the state-check in PR body under "State-Check" header with file paths + line counts inspected.

Skipping this step = risk of shipping duplicate work (cf. GAP-190/197 rewrite incident 2026-04-20).

### Step 3: Generate PR/Wave Structure

#### For PR mode:

```bash
# Branch naming
Priority P0: feat/kite-{gap-short}     # e.g., feat/kite-classification
Priority P1: feat/{gap-short}          # e.g., feat/wizard-autosave
Priority P2: chore/{gap-short}
```

#### PR title template:

```
{type}({scope}): {gap title short} ({GAP-XXX})

Examples:
feat(branding): resource classification pipeline (GAP-007)
fix(security): SVG XSS + SSRF protection (GAP-041)
docs(quality): template library curation (GAP-011)
```

#### PR body template:

```markdown
## Summary

**Closes:** GAP-XXX
**Epic:** [E# - Epic Name] (see ROADMAP.md)
**Priority:** P0/P1/P2
**Type:** feat / fix / docs / chore

{short description from gap.Problem section}

## Changes

{from gap.Proposed Fix section, bullet list}

## Design Patterns Applied

{from .claude/rules/design-patterns.md §2 — list patterns used}
- Pattern X: why
- Pattern Y: why

## Related Gaps

- Blocks: {list}
- Blocked by: {list, all should be DONE}
- Integrates with: {list}

## Acceptance Criteria

{from gap.Acceptance Criteria — copy as checklist}

## Test Plan

- [ ] Unit tests added/updated
- [ ] Integration tests cover critical paths
- [ ] Business doc updated (per CLAUDE.md Living Docs rule)
- [ ] Design pattern documented in javadoc
- [ ] Manual verification per AC

## Required Audits (auto-detect from scope)

{Based on files this PR will change — check mapping table below}

| If PR changes... | Run audit | Command |
|-----------------|-----------|---------|
| `*-frontend/**` | UI Review /128 | `/ui-review` |
| `rules.md`, `application.yml` | Business Logic /100 | `/business-logic-audit` |
| `*Controller.java`, `*Dto.java` | API Contract /100 | `/api-contract-audit` |
| `pom.xml`, `package.json` | Security /100 | `/security-audit` |
| `infrastructure/**`, `Dockerfile` | Ops Readiness /100 | `/ops-readiness-audit` |

Hook `audit-gate.py` enforces at merge time. Run proactively to avoid surprises.

## Dependencies / Blockers

{list any external dependencies}

---

**Gap status:** will be marked 🟠 IN_PROGRESS when PR opens, 🟢 DONE when merged.
```

### Step 4: Task Breakdown (from gap)

Parse `Proposed Fix` section → generate TDD task list:

```
1. [Test-first] Write unit tests for {component} (RED)
2. [Implement] Create {class/module} per pattern Y (GREEN)
3. [Refactor] Extract {concern}, improve readability
4. [Docs] Update business-rules.md + api-contract.md
5. [Integration test] End-to-end verification
6. [Self-review] Apply two-stage-code-review skill
7. [PR] Open với template above
```

### Step 5: Update Gap File

Mark gap as `🟡 PLANNED`:
```markdown
**Status:** 🟡 PLANNED
**Branch:** feat/kite-{name}
**PR:** (to be filled when created)
**Sprint:** {from ROADMAP}
```

Add log entry:
```
- {date} — Converted to PR/wave. Branch: feat/kite-{name}. Sprint: {N}.
```

### Step 6: Wave Mode — Grouping + Parallel Agent Execution

Nếu convert nhiều gaps thành wave:

```bash
# Validate all gaps same epic (from ROADMAP)
# Generate wave branch: wave/{epic-name}
# Wave plan doc: documents/03-planning/wave-{epic}.md
```

Wave plan template:
```markdown
# Wave: {Epic Name}

**Gaps:** GAP-X, GAP-Y, GAP-Z
**Epic:** E# from ROADMAP
**Sprint:** {N}
**Duration estimate:** {sum of gap efforts}

## Sub-PRs

1. PR-1: GAP-X (branch: feat/...)
2. PR-2: GAP-Y (branch: feat/...)
3. PR-3: GAP-Z (branch: feat/...)

## Integration plan
...

## Acceptance
{union of all gaps' AC}
```

### Step 7: Parallel Agent Execution (Wave mode)

Khi wave có nhiều independent gaps → dùng subagents parallel:

```
Parent agent:
1. Tạo wave branch
2. Validate dependencies
3. Spawn subagents (isolation: worktree) cho independent gaps
4. Sequential merge cho dependent gaps
5. Integration test
6. Wave completion check

Per-gap agent (worktree isolated):
1. Read gap file → understand scope
2. TDD: write tests first
3. Implement fix
4. Check: no unused imports/fields/vars, no deprecated APIs
5. Self-review
6. Return: files changed + test results
```

**Rules cho parallel execution:**
- Gaps với dependency → sequential (agent A xong → agent B bắt đầu)
- Gaps independent → parallel (worktree isolation)
- Max 3 agents parallel (tránh resource contention)
- Mỗi agent commit riêng → parent cherry-pick vào wave branch

### Step 8: Post-Fix — Update Gap + ROADMAP (per audit-to-gap-pipeline rule)

**BẮT BUỘC** sau khi fix:
1. Update gap file: status `🟢 DONE`, thêm PR number
2. Update ROADMAP.md: mark gap completed trong epic table
3. Run relevant audits (auto-detect từ changed files)

---

## Example Usage

### Example 1: Single gap PR

```
User: /gap-to-pr GAP-007

Output:
✓ Gap loaded: Resource Classification Pipeline (P0)
✓ Dependencies checked: None blocking
✓ Epic: E2 Core Pipeline (Sprint 1)
✓ Design patterns identified: Chain of Responsibility, Strategy

Branch: feat/kite-classification-pipeline

PR Title: feat(branding): resource classification pipeline (GAP-007)

Tasks:
1. Test: ResourceClassifierTest (4 scenarios)
2. Create: ResourceCategory enum + BrandingResource entity
3. Implement: ResourceRoutingService (Chain of Responsibility)
4. Integration test: full pipeline with mock assets
5. Docs: update 01-business/kitehub/ai-branding/rules.md
6. Review: self-review + two-stage-code-review skill

Gap status updated: 🟡 PLANNED
```

### Example 2: Wave from multiple gaps

```
User: /gap-to-pr GAP-007,GAP-008,GAP-009 --wave core-pipeline

Output:
✓ 3 gaps loaded, all Epic 2
✓ No conflicting dependencies
✓ Estimated effort: L + XL + L = ~35 days (3 devs ~12 days parallel)

Wave branch: wave/core-pipeline
Wave plan: documents/03-planning/wave-core-pipeline.md

Sub-PRs:
- PR-1: GAP-007 (first, no deps)
- PR-2: GAP-008 (depends PR-1)
- PR-3: GAP-009 (depends PR-1)

All gaps marked 🟡 PLANNED
```

---

## Validation Checks

Before generating:
- [ ] Gap exists và well-structured (has AC, Problem, Proposed Fix)
- [ ] Dependencies resolved
- [ ] Branch name doesn't conflict
- [ ] Current sprint matches ROADMAP position

After generating:
- [ ] Gap status updated trong file
- [ ] ROADMAP sprint tracking updated (if applicable)
- [ ] Branch created locally
- [ ] PR template ready (but not submitted — user reviews first)

---

## Integration với Other Skills

| Skill | Relation |
|-------|----------|
| `core/task-breakdown-guide.md` | Source for task list generation |
| `core/tdd-enforcement.md` | Tasks structured as RED-GREEN-REFACTOR |
| `core/two-stage-code-review.md` | Referenced in PR template |
| `quality/pre-flight-check.md` | Run after generating before coding |
| `reference/design-pattern-advisor.md` | Called during pattern identification |
| `workflow/start-pr/` | Handles actual branch + PR creation |
| `workflow/wave-completion-check.md` | Use after wave implementation complete |

---

## Rules

- ✅ Luôn check dependencies trước khi convert
- ✅ Reference ROADMAP để đảm bảo đúng sprint
- ✅ Update gap status sau khi convert
- ✅ Include design patterns trong PR template
- ❌ Không convert gap bị block
- ❌ Không tự động create branch — user confirm trước
- ❌ Không submit PR tự động — user review template trước

---

## Skill Contents

- This SKILL.md — converter methodology
- Related: `.claude/skills/workflow/start-pr/` — actual PR creation
- Data source: `documents/04-quality/gaps/GAP-*.md` + `ROADMAP.md`

---

## Gotchas

- **Step 2.5 state-check is non-negotiable** — gaps filed weeks ago routinely have partial code already; per `.claude/rules/audit-to-gap-pipeline.md` Step 2.5, grep `kiteclass-core/` + `kitehub-*/` paths in the gap's Proposed Fix BEFORE generating the branch, or you ship duplicate work (GAP-190/197 rewrite incident, memory `feedback_gap_state_check_required.md`)
- **Wave mode with ≥3 disjoint sub-tasks beats serial PRs** — for a wave-eligible gap, spawn parallel worktree agents from `wave/X` branch instead of producing one PR per sub-task; serial PRs cost ~3× wall-clock (see `feedback_wave_plan_before_serial_prs.md`, GAP-229 incident 2026-04-26)
- **Wave plan doc itself must go through PR** — generated `documents/03-planning/waves/wave-X-*.md` is NOT exempt from CLAUDE.md "everything via PR" rule; do NOT push wave plan directly to main even if "agents need plan ready now" (memory `feedback_wave_plan_through_pr.md`, 2026-04-26 violation)
- **DONE flip in Step 8 must satisfy gap-done-discipline** — `.claude/rules/gap-done-discipline.md` §2 blocks Status `🟢 DONE` if any AC checkbox is unchecked OR Log entry contains banned phrases (`deferred`, `manual run`, `partial`, etc.); if a sub-task is genuinely deferred, flip to `🟡 PARTIAL` and file follow-up gap (incident GAP-235 Sub-PR G silent-deferral)
- **Branch naming `feat/kite-{...}` collides across services** — both kiteclass-core and kitehub-branding can produce `feat/kite-classification`; prefix with service when scope is single-service (`feat/kc-classification`, `feat/kh-branding-routing`) to keep `git branch -r` legible
