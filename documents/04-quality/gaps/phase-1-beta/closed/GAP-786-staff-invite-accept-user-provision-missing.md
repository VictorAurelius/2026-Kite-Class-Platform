---
audience: dev
---

# GAP-786 — Staff invite accept service không create user record (Bug #17 Wave meta-6 walk shutdown)

**Status:** 🟢 DONE
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

## LOCKED architecture (2026-05-28 Day 2 — Option D Re-host kitehub-subscription canonical)

Day 1 investigation surfaced **all 3 original options non-viable**:

- **Option A (direct UserRepository inject in kiteclass-core)** — non-viable: kiteclass-core (port 5432, `kiteclass_dev` DB) cannot share JPA UserRepository với kitehub-subscription (port 5433, `kitehub` DB). Different datasources + zero Maven dep + zero cross-module imports.
- **Option B (outbox event cross-DB)** — non-viable: kitehub-platform là SHARED LIBRARY (no runtime, no `application.yml`, no HTTP server). Cannot host listener.
- **Option C (sync HTTP to kitehub-platform endpoint)** — non-viable: same reason as B (platform = library, no HTTP server).

**Investigation revealed Option D = Re-host kitehub-subscription canonical:**

Two parallel staff invitation flows exist:
1. `kitehub-subscription/.../staff/controller/StaffInvitationController.java:242` — **DOES create user on accept** (production-proven pre-Wave meta-6 — Wave 80 era ship)
2. `kiteclass-core/.../module/staff/...` — Wave meta-6 MVP **does NOT create user** (Bug #17 root cause, code self-documents BLOCKED ON GAP-786)

User confirmed reversal 2026-05-28 (overriding prior Wave meta-6 routing decision documented in gateway application.yml). Fix scope:

**Changes shipped this PR (Wave A Bucket B):**
- Revert gateway routing — add explicit route `/api/v1/staff-invitations/**` → `kitehub-subscription:8080` (was falling through `instance-apis` catch-all → kiteclass-core)
- Delete kiteclass-core staff module (13 files: 1 controller + 1 service interface + 1 service impl + 1 repository + 1 entity + 4 DTOs + 1 enum + 1 package-info + 2 test classes)
- Add V72 Flyway migration deprecating `staff_invitations` table trong kiteclass-core (COMMENT only — data preserved for rollback safety)
- FE call sites unchanged — URL pattern `/api/v1/staff-invitations/*` same; gateway re-routes to kitehub-subscription transparently

**Effort:** ~2-3 eng-days (vs Option B 5-7 ed estimate trong original Wave A plan).

**Remaining for DONE flip (~30% to go):**
- [x] RST walk per `feature-ship-runtime-walk-mandate.md` §3 — verify accept flow on production-equivalent stack with Owner persona (Bước 2.10 PASS) ✅ 2026-05-28
- [x] Sub-gap follow-up: live walk verification post-merge ✅ walk evidence below

## Walk evidence (per `feature-ship-runtime-walk-mandate.md` §3)

**Stack:** Local Docker Compose, all 10 services healthy 2026-05-28 ~08:00 UTC
**Branch:** `wave/phase2-beta-wave-a-bucket-b-rehost-staff-invitations`
**Persona:** Owner → Staff recipient (cô Linh)

### Step 1 — Stack-up
- 10/10 services healthy (kite-postgres, kite-redis, kite-rabbitmq, kite-minio, kite-mailhog, kite-gateway, kitehub-subscription, kitehub-email, kitehub-admin, kitehub-branding, kiteclass-core, kitehub-frontend, kiteclass-frontend)

### Step 2 — Owner login + JWT verify
- Fresh Owner registered: `walk.owner+bucketb@skyedu.vn` / subdomain `walk-bucket-b-1779954467`
- Login HTTP 200; JWT contains `tenantId=ba8bfdce-2669-44be-b288-cedf73559c8a` (GAP-704 verify confirmed)
- Owner ID: `f1cb6aee-770c-4e24-9763-3eb40721cb38`

### Step 3 — Owner invite staff + routing verify
- `POST /api/v1/staff-invitations` HTTP 201 (with `X-Tenant-Id` header)
- Response shape matches kitehub-subscription format (`tenantId`, `invitedBy`, `expiresAt`)
- DB row in **kitehub.staff_invitations** (NOT kiteclass-core) — routing reverted correctly
- Email sent to MailHog with Vietnamese subject "Bạn được mời tham gia..." (kitehub-subscription sync email path works)
- Token extracted from Q-P encoded email body: `YJpAjCy0jIL2VU63pHQ4mEYuZk_pP1-51IEZ0m3ykLI`
- SHA-256(token) matches `staff_invitations.token_hash` ✓

### Step 4 — Recipient accept (CRITICAL — Bug #17 verification)
- `GET /api/v1/staff-invitations/by-token/{token}` HTTP 200 (post Bug #18+#19 fixes)
- `POST /api/v1/staff-invitations/{token}/accept` HTTP 200
- Response: `{userId: "a2da980b-de63-475a-a8a0-6a921591fb5e", role: "STAFF"}`
- **User row CREATED in `users` table**: id=`a2da980b-...`, name=`Cô Linh`, role=`STAFF`
- Invitation flipped `status=ACCEPTED`, `accepted_at` set, `accepted_user_id=a2da980b-...`

### Step 5 — New staff login
- `POST /api/auth/login` với `WalkStaff@2026B` HTTP 200
- Staff JWT issued (decoded: `sub=a2da980b-...`, `role=STAFF`, `email=teacher.walk+bucketb@skyedu.vn`)
- Tenant-scoped endpoint `/api/v1/onboarding-progress` HTTP 200 với explicit `X-Tenant-Id` header

## Bugs surfaced by walk (per `feature-ship-runtime-walk-mandate.md` v1.1.0 §3.4 catalog-then-batch-fix)

3 bugs surfaced + batch-fixed này PR:

| # | Class | Severity | File | Fix |
|---|---|---|---|---|
| **#18** | Gateway whitelist | P0 (blocked all accept attempts) | `kitehub-gateway/.../JwtAuthenticationGatewayFilter.java:isPublicPath()` | Add `/api/v1/staff-invitations/by-token/**` + `/{token}/accept` to public path list |
| **#19** | Subscription SecurityConfig | P0 (blocked all accept attempts) | `kitehub-subscription/.../SecurityConfig.java:authorizeHttpRequests()` | Add 2 permitAll matchers for public recipient endpoints |
| **#20** | STAFF JWT thiếu tenantId claim | P1 (architectural — STAFF tenant binding deferred) | `kitehub-subscription/.../AuthService.resolveTenantIdForRole()` lines 644-660 | DEFER — file follow-up gap. Workaround: FE pass `X-Tenant-Id` header from subdomain (Bug #16 walk-fix pattern). Beta production uses subdomain → gateway resolver handles. |

**Bug #18 + #19 fixed inline this PR** (same scope — re-host re-exposed previously-hidden ghost-guard class).
**Bug #20 follow-up gap to file** post-merge (architectural choice between Option 1/2/3 — defer Wave A Bucket B closure to not bloat scope).

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

- **2026-05-28** (Wave A Bucket B DONE 100%) — RST walk PASS on local production-equivalent stack. End-to-end flow verified: Owner login → invite POST → email sent → recipient accept → user record created → staff login PASS. Bug #17 RESOLVED. Walk surfaced 2 new P0 bugs (Gateway whitelist + Subscription SecurityConfig — both batch-fixed this PR per `feature-ship-runtime-walk-mandate.md` v1.1.0 §3.4 catalog-then-batch protocol) + 1 P1 deferred (STAFF JWT tenantId — follow-up gap). Wave A Bucket B SHIPPED. META rule v1.1.0 added inline §3.4 batch-fix workflow discipline closing inline-rebuild thrash anti-pattern.
- **2026-05-28** (Wave A Bucket B PARTIAL ~70%) — Day 1 investigation 3 Opus background agents revealed cross-DB blocker (kiteclass-core port 5432 vs kitehub-subscription port 5433 separate DBs + zero Maven dep) — Options A/B/C all non-viable. Day 2 user-confirmed reversal: re-host kitehub-subscription canonical (Option D). Code fix shipped this PR: (1) gateway routing reverted `/api/v1/staff-invitations/**` → kitehub-subscription (overrides instance-apis catch-all + reverses Wave meta-6 documented routing decision); (2) kiteclass-core staff module deleted (13 files); (3) V72 Flyway migration deprecates kiteclass-core `staff_invitations` table (COMMENT-only, data preserved). FE unchanged. Remaining ~30%: RST walk verify per `feature-ship-runtime-walk-mandate.md` §3. Sub-gap follow-up pending live walk.
- **2026-05-28** — Filed in response to Wave meta-6 Bucket A RST walk shutdown (17 bugs surfaced). P0 because feature non-functional end-to-end — staff cannot log in after accept. Architecture decision deferred to wave plan. Walk-fix not applicable (feature path missing, not bug to patch). Phase 2 BETA Wave B scope per audit retro recommendation.
