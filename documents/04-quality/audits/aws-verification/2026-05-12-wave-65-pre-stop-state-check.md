---
title: AWS Verification — Wave 65 pre-stop state-check (per pre-mutation-state-check.md v1.1.0)
status: complete
created: 2026-05-12
phase: wave-65-cleanup
wave: 65
gaps: [GAP-473]
---

# AWS Verification — Pre-Stop State-Check

## Scope

Verify current AWS state before user-executed stop ops. Per `pre-mutation-state-check.md` v1.1.0 §3 mandate + `agent-aws-access.md` §4.1 (`stop-*` Tier 3 BANNED for agent — user executes).

Trigger: user request "tắt resource AWS đúng theo rules" post Wave 65 closure to stop billing during idle window.

## Commands run (Tier 1 read-only per `agent-aws-access.md` §2.1)

```bash
aws ec2 describe-instances --region ap-southeast-1 --filters 'Name=tag:Name,Values=kitehub-*'
aws rds describe-db-instances --region ap-southeast-1
gh run list --status in_progress --limit 5  # concurrent-production-mutation-ops.md check
```

## Findings

### Current AWS state (running)

| Resource | ID | Tag Name | State | Charge while running |
|----------|-----|----------|-------|---------------------|
| EC2 | `i-00505094277deda29` | kitehub-kh-backend | running | t3.medium ~$0.0416/hr |
| EC2 | `i-007b72fffc6dcad22` | kitehub-kc-app | running | t3.medium ~$0.0416/hr |
| RDS | `kitehub-postgres` | — | available | db.t3.micro Free Tier eligible |

### Concurrent ops check (per `concurrent-production-mutation-ops.md`)

- 0 active GitHub workflows
- No terraform apply in progress
- No SSM commands running on these EC2
- **Verdict:** SAFE to stop — no concurrent mutations on shared resources

### Known issue — stop-stack.sh stale instance IDs

`scripts/aws/stop-stack.sh` hardcodes OLD EC2 IDs (`i-0b65c3947d36cae61` + `i-07f6de54544162124` — both terminated 2026-05-12 04:11 by Wave 64 AMI bump replacement). Script will fail or no-op on those IDs.

This is the SAME bug class as GAP-482 (hardcoded IDs in deploy-production.yml — fixed PR #1199). Need similar fix for `stop-stack.sh` + `start-stack.sh` → **file follow-up GAP-492**.

## Recommended action (user-executable)

Stop directly via AWS CLI (since stop-stack.sh has stale IDs):

```bash
# Stop both EC2 (60s grace recommended for graceful container shutdown)
AWS_PROFILE=dev-admin aws ec2 stop-instances \
  --region ap-southeast-1 \
  --instance-ids i-00505094277deda29 i-007b72fffc6dcad22

# Stop RDS (Free Tier — optional; instance auto-restarts after 7 days if stopped)
AWS_PROFILE=dev-admin aws rds stop-db-instance \
  --region ap-southeast-1 \
  --db-instance-identifier kitehub-postgres

# Verify post-stop (after ~2 min)
AWS_PROFILE=kite-readonly aws ec2 describe-instances \
  --region ap-southeast-1 \
  --filters 'Name=tag:Name,Values=kitehub-*' \
  --query 'Reservations[].Instances[].[Tags[?Key==`Name`].Value|[0],State.Name]' \
  --output table

AWS_PROFILE=kite-readonly aws rds describe-db-instances \
  --region ap-southeast-1 \
  --query 'DBInstances[].[DBInstanceIdentifier,DBInstanceStatus]' \
  --output table
```

Expected: EC2 → `stopped`, RDS → `stopped`.

## Cost impact

Stopping saves ~$0.10/hr (2 EC2 t3.medium). ALB stays running ($16/mo fixed) — design intent per Wave 43 (DNS reuse). Net Free Tier coverage means actual payable = $0 either way (per Bills tab Estimated grand total: $0.00), but stopping reduces gross usage for future when Free Tier exhausts.

## Pending (this op)

| Action | Owner | Notes |
|--------|-------|-------|
| EC2 stop-instances × 2 | **User** | `aws ec2 stop-instances` per recommendation above |
| RDS stop-db-instance | **User** | Optional — Free Tier covers idle |
| Update stop-stack.sh dynamic lookup | Wave 66 follow-up | GAP-492 (file separately) |

## Next steps

1. User runs stop commands per §Recommended action
2. Verify post-stop state via the verify commands (Tier 1 OK for agent)
3. Re-start before next deploy session via `aws ec2 start-instances ...` (similar pattern; also needs script fix per GAP-492)

## References

- `pre-mutation-state-check.md` v1.1.0 §3 (this audit pattern)
- `agent-aws-access.md` §4.1 (stop-* Tier 3 BANNED for agent)
- `concurrent-production-mutation-ops.md` v1.0.0 (concurrent check)
- `scripts/aws/stop-stack.sh` (needs ID update — GAP-492 candidate)
- Wave 43 cost-saving design (ALB stays running)
