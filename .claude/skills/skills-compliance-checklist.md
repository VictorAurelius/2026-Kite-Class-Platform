# Skills Compliance Checklist

**Version:** 1.0
**Last Updated:** 2026-01-27
**Purpose:** Ensure ALL project skills are verified after EVERY PR before committing

---

## 📋 Overview

This skill provides a **MANDATORY CHECKLIST** to run after completing ANY PR to ensure 100% compliance with all project skills. This prevents violations such as:

- Missing QUICK-START.md updates
- Incorrect `@since` annotations
- Missing implementation plan tracking updates
- Missing skill documentation for new features
- Code style violations
- Missing tests

**CRITICAL:** This checklist MUST be completed BEFORE running the commit command for any PR.

---

## ✅ Mandatory Pre-Commit Checklist

### Phase 1: Code Quality & Standards

#### 1. Code Style (`code-style.md`)

**Verification Steps:**
```bash
# Check all new/modified Java files
git diff --name-only HEAD | grep "\.java$"
```

**Requirements:**
- [ ] All classes have JavaDoc with `@author` and `@since` tags
- [ ] `@since` version matches current PR version (e.g., PR 2.2 → `@since 2.2.0`)
- [ ] Method JavaDoc includes `@param`, `@return`, `@throws` where applicable
- [ ] Class names are PascalCase
- [ ] Method names are camelCase
- [ ] Constants are UPPER_SNAKE_CASE
- [ ] Package names are lowercase
- [ ] No magic numbers (use constants)
- [ ] Proper indentation (4 spaces, no tabs)

**Common Violations:**
- Wrong `@since` version (copy-paste from other files)
- Missing JavaDoc for new classes/methods
- Magic numbers in code

---

#### 2. Enums & Constants (`enums-constants.md`)

**Verification Steps:**
```bash
# Check for new enum files
git diff --name-only HEAD | grep "enum"
git diff --cached | grep -A 5 "enum.*{"
```

**Requirements:**
- [ ] All enums follow naming conventions (Status, Type, etc.)
- [ ] Enum values are UPPER_SNAKE_CASE
- [ ] Each enum has `getValue()` and `getDescription()` methods
- [ ] Enums have proper JavaDoc
- [ ] No magic strings for enum values in business logic

**Common Violations:**
- Missing `getValue()` or `getDescription()` methods
- Using string literals instead of enum constants

---

#### 3. Error Handling & Logging (`error-logging.md`)

**Verification Steps:**
```bash
# Check exception handling patterns
git diff --cached | grep -E "(catch|throw|Exception)"
```

**Requirements:**
- [ ] Custom exceptions extend appropriate base class
- [ ] All exceptions logged with proper level (ERROR, WARN, INFO)
- [ ] Sensitive data NOT logged (passwords, tokens)
- [ ] Stack traces logged for unexpected errors
- [ ] User-friendly error messages in responses
- [ ] Correlation IDs included in logs (if applicable)

**Common Violations:**
- Swallowing exceptions without logging
- Logging sensitive data
- Generic error messages to users

---

#### 4. API Design (`api-design.md`)

**Verification Steps:**
```bash
# Check for new REST controllers
git diff --name-only HEAD | grep "Controller\.java$"
```

**Requirements:**
- [ ] RESTful URL conventions (`/api/v1/resource`)
- [ ] Proper HTTP methods (GET, POST, PUT, DELETE, PATCH)
- [ ] Request/Response DTOs (not entities)
- [ ] Validation annotations (`@Valid`, `@NotNull`, etc.)
- [ ] Proper status codes (200, 201, 204, 400, 404, 500)
- [ ] API versioning in URL
- [ ] Pagination for list endpoints
- [ ] Standard response wrapper (`ApiResponse<T>`, `PageResponse<T>`)

**Common Violations:**
- Exposing entities directly
- Missing validation
- Inconsistent URL patterns

---

### Phase 2: Database & Dependencies

#### 5. Database Design (`database-design.md`)

**Verification Steps:**
```bash
# Check for new migrations
ls -lt src/main/resources/db/migration/ | head -10

# Check for new entities
git diff --name-only HEAD | grep "entity"
```

**Requirements:**
- [ ] Flyway migration files follow naming: `V{version}__{description}.sql`
- [ ] Entities have `BaseEntity` fields (if using common base)
- [ ] Foreign key constraints defined
- [ ] Indexes on frequently queried columns
- [ ] `@Column` annotations specify constraints
- [ ] No `ddl-auto=update` in production configs
- [ ] Migration tested locally

**Common Violations:**
- Missing migrations for schema changes
- Wrong migration version numbers
- Missing indexes

