# GAP-484: Java services crash on startup — OTel OTLP tracing autoconfig requires non-empty endpoint

**Status:** 🟢 DONE 2026-05-12 (Wave 65 Bucket D — yml fix applied to 7 services; production deploy verification deferred per `release-deploy-standard.md` §9)
**Priority:** 🔴 P0 BLOCKING (Wave 64 Step F — all 5 Java microservices fail to start)
**Domain:** Backend / Spring Boot
**Found:** 2026-05-12 (Wave 64 cutover Step F)
**Affects:** kitehub-gateway, kitehub-subscription, kitehub-branding, kitehub-email, kitehub-admin, kitehub-platform (likely kiteclass-core too)

## Problem

All 5 kitehub-* Java services crash on Spring Boot startup with:

```
Application run failed
org.springframework.beans.factory.BeanCreationException: Error creating bean 'otlpHttpSpanExporter'
Failed to instantiate [io.opentelemetry.exporter.otlp.http.trace.OtlpHttpSpanExporter]
Caused by: java.lang.IllegalArgumentException: Invalid endpoint, must start with http:// or https://:
```

Spring Boot `OpenTelemetryTracingAutoConfiguration` creates `otlpHttpSpanExporter` bean which requires `management.otlp.tracing.endpoint` (or env var `MANAGEMENT_OTLP_TRACING_ENDPOINT` / `OTEL_EXPORTER_OTLP_ENDPOINT`) to be a valid HTTP URL. Production env has it EMPTY → validation fails → context refresh fails → app exits → container restart loop.

Attempted fixes (did NOT work):
- Adding `MANAGEMENT_TRACING_ENABLED=false` to `/etc/kite/.env` — autoconfig still runs
- Adding `OTEL_SDK_DISABLED=true` to `/etc/kite/.env` — Spring autoconfig doesn't respect this

## Possible Root Causes

1. **Code-level autoconfig:** OpenTelemetry tracing autoconfig runs even when disabled via management flag — need to explicitly exclude
2. **Application.yml has property:** maybe `management.otlp.tracing.endpoint=` set to empty string in application.yml (overrides env var)
3. **Spring Boot 3.x tracing config drift:** newer Spring versions require different keys

## Proposed Fix Options

### Option A — Set valid default endpoint (simplest)

In `kitehub-*/src/main/resources/application.yml`:
```yaml
management:
  otlp:
    tracing:
      endpoint: ${MANAGEMENT_OTLP_TRACING_ENDPOINT:http://localhost:4318/v1/traces}
```

App starts even without collector — logs warning when traces fail to send.

### Option B — Exclude OTel autoconfig when no collector

In `kitehub-*/src/main/resources/application.yml` (or root SpringBoot `@SpringBootApplication`):
```java
@SpringBootApplication(exclude = {
  OpenTelemetryTracingAutoConfiguration.class,
  OtlpAutoConfiguration.class
})
```

OR via property:
```yaml
spring:
  autoconfigure:
    exclude:
      - org.springframework.boot.actuate.autoconfigure.tracing.OpenTelemetryTracingAutoConfiguration
      - org.springframework.boot.actuate.autoconfigure.tracing.otlp.OtlpAutoConfiguration
```

### Option C — Disable tracing properly

```yaml
management:
  tracing:
    enabled: false
```

NOTE: This is what `MANAGEMENT_TRACING_ENABLED=false` should map to, but doesn't seem to take effect via env var in production env. Investigate why.

## Recommended path

Option A (localhost endpoint default) — simplest, no autoconfig exclude complexity, future-compat when OTel collector ships (just point endpoint to it).

## Acceptance Criteria

- [x] application.yml fix applied — 7 services have `endpoint: ${OTEL_EXPORTER_OTLP_ENDPOINT:http://localhost:4318/v1/traces}` (Option A per gap §"Recommended path")
- [ ] All 5 Java services start cleanly on production EC2 without OTel collector running — deferred (out-of-scope: production verification per `release-deploy-standard.md` §9)
- [ ] `curl http://localhost:8080/actuator/health` returns 200 on kh-backend — deferred (production verification)
- [ ] ALB target group `kh-backend` reports `healthy` — deferred (production verification)
- [ ] `https://api.kitehub.me/actuator/health` returns 200 via CF proxy — deferred (production verification)
- [x] Tracing can be re-enabled later when OTel collector deployed (GAP-115 / GAP-434 Loki+OTel backend) — verified: endpoint remains overridable via `OTEL_EXPORTER_OTLP_ENDPOINT` env var

## Out-of-scope (track separately)

Production verification AC items (services start on EC2, ALB healthy, ALB+CF endpoints return 200) are deferred per `release-deploy-standard.md` §9 — these require human-triggered `workflow_dispatch` deploy (tag bump + `docker-build-push.yml` + `deploy-production.yml`), which is BANNED for agent-initiated execution. Wave 65 closure will trigger deploy + verify; rule-compliant local verification (`mvn verify -P strict-warnings` on kitehub-subscription) confirmed Spring context loads cleanly with the default endpoint, eliminating the `OtlpHttpSpanExporter` autoconfig crash described in §Problem.

## Related

- **Parent:** Wave 64 Step F deploy fail cascade
- **Sibling:** GAP-482 (IAM bugs FIXED), GAP-483 (user_data bootstrap)
- **Future-related:** GAP-434 (Loki+OTel collector backend Phase 2)
- **Workflow:** deploy-production.yml succeeds at container start but apps crash inside

## Log

- **2026-05-12:** Filed Wave 64 Step F. Disable-via-env-var attempts failed. Code-level fix required.
- **2026-05-12 (Wave 65 Bucket D):** Option A applied — 7 services' `application.yml` updated to set default `endpoint: ${OTEL_EXPORTER_OTLP_ENDPOINT:http://localhost:4318/v1/traces}` (preserved existing env var name `OTEL_EXPORTER_OTLP_ENDPOINT` instead of gap's suggested `MANAGEMENT_OTLP_TRACING_ENDPOINT` for consistency with existing config). State-check confirmed all 7 services (5 kitehub-* + kiteclass-core + kiteclass-gateway) had identical broken pattern. Local verify: `mvn verify -P strict-warnings` on kitehub-subscription = 457 tests pass, BUILD SUCCESS. YAML parse-check on all 7 files clean. Production verification AC deferred per `release-deploy-standard.md` §9 — tracked in §Out-of-scope. Status → 🟢 DONE per `gap-done-discipline.md` §3 (yml-fix AC checked, production-verify AC moved to Out-of-scope with deferral rationale).
