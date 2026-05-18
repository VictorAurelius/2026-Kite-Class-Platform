# GAP-656: UI Coordinator — widget collision prereq + staggered first-login reveal

**Status:** 🟡 PARTIAL (80% — Wave 98 Bucket B0 ships foundation + 6 files; B5 wires actual FeedbackForm modal + B6 Zalo OA link)
**Priority:** 🔴 P0
**Domain:** Mixed (Frontend coordinator + Backend persona-aware state)
**Detected:** 2026-05-18 (Wave 98 prep — outside-in audit 3-agent convergence)
**Parent audit:** `documents/04-quality/audits/persona-review/2026-05-18-wave-98-cluster-b-beta-cohort-outside-in.md` F-NEW-2/4 + `2026-05-18-wave-98-cluster-b-failure-mode-matrix.md` M-NEW-7

## Problem

6 PARTIAL gaps Cluster B (GAP-538/539/540/541/542) đều ship UI surface độc lập 80-90% complete NHƯNG không có UI coordinator:

| Gap | UI surface | Mount location |
|---|---|---|
| GAP-539 | Beta disclaimer banner | Dashboard top (full-width) |
| GAP-538 | Day-1 onboarding checklist overlay | Center modal khi first-login |
| GAP-540 | Support widget (floating button) | Góc phải-dưới mobile + desktop |
| GAP-542 | Feedback widget (floating button) | Góc phải-dưới (SAME slot) |

**3 cụ thể problems:**
1. **Floating button collision** — GAP-540 + GAP-542 cả hai claim góc phải-dưới mobile screen ≤375px → physical overlap, button không click được
2. **First-login overload** — banner + onboarding modal + 2 floating buttons cùng mount cùng lúc → cognitive load quá mức → bounce rate tăng ~40% theo persona walkthrough
3. **Mobile ≤375px untested** — không component nào trong 4 đã được test Playwright ≤375px viewport regression; Zalo in-app WebView strips media queries

## Root Cause

Inside-out scope mỗi gap ship độc lập (per-gap completeness optimized). Không có:
- Shared `OnboardingPhase` state model (anonymous vs first-login vs day-1 vs day-7+)
- UI coordinator component decide widget nào mount + khi nào
- Mobile viewport regression test cho overlapping floating widgets

## Proposed Fix

### Step 1: OnboardingPhase shared state hook

`kitehub/kitehub-frontend/src/hooks/useOnboardingPhase.ts`:

```ts
type OnboardingPhase = 'anonymous' | 'first-login' | 'day-1' | 'day-7' | 'steady';

export function useOnboardingPhase(): OnboardingPhase {
  // Reads JWT claims (createdAt, lastLogin, role) + computes phase
  // Persists dismissal state per phase via httpOnly cookie (cross-tab/cross-browser)
}
```

### Step 2: Unified bottom-right `?` dropdown menu

`kitehub-frontend/src/components/support/SupportMenu.tsx` — single floating button `?` (góc phải-dưới) opening dropdown:
- "Hướng dẫn nhanh" → Vy/anonymous: `/help/anonymous`; logged-in: persona-aware route
- "Liên hệ hỗ trợ" → mailto:support@kitehub.me + Zalo OA link (post-GAP-660)
- "Gửi phản hồi" → in-app feedback form (replaces GAP-542 standalone widget)
- "Trạng thái beta" → `/beta-status`

Merges GAP-540 support widget + GAP-542 feedback widget vào 1 menu — eliminates collision.

### Step 3: Staggered first-login reveal

Order priority (1 visible at a time):
1. Banner (always-visible after dismissal check)
2. Onboarding checklist modal (dismiss → show next)
3. `?` button (always-visible after onboarding closed)

Implementation: `useOnboardingPhase()` returns current phase → each component conditionally renders.

### Step 4: Mobile ≤375px regression test

`kitehub-frontend/playwright/onboarding-mobile.spec.ts`:
- Viewport 375×812 + 360×640
- Verify banner + `?` button + onboarding modal each render correctly
- Verify no overlapping touch targets (≥44×44px per WCAG)
- Verify Zalo in-app WebView simulation (UA override)

### Step 5: Cross-tab dismiss sync

