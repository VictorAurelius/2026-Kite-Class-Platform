# GAP-977: FE payment auto-detect + beta-mode banner

**Status:** 🟡 PARTIAL
**Priority:** 🟠 P1
**Domain:** Frontend
**Found:** 2026-06-04 (Wave flow-kh3-2 SePay integration plan)
**Affects:** `kitehub-frontend` billing payment page + BetaModeBanner

## Problem

The payment page needed (a) a beta-mode banner telling the owner the displayed transfer
amount is the symbolic 10.000đ during Phase 1 BETA, and (b) automatic detection of
payment completion → success toast → redirect.

## Proposed Fix

Add a `BetaModeBanner` component gated on `NEXT_PUBLIC_BETA_PAYMENT_OVERRIDE` and render
it on the payment page.

**Design deviation (documented):** the api-contract specified a WebSocket subscribe to
`/topic/payments/{paymentId}`. The existing `usePayment` hook already polls payment
status every 5s and the page already shows success toast + redirect on `COMPLETED`, and
the backend exposes no STOMP broker (Bucket B added no WS publish). So the auto-detect
UX is delivered via the existing polling path; a WebSocket push is deferred until a BE
broker exists. The banner is the net-new piece.

## Acceptance Criteria

- [x] `BetaModeBanner.tsx` renders only when `NEXT_PUBLIC_BETA_PAYMENT_OVERRIDE === 'true'`
- [x] Banner copy matches api-contract (10.000đ symbolic / 599.000đ production)
- [x] Banner wired into payment page
- [x] Vitest unit tests (render-when-true / render-nothing-when-false/unset)
- [x] Auto-detect completion → toast + redirect (pre-existing via `usePayment` polling)
- [ ] WebSocket push `/topic/payments/{paymentId}` — DEFERRED (no BE STOMP broker; polling covers UX)

## Related

- Implemented in: Wave flow-kh3-2 Bucket D (inline)
- Deviation per `design-source-implementation-parity.md` §3 (documented drop + reason)
- Sister: [[GAP-975]], [[GAP-976]]
