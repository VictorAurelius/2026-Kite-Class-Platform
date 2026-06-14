---
gap_id: GAP-701
title: KiteClass core EmailService → kitehub-email HTTP integration (cross-product wire)
status: OPEN
priority: P1
domain: Backend
phase: phase-1-beta
completion_pct: 0
filed_date: 2026-05-21
last_updated: 2026-05-21
filed_by: Wave 102.9 session (post-Bucket-E user question on local Resend + KiteClass routing)
---

# GAP-701 — KiteClass core EmailService → kitehub-email HTTP integration

## Problem

`kiteclass-core/EmailService` interface (`kiteclass/kiteclass-core/src/main/java/com/kiteclass/core/common/service/email/EmailService.java`) declare 5 method tenant-facing:

| Method | Use case | BR reference |
|---|---|---|
| `sendSimpleEmail` | Generic plain-text email | — |
| `sendHtmlEmail` | Generic HTML email | — |
| `sendTemplateEmail` | Template-based email (Thymeleaf) | — |
| `sendContactNotification` | Contact form → admin/teacher notification | BR-MKT-003 |
| `sendLeadConfirmation` | Lead confirmation email | BR-MKT-004 |

**Implementation duy nhất:** `LoggingEmailService.java` — chỉ `log.info(...)` ra console, KHÔNG gọi sang kitehub-email service. Javadoc explicitly: *"Useful for development and testing environments where SMTP is not configured. To use real email sending, replace this with SmtpEmailService or ExternalEmailService (SendGrid, AWS SES, etc.)."*

**Hậu quả production:**
- Tenant onboarding tại KiteClass: contact form submission → `log.info` console, KHÔNG email tới admin/teacher → tenant tưởng đã gửi, admin/teacher KHÔNG nhận
- Lead capture từ marketing CMS: lead confirmation email KHÔNG đến → conversion drop silent
- Future scope (Phase 1.5+ marketing email, parent notification): hoàn toàn silent fail

**Hậu quả architecture:**
- `EMAIL_SERVICE_URL: http://kitehub-email:8080` env wired CHỈ cho `kitehub-subscription` service (compose `docker-compose.kitehub.yml:561`) — kiteclass-core KHÔNG có env này
- KHÔNG có HTTP client (RestTemplate/WebClient) trong kiteclass-core gọi sang kitehub-email
- Cross-product email routing path = **architectural gap chưa được file** trước đây

## Root Cause

KiteClass core ship `EmailService` interface + `LoggingEmailService` stub từ pre-Wave-1 era (per javadoc `@since 2.17`). Wave 4 AI Branding + Wave 81 Resend provisioning + Wave 98 ResendEmailService stub đều focus vào KiteHub-side email (admin/owner notification, beta invite, etc.) — KHÔNG có wave nào explicitly wire KiteClass cross-product call.

`audit-to-gap-pipeline.md` §2.7 Decision-Doc Code-Sync miss: marketing CMS gap files (BR-MKT-003/004) reference EmailService nhưng KHÔNG check implementation thực tế = LoggingEmailService stub.

Outside-in benchmark: industry pattern (Stripe, Notion, Linear) — single email service per stack, cross-product gọi qua HTTP/queue. KiteHub đã có kitehub-email microservice, kiteclass-core chỉ cần wire HTTP client.

## Proposed Fix

3-phase implementation:

### Phase 1: HTTP client setup (~2h)

1. Add `EMAIL_SERVICE_URL` env vào kiteclass-core compose block (`docker-compose.kitehub.yml`):
   ```yaml
   kiteclass-core:
     environment:
       EMAIL_SERVICE_URL: http://kitehub-email:8080  # Match subscription pattern
   ```

2. Implement `HttpEmailService extends EmailService` trong `kiteclass-core`:
   - Path: `kiteclass/kiteclass-core/src/main/java/com/kiteclass/core/common/service/email/HttpEmailService.java`
   - `@Service @ConditionalOnProperty(name = "email.provider", havingValue = "http", matchIfMissing = false)`
   - RestTemplate/WebClient POST `${EMAIL_SERVICE_URL}/api/email/send` với payload shape match kitehub-email `EmailController`
   - Circuit breaker + timeout + retry per `design-patterns.md` §3.6 resilience
   - Tenant context propagation: include `X-Tenant-Id` header + `tenantId` body field

3. Add `email.provider` config trong kiteclass-core `application.yml`:
   ```yaml
   email:
     provider: ${EMAIL_PROVIDER:logging}  # logging (dev default) | http (production)
   ```

### Phase 2: Tenant context + branding propagation (~1h)

- HTTP payload include `tenantId` từ `@TenantSecurity` interceptor
- kitehub-email service load tenant branding (per Wave 4 GAP-021 — `kitehub.email.branding.core-base-url` config) để render email với tenant brand
- Verify branding cascade: tenant A contact form → kitehub-email render với tenant A logo + colors

### Phase 3: Production wire + verify (~1h)

