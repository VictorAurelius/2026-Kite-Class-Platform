---
title: Release 1 Deploy Runbook — Phase 1 BETA (v0.9.0-beta) ordered sequence
status: draft
created: 2026-05-07
updated: 2026-05-07
---

# Release 1 Deploy Runbook — Phase 1 BETA (v0.9.0-beta)

**Status:** Draft 2026-05-07 (Wave 38 closure)
**Architecture:** B (AWS Singapore EC2, per ADR-025)
**Cloud:** `ap-southeast-1` (Singapore)
**Target tag:** `v0.9.0-beta` (PRE-RELEASE per `release-deploy-standard.md` §3.1)

> **Tại sao file này?** `release-1-deploy-plan.md` §1.1+§2.2 còn references Oracle Cloud (stale post-ADR-025). Runbook này là single-source ordered sequence — tránh miss step như đã từng. Plan refresh cùng wave (Phase 0).

---

## Pre-flight: Wave dependencies (đã ship)

| Wave | Output | Status |
|---|---|---|
| 25 | `secrets-management-runbook.md` (GAP-379) | 🟡 PARTIAL — template only |
| 25 | `rollback-runbook` (GAP-378) | 🟢 DONE |
| 26 | `scripts/smoke-test.sh` 18 assertions (GAP-377) | 🟢 DONE |
| 33 | DNS production runbook (GAP-369), email templates (GAP-370), beta-invite flow BE+FE (GAP-372), `ProductionSeedRunner` (GAP-376), `.env.production.template` | 🟡 PARTIAL all |
| 35 | 5 P0 BLOCKERS (admin auth + PDPL consent + quality threshold + beta metrics + N+1 DB + V31 indexes) | 🟢 DONE |
| 36 | 5 P1 hardening (security + API polish + ops + perf + UI quota refresh) | 🟢 DONE |
| 37 | Terraform AWS Singapore (Architecture B), Docker release pipeline 5 KH services + ECR + Trivy + multi-arch + SBOM/Cosign, E2E pre-release workflow + 3 beta-funnel specs + visual regression scaffold + OWASP baseline doc, Compose profiles + JVM cap + Ollama/WSL2 docs, AWS sizing matrix + ADR-026 Ollama defer + Activate pitch deck + Budgets + EKS migration plan | 🟡 PARTIAL most |
| 38 | Tag-based release CI workflow + changelog generator, Cloudflare CDN runbook + verify-cdn-headers script, Status page (Instatus) runbook + post-mortem template + ADR-027, Staging activation Architecture B (EC2 + docker-compose) | 🟡 PARTIAL most |

**Tracker tổng:** 22 P0+P1 gaps Phase 1 BETA shipped 2026-05-07 (ROADMAP §🚀 Next Action). Remaining = USER-EXECUTED steps below.

---

## Phase 0 — Plan refresh (✅ DONE 2026-05-07)

- [x] **0.1** Update `release-1-deploy-plan.md` §1.1 (Oracle → AWS Singapore Architecture B)
- [x] **0.2** Update `release-1-deploy-plan.md` §2.2 deploy steps (terraform-oracle → terraform-aws + ECR + Helm-skip per Architecture B)
- [x] **0.3** Update §2.4 smoke test references → align với Wave 26 `scripts/smoke-test.sh` + Wave 37 E2E specs + Wave 37 OWASP baseline
- [x] **0.4** Cross-link ADR-025 (AWS Singapore) + ADR-026 (Ollama defer) + ADR-027 (Statuspage) trong §1
- [x] **0.5** Archive `kitehub-oracle-cloud-deployment.md` → `documents/07-archived/oracle-deploy-2026/`
- [x] **0.6** Cross-link runbook này vào ROADMAP §🚀 Next Action

**Sub-task 0.5 expanded:** updated `documents/02-architecture/deployment-strategy.md` + `documents/03-planning/README.md` + `documents/03-planning/infrastructure/kitehub-infrastructure.md` cross-refs to point to runbook + archived Oracle path.

**Phase 1 user-actions UNBLOCKED.**

---

## Phase 1 — User-action prereqs (parallel, ~1-2 ngày user time)

