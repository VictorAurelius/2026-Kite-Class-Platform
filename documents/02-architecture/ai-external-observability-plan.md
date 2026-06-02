---
audience: mixed
---

# AI External Provider Observability Plan

**Status:** PROPOSED (design phase — paired with ADR-038)
**Created:** 2026-06-02
**Owner:** @nguyenvankiet (solo-dev)
**Related:** ADR-038 (AI external provider strategy) · GAP-867 (integration + observability + load verify) · GAP-005 Phase 1 (fair-scheduling core retained Wave 3)

---

## 1. Mục tiêu

Observability plan cho external AI provider integration (Gemini Free Tier + OpenAI fallback per ADR-038). Plan này định nghĩa:

- **Metrics** cần emit (provider call count, latency p50/p95/p99, token usage, cost tracking)
- **Logs** cần capture (request/response sampling với PII scrub, error classification)
- **Cost tracking** per-tenant per-call + monthly cap alert
- **Load test plan** k6 scenario outline cho production-scale verify

Plan là **design phase**; actual instrumentation code + Grafana JSON authoring + k6 execution = follow-up implementation wave (out of scope this PR).

---

## 2. Metrics

Tất cả metrics export qua **Micrometer** (existing Spring Boot Actuator stack) → Prometheus scrape → Grafana dashboard. Naming convention: `ai.{aspect}.{measure}` snake-case tags.

### 2.1 Call count + outcome

| Metric | Type | Tags | Description |
|---|---|---|---|
| `ai.provider.calls.total` | Counter | `provider, model, outcome, tenant_tier` | Total AI call attempts. `outcome` ∈ `success / failure / rate_limited / circuit_open / fallback_triggered` |
| `ai.provider.fallback.triggered` | Counter | `primary_provider, fallback_provider, reason` | Fallback events. `reason` ∈ `circuit_open / rate_limit / timeout / explicit_config` |
| `ai.provider.circuit_breaker.state` | Gauge | `provider` | Resilience4j Circuit Breaker state (0=closed, 1=half_open, 2=open) |

**Use case:** Track success rate per provider; detect fallback frequency; alert khi Circuit Breaker open sustained.

### 2.2 Latency histograms

| Metric | Type | Tags | Description |
|---|---|---|---|
| `ai.provider.latency.seconds` | Histogram (Timer) | `provider, model, operation, tenant_tier` | End-to-end call latency. `operation` ∈ `generate_text / generate_image / analyze_logo / generate_theme` |
| `ai.provider.queue.wait.seconds` | Histogram | `tenant_tier` | Time spent in RabbitMQ tier queue before processing (existing GAP-005a scaffold) |

Quantiles emit: p50, p95, p99 (Prometheus `histogram_quantile`).

**SLA targets (per GAP-867 §Scope item 2):**
- Premium tier P95 < 60s
- Pro tier P95 < 120s
- Free tier P95 < 300s

### 2.3 Token usage + cost tracking

| Metric | Type | Tags | Description |
|---|---|---|---|
| `ai.provider.tokens.input` | Counter | `provider, model, tenant_id, tenant_tier` | Input tokens consumed per call |
| `ai.provider.tokens.output` | Counter | `provider, model, tenant_id, tenant_tier` | Output tokens generated per call |
| `ai.provider.cost.usd` | Counter | `provider, model, tenant_id, tenant_tier` | Estimated USD cost per call (computed: `tokens × price_per_token` from `ai.{provider}.pricing.*` config) |
| `ai.provider.quota.remaining` | Gauge | `provider, tenant_id, tenant_tier` | Remaining quota (Gemini Free Tier RPM + daily token cap) |

**Cost calculation logic:**

```java
double costUsd = (inputTokens * pricing.inputPricePerThousand / 1000.0)
               + (outputTokens * pricing.outputPricePerThousand / 1000.0);
costMetric.tag("tenant_id", tenantId).tag("provider", providerName).increment(costUsd);
```

**Pricing config (`application.yml`):**

```yaml
ai:
  pricing:
    gemini-1.5-flash:
      input-per-1k-usd: 0.0   # Free Tier
      output-per-1k-usd: 0.0
    gemini-1.5-pro:
      input-per-1k-usd: 0.00125
      output-per-1k-usd: 0.005
    gpt-4o-mini:
      input-per-1k-usd: 0.00015
      output-per-1k-usd: 0.0006
    gpt-image-1:
      per-image-usd: 0.04   # banner generation per ADR-037
```

### 2.4 Per-tenant cost cap alert

