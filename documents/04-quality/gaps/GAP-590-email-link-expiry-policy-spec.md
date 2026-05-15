# GAP-590: Email verification + password reset link expiry policy spec

**Status:** 🔵 OPEN
**Priority:** 🟠 P1
**Domain:** Backend / Auth
**Phase:** phase-1-beta
**Found:** 2026-05-15 (Wave 86 Bucket A benchmark-vn-saas-edu Q5 + simulation cell 5)
**Affects:** Magic link / invite link / password reset link / 2FA backup code expiry

## Problem

Industry standard (benchmark Q5):
- **Email verification: 24h** (industry norm)
- **Magic links / 2FA: 10-15 min** (security-sensitive)
- **Password reset: 24h** (standard)

Wave 86 plan KHÔNG explicit document token TTL policy. `pre-launch-auth-hardening-checklist.md` Cat 4 §2.8 token rotation rule chưa cite TTL cụ thể. Simulation cell 5: tenant Hằng's network drops mid invite-link click → token expires quá ngắn (<5 phút) → tenant phải resend tedious → UX damage + abandonment risk.

## Root Cause

Token TTL không codified trong code OR documentation. Backend `JwtUtils.java` default TTL có thể không match industry expectations.

## Proposed Fix

1. **Spec doc** `documents/01-business/auth/token-expiry-policy.md`:
   - First signup invite (welcome email): **24h** TTL
   - Password reset: **24h** TTL
   - Magic link login: **15 min** TTL (security-sensitive)
   - 2FA backup code: **10 min** TTL
   - Resend endpoint rate limit: **5/hour/email**
2. **Implementation** `kitehub-auth/.../JwtConfig.java`:
   - Configure `auth.token.ttl.signup-invite=24h`
   - Configure `auth.token.ttl.password-reset=24h`
   - Configure `auth.token.ttl.magic-link=15m`
   - Configure `auth.token.ttl.2fa-backup=10m`
   - Reload Spring property → no hard-code
3. **FE countdown**: invite landing page displays "Link expires in X hours/minutes" countdown
4. **Update `pre-launch-auth-hardening-checklist.md`** Cat 4 row 7: cite TTL specifics
5. **Integration test**: verify token expires correctly per TTL

## Acceptance Criteria

- [ ] Spec doc shipped
- [ ] Spring config keys applied + verified runtime
- [ ] FE countdown displayed on invite landing
- [ ] Auth checklist Cat 4 row 7 updated + verified PASS
- [ ] Integration test verifies TTL enforcement per token type
- [ ] Resend rate limit 5/hour/email enforced

## Related

- Audit benchmark: `documents/04-quality/audits/persona-review/2026-05-15-pre-wave-86-benchmark-vn-saas-edu.md` §3 Q5 + §6 GAP-NEW-2
- Audit simulation: `documents/04-quality/audits/persona-review/2026-05-15-pre-wave-86-simulation-3axis.md` §3 cell 5 + §5 E-AC1
- Wave 86 plan §3 Bucket E AC E-AC1
- `pre-launch-auth-hardening-checklist.md` Cat 4 §2.8
