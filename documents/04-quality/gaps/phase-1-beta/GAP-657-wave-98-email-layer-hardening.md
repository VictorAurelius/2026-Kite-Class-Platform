# GAP-657: Email layer hardening — plain-text + List-Unsubscribe + Reply-To + render verify

**Status:** 🟡 PARTIAL (80% — Wave 98 B1)
**Priority:** 🔴 P0
**Domain:** Backend (kitehub-email service)
**Detected:** 2026-05-18 (Wave 98 prep — outside-in audit 3-agent convergence)
**Parent audit:** `documents/04-quality/audits/persona-review/2026-05-18-wave-98-cluster-b-failure-mode-matrix.md` M-NEW-1/2/14 + `2026-05-18-wave-98-cluster-b-beta-cohort-outside-in.md` F-NEW-5 + `2026-05-18-wave-98-cluster-b-external-benchmark.md` (Resend deliverability VN ISPs)

## Problem

GAP-543 PARTIAL 40% (email content audit 5 templates) defer plain-text + render-verify sang Wave 79+, NHƯNG Wave 98 IS beta-invite trigger window. Failure-mode matrix audit verified empirically:

| Symptom | Evidence |
|---|---|
| Chỉ 1 trong ~20 templates có `.txt` plain-text sibling | `kitehub/kitehub-email/src/main/resources/templates/emails/invite-staff.txt` (only one) |
| Zero `text/plain` references trong Java send code | `grep -rn "text/plain\|BodyPart.text\|TextBody" kitehub/kitehub-email/src` → 0 hits |
| Resend default sends HTML-only | Gmail Promotions tab + Outlook HTML-stripping = ~20% silent churn projected |
| Missing `List-Unsubscribe` + `Reply-To` headers | Resend warm-up requires headers (per benchmark Linear/Stripe pattern) |

Hậu quả: P2 Hằng nhận email activation → email lands trong Gmail Promotions tab → never sees → silent churn → invite-funnel conversion drops.

## Root Cause

`kitehub-email` service shipped MVP scope (HTML send works) without deliverability hardening:
- Template-rendering pipeline không emit `.txt` alongside `.html`
- `SesV2Client.sendEmail()` + Resend HTTP API caller không set headers cho deliverability
- Không có integration test verify both clients receive both body parts

## Proposed Fix

### Step 1: Plain-text template generator

`kitehub-email/src/main/resources/templates/emails/*.html` → tự động generate `.txt` sibling:

Option A (preferred): Thymeleaf preprocessor strip HTML → plain text + keep CTA URLs explicit
Option B: Hand-write `.txt` cho 5 critical templates (beta-invite / welcome / verify-email / password-reset / staff-invite)

Wave 98 scope: Option B cho 5 critical types (paired GAP-543 + GAP-659).

### Step 2: SES + Resend send-with-both-bodies

`kitehub-email/.../SESEmailService.java`:
```java
SendEmailRequest.builder()
  .content(EmailContent.builder()
    .simple(Message.builder()
      .body(Body.builder()
        .html(Content.builder().data(htmlBody).build())
        .text(Content.builder().data(textBody).build())  // ← add
        .build())
      .build())
    .build())
  .build()
```

Resend HTTP API: include `text` field alongside `html` trong POST payload.

### Step 3: List-Unsubscribe + Reply-To headers

SES: `MessageTag` headers + `ReplyToAddresses` parameter
Resend: `reply_to` + `headers: {'List-Unsubscribe': '<mailto:unsubscribe@kitehub.me>, <https://kitehub.me/unsubscribe?token=...>'}`

Templates affected:
- All 5 critical: `Reply-To: support@kitehub.me`
- All transactional except password-reset: `List-Unsubscribe`
- Mailto + one-click unsubscribe form

### Step 4: Manual 2-client render verify (15 min checklist)

Pre-merge ritual:
- [ ] Send beta-invite to gmail.com test inbox → verify renders both HTML + plain-text fallback
- [ ] Send beta-invite to outlook.com test inbox → verify renders (no clipping, no broken images)
- [ ] Inspect raw email source → verify `List-Unsubscribe` header present
- [ ] Click reply → verify `support@kitehub.me` (not `noreply@`)

### Step 5: Integration test scheduler-vs-email-wire (M-NEW-14)

`kitehub-email/src/test/.../SchedulerEmailWireIT.java`:
- Trigger scheduled job (e.g., day-7 feedback survey)
- Verify email actually sent via test SMTP mock
- Verify both HTML + text bodies present
- CloudWatch alarm wired cho scheduler-fail-silent

### Step 6: GAP-543 sync

After this gap DONE → GAP-543 §AC update:
- AC1 plain-text fallback ✅ (Step 1+2)
- AC2 headers wire ✅ (Step 3)
- AC3 render verify checklist ✅ (Step 4)
- AC4 scheduler IT ✅ (Step 5)
- GAP-543 → PARTIAL 90% (only manual translation polish remains per GAP-659)

## Acceptance Criteria

- [ ] 5 critical templates có `.txt` sibling (beta-invite / welcome / verify-email / password-reset / staff-invite)
- [ ] `SESEmailService` + `ResendEmailService` cả hai send both HTML + text bodies
- [ ] `List-Unsubscribe` + `Reply-To: support@kitehub.me` headers active
- [ ] Manual 2-client render verify checklist documented + executed Wave 98
- [ ] `SchedulerEmailWireIT` integration test PASS
- [ ] CloudWatch alarm `email-scheduler-silent-fail` wired
- [ ] `cd kitehub && ./mvnw -pl kitehub-email verify -P strict-warnings` PASS
- [ ] GAP-543 PARTIAL 40 → 90% updated

