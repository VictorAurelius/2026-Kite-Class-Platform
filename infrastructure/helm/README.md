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

### Alertmanager production receivers (GAP-144 / ADR-022)

> **TL;DR — production receivers are opt-in via
> `monitoring.alertmanager.receivers.production.enabled=true`. Foundation install
> ships placeholder receivers so dev / local clusters keep working without AWS.**

`values.yaml -> monitoring.kube-prometheus-stack.alertmanager.config` defines
THREE receivers in two modes:

| Mode | Receiver | Backend |
|------|----------|---------|
| Placeholder (default) | `default-webhook` | `http://alertmanager-webhook-placeholder.invalid/default` — alerts silently drop |
| Placeholder (default) | `critical-webhook` | `http://alertmanager-webhook-placeholder.invalid/critical` — alerts silently drop |
| Placeholder (default) | `warning-email` | `smtp.placeholder.invalid:587` to `ops@kitehub.me` — alerts silently drop |
| Production (opt-in) | `default-webhook` | Slack via `slack_configs.api_url_file` — webhook URL from AWS SM `kitehub/<env>/alertmanager/slack-webhook` |
| Production (opt-in) | `critical-webhook` | PagerDuty via `pagerduty_configs.service_key_file` — routing key from AWS SM `kitehub/<env>/alertmanager/pagerduty-key` |
| Production (opt-in) | `warning-email` | AWS SES via `email-smtp.<region>.amazonaws.com:587` — SMTP password from AWS SM `kitehub/<env>/alertmanager/smtp-password` |

#### Secret strategy — External Secrets Operator + AWS Secrets Manager

Per [ADR-022](../../documents/02-architecture/adr/ADR-022-alertmanager-secret-strategy.md):

- **Backend:** AWS Secrets Manager (matches existing `infrastructure/terraform-aws/secrets.tf` pattern; `secret_arns` output line 48-56 was already labelled "for External Secrets Operator")
- **Sync:** ESO `ExternalSecret` resource (`templates/alertmanager-external-secret.yaml`) reconciles every 1h, materializes a k8s `Secret` named `alertmanager-receivers` with three keys
- **Mount:** Alertmanager pod mounts the Secret at `/etc/alertmanager/secrets/alertmanager-receivers/` (volume only attaches when `production.enabled=true`)
- **Reference:** Alertmanager config uses `*_file:` directives — credentials never appear in env vars, command lines, or `kubectl describe`

Alternatives considered + rejected (sealed-secrets, raw values, Vault) — see ADR-022 §Alternatives Considered.

#### Activate production receivers (per environment)

One-time setup (per cluster):

```bash
# 1. Install External Secrets Operator (idempotent)
helm repo add external-secrets https://charts.external-secrets.io
helm install external-secrets external-secrets/external-secrets \
  -n external-secrets --create-namespace

# 2. Create a ClusterSecretStore wired to AWS SM in the target region.
#    Use IRSA (recommended) or static IAM creds. Example (IRSA):
cat <<EOF | kubectl apply -f -
apiVersion: external-secrets.io/v1beta1
kind: ClusterSecretStore
metadata:
  name: aws-secrets-manager
spec:
  provider:
    aws:
      service: SecretsManager
      region: ap-southeast-1
      auth:
        jwt:
          serviceAccountRef:
            name: external-secrets
            namespace: external-secrets
EOF

# 3. Provision three AWS SM secrets (separate Terraform PR — values populated
#    via AWS Console / aws-cli, NEVER committed):
aws secretsmanager create-secret \
  --name kitehub/prod/alertmanager/slack-webhook \
  --secret-string "https://hooks.slack.com/services/REDACTED"
aws secretsmanager create-secret \
  --name kitehub/prod/alertmanager/pagerduty-key \
  --secret-string "REDACTED-32-char-routing-key"
aws secretsmanager create-secret \
  --name kitehub/prod/alertmanager/smtp-password \
  --secret-string "REDACTED-SES-SMTP-password"
```

Per release:

```bash
helm upgrade --install kitehub infrastructure/helm/kitehub/ \
  --namespace kitehub --create-namespace \
  --set monitoring.enabled=true \
  --set monitoring.alertmanager.receivers.production.enabled=true \
  --set monitoring.alertmanager.receivers.production.environment=prod \
  --set monitoring.alertmanager.smtp.region=ap-southeast-1 \
  --set monitoring.alertmanager.receivers.production.slack.channel='#kitehub-alerts'
```

Verify ESO sync:

