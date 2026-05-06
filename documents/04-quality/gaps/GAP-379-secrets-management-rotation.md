# GAP-379: Secrets Management — AWS Secrets Manager + Rotation Policy

**Status:** 🔵 OPEN
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

- [ ] AWS Secrets Manager configured + IAM policies
- [ ] Terraform extends `secrets.tf` cho all required secrets
- [ ] Spring Boot config integration tested
- [ ] All hardcoded secrets removed from config files / env vars
- [ ] Rotation Lambda functional cho DB passwords
- [ ] Rotation policy documented
- [ ] Audit: CloudTrail logs secret access
- [ ] Smoke test: rotate secret → verify no service downtime
- [ ] Documentation: how to add new secret + rotate manually

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
