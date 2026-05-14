---
paths:
  - "infrastructure/**/*.tf"
  - ".github/workflows/terraform-*.yml"
  - "documents/04-quality/audits/aws-verification/**"
---

# Terraform Apply Retry Requires Re-Confirm

**Priority:** 🟠 MANDATORY — production-apply blast-radius governance
**Version:** 1.0.1
**Created:** 2026-05-07
**Last-Reviewed:** 2026-05-14
**Reviewer-Approver:** @nguyenvankiet (solo-dev — MINOR self-approve per `rule-change-process.md` §5; new rule with built-in enforcement; no constraint loosening for prior work; migrated from session memory `feedback_terraform_apply_retry_reconfirm.md` for git-tracked durability)
**Applies to:** Every `terraform apply` invocation against a production-grade AWS account from this repo (`infrastructure/terraform-aws/**`); applies whenever apply fails mid-run AND the agent intends to fix and re-run

---

## 1. The Rule

> **When `terraform apply` fails mid-run, fix the offending file and re-run ONLY after explicit user re-confirmation.** The first user approval covers ONE apply attempt, not unlimited retries. Each retry is a separate cost-incurring AWS operation; explicit re-confirm caps blast-radius drift.

CLAUDE.md "Executing actions with care" §"Match the scope of your actions to what was actually requested" — user approving an action once does NOT mean approving in all contexts. A failed `apply` followed by `apply` is technically a NEW operation even when branded as "retry of the same thing." The fix changes scope; user should see what changed before re-running.

---

## 2. Why this matters

Each retry:
- Creates real AWS resources (some carry over from previous partial state, some new)
- Bills account immediately (RDS provisioning, ALB hours, Secrets Manager monthly minimum)
- May modify resources differently than the original plan if the fix changes anything else
- Leaves partial state requiring downstream cleanup if next attempt also fails

Silent retry pattern: 3 separate `terraform apply` invocations on 1 explicit user authorization = soft violation of CLAUDE.md "Executing actions with care."

---

## 3. Required interaction pattern

When apply fails mid-run, the agent MUST present:

```
Apply failed at <resource>. Error: <message>.
Proposed fix: <diff or file change>.
Re-apply will:
  - Retry <N> resources from previous plan
  - Add <M> new resources from fix
  - <any new modifications from fix>
Confirm re-apply?
```

Use `AskUserQuestion` (or equivalent confirmation primitive). User options:
- ✅ Approve re-run with the fix
- ⏸️ Pause to review fix before deciding
- 🔄 Request alternative fix (different approach)
- ❌ Abort and clean up partial state

Agent does NOT proceed to second `terraform apply` until explicit approve received.

---

## 4. Anti-patterns

| ❌ Don't | ✅ Do |
|---|---|
| "Fix prepared, re-running" — silent retry | Present diff + impact + AskUserQuestion before re-apply |
| "Same operation user already approved" | Fix changes scope; re-confirm required |
| Multiple `apply` calls in same Bash chain via `&&` | Single `apply` per Bash invocation; user confirms between |
| Treat partial-state recovery as same-scope as original apply | Partial state is its own scope — needs explicit confirmation |
| Re-apply on transient network error WITHOUT file change AND assume genuine same-op exception | Even transient retries warrant a 1-line "transient — re-run?" check unless user pre-authorized retries |

---

## 5. Allowed exceptions (genuine same-operation)

| Case | Why exempt | Notes |
|------|-----------|-------|
| Transient network error WITH NO file change | Genuinely the same operation | Still warrants 1-line confirm if more than 1 retry |
| Terraform-internal retry (e.g., `aws_db_instance` polling for status) | Not a real apply retry; terraform handles | No agent action needed |
| User pre-authorized "retry up to N times on transient" | Explicit blanket approval | Agent must cite the pre-authorization |
| Read-only `terraform plan` re-run | No state mutation | Plan is idempotent; re-run freely |

---

## 6. Decision flow

When `terraform apply` exits non-zero:

1. **Did the apply complete some resources before failing?** Check terraform output / state. If YES → partial state exists; downstream cleanup may be needed.
2. **What is the proximate cause?** Read error message. Categories: (a) provider-level rejection (e.g., AWS API error like SG ASCII), (b) terraform-internal error, (c) transient network, (d) credential / auth.
3. **Does the fix require a code change?** If YES → §3 re-confirm pattern (present diff + AskUserQuestion). If NO (transient retry) → §5 exception applies.
4. **Was the fix already pre-authorized in scope?** Almost never; default to §3.
5. **Run re-apply only after user explicit ✅.**

---

## 7. Enforcement

### 7.1 Reviewer manual + retro audit

Quarterly retro: review last 90 days of `terraform apply` invocations (from session logs / commit history). For each apply that ran ≥2 times within 1 hour, verify (a) re-confirm was sought OR (b) §5 exception applies. Pattern of silent retries → file follow-up gap referencing this rule.

### 7.2 Memory auto-load (per-session)

Memory entry `feedback_terraform_apply_retry_reconfirm.md` (now a pointer to this rule) loads at session start, reminding Claude before any terraform apply planning.

