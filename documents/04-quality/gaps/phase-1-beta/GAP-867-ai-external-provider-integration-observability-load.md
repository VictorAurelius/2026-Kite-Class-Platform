---
audience: dev
---

# GAP-867 — External AI provider integration + observability + load verify (spun out from GAP-005 Phase 2)

**Status:** 🟡 PARTIAL (design phase DONE Wave local-doable-8 Bucket D 2026-06-02; implementation + load test execution → follow-up wave)
**Priority:** 🟠 P1 (production scale verify)
**Domain:** AI / Backend / DevOps
**Created:** 2026-06-02 (Wave local-doable-6 sync — re-scope follow-up của GAP-005)
**Affects:** kitehub-branding AI client + Grafana observability stack + load test harness
**Phase:** phase-1-beta

## Problem

GAP-005 Phase 2 originally bundled 4 items: (a) Ollama horizontal scaling, (b) Circuit breaker real-call wiring, (c) Load test 100 concurrent users, (d) Grafana dashboard AI metrics. 2026-06-02 architecture pivot decided AI inference = external API only (Gemini free tier + OpenAI API); item (a) → WONTFIX-superseded. Còn 3 items vẫn cần thực thi nhưng KHÔNG thuộc original Ollama scope của GAP-005 — tách thành GAP-867 để rõ scope + tránh GAP-005 umbrella không close.

## Scope (3 items)

### 1. External AI provider client + Circuit breaker wiring

- File: `kitehub/kitehub-branding/src/main/java/com/kitehub/branding/ai/client/` — `AIClient` interface adapter
- Implementations: `GeminiAIClient` (Gemini free tier API) + `OpenAIClient` (existing OpenAI integration, verify pattern)
- Provider selection: config-driven (`ai.provider` property → `gemini-free | openai`); fail-over fallback nếu primary unavailable
- Wrap real client calls với `@CircuitBreaker(name = "ai-provider", fallbackMethod = "templateFallback")` — uses Resilience4j config đã scaffold từ GAP-005 Phase 1
- Replace any Ollama-specific code paths với neutral domain types per `design-patterns.md` §3.10 Leaky Abstraction (no `OllamaResponse` types in domain layer)

### 2. Load test 100 concurrent users

- Script: `scripts/load/ai-100-concurrent.sh` — fires 100 simultaneous AI generation requests với 30% premium / 40% pro / 30% free tier distribution (per GAP-005 §Capacity Planning)
- Verify SLA targets: premium P95 < 60s, pro P95 < 120s, free P95 < 300s
- Tool: k6 (already in stack per repo conventions, OR fall back to wrk/hey)
- MailHog/MinIO not relevant — AI generation pure inference path
- Report output: `documents/04-quality/audits/performance/2026-XX-XX-ai-load-100-concurrent.md` per `output-review-mandate.md` §3

### 3. Grafana dashboard cho AI queue metrics

- Dashboard JSON: `infrastructure/grafana/dashboards/ai-queue.json`
- Panels:
  - `ai.queue.depth{tier}` time-series per tier
  - `ai.job.wait.time{tier}` p50/p95/p99 histogram quantiles (Prometheus `histogram_quantile`)
  - `ai.job.duration{tier}` p50/p95/p99
  - `ai.job.outcome{tier,outcome}` rate + ratio (success/failure/concurrency_limited/degraded)
  - `ai.circuit_breaker_state{name}` open/closed/half-open
- Alerting: SLA violation alert when `ai.job.wait.time{tier="premium"}` p95 > 60s sustained 5 min

## Acceptance Criteria

- [ ] `AIClient` adapter interface with Gemini + OpenAI implementations
- [ ] `@CircuitBreaker(name="ai-provider")` wraps real-call paths with template fallback
- [ ] Provider config switch verified via `application.yml` → spring profiles tests
- [ ] Load test script ships + reports premium P95 < 60s achieved trên running stack (local OR staging)
- [ ] Performance audit report shipped to `documents/04-quality/audits/performance/`
- [ ] Grafana dashboard JSON committed + verified renders against local Prometheus
- [ ] SLA violation alert rule + AlertManager routing (deferred ok nếu AlertManager not ready)
- [ ] GAP-005 Log entry cross-references GAP-867 ship dates per item

