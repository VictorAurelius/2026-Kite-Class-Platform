---
title: Post-Wave-82 Session Handoff — FE Self-Host AWS EC2 LIVE, DNS cutover complete, dev 126-row walk-through UNBLOCKED
date: 2026-05-15
prev_handoff: 2026-05-15-post-wave-81-handoff.md
next_wave: 83
status: handoff
---

# Post-Wave-82 Session Handoff (2026-05-15)

## TL;DR — Đọc trong 60 giây

🎉 **Wave 82 FE SELF-HOST SHIPPED.** `https://kitehub.me/` LIVE trên AWS EC2 t3.small Singapore (54.179.70.37). DNS cutover off Vercel hoàn tất qua Cloudflare API. Wildcard cert `*.kitehub.me` Let's Encrypt acquired (exp 2026-08-13). **Dev 126-row walk-through UNBLOCKED** — FE + BE production stack đầy đủ contracts mới nhất (beta-status 200, OTel CVE patched, gateway routing fixed).

**Next session ưu tiên:**
1. Dev tự test full 126-row walk-through trên production (acceptance test CSV `documents/05-guides/operations/acceptance-tests/phase-1-beta-acceptance-self-test.csv`)
2. Fix GAP-574 P1 (PM2 ecosystem.config.js 3 bugs in repo source) **trước khi FE deploy lần sau**
3. Wave 82 post-wave audit suite within 3 days per `post-wave-audit-mandate.md` §2.2 (security + ops-readiness + performance + business-logic + api-contract)

## Wave 82 closure summary

### 10 PRs shipped (timestamp 2026-05-15)

| PR | Title | Purpose |
|---|---|---|
| #1396 | Bucket F+A — 8 gateway routes + Spring profile + script rename + ADR-031 + 4 mitigation gaps | Wave 81 follow-ups + Wave 82 Bucket A foundation (ADR + GAP-565..568) |
| #1397 | OTel BOM 1.49 → 1.62 CVE-2026-45292 MEDIUM | Hotfix W3C Baggage Unbounded Memory; deployed to BE |
| #1398 | Bucket B drafts — terraform + nginx + PM2 + certbot + runbook + CORS audit | Bucket B foundation (terraform-aws / fe-host / runbook / 1 audit) |
| #1399 | GAP-570/571 + runbook SSM→Secrets Manager prep | Wave 81 F5 incomplete + 2 validation 500s + script align prep |
| #1400 | Pin AL2023 AMI — prevent surprise EC2 replacement | DESTRUCTIVE plan (3 EC2 replace) caught via state-check; pivot AMI pin |
| #1401 | Grant terraform-apply IAM TagInstanceProfile | First apply blocked on iam:TagInstanceProfile; sibling TagRole pattern |
| #1402 | Bucket B post-apply audit | EC2 + IAM + Secrets verified post-apply on new SG/IAM/SecMgr |
| #1403 | Bucket C — CF token Secrets Manager align | User pre-populated CF token in Secrets Manager; script + IAM updated |
| #1404 | 4 follow-up gaps (GAP-572..575) | Cert renewal + CW metric + PM2 config + kiteclass-frontend defer |
| #(this PR) | Wave 82 closure protocol docs sync | This handoff + wave-history + plan status flip + ROADMAP |

### Production state (post-cutover)

