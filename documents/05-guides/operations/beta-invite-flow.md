# Beta Invite Flow — End-to-End Runbook

**Domain:** Beta tenant invite mechanism (Phase 1 BETA)
**Closes:** GAP-480 — Beta invitation flow undefined
**Last Updated:** 2026-05-14
**Owner:** Solo dev / Beta coordinator
**References:**
- Business: [`documents/01-business/kitehub/beta-access/`](../../01-business/kitehub/beta-access/) — `rules.md` / `use-cases.md` / `api-contract.md`
- Code (BE): [`kitehub/kitehub-subscription/src/main/java/com/kitehub/subscription/beta/`](../../../kitehub/kitehub-subscription/src/main/java/com/kitehub/subscription/beta/)
- Code (FE): `kitehub/kitehub-frontend/src/app/(public)/request-beta-access/`, `src/app/(auth)/beta-signup/`, `src/app/(admin)/admin/beta-requests/`
- Auth: [`pre-handoff-self-test-completeness.md`](../../../.claude/rules/pre-handoff-self-test-completeness.md) §2.4 admin checklist

---

## 1. Mục đích

Runbook mô tả luồng hoàn chỉnh từ lúc người quan tâm gửi yêu cầu Beta cho tới khi tenant chủ đăng nhập thành công lần đầu. Mỗi bước có:
- Actor (ai làm)
- Trigger (cái gì khởi động)
- Action (làm gì)
- Verify (kiểm tra ra sao)
- Failure mode + recovery

Sử dụng khi:
- Onboarding coordinator mới
- Debug 1 request Beta bị mắc kẹt
- Smoke test luồng end-to-end trước launch
- Audit luồng theo `pre-handoff-self-test-completeness.md` §2.3 (email-driven) + §2.4 (admin) cho `release-deploy-standard.md` §3.1 / §3.4

---

## 2. Tổng quan luồng (5 bước chính)

```
┌─────────────────────────────────────────────────────────────────────┐
│  Bước 1: Public Request                                              │
│  Visitor → POST /api/v1/auth/request-beta-access (PDPL consent)      │
│  → Row PENDING trong `beta_access_request` table                     │
└─────────────────────────────────────────────────────────────────────┘
                                  │
                                  ▼
┌─────────────────────────────────────────────────────────────────────┐
│  Bước 2: Admin Review                                                │
│  Coordinator login → /admin/beta-requests → đọc PENDING list         │
│  → Quyết định: Approve hoặc Reject                                   │
└─────────────────────────────────────────────────────────────────────┘
                                  │
                                  ▼
┌─────────────────────────────────────────────────────────────────────┐
│  Bước 3: Approve → Email Invite                                      │
│  POST /admin/beta-requests/{id}/approve                              │
│  → status APPROVED + invite_token + claim_code (TTL 24h)             │
│  → Outbox event `beta.invite.sent` → Email service render template   │
│  → Resend/SES gửi email kèm link `/beta-signup?claim=<6-digit>`      │
└─────────────────────────────────────────────────────────────────────┘
                                  │
                                  ▼
┌─────────────────────────────────────────────────────────────────────┐
│  Bước 4: Invitee Signup                                              │
│  Invitee click link → FE exchange claim → validate token             │
│  → Form chọn subdomain + password                                    │
│  → POST /api/v1/auth/beta-signup                                     │
│  → BE: provision tenant (subdomain reservation, owner user)          │
│  → status SIGNED_UP                                                  │
└─────────────────────────────────────────────────────────────────────┘
                                  │
                                  ▼
┌─────────────────────────────────────────────────────────────────────┐
│  Bước 5: Owner First Login                                           │
│  Owner login với email + password vừa set                            │
│  → JWT scoped tenantId → redirect /dashboard                         │
│  → Onboarding checklist visible (Phase 1 BETA scope)                 │
└─────────────────────────────────────────────────────────────────────┘
```

---

## 3. Bước 1 — Public Request (chưa xác thực)

