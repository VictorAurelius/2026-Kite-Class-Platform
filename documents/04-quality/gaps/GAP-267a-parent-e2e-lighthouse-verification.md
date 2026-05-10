# GAP-267a: kc-parent E2E spec + Lighthouse PWA ≥90 verification

**Status:** 🔵 OPEN
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

- [ ] Playwright spec passes locally + in `frontend-ci.yml` E2E job
- [ ] Lighthouse PWA ≥90 on Vercel preview (audit report committed)
- [ ] GAP-267 parent gap flipped 🟡 PARTIAL → 🟢 DONE per `gap-done-discipline.md` §2

## Related

- Parent: GAP-267
- Wave 49 closure PR #1095
- Wave 49 Bucket A PR #1092
- Sibling: GAP-269c (kc-student equivalent — bundle if both ready in same audit run)

## Log

- **2026-05-10**: Filed at Wave 49 closure as named follow-up promised in GAP-267 Log entry (PR #1092 Bucket A coordinator note). Per `audit-to-gap-pipeline.md` §3 + `gap-done-discipline.md` §3, deferred items get real gap files instead of orphan Log mentions.
