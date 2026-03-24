# Portable Skill — adapt {project} placeholders to your project

# Skill: Two-Stage Code Review

**Purpose:** Systematic code review — spec compliance first, then code quality.

---

## When to Use

**Mandatory:** All PRs with code changes before merging to main.

**Skip:** Documentation-only PRs, config-only changes (quick sanity check).

---

## Stage 1: Specification Compliance (15-20 min) — BLOCKING

**Question:** Does this PR do what was asked?

### Checklist:

1. **Requirements Match**
   - [ ] Matches PR description exactly
   - [ ] All acceptance criteria implemented
   - [ ] No missing features (incomplete)
   - [ ] No extra features (scope creep)

2. **Edge Cases Coverage**
   - [ ] Handles null/empty inputs
   - [ ] Handles invalid data (validation)
   - [ ] Handles errors gracefully

3. **File Locations Match Plan**
   - [ ] Code files in correct locations
   - [ ] Test files in corresponding test directories

4. **API Contracts Match Design**
   - [ ] Request/Response DTOs match spec
   - [ ] HTTP status codes correct (200, 201, 400, 404)
   - [ ] Endpoint paths follow conventions

5. **Tests Prove Requirements Met**
   - [ ] Every acceptance criterion has a test
   - [ ] Tests actually verify the requirement
   - [ ] Tests pass (green)

**Outcome:** PASS -> proceed to Stage 2. FAIL -> BLOCK PR, return with specific issues.

**CRITICAL:** Do NOT review code quality if Stage 1 fails.

---

## Stage 2: Code Quality (20-30 min) — GRADED

**Question:** Is this code production-ready?

### CRITICAL Issues (Must Fix — BLOCKING)

- SQL injection / security vulnerabilities
- Data loss risks (missing @Transactional)
- Breaking API changes
- Authentication/authorization bypasses

### MAJOR Issues (Should Fix — Strong Recommendation)

- N+1 queries / performance problems
- Test coverage gaps (<80% on new code)
- Missing error handling (uncaught exceptions)
- Overly complex classes (>300 lines)

### MINOR Issues (Nice to Have — Non-Blocking)

- Naming improvements
- Code duplication
- Missing documentation
- Style inconsistencies

### Outcome:

- APPROVE: No critical/major issues
- APPROVE with recommendations: Major issues noted, not blocking
- BLOCK: Critical issues must be fixed

---

## Review Template

```markdown
## Code Review: PR #XX - [Feature Name]

### Stage 1: Specification Compliance [PASS/FAIL]

**Requirements:** [List each, mark PASS/FAIL]
**Edge Cases:** [Covered?]
**API Contracts:** [Match spec?]
**Tests:** [Prove requirements?]

### Stage 2: Code Quality (only if Stage 1 PASS)

**Critical Issues:** [None / List]
**Major Issues:** [None / List with file:line]
**Minor Issues:** [None / List]

### Outcome: [APPROVE / APPROVE with recommendations / BLOCK]

**Summary:** [1-2 sentences]
**Next Steps:** [Actions]
```

---

## Quick Reference Checklist

**Stage 1 (MUST PASS):**
- [ ] All requirements implemented
- [ ] Edge cases covered
- [ ] Files in correct locations
- [ ] API contracts match design
- [ ] Tests prove requirements met

**Stage 2 (GRADED):**
- [ ] No CRITICAL issues (security, data loss, breaking changes)
- [ ] Minimal MAJOR issues (performance, coverage, error handling)
- [ ] MINOR issues noted for follow-up

---

## Success Metrics

- Stage 1 first-time pass rate: >80%
- Review iterations per PR: <=2
- Total review time: <45 min
