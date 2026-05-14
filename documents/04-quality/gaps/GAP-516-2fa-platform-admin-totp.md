# GAP-516: 2FA TOTP mandatory for PLATFORM_ADMIN

**Status:** 🟡 PARTIAL
**Priority:** 🟠 P1 (Phase 1 BETA admin surface)
**Domain:** Backend + Frontend
**Found:** 2026-05-13 (Wave 71c per `pre-launch-auth-hardening-checklist.md` §2.4)

## Problem

PLATFORM_ADMIN role currently login với chỉ email+password. Một password compromise → full platform takeover (suspend tenants, approve fake beta requests, edit system config).

## Proposed Fix

1. Add `totp_secret VARCHAR(64)`, `totp_enrolled_at TIMESTAMP` to `users`
2. Enrollment flow: first PLATFORM_ADMIN login → forced to enroll TOTP (QR code via OtpAuth URI)
3. Subsequent logins: password OK → present TOTP challenge → verify 6-digit code → issue JWT
4. Library: `dev.samstevens.totp:totp:1.7.1` (well-maintained, RFC 6238)
5. FE: enrollment page + login TOTP step

## Acceptance Criteria

### FE half (Wave 72b Bucket B — this PR)
- [x] `/2fa-setup` page — QR + recovery codes + first-TOTP confirm; gated by `requires2fa_enrollment` login flag
- [x] `/2fa-challenge` page — 6-digit TOTP input + recovery-code fallback toggle; gated by `requires2fa` login flag
- [x] `TotpInput` component — 6 single-digit boxes with auto-focus-next + paste handling
- [x] `RecoveryCodesDisplay` component — mono grid + Copy/Download/Print actions
- [x] Login page branches on `requires2fa` / `requires2fa_enrollment` response shapes
- [x] FE consumes MSW handlers per `documents/01-business/kitehub/auth/api-contract.md`
- [x] Tests: 4 spec files (`TotpInput`, `2fa-setup/page`, `2fa-challenge/page`, `login/page` 2FA branching) — 21 cases passing
- [x] Build green; lint warnings unchanged from baseline

### BE half (Wave 72b Bucket A — separate PR)
- [ ] Admin enrolls via QR scan (Google Authenticator / 1Password) — real TwoFactorController
- [ ] Login với wrong TOTP → 401
- [ ] Login với correct TOTP → JWT issued
- [ ] Recovery codes generated (10 single-use) — persisted bcrypt-hashed

### Live verification (deferred to post-A+B merge)
- [ ] End-to-end test: admin@kitehub.me first login → /2fa-setup → enroll → next login → /2fa-challenge → verify → /admin
- [ ] Recovery code path live verified

## Out-of-scope (this PR)

- Settings page "Đăng xuất tất cả thiết bị" + "Tạo lại mã khôi phục" UI (deferred to follow-up; depends on BE recovery-code regenerate endpoint)
- Real production end-to-end test (deferred until both Bucket A BE + Bucket B FE merged to main + deployed to staging)

## Related

- Rule: `pre-launch-auth-hardening-checklist.md` §2.4
- Contract: `documents/01-business/kitehub/auth/api-contract.md` (Bucket 0 Foundation merged via PR #1294)
- MSW handlers: `kitehub/kitehub-frontend/src/test/msw/handlers/auth.ts` (Bucket 0)
- Wave 72b Bucket B (this PR) FE half — combined BE+FE coverage ~85% post-merge of both halves

## Log

- **2026-05-14:** Wave 72b Bucket B FE half shipped — 5 source files (2 pages + 2 components + login patch) + 4 test files (21 cases, all passing) + qrcode.react@4.2.0 dep added. Build green (`/2fa-setup` 12.1kB, `/2fa-challenge` 4.72kB routes generated). MSW-backed integration tests cover happy path (TOTP verify → /admin redirect, recovery code → +regenerate_recommended flag), failure paths (INVALID_TOTP clears input, ACCOUNT_LOCKED message, requires2fa_enrollment redirect), and component invariants (TotpInput auto-advance, backspace jump-back, paste 6 digits). Status flipped OPEN → PARTIAL — BE half remains in Bucket A and live verification deferred to post-A+B merge per AC split above.
