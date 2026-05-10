# Alertmanager Mock-Fire Runbook

**Status:** active
**Created:** 2026-05-11
**Owner:** SRE / on-call engineer
**Scope:** Verifying that Alertmanager production receivers (Slack + PagerDuty + SMTP) actually deliver to their downstream destinations after deploy
**Closes:** Live-cluster mock-fire ACs of GAP-144 (Wave 55 Bucket C)

---

## 1. Purpose

The Alertmanager Helm chart ships with three production receivers (`default-webhook` → Slack, `critical-webhook` → PagerDuty, `warning-email` → SES SMTP) gated by `monitoring.alertmanager.receivers.production.enabled=true`. Credentials are sourced from AWS Secrets Manager via External-Secrets-Operator (ESO) and mounted as files at `/etc/alertmanager/secrets/alertmanager-receivers/`.

This runbook verifies that, after a deploy to a live cluster, each receiver actually reaches its destination. **An Alertmanager that boots cleanly with bad credentials is silent until a real incident.** The mock-fire procedure forces a synthetic alert through each routing path so we discover misconfigurations before production traffic does.

Two recipes are documented:

- **§2 Offline `amtool check-config`** — runs against the rendered Helm output, no live cluster needed. Catches schema errors + reference typos pre-deploy.
- **§3 Online `amtool alert add`** — fires a synthetic alert through the live cluster's Alertmanager, verifies actual delivery to Slack / PagerDuty / SES.

---

## 2. Offline recipe — `amtool check-config`

### 2.1 Render the chart

```bash
cd <repo-root>
helm template infrastructure/helm/kitehub \
  --set monitoring.enabled=true \
  --set monitoring.alertmanager.receivers.production.enabled=true \
  > /tmp/kitehub-rendered.yaml
```

Expected: exit 0, no `Error:` lines on stderr.

### 2.2 Extract the alertmanager config

The alertmanager config is the YAML under `monitoring.kube-prometheus-stack.alertmanager.config:` inside the rendered helm output. Pull it out into a standalone file `amtool` can validate:

```bash
python3 - <<'PY' > /tmp/alertmanager-extracted.yaml
import yaml, sys, pathlib
docs = list(yaml.safe_load_all(pathlib.Path('/tmp/kitehub-rendered.yaml').read_text()))
# Find the values-rendered configmap or the alertmanager config block
for d in docs:
    if not d:
        continue
    if d.get('kind') == 'ConfigMap' and 'alertmanager.yaml' in (d.get('data') or {}):
        print(d['data']['alertmanager.yaml'])
        sys.exit(0)
# Fallback: walk values-style render output for the inline config
for d in docs:
    cfg = (d or {}).get('monitoring', {}).get('kube-prometheus-stack', {}).get('alertmanager', {}).get('config')
    if cfg:
        print(yaml.safe_dump(cfg, sort_keys=False))
        sys.exit(0)
sys.exit("no alertmanager config found in rendered output")
PY
```

### 2.3 Validate with `amtool`

```bash
amtool check-config /tmp/alertmanager-extracted.yaml
```

Expected output:

```
Checking '/tmp/alertmanager-extracted.yaml'  SUCCESS
Found:
 - global config
 - route
 - 0 inhibit rules
 - 3 receivers
 - 0 templates
```

If `amtool` is not installed locally:

```bash
# Linux/amd64
curl -fsSL https://github.com/prometheus/alertmanager/releases/download/v0.27.0/alertmanager-0.27.0.linux-amd64.tar.gz \
  | tar -xz --strip-components=1 -C /tmp alertmanager-0.27.0.linux-amd64/amtool
sudo mv /tmp/amtool /usr/local/bin/
```

### 2.4 Common offline failures

| Error | Cause | Fix |
|---|---|---|
| `unknown field "api_url_file"` | Alertmanager < 0.22 | Bump chart's alertmanager image to ≥ 0.22 |
| `field smtp_smarthost is required` | `monitoring.alertmanager.smtp.smarthost` empty AND region default not rendering | Set `--set monitoring.alertmanager.smtp.smarthost=email-smtp.ap-southeast-1.amazonaws.com:587` |
| `template: ... function "..." not defined` | Helm rendered a template block as data | Re-render using `helm template ...`, not `kubectl apply -f -` |
| `receiver "X" referenced but not defined` | Route → receiver name typo | Fix `route.receiver` to match a `receivers[].name` |

---

## 3. Online recipe — `amtool alert add`

