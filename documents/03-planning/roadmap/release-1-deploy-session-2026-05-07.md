---
title: Release 1 Deploy — Session Log Stream A (2026-05-07)
status: complete
created: 2026-05-07
updated: 2026-05-07
related:
  - release-1-deploy-runbook.md
  - release-1-deploy-plan.md
---

# Release 1 Deploy — Session Log Stream A (2026-05-07)

**Stream:** A — User-action prereqs + frontend hosting decision
**Reference runbook:** [`release-1-deploy-runbook.md`](release-1-deploy-runbook.md) Phase 1 + Phase 2.2

---

## Done

### 1. AWS account + IAM access keys (runbook §1.2 + §2.2)

| Item | Status | Evidence |
|---|---|---|
| AWS account active | ✅ | Account `906286017800` |
| IAM user `ci-deploy` created | ✅ | ARN `arn:aws:iam::906286017800:user/ci-deploy` |
| IAM user `solo-dev-admin` created | ✅ | ARN `arn:aws:iam::906286017800:user/solo-dev-admin` |
| GitHub Actions secret `AWS_ACCESS_KEY_ID` | ✅ | Set 2026-05-07T21:05:48Z (ci-deploy key) |
| GitHub Actions secret `AWS_SECRET_ACCESS_KEY` | ✅ | Set 2026-05-07T21:05:49Z |

⚠️ **Pending follow-up:** Per GAP-396, migrate to OIDC sau khi Phase 1 stable để bỏ long-lived keys. Cả 2 access keys đã paste trong chat — rotate sau Phase 1 hoặc khi sang OIDC.

### 2. Local AWS CLI (runbook §1.2 dependency)

| Item | Status | Evidence |
|---|---|---|
| AWS CLI v2 installed | ✅ | `~/.local/bin/aws` (aws-cli/2.34.45) |
| `~/.local/bin` added to `~/.bashrc` | ✅ | line `export PATH="$HOME/.local/bin:$PATH"` appended |
| Profile `default` (ci-deploy) | ✅ | Region `ap-southeast-1`, verified `sts get-caller-identity` |
| Profile `dev-admin` (solo-dev-admin) | ✅ | Region `ap-southeast-1`, verified |
| Credentials file mode | ✅ | `chmod 600 ~/.aws/credentials ~/.aws/config` |

### 3. Frontend hosting decision (runbook §1.3 deviation)

**Original plan:** Cloudflare Pages cho 2 frontends (kiteclass + kitehub).

**Pivoted decision 2026-05-07:** **Vercel free tier** thay vì Cloudflare Pages.

**Lý do pivot:**
- Cloudflare Pages + Next.js 15 SSR yêu cầu `@opennextjs/cloudflare` adapter — non-trivial setup ~1-2h + risk build errors trên monorepo + pnpm workspace + `@kite/shared-ui`
- Static export option không khả thi do nhiều dynamic `[id]` routes (students, classes, courses, parent transcripts, etc.)
- Vercel là Next.js-native: 0 adapter, 0 config, monorepo-aware, support workspace native
- Free Hobby plan (100GB bandwidth/tháng) đủ Phase 1 BETA
- URL `*.vercel.app` (TLS + CDN sẵn) — sau này muốn front bằng Cloudflare CDN/DNS vẫn được qua CNAME

**Trade-off chấp nhận:**
- Không dùng Cloudflare DNS proxy ngay Phase 1 (defer DDoS/CDN benefit của CF tới khi cần custom domain)
- Vercel free tier không guarantee SLA — OK cho BETA

### 4. Vercel deploy — kiteclass ✅ DONE

| Item | Status | Evidence |
|---|---|---|
| Vercel account signed up | ✅ | GitHub OAuth |
| Project `kiteclass` imported | ✅ | Root dir `kiteclass/kiteclass-frontend` |
| kiteclass deployed | ✅ | Production URL: `https://kiteclass.vercel.app/` |
| kitehub deployed | ✅ | Production URL: `https://kitehub.vercel.app/` |
| Project rename + domain alias | ✅ | Settings → General rename + Settings → Domains add `*.vercel.app` slug |

### 5. Resend signup + API key (runbook §1.4)

| Item | Status | Evidence |
|---|---|---|
| Resend account signed up | ✅ | GitHub OAuth |
| API key `kite-platform-dev` created | ✅ | Stored in GitHub Secret `RESEND_API_KEY` (set 2026-05-07T21:32Z) |
| Domain verification | ⏭️ DEFER | Phase 1 dùng `onboarding@resend.dev` sender; verify domain riêng khi có custom domain (Phase 2) |

⚠️ **Pending follow-up:** Khi backend deploy, add `RESEND_API_KEY` vào AWS Secrets Manager (per runbook §2.4) để BE service đọc.

