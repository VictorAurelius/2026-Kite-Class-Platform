# KiteHub Frontend

> Modern Next.js 15 frontend for KiteHub - Multi-tenant educational platform management system

## Overview

KiteHub Frontend is a comprehensive web application built with Next.js 15 (App Router) that enables educational centers to manage their KiteClass instances. The platform features subscription management, billing integration, instance dashboards, and AI-powered branding tools.

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
- **Shadcn/UI** - Accessible component library
- **Radix UI** - Headless UI primitives
- **Lucide React** - Icon library

### Forms & Validation
- **React Hook Form 7.54.2** - Form state management
- **Zod 3.24.1** - Schema validation

## Project Structure

```
kitehub-frontend/
├── src/
│   ├── app/                      # Next.js App Router
│   │   ├── (public)/            # Public marketing pages
│   │   │   ├── page.tsx         # Landing page
│   │   │   └── pricing/         # Pricing page
│   │   ├── (auth)/              # Authentication
│   │   │   ├── login/           # Login page
│   │   │   └── register/        # Registration page
│   │   ├── (customer)/          # Customer portal
│   │   │   ├── dashboard/       # Main dashboard
│   │   │   ├── instances/       # Instance management
│   │   │   ├── billing/         # Subscription & payments
│   │   │   │   ├── page.tsx     # Billing overview
│   │   │   │   ├── upgrade/     # Tier change wizard
│   │   │   │   ├── payment/     # Payment page with QR
│   │   │   │   └── history/     # Payment history
│   │   │   └── settings/        # Account settings
│   │   └── (admin)/             # Admin portal
│   │       └── admin/           # Admin dashboard
│   ├── components/              # React components
│   │   ├── ui/                 # Shadcn UI primitives
│   │   ├── layout/             # Layout components
│   │   ├── common/             # Shared components
│   │   └── billing/            # Billing-specific components
│   ├── lib/                     # Utilities & config
│   │   ├── api/                # API client & endpoints
│   │   ├── validations/        # Zod schemas
│   │   ├── pricing.ts          # Pricing calculations
│   │   ├── error-handler.ts    # Error utilities
│   │   └── utils.ts            # Helper functions
│   ├── stores/                  # Zustand stores
│   │   └── auth-store.ts       # Auth state
│   ├── hooks/                   # Custom React hooks
│   │   ├── use-instances.ts    # Instance queries
│   │   ├── use-subscriptions.ts # Subscription mutations
│   │   └── use-payments.ts     # Payment queries
│   └── types/                   # TypeScript types
│       ├── instance.ts         # Instance types
│       ├── subscription.ts     # Subscription types
│       ├── payment.ts          # Payment types
│       └── api.ts              # API response types
├── public/                      # Static assets
├── docs/                        # Documentation
│   ├── PR-5.1-INFRASTRUCTURE.md
│   ├── PR-5.2-MARKETING-AUTH.md
│   ├── PR-5.3-CUSTOMER-DASHBOARD.md
│   └── billing/README.md
└── .env.local                   # Environment variables
```

## Features

### 📱 Marketing & Landing
- **Landing Page**: Hero, features, stats, CTA
- **Pricing Page**: 4-tier comparison (FREE/BASIC/PREMIUM/ENTERPRISE)
- **Vietnamese Content**: Fully localized

### 🔐 Authentication
- **Login/Register**: Form validation with Zod
- **JWT Auth**: Auto token refresh on 401
- **Role-based Access**: OWNER vs ADMIN portals
- **Persistent Sessions**: LocalStorage + Zustand

### 🏢 Instance Management
- **Dashboard**: Instance grid with status badges
- **Trial Tracking**: Countdown timer for trial instances
- **Instance Details**: Subdomain, tier, created date
- **Empty States**: CTAs for first-time users

### 💳 Subscription & Billing
- **Billing Overview**: Current plan display with progress bar
- **Tier Comparison**: Interactive plan selection
- **Upgrade/Downgrade**: Multi-step wizard
- **Prorated Pricing**: Mid-cycle upgrade calculations
- **VietQR Integration**: QR code payment with auto-polling
- **Payment History**: Filterable transaction table

### 🎨 AI Branding (Future)
- Logo upload → AI-generated branding assets
- Landing page builder
- Color scheme generator

## Getting Started