| Metric | Threshold | Alert |
|---|---|---|
| Daily per-tenant cost | > $1.00 USD/day | WARN — investigate tenant usage |
| Daily per-tenant cost | > $5.00 USD/day | CRITICAL — auto-throttle tenant + email ops |
| Monthly per-tenant cost | > $20 USD/month | WARN — review tier upgrade eligibility |
| Monthly total cost | > $50 USD/month | WARN — review Phase 1 BETA cost projection |
| Monthly total cost | > $200 USD/month | CRITICAL — auto-disable AI features + investigate |

Alert delivery: AlertManager → SNS topic (per `documents/02-architecture/observability-architecture.md` if exists) → email + Slack (deferred until AlertManager wired per GAP-144).

---

## 3. Logs

Structured JSON logs per `.claude/rules/logs-format-standard.md` (GAP-175 ship). Fields cover audit trail (PDPL Article 11 tamper-proof) + debug context.

### 3.1 Per-call log schema

```json
{
  "timestamp": "2026-06-02T10:00:00.123Z",
  "service": "kitehub-branding",
  "level": "INFO",
  "tenantId": "uuid-xxx",
  "traceId": "trace-yyy",
  "spanId": "span-zzz",
  "event": "ai.provider.call",
  "provider": "gemini",
  "model": "gemini-1.5-flash",
  "operation": "generate_text",
  "tenant_tier": "FREE",
  "outcome": "success",
  "latency_ms": 1234,
  "tokens_input": 150,
  "tokens_output": 320,
  "cost_usd": 0.0,
  "prompt_hash": "sha256:abcd1234...",
  "response_hash": "sha256:efgh5678...",
  "circuit_breaker_state": "closed",
  "request_id": "req-uuid"
}
```

**Critical:** prompt + response NOT stored plaintext per PII concern (per ADR-038 §2.4). SHA-256 hash captured for:
- Audit replay capability (admin can verify hash matches if user re-submits)
- Debugging without exposing PII
- PDPL Article 11 tamper-proof (hash chain immutable in `audit_log` table)

### 3.2 Request/response sampling (debug)

Production default: hash-only (above schema). Debug mode (`ai.logging.debug-sample-rate=0.01` = 1% sample) capture full plaintext for troubleshooting:

```json
{
  ...
  "event": "ai.provider.call.debug_sample",
  "prompt_preview": "Generate hero copy for Trường ABC, target audience học sinh THPT...",
  "response_preview": "Nâng tầm học tập với công nghệ hiện đại..."
}
```

**Sampling rule:** Only sample khi `tenant_tier IN (ENTERPRISE, ADMIN_DEBUG)` AND opt-in flag set (privacy preserved cho FREE/PRO/PREMIUM tenants).

### 3.3 Error classification

| Error code | Trigger | Severity | Action |
|---|---|---|---|
| `AI_RATE_LIMIT_EXCEEDED` | HTTP 429 from provider | WARN | Fallback to secondary provider OR retry after delay |
| `AI_QUOTA_EXHAUSTED` | Free Tier daily cap hit | WARN | Switch to fallback provider; alert if daily usage > expected |
| `AI_TIMEOUT` | Request exceeds `timeout-ms` config | WARN | Increment retry counter; fall back after `max-retries` exhausted |
| `AI_INVALID_RESPONSE` | Provider returns malformed JSON / unparseable | ERROR | Log full response; fall back; file gap if pattern repeats |
| `AI_AUTH_FAILED` | HTTP 401/403 from provider | CRITICAL | API key issue — alert ops immediately; pause AI feature |
| `AI_CONTENT_POLICY_VIOLATION` | Provider rejects prompt for safety reasons | INFO | Log + return user-friendly error; do NOT retry |
| `AI_COST_CAP_EXCEEDED` | Daily/monthly cost cap hit | CRITICAL | Auto-throttle tenant; alert ops |
| `AI_CIRCUIT_BREAKER_OPEN` | Circuit Breaker prevents call | WARN | Fall back to template OR alternative provider |

### 3.4 Audit log integration (PDPL Article 11)

Per `documents/01-business/audit-log-retention.md` — retain 7 năm immutable. AI call events written to `audit_log` table:

```sql
INSERT INTO audit_log (
    event_type,        -- 'AI_PROVIDER_CALL'
    tenant_id,
    user_id,
    timestamp,
    payload_hash,      -- SHA-256 of full request+response
    metadata           -- JSONB {provider, model, outcome, cost_usd, tokens}
) VALUES (...);
```

`payload_hash` chain: each row includes prev row's hash → tamper-evident.

---

## 4. Grafana dashboard

