---
title: Wave flow-kh2 — Auth + Onboarding walk
status: active
created: 2026-06-03
updated: 2026-06-03
waves: [flow-kh2]
tag_primary: flow
tags_secondary: [kh2, auth, onboarding, campaign]
counter: 1
gaps: []
campaign: flow-verification-campaign
---

# Wave flow-kh2 — Auth + Onboarding walk

**Goal:** Walk end-to-end flow KH-2 (register → email verify → login → 2FA → onboarding wizard) trên production-equivalent stack, đạt **G1 PASS** (per `flow-verification-campaign.md` §1 3-gate); hand off G2 (human local test) + G3 (production parity).
**Trigger:** Flow đầu tiên (root) trong campaign topological order — mọi flow authenticated sau đó depend KH-2. Per CLAUDE.md §🔄 Flow Verification Campaign sub-mode.
**Estimated wall-clock:** Loop đầu ~45-90 min (build + stack-up + walk + batch-fix 1 cycle); subsequent ~15-30 min/cycle.

---

## 1. Brainstorm

**Q1 (alignment):** Persona `Owner` (tenant creator, role OWNER) + `PlatformAdmin` (role PLATFORM_ADMIN/OWNER backward-compat, totp_required=true). Domain `auth` + `auth-2fa` + `onboarding`. Root dependency cho mọi flow authenticated subsequent (KH-1/3, KC-1..12).

**Q2 (trade-offs):** Walk 5 sub-step cùng wave (vs tách micro-flow):
- Token-passing register→verify→login→2FA-challenge→wizard tight coupling; split mất context continuity
- Per `feature-ship-runtime-walk-mandate` §3.4 catalog-then-batch: walk hết → catalog → 1 rebuild → re-walk. Tách = N rebuild waste
- Reject parallel agents: walk yêu cầu live state continuity (cookie/JWT/email link/TOTP secret) — không disjoint

**Q3 (risks):**
- Email verify link production-parity (G3): local MailHog vs production AWS SES sign key khác — note inline khi walk
- 2FA TOTP secret stability: TOTP cần synced clock + persistent secret
- Onboarding wizard race: lazy-init checklist trong cùng transaction (UC-ONBOARD-001 step 5); verify không double-row qua re-login
- Stack-up missing service: KH-2 cần `kitehub-platform/subscription/email/admin` + gateway + FE; kiteclass-* optional

---

## 2. Task Breakdown

| Bucket | Scope | Owner | Effort | Disjoint? |
|--------|-------|-------|--------|-----------|
| A | Loop walk + catalog (5 sub-step) | claude (this session) | 30-60min | n/a — single agent |
| B | Batch-fix blocker (nếu lòi) | claude | 10-30min/cycle | n/a |
| C | Re-walk + G1 verdict | claude | 15min/cycle | n/a |

Single-agent campaign-loop, KHÔNG spawn parallel agents vì walk yêu cầu live state continuity.

---

## 3. Scope

**Stake tier:** MEDIUM — root flow ảnh hưởng mọi flow downstream nhưng walk-only, không refactor production code (fix-then-walk OK nhưng major refactor → wave riêng)
**Cross-layer?:** YES (FE wizard + BE auth + email + Postgres + Redis) — walk-only nên không cần Bucket 0 Foundation; contract đã ship Wave 72b

### 3.1 Files in scope

| Bucket | Files (glob) | Spawn order |
|:------:|--------------|:-----------:|
| A | (read-only walk evidence; live state HTTP/DB/email) | serial |
| B | `kitehub/kitehub-subscription/src/main/java/**/auth/**`, `kitehub-frontend/src/app/(auth)/**` hoặc tương đương (varies theo blocker) | after A catalog |
| C | wave plan §8 status + campaign table | after B |

### 3.2 Walk scope — 5 sub-step

**S1 — Register (POST /api/auth/register)**
- Actor: Anonymous → Owner; Endpoint per `seed-data.sh:32` + `AuthController.java:39`
- Body: `{organizationName, subdomain, ownerEmail, ownerPassword}` → 201 + `{accessToken, instance, user}`
- Happy: DB row `users` (email_verified=false) + DB row `instances/tenants` + MailHog verify message
- Sad: email exists → 409; subdomain exists → 409; captcha fail → 400 (local bypass `captcha.enabled=false`)
- Evidence: HTTP code + JSON keys + `psql SELECT email, email_verified, role FROM users` + MailHog `curl http://localhost:8025/api/v2/messages`

