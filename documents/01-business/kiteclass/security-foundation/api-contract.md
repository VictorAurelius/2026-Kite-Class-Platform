# Security Foundation — API Contract

> Internal Java SPI. No REST endpoints in Sub-PR 4.0; downstream sub-PRs may expose endpoints backed by this foundation.

## AuditLogWriter

### `record(AuditLogEvent) → AuditLog`
Propagation.MANDATORY — caller must already be `@Transactional`.

```java
AuditLog row = auditLog.record(AuditLogEvent.builder()
    .actionType("rebrand.rejected")     // snake-style {domain}.{verb_past}
    .aggregateType("RebrandApproval")
    .aggregateId(String.valueOf(id))
    .actorUserId(approverId)
    .actorRole("ADMIN")
    .reason("off-brand colours")
    .payload(jsonPayload)
    .build());
```

Constants:
- `MAX_PAYLOAD_CHARS = 8000` — silent truncation with ellipsis
- `MAX_REASON_CHARS = 500`

## AuditLogRepository

| Method | Returns |
|--------|---------|
| `findByAggregateTypeAndAggregateIdOrderByCreatedAtDesc(type, id)` | Timeline for an aggregate |
| `countByActionType(type)` | For dashboard metrics |
| Standard JpaRepository | — |

## Security SPI

```java
public interface SvgSanitizer {
    String sanitize(String rawSvg);
}

public interface UrlAllowlistValidator {
    boolean isAllowed(String url, String tenantId);
}

public interface CsrfTokenProvider {
    String issue();
    boolean verify(String token, String cookie);
}
```

Concrete impls: Sub-PR 4.2. Consumers throughout 4.1/4.3/4.4 depend on interfaces only.

## Log
- 2026-04-14 — Initial contract
