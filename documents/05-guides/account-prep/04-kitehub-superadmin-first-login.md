# 04 — KiteHub Superadmin First-Login Runbook

**Audience:** Solo dev / first ops admin chạy production-data-seed → first-login dashboard production sau khi infra live.
**Standards:** OWASP ASVS V2 (auth) · `release-deploy-standard.md` §3.4 · CLAUDE.md `gap-done-discipline.md` §2 (verification artifact pointer required).
**Cross-link:** Runs AFTER `01-aws-account-creation.md` + `02-domain-registrar.md` + `03-password-manager.md` + Wave 33 `production-data-seed` (GAP-376) + Wave 33 `email-ses-setup-runbook.md` (GAP-370) + Wave 38 `cloudflare-setup.md` + Wave 33 `dns-setup-runbook.md` (GAP-369).
**Estimated time:** ~2h end-to-end (seed + email verify + first-login + MFA enroll + invite ops admin).

---

## 1. Pre-conditions

| Item | Status | Reference |
|------|--------|-----------|
| AWS account ready với IAM admin user | ✅ | `01-aws-account-creation.md` §2.5 |
| Domain `kitehub.vn` active + DNS migrated Cloudflare | ✅ | `02-domain-registrar.md` §2.4 |
| `dns-setup-runbook.md` records applied: A/AAAA → origin, MX → SES inbound (optional), TXT → SES domain identity verify | ✅ | DNS records applied |
| SES production access (out of sandbox) approved | ✅ | `email-ses-setup-runbook.md` §production-access |
| Beta domain `beta.kitehub.vn` resolves to gateway 443 | ✅ | `dig beta.kitehub.vn @8.8.8.8` returns Cloudflare orange-cloud IP |
| Production stack deployed via terraform apply (Wave 37 GAP-395..397) | ✅ | `terraform apply -auto-approve` đã chạy |
| Database migration V1..VNN applied (Flyway) | ✅ | App boot logs verify migration count |
| `kitehub` container healthy + serves `/actuator/health` 200 | ✅ | `curl https://beta.kitehub.vn/actuator/health` |
| Production seed cron NOT yet auto-fired | ✅ | `seed-production.sh` is one-shot, not cron |
| Bitwarden vault `Kite-Production` ready cho `superadmin@kitehub.vn` entry | ✅ | `03-password-manager.md` §3.4 |

⚠️ Nếu BẤT KỲ pre-condition fail → STOP, fix dependency trước. KHÔNG chạy seed nếu DB chưa migrated (sẽ leave DB nửa state).

---

## 2. Step-by-step

### 2.1 Run production seed (~5 min)

`bash kitehub/scripts/seed-production.sh` (Wave 33 GAP-376) is wrapper invoking `ProductionSeedRunner` Java component.

```bash
# SSH vào AWS EC2 instance hoặc bastion
ssh ec2-user@<bastion-ip>

# Cd into app dir
cd /opt/kitehub

# Run seed (idempotent — V27 migration uses INSERT ... ON CONFLICT DO NOTHING)
SUPERADMIN_EMAIL="superadmin@kitehub.vn" \
SUPERADMIN_NAME="Solo Dev Admin" \
bash scripts/seed-production.sh

# Expected output:
# [seed] Connecting to DB...
# [seed] Inserting superadmin row id=<uuid>...
# [seed] Generating invite token (24h expiry)...
# [seed] Publishing kitehub.email.invite event...
# [seed] OK — invite email queued. Check superadmin@kitehub.vn inbox.
```

Verify DB row:
```bash
docker exec kite-postgres psql -U kitehub -d kitehub -c \
  "SELECT id, email, role, invite_status, invite_expires_at FROM kitehub_user WHERE email='superadmin@kitehub.vn';"
# → 1 row, role='SUPERADMIN', invite_status='PENDING', expires_at = now + 24h
```

### 2.2 Verify SES email delivery (~2 min)

1. Check inbox `superadmin@kitehub.vn`. Email subject: "Chào mừng đến KiteHub — Kích hoạt tài khoản admin".
2. Email từ `noreply@kitehub.vn` (SES domain identity).
3. Body chứa link `https://beta.kitehub.vn/admin/activate?token=<jwt>` (24h expiry).

Nếu email không đến trong 5 phút:
```bash
# AWS SES dashboard → Sending statistics → check rate
# AWS CloudWatch logs → /aws/lambda/ses-* logs
# kitehub-email service logs:
docker logs kitehub-email 2>&1 | grep -i "superadmin\|invite\|bounce"
```

Nếu bounce (email không tồn tại): check `superadmin@kitehub.vn` mailbox đã setup chưa (qua Google Workspace / Zoho / Cloudflare Email Routing). Phase 1 BETA dùng Cloudflare Email Routing free tier forward `*@kitehub.vn` → user inbox.

