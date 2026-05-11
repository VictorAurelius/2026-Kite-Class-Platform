# GAP-449: Terraform-apply workflow_dispatch + revise §9 distinguish 3 cases

**Status:** 🔵 OPEN
**Priority:** 🔴 P0 (blocks Wave 43 verification + future infra applies)
**Domain:** Infrastructure / CI / Governance / Meta-rule
**Found:** 2026-05-08 (Wave 43 closure — user-flagged "tại sao cần rule terraform apply human-only?")
**Affects:** All future production AWS infra applies + Wave 43 GAP-446/447 → DONE blocker

## Problem

Wave 43 ship Terraform changes (scheduler.tf + ec2.tf + cloudwatch.tf) cần `terraform apply` để activate. Existing infra:

| Workflow | Scope |
|---|---|
| `terraform-plan.yml` | PR plan-only, **không apply** |
| `deploy-production.yml` | App deploy SSM + docker-compose, **không terraform apply** |

**Không có terraform-apply workflow** → user phải chạy `terraform apply` LOCAL với admin key:
- Yêu cầu admin access key trên laptop (security risk — exposed key đã pending rotation)
- No audit trail trên GitHub Actions
- No state-locked CI environment guarantee
- No reproducibility (laptop env dependent)

## Root Cause — Rule §9 over-restrictive

Existing rule `release-deploy-standard.md` §9 + `agent-aws-access.md` §4.3 ban "terraform apply" without distinguishing:

| Case | Existing rule | Should be |
|---|---|---|
| Auto-apply on git push | ❌ BAN | ✅ Vẫn BAN |
| Agent-spawned `terraform apply` autonomously | ❌ BAN | ✅ Vẫn BAN |
| Workflow_dispatch + confirm input + human-click | ❌ BAN (theo letter) | ⚠️ **Should ALLOW** |

Rule conflates "agent autonomy" với "any CI apply". `workflow_dispatch` + confirm "APPLY" verbatim + human-click = **explicit human action with audit trail** — same accountability as local apply, with better security (OIDC ephemeral creds vs static admin key on laptop).

## Proposed Fix

### Phase 1 — Rule revision (Bucket A)

**Revise** `release-deploy-standard.md` §9 (+ matrix update):

```markdown
| Phase | Agent role | Reason |
|---|---|---|
| Deploy execution | ❌ AUTONOMOUS — auto-apply on push BANNED. ❌ Agent-spawned apply BANNED. ✅ Human-click `workflow_dispatch` + confirm input + narrow OIDC role ALLOWED — same human accountability with better security than local admin key | Production blast radius high; human-click checkpoint preserved; CI audit trail; ephemeral OIDC > static admin key |
```

**Cross-link** `agent-aws-access.md` §4.3 update — clarify Tier 3 ban applies to AGENT-INITIATED apply, not user-triggered workflow_dispatch.

### Phase 2 — Workflow scaffold (Bucket B)

**Create** `.github/workflows/terraform-apply.yml`:
- Trigger: `workflow_dispatch` only (no push/PR)
- Inputs: `version` (description) + `confirm` (must equal "APPLY" verbatim) + `dry_run` (default true — plan-only mode)
- Steps: checkout → setup terraform → OIDC auth (new apply role) → init → plan (always) → apply (only if dry_run=false + confirm validated)
- Environment: `production` (GitHub environment với optional manual approval gate)

**Amend** `infrastructure/terraform-aws/iam.tf`:
- New `aws_iam_role "github_terraform_apply"` với:
  - assume_role_policy condition: `repo:${var.github_repo}:environment:production` (require GitHub environment for stricter gate)
  - Attach `PowerUserAccess` + IAM management perms (terraform manages IAM)
  - S3 + DynamoDB state access (same as plan role)
- Output `github_terraform_apply_role_arn`

### Phase 3 — Bootstrap runbook (Bucket C)

**Create** `documents/05-guides/deploy/terraform-apply-bootstrap-runbook.md`:
- One-time chicken-and-egg bootstrap: provision new IAM role qua local apply (admin key)
- Sau bootstrap: GitHub Variable `AWS_TERRAFORM_APPLY_ROLE_ARN` set → `gh workflow run terraform-apply.yml`
- Verify post-bootstrap: workflow_dispatch tested với dry_run=true trước khi real apply
- Wave 43 first apply via workflow (scheduler + right-size + cloudwatch)
- Verification artifacts per `agent-aws-access.md` §5

## Acceptance Criteria

