---
paths:
  - "infrastructure/terraform-aws/**"
  - "infrastructure/terraform-oracle/**"
  - ".github/workflows/**"
  - "scripts/aws/**"
---

# Pre-Mutation State-Check — investigate before applying production changes

**Priority:** 🔴 CRITICAL — production mutation discipline
**Version:** 1.2.1
**Created:** 2026-05-12
**Last-Reviewed:** 2026-05-31
**Reviewer-Approver:** @nguyenvankiet (solo-dev — MINOR self-approve per `rule-change-process.md` §5; new rule with built-in enforcement (PR template + reviewer-checklist + memory + worked self-test on Wave 64 cutover) per §6.5 Enforcement Parity Mandate; no constraint loosening — adds previously-uncovered pre-mutation investigation log mandate)
**Applies to:** Every production-grade mutation operation — `terraform apply` (whether via workflow_dispatch or local), `aws acm import-certificate`, `aws ses verify-*`, `aws iam create-*`, AWS Secrets Manager rotate, Cloudflare DNS POST/PATCH/DELETE on production zones, GitHub Variable/Secret create/update on `production` environment, Kubernetes `kubectl apply` against prod cluster

---

## 1. The Rule

> **Trước mọi mutation op trên production-grade infrastructure, agent PHẢI:**
> 1. **Read current state** via Tier 1 read-only commands (per `agent-aws-access.md` §2)
> 2. **Search prior actions** trong `documents/04-quality/audits/` + git history để tránh duplicate / understand drift
> 3. **Document findings** trong audit artifact `documents/04-quality/audits/<category>/YYYY-MM-DD-<topic>.md` **TRƯỚC khi mutation chạy**
> 4. **Audit artifact PHẢI include:** scope + state-check commands run + real-vs-phantom analysis của planned changes + prior actions verified + recommendation/decision

This closes the gap that `audit-to-gap-pipeline.md` §2.5/§2.6/§2.7/§2.8 covers state-check for GAPS / WAVE-PLANS / DECISION-DOCS / FIX-TIME, but NOT for DEPLOY/MUTATION ops. Per `agent-aws-access.md` §5, **verification sessions** require logging artifacts — but **mutation sessions** had no equivalent pre-mutation audit mandate until this rule.

User-flagged 2026-05-12 during Wave 64 cutover: "thao tác deploy cũng giống như fix gaps, phải lưu logs và state check chứ?" — yes, same discipline applies.

---

## 1.5 Terraform-specific workflow (mandatory when touching `infrastructure/terraform-aws/**` or `infrastructure/terraform-oracle/**`)

Added v1.1.0 sau user-flagged meta-gap 2026-05-12 trong Wave 64 Step F: 3 cascading IAM bugs (tag mismatch + missing perm + secret prefix mismatch) should have been caught in 1 review pass instead of 2+ retry cycles. Per `release-fix-retry-budget.md` §3 — retry #2 from same gate = redesign trigger; for terraform that means structured cross-reference review BEFORE apply.

**Mandatory workflow when editing any `.tf` file:**

1. **Skill-driven review FIRST** — invoke `.claude/skills/devops/terraform-cloud-deploy/SKILL.md` mode "Terraform Review" OR perform equivalent manual cross-reference pass:
   - For IAM policy edits: scan ALL Resource ARN patterns against actual resource names in companion `.tf` files (e.g., `secrets.tf` resource names vs IAM Resource scope; `default_tags` values vs Condition tag values)
   - For variable-driven naming: verify `var.project_name`/`var.environment`/etc. expand to the same value used in resource definitions AND policies
   - For action lists: cross-reference against the actual workflow/script that calls the role (`grep "aws " .github/workflows/<workflow>.yml`, `grep "aws " scripts/<script>.sh`) — every CLI call needs matching IAM action
   - For Condition scopes: verify tag KEY (e.g., `aws:ResourceTag/Project`) and tag VALUE match what `default_tags` sets and what actual resources carry (via `aws ec2 describe-instances --query 'Tags'`)

2. **Pre-apply diff scan** — `terraform plan` output review per `pre-mutation-state-check.md` §3 (already mandatory):
   - "Real vs phantom" classification per resource
   - For every "create/update/replace/destroy" line, cross-reference companion `.tf` files to verify intent

