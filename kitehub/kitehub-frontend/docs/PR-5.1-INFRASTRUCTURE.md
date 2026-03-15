# PR 5.1: Project Setup & Shared Infrastructure

## Overview
Foundation setup for KiteHub Frontend including Next.js 15 configuration, API client, state management, and shared components.

## Tech Stack

### Core Framework
- **Next.js 15** - App Router with TypeScript strict mode
- **React 19** - Latest React with Server Components support
- **TypeScript 5.3** - Strict type checking enabled

### UI & Styling
- **Shadcn/UI** - Accessible component library
- **Tailwind CSS 3.4** - Utility-first CSS framework
- **Radix UI** - Unstyled accessible components
- **Lucide React** - Icon library

### State & Data
- **Zustand** - Lightweight state management
- **TanStack Query (React Query)** - Server state management
- **Axios** - HTTP client with interceptors

### Form & Validation
- **React Hook Form** - Form state management
- **Zod** - TypeScript-first schema validation

### Developer Tools
- **ESLint** - Code linting
- **Prettier** - Code formatting
- **TypeScript** - Static type checking

## Project Structure

```
kitehub-frontend/
├── src/
│   ├── app/                    # Next.js App Router pages
│   │   ├── (public)/          # Public marketing pages
│   │   ├── (auth)/            # Authentication pages
│   │   ├── (customer)/        # Customer portal
│   │   └── (admin)/           # Admin portal
│   ├── components/            # React components
│   │   ├── ui/               # Shadcn UI components
│   │   ├── layout/           # Layout components
│   │   └── common/           # Shared components
│   ├── lib/                   # Utilities and configurations
│   │   ├── api/              # API client & endpoints
│   │   ├── validations/      # Zod schemas
│   │   └── utils.ts          # Helper functions
│   ├── stores/               # Zustand stores
│   ├── hooks/                # Custom React hooks
│   └── types/                # TypeScript type definitions
├── public/                    # Static assets
└── docs/                      # Documentation
```

## API Client (`lib/api/client.ts`)

### Features
- **Automatic Token Injection**: Adds JWT to every request
- **Token Refresh**: Auto-refreshes expired tokens using refresh token
- **Vietnamese Locale**: Sets `Accept-Language: vi` header
- **Timeout Handling**: 15s timeout for all requests
- **SSR Compatible**: Checks for `window` before accessing localStorage

### Configuration

```typescript
export const apiClient: AxiosInstance = axios.create({
  baseURL: process.env.NEXT_PUBLIC_API_URL || 'http://localhost:9000',
  timeout: 15000,
  headers: {
    'Content-Type': 'application/json',
    'Accept-Language': 'vi',
  },
});
```

### Request Interceptor
Automatically adds Bearer token from localStorage to Authorization header.

### Response Interceptor
- **401 Handling**: Attempts token refresh on 401 Unauthorized
- **Retry Logic**: Retries original request with new token
- **Auto Logout**: Redirects to `/login` if refresh fails

### Usage Example

```typescript
import apiClient from '@/lib/api/client';

// GET request
const response = await apiClient.get('/api/instances');

// POST request
const result = await apiClient.post('/api/auth/login', {
  email: 'user@example.com',
  password: 'password123',
});

// With endpoints helper
import { endpoints } from '@/lib/api/endpoints';
const data = await apiClient.get(endpoints.instances.byOwner(userId));
```

## Auth Store (`stores/auth-store.ts`)

### Features
- **Zustand State Management**: Lightweight global state
- **LocalStorage Persistence**: Survives page refreshes
- **TypeScript Support**: Fully typed with interfaces
- **Role-based Access**: Supports OWNER and ADMIN roles

### State Shape

```typescript
interface User {
  id: number;
  email: string;
  name: string;
  role: 'OWNER' | 'ADMIN';
}

interface AuthState {
  user: User | null;
  accessToken: string | null;
  refreshToken: string | null;
  isAuthenticated: boolean;

  setAuth: (user, accessToken, refreshToken) => void;
  clearAuth: () => void;
  updateUser: (user: Partial<User>) => void;
}
```

### Usage Example

