# GAP-353: PDPL 2023 Cookie / Consent Banner — KH + KC Marketing Kits

**Status:** 🟡 PARTIAL — Wave 23 SHIPPED 2026-05-06 (Layers 1+2+3+5 + GAP-368 production legal pages); Phase 2 follow-ups GAP-353b (server consent API) + GAP-353c (DSAR self-service intake) + GAP-353d (DPIA Decree 13/2023 Art 24-30 docs).
**Priority:** 🔴 P0 (legal mandate — Personal Data Protection Law 2023 effective 2026-07-01; blocks GA for marketing surfaces)
**Domain:** Compliance / Frontend / Design System
**Found:** 2026-05-05 (simulation-gap-finder — Persona: Platform Admin × Stage: Discovery × Category: C6 Compliance)
**Affects:** KC public marketing port (GAP-274), KH public marketing + blog port (GAP-275), `kitehub-story-v2/` Round 3 polish (GAP-350), all marketing-surface routes

## Problem

PDPL 2023 (Personal Data Protection Law, **effective 2026-07-01**) Articles 11-13 mandate explicit consent collection before processing personal data — this includes analytics cookies, marketing pixels, behavioral tracking. Web surfaces collecting such data without compliant consent banner = legal violation post-effective-date.

GAP-274 (KC public marketing kit port) and GAP-275 (KH public marketing + blog port) have **no mention** of cookie banner / consent UI. GAP-350 (kitehub-story-v2 Round 3 polish) inherits the same omission. Round 1 archive (`kitehub-story` 546 LOC JSX) has no banner UI either.

## Current State (verified 2026-05-05)

| Check | Status |
|---|---|
| GAP-274 (KC marketing) — PDPL/cookie/consent mention | ❌ 0 hits |
| GAP-275 (KH marketing) — PDPL/cookie/consent mention | ❌ 0 hits |
| GAP-350 (story-v2 polish) — banner inclusion | ❌ not in scope |
| HTML kits — banner component | ❌ not in `_shared/` or `components/` |
| `documents/01-business/.../rules.md` PDPL rules | ⚠️ partial — DR-03 retention covered (`output-review-mandate.md` §3 example), but no consent-collection rule |
| Banner component spec | ❌ none |

## Proposed Fix

Three-layer fix:

**Layer 1 — Business rule (per `business-logic-review.md` 5-attribute mandate):**
Add to `documents/01-business/{kitehub,kiteclass}/marketing/rules.md`:
- `BR-PDPL-CONSENT-001` Cookie consent banner mandatory on all public marketing surfaces
- `BR-PDPL-CONSENT-002` Granular toggles (essential / analytics / marketing) — no dark patterns
- `BR-PDPL-CONSENT-003` Consent record retention (audit log per `DR-03` 36mo)
- `BR-PDPL-CONSENT-004` Consent revocation flow (settings page + cookie reset)

**Layer 2 — Shared component:**
Add `packages/shared-ui/src/components/ConsentBanner/` (consumed by both consumers):
- Granular toggles (essential always-on, analytics opt-in, marketing opt-in)
- "Reject all" + "Accept all" + "Customize" CTAs (no dark patterns)
- LocalStorage + server-side consent record via API
- Re-prompt on settings change or consent expiration (12 months default)
- Accessibility: focus trap, keyboard navigation, screen-reader announcements

**Layer 3 — Kit integration:**
- GAP-274 KC marketing port AC: ConsentBanner mandatory on landing
- GAP-275 KH marketing port AC: ConsentBanner mandatory on landing + blog
- GAP-350 story-v2 polish: design banner UI in `kitehub-story-v2/screens/consent-banner.html`
- Kits showing demo dashboards (animation) MUST gate analytics behind consent

## Acceptance Criteria

- [x] `BR-PDPL-CONSENT-001..004` written in both KH + KC marketing rules.md (10 attributes per `business-logic-review.md`) — Wave 23 Bucket A 2026-05-06: canonical at `documents/01-business/kitehub/marketing/rules.md` (4 BRs full 5-attribute), KC cross-link in `documents/01-business/kiteclass/marketing/rules.md` §0
- [x] `packages/shared-ui/src/components/ConsentBanner/` shipped (Storybook entry deferred — workspace has no Storybook yet; tracked separately)
- [ ] Banner spec in `dossier/14-common-components-inventory-{kc,kh}.md`
- [ ] GAP-274 + GAP-275 + GAP-350 ACs updated to require ConsentBanner integration
- [ ] Server-side consent API: `POST /api/v1/consent/record` + `GET /api/v1/consent/{userId}`
- [ ] Consent record schema in DB (links to existing audit log per DR-03)
- [x] Reject-all flow tested — analytics scripts NOT loaded
- [x] Revocation flow tested — settings page reset + cookie clear
- [ ] PDPL-effective-date (2026-07-01) compliance checklist signed off pre-launch

