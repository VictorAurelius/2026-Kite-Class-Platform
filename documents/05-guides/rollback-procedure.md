# Rollback Procedure Per Service

> Last updated: 2026-04-16 | Owner: DevOps / SRE

Step-by-step rollback for each KiteHub/KiteClass service. Bookmark this — you will need it at 2 AM.

---

## 1. General Strategy

| Strategy | When to Use | Downtime |
|----------|------------|----------|
| **Helm rollback** | Application bug, config error | ~2 min (rolling) |
| **DB restore** | Data corruption, bad migration | 10-30 min (full outage) |
| **Blue-green switch** | Pre-staged alternative ready | ~30 sec |

**Default:** Helm rollback (covers 90% of cases). DB rollback only when data is affected.

---

## 2. Helm Quick Reference

```bash
# Check release history
helm history kitehub -n kitehub

# Rollback to previous revision
helm rollback kitehub 0 -n kitehub    # 0 = previous revision

# Rollback to specific revision
helm rollback kitehub <revision> -n kitehub

# Verify rollout completed
kubectl rollout status deployment/kitehub-gateway -n kitehub
kubectl rollout status deployment/kitehub-subscription -n kitehub

# Check all pods running
kubectl get pods -n kitehub
```

---

## 3. Per-Service Rollback

### kitehub-gateway (stateless)

**Risk:** Low — no persistent state.

```bash
# Rollback
helm rollback kitehub <revision> -n kitehub
kubectl rollout status deployment/kitehub-gateway -n kitehub

# Verify
curl -sf http://<gateway-url>:9000/actuator/health | jq .status
# Expected: "UP"

# Check routes work
curl -sf http://<gateway-url>:9000/api/v1/subscriptions/health
```

**Gotcha:** If CORS config changed, browser may cache old preflight. Tell users to hard-refresh.

---

### kitehub-subscription (stateful — DB + events)

**Risk:** Medium — has database state and publishes RabbitMQ events.

```bash
# 1. Rollback application
helm rollback kitehub <revision> -n kitehub
kubectl rollout status deployment/kitehub-subscription -n kitehub

# 2. Check if DB migration needs undo (see Section 4)
kubectl exec -n kitehub kitehub-subscription-<pod> -- \
  curl -s http://localhost:8080/actuator/flyway | jq '.contexts[].flywayBeans[].migrations[-1]'

# 3. Verify subscription operations
curl -sf http://localhost:8081/actuator/health | jq .
```

**Gotcha:** If a migration added a NOT NULL column, old code version may fail INSERTs. Check migration backward compatibility BEFORE rollback.

---

### kitehub-branding (stateful — MinIO assets)

**Risk:** Low-Medium — MinIO assets are immutable (versioned by job ID).

```bash
# 1. Rollback application
helm rollback kitehub <revision> -n kitehub
kubectl rollout status deployment/kitehub-branding -n kitehub

# 2. Assets in MinIO do NOT need rollback (immutable, keyed by jobId)
# Old version code will reference old asset URLs — still valid

# 3. Verify
curl -sf http://localhost:8083/actuator/health | jq .status
```

**Gotcha:** If new version changed asset URL pattern, old code may 404 on assets created by new version. Check MinIO bucket for orphaned assets.

---

### kitehub-admin (stateless)

**Risk:** Low — read-heavy service, no critical writes.

```bash
helm rollback kitehub <revision> -n kitehub
kubectl rollout status deployment/kitehub-admin -n kitehub
curl -sf http://localhost:8085/actuator/health | jq .status
```

---

### kitehub-email (stateful — RabbitMQ queues)

**Risk:** Medium — messages in queue may be incompatible with old version.

```bash
# 1. Rollback application
helm rollback kitehub <revision> -n kitehub
kubectl rollout status deployment/kitehub-email -n kitehub

# 2. Check DLQ for failed messages
# RabbitMQ Management: http://localhost:15673 (dev) or port-forward in k8s
# Queue: email.send.dlq — messages that failed deserialization after rollback

# 3. If DLQ has messages from new version format:
#    Option A: Drain and re-enqueue after format fix
#    Option B: Dead-letter and manually process later

# 4. Verify email sending
curl -sf http://localhost:8084/actuator/health | jq .status
```

**Gotcha:** New message format published by new version cannot be consumed by old version. Drain DLQ manually.

---

### kitehub-frontend (stateless — CDN cache concern)

**Risk:** Low — static files served by nginx container.

```bash
# 1. Rollback application
helm rollback kitehub <revision> -n kitehub
kubectl rollout status deployment/kitehub-frontend -n kitehub

# 2. CDN cache invalidation (if using CloudFront/CDN)
aws cloudfront create-invalidation \
  --distribution-id <DIST_ID> \
  --paths "/*"

# 3. Verify
curl -sf http://localhost:3001/ -o /dev/null -w "%{http_code}"
# Expected: 200
```

