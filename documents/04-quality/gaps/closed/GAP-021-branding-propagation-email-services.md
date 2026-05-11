# GAP-021: Branding Propagation to Email & Other Services

**Status:** 🟢 DONE (Wave 4 MVP)
**Priority:** 🟠 P1
**Domain:** Backend / Integration
**Detected:** 2026-04-14 (simulation)
**Resolved:** 2026-04-18 (Wave 4 — branding propagation cluster)

## Wave 4 resolution (MVP)

- `kitehub-email` gains a `BrandingClient` (`WebClient` + Caffeine 5-min TTL cache)
  that fetches the tenant branding package; `SESEmailService` injects a
  `branding` variable into every Thymeleaf context.
- `EmailRequest` now accepts `instanceId` + `tenantId`. Feature-flagged via
  `kitehub.email.branding-enabled=true`; fails-closed to legacy defaults.
- `welcome.html`, `email-verification.html`, and `trial-expiration-warning.html`
  now use `var(--brand-primary)` / `var(--brand-secondary)` + `branding.logoUrl`.
  Remaining 8 templates to be migrated in a follow-up PR.
- `kiteclass-core` publishes `branding.updated` (topic exchange
  `branding.events`) from `BrandingServiceImpl.updateBranding()` via the
  existing Outbox + a best-effort direct RabbitTemplate publish.
- `kitehub-email` binds queue `email.branding.updated` and evicts the local
  cache on every event.

Tests: `BrandingClientTest` (4), `SESEmailServiceTest` (5 incl. branding
injection + fallback), `BrandingVersionServiceTest` — all green.

## Problem

Khi tenant update branding, các service khác **KHÔNG tự động** nhận update:

- ❌ `kitehub-email` dùng default templates, không tenant branding
- ❌ Welcome/payment reminder/trial expiration emails → vẫn default colors + logo
- ❌ Marketing emails từ kiteclass → không match tenant branding
- ❌ Invoice PDFs → default header
- ❌ Admin-sent notifications → không tenant-specific

**Impact:** Tenant đăng ký → rebrand đẹp → nhưng emails students nhận về **vẫn trông như default KiteClass**. Broken promise.

## Evidence

- `kitehub-email` service: templates trong `resources/templates/emails/` với hardcoded logo/colors
- Không có cơ chế fetch tenant branding khi compose email
- Không có event listener `branding.updated` trong kitehub-email

## Proposed Fix

### 1. Branding-aware Email Templates

Convert static templates → Thymeleaf với variables:

```html
<!-- Before -->
<img src="{{ default_logo_url }}" />
<h1 style="color: #2563eb">Welcome</h1>

<!-- After -->
<img src="${tenant.branding.logoUrl}" />
<h1 style="color: ${tenant.branding.primaryColor}">Welcome</h1>
```

### 2. Event-driven Cache Invalidation

```java
// kitehub-email service
@RabbitListener(queues = "kite.branding.updated")
public void onBrandingUpdated(BrandingUpdatedEvent e) {
  // Invalidate cached branding for tenant
  brandingCache.evict(e.tenantId);
}
```

### 3. Email Compose Flow

```java
@Service
public class EmailService {
  public void send(String tenantId, EmailTemplate template, Map<String, Object> vars) {
    // 1. Fetch tenant branding (cached)
    BrandingPackage branding = brandingClient.getPackage(tenantId);

    // 2. Merge branding into template vars
    vars.put("branding", branding.getTheme());
    vars.put("tenant", tenantContext);

    // 3. Render + send
    emailSender.send(render(template, vars));
  }
}
```

### 4. Other Services to Update

| Service | What uses branding |
|---------|-------------------|
| kitehub-email | All transactional emails |
| kitehub-admin | Admin dashboard for tenant |
| kiteclass-core | Invoice PDFs, notifications, SMS |
| kitehub-subscription | Payment confirmation pages |

Each must:
1. Fetch branding from kitehub-branding API
2. Listen to `branding.updated` event → invalidate cache
3. Use branding in all user-facing output

### 5. Fallback

Nếu fetch branding fail (service down):
- Use cached version (stale-while-revalidate)
- Fallback to default KiteClass branding
- Log warning for ops

## Acceptance Criteria

- [ ] All email templates converted to use `${tenant.branding.*}` variables
- [ ] `BrandingClient` service in kitehub-email + kitehub-subscription + kiteclass-core
- [ ] Event listener `branding.updated` in all 4 services
- [ ] Cache with TTL 5 min + event-driven invalidation
- [ ] Integration test: update branding → send email → email uses new branding
- [ ] Fallback: branding service down → email still sends with default

## Dependencies

- GAP-010 (package API) — source of truth
- GAP-009 (lifecycle) — event publisher

## Log

- 2026-04-14 — Broken promise scenario identified qua simulation