```typescript
import { useAuthStore } from '@/stores/auth-store';

function MyComponent() {
  const { user, isAuthenticated, setAuth, clearAuth } = useAuthStore();

  const handleLogin = async () => {
    const response = await apiClient.post('/api/auth/login', credentials);
    const { user, accessToken, refreshToken } = response.data.data;
    setAuth(user, accessToken, refreshToken);
  };

  const handleLogout = () => {
    clearAuth();
    localStorage.clear();
    router.push('/login');
  };

  return <div>{user?.email}</div>;
}
```

### Persistence
- **Storage Key**: `kitehub-auth`
- **Persisted Fields**: user, accessToken, refreshToken, isAuthenticated
- **Rehydration**: Automatically loads from localStorage on app start

## API Endpoints (`lib/api/endpoints.ts`)

### Centralized Endpoint Management
All backend API endpoints defined in one place for easy maintenance.

### Structure

```typescript
export const endpoints = {
  auth: {
    login: '/api/auth/login',
    register: '/api/auth/register',
    refresh: '/api/auth/refresh',
    me: '/api/auth/me',
  },
  instances: {
    all: '/api/instances',
    byId: (id: string) => `/api/instances/${id}`,
    byOwner: (ownerId: number) => `/api/instances/owner/${ownerId}`,
    create: '/api/instances',
  },
  subscriptions: {
    active: (instanceId: string) => `/api/subscriptions/instance/${instanceId}/active`,
    byInstance: (instanceId: string) => `/api/subscriptions/instance/${instanceId}`,
    upgrade: (id: string) => `/api/subscriptions/${id}/upgrade`,
    downgrade: (id: string) => `/api/subscriptions/${id}/downgrade`,
  },
  payments: {
    create: '/api/payments',
    byId: (id: string) => `/api/payments/${id}`,
    bySubscription: (subId: string) => `/api/payments/subscription/${subId}`,
  },
};
```

### Benefits
- **Type Safety**: TypeScript autocomplete for endpoints
- **DRY Principle**: No hardcoded URLs in components
- **Easy Refactoring**: Update endpoint in one place
- **Parameterized URLs**: Helper functions for dynamic segments

## Validation Schemas (`lib/validations/auth.ts`)

### Zod Schemas for Forms
Type-safe validation for authentication forms.

```typescript
export const loginSchema = z.object({
  email: z.string().email('Email không hợp lệ'),
  password: z.string().min(6, 'Mật khẩu phải có ít nhất 6 ký tự'),
});

export const registerSchema = z.object({
  name: z.string().min(2, 'Tên phải có ít nhất 2 ký tự'),
  email: z.string().email('Email không hợp lệ'),
  password: z.string().min(6, 'Mật khẩu phải có ít nhất 6 ký tự'),
  confirmPassword: z.string(),
}).refine((data) => data.password === data.confirmPassword, {
  message: 'Mật khẩu xác nhận không khớp',
  path: ['confirmPassword'],
});
```

### Usage with React Hook Form

```typescript
import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { loginSchema } from '@/lib/validations/auth';

function LoginForm() {
  const form = useForm({
    resolver: zodResolver(loginSchema),
    defaultValues: { email: '', password: '' },
  });

  const onSubmit = async (data) => {
    // data is fully typed and validated
  };
}
```

## Utilities (`lib/utils.ts`)

### Helper Functions
Common utility functions used across the app.

```typescript
import { clsx, type ClassValue } from 'clsx';
import { twMerge } from 'tailwind-merge';

// Merge Tailwind classes without conflicts
export function cn(...inputs: ClassValue[]) {
  return twMerge(clsx(inputs));
}
```

### Usage Example

```typescript
import { cn } from '@/lib/utils';

<div className={cn(
  'base-class',
  isActive && 'active-class',
  className // props className overrides
)} />
```

## Layout Components

### DashboardLayout (`components/layout/DashboardLayout.tsx`)
- **Purpose**: Main layout for authenticated pages
- **Features**: Sidebar, header, user menu, logout
- **Auth Guard**: Redirects to `/login` if not authenticated

### Sidebar (`components/layout/Sidebar.tsx`)
- **Variants**: Customer and Admin navigation
- **Active State**: Highlights current route
- **Icons**: Emoji-based navigation icons

