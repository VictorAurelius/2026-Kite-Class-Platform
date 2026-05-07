# GAP-272i: Slug-availability backend endpoint for AI Branding Wizard Step 1

**Status:** 🔵 OPEN
**Priority:** 🟠 P1
**Domain:** Backend (kitehub-subscription) + Frontend wiring
**Found:** 2026-05-07 (Wave 32 REWORK Bucket A — `WelcomeStep.tsx` slug validation)
**Affects:** `kitehub-frontend` AI Branding Wizard Step 1 — slug uniqueness UX
**Related:** GAP-272 (parent — Track 2 port for ai-branding-wizard v2)

## Problem

The Wave 32 6-step wizard (Direction C, Step 1) requires real-time slug
availability validation as the user types — kit screens
`step1-welcome-{validating,conflict}.html` specify a debounced check that
returns either `available` or `conflict` with alternative suggestions.

State-check 2026-05-07:
- `kitehub-frontend/src/lib/api/endpoints.ts` has NO `slug-availability`
  entry under `branding` (only `uploadAsset`, `analyzeLogo`, `jobs`,
  `templates`, `applyTemplate`).
- `grep -rn "slug.*availab\|/slug\b" kitehub/kitehub-subscription/src/main/java`
  returns 0 results.
- No Flyway migration creates a `tenant_slug` index that would back this query.

Result: `WelcomeStep.tsx` ships a deterministic client-side `checkSlugStub`
function (3 hardcoded taken slugs, scripted suggestions) so the kit
sub-states (`validating` / `conflict` / `available`) are demonstrable.
The stub is flagged inline with `TODO(GAP-272i)` per
`gap-done-discipline.md` §3 PARTIAL exit-ramp.

## Root Cause

The Track 2 v2 wizard kit added the slug-validation flow as a new UX
pattern; the backend slug-availability endpoint was never specified or
shipped alongside the legacy 4-step wizard (which never had a slug step).

## Proposed Fix

1. **Backend (kitehub-subscription):** new endpoint
   `GET /api/v1/branding/slug-availability?slug={slug}` returning
   `{ available: boolean, suggestions: string[] }`.
   - Look up `tenant_slug` table (or `tenants.slug` column if denormalized).
   - When unavailable, generate 3-4 deterministic suggestions
     (`{slug}-2026`, `{slug}-edu`, `tt-{slug}`, `{slug}-vn`).
   - Rate-limit per IP to defend against slug-enumeration scraping.
2. **Endpoint registration:** add to `kitehub-frontend/src/lib/api/endpoints.ts`:
   ```ts
   branding: {
     // ...
     slugAvailability: (slug: string) => `${API_BASE}/branding/slug-availability?slug=${encodeURIComponent(slug)}`,
   }
   ```
3. **Frontend wiring:** replace `checkSlugStub` in
   `kitehub-frontend/src/components/branding/wizard/WelcomeStep.tsx`
   with a real `apiClient.get(...)` call. Remove the `TODO(GAP-272i)` and
   the `SLUG_STUB_TAKEN` / `SLUG_STUB_SUGGESTIONS_BY_BASE` constants.
4. **Tests:** add MSW handler + assertion that `WelcomeStep` displays
   server-suggested alternatives.
5. **Business rule entry:** add `BR-SLUG-001..003` to
   `documents/01-business/kitehub/branding/rules.md` — slug pattern,
   reservation rules, suggestion algorithm. Per `business-logic-review.md` §2.

## Acceptance Criteria

- [ ] `GET /api/v1/branding/slug-availability` endpoint shipped with controller +
      service + integration test.
- [ ] `endpoints.ts` lists the slug-availability route.
- [ ] `WelcomeStep.tsx` calls the real endpoint; `checkSlugStub` deleted.
- [ ] `SLUG_STUB_TAKEN` / `SLUG_STUB_SUGGESTIONS_BY_BASE` constants removed.
- [ ] Component test mocks the endpoint (MSW or `vi.mock`) and verifies
      both `available` and `conflict` paths render correctly.
- [ ] Rate-limit covers the new endpoint per
      `ai-branding-guidelines.md` §2.5 spirit (cost-bound external surface).
- [ ] Business-rules doc entries `BR-SLUG-001..003` added.

## Related

- Parent: GAP-272 — Track 2 port for ai-branding-wizard v2
- Wave plan: `documents/03-planning/waves/wave-2026-05-07-32-ai-branding-wizard-v2-rework.md` §4 Bucket A
- Rules: `.claude/rules/ai-branding-guidelines.md`, `.claude/rules/gap-done-discipline.md`
