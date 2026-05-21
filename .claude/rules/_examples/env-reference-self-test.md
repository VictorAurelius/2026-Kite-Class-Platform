# Env Reference Self-Test Fixture

Sample doc demonstrating env-var substitution per `.claude/rules/markdown-variable-reference.md` v1.0.0.

This file is the SOURCE — `scripts/render-env-vars.sh` will substitute placeholders với values từ `documents/02-architecture/env-reference.yaml`.

The matching control file `env-reference-self-test-expected-production.md` contains the expected rendered output for `production` env. CI job `env-vars-render` (per `.github/workflows/script-quality.yml`) runs render + diff check to verify byte-identical output.

Note: literal placeholder examples below use the `\{{...\}}` escape so render passes through as literal text — see rule §2 Syntax for escape semantics.

---

## Sample deploy plan section

KiteHub production deploy targets AWS account `{{aws_account_id}}` in region `{{aws_region}}`.

Primary domain: `{{domain_root}}`. Support contact: `{{support_email}}`.

Secrets Manager prefix: `{{secret_prefix}}/<service-name>` per service (e.g., `{{secret_prefix}}/jwt-signing-key`).

ECR registry: `{{ecr_uri}}` (push/pull Docker images via `aws ecr get-login-password`).

Deploy IAM role ARN: `{{iam_deploy_role}}` (assumed via OIDC by `.github/workflows/terraform-apply.yml`).

CloudTrail trail name: `{{cloudtrail_name}}` (verified `IsLogging=true` pre-apply per `aws-observability-first.md` mandate).

## Sample database section

RDS database name: `{{db_name}}`
RDS master user: `{{db_user}}`

## Sample escape syntax

End of fixture. After rendering, output should contain ZERO `\{{...\}}`-shaped placeholders — check-unresolved-env-vars.sh validates this.
