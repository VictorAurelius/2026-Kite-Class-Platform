---
name: Phase 3 - E2E/CI/CD PR
about: Template cho E2E Testing & CI/CD Setup PRs (Phase 3)
title: '[E2E/CICD] PR 3.X: <Title>'
labels: e2e, cicd, phase-3, critical
assignees: ''
---

## PR Information

**PR Number:** PR 3.X (từ code-review-pr-plan.md)
**Phase:** Phase 3 - E2E & CI/CD
**Priority:** 🔴 P0 - CRITICAL
**Type:**
- [ ] E2E Testing (Playwright)
- [ ] CI/CD Setup (GitHub Actions)
- [ ] Quality Gates

**Estimated Effort:** X days
**Assignee:** @username

---

## E2E Testing Scope (if applicable)

### User Journeys to Test

- [ ] Guest user journey (landing page → contact)
- [ ] Trial signup journey (register → 14-day trial)
- [ ] Trial countdown journey (days remaining → grace → suspend)
- [ ] Payment journey (upgrade → QR → verify → tier upgrade)
- [ ] Feature upgrade journey (locked feature → upgrade → payment)
- [ ] AI branding journey (generate → preview → approve)
- [ ] Multi-tenant isolation (verify data separation)

### Expected E2E Tests

- Total E2E Tests: XX tests
- Browser Coverage: Chromium, Firefox, WebKit
- Mobile Coverage: Yes / No

---

## CI/CD Setup Scope (if applicable)

### Workflows to Create

- [ ] Backend tests workflow (`backend-tests.yml`)
- [ ] Frontend tests workflow (`frontend-tests.yml`)
- [ ] E2E tests workflow (`e2e-tests.yml`)
- [ ] Security scanning workflow (`security-scan.yml`)
- [ ] Coverage reporting workflow (`coverage.yml`)
- [ ] Quality gates workflow (`quality-gates.yml`)

### Quality Gates to Enforce

- [ ] Test coverage ≥80% (fail if below)
- [ ] Security scan (fail on HIGH/CRITICAL)
- [ ] Linting (fail on errors)
- [ ] Build success (fail if build fails)
- [ ] Performance benchmarks (warn on regression)

---

## Implementation Checklist

### E2E Testing Setup (if applicable)

- [ ] Playwright installed and configured
- [ ] `playwright.config.ts` created
- [ ] Test utilities/helpers created
- [ ] Test data seeding strategy implemented
- [ ] Visual regression baseline captured
- [ ] Screenshot comparison configured

### E2E Test Writing

- [ ] Happy path tests written
- [ ] Error flow tests written
- [ ] Edge case tests written
- [ ] Multi-browser tests passing
- [ ] Mobile tests passing (if applicable)
- [ ] Visual regression tests passing

### CI/CD Setup (if applicable)

- [ ] GitHub Actions workflows created
- [ ] Service containers configured (PostgreSQL, Redis)
- [ ] Environment variables configured
- [ ] Secrets configured
- [ ] Caching configured
- [ ] Parallel execution configured

### Quality Gates

- [ ] Coverage gate configured (≥80%)
- [ ] Security gate configured (Snyk, OWASP)
- [ ] Branch protection rules created
- [ ] CODEOWNERS file updated
- [ ] Merge criteria configured

---

## E2E Test Examples

### Guest User Journey

```typescript
// e2e/guest-user-journey.spec.ts
import { test, expect } from '@playwright/test';

test.describe('Guest User Journey', () => {
  test('should display instance branding', async ({ page }) => {
    await page.goto('https://abc-learning.kiteclass.local');

    await expect(page.locator('h1')).toContainText('ABC Learning Center');
    await expect(page.locator('img[alt*="logo"]')).toBeVisible();
    await expect(page.locator('text=contact@abc-learning.com')).toBeVisible();
  });

  test('should NOT show registration form (B2B model)', async ({ page }) => {
    await page.goto('https://xyz-school.kiteclass.local');

    await expect(page.locator('button:has-text("Sign Up")')).not.toBeVisible();
    await expect(page.locator('text=/contact.*office.*to enroll/i')).toBeVisible();
  });
});
```

### Payment Journey

