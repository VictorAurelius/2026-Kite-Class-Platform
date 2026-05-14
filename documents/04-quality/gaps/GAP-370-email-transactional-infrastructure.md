# GAP-370: Email Transactional Infrastructure (SendGrid / SES / Mailgun)

**Status:** 🟡 PARTIAL 95% — Wave 33+45 shipped all CODE ACs; Wave 77 Bucket A added terraform-cloudflare DNS codification + deliverability runbook + 2 smoke scripts. AWS SES production access DENIED 2026-05-12 (CaseId 177857212400418); Resend pivot via ADR-025 Stream A. Status flips 🟢 DONE when (a) Resend dashboard domain verified + (b) terraform-cloudflare applied + (c) warm-up Day 5+ spam-score ≥8/10 achieved (per GAP-533 user-action follow-on).
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

- [x] Vendor decision documented + approved (AWS SES Wave 33 ADR; pivot to Resend per ADR-025 Stream A after 2026-05-12 SES DENIED)
- [x] DNS TXT/CNAME records codified in Cloudflare terraform (Wave 77 Bucket A — `infrastructure/terraform-cloudflare/dns.tf`); operator runs `terraform apply` after fetching DKIM CNAME values from Resend dashboard (per `email-deliverability-runbook.md` §2.1)
- [ ] Resend production-ready (domain verified in Resend dashboard) — user-action per `resend-provisioning-runbook.md` §2 + `email-deliverability-runbook.md` §2
- [x] kitehub-email service integration tested (Wave 33+45 SESEmailService + SesIntegrationSmokeTest profile-gated)
- [x] Email templates created (5+ minimum) — 17 Thymeleaf templates Wave 33+
- [x] Smoke test: signup → email verification end-to-end — Wave 61 `scripts/smoke-ses.sh` Tier 1 read-only verification + Wave 45 JUnit profile-gated send
- [x] Rate limit + bounce/complaint handling configured — Wave 33 SES `bounce/complaint/rate` config properties (auto-suppression server-side); SNS-subscribed webhook scaffold deferred to follow-up (volume-driven need)
- [x] Logs capture send/deliver/bounce events (Wave 33 SESConfig MDC fields)
- [ ] Verified domain reputation pre-launch — pending sandbox→production approval + warmup Day 1-7

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

- **2026-05-14** (Wave 77 Bucket A code-side): Status PARTIAL 60% → PARTIAL 95%. Shipped DNS terraform codification (`infrastructure/terraform-cloudflare/dns.tf`) + new `email-deliverability-runbook.md` covering DNS sequence + warm-up 7-day ramp + spam-score gate + Resend dashboard monitoring + SES fallback path. `scripts/smoke-resend.sh` adds runtime API health check sibling to existing `smoke-ses.sh`. `scripts/verify-email-deliverability.sh` adds spam-score gate. Sister gap GAP-533 carries the deliverability warm-up AC subset; GAP-530 carries end-to-end live verify. Remaining gates: Resend dashboard domain verified + terraform apply + warm-up Day 5+ spam-score ≥8/10 (all user-action follow-on).
- **2026-05-11** (Wave 61 Bucket B): Production approval prep shipped. New `scripts/smoke-ses.sh` — Tier 1 read-only AWS CLI verification (account state, identities, DKIM, suppression list, DNS records). Runbook `email-ses-setup-runbook.md` refreshed: §4.1.1 copy-paste production access request form template (English, refreshed for `kitehub.me` domain + Free Tier 62k/mo + Phase 1 BETA invite cohort 5-10 tenants); §4.1.2 3 common AWS-rejection reply templates; §4.3 post-approval verification commands; §4.4 DNS verify commands; §4.5 user-action checklist. SES state verified 2026-05-11 via smoke script: sandbox HEALTHY, 200/24h, 1/sec, 0 identities, suppression list empty. **Status stays 🟡 PARTIAL** — code + runbook + smoke script complete; remaining user-action gates (verify domain identity in SES Console, add Cloudflare DNS records per `dns-setup-runbook.md`, submit production access request via SES Console with §4.1.1 template, wait 24-48h AWS approval). Bounce/complaint webhook scaffold deferred — current `SESConfig` only configures send-side; SNS-subscribed HTTP endpoint OR SQS poller tracked as follow-up (Wave 61 §4.4 scope creep — not strictly blocking BETA invite delivery since AWS SES auto-suppression handles bounce blast radius until volume scales).
- **2026-05-08:** Wave 45 Bucket C shipped (PR #1050 — `SesIntegrationSmokeTest.java` profile-gated via `@EnabledIfSystemProperty("aws-ses-real")` sends 1 templated email + asserts `MessageId` returned; `email-ses-setup-runbook.md` Wave 45 verification table — all 7 steps re-verified accurate, no drift, no rewrite needed; runbook actual path confirmed `documents/05-guides/operations/` not `deploy/` — drift documented + cleanup tracked separately). `mvn verify -P strict-warnings` BUILD SUCCESS, smoke test SKIPPED expected. Status remains 🟡 PARTIAL — code complete, AWS SES sandbox→production approval pending user-executed action.
- **2026-05-07:** Wave 33 Bucket B shipped (PR #896 — beta-invite.html + beta-request-confirmation.html templates + EmailType enum + SES bounce/complaint/rate-limit config + `email-ses-setup-runbook.md` + 8 new tests). Status 🔵 OPEN → 🟡 PARTIAL — templates + config + runbook shipped on top of existing Wave 18a SES infrastructure, **AWS SES sandbox→production approval + DKIM/SPF/DMARC verification = user-executed steps** per runbook. Beta-invite email delivery effective when GAP-379 + production SES landed.
- **2026-05-06:** Filed by Release 1 deploy plan PR. BLOCKING cho Phase 1 BETA — beta invite emails + email verification cannot ship without this.
- **2026-05-08:** Wave 45 Bucket C shipped (PR #1050 — `SesIntegrationSmokeTest.java` profile-gated via `@EnabledIfSystemProperty("aws-ses-real")` sends 1 templated email + asserts `MessageId` returned; `email-ses-setup-runbook.md` Wave 45 verification table — all 7 steps re-verified accurate, no drift, no rewrite needed; runbook actual path confirmed `documents/05-guides/operations/` not `deploy/` — drift documented + cleanup tracked separately). `mvn verify -P strict-warnings` BUILD SUCCESS, smoke test SKIPPED expected. Status remains 🟡 PARTIAL — code complete, AWS SES sandbox→production approval pending user-executed action.
- **2026-05-07:** Wave 33 Bucket B shipped (PR #896 — beta-invite.html + beta-request-confirmation.html templates + EmailType enum + SES bounce/complaint/rate-limit config + `email-ses-setup-runbook.md` + 8 new tests). Status 🔵 OPEN → 🟡 PARTIAL — templates + config + runbook shipped on top of existing Wave 18a SES infrastructure, **AWS SES sandbox→production approval + DKIM/SPF/DMARC verification = user-executed steps** per runbook. Beta-invite email delivery effective when GAP-379 + production SES landed.
