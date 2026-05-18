# GAP-274: Track 2 Coverage — KC public marketing kit

**Status:** 🔵 OPEN
**Priority:** 🟡 P2 (UX growth — Prospects pre-tenant + course-discovery)
**Domain:** Frontend / Design System
**Found:** 2026-04-29 via `documents/04-quality/audits/ui-review/2026-04-29-frontend-ui-coverage-audit.md` §2.1
**Affects:** `kiteclass-frontend/src/app/(public)/**` + `kiteclass-frontend/src/components/{landing,sections,public}/**`

## Problem

Wave UI Coverage Audit identified 5 KC public marketing pages + 14 marketing components with ❌ NO HTML kit coverage. Direction A (kitehub-story marketing) was deliberately deferred per `dossier/08-direction-decisions.md` Decision 3, but KC marketing was never in R2/R3 scope.

## Current State (verified 2026-04-29 via audit)

KC `(public)/` routes EXIST but visually predate Round 2 design system (~73/128 R1 baseline).

| Path | LOC | Status |
|------|----:|:------:|
| `(public)/page.tsx` | ~12 | exists, R1 design |
| `(public)/about/page.tsx` | ~8 | exists, R1 |
| `(public)/catalog/page.tsx` | ~14 | exists, R1 |
| `(public)/catalog/[id]/page.tsx` | ~18 | exists, R1 |
| `(public)/contact/page.tsx` | ~16 | exists, R1 |

Plus `components/landing/CourseCard.tsx`, 12 `sections/*.tsx`, 2 `public/*.tsx` — all unstyled per R2 standard.

## Proposed Fix

Create `documents/02-architecture/design-system/ui_kits/kiteclass-public/` HTML kit following R2/R3 pattern. Then port to production via Track 2.

**Phase 1 (kit):** ~5 marketing screens × 4-6 states each. Designer references kitehub-story v2 archived (Direction A) for marketing tone reference.

**Phase 2 (port):** redesign 5 production pages + extract reusable marketing components.

## Acceptance Criteria

- [ ] HTML kit `ui_kits/kiteclass-public/` shipped with 5 screens ≥105/128
- [ ] Course catalog supports filter + search + persona-based recommendations
- [ ] Contact form with VN-realistic validation + Zalo integration option
- [ ] Production routes ported, ≥105/128 in production usage
- [ ] WCAG AA preserved
- [ ] Vietnamese-only
- [ ] **ConsentBanner integrated on landing** per `BR-PDPL-CONSENT-001..004` (Wave 23 Bucket A) — production component shipped Wave 23 Bucket BC (`packages/shared-ui/src/components/ConsentBanner/`). Cross-ref: GAP-353 (banner spec), GAP-368 (production legal pages). Banner mounts in `(public)/layout.tsx` and gates analytics/marketing scripts behind explicit consent before PDPL effective date 2026-07-01.

## Related

- Audit evidence: `documents/04-quality/audits/ui-review/2026-04-29-frontend-ui-coverage-audit.md` §2.1
- Decision context: `dossier/08-direction-decisions.md` Decision 3
- Sister gaps: GAP-275 (KH marketing), GAP-276 (auth flows)

## Effort estimate

~1-2 weeks (~1 wave for kit + ~1 wave for port). Wave-pack candidate when sliced into sections (hero / course-list / course-detail / about+contact).

## Log

- **2026-05-06 (Wave 23 Bucket E):** AC extended to require ConsentBanner integration on landing per PDPL 2023 (effective 2026-07-01). Cross-ref BR-PDPL-CONSENT-001..004 (Wave 23 Bucket A) + ConsentBanner production component (Wave 23 Bucket BC) + GAP-353 + GAP-368. Status remains 🔵 OPEN — port work itself unchanged; only AC scope enriched.
- **2026-04-29:** Filed from Wave UI Coverage Audit synthesis. Audit identified 5 pages + 14 components missing kit coverage.
