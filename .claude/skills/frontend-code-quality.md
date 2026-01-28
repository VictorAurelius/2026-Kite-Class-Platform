# Skill: Frontend Code Quality & Testing

Complete guide for TypeScript/React code quality, testing patterns, and best practices.

**Version:** 1.0
**Date:** 2026-01-28
**Applies to:** Next.js, React, TypeScript projects

## Mô tả

Quy chuẩn code quality cho Frontend:
- TypeScript strict mode & type safety
- React best practices & hooks rules
- Testing requirements (Unit, Integration, E2E)
- Code organization & naming conventions
- Performance optimization
- Accessibility (a11y)
- Security best practices

## Trigger phrases

- "frontend quality"
- "react testing"
- "typescript types"
- "code review frontend"
- "frontend best practices"

---

# PART 1: TypeScript Code Quality

## Strict Mode Configuration

### tsconfig.json

```json
{
  "compilerOptions": {
    "strict": true,
    "noImplicitAny": true,
    "strictNullChecks": true,
    "strictFunctionTypes": true,
    "strictBindCallApply": true,
    "strictPropertyInitialization": true,
    "noImplicitThis": true,
    "alwaysStrict": true,
    "noUnusedLocals": true,
    "noUnusedParameters": true,
    "noImplicitReturns": true,
    "noFallthroughCasesInSwitch": true,
    "noUncheckedIndexedAccess": true
  }
}
```

## Type Safety Rules

### ❌ BAD: Using `any`

```typescript
// NEVER use `any` type
function processData(data: any) {  // ❌ BAD
  return data.value;
}

const response: any = await fetch('/api');  // ❌ BAD
```

### ✅ GOOD: Proper Types

```typescript
// Define proper types
interface ApiResponse<T> {
  data: T;
  message: string;
  success: boolean;
}

function processData<T>(data: ApiResponse<T>): T {  // ✅ GOOD
  return data.data;
}

const response: ApiResponse<User> = await api.get('/users/me');  // ✅ GOOD
```

### Type Definitions Location

```typescript
// src/types/student.ts
export interface Student {
  id: number;
  name: string;
  email: string;
  phone: string;
  dateOfBirth: Date | null;
  status: StudentStatus;
}

export enum StudentStatus {
  ACTIVE = 'ACTIVE',
  INACTIVE = 'INACTIVE',
  GRADUATED = 'GRADUATED',
}

// src/types/api.ts
export interface ApiResponse<T> {
  data: T;
  message: string;
  success: boolean;
}

export interface PageResponse<T> {
  data: T[];
  total: number;
  page: number;
  pageSize: number;
  totalPages: number;
}

export interface ErrorResponse {
  code: string;
  message: string;
  path: string;
  timestamp: string;
  fieldErrors?: Record<string, string[]>;
}
```

### Utility Types

```typescript
// Use built-in utility types
type PartialStudent = Partial<Student>;  // All fields optional
type RequiredStudent = Required<Student>;  // All fields required
type StudentKeys = keyof Student;  // Union of key names
type StudentPick = Pick<Student, 'id' | 'name'>;  // Subset of properties
type StudentOmit = Omit<Student, 'dateOfBirth'>;  // Exclude properties
```

## Naming Conventions

### Files

```
// Components (PascalCase)
StudentForm.tsx
DataTable.tsx
PageHeader.tsx

// Hooks (camelCase with 'use' prefix)
use-students.ts
use-auth.ts
use-debounce.ts

// Utils (camelCase)
format-date.ts
validate-email.ts
api-client.ts

// Types (camelCase)
student.ts
api.ts
```

### Variables & Functions

```typescript
// ✅ GOOD: Descriptive names
const isLoading = true;
const hasPermission = checkPermission();
const studentCount = students.length;

function fetchStudents() { }
function handleSubmit() { }
function validateEmail(email: string) { }

// ❌ BAD: Unclear names
const flag = true;
const data = getData();
const n = students.length;

function doSomething() { }
function process() { }
```

### Constants

```typescript
// src/lib/constants.ts
export const API_BASE_URL = process.env.NEXT_PUBLIC_API_URL!;
export const DEFAULT_PAGE_SIZE = 10;
export const MAX_FILE_SIZE = 5 * 1024 * 1024; // 5MB
export const PHONE_REGEX = /^0\d{9}$/;
```

