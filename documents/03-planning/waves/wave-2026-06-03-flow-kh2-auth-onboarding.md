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

## 2. Task Breakdown

| Bucket | Scope | Owner | Effort | Disjoint? |
|--------|-------|-------|--------|-----------|
| A | Loop walk + catalog (5 sub-step) | claude (this session) | 30-60min | n/a — single agent, no parallel disjoint (walk needs live state continuity) |
| B | Batch-fix blocker (nếu lòi) | claude (this session) | 10-30min/cycle | n/a |
| C | Re-walk + G1 verdict | claude (this session) | 15min/cycle | n/a |

Single-agent campaign-loop, không spawn parallel agents vì walk yêu cầu live state continuity (cookie/JWT/email link/2FA secret) — không disjoint qua sub-step.

---

## 3. Scope

**Stake tier:** MEDIUM — root flow ảnh hưởng mọi flow downstream nhưng KH-2 chỉ verify-and-walk, không refactor production code (fix-then-walk OK nhưng major refactor cần wave riêng)
**Cross-layer?:** YES (FE wizard + BE auth + email service + Postgres + Redis) — nhưng walk-only nên không cần Bucket 0 Foundation; production contract đã ship Wave 72b

| # | Bucket | Files (glob) | Spawn order |
|:-:|--------|--------------|:-----------:|
| A | Loop walk 5 sub-step | (read-only walk evidence) | serial |
| B | Batch-fix blocker discovered | (varies — likely `kitehub/kitehub-subscription/src/main/java/**/auth/**` hoặc FE auth flow) | after A catalog complete |
| C | Re-walk verdict | (campaign-table + wave plan §7 status) | after B |

---

## 4. State-Check Evidence

Pre-walk state verified 2026-06-03 trước khi bắt đầu:

- ✅ Branch `wave/2026-06-03-flow-kh2-auth-onboarding` tạo từ main HEAD `8ebf11c6` (clean)
- ✅ Campaign `flow-verification-campaign.md` §3 topological order — KH-2 = root (no upstream dep)
- ✅ Auth use-cases verified: `documents/01-business/kitehub/auth/use-cases.md` UC-AUTH-001 + onboarding UC-ONBOARD-001/002 + auth-2fa UC-AUTH-002/003
- ✅ Register endpoint actual shape: `kitehub/scripts/seed-data.sh:32` register_user() + `AuthController.java:39` — body `{organizationName, subdomain, ownerEmail, ownerPassword}`, returns 201 + accessToken
- ✅ Verify-email endpoint: `AuthController.java:78` POST /api/auth/verify-email?token=<uuid>
- ✅ Captcha bypass mặc định local: `CaptchaService.java:29` `@Value("${captcha.enabled:false}")` — local không cần captcha token
- ✅ Seed admin: `admin@kitehub.com / Admin@KiteHub123` per `kitehub/scripts/seed-data.sh:217` (V46 PLATFORM_ADMIN aka OWNER backward-compat)
- ✅ Stack scripts: `kitehub/scripts/build-all.sh` + `up.sh` + `wait-for-healthy.sh` confirmed existing
- ⏳ Build images: in progress background (PID 979498) — kitehub-base Maven build pull deps (long-running first time)
- ⏳ Stack-up: chờ build complete

Per `pre-mutation-state-check.md` — pre-walk state-check artifact pháp tách rời trước khi mutate stack/code; assumptions verified empirically.

---

## 5. Verification Gates

Per `flow-verification-campaign.md` §1 3-gate:

| Gate | Ai | Tiêu chí | Status |
|---|---|---|---|
| **G1 — Agent runtime walk** | Claude (this session) | Walk end-to-end 5 sub-step PASS, happy + ≥1 sad path, evidence (HTTP + DB + side effect) cited inline §7 | ⬜ |
| **G2 — Human real local test** | User | User tự test 5 sub-step trên local stack → confirm trải nghiệm thật đúng | ⬜ |
| **G3 — Production-parity guarantee** | Claude + User | Walk trên production-equivalent (Postgres+Flyway thật không H2, prod-profile config, JWT→header gateway, env-var đủ) per `local-fix-production-parity-check.md` | ⬜ — chú ý S2 verify email link production-key SES vs local MailHog |

