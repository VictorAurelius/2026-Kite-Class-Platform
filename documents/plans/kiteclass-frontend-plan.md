# PLAN: KiteClass Frontend Implementation

## Thông tin

| Thuộc tính | Giá trị |
|------------|---------|
| **Project** | kiteclass-frontend |
| **Tech Stack** | Next.js 14, TypeScript, Tailwind CSS, Shadcn/UI |
| **Mục đích** | Multi-portal frontend: Teacher, Student, Parent |
| **Tham chiếu** | architecture-overview, ui-components, theme-system, api-design |

---

# PHASE 1: PROJECT INITIALIZATION

## 1.1. Create Project

```bash
# Create Next.js project
pnpm create next-app@latest kiteclass-frontend --typescript --tailwind --eslint --app --src-dir --import-alias "@/*"

cd kiteclass-frontend

# Install core dependencies
pnpm add @tanstack/react-query @tanstack/react-query-devtools
pnpm add zustand
pnpm add axios
pnpm add zod @hookform/resolvers react-hook-form
pnpm add date-fns
pnpm add lucide-react
pnpm add clsx tailwind-merge
pnpm add next-themes

# Dev dependencies
pnpm add -D @types/node prettier prettier-plugin-tailwindcss
```

## 1.2. Setup Shadcn/UI

```bash
# Initialize shadcn
pnpm dlx shadcn-ui@latest init

# Add essential components
pnpm dlx shadcn-ui@latest add button
pnpm dlx shadcn-ui@latest add input
pnpm dlx shadcn-ui@latest add label
pnpm dlx shadcn-ui@latest add card
pnpm dlx shadcn-ui@latest add table
pnpm dlx shadcn-ui@latest add dialog
pnpm dlx shadcn-ui@latest add dropdown-menu
pnpm dlx shadcn-ui@latest add select
pnpm dlx shadcn-ui@latest add form
pnpm dlx shadcn-ui@latest add toast
pnpm dlx shadcn-ui@latest add avatar
pnpm dlx shadcn-ui@latest add badge
pnpm dlx shadcn-ui@latest add sheet
pnpm dlx shadcn-ui@latest add tabs
pnpm dlx shadcn-ui@latest add skeleton
pnpm dlx shadcn-ui@latest add separator
pnpm dlx shadcn-ui@latest add popover
pnpm dlx shadcn-ui@latest add calendar
pnpm dlx shadcn-ui@latest add checkbox
pnpm dlx shadcn-ui@latest add alert
pnpm dlx shadcn-ui@latest add tooltip
```

## 1.3. Project Structure

```
kiteclass-frontend/
├── src/
│   ├── app/                              # App Router
│   │   ├── (auth)/                       # Auth layout group
│   │   │   ├── login/
│   │   │   │   └── page.tsx
│   │   │   ├── forgot-password/
│   │   │   │   └── page.tsx
│   │   │   └── layout.tsx
│   │   │
│   │   ├── (dashboard)/                  # Dashboard layout group
│   │   │   ├── layout.tsx                # Sidebar + Header
│   │   │   ├── page.tsx                  # Dashboard home
│   │   │   │
│   │   │   ├── students/                 # Student management
│   │   │   │   ├── page.tsx              # List students
│   │   │   │   ├── [id]/
│   │   │   │   │   ├── page.tsx          # Student detail
│   │   │   │   │   └── edit/
│   │   │   │   │       └── page.tsx      # Edit student
│   │   │   │   └── new/
│   │   │   │       └── page.tsx          # Create student
│   │   │   │
│   │   │   ├── classes/                  # Class management
│   │   │   │   ├── page.tsx
│   │   │   │   └── [id]/
│   │   │   │       ├── page.tsx
│   │   │   │       ├── attendance/
│   │   │   │       │   └── page.tsx
│   │   │   │       └── students/
│   │   │   │           └── page.tsx
│   │   │   │
│   │   │   ├── courses/                  # Course management
│   │   │   │   ├── page.tsx
│   │   │   │   └── [id]/
│   │   │   │       └── page.tsx
│   │   │   │
│   │   │   ├── attendance/               # Attendance overview
│   │   │   │   └── page.tsx
│   │   │   │
│   │   │   ├── billing/                  # Billing management
│   │   │   │   ├── invoices/
│   │   │   │   │   ├── page.tsx
│   │   │   │   │   └── [id]/
│   │   │   │   │       └── page.tsx
│   │   │   │   └── payments/
│   │   │   │       └── page.tsx
│   │   │   │
│   │   │   ├── reports/                  # Reports
│   │   │   │   └── page.tsx
│   │   │   │
│   │   │   └── settings/                 # Settings
│   │   │       ├── page.tsx
│   │   │       ├── branding/
│   │   │       │   └── page.tsx
│   │   │       └── profile/
│   │   │           └── page.tsx
│   │   │
│   │   ├── (parent)/                     # Parent portal
│   │   │   ├── layout.tsx
│   │   │   ├── page.tsx                  # Parent dashboard
│   │   │   ├── children/
│   │   │   │   └── [id]/
│   │   │   │       ├── page.tsx
│   │   │   │       ├── attendance/
│   │   │   │       │   └── page.tsx
│   │   │   │       └── grades/
│   │   │   │           └── page.tsx
│   │   │   └── invoices/
│   │   │       └── page.tsx
│   │   │
│   │   ├── globals.css
│   │   └── layout.tsx                    # Root layout
│   │
│   ├── components/                       # Shared components
│   │   ├── ui/                           # Shadcn UI components
│   │   │   └── ...
│   │   │
│   │   ├── layout/                       # Layout components
│   │   │   ├── sidebar/
│   │   │   │   ├── sidebar.tsx
│   │   │   │   ├── sidebar-item.tsx
│   │   │   │   └── sidebar-config.ts
│   │   │   ├── header/
│   │   │   │   ├── header.tsx
│   │   │   │   ├── user-nav.tsx
│   │   │   │   └── theme-toggle.tsx
│   │   │   └── breadcrumb.tsx
│   │   │
│   │   ├── forms/                        # Form components
│   │   │   ├── student-form.tsx
│   │   │   ├── class-form.tsx
│   │   │   ├── course-form.tsx
│   │   │   └── attendance-form.tsx
│   │   │
│   │   ├── tables/                       # Table components
│   │   │   ├── data-table.tsx
│   │   │   ├── data-table-toolbar.tsx
│   │   │   ├── data-table-pagination.tsx
│   │   │   └── columns/
│   │   │       ├── student-columns.tsx
│   │   │       ├── class-columns.tsx
│   │   │       └── invoice-columns.tsx
│   │   │
│   │   └── shared/                       # Other shared components
│   │       ├── page-header.tsx
│   │       ├── loading-spinner.tsx
│   │       ├── empty-state.tsx
│   │       ├── error-boundary.tsx
│   │       ├── status-badge.tsx
│   │       ├── confirm-dialog.tsx
│   │       └── stats-card.tsx
│   │
│   ├── hooks/                            # Custom hooks
│   │   ├── use-auth.ts
│   │   ├── use-students.ts
│   │   ├── use-classes.ts
│   │   ├── use-courses.ts
│   │   ├── use-attendance.ts
│   │   ├── use-invoices.ts
│   │   ├── use-branding.ts
│   │   ├── use-debounce.ts
│   │   └── use-media-query.ts
│   │
│   ├── lib/                              # Utilities
│   │   ├── api/
│   │   │   ├── client.ts                 # Axios instance
│   │   │   ├── endpoints.ts              # API endpoints
│   │   │   └── types.ts                  # API response types
│   │   ├── utils.ts                      # Helper functions
│   │   ├── validations/                  # Zod schemas
│   │   │   ├── student.ts
│   │   │   ├── class.ts
│   │   │   ├── course.ts
│   │   │   └── auth.ts
│   │   ├── constants.ts                  # Constants
│   │   └── format.ts                     # Formatters (date, currency)
│   │
│   ├── providers/                        # React providers
│   │   ├── query-provider.tsx            # React Query
│   │   ├── theme-provider.tsx            # Theme (light/dark)
│   │   ├── auth-provider.tsx             # Auth context
│   │   └── toaster-provider.tsx          # Toast notifications
│   │
│   ├── stores/                           # Zustand stores
│   │   ├── auth-store.ts
│   │   ├── ui-store.ts
│   │   └── index.ts
│   │
│   └── types/                            # TypeScript types
│       ├── api.ts
│       ├── student.ts
│       ├── class.ts
│       ├── course.ts
│       ├── attendance.ts
│       ├── invoice.ts
│       ├── user.ts
│       └── index.ts
│
├── public/
│   └── images/
│       ├── logo.svg
│       └── placeholder.svg
│
├── .env.local
├── .env.example
├── next.config.js
├── tailwind.config.ts
├── tsconfig.json
├── package.json
└── README.md
```

## 1.4. Environment Configuration

```bash
# .env.example
NEXT_PUBLIC_API_URL=http://localhost:8080/api/v1
NEXT_PUBLIC_APP_NAME=KiteClass
NEXT_PUBLIC_APP_VERSION=1.0.0
```

## 1.5. Next.js Configuration

```typescript
// next.config.js
/** @type {import('next').NextConfig} */
const nextConfig = {
  images: {
    remotePatterns: [
      {
        protocol: 'https',
        hostname: 'cdn.kiteclass.com',
      },
    ],
  },
  experimental: {
    serverActions: {
      bodySizeLimit: '2mb',
    },
  },
};

module.exports = nextConfig;
```

## 1.6. Tailwind Configuration

