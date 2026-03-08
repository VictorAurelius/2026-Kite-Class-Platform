# Skill: Priority PR Planning

**Version:** 1.0
**Last Updated:** 2026-02-12
**Purpose:** Standards for creating temporary priority PR plans that comply with master implementation plan

---

## 📋 Overview

Skill này định nghĩa cách tạo **temporary priority PR plans** để thực hiện các PR urgent/critical trong khi vẫn tuân thủ 100% chuẩn từ **master implementation plan**.

**Use Cases:**
- 🚨 Critical bugs cần fix ngay
- ⚡ Unblocked PRs cần prioritize
- 🔧 Technical debt cần giải quyết urgent
- 📊 Phân tích lệch chuẩn sau manual testing

---

## 🎯 Core Principle

> **Priority plans PHẢI tuân thủ TẤT CẢ quality standards từ master plan**

```
┌─────────────────────────────────────────────────────────────────┐
│                   MASTER IMPLEMENTATION PLAN                     │
│                 (documents/.../kiteclass-implementation-plan.md) │
│                                                                  │
│  Quality Standards (NON-NEGOTIABLE):                            │
│  ✅ Backend: 80% coverage, JavaDoc, multi-tenant, soft delete  │
│  ✅ Frontend: TypeScript strict, React Testing Library         │
│  ✅ Security: Input validation, HMAC auth, tenant isolation    │
│  ✅ Testing: Unit + Integration + Multi-tenant tests           │
│                                                                  │
└──────────────────────────┬──────────────────────────────────────┘
                           │
                           │ INHERIT ALL STANDARDS
                           ▼
┌─────────────────────────────────────────────────────────────────┐
│                    PRIORITY PR PLAN                              │
│              (PRIORITY-PLAN-YYYY-MM-DD.md)                       │
│                                                                  │
│  Must include:                                                  │
│  ✅ All quality standards from master plan                      │
│  ✅ Workflow references to .claude/skills/                      │
│  ✅ Complete acceptance criteria                                │
│  ✅ Testing checklist (unit + integration + regression)         │
│  ✅ CI validation steps                                         │
│                                                                  │
└─────────────────────────────────────────────────────────────────┘
```

---

## ✅ Mandatory Compliance Checklist

### 1. Quality Standards (From Master Plan)

**Every priority PR MUST comply with:**

#### Backend Quality Standards
- [ ] **Code Coverage**: Minimum 80% for service layer (JaCoCo)
- [ ] **Test Types**: Unit tests (Mockito) + Integration tests (Testcontainers)
- [ ] **No Warnings**: Zero compiler warnings, zero deprecation warnings
- [ ] **JavaDoc**: All public methods with `@param`, `@return`, `@throws`
- [ ] **Error Handling**: Error codes from `messages.properties`
- [ ] **Validation**: Jakarta Bean Validation on DTOs
- [ ] **Multi-Tenant**: All entities with `instance_id` + Hibernate filters
- [ ] **Soft Delete**: `deleted` flag + `...AndDeletedFalse` repository methods
- [ ] **Audit Fields**: `createdAt`, `updatedAt`, `createdBy`, `updatedBy`
- [ ] **Git Hooks**: Pre-commit checks pass

**Reference:** `.claude/skills/code-style.md`, `testing-guide.md`, `spring-boot-testing-quality.md`

#### Frontend Quality Standards
- [ ] **TypeScript Strict**: No `any` type, all props typed
- [ ] **Component Structure**: Proper UI/container/hooks separation
- [ ] **Testing**: React Testing Library for all components
- [ ] **Accessibility**: ARIA labels, semantic HTML, keyboard nav
- [ ] **Error Handling**: User-friendly messages, loading states
- [ ] **API Integration**: React Query with proper cache
- [ ] **State Management**: Zustand/Context properly
- [ ] **Form Validation**: Zod schemas with clear errors
- [ ] **Feature Gates**: Use `<FeatureGate>` for tier features
- [ ] **Responsive**: Mobile-first, all screen sizes

**Reference:** `.claude/skills/frontend-development.md`, `frontend-code-quality.md`

#### Security Standards
- [ ] **Input Validation**: Validate at API and UI layers
- [ ] **SQL Injection**: Parameterized queries (automatic with Spring Data JPA)
- [ ] **XSS Prevention**: Escape output, store raw in DB
- [ ] **Authentication**: JWT with refresh mechanism
- [ ] **Authorization**: RBAC enforcement
- [ ] **Multi-Tenant Isolation**: Hibernate filters
- [ ] **Internal APIs**: HMAC-SHA256 signatures
- [ ] **Sensitive Data**: Never commit secrets

**Reference:** `.claude/skills/architecture-overview.md`, `cross-service-data-strategy.md`

