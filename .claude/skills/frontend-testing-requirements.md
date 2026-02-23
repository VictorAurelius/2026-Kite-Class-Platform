# Skill: Frontend Testing Requirements

**Version:** 1.0
**Date:** 2026-02-22
**Purpose:** Mandatory testing standards for KiteClass Frontend

---

## 📋 Overview

**CRITICAL:** Every frontend PR MUST include comprehensive tests before merge.

## ✅ Testing Requirements

### Coverage Thresholds (NON-NEGOTIABLE)

```typescript
// vitest.config.ts
coverage: {
  thresholds: {
    lines: 80,       // ≥80% line coverage
    functions: 80,   // ≥80% function coverage
    branches: 75,    // ≥75% branch coverage
    statements: 80,  // ≥80% statement coverage
  }
}
```

### Test Types Required

#### 1. Unit Tests
**What:** Individual functions, utilities, helpers
**Tool:** Vitest
**Location:** `src/**/__tests__/*.test.ts`

```typescript
// Example: src/lib/__tests__/utils.test.ts
describe('formatDate', () => {
  it('should format ISO date to Vietnamese locale', () => {
    expect(formatDate('2024-01-15')).toBe('15/01/2024');
  });
});
```

#### 2. Component Tests
**What:** UI components in isolation
**Tool:** Vitest + React Testing Library
**Location:** `src/components/**/__tests__/*.test.tsx`

**Must Test:**
- ✅ Rendering with different props
- ✅ User interactions (click, type, submit)
- ✅ Conditional rendering
- ✅ Loading and error states
- ✅ Accessibility (ARIA labels, keyboard nav)

```typescript
// Example: DataTable.test.tsx
describe('DataTable', () => {
  it('should render table with data', () => {
    render(<DataTable columns={cols} data={data} />);
    expect(screen.getByText('John Doe')).toBeInTheDocument();
  });

  it('should handle pagination', async () => {
    const user = userEvent.setup();
    render(<DataTable columns={cols} data={data} pageCount={5} />);

    await user.click(screen.getByRole('button', { name: /next/i }));
    expect(onPaginationChange).toHaveBeenCalled();
  });
});
```

#### 3. Integration Tests
**What:** Components + API + State Management
**Tool:** Vitest + RTL + MSW
**Location:** `src/hooks/__tests__/*.test.tsx`, `src/components/**/__tests__/*.test.tsx`

**Must Test:**
- ✅ API calls with mocked responses (MSW)
- ✅ React Query cache updates
- ✅ Form submissions
- ✅ Error handling from API
- ✅ Loading states during async operations

```typescript
// Example: use-students.test.tsx
describe('useStudents', () => {
  it('should fetch students list successfully', async () => {
    const { result } = renderHook(() => useStudents(), { wrapper });

    await waitFor(() => {
      expect(result.current.isSuccess).toBe(true);
    });

    expect(result.current.data?.content).toHaveLength(2);
  });

  it('should handle API errors', async () => {
    server.use(
      http.get('/api/v1/students', () => HttpResponse.error())
    );

    const { result } = renderHook(() => useStudents(), { wrapper });

    await waitFor(() => {
      expect(result.current.isError).toBe(true);
    });
  });
});
```

#### 4. Form Tests
**What:** Form validation, submission, error handling
**Must Test:**
- ✅ Required field validation
- ✅ Email/phone format validation
- ✅ Custom validation rules (Zod schemas)
- ✅ Submission with valid data
- ✅ API error display
- ✅ Disabled state during submission

