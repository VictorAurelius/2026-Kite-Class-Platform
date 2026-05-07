# Phase 2.3 Production Infrastructure — Smoke Test Checklist

Verifies the ~71 resources created by `infrastructure/terraform-aws/` Phase 2.3 apply.
**Automation:** `scripts/smoke-aws-phase-2-3.sh` (run from repo root).

**Reference:** `documents/04-quality/audits/aws-verification/2026-05-08-phase-2-3-post-apply.md` (first artifact).

---

## Pre-flight

```bash
# 1. Confirm AWS account + region
aws sts get-caller-identity --query 'Account' --output text
# Expected: 906286017800

aws configure get region
# Expected: ap-southeast-1 (or override with --region ap-southeast-1 in commands)
```

If account ≠ 906286017800 → ABORT. Wrong-account command may incur cross-tenant cost / data leak risk.

---

## Section A — VPC + Networking (10 resources)

```bash
aws ec2 describe-vpcs \
  --filters "Name=tag:Name,Values=kitehub-vpc" \
  --query 'Vpcs[].[VpcId,CidrBlock,State]' --output table

aws ec2 describe-subnets \
  --filters "Name=vpc-id,Values=<VPC_ID>" \
  --query 'Subnets[].[SubnetId,CidrBlock,AvailabilityZone,MapPublicIpOnLaunch]' --output table

aws ec2 describe-internet-gateways \
  --filters "Name=attachment.vpc-id,Values=<VPC_ID>" \
  --query 'InternetGateways[].InternetGatewayId' --output text
```

Pass criteria:
- 1 VPC `10.0.0.0/16` state `available`
- 4 subnets (2 public with `MapPublicIpOnLaunch=true`, 2 private)
- 1 IGW attached

---

## Section B — EC2 + ALB (10 resources)

```bash
aws ec2 describe-instances \
  --filters "Name=tag:Project,Values=kitehub" "Name=instance-state-name,Values=running" \
  --query 'Reservations[].Instances[].[InstanceId,InstanceType,PublicIpAddress,Tags[?Key==`Name`].Value|[0]]' \
  --output table

aws elbv2 describe-load-balancers \
  --names kitehub-alb \
  --query 'LoadBalancers[].[DNSName,State.Code,Scheme]' --output table

aws elbv2 describe-target-groups \
  --query 'TargetGroups[?contains(LoadBalancerArns[0],`kitehub-alb`)].[TargetGroupName,Protocol,Port,HealthCheckPath]' \
  --output table

aws elbv2 describe-target-health \
  --target-group-arn <TG_ARN_KH> \
  --query 'TargetHealthDescriptions[].[Target.Id,TargetHealth.State,TargetHealth.Reason]' --output table
```

Pass criteria:
- 2 EC2 t3.micro running (KH + KC)
- 1 ALB state `active`, scheme `internet-facing`
- 2 target groups with health check path
- Target health: `unhealthy` ACCEPTABLE if Phase 3 not run yet (no app deployed); `healthy` AFTER Phase 3

---

## Section C — RDS (2 resources)

```bash
aws rds describe-db-instances \
  --db-instance-identifier kitehub-postgres \
  --query 'DBInstances[].[DBInstanceIdentifier,DBInstanceStatus,Engine,EngineVersion,DBInstanceClass,PubliclyAccessible,MultiAZ]' \
  --output table

aws rds describe-db-subnet-groups \
  --db-subnet-group-name kitehub-db-subnet-group \
  --query 'DBSubnetGroups[].[DBSubnetGroupName,SubnetGroupStatus]' --output table
```

Pass criteria:
- Status `available`
- Engine `postgres`, version `15.x`
- `PubliclyAccessible=false` (defense-in-depth)
- `MultiAZ=false` (Free Tier)

---

## Section D — ECR (10 repos)

