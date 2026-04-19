# GAP-139: Parent Dashboard MVP is Placeholder-Only (Wave 5 Widgets Missing)

**Status:** 🔵 OPEN
**Priority:** 🟠 P1
**Domain:** Frontend / Feature Completeness
**Found:** 2026-04-19 (UI audit catch-up — ui-review-2026-04-19.md §New Issues U-3)
**Affects:** `kiteclass-frontend` `(dashboard)/parent/page.tsx` — Wave 2 (#337 GAP-052a) parent portal MVP

## Problem

Wave 2 parent portal ships a dashboard with:
- Greeting header
- Link count text ("Bạn đang liên kết với N con")
- One card listing linked children (name + className + grade + linkType)
- Footer placeholder: "Các widget điểm danh, học lực và học phí sẽ có trong Wave 5."

That's the entire page. From an external-auditor perspective (the UI audit rubric — "score what you SEE"), this does not pass the "có feature" bar for a logged-in parent dashboard. A parent logging in expects at minimum:
- Today's attendance status per child
- Unpaid invoices count (or "Không có" empty state)
- Recent grades (or announcements)
- Upcoming classes

**Evidence (2026-04-19):**
```tsx
// kiteclass-frontend/src/app/(dashboard)/parent/page.tsx:154-156
<p className="text-center text-xs text-muted-foreground">
  Các widget điểm danh, học lực và học phí sẽ có trong Wave 5.
</p>
```

## Root Cause

Wave 2 deliberately scoped MVP to identity + invitation (GAP-052a), explicitly deferring widgets to Wave 5. The gap itself is a planning decision, not a bug. This gap tracks that the parent portal, as shipped, is not production-quality for real parent users.

## Proposed Fix

Wave 5 (or a bridging PR) should ship baseline widgets:

1. **Attendance widget** — per child, last class attendance status + link to full history
2. **Billing widget** — unpaid invoices count + amount + link to pay
3. **Grades widget** — recent 3 grades + link to full transcript
4. **Schedule widget** — upcoming class for each child
5. **Announcements widget** — school communications (optional — defer if no backend)

Each widget:
- Uses backend endpoints (may need new aggregation API in kiteclass-core under `/api/v1/parent/me/...`)
- Has empty state + error state
- Respects tenant branding (per Wave 4 GAP-037)
- Vietnamese copy

## Acceptance Criteria

- [ ] Parent dashboard has ≥3 content widgets (attendance, billing, grades as minimum)
- [ ] Each widget has loading + empty + error states
- [ ] Backend endpoints defined + documented in `documents/01-business/parent/api-contract.md`
- [ ] E2E: parent logs in → sees non-empty dashboard (mock data at minimum)
- [ ] UI audit re-score → ≥85/128 for parent dashboard screen
- [ ] Remove "Wave 5 widgets sẽ có" placeholder footer when widgets ship

## Related

- Audit: `documents/04-quality/audits/ui/ui-review-2026-04-19.md` §New Issues U-3
- Wave 2 PR: #337 (GAP-052a — parent portal identity + invitation MVP)
- Parent portal 3-layer docs: GAP-105 (may need extension for widgets)
- Parent portal backend scope: GAP-052-parent-portal.md (parent flag)

## Log

- 2026-04-19 — Flagged during UI audit catch-up. Wave 2 shipped as MVP, widgets planned Wave 5 but not yet in roadmap.
