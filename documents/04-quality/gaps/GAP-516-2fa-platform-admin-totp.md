# GAP-516: 2FA TOTP mandatory for PLATFORM_ADMIN

**Status:** 🔵 OPEN
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

- [ ] Admin enrolls via QR scan (Google Authenticator / 1Password)
- [ ] Login với wrong TOTP → 401
- [ ] Login với correct TOTP → JWT issued
- [ ] Recovery codes generated (10 single-use)

## Related

- Rule: `pre-launch-auth-hardening-checklist.md` §2.4
- Wave 72 candidate (post-Phase 1 BETA stabilize)
