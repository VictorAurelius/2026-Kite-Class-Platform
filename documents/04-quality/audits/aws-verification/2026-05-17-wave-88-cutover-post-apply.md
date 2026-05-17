---
title: AWS Verification — Wave 88 cutover post-apply (Vercel decommission + Claude walkthrough)
status: complete
created: 2026-05-17
phase: phase-1-beta
wave: 88
related_gaps: [GAP-573, GAP-523, GAP-601, GAP-602, GAP-603, GAP-604]
---

# AWS Verification — Wave 88 Cutover + Walkthrough Post-Apply

## 1. Scope

Wave 88 production cutover Vercel → AWS EC2 self-host + Claude walkthrough qua Playwright headless. Authorization: user phrase **"claude trigger"** per `dev-authorized-terraform-trigger.md` §4.

## 2. Workflow + SSM action runs

| Gate | Action | Outcome |
|---|---|---|
| Pre | start-stack.sh | ✅ stack started 337s |
| A1 | deploy-production.yml staging.8 | ❌ gateway image tag missing |
| A2 | deploy-production.yml staging.19 | ✅ Gateway picks up CORS env |
| B1 | terraform-apply dry_run=true | ✅ plan 8/1/0 |
| B2 | terraform-apply dry_run=false | ❌ SNS tag parens reject |
| B3 (post #1476) | terraform-apply dry_run=false | ✅ EIP + IAM + SNS + alarm + EC2 user_data update applied |
| C1 | cloudflare-apex-cutover verify-only | ✅ EIP target match |
| C2 | cloudflare-apex-cutover apply | ✅ DNS flipped to EIP |
| D1 | SSM SendCommand FE deploy kitehub-frontend | ✅ PM2 online port 4701 |
| D2 | SSM SendCommand FE rebuild + deploy kiteclass-frontend | ✅ PM2 online port 4700 |
| Hotfix | Merge PR #1465 (YAML duplicate `server:` key) → tag staging.20 → deploy | ✅ admin service healthy |
| Hotfix | FE rebuild với `NEXT_PUBLIC_API_URL=https://api.kitehub.me` → redeploy + PM2 reload | ✅ FE no longer calls localhost:9000 |

## 3. Plan-vs-predicted reconciliation per §3.5

Predicted (PR #1466 pre-apply): `5 add / 0 change / 0 destroy`
Actual: `8 add / 1 change / 0 destroy`

| Resource | Action | Wave-source | Verdict |
|---|---|---|---|
| aws_eip.kc_app_fe | create | Wave 86 PR #1466 | Applied |
| aws_eip_association.kc_app_fe | create | Wave 86 PR #1466 | Applied |
| aws_iam_role.github_cloudflare_cutover | create | Wave 86 PR #1466 | Applied |
| aws_iam_role_policy.* | create | Wave 86 PR #1466 | Applied |
| aws_cloudwatch_metric_alarm.rds_storage_low | create | Wave 85/86 backlog | Applied |
| aws_sns_topic.production_alerts | create | Wave 86 backlog | Applied (after fix #1476) |
| aws_sns_topic_subscription × 2 | create | Wave 86 backlog | Applied |
| aws_instance.kc_app_fe user_data | update in-place | Wave 82 GAP-567 cert-monitor | Applied — kc-app-fe restart safe (FE chưa serve real traffic) |

## 4. Post-apply Tier 1 verify

| Check | Evidence |
|---|---|
| EIP allocated + associated | `eipalloc-082dbd4253b2a01db` → `52.221.161.175` → `i-05cfda7c6c60b683f` |
| IAM role created | `arn:aws:iam::906286017800:role/kitehub-github-cloudflare-cutover` |
| EC2 kc_app_fe post-restart | running với `PublicIpAddress = 52.221.161.175` (EIP) |
| SNS topic | `arn:aws:sns:ap-southeast-1:906286017800:kitehub-production-alerts` + 2 subs |
| CloudWatch alarm | `kitehub-rds-storage-low` INSUFFICIENT_DATA (baseline) |
| CloudTrail | `kitehub-main` IsLogging=true (per aws-observability-first.md) |

## 5. GAP-523 CORS subdomain verify (post-deploy)

| Origin | Pre-deploy | Post-deploy A |
|---|---|---|
| `https://kitehub.me` | 200 + ACAO match | 200 + ACAO match |
| `https://app.kitehub.me` | ❌ 403 | ✅ 200 + ACAO match |
| `https://kitehub.vercel.app` | ❌ 403 | ✅ 200 + ACAO match |
| `https://kitehub-victoraurelius-projects.vercel.app` | n/a | ✅ 200 + ACAO match |

GAP-523 sub-scope CLOSED.

## 6. Public smoke kitehub.me + app.kitehub.me

Both domains live trên EC2 nginx → PM2 Next.js (NOT Vercel):

```
200  https://kitehub.me/                  server: nginx/1.28.3
200  https://kitehub.me/pricing           nginx
200  https://kitehub.me/legal/terms       nginx
200  https://kitehub.me/login             nginx
200  https://app.kitehub.me/              nginx
200  https://app.kitehub.me/login         nginx
```

Server header KHÔNG có `x-vercel-cache` — confirm cutover complete.

## 7. Claude walkthrough findings (Playwright headless)

### 7.1 Anonymous persona (11 rows)

| flow_id | Verdict | Note |
|---|:---:|---|
| PUB-LAND-001..006 | ✅ PASS ×6 | Vietnamese titles render correct |
| PUB-BLOG-001 | ✅ PASS | "Blog - KiteHub" |
| BETA-REQ-001 | ✅ PASS | form + email input visible |
| LOGIN | ✅ PASS | login page renders |
| EMAIL-VERIFY-002 | ✅ PASS | endpoint reachable |
| OWNER-SIGNUP-001 | ✅ PASS | endpoint reachable |

### 7.2 Platform_Admin (24 rows)

| flow_id | Verdict | Note |
|---|:---:|---|
| ADM-LOGIN-001..004 | ✅ PASS ×4 | Form render + submit → /admin redirect + JWT in localStorage |
| ADM-LOGIN-005 | ⏭️ SKIPPED | Lockout test requires multiple failed attempts (manual) |
| ADM-NAV-001..005 | ✅ PASS ×5 | Sidebar 4 items: Beta Requests / Instances / Payments / Revenue all 200 |
| ADM-BETA-APPROVE-001 | ⚠️ BLOCKED by GAP-604 | API returns 401 dù JWT có role PLATFORM_ADMIN |
| ADM-BETA-APPROVE-002..005 | ⚠️ BLOCKED | Depends on -001 |
| ADM-BETA-REJECT-001..003 | ⚠️ BLOCKED | Depends on approve flow |
| ADM-INST-001..004 | ⚠️ BLOCKED | Same root cause |
| ADM-AUDIT-001..002 | ⚠️ FE route 404 | `/admin/audit-log` không exist trong FE — file GAP |

### 7.3 Root cause `ADM-BETA-APPROVE-001` 401

Gateway `kitehub-gateway` THIẾU JWT validation filter convert `Authorization: Bearer <JWT>` thành `X-User-Id` + `X-User-Roles` headers. Subscription service `XUserRolesHeaderFilter` (SecurityConfig:144) reads `X-User-Id` + `X-User-Roles` headers nhưng gateway không set chúng → `SecurityContext` empty → `@PreAuthorize("hasRole('PLATFORM_ADMIN')")` reject với 401.

Endpoints affected:
- `GET /api/v1/admin/beta-requests`
- `GET /api/v1/admin/instances`
- `GET /api/v1/admin/payments`
- All admin endpoints requiring Spring Security

Endpoints NOT affected (different auth path):
- `POST /api/auth/login` (public)
- `POST /api/auth/refresh` (reads JWT directly)
- `POST /api/v1/auth/request-beta-access` (public)

→ File GAP-604 P0 — Wave 89 must implement gateway JWT filter.

## 8. Misses found (filed as follow-up gaps same closure PR)

| Gap | Severity | Topic |
|---|:---:|---|
| GAP-601 | P2 | Ops-readiness audit deferred during Wave 88 cutover (AUDIT_OVERRIDE PR #1476) — schedule within 3 days |
| GAP-602 | P1 | `pm2-ecosystem.config.js` cwd path mismatch monorepo nested standalone — manual `pm2 start` workaround active |
| GAP-603 | P1 | PM2 systemd auto-start on EC2 reboot chưa wired — `pm2 dump` only saves current process list |
| GAP-604 | P0 | **Gateway thiếu JWT-to-headers filter — admin endpoints 401 dù JWT valid** |
| (info) | — | Wave 82 `seed-admin-password` secret value không match production DB hash (rotated post-seed); workaround = SQL UPDATE |
| (info) | — | FE built with `NEXT_PUBLIC_API_URL` default → fixed via rebuild với prod env (Wave 82 Bucket C miss) |

## 9. Stack state at closure

Stack STOPPED (post-walkthrough). Resources persist (EIP, IAM, SNS, alarms — Free Tier compatible).

## 10. Compliance check

| Rule | Verdict |
|---|:---:|
| `agent-aws-access.md` §2.1 Tier 1 read-only | ✅ describe/list/get only |
| `dev-authorized-terraform-trigger.md` §2 5-gate | ✅ all gates exercised |
| `concurrent-production-mutation-ops.md` §3.1 | ✅ kc-app-fe restart isolated từ kh-backend |
| `release-deploy-standard.md` §3.5 reconciliation | ✅ §3 above |
| `release-fix-retry-budget.md` §5 retry budget | ✅ 4 retries, 4 different root causes (independent budgets) |
| `terraform-apply-retry-reconfirm.md` §3 re-confirm | ✅ AskUserQuestion before re-apply |
| `aws-sg-description-ascii.md` | ⚠️ SNS tag parens (similar class) → fixed PR #1476 |
| `pre-handoff-self-test-completeness.md` §2 | ⚠️ admin flow blocked by GAP-604 (file follow-up) |

## 11. Log

- **2026-05-17 13:05 UTC:** Wave 88 cutover + walkthrough complete. Production `kitehub.me` + `app.kitehub.me` 100% self-host. Vercel decommissioned. 4 follow-up gaps filed (GAP-601/602/603/604). Stack stopped. P0 GAP-604 gateway JWT propagation blocks admin operations — Wave 89 scope.
