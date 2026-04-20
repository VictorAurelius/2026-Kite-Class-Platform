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
# Monitoring endpoints (broad scope — catches all submodules + infrastructure)
grep -rn "actuator\|prometheus\|metrics\|health" --include="*.yml" --include="*.yaml" \
  | grep -v node_modules | grep -v target | head -30

# Logging config (broad scope)
grep -rn "logging\.\|logback\|log4j\|winston\|pino" --include="*.yml" --include="*.xml" --include="*.ts" \
  | grep -v node_modules | grep -v target | head -20

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

## Context Management

Token budget ~25-35K. Kiểm soát:

1. **Grep infrastructure files** — `| head -20` per grep. Infrastructure files nhiều nhưng config sections lặp lại.
2. **Helm values** — Chỉ đọc security-relevant keys (`resources`, `probes`, `securityContext`), không đọc full values.yaml.
3. **Terraform** — Chỉ check state backend config + resource count, không đọc full .tf files.
4. **Doc existence check** — Dùng `ls` thay vì `cat` cho backup/DR docs. Chỉ đọc nội dung nếu cần verify chi tiết.

## Gotchas

- Spring Boot Actuator may be enabled but endpoints not exposed — check `management.endpoints.web.exposure`
- Prometheus metrics need `/actuator/prometheus` endpoint AND scrape config in k8s
- RabbitMQ monitoring: separate from app monitoring — check `rabbitmq_management` plugin
- MinIO: backup strategy different from PostgreSQL — object storage vs relational
- KiteHub has 6 microservices — each needs independent health check
- Terraform state: must be remote (S3/OCI bucket), not local
- **Multi-module scope** — narrow grep `kiteclass/ kitehub/` may miss submodule config files; prefer broad `--include="*.yml"` from root. Ref: GAP-149, memory `feedback_audit_grep_scope.md`.

## Skill Contents

- `reference/scoring-guide.md` — Detailed rubric per category
