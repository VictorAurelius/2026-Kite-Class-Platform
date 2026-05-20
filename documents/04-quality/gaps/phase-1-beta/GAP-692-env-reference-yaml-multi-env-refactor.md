# GAP-692: env-reference.yaml multi-env refactor (docs + scripts + terraform hardcoded values)

**Status:** 🔵 OPEN
**Priority:** 🟠 P1 (META — force-multiplier per `meta-gap-priority.md` §3; eliminates Class 4 config-drift recurrence)
**Domain:** DevOps + Meta
**Detected:** 2026-05-21
**Related PRs:** TBD
**Related Docs:** Outside-in audit synthesis 2026-05-21; `.claude/rules/production-env-config-registry.md`; `.claude/rules/audit-to-gap-pipeline.md` §2.7

## Current State (verified 2026-05-21 via 3 parallel audit agents)

> Per `audit-to-gap-pipeline.md` §2.5 + §2.7. Audit agents A (docs) + B (infra) + C (design) completed 2026-05-21. Existing partial registry exists nhưng scope hẹp (application*.yml only) — broader hardcoded-in-docs/scripts/terraform scope CHƯA covered.

### Existing infrastructure (DO NOT duplicate)

| Piece | File / Path | Status |
|-------|-------------|--------|
| Production env config registry (runtime env-var coverage) | `.claude/rules/production-env-config-registry.md` v1.1.1 | ✅ shipped; scopes `application*.yml` + `docker-compose*.yml` + `fetch-secrets.sh` (17/45 candidates indexed per registry doc) |
| Env-vars registry doc | `documents/02-architecture/env-vars-registry.md` | ✅ shipped (rule §3.1 mandate) |
| Audit scripts (runtime coverage) | `scripts/audit-env-coverage.sh` + 3 sister audits | ✅ shipped Wave 71 Bucket E |
| Terraform variables centralized | `infrastructure/terraform-aws/variables.tf` | ⚠️ partial — `var.aws_region` `var.project_name` `var.rds_db_name` ✅; `var.aws_account_id` ❌; `var.domain_name = "kiteclass.com"` **STALE mismatch** với `kitehub.me`; secrets prefix ❌ |
| Spring profile env override pattern | `${VAR:default}` in `application-production.yml` | ✅ shipped |

### Missing pieces (gap delta)

| Piece | File / Path | Status |
|-------|-------------|--------|
| Canonical env reference YAML | `documents/02-architecture/env-reference.yaml` | ❌ missing |
| Markdown variable substitution syntax | `{{var_name}}` mkdocs-macros convention | ❌ no docs use this yet |
| Render script | `scripts/render-env-vars.sh` | ❌ missing |
| CI validator unresolved refs | `scripts/check-unresolved-env-vars.sh` | ❌ missing |
| Pre-commit hook forbid new hardcoded values | `.husky/check-hardcoded-env-values.sh` | ❌ missing |
| Migration plan rule | `.claude/rules/markdown-variable-reference.md` | ❌ missing |

### Audit findings — hardcoded occurrence (combined docs + infra scope)

| Value | Files | Occurrences | Already var? |
|-------|------:|------------:|--------------|
| `kitehub.me` domain | 95 | 1,439 | ⚠️ TF `var.domain_name = "kiteclass.com"` mismatch (STALE) |
| `ap-southeast-1` region | 100 | 559 | ✅ TF `var.aws_region` exists; 93 hardcoded literals trong workflows + helm |
| `906286017800` account ID | 52 | 214 | ❌ no var; CI secret partial |
| `kitehub/production/` secret prefix | 32 | 223 | ❌ no var |
| Subdomains `app/api/admin.kitehub.me` | 27 | 303 | ❌ no var |
| Email `@kitehub.me` | 35 | 230 | ⚠️ Spring `${AWS_SES_FROM_EMAIL:default}` partial |
| EC2 instance IDs (specific) | 24 | 135 | ❌ should pull from `terraform output` |
| CloudWatch log group `/kitehub/` | 27 | 59 | ❌ no var |
| S3 buckets `kitehub-*` | 50+ | 2,800+ | ⚠️ TF `${var.project_name}-*` partial |

