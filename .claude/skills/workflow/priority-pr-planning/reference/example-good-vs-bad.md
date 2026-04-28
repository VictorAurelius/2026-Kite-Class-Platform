# Example: Good vs Bad Priority Plan + Summary

> Pointer: read this AFTER your first draft to compare against worked examples. Parent skill: `../SKILL.md`.

## 📝 Example: Good vs Bad Priority Plan

### ❌ BAD Priority Plan
```markdown
# Fix Bug

## Problem
Email not working

## Solution
Fix the code

## Testing
Run tests

## Commit
git commit -m "fix bug"
```

**Issues:**
- No quality standards referenced
- No root cause analysis
- No specific implementation steps
- No testing checklist
- No workflow references
- Incomplete commit message

---

### ✅ GOOD Priority Plan
```markdown
# Priority PRs Execution Plan - 2026-02-12

## ⚠️ PRIORITY 1: Fix Multi-Tenant Email Test

**Branch:** `fix/KC-001-multi-tenant-email-filter`
**Service:** Core
**Time:** 2 hours
**Ticket:** KC-001

### 1. Problem Statement

**Current Issue:**
- Test `createStudent_multipleTenantsWithSameEmail` is DISABLED
- Email uniqueness is GLOBAL (not scoped to tenant)
- Hibernate filter NOT working in test environment

**Root Cause:**
```java
// StudentServiceImpl.createStudent()
Optional<Student> existing = studentRepository
    .findByEmailAndDeletedFalse(email);
// ↑ Query should filter by tenantId but doesn't in tests
```

**Impact:**
- Multi-tenant isolation broken
- Different tenants cannot use same email
- Blocks PR 1.8 Gateway Cross-Service

### 2. Workflow: Feature Branch Creation

**Reference:** `.claude/skills/development-workflow.md` - Section "Branching Strategy"

```bash
git checkout main
git pull origin main
git checkout -b fix/KC-001-multi-tenant-email-filter
git branch --show-current
```

### 3. Implementation Steps

#### 3.1 Add Migration
**File:** `V6__add_multi_tenant_email_constraint.sql`
```sql
ALTER TABLE students
ADD CONSTRAINT uk_student_email_instance
UNIQUE (email, instance_id, deleted)
WHERE deleted = FALSE;
```

#### 3.2 Add Repository Method
**File:** `StudentRepository.java`
```java
/**
 * Finds student by email and instance_id for tenant-scoped uniqueness.
 *
 * @param email the email
 * @param instanceId the tenant ID
 * @return student if found
 * @since 2.13.0
 */
Optional<Student> findByEmailAndInstanceIdAndDeletedFalse(
    String email, UUID instanceId
);
```

[... detailed implementation continues ...]

### 4. Testing Checklist

**Reference:** `.claude/skills/testing-guide.md`

#### 4.1 Unit Tests
- Not needed (repository auto-generated)

#### 4.2 Integration Tests
```java
@Test  // ✅ Re-enabled
void createStudent_multipleTenantsWithSameEmail_shouldIsolateData() {
    // Create student in Tenant A
    // Create student with SAME email in Tenant B
    // Verify both exist with different instance_id
}

@Test  // New test
void createStudent_duplicateEmailInSameTenant_shouldFail() {
    // Create student in Tenant A
    // Try same email in SAME tenant → expect 409 Conflict
}
```

#### 4.3 Regression Tests
```bash
./mvnw clean test
# Expected: 235 tests (was 234, +1 new), 0 failures
```

### 5. Commit Strategy

**Reference:** `.claude/skills/development-workflow.md` - Section "Commit Messages"

```bash
git add .
git commit -m "$(cat <<'EOF'
fix(core): scope email uniqueness to tenant

Changes:
- Add V6 migration: composite unique (email, instance_id, deleted)
- Add repository: findByEmailAndInstanceIdAndDeletedFalse
- Update service: check uniqueness per tenant
- Re-enable test: createStudent_multipleTenantsWithSameEmail
- Add test: createStudent_duplicateEmailInSameTenant

Fixes:
- Multi-tenant isolation for email uniqueness
- Different tenants can use same email
- Same tenant enforces uniqueness

