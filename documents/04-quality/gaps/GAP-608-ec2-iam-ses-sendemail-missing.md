# GAP-608 — EC2 IAM role `kitehub-production-ec2-app` thiếu `ses:SendEmail` permission

**Status:** 🔵 OPEN
**Priority:** 🔴 P0
**Domain:** DevOps
**Found:** 2026-05-17 (Wave 90 walkthrough — direct POST to kitehub-email failed AWS SES 403)
**Affects:** Mọi email gửi qua SESEmailService — beta invite, welcome, admin alerts, trial reminders, DSAR, password reset, EVERYTHING

## Problem

`SESEmailService` calls AWS SES SDK với EC2 instance profile credentials. IAM policy `kitehub-ec2-secrets-s3` attached to role `kitehub-production-ec2-app` chỉ có `secretsmanager:GetSecretValue` + `s3:GetObject/PutObject`. **Không có statement nào cho `ses:SendEmail` hoặc `ses:SendRawEmail`**.

AWS evidence (Wave 90 verify):
```
errorMessage: "User `arn:aws:sts::906286017800:assumed-role/kitehub-production-ec2-app/i-05d7af46d01436b96`
is not authorized to perform `ses:SendEmail`
on resource `arn:aws:ses:ap-southeast-1:906286017800:identity/mvann1207@gmail.com`
(Service: Ses, Status Code: 403)"
```

IAM policy doc evidence:
```bash
$ aws iam get-role-policy --role-name kitehub-production-ec2-app --policy-name kitehub-ec2-secrets-s3
# Returns: secretsmanager + s3 statements only — zero SES action.
```

## Root cause

Phase 1 BETA terraform `infrastructure/terraform-aws/iam.tf` (or similar) khi declare ec2-app role chỉ scope cho secrets + assets bucket. SES permission chưa thêm — assumption rằng email service sẽ chạy ở service khác hoặc dùng SMTP credentials.

Verify từ kitehub-email actual code:
- `SESEmailService` uses AWS SDK `SesClient` với default credentials provider (EC2 instance profile fallback)
- Production config `EMAIL_PROVIDER=ses` hardcoded
- SMTP credentials secret `kitehub/production/ses-smtp-credentials` exists nhưng SESEmailService không dùng SMTP path

## Production impact

🔴 **Mọi outbound email từ kitehub-email production fail HTTP 500.**

Symptoms cộng hưởng:
- GAP-606 admin-new-login-alert: template missing AND IAM missing — combined error chain
- Beta invites: outbox stuck (GAP-605) → nếu dispatcher fix, vẫn fail tại IAM
- DSAR acknowledgement, welcome, all: all blocked

## Proposed Fix

### Phase 1 (terraform code, ≤10 min)
Edit `infrastructure/terraform-aws/iam.tf` (or wherever `kitehub-production-ec2-app` defined) — add statement:

```hcl
statement {
  sid    = "SesSendEmail"
  effect = "Allow"
  actions = [
    "ses:SendEmail",
    "ses:SendRawEmail",
    "ses:SendTemplatedEmail",
    "ses:GetSendQuota",
  ]
  resources = [
    "arn:aws:ses:ap-southeast-1:906286017800:identity/kitehub.me",
    "arn:aws:ses:ap-southeast-1:906286017800:identity/*@kitehub.me",
    # During SES sandbox: allow any verified identity destination
    "arn:aws:ses:ap-southeast-1:906286017800:identity/*",
    "arn:aws:ses:ap-southeast-1:906286017800:configuration-set/*",
  ]
}
```

### Phase 2 (terraform apply, ≤5 min)
`gh workflow run terraform-apply.yml -f targets='aws_iam_role_policy.ec2_secrets_s3' -f confirm=APPLY -f dry_run=true` → review → `dry_run=false`.

IAM update is in-place; no EC2 restart needed. Container immediately gets new permission via instance profile.

### Phase 3 (SES sandbox exit — separate gap candidate)
Currently SES sandbox = only verified recipients can receive. Beta cohort scale needs production access (~24h AWS review). File follow-up gap GAP-609.

### Phase 4 (consider Resend alternative)
`kitehub/production/resend-api-key` secret exists; `EMAIL_PROVIDER=ses` hardcoded production. Could switch to Resend for higher deliverability + no AWS quota concern. Cost-benefit: Resend Pro $20/mo vs SES sandbox+production-access overhead. Tracked as decision gap candidate GAP-610.

## Acceptance Criteria

- [ ] Phase 1: IAM policy `kitehub-ec2-secrets-s3` includes SES statement
- [ ] Phase 2: terraform apply success; `aws iam simulate-principal-policy --policy-source-arn <role-arn> --action-names ses:SendEmail` returns Allow
- [ ] Live verify: POST kitehub-email/api/platform/emails/send với verified recipient (admin@kitehub.me) returns `status: SENT` + Resend/SES `messageId` populated
- [ ] Email actually arrives tới verified recipient

## Related

- GAP-605 (sister — outbox dispatcher; both Wave 90 surfaced)
- GAP-606 (sister — template missing; cascade with this)
- GAP-607 (sister — DLQ missing; allowed retry spam)
- (future) GAP-609 — SES sandbox exit / production access
- (future) GAP-610 — SES vs Resend vendor decision
- `release-deploy-standard.md` §3.1 PRE-RELEASE smoke admin-login (current only checks login HTTP 200 — extend với email-deliver check would have caught this Wave 88)
- `terraform-cloud-deploy/SKILL.md` IAM review checklist (paired same wave)

## Log

- **2026-05-17:** Gap filed during Wave 90 walkthrough. Direct kitehub-email POST returned HTTP 200 with body `{"status":"FAILED","errorMessage":"...ses:SendEmail 403"}`. Same error pattern in admin-new-login-alert retries since Wave 88 cutover (every admin login emits failed-to-send event). Critical for beta cohort onboarding — zero outbound email works currently.
