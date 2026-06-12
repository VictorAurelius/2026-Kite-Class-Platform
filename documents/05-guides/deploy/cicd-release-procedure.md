# CI/CD Release Procedure

> Last updated: 2026-04-18 | Owner: DevOps / Release manager

Step-by-step procedure từ PR merge → deploy to production. Covers normal releases (feature waves) and hotfixes.

---

## 1. Release Cadence

| Track | Cadence | Trigger |
|-------|---------|---------|
| **Feature release** | End of each wave (2-4 weeks) | Wave completion check passed |
| **Hotfix** | On-demand | P0/P1 bug confirmed in prod |
| **Dependency updates** | Weekly automated | Dependabot PRs |
| **Staging deploy** | On every main merge | Auto-trigger |
| **Production deploy** | Manual approval | Post-staging smoke pass |

---

## 2. Release Types

### 2.1 Standard Feature Release (Wave-based)

**Preconditions:**
- [ ] Wave branch merged into `main`
- [ ] All gaps in wave marked DONE
- [ ] Wave completion check passed (see `.claude/skills/workflow/wave-completion-check.md`)
- [ ] Deploy go/no-go checklist signed ([`deploy-go-nogo-checklist.md`](deploy-go-nogo-checklist.md))
- [ ] Release notes drafted

**Procedure:** follow Section 4 below.

### 2.2 Hotfix Release

**Preconditions:**
- [ ] P0/P1 bug confirmed reproducible in prod
- [ ] Fix localized (≤3 files, no schema change ideally)
- [ ] Tests cover the bug
- [ ] Incident ticket created per [`incident-response-runbook.md`](incident-response-runbook.md)

**Procedure:**
```bash
# 1. Branch from current production tag
git fetch --tags
git checkout -b hotfix/PR-NNN-description tags/v1.2.3

# 2. Apply minimal fix + test
# ... code + test ...

# 3. PR to main với label "hotfix"
gh pr create --base main --label hotfix --title "hotfix: <description>"

# 4. Expedited review (1 reviewer OK nếu P0)
# 5. Merge + deploy with Section 4, skip staging soak time
```

---

## 3. Versioning Scheme

**Semantic-ish versioning** for releases:
- `vMAJOR.MINOR.PATCH`
- `MAJOR` — breaking API change (rare, plan 1 wave in advance)
- `MINOR` — feature wave shipped (default for each wave completion)
- `PATCH` — hotfix, dependency bump, docs

**Current version:** check `git describe --tags --abbrev=0` on main.

---

## 4. Production Deploy Procedure

### Step 1 — Pre-flight (T-30 min)

```bash
# 1.1 Verify main is clean
git checkout main && git pull --ff-only
git status    # expect clean

# 1.2 Verify CI green
gh run list --branch main --limit 5
./scripts/check-ci.sh main --status

# 1.3 Run go/no-go checklist
# (follow 05-guides/deploy/deploy-go-nogo-checklist.md)

# 1.4 Announce in #releases Slack
# Template: "Deploying v1.2.3 to prod in 15 min. Changelog: <link>. Rollback: git revert <sha>."
```

### Step 2 — Tag Release

```bash
# Tag main với version mới
VERSION=v1.2.3
git tag -a $VERSION -m "Release $VERSION — Wave N complete"
git push origin $VERSION

# Create GitHub release với changelog
gh release create $VERSION --generate-notes --latest
```

### Step 3 — Deploy Trigger

```bash
# Manual trigger production workflow (GitHub Actions)
gh workflow run deploy-production.yml --ref $VERSION \
  -f confirmation=DEPLOY \
  -f rollback_version=$(git describe --tags --abbrev=0 HEAD~1)

# Monitor trong GH Actions UI
gh run watch
```

### Step 4 — Post-deploy Verification (T+15 min)

```bash
# 4.1 Health checks
for svc in gateway subscription branding email admin; do
  curl -f https://api.kitehub.me/$svc/actuator/health || echo "FAIL: $svc"
done

# 4.2 Smoke test
./scripts/smoke-test-prod.sh   # TODO: GAP-089

# 4.3 Check Grafana dashboards (Wave 6)
# - Queue depth, error rate, P95 latency

# 4.4 Watch logs 15 min
kubectl logs -n kitehub -l app.kubernetes.io/managed-by=Helm \
  --since=15m --tail=100 | grep -iE "error|exception|5[0-9]{2}"
```