3. **Companion file scan** — when editing IAM, ALSO scan:
   - The workflow YAML that uses the role (e.g., `deploy-production.yml`) for all `aws <verb>` commands
   - The shell scripts the role triggers (e.g., `deploy-prod.sh`) for `aws secretsmanager get-secret-value`, `aws ecr get-login-password`, etc.
   - The corresponding resource `.tf` files (`secrets.tf`, `ec2.tf`, `rds.tf`) for actual resource name patterns

4. **Cross-reference matrix** — document in pre-apply audit artifact (`documents/04-quality/audits/aws-verification/...`):

| IAM Action | Resource pattern in policy | Actual resource name (verified) | Workflow caller | Verdict |
|------------|---------------------------|--------------------------------|-----------------|---------|
| ssm:SendCommand | `*` Condition Project=Kite | EC2 tag Project=Kite (✓) | deploy-production.yml line N | ✅ match |
| secretsmanager:GetSecretValue | `kite/prod/*` (BUG!) | `kitehub/production/*` | deploy-prod.sh line N | ❌ mismatch |
| ec2:DescribeInstances | (missing) | — | ec2_lookup step | ❌ missing action |

Bugs surface in the matrix → fix all in same PR.

5. **Banned shortcut:** "I'll fix one bug, run, see what next bug surfaces" — that's retry-cycle anti-pattern. Catch ALL bugs in one review pass via matrix.

---

## 2. What counts as "production-grade mutation" (in scope)

In scope (rule applies — pre-mutation audit log MANDATORY):

| Op class | Examples |
|----------|----------|
| Terraform apply | Any `terraform apply` on `infrastructure/terraform-aws/**` or `infrastructure/terraform-oracle/**`, whether via workflow_dispatch or local |
| AWS IAM mutations | `create-role` / `create-policy` / `attach-role-policy` / `update-assume-role-policy` |
| AWS ACM | `import-certificate` / `delete-certificate` |
| AWS Secrets Manager | `create-secret` / `put-secret-value` / `rotate-secret` |
| AWS SES | `verify-domain-identity` / `verify-domain-dkim` / `update-account-sending-enabled` |
| AWS RDS | `create-db-instance` / `modify-db-instance` / `delete-db-instance` |
| AWS ECR | `delete-repository` (`create-repository` exempt — additive only) |
| Cloudflare DNS | POST/PATCH/DELETE on `kitehub.me` zone (or any production zone) |
| Cloudflare SSL/Zone settings | PATCH `/zones/{id}/settings/ssl` / `always_use_https` / etc. |
| GitHub Variables/Secrets | `gh variable set` / `gh secret set` on `production` environment |
| Kubernetes prod | `kubectl apply` / `kubectl delete` on production namespace |

Out of scope (rule does NOT apply — but other rules may):

| Op class | Why exempt |
|----------|-----------|
| Tier 1 read-only ops | `describe-*` / `list-*` / `get-*` (per `agent-aws-access.md` §2.1 allowlist) |
| Dev/local environments | docker-compose dev stack, local k8s (kind/minikube) |
| Repo-local file edits | Markdown docs, code files, configs not deployed |
| GitHub Actions workflow edits | Covered by PR review + `release-deploy-standard.md` §3 |
| Dependabot AUTO PRs | Automated routine maintenance, not mutation |
| Rollback ops triggered AFTER incident | `terraform-apply-retry-reconfirm.md` + rollback runbook take precedence |

---

## 3. Required artifact structure

Audit artifact MUST live under `documents/04-quality/audits/<category>/YYYY-MM-DD-<topic>.md` where `<category>` matches:

| Category | When |
|----------|------|
| `aws-verification/` | AWS terraform apply, AWS CLI mutation |
| `cloudflare-verification/` | Cloudflare API mutations (new category — create if needed) |
| `infrastructure-verification/` | Cross-vendor or unclassified production mutation |

### Required sections