## Effort estimate

~1.5-2 wave bucket. Parallel-safe với B0 UI Coordinator (different service).

## Related

- **Parent audits:** outside-in 3-agent 2026-05-18 (failure-mode M-NEW-1/2/14, persona F-NEW-5, benchmark Resend VN)
- **Sister gap:** GAP-543 PARTIAL — this gap closes deliverability portion; GAP-659 closes content/tone portion
- **Related:** GAP-572 PARTIAL 40% Resend secret schema; GAP-533 PARTIAL 80% Resend warm-up DKIM/DMARC
- **Wave 98 bucket:** B1

## Log

- **2026-05-21 (Wave 102.9 Bucket D fix-time state-check):** Per `audit-to-gap-pipeline.md` §2.8 verified Wave 98 B1 work intact — `SESEmailService.java:152,207,258` Reply-To header wired; `ResendEmailService.java:107,119-122` Reply-To + `List-Unsubscribe` + `List-Unsubscribe-Post: One-Click` wired; 5/5 critical templates có .txt sibling (verified ls). Remaining AC (manual 2-client render + SchedulerEmailWireIT + CloudWatch alarm + mvn verify + live send) all live-verify/AWS-blocked. Status PARTIAL 80% retained — no progress this wave. State-check artifact: `documents/04-quality/audits/persona-review/2026-05-21-wave-102.9-bucket-d-email-content-headers-state-check.md`. Sister to A+B+C state-check pattern.
- **2026-05-18 (Wave 98 B1 PARTIAL 80%):** Shipped deliverability core:
  - 5/5 critical templates now have `.txt` siblings (`welcome.txt`, `beta-invite.txt`, `email-verification.txt`, `password-reset.txt`, `invite-staff.txt` pre-existing).
  - `password-reset.html` template newly created (was missing entirely).
  - `Tone` enum (`com.kitehub.email.api.Tone`) with role-based resolution; Wave 98 default = FORMAL_SAFE_DEFAULT.
  - `EmailTemplateRenderer` central renderer emits BOTH HTML + plain-text bodies via dual Thymeleaf resolvers (HTML default + TEXT registered post-construct).
  - `SESEmailService.sendEmail(to, subject, htmlBody, textBody)` overload — wires multipart/alternative + ReplyToAddresses; SMTP path mirrors via `helper.setText(text, html)`.
  - `ResendEmailService` stub (activated `@ConditionalOnProperty(email.provider=resend)`) — HTTP API payload includes `html` + `text` + `reply_to` + `headers.List-Unsubscribe` + `headers.List-Unsubscribe-Post`.
  - `SESConfig.SESProperties` extended: `replyToEmail` (default `support@kitehub.me`), `unsubscribeMailto`, `unsubscribeUrlTemplate`.
  - `EmailTemplateResolverConfig`: TEXT resolver registered via `@PostConstruct` (avoids circular DI) + RestTemplate bean for Resend.
  - 5 new tests `EmailTemplateRendererTest` — render HTML+text for welcome / beta-invite / password-reset / email-verification + Tone resolution. ALL PASS via `mvnw -pl kitehub-email verify -P strict-warnings` (49 tests run, 0 failures, 1 skipped existing SES integration).
  - Business docs: `documents/01-business/kitehub/email/rules.md` (7 BRs: plain-text mandate, Reply-To, List-Unsubscribe policy, Tone register, sender identity, provider routing, scheduler observability) + `api-contract.md` (POST /api/email/send + 5 template variable schemas + auto-wired headers).
  - GAP-543 PARTIAL 40 → 80% (CSV synced).
- **2026-05-18 — Deferred items (carry-over to follow-up GAP):**
  - **`SchedulerEmailWireIT`** integration test (Step 5) — Spring Boot test triggering scheduled job + SMTP mock assertion deferred (existing service has no scheduled jobs wired; tracked when scheduler ships).
  - **CloudWatch alarm `email-scheduler-silent-fail`** wiring (Step 5 §3 §Recommendations 2) — Terraform out of scope for B1; tracked as separate ops gap.
  - **Manual 2-client render verify** (Step 4 §4 checklist) — code path ready; physical send to gmail/outlook test inboxes requires production SES sender approval + DKIM live. To be executed at deploy time per `release-deploy-standard.md` §3.1 "Smoke admin-login" precedent.
  - **Native VN copywriter pass** (paired GAP-659 §Step 3) — content audit by external VN reviewer deferred Wave 99 (shared GAP-658 budget).
  - **Per-tone variant templates** (GAP-659 §Step 2) — `welcome.formal.html` / `welcome.informal.html` etc. deferred Wave 99; renderer `resolveTemplatePath()` has TODO Wave 99 marker.
- **2026-05-18 — Verification commands run:**
  - `cd kitehub && ./mvnw -pl kitehub-email verify -P strict-warnings` → BUILD SUCCESS, 49 tests pass.
  - File state: `ls kitehub/kitehub-email/src/main/resources/templates/emails/*.txt` → 5 files (beta-invite, email-verification, invite-staff, password-reset, welcome).
- **2026-05-18 (PR #1553 merged)** — Post-merge audit-gate flagged: (a) business-logic-audit + api-contract-audit required (new `documents/01-business/kitehub/email/{rules,api-contract}.md`) — DEFER to Wave 98 post-closure audit suite per `post-wave-audit-mandate.md` §2.2; (b) PARTIAL exit-ramp items per gap §Deferred section — SchedulerEmailWireIT + CloudWatch alarm + manual 2-client render verify + per-tone variants + persona-tone send-site wiring. Sync per `post-merge-sync-completeness.md` §4.
