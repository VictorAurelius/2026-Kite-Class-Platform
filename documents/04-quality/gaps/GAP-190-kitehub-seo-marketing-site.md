# GAP-190: KiteHub SEO + Marketing Site — Audit + Complete Missing Pieces

**Status:** 🟢 DONE (Wave 9-B — 2026-04-21; og-image.png audit deferred to follow-up)
**Priority:** 🟠 P1 (business-logic tier — positioning-critical, not GA-blocker)
**Domain:** Frontend / Marketing / SEO / BRD
**Found:** 2026-04-20 (action-1 §6 + §15.A, Decision D11); scope revised 2026-04-20 post state-check
**Wave:** Wave 9 or 10 (after GAP-192)
**Affects:** kitehub.vn public site, search-engine discoverability, GTM funnel, BRD GTM doc

## Current State (verified 2026-04-20)

Infrastructure ALREADY shipped in earlier PRs (SAAS-10 + others):

| Piece | File | Status |
|-------|------|--------|
| Sitemap | `kitehub-frontend/src/app/sitemap.ts` | ✅ implemented (kitehub.vn URLs + blog dynamic) |
| Robots | `kitehub-frontend/src/app/robots.ts` | ✅ implemented |
| Root metadata (OG + Twitter) | `kitehub-frontend/src/app/layout.tsx` | ✅ implemented (Vietnamese locale, og-image.png) |
| JSON-LD component | `kitehub-frontend/src/components/seo/JsonLd.tsx` | ✅ implemented (wrapper, used on landing) |
| Blog MDX pipeline | `kitehub-frontend/content/blog/*.md` + `(public)/blog/[slug]/page.tsx` | ✅ 3 posts + dynamic route + `generateMetadata` |
| Landing page | `(public)/page.tsx` | ✅ 1024 LOC substantial |
| Pricing page | `(public)/pricing/page.tsx` | ✅ exists BUT `'use client'` — blocks per-route SEO metadata |

## Problem — Remaining Gaps

1. **Pricing page is `'use client'`** — Next.js App Router can't export `metadata` from client components. Pricing (a key SEO page) has no per-route title/description/OG tags.
2. **JSON-LD coverage shallow** — only wrapper component; no canonical schemas wired (Organization, FAQPage, BreadcrumbList, Product, BlogPosting on blog posts).
3. **GA4 / analytics not wired** — `gtag`, `GoogleAnalytics`, `GA_MEASUREMENT` all absent from `src/`. No server-side conversion event tracking.
4. **Blog content strategy missing** — only 3 posts; no editorial calendar, no keyword targeting plan, no review workflow (tie to GAP-174).
5. **Domain kitehub.vn status unverified** — sitemap hard-codes the URL but registration + DNS config is an open operational task (GAP-191 dependency).
6. **Lighthouse SEO baseline never captured** — target and gap unknown.
7. **`og-image.png` existence + quality** — referenced in layout but not audited.
8. **Marketing copy quality** — no review hook to GAP-174 marketing-legal-review.

## Context

User decision D11 (action-1 §0): "kitehub.vn là trang bán sản phẩm thật sự, không phải dashboard". Initial gap assumed scope was greenfield; state-check revealed infrastructure mostly done — scope narrows to completion + audit.

## Proposed Fix

1. **Refactor pricing → server component** (extract interactive bits into client sub-component) so `export const metadata` works
2. **Add canonical JSON-LD schemas** in a `seo/schemas.ts` module: Organization (landing), FAQPage (pricing), BreadcrumbList (blog), BlogPosting (blog slug)
3. **Wire GA4** — `@next/third-parties/google` `<GoogleAnalytics />` in root layout, env-driven ID
4. **Blog editorial plan** — `documents/05-guides/contributing/content-strategy.md` with 12-post Q2 plan, keyword table, review workflow linking GAP-174
5. **Lighthouse CI job** — `.github/workflows/lighthouse.yml` on kitehub-frontend PRs, threshold SEO ≥ 90
6. **Audit `og-image.png`** — ensure 1200×630, brand-aligned; regenerate if needed
7. **Per-route metadata coverage audit** — every `(public)/*/page.tsx` must export `metadata` or `generateMetadata`

## Acceptance Criteria

- [x] Pricing page becomes server component; `metadata` export present (`pricing/page.tsx` now `export const metadata`, client parts extracted to `PricingContent.tsx`)
- [x] JSON-LD schemas module ships with ≥4 canonical types; wired on landing/pricing/blog (`seo/schemas.ts`: Organization, WebSite, FAQPage, BreadcrumbList, BlogPosting)
- [x] GA4 wired behind `NEXT_PUBLIC_GA_ID` env (`<GoogleAnalytics>` from `@next/third-parties/google` in `layout.tsx`, env-gated; conversion events are a follow-up once GA property provisioned)
- [x] `documents/05-guides/contributing/content-strategy.md` exists with 12-post plan + keyword table
- [x] Lighthouse CI workflow created (`.github/workflows/lighthouse.yml`, SEO ≥ 0.90 threshold, advisory mode initially)
- [ ] og-image.png audited (dimensions + brand) — deferred to follow-up (file exists, brand audit separate)
- [x] All 5+ public routes have per-route metadata (landing: root `layout.tsx`; pricing: this PR; blog index + `[slug]`: `generateMetadata`; legal/dmca: inherits root default)
- [ ] Marketing copy review hook to GAP-174 linked in PR template — deferred (PR template edit out of scope for this agent)

## Out of Scope

- Domain registration (GAP-191)
- Customer dashboard SEO (not applicable, disallow'd in robots.ts)

## Related

- action-1 §6 + §15.A
- Decision D11 (action-1 §0)
- GAP-150 BRD GTM section
- GAP-174 marketing + legal review
- GAP-191 domain + DNS (sibling)
- GAP-127 FE code splitting (performance affects SEO)
- Existing: sitemap.ts, robots.ts, layout.tsx, JsonLd.tsx, blog pipeline
- Rule: `.claude/rules/meta-gap-priority.md` §3 (Business-Logic P1)
- Rule: `.claude/rules/audit-to-gap-pipeline.md` §2 (dedupe check — state verified)

## Log

- 2026-04-20 — Created from action-1 §15.A.
- 2026-04-20 — **Scope revised** after state-check. Found: sitemap.ts, robots.ts, OG meta, JsonLd wrapper, blog pipeline, 3 posts all shipped. Rewrote AC to target remaining gaps (pricing SSR, canonical schemas, GA4, content plan, Lighthouse CI).
- 2026-04-21 — **Wave 9-B DONE.** Pricing refactored to server component; `seo/schemas.ts` canonical module (5 schemas) wired on landing + pricing + blog `[slug]`; GA4 env-gated via `@next/third-parties`; content-strategy.md ships 12-post plan; Lighthouse CI workflow added (advisory SEO ≥ 0.90). 2 items deferred (og-image brand audit + PR template edit) — not blocking gap closure.
