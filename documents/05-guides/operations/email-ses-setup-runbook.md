# AWS SES Production Setup Runbook

**Audience:** Solo dev / SRE provisioning AWS SES for KiteHub email transactional traffic
**Status:** Active runbook (Wave 33 Bucket B — GAP-370)
**Last reviewed:** 2026-05-07
**Cross-refs:**
- `documents/04-quality/gaps/GAP-370-email-transactional-infrastructure.md`
- `documents/05-guides/operations/dns-setup-runbook.md` (Bucket D — TXT records)
- `kitehub/kitehub-email/src/main/resources/application.yml` (`aws.ses.*` keys)
- `.claude/rules/release-deploy-standard.md` §3.4 (MAJOR release email checklist)

---

## 1. Overview

KiteHub email transactional pipeline sử dụng AWS SES vì cost-effective + tích hợp sẵn AWS infra. Bài runbook này cover toàn bộ steps từ sandbox → production:

1. Verify sender domain (DKIM + SPF + DMARC)
2. Sandbox → production access request
3. Configure bounce + complaint feedback loops
4. Configure sending limits + warmup schedule
5. Wire app-side config (`application.yml`)
6. Smoke test

**Region:** `ap-southeast-1` (Singapore — closest to VN, lowest latency)
**Sender:** `noreply@kitehub.vn` (production) / `noreply@localhost` (dev)

---

## 2. Prerequisites

- [ ] AWS account active với billing enabled
- [ ] IAM user/role có permissions: `ses:*`, `sns:*` (or scoped: `ses:SendEmail`, `ses:SendRawEmail`, `ses:GetIdentityVerificationAttributes`, `sns:CreateTopic`, `sns:Subscribe`)
- [ ] Domain `kitehub.vn` đã được mua + DNS managed (per `dns-setup-runbook.md`)
- [ ] Terraform state cho `terraform-aws/` initialized

---

## 3. Sender domain verification

### 3.1 Initiate verification (AWS console / CLI)

**AWS Console:**
SES → **Verified identities** → **Create identity** → **Domain** → enter `kitehub.vn` → enable **DKIM** (Easy DKIM).

**CLI equivalent:**
```bash
aws ses verify-domain-identity \
  --region ap-southeast-1 \
  --domain kitehub.vn

aws ses verify-domain-dkim \
  --region ap-southeast-1 \
  --domain kitehub.vn
```

CLI returns 3 DKIM tokens (CNAME records) — needed for §3.2.

### 3.2 Add TXT + CNAME records to DNS

Add these DNS records (per `dns-setup-runbook.md` Bucket D will own actual record creation; here we list the values):

| Type | Name | Value | Purpose |
|------|------|-------|---------|
| TXT | `_amazonses.kitehub.vn` | `<verification-token-from-ses>` | Domain ownership |
| CNAME | `<token1>._domainkey.kitehub.vn` | `<token1>.dkim.amazonses.com` | DKIM key 1 |
| CNAME | `<token2>._domainkey.kitehub.vn` | `<token2>.dkim.amazonses.com` | DKIM key 2 |
| CNAME | `<token3>._domainkey.kitehub.vn` | `<token3>.dkim.amazonses.com` | DKIM key 3 |
| TXT | `kitehub.vn` | `v=spf1 include:amazonses.com -all` | SPF |
| TXT | `_dmarc.kitehub.vn` | `v=DMARC1; p=quarantine; rua=mailto:dmarc-reports@kitehub.vn; pct=100; sp=quarantine; aspf=s; adkim=s` | DMARC |

**Verification time:** SES auto-checks DNS every ~15min. Status `Pending` → `Verified` once records propagate (usually <30min).

```bash
# Check verification status
aws ses get-identity-verification-attributes \
  --region ap-southeast-1 \
  --identities kitehub.vn
```

Expected: `"VerificationStatus": "Success"`.

---

## 4. Sandbox → Production access request

By default, SES accounts are in **sandbox mode**:
- Can only send **to verified addresses** (recipients must opt-in)
- Cap: **200 emails/day**, **1 email/second**

For production (send to anyone, higher limits), you must request out-of-sandbox.

### 4.1 Submit request

**AWS Console:** SES → **Account dashboard** → **Request production access**

