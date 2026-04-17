# DevOps Standards

**Version:** 2.0 (Consolidated)
**Gop tu:** ci-cd-best-practices, ci-cd-quality-enforcement, ci-cleanup-workflow,
deployment-quality-standards, cloud-infrastructure, log-management,
environment-setup, setup-github-cli, docker-scripts-required

---

## Muc luc nhanh

| Can gi | Xem section |
|--------|-------------|
| Docker scripts (BAT BUOC) | [1. Docker Scripts](#1-docker-scripts-bat-buoc) |
| CI/CD quality gates | [2. CI/CD Pipeline](#2-cicd-pipeline) |
| GitHub Actions versions | [2.2 Actions Versions](#22-github-actions-versions-2026) |
| Deployment / zero-downtime | [3. Deployment](#3-deployment-standards) |
| Cloud architecture | [4. Cloud Infrastructure](#4-cloud-infrastructure) |
| Log management | [5. Log Management](#5-log-management) |
| Environment setup | [6. Environment Setup](#6-environment-setup) |
| CI cleanup | [7. CI Cleanup](#7-ci-cleanup) |

---

## 1. Docker Scripts (BAT BUOC)

**KHONG BAO GIO** chay lenh Docker truc tiep. **LUON LUON** dung scripts.

```bash
# BAD - NEVER
docker-compose -f docker-compose.kitehub.yml up -d

# GOOD - ALWAYS
./scripts/up.sh
```

### Scripts Reference (kitehub/scripts/)

| Script | Thay the cho | Mo ta |
|--------|-------------|-------|
| `up.sh` | `docker-compose up -d` | Start stack |
| `down.sh` | `docker-compose down` | Stop stack |
| `down.sh --volumes` | `docker-compose down -v` | Stop + remove data |
| `logs.sh [service] -f` | `docker-compose logs -f` | View logs |
| `build-all.sh` | `docker build` all | Build all images |
| `rebuild.sh [service]` | `docker-compose build` + up | Rebuild single service |
| `restart.sh [service]` | `docker-compose restart` | Restart service |
| `exec.sh [service]` | `docker exec -it` | Run command in container |
| `status.sh --health` | `docker ps` | Check status + health |
| `clean.sh --all` | `docker system prune` | Full cleanup |
| `help.sh` | - | Show all commands |

**Ly do dung scripts:**
1. Consistency: Dung dung project name, dung compose file
2. Safety: Confirmation cho destructive operations
3. Convenience: Defaults hop ly, giam typing
4. Best practices: Dung dung thu tu build (base → children)

---

## 2. CI/CD Pipeline

### Quality Gates (BAT BUOC pass truoc khi merge)

| Gate | Requirement | Blocking |
|------|-------------|----------|
| Unit Tests | All pass | Yes |
| Integration Tests | All pass | Yes |
| Test Coverage | >= 80% line coverage | Yes |
| Security Scan | No HIGH/CRITICAL CVEs | Yes |
| Linting | 0 errors | Yes |
| Build | Successful | Yes |

### Branch Protection Rules

```yaml
# .github/branch-protection.yml (conceptual)
main:
  required_status_checks:
    - build-and-test
    - security-scan
    - coverage-check
  required_pull_request_reviews: 1
  dismiss_stale_reviews: true
```

### Standard Workflow Template

```yaml
name: CI

on:
  push:
    branches: [main, 'wave/*', 'feature/*']
  pull_request:
    branches: [main, 'wave/*']

permissions:
  contents: read
  checks: write
  security-events: write

jobs:
  test:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      - uses: actions/setup-java@v4
        with:
          java-version: '21'
          distribution: 'temurin'
          cache: 'maven'
      - name: Build and Test
        run: mvn verify -P coverage
      - name: Upload coverage
        uses: codecov/codecov-action@v5
```

### 2.2 GitHub Actions Versions (2026)

```yaml
# CURRENT VERSIONS - keep up-to-date
actions/checkout@v4                           # Stable
actions/setup-java@v4                         # Stable
actions/setup-node@v4                         # Stable
actions/cache@v4                              # v3 deprecated
actions/upload-artifact@v4                    # v3 deprecated Apr 2024
codecov/codecov-action@v5                     # v3 outdated
github/codeql-action/upload-sarif@v4          # v2 deprecated
EnricoMi/publish-unit-test-result-action@v2   # Stable
actions/github-script@v7                      # Stable
aquasecurity/trivy-action@master              # Use master
```

**Sau khi upgrade actions:**
1. Check release notes cho breaking changes
2. Update permissions neu can
3. Test workflow run sau khi upgrade

---

## 3. Deployment Standards

### Environments

| Environment | Purpose | Trigger | Approval |
|-------------|---------|---------|----------|
| Local | Development | Manual | No |
| Staging | QA/Testing | Auto (main) | No |
| Production | Live users | Manual (tagged release) | Yes |

### Zero-Downtime Deployment

```yaml
# Kubernetes rolling update
spec:
  strategy:
    type: RollingUpdate
    rollingUpdate:
      maxSurge: 1
      maxUnavailable: 0  # Never reduce capacity during deploy
  readinessProbe:
    httpGet:
      path: /actuator/health/readiness
      port: 8080
    initialDelaySeconds: 30
    periodSeconds: 10
```

### Database Migration Safety

```properties
# Flyway config
spring.flyway.enabled=true
spring.flyway.validate-on-migrate=true
spring.flyway.out-of-order=false
```

Migration rules:
1. **Idempotent**: safe to run multiple times
2. **Backward compatible**: old code works with new schema during rollout
3. **No data loss**: NEVER DROP without retention period
4. **Transactional**: use transactions where DB supports DDL

### Rollback Strategy

```bash
# Fast rollback (< 5 minutes)
# 1. Kubernetes: rollback deployment
kubectl rollout undo deployment/kiteclass-core

# 2. Database: Flyway repair (if migration failed)
mvn flyway:repair

# 3. Feature flags: disable problematic feature
curl -X POST /api/admin/features/new-feature/disable
```

### Deployment Checklist

Pre-deploy:
- [ ] All tests pass in CI
- [ ] Security scan: no HIGH/CRITICAL CVEs
- [ ] Migration scripts reviewed
- [ ] Rollback plan ready
- [ ] Stakeholders notified (production)

Post-deploy:
- [ ] Smoke tests pass
- [ ] Error rate normal (< 0.1%)
- [ ] Response times normal
- [ ] Database migrations applied

---

## 4. Cloud Infrastructure

### AWS Services Stack

```
Route 53 (DNS)
  └── CloudFront (CDN) - static assets + Next.js
      └── Application Load Balancer (SSL termination)
          └── Amazon EKS Cluster
              ├── KiteHub Backend (Pod)
              ├── KiteClass Gateway (Pod)
              └── KiteClass Core (Pod)
                  ├── RDS PostgreSQL (per tenant DB)
                  ├── ElastiCache Redis (cache)
                  └── Amazon MQ RabbitMQ (messaging)
```

### Kubernetes Resource Requests

```yaml
resources:
  requests:
    memory: "256Mi"
    cpu: "250m"
  limits:
    memory: "512Mi"
    cpu: "500m"
```

### Monitoring Stack

```
Prometheus -> Grafana dashboards
  ├── API response times (P50, P95, P99)
  ├── Error rates per service
  ├── Cache hit rates
  └── Database query times
```

**Alerts:**
- Error rate > 1% → PagerDuty
- P95 latency > 500ms → Slack
- Cache hit rate < 80% → Slack

---

## 5. Log Management

### Log Structure (JSON)

```json
{
  "timestamp": "2026-03-23T10:00:00Z",
  "level": "INFO",
  "service": "kiteclass-core",
  "instanceId": "uuid-here",
  "traceId": "abc123",
  "message": "Student created",
  "studentId": 456
}
```

### What NOT to Commit

```
.log/
├── check-ci.sh    # OK - commit this (reusable script)
├── *.txt          # NO - downloaded CI logs (gitignored)
├── *.log          # NO - application logs (gitignored)
└── */             # NO - temp directories (gitignored)
```

### Log Retention

| Location | Retention |
|----------|-----------|
| Local dev | Delete logs > 7 days |
| GitHub Actions | Auto-delete after 90 days |
| Production (CloudWatch) | 30 days INFO, 90 days ERROR |

---

## 6. Environment Setup

### Required Tools

| Tool | Version | Install |
|------|---------|---------|
| JDK | 17+ (or 21) | [Adoptium](https://adoptium.net/) |
| Node.js | 20 LTS | [nodejs.org](https://nodejs.org/) |
| pnpm | 8+ | `npm install -g pnpm` |
| Docker | 24+ | [docker.com](https://docker.com/) |
| Docker Compose | 2.20+ | Included with Docker |
| Git | 2.40+ | [git-scm.com](https://git-scm.com/) |
| gh CLI | latest | `apt install gh` |

### Verify Installation

```bash
java --version       # Java 17 or 21
node --version       # v20.x
pnpm --version       # 8.x
docker --version     # 24.x
docker compose version  # 2.20+
gh --version         # 2.x
```

### GitHub CLI Setup

```bash
# Ubuntu/WSL2
curl -fsSL https://cli.github.com/packages/githubcli-archive-keyring.gpg \
  | sudo dd of=/usr/share/keyrings/githubcli-archive-keyring.gpg
echo "deb [arch=$(dpkg --print-architecture) signed-by=/usr/share/keyrings/githubcli-archive-keyring.gpg] \
  https://cli.github.com/packages stable main" \
  | sudo tee /etc/apt/sources.list.d/github-cli.list > /dev/null
sudo apt update && sudo apt install gh

# Authenticate
gh auth login --web
```

### IntelliJ IDEA Setup

```
File > Settings > Build, Execution, Deployment > Build Tools > Maven
  -> Maven home: Use bundled or system Maven

File > Settings > Editor > Code Style > Java
  -> Scheme: GoogleStyle (import from checkstyle/google_checks.xml)

Plugins needed:
  - Lombok
  - MapStruct Support
  - Docker
  - PlantUML Integration
```

---

## 7. CI Cleanup

### When to Clean

**Run AFTER:**
- Merging feature branch to main
- Closing stale PRs (> 30 days no activity)
- Completing a milestone

**DON'T run on:**
- main/develop/release branches (preserve history)
- Active feature branches
- When debugging CI failures

### Preservation Rules

| Keep | Delete |
|------|--------|
| All runs on main/develop/release | Failed runs on merged branches (> 7 days) |
| Last run per workflow on merged branch | Duplicate runs (keep only latest per commit) |
| Runs from last 7 days | Runs from closed/abandoned PRs |

### Cleanup Script

```bash
# Delete failed runs on a merged branch
gh run list --branch feature/my-feature --json databaseId,status \
  | jq '.[] | select(.status == "failure") | .databaseId' \
  | xargs -I {} gh run delete {}
```