**Grep commands run:** delegated to Explore agents A + B 2026-05-21 (audit artifacts đính kèm trong synthesis).

## Problem

Phase 1 BETA → Phase 1.5+ multi-env (test / v1 / v2 / staging) sẽ multiply environment-specific values. Hiện tại:

1. **Account swap rebuild requires sweep 52+ files** mỗi lần đổi account ID (per GAP-612 if rebuild triggers Item 1 path B)
2. **TF var.domain_name = "kiteclass.com"` STALE** mismatch real domain `kitehub.me` — silent risk (TF default applied accidentally → DNS routes to wrong zone)
3. **Class 4 failure-mode (config-drift) recurrence rate 100% on rebuild** per outside-in failure-mode matrix synthesis 2026-05-21 — GAP-458→GAP-459 AWS Activate denial pattern
4. **GitHub-rendered markdown shows raw `{{var}}`** — UX trade-off needs explicit design

`production-env-config-registry.md` v1.1.1 covers RUNTIME env-var defaults trong `application*.yml`. This gap covers BROADER scope: docs / scripts / terraform / workflows hardcoded values that need parameterization for multi-env.

Per `meta-gap-priority.md` §3 — META P1 force-multiplier (every future rebuild + env-clone + domain rotation benefits).

## Context

- 2026-05-21 outside-in audit synthesis (3 parallel agents) surfaced this as critical insight #1: refactor TRƯỚC rebuild (not after)
- User-chosen sequencing 2026-05-21: Phase 0 local self-test fix → Item 2 (this gap) → rebuild
- Existing 2026-05-09 incident GAP-458→GAP-459 (21 stale `kitehub.vn` refs after domain decision) = proof the pattern recurs

## Evidence

- Outside-in audit synthesis 2026-05-21 — 3 agent reports (A docs scope / B infra scope / C design pattern proposal)
- `.claude/rules/audit-to-gap-pipeline.md` §2.7 Decision-Doc Code-Sync — exactly this pattern
- GAP-458 / GAP-459 prior incident (AWS Activate Founder denied due to stale `kitehub.vn` refs)
- `infrastructure/terraform-aws/variables.tf` line 28-32 — `var.domain_name = "kiteclass.com"` STALE evidence

## Proposed Fix

### Phase 1 — Ship tooling (~1 dev-day, no doc migration yet)

Ship same PR:
- `documents/02-architecture/env-reference.yaml` — canonical schema (8-10 example rows: aws_account_id, aws_region, domain_root, support_email, db_name, db_user, secret_prefix, ecr_uri, iam_deploy_role, cloudtrail_name)
- `scripts/render-env-vars.sh` ~50 LOC bash (yq + sed; handle escape `\{{var}}` for literal)
- `scripts/check-unresolved-env-vars.sh` ~20 LOC CI validator
- `.github/workflows/script-quality.yml` job `env-vars-render` extending
- `.claude/rules/markdown-variable-reference.md` v1.0.0 — usage convention + migration policy
- `_examples/env-reference-self-test.md` — roundtrip render test (1 sample doc rendered for prod → verify byte-identical to source with values substituted)

**Fix existing TF mismatch:** update `infrastructure/terraform-aws/variables.tf` `var.domain_name` default `"kiteclass.com"` → `"kitehub.me"` (or remove default entirely, force explicit). Add `var.aws_account_id` + `var.secrets_prefix`.

### Phase 2 — Opportunistic refactor top 10 high-leverage files (~3 tuần parallel với feature work)

Refactor (chỉ hardcoded → `{{var}}`):
1. `documents/05-guides/deploy/release-1-deploy-plan.md`
2. `documents/05-guides/deploy/release-1-deploy-runbook.md`
3. `documents/05-guides/operations/incident-response-runbook.md`
4. Top 5 `documents/04-quality/audits/aws-verification/*.md` audit templates
5. `documents/05-guides/operations/acceptance-tests/*.csv` companion READMEs
6. `.github/workflows/deploy-production.yml` — extract hardcoded region/account/secret-prefix → workflow env vars
7. `infrastructure/helm/kitehub/values.yaml` — replace 93 `ap-southeast-1` literals với templated `{{ .Values.region }}`
8. `scripts/deploy-prod.sh` — replace hardcoded EC2 IDs với `terraform output` lookup
9. `scripts/fetch-secrets.sh` — replace `kitehub/production/` hardcoded với env var `${SECRETS_PREFIX:kitehub/production}`
10. `infrastructure/terraform-aws/secrets.tf` — replace `kitehub/production/` literal với `var.secrets_prefix`

AC mỗi file: render produces byte-identical output cho prod env; CI validator green.

### Phase 3 — Forbid new hardcoded values (pre-commit hook, ~1 tuần)

Pre-commit hook `.husky/check-hardcoded-env-values.sh`:
- Regex `906286017800|kitehub\.me|kitehub/production` outside code-fence + outside `env-reference.yaml` + outside `documents/07-archived/` → FAIL
- WARN mode 30 ngày → HARD STOP
- Existing files grandfathered until next refresh

## Acceptance Criteria

- [ ] Phase 1 — `env-reference.yaml` shipped với 10+ rows (prod/test/dev values)
- [ ] Phase 1 — `render-env-vars.sh` + `check-unresolved-env-vars.sh` shipped + self-test PASS
- [ ] Phase 1 — `.claude/rules/markdown-variable-reference.md` v1.0.0 with usage examples
- [ ] Phase 1 — TF `var.domain_name` STALE mismatch fixed (`kiteclass.com` → `kitehub.me`) + `var.aws_account_id` + `var.secrets_prefix` added
- [ ] Phase 1 — CI job `env-vars-render` validates 1 sample rendered doc identical
- [ ] Phase 2 — Top 10 high-leverage files refactored với `{{var}}` syntax; render for prod = byte-identical to current
- [ ] Phase 2 — 5,500+ S3/region/account/domain occurrences reduced ≥40% (target audit re-run shows count drop)
- [ ] Phase 3 — Pre-commit hook `check-hardcoded-env-values.sh` shipped (WARN mode); grace 30 ngày
- [ ] Phase 3 — Hook HARD STOP enabled post-grace; documented exemption trailer mechanism
- [ ] Integration verified — `production-env-config-registry.md` v1.1.1 still applies (runtime YAML scope) + this gap covers broader scope (docs/scripts/terraform); no duplicate enforcement; cross-link added

## Related

- **GAP-612** AWS account suspension (rebuild triggers this gap urgency)
- **GAP-694** local self-test investigation (parallel work; can ship before/after Phase 1)
- **GAP-693** AWS rebuild SOP playbook (downstream; consumes env-reference.yaml as input)
- **GAP-458 / GAP-459** prior config-drift incident (AWS Activate denial) — exact pattern this gap prevents
- `.claude/rules/production-env-config-registry.md` v1.1.1 — sister rule (runtime env-var scope); this gap = complementary docs/scripts/terraform scope
- `.claude/rules/audit-to-gap-pipeline.md` §2.7 — Decision-Doc Code-Sync (rule mandate this gap operationalizes)
- `.claude/rules/no-vercel-references.md` v1.0.0 — sister pattern (decommission vendor sweep) using grep detector approach
- `meta-gap-priority.md` §3 — META P1 force-multiplier classification
- `incident-to-rule-pipeline.md` 5-stage (this gap = Detect ✓ Classify ✓ Rule+Enforce upcoming Phase 1)
- Outside-in audit synthesis 2026-05-21 (this gap origin — 3 parallel agents)

## Log

- **2026-05-21** — Gap filed from outside-in audit synthesis (Agents A + B + C). Existing `production-env-config-registry.md` v1.1.1 covers application*.yml runtime scope (17/45 candidates indexed per rule §2.1 audit); this gap extends scope to docs/scripts/terraform hardcoded values (broader 95 files × ~5,500+ occurrences). Critical insight: Phase 1 ship BEFORE rebuild eliminates Class 4 config-drift recurrence permanently. Phase 2 refactor opportunistic parallel feature work. Phase 3 pre-commit hook = HARD STOP post-grace.
