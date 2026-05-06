# Logging Standard — Operations Guide

**Audience:** Backend developers, SREs, on-call engineers.
**Authoritative spec:** [`.claude/rules/logs-format-standard.md`](../../../.claude/rules/logs-format-standard.md)
**Implementation:** GAP-114 (structured JSON), GAP-116 (PII scrubbing). Aggregation pipeline = GAP-115 (deferred).

This document is the practical, day-to-day guide for emitting and querying logs across KiteHub + KiteClass services. The rules-side document is the contract; this is the runbook.

---

## 1. What every log line includes

Each service emits one JSON event per line on stdout. Required fields (auto-emitted by `LogstashEncoder`):

| Field | Source | Example |
|-------|--------|---------|
| `@timestamp` | logback | `2026-05-06T07:14:32.481Z` |
| `level` | logback | `INFO` |
| `service` | `spring.application.name` | `kitehub-subscription` |
| `message` | caller | `Subscription renewed` |
| `logger_name` | framework | `com.kitehub.subscription.RenewService` |
| `thread_name` | framework | `http-nio-8080-exec-3` |

Contextual MDC fields (populated by `TenantContextFilter` + `RabbitMQTenantInterceptor`):

| Field | When |
|-------|------|
| `tenantId` | request bound to a tenant (header `X-Tenant-Id` or JWT claim) |
| `traceId` | every request — synthesized if absent |
| `spanId` | when the gateway / Sleuth / OTel populates it |
| `userId` | authenticated request — from `X-User-Id` header or `Authentication#getName()` |
| `requestId` | header `X-Request-Id` or derived prefix of `traceId` |

---

## 2. Levels — what to use when

| Level | Use for | Examples |
|-------|---------|----------|
| `TRACE` | Method entry/exit, loop iterations | disabled in prod by default |
| `DEBUG` | Flow decisions, computed values | enabled in staging / prod-debug tenant |
| `INFO` | Business events, successful ops | `subscription.renewed`, `instance.deployed` |
| `WARN` | Degraded but recoverable | fallback activated, circuit-breaker half-open |
| `ERROR` | Operation failed; user-visible or cascade risk | exception caught in handler |

Do not log at `ERROR` for expected business outcomes (validation failure, idempotent retry on a duplicate). Reserve `ERROR` for things on-call should look at.

---

## 3. PII scrubbing — what is masked

Patterns listed here mirror `logs-format-standard.md` §3.1. `PIIScrubber` is the regex layer — `@Redact` on DTO fields is the explicit primary defence.

| Pattern | Mask |
|---------|------|
| Email `alice@kite.com` | `a***@kite.com` |
| VN phone `0987654321` | `09******21` |
| Credit-card-shaped digits | `************1234` (last 4 only) |
| JWT bearer `eyJ...` | `<REDACTED_JWT>` |
| `password=...` / `"password":"..."` | `password=<REDACTED>` |
| `apiKey=...`, `secret=...`, `token=...` | `<key>=<REDACTED>` |
| Contextual `CCCD: 123456789012` | `CCCD=<REDACTED_ID>` |

### Calling code — preferred patterns

```java
// ❌ avoid — interpolation lets the value escape into `message`
log.info("Welcome email sent to " + user.getEmail());

// ✅ structured — PII fields go through @Redact + MDC
log.info("welcome.email.sent", kv("recipient_domain", user.getEmailDomain()));
```

DTOs that travel through error logs — annotate sensitive fields:

```java
public class UserDto {
    private String email;        // OK to log domain only

    @Redact
    private String phoneNumber;  // never serialized

    @Redact
    private String passwordHash;
}
```

---

## 4. Querying logs

Until GAP-115 lands a real aggregator, services log to stdout and Docker collects them.

### Local dev (plain-text profile)

```bash
docker compose logs -f kitehub-subscription | grep tenant=tenant-abc
```

### Staging / production (JSON profile)

```bash
# Find every event for one tenant in the last hour
docker compose logs --since 1h kitehub-subscription \
  | jq -c 'select(.tenantId == "tenant-abc")'

# Trace a single request across services
TRACE=4bf92f3577b34da6a3ce929d0e0e4736
docker compose logs --since 30m \
  | jq -c "select(.traceId == \"$TRACE\")"

# Errors only with stack
docker compose logs --since 24h kiteclass-core \
  | jq -c 'select(.level == "ERROR")'
```

When GAP-115 lands Loki / OpenSearch, the same selectors map to LogQL / KQL.

---

## 5. Local profile

Set `SPRING_PROFILES_ACTIVE=local` (or `dev`) and the appender flips to plain-text with `tenant=` / `trace=` inline. Use this for terminal-piped reads. Production / staging profiles always emit JSON.

---

## 6. Adding new MDC keys

If a service needs a new contextual field beyond the project standard:

1. File a sub-gap citing `.claude/rules/logs-format-standard.md` §2.2 — propose the key name + when it's populated.
2. Extend `TenantContextFilter` (or service-local filter) to populate the key.
3. Add `<includeMdcKeyName>foo</includeMdcKeyName>` to `logback-spring.xml`.
4. Document the key in this file.

Do not add ad-hoc MDC keys silently — every key becomes a Loki / OpenSearch index column long-term.

---

## 7. Retention (high-level)

Authoritative table is in `.claude/rules/logs-format-standard.md` §4. Hot=7d, Warm=30d, Cold=180d. Security/audit logs = 7 years. DEBUG/TRACE = 24h.

Until GAP-115 lands real retention, Docker log driver defaults apply (rotated by service config).

---

## 8. Smoke test (per-service)

Every service inherits `kitehub-platform` (KiteHub) or includes `logstash-logback-encoder` directly (KiteClass). On start-up:

1. The first INFO event MUST be JSON-structured (or plain-text under `local`/`dev` profile).
2. A test event containing `password=hunter2` MUST come out as `password=<REDACTED>` when the `pii` converter is active (kitehub-* services).
3. `tenantId` / `traceId` MDC fields MUST appear when a request enters with the relevant headers.

A unit test exercising (1) + (3) is co-located in `kitehub-platform/src/test/java/com/kitehub/shared/logging/`. See `PIIScrubberTest`, `TenantContextFilterTest`, `RabbitMQTenantInterceptorTest`.

---

## 9. Anti-patterns

- `printStackTrace()` — always `log.error("...", ex)`.
- `System.out.println` in `src/main/java` — use the SLF4J logger.
- Logging entire request / response bodies — selective fields with `@Redact` on the body class.
- Custom per-service log schema — every service inherits the shared `logback-spring.xml`.
- Changing log level globally in prod to chase a bug — use the actuator endpoint per-logger.

---

## 10. Related

- Spec: [`.claude/rules/logs-format-standard.md`](../../../.claude/rules/logs-format-standard.md)
- GAP-114 (this guide closes the implementation half)
- GAP-116 (PII scrubbing)
- GAP-115 (aggregation — deferred)
- Output review: [`.claude/rules/output-review-mandate.md`](../../../.claude/rules/output-review-mandate.md) §3 row "Logs format"
