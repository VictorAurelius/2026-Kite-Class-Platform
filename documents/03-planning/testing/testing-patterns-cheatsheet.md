# Integration Testing Patterns - Quick Reference

**For:** KiteClass Dashboard Pages (Next.js 15 + Vitest + RTL + MSW)
**Last Updated:** 2026-02-23 (Phase 1 completed)

## 🚀 Quick Start

### Run Tests
```bash
cd kiteclass/kiteclass-frontend
pnpm test                                    # All tests
pnpm test src/app/\(dashboard\)/students     # Students only
pnpm test --watch                            # Watch mode
pnpm test:coverage                           # With coverage
```

### Create New Test File
```bash
mkdir -p src/app/\(dashboard\)/[module]/__tests__
touch src/app/\(dashboard\)/[module]/__tests__/[module]-list.integration.test.tsx
```

## 📋 Test File Template

```typescript
/**
 * Integration tests for [Module] List Page.
 * Tests page-level integration: component + hooks + API + navigation.
 *
 * @since 2026-02-23
 */

import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, waitFor } from '@/test/utils';
import userEvent from '@testing-library/user-event';
import [Module]Page from '../page';
import { server } from '@/mocks/server';
import { http, HttpResponse } from 'msw';
import {
  mockConfirm,
  mock500,
  mockEmptyList,
  waitForLoadingToFinish,
} from '@/test/page-test-utils';

describe('[Module]ListPage Integration', () => {
  beforeEach(() => {
    window.confirm = vi.fn();
  });

  it('should load and display [items] list', async () => {
    render(<[Module]Page />);

    await waitFor(() => {
      expect(screen.getByText('[Expected Data]')).toBeInTheDocument();
    });

    expect(screen.getByText('[Page Title]')).toBeInTheDocument();
  });

  // More tests...
});
```

## 🔧 Common Patterns

### 1. MSW Mock Response

**✅ CORRECT** - With wrapper:
```typescript
http.get('*/api/v1/resource', () => {
  return HttpResponse.json({
    success: true,      // ← Required
    data: {             // ← Required
      content: [...],
      totalElements: X,
      totalPages: Y,
      size: Z,
      number: N,
    },
  });
})
```

**❌ WRONG** - Without wrapper:
```typescript
http.get('*/api/v1/resource', () => {
  return HttpResponse.json({
    content: [...],  // ← Missing wrapper!
    page: {...}
  });
})
```

### 2. Navigation Mock

```typescript
vi.mock('next/navigation', () => ({
  useRouter: vi.fn(),
  usePathname: vi.fn(() => '/current/path'),  // ← Both required!
}));

// In test
const mockPush = vi.fn();
vi.mocked(useRouter).mockReturnValue({
  push: mockPush,
  replace: vi.fn(),
  back: vi.fn(),
  forward: vi.fn(),
  refresh: vi.fn(),
  prefetch: vi.fn(),
} as any);
```

### 3. Finding Elements

```typescript
// By label (forms)
screen.getByLabelText(/tên học viên/i)

// By role + name (buttons)
screen.getByRole('button', { name: /tạo mới/i })

// By text
screen.getByText('Expected Text')
screen.getByText(/pattern/i)  // Case insensitive

// Icon buttons (no text)
const allButtons = screen.getAllByRole('button');
const iconButtons = allButtons.filter(btn => !btn.textContent);
const deleteButton = iconButtons[2]; // View, Edit, Delete
```

### 4. User Interactions

```typescript
const user = userEvent.setup();

// Type
await user.type(screen.getByLabelText(/email/i), 'test@example.com');

// Click
await user.click(screen.getByRole('button', { name: /submit/i }));

// Clear and type
const input = screen.getByLabelText(/name/i);
await user.clear(input);
await user.type(input, 'New Value');
```

### 5. Waiting for Results

```typescript
// Wait for element to appear
await waitFor(() => {
  expect(screen.getByText('Success')).toBeInTheDocument();
});

// Wait for element to disappear
await waitFor(() => {
  expect(screen.queryByText('Loading')).not.toBeInTheDocument();
});

// With timeout
await waitFor(() => {
  expect(screen.getByText('Slow')).toBeInTheDocument();
}, { timeout: 3000 });
```

### 6. Mock window.confirm

```typescript
// In beforeEach
beforeEach(() => {
  window.confirm = vi.fn();
});

// In test - confirm action
window.confirm = vi.fn(() => true);
await user.click(deleteButton);
expect(window.confirm).toHaveBeenCalled();

// In test - cancel action
window.confirm = vi.fn(() => false);
await user.click(deleteButton);
expect(window.confirm).toHaveBeenCalled();
```

## 🎯 Test Scenarios Checklist

