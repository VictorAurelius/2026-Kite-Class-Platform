# KiteClass Frontend

Frontend cho hệ thống quản lý trung tâm tiếng Anh KiteClass.

## Tech Stack

- **Framework:** Next.js 15 (App Router)
- **Language:** TypeScript
- **Styling:** Tailwind CSS
- **UI Components:** Shadcn/UI
- **Theme System:** CSS Variables + React Context (PR-THEME-1)
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
pnpm dev          # Start dev server at http://localhost:4700

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
│   ├── (public)/          # Public landing pages
│   └── (parent)/          # Parent portal
├── components/
│   ├── ui/                # Shadcn UI components
│   ├── layout/            # Layout components (sidebar, header)
│   ├── forms/             # Form components
│   ├── tables/            # Data table components
│   ├── theme/             # Theme system components (ThemeReceiver)
│   └── shared/            # Shared components
├── contexts/              # React Context (ThemeContext)
├── hooks/                 # Custom React hooks (useTheme)
├── lib/                   # Utilities
│   ├── api/              # API client & endpoints
│   ├── theme/            # Theme system (types, utils, defaults)
│   ├── postMessage/      # postMessage handlers (theme receiver)
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

- **Multi-portal UI** - Teacher, Student, Parent portals
- **Responsive Design** - Mobile-first, works on all devices
- **Theme System** - Per-instance branding with CSS variables (see [docs/THEME-SYSTEM.md](docs/THEME-SYSTEM.md))
- **Dark Mode** - Support for dark/light themes
- **Form Validation** - Zod schema validation
- **Optimistic Updates** - React Query for seamless UX
- **Server-Side Rendering** - SSR with Next.js App Router
- **Authentication** - Secure auth & authorization

## Development Guidelines

- Follow TypeScript conventions in `.claude/skills/code-style.md`
- Use Shadcn UI components for consistency
- Write tests for components and hooks
- Follow the design system in `ui-components.md`

## License

Proprietary - KiteClass Platform
