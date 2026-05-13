# GAP-517: PLATFORM_ADMIN login alert from new IP/UA

**Status:** 🔵 OPEN
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

- [ ] LoginAuditService entity + repository + migration
- [ ] Email fires on new fingerprint, NOT on known fingerprint
- [ ] Manual test: login từ 2 different browsers → 2nd triggers email

## Related

- Rule: `pre-launch-auth-hardening-checklist.md` §2.5
