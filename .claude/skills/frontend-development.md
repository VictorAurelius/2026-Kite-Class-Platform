# Skill: Frontend Development

Comprehensive guide for frontend development on KiteClass Platform.

**Version:** 2.0
**Date:** 2026-01-27
**Merged from:** ui-components.md, theme-system.md, i18n-messages.md

## Mô tả

Tài liệu tổng hợp về Frontend Development:
- Design System & UI Components (Shadcn/UI, Tailwind)
- Theme System & Customization (Hybrid model, Branding)
- Internationalization & Messages (i18n patterns)
- Layout patterns, Form conventions, Responsive design

## Trigger phrases

- "frontend"
- "ui components"
- "design system"
- "shadcn"
- "tailwind"
- "theme system"
- "giao diện"
- "branding"
- "customization"
- "i18n"
- "internationalization"
- "đa ngữ"

---

# Part 1: Design System & UI Components

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

---

# Part 2: Theme System & Customization

## Theme Architecture

### Hybrid Model

```
┌─────────────────────────────────────────────────────────────────────────────────┐
│                           THEME SYSTEM ARCHITECTURE                             │
├─────────────────────────────────────────────────────────────────────────────────┤
│                                                                                 │
│  ┌─────────────────────────────────────────────────────────────────────────┐   │
│  │                              KITEHUB                                     │   │
│  │                                                                          │   │
│  │   ┌──────────────────┐    ┌──────────────────┐    ┌────────────────┐   │   │
│  │   │ Theme Marketplace │    │ Instance Manager │    │ Theme Preview  │   │   │
│  │   │                   │    │                  │    │                │   │   │
│  │   │ • Free Themes     │    │ • Assign Theme   │    │ • Live Preview │   │   │
│  │   │ • Pro Themes      │    │ • Custom CSS     │    │ • Before/After │   │   │
│  │   │ • Custom Themes   │    │ • Bulk Update    │    │                │   │   │
│  │   └──────────────────┘    └──────────────────┘    └────────────────┘   │   │
│  │                                     │                                    │   │
│  └─────────────────────────────────────┼────────────────────────────────────┘   │
│                                        │                                        │
│                                        │ Theme Config Sync                      │
│                                        ▼                                        │
│  ┌─────────────────────────────────────────────────────────────────────────┐   │
│  │                         KITECLASS INSTANCE                               │   │
│  │                                                                          │   │
│  │   ┌──────────────────┐    ┌──────────────────┐    ┌────────────────┐   │   │
│  │   │ Theme Engine     │    │ Branding Settings│    │ User Prefs     │   │   │
│  │   │                  │    │                  │    │                │   │   │
│  │   │ • Apply Template │    │ • Logo           │    │ • Dark Mode    │   │   │
│  │   │ • CSS Variables  │    │ • Primary Color  │    │ • Font Size    │   │   │
│  │   │ • Component Theme│    │ • Display Name   │    │ • Compact View │   │   │
│  │   └──────────────────┘    └──────────────────┘    └────────────────┘   │   │
│  │                                                                          │   │
│  └──────────────────────────────────────────────────────────────────────────┘   │
│                                                                                 │
└─────────────────────────────────────────────────────────────────────────────────┘
```

### Phân quyền quản lý

| Cấu hình | Quản lý tại | Ai có quyền | Tần suất thay đổi |
|----------|-------------|-------------|-------------------|
| Theme Template | KiteHub | Customer (Owner) | Hiếm khi |
| Custom CSS | KiteHub | Customer (Enterprise) | Hiếm khi |
| Logo, Favicon | Instance | Center Admin | Thỉnh thoảng |
| Primary Color | Instance | Center Admin | Thỉnh thoảng |
| Display Name | Instance | Center Admin | Hiếm khi |
| Dark/Light Mode | Instance | End User | Thường xuyên |

---

## Theme Templates

### Available Templates

