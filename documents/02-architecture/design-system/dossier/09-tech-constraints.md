# 09 — Tech Constraints

The locked stack. Round 2 deliverables that propose alternative libraries get rejected — every component must be reproducible in this stack.

**Source:** `kitehub/kitehub-frontend/package.json`, `kiteclass/kiteclass-frontend/package.json`, `tailwind.config.ts` of both apps (read 2026-04-29).

---

## 1. Framework + runtime

| Tech | Version | Apps | Notes |
|------|---------|------|-------|
| **Next.js** | 15.5.15 | both | App Router (not Pages); Server Components + Client Components; `use client` directive required for interactivity |
| **React** | 19.2.5 | both | concurrent features, `useFormState`, `useOptimistic` available; no class components |
| **React DOM** | 19.2.5 | both | — |
| **TypeScript** | 5.7.2 | both | strict mode enabled |
| **Node** | ≥20.x | dev | required by Next.js 15 |
| **Package manager** | pnpm | dev | with overrides: picomatch, flatted, minimatch, rollup, postcss |
| **Monorepo** | none | — | Independent apps; no Turbo/Nx |

**Constraint:** No Vue, Svelte, plain HTML/JS without React equivalent. No frameworks Tailwind doesn't support.

---

## 2. Styling

| Tech | Version | Apps | Notes |
|------|---------|------|-------|
| **Tailwind CSS** | 3.4.17 | both | NOT v4 yet |
| **Tailwind Animate** | 1.0.7 | both | for shadcn animations |
| **PostCSS** | locked via pnpm overrides | dev | — |
| **Dark mode** | `class` strategy | both | toggle via `next-themes` |
| **CSS variables** | HSL (shadcn) + RGB (KC theme overlay) | both | see `colors_and_type.css` |

**KH Tailwind extensions:**
- Container padding: 1rem → 2rem → 4rem → 5rem → 6rem (sm/lg/xl/2xl)
- Custom shadows: `shadow-soft`, `shadow-soft-lg`, `shadow-soft-xl` (warm diffuse)
- Custom fonts: Inter (sans), JetBrains Mono (mono)
- HSL color tokens: border, input, ring, background, foreground, primary, secondary, destructive, muted, accent, popover, card

**KC Tailwind extensions (theme overlay v2):**
- All KH tokens above
- **+** Theme overlay system (RGB-based with alpha):
  - `--theme-primary`, `--theme-secondary`, `--theme-accent`, `--theme-background`
  - `--theme-border-radius`
  - `--theme-shadow-sm`, `--theme-shadow-md`, `--theme-shadow-lg`
  - `--theme-font-heading`, `--theme-font-body`

**Constraint:** No Bootstrap, MUI, Chakra, Ant Design. No styled-components, Emotion. Tailwind + shadcn ONLY.

---

## 3. UI primitives

### shadcn/ui base — installed components

**KH (17):** alert, alert-dialog, badge, button, card, checkbox, dialog, dropdown-menu, input, label, progress, select, separator, switch, table, tabs, textarea
**KC (22):** alert, alert-dialog, avatar, badge, button, calendar, card, checkbox, confirm-dialog, dialog, dropdown-menu, form, input, label, popover, progress, radio-group, select, separator, sheet, skeleton, switch, tabs, textarea, toast, toaster, tooltip

### Radix UI primitives (under shadcn)

| Primitive | KH | KC |
|-----------|:--:|:--:|
| Alert Dialog | ✓ | ✓ |
| Avatar | — | ✓ |
| Checkbox | ✓ | ✓ |
| Dialog | ✓ | ✓ |
| Dropdown Menu | ✓ | ✓ |
| Label | ✓ | ✓ |
| Popover | — | ✓ |
| Radio Group | — | ✓ |
| Select | ✓ | ✓ |
| Separator | ✓ | ✓ |
| Slot | ✓ | ✓ |
| Switch | ✓ | ✓ |
| Tabs | ✓ | ✓ |
| Toast | ✓ | ✓ |
| Tooltip | ✓ | ✓ |

**Custom KH components (4):** `gradient-button`, `gradient-text`, `page-header`, `section-title`

**Constraint:** Round 2 uses shadcn primitives; new components extend them. If a primitive is missing (e.g., Combobox), install shadcn version, don't bring in Headless UI / etc.

---

## 4. State + data

| Tech | Version | Apps | Use |
|------|---------|------|-----|
| **TanStack Query** | 5.100.5 | both | server state, polling, optimistic updates |
| **TanStack Table** | 8.21.3 | both | data tables (replaces ag-grid) |
| **Zustand** | 5.0.12 | both | client state (UI-only) |
| **react-hook-form** | 7.74.0 | both | form state |
| **Zod** | 3.24.1 | both | schema validation (forms + API contracts) |
| **axios** | 1.15.2 | both | HTTP client (ATM — could swap to fetch later) |