### 1.1 Domain registration
- [ ] **1.1.1** Đăng ký `kitehub.vn` tại VN registrar (Mat Bao / PA Vietnam / iNet)
- [ ] **1.1.2** Đăng ký `kitehub.me` tại VN registrar (cùng vendor recommend cho quản lý)
- [ ] **1.1.3** Verify domain ownership email confirmation
- [ ] **1.1.4** Lưu credentials vào password manager (KHÔNG commit)

### 1.2 AWS account + Activate credit
- [ ] **1.2.1** Tạo AWS account (root) — bật MFA hardware
- [ ] **1.2.2** Tạo IAM admin user — không dùng root cho ops
- [ ] **1.2.3** Submit AWS Activate Founders Pack với pitch deck (Wave 37 Bucket E `documents/00-brd/kite-pitch-deck.md`)
- [ ] **1.2.4** Khi approved: $1k credit ghi vào billing dashboard (~13.9 tháng cover Architecture B)
- [ ] **1.2.5** Setup AWS Budgets 3 alarms (Wave 37 Bucket E `documents/05-guides/deploy/aws-cost-monitoring.md`)

### 1.3 Frontend hosting + DNS

> **🔄 PIVOT 2026-05-07 (Stream A):** Cloudflare Pages → **Vercel free tier**. Lý do: Cloudflare Pages + Next.js 15 SSR yêu cầu `@opennextjs/cloudflare` adapter ~1-2h setup + risk monorepo + pnpm workspace + `@kite/shared-ui`; Vercel là Next.js-native (0 adapter, 0 config, monorepo-aware). Trade-off: defer Cloudflare DNS proxy/CDN benefits tới Phase 2 khi promote production. Reference: `release-1-deploy-session-2026-05-07.md`.

**Option A — Vercel (Phase 1 BETA, currently active 2026-05-07):**
- [x] **1.3.A.1** Vercel signup qua GitHub OAuth
- [x] **1.3.A.2** Import project `kiteclass` từ repo, root dir `kiteclass/kiteclass-frontend`
- [x] **1.3.A.3** Import project `kitehub` từ repo, root dir `kitehub/kitehub-frontend`
- [x] **1.3.A.4** Override Install Command: `cd ../.. && pnpm install --frozen-lockfile`; Node 20.x
- [x] **1.3.A.5** Production URLs claimed: `kiteclass.vercel.app` + `kitehub.vercel.app`
- [ ] **1.3.A.6** (defer Phase 2) Add `NEXT_PUBLIC_API_BASE_URL` env var pointing to ALB DNS sau Phase 2.3 apply

**Option B — Cloudflare proxy + custom domain (Phase 2+ when needed):**
- [ ] **1.3.B.1** Tạo Cloudflare account Free tier
- [ ] **1.3.B.2** Add domain `kitehub.vn` + `kitehub.me`
- [ ] **1.3.B.3** Cloudflare emit nameservers — copy
- [ ] **1.3.B.4** Update nameservers tại VN registrar (DNS migration thường 2-24h propagate)
- [ ] **1.3.B.5** Verify DNS migrated qua `dig +short NS kitehub.vn` → trả về Cloudflare NS
- [ ] **1.3.B.6** Configure SSL/TLS Full (strict), Bot Fight Mode, Always Use HTTPS, Auto Minify, Brotli (per Wave 38 Bucket B `cloudflare-setup.md`)
- [ ] **1.3.B.7** CNAME `app.kitehub.vn` → `kitehub.vercel.app` (proxy through Cloudflare)

### 1.4 Email transactional

> **🔄 PIVOT 2026-05-07 (Stream A):** AWS SES → **Resend free tier**. Lý do: SES production access request 24-48h delay + DKIM/SPF/DMARC record setup phức tạp khi chưa có domain; Resend signup ngay qua GitHub OAuth + sandbox sender `onboarding@resend.dev` đủ Phase 1 BETA invite-only. Trade-off: domain verification defer tới khi có custom domain (Phase 2). Reference: `release-1-deploy-session-2026-05-07.md`.

