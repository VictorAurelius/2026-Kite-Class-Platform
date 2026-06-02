---
audience: dev
---

# GAP-867 — External AI provider integration + observability + load verify (spun out from GAP-005 Phase 2)

**Status:** 🔵 OPEN
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

- **2026-06-02** (OPEN): Filed during Wave local-doable-6 sync to re-scope GAP-005 post architecture pivot to external AI APIs only. Inherits 3 residual AC items từ GAP-005 Phase 2 (Circuit breaker real-call wiring + Load test + Grafana). Phase-1-beta priority — needed before beta tenant invites scale > pilot 5 to validate SLA tiers actually achievable với external API latency/rate-limits.
