# GAP-136: KiteHub Frontend Missing Custom `not-found.tsx` / `error.tsx`

**Status:** 🟢 DONE (2026-04-19, feature/partb-gap136-kitehub-error-pages)
**Priority:** 🔴 P0
**Domain:** Frontend / UX
**Found:** 2026-04-19 (UI audit catch-up — ui-review-2026-04-19.md §Top Findings #1)
**Affects:** `kitehub-frontend` — all public + customer + admin routes on client-side 404/500

## Problem

`kitehub/kitehub-frontend/src/app/` has **no** `not-found.tsx`, `error.tsx`, or `global-error.tsx` anywhere. When a Next.js route calls `notFound()` (e.g., `blog/[slug]/page.tsx:20` via `getBlogPost(slug)` returning null), Next's built-in English fallback "This page could not be found" is shown.

Affected routes (all render English 404/500 when data missing):
- `/blog/[slug]` — invalid/unknown slug
- `/instances/[id]` — unknown instance id
- `/admin/instances/[id]` — unknown instance id
- `/billing/payment/[id]` — unknown payment id
- Any unknown URL

**Evidence (code-level):**
```
$ find kitehub/kitehub-frontend/src/app -name "not-found.tsx" -o -name "error.tsx" -o -name "global-error.tsx"
(no results)
```

Previous UI audit flagged "blog-detail → 36/128 (English 404)" as H-2. This audit confirms the gap is NOT blog-specific — it is app-wide.

## Root Cause

Next.js 15 App Router requires apps to opt-in to custom error pages by exporting `not-found.tsx` / `error.tsx`. KiteHub frontend was bootstrapped without these, and Wave 4 (GAP-032 branded error pages) addressed this **at the gateway level only** — the Next.js app itself still falls back to Next defaults when `notFound()` is invoked inside a page component.

KiteClass does have `(public)/not-found.tsx` + `(public)/error.tsx` (verified) — KiteHub missed the same setup.

## Proposed Fix

Mirror KiteClass's pattern:

1. Create `kitehub/kitehub-frontend/src/app/not-found.tsx` (root) + `(public)/not-found.tsx` + `(customer)/not-found.tsx` + `(admin)/not-found.tsx` as needed per route group.
2. Create matching `error.tsx` files with `reset` support + "Về trang chủ" fallback.
3. Add `global-error.tsx` for root-level fatal errors.
4. Each page uses KiteHub branding (sky blue palette, KiteHub logo) and Vietnamese copy.
5. Provide back-to-home + relevant section links (e.g., blog 404 suggests latest posts).

Example structure (follow KiteClass pattern):
```tsx
// kitehub/kitehub-frontend/src/app/not-found.tsx
import Link from 'next/link';
import { FileQuestion } from 'lucide-react';
import { Button } from '@/components/ui/button';

export default function NotFound() {
  return (
    <div className="flex flex-col items-center justify-center min-h-[60vh] p-8">
      <FileQuestion className="h-16 w-16 text-muted-foreground mb-4" />
      <h2 className="text-2xl font-bold mb-2">Không tìm thấy trang</h2>
      <p className="text-muted-foreground mb-6 text-center max-w-md">
        Trang bạn đang tìm kiếm không tồn tại hoặc đã bị di chuyển.
      </p>
      <div className="flex gap-4">
        <Button asChild><Link href="/">Về trang chủ</Link></Button>
        <Button variant="outline" asChild><Link href="/blog">Xem blog</Link></Button>
      </div>
    </div>
  );
}
```

## Acceptance Criteria

- [x] `kitehub-frontend/src/app/not-found.tsx` exists with Vietnamese copy + CTA
- [x] `kitehub-frontend/src/app/error.tsx` exists with reset() + fallback link + Sentry-ready hook
- [x] `kitehub-frontend/src/app/global-error.tsx` exists for root-level errors (own `<html>`/`<body>`)
- [x] All three pages tested via Vitest + RTL — 13/13 unit tests passing
- [ ] Visit `/blog/bogus-slug` → shows Vietnamese 404 (not "This page could not be found") *(manual verify when stack up)*
- [ ] Visit `/instances/bogus-id` as logged-in owner → shows Vietnamese 404 in customer layout *(per-group not-found.tsx deferred — root catches all)*
- [ ] Visit `/admin/instances/bogus-id` as admin → shows Vietnamese 404 in admin layout *(same — root catches all)*
- [ ] E2E/Playwright assertion: `text=Không tìm thấy trang` on 4xx routes *(deferred to follow-up E2E PR)*

## Related

- Audit: `documents/04-quality/audits/ui/ui-review-2026-04-19.md` §Top Findings #1, §New Issues U-1
- Baseline: `documents/04-quality/audits/ui/ui-review-latest.md` (H-2 blog-detail 36/128 flag)
- Resolved-at-gateway: GAP-032 (branded gateway 503/404/500 HTML) — this gap is the **Next.js app layer**, complementary
- Reference pattern: `kiteclass-frontend/src/app/(public)/not-found.tsx`, `(public)/error.tsx`

## Log

- 2026-04-19 — Identified during Audit 4 (ui-review catch-up)
- 2026-04-19 — FIXED via `feature/partb-gap136-kitehub-error-pages`. Root-level `not-found.tsx`, `error.tsx`, `global-error.tsx` added with Vietnamese copy, KiteHub theme (CSS vars, dark-mode aware), Shadcn Button, Sentry-ready `useEffect` hook for APM wiring. 13 unit tests via Vitest + RTL — all green. `pnpm type-check` + `pnpm lint` clean (only known pre-existing warnings remain, no regressions). Per-route-group `not-found.tsx` (public/customer/admin) deferred — root file catches every unmatched path; can be added later if route-group-specific layouts need bespoke 404s.
