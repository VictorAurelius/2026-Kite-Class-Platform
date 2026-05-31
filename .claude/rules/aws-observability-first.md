---
paths:
  - "infrastructure/terraform-aws/**"
---

# AWS Observability Baseline BEFORE Infra Apply

**Priority:** 🔴 CRITICAL — audit baseline must precede non-trivial AWS apply
**Version:** 1.0.1
**Created:** 2026-05-07
**Last-Reviewed:** 2026-05-31
**Reviewer-Approver:** @nguyenvankiet (solo-dev — MINOR self-approve per `rule-change-process.md` §5; new rule with built-in enforcement; no constraint loosening for prior work; migrated from session memory `feedback_aws_observability_first.md` for git-tracked durability)
**Applies to:** Every terraform apply in `infrastructure/terraform-aws/**` that creates ≥10 AWS resources OR any internet-facing service (ALB, EC2 with public subnet, public S3, public RDS), in any AWS account that has CloudTrail OFF (which is the default for new accounts)

---

## 1. The Rule

> **Before applying any non-trivial AWS infrastructure, CloudTrail multi-region trail + audit-log S3 bucket MUST be in place and `IsLogging = true`.** Audit baseline first, infra second.

New AWS accounts have CloudTrail OFF by default. Applying a full Architecture B stack (EC2 + RDS + ALB + secrets + IAM) without CloudTrail leaves the API calls that created those resources UNLOGGED. If credentials later leak, you cannot reconstruct (a) which resources the attacker created/modified, (b) when the attack started, (c) what scope of API access they exercised.

Compliance angle: PDPL 2023 + ISO27001 + SOC2 readiness all require API audit log. Enabling CloudTrail post-hoc still leaves the bootstrap window unaudited.

---

## 2. Required apply order

Phase 1 BETA, AWS account 906286017800 reference order:

| # | Phase | Resources | Why this order |
|---|-------|-----------|----------------|
| 1 | **Phase 2.1** Bootstrap state backend | S3 + DynamoDB lock | Minimal blast radius; chicken-and-egg with terraform state |
| 2 | **Phase 2.2** OIDC IAM role(s) | Read-only initially (terraform-plan) | Identity primitive needed by all downstream apply |
| 3 | **GAP-437 Phase 1 CloudTrail** | Multi-region trail + audit log S3 bucket + bucket policy | **Audit baseline — apply BEFORE Phase 2.3** |
| 4 | **Phase 2.3** Production infrastructure | Full Architecture B (EC2, RDS, ALB, secrets, IAM) | CloudTrail captures every API call from this point forward |
| 5 | **GAP-437 Phase 2** CloudWatch dashboard | Visualization layer | After infra exists; reads CloudTrail data |
| 6 | **GAP-437 Phase 3+** | AWS Config + metric filters + SNS alerts | Builds on CloudTrail + dashboard |

**Hard rule:** step 3 MUST land before step 4. Reversing the order leaves the production-resource creation API calls UNLOGGED.

---

## 3. Targeted apply pattern

CloudTrail-only apply (without touching EC2/RDS/ALB):

```bash
cd infrastructure/terraform-aws
terraform apply \
  -target=aws_cloudtrail.main \
  -target=aws_s3_bucket.cloudtrail_logs \
  -target=aws_s3_bucket_policy.cloudtrail_logs \
  -target=aws_s3_bucket_public_access_block.cloudtrail_logs \
  -target=aws_s3_bucket_versioning.cloudtrail_logs \
  -target=aws_s3_bucket_server_side_encryption_configuration.cloudtrail_logs
```

Verify post-apply:
```bash
aws cloudtrail get-trail-status --name kitehub-main \
  --query 'IsLogging' --output text
# Expected: True
```

If `IsLogging = False` → trail created but not started; run `aws cloudtrail start-logging --name kitehub-main` and re-verify.

---

## 4. Cost guard

CloudTrail management events (first copy) FREE per AWS docs. S3 storage <30MB/month for Phase 1 BETA = $0 within Free Tier 5GB. **No cost-based reason to defer CloudTrail.**

If management-event volume exceeds free tier (heavy multi-region multi-trail setup), data-events tier ~$0.10 per 100k events still cheap relative to incident-investigation cost.

---

## 5. Anti-patterns

| ❌ Don't | ✅ Do |
|---|---|
| Run full Phase 2.3 first, "we'll add CloudTrail later" | Targeted CloudTrail apply (§3) before Phase 2.3 |
| Use AWS Config without CloudTrail | Config consumes CloudTrail data; trail must exist first |
| Trust account-level GuardDuty as audit substitute | GuardDuty is threat detection, not audit log |
| Skip CloudTrail "because we have CloudWatch logs on EC2" | EC2 logs ≠ AWS API audit log; different scope |
| Defer trail because "Free Tier might run out" | First copy free; storage <$0 Phase 1 BETA |
| Apply CloudTrail in same apply as production infra | Targeted apply isolates the audit baseline; production-apply API calls then logged from second one |

---

## 6. Decision flow

Before any terraform apply touching `infrastructure/terraform-aws/**`:

1. **Does this apply create ≥10 AWS resources OR any internet-facing service?** If NO → rule N/A (small/internal-only apply).
2. **Is CloudTrail `IsLogging = true` on the target account?** Verify: `aws cloudtrail describe-trails --query 'trailList[*].Name' --output text` then `aws cloudtrail get-trail-status --name <trail>`. If YES → proceed to apply.
3. **If NO →** STOP. Run targeted CloudTrail apply (§3) first. Re-verify. Then resume planned apply.