### Prerequisites
- Node.js 18+
- npm 9+
- Backend API running (default: http://localhost:9000)

### Installation

```bash
# Clone repository
git clone https://github.com/VictorAurelius/2026-Kite-Class-Platform.git
cd 2026-Kite-Class-Platform/kitehub/kitehub-frontend

# Install dependencies
npm install

# Create environment file
cp .env.example .env.local

# Start development server
npm run dev
```

### Environment Variables

```env
# Required
NEXT_PUBLIC_API_URL=http://localhost:9000

# Optional: Production
NEXT_PUBLIC_API_URL=https://api.kiteclass.com
```

### Development

```bash
npm run dev      # Start dev server (http://localhost:3001)
npm run build    # Production build
npm run start    # Start production server
npm run lint     # Run ESLint
npm run format   # Format with Prettier
npm run test     # Run unit tests
npm run test:e2e # Run E2E tests
```

## Running with Docker

### Docker Build

```bash
# Build Docker image
cd kitehub/kitehub-frontend
docker build -t kitehub-frontend:latest .

# Verify image size (should be < 150MB)
docker images kitehub-frontend:latest

# Run container
docker run -d \
  --name kitehub-frontend \
  -p 3001:3001 \
  -e NEXT_PUBLIC_API_URL=http://localhost:9000 \
  kitehub-frontend:latest

# Check health
curl http://localhost:3001/api/health

# View logs
docker logs -f kitehub-frontend

# Stop container
docker stop kitehub-frontend
docker rm kitehub-frontend
```

### Docker Compose

The recommended way to run KiteHub Frontend with all dependencies:

```bash
# From project root
docker compose -f docker-compose.kitehub.yml up -d

# View logs
docker compose -f docker-compose.kitehub.yml logs -f kitehub-frontend

# Stop services
docker compose -f docker-compose.kitehub.yml down

# Rebuild after code changes
docker compose -f docker-compose.kitehub.yml up -d --build
```

### Environment Variables (Docker)

Copy `.env.docker.example` to configure:

```env
# Required
NEXT_PUBLIC_API_URL=http://gateway:9000   # Docker network
NODE_ENV=production
PORT=3001

# Optional
NEXT_TELEMETRY_DISABLED=1
```

### Docker Troubleshooting

**Image too large (>150MB)**
```bash
# Check layer sizes
docker history kitehub-frontend:latest

# Common fixes:
# - Ensure .dockerignore excludes node_modules
# - Verify standalone output is enabled in next.config.js
```

**Container exits immediately**
```bash
# Check logs
docker logs kitehub-frontend

# Common issues:
# - Missing standalone build: ensure 'pnpm build' ran successfully
# - Port conflict: check if 3001 is already in use
```

**Health check fails**
```bash
# Test health endpoint inside container
docker exec kitehub-frontend curl http://localhost:3001/api/health

# Check if Next.js server started
docker exec kitehub-frontend ps aux | grep node
```

**Can't connect to backend API**
```bash
# Verify API URL
docker exec kitehub-frontend env | grep NEXT_PUBLIC_API_URL

# Test connectivity from container
docker exec kitehub-frontend curl http://gateway:9000/health
```

## API Integration

### API Client (`lib/api/client.ts`)
Axios instance with automatic:
- Bearer token injection
- Token refresh on 401
- Vietnamese locale header
- 15s timeout

### Endpoints (`lib/api/endpoints.ts`)
Centralized endpoint management:

```typescript
endpoints.auth.login            // POST /api/auth/login
endpoints.instances.byOwner()   // GET /api/instances/owner/{id}
endpoints.subscriptions.active() // GET /api/subscriptions/instance/{id}/active
endpoints.payments.create       // POST /api/payments
```

### React Query Hooks
Type-safe data fetching:

```typescript
// Instances
useOwnerInstances(ownerId)
useInstance(id)
useTrialStatus(instanceId)

// Subscriptions
useActiveSubscription(instanceId)
useUpgradeSubscription()
useDowngradeSubscription()

// Payments
usePayment(paymentId)      // Auto-polls every 5s if PENDING
usePaymentHistory(subscriptionId)
useCreatePayment()
```

## Key Patterns

### 1. Protected Routes
```typescript
// layout.tsx in (customer) group
if (!isAuthenticated) {
  router.push('/login');
  return null;
}
```

### 2. Form Validation
```typescript
const form = useForm({
  resolver: zodResolver(loginSchema),
});
```

### 3. API Error Handling
```typescript
import { showErrorToast } from '@/lib/error-handler';

try {
  await apiClient.post('/endpoint', data);
} catch (error) {
  showErrorToast(error);
}
```

### 4. State Management
```typescript
// Global state (auth)
const { user, setAuth } = useAuthStore();

// Server state (data)
const { data, isLoading } = useOwnerInstances(user?.id);
```

### 5. Auto-Polling
```typescript
const { data: payment } = usePayment(paymentId);
// Automatically refetches every 5s if status === 'PENDING'
```

## Component Library

### Common Components
- `LoadingSpinner` - Loading indicator
- `ErrorAlert` - Error display with retry
- `EmptyState` - Empty state with CTA
- `StatusBadge` - Status badges (TRIAL/ACTIVE/etc.)

### Billing Components
- `CurrentPlanCard` - Current subscription summary
- `PlanComparison` - 4-tier comparison grid
- `TierSelector` - Interactive tier selection
- `ChangeConfirmation` - Upgrade/downgrade confirmation
- `QRCodeDisplay` - QR code with countdown timer
- `PaymentHistoryTable` - Transaction list with filters

### Layout Components
- `DashboardLayout` - Main authenticated layout
- `Sidebar` - Navigation sidebar (customer/admin variants)

## Documentation

### PR Documentation
- [**PR 5.1**: Infrastructure](docs/PR-5.1-INFRASTRUCTURE.md) - API client, auth store, layouts
- [**PR 5.2**: Marketing & Auth](docs/PR-5.2-MARKETING-AUTH.md) - Landing page, login/register
- [**PR 5.3**: Customer Dashboard](docs/PR-5.3-CUSTOMER-DASHBOARD.md) - Dashboard, instances
- [**PR 5.4**: Billing](src/app/(customer)/billing/README.md) - Subscription & payments

### Feature Guides
- [Billing Module](src/app/(customer)/billing/README.md) - Complete billing documentation

## TypeScript

### Strict Mode Enabled
```json
{
  "strict": true,
  "noImplicitAny": true,
  "strictNullChecks": true
}
```

### Path Aliases
```typescript
import { useAuthStore } from '@/stores/auth-store';
import { Button } from '@/components/ui/button';
```

## Testing (Future)

### Planned Testing Stack
- **Vitest** - Unit testing
- **React Testing Library** - Component testing
- **Playwright** - E2E testing

### Testing Checklist
- [ ] Component unit tests
- [ ] Hook tests
- [ ] API integration tests
- [ ] E2E user flows

## Performance

### Optimizations
- Next.js automatic code splitting
- React Query caching (5-minute stale time)
- Image optimization (Next.js Image)
- Font optimization (next/font)

### Metrics
- Lighthouse score: 90+ (target)
- First Contentful Paint: < 1.8s
- Time to Interactive: < 3.8s

## Deployment

### Option 1: Docker (Recommended for Production)

**Prerequisites:**
- Docker Engine 20.10+
- Docker Compose 2.0+

**Steps:**
```bash
# 1. Build image
docker build -t kitehub-frontend:v1.0.0 .

# 2. Run with Docker Compose
docker compose -f docker-compose.kitehub.yml up -d

# 3. Verify deployment
curl http://localhost:3001/api/health
```

**Production Checklist:**
- [ ] Image size < 150MB
- [ ] Health check responds within 3s
- [ ] Environment variables configured
- [ ] Logs accessible via `docker logs`
- [ ] Auto-restart enabled (`restart: unless-stopped`)

### Option 2: Vercel (Recommended for Staging)

```bash
# Install Vercel CLI
npm i -g vercel

# Deploy to production
vercel --prod
```

**Environment Variables:**
Set in Vercel dashboard:
- `NEXT_PUBLIC_API_URL` - Backend API URL

### Option 3: Manual Build

```bash
# Build for production
npm run build

# Start production server
npm run start

# Or use PM2 for process management
pm2 start npm --name "kitehub-frontend" -- start
```

### Build Output

```bash
npm run build
# Output:
# - .next/ folder (all build artifacts)
# - .next/standalone/ folder (Docker-optimized)
# - .next/static/ folder (static assets)
```

## Contributing

### Branch Naming
- `feature/KC-X.Y-description` - New features
- `fix/description` - Bug fixes
- `docs/description` - Documentation

### Commit Convention
```
feat(scope): Description
fix(scope): Description
docs(scope): Description
refactor(scope): Description
```

### PR Template
- **Summary**: What was implemented
- **Features**: Bullet list of features
- **Components**: New components created
- **Testing Checklist**: What was tested
- **Documentation**: Links to docs

## Troubleshooting

### Common Issues

**1. API Connection Refused**
```bash
# Check backend is running
curl http://localhost:9000/api/health

# Verify .env.local
cat .env.local | grep NEXT_PUBLIC_API_URL
```

**2. Token Expired**
```bash
# Clear localStorage
localStorage.clear()

# Restart dev server
npm run dev
```

**3. Module Not Found**
```bash
# Clear cache and reinstall
rm -rf .next node_modules
npm install
npm run dev
```

## Roadmap

### Q1 2026 ✅
- [x] Project setup
- [x] Marketing pages
- [x] Auth flows
- [x] Customer dashboard
- [x] Billing management

### Q2 2026 🚧
- [ ] AI Branding tools
- [ ] Instance settings
- [ ] Team management
- [ ] Analytics dashboard

### Q3 2026 📋
- [ ] Mobile app (React Native)
- [ ] Advanced reporting
- [ ] API webhooks
- [ ] Multi-language support

## License

Proprietary - © 2026 KiteClass Platform

## Support

- **Issues**: GitHub Issues
- **Email**: support@kiteclass.com
- **Docs**: /docs folder

## Team

- **Lead Developer**: VictorAurelius
- **Design**: TBD
- **QA**: TBD

---

**Built with ❤️ using Next.js 15 and TypeScript**
