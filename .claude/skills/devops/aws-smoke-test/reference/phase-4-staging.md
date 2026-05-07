# Phase 4 Staging Environment — Smoke Test Checklist

Verifies the staging environment provisioned by `infrastructure/terraform-aws/staging.tf`.

**Status:** Phase 4 staging may be partial — verify whichever resources have been applied.

---

## Pre-flight

```bash
aws sts get-caller-identity --query 'Account' --output text  # 906286017800
```

---

## Resources to verify (subset of Phase 2.3 with staging tags)

```bash
# EC2 staging instances
aws ec2 describe-instances \
  --filters "Name=tag:Environment,Values=staging" "Name=instance-state-name,Values=running,stopped" \
  --query 'Reservations[].Instances[].[InstanceId,InstanceType,State.Name,Tags[?Key==`Name`].Value|[0]]' \
  --output table

# Staging ALB (if separate from prod)
aws elbv2 describe-load-balancers \
  --query 'LoadBalancers[?contains(LoadBalancerName,`staging`)].[LoadBalancerName,DNSName,State.Code]' \
  --output table

# Staging RDS (if separate)
aws rds describe-db-instances \
  --query 'DBInstances[?contains(DBInstanceIdentifier,`staging`)].[DBInstanceIdentifier,DBInstanceStatus]' \
  --output table

# Staging secrets
aws secretsmanager list-secrets \
  --query 'SecretList[?starts_with(Name,`kitehub/staging/`)].[Name,LastChangedDate]' \
  --output table

# Staging-specific endpoint probe
curl -sI -m 10 https://staging.kitehub.example/ | head -1 || echo "staging DNS not yet configured"
```

---

## Pass criteria

Phase 4 status:
- [ ] If staging EC2 deployed → instances `running`
- [ ] If staging ALB deployed → state `active`, returns valid HTTP code
- [ ] If staging DB separate → `available`
- [ ] Staging secrets metadata exists (NEVER read values)
- [ ] Staging tagged distinctly from production (`Environment=staging`)

---

## Tier 1 only

Same restrictions as Phase 2.3. NO `secretsmanager get-secret-value` even on staging — staging secrets can also embed live API keys.

---

## When Phase 4 is partial

If staging not yet fully provisioned:
1. Document what's MISSING in audit artifact §Findings
2. File follow-up gap noting the staging-parity gap
3. Reference `release-deploy-standard.md` §3.4 staging-environment-parity requirement