**S2 — Email verify (POST /api/auth/verify-email?token=)**
- Actor: Owner (email_verified=false); Endpoint per `AuthController.java:78`
- Happy: token UUID query param → 200 + `LoginResponse {accessToken, refreshToken, user}` + DB `email_verified=true`
- Sad: token invalid/expired → 400/404
- Evidence: HTTP code + `psql SELECT email_verified FROM users`

**S3 — Login (POST /api/auth/login)**
- Actor: Owner verified; per UC-AUTH-001
- Happy (no 2FA Owner): `{email, password}` → 200 + JWT tokens + `login_audit_log` row
- Sad: wrong password → 401; 5 fails → 423 LOCKED (BR-AUTH-001)
- Evidence: HTTP code + JWT decoded (tenantId claim) + DB `login_audit_log`

**S4 — 2FA enroll/challenge (PLATFORM_ADMIN branch)**
- Actor: `admin@kitehub.com / Admin@KiteHub123` (seed); per UC-AUTH-002/003
- Happy: Login → `{requires2fa_enrollment, challenge_token}` → `/login/2fa-enroll` → enroll-init `{secret, qr_uri, recovery_codes[10]}` → enroll-confirm với TOTP code → 200 + `totp_enrolled_at=now()` + access_token
- Sad: wrong TOTP → 401 `INVALID_TOTP_CODE`
- Evidence: HTTP codes + `psql SELECT totp_enrolled_at FROM users WHERE email='admin@kitehub.com'` + recovery codes shown once

**S5 — Onboarding wizard (GET/PUT /api/v1/onboarding-progress)**
- Actor: Owner post-login; per UC-ONBOARD-001/002
- Happy GET: lazy-init row `onboarding_progress` 5-step + `completionPercent:0`; PUT step → state update + emit `onboarding.demo-data.requested`/`onboarding.completed` outbox event nếu trigger
- Sad: JWT thiếu tenantId claim → 403 `TENANT_CONTEXT_MISSING`
- Evidence: HTTP 200 GET/PUT + DB row `onboarding_progress` + FE checklist render

### 3.3 Persona credentials

| Persona | Email | Password | Role | Tạo thế nào |
|---|---|---|---|---|
| Owner (S1-S3, S5) | `owner+kh2walk@example.com` | (S1 sets `Walk@KH2Test123`) | OWNER | S1 register |
| PlatformAdmin (S4) | `admin@kitehub.com` | `Admin@KiteHub123` | OWNER canonical / PLATFORM_ADMIN alias (V46) | V9 Flyway seed |

Per `pre-handoff-self-test-completeness` §2.1 (a): credential cite trong walk evidence + handoff.

---

## 4. State-Check Evidence

Pre-walk state verified 2026-06-03:

- ✅ Branch `wave/2026-06-03-flow-kh2-auth-onboarding` từ main HEAD `8ebf11c6` clean
- ✅ Campaign topological order — KH-2 root (no upstream dep)
- ✅ Auth use-cases: `documents/01-business/kitehub/auth/use-cases.md` UC-AUTH-001 + auth-2fa UC-AUTH-002/003 + onboarding UC-ONBOARD-001/002
- ✅ Register endpoint: body `{organizationName, subdomain, ownerEmail, ownerPassword}` → 201 + accessToken (verified `seed-data.sh:32` + `AuthController.java:39`)
- ✅ Verify-email endpoint: `POST /api/auth/verify-email?token=<uuid>` (verified `AuthController.java:78`)
- ✅ Captcha bypass local: `@Value("${captcha.enabled:false}")` (verified `CaptchaService.java:29`)
- ✅ Seed admin: `admin@kitehub.com / Admin@KiteHub123` (`seed-data.sh:217`)
- ✅ Build images latest: `bash kitehub/scripts/build-all.sh` ✅ DONE (background, exit 0, 8 KH images built)
- ✅ Stack-up: `bash kitehub/scripts/up.sh` ✅ DONE — infra 5/5 healthy (postgres/redis/rabbit/minio/mailhog) + KH BE 4/4 healthy (admin/branding/email/subscription); gateway+FE starting (~5s)
- ⏳ Gateway + FE health: starting; chờ ~30-60s before walk S1

Per `pre-mutation-state-check.md` — pre-walk state-check artifact tách rời TRƯỚC khi mutate stack/code.

---

## 5. Verification Gates

Per `flow-verification-campaign.md` §1 3-gate:

