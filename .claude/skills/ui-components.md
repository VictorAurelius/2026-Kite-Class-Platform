# Skill: UI Components

Design system và component library cho KiteClass Frontend.

## Mô tả

Tài liệu về UI/UX:
- Design tokens (colors, typography, spacing)
- Component library (Shadcn/UI)
- Layout patterns
- Form conventions
- Responsive design

## Trigger phrases

- "ui components"
- "design system"
- "shadcn"
- "tailwind"
- "giao diện"

---

## Design Tokens

### Colors

```typescript
// tailwind.config.js
const colors = {
  // Brand colors
  primary: {
    50: '#f0f9ff',
    100: '#e0f2fe',
    200: '#bae6fd',
    300: '#7dd3fc',
    400: '#38bdf8',
    500: '#0ea5e9',  // Main primary
    600: '#0284c7',
    700: '#0369a1',
    800: '#075985',
    900: '#0c4a6e',
  },

  // Semantic colors
  success: {
    light: '#dcfce7',
    DEFAULT: '#22c55e',
    dark: '#15803d',
  },
  warning: {
    light: '#fef3c7',
    DEFAULT: '#f59e0b',
    dark: '#b45309',
  },
  error: {
    light: '#fee2e2',
    DEFAULT: '#ef4444',
    dark: '#b91c1c',
  },
  info: {
    light: '#dbeafe',
    DEFAULT: '#3b82f6',
    dark: '#1d4ed8',
  },

  // Neutral (Gray scale)
  gray: {
    50: '#f9fafb',
    100: '#f3f4f6',
    200: '#e5e7eb',
    300: '#d1d5db',
    400: '#9ca3af',
    500: '#6b7280',
    600: '#4b5563',
    700: '#374151',
    800: '#1f2937',
    900: '#111827',
  },
}
```

### Typography

```css
/* Font Family */
--font-sans: 'Inter', system-ui, sans-serif;
--font-mono: 'JetBrains Mono', monospace;

/* Font Sizes */
--text-xs: 0.75rem;     /* 12px */
--text-sm: 0.875rem;    /* 14px */
--text-base: 1rem;      /* 16px */
--text-lg: 1.125rem;    /* 18px */
--text-xl: 1.25rem;     /* 20px */
--text-2xl: 1.5rem;     /* 24px */
--text-3xl: 1.875rem;   /* 30px */

/* Font Weights */
--font-normal: 400;
--font-medium: 500;
--font-semibold: 600;
--font-bold: 700;

/* Line Heights */
--leading-tight: 1.25;
--leading-normal: 1.5;
--leading-relaxed: 1.75;
```

### Spacing

```css
/* Spacing Scale (4px base) */
--space-0: 0;
--space-1: 0.25rem;   /* 4px */
--space-2: 0.5rem;    /* 8px */
--space-3: 0.75rem;   /* 12px */
--space-4: 1rem;      /* 16px */
--space-5: 1.25rem;   /* 20px */
--space-6: 1.5rem;    /* 24px */
--space-8: 2rem;      /* 32px */
--space-10: 2.5rem;   /* 40px */
--space-12: 3rem;     /* 48px */
--space-16: 4rem;     /* 64px */
```

### Border Radius

```css
--radius-none: 0;
--radius-sm: 0.125rem;   /* 2px */
--radius-md: 0.375rem;   /* 6px */
--radius-lg: 0.5rem;     /* 8px */
--radius-xl: 0.75rem;    /* 12px */
--radius-2xl: 1rem;      /* 16px */
--radius-full: 9999px;
```

---

## Component Library (Shadcn/UI)

### Installation

```bash
# Init shadcn
pnpm dlx shadcn-ui@latest init

# Add components
pnpm dlx shadcn-ui@latest add button
pnpm dlx shadcn-ui@latest add input
pnpm dlx shadcn-ui@latest add table
pnpm dlx shadcn-ui@latest add dialog
pnpm dlx shadcn-ui@latest add dropdown-menu
pnpm dlx shadcn-ui@latest add select
pnpm dlx shadcn-ui@latest add form
pnpm dlx shadcn-ui@latest add toast
```

### Core Components

#### Button

```tsx
import { Button } from "@/components/ui/button"

// Variants
<Button>Default</Button>
<Button variant="secondary">Secondary</Button>
<Button variant="destructive">Delete</Button>
<Button variant="outline">Outline</Button>
<Button variant="ghost">Ghost</Button>
<Button variant="link">Link</Button>

// Sizes
<Button size="sm">Small</Button>
<Button size="default">Default</Button>
<Button size="lg">Large</Button>
<Button size="icon"><Icon /></Button>

// States
<Button disabled>Disabled</Button>
<Button loading>
  <Loader2 className="mr-2 h-4 w-4 animate-spin" />
  Loading
</Button>
```

#### Input