```bash
kubectl describe externalsecret alertmanager-receivers -n kitehub
# Expect: Status: Synced, Refresh Time: <recent>

kubectl get secret alertmanager-receivers -n kitehub -o jsonpath='{.data}' | jq 'keys'
# Expect: ["pagerduty-routing-key", "slack-webhook-url", "smtp-password"]
```

> ⚠️ Do NOT decode the Secret values in scripts or CI logs. Use
> `kubectl exec` + `cat /etc/alertmanager/secrets/alertmanager-receivers/...`
> from inside the Alertmanager pod for live debugging only.

### Override per environment (non-secret values)

Create `values-prod.yaml` next to the chart and pass with `-f`. Use this for
environment-specific routing (Slack channel, email recipient, SMTP region) —
NEVER for credentials:

```yaml
# infrastructure/helm/kitehub/values-prod.yaml (gitignored — environment overrides)
monitoring:
  enabled: true
  alertmanager:
    receivers:
      production:
        enabled: true
        environment: prod
        slack:
          channel: "#kitehub-prod-alerts"
        email:
          to: "ops-prod@kitehub.me"
    smtp:
      region: ap-southeast-1
```

### Testing Alertmanager Receivers (mock-fire alerts)

After production opt-in, verify each receiver works end-to-end with `amtool`
(Alertmanager's CLI). Total time: <2 min Slack, <2 min PagerDuty, <5 min email.

```bash
# 1. Port-forward Alertmanager so amtool can target it locally
kubectl port-forward -n kitehub svc/kitehub-monitoring-alertmanager 9093:9093 &
PF_PID=$!

# 2. Mock Slack alert (severity=warning routes to default-webhook → Slack)
amtool alert add \
  alertname=TestSlackDelivery \
  severity=warning \
  job=mock-test \
  --annotation=summary="Mock Slack alert from amtool" \
  --annotation=description="If you see this in Slack, default-webhook is healthy" \
  --annotation=runbook_url="https://example.com/runbooks/mock" \
  --alertmanager.url=http://localhost:9093 \
  --start="$(date -u +%Y-%m-%dT%H:%M:%SZ)"
# Expect: Slack message in #kitehub-alerts within ~2 min (group_wait=30s + propagation)

# 3. Mock critical alert (severity=critical routes to critical-webhook → PagerDuty)
amtool alert add \
  alertname=TestPagerDutyDelivery \
  severity=critical \
  job=mock-test \
  --annotation=summary="Mock PagerDuty incident from amtool" \
  --annotation=description="If you receive a PagerDuty page, critical-webhook is healthy" \
  --alertmanager.url=http://localhost:9093 \
  --start="$(date -u +%Y-%m-%dT%H:%M:%SZ)"
# Expect: PagerDuty incident created within ~2 min; resolves automatically when alert ages out

# 4. Mock warning email (severity=warning is duplicated to warning-email via continue:true on critical-route — verify route config)
#    (Email path tested via the same severity=warning alert from step 2 — also reaches warning-email via continue:false on the warning route.)
#    Expect: email at ops@kitehub.me within ~5 min

# 5. Verify inhibition: ServiceDown should silence HighErrorRate for same job
amtool alert add \
  alertname=ServiceDown \
  severity=critical \
  job=branding \
  --alertmanager.url=http://localhost:9093 \
  --start="$(date -u +%Y-%m-%dT%H:%M:%SZ)"

amtool alert add \
  alertname=HighErrorRate \
  severity=warning \
  job=branding \
  --alertmanager.url=http://localhost:9093 \
  --start="$(date -u +%Y-%m-%dT%H:%M:%SZ)"

# 6. Inspect inhibition outcome
amtool alert query --alertmanager.url=http://localhost:9093
# Expect: ServiceDown shows "active"; HighErrorRate same job shows "suppressed"

# 7. Cleanup
kill $PF_PID
amtool silence add alertname=~"Test.*" --duration=1h --comment="amtool smoke test"
```

If any step fails, inspect with:

```bash
kubectl logs -n kitehub statefulset/alertmanager-kitehub-monitoring-alertmanager -c alertmanager --tail 200
# Common failures:
#   - "no such file or directory" → ESO didn't materialize Secret; check ExternalSecret + ClusterSecretStore status
#   - "401 / forbidden" from Slack → webhook URL wrong in AWS SM (rotate + re-apply)
#   - "EHLO" / "535 Authentication failed" → SMTP password wrong, or SES region mismatch with smtp.region value
```

### Alert rules sync

`templates/prometheusrule.yaml` mirrors
`kitehub/docker/prometheus/alert-rules.yml`. **When changing alert thresholds,
update BOTH files in the same PR** so dev (docker-compose) and prod (Helm) stay
aligned. Drift detection is tracked in GAP-144.