Dashboard JSON path: `infrastructure/grafana/dashboards/ai-external-providers.json` (authoring deferred to follow-up implementation wave).

### 4.1 Panel layout

**Row 1: Service health**
- Panel 1.1: `ai.provider.calls.total{outcome="success"}` rate per provider (line chart)
- Panel 1.2: `ai.provider.calls.total{outcome="failure"}` rate per provider (line chart)
- Panel 1.3: Circuit Breaker state per provider (state timeline — closed/half_open/open)
- Panel 1.4: Fallback trigger rate (counter rate)

**Row 2: Latency SLA**
- Panel 2.1: `ai.provider.latency.seconds` p50/p95/p99 per tier (multi-line)
- Panel 2.2: SLA violation rate — % calls exceeding tier threshold (Premium 60s, Pro 120s, Free 300s)
- Panel 2.3: Queue wait time `ai.provider.queue.wait.seconds` p95 per tier
- Panel 2.4: End-to-end p99 latency heatmap (provider × operation matrix)

**Row 3: Cost + quota**
- Panel 3.1: Daily cost trend per provider (`ai.provider.cost.usd` rate × 86400)
- Panel 3.2: Per-tenant cost top 10 (table — `tenant_id` aggregation)
- Panel 3.3: Token usage per provider (input + output stacked)
- Panel 3.4: Gemini quota remaining (`ai.provider.quota.remaining` gauge)
- Panel 3.5: Monthly cost projection vs budget cap

**Row 4: Error breakdown**
- Panel 4.1: Error rate per error code (stacked area)
- Panel 4.2: Auth/critical errors over time (alert overlay)
- Panel 4.3: Content policy violations rate
- Panel 4.4: Recent error log tail (Loki/CloudWatch query)

### 4.2 Alert rules

Prometheus alerting rules (will be authored in `infrastructure/prometheus/rules/ai-external.yml` follow-up implementation):

```yaml
groups:
- name: ai-external-provider
  rules:
  - alert: AICallSlaPremiumViolation
    expr: histogram_quantile(0.95, sum(rate(ai_provider_latency_seconds_bucket{tenant_tier="PREMIUM"}[5m])) by (le)) > 60
    for: 5m
    labels:
      severity: warning
    annotations:
      summary: "Premium tier AI P95 latency > 60s sustained 5m"

  - alert: AICircuitBreakerOpen
    expr: ai_provider_circuit_breaker_state == 2
    for: 2m
    labels:
      severity: warning
    annotations:
      summary: "AI Circuit Breaker open sustained 2m for {{ $labels.provider }}"

  - alert: AIDailyCostCapExceeded
    expr: sum(increase(ai_provider_cost_usd_total[1d])) > 50
    labels:
      severity: critical
    annotations:
      summary: "Daily AI cost exceeds $50 cap"

  - alert: AIAuthFailed
    expr: rate(ai_provider_calls_total{outcome="failure",reason="auth"}[5m]) > 0
    labels:
      severity: critical
    annotations:
      summary: "AI provider authentication failing - API key issue?"
```

Alert routing: AlertManager → SNS topic → email + Slack (deferred until GAP-144 AlertManager wiring).

---

## 5. Load test plan (k6 scenario outline)

**Scope (per GAP-867 §Scope item 2):** Verify SLA tier targets achievable với external API latency + rate limits at production scale (100 concurrent users).

**Tool:** k6 (already in stack per repo conventions; fall back to `wrk` if k6 unavailable).

**Script path:** `scripts/load/ai-100-concurrent.js` (k6 ES6 syntax) — authoring deferred to follow-up implementation wave.

### 5.1 Scenario outline

```javascript
import http from 'k6/http';
import { check, sleep } from 'k6';
import { Rate, Trend } from 'k6/metrics';

export const options = {
  scenarios: {
    premium_tier: {
      executor: 'constant-vus',
      vus: 30,                       // 30% of 100
      duration: '5m',
      exec: 'premiumGenerate',
    },
    pro_tier: {
      executor: 'constant-vus',
      vus: 40,                       // 40% of 100
      duration: '5m',
      exec: 'proGenerate',
    },
    free_tier: {
      executor: 'constant-vus',
      vus: 30,                       // 30% of 100
      duration: '5m',
      exec: 'freeGenerate',
    },
  },
  thresholds: {
    'http_req_duration{tier:premium}': ['p(95)<60000'],   // 60s
    'http_req_duration{tier:pro}': ['p(95)<120000'],      // 120s
    'http_req_duration{tier:free}': ['p(95)<300000'],     // 300s
    'http_req_failed': ['rate<0.05'],                     // <5% error rate
  },
};

export function premiumGenerate() {
  const res = http.post(`${BASE}/api/v1/branding/jobs/regenerate`, ..., { tags: { tier: 'premium' } });
  check(res, { 'status 200': (r) => r.status === 200 });
  sleep(1);
}
// similar pro, free
```