---

# PART 2: React Best Practices

## Component Structure

### Functional Components

```typescript
// ✅ GOOD: Clear component structure
import { useState, useEffect } from 'react';
import { Button } from '@/components/ui/button';

interface StudentFormProps {
  student?: Student;
  onSubmit: (data: StudentFormData) => Promise<void>;
  onCancel: () => void;
}

export function StudentForm({ student, onSubmit, onCancel }: StudentFormProps) {
  // 1. Hooks
  const [isSubmitting, setIsSubmitting] = useState(false);
  const form = useForm<StudentFormData>();

  // 2. Effects
  useEffect(() => {
    if (student) {
      form.reset(student);
    }
  }, [student]);

  // 3. Event handlers
  const handleSubmit = async (data: StudentFormData) => {
    setIsSubmitting(true);
    try {
      await onSubmit(data);
    } finally {
      setIsSubmitting(false);
    }
  };

  // 4. Render
  return (
    <form onSubmit={form.handleSubmit(handleSubmit)}>
      {/* Form content */}
    </form>
  );
}
```

## Hooks Rules

### ✅ DO

```typescript
// Always call hooks at top level
function MyComponent() {
  const [count, setCount] = useState(0);  // ✅
  const { data } = useQuery(...);  // ✅

  return <div>{count}</div>;
}

// Custom hooks start with 'use'
function useLocalStorage(key: string) {  // ✅
  const [value, setValue] = useState(() => {
    return localStorage.getItem(key);
  });
  return [value, setValue];
}
```

### ❌ DON'T

```typescript
// Never call hooks conditionally
function MyComponent({ condition }) {
  if (condition) {
    const [count, setCount] = useState(0);  // ❌ BAD
  }
  return <div />;
}

// Never call hooks in loops
function MyComponent({ items }) {
  items.forEach(item => {
    const [state, setState] = useState();  // ❌ BAD
  });
  return <div />;
}
```

## Props & State Management

### Props Interface

```typescript
// ✅ GOOD: Explicit props interface
interface ButtonProps {
  children: React.ReactNode;
  variant?: 'primary' | 'secondary' | 'outline';
  size?: 'sm' | 'md' | 'lg';
  disabled?: boolean;
  onClick?: () => void;
}

export function Button({
  children,
  variant = 'primary',
  size = 'md',
  disabled = false,
  onClick,
}: ButtonProps) {
  // ...
}
```

### State Management

```typescript
// ✅ GOOD: Proper state management
// Server state: Use React Query
const { data: students, isLoading } = useQuery({
  queryKey: ['students'],
  queryFn: () => api.get<Student[]>('/students'),
});

// Client state: Use Zustand
interface AuthStore {
  user: User | null;
  token: string | null;
  login: (credentials: LoginCredentials) => Promise<void>;
  logout: () => void;
}

export const useAuthStore = create<AuthStore>((set) => ({
  user: null,
  token: null,
  login: async (credentials) => {
    const { user, token } = await api.post('/auth/login', credentials);
    set({ user, token });
  },
  logout: () => set({ user: null, token: null }),
}));
```

---

# PART 3: Testing Requirements

## Test Coverage Requirements

| Layer | Minimum Coverage |
|-------|-----------------|
| React Hooks | 80% |
| React Components | 70% |
| Utility Functions | 90% |
| API Client | 80% |

## Unit Testing (Vitest + React Testing Library)

### Setup

```typescript
// vitest.config.ts
import { defineConfig } from 'vitest/config';
import react from '@vitejs/plugin-react';
import path from 'path';

export default defineConfig({
  plugins: [react()],
  test: {
    environment: 'jsdom',
    setupFiles: ['./src/test/setup.ts'],
    coverage: {
      provider: 'v8',
      reporter: ['text', 'json', 'html'],
      include: ['src/**/*.{ts,tsx}'],
      exclude: [
        'src/**/*.test.{ts,tsx}',
        'src/**/*.spec.{ts,tsx}',
        'src/types/**',
        'src/test/**',
      ],
    },
  },
  resolve: {
    alias: {
      '@': path.resolve(__dirname, './src'),
    },
  },
});
```

