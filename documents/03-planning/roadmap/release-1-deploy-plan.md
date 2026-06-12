---
title: Release Lần 1 Deploy Plan — v0.9.0-beta + v1.0.0 (Phase 1 BETA + Phase 1.5 PAID)
status: active
created: 2026-05-06
updated: 2026-05-07
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

> **Cloud platform locked AWS Singapore Free Tier per [ADR-025](../../02-architecture/adr/ADR-025-aws-singapore-free-tier-architecture.md) (ACCEPTED 2026-05-07).** Oracle Cloud Always Free path abandoned do signup reject rate ~50% VN. Phase 1 BETA = Architecture B (single EC2 + docker-compose) per Wave 38 Bucket D staging activation. Ollama AI inference deferred Phase 2 per [ADR-026](../../02-architecture/adr/ADR-026-ollama-defer-phase-2.md). Status page vendor = Instatus per [ADR-027](../../02-architecture/adr/ADR-027-statuspage-vendor-instatus.md).

### 1.1 Architecture B — AWS Singapore single-EC2 (Phase 1 BETA)

```
┌──────────────────────────────────────────────────────────┐
│  AWS Region: ap-southeast-1 (Singapore)                  │
│  Account: AWS Activate Founders Pack ($1k credit ~13.9mo)│
│                                                          │
│  Single EC2 t3.large (8GB RAM) + docker-compose          │
│  ├── 9 services (KH 6 + KC core + 2 frontends)           │
│  ├── PostgreSQL on RDS db.t3.micro (Free Tier 12 mo)     │
│  ├── Redis ElastiCache cache.t3.micro                    │
│  ├── Nginx reverse proxy + Let's Encrypt SSL             │
│  └── No Ollama (OpenAI/Anthropic API per ADR-026)        │
│                                                          │
│  Edge / Frontend                                         │
│  ├── Cloudflare proxy (Free) — DDoS + CDN + SSL          │
│  ├── Route53 + ALB DNS                                   │
│  └── ECR ap-southeast-1 (9 repos kite/<service>)         │
│                                                          │
│  Storage / Email / Status                                │
│  ├── S3 (per-tenant prefix) + Glacier cold tier         │
│  ├── SES ap-southeast-1 (transactional email)            │
│  └── Instatus (Free) — status.kitehub.vn                 │
└──────────────────────────────────────────────────────────┘
```

**Phase 1.5+ migration path:** EKS + RDS Multi-AZ documented in [`infrastructure/terraform-aws/`](../../../infrastructure/terraform-aws/) + `documents/05-guides/deploy/aws-architecture-sizing-matrix.md` (Wave 37 Bucket E). Trigger = ≥5 tenants live + Phase 1.5 PAID promotion.

### 1.2 Environments

| Env | Purpose | Cloud | Status |
|---|---|---|---|
| **dev** | Local development | WSL2 + docker-compose | ✅ active (Wave 24 cadence) |
| **staging** | Pre-prod E2E gate | AWS Singapore EC2 (Architecture B) | 🟡 PARTIAL (Wave 38 Bucket D — `staging.tf` + `deploy-staging.yml` ready; `enable_staging=false` default; user runs `terraform apply` to activate) |
| **production** | Live tenants | AWS Singapore (Architecture B) | ❌ not activated — pending Phase 1 user-actions per `release-1-deploy-runbook.md` |

### 1.3 Existing artifacts

