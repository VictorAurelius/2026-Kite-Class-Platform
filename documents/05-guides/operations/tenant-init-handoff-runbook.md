# Tenant Init Handoff Runbook — admin approve → tenant ready

**Audience:** Platform admin + dev on-call + tenant Owner support
**Created:** 2026-05-14 (Wave 78 Bucket E, closes GAP-531)
**Trigger:** Mỗi lần admin approve một beta-access request mới qua `/admin/beta-requests`.
**Status:** Phase 1 BETA — `[v1 pending counsel review]`

---

## Mục đích

Runbook end-to-end cho flow tenant init handoff: từ thời điểm admin click "Approve" → đến khi tenant Owner đăng nhập được vào dashboard. Cover cả happy path + 4 failure modes thường gặp.

Per `pre-handoff-self-test-completeness.md` §2.1 (auth-gated user-flow) + §2.4 (admin-flow), runbook walks step-by-step với verification command tại mỗi gate.

---

## Pre-conditions (verify trước khi approve)

| Check | Command | Pass criterion |
|-------|---------|----------------|
| kitehub-email service healthy | `bash scripts/smoke-email-actuator.sh` | exit 0 + status=UP |
| Resend API key present | `gh secret list -e production \| grep RESEND_API_KEY` | row exists, length ≥ 30 |
| RDS reachable | `aws rds describe-db-instances --db-instance-identifier kite-postgres-prod --query 'DBInstances[0].DBInstanceStatus' --output text` | `available` |
| Admin role guard active | Login `admin@kitehub.me` → expect `/admin` URL (not redirect to `/dashboard`) | URL = `/admin` |

Nếu bất kỳ check fail → **STOP**, đừng approve. File incident gap.

---

## Happy path (6 bước)

### Bước 1 — Admin click "Approve" trên `/admin/beta-requests`

- **Actor:** Platform admin (`PLATFORM_ADMIN` role).
- **UI surface:** `/admin/beta-requests` (per GAP-526 verified).
- **Action:** Click "Approve" button bên cạnh row pending request.
- **Backend:** POST `/api/v1/admin/beta-requests/{id}/approve` → kitehub-subscription.

**Verify:**
```bash
# DB row state changed
docker exec kite-postgres psql -U kite -d kitehub -c \
  "SELECT id, status, approved_at FROM beta_access_request WHERE id = '<request-id>';"
# Expected: status = 'APPROVED', approved_at = <now>
```

**Failure mode:** approve button không hiển thị → role guard mismatch (BE `PLATFORM_ADMIN` vs FE `'ADMIN'` — GAP-518 fix). Check JWT claim `role` value.

---

### Bước 2 — Backend triggers tenant provision

- **Actor:** System (kitehub-subscription → kiteclass-core).
- **Backend:** Flyway migrations apply (V35+); default tenant config seed; branding stub.
- **Event:** `tenant.provisioned` published qua outbox → RabbitMQ.

**Verify:**
```bash
# Tenant row created
docker exec kite-postgres psql -U kite -d kitehub -c \
  "SELECT id, slug, beta_flag, created_at FROM tenant WHERE slug LIKE '<expected-slug>%';"
# Expected: 1 row, beta_flag = false (sẽ flip true sau signup), created_at = <now>

# Outbox event published
docker exec kite-postgres psql -U kite -d kitehub -c \
  "SELECT id, routing_key, published_at FROM outbox_event \
   WHERE routing_key = 'tenant.provisioned' ORDER BY created_at DESC LIMIT 5;"
# Expected: latest row published_at IS NOT NULL
```

**Failure mode:** migration fail (V35+ syntax error) → tenant row missing → re-run migration manually + file GAP. Outbox event không publish → RabbitMQ broker reachable? `docker ps | grep kite-rabbit`.

---

### Bước 3 — Invite email gửi đi (GAP-527 verified)

- **Actor:** System (kitehub-email consumer of `tenant.provisioned`).
- **Template:** `beta-invite.html` (audit ở `documents/01-business/kitehub/email/templates/beta-invite-audit.md`).
- **Provider:** Resend HTTP API (Phase 1 BETA per ADR-025).
- **Recipient:** Email từ beta-access request form.

**Verify:**
```bash
# kitehub-email actuator + metric
bash scripts/smoke-email-actuator.sh
# Resend dashboard → Logs → Sent → confirm delivery
open "https://resend.com/emails"  # Resend web UI
```

Expected within 60s of approve:
- Resend dashboard log shows recipient + `subject="Lời mời beta - <brand>"` + status `delivered`.
- Email arrives at inbox (check `vannkite@outlook.com` testing inbox first).

