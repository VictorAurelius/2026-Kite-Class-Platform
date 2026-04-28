---
name: fix-pr
description: "Dùng khi user runs `/fix-pr <PR-number>` sau khi `/check-pr` flagged issues. Determines fix strategy (Case A: PR open → amend on branch; Case B: merged → follow-up PR), then addresses brainstorm/test/doc/methodology gaps."
disable-model-invocation: true
---

# Fix PR Quality Issues

**Usage:** `/fix-pr <PR-number>`

**Example:** `/fix-pr 95`

**Prerequisite:** Run `/check-pr <number>` first to identify issues

---

## Instructions

Khi user invoke `/fix-pr $ARGUMENTS`:

### Bước 1: Get PR Status & Check Results

```bash
# Check if PR is merged
gh pr view $ARGUMENTS --json state,mergedAt,headRefName

# Get the check-pr results (from previous run or re-run)
```

### Bước 2: Determine Fix Strategy

#### Case A: PR Still Open (chưa merge)

**Strategy:** Sửa trực tiếp trên branch

```bash
# Checkout PR branch
gh pr checkout $ARGUMENTS

# Or if local branch exists
git checkout <branch-name>
```

**Actions:**
1. Bổ sung artifacts thiếu (brainstorm, breakdown)
2. Thêm tests nếu thiếu
3. Update PR description
4. Amend commits nếu cần (chưa push)
5. Push updates

#### Case B: PR Already Merged

**Strategy:** Tạo follow-up PR

```bash
# Create follow-up branch
git checkout main
git pull origin main
git checkout -b fix/PR-$ARGUMENTS-quality-improvements
```

**Actions:**
1. Tạo documentation cho methodology gaps
2. Thêm tests còn thiếu
3. Tạo PR mới với reference đến PR gốc

---

### Bước 3: Fix Each Category

#### Fix 1: Brainstorm Gaps

**Nếu thiếu brainstorm:**

```markdown
## Retroactive Brainstorm: PR-$ARGUMENTS

> Note: This brainstorm was created retroactively to document design decisions.

### Scope
[Analyze from PR changes - what was actually built]

### Risks Identified (Post-hoc)
[What risks existed, were they handled?]

### Edge Cases
[What edge cases exist in the implementation?]

### Alternatives Considered
[What other approaches could have been taken?]

### Decision Rationale
[Why was this approach chosen?]
```

**Output location:**
- PR chưa merge → Update PR description
- PR đã merge → Add to `documents/04-decisions/` or follow-up PR description

#### Fix 2: Task Breakdown Gaps

**Nếu thiếu breakdown:**

```markdown
## Retroactive Task Breakdown: PR-$ARGUMENTS

> Note: This breakdown was created retroactively.

### Tasks Completed

| # | Task | Actual Time | Type |
|---|------|-------------|------|
| 1 | [Infer from commits] | ~X min | Test/Code |
| 2 | [Infer from commits] | ~X min | Test/Code |

### Lessons for Future
- [What should have been planned differently]
```

#### Fix 3: TDD Gaps

**Nếu tests thiếu hoặc sai order:**

**Option A: Add missing tests (recommended)**
```bash
# Create test files for untested code
# Follow existing test patterns in codebase
```

**Option B: Document why tests not needed**
```markdown
## Test Coverage Note: PR-$ARGUMENTS

**Files without tests:**
- `file1.ts` - Config only, no logic
- `file2.ts` - [Reason]

**Existing coverage:**
- [Related test files that cover this code]
```

#### Fix 4: Code Review Gaps

**Nếu thiếu review evidence:**

```markdown
## Retroactive Code Review: PR-$ARGUMENTS

### Self-Review Checklist
- [x] Code follows project conventions
- [x] No security vulnerabilities
- [x] Error handling adequate
- [x] Performance acceptable
- [ ] [Items that should be improved]

### Test Plan (Retroactive)
- [x] [Test that was run]
- [x] [Test that was run]

### Notes
[Any issues found during retroactive review]
```

---

### Bước 4: Handle Dependencies

#### Khi fix ảnh hưởng PR khác

**Detect Dependencies:**
```bash
# Find PRs that depend on this one
gh pr list --search "base:main" --json number,title,body | jq '.[] | select(.body | contains("PR-$ARGUMENTS"))'

# Find PRs this one depends on
# (Check PR description for references)
```

**Dependency Matrix:**

```markdown
## Dependency Analysis: PR-$ARGUMENTS

### This PR depends on:
| PR | Status | Impact |
|----|--------|--------|
| #X | Merged | None - already integrated |
| #Y | Open | Must merge first |

### PRs that depend on this:
| PR | Status | Impact of Fix |
|----|--------|---------------|
| #A | Open | Need to rebase after fix |
| #B | Merged | May need follow-up fix |

### Recommended Fix Order:
1. Fix PR-$ARGUMENTS first
2. Then update PR-#A (rebase)
3. Create follow-up for PR-#B if needed
```

**Actions based on dependencies:**

1. **No dependencies** → Fix directly
2. **Depends on open PR** → Wait or coordinate
3. **Other PRs depend on this** →
   - Notify in those PRs
   - Create issues for follow-up fixes
   - Update dependency documentation