### 3.1 Actor + Trigger

- **Actor:** Visitor (P1 solo teacher hoặc P2 center owner) — chưa đăng nhập
- **Trigger:** Visitor vào landing page `https://kitehub.me/` → click CTA "Yêu cầu truy cập Beta"
- **Endpoint frontend:** `/request-beta-access` (route trong `(public)` group)

### 3.2 Action

1. Visitor điền form: `email`, `name`, `orgName`, `persona` (P1/P2/P3...), `referralSource` (optional)
2. Tick PDPL consent checkbox (`consentGiven=true`) — Submit button disabled cho tới khi tick
3. FE submit JSON tới `POST /api/v1/auth/request-beta-access` với `honeypot=""`
4. BE validate (`@Valid BetaRequestDto`):
   - `consentGiven` phải true → nếu false trả `400 BETA_CONSENT_REQUIRED`
   - `email` format → `400 BETA_INVALID_EMAIL`
   - `persona` trong enum → `400 BETA_INVALID_PERSONA`
   - `honeypot` rỗng → nếu non-empty trả `400 BETA_HONEYPOT_FILLED` (silent — bot trap)
5. BE check duplicate email (BR-BETA-002): nếu đã có row PENDING/APPROVED còn hiệu lực → `409 BETA_DUPLICATE_EMAIL`
6. BE persist `BetaAccessRequest` với `status=PENDING`, `consent_given=true`, `consent_at=now()`
7. BE emit `beta.consent.given` event qua per-module outbox emitter
8. BE return `201 Created` + `BetaRequestResponse` body
9. FE hiển thị success screen: "Đã nhận yêu cầu — coordinator sẽ review trong vòng X ngày"

### 3.3 Verify (smoke test)

```bash
# Test endpoint bằng curl
curl -X POST https://kitehub.me/api/v1/auth/request-beta-access \
  -H "Content-Type: application/json" \
  -d '{
    "email": "test@example.com",
    "name": "Nguyễn Văn A",
    "orgName": "Trung tâm Anh ngữ ABC",
    "persona": "P2_CENTER_OWNER",
    "referralSource": "google",
    "honeypot": "",
    "consentGiven": true
  }'
# Kỳ vọng: HTTP 201 + body chứa `id`, `email`, `status=PENDING`

# Verify DB row tồn tại
docker exec kite-postgres psql -U kite -d kitehub -c \
  "SELECT id, email, status, consent_given, created_at FROM beta_access_request WHERE email='test@example.com';"
```

### 3.4 Failure modes + recovery

| Triệu chứng | Nguyên nhân khả dĩ | Cách xử lý |
|---|---|---|
| FE submit disabled hoài | `consentGiven=false` (checkbox chưa tick) | User tick checkbox |
| 429 `RATE_LIMITED` | Gateway rate-limit per IP vượt ngưỡng | Wait 15min; nếu legitimate user, whitelist IP |
| 409 `BETA_DUPLICATE_EMAIL` | Đã có request PENDING/APPROVED chưa hết hạn | Coordinator check `beta_access_request` table; reject pending hoặc trả lời user dùng email khác |
| 500 + log error "outbox emit failed" | RabbitMQ down hoặc outbox-poller chết | Restart `kitehub-subscription` service; row vẫn được persist (outbox-first) → poller tự retry |
| BE log "honeypot filled" | Bot scraping | Không cần action; row không lưu |

---

## 4. Bước 2 — Admin Review (coordinator action)

### 4.1 Actor + Trigger

- **Actor:** Beta coordinator (role `PLATFORM_ADMIN` — Backend seed value; FE compat accept cả `ADMIN` per GAP-518 Wave 78 Bucket D)
- **Trigger:** Coordinator nhận notification (email/Slack — Phase 1.5 scope) HOẶC manual check daily

### 4.2 Action — Login + Navigate