### Navigation Structure

**Customer Sidebar:**
- 📊 Tổng quan (`/dashboard`)
- 💳 Thanh toán (`/billing`)
- 🎨 AI Branding (`/branding`)
- ⚙️ Cài đặt (`/settings`)

**Admin Sidebar:**
- 📊 Dashboard (`/admin`)
- 🏢 Instances (`/admin/instances`)
- 💳 Thanh toán (`/admin/payments`)
- 📈 Doanh thu (`/admin/revenue`)

## Common Components

### LoadingSpinner (`components/common/LoadingSpinner.tsx`)
Reusable loading indicator with Tailwind animation.

### ErrorAlert (`components/common/ErrorAlert.tsx`)
Error display component with consistent styling.

## Environment Variables

### Required Variables

```env
# API Configuration
NEXT_PUBLIC_API_URL=http://localhost:9000

# Optional: Production
NEXT_PUBLIC_API_URL=https://api.kiteclass.com
```

### Usage
- Prefix with `NEXT_PUBLIC_` for client-side access
- Set in `.env.local` for local development
- Configure in Vercel/hosting for production

## TypeScript Configuration

### Strict Mode Enabled
```json
{
  "compilerOptions": {
    "strict": true,
    "noImplicitAny": true,
    "strictNullChecks": true,
    "noUnusedLocals": true,
    "noUnusedParameters": true
  }
}
```

### Path Aliases
```json
{
  "compilerOptions": {
    "paths": {
      "@/*": ["./src/*"]
    }
  }
}
```

## Development Workflow

### Setup
```bash
cd kitehub/kitehub-frontend
npm install
npm run dev
```

### Commands
- `npm run dev` - Start dev server (port 3000)
- `npm run build` - Production build
- `npm run start` - Start production server
- `npm run lint` - Run ESLint
- `npm run type-check` - TypeScript check

### Hot Reload
Next.js automatically reloads on file changes.

## Best Practices

### 1. Use API Client
❌ Don't: `fetch('http://localhost:9000/api/instances')`
✅ Do: `apiClient.get(endpoints.instances.all)`

### 2. Use Auth Store
❌ Don't: `localStorage.getItem('user')`
✅ Do: `const { user } = useAuthStore()`

### 3. Use Endpoints Helper
❌ Don't: `` `/api/instances/${id}` ``
✅ Do: `endpoints.instances.byId(id)`

### 4. Validate with Zod
❌ Don't: Manual validation in components
✅ Do: Use Zod schemas with React Hook Form

### 5. TypeScript First
❌ Don't: Use `any` type
✅ Do: Define proper interfaces

## Testing Checklist

- [x] API client configured with correct baseURL
- [x] Token refresh working on 401
- [x] Auth store persists to localStorage
- [x] Login/logout flow working
- [x] Protected routes redirect to login
- [x] Sidebar navigation active states
- [x] TypeScript builds without errors
- [x] ESLint passes
- [x] Environment variables loaded

## Future Enhancements

- [ ] API client error logging to service (Sentry)
- [ ] Refresh token rotation for security
- [ ] Auth store encryption for sensitive data
- [ ] Rate limiting on API client
- [ ] Request/response logging for debugging
- [ ] Multi-language support (i18n)
- [ ] Theme switching (light/dark mode)
- [ ] Progressive Web App (PWA) support

## Dependencies

### Production
- `next`: ^15.1.3
- `react`: ^19.0.0
- `zustand`: ^5.0.2
- `@tanstack/react-query`: ^5.62.11
- `axios`: ^1.7.9
- `react-hook-form`: ^7.54.2
- `zod`: ^3.24.1
- `tailwindcss`: ^3.4.17
- `@radix-ui/*`: Various UI primitives

### Development
- `typescript`: ^5.7.2
- `eslint`: ^9.18.0
- `prettier`: ^3.4.2
- `@types/node`: ^22.10.2
- `@types/react`: ^19.0.6

## Related PRs
- **PR 5.2**: Marketing Pages & Auth Forms
- **PR 5.3**: Customer Dashboard & Instances
- **PR 5.4**: Subscription & Billing Management
