# Common Pitfalls to Avoid

> Pointer: read once before drafting your first priority plan. Parent skill: `../SKILL.md`.

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
