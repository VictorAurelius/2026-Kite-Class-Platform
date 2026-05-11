---
title: Wave 63 — Rollback workflow (GAP-477 P1)
status: complete
created: 2026-05-11
updated: 2026-05-11
waves: [63]
gaps: [GAP-477]
---

# Wave 63 — Rollback workflow (GAP-477)

**Goal:** Production rollback workflow shipped, IAM least-priv OIDC role provisioned, smoke-rollback-cycle.sh wired to real workflow → unblocks beta-launch rollback readiness per `release-deploy-standard.md` §3.1.
**Trigger:** GAP-477 P1 filed Wave 62 closure. Sub-6 of GAP-475 PARTIAL pending this workflow. Beta tenant invite gated on rollback readiness.
**Estimated wall-clock:** ~2h serial → 3 buckets parallel ~1h longest bucket.

---

## 1. Brainstorm (5-10 min)

**Q1 (alignment):** Phase 1 BETA cutover step 5 beta tenant onboarding requires rollback runbook + workflow tested. All 5 personas affected indirectly (rollback = data preservation gate). Production blast-radius highest of any Wave 62-63 deliverable.

**Q2 (trade-offs):**
- Considered: ECR image retag approach (rejected — k8s deployment update is cleaner for K8s stack) → actually KiteHub Phase 1 BETA = EC2 + ECS-style, NOT K8s yet → use ECS task definition rollback OR docker-compose pull-and-restart pattern
- Considered: single workflow apply role (rejected — least-priv: rollback role narrower than apply role per `agent-aws-access.md` §3 Tier 3 spec)
- Considered: skip dry-run path (rejected — production smoke needs dry-run before real execute per `release-deploy-standard.md` §4.3)

**Q3 (risks):**
- IAM role conflict with existing apply role → use distinct `rollback-role` name + narrower policy
- Workflow_dispatch `confirm=APPLY` UX friction → mirror existing `tier-3-cutover.yml` pattern (precedent from Wave 35)
- TTR measurement may exceed 10min on cold-stack → document expectation in runbook

---

## 2. Task Breakdown

| Bucket | Scope | Owner | Effort | Disjoint? |
|--------|-------|-------|--------|-----------|
| A | terraform IAM rollback role + `.github/workflows/rollback.yml` | bg-agent | ~1h | ✅ `infrastructure/terraform-aws/` + `.github/workflows/` (atomic infra) |
| B | `scripts/smoke-rollback-cycle.sh` update — remove [DEFER] path | bg-agent | ~30min | ✅ `scripts/smoke-rollback-cycle.sh` only |
| C | Docs cross-links — runbook §8 + `release-deploy-standard.md` §4.3 | bg-agent | ~30min | ✅ `documents/05-guides/operations/incident-response-runbook.md` + `.claude/rules/release-deploy-standard.md` |

Disjoint check: A touches terraform + workflow (atomic), B touches script, C touches docs. Zero file overlap.

---

## 3. Scope

**Stake tier:** HIGH (production rollback infrastructure + IAM least-priv + OIDC) → model: **Opus full** per `feedback_sonnet_baseline_context_thrash.md`
**Cross-layer?:** NO (infra + script + docs, no FE+BE consumer pair) → skip Bucket 0 Foundation

| # | Bucket | Scope | Priority | Files | Spawn order |
|:-:|--------|-------|:--------:|-------|:-----------:|
| 1 | **A** | IAM rollback role + workflow YAML | 🟠 P1 | `infrastructure/terraform-aws/iam.tf` (extend) + `.github/workflows/rollback.yml` (new) | parallel |
| 2 | **B** | smoke-rollback-cycle.sh wire-up | 🟠 P1 | `scripts/smoke-rollback-cycle.sh` | parallel |
| 3 | **C** | Docs cross-links | 🟡 P2 | `documents/05-guides/operations/incident-response-runbook.md` + `.claude/rules/release-deploy-standard.md` | parallel |