**Constraint:** No Redux, no MobX, no Recoil. No Apollo Client (we don't have GraphQL). TanStack everything.

---

## 5. Animation + interactions

| Tech | Version | Apps | Use |
|------|---------|------|-----|
| **Framer Motion** | 12.38.0 | KH only | hero animations, page transitions, scroll-driven |
| **Tailwind Animate** | 1.0.7 | both | shadcn enter/exit |
| **next-themes** | 0.4.4 | both | dark mode toggle (no FOUC) |

**Constraint for KC:** No Framer Motion (not installed) — use CSS transitions + Tailwind Animate. If Round 2 D (kiteclass-parent) needs heavy animation, add Framer Motion to KC `package.json` as a port-time decision (file ADR if needed).

---

## 6. Icons + media

| Tech | Version | Apps | Use |
|------|---------|------|-----|
| **lucide-react** | 0.577.0 | both | all functional UI icons |
| **Inter (Google Fonts)** | latest | both | sans-serif, vietnamese subset |
| **JetBrains Mono** | latest | both | mono fallback |
| **recharts** | 2.15.3 | KH only | charts (sparklines, bars) |

**Constraint:** lucide ONLY for icons. No Heroicons, FontAwesome, Material Icons. Emoji used sparingly (per `colors_and_type.css` README).

**For KC charts:** install recharts at port time (if Round 2 B sparkline lands in KC). It's MIT-licensed, ~85KB gzipped.

---

## 7. Notifications + utility

| Tech | Version | Apps | Use |
|------|---------|------|-----|
| **sonner** | 1.7.0 | KH only | toasts |
| **shadcn toast/toaster** | latest | KC | toasts (Radix-based) |
| **clsx** | 2.1.1 | both | conditional classNames |
| **tailwind-merge** | 2.6.0 | both | merge Tailwind classes |
| **class-variance-authority** | 0.7.1 | both | component variants |
| **date-fns** | 4.1.0 | both | date utilities (NOT moment, NOT dayjs) |

---

## 8. Test + dev

| Tech | Version | Use |
|------|---------|-----|
| **Vitest** | 4.1.5 | unit tests |
| **@testing-library/react** | 16.3.2 | component tests |
| **Playwright** | 1.59.1 | E2E |
| **MSW** | 2.13.6 | API mocking (KC dev dep; KH N/A — affects screen capture) |
| **ESLint** | 8.57.1 | lint |
| **Prettier** | 3.8.3 | format |

---

## 9. Constraints summary for Claude Design

When Round 2 outputs HTML mockups, the implicit constraint is "must port cleanly to this stack." Practical implications:

| If you're tempted to use... | Use this instead |
|-----------------------------|------------------|
| Bootstrap classes | Tailwind classes |
| MUI components | shadcn primitives |
| Vue / Alpine | React (Next.js App Router) |
| jQuery | React + TanStack Query |
| Moment.js | date-fns |
| Heroicons | lucide-react |
| Custom CSS frameworks | Tailwind + CSS variables from `colors_and_type.css` |
| Inline `style="color: #..."` | Tailwind class with HSL var, e.g., `text-primary` |
| Custom font loader | Inter via Google Fonts (already loaded) |
| Material Design ripple | shadcn button hover (subtle) |
| Charts.js | recharts (KH) — for KC, defer chart screens to Round 3 OR add recharts as port-time decision |

---

## 10. What you DON'T need to worry about

- **Backend stack:** Spring Boot 3.5 + PostgreSQL 16 + Redis + RabbitMQ + MinIO. Design doesn't touch backend.
- **Auth:** JWT-based, handled by gateway. Design assumes user is authenticated; no need to design login internals beyond surface.
- **Multi-tenancy:** subdomain-keyed routing, theme overlay applied via React Context. Design with tenant theme colors as variables.
- **i18n library:** react-i18next OR next-intl (TBD — file the i18n key in comments, library agnostic).
- **CMS / blog:** MDX-based, no headless CMS. Design treats as static content.
- **Build / deploy:** Next.js standalone build → Docker → ECR → ECS. Design doesn't change this.

---

## 11. Constraints DURING design (not just port)

For Claude Design output to be acceptable Round 2:

- **All HTML files reference `colors_and_type.css`** (already in bundle) — no inline color values, no hex literals
- **All icons from lucide unpkg CDN** in HTML mocks: `<script src="https://unpkg.com/lucide@latest"></script>`
- **All fonts from Google Fonts CDN** in HTML mocks: `<link href="https://fonts.googleapis.com/css2?family=Inter:..."`
- **No frameworks loaded via CDN beyond lucide + Inter** — keeps mock surface clean
- **Tailwind via Play CDN** acceptable for HTML mocks: `<script src="https://cdn.tailwindcss.com"></script>` — port-time replaces with config-driven
- **JSX files use React-like syntax** (no Vue templates) — even if it's `app.jsx`, write as functional component for portability