```typescript
// e2e/payment-journey.spec.ts
test('owner upgrades from BASIC to STANDARD via QR payment', async ({ page }) => {
  const helpers = new KiteClassTestHelpers(page);

  await helpers.setupInstance('BASIC');
  await helpers.login('owner@school.com', 'password');

  // Navigate to pricing
  await page.click('text=Settings');
  await page.click('text=Pricing');

  // Click upgrade
  await page.click('button:has-text("Upgrade to Standard")');

  // Wait for QR code
  await helpers.waitForPaymentQR();

  const orderId = await page.locator('text=/ORD-/').textContent();
  expect(orderId).toMatch(/ORD-\d{8}-[A-Z0-9]+/);

  // Simulate payment
  await helpers.simulatePayment(orderId!);

  // Verify success
  await expect(page.locator('text=/payment successful/i')).toBeVisible();
  await expect(page.locator('text=/upgraded to standard/i')).toBeVisible();
});
```

---

## CI/CD Workflow Examples

### Backend Tests Workflow

```yaml
# .github/workflows/backend-tests.yml
name: Backend Tests

on:
  pull_request:
  push:
    branches: [main, develop]

jobs:
  test:
    runs-on: ubuntu-latest
    services:
      postgres:
        image: postgres:16
        env:
          POSTGRES_DB: kiteclass_test
          POSTGRES_USER: test
          POSTGRES_PASSWORD: test
        options: --health-cmd pg_isready

    steps:
      - uses: actions/checkout@v4

      - name: Set up JDK 21
        uses: actions/setup-java@v4
        with:
          java-version: '21'
          distribution: 'temurin'
          cache: 'maven'

      - name: Run tests
        run: ./mvnw test

      - name: Check coverage
        run: ./mvnw jacoco:check
```

### Quality Gates Workflow

```yaml
# .github/workflows/quality-gates.yml
name: Quality Gates

on: [pull_request]

jobs:
  quality-check:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4

      - name: Check coverage
        run: |
          coverage=$(cat coverage-report.json | jq '.total.lines.pct')
          if (( $(echo "$coverage < 80" | bc -l) )); then
            echo "❌ Coverage below 80%: $coverage%"
            exit 1
          fi
```

---

## Testing Strategy Reference

### E2E Testing
- **Standards:** `.claude/skills/e2e-testing-standards.md`
- **Patterns:** Playwright best practices
- **Coverage:** Critical user journeys (≥50 tests)

### CI/CD
- **Standards:** `.claude/skills/ci-cd-quality-enforcement.md`
- **Workflows:** GitHub Actions
- **Gates:** Coverage (≥80%), Security (no HIGH/CRITICAL)

---

## Test Execution

### Run E2E Tests Locally

```bash
# Install Playwright
npm run playwright install --with-deps

# Run all E2E tests
npm run test:e2e

# Run specific test file
npm run test:e2e -- guest-user-journey.spec.ts

# Run with UI mode
npm run test:e2e -- --ui

# Debug mode
npm run test:e2e -- --debug
```

### Expected Results

```
✅ Running 50 tests using 3 workers
✅ 50 passed (5.2m)

Browsers: Chromium, Firefox, WebKit
Tests: ✅ All passing
Screenshots: ✅ No visual regressions
```

---

## CI/CD Verification

### Check Workflows

```bash
# Validate workflow syntax
act -l

# Run workflow locally (using act)
act pull_request

# Check workflow status
gh run list --workflow=backend-tests.yml
```

### Expected CI/CD Results

```
✅ Backend Tests: Passing
✅ Frontend Tests: Passing
✅ E2E Tests: Passing
✅ Security Scan: No vulnerabilities
✅ Coverage: 85.2% (≥80%)
✅ Quality Gates: All passing
```

---

## Review Criteria

### E2E Tests (if applicable)

- [ ] All critical user journeys covered
- [ ] Tests are stable (no flakiness)
- [ ] Tests use page objects/helpers
- [ ] Tests have descriptive names
- [ ] Visual regression tests included
- [ ] Multi-browser tests passing

### CI/CD (if applicable)

- [ ] All workflows tested locally
- [ ] Workflows run successfully in GitHub
- [ ] Quality gates enforce requirements
- [ ] Branch protection configured
- [ ] Notifications configured (Slack, email)
- [ ] Caching optimizes build time

---

## Merge Criteria

### E2E PRs
- [ ] All E2E tests passing (100%)
- [ ] Visual regression baseline approved
- [ ] Tests run in CI successfully
- [ ] Code review approved

### CI/CD PRs
- [ ] All workflows operational
- [ ] Quality gates enforcing
- [ ] Test execution successful
- [ ] Documentation updated

---

## Post-Merge Actions

- [ ] Monitor CI/CD pipeline
- [ ] Update E2E test dashboard
- [ ] Notify team of new workflows
- [ ] Document any known issues

---

**Reference Documents:**
- `.claude/skills/e2e-testing-standards.md`
- `.claude/skills/ci-cd-quality-enforcement.md`
- `documents/plans/code-review-pr-plan.md` (Phase 3)
