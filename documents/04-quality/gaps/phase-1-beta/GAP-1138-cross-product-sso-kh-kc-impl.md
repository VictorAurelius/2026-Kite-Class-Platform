# GAP-1138: Cross-product SSO KiteHub→KiteClass implementation (dedicated wave)

**Status:** 🟡 PARTIAL — code + unit tests shipped Wave RBAC-SSO 1 (2026-06-14); runtime G2 walk (human) pending
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

- [ ] Owner/Staff login KH `:3001` → click → vào KC `:3000` owner-shell không re-login (code shipped; runtime G2 walk pending — nút "Mở quản lý trường" + `/sso/callback`)
- [x] One-time code single-use + TTL ≤60s enforced (replay rejected) — `SsoCodeService` GETDEL single-use + TTL clamp ≤60s; unit-tested (replay → empty → exchange 401)
- [x] KC session scoped đúng tenant (gateway header inject verified) — `TokenService.generateAccessToken` mints `tenantId` claim; gateway `TenantHeaderGuardFilter` injects `X-Tenant-Id` (ADR-039 precedent, pre-existing); claim-mint unit-tested
- [x] Security review pass (no token-in-URL leak / CSRF guard) — opaque code-only in URL (JWT minted only in exchange response body); CSRF guard = `consumes=application/json` (form-POST → 415, unit-tested); replay → 401; refresh-token rejected at issue-code
- [ ] Runtime walk per `feature-ship-runtime-walk-mandate.md` §3 (human G2 walk on production-equivalent stack — coordinator does code+tests, human walks)

## Related

- Design: ADR-040 (ACCEPTED 2026-06-14) — cross-product SSO KH→KC; Option A implemented
- Discovered in: Wave RBAC-Shell 1 Bucket C (branch wave/rbac-shell-1-c-sso)
- Implemented in: Wave RBAC-SSO 1 (branch wave/rbac-sso-gap-1138)
- Umbrella: GAP-1119; precedent ADR-039
- Beta unblock: Bucket B owner/staff dùng KC-native fallback login; KHÔNG chặn LMS-FE

## Implementation note (2026-06-14)

Wave RBAC-SSO 1 đã ship code + unit tests cho cả 4 surface:
- `kitehub-subscription`: `SsoCodeService` (Redis one-time code, TTL ≤60s, single-use GETDEL) + `SsoController` (`POST /api/v1/auth/sso/issue-code` self-validate Bearer + `/exchange` consume → KH-minted JWT). DTO `SsoIssueCodeResponse` + `SsoExchangeRequest`. 18 unit tests (`SsoCodeServiceTest` 10 + `SsoControllerTest` 8) PASS.
- `kitehub-frontend`: `lib/api/sso.ts` (`issueSsoCode` + `buildKiteClassSsoCallbackUrl`) + `OpenSchoolManagementButton` wired vào customer dashboard per-instance. Vitest 3/3 PASS.
- `kiteclass-frontend`: `lib/api/sso.ts` (`exchangeSsoCode`) + route `/sso/callback` (Suspense-wrapped useSearchParams; single-use guard; establish KC session via `setTokens`+`setAuth`+roleHome redirect). Vitest 2/2 PASS.

Endpoints under `/api/v1/auth/sso/**` đã public sẵn ở gateway (`JwtAuthenticationGatewayFilter.isPublicPath`) + subscription SecurityConfig (`/api/v1/auth/** permitAll`) → không cần đổi gateway/security config. Gateway route `kitehub-auth-v1` (`Path=/api/v1/auth/**`) đã trỏ subscription.

Còn lại: runtime G2 walk (human) trên stack production-equivalent + paired re-walk evidence per `feature-ship-runtime-walk-mandate.md` §3 trước khi flip DONE.

## G1 runtime walk (2026-06-14) — gateway BE-contract: ✅ FULL PASS (status giữ PARTIAL)

Per `documents/04-quality/audits/rst-html/2026-06-14-g1-runtime-walk-rbac-lms.md`. SSO full chain qua gateway `:9000`: `POST /api/v1/auth/sso/issue-code` (KH access token) → 200 `{code, expiresIn:60}`; `POST /api/v1/auth/sso/exchange {code}` → 200 (accessToken+refreshToken+user, no re-login); replay code lần 2 → 401 (single-use GETDEL); CSRF non-JSON → 415; no-token issue → 401. Toàn bộ AC BE PASS (TTL 60s, single-use, CSRF JSON-guard). **Còn lại G2★ human:** browser-walk nút "Mở quản lý trường" KH `:3001` → KC `:3000` callback cross-origin (FE image stale). KHÔNG flip DONE.

## G1-FE browser walk note (2026-06-14)

G1-FE BLOCKED: SSO KH→KC không browser-walk được local — thiếu KiteHub owner credential (KH auth = kitehub-subscription tách biệt KC tenant-auth). KH `:3001/dashboard` đá `/login`. Filed **GAP-1305** (seed KH owner cred). BE-contract walk 2026-06-14 đã verify SSO issue→exchange→replay-401. — verified qua Playwright headless trên FE thật `skytest.127.0.0.1.nip.io:3000` (rebuild kiteclass-frontend). Evidence: `documents/04-quality/audits/rst-html/2026-06-14-g1-fe-browser-walk.md`. **Giữ PARTIAL** — human G2★ vẫn bắt buộc (mutation deep-interaction chưa walk).

## SSO hardening — E2E regression guard shipped (PR #2398, 2026-06-14)

Hardening landed cho 70%-shipped SSO chain (status giữ **PARTIAL** — human G2★ vẫn pending):
- **E2E regression guard:** `kiteclass-frontend/e2e/sso/sso-callback-regression.spec.ts` (6 test: happy opaque-code→exchange-once→role-home no-relogin + replay/expired/missing/empty-code sad paths + StrictMode single-exchange + opaque-code-NOT-JWT + CSRF guard) + `kitehub-frontend/e2e/sso/sso-issue-redirect.spec.ts` (3 test: redirect carries opaque code not JWT + 401→/login + 500→inline alert). Wired vào `test:e2e:gates` → CI Playwright gate PASS (9/9).
- **ADR-040 doc-drift fix:** mermaid line 54 `tenant-auth/sso/exchange` → `/api/v1/auth/sso/exchange` (khớp endpoint đã ship).
- **Determinism root-cause filed:** **GAP-1306** (P1) — `AuthService.resolveTenantIdForRole` `findFirst()` không `ORDER BY` → tenantId JWT non-deterministic cho owner >1 instance; GAP-1305 single-instance seed chỉ là workaround.

Per `feature-ship-runtime-walk-mandate.md` §3: code + automated guard verified; chỉ còn human G2★ cross-origin browser walk để flip DONE.
