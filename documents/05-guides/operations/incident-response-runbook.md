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

## 8. Rollback Workflow & Cycle Validation (GAP-475 Sub-6 + GAP-477)

`rollback.yml` workflow đã landed Wave 63 (GAP-477). Cấu hình human-triggered `workflow_dispatch` + confirm input `APPLY` verbatim + narrow OIDC role `kitehub-rollback-role`, theo pattern `release-deploy-standard.md` §9 (deploy execution allowed case).

### 8.1 Invocation

Trigger rollback (production incident response — P0/P1 only):

```bash
gh workflow run rollback.yml \
  -f target_sha=<sha> \
  -f confirm=APPLY \
  -f dry_run=false
```

| Input | Type | Required | Description |
|-------|------|----------|-------------|
| `target_sha` | string | yes | Full 40-char commit SHA from `main` to rollback to (verified exists in repo history) |
| `confirm` | string | yes | Must equal `APPLY` verbatim (case-sensitive cognitive checkpoint per `release-deploy-standard.md` §9) |
| `dry_run` | bool | yes | `true` = log intended actions, no real revert; `false` = execute rollback |

Workflow gates:
- GitHub Environment `production` requires reviewer approval before the apply job runs (manual gate, 1 approver minimum)
- OIDC role `kitehub-rollback-role` scoped least-privilege (ECS service update + ALB health probe + CloudWatch metrics write)
- Output: GitHub Step Summary với target_sha + smoke pre/post status + TTR (time-to-recovery) emitted dưới dạng metric `KiteHub/Rollback/TimeToRecovery`

### 8.2 Cadence — periodic validation cycle

Periodic validation của full rollback → smoke → restore → smoke cycle. Validates rằng rollback workflow + smoke gates phát hiện được regression và restore-forward path để hệ thống về trạng thái known-good. Per `release-deploy-standard.md` §4.3 post-deploy schedule:

| Cadence | Command | Purpose |
|---------|---------|---------|
| Monthly | `bash scripts/smoke-rollback-cycle.sh --dry-run` | Verify pre-flight smoke + SHA resolution + report scaffold (no real rollback). Default behavior của script. |
| Quarterly (maintenance window) | `bash scripts/smoke-rollback-cycle.sh --execute` | Real rollback to previous main SHA → re-smoke → restore forward → re-smoke. Emits JSON report to `/tmp/rollback-cycle-<epoch>.json` + baseline TTR cho monitoring. |

Reference: [`scripts/smoke-rollback-cycle.sh`](../../../scripts/smoke-rollback-cycle.sh), [`scripts/smoke-test.sh`](../../../scripts/smoke-test.sh), [`.github/workflows/rollback.yml`](../../../.github/workflows/rollback.yml), [Rollback Procedure](./rollback-procedure.md).

### 8.3 TTR (Time-to-Recovery) target

| Metric | Target | Notes |
|--------|--------|-------|
| Workflow trigger → `production` env approval | <2 min | Reviewer responsiveness (P0 = page on-call) |
| Approval → ECS service stable on target_sha | <2 min | Image pull + task replacement |
| Stable → smoke probe green | <1 min | Health endpoint + smoke-test.sh |
| **End-to-end TTR** | **<5 min** | Trigger → health-back; tracked CloudWatch metric `KiteHub/Rollback/TimeToRecovery` |

Pattern TTR >5 min trong 2 incidents liên tiếp = file follow-up gap để retune workflow (image cache, smoke probe, approval SLA).

### 8.4 Troubleshooting

| Symptom | Likely cause | Fix |
|---------|--------------|-----|
| Workflow fails ngay với "target_sha not found" | SHA quá cũ (rebased away) hoặc typo | Verify với `git log --all --oneline <sha>`; nếu SHA bị GC, dùng tag `v*` gần nhất thay vì SHA |
| OIDC token request bị deny | `kitehub-rollback-role` IAM trust policy stale hoặc workflow `id-token: write` permission thiếu | Check `.github/workflows/rollback.yml` permissions + AWS console role trust policy includes `repo:VictorAurelius/2026-Kite-Class-Platform:ref:refs/heads/main` |
| Health probe timeout sau rollback | ECS task vẫn pull image hoặc DB migration không backward-compat | (a) tăng probe grace period 60s; (b) verify migration revert đã chạy; (c) escalate P0 — có thể cần restore-forward + manual DB fix |
| `confirm` input bị reject | Không gõ đúng `APPLY` (verbatim, case-sensitive) | Re-run workflow với `confirm=APPLY` chính xác (no quotes, no whitespace) |
| Smoke post-rollback fail | Application code có bug ở target_sha cũ HOẶC dependency drift | Escalate P0; option: restore-forward (rollback the rollback) HOẶC fix-forward hotfix PR |

⚠️ **Khi nào KHÔNG dùng rollback workflow:** config-only changes (revert qua env var), data corruption (cần DB restore, không phải code rollback), security incident yêu cầu evidence preservation (rollback xóa stack trace runtime).

---

## 9. Pre-launch dry-run (Wave 86 Bucket H H-AC7)

Pre-tag-v1.0.0-rc.1 mandate: chạy 1 dry-run incident response scenario để measure baseline MTTD (Mean Time To Detect) + MTTR (Mean Time To Recovery) trước khi invite Phase 1.5 tenants. Mục đích: validate on-call procedures + verify monitoring/alerting kích hoạt đúng cách + identify gaps trước khi production traffic blast.

### 9.1 Scenario — "Email không gửi" (Resend API down / DKIM fail / SPF reject)