**Option A — Resend (Phase 1 BETA, currently active 2026-05-07):**
- [x] **1.4.A.1** Resend signup qua GitHub OAuth
- [x] **1.4.A.2** Tạo API key `kite-platform-dev` → store GitHub Secret `RESEND_API_KEY`
- [x] **1.4.A.3** Sandbox sender `onboarding@resend.dev` — đủ Phase 1 BETA invite test
- [ ] **1.4.A.4** (defer Phase 2) Domain verification khi có custom domain → real sender `noreply@kitehub.vn`
- [ ] **1.4.A.5** (sau Phase 2.3) Sao chép `RESEND_API_KEY` từ GH Secret sang AWS Secrets Manager (Phase 2.4) để BE service đọc

**Option B — AWS SES (Phase 2+ khi có domain):**
- [ ] **1.4.B.1** AWS Console → SES → Region ap-southeast-1
- [ ] **1.4.B.2** Verify domain `kitehub.vn` (DKIM + SPF + DMARC records via Cloudflare DNS)
- [ ] **1.4.B.3** Submit SES production access request (sandbox → production) — usually 24-48h
- [ ] **1.4.B.4** Configure verified sender `noreply@kitehub.vn`
- [ ] **1.4.B.5** Set up bounce + complaint topic SNS (per Wave 33 Bucket B email runbook)

### 1.5 Status page

> **🔄 PIVOT 2026-05-07 (Stream A):** Instatus → **Better Stack Free tier**. Lý do: UptimeRobot (đề xuất ban đầu trong Wave 38) đã đổi pricing — free tier không còn cấp public status page; Instatus cũng cần custom domain config. Better Stack free tier (10 monitors, 3-min checks, public status page miễn phí) đáp ứng đủ. Reference: `release-1-deploy-session-2026-05-07.md`.

**Option A — Better Stack (Phase 1 BETA, currently active 2026-05-07):**
- [x] **1.5.A.1** Better Stack signup qua GitHub OAuth
- [x] **1.5.A.2** Add 2 monitors: `kiteclass.vercel.app` + `kitehub.vercel.app` (3-min interval)
- [x] **1.5.A.3** Public status page live: `https://kite-platform.betteruptime.com/`
- [ ] **1.5.A.4** (sau Phase 2.3) Add 3 monitors cho ALB endpoints (KH-API + KC-API + Auth)
- [ ] **1.5.A.5** (defer Phase 2) Custom domain `status.kitehub.vn` → Better Stack CNAME khi có domain

**Option B — Instatus (Phase 2+ alternative, per Wave 38 Bucket C ADR-027):**
- [ ] **1.5.B.1** Tạo Instatus account Free tier
- [ ] **1.5.B.2** Define 5 components: KH-API, KC-API, Marketing, Auth, Email (+ 6th AI Branding optional)
- [ ] **1.5.B.3** Configure custom domain `status.kitehub.vn` → Cloudflare CNAME tới Instatus
- [ ] **1.5.B.4** Configure incident severity levels per Wave 38 Bucket C `incident-comms-runbook.md` §3
- [ ] **1.5.B.5** Test: tạo incident sample → resolve → verify subscriber email

---

## Phase 2 — Bootstrap infra (user runs Terraform; agent verify)

### 2.1 Terraform state backend
- [ ] **2.1.1** `cd infrastructure/terraform-aws/bootstrap`
- [ ] **2.1.2** Configure AWS CLI credentials (`aws configure --profile kite-prod` — region ap-southeast-1)
- [ ] **2.1.3** `terraform init`
- [ ] **2.1.4** `terraform plan -out=bootstrap.tfplan` — verify creates S3 bucket + DynamoDB lock table
- [ ] **2.1.5** `terraform apply bootstrap.tfplan` — output S3 bucket name + DynamoDB table name
- [ ] **2.1.6** Update `infrastructure/terraform-aws/backend.tf` với bucket/table values

### 2.2 GitHub repo configuration
- [ ] **2.2.1** GitHub Settings → Secrets and variables → Actions
- [ ] **2.2.2** Set variables:
  - `AWS_CONFIGURED=true` (per Wave 37 Bucket B docker-build-push gate)
  - `AWS_REGION=ap-southeast-1`
  - `ECR_REGISTRY=<account-id>.dkr.ecr.ap-southeast-1.amazonaws.com`
