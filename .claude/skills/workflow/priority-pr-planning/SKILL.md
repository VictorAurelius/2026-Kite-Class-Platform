---
name: priority-pr-planning
description: Use when user says 'priority PR plan', 'urgent PR queue', 'plan PRs by priority', 'critical bug fix queue', 'PR-KC plan', 'lập kế hoạch PR ưu tiên', 'PR khẩn', or when scoping a temporary deviation from the master implementation plan (critical bug, unblocked PR, urgent technical debt, manual-test findings). Standards for creating temporary priority PR plans that comply with master implementation plan.
type: workflow
---

# Skill: Priority PR Planning

**Version:** 1.1 (folder split from 800-line monolith — Wave 9 / GAP-251 follow-up)
**Last Updated:** 2026-04-28
**Purpose:** Standards for creating temporary priority PR plans that comply with master implementation plan

## Overview

A "priority PR plan" is a **temporary** scoped doc (`PRIORITY-PLAN-YYYY-MM-DD.md`) that lists urgent PRs to ship out-of-order vs the master implementation plan. It exists when something genuinely cannot wait for the queue: production-blocking bug, security vuln, an unblocked PR with deps now satisfied, or critical tech debt surfaced by manual testing.

The plan **inherits 100%** of quality, workflow, and review standards from the master plan and `.claude/skills/`. It does NOT relax tests, JavaDoc, multi-tenant isolation, commit format, or PR review. Its only purpose is justifying + sequencing the deviation. Every PR in the plan still ships through normal review + CI + audit gates.

## When to Use This Skill

- User explicitly asks for a "priority plan" / "urgent PR queue" / scoping out-of-order delivery
- A P0 bug, security issue, or unblocked dependency forces deviation from master plan order
- Manual testing reveals issues that demand immediate attention before the next planned PR
- See `reference/when-to-create.md` for the decision matrix (CREATE vs DON'T CREATE)

## Skill Contents

| Reference | When to read |
|-----------|--------------|
| `reference/core-principle.md` | First-time read or when in doubt about authority — diagram of master-plan inheritance |
| `reference/compliance-checklist.md` | Building the plan — what MUST be present (backend, FE, security, testing, workflow, docs) |
| `reference/plan-template.md` | Filling the template — section-by-section guide (OVERVIEW + PRIORITY N + EXECUTION SUMMARY) |
| `reference/validation-checklist.md` | Self-review before posting — Plan Completeness + Quality Gate review |
| `reference/pitfalls.md` | Common mistakes — read once, before drafting |
| `reference/when-to-create.md` | Decision: priority plan vs normal PR? + Related skills + master plan locations |
| `reference/execution-workflow.md` | Day-to-day execution against the plan (5-stage flow) |
| `reference/example-good-vs-bad.md` | Worked example — read after first draft to compare |

## Gotchas

- Priority plans are **temporary** — never replace the master plan, only deviate within bounded scope
- Compliance with master plan is **non-negotiable** — see `reference/compliance-checklist.md`
- Reference `.claude/skills/` for all workflow steps (Git, commits, PR process, testing) — do NOT invent custom workflow inside the priority plan
- Branch naming convention: `{type}/KC-{id}-{short-desc}` (lowercase, hyphenated, ticket ID embedded)
- Acceptance criteria with fewer than 8 items is a red flag — copy the master-plan checklist verbatim
- Commit messages MUST use HEREDOC format with conventional-commit type/scope (see `reference/plan-template.md` §5)
- Every PR in the plan needs unit + integration + regression tests — "run tests" alone is rejected
- Gap-to-PR integration: use `documents/04-quality/gaps/ROADMAP.md` as priority source; convert via `.claude/skills/workflow/gap-to-pr-converter.md`
- Project policy: do NOT add `Co-Authored-By` trailers to commits (CLAUDE.md §Commit Message Rules) — older example in `reference/example-good-vs-bad.md` shows the trailer for historical reasons but is superseded by current project rule <!-- TODO: verify against current state -->

## Related Skills

- `.claude/skills/workflow/development-workflow.md` — Git, commit, PR process (referenced from every priority plan)
- `.claude/skills/workflow/gap-to-pr-converter.md` — Convert ROADMAP.md gaps into a priority plan
- `.claude/skills/workflow/wave-completion-check.md` — Verifies all gaps in scope hit DONE before sign-off
