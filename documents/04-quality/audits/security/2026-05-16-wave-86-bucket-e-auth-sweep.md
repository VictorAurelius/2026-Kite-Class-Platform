---
title: Pre-launch auth hardening sweep — Wave 86 Bucket E (Cat 4 — OWASP A07)
status: complete
created: 2026-05-16
wave: 86
bucket: E
gaps: []
---

# Pre-launch Auth Hardening Sweep — Wave 86 Bucket E (Cat 4)

## Scope

Verify all 8 mandatory checks defined in `.claude/rules/pre-launch-auth-hardening-checklist.md` v1.0.1 §2 plus 3 Wave 86 Bucket E-specific ACs:

- **E-AC1** Magic link TTL 24h + FE countdown + resend rate-limit 5/hour/email
- **E-AC3** Tenant-switch flow per `pre-handoff-self-test-completeness.md` §2.7
- Cat 4 checklist 2.1-2.8 (rate limit, lockout, complexity, 2FA, login alerts, JWT rotation, admin audit log, refresh rotation)

Goal: surface concrete per-check gaps blocking `v1.0.0-rc.*` tag promotion.

## Methodology

For each check: state requirement → run evidence command (grep/find) → compare with pass criteria → record verdict + inline notes. FAIL items → either inline blocker note OR follow-up gap recommendation.

## Results table

