# Incident Response Runbook

> Last updated: 2026-04-16 | Owner: SRE / On-call

Quick-reference for production incidents. Goal: detect → triage → communicate → fix/rollback → postmortem.

---

## 1. Severity Levels & SLAs

| Severity | Description | Response | Escalation | Example |
|----------|-------------|----------|------------|---------|
| **P0** | All users affected, data loss risk | 15 min | Immediate — all hands | DB crashed, all services down |
| **P1** | Major feature broken for many users | 1 hour | Team lead + on-call | Payment fails, email queue stuck |
| **P2** | Partial degradation, workaround exists | 4 hours | On-call engineer | Single tenant login broken |
| **P3** | Minor issue, cosmetic | 24 hours | Next business day | Button misaligned, log noise |

---

## 2. On-Call Escalation Path

```
1. On-call engineer (PagerDuty/Slack alert)
   ↓ (if no response in 10 min OR P0)
2. Team lead / backup on-call
   ↓ (if no response in 15 min OR data loss)
3. CTO / all-hands emergency
```

**Channels:** `#incidents` (Slack), PagerDuty, phone for P0.

---

## 3. First Responder Checklist

### Step 1: Assess (0-5 min)

```bash
# Quick health check — all KiteHub services
curl -s http://localhost:9000/actuator/health | jq .status     # gateway
curl -s http://localhost:8081/actuator/health | jq .status     # subscription
curl -s http://localhost:8083/actuator/health | jq .status     # branding
curl -s http://localhost:8084/actuator/health | jq .status     # email
curl -s http://localhost:8085/actuator/health | jq .status     # admin

# Or use the status script (dev/docker-compose)
cd kitehub && ./scripts/status.sh --simple

# Kubernetes — check pod status
kubectl get pods -n kitehub --field-selector=status.phase!=Running
kubectl get pods -n kiteclass-instances --field-selector=status.phase!=Running
```

### Step 2: Communicate (5-10 min)

Post in `#incidents`:
```
INCIDENT: [P0/P1/P2/P3] — [short description]
IMPACT: [who is affected, which tenants]
STATUS: Investigating
OWNER: @[your-name]
NEXT UPDATE: [time, max 15 min for P0/P1]
```

### Step 3: Mitigate (10-30 min)

Pick the fastest fix:
1. **Recent deploy?** → Rollback (see `rollback-procedure.md`)
2. **Resource exhaustion?** → Scale up: `kubectl scale deployment/<svc> --replicas=5 -n kitehub`
3. **External dependency?** → Circuit breaker should kick in; verify with `/actuator/circuitbreakers`
4. **Config error?** → Fix env var, restart: `kubectl rollout restart deployment/<svc> -n kitehub`

### Step 4: Resolve & Verify

```bash
# Run health checks again
kubectl get pods -n kitehub
curl -s http://localhost:9000/actuator/health

# Check error rate returned to normal
kubectl logs --tail=100 deployment/kitehub-gateway -n kitehub | grep -c ERROR
```

### Step 5: Postmortem (within 3 days for P0/P1)

Use template in Section 6.

---

## 4. Common Incidents by Service

### kitehub-subscription (port 8081)

| Incident | Symptoms | Quick Fix |
|----------|----------|-----------|
| Payment webhook failure | Tenants stuck in PENDING | Check Stripe webhook logs; replay failed events |
| Trial expiration missed | Expired tenants still active | Run `TrialExpirationScheduler` manually; check cron config |
| Email queue stuck | Welcome/invoice emails delayed | Check RabbitMQ `subscription.events` queue depth |

### kitehub-branding (port 8083)

| Incident | Symptoms | Quick Fix |
|----------|----------|-----------|
| AI generation timeout | Job stuck >10 min | Check Ollama/AI service health; timeout config `ai.timeout-seconds` |
| MinIO upload failure | Logo/banner 500 errors | Check MinIO health: `curl http://localhost:9191/minio/health/live` |
| Template rendering crash | NPE on provisioning | Check template data; fallback to DEFAULT template |

### kitehub-email (port 8084)

| Incident | Symptoms | Quick Fix |
|----------|----------|-----------|
| SMTP auth failure | All emails fail | Verify `MAIL_PASSWORD` env; check provider status |
| RabbitMQ consumer dead | Queue depth growing | Restart service; check DLQ for poison messages |
| Rate limit by provider | Partial delivery | Slow down send rate in config; check bounce list |