```typescript
// tailwind.config.ts
import type { Config } from 'tailwindcss';

const config: Config = {
  darkMode: ['class'],
  content: [
    './src/pages/**/*.{js,ts,jsx,tsx,mdx}',
    './src/components/**/*.{js,ts,jsx,tsx,mdx}',
    './src/app/**/*.{js,ts,jsx,tsx,mdx}',
  ],
  theme: {
    extend: {
      colors: {
        // CSS variables for theming
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
  plugins: [require('tailwindcss-animate')],
};

export default config;
```

---

# PHASE 2: CORE INFRASTRUCTURE

## 2.1. API Client

```typescript
// src/lib/api/client.ts
import axios, { AxiosError, InternalAxiosRequestConfig } from 'axios';
import { useAuthStore } from '@/stores/auth-store';

const API_URL = process.env.NEXT_PUBLIC_API_URL || 'http://localhost:8080/api/v1';

export const apiClient = axios.create({
  baseURL: API_URL,
  timeout: 30000,
  headers: {
    'Content-Type': 'application/json',
  },
});

// Request interceptor
apiClient.interceptors.request.use(
  (config: InternalAxiosRequestConfig) => {
    const token = useAuthStore.getState().accessToken;
    if (token) {
      config.headers.Authorization = `Bearer ${token}`;
    }
    return config;
  },
  (error) => Promise.reject(error)
);

// Response interceptor
apiClient.interceptors.response.use(
  (response) => response,
  async (error: AxiosError) => {
    const originalRequest = error.config as InternalAxiosRequestConfig & { _retry?: boolean };

    // Handle 401 - Token expired
    if (error.response?.status === 401 && !originalRequest._retry) {
      originalRequest._retry = true;

      const refreshToken = useAuthStore.getState().refreshToken;
      if (refreshToken) {
        try {
          const response = await axios.post(`${API_URL}/auth/refresh`, {
            refreshToken,
          });

          const { accessToken } = response.data.data;
          useAuthStore.getState().setAccessToken(accessToken);

          originalRequest.headers.Authorization = `Bearer ${accessToken}`;
          return apiClient(originalRequest);
        } catch (refreshError) {
          useAuthStore.getState().logout();
          window.location.href = '/login';
        }
      }
    }

    return Promise.reject(error);
  }
);

// API helper functions
export const api = {
  get: <T>(url: string, params?: object) =>
    apiClient.get<T>(url, { params }).then((res) => res.data),

  post: <T>(url: string, data?: object) =>
    apiClient.post<T>(url, data).then((res) => res.data),

  put: <T>(url: string, data?: object) =>
    apiClient.put<T>(url, data).then((res) => res.data),

  patch: <T>(url: string, data?: object) =>
    apiClient.patch<T>(url, data).then((res) => res.data),

  delete: <T>(url: string) =>
    apiClient.delete<T>(url).then((res) => res.data),
};
```

## 2.2. API Endpoints

```typescript
// src/lib/api/endpoints.ts
export const API_ENDPOINTS = {
  // Auth
  auth: {
    login: '/auth/login',
    logout: '/auth/logout',
    refresh: '/auth/refresh',
    me: '/auth/me',
    forgotPassword: '/auth/forgot-password',
    resetPassword: '/auth/reset-password',
  },

  // Users
  users: {
    list: '/users',
    detail: (id: number) => `/users/${id}`,
    create: '/users',
    update: (id: number) => `/users/${id}`,
    delete: (id: number) => `/users/${id}`,
  },

  // Students
  students: {
    list: '/students',
    detail: (id: number) => `/students/${id}`,
    create: '/students',
    update: (id: number) => `/students/${id}`,
    delete: (id: number) => `/students/${id}`,
    enroll: (id: number) => `/students/${id}/enroll`,
  },

  // Classes
  classes: {
    list: '/classes',
    detail: (id: number) => `/classes/${id}`,
    create: '/classes',
    update: (id: number) => `/classes/${id}`,
    delete: (id: number) => `/classes/${id}`,
    students: (id: number) => `/classes/${id}/students`,
    sessions: (id: number) => `/classes/${id}/sessions`,
    attendance: (id: number) => `/classes/${id}/attendance`,
  },

  // Courses
  courses: {
    list: '/courses',
    detail: (id: number) => `/courses/${id}`,
    create: '/courses',
    update: (id: number) => `/courses/${id}`,
  },

  // Invoices
  invoices: {
    list: '/invoices',
    detail: (id: number) => `/invoices/${id}`,
    create: '/invoices',
    send: (id: number) => `/invoices/${id}/send`,
    payments: (id: number) => `/invoices/${id}/payments`,
  },

  // Settings
  settings: {
    branding: '/settings/branding',
    uploadLogo: '/settings/branding/logo',
    theme: '/settings/theme',
    preferences: '/users/me/preferences',
  },

  // Parent Portal
  parent: {
    children: '/parent/children',
    childDetail: (id: number) => `/parent/children/${id}`,
    childAttendance: (id: number) => `/parent/children/${id}/attendance`,
    childGrades: (id: number) => `/parent/children/${id}/grades`,
    invoices: '/parent/invoices',
  },
} as const;
```

## 2.3. TypeScript Types

```typescript
// src/types/api.ts
export interface ApiResponse<T> {
  data: T;
  message?: string;
  timestamp: string;
}

export interface PageResponse<T> {
  content: T[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
  first: boolean;
  last: boolean;
}

export interface ErrorResponse {
  error: {
    code: string;
    message: string;
    details?: Record<string, string>;
  };
  timestamp: string;
  path: string;
}

// src/types/student.ts
export type StudentStatus = 'ACTIVE' | 'INACTIVE' | 'GRADUATED' | 'DROPPED' | 'PENDING';
export type Gender = 'MALE' | 'FEMALE' | 'OTHER';

export interface Student {
  id: number;
  name: string;
  email: string | null;
  phone: string | null;
  dateOfBirth: string | null;
  gender: Gender | null;
  address: string | null;
  avatarUrl: string | null;
  status: StudentStatus;
  note: string | null;
  createdAt: string;
  updatedAt: string;
}

export interface CreateStudentRequest {
  name: string;
  email?: string;
  phone?: string;
  dateOfBirth?: string;
  gender?: Gender;
  address?: string;
  note?: string;
}

export interface UpdateStudentRequest {
  name: string;
  email?: string;
  phone?: string;
  dateOfBirth?: string;
  gender?: Gender;
  address?: string;
  status?: StudentStatus;
  note?: string;
}

// src/types/class.ts
export type ClassStatus = 'DRAFT' | 'SCHEDULED' | 'IN_PROGRESS' | 'COMPLETED' | 'CANCELLED';
export type DayOfWeek = 'MONDAY' | 'TUESDAY' | 'WEDNESDAY' | 'THURSDAY' | 'FRIDAY' | 'SATURDAY' | 'SUNDAY';

export interface ClassSchedule {
  id: number;
  dayOfWeek: DayOfWeek;
  startTime: string;
  endTime: string;
  room: string | null;
}

export interface Class {
  id: number;
  name: string;
  course: {
    id: number;
    name: string;
  };
  teacher: {
    id: number;
    name: string;
  };
  maxStudents: number;
  currentStudents: number;
  tuitionFee: number;
  startDate: string;
  endDate: string;
  status: ClassStatus;
  schedules: ClassSchedule[];
}

// src/types/attendance.ts
export type AttendanceStatus = 'PRESENT' | 'ABSENT' | 'LATE' | 'EXCUSED';

export interface Attendance {
  id: number;
  sessionId: number;
  studentId: number;
  studentName: string;
  status: AttendanceStatus;
  checkinTime: string | null;
  note: string | null;
}

export interface MarkAttendanceRequest {
  studentId: number;
  status: AttendanceStatus;
  note?: string;
}

// src/types/user.ts
export type UserRole = 'OWNER' | 'ADMIN' | 'TEACHER' | 'STAFF' | 'STUDENT' | 'PARENT';
export type UserStatus = 'ACTIVE' | 'INACTIVE' | 'PENDING' | 'LOCKED';

export interface User {
  id: number;
  email: string;
  name: string;
  phone: string | null;
  avatar: string | null;
  roles: UserRole[];
  permissions: string[];
  status: UserStatus;
}

export interface AuthResponse {
  accessToken: string;
  refreshToken: string;
  tokenType: string;
  expiresIn: number;
  user: User;
}
```

## 2.4. Zustand Store - Auth

```typescript
// src/stores/auth-store.ts
import { create } from 'zustand';
import { persist, createJSONStorage } from 'zustand/middleware';
import type { User } from '@/types/user';

interface AuthState {
  user: User | null;
  accessToken: string | null;
  refreshToken: string | null;
  isAuthenticated: boolean;

  // Actions
  setAuth: (user: User, accessToken: string, refreshToken: string) => void;
  setAccessToken: (token: string) => void;
  logout: () => void;
  hasRole: (role: string) => boolean;
  hasPermission: (permission: string) => boolean;
}

export const useAuthStore = create<AuthState>()(
  persist(
    (set, get) => ({
      user: null,
      accessToken: null,
      refreshToken: null,
      isAuthenticated: false,

      setAuth: (user, accessToken, refreshToken) => {
        set({
          user,
          accessToken,
          refreshToken,
          isAuthenticated: true,
        });
      },

      setAccessToken: (token) => {
        set({ accessToken: token });
      },

      logout: () => {
        set({
          user: null,
          accessToken: null,
          refreshToken: null,
          isAuthenticated: false,
        });
      },

      hasRole: (role) => {
        const user = get().user;
        return user?.roles?.includes(role as any) ?? false;
      },

      hasPermission: (permission) => {
        const user = get().user;
        return user?.permissions?.includes(permission) ?? false;
      },
    }),
    {
      name: 'kiteclass-auth',
      storage: createJSONStorage(() => localStorage),
      partialize: (state) => ({
        accessToken: state.accessToken,
        refreshToken: state.refreshToken,
        user: state.user,
      }),
    }
  )
);
```