```
┌─────────────────────────────────────────────────────────────────────────────────┐
│                           THEME TEMPLATE CATALOG                                │
├─────────────────────────────────────────────────────────────────────────────────┤
│                                                                                 │
│  FREE THEMES                                                                    │
│  ════════════════════════════════════════════════════════════════════════════  │
│                                                                                 │
│  ┌─────────────────┐  ┌─────────────────┐  ┌─────────────────┐                │
│  │    CLASSIC      │  │    MODERN       │  │   FRIENDLY      │                │
│  │   ┌─────────┐   │  │   ┌─────────┐   │  │   ┌─────────┐   │                │
│  │   │ ══════  │   │  │   │ ══════  │   │  │   │ 🎨═════ │   │                │
│  │   │ ┌─┐ ┌─┐ │   │  │   │         │   │  │   │ ┌─┐ ┌─┐ │   │                │
│  │   │ │ │ │ │ │   │  │   │ ┌─────┐ │   │  │   │ │●│ │●│ │   │                │
│  │   │ └─┘ └─┘ │   │  │   │ │     │ │   │  │   │ └─┘ └─┘ │   │                │
│  │   └─────────┘   │  │   └─────────┘   │  │   └─────────┘   │                │
│  │                 │  │                 │  │                 │                │
│  │ Truyền thống    │  │ Tối giản       │  │ Nhiều màu sắc   │                │
│  │ Chuyên nghiệp   │  │ Hiện đại       │  │ Thân thiện      │                │
│  └─────────────────┘  └─────────────────┘  └─────────────────┘                │
│                                                                                 │
│  PRO THEMES (Gói Pro+)                                                         │
│  ════════════════════════════════════════════════════════════════════════════  │
│                                                                                 │
│  ┌─────────────────┐  ┌─────────────────┐  ┌─────────────────┐                │
│  │    PLAYFUL      │  │    PREMIUM      │  │   DARK PRO      │                │
│  │   ┌─────────┐   │  │   ┌─────────┐   │  │   ┌─────────┐   │                │
│  │   │ 🌈✨🎮  │   │  │   │ ▓▓▓▓▓▓▓ │   │  │   │ ▒▒▒▒▒▒▒ │   │                │
│  │   │ ┌─┐ ┌─┐ │   │  │   │ ┌─────┐ │   │  │   │ ┌─────┐ │   │                │
│  │   │ │☺│ │☺│ │   │  │   │ │ ◆◆◆ │ │   │  │   │ │ ░░░ │ │   │                │
│  │   │ └─┘ └─┘ │   │  │   │ └─────┘ │   │  │   │ └─────┘ │   │                │
│  │   └─────────┘   │  │   └─────────┘   │  │   └─────────┘   │                │
│  │                 │  │                 │  │                 │                │
│  │ Animation       │  │ Sang trọng      │  │ Dark by default │                │
│  │ Gamification    │  │ IELTS/Business  │  │ Easy on eyes    │                │
│  └─────────────────┘  └─────────────────┘  └─────────────────┘                │
│                                                                                 │
│  ENTERPRISE ONLY                                                                │
│  ════════════════════════════════════════════════════════════════════════════  │
│                                                                                 │
│  ┌─────────────────┐  ┌─────────────────┐                                      │
│  │  CUSTOM THEME   │  │  WHITE LABEL    │                                      │
│  │   ┌─────────┐   │  │   ┌─────────┐   │                                      │
│  │   │ YOUR    │   │  │   │ YOUR    │   │                                      │
│  │   │ BRAND   │   │  │   │ BRAND   │   │                                      │
│  │   │ HERE    │   │  │   │ 100%    │   │                                      │
│  │   └─────────┘   │  │   └─────────┘   │                                      │
│  │                 │  │                 │                                      │
│  │ Full CSS access │  │ No KiteClass   │                                      │
│  │ Custom design   │  │ branding       │                                      │
│  └─────────────────┘  └─────────────────┘                                      │
│                                                                                 │
└─────────────────────────────────────────────────────────────────────────────────┘
```

### Template Specifications

| Template | Primary Color | Style | Target Audience |
|----------|---------------|-------|-----------------|
| **Classic** | `#1e40af` (Blue) | Professional, Traditional | Trung tâm luyện thi, ôn tập |
| **Modern** | `#0ea5e9` (Sky) | Minimal, Clean | Trung tâm ngoại ngữ |
| **Friendly** | `#22c55e` (Green) | Colorful, Warm | Trung tâm đa lĩnh vực |
| **Playful** | `#f59e0b` (Amber) | Fun, Animated | Trung tâm trẻ em, mầm non |
| **Premium** | `#7c3aed` (Violet) | Luxurious, Elegant | IELTS, Business English |
| **Dark Pro** | `#6366f1` (Indigo) | Dark, Modern | Tech-savvy centers |

---

## Data Models

### Database Schema

