---
description: "Dùng khi user nói 'convert gap X thành PR', 'start PR cho gap Y', 'gap này thành wave', 'từ gap tạo PR'. Input: gap ID (GAP-XXX). Output: branch name + PR template + task breakdown + dependency check."
---

# Skill: Gap → PR/Wave Converter

**Version:** 1.0
**Created:** 2026-04-14
**Purpose:** Transform a gap file (`GAP-XXX.md`) into actionable PR/wave với proper structure.

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

### Step 6: Wave Mode — Grouping

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
| `pre-flight-check.md` | Run after generating before coding |
| `design-pattern-advisor.md` | Called during pattern identification |
| `workflow/start-pr/` | Handles actual branch + PR creation |
| `wave-completion-check.md` | Use after wave implementation complete |

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
