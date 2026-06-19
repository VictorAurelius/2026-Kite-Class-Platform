# GAP-923: CSP report-only excludes `http://localhost:9000` (gateway) → browser violation logs trong local dev

**Status:** 🔵 OPEN
**Priority:** 🟡 P2 (cosmetic local dev DX — report-only mode, không block)
**Domain:** Frontend / Security
**Found:** 2026-06-04 (Wave flow-kh1 G2 handoff browser console)
**Affects:**
- `kitehub/kitehub-frontend/next.config.js` hoặc CSP middleware
- Local dev DX (production OK — kitehub.me CSP correct)

## Problem

User browser console errors during G2:
```
Connecting to 'http://localhost:9000/api/v1/auth/beta-signup/exchange-claim-code' violates
the following Content Security Policy directive: "connect-src 'self' https://kitehub.me
https://*.kitehub.me wss://*.kitehub.me". The policy is report-only, so the violation
has been logged but no further action has been taken.
```

CSP directive `connect-src` chỉ allow:
- `'self'` (same origin = `http://localhost:3001` cho FE)
- `https://kitehub.me` (production apex)
- `https://*.kitehub.me` (production wildcard)
- `wss://*.kitehub.me` (websocket production)

KHÔNG include `http://localhost:9000` (gateway local). FE fetch tới gateway từ browser → CSP violation logged.

**Note: Report-only mode** (`Content-Security-Policy-Report-Only` header) → violation logged BUT KHÔNG block request. Subsequent 404 từ FE call là separate issue (claim_code consumed — single-use semantic, NOT CSP-blocked).

## Root Cause

CSP config viết cho production cảnh nhưng KHÔNG có override cho local dev gateway URL. Possible locations:
- Next.js middleware setting CSP header
- `next.config.js` `headers()` function
- Direct response header from Next.js routes

## Proposed Fix

Add `http://localhost:9000` to `connect-src` directive WHEN `NODE_ENV !== 'production'` (local dev only). KHÔNG add `localhost:9000` cho production CSP — production gateway = `kitehub.me` HTTPS.

Example:
```js
const csp = isProduction
  ? "connect-src 'self' https://kitehub.me https://*.kitehub.me wss://*.kitehub.me"
  : "connect-src 'self' http://localhost:9000 https://kitehub.me https://*.kitehub.me wss://*.kitehub.me";
```

Alternative: switch CSP from report-only to enforced mode trong production (after verify), keep report-only local. Doesn't fix browser console noise but reduces production risk.

## Acceptance Criteria

- [ ] CSP `connect-src` accept `http://localhost:9000` khi local dev (NODE_ENV != production)
- [ ] Production CSP unchanged (kitehub.me only)
- [ ] Re-walk G2 → browser console KHÔNG có CSP violation cho gateway URL
- [ ] Documentation note trong `documents/05-guides/operations/local-dev-setup.md` về CSP local override

## Related

- Discovered in: Wave flow-kh1 G2 handoff session 2026-06-04 (user browser console)
- Sister: GAP-922 (duplicate email — separate root cause)
- Severity P2 because: (a) report-only mode → KHÔNG block actual functionality; (b) production CSP correct; (c) only local dev DX noise
