---
audience: dev
---

# GAP-747 — SES IAM live verify post AWS account restore (GAP-608 follow-up)

**Status:** 🔵 OPEN
**Priority:** 🟠 P1
**Domain:** DevOps
**Found:** 2026-05-25 (Wave beta-readiness-5 Bucket B — paired follow-up filing GAP-608 PARTIAL closure)
**Last-Verified:** 2026-05-25
**Blocked-by:** GAP-612 (AWS account suspended 2026-05-17 16:50 UTC; live apply + verify cannot run until account restored)

## Problem

GAP-608 Wave beta-readiness-5 Bucket B PR shipped terraform IaC declaration cho `aws_iam_role_policy.ec2_ses_send` (attach SES send permissions vào `kitehub-production-ec2-app` role). Static syntax check (`terraform fmt`) PASS. NHƯNG live verify deferred do AWS account suspended per GAP-612 — không thể run `terraform apply` hay test actual SES SendEmail call.

Acceptance criteria của GAP-608 còn 1/4 items pending verification:

- [x] `aws_iam_role_policy.ec2_ses_send` resource declared in `infrastructure/terraform-aws/iam.tf`
- [x] `terraform fmt -check` PASS (static syntax valid)
- [ ] **Live apply:** `gh workflow run terraform-apply.yml` post-account-restore → confirm IAM policy attached without error
- [ ] **Live verify simulate:** `aws iam simulate-principal-policy --policy-source-arn arn:aws:iam::906286017800:role/kitehub-production-ec2-app --action-names ses:SendEmail --resource-arns arn:aws:ses:ap-southeast-1:906286017800:identity/admin@kitehub.me` returns `EvalDecision: allowed`
- [ ] **Live verify end-to-end:** POST `kitehub-email/api/platform/emails/send` với verified recipient `admin@kitehub.me` returns `{"status": "SENT", "messageId": "..."}` + email actually arrives inbox

## Root Cause

AWS account suspension (GAP-612) blocks all production mutation ops including terraform apply. Per `local-fix-production-parity-check.md` v1.0.0 §3.2 follow-up gap exit ramp — IaC source change ships now, verification deferred with explicit blocker reference. Standard pattern cho infrastructure changes during account-suspended window.

## Proposed Fix

### Phase 1 — post GAP-612 unblock (≤30 min)

1. Confirm GAP-612 status flipped to DONE (AWS account restored)
2. Trigger terraform apply via `terraform-apply.yml` workflow:
   ```bash
   gh workflow run terraform-apply.yml \
     -f targets='aws_iam_role_policy.ec2_ses_send' \
     -f confirm=APPLY -f dry_run=true
   # Review plan output — expect 1 add (new inline policy)
   gh workflow run terraform-apply.yml \
     -f targets='aws_iam_role_policy.ec2_ses_send' \
     -f confirm=APPLY -f dry_run=false
   ```
3. Verify policy attached:
   ```bash
   aws iam get-role-policy --role-name kitehub-production-ec2-app \
     --policy-name kitehub-ec2-ses-send
   # Expected: returns policy doc với SES statement
   ```

### Phase 2 — live end-to-end SES verify (≤15 min)

1. Run `aws iam simulate-principal-policy` per AC row 3 → expect `allowed`
2. POST kitehub-email send endpoint với verified recipient → expect SENT status + messageId
3. Confirm email arrives admin@kitehub.me inbox trong vòng 1-2 phút
4. Flip GAP-608 từ 🟡 PARTIAL → 🟢 DONE (per `gap-done-discipline.md` §2 — all 4 AC verified live)

### Out-of-scope

- SES sandbox exit (production access approval) — separate gap candidate (per GAP-608 §Proposed Fix Phase 3 note)
- SES vs Resend vendor decision — separate gap candidate (per GAP-608 §Proposed Fix Phase 4 note)
- IAM least-privilege narrowing (specific identity ARNs thay vì wildcard `identity/*`) — post-sandbox-exit follow-up

## Acceptance Criteria

- [ ] GAP-612 unblocked (AWS account restored)
- [ ] `terraform-apply.yml` workflow run với `aws_iam_role_policy.ec2_ses_send` target → SUCCESS
- [ ] `aws iam get-role-policy` returns the new policy doc
- [ ] `aws iam simulate-principal-policy` cho `ses:SendEmail` returns `allowed`
- [ ] POST kitehub-email send với verified recipient → `status: SENT` + email arrives
- [ ] GAP-608 flipped DONE per `gap-done-discipline.md` §2 với verification artifacts cited
- [ ] Pre-mutation audit artifact saved per `pre-mutation-state-check.md` §3 trong `documents/04-quality/audits/aws-verification/YYYY-MM-DD-ses-iam-live-verify.md`

## Related

- **GAP-608** (parent — PARTIAL 90% pending this verify)
- **GAP-612** (blocker — AWS account suspended; must unblock first)
- **GAP-609** (sister — SES sandbox exit / production access; can pair with this)
- **GAP-610** (sister — SES vs Resend vendor decision)
- `local-fix-production-parity-check.md` v1.0.0 §3.2 (follow-up gap exit ramp pattern this gap operationalizes)
- `pre-mutation-state-check.md` §3 (pre-apply audit artifact mandate)
- `release-deploy-standard.md` §3.1 PRE-RELEASE "Smoke admin-login" — extension candidate post-fix: add email-deliver smoke check

## Log

- **2026-05-25:** Gap filed Wave beta-readiness-5 Bucket B paired với GAP-608 PARTIAL closure. Terraform IaC declaration shipped same PR (`aws_iam_role_policy.ec2_ses_send` in `infrastructure/terraform-aws/iam.tf`); live verify gated by GAP-612 AWS account restore. Per `local-fix-production-parity-check.md` v1.0.0 §3.2 follow-up gap exit ramp — explicit blocker reference + completion criteria. Unblock condition: GAP-612 status flip to DONE.