```typescript
// Example: StudentForm.test.tsx
describe('StudentForm', () => {
  it('should validate required fields', async () => {
    const user = userEvent.setup();
    render(<StudentForm onSubmit={vi.fn()} />);

    await user.click(screen.getByRole('button', { name: /submit/i }));

    expect(screen.getByText(/name.*required/i)).toBeInTheDocument();
  });

  it('should submit valid data', async () => {
    const onSubmit = vi.fn();
    const user = userEvent.setup();
    render(<StudentForm onSubmit={onSubmit} />);

    await user.type(screen.getByLabelText(/name/i), 'John Doe');
    await user.type(screen.getByLabelText(/email/i), 'john@example.com');
    await user.click(screen.getByRole('button', { name: /submit/i }));

    await waitFor(() => {
      expect(onSubmit).toHaveBeenCalledWith(
        expect.objectContaining({
          name: 'John Doe',
          email: 'john@example.com',
        })
      );
    });
  });
});
```

---

## 🛠️ Test Infrastructure

### Required Setup

#### 1. MSW (Mock Service Worker)
**Purpose:** Mock API responses in tests

```typescript
// src/mocks/handlers.ts
export const handlers = [
  http.get('/api/v1/students', () => {
    return HttpResponse.json({
      success: true,
      data: { content: [...], totalElements: 10 }
    });
  }),

  http.post('/api/v1/students', async ({ request }) => {
    const body = await request.json();
    return HttpResponse.json({
      success: true,
      data: { id: 1, ...body }
    });
  }),
];
```

```typescript
// src/mocks/server.ts
import { setupServer } from 'msw/node';
import { handlers } from './handlers';

export const server = setupServer(...handlers);
```

#### 2. Test Setup
```typescript
// src/test/setup.ts
import { beforeAll, afterEach, afterAll } from 'vitest';
import { server } from '../mocks/server';

beforeAll(() => server.listen({ onUnhandledRequest: 'error' }));
afterEach(() => server.resetHandlers());
afterAll(() => server.close());
```

#### 3. Test Utilities
```typescript
// src/test/utils.tsx
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';

export function AllTheProviders({ children }: { children: React.ReactNode }) {
  const testQueryClient = new QueryClient({
    defaultOptions: {
      queries: { retry: false },
      mutations: { retry: false },
    },
  });

  return (
    <QueryClientProvider client={testQueryClient}>
      {children}
    </QueryClientProvider>
  );
}

export function renderWithProviders(ui: ReactElement) {
  return render(ui, { wrapper: AllTheProviders });
}
```

---

## 📝 Testing Checklist (Pre-Commit)

Before committing frontend code:

### Component Tests
- [ ] All new components have tests
- [ ] Happy path tested (normal usage)
- [ ] Edge cases tested (empty data, errors)
- [ ] User interactions tested (click, type, submit)
- [ ] Loading states tested
- [ ] Error states tested
- [ ] Accessibility tested (ARIA, keyboard nav)

### Form Tests
- [ ] Required field validation
- [ ] Format validation (email, phone, etc.)
- [ ] Custom validation rules (Zod)
- [ ] Successful submission
- [ ] API error handling
- [ ] Disabled state during submission

### Integration Tests
- [ ] API calls mocked with MSW
- [ ] Success responses tested
- [ ] Error responses tested
- [ ] React Query cache invalidation tested
- [ ] Toast notifications tested

### Coverage
- [ ] Run `pnpm test:coverage`
- [ ] Lines ≥80%
- [ ] Functions ≥80%
- [ ] Branches ≥75%
- [ ] Statements ≥80%

### Quality
- [ ] All tests pass: `pnpm test`
- [ ] No console errors in tests
- [ ] No skipped tests (`.skip`) without justification
- [ ] No focused tests (`.only`)

---

## 🚫 Common Mistakes to Avoid

### ❌ DON'T

```typescript
// ❌ Testing implementation details
expect(component.state.count).toBe(1);

// ❌ Using getByTestId everywhere
screen.getByTestId('student-name');

// ❌ Not testing user interactions
// Just rendering without simulating clicks/types

// ❌ Skipping error states
// Only testing happy path

// ❌ Not cleaning up after tests
// Leaving timers, listeners, or global state
```

### ✅ DO

