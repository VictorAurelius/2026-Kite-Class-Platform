---
title: AWS Verification — Wave 90 live verify (Wave 89 follow-up)
status: complete
created: 2026-05-17
phase: phase-1-beta
wave: 90
related_gaps: [GAP-604, GAP-602, GAP-603]
---

# AWS Verification — Wave 90 Live Verify

## 1. Scope

Live verification of Wave 89 PRs #1479 (PM2 ops) + #1480 (gateway JWT) deployed to production EC2. Authorization: user "claude trigger" → full Wave 90 sequence (deploy + terraform + reboot test).

## 2. Sequence executed

| Gate | Action | Outcome | Wall-clock |
|---|---|---|---|
| 0 | `bash scripts/aws/start-stack.sh` (AWS_PROFILE_START=default override) | ✅ stack STARTED 331s | ~5.5min |
| 1 | Render XLSX `phase-1-beta-acceptance-self-test.xlsx` (openpyxl) | ✅ 127 rows, 13 cols | ~5s |
| 2a | Login probe with secret value | ❌ 400 "Invalid email or password" (DB hash desync, Wave 88 §8 pattern) |  |
| 2b | SSM SQL UPDATE password hash (psql dollar-quote `$DOLLAR$...$DOLLAR$`) — 2 retries to escape `$2a$` mangling | ✅ DB hash realigned to bcrypt(`H%TU...`) |  |
| 2c | Login retry → JWT issued with `role=PLATFORM_ADMIN` | ✅ JWT len 304 |  |
| 3 | GAP-604 **baseline** probe (old gateway, no JWT filter) | `/api/v1/admin/beta-requests` → **HTTP 401** |  |
| 4 | `git tag v0.9.0-beta-staging.21 + push` → docker-build-push.yml | ✅ run 25995733700 success | ~3min |
| 5 | `deploy-production.yml staging.21 confirm=DEPLOY` | ✅ run 25995821417 success | ~2min |
| 6 | Wait gateway + subscription bootup | ✅ ready ~3min |  |
| 7 | GAP-604 **post-deploy** probe with same JWT | `/api/v1/admin/beta-requests` → **HTTP 200** ✅ |  |
| 8 | `terraform-apply.yml dry_run=true targets=aws_instance.kc_app_fe confirm=APPLY` | ✅ Plan: 0/1/0 (user_data in-place change) | ~1min |
| 9 | `terraform-apply.yml dry_run=false targets=aws_instance.kc_app_fe` | ✅ apply success | ~1.5min |
| 10 | Wait kc_app_fe stop→start cycle | ✅ FE up at 23:09:37 |  |
| 11 | SSM probe pm2 + systemd state → `(none yet)` confirmed | ✅ GAP-603 problem reproduced |  |
| 12 | SSM `pm2 startup systemd -u ec2-user ...` → `systemctl enable pm2-ec2-user` | ✅ unit installed + enabled |  |
| 13 | SSM `pm2 resurrect` from dump.pm2 | ✅ both apps online; cwd correct (monorepo nested path) |  |
| 14 | SSM `pm2 save` → fresh dump.pm2 written | ✅ 11723 bytes |  |
| 15 | `aws ec2 reboot-instances kc_app_fe` | ✅ rebooted at 23:26:10 |  |
| 16 | Wait FE recovery → kitehub.me 200 | ✅ FE back at 23:26:47 (37s) |  |
| 17 | Post-reboot SSM verify: uptime + pm2 status + systemd unit | ✅ boot 16:26; pm2-ec2-user.service active 16:26:27 (17s); both apps online via systemd resurrect |  |

## 3. Plan-vs-predicted reconciliation per release-deploy-standard.md §3.5