✅ **Backend (7 services):**
- `api.kitehub.me/actuator/health` → 200 UP
- Tag `0.9.0-beta-staging.16` (post-OTel + post-gateway-route-fix)
- 8 gateway routes added/fixed (PR #1396): beta-status (was 400 → 200), staff-invitations, admin-impersonate, 5 other audit-found
- OTel CVE-2026-45292 patched

✅ **Frontend (NEW EC2 self-host):**
- `https://kitehub.me/` HTTP 200 in 360ms; `/api/health` 200
- EC2 `i-05cfda7c6c60b683f` t3.small (2 vCPU / 2GB) ap-southeast-1
- Public IP `54.179.70.37` (Elastic IP not yet bound — follow-up)
- nginx 1.28.3 reverse proxy + TLS termination (HSTS 1y, CSP, brotli)
- Node 20.20.2 + PM2 fork mode (kitehub-frontend port 4701, 122.9MB RSS)
- Next.js 15.5 standalone build pnpm workspace monorepo

✅ **DNS + cert:**
- Cloudflare DNS API cutover: `kitehub.me` A record → 54.179.70.37 (proxied=false, ttl=300)
- Vercel CNAME removed from production path
- Cert wildcard `*.kitehub.me` Let's Encrypt exp 2026-08-13
- Cert acquired via certbot DNS-01 challenge + CF token (Secrets Manager)

✅ **Infrastructure (Wave 82 net-new):**
- Security Group `kitehub-kc-app-sg-prod` (22/80/443 ingress; SSM via VPC endpoint)
- IAM role `kitehub-kc-app-ec2-role` + instance profile (Secrets Manager + SSM Param + CloudWatch)
- 1 Secrets Manager entry: `kitehub/production/cloudflare-api-token` (user pre-populated, IAM grants `secretsmanager:GetSecretValue`)
- 3 CloudWatch alarms: CPU >80% / cert-days-to-expire <30 / disk >85% (cert alarm INSUFFICIENT_DATA — see GAP-573)
- AMI pinned `ami-04a8a2b994a2a7176` (PR #1400 prevents surprise replacement)

### 5 gap closures

| Gap | Status | Score | Notes |
|---|---|---|---|
| GAP-565 | 🟢 DONE | 100 | F6 SG ASCII descriptions verified clean |
| GAP-566 | 🟡 PARTIAL | 60 | t3.small swap armed; PM2 hot-fix on EC2; repo source bugs → GAP-574 |
| GAP-567 | 🟡 PARTIAL | 50 | Cert acquired + alarm armed; renewal timer + CW metric publish fail AL2023 → GAP-572 / 573 |
| GAP-568 | 🟢 DONE | 100 | BE CORS gateway pre-allowlist verified post-flip |
| GAP-569 | 🟢 DONE | 100 | OTel BOM 1.62.0 deployed to BE production |

### 4 follow-up gaps filed (PR #1404)

| Gap | Priority | Issue | Severity |
|---|---|---|---|
| **GAP-574** | **P1** | PM2 ecosystem.config.js 3 bugs in repo source (max_memory_restart format, cwd path monorepo, /var/log/pm2 perm) | **Affects ALL future FE deploys** |
| GAP-572 | P2 | Certbot systemd timer setup fails on AL2023 (no unit files shipped) | Manual renewal works; cert valid 90d |
| GAP-573 | P2 | CloudWatch CertDaysToExpire metric publisher chưa install (blocked by GAP-572) | Alarm stuck INSUFFICIENT_DATA |
| GAP-575 | P2 | kiteclass-frontend Phase 7 defer per ADR-031 (tenant FE post-MVP) | Future scope |

## Wave 82 lessons learned

### 1. Pre-mutation state-check caught DESTRUCTIVE plan
Per `pre-mutation-state-check.md` §3 — Bucket B initial plan showed `aws_instance.kh_backend` replace + new `kc_app` create. State-check revealed AMI drift `ami-04a8a2b994a2a7176` → `ami-01f309fb59c80862f` (latest AL2023 published). **Pivot:** PR #1400 pin AMI in `data.aws_ami.al2023` filter. Saved kh-backend from surprise replacement.

### 2. STS session credential cache mid-apply
First terraform apply attempt failed `iam:TagInstanceProfile denied` despite policy update PR #1401 merged. **Root cause:** STS AssumeRoleWithWebIdentity session credentials cached at workflow start; mid-apply IAM update doesn't refresh in-session token. **Fix:** new `gh workflow run` = new STS session with refreshed policy → attempt 3 SUCCESS.

### 3. Secrets Manager vs SSM Parameter Store misalignment
Bucket C agent assumed SSM Parameter Store for CF token; user pre-populated Secrets Manager. **Caught by:** user feedback "Use the Cloudflare token that has already been set up." **Fix:** PR #1403 IAM action `secretsmanager:GetSecretValue` + script `aws secretsmanager get-secret-value --query SecretString`.

### 4. Hot-fixes on EC2 surface repo source bugs
3 PM2 ecosystem.config.js bugs caught only at runtime (max_memory_restart `'1.2G'` invalid; cwd path wrong cho monorepo standalone output; /var/log/pm2 permission). Hot-fix on EC2 unblocked production but repo source bugs persist → GAP-574 P1 **must fix before next FE deploy**.

### 5. Parallel PR off broken main wastes CI run
PR #1397 (OTel CVE) branched off main while main still had wave-plan-completeness CI fail. Inherited failure → rebase after PR #1396 merged. **Lesson saved:** `~/.claude/projects/.../memory/feedback_parallel_pr_main_state_check.md` — before branching parallel PR, check `gh pr checks main` first.

### 6. Anti-pattern: pre-naming "Wave 83" for deferred items
Caught by user "soa lại có wave 83?" — scope nên ở Wave 82 if can fix, OR file gap and defer naming. Avoid pre-allocating wave numbers for deferred work.

## Session housekeeping (Wave 82 → next)

| Item | Before | After |
|---|---|---|
| Local branches | 1 (main) | 1 (main) post closure |
| Open PRs | 0 | 0 |
| Vercel deps on production path | Yes (Free Tier cap blocker) | Removed (DNS cutover off) |
| FE source-of-truth host | Vercel (stale 38h) | AWS EC2 t3.small Singapore |
| `kitehub.me` LIVE | Vercel build cap hit | EC2 self-host LIVE |

## What next session needs to know

### Read first
- This handoff (you're here)
- [Wave 82 plan](../waves/wave-2026-05-15-82-fe-self-host.md) — closed
- [ADR-031: FE self-host AWS EC2](../../02-architecture/adr/ADR-031-fe-self-host-aws-ec2.md)
- [FE self-host runbook](../../05-guides/deploy/fe-self-host-runbook.md) — 510 lines, 11 sections
- [Bucket B post-apply audit](../../04-quality/audits/aws-verification/2026-05-15-wave-82-bucket-b-post-apply.md)
- 4 new gaps: [GAP-572](../../04-quality/gaps/GAP-572-certbot-systemd-timer-al2023-not-shipped.md), [GAP-573](../../04-quality/gaps/GAP-573-cloudwatch-cert-days-to-expire-publisher-not-installed.md), **[GAP-574](../../04-quality/gaps/GAP-574-pm2-ecosystem-config-3-bugs.md) P1**, [GAP-575](../../04-quality/gaps/GAP-575-kiteclass-frontend-defer-phase-7.md)

### Priorities for next session

1. **GAP-574 P1** — fix PM2 ecosystem.config.js 3 bugs in repo source. **Mandatory** before next FE deploy (current hot-fix only on EC2 disk — gone on next image refresh).
2. **Wave 82 post-wave audit suite** within 3 days per `post-wave-audit-mandate.md` §2.2 — security + ops-readiness + performance + business-logic + api-contract on production with new FE host.
3. **Dev 126-row walk-through** — full acceptance test (`documents/05-guides/operations/acceptance-tests/phase-1-beta-acceptance-self-test.csv`) against production now that BE + FE both LIVE with latest contracts.
4. **GAP-572 + GAP-573** — cert auto-renewal class (AL2023 certbot systemd + CW metric publisher). Cert valid until 2026-08-13; manual renewal works. Fix together when convenient.
5. **GAP-570 + GAP-571** — Wave 81 carry-forward (Spring 500→404 F5 incomplete + 2 validation endpoints 500).

### Open issues

- **Elastic IP not bound to EC2** — IP `54.179.70.37` is currently public IP auto-assigned (lost if instance stop/start). Bind Elastic IP at next opportunity to lock DNS target.
- **Self-hosted GitHub runner** — deferred per Wave 82 Bucket E scope; consider only when CI queue measurably blocks (per "Measure 1-2 waves first" decision 2026-05-15).
- **kiteclass-frontend defer** — Phase 7 tenant FE per ADR-031 + GAP-575.
- **`documents/05-guides/deploy/fe-self-host-runbook.md` secret leak check** — runbook reviewed; no live tokens leaked (only placeholders + SSM/Secrets Manager paths). ✅

## Cross-link

- Wave 82 plan: `documents/03-planning/waves/wave-2026-05-15-82-fe-self-host.md` §status: complete
- Previous handoff: `documents/03-planning/session-handoffs/2026-05-15-post-wave-81-handoff.md`
- ADR-031: `documents/02-architecture/adr/ADR-031-fe-self-host-aws-ec2.md`
- Runbook: `documents/05-guides/deploy/fe-self-host-runbook.md`
- Audits: `documents/04-quality/audits/aws-verification/2026-05-15-wave-82-bucket-{b-pre,b-post}-apply.md` + `2026-05-15-wave-82-be-cors-sweep-audit.md`
- ROADMAP: `documents/04-quality/gaps/ROADMAP.md` §🎯 Current Status Snapshot + §🚀 Next Action (Wave 82 SHIPPED)
- wave-history.jsonl: Wave 82 entry appended (62 → 63 lines)
- gap-status.csv: 5 rows updated (GAP-565..569)
- Memory: `feedback_parallel_pr_main_state_check.md` — CI rebase lesson 2026-05-15