This requires a live cluster with Alertmanager deployed and the `alertmanager-receivers` k8s Secret synced by ESO. Run after every production deploy that touches alertmanager values.

### 3.1 Prerequisite checks

```bash
# 1. ExternalSecret synced — status should be 'SecretSynced'
kubectl -n monitoring get externalsecret alertmanager-receivers \
  -o jsonpath='{.status.conditions[?(@.type=="Ready")].status}'
# expect: True

# 2. k8s Secret materialized with all 5 keys
kubectl -n monitoring get secret alertmanager-receivers \
  -o jsonpath='{.data}' | jq 'keys'
# expect: ["pagerduty-routing-key","slack-webhook-url","smtp-host","smtp-password","smtp-username"]

# 3. Alertmanager pod running + has the secret mounted
kubectl -n monitoring get pod -l app.kubernetes.io/name=alertmanager
kubectl -n monitoring exec alertmanager-kube-prometheus-stack-alertmanager-0 -c alertmanager \
  -- ls /etc/alertmanager/secrets/alertmanager-receivers/
# expect: pagerduty-routing-key  slack-webhook-url  smtp-host  smtp-password  smtp-username
```

### 3.2 Inject smtp-host + smtp-username into values overlay

Alertmanager 0.27 does NOT support `smtp_smarthost_file` / `smtp_auth_username_file`, so these credentials are bound at config-render time from values.yaml, not from the mounted secret directly. Read them out of the materialized secret, then pin them into the per-env values overlay:

```bash
SMTP_HOST=$(kubectl -n monitoring get secret alertmanager-receivers \
  -o jsonpath='{.data.smtp-host}' | base64 -d)
SMTP_USERNAME=$(kubectl -n monitoring get secret alertmanager-receivers \
  -o jsonpath='{.data.smtp-username}' | base64 -d)

# Apply via helm upgrade --set, OR persist in values-<env>.yaml:
helm upgrade --install kitehub infrastructure/helm/kitehub \
  -f values-prod.yaml \
  --set monitoring.alertmanager.smtp.smarthost="$SMTP_HOST" \
  --set monitoring.alertmanager.smtp.authUsername="$SMTP_USERNAME"
```

After upgrade, alertmanager reloads its config (SIGHUP via reloader sidecar) and picks up the new SMTP host/username. The password continues to come from the mounted file.

### 3.3 Port-forward to local for `amtool` access

```bash
kubectl -n monitoring port-forward svc/alertmanager-operated 9093:9093 &
PF_PID=$!
trap "kill $PF_PID" EXIT
```

### 3.4 Fire mock alerts

Three alerts, one per receiver path. Each uses a unique alertname so we can correlate in destinations.

```bash
# Mock #1 — should reach Slack via default-webhook (severity not matched → default route)
amtool --alertmanager.url=http://localhost:9093 alert add \
  alertname=MockFireSlack \
  service=mock-fire-test \
  summary="Mock fire - Slack receiver verification" \
  description="Wave 55 Bucket C verification - if you see this in #alerts, Slack receiver works"

# Mock #2 — should reach PagerDuty via critical-webhook
amtool --alertmanager.url=http://localhost:9093 alert add \
  alertname=MockFirePagerDuty \
  severity=critical \
  service=mock-fire-test \
  summary="Mock fire - PagerDuty receiver verification" \
  description="Wave 55 Bucket C verification - if PD incident opens, PagerDuty receiver works"

# Mock #3 — should reach email via warning-email
amtool --alertmanager.url=http://localhost:9093 alert add \
  alertname=MockFireEmail \
  severity=warning \
  service=mock-fire-test \
  summary="Mock fire - SES receiver verification" \
  description="Wave 55 Bucket C verification - if email arrives at ops@, SES receiver works"
```

### 3.5 Expected outcomes

| Alert | Where to look | Expected within | What to record |
|---|---|---|---|
| `MockFireSlack` | Configured Slack channel (default `#alerts`) | 30 s after `group_wait` (default 30s) | Slack message permalink |
| `MockFirePagerDuty` | PagerDuty incidents page for the integrated service | 1 minute | PagerDuty incident ID |
| `MockFireEmail` | Mailbox of `monitoring.alertmanager.receivers.production.email.to` | 2 minutes (SES delay) | Email Message-ID header |

### 3.6 Resolve the mock alerts

Mock alerts auto-resolve at the `EndsAt` timestamp (default 4h from `Sends`). To clean up immediately:

```bash
amtool --alertmanager.url=http://localhost:9093 alert add \
  alertname=MockFireSlack service=mock-fire-test --end="$(date -u -d '+1min' +%Y-%m-%dT%H:%M:%SZ)"
# Repeat with MockFirePagerDuty + MockFireEmail
```

Or post `EndsAt` in past via the Alertmanager API directly:

```bash
curl -sS -X POST http://localhost:9093/api/v2/alerts -H 'Content-Type: application/json' -d "[
  {\"labels\":{\"alertname\":\"MockFireSlack\",\"service\":\"mock-fire-test\"},\"endsAt\":\"$(date -u +%Y-%m-%dT%H:%M:%SZ)\"}
]"
```

---

## 4. Troubleshooting

### 4.1 ExternalSecret not synced

```bash
kubectl -n monitoring describe externalsecret alertmanager-receivers
```

Common causes:
- ClusterSecretStore `aws-secrets-manager` missing or unhealthy → `kubectl get clustersecretstore aws-secrets-manager`
- IRSA role lacks `secretsmanager:GetSecretValue` on `kitehub/<env>/alertmanager/*` ARN
- AWS SM secret key not yet populated (placeholder `kitehub/prod/alertmanager/slack-webhook` doesn't exist) → populate via `aws secretsmanager create-secret`

### 4.2 Alertmanager fails to start with config error

```bash
kubectl -n monitoring logs alertmanager-kube-prometheus-stack-alertmanager-0 -c alertmanager | head -50
```

Common causes:
- Secret file missing → ESO not synced; see §4.1
- `smtp_smarthost` empty → set per §3.2
- Slack webhook URL malformed → re-check AWS SM secret value (must start with `https://hooks.slack.com/services/`)

### 4.3 Alert fires but no message arrives

Check the alertmanager UI's silence + active alerts pages:

```bash
# Active alerts
curl -sS http://localhost:9093/api/v2/alerts | jq '.[].labels.alertname'

# Silences (an active silence will swallow alerts)
curl -sS http://localhost:9093/api/v2/silences | jq '.[] | select(.status.state=="active")'
```

If alert is active but not delivering, check alertmanager's downstream-error metrics:

```bash
curl -sS http://localhost:9093/metrics | grep -E 'alertmanager_notifications_(total|failed_total)'
# Look for non-zero alertmanager_notifications_failed_total per integration
```

Per-integration debugging:

| Integration | Debug |
|---|---|
| Slack | Webhook URL valid? `curl -X POST <url> -d '{"text":"test"}'` from inside cluster |
| PagerDuty | Routing key matches a service? Verify in PagerDuty UI under "Integrations" → "Events API v2" |
| SMTP | Network egress to SES port 587 open? `kubectl exec -it ... -- nc -vz email-smtp.<region>.amazonaws.com 587`; SES sandbox mode restricts recipients — verify recipient is in verified identities list if account is sandboxed |

### 4.4 Network egress blocked

For private/restricted clusters, alertmanager pod must have:
- DNS resolution for `hooks.slack.com`, `events.pagerduty.com`, `email-smtp.<region>.amazonaws.com`
- Outbound 443 (Slack, PagerDuty)
- Outbound 587 or 465 (SES SMTP)

If NetworkPolicies are in effect, ensure alertmanager namespace has egress to public internet on these ports.

---

## 5. Acceptance — when this runbook closes its scope

Run §2 offline check on every PR that touches `infrastructure/helm/kitehub/values.yaml` or `templates/alertmanager-external-secret.yaml`. Run §3 online check after every production deploy + once weekly as a synthetic-monitoring exercise.

If §3 produces all three expected outcomes (Slack message + PD incident + email arrival), GAP-144 mock-fire ACs are demonstrably satisfied. Record the evidence in `documents/04-quality/audits/observability/<date>-alertmanager-mock-fire.md`.

---

## 6. Related

- Gap: `documents/04-quality/gaps/GAP-144-alertmanager-production-receivers.md`
- ADR: `documents/02-architecture/adr/ADR-022-alertmanager-secret-strategy.md`
- Helm chart: `infrastructure/helm/kitehub/values.yaml` (alertmanager config) + `templates/alertmanager-external-secret.yaml`
- Sister runbooks: `documents/05-guides/operations/runbooks/{service-down,high-error-rate,deployment-procedures}.md`
- Wave plan: `documents/03-planning/waves/wave-2026-05-11-55-observability-validation.md` §3 Bucket C
