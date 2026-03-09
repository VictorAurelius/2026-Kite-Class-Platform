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

---

# PART 11: Multi-Tenant & Theme System Quality

## Multi-Tenant Architecture

KiteClass Platform là multi-tenant system - mỗi instance có theme và branding riêng.

### Theme Hierarchy

```
┌─────────────────────────────────────────────────────────────┐
│                    THEME SYSTEM LAYERS                       │
├─────────────────────────────────────────────────────────────┤
│                                                              │
│  Layer 1: Theme Template (from KiteHub)                     │
│  ┌────────────────────────────────────────────────────┐     │
│  │ Customer chọn: Classic, Modern, Friendly, etc.     │     │
│  │ Defines: Base colors, typography, component styles │     │
│  └────────────────────────────────────────────────────┘     │
│                          ↓                                   │
│  Layer 2: Branding Settings (Instance Level)                │
│  ┌────────────────────────────────────────────────────┐     │
│  │ Center Admin tùy chỉnh: Logo, Primary Color        │     │
│  │ Overrides template defaults                        │     │
│  └────────────────────────────────────────────────────┘     │
│                          ↓                                   │
│  Layer 3: User Preferences (User Level)                     │
│  ┌────────────────────────────────────────────────────┐     │
│  │ End User chọn: Dark/Light mode, Font size          │     │
│  │ Personal preferences only                          │     │
│  └────────────────────────────────────────────────────┘     │
│                                                              │
└─────────────────────────────────────────────────────────────┘
```

## Type Safety for Theme System

### Required Types

```typescript
// src/types/theme.ts

// Theme Template (from KiteHub API)
export interface ThemeTemplate {
  id: string;
  name: string;
  description: string;
  thumbnailUrl: string;
  tier: 'free' | 'pro' | 'enterprise';
  config: ThemeConfig;
}

export interface ThemeConfig {
  colors: {
    primary: ColorScale;
    secondary: ColorScale;
    accent: ColorScale;
    neutral: ColorScale;
    success: string;
    warning: string;
    error: string;
    info: string;
  };
  typography: {
    fontFamily: {
      sans: string;
      mono: string;
    };
    fontSize: Record<string, string>;
    fontWeight: Record<string, number>;
  };
  spacing: Record<string, string>;
  borderRadius: Record<string, string>;
}

export interface ColorScale {
  50: string;
  100: string;
  200: string;
  300: string;
  400: string;
  500: string; // Default
  600: string;
  700: string;
  800: string;
  900: string;
}

// Branding Settings (Instance Level)
export interface BrandingSettings {
  logoUrl: string | null;
  faviconUrl: string | null;
  displayName: string;
  tagline: string | null;
  primaryColor: string | null; // Hex color, overrides template
  secondaryColor: string | null;
  contactEmail: string | null;
  contactPhone: string | null;
  address: string | null;
  socialLinks: {
    facebook?: string;
    zalo?: string;
  };
}

// User Preferences
export interface UserPreferences {
  colorMode: 'light' | 'dark' | 'system';
  fontSize: 'small' | 'medium' | 'large';
  compactMode: boolean;
}

// Resolved Theme (combined)
export interface ResolvedTheme {
  template: ThemeTemplate;
  branding: BrandingSettings;
  userPrefs: UserPreferences;
  effectiveColorMode: 'light' | 'dark';
  effectivePrimaryColor: string;
}
```

## CSS Variables Pattern

### ✅ GOOD: Scoped CSS Variables

```typescript
// lib/theme-utils.ts
export function applyThemeVariables(theme: ResolvedTheme): void {
  const root = document.documentElement;
  
  // Apply primary color (from branding or template)
  const primaryColor = theme.branding.primaryColor || 
                       theme.template.config.colors.primary[500];
  
  root.style.setProperty('--color-primary', primaryColor);
  
  // Generate color scale from primary
  const scale = generateColorScale(primaryColor);
  Object.entries(scale).forEach(([key, value]) => {
    root.style.setProperty(`--color-primary-${key}`, value);
  });
  
  // Apply typography
  root.style.setProperty(
    '--font-sans', 
    theme.template.config.typography.fontFamily.sans
  );
}

// Validate color format
function validateHexColor(color: string): boolean {
  return /^#[0-9A-Fa-f]{6}$/.test(color);
}

// Generate color scale (lighten/darken)
function generateColorScale(baseColor: string): ColorScale {
  if (!validateHexColor(baseColor)) {
    throw new Error(`Invalid color format: ${baseColor}`);
  }
  // Use chroma.js or similar library
  return {
    50: lighten(baseColor, 0.95),
    100: lighten(baseColor, 0.9),
    // ...
    900: darken(baseColor, 0.5),
  };
}
```

### ❌ BAD: Hardcoded Theme Values

```typescript
// ❌ DON'T hardcode theme colors
const PRIMARY_COLOR = '#0ea5e9';

// ❌ DON'T use inline styles
<div style={{ color: '#0ea5e9' }}>Text</div>

// ✅ DO use CSS variables
<div className="text-primary">Text</div>
```

## Testing Theme System

### Test Theme Provider

```typescript
// src/__tests__/providers/theme-provider.test.tsx
import { describe, it, expect, vi, beforeEach } from 'vitest';
import { renderHook, waitFor } from '@testing-library/react';
import { ThemeProvider, useTheme } from '@/providers/theme-provider';
import * as api from '@/lib/api/client';

vi.mock('@/lib/api/client');

describe('ThemeProvider', () => {
  const mockTheme: ResolvedTheme = {
    template: {
      id: 'classic',
      name: 'Classic',
      config: {
        colors: {
          primary: {
            500: '#1e40af',
            // ...
          },
        },
      },
    },
    branding: {
      displayName: 'Test Center',
      primaryColor: '#0ea5e9', // Override template
      logoUrl: null,
    },
    userPrefs: {
      colorMode: 'light',
      fontSize: 'medium',
      compactMode: false,
    },
    effectiveColorMode: 'light',
    effectivePrimaryColor: '#0ea5e9',
  };

  beforeEach(() => {
    vi.clearAllMocks();
  });

  it('fetches and applies theme on mount', async () => {
    vi.mocked(api.get).mockResolvedValue({ data: mockTheme });

    const { result } = renderHook(() => useTheme(), {
      wrapper: ThemeProvider,
    });

    await waitFor(() => {
      expect(result.current.theme).toEqual(mockTheme);
    });

    // Check CSS variables applied
    const primaryColor = document.documentElement.style.getPropertyValue(
      '--color-primary'
    );
    expect(primaryColor).toBe('#0ea5e9');
  });

  it('uses branding color over template color', async () => {
    vi.mocked(api.get).mockResolvedValue({ data: mockTheme });

    const { result } = renderHook(() => useTheme(), {
      wrapper: ThemeProvider,
    });

    await waitFor(() => {
      expect(result.current.theme?.effectivePrimaryColor).toBe('#0ea5e9');
    });
  });

  it('switches color mode', async () => {
    vi.mocked(api.get).mockResolvedValue({ data: mockTheme });

    const { result } = renderHook(() => useTheme(), {
      wrapper: ThemeProvider,
    });

    await waitFor(() => expect(result.current.theme).toBeDefined());

    // Switch to dark mode
    result.current.setColorMode('dark');

    await waitFor(() => {
      expect(document.documentElement.classList.contains('dark')).toBe(true);
    });
  });
});
```

### Test Theme Isolation

```typescript
// src/__tests__/lib/theme-utils.test.ts
import { describe, it, expect, beforeEach, afterEach } from 'vitest';
import { applyThemeVariables } from '@/lib/theme-utils';

describe('Theme Isolation', () => {
  beforeEach(() => {
    // Clear all CSS variables
    document.documentElement.removeAttribute('style');
  });

  afterEach(() => {
    document.documentElement.removeAttribute('style');
  });

  it('applies theme variables without global pollution', () => {
    const theme1 = createMockTheme({ primaryColor: '#0ea5e9' });
    applyThemeVariables(theme1);

    expect(
      document.documentElement.style.getPropertyValue('--color-primary')
    ).toBe('#0ea5e9');

    // Apply different theme (simulating different tenant)
    const theme2 = createMockTheme({ primaryColor: '#7c3aed' });
    applyThemeVariables(theme2);

    // Should completely replace, not merge
    expect(
      document.documentElement.style.getPropertyValue('--color-primary')
    ).toBe('#7c3aed');
  });

  it('validates color format before applying', () => {
    const invalidTheme = createMockTheme({ primaryColor: 'invalid' });

    expect(() => applyThemeVariables(invalidTheme)).toThrow(
      'Invalid color format'
    );
  });
});
```

