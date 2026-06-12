# GAP-698: Wave 102.8 ops-readiness audit deferred + terraform-plan OIDC investigation

**Status:** 🔵 OPEN
**Priority:** 🟠 P1 (META — audit cadence enforcement + CI infra observability)
**Domain:** DevOps + Meta
**Detected:** 2026-05-21
**Related PRs:** #1692 (Bucket B trigger), Wave 102.8 closure PR (TBD), Wave 102.9 (rebuild SOP milestone)
**Related Docs:** `.claude/rules/post-wave-audit-mandate.md` §2.1 + §2.4; `.claude/rules/agent-aws-access.md`; Wave 102.8 plan

## Current State (verified 2026-05-21 via PR #1692 audit-gate hook)

### Trigger event

Wave 102.8 Bucket B (PR #1692) merged 2026-05-21 touching `infrastructure/terraform-aws/variables.tf` — 3-line change:
- `var.domain_name` default `"kitehub.me"` → `"kitehub.me"` (STALE mismatch fix per GAP-692)
- Add `var.aws_account_id` (no default — force explicit)
- Add `var.secrets_prefix` default `"kitehub/production"`

Per `post-wave-audit-mandate.md` §2.1 row "infrastructure/, ... terraform" → triggers ops-readiness audit /100 within 3 days. Audit-gate hook flagged missing audit post-merge.

### Pre-existing CI infrastructure failure

`terraform-plan` workflow has been failing on 2 consecutive PRs (2026-05-20 `feature/gap-692-phase-1-env-reference-yaml-v2` + 2026-05-21 PR #1692):

```
##[error]Could not assume role with OIDC: Not authorized to perform sts:AssumeRoleWithWebIdentity
```

Failure occurs at `aws-actions/configure-aws-credentials@v6` step using `${{ vars.AWS_TERRAFORM_PLAN_ROLE_ARN }}`. Root cause unknown — candidate hypotheses:
1. GitHub Variable `AWS_TERRAFORM_PLAN_ROLE_ARN` empty / incorrect (drift)
2. IAM role trust policy doesn't allow current org/repo OIDC subject
3. IAM role deleted/renamed (correlates with GAP-612 AWS account state)
4. AWS account suspension partial residual

PR #1692 used `terraform validate` PASS locally (Bucket B agent) as substitute evidence; merge proceeded with `AUDIT_OVERRIDE:` trailer per `post-wave-audit-mandate.md` §3 override mechanism.

## Problem

Two coupled issues blocking Wave 102.8 audit completeness:

1. **Ops-readiness audit /100 cadence violation:** Bucket B touched infra files; audit due within 3 days. Without audit, hook blocks future infra-touching PRs (compliance gate active).
2. **Pre-existing `terraform-plan` OIDC failure:** CI cannot validate TF plan output for any PR touching `infrastructure/terraform-aws/**` until OIDC role restored. Wave 102.9 (rebuild SOP) execution scope cannot rely on `terraform-plan` CI without this fix.

## Context

- Wave 102.8 scope = local-self-test foundation (NOT production deploy); TF change is groundwork for Wave 102.9 rebuild SOP
- GAP-612 AWS account suspension (parallel work) may correlate with OIDC failure
- Per `post-wave-audit-mandate.md` §2.4 Domain-Milestone Audit Cadence: `release-deploy-artifacts` domain key allows deferral when wave touches single domain (infra scope only); milestone audit MUST run at Wave 102.9 (rebuild SOP) closure.
- Per `audit-to-gap-pipeline.md` §2.6 wave-plan state-check: GAP-698 filed prospectively from PR #1692 audit hook trigger.

## Evidence

- PR #1692 audit-gate hook output 2026-05-21 — 6 violations including "Missing audits: ops-readiness-audit"
- `gh run view 26205817909 --log-failed` — terraform-plan workflow OIDC failure
- `gh run list --workflow=terraform-plan.yml --limit 5` — 2 consecutive failures on 2 branches confirm pre-existing infra issue
- Bucket B local verify (per PR #1692 §Self-test): `terraform validate` PASS, shellcheck PASS, render-env-vars roundtrip PASS

## Proposed Fix

### Phase 1 — Ops-readiness audit /100 (Wave 102.8 closure OR Wave 102.9 milestone, ≤3 days from 2026-05-21)

Per `.claude/skills/quality/ops-readiness-audit/SKILL.md`:
- Score 10 categories /100 (CloudWatch + SLO + alerts + runbooks + IaC + secrets + DR + observability + cost + auth)
- Compare delta vs Wave 94c 77/100 C+ baseline (last refresh per `output-review-mandate.md` §3)
- Findings → gaps per `audit-to-gap-pipeline.md` §3
- Save report `documents/04-quality/audits/ops/2026-05-2X-wave-102-8-ops-readiness.md`
- Update `audits-index.csv` per `meta-csv-index-pattern.md`

### Phase 2 — `terraform-plan` OIDC role investigation + restoration

Per `agent-aws-access.md` Tier 1 read-only:
```bash
# Investigate role state (NOT mutate)
gh variable list 2>&1 | grep -i AWS_TERRAFORM
aws iam get-role --role-name <terraform-plan-role-from-variable> 2>&1 | head
aws iam get-role-policy --role-name <role> --policy-name <policy> 2>&1 | head
aws iam list-attached-role-policies --role-name <role>
# Trust policy check
aws iam get-role --role-name <role> --query 'Role.AssumeRolePolicyDocument'
```

Findings → restoration plan (per `pre-mutation-state-check.md` if role recreation needed; per `agent-aws-access.md` §4.3 Tier 3 if mutation). Defer Wave 102.9+ if AWS account state (GAP-612) needs resolve first.

### Phase 3 — Re-test PR #1692 TF state retroactively (optional, low priority)

Post-OIDC fix: trigger fresh `terraform-plan` on `main` HEAD to verify Bucket B var changes don't break plan. Acceptable defer if no production apply scheduled.

## Acceptance Criteria

- [ ] Phase 1 ops-readiness audit /100 shipped within 3 days from 2026-05-21 (deadline 2026-05-24) — report + findings + new gaps if any
- [ ] Phase 1 audit covers Wave 102.8 Bucket B infra scope changes (TF var additions)
- [ ] Phase 2 OIDC role state documented (Tier 1 read-only investigation artifact under `aws-verification/`)
- [ ] Phase 2 root cause identified (drift / suspension residual / config / etc.) + restoration plan filed
- [ ] PR #1692 `AUDIT_OVERRIDE:` trailer references this gap (GAP-698) ✅ (already added 2026-05-21)
- [ ] `terraform-plan` workflow re-enabled OR documented as deprecated-in-favor-of-alternative

## Related

- **PR #1692** — trigger event; AUDIT_OVERRIDE trailer cites this gap
- **GAP-692** — env-reference.yaml refactor; Wave 102.8 Bucket B scope that touched `variables.tf`
- **GAP-612** — AWS account suspension; potential correlation with OIDC failure
- **GAP-693** — AWS rebuild SOP (Wave 102.9 milestone — must include this gap's Phase 1+2 closure)
- **GAP-695** — self-test readiness catalog (Tier 0+1 closed Wave 102.8)
- `.claude/rules/post-wave-audit-mandate.md` §2.1 + §2.4 — mandates this audit
- `.claude/rules/agent-aws-access.md` — Tier 1 investigation commands
- `.claude/rules/release-deploy-standard.md` v1.2.0 §9 — agent role in deploy
- `.claude/rules/audit-to-gap-pipeline.md` §2.7 + §2.8 — state-check pattern
- `.claude/skills/quality/ops-readiness-audit/SKILL.md` — Phase 1 audit execution skill
- Wave 102.8 plan + Wave 102.9 outline (defer milestone)

## Log

- **2026-05-21** — Gap filed in response to PR #1692 audit-gate hook 6-violation report. Bucket B `infrastructure/terraform-aws/variables.tf` 3-line change triggered `post-wave-audit-mandate.md` §2.1 ops-readiness audit requirement. Concurrent `terraform-plan` CI OIDC failure (2 consecutive runs across 2 branches) surfaces as parallel infrastructure investigation scope. Both consolidated into single META gap to allow domain-milestone audit deferral per §2.4 (`release-deploy-artifacts` domain key); milestone = Wave 102.9 rebuild SOP closure (BLOCKED on GAP-612 + GAP-694 DONE + GAP-692 Phase 1 DONE — Wave 102.8 closing those prereqs makes 102.9 unblocked). PR #1692 AUDIT_OVERRIDE trailer references this gap.
