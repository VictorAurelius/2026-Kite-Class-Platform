# Runbook: AI Provider High Failure Rate

**Alert:** `AIProviderHighFailureRate`
**Severity:** warning
**Last updated:** 2026-04-28

## What does this alert mean?

The AI inference path (Ollama local, OpenAI, Bedrock, MiniMax) emitted >10% failure rate over a 5-minute window. Source metric: `kite_ai_request_total{result="failure"} / kite_ai_request_total` exceeds 0.10. Failures here are timeouts, 5xx, schema-mismatch responses, or circuit-breaker open events from the `AIClient` Strategy implementations. This typically degrades **branding generation** (kitehub-branding pipelines stall), and triggers fallback to template/STATIC routing per `ai-branding-guidelines.md` §3 — so end users may not notice immediately, but new tenant provisioning slows and the regenerate-budget for FREE/PRO tiers is consumed without producing assets.

## Note

> Metric `kite_ai_request_total{provider, result}` requires Micrometer counters wrapping each `AIClient` Strategy implementation. If not yet emitted, the alert ships **metric-pending** in `kitehub-platform-alerts` group and will activate when GAP-019 (AI observability) lands.

## Immediate checks (0-5 min)

1. **Identify which provider is failing** — alert label `{{ $labels.provider }}`:
   ```bash
   # Service emitting failures (kitehub-branding:8083)
   kubectl logs -n kitehub deploy/kitehub-branding --tail=300 \
     | grep -E 'AIClient|OllamaClient|OpenAIClient|MinimaxClient|fallback|CircuitBreaker' -A 3
   ```
2. **Check provider health from inside the cluster:**
   ```bash
   # Ollama (local/sidecar)
   curl -fsS http://ollama:11434/api/tags | jq '.models[].name'
   # OpenAI
   curl -fsS https://api.openai.com/v1/models -H "Authorization: Bearer $OPENAI_API_KEY" | jq '.data[0]'
   # Provider statuspage check via outside-in
   curl -sS https://status.openai.com/api/v2/status.json | jq '.status'
   ```
3. **Circuit breaker state** via actuator:
   ```bash
   curl -fsS http://kitehub-branding:8083/actuator/circuitbreakers | jq '.circuitBreakers'
   # Look for "state":"OPEN" — fallback should already be active
   ```
4. **API quota / rate limit** — if OpenAI/MiniMax, check the relevant dashboard or log for `429 Too Many Requests` or `insufficient_quota`.

## Likely causes

- **Provider outage** → upstream is down (statuspage will reflect). **Fix:** Strategy fallback should already activate (`AIClient` interface; OpenAIClient → BedrockClient → templateFallback per `ai-branding-guidelines.md` §3 + `design-patterns.md` §3.5.1). Verify fallback chain by switching provider config: `ai.provider.primary=ollama` → `bedrock`.
- **API key rotated without secret update** → provider returns 401/403, classified as failure. **Fix:** rotate key in K8s secret (`ai-provider-credentials`), restart kitehub-branding deployment.
- **Quota exhausted** → monthly token/request budget hit. **Fix:** raise quota with provider, OR shift load to alternate provider via Strategy config; alert finance owner if cost spike.
- **Local Ollama OOM / model not loaded** → Ollama sidecar restarted and never re-pulled the model. See `feedback_gap006_infra_blocker.md`. **Fix:** `docker exec ollama ollama pull llama3.2` (or configured model), verify with `/api/tags`.
- **Prompt template regression** → recent change broke the JSON schema the model returns; `PlannerService` parsing fails, counted as failure. **Fix:** roll back prompt template; per `ai-branding-guidelines.md` §11.4, AI behavior changes need migration test suite (5 sample outputs, A/B vs baseline).
- **Network egress blocked** → for cloud providers, an egress firewall or service-mesh policy dropped the connection. **Fix:** check `NetworkPolicy` and outbound egress.

## Mitigation

```bash
# 1. Force-switch to fallback provider via runtime config refresh (Spring Cloud Config or actuator)
curl -X POST http://kitehub-branding:8083/actuator/env \
  -H 'Content-Type: application/json' \
  -d '{"name":"ai.provider.primary","value":"bedrock"}'
curl -X POST http://kitehub-branding:8083/actuator/refresh

# 2. If Ollama is the failing provider, restart and verify model
docker restart ollama
docker exec ollama ollama list
# Re-pull if missing:
docker exec ollama ollama pull llama3.2

# 3. Drain in-flight AI jobs to fallback queue (kitehub-branding RabbitMQ ai.generate.* queues)
# Open RabbitMQ admin UI: http://kite-rabbitmq:15672
# Move messages from ai.generate.full_ai → ai.generate.template_fallback if persistent failure

# 4. After provider recovers, half-open the circuit breaker
curl -X POST http://kitehub-branding:8083/actuator/circuitbreakers/aiClient/state \
  -H 'Content-Type: application/json' -d '{"state":"HALF_OPEN"}'
```

After mitigation, monitor failure rate for 15 min. If template fallback rate >40% sustained, file a follow-up gap — prolonged degradation indicates the provider strategy needs re-prioritization (e.g. flip primary from OpenAI to Bedrock until issue root-caused).

## When to escalate

- Failure rate >30% for >15 min AND fallback chain also failing → page AI lead; risk of platform-wide branding outage
- New tenant provisioning stalled (correlate with [`tenant-provisioning-failure.md`](./tenant-provisioning-failure.md))
- Cost anomaly on cloud provider — escalate to finance + AI lead together

## Related

- Alert rule: `kitehub/docker/prometheus/alert-rules.yml` (kitehub-platform-alerts group), `infrastructure/helm/kitehub/templates/prometheusrule.yaml`
- Architecture: `documents/02-architecture/ai-branding-v2-redesign.md`, `.claude/rules/ai-branding-guidelines.md` §3 (Strategy pattern), `.claude/rules/design-patterns.md` §3.6 (Resilience)
- Memory: `feedback_gap006_infra_blocker.md`, `feedback_ai_branding_governance_gap.md`
- Related runbooks: [`tenant-provisioning-failure.md`](./tenant-provisioning-failure.md), [`branding-quality-gate-fail-rate.md`](./branding-quality-gate-fail-rate.md), [`high-error-rate.md`](./high-error-rate.md)