### 2.3 Click activation link (~3 min)

1. Click `https://beta.kitehub.vn/admin/activate?token=<jwt>` từ email.
2. Browser redirects to KiteHub admin activation page.
3. Form yêu cầu:
   - **Set new password** (≥12 chars, mix upper/lower/digit/special; KHÔNG dùng password Bitwarden master)
   - **Confirm password**
   - **Personal name** (đã pre-filled từ seed `SUPERADMIN_NAME`)
4. Click "Kích hoạt tài khoản" / "Activate account".
5. Backend validates token + creates auth session.
6. Redirect to `/admin/mfa-enroll`.

⚠️ **Token single-use.** Click 2 lần → second click fails với 410 Gone. Nếu page reload mid-flow lost activation, request new invite qua DB:
```bash
# Invalidate stale + regenerate
docker exec kite-postgres psql -U kitehub -d kitehub -c \
  "UPDATE kitehub_user SET invite_token=gen_random_uuid()::text, invite_expires_at=now()+'24 hours' WHERE email='superadmin@kitehub.vn';"
# Then trigger email re-send qua admin endpoint OR re-run seed (idempotent ON CONFLICT)
```

### 2.4 MFA TOTP enroll (BẮT BUỘC, ~5 min)

⚠️ **MUST enroll MFA before access dashboard.** Hard-required (gate cannot bypass).

1. `/admin/mfa-enroll` page shows QR code.
2. Open Bitwarden → `Kite-Production / superadmin@kitehub.vn` entry → Edit → TOTP field → "+" Add → scan QR.
3. Bitwarden generates 6-digit code → copy + paste into KiteHub form.
4. Submit. Backend validates → enables MFA flag on user.
5. Page shows **10 recovery codes** → click "Tải xuống" / "Download".
6. **Save recovery codes:**
   - Bitwarden notes field (cùng entry)
   - Print giấy → két sắt offline
7. Click "Đã lưu, tiếp tục" / "Saved, continue".
8. Redirect to `/admin/dashboard`.

### 2.5 First dashboard verify (~5 min)

Verify dashboard loads + key sections accessible:

- [ ] `/admin/dashboard` — KPIs render (likely zeros vì fresh prod)
- [ ] `/admin/instances` — list shows 0 instances (no tenants yet)
- [ ] `/admin/users` — list shows 1 user (superadmin self)
- [ ] `/admin/branding` — AI Branding queue accessible (empty)
- [ ] `/admin/system` — System health page shows: DB ✅, Redis ✅, RabbitMQ ✅, MinIO ✅, SES ✅
- [ ] Top-right user menu → Logout works
- [ ] Re-login với MFA prompt fires (TOTP code from Bitwarden)
- [ ] User profile page hiển thị "MFA: Enabled, Last reviewed: 2026-05-07"

### 2.6 Invite ops-admin (recommended, ~10 min)

Solo dev mode → KHÔNG invite team Phase 1. Skip §2.6, mark backlog cho Phase 2.

Khi Phase 2 ready invite:

1. `/admin/users` → "Mời admin mới" / "Invite admin".
2. Form: email `ops-admin@kitehub.vn` (cần mailbox setup trước qua Cloudflare Email Routing OR Google Workspace).
3. Role: **OPS_ADMIN** (lesser permissions than SUPERADMIN — no billing/account close).
4. Submit → invite email gửi tới ops-admin.
5. ops-admin follows §2.3-2.5 (activation + MFA enroll).
6. Update Bitwarden vault: chia sẻ `Kite-Production` collection với ops-admin.

### 2.7 Smoke test post-first-login (~10 min)

Verify admin operations work end-to-end:

```bash
# 1. Create test instance via admin API
curl -X POST https://beta.kitehub.vn/api/v1/admin/instances \
  -H "Authorization: Bearer <session-token>" \
  -H "Content-Type: application/json" \
  -d '{"name":"smoke-test","ownerEmail":"smoke@example.com"}'
# Expect: 201 Created

# 2. Verify instance shows in /admin/instances
# 3. Trigger AI Branding regen (if quota available)
# 4. Delete test instance
curl -X DELETE https://beta.kitehub.vn/api/v1/admin/instances/<id> \
  -H "Authorization: Bearer <session-token>"
# Expect: 204 No Content
```

Run `bash scripts/smoke-test.sh https://beta.kitehub.vn https://beta.kitehub.me` (Wave 26 GAP-377) — expect 18/18 assertions pass.

---

## 3. Verification checklist

