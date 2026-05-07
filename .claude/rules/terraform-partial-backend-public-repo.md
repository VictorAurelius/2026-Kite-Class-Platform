# Terraform Partial Backend Config for Public Repos

**Priority:** 🟠 MANDATORY — defense-in-depth against AWS account ID exposure
**Version:** 1.0.0
**Created:** 2026-05-07
**Last-Reviewed:** 2026-05-07
**Reviewer-Approver:** @nguyenvankiet (solo-dev — MINOR self-approve per `rule-change-process.md` §5; new rule with built-in enforcement; no constraint loosening for prior work; migrated from session memory `feedback_terraform_partial_backend_public_repo.md` for git-tracked durability)
**Applies to:** Every `terraform { backend "s3" { ... } }` block in this repo (currently `infrastructure/terraform-aws/backend.tf`); applies whenever the repo is or could become PUBLIC

---

## 1. The Rule

> **In a PUBLIC repo, the terraform S3 backend block MUST use partial config (no hardcoded `bucket = "..."`).** The bucket name (which embeds AWS account ID) is supplied at init-time via gitignored `backend.config` (local) or GitHub Variable `TERRAFORM_STATE_BUCKET` (CI).

Public-repo exposure of AWS account ID is not a credential leak by itself — AWS officially says account IDs are not secrets — but defense-in-depth says minimize exposure. Account ID enables enumeration attacks (IAM user lookup via trust policy guesses, S3 bucket enumeration, support-channel phishing) and accelerates blast radius if a credential leaks.

---

## 2. Pattern

### 2.1 `backend.tf` (committed, partial config)

```hcl
terraform {
  backend "s3" {
    # bucket — supplied via backend.config (gitignored) or CI -backend-config
    key            = "phase-1-beta/terraform.tfstate"
    region         = "ap-southeast-1"
    dynamodb_table = "kitehub-terraform-locks"
    encrypt        = true
  }
}
```

### 2.2 `backend.config.example` (committed, template)

```
# Template — copy to backend.config (gitignored) and fill in bucket name
# bucket = "kitehub-terraform-state-<AWS_ACCOUNT_ID>"
```

### 2.3 `backend.config` (gitignored)

```
bucket = "kitehub-terraform-state-906286017800"
```

### 2.4 `.gitignore` entry

```
infrastructure/terraform-aws/backend.config
```

### 2.5 Local init

```bash
cd infrastructure/terraform-aws
cp backend.config.example backend.config  # first time
# edit backend.config with actual bucket name
terraform init -backend-config=backend.config
```

### 2.6 CI workflow (`.github/workflows/terraform-plan.yml`)

```yaml
- run: |
    terraform init \
      -backend-config="bucket=${{ vars.TERRAFORM_STATE_BUCKET }}"
```

---

## 3. How to apply

### 3.1 New backend setup

1. Create the S3 bucket + DynamoDB lock table (one-time bootstrap, can be hardcoded since bootstrap state is local-only)
2. Add `backend.tf` with partial config (no bucket)
3. Create `backend.config.example` (committed) + `backend.config` (gitignored)
4. Add `.gitignore` entry for `backend.config`
5. Set GitHub repo Variable `TERRAFORM_STATE_BUCKET` for CI
6. Document local init in repo README / runbook

### 3.2 Migrating from hardcoded backend

1. Read current `backend.tf` — note bucket name
2. Save bucket name to `backend.config` (gitignored)
3. Remove `bucket = "..."` line from `backend.tf`
4. Add `.gitignore` entry
5. Run `terraform init -backend-config=backend.config -reconfigure` to re-init
6. Verify state still accessible: `terraform plan` succeeds

---

## 4. Anti-patterns

| ❌ Don't | ✅ Do |
|---|---|
| Hardcode `bucket = "kitehub-terraform-state-906286017800"` in `backend.tf` of a public repo | Partial config — bucket via `backend.config` or CI variable |
| Commit `backend.config` "just to make onboarding easier" | `.gitignore` entry; new contributors copy from `.example` |
| Skip `backend.config.example` template | Template gives onboarders the schema without leaking account ID |
| Hardcode "because the repo is private now" | Repos flip public; partial-config is cheap insurance |
| Use account-ID-bearing names for OTHER resources in tags / outputs visible in plan output | Use abstract names where feasible; CI plan output is also public-visible via PR comments |