## 2.5. Zustand Store - UI

```typescript
// src/stores/ui-store.ts
import { create } from 'zustand';
import { persist } from 'zustand/middleware';

interface UIState {
  sidebarOpen: boolean;
  sidebarCollapsed: boolean;

  // Actions
  toggleSidebar: () => void;
  setSidebarOpen: (open: boolean) => void;
  toggleSidebarCollapsed: () => void;
}

export const useUIStore = create<UIState>()(
  persist(
    (set) => ({
      sidebarOpen: true,
      sidebarCollapsed: false,

      toggleSidebar: () => set((state) => ({ sidebarOpen: !state.sidebarOpen })),
      setSidebarOpen: (open) => set({ sidebarOpen: open }),
      toggleSidebarCollapsed: () =>
        set((state) => ({ sidebarCollapsed: !state.sidebarCollapsed })),
    }),
    {
      name: 'kiteclass-ui',
      partialize: (state) => ({ sidebarCollapsed: state.sidebarCollapsed }),
    }
  )
);
```

---

# PHASE 3: PROVIDERS & ROOT LAYOUT

## 3.1. Query Provider

```typescript
// src/providers/query-provider.tsx
'use client';

import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { ReactQueryDevtools } from '@tanstack/react-query-devtools';
import { useState } from 'react';

export function QueryProvider({ children }: { children: React.ReactNode }) {
  const [queryClient] = useState(
    () =>
      new QueryClient({
        defaultOptions: {
          queries: {
            staleTime: 60 * 1000, // 1 minute
            gcTime: 5 * 60 * 1000, // 5 minutes
            retry: 1,
            refetchOnWindowFocus: false,
          },
        },
      })
  );

  return (
    <QueryClientProvider client={queryClient}>
      {children}
      <ReactQueryDevtools initialIsOpen={false} />
    </QueryClientProvider>
  );
}
```

## 3.2. Theme Provider

```typescript
// src/providers/theme-provider.tsx
'use client';

import { ThemeProvider as NextThemesProvider } from 'next-themes';
import { useEffect, useState } from 'react';
import { useQuery } from '@tanstack/react-query';
import { api } from '@/lib/api/client';
import { API_ENDPOINTS } from '@/lib/api/endpoints';

interface ThemeConfig {
  templateId: string;
  colors: {
    primary: Record<string, string>;
    background: string;
    foreground: string;
  };
  typography: {
    fontFamily: string;
    fontSize: Record<string, string>;
  };
  borderRadius: string;
}

function applyThemeVariables(theme: ThemeConfig) {
  const root = document.documentElement;

  // Apply primary colors
  Object.entries(theme.colors.primary).forEach(([key, value]) => {
    root.style.setProperty(`--color-primary-${key}`, value);
  });
  root.style.setProperty('--color-primary', theme.colors.primary['500']);

  // Apply font
  root.style.setProperty('--font-sans', theme.typography.fontFamily);

  // Apply border radius
  root.style.setProperty('--radius-sm', '0.125rem');
  root.style.setProperty('--radius-md', theme.borderRadius);
  root.style.setProperty('--radius-lg', '0.5rem');
  root.style.setProperty('--radius-xl', '0.75rem');
}

export function ThemeProvider({ children }: { children: React.ReactNode }) {
  const [mounted, setMounted] = useState(false);

  const { data: theme } = useQuery({
    queryKey: ['theme'],
    queryFn: () => api.get<{ data: ThemeConfig }>(API_ENDPOINTS.settings.theme),
    staleTime: 60 * 60 * 1000, // 1 hour
    enabled: mounted,
  });

  useEffect(() => {
    setMounted(true);
  }, []);

  useEffect(() => {
    if (theme?.data) {
      applyThemeVariables(theme.data);
    }
  }, [theme]);

  if (!mounted) {
    return null;
  }

  return (
    <NextThemesProvider
      attribute="class"
      defaultTheme="system"
      enableSystem
      disableTransitionOnChange
    >
      {children}
    </NextThemesProvider>
  );
}
```

## 3.3. Auth Provider

```typescript
// src/providers/auth-provider.tsx
'use client';

import { useEffect, useState } from 'react';
import { useRouter, usePathname } from 'next/navigation';
import { useAuthStore } from '@/stores/auth-store';
import { LoadingSpinner } from '@/components/shared/loading-spinner';

const publicPaths = ['/login', '/forgot-password', '/reset-password'];

export function AuthProvider({ children }: { children: React.ReactNode }) {
  const [isLoading, setIsLoading] = useState(true);
  const router = useRouter();
  const pathname = usePathname();
  const { isAuthenticated, accessToken } = useAuthStore();

  useEffect(() => {
    const isPublicPath = publicPaths.some((path) => pathname.startsWith(path));

    if (!accessToken && !isPublicPath) {
      router.replace('/login');
    } else if (accessToken && isPublicPath) {
      router.replace('/');
    }

    setIsLoading(false);
  }, [accessToken, pathname, router]);

  if (isLoading) {
    return (
      <div className="flex h-screen items-center justify-center">
        <LoadingSpinner size="lg" />
      </div>
    );
  }

  return <>{children}</>;
}
```

## 3.4. Root Layout

```typescript
// src/app/layout.tsx
import type { Metadata } from 'next';
import { Inter } from 'next/font/google';
import './globals.css';
import { QueryProvider } from '@/providers/query-provider';
import { ThemeProvider } from '@/providers/theme-provider';
import { AuthProvider } from '@/providers/auth-provider';
import { Toaster } from '@/components/ui/toaster';

const inter = Inter({ subsets: ['latin', 'vietnamese'], variable: '--font-sans' });

export const metadata: Metadata = {
  title: 'KiteClass',
  description: 'Hệ thống quản lý trung tâm dạy học',
};

export default function RootLayout({
  children,
}: {
  children: React.ReactNode;
}) {
  return (
    <html lang="vi" suppressHydrationWarning>
      <body className={inter.className}>
        <QueryProvider>
          <ThemeProvider>
            <AuthProvider>
              {children}
              <Toaster />
            </AuthProvider>
          </ThemeProvider>
        </QueryProvider>
      </body>
    </html>
  );
}
```

---

# PHASE 4: SHARED COMPONENTS

## 4.1. Page Header

```typescript
// src/components/shared/page-header.tsx
import { cn } from '@/lib/utils';

interface PageHeaderProps {
  title: string;
  description?: string;
  actions?: React.ReactNode;
  className?: string;
}

export function PageHeader({ title, description, actions, className }: PageHeaderProps) {
  return (
    <div className={cn('flex items-center justify-between', className)}>
      <div className="space-y-1">
        <h1 className="text-2xl font-bold tracking-tight">{title}</h1>
        {description && (
          <p className="text-sm text-muted-foreground">{description}</p>
        )}
      </div>
      {actions && <div className="flex items-center gap-2">{actions}</div>}
    </div>
  );
}
```

## 4.2. Loading Spinner

```typescript
// src/components/shared/loading-spinner.tsx
import { cn } from '@/lib/utils';
import { Loader2 } from 'lucide-react';

interface LoadingSpinnerProps {
  size?: 'sm' | 'md' | 'lg';
  className?: string;
}

const sizeClasses = {
  sm: 'h-4 w-4',
  md: 'h-8 w-8',
  lg: 'h-12 w-12',
};

export function LoadingSpinner({ size = 'md', className }: LoadingSpinnerProps) {
  return (
    <Loader2
      className={cn('animate-spin text-primary', sizeClasses[size], className)}
    />
  );
}
```

## 4.3. Status Badge

```typescript
// src/components/shared/status-badge.tsx
import { Badge } from '@/components/ui/badge';
import { cn } from '@/lib/utils';

type StatusVariant = 'default' | 'success' | 'warning' | 'error' | 'info';

interface StatusBadgeProps {
  status: string;
  variant?: StatusVariant;
  className?: string;
}

const variantClasses: Record<StatusVariant, string> = {
  default: 'bg-gray-100 text-gray-800 hover:bg-gray-100',
  success: 'bg-green-100 text-green-800 hover:bg-green-100',
  warning: 'bg-yellow-100 text-yellow-800 hover:bg-yellow-100',
  error: 'bg-red-100 text-red-800 hover:bg-red-100',
  info: 'bg-blue-100 text-blue-800 hover:bg-blue-100',
};

// Map status to variant
const statusVariantMap: Record<string, StatusVariant> = {
  ACTIVE: 'success',
  IN_PROGRESS: 'success',
  PRESENT: 'success',
  PAID: 'success',
  INACTIVE: 'default',
  DRAFT: 'default',
  PENDING: 'info',
  SCHEDULED: 'info',
  SENT: 'info',
  LATE: 'warning',
  OVERDUE: 'warning',
  PARTIAL: 'warning',
  ABSENT: 'error',
  CANCELLED: 'error',
  DROPPED: 'error',
};

export function StatusBadge({ status, variant, className }: StatusBadgeProps) {
  const resolvedVariant = variant || statusVariantMap[status] || 'default';

  return (
    <Badge
      variant="outline"
      className={cn(variantClasses[resolvedVariant], className)}
    >
      {status}
    </Badge>
  );
}
```

## 4.4. Empty State

