# Phase 2.2 OIDC + IAM Roles — Smoke Test Checklist

Verifies the GitHub Actions OIDC provider + 4 deploy roles created by Phase 2.2.

---

## Commands

```bash
# Account verify
aws sts get-caller-identity --query 'Account' --output text  # Expected: 906286017800

# OIDC provider
aws iam list-open-id-connect-providers \
  --query 'OpenIDConnectProviderList[?contains(Arn,`token.actions.githubusercontent.com`)].Arn' \
  --output text
# Expected: arn:aws:iam::906286017800:oidc-provider/token.actions.githubusercontent.com

# Roles
for role in kitehub-github-terraform-plan \
            kitehub-github-deploy \
            kitehub-github-ecr-push \
            kitehub-github-restore-drill; do
  echo "=== $role ==="
  aws iam get-role --role-name "$role" \
    --query 'Role.[RoleName,CreateDate,MaxSessionDuration]' --output table
done

# Trust policy verification (each role must trust GitHub OIDC)
aws iam get-role --role-name kitehub-github-terraform-plan \
  --query 'Role.AssumeRolePolicyDocument.Statement[0].[Principal.Federated,Condition]' \
  --output json
# Expected: federated arn ends with `oidc-provider/token.actions.githubusercontent.com`
# Condition includes `token.actions.githubusercontent.com:sub` matching repo

# Attached policies (read-only enumeration)
for role in kitehub-github-terraform-plan kitehub-github-deploy \
            kitehub-github-ecr-push kitehub-github-restore-drill; do
  echo "=== $role policies ==="
  aws iam list-attached-role-policies --role-name "$role" \
    --query 'AttachedPolicies[].PolicyName' --output text
  aws iam list-role-policies --role-name "$role" \
    --query 'PolicyNames' --output text
done
```

---

## Pass criteria

- [ ] OIDC provider exists for `token.actions.githubusercontent.com`
- [ ] 4 roles exist with correct names
- [ ] Each role's trust policy restricts to `repo:VictorAurelius/2026-Kite-Class-Platform:*` (or appropriate sub claim)
- [ ] `kitehub-github-terraform-plan` has AWS-managed `ReadOnlyAccess` + state bucket inline
- [ ] `kitehub-github-deploy` has SSM + Secrets read on `kite/{staging,prod}/*`
- [ ] `kitehub-github-ecr-push` has ECR push on `kite/*` repos only
- [ ] `kitehub-github-restore-drill` has S3 backup read

---

## Tier 1 only

All commands `iam get-*` / `iam list-*` — metadata enumeration. No `iam create-*`, `iam attach-*`, `iam put-*`.

`iam get-access-key-last-used` is BANNED per `agent-aws-access.md` §2.2 (surface for credential mining).

---

## End-to-end OIDC flow verify (optional — requires GitHub Actions trigger)

The roles are useless until a workflow assumes them. To verify end-to-end:
1. Tag a no-op commit to trigger `terraform-plan.yml`
2. Watch the job log for `Configure AWS credentials via OIDC` step
3. Confirm `aws sts get-caller-identity` in the workflow shows the role ARN

This is NOT done from this skill (workflow trigger is a write action). User triggers, agent observes.