---

## 5. When NOT applicable

| Case | Why exempt |
|------|-----------|
| Private repo with no flip-public plan | Hardcoding acceptable; still prefer partial-config as habit |
| Bootstrap state (creates the backend itself) | Bootstrap can use local state; chicken-and-egg requires hardcoded one-time |
| Workspace-per-environment patterns | May need different approach (per-workspace key path); document case-by-case |
| Self-hosted state backend (e.g., Terraform Cloud) | Cloud-managed backends don't expose account ID via config; rule N/A |

---

## 6. Enforcement

### 6.1 Reviewer manual (active now)

Pre-merge PR review for any diff touching `infrastructure/terraform-aws/backend.tf` or `.gitignore`: reviewer confirms (a) `backend.tf` has no `bucket = ...` literal, (b) `backend.config.example` exists as template, (c) `.gitignore` excludes `backend.config`.

### 6.2 Memory auto-load (per-session)

Memory entry `feedback_terraform_partial_backend_public_repo.md` (now a pointer to this rule) loads at session start, reminding Claude before any terraform backend edit.

### 6.3 Visible template (active now)

`backend.config.example` is itself the enforcement artifact — its presence in the repo signals the partial-config pattern to onboarders.

### 6.4 CI guard (deferred)

Future enhancement — `.github/workflows/terraform-plan.yml` step that greps `backend.tf` for `bucket = "..."` literal and fails the build. Tracked as future enhancement; reviewer manual + memory + visible template sufficient for solo-dev mode.

---

## 7. Self-test (worked example — Phase 2.2 OIDC migration 2026-05-08)

**Scenario:** Repo `VictorAurelius/2026-Kite-Class-Platform` is PUBLIC; pre-PR-#990 `backend.tf` had hardcoded `bucket = "kitehub-terraform-state-906286017800"` exposing AWS account ID.

**At decision time:** rule §1 would have flagged hardcoded literal in public repo.

**Outcome without rule:** PR #989 (predecessor with hardcoded backend) was closed; PR #990 introduced partial-config pattern; PR #992 verified end-to-end (terraform-plan ran via OIDC + partial config + GitHub Variable, posted real plan to PR comment).

**Verdict:** rule fires correctly. PR #989 → #990 transition IS the worked self-test of this rule applied to existing state. ✅

---

## 8. Override mechanism

Genuine exception (e.g., repo is being archived, account ID already disclosed elsewhere, infrastructure scheduled for teardown):

```
git commit -m "...
TERRAFORM_BACKEND_PUBLIC_OVERRIDE: <reason — explain why partial-config not feasible>"
```

Trailer logged in quarterly retro. Pattern frequency >5% triggers meta-review.

---

## 9. Relationship to other rules

- **`release-deploy-standard.md`** §3 — production secrets management; this rule covers the terraform-state-backend slice
- **`agent-aws-access.md`** — production AWS operations; this rule pairs with backend access patterns
- **`incident-to-rule-pipeline.md`** — this rule's origin: PR #989 hardcoded backend → user-flagged exposure → PR #990 fix → codified here per 5-stage pipeline
- **`rule-change-process.md`** §6.5 Enforcement Parity Mandate — rule + memory auto-load + visible `backend.config.example` template land same PR
- **`feedback_terraform_partial_backend_public_repo.md`** (memory pointer to this rule)

---

## 10. Log

- **2026-05-07 (v1.0.0):** Migrated from session memory `feedback_terraform_partial_backend_public_repo.md` per user request "memory persistence strategy = migrate to .claude/rules/ for git-tracked durability". Original incident: 2026-05-08 PR #989 closed (hardcoded predecessor exposed account ID `906286017800` in public repo) → PR #990 merged (partial config pattern with `backend.config` gitignored + `backend.config.example` template + GitHub Variable `TERRAFORM_STATE_BUCKET` for CI) → PR #992 verified end-to-end via OIDC + real plan post. Reviewer: @nguyenvankiet (solo-dev MINOR self-approve per §5). Enforcement: reviewer manual + memory auto-load + visible template now; CI guard deferred.
