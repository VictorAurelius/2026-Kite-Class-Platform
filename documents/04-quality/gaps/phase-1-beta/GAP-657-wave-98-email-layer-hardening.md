# GAP-657: Email layer hardening — plain-text + List-Unsubscribe + Reply-To + render verify

**Status:** 🔵 OPEN
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
