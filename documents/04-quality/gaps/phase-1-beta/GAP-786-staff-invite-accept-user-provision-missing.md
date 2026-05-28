---
audience: dev
---

# GAP-786 — Staff invite accept service không create user record (Bug #17 Wave meta-6 walk shutdown)

**Status:** 🔵 OPEN
**Priority:** 🔴 P0
**Domain:** Backend
**Found:** 2026-05-28 (Wave meta-6 Bucket A RST walk shutdown — see `documents/04-quality/audits/rst-html/2026-05-28-wave-meta-6-bucket-a-walk-shutdown-findings.md` §Bug class F #17)
**Phase:** phase-1-beta

## Problem

`StaffInvitationServiceImpl.accept()` marks invitation `ACCEPTED` + sets `acceptedAt` timestamp, but **does NOT create user record**. Password from accept request is dropped on the floor. Code comment self-documents the deferral:

```java
// acceptedUserId set by gateway after it provisions the User row;
// gateway calls back via internal endpoint OR we update via a follow-up
// attach call. For MVP the field remains null until gateway integration
// lands (paired GAP-779 KH auth /me endpoint).
```

The reference `paired GAP-779` is **misleading** — GAP-779 = adds `GET /api/auth/me` endpoint (different concern entirely). Real user-provision-on-accept integration was never implemented + never tracked as gap until this filing.

**Consequence:** Staff who claim invitation via accept flow:
1. ✅ Invitation marked ACCEPTED in DB
2. ❌ No user record created
3. ❌ No auth credentials provisioned
4. ❌ Cannot log in afterward

Feature non-functional end-to-end despite invitation row appearing "claimed".

## Reproduction

Per Wave meta-6 Bucket A walk shutdown 2026-05-28:

```bash
# Owner POST invite (works after walk-fixes)
# Recipient accepts via curl:
curl -X POST "http://localhost:9000/api/v1/staff-invitations/<token>/accept" \
  -H 'Content-Type: application/json' \
  -H 'X-Instance-Subdomain: sky-edu-test' \
  -d '{"password":"StaffTest@2026","fullName":"Thầy Tâm"}'
# → 200 OK with acceptedAt

# Try login as new staff:
curl -X POST "http://localhost:9000/api/auth/login" \
  -d '{"email":"staff.test1@test.vn","password":"StaffTest@2026"}'
# → 400 "Invalid email or password"

# Verify in DB:
docker exec kite-postgres psql -U kitehub -d kitehub \
  -c "SELECT email FROM users WHERE email = 'staff.test1@test.vn';"
# → 0 rows
```

## Root Cause

`StaffInvitationServiceImpl.accept()` lines 155-175 (Wave meta-6 Bucket A PR #1904 ship):

```java
Instant now = Instant.now();
invitation.setStatus(StaffInvitationStatus.ACCEPTED);
invitation.setAcceptedAt(now);
// acceptedUserId set by gateway after it provisions the User row;
// gateway calls back via internal endpoint OR we update via a follow-up
// attach call. For MVP the field remains null until gateway integration
// lands (paired GAP-779 KH auth /me endpoint).
invitationRepository.save(invitation);
```

User provisioning architecture decision was deferred but never followed through. Code path missing:
- Hash password from request
- Create user row in `users` table với role from invitation
- Set `invitation.acceptedUserId` to created user ID
- Emit event for downstream consumers (welcome email, audit log, etc.)

## Proposed Fix

**Architecture decision needed:** which service owns user provisioning?

**Option A** — kiteclass-core creates user directly:
- Add `UserRepository` injection to `StaffInvitationServiceImpl`
- Hash password (BCrypt min cost 12 per `pre-launch-auth-hardening-checklist.md` §2.6)
- Create user row với `tenant_id = invitation.instanceId`, `role = invitation.role`, `email = invitation.email`
- Update `invitation.acceptedUserId` to new user ID
- Pros: simplest, all in one service
- Cons: kiteclass-core writes to central kitehub `users` table (cross-DB concern)

**Option B** — Gateway callback to kitehub-platform:
- kiteclass-core publishes `StaffInvitationAccepted` event với `email, role, hashedPassword, tenantId`
- kitehub-platform consumer creates user
- Callback updates `invitation.acceptedUserId`
- Pros: clean DB ownership (kitehub-platform owns users)
- Cons: complex eventual-consistency, requires outbox + binding

**Option C** — Direct kitehub-platform endpoint:
- kiteclass-core HTTP POST to `kitehub-platform:/api/internal/users/provision-from-staff-invite`
- Synchronous response with userId
- kiteclass-core updates `invitation.acceptedUserId` immediately
- Pros: simpler than eventual-consistency, cleaner than direct DB write
- Cons: synchronous coupling

**Recommendation:** Option C — synchronous internal endpoint. Aligns với existing kitehub-platform pattern (creates users in central DB). Simpler than Option B outbox. Avoids Option A's cross-DB write.

## Acceptance Criteria

- [ ] Architecture decision Option A/B/C logged in this gap §Log
- [ ] User record created in `users` table với correct tenant_id + role + hashed password after accept
- [ ] `invitation.acceptedUserId` populated với new user ID
- [ ] Login as new staff WORKS (Bước 2.10 walk PASS)
- [ ] `feature-ship-runtime-walk-mandate.md` v1.0.0 §3 walk evidence in gap closure (HTTP code + DB row + login JWT verified)
- [ ] Test: `StaffInvitationServiceImpl.accept()` IT covers user-create path
- [ ] Code comment in service updated reference real GAP-786 (not phantom GAP-779)

## Related

- Walk shutdown findings: `documents/04-quality/audits/rst-html/2026-05-28-wave-meta-6-bucket-a-walk-shutdown-findings.md` Bug class F #17
- Sister gap GAP-787 (Bug #14 email never sent — paired feature gap)
- META rule: `.claude/rules/feature-ship-runtime-walk-mandate.md` v1.0.0 — this gap closure MUST satisfy walk evidence requirement
- Wave meta-6 Bucket A: `documents/03-planning/waves/wave-2026-05-27-meta-6-fix-p0-meta-update-rst-html.md` (re-classification candidate per audit retro doc)
- Related (NOT same scope): GAP-779 `/api/auth/me endpoint missing` — code comment mis-referenced this

## Log

- **2026-05-28** — Filed in response to Wave meta-6 Bucket A RST walk shutdown (17 bugs surfaced). P0 because feature non-functional end-to-end — staff cannot log in after accept. Architecture decision deferred to wave plan. Walk-fix not applicable (feature path missing, not bug to patch). Phase 2 BETA Wave B scope per audit retro recommendation.