---

### Bước 5: Execute Fix

#### For Open PRs:

```bash
# 1. Checkout branch
gh pr checkout $ARGUMENTS

# 2. Make fixes
# [Add tests, update docs, etc.]

# 3. Commit fixes
git add .
git commit -m "fix(quality): Add missing methodology artifacts for PR-$ARGUMENTS

Changes:
- Add retroactive brainstorm documentation
- Add missing test coverage
- Update PR description with task breakdown

Quality improvement for Superpowers compliance."

# 4. Push
git push origin <branch>

# 5. Update PR description
gh pr edit $ARGUMENTS --body "$(cat <<'EOF'
[Updated PR description with methodology artifacts]
EOF
)"
```

#### For Merged PRs:

```bash
# 1. Create follow-up branch
git checkout -b fix/PR-$ARGUMENTS-quality

# 2. Add missing artifacts
# - Tests
# - Documentation
# - Any code improvements identified

# 3. Commit
git add .
git commit -m "fix(quality): Add missing artifacts for PR-$ARGUMENTS

Retroactive quality improvements:
- Add tests for [feature]
- Document design decisions
- [Other improvements]

Follows up on PR #$ARGUMENTS."

# 4. Create PR
gh pr create --title "fix(quality): Quality improvements for PR-$ARGUMENTS" --body "$(cat <<'EOF'
## Summary

Retroactive quality improvements for PR #$ARGUMENTS.

### Original PR Issues
- [ ] Missing brainstorm → Added to docs
- [ ] Missing tests → Added X test files
- [ ] Missing breakdown → Documented

### Changes
- Add test coverage for [feature]
- Document design decisions
- [Other changes]

### Related
- Follows up: #$ARGUMENTS
- Depends on: #X (if any)
- Blocks: #Y (if any)

---

**Quality Score Impact:**
- Before: X/100
- After: Y/100 (estimated)
EOF
)"
```

---

### Bước 6: Verify Fix

Sau khi fix, chạy lại `/check-pr $ARGUMENTS` để verify:

```markdown
## Fix Verification: PR-$ARGUMENTS

### Before Fix
| Category | Score |
|----------|-------|
| Brainstorm | X/25 |
| Task Breakdown | X/25 |
| TDD | X/25 |
| Code Review | X/25 |
| **Total** | **X/100** |

### After Fix
| Category | Score |
|----------|-------|
| Brainstorm | Y/25 |
| Task Breakdown | Y/25 |
| TDD | Y/25 |
| Code Review | Y/25 |
| **Total** | **Y/100** |

### Improvement: +Z points
```

---

## Output Summary

```markdown
## Fix Plan: PR-$ARGUMENTS

### Status
- PR State: Open/Merged
- Current Score: X/100 (Grade)
- Target Score: 80+ (Grade B)

### Fix Strategy
- [ ] Strategy: Direct fix / Follow-up PR

### Actions Required

| # | Category | Issue | Fix Action | Effort |
|---|----------|-------|------------|--------|
| 1 | Brainstorm | Missing | Add retro brainstorm | 15 min |
| 2 | TDD | No tests | Add X test files | 1 hour |
| 3 | Review | No test plan | Update PR description | 10 min |

### Dependencies
- Blocks: [None / PR #X, #Y]
- Blocked by: [None / PR #Z]

### Estimated Total Effort: X hours

### Next Steps
1. [First action]
2. [Second action]
3. Run `/check-pr $ARGUMENTS` to verify
```

---

## Quick Reference

| Scenario | Strategy |
|----------|----------|
| PR Open, minor issues | Update PR directly |
| PR Open, major issues | May need to close and recreate |
| PR Merged, missing tests | Follow-up PR with tests |
| PR Merged, missing docs | Add to docs folder |
| Multiple PRs affected | Create dependency graph, fix in order |

---

## Gotchas

- **Case A `gh pr checkout` strips local commits** — if working tree has uncommitted changes the checkout fails; `git stash` first OR use Case B (follow-up PR) instead — never `--force` over local work
- **Case B follow-up PR must NOT amend a closed gap to DONE** — if original PR closed GAP-XYZ as PARTIAL with deferred items, follow-up PR adds the missing pieces and only THEN flips Status (per `gap-done-discipline.md` §2 — banned phrases like "deferred to follow-up" + DONE in same diff trigger Rule 13 BLOCK)
- **Retroactive brainstorm is NOT a free pass** — adding a `## Retroactive Brainstorm` section after-the-fact does not satisfy `output-review-mandate.md` §3 review standard; if the original PR shipped without methodology artifacts, the gap is in the process not the doc — surface that in commit message
- **Docs-only fix PRs still need audit-gate awareness** — even pure-docs follow-ups can trigger `audit-gate.py` AUDIT_RULES if file patterns match (`pom.xml`, `Controller.java`); per `post-wave-audit-mandate.md` §3 docs-only exception kicks in only when ALL paths are `.md`/`.claude/`/`documents/`
- **Co-Authored-By auto-trailer must be stripped** — Claude Code adds the line by default; commit messages in fix PRs follow project convention (memory `feedback_no_coauthored_by.md`) — verify before push
