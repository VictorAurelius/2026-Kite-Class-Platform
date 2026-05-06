# GAP-370: Email Transactional Infrastructure (SendGrid / SES / Mailgun)

**Status:** 🔵 OPEN
**Priority:** 🔴 P0 BLOCKING (Phase 1 BETA — required cho email verification + invite emails + password reset)
**Domain:** Infrastructure / DevOps
**Found:** 2026-05-06 (Release 1 deploy plan)
**Affects:** Tenant signup flow, beta invite delivery, password reset, account notifications

## Problem

KHÔNG có email transactional service setup. Cần để:
- Email verification cho signup
- Beta invite emails với signup token
- Password reset emails
- Account notifications (subscription, payment, support)
- Marketing emails (post-Release 1)

Existing `kitehub-email` service tồn tại (per Wave 18a) nhưng chưa có production-grade outbound email provider integration.

## Proposed Fix

### Vendor decision (3 options)

**Option A — SendGrid:**
- Pros: industry standard, good deliverability, free tier 100 emails/day
- Cons: Vietnam regulation compliance unclear, $$$ at scale ($90/mo for 100k emails)
- Setup: 1-2 days

**Option B — AWS SES:**
- Pros: low cost ($0.10/1000 emails), tích hợp EKS, SPF/DKIM auto
- Cons: deliverability lower mặc định (sandbox limits + warmup), Vietnam region không có (us-east-1 / ap-southeast-1)
- Setup: 2-3 days (sandbox → production approval)

**Option C — Mailgun:**
- Pros: developer-friendly, good API, EU-based
- Cons: $$$ ($35/mo for 50k emails), lesser brand recognition VN

**Recommend:** AWS SES vì cost-effective + AWS infrastructure already in place (terraform-aws). Set up trong AWS account hiện tại.

### Implementation

- **AWS SES setup:**
  - Verify domain (SPF/DKIM/DMARC TXT records added per GAP-369)
  - Request production access (sandbox → out-of-sandbox approval)
  - Configure sending limits + warmup schedule
- **kitehub-email service integration:**
  - Use AWS SES SDK (`software.amazon.awssdk:ses`)
  - Configure region + IAM role
  - Email templates externalized (FreeMarker / Thymeleaf)
- **Templates needed:**
  - Beta invite email (signup token + disclaimer + beta period)
  - Email verification (code + link)
  - Welcome email post-signup
  - Password reset (token + expiry)
  - Subscription notifications
  - Support notifications (ticket received, ticket resolved)

## Acceptance Criteria

- [ ] Vendor decision documented + approved
- [ ] DNS TXT records added (SPF, DKIM, DMARC)
- [ ] AWS SES production-ready (or chosen vendor)
- [ ] kitehub-email service integration tested
- [ ] Email templates created (5+ minimum)
- [ ] Smoke test: signup → email verification end-to-end
- [ ] Rate limit + bounce/complaint handling configured
- [ ] Logs capture send/deliver/bounce events
- [ ] Verified domain reputation pre-launch

## Open decisions

- Vendor pick (SendGrid vs SES vs Mailgun)
- Sender address (noreply@kitehub.vn? hello@kitehub.vn?)
- Reply-to handling (noreply with auto-reply vs real inbox)

## Effort estimate

~2-3 ngày setup + ~1 ngày integration testing.

## Related

- Parent plan: `documents/03-planning/roadmap/release-1-deploy-plan.md`
- Sister: GAP-369 (DNS setup — TXT records dependency)
- Existing: `kitehub-email` service module

## Log

- **2026-05-06:** Filed by Release 1 deploy plan PR. BLOCKING cho Phase 1 BETA — beta invite emails + email verification cannot ship without this.