```markdown
---
title: AWS Verification — <topic>
status: complete
created: YYYY-MM-DD
phase: <wave-name or release-phase>
wave: <NN>
gaps: [GAP-XXX, GAP-YYY]
---

# AWS Verification Report — <topic>

## Scope

<What mutation is about to happen, why, which rules apply>

## Commands run (Tier 1 read-only per `agent-aws-access.md` §2.1)

```bash
<list every read-only command + brief purpose>
```

## Findings

### Real changes (must verify intent)

| # | Resource | Action | Root cause | Risk |
|---|----------|--------|-----------|------|
| 1 | <name> | create/update/replace/destroy | <why> | <impact> |

### Phantom updates (no real change — terraform state metadata refresh)

| Resource | Why phantom |
|----------|-------------|
| <name> | <explain — e.g. lifecycle ignore_changes, hidden attributes> |

### Verdict

<Real changes intentional/acceptable? Phantom changes non-functional? Production data at risk?>

## Prior actions verified (per `audit-to-gap-pipeline.md` §2.8 — avoid duplicate work)

| Action | When | Where verified |
|--------|------|----------------|
| <prior action> | <date> | <audit doc or git ref> |

## Pending (this op)

| Action | Owner | Notes |
|--------|-------|-------|
| <op> | <user/agent> | <details> |

## Recommendations

1. <Apply / Hold / Investigate further>
2. <Post-mutation verification commands>
3. <Watch-for items>

## References

- Workflow run / PR / commit links
- Related GAPs
- Rules applied
```

### Banned shortcuts

- ❌ "I'll write the audit after apply" — must exist BEFORE mutation runs
- ❌ "Small change, skip audit" — if it's Tier 3 mutation per §2, audit required
- ❌ "User already authorized" — authorization ≠ investigation. Audit captures findings, not approval
- ❌ Audit artifact in non-canonical location (PR description, ad-hoc note) — must be repo file
- ❌ "Phantom changes" claim without explaining WHY phantom (state ignore_changes, attribute count, etc.)

### 3.5 Plan-vs-predicted reconciliation (added v1.2.0)

Pre-apply audit (§3) often predicts diff based on **PR scope** alone (e.g., "this PR adds 2 CF Page Rules → expect 2 add / 0 change / 0 destroy"). But real `terraform plan` covers the **entire workspace** including drift from prior un-applied work in other `.tf` files.

> **Before flipping `dry_run=true → false`, the audit's `## Pending` or `## Verdict` section MUST reconcile predicted-diff vs actual plan output. Every line of `terraform plan` (X add / Y change / Z destroy) classified by Wave-source + Intent (Real / Phantom / Backlog-accumulated).**

Required reconciliation table appended before apply:

| Resource | Plan action | Wave-source | Intent | Decision |
|---|---|---|---|---|
| `<addr>` | create/update/replace/destroy | Wave N (PR #M) | Real / Phantom / Backlog | Apply / Defer / Investigate |

Banned: triggering `dry_run=false` when actual summary (e.g. `7 add / 8 change / 4 destroy`) ≠ predicted (e.g. `2 add / 0 change / 0 destroy`) without reconciliation. Mismatch = STOP, write table, then choose:
- (a) Use workflow `targets` input apply only PR-scoped subset (matches prediction)
- (b) Extend audit covering full backlog + user confirm
- (c) Defer apply, file follow-up gap for backlog drift

Origin: 2026-05-16 PR #1437 — audit predicted 2 add (CF Page Rules), actual plan 7/8/4 incl 3 EC2 force-replace from Wave 37 backlog 9 days un-applied. Without reconciliation gate, applying would have triggered unintended EC2 replace.

---

## 4. Concrete examples

See `_examples/pre-mutation-state-check-examples.md` §Concrete examples (4 worked cases: Wave 64 Step E good apply, blind-apply bad case, Cloudflare DNS PATCH good, DNS DELETE bad).

---

## 5. Enforcement (per `rule-change-process.md` §6.5)

### 5.1 PR template checkbox (lands same PR)

Add to `.github/PULL_REQUEST_TEMPLATE.md` Output Review Checklist:

```markdown
- [ ] **Pre-mutation state-check** — if PR triggers production mutation (terraform apply, AWS CLI write, CF API PATCH/DELETE, k8s prod apply), audit artifact under `documents/04-quality/audits/<category>/YYYY-MM-DD-<topic>.md` exists with Scope + Commands + Findings + Prior-actions + Recommendation per `.claude/rules/pre-mutation-state-check.md` §3
```

### 5.2 Memory auto-load

Memory entry `feedback_pre_mutation_state_check.md` (paired same-PR) reminds at session start before any deploy/mutation work begins.

### 5.3 Reviewer-checklist

When reviewing a PR that contains mutation-trigger artifacts (workflow_dispatch invocation, terraform tfvars change, IAM policy file change, etc.), reviewer asks:
- Is there a pre-mutation audit artifact in `documents/04-quality/audits/`?
- Does it cover scope + state-check + prior-actions + verdict?
- If artifact absent → BLOCK pending audit OR ship audit alongside

### 5.4 Override mechanism

Genuine exception (emergency hotfix, regulator deadline, P0 incident):

```
git commit -m "...
PRE_MUTATION_OVERRIDE: <reason — e.g. P0 production incident, audit deferred to post-mortem>
PRE_MUTATION_FOLLOWUP: <link to gap scheduling audit within 48h>"
```

Trailer logged in quarterly retro. Pattern frequency >5% triggers meta-review.

### 5.5 Detector (deferred per `incident-to-rule-pipeline.md` premature-rule guard)

Future enhancement — `audit-gate.py` AUDIT_RULES rule scanning for mutation patterns (`gh workflow run terraform-apply`, `aws.*create-`, `aws.*put-`, etc.) without matching `documents/04-quality/audits/` artifact in same PR. Defer until 2nd recurrence; reviewer-checklist + memory + worked self-test sufficient for v1.0.0.

---

## 6. Anti-patterns

| ❌ Don't | ✅ Do |
|---------|------|
| Apply terraform plan without reading full plan output | Grep "must be replaced" / "destroyed" / "to add" first |
| Skip audit "because I already understand the change" | Audit IS the record — for future-you, reviewer, or next session |
| Mention investigation findings only in chat | Write to audit artifact file — repo-tracked |
| Use generic catch-all audit names like "deploy.md" | Specific topic + date: `2026-05-12-wave-64-pre-apply-plan-investigation.md` |
| Document "11 add 14 change 4 destroy" without per-resource analysis | Per-resource table with real vs phantom + risk |
| Trust dependency on previous audit without re-verify | Each mutation = fresh state-check (even 30min after previous) |
| Pre-mutation audit in same commit as mutation trigger | Audit lands in separate PR or strictly before workflow_dispatch trigger |

---

## 7. Self-test

See `_examples/pre-mutation-state-check-examples.md` §Self-test (Wave 64 Step E worked example — all §3 sections present, rule fires correctly).

---

## 8. Relationship to other rules

- **`audit-to-gap-pipeline.md`** §2.5-§2.8 — state-check for GAP/wave/decision-doc/fix-time. This rule extends pattern to MUTATION ops (deploy/apply).
- **`agent-aws-access.md`** §5 — logging mandate for VERIFICATION sessions. This rule extends to MUTATION sessions (which are higher-stakes).
- **`terraform-apply-retry-reconfirm.md`** — covers RETRY discipline AFTER apply fails. This rule covers PRE-apply investigation BEFORE first apply.
- **`release-deploy-standard.md`** §9 — defines WHO triggers apply (human-only). This rule defines WHAT investigation must precede the trigger.
- **`rule-change-process.md`** §6.5 Enforcement Parity Mandate — rule + memory + PR template + worked self-test all ship same PR (this PR demonstrates).
- **`incident-to-rule-pipeline.md`** — this rule is direct output of user-flagged meta-gap 2026-05-12 via 5-stage pipeline.
- **`gap-done-discipline.md`** — mutation that closes a GAP must produce both audit artifact (this rule) AND gap closure log (gap-done rule).
- **`feedback_pre_mutation_state_check.md`** (memory, paired same-PR).

---

## 9. Log

- **2026-05-31** (v1.2.1): PATCH — added `paths:` frontmatter per `context-budget-mandate.md` §3.2 (rule was always-load, violating §3.2 size-gate ≥1k tokens requires path-scope/justification/hook). Scope matches rule's own **Applies to** — no behavior change (rule still fires when relevant files touched); removes ~19k chars from base session context. Part of Wave meta context-budget rule-scoping batch 2026-05-31. Reviewer: @nguyenvankiet (solo-dev PATCH self-approve per §5 — path-scope correction, no constraint loosening).

- **2026-05-16 (v1.2.0):** MINOR — added §3.5 Plan-vs-predicted reconciliation mandate. Triggered by Wave 86 PR #1437 incident: agent audit predicted `2 add` based on PR scope; actual `terraform plan` returned `7 add / 8 change / 4 destroy` incl 3 EC2 force-replace from Wave 37 backlog un-applied. Without reconciliation gate, applying `dry_run=false` would have triggered unintended EC2 replace. Per `incident-to-rule-pipeline.md` 5-stage: Detect ✓ Classify ✓ (existing §3 audit mandate but no reconciliation rule) Rule+Enforce ✓ (this §3.5 + paired same-PR workflow `targets` input enabling subset apply) Self-Test ✓ (PR #1437 reconciliation: EC2 = Wave 37 backlog / alarms = Wave 85 / Page Rules = Wave 86 / cascade; targeted apply ships PR-scope only) Retro Log ✓. Reviewer: @nguyenvankiet (solo-dev MINOR self-approve per `rule-change-process.md` §5).
- **2026-05-14 (v1.1.1):** PATCH — Wave 76 Bucket E body streamline. §4 Concrete examples + §7 Self-test moved to `_examples/pre-mutation-state-check-examples.md`; body replaced with 1-line stub pointers. No constraint change; content preserved (deferred-load). Reviewer: @nguyenvankiet (solo-dev PATCH self-approve per `rule-change-process.md` §5).
- **2026-05-12 (v1.1.0):** MINOR — added §1.5 Terraform-specific workflow mandate. Triggered by user-flagged meta-gap during Wave 64 Step F deploy retry: "bổ sung đúng workflow khi động đến terraform" — 3 cascading IAM bugs (tag mismatch + missing ec2:DescribeInstances + secret prefix mismatch) shipped in 2+ retry cycles instead of 1 review pass. Per `incident-to-rule-pipeline.md` 5-stage: Detect ✓ (user-flagged) → Classify ✓ (existing §3 audit artifact mandate but no explicit "terraform-review cross-reference matrix" workflow) → Rule+Enforce ✓ (this §1.5 + matrix template + companion file scan mandate paired same-PR with concrete fix) → Self-Test ✓ (matrix applied retroactively to Wave 64 Step F caught all 3 bugs in 1 pass) → Retro Log ✓ (this entry). Reviewer: @nguyenvankiet (solo-dev MINOR self-approve per `rule-change-process.md` §5 — new constraint adds terraform-specific cross-reference workflow, no constraint loosening). Detector deferred per `incident-to-rule-pipeline.md` premature-rule guard ≥7 days.
- **2026-05-12 (v1.0.0):** Rule created. Triggered by user comment during Wave 64 Step E: "thao tác deploy cũng giống như fix gaps, phải lưu logs và state check chứ?" (mid-session, after agent shipped investigation log organically but user flagged that existing rules didn't MANDATE the discipline for deploy ops). Per `incident-to-rule-pipeline.md` 5-stage: Detect ✓ (user-flagged meta-gap during ongoing mutation session) → Classify ✓ (`audit-to-gap-pipeline.md` covers GAP/wave/decision/fix-time state-check; `agent-aws-access.md` covers verification logging; NO rule explicitly mandated pre-mutation audit log) → Rule+Enforce ✓ (this rule + paired same-PR PR template checkbox + memory `feedback_pre_mutation_state_check.md` + Wave 64 investigation log as worked self-test per `rule-change-process.md` §6.5) → Self-Test ✓ (§7 worked example on the originating Wave 64 Step E session — rule fires correctly + investigation written organically matches all §3 sections) → Retro Log ✓ (this entry). Reviewer: @nguyenvankiet (solo-dev MINOR self-approve per §5 — new constraint adding previously-uncovered pre-mutation investigation log mandate, no constraint loosening for prior work; existing audit artifacts grandfathered, rule applies prospectively from this PR). Detector deferred per premature-rule guard ≥7 days.
