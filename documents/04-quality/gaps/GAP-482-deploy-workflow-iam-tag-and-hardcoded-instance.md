# GAP-482: Deploy workflow blocked by IAM tag mismatch + hardcoded EC2 ID

**Status:** 🟢 DONE 2026-05-13 — E2E deploy-production verified via run 25776387051 on tag v0.9.0-beta-staging.11. Containers all started; `https://api.kitehub.me/actuator/health` HTTP 200; ALB target `i-05d7af46d01436b96` healthy. Workflow gate reported false-failure (timeout poll-Status-vs-actual-SSM-Success divergence — tracked GAP-498). All 7 cascade bugs functionally closed.
**Priority:** 🔴 P0 BLOCKING (Wave 64 Step F deploy-production fails)
**Domain:** DevOps / Infrastructure
**Found:** 2026-05-12 (Wave 64 Step F deploy-production.yml run 25713149664 failed)
**Affects:** All deploys via `deploy-production.yml` workflow

## Problem

Wave 64 Step F (deploy-production workflow_dispatch for v0.9.0-beta-staging.9) failed with:

```
aws: [ERROR]: An error occurred (AccessDeniedException) when calling the SendCommand operation:
User: arn:aws:sts::906286017800:assumed-role/kitehub-github-deploy/GitHubActions is not authorized
to perform: ssm:SendCommand on resource: arn:aws:ec2:ap-southeast-1:906286017800:instance/i-0b65c3947d36cae61
```

State-check findings:

### Bug 1 — IAM tag value mismatch

`infrastructure/terraform-aws/iam.tf` line ~239 (kitehub-github-deploy-inline policy):
```hcl
Resource = "*"
Condition = {
  StringEquals = {
    "aws:ResourceTag/Project" = var.project_name   # → "kitehub" lowercase
  }
}
```

`infrastructure/terraform-aws/main.tf` provider default_tags:
```hcl
default_tags {
  tags = {
    Project = "Kite"   # capitalized — actual tag on all resources
  }
}
```

Actual EC2 tag (verified via describe-instances):
```json
{ "Key": "Project", "Value": "Kite" }
```

→ Condition `aws:ResourceTag/Project = "kitehub"` NEVER matches actual `"Kite"` → all SSM ops to EC2 instances denied.

`scheduler.tf` correctly hardcodes `"Kite"` in same pattern — so EventBridge schedulers work. Only IAM role conditions reference the wrong `var.project_name` value.

Affected roles (all with same bug):
- `kitehub-github-deploy` (this incident)
- `kitehub-github-rollback` (Wave 63 — would fail rollback exec)
- `kitehub-rollback-role` (also Wave 63 alternate name)

### Bug 2 — Hardcoded stale EC2 instance ID

`.github/workflows/deploy-production.yml`:
```yaml
env:
  DEPLOY_INSTANCE_ID_KH: i-0b65c3947d36cae61   # kh-backend EC2 (Phase 2.3 output)
```

This is the OLD instance, terminated 2026-05-12 04:11 when terraform apply replaced EC2 due to AMI bump. New ID: `i-00505094277deda29`.

Hardcoded values break on every EC2 replacement (AMI bump, instance class change, etc.). Should be dynamic lookup via tag.

## Proposed Fix

### Fix 1 — Correct IAM tag condition

Edit `infrastructure/terraform-aws/iam.tf`:
- Change `"aws:ResourceTag/Project" = var.project_name` → `"aws:ResourceTag/Project" = "Kite"` for all 3 SSM SendCommand conditions
- Alternative: change `default_tags.tags.Project` in main.tf from `"Kite"` to `var.project_name` (= "kitehub") — would change every existing resource tag, requires fleet-wide tag update

→ Pick: change iam.tf (3 lines edit, no resource tag churn)

### Fix 2 — Dynamic EC2 lookup in deploy workflow