### Component Testing

```typescript
// src/__tests__/components/shared/page-header.test.tsx
import { describe, it, expect } from 'vitest';
import { render, screen } from '@testing-library/react';
import { PageHeader } from '@/components/shared/page-header';

describe('PageHeader', () => {
  it('renders title correctly', () => {
    render(<PageHeader title="Test Title" />);
    expect(screen.getByText('Test Title')).toBeInTheDocument();
  });

  it('renders description when provided', () => {
    render(
      <PageHeader title="Test" description="Test description" />
    );
    expect(screen.getByText('Test description')).toBeInTheDocument();
  });

  it('renders actions when provided', () => {
    render(
      <PageHeader
        title="Test"
        actions={<button>Action</button>}
      />
    );
    expect(screen.getByRole('button', { name: 'Action' })).toBeInTheDocument();
  });

  it('does not render description when not provided', () => {
    render(<PageHeader title="Test" />);
    expect(screen.queryByText(/description/i)).not.toBeInTheDocument();
  });
});
```

### Hook Testing

```typescript
// src/__tests__/hooks/use-students.test.ts
import { describe, it, expect, beforeEach, vi } from 'vitest';
import { renderHook, waitFor } from '@testing-library/react';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { useStudents } from '@/hooks/use-students';
import * as api from '@/lib/api/client';

// Mock API
vi.mock('@/lib/api/client');

const createWrapper = () => {
  const queryClient = new QueryClient({
    defaultOptions: {
      queries: { retry: false },
    },
  });
  return ({ children }: { children: React.ReactNode }) => (
    <QueryClientProvider client={queryClient}>
      {children}
    </QueryClientProvider>
  );
};

describe('useStudents', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  it('fetches students successfully', async () => {
    const mockStudents = [
      { id: 1, name: 'John Doe', email: 'john@example.com' },
    ];
    vi.mocked(api.get).mockResolvedValue({
      data: mockStudents,
      total: 1,
    });

    const { result } = renderHook(() => useStudents(), {
      wrapper: createWrapper(),
    });

    await waitFor(() => expect(result.current.isSuccess).toBe(true));
    expect(result.current.data?.data).toEqual(mockStudents);
  });

  it('handles error when fetch fails', async () => {
    vi.mocked(api.get).mockRejectedValue(new Error('API Error'));

    const { result } = renderHook(() => useStudents(), {
      wrapper: createWrapper(),
    });

    await waitFor(() => expect(result.current.isError).toBe(true));
    expect(result.current.error).toBeDefined();
  });
});
```

### Form Testing

```typescript
// src/__tests__/components/forms/student-form.test.tsx
import { describe, it, expect, vi } from 'vitest';
import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { StudentForm } from '@/components/forms/student-form';

describe('StudentForm', () => {
  const mockOnSubmit = vi.fn();
  const mockOnCancel = vi.fn();

  it('validates required fields', async () => {
    const user = userEvent.setup();
    render(<StudentForm onSubmit={mockOnSubmit} onCancel={mockOnCancel} />);

    const submitButton = screen.getByRole('button', { name: /lưu/i });
    await user.click(submitButton);

    await waitFor(() => {
      expect(screen.getByText(/tên là bắt buộc/i)).toBeInTheDocument();
      expect(screen.getByText(/email là bắt buộc/i)).toBeInTheDocument();
    });

    expect(mockOnSubmit).not.toHaveBeenCalled();
  });

  it('validates email format', async () => {
    const user = userEvent.setup();
    render(<StudentForm onSubmit={mockOnSubmit} onCancel={mockOnCancel} />);

    const emailInput = screen.getByLabelText(/email/i);
    await user.type(emailInput, 'invalid-email');
    await user.tab(); // Trigger blur

    await waitFor(() => {
      expect(screen.getByText(/email không hợp lệ/i)).toBeInTheDocument();
    });
  });

  it('submits form with valid data', async () => {
    const user = userEvent.setup();
    mockOnSubmit.mockResolvedValue(undefined);

    render(<StudentForm onSubmit={mockOnSubmit} onCancel={mockOnCancel} />);

    await user.type(screen.getByLabelText(/tên/i), 'John Doe');
    await user.type(screen.getByLabelText(/email/i), 'john@example.com');
    await user.type(screen.getByLabelText(/số điện thoại/i), '0912345678');

    await user.click(screen.getByRole('button', { name: /lưu/i }));

    await waitFor(() => {
      expect(mockOnSubmit).toHaveBeenCalledWith({
        name: 'John Doe',
        email: 'john@example.com',
        phone: '0912345678',
      });
    });
  });
});
```

