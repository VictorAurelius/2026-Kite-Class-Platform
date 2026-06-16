# GAP-1442: CSP console warning — 'upgrade-insecure-requests' bị ignore trong report-only policy (no-op directive)

**Status:** 🔵 OPEN
**Priority:** 🟢 P3
**Domain:** Frontend
**Found:** 2026-06-16 (Phase-2 browser walk flow KH-9)
**Affects:** KH-9 — CSP config (`kitehub-frontend next.config`/middleware `Content-Security-Policy-Report-Only` header)

## Problem
Discovered Phase-2 browser walk KH-9. Console mọi trang log: "The Content Security Policy directive 'upgrade-insecure-requests' is ignored when delivered in a report-only policy". Cosmetic — không ảnh hưởng chức năng.

## Proposed Fix
Bỏ `upgrade-insecure-requests` khỏi report-only CSP HOẶC chuyển sang enforcing CSP nếu muốn directive có hiệu lực.

## Acceptance Criteria
- [ ] Console không còn warning 'upgrade-insecure-requests ignored'

## Related
- Discovered in: Phase-2 browser walk (flow KH-9), 2026-06-16