```typescript
// src/components/shared/empty-state.tsx
import { cn } from '@/lib/utils';
import { LucideIcon } from 'lucide-react';

interface EmptyStateProps {
  icon?: LucideIcon;
  title: string;
  description?: string;
  action?: React.ReactNode;
  className?: string;
}

export function EmptyState({
  icon: Icon,
  title,
  description,
  action,
  className,
}: EmptyStateProps) {
  return (
    <div
      className={cn(
        'flex flex-col items-center justify-center py-12 text-center',
        className
      )}
    >
      {Icon && <Icon className="mb-4 h-12 w-12 text-muted-foreground" />}
      <h3 className="text-lg font-semibold">{title}</h3>
      {description && (
        <p className="mt-1 text-sm text-muted-foreground">{description}</p>
      )}
      {action && <div className="mt-4">{action}</div>}
    </div>
  );
}
```

## 4.5. Stats Card

```typescript
// src/components/shared/stats-card.tsx
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card';
import { cn } from '@/lib/utils';
import { LucideIcon } from 'lucide-react';

interface StatsCardProps {
  title: string;
  value: string | number;
  description?: string;
  icon?: LucideIcon;
  trend?: {
    value: number;
    isPositive: boolean;
  };
  className?: string;
}

export function StatsCard({
  title,
  value,
  description,
  icon: Icon,
  trend,
  className,
}: StatsCardProps) {
  return (
    <Card className={className}>
      <CardHeader className="flex flex-row items-center justify-between space-y-0 pb-2">
        <CardTitle className="text-sm font-medium">{title}</CardTitle>
        {Icon && <Icon className="h-4 w-4 text-muted-foreground" />}
      </CardHeader>
      <CardContent>
        <div className="text-2xl font-bold">{value}</div>
        {(description || trend) && (
          <p className="text-xs text-muted-foreground">
            {trend && (
              <span
                className={cn(
                  'mr-1',
                  trend.isPositive ? 'text-green-600' : 'text-red-600'
                )}
              >
                {trend.isPositive ? '+' : ''}{trend.value}%
              </span>
            )}
            {description}
          </p>
        )}
      </CardContent>
    </Card>
  );
}
```

## 4.6. Confirm Dialog

```typescript
// src/components/shared/confirm-dialog.tsx
'use client';

import {
  AlertDialog,
  AlertDialogAction,
  AlertDialogCancel,
  AlertDialogContent,
  AlertDialogDescription,
  AlertDialogFooter,
  AlertDialogHeader,
  AlertDialogTitle,
} from '@/components/ui/alert-dialog';

interface ConfirmDialogProps {
  open: boolean;
  onOpenChange: (open: boolean) => void;
  title: string;
  description: string;
  confirmText?: string;
  cancelText?: string;
  variant?: 'default' | 'destructive';
  onConfirm: () => void;
  isLoading?: boolean;
}

export function ConfirmDialog({
  open,
  onOpenChange,
  title,
  description,
  confirmText = 'Xác nhận',
  cancelText = 'Hủy',
  variant = 'default',
  onConfirm,
  isLoading,
}: ConfirmDialogProps) {
  return (
    <AlertDialog open={open} onOpenChange={onOpenChange}>
      <AlertDialogContent>
        <AlertDialogHeader>
          <AlertDialogTitle>{title}</AlertDialogTitle>
          <AlertDialogDescription>{description}</AlertDialogDescription>
        </AlertDialogHeader>
        <AlertDialogFooter>
          <AlertDialogCancel disabled={isLoading}>{cancelText}</AlertDialogCancel>
          <AlertDialogAction
            onClick={onConfirm}
            disabled={isLoading}
            className={
              variant === 'destructive'
                ? 'bg-red-600 hover:bg-red-700'
                : undefined
            }
          >
            {isLoading ? 'Đang xử lý...' : confirmText}
          </AlertDialogAction>
        </AlertDialogFooter>
      </AlertDialogContent>
    </AlertDialog>
  );
}
```

---

# PHASE 5: LAYOUT COMPONENTS

## 5.1. Sidebar Configuration

```typescript
// src/components/layout/sidebar/sidebar-config.ts
import {
  LayoutDashboard,
  Users,
  GraduationCap,
  Calendar,
  ClipboardCheck,
  Receipt,
  Settings,
  BarChart3,
  BookOpen,
  UserCheck,
} from 'lucide-react';

export interface NavItem {
  title: string;
  href: string;
  icon: any;
  badge?: number;
  roles?: string[];
  children?: NavItem[];
}

export const navItems: NavItem[] = [
  {
    title: 'Dashboard',
    href: '/',
    icon: LayoutDashboard,
  },
  {
    title: 'Học viên',
    href: '/students',
    icon: Users,
    roles: ['OWNER', 'ADMIN', 'TEACHER', 'STAFF'],
  },
  {
    title: 'Khóa học',
    href: '/courses',
    icon: BookOpen,
    roles: ['OWNER', 'ADMIN'],
  },
  {
    title: 'Lớp học',
    href: '/classes',
    icon: GraduationCap,
    roles: ['OWNER', 'ADMIN', 'TEACHER'],
  },
  {
    title: 'Điểm danh',
    href: '/attendance',
    icon: ClipboardCheck,
    roles: ['OWNER', 'ADMIN', 'TEACHER'],
  },
  {
    title: 'Hóa đơn',
    href: '/billing/invoices',
    icon: Receipt,
    roles: ['OWNER', 'ADMIN', 'STAFF'],
  },
  {
    title: 'Báo cáo',
    href: '/reports',
    icon: BarChart3,
    roles: ['OWNER', 'ADMIN'],
  },
  {
    title: 'Cài đặt',
    href: '/settings',
    icon: Settings,
    roles: ['OWNER', 'ADMIN'],
  },
];

export const parentNavItems: NavItem[] = [
  {
    title: 'Tổng quan',
    href: '/',
    icon: LayoutDashboard,
  },
  {
    title: 'Con em',
    href: '/children',
    icon: Users,
  },
  {
    title: 'Hóa đơn',
    href: '/invoices',
    icon: Receipt,
  },
];
```

## 5.2. Sidebar Component

```typescript
// src/components/layout/sidebar/sidebar.tsx
'use client';

import Link from 'next/link';
import { usePathname } from 'next/navigation';
import { cn } from '@/lib/utils';
import { navItems, NavItem } from './sidebar-config';
import { useAuthStore } from '@/stores/auth-store';
import { useUIStore } from '@/stores/ui-store';
import { Button } from '@/components/ui/button';
import { ChevronLeft, ChevronRight } from 'lucide-react';

function SidebarItem({ item, collapsed }: { item: NavItem; collapsed: boolean }) {
  const pathname = usePathname();
  const isActive = pathname === item.href || pathname.startsWith(`${item.href}/`);

  return (
    <Link
      href={item.href}
      className={cn(
        'flex items-center gap-3 rounded-lg px-3 py-2 text-sm transition-colors',
        isActive
          ? 'bg-primary/10 text-primary font-medium'
          : 'text-muted-foreground hover:bg-muted hover:text-foreground'
      )}
    >
      <item.icon className="h-4 w-4 shrink-0" />
      {!collapsed && <span>{item.title}</span>}
      {!collapsed && item.badge !== undefined && item.badge > 0 && (
        <span className="ml-auto flex h-5 w-5 items-center justify-center rounded-full bg-primary text-[10px] font-medium text-primary-foreground">
          {item.badge}
        </span>
      )}
    </Link>
  );
}

export function Sidebar() {
  const { hasRole } = useAuthStore();
  const { sidebarCollapsed, toggleSidebarCollapsed } = useUIStore();

  const filteredItems = navItems.filter((item) => {
    if (!item.roles) return true;
    return item.roles.some((role) => hasRole(role));
  });

  return (
    <aside
      className={cn(
        'flex h-screen flex-col border-r bg-background transition-all duration-300',
        sidebarCollapsed ? 'w-16' : 'w-64'
      )}
    >
      {/* Logo */}
      <div className="flex h-16 items-center justify-between border-b px-4">
        {!sidebarCollapsed && (
          <Link href="/" className="flex items-center gap-2">
            <span className="text-xl font-bold text-primary">KiteClass</span>
          </Link>
        )}
        <Button
          variant="ghost"
          size="icon"
          onClick={toggleSidebarCollapsed}
          className="ml-auto"
        >
          {sidebarCollapsed ? (
            <ChevronRight className="h-4 w-4" />
          ) : (
            <ChevronLeft className="h-4 w-4" />
          )}
        </Button>
      </div>

      {/* Navigation */}
      <nav className="flex-1 space-y-1 overflow-y-auto p-3">
        {filteredItems.map((item) => (
          <SidebarItem key={item.href} item={item} collapsed={sidebarCollapsed} />
        ))}
      </nav>
    </aside>
  );
}
```

## 5.3. Header Component

```typescript
// src/components/layout/header/header.tsx
'use client';

import { useAuthStore } from '@/stores/auth-store';
import { useUIStore } from '@/stores/ui-store';
import { Button } from '@/components/ui/button';
import { UserNav } from './user-nav';
import { ThemeToggle } from './theme-toggle';
import { Menu, Bell } from 'lucide-react';

export function Header() {
  const { toggleSidebar } = useUIStore();
  const { user } = useAuthStore();

  return (
    <header className="flex h-16 items-center justify-between border-b bg-background px-4 lg:px-6">
      {/* Mobile menu button */}
      <Button
        variant="ghost"
        size="icon"
        className="lg:hidden"
        onClick={toggleSidebar}
      >
        <Menu className="h-5 w-5" />
      </Button>

      {/* Search - optional */}
      <div className="flex-1" />

      {/* Right side actions */}
      <div className="flex items-center gap-2">
        {/* Notifications */}
        <Button variant="ghost" size="icon" className="relative">
          <Bell className="h-5 w-5" />
          <span className="absolute right-1 top-1 h-2 w-2 rounded-full bg-red-500" />
        </Button>

        <ThemeToggle />
        <UserNav user={user} />
      </div>
    </header>
  );
}
```

