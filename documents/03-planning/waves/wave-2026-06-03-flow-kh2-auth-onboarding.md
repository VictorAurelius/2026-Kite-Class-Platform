---
title: Wave flow-kh2 — Auth + Onboarding walk
status: active
created: 2026-06-03
updated: 2026-06-03
waves: [flow-kh2]
tag_primary: flow
tags_secondary: [kh2, auth, onboarding, campaign]
gaps: []
campaign: flow-verification-campaign
---

# Wave flow-kh2 — Auth + Onboarding walk

**Goal:** Walk end-to-end flow KH-2 (register → email verify → login → 2FA → onboarding wizard) trên production-equivalent stack, đạt **G1 PASS** (per `flow-verification-campaign.md` §1 3-gate); chuẩn bị hand off G2 (human local test) + G3 (production parity).
**Trigger:** Flow đầu tiên (root) trong campaign topological order — mọi flow authenticated sau đó depend lên KH-2. Per CLAUDE.md §🔄 Flow Verification Campaign sub-mode.
**Estimated wall-clock:** Loop đầu ~45-90 min (build + stack-up + walk + batch-fix 1 cycle); subsequent loop ~15-30 min/cycle.

---

## 1. Brainstorm (5-10 min)

**Q1 (alignment):** Phục vụ persona `Owner` (tenant creator, role OWNER) + `PlatformAdmin` (role PLATFORM_ADMIN — 2FA mandatory branch). Domain `auth` + `auth-2fa` + `onboarding`. Depend xuống bởi mọi flow authenticated subsequent (KH-1/3/...KC-1..12).

**Q2 (trade-offs):** Walk full register-to-wizard cùng wave thay vì tách micro-flow vì:
- 5 sub-step liên kết tight (token-passing register→verify→login→2FA-challenge→wizard); split = mất context continuity
- Per `feature-ship-runtime-walk-mandate` §3.4 catalog-then-batch: walk hết → catalog → batch-fix 1 rebuild → re-walk. Tách wave = N rebuild waste
- Alternative rejected: spawn parallel agents cho từng sub-step. Lý do reject: walk yêu cầu live state continuity (cookie/JWT/email link), không disjoint

**Q3 (risks):**
- Email verify link production-parity (G3): local MailHog vs production AWS SES/Resend signs khác key — note inline khi walk; verify side-effect (link works, không 404) per `pre-handoff-self-test-completeness` §2.3
- 2FA TOTP secret stability: TOTP requires synced clock + persistent secret; verify DB row `users.totp_enrolled_at` post-confirm
- Onboarding wizard race: lazy-init checklist trong cùng transaction (UC-ONBOARD-001 step 5); verify không double-row qua re-login
- Stack-up missing service: KH-2 cần `kitehub-platform` + `kitehub-subscription` (auth code) + `kitehub-email` (verify mail) + `kitehub-gateway` + `kitehub-frontend` + Postgres/Redis. Nếu service down giữa walk → catalog, không rebuild giữa chừng

---

## 2. Loop protocol (per `feature-ship-runtime-walk-mandate` §3.4)

```
1. ⏳ Build images mới nhất (background — đang chạy `bash kitehub/scripts/build-all.sh`)
2. ⏳ Stack up: `bash kitehub/scripts/up.sh` + `bash kitehub/scripts/wait-for-healthy.sh` (production-parity, không H2)
3. Walk 5 sub-step (§3) → catalog mọi blocker đến hết (no rebuild giữa chừng)
4. Batch-fix P0/P1 blocker → single rebuild → re-walk
5. Lặp 3-5 đến khi G1 PASS toàn 5 sub-step
6. Hand user G2 (human local test) + xác nhận G3 (production parity)
7. Flip flow KH-2 ✅ THÔNG trong `flow-verification-campaign.md` §4 + evidence vào §5 wave plan
```

Gap chặn flow lòi ra → fix tại chỗ + file gap inline per `discovery-to-gap-inline-filing`. Gap không chặn → ghi defer + không fix trong campaign.

---

## 3. Walk scope — 5 sub-step

### S1 — Register (POST /api/auth/register)

**Actor:** Anonymous → Owner (tenant creator)
**Endpoint:** `POST /api/auth/register` (per `documents/01-business/roles/api-contract.md:55`)
**Happy path:**
- Submit `{email, password, full_name, tenant_name (or invite_token)}` → BE create user + tenant + assign OWNER role + send verify email
- Expect: HTTP 201 + DB row `users` (status=PENDING_VERIFY) + DB row `tenants` + side effect: email-verify message vào MailHog

**Sad path:** email đã tồn tại → HTTP 409 `EMAIL_EXISTS`

**Evidence cần catalog:** HTTP code + `psql SELECT email, email_verified_at, role FROM users WHERE email=...` + MailHog `curl http://localhost:8025/api/v2/messages | jq '.items[0].Content.Headers.Subject'`

### S2 — Email verify (click link)

**Actor:** Owner (PENDING_VERIFY)
**Endpoint:** `GET /api/auth/verify-email?token=<jwt>` (token từ email body)
**Happy path:**
- Click link → BE validate token → set `users.email_verified_at=now()` → redirect FE `/login` với banner success
- Expect: HTTP 302 → `/login?verified=1` + DB row updated

