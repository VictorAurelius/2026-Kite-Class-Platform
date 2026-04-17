# Review Stage Decision Tree

**Quick Reference** - Two-Stage Code Review Process

---

## Decision Flow

```
                    START REVIEW
                         │
                         ▼
        ┌────────────────────────────────┐
        │   STAGE 1: SPEC COMPLIANCE     │
        │      (15-20 minutes)           │
        └────────────────────────────────┘
                         │
          ┌──────────────┴──────────────┐
          │                             │
      ❌ FAIL                        ✅ PASS
          │                             │
          ▼                             ▼
    ┌──────────┐              ┌──────────────────┐
    │ RETURN   │              │   STAGE 2:       │
    │    TO    │              │ CODE QUALITY     │
    │ DEVELOPER│              │  (20-30 min)     │
    └──────────┘              └──────────────────┘
                                       │
                    ┌──────────────────┼──────────────────┐
                    │                  │                  │
                🔴 CRITICAL        🟠 MAJOR           🟡 MINOR
                    │                  │                  │
                    ▼                  ▼                  ▼
              ┌──────────┐      ┌──────────┐      ┌──────────┐
              │  BLOCK   │      │ APPROVE  │      │ APPROVE  │
              │          │      │   WITH   │      │          │
              │          │      │  NOTES   │      │          │
              └──────────┘      └──────────┘      └──────────┘
```

---

## Stage 1: Specification Compliance 🔴 BLOCKING

### Checklist (ALL must pass)

**1. Requirements Match**
- [ ] Matches PR description exactly
- [ ] All acceptance criteria implemented
- [ ] No missing features
- [ ] No extra features (scope creep)

**2. Edge Cases**
- [ ] Null/empty input handling
- [ ] Invalid data validation
- [ ] Error handling present
- [ ] Multi-tenant isolation (if applicable)

**3. File Locations**
- [ ] Code in correct directories
- [ ] Tests in corresponding test dirs
- [ ] No unexpected changes

**4. API Contracts**
- [ ] DTOs match api-design.md
- [ ] HTTP status codes correct
- [ ] Endpoint paths follow conventions

**5. Tests Prove Requirements**
- [ ] Every criterion has test
- [ ] Tests verify actual requirement
- [ ] All tests pass ✅
- [ ] Coverage ≥ 80%

### Decision

```
IF any checkbox is unchecked:
    RETURN "❌ FAIL - Stage 1"
    LIST specific issues
    BLOCK PR (do NOT proceed to Stage 2)
ELSE:
    PROCEED to Stage 2
```

---

## Stage 2: Code Quality 🟠🟡 GRADED

### 🔴 Critical Issues (BLOCKING)

**Immediately BLOCK if found:**
- Security vulnerabilities (SQL injection, XSS, auth bypass)
- Data loss risks (missing transactions, incorrect deletes)
- Breaking changes (API changes without versioning)
- Secrets in code/logs
- Financial data exposure

**Decision:** Any CRITICAL → 🔴 BLOCK PR

---

### 🟠 Major Issues (RECOMMENDED)

**Strong recommendation to fix:**
- N+1 queries or performance problems
- Missing error handling
- Class size >300 lines
- Code duplication (violates DRY)
- Test coverage <80%

**Decision:** MAJOR issues → 🟠 APPROVE with recommendations

---

### 🟡 Minor Issues (OPTIONAL)

**Nice to have improvements:**
- Unclear naming
- Missing JavaDoc
- Code style inconsistencies
- Commented-out code
- TODO comments

**Decision:** MINOR only → ✅ APPROVE (note improvements)

---

## Outcome Summary

| Stage 1 | Stage 2 Critical | Stage 2 Major | Stage 2 Minor | OUTCOME |
|---------|------------------|---------------|---------------|---------|
| ❌ FAIL | - | - | - | 🔴 **BLOCK** - Fix requirements first |
| ✅ PASS | 🔴 Found | - | - | 🔴 **BLOCK** - Security/data issues |
| ✅ PASS | ✅ None | 🟠 Found | - | 🟠 **APPROVE** with strong recommendations |
| ✅ PASS | ✅ None | ✅ None | 🟡 Found | ✅ **APPROVE** (note minor improvements) |
| ✅ PASS | ✅ None | ✅ None | ✅ None | ✅ **APPROVE** - Excellent work! |