---

#### 6. Maven Dependencies (`maven-dependencies.md`)

**Verification Steps:**
```bash
# Check pom.xml changes
git diff pom.xml
```

**Requirements:**
- [ ] Spring Boot version: **3.5.10** (not 3.2.x)
- [ ] JJWT version: **0.12.6** (not 0.12.3)
- [ ] MapStruct version: **1.6.3** (not 1.5.5)
- [ ] SpringDoc version: **2.8.4** (not 2.3.0)
- [ ] No version conflicts (`mvn dependency:tree`)
- [ ] New dependencies justified and documented
- [ ] Test dependencies in `test` scope

**Common Violations:**
- Using outdated dependency versions
- Adding unnecessary dependencies
- Wrong scope for dependencies

---

### Phase 3: Testing

#### 7. Testing Guide (`testing-guide.md`)

**Verification Steps:**
```bash
# Run all tests
./mvnw clean test

# Check test coverage
./mvnw verify
```

**Requirements:**
- [ ] Unit tests for all new business logic
- [ ] Integration tests for new endpoints/features
- [ ] Test naming: `methodName_scenario_expectedBehavior`
- [ ] Tests use proper assertions (AssertJ or JUnit 5)
- [ ] Tests are independent (no order dependency)
- [ ] Mock external dependencies
- [ ] Test data follows patterns in skill
- [ ] All tests PASS before commit
- [ ] Code coverage ≥ 80% for new code

**Common Violations:**
- No tests for new features
- Tests that depend on execution order
- Not testing error cases
- Low code coverage

---

### Phase 4: Documentation

#### 8. Documentation Structure (`documentation-structure.md`)

**Verification Steps:**
```bash
# Check for documentation changes
git diff --name-only HEAD | grep "\.md$"
ls -la docs/
```

**Requirements:**
- [ ] README.md updated if project structure changed
- [ ] API documentation updated (if endpoints added)
- [ ] Architecture diagrams updated (if architecture changed)
- [ ] Migration guide updated (if breaking changes)
- [ ] Inline comments for complex logic

**Common Violations:**
- Missing documentation for new features
- Outdated README

---

#### 9. Update Documentation (`development-workflow.md` Part 4) ⚠️ CRITICAL

**Verification Steps:**
```bash
# Check QUICK-START.md exists and is updated
cat docs/QUICK-START.md | head -30
```

**Requirements:**
- [ ] **QUICK-START.md EXISTS** for the service (kiteclass-gateway, kiteclass-core, kiteclass-frontend)
- [ ] Current status section updated with latest PR
- [ ] Next recommended PR updated
- [ ] Test coverage numbers updated
- [ ] Completed PRs checklist updated
- [ ] Available options reflect current state
- [ ] File paths in "Important Files" section are correct
- [ ] "Last Updated" date is current

**Common Violations:**
- ❌ **QUICK-START.md missing entirely** (MOST CRITICAL)
- Outdated PR status
- Wrong test coverage numbers
- Outdated "Next" recommendation

---

#### 10. Update Plan Tracking (`development-workflow.md` Part 4.1) ⚠️ CRITICAL

**Verification Steps:**
```bash
# Check implementation plan tracking
cat /mnt/e/person/2026-Kite-Class-Platform/documents/scripts/kiteclass-implementation-plan.md | grep -A 30 "PROGRESS TRACKING"
```

**Requirements:**
- [ ] PR marked as ✅ COMPLETE (not 🚧 or ⏳)
- [ ] Service progress percentage updated
- [ ] Overall progress percentage updated
- [ ] PR description includes what was implemented
- [ ] Documentation status noted (if applicable)
- [ ] "Last Updated" and "Current Work" sections updated
- [ ] Added PR notes (if PR was added/changed from plan)

**Common Violations:**
- ❌ **PR marked as 🚧 instead of ✅** (CRITICAL)
- Wrong progress percentages
- Missing PR descriptions
- Outdated "Current Work"

---

#### 11. Create Skill Documentation (if applicable)

**Verification Steps:**
```bash
# Check if new feature needs skill documentation
ls -la .claude/skills/
```

**When to Create:**
- New module/feature with reusable patterns (e.g., email-service.md, auth-module.md)
- New architectural pattern introduced
- New technology/library integrated

**Requirements:**
- [ ] Skill file created in `.claude/skills/`
- [ ] Skill follows naming convention (kebab-case.md)
- [ ] Skill documents architecture, flows, patterns, configuration
- [ ] Skill includes examples and best practices
- [ ] Related skills cross-reference each other

**Common Violations:**
- ❌ **Missing skill for significant new feature** (e.g., PR 1.5 needed email-service.md)
- Insufficient detail in skill