```tsx
import { Input } from "@/components/ui/input"
import { Label } from "@/components/ui/label"

<div className="space-y-2">
  <Label htmlFor="email">Email</Label>
  <Input
    id="email"
    type="email"
    placeholder="name@example.com"
  />
</div>

// With error
<div className="space-y-2">
  <Label htmlFor="email">Email</Label>
  <Input
    id="email"
    className="border-red-500"
    aria-invalid="true"
  />
  <p className="text-sm text-red-500">Email không hợp lệ</p>
</div>
```

#### Data Table

```tsx
import {
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from "@/components/ui/table"

<Table>
  <TableHeader>
    <TableRow>
      <TableHead>Tên</TableHead>
      <TableHead>Email</TableHead>
      <TableHead>Trạng thái</TableHead>
      <TableHead className="text-right">Actions</TableHead>
    </TableRow>
  </TableHeader>
  <TableBody>
    {students.map((student) => (
      <TableRow key={student.id}>
        <TableCell className="font-medium">{student.name}</TableCell>
        <TableCell>{student.email}</TableCell>
        <TableCell>
          <Badge variant={student.active ? "success" : "secondary"}>
            {student.active ? "Đang học" : "Nghỉ"}
          </Badge>
        </TableCell>
        <TableCell className="text-right">
          <DropdownMenu>
            <DropdownMenuTrigger asChild>
              <Button variant="ghost" size="icon">
                <MoreHorizontal className="h-4 w-4" />
              </Button>
            </DropdownMenuTrigger>
            <DropdownMenuContent align="end">
              <DropdownMenuItem>Xem chi tiết</DropdownMenuItem>
              <DropdownMenuItem>Chỉnh sửa</DropdownMenuItem>
              <DropdownMenuSeparator />
              <DropdownMenuItem className="text-red-600">
                Xóa
              </DropdownMenuItem>
            </DropdownMenuContent>
          </DropdownMenu>
        </TableCell>
      </TableRow>
    ))}
  </TableBody>
</Table>
```

#### Dialog/Modal

```tsx
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogFooter,
  DialogHeader,
  DialogTitle,
  DialogTrigger,
} from "@/components/ui/dialog"

<Dialog>
  <DialogTrigger asChild>
    <Button>Thêm học viên</Button>
  </DialogTrigger>
  <DialogContent className="sm:max-w-[425px]">
    <DialogHeader>
      <DialogTitle>Thêm học viên mới</DialogTitle>
      <DialogDescription>
        Nhập thông tin học viên. Nhấn Lưu khi hoàn tất.
      </DialogDescription>
    </DialogHeader>
    <div className="grid gap-4 py-4">
      {/* Form fields */}
    </div>
    <DialogFooter>
      <Button type="button" variant="outline">Hủy</Button>
      <Button type="submit">Lưu</Button>
    </DialogFooter>
  </DialogContent>
</Dialog>
```

---

## Custom Components

### PageHeader

```tsx
// components/shared/page-header.tsx
interface PageHeaderProps {
  title: string
  description?: string
  actions?: React.ReactNode
}

export function PageHeader({ title, description, actions }: PageHeaderProps) {
  return (
    <div className="flex items-center justify-between">
      <div>
        <h1 className="text-2xl font-bold tracking-tight">{title}</h1>
        {description && (
          <p className="text-muted-foreground">{description}</p>
        )}
      </div>
      {actions && <div className="flex gap-2">{actions}</div>}
    </div>
  )
}

// Usage
<PageHeader
  title="Quản lý học viên"
  description="Danh sách tất cả học viên trong hệ thống"
  actions={
    <>
      <Button variant="outline">Export</Button>
      <Button>Thêm học viên</Button>
    </>
  }
/>
```

### DataTable with Features

```tsx
// components/shared/data-table.tsx
interface DataTableProps<T> {
  columns: ColumnDef<T>[]
  data: T[]
  searchKey?: string
  searchPlaceholder?: string
  pagination?: boolean
  pageSize?: number
}

export function DataTable<T>({
  columns,
  data,
  searchKey,
  searchPlaceholder = "Tìm kiếm...",
  pagination = true,
  pageSize = 10,
}: DataTableProps<T>) {
  // Implementation with tanstack/react-table
}
```

### StatusBadge

```tsx
// components/shared/status-badge.tsx
const statusStyles = {
  active: "bg-green-100 text-green-800",
  inactive: "bg-gray-100 text-gray-800",
  pending: "bg-yellow-100 text-yellow-800",
  error: "bg-red-100 text-red-800",
}

interface StatusBadgeProps {
  status: keyof typeof statusStyles
  children: React.ReactNode
}

export function StatusBadge({ status, children }: StatusBadgeProps) {
  return (
    <span className={cn(
      "inline-flex items-center rounded-full px-2.5 py-0.5 text-xs font-medium",
      statusStyles[status]
    )}>
      {children}
    </span>
  )
}
```

---

## Layout Patterns

### Dashboard Layout

