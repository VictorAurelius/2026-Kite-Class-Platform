---
title: AWS Verification — Wave aws-restore-1 closure post-apply
status: complete
created: 2026-05-26
audience: dev
audit_type: aws-verification
scope: Wave aws-restore-1 closure verification — production stack restore + ALB elimination + cascade unblock readiness
wave: aws-restore-1
gaps: [GAP-612, GAP-693, GAP-717]
---

# AWS Verification — Wave aws-restore-1 closure

## Scope

Post-apply verification of Wave aws-restore-1 (Phase A→B→C→D coordinator-inline ~3.5h). Wave SHIPPED 2026-05-26.

## Commands run (Tier 1 read-only per `agent-aws-access.md` §2.1)

```bash
aws ec2 describe-instances --query 'Reservations[].Instances[].[State.Name,Tags[?Key==\`Name\`].Value|[0]]'
aws rds describe-db-instances --db-instance-identifier kitehub-postgres --query 'DBInstances[0].DBInstanceStatus'
aws elbv2 describe-load-balancers --query 'LoadBalancers[]'  # expect empty
aws ec2 describe-security-groups --group-ids sg-0b9d8e37f2bf977b7 sg-02dfda0973b34a130
aws secretsmanager list-secrets --query 'SecretList[?starts_with(Name,\`kitehub/production/\`)]'
aws cloudtrail get-trail-status --name kitehub-main
aws ssm send-command --instance-ids i-05cfda7c6c60b683f (nginx + git status, curl localhost)
curl -sI https://api.kitehub.me/actuator/health  # from outside, end-to-end
curl -sI https://kitehub.me/  # apex preserved check
curl -s https://api.kitehub.me/actuator/health  # full health JSON
```

## Findings

### Account + observability
- ✅ Account `906286017800` ACTIVE (`get-caller-identity` OK)
- ✅ CloudTrail `kitehub-main` IsLogging=True throughout

### Compute layer (Phase A — DONE)
- ✅ EC2 3/3 running:
  - `i-05d7af46d01436b96` kitehub-kh-backend (10.0.0.129, public 52.221.222.38)
  - `i-01ad56b0067d0213b` kitehub-kc-app (10.0.0.155, public 52.77.242.87)
  - `i-05cfda7c6c60b683f` kitehub-kc-app-fe (10.0.0.84, EIP `52.221.161.175`)
- ✅ SSM Online 3/3 (PingStatus Online)
- ✅ kh_backend Docker stack 7/7 HEALTHY (gateway, admin, branding, email, subscription, rabbitmq, redis)
- ✅ kc_app_fe PM2 2/2 online (kitehub-frontend + kiteclass-frontend 117min uptime post-restart)
- ✅ kc_app_fe nginx active + serving apex + new api vhost

### Data layer (Phase B — DONE)
- ✅ RDS `kitehub-postgres` DBInstanceStatus=`available`
- ✅ Engine: postgres 15.17 (matches snapshot)
- ✅ Endpoint: `kitehub-postgres.c3awuqw4ugex.ap-southeast-1.rds.amazonaws.com:5432`
- ✅ Restored from manual snapshot `final-kitehub-postgresa9068e7e-9e0c-4c36-973b-d6f7800c3af3` (2026-05-21) — production data preserved
- ✅ CloudTrail evidence: `RestoreDBInstanceFromDBSnapshot` + `ModifyDBInstance` events via OIDC `github-terraform-apply-26432539267` (Phase B run)
- ✅ kh_backend Spring services connect to RDS — `/actuator/health` shows db UP details PostgreSQL validationQuery `isValid()`

