# ADR-008: Resilience for External AI Calls (Circuit Breaker + Bulkhead + Retry)

**Status:** ACCEPTED
**Date:** 2026-04-14
**Deciders:** Tech Lead + Architect
**Related Gap:** Wave 3 Sub-PR 3.2 (AI Adapter + Resilience)

## Context

Branding pipeline will make external calls to AI providers (Ollama, OpenAI, Bedrock). They fail frequently at scale:
- 5xx from provider
- Rate limits
- Network timeouts
- Cold starts (Ollama model load 10–60s)

Without protection, cascade failures hit the whole branding pipeline and threads pile up waiting on stuck calls.

## Decision

**Three-layer resilience via Resilience4j:**

1. **Retry** (3 attempts, 2s exponential backoff) — handles transient 5xx and network blips
2. **Circuit Breaker** — opens after 50% failure rate in sliding window of 20 calls; half-open after 30s; counts timeouts as failures
3. **Bulkhead** (semaphore) — caps concurrent calls per provider (default 10) to keep other parts of the app responsive

```java
@CircuitBreaker(name = "ai", fallbackMethod = "templateFallback")
@Bulkhead(name = "ai")
@Retry(name = "ai")
public AnalysisResult invoke(AnalysisRequest req) { ... }

private AnalysisResult templateFallback(AnalysisRequest req, Throwable cause) {
  log.warn("AI fallback: {}", cause.getMessage());
  return AnalysisResult.templateOnly();   // Signals routing to TEMPLATE category
}
```

All wrapped in an `AIClient` interface (Adapter pattern, ADR-006 and rule §3.10). Domain code never touches vendor types.

## Consequences

### Positive
- ✅ Failures isolate to AI subsystem; rest of app stays responsive
- ✅ Template-first fallback aligns with `ai-branding-guidelines.md` core principle (80% traffic should not hit AI)
- ✅ Satisfies `design-patterns.md` §3.6 (mandatory resilience on external calls)
- ✅ Metrics (Micrometer) for free — can alert on open circuit

### Negative
- ❌ Added dependency: `resilience4j-spring-boot3`
- ❌ Config tuning required (thresholds, windows)
- ❌ Fallback path must be tested; otherwise dead code

## Alternatives

- **A. Plain try/catch around AIClient calls** — rejected: no circuit protection, no bulkhead, duplicated across call sites.
- **B. Istio/service mesh retries** — rejected: not useful for in-process Ollama calls; adds ops complexity.
- **C. Hystrix** — rejected: in maintenance mode.

## Implementation Notes

### Config

```yaml
resilience4j:
  circuitbreaker:
    instances:
      ai:
        slidingWindowSize: 20
        failureRateThreshold: 50
        waitDurationInOpenState: 30s
        permittedNumberOfCallsInHalfOpenState: 3
  bulkhead:
    instances:
      ai:
        maxConcurrentCalls: 10
        maxWaitDuration: 0
  retry:
    instances:
      ai:
        maxAttempts: 3
        waitDuration: 2s
        exponentialBackoffMultiplier: 2
        retryExceptions:
          - java.io.IOException
          - org.springframework.web.client.RestClientException
        ignoreExceptions:
          - com.kiteclass.core.ai.NonRetryableAIException
```

### Providers

- `OllamaAIClient` — primary (local, cheap)
- `OpenAIAIClient` — premium tier, Enterprise only
- `MockAIClient` — tests + sandbox tenant (GAP-075)

Strategy selected via `ai.provider` property; Adapter isolates vendor types.

### Metrics

Already exposed by Resilience4j:
- `resilience4j_circuitbreaker_state{name="ai"}`
- `resilience4j_circuitbreaker_calls_total{kind="failed|successful"}`
- `resilience4j_bulkhead_available_concurrent_calls`

Wired to Grafana dashboard in Wave 6 Ops.

## References

- Wave 3 Sub-PR 3.2
- design-patterns.md §3.6 (mandatory)
- ai-branding-guidelines.md §3.3 (async heavy tasks)
- resilience4j.readme.io

## Log

- 2026-04-14 — Accepted