No "we'll add CloudTrail next sprint" — gap between rule violations and audit-baseline existence is the exposure window this rule prevents.

---

## 7. Enforcement

### 7.1 Reviewer manual on terraform-aws PRs (active now)

Pre-merge PR review for any diff touching `infrastructure/terraform-aws/**` that adds production resources: reviewer asks "is CloudTrail already in place on the target account?" and confirms via `aws cloudtrail get-trail-status` output cited in PR description or commit body.

### 7.2 Memory auto-load (per-session)

Memory entry `feedback_aws_observability_first.md` (now a pointer to this rule) loads at session start, reminding Claude before any AWS apply planning.

### 7.3 Cross-link to release-deploy-standard.md §9

`release-deploy-standard.md` §9 (Claude agent role in deploy) — agent role "Deploy execution" SKIP for human; this rule extends with the pre-apply ordering requirement that the human executes.

### 7.4 Audit gate (deferred)

Future enhancement — `audit-gate.py` AUDIT_RULES rule that detects PRs adding production resource definitions (RDS, EC2, ALB) without CloudTrail-related resources OR a `CLOUDTRAIL_ALREADY_LIVE: yes` trailer. Tracked as future enhancement; reviewer manual + memory + cross-link sufficient for solo-dev mode.

---

## 8. Self-test (worked example — Phase 2.3 production apply 2026-05-08)

**Scenario:** Account 906286017800, Phase 2.3 about to apply 71 resources (full Architecture B).

**At decision time:** user-flagged miss "tera đã được chạy nhưng không có logs hay dashboard kiểm soát?" surfaced the gap. CloudTrail OFF on account.

**Action taken:** GAP-437 Phase 1 shipped (PR #992) BETWEEN Phase 2.2 (OIDC plan role) and Phase 2.3 (production apply). Trail `kitehub-main` `IsLogging = true` confirmed. Then Phase 2.3 applied → CloudTrail captured every API call including the 71-resource apply itself.

**Verdict:** rule fires correctly on the original incident. The very phasing (2.1 → 2.2 → GAP-437 Phase 1 → 2.3) IS the rule applied. Without rule codified, the natural temptation would have been to combine Phase 1 CloudTrail into Phase 2.3 production apply — losing the audit baseline for the production-creation calls themselves. ✅

---

## 9. Override mechanism

Genuine exception (e.g., apply is rollback / teardown only; no new API calls of audit interest):

```
git commit -m "...
AWS_OBSERVABILITY_FIRST_OVERRIDE: <reason — explain why audit baseline N/A for this apply>"
```

Trailer logged. Pattern frequency >5% per quarter triggers meta-review (rule probably mis-scoped).

---

## 10. Relationship to other rules

- **`release-deploy-standard.md`** §9 — Claude agent role in deploy; this rule mandates the pre-apply ordering that the human executes
- **`agent-aws-access.md`** — production AWS operations; this rule is pre-flight gate
- **`terraform-apply-retry-reconfirm.md`** — sister rule on apply discipline; both bind during Phase 2.3
- **`output-review-mandate.md`** §3 — Ops Readiness audit row covers observability; this rule is the pre-apply enforcement
- **`incident-to-rule-pipeline.md`** — this rule originated from user-flagged miss "tera đã được chạy nhưng không có logs?" → codified per 5-stage pipeline
- **`rule-change-process.md`** §6.5 Enforcement Parity Mandate — rule + memory auto-load + cross-link `release-deploy-standard.md §9` land same PR
- **`feedback_aws_observability_first.md`** (memory pointer to this rule)

---

## 11. Log

- **2026-05-31** (v1.0.1): PATCH — added `paths:` frontmatter per `context-budget-mandate.md` §3.2 (rule was always-load, violating §3.2 size-gate ≥1k tokens requires path-scope/justification/hook). Scope matches rule's own **Applies to** — no behavior change (rule still fires when relevant files touched); removes ~9k chars from base session context. Part of Wave meta context-budget rule-scoping batch 2026-05-31. Reviewer: @nguyenvankiet (solo-dev PATCH self-approve per §5 — path-scope correction, no constraint loosening).

- **2026-05-07 (v1.0.0):** Migrated from session memory `feedback_aws_observability_first.md` per user request "memory persistence strategy = migrate to .claude/rules/ for git-tracked durability". Original incident: 2026-05-08 user-flagged "tera đã được chạy nhưng không có logs hay dashboard kiểm soát?" caught the missing audit baseline before Phase 2.3 production apply on account 906286017800. Recovery: GAP-437 Phase 1 shipped (PR #992) between Phase 2.2 and Phase 2.3 → trail `kitehub-main` `IsLogging=true` → Phase 2.3 71-resource apply fully captured. Reviewer: @nguyenvankiet (solo-dev MINOR self-approve per §5). Enforcement: reviewer manual + memory auto-load + cross-link to `release-deploy-standard.md §9` now; audit-gate.py rule deferred. Future scope: GAP-437 Phase 3 (AWS Config drift tracking) + Phase 4 (CloudTrail metric filters → SNS alerts on root account use, failed IAM, etc.) extend this rule's coverage.
