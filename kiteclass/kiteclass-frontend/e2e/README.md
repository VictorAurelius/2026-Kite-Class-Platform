# E2E Tests

End-to-end tests using Playwright.

## Running Tests

```bash
# Run all E2E tests
pnpm test:e2e

# Run specific test file
pnpm test:e2e -- e2e/theme.spec.ts

# Run in headed mode (see browser)
pnpm test:e2e -- --headed
```

## Test Files

- `auth.spec.ts` - Authentication flows
- `theme.spec.ts` - Theme system (PR-THEME-1) - 17 tests
- `billing.spec.ts` - Billing and payments
- `branding.spec.ts` - AI branding
- `classes.spec.ts` - Class management
- `attendance-enhancements.spec.ts` - Attendance

## Theme Tests

See [theme.spec.ts](./theme.spec.ts) for comprehensive theme system tests.

**Test Coverage:**
- Default theme loading
- Theme persistence (localStorage)
- postMessage live updates
- Invalid data handling
- Tailwind utilities integration
- Visual consistency

**Total:** 17 test cases covering all theme system functionality.