#### Testing Standards
- [ ] **Unit Tests**: Fast, isolated, mocked dependencies
- [ ] **Integration Tests**: Real DB with Testcontainers
- [ ] **API Tests**: Full HTTP request/response cycle
- [ ] **Edge Cases**: Validation errors, boundaries, nulls
- [ ] **Multi-Tenant Tests**: Tenant isolation verified
- [ ] **Error Scenarios**: 4xx and 5xx responses tested
- [ ] **CI Pipeline**: All tests pass in GitHub Actions

**Reference:** `.claude/skills/testing-guide.md`

---

### 2. Workflow Compliance (From Skills)

**Every priority PR MUST follow:**

#### Git Workflow
- [ ] **Branch Naming**: `{type}/KC-{id}-{short-desc}` (lowercase, `-`, ticket ID)
  - Examples: `fix/KC-001-multi-tenant-email`, `feature/KC-002-gateway-integration`
- [ ] **Branch From**: Always branch from `main` (after `git pull origin main`)
- [ ] **Commit Format**: Conventional Commits (type(scope): subject)
- [ ] **Commit Message**: HEREDOC for complex commits with body
- [ ] **Co-Authored-By**: ALWAYS include for AI assistance

**Reference:** `.claude/skills/development-workflow.md` - Section "Branching Strategy"

#### Pull Request Process
- [ ] **PR Title**: `{type}({service}): {description} (KC-{id})`
- [ ] **PR Body**: Summary, Problem, Solution, Changes, Testing, Checklist, References
- [ ] **PR Creation**: Use `gh pr create` with detailed body
- [ ] **CI Monitoring**: `gh run watch` to monitor tests
- [ ] **Merge Strategy**: `gh pr merge --squash --delete-branch`

**Reference:** `.claude/skills/development-workflow.md` - Section "Pull Request Process"

---

### 3. Documentation Compliance

**Every priority PR MUST update:**

- [ ] **STATUS-UPDATE-YYYY-MM-DD.md**: Mark PR as complete, update progress %
- [ ] **Master Implementation Plan**: Update PR status (✅ done)
- [ ] **MEMORY.md**: Add lessons learned if applicable
- [ ] **README**: Update if adding new features/endpoints

---

## 📝 Priority Plan Template Structure

### Required Sections

```markdown
# Priority PRs Execution Plan - YYYY-MM-DD

## 📋 OVERVIEW
- Priority Queue (list all PRs in order)
- Prerequisites Checklist
- Estimated total time

## ⚠️ PRIORITY 1: [PR Name]
**Branch:** {type}/KC-{id}-{desc}
**Service:** Core/Gateway/Frontend
**Time:** X hours
**Ticket:** KC-{id}

### 1. Problem Statement
- Clear description of the issue
- Root cause analysis
- Impact assessment

### 2. Workflow: Feature Branch Creation
**Reference:** `.claude/skills/development-workflow.md` - Section "Branching Strategy"

```bash
git checkout main
git pull origin main
git checkout -b {branch-name}
git branch --show-current
```

### 3. Implementation Steps
- Detailed code changes for each file
- Complete with file paths
- Include JavaDoc/comments
- Reference skills where applicable

### 4. Testing Checklist
**Reference:** `.claude/skills/testing-guide.md`

#### 4.1 Unit Tests
- Test cases with expected results

#### 4.2 Integration Tests
- @SpringBootTest or @WebMvcTest patterns
- TestContainers setup if needed

#### 4.3 Regression Tests
```bash
./mvnw clean test
# Expected: X tests, 0 failures
```

### 5. Commit Strategy
**Reference:** `.claude/skills/development-workflow.md` - Section "Commit Messages"

```bash
git add .
git commit -m "$(cat <<'EOF'
{type}({service}): {subject}

Changes:
- Detail 1
- Detail 2

Tests: X passing (was Y, +Z new)

Co-Authored-By: Claude Sonnet 4.5 <noreply@anthropic.com>
EOF
)"
```

### 6. CI Validation
```bash
# Ask user first
git push -u origin {branch-name}

# Create PR
gh pr create --title "..." --body "..."

# Monitor
gh run watch

# Merge when green
gh pr merge --squash --delete-branch
```

### 7. Acceptance Criteria
- [ ] Quality standards checklist (copy from above)
- [ ] All tests passing
- [ ] CI green
- [ ] Documentation updated
- [ ] No regression

## 📊 EXECUTION SUMMARY
- Timeline table
- Success metrics
- Next steps after completion
```

---

## 🔍 Validation Checklist for Priority Plans

Before executing a priority plan, verify:

