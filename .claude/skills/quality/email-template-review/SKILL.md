---
name: email-template-review
description: "Use when PR touches kitehub-email templates (*.html under templates/emails/), kiteclass-gateway email templates, any new customer-facing email, phrases 'review email', 'kiểm tra email template', 'gửi email cho tenant', 'trial-expiry email', 'welcome email'. Enforces brand/legal/i18n/mobile/GDPR-PDPL gate before customer send per output-review-mandate §5.7."
user-invocable: true
---

# Email Template Review

Review gate cho customer-facing email templates TRƯỚC KHI ship hoặc send. Closes GAP-173 + output-review-mandate §5.7 VIOLATION #4.

## When to run

- PR adds/modifies any file under:
  - `kitehub/kitehub-email/src/main/resources/templates/emails/*.html`
  - `kiteclass/kiteclass-gateway/src/main/resources/templates/email/*.html`
- New email type added to `EmailService` / `NotificationService`
- Brand/logo change that affects email header
- Legal copy change (privacy policy link, unsubscribe URL, address)

**Required reviewers per email class:**
| Email class | Engineer | Brand/Marketing | Legal |
|-------------|:--------:|:---------------:|:-----:|
| Transactional (verification, password reset, invitation) | ✅ | optional | optional |
| Lifecycle (welcome, trial-expiry, payment) | ✅ | ✅ | ✅ |
| Marketing/promo | ✅ | ✅ | ✅ |

## Process

1. **Identify email class** — look up template name + trigger. Apply reviewer matrix above.
2. **Run through checklist** — open `reference/checklist.md` and tick every item. Do NOT skip compliance rows.
3. **Render preview with sample data** — use `reference/sample-data.md` JSON blocks to feed Thymeleaf preview (MailHog / Mailtrap / local `EmailPreviewController` if wired).
4. **Mobile test** — render at 320px width, verify no horizontal scroll, CTA tappable.
5. **Clients test** — capture screenshot in Gmail web + mobile + Outlook web (minimum). If Litmus/Email-on-Acid available, run full client matrix.
6. **Record review** — add PR comment: "Email review PASS per `.claude/skills/quality/email-template-review/`. Sign-offs: eng=@name, brand=@name, legal=@name (or N/A with reason)."
7. **If FAIL** — list failing checklist items in PR, block merge until fixed.

## Skill Contents

- `reference/checklist.md` — full 40-point checklist (brand, legal, i18n, variables, mobile, accessibility, tenant isolation, compliance)
- `reference/sample-data.md` — canonical Thymeleaf sample data per template for preview rendering

## Gotchas

- **Current baseline (verified 2026-04-20):** kitehub-email has 13 templates + kiteclass-gateway 3 = 16 total. ALL currently missing: unsubscribe link, physical company address, EN fallback, explicit PDPL/GDPR legal basis text. First review round = MANY failures; file as GAP or fix in same PR — do not waive.
- **Thymeleaf escaping:** `th:text` auto-escapes; but `th:utext` does NOT — grep for `th:utext` and verify variables are sanitized server-side (XSS in email client preview panes is real).
- **Logo URL:** must be absolute HTTPS URL with stable CDN/MinIO path. Relative URLs break in most email clients. Verify `${branding.logoUrl}` is resolved to full URL in `EmailService` BEFORE Thymeleaf render.
- **Dark mode:** Gmail iOS + Outlook apply auto dark-mode inversion — test header gradient + button contrast in dark background. Add `<meta name="color-scheme" content="light only">` if invertion ruins brand.
- **Tenant isolation in merge-fields:** verify template uses `${tenantContext.displayName}` not `${currentUser.tenant.name}` (latter leaks across sessions on reused threads).
- **Inline CSS:** email clients strip `<style>` block inconsistently. Use inline styles for critical brand/CTA colors. Our current templates use `<style th:inline>` which works in Gmail but fails in Outlook 2016+ — run Premailer or equivalent inliner before sending.
- **Unsubscribe mandate:** CAN-SPAM + PDPL require 1-click unsubscribe link for marketing/promo emails; transactional emails are exempt but still should carry "Why did I get this?" link.
- **Vietnam advertising law (Nghị định 91/2020):** marketing emails MUST have `[QC]` prefix in subject for promotional content, sender identity in body, and opt-out instructions. See `reference/checklist.md` §Compliance.

## Related

- Rule: `.claude/rules/output-review-mandate.md` §5.7
- Sibling skill: `.claude/skills/quality/marketing-legal-review/SKILL.md` (broader marketing + legal scope)
- Gap: `documents/04-quality/gaps/closed/GAP-173-email-template-review.md`
- Sibling gap: GAP-063 (SMS + Zalo notifications — same review pattern applies there once wired)
