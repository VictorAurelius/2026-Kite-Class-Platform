# GAP-114: Structured JSON Logging + MDC Propagation

**Status:** 🟡 PARTIAL — infrastructure shipped 2026-05-06 (Wave 25 Bucket B); per-service end-to-end smoke (3-service traceId trace) deferred to GAP-115 deploy + cluster trace verification.
**Priority:** 🔴 P0
**Domain:** Backend / Observability
**Found:** 2026-04-19 (ops-readiness audit — baseline)
**Affects:** 6 KiteHub services + kiteclass-core + kiteclass-gateway

## Problem

Backend services dùng Spring Boot default **TEXT logging** (không structured), thiếu các required fields cho multi-tenant SaaS: `tenantId`, `traceId`, `userId`, `correlationId`, `service`.

Evidence:
- `grep -rn "logback\|log4j" kitehub/*/src kiteclass/*/src --include="*.xml"` → 0 kết quả (không có `logback.xml` nào)
- `application.yml` chỉ có `logging.level.<package>` config
- Logs hiện tại text format: `2026-04-19 10:00:00 INFO c.k.subscription.SomeService - message`

Hậu quả:
- Không thể query logs programmatically (ELK/Loki query fail nếu không JSON)
- Không isolate được lỗi per-tenant (multi-tenant platform)
- Không correlate request xuyên services (không có traceId)
- FERPA/PDPA compliance risk nếu PII leak vào plaintext logs (xem GAP-116)

`output-review-mandate.md` Section 4 liệt kê Logs format = **VIOLATION** — GAP này đóng đúng violation đó.

## Root Cause

Không có project-wide logging standard document. Mỗi service tự dùng default config. Thiếu MDC filter cho HTTP + RabbitMQ boundaries.

## Current State (verified 2026-05-06 — Wave 25 Bucket B)

Infrastructure now in place:
- `kitehub/pom.xml` `<dependencyManagement>` declares `logstash-logback-encoder` 8.0
- `kitehub-platform/pom.xml` consumes the dep; `kiteclass/kiteclass-core/pom.xml` consumes directly
- Shared classes in `kitehub/kitehub-platform/src/main/java/com/kitehub/shared/logging/`:
  - `PIIScrubber` + `PIIScrubberConverter` (regex masks per `logs-format-standard.md` §3.1)
  - `@Redact` + `RedactSerializer` (Jackson-driven primary defence)
  - `TenantContextFilter` (HTTP MDC populator)
  - `RabbitMQTenantInterceptor` (AMQP MDC propagator)
  - `LoggingAutoConfiguration` (Spring Boot auto-config registered via `META-INF/spring/...AutoConfiguration.imports`)
- 8 `logback-spring.xml` files: 6 KiteHub services use the shared `pii` converter; 2 KiteClass services rely on Jackson `@Redact` + structured-arg discipline (PIIScrubber follow-up tracked in `GAP-116-followup-existing-code-pii-audit.md`).
- Operations guide: `documents/05-guides/operations/logging-standard.md`

## Proposed Fix

1. Create `logback-spring.xml` trong `kitehub/kitehub-base/src/main/resources/` (base image) và `kiteclass/kiteclass-core/src/main/resources/`:
   ```xml
   <configuration>
     <appender name="JSON" class="ch.qos.logback.core.ConsoleAppender">
       <encoder class="net.logstash.logback.encoder.LogstashEncoder">
         <includeMdcKeyName>tenantId</includeMdcKeyName>
         <includeMdcKeyName>traceId</includeMdcKeyName>
         <includeMdcKeyName>userId</includeMdcKeyName>
         <includeMdcKeyName>correlationId</includeMdcKeyName>
         <customFields>{"service":"${spring.application.name}"}</customFields>
       </encoder>
     </appender>
     <root level="INFO"><appender-ref ref="JSON"/></root>
   </configuration>
   ```
2. Add dependency `net.logstash.logback:logstash-logback-encoder`
3. Create `TenantContextFilter` + `RabbitMQTenantInterceptor` để populate MDC từ JWT claims + message headers
4. Gateway: extract tenantId từ subdomain / JWT → set MDC → forward qua `X-Tenant-Id` header
5. Document standard trong `documents/05-guides/operations/logging-standard.md`:
   - Required fields
   - Log levels (INFO/WARN/ERROR criteria)
   - PII scrubbing rules (pointer tới GAP-116)

## Acceptance Criteria

- [x] All services output JSON log format (8 logback-spring.xml shipped — kitehub-platform, kitehub-subscription, kitehub-branding, kitehub-email, kitehub-admin, kitehub-gateway, kiteclass-core, kiteclass-gateway)
- [x] MDC fields populated: `tenantId`, `traceId`, `userId`, `service` (`TenantContextFilter` HTTP boundary; `service` via `LogstashEncoder` `customFields`)
- [x] Gateway propagate tenantId xuyên request chain (`TenantContextFilter` echoes `X-Trace-Id` on the response; downstream services read same header)
- [x] RabbitMQ messages preserve MDC qua headers (`RabbitMQTenantInterceptor` outbound + `applyToInbound()` for consumers)
- [x] `logging-standard.md` doc tạo trong 05-guides
- [ ] Verify: log 1 request → grep tenantId → trace được đủ 3 services — deferred to live cluster verification (no 3-service stack up in worktree); cross-link to GAP-115 deploy verification step

## Related

- Audit: `documents/04-quality/audits/ops/ops-readiness-audit-2026-04-19.md` §5
- Unblocks: GAP-115 (log aggregation), GAP-112 (distributed tracing)
- Related: GAP-116 (PII scrubbing) + `GAP-116-followup-existing-code-pii-audit.md`
- Spec: `.claude/rules/logs-format-standard.md`
- Mandate violation: `output-review-mandate.md` §4 "Logs format VIOLATION"

## Log

- 2026-04-19 — Discovered in ops-readiness baseline audit
- **2026-05-06** — Wave 25 Bucket B shipped the JSON logging stack. Status flipped 🔵 OPEN → 🟡 PARTIAL (5 of 6 AC ticked). Live 3-service traceId trace AC remains unchecked because the closing PR is a code change in a worktree without a running 3-service Docker stack; verification rolls into GAP-115 deploy step where the cluster is stood up in earnest. No deferral within a DONE flip per `gap-done-discipline.md` §3 PARTIAL exit ramp.