### Plan Completeness
- [ ] All quality standards from master plan included
- [ ] All workflow references point to `.claude/skills/`
- [ ] Each PR has complete 7-section structure
- [ ] Testing checklist covers unit + integration + regression
- [ ] Commit messages follow Conventional Commits
- [ ] CI validation steps with gh CLI
- [ ] Acceptance criteria comprehensive

### Code Quality
- [ ] Migration files have comments and version numbers
- [ ] Repository methods have JavaDoc
- [ ] Service layer has proper error handling
- [ ] Tests cover happy path + edge cases + errors
- [ ] Multi-tenant isolation verified in tests

### Documentation
- [ ] Problem statement clear and actionable
- [ ] Implementation steps detailed enough to execute
- [ ] References to skills documented
- [ ] Next steps after completion defined

---

## 🚨 Common Pitfalls to Avoid

### ❌ Don't Do This

1. **Skipping Quality Standards**
   ```markdown
   # ❌ BAD
   ## Testing
   - Run tests

   # ✅ GOOD
   ## Testing Checklist
   **Reference:** `.claude/skills/testing-guide.md`

   ### Unit Tests
   - Test X with input Y expects Z

   ### Integration Tests
   - @SpringBootTest with TestContainers
   - Verify database persistence

   ### Regression Tests
   ```bash
   ./mvnw clean test
   # Expected: 234 tests, 0 failures
   ```
   ```

2. **Creating New Workflow Instead of Referencing Skill**
   ```markdown
   # ❌ BAD
   ## My Custom Git Workflow
   1. Create branch however you want
   2. Commit with any message

   # ✅ GOOD
   ## Workflow: Feature Branch Creation
   **Reference:** `.claude/skills/development-workflow.md` - Section "Branching Strategy"

   ```bash
   git checkout -b fix/KC-001-issue-desc
   ```
   ```

3. **Incomplete Acceptance Criteria**
   ```markdown
   # ❌ BAD
   ## Acceptance Criteria
   - [ ] Tests pass

   # ✅ GOOD
   ## Acceptance Criteria
   - [ ] Code Coverage: 80%+ for service layer
   - [ ] JavaDoc: All public methods documented
   - [ ] Multi-Tenant: instance_id set correctly
   - [ ] Soft Delete: deleted flag used
   - [ ] Tests: 235 passing (was 234, +1 new)
   - [ ] CI: All checks green
   - [ ] Regression: No existing tests broken
   - [ ] Manual Testing: Multi-tenant isolation verified
   ```

4. **Missing Root Cause Analysis**
   ```markdown
   # ❌ BAD
   ## Problem
   - Test is failing

   # ✅ GOOD
   ## Problem Statement
   **Current Issue:**
   - Test `createStudent_multipleTenantsWithSameEmail` is DISABLED
   - Email uniqueness is GLOBAL (not scoped to tenant)

   **Root Cause:**
   ```java
   // StudentServiceImpl.createStudent()
   Optional<Student> existing = studentRepository.findByEmailAndDeletedFalse(email);
   // ↑ This query SHOULD be filtered by tenantId but ISN'T
   ```

   **Impact:**
   - Multi-tenant isolation broken
   - Tenants cannot use same email
   ```

---

## 📊 Priority Plan Compliance Matrix

| Requirement | Master Plan Source | Priority Plan Location | Status |
|-------------|-------------------|------------------------|--------|
| **Backend Quality** | Lines 85-96 | Section 7: Acceptance Criteria | ✅ Required |
| **Frontend Quality** | Lines 99-111 | Section 7: Acceptance Criteria | ✅ Required |
| **Security Standards** | Lines 113-122 | Section 7: Acceptance Criteria | ✅ Required |
| **Testing Standards** | Lines 125-134 | Section 4: Testing Checklist | ✅ Required |
| **Git Workflow** | `development-workflow.md` | Section 2: Workflow | ✅ Required |
| **Commit Format** | `development-workflow.md` | Section 5: Commit Strategy | ✅ Required |
| **PR Process** | `development-workflow.md` | Section 6: CI Validation | ✅ Required |
| **Multi-Tenant** | Architecture skill | Implementation + Tests | ✅ Required |
| **Soft Delete** | Architecture skill | Implementation + Tests | ✅ Required |
| **Error Codes** | `error-logging.md` | Implementation + Tests | ✅ Required |

---

## 🎯 When to Create a Priority Plan

### ✅ CREATE Priority Plan When:
1. **Critical Bug Discovered**
   - Blocks production deployment
   - Breaks core functionality
   - Security vulnerability

2. **PR Becomes Unblocked**
   - Dependency PR completed
   - External service ready
   - Blocker resolved

3. **Technical Debt Critical**
   - Accumulated warnings breaking build
   - Deprecated APIs need immediate upgrade
   - Performance bottleneck urgent