1. Coordinator vào `https://kitehub.me/login`
2. Đăng nhập với credential admin (`admin@kitehub.me` — credential lấy từ AWS Secrets Manager):
   ```bash
   aws secretsmanager get-secret-value \
     --secret-id kitehub/production/admin-bootstrap \
     --query SecretString --output text --profile dev-admin
   ```
3. FE login (`app/(auth)/login/page.tsx`) gọi `POST /api/v1/auth/login`:
   - BE return JWT với `role: "PLATFORM_ADMIN"`
   - FE `isPlatformAdmin(user.role)` returns `true` (accept cả `PLATFORM_ADMIN` lẫn legacy `ADMIN`)
   - Redirect → `/admin` (admin dashboard)
4. Navigate đến `/admin/beta-requests` (link có trong Sidebar variant="admin"):
   - `AdminLayout.tsx` guard re-check `isPlatformAdmin(user.role)` → render content
   - Trang gọi `GET /api/v1/admin/beta-requests?status=PENDING&page=0&size=20`
   - Hiển thị list: email, name, orgName, persona, createdAt, source

### 4.3 Verify (admin walkthrough)

Thực hiện checklist `pre-handoff-self-test-completeness.md` §2.4:

| Check | Cách verify | Pass criterion |
|---|---|---|
| (a) Role match BE seed `PLATFORM_ADMIN` vs FE guard | `grep "PLATFORM_ADMIN" kitehub-subscription/src/main/java/.../ProductionSeedRunner.java` + `grep "isPlatformAdmin" kitehub-frontend/src/lib/auth-helpers.ts` | BE seeds `role("PLATFORM_ADMIN")`; FE `isPlatformAdmin` accepts cả `PLATFORM_ADMIN` lẫn `ADMIN` |
| (b) Admin sees admin dashboard | Browser test login → redirect `/admin` | Post-login URL = `/admin`, không phải `/dashboard` |
| (c) Admin can navigate /admin/beta-requests | Sidebar có link "Beta Requests" trong admin variant | Click vào link OR gõ URL trực tiếp đều load page (không bị bounce sang `/login`) |
| (d) Approve action POST tới subscription | DevTools Network tab khi click "Approve" | POST `/api/v1/admin/beta-requests/{id}/approve` → 200 |

### 4.4 Failure modes + recovery

| Triệu chứng | Nguyên nhân khả dĩ | Cách xử lý |
|---|---|---|
| Login OK nhưng redirect `/dashboard` chứ không phải `/admin` | `isPlatformAdmin(user.role)` return `false` → role không match | Check JWT payload (DevTools → Application → localStorage `accessToken` → decode); nếu role không phải `PLATFORM_ADMIN`/`ADMIN` → BE seed lỗi, xem `ProductionSeedRunner` |
| /admin/beta-requests redirect /login | AdminLayout guard fail (chưa hydrate / token hết hạn / role không match) | Clear localStorage + re-login; check refresh-token rotation |
| 403 từ BE khi click Approve | `@PreAuthorize("hasRole('PLATFORM_ADMIN')")` từ chối | Verify gateway forward `X-User-Roles: PLATFORM_ADMIN` header; check `SecurityConfig.XUserRolesHeaderFilter` |
| List empty dù có row PENDING trong DB | Query filter sai status hoặc page out-of-range | Check Network response; SQL trực tiếp: `SELECT * FROM beta_access_request WHERE status='PENDING' ORDER BY created_at DESC` |

---

## 5. Bước 3 — Approve → Email Invite

### 5.1 Actor + Trigger

- **Actor:** Beta coordinator (đã ở `/admin/beta-requests`)
- **Trigger:** Coordinator click button "Approve" trên 1 row PENDING

### 5.2 Action

1. FE gọi `POST /api/v1/admin/beta-requests/{id}/approve` với body `BetaApproveCommand` (có thể có note)
2. BE `BetaAccessService.approveRequest()`:
   - Validate state hiện tại là PENDING (nếu khác → 409)
   - Generate `invite_token` (UUID v4) + `claim_code` (6-digit random — GAP-388 388-B 2FA)
   - Set `status=APPROVED`, `invite_token_expires_at = now() + 24h`
   - Persist row
   - Emit `beta.invite.sent` event qua outbox với payload `{email, name, orgName, persona, invite_token, claim_code, expires_at}`
