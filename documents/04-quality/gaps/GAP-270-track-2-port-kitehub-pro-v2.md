# GAP-270: Track 2 Port — kitehub-pro v2 → production Next.js

**Status:** 🔵 OPEN
**Priority:** 🟡 P2 (UX growth — P2 Center Owner KH side, SaaS control plane)
**Domain:** Frontend
**Found:** 2026-04-29
**Affects:** `kitehub-frontend/src/app/(customer)/` — owner self-service routes

## Problem

HTML prototype `kitehub-pro-v2/` (avg 107.8/128, 24 screens, R2 PR #673) covers KH SaaS control plane: billing + branding hub + instance lifecycle + customer dashboard. Production routes exist but predate Round 2.

## Current State

`kitehub-frontend/src/app/(customer)/` exists. Owner self-service routes need redesign per HTML prototype.

## Proposed Fix

Port 24 KH SaaS dashboard screens.

**Scope (per HTML prototype):**
- Customer dashboard (subscription health + usage KPIs)
- Billing (invoices + payment methods + tier upgrade)
- Branding hub (theme + logo + AI Branding wizard entry)
- Instance lifecycle (G9 component — see GAP-273)
- Domain management
- Team members (multi-tenant admin invite)
- Settings + profile

## Acceptance Criteria

- [ ] All 24 screens ≥105/128
- [ ] G9 instance lifecycle component imported (post-GAP-273)
- [ ] Subscription health KPIs from existing kitehub-subscription endpoints
- [ ] Tier upgrade flow E2E (existing rate-limit + tier-multiplier per GAP-259/260)
- [ ] AI Branding wizard entry routes to ported wizard (GAP-272)
- [ ] WCAG AA preserved
- [ ] Vietnamese-only

## Related

- HTML prototype: `ui_kits/kitehub-pro-v2/`
- Component dependency: GAP-273 (G9 instance lifecycle, G10 payment timeline, G11 theme preview)
- Sister gap: GAP-272 (ai-branding-wizard-v2 port)

## Effort estimate

~1-2 weeks. Wave-pack candidate when sliced.

## Log

- **2026-04-29:** Filed after user accepted Round 3 quality.
