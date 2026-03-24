# Frontend Standards

**Version:** 2.0 (Consolidated)
**Gop tu:** frontend-development, frontend-code-quality, code-style (frontend section)

---

## Muc luc nhanh

| Can gi | Xem section |
|--------|-------------|
| TypeScript strict / types | [1. TypeScript Quality](#1-typescript-quality) |
| React hooks / patterns | [2. React Best Practices](#2-react-best-practices) |
| Design system / Shadcn | [3. Design System](#3-design-system) |
| Theme / branding | [4. Theme System](#4-theme-system) |
| i18n / messages | [5. Internationalization](#5-internationalization) |
| Performance / accessibility | [6. Performance & A11y](#6-performance--accessibility) |
| Code organization | [7. Code Organization](#7-code-organization) |

---

## 1. TypeScript Quality

### TypeScript Config (tsconfig.json)

```json
{
  "compilerOptions": {
    "strict": true,
    "noImplicitAny": true,
    "strictNullChecks": true,
    "noUnusedLocals": true,
    "noUnusedParameters": true,
    "noImplicitReturns": true
  }
}
```

### Type Safety Rules

```typescript
// BAD: Never use `any`
function processData(data: any) { return data.value; }

// GOOD: Proper generic types
interface ApiResponse<T> {
  data: T;
  message: string;
  success: boolean;
}

function processData<T>(data: ApiResponse<T>): T {
  return data.data;
}
```

### API Types Pattern

```typescript
// Shared types in lib/types/
export interface Student {
  id: number;
  name: string;
  email: string;
  status: StudentStatus;
}

export type StudentStatus = 'ACTIVE' | 'INACTIVE' | 'PENDING';

// API request/response types
export interface CreateStudentRequest {
  name: string;
  email: string;
  phone?: string;
}

export interface PageResponse<T> {
  content: T[];
  totalElements: number;
  totalPages: number;
  page: number;
  size: number;
}
```

---

## 2. React Best Practices

### Custom Hook Pattern

```typescript
// hooks/use-students.ts
export function useStudents(params: StudentQueryParams) {
  return useQuery({
    queryKey: ['students', params],
    queryFn: () => studentApi.findAll(params),
    staleTime: 5 * 60 * 1000, // 5 minutes
  });
}

export function useCreateStudent() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: studentApi.create,
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['students'] });
      toast.success('Tao hoc vien thanh cong');
    },
    onError: (error) => {
      toast.error(getErrorMessage(error));
    },
  });
}
```

### Component Structure

```typescript
// components/students/student-form.tsx
interface StudentFormProps {
  initialData?: Student;
  onSubmit: (data: CreateStudentRequest) => void;
  isSubmitting?: boolean;
}

export function StudentForm({ initialData, onSubmit, isSubmitting }: StudentFormProps) {
  const form = useForm<CreateStudentRequest>({
    resolver: zodResolver(createStudentSchema),
    defaultValues: initialData ? {
      name: initialData.name,
      email: initialData.email,
    } : undefined,
  });

  return (
    <Form {...form}>
      <form onSubmit={form.handleSubmit(onSubmit)}>
        <FormField
          control={form.control}
          name="name"
          render={({ field }) => (
            <FormItem>
              <FormLabel>Ho ten</FormLabel>
              <FormControl>
                <Input {...field} />
              </FormControl>
              <FormMessage />
            </FormItem>
          )}
        />
        <Button type="submit" disabled={isSubmitting}>
          {isSubmitting ? 'Dang luu...' : 'Luu'}
        </Button>
      </form>
    </Form>
  );
}
```

### Zod Schema Pattern

```typescript
// lib/validations/student.ts
export const createStudentSchema = z.object({
  name: z.string().min(2, 'Ten phai co it nhat 2 ky tu').max(100),
  email: z.string().email('Email khong hop le'),
  phone: z.string().regex(/^(0|\+84)[0-9]{9}$/, 'So dien thoai khong hop le').optional(),
});

export type CreateStudentRequest = z.infer<typeof createStudentSchema>;
```

### React Hooks Rules

```typescript
// GOOD: Hooks only at top level
function StudentList() {
  const { data, isLoading, error } = useStudents({ page: 0 });
  // ...
}

// BAD: Conditional hooks
function BadComponent({ userId }: { userId?: number }) {
  if (!userId) return null;  // BAD: return before hook
  const user = useUser(userId);  // Hook after conditional return
}

// GOOD: Handle optional early
function GoodComponent({ userId }: { userId?: number }) {
  const user = useUser(userId ?? 0);  // Always call hook
  if (!userId || !user) return null;
}
```

---

## 3. Design System

### Tech Stack

- **UI Components**: Shadcn/UI (copy-paste, customizable)
- **Styling**: Tailwind CSS
- **Icons**: Lucide React
- **Forms**: React Hook Form + Zod
- **State**: React Query (server) + Zustand (client)

### Color Tokens (tailwind.config.js)

```typescript
const colors = {
  primary: { 500: '#0ea5e9' },  // Main blue
  success: { DEFAULT: '#22c55e' },
  warning: { DEFAULT: '#f59e0b' },
  error: { DEFAULT: '#ef4444' },
  info: { DEFAULT: '#3b82f6' },
};
```

### Component Imports

```typescript
// Always import from @/components/ui/
import { Button } from '@/components/ui/button';
import { Input } from '@/components/ui/input';
import { Form, FormField, FormItem, FormLabel, FormControl, FormMessage }
  from '@/components/ui/form';
import { DataTable } from '@/components/data-table';
```

### Loading / Error States Pattern

```typescript
export function StudentListPage() {
  const { data, isLoading, error } = useStudents({ page: 0 });

  if (isLoading) return <TableSkeleton />;
  if (error) return <ErrorMessage error={error} />;
  if (!data?.content?.length) return <EmptyState message="Chua co hoc vien nao" />;

  return <DataTable columns={studentColumns} data={data.content} />;
}
```

---

## 4. Theme System

### Hybrid Theme Architecture

```typescript
// 3-layer theme resolution:
// 1. Base theme (free for all)
// 2. Template theme (tier-based: FREE/PRO/ENTERPRISE)
// 3. Custom branding (override: logo, colors, fonts)

interface ThemeConfig {
  templateId: string;          // e.g., 'modern-light'
  primaryColor?: string;       // Custom override
  logoUrl?: string;            // Custom logo
  fontFamily?: string;         // Custom font
}
```

### Branding Application

```typescript
// Apply branding from API
export function applyBranding(branding: BrandingConfig) {
  const root = document.documentElement;

  if (branding.primaryColor) {
    root.style.setProperty('--primary', hexToHsl(branding.primaryColor));
  }
  if (branding.fontFamily) {
    root.style.setProperty('--font-sans', branding.fontFamily);
  }
}
```

---

## 5. Internationalization

### Vietnamese-first Pattern

```typescript
// All user-facing text in Vietnamese
const messages = {
  students: {
    title: 'Danh sach hoc vien',
    createSuccess: 'Tao hoc vien thanh cong',
    deleteConfirm: 'Ban co chac muon xoa hoc vien nay?',
  },
  errors: {
    required: 'Truong nay bat buoc',
    invalidEmail: 'Email khong hop le',
    network: 'Loi ket noi mang. Vui long thu lai.',
  },
};
```

### Date/Number Formatting

```typescript
// Vietnamese locale formatting
const dateFormatter = new Intl.DateTimeFormat('vi-VN', {
  day: '2-digit', month: '2-digit', year: 'numeric'
});

const currencyFormatter = new Intl.NumberFormat('vi-VN', {
  style: 'currency', currency: 'VND'
});

// Usage
dateFormatter.format(new Date());    // "23/03/2026"
currencyFormatter.format(500000);    // "500.000 ₫"
```

---

## 6. Performance & A11y

### Performance Rules

```typescript
// 1. Dynamic import for heavy components
const RichTextEditor = dynamic(() => import('./rich-text-editor'), {
  loading: () => <Skeleton className="h-64" />,
});

// 2. useMemo for expensive computations
const sortedStudents = useMemo(() =>
  students.sort((a, b) => a.name.localeCompare(b.name, 'vi')),
  [students]
);

// 3. React.memo for pure components
export const StudentCard = React.memo(({ student }: Props) => (
  <Card>{student.name}</Card>
));
```

### Accessibility (A11y) Rules

```typescript
// GOOD: Semantic HTML + ARIA
<Button
  aria-label="Xoa hoc vien Nguyen Van A"
  onClick={handleDelete}
>
  <Trash2 className="h-4 w-4" />
</Button>

// GOOD: Form labels
<Label htmlFor="student-name">Ho ten *</Label>
<Input id="student-name" aria-required="true" />

// GOOD: Error announcement
<p role="alert" aria-live="polite">
  {error && getErrorMessage(error)}
</p>
```

---

## 7. Code Organization

### File Structure

```
src/
├── app/                    # Next.js App Router pages
│   ├── (auth)/             # Auth routes group
│   ├── (dashboard)/        # Dashboard routes group
│   └── api/                # API routes
├── components/
│   ├── ui/                 # Shadcn base components
│   ├── students/           # Feature-specific components
│   └── shared/             # Shared components
├── hooks/                  # Custom React hooks
├── lib/
│   ├── api/                # API client functions
│   ├── types/              # TypeScript types
│   ├── validations/        # Zod schemas
│   └── utils/              # Utility functions
└── stores/                 # Zustand stores
```

### Import Order

```typescript
// 1. React
import { useState, useEffect } from 'react';

// 2. Third-party
import { useQuery } from '@tanstack/react-query';
import { zodResolver } from '@hookform/resolvers/zod';

// 3. Internal - absolute paths (@/)
import { Button } from '@/components/ui/button';
import { useStudents } from '@/hooks/use-students';
import type { Student } from '@/lib/types';
```

### Pre-commit Checklist

- [ ] No `any` types (use proper types)
- [ ] `pnpm lint` passes (0 errors)
- [ ] `pnpm typecheck` passes
- [ ] All components have loading/error/empty states
- [ ] Forms use Zod validation
- [ ] Vietnamese user-facing messages
- [ ] No hardcoded colors (use Tailwind tokens)