### Ingress layer (Phase C — DONE — ALB ELIMINATED)
- ✅ ALB: 0 load balancers (`describe-load-balancers` empty)
- ✅ ALB SG `sg-02dfda0973b34a130`: `InvalidGroup.NotFound` (terraform destroy succeeded retry #1)
- ✅ ec2_app SG `sg-0b9d8e37f2bf977b7`: 3 ingress rules from kc_app_fe SG `sg-06ecdb1c30c2fecd1` on ports 80/443/8080 (architecture pivot complete)
- ✅ kc_app_fe nginx config: 17832 bytes (extended +2382 bytes with api.kitehub.me vhost — git HEAD `f989f10b`)
- ✅ `nginx -t` OK + `systemctl is-active nginx` active
- ✅ CF DNS `api.kitehub.me`: CNAME → `kitehub.me`, proxied=true, ID `11c30707e2cb9bc4f8104a22188865cc`
- ✅ Orphan ALB CNAME deleted (was `kitehub-alb-224105328.ap-southeast-1.elb.amazonaws.com`)
- ✅ Cost outcome: ~$20-25/mo ALB cost ELIMINATED PERMANENTLY (var.enable_alb default `true` → `false`)

### Secrets layer (terraform import — DONE)
- ✅ `aws_secretsmanager_secret.jwt_challenge` imported to state (binds existing `kitehub/production/jwt-challenge-secret` from Wave 81 manual creation)
- ✅ `aws_secretsmanager_secret.resend_api_key` imported (binds existing `kitehub/production/resend-api-key` from Wave 71b GAP-513)
- ✅ Both `aws_secretsmanager_secret_version` resources got `lifecycle.ignore_changes = [secret_string]` — Wave 81 + Wave 71b production values preserved (not overwritten by random_password placeholders)

### Live smoke (Phase D — DONE)
- ✅ `https://api.kitehub.me/actuator/health` → **HTTP/2 200**
  - Server: `cloudflare` (CF edge proxy)
  - Content-Type: `application/vnd.spring-boot.actuator.v3+json`
  - Components: `db:UP (PostgreSQL)`, `redis:UP (7.4.9)`, `diskSpace:UP (17983741952 bytes free)`, `ping:UP`, `ssl:UP`, `refreshScope:UP`
- ✅ `https://kitehub.me/` → HTTP/2 200 (Server: nginx/1.28.3 — apex preserved)
- ✅ Other endpoints: `/actuator/info` 200, `/` 404 (no root route — expected), `/api/v1/admin/health` 401 (auth chain working — NOT 500)

### Chain validation

CF edge → kc_app_fe EIP `52.221.161.175` → nginx vhost `api.kitehub.me` (server block 3.5) → upstream `kh_backend_gateway` (`10.0.0.129:8080`) → Spring `/actuator/health` → 200 UP ✅

## Issues + retries

**Phase C2 retry #1 needed** (per `release-fix-retry-budget.md` §3):
- First apply (run 26433936662) failed after 15min `DependencyViolation`: terraform tried `aws_security_group.alb` DELETE in parallel với `aws_security_group.ec2_app` UPDATE. ec2_app SG real-AWS-state still referenced ALB SG (10 ingress rules) — terraform's natural dep graph missed this because new config has NO references (dynamic block evaluates empty when `enable_alb=false`).
- Fix path (user-approved Path 1): manually `aws ec2 revoke-security-group-ingress` 10 orphan rules from ec2_app SG → re-trigger terraform-apply → succeeded.
- Retry budget: 1 retry (within §2 limit; root cause understood; same-day fix).
- Lesson candidate: terraform dynamic ingress blocks với `var.enable_alb` conditional + AWS dependency graph need explicit `depends_on` OR refactor to `aws_vpc_security_group_ingress_rule` resources for proper dep tracking. Defer follow-up gap.

**Phase B pre-flight code gap (caught + fixed in-wave):**
- `rds.tf` lacked `snapshot_identifier` parameter → restore impossible as-is.
- Fix: PR #1852 + #1853 added `var.rds_restore_from_snapshot` + `lifecycle.ignore_changes` + workflow `TF_VAR_*` injection.
- Cascade fix: PR #1853 added missing `TF_VAR_aws_account_id` injection (GAP-692 Phase 1 wiring gap).
- Cascade fix: PR #1855 removed 4 ALB widgets from cloudwatch-dashboard.tf (Invalid index errors when `aws_lb.main` empty tuple).
- Cascade fix: PR #1856 added terraform import blocks for jwt_challenge + resend_api_key secrets (closes GAP-717 live verify).

**Application-layer cascades (out of Wave aws-restore-1 scope):**
- `kitehub-subscription` logs show email-send 500 to kitehub-email — Resend API key + IAM + Resend dashboard verify chain — Wave rst-cascade-1 scope (13 dependent gaps).
- Phase D smoke verified infrastructure layer (CF + nginx + Spring gateway + DB + Redis) — application-layer functional flows defer Wave rst-cascade-1 live walkthroughs.

## Wave-history.jsonl + ROADMAP sync

- ✅ wave-history.jsonl: append entry `tag_primary=aws-restore counter=1` (this PR)
- ✅ ROADMAP §🎯 Current Status Snapshot: add Wave aws-restore-1 SHIPPED 2026-05-26 entry (this PR)
- ✅ Session handoff `2026-05-26-wave-aws-restore-1-shipped-rst-cascade-queued.md` (this PR)

## Recommendations cho Wave rst-cascade-1 (next session priority)

Wave aws-restore-1 unblocks 13 PARTIAL gaps cascade via live walkthrough:

| Gap | % current | Live walkthrough scope | Expected DONE? |
|---|---|---|---|
| GAP-657 | 95 | EmailHardeningTest live render verify | yes if Resend chain healthy |
| GAP-658 | 80 | VN sample seed worker integration verify | partial — paired Bucket B4 i18n needed |
| GAP-659 | 95 | per-tone email variant live send | yes if Resend chain healthy |
| GAP-543 | 95 | 5 email types content/tone live verify | yes if Resend chain healthy |
| GAP-530 | 10 | 5-email-type live verify | needs GAP-533 warm-up Day 5+ user-action |
| GAP-370 | 95 | Resend dashboard verify + terraform apply | needs user-action |
| GAP-608 | 90 | IAM ses:SendEmail live verify | yes (IAM applied Phase B/C2) |
| GAP-684 | 0 | Admin login walk per `pre-handoff-self-test-completeness.md` §2.4 | first live walkthrough scope |
| GAP-508 | 90 | Resend live verify post-restore | yes if Resend chain healthy |
| GAP-514 | 90 | Live 429 smoke gateway rate limit | yes |
| GAP-534 | 80 | Invite token live verify (Flyway V39 + service) | yes if deploy fresh image |
| GAP-538 | 96 | Day-1 onboarding live walkthrough | yes |
| GAP-599 | 85 | Multi-tab JWT sessionStorage live verify | yes |
| GAP-502 | 90 | kh_backend healthy stability verify | yes (3.5h uptime smoke) |

**Sequencing chốt cho next session:**

```
Wave rst-cascade-1 (live walkthroughs 13 cascade gaps) — coordinator-inline OR 3-4 parallel agents
  ↓ parallel với
Wave class-teacher-fix-1 (GAP-727 hasAccessToClass) — separate scope
Wave idempotency-finish-1 (GAP-730 port pattern) — separate scope
  ↓ background
GAP-533 Resend warm-up Day 1-7 user-action (~5-7 ngày)
  ↓
4 hard-blocker waves (security-1 / ops-1 / compliance-1 / perf-1) per Wave audit-stale-sweep-1 recommendation
  ↓
Đợt 108 RST 100%
```

## References

- Wave plan: `documents/03-planning/waves/wave-2026-05-26-aws-restore-1-production-stack-recovery.md`
- Phase C2 pre-apply audit: `documents/04-quality/audits/aws-verification/2026-05-26-wave-aws-restore-1-phase-c2-enable-alb-false-preapply.md`
- PRs shipped: #1852 (Wave plan + RDS snapshot var) + #1853 (TF_VAR_aws_account_id) + #1854 (Phase C ALB elimination) + #1855 (dashboard fix) + #1856 (secrets import)
- Sister gaps closed: GAP-612 DONE / GAP-717 DONE / GAP-693 PARTIAL 70% (SOP defer)
- Cross-link: GAP-693 (SOP runbook Wave aws-rebuild-sop-1 follow-up)
- Cross-reference rules: `release-deploy-standard.md` §9 + `dev-authorized-terraform-trigger.md` §2 + `pre-mutation-state-check.md` §3 + `terraform-apply-retry-reconfirm.md` §3 + `concurrent-production-mutation-ops.md` §1 + `aws-observability-first.md` §6 + `agent-aws-access.md` §2.1 Tier 1 + `local-fix-production-parity-check.md` §1 + `gap-done-discipline.md` §2