```bash
aws ecr describe-repositories \
  --query 'repositories[?starts_with(repositoryName,`kite/`)].[repositoryName,createdAt]' \
  --output table

# Per-repo image count (expect 0 until Phase 3)
for repo in kiteclass-core kiteclass-frontend kiteclass-gateway \
            kitehub-admin kitehub-branding kitehub-email kitehub-frontend \
            kitehub-gateway kitehub-platform kitehub-subscription; do
  count=$(aws ecr list-images --repository-name "kite/$repo" --query 'length(imageIds)' --output text 2>/dev/null || echo "?")
  echo "kite/$repo: $count images"
done
```

Pass criteria:
- 10 repos exist
- Empty (`0 images`) ACCEPTABLE pre-Phase-3; ≥1 image AFTER first `v0.9.0-staging.*` tag

---

## Section E — Secrets Manager (8 secrets — METADATA ONLY)

```bash
aws secretsmanager list-secrets \
  --query 'SecretList[?starts_with(Name,`kitehub/production/`)].[Name,LastChangedDate]' \
  --output table

# Per-secret metadata (NEVER get-secret-value — that's Tier 2)
for secret in rds-password jwt-secret encryption-key \
              ai-openai-api-key ai-anthropic-api-key \
              cloudflare-api-token rabbitmq-default-creds ses-smtp-credentials; do
  aws secretsmanager describe-secret \
    --secret-id "kitehub/production/$secret" \
    --query '[Name,VersionIdsToStages!=null]' --output text
done
```

Pass criteria:
- 8 secrets exist
- 3 auto-populated (rds-password, jwt-secret, encryption-key) have version stage `AWSCURRENT`
- 5 placeholder (ai-openai, ai-anthropic, cloudflare, rabbitmq, ses) — version stage status documented in audit artifact

⚠️ NEVER run `aws secretsmanager get-secret-value` from this skill — Tier 2 requires user confirm per `agent-aws-access.md` §3.

---

## Section F — S3 + DynamoDB (state backend cross-check)

```bash
aws s3api head-bucket --bucket kitehub-terraform-state-906286017800
# Exit 0 = exists + accessible

aws s3api head-bucket --bucket kitehub-cloudtrail-logs-906286017800
aws s3api head-bucket --bucket kitehub-assets-production-906286017800

aws dynamodb describe-table \
  --table-name kitehub-terraform-locks \
  --query 'Table.[TableName,TableStatus,BillingModeSummary.BillingMode]' --output table
```

Pass criteria:
- All 3 buckets accessible (exit 0)
- DynamoDB lock table `ACTIVE`, `PAY_PER_REQUEST`

---

## Section G — CloudTrail (audit baseline)

```bash
aws cloudtrail describe-trails \
  --query 'trailList[?Name==`kitehub-main`].[Name,IsMultiRegionTrail,LogFileValidationEnabled]' \
  --output table

aws cloudtrail get-trail-status \
  --name kitehub-main \
  --query '[IsLogging,LatestDeliveryTime]' --output text
```

Pass criteria:
- Trail exists, multi-region `true`, log validation `true`
- `IsLogging = True`
- `LatestDeliveryTime` < 1 hour ago (active delivery)

---

## Section H — Endpoint accessibility (HEAD probes only)

```bash
# Vercel frontends (Stream A)
curl -sI -m 10 https://kitehub.vercel.app/ | head -1
curl -sI -m 10 https://kiteclass.vercel.app/ | head -1

# AWS ALB (Stream B)
curl -sI -m 10 http://<ALB_DNS>/ | head -1

# EC2 direct (should TIMEOUT — SG blocks)
timeout 12 curl -sI http://<EC2_KH_IP>:8080/ 2>&1 | head -1 || echo "timeout (SG blocks — expected)"
```

Pass criteria:
- Vercel: HTTP 200 ✅
- ALB: HTTP 200 (Phase 3 done) OR HTTP 502/503 (Phase 3 pending — app not deployed yet)
- EC2 direct: timeout (defense-in-depth working)

---

## Findings → Gap pipeline

If verification surfaces issues that need fixing:
1. Document in audit artifact §Findings (use `reference/audit-artifact-template.md`)
2. File follow-up gap per `audit-to-gap-pipeline.md` Step 3
3. Update gap §Related to cite the audit artifact path
4. Severity guide: P0 = production blocker, P1 = should fix this wave, P2 = next wave
