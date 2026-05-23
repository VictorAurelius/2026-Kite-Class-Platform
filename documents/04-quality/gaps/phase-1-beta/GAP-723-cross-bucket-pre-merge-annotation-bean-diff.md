---
id: GAP-723
title: Cross-bucket pre-merge annotation/bean diff — Wave 105 surfaced 2 conflicts post-merge
status: OPEN
priority: P1
domain: Meta
phase: phase-1-beta
audience: dev
found: 2026-05-23
last_verified: 2026-05-23
completion_pct: 0
related: [GAP-722, pre-mutation-state-check.md §1.5]
---

# GAP-723 — Cross-bucket pre-merge annotation/bean diff (META lesson Wave 105)

## Problem

Wave 105 ship sequence (`#1727` Bucket E + `#1723` Bucket C + `#1725` Bucket B + `#1724` Bucket D + `#1726` Bucket A) had **2 cross-bucket conflicts** surfaced ONLY AFTER MERGE by next PR's CI Test Core Service job:

1. **GradeController duplicate `@PreAuthorize`** on `initializeGrade()` + `getGradesByClass()` — Bucket E0 (PR #1721) already added the annotations; Bucket E (#1727) added them again → Java compile error `is not a repeatable annotation type`.

2. **`@Component("authz")` bean collision** — Bucket C (#1723) added `AuthorizationHelper` (broken-on-Postgres — queried non-existent `teacher_classes` table); Bucket E (#1727) added `AuthorizationBean` (schema-correct via `classes.teacher_id`). Both registered as Spring bean `"authz"` → `ConflictingBeanDefinitionException` on context startup.

Required inline **hotfix PR #1728** (3 commits): annotation dedupe + delete AuthorizationHelper+TeacherIdHolder+test + IT bean-type swap.

## Root cause

Multi-bucket waves with parallel agent worktrees produce PRs that:
- Each pass own CI in isolation (no overlap with other bucket branches)
- Land on main sequentially; subsequent PRs trigger CI against merged-with-main state
- Java/Spring compile-time + bean-registration conflicts surface ONLY at second PR's CI run

Existing rule `pre-mutation-state-check.md` §1.5 covers terraform-specific cross-reference matrix (IAM action ↔ resource ARN ↔ workflow consumer) BUT no equivalent rule for Java code annotation/bean diff across concurrent bucket branches.

Per `outside-in-coverage-trigger.md` v1.1.0 §2.1 architecture-decision keywords (`@Component`, `@PreAuthorize`), 2 bucket PRs declaring same bean name OR re-adding same annotation on same method = signal that should fire pre-merge — currently doesn't.

## Proposed Fix

### Option A: Extend `pre-mutation-state-check.md` §1.5 (preferred)

Add new subsection §1.6 "Java cross-bucket annotation/bean diff (mandatory when ≥3 concurrent bucket PRs touch shared packages)":

```markdown
## 1.6 Java cross-bucket annotation/bean diff

When wave has ≥3 concurrent bucket PRs that touch any of:
- `**/security/**`, `**/auth/**` (authz beans + filters)
- `**/exception/**` (handlers + advice)
- `**/controller/**` overlap (shared base controllers, method annotations)

Pre-wave-merge coordinator MUST run pairwise diff:

```bash
# For each pair of concurrent bucket branches:
for pair in "wave/N-bucket-A wave/N-bucket-B" ...; do
  BRANCH_A=$(echo $pair | cut -d' ' -f1)
  BRANCH_B=$(echo $pair | cut -d' ' -f2)
  # Find files touched by BOTH
  comm -12 <(git diff --name-only main...$BRANCH_A | sort) \
           <(git diff --name-only main...$BRANCH_B | sort) \
    | grep '\.java$'
done
```

For each overlapping file, manually diff:
- Duplicate `@PreAuthorize` / `@Transactional` / annotation additions on same method
- Duplicate `@Component("name")` / `@Bean` name registrations
- Duplicate `@ExceptionHandler` for same exception class

Catch BEFORE merge instead of CI cascade after.
```

### Option B: Reviewer checklist add line

Add to PR template Output Review Checklist:
- [ ] **Cross-bucket diff** — if wave has ≥3 concurrent bucket PRs, coordinator ran pairwise annotation/bean diff per `pre-mutation-state-check.md` §1.6

### Option C: CI grep detector (deferred per premature-rule guard)

Future: `audit-gate.py` rule scanning merge-train for duplicate `@Component`/`@PreAuthorize` patterns. Defer until 2nd recurrence; reviewer + matrix sufficient v1.

## Acceptance Criteria

- [ ] `pre-mutation-state-check.md` extended with §1.6 (Option A) OR sister rule filed
- [ ] PR template updated with cross-bucket checkbox (Option B)
- [ ] Worked self-test §6 applied to Wave 105 incident — both conflicts caught counterfactually
- [ ] Rule applies prospectively from Wave 106+ multi-bucket sessions
- [ ] Update `output-review-mandate.md` §3 with row tracking new rule

## Related

- Wave 105 hotfix PR #1728 (commit `1fb853fb`) — concrete incident
- `pre-mutation-state-check.md` §1.5 — sister rule (terraform scope)
- `outside-in-coverage-trigger.md` v1.1.0 §2.1 — architecture-decision keywords
- `meta-gap-priority.md` §3 — META P1 force-multiplier
- `release-fix-retry-budget.md` §3 — retry pivot (this case caught at retry #1)
- Session handoff `documents/03-planning/session-handoffs/2026-05-23-wave-105-handoff.md` — first documented mention of META lesson

## Cost-save projection

Without rule: ~1 hotfix PR per wave with ≥3 concurrent buckets touching shared packages = ~15-30min round-trip + reviewer cognitive load.

With rule: pairwise diff ~5-10min pre-merge eliminates hotfix cycle. Net savings ~50% time on every multi-bucket wave.

Pattern frequency observed: Wave 105 (5 buckets — 2 conflicts). Estimate ~1-2 multi-bucket waves per month for Phase 1 BETA. Rule force-multiplier compounds prospectively.

## Log

- **2026-05-23**: GAP filed as META retro from Wave 105 self-test deep verify session. Goal "làm cho đến khi hết bug local" achieved (5 surfaced bugs fixed in #1730 + #1731); this META gap captures the process improvement candidate from the same retro. Defer concrete rule extension to next session per context budget; gap acceptance criteria mandates rule edit + reviewer-checklist + worked self-test all paired same PR per `rule-change-process.md` §6.5 Enforcement Parity Mandate.