- [ ] **Phase 1 (Bucket A):** `release-deploy-standard.md` §9 revised distinguishing 3 cases; `agent-aws-access.md` §4.3 cross-link updated
- [ ] **Phase 2 (Bucket B):** `.github/workflows/terraform-apply.yml` shipped với confirm-input gate + OIDC + dry_run mode
- [ ] **Phase 2 (Bucket B):** `infrastructure/terraform-aws/iam.tf` thêm `github_terraform_apply` role với PowerUserAccess + IAM perms + state access; output ARN
- [ ] **Phase 3 (Bucket C):** `documents/05-guides/deploy/terraform-apply-bootstrap-runbook.md` runbook chi tiết step-by-step
- [ ] **Self-test §6.5 Enforcement Parity:** rule revision + workflow + runbook ship cùng PR (Wave 44 closure PR)
- [ ] **Bootstrap (post-merge user action):** local `terraform apply` lần 1 → provision IAM apply role + Wave 43 changes (scheduler + right-size + cloudwatch) cùng lúc
- [ ] **Verify (post-bootstrap):** GitHub Variable `AWS_TERRAFORM_APPLY_ROLE_ARN` set → `gh workflow run terraform-apply.yml -f confirm=APPLY -f dry_run=true` returns plan; `dry_run=false` applies clean
- [ ] **Wave 43 verify artifacts:** 2× files `documents/04-quality/audits/aws-verification/2026-05-XX-wave-43-{scheduler,right-size}.md`
- [ ] **Flip GAP-446/447 → 🟢 DONE** sau verify

## Out of scope (defer)

- Per-environment apply roles (staging vs prod) — Phase 2 enhancement
- Manual approval gate via GitHub Environment protection rules — recommended Phase 2
- Drift detection workflow (terraform plan on schedule) — separate gap

## Related

- Parent: Wave 43 closure (#1040) — GAP-446/447 PARTIAL state caused by missing apply mechanism
- Cross-cutting rules: `release-deploy-standard.md` §9 (the rule being revised), `agent-aws-access.md` §4.3 (cross-link), `terraform-apply-retry-reconfirm.md` (still applies — workflow_dispatch retry needs re-confirm), `admin-merge-discipline.md` (workflow itself = code change → CI gate)
- ADR-015 AWS Agent Plugins evaluation (defer Q3 2026 — this is parallel: human-triggered workflow ≠ AI agent autonomy)
- Memory `feedback_terraform_apply_retry_reconfirm.md` (still applies)
- Memory `feedback_terraform_partial_backend_public_repo.md` (workflow uses partial backend config same pattern)

## Log

- **2026-05-08** — OPEN. Filed sau Wave 43 closure user-flagged "tại sao cần rule terraform apply human-only?" — surfaced rule §9 over-restriction conflating 3 cases (auto-apply / agent-apply / workflow_dispatch-apply). Per `incident-to-rule-pipeline.md` 5-stage Stage 3: rule revision + workflow scaffold + bootstrap runbook ship cùng Wave 44 (3 buckets parallel ~25-30min).
- **2026-05-11** — Pattern reuse note (Wave 61 Bucket C): `documents/05-guides/deploy/production-seed-runbook.md` shipped following the same human-executed-mutation pattern this gap is formalizing for terraform apply — runbook explicitly marks stack resume / stop + secrets fetch as USER-EXECUTED per `agent-aws-access.md` §4 Tier 3 ban; agent ships scripts + runbook only. Demonstrates GAP-449's "3 cases" distinction (auto-apply BAN / agent-apply BAN / human-triggered ALLOWED) already governing non-terraform mutation flows in this repo. No AC ticked — GAP-449 scope remains terraform-apply workflow specifically. Cross-ref only.
- **2026-05-11 (Wave 61 Bucket A — workflow_dispatch path eligibility confirmed):** State-check artifact `documents/04-quality/audits/aws-verification/2026-05-11-wave-61-bucket-a-dns-state.md` confirms Tier 3 cutover flow (ACM cert import + ALB HTTPS listener create + SSL strict + Always HTTPS toggle) per `release-1-tier-3-cutover.md` §0.5 hỗ trợ Path Y workflow_dispatch (`.github/workflows/tier-3-cutover.yml` — đã shipped Wave 44+ qua sister bucket). Wave 61 Bucket A scope demonstrates Path Y eligibility for human-triggered mutation workflow per `release-deploy-standard.md` §9 carve-out: human-click + confirm input "APPLY" verbatim + narrow OIDC role = allowed. GAP-449 advances toward 🟢 DONE pending: (1) Wave 43 verification artifacts shipped per AC line "Wave 43 verify artifacts: 2× files documents/04-quality/audits/aws-verification/2026-05-XX-wave-43-{scheduler,right-size}.md"; (2) GAP-446/447 flipped 🟢 DONE post-verify. Wave 61 Bucket A KHÔNG đụng terraform-apply.yml directly (DNS state-check là docs-only); workflow_dispatch path Y reference reinforced trong runbook §0.5.
