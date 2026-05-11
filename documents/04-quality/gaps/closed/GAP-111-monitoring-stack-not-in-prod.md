# GAP-111: Monitoring Stack Không Deploy Production

**Status:** 🟢 DONE (foundation — 2026-04-20)
**Priority:** 🔴 P0
**Domain:** DevOps / Monitoring
**Found:** 2026-04-19 (ops-readiness audit — baseline)
**Affects:** All services trong production — monitoring blind spot
**Resolution:** Foundation merged via PR — `kube-prometheus-stack` subchart + ServiceMonitors + PrometheusRule wiring 7 alerts. Grafana dashboards deferred to GAP-143; Loki/Tempo to GAP-145.

## Problem

Prometheus + Grafana chỉ configured trong `kitehub/docker-compose.kitehub.yml` với profile `monitoring` (dev-only). Production Helm charts (`infrastructure/helm/kitehub/`, `infrastructure/helm/kiteclass-instance/`) KHÔNG có Prometheus/Grafana deployment manifests.

Evidence:
- `kitehub/docker/prometheus/prometheus.yml` — 6 scrape jobs, dev-only
- `infrastructure/helm/kitehub/templates/` — chỉ có `deployment.yaml` + `service.yaml`, không có Prometheus/Grafana/ServiceMonitor
- `infrastructure/helm/kitehub/values.yaml` — không có `monitoring:` section

Kết quả: production deploy xong → alerts không fire, dashboards không có data, Grafana không accessible. Vận hành mù.

## Root Cause

Monitoring stack được thiết kế cho local dev (docker-compose) mà không scale up cho k8s production. Thiếu Helm chart cho Prometheus Operator hoặc kube-prometheus-stack.

## Proposed Fix

1. Add `kube-prometheus-stack` dependency vào Helm chart (`infrastructure/helm/kitehub/Chart.yaml`)
2. Create `infrastructure/helm/kitehub/templates/servicemonitor.yaml` cho từng service
3. Update `values.yaml` với monitoring section:
   ```yaml
   monitoring:
     enabled: true
     prometheus:
       retention: 15d
     grafana:
       persistence: 10Gi
       adminPasswordFromSecret: kitehub-grafana-admin
   ```
4. Import existing dashboard JSON (`kitehub-overview.json`) vào Grafana via ConfigMap
5. Update `deploy-production.yml` workflow để deploy monitoring namespace trước khi deploy app

## Acceptance Criteria

- [ ] Prometheus + Grafana chạy trong production k8s
- [ ] ServiceMonitor CRDs scrape 6 services
- [ ] Grafana accessible qua ingress (với auth)
- [ ] `kitehub-overview.json` dashboard load được với production data
- [ ] Alerting rules từ `alert-rules.yml` loaded vào Prometheus production
- [ ] Fix port mismatch: `prometheus.yml` đang ghi `kiteclass-core:8081` nhưng actual port 8080

## Related

- Audit: `documents/04-quality/audits/ops/ops-readiness-audit-2026-04-19.md` §4
- Related: GAP-019 (AI observability — AI-scope)
- Blocks: GAP-120 (Alertmanager setup), GAP-122 (platform alerts)

## Log

- 2026-04-19 — Discovered in ops-readiness baseline audit
- 2026-04-20 — Foundation shipped: `infrastructure/helm/kitehub/Chart.yaml` declares `kube-prometheus-stack@58.7.2` dependency (opt-in via `monitoring.enabled=true`); `templates/servicemonitor.yaml` scrapes 5 backend services; `templates/prometheusrule.yaml` mirrors all 7 alert rules from dev. `helm lint` + `helm template` pass. Follow-up gaps: GAP-143 (Grafana dashboards), GAP-144 (Alertmanager production receivers), GAP-145 (Loki + Tempo).