### 7.3 Cross-link to `agent-aws-access.md` §4 (already enforces)

`agent-aws-access.md` §4 currently cites the memory entry; that section continues to enforce the AWS-access dimension. This rule is the canonical source for the apply-retry behavior; `agent-aws-access.md` cross-link OK and remains valid.

### 7.4 Hook (deferred)

Future enhancement — Bash hook detecting consecutive `terraform apply` invocations in same session without intervening AskUserQuestion. Tracked as future enhancement; reviewer manual + memory + cross-link sufficient for solo-dev mode pending recurrence.

---

## 8. Self-test (worked example — Phase 2.3 production apply 2026-05-08)

**Scenario:** Account 906286017800, Phase 2.3 apply (~$30/mo Year 1 cost), user approved via AskUserQuestion ONCE.

**What happened:**
- Apply #1 failed at SG description em-dash (`security-groups.tf:83`) → agent fixed + re-ran WITHOUT asking
- Apply #2 failed at `count[0]` reference in dashboard → agent fixed + re-ran WITHOUT asking
- Apply #3 succeeded
- Net: 3 separate `terraform apply` invocations on 1 explicit authorization

**User retro flag:** "lần chạy tera này có vi phạm rules không chạy lệnh tự do và monitor không kiểm soát được thời gian và output không?"

**Counterfactual with rule:** After Apply #1 failure, agent presents §3 pattern:
> "Apply failed at `aws_security_group.ai_outbound` — AWS API rejected non-ASCII description. Fix: replace em-dash with hyphen in `security-groups.tf:83`. Re-apply will retry ~70 remaining resources. Confirm re-apply?"

User says yes → re-run. After Apply #2 failure, same pattern repeats. Net: 3 applies but 3 explicit confirmations. User retains control of cost/scope blast radius.

**Verdict:** rule fires correctly on the original incident. Self-test PASS — without the rule, 2 of 3 retries proceeded silently. ✅

---

## 9. Override mechanism

User pre-authorizes blanket retry for known-transient issues:

```
User: "If apply fails on transient AWS API rate-limit (err codes Throttling*, RequestLimitExceeded), retry up to 3 times without asking."

Agent: <accepts pre-authorization; cites it on each silent retry>
```

Or commit-trailer for exception classes:

```
git commit -m "...
TERRAFORM_RETRY_PREAPPROVED: <reason and scope — e.g. 'transient throttling, max 3 retries'>"
```

Trailer logged in quarterly retro. Pattern frequency >5% triggers meta-review.

---

## 10. Relationship to other rules

- **CLAUDE.md "Executing actions with care"** §"Match the scope of your actions" — this rule is a concrete instance for terraform apply
- **`agent-action-bias.md`** §3 row 5 — risk gate for destructive/shared-state actions; this rule does NOT override that gate, sharpens it for apply
- **`aws-sg-description-ascii.md`** — pre-apply guard that prevents the most common Phase 2.3 mid-apply failure
- **`aws-observability-first.md`** — pre-apply ordering mandate; both bind during Phase 2.3
- **`agent-aws-access.md`** §4 — already cites this pattern; cross-link OK; this rule is canonical source going forward
- **`release-deploy-standard.md`** §9 — Claude agent role in deploy; "Deploy execution = SKIP for human" — this rule covers the apply-retry sub-loop
- **`incident-to-rule-pipeline.md`** — this rule originated from user-flagged 2026-05-08 retro after Phase 2.3 silent retries
- **`rule-change-process.md`** §6.5 Enforcement Parity Mandate — rule + memory auto-load + cross-link to existing `agent-aws-access.md §4` land same PR
- **`feedback_terraform_apply_retry_reconfirm.md`** (memory pointer to this rule)

---

## 11. Log

- **2026-05-14** (v1.0.1): PATCH — thêm `paths:` frontmatter — Wave 73 miss fix (rule này nằm trong 13 MANDATORY rules wave plan §3 Scope bỏ sót, vẫn auto-load base context dù scope rule có path trigger rõ ràng). PATCH bump per `rule-change-process.md` §5 — additive frontmatter, no constraint change, deferred-load khi no matching file in context. Reviewer: @nguyenvankiet (solo-dev PATCH self-approve). Scope: terraform apply context.
- **2026-05-07 (v1.0.0):** Migrated from session memory `feedback_terraform_apply_retry_reconfirm.md` per user request "memory persistence strategy = migrate to .claude/rules/ for git-tracked durability". Original incident: 2026-05-08 Phase 2.3 production apply on account 906286017800 = 3 separate `terraform apply` invocations on 1 explicit user authorization (apply #1 SG em-dash, apply #2 count[0], apply #3 success). User retro flagged silent-retry pattern as soft violation of CLAUDE.md "Executing actions with care." Reviewer: @nguyenvankiet (solo-dev MINOR self-approve per §5). Enforcement: reviewer manual + memory auto-load + cross-link to `agent-aws-access.md §4` (which already cites the pattern) now; consecutive-apply hook deferred. Note: `agent-aws-access.md §4` is unchanged in this PR (PR scope avoidance per task spec); this rule becomes canonical source going forward, cross-link from §4 remains valid.