| Gate | Ai | Tiêu chí | Status |
|---|---|---|---|
| **G1 — Agent runtime walk** | Claude (this session) | Walk 5 sub-step PASS, happy + ≥1 sad path, evidence (HTTP + DB + side effect) cited §7 | ⬜ |
| **G2 — Human real local test** | User | User tự test 5 sub-step trên local stack → confirm trải nghiệm thật | ⬜ |
| **G3 — Production-parity guarantee** | Claude + User | Walk production-equivalent (Postgres+Flyway thật không H2, prod-profile config, gateway JWT→header, env-var đủ) per `local-fix-production-parity-check.md` | ⬜ — chú ý S2 verify-email link prod SES vs local MailHog |

Flow `✅ THÔNG` khi G1+G2+G3 đều PASS. G1 đạt → flip campaign row `🔄 walk-pass-pending-human` chờ G2.

---

## 6. Agent Spawn Pattern

**Single-agent loop** (no parallel spawn). Wave KH-2 = walk + batch-fix campaign loop per `feature-ship-runtime-walk-mandate` §3.4:

```
1. (DONE) Build images latest (background)
2. (DONE) Stack up production-equivalent (no H2, full profile)
3. Walk 5 sub-step → catalog mọi blocker đến hết (no rebuild giữa chừng)
4. Batch-fix P0/P1 blocker → single rebuild → re-walk
5. Lặp 3-5 đến khi G1 PASS toàn 5 sub-step
6. Hand user G2 + xác nhận G3
7. Flip flow KH-2 ✅ THÔNG campaign §4 + evidence vào §7 closure
```

Gap chặn flow lòi ra → fix tại chỗ + file gap inline per `discovery-to-gap-inline-filing`. Gap không chặn → defer, không fix trong campaign.

### 6.1 Catalog blocker (live — fill khi walk)

| # | Sub-step | Blocker | Severity | Root cause | Fix idea | Workaround |
|---|---|---|---|---|---|---|
| _(điền khi walk lòi ra)_ |

Anti-pattern per rule §3.4: KHÔNG rebuild giữa walk; catalog hết → batch fix → 1 rebuild → re-walk.

---

## 7. Closure Protocol

### 7.1 Walk evidence (per `feature-ship-runtime-walk-mandate` §3) — fill sau khi G1 PASS

```
Stack: kitehub/scripts/up.sh full profile, N/N services healthy
Persona: Owner owner+kh2walk@example.com + PlatformAdmin admin@kitehub.com

S1 register: HTTP 201 + DB row id=X + MailHog message subject "..."
S2 verify: HTTP 200 + LoginResponse {accessToken} + email_verified=true
S3 login: HTTP 200 + JWT decoded {tenantId, role: OWNER, exp} + login_audit_log row
S4 2FA: HTTP 200 enroll-init + 200 enroll-confirm + totp_enrolled_at NOT NULL + 10 recovery codes shown
S5 wizard: HTTP 200 GET 5 step lazy-init + PUT step1 + completionPercent: 20

Verdict: ✅ G1 PASS; chờ G2 human local test + G3 production parity confirm
```

### 7.2 Status (live)

⬜ S1 register · ⬜ S2 verify · ⬜ S3 login · ⬜ S4 2FA · ⬜ S5 wizard
**Gate:** G1 ⬜ · G2 ⬜ · G3 ⬜

### 7.3 Scope-Completeness Reconciliation (per `wave-closure-scope-completeness.md` §3) — fill khi closure

| # | Plan §3 Scope item | Verdict | Follow-up |
|---|---|---|---|
| 1 | Bucket A — Walk 5 sub-step | (TBD) | — |
| 2 | Bucket B — Batch-fix blocker | (TBD nếu lòi blocker) | — |
| 3 | Bucket C — Re-walk + G1 verdict | (TBD) | — |
| 4 | G2 human local test | (Hand off user) | Campaign row flip pending |
| 5 | G3 production parity confirm | (Hand off post G2) | Campaign §1 G3 |

### 7.4 Post-wave cleanup

```bash
bash scripts/prune-merged-worktreed.sh --yes  # nếu spawn worktree (KH-2 không spawn, skip)
```

KH-2 single-agent — no worktree spawn → cleanup N/A. Campaign row update trong cùng PR closure.

---

## 8. Log

- **2026-06-03**: Wave plan tạo. Build images latest (`bash kitehub/scripts/build-all.sh` exit 0). Stack-up (`bash kitehub/scripts/up.sh` exit 0) — infra + KH BE healthy; gateway + FE starting. Loop protocol per `feature-ship-runtime-walk-mandate` §3.4 catalog-then-batch ready. Walk S1 register sẽ bắt đầu sau khi gateway + FE health.
