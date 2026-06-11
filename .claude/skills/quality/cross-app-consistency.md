---
name: cross-app-check
description: "Dùng khi 'check consistency', 'cross-app', 'KiteHub vs KiteClass', 'shared infra'. Verify shared conventions giữa 2 apps."
user-invocable: true
---

# /cross-app-check — Cross-App Consistency

Verify KiteHub ↔ KiteClass shared conventions. Chạy khi PR touches shared infrastructure hoặc trước wave merge.

## Checklist

### 1. Shared Infrastructure Config
- [ ] PostgreSQL: connection string prefix `kite-postgres`, same DB server
- [ ] Redis: key namespace không collision (`kitehub:*` vs `kiteclass:*`)
- [ ] RabbitMQ: exchange/queue names follow convention (`kite.*` shared, `kitehub.*` / `kiteclass.*` service-specific)
- [ ] MinIO: bucket naming consistent

### 2. API Response Format
- [ ] Error response format giống nhau: `{ error, message, status, timestamp }`
- [ ] Pagination format: `{ content, page, size, totalElements, totalPages }`
- [ ] HTTP status codes consistent (400 validation, 404 not found, 409 conflict)

### 3. Authentication & Security
- [ ] JWT claims match: `sub`, `roles`, `tenantId` — cùng format cả 2 apps
- [ ] Token validation logic consistent (expiry, issuer, audience)
- [ ] Gateway routing: `kite-gateway` routes đúng prefix

### 4. Domain Model Alignment
- [ ] `InstanceStatus` enum values match giữa KiteHub (producer) và KiteClass (consumer)
- [ ] `SubscriptionTier` (FREE/BASIC/PREMIUM/ENTERPRISE) consistent
- [ ] Tenant ID format: UUID, truyền qua header `X-Tenant-Id`

### 5. Docker Naming
- [ ] `kite-*` = shared infra only (postgres, redis, rabbitmq, gateway)
- [ ] `kitehub-*` = KiteHub services
- [ ] `kiteclass-*` = KiteClass services
- [ ] No cross-prefix violations

### 6. RabbitMQ Message Contracts
- [ ] Producer message format matches consumer deserialization
- [ ] Dead letter queue configured for both apps
- [ ] Message versioning: breaking changes require new queue

## Gotchas

- KiteHub `application.yml` và KiteClass `application.yml` PHẢI point cùng infra — check `spring.datasource.url`, `spring.rabbitmq.host`
- Enum changes trong 1 app mà không update app kia = runtime `IllegalArgumentException`
- Gateway routes defined in `kite-gateway` — thay đổi path prefix affect cả 2 apps
- Redis key TTL khác nhau OK, nhưng key format phải prefix đúng app