Fill out form:
- **Use case:** Transactional (signup verification, beta invites, password reset, account notifications)
- **Website URL:** `https://kitehub.vn`
- **How do you build/maintain mailing lists:** "Users explicitly sign up; we do not send marketing without opt-in. Beta invite list is curated from `BetaAccessRequest` table — only users who submitted the request form."
- **How do you handle bounces/complaints:** "SNS topics `ses-bounces` + `ses-complaints` subscribed; bounced addresses auto-suppressed in `email_suppression_list` table. Hard bounces blocked permanently; soft bounces retry max 3× then suppress."
- **How do you handle unsubscribe:** "All transactional emails include unsubscribe link in footer for marketing-class messages. Critical transactional (verification, password reset) cannot be unsubscribed per industry standard."
- **Expected daily volume:** Phase 1 BETA: ~500/day; Phase 2: ~5,000/day; Phase 3: ~50,000/day
- **Expected bounce rate:** <2% (industry threshold; SES suspends if >5%)
- **Expected complaint rate:** <0.1% (SES suspends if >0.3%)

**Approval time:** Typically 24-48h, occasionally up to 7 days.

### 4.2 Post-approval limits

After approval, default production tier:
- **50,000 emails/day**
- **14 emails/second**

These match `aws.ses.rate.max-per-day=50000` + `aws.ses.rate.max-per-second=14` defaults (set conservatively at 10 in code; bump after warmup).

To request higher limits later: SES → Account dashboard → **Request quota increase** với justification.

---

## 5. Bounce + complaint feedback loops

SES requires explicit bounce/complaint handling to maintain reputation.

### 5.1 Create SNS topics

```bash
# Bounces
aws sns create-topic \
  --region ap-southeast-1 \
  --name ses-bounces

# Complaints
aws sns create-topic \
  --region ap-southeast-1 \
  --name ses-complaints
```

Output: 2 ARNs like `arn:aws:sns:ap-southeast-1:123456789012:ses-bounces`.

### 5.2 Configure SES → SNS notifications

**Console:** SES → Verified identities → `kitehub.vn` → **Notifications** tab → set:
- **Bounces:** select `ses-bounces` topic
- **Complaints:** select `ses-complaints` topic
- Enable **Include original headers**

**CLI:**
```bash
aws ses set-identity-notification-topic \
  --region ap-southeast-1 \
  --identity kitehub.vn \
  --notification-type Bounce \
  --sns-topic arn:aws:sns:ap-southeast-1:123456789012:ses-bounces

aws ses set-identity-notification-topic \
  --region ap-southeast-1 \
  --identity kitehub.vn \
  --notification-type Complaint \
  --sns-topic arn:aws:sns:ap-southeast-1:123456789012:ses-complaints
```

### 5.3 Subscribe app endpoint to SNS topics

Two options:
1. **HTTPS endpoint** (recommended for prod): `https://api.kitehub.vn/internal/ses/bounce` — kitehub-email service exposes the handler. SNS auto-confirms subscription on first POST.
2. **SQS queue** (simpler for solo-dev): subscribe SQS queue, kitehub-email polls.

For Wave 33 BETA: **SQS queue** is faster to set up. Track HTTPS endpoint as follow-up gap when scale demands.

### 5.4 Wire app config

After SNS topic ARNs are ready, set environment variables:

```bash
export AWS_SES_BOUNCE_TOPIC_ARN="arn:aws:sns:ap-southeast-1:123456789012:ses-bounces"
export AWS_SES_COMPLAINT_TOPIC_ARN="arn:aws:sns:ap-southeast-1:123456789012:ses-complaints"
```

Or in production Helm values / `.env.production` per `secrets-management-runbook.md` (Bucket D).

---

## 6. Sending limits + warmup schedule

Even after production approval, **deliverability requires warmup**. New IPs/domains start with low reputation; sending too much too fast triggers spam filtering.

### 6.1 Warmup schedule (recommended)

| Day | Max emails/day | Notes |
|-----|----------------|-------|
| 1 | 50 | Internal testing only — verified addresses |
| 2 | 100 | First beta cohort (small) |
| 3 | 200 | Expand to ~50 beta tenants |
| 4 | 500 | Continue beta — monitor bounce rate |
| 5 | 1,000 | Add support emails |
| 7 | 2,000 | All beta tenants active |
| 14 | 10,000 | Phase 2 ramp |
| 30 | 50,000 | Production tier reached |

Set `aws.ses.rate.max-per-day` to match the current day target. App-side limiter rejects emails over the cap (per `feedback_dependabot_first_run.md` defensive pattern).

### 6.2 Rate limit enforcement

