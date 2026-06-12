# KiteClass Deployment Runbooks

**Version:** 1.0
**Created:** 2026-03-10
**Purpose:** Operational procedures for deploying, rolling back, and troubleshooting KiteClass platform
**Audience:** DevOps, SRE, On-call Engineers

---

## Table of Contents

1. [Standard Deployment](#standard-deployment)
2. [Rollback Procedures](#rollback-procedures)
3. [Database Migrations](#database-migrations)
4. [Troubleshooting Guide](#troubleshooting-guide)
5. [Incident Response](#incident-response)
6. [Emergency Procedures](#emergency-procedures)

---

## Standard Deployment

### Prerequisites Checklist

**Before Every Deployment:**
- [ ] All CI tests passing (unit + integration)
- [ ] Code review approved by 2+ engineers
- [ ] Staging deployment successful
- [ ] Database migrations tested in staging
- [ ] Rollback plan documented
- [ ] On-call engineer notified
- [ ] Deployment window scheduled (avoid peak hours)

**Peak Hours to Avoid:**
- Monday-Friday: 9AM-5PM Vietnam time (business hours)
- Saturday-Sunday: 10AM-2PM (weekend classes)
- **Best Deployment Window:** Tuesday-Thursday 10PM-11PM Vietnam time

---

### Deployment Workflow

#### Step 1: Pre-Deployment Verification

```bash
# 1. Check current production version
kubectl get deployments -n kiteclass -o wide

# 2. Verify all services healthy
kubectl get pods -n kiteclass -w

# 3. Check current error rate (should be < 0.1%)
curl -s http://prometheus:9090/api/v1/query?query='rate(http_server_requests_seconds_count{status=~"5.."}[5m])'

# 4. Verify database connectivity
kubectl exec -it kiteclass-core-0 -n kiteclass -- \
  curl -s http://localhost:8080/actuator/health | jq '.status'
```

**Expected Output:**
```json
{
  "status": "UP",
  "components": {
    "db": {"status": "UP"},
    "redis": {"status": "UP"}
  }
}
```

---

#### Step 2: Deploy New Version (Rolling Update)

**Option A: Kubernetes Rolling Update**

```bash
# 1. Set new image version
export NEW_VERSION="v1.2.3"

# 2. Update Gateway
kubectl set image deployment/kiteclass-gateway \
  gateway=kiteclass/gateway:$NEW_VERSION \
  -n kiteclass

# 3. Watch rollout status
kubectl rollout status deployment/kiteclass-gateway -n kiteclass

# Expected output: "deployment "kiteclass-gateway" successfully rolled out"

# 4. Verify new pods running
kubectl get pods -n kiteclass -l app=kiteclass-gateway

# 5. Repeat for Core Service
kubectl set image deployment/kiteclass-core \
  core=kiteclass/core:$NEW_VERSION \
  -n kiteclass

kubectl rollout status deployment/kiteclass-core -n kiteclass
```

**Option B: GitHub Actions CD Pipeline (Recommended)**

```yaml
# .github/workflows/deploy-production.yml
name: Deploy to Production

on:
  workflow_dispatch:
    inputs:
      version:
        description: 'Version to deploy (e.g., v1.2.3)'
        required: true

jobs:
  deploy:
    runs-on: ubuntu-latest
    environment: production
    steps:
      - name: Checkout
        uses: actions/checkout@v4
        with:
          ref: ${{ github.event.inputs.version }}

      - name: Deploy to Kubernetes
        run: |
          # Authenticate to cluster
          aws eks update-kubeconfig --name kiteclass-prod

          # Deploy with Helm
          helm upgrade kiteclass ./infrastructure/k8s/infrastructure/helm/kiteclass \
            --namespace kiteclass \
            --set image.tag=${{ github.event.inputs.version }} \
            --wait --timeout 10m

      - name: Run smoke tests
        run: ./scripts/smoke-tests.sh

      - name: Notify Slack
        uses: slackapi/slack-github-action@v1
        with:
          payload: |
            {
              "text": "✅ Production deployment complete: ${{ github.event.inputs.version }}"
            }
```

**Trigger Deployment:**
```bash
# Via GitHub UI: Actions → Deploy to Production → Run workflow
# Or via GitHub CLI:
gh workflow run deploy-production.yml -f version=v1.2.3
```

---

#### Step 3: Post-Deployment Verification

**Automated Smoke Tests:**

```bash
#!/bin/bash
# scripts/smoke-tests.sh

set -e

BASE_URL="https://api.kitehub.me"

echo "🔍 Running smoke tests..."

# Test 1: Health Check
echo "1. Health check..."
STATUS=$(curl -s $BASE_URL/actuator/health | jq -r '.status')
if [ "$STATUS" != "UP" ]; then
  echo "❌ Health check failed: $STATUS"
  exit 1
fi
echo "✅ Health check passed"

# Test 2: Authentication
echo "2. Authentication..."
TOKEN=$(curl -s -X POST $BASE_URL/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"test@example.com","password":"test123"}' \
  | jq -r '.accessToken')

if [ -z "$TOKEN" ] || [ "$TOKEN" = "null" ]; then
  echo "❌ Authentication failed"
  exit 1
fi
echo "✅ Authentication passed"

# Test 3: Core API
echo "3. Core API..."
HTTP_CODE=$(curl -s -o /dev/null -w "%{http_code}" \
  $BASE_URL/api/v1/students \
  -H "Authorization: Bearer $TOKEN")

if [ "$HTTP_CODE" != "200" ]; then
  echo "❌ Core API failed: HTTP $HTTP_CODE"
  exit 1
fi
echo "✅ Core API passed"

# Test 4: Database connectivity
echo "4. Database connectivity..."
STUDENT_COUNT=$(curl -s $BASE_URL/api/v1/students \
  -H "Authorization: Bearer $TOKEN" \
  | jq '.totalElements')

if [ -z "$STUDENT_COUNT" ]; then
  echo "❌ Database query failed"
  exit 1
fi
echo "✅ Database connectivity passed (found $STUDENT_COUNT students)"

echo ""
echo "🎉 All smoke tests passed!"
```

**Run Smoke Tests:**
```bash
./scripts/smoke-tests.sh
```

---

**Manual Verification:**

```bash
# 1. Check error rate (should remain < 0.1%)
curl -s 'http://prometheus:9090/api/v1/query?query=rate(http_server_requests_seconds_count{status=~"5.."}[5m])' \
  | jq '.data.result[].value[1]'

# 2. Check response time p95 (should be < 500ms)
curl -s 'http://prometheus:9090/api/v1/query?query=histogram_quantile(0.95, rate(http_server_requests_seconds_bucket[5m]))' \
  | jq '.data.result[].value[1]'

# 3. Monitor logs for errors
kubectl logs -f deployment/kiteclass-gateway -n kiteclass --tail=50 | grep ERROR

# 4. Check Grafana dashboard
# Open: http://grafana.kitehub.me/d/service-overview
```

**Watch for 15 minutes** after deployment to ensure stability.

---

#### Step 4: Notify Stakeholders

**Slack Notification:**
```bash
curl -X POST https://hooks.slack.com/services/YOUR/WEBHOOK/URL \
  -H 'Content-Type: application/json' \
  -d '{
    "text": "✅ Production Deployment Complete",
    "blocks": [
      {
        "type": "section",
        "text": {
          "type": "mrkdwn",
          "text": "*Production Deployment Complete*\n\n*Version:* v1.2.3\n*Services:* Gateway, Core\n*Deployed by:* @engineer\n*Status:* All smoke tests passed ✅"
        }
      }
    ]
  }'
```

---

## Rollback Procedures

### When to Rollback

**Immediate Rollback If:**
- Error rate > 1% for 5+ minutes
- Critical functionality broken (login, payment, data loss)
- Database corruption detected
- Security vulnerability exploited
- Service completely down

**Consider Rollback If:**
- Error rate > 0.5% for 10+ minutes
- Response time p95 > 2 seconds
- Widespread user complaints
- Bug affecting > 10% of users

---

### Rollback Workflow

#### Fast Rollback (Kubernetes)

```bash
# 1. Find previous deployment version
kubectl rollout history deployment/kiteclass-gateway -n kiteclass

# Output:
# REVISION  CHANGE-CAUSE
# 1         <none>
# 2         Updated to v1.2.2
# 3         Updated to v1.2.3 (current)

# 2. Rollback to previous version
kubectl rollout undo deployment/kiteclass-gateway -n kiteclass

# 3. Watch rollback progress
kubectl rollout status deployment/kiteclass-gateway -n kiteclass --watch

# 4. Verify pods restarted
kubectl get pods -n kiteclass -l app=kiteclass-gateway

# 5. Repeat for all affected services
kubectl rollout undo deployment/kiteclass-core -n kiteclass
kubectl rollout undo deployment/kiteclass-frontend -n kiteclass
```

**Rollback Time:** ~2-3 minutes for complete rollback

---

#### Rollback to Specific Version

```bash
# 1. Check deployment history
kubectl rollout history deployment/kiteclass-gateway -n kiteclass

# 2. Rollback to revision 2
kubectl rollout undo deployment/kiteclass-gateway --to-revision=2 -n kiteclass

# 3. Verify rollback
kubectl describe deployment kiteclass-gateway -n kiteclass | grep Image
```

---

#### Post-Rollback Verification

```bash
# 1. Run smoke tests again
./scripts/smoke-tests.sh

# 2. Check error rate normalized
curl -s 'http://prometheus:9090/api/v1/query?query=rate(http_server_requests_seconds_count{status=~"5.."}[5m])'

# 3. Verify user-facing features
# Manually test: login, student creation, course enrollment

# 4. Notify team
# Slack: "⚠️ Rolled back to v1.2.2 due to high error rate"
```

---

### Database Rollback

**CRITICAL: Database rollbacks are DESTRUCTIVE and should be last resort**

**Option A: Restore from Backup**

```bash
# 1. Stop all services (prevent new writes)
kubectl scale deployment/kiteclass-core --replicas=0 -n kiteclass

# 2. Download latest backup
aws s3 cp s3://kiteclass-backups/production/2026-03-09-23-00.sql.gz .

# 3. Restore database
gunzip 2026-03-09-23-00.sql.gz
psql -h postgres.kitehub.me -U kiteclass -d kiteclass < 2026-03-09-23-00.sql

# 4. Restart services
kubectl scale deployment/kiteclass-core --replicas=3 -n kiteclass

# 5. Verify data integrity
psql -h postgres.kitehub.me -U kiteclass -d kiteclass -c "SELECT COUNT(*) FROM students;"
```

**Option B: Revert Migration**

```bash
# 1. Connect to database
kubectl port-forward svc/postgres 5432:5432 -n kiteclass

# 2. Check current Flyway version
psql -h localhost -U kiteclass -d kiteclass \
  -c "SELECT version FROM flyway_schema_history ORDER BY installed_rank DESC LIMIT 1;"

# 3. Manually revert migration
# WARNING: Write manual DOWN migration script
psql -h localhost -U kiteclass -d kiteclass < migrations/V13__undo_problematic_change.sql

# 4. Update Flyway history
psql -h localhost -U kiteclass -d kiteclass \
  -c "DELETE FROM flyway_schema_history WHERE version = '13';"
```

---

## Database Migrations

### Safe Migration Practices

**Zero-Downtime Migration Pattern:**

1. **Phase 1: Add new column (nullable)**
   ```sql
   -- V13__add_student_status.sql
   ALTER TABLE students ADD COLUMN status VARCHAR(50);
   ```

2. **Phase 2: Backfill data (separate deployment)**
   ```sql
   -- V14__backfill_student_status.sql
   UPDATE students SET status = 'ACTIVE' WHERE deleted = false;
   UPDATE students SET status = 'INACTIVE' WHERE deleted = true;
   ```

3. **Phase 3: Make column NOT NULL (after verification)**
   ```sql
   -- V15__student_status_not_null.sql
   ALTER TABLE students ALTER COLUMN status SET NOT NULL;
   ```

---

### Running Migrations

**Automatic (via Flyway on startup):**

```yaml
# application.yml
spring:
  flyway:
    enabled: true
    baseline-on-migrate: true
    validate-on-migrate: true
```

Flyway runs migrations automatically when service starts.

---

**Manual (for complex migrations):**

```bash
# 1. Scale service to 0 replicas (prevent conflicts)
kubectl scale deployment/kiteclass-core --replicas=0 -n kiteclass

# 2. Run migration job
kubectl apply -f - <<EOF
apiVersion: batch/v1
kind: Job
metadata:
  name: flyway-migrate
  namespace: kiteclass
spec:
  template:
    spec:
      containers:
      - name: flyway
        image: flyway/flyway:10.4
        command:
          - flyway
          - migrate
          - -url=jdbc:postgresql://postgres:5432/kiteclass
          - -user=kiteclass
          - -password=${DB_PASSWORD}
          - -locations=filesystem:/migrations
        volumeMounts:
          - name: migrations
            mountPath: /migrations
      volumes:
        - name: migrations
          configMap:
            name: flyway-migrations
      restartPolicy: Never
EOF

# 3. Watch job completion
kubectl logs -f job/flyway-migrate -n kiteclass

# 4. Verify migration
psql -h postgres -U kiteclass -d kiteclass \
  -c "SELECT * FROM flyway_schema_history ORDER BY installed_rank DESC LIMIT 5;"

# 5. Scale service back up
kubectl scale deployment/kiteclass-core --replicas=3 -n kiteclass
```

---

### Migration Rollback

**If migration fails:**

```bash
# 1. Check Flyway history
psql -c "SELECT * FROM flyway_schema_history WHERE success = false;"

# 2. Mark failed migration as resolved
psql -c "DELETE FROM flyway_schema_history WHERE version = '13' AND success = false;"

# 3. Fix migration script (V13__*.sql)
# - Correct SQL syntax
# - Add missing constraints
# - Test in staging first

# 4. Retry migration
kubectl rollout restart deployment/kiteclass-core -n kiteclass
```

---

## Troubleshooting Guide

### Service Won't Start

**Symptom:** Pods in `CrashLoopBackOff` status

**Diagnosis:**
```bash
# 1. Check pod logs
kubectl logs -f kiteclass-core-abc123 -n kiteclass

# Common errors:
# - "Connection refused" → Database not reachable
# - "Authentication failed" → Wrong credentials
# - "Flyway migration failed" → Database schema issue
# - "OutOfMemoryError" → Insufficient memory
```

**Solutions:**

```bash
# Database Connection Issue
# Check database is running
kubectl get pods -n kiteclass -l app=postgres

# Verify database credentials
kubectl get secret kiteclass-db-secret -n kiteclass -o yaml | yq '.data.password' | base64 -d

# Test connection manually
kubectl run -it --rm debug --image=postgres:15 --restart=Never -- \
  psql -h postgres.kiteclass.svc.cluster.local -U kiteclass -d kiteclass

# Flyway Migration Issue
# Check migration history
kubectl exec -it postgres-0 -n kiteclass -- \
  psql -U kiteclass -d kiteclass -c "SELECT * FROM flyway_schema_history WHERE success = false;"

# Memory Issue
# Increase memory limit
kubectl set resources deployment/kiteclass-core \
  --limits=memory=2Gi \
  --requests=memory=1Gi \
  -n kiteclass
```

---

### High Error Rate

**Symptom:** Error rate > 1%, users reporting 500 errors

**Diagnosis:**
```bash
# 1. Check Prometheus alert
curl 'http://prometheus:9090/api/v1/query?query=rate(http_server_requests_seconds_count{status=~"5.."}[5m])'

# 2. Check recent logs for errors
kubectl logs --tail=100 deployment/kiteclass-core -n kiteclass | grep ERROR

# 3. Check Jaeger for failed traces
# Open: http://jaeger.kitehub.me/search
# Filter: service=kiteclass-core, status=error

# 4. Check database connections
curl http://kiteclass-core:8080/actuator/metrics/hikaricp.connections.active
```

**Common Causes:**

1. **Database Connection Pool Exhausted**
   ```bash
   # Check active connections
   kubectl exec postgres-0 -n kiteclass -- \
     psql -U postgres -c "SELECT COUNT(*) FROM pg_stat_activity WHERE datname='kiteclass';"

   # Increase pool size
   # Edit deployment: SPRING_DATASOURCE_HIKARI_MAXIMUM_POOL_SIZE=20
   ```

2. **Redis Connection Failed**
   ```bash
   # Check Redis status
   kubectl exec -it redis-0 -n kiteclass -- redis-cli PING

   # Clear cache (if corrupted)
   kubectl exec -it redis-0 -n kiteclass -- redis-cli FLUSHDB
   ```

3. **External Service Timeout (OpenAI, Payment Gateway)**
   ```bash
   # Check circuit breaker status
   curl http://kiteclass-branding:8080/actuator/circuitbreakers

   # Temporarily disable feature if external service down
   kubectl set env deployment/kiteclass-branding OPENAI_ENABLED=false -n kiteclass
   ```

---

### Slow Response Time

**Symptom:** p95 latency > 2 seconds

**Diagnosis:**
```bash
# 1. Check Grafana latency dashboard
# Open: http://grafana.kitehub.me/d/service-latency

# 2. Find slowest endpoints
curl 'http://prometheus:9090/api/v1/query?query=topk(10, histogram_quantile(0.95, rate(http_server_requests_seconds_bucket[5m])))'

# 3. Check database query performance
kubectl exec postgres-0 -n kiteclass -- \
  psql -U kiteclass -d kiteclass -c "
    SELECT query, mean_exec_time, calls
    FROM pg_stat_statements
    ORDER BY mean_exec_time DESC
    LIMIT 10;
  "

# 4. Check for N+1 queries in logs
kubectl logs deployment/kiteclass-core -n kiteclass | grep "Hibernate:"
```

**Solutions:**

```bash
# Add database index
kubectl exec postgres-0 -n kiteclass -- \
  psql -U kiteclass -d kiteclass -c "
    CREATE INDEX CONCURRENTLY idx_students_email ON students(email);
  "

# Enable Redis caching
kubectl set env deployment/kiteclass-core SPRING_CACHE_TYPE=redis -n kiteclass

# Scale up replicas (horizontal scaling)
kubectl scale deployment/kiteclass-core --replicas=5 -n kiteclass
```

---

### Out of Memory

**Symptom:** Pod killed with `OOMKilled` status

**Diagnosis:**
```bash
# 1. Check pod events
kubectl describe pod kiteclass-core-abc123 -n kiteclass

# Look for: "reason: OOMKilled, container: core, last state: terminated"

# 2. Check memory usage trend
curl 'http://prometheus:9090/api/v1/query?query=container_memory_usage_bytes{pod=~"kiteclass-core.*"}'

# 3. Check JVM heap usage
curl http://kiteclass-core:8080/actuator/metrics/jvm.memory.used
```

**Solutions:**

```bash
# 1. Increase memory limit
kubectl set resources deployment/kiteclass-core \
  --limits=memory=2Gi \
  --requests=memory=1Gi \
  -n kiteclass

# 2. Tune JVM heap size
kubectl set env deployment/kiteclass-core \
  JAVA_OPTS="-Xmx1536m -Xms1024m -XX:+UseG1GC" \
  -n kiteclass

# 3. Enable heap dump on OOM (for analysis)
kubectl set env deployment/kiteclass-core \
  JAVA_OPTS="-XX:+HeapDumpOnOutOfMemoryError -XX:HeapDumpPath=/tmp/heapdump.hprof" \
  -n kiteclass

# 4. Analyze heap dump
kubectl cp kiteclass-core-abc123:/tmp/heapdump.hprof ./heapdump.hprof -n kiteclass
# Use Eclipse MAT or VisualVM to analyze
```

---

## Incident Response

### Incident Severity Levels

| Level | Description | Response Time | Example |
|-------|-------------|---------------|---------|
| **P0** | Critical - Service down for all users | 15 minutes | Database crashed, all services down |
| **P1** | High - Major functionality broken | 1 hour | Payment processing failing |
| **P2** | Medium - Partial functionality affected | 4 hours | One course's students can't login |
| **P3** | Low - Minor issue, workaround available | 24 hours | UI button misaligned |

---

### P0 Incident Response

**1. Acknowledge (0-5 minutes)**
```bash
# PagerDuty alert received → Acknowledge immediately
# Post in Slack #incidents: "P0 incident - investigating"
```

**2. Assess (5-10 minutes)**
```bash
# Check what's down
kubectl get pods -n kiteclass --field-selector=status.phase!=Running

# Check error rate
curl 'http://prometheus:9090/api/v1/query?query=rate(http_server_requests_seconds_count{status=~"5.."}[5m])'

# Check recent deployments
kubectl rollout history deployment/kiteclass-core -n kiteclass
```

**3. Mitigate (10-30 minutes)**
```bash
# Option 1: Rollback if recent deployment
kubectl rollout undo deployment/kiteclass-core -n kiteclass

# Option 2: Scale up if resource issue
kubectl scale deployment/kiteclass-core --replicas=10 -n kiteclass

# Option 3: Failover if database issue
# Switch to read replica (if available)
kubectl set env deployment/kiteclass-core \
  SPRING_DATASOURCE_URL=jdbc:postgresql://postgres-replica:5432/kiteclass \
  -n kiteclass
```

**4. Communicate (Throughout)**
```bash
# Update Slack every 15 minutes:
# "10:30 - Incident confirmed: Database connection pool exhausted"
# "10:45 - Mitigation: Increased pool size from 10 to 50"
# "11:00 - Resolution: Error rate back to normal. Monitoring."

# Update status page: https://status.kitehub.me
```

**5. Resolve (30-60 minutes)**
```bash
# Verify system stable
./scripts/smoke-tests.sh

# Post-incident update
# Slack: "✅ P0 incident resolved. Root cause: database connection pool exhaustion. Fix: increased pool size. Post-mortem scheduled."
```

**6. Post-Mortem (Within 3 days)**
- What happened?
- Why did it happen?
- How did we respond?
- How do we prevent it?
- Action items with owners

---

## Emergency Procedures

### Total System Failure

**If all services down:**

```bash
# 1. Check Kubernetes cluster health
kubectl get nodes
kubectl get pods --all-namespaces

# 2. Check control plane
kubectl get componentstatuses

# 3. Restart entire namespace (LAST RESORT)
kubectl delete pods --all -n kiteclass

# Services will be recreated by deployments

# 4. Verify services come back up
watch kubectl get pods -n kiteclass
```

---

### Database Disaster Recovery

**If production database corrupted:**

```bash
# 1. STOP ALL WRITES IMMEDIATELY
kubectl scale deployment --all --replicas=0 -n kiteclass

# 2. Assess damage
psql -h postgres.kitehub.me -U postgres -c "SELECT pg_database_size('kiteclass');"

# 3. Restore from latest backup
aws s3 cp s3://kiteclass-backups/production/latest.sql.gz .
gunzip latest.sql.gz

# Drop and recreate database
psql -h postgres.kitehub.me -U postgres -c "DROP DATABASE kiteclass;"
psql -h postgres.kitehub.me -U postgres -c "CREATE DATABASE kiteclass;"

# Restore
psql -h postgres.kitehub.me -U kiteclass -d kiteclass < latest.sql

# 4. Verify data integrity
psql -h postgres.kitehub.me -U kiteclass -d kiteclass -c "
  SELECT COUNT(*) FROM students;
  SELECT COUNT(*) FROM courses;
  SELECT COUNT(*) FROM invoices;
"

# 5. Bring services back online
kubectl scale deployment --all --replicas=3 -n kiteclass

# 6. Run smoke tests
./scripts/smoke-tests.sh
```

**Data Loss:** Up to 1 hour (time since last backup)

---

### Security Breach Response

**If security incident detected:**

```bash
# 1. Contain immediately
# Revoke all JWT tokens
kubectl exec redis-0 -n kiteclass -- redis-cli FLUSHDB

# Block attacker IP
kubectl apply -f - <<EOF
apiVersion: networking.k8s.io/v1
kind: NetworkPolicy
metadata:
  name: block-attacker
  namespace: kiteclass
spec:
  podSelector: {}
  policyTypes:
  - Ingress
  ingress:
  - from:
    - ipBlock:
        cidr: 0.0.0.0/0
        except:
        - 1.2.3.4/32  # Attacker IP
EOF

# 2. Preserve evidence
kubectl logs deployment/kiteclass-gateway -n kiteclass --tail=10000 > incident-logs.txt
aws s3 cp incident-logs.txt s3://kiteclass-security-incidents/$(date +%Y%m%d-%H%M%S)/

# 3. Notify
# Email: security@kitehub.me
# Slack: #security-incidents
# PagerDuty: Escalate to security team

# 4. Investigate
# Review audit logs
# Check for data exfiltration
# Identify vulnerability

# 5. Remediate
# Patch vulnerability
# Reset all passwords (if credential leak)
# Deploy security fix
```

---

## Related Documentation

- [Monitoring & Observability](./monitoring-observability.md)
- [Security Design](../../04-quality/security-design.md)
- [KiteHub Infrastructure](../implementation/kitehub-infrastructure.md)

---

**Last Updated:** 2026-03-10
**Status:** Production-ready procedures
