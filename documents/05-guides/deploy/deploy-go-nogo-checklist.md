# Deploy Go/No-Go Checklist

> Last updated: 2026-04-16 | Owner: Release Manager / Tech Lead

Decision framework for "Should we deploy this version to production?"

---

## 1. Pre-Deploy Checklist

Complete ALL items before proceeding. Any unchecked = NO-GO unless explicitly waived.

### Code Quality

- [ ] CI green on main (all workflows passing)
- [ ] All P0 gaps closed (check `documents/04-quality/gaps/ROADMAP.md`)
- [ ] No P1 gaps blocking this release
- [ ] Code review approved (two-stage review completed)
- [ ] No known regressions from previous release

### Database

- [ ] Migrations reviewed (backward compatible with current running version)
- [ ] Rollback SQL script prepared for each new migration
- [ ] Migrations tested in staging with production-like data volume
- [ ] No destructive operations (DROP TABLE, DELETE without WHERE) without explicit approval
- [ ] Flyway history clean — no failed migrations in staging

### API & Compatibility

- [ ] API contract backward compatible (no breaking changes to existing endpoints)
- [ ] If breaking change: version bumped, deprecation notice sent, migration guide ready
- [ ] Frontend compatible with both old and new API (during rolling update window)

### Security

- [ ] Security audit score >= 70/100 (or no regressions from last audit)
- [ ] No new critical/high CVEs in dependencies (`./scripts/check-deps.sh`)
- [ ] Secrets rotated if any were exposed
- [ ] No hardcoded credentials in diff

### Performance

- [ ] Performance baseline recorded (key endpoints p95 latency, DB query times)
- [ ] No N+1 queries introduced (check Hibernate logs in staging)
- [ ] Load test passed if significant traffic-pattern change

### Operational Readiness

- [ ] Rollback procedure verified in staging (see `rollback-procedure.md`)
- [ ] Monitoring dashboards accessible and showing data
- [ ] Alerting rules configured for new features (if applicable)
- [ ] On-call engineer identified and available for deploy window
- [ ] Incident response runbook reviewed (see `incident-response-runbook.md`)

### Communication

- [ ] Tenant notification sent if maintenance window needed
- [ ] Team notified of deploy schedule in `#deployments` channel
- [ ] Feature flags configured for gradual rollout (if applicable)

---

## 2. Deploy Steps by Environment

### Staging (mandatory before production)

```bash
# 1. Deploy to staging
helm upgrade kitehub ./infrastructure/helm/kitehub \
  --namespace kitehub-staging \
  --set global.image.tag=<version>

# 2. Run full test suite against staging
./scripts/test-api-e2e.sh --env staging

# 3. Manual smoke test (5 min)
#    - Login as admin → dashboard loads
#    - Create trial tenant → provisioning completes
#    - Check subscription flow → payment page renders

# 4. Verify metrics (15 min soak)
#    - Error rate < 0.1%
#    - p95 latency < 500ms
#    - No OOM restarts
```

### Canary (recommended for risky changes)

```bash
# 1. Deploy canary (1 replica with new version alongside old)
helm upgrade kitehub ./infrastructure/helm/kitehub \
  --namespace kitehub \
  --set gateway.canary.enabled=true \
  --set gateway.canary.weight=10 \
  --set global.image.tag=<version>

# 2. Monitor canary for 30 min
#    - Compare error rate: canary vs stable
#    - Compare latency: canary vs stable

# 3. If healthy → promote to full rollout
# 4. If unhealthy → rollback canary immediately
```

### Production

```bash
# 1. Take database backup
kubectl exec -n kitehub postgres-0 -- \
  pg_dump -U kitehub kitehub | gzip > backup-$(date +%Y%m%d-%H%M).sql.gz

# 2. Deploy
helm upgrade kitehub ./infrastructure/helm/kitehub \
  --namespace kitehub \
  --set global.image.tag=<version> \
  --wait --timeout 10m

# 3. Verify rollout
kubectl rollout status deployment/kitehub-gateway -n kitehub
kubectl rollout status deployment/kitehub-subscription -n kitehub
kubectl rollout status deployment/kitehub-branding -n kitehub
kubectl rollout status deployment/kitehub-email -n kitehub
kubectl rollout status deployment/kitehub-admin -n kitehub
kubectl rollout status deployment/kitehub-frontend -n kitehub

# 4. Post-deploy smoke test
curl -sf http://localhost:9000/actuator/health | jq .status
# Expected: "UP"

# 5. Monitor for 30 min before declaring success
```

---

## 3. Go/No-Go Decision Matrix

| Criteria | GO | NO-GO |
|----------|-----|-------|
| CI status | All green | Any red |
| P0 gaps | 0 open | Any open |
| Staging test | All pass | Any critical fail |
| DB migration | Backward compatible | Breaking without rollback script |
| Security | No new critical CVEs | Unpatched critical CVE |
| On-call | Engineer available | No one available |
| Rollback | Tested in staging | Not tested |
| Deploy window | Off-peak (Tue-Thu 10PM VN) | Peak hours without approval |

### Who Decides

| Scenario | Decision Maker |
|----------|---------------|
| Standard deploy (all criteria GO) | On-call engineer + tech lead |
| 1-2 criteria waived (non-critical) | Tech lead approval required |
| Any security NO-GO | CTO approval required |
| Emergency hotfix (P0 incident) | On-call engineer (fast-track) |

---

## 4. Rollback Trigger Conditions

### Automatic Rollback (if configured)

- Pod fails health check 3 consecutive times → Kubernetes rolls back
- Helm `--wait` timeout (10 min) → Helm auto-rollback

### Manual Rollback — Trigger Immediately If

- Error rate > 1% for 5+ minutes after deploy
- Any P0 incident (data loss, all users affected)
- Critical security vulnerability discovered post-deploy
- Database migration failed and data is inconsistent

### Manual Rollback — Consider If

- Error rate > 0.5% for 10+ minutes
- p95 latency > 2x baseline for 15+ minutes
- > 10 tenant complaints within 1 hour
- Feature not working as expected (regression)

See `rollback-procedure.md` for detailed rollback steps.

---

## 5. Post-Deploy Checklist

- [ ] All services healthy (health endpoints return UP)
- [ ] Error rate normal (< 0.1%)
- [ ] No OOMKilled pods in last 30 min
- [ ] Key user flows verified (login, create tenant, subscription)
- [ ] Deploy recorded in `#deployments` channel with version + changelog link
- [ ] Monitoring dashboard bookmarked for next 24h observation

---

## Related

- [Incident Response Runbook](./incident-response-runbook.md)
- [Rollback Procedure](./rollback-procedure.md)
- [Deployment Procedures](./operations/runbooks/deployment-procedures.md)
- [Secret Management](./SECRET-MANAGEMENT.md)