3. Outbox poller pick event → publish RabbitMQ routing key `beta.invite.sent`
4. `kitehub-email` consumer pick → render template `beta-invite.html`:
   - Subject: "Bạn được mời tham gia Beta KiteHub"
   - Body chứa: tên người được mời, organization, 6-digit claim code, link `https://kitehub.me/beta-signup?claim=<code>` (link bypass code, FE auto exchange)
   - CTA: "Hoàn tất đăng ký"
   - Footer: thông tin liên hệ + unsubscribe (chưa active Phase 1)
5. Resend/AWS SES gửi email → tracking event `delivered`

### 5.3 Verify

```bash
# 1. Verify row APPROVED + token issued
docker exec kite-postgres psql -U kite -d kitehub -c \
  "SELECT id, email, status, invite_token, claim_code, invite_token_expires_at
   FROM beta_access_request WHERE email='test@example.com';"
# Kỳ vọng: status=APPROVED, invite_token + claim_code không null, expires_at = +24h

# 2. Verify outbox event được emit
docker exec kite-postgres psql -U kite -d kitehub -c \
  "SELECT routing_key, payload, status FROM outbox_event
   WHERE routing_key='beta.invite.sent' ORDER BY created_at DESC LIMIT 1;"
# Kỳ vọng: status=PUBLISHED (sau khi poller chạy)

# 3. Verify email render trong Resend dashboard
# https://resend.com/emails → tìm theo recipient email
# Hoặc check kitehub-email logs:
docker logs kitehub-email | grep "beta-invite"
```

### 5.4 Failure modes + recovery

| Triệu chứng | Nguyên nhân khả dĩ | Cách xử lý |
|---|---|---|
| Click Approve → 409 Conflict | Row đã APPROVED hoặc REJECTED rồi | Refresh list; nếu cần re-invite, dùng "Resend Invite" (Phase 1.5 scope) hoặc reject + ask user submit lại |
| Email không tới inbox | DNS/DKIM/SPF chưa setup (xem `documents/05-guides/deploy/dns-dkim-runbook.md`) | Verify DNS records; check Resend/SES dashboard `bounced/spam/blocked` reason |
| Email tới spam folder | DKIM/DMARC chưa pass | Run `dig TXT _dmarc.kitehub.me` + Resend dashboard SPF/DKIM verification |
| Outbox event stuck `PENDING` | Outbox poller down hoặc RabbitMQ unreachable | Restart `kitehub-subscription`; check `docker logs kitehub-subscription \| grep OutboxPublisher` |
| Token expired trước khi user click | TTL 24h, user trễ | Coordinator re-approve hoặc gọi `POST /admin/beta-requests/{id}/resend-invite` (Phase 1.5 scope) |

---

## 6. Bước 4 — Invitee Signup

### 6.1 Actor + Trigger

- **Actor:** Invitee (chưa có account — sẽ thành owner của tenant mới)
- **Trigger:** Invitee click link trong email `https://kitehub.me/beta-signup?claim=<6-digit>`

### 6.2 Action

1. FE trang `/beta-signup` (route trong `(auth)` group) đọc `claim` query param
2. FE gọi `POST /api/v1/auth/beta-signup/exchange-claim-code` với `{claimCode: "123456"}`:
   - BE lookup row có matching `claim_code` + `status=APPROVED` + chưa expire
   - Return `{invite_token, email, name, persona}` để pre-fill form
   - Nếu không match → 404, FE hiển thị "Link không hợp lệ hoặc đã hết hạn"
3. FE pre-fill form (email + name read-only), invitee điền thêm:
   - `subdomain` (sẽ thành `<subdomain>.kitehub.me`) — FE validate slug VN-friendly per GAP-535
   - `ownerPassword` + confirm password