```sql
-- KiteHub Database
-- ================

-- Theme templates (managed by KiteHub Admin)
CREATE TABLE themes.templates (
    id VARCHAR(50) PRIMARY KEY,           -- 'classic', 'modern', etc.
    name VARCHAR(100) NOT NULL,
    description TEXT,
    thumbnail_url VARCHAR(500),
    preview_url VARCHAR(500),

    -- Pricing
    tier VARCHAR(20) NOT NULL,            -- 'free', 'pro', 'enterprise'

    -- Theme configuration
    config JSONB NOT NULL,                -- Full theme config

    -- Metadata
    version VARCHAR(20) NOT NULL,
    is_active BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);

-- Instance theme assignments
CREATE TABLE themes.instance_themes (
    instance_id BIGINT PRIMARY KEY REFERENCES maintaining.instances(id),
    template_id VARCHAR(50) REFERENCES themes.templates(id),
    custom_css TEXT,                      -- Enterprise only
    assigned_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    assigned_by BIGINT REFERENCES sales.customers(id)
);

-- KiteClass Instance Database
-- ============================

-- Branding settings (managed by Center Admin)
CREATE TABLE settings.branding (
    id BIGSERIAL PRIMARY KEY,

    -- Visual branding
    logo_url VARCHAR(500),
    favicon_url VARCHAR(500),
    display_name VARCHAR(200),
    tagline VARCHAR(500),

    -- Colors (override template defaults)
    primary_color VARCHAR(7),             -- Hex: #0ea5e9
    secondary_color VARCHAR(7),

    -- Contact info shown in footer
    contact_email VARCHAR(255),
    contact_phone VARCHAR(20),
    address TEXT,

    -- Social links
    facebook_url VARCHAR(500),
    zalo_url VARCHAR(500),

    -- Metadata
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    updated_by BIGINT REFERENCES users(id)
);

-- User preferences (per user)
CREATE TABLE settings.user_preferences (
    user_id BIGINT PRIMARY KEY REFERENCES users(id),

    -- Theme preferences
    color_mode VARCHAR(10) DEFAULT 'system',  -- 'light', 'dark', 'system'
    font_size VARCHAR(10) DEFAULT 'medium',   -- 'small', 'medium', 'large'
    compact_mode BOOLEAN DEFAULT FALSE,

    -- Notification preferences
    email_notifications BOOLEAN DEFAULT TRUE,
    push_notifications BOOLEAN DEFAULT TRUE,

    updated_at TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);
```

### TypeScript Types

```typescript
// Theme Template (from KiteHub)
interface ThemeTemplate {
  id: string;
  name: string;
  description: string;
  thumbnailUrl: string;
  previewUrl: string;
  tier: 'free' | 'pro' | 'enterprise';
  version: string;
  config: ThemeConfig;
}

// Theme Configuration
interface ThemeConfig {
  // Colors
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

  // Typography
  typography: {
    fontFamily: {
      sans: string;
      mono: string;
    };
    fontSize: Record<string, string>;
    fontWeight: Record<string, number>;
    lineHeight: Record<string, number>;
  };

  // Spacing & Layout
  spacing: Record<string, string>;
  borderRadius: Record<string, string>;

  // Shadows
  shadows: Record<string, string>;

  // Component-specific
  components: {
    button: ComponentTheme;
    input: ComponentTheme;
    card: ComponentTheme;
    sidebar: ComponentTheme;
    // ...
  };
}

interface ColorScale {
  50: string;
  100: string;
  200: string;
  300: string;
  400: string;
  500: string;  // Default
  600: string;
  700: string;
  800: string;
  900: string;
}

// Branding Settings (Instance level)
interface BrandingSettings {
  logoUrl: string | null;
  faviconUrl: string | null;
  displayName: string;
  tagline: string | null;
  primaryColor: string | null;  // Override template
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
interface UserPreferences {
  colorMode: 'light' | 'dark' | 'system';
  fontSize: 'small' | 'medium' | 'large';
  compactMode: boolean;
  emailNotifications: boolean;
  pushNotifications: boolean;
}

// Resolved Theme (combined)
interface ResolvedTheme {
  template: ThemeTemplate;
  branding: BrandingSettings;
  userPrefs: UserPreferences;

  // Computed
  effectiveColorMode: 'light' | 'dark';
  effectivePrimaryColor: string;
}
```

---

## API Endpoints

### KiteHub APIs

```
# Theme Templates
GET    /api/v1/themes/templates              # List available templates
GET    /api/v1/themes/templates/{id}         # Get template details
GET    /api/v1/themes/templates/{id}/preview # Get preview URL

# Instance Theme Management
GET    /api/v1/instances/{id}/theme          # Get instance theme
PUT    /api/v1/instances/{id}/theme          # Update instance theme
POST   /api/v1/instances/{id}/theme/preview  # Generate preview
```

### KiteClass Instance APIs

```
# Branding (Center Admin)
GET    /api/v1/settings/branding             # Get branding settings
PUT    /api/v1/settings/branding             # Update branding
POST   /api/v1/settings/branding/logo        # Upload logo
POST   /api/v1/settings/branding/favicon     # Upload favicon

# User Preferences (Any user)
GET    /api/v1/users/me/preferences          # Get my preferences
PUT    /api/v1/users/me/preferences          # Update my preferences

# Theme (Public)
GET    /api/v1/theme                         # Get resolved theme for current instance
```

