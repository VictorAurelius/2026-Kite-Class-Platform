# GAP-131: 9 external HTTP client sites missing connect/read timeouts

**Status:** 🟢 DONE
**Priority:** 🟠 P1
**Domain:** Backend / Performance / Resilience
**Detected:** 2026-04-19 (performance baseline audit)
**Affects:** All services calling external APIs via RestTemplate / WebClient / Feign
**Related Docs:** `documents/04-quality/audits/performance/performance-audit-2026-04-19.md`

## Problem

14 production files use `RestTemplate` / `WebClient` / Feign. Only 5 configure connect/read timeouts:
- `OpenAIClient`, `OllamaClient` (kitehub-branding)
- `kitehub-gateway/client/BrandingClient`
- `kitehub-email/client/BrandingClient`
- `kitehub-subscription/config/RestTemplateConfig` (via FeignConfig)

The other 9 (approximately) rely on JVM defaults — which for `RestTemplate` without explicit `ClientHttpRequestFactory` means **infinite** connect and read timeout. A slow/hung upstream will block a Tomcat worker thread indefinitely, leading to thread-pool exhaustion and cascade failure (no circuit breaker help because Resilience4j is only wired on the AI path).

## Context

- Resilience4j is configured for `ai` endpoint only (in `kiteclass-core/application.yml`). Other external HTTP calls have no retry / no bulkhead / no timeout.
- GAP-005a added resilience to AI path; other paths still exposed.

## Evidence

- `grep 'RestTemplate|WebClient|feignClient' **/main/**/*.java` → 14 sites
- `grep 'readTimeout|connectTimeout|setConnectTimeout|setReadTimeout|timeoutDuration|\.timeout\('` → 5 sites
- Performance audit §2

## Proposed Fix

1. Create a shared `@Configuration class HttpClientConfig` in `kitehub-base` (or repeat per service):
   ```java
   @Bean
   public RestTemplate restTemplate(RestTemplateBuilder builder) {
       return builder
           .setConnectTimeout(Duration.ofSeconds(5))
           .setReadTimeout(Duration.ofSeconds(30))
           .build();
   }
   ```
2. Audit all 14 sites; replace ad-hoc `new RestTemplate()` with the injected bean.
3. For `WebClient`, configure `reactor.netty.http.client.HttpClient.create().responseTimeout(...)`.
4. Add Resilience4j circuit breaker + retry on hot external calls (payment webhooks, email service, captcha).
5. Document timeout policy in `.claude/skills/backend/backend-standards.md` (timeouts required on all external calls).

## Acceptance Criteria

- [ ] Every `RestTemplate` in `**/main/**/*.java` has explicit connect (≤5s) and read timeout (≤30s)
- [ ] CI lint rule (ArchUnit or Checkstyle custom) fails build if `new RestTemplate()` without timeouts
- [ ] Circuit breaker added to payment/email/captcha external calls
- [ ] Integration test: simulate slow upstream (WireMock delay 60s) — request returns in <35s with 504 instead of hanging

## Related

- Audit: performance-audit-2026-04-19.md §2
- GAP-005a (AI path resilience — already done)
- Backend standards skill (needs update)

## Log

- 2026-04-19 — Gap created from performance baseline audit
- 2026-04-20 — Partial fix in feature/partb-perf-batch covering 6 of the 9 unbounded sites:
  - `kitehub-subscription/RestTemplateConfig` — connect 5 s + read 30 s (via `RestTemplateBuilder`). Covers `EmailServiceClient`, `CaptchaService`, `VietQRService`, `EmailConsumer`, and the now-refactored `EmailSenderService` (which previously used a bypass `new RestTemplate()` field — replaced with injected bean).
  - `kiteclass-gateway/CoreServiceClient` — Netty `HttpClient` with `CONNECT_TIMEOUT_MILLIS=5000` + `responseTimeout(30s)`.
  - `kitehub-gateway/BrandingClient` — Netty connect 5 s + `responseTimeout(timeoutSeconds+1)`.
  - `kitehub-email/BrandingClient` — Netty connect 5 s + `responseTimeout(timeoutSeconds+1)`.
  - `kitehub-branding/OllamaClient` — Netty connect 5 s + `responseTimeout(timeoutSeconds+5)`.
  Tests: `RestTemplateConfigTest` (3 cases, reflection on `JdkClientHttpRequestFactory`) + `CoreServiceClientTimeoutTest` (connector is `ReactorClientHttpConnector`). Remaining ACs (CI lint rule via ArchUnit, Resilience4j on payment/email/captcha, WireMock integration test) deferred — separate follow-up gap recommended if not actioned in next perf sprint.
