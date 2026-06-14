# GAP-1356: kitehub-branding ResilientAIClient thiếu @Bulkhead

**Status:** 🟢 DONE
**Priority:** 🟠 P1
**Domain:** Backend
**Found:** 2026-06-14 (Performance full audit post wave-p0-closeout-1, sub-check 5.2)
**Affects:** `kitehub/kitehub-branding/src/main/java/com/kitehub/branding/client/ResilientAIClient.java`

## Problem

`kitehub-branding ResilientAIClient` chỉ có `@CircuitBreaker` (lines 58/64/77/82) + `@Retry`, KHÔNG có `@Bulkhead`. `application.yml:158-164` resilience4j có khối `circuitbreaker` nhưng KHÔNG có khối `bulkhead`.

AI image-generation (OpenAI gpt-image-1 / Gemini) là external call CHẬM nhất hệ thống (10-30s/call). Không bulkhead → một burst request branding có thể chiếm hết Tomcat thread pool → cascade fail sang request khác. Circuit breaker chỉ mở SAU khi đã fail (reactive), KHÔNG giới hạn concurrency proactively như bulkhead.

Đối chiếu `kiteclass-core ResilientAIClient` có đủ `@CircuitBreaker + @Bulkhead(maxConcurrentCalls:10) + @Retry` (application.yml:241) — branding lệch chuẩn so với core.

## Proposed Fix

Thêm `@Bulkhead(name="ai-provider")` lên các method AI của branding ResilientAIClient + khối `resilience4j.bulkhead.instances.ai-provider.maxConcurrentCalls` vào branding application.yml (mirror core: maxConcurrentCalls 10, maxWaitDuration 0).

## Acceptance Criteria

- [x] Mỗi method external AI call trong branding ResilientAIClient có `@Bulkhead`
- [x] `resilience4j.bulkhead.instances.<name>` khai báo trong branding application.yml
- [x] grep `@Bulkhead` count khớp số external AI call site trong branding

## Resolution (2026-06-15, branch fix/audit-fixE-perf-2026-06-14)

- Thêm `@Bulkhead(name = CB_NAME)` lên cả 4 method AI call của `ResilientAIClient`
  (`analyzeLogo`, `generateImage`, `generateImageStrict`, `generateText`) — khớp số CB site.
- Thêm khối `resilience4j.bulkhead.instances.ai-provider` (`maxConcurrentCalls: 10`,
  `maxWaitDuration: 0`) vào `kitehub-branding/application.yml`, mirror `kiteclass-core` `ai`.
- Bulkhead là innermost aspect; khi đầy → reject → CB `fallbackMethod` xử lý graceful.
- Test: `ResilientAIClientTest.externalAiMethods_haveBulkhead` (reflection assert annotation).
  Full branding surefire 355 tests PASS.

## Related

- Discovered in: 2026-06-14 performance audit (`performance/2026-06-14-performance-full-audit.md` F-001)
- `.claude/rules/design-patterns.md` §2 (bulkhead mandatory external call)
- Pattern ref: `kiteclass-core ResilientAIClient` (đã đủ resilience)