```tsx
// app/(dashboard)/layout.tsx
export default function DashboardLayout({
  children,
}: {
  children: React.ReactNode
}) {
  return (
    <div className="flex h-screen">
      {/* Sidebar */}
      <aside className="hidden md:flex md:w-64 md:flex-col">
        <Sidebar />
      </aside>

      {/* Main content */}
      <div className="flex flex-1 flex-col overflow-hidden">
        {/* Header */}
        <header className="h-16 border-b bg-white px-4 flex items-center justify-between">
          <MobileSidebarTrigger />
          <HeaderActions />
        </header>

        {/* Page content */}
        <main className="flex-1 overflow-y-auto p-6">
          {children}
        </main>
      </div>
    </div>
  )
}
```

### Page Layout

```tsx
// Typical page structure
export default function StudentsPage() {
  return (
    <div className="space-y-6">
      {/* Page header */}
      <PageHeader
        title="Học viên"
        actions={<Button>Thêm mới</Button>}
      />

      {/* Filters */}
      <div className="flex gap-4">
        <Input placeholder="Tìm kiếm..." className="max-w-sm" />
        <Select>
          <SelectTrigger className="w-[180px]">
            <SelectValue placeholder="Trạng thái" />
          </SelectTrigger>
          <SelectContent>
            <SelectItem value="all">Tất cả</SelectItem>
            <SelectItem value="active">Đang học</SelectItem>
            <SelectItem value="inactive">Nghỉ</SelectItem>
          </SelectContent>
        </Select>
      </div>

      {/* Content */}
      <Card>
        <CardContent className="p-0">
          <DataTable columns={columns} data={students} />
        </CardContent>
      </Card>
    </div>
  )
}
```

---

## Form Conventions

### React Hook Form + Zod

```tsx
import { useForm } from "react-hook-form"
import { zodResolver } from "@hookform/resolvers/zod"
import * as z from "zod"

const studentSchema = z.object({
  name: z.string().min(2, "Tên phải có ít nhất 2 ký tự"),
  email: z.string().email("Email không hợp lệ"),
  phone: z.string().regex(/^0\d{9}$/, "Số điện thoại không hợp lệ"),
  dateOfBirth: z.date().optional(),
})

type StudentFormValues = z.infer<typeof studentSchema>

export function StudentForm() {
  const form = useForm<StudentFormValues>({
    resolver: zodResolver(studentSchema),
    defaultValues: {
      name: "",
      email: "",
      phone: "",
    },
  })

  const onSubmit = async (data: StudentFormValues) => {
    // Handle submit
  }

  return (
    <Form {...form}>
      <form onSubmit={form.handleSubmit(onSubmit)} className="space-y-4">
        <FormField
          control={form.control}
          name="name"
          render={({ field }) => (
            <FormItem>
              <FormLabel>Họ và tên</FormLabel>
              <FormControl>
                <Input placeholder="Nguyễn Văn A" {...field} />
              </FormControl>
              <FormMessage />
            </FormItem>
          )}
        />

        <FormField
          control={form.control}
          name="email"
          render={({ field }) => (
            <FormItem>
              <FormLabel>Email</FormLabel>
              <FormControl>
                <Input type="email" {...field} />
              </FormControl>
              <FormMessage />
            </FormItem>
          )}
        />

        <Button type="submit" disabled={form.formState.isSubmitting}>
          {form.formState.isSubmitting ? "Đang lưu..." : "Lưu"}
        </Button>
      </form>
    </Form>
  )
}
```

---

## Responsive Design

### Breakpoints

```css
/* Tailwind default breakpoints */
sm: 640px   /* Mobile landscape */
md: 768px   /* Tablet */
lg: 1024px  /* Desktop */
xl: 1280px  /* Large desktop */
2xl: 1536px /* Extra large */
```

### Responsive Patterns

```tsx
{/* Hide on mobile, show on desktop */}
<div className="hidden md:block">Desktop content</div>

{/* Show on mobile, hide on desktop */}
<div className="md:hidden">Mobile content</div>

{/* Grid responsive */}
<div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4">
  {items.map(item => <Card key={item.id} />)}
</div>

{/* Responsive padding */}
<div className="px-4 md:px-6 lg:px-8">Content</div>

{/* Responsive text */}
<h1 className="text-xl md:text-2xl lg:text-3xl">Title</h1>
```

---

## Icons

### Lucide React

```tsx
import {
  Home,
  Users,
  GraduationCap,
  Calendar,
  CreditCard,
  Settings,
  ChevronDown,
  Search,
  Plus,
  Trash2,
  Edit,
  MoreHorizontal,
} from "lucide-react"

// Usage
<Button>
  <Plus className="mr-2 h-4 w-4" />
  Thêm mới
</Button>
```

### Icon Sizes

| Size | Class | Usage |
|------|-------|-------|
| xs | `h-3 w-3` | Inline icons |
| sm | `h-4 w-4` | Buttons, inputs |
| md | `h-5 w-5` | Navigation |
| lg | `h-6 w-6` | Headers |
| xl | `h-8 w-8` | Empty states |

## Actions

### Thêm component mới
```bash
pnpm dlx shadcn-ui@latest add [component-name]
```

### Xem tất cả components
https://ui.shadcn.com/docs/components