---

## Example Review Flow

### Scenario 1: Stage 1 Failure

```
Reviewer checks Stage 1:
- [x] Requirements match
- [ ] Edge cases covered  ❌ No null check for student email
- [x] File locations correct
- [x] API contracts match
- [ ] Tests prove requirements  ❌ Missing test for duplicate email

OUTCOME: ❌ FAIL Stage 1
ACTION: Return to developer with issues
DO NOT proceed to Stage 2
```

### Scenario 2: Stage 1 Pass, Critical Issue in Stage 2

```
Stage 1: ✅ PASS

Reviewer checks Stage 2:
🔴 CRITICAL: SQL injection in findByName() method
   Line 45: Uses string concatenation instead of parameterized query

OUTCOME: 🔴 BLOCK - Critical security issue
ACTION: Developer must fix before merge
```

### Scenario 3: Stage 1 Pass, Major Issue in Stage 2

```
Stage 1: ✅ PASS

Reviewer checks Stage 2:
✅ No critical issues
🟠 MAJOR: N+1 query in getStudentsWithCourses()
   Recommendation: Add @EntityGraph annotation

OUTCOME: 🟠 APPROVE with recommendations
ACTION: Create follow-up issue for performance optimization
PR can merge, but note performance concern
```

### Scenario 4: Perfect PR

```
Stage 1: ✅ PASS (all requirements met)

Stage 2:
✅ No critical issues
✅ No major issues
🟡 MINOR: Missing JavaDoc on public method (optional)

OUTCOME: ✅ APPROVE
ACTION: Merge! Optional: Add JavaDoc in next PR touching this file
```

---

## Time Estimates

| Stage | Time | Activity |
|-------|------|----------|
| Stage 1 | 15-20 min | Verify requirements, tests, contracts |
| Stage 2 Critical | 5 min | Security/data loss scan |
| Stage 2 Major | 10 min | Performance, error handling, coverage |
| Stage 2 Minor | 5 min | Style, naming, docs |
| **Total** | **40-50 min** | Complete review |

---

## Common Mistakes

### ❌ Reviewing Code Quality Before Spec Compliance
```
Reviewer starts with:
"This method naming is unclear"  ❌ WRONG - Stage 2 issue

Should check FIRST:
"Does this implement requirement X?"  ✅ CORRECT - Stage 1
```

### ❌ Blocking on Minor Issues
```
Reviewer blocks PR because:
"Missing JavaDoc on line 45"  ❌ WRONG - Minor issue, non-blocking

Should:
✅ APPROVE with note: "Consider adding JavaDoc"  ✅ CORRECT
```

### ❌ Approving Critical Issues
```
Reviewer approves despite:
"SQL injection on line 78 - fix in follow-up"  ❌ WRONG - Critical = BLOCK

Should:
🔴 BLOCK immediately - security cannot wait  ✅ CORRECT
```

---

## Success Criteria

- ✅ Stage 1 completed BEFORE Stage 2
- ✅ Critical issues always block (no exceptions)
- ✅ Major issues documented with recommendations
- ✅ Minor issues noted but don't block
- ✅ Review completed in 40-50 min

---

## Quick Decision Guide

**Ask yourself:**

1️⃣ "Does it do what was asked?" (Stage 1)
   - NO → ❌ FAIL, return to developer
   - YES → Proceed to #2

2️⃣ "Is there a security/data risk?" (Stage 2 Critical)
   - YES → 🔴 BLOCK immediately
   - NO → Proceed to #3

3️⃣ "Are there performance/quality concerns?" (Stage 2 Major)
   - YES → 🟠 APPROVE with recommendations
   - NO → Proceed to #4

4️⃣ "Any style/doc improvements?" (Stage 2 Minor)
   - YES → ✅ APPROVE, note improvements
   - NO → ✅ APPROVE, excellent work!

---

**Reference:** `.claude/skills/two-stage-code-review.md`
**Template:** `.github/PULL_REQUEST_TEMPLATE.md`
**Target:** Reduce review iterations from 2.5 → 2.0