## Mock Service Worker (MSW) for API Mocking

```typescript
// src/test/mocks/handlers.ts
import { http, HttpResponse } from 'msw';

export const handlers = [
  http.get('/api/v1/students', () => {
    return HttpResponse.json({
      data: [
        { id: 1, name: 'John Doe', email: 'john@example.com' },
        { id: 2, name: 'Jane Smith', email: 'jane@example.com' },
      ],
      total: 2,
      page: 1,
      pageSize: 10,
      totalPages: 1,
    });
  }),

  http.post('/api/v1/students', async ({ request }) => {
    const body = await request.json();
    return HttpResponse.json({
      data: { id: 3, ...body },
      message: 'Tạo học viên thành công',
      success: true,
    });
  }),

  http.get('/api/v1/students/:id', ({ params }) => {
    const { id } = params;
    return HttpResponse.json({
      data: { id, name: 'John Doe', email: 'john@example.com' },
      success: true,
    });
  }),
];

// src/test/mocks/server.ts
import { setupServer } from 'msw/node';
import { handlers } from './handlers';

export const server = setupServer(...handlers);

// src/test/setup.ts
import { beforeAll, afterEach, afterAll } from 'vitest';
import { server } from './mocks/server';
import '@testing-library/jest-dom';

beforeAll(() => server.listen());
afterEach(() => server.resetHandlers());
afterAll(() => server.close());
```

## E2E Testing (Playwright)

```typescript
// e2e/auth.spec.ts
import { test, expect } from '@playwright/test';

test.describe('Authentication', () => {
  test('should login successfully with valid credentials', async ({ page }) => {
    await page.goto('/login');

    await page.fill('input[name="email"]', 'owner@kiteclass.local');
    await page.fill('input[name="password"]', 'Admin@123');
    await page.click('button[type="submit"]');

    await expect(page).toHaveURL('/dashboard');
    await expect(page.getByText('Dashboard')).toBeVisible();
  });

  test('should show error with invalid credentials', async ({ page }) => {
    await page.goto('/login');

    await page.fill('input[name="email"]', 'wrong@example.com');
    await page.fill('input[name="password"]', 'wrongpassword');
    await page.click('button[type="submit"]');

    await expect(page.getByText(/email hoặc mật khẩu không đúng/i)).toBeVisible();
    await expect(page).toHaveURL('/login');
  });
});

// e2e/students.spec.ts
test.describe('Student Management', () => {
  test.beforeEach(async ({ page }) => {
    // Login first
    await page.goto('/login');
    await page.fill('input[name="email"]', 'owner@kiteclass.local');
    await page.fill('input[name="password"]', 'Admin@123');
    await page.click('button[type="submit"]');
    await expect(page).toHaveURL('/dashboard');
  });

  test('should create new student', async ({ page }) => {
    await page.goto('/students');
    await page.click('button:has-text("Thêm học viên")');

    await page.fill('input[name="name"]', 'Test Student');
    await page.fill('input[name="email"]', 'test@example.com');
    await page.fill('input[name="phone"]', '0912345678');

    await page.click('button[type="submit"]');

    await expect(page.getByText(/tạo học viên thành công/i)).toBeVisible();
    await expect(page.getByText('Test Student')).toBeVisible();
  });
});
```

---

# PART 4: ESLint & Prettier

## ESLint Configuration