- ✅ `infrastructure/terraform-aws/` — AWS Singapore Architecture B (Wave 37 Bucket A: VPC + EC2 + RDS + ElastiCache + ECR 9 repos + Secrets Manager + IAM OIDC + ALB)
- ✅ `infrastructure/terraform-aws/staging.tf` — staging EC2 (Wave 38 Bucket D — gated `enable_staging`)
- ✅ `infrastructure/helm/` — Kubernetes Helm charts (deferred Phase 1.5+ EKS migration; not used Phase 1 BETA)
- ✅ `.github/workflows/deploy-staging.yml` — staging deploy (Wave 38 Bucket D rewrite — SSM into EC2 + docker-compose)
- ✅ `.github/workflows/deploy-production.yml` — production deploy workflow (manual `workflow_dispatch` với confirm `DEPLOY`)
- ✅ `.github/workflows/release-tag.yml` — tag-based release (Wave 38 Bucket A — validate-tag + generate-changelog + GitHub Release)
- ✅ `.github/workflows/docker-build-push.yml` — 9 services × multi-arch + Trivy + SBOM/Cosign (Wave 37 Bucket B)
- ✅ `.github/workflows/e2e-pre-release.yml` — 3 beta-funnel specs + visual regression scaffold (Wave 37 Bucket C)
- ✅ `.github/workflows/zap-baseline.yml` — OWASP baseline scan workflow_dispatch (Wave 37 Bucket C)
- ✅ `documents/05-guides/deploy/{aws-architecture-sizing-matrix,cloudflare-setup,dns-setup-runbook,secrets-management-runbook,staging-activation-runbook,aws-cost-monitoring,email-ses-setup-runbook}.md`
- ✅ `documents/05-guides/operations/{incident-comms-runbook,runbooks/rollback-runbook,runbooks/deployment-procedures}.md`
- ✅ `documents/05-guides/deploy/deploy-go-nogo-checklist.md` — generic go-nogo checklist (2026-04-16)
- ✅ `documents/03-planning/roadmap/release-1-deploy-runbook.md` — Phase 0-9 ordered single-source sequence (Wave 38 closure 2026-05-07)
- ✅ Oracle Cloud artifacts archived `documents/07-archived/oracle-deploy-2026/`

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

**Infrastructure (Architecture B — AWS Singapore per ADR-025):**
- [ ] AWS account + Activate Founders Pack approved ($1k credit)
- [ ] Terraform bootstrap state backend (S3 + DynamoDB lock) applied
- [ ] Production Terraform applied: VPC + EC2 t3.large + RDS db.t3.micro + ElastiCache + ECR 9 repos + Secrets Manager + ALB
- [ ] DNS configured: `kitehub.vn` + `kitehub.me` (per **GAP-369**) via Cloudflare → ALB
- [ ] SSL certs Let's Encrypt + Cloudflare Full (strict) activated
- [ ] Cloudflare proxy configured + headers verified (per **GAP-371** — Wave 38 Bucket B `cloudflare-setup.md` + `verify-cdn-headers.sh`)
- [ ] SES production access approved + domain verified DKIM/SPF/DMARC (per **GAP-370** — Wave 33 Bucket B `email-ses-setup-runbook.md`)
- [ ] Secrets Manager populated `kite/prod/*` (per **GAP-379** — Wave 33 Bucket D template)
- [ ] PostgreSQL RDS reachable + Flyway migrations applied
- [ ] Redis ElastiCache reachable
- [ ] AI inference: OpenAI/Anthropic API keys configured (Ollama deferred Phase 2 per ADR-026)
- [ ] Docker images pushed ECR (9 services × multi-arch) per **Wave 37 Bucket B `docker-build-push.yml`**

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

### 2.2 Deploy steps (BETA — Architecture B AWS Singapore)

> **Canonical sequence:** [`release-1-deploy-runbook.md`](release-1-deploy-runbook.md) Phase 0-9 — single-source ordered checklist với per-step ownership (user vs agent). Block dưới đây là TL;DR; runbook là source of truth cho execution.

