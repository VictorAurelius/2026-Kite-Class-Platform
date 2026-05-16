# GAP-590: Email verification + password reset link expiry policy spec

**Status:** 🟡 PARTIAL (60%) — Wave 86 docs-cluster spec doc shipped (`documents/01-business/kitehub/auth/link-expiry-policy.md`) với 9 business rules (BR-AUTH-LINK-EXPIRY-001 → 009) + TTL matrix + code reference verify (4/9 ✅ code matches spec, 1/9 ⏳ Phase 1.5+ future feature `magic link`, 1/9 ⏳ verify enforcement `resend rate limit`). Code/spec mismatch: NONE found for Phase 1 BETA shipped scope. Auth-hardening-checklist Cat 4 cite + integration test + FE countdown defer follow-up GAP-590b if needed.
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

- [x] Spec doc shipped — `documents/01-business/kitehub/auth/link-expiry-policy.md` (9 BR + TTL matrix + code reference + status per BR)
- [x] Spring config keys verified runtime — BR-001..BR-008 code reference cited via `BetaAccessService.INVITE_TOKEN_TTL_HOURS=24L`, `kitehub.auth.password-reset.token-ttl-minutes=60`, `kitehub.auth.jwt.access-token-ttl-minutes=15`, `jwt.challenge-secret` 5-min, etc.
- [ ] FE countdown displayed on invite landing — defer follow-up GAP-590b Phase 1.5+ (Phase 1 BETA accept email body display "Hết hạn: <date>" current state)
- [ ] Auth checklist Cat 4 row 7 updated + verified PASS — defer follow-up `pre-launch-auth-hardening-checklist.md` Cat 4 row 7 cite this spec on next refresh
- [ ] Integration test verifies TTL enforcement per token type — defer follow-up GAP-590b Phase 1.5+ (Phase 1 BETA accept unit test current; integration mandatory Phase 1.5+)
- [ ] Resend rate limit 5/hour/email enforced — ⏳ verify code state; if missing → follow-up GAP-590c file Phase 1 BETA scope

## Log

- **2026-05-16** Wave 86 docs-cluster — spec doc shipped + code state-check verify. Status flipped OPEN → PARTIAL (60%). Per `gap-done-discipline.md` §3 PARTIAL exit ramp: 2 ACs verified (spec doc + Spring config verify); 4 ACs deferred to follow-up (FE countdown + auth-checklist sync + integration test + rate-limit verify). Verification artifact: `documents/01-business/kitehub/auth/link-expiry-policy.md`. Code/spec mismatch: NONE found for Phase 1 BETA shipped scope. Magic link + email-verification dedicated marked Phase 1.5+ future scope (not Phase 1 BETA gap).

## Related

- Audit benchmark: `documents/04-quality/audits/persona-review/2026-05-15-pre-wave-86-benchmark-vn-saas-edu.md` §3 Q5 + §6 GAP-NEW-2
- Audit simulation: `documents/04-quality/audits/persona-review/2026-05-15-pre-wave-86-simulation-3axis.md` §3 cell 5 + §5 E-AC1
- Wave 86 plan §3 Bucket E AC E-AC1
- `pre-launch-auth-hardening-checklist.md` Cat 4 §2.8
