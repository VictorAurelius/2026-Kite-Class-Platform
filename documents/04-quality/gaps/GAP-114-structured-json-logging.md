# GAP-114: Structured JSON Logging + MDC Propagation

**Status:** 🔵 OPEN
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

- [ ] All services output JSON log format
- [ ] MDC fields populated: `tenantId`, `traceId`, `userId`, `service`
- [ ] Gateway propagate tenantId xuyên request chain
- [ ] RabbitMQ messages preserve MDC qua headers
- [ ] `logging-standard.md` doc tạo trong 05-guides
- [ ] Verify: log 1 request → grep tenantId → trace được đủ 3 services

## Related

- Audit: `documents/04-quality/audits/ops/ops-readiness-audit-2026-04-19.md` §5
- Unblocks: GAP-115 (log aggregation), GAP-112 (distributed tracing)
- Related: GAP-116 (PII scrubbing)
- Mandate violation: `output-review-mandate.md` §4 "Logs format VIOLATION"

## Log

- 2026-04-19 — Discovered in ops-readiness baseline audit
