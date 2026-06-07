# GAP-1045: tenant.created/tenant.deployed consumer `@RabbitListener(String)` vs Jackson factory → message rejected, saga chết

**Status:** 🟢 DONE
**Priority:** 🔴 P0
**Domain:** Backend
**Found:** 2026-06-07 (KC-1 provisioning-1 saga live walk — G1)
**Affects:** Cross-service tenant-provisioning saga (kitehub-subscription ↔ kiteclass-core); chặn toàn bộ GAP-945/946/947/948/952/953/954

## Problem

Walk KC-1 (beta signup → saga → tenant ready) Bước 1 PASS (HTTP 200, `tenant.created` published) nhưng saga **không bao giờ chạy**: `frontend_instances` absent sau 120s. Log kiteclass-core:

```
Fatal message conversion error; message rejected; it will be dropped ...
receivedExchange=email.exchange receivedRoutingKey=tenant.created consumerQueue=tenant.created.queue
(Body byte[121], contentType=application/json, headers={})
```

**Root cause:** `TenantCreatedEventConsumer` (kiteclass-core) + `TenantDeployedEventConsumer` (kitehub-subscription) dùng `@RabbitListener` method signature `handle(String payloadJson)` + tự `objectMapper.readValue(...)`. Nhưng listener container factory ở CẢ HAI service set `Jackson2JsonMessageConverter` (`RabbitConfig:141` kiteclass + `EmailQueueConfig:98` subscription). Producer (`SubscriptionEventEmitter.emit`) gửi **raw UTF-8 JSON bytes** + `contentType=application/json` qua `rabbitTemplate.send(new Message(...))`. Jackson converter cố map JSON object → `String.class` → `MessageConversionException` → message bị reject + drop (không DLQ) → saga keystone chết câm.

Lớp **GAP-925 wire-format** tái diễn. Proven-working `EmailConsumer` ngược lại dùng **typed param** (`handleEmailEvent(EmailEvent)`) + `convertAndSend` (Jackson 2 chiều) — pattern khác hẳn. 8 gap provisioning-1 đều 🟡 PARTIAL (chưa từng live-walk) nên giấu bug này tới tận walk hôm nay.

## Root Cause

Mismatch giữa producer (raw bytes, content-type json) và consumer signature (String param) dưới Jackson converter. `String` param + `application/json` body → Jackson không deserialize được thành String.

## Proposed Fix (SHIPPED)

Consumer nhận raw `org.springframework.amqp.core.Message` param (bypass converter hoàn toàn) → decode `new String(message.getBody(), UTF_8)` → giữ logic manual-parse hiện có trong method `handlePayload(String)` package-visible (cho unit test). Áp dụng 2 consumer trong KC-1 critical path:
- `kiteclass-core .../provisioning/TenantCreatedEventConsumer` (observed-failing)
- `kitehub-subscription .../consumer/TenantDeployedEventConsumer` (sweep — cùng class, cùng Jackson factory, cùng raw-bytes producer)

## Acceptance Criteria

- [x] Cả 2 consumer nhận `Message` + decode UTF-8 → delegate `handlePayload(String)`
- [x] Unit test guard cho `handle(Message)` decode path (regression) + existing test call `handlePayload(String)`
- [x] Live walk: `tenant.created` consumed không lỗi → `frontend_instances` DEPLOYED → `tenant.deployed` consumed → email + status flip (verified KC-1 walk#3 2026-06-07: frontend_instance id=5 DEPLOYED + TENANT_PROVISIONED audit + tenant-ready email queued)

## Cross-flow sweep evidence (per cross-flow-bug-class-sweep.md §3)

**Bug class signature:** `@RabbitListener` method `handle(String)` + manual objectMapper parse, dưới Jackson2JsonMessageConverter factory, producer gửi raw application/json bytes.

**Grep:** `@RabbitListener` methods với String param trong cả 2 service codebase.

| # | Site | Verdict | Reason |
|---|---|---|---|
| 1 | `kiteclass TenantCreatedEventConsumer:50` | **FIX** | KC-1 critical path; observed-failing |
| 2 | `subscription TenantDeployedEventConsumer:56` | **FIX** | KC-1 critical path; same class/factory/producer |
| 3 | `kiteclass ClassRescheduledNoOpConsumer:39` | **DEFER** | KC-12 flow; verify producer content-type trước khi đổi (tránh đụng code đang chạy) → GAP-1046 |
| 4 | `kiteclass ClassRescheduledEmailConsumer:65` | **DEFER** | KC-12/email flow → GAP-1046 |
| 5 | `kitehub-email ClassRescheduledEmailService:69` | **DEFER** | email service factory có thể khác (SimpleMessageConverter?) → GAP-1046 |
| 6 | `kitehub-email EmailEventListener:97 (onEmailEvent)` | **DEFER** | email service; proven email flow đã chạy → verify converter trước → GAP-1046 |

- Sites FIXED this PR: 2
- Sites DEFERRED to follow-up GAP-1046: 4 (verify producer content-type + factory converter per site; convert nếu cùng broken class)

## Related

- Discovered in: KC-1 provisioning-1 saga live walk 2026-06-07 (session post-Wave-provisioning-1-closure)
- Blocks: GAP-945/946/947/948/952/953/954 DONE-flip (saga must run end-to-end)
- Sister: GAP-925 (wire-format origin) + GAP-1046 (DEFER-sweep 4 remaining raw-String consumers)