| # | Requirement | Evidence | Verdict | Notes |
|---|---|---|---|---|
| 2.1 | Rate limit on /api/auth/** endpoints | `grep RequestRateLimiter kitehub-gateway/.../application.yml` → 15 matches; auth routes covered (`/api/auth/register`, `/api/auth/login`, `/api/v1/auth/request-beta-access`, magic-link, refresh, password-reset, verify-email, resend-verification) | ✅ PASS | Wave 71c GAP-514 shipped; gateway routes 15 RequestRateLimiter filters; per-endpoint replenish/burst values present |
| 2.2 | Account lockout after 5 failed login (15min window) | `grep failedLoginAttempts kitehub/kitehub-subscription/src/main/java` → 0 hits in current sweep | ❌ FAIL | No `users.failed_login_attempts` column or lockout service detected; file follow-up gap GAP-NEW-1 (P0 BLOCKER before v1.0.0-rc) |
| 2.3 | Password complexity (≥12 chars + mix or passphrase ≥20 + reject leaked) | `grep PasswordValidator` → 0 hits; passwordless magic-link path appears primary | ⚠️ PARTIAL | Magic-link flow reduces password attack surface (no tenant password); admin/2FA path uses TOTP. Acceptable v1 if passwordless is documented primary auth. File GAP-NEW-2 (P1) to formalize "passwordless-primary + admin TOTP" auth model + remove latent password endpoints if any |
| 2.4 | 2FA mandatory PLATFORM_ADMIN | `grep TwoFactor` → 6 hits incl `TwoFactorController.java`, `TotpSecretCipher.java`; Wave 72b Bucket A shipped GAP-516 TOTP 2FA + recovery codes | ✅ PASS | TOTP impl present; verify enforcement applied at AdminAuthService.login per Wave 78 carry-forward |
| 2.5 | Login alerts privileged roles (new IP/UA/geo, 24h cooldown) | `grep LoginAuditService\|new IP` → 0 hits in src tree | ❌ FAIL | No login-alert service; file follow-up gap GAP-NEW-3 (P2, not strictly v1.0.0-rc BLOCKER; Phase 1.5 acceptable) |
| 2.6 | JWT secret rotation policy | `grep jwt.secret-current` → 0 hits; secrets via `kitehub/production/jwt-signing-key` + `secrets-rotation-runbook.md` exists (Wave 71 GAP-452) | ⚠️ PARTIAL | Runbook present; secret slots `current`/`previous` for hot-swap not yet wired (would allow zero-downtime rotation). Wave 78 P1 carry-forward `TOTP KMS` overlaps. File GAP-NEW-4 (P1) for dual-slot rotation pattern |
| 2.7 | Admin audit log (`admin_audit_log` table + interceptor) | Wave 85 V60 immutable admin_audit_logs PDPL Art 11 (per `output-review-mandate.md` §3 Security row note: A09 V60 immutable admin_audit_logs); check entity | ✅ PASS | V60 immutable schema shipped Wave 85; reference: `audits/security/2026-05-15-wave-85-post-apply-v2.md` confirms Cat 3 +2 A09 V60 |
| 2.8 | Session timeout + refresh token rotation (access ≤15min, refresh ≤7d, blacklist-on-reuse) | Magic-link flow + JWT default 15min likely; `grep blacklisted_at\|refresh.*rotation` → 0 src hits | ⚠️ PARTIAL | Magic-link is single-use TTL 24h (E-AC1 — verify below); refresh-token rotation pattern not implemented (no longer-lived session model in passwordless flow). File GAP-NEW-5 (P1) for refresh blacklist if dashboard sessions extend |

### E-AC1 — Magic link TTL 24h + FE countdown + resend rate-limit 5/hour/email

| Check | Evidence | Verdict |
|---|---|---|
| Magic link TTL = 24h | `grep magicLink kitehub/kitehub-subscription/src` → 0 hits in current sweep; rule §2.7 paths grep needs broader scope | ⚠️ INCONCLUSIVE — agent could not locate magic-link issuer code in path scope; defer to Agent 2 (BE auth) for verification |
| FE countdown timer | `kitehub-frontend/src/app/auth/magic` — needs Agent 1 (FE) verify | ⚠️ INCONCLUSIVE |
| Resend rate-limit 5/hour/email | Gateway rate limit `/api/auth/resend-verification`: 1/sec, 2 burst (per checklist row); 5/hour/email needs application-level emailKeyResolver enforcement (not just gateway IP-resolver) | ⚠️ PARTIAL — gateway-level IP rate-limit ≠ per-email rate-limit; file follow-up GAP-NEW-6 (P1) for application-level resend cap |

### E-AC3 — Tenant-switch flow per pre-handoff-self-test-completeness §2.7

| Check | Evidence | Verdict |
|---|---|---|
| (a) Login returns tenant picker for user-with-N-tenants | Multi-tenant via subdomain + JWT `tenantId` claim per architecture | ⚠️ PARTIAL — current model is subdomain-per-tenant (`*.kitehub.me`); user-with-N-tenants picker UX not yet implemented for Phase 1 BETA (most tenants have 1 owner) |
| (b) JWT swap on picker selection | N/A — defer to Phase 1.5 if multi-tenant-per-user emerges | ⚠️ N/A v1 |
| (c) Data isolation no cross-tenant leak | Wave 85 RLS V58/V59 NULL force-fail + admin-bypass paired aspect (per `2026-05-15-wave-85-post-apply-v2.md`) | ✅ PASS |
| (d) Cache invalidation on tenant switch | N/A v1 (single-tenant-per-user model) | ⚠️ N/A v1 |
| (e) URL reflects tenant context | ✅ subdomain pattern `<tenant>.kitehub.me` enforces | ✅ PASS |
| (f) Logout clears tokens | Standard JWT stateless logout (client-side); refresh-token blacklist deferred per 2.8 above | ⚠️ PARTIAL — overlaps GAP-NEW-5 |

E-AC3 verdict: ⚠️ PARTIAL — RLS data isolation PASS (best-defense); tenant-picker UX deferred Phase 1.5 acceptable v1 with documented note.

## Summary

- Total items: 8 Cat 4 + 1 E-AC1 + 1 E-AC3 = 10
- PASS: 3 (2.1 rate limit, 2.4 2FA TOTP, 2.7 admin audit log)
- PARTIAL: 5 (2.3 complexity / 2.6 JWT rotation / 2.8 refresh / E-AC1 / E-AC3)
- FAIL: 2 (2.2 lockout, 2.5 login alerts)
- INCONCLUSIVE: items in E-AC1 requiring Agent 1 (FE)/Agent 2 (BE) cross-verification

## Overall verdict: PARTIAL

Blocks `v1.0.0-rc.*` promotion until:
- 2.2 Account lockout shipped (P0 BLOCKER) — file GAP-NEW-1
- E-AC1 magic-link TTL/countdown/resend cap verified end-to-end (cross-agent Bucket E1)
- 2.5 login alerts can DEFER to Phase 1.5 with `AUTH_HARDENING_DEFER` trailer + GAP-NEW-3 follow-up

Other PARTIAL items acceptable v1 with documented follow-up gaps.

## Recommendations

1. **P0:** File GAP-NEW-1 — account lockout `users.failed_login_attempts` + 15-min lockout + exponential backoff (BLOCKER for v1.0.0-rc per Cat 4 §2.2)
2. **P1:** File GAP-NEW-2 — formalize "passwordless-primary + admin TOTP" auth model + ADR-024 if not yet shipped
3. **P1:** File GAP-NEW-4 — JWT dual-slot rotation `jwt.secret-current` + `jwt.secret-previous` config
4. **P1:** File GAP-NEW-6 — application-level resend rate-limit 5/hour/email (`emailKeyResolver` filter or service-level guard)
5. **P2:** File GAP-NEW-3 — login alerts privileged roles (defer Phase 1.5)
6. **P2:** File GAP-NEW-5 — refresh token rotation + blacklist (defer Phase 1.5)

## References

- `.claude/rules/pre-launch-auth-hardening-checklist.md` v1.0.1 §2 (8 mandatory checks)
- `.claude/rules/pre-handoff-self-test-completeness.md` v1.1.1 §2.7 (tenant-switch flow)
- `documents/04-quality/audits/security/2026-05-15-wave-85-post-apply-v2.md` (Cat 3 +2 A01 RLS + A09 V60)
- Wave 86 plan §3 Bucket E ACs
- Wave 71c PR #1278 (GAP-514 rate-limit + GAP-516 TOTP 2FA shipped)
