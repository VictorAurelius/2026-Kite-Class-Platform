# Quick Start — kitehub-frontend

## Prerequisites

- Node.js 20+
- pnpm 9+

## Install

```bash
cd kitehub/kitehub-frontend/
pnpm install
```

## Run

```bash
# Development (hot reload)
pnpm dev
# Open http://localhost:4701

# Via Docker
cd kitehub/
./scripts/up.sh kitehub-frontend
```

**Port:** 4701

## Build

```bash
pnpm build
pnpm start  # production mode
```

## Test

```bash
# Unit tests
pnpm test

# E2E (Playwright)
cd kitehub/
./scripts/test-e2e-frontend.sh
```

## Project Structure

```
src/
├── app/
│   ├── (public)/      # Marketing pages, blog, landing
│   ├── (auth)/        # Login, register, verify
│   ├── (customer)/    # Dashboard, branding, settings
│   └── (admin)/       # Admin dashboard
├── components/        # Shared components (Shadcn UI)
├── hooks/             # Custom React hooks
├── lib/               # API client, utils
└── types/             # TypeScript types
```

## Environment

Copy `.env.example` to `.env.local`:
```bash
NEXT_PUBLIC_API_URL=http://localhost:9000
NEXT_PUBLIC_KITECLASS_URL_PATTERN=http://localhost:4700?tenant={subdomain}
```

## Documentation

- Business logic: [documents/01-business/kitehub/](../../../documents/01-business/kitehub/)
- Full docs: [documents/README.md](../../../documents/README.md)
