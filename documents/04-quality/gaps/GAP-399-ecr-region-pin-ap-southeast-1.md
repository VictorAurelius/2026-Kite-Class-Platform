# GAP-399: ECR Region Pin `us-east-1` → `ap-southeast-1`

**Status:** 🔵 OPEN
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

- [ ] All `AWS_REGION` references = `ap-southeast-1`
- [ ] ECR repo names follow `kite/<service>` convention (consistent với Docker naming convention CLAUDE.md)
- [ ] Workflow YAML validated
- [ ] First successful push verified ECR Singapore via `aws ecr describe-images --region ap-southeast-1`

## Related

- ADR-025 AWS Singapore Free Tier
- GAP-395 (Terraform creates ECR repos in ap-southeast-1)
- GAP-398 (parent: build all images)