---

## Frontend Implementation

### Theme Provider

```typescript
// providers/theme-provider.tsx
'use client';

import { createContext, useContext, useEffect, useState } from 'react';
import { useQuery } from '@tanstack/react-query';

interface ThemeContextType {
  theme: ResolvedTheme | null;
  colorMode: 'light' | 'dark';
  setColorMode: (mode: 'light' | 'dark' | 'system') => void;
  isLoading: boolean;
}

const ThemeContext = createContext<ThemeContextType | null>(null);

export function ThemeProvider({ children }: { children: React.ReactNode }) {
  const [colorMode, setColorModeState] = useState<'light' | 'dark'>('light');

  // Fetch theme from API
  const { data: theme, isLoading } = useQuery({
    queryKey: ['theme'],
    queryFn: () => api.get<ResolvedTheme>('/theme'),
    staleTime: 1000 * 60 * 60, // 1 hour
  });

  // Apply CSS variables when theme changes
  useEffect(() => {
    if (theme) {
      applyThemeVariables(theme);
    }
  }, [theme]);

  // Handle color mode
  useEffect(() => {
    const root = document.documentElement;
    root.classList.remove('light', 'dark');
    root.classList.add(colorMode);
  }, [colorMode]);

  const setColorMode = (mode: 'light' | 'dark' | 'system') => {
    if (mode === 'system') {
      const systemMode = window.matchMedia('(prefers-color-scheme: dark)').matches
        ? 'dark'
        : 'light';
      setColorModeState(systemMode);
    } else {
      setColorModeState(mode);
    }

    // Save to user preferences
    api.put('/users/me/preferences', { colorMode: mode });
  };

  return (
    <ThemeContext.Provider value={{ theme, colorMode, setColorMode, isLoading }}>
      {children}
    </ThemeContext.Provider>
  );
}

export const useTheme = () => {
  const context = useContext(ThemeContext);
  if (!context) throw new Error('useTheme must be used within ThemeProvider');
  return context;
};
```

### Apply CSS Variables

```typescript
// lib/theme-utils.ts
export function applyThemeVariables(theme: ResolvedTheme) {
  const root = document.documentElement;
  const { template, branding } = theme;

  // Apply colors
  const primaryColor = branding.primaryColor || template.config.colors.primary[500];
  root.style.setProperty('--color-primary', primaryColor);

  // Generate color scale from primary
  const primaryScale = generateColorScale(primaryColor);
  Object.entries(primaryScale).forEach(([key, value]) => {
    root.style.setProperty(`--color-primary-${key}`, value);
  });

  // Apply other template colors
  const { colors } = template.config;
  Object.entries(colors.neutral).forEach(([key, value]) => {
    root.style.setProperty(`--color-neutral-${key}`, value);
  });

  // Apply typography
  const { typography } = template.config;
  root.style.setProperty('--font-sans', typography.fontFamily.sans);
  root.style.setProperty('--font-mono', typography.fontFamily.mono);

  // Apply border radius
  Object.entries(template.config.borderRadius).forEach(([key, value]) => {
    root.style.setProperty(`--radius-${key}`, value);
  });
}

// Generate color scale from a single color
function generateColorScale(baseColor: string): Record<string, string> {
  // Use chroma.js or similar library
  return {
    50: lighten(baseColor, 0.95),
    100: lighten(baseColor, 0.9),
    200: lighten(baseColor, 0.75),
    300: lighten(baseColor, 0.5),
    400: lighten(baseColor, 0.25),
    500: baseColor,
    600: darken(baseColor, 0.1),
    700: darken(baseColor, 0.25),
    800: darken(baseColor, 0.4),
    900: darken(baseColor, 0.5),
  };
}
```

### Tailwind Configuration

```javascript
// tailwind.config.js
module.exports = {
  darkMode: 'class',
  theme: {
    extend: {
      colors: {
        // Use CSS variables for theming
        primary: {
          50: 'var(--color-primary-50)',
          100: 'var(--color-primary-100)',
          200: 'var(--color-primary-200)',
          300: 'var(--color-primary-300)',
          400: 'var(--color-primary-400)',
          500: 'var(--color-primary-500)',
          600: 'var(--color-primary-600)',
          700: 'var(--color-primary-700)',
          800: 'var(--color-primary-800)',
          900: 'var(--color-primary-900)',
          DEFAULT: 'var(--color-primary)',
        },
        // ... other colors
      },
      fontFamily: {
        sans: ['var(--font-sans)', 'system-ui', 'sans-serif'],
        mono: ['var(--font-mono)', 'monospace'],
      },
      borderRadius: {
        sm: 'var(--radius-sm)',
        md: 'var(--radius-md)',
        lg: 'var(--radius-lg)',
        xl: 'var(--radius-xl)',
      },
    },
  },
};
```

