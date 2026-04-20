# Helm Charts for KiteHub Platform

## Charts

### kitehub/ - Platform Services
Deploys all KiteHub platform services: gateway, subscription, branding, admin, email, frontend.

```bash
# Deploy platform
helm install kitehub ./helm/kitehub \
  --namespace kitehub --create-namespace \
  --set global.image.registry=<ECR_REGISTRY> \
  --set global.database.host=<RDS_ENDPOINT> \
  --set global.redis.host=<REDIS_ENDPOINT>

# Upgrade
helm upgrade kitehub ./helm/kitehub \
  --set global.image.tag=v1.2.0

# Rollback
helm rollback kitehub 1
```

### kiteclass-instance/ - Per-Tenant Instance
Deploys a KiteClass instance for a specific customer.

```bash
# Deploy instance
helm install customer1 ./helm/kiteclass-instance \
  --namespace kiteclass-instances --create-namespace \
  --set instanceId=abc12345-uuid \
  --set subdomain=customer1 \
  --set tier=BASIC \
  --set database.url=jdbc:postgresql://rds:5432/kiteclass_abc12345 \
  --set database.username=kiteclass_abc12345_user \
  --set image.registry=<ECR_REGISTRY>

# Scale up tier
helm upgrade customer1 ./helm/kiteclass-instance \
  --set tier=PREMIUM
```

## Resource Quotas by Tier

| Tier | Replicas | CPU | Memory | Rate Limit |
|------|----------|-----|--------|------------|
| FREE | 1 | 250-500m | 512Mi-1Gi | 100 req/min |
| BASIC | 2 | 500m-1 | 1-2Gi | 500 req/min |
| PREMIUM | 2 | 1-2 | 2-4Gi | 2000 req/min |
| ENTERPRISE | 3 | 2-4 | 4-8Gi | 10000 req/min |

## Monitoring (foundation — GAP-111 + GAP-120)

The `kitehub` chart now ships an OPTIONAL monitoring stack via the
`kube-prometheus-stack` subchart. Disabled by default; enable explicitly per
environment.

### What ships in this foundation

| Component | Status |
|-----------|--------|
| Prometheus (CRD-based) | ✅ enabled when `monitoring.enabled=true` |
| Alertmanager + receiver stubs (webhook + email) | ✅ enabled |
| ServiceMonitor for 5 backend services | ✅ enabled |
| PrometheusRule wiring 7 existing alerts | ✅ enabled |
| Grafana dashboards | ⏸️ deferred → GAP-143 |
| Production Slack/PagerDuty receivers + secrets | ⏸️ deferred → GAP-144 |
| Loki / log aggregation / tracing | ⏸️ deferred → GAP-145 |

### Enable monitoring

```bash
# 1. Add the prometheus-community repo (one-time per workstation/CI runner)
helm repo add prometheus-community https://prometheus-community.github.io/helm-charts
helm repo update

# 2. Pull subchart dependencies
helm dependency update infrastructure/helm/kitehub/

# 3. Lint + dry-run before applying
helm lint infrastructure/helm/kitehub/ --set monitoring.enabled=true
helm template infrastructure/helm/kitehub/ --set monitoring.enabled=true | head -200

# 4. Install / upgrade with monitoring on
helm upgrade --install kitehub infrastructure/helm/kitehub/ \
  --namespace kitehub --create-namespace \
  --set monitoring.enabled=true
```

### Receiver configuration (placeholders today)

`values.yaml -> monitoring.kube-prometheus-stack.alertmanager.config` defines
THREE receiver stubs:

- `default-webhook` — replace URL with Slack/Teams webhook
- `critical-webhook` — replace URL with PagerDuty Events API URL
- `warning-email` — overrides SMTP server in prod values file

All point at `*.invalid` placeholder hosts so Alertmanager starts cleanly but
silently drops notifications until real values are wired (tracked in GAP-144).

### Override per environment

Create `values-prod.yaml` next to the chart and pass with `-f`:

```yaml
# infrastructure/helm/kitehub/values-prod.yaml (NOT committed — uses real secrets)
monitoring:
  enabled: true
  kube-prometheus-stack:
    alertmanager:
      config:
        receivers:
          - name: default-webhook
            webhook_configs:
              - url: https://hooks.slack.com/services/T.../B.../...
                send_resolved: true
```

### Alert rules sync

`templates/prometheusrule.yaml` mirrors
`kitehub/docker/prometheus/alert-rules.yml`. **When changing alert thresholds,
update BOTH files in the same PR** so dev (docker-compose) and prod (Helm) stay
aligned. Drift detection is tracked in GAP-144.
