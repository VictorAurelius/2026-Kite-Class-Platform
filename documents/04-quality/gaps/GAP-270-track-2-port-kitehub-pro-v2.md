# GAP-270: Track 2 Port — kitehub-pro v2 → production Next.js

**Status:** 🟡 PARTIAL — Wave 31 SHIPPED 2026-05-06 (foundation + 4 page-cluster buckets); remaining visual regression baseline (GAP-270b) + E2E test (GAP-270c) + bundle-size verify + real `/api/instances/{id}/status` polling endpoint wiring (Bucket D mocked client-side)
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

- [x] Foundation primitives shipped (ThemeProvider + KPICard + Sparkline + CommandPalette + SuccessConfetti) — Bucket A PR #880, Decision B duplicate với storage key re-namespace `kitehub.dashboard.theme`
- [x] Customer dashboard home — subscription health KPIs + sparklines + tier-status card (Bucket A)
- [x] Billing — invoices list + G6 InvoiceDetail + G5 PaymentMethodSelector + tier upgrade CTA (Bucket B PR #877)
- [x] Branding hub — quota counter + G11 ThemePreview + 6-template gallery + wizard CTA placeholder (Bucket C PR #876, đã tồn tại từ trước Wave 31; Wave 31 redesign theo pro v2 tokens)
- [x] Instances list + detail với G9 InstanceLifecycleStatus (Bucket D PR #879 — mock client-side vì `/api/instances/{id}/status` chưa có; swap-site comment ghi sẵn)
- [x] Settings notifications + locale toggles (Bucket D)
- [x] WCAG AA preserved
- [x] Vietnamese-only
- [ ] All 24 screens ≥105/128 — chưa verify per-screen score (GAP-270b visual regression follow-up)
- [ ] Subscription health KPIs từ real `/api/subscription/health` endpoint — Bucket A mock với TODO inline (follow-up khi backend ship endpoint)
- [ ] Tier upgrade flow E2E — Bucket B ship UI; E2E test → GAP-270c
- [ ] G9 wired với real `/api/instances/{id}/status` polling — endpoint chưa có (GAP-270d backend follow-up)
- [ ] AI Branding wizard entry routes — placeholder href `/branding/wizard` đã có; Wave 32 (GAP-272) wires internals

## Related

- HTML prototype: `ui_kits/kitehub-pro-v2/`
- Component dependency: GAP-273 (G9 instance lifecycle, G10 payment timeline, G11 theme preview)
- Sister gap: GAP-272 (ai-branding-wizard-v2 port)

## Effort estimate

~1-2 weeks. Wave-pack candidate when sliced.

## Log

- **2026-05-06:** Wave 31 SHIPPED — 4 buckets parallel. Status flip 🔵 OPEN → 🟡 PARTIAL. PRs: #880 (Bucket A foundation + dashboard, 16 tests), #877 (Bucket B billing, 7 tests), #876 (Bucket C branding hub, 3 tests), #879 (Bucket D instances + settings, 18 tests). Side-PR #870 (Wave 31 plan) + #878 (Wave 32 plan PIPELINED). Final KH frontend: 509/509 tests pass, build clean, `/dashboard` 9.76 kB / 177 kB First Load JS. Foundation reuse decision: **B (duplicate)** — Wave 30 KC primitives copied verbatim với 1 storage-key re-namespace (`kiteclass.dashboard.theme` → `kitehub.dashboard.theme`); workspace factor deferred Wave 32+ nếu lock-step drift. Coordinator merge clean (0 conflicts vì B/C/D không touch `_shared/dashboard-foundation/types.ts`). 67th consecutive 0-clarification streak (4 agents 0-clarif). Wall-clock per agent: A 13min, B 8min, C 5.5min, D 11.9min — cumulative parallel ~13.5min. Follow-ups: GAP-270b visual regression baseline + GAP-270c E2E test + GAP-270d backend `/api/instances/{id}/status` endpoint.
- **2026-04-29:** Filed after user accepted Round 3 quality.

- **2026-05-11 (Wave 53 Phase 4 milestone audit — UI /128 ❌ NOT DONE-eligible):** Bucket A static-analysis audit (PR #1106) avg 107.8/128 (range 100-113); 3 screens <105 (branding/billing-pay/instances loading-empty-error). Carry-forward to existing GAP-429 umbrella (transient-state UX pattern: loading skeletons + empty states + error recovery) — coordinator confirmed NO new gap needed. Status stays 🟡 PARTIAL pending GAP-429 cluster closure.
