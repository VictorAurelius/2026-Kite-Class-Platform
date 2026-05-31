# CI Workflows — Index & Taxonomy

Canonical classification of every GitHub Actions workflow. `name:` fields use a **6-category prefix taxonomy** (`Quality —` / `Test —` / `Security —` / `Build —` / `Deploy —` / `Ops —`) so the Actions tab + `gh run list` group by purpose. File names are kept stable (self-referenced in `paths:` triggers + docs).

> **Why prefixes, not GitHub tags?** Actions has no first-class workflow tags. The `name:` prefix is the de-facto tag — `gh run list --workflow=...` and the Actions UI sort/filter by it.

`main` has **no branch protection** → no required status checks. The merge gate for docs-only PRs is rule logic (`docs-only-pr-auto-merge.md` reads `gh pr checks` state), not check-name pinning — so renaming `name:` is safe.

---

## Categories

| Category | Meaning | Trigger style |
|---|---|---|
| **Quality** | Static checks / lint / docs governance (no build, no deploy) | PR (path-scoped) |
| **Test** | Build + test service code (Java / TS / E2E / perf) | PR (code paths) |
| **Security** | Secret scan / DAST | PR (code) + dispatch |
| **Build** | Produce artifacts (Docker images, Pages, release tag) | push / tag |
| **Deploy** | Mutate infra / production (human dispatch only) | workflow_dispatch |
| **Ops** | Scheduled maintenance | cron / dispatch |

---

## Workflow registry

| Workflow file | `name:` | Category | Trigger | Docs-only PR? | Notes |
|---|---|:---:|---|:---:|---|
| `quality-code.yml` | Quality — Code | Quality | PR | ⚠️ partial | Most jobs skip on docs (changed-file detect); **Mermaid `<br/>` check** runs on `**/*.md` |
| `quality-docs.yml` | Quality — Docs | Quality | PR | ✅ | READMEs + gaps + audits + waves + 3-layer + docs scaling |
| `quality-rules-skills.yml` | Quality — Rules / Skills | Quality | PR | ✅ | Rule frontmatter + skill conventions + staleness + rule count + meta CSV + **context-budget** + env-vars |
| `quality-infra.yml` | Quality — Infra | Quality | PR | ❌ | Helm lint + alert runbook URL (infra paths) |
| `actionlint.yml` | Quality — Actionlint | Quality | PR + push | ❌ | Only `.github/workflows/**` |
| `terraform-plan.yml` | Quality — Terraform Plan | Quality | PR | ❌ | Read-only plan on `infrastructure/terraform-aws/**` |
| `core-ci.yml` | Test — Core Service (KiteClass) | Test | PR | ❌ | KiteClass core Java build+test |
| `kitehub-ci.yml` | Test — KiteHub Platform | Test | PR | ❌ | KiteHub services Java build+test |
| `frontend-ci.yml` | Test — KiteClass Frontend | Test | PR | ❌ | |
| `kitehub-frontend-ci.yml` | Test — KiteHub Frontend | Test | PR + push | ❌ | |
| `smoke-tests.yml` | Test — Smoke Tests | Test | PR + dispatch | ❌ | |
| `e2e-pre-release.yml` | Test — E2E Pre-release Gate | Test | push + dispatch | ❌ | |
| `lighthouse.yml` | Test — Lighthouse | Test | PR + dispatch | ❌ | Perf budget on kitehub-frontend |
| `ui-kits-integration.yml` | Test — UI Kits Integration | Test | PR | ❌ | Only `design-system/ui_kits/**` |
| `gitleaks-scan.yml` | Security — Gitleaks Secret Scan | Security | PR + push | ❌ | **Excludes docs** (code/config/`.env` only) |
| `zap-baseline.yml` | Security — OWASP ZAP Baseline | Security | dispatch | ❌ | DAST against deployed env |
| `docker-build-push.yml` | Build — Docker Images | Build | PR + push + dispatch | ❌ | Push images to ECR |
| `deploy-design-system.yml` | Build — Design System (Pages) | Build | push + dispatch | ❌ | GitHub Pages publish |
| `release-tag.yml` | Build — Release on Tag | Build | push (tag) | ❌ | GitHub Release on `v*` tag |
| `deploy-production.yml` | Deploy — Production | Deploy | dispatch | ❌ | Human-triggered (per `release-deploy-standard.md` §9) |
| `deploy-staging.yml` | Deploy — Staging | Deploy | push + dispatch | ❌ | |
| `terraform-apply.yml` | Deploy — Terraform Apply | Deploy | dispatch | ❌ | confirm=APPLY gate + OIDC role |
| `rollback.yml` | Deploy — Rollback Production | Deploy | dispatch | ❌ | confirm=APPLY gate |
| `seed-production.yml` | Deploy — Seed Production Data | Deploy | dispatch | ❌ | |
| `ec2-bootstrap.yml` | Deploy — EC2 Bootstrap | Deploy | dispatch | ❌ | One-time provisioning |
| `cloudflare-apex-cutover.yml` | Deploy — Cloudflare Apex Cutover | Deploy | dispatch | ❌ | DNS apex cutover |
| `tier-3-cutover.yml` | Deploy — Tier 3 Cutover | Deploy | dispatch | ❌ | |
| `ci-cleanup.yml` | Ops — CI History Cleanup | Ops | cron + dispatch | ❌ | Weekly run retention (50-run cap) |
| `restore-drill.yml` | Ops — Restore Drill (monthly) | Ops | cron + dispatch | ❌ | Backup restore verification |

---

## Docs-only PR CI surface (quick reference)

A docs-only PR (diff ⊂ `documents/**` + `.claude/rules/**` + `.claude/skills/**` + `*.md`) fires **only the `Quality —` family**, scoped by which subpath changed:

| Diff touches | Fires |
|---|---|
| `.claude/rules/**` | `Quality — Rules / Skills` + `Quality — Code` (Mermaid only) |
| `documents/04-quality/gaps/**` | `Quality — Docs` + `Quality — Rules / Skills` + `Quality — Code` (Mermaid) |
| any `*.md` with a Mermaid diagram | `Quality — Code` (Mermaid `<br/>`/`;` check) |

All `Test —` / `Security —` / `Build —` / `Deploy —` workflows **skip** docs-only (path filters exclude docs — saves CI minutes per solo-dev mode).

Local-CI parity for docs-only PRs (run before push): see `.claude/rules/ci-queue-local-runner-threshold.md` §3.

---

## Maintenance

- New workflow → add `name:` with a category prefix + a row here (same PR).
- Re-categorizing → update the `name:` prefix + this row together.
- Keep file names stable; rename only when a workflow's purpose fundamentally changes (then update the 16 self-referenced `paths:` + docs links).
