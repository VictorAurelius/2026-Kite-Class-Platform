# GAP-190: KiteHub SEO + Marketing Site

**Status:** 🔵 OPEN
**Priority:** 🟠 P1 (business-logic tier — positioning-critical, not GA-blocker)
**Domain:** Frontend / Marketing / SEO / BRD
**Found:** 2026-04-20 (action-1 §6 + §15.A, Decision D11)
**Wave:** Wave 9 or 10 (after GAP-192)
**Affects:** kitehub.vn public site, search-engine discoverability, GTM funnel, BRD GTM doc

## Problem

kitehub.vn is currently framed as a dashboard / control plane. Decision D11 (action-1 §0) reclassified it as "trang bán sản phẩm thật sự":
- No SEO meta tags, Open Graph, structured data (schema.org/Product, FAQ, Organization)
- No blog / MDX content pipeline — required for inbound SEO + education-market content marketing
- No sitemap.xml / robots.txt strategy
- No landing-page CTA hierarchy tested (hero → features → pricing → trial)
- Marketing copy not tracked under review standard (ref: `output-review-mandate.md` VIOLATION: marketing copy)

Blocks GAP-150 GTM BRD doc — GTM plan assumes an actual acquisition surface.

## Context

User decision D11 + session Q&A (action-1 lines around 227–230). Related gaps:
- GAP-150 BRD docs completion (GTM section depends on this)
- GAP-038 developer API docs/SDK (technical marketing adjacency)
- GAP-174 marketing + legal review (review standard for copy)

## Proposed Fix

1. **Information architecture** — sitemap: `/` (hero), `/features`, `/pricing`, `/trial`, `/blog/*`, `/docs/*`, `/about`, `/contact`.
2. **SEO foundation**
   - `<meta>` per route + Open Graph + Twitter cards
   - schema.org JSON-LD (Organization, Product, FAQPage, BlogPosting)
   - `sitemap.xml` auto-generated
   - `robots.txt` per env
3. **Blog pipeline** — MDX under `kitehub-frontend/content/blog/` + listing page + RSS
4. **Landing hero** — 6-component layout (hero, logos, features, testimonial, pricing teaser, CTA)
5. **Analytics** — GA4 + server-side conversion events (trial-signup, contact-form)
6. **Review standard** — attach to `GAP-174 marketing + legal review` for all copy

## Acceptance Criteria

- [ ] Public routes rendered with SSR/SSG (not client-only) for crawlability
- [ ] Lighthouse SEO score ≥ 90 on landing + pricing
- [ ] Blog can publish MDX with front-matter (title, description, author, date, tags)
- [ ] sitemap.xml + robots.txt live
- [ ] Open Graph preview verified on Facebook/Twitter debuggers
- [ ] GA4 events fired for trial-signup + contact-form
- [ ] Marketing copy passes `GAP-174` review before merge

## Related

- action-1 §6 + §15.A
- Decision D11 (action-1 §0)
- GAP-150 BRD GTM section
- GAP-174 marketing + legal review
- GAP-191 domain + DNS (sister-gap for kitehub.vn)
- Rule: `.claude/rules/meta-gap-priority.md` §3 (Business-Logic P1)

## Log

- 2026-04-20 — Created from action-1 §15.A.
