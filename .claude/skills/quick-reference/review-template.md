# Code Review Template — Copy-Paste Ready

```markdown
## Code Review: PR #XX — [Feature Name]

**Reviewer:** [Name]  **Date:** YYYY-MM-DD

---

### Stage 1: Specification Compliance ✅/❌

**Requirements Verification:**
1. [Requirement 1]: ✅/❌ [Note]
2. [Requirement 2]: ✅/❌ [Note]
3. [Requirement 3]: ✅/❌ [Note]

**Edge Cases:**
- Null/empty inputs: ✅/❌
- Invalid data: ✅/❌
- Error handling: ✅/❌
- Multi-tenant isolation: ✅/❌ / N/A

**File Locations:**
- Code files: ✅/❌
- Test files: ✅/❌

**API Contracts:**
- DTOs match design: ✅/❌
- HTTP status codes correct: ✅/❌

**Tests:**
- All criteria covered: ✅/❌
- Tests pass (CI green): ✅/❌

**Stage 1 Outcome:** ✅ PASS / ❌ FAIL

> If FAIL, list issues below and STOP (don't proceed to Stage 2):
> - [Issue 1]
> - [Issue 2]

---

### Stage 2: Code Quality (Only if Stage 1 PASS)

**🔴 Critical Issues (BLOCKING):**
- Security vulnerabilities: ✅ None / ❌ [issue]
- Data loss risks: ✅ None / ❌ [issue]
- Breaking API changes: ✅ None / ❌ [issue]
- Auth bypasses: ✅ None / ❌ [issue]
- Multi-tenant data leak: ✅ None / ❌ [issue]

**🟠 Major Issues (Recommended):**
1. [Issue — file:line — recommendation]
2. [Issue — file:line — recommendation]

**🟡 Minor Issues (Optional):**
1. [Issue — file:line — recommendation]
2. [Issue — file:line — recommendation]

---

### Stage 2 Outcome:

- [ ] ✅ APPROVE — no critical/major issues
- [ ] 🟠 APPROVE with recommendations — major issues noted, non-blocking
- [ ] 🔴 BLOCK — critical issues must be fixed first

**Summary:**
[What's good, what needs work, overall assessment]

**Next Steps:**
- [ ] [Action if any]
```

---

## Filled Example: PR 2.15 Student CRUD

```markdown
## Code Review: PR #215 — Student CRUD Endpoints

**Reviewer:** Claude Code  **Date:** 2026-03-13

### Stage 1: ✅ PASS

Requirements: POST ✅, GET ✅, PUT ✅, DELETE (soft) ✅
Edge cases: null inputs ✅, duplicate email ✅, multi-tenant ✅
Files: correct locations ✅, tests in kiteclass-core/test ✅
API: DTOs match ✅, 201/200/404 correct ✅
Tests: 18 tests, all green ✅

### Stage 2: Code Quality

🔴 Critical: None ✅
🟠 Major:
1. Test coverage 75% (target 80%) — missing: `updateStudent_WithSameData_ShouldNotUpdateTimestamp`
🟡 Minor:
1. Line 45: `updateStudent()` missing JavaDoc

### Outcome: 🟠 APPROVE with recommendations

Solid implementation! All requirements met, good test coverage overall.
Recommend: add edge case test for update-no-change scenario (non-blocking).
```
