# ADR-006: AI Agent Orchestration (Analyzer → Planner → Executor)

**Status:** ACCEPTED
**Date:** 2026-04-14
**Deciders:** Tech Lead + Architect
**Related Gap:** GAP-008 (Wave 3 Sub-PR 3.5)

## Context

Current `AIBrandingService` calls `AIClient.generate()` directly trong a single synchronous method:
- 2–5 min blocking (unacceptable UX)
- No rollback when partial failure
- Can't inject template-first routing logic (GAP-007)
- Violates `design-patterns.md §3.4` (direct external coupling)

## Decision

Split into 3 layers + pipeline of Steps:

```
AnalyzerService  ─(AnalysisResult)→  PlannerService  ─(Plan)→  PlanExecutor
     │                                      │                        │
  reads context                      orders Steps                runs async
  (tenant, logo,                     applies tier gates          honors fallbacks
   brand signals)                    injects preset                publishes progress
```

**Step interface (Command pattern):**

```java
interface Step {
  String name();
  void execute(StepContext ctx);           // throws StepException
  default boolean hasFallback() { return false; }
  default void fallback(StepContext ctx) { throw new UnsupportedOperationException(); }
}
```

**Plan (Composite pattern):** ordered `List<Step>`; `PlanExecutor` iterates, on `StepException` either calls `fallback()` (if hasFallback) or aborts the saga.

Heavy Steps (image generation) enqueue into RabbitMQ `ai.generate.{tier}` queue; executor returns a jobId; FE polls/SSE.

## Consequences

### Positive
- ✅ Each Step independently testable + fallbackable
- ✅ Pattern alignment: Command + Composite + Saga
- ✅ Async-compatible (steps know their own sync/async nature)
- ✅ Easy to add new Step without touching existing

### Negative
- ❌ More classes (~10 Steps expected)
- ❌ `StepContext` as mutable "god-bag" if not disciplined

## Alternatives

- **A. One big `generate()` method** — keeps current, rejected: violates SRP, not testable, violates §3.4.
- **B. Strategy-per-asset-type (LogoStrategy, BannerStrategy)** — rejected: couples per-asset choices to top-level API; Composite of Steps more flexible.

## Implementation Notes

Steps land in Wave 3 Sub-PR 3.5. Concrete set:
`ExtractPaletteStep`, `PickTemplateStep`, `GenerateLogoStep` (async), `GenerateBannerStep` (async), `ComposeThemeStep`, `PublishPackageStep`.

Saga + compensation per `TenantProvisioningSaga` (Sub-PR 3.6).

## References

- GAP-008, GAP-007 (classification chain feeds Analyzer/Planner)
- design-patterns.md §2, §3.3 (no switch on step type)
- ai-branding-v2-redesign.md §3

## Log

- 2026-04-14 — Accepted