---

### Phase 5: Git & Workflow

#### 12. Git Workflow (`development-workflow.md` Part 1)

**Verification Steps:**
```bash
# Check current branch
git branch -vv

# Check working tree
git status
```

**Requirements:**
- [ ] On correct feature branch (feature/gateway, feature/core, feature/frontend)
- [ ] All changes staged: `git add -A`
- [ ] No untracked files that should be committed
- [ ] No sensitive files staged (.env, credentials, etc.)
- [ ] Ready to commit with proper message format

**Common Violations:**
- Working on wrong branch
- Forgetting to stage new files
- Committing sensitive data

---

#### 13. PR & Commit Workflow (`development-workflow.md` Parts 1-2)

**Verification Steps:**
```bash
# Review staged changes
git diff --cached --stat
```

**Requirements:**
- [ ] All PR work completed and tested
- [ ] **THIS CHECKLIST COMPLETED** ✅
- [ ] Commit message follows format:
  ```
  <type>(<scope>): <description>

  Co-Authored-By: Claude Sonnet 4.5 <noreply@anthropic.com>
  ```
- [ ] Type is one of: feat, fix, docs, style, refactor, test, chore
- [ ] Scope matches service (gateway, core, frontend)
- [ ] Description is concise and clear

**Common Violations:**
- Wrong commit message format
- Missing Co-Authored-By
- Committing before checklist complete

---

### Phase 6: Feature-Specific (if applicable)

#### 14. Feature Development Checklist (`development-workflow.md` Part 2)

**When Applicable:** New feature implementation (not just refactoring/fixes)

**Requirements:**
- [ ] Feature matches acceptance criteria
- [ ] Edge cases handled
- [ ] Error cases handled
- [ ] Validation implemented
- [ ] Logging added
- [ ] Tests cover all scenarios
- [ ] Documentation updated

---

#### 15. I18n Messages (`frontend-development.md` Part 3)

**When Applicable:** User-facing messages added/changed

**Verification Steps:**
```bash
git diff --cached | grep -E "(messages\.properties|MessageSource)"
```

**Requirements:**
- [ ] All user messages in i18n files (not hardcoded)
- [ ] Keys follow naming convention
- [ ] All supported languages have translations
- [ ] No missing translation keys

---

#### 16. Theme System (`frontend-development.md` Part 2)

**When Applicable:** UI components added/changed (frontend PRs)

**Requirements:**
- [ ] Components use theme variables (not hardcoded colors)
- [ ] Dark mode supported
- [ ] Responsive design implemented
- [ ] Accessibility considered

---

#### 17. UI Components (`frontend-development.md` Part 1)

**When Applicable:** Reusable UI components added (frontend PRs)

**Requirements:**
- [ ] Component follows naming conventions
- [ ] PropTypes/TypeScript types defined
- [ ] Component documented (Storybook/README)
- [ ] Responsive and accessible

---

## 🔄 Complete Checklist Process

### Before Starting Any PR

1. Read the PR requirements from implementation plan
2. Read relevant skills (code-style, testing-guide, etc.)
3. Understand what needs to be done

### During PR Implementation

1. Follow relevant skills as you code
2. Write tests as you implement features
3. Run tests frequently: `./mvnw test`

### After PR Implementation (BEFORE COMMIT)

**Step 1:** Run full test suite
```bash
./mvnw clean verify
```
✅ All tests must PASS

**Step 2:** Review ALL modified files
```bash
git status
git diff
git diff --cached
```

**Step 3:** Go through THIS CHECKLIST SYSTEMATICALLY

- Check each applicable section
- Fix any violations found
- Re-run tests if code changed

**Step 4:** Update Documentation (CRITICAL)

- [ ] Update QUICK-START.md (current status, next PR, test coverage)
- [ ] Update implementation plan tracking (mark PR as ✅, update percentages)
- [ ] Create skill documentation if needed

**Step 5:** Final Verification

- [ ] All tests passing
- [ ] All checklist items verified
- [ ] Documentation updated
- [ ] No sensitive data in commit

**Step 6:** Commit

```bash
git add -A
git commit -m "feat(service): description

Co-Authored-By: Claude Sonnet 4.5 <noreply@anthropic.com>"
```

---

## 🚨 Common Violation Patterns

Based on past PRs, these are the MOST COMMON violations to watch for:

### Critical Violations

1. ❌ **Missing QUICK-START.md update** (PR 2.1 violation)
   - **Fix:** Always update QUICK-START.md after each PR
   - **Check:** `cat docs/QUICK-START.md | grep "PR X.Y"`