## Security Considerations

### ⚠️ CSS Injection Prevention

```typescript
// ✅ GOOD: Validate and sanitize colors
export function sanitizeBrandingSettings(
  settings: BrandingSettings
): BrandingSettings {
  return {
    ...settings,
    // Only allow valid hex colors
    primaryColor: settings.primaryColor
      ? validateAndSanitizeColor(settings.primaryColor)
      : null,
    secondaryColor: settings.secondaryColor
      ? validateAndSanitizeColor(settings.secondaryColor)
      : null,
    // Sanitize URLs
    logoUrl: settings.logoUrl ? sanitizeUrl(settings.logoUrl) : null,
    faviconUrl: settings.faviconUrl ? sanitizeUrl(settings.faviconUrl) : null,
  };
}

function validateAndSanitizeColor(color: string): string | null {
  // Only accept hex format
  const hexRegex = /^#[0-9A-Fa-f]{6}$/;
  if (!hexRegex.test(color)) {
    console.warn(`Invalid color format: ${color}`);
    return null;
  }
  return color.toLowerCase();
}

function sanitizeUrl(url: string): string | null {
  try {
    const parsed = new URL(url);
    // Only allow https
    if (parsed.protocol !== 'https:') {
      console.warn(`Invalid protocol: ${parsed.protocol}`);
      return null;
    }
    return url;
  } catch {
    console.warn(`Invalid URL: ${url}`);
    return null;
  }
}
```

### ❌ NEVER Allow Custom CSS

```typescript
// ❌ NEVER do this - allows arbitrary CSS injection
interface BrandingSettings {
  customCSS?: string; // ❌ DANGEROUS!
}

// ❌ NEVER inject custom CSS directly
const style = document.createElement('style');
style.textContent = customCSS; // ❌ XSS risk
document.head.appendChild(style);

// ✅ DO: Only allow predefined theme templates + color overrides
```

## Performance Optimization

### Theme Caching

```typescript
// lib/theme-cache.ts
const THEME_CACHE_KEY = 'kiteclass:theme';
const THEME_CACHE_TTL = 1000 * 60 * 60; // 1 hour

export function getCachedTheme(): ResolvedTheme | null {
  try {
    const cached = localStorage.getItem(THEME_CACHE_KEY);
    if (!cached) return null;

    const { theme, timestamp } = JSON.parse(cached);
    
    // Check if expired
    if (Date.now() - timestamp > THEME_CACHE_TTL) {
      localStorage.removeItem(THEME_CACHE_KEY);
      return null;
    }

    return theme;
  } catch {
    return null;
  }
}

export function setCachedTheme(theme: ResolvedTheme): void {
  try {
    localStorage.setItem(
      THEME_CACHE_KEY,
      JSON.stringify({
        theme,
        timestamp: Date.now(),
      })
    );
  } catch (error) {
    console.warn('Failed to cache theme:', error);
  }
}
```

### Avoid Theme Flash

```typescript
// app/layout.tsx
export default function RootLayout({ children }: { children: React.ReactNode }) {
  return (
    <html lang="vi" suppressHydrationWarning>
      {/* Inline script to prevent flash */}
      <head>
        <script
          dangerouslySetInnerHTML={{
            __html: `
              (function() {
                try {
                  const cached = localStorage.getItem('kiteclass:theme');
                  if (cached) {
                    const { theme } = JSON.parse(cached);
                    const primaryColor = theme.branding.primaryColor || 
                                       theme.template.config.colors.primary[500];
                    document.documentElement.style.setProperty('--color-primary', primaryColor);
                  }
                } catch {}
              })();
            `,
          }}
        />
      </head>
      <body>
        <ThemeProvider>{children}</ThemeProvider>
      </body>
    </html>
  );
}
```

## Quality Checklist for Theme System

### Development
- [ ] All theme types properly defined (ThemeTemplate, BrandingSettings, etc.)
- [ ] ThemeProvider fetches theme from API
- [ ] CSS variables applied dynamically
- [ ] Color validation for branding settings
- [ ] URL sanitization for logos/favicons
- [ ] Theme caching implemented
- [ ] No theme flash on page load

### Testing
- [ ] ThemeProvider tested with MSW
- [ ] Theme switching tested
- [ ] Branding override tested (primary color)
- [ ] Color validation tested
- [ ] Theme isolation tested (no cross-contamination)
- [ ] Performance tested (no lag on theme switch)

