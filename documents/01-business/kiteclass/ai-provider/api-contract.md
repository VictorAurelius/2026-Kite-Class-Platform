# AI Provider — API Contract

> Internal Java SPI (no REST endpoints — AI is infrastructure).

## AIClient (SPI)

```java
public interface AIClient {
    AnalysisResult analyze(AnalysisRequest request) throws AIException;
    GenerationResult generate(GenerationRequest request) throws AIException;
}
```

Implementations:
| Bean name | Profile | Notes |
|-----------|---------|-------|
| `baseAIClient` (MockAIClient) | `!ai-live` (default) | Fixture data, no I/O |
| `baseAIClient` (OllamaAIClient) | `ai-live` | HTTP to Ollama daemon (scaffold) |
| `resilientAIClient` (ResilientAIClient) | always | @Primary — wraps the active baseAIClient |

## Exceptions

```java
AIException              // transient — resilience4j retries
NonRetryableAIException  // permanent — resilience4j ignoreExceptions
```

## DTOs

### AnalysisRequest
```java
AnalysisRequest.builder()
    .logoBytes(byte[])      // optional
    .logoMimeType("image/png")
    .audience("K-12")
    .tone("friendly")
    .context(Map.of("preset", "k12-standard"))
    .build();
```

### AnalysisResult
```java
AnalysisResult(
    palette:         List<String>  // ["#HEX", ...]
    typographyStyle: String        // "serif" | "sans-serif" | "rounded"
    moodTags:        List<String>
    templateOnly:    boolean       // true if fallback fired
);
```

### GenerationRequest
```java
GenerationRequest.builder()
    .prompt(String)          // backend-composed
    .resourceType("BANNER")
    .width(1920).height(600)
    .seed(42L)               // optional
    .providerHints(Map.of())
    .build();
```

### GenerationResult
```java
GenerationResult(
    imageUrl:         String   // or null
    imageBytes:       byte[]   // or null
    mimeType:         String
    templateFallback: boolean  // true if fallback fired
);
```

## Fallback contract

Both methods on `ResilientAIClient` return a domain-safe value instead of throwing when
fallback fires:

```java
AnalysisResult.templateOnly()         // templateOnly=true, empty palette
GenerationResult.templateFallback()   // templateFallback=true, null url/bytes
```

Callers MUST check `isTemplateOnly()` / `isTemplateFallback()` and route through the
TEMPLATE category path (GAP-007 classification chain).

## Metrics (auto-exposed by Resilience4j)

| Metric | Purpose |
|--------|---------|
| `resilience4j_circuitbreaker_state{name="ai"}` | 0=closed, 1=open, 2=half-open |
| `resilience4j_circuitbreaker_calls_total{kind="successful\|failed"}` | Throughput |
| `resilience4j_bulkhead_available_concurrent_calls` | Concurrent slots remaining |
| `resilience4j_retry_calls_total{kind="successful_without_retry\|successful_with_retry\|failed_with_retry"}` | Retry outcome |

Alerts wired in Wave 6 Ops Readiness.

## Log
- 2026-04-14 — Initial contract
