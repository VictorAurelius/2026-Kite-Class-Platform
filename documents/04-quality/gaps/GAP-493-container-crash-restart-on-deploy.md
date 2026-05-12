# GAP-493: kitehub-* containers crash-restart loop on first deploy

**Status:** 🔵 OPEN
**Priority:** 🔴 P0 BLOCKING (blocks Phase 1 BETA soft launch — deploy never reaches healthy state)
**Domain:** DevOps / Backend
**Found:** 2026-05-12 (post-GAP-491 verified deploy run 25748003956)
**Affects:** Every deploy attempt until container startup config diagnosed

## Problem

Deploy run 25748003956 (v0.9.0-beta-staging.10, post-OTel-fix per #1209 + post-GAP-491 visibility) shows all 5 kitehub-* containers in crash-restart loop after `docker compose up -d`:

```
NAMES                  STATUS
kitehub-gateway        Up 54 seconds (health: starting)
kitehub-subscription   Up 57 seconds (health: starting)
kitehub-branding       Up About a minute (health: starting)
kitehub-admin          Up 2 seconds (health: starting)        ← just restarted
kitehub-email          Up Less than a second (health: starting) ← just restarted
kite-rabbitmq          Up 13 minutes (healthy)
kite-redis             Up 13 minutes (healthy)
```

Symptoms:
- `kitehub-gateway` port 8080: `curl: (56) Recv failure: Connection reset by peer` (container down at moment of probe)
- ALB target `i-05d7af46d01436b96` = `unhealthy` (Target.FailedHealthChecks)
- Containers cycle Up-seconds → crash → restart per docker-compose `restart: unless-stopped` policy
- SSM `Status=InProgress` for 8min even though deploy-prod.sh script finished (script exit + container restart cycle don't align)

## Root cause — to diagnose

Candidates (need EC2 container log inspection):
1. Spring Boot config error (missing env var? Wrong DB URL? Secrets not propagated?)
2. RabbitMQ ephemeral creds: deploy-prod.sh warning "rabbitmq-default-creds empty — generating ephemeral" — services may fail to connect with wrong/missing creds
3. Health check too aggressive (compose `healthcheck` interval/retries cause kill before Spring boots fully)
4. JVM memory exhaustion on t3.medium (GAP-447 right-size — 4GB RAM × 5 services × Spring Boot heap)

## Diagnostic commands (next session)

```bash
AWS_PROFILE=dev-admin aws ssm send-command --region ap-southeast-1 \
  --instance-ids i-05d7af46d01436b96 --document-name AWS-RunShellScript \
  --parameters 'commands=["docker logs --tail 100 kitehub-admin 2>&1; echo ===; docker logs --tail 100 kitehub-gateway 2>&1"]' \
  --query 'Command.CommandId' --output text
```

Or via Session Manager: inspect `/var/log/containers/*` if compose logs missing.

## Proposed Fix

TBD pending log inspection. Likely 1 of:
- Fix `populate-secrets.sh` to seed rabbitmq creds (if root cause = creds)
- Adjust docker-compose healthcheck `start_period: 120s` (if root cause = aggressive probe)
- JVM heap tune `-Xmx512m` (if root cause = OOM)
- Spring profile / env var injection (if root cause = config)

## Acceptance Criteria

- [ ] Root cause identified via container logs from `docker logs kitehub-admin/gateway/...`
- [ ] Fix shipped (config / secrets / healthcheck / heap as appropriate)
- [ ] Deploy retry succeeds: SSM `Status=Success`, ALB target `healthy`, `curl https://api.kitehub.me/actuator/health` returns 200
- [ ] Container `STATUS` column shows `(healthy)` for all 5 kitehub-* services after 2 min

## Related

- **Tooling:** GAP-491 (CloudWatch streaming) — VERIFIED working; surfaced this issue
- **Adjacent:** GAP-484 OTel fix (#1209) — already merged, NOT root cause
- **Adjacent:** GAP-447 EC2 right-size — t3.medium 4GB may be tight
- **Adjacent:** GAP-376 production data seed — secrets seeding scope
- **Rule:** Per `release-fix-retry-budget.md` v1.1.0 §3 — this is retry #2 from same deploy gate; STOP-AND-REDESIGN trigger applies. Next session must diagnose root cause BEFORE another retry.

## Log

- **2026-05-12:** Filed after deploy retry 25748003956 (GAP-491 verified) showed all kitehub-* containers in crash-restart loop. Visibility now works; this gap is what visibility surfaced.
