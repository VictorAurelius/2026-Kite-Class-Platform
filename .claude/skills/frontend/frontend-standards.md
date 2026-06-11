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
| **Page templates (copy-paste)** | [8. Page Templates](#8-page-templates-copy-paste) |
| **Toast vs Modal vs Page** | [9. UX Pattern Rules](#9-ux-pattern-rules) |
| **Spacing conventions** | [10. Spacing & Layout Tokens](#10-spacing--layout-tokens) |
| **Responsive breakpoints** | [11. Responsive Conventions](#11-responsive-conventions) |

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
// 2. Template theme (tier-based: FREE/BASIC/PREMIUM/ENTERPRISE)
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

---

## 8. Page Templates (Copy-Paste)

### 8.1 List Page (CRUD)

```tsx
'use client';

import { useState } from 'react';
import Link from 'next/link';
import { Plus } from 'lucide-react';
import { Button } from '@/components/ui/button';
import { DataTable } from '@/components/common/data-table';
import { SearchInput } from '@/components/common/search-input';
import { LoadingSpinner } from '@/components/common/loading-spinner';
import { ErrorAlert } from '@/components/common/error-alert';
import { useItems } from '@/hooks/use-items';
import { columns } from './columns';

export default function ItemListPage() {
  const [search, setSearch] = useState('');
  const { data, isLoading, error } = useItems({ search });

  return (
    <div className="space-y-6">
      {/* Header */}
      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-3xl font-bold">Danh sách</h1>
          <p className="text-muted-foreground">Quản lý danh sách items</p>
        </div>
        <Link href="/dashboard/items/new">
          <Button><Plus className="mr-2 h-4 w-4" />Thêm mới</Button>
        </Link>
      </div>

      {/* Search */}
      <div className="flex items-center gap-4">
        <div className="max-w-md">
          <SearchInput placeholder="Tìm kiếm..." onSearch={setSearch} />
        </div>
      </div>

      {/* Content */}
      {isLoading && (
        <div className="flex justify-center py-12">
          <LoadingSpinner />
        </div>
      )}
      {error && <ErrorAlert title="Lỗi" message="Không thể tải dữ liệu" />}
      {data && <DataTable columns={columns} data={data.content} />}
    </div>
  );
}
```

### 8.2 Detail Page

```tsx
'use client';

import Link from 'next/link';
import { Pencil, Trash2 } from 'lucide-react';
import { Button } from '@/components/ui/button';
import { StatusBadge } from '@/components/common/status-badge';
import { ConfirmDialog } from '@/components/ui/confirm-dialog';
import { LoadingSpinner } from '@/components/common/loading-spinner';
import { useItem, useDeleteItem } from '@/hooks/use-items';

export default function ItemDetailPage({ params }: { params: { id: string } }) {
  const { data: item, isLoading } = useItem(params.id);
  const { mutate: deleteItem } = useDeleteItem();
  const [showDelete, setShowDelete] = useState(false);

  if (isLoading) return <div className="flex justify-center py-12"><LoadingSpinner /></div>;
  if (!item) return <ErrorAlert title="Không tìm thấy" />;

  return (
    <div className="space-y-6">
      {/* Header + Actions */}
      <div className="flex items-start justify-between">
        <div className="flex items-center gap-3">
          <h1 className="text-3xl font-bold">{item.name}</h1>
          <StatusBadge status={item.status} />
        </div>
        <div className="flex gap-2">
          <Link href={`/dashboard/items/${item.id}/edit`}>
            <Button variant="outline"><Pencil className="mr-2 h-4 w-4" />Chỉnh sửa</Button>
          </Link>
          <Button variant="destructive" onClick={() => setShowDelete(true)}>
            <Trash2 className="mr-2 h-4 w-4" />Xóa
          </Button>
        </div>
      </div>

      {/* Info Card */}
      <div className="rounded-lg border bg-card p-6 space-y-6">
        <div className="grid grid-cols-2 gap-4 md:grid-cols-3">
          <div>
            <p className="text-sm font-medium text-muted-foreground">Trường 1</p>
            <p className="mt-1">{item.field1}</p>
          </div>
          <div>
            <p className="text-sm font-medium text-muted-foreground">Trường 2</p>
            <p className="mt-1">{item.field2}</p>
          </div>
        </div>
      </div>

      {/* Delete Confirm */}
      <ConfirmDialog
        open={showDelete}
        onOpenChange={setShowDelete}
        onConfirm={() => deleteItem(item.id)}
        title="Xóa item"
        description="Hành động này không thể hoàn tác."
        confirmText="Xóa"
        variant="destructive"
      />
    </div>
  );
}
```

### 8.3 Form Page (Create/Edit)

```tsx
'use client';

import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { Button } from '@/components/ui/button';
import { FormInput } from '@/components/forms/form-input';
import { FormSelect } from '@/components/forms/form-select';
import { FormTextarea } from '@/components/forms/form-textarea';
import { LoadingSpinner } from '@/components/common/loading-spinner';
import { itemSchema, type ItemFormData } from '@/lib/validations/item';

export default function ItemFormPage({ initialData }: { initialData?: ItemFormData }) {
  const isEditing = !!initialData;
  const { register, handleSubmit, formState: { errors, isSubmitting } } = useForm({
    resolver: zodResolver(itemSchema),
    defaultValues: initialData,
  });

  const onSubmit = async (data: ItemFormData) => { /* mutation */ };

  return (
    <div className="space-y-6">
      <h1 className="text-3xl font-bold">{isEditing ? 'Chỉnh sửa' : 'Tạo mới'}</h1>

      <form onSubmit={handleSubmit(onSubmit)} className="space-y-6">
        {/* 2 columns on desktop, 1 on mobile */}
        <div className="grid grid-cols-1 gap-6 md:grid-cols-2">
          <FormInput label="Tên" required error={errors.name?.message} {...register('name')} />
          <FormInput label="Email" type="email" error={errors.email?.message} {...register('email')} />
        </div>

        {/* Full width fields */}
        <FormTextarea label="Mô tả" error={errors.description?.message} {...register('description')} />

        {/* Actions */}
        <div className="flex justify-end gap-4">
          <Button type="button" variant="outline" onClick={() => history.back()}>Hủy</Button>
          <Button type="submit" disabled={isSubmitting}>
            {isSubmitting ? <><LoadingSpinner size="sm" className="mr-2" />Đang lưu...</> : isEditing ? 'Cập nhật' : 'Tạo mới'}
          </Button>
        </div>
      </form>
    </div>
  );
}
```

### 8.4 Stats Dashboard

```tsx
// Stats cards row
<div className="grid gap-4 md:grid-cols-4">
  {stats.map((stat) => (
    <div key={stat.label} className="rounded-lg border bg-card p-4">
      <div className="flex items-center gap-3">
        <stat.icon className="h-8 w-8 text-muted-foreground" />
        <div>
          <p className="text-sm text-muted-foreground">{stat.label}</p>
          <p className="text-2xl font-bold">{stat.value}</p>
        </div>
      </div>
    </div>
  ))}
</div>
```

---

## 9. UX Pattern Rules

### Khi nào dùng gì?

| Action | Pattern | Lý do |
|--------|---------|-------|
| CRUD success/error | **Toast** | Không block UI, tự biến mất |
| Delete/destructive | **ConfirmDialog** | Cần xác nhận, không thể undo |
| View detail inline | **Sheet/Drawer** | Không rời trang hiện tại |
| Create/Edit full | **Page navigation** | Form phức tạp cần space |
| Validation error | **Inline field error** | Ngay dưới field bị lỗi |
| System error | **ErrorAlert** | Hiển thị rõ ràng, có retry |
| Loading | **Skeleton/Spinner** | Skeleton cho layout, Spinner cho data |

### Toast Rules

```tsx
// SUCCESS — dùng cho mọi CRUD success
toast({ title: "Thành công", description: "Đã tạo học viên" });

// ERROR — chỉ cho API errors
toast({ title: "Lỗi", description: message, variant: "destructive" });

// KHÔNG dùng toast cho: validation errors, navigation, loading states
```

### Confirm Dialog Rules

```tsx
// BẮT BUỘC cho: Delete, Archive, Suspend, Cancel (destructive actions)
// KHÔNG dùng: window.confirm() — luôn dùng <ConfirmDialog />
// Variant: "destructive" cho delete, "default" cho archive/cancel
```

---

## 10. Spacing & Layout Tokens

### Quy ước spacing (từ actual codebase)

| Token | Dùng cho | Ví dụ |
|-------|---------|-------|
| `gap-2` | Tight: icon+text, button groups | `flex items-center gap-2` |
| `gap-4` | **Default**: grid columns, form fields | `grid gap-4 md:grid-cols-2` |
| `gap-6` | Large: major sections | `grid gap-6` |
| `space-y-2` | Field label + input | Form field groups |
| `space-y-4` | Component sections | Card sections |
| `space-y-6` | **Page sections** | Main content wrapper |
| `p-4` | Container padding | Stat cards, bordered sections |
| `p-6` | **Card padding** | Info cards, form cards |
| `py-12` | Vertical centering | Loading states |

### Layout Rules

```tsx
// Page wrapper — LUÔN dùng space-y-6
<div className="space-y-6">
  {/* header */}
  {/* filters */}
  {/* content */}
</div>

// Card — LUÔN dùng p-6
<div className="rounded-lg border bg-card p-6 space-y-6">

// Form grid — LUÔN dùng gap-6 + md:grid-cols-2
<div className="grid grid-cols-1 gap-6 md:grid-cols-2">

// Stats grid — LUÔN dùng gap-4 + md:grid-cols-4
<div className="grid gap-4 md:grid-cols-4">
```

---

## 11. Responsive Conventions

### Breakpoint usage (từ actual codebase)

| Breakpoint | Width | Dùng cho |
|-----------|-------|---------|
| (none) | <768px | Mobile: 1 column, stack vertical |
| `sm:` | 640px+ | Rarely used, flex-row |
| `md:` | 768px+ | **Primary**: 2-3 columns, horizontal layout |
| `lg:` | 1024px+ | 4 columns (rare, chỉ cho grids lớn) |

### Common patterns

```tsx
// Grid responsive: 1 → 2 → 3 columns
<div className="grid grid-cols-1 gap-4 md:grid-cols-2 lg:grid-cols-3">

// Stats: 1 → 4 columns
<div className="grid gap-4 md:grid-cols-4">

// Flex stack → row
<div className="flex flex-col gap-4 sm:flex-row sm:items-center sm:justify-between">

// Hide on mobile
<nav className="hidden md:flex items-center gap-6">

// Form: 1 → 2 columns
<div className="grid grid-cols-1 gap-6 md:grid-cols-2">
```

### KHÔNG làm

```tsx
// ❌ Quá nhiều breakpoints
<div className="grid grid-cols-1 sm:grid-cols-2 md:grid-cols-3 lg:grid-cols-4 xl:grid-cols-5">

// ✅ Đơn giản, dùng md: là chính
<div className="grid grid-cols-1 gap-4 md:grid-cols-3">
```
