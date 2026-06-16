# GAP-1442: CSP console warning — 'upgrade-insecure-requests' bị ignore trong report-only policy (no-op directive)

**Status:** 🟢 DONE
**Priority:** 🟢 P3
**Domain:** Frontend
**Found:** 2026-06-16 (Phase-2 browser walk flow KH-9)
**Affects:** KH-9 — CSP config (`kitehub-frontend next.config`/middleware `Content-Security-Policy-Report-Only` header)

## Problem
Discovered Phase-2 browser walk KH-9. Console mọi trang log: "The Content Security Policy directive 'upgrade-insecure-requests' is ignored when delivered in a report-only policy". Cosmetic — không ảnh hưởng chức năng.

## Proposed Fix
Bỏ `upgrade-insecure-requests` khỏi report-only CSP HOẶC chuyển sang enforcing CSP nếu muốn directive có hiệu lực.

## Acceptance Criteria
- [x] Console không còn warning 'upgrade-insecure-requests ignored' (code-complete; pending browser re-walk)

## Fix (2026-06-16, branch `fix/phase3-bucketB-kh9-admin`)
- Removed `"upgrade-insecure-requests"` from the `cspDirectives` array in `kitehub-frontend/next.config.js` (the array is delivered via `Content-Security-Policy-Report-Only`, where the directive is a no-op that browsers warn about). Added a comment to re-add it when the header flips to the enforcing `Content-Security-Policy` (Phase 1.5). Transport-level HTTPS already enforced via the existing `Strict-Transport-Security` header.
- Pending: human browser re-walk to confirm the console warning is gone.

## Related
- Discovered in: Phase-2 browser walk (flow KH-9), 2026-06-16