```typescript
// ✅ Test user-visible behavior
expect(screen.getByText('John Doe')).toBeInTheDocument();

// ✅ Use semantic queries
screen.getByRole('button', { name: /submit/i });
screen.getByLabelText(/email/i);

// ✅ Test user interactions
const user = userEvent.setup();
await user.click(button);
await user.type(input, 'text');

// ✅ Test all states
// happy path + loading + error + empty

// ✅ Clean up properly
afterEach(() => {
  cleanup();
  server.resetHandlers();
});
```

---

## 🔍 Example: Complete Component Test Suite

```typescript
// StudentForm.test.tsx - COMPLETE EXAMPLE

import { describe, it, expect, vi } from 'vitest';
import { render, screen, waitFor } from '@/test/utils';
import userEvent from '@testing-library/user-event';
import { StudentForm } from '../student-form';
import { server } from '@/mocks/server';
import { http, HttpResponse } from 'msw';

describe('StudentForm', () => {
  // 1. Rendering
  it('should render all fields', () => {
    render(<StudentForm onSubmit={vi.fn()} />);

    expect(screen.getByLabelText(/name/i)).toBeInTheDocument();
    expect(screen.getByLabelText(/email/i)).toBeInTheDocument();
    expect(screen.getByLabelText(/phone/i)).toBeInTheDocument();
  });

  // 2. Validation
  it('should validate required fields', async () => {
    const user = userEvent.setup();
    render(<StudentForm onSubmit={vi.fn()} />);

    await user.click(screen.getByRole('button', { name: /submit/i }));

    expect(screen.getByText(/name.*required/i)).toBeInTheDocument();
  });

  it('should validate email format', async () => {
    const user = userEvent.setup();
    render(<StudentForm onSubmit={vi.fn()} />);

    await user.type(screen.getByLabelText(/email/i), 'invalid');
    await user.click(screen.getByRole('button', { name: /submit/i }));

    expect(screen.getByText(/invalid email/i)).toBeInTheDocument();
  });

  // 3. Submission
  it('should submit valid data', async () => {
    const onSubmit = vi.fn();
    const user = userEvent.setup();
    render(<StudentForm onSubmit={onSubmit} />);

    await user.type(screen.getByLabelText(/name/i), 'John');
    await user.type(screen.getByLabelText(/email/i), 'john@example.com');
    await user.click(screen.getByRole('button', { name: /submit/i }));

    await waitFor(() => {
      expect(onSubmit).toHaveBeenCalled();
    });
  });

  // 4. Loading state
  it('should disable submit during submission', () => {
    render(<StudentForm onSubmit={vi.fn()} isSubmitting={true} />);

    expect(screen.getByRole('button', { name: /submitting/i })).toBeDisabled();
  });

  // 5. Edit mode
  it('should pre-fill data in edit mode', () => {
    const initialData = { name: 'John', email: 'john@example.com' };
    render(<StudentForm onSubmit={vi.fn()} initialData={initialData} />);

    expect(screen.getByDisplayValue('John')).toBeInTheDocument();
  });

  // 6. Error handling
  it('should display API errors', async () => {
    server.use(
      http.post('/api/v1/students', () => {
        return HttpResponse.json(
          { message: 'Email already exists' },
          { status: 400 }
        );
      })
    );

    const user = userEvent.setup();
    render(<StudentForm onSubmit={vi.fn()} />);

    // Fill and submit form...

    await waitFor(() => {
      expect(screen.getByText(/email already exists/i)).toBeInTheDocument();
    });
  });
});
```

---

## 🎯 Summary

**Every PR MUST:**
1. ✅ Include tests for ALL new components/hooks
2. ✅ Achieve ≥80% coverage (lines, functions, statements)
3. ✅ Test all user interactions
4. ✅ Test all states (loading, error, empty, success)
5. ✅ Use MSW for API mocking
6. ✅ Pass `pnpm test` with 0 failures

**Before merge:**
- Run `pnpm test`
- Run `pnpm test:coverage`
- Verify coverage ≥80%
- All tests passing
- No console errors

---

**Version:** 1.0
**Last Updated:** 2026-02-22
**Applies to:** All Frontend PRs (3.1+)
