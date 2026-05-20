# Env reference self-test fixture

Sample markdown sử dụng mkdocs-macros syntax per `.claude/rules/markdown-variable-reference.md`.

Dùng để verify `scripts/render-env-vars.sh` substitute đúng + `scripts/check-unresolved-env-vars.sh` không false-positive trên rendered output.

## Sample references

- AWS account ID: `{{aws_account_id}}`
- AWS region: `{{aws_region}}`
- Apex domain: `{{domain_root}}`
- Support email: `{{support_email}}`
- DB name: `{{db_name}}`
- Secret prefix: `{{secret_prefix}}`
- ECR URI: `{{ecr_uri}}`
- CloudTrail name: `{{cloudtrail_name}}`

## Expected (prod) output

After `bash scripts/render-env-vars.sh prod .claude/rules/_examples/env-reference-self-test.md /tmp/out.md`:

- `aws_account_id` → `906286017800`
- `domain_root` → `kitehub.me`
- `secret_prefix` → `kitehub/production`

CI guarantees: rendered file scan via `scripts/check-unresolved-env-vars.sh` exits 0 (no unresolved references after substitution).