Tests: 235 passing (was 234, +1 new), 0 failures

Co-Authored-By: Claude Sonnet 4.5 <noreply@anthropic.com>
EOF
)"
```
<!-- TODO: verify against current state — current CLAUDE.md says do NOT add Co-Authored-By trailer; example kept verbatim from v1.0 monolith -->

### 6. CI Validation

**Reference:** `.claude/skills/development-workflow.md` - Section "Pull Request Process"

```bash
# Step 1: Push (ask user first)
git push -u origin fix/KC-001-multi-tenant-email-filter

# Step 2: Create PR
gh pr create \
  --title "fix(core): scope email uniqueness to tenant (KC-001)" \
  --body "$(cat <<'EOF'
## Summary
Fix multi-tenant email uniqueness by scoping to (email, instance_id).

## Problem
- Email uniqueness was GLOBAL
- Tenants could not use same email

## Solution
- Composite unique constraint
- Service checks per-tenant uniqueness

## Changes
- Migration: V6__add_multi_tenant_email_constraint.sql
- Repository: findByEmailAndInstanceIdAndDeletedFalse
- Service: Updated validation
- Tests: Re-enabled + 1 new

## Testing
- ✅ 235 tests passing (was 234)
- ✅ Multi-tenant isolation verified

## Checklist
- [x] Migration tested
- [x] All tests pass
- [x] No regression
- [x] CI green
EOF
)"

# Step 3: Monitor CI
scripts/check-ci.sh

# Step 4: Merge
gh pr merge --squash --delete-branch
```

### 7. Acceptance Criteria

#### Quality Standards (From Master Plan)
- [ ] **Code Coverage**: 80%+ service layer
- [ ] **JavaDoc**: Repository method documented
- [ ] **Multi-Tenant**: Composite constraint (email, instance_id)
- [ ] **Soft Delete**: Constraint WHERE deleted = FALSE
- [ ] **Error Handling**: Error code STUDENT_EMAIL_EXISTS
- [ ] **Validation**: Bean validation on DTOs

#### Testing
- [ ] **Unit Tests**: N/A (repository auto-generated)
- [ ] **Integration Tests**: 2 tests (re-enabled + new)
- [ ] **Edge Cases**: Duplicate in same tenant → 409
- [ ] **Multi-Tenant**: Different tenants same email → 201
- [ ] **Regression**: All 235 tests pass

#### CI & Documentation
- [ ] **CI Pipeline**: All checks green
- [ ] **Migration**: V6 applied successfully
- [ ] **No Warnings**: Zero compiler/deprecation warnings
- [ ] **STATUS-UPDATE**: Marked Priority 1 complete
- [ ] **Master Plan**: PR status updated
```

**Why This Is Good:**
- ✅ All quality standards referenced
- ✅ Root cause clearly explained
- ✅ Implementation detailed with code
- ✅ Testing comprehensive
- ✅ Workflow references skills
- ✅ Commit uses HEREDOC
- ✅ PR body complete
- ✅ Acceptance criteria thorough

---

## 🎓 Summary

**Key Takeaways:**

1. **Priority plans INHERIT all standards from master plan** - không tạo chuẩn mới
2. **Always reference `.claude/skills/`** - không tự viết workflow
3. **Quality gates NON-NEGOTIABLE** - 80% coverage, JavaDoc, multi-tenant, etc.
4. **Complete 7-section structure** - Problem → Workflow → Implementation → Testing → Commit → CI → Acceptance
5. **Validate before execution** - Use quality gate checklist

**Red Flags:**
- ❌ Plan không có quality standards section
- ❌ Plan tự tạo workflow thay vì reference skills
- ❌ Acceptance criteria quá ngắn (< 8 items)
- ❌ Testing checklist chỉ có "run tests"
- ❌ Commit message không dùng HEREDOC
- ❌ Không có root cause analysis

---

**Skill Version:** 1.0 (original monolith — superseded by 1.1 folder split)
**Created:** 2026-02-12
**Author:** KiteClass Development Team
**Related Skills:** `development-workflow.md`, `testing-guide.md`, `code-style.md`