4. **Manual Testing Reveals Issues**
   - Multi-tenant isolation broken
   - Cross-service integration failing
   - CI not catching real bugs

### ❌ DON'T CREATE Priority Plan When:
1. **Regular PR in Queue**
   - Follow master plan order
   - No urgent reason to skip ahead

2. **Minor Refactoring**
   - Can be bundled with next PR
   - No functional impact

3. **Documentation Only**
   - Use regular commit, no special plan needed

---

## 📚 Related Skills Reference

**MUST READ before creating priority plans:**
- `.claude/skills/development-workflow.md` - Git, commits, PRs
- `.claude/skills/testing-guide.md` - Test patterns
- `.claude/skills/code-style.md` - Coding standards
- `.claude/skills/architecture-overview.md` - Multi-tenant, security
- `.claude/skills/cross-service-data-strategy.md` - Integration patterns

**Master Plan Location:**
- `documents/03-planning/implementation/kiteclass-implementation-plan.md`

**Status Updates Location:**
- `documents/03-planning/implementation/STATUS-UPDATE-YYYY-MM-DD.md`

---

## ✅ Quality Gate: Plan Review Checklist

Before executing a priority plan, review:

### Compliance
- [ ] All quality standards from master plan copied verbatim
- [ ] All workflow steps reference `.claude/skills/` (not custom workflow)
- [ ] Branch naming follows convention: `{type}/KC-{id}-{desc}`
- [ ] Commit format uses Conventional Commits with HEREDOC
- [ ] PR creation uses `gh pr create` with detailed body

### Completeness
- [ ] Problem statement with root cause analysis
- [ ] Implementation steps with file paths and code
- [ ] Testing checklist: unit + integration + regression
- [ ] Commit strategy with HEREDOC example
- [ ] CI validation with gh CLI commands
- [ ] Acceptance criteria comprehensive (8+ items)

### Code Quality
- [ ] JavaDoc for all public methods
- [ ] Error codes from `messages.properties`
- [ ] Multi-tenant with `instance_id` + Hibernate filters
- [ ] Soft delete with `deleted` flag
- [ ] Audit fields: `createdAt`, `updatedAt`, `createdBy`, `updatedBy`
- [ ] Input validation with Jakarta Bean Validation

### Testing
- [ ] Unit tests with mocked dependencies
- [ ] Integration tests with Testcontainers
- [ ] Multi-tenant isolation tests
- [ ] Error scenario tests (4xx, 5xx)
- [ ] Regression: all existing tests still pass

### Documentation
- [ ] Clear problem statement
- [ ] Detailed implementation steps
- [ ] References to skills documented
- [ ] Next steps defined
- [ ] Links to master plan PRs

---

## 🔄 Execution Workflow

```
┌─────────────────────────────────────────────────────────────────┐
│ 1. ANALYZE ISSUE                                                 │
│    - Root cause identified                                       │
│    - Impact assessed                                             │
│    - Priority justified                                          │
└────────────────────────────┬────────────────────────────────────┘
                             ▼
┌─────────────────────────────────────────────────────────────────┐
│ 2. CREATE PRIORITY PLAN                                          │
│    ✅ Copy quality standards from master plan                    │
│    ✅ Reference .claude/skills/ for workflow                     │
│    ✅ Include all 7 required sections                            │
│    ✅ Complete acceptance criteria                               │
└────────────────────────────┬────────────────────────────────────┘
                             ▼
┌─────────────────────────────────────────────────────────────────┐
│ 3. VALIDATE PLAN                                                 │
│    ✅ Quality gate checklist (see above)                         │
│    ✅ Compliance matrix verified                                 │
│    ✅ All sections complete                                      │
└────────────────────────────┬────────────────────────────────────┘
                             ▼
┌─────────────────────────────────────────────────────────────────┐
│ 4. EXECUTE PLAN                                                  │
│    - Create branch                                               │
│    - Implement changes                                           │
│    - Write tests                                                 │
│    - Commit with proper message                                  │
│    - Push and create PR                                          │
│    - Monitor CI                                                  │
└────────────────────────────┬────────────────────────────────────┘
                             ▼
┌─────────────────────────────────────────────────────────────────┐
│ 5. POST-EXECUTION                                                │
│    ✅ Update STATUS-UPDATE-YYYY-MM-DD.md                         │
│    ✅ Update master implementation plan                          │
│    ✅ Update MEMORY.md if lessons learned                        │
│    ✅ Close ticket (KC-{id})                                     │
└─────────────────────────────────────────────────────────────────┘
```

---

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

# Step 3: Monitor
gh run watch

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

**Skill Version:** 1.0
**Created:** 2026-02-12
**Author:** KiteClass Development Team
**Related Skills:** `development-workflow.md`, `testing-guide.md`, `code-style.md`