### Bucket A — terraform IAM rollback role + workflow YAML

- Files (RELATIVE):
  - `infrastructure/terraform-aws/iam.tf` (extend with `aws_iam_role.rollback` + policy)
  - `.github/workflows/rollback.yml` (new)
- IAM role design (least-priv):
  - Trust policy: GitHub OIDC, repo `VictorAurelius/2026-Kite-Class-Platform`, branch `main` only
  - Permissions: ECR PullImage + ECS UpdateService/DescribeServices/DescribeTaskDefinition + EC2 DescribeInstances (read), CloudWatch PutMetricData (for TTR metric) — verify ECS vs EC2 stack architecture first via `cat infrastructure/terraform-aws/*.tf | grep -E "aws_ecs|aws_ec2_instance|aws_launch_template"`
  - NO ALB modify, NO Route53 modify, NO IAM modify (least-priv vs apply role)
- Workflow design:
  - `workflow_dispatch` with inputs: `target_sha` (required, validated regex `^[a-f0-9]{7,40}$`), `confirm` (required, must equal "APPLY" verbatim)
  - Steps:
    1. Validate inputs (`if: ${{ inputs.confirm == 'APPLY' }}`)
    2. Assume rollback role via OIDC
    3. Validate `target_sha` exists in main + age <30 days
    4. Retag previous image: `aws ecr batch-get-image` + `put-image` for `latest` tag
    5. Trigger redeploy (ECS update-service force-new-deployment OR EC2 SSM run-command)
    6. Wait health probes via curl polling smoke endpoint (max 5min)
    7. Post status summary to GitHub workflow summary
  - Environment: `production` (GitHub environment with required reviewers gate)
- Acceptance:
  - Workflow `bash -n` / `actionlint` clean
  - Terraform `terraform fmt -check` clean
  - `terraform plan` runs clean against current state (no apply in this PR)
  - Workflow shows up in GitHub Actions list post-merge

### Bucket B — smoke-rollback-cycle.sh wire-up

- Files: `scripts/smoke-rollback-cycle.sh`
- Changes:
  - Remove `[DEFER]` path in `trigger_rollback()` — replace with real `gh workflow run rollback.yml -f target_sha=<sha> -f confirm=APPLY`
  - `workflow_exists()` still useful but should now always return true post-Bucket A
  - Update `--execute` flag handling to poll `gh run watch` after dispatch
  - JSON report adds `workflow_run_id` field for traceability
- Acceptance:
  - `bash -n && shellcheck` clean
  - `bash scripts/smoke-rollback-cycle.sh --dry-run` exit 0 + valid JSON report
  - `--execute` path documented but actual exec deferred to user (per `agent-aws-access.md` Tier 3)

### Bucket C — Docs cross-links

- Files:
  - `documents/05-guides/operations/incident-response-runbook.md` §8 — flip from "[DEFER]" note to "rollback.yml available since Wave 63"; document the `--execute` cadence (quarterly during maintenance window per `release-deploy-standard.md` §4.3)
  - `.claude/rules/release-deploy-standard.md` §4.3 — add row "Rollback workflow: `.github/workflows/rollback.yml`" to per-bump-type matrix; cross-link to `smoke-rollback-cycle.sh`
- Acceptance:
  - Both files updated; markdown lint clean (no broken links)
  - Cross-references resolve to actual paths

---

## 4. State-Check Evidence