---

## Branding Settings UI

### Instance Admin Settings Page

```typescript
// app/(dashboard)/settings/branding/page.tsx
'use client';

import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { useBranding, useUpdateBranding } from '@/hooks/use-branding';

const brandingSchema = z.object({
  displayName: z.string().min(2).max(200),
  tagline: z.string().max(500).optional(),
  primaryColor: z.string().regex(/^#[0-9A-Fa-f]{6}$/).optional(),
  contactEmail: z.string().email().optional(),
  contactPhone: z.string().optional(),
  address: z.string().optional(),
});

export default function BrandingSettingsPage() {
  const { data: branding, isLoading } = useBranding();
  const { mutate: updateBranding, isPending } = useUpdateBranding();

  const form = useForm({
    resolver: zodResolver(brandingSchema),
    defaultValues: branding,
  });

  if (isLoading) return <LoadingSpinner />;

  return (
    <div className="space-y-6">
      <PageHeader
        title="Cài đặt thương hiệu"
        description="Tùy chỉnh giao diện và thông tin hiển thị của trung tâm"
      />

      <Form {...form}>
        <form onSubmit={form.handleSubmit(updateBranding)} className="space-y-8">

          {/* Logo Upload */}
          <Card>
            <CardHeader>
              <CardTitle>Logo & Favicon</CardTitle>
            </CardHeader>
            <CardContent className="space-y-4">
              <div className="flex gap-8">
                <LogoUpload
                  currentUrl={branding?.logoUrl}
                  onUpload={(url) => form.setValue('logoUrl', url)}
                />
                <FaviconUpload
                  currentUrl={branding?.faviconUrl}
                  onUpload={(url) => form.setValue('faviconUrl', url)}
                />
              </div>
            </CardContent>
          </Card>

          {/* Basic Info */}
          <Card>
            <CardHeader>
              <CardTitle>Thông tin cơ bản</CardTitle>
            </CardHeader>
            <CardContent className="space-y-4">
              <FormField
                control={form.control}
                name="displayName"
                render={({ field }) => (
                  <FormItem>
                    <FormLabel>Tên hiển thị</FormLabel>
                    <FormControl>
                      <Input placeholder="Trung tâm Anh ngữ ABC" {...field} />
                    </FormControl>
                    <FormMessage />
                  </FormItem>
                )}
              />

              <FormField
                control={form.control}
                name="tagline"
                render={({ field }) => (
                  <FormItem>
                    <FormLabel>Slogan</FormLabel>
                    <FormControl>
                      <Input placeholder="Nơi ươm mầm tài năng" {...field} />
                    </FormControl>
                    <FormMessage />
                  </FormItem>
                )}
              />
            </CardContent>
          </Card>

          {/* Colors */}
          <Card>
            <CardHeader>
              <CardTitle>Màu sắc</CardTitle>
              <CardDescription>
                Tùy chỉnh màu chủ đạo của giao diện
              </CardDescription>
            </CardHeader>
            <CardContent>
              <FormField
                control={form.control}
                name="primaryColor"
                render={({ field }) => (
                  <FormItem>
                    <FormLabel>Màu chủ đạo</FormLabel>
                    <FormControl>
                      <div className="flex items-center gap-3">
                        <ColorPicker
                          value={field.value}
                          onChange={field.onChange}
                        />
                        <Input
                          {...field}
                          placeholder="#0ea5e9"
                          className="w-32 font-mono"
                        />
                      </div>
                    </FormControl>
                    <FormDescription>
                      Màu này sẽ được áp dụng cho buttons, links và các thành phần chính
                    </FormDescription>
                    <FormMessage />
                  </FormItem>
                )}
              />

              {/* Color Preview */}
              <div className="mt-4 p-4 border rounded-lg">
                <p className="text-sm text-muted-foreground mb-2">Xem trước:</p>
                <div className="flex gap-2">
                  <Button style={{ backgroundColor: form.watch('primaryColor') }}>
                    Button Primary
                  </Button>
                  <Button variant="outline" style={{
                    borderColor: form.watch('primaryColor'),
                    color: form.watch('primaryColor')
                  }}>
                    Button Outline
                  </Button>
                </div>
              </div>
            </CardContent>
          </Card>

          {/* Contact Info */}
          <Card>
            <CardHeader>
              <CardTitle>Thông tin liên hệ</CardTitle>
              <CardDescription>
                Hiển thị ở footer và trang liên hệ
              </CardDescription>
            </CardHeader>
            <CardContent className="space-y-4">
              {/* Email, Phone, Address fields */}
            </CardContent>
          </Card>

          {/* Theme Info (Read-only) */}
          <Card>
            <CardHeader>
              <CardTitle>Theme Template</CardTitle>
              <CardDescription>
                Được quản lý trên KiteHub
              </CardDescription>
            </CardHeader>
            <CardContent>
              <div className="flex items-center gap-4">
                <img
                  src={branding?.template?.thumbnailUrl}
                  alt={branding?.template?.name}
                  className="w-24 h-16 object-cover rounded"
                />
                <div>
                  <p className="font-medium">{branding?.template?.name}</p>
                  <p className="text-sm text-muted-foreground">
                    {branding?.template?.description}
                  </p>
                </div>
              </div>
              <p className="mt-4 text-sm text-muted-foreground">
                <InfoIcon className="inline h-4 w-4 mr-1" />
                Để thay đổi theme, vui lòng truy cập{' '}
                <a href="https://kitehub.vn/settings" className="text-primary underline">
                  KiteHub
                </a>
              </p>
            </CardContent>
          </Card>

          {/* Submit */}
          <div className="flex justify-end">
            <Button type="submit" disabled={isPending}>
              {isPending ? 'Đang lưu...' : 'Lưu thay đổi'}
            </Button>
          </div>
        </form>
      </Form>
    </div>
  );
}
```

