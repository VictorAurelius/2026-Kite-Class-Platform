# Check Catalog — 8 categories with rationale + standard reference

Each check identifies STATIC mismatch in deploy artifacts before tag push triggers real CI. Output format: `[PASS|WARN|FAIL] <category>: <one-line summary> [→ <file:line>]`.

---

## Category 1 — Multi-arch base image manifest

**Standard:** OCI Image Spec §5.1 + CIS Docker Benchmark §4.1

**What it checks:** For every workflow `platforms: linux/<arch>,linux/<arch>` declaration, verify each `FROM` line in matrix Dockerfiles has registry manifest for every declared arch.

**Tool:** `docker buildx imagetools inspect <image>` (read-only) or `curl` against registry manifest endpoint.

**Failure example:** `maven:3.9-eclipse-temurin-17-alpine` Docker Hub publishes amd64 manifest only; multi-arch `linux/amd64,linux/arm64` build fails with `no match for platform in manifest: not found`.

**Worked precedent:** Phase 3 attempt 1 (run #25527705091, 2026-05-07) — 7 Java services failed.

---

## Category 2 — IAM resource ARN naming drift

**Standard:** NIST SP 800-53 AC-6 (Least Privilege) + AWS IAM Best Practices + HashiCorp Terraform style

**What it checks:** For every `aws_iam_role_policy` Resource pattern, cross-reference against actual resource ARN format from companion `.tf` files (ECR repo names from `ecr.tf`, S3 bucket names from `s3.tf`, Secrets paths from `secrets.tf`).

**Detection:** parse JSON-encoded `policy.Statement[*].Resource` strings; for each `repository/<pattern>`, check pattern matches every entry in `locals.ecr_services`.

**Failure example:** policy says `repository/${var.project_name}-*` (expands `repository/kitehub-*`) but ecr.tf creates `kite/kitehub-*` (slash namespace) → ECR ARN format `repository/kite/kitehub-subscription` doesn't match wildcard `kitehub-*`.

**Worked precedent:** Phase 3 attempt 2 (run #25528087813, 2026-05-07) — 8 Push to ECR jobs returned 403 Forbidden on HEAD blob check.

---

## Category 3 — OIDC trust policy claim scope

**Standard:** GitHub Actions Security Hardening Guide + Sigstore Cosign Best Practices

**What it checks:** For every `aws_iam_role` with GitHub OIDC trust policy, verify `token.actions.githubusercontent.com:sub` claim covers the actual ref pattern that workflow `on:` triggers will produce.

**Common failure modes:**
- Trust policy `sub` = `repo:org/repo:ref:refs/heads/main` only, but workflow triggered on `pull_request` (sub = `repo:org/repo:pull_request`)
- Trust policy `sub` = `repo:org/repo:ref:refs/tags/v*` but tag is `v0.9.0-beta-staging.1` (matches OK) vs `v1.0.0-rc.1` (also matches)
- Trust policy missing `environment:staging` claim when workflow uses GitHub Environments

**Detection:** parse workflow `on:` events + ref patterns + environment refs; cross-check against trust policy `Condition.StringLike` / `StringEquals` claims.

---

## Category 4 — Secret naming drift

**Standard:** Twelve-Factor App Factor III (Config) + AWS Secrets Manager naming consistency

**What it checks:** Cross-reference secret IDs across:
- Terraform `aws_secretsmanager_secret.name`
- Bash scripts `--secret-id <id>`
- Application code `secretsmanager:GetSecretValue` references
- Runbook documentation
- IAM role policy `Resource = "arn:aws:secretsmanager:*:*:secret:<pattern>"`

**Detection:** grep all 5 sources; report mismatches (e.g., terraform creates `kite/prod/db-password` hyphen but script reads `kite/prod/db/password` slash).

**Failure example:** Wave 42 Bucket C populate-secrets.sh used `kite/prod/db/password` (slash); terraform may have created `kite/prod/rds-password` (hyphen prefix). Agent flagged this in PR #1001 body.

---

## Category 5 — Region pin consistency

**Standard:** Twelve-Factor App Factor V (Build/release/run separation) + AWS Well-Architected OPS-04

**What it checks:** Verify `AWS_REGION` consistent across:
- `.github/workflows/*.yml` `env.AWS_REGION`
- `infrastructure/terraform-aws/variables.tf` `var.aws_region.default`
- `documents/05-guides/deploy/*.md` runbook references (regex `ap-southeast-1`, `ap-southeast-2`, etc.)
- `kitehub/scripts/*.sh` AWS CLI invocations

**Failure example:** workflow uses `ap-southeast-1`, runbook accidentally references `us-east-1` from copy-paste of AWS docs.

---

## Category 6 — Workflow `if:` condition coverage

**Standard:** GitHub Actions Security Hardening Guide §"Conditional execution"

**What it checks:** For every `if:` condition referencing `vars.<X>` or `secrets.<X>`, verify variable/secret actually exists on the repo (via `gh api`).

**Failure example:** Workflow `if: vars.AWS_CONFIGURED == 'true'` skips entire job silently when `AWS_CONFIGURED` not set (default empty string ≠ `'true'`). User thinks workflow runs; actually skipped.

**Worked precedent:** Phase 3 setup 2026-05-07 — `AWS_CONFIGURED` was missing from GitHub Variables until coordinator set it pre-tag.

---

## Category 7 — GitHub Variables + Secrets pre-flight

**Standard:** GitHub Actions Security Hardening Guide + OpenSSF Scorecard Token-Permissions check

**What it checks:** Enumerate `vars.*` and `secrets.*` references in all `.github/workflows/*.yml`; verify each exists on repo via `gh api repos/.../actions/variables` + `gh api repos/.../actions/secrets`.

**Tool:** `gh` CLI (read-only `repos/<owner>/<repo>/actions/variables` + `secrets`).

**Failure example:** Workflow references `secrets.AWS_ECR_PUSH_ROLE_ARN` but secret not yet created on repo → role-to-assume becomes empty string → `aws-actions/configure-aws-credentials` fails with cryptic "InvalidParameter".

---

## Category 8 — Dockerfile `FROM` reachability

**Standard:** CIS Docker Benchmark §4.1 (use trusted base images) + Chainguard Distroless practices

**What it checks:** For every `FROM <image>` in deploy Dockerfiles, attempt manifest fetch via `docker buildx imagetools inspect`. Verify:
- Image exists in declared registry
- Image not deprecated (some EOL images return 410)
- Base image SHA published recently (CIS recommends < 6 months old to ensure security patches)

**Tool:** `docker buildx imagetools inspect` (read-only) — does NOT pull image, only manifest.

**Failure example:** Dockerfile `FROM eclipse-temurin:17-jre-alpine` works today; `FROM eclipse-temurin:21-jdk-alpine-test-only` may 404. Multi-arch sub-check (Category 1) extends this with arch matching.

---

## Output schema

Each script in `scripts/checks/` outputs lines:

```
[PASS|WARN|FAIL] <category-id>: <summary>
  └─ <file>:<line>: <detail>
  └─ standard: <reference>
  └─ fix: <suggested-action>
```

Exit codes:
- `0` — all PASS
- `1` — at least one WARN (no FAIL) — proceed with caution
- `2` — at least one FAIL — block deploy

Top-level `preflight.sh` aggregates all 8 category outputs + summary line + appends to `data/runs.log`.

---

## Updates

To add a 9th category:
1. Cite primary or secondary standard from `standards-references.md`
2. Add row to this catalog with rationale + worked example
3. Implement `scripts/checks/<category-id>.sh`
4. Update `scripts/preflight.sh` registry of categories
5. Append to `data/runs.log` precedent run

PRs adding categories without standard reference → reject (per `standards-references.md` anti-references policy).