- [ ] **2.2.3** Configure GitHub OIDC provider in AWS IAM:
  - Identity provider: `token.actions.githubusercontent.com`
  - Trust policy condition: repo + branch (per Wave 37 Bucket A IAM `terraform-plan-role`)
- [ ] **2.2.4** Set secrets nếu chưa OIDC-only:
  - `AWS_ROLE_ARN_TERRAFORM_PLAN`
  - `AWS_ROLE_ARN_ECR_PUSH`

### 2.3 Production Terraform apply
- [ ] **2.3.1** Push commit nhỏ vào main (vd: README typo) → trigger `.github/workflows/terraform-plan.yml`
- [ ] **2.3.2** Verify workflow comments plan diff vào PR (Wave 37 GAP-397)
- [ ] **2.3.3** Manual review plan output — pay attention to: VPC CIDR, EC2 sizing, RDS sizing, SG rules, IAM
- [ ] **2.3.4** User approve + `cd infrastructure/terraform-aws && terraform apply` locally (NOT via CI)
- [ ] **2.3.5** Capture outputs: ALB DNS name, RDS endpoint, ECR repo URLs (9 repos `kite/<service>`), Route53 zone ID, Secrets Manager ARNs

### 2.4 Secrets Manager populate
- [ ] **2.4.1** AWS Console → Secrets Manager → ap-southeast-1
- [ ] **2.4.2** Update placeholder secrets (Wave 33 Bucket D `.env.production.template` listing):
  - `kite/prod/db/password` — random 32-char
  - `kite/prod/jwt/secret` — random 64-char
  - `kite/prod/encryption/master-key` — random 32-byte base64
  - `kite/prod/internal-api/secret` — random 32-char
  - `kite/prod/openai/api-key` — real OpenAI key (Phase 1 fallback per ADR-026)
  - `kite/prod/anthropic/api-key` — real Anthropic key (alternative)
  - `kite/prod/ses/configuration-set` — `kitehub-prod`
- [ ] **2.4.3** Verify EC2 IAM role có `secretsmanager:GetSecretValue` cho `kite/prod/*` (Wave 37 Bucket A IAM)

---

## Phase 3 — Image push (CI auto + user trigger ad-hoc test tag)

> **Detailed runbook:** [`documents/05-guides/deploy/phase-3-image-push.md`](../../05-guides/deploy/phase-3-image-push.md) — pre-flight checklist (Variables + OIDC roles + 10 ECR repos), trigger sequence, monitor, verification (ECR images / Trivy / Cosign / SBOM), failure modes (F1-F4), post-success actions.

- [ ] **3.1** User tag ad-hoc test: `git tag v0.9.0-beta-staging.1 && git push origin v0.9.0-beta-staging.1`
- [ ] **3.2** `docker-build-push.yml` (Wave 37 Bucket B) auto-fires:
  - 9 services × 2 archs = 18 builds
  - Trivy scan post-build (fail HIGH/CRITICAL — initial run may need exception)
  - Multi-arch push amd64 + arm64
  - SBOM Syft CycloneDX + Cosign keyless sign
- [ ] **3.3** Agent monitor CI green (~10-15 min) — fix Dockerfile issues nếu fail
- [ ] **3.4** Verify images trong ECR Console — 9 repos × 2 tags (`v0.9.0-beta-staging.1` + `latest`)

---

## Phase 4 — Staging E2E gate (user trigger; agent run + monitor)

> **Quy ước local vs CI/ECR (per GAP-425 Option E):**
> - **Iteration code dev**: build local qua `bash kitehub/scripts/up.sh --profile beta-funnel --rebuild` (cycle ~2-5 min, không cần network). `--rebuild` tự bật `--force-recreate` để tránh footgun container chạy image cũ (GAP-425).
> - **Visual smoke local**: build local + `--force-recreate` để catch source-vs-image drift (root cause GAP-425 + GAP-426 surfaced 2026-05-07).
> - **Phase 4 staging gate (PHẢI dùng)**: pull CI-built image từ ECR qua `bash kitehub/scripts/up.sh --profile beta-funnel --pull-from-ecr v0.9.0-staging.X`. Đây là production parity test — image bit-for-bit identical với prod deploy. Trivy CVE scan + SBOM + Cosign sign chỉ wired ở CI.
> - **Phase 7 prod (BẮT BUỘC)**: chỉ deploy CI tag-driven image. Locally-built BANNED trong production.