## 5.4. User Navigation

```typescript
// src/components/layout/header/user-nav.tsx
'use client';

import { useRouter } from 'next/navigation';
import { Avatar, AvatarFallback, AvatarImage } from '@/components/ui/avatar';
import {
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuItem,
  DropdownMenuLabel,
  DropdownMenuSeparator,
  DropdownMenuTrigger,
} from '@/components/ui/dropdown-menu';
import { useAuthStore } from '@/stores/auth-store';
import { User, Settings, LogOut } from 'lucide-react';
import type { User as UserType } from '@/types/user';

interface UserNavProps {
  user: UserType | null;
}

export function UserNav({ user }: UserNavProps) {
  const router = useRouter();
  const { logout } = useAuthStore();

  const handleLogout = () => {
    logout();
    router.push('/login');
  };

  const initials = user?.name
    ?.split(' ')
    .map((n) => n[0])
    .join('')
    .toUpperCase()
    .slice(0, 2);

  return (
    <DropdownMenu>
      <DropdownMenuTrigger asChild>
        <button className="flex items-center gap-2 rounded-full focus:outline-none focus:ring-2 focus:ring-primary focus:ring-offset-2">
          <Avatar className="h-8 w-8">
            <AvatarImage src={user?.avatar || undefined} alt={user?.name} />
            <AvatarFallback>{initials || 'U'}</AvatarFallback>
          </Avatar>
        </button>
      </DropdownMenuTrigger>
      <DropdownMenuContent align="end" className="w-56">
        <DropdownMenuLabel>
          <div className="flex flex-col space-y-1">
            <p className="text-sm font-medium">{user?.name}</p>
            <p className="text-xs text-muted-foreground">{user?.email}</p>
          </div>
        </DropdownMenuLabel>
        <DropdownMenuSeparator />
        <DropdownMenuItem onClick={() => router.push('/settings/profile')}>
          <User className="mr-2 h-4 w-4" />
          Hồ sơ cá nhân
        </DropdownMenuItem>
        <DropdownMenuItem onClick={() => router.push('/settings')}>
          <Settings className="mr-2 h-4 w-4" />
          Cài đặt
        </DropdownMenuItem>
        <DropdownMenuSeparator />
        <DropdownMenuItem onClick={handleLogout} className="text-red-600">
          <LogOut className="mr-2 h-4 w-4" />
          Đăng xuất
        </DropdownMenuItem>
      </DropdownMenuContent>
    </DropdownMenu>
  );
}
```

## 5.5. Theme Toggle

```typescript
// src/components/layout/header/theme-toggle.tsx
'use client';

import { useTheme } from 'next-themes';
import { Button } from '@/components/ui/button';
import {
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuItem,
  DropdownMenuTrigger,
} from '@/components/ui/dropdown-menu';
import { Sun, Moon, Monitor } from 'lucide-react';

export function ThemeToggle() {
  const { theme, setTheme } = useTheme();

  return (
    <DropdownMenu>
      <DropdownMenuTrigger asChild>
        <Button variant="ghost" size="icon">
          <Sun className="h-5 w-5 rotate-0 scale-100 transition-all dark:-rotate-90 dark:scale-0" />
          <Moon className="absolute h-5 w-5 rotate-90 scale-0 transition-all dark:rotate-0 dark:scale-100" />
          <span className="sr-only">Toggle theme</span>
        </Button>
      </DropdownMenuTrigger>
      <DropdownMenuContent align="end">
        <DropdownMenuItem onClick={() => setTheme('light')}>
          <Sun className="mr-2 h-4 w-4" />
          Sáng
        </DropdownMenuItem>
        <DropdownMenuItem onClick={() => setTheme('dark')}>
          <Moon className="mr-2 h-4 w-4" />
          Tối
        </DropdownMenuItem>
        <DropdownMenuItem onClick={() => setTheme('system')}>
          <Monitor className="mr-2 h-4 w-4" />
          Hệ thống
        </DropdownMenuItem>
      </DropdownMenuContent>
    </DropdownMenu>
  );
}
```

## 5.6. Dashboard Layout

```typescript
// src/app/(dashboard)/layout.tsx
import { Sidebar } from '@/components/layout/sidebar/sidebar';
import { Header } from '@/components/layout/header/header';

export default function DashboardLayout({
  children,
}: {
  children: React.ReactNode;
}) {
  return (
    <div className="flex h-screen">
      {/* Desktop Sidebar */}
      <div className="hidden lg:flex">
        <Sidebar />
      </div>

      {/* Main Content */}
      <div className="flex flex-1 flex-col overflow-hidden">
        <Header />
        <main className="flex-1 overflow-y-auto bg-muted/30 p-4 lg:p-6">
          {children}
        </main>
      </div>
    </div>
  );
}
```

---

# PHASE 6: CUSTOM HOOKS (React Query)

## 6.1. Students Hook

```typescript
// src/hooks/use-students.ts
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { api } from '@/lib/api/client';
import { API_ENDPOINTS } from '@/lib/api/endpoints';
import { toast } from '@/components/ui/use-toast';
import type {
  Student,
  CreateStudentRequest,
  UpdateStudentRequest,
} from '@/types/student';
import type { ApiResponse, PageResponse } from '@/types/api';

interface UseStudentsParams {
  page?: number;
  size?: number;
  search?: string;
  status?: string;
}

export function useStudents(params: UseStudentsParams = {}) {
  const { page = 0, size = 20, search, status } = params;

  return useQuery({
    queryKey: ['students', { page, size, search, status }],
    queryFn: () =>
      api.get<PageResponse<Student>>(API_ENDPOINTS.students.list, {
        page,
        size,
        search,
        status,
      }),
  });
}

export function useStudent(id: number) {
  return useQuery({
    queryKey: ['students', id],
    queryFn: () =>
      api.get<ApiResponse<Student>>(API_ENDPOINTS.students.detail(id)),
    enabled: !!id,
  });
}

export function useCreateStudent() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: (data: CreateStudentRequest) =>
      api.post<ApiResponse<Student>>(API_ENDPOINTS.students.create, data),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['students'] });
      toast({
        title: 'Thành công',
        description: 'Đã tạo học viên mới',
      });
    },
    onError: (error: any) => {
      toast({
        title: 'Lỗi',
        description: error.response?.data?.error?.message || 'Không thể tạo học viên',
        variant: 'destructive',
      });
    },
  });
}

export function useUpdateStudent() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: ({ id, data }: { id: number; data: UpdateStudentRequest }) =>
      api.put<ApiResponse<Student>>(API_ENDPOINTS.students.update(id), data),
    onSuccess: (_, variables) => {
      queryClient.invalidateQueries({ queryKey: ['students'] });
      queryClient.invalidateQueries({ queryKey: ['students', variables.id] });
      toast({
        title: 'Thành công',
        description: 'Đã cập nhật học viên',
      });
    },
    onError: (error: any) => {
      toast({
        title: 'Lỗi',
        description: error.response?.data?.error?.message || 'Không thể cập nhật',
        variant: 'destructive',
      });
    },
  });
}

export function useDeleteStudent() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: (id: number) =>
      api.delete(API_ENDPOINTS.students.delete(id)),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['students'] });
      toast({
        title: 'Thành công',
        description: 'Đã xóa học viên',
      });
    },
    onError: (error: any) => {
      toast({
        title: 'Lỗi',
        description: error.response?.data?.error?.message || 'Không thể xóa học viên',
        variant: 'destructive',
      });
    },
  });
}
```

## 6.2. Classes Hook

```typescript
// src/hooks/use-classes.ts
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { api } from '@/lib/api/client';
import { API_ENDPOINTS } from '@/lib/api/endpoints';
import { toast } from '@/components/ui/use-toast';
import type { Class } from '@/types/class';
import type { ApiResponse, PageResponse } from '@/types/api';

interface UseClassesParams {
  page?: number;
  size?: number;
  search?: string;
  status?: string;
  teacherId?: number;
}

export function useClasses(params: UseClassesParams = {}) {
  const { page = 0, size = 20, search, status, teacherId } = params;

  return useQuery({
    queryKey: ['classes', { page, size, search, status, teacherId }],
    queryFn: () =>
      api.get<PageResponse<Class>>(API_ENDPOINTS.classes.list, {
        page,
        size,
        search,
        status,
        teacherId,
      }),
  });
}

export function useClass(id: number) {
  return useQuery({
    queryKey: ['classes', id],
    queryFn: () =>
      api.get<ApiResponse<Class>>(API_ENDPOINTS.classes.detail(id)),
    enabled: !!id,
  });
}

export function useClassStudents(classId: number) {
  return useQuery({
    queryKey: ['classes', classId, 'students'],
    queryFn: () =>
      api.get<PageResponse<any>>(API_ENDPOINTS.classes.students(classId)),
    enabled: !!classId,
  });
}

export function useClassSessions(classId: number) {
  return useQuery({
    queryKey: ['classes', classId, 'sessions'],
    queryFn: () =>
      api.get<PageResponse<any>>(API_ENDPOINTS.classes.sessions(classId)),
    enabled: !!classId,
  });
}
```

## 6.3. Attendance Hook

