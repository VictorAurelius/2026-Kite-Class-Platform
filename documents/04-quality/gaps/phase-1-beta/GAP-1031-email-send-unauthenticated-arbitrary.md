# GAP-1031: Arbitrary unauthenticated email send via gateway `/api/platform/emails/**`

**Status:** 🔵 OPEN
**Priority:** 🔴 P0
**Domain:** Backend (gateway + kitehub-email)
**Found:** 2026-06-06 (KH-10 notification/email/feedback/support G1 walk)
**Affects:** `kitehub-gateway` route `platform-email` (application.yml:295-300) + `kitehub-email` EmailController (zero Spring Security)

## Problem

KH-10 G1 walk — headline finding (FM-1). Bất kỳ caller **không xác thực** (anonymous, no JWT) nào cũng gửi được email tùy ý từ hạ tầng email KiteHub:

**Walk evidence (live, gateway :9000):**
```
# Anonymous — KHÔNG có Authorization header
POST http://localhost:9000/api/platform/emails/send
  {"to":"victim@example.com","subject":"...","htmlBody":"<p>...</p>"}
→ HTTP 200 {"status":"SENT","messageId":"smtp-..."}  ← email DELIVERED to MailHog
```

Recipient / subject / HTML body hoàn toàn do caller kiểm soát → **spam relay + phishing/spoof từ domain + SMTP reputation của KiteHub**, resource abuse. Direct-to-service `http://kitehub-email:8080/api/platform/emails/send` cũng `SENT` (email module zero security).

## Root Cause (fully diagnosed — `release-fix-retry-budget.md` §3.5 investigation done)

Lỗ hổng là **hai lớp phòng thủ đều hổng**, cộng hưởng:

1. **Gateway pass-through khi thiếu token** — `JwtAuthenticationGatewayFilter.java:130-133`: khi request KHÔNG có `Authorization` header → `return chain.filter(exchange)` (pass-through), dựa vào *"downstream Spring Security sẽ reject nếu endpoint cần auth"*. Đây là design cho optionally-authed endpoints. `/api/platform/emails` KHÔNG nằm trong `isPublicPath()` whitelist — nhưng pass-through-on-missing-token vẫn cho qua.
2. **kitehub-email có ZERO Spring Security** — `kitehub-email/pom.xml` KHÔNG có `spring-boot-starter-security`; không `SecurityConfig`; `EmailController` không `@PreAuthorize` (chỉ `@Tag("Internal email sending API")`). Nên gateway-assumption *"downstream sẽ reject"* là **SAI** cho riêng service này → không có gì chặn.

So sánh empirical (cùng anon, no token, qua gateway):
| Path | Downstream security | Anon result |
|------|--------------------|-------------|
| `/api/platform/admin/emails/stats` | kitehub-subscription Spring Security | **401** ✅ |
| `/api/v1/notification-preferences` | kitehub-subscription Spring Security | **401** ✅ |
| `/api/platform/emails/send` | kitehub-email **NONE** | **200 SENT** 🔴 |

kitehub-email là service DUY NHẤT không có Spring Security → là chỗ duy nhất pass-through model vỡ.

## Scope — route này có cần expose qua gateway không? KHÔNG.

`/api/platform/emails/send` là **internal service-to-service API**. 3 caller hợp lệ đều dùng **direct docker URL `http://kitehub-email:8080`** (không qua gateway):
- `EmailConsumer.java:52` (RabbitMQ `email.send` queue listener → HTTP)
- `EmailServiceClient.java:925`
- `EmailSenderService.java:57,96`

FE chỉ **định nghĩa** `endpoints.email.send` (`endpoints.ts:69`) nhưng **KHÔNG gọi** ở đâu (dead definition — grep toàn FE zero call site). ⇒ Gateway route `platform-email` (`Path=/api/platform/emails/**`) **không có caller hợp lệ nào** → là pure attack surface.

`application-production.yml` KHÔNG có route email → có thể prod đã không expose (cần verify ở G3 production-parity).

## Proposed Fix

**Option A (recommended — lowest risk):** Xoá gateway route `platform-email` (application.yml:295-300). Internal email tiếp tục chạy qua direct docker `kitehub-email:8080`; external surface đóng. Zero functional impact (đã verify không caller hợp lệ qua gateway).

**Option B (defense-in-depth, pair với A):** Thêm `spring-boot-starter-security` + `SecurityConfig` cho kitehub-email (deny-all external, hoặc require service token / network policy) — để pass-through assumption của gateway không bao giờ vỡ lần nữa cho service này.

**Option C (nếu phải giữ route):** Gateway treat `/api/platform/emails/**` như must-auth path (reject missing-token thay vì pass-through) + require `PLATFORM_ADMIN`. Kém hơn A vì giữ surface.

Khuyến nghị **A + B** trong Wave security-1 (cùng batch GAP-1015/1019/1023/1025 IDOR cluster). Cần gateway rebuild + re-walk các flow phụ thuộc email (welcome/trial/DSAR/feedback) để xác nhận internal email không gãy.

## Acceptance Criteria

- [ ] Anonymous `POST :9000/api/platform/emails/send` (valid body) → **401/404** (không còn 200 SENT)
- [ ] Authenticated non-admin (OWNER) `POST :9000/api/platform/emails/send` → **401/403**
- [ ] Internal email vẫn gửi được: welcome / trial-midpoint / DSAR / feedback-survey path PASS (re-walk MailHog có email)
- [ ] kitehub-email không nhận external request (verify gateway route removed OR service-level deny)
- [ ] G3 production-parity: xác nhận prod gateway không expose `/api/platform/emails`

## Related

- Discovered in: KH-10 G1 walk (Wave flow-kh10), pre-walk FM-1 `documents/04-quality/audits/persona-review/2026-06-06-pre-walk-kh10-notification-email-feedback-support.md`
- Sister P0 security từ G1 walks (batch Wave security-1): GAP-1015 (KH-5 IDOR), GAP-1019 (KH-6 IDOR), GAP-1023 (KH-7 IDOR), GAP-1025 (KH-8 purge)
- Gateway pass-through model: `JwtAuthenticationGatewayFilter.java:130-133` `isPublicPath()` :254
- Same incident-class precedent cited in gateway comment: Wave meta-6 Bug #16 (gateway public-path)