- Production override `EMAIL_PROVIDER=http` trong compose production block
- Smoke test: tenant submit contact form → admin email arrival (verify trong Resend dashboard hoặc SES)
- Per `pre-handoff-self-test-completeness.md` §2.3 email-driven flow checklist (a)+(b)+(c): provider delivery confirmed + link in email points to live URL + clicking advances state

## Acceptance Criteria

- [ ] `HttpEmailService.java` shipped với CB + timeout + retry + tenant header propagation
- [ ] `EMAIL_SERVICE_URL` env wired vào kiteclass-core compose block (match subscription pattern)
- [ ] `application.yml` email.provider config added với `logging` default (dev safe) + `http` production override
- [ ] Integration test: `HttpEmailService.sendContactNotification(...)` → kitehub-email service receives correct payload (use WireMock OR Testcontainers per `postgres-specific-type-testcontainers.md` pattern)
- [ ] Smoke test: tenant contact form submission → admin email arrival (Resend dashboard verify OR direct inbox check)
- [ ] Branding cascade: tenant A email rendered với tenant A logo/colors (per Wave 4 GAP-021 propagation)
- [ ] `documents/02-architecture/email-architecture.md` §4 (added via [[GAP-700]]) updated post-implementation
- [ ] BR-MKT-003 + BR-MKT-004 flows verified end-to-end (NOT just logged)
- [ ] LoggingEmailService grandfathered as dev/test default (NOT removed — `@ConditionalOnProperty matchIfMissing=true` for backward compat)

## Wave target

**Recommended: Phase 1.5+ (Wave 105+ estimated)** — không block Phase 1 BETA invite-only cohort vì:
- Phase 1 BETA cohort = 5 invite tenants với close manual onboarding support (email contact form silent gap có thể catch qua other channels Zalo/phone)
- Marketing CMS lead capture (BR-MKT-004) thực sự cần real email khi public paid launch (Phase 1.5)
- Contact form (BR-MKT-003) admin notification ưu tiên tăng khi tenant count >10 (scale beyond manual support)

**Alternative: promote vào Phase 1 BETA nếu:**
- Wave 102.9+ outside-in audit phát hiện tenant onboarding flow phụ thuộc email cross-product
- Beta tenant flag bug "contact form không gửi email" sớm trong post-launch retro

**Effort estimate:** ~4h total (Phase 1: 2h, Phase 2: 1h, Phase 3: 1h)

**Sequencing:**
- Sau [[GAP-700]] (doc refresh) — provides architectural context
- Cùng wave với Marketing CMS production scope (TBD wave reference)
- Có thể chia sub-wave 3 phase nếu wave-pack disjoint paths

## Related

- Rule: `audit-to-gap-pipeline.md` §2.7 Decision-Doc Code-Sync (origin pattern — marketing CMS gaps reference EmailService nhưng không check impl)
- Rule: `design-patterns.md` §3.6 resilience (CB + timeout cho external call)
- Rule: `pre-handoff-self-test-completeness.md` §2.3 email-driven flow checklist
- Rule: `postgres-specific-type-testcontainers.md` (Testcontainers pattern cho integration test)
- Code (current state): `kiteclass/kiteclass-core/src/main/java/com/kiteclass/core/common/service/email/EmailService.java`
- Code (stub): `kiteclass/kiteclass-core/src/main/java/com/kiteclass/core/common/service/email/LoggingEmailService.java`
- Code (target): kitehub-email `EmailController` + `NotificationChannel` (Resend/SES dispatch)
- Code (reference pattern): `kitehub-subscription` EMAIL_SERVICE_URL wiring (`docker-compose.kitehub.yml:561`)
- Cross-product BR: BR-MKT-003 (contact notification) + BR-MKT-004 (lead confirmation)
- [[GAP-700]]: Email Architecture doc refresh (paired sister gap — provides arch context for this implementation)
- ADR-025: Email vendor strategy (Stream A Resend, Stream B SES)
- Wave 4 GAP-021: Tenant branding propagation (kitehub-email reads kiteclass-core branding API)

## Log


- 2026-06-14: phase re-triage — phase-1-beta→phase-1.5-paid (notes 'Phase 1.5+; khong block Phase 1 BETA').
- **2026-05-21 (Wave 102.9 session):** Gap created. Triggered bởi user question "ở local thì có gửi mail bằng resend được không, có routing được cho kiteclass không?" — investigation surfaced silent gap: `kiteclass-core/EmailService` interface có 5 method nhưng impl duy nhất là `LoggingEmailService` (log-only); KHÔNG có HTTP client gọi sang kitehub-email; `EMAIL_SERVICE_URL` env CHỈ wired cho kitehub-subscription. Filed cùng session với sister gap [[GAP-700]] (email-architecture.md doc refresh). Status OPEN; effort ~4h (3 phase: HTTP client + tenant context + production wire); P1 priority cao hơn GAP-700 vì impact tenant-facing flow (BR-MKT-003/004 marketing email silent fail). Recommend wave target Phase 1.5+ (Wave 105+) — không block Phase 1 BETA invite cohort vì close manual support. Outside-in benchmark: industry pattern cross-product email qua HTTP/queue — KiteHub đã có kitehub-email microservice, kiteclass-core chỉ cần wire client.
