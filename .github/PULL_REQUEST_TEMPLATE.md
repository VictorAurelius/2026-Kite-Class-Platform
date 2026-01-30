# Pull Request

## PR Information

**PR Type:** (chọn một)
- [ ] 🔒 Security Fix (Phase 1)
- [ ] ✅ Testing Implementation (Phase 2)
- [ ] 🎭 E2E Testing (Phase 3)
- [ ] ⚙️ CI/CD Setup (Phase 3)
- [ ] 📊 Performance Testing (Phase 4)
- [ ] 📚 Documentation (Phase 4)

**Related PR Plan:** `PR X.Y` từ `documents/plans/code-review-pr-plan.md`

**Assignee:** @username

**Estimated Effort:** X days

**Priority:**
- [ ] 🔴 P0 - Critical (MUST merge)
- [ ] 🟡 P1 - High (SHOULD merge)
- [ ] 🟢 P2 - Medium (NICE to merge)

---

## Description

### What does this PR do?

(Mô tả ngắn gọn những gì PR này làm)

### Why is this change needed?

(Tại sao cần thay đổi này? Link đến code quality audit findings nếu có)

### Related Issues/PRs

- Closes #XXX
- Depends on #YYY
- Related to #ZZZ

---

## Changes Made

### Files Changed

**Backend:**
- `path/to/file1.java` - (mô tả thay đổi)
- `path/to/file2.java` - (mô tả thay đổi)

**Frontend:**
- `path/to/component1.tsx` - (mô tả thay đổi)
- `path/to/component2.tsx` - (mô tả thay đổi)

**Tests:**
- `path/to/test1.java` - (X tests added)
- `path/to/test2.spec.ts` - (Y tests added)

### Test Coverage

**Before:**
- Backend: XX%
- Frontend: YY%

**After:**
- Backend: XX% (+Z%)
- Frontend: YY% (+Z%)

**New Tests Added:**
- Unit Tests: X tests
- Integration Tests: Y tests
- E2E Tests: Z tests

---

## Code Review Checklist

### KiteClass-Specific Checks (MANDATORY)

#### Multi-Tenant Security
- [ ] All repository queries include `instance_id` filter
- [ ] No hardcoded instance IDs (`UUID.fromString(...)`)
- [ ] Services use `TenantContext.getCurrentInstanceId()`
- [ ] Cross-tenant access tests added and passing
- [ ] API responses filtered by current tenant

#### Feature Detection
- [ ] STANDARD/PREMIUM endpoints have `@RequireFeature` annotation
- [ ] Feature config cached with 1-hour TTL
- [ ] Tier limits enforced (maxStudents, maxCourses, storageGB)
- [ ] API returns 403 Forbidden for unavailable features
- [ ] Feature detection tests added

#### Payment Security (if applicable)
- [ ] Amount validation matches tier pricing (499k/999k)
- [ ] Double-payment prevention implemented
- [ ] Order expiry validation (10-minute QR)
- [ ] Transaction idempotency guaranteed
- [ ] Audit logging for payment state changes
- [ ] No financial data in logs

#### Trial System (if applicable)
- [ ] Trial days remaining calculated correctly
- [ ] Grace period (3 days) enforced
- [ ] Instance suspended after grace period
- [ ] Status transitions validated
- [ ] Trial system tests added

#### General Security
- [ ] Input validation (`@Valid`, `@NotNull`, `@Size`)
- [ ] No SQL injection vulnerabilities (using JPA/JPQL)
- [ ] No XSS vulnerabilities (frontend sanitization)
- [ ] Authentication required for non-public endpoints
- [ ] Authorization enforced (role-based access)
- [ ] No secrets in code/logs

### General Quality Checks

- [ ] Code follows project style guidelines
- [ ] Self-reviewed code before requesting review
- [ ] Added/updated tests (coverage ≥80%)
- [ ] All tests passing locally
- [ ] No compilation warnings
- [ ] Updated documentation (if needed)
- [ ] No duplicate code
- [ ] Performance acceptable (no obvious bottlenecks)

### Testing Requirements

- [ ] Unit tests cover happy path
- [ ] Unit tests cover edge cases
- [ ] Unit tests cover error scenarios
- [ ] Integration tests for API endpoints (if applicable)
- [ ] E2E tests for critical flows (if applicable)
- [ ] All new tests passing
- [ ] No test warnings or flaky tests

---

## Testing Instructions

### How to Test This PR

1. **Setup:**
   ```bash
   # Commands to setup test environment
   git checkout <branch-name>
   ./mvnw clean install
   ```

2. **Run Tests:**
   ```bash
   # Backend tests
   ./mvnw test

   # Frontend tests
   npm run test

   # E2E tests
   npm run test:e2e
   ```

3. **Manual Testing:**
   - Step 1: ...
   - Step 2: ...
   - Expected result: ...

### Test Evidence

**Screenshots/Videos:**
(Attach screenshots or videos demonstrating the changes)

**Test Results:**
```
# Paste test output here
✅ All tests passed
Coverage: 85.2%
```

---

## Security Checklist (for Security PRs)

- [ ] Security vulnerability identified and documented
- [ ] Fix implemented and tested
- [ ] Security tests added (≥5 tests per vulnerability)
- [ ] No new security vulnerabilities introduced
- [ ] Penetration testing completed (if applicable)
- [ ] Security team review requested

---

## Performance Impact

- [ ] No performance regression
- [ ] Performance benchmarks run (if applicable)
- [ ] Response time within targets (P95 < 500ms)
- [ ] Database queries optimized (< 100ms)
- [ ] Frontend bundle size acceptable (< 500KB)

**Performance Test Results:**
```
# Paste k6 or Lighthouse results here
```

---

## Deployment Notes

### Database Changes

- [ ] Database migration required
- [ ] Migration script tested
- [ ] Migration is idempotent
- [ ] Rollback script provided

**Migration Files:**
- `V{version}__{description}.sql`

### Environment Variables

- [ ] New environment variables required
- [ ] `.env.example` updated
- [ ] Documentation updated

**New Variables:**
```
NEW_VAR_NAME=default_value
```

### Breaking Changes

- [ ] No breaking changes
- [ ] Breaking changes documented
- [ ] Migration guide provided

**Breaking Changes:**
(List any breaking changes and migration steps)

---

## Merge Criteria

### Must Pass Before Merge

- [ ] All CI/CD checks passing
- [ ] Code review approved (≥1 reviewer)
- [ ] Security review approved (for security PRs)
- [ ] Test coverage ≥80%
- [ ] No HIGH/CRITICAL security vulnerabilities
- [ ] No merge conflicts
- [ ] All conversations resolved

### Dependent PRs

- [ ] All dependent PRs merged (if applicable)

---

## Post-Merge Actions

- [ ] Delete feature branch
- [ ] Update project board
- [ ] Notify stakeholders
- [ ] Monitor production (if deployed)

---

## Additional Context

(Any additional context, screenshots, or information that would help reviewers)

---

## Reviewer Notes

**For Reviewers:**
- Review against `.claude/skills/development-workflow.md` (KiteClass-Specific Checklist)
- Check security implications (especially for Phase 1 PRs)
- Verify test coverage meets requirements
- Run tests locally before approving

**Review Checklist:**
- [ ] Code quality acceptable
- [ ] Tests comprehensive
- [ ] Security considerations addressed
- [ ] Documentation adequate
- [ ] No obvious issues

---

**Co-Authored-By:** Claude Sonnet 4.5 <noreply@anthropic.com>