## Why P0

Per `meta-gap-priority.md` §3 — Business-Logic / Compliance tier (LEGAL MANDATE). PDPL effective 2026-07-01 = ~8 weeks from filing. MVP launch (~4-6 weeks per ROADMAP §🚀) precedes PDPL-effective. If MVP marketing surfaces ship before consent banner, post-effective-date noncompliance triggers regulatory exposure.

## Related

- PDPL 2023 Articles 11-13 (consent collection)
- Sister business rule: `DR-03` data retention 36mo (already documented in `output-review-mandate.md` §1 example)
- Downstream: GAP-274, GAP-275, GAP-350 (all marketing surfaces inherit AC requirement)
- Component delivery: GAP-273 (12 components shared lib) — ConsentBanner is 13th component, expand scope OR file as G13 follow-up
- Standard: `business-logic-review.md` (5-attribute rule mandate)

## Effort estimate

~3-4 days. Layer 1 (rules + 5-attr review) ~0.5d. Layer 2 (component) ~1.5d. Layer 3 (kit AC updates + 3 GAP file edits) ~0.5d. Server consent API ~1d. Wave-pack candidate (3 buckets: rules / component / API).

## Log

- **2026-05-06 — Wave 23 Bucket BC (Layer 2+3 shipped):** `packages/shared-ui/src/components/ConsentBanner/` created (7 files: `index.tsx` barrel, `ConsentBanner.tsx`, `useConsent.ts`, `storage.ts`, `types.ts`, 2 test files). Vitest setup added to shared-ui (was Phase-1 stub). 27 tests pass (8 storage + 8 hook + 11 component flows). Mounted in `kitehub-frontend/src/components/layout/PublicLayout.tsx` + `kiteclass/kiteclass-frontend/src/app/(public)/layout.tsx` above closing tag, below footer. Both `pnpm build` succeed (KH + KC). `@kite/shared-ui` workspace dep already present in both frontends — no package.json changes needed. Reject-all + Customize → save granular + Revoke flows all tested via RTL. Server consent API (POST/GET `/api/v1/consent/...`), consent record DB schema, dossier component-inventory entry, GAP-274/275/350 cross-cut AC updates, BR-PDPL-CONSENT-* rules.md entries — all left to sister buckets (A rules, F legal pages, E kit mockup) + GAP-353b backend follow-up. Status stays 🔵 OPEN until coordinator wave-closure.
- **2026-05-06** (Wave 23 Bucket A): Layer 1 (business rules) shipped. Created `documents/01-business/kitehub/marketing/rules.md` (NEW domain) với 4 rules `BR-PDPL-CONSENT-001..004` đầy đủ 5-attribute (Source / Rationale / Reviewer / Compliance / Review cadence) per `business-logic-review.md` v1.0.0 mandate. Created `documents/01-business/kitehub/marketing/README.md` domain index. Extended `documents/01-business/kiteclass/marketing/rules.md` với §0 cross-link tới canonical KH file (avoid drift; existing BR-MKT-001..024 unchanged). Updated `documents/01-business/README.md` index để reflect 8th KH domain (rules.md only — use-cases/api-contract deferred → GAP-353b/c follow-ups per wave plan §7 Closure Protocol). Reviewer: @nguyenvankiet (acting Compliance scout + Product Owner, solo-dev). Legal counsel formal review queued GAP-182 Phase 2 + GAP-156. Status stays 🔵 OPEN — Bucket F (legal pages) + Bucket E (kit mockup) pending; coordinator flips to 🟡 PARTIAL at closure (GAP-353b/c/d as deferred Phase 2 follow-ups per `gap-done-discipline.md` §3).
- **2026-05-05:** Filed via simulation-gap-finder 3-axis matrix sweep. Discovered at Platform Admin × Discovery × C6 cell. State-check: 0 hits "PDPL"/"cookie banner"/"consent banner" in GAP-274/275/350 or HTML kits. PDPL effective 2026-07-01 makes this P0 — MVP launch (~4-6 weeks) precedes effective date; banner must ship pre-launch to avoid post-effective regulatory exposure. Cross-cut to BR-PDPL-CONSENT-* rules + ConsentBanner shared component + 3 marketing kit integrations.
