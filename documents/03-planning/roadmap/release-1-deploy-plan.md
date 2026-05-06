---
title: Release Lần 1 Deploy Plan — v0.9.0-beta + v1.0.0 (Phase 1 BETA + Phase 1.5 PAID)
status: active
created: 2026-05-06
updated: 2026-05-06
parent: release-1-plan-2026.md
versioning: versioning-policy.md
target_versions: [v0.9.0-beta, v1.0.0-rc, v1.0.0]
---

# Release Lần 1 Deploy Plan — v0.9.0-beta + v1.0.0

**Trạng thái:** ACTIVE — chốt 2026-05-06.
**Parent plan:** [`release-1-plan-2026.md`](release-1-plan-2026.md)
**Versioning:** [`versioning-policy.md`](versioning-policy.md)

**Mục đích:** Tổng hợp tất cả deploy artifacts (infra + checklists + runbooks + workflows + missing pieces) cho Phase 1 BETA (v0.9.0-beta) và Phase 1.5 PAID (v1.0.0) production launch.

---

## 1. Architecture summary

### 1.1 Dual-cloud strategy (per `kitehub-oracle-cloud-deployment.md`)

```
┌─────────────────────────────────────────────────────┐
│           KiteHub Platform (control plane)           │
│  PRIMARY: Oracle Cloud Always Free ($0/tháng)       │
│  ├── KiteHub services: Spring Boot + Next.js        │
│  ├── PostgreSQL self-hosted on VM                   │
│  ├── Redis self-hosted on VM                        │
│  ├── Ollama AI llama3.1:8b                          │
│  └── Nginx reverse proxy + SSL                      │
│                                                     │
│  BACKUP: AWS ($338/tháng)                           │
│  ├── EKS + RDS + ElastiCache (Terraform/Helm)       │
│  └── Activate khi Oracle fail / capacity issue       │
├─────────────────────────────────────────────────────┤
│        KiteClass Instances (per-tenant)              │
│  AWS (không đổi)                                    │
│  ├── Per-tenant databases (RDS multi-tenant)        │
│  ├── S3 storage (per-tenant prefix)                 │
│  └── CloudFront CDN                                 │
└─────────────────────────────────────────────────────┘
```

### 1.2 Environments

| Env | Purpose | Cloud | Status |
|---|---|---|---|
| **dev** | Local development | WSL2 + docker-compose | ✅ active (Wave 24 cadence) |
| **staging** | Pre-prod test | AWS EKS staging | ⚠️ terraform exists; activation status TBD |
| **production** | Live tenants | Oracle Cloud (primary) + AWS (backup) | ❌ not activated |

### 1.3 Existing artifacts

- ✅ `infrastructure/terraform-oracle/` — Oracle Cloud terraform (compute, network, variables, main, outputs)
- ✅ `infrastructure/terraform-aws/` — AWS terraform (EKS + RDS + ElastiCache + VPC + DNS + S3+ECR + secrets)
- ✅ `infrastructure/helm/` — Kubernetes Helm charts cho services
- ✅ `.github/workflows/deploy-staging.yml` — staging deploy workflow
- ✅ `.github/workflows/deploy-production.yml` — production deploy workflow (manual `workflow_dispatch` với confirm `DEPLOY`)
- ✅ `documents/05-guides/deploy/deploy-go-nogo-checklist.md` — generic go-nogo checklist (2026-04-16)
- ✅ `documents/05-guides/operations/runbooks/deployment-procedures.md` — generic runbook
- ✅ `documents/03-planning/infrastructure/{kitehub-oracle-cloud-deployment,monitoring-observability,kitehub-infrastructure,kitehub-database-provisioning,local-prod-separation-plan}.md`

---

## 2. Phase 1 BETA deploy plan (v0.9.0-beta) — Week 9-12

### 2.1 Pre-deploy checklist

**Code quality:**
- [ ] CI green on main (all workflows passing)
- [ ] All Phase 1 BETA gaps closed (per `release-1-plan-2026.md` §3.6)
- [ ] No P0 incidents trong 1 tuần
- [ ] Quality audit /100 ≥ 80

**Database:**
- [ ] Flyway migrations V1..V57+ tested staging
- [ ] Initial seed: admin user + system config + kitehub instance + sample tenant
- [ ] Backup snapshot taken pre-deploy
- [ ] Rollback SQL prepared (last DDL revert)

