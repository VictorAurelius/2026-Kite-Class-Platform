# Logs Format Standard

**Priority:** 🟡 MANDATORY — observability governance (this rule = spec)
**Version:** 1.0.0
**Created:** 2026-04-20
**Last-Reviewed:** 2026-04-28
**Reviewer-Approver:** @nguyenvankiet (starter-kit upstream maintainer)
**Closes:** GAP-175 (standard), drives GAP-114 (implementation) + GAP-116 (PII scrubbing)
**Applies to:** All services (the project 6 services + gateway, the project core + gateway, shared libs), all Python/Node scripts that ship logs

---

## 1. The Rule

> **Every service emits structured JSON logs on stdout with a fixed schema. Required fields are non-negotiable. PII is scrubbed at the logger level, not after the fact. Retention follows hot/warm/cold tiers.**

This rule is the **specification**. The **implementation** (logback-spring.xml, MDC filters, PII scrubber beans, log aggregation pipeline) belongs to GAP-114 / GAP-115 / GAP-116 and lands in Wave 7. Do not implement the JSON encoder here — just conform to this schema when it lands.

---

## 2. Log Schema (JSON, one event per line)

### 2.1 Required fields — every log event MUST have these

| Field | Type | Source | Example |
|-------|------|--------|---------|
| `timestamp` | ISO-8601 UTC with milliseconds | Logger framework | `2026-04-20T14:32:17.481Z` |
| `level` | Enum: `TRACE`/`DEBUG`/`INFO`/`WARN`/`ERROR` | Logger | `INFO` |
| `service` | String, kebab-case, matches container name | App config | `<subscription-service>` |
| `message` | Human-readable; NO string interpolation of PII | Caller | `Subscription renewed` |
| `logger` | Fully-qualified class name | Framework | `com.kite.hub.subscription.RenewService` |
| `thread` | Thread name | Framework | `http-nio-8080-exec-3` |

### 2.2 Contextual fields — required when context exists (MDC)

| Field | When | Example |
|-------|------|---------|
| `tenantId` | Any request bound to a tenant | `tenant-abc123` |
| `traceId` | Any request (inject via gateway) | `4bf92f3577b34da6a3ce929d0e0e4736` |
| `spanId` | Same scope as traceId | `00f067aa0ba902b7` |
| `userId` | Authenticated request, nullable for anonymous | `usr-9f2e8d` or `null` |
| `requestId` | Ingress-assigned; falls back to traceId prefix | `req-2026-abc` |

**Rule:** traceId, spanId, tenantId, userId propagate via SLF4J MDC. Filters on gateway inject traceId; interceptor after auth injects userId + tenantId. Never log these as part of `message` — always as distinct fields.

### 2.3 Optional fields — include where semantically meaningful

| Field | When | Example |
|-------|------|---------|
| `httpMethod` | HTTP handler logs | `POST` |
| `httpPath` | HTTP handler logs | `/api/v1/subscriptions/renew` |
| `httpStatus` | HTTP response logs | `200` |
| `durationMs` | Timed operations | `47` |
| `errorCode` | Domain error | `SUBSCRIPTION_EXPIRED` |
| `errorType` | Exception class | `IllegalStateException` |
| `stack` | ERROR level only, multiline OK | `at com.kite...\n at ...` |
| `event` | Domain event name | `subscription.renewed` |
| `entityId` | Entity the log refers to | `sub-2026-0401` |

### 2.4 Fields BANNED in logs (any level)

- Passwords, API keys, bearer tokens, OTPs, session cookies (scrubber MUST drop or mask)
- Full credit card numbers (PCI — only last 4 allowed, prefixed `****`)
- Raw SSN / CMND / CCCD
- Full base64 of uploaded files / images
- Request/response bodies containing any of the above

---

## 3. PII Scrubbing Rules

PII scrubbing is a **logger filter**, not a caller responsibility. The filter runs before the appender writes to stdout. GAP-116 implements it; this rule defines behavior.

### 3.1 Patterns + masks

