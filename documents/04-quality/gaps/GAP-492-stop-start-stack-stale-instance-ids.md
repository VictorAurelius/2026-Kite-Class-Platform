# GAP-492: stop-stack.sh + start-stack.sh hardcoded stale EC2 instance IDs

**Status:** 🔵 OPEN
**Priority:** 🟠 P1 (every EC2 replacement makes scripts no-op — same bug class as GAP-482 fixed for deploy-production.yml)
**Domain:** DevOps
**Found:** 2026-05-12 (Wave 65 pre-stop audit)
**Affects:** `scripts/aws/stop-stack.sh`, `scripts/aws/start-stack.sh` (both have same pattern)

## Problem

`scripts/aws/stop-stack.sh` lines 19-20 hardcode EC2 IDs:
```bash
#   EC2: i-0b65c3947d36cae61 (kitehub-kh-backend)
#        i-07f6de54544162124 (kitehub-kc-app)
```

These IDs are STALE — both terminated 2026-05-12 04:11 by Wave 64 AMI bump replacement. Current IDs: `i-00505094277deda29` + `i-007b72fffc6dcad22`.

`scripts/aws/start-stack.sh` likely has same pattern (Wave 61 GAP-473 sister script).

Same bug class as GAP-482 (fixed for deploy-production.yml via dynamic tag lookup PR #1199). Every future EC2 replacement breaks these scripts.

## Proposed Fix

Replace hardcoded IDs with dynamic tag lookup pattern (per GAP-482 fix in deploy-production.yml):

```bash
# Lookup current instances by tag
INSTANCES=$(aws ec2 describe-instances \
  --region "$AWS_REGION" \
  --filters \
    "Name=tag:Name,Values=kitehub-kh-backend,kitehub-kc-app" \
    "Name=instance-state-name,Values=running,stopped" \
  --query 'Reservations[].Instances[].InstanceId' \
  --output text)
```

Update both `stop-stack.sh` + `start-stack.sh`.

## Acceptance Criteria

- [ ] `stop-stack.sh` uses dynamic tag lookup (no hardcoded IDs)
- [ ] `start-stack.sh` uses dynamic tag lookup (no hardcoded IDs)
- [ ] `--dry-run` validates lookup output before applying
- [ ] Header comments removed/updated (no hardcoded ID references)
- [ ] Both scripts survive next EC2 replacement without manual edit

## Related

- **Same bug class:** GAP-482 (deploy-production.yml hardcoded ID — fixed PR #1199)
- **Parent:** Wave 61 GAP-473 (start/stop scripts shipped)
- **Surfaced by:** Wave 65 pre-stop audit 2026-05-12

## Log

- **2026-05-12:** Filed during Wave 65 pre-stop audit. Same fix pattern as GAP-482; ~30min terraform-free script edit.
