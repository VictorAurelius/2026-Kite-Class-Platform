# GAP-1138: Cross-product SSO KiteHub→KiteClass implementation (dedicated wave)

**Status:** 🔵 OPEN
**Priority:** 🟠 P1
**Domain:** Mixed
**Found:** 2026-06-10 (Wave RBAC-Shell 1 Bucket C design-first)
**Affects:** kitehub-frontend / kitehub-subscription / kiteclass-frontend / kiteclass-core (gateway)

## Problem

Owner/Staff đăng nhập KiteHub (`:3001`, `/api/v1/auth`) cần vào KiteClass owner-shell (`:3000`) để quản lý nghiệp vụ trường **không re-login** (cross-product SSO). Bucket C của Wave RBAC-Shell 1 đã design-first (per `design-first-investigation-order.md`) → kết luận **SPLIT sang wave riêng** vì impl chạm 4 surface + security-sensitive (one-time-code exchange, replay/CSRF/token-in-URL avoidance) → > 1 bucket.

Token VALIDATION đã thống nhất: gateway `TenantHeaderGuardFilter` validate HS512 JWT bằng `JWT_SECRET` dùng chung với kitehub-subscription (precedent ADR-039). Phần MỚI = token TRANSPORT cross-origin KH→KC.

## Proposed Fix

Triển khai Option A của **ADR-040** (redirect + one-time exchange code):
1. `kitehub-subscription`: `POST /api/v1/auth/sso/issue-code` (mint one-time code, Redis TTL ≤60s, single-use) + `exchange` consume.
2. `kitehub-frontend`: nút "Mở quản lý trường" → request code → redirect `:3000/sso/callback?code=...`.
3. `kiteclass-frontend`: route `/sso/callback` → exchange code → establish KC session.
4. Security review: replay, single-use, CSRF, no raw JWT in URL.

## Acceptance Criteria

- [ ] Owner/Staff login KH `:3001` → click → vào KC `:3000` owner-shell không re-login
- [ ] One-time code single-use + TTL ≤60s enforced (replay rejected)
- [ ] KC session scoped đúng tenant (gateway header inject verified)
- [ ] Security review pass (no token-in-URL leak / CSRF guard)
- [ ] Runtime walk per `feature-ship-runtime-walk-mandate.md` §3

## Related

- Design: ADR-040 (PROPOSED) — cross-product SSO KH→KC
- Discovered in: Wave RBAC-Shell 1 Bucket C (branch wave/rbac-shell-1-c-sso)
- Umbrella: GAP-1119; precedent ADR-039
- Beta unblock: Bucket B owner/staff dùng KC-native fallback login; KHÔNG chặn LMS-FE