### 4.1 Staging Terraform
- [ ] **4.1.1** Edit `infrastructure/terraform-aws/staging.tf` (Wave 38 Bucket D) — set `enable_staging=true`
- [ ] **4.1.2** `terraform plan -out=staging.tfplan && terraform apply staging.tfplan`
- [ ] **4.1.3** Capture staging outputs (EC2 ID, RDS endpoint, public IP)

### 4.2 Staging DNS

> **2 path — pick A (free, recommended cho RC) hoặc B (custom domain):**

**Path A — `sslip.io` wildcard DNS (Phase 1 BETA recommended, $0):**
- [ ] **4.2.A.1** Capture staging EC2 public IP (vd `13.228.45.67`) từ Phase 4.1.3
- [ ] **4.2.A.2** URL trực tiếp: `https://13-228-45-67.sslip.io` (replace `.` → `-`) — sslip.io tự resolve về IP gốc, không cần DNS register
- [ ] **4.2.A.3** Tương tự cho KC: dùng cùng EC2 IP (Architecture B = single EC2, gateway routes) hoặc separate IP nếu split
- [ ] **4.2.A.4** TLS: cấp Let's Encrypt cert qua certbot trên EC2 (`sudo certbot --nginx -d 13-228-45-67.sslip.io`) — sslip.io chấp nhận ACME HTTP-01 challenge
- [ ] **4.2.A.5** Verify `curl -sI https://13-228-45-67.sslip.io/actuator/health` → 200 + valid cert
- [ ] **4.2.A.6** Cập nhật Better Stack monitor + smoke-test target URL về sslip URL

**Path B — Custom domain `staging.kitehub.vn` (yêu cầu Phase 1.1 domain registered):**
- [ ] **4.2.B.1** Cloudflare DNS: A record `staging.kitehub.vn` → staging EC2 public IP (proxied)
- [ ] **4.2.B.2** Cloudflare DNS: A record `staging.kitehub.me` → staging EC2 public IP (proxied)
- [ ] **4.2.B.3** Verify `dig +short staging.kitehub.vn` resolves to Cloudflare IP

**Decision rule:** RC dùng Path A (zero cost, instant); production launch (Phase 7) bắt buộc Path B (domain branded).

### 4.3 Staging deploy
- [ ] **4.3.1** Trigger `.github/workflows/deploy-staging.yml` workflow_dispatch (Wave 38 Bucket D rewrite)
- [ ] **4.3.2** SSM session vào staging EC2 → `docker compose pull && docker compose up -d`
- [ ] **4.3.3** Run Flyway migrations auto via Spring Boot start
- [ ] **4.3.4** SSH execute `bash scripts/seed-staging-fixtures.sh` (Wave 38 Bucket D — 5-10 synthetic tenants)

### 4.4 Smoke test
- [ ] **4.4.1** `bash scripts/smoke-test.sh https://staging.kitehub.vn https://staging.kitehub.me` (Wave 26 GAP-377)
- [ ] **4.4.2** Verify 18 assertions pass: health endpoints, legal pages, login/register, KH `/api/health`, ConsentBanner mount, KC public APIs, 404 handling, gateway routing
- [ ] **4.4.3** Trigger `.github/workflows/zap-baseline.yml` workflow_dispatch (Wave 37 Bucket C OWASP scan) — manual review report

### 4.5 E2E Playwright
- [ ] **4.5.1** Trigger `.github/workflows/e2e-pre-release.yml` workflow_dispatch (Wave 37 Bucket C — typically `tags: v*.*.*-rc*` but workflow_dispatch overrides)
- [ ] **4.5.2** Verify 3 beta-funnel specs run on staging:
  - `request-flow.spec.ts` — visitor → request beta access form → confirmation
  - `admin-approve.spec.ts` — admin login → review request → approve → claim code
  - `signup-with-claim-code.spec.ts` — visitor uses claim code → signup → tenant created
- [ ] **4.5.3** Trace upload artifact — agent download for failure analysis nếu cần
- [ ] **4.5.4** Visual regression baseline capture (GAP-405 PARTIAL → DONE) — first-run on staging