```json
// .eslintrc.json
{
  "extends": [
    "next/core-web-vitals",
    "next/typescript",
    "plugin:@typescript-eslint/recommended",
    "plugin:react-hooks/recommended"
  ],
  "rules": {
    "@typescript-eslint/no-explicit-any": "error",
    "@typescript-eslint/no-unused-vars": ["error", {
      "argsIgnorePattern": "^_"
    }],
    "react-hooks/rules-of-hooks": "error",
    "react-hooks/exhaustive-deps": "warn",
    "no-console": ["warn", {
      "allow": ["warn", "error"]
    }]
  }
}
```

## Prettier Configuration

```json
// .prettierrc
{
  "semi": true,
  "trailingComma": "es5",
  "singleQuote": true,
  "printWidth": 100,
  "tabWidth": 2,
  "useTabs": false,
  "plugins": ["prettier-plugin-tailwindcss"]
}
```

---

# PART 5: Performance Optimization

## React.memo

```typescript
// ✅ GOOD: Memoize expensive components
export const StudentCard = React.memo(({ student }: { student: Student }) => {
  return (
    <Card>
      <CardContent>
        <h3>{student.name}</h3>
        <p>{student.email}</p>
      </CardContent>
    </Card>
  );
});

StudentCard.displayName = 'StudentCard';
```

## useMemo & useCallback

```typescript
// ✅ GOOD: Memoize expensive calculations
function StudentList({ students }: { students: Student[] }) {
  const activeStudents = useMemo(() => {
    return students.filter(s => s.status === 'ACTIVE');
  }, [students]);

  const handleSelect = useCallback((id: number) => {
    console.log('Selected:', id);
  }, []);

  return (
    <div>
      {activeStudents.map(student => (
        <StudentCard
          key={student.id}
          student={student}
          onSelect={handleSelect}
        />
      ))}
    </div>
  );
}
```

## Dynamic Imports

```typescript
// ✅ GOOD: Lazy load components
import dynamic from 'next/dynamic';

const HeavyComponent = dynamic(() => import('@/components/heavy-component'), {
  loading: () => <LoadingSpinner />,
  ssr: false, // Disable SSR if needed
});
```

---

# PART 6: Accessibility (a11y)

## Semantic HTML

```typescript
// ✅ GOOD: Use semantic elements
<main>
  <section aria-labelledby="students-heading">
    <h2 id="students-heading">Danh sách học viên</h2>
    <table>
      <caption className="sr-only">Danh sách học viên</caption>
      <thead>
        <tr>
          <th scope="col">Tên</th>
          <th scope="col">Email</th>
        </tr>
      </thead>
    </table>
  </section>
</main>

// ❌ BAD: div soup
<div>
  <div>Danh sách học viên</div>
  <div>
    <div>Tên</div>
    <div>Email</div>
  </div>
</div>
```

## ARIA Attributes

```typescript
// ✅ GOOD: Proper ARIA labels
<button
  aria-label="Xóa học viên John Doe"
  aria-describedby="delete-description"
>
  <TrashIcon />
</button>
<span id="delete-description" className="sr-only">
  Hành động này không thể hoàn tác
</span>

// Form labels
<label htmlFor="email">Email</label>
<input
  id="email"
  type="email"
  aria-required="true"
  aria-invalid={!!errors.email}
  aria-describedby={errors.email ? 'email-error' : undefined}
/>
{errors.email && (
  <span id="email-error" role="alert" className="text-red-500">
    {errors.email}
  </span>
)}
```

## Keyboard Navigation

```typescript
// ✅ GOOD: Handle keyboard events
function SearchInput() {
  const handleKeyDown = (e: React.KeyboardEvent) => {
    if (e.key === 'Escape') {
      // Clear search
    }
  };

  return (
    <input
      type="search"
      placeholder="Tìm kiếm..."
      onKeyDown={handleKeyDown}
    />
  );
}
```

---

# PART 7: Security Best Practices

## XSS Prevention

```typescript
// ✅ GOOD: React escapes by default
function UserProfile({ user }: { user: User }) {
  return <div>{user.name}</div>; // Safe - React escapes
}

// ⚠️ CAUTION: Only use dangerouslySetInnerHTML with sanitized content
import DOMPurify from 'dompurify';

function RichTextDisplay({ html }: { html: string }) {
  const sanitized = DOMPurify.sanitize(html);
  return <div dangerouslySetInnerHTML={{ __html: sanitized }} />;
}
```