---

## Business Rules

### Theme Access by Plan

| Feature | Basic | Pro | Enterprise |
|---------|:-----:|:---:|:----------:|
| Free Themes | ✅ | ✅ | ✅ |
| Pro Themes | ❌ | ✅ | ✅ |
| Custom Primary Color | ✅ | ✅ | ✅ |
| Custom Logo/Favicon | ✅ | ✅ | ✅ |
| Custom CSS | ❌ | ❌ | ✅ |
| White Label | ❌ | ❌ | ✅ |
| Custom Theme Design | ❌ | ❌ | ✅ (Add-on) |

### Theme Change Rules

1. **Downgrade**: Nếu downgrade từ Pro → Basic, theme Pro sẽ chuyển về Classic (Free)
2. **Upgrade**: Khi upgrade, có thể chọn theme mới ngay lập tức
3. **Preview**: Luôn cho phép preview trước khi apply
4. **Rollback**: Lưu theme trước đó để rollback nếu cần

---

# Part 3: Internationalization & Messages

## Nguyên tắc bắt buộc

1. **KHÔNG hardcode messages trong Java code**
2. **Sử dụng message codes** (VD: `error.user.not_found`)
3. **Messages được định nghĩa trong** `messages.properties`
4. **Hỗ trợ parameters** cho dynamic values

---

## Cấu trúc Message Codes

### Format

```
{category}.{entity}.{action}
```

### Categories

| Category | Mô tả | Ví dụ |
|----------|-------|-------|
| `error` | Lỗi | `error.user.not_found` |
| `validation` | Validation | `validation.email.invalid` |
| `success` | Thành công | `success.user.created` |
| `info` | Thông tin | `info.payment.pending` |

### Ví dụ Message Codes

```properties
# Error messages
error.entity.not_found={0} với ID {1} không tồn tại
error.entity.duplicate={0} đã tồn tại
error.internal=Đã xảy ra lỗi hệ thống. Vui lòng thử lại sau.
error.unauthorized=Bạn không có quyền truy cập
error.forbidden=Truy cập bị từ chối

# Auth errors
error.auth.invalid_credentials=Email hoặc mật khẩu không đúng
error.auth.account_locked=Tài khoản đã bị khóa
error.auth.account_inactive=Tài khoản chưa được kích hoạt
error.auth.token_expired=Phiên đăng nhập đã hết hạn
error.auth.token_invalid=Token không hợp lệ

# Validation messages
validation.required={0} là bắt buộc
validation.min_length={0} phải có ít nhất {1} ký tự
validation.max_length={0} không được vượt quá {1} ký tự
validation.email.invalid=Email không hợp lệ
validation.phone.invalid=Số điện thoại không hợp lệ (phải có 10 số, bắt đầu bằng 0)
validation.date.invalid=Ngày không hợp lệ
validation.data.invalid=Dữ liệu không hợp lệ

# Success messages
success.user.created=Tạo người dùng thành công
success.user.updated=Cập nhật thành công
success.user.deleted=Xóa thành công
success.password.changed=Đổi mật khẩu thành công
success.password.reset_sent=Nếu email tồn tại, bạn sẽ nhận được hướng dẫn đặt lại mật khẩu
```

---

## File Structure

```
src/main/resources/
├── messages.properties          # Default (Vietnamese)
├── messages_en.properties       # English
└── messages_vi.properties       # Vietnamese (explicit)
```

