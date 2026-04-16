---
name: ops-readiness-audit
description: "Dùng khi user nói 'ops audit', 'production ready?', 'kiểm tra ops', 'deploy checklist', 'monitoring check', hoặc trước GA release. Production operations readiness /100."
user-invocable: true
---

# /ops-readiness-audit — Production Operations Readiness

Score /100. Verify the platform is ready for production operations: monitoring, logging, backup, alerting, deployment.

## Process

### 1. Check Infrastructure Config

```bash
# Monitoring endpoints
grep -rn "actuator\|prometheus\|metrics\|health" --include="*.yml" --include="*.yaml" \
  kiteclass/ kitehub/ infrastructure/ | grep -v node_modules | head -30

# Logging config
grep -rn "logging\.\|logback\|log4j\|winston\|pino" --include="*.yml" --include="*.xml" --include="*.ts" \
  kiteclass/ kitehub/ | grep -v node_modules | head -20

# Backup/DR docs
ls documents/05-guides/*backup* documents/05-guides/*disaster* documents/05-guides/*recovery* 2>/dev/null

# Helm/k8s health probes
grep -rn "livenessProbe\|readinessProbe\|startupProbe" infrastructure/ | head -10

# Alert rules
ls infrastructure/*/alerting* infrastructure/*/alerts* 2>/dev/null
```

### 2. Score 5 Categories

| # | Category (20pts) | Key Checks |
|---|-----------------|------------|
| 1 | **Monitoring & Observability** | Actuator, Prometheus, Grafana dashboards |
| 2 | **Logging Standards** | Structured JSON, required fields, PII scrubbing |
| 3 | **Backup & Recovery** | DB backup strategy, DR plan, RTO/RPO defined |
| 4 | **Alerting** | Alert rules, on-call process, runbooks |
| 5 | **Deployment Pipeline** | Rolling/blue-green, rollback, health checks |

Scoring details: `reference/scoring-guide.md`

### 3. Output

Save to `documents/04-quality/audits/ops/ops-readiness-audit-[date].md`

## Gotchas

- Spring Boot Actuator may be enabled but endpoints not exposed — check `management.endpoints.web.exposure`
- Prometheus metrics need `/actuator/prometheus` endpoint AND scrape config in k8s
- RabbitMQ monitoring: separate from app monitoring — check `rabbitmq_management` plugin
- MinIO: backup strategy different from PostgreSQL — object storage vs relational
- KiteHub has 6 microservices — each needs independent health check
- Terraform state: must be remote (S3/OCI bucket), not local

## Skill Contents

- `reference/scoring-guide.md` — Detailed rubric per category