Edit `.github/workflows/deploy-production.yml`:
- Replace hardcoded `DEPLOY_INSTANCE_ID_KH` env var
- Add step before "Send deploy command via SSM":
  ```yaml
  - name: Lookup current kh-backend instance ID
    id: ec2_lookup
    run: |
      INSTANCE_ID=$(aws ec2 describe-instances \
        --region "${AWS_REGION}" \
        --filters \
          "Name=tag:Name,Values=kitehub-kh-backend" \
          "Name=instance-state-name,Values=running" \
        --query "Reservations[0].Instances[0].InstanceId" \
        --output text)
      if [ -z "$INSTANCE_ID" ] || [ "$INSTANCE_ID" = "None" ]; then
        echo "::error ::No running kitehub-kh-backend instance found"; exit 1
      fi
      echo "instance_id=$INSTANCE_ID" >> "$GITHUB_OUTPUT"
  ```
- Reference: `${{ steps.ec2_lookup.outputs.instance_id }}` in subsequent SSM steps

### Apply order

1. Ship fixes in 1 PR
2. terraform apply (IAM-only this time, no EC2 replace) via workflow_dispatch
3. Re-trigger deploy-production.yml v0.9.0-beta-staging.9

## Acceptance Criteria

- [ ] `iam.tf` 3 SSM SendCommand conditions use `"Kite"` literal matching default_tags
- [ ] `deploy-production.yml` dynamically looks up instance ID by tag (no hardcoded ID)
- [ ] `rollback.yml` (Wave 63) — verify same fix applied if has hardcoded IDs
- [ ] terraform plan shows ONLY IAM policy update (no EC2 churn)
- [ ] terraform apply succeeds
- [ ] deploy-production.yml succeeds on v0.9.0-beta-staging.9
- [ ] kh-backend target group reports `healthy` after deploy

## Related

- **Surfaced by:** Wave 64 Step F deploy-production run 25713149664
- **Sibling:** GAP-481 (gateway path routing 404) — may be related once deploy works
- **Reference rules:**
  - `pre-mutation-state-check.md` v1.0.0 — applied this investigation
  - `release-fix-retry-budget.md` §3 — retry #1, root-cause fix not patch
  - `terraform-apply-retry-reconfirm.md` — apply after fix
- **Blocks:** Wave 64 closure + Release 1 invite

## Log