```typescript
// src/hooks/use-attendance.ts
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { api } from '@/lib/api/client';
import { API_ENDPOINTS } from '@/lib/api/endpoints';
import { toast } from '@/components/ui/use-toast';
import type { Attendance, MarkAttendanceRequest } from '@/types/attendance';

interface UseAttendanceParams {
  classId: number;
  date?: string;
  month?: string;
}

export function useAttendance({ classId, date, month }: UseAttendanceParams) {
  return useQuery({
    queryKey: ['attendance', classId, { date, month }],
    queryFn: () =>
      api.get<{ content: Attendance[] }>(API_ENDPOINTS.classes.attendance(classId), {
        date,
        month,
      }),
    enabled: !!classId,
  });
}

interface MarkAttendanceData {
  classId: number;
  sessionId: number;
  date: string;
  records: MarkAttendanceRequest[];
}

export function useMarkAttendance() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: (data: MarkAttendanceData) =>
      api.post(API_ENDPOINTS.classes.attendance(data.classId), {
        sessionId: data.sessionId,
        date: data.date,
        records: data.records,
      }),
    onSuccess: (_, variables) => {
      queryClient.invalidateQueries({
        queryKey: ['attendance', variables.classId],
      });
      toast({
        title: 'Thành công',
        description: 'Đã lưu điểm danh',
      });
    },
    onError: (error: any) => {
      toast({
        title: 'Lỗi',
        description: error.response?.data?.error?.message || 'Không thể lưu điểm danh',
        variant: 'destructive',
      });
    },
  });
}
```

## 6.4. Auth Hook

```typescript
// src/hooks/use-auth.ts
import { useMutation } from '@tanstack/react-query';
import { useRouter } from 'next/navigation';
import { api } from '@/lib/api/client';
import { API_ENDPOINTS } from '@/lib/api/endpoints';
import { useAuthStore } from '@/stores/auth-store';
import { toast } from '@/components/ui/use-toast';
import type { AuthResponse } from '@/types/user';

interface LoginCredentials {
  email: string;
  password: string;
}

export function useLogin() {
  const router = useRouter();
  const { setAuth } = useAuthStore();

  return useMutation({
    mutationFn: (credentials: LoginCredentials) =>
      api.post<{ data: AuthResponse }>(API_ENDPOINTS.auth.login, credentials),
    onSuccess: (response) => {
      const { user, accessToken, refreshToken } = response.data;
      setAuth(user, accessToken, refreshToken);
      router.push('/');
      toast({
        title: 'Đăng nhập thành công',
        description: `Chào mừng ${user.name}`,
      });
    },
    onError: (error: any) => {
      toast({
        title: 'Đăng nhập thất bại',
        description:
          error.response?.data?.error?.message || 'Email hoặc mật khẩu không đúng',
        variant: 'destructive',
      });
    },
  });
}

export function useLogout() {
  const router = useRouter();
  const { logout } = useAuthStore();

  return useMutation({
    mutationFn: () => api.post(API_ENDPOINTS.auth.logout),
    onSettled: () => {
      logout();
      router.push('/login');
    },
  });
}
```

---

# PHASE 7: FORM COMPONENTS

## 7.1. Zod Validations

```typescript
// src/lib/validations/student.ts
import { z } from 'zod';

export const studentSchema = z.object({
  name: z
    .string()
    .min(2, 'Tên phải có ít nhất 2 ký tự')
    .max(100, 'Tên không được quá 100 ký tự'),
  email: z.string().email('Email không hợp lệ').optional().or(z.literal('')),
  phone: z
    .string()
    .regex(/^0\d{9}$/, 'Số điện thoại không hợp lệ (VD: 0901234567)')
    .optional()
    .or(z.literal('')),
  dateOfBirth: z.string().optional().or(z.literal('')),
  gender: z.enum(['MALE', 'FEMALE', 'OTHER']).optional(),
  address: z.string().max(1000, 'Địa chỉ không được quá 1000 ký tự').optional(),
  note: z.string().optional(),
});

export type StudentFormData = z.infer<typeof studentSchema>;

// src/lib/validations/auth.ts
export const loginSchema = z.object({
  email: z.string().email('Email không hợp lệ'),
  password: z.string().min(6, 'Mật khẩu phải có ít nhất 6 ký tự'),
});

export type LoginFormData = z.infer<typeof loginSchema>;
```

## 7.2. Student Form

```typescript
// src/components/forms/student-form.tsx
'use client';

import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { studentSchema, StudentFormData } from '@/lib/validations/student';
import { Button } from '@/components/ui/button';
import {
  Form,
  FormControl,
  FormField,
  FormItem,
  FormLabel,
  FormMessage,
} from '@/components/ui/form';
import { Input } from '@/components/ui/input';
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from '@/components/ui/select';
import { Textarea } from '@/components/ui/textarea';
import { Calendar } from '@/components/ui/calendar';
import { Popover, PopoverContent, PopoverTrigger } from '@/components/ui/popover';
import { CalendarIcon, Loader2 } from 'lucide-react';
import { format } from 'date-fns';
import { vi } from 'date-fns/locale';
import { cn } from '@/lib/utils';

interface StudentFormProps {
  defaultValues?: Partial<StudentFormData>;
  onSubmit: (data: StudentFormData) => void;
  isLoading?: boolean;
}

const genderOptions = [
  { value: 'MALE', label: 'Nam' },
  { value: 'FEMALE', label: 'Nữ' },
  { value: 'OTHER', label: 'Khác' },
];

export function StudentForm({ defaultValues, onSubmit, isLoading }: StudentFormProps) {
  const form = useForm<StudentFormData>({
    resolver: zodResolver(studentSchema),
    defaultValues: {
      name: '',
      email: '',
      phone: '',
      dateOfBirth: '',
      gender: undefined,
      address: '',
      note: '',
      ...defaultValues,
    },
  });

  return (
    <Form {...form}>
      <form onSubmit={form.handleSubmit(onSubmit)} className="space-y-6">
        <div className="grid grid-cols-1 gap-6 md:grid-cols-2">
          {/* Name */}
          <FormField
            control={form.control}
            name="name"
            render={({ field }) => (
              <FormItem>
                <FormLabel>Họ và tên *</FormLabel>
                <FormControl>
                  <Input placeholder="Nguyễn Văn A" {...field} />
                </FormControl>
                <FormMessage />
              </FormItem>
            )}
          />

          {/* Email */}
          <FormField
            control={form.control}
            name="email"
            render={({ field }) => (
              <FormItem>
                <FormLabel>Email</FormLabel>
                <FormControl>
                  <Input type="email" placeholder="email@example.com" {...field} />
                </FormControl>
                <FormMessage />
              </FormItem>
            )}
          />

          {/* Phone */}
          <FormField
            control={form.control}
            name="phone"
            render={({ field }) => (
              <FormItem>
                <FormLabel>Số điện thoại</FormLabel>
                <FormControl>
                  <Input placeholder="0901234567" {...field} />
                </FormControl>
                <FormMessage />
              </FormItem>
            )}
          />

          {/* Date of Birth */}
          <FormField
            control={form.control}
            name="dateOfBirth"
            render={({ field }) => (
              <FormItem>
                <FormLabel>Ngày sinh</FormLabel>
                <Popover>
                  <PopoverTrigger asChild>
                    <FormControl>
                      <Button
                        variant="outline"
                        className={cn(
                          'w-full pl-3 text-left font-normal',
                          !field.value && 'text-muted-foreground'
                        )}
                      >
                        {field.value ? (
                          format(new Date(field.value), 'dd/MM/yyyy', { locale: vi })
                        ) : (
                          <span>Chọn ngày sinh</span>
                        )}
                        <CalendarIcon className="ml-auto h-4 w-4 opacity-50" />
                      </Button>
                    </FormControl>
                  </PopoverTrigger>
                  <PopoverContent className="w-auto p-0" align="start">
                    <Calendar
                      mode="single"
                      selected={field.value ? new Date(field.value) : undefined}
                      onSelect={(date) =>
                        field.onChange(date ? format(date, 'yyyy-MM-dd') : '')
                      }
                      disabled={(date) => date > new Date()}
                      initialFocus
                    />
                  </PopoverContent>
                </Popover>
                <FormMessage />
              </FormItem>
            )}
          />

          {/* Gender */}
          <FormField
            control={form.control}
            name="gender"
            render={({ field }) => (
              <FormItem>
                <FormLabel>Giới tính</FormLabel>
                <Select onValueChange={field.onChange} value={field.value}>
                  <FormControl>
                    <SelectTrigger>
                      <SelectValue placeholder="Chọn giới tính" />
                    </SelectTrigger>
                  </FormControl>
                  <SelectContent>
                    {genderOptions.map((option) => (
                      <SelectItem key={option.value} value={option.value}>
                        {option.label}
                      </SelectItem>
                    ))}
                  </SelectContent>
                </Select>
                <FormMessage />
              </FormItem>
            )}
          />
        </div>

        {/* Address */}
        <FormField
          control={form.control}
          name="address"
          render={({ field }) => (
            <FormItem>
              <FormLabel>Địa chỉ</FormLabel>
              <FormControl>
                <Textarea
                  placeholder="Số nhà, đường, phường/xã, quận/huyện, tỉnh/thành"
                  {...field}
                />
              </FormControl>
              <FormMessage />
            </FormItem>
          )}
        />

        {/* Note */}
        <FormField
          control={form.control}
          name="note"
          render={({ field }) => (
            <FormItem>
              <FormLabel>Ghi chú</FormLabel>
              <FormControl>
                <Textarea placeholder="Ghi chú thêm về học viên..." {...field} />
              </FormControl>
              <FormMessage />
            </FormItem>
          )}
        />

        {/* Submit */}
        <div className="flex justify-end gap-4">
          <Button type="button" variant="outline">
            Hủy
          </Button>
          <Button type="submit" disabled={isLoading}>
            {isLoading && <Loader2 className="mr-2 h-4 w-4 animate-spin" />}
            {isLoading ? 'Đang lưu...' : 'Lưu'}
          </Button>
        </div>
      </form>
    </Form>
  );
}
```

---

# PHASE 8: PAGE IMPLEMENTATIONS