### kiteclass-core

| Incident | Symptoms | Quick Fix |
|----------|----------|-----------|
| DB connection pool exhausted | 500 errors, slow responses | Increase `hikari.maximum-pool-size`; check for leaked connections |
| Attendance data loss | Missing records | Check `flyway_schema_history`; restore from backup if needed |
| Multi-tenant data leak | Wrong tenant data shown | **P0** — take service offline immediately; audit tenant isolation |

### kite-gateway (port 9000)

| Incident | Symptoms | Quick Fix |
|----------|----------|-----------|
| Rate limit breach | 429 errors for legitimate users | Adjust rate limit config; whitelist affected tenant |
| CORS errors | Frontend can't reach API | Check `cors.allowedOrigins` in gateway config |
| Route not found | 404 on valid endpoints | Verify service discovery; restart gateway |

### Infrastructure

| Incident | Symptoms | Quick Fix |
|----------|----------|-----------|
| PostgreSQL disk full | Write errors, service crash | Expand volume; clean old WAL; vacuum |
| Redis OOM | Cache evictions, slow responses | Increase memory limit; review eviction policy |
| RabbitMQ queue buildup | Consumer lag, delayed processing | Scale consumers; check for poison messages in DLQ |
| MinIO unreachable | Asset upload/download 500s | Check MinIO pods/container; verify bucket policies |

---

## 5. Communication Templates

### Internal (Slack #incidents)

```
UPDATE [HH:MM] — [P0/P1] [title]
IMPACT: [X tenants affected / all users / specific feature]
ROOT CAUSE: [identified / investigating]
MITIGATION: [action taken]
ETA: [estimated resolution time]
NEXT UPDATE: [time]
```

### Tenant-Facing (if P0/P1 >30 min)

```
Subject: [KiteHub] Service Disruption — [feature]

We are currently experiencing issues with [feature].
Our team is actively working on a resolution.

Impact: [what is not working]
Workaround: [if any]
ETA: [estimated resolution]

We will provide updates every [30 min / 1 hour].
Apologies for the inconvenience.
```

---

## 6. Postmortem Template (5-Whys)

```markdown
# Postmortem: [Incident Title]

**Date:** YYYY-MM-DD
**Severity:** P0/P1/P2
**Duration:** X hours Y minutes
**Author:** [name]

## Timeline
- HH:MM — Alert triggered
- HH:MM — On-call acknowledged
- HH:MM — Root cause identified
- HH:MM — Fix deployed
- HH:MM — Verified resolved

## Impact
- [N] tenants affected
- [X] minutes of downtime
- [Y] failed requests

## Root Cause (5 Whys)
1. Why did [symptom]? — Because [cause 1]
2. Why did [cause 1]? — Because [cause 2]
3. Why did [cause 2]? — Because [cause 3]
4. Why did [cause 3]? — Because [cause 4]
5. Why did [cause 4]? — Because [root cause]

## What Went Well
- [detection, response, communication]

## What Went Wrong
- [gaps, delays, missing runbook]

## Action Items
| # | Action | Owner | Due | Status |
|---|--------|-------|-----|--------|
| 1 | [preventive action] | @name | date | TODO |
```

---

## 7. Monitoring Quick Reference

| What | URL / Command | Purpose |
|------|---------------|---------|
| Service health | `/actuator/health` on each port | UP/DOWN status |
| Metrics | `/actuator/metrics` | JVM, HTTP, DB pool stats |
| Logs (dev) | `cd kitehub && ./scripts/logs.sh [service]` | Docker Compose logs |
| Logs (k8s) | `kubectl logs -f deploy/<svc> -n kitehub` | Pod logs |
| RabbitMQ | `http://localhost:15673` (dev) | Queue depths, consumers |
| MinIO | `http://localhost:9191` (dev) | Object storage health |
| DB connections | `/actuator/metrics/hikaricp.connections.active` | Pool usage |

---

## Related

- [Rollback Procedure](./rollback-procedure.md)
- [Deploy Go/No-Go Checklist](./deploy-go-nogo-checklist.md)
- [Deployment Procedures](./operations/runbooks/deployment-procedures.md)
- [Secret Management](./SECRET-MANAGEMENT.md)