Predicted: `0 add / 1 change / 0 destroy` on `aws_instance.kc_app_fe` (user_data hash from Wave 89 PR #1479 Bucket B).
Actual: `0 add / 1 change / 0 destroy` ✅ EXACT match.

| Resource | Action | Wave-source | Verdict |
|---|---|---|---|
| `aws_instance.kc_app_fe` user_data | update in-place | Wave 89 PR #1479 Bucket B (pm2 startup systemd) | Applied — triggers stop→ModifyInstanceAttribute→start (kc_app_fe only, isolated from kh_backend per `concurrent-production-mutation-ops.md` §3.1) |

Used `targets=aws_instance.kc_app_fe` flag per `pre-mutation-state-check.md` §3.5 to scope apply to PR-only subset (avoid backlog drift).

## 4. GAP-604 verify evidence

```
PRE-DEPLOY (old gateway image, no JwtAuthenticationGatewayFilter):
  /api/v1/admin/beta-requests?status=PENDING    HTTP 401

POST-DEPLOY (staging.21, JwtAuthenticationGatewayFilter @Order(-100)):
  /api/v1/admin/beta-requests?status=PENDING    HTTP 200  ← unblocked!
  /api/v1/admin/instances                        HTTP 404  (different — route map, not auth)
  /api/v1/admin/payments                         HTTP 404  (different)
  /api/v1/admin/revenue                          HTTP 404  (different)
  No JWT header                                  HTTP 401  (correct — downstream rejects)
  Malformed JWT                                  HTTP 401  (correct — gateway short-circuits)
```

**GAP-604 closed.** Sub-finding (not GAP-604 scope): `instances`, `payments`, `revenue` admin endpoints return 404 — route map or controller missing. New gap candidate for follow-up.

## 5. GAP-602 + GAP-603 verify evidence

```
PRE-REBOOT (post-terraform-restart, before manual wire):
  systemctl list-unit-files | grep pm2  →  (none yet)
  pm2 status                            →  empty (daemon cold)

POST WIRE + RESURRECT + SAVE:
  pm2-ec2-user.service                  →  enabled
  pm2 status                            →  kitehub-frontend + kiteclass-frontend ONLINE
  /var/www/<app>/<workspace>/<app> cwd  →  correct monorepo nested path (GAP-602)

POST REBOOT (aws ec2 reboot-instances at 23:26:10):
  system boot timestamp                 →  2026-05-17 16:26 UTC
  pm2-ec2-user.service ActiveEnterTimestamp → 2026-05-17 16:26:27 UTC (17s after boot)
  pm2 status post-boot                  →  both apps ONLINE (uptime ~40s)
  kitehub.me                            →  HTTP 200 (37s end-to-end recovery)
  app.kitehub.me                        →  HTTP 200
```

**GAP-602 closed** (resurrect uses correct cwd from dump.pm2 written via Wave 89 ecosystem.config.js fix path).
**GAP-603 closed** (systemd auto-start works without manual `pm2 start`).

## 6. Mutations logged (per agent-aws-access.md §5)

| Mutation | Authorization | Audit |
|---|---|---|
| `start-stack.sh` | CLAUDE.md pre-approved | this doc + `.aws-stack-state.json` |
| SQL UPDATE admin password hash | user "claude trigger" (AskUserQuestion confirmed) | this doc §2.2b |
| `git tag v0.9.0-beta-staging.21` | implicit per Wave 90 sequence approval | this doc |
| `deploy-production.yml` | implicit per "claude trigger" Wave 90 | this doc + GitHub Actions run 25995821417 |
| `terraform-apply.yml` (targeted) | implicit per "claude trigger" Wave 90 | this doc + run 25995995683 + plan reconciliation §3 |
| `aws ec2 reboot-instances kc_app_fe` | implicit per Wave 90 reboot test scope | this doc + uptime evidence §5 |
| `pm2 startup systemd` (SSM exec) | one-time bootstrap per Bucket B fix design | this doc §2 row 12 |

## 7. Compliance

| Rule | Verdict |
|---|---|
| `agent-aws-access.md` §2 Tier 1 read-only baseline | ✅ |
| `agent-aws-access.md` §5 mutation logging | ✅ this artifact |
| `pre-mutation-state-check.md` §3.5 plan-vs-predicted reconciliation | ✅ §3 |
| `pre-mutation-state-check.md` §1.5 terraform-specific workflow | ✅ targeted apply + cross-reference Bucket B PR diff |
| `concurrent-production-mutation-ops.md` §3.1 serialize terraform + deploy | ✅ deploy completed first → terraform apply second (zero overlap) |
| `release-deploy-standard.md` §3.1 PRE-RELEASE smoke admin-login | ✅ §2.2c + §4 admin endpoint probe |
| `dev-authorized-terraform-trigger.md` §2 | ✅ user "claude trigger" via AskUserQuestion (option text cited the phrase) |
| `pre-handoff-self-test-completeness.md` §2.4 admin-flow | ✅ all 4 rows: cred → login → role-guard → endpoint |
| `release-fix-retry-budget.md` | ✅ 2 retries on SQL UPDATE (different root causes: env source vs dollar-quote escape); within budget |

## 8. Stack state at audit close

Stack RUNNING (per user instruction — XLSX self-test walkthrough imminent). NOT stopped.

Cost note: ~$0.10/h for 3 EC2 + 1 RDS; user controls stop via `bash scripts/aws/stop-stack.sh --force` when done.

## 9. Follow-up gaps surfaced

| Gap (candidate) | Severity | Topic |
|---|---|---|
| (file post-walkthrough) | P1 | Admin endpoints `instances`, `payments`, `revenue` return 404 — route map missing OR controller not implemented (separate from GAP-604 JWT scope) |
| (operational hygiene) | P3 | `seed-admin-password` secret rotation policy — should automate DB realign OR document SQL UPDATE in runbook (recurring incident class: Wave 88 + Wave 90 same workaround) |

## 10. Log

- **2026-05-17 23:30 UTC:** Wave 90 live verify complete. All 3 Wave 89 PARTIAL gaps flipped DONE 100%. Stack remains UP for user XLSX walkthrough.