---

## Spring Configuration

### MessageSource Bean

```java
package com.kiteclass.gateway.config;

import org.springframework.context.MessageSource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.support.ReloadableResourceBundleMessageSource;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;

@Configuration
public class MessageConfig {

    @Bean
    public MessageSource messageSource() {
        ReloadableResourceBundleMessageSource messageSource =
            new ReloadableResourceBundleMessageSource();
        messageSource.setBasename("classpath:messages");
        messageSource.setDefaultEncoding("UTF-8");
        messageSource.setCacheSeconds(3600); // Reload every hour in dev
        return messageSource;
    }

    @Bean
    public LocalValidatorFactoryBean validator(MessageSource messageSource) {
        LocalValidatorFactoryBean bean = new LocalValidatorFactoryBean();
        bean.setValidationMessageSource(messageSource);
        return bean;
    }
}
```

---

## MessageService

```java
package com.kiteclass.gateway.common.service;

import lombok.RequiredArgsConstructor;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.stereotype.Service;

import java.util.Locale;

/**
 * Service for retrieving i18n messages.
 */
@Service
@RequiredArgsConstructor
public class MessageService {

    private final MessageSource messageSource;

    /**
     * Get message by code with current locale.
     */
    public String getMessage(String code) {
        return messageSource.getMessage(code, null, LocaleContextHolder.getLocale());
    }

    /**
     * Get message by code with parameters.
     */
    public String getMessage(String code, Object... args) {
        return messageSource.getMessage(code, args, LocaleContextHolder.getLocale());
    }

    /**
     * Get message with specific locale.
     */
    public String getMessage(String code, Locale locale, Object... args) {
        return messageSource.getMessage(code, args, locale);
    }
}
```

---

## Message Codes Class

```java
package com.kiteclass.gateway.common.constant;

/**
 * Centralized message codes for i18n.
 *
 * <p>IMPORTANT: All messages must be defined in messages.properties
 */
public final class MessageCodes {

    private MessageCodes() {}

    // Error codes
    public static final String ENTITY_NOT_FOUND = "error.entity.not_found";
    public static final String ENTITY_DUPLICATE = "error.entity.duplicate";
    public static final String INTERNAL_ERROR = "error.internal";
    public static final String UNAUTHORIZED = "error.unauthorized";
    public static final String FORBIDDEN = "error.forbidden";

    // Auth codes
    public static final String AUTH_INVALID_CREDENTIALS = "error.auth.invalid_credentials";
    public static final String AUTH_ACCOUNT_LOCKED = "error.auth.account_locked";
    public static final String AUTH_ACCOUNT_INACTIVE = "error.auth.account_inactive";
    public static final String AUTH_TOKEN_EXPIRED = "error.auth.token_expired";
    public static final String AUTH_TOKEN_INVALID = "error.auth.token_invalid";

    // Validation codes
    public static final String VALIDATION_REQUIRED = "validation.required";
    public static final String VALIDATION_MIN_LENGTH = "validation.min_length";
    public static final String VALIDATION_MAX_LENGTH = "validation.max_length";
    public static final String VALIDATION_EMAIL_INVALID = "validation.email.invalid";
    public static final String VALIDATION_PHONE_INVALID = "validation.phone.invalid";
    public static final String VALIDATION_DATA_INVALID = "validation.data.invalid";

    // Success codes
    public static final String SUCCESS_CREATED = "success.user.created";
    public static final String SUCCESS_UPDATED = "success.user.updated";
    public static final String SUCCESS_DELETED = "success.user.deleted";
}
```

---

## Updated Exception Classes

### BusinessException

```java
@Getter
public class BusinessException extends RuntimeException {

    private final String code;          // Message code (e.g., "error.auth.invalid_credentials")
    private final HttpStatus status;
    private final Object[] args;        // Arguments for message formatting

    public BusinessException(String code, HttpStatus status) {
        super(code);
        this.code = code;
        this.status = status;
        this.args = null;
    }

    public BusinessException(String code, HttpStatus status, Object... args) {
        super(code);
        this.code = code;
        this.status = status;
        this.args = args;
    }
}
```

### EntityNotFoundException

```java
public class EntityNotFoundException extends BusinessException {

    public EntityNotFoundException(String entityName, Long id) {
        super(MessageCodes.ENTITY_NOT_FOUND, HttpStatus.NOT_FOUND, entityName, id);
    }

    public EntityNotFoundException(String entityName, String field, String value) {
        super(MessageCodes.ENTITY_NOT_FOUND, HttpStatus.NOT_FOUND, entityName, value);
    }
}
```

---

## Updated GlobalExceptionHandler