- [ ] Production seed run successfully + DB row inserted
- [ ] SES email delivered to `superadmin@kitehub.vn` within 5 min
- [ ] Activation link clicked + password set
- [ ] MFA TOTP enrolled + recovery codes saved Bitwarden + giấy
- [ ] Dashboard loads + 6 sections accessible
- [ ] Logout + re-login với MFA prompt confirmed
- [ ] Smoke test 18/18 assertions pass
- [ ] Bitwarden `Kite-Production / superadmin@kitehub.vn` entry has password + TOTP + recovery codes
- [ ] Calendar reminder: rotate password 180 ngày + re-review MFA 365 ngày

---

## 4. What can go wrong

| Symptom | Root cause | Fix |
|---------|-----------|-----|
| `seed-production.sh` exits với "Connection refused" | DB chưa healthy hoặc `JDBC_URL` env wrong | Check `docker ps` + `docker logs kite-postgres`; verify `.env` `SPRING_DATASOURCE_URL` |
| Seed run OK nhưng email không đến >10 min | SES bounce hoặc rate limit | Check SES dashboard "Suppressed destinations"; check CloudWatch logs `kitehub-email` |
| Email "Activation link expired" >24h | User missed window | Re-run seed (idempotent) → new invite email |
| MFA QR scan fail | Camera/screen too small | Click "Manual entry" → copy secret string → paste vào Bitwarden |
| MFA code reject sau enroll | Time skew between server & device | Sync time NTP cả 2; retry với new code |
| Recovery code lost mid-enroll | Page reload before save | Reset MFA via DB: `UPDATE kitehub_user SET mfa_secret=NULL, mfa_enabled=false WHERE email='superadmin@...'` → re-enroll |
| Dashboard 500 error post-login | Missing Redis session OR DB schema mismatch | Check application logs; verify Flyway migration count = expected |
| Smoke test fail "Beta domain not resolving" | Cloudflare DNS not propagated | Wait + retry với `dig beta.kitehub.vn @1.1.1.1` |
| `INVITE_LOCKED` returned từ activation | Multiple concurrent click attempts | Wait 5 min lock release; OR DB reset invite_status='PENDING' |
| User locked out post-MFA recovery codes lost | All factors lost | DB reset MFA + regenerate invite token (last resort solo-dev escape hatch) |

---

## 5. Recovery — superadmin lost MFA + recovery codes

⚠️ **Solo-dev escape hatch only.** Phase 2 với multiple admins, ops-admin có thể reset MFA cho peer thay vì DB direct.

```bash
# DANGER: bypass auth controls. Run only nếu BẠN là solo dev và lost access.
docker exec kite-postgres psql -U kitehub -d kitehub <<EOF
UPDATE kitehub_user
SET mfa_enabled=false,
    mfa_secret=NULL,
    invite_token=gen_random_uuid()::text,
    invite_expires_at=now() + interval '1 hour',
    invite_status='PENDING'
WHERE email='superadmin@kitehub.vn';
EOF
# Re-trigger invite email manually OR run seed (idempotent)
```

Audit log entry: insert vào `audit_log` với reason "MFA reset by DB direct query — solo-dev recovery".

---

## 6. Out-of-scope (Phase 2+)

- WebAuthn / Passkey support (currently TOTP only)
- Hardware security key (YubiKey FIDO2) — optional, mentioned `03-password-manager.md` §6
- SSO (SAML / OIDC) cho admin team — Phase 2-3
- Per-admin audit log review UI — Phase 2 (currently raw DB query)
- IP allowlist cho admin endpoints — Phase 2 (Cloudflare WAF cấu hình)
- Session revocation by admin — Phase 2

---

## 7. Cross-link

- `01-aws-account-creation.md` — AWS pre-conditions
- `02-domain-registrar.md` — domain pre-conditions
- `03-password-manager.md` — vault entry destination
- `documents/05-guides/deploy/email-ses-setup-runbook.md` — SES production access
- `documents/05-guides/deploy/dns-setup-runbook.md` — DNS records pre-conditions
- `documents/05-guides/vietnamese/cloudflare-setup.md` — Cloudflare Email Routing setup `superadmin@kitehub.vn`
- Wave 33 GAP-376 `production-data-seed` — seed script implementation
- Wave 26 GAP-377 `smoke-test.sh` — post-deploy verify
- `gap-done-discipline.md` §2 criterion 5 — verification artifact pointer (smoke test 18/18 pass = artifact)

---

## 8. Log

- **2026-05-07** — Runbook created. Phase 1 GAP-394 sub-runbook 4/4 (last). Covers end-to-end: seed → SES email → activation → MFA enroll → dashboard verify → smoke test. Solo-dev escape hatch §5 documented (DB direct MFA reset). Out-of-scope: WebAuthn / SSO / IP allowlist / SAML → Phase 2-3.