**Sad path:** token expired (link-expiry-policy.md TTL) → HTTP 400 `TOKEN_EXPIRED`

**Evidence:** HTTP code + DB row `email_verified_at NOT NULL` + FE banner render

### S3 — Login (POST /api/auth/login)

**Actor:** Owner (verified)
**Endpoint:** `POST /api/auth/login` (per `documents/01-business/kitehub/auth/use-cases.md` UC-AUTH-001)
**Happy path (Owner, no 2FA):** Submit `{email, password}` → HTTP 200 + `{access_token, refresh_token, user}` + DB row `login_audit_log`
**Sad path:** wrong password → 401 + `failed_login_attempts++`; 5 fails → 423 LOCKED per BR-AUTH-001

**Evidence:** HTTP code + JWT decoded (jwt.io) verify `tenantId` claim + DB `login_audit_log` row

### S4 — 2FA enroll/challenge (PLATFORM_ADMIN branch)

**Actor:** PlatformAdmin (separate persona, role=PLATFORM_ADMIN, totp_required=true)
**Endpoints:** `POST /api/auth/2fa/enroll-init` + `POST /api/auth/2fa/enroll-confirm` (UC-AUTH-002) OR `POST /api/auth/2fa/challenge` (UC-AUTH-003 nếu đã enrolled)
**Happy path:**
- Login → `{requires2fa_enrollment: true, challenge_token}` → FE redirect `/login/2fa-enroll?token=...`
- Init returns `{secret, qr_uri, recovery_codes:[10]}`
- Scan QR + nhập first TOTP code → confirm → `users.totp_enrolled_at=now()` + issue access_token

**Sad path:** wrong TOTP → 401 `INVALID_TOTP_CODE`

**Evidence:** HTTP codes + `psql SELECT totp_enrolled_at, totp_secret FROM users WHERE email='admin@kitehub.me'` (secret encrypted) + recovery codes shown ONCE

### S5 — Onboarding wizard (GET /api/v1/onboarding-progress)

**Actor:** Owner (post-login)
**Endpoint:** `GET /api/v1/onboarding-progress` (per `documents/01-business/kitehub/onboarding/use-cases.md` UC-ONBOARD-001)
**Happy path:**
- FE auto-fetch sau login → BE lazy-init row `onboarding_progress` (5 step, completed=false) → return `{steps:[...], completionPercent:0}`
- Click 1 step → `PUT /api/v1/onboarding-progress {stepId, completed:true}` (UC-ONBOARD-002) → state update + completionPercent recompute

**Sad path:** JWT thiếu `tenantId` claim → 403 `TENANT_CONTEXT_MISSING`

**Evidence:** HTTP 200 + DB row `onboarding_progress` lazy-init + FE checklist render 5 step + state update sau PUT

---

## 4. Persona credentials cần chuẩn bị

| Persona | Email | Password | Role | Tạo thế nào |
|---|---|---|---|---|
| Owner (S1-S3, S5) | `owner+kh2walk@example.com` | (S1 sets) | OWNER | Tạo qua S1 register |
| PlatformAdmin (S4) | `admin@kitehub.me` | (seed) | PLATFORM_ADMIN | Per seed-data.sh hoặc manual SQL |

Per `pre-handoff-self-test-completeness` §2.1 (a): credential PHẢI cite trong walk evidence + handoff message.

---

## 5. Catalog blocker (live — fill khi walk)

| # | Sub-step | Blocker | Severity | Root cause | Fix idea | Workaround |
|---|---|---|---|---|---|---|
| _(điền khi walk lòi ra)_ |

---

## 6. Evidence ghi sau khi G1 PASS

Per `feature-ship-runtime-walk-mandate` §3:

```markdown
## Walk evidence (per feature-ship-runtime-walk-mandate.md §3)

**Stack:** kitehub/scripts/up.sh full profile, 8/8 services healthy
**Persona:** Owner owner+kh2walk@example.com + PlatformAdmin admin@kitehub.me

S1 register: HTTP 201 + DB row id=X + MailHog message subject "Xác thực email"
S2 verify: HTTP 302 → /login?verified=1 + email_verified_at NOT NULL
S3 login: HTTP 200 + JWT decoded {tenantId: ..., role: OWNER, exp: ...}
S4 2FA: HTTP 200 init + 200 confirm + totp_enrolled_at NOT NULL + 10 recovery codes shown
S5 wizard: HTTP 200 + 5 step lazy-init + PUT step1 + completionPercent: 20

**Verdict:** ✅ G1 PASS; chờ G2 human local test + G3 production parity confirm
```

---

## 7. Status

⬜ S1 register — chưa walk
⬜ S2 verify — chưa walk
⬜ S3 login — chưa walk
⬜ S4 2FA enroll/challenge — chưa walk
⬜ S5 onboarding wizard — chưa walk

**Gate:** G1 ⬜ · G2 ⬜ · G3 ⬜

---

## 8. Log

- **2026-06-03**: Wave plan tạo. Build images background (`bash kitehub/scripts/build-all.sh` in background). Stack-up + walk sẽ trigger sau khi build complete. Loop protocol per `feature-ship-runtime-walk-mandate` §3.4 catalog-then-batch.
