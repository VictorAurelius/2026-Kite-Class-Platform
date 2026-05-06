# GAP-368: Production Legal Pages — `/legal/privacy`, `/legal/terms`, `/legal/cookies` (KH + KC)

**Status:** 🔵 OPEN
**Priority:** 🔴 P0 LEGAL (companion to GAP-353 — ConsentBanner cite Privacy + Cookie Policy links; without resolved targets, banner non-compliant per PDPL Art 12 right-to-information)
**Domain:** Compliance / Frontend / Legal
**Found:** 2026-05-06 (Wave 23 simulation-gap-finder cluster pass)
**Affects:** `kiteclass-frontend/src/app/(public)/legal/`, `kitehub-frontend/src/app/(public)/legal/`; downstream blocks **GAP-353** ConsentBanner UX defensibility

## Problem

PDPL 2023 Articles 11-13 + Decree 13/2023 mandate explicit consent collection AND accessible disclosure of:
- Privacy policy (Art 11 — data subject notice before processing)
- Cookie policy / categorized purpose disclosure (Art 13(3))
- Terms of service (contract basis per Art 11)

State-check 2026-05-06:

| Surface | Status |
|---|---|
| `documents/00-brd/privacy-policy.md` | ✅ exists (16-section skeleton, GAP-182 Phase 1 PR #691) |
| `documents/00-brd/terms-of-service.md` | ✅ exists (BRD skeleton) |
| Cookie policy doc | ❌ missing |
| `kitehub-frontend/src/app/(public)/legal/privacy/page.tsx` | ❌ missing |
| `kitehub-frontend/src/app/(public)/legal/terms/page.tsx` | ❌ missing |
| `kitehub-frontend/src/app/(public)/legal/cookies/page.tsx` | ❌ missing |
| `kiteclass-frontend/src/app/(public)/legal/` | ❌ folder missing |
| `kitehub-frontend/src/app/(public)/legal/dmca/page.tsx` | ✅ exists (only DMCA) |

GAP-353 ConsentBanner spec (Layer 2) cites links to "Privacy Policy" + "Cookie Policy" + "Customize". If these targets don't resolve, banner = dead links = non-compliant disclosure. PDPL effective 2026-07-01 (~8 weeks); MVP launch ~4-6 weeks precedes effective date.

## Why separate gap (not folded into GAP-353)

Per `audit-to-gap-pipeline.md` Step 2 — separate concerns:
- GAP-353 = consent **collection** (banner + business rules)
- GAP-368 = consent **disclosure** (legal pages for banner to link to)

Both must ship same wave for PDPL compliance, but scope-disjoint paths (GAP-353 = `_shared/components/ConsentBanner/`, GAP-368 = `(public)/legal/{privacy,terms,cookies}/page.tsx`). Per `meta-gap-priority.md` clean gap-per-concern rule.

## Proposed Fix

**Layer 1 — Cookie Policy BRD doc (NEW):**
Create `documents/00-brd/cookie-policy.md` (companion to existing privacy-policy.md + terms-of-service.md):
- Cookie categories (essential / analytics / marketing) with retention periods
- Third-party cookies (none currently, future-ready section)
- LocalStorage usage
- Re-prompt cadence (12 months default per `BR-PDPL-CONSENT-002`)
- Cross-link to privacy-policy.md §15 Cookie section

**Layer 2 — Production page ports** (6 routes total — KH + KC):

For each frontend (`kitehub-frontend` + `kiteclass-frontend`):
- `src/app/(public)/legal/privacy/page.tsx` — port `documents/00-brd/privacy-policy.md` §1-16 to user-facing markdown-rendered page
- `src/app/(public)/legal/terms/page.tsx` — port `documents/00-brd/terms-of-service.md`
- `src/app/(public)/legal/cookies/page.tsx` — port new cookie-policy.md

Each page header includes:
- "v1 — đang chờ legal counsel review" disclaimer (PDPL Phase 2 — tracked GAP-182/184)
- Last-updated date
- Effective-date placeholder (defaulting to today; updates on counsel sign-off)
- Vietnamese-first copy; EN translation deferred to GAP-182 Phase 2

**Layer 3 — Existing layout footer link** (in scope if minimal):
Add 3 footer links (Privacy / Terms / Cookies) in `(public)/layout.tsx` IF currently missing. Skip if BC bucket of Wave 23 owns layout.tsx — coordinate via plan §3 Bucket boundary.

## Acceptance Criteria

- [ ] `documents/00-brd/cookie-policy.md` skeleton (cookie-categories + retention + LocalStorage + revocation flow)
- [ ] `kitehub-frontend/src/app/(public)/legal/privacy/page.tsx` rendering BRD content
- [ ] `kitehub-frontend/src/app/(public)/legal/terms/page.tsx` rendering BRD content
- [ ] `kitehub-frontend/src/app/(public)/legal/cookies/page.tsx` rendering new cookie-policy
- [ ] `kiteclass-frontend/src/app/(public)/legal/privacy/page.tsx` rendering BRD content
- [ ] `kiteclass-frontend/src/app/(public)/legal/terms/page.tsx` rendering BRD content
- [ ] `kiteclass-frontend/src/app/(public)/legal/cookies/page.tsx` rendering new cookie-policy
- [ ] Each page has "v1 — counsel review pending GAP-182/184 Phase 2" header disclaimer
- [ ] Each page Vietnamese-first; EN deferred to GAP-182 Phase 2
- [ ] WCAG AA: heading hierarchy, link contrast, semantic HTML
- [ ] Responsive: readable on mobile (PDPL doesn't mandate but accessibility expectation)
- [ ] Cross-link from each page to others (privacy → terms → cookies → privacy chain)
- [ ] GAP-353 ConsentBanner can resolve "Privacy Policy" + "Cookie Policy" links to these routes
- [ ] Routes registered in Next.js sitemap (or sitemap.ts) — discoverable for SEO + PDPL Art 13 right-to-information

## Related

- **Companion gap:** GAP-353 (ConsentBanner — disclosure target consumer)
- **BRD source:** GAP-182 PARTIAL (Privacy Policy BRD §15 Cookie section), GAP-184 PARTIAL (Data Retention)
- **Phase 2 follow-up:** GAP-182 + GAP-184 Phase 2 (legal counsel review + EN translation + DPO designation + MPS A05 consultation)
- **DSAR intake form:** GAP-353c (filed at Wave 23 closure — public form for PDPL Art 14 rights exercise)
- **DPIA documentation:** GAP-353d (filed at Wave 23 closure — Decree 13/2023 Art 24-30 if processing >100k subjects)
- **Existing legal page:** `kitehub-frontend/src/app/(public)/legal/dmca/page.tsx` (DMCA — pattern reference)

## Effort estimate

~10h. 1 cookie-policy.md doc creation (~2h) + 6 page.tsx creations (~1h each = 6h) + sitemap registration + footer link (~2h). Single agent bucket; pair-wave with GAP-353 buckets.

## Log

- **2026-05-06:** Filed at Wave 23 plan PR. Surfaced by simulation-gap-finder cluster pass focused on PDPL/legal/data-collection axis around GAP-353. Hard dependency on banner UX defensibility — must ship same wave as GAP-353 for MVP legal compliance.