---

## Phase 5 — Bug-fix iteration (agent-scope)

- [ ] **5.1** E2E findings → file gaps → fix PRs (typical: ~3-7 sub-PRs first iteration)
- [ ] **5.2** Re-trigger E2E sau mỗi fix → agent monitors
- [ ] **5.3** Loop until 3/3 specs green + smoke test 18/18 pass
- [ ] **5.4** Pen-test light manual checklist (release-deploy-standard §3.4 — OWASP top 10 + headers + CSRF) — security audit refresh

---

## Phase 6 — Release Candidate tag

- [ ] **6.1** Tag `git tag -s v0.9.0-rc.1 -m "Release Candidate 1 — Phase 1 BETA"` && `git push origin v0.9.0-rc.1`
- [ ] **6.2** `release-tag.yml` (Wave 38 Bucket A) auto-fires:
  - `validate-tag` → parse version + is_prerelease=true
  - `release` → generate changelog từ conventional commits + create GitHub Release (prerelease=true)
  - `notify` → placeholder log
- [ ] **6.3** Agent verify GitHub Release content correct
- [ ] **6.4** docker-build-push.yml fires same tag → ECR push `v0.9.0-rc.1` (skip `latest` tag for prereleases)

---

## Phase 7 — Production deploy (user execute, agent monitor)

### 7.1 T-7 days pre-deploy
- [ ] **7.1.1** Final feature freeze on `main` (no merges except hotfix)
- [ ] **7.1.2** Quality audit /100 ≥80 verify (Wave 36 baseline = 80; Wave 39+ refresh)
- [ ] **7.1.3** No P0 incidents trong 7 days
- [ ] **7.1.4** Backup snapshot RDS staging (rehearsal restore drill per GAP-117)

### 7.2 T-1 day
- [ ] **7.2.1** Code freeze main
- [ ] **7.2.2** Backup snapshot prod RDS (về S3 cold tier)
- [ ] **7.2.3** Coordinator on-call standby

### 7.3 T-0 deploy
- [ ] **7.3.1** Tag `git tag -s v0.9.0-beta -m "Phase 1 BETA invite-only launch"` && `git push origin v0.9.0-beta`
- [ ] **7.3.2** docker-build-push.yml fires → 9 services pushed với `v0.9.0-beta` + `latest`
- [ ] **7.3.3** release-tag.yml fires → GitHub Release prerelease=true với changelog
- [ ] **7.3.4** Trigger `.github/workflows/deploy-production.yml` workflow_dispatch — user confirms
- [ ] **7.3.5** SSM into prod EC2 → docker-compose pull + up -d
- [ ] **7.3.6** Verify Flyway migrations apply auto (Spring Boot start)
- [ ] **7.3.7** Run `ProductionSeedRunner` (Wave 33 GAP-376) — admin user + system config + first kitehub instance
- [ ] **7.3.8** `bash scripts/smoke-test.sh https://kitehub.vn https://kitehub.me` — 18 assertions
- [ ] **7.3.9** DNS cutover — **Path B BẮT BUỘC cho production** (Path A `sslip.io` không brandable, không SEO; trust score gửi gmail/zalo = 0):
  - [ ] **7.3.9.1** Verify domain `kitehub.vn` + `kitehub.me` registered (Phase 1.1) — nếu chưa, đăng ký qua Mat Bao / PA Vietnam / iNet (~270k VND/năm/domain, ~30 phút onboarding + verify ID)
  - [ ] **7.3.9.2** Cloudflare orange-cloud `kitehub.vn` apex + `www` → AWS ALB DNS (CNAME flattening)
  - [ ] **7.3.9.3** Cloudflare orange-cloud `kitehub.me` apex + `www` → AWS ALB DNS
  - [ ] **7.3.9.4** Verify `dig +short kitehub.vn` → Cloudflare IP; `curl -sI https://kitehub.vn` → 200 + valid cert
  - [ ] **7.3.9.5** Update Better Stack monitors + status page sang custom domain
- [ ] **7.3.10** Public announcement Instatus status page (Phase 1 BETA invite-only live)

---

## Phase 8 — Beta invite (manual + admin tooling)

