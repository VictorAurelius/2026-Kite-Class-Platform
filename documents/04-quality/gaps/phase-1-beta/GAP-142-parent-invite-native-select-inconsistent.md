# GAP-142: Parent-Invite Uses Native `<select>` — Inconsistent with Shadcn

**Status:** 🔵 OPEN
**Priority:** 🟡 P2
**Domain:** Frontend / UX / Consistency
**Found:** 2026-04-19 (UI audit catch-up — ui-review-2026-04-19.md §New Issues U-7)
**Affects:** `kiteclass-frontend` `(auth)/parent-invite/[token]/page.tsx` — relationship picker

## Problem

The parent invite redemption page uses a native HTML `<select>` element for the relationship picker (page.tsx:164-172) instead of the Shadcn `<Select>` / `<SelectTrigger>` / `<SelectContent>` pattern used everywhere else in KiteClass.

**Evidence (2026-04-19):**
```tsx
// kiteclass-frontend/src/app/(auth)/parent-invite/[token]/page.tsx:163-172
<select
  id="relationship"
  className="flex h-10 w-full rounded-md border border-input bg-background px-3 py-2 text-sm"
  value={form.relationship}
  onChange={(e) => update('relationship', e.target.value as Relationship)}
>
  <option value="FATHER">Bố</option>
  <option value="MOTHER">Mẹ</option>
  <option value="GUARDIAN">Người giám hộ</option>
</select>
```

Visual result:
- Native browser styling (varies per OS)
- No animation on open
- No search/filter (acceptable for 3 items but inconsistent with other pickers in product)
- Different hover/focus styles vs other Shadcn form controls
- Accessibility: relies on browser default (mostly fine, but no keyboard navigation consistency with Shadcn)

Elsewhere in the product (register-student gender, settings forms), Shadcn Select is used. Wave 2 MVP page slipped this detail.

## Root Cause

Wave 2 scoped for MVP identity + invitation functionality; polish deferred. No lint rule to block native `<select>` usage.

## Proposed Fix

Replace with Shadcn Select component:

```tsx
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from '@/components/ui/select';

<Select value={form.relationship} onValueChange={(v) => update('relationship', v as Relationship)}>
  <SelectTrigger id="relationship">
    <SelectValue placeholder="Chọn quan hệ" />
  </SelectTrigger>
  <SelectContent>
    <SelectItem value="FATHER">Bố</SelectItem>
    <SelectItem value="MOTHER">Mẹ</SelectItem>
    <SelectItem value="GUARDIAN">Người giám hộ</SelectItem>
  </SelectContent>
</Select>
```

Scan-and-replace any other native `<select>` in the codebase (register-student has one too at `page.tsx:256-266`).

## Acceptance Criteria

- [ ] Parent-invite relationship picker uses Shadcn Select
- [ ] Scan all `<select>` in `src/**` → replace with Shadcn unless justified
- [ ] Add ESLint rule or review checklist item to prevent future native `<select>` usage
- [ ] Visual regression: parent-invite screenshot shows consistent picker styling

## Related

- Audit: `documents/04-quality/audits/ui/ui-review-2026-04-19.md` §New Issues U-7
- Wave 2 PR: #337 (GAP-052a — parent portal identity + invitation MVP)
- Shadcn Select component: `kiteclass-frontend/src/components/ui/select.tsx`

## Log

- 2026-04-19 — Flagged during UI audit catch-up.
