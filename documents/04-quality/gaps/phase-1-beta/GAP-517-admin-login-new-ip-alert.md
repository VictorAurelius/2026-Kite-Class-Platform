# GAP-517: PLATFORM_ADMIN login alert from new IP/UA

**Status:** 🟡 PARTIAL (BE wired Wave 72b Bucket C; Resend template config = ops follow-up)
**Priority:** 🟡 P2 (defense-in-depth)
**Domain:** Backend
**Found:** 2026-05-13 (Wave 71c per `pre-launch-auth-hardening-checklist.md` §2.5)

## Problem

Không có notification khi admin login từ IP/UA mới. Stealth compromise có thể không bị phát hiện trong nhiều ngày.

## Proposed Fix

1. `LoginAuditService` writes (user_id, ip, user_agent, geo_country, login_at) on every login
2. Compute (ip, ua) fingerprint; if new for PLATFORM_ADMIN → emit email to admin
3. Cooldown 24h per fingerprint (avoid spam)
4. Resend transactional template `admin-new-login-alert`

## Acceptance Criteria

- [x] LoginAuditService entity + repository + migration (V38 + `audit/login/` subpackage, Wave 72b Bucket C)
- [x] Email fires on new fingerprint, NOT on known fingerprint (verified via `LoginAuditServiceTest` — 6 unit tests covering new + known + cooldown + non-admin + null-request paths)
- [ ] Manual test: login từ 2 different browsers → 2nd triggers email — **deferred to ops**: Resend template `admin-new-login-alert` configuration in Resend dashboard needs to land before E2E manual test can succeed (BE EventListener wired + EmailServiceClient.sendAdminNewLoginAlert dispatched; if template absent → kitehub-email service logs warning and login flow continues unaffected)

## Log

- **2026-05-14** Wave 72b Bucket C — BE implementation shipped. Migration V38 + 5 new classes in `com.kitehub.subscription.audit.login` sub-package (separate from Wave 72a's `audit.admin` admin-audit scope). AuthService.login(LoginRequest, HttpServletRequest) threads request → LoginAuditService.recordLogin → SHA-256(ip+ua) fingerprint → cooldown check → ApplicationEvent → AdminLoginAlertEventListener (@Async) → EmailServiceClient.sendAdminNewLoginAlert. Email failures swallowed; login response never waits on alert dispatch. Email template name `admin-new-login-alert` documented in `documents/01-business/kitehub/email-lifecycle/api-contract.md` Templates Registry table. Local verify `./mvnw -pl kitehub-subscription verify -P strict-warnings -DskipITs` GREEN for all GAP-517 tests (8/8); pre-existing 6 InstanceControllerIntegrationTest errors are Wave 72a JSONB-on-H2 unrelated bug. Status stays PARTIAL per `gap-done-discipline.md` §3 PARTIAL exit ramp — Resend template config = separate ops follow-up gap before flipping DONE.

## Related

- Rule: `pre-launch-auth-hardening-checklist.md` §2.5
- Sister gap: Wave 72a GAP-521 (admin action audit log, `audit.admin` sub-package)
- Ops follow-up: Resend template `admin-new-login-alert` setup (separate scope)