**Gotcha:** Browsers cache aggressively. Filename hashing (Vite default) handles this, but check if any un-hashed assets exist.

---

### kiteclass-core (stateful — per-tenant DB)

**Risk:** Medium-High — multi-tenant data, Flyway migrations.

```bash
# 1. Rollback application
helm rollback kiteclass-instance <revision> -n kiteclass-instances

# 2. Check Flyway migration status
kubectl exec -n kiteclass-instances <pod> -- \
  curl -s http://localhost:8080/actuator/flyway | jq '.contexts[].flywayBeans[].migrations[-1]'

# 3. If migration needs undo — see Section 4

# 4. Verify tenant data integrity
curl -sf http://<instance-url>/actuator/health | jq .
```

**Gotcha:** Multi-tenant rollback is all-or-nothing per KiteClass instance. Cannot rollback one tenant's data without affecting others on the same instance.

---

## 4. Database Rollback

### When Needed

Database rollback is needed when:
- New migration added a breaking schema change
- Data migration corrupted existing records
- Old application version cannot work with new schema

### Option A: Flyway Undo Migration (preferred)

```bash
# 1. Check current migration version
kubectl exec -n kitehub postgres-0 -- \
  psql -U kitehub -d kitehub -c \
  "SELECT version, description, success FROM flyway_schema_history ORDER BY installed_rank DESC LIMIT 5;"

# 2. Apply undo script (must be prepared BEFORE deploy)
# Naming: U<version>__undo_<description>.sql
kubectl exec -n kitehub postgres-0 -- \
  psql -U kitehub -d kitehub < migrations/U14__undo_add_column.sql

# 3. Remove migration record from Flyway history
kubectl exec -n kitehub postgres-0 -- \
  psql -U kitehub -d kitehub -c \
  "DELETE FROM flyway_schema_history WHERE version = '14';"

# 4. Verify schema matches old application version
kubectl exec -n kitehub postgres-0 -- \
  psql -U kitehub -d kitehub -c "\d+ <affected_table>"
```

### Option B: Backup Restore (last resort)

```bash
# 1. STOP ALL WRITES — scale all services to 0
kubectl scale deployment --all --replicas=0 -n kitehub

# 2. Restore from pre-deploy backup
gunzip backup-<timestamp>.sql.gz
kubectl exec -i -n kitehub postgres-0 -- \
  psql -U kitehub -d kitehub < backup-<timestamp>.sql

# 3. Verify data
kubectl exec -n kitehub postgres-0 -- \
  psql -U kitehub -d kitehub -c "SELECT COUNT(*) FROM subscriptions;"

# 4. Restart services with OLD version
helm rollback kitehub <revision> -n kitehub

# 5. Verify everything works
kubectl get pods -n kitehub
curl -sf http://localhost:9000/actuator/health
```

**Data loss:** All changes since backup will be lost. Backup frequency = max data loss window.

### Multi-Tenant Considerations

- Per-tenant rollback is **NOT supported** — rollback affects all tenants on the instance
- Always communicate to all affected tenants before DB rollback
- If only one tenant is affected, consider a targeted data fix instead of full rollback

---

## 5. Docker Compose Rollback (Dev/Staging)

For local development or staging with Docker Compose:

```bash
# Using KiteHub scripts
cd kitehub

# 1. Stop current stack
./scripts/down.sh

# 2. Checkout previous version
git checkout <previous-tag>

# 3. Rebuild and restart
./scripts/build-all.sh
./scripts/up.sh

# 4. Verify
./scripts/status.sh
```

---

## 6. Post-Rollback Verification

After ANY rollback, complete this checklist:

- [ ] All pods Running and Ready (`kubectl get pods -n kitehub`)
- [ ] Health endpoints return UP for all services
- [ ] No error spikes in logs (`kubectl logs --tail=50 deploy/<svc> -n kitehub | grep ERROR`)
- [ ] Key flows work: login, tenant creation, subscription check
- [ ] RabbitMQ queues not growing unbounded (check management UI)
- [ ] Database connections healthy (`/actuator/metrics/hikaricp.connections.active`)
- [ ] Notify team in `#incidents`: rollback complete, version reverted to X
- [ ] Create postmortem ticket for the failed deploy

---

## Related

- [Incident Response Runbook](./incident-response-runbook.md)
- [Deploy Go/No-Go Checklist](./deploy-go-nogo-checklist.md)
- [Deployment Procedures](./operations/runbooks/deployment-procedures.md)
- [Helm Charts README](../../infrastructure/helm/README.md)