### Security
- [ ] Only hex colors allowed (#RRGGBB format)
- [ ] Only HTTPS URLs allowed for logos
- [ ] No custom CSS injection allowed
- [ ] Input sanitization for all branding fields
- [ ] CSP headers configured properly

### Accessibility
- [ ] Color contrast meets WCAG AA in all themes
- [ ] Dark mode properly implemented
- [ ] Font sizes adjustable by user
- [ ] Theme switching keyboard accessible

---

## Summary for Multi-Tenant Frontend

**Key Principles:**
1. **Type Safety First**: All theme configurations typed
2. **Security by Design**: Validate and sanitize all inputs
3. **Performance Matters**: Cache themes, avoid flash
4. **Test Isolation**: Each tenant's theme independent
5. **User Experience**: Smooth theme switching, no flicker

**Common Mistakes to Avoid:**
- ❌ Hardcoding theme colors in components
- ❌ Allowing custom CSS injection
- ❌ Not validating color formats
- ❌ Not caching theme (API call on every page)
- ❌ Theme flash on page load
- ❌ Not testing theme switching

**Best Practices:**
- ✅ Use CSS variables for all theme properties
- ✅ Validate all user inputs (colors, URLs)
- ✅ Cache theme in localStorage
- ✅ Test theme isolation thoroughly
- ✅ Support SSR without theme flash
- ✅ Type all theme interfaces strictly

---

# PART 12: Feature Flag System & Tier-Based UI

## Multi-Tier Subscription Architecture

KiteClass có 3 gói chính + 2 add-ons:
- **BASIC** (500k/tháng): ≤50 học viên, core features only
- **STANDARD** (1tr/tháng): ≤200 học viên, + Engagement Pack
- **PREMIUM** (2tr/tháng): Unlimited, + AI Marketing Agent

**Add-ons:**
- **ENGAGEMENT PACK** (+300k): Gamification, Parent Portal, Forum
- **MEDIA PACK** (+500k): Video Upload, Live Streaming

## Feature Detection Pattern

### Type Definitions

```typescript
// src/types/subscription.ts
export type SubscriptionTier = 'BASIC' | 'STANDARD' | 'PREMIUM';

export interface InstanceFeatures {
  tier: SubscriptionTier;
  services: ServiceType[];
  features: FeatureFlags;
  limits: ResourceLimits;
}

export type ServiceType =
  | 'user-gateway'
  | 'core'
  | 'engagement'
  | 'media'
  | 'frontend';

export interface FeatureFlags {
  // Core features (all tiers)
  classManagement: boolean;
  studentManagement: boolean;
  attendance: boolean;
  grading: boolean;
  billing: boolean;

  // Engagement Pack features (STANDARD+)
  gamification: boolean;
  parentPortal: boolean;
  forum: boolean;

  // Media Pack features (add-on)
  videoUpload: boolean;
  liveStreaming: boolean;

  // Premium features
  aiMarketing: boolean;
  prioritySupport: boolean;
}

export interface ResourceLimits {
  maxStudents: number; // 50, 200, or Infinity
  maxCourses?: number;
  videoStorageGB: number; // 0, 50, or Infinity
  maxConcurrentStreams?: number;
}

// API Response
export interface InstanceConfig {
  instanceId: string;
  tier: SubscriptionTier;
  addOns: ('ENGAGEMENT' | 'MEDIA')[];
  features: FeatureFlags;
  limits: ResourceLimits;
}
```

### Feature Flag Provider

```typescript
// src/providers/FeatureFlagProvider.tsx
'use client';

import { createContext, useContext, useEffect, useState } from 'react';
import { InstanceConfig, FeatureFlags } from '@/types/subscription';

interface FeatureFlagContextValue {
  config: InstanceConfig | null;
  features: FeatureFlags | null;
  isLoading: boolean;
  error: Error | null;
  hasFeature: (feature: keyof FeatureFlags) => boolean;
  hasTier: (tier: SubscriptionTier) => boolean;
  checkLimit: (resource: 'students' | 'courses' | 'videoGB') => {
    current: number;
    max: number;
    isAtLimit: boolean;
  };
}

const FeatureFlagContext = createContext<FeatureFlagContextValue | null>(null);

export function FeatureFlagProvider({ children }: { children: React.ReactNode }) {
  const [config, setConfig] = useState<InstanceConfig | null>(null);
  const [isLoading, setIsLoading] = useState(true);
  const [error, setError] = useState<Error | null>(null);

  useEffect(() => {
    fetchFeatureFlags();
  }, []);

  const fetchFeatureFlags = async () => {
    try {
      setIsLoading(true);
      const response = await fetch('/api/instance/config');
      if (!response.ok) throw new Error('Failed to fetch feature flags');
      const data = await response.json();
      setConfig(data);
      setError(null);
    } catch (err) {
      setError(err as Error);
      console.error('Feature flag fetch error:', err);
    } finally {
      setIsLoading(false);
    }
  };

  const hasFeature = (feature: keyof FeatureFlags): boolean => {
    return config?.features[feature] ?? false;
  };

  const hasTier = (tier: SubscriptionTier): boolean => {
    if (!config) return false;
    const tierOrder = { BASIC: 0, STANDARD: 1, PREMIUM: 2 };
    return tierOrder[config.tier] >= tierOrder[tier];
  };

  const checkLimit = (resource: 'students' | 'courses' | 'videoGB') => {
    // Mock implementation - replace with actual API call
    return {
      current: 0,
      max: config?.limits.maxStudents ?? 0,
      isAtLimit: false,
    };
  };

  return (
    <FeatureFlagContext.Provider
      value={{
        config,
        features: config?.features ?? null,
        isLoading,
        error,
        hasFeature,
        hasTier,
        checkLimit,
      }}
    >
      {children}
    </FeatureFlagContext.Provider>
  );
}

export function useFeatureFlag(feature: keyof FeatureFlags): boolean {
  const context = useContext(FeatureFlagContext);
  if (!context) throw new Error('useFeatureFlag must be used within FeatureFlagProvider');
  return context.hasFeature(feature);
}

export function useFeatureFlags() {
  const context = useContext(FeatureFlagContext);
  if (!context) throw new Error('useFeatureFlags must be used within FeatureFlagProvider');
  return context;
}
```

### Conditional UI Rendering

```typescript
// src/components/navigation/MainNav.tsx
import { useFeatureFlag } from '@/providers/FeatureFlagProvider';

export function MainNav() {
  const hasGamification = useFeatureFlag('gamification');
  const hasParentPortal = useFeatureFlag('parentPortal');
  const hasForum = useFeatureFlag('forum');
  const hasVideoUpload = useFeatureFlag('videoUpload');

  return (
    <nav className="flex flex-col gap-2">
      {/* Core features - always visible */}
      <NavItem href="/classes" icon={BookOpen}>Lớp học</NavItem>
      <NavItem href="/students" icon={Users}>Học viên</NavItem>
      <NavItem href="/attendance" icon={CheckCircle}>Điểm danh</NavItem>
      <NavItem href="/billing" icon={DollarSign}>Học phí</NavItem>

      {/* Engagement Pack features - conditional */}
      {hasGamification && (
        <NavItem href="/gamification" icon={Trophy}>Game hóa</NavItem>
      )}
      {hasParentPortal && (
        <NavItem href="/parents" icon={UserCheck}>Phụ huynh</NavItem>
      )}
      {hasForum && (
        <NavItem href="/forum" icon={MessageSquare}>Diễn đàn</NavItem>
      )}

      {/* Media Pack features - conditional */}
      {hasVideoUpload && (
        <NavItem href="/media" icon={Video}>Video</NavItem>
      )}
    </nav>
  );
}
```

### Feature-Locked Components

```typescript
// src/components/upgrade/FeatureLock.tsx
import { useFeatureFlags } from '@/providers/FeatureFlagProvider';
import { Lock } from 'lucide-react';
import { Button } from '@/components/ui/button';
import { Card } from '@/components/ui/card';

interface FeatureLockProps {
  feature: keyof FeatureFlags;
  featureName: string;
  requiredTier: SubscriptionTier;
  children?: React.ReactNode;
}

export function FeatureLock({
  feature,
  featureName,
  requiredTier,
  children,
}: FeatureLockProps) {
  const { hasFeature, config } = useFeatureFlags();

  // Feature is available
  if (hasFeature(feature)) {
    return <>{children}</>;
  }

  // Feature is locked
  return (
    <Card className="p-8 text-center">
      <Lock className="mx-auto mb-4 h-16 w-16 text-muted-foreground" />
      <h3 className="mb-2 text-xl font-semibold">
        {featureName} chỉ có trên gói {requiredTier}
      </h3>
      <p className="mb-4 text-muted-foreground">
        Bạn đang dùng gói {config?.tier}. Nâng cấp để sử dụng tính năng này.
      </p>
      <div className="flex gap-2 justify-center">
        <Button onClick={() => window.location.href = '/upgrade'}>
          Nâng cấp gói
        </Button>
        <Button variant="outline" onClick={() => window.history.back()}>
          Quay lại
        </Button>
      </div>
    </Card>
  );
}

// Usage example
export function GamificationPage() {
  return (
    <FeatureLock
      feature="gamification"
      featureName="Game hóa"
      requiredTier="STANDARD"
    >
      <GamificationDashboard />
    </FeatureLock>
  );
}
```

### Resource Limit Warnings

```typescript
// src/components/limits/StudentLimitWarning.tsx
import { useFeatureFlags } from '@/providers/FeatureFlagProvider';
import { Alert, AlertTitle, AlertDescription } from '@/components/ui/alert';

export function StudentLimitWarning({ currentCount }: { currentCount: number }) {
  const { config } = useFeatureFlags();

  if (!config) return null;

  const { maxStudents } = config.limits;
  const percentage = (currentCount / maxStudents) * 100;

  // At 80% capacity
  if (percentage >= 80 && percentage < 100) {
    return (
      <Alert variant="warning">
        <AlertTitle>Sắp đạt giới hạn học viên</AlertTitle>
        <AlertDescription>
          Bạn đang có {currentCount}/{maxStudents} học viên ({percentage.toFixed(0)}%).
          Nâng cấp gói để tăng giới hạn.
        </AlertDescription>
      </Alert>
    );
  }

  // At 100% capacity
  if (percentage >= 100) {
    return (
      <Alert variant="destructive">
        <AlertTitle>Đã đạt giới hạn học viên</AlertTitle>
        <AlertDescription>
          Bạn đã đạt giới hạn {maxStudents} học viên.
          Vui lòng nâng cấp gói để thêm học viên mới.
        </AlertDescription>
      </Alert>
    );
  }

  return null;
}
```

## Testing Feature Flags

```typescript
// src/__tests__/providers/feature-flag-provider.test.tsx
import { describe, it, expect, vi } from 'vitest';
import { render, screen, waitFor } from '@testing-library/react';
import { FeatureFlagProvider, useFeatureFlag } from '@/providers/FeatureFlagProvider';

// Test component
function TestComponent({ feature }: { feature: keyof FeatureFlags }) {
  const hasFeature = useFeatureFlag(feature);
  return <div>{hasFeature ? 'Enabled' : 'Disabled'}</div>;
}

describe('FeatureFlagProvider', () => {
  it('loads BASIC tier features correctly', async () => {
    global.fetch = vi.fn().mockResolvedValue({
      ok: true,
      json: async () => ({
        tier: 'BASIC',
        features: {
          classManagement: true,
          gamification: false,
          parentPortal: false,
          videoUpload: false,
        },
      }),
    });

    render(
      <FeatureFlagProvider>
        <TestComponent feature="classManagement" />
        <TestComponent feature="gamification" />
      </FeatureFlagProvider>
    );

    await waitFor(() => {
      expect(screen.getByText('Enabled')).toBeInTheDocument();
      expect(screen.getByText('Disabled')).toBeInTheDocument();
    });
  });

  it('loads STANDARD tier with Engagement Pack', async () => {
    global.fetch = vi.fn().mockResolvedValue({
      ok: true,
      json: async () => ({
        tier: 'STANDARD',
        features: {
          classManagement: true,
          gamification: true,
          parentPortal: true,
          forum: true,
          videoUpload: false,
        },
      }),
    });

    render(
      <FeatureFlagProvider>
        <TestComponent feature="gamification" />
        <TestComponent feature="videoUpload" />
      </FeatureFlagProvider>
    );

    await waitFor(() => {
      const results = screen.getAllByText(/Enabled|Disabled/);
      expect(results[0]).toHaveTextContent('Enabled'); // gamification
      expect(results[1]).toHaveTextContent('Disabled'); // videoUpload
    });
  });
});
```

## Best Practices

### ✅ DO
- Fetch feature flags once on app load, cache in context
- Use `useFeatureFlag` hook for conditional rendering
- Show upgrade prompts for locked features
- Test all tier combinations
- Provide clear messaging about tier limitations

### ❌ DON'T
- Don't hardcode feature availability in components
- Don't make API calls on every feature check
- Don't hide features without explanation
- Don't allow actions that exceed tier limits
- Don't forget to handle loading/error states

---

# PART 13: AI-Generated Content Integration

## AI Branding Assets System

KiteClass sử dụng AI để tạo **10+ marketing assets** từ 1 ảnh upload:
- Profile images (3 variations)
- Hero banner (1920x600)
- Section banners (3 items)
- Logo variants (3 items)
- Open Graph image
- Marketing copy (headlines, CTAs)

## Type Definitions

```typescript
// src/types/branding.ts
export interface BrandingAssets {
  profileImages: {
    cutout: string; // Background removed
    circle: string; // 400x400 circle crop
    square: string; // 400x400 square crop
  };
  heroBanner: string; // 1920x600 AI-generated gradient
  sectionBanners: {
    about: string;
    courses: string;
    contact: string;
  };
  logos: {
    primary: string; // Cutout + circular bg + org name
    secondary: string; // Alternate color scheme
    iconOnly: string; // No text
  };
  ogImage: string; // 1200x630 for social sharing
  marketingCopy: {
    heroHeadline: string; // 10 words max
    subHeadline: string; // 20 words max
    callToAction: string;
    valueProps: string[]; // 3 items
  };
}

export interface BrandingGenerationJob {
  jobId: string;
  status: 'pending' | 'processing' | 'completed' | 'failed';
  progress: number; // 0-100
  currentStep: string;
  assets?: BrandingAssets;
  error?: string;
  createdAt: Date;
  completedAt?: Date;
}
```

## Asset Display Components

```typescript
// src/components/branding/BrandedHero.tsx
import { useBranding } from '@/providers/BrandingProvider';
import Image from 'next/image';

export function BrandedHero() {
  const { branding, isLoading } = useBranding();

  if (isLoading) {
    return <HeroSkeleton />;
  }

  if (!branding.generatedAssets) {
    return <DefaultHero />;
  }

  const { heroBanner, marketingCopy } = branding.generatedAssets;

  return (
    <section
      className="relative h-[600px] flex items-center justify-center text-white"
      style={{
        backgroundImage: `url(${heroBanner})`,
        backgroundSize: 'cover',
        backgroundPosition: 'center',
      }}
    >
      <div className="relative z-10 max-w-4xl text-center">
        <h1 className="text-5xl font-bold mb-4">
          {marketingCopy.heroHeadline}
        </h1>
        <p className="text-xl mb-8">
          {marketingCopy.subHeadline}
        </p>
        <button className="bg-primary px-8 py-3 rounded-lg text-lg font-semibold">
          {marketingCopy.callToAction}
        </button>
      </div>
      {/* Overlay for better text readability */}
      <div className="absolute inset-0 bg-black/30" />
    </section>
  );
}
```

## Asset URL Validation

```typescript
// src/lib/asset-validation.ts

/**
 * Validates CDN URLs for AI-generated assets
 * Only allow trusted CDN domains
 */
export function isValidAssetUrl(url: string): boolean {
  const ALLOWED_CDN_DOMAINS = [
    'cdn.kiteclass.com',
    'r2.cloudflare.com',
    's3.amazonaws.com',
  ];

  try {
    const urlObj = new URL(url);

    // Must be HTTPS
    if (urlObj.protocol !== 'https:') {
      console.warn(`Asset URL must use HTTPS: ${url}`);
      return false;
    }

    // Must be from allowed CDN
    const isAllowedDomain = ALLOWED_CDN_DOMAINS.some(domain =>
      urlObj.hostname.endsWith(domain)
    );

    if (!isAllowedDomain) {
      console.warn(`Asset URL from untrusted domain: ${urlObj.hostname}`);
      return false;
    }

    return true;
  } catch {
    console.warn(`Invalid asset URL: ${url}`);
    return false;
  }
}

/**
 * Validates all assets in BrandingAssets object
 */
export function validateBrandingAssets(assets: BrandingAssets): boolean {
  const urls = [
    assets.heroBanner,
    assets.ogImage,
    ...Object.values(assets.profileImages),
    ...Object.values(assets.sectionBanners),
    ...Object.values(assets.logos),
  ];

  return urls.every(url => isValidAssetUrl(url));
}
```

## Image Optimization

```typescript
// src/components/branding/OptimizedBrandImage.tsx
import Image from 'next/image';
import { isValidAssetUrl } from '@/lib/asset-validation';

interface OptimizedBrandImageProps {
  src: string;
  alt: string;
  width: number;
  height: number;
  priority?: boolean;
  className?: string;
}

export function OptimizedBrandImage({
  src,
  alt,
  width,
  height,
  priority = false,
  className,
}: OptimizedBrandImageProps) {
  // Validate URL before rendering
  if (!isValidAssetUrl(src)) {
    console.error('Invalid brand image URL:', src);
    return <ImagePlaceholder width={width} height={height} />;
  }

  return (
    <Image
      src={src}
      alt={alt}
      width={width}
      height={height}
      priority={priority}
      className={className}
      // Use Next.js image optimization with CDN
      loader={({ src, width, quality }) => {
        // CDN already optimized, just pass through
        return src;
      }}
    />
  );
}
```

## Asset Generation UI

```typescript
// src/app/(admin)/branding/page.tsx
'use client';

import { useState } from 'react';
import { Upload } from 'lucide-react';
import { Progress } from '@/components/ui/progress';
import { Card } from '@/components/ui/card';

export default function BrandingUploadPage() {
  const [isGenerating, setIsGenerating] = useState(false);
  const [progress, setProgress] = useState(0);
  const [currentStep, setCurrentStep] = useState('');
  const [result, setResult] = useState<BrandingAssets | null>(null);

  const handleUpload = async (file: File) => {
    try {
      setIsGenerating(true);
      setProgress(0);

      // Upload image
      const formData = new FormData();
      formData.append('image', file);

      const response = await fetch('/api/branding/generate', {
        method: 'POST',
        body: formData,
      });

      const { jobId } = await response.json();

      // Poll for completion
      const assets = await pollGenerationStatus(jobId, (status) => {
        setProgress(status.progress);
        setCurrentStep(status.currentStep);
      });

      setResult(assets);
    } catch (error) {
      console.error('Branding generation failed:', error);
    } finally {
      setIsGenerating(false);
    }
  };

  return (
    <div className="container mx-auto py-8">
      <h1 className="text-3xl font-bold mb-8">
        Tạo Branding tự động với AI
      </h1>

      {!isGenerating && !result && (
        <Card className="p-8">
          <ImageUploadZone onUpload={handleUpload} />
          <p className="mt-4 text-muted-foreground text-center">
            Upload logo hoặc ảnh đại diện. AI sẽ tạo 10+ marketing assets trong ~5 phút.
          </p>
        </Card>
      )}

      {isGenerating && (
        <Card className="p-8">
          <div className="space-y-4">
            <Progress value={progress} className="w-full" />
            <p className="text-center font-medium">{currentStep}</p>
            <p className="text-center text-sm text-muted-foreground">
              {progress}% hoàn thành
            </p>
          </div>
        </Card>
      )}

      {result && (
        <div className="space-y-8">
          <Card className="p-8">
            <h2 className="text-2xl font-semibold mb-4">Asset Preview</h2>
            <BrandingAssetPreview assets={result} />
          </Card>
          <div className="flex gap-4">
            <Button onClick={() => applyBranding(result)}>
              Áp dụng Branding
            </Button>
            <Button variant="outline" onClick={() => setResult(null)}>
              Tạo lại
            </Button>
          </div>
        </div>
      )}
    </div>
  );
}

async function pollGenerationStatus(
  jobId: string,
  onProgress: (status: BrandingGenerationJob) => void
): Promise<BrandingAssets> {
  const maxAttempts = 60; // 5 minutes (5s interval)
  let attempts = 0;

  while (attempts < maxAttempts) {
    const response = await fetch(`/api/branding/status/${jobId}`);
    const status: BrandingGenerationJob = await response.json();

    onProgress(status);

    if (status.status === 'completed' && status.assets) {
      return status.assets;
    }

    if (status.status === 'failed') {
      throw new Error(status.error || 'Generation failed');
    }

    await new Promise(resolve => setTimeout(resolve, 5000));
    attempts++;
  }

  throw new Error('Generation timeout');
}
```

## Performance: Asset Caching

```typescript
// src/lib/branding-cache.ts
const BRANDING_CACHE_KEY = 'kiteclass:branding_assets';
const CACHE_TTL = 7 * 24 * 60 * 60 * 1000; // 7 days

export function getCachedBrandingAssets(): BrandingAssets | null {
  try {
    const cached = localStorage.getItem(BRANDING_CACHE_KEY);
    if (!cached) return null;

    const { assets, timestamp } = JSON.parse(cached);

    // Check expiration
    if (Date.now() - timestamp > CACHE_TTL) {
      localStorage.removeItem(BRANDING_CACHE_KEY);
      return null;
    }

    // Validate all URLs before returning
    if (!validateBrandingAssets(assets)) {
      console.warn('Cached assets failed validation');
      localStorage.removeItem(BRANDING_CACHE_KEY);
      return null;
    }

    return assets;
  } catch {
    return null;
  }
}

export function setCachedBrandingAssets(assets: BrandingAssets): void {
  try {
    localStorage.setItem(
      BRANDING_CACHE_KEY,
      JSON.stringify({
        assets,
        timestamp: Date.now(),
      })
    );
  } catch (error) {
    console.warn('Failed to cache branding assets:', error);
  }
}
```

## Testing AI-Generated Content

```typescript
// src/__tests__/lib/asset-validation.test.ts
import { describe, it, expect } from 'vitest';
import { isValidAssetUrl, validateBrandingAssets } from '@/lib/asset-validation';

describe('Asset URL Validation', () => {
  it('accepts valid CDN URLs', () => {
    expect(isValidAssetUrl('https://cdn.kiteclass.com/banner.jpg')).toBe(true);
    expect(isValidAssetUrl('https://r2.cloudflare.com/assets/logo.png')).toBe(true);
  });

  it('rejects HTTP (not HTTPS)', () => {
    expect(isValidAssetUrl('http://cdn.kiteclass.com/banner.jpg')).toBe(false);
  });

  it('rejects untrusted domains', () => {
    expect(isValidAssetUrl('https://evil.com/malicious.jpg')).toBe(false);
  });

  it('validates complete BrandingAssets object', () => {
    const validAssets: BrandingAssets = {
      heroBanner: 'https://cdn.kiteclass.com/hero.jpg',
      profileImages: {
        cutout: 'https://cdn.kiteclass.com/cutout.png',
        circle: 'https://cdn.kiteclass.com/circle.png',
        square: 'https://cdn.kiteclass.com/square.png',
      },
      // ... all other fields with valid URLs
    };

    expect(validateBrandingAssets(validAssets)).toBe(true);
  });
});
```

## Best Practices

### ✅ DO
- Validate all asset URLs before rendering
- Use Next.js Image component for optimization
- Cache assets in localStorage (7-day TTL)
- Preload hero banner for better LCP
- Lazy load section banners
- Show loading states during generation
- Poll generation status with timeout

### ❌ DON'T
- Don't trust asset URLs without validation
- Don't render images without alt text
- Don't skip optimization for AI-generated images
- Don't cache failed/invalid assets
- Don't block UI during asset generation
- Don't forget to handle generation errors

---

# PART 14: Guest User & Public Routes

## Public vs Authenticated Routes

KiteClass có 2 loại routes:
1. **Public routes**: Landing page, pricing, features (không cần auth)
2. **Authenticated routes**: Dashboard, classes, students (cần login)

## Route Structure

```typescript
// App Router structure
src/app/
├── (public)/              # Public group (no auth required)
│   ├── page.tsx           # Landing page
│   ├── pricing/page.tsx   # Pricing page
│   ├── features/page.tsx  # Features showcase
│   └── contact/page.tsx   # Contact form
│
├── (auth)/                # Auth group (login/register)
│   ├── login/page.tsx
│   ├── register/page.tsx
│   └── forgot-password/page.tsx
│
└── (authenticated)/       # Protected group (requires auth)
    ├── dashboard/page.tsx
    ├── classes/page.tsx
    ├── students/page.tsx
    └── ...
```

## Authentication Middleware

```typescript
// src/middleware.ts
import { NextResponse } from 'next/server';
import type { NextRequest } from 'next/server';

export function middleware(request: NextRequest) {
  const { pathname } = request.nextUrl;

  // Public routes - allow without auth
  const publicRoutes = ['/', '/pricing', '/features', '/contact'];
  const authRoutes = ['/login', '/register', '/forgot-password'];

  if (publicRoutes.includes(pathname) || authRoutes.includes(pathname)) {
    return NextResponse.next();
  }

  // Protected routes - check for auth token
  const token = request.cookies.get('auth_token')?.value;

  if (!token) {
    // Redirect to login with return URL
    const url = new URL('/login', request.url);
    url.searchParams.set('returnUrl', pathname);
    return NextResponse.redirect(url);
  }

  return NextResponse.next();
}

export const config = {
  matcher: [
    /*
     * Match all request paths except:
     * - api (API routes)
     * - _next/static (static files)
     * - _next/image (image optimization)
     * - favicon.ico (favicon)
     */
    '/((?!api|_next/static|_next/image|favicon.ico).*)',
  ],
};
```

## Guest User Types

```typescript
// src/types/user.ts
export type UserRole =
  | 'CENTER_OWNER'
  | 'CENTER_ADMIN'
  | 'TEACHER'
  | 'STUDENT'
  | 'PARENT'
  | 'GUEST'; // NEW

export type AccountType =
  | 'TRIAL'      // 14-day trial (full features, limited students)
  | 'FREE'       // Free tier (limited features)
  | 'PAID';      // Paid subscription

export interface User {
  id: string;
  email: string;
  name: string;
  role: UserRole;
  accountType: AccountType;

  // Trial-specific fields
  trialStartedAt?: Date;
  trialExpiresAt?: Date;
  isTrialExpired?: boolean;

  // Limitations
  limitations?: {
    maxStudents?: number;
    maxCourses?: number;
    allowedFeatures?: string[];
  };
}

export interface GuestSession {
  sessionId: string;
  createdAt: Date;
  expiresAt: Date;
  viewedCourses: string[];
  source?: string; // 'landing_page', 'social_media', etc.
}
```

## Public Landing Page

```typescript
// src/app/(public)/page.tsx
import { BrandedHero } from '@/components/branding/BrandedHero';
import { FeatureShowcase } from '@/components/landing/FeatureShowcase';
import { PricingSection } from '@/components/landing/PricingSection';
import { CTASection } from '@/components/landing/CTASection';

export default function LandingPage() {
  return (
    <main>
      <BrandedHero />
      <FeatureShowcase />
      <PricingSection />
      <CTASection />
    </main>
  );
}

// SEO metadata
export const metadata = {
  title: 'KiteClass - Quản lý trung tâm thông minh',
  description: 'Quản lý lớp học, học viên, điểm danh, học phí với AI Marketing',
  openGraph: {
    title: 'KiteClass Platform',
    description: 'Quản lý trung tâm toàn diện với AI',
    images: ['/og-image.jpg'],
  },
};
```

## Trial Signup Flow

```typescript
// src/components/trial/TrialSignupButton.tsx
'use client';

import { useState } from 'react';
import { Button } from '@/components/ui/button';
import { Dialog, DialogContent } from '@/components/ui/dialog';
import { TrialSignupForm } from './TrialSignupForm';

export function TrialSignupButton() {
  const [isOpen, setIsOpen] = useState(false);

  return (
    <>
      <Button size="lg" onClick={() => setIsOpen(true)}>
        Dùng thử miễn phí 14 ngày
      </Button>

      <Dialog open={isOpen} onOpenChange={setIsOpen}>
        <DialogContent className="max-w-md">
          <TrialSignupForm onSuccess={() => {
            // Redirect to trial dashboard
            window.location.href = '/dashboard?trial=true';
          }} />
        </DialogContent>
      </Dialog>
    </>
  );
}

// src/components/trial/TrialSignupForm.tsx
export function TrialSignupForm({ onSuccess }: { onSuccess: () => void }) {
  const handleSubmit = async (data: TrialSignupData) => {
    try {
      const response = await fetch('/api/auth/trial-signup', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(data),
      });

      if (!response.ok) throw new Error('Signup failed');

      const { user, token } = await response.json();

      // Store auth token
      document.cookie = `auth_token=${token}; path=/; max-age=604800`; // 7 days

      onSuccess();
    } catch (error) {
      console.error('Trial signup error:', error);
    }
  };

  return (
    <form onSubmit={handleSubmit}>
      <h2 className="text-2xl font-bold mb-4">Bắt đầu dùng thử</h2>
      <p className="text-muted-foreground mb-6">
        Trải nghiệm đầy đủ tính năng trong 14 ngày, không cần thẻ tín dụng.
      </p>

      <Input
        name="organizationName"
        label="Tên trung tâm"
        required
      />
      <Input
        name="name"
        label="Họ và tên"
        required
      />
      <Input
        name="email"
        type="email"
        label="Email"
        required
      />
      <Input
        name="phone"
        type="tel"
        label="Số điện thoại"
        required
      />

      <Button type="submit" className="w-full">
        Bắt đầu dùng thử
      </Button>

      <p className="mt-4 text-xs text-muted-foreground text-center">
        Bằng cách đăng ký, bạn đồng ý với Điều khoản dịch vụ và Chính sách bảo mật.
      </p>
    </form>
  );
}
```

## Trial Expiration Handling

```typescript
// src/components/trial/TrialExpirationBanner.tsx
import { useAuth } from '@/providers/AuthProvider';
import { Alert, AlertTitle, AlertDescription } from '@/components/ui/alert';
import { Button } from '@/components/ui/button';
import { differenceInDays } from 'date-fns';

export function TrialExpirationBanner() {
  const { user } = useAuth();

  if (!user || user.accountType !== 'TRIAL' || !user.trialExpiresAt) {
    return null;
  }

  const daysLeft = differenceInDays(new Date(user.trialExpiresAt), new Date());

  // Trial expired
  if (daysLeft <= 0) {
    return (
      <Alert variant="destructive" className="mb-4">
        <AlertTitle>Thời gian dùng thử đã hết</AlertTitle>
        <AlertDescription className="flex items-center justify-between">
          <span>Nâng cấp lên gói trả phí để tiếp tục sử dụng.</span>
          <Button onClick={() => window.location.href = '/upgrade'}>
            Nâng cấp ngay
          </Button>
        </AlertDescription>
      </Alert>
    );
  }

  // 3 days or less remaining
  if (daysLeft <= 3) {
    return (
      <Alert variant="warning" className="mb-4">
        <AlertTitle>Thời gian dùng thử còn {daysLeft} ngày</AlertTitle>
        <AlertDescription className="flex items-center justify-between">
          <span>Nâng cấp ngay để không bị gián đoạn dịch vụ.</span>
          <Button variant="outline" onClick={() => window.location.href = '/pricing'}>
            Xem gói
          </Button>
        </AlertDescription>
      </Alert>
    );
  }

  return null;
}
```

## Guest Analytics

```typescript
// src/lib/analytics/guest-tracking.ts

/**
 * Track guest user behavior (anonymous)
 */
export function trackGuestAction(action: string, metadata?: Record<string, unknown>) {
  // Get or create guest session
  const sessionId = getOrCreateGuestSession();

  // Send to analytics
  fetch('/api/analytics/guest', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({
      sessionId,
      action,
      metadata,
      timestamp: new Date().toISOString(),
      page: window.location.pathname,
      referrer: document.referrer,
    }),
  }).catch(err => console.warn('Analytics tracking failed:', err));
}

function getOrCreateGuestSession(): string {
  const GUEST_SESSION_KEY = 'kiteclass:guest_session';

  let session = localStorage.getItem(GUEST_SESSION_KEY);

  if (!session) {
    session = `guest_${Date.now()}_${Math.random().toString(36).substr(2, 9)}`;
    localStorage.setItem(GUEST_SESSION_KEY, session);
  }

  return session;
}

// Usage examples
trackGuestAction('view_pricing');
trackGuestAction('click_trial_button');
trackGuestAction('view_course', { courseId: '123' });
```

## Testing Public Routes

```typescript
// src/__tests__/pages/public-landing.test.tsx
import { describe, it, expect } from 'vitest';
import { render, screen } from '@testing-library/react';
import LandingPage from '@/app/(public)/page';

describe('Public Landing Page', () => {
  it('renders without authentication', () => {
    render(<LandingPage />);

    expect(screen.getByText(/Dùng thử miễn phí/i)).toBeInTheDocument();
    expect(screen.getByText(/Xem demo/i)).toBeInTheDocument();
  });

  it('shows trial signup button', () => {
    render(<LandingPage />);

    const trialButton = screen.getByRole('button', { name: /Dùng thử miễn phí 14 ngày/i });
    expect(trialButton).toBeInTheDocument();
  });

  it('has proper SEO metadata', () => {
    // Verify metadata is exported
    expect(metadata.title).toBe('KiteClass - Quản lý trung tâm thông minh');
    expect(metadata.openGraph?.images).toBeDefined();
  });
});
```

## B2B Owner-Centric Model (PART 4)

**Critical:** KiteClass follows a **B2B owner-centric model**, NOT B2C:
- Trial is for **CENTER_OWNER** only (business owners), NOT for students
- Guests CANNOT self-register for courses
- Guests must **contact OWNER** to enroll
- OWNER manages all enrollments manually
- Guest flow: Browse → Contact OWNER → Owner handles enrollment

### Contact Owner Section

```tsx
// components/landing/ContactOwnerSection.tsx
import { Button } from '@/components/ui/button';
import { Card, CardHeader, CardTitle, CardDescription, CardContent } from '@/components/ui/card';
import { Alert, AlertDescription } from '@/components/ui/alert';
import { Facebook, MessageCircle, MessageSquare, Phone, Mail, AlertCircle } from 'lucide-react';

interface OwnerContactInfo {
  ownerName: string;
  phone?: string;
  email?: string;
  facebookUrl?: string;
  messengerUrl?: string;
  zaloUrl?: string;
}

interface ContactOwnerSectionProps {
  contactInfo: OwnerContactInfo;
}

/**
 * ContactOwnerSection - Hiển thị thông tin liên hệ OWNER
 *
 * IMPORTANT: B2B Model - Guest KHÔNG thể tự đăng ký
 * Guest phải liên hệ OWNER để được tư vấn và tuyển sinh
 */
export function ContactOwnerSection({ contactInfo }: ContactOwnerSectionProps) {
  return (
    <Card className="bg-gradient-to-r from-primary/10 to-primary/5">
      <CardHeader>
        <CardTitle>📞 Liên hệ tư vấn</CardTitle>
        <CardDescription>
          Bạn quan tâm đến khóa học? Liên hệ trực tiếp với trung tâm!
        </CardDescription>
      </CardHeader>
      <CardContent className="space-y-4">
        {/* Owner name */}
        <div>
          <h4 className="font-semibold mb-2">
            Liên hệ: {contactInfo.ownerName}
          </h4>
        </div>

        {/* Contact buttons - Prominent & Mobile-friendly */}
        <div className="grid grid-cols-2 md:grid-cols-3 gap-3">
          {/* Facebook */}
          {contactInfo.facebookUrl && (
            <Button
              variant="outline"
              className="h-12"
              onClick={() => window.open(contactInfo.facebookUrl, '_blank')}
            >
              <Facebook className="mr-2 h-5 w-5 text-blue-600" />
              Facebook
            </Button>
          )}

          {/* Messenger */}
          {contactInfo.messengerUrl && (
            <Button
              variant="outline"
              className="h-12"
              onClick={() => window.open(contactInfo.messengerUrl, '_blank')}
            >
              <MessageCircle className="mr-2 h-5 w-5 text-blue-500" />
              Messenger
            </Button>
          )}

          {/* Zalo */}
          {contactInfo.zaloUrl && (
            <Button
              variant="outline"
              className="h-12"
              onClick={() => window.open(contactInfo.zaloUrl, '_blank')}
            >
              <MessageSquare className="mr-2 h-5 w-5 text-blue-700" />
              Zalo
            </Button>
          )}

          {/* Phone */}
          {contactInfo.phone && (
            <Button
              variant="outline"
              className="h-12"
              onClick={() => window.location.href = `tel:${contactInfo.phone}`}
            >
              <Phone className="mr-2 h-5 w-5 text-green-600" />
              {contactInfo.phone}
            </Button>
          )}

          {/* Email */}
          {contactInfo.email && (
            <Button
              variant="outline"
              className="h-12 col-span-2 md:col-span-1"
              onClick={() => window.location.href = `mailto:${contactInfo.email}`}
            >
              <Mail className="mr-2 h-5 w-5 text-red-600" />
              {contactInfo.email}
            </Button>
          )}
        </div>

        {/* B2B Model Alert */}
        <Alert>
          <AlertCircle className="h-4 w-4" />
          <AlertDescription>
            💡 <strong>Lưu ý:</strong> Chúng tôi không hỗ trợ đăng ký trực tuyến.
            Vui lòng liên hệ trung tâm để được tư vấn chi tiết về khóa học
            và thủ tục tuyển sinh.
          </AlertDescription>
        </Alert>
      </CardContent>
    </Card>
  );
}
```

### Public Course Visibility Control

```typescript
// types/course.ts
export enum PublicVisibility {
  PRIVATE = 'PRIVATE',  // Guest không thấy
  PUBLIC = 'PUBLIC'     // Guest thấy trong public catalog
}

export interface Course {
  id: string;
  title: string;
  description: string;
  publicVisibility: PublicVisibility; // Admin-controlled
  // ... other fields
}
```

```tsx
// components/forms/CourseForm.tsx
import { RadioGroup, RadioGroupItem } from '@/components/ui/radio-group';

function CourseForm() {
  return (
    <Form>
      {/* ... existing fields ... */}

      {/* Public Visibility Control */}
      <FormField
        control={form.control}
        name="publicVisibility"
        render={({ field }) => (
          <FormItem>
            <FormLabel>🌐 Public Visibility</FormLabel>
            <FormDescription>
              Kiểm soát khóa học có hiển thị trên trang web công khai không
            </FormDescription>
            <FormControl>
              <RadioGroup
                onValueChange={field.onChange}
                defaultValue={field.value}
                className="flex flex-col space-y-1"
              >
                <FormItem className="flex items-center space-x-3 space-y-0">
                  <FormControl>
                    <RadioGroupItem value="PRIVATE" />
                  </FormControl>
                  <FormLabel className="font-normal">
                    🔒 Private - Chỉ thành viên mới thấy
                  </FormLabel>
                </FormItem>
                <FormItem className="flex items-center space-x-3 space-y-0">
                  <FormControl>
                    <RadioGroupItem value="PUBLIC" />
                  </FormControl>
                  <FormLabel className="font-normal">
                    🌍 Public - Hiển thị trên website công khai
                  </FormLabel>
                </FormItem>
              </RadioGroup>
            </FormControl>
            <FormMessage />
          </FormItem>
        )}
      />

      {/* Warning when PUBLIC */}
      {form.watch('publicVisibility') === 'PUBLIC' && (
        <Alert>
          <AlertCircle className="h-4 w-4" />
          <AlertTitle>Public Course</AlertTitle>
          <AlertDescription>
            Khóa học này sẽ hiển thị trên trang web công khai.
            Guest có thể xem thông tin nhưng KHÔNG thể tự đăng ký.
            Guest phải liên hệ OWNER để tuyển sinh (B2B model).
          </AlertDescription>
        </Alert>
      )}
    </Form>
  );
}
```

### Trial System - B2B Model

```typescript
// types/trial.ts

/**
 * CRITICAL: Trial chỉ dành cho CENTER_OWNER (business owners)
 * KHÔNG dành cho students hoặc end-users
 */
export interface TrialConfig {
  // Trial duration
  trialDurationDays: 14;
  gracePeriodDays: 3;          // Read-only mode
  dataRetentionDays: 90;       // After grace period

  // Base tier during trial
  baseTier: 'BASIC';           // 50 students, 10 courses

  // Features available during trial (ALL features FREE)
  availableFeatures: [
    'ENGAGEMENT',  // Attendance, grades, progress tracking
    'MEDIA',       // Image/video in lessons
    'PREMIUM'      // Analytics, reporting, AI branding
  ];

  // Early-bird discount
  earlyBirdDiscount: {
    percentage: 20;
    validDays: 10;             // Day 1-10 of trial
    message: 'Giảm 20% nếu nâng cấp trong 10 ngày đầu!';
  };

  // Duplicate prevention
  preventDuplicateTrial: {
    checkEmail: true;
    checkPhone: true;
    checkBusinessName: false;  // Allow multiple trials per business (different locations)
  };
}

export interface TrialUser {
  id: string;
  email: string;
  phone: string;
  role: 'CENTER_OWNER';        // MUST be OWNER
  accountType: 'TRIAL';
  trialStartedAt: Date;
  trialExpiresAt: Date;
  organizationName: string;    // Business name

  // Trial limitations
  limitations: {
    maxStudents: 50;
    maxCourses: 10;
    // All features available
  };
}
```

```tsx
// components/trial/TrialSignupForm.tsx
export function TrialSignupForm() {
  return (
    <form>
      <h2 className="text-2xl font-bold mb-4">Dùng thử 14 ngày</h2>
      <p className="text-muted-foreground mb-6">
        ✨ Dành cho <strong>chủ trung tâm</strong> muốn trải nghiệm hệ thống quản lý.
        Đầy đủ tính năng, không cần thẻ tín dụng.
      </p>

      <Alert className="mb-4">
        <AlertCircle className="h-4 w-4" />
        <AlertDescription>
          🎯 <strong>B2B Model:</strong> Trial dành cho chủ trung tâm (CENTER_OWNER).
          Sau khi đăng ký, bạn sẽ có instance riêng để quản lý học viên và khóa học.
        </AlertDescription>
      </Alert>

      <Input
        name="organizationName"
        label="Tên trung tâm"
        required
      />
      <Input
        name="name"
        label="Họ và tên (Chủ trung tâm)"
        required
      />
      <Input
        name="email"
        type="email"
        label="Email"
        required
      />
      <Input
        name="phone"
        type="tel"
        label="Số điện thoại"
        required
      />

      <Button type="submit" className="w-full">
        Bắt đầu dùng thử
      </Button>

      <div className="mt-4 space-y-2 text-xs text-muted-foreground">
        <p>✅ 14 ngày dùng thử đầy đủ tính năng</p>
        <p>✅ Không cần thẻ tín dụng</p>
        <p>✅ Giảm 20% nếu nâng cấp trong 10 ngày đầu</p>
        <p>✅ Hỗ trợ thiết lập miễn phí</p>
      </div>
    </form>
  );
}
```

## Best Practices

### ✅ DO
- Use route groups to organize public vs authenticated pages
- Implement middleware for auth checking
- Show trial expiration warnings 3+ days before
- Track guest behavior for conversion optimization
- Provide clear CTAs for trial signup
- Handle trial expiration gracefully
- Use SSR for public pages (SEO)
- **Display ContactOwnerSection prominently on public course pages** ⭐ NEW
- **Emphasize B2B model - trial for OWNER, not students** ⭐ NEW
- **Allow ADMIN to control Course.publicVisibility** ⭐ NEW
- **Track guest analytics for OWNER insights** ⭐ NEW
- **Show contact buttons (Facebook, Messenger, Zalo) prominently** ⭐ NEW

### ❌ DON'T
- Don't show authenticated content to guests
- Don't require payment info for trials
- Don't expire trials without warning
- Don't lose guest progress on signup
- Don't forget GDPR compliance for guest tracking
- Don't skip loading states on auth check
- **Don't allow guests to self-register for courses** ⭐ NEW (B2B model)
- **Don't give trial access to students** ⭐ NEW (OWNER only)
- **Don't hide contact information from guests** ⭐ NEW
- **Don't auto-enroll guests** ⭐ NEW (manual OWNER-led process)

---

# PART 15: Documentation Standards

## Language Requirements

### Vietnamese for Business Documents

**Rule:** All architecture documents, reports, and business analysis MUST be written in Vietnamese.

**Rationale:**
- Product Owner is Vietnamese speaker
- Easier to read and understand
- Better communication with stakeholders
- Reduce misunderstanding
- Faster decision making

### When to Use Vietnamese

✅ **MUST use Vietnamese:**
- Architecture documents (system-architecture-*.md)
- Business analysis reports (preview-website-best-practices.md)
- Q&A documents (architecture-clarification-qa.md)
- Implementation plans (kiteclass-implementation-plan.md)
- User-facing content (landing pages, marketing copy)
- Meeting notes and decisions
- PRD (Product Requirements Document)
- Technical specifications for non-technical stakeholders

⚠️ **Can use English:**
- Code comments (if explaining complex logic)
- Type definitions and interfaces
- API documentation (standard REST conventions)
- Error messages (standard HTTP status)
- Git commit messages (concise English is acceptable)
- Technical terms (React, TypeScript, API, etc.)

❌ **DON'T use English for:**
- Architecture reports intended for Product Owner
- Business value analysis
- User stories and requirements
- Implementation planning documents

### Document Structure

```markdown
# [Tiêu đề Tiếng Việt]

**Ngày tạo:** 2026-01-30
**Mục đích:** [Mô tả ngắn gọn bằng tiếng Việt]
**Trạng thái:** [Draft / Review / Approved]
**Người đọc:** Product Owner / Tech Lead / Dev Team

---

## MỤC LỤC

1. [Tóm Tắt](#tom-tat)
2. [Vấn Đề](#van-de)
3. [Giải Pháp](#giai-phap)
...

## TÓM TẮT

[Nội dung tiếng Việt]

### Code Examples

```typescript
// Code có thể có comment tiếng Anh hoặc tiếng Việt
interface User {
  id: string        // User ID
  name: string      // Tên người dùng
  email: string     // Email
}
```

### Technical Terms

- Giữ nguyên: React, TypeScript, API, REST, JWT, OAuth
- Dịch: Component → Thành phần, Hook → Móc nối, State → Trạng thái
- Ưu tiên: Sử dụng thuật ngữ tiếng Anh khi phổ biến, giải thích tiếng Việt nếu cần

## KẾT LUẬN

[Nội dung tiếng Việt]
```

### Examples

#### ❌ BAD: English Report

```markdown
# Preview Website Best Practices

## Executive Summary

The Preview Website feature enables education centers to attract
prospective students through an SEO-optimized public website...

## Business Value

| Benefit | Impact |
|---------|--------|
| Student Acquisition | +30-50% enrollment |
...
```

#### ✅ GOOD: Vietnamese Report

```markdown
# Best Practices: Trang Web Giới Thiệu Công Khai

## TÓM TẮT

Tính năng Trang Web Giới Thiệu giúp trung tâm giáo dục thu hút
học viên tiềm năng thông qua website công khai được tối ưu SEO...

## GIÁ TRỊ KINH DOANH

| Lợi ích | Ảnh hưởng |
|---------|-----------|
| Thu hút học viên | +30-50% tuyển sinh |
...
```

### Translation Guidelines

**Common Technical Terms:**

| English | Tiếng Việt | Usage |
|---------|------------|-------|
| Landing Page | Trang đích / Trang giới thiệu | Use Vietnamese |
| Course Catalog | Danh mục khóa học | Use Vietnamese |
| Authentication | Xác thực | Use Vietnamese |
| Authorization | Phân quyền | Use Vietnamese |
| API Endpoint | API Endpoint | Keep English |
| REST API | REST API | Keep English |
| Component | Component / Thành phần | Either OK |
| Hook | Hook / Móc nối | Either OK |
| State Management | Quản lý State | Mix OK |
| Server-Side Rendering | Server-Side Rendering (SSR) | Keep English + abbreviation |

**Best Practice:** Use Vietnamese for concepts, keep English for established technical terms.

### Code Comments

```typescript
// ✅ GOOD: Mix Vietnamese explanation with English terms
/**
 * Lấy danh sách khóa học công khai
 *
 * @param instanceId - ID của instance
 * @returns Promise<PublicCourse[]> - Danh sách courses
 * @throws ApiError nếu instance không tồn tại
 */
async function fetchPublicCourses(instanceId: string): Promise<PublicCourse[]> {
  // Cache kết quả trong 30 phút
  const cached = await getCachedCourses(instanceId)
  if (cached) return cached

  // Gọi API và filter các course đã publish
  const courses = await api.get(`/public/instance/${instanceId}/courses`)
  return courses.filter(course => course.status === 'PUBLISHED')
}
```

### Commit Messages

**Acceptable Formats:**

```bash
# English (concise, standard format)
git commit -m "docs(architecture): add Preview Website best practices"

# Vietnamese (more descriptive)
git commit -m "docs(architecture): bổ sung best practices cho Preview Website"

# Mix (recommended for complex changes)
git commit -m "docs: bổ sung báo cáo Preview Website bằng tiếng Việt

- Thêm 60+ trang phân tích best practices
- Khuyến nghị: Public Marketing Landing Page
- Timeline: 2 tuần implementation
- Tech stack: Next.js SSG, ISR, public APIs"
```

## Quality Checklist

### Documentation Review

- [ ] Document is in Vietnamese (if business/architecture doc)
- [ ] Title is descriptive and in Vietnamese
- [ ] Has table of contents (Mục lục)
- [ ] Section headings are in Vietnamese
- [ ] Code examples have Vietnamese comments where helpful
- [ ] Technical terms are used appropriately (English when standard)
- [ ] Summary section in Vietnamese (Tóm tắt)
- [ ] Conclusion section in Vietnamese (Kết luận)
- [ ] Proper grammar and spelling

### Technical Terms

- [ ] API, REST, HTTP, JWT, OAuth → Keep English
- [ ] React, TypeScript, Next.js → Keep English
- [ ] Component, Hook, State → Either Vietnamese or English OK
- [ ] Business concepts → Use Vietnamese (tuyển sinh, khóa học, học viên)

---

## Summary

**Frontend Code Quality đã cover:**
1. ✅ TypeScript strict mode & type safety (PART 1)
2. ✅ React best practices (PART 2)
3. ✅ Testing requirements (PART 3)
4. ✅ Performance & a11y (PARTS 5-6)
5. ✅ Security best practices (PART 7)
6. ✅ Multi-tenant theme system (PART 11)
7. ✅ Feature flag system & tier-based UI (PART 12)
8. ✅ AI-generated content integration (PART 13)
9. ✅ Guest user & public routes (PART 14) - **UPDATED with B2B model**
10. ✅ Documentation standards (PART 15)

**Tất cả requirements từ 4 architecture concerns đã được addressed:**
- ✅ Pricing tier UI customization → PART 12 (Feature Detection & Tier-based UI)
- ✅ AI Branding system → PART 13 (AI-Generated Assets Integration)
- ✅ Preview Website → PART 14 (Public Marketing Landing Page) + Best Practices Report
- ✅ Guest user & Trial system → PART 14 (B2B Owner-Centric Model)

**PART 14 updates (PART 4: Guest & Trial System):**
- ✅ B2B owner-centric model (trial for OWNER only, not students)
- ✅ ContactOwnerSection component (Facebook, Messenger, Zalo buttons)
- ✅ Public course visibility control (Course.publicVisibility toggle)
- ✅ No auto-enrollment (guests contact OWNER)
- ✅ Guest analytics tracking for OWNER insights
- ✅ Trial system specifications (14 days, grace period, early-bird discount)
