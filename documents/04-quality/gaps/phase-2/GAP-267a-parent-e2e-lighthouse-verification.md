# GAP-267a: kc-parent E2E spec + Lighthouse PWA ≥90 verification

**Status:** 🟡 PARTIAL — Playwright spec shipped Wave 51 Bucket A; Lighthouse PWA defer to post-staging-HTTPS
**Priority:** 🟡 P2 (verification of Wave 49 Bucket A AC; not blocking new feature ship)
**Domain:** Frontend testing + PWA deployment
**Found:** 2026-05-10 (Wave 49 Bucket A PARTIAL exit-ramp per `gap-done-discipline.md` §3)
**Parent:** [GAP-267](GAP-267-track-2-port-kiteclass-parent.md)
**Affects:** `kiteclass-frontend/src/app/(dashboard)/parent/**` + Vercel preview deploy

## Problem

Wave 49 Bucket A (PR #1092) shipped 17 kc-parent screens consolidated into 8 production routes; logical flow exists end-to-end (parent-invite redeem → home → child card → transcript / billing → pay → success) but two AC items defer to dedicated verification:

1. **Lighthouse PWA score ≥90** — requires HTTPS deploy to measure (Lighthouse refuses `http://localhost:3001`); cannot run inside PR verify scope.
2. **Playwright E2E spec for invite-redemption → pay-tuition flow** — logical flow navigable manually; automated spec authoring deferred to keep Wave 49 wall-clock under cap.

## Current State (verified 2026-05-10)

| Artifact | Status |
|---|---|
| Production routes + components | ✅ shipped Wave 49 Bucket A (PR #1092) |
| Web Push subscribe/unsubscribe via MSW stub | ✅ unit-tested |
| `useMyChildren` + `(dashboard)/parent/transcript/[childId]` from Wave 18b1 | ✅ preserved verbatim |
| Playwright spec covering invite → child-binding → pay tuition | ❌ not authored |
| Lighthouse PWA ≥90 measurement on staging URL | ❌ not measured |

## Proposed Fix

1. Author Playwright E2E spec at `kiteclass-frontend/e2e/parent-invite-pay-flow.spec.ts`:
   - Login as parent → land on `(dashboard)/parent/home` → see child card → click child → see transcript → navigate to billing → see invoice list → click pay → mock payment flow → success state
   - Use existing MSW handlers for `/api/parents/me/children` + payment-mock provider
2. Run Lighthouse on Vercel preview URL post-merge of any kc-parent PR; capture score in `documents/04-quality/audits/ui/lighthouse-kc-parent-YYYY-MM-DD.md`
3. Update parent gap GAP-267 AC checkbox + Log entry + flip to 🟢 DONE if both pass

## Acceptance Criteria

- [x] Playwright spec authored (`kiteclass-frontend/e2e/wave-49-followups/parent-invite-pay-flow.spec.ts`) — Wave 51 Bucket A
- [ ] Lighthouse PWA ≥90 on Vercel preview (audit report committed) — DEFERRED post-staging-HTTPS
- [ ] GAP-267 parent gap flipped 🟡 PARTIAL → 🟢 DONE per `gap-done-discipline.md` §2 (blocked on Lighthouse measurement)

## Related

- Parent: GAP-267
- Wave 49 closure PR #1095
- Wave 49 Bucket A PR #1092
- Sibling: GAP-269c (kc-student equivalent — bundle if both ready in same audit run)

## Log

- **2026-05-10** (PARTIAL flip): Wave 51 Bucket A shipped `kiteclass-frontend/e2e/wave-49-followups/parent-invite-pay-flow.spec.ts` — happy path (home → child card → transcript → billing → pay) + 1 error branch (children API 500 → error region). Stays PARTIAL per `gap-done-discipline.md` §3 because Lighthouse PWA ≥90 requires HTTPS staging (Lighthouse refuses `http://localhost:4700`). Lighthouse measurement deferred to dedicated follow-up post-staging-HTTPS deploy.
- **2026-05-10**: Filed at Wave 49 closure as named follow-up promised in GAP-267 Log entry (PR #1092 Bucket A coordinator note). Per `audit-to-gap-pipeline.md` §3 + `gap-done-discipline.md` §3, deferred items get real gap files instead of orphan Log mentions.