## Environment Variables

```typescript
// ✅ GOOD: Use NEXT_PUBLIC_ prefix for client-side env vars
const API_URL = process.env.NEXT_PUBLIC_API_URL;

// ❌ BAD: Never expose secrets
// process.env.SECRET_KEY // Only available on server

// ✅ GOOD: Validate env vars
if (!API_URL) {
  throw new Error('NEXT_PUBLIC_API_URL is not defined');
}
```

## Input Validation

```typescript
// ✅ GOOD: Always validate user input
import { z } from 'zod';

const studentSchema = z.object({
  name: z.string().min(2).max(100),
  email: z.string().email(),
  phone: z.string().regex(/^0\d{9}$/),
});

function StudentForm() {
  const form = useForm({
    resolver: zodResolver(studentSchema),
  });
  // ...
}
```

---

# PART 8: Pre-Commit Checklist

## Frontend Code Quality Checklist

Before committing frontend code, verify:

### TypeScript
- [ ] No `any` types used (check with `grep -r ":\s*any" src/`)
- [ ] All props interfaces defined
- [ ] Proper return types for functions
- [ ] No TypeScript errors (`pnpm tsc --noEmit`)

### React
- [ ] Components follow naming conventions (PascalCase)
- [ ] Hooks called at top level only
- [ ] Proper dependency arrays in useEffect/useMemo/useCallback
- [ ] No inline function definitions in JSX (use useCallback)

### Testing
- [ ] Unit tests for new hooks
- [ ] Component tests for new UI components
- [ ] All tests pass (`pnpm test`)
- [ ] Coverage meets requirements

### Code Quality
- [ ] ESLint passes (`pnpm lint`)
- [ ] Prettier formatted (`pnpm format`)
- [ ] No console.log statements (except console.warn/error)
- [ ] No commented-out code

### Accessibility
- [ ] Semantic HTML used
- [ ] ARIA labels on interactive elements
- [ ] Keyboard navigation works
- [ ] Color contrast meets WCAG AA

### Performance
- [ ] Large lists virtualized
- [ ] Images optimized (next/image)
- [ ] Heavy components lazy loaded
- [ ] Expensive calculations memoized

### Security
- [ ] User input validated with Zod
- [ ] No dangerouslySetInnerHTML without sanitization
- [ ] No sensitive data in client-side code
- [ ] Environment variables properly prefixed

---

# PART 9: Git Hooks Integration

## Pre-commit Hook Script

```bash
#!/bin/bash
# .git/hooks/pre-commit

echo "🔍 Running Frontend Code Quality Checks..."

# Colors
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m'

# Check if there are staged frontend files
STAGED_FILES=$(git diff --cached --name-only --diff-filter=ACM | grep -E '\.(tsx?|jsx?)$')

if [ -z "$STAGED_FILES" ]; then
  echo -e "${GREEN}✅ No frontend files to check${NC}"
  exit 0
fi

echo "📝 Checking TypeScript files..."

# 1. Check for 'any' type
echo -e "\n🚨 Checking for 'any' types..."
ANY_USAGE=$(echo "$STAGED_FILES" | xargs grep -n ":\s*any" 2>/dev/null || true)
if [ -n "$ANY_USAGE" ]; then
  echo -e "${RED}❌ Found 'any' type usage (forbidden):${NC}"
  echo "$ANY_USAGE"
  echo -e "${YELLOW}Please replace 'any' with proper types${NC}"
  exit 1
fi
echo -e "${GREEN}✅ No 'any' types found${NC}"

# 2. Check for console.log
echo -e "\n🚨 Checking for console.log..."
CONSOLE_LOG=$(echo "$STAGED_FILES" | xargs grep -n "console\.log" 2>/dev/null || true)
if [ -n "$CONSOLE_LOG" ]; then
  echo -e "${YELLOW}⚠️  Found console.log statements:${NC}"
  echo "$CONSOLE_LOG"
  echo -e "${YELLOW}Consider removing or replacing with console.warn/error${NC}"
  # Warning only, don't exit
fi

# 3. TypeScript type check
if [ -f "kiteclass/kiteclass-frontend/tsconfig.json" ]; then
  echo -e "\n📘 Running TypeScript type check..."
  cd kiteclass/kiteclass-frontend
  if ! pnpm tsc --noEmit; then
    echo -e "${RED}❌ TypeScript type check failed${NC}"
    exit 1
  fi
  cd ../..
  echo -e "${GREEN}✅ TypeScript type check passed${NC}"
fi

# 4. ESLint
if [ -f "kiteclass/kiteclass-frontend/.eslintrc.json" ]; then
  echo -e "\n🔧 Running ESLint..."
  cd kiteclass/kiteclass-frontend
  if ! pnpm lint --quiet; then
    echo -e "${RED}❌ ESLint check failed${NC}"
    exit 1
  fi
  cd ../..
  echo -e "${GREEN}✅ ESLint passed${NC}"
fi

# 5. Check for TODO without ticket reference
echo -e "\n📝 Checking for TODO comments..."
TODO_WITHOUT_TICKET=$(echo "$STAGED_FILES" | xargs grep -n "TODO" | grep -v "TODO(" 2>/dev/null || true)
if [ -n "$TODO_WITHOUT_TICKET" ]; then
  echo -e "${YELLOW}⚠️  Found TODO without ticket reference:${NC}"
  echo "$TODO_WITHOUT_TICKET"
  echo -e "${YELLOW}Format: // TODO(TICKET-123): Description${NC}"
fi

echo -e "\n━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo -e "${GREEN}✅ All frontend checks passed! Safe to commit.${NC}"
echo -e "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n"

exit 0
```