```bash
# 1. Bootstrap Terraform state backend (one-time)
cd infrastructure/terraform-aws/bootstrap
terraform init && terraform plan -out=bootstrap.tfplan && terraform apply bootstrap.tfplan
# → S3 bucket + DynamoDB lock created; copy outputs vào backend.tf

# 2. Apply production Terraform (one-time)
cd infrastructure/terraform-aws
terraform plan -out=tfplan
terraform apply tfplan
# → VPC + EC2 + RDS + ElastiCache + ECR 9 repos + Secrets Manager + ALB + Route53 zone

# 3. Populate Secrets Manager (manual via AWS Console or aws-cli)
# kite/prod/{db/password,jwt/secret,encryption/master-key,internal-api/secret,openai/api-key,...}

# 4. Tag release → CI builds + pushes ECR (Wave 37 Bucket B + Wave 38 Bucket A)
git tag -s v0.9.0-beta -m "Release v0.9.0-beta — Phase 1 BETA invite-only launch"
git push origin v0.9.0-beta
# → docker-build-push.yml fires: 9 services × 2 archs + Trivy + SBOM/Cosign + push ECR
# → release-tag.yml fires: validate + generate-changelog + GitHub Release prerelease=true

# 5. Trigger production deploy workflow (manual gate)
gh workflow run deploy-production.yml -f confirm=DEPLOY -f tag=v0.9.0-beta

# 6. SSM into EC2 → docker compose pull && docker compose up -d
aws ssm start-session --target i-<id> --region ap-southeast-1
sudo -u kite docker compose -f /opt/kite/docker-compose.prod.yml pull
sudo -u kite docker compose -f /opt/kite/docker-compose.prod.yml up -d

# 7. Flyway migrations apply auto via Spring Boot start (verify logs)

# 8. Seed initial data (Wave 33 GAP-376 ProductionSeedRunner)
sudo -u kite docker exec kiteclass-core java -jar app.jar --command=seed-production

# 9. DNS cutover Cloudflare → ALB
# Cloudflare Console: A record kitehub.vn + kitehub.me → ALB DNS (proxied orange-cloud)

# 10. Verify smoke tests (Wave 26 GAP-377 — 18 assertions)
./scripts/smoke-test.sh https://kitehub.vn https://kitehub.me

# 11. Trigger E2E pre-release Playwright + OWASP ZAP (Wave 37 Bucket C)
gh workflow run e2e-pre-release.yml
gh workflow run zap-baseline.yml

# 12. Send invite emails — TIGHTLY-CONTROLLED HANDFUL ONLY (Wave 76 audit-tightened)
# Initial wave: 2-3 trusted users ONLY, expand to 10-20 ONLY after 4 must-close blockers verified live
# Must-close pre-broader-invite (per Phase 1 BETA persona audit 2026-05-14):
#   - GAP-530 P0: Email-driven flow end-to-end live verify §2.3
#   - GAP-518: Admin role mismatch (AC 1-2 live verify)
#   - GAP-502: kh_backend stability ≥80% acceptable for handful, ≥95% for broader
#   - GAP-372: Beta invite mechanism live walkthrough
# Via admin endpoint /admin/beta-requests (Wave 33 Bucket C BetaAccessRequest flow)
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

> **Automated** post-deploy via `scripts/smoke-test.sh <KH-url> <KC-url>` (Wave 26 GAP-377). 18 assertions covering health, legal pages, login/register, KH `/api/health`, ConsentBanner mount, KC public APIs, error handling, gateway routing.
>
> **E2E gate** via `.github/workflows/e2e-pre-release.yml` (Wave 37 Bucket C — GAP-403/404/406): 3 beta-funnel Playwright specs (`request-flow.spec.ts`, `admin-approve.spec.ts`, `signup-with-claim-code.spec.ts`) + visual regression scaffold + trace upload artifact.
>
> **Security baseline** via `.github/workflows/zap-baseline.yml` (Wave 37 Bucket C — GAP-405) workflow_dispatch — OWASP top 10 baseline scan with manual review.
>
> Manual checklist below tracks supersets the automation does not yet cover.

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
- [ ] DNS production cutover plan: `kitehub.vn` + `kitehub.me` (per **GAP-369**)
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

> **Automated** baseline same as §2.4 — invoke `scripts/smoke-test.sh https://kitehub.vn https://kitehub.me` (GAP-377 / Wave 26 Bucket C). The 18 baseline assertions execute against production URLs; deploy CI step (`.github/workflows/deploy-staging.yml`) is the canonical pre-cutover gate. Production-specific manual items below extend that baseline.

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

> 📖 **Detailed runbook:** [`documents/05-guides/operations/runbooks/rollback-runbook.md`](../../05-guides/operations/runbooks/rollback-runbook.md) — full per-component specifics, communication templates (VN/EN), validation checklist, pg_restore recovery path. The summary below is the TL;DR; the runbook is the source of truth for execution under incident pressure.

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
./scripts/smoke-test.sh https://kitehub.vn https://kitehub.me

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
| **GAP-369** | Production DNS + domain setup (kitehub.vn + kitehub.me) | 🔴 P0 BLOCKING | Phase 1 BETA + 1.5 PAID |
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
- [ ] Beta tenant invite list — **TIGHTENED Wave 76 (2026-05-14)**: 2-3 trusted contacts ONLY for initial wave; expand to 10-20 ONLY after 4 must-close blockers verified (GAP-530 email e2e + GAP-518 admin role + GAP-502 stability + GAP-372 invite walkthrough). Per Phase 1 BETA persona audit verdict "tightly-controlled handful".
- [ ] Marketing content for v1.0.0 launch announcement
- [ ] Onboarding tour script (per GAP-288)

---

## 9. Cross-references

