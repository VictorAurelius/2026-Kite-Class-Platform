# KiteClass Frontend

Frontend cho hệ thống quản lý trung tâm tiếng Anh KiteClass.

## Tech Stack

- **Framework:** Next.js 15 (App Router)
- **Language:** TypeScript
- **Styling:** Tailwind CSS
- **UI Components:** Shadcn/UI
- **State Management:**
  - Zustand (Client State)
  - React Query (Server State)
- **Form Handling:** React Hook Form + Zod
- **HTTP Client:** Axios

## Getting Started

### Prerequisites

- Node.js 20+
- pnpm 10+

### Installation

```bash
# Install dependencies
pnpm install

# Copy environment file
cp .env.example .env.local

# Start development server
pnpm dev
```

### Available Scripts

```bash
# Development
pnpm dev          # Start dev server at http://localhost:3000

# Build
pnpm build        # Build for production
pnpm start        # Start production server

# Code Quality
pnpm lint         # Run ESLint
pnpm format       # Format with Prettier
```

## Project Structure

```
src/
├── app/                    # Next.js App Router
│   ├── (auth)/            # Auth pages (login, forgot password)
│   ├── (dashboard)/       # Dashboard pages
│   └── (parent)/          # Parent portal
├── components/
│   ├── ui/                # Shadcn UI components
│   ├── layout/            # Layout components (sidebar, header)
│   ├── forms/             # Form components
│   ├── tables/            # Data table components
│   └── shared/            # Shared components
├── hooks/                 # Custom React hooks
├── lib/                   # Utilities
│   ├── api/              # API client & endpoints
│   └── validations/      # Zod schemas
├── providers/            # React context providers
├── stores/               # Zustand stores
└── types/                # TypeScript types
```

## Environment Variables

```bash
NEXT_PUBLIC_API_URL=http://localhost:8080/api/v1
NEXT_PUBLIC_APP_NAME=KiteClass
NEXT_PUBLIC_APP_VERSION=1.0.0
```

## Features

- Multi-portal UI (Teacher, Student, Parent)
- Responsive design
- Dark mode support
- Form validation with Zod
- Optimistic updates with React Query
- Server-side rendering (SSR)
- Authentication & authorization

## Development Guidelines

- Follow TypeScript conventions in `.claude/skills/code-style.md`
- Use Shadcn UI components for consistency
- Write tests for components and hooks
- Follow the design system in `ui-components.md`

## License

Proprietary - KiteClass Platform