```yaml
# application-production.yml
aws:
  ses:
    rate:
      max-per-second: 10   # Day 1: stay well below 14/s SES cap
      max-per-day: 500     # Day 4 target
```

App enforces both caps via Resilience4j RateLimiter (configured separately — TODO follow-up if not yet wired).

### 6.3 Monitoring

CloudWatch metrics to watch:
- `Send` — total successful sends
- `Bounce` — bounced emails (target: <2%)
- `Complaint` — spam complaints (target: <0.1%)
- `Reputation.BounceRate` (custom)
- `Reputation.ComplaintRate` (custom)

Alert thresholds (per `monitoring-runbook.md` future scope):
- Bounce rate >3% → WARN
- Bounce rate >5% → SES auto-suspend; PAGE on-call
- Complaint rate >0.2% → WARN
- Complaint rate >0.3% → SES auto-suspend; PAGE on-call

---

## 7. Smoke test (post-setup verification)

```bash
# Set production credentials
export AWS_SES_MOCK_MODE=false
export EMAIL_PROVIDER=ses
export AWS_SES_REGION=ap-southeast-1
export AWS_SES_FROM_EMAIL=noreply@kitehub.vn
export AWS_SES_FROM_NAME="KiteHub"
export AWS_ACCESS_KEY_ID=<...>
export AWS_SECRET_ACCESS_KEY=<...>

# Run smoke test (kitehub-email exposes /internal/test endpoint in dev/staging only)
curl -X POST https://api.kitehub.vn/internal/test/email \
  -H "Content-Type: application/json" \
  -d '{
    "to": "your-personal@gmail.com",
    "subject": "[SMOKE TEST] KiteHub SES production",
    "templateName": "beta-invite",
    "variables": {
      "orgName": "Test Org",
      "inviteToken": "smoke-test-token-1234",
      "inviteUrl": "https://kitehub.vn/auth/beta-signup?token=smoke-test-token-1234",
      "expiryDate": "2026-12-31"
    }
  }'
```

Verify:
- [ ] Email arrives in inbox (not spam folder) within 1min
- [ ] DKIM signature valid (check raw headers for `Authentication-Results: dkim=pass`)
- [ ] SPF passes (`spf=pass`)
- [ ] DMARC passes (`dmarc=pass`)
- [ ] Branding/styling renders correctly across Gmail / Outlook / mobile

---

## 8. Rollback / break-glass

If SES is suspended due to bounce/complaint spike:

1. **Stop all sends immediately:** Set `EMAIL_PROVIDER=mock` in production config + redeploy. Emails are logged but not sent.
2. **Investigate cause:** Pull last 24h sends from SES → identify spike source (test data leak? mailing list error? template misconfig?).
3. **Clean suppression list:** Mark recently-bounced addresses as suppressed; they will not be re-attempted.
4. **Submit reinstatement request:** SES Console → Reputation dashboard → **Request reinstatement** với root cause + remediation plan.
5. **Re-warm IP:** After reinstatement, restart at Day 1 of warmup schedule.

---

## 9. Open items / follow-ups

- [ ] App-side rate limiter wiring — `RateLimiter` bean using `aws.ses.rate.*` props (track in follow-up gap)
- [ ] HTTPS bounce/complaint webhook endpoint — replace SQS polling once scale demands
- [ ] DMARC report aggregation — set up `dmarc-reports@kitehub.vn` mailbox + parsing pipeline
- [ ] Suppression list table + auto-clean job (90d retention per PDPL)
- [ ] CloudWatch dashboard + alerts wired to PagerDuty/ntfy

---

## 10. Standards reference

Per `.claude/rules/release-deploy-standard.md` §2:
- **AWS Well-Architected** — Operational Excellence (warmup), Security (SPF/DKIM/DMARC), Reliability (bounce/complaint feedback)
- **OWASP Top 10** — A05:2021 Security Misconfiguration (proper email auth records prevent spoofing)
- **PDPL 2023** — Art 23 retention (suppression list 90d cap)
- **GAP-370** — closes BETA blocker for transactional email

---

## 11. Log

- **2026-05-07** (Wave 33 Bucket B): Runbook created. SES sandbox→production approval steps + DKIM/SPF/DMARC TXT values + bounce/complaint SNS subscription + warmup schedule (Day 1: 50/day → Day 14: 10K/day → Day 30: 50K/day). Paired same-PR với `beta-invite.html` + `beta-request-confirmation.html` templates + `EmailType` enum + SES `bounce/complaint/rate` config properties.
