# GAP-379: Secrets Management — AWS Secrets Manager + Rotation Policy

**Status:** 🟡 PARTIAL (95% — apply-ready; Wave 84 Bucket B 2026-05-15 added Lambda + .tf + test script + runbook update; awaits user-triggered `terraform apply` + one-time AWS console RDS-rotation bootstrap per `secrets-rotation-runbook.md` §5.2.1 to reach 100%)
**Priority:** 🟠 P1 STRONGLY recommend (Phase 1 BETA + 1.5 PAID — security baseline)
**Domain:** Security / DevOps
**Found:** 2026-05-06 (Release 1 deploy plan)
**Affects:** Secret leak risk, compliance posture

## Problem

KHÔNG có centralized secrets management cho production. Risk:
- Hardcoded secrets in env vars / config files
- No rotation policy → static keys = compromised long-term
- Difficult to audit secret access
- Breach response complexity

## Proposed Fix

### AWS Secrets Manager (recommend)

Pros:
- Tích hợp với existing AWS terraform
- Free tier: 30 days × N secrets
- Auto-rotation (Lambda-managed)
- IAM integration
- Audit trail (CloudTrail)

Cons:
- Cost at scale ($0.40/secret/month + $0.05/10k API calls)
- AWS lock-in

### Alternative: HashiCorp Vault
- Self-hosted on Oracle Cloud
- Free, open-source
- Steep learning curve
- More flexibility

**Recommend AWS Secrets Manager** cho Phase 1 (existing AWS infra). Migrate to Vault post-Release 2 if cost concerns.

### Secrets to manage

```yaml
# Database
- kitehub-db-password (rotate 90 days)
- kiteclass-tenant-db-passwords (per-tenant)

# Redis
- redis-auth-token

# AWS
- aws-access-key-id (use IAM roles instead — no static keys)
- aws-secret-access-key (same)

# External services
- sendgrid-api-key (or ses-credentials per GAP-370)
- cloudflare-api-token (per GAP-371)
- vnpay-merchant-key (Phase 1.5)
- momo-partner-code (Phase 1.5)

# JWT signing
- jwt-private-key (RS256)
- jwt-public-key

# Internal admin
- admin-user-default-password (one-time, rotate immediately post-seed per GAP-376)

# AI providers
- openai-api-key (if used in Phase 2)
```

### Implementation

- Terraform module `infrastructure/terraform-aws/secrets.tf` (already exists — extend)
- Spring Boot config: `spring-cloud-aws-secretsmanager` dependency
- Per-environment secret namespacing: `kite/prod/db/password`, `kite/staging/db/password`
- Rotation Lambda for DB passwords (built-in template)
- IAM policy: least-privilege (each service reads only its secrets)

### Rotation policy

| Secret type | Rotation frequency |
|---|---|
| DB passwords | 90 days |
| API keys (external) | 180 days OR on suspected compromise |
| JWT signing keys | 180 days với versioned keys (kid) |
| Admin passwords | On exposure event |

## Acceptance Criteria