## 8.1. Login Page

```typescript
// src/app/(auth)/login/page.tsx
'use client';

import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { loginSchema, LoginFormData } from '@/lib/validations/auth';
import { useLogin } from '@/hooks/use-auth';
import { Button } from '@/components/ui/button';
import {
  Form,
  FormControl,
  FormField,
  FormItem,
  FormLabel,
  FormMessage,
} from '@/components/ui/form';
import { Input } from '@/components/ui/input';
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card';
import { Loader2 } from 'lucide-react';
import Link from 'next/link';

export default function LoginPage() {
  const { mutate: login, isPending } = useLogin();

  const form = useForm<LoginFormData>({
    resolver: zodResolver(loginSchema),
    defaultValues: {
      email: '',
      password: '',
    },
  });

  const onSubmit = (data: LoginFormData) => {
    login(data);
  };

  return (
    <div className="flex min-h-screen items-center justify-center bg-muted/30 p-4">
      <Card className="w-full max-w-md">
        <CardHeader className="space-y-1 text-center">
          <CardTitle className="text-2xl font-bold">Đăng nhập</CardTitle>
          <CardDescription>
            Nhập email và mật khẩu để truy cập hệ thống
          </CardDescription>
        </CardHeader>
        <CardContent>
          <Form {...form}>
            <form onSubmit={form.handleSubmit(onSubmit)} className="space-y-4">
              <FormField
                control={form.control}
                name="email"
                render={({ field }) => (
                  <FormItem>
                    <FormLabel>Email</FormLabel>
                    <FormControl>
                      <Input
                        type="email"
                        placeholder="email@example.com"
                        {...field}
                      />
                    </FormControl>
                    <FormMessage />
                  </FormItem>
                )}
              />

              <FormField
                control={form.control}
                name="password"
                render={({ field }) => (
                  <FormItem>
                    <FormLabel>Mật khẩu</FormLabel>
                    <FormControl>
                      <Input type="password" placeholder="••••••••" {...field} />
                    </FormControl>
                    <FormMessage />
                  </FormItem>
                )}
              />

              <div className="flex items-center justify-end">
                <Link
                  href="/forgot-password"
                  className="text-sm text-primary hover:underline"
                >
                  Quên mật khẩu?
                </Link>
              </div>

              <Button type="submit" className="w-full" disabled={isPending}>
                {isPending && <Loader2 className="mr-2 h-4 w-4 animate-spin" />}
                Đăng nhập
              </Button>
            </form>
          </Form>
        </CardContent>
      </Card>
    </div>
  );
}
```

## 8.2. Dashboard Page

```typescript
// src/app/(dashboard)/page.tsx
'use client';

import { PageHeader } from '@/components/shared/page-header';
import { StatsCard } from '@/components/shared/stats-card';
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card';
import { useAuthStore } from '@/stores/auth-store';
import { Users, GraduationCap, ClipboardCheck, Receipt } from 'lucide-react';

export default function DashboardPage() {
  const { user } = useAuthStore();

  return (
    <div className="space-y-6">
      <PageHeader
        title={`Xin chào, ${user?.name}`}
        description="Tổng quan hoạt động của trung tâm"
      />

      {/* Stats Cards */}
      <div className="grid gap-4 md:grid-cols-2 lg:grid-cols-4">
        <StatsCard
          title="Tổng học viên"
          value="256"
          description="so với tháng trước"
          icon={Users}
          trend={{ value: 12, isPositive: true }}
        />
        <StatsCard
          title="Lớp đang học"
          value="18"
          description="lớp đang hoạt động"
          icon={GraduationCap}
        />
        <StatsCard
          title="Điểm danh hôm nay"
          value="85%"
          description="tỷ lệ có mặt"
          icon={ClipboardCheck}
        />
        <StatsCard
          title="Doanh thu tháng"
          value="45.2M"
          description="so với tháng trước"
          icon={Receipt}
          trend={{ value: 8, isPositive: true }}
        />
      </div>

      {/* Recent Activity & Quick Actions */}
      <div className="grid gap-4 md:grid-cols-2">
        <Card>
          <CardHeader>
            <CardTitle>Hoạt động gần đây</CardTitle>
          </CardHeader>
          <CardContent>
            {/* Activity list */}
            <p className="text-sm text-muted-foreground">
              Chưa có hoạt động nào
            </p>
          </CardContent>
        </Card>

        <Card>
          <CardHeader>
            <CardTitle>Lịch hôm nay</CardTitle>
          </CardHeader>
          <CardContent>
            {/* Today's schedule */}
            <p className="text-sm text-muted-foreground">
              Không có lịch học hôm nay
            </p>
          </CardContent>
        </Card>
      </div>
    </div>
  );
}
```

## 8.3. Students List Page

```typescript
// src/app/(dashboard)/students/page.tsx
'use client';

import { useState } from 'react';
import { useRouter } from 'next/navigation';
import { useStudents, useDeleteStudent } from '@/hooks/use-students';
import { PageHeader } from '@/components/shared/page-header';
import { EmptyState } from '@/components/shared/empty-state';
import { StatusBadge } from '@/components/shared/status-badge';
import { ConfirmDialog } from '@/components/shared/confirm-dialog';
import { Button } from '@/components/ui/button';
import { Input } from '@/components/ui/input';
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from '@/components/ui/select';
import {
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from '@/components/ui/table';
import {
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuItem,
  DropdownMenuTrigger,
} from '@/components/ui/dropdown-menu';
import { Card, CardContent } from '@/components/ui/card';
import { Skeleton } from '@/components/ui/skeleton';
import { Plus, Search, MoreHorizontal, Eye, Edit, Trash2, Users } from 'lucide-react';
import { useDebounce } from '@/hooks/use-debounce';

const statusOptions = [
  { value: 'all', label: 'Tất cả trạng thái' },
  { value: 'ACTIVE', label: 'Đang học' },
  { value: 'INACTIVE', label: 'Tạm nghỉ' },
  { value: 'GRADUATED', label: 'Đã tốt nghiệp' },
  { value: 'DROPPED', label: 'Đã nghỉ học' },
];

export default function StudentsPage() {
  const router = useRouter();
  const [search, setSearch] = useState('');
  const [status, setStatus] = useState('all');
  const [page, setPage] = useState(0);
  const [deleteId, setDeleteId] = useState<number | null>(null);

  const debouncedSearch = useDebounce(search, 300);

  const { data, isLoading } = useStudents({
    page,
    search: debouncedSearch || undefined,
    status: status === 'all' ? undefined : status,
  });

  const { mutate: deleteStudent, isPending: isDeleting } = useDeleteStudent();

  const handleDelete = () => {
    if (deleteId) {
      deleteStudent(deleteId, {
        onSuccess: () => setDeleteId(null),
      });
    }
  };

  return (
    <div className="space-y-6">
      <PageHeader
        title="Học viên"
        description="Quản lý danh sách học viên của trung tâm"
        actions={
          <Button onClick={() => router.push('/students/new')}>
            <Plus className="mr-2 h-4 w-4" />
            Thêm học viên
          </Button>
        }
      />

      {/* Filters */}
      <div className="flex flex-col gap-4 sm:flex-row">
        <div className="relative flex-1">
          <Search className="absolute left-3 top-1/2 h-4 w-4 -translate-y-1/2 text-muted-foreground" />
          <Input
            placeholder="Tìm theo tên, email, số điện thoại..."
            value={search}
            onChange={(e) => setSearch(e.target.value)}
            className="pl-10"
          />
        </div>
        <Select value={status} onValueChange={setStatus}>
          <SelectTrigger className="w-full sm:w-[180px]">
            <SelectValue placeholder="Trạng thái" />
          </SelectTrigger>
          <SelectContent>
            {statusOptions.map((option) => (
              <SelectItem key={option.value} value={option.value}>
                {option.label}
              </SelectItem>
            ))}
          </SelectContent>
        </Select>
      </div>

      {/* Table */}
      <Card>
        <CardContent className="p-0">
          {isLoading ? (
            <div className="p-4">
              {[...Array(5)].map((_, i) => (
                <Skeleton key={i} className="mb-4 h-12 w-full" />
              ))}
            </div>
          ) : !data?.content?.length ? (
            <EmptyState
              icon={Users}
              title="Chưa có học viên nào"
              description="Thêm học viên đầu tiên để bắt đầu"
              action={
                <Button onClick={() => router.push('/students/new')}>
                  <Plus className="mr-2 h-4 w-4" />
                  Thêm học viên
                </Button>
              }
            />
          ) : (
            <Table>
              <TableHeader>
                <TableRow>
                  <TableHead>Họ tên</TableHead>
                  <TableHead>Email</TableHead>
                  <TableHead>Số điện thoại</TableHead>
                  <TableHead>Trạng thái</TableHead>
                  <TableHead className="w-[50px]"></TableHead>
                </TableRow>
              </TableHeader>
              <TableBody>
                {data.content.map((student) => (
                  <TableRow key={student.id}>
                    <TableCell className="font-medium">{student.name}</TableCell>
                    <TableCell>{student.email || '-'}</TableCell>
                    <TableCell>{student.phone || '-'}</TableCell>
                    <TableCell>
                      <StatusBadge status={student.status} />
                    </TableCell>
                    <TableCell>
                      <DropdownMenu>
                        <DropdownMenuTrigger asChild>
                          <Button variant="ghost" size="icon">
                            <MoreHorizontal className="h-4 w-4" />
                          </Button>
                        </DropdownMenuTrigger>
                        <DropdownMenuContent align="end">
                          <DropdownMenuItem
                            onClick={() => router.push(`/students/${student.id}`)}
                          >
                            <Eye className="mr-2 h-4 w-4" />
                            Xem chi tiết
                          </DropdownMenuItem>
                          <DropdownMenuItem
                            onClick={() => router.push(`/students/${student.id}/edit`)}
                          >
                            <Edit className="mr-2 h-4 w-4" />
                            Chỉnh sửa
                          </DropdownMenuItem>
                          <DropdownMenuItem
                            onClick={() => setDeleteId(student.id)}
                            className="text-red-600"
                          >
                            <Trash2 className="mr-2 h-4 w-4" />
                            Xóa
                          </DropdownMenuItem>
                        </DropdownMenuContent>
                      </DropdownMenu>
                    </TableCell>
                  </TableRow>
                ))}
              </TableBody>
            </Table>
          )}
        </CardContent>
      </Card>

      {/* Pagination */}
      {data && data.totalPages > 1 && (
        <div className="flex items-center justify-between">
          <p className="text-sm text-muted-foreground">
            Hiển thị {data.content.length} / {data.totalElements} học viên
          </p>
          <div className="flex gap-2">
            <Button
              variant="outline"
              size="sm"
              disabled={data.first}
              onClick={() => setPage((p) => p - 1)}
            >
              Trước
            </Button>
            <Button
              variant="outline"
              size="sm"
              disabled={data.last}
              onClick={() => setPage((p) => p + 1)}
            >
              Sau
            </Button>
          </div>
        </div>
      )}

      {/* Delete Confirmation */}
      <ConfirmDialog
        open={!!deleteId}
        onOpenChange={(open) => !open && setDeleteId(null)}
        title="Xác nhận xóa"
        description="Bạn có chắc chắn muốn xóa học viên này? Hành động này không thể hoàn tác."
        confirmText="Xóa"
        variant="destructive"
        onConfirm={handleDelete}
        isLoading={isDeleting}
      />
    </div>
  );
}
```