```java
@Slf4j
@RestControllerAdvice
@RequiredArgsConstructor
public class GlobalExceptionHandler {

    private final MessageService messageService;

    @ExceptionHandler(BusinessException.class)
    public Mono<ResponseEntity<ErrorResponse>> handleBusinessException(
            BusinessException ex,
            ServerWebExchange exchange) {

        // Resolve message from code
        String message = ex.getArgs() != null
            ? messageService.getMessage(ex.getCode(), ex.getArgs())
            : messageService.getMessage(ex.getCode());

        String path = exchange.getRequest().getPath().value();
        ErrorResponse response = ErrorResponse.of(ex.getCode(), message, path);

        return Mono.just(ResponseEntity.status(ex.getStatus()).body(response));
    }

    @ExceptionHandler(WebExchangeBindException.class)
    public Mono<ResponseEntity<ErrorResponse>> handleValidationException(
            WebExchangeBindException ex,
            ServerWebExchange exchange) {

        Map<String, List<String>> fieldErrors = new HashMap<>();
        for (FieldError error : ex.getFieldErrors()) {
            fieldErrors.computeIfAbsent(error.getField(), k -> new ArrayList<>())
                    .add(error.getDefaultMessage()); // Already resolved by validator
        }

        String path = exchange.getRequest().getPath().value();
        String message = messageService.getMessage(MessageCodes.VALIDATION_DATA_INVALID);
        ErrorResponse response = ErrorResponse.withFieldErrors(
                MessageCodes.VALIDATION_DATA_INVALID,
                message,
                path,
                fieldErrors
        );

        return Mono.just(ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response));
    }

    @ExceptionHandler(Exception.class)
    public Mono<ResponseEntity<ErrorResponse>> handleUnexpectedException(
            Exception ex,
            ServerWebExchange exchange) {
        log.error("Unexpected exception", ex);

        String path = exchange.getRequest().getPath().value();
        String message = messageService.getMessage(MessageCodes.INTERNAL_ERROR);
        ErrorResponse response = ErrorResponse.of(MessageCodes.INTERNAL_ERROR, message, path);

        return Mono.just(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response));
    }
}
```

---

## Usage Examples

### Throwing Exceptions

```java
// ❌ KHÔNG hardcode message
throw new BusinessException("AUTH_ERROR", "Email không đúng", HttpStatus.UNAUTHORIZED);

// ✅ Sử dụng message code
throw new BusinessException(MessageCodes.AUTH_INVALID_CREDENTIALS, HttpStatus.UNAUTHORIZED);

// ✅ Với parameters
throw new EntityNotFoundException("User", userId);
// -> "User với ID 123 không tồn tại"
```

### In Service Layer

```java
@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final MessageService messageService;

    public UserResponse createUser(CreateUserRequest request) {
        // ... create user ...

        // Return success message
        String message = messageService.getMessage(MessageCodes.SUCCESS_CREATED);
        return ApiResponse.success(user, message);
    }
}
```

### Bean Validation

```java
public record CreateUserRequest(
    @NotBlank(message = "{validation.required}")
    @Size(min = 2, max = 100, message = "{validation.min_length}")
    String name,

    @NotBlank(message = "{validation.required}")
    @Email(message = "{validation.email.invalid}")
    String email,

    @Pattern(regexp = "^0\\d{9}$", message = "{validation.phone.invalid}")
    String phone
) {}
```

---

## Checklist

Khi code exception/message:

1. ✅ Tạo message code trong `MessageCodes.java`
2. ✅ Thêm message vào `messages.properties`
3. ✅ Sử dụng `MessageService` hoặc exception với code
4. ❌ KHÔNG hardcode string message trong Java
5. ❌ KHÔNG dùng String.format() cho error messages

---

## Actions

### Thêm component mới (UI)
```bash
pnpm dlx shadcn-ui@latest add [component-name]
```

### Xem tất cả Shadcn components
https://ui.shadcn.com/docs/components

### Chọn theme khi tạo instance
Trên KiteHub → Create Instance → Step 3: Choose Theme

### Đổi logo/màu
Instance → Settings → Branding → Upload/Edit

### Đổi theme template
KiteHub → Instances → [instance] → Settings → Theme

### Custom CSS (Enterprise)
KiteHub → Instances → [instance] → Settings → Advanced → Custom CSS

### Thêm message mới (i18n)
1. Thêm constant vào `MessageCodes.java`
2. Thêm message vào `messages.properties`
3. (Optional) Thêm vào `messages_en.properties`

### Khi review code
- Tìm hardcoded Vietnamese strings
- Đảm bảo tất cả exception dùng message codes
- Verify messages.properties có đầy đủ entries
