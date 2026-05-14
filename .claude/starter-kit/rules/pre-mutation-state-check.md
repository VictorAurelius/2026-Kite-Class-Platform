# Pre-Mutation State-Check — investigate before applying production changes

**Priority:** 🔴 CRITICAL — production mutation discipline
**Version:** 1.1.0
**Created:** 2026-05-12
**Last-Reviewed:** 2026-05-12
**Reviewer-Approver:** @nguyenvankiet (starter-kit upstream maintainer) per §6.5 Enforcement Parity Mandate; no constraint loosening — adds previously-uncovered pre-mutation investigation log mandate)
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
| ssm:SendCommand | `*` Condition Project=the project | EC2 tag Project=the project (✓) | deploy-production.yml line N | ✅ match |
| secretsmanager:GetSecretValue | `<secret-prefix>/prod/*` (BUG!) | `<secret-prefix>/production/*` | deploy-prod.sh line N | ❌ mismatch |
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
| Cloudflare DNS | POST/PATCH/DELETE on `<production-domain>` zone (or any production zone) |
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

---

## 4. Concrete examples

### ✅ GOOD — Wave 64 Step E pre-apply (this PR's worked example)

Before triggering `terraform-apply.yml` workflow_dispatch:
1. Run `gh run download` to get plan output
2. Grep for `must be replaced` / `will be created` / phantom indicators
3. Run AWS describe-instances to verify current state
4. Search `documents/04-quality/audits/aws-verification/` for prior apply audits
5. Write `2026-05-12-wave-64-pre-apply-plan-investigation.md` documenting:
   - 11 add (real: HTTPS listener, 2 IAM roles, memory alarm; +cascades)
   - 14 change (real: 1 (lb_listener.http redirect flip); phantom: 13 (random_password ignore_changes, db_instance metadata, schedulers))
   - 4 destroy (real: 2 EC2 AMI bump pre-launch acceptable + 2 cascading)
6. Verdict: safe to apply (pre-launch, no data, all changes desired/beneficial)
7. THEN user triggers `dry_run=false`

### ❌ BAD — apply without investigation

```
agent: "Plan shows 11 add 14 change 4 destroy. Apply?"
user: "Yes"
→ agent triggers workflow_dispatch
```

Risk: drift hidden in "4 destroy" — could be EC2 replacement (data loss on local state), could be IAM role deletion (auth break), could be RDS replacement (DB loss). Without investigation, blind apply.

### ✅ GOOD — Cloudflare DNS PATCH on production

Before `curl PATCH /zones/{id}/dns_records/{id}` to modify existing SPF:
1. GET current record state (verify content + record_id)
2. Search `aws-verification/` audits for prior DNS changes
3. List CF Email Routing rules to confirm SPF still needed for those forwarders
4. Document in audit artifact:
   - Current SPF value, proposed merged value
   - 2 active Email Routing rules depend on `_spf.mx.cloudflare.net`
   - Merge keeps both routing + adds amazonses
5. PATCH

### ❌ BAD — DNS DELETE without state-check

```
agent: "Old SPF record, delete?"
→ DELETE /zones/{id}/dns_records/{id}
```

Risk: didn't check if CF Email Routing depends on it → routing breaks silently.

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

## 7. Self-test (worked example — Wave 64 Step E)

**Scenario:** 2026-05-12 03:48 UTC — Wave 64 cutover Step E. User-triggered terraform-apply workflow_dispatch `dry_run=true` produced plan summary `11 to add, 14 to change, 4 to destroy`. Agent must decide: apply now (dry_run=false) or hold?

**Apply rule §3 mandate:**
1. ✅ Read current state — `aws ec2 describe-instances` confirmed actual IDs
2. ✅ Search prior actions — found `2026-05-08-wave-43-44-bootstrap-apply.md` + `2026-05-08-current-state.md` + `2026-05-11-wave-61-bucket-a-dns-state.md` + GAP-450 investigation logs
3. ✅ Document findings → `documents/04-quality/audits/aws-verification/2026-05-12-wave-64-pre-apply-plan-investigation.md` (this audit)
4. ✅ Sections present: Scope + Commands + Findings (real-vs-phantom 11/14/4 broken down) + Prior actions table (10 items) + Pending table + Recommendations + References

**Verdict:** all §3 sections present + decisions justified. Rule fires correctly. ✅

**Without this rule:** session 2026-05-12 would have run apply with shallow understanding of 4-destroy items + 14-change items, potentially missing the AMI replacement detail OR misinterpreting phantom updates as real rotations.

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

- **2026-05-12 (v1.1.0):** MINOR — added §1.5 Terraform-specific workflow mandate. Triggered by user-flagged meta-gap during Wave 64 Step F deploy retry: "bổ sung đúng workflow khi động đến terraform" — 3 cascading IAM bugs (tag mismatch + missing ec2:DescribeInstances + secret prefix mismatch) shipped in 2+ retry cycles instead of 1 review pass. Per `incident-to-rule-pipeline.md` 5-stage: Detect ✓ (user-flagged) → Classify ✓ (existing §3 audit artifact mandate but no explicit "terraform-review cross-reference matrix" workflow) → Rule+Enforce ✓ (this §1.5 + matrix template + companion file scan mandate paired same-PR with concrete fix) → Self-Test ✓ (matrix applied retroactively to Wave 64 Step F caught all 3 bugs in 1 pass) → Retro Log ✓ (this entry). Reviewer: @nguyenvankiet (solo-dev MINOR self-approve per `rule-change-process.md` §5 — new constraint adds terraform-specific cross-reference workflow, no constraint loosening). Detector deferred per `incident-to-rule-pipeline.md` premature-rule guard ≥7 days.
- **2026-05-12 (v1.0.0):** Rule created. Triggered by user comment during Wave 64 Step E: "thao tác deploy cũng giống như fix gaps, phải lưu logs và state check chứ?" (mid-session, after agent shipped investigation log organically but user flagged that existing rules didn't MANDATE the discipline for deploy ops). Per `incident-to-rule-pipeline.md` 5-stage: Detect ✓ (user-flagged meta-gap during ongoing mutation session) → Classify ✓ (`audit-to-gap-pipeline.md` covers GAP/wave/decision/fix-time state-check; `agent-aws-access.md` covers verification logging; NO rule explicitly mandated pre-mutation audit log) → Rule+Enforce ✓ (this rule + paired same-PR PR template checkbox + memory `feedback_pre_mutation_state_check.md` + Wave 64 investigation log as worked self-test per `rule-change-process.md` §6.5) → Self-Test ✓ (§7 worked example on the originating Wave 64 Step E session — rule fires correctly + investigation written organically matches all §3 sections) → Retro Log ✓ (this entry). Reviewer: @nguyenvankiet (solo-dev MINOR self-approve per §5 — new constraint adding previously-uncovered pre-mutation investigation log mandate, no constraint loosening for prior work; existing audit artifacts grandfathered, rule applies prospectively from this PR). Detector deferred per premature-rule guard ≥7 days.
