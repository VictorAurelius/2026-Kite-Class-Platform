# GAP-539: Beta disclaimer banner + /beta-status page

**Status:** 🟡 PARTIAL — Wave 78 Bucket B code+tests shipped; live verify gated on next deploy
**Priority:** 🔴 P0
**Domain:** Frontend
**Detected:** 2026-05-14
**Related PRs:** Wave 78 Bucket B (this PR) — `BetaDisclaimerBanner` + `/beta-status` page + `BetaStatusController` (static MVP markdown source)
**Related Docs:** `documents/03-planning/waves/wave-2026-05-14-78-beta-invite-launch-retain.md`

## Current State (verified 2026-05-14)

| Piece | File / Path | Status |
|-------|-------------|--------|
| Beta disclaimer banner FE component | `kitehub/kitehub-frontend/src/components/layout/beta-disclaimer-banner.tsx` | ❌ missing |
| `/beta-status` public route | `kitehub/kitehub-frontend/src/app/(public)/beta-status/page.tsx` | ❌ missing |
| Cookie persist mechanism cho dismissible banner | `kitehub/kitehub-frontend/src/lib/cookies/` | partial — generic cookie lib có thể có sẵn nhưng banner-specific key chưa |
| api-contract.md cho `/api/v1/beta-status` | `documents/01-business/beta-status/api-contract.md` | ❌ missing |
| Beta status content (markdown payload) | `documents/05-guides/operations/beta-status-content.md` | ❌ missing |

**Grep commands run:**
```bash
find kitehub/kitehub-frontend/src -name "*beta-disclaimer*" -o -name "*beta-status*"  # 0 matches
ls documents/01-business/beta-status/ 2>&1                                              # folder absent
```

## Problem

Phase 1 BETA invite cần truyền tải rõ ràng cho user: (1) đây là beta — data có thể reset; (2) phản hồi gửi support@; (3) status page liệt kê known issues + announcements. Hiện tại không có banner persistent (dismissible) trên dashboard + không có `/beta-status` route → user không biết môi trường là beta, dễ bị surprise khi data reset hoặc service degrade.

## Context

Outside-in audit 2026-05-14 (N2 finding) — Tier 1 beta tenant trust signal cần: banner thấy được + status page link. Comparable SaaS (Linear, Vercel, Stripe early access) đều có banner "Beta" + `/status` route public.

## Evidence

- Outside-in audit 2026-05-14 N2 finding
- Inside-out audit không catch vì developer perspective không cần (dev đã biết môi trường là beta)
- Phase 1 BETA target tenant = real user (P2 Trung tâm Owner + P3 Manager) — không phải dev

## Proposed Fix

1. Bucket 0 Foundation: `documents/01-business/beta-status/api-contract.md` CREATE với `GET /api/v1/beta-status` (response: markdown payload + last_updated timestamp)
2. BE static endpoint trong kitehub-subscription HOẶC kitehub-platform: serve markdown content từ `documents/05-guides/operations/beta-status-content.md` (file-system read, hoặc embedded resource)
3. FE banner component: `kitehub-frontend/src/components/layout/beta-disclaimer-banner.tsx`
   - Render trên dashboard layout
   - Dismissible button → set cookie `beta_disclaimer_dismissed_v1=true` (versioned key để bump khi nội dung thay đổi)
   - Link đến `/beta-status`
4. FE public route: `kitehub-frontend/src/app/(public)/beta-status/page.tsx`
   - SSR fetch markdown content từ `/api/v1/beta-status`
   - Render markdown qua existing markdown renderer
5. Content (static MVP): `documents/05-guides/operations/beta-status-content.md`
   - Heading "Beta status — Phase 1"
   - Sections: Current status / Known issues / Recent announcements / Support contact (support@kitehub.me)
   - Manual update khi có outage; live status (P0 incident → manual edit + redeploy)

## Acceptance Criteria

- [x] api-contract.md cho `/api/v1/beta-status` ship trong Bucket 0 Foundation (Wave 78 Bucket 0 PR #1349)
- [x] Beta disclaimer banner hiển thị trên `/onboarding` page (initial scope); cross-cuts dashboard layout integration deferred to follow-up Bucket A FE polish
- [x] Banner dismissible với cookie persist (`kitehub_beta_disclaimer_dismissed`, Max-Age 1y; versioned key for future content bump)
- [x] `/beta-status` public route accessible (no auth required) — SSR renders Vietnamese markdown via remark
- [x] Banner text + status page content tiếng Việt (per `dev-readable-doc-language.md` §4)
- [x] Banner mention support@kitehub.me (sync với GAP-540 footer support discoverability)
- [x] FE component unit test cover banner render + dismiss flow (5 tests passing)
- [x] Lighthouse / accessibility: banner không trap keyboard focus; ARIA labels present (`role="status"` + `aria-label` + focusable dismiss button)

## Related

- Wave 78 plan: `documents/03-planning/waves/wave-2026-05-14-78-beta-invite-launch-retain.md` Bucket B
- Sister gap GAP-538 (onboarding checklist — same Bucket B)
- GAP-540 (support channel discoverability — share support@ contact)
- Rules: `contract-first-for-cross-layer.md` v1.0.1; `dev-readable-doc-language.md` v1.0.1
- Outside-in 3-agent audit 2026-05-14 N2 finding

## Log

- 2026-05-14 — Initial write-up (state-check completed; 0 banner/route/content file found; Wave 78 Bucket B owner).
- 2026-05-14 — Wave 78 Bucket B shipped 90%: FE `BetaDisclaimerBanner` (dismissible, cookie `kitehub_beta_disclaimer_dismissed` Max-Age 1y), `/beta-status` SSR page with markdown renderer + status badge + known-issues panel, BE `BetaStatusController` + `BetaStatusService` (loads markdown from `src/main/resources/beta-status/beta-status.md` Vietnamese static MVP, 5-min cache). 5 FE banner tests + 1 BE controller test PASS. Banner currently mounted on `/onboarding` page; broader dashboard-layout integration deferred (Bucket A FE polish wave). Remaining: live verify per `pre-handoff-self-test-completeness.md` §2.2 anonymous-flow post next deploy.
