# GAP-1053: tenant-ready email Thymeleaf template render error — no MailHog delivery

**Status:** 🟢 DONE
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

- [x] `tenant-ready` Thymeleaf template (HTML + txt) exists + renders without throwing — **created + verified 2026-06-07** (kitehub-email 102 tests green)
- [x] `renderHtmlWithFallback` degrades gracefully on missing template (no exception escapes) — **fixed 2026-06-07** (degrades gracefully; no TemplateInputException)
- [x] G3 walk: publish `tenant.deployed` for fresh instance → MailHog shows rendered tenant-ready email (closes GAP-948 AC#2) — **verified live 2026-06-07** (fresh recipient nhi-hoathcs → `template: tenant-ready` → `[SMTP] Email sent`; MailHog 25→26)

## Log

- **2026-06-07 (Wave p0-prov-1 closure):** Status OPEN → 🟢 DONE. Created `tenant-ready` Thymeleaf template (HTML + txt) + made `EmailTemplateRenderer.renderHtmlWithFallback` degrade gracefully instead of throwing (kitehub-email 102 tests green). Live verify: published `tenant.deployed` for fresh recipient nhi-hoathcs → kitehub-email `template: tenant-ready`, `textBody present: true`, `[SMTP] Email sent`; MailHog 25→26, rendered tenant-ready email delivered (no TemplateInputException). Closes GAP-948 AC#2 (tenant-ready render/delivery); GAP-948 flipped DONE same wave.

## Related

- Discovered in: Wave p0-1 G3 walk 2026-06-07 (PR #2241)
- Parent: GAP-948 (tenant-ready email wiring — wiring DONE; this template/delivery residual closed same wave)
- Sister: GAP-948 AC#2 "Resend tenant-ready template pending" — now satisfied
