# Update Plan Tracking Skill

## Purpose

Ensure the implementation plan tracking file is updated after completing each PR to maintain accurate progress visibility across sessions.

---

## File Location

**Implementation Plan:** `/mnt/e/person/2026-Kite-Class-Platform/documents/scripts/kiteclass-implementation-plan.md`

This file contains all 27 PRs for the KiteClass platform (Gateway, Core, Frontend) with status tracking.

---

## When to Update

**TRIGGER:** After completing ANY PR implementation and running tests successfully.

**TIMING:** Update the plan BEFORE committing the PR code.

---

## What to Update

### 1. Progress Tracking Section (Top of File)

Update the statistics in the `📊 PROGRESS TRACKING` section:

```markdown
## Gateway Service (feature/gateway branch)
- ✅ PR 1.1: Project Setup
- ✅ PR 1.2: Common Components
...
- ✅ PR 1.X: [Just completed PR] ← UPDATE THIS

**Gateway Status:** X/7 PRs completed (X.X%) ← UPDATE THIS
**Tests:** XX passing (XX unit + XX integration) ← UPDATE THIS
**Last Updated:** 2026-XX-XX (PR 1.X - Description) ← UPDATE THIS
**Current Work:** [Next planned work] ← UPDATE THIS
```

### 2. PR Status Icon

Change the PR header from `⏳` to `✅`:

**Before:**
```markdown
## ⏳ PR 1.5 - Email Service
```

**After:**
```markdown
## ✅ PR 1.5 - Email Service
```

### 3. Overall Progress

Update the overall statistics:

```markdown
**Overall Progress:** X/27 PRs completed (X.X%) ← UPDATE THIS
**Last Updated:** 2026-XX-XX (PR X.X - Description) ← UPDATE THIS
```

---

## Status Icons

- ✅ `✅` - Completed PR
- 🚧 `🚧` - In Progress (current work)
- ⏳ `⏳` - Pending (not started)

**Rules:**
- Only ONE PR should be marked `🚧` at a time
- When starting a new PR, mark it `🚧`
- When completing a PR, change `🚧` to `✅`

---

## Example Workflow

### Before Starting PR 1.6

```markdown
## ✅ PR 1.5 - Email Service

## 🚧 PR 1.6 - Gateway Configuration  ← Mark as in progress

**Gateway Status:** 5/7 PRs completed (71.4%)
**Current Work:** PR 1.6 - Gateway Configuration
```

### After Completing PR 1.6

```markdown
## ✅ PR 1.5 - Email Service

## ✅ PR 1.6 - Gateway Configuration  ← Mark as complete

**Gateway Status:** 6/7 PRs completed (85.7%)  ← Update count
**Tests:** 95 passing (50 unit + 45 integration)  ← Update test count
**Last Updated:** 2026-01-27 (PR 1.6 - Gateway Configuration)  ← Update date
**Current Work:** Planning next steps (PR 1.7 or PR 2.1)  ← Update next work
```

---

## Integration with Other Skills

This skill works together with:

1. **pr-commit-workflow.md** - Commit code AFTER updating plan
2. **update-quick-start.md** - Update QUICK-START.md AFTER updating plan
3. **git-workflow.md** - Follow git patterns for commits

**Order of operations:**
1. Complete PR implementation and tests ✅
2. Update implementation plan (THIS SKILL) ✅
3. Update QUICK-START.md ✅
4. Commit all changes together ✅

---

## Checklist After Each PR

Before committing PR code, verify:

- [ ] Implementation plan status icon updated (⏳ → ✅)
- [ ] Progress tracking statistics updated
- [ ] Test count updated
- [ ] Last Updated date updated
- [ ] Current Work updated
- [ ] Overall progress percentage recalculated

---

## Notes

### Adding New PRs to Plan

If a PR is added that wasn't in the original plan (like PR 1.4.1 Docker Setup or PR 1.5 Email Service):

1. Add a new section with clear note:
   ```markdown
   ### ✅ PR 1.4.1 - Docker Setup *(ADDED TO PLAN)*

   **Note:** This PR was added between 1.4 and 1.5 to complete Docker infrastructure early.
   ```

2. Update numbering of subsequent PRs if needed, with notes:
   ```markdown
   ## ⏳ PR 1.6 - Gateway Configuration (ORIGINAL PR 1.5)

   **Note:** This is the original PR 1.5 from the plan, renumbered to 1.6 after additions.
   ```

3. Update total PR counts in progress tracking

### Skipping or Combining PRs

If PRs are combined or skipped:

1. Mark as complete with note:
   ```markdown
   ## ✅ PR 1.7 - Gateway Database & Docker (ORIGINAL PR 1.6)

   **Note:** Most tasks already completed in PR 1.4.1.
   **Status:** ✅ MOSTLY COMPLETE via PR 1.4.1
   ```

---

## Why This Matters

**Context Preservation:** When context is cleared or a new Claude session starts, the implementation plan provides:
- Clear visibility of what's been completed
- What's currently in progress
- What's next in the roadmap
- Test coverage at each stage

**Team Coordination:** If multiple developers work on the project, the plan shows progress at a glance.

**Decision Making:** Helps decide whether to continue with the current service or switch to another.

---

## Example Commit Message

After updating the plan:

```bash
git commit -m "docs(plan): update tracking for PR 1.5 - Email Service

Update implementation plan with:
- Mark PR 1.5 as complete
- Update Gateway progress to 6/7 (85.7%)
- Update test count to 82 passing
- Update last modified date

Related: PR 1.5 Email Service

Co-Authored-By: Claude Sonnet 4.5 <noreply@anthropic.com>"
```

---

**Last Updated:** 2026-01-27
**Version:** 1.0
**Status:** ✅ Active
