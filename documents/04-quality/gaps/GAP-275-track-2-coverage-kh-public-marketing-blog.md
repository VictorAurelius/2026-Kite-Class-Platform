# GAP-275: Track 2 Coverage — KH public marketing + blog kit

**Status:** 🔵 OPEN
**Priority:** 🟡 P2 (UX growth — Prospects pre-tenant SaaS evaluation)
**Domain:** Frontend / Design System
**Found:** 2026-04-29 via audit §2.2
**Affects:** `kitehub-frontend/src/app/(public)/**` + KH marketing components

## Problem

5 KH public pages + public layout + ~12 marketing components with ❌ NO kit coverage. `kitehub-story v2` (Direction A) was deferred per Decision 3. This GAP partially replaces it with content-realistic marketing kit + MDX blog templates.

## Current State

| Path | Status |
|------|:------:|
| `(public)/page.tsx` | exists, R1 |
| `(public)/pricing/page.tsx` | exists, R1 |
| `(public)/blog/page.tsx` | exists, R1 (MDX-driven) |
| `(public)/blog/[slug]/page.tsx` | exists, R1 |
| `(public)/legal/dmca/page.tsx` | exists, plain |
| `(public)/layout.tsx` | exists, R1 |

## Proposed Fix

Create `ui_kits/kitehub-public/` HTML kit:
- SaaS marketing landing (hero + features + social proof + pricing CTA)
- Pricing page (tier comparison table + FAQ)
- Blog index template
- Blog post detail (MDX article layout with TOC + author + share)
- Legal DMCA template
- Public site shell (header + footer)

## Acceptance Criteria

- [ ] HTML kit `ui_kits/kitehub-public/` ≥105/128 across 5 screens
- [ ] Pricing tier comparison matches `documents/01-business/kitehub/pricing/rules.md`
- [ ] MDX blog template supports VN typography (font-feature-settings)
- [ ] Legal pages template usable for TOS/Privacy/Refund/etc. (BRD docs)
- [ ] Production ported ≥105/128
- [ ] WCAG AA + SEO meta tags
- [ ] Vietnamese-only

## Related

- Audit evidence: §2.2
- Decision context: Decision 3 (kitehub-story partial replacement)
- Existing BRD legal docs: `documents/00-brd/legal/*.md` (TOS/AUP/Privacy/Retention/Refund/Billing/ChildProtection — Phase 1 skeletons shipped)

## Effort estimate

~1-2 weeks (kit + port). Wave-pack candidate.

## Log

- **2026-04-29:** Filed from audit synthesis. Replaces deferred kitehub-story v2 partially.
