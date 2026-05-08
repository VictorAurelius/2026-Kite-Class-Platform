---
name: deploy-preflight-simulator
description: Use khi sắp tag release / push image / merge PR thay đổi terraform-aws hoặc workflow CI. Phrases trigger — "preflight deploy", "simulate deploy", "kiểm tra deploy trước khi tag", "check trước khi staging", "what could break before deploy", "deploy dry-run", "find pre-deploy issues". Chạy 8 categories of checks chống lại hệ thống thật để bắt class-of-issues mà tag-time fail (multi-arch base image, IAM ARN naming drift, OIDC trust scope, secret naming, region pin, workflow if-conditions). Grounds in AWS Well-Architected, OpenSSF Scorecard, GitHub Actions Hardening Guide, Sigstore Cosign Best Practices, CIS Docker Benchmark.
---

# Deploy Preflight Simulator

Cau-trúc-static check chống lại deploy artifacts (terraform IAM/ECR, GitHub workflows, Dockerfiles, ECR repo names, secrets) để bắt mismatch BEFORE tag push triggers real CI run.

## Why this skill exists

Phase 3 first 2 OIDC trigger attempts failed (run #25527705091 multi-arch + #25528087813 IAM ARN drift). Both = static-resolvable mismatches mà nếu pre-deploy simulator chạy, sẽ catch trước khi user-tag. Cost mỗi failed run = ~10min CI minutes + 1 fix PR + retry tag overhead.

Per `incident-to-rule-pipeline.md` premature-rule guard: 1st recurrence = no formal rule, 2nd recurrence = consider tier 2. Multi-arch + IAM ARN = 2 distinct classes of issues caught in same Phase 3 → escalate to skill (lighter than rule, runs on-demand).

## Skill contents

- `reference/standards-references.md` — Authoritative sources cited (AWS WA, OpenSSF, GitHub Actions Hardening, etc.)
- `reference/check-catalog.md` — 8 categories × specific checks + rationale + standard reference
- `reference/known-failure-modes.md` — Catalog of past Phase 1-3 incidents this skill prevents
- `scripts/preflight.sh` — One-command runner; exit 0 = clean, non-zero = issues found
- `scripts/checks/` — Per-category check scripts (each idempotent, read-only)
- `data/runs.log` — Append-only history (date + categories run + issues found)

## Categories (each script in `scripts/checks/`)

1. **Multi-arch base image manifest** — verify every `FROM` line in matrix Dockerfiles has manifest for every `platforms:` declared in workflow
2. **IAM resource ARN naming drift** — verify terraform `aws_iam_role_policy` Resource patterns match actual resource ARNs (ECR repo names, S3 bucket names, Secrets Manager paths)
3. **OIDC trust policy claim scope** — verify GitHub Actions `sub` claim covers actually-used branches/tags/environments
4. **Secret naming drift** — cross-check secret IDs in script/runbook/code/terraform — same naming convention everywhere
5. **Region pin consistency** — verify `AWS_REGION` env in workflow matches `var.aws_region` in terraform matches runbook references
6. **Workflow `if:` condition coverage** — verify conditions don't silently skip required jobs (e.g., `vars.AWS_CONFIGURED == 'true'` requires var actually set)
7. **GitHub Variables + Secrets pre-flight** — verify required `vars.*` and `secrets.*` referenced by workflow exist on repo
8. **Dockerfile `FROM` reachability** — pull-test each base image to confirm registry availability + manifest

## Quick usage

```bash
# Run all 8 categories
bash .claude/skills/devops/deploy-preflight-simulator/scripts/preflight.sh

# Run single category
bash .claude/skills/devops/deploy-preflight-simulator/scripts/preflight.sh --only iam-arn

# CI mode (fail on any non-zero)
bash .claude/skills/devops/deploy-preflight-simulator/scripts/preflight.sh --strict
```

Output: per-category `[PASS]` / `[WARN]` / `[FAIL]` with concrete file:line + suggested fix. Append to `data/runs.log`.

## Gotchas

- **Tier 1 read-only only** per `agent-aws-access.md` — categories 7 + 8 use `gh api` + `docker buildx imagetools inspect` (both read-only); NEVER mutates AWS or pushes images
- **Slow path** — category 8 (Dockerfile FROM reachability) requires network + may take 30-60s for 10 base images. Skip via `--skip 8` for quick iteration
- **False positives khi terraform module resource refactored mid-flight** — re-run after terraform apply to reset baseline
- **Not a substitute for `terraform plan`** — preflight checks STATIC strings; terraform plan checks AWS API state. Run both before merge
- **`gh` CLI required** for category 7 (GitHub Variables/Secrets enumeration); falls back to WARN if unauthenticated
- **`docker buildx` required** for category 8; degrades to manifest-fetch via `curl` if buildx unavailable
- **NOT designed for production runtime** — pre-deploy artifact check only; runtime SLO/health is separate concern (see `quality/ops-readiness-audit/SKILL.md`)

## Standards & references

See `reference/standards-references.md` for full citations. Anchor sources:
- AWS Well-Architected Framework — Operational Excellence pillar (design for failure, automate operations)
- OpenSSF Scorecard — supply chain hygiene
- GitHub Actions Security Hardening Guide — OIDC + token scope
- Sigstore Cosign Best Practices — keyless signing prerequisites
- CIS Docker Benchmark v1.6 — base image hygiene
- HashiCorp Terraform Best Practices — IAM least-privilege ARN patterns

## Out of scope

- Runtime ops monitoring → `quality/ops-readiness-audit/SKILL.md`
- AWS API live verification → `quality/business-logic-audit/SKILL.md` + `devops/aws-smoke-test/SKILL.md`
- Code quality / business logic correctness → `quality/quality-audit/SKILL.md`
- Security scanning (CVE, SAST) → `quality/security-audit/SKILL.md`

This skill = static deploy-artifact preflight only.

## Log

- 2026-05-07 — Skill created (Wave 42 closure). Triggered by Phase 3 first OIDC trigger 2-attempt failure (multi-arch + IAM ARN). Tier 1 of escalation per `incident-to-rule-pipeline.md` — skill before formal rule.
