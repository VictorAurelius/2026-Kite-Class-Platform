# GAP-274: Track 2 Coverage — KC public marketing kit

**Status:** 🟢 DONE — kit scope hoàn tất (PR #2326 ship 5 screen + Wave ui-kits-100 Bucket C: external verify ≥105 + ConsentBanner spec + favicon spec). AC "Production routes ported" RE-SCOPED sang Track 2 umbrella (user approved 2026-06-11 plan PR #2327).
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

- [x] HTML kit shipped — `ui_kits/kiteclass-public/` (4 screen) + `landing-personal/` (landing) = 5 screen, ≥105/128. Evidence: PR #2326 + external verify `documents/04-quality/audits/ui-review/2026-06-11-kiteclass-public-landing-personal-external-verify.md` (108.8/128 avg, 5/5 ≥105 PASS).
- [x] Course catalog supports filter + search + persona-based recommendations — `kiteclass-public/screens/catalog.html` (search + filter cấp lớp/trình độ + sort + block "Gợi ý cho con anh/chị" persona-reco runtime).
- [x] Contact form with VN-realistic validation + Zalo integration option — `kiteclass-public/screens/contact.html` (regex SĐT `^0\d{9}$` + email optional + lời nhắn ≥10 + nút Zalo toggle theo `tenant.zaloUrl`).
- [x] ~~Production routes ported, ≥105/128 in production usage~~ → **RE-SCOPED** sang Track 2 port umbrella (production→kit back-port là multi-week port scope ngoài wave kit-coverage; user approved 2026-06-11 plan PR #2327). Kit = design baseline; port production = `wave-track-2-ui-kits-port-umbrella.md` queue (GAP-269/271).
- [x] WCAG AA preserved — đo trong comment đầu mỗi screen (catalog/detail/about/contact) + landing-personal `index.html`; cặp text-bearing chính ≥4.5:1.
- [x] Vietnamese-only — 100% tiếng Việt, VND format, VN sample data (Nguyễn Thị Hà / Trần Thị Hồng), Zalo culture. Không `John Doe`/`$`.
- [x] **ConsentBanner integrated on landing** per `BR-PDPL-CONSENT-001..004` — kit-design spec ship Wave ui-kits-100 Bucket C trong `landing-personal/README.md` §"ConsentBanner PDPL" (vị trí mount `(public)/layout.tsx` + gate analytics + privacy-by-default). Mockup: `kitehub-story-v2/consent-banner.html`. Production component đã ship Wave 23 Bucket BC. Cross-ref: GAP-353, GAP-368, GAP-1229 (favicon head spec cùng kit).

## Related

- Audit evidence: `documents/04-quality/audits/ui-review/2026-04-29-frontend-ui-coverage-audit.md` §2.1
- Decision context: `dossier/08-direction-decisions.md` Decision 3
- Sister gaps: GAP-275 (KH marketing), GAP-276 (auth flows)

## Effort estimate

~1-2 weeks (~1 wave for kit + ~1 wave for port). Wave-pack candidate when sliced into sections (hero / course-list / course-detail / about+contact).

## Log

- **2026-06-11 (Wave ui-kits-100 Bucket C — flip DONE, kit scope):** Kit `kiteclass-public/` (4 screen) + `landing-personal/` (landing) shipped PR #2326. Bucket C residual hoàn tất: (1) external verify 5 screen `documents/04-quality/audits/ui-review/2026-06-11-kiteclass-public-landing-personal-external-verify.md` — 108.8/128 avg, 5/5 ≥105 PASS, production-parity clean (port-from-production lens, 0 kit drift); (2) ConsentBanner PDPL spec + favicon head spec (GAP-1229 phần C) thêm vào `landing-personal/README.md`. **AC "Production routes ported, ≥105/128 in production usage" RE-SCOPED** sang Track 2 port umbrella (`wave-track-2-ui-kits-port-umbrella.md` queue, GAP-269/271) — production→kit back-port multi-week port scope ngoài wave kit-coverage; user approved 2026-06-11 plan PR #2327 per `wave-closure-scope-completeness.md` §2.5(c). Còn lại 6/6 AC kit ✅. CSV row + git mv → phase-2/closed/. Reviewer: @nguyenvankiet (Wave ui-kits-100 Bucket C agent).
- **2026-05-06 (Wave 23 Bucket E):** AC extended to require ConsentBanner integration on landing per PDPL 2023 (effective 2026-07-01). Cross-ref BR-PDPL-CONSENT-001..004 (Wave 23 Bucket A) + ConsentBanner production component (Wave 23 Bucket BC) + GAP-353 + GAP-368. Status remains 🔵 OPEN — port work itself unchanged; only AC scope enriched.
- **2026-04-29:** Filed from Wave UI Coverage Audit synthesis. Audit identified 5 pages + 14 components missing kit coverage.
