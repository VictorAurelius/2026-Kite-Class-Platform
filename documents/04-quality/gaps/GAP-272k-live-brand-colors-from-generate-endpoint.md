# GAP-272k: Live brand colors from generate endpoint for G11 ThemePreview

**Status:** 🔵 OPEN
**Priority:** 🟠 P1
**Domain:** Backend (kitehub-branding) + Frontend wiring
**Found:** 2026-05-07 (Wave 32 REWORK Bucket C — `Step6Preview.tsx` G11 integration)
**Affects:** AI Branding Wizard Step 6 — G11 ThemePreview must receive LIVE colors derived from selected template + generation result
**Related:** GAP-272 (parent — Track 2 port for ai-branding-wizard v2)

## Problem

Step 6 preview integrates `@kite/shared-ui` G11 `ThemePreview` component to
show the user's chosen brand colors before deploy. Per Wave 32 rework §4
Bucket C delta, ThemePreview MUST receive LIVE colors from wizard state /
generation result, NOT hardcoded `MOCK_BRAND` (v1 violation).

State-check 2026-05-07:
- `kitehub/kitehub-branding/src/main/java/.../AIBrandingController.java`
  generate endpoint (`POST /branding/jobs`) returns job ID; does NOT
  return derived brand colors in response payload
- No endpoint returns `{ primary, secondary, background, foreground }` or
  similar structured color set per job

Result: `Step6Preview.tsx` derives sample colors from selected template ID
deterministically (TEMPLATE_TO_COLORS map) as a stand-in. Inline TODO:
`TODO(GAP-272k): wire live brand colors from generate endpoint`.

## Root Cause

Direction C v2 wizard introduces visible color preview as a separate step
from generate. Backend generate endpoint focuses on job creation, not
exposing the derived color palette. Color derivation logic exists internally
(LogoAnalyzer + template params) but isn't surfaced via API.

## Proposed Fix

1. **Backend:** extend `GET /api/v1/branding/jobs/{jobId}` response to include
   `colors: { primary, secondary, background, foreground, accent }` populated
   once analyze stage completes
2. **Frontend wiring:** `Step6Preview` reads `colors` from job query, passes
   to G11 `ThemePreview brandColors={...}`
3. **Fallback:** if colors absent (job still analyzing), use template default
   from `TEMPLATE_TO_COLORS` map until live values arrive

## Acceptance Criteria

- [ ] Job DTO includes `colors` field after analyze complete
- [ ] Frontend reads colors from job query, removes `TEMPLATE_TO_COLORS`
      fallback (or relegates it to "still analyzing" state)
- [ ] Test: integration test job → analyze → response colors populated

## Related

- GAP-272 (parent)
- Wave 32 rework Bucket C (PR #888) — Step 6 preview G11 integration