| PII | Regex (conceptual) | Masked output |
|-----|--------------------|---------------|
| Email | `[\w._%+-]+@[\w.-]+\.\w{2,}` | `a***@domain.com` (first char + `***@` + domain) |
| Vietnamese phone (10–11 digits starting 0) | `0\d{9,10}` | `09**12***89` (first 2 + `**` + middle 4 truncated + last 2) |
| Credit card (16 digits, Luhn passes) | `\b\d{13,19}\b` | `************1234` |
| CCCD / CMND (9 or 12 digits in ID context) | contextual | `*********` (full mask) |
| JWT bearer | `eyJ[A-Za-z0-9_-]+\.[A-Za-z0-9_-]+\.[A-Za-z0-9_-]+` | `<REDACTED_JWT>` |
| Password keyword | `password=\S+`, `"password":"..."` | `password=<REDACTED>` |
| API key keyword | `(api[_-]?key|secret|token)[=:]\s*\S+` | `<key>=<REDACTED>` |

**Implementation constraint:** scrubber compiles patterns once at startup; hot path cost <100µs per event. If scrub is ambiguous (e.g., 10-digit string in non-phone context), err on side of masking — false positives are cheap, PII leaks are not.

### 3.2 Contextual logging over interpolation

Callers MUST prefer structured args over string building:

```java
// BAD — email ends up in message, may leak if scrubber regex misses
log.info("Welcome email sent to " + user.getEmail());

// GOOD — email is a structured arg, scrubber masks before write
log.info("Welcome email sent", kv("recipient", user.getEmail()));
```

Scrubber still runs for defense-in-depth on `message`, but structured fields are the primary protection.

### 3.3 Scrubber unit-test requirement

Each service inheriting scrubber dependency MUST run a smoke test at startup:
- Emit 1 test event containing sample email + phone + JWT
- Capture stdout
- Assert masked output
- Fail boot if mask missing (catches misconfigured filter early)

---

## 4. Retention Policy

Defined here; enforced by log aggregation infrastructure (GAP-115, Wave 7).

| Tier | Window | Purpose | Storage |
|------|--------|---------|---------|
| **Hot** | 0–7 days | Active debugging, on-call | Elasticsearch / Loki (SSD) |
| **Warm** | 8–30 days | Recent incident RCA, audit trail | Elasticsearch (cold node) or S3/object warm tier |
| **Cold** | 31–180 days | Compliance, legal hold | S3 Glacier / archive tier |
| **Delete** | >180 days | — | Automated purge job |

**Exceptions (longer retention):**
- Security / audit logs (auth success+failure, admin actions, payment events) → **7 years** (per ND-13/2023/NĐ-CP data-retention on financial records)
- Legal hold (active litigation / regulator request) → indefinite until release

**Shorter retention (must purge sooner):**
- DEBUG/TRACE level → **24 hours** (noise, not forensic value)

---

## 5. Level Usage Policy

| Level | When to use | Example |
|-------|-------------|---------|
| `TRACE` | Method entry/exit, loop iterations | disabled in prod by default |
| `DEBUG` | Flow decisions, computed values | enabled in staging / prod-debug tenant |
| `INFO` | Business events, successful ops | `subscription.renewed` |
| `WARN` | Degraded but recoverable | fallback activated, circuit-breaker half-open |
| `ERROR` | Operation failed, user-visible or cascade risk | exception caught in handler |

**BANNED patterns:**
- `System.out.println` / `System.err.println` in main code (ArchUnit test enforces — GAP-114)
- `printStackTrace()` — always via `log.error("...", ex)`
- Multi-line multi-event dumps — one event per `log.info()` call

---

## 6. Reference: logback-spring.xml snippet (illustrative — GAP-114 lands it)

```xml
<!-- logback-spring.xml (draft — DO NOT COPY until GAP-114 delivers shared starter module) -->
<configuration>
  <appender name="JSON_STDOUT" class="ch.qos.logback.core.ConsoleAppender">
    <encoder class="net.logstash.logback.encoder.LogstashEncoder">
      <!-- required fields auto-emitted by logstash-encoder: timestamp, level, logger, thread, message -->
      <customFields>{"service":"${spring.application.name}"}</customFields>
      <includeMdcKeyName>tenantId</includeMdcKeyName>
      <includeMdcKeyName>traceId</includeMdcKeyName>
      <includeMdcKeyName>spanId</includeMdcKeyName>
      <includeMdcKeyName>userId</includeMdcKeyName>
      <includeMdcKeyName>requestId</includeMdcKeyName>
    </encoder>
    <!-- PII scrubber filter registered as Spring bean, applied via composite encoder: see GAP-116 -->
  </appender>

  <root level="INFO">
    <appender-ref ref="JSON_STDOUT"/>
  </root>
</configuration>
```

