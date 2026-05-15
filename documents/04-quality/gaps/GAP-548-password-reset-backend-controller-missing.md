# GAP-548: Password-reset BE controller missing — gateway rate-limit ready, backend forwards 404

**Status:** 🔵 OPEN
**Priority:** 🟠 P1
**Domain:** Backend (kitehub-subscription) / Docs (api-contract)
**Found:** 2026-05-14 (API Contract audit post-Wave-78)
**Affects:** Beta user UX (cannot self-serve password reset); pre-launch UX completeness

## Problem

Wave 78 Bucket C (PR #1354) shipped gateway-side rate-limit configuration cho `POST /api/auth/password-reset-request`:
- `kitehub/kitehub-gateway/src/main/resources/application.yml` route `auth-password-reset-request` với `RequestRateLimiter` (1/sec replenish, 2 burst, email-keyed)
- Comment trong yml ghi rõ: "Subscription service does not yet implement the password-reset feature; until the route lands the gateway forwards to subscription which returns 404 — but the rate-limit guard is in place defense-in-depth so the rate cannot be bypassed once the feature ships."

Hệ quả:
1. Beta user click "Quên mật khẩu" → FE POST → gateway rate-limit OK → forward đến `kitehub-subscription` → **HTTP 404** (no controller match)
2. Không có `documents/01-business/kitehub/password-reset/api-contract.md` documenting expected shape

## Root Cause

Wave 78-C scope intentionally narrowed cho rate-limit infrastructure trước (defense-in-depth), defer BE controller implementation cho Wave 79+ per task spec. Gateway-side artifact đã ship + tested (`AuthRouteRateLimitConfigTest.java`), nhưng end-to-end UX không khả dụng.

## Proposed Fix

### Phase 1 — BE controller + contract (Wave 79 candidate, P1)

1. Tạo `documents/01-business/kitehub/password-reset/` (3 files per `business-docs-3-layer.md`):
   - `rules.md` — BR-PWRESET-001..004 (token TTL, single-use, request rate, brute-force protection)
   - `use-cases.md` — UC-PWRESET-001..003 (request, confirm, complete)
   - `api-contract.md` — Endpoints:
     - `POST /api/auth/password-reset-request` (email → 202 Accepted, send email với secure token)
     - `POST /api/auth/password-reset-confirm` (token + new password → 200 OK)
2. Implement `PasswordResetController` trong kitehub-subscription:
   - Controller match gateway-rate-limit route
   - Service layer: generate token, persist `password_reset_tokens` table, emit `password.reset.requested` outbox event → email-service consume + send link
   - Confirm: validate token (TTL + single-use), update `users.password_hash`, invalidate active sessions
3. Flyway migration `V[N]__create_password_reset_tokens_table.sql`
4. Integration tests covering happy + error paths

### Phase 2 — Versioning alignment (combined với GAP-547 Phase 2)

Migrate cùng `/api/auth/**` namespace sang `/api/v1/auth/**` per `versioning-policy.md` §7.1.

## Acceptance Criteria

- [ ] 3 files `documents/01-business/kitehub/password-reset/{rules.md, use-cases.md, api-contract.md}` exist
- [ ] api-contract.md liệt kê 2 endpoints (request + confirm) với schema + error codes + rate-limit reference
- [ ] `PasswordResetController` + service + entity + migration shipped trong kitehub-subscription
- [ ] Integration test verify gateway route → controller (HTTP 202 trên valid; 400 trên invalid email format; 429 trên rate-limit hit)
- [ ] FE consumer (kitehub-frontend hoặc kiteclass-frontend) wire login page link
- [ ] Email template (per Wave 78 Bucket E pattern) — password reset link template + plain-text fallback
- [ ] End-to-end smoke: user submit email → email received với secure link → click link → reset password page → submit new password → can login với password mới
- [ ] gap-status.csv row updated

## Related

- Audit: `documents/04-quality/audits/api-contract/2026-05-14-post-wave-78.md` (bug list P1 #7)
- Wave 78-C commit: `dbbde453 feat(wave-78-C): backend close-out — auth rate limit + Retry-After UX + env config (GAP-508/514/515)`
- Gateway route: `kitehub/kitehub-gateway/src/main/resources/application.yml` lines `auth-password-reset-request`
- Existing infra: `kiteclass/kiteclass-gateway/src/main/java/com/kiteclass/gateway/module/auth/entity/PasswordResetToken.java` — có thể reference design (kiteclass-side existing implementation)
- Rule: `.claude/rules/pre-launch-auth-hardening-checklist.md` §2.1 (password-reset rate limit)
- Rule: `.claude/rules/business-docs-3-layer.md`
- Cross-link: GAP-547 (versioning migration shared Phase 2)

## Log

- **2026-05-14:** DONE — Wave 79 Bucket C closure. PasswordResetController + V47__add_user_password_reset_columns.sql migration shipped; gateway rate-limit forwards real 200/429 path now (PR #1367).

- **2026-05-14:** Filed from post-Wave-78 API contract audit. P1 — defense-in-depth gateway ready, but end-to-end UX requires BE controller. Wave 79 candidate; non-blocking cho Phase 1 BETA admin-managed reset fallback nhưng cần ship trước public Phase 1.5 PAID launch.