Flow chỉ `✅ THÔNG` khi G1 + G2 + G3 đều PASS. G1 đạt → flip campaign row `🔄 walk-pass-pending-human` chờ G2.

---

## 6. Loop protocol (per `feature-ship-runtime-walk-mandate` §3.4)

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

## 7. Walk scope — 5 sub-step

### S1 — Register (POST /api/auth/register)

**Actor:** Anonymous → Owner (tenant creator)
**Endpoint:** `POST /api/auth/register` (per `kitehub/scripts/seed-data.sh:32` register_user())
**Body shape (verified từ seed-data.sh):**
```json
{
  "organizationName": "KH-2 Walk Test",
  "subdomain": "kh2-walk-test",
  "ownerEmail": "owner+kh2walk@example.com",
  "ownerPassword": "Walk@KH2Test123"
}
```
**Happy path:** HTTP 201 + body `{accessToken, instance.id (hoặc instances[0].id), user}` + DB row `users` + DB row `tenants/instances` + (possible) email-verify message vào MailHog

**Sad path candidates:** email/subdomain đã tồn tại → 409; password yếu → 400

**Evidence cần catalog:** HTTP code + JSON keys actual returned + `psql SELECT email, email_verified_at, role FROM users WHERE email='owner+kh2walk@example.com'` + MailHog `curl http://localhost:8025/api/v2/messages` (nếu verify email branch wired)

**Open question:** Register returns accessToken immediately — verify whether (a) email-verify is bypassed OR (b) async (token issued với "verified=false" claim) — walk sẽ làm rõ

### S2 — Email verify (POST /api/auth/verify-email)

**Actor:** Owner (email_verified=false từ S1)
**Endpoint:** `POST /api/auth/verify-email?token=<uuid>` (per `AuthController.java:78`)
**Body:** none — token là query param
**Happy path:**
- Token UUID (từ verification email body) → BE validate token → set `users.email_verified=true` → return `LoginResponse {accessToken, refreshToken, user}`
- Expect: HTTP 200 + DB row `email_verified=true`

**Sad path:** token invalid/expired → 400/404

**Evidence:** HTTP code + body keys + `psql SELECT email, email_verified FROM users WHERE email='owner+kh2walk@example.com'`

**Note:** Captcha bypass — `CaptchaService.java` mặc định `captcha.enabled=false`; local dev không cần captcha token cho S1 register. Production verify trong G3.

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

## 8. Persona credentials cần chuẩn bị

| Persona | Email | Password | Role | Tạo thế nào |
|---|---|---|---|---|
| Owner (S1-S3, S5) | `owner+kh2walk@example.com` | (S1 sets) | OWNER | Tạo qua S1 register |
| PlatformAdmin (S4) | `admin@kitehub.com` | (seed V9 — bcrypt hash) | OWNER (canonical) / PLATFORM_ADMIN (backward-compat alias per V46) | V9 Flyway seed; password = look up via `kitehub/seed/ADMIN-CREDENTIALS.md` hoặc reset qua SQL |

Per `pre-handoff-self-test-completeness` §2.1 (a): credential PHẢI cite trong walk evidence + handoff message.

---

## 9. Catalog blocker (live — fill khi walk)

| # | Sub-step | Blocker | Severity | Root cause | Fix idea | Workaround |
|---|---|---|---|---|---|---|
| _(điền khi walk lòi ra)_ |

---

## 10. Evidence ghi sau khi G1 PASS

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

## 11. Status

⬜ S1 register — chưa walk
⬜ S2 verify — chưa walk
⬜ S3 login — chưa walk
⬜ S4 2FA enroll/challenge — chưa walk
⬜ S5 onboarding wizard — chưa walk

**Gate:** G1 ⬜ · G2 ⬜ · G3 ⬜

---

## 12. Log

- **2026-06-03**: Wave plan tạo. Build images background (`bash kitehub/scripts/build-all.sh` in background). Stack-up + walk sẽ trigger sau khi build complete. Loop protocol per `feature-ship-runtime-walk-mandate` §3.4 catalog-then-batch.
