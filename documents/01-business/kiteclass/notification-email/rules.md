# Notification & Email Business Logic

> Last verified: 2026-03-24 | Source: `kiteclass-core/common/service/email/`

## 1. Rules

| ID | Rule | Detail | Config Key |
|----|------|--------|-----------|
| EM-01 | Email service interface | 5 methods: simple, HTML, template, contact notification, lead confirmation | — |
| EM-02 | Default implementation | LoggingEmailService — logs to console, no SMTP | `@Service` default bean |
| EM-03 | Production implementation | Replace with SmtpEmailService or external (SendGrid, AWS SES) | SMTP config in `application.yml` |
| EM-04 | Contact notification recipient | Admin email from config | `contact.admin-email` |
| EM-05 | Contact notification default | `admin@kitehub.me` | `contact.admin-email` default |
| EM-06 | Email failure isolation | Email send failure does NOT fail the business operation (try-catch) | — |
| EM-07 | Multi-tenant isolation | Emails triggered per tenant context (instanceId) | — |
| EM-08 | Lead email uniqueness | One lead per email per tenant — duplicate rejected before email | BR-MKT-002 |

## 2. Flow

### Contact Message Flow (BR-MKT-003)
```
Visitor submits contact form
  → Save ContactMessage (instanceId = tenantId)
  → sendContactNotification(adminEmail, name, email, subject, message)
    → Format: "You have received a new contact message..."
    → Subject: "New Contact Message: {subject}"
  → If email fails → log error, return success anyway
```

### Lead Confirmation Flow (BR-MKT-004)
```
Visitor registers trial/interest
  → Validate email unique within tenant (BR-MKT-002)
  → Save Lead (instanceId = tenantId)
  → sendLeadConfirmation(email, name)
    → Format: "Dear {name}, Thank you for your interest..."
    → Subject: "Thank you for your interest - KiteClass"
  → If email fails → log error, return success anyway
```

### Template Email Flow
```
Service calls sendTemplateEmail(to, subject, templateName, variables)
  → Resolve template by name (without extension)
  → Apply variables to template
  → Send HTML email
```

## 3. Emails

| Trigger | Method | Recipient | Subject Pattern |
|---------|--------|-----------|----------------|
| Contact form submitted | `sendContactNotification()` | Admin (config) | "New Contact Message: {subject}" |
| Lead/trial registration | `sendLeadConfirmation()` | Lead email | "Thank you for your interest - KiteClass" |
| Generic notification | `sendSimpleEmail()` | Any | Custom |
| HTML content | `sendHtmlEmail()` | Any | Custom |
| Template-based | `sendTemplateEmail()` | Any | Custom |

## 4. Config

```yaml
# Contact notification
contact:
  admin-email: admin@kitehub.me    # EM-04, EM-05: recipient for contact form notifications

# SMTP (production — not yet implemented)
spring:
  mail:
    host: smtp.example.com
    port: 587
    username: ${SMTP_USERNAME}
    password: ${SMTP_PASSWORD}
    properties:
      mail.smtp.auth: true
      mail.smtp.starttls.enable: true

# External provider (alternative)
# email.provider: sendgrid | aws-ses
# email.api-key: ${EMAIL_API_KEY}
```

### Implementation Notes
- Current: `LoggingEmailService` (dev/test only, logs all emails)
- Production: swap bean to SMTP or external service
- All 5 methods in `EmailService` interface must be implemented
- Contact + Lead flows use try-catch — business operation always succeeds

## Five-attribute review per `business-logic-review.md`

Per-rule attributes (Source / Rationale / Reviewer / Compliance check / Review cadence) backfilled at file-level placeholder per Phase 1 of GAP-433. Per-rule granularity tracked via GAP-156 Phase 2 stakeholder sign-offs.

- **Source:** Existing rules in this file derive from a mix of: feature gaps cited inline (where present), ADRs, persona reviews, and informed-gut estimates from Wave 1-30 work. Rules without inline citation default to `informed gut` per `business-logic-review.md` §2.1 and inherit quarterly re-review obligation below.
- **Rationale:** Rule values reflect product judgment + (where applicable) competitor benchmarks + VN regulatory minimums. Detailed per-rule rationale to be backfilled during GAP-156 Phase 2 stakeholder review; until then, treat values as `informed gut` subject to next quarterly review.
- **Reviewer:** @nguyenvankiet (acting Product Owner, solo-dev, 2026-05-08). Formal stakeholder + legal counsel sign-off queued via GAP-156. Solo-dev exemption per `business-logic-review.md` §2.3 — the Reviewer line documents which hat is being worn AND obligation is attached for team-growth or pre-launch trigger.
- **Compliance check:** **Compliant** — PDPL 2023 Art 11 (email consent); CAN-SPAM-equivalent practices (unsubscribe link mandatory). Cross-reference `kitehub/notification/rules.md`.
- **Review cadence:** Quarterly (default per `business-logic-review.md` §2.5). **Next review:** 2026-08-08. Event triggers: PDPL implementing-decree on direct marketing, email-platform swap.

## Log

- **2026-05-08** Backfill 5-attribute review section per GAP-433 Phase 1 (`business-logic-review.md` §2 standard). Placeholder Reviewer + Quarterly cadence + domain-specific Compliance check. GAP-156 Phase 2 will replace placeholders with stakeholder sign-offs.