### 5.2 Tier distribution rationale

Per `documents/01-business/kitehub/ai-branding/rules.md` capacity planning (Wave 6 era):
- Premium: 30% volume (high-value tenants)
- Pro: 40% (majority paid)
- Free: 30% (beta + signup)

### 5.3 Pre-flight environment

- **Target:** Local Docker stack (per `local-self-test-before-aws-deploy.md`) HOẶC AWS staging (preferred for realistic external API latency baseline)
- **Mock provider:** Use real Gemini Free Tier endpoint (Phase 1 BETA realistic) but cap to ≤10 RPM to avoid burning quota
- **Audit log:** Verify per-call audit log written; sample 5 entries to validate schema §3.1

### 5.4 Pass criteria

| Metric | Target |
|---|---|
| Premium P95 latency | < 60s |
| Pro P95 latency | < 120s |
| Free P95 latency | < 300s |
| Error rate | < 5% |
| Circuit Breaker openings | 0 (Gemini Free Tier should handle 100 concurrent if rate-limit per-tenant Redis caps work) |
| Fallback triggers | < 10% (fallback to OpenAI acceptable for spike but not steady-state) |
| Total cost (5min run) | < $1.00 USD (mostly Free Tier; OpenAI fallback minimal) |

### 5.5 Report artifact

Output: `documents/04-quality/audits/performance/YYYY-MM-DD-ai-load-100-concurrent.md` per `output-review-mandate.md` §3 audit report standard. Include:
- Run config (tier distribution, duration, target env)
- Raw k6 output (p50/p95/p99 per tier)
- SLA pass/fail verdict per tier
- Provider call breakdown (Gemini vs OpenAI fallback %)
- Cost incurred
- Recommendations (vd tighten rate limit if fallback triggered too often)

**Execution deferred:** k6 run is implementation phase (separate gap follow-up). This plan = design + outline only.

---

## 6. Implementation roadmap (out-of-scope this PR — informational)

| Phase | Scope | Owner |
|---|---|---|
| **Design (this PR)** | ADR-038 + observability plan + business doc update | Solo-dev — DONE Wave local-doable-8 Bucket D |
| **Implementation Phase 1** | `AIClient` interface + `GeminiAIClient` impl + Micrometer metric registration | Future wave |
| **Implementation Phase 2** | `OpenAIClient` fallback impl + Circuit Breaker wiring + cost cap enforcement | Future wave |
| **Observability wiring** | Grafana dashboard JSON authoring + Prometheus alert rules + AlertManager integration | Future wave (depends GAP-144 AlertManager) |
| **Load test execution** | k6 script + run + audit report | Future wave (post-implementation) |

Estimated total Phase 1+2 implementation effort: 1-2 waves (1 BE bucket + 1 observability bucket).

---

## 7. References

- **ADR-038** — AI external provider strategy (paired PR — provider selection + interface design + config model)
- **GAP-867** — External AI provider integration + observability + load verify
- **GAP-005** — AI queue fair scheduling (Phase 1 fair-scheduling scaffold retained Wave 3 GAP-005a; Phase 2 rescoped Wave 6)
- **GAP-144** — AlertManager receivers (blocker for alert routing wiring)
- **`.claude/rules/logs-format-standard.md`** — structured JSON log schema (GAP-175 ship)
- **`documents/01-business/audit-log-retention.md`** — 7-year immutable retention (PDPL Article 11)
- **`.claude/rules/ai-branding-guidelines.md`** — STATIC/TEMPLATE/FULL_AI taxonomy + quality gate (§5 5-dimension scoring)
- **`.claude/rules/design-patterns.md`** §3.6 Resilience (Circuit Breaker) + §3.10 Leaky Abstraction prevention

---

## 8. Log

- **2026-06-02** (PROPOSED): Observability plan created — paired with ADR-038 (AI external provider strategy). Design phase deliverable for GAP-867 §Scope item 3 (Grafana dashboard) + item 2 (load test plan outline). Actual implementation (Micrometer code + Grafana JSON + k6 script + alert rules) deferred to follow-up implementation wave. PDPL compliance angle integrated (prompt/response hash-only by default; per-call audit_log entry; sampling debug opt-in for ENTERPRISE only). Owner: @nguyenvankiet (solo-dev). Status flip to ACCEPTED khi implementation wave plan filed.
