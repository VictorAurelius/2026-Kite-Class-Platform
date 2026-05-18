# GAP-019: AI Observability & Cost Monitoring

**Status:** 🔵 OPEN
**Priority:** 🟠 P1
**Domain:** DevOps / Monitoring / AI
**Detected:** 2026-04-14 (simulation)

## Problem

Không có observability stack cho AI branding:

- ❌ Không Grafana dashboards cho AI metrics
- ❌ Không alerting queue depth/worker failures/cost spikes
- ❌ Không per-tenant cost visibility
- ❌ Không SLA compliance tracking (per tier)
- ❌ Không anomaly detection

Vận hành mù → không biết khi nào hệ thống fail hoặc tenant nào lạm dụng.

## Proposed Metrics (Prometheus)

### AI Pipeline Metrics

```
ai_request_total{tier,feature,status}         # counter
ai_request_duration_seconds{tier,feature}     # histogram
ai_queue_depth{tier}                          # gauge
ai_worker_utilization{instance}               # gauge
ai_model_inference_duration{model}            # histogram
ai_moderation_rejections_total{type}          # counter
ai_quality_gate_score{tier}                   # histogram
ai_sla_violations_total{tier}                 # counter
```

### Cost Metrics

```
ai_estimated_cost_usd{tenant,feature}         # counter
ai_compute_units_used{tenant}                 # counter
ai_generations_total{tenant,feature}          # counter
```

### Business Metrics

```
branding_wizard_started_total                 # counter
branding_wizard_completed_total               # counter
branding_wizard_abandoned_total{step}         # counter
branding_regenerations_total{tier,resource}   # counter
```

## Grafana Dashboards

### Dashboard 1: AI Operations
- Request rate per tier
- Error rate
- Queue depth (real-time)
- Worker utilization
- P50/P95/P99 latency

### Dashboard 2: AI Cost
- Cost per tenant (top 20)
- Cost per feature
- Monthly trend
- Budget vs actual

### Dashboard 3: Quality
- Quality score distribution
- Fail rate per template
- Regeneration rate

## Alerting Rules

```yaml
# High queue depth
- alert: AIQueueBacklog
  expr: ai_queue_depth{tier=~"premium|enterprise"} > 50
  for: 5m
  severity: warning

# SLA violation
- alert: AISLABreach
  expr: rate(ai_sla_violations_total[5m]) > 0.1
  severity: critical

# Cost spike
- alert: AICostSpike
  expr: rate(ai_estimated_cost_usd[1h]) > 10  # $10/hr
  severity: warning

# Worker down
- alert: AIWorkerDown
  expr: up{job="kite-ollama"} == 0
  for: 1m
  severity: critical
```

## Acceptance Criteria

- [ ] Prometheus metrics published từ kitehub-branding
- [ ] 3 Grafana dashboards created
- [ ] 5+ alerting rules configured
- [ ] Runbook cho mỗi alert (link GAP-030 disaster recovery)
- [ ] Load test validates dashboards show accurate data
- [ ] On-call rotation setup

## Dependencies

- Prometheus + Grafana đã có trong infrastructure
- GAP-005 (queue) — metrics source

## Log

- 2026-04-14 — Gap qua simulation scaling scenario
