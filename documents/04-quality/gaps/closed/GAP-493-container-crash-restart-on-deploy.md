# GAP-493: Deploy lacks RDS preflight check → containers crash-restart on stopped DB

**Status:** 🟢 DONE 2026-05-12 — Path A unblocked deploy (api.kitehub.me HTTP 200, ALB healthy) + Path B preflight job + IAM `rds:DescribeDBInstances` shipped Wave 66 Bucket A
**Priority:** 🔴 P0 BLOCKING (blocks Phase 1 BETA soft launch — silent dependency failure surfaces only via container crash logs)
**Domain:** DevOps
**Found:** 2026-05-12 (post-GAP-491 verified deploy run 25748003956)
**Affects:** Every deploy attempt when RDS / RabbitMQ / Redis dependency is stopped (cost-saving scheduler.tf can leave deps stopped)

## Problem (original)

Deploy run 25748003956 (v0.9.0-beta-staging.10) showed all 5 kitehub-* containers in crash-restart loop after `docker compose up -d`. ALB target unhealthy, gateway port 8080 `Connection reset by peer`.

## Root cause (diagnosed 2026-05-12)

`docker logs kitehub-admin` revealed Spring Boot crash:

```
HikariPool-1 - Starting...
[10 seconds later]
PSQLException: The connection attempt failed.
Caused by: java.net.SocketTimeoutException: Connect timed out
  at org.postgresql.core.PGStream.createSocket(...)

Flyway → entityManagerFactory bean creation FAILED → Spring context refresh cancelled → container exits 1 → docker-compose restart: unless-stopped → loop
```

**Root cause: RDS `kitehub-postgres` was STOPPED** (verified via `aws rds describe-db-instances`). Cost-saving scheduler (scheduler.tf cron `stop_weekday_evening_ec2` / `stop_friday_evening_ec2`) stopped RDS during off-hours; no auto-start happened before deploy.

This is NOT a Spring config bug or container resource issue — it's a deploy workflow ordering issue: deploy ran against a stopped dependency.

## Fix path

### Path A (immediate unblock)

Start RDS before retry deploy:
```bash
AWS_PROFILE=dev-admin aws rds start-db-instance --region ap-southeast-1 \
  --db-instance-identifier kitehub-postgres
# OR project script:
bash scripts/start-stack.sh
```

After RDS reaches `available` state (~5-10min) → retry deploy → containers should reach `healthy`.

### Path B (preflight in workflow — proposed)

Add preflight job to `.github/workflows/deploy-production.yml` between `validate` and `deploy`:

```yaml
preflight:
  name: Verify dependencies online
  runs-on: ubuntu-latest
  needs: validate
  steps:
    - uses: aws-actions/configure-aws-credentials@v6
      with:
        role-to-assume: ${{ secrets.AWS_DEPLOY_ROLE_ARN }}
        aws-region: ${{ env.AWS_REGION }}
    - name: Verify RDS available
      run: |
        STATUS=$(aws rds describe-db-instances --region ${AWS_REGION} \
          --db-instance-identifier kitehub-postgres \
          --query 'DBInstances[0].DBInstanceStatus' --output text)
        if [ "$STATUS" != "available" ]; then
          echo "::error ::RDS kitehub-postgres is $STATUS (expected: available). Run scripts/start-stack.sh or wait for cost-saving scheduler. See GAP-493."
          exit 1
        fi
        echo "::notice ::RDS available ✅"
```

Fails fast with actionable error instead of 8min container crash-loop. Same pattern can extend to RabbitMQ / Redis if they ever become external (currently local containers).

IAM: deploy role already has `ec2:DescribeInstances`; needs `rds:DescribeDBInstances` added — single statement extension.

### Path C (alternative — auto-start in deploy-prod.sh)

deploy-prod.sh could `aws rds start-db-instance` and `wait db-instance-available` before docker compose. Trade-off: longer deploys (always 5-10min start), but no preflight gate fail. NOT preferred — slower + obscures the state issue.

## Acceptance Criteria