**Security:**
- [ ] No critical/high CVEs (`./scripts/check-deps.sh`)
- [ ] Secrets rotated từ dev to prod (separate keys)
- [ ] HTTPS / TLS certs ready (Let's Encrypt via Nginx)
- [ ] Security headers test (CSP, HSTS, X-Frame-Options) — manual scan trước launch

**Beta-specific:**
- [ ] Public signup form replaced với "Request Beta Access" (per **GAP-372**)
- [ ] Beta invite manual approval flow tested
- [ ] Beta dashboard banner: "Kite v0.9.0-beta — Beta period free"
- [ ] Footer build info: `v0.9.0-beta+build.YYYYMMDD.HHMM`
- [ ] Beta-only feature flags configured

**Infrastructure:**
- [ ] Oracle Cloud VM provisioned (1× 4 OCPU + 24 GB RAM Always Free ARM A1.Flex)
- [ ] DNS configured: `beta.kitehub.vn` + `beta.kiteclass.vn` (per **GAP-369**)
- [ ] SSL certs Let's Encrypt activated
- [ ] Cloudflare proxy configured (per **GAP-371**)
- [ ] Email transactional setup (per **GAP-370** — SendGrid/SES decision)
- [ ] PostgreSQL self-hosted on Oracle VM operational
- [ ] Redis self-hosted operational
- [ ] Ollama llama3.1:8b loaded
- [ ] Nginx reverse proxy với SSL configured

**Observability:**
- [ ] Loki log aggregation pipeline live (per GAP-115)
- [ ] Grafana dashboards configured + alerts wired (per **GAP-NEW-monitoring-dashboards** = part of GAP-115 scope)
- [ ] Tracing OpenTelemetry endpoints active (per GAP-112)
- [ ] FE error tracking Sentry/equivalent active (per GAP-113)
- [ ] Alert routing → email/SMS to coordinator on-call

**Documentation:**
- [ ] Beta tenant onboarding guide drafted
- [ ] Email templates: invite, welcome, password reset, support
- [ ] FAQ minimal (per cluster 6 in `release-1-plan-2026.md` §3.3)
- [ ] Status page minimum (per **GAP-373**)

### 2.2 Deploy steps (BETA)

```bash
# 1. Tag release
git tag -s v0.9.0-beta -m "Release v0.9.0-beta — Phase 1 BETA invite-only launch"
git push origin v0.9.0-beta

# 2. Build production Docker images
docker buildx build -t kiteclass-core:v0.9.0-beta -f kiteclass/kiteclass-core/Dockerfile .
docker buildx build -t kitehub-frontend:v0.9.0-beta -f kitehub/kitehub-frontend/Dockerfile .
# ... rest of services

# 3. Push to ECR (or Oracle Container Registry)
docker push <registry>/kiteclass-core:v0.9.0-beta
# ... rest

# 4. Apply Oracle Cloud terraform (if first deploy)
cd infrastructure/terraform-oracle
terraform plan -out=tfplan
terraform apply tfplan

# 5. Provision VM + install dependencies (Docker, PostgreSQL, Redis, Ollama, Nginx)
# Per Oracle Cloud deployment doc steps

# 6. Pull + run services on Oracle VM
ssh ubuntu@<oracle-vm-ip>
docker-compose -f /opt/kite/docker-compose.beta.yml up -d

# 7. Run Flyway migrations
docker exec kiteclass-core java -jar app.jar --spring.profiles.active=migrate

# 8. Seed initial data (per GAP-376)
docker exec kiteclass-core java -jar app.jar --command=seed-beta

# 9. Verify smoke tests (per GAP-377)
./scripts/smoke-test.sh https://beta.kitehub.vn https://beta.kiteclass.vn

# 10. Activate Cloudflare proxy
# Manual step: enable proxy on DNS records

# 11. Send invite emails to 10-20 trusted beta tenants
# Via beta admin tooling
```

### 2.3 Beta invite mechanism flow (per GAP-372)

```
1. User visits https://kitehub.vn → "Request Beta Access" form
2. Form fields: email, name, organization name, persona (P1/P2), referral source
3. Submit → store in beta_request table
4. Coordinator receives email notification
5. Coordinator reviews request manually (criteria: trusted referral, P1+P2 fit, reasonable contact)
6. Approve → trigger invite email với signup token
7. Invite email contains: signup link with token, beta disclaimer, beta period duration (4-6 tuần), feedback channel
8. User clicks → completes signup with token (token validates beta-tenant flag)
9. Tenant provisioned with beta-flag = true
10. Dashboard shows beta banner + footer build info
```

### 2.4 BETA smoke tests (post-deploy)

- [ ] Public marketing pages load: `/` `/blog` `/pricing` `/legal/privacy` `/legal/terms` `/legal/cookies` `/legal/dmca`
- [ ] Beta access form submits successfully
- [ ] Beta invite email received + signup token validates
- [ ] Tenant signup flow end-to-end (email verification + dashboard load)
- [ ] Owner dashboard loads với basic data
- [ ] Add student / class / lesson works
- [ ] AI Branding minimum (logo upload + theme picker) functional
- [ ] PDPL ConsentBanner shows on first visit; consent persists; Privacy/Cookie links resolve
- [ ] Logout / login / forgot password flows
- [ ] Mobile responsive (Chrome DevTools toolbar test 4 viewports)
- [ ] Error pages (`/404`, `/500`) render branded

---

## 3. Phase 1.5 PAID deploy plan (v1.0.0) — Week 13-18

### 3.1 Pre-deploy checklist (extends Phase 1 BETA)

Tất cả items §2.1 PLUS:

**Phase 1.5 BLOCKING gaps closed:**
- [ ] **GAP-NEW-payment-processor-init** — VNPay/MoMo init flow live (sandbox + production keys)
- [ ] **GAP-183 close** — Refund/dispute resolution policy production-ready
- [ ] **GAP-181 close** — Acceptable Use Policy production-ready
- [ ] **GAP-353c close** — DSAR self-service intake form live
- [ ] **GAP-073 close** — Account deletion / RTBF endpoint + UI live
- [ ] **GAP-185 close** — VAT eInvoice TT 78/2021 NĐ 123/2020 production-ready

**Phase 1.5 STRONGLY recommend gaps closed:**
- [ ] **GAP-NEW-pen-test-light** — OWASP top 10 audit + security headers + CSRF config
- [ ] **GAP-NEW-deploy-runbook close** — comprehensive go-live runbook (this doc §5)
- [ ] **GAP-NEW-monitoring-dashboards** — Grafana dashboards live + alerts on-call
- [ ] **GAP-135 close** — API P95 SLO targets documented + alerts wired

**Beta feedback loop:**
- [ ] 4-6 weeks beta period completed
- [ ] 0 P0 incidents trong 4 tuần
- [ ] Beta tenants happy (interview + survey)
- [ ] Critical bugs from beta resolved

**Production-specific:**
- [ ] DNS production cutover plan: `kitehub.vn` + `kiteclass.vn` (per **GAP-369**)
- [ ] SSL production certs (Let's Encrypt or paid CA)
- [ ] Public signup form re-enabled (replace "Request Beta Access")
- [ ] Payment processor production keys verified
- [ ] Status page activated (per **GAP-373**)
- [ ] Quality audit /100 ≥ 85 (higher bar than BETA 80)

### 3.2 Deploy steps (v1.0.0 PAID)

```bash
# 1. Final Beta period close
# Communicate to beta tenants: "Beta ending YYYY-MM-DD; transition to v1.0.0 paid"
# Offer migration path (free credit, etc.)

# 2. Tag release candidate
git tag -s v1.0.0-rc.1 -m "Release v1.0.0-rc.1 — Phase 1.5 PAID candidate"
git push origin v1.0.0-rc.1
# Deploy RC to staging; full E2E + load test

# 3. Final deploy steps (similar to BETA §2.2)
git tag -s v1.0.0 -m "Release v1.0.0 — Public Paid Launch (Release Lần 1 PRODUCTION)"
git push origin v1.0.0

# 4. Build + push v1.0.0 images
# 5. Apply terraform updates (any infra changes since BETA)
# 6. Blue-green deploy: provision new VM với v1.0.0 alongside v0.9.0-beta
# 7. DNS cutover: kitehub.vn → new VM
# 8. Verify production smoke tests
# 9. Decommission beta VM after stable period
# 10. Send announcement to all tenants + public marketing
```

### 3.3 v1.0.0 production smoke tests

Tất cả tests §2.4 PLUS:
- [ ] Payment processor: trial→paid migration end-to-end (sandbox)
- [ ] Refund flow tested (mock dispute)
- [ ] Account deletion flow: user requests deletion → 30-day grace period → confirmed deletion
- [ ] DSAR self-service: user submits request → ticket queue → response within 20-30 days
- [ ] Public signup form functional (re-enabled)
- [ ] Cross-region monitoring active
- [ ] SLO alerts trigger on threshold breach

### 3.4 Post-launch monitoring (first 4 tuần)

- Daily check: error rate, P95 latency, signup conversion, active tenants
- Weekly review: incident count, customer feedback, performance trend
- Monthly: full quality audit + persona-based business review
- Hotfix queue: PATCH releases (v1.0.1, v1.0.2, ...) for bugs surfaced

---

## 4. Go-live runbook (v1.0.0 production cutover)

### 4.1 T-7 days

- [ ] Final beta tenant satisfaction survey
- [ ] Production environment provisioned + tested
- [ ] DNS records pre-configured (TTL set to 5 min for fast cutover)
- [ ] Email templates v1.0.0 finalized
- [ ] Status page live
- [ ] Payment processor production keys verified

### 4.2 T-1 day

- [ ] Code freeze on `main` (no merges except hotfix)
- [ ] Final smoke test on staging
- [ ] Backup snapshot taken
- [ ] On-call coordinator on standby

### 4.3 T-0 (deploy day)

```
00:00 — Maintenance window starts
00:15 — Backup verified; snapshot ID logged
00:30 — Tag v1.0.0 + push images
00:45 — Apply terraform (any infra updates)
01:00 — Blue-green deploy: provision v1.0.0 VM alongside v0.9.0-beta
01:30 — Run Flyway migrations on v1.0.0 DB
02:00 — Smoke tests on v1.0.0 endpoints (internal IP)
02:30 — DNS cutover: kitehub.vn → v1.0.0 IP (TTL 5 min)
03:00 — Wait DNS propagation; verify external smoke tests
03:30 — Public signup re-enable on production
04:00 — Send announcement email to tenants
04:30 — Monitor dashboards for 30 min post-cutover
05:00 — Maintenance window closes; announcement to public
```

### 4.4 T+1 hour to T+24 hours

- Continuous monitoring: error rate, P95 latency, signup flow
- On-call coordinator alert if any threshold breach
- Hotfix queue ready (rollback option available)

---

## 5. Rollback procedure (per GAP-378)

### 5.1 Rollback triggers

Initiate rollback IF any of:
- Critical bug affecting >10% tenants in first 24h
- Database corruption / data loss
- Authentication completely broken
- Payment processor failures (>50% transactions failed)
- Performance degradation > 2× baseline P95

### 5.2 Rollback steps

```bash
# 1. Announce maintenance to tenants
# Via status page + email

# 2. DNS cutover back to v0.9.0-beta IP (TTL 5 min)
# kitehub.vn → previous beta IP

# 3. Revert Helm release (Kubernetes case)
helm rollback kitehub <previous-revision>

# 4. Revert DB migrations IF schema-breaking
# Apply rollback SQL prepared pre-deploy
docker exec kitehub-db psql -U postgres < rollback-vN.sql

# 5. Restore data backup IF data corruption
# pg_restore from latest snapshot

# 6. Verify smoke tests on rolled-back version
./scripts/smoke-test.sh https://kitehub.vn https://kiteclass.vn

# 7. Communicate to tenants
# "Rolled back to v0.9.0-beta; v1.0.0 launch postponed; investigation ongoing"

# 8. Post-incident review trong 48h
# Per `output-review-mandate.md` Section 6
```

### 5.3 Rollback validation

- [ ] All public marketing pages load
- [ ] Tenant login works
- [ ] Existing data accessible
- [ ] Beta tenants unaffected (still on v0.9.0-beta)

---

## 6. Missing pieces — gap files filed inline

12 BLOCKING + STRONGLY recommend gaps filed in this PR (GAP-369..380):

| Gap | Title | Priority | Phase |
|---|---|---|---|
| **GAP-369** | Production DNS + domain setup (kitehub.vn + kiteclass.vn) | 🔴 P0 BLOCKING | Phase 1 BETA + 1.5 PAID |
| **GAP-370** | Email transactional infrastructure (SendGrid/SES) | 🔴 P0 BLOCKING | Phase 1 BETA |
| **GAP-371** | CDN setup (Cloudflare proxy + DDoS protection) | 🟠 P1 STRONGLY recommend | Phase 1 BETA |
| **GAP-372** | Beta tenant invite mechanism (Request Beta Access form + manual approval flow) | 🔴 P0 BLOCKING | Phase 1 BETA |
| **GAP-373** | Status page + incident comms (status.kitehub.vn) | 🟠 P1 STRONGLY recommend | Phase 1 BETA |
| **GAP-374** | Tag-based release automation CI workflow | 🟠 P1 STRONGLY recommend | Phase 1 BETA + 1.5 PAID |
| **GAP-375** | GitHub Release template + changelog folder | 🟡 P2 nice-to-have | Phase 1 BETA |
| **GAP-376** | Production data seed (admin user, system config) | 🔴 P0 BLOCKING | Phase 1 BETA |
| **GAP-377** | Smoke test post-deploy automation | 🟠 P1 STRONGLY recommend | Phase 1 BETA |
| **GAP-378** | Rollback procedure runbook (detailed) | 🟠 P1 STRONGLY recommend | Phase 1 BETA |
| **GAP-379** | Secrets management (AWS Secrets Manager + rotation policy) | 🟠 P1 STRONGLY recommend | Phase 1 BETA |
| **GAP-380** | Staging environment activation + parity validation | 🟠 P1 STRONGLY recommend | Phase 1 BETA |

---

## 7. CI/CD tag-based release automation (per GAP-374)

Future workflow `.github/workflows/release-tag.yml`:

```yaml
name: Release on tag
on:
  push:
    tags:
      - 'v[0-9]+.[0-9]+.[0-9]+*'

jobs:
  build-and-publish:
    steps:
      - checkout
      - extract-version-from-tag
      - build-multi-arch-docker (linux/amd64, linux/arm64)
      - push-to-registry (with version tag + latest)
      - generate-changelog (from commit messages since previous tag)
      - create-github-release (with changelog body)
      - notify-deployment-channel
```

Tag conventions:
- `v0.9.0-beta` → push to staging registry only; manual deploy-staging trigger
- `v0.9.0-beta.N` → patch incremental
- `v1.0.0-rc.N` → push to staging + production registry; staging deploy auto
- `v1.0.0` → push to production; manual deploy-production trigger với `confirm: DEPLOY`
- `v1.0.x` (patches) → push to production; manual trigger với expedited approval

---

## 8. Open items / follow-ups

- [ ] Decide email transactional vendor (SendGrid vs SES vs Mailgun) — per GAP-370
- [ ] Decide DNS registrar + hosting (Vercel? Cloudflare? Direct domain?) — per GAP-369
- [ ] Decide payment processor primary (VNPay vs MoMo first) — per GAP-NEW-payment-processor-init
- [ ] Counsel engagement timeline confirmation
- [ ] Beta tenant invite list curation (10-20 trusted contacts)
- [ ] Marketing content for v1.0.0 launch announcement
- [ ] Onboarding tour script (per GAP-288)

---

## 9. Cross-references

- Parent: [`release-1-plan-2026.md`](release-1-plan-2026.md) — phase structure + gap clusters
- Versioning: [`versioning-policy.md`](versioning-policy.md) — semver convention + release process
- Architecture: [`infrastructure/kitehub-oracle-cloud-deployment.md`](../infrastructure/kitehub-oracle-cloud-deployment.md)
- Generic checklist: [`documents/05-guides/deploy/deploy-go-nogo-checklist.md`](../../05-guides/deploy/deploy-go-nogo-checklist.md)
- Generic runbook: [`documents/05-guides/operations/runbooks/deployment-procedures.md`](../../05-guides/operations/runbooks/deployment-procedures.md)
- Existing workflows: `.github/workflows/deploy-{staging,production}.yml`
- Memory: `feedback_release_1_first_session_priority.md`

---

## 10. Log

- **2026-05-06:** Plan created. Aggregates infrastructure base (terraform Oracle + AWS, Helm, workflows, generic checklists/runbooks) into version-specific deploy plan cho v0.9.0-beta + v1.0.0. Filed 12 BLOCKING/STRONGLY recommend gaps (GAP-369..380) inline. Phase 1 BETA deploy steps documented + Phase 1.5 PAID delta documented. Go-live runbook §4 + rollback procedure §5 (high-level; detailed rollback runbook → GAP-378). Open items §8 require user decisions (email vendor, DNS registrar, payment processor primary).