Planned location after GAP-114: `<backend-shared-module>/src/main/resources/logback-base.xml` (extended per-service via `spring.profiles.active`).

---

## 7. Non-JVM Services

### Python scripts + hooks (`scripts/*.py`, `.claude/hooks/*.py`)
- Use `logging` stdlib with a JSON formatter (e.g., `python-json-logger`)
- Emit same required fields (`timestamp`, `level`, `service`=script name, `message`)
- MDC equivalent: use `logging.LoggerAdapter` for `tenantId`/`traceId` when called in CI context

### Node / TypeScript (frontend SSR, Playwright harness)
- Use `pino` with `{ level, timestamp, service, message, ...bindings }` schema
- traceId propagated via HTTP headers (`traceparent`)

### Shell scripts
- Informational output to stdout acceptable as plain text (humans read it)
- But any script shipped to CI MUST use structured format via `jq -c` or echo-JSON helper if log is consumed downstream
- No PII in shell scripts — use `--quiet` flags on commands that echo tokens

---

## 8. Migration Path (tracks GAP-114 / Wave 7)

| Phase | Scope | Owner |
|-------|-------|-------|
| 1 (now) | This rule published | ✅ GAP-175 closed |
| 2 (Wave 7) | Shared logback base + LogstashEncoder in all JVM services | GAP-114 |
| 3 (Wave 7) | PII scrubber filter + startup smoke test | GAP-116 |
| 4 (Wave 7) | Log aggregation (Loki or Elasticsearch) with retention tiers configured | GAP-115 |
| 5 (Wave 8+) | Non-JVM services conform (Python, Node) | follow-up gap |

---

## 9. Verification Checklist

Reviewers during Wave 7+ PRs MUST verify:

- [ ] Service uses shared logback base (not custom config)
- [ ] `spring.application.name` matches `service` field expected in aggregator
- [ ] Gateway injects `traceId` + `requestId` headers
- [ ] Auth interceptor populates MDC `tenantId` + `userId` post-auth
- [ ] No `System.out.println` in `src/main/java` (ArchUnit test green)
- [ ] Scrubber startup smoke test present and passing
- [ ] Retention documented in service README if non-default

---

## 10. Anti-Patterns

| ❌ Don't | ✅ Do |
|---------|------|
| `log.info("User " + email + " logged in")` | `log.info("User logged in", kv("userEmail", email))` — scrubber masks email |
| Custom per-service log schema | Inherit shared logback base |
| Log JWT / password / API key even at DEBUG | Scrub at source, never emit plaintext secret |
| Log full request body by default | Selective field logging with scrubber |
| Use `printStackTrace()` or `e.toString()` | `log.error("op failed", e)` — encoder renders stack |
| Change log level globally in prod to chase a bug | Use per-tenant / per-logger override endpoint (GAP-114 exposes actuator) |
| Retention "forever" because "logs are cheap" | Follow tier policy; storage cost + GDPR/PII exposure compound |

---

## 11. Related

- Parent violation: `output-review-mandate.md` §4 #6 + §5.9 (Logs Standard)
- Implementation gaps: GAP-114 (JSON logging), GAP-115 (aggregation pipeline), GAP-116 (PII scrubbing)
- Closes: GAP-175 (this rule IS the standard; implementation deferred to Wave 7)
- Meta: this rule is a force-multiplier per `meta-gap-priority.md` §5.1 — one rule × 10+ services × every log event

---

## 12. Log

- **2026-04-28** (v1.0.0 backfill): Frontmatter backfill per GAP-249 — added Version + Last-Reviewed + Reviewer-Approver fields. No content change. Solo-dev PATCH self-approve per `rule-change-process.md` §5.
- **2026-04-20:** Rule created as Wave 8b-D deliverable closing GAP-175. Spec only — logback XML + PII scrubber + aggregator land in Wave 7 via GAP-114/115/116.