- **2026-05-13 (🟢 DONE — functional E2E verified)**: Deploy-production run [25776387051](https://github.com/VictorAurelius/2026-Kite-Class-Platform/actions/runs/25776387051) for tag `v0.9.0-beta-staging.11` (post PR #1235 ECR lifecycle + sha-conditional fixes). Outcome:
  - ✅ All 5 containers (kitehub-admin/branding/email/gateway/subscription) `Started` with image `0.9.0-beta-staging.11`
  - ✅ `curl https://api.kitehub.me/actuator/health` → HTTP 200 (0.75s)
  - ✅ ALB target `i-05d7af46d01436b96` → `healthy`
  - ✅ SSM command `36ce3eae-8848-4cfd-b5f0-87cb347aced3` → Status=Success (verified via `aws ssm get-command-invocation`)
  - ⚠️ Workflow gate reported `failure` due to false-positive 8-min poll timeout — workflow's IAM context saw stale `InProgress` for 48 attempts despite SSM actual Success. Tracked separately as **GAP-498** (workflow poll redesign — poll ALB target health + curl smoke directly instead of SSM Status field).
  - GAP-498 is non-blocking for invite path — deploy IS functional. Closure based on `gap-done-discipline.md` §2 verification artifact pointer = ALB 200 + target healthy evidence.
- **2026-05-12** (Wave 66 Bucket Z — status sync per `post-merge-sync-completeness.md` Rule 17): Status frontmatter was stale "🔵 OPEN" while file §Log showed 4 of 7 cascade bugs FIXED (PR #1199 + #1200) + dependencies cleared (GAP-484 OTel #1209, GAP-483 user_data DONE). Flipped to 🟡 PARTIAL 95%. Remaining 5% = E2E deploy-production.yml run with green ALB target — gated on Wave 66 Bucket A merge (GAP-493 Path B preflight). Will flip 🟢 DONE when user triggers `deploy-production.yml workflow_dispatch dry_run=false` post-Bucket-A merge and target reports healthy.
- **2026-05-12:** Filed Wave 64 Step F deploy fail investigation. 2 bugs in 1 gap (architectural fix needed in same PR for retry budget discipline).
- **2026-05-12 (post-PR #1199 + #1200 cascade discovery):** Wave 64 Step F deploy surfaced 7 cascading bugs in single session — retry budget per `release-fix-retry-budget.md` §3 exceeded at retry #4. Pivot to file separate gaps + close session.

  Bugs discovered (status):
  1. ✅ FIXED (PR #1199) — IAM tag mismatch `var.project_name` (kitehub) vs `default_tags` (Kite)
  2. ✅ FIXED (PR #1199) — Hardcoded EC2 instance ID DEPLOY_INSTANCE_ID_KH
  3. ✅ FIXED (PR #1200) — ec2:DescribeInstances missing for dynamic lookup
  4. ✅ FIXED (PR #1200) — Secret prefix mismatch `kite/prod/*` vs `kitehub/production/*`
  5. ⚠️ PARTIAL (manual SSM bootstrap) — EC2 user_data doesn't clone repo; new instances lack `/opt/kite-prod`. Manual SSM RunCommand ran git clone + checkout tag. NOT in terraform code → future EC2 replacements will hit same issue. → **File new GAP for user_data clone bootstrap**
  6. ⚠️ PARTIAL (manual install) — EC2 user_data doesn't install `git`. Same future-replacement issue. → **Roll into GAP-483 with #5**
  7. ❌ BLOCKING (this iteration ended) — Java services (5x kitehub-*) crash on startup: `OtlpHttpSpanExporter: Invalid endpoint, must start with http:// or https://`. OpenTelemetry tracing autoconfig requires `MANAGEMENT_OTLP_TRACING_ENDPOINT` or `OTEL_EXPORTER_OTLP_ENDPOINT` env var with valid URL. Adding `MANAGEMENT_TRACING_ENABLED=false + OTEL_SDK_DISABLED=true` to `/etc/kite/.env` did NOT prevent autoconfig from running. Need code-level fix: exclude autoconfig OR set valid endpoint default (even non-listening localhost) OR proper Spring property. → **File new GAP for OTel config**

  Cutover state at session end:
  - ✅ ACM cert imported (cert ARN e0adcd76-...)
  - ✅ ALB HTTPS:443 listener live
  - ✅ HTTP:80 redirects to HTTPS
  - ✅ CF SSL `full strict` + Always HTTPS `on`
  - ✅ api.kitehub.me proxied=true through CF
  - ✅ DNS records 6 records (SES verify + 3 DKIM + DMARC + SPF merge)
  - ✅ Docker images pushed v0.9.0-beta-staging.9 (10 services)
  - ✅ deploy-prod.sh runs successfully (containers start)
  - ❌ Java services crash on startup (OTel config)
  - ⏳ SES production access form (user action — separate gate, can submit while OTel fix shipped)
  - ⏳ kc-app instance EMPTY (deferred Phase 7 per ec2.tf design)
  - ⏳ Production seed run (gated by Java services healthy first)

  Next session pickup:
  1. File GAP-483 (EC2 user_data bootstrap missing git + repo clone + tag checkout)
  2. File GAP-484 (Java services OTel autoconfig crash on missing endpoint)
  3. Fix OTel in code (likely `application.yml` set valid default endpoint or exclude autoconfig)
  4. Re-trigger deploy
  5. Verify health → continue cutover
