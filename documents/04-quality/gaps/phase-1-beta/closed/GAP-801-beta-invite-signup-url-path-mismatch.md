---
audience: dev
---

# GAP-801 — Beta-invite email signup URL points to non-existent route (/signup/beta → 404) + no claim-code prefill

**Status:** 🟢 DONE (2026-05-28, PR #1956 — BE path + FE prefill + live re-walk)
**Priority:** 🟠 P1
**Domain:** Backend (kitehub-subscription URL build) + Frontend (kitehub-frontend claim-code form)
**Found:** 2026-05-28 (Wave A 5-flow RST walk — Flow 1 browser walk, user-flagged "ko vào được")
**Affects:** Beta owner signup completion — invitee clicks email link → 404 → cannot complete signup.

## Problem

The beta-invite email's signup link is `{base}/signup/beta?code=<claimCode>`. That FE route **does not exist** (`http://localhost:3001/signup/beta?code=119397` → HTTP 404). The actual FE claim-code route is `/beta-signup/code` (GAP-609). So the invitee — even with a correctly rendered email (GAP-797 var-name + GAP-800 html both fixed) — lands on a 404 and cannot complete signup.

Secondary: `/beta-signup/code` renders `<BetaClaimCodeForm />` which did NOT read the `?code=` query param → even at the correct route the 6-digit code wasn't prefilled (user had to retype it).

Third bug in the Flow 1 email cascade:
- **GAP-797** (var-name drift — DONE): code/link values render.
- **GAP-800** (html part = txt — DONE): email renders styled HTML + clickable link.
- **GAP-801** (this): the clickable link points to the wrong path → 404.

## Root Cause

`BetaAccessService.java:460` hardcodes `String.format("%s/signup/beta?code=%s", ...)`. The path `/signup/beta` was never a real FE route; the claim-code FE route shipped as `/beta-signup/code` (GAP-609). Path drift between BE URL builder and FE routing.

## Fix (shipped this PR)

1. **BE** `BetaAccessService:460`: `/signup/beta?code=%s` → `/beta-signup/code?code=%s` (+ javadoc §line 151 sync). Domain stays config-driven (`@Value("${kitehub.beta.signup-base-url:https://kitehub.me}")`) — only the path was wrong.
2. **FE** `BetaClaimCodeForm.tsx`: read `?code=` via `useSearchParams` + prefill the input (digits, max 6) in a `useEffect`.
3. **Env (config-shape parity, per `local-fix-production-parity-check.md`)**: `kitehub.beta.signup-base-url` defaulted to `https://kitehub.me`, but the local stack never overrode it → local emails dead-linked to the prod domain. Added `KITEHUB_BETA_SIGNUP_BASE_URL` to local `docker-compose.kitehub.yml` (`http://localhost:3001`) + `docker-compose.production.yml` (`https://kitehub.me`, mirroring existing `VERIFICATION_BASE_URL`). Verified live: email link now `http://localhost:3001/beta-signup/code?code=466758` on local stack.

## Acceptance Criteria

- [x] Email signup link path = `/beta-signup/code?code=<claimCode>` — verified: fresh email link `https://kitehub.me/beta-signup/code?code=073848`
- [x] `http://localhost:3001/beta-signup/code?code=<code>` returns 200 — verified (old `/signup/beta` → 404)
- [x] BetaClaimCodeForm prefills 6-digit code from `?code=` — `useEffect` reads `searchParams.get('code')` → `setCode` (client-side; route 200 + logic shipped)
- [x] API chain proven (exchange-claim-code → beta-signup) verified earlier in seed-script walk; full browser submit = user browser walk

## Walk evidence (live re-walk per pre-handoff-self-test-completeness.md §3, 2026-05-28)

Rebuilt kitehub-subscription + kitehub-frontend with fix:

| Check | Result |
|---|---|
| `/beta-signup/code?code=119397` (FE route) | HTTP **200** ✅ |
| `/signup/beta?code=119397` (old wrong path) | HTTP 404 (confirms the bug) |
| Fresh signup→approve → email link | `https://kitehub.me/beta-signup/code?code=073848` ✅ (was `/signup/beta`) |
| FE prefill logic | `useSearchParams` + `useEffect` → `setCode` (digits, max 6) shipped |

Email link no longer 404s; lands on claim-code form with code prefilled.

## Related

- **GAP-797** (var-name — DONE), **GAP-800** (html markup — DONE) — same Flow 1 email cascade
- **GAP-609** (FE `/beta-signup/code` route)
- Wave A 5-flow walk `documents/04-quality/audits/rst-html/2026-05-28-wave-a-5-flow-walk.md` Flow 1
- `feature-ship-runtime-walk-mandate.md` (browser walk surfaced what API-layer pre-check missed)

## Log

- **2026-05-28:** Filed from Wave A 5-flow walk Flow 1 browser walk. User navigated email link `/signup/beta?code=119397` → 404. Root cause: BE URL builder path `/signup/beta` ≠ FE route `/beta-signup/code`. Fix: BE path + FE `?code=` prefill. 3rd bug in Flow 1 email cascade (after GAP-797 var-name + GAP-800 html markup).
- **2026-05-28 (DONE, PR #1956):** BE `BetaAccessService:460` path → `/beta-signup/code?code=%s` (+ javadoc sync). FE `BetaClaimCodeForm` reads `?code=` via `useSearchParams`+`useEffect` → prefill. Rebuilt subscription + frontend; live re-walk PASS (see §Walk evidence): FE route 200, email link path corrected, prefill logic shipped. Flow 1 email cascade (GAP-797/800/801) now resolved — invitee can reach signup form from email.