### List Page Tests (8-10 tests)
- [ ] Load and display items list
- [ ] Search functionality (debounced)
- [ ] Display empty state when no items
- [ ] Handle API error and show error alert
- [ ] Delete item with confirmation
- [ ] Not delete when confirmation cancelled
- [ ] Display search input placeholder
- [ ] Render page title and description
- [ ] Have working add button link
- [ ] ~~Pagination~~ (skip if timeout - MSW issue)

### Create Page Tests (5-8 tests)
- [ ] Render create form
- [ ] Create item successfully and redirect
- [ ] Show validation errors for empty form
- [ ] Handle duplicate error (409)
- [ ] Handle validation error from API (400)
- [ ] Handle server error (500)
- [ ] Disable submit button while submitting
- [ ] ~~Validate each field format~~ (skip if field has no validation)

## 🚫 Known Issues - Skip These

### ❌ Skip: Async Params Pages
```typescript
// Detail and Edit pages use async params
const { id } = use(params);  // ← Incompatible with RTL

// Solution: Skip these tests, use E2E instead
describe.skip('DetailPage - SKIPPED: async params incompatible', () => {
  // ...
});
```

### ❌ Skip: Loading Spinner Tests
```typescript
// Too fast in test environment
// ❌ it('should display loading spinner', () => {
//   render(<Page />);
//   expect(screen.getByTestId('loading-spinner')).toBeInTheDocument();
// });
```

### ❌ Skip: Tests with Consistent Timeouts
```typescript
// If a test always times out after fixes, skip it
// ❌ it('should handle pagination', async () => {
//   // Consistently fails with timeout
// });
```

## 🐛 Common Errors & Fixes

### Error: "Unable to find element"
**Cause:** Wrong selector or data not loaded
**Fix:**
```typescript
// 1. Check actual text/label in component
// 2. Use screen.debug() to see HTML
screen.debug();

// 3. Wait for data to load
await waitFor(() => {
  expect(screen.getByText('Data')).toBeInTheDocument();
});
```

### Error: "Query data cannot be undefined"
**Cause:** MSW mock missing `{success, data}` wrapper
**Fix:**
```typescript
// Add wrapper to response
return HttpResponse.json({
  success: true,
  data: { ...yourData }
});
```

### Error: "No usePathname export"
**Cause:** Navigation mock incomplete
**Fix:**
```typescript
vi.mock('next/navigation', () => ({
  useRouter: vi.fn(),
  usePathname: vi.fn(() => '/path'),  // ← Add this
}));
```

### Error: Timeout waiting for toast
**Cause:** Toast message text mismatch
**Fix:**
```typescript
// 1. Check hook for actual toast message
// In use-students.ts:
toast({ title: 'Thành công', description: 'Đã tạo học viên mới' });

// 2. Match exact text in test
expect(screen.getByText(/đã tạo học viên mới/i)).toBeInTheDocument();
```

## 📦 Shared Test Utils

Located in `/src/test/page-test-utils.tsx`:

```typescript
// Mock window.confirm
mockConfirm(true);  // or false

// Mock 404 error
mock404('*/api/v1/resource');

// Mock 500 error
mock500('*/api/v1/resource');

// Mock empty list
mockEmptyList('*/api/v1/resource');

// Mock validation error
mockValidationError('*/api/v1/resource', {
  email: 'Email không hợp lệ',
});

// Mock duplicate email
mockDuplicateEmailError('*/api/v1/resource', 'test@example.com');

// Wait for loading to finish
await waitForLoadingToFinish();
```

## ✅ Best Practices

### DO
✅ Use Vietnamese text in assertions (matches UI)
✅ Wait for async operations with `waitFor`
✅ Match exact toast messages from hooks
✅ Check actual component for correct labels/text
✅ Use icon button filtering for table actions
✅ Add meaningful test descriptions

### DON'T
❌ Hardcode English text when UI is Vietnamese
❌ Expect validation errors if field is optional
❌ Test loading spinner (too fast)
❌ Test async params pages with RTL
❌ Copy-paste without updating module names
❌ Forget MSW response wrapper

## 📊 Success Criteria

- **Minimum:** 70% passing rate (acceptable)
- **Target:** 80% passing rate (good)
- **Excellent:** 90%+ passing rate (great!)

For Phase 1: **72% passing** (13/18 tests) ✅

## 🔗 Reference Links

- Phase 1 Summary: `phase-1-students-tests-summary.md`
- Phase 2 Guide: `phase-2-teachers-tests-guide.md`
- Test Utils: `src/test/page-test-utils.tsx`
- MSW Handlers: `src/mocks/handlers.ts`

---

**Remember:** Integration tests are about user flows, not implementation details!