2. ❌ **PR marked 🚧 instead of ✅ in plan** (PR 2.1 violation)
   - **Fix:** Update plan tracking with ✅ when PR complete
   - **Check:** `cat documents/scripts/kiteclass-implementation-plan.md | grep "PR X.Y"`

3. ❌ **Wrong `@since` version** (PR 1.5 violation)
   - **Fix:** Use current PR version (PR 2.2 → `@since 2.2.0`)
   - **Check:** `git diff --cached | grep "@since"`

4. ❌ **Missing skill documentation** (PR 1.5 violation - email-service.md)
   - **Fix:** Create skill for significant new features
   - **Check:** Review `.claude/skills/` for missing patterns

### Moderate Violations

5. ⚠️ **Missing tests** for new features
   - **Fix:** Write unit + integration tests
   - **Check:** `./mvnw test`

6. ⚠️ **Outdated dependency versions**
   - **Fix:** Use versions from maven-dependencies.md skill
   - **Check:** `cat pom.xml | grep -A 1 "version"`

7. ⚠️ **Missing JavaDoc** for public methods
   - **Fix:** Add JavaDoc with @param, @return, @throws
   - **Check:** Manual review of new classes

### Minor Violations

8. ℹ️ **Inconsistent naming** (camelCase vs PascalCase)
   - **Fix:** Follow code-style.md conventions
   - **Check:** Manual review

9. ℹ️ **Magic numbers** in code
   - **Fix:** Extract to constants
   - **Check:** `git diff --cached | grep -E "[0-9]{2,}"`

---

## 📊 Skills Coverage Summary

| Category | Skills | Critical | Always Check |
|----------|--------|----------|--------------|
| **Code Quality** | code-style, enums-constants, error-logging | ⚠️ Yes | ✅ Every PR |
| **API & Design** | api-design, database-design | ⚠️ Yes | ✅ If API/DB changes |
| **Dependencies** | maven-dependencies | ⚠️ Yes | ✅ If pom.xml changed |
| **Testing** | testing-guide | ⚠️ Yes | ✅ Every PR |
| **Documentation** | documentation-structure, update-quick-start, update-plan-tracking | 🚨 CRITICAL | ✅ EVERY PR |
| **Git Workflow** | git-workflow, pr-commit-workflow | ⚠️ Yes | ✅ Every commit |
| **Feature-Specific** | feature-development-checklist, i18n-messages, theme-system, ui-components | ℹ️ If applicable | ✅ When applicable |
| **Architecture** | architecture-overview, auth-module, email-service | ℹ️ Reference | ✅ As needed |
| **Planning** | project-schedule, survey-interview-plan | ℹ️ Planning only | ✅ At project start |
| **Utilities** | fix-loop-escape, required-knowledge, environment-setup | ℹ️ Utilities | ✅ As needed |
| **Infrastructure** | cloud-infrastructure | ℹ️ Deployment | ✅ Deployment PRs only |

---

## 🎯 Quick Reference: Minimum Checklist

**For EVERY PR, you MUST verify these 5 critical items:**

1. ✅ **Tests passing:** `./mvnw clean verify`
2. ✅ **QUICK-START.md updated** with current PR status
3. ✅ **Implementation plan updated** with ✅ and progress %
4. ✅ **Code style compliant** (JavaDoc with correct `@since`)
5. ✅ **Ready to commit** with proper message format

**If ANY of these 5 items is missing, DO NOT COMMIT.**

---

## 📝 Integration with development-workflow.md

This checklist is referenced in `development-workflow.md` as a **required step before commit**.

**development-workflow.md includes:**

```markdown
## Step 3: Skills Compliance Verification ⚠️ REQUIRED

**BEFORE committing, you MUST complete the skills-compliance-checklist.md:**

1. Read `.claude/skills/skills-compliance-checklist.md`
2. Go through the checklist systematically
3. Fix any violations found
4. Verify the 5 critical minimum items

**DO NOT proceed to commit until checklist is 100% complete.**
```

---

## ✅ Checklist Complete Confirmation

When you have completed this entire checklist, you can proceed to commit with confidence that:

- ✅ All code follows project standards
- ✅ All tests pass
- ✅ All documentation is updated
- ✅ No violations of any project skills
- ✅ The PR is ready for review/merge

**Remember:** This checklist is not optional. It is a **MANDATORY** part of the development workflow to ensure consistent, high-quality code across the entire KiteClass platform.

---

**Last Updated:** 2026-01-27
**Related Skills:** development-workflow.md, frontend-development.md, code-style.md, testing-guide.md, troubleshooting.md
**Author:** KiteClass Team