### Step 5 — Announce + Close

```bash
# Update #releases
# Template: "v1.2.3 deployed + verified. No regressions. Next: <next wave>."

# Update PR index (if wave release)
# Edit documents/03-planning/prs/00-master-pr-index.md
```

---

## 5. Rollback Triggers

Immediately rollback nếu phát hiện trong 30 phút đầu:

- ❌ Error rate > 2x baseline (check Prometheus `kitehub_http_requests_total` by status)
- ❌ P95 latency > 2x baseline (check `http_request_duration_seconds`)
- ❌ Health check failures on any service > 1 consecutive
- ❌ User-reported data issue (wrong data, missing feature)
- ❌ Security alert (unexpected auth failures, suspicious traffic)

Follow [`rollback-procedure.md`](rollback-procedure.md) for specific steps.

---

## 6. Staging Deploy (Auto)

Staging deploys automatic mỗi lần main merge. Procedure:

```yaml
# .github/workflows/deploy-staging.yml (existing)
on:
  push:
    branches: [main]
jobs:
  build-and-deploy:
    # 1. Build Docker images per service
    # 2. Push to ghcr.io với tag :main-{sha7}
    # 3. helm upgrade --install trên Oracle Cloud staging cluster
    # 4. Smoke test (basic health checks)
    # 5. Notify #engineering
```

Không cần manual action. Failure trong staging blocks promote to prod.

---

## 7. Dependency Updates

Dependabot tạo PRs cho:
- Maven dependencies (weekly)
- npm packages (weekly)
- Docker base images (monthly)
- GitHub Actions (monthly)

**Review procedure:**
- Low risk (patch bumps): auto-merge after CI green
- Medium risk (minor bumps): 1 reviewer, CI green
- High risk (major bumps, security patches): full review + test suite

---

## 8. Emergency Procedures

### 8.1 Deploy freeze
Trigger: pre-holiday, stakeholder event, wave branch in-flight.

```bash
# Disable auto-staging deploy
gh workflow disable deploy-staging.yml

# Post in #releases: "Deploy freeze until YYYY-MM-DD. Hotfixes only with CTO approval."
```

### 8.2 Hotfix during freeze
Requires CTO + SRE lead approval. Document incident ticket. Deploy following Section 4 with expedited timeline.

### 8.3 Failed production deploy recovery
1. Stop any further deploy attempts
2. Follow [`rollback-procedure.md`](rollback-procedure.md) Step 1-3
3. Verify rollback successful
4. Post-mortem within 48h
5. Create GAP file if missing test coverage caused the bug

---

## 9. Release Checklist Template

Copy this into release PR description:

```markdown
## Release v1.2.3 Checklist

### Pre-flight
- [ ] Main CI green
- [ ] Wave completion check passed
- [ ] Deploy go/no-go signed
- [ ] Release notes drafted
- [ ] Staging soak 24h (waive for hotfix)
- [ ] Rollback tested on staging

### Deploy
- [ ] Tagged
- [ ] GitHub release created
- [ ] Production workflow triggered
- [ ] All 5 services healthy post-deploy
- [ ] Smoke test passed
- [ ] Grafana dashboards green

### Post-deploy
- [ ] #releases announcement posted
- [ ] PR index updated
- [ ] Gap files marked DONE
- [ ] Monitor 30 min, no regressions
```

---

## 10. Related

- [`deploy-go-nogo-checklist.md`](deploy-go-nogo-checklist.md) — pre-deploy gate (GAP-087)
- [`rollback-procedure.md`](rollback-procedure.md) — rollback steps (GAP-088)
- [`incident-response-runbook.md`](incident-response-runbook.md) — when deploy goes wrong (GAP-086)
- [`../02-architecture/deployment-strategy.md`](../02-architecture/deployment-strategy.md) — philosophy
- `.claude/skills/workflow/wave-completion-check.md` — wave gate before release
- `scripts/check-ci.sh` — CI status verification

---

## 11. Log

- **2026-04-18:** Created (GAP-102 Part 1 P2 batch).