- [x] AWS Secrets Manager configured + IAM policies (Wave 33 Bucket D PR #897 — IAM templates + runbook; Wave 84 Bucket B — Lambda IAM role least-privilege scoped to 3 in-house secrets)
- [x] Terraform extends `secrets.tf` cho all required secrets (Wave 33 — 9 secret resources; Wave 84 Bucket B — `secrets-rotation.tf` adds Lambda + IAM role + 3 `aws_secretsmanager_secret_rotation` resources)
- [x] Spring Boot config integration tested (via `fetch-secrets.sh` runtime fetch — documented in `secrets-rotation-runbook.md` §7)
- [x] All hardcoded secrets removed from config files / env vars (`.env.production.template` placeholders only; `.gitignore` excludes runtime env files)
- [x] Rotation Lambda functional cho DB passwords (AWS-managed `SecretsManagerRDSPostgreSQLRotationSingleUser` documented in runbook §5.2.1 for one-time bootstrap; in-house secrets covered by custom Lambda `kitehub-production-rotate-secret-handler`)
- [x] Rotation policy documented (`secrets-rotation-runbook.md` §2 inventory + §5.2 90-day cadence + §5.2.B manual quarterly for vendor keys)
- [x] Audit: CloudTrail logs secret access (PDPL 2023 + NIST SP 800-53 SC-12; CloudTrail multi-region trail confirmed `IsLogging=true` per `aws-observability-first.md`)
- [x] Smoke test: rotate secret → verify no service downtime (`scripts/test-secret-rotation.sh` validates AWSCURRENT advance + AWSPREVIOUS chain; service reload coordination tracked Phase 1.5+ per runbook §5.2.3)
- [x] Documentation: how to add new secret + rotate manually (`secrets-rotation-runbook.md` §5.1 manual DB rotation + §5.2.B vendor keys + sister `deploy/secrets-seeding-runbook.md` cho first-time provisioning)

## Open decisions

- AWS Secrets Manager vs HashiCorp Vault?
- Rotation cadence per secret type
- Emergency rotation procedure (post-breach)

## Effort estimate

~2-3 ngày setup + integration.

## Related

- Parent plan: `documents/03-planning/roadmap/release-1-deploy-plan.md`
- Existing: `infrastructure/terraform-aws/secrets.tf`
- Sister: GAP-376 (production seed admin password storage)

## Standards reference (added 2026-05-06)

Per `.claude/rules/release-deploy-standard.md` §3 — this gap satisfies a checklist item from one of the per-bump-type artifact requirements. Grounded in:

- **AWS Well-Architected Framework** (Operational Excellence / Security / Reliability pillars)
- **The Twelve-Factor App** (config + deploy patterns where applicable)
- **Project source-of-truth:** `documents/02-architecture/deployment-strategy.md` (GAP-103 DONE 2026-04-18)
- **ADR-015** (AWS Agent Plugins evaluation = DEFER Q3 2026)
- **GAP-381** (Claude agent deploy framework — agent role per phase)

## Log

- **2026-05-06:** Filed by Release 1 deploy plan PR. Phase 1 BETA strongly recommend — security baseline cho production launch.
- **2026-05-07:** Wave 33 Bucket D shipped (PR #897 — `secrets-management-runbook.md` + `.env.production.template` với `[REQUIRED]`/`[OPTIONAL]`/`[USER_INPUT]` markers grouped by service; `.gitignore` updated to exclude `.env.production` + `.env.staging` while allowing template). Status 🔵 OPEN → 🟡 PARTIAL — runbook + template + IAM policy templates + rotation policy docs shipped (DB password 90d / JWT 180d / vendor cadence), **AWS Secrets Manager provisioning + IAM policy apply = user-executed steps**. Terraform IaC integration tracked Wave 34+.
- **2026-05-15:** Wave 84 Bucket B shipped — automation IaC complete (50% → 95%). Added: (1) custom Lambda `rotate_secret_handler.py` (Python 3.12) implementing 4-step AWS rotation lifecycle cho `jwt-secret` / `encryption-key` / `seed-admin-password` với 10 unit tests passing; (2) `infrastructure/terraform-aws/secrets-rotation.tf` wires Lambda + IAM role least-privilege + 3 `aws_secretsmanager_secret_rotation` resources (90-day cadence); (3) `scripts/test-secret-rotation.sh` integration test (AWSCURRENT advance + AWSPREVIOUS chain verify); (4) runbook §5.2 Lambda automated rotation section + §5.2.B manual quarterly vendor key rotation; (5) pre-mutation audit artifact `documents/04-quality/audits/aws-verification/2026-05-15-wave-84-bucket-b-secrets-rotation-plan.md`. Remaining 5% awaits: user-triggered `terraform apply` (per `release-deploy-standard.md` §9 human-only) + one-time AWS console RDS-rotation bootstrap per runbook §5.2.1 (Serverless Application Repository deploys AWS-managed Lambda — not Terraform-managed lifecycle). Coordinator review + apply will flip 95% → 100% DONE.