**Bối cảnh:** Beta tenant tạo signup → kỳ vọng email verification trong 30s → email không tới. Tenant submits support ticket.

**Why this scenario:** Email là điểm rủi ro cao trong Phase 1 BETA workflow (signup verification, password reset, beta invite). Resend Free tier 100/day = single-point dependency; DKIM/SPF/DMARC propagation Cloudflare cũng có thể drift. Mock scenario này covers 3 layers: vendor (Resend) + DNS (Cloudflare) + downstream (kitehub-email service).

### 9.2 Dry-run procedure

**Setup:** Pick maintenance window (low-traffic period); notify team (solo-dev → log to memory) before starting.

**Detect (target MTTD < 5 min):**

1. Trigger mock failure (one of):
   - **Option A — Resend API simulation:** Suspend Resend API key trong Resend dashboard tạm thời (Settings → API Keys → click revoke → revoke for 10 min)
   - **Option B — Bad DNS:** Update Cloudflare SPF record → invalid value (e.g. remove `include:amazonses.com`) → Resend send fails authentication
   - **Option C — kitehub-email crash:** SSM `docker stop kitehub-email-container` on EC2 (only kitehub-email — gateway sẽ route requests but no consumer)
2. Trigger ≥3 test signups (simulating real user traffic) → email send attempts fail
3. **Detect mechanism**: monitor:
   - CloudWatch dashboard `KiteHub/Email/SendFailureRate` alarm fires (per Wave 84 Bucket H GAP-437)
   - Grafana panel "Resend API status" turns red
   - Application log `kitehub-email` shows `RESEND_API_ERROR` repeated
   - **Manual alternative:** Resend dashboard "Activity" tab shows `failed` status
4. Record timestamp `T_detect` — first signal received

**Diagnose (target 5-10 min):**

5. Apply §3 First Responder Checklist:
   - Step 1: Severity P1 (email broken — degraded user signup, not complete outage)
   - Step 2: Communicate — log to incident channel (solo-dev: memory entry + status page draft)
   - Step 3: Inspect — `aws ssm send-command` to EC2 instance to check `kitehub-email` container logs
6. Identify root cause via 5-Whys per §6:
   - Why email fails? → Resend API returns 401 / DNS fails / container down
   - Why? → API key revoked / SPF invalid / process crashed
   - Verify with `curl https://api.resend.com/domains -H "Authorization: Bearer $KEY"` OR `dig +short TXT kitehub.me` OR `docker ps | grep email`

**Recover (target 10-30 min):**

7. Apply mitigation (matches scenario):
   - **A:** Re-enable Resend API key trong dashboard
   - **B:** Restore Cloudflare SPF record correct value
   - **C:** `docker start kitehub-email-container` via SSM
8. Verify recovery:
   - Re-trigger test signup → email arrives <30s
   - CloudWatch alarm clears
   - Grafana panel "Resend API status" returns green
9. Record timestamp `T_recover` — first successful email post-fix

**Postmortem (target within 24h):**

10. MTTD = `T_detect - T_inject` (target < 5 min — alarm fire to operator notice)
11. MTTR = `T_recover - T_detect` (target < 30 min for P1 — including diagnose + apply fix)
12. Write postmortem per §6 template → save to `documents/04-quality/audits/incidents/2026-MM-DD-dry-run-email-failure.md`
13. Action items: gaps surfaced trong drill → file follow-up gaps

### 9.3 Expected baseline ranges (calibration)

| Metric | Target | Acceptable Phase 1 BETA | Action if exceeded |
|---|---|---|---|
| MTTD (alarm fire → operator notice) | < 5 min | < 10 min | Tune CloudWatch alarm thresholds + PagerDuty/SNS routing |
| MTTR (diagnose + fix) | < 30 min for P1 | < 60 min | File gap to add runbook automation (e.g. failover script, restart playbook) |
| False-positive alarm rate | 0 | < 1/week | Tune alarm sensitivity |
| Recovery verify (smoke test) | < 5 min | < 10 min | Automate smoke per `scripts/smoke-test.sh` |

### 9.4 Cadence

| Trigger | Cadence | Scenario rotation |
|---|---|---|
| Pre-tag v1.0.0-rc.1 | 1 time (mandatory) | "Email không gửi" (this scenario) |
| Quarterly (after v1.0.0) | Every 90d | Rotate scenarios: Email fail / DB connection pool exhausted / Gateway 502 / EC2 instance unhealthy |
| Post-major-deploy regression | Optional | Re-run last-quarter scenario for regression check |
| New team member onboard | First week | Run dry-run as training |

### 9.5 Acceptance criteria for H-AC7

- [ ] Dry-run scenario procedure documented (§9.1-§9.2) ✅
- [ ] Baseline MTTD + MTTR ranges defined (§9.3) ✅
- [ ] Cadence schedule documented (§9.4) ✅
- [ ] Actual dry-run executed pre-tag-v1.0.0-rc.1 → MTTD/MTTR measured + within target → ⏳ defer to pre-tag run
- [ ] Postmortem template ready (§6 existing) ✅
- [ ] Follow-up gap mechanism ready (`audit-to-gap-pipeline.md`) ✅

**Status:** doc shipped Wave 86 Bucket H; actual dry-run execution + timing measure = pre-tag-rc1 task.

---

## Related

- [Rollback Procedure](./rollback-procedure.md)
- [Deploy Go/No-Go Checklist](./deploy-go-nogo-checklist.md)
- [Deployment Procedures](./operations/runbooks/deployment-procedures.md)
- [Secret Management](./SECRET-MANAGEMENT.md)
