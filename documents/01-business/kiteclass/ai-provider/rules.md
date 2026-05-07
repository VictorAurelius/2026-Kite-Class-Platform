# AI Provider — Business Rules

**Domain:** ai-provider
**Source:** Wave 3 Sub-PR 3.2, ADR-008

## Rules

### AIClient interface
| ID | Rule |
|----|------|
| BR-AI-001 | Domain code MUST reference `AIClient` interface, never concrete provider types (Adapter pattern) |
| BR-AI-002 | All calls routed through `ResilientAIClient` (primary bean) — Circuit Breaker + Bulkhead + Retry + fallback |
| BR-AI-003 | `AIException` = retryable; `NonRetryableAIException` = permanent — resilience4j honors `ignoreExceptions` |
| BR-AI-004 | Fallback MUST return domain-safe value (`AnalysisResult.templateOnly()` / `GenerationResult.templateFallback()`) — never throw |
| BR-AI-005 | Exactly one `baseAIClient` bean active per environment: `MockAIClient` (default) or `OllamaAIClient` (profile `ai-live`) |
| BR-AI-006 | Prompts composed in backend (ai-branding-guidelines.md §2.3) — no free-form user prompts except Enterprise Advanced Mode |

### Resilience knobs (config key `resilience4j.circuitbreaker.instances.ai`)

| Parameter | Default | Rationale |
|-----------|---------|-----------|
| `slidingWindowSize` | 20 calls | Small enough to react fast, large enough to avoid noise |
| `failureRateThreshold` | 50% | Open circuit when half the recent calls fail |
| `waitDurationInOpenState` | 30s | Give provider time to recover before probe |
| `permittedNumberOfCallsInHalfOpenState` | 3 | 3 probes to verify health |

### Bulkhead (cap concurrent AI calls)

| Parameter | Default |
|-----------|---------|
| `maxConcurrentCalls` | 10 |
| `maxWaitDuration` | 0 (fail-fast, fallback immediately) |

### Retry

| Parameter | Default |
|-----------|---------|
| `maxAttempts` | 3 |
| `waitDuration` | 2s |
| `exponentialBackoffMultiplier` | 2 |
| `retryExceptions` | AIException |
| `ignoreExceptions` | NonRetryableAIException |

## Supported providers (current scaffolding)

| Provider | Profile | Status |
|----------|---------|--------|
| `MockAIClient` | default (no profile) | ✅ ready (tests + sandbox) |
| `OllamaAIClient` | `ai-live` | 🏗️ scaffold only — full HTTP impl in Sub-PR 3.5 |
| `OpenAIClient` | future, enterprise tier | ⏳ not yet scaffolded |
| `BedrockAIClient` | future | ⏳ not yet scaffolded |

## Config keys

| Key | Default | Purpose |
|-----|---------|---------|
| `ai.ollama.base-url` | `http://localhost:11434` | Ollama daemon URL |
| `ai.ollama.default-model` | `gemma2` | Default model for Ollama calls |
| `spring.profiles.active` includes `ai-live` | — | Switches from Mock to Ollama |

## Five-attribute review per `business-logic-review.md`

Per-rule attributes (Source / Rationale / Reviewer / Compliance check / Review cadence) backfilled at file-level placeholder per Phase 1 of GAP-433. Per-rule granularity tracked via GAP-156 Phase 2 stakeholder sign-offs.

- **Source:** Existing rules in this file derive from a mix of: feature gaps cited inline (where present), ADRs, persona reviews, and informed-gut estimates from Wave 1-30 work. Rules without inline citation default to `informed gut` per `business-logic-review.md` §2.1 and inherit quarterly re-review obligation below.
- **Rationale:** Rule values reflect product judgment + (where applicable) competitor benchmarks + VN regulatory minimums. Detailed per-rule rationale to be backfilled during GAP-156 Phase 2 stakeholder review; until then, treat values as `informed gut` subject to next quarterly review.
- **Reviewer:** @nguyenvankiet (acting Product Owner, solo-dev, 2026-05-08). Formal stakeholder + legal counsel sign-off queued via GAP-156. Solo-dev exemption per `business-logic-review.md` §2.3 — the Reviewer line documents which hat is being worn AND obligation is attached for team-growth or pre-launch trigger.
- **Compliance check:** **Considered** — PDPL 2023 Art 23 (data routed to AI provider may include user-generated content); cross-border data transfer if provider hosted outside VN (Decree 53/2022 Art 26).
- **Review cadence:** Quarterly (default per `business-logic-review.md` §2.5). **Next review:** 2026-08-08. Event triggers: Provider swap (e.g., add OpenAI/Bedrock), VN data-localization regulation update.

## Log
- 2026-04-14 — Initial rules (Wave 3 Sub-PR 3.2)
