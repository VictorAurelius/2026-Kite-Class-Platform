# Portable Skill — adapt {project} placeholders to your project

# Skill: Systematic Debugging

**Purpose:** 4-phase root cause analysis for faster, more effective debugging.

---

## When to Use

**Use for:** Bug investigations, unexpected test behavior, production issues, integration failures, performance issues.

**Skip:** Typos/syntax errors, compiler errors with clear messages, known issues in docs.

---

## 4-Phase Process

### Phase 1: Reproduce (15-30 min)

**Goal:** Consistently trigger the bug in a controlled environment.

1. **Create failing test case** (Arrange-Act-Assert)
2. **Document exact steps** (user actions, system state, environment)
3. **Verify consistency** (run 3+ times, should fail reliably)
4. **Record environment** (framework version, DB state, cache state, external services)

```markdown
## Bug Reproduction
**Test:** {TestClass}#{testMethod}
**Consistent:** Yes/No (X/Y runs)
**Steps:**
1. [Setup step]
2. [Action step]
3. Expected: [what should happen]
4. Actual: [what actually happens]
```

### Phase 2: Trace (30-60 min)

**Goal:** Follow execution flow to find where behavior diverges from expected.

**Tools:** Debugger (breakpoints, step-through), debug logging at decision points, stack trace analysis (read bottom-up).

```
Request -> Controller -> Service -> Repository -> Database -> Response

Mark each step: PASS or FAIL
Find the DIVERGENCE POINT
```

### Phase 3: Root Cause (30-45 min)

**Goal:** Distinguish symptom from underlying cause using 5 Whys.

```
Symptom: [What went wrong]

Why 1: Why did [symptom] happen?
-> Because [reason]

Why 2: Why did [reason] happen?
-> Because [deeper reason]

... repeat until ROOT CAUSE found (usually 3-5 levels)
```

**Validate:** Search past issues, check recent changes in the area, look for similar patterns.

### Phase 4: Defensive Fix (1-2 hours)

**Goal:** Fix root cause AND prevent recurrence.

1. **Fix root cause** (not symptom — no band-aids)
2. **Add regression test** (proves fix works, prevents recurrence)
3. **Check related scenarios** (same pattern elsewhere?)
4. **Update documentation** (troubleshooting docs, known issues)

```markdown
## Fix Summary
**Changes:**
1. [What was fixed and where]
2. [Regression test added]
3. [Related fixes applied]
4. [Docs updated]
```

---

## Quick Reference Checklist

- [ ] **Phase 1:** Can I consistently reproduce the bug? (test case exists)
- [ ] **Phase 2:** Have I traced execution flow? (found divergence point)
- [ ] **Phase 3:** Did I identify root cause? (5 Whys applied, not just symptom)
- [ ] **Phase 4:** Did I add regression test? (prevents recurrence)
- [ ] **Phase 4:** Did I update documentation? (known issues, troubleshooting)

**If stuck:** Explain the problem out loud (rubber duck debugging) or pair with another developer.

---

## Success Metrics

- Time to reproduce: <30 min
- Time to root cause: <1 hour
- Time to fix: <1 hour
- Total debugging time: <2 hours (down from ~3 hours ad-hoc)
- Bug recurrence rate: <5% (regression tests prevent)
