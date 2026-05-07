# GAP-370: Email Transactional Infrastructure (SendGrid / SES / Mailgun)

**Status:** 🟡 PARTIAL
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

## Standards reference (added 2026-05-06)

Per `.claude/rules/release-deploy-standard.md` §3 — this gap satisfies a checklist item from one of the per-bump-type artifact requirements. Grounded in:

- **AWS Well-Architected Framework** (Operational Excellence / Security / Reliability pillars)
- **The Twelve-Factor App** (config + deploy patterns where applicable)
- **Project source-of-truth:** `documents/02-architecture/deployment-strategy.md` (GAP-103 DONE 2026-04-18)
- **ADR-015** (AWS Agent Plugins evaluation = DEFER Q3 2026)
- **GAP-381** (Claude agent deploy framework — agent role per phase)

## Log

- **2026-05-06:** Filed by Release 1 deploy plan PR. BLOCKING cho Phase 1 BETA — beta invite emails + email verification cannot ship without this.
- **2026-05-07:** Wave 33 Bucket B shipped (PR #896 — beta-invite.html + beta-request-confirmation.html templates + EmailType enum + SES bounce/complaint/rate-limit config + `email-ses-setup-runbook.md` + 8 new tests). Status 🔵 OPEN → 🟡 PARTIAL — templates + config + runbook shipped on top of existing Wave 18a SES infrastructure, **AWS SES sandbox→production approval + DKIM/SPF/DMARC verification = user-executed steps** per runbook. Beta-invite email delivery effective when GAP-379 + production SES landed.
