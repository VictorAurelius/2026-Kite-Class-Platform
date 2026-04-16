# Ops Readiness Audit — Scoring Guide

## Grading Scale

| Score | Grade | Meaning |
|-------|-------|---------|
| 90-100 | A | Production-ready operations |
| 80-89 | B | Good — minor gaps in observability |
| 70-79 | C | Acceptable — manual ops still needed |
| 60-69 | D | Significant ops gaps — risky to deploy |
| <60 | F | Not ready for production |

---

## Category 1: Monitoring & Observability (20 pts)

| Score | Criteria |
|-------|----------|
| 20 | Actuator + Prometheus + Grafana dashboards + distributed tracing (Zipkin/Jaeger) |
| 16 | Actuator + Prometheus + basic Grafana, no tracing |
| 12 | Actuator exposed, Prometheus configured, no dashboard |
| 8 | Actuator enabled but limited exposure |
| 4 | Health endpoint only |
| 0 | No monitoring |

**Checks:**
- `management.endpoints.web.exposure.include` in application.yml
- Prometheus scrape config in k8s/Helm
- Grafana dashboard JSON/provisioning in infrastructure/
- Custom metrics (`@Timed`, `MeterRegistry`) on business operations
- Frontend: error tracking (Sentry or similar) configured?

---

## Category 2: Logging Standards (20 pts)

| Score | Criteria |
|-------|----------|
| 20 | Structured JSON, required fields (timestamp, service, level, traceId, tenantId), PII scrubbing, log aggregation (ELK/Loki) |
| 16 | Structured JSON, most required fields, basic aggregation |
| 12 | Structured logging configured, some fields missing |
| 8 | Text-based logging with consistent format |
| 4 | Default Spring Boot logging, no structure |
| 0 | No logging strategy |

**Required fields for multi-tenant SaaS:**
- `timestamp` (ISO 8601)
- `service` (which microservice)
- `level` (INFO/WARN/ERROR)
- `traceId` (distributed tracing correlation)
- `tenantId` (multi-tenant isolation — CRITICAL for KiteClass)
- `userId` (audit trail)

**PII scrubbing:**
- Email addresses masked in logs
- Passwords never logged
- Student data (FERPA-adjacent) protected

---

## Category 3: Backup & Recovery (20 pts)

| Score | Criteria |
|-------|----------|
| 20 | Automated backup, tested restore, DR plan with RTO<1h/RPO<5min, documented |
| 16 | Automated backup, restore tested once, DR plan exists |
| 12 | Backup configured, not regularly tested, DR plan drafted |
| 8 | Manual backup procedure documented |
| 4 | Backup mentioned but not configured |
| 0 | No backup strategy |

**Components requiring backup:**
- PostgreSQL (primary data store) — pg_dump or WAL archiving
- MinIO (AI-generated assets, logos) — bucket replication or periodic sync
- Redis (cache only — acceptable to lose, but session data?)
- RabbitMQ (transient — Outbox pattern ensures no message loss)
- Terraform state (remote backend with versioning)

---

## Category 4: Alerting (20 pts)

| Score | Criteria |
|-------|----------|
| 20 | Alert rules per service, escalation policy, on-call rotation, runbooks per alert |
| 16 | Key alerts defined, on-call process documented, some runbooks |
| 12 | Basic alerts (disk, CPU, service down), no escalation |
| 8 | Health check alerts only |
| 4 | Alerts planned but not configured |
| 0 | No alerting |

**Critical alerts for this platform:**
- Service health: any service DOWN > 30s
- DB connections: pool exhaustion > 80%
- API latency: p95 > 2s
- Error rate: 5xx > 1% in 5min window
- Disk: > 80% usage
- Queue depth: RabbitMQ queue > 1000 messages
- Certificate expiry: < 14 days
- AI provider: Ollama/external API failure rate > 10%

---

## Category 5: Deployment Pipeline (20 pts)

| Score | Criteria |
|-------|----------|
| 20 | Blue/green deploy, automated rollback, health checks gate, zero-downtime, IaC |
| 16 | Rolling deploy, manual rollback procedure, health checks, Helm/Terraform |
| 12 | Basic deploy with health checks, some IaC |
| 8 | Manual deploy with documented procedure |
| 4 | CI builds but manual deploy |
| 0 | No deployment automation |

**Checks:**
- Helm: `strategy.type: RollingUpdate` or blue/green
- k8s: `livenessProbe`, `readinessProbe`, `startupProbe` configured
- Rollback: `helm rollback` or `kubectl rollout undo` documented
- Database migration: runs before app starts (Flyway on boot)
- Feature flags: can disable new features without redeploy?
- Canary: partial traffic to new version before full rollout?