## 8.4. Create Student Page

```typescript
// src/app/(dashboard)/students/new/page.tsx
'use client';

import { useRouter } from 'next/navigation';
import { useCreateStudent } from '@/hooks/use-students';
import { PageHeader } from '@/components/shared/page-header';
import { StudentForm } from '@/components/forms/student-form';
import { Card, CardContent } from '@/components/ui/card';
import { StudentFormData } from '@/lib/validations/student';

export default function CreateStudentPage() {
  const router = useRouter();
  const { mutate: createStudent, isPending } = useCreateStudent();

  const handleSubmit = (data: StudentFormData) => {
    createStudent(
      {
        name: data.name,
        email: data.email || undefined,
        phone: data.phone || undefined,
        dateOfBirth: data.dateOfBirth || undefined,
        gender: data.gender,
        address: data.address || undefined,
        note: data.note || undefined,
      },
      {
        onSuccess: () => router.push('/students'),
      }
    );
  };

  return (
    <div className="space-y-6">
      <PageHeader
        title="Thêm học viên mới"
        description="Nhập thông tin học viên để thêm vào hệ thống"
      />

      <Card>
        <CardContent className="pt-6">
          <StudentForm onSubmit={handleSubmit} isLoading={isPending} />
        </CardContent>
      </Card>
    </div>
  );
}
```

---

# PHASE 9: TESTING

## 9.1. Test Setup

```bash
# Install testing dependencies
pnpm add -D vitest @vitejs/plugin-react @testing-library/react @testing-library/jest-dom
pnpm add -D @testing-library/user-event
pnpm add -D msw
pnpm add -D playwright @playwright/test
```

## 9.2. Vitest Configuration

```typescript
// vitest.config.ts
import { defineConfig } from 'vitest/config';
import react from '@vitejs/plugin-react';
import path from 'path';

export default defineConfig({
  plugins: [react()],
  test: {
    environment: 'jsdom',
    globals: true,
    setupFiles: ['./src/test/setup.ts'],
    include: ['src/**/*.{test,spec}.{ts,tsx}'],
    coverage: {
      reporter: ['text', 'html'],
      exclude: ['node_modules/', 'src/test/'],
    },
  },
  resolve: {
    alias: {
      '@': path.resolve(__dirname, './src'),
    },
  },
});
```

## 9.3. Test Example - Component

```typescript
// src/components/shared/__tests__/status-badge.test.tsx
import { render, screen } from '@testing-library/react';
import { StatusBadge } from '../status-badge';

describe('StatusBadge', () => {
  it('renders with correct text', () => {
    render(<StatusBadge status="ACTIVE" />);
    expect(screen.getByText('ACTIVE')).toBeInTheDocument();
  });

  it('applies success variant for ACTIVE status', () => {
    render(<StatusBadge status="ACTIVE" />);
    const badge = screen.getByText('ACTIVE');
    expect(badge).toHaveClass('bg-green-100');
  });

  it('applies error variant for ABSENT status', () => {
    render(<StatusBadge status="ABSENT" />);
    const badge = screen.getByText('ABSENT');
    expect(badge).toHaveClass('bg-red-100');
  });

  it('allows custom variant override', () => {
    render(<StatusBadge status="CUSTOM" variant="warning" />);
    const badge = screen.getByText('CUSTOM');
    expect(badge).toHaveClass('bg-yellow-100');
  });
});
```

## 9.4. Test Example - Hook with MSW

```typescript
// src/hooks/__tests__/use-students.test.tsx
import { renderHook, waitFor } from '@testing-library/react';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { http, HttpResponse } from 'msw';
import { setupServer } from 'msw/node';
import { useStudents } from '../use-students';

const server = setupServer(
  http.get('/api/v1/students', () => {
    return HttpResponse.json({
      content: [
        { id: 1, name: 'Student 1', status: 'ACTIVE' },
        { id: 2, name: 'Student 2', status: 'ACTIVE' },
      ],
      totalElements: 2,
      totalPages: 1,
      first: true,
      last: true,
    });
  })
);

beforeAll(() => server.listen());
afterEach(() => server.resetHandlers());
afterAll(() => server.close());

const wrapper = ({ children }: { children: React.ReactNode }) => {
  const queryClient = new QueryClient({
    defaultOptions: { queries: { retry: false } },
  });
  return (
    <QueryClientProvider client={queryClient}>{children}</QueryClientProvider>
  );
};

describe('useStudents', () => {
  it('fetches students successfully', async () => {
    const { result } = renderHook(() => useStudents(), { wrapper });

    await waitFor(() => expect(result.current.isSuccess).toBe(true));

    expect(result.current.data?.content).toHaveLength(2);
    expect(result.current.data?.content[0].name).toBe('Student 1');
  });

  it('handles search parameter', async () => {
    const { result } = renderHook(
      () => useStudents({ search: 'test' }),
      { wrapper }
    );

    await waitFor(() => expect(result.current.isSuccess).toBe(true));
  });
});
```

---

# IMPLEMENTATION CHECKLIST

## Phase 1: Project Setup
- [ ] Create Next.js project with TypeScript
- [ ] Install and configure Shadcn/UI
- [ ] Setup Tailwind CSS with custom theme
- [ ] Configure environment variables
- [ ] Setup project folder structure

## Phase 2: Core Infrastructure
- [ ] API client with interceptors
- [ ] API endpoints configuration
- [ ] TypeScript types for all entities
- [ ] Zustand stores (auth, ui)

## Phase 3: Providers & Root Layout
- [ ] Query Provider (React Query)
- [ ] Theme Provider (next-themes)
- [ ] Auth Provider (protected routes)
- [ ] Root layout with providers

## Phase 4: Shared Components
- [ ] PageHeader
- [ ] LoadingSpinner
- [ ] StatusBadge
- [ ] EmptyState
- [ ] StatsCard
- [ ] ConfirmDialog

## Phase 5: Layout Components
- [ ] Sidebar with navigation
- [ ] Header with user nav
- [ ] Theme toggle
- [ ] Dashboard layout

## Phase 6: Custom Hooks
- [ ] useStudents (CRUD + search)
- [ ] useClasses (CRUD + sessions)
- [ ] useAttendance
- [ ] useAuth (login/logout)
- [ ] useBranding
- [ ] useDebounce

## Phase 7: Form Components
- [ ] Zod validation schemas
- [ ] StudentForm
- [ ] ClassForm
- [ ] AttendanceForm

## Phase 8: Pages
- [ ] Login page
- [ ] Dashboard page
- [ ] Students list page
- [ ] Student detail page
- [ ] Create/Edit student pages
- [ ] Classes pages
- [ ] Attendance pages
- [ ] Billing pages
- [ ] Settings pages
- [ ] Parent portal pages

## Phase 9: Testing
- [ ] Vitest setup
- [ ] Component tests
- [ ] Hook tests with MSW
- [ ] E2E tests with Playwright

---

# NOTES FOR CLAUDE

1. **Implement pages incrementally** - Start with Login, Dashboard, then Students module
2. **Follow ui-components.md** for design tokens and component patterns
3. **Use theme-system.md** for CSS variables and theming
4. **API integration** follows api-design.md endpoints
5. **TypeScript types** must match backend DTOs
6. **Form validation** uses Zod with Vietnamese error messages
7. **State management**: React Query for server state, Zustand for client state
8. **Testing**: Write tests for components and hooks

## Commands to Run

```bash
# Development
pnpm dev

# Build
pnpm build

# Run tests
pnpm test

# Run E2E tests
pnpm test:e2e

# Lint
pnpm lint

# Format
pnpm format
```

## Key Patterns

1. **Data fetching**: Always use React Query hooks
2. **Forms**: React Hook Form + Zod
3. **Styling**: Tailwind CSS + cn() utility
4. **Components**: Shadcn/UI base + custom shared
5. **Navigation**: Next.js App Router
6. **Auth**: Zustand persist + API interceptors