## Related

- **Parent:** GAP-005 (Phase 2 architecture pivot — this gap inherits residual observability + verify scope)
- **Wave reference:** GAP-005a Phase 1 ship (Wave 3, 2026-04-18) — fair-queueing core intact, provider swap orthogonal
- **Pattern:** `design-patterns.md` §3.10 Leaky Abstraction (neutral domain types) + §3.6 Resilience (Circuit Breaker + fallback)
- **Out-of-scope:** Ollama replicas + GPU node pool + K8s HPA (WONTFIX per GAP-005 §Re-scope)

## Log

- **2026-06-02** (PARTIAL — design phase DONE Wave local-doable-8 Bucket D): Shipped design-phase artifacts paired same PR:
  - **`documents/02-architecture/adr/ADR-038-ai-external-provider-strategy.md`** (PROPOSED) — provider selection (Gemini Free Tier primary + OpenAI fallback + Bedrock deferred Phase 2) + `AIClient` provider-agnostic interface design (NotificationChannel-style per `design-patterns.md` §3.10) + config model (`ai.provider.primary` / `ai.provider.fallback` / `ai.circuit-breaker.*`) + PDPL 2023 cross-border data flow compliance posture (privacy policy disclosure + opt-in + DPA signing pre-Phase-1.5)
  - **`documents/02-architecture/ai-external-observability-plan.md`** (PROPOSED) — metrics (call count + latency p50/p95/p99 + token usage + cost USD per-tenant), structured JSON logs (PII-scrubbed via SHA-256 hashes per `logs-format-standard.md`), error classification taxonomy (8 codes), Grafana dashboard panel layout (4 rows × 3-5 panels), Prometheus alert rules outline (4 alerts), k6 load test scenario outline (100 concurrent users, 30% Premium + 40% Pro + 30% Free, SLA pass criteria)
  - **`documents/01-business/kitehub/ai-branding/api-contract.md`** — updated header note documenting provider-agnostic `AIClient` abstraction (cite ADR-038 + observability plan)
  - Design rationale per Wave 6 rescope commit `2826cd2f`: Ollama self-host WONTFIX-superseded; external API only = $0 recurring Phase 1 BETA (Gemini Free Tier covers 5 beta tenants × 3 regen/day projection) + cost-controlled fallback (OpenAI pay-per-use ~$5-20/mo) + no GPU infrastructure cost + faster time-to-market (1 wave AIClient adapter vs 3-4 waves Ollama self-host)
  - **Deferred to follow-up implementation wave** (file separate gap GAP-NNN-ai-external-provider-impl):
    - AC #1 `AIClient` adapter interface + `GeminiAIClient` + `OpenAIClient` implementations
    - AC #2 Circuit Breaker real-call wiring (Resilience4j integration với scaffold từ GAP-005a Phase 1)
    - AC #3 `application.yml` provider config switch + Spring profile tests
    - AC #4 Load test script + execution + SLA verify (k6 100 concurrent users)
    - AC #5 Performance audit report ship → `documents/04-quality/audits/performance/`
    - AC #6 Grafana dashboard JSON authoring + local Prometheus render verify
    - AC #7 SLA violation alert rules + AlertManager routing (depends GAP-144 AlertManager wiring)
  - **No scaffold code this PR** per task spec scope (design phase only)
  - **Completion:** 40% (design phase = ~40% of total scope; implementation + observability wiring + load test execution = remaining 60%)
- **2026-06-02** (OPEN): Filed during Wave local-doable-6 sync to re-scope GAP-005 post architecture pivot to external AI APIs only. Inherits 3 residual AC items từ GAP-005 Phase 2 (Circuit breaker real-call wiring + Load test + Grafana). Phase-1-beta priority — needed before beta tenant invites scale > pilot 5 to validate SLA tiers actually achievable với external API latency/rate-limits.