- [ ] **8.1** Coordinator login admin endpoint
- [ ] **8.2** Manually create first BetaAccessRequest entries (5-10 trusted referrals — P1 prospects + P2 SaaS owners)
- [ ] **8.3** Admin approve → kitehub-email send invite với 24h UUID claim code (Wave 33 GAP-372)
- [ ] **8.4** First tenant signup with claim code end-to-end test (coordinator self-test với own email)
- [ ] **8.5** Verify tenant provisioned với beta-flag=true + dashboard banner "Kite v0.9.0-beta"
- [ ] **8.6** Send remaining 5-10 invites theo wave (1 invite/ngày để monitor)

---

## Phase 9 — Post-launch (agent observe + flag)

### 9.1 T+1h continuous monitoring
- [ ] **9.1.1** Watch Grafana (per GAP-115 PARTIAL — limited dashboards Phase 1)
- [ ] **9.1.2** Watch error rate spike — Sentry equivalent (per GAP-113 partial)
- [ ] **9.1.3** Watch RDS connections + CPU + memory
- [ ] **9.1.4** Statuspage update nếu incident

### 9.2 T+24h daily
- [ ] **9.2.1** Error rate trend
- [ ] **9.2.2** P95 latency
- [ ] **9.2.3** Signup conversion (request → invite → signup completion)
- [ ] **9.2.4** Cost burn rate (AWS Budgets 1st alarm at $80)

### 9.3 T+7d weekly
- [ ] **9.3.1** Beta tenant feedback compile
- [ ] **9.3.2** Performance baseline measure vs Wave 36 (P0 N+1 indexes V31 Wave 35)
- [ ] **9.3.3** Bug-fix sub-PRs queued

### 9.4 T+30d monthly
- [ ] **9.4.1** Full quality audit /100 (Phase 1 BETA promotion gate target ≥80)
- [ ] **9.4.2** Persona-based business review (P1+P2 user feedback)
- [ ] **9.4.3** EC2 right-sizing review (Wave 37 Bucket E `2026-06-template.md` cost report)
- [ ] **9.4.4** Phase 1 → Phase 1.5 promotion decision

---

## Phase 1 BETA promotion gate (post-Phase 9)

Per `release-1-plan-2026.md` §11.1 — promote sang Phase 1.5 PAID khi tất cả:
- [ ] Quality audit /100 ≥80 (rolling 7-day)
- [ ] 5+ beta tenants live + active (≥1 lesson scheduled / class)
- [ ] 0 P0 incidents trong 14 ngày liên tiếp
- [ ] Beta tenant retention ≥80% (no abandoned tenants)
- [ ] Performance P95 latency <500ms cho critical endpoints

---

## Cross-references

- `release-1-plan-2026.md` — 3-phase rollout strategy
- `release-1-deploy-plan.md` — pre-deploy/deploy/post checklist (cần refresh post-Wave-37)
- `release-deploy-standard.md` — per-bump-type artifact checklist
- `versioning-policy.md` — semver convention
- ADR-025 AWS Singapore Free Tier (Wave 35)
- ADR-026 Ollama defer Phase 2 (Wave 37 Bucket E)
- ADR-027 Statuspage vendor Instatus (Wave 38 Bucket C)
- `documents/05-guides/deploy/aws-architecture-sizing-matrix.md` (Wave 37 Bucket E)
- `documents/05-guides/deploy/cloudflare-setup.md` (Wave 38 Bucket B)
- `documents/05-guides/operations/incident-comms-runbook.md` (Wave 38 Bucket C)
- `documents/05-guides/deploy/staging-activation-runbook.md` (Wave 38 Bucket D)

---

## Log

- **2026-05-07 (draft):** Runbook tạo từ re-trace toàn bộ Wave 25-38 outputs sau user-flagged miss "isn't this an E2E test?". Phase 0 plan refresh BLOCKING — `release-1-deploy-plan.md` §1.1+§2.2 vẫn nói Oracle Cloud (stale post-ADR-025). Identified 5 missed-items: (1) plan stale, (2) GitHub repo vars setup, (3) ad-hoc test tag sequence trước RC, (4) OWASP ZAP wire vào staging chain, (5) visual regression baseline. Single-source ordered sequence Phase 0-9 với checkboxes — track per-step completion.