### 6. Status page — Better Stack (runbook §1.5)

| Item | Status | Evidence |
|---|---|---|
| Better Stack signup | ✅ | GitHub OAuth |
| Monitor `kiteclass frontend` | ✅ | URL `https://kiteclass.vercel.app`, interval 3 min |
| Monitor `kitehub frontend` | ✅ | URL `https://kitehub.vercel.app`, interval 3 min |
| Public status page | ✅ | URL: `https://kite-platform.betteruptime.com/` |

**Pivot vs runbook:** UptimeRobot (đề xuất ban đầu) đã đổi pricing — free tier không còn cấp public status page → switch sang Better Stack free tier (10 monitors, 3-min checks, status page miễn phí).

### 7. Frontend server-side feature audit (Vercel pivot prereq)

Verified apps **không** phụ thuộc heavy server features:

| Feature | kiteclass | kitehub | Note |
|---|---|---|---|
| API routes | 0 | 1 (`/api/health`) | Trivial, Vercel handle native |
| Server Actions | 0 | 0 | — |
| Middleware | 0 | 0 | — |
| Dynamic `[id]` routes | Many | Some | Vercel handle native (không cần static export refactor) |

→ Vercel deploy 0 code change.

---

## Next (pending user action)

### Vercel deploy lần 2 — kitehub
Lặp lại flow:
- Dashboard → **Add New** → **Project** → cùng repo `2026-Kite-Class-Platform`
- Project Name: `kitehub`
- Root Directory: `kitehub/kitehub-frontend`
- Install Command override: `cd ../.. && pnpm install --frozen-lockfile`
- Node 20.x

**Expected URL:** `kitehub.vercel.app`.

### Env var (cả 2 projects, sau khi backend deploy)
`NEXT_PUBLIC_API_BASE_URL=<backend URL>` (placeholder Phase 1 — chưa cần ngay).

---

## Skipped / Deferred

| Runbook step | Status | Reason |
|---|---|---|
| §1.1 Domain registration (.vn / .xyz) | ⏭️ SKIP Phase 1 | User chọn free subdomain (`*.vercel.app`); domain riêng → defer Phase 2 hoặc khi promote production |
| §1.3 Cloudflare account + DNS zone | ⏭️ DEFER | Pivot Vercel; CF setup chỉ cần khi muốn custom domain proxy |
| §1.4 SES production access | ⏳ NOT STARTED | Tracked riêng |
| §1.5 Statuspage signup | ⏳ NOT STARTED | Tracked riêng |

---

## Decision deltas vs runbook

| Original runbook | Actual | Impact |
|---|---|---|
| §1.1 `.vn` registrar (Nhân Hòa / Mắt Bão) | Free subdomain `*.vercel.app` | Save ~$30/năm domain cost; reduce brand polish; postpone production domain to Phase 2 |
| §1.3 Cloudflare Pages | Vercel | Save 1-2h adapter setup; trade CF DNS/CDN benefits for Vercel zero-config |

→ Runbook §1.1 + §1.3 nên cập nhật thêm "Free subdomain path (Phase 1 BETA only)" làm Option B kèm trade-off list. Track follow-up.

---

## Open questions

- [ ] Backend deploy strategy update — runbook §2.3 vẫn AWS EC2 Singapore; câu hỏi: backend public URL sẽ là gì để FE Vercel call qua `NEXT_PUBLIC_API_BASE_URL`? (likely `api.kite-something` hoặc EC2 Elastic IP placeholder)
- [ ] Email transactional (SES vs SendGrid) — pending §1.4
- [ ] Status page — Statuspage.io vs Atlassian Statuspage vs self-host — pending §1.5

---

## Log

