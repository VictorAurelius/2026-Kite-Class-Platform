# GAP-120: Alertmanager + Receiver Setup (Slack/PagerDuty)

**Status:** 🔵 OPEN
**Priority:** 🔴 P0
**Domain:** DevOps / Alerting
**Found:** 2026-04-19 (ops-readiness audit — baseline)
**Affects:** On-call response time; alert rules trống vô ích nếu không có receiver

## Problem

`alert-rules.yml` định nghĩa 7 alert rules (ServiceDown, HighErrorRate, HighResponseTime, HighMemoryUsage, DatabasePoolExhausted, HighDiskUsage, RabbitMQQueueBacklog) nhưng **không có Alertmanager config** → alerts không route đi đâu.

Evidence:
- `kitehub/docker/prometheus/prometheus.yml` — không có `alerting:` section
- Không có file `alertmanager.yml`
- Không có Alertmanager service trong docker-compose hoặc Helm
- `incident-response-runbook.md` mention "PagerDuty/Slack alert" nhưng không có config thực sự

Hậu quả: alert condition true → Prometheus mark alert firing → không ai nhận được notification.

## Root Cause

Alert rules được design first, integration để lại sau. Alertmanager chart/config chưa triển khai.

## Proposed Fix

1. Deploy Alertmanager (part of kube-prometheus-stack — depends on GAP-111)
2. Create `alertmanager.yml` config:
   ```yaml
   route:
     group_by: ['alertname', 'severity']
     group_wait: 30s
     group_interval: 5m
     repeat_interval: 12h
     receiver: 'slack-default'
     routes:
       - match: { severity: critical }
         receiver: 'pagerduty-critical'
       - match: { severity: warning }
         receiver: 'slack-warnings'
   receivers:
     - name: 'slack-default'
       slack_configs:
         - api_url_file: /etc/alertmanager/slack-webhook
           channel: '#alerts'
     - name: 'pagerduty-critical'
       pagerduty_configs:
         - service_key_file: /etc/alertmanager/pagerduty-key
     - name: 'slack-warnings'
       slack_configs:
         - channel: '#alerts-warning'
   ```
3. Update Prometheus config → point to Alertmanager
4. Secrets (Slack webhook, PagerDuty service key) inject via k8s Secrets
5. Test each receiver: trigger mock alert → verify delivery
6. Inhibition rules: suppress dependent alerts (vd. ServiceDown suppresses HighErrorRate cùng service)
7. Silencing UI: port-forward Alertmanager UI cho ops team

## Acceptance Criteria

- [ ] Alertmanager deploy vào staging + prod
- [ ] Slack webhook receiver configured + tested
- [ ] PagerDuty receiver cho critical alerts
- [ ] All 7 existing rules route đúng
- [ ] Inhibition rules prevent alert storm
- [ ] Test: ServiceDown alert fires → Slack + PagerDuty notify trong <2 min
- [ ] Runbook link trong mỗi alert annotation (depends on GAP-121)

## Related

- Audit: `documents/04-quality/audits/ops/ops-readiness-audit-2026-04-19.md` §7
- Depends: GAP-111 (monitoring stack in prod)
- Enables: GAP-121 (per-alert runbooks — annotation link target), GAP-122 (additional alerts)

## Log

- 2026-04-19 — Discovered in ops-readiness baseline audit
