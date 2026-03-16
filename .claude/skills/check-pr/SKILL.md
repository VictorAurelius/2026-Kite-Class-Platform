---
name: check-pr
description: Audit a completed PR for Superpowers methodology compliance
disable-model-invocation: true
---

# Check PR Quality

**Usage:** `/check-pr <PR-number>`

**Example:** `/check-pr 95`

---

## Instructions

Khi user invoke `/check-pr $ARGUMENTS`:

### Bước 1: Fetch PR Information

```bash
# Get PR details
gh pr view $ARGUMENTS --json title,body,state,commits,files,additions,deletions,author,createdAt,mergedAt

# Get commit history
gh pr view $ARGUMENTS --json commits --jq '.commits[].messageHeadline'

# Get files changed
gh pr view $ARGUMENTS --json files --jq '.files[].path'
```

### Bước 2: Analyze Superpowers Compliance

Kiểm tra từng tiêu chí:

#### 1. Brainstorm Document (25 points)

**Tìm trong PR description hoặc commits:**
- [ ] Scope defined? (5 pts)
- [ ] Risks identified? (5 pts)
- [ ] Edge cases listed? (5 pts)
- [ ] Alternatives considered? (5 pts)
- [ ] Decision rationale? (5 pts)

**Scoring:**
- ✅ Full (20-25 pts): Có đầy đủ các items
- ⚠️ Partial (10-19 pts): Thiếu 1-2 items
- ❌ Missing (0-9 pts): Không có hoặc thiếu nhiều

#### 2. Task Breakdown (25 points)

**Tìm trong PR description hoặc commits:**
- [ ] Tasks listed? (10 pts)
- [ ] Estimates provided? (10 pts)
- [ ] Logical order? (5 pts)

**Scoring:**
- ✅ Full (20-25 pts): Có tasks + estimates
- ⚠️ Partial (10-19 pts): Có tasks, thiếu estimates
- ❌ Missing (0-9 pts): Không có breakdown

#### 3. TDD Compliance (25 points)

**Analyze commit order:**
```bash
# Check if test files committed before/with implementation
gh pr view $ARGUMENTS --json commits --jq '.commits[] | {sha: .oid[0:7], msg: .messageHeadline}'
```

**Criteria:**
- [ ] Test files exist? (10 pts)
- [ ] Tests committed before/with implementation? (10 pts)
- [ ] Good test coverage (based on file ratio)? (5 pts)

**Indicators:**
- Test files: `*.test.ts`, `*.test.tsx`, `*.spec.ts`, `*Test.java`, `*IT.java`
- Implementation: `*.ts`, `*.tsx`, `*.java` (excluding tests)

**Scoring:**
- ✅ Full (20-25 pts): Tests trước hoặc cùng lúc implementation
- ⚠️ Partial (10-19 pts): Tests có nhưng sau implementation
- ❌ Missing (0-9 pts): Không có tests hoặc rất ít

#### 4. Code Review Quality (25 points)

**Check PR description và comments:**
- [ ] Self-review mentioned? (5 pts)
- [ ] Test plan included? (10 pts)
- [ ] Breaking changes noted? (5 pts)
- [ ] Clean commit messages? (5 pts)

**Scoring:**
- ✅ Full (20-25 pts): Có test plan + clean commits
- ⚠️ Partial (10-19 pts): Thiếu test plan hoặc messy commits
- ❌ Missing (0-9 pts): No review evidence

---

### Bước 3: Output Quality Report

```markdown
## PR Quality Report: #<number>

**Title:** <PR title>
**Author:** <author>
**Status:** <Open/Merged/Closed>
**Created:** <date>
**Merged:** <date or N/A>

---

### Summary

| Category | Score | Status |
|----------|-------|--------|
| Brainstorm | X/25 | ✅/⚠️/❌ |
| Task Breakdown | X/25 | ✅/⚠️/❌ |
| TDD Compliance | X/25 | ✅/⚠️/❌ |
| Code Review | X/25 | ✅/⚠️/❌ |
| **Total** | **X/100** | **Grade** |

### Grade Scale
- 90-100: A (Excellent) - Superpowers fully applied
- 80-89: B (Good) - Minor improvements needed
- 70-79: C (Acceptable) - Some methodology gaps
- 60-69: D (Needs Work) - Significant gaps
- <60: F (Failed) - Methodology not followed

---

### Detailed Analysis

#### 1. Brainstorm (X/25)
- Scope: ✅/❌ [details]
- Risks: ✅/❌ [details]
- Edge Cases: ✅/❌ [details]
- Alternatives: ✅/❌ [details]
- Rationale: ✅/❌ [details]

#### 2. Task Breakdown (X/25)
- Tasks Listed: ✅/❌ [count]
- Estimates: ✅/❌ [details]
- Order: ✅/❌ [details]

#### 3. TDD Compliance (X/25)
- Test Files: ✅/❌ [count]
- Test-First Order: ✅/❌ [analysis]
- Coverage Ratio: X% (tests/total files)

**Commit Timeline:**
| Order | Commit | Type |
|-------|--------|------|
| 1 | abc1234 - Add tests | Test ✅ |
| 2 | def5678 - Implement | Code |
| ... | ... | ... |

#### 4. Code Review (X/25)
- Self-Review: ✅/❌
- Test Plan: ✅/❌
- Breaking Changes: ✅/❌/N/A
- Commit Quality: ✅/❌

---

### Files Changed

**Total:** X files (+Y/-Z lines)

| Type | Count | Files |
|------|-------|-------|
| Tests | X | file1.test.ts, ... |
| Implementation | Y | file2.ts, ... |
| Config | Z | ... |

---

### Recommendations

[List specific improvements for future PRs]

1. **[Category]:** [Specific recommendation]
2. **[Category]:** [Specific recommendation]

---

### Next Steps

If score < 80, run `/fix-pr $ARGUMENTS` to create improvement plan.
```

---

## Quality Thresholds

### Minimum Acceptable (Grade C = 70+)

- Brainstorm: At least scope defined
- Task Breakdown: Tasks listed (estimates optional)
- TDD: Tests exist (order flexible)
- Code Review: Test plan present

### Target Quality (Grade A = 90+)

- Brainstorm: Full analysis with alternatives
- Task Breakdown: Tasks with estimates
- TDD: Tests committed before implementation
- Code Review: Self-review + test plan + clean commits