---

# PART 10: Common Mistakes to Avoid

## ❌ Common Anti-Patterns

### 1. Prop Drilling

```typescript
// ❌ BAD: Deep prop drilling
function App() {
  const user = useUser();
  return <Layout user={user} />;
}

function Layout({ user }) {
  return <Sidebar user={user} />;
}

function Sidebar({ user }) {
  return <UserMenu user={user} />;
}

// ✅ GOOD: Use Context or Zustand
const UserContext = createContext<User | null>(null);

function App() {
  const user = useUser();
  return (
    <UserContext.Provider value={user}>
      <Layout />
    </UserContext.Provider>
  );
}

function UserMenu() {
  const user = useContext(UserContext);
  return <div>{user?.name}</div>;
}
```

### 2. Unnecessary Re-renders

```typescript
// ❌ BAD: Creates new object on every render
function StudentList() {
  const style = { color: 'red' };  // ❌ New object each render
  return <div style={style}>...</div>;
}

// ✅ GOOD: Define outside component or use useMemo
const style = { color: 'red' };

function StudentList() {
  return <div style={style}>...</div>;
}
```

### 3. Missing Dependencies in useEffect

```typescript
// ❌ BAD: Missing dependencies
function Component({ userId }) {
  useEffect(() => {
    fetchUser(userId);  // ❌ userId not in deps
  }, []);
}

// ✅ GOOD: Include all dependencies
function Component({ userId }) {
  useEffect(() => {
    fetchUser(userId);
  }, [userId]);
}
```

---

# Actions

### Run Tests
```bash
pnpm test              # Run all tests
pnpm test:watch        # Watch mode
pnpm test:coverage     # With coverage
pnpm test:e2e          # E2E tests
```

### Code Quality
```bash
pnpm lint              # ESLint
pnpm lint:fix          # Fix auto-fixable issues
pnpm format            # Prettier
pnpm tsc --noEmit      # Type check
```

### Pre-commit Manual Check
```bash
# Check for 'any' types
grep -r ":\s*any" src/

# Check for console.log
grep -r "console\.log" src/

# Run all checks
pnpm lint && pnpm tsc --noEmit && pnpm test
```

---

# Summary

✅ **ALWAYS**:
- Use proper TypeScript types (no `any`)
- Write tests for new code
- Follow React hooks rules
- Use semantic HTML
- Validate user input
- Format with Prettier

❌ **NEVER**:
- Use `any` type
- Hardcode strings (use constants)
- Leave console.log in production
- Use dangerouslySetInnerHTML without sanitization
- Ignore ESLint errors
- Skip accessibility attributes
