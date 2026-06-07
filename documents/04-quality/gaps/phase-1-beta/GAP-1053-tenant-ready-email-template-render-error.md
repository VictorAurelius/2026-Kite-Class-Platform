# GAP-1053: tenant-ready email Thymeleaf template render error — no MailHog delivery

**Status:** 🔵 OPEN
**Priority:** 🟠 P1
**Domain:** Backend
**Found:** 2026-06-07 (Wave p0-1 G3 walk — GAP-948 verification)
**Affects:** `kitehub-email` — tenant-ready email delivery (KC-1 provisioning trust signal)

## Problem

GAP-948 wiring is G3-verified live (tenant.deployed → consumer → outbox → EmailConsumer → kitehub-email `EmailController` receives the send request). But the tenant-ready email never renders/delivers to MailHog because the **Thymeleaf template render throws**:

```
org.thymeleaf.TemplateEngine.process(...)
  at com.kitehub.email.service.EmailTemplateRenderer.renderHtmlWithFallback(EmailTemplateRenderer.java:110)
  at com.kitehub.email.service.EmailTemplateRenderer.render(EmailTemplateRenderer.java:95)
  at com.kitehub.email.service.SESEmailService.sendTemplatedEmail(SESEmailService.java:113)
  at com.kitehub.email.service.EmailProviderRouter.sendTemplatedEmail(EmailProviderRouter.java:104)
  at com.kitehub.email.listener.EmailEventListener.onEmailEvent(EmailEventListener.java:145)
```

Observed Wave p0-1 G3 walk: published `tenant.deployed` for 2 instances (Sky Education, khanh-phapluat) → both reached kitehub-email `EmailController` ("Received email send request for") but no MailHog message arrived (total stayed 25). Sync HTTP path returned DUPLICATE (Redis idempotency `http:<hash>` from prior session); async `EmailEventListener` path threw the template render error. This matches GAP-948 AC#2 "Resend `tenant-ready` template (HTML+txt) creation pending".

## Proposed Fix

Create/repair the `tenant-ready` email template (HTML + txt) so `EmailTemplateRenderer.renderHtmlWithFallback` resolves it; verify `renderHtmlWithFallback` truly falls back (it currently throws instead of degrading gracefully — the "fallback" path itself may be the bug). Then re-run G3 walk: publish `tenant.deployed` for a fresh recipient → MailHog shows rendered tenant-ready email.

## Acceptance Criteria

- [ ] `tenant-ready` Thymeleaf template (HTML + txt) exists + renders without throwing
- [ ] `renderHtmlWithFallback` degrades gracefully on missing template (no exception escapes)
- [ ] G3 walk: publish `tenant.deployed` for fresh instance → MailHog shows rendered tenant-ready email (closes GAP-948 AC#2)

## Related

- Discovered in: Wave p0-1 G3 walk 2026-06-07 (PR #2241)
- Parent: GAP-948 (tenant-ready email wiring — wiring DONE, this is the template/delivery residual)
- Sister: GAP-948 AC#2 "Resend tenant-ready template pending"