4. Invitee submit → `POST /api/v1/auth/beta-signup` với `{inviteToken, subdomain, ownerPassword}`:
   - BE `BetaAccessService.completeBetaSignup()`: flip `status=SIGNED_UP`, mark token redeemed
   - BE `AuthService.registerFromBetaInvite()`: provision tenant via standard registration pipeline
     - Reserve subdomain (409 nếu trùng → rollback row sang APPROVED với token mới)
     - Create owner user với email + bcrypt-hashed password + role `OWNER`
     - Initialize tenant (default branding, default theme, default settings)
   - On success: return `200 OK` với `BetaRequestResponse`
   - On failure: rollback (row về APPROVED với fresh token) + trả 409/500

### 6.3 Verify

```bash
# 1. Verify claim code exchange
curl -X POST https://kitehub.me/api/v1/auth/beta-signup/exchange-claim-code \
  -H "Content-Type: application/json" \
  -d '{"claimCode":"123456"}'
# Kỳ vọng: 200 + body có invite_token + email + persona

# 2. Verify signup completes
curl -X POST https://kitehub.me/api/v1/auth/beta-signup \
  -H "Content-Type: application/json" \
  -d '{
    "inviteToken": "<uuid>",
    "subdomain": "abc-school",
    "ownerPassword": "SecureP@ss123"
  }'
# Kỳ vọng: 200 + body có status=SIGNED_UP

# 3. Verify tenant + owner row tồn tại
docker exec kite-postgres psql -U kite -d kitehub -c \
  "SELECT t.id, t.subdomain, t.org_name, u.email, u.role
   FROM tenant t JOIN \"user\" u ON u.tenant_id = t.id
   WHERE t.subdomain='abc-school';"
# Kỳ vọng: 1 row, u.role='OWNER'
```

### 6.4 Failure modes + recovery

| Triệu chứng | Nguyên nhân khả dĩ | Cách xử lý |
|---|---|---|
| 404 trên exchange-claim-code | Token expire / code không tồn tại / typo | Verify expires_at; coordinator re-approve |
| 409 trên signup `subdomain conflict` | Subdomain đã được reserve | User chọn subdomain khác; row tự rollback về APPROVED với token mới |
| 500 trên signup | Lỗi DB / provisioning pipeline | Check `kitehub-subscription` logs; row tự rollback → user retry |
| Tenant tạo nhưng owner user thiếu | Migration race / V-script chưa apply | Run `flyway info` trên DB; nếu thiếu owner role, manually insert hoặc rollback toàn bộ tenant |
| Subdomain hợp lệ ở FE nhưng BE reject | Slug normalize VN không khớp (GAP-535) | Verify cả 2 layer dùng cùng `normalizeSlug()` |

---

## 7. Bước 5 — Owner First Login

### 7.1 Actor + Trigger

- **Actor:** Tenant owner (vừa hoàn tất signup)
- **Trigger:** Sau signup thành công, FE redirect tới `/login` hoặc auto-issue JWT (tuỳ thiết kế Phase 1)

### 7.2 Action

1. Owner vào `/login` (route trong `(auth)` group)
2. Submit email + password vừa set
3. FE gọi `POST /api/v1/auth/login`:
   - BE verify bcrypt password
   - Issue JWT với `tenantId`, `role: OWNER`
4. FE `useAuthStore.setAuth(user, token)` + `isPlatformAdmin(user.role)` return `false`
5. Redirect `/dashboard` (tenant-scoped customer layout)
6. Dashboard hiển thị:
   - Header: org name + owner email
   - Onboarding checklist (Phase 1 BETA scope — chi tiết tuỳ thiết kế):
     - Create first user (teacher)
     - Set branding (logo + color — link sang AI Branding)
     - Configure subscription tier
     - Invite first students

### 7.3 Verify