| Symbol | Type | Verification command | Evidence | Verdict |
|--------|------|----------------------|----------|---------|
| `.github/workflows/rollback.yml` | Workflow | `ls .github/workflows/rollback.yml` | not found | 🆕 to-be-created (Bucket A) |
| `infrastructure/terraform-aws/iam.tf` | Terraform | `ls infrastructure/terraform-aws/iam.tf` | TBD by agent | ✅ exists (Bucket A extends) — verify or 🆕 if absent |
| `scripts/smoke-rollback-cycle.sh` | Script | `ls scripts/smoke-rollback-cycle.sh` | shipped Wave 62 #1185 | ✅ exists (Bucket B updates) |
| `documents/05-guides/operations/incident-response-runbook.md` | Runbook | `ls documents/05-guides/operations/incident-response-runbook.md` | exists, §8 added Wave 62 #1185 | ✅ exists (Bucket C extends §8) |
| `.claude/rules/release-deploy-standard.md` | Rule | `ls .claude/rules/release-deploy-standard.md` | exists | ✅ exists (Bucket C extends §4.3) |
| ECS vs EC2 stack architecture | Infra | `grep -E "aws_ecs\|aws_ec2_instance\|aws_launch_template" infrastructure/terraform-aws/*.tf` | TBD by Bucket A | ✅ verify-at-exec |
| `aws_iam_openid_connect_provider` (existing OIDC) | Terraform | `grep -rn "openid_connect_provider" infrastructure/terraform-aws/` | TBD verify by Bucket A | ✅ exists (Wave 37 Bucket B per release-deploy-standard.md §9) |

---

## 5. Verification Gates (per bucket)

| Bucket | Local verify command | CI gate |
|--------|---------------------|---------|
| A | `cd infrastructure/terraform-aws && terraform fmt -check && terraform init -backend-config=backend.config && terraform validate` + `actionlint .github/workflows/rollback.yml` | Terraform fmt + actionlint CI |
| B | `bash -n scripts/smoke-rollback-cycle.sh && shellcheck scripts/smoke-rollback-cycle.sh && bash scripts/smoke-rollback-cycle.sh --dry-run` | ShellCheck CI |
| C | Markdown links visually verified; no CI gate needed | None |

---

## 6. Agent Spawn Pattern

Per `feedback_parallel_agent_strategy.md` + `agent-background-spawn-default.md`:
- 3 buckets spawned `run_in_background: true` + `isolation: worktree`
- Model: **Opus full** (HIGH-stakes production infrastructure) per `feedback_sonnet_baseline_context_thrash.md`
- RELATIVE paths in agent prompts per `feedback_worktree_absolute_path_contamination.md`
- Bucket A is most complex (terraform + workflow) — give detailed IAM policy + workflow template

---

## 7. Closure Protocol

- Each bucket PR updates GAP-477 Log + completion_pct in CSV
- ROADMAP §🚀 Next Action updated in closure PR
- Wave plan `status: complete` flip in closure PR
- `wave-history.jsonl` append
- GAP-475 Sub-6 AC re-checked + GAP-475 completion_pct bump (75 → 90% if Sub-6 fully unblocked)
- Worktree cleanup via `bash scripts/prune-merged-worktrees.sh --yes`
- **`## Release Plan Progress` section** — Wave 63 contribution unblocks step 5 beta tenant onboarding rollback gate

---

## 8. Log

- **2026-05-11** (draft): Plan created. GAP-477 decomposed → 3 parallel buckets. Stake HIGH → Opus full. ECS-vs-EC2 verification deferred to Bucket A state-check at exec.
- **2026-05-11** (complete): Wave SHIPPED. 3 parallel Opus-full agents → PRs #1188 (Bucket A IAM+workflow, +383 LOC) + #1189 (Bucket B script wire-up, +21 net) + #1190 (Bucket C docs, +67 net). Stack architecture chosen at state-check: EC2+SSM RunCommand (zero ECS resources found in terraform). IAM trust policy scoped to GitHub Environment `production` per least-priv design. Workflow 3-job pattern (validate / rollback / notify) mirroring `deploy-production.yml`. GAP-477 OPEN → PARTIAL 85%. Remaining: user-action `terraform apply` + GitHub Environment config + first live `--execute` for TTR baseline. Streak: 97 consecutive 0-clarification waves.