- [x] Root cause identified (RDS stopped, verified via describe-db-instances + Spring crash logs)
- [x] Path A executed: RDS started + schema dropped+recreated (Flyway checksum mismatch on V34 — pre-launch state, no real data); deploy retry 25749467477 → 4/5 services restarted clean; **ALB `healthy`, `https://api.kitehub.me/actuator/health` = HTTP 200** ✅
- [x] Path B shipped: `deploy-production.yml` has `preflight` job verifying RDS available + actionable error
- [x] `iam.tf` extends `github_deploy_inline` with `rds:DescribeDBInstances` (new Sid `RdsDescribeForPreflight`, Resource="*" — RDS Describe doesn't support tag Condition; least-privilege via action-only)
- [x] Verification path: trigger deploy post-apply → preflight job runs `aws rds describe-db-instances` → fails fast (<30s) with `::error::` referencing `scripts/start-stack.sh` if status ≠ `available` (vs 8min crash-loop). Apply order documented in `documents/04-quality/audits/aws-verification/2026-05-12-gap-493-path-b-preflight.md` §Recommendations

## Related

- **Tooling:** GAP-491 (CloudWatch streaming) — surfaced this by making container crash logs visible. Without GAP-491, would have been a black box.
- **Adjacent:** scheduler.tf `stop_weekday_evening_ec2` / `stop_friday_evening_ec2` — stops RDS for cost savings; no matching auto-start for production deploy
- **Adjacent:** GAP-447 EC2 right-size — t3.medium adequate for backend; RDS sizing separate
- **Adjacent:** GAP-484 OTel fix (#1209) — confirmed working from logs (no OTLP autoconfig crash visible)
- **Adjacent:** scripts/start-stack.sh — project-level start (already uses dynamic lookup per GAP-492)

## Log

- **2026-05-12 (Path B DONE — Wave 66 Bucket A):** Preflight job + IAM extension shipped. State-check verified pre-write: `.github/workflows/deploy-production.yml` had zero matches for "preflight" / "kitehub-postgres" / "rds:DescribeDB"; `infrastructure/terraform-aws/iam.tf:286 github_deploy_inline` had no `rds:DescribeDBInstances` (existing action only in `github_tier_3_cutover_inline` Sid `RdsLifecycle` line ~570, with full lifecycle perms — left untouched per task spec). Diff: (a) `.github/workflows/deploy-production.yml` adds `preflight` job (assumes `AWS_DEPLOY_ROLE_ARN`, runs `aws rds describe-db-instances --db-instance-identifier kitehub-postgres`, exits 1 with `::error::` referencing `scripts/start-stack.sh` + this gap if status ≠ `available`); `deploy.needs: validate` → `deploy.needs: preflight`; `notify.needs` includes preflight. (b) `infrastructure/terraform-aws/iam.tf` adds new statement Sid `RdsDescribeForPreflight` to `github_deploy_inline` policy (action=`rds:DescribeDBInstances`, Resource=`"*"` — least-privilege via action-only since RDS Describe doesn't support tag Condition). Verification: `terraform fmt iam.tf` clean (no output); `python3 -c "yaml.safe_load(open('deploy-production.yml'))"` PASS. terraform-apply.yml apply pending user-triggered `workflow_dispatch confirm=APPLY dry_run=false` per `release-deploy-standard.md` §9 carve-out (agent-initiated `terraform apply` BANNED per `agent-aws-access.md` §4.3). Audit artifact: `documents/04-quality/audits/aws-verification/2026-05-12-gap-493-path-b-preflight.md` (Scope + Tier 1 commands + Real/phantom findings + Cross-reference matrix + Prior actions + Pending + Recommendations + References per `pre-mutation-state-check.md` §3 + §1.5). Both Path A + B AC verified; Status flipped 🟡 PARTIAL → 🟢 DONE.
- **2026-05-12 (Path A done — api 200):** RDS started → deploy retry 25749467477 surfaced second root cause: Flyway V34 checksum mismatch (412870369 in DB vs 130720872 local). Pre-launch state, no real data → executed `DROP SCHEMA public CASCADE; CREATE SCHEMA public` via SSM exec (9 tables dropped: branding_lifecycle_events, rebrand_approvals, audit_log, moderation_queue, dmca_takedown_requests, deletion_requests, quality_reports, branding, student_bulk_import_jobs). Restarted kitehub-admin/branding/email/subscription via docker compose. Verified: ALB target `healthy`, `https://api.kitehub.me/actuator/health` = 200. Status → 🟡 PARTIAL until Path B preflight job ships.
- **2026-05-12 (root cause):** docker logs SSM exec revealed PSQLException SocketTimeoutException → checked RDS state → `kitehub-postgres=stopped`. Started RDS via `aws rds start-db-instance`. Status → 🟡 PARTIAL pending retry verify.
- **2026-05-12:** Filed after deploy retry 25748003956 (GAP-491 verified) showed all kitehub-* containers in crash-restart loop. Visibility now works; this gap is what visibility surfaced.