```bash
# 1. Verify login + JWT
curl -X POST https://kitehub.me/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"test@example.com","password":"SecureP@ss123"}'
# Kỳ vọng: 200 + body có accessToken + user.role='OWNER' + user.tenantId

# 2. Verify dashboard render (browser test)
# Open https://kitehub.me/dashboard sau khi login
# Kỳ vọng: page load không spinner-forever, không 403, không crash
```

### 7.4 Failure modes + recovery

| Triệu chứng | Nguyên nhân khả dĩ | Cách xử lý |
|---|---|---|
| Login 401 dù password đúng | bcrypt cost mismatch / hashed lưu sai cột | Verify `user.password_hash`; reset password qua admin path (Phase 1.5 scope — manual SQL update tạm thời) |
| Login OK nhưng redirect `/admin` | Role mismatch (user.role = `PLATFORM_ADMIN` thay vì `OWNER`) | Verify `ProductionSeedRunner` chỉ seed admin user; new owner phải có role OWNER. Manual SQL fix nếu cần |
| Dashboard 500 / blank | Tenant data chưa init đầy đủ (missing branding row, settings row) | Check `branding` + `tenant_settings` tables; run seed cho tenant đó |
| Onboarding checklist không hiển thị | Phase 1 BETA chưa ship checklist component | Acceptable Phase 1 — track follow-up gap |

---

## 8. Quick reference — endpoints + roles

| Endpoint | Method | Auth | Role |
|---|---|---|---|
| `/api/v1/auth/request-beta-access` | POST | Public | — |
| `/api/v1/auth/beta-signup/exchange-claim-code` | POST | Public | — |
| `/api/v1/auth/beta-signup/validate?token=...` | GET | Public | — |
| `/api/v1/auth/beta-signup` | POST | Public | — |
| `/api/v1/admin/beta-requests` | GET | Bearer | `PLATFORM_ADMIN` |
| `/api/v1/admin/beta-requests/{id}/approve` | POST | Bearer | `PLATFORM_ADMIN` |
| `/api/v1/admin/beta-requests/{id}/reject` | POST | Bearer | `PLATFORM_ADMIN` |
| `/api/v1/auth/login` | POST | Public | — (returns JWT) |

---

## 9. Pre-launch smoke test checklist

Trước mỗi pre-release tag (`v0.9.0-beta-staging.N`) hoặc sau hotfix luồng Beta, chạy luồng full end-to-end:

- [ ] **Bước 1** — POST request từ landing page → DB row PENDING + audit event emit
- [ ] **Bước 2** — Login `admin@kitehub.me` → redirect `/admin` (không `/dashboard`)
- [ ] **Bước 2** — Navigate `/admin/beta-requests` → list PENDING render
- [ ] **Bước 3** — Click Approve → status APPROVED + token issued + outbox event PUBLISHED
- [ ] **Bước 3** — Email tới inbox (kiểm tra Resend dashboard `delivered`)
- [ ] **Bước 4** — Click link email → FE exchange claim code → form pre-fill
- [ ] **Bước 4** — Submit signup → tenant + owner user provision thành công
- [ ] **Bước 5** — Owner login → redirect `/dashboard` (không `/admin`)
- [ ] **Bước 5** — Dashboard render data (không spinner-forever / 500)

Cross-reference: [`pre-handoff-self-test-completeness.md`](../../../.claude/rules/pre-handoff-self-test-completeness.md) §2.3 (email-driven) + §2.4 (admin).

---

## 10. Related runbooks

- [`stack-on-demand-runbook.md`](./stack-on-demand-runbook.md) — start/stop AWS stack trước khi smoke test
- [`incident-response-runbook.md`](./incident-response-runbook.md) — khi luồng Beta down
- [`credential-rotation-runbook.md`](./credential-rotation-runbook.md) — rotate admin credential
- [`../deploy/dns-dkim-runbook.md`](../deploy/dns-dkim-runbook.md) — DNS + DKIM/DMARC cho email delivery

---

## 11. Change log

- **2026-05-14** — Initial runbook (Wave 78 Bucket D, closes GAP-480). Covers 5 bước luồng end-to-end + smoke test checklist.