- Parent: [`release-1-plan-2026.md`](release-1-plan-2026.md) — phase structure + gap clusters
- **Ordered runbook (Phase 0-9):** [`release-1-deploy-runbook.md`](release-1-deploy-runbook.md) — single-source ordered sequence
- Versioning: [`versioning-policy.md`](versioning-policy.md) — semver convention + release process
- ADR-025: [AWS Singapore Free Tier Architecture](../../02-architecture/adr/ADR-025-aws-singapore-free-tier-architecture.md) — cloud platform decision
- ADR-026: [Ollama defer Phase 2](../../02-architecture/adr/ADR-026-ollama-defer-phase-2.md) — AI inference strategy
- ADR-027: [Statuspage vendor Instatus](../../02-architecture/adr/ADR-027-statuspage-vendor-instatus.md) — status page decision
- Sizing matrix: [`documents/05-guides/deploy/aws-architecture-sizing-matrix.md`](../../05-guides/deploy/aws-architecture-sizing-matrix.md)
- Cloudflare setup: [`documents/05-guides/deploy/cloudflare-setup.md`](../../05-guides/deploy/cloudflare-setup.md)
- DNS runbook: [`documents/05-guides/deploy/dns-setup-runbook.md`](../../05-guides/deploy/dns-setup-runbook.md)
- Secrets runbook: [`documents/05-guides/deploy/secrets-management-runbook.md`](../../05-guides/deploy/secrets-management-runbook.md)
- Staging activation: [`documents/05-guides/deploy/staging-activation-runbook.md`](../../05-guides/deploy/staging-activation-runbook.md)
- Email SES setup: [`documents/05-guides/deploy/email-ses-setup-runbook.md`](../../05-guides/deploy/email-ses-setup-runbook.md)
- Incident comms: [`documents/05-guides/operations/incident-comms-runbook.md`](../../05-guides/operations/incident-comms-runbook.md)
- Rollback runbook: [`documents/05-guides/operations/runbooks/rollback-runbook.md`](../../05-guides/operations/runbooks/rollback-runbook.md)
- Generic checklist: [`documents/05-guides/deploy/deploy-go-nogo-checklist.md`](../../05-guides/deploy/deploy-go-nogo-checklist.md)
- Existing workflows: `.github/workflows/{deploy-staging,deploy-production,docker-build-push,release-tag,e2e-pre-release,zap-baseline}.yml`
- Archived (Oracle path superseded by ADR-025): [`documents/07-archived/oracle-deploy-2026/`](../../07-archived/oracle-deploy-2026/)
- Memory: `feedback_release_1_first_session_priority.md`

---

## 10. Log

- **2026-05-07 (Phase 0 plan refresh):** Updated post Wave 38 + PR #947 runbook merge. Cloud platform locked AWS Singapore Architecture B per ADR-025 (Oracle Cloud path archived). §1.1 rewritten dual-cloud → single-EC2 + docker-compose Architecture B. §1.2 environments updated (staging Architecture B Wave 38 Bucket D, production AWS Singapore). §1.3 artifacts list refreshed (Wave 37/38 Terraform AWS + 9 ECR repos + Wave 33/38 runbooks + workflows release-tag/docker-build-push/e2e-pre-release/zap-baseline). §2.1 Infrastructure checklist rewritten Architecture B context. §2.2 deploy steps rewritten 12-step sequence (terraform-aws + ECR + SSM + smoke + E2E + ZAP) replacing Oracle terraform-oracle path. §2.4 smoke test section extended với Wave 37 E2E + OWASP refs. §9 cross-references rebuilt với ADR-025/026/027 + 6 runbook links + archived Oracle pointer. Closes Phase 0 of `release-1-deploy-runbook.md` (sub-tasks 0.1-0.4, 0.6). Sub-task 0.5 (git mv Oracle doc → 07-archived) shipped same PR. Phase 1 user-actions unblocked.
- **2026-05-06:** Plan created. Aggregates infrastructure base (terraform Oracle + AWS, Helm, workflows, generic checklists/runbooks) into version-specific deploy plan cho v0.9.0-beta + v1.0.0. Filed 12 BLOCKING/STRONGLY recommend gaps (GAP-369..380) inline. Phase 1 BETA deploy steps documented + Phase 1.5 PAID delta documented. Go-live runbook §4 + rollback procedure §5 (high-level; detailed rollback runbook → GAP-378). Open items §8 require user decisions (email vendor, DNS registrar, payment processor primary).
