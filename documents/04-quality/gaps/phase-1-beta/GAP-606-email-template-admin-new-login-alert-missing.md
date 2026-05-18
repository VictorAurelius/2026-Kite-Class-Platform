# GAP-606 — Email template `admin-new-login-alert.html` MISSING; kitehub-email returns HTTP 500 → consumer infinite retry

**Status:** 🔵 OPEN
**Priority:** 🔴 P0
**Domain:** Backend
**Found:** 2026-05-17 (Wave 90 walkthrough — phát hiện trong kitehub-email log spam)
**Affects:** Mọi admin login event (Wave 90 audit ghi 1 admin login → ∞ retries); RMQ consumer poisoned; queue depth tăng vô hạn

## Problem

`kitehub-admin` (hoặc `kitehub-subscription` auth flow) phát event `type=admin-new-login-alert, to=admin@kitehub.me` qua RMQ. EmailConsumer trong kitehub-subscription pickup event → POST kitehub-email/api/platform/emails/send với `templateName=admin-new-login-alert` → kitehub-email Thymeleaf throws:

```
org.thymeleaf.exceptions.TemplateInputException: Error resolving template [emails/admin-new-login-alert],
template might not exist or might not be accessible by any of the configured Template Resolvers
  at org.thymeleaf.engine.TemplateManager.resolveTemplate(TemplateManager.java:869)
  at com.kitehub.email.service.SESEmailService.renderTemplate(SESEmailService.java:232)
  ...
```

→ kitehub-email returns **HTTP 500**. EmailConsumer retries indefinitely (no DLQ — see GAP-607).

`find kitehub/kitehub-email/src/main/resources/templates/emails -name "admin-new-login*"` → 0 results. Template file genuinely missing from source.

## Affected templates (audit may surface more)

| Template | Status | Producer | Triggered when |
|---|---|---|---|
| `admin-new-login-alert.html` | ❌ MISSING | kitehub-admin (login flow) | Every admin login |
| `beta-invite.html` | ✅ EXISTS | kitehub-subscription BetaAccessService | Beta approve |
| `beta-request-confirmation.html` | ✅ EXISTS | kitehub-subscription | Beta submission |
| `welcome.html`, `trial-*.html`, `subscription-*.html` | ✅ EXISTS | Various | Various |

Full audit needed via grep `templateName=` references vs `templates/emails/` file list.

## Root cause

Producer (admin login flow) shipped without paired template file. Either:
1. Template authored but not committed
2. Template path mismatch (`admin-new-login-alert` vs `admin-login-alert` vs `new-admin-login`)
3. Template never created — producer code references aspirational template name

Wave 90 audit found ZERO references trong source code: `grep -rn admin-new-login-alert kitehub/kitehub-email/src` returns nothing. Producer side `grep -rn admin-new-login-alert kitehub/kitehub-admin/src kitehub/kitehub-subscription/src` need check.

## Production impact

🔴 Every Platform Admin login → 1 event → consumer infinite retry → RMQ queue grows + container log spam (~10 entries/sec) + EmailConsumer thread saturated for other events. **Indirectly contributed to GAP-605 beta.invite delays** (consumer thread busy on poison message).

## Proposed Fix

### Phase 1 (hotfix, ≤30 min)
1. Determine intended template content (security alert: "Admin login from new IP at HH:MM" — see Wave 85 GAP-577 admin hardening for context)
2. Create `kitehub/kitehub-email/src/main/resources/templates/emails/admin-new-login-alert.html` per `email-template-review/SKILL.md` 40-point checklist
3. Variables required (infer from EmailEvent producer): `adminEmail`, `loginTime`, `ipAddress`, `userAgent`, `geolocation` (optional)
4. Vietnamese narrative per `dev-readable-doc-language.md`
5. Brand fallback per existing template pattern

### Phase 2 (audit + harden)
1. Add CI check `scripts/check-email-template-coverage.sh`: scan producer `templateName=*` strings vs `templates/emails/*.html` filenames; fail if mismatch
2. Per `release-deploy-standard.md` §3.1 PRE-RELEASE smoke admin-login (which exists) — extend with email-delivery smoke check
3. File follow-up for any other missing templates surfaced via audit

## Acceptance Criteria

- [ ] `admin-new-login-alert.html` template ships in next deploy
- [ ] Production EmailConsumer log: no `TemplateInputException` for 1 hour post-deploy
- [ ] Admin login → email actually delivers tới `admin@kitehub.me` (SES production access OR verified identity)
- [ ] CI grep check producer↔template parity (Phase 2)

## Related

- GAP-605 (sister — outbox dispatcher; both bugs surface same incident)
- GAP-607 (sister — RMQ DLQ missing; allowed infinite retry)
- Wave 85 GAP-577 Platform admin hardening (likely intended consumer of this alert email)
- `email-template-review/SKILL.md` — 40-point template checklist for v1
- `release-deploy-standard.md` §3.1 — PRE-RELEASE smoke admin-login (current scope: login HTTP 200; extend: + email delivery within 60s)

## Log

- **2026-05-17:** Gap filed during Wave 90 walkthrough. Log spam pattern caught when investigating beta.invite delivery failure. Wave 90 audit observation: ~10 retries/sec since Wave 88 cutover = ~864K wasted RMQ messages over 24h.
