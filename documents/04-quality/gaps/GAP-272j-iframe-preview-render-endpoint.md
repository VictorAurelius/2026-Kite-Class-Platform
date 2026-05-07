# GAP-272j: Iframe live preview render endpoint for AI Branding Wizard Step 6

**Status:** 🟢 DONE 2026-05-07 (Wave 34 Bucket B — PR #906 + Bucket D PR #910)
**Priority:** 🟠 P1
**Domain:** Backend (kitehub-branding) + Frontend wiring
**Found:** 2026-05-07 (Wave 32 REWORK Bucket C — `Step6Preview.tsx`)
**Affects:** AI Branding Wizard Step 6 preview UX — iframe live preview of generated theme
**Related:** GAP-272 (parent — Track 2 port for ai-branding-wizard v2)

## Problem

Step 6 main preview (`step6-preview-default.html` kit screen — HEADLINE 122/128)
specifies an iframe rendering the user's selected template + brand state,
showing exactly what the deployed instance will look like.

State-check 2026-05-07:
- No `/api/.../preview` endpoint serving rendered HTML for branded preview
- No public preview URL pattern in `kitehub-branding` controllers
- `Step6Preview.tsx` ships with `<iframe src={previewUrl} />` accepting a
  `previewUrl` prop; defaults to a `data:text/html,...` URI showing static
  sample HTML with the selected brand colors

Inline TODO: `TODO(GAP-272j): wire to real preview render endpoint`.

## Root Cause

Direction C v2 wizard introduces in-flow preview before deploy. Legacy
4-step wizard didn't have iframe preview — user committed blind via
"Generate" then reviewed deployed result. New UX requires backend to
render preview HTML.

## Proposed Fix

1. **Backend (kitehub-branding):** new endpoint
   `GET /api/v1/branding/jobs/{jobId}/preview` (text/html)
   - Renders Thymeleaf template with brand colors + logo URL + sample
     content (heading, button, card)
   - Cached 5min per jobId
   - WCAG-AA contrast verified server-side before serving
2. **Frontend wiring:** `Step6Preview` consumer passes `previewUrl =
   /api/v1/branding/jobs/${jobId}/preview` instead of data: URI
3. **CORS:** allow iframe from same origin (Next.js app)

## Acceptance Criteria

- [x] Backend endpoint renders preview HTML — `PreviewController` + Thymeleaf template `wizard/preview.html` (Bucket B)
- [x] Frontend wired to real endpoint — `usePreview` hook + `<iframe src={/api/v1/branding/jobs/${jobId}/preview}>` (Bucket D)
- [x] `X-Frame-Options: SAMEORIGIN` + `Content-Security-Policy: frame-ancestors 'self'` headers (Bucket B)
- [x] Test: `PreviewControllerTest` verifies HTML response with brand colour CSS variables; testids `[data-testid='preview-hero']` + `[data-testid='preview-card']` for FE assertion

## Log

- **2026-05-07:** Wave 34 Bucket B (PR #906) shipped Thymeleaf-rendered preview endpoint with iframe-safe headers. Bucket D (PR #910) replaced Wave 32 v1 `data:text/html` URI with v1 endpoint path. WCAG-AA contrast verification deferred to GAP-226 server-side measurement infrastructure (out-of-scope for 272j shape).

## Related

- GAP-272 (parent)
- Wave 32 rework Bucket C (PR #888) — Step 6 preview scaffolding