**Failure mode:**
- Email không arrive 60s → check `docker logs kitehub-email --tail 100` cho Resend API errors.
- Resend rate-limit → fall back to AWS SES nếu provider config switch (`EMAIL_PROVIDER=ses`).
- Email vào spam → SPF/DKIM/DMARC verify per `email-ses-setup-runbook.md` (DNS).

---

### Bước 4 — User click token link → signup form

- **Actor:** Tenant Owner (recipient).
- **UI surface:** `/signup?token=<claim-code>` hoặc `/signup?code=<6-digit>`.
- **Validation:** Token TTL ≤ 7 ngày (per `pre-handoff-self-test-completeness.md` §2.10).

**Verify (manual):**
- Click link trong email → browser open URL.
- Form render đúng + token validated client-side.
- Submit credentials → POST `/api/v1/auth/signup-with-token`.

**Failure mode:**
- Token expired → user thấy "Lời mời đã hết hạn — liên hệ support@kitehub.me".
- Token invalid → "Mã không hợp lệ" + suggest re-request.

---

### Bước 5 — Signup complete → tenant beta-flag flip

- **Actor:** System.
- **Backend:** kiteclass-core auth flow validates token, creates user with Owner role, flips `tenant.beta_flag = true`.

**Verify:**
```bash
docker exec kite-postgres psql -U kite -d kitehub -c \
  "SELECT t.id, t.slug, t.beta_flag, u.email, u.role \
   FROM tenant t JOIN tenant_user u ON u.tenant_id = t.id \
   WHERE t.slug = '<expected-slug>';"
# Expected: beta_flag = true, user role = OWNER, email = <recipient>
```

---

### Bước 6 — Dashboard load với default state

- **Actor:** Tenant Owner (post-login).
- **UI surface:** `/dashboard` (tenant-scoped via subdomain hoặc path).
- **Expected render:** welcome banner + onboarding checklist (5 steps) + empty class list + branding default colors.

**Verify (manual + per `pre-handoff-self-test-completeness.md` §2.1):**
- (a) Credential available — ✅ user just signed up
- (b) Login flow works — ✅ token-based signup flips to auth session
- (c) Role-guard accepts OWNER — ✅ /dashboard not /admin
- (d) Navigation path — ✅ /dashboard direct after signup
- (e) Target page renders — ✅ onboarding checklist visible
- (f) Target action succeeds — ✅ user can click "Bắt đầu thiết lập"

---

## Failure modes summary

| Failure | Bước | Likelihood | Recovery |
|---------|------|------------|----------|
| Approve button không hiển thị | 1 | LOW (post GAP-518) | Verify JWT role claim = `PLATFORM_ADMIN`; clear cache |
| Migration fail (V35+) | 2 | LOW | Re-run migration manually + file GAP |
| Outbox event không publish | 2 | LOW | RabbitMQ broker reachable check; restart kitehub-email |
| Email không arrive 60s | 3 | MEDIUM | Check Resend dashboard + kitehub-email logs; provider failover |
| Email vào spam | 3 | MEDIUM | Verify SPF/DKIM/DMARC; re-send sau DNS fix |
| Token expired | 4 | LOW | Re-trigger approve flow → new token |
| Dashboard không render | 6 | LOW | Check FE console + role guard; clear localStorage |

---

## Empirical walkthrough log (Wave 78 Bucket E)

**Run:** TBD — first live walkthrough sau khi PR Wave 78 Bucket E merge.

Trước Wave 78, mỗi bước verify riêng:
- Bước 1: Wave 72 (admin role guard) + Wave 75 GAP-526 (admin UI verified)
- Bước 2: Wave 33 Bucket C (BetaAccessRequest flow)
- Bước 3: Wave 77 Bucket A (SEND foundation) + Wave 78 Bucket E (E2E smoke)
- Bước 4-5: Wave 73 (signup)
- Bước 6: Wave 4 + Wave 50+ (dashboard render)

**End-to-end walkthrough Wave 78+:** ship template Plan 1 invite send → walked through real persona → record findings vào audit artifact `documents/04-quality/audits/persona/2026-XX-XX-tenant-init-handoff-walkthrough.md`.

---

## Related

- Gap: GAP-531 (parent), GAP-526 (admin UI), GAP-527 (E2E smoke), GAP-530 (email side), GAP-518 (role mismatch fix)
- Rule: `pre-handoff-self-test-completeness.md` §2.1 + §2.4
- Adjacent runbooks: `tenant-off-boarding.md`, `secrets-rotation-runbook.md`
- Email template: `documents/01-business/kitehub/email/templates/beta-invite-audit.md`
- Smoke: `scripts/smoke-email-actuator.sh`
