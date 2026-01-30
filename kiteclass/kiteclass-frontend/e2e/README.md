# E2E Tests

End-to-end tests using Playwright.

## Structure

```
e2e/
├── auth.spec.ts          # Authentication flows
├── students.spec.ts      # Student management
├── classes.spec.ts       # Class management
├── attendance.spec.ts    # Attendance marking
└── billing.spec.ts       # Billing operations
```

## Running E2E Tests

```bash
# Run all E2E tests
pnpm test:e2e

# Run in headed mode (see browser)
pnpm exec playwright test --headed

# Run specific test file
pnpm exec playwright test e2e/auth.spec.ts

# Debug mode
pnpm exec playwright test --debug

# UI mode
pnpm exec playwright test --ui
```

## Writing E2E Tests

Example:

```typescript
import { test, expect } from '@playwright/test';

test.describe('Authentication', () => {
  test('should login successfully', async ({ page }) => {
    await page.goto('/login');
    await page.fill('input[name="email"]', 'owner@kiteclass.local');
    await page.fill('input[name="password"]', 'Admin@123');
    await page.click('button[type="submit"]');

    await expect(page).toHaveURL('/dashboard');
  });
});
```

## Tips

- Use `test.beforeEach()` for common setup (login, navigation)
- Use `page.locator()` instead of `page.$()` for better auto-waiting
- Use `await expect(page).toHaveURL()` to verify navigation
- Use `await expect(element).toBeVisible()` to verify UI state
