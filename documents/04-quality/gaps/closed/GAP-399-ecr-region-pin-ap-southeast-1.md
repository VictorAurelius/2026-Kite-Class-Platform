# GAP-399: ECR Region Pin `us-east-1` → `ap-southeast-1`

**Status:** 🟢 DONE 2026-05-12 (Wave 66 Bucket Z — region pin verified clean; zero `us-east-1` config refs)
**Priority:** 🔴 P0 v0.9.0-beta
**Domain:** Docker / AWS region
**Found:** 2026-05-07 (Wave 37 audit — `docker-build-push.yml` line 43)
**Affects:** ECR cross-region pull latency + cost

## Problem

`.github/workflows/docker-build-push.yml` hardcode `AWS_REGION: us-east-1` + `ECR_REPOSITORY_*: kiteclass/...` ngược với ADR-025 ACCEPTED Phase 1 BETA = AWS Singapore (`ap-southeast-1`).

**Impact:**
- Cross-region pull EC2 ap-southeast-1 → ECR us-east-1 = ~200ms latency + data transfer cost ($0.02/GB)
- Bootstrap deploy slow (image pull 10-30s vs <5s same-region)
- Architecture B cost projection assumes same-region (free transfer)

## Proposed Fix

```yaml
# .github/workflows/docker-build-push.yml
env:
  AWS_REGION: ap-southeast-1
  ECR_REPOSITORY_KH_SUBSCRIPTION: kite/kitehub-subscription
  ECR_REPOSITORY_KH_BRANDING: kite/kitehub-branding
  # ... + 9 more
```

Update IAM OIDC role region trust policy. Bootstrap region-specific ECR repos via Terraform (GAP-395).

## Acceptance Criteria

- [x] All `AWS_REGION` references = `ap-southeast-1` (env block line 41)
- [x] ECR repo names follow `kite/<service>` convention (matches Bucket A Terraform naming)
- [x] Workflow YAML validated (`python3 -c "import yaml; yaml.safe_load(...)"` → OK)
- [x] First successful push verified ECR Singapore — Wave 64 cutover pushed v0.9.0-beta-staging.9 across 10 services to `ap-southeast-1` ECR; `aws ecr describe-repositories --region ap-southeast-1` confirms 10 `kite/<svc>` repos live

## Log

- **2026-05-12** (Wave 66 Bucket Z — flip 🟢 DONE): State-check per `gap-done-discipline.md` §2:
  - `grep -l "us-east-1" .github/workflows/*.yml infrastructure/terraform-aws/*.tf` → 1 file (`cloudtrail.tf`); both matches (lines 9 + 130) are in COMMENTS only, zero actual config references
  - `aws ecr describe-repositories --region ap-southeast-1` → 10 `kite/<svc>` repos live in Singapore
  - Wave 64 v0.9.0-beta-staging.9 push verified ECR Singapore live (per GAP-398 §Log).
- **2026-05-07** (Wave 37 Bucket B): `AWS_REGION` flipped `us-east-1` → `ap-southeast-1`; ECR env block expanded to 9 services per `kite/<service>` convention; matrix uses ECR repo per service. Final criterion (live ECR push verification) deferred to post-Bucket-A merge when AWS_ROLE_ARN secret + ECR repos exist.

## Related

- ADR-025 AWS Singapore Free Tier
- GAP-395 (Terraform creates ECR repos in ap-southeast-1)
- GAP-398 (parent: build all images)
