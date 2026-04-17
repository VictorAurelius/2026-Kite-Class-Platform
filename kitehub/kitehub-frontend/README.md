# KiteHub Frontend

Modern Next.js 15 frontend for KiteHub - Multi-tenant educational platform management system.

## Tech Stack

### Core
- **Next.js 15.1.3** - React framework with App Router
- **React 19.0.0** - UI library with Server Components
- **TypeScript 5.7.2** - Static type checking
- **Tailwind CSS 3.4.17** - Utility-first CSS

### State & Data
- **Zustand 5.0.2** - Lightweight global state
- **TanStack Query 5.62.11** - Server state management
- **Axios 1.7.9** - HTTP client

### UI Components
- **Shadcn/UI** + **Radix UI** - Accessible component library
- **Lucide React** - Icon library
- **React Hook Form 7.54.2** + **Zod 3.24.1** - Forms & validation

## Port

| Context | Port |
|---------|------|
| Development | `3001` |
| Docker (host) | `3001` |

## Project Structure

```
kitehub-frontend/
├── src/
│   ├── app/                    # Next.js App Router
│   │   ├── (public)/          # Marketing pages (landing, pricing)
│   │   ├── (auth)/            # Login, register
│   │   ├── (customer)/        # Customer portal (dashboard, billing, settings)
│   │   └── (admin)/           # Admin portal
│   ├── components/            # React components (ui/, layout/, common/, billing/)
│   ├── lib/                   # API client, validations, utilities
│   ├── stores/                # Zustand stores (auth)
│   ├── hooks/                 # Custom hooks (instances, subscriptions, payments)
│   └── types/                 # TypeScript type definitions
├── public/                    # Static assets
└── docs/                      # Service-specific documentation
```

## Features

- **Marketing** - Landing page, pricing comparison (Vietnamese content)
- **Authentication** - JWT auth with auto-refresh, role-based access (OWNER/ADMIN)
- **Instance Management** - Dashboard with status badges, trial countdown
- **Billing** - Subscription management, upgrade/downgrade wizard, VietQR payments
- **AI Branding** - Logo analysis, theme generation (planned)

## Environment Variables

| Variable | Default | Description |
|----------|---------|-------------|
| `NEXT_PUBLIC_API_URL` | `http://localhost:9000` | Backend API URL (gateway) |

## Development

```bash
cd kitehub/kitehub-frontend
npm install
npm run dev        # Start dev server (http://localhost:4701)
npm run build      # Production build
npm run lint       # Run ESLint
npm run test       # Run unit tests
npm run test:e2e   # Run E2E tests (Playwright)
```

## Docker

```bash
# Full stack (recommended)
cd kitehub && ./scripts/up.sh

# Standalone
docker build -t kitehub-frontend:latest .
docker run -d -p 3001:3001 -e NEXT_PUBLIC_API_URL=http://localhost:9000 kitehub-frontend:latest
```

## Key Patterns

- **API Client**: `apiClient.get(endpoints.instances.byOwner(userId))` - auto token injection
- **Auth Store**: `useAuthStore()` - Zustand with localStorage persistence
- **Server State**: React Query hooks (`useOwnerInstances`, `useActiveSubscription`)
- **Validation**: Zod schemas with React Hook Form resolver
- **Protected Routes**: Layout-level auth guard with role-based redirect

## Monitoring

- Health: `/api/health`

## Links

- Business logic: [documents/01-business/kitehub/](../../documents/01-business/kitehub/)
- Architecture: [documents/02-architecture/](../../documents/02-architecture/)
- Archived PR docs: [documents/07-archived/kiteclass-legacy-docs/kitehub-frontend-prs/](../../documents/07-archived/kiteclass-legacy-docs/kitehub-frontend-prs/)