Banner dismiss state lưu trong httpOnly cookie (server-set sau client POST `/api/preferences/dismiss-banner`) thay vì sessionStorage → cross-tab + cross-browser sync (P2 Hằng's Zalo↔Chrome workflow).

## Acceptance Criteria

- [ ] `useOnboardingPhase()` hook implement + 5 phase types
- [ ] `SupportMenu` component thay thế GAP-540 + GAP-542 floating widgets
- [ ] Staggered reveal logic — 1 modal/banner active at a time
- [ ] Playwright spec `onboarding-mobile.spec.ts` PASS ≥375px + 360px viewports
- [ ] httpOnly cookie cross-tab dismiss sync verified (test 2-tab same browser + 2 browsers)
- [ ] GAP-540 + GAP-542 scope updates to reference this gap as prereq
- [ ] `cd kitehub/kitehub-frontend && pnpm test --run && pnpm build && pnpm lint` PASS

## Effort estimate

~1-1.5 wave bucket. Blocks GAP-540 + GAP-542 finishing strokes (parallel-unsafe — they need this coordinator first).

## Related

- **Parent audits:** outside-in 3-agent 2026-05-18 (persona F-NEW-2/4 + failure-mode M-NEW-7)
- **Blocks:** GAP-540 (support discoverability), GAP-542 (feedback channel)
- **Sister gaps:** GAP-538 (onboarding checklist), GAP-539 (banner) — both consume `useOnboardingPhase` hook
- **Wave 98 bucket:** B0 (PREREQ — blocks B5)

## Log

- **2026-05-18** — Initial filing post outside-in audit 3-agent convergence; Wave 98 Bucket B0 PREREQ.
- **2026-05-18** — Bucket B0 PARTIAL 80% ship. Files added:
  - `kitehub/kitehub-frontend/src/hooks/useOnboardingPhase.ts` (5-phase hook + JWT claim reader + cookie dismissal sync)
  - `kitehub/kitehub-frontend/src/components/support/SupportMenu.tsx` (Radix DropdownMenu, 4 items: Hướng dẫn / Liên hệ / Phản hồi / Trạng thái beta)
  - `kitehub/kitehub-frontend/src/components/onboarding/OnboardingCoordinator.tsx` (sequences banner + SupportMenu visibility per modal state)
  - `kitehub/kitehub-subscription/src/main/java/com/kitehub/subscription/preferences/controller/PreferencesController.java` (POST /api/v1/preferences/dismiss-banner-state — Set-Cookie 30-day, in-memory Phase 1)
  - `kitehub/kitehub-frontend/playwright/onboarding-mobile.spec.ts` (375×812 + 360×640 + Zalo WebView UA simulation)
  - `documents/01-business/kitehub/preferences/api-contract.md` (cross-layer contract per `contract-first-for-cross-layer.md`)
  - **Scope deviation logged:** PreferencesController placed in `kitehub-subscription` (NOT `kitehub-platform`) — `kitehub-platform` is shared library JAR (no REST surface); sister public-write controllers (FeedbackController, BetaAccessController) already live in subscription. No AC change.
  - Verify: FE lint PASS (warnings only, all pre-existing), FE build PASS, FE tests 774/774 PASS, BE compile + test-compile PASS.
  - Playwright spec written but not executed (Playwright requires browser binaries install + dev server — deferred; spec exercised by future CI).
  - Deferred to Bucket B5/B6: actual FeedbackForm modal wiring (currently placeholder), Zalo OA link wiring (currently TODO comment per GAP-660 unblock), persona-aware help routing beyond `/help/anonymous` + `/help` defaults.
  - Phase 2 (Wave 99+) deferred: user_preferences table persistence (currently in-memory ConcurrentHashMap).
- **2026-05-18 (PR #1548 merged)** — Post-merge audit-gate flagged: (a) api-contract-audit required for new endpoint `POST /api/v1/preferences/dismiss-banner-state` — DEFER to Wave 98 post-closure audit suite per `post-wave-audit-mandate.md` §2.2 (cadence ≤3 days post-wave-merge, applies at wave closure not per-bucket); (b) no test files for new Java code (PreferencesController) — Phase 2 follow-up (Wave 99+ when user_preferences table lands per gap §4 Step 4 in-memory ConcurrentHashMap → DB). Sync per `post-merge-sync-completeness.md` §4.
- **2026-05-18 (PR #1555 + #1557 merged)** — Wave 98 B5 + B6 consumed SupportMenu placeholders: B5 wired actual FeedbackForm Radix Dialog (Gửi phản hồi item); B6 wired actual Zalo OA item (added as 5th menu item between mailto + feedback). GAP-656 PARTIAL 80% unchanged (live verify still gated GAP-612 AWS per §5.4). Sync per `post-merge-sync-completeness.md` §4.
