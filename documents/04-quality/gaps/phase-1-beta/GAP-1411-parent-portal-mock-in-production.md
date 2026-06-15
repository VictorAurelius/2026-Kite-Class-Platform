# GAP-1411: Parent portal renders mock data to real parents (mock-in-production)

**Status:** 🔵 OPEN
**Priority:** 🟠 P1
**Domain:** Frontend
**Found:** 2026-06-15 (hardcode-mock state-check, FE agent)
**Affects:** `kiteclass-frontend/src/components/parent/parent-mock-data.ts` + 7 `(dashboard)/parent/*` pages

## Problem

7 parent-portal pages import `parent-mock-data.ts` (141 LOC fixtures) and render them to real authenticated parents — NOT wired to API. Pages: `(dashboard)/parent/{page,grades,attendance,billing,...}`. **Billing/invoices fabricated = financial trust risk** (parent sees fake fees/payments). This is MOCK (unwired), not hardcode — fix = wire to real parent-facet API (KC-8 parent portal BE facets exist per campaign §4 KC-8 G1 PASS).

## Proposed Fix

Wire parent pages to real KC-8 parent-facet endpoints (`/me/children` + transcript/attendance/fees/conduct already G1-verified). Remove `parent-mock-data` import from render path (keep for storybook/test only if needed). Fail-loud (empty/error state) instead of mock fallback.

## Acceptance Criteria

- [ ] 7 parent pages fetch real data via parent-facet API (no `parent-mock-data` in render path)
- [ ] Billing/fees page shows real invoice data (or proper empty state) — no fabricated amounts
- [ ] `parent-mock-data.ts` import removed from production pages (test/storybook only)

## Related

- Umbrella: GAP-1410 · Audit: `2026-06-15-hardcode-mock-state-check.md`
- KC-8 parent portal (campaign §4 — BE facets G1 PASS); GAP-268 (teacher mock sibling)