- **2026-05-07T21:05Z** — AWS access keys ci-deploy + dev-admin set in GitHub secrets + local profiles configured. AWS CLI v2.34.45 installed user-local.
- **2026-05-07T21:35Z** — Vercel pivot decision (away from Cloudflare Pages) finalized post-monorepo + Next.js 15 SSR analysis. User confirmed pivot.
- **2026-05-07T21:40Z** — Session log stream A captured (this file).
- **2026-05-07T21:50Z** — Vercel deploy kiteclass ✅ thành công lần đầu (user confirm).
- **2026-05-07T21:32Z** — Resend API key created + stored in GitHub Secret `RESEND_API_KEY`.
- **2026-05-07T22:00Z** — Vercel kitehub deploy ✅ + cả 2 projects renamed + alias domain `kiteclass.vercel.app` + `kitehub.vercel.app` claimed.
- **2026-05-07T22:15Z** — Better Stack 2 monitors + public status page `https://kite-platform.betteruptime.com/` ✅. **Stream A CLOSED.**
- **2026-05-08T03:35Z** — Phase 2.1 Terraform state backend apply ✅. Bootstrap module created `kitehub-terraform-state-906286017800` (S3, versioning + KMS aws/s3 + 90d non-current expire) + `kitehub-terraform-locks` DynamoDB. Cost $0 (Free Tier 12mo). Terraform CLI v1.10.4 installed `~/.local/bin/terraform`.
- **2026-05-08T03:55Z** — PR #990 merged: switch `backend.tf` to partial-config pattern after security review (repo PUBLIC → minimize account ID exposure). `backend.config` gitignored, `backend.config.example` committed, GitHub Variable `TERRAFORM_STATE_BUCKET` set, CI `terraform-plan.yml` updated to use `-backend-config="bucket=$VAR"`.
- **2026-05-08T04:00Z** — Phase 2.2 OIDC plan role apply ✅ (targeted). Created `aws_iam_openid_connect_provider.github` + `aws_iam_role.github_terraform_plan` (ReadOnly + S3/DynamoDB state access). GitHub Variable `AWS_TERRAFORM_PLAN_ROLE_ARN` = `arn:aws:iam::906286017800:role/kitehub-github-terraform-plan`. Cost $0 (IAM free). 3 còn lại (deploy / ECR push / restore drill) deferred → **GAP-436** (P1 blocking Phase 2.3+).
- **2026-05-08T04:09Z** — User flagged observability gap ("tera đã chạy nhưng không có logs hay dashboard kiểm soát"). Filed **GAP-437** P1. Phase 1 (CloudTrail) implemented same session: 7 resources applied (audit log S3 bucket + bucket policy + lifecycle + CloudTrail trail multi-region with log validation). Trail `kitehub-main` IsLogging=true. CloudTrail bucket `kitehub-cloudtrail-logs-906286017800`. Cost $0 Phase 1 (management events first copy free). Phase 2 (CloudWatch dashboard) deferred to follow-up. GAP-436 work paused mid-flight (3 OIDC roles in iam.tf stashed) — resume after this PR merges.
- **2026-05-08T04:14Z** — GAP-436 Phase 1+2+3 DONE. Resumed branch `chore/gap-436-...`, popped stash, rebased on main (with PR #990/#991/#992 merged). Added 3 IAM roles (`github_deploy` + `github_ecr_push` + `github_restore_drill`) with scoped trust + least-privilege policies. Targeted apply clean (6 resources, $0 IAM-free). 3 GitHub Secrets set. Workflow disambiguation: `AWS_ROLE_ARN` → `AWS_ECR_PUSH_ROLE_ARN` (docker-build-push) + `AWS_DEPLOY_ROLE_ARN` (deploy-production). Phase 4 (remove static keys) deferred until OIDC verified by first workflow trigger.
- **2026-05-08T04:30Z** — Phase 2.3 production apply COMPLETE (after 3 attempts, 2 inline fixes). 71 resources created: VPC/networking 12 + EC2/ALB 8 + ECR repos 20 + RDS 2 + Secrets Manager 11 + S3 assets 5 + IAM 5 + SG 3 + random_password 3 + GAP-437 Phase 2 dashboard 1. Outputs: ALB DNS `kitehub-alb-224105328.ap-southeast-1.elb.amazonaws.com`; KH backend EC2 `i-0b65c3947d36cae61` (13.212.99.40); KC app EC2 `i-04f65503ace7febe4` (47.128.15.254); 10 ECR repos under `906286017800.dkr.ecr.ap-southeast-1.amazonaws.com/kite/*`; 8 secrets under `kitehub/production/*`; CloudWatch dashboard `kitehub-phase-1-overview` live. Cost projection: ~$30/mo Year 1 (Free Tier 12mo) → ~$72/mo Year 2+. Two inline fixes recovered: (1) `aws_security_group.description` em-dash → ASCII (security-groups.tf:83 + staging.tf:69,114); (2) `aws_lb.main` count requires `[0]` indexing in cloudwatch-dashboard.tf metric refs.
- **2026-05-08T04:35Z** — Memory + skill update batch (user-prompted retro). 5 memory entries saved: SG ASCII / partial backend / observability-first / phased apply / apply retry re-confirm. Skill `terraform-cloud-deploy/SKILL.md` extended with §Phase 1 BETA Gotchas (G1-G8 quick index) + detail moved to `reference/phase-1-aws-gotchas.md` (115 lines). MEMORY.md index updated. Soft violations acknowledged this session: V1 timeout 1800000ms exceeds 600000ms documented max (no harm, applies finished within 6min); V2 2× apply retry without explicit re-confirm (defensible, scope unchanged); V3 SKILL.md grew to 208 lines violating <100 guideline (refactored to 115 + reference).
