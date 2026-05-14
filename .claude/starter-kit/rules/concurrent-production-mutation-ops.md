# Concurrent Production Mutation Ops — serialize, never parallelize

**Priority:** 🔴 CRITICAL — prevents production resource state-conflict from concurrent mutations
**Version:** 1.0.0
**Created:** 2026-05-12
**Last-Reviewed:** 2026-05-14
**Reviewer-Approver:** @nguyenvankiet (starter-kit upstream maintainer)
**Applies to:** Every production mutation op (terraform apply / deploy-production.yml / SSM SendCommand / aws CLI write / kubectl apply prod / Cloudflare PATCH-DELETE / GitHub variable set production) — agent OR human triggered

---

## 1. The Rule

> **Never run two or more mutation ops concurrently on the same shared production resource.** Serialize them strictly: op A complete → verify resource healthy → op B start.

This includes seemingly-safe combinations like terraform in-place update + SSM SendCommand on same EC2: AWS implements `user_data` modification as **stop → ModifyInstanceAttribute → start**, killing running SSM commands with `SIGTERM` (exit 143).

Rule scope is intentionally broad — ANY two mutation ops touching same physical resource (EC2 instance / RDS / ECS service / ALB / DNS record / secrets / IAM role) must be serialized.

---

## 2. Why this matters — 2026-05-12 incident

**Timeline (UTC):**
- 07:50:19 — `terraform-apply.yml` triggered (dry_run=false, Bucket E ec2.tf user_data change)
- 07:50:41 — `deploy-production.yml` triggered (staging.10, SSM SendCommand on `i-00505094277deda29`)
- 07:51:12 — SSM command START: deploy-prod.sh begins, ECR login OK, fetch-secrets begins
- 07:51:20 — SSM command **Failed** with `Terminated / exit status 143` after 7.88s
- 07:51:39 — EC2 kc_app new LaunchTime (post-restart)
- 07:51:49 — EC2 kh_backend new LaunchTime (post-restart)
- 07:52:06 — Terraform apply complete (`0 added, 2 changed, 0 destroyed` in-place user_data)

**Root cause:** Terraform `aws_instance.user_data` modification on a RUNNING instance requires `stop → ModifyInstanceAttribute → start`. AWS API does this implicitly. The stop call sent SIGTERM to all running processes including the SSM-launched `deploy-prod.sh` mid-secret-fetch.

**Symptom masking:** `deploy-production.yml` poll loop showed `Status=InProgress` for ~15 attempts (2.5 min) while the SSM command was already terminated at 7s. Lack of CloudWatch output streaming = no early-failure detection. Tooling gap separately covered by `release-fix-retry-budget.md` §4 row "Tooling visibility gap" + GAP-491 follow-up.

**Cost of the miss:**
- 1 wasted SSM command + workflow run (~$0 but ~10 min wall-clock)
- 1 forced cancel + re-trigger cycle
- Confusion + audit overhead (this rule + memory + audit artifact extension)

If rule existed at decision time → I would have run terraform apply FIRST, waited for both EC2 healthy + verified user_data hash updated, THEN triggered deploy. Zero conflict.

---

## 3. Required serialization patterns

### 3.1 Terraform apply touching aws_instance + deploy → serialize

**Trigger:** terraform plan output contains `aws_instance.*.user_data` OR `aws_instance.*.instance_type` OR similar field that requires EC2 stop/start.

**Pattern:**
1. Trigger `terraform-apply.yml` → wait `completed/success`
2. Verify EC2 reached `running` state via `aws ec2 describe-instances --query 'Reservations[].Instances[].State.Name'` — all `running`
3. Verify EC2 user_data hash updated via `terraform state show aws_instance.<name>` if applicable
4. THEN trigger `deploy-production.yml`

**Banned:** triggering both workflows in same minute, even if one says "in-place".

### 3.2 Two terraform apply concurrently

Terraform state lock prevents this for same backend. Still — agent MUST NOT trigger 2nd `terraform-apply.yml` while first is `in_progress`.

### 3.3 Database migration + deploy

Schema migration (Flyway) + container restart with new image can race on DB connection. Serialize: migrate first → verify schema version → deploy.

### 3.4 RDS modify + deploy

`modify-db-instance` reboots RDS. Don't deploy services that connect to that RDS during the reboot window. Wait `DBInstanceStatus=available` after modify.

### 3.5 IAM role-policy update + deploy using that role

IAM eventual consistency window (~10s). Don't trigger deploy that uses the updated role within 10s of IAM change.

### 3.6 ALB/target-group change + deploy

ALB rule change has 15-30s propagation. Don't deploy if deploy verification depends on the rule taking effect immediately.

---

## 4. Decision flow before trigger

Before any 2nd mutation op trigger, agent runs §4 checklist:

1. **List active workflows in progress** via `gh run list --status in_progress` — any active terraform/deploy/migration?
2. **Identify shared resource** — does the queued op touch ANY resource the active op might mutate?
   - EC2 instance ID overlap?
   - RDS instance overlap?
   - IAM role overlap?
   - Same Cloudflare zone?
3. **If overlap detected → STOP.** Wait active op `completed`. Verify resource healthy. Then proceed.
4. **If no overlap → safe to parallel.**

For deploy + terraform pairs: ALWAYS sequential — they touch EC2.

---

## 5. Anti-patterns

| ❌ Don't | ✅ Do |
|---|---|
| Trigger terraform-apply.yml + deploy-production.yml within same minute | Wait terraform `completed`, verify EC2 `running`, THEN deploy |
| Assume "in-place" terraform update = zero disruption | Read terraform docs: some fields require stop/start (user_data, instance_type, root_block_device) |
| Use parallel workflow runs to "save time" | Wall-clock saved is dwarfed by debug + cancel + re-trigger cost when conflict surfaces |
| Trust workflow poll status alone (e.g., SSM `InProgress`) without underlying command verification | Read actual command state via `aws ssm get-command-invocation` Tier 1 read-only |
| Concurrent `aws ssm send-command` on same instance | Serialize SSM commands; AWS allows multiple but inter-command race common |
| Bulk-trigger N workflows for "wave-pack parallelism" on production resources | Wave-pack pattern is for CODE WORK (agent worktrees). Production mutations always serial. |

---

## 6. Decision matrix — common pair concurrency

| Op 1 | Op 2 | Concurrent OK? | Reason |
|------|------|---------------|--------|
| `terraform apply` (aws_instance user_data) | `deploy-production.yml` | ❌ NO | Today's incident — EC2 stop kills SSM |
| `terraform apply` (aws_instance instance_type) | Anything on that EC2 | ❌ NO | Stop→modify→start required |
| `terraform apply` (aws_security_group rule add) | Deploy needing that SG rule | ⚠️ MAYBE | Race possible — wait apply complete |
| `terraform apply` (aws_iam_role policy) | Deploy using that role | ❌ NO (10s wait) | IAM eventual consistency |
| `terraform apply` (read-only `terraform plan`) | Anything | ✅ YES | Plan is read-only |
| `deploy-production.yml` (image update) | `aws ssm send-command` manual | ❌ NO | Both invoke SSM on EC2 |
| Two `aws_secretsmanager_secret_version` updates | — | ❌ NO | Race on secret version |
| `terraform apply` (RDS modify) | App restarts needing DB | ❌ NO | DB unavailable during reboot |
| `cloudflare DNS PATCH` on apex | App using apex domain | ⚠️ MAYBE | DNS TTL determines blast radius |
| `gh secret set production` | Workflow consuming that secret | ❌ NO (1-2s) | Secret manager propagation |

---

## 7. Enforcement

Per `rule-change-process.md` §6.5 Enforcement Parity:

### 7.1 Pre-mutation-state-check.md §3 extension (active now)

`pre-mutation-state-check.md` §3 audit artifact "Pending" section MUST list any concurrent ops to verify serialization:

```markdown
## Pending (this op)

| Action | Owner | Notes |
|--------|-------|-------|
| <op> | <user/agent> | <notes> |
| **Concurrent op check** | Agent verification | List active workflows touching same resource — confirm zero overlap before trigger |
```

### 7.2 Reviewer-checklist (manual)

When reviewing a PR or workflow trigger session that includes ≥2 mutation ops in flight, reviewer asks:
- Are ops touching the same physical resource?
- If yes, is sequential order documented + verified?
- If `aws_instance.user_data` in terraform diff, was deploy serialized AFTER apply?

### 7.3 Memory auto-load

`feedback_concurrent_mutation_ops_conflict.md` (paired same PR) — reminds at session start about §6 decision matrix before any 2nd mutation op trigger.

### 7.4 Override mechanism

For genuine independent ops on different physical resources:

```
git commit -m "...
CONCURRENT_OPS_OK: <op-1> + <op-2> — resources disjoint (<op-1 touches X, op-2 touches Y>)"
```

Trailer logged in quarterly retro. Pattern frequency >5% concurrent-ops claims → meta-review.

### 7.5 Audit gate (deferred per `incident-to-rule-pipeline.md` premature-rule guard)

Future: `audit-gate.py` rule scanning `gh workflow run` invocations in session for overlapping resource scopes. Defer until 2nd recurrence; reviewer-checklist + memory + worked self-test sufficient for v1.0.0.

---

## 8. Self-test (worked example — 2026-05-12 incident)

**Scenario:** 2026-05-12 07:50:41 UTC — agent triggered terraform-apply.yml (Bucket E ec2.tf user_data change) + deploy-production.yml (staging.10 OTel fix) within 22 seconds.

**Apply §4 decision flow retroactively at trigger moment:**

1. **List active workflows:** `terraform-apply.yml run 25721028738 in_progress` — applies to `aws_instance.kh_backend` + `aws_instance.kc_app` (Bucket E plan output verified)
2. **Identify shared resource:** deploy-production.yml `SSM SendCommand` target = `i-00505094277deda29` (= kh_backend EC2). **OVERLAP** with terraform user_data update.
3. **Decision:** STOP. Wait terraform `completed/success`. Verify EC2 `running`. THEN trigger deploy.

**Actual decision at the time:** Triggered both within 22s. Conflict.

**Counterfactual cost with rule applied:**
- Save ~10 min wall-clock (no cancel + re-trigger cycle)
- Save 1 wasted SSM command + workflow run
- Save audit overhead + rule-creation work (recurring class avoided)

**Verdict:** rule fires correctly on the original incident. Self-test PASS ✅

---

## 9. Relationship to other rules

- **`pre-mutation-state-check.md`** v1.1.0 §3 — generic pre-mutation audit; this rule extends "Pending" section with concurrency check
- **`terraform-apply-retry-reconfirm.md`** — covers terraform retry after failure; this rule covers prevention BEFORE first run
- **`agent-aws-access.md`** §4.3 — Tier 3 banned mutations agent-initiated; this rule adds concurrency layer on top
- **`release-deploy-standard.md`** §9 — deploy execution = human-triggered workflow_dispatch; this rule adds "don't run 2 of those at once"
- **`release-fix-retry-budget.md`** §4 (extended v1.1.0 same PR) — adds "Tooling visibility gap" pivot signal; sister rule complements this one
- **`incident-to-rule-pipeline.md`** — this rule direct output of 2026-05-12 incident via 5-stage pipeline
- **`rule-change-process.md`** §6.5 Enforcement Parity — this rule + memory + audit-artifact-extension + worked self-test all ship same PR
- **`feedback_concurrent_mutation_ops_conflict.md`** (memory, paired same PR)

---

## 10. Log

- **2026-05-12 (v1.0.0):** Rule created. Triggered by 2026-05-12 07:50:41 UTC incident: terraform-apply.yml + deploy-production.yml triggered within 22s on same EC2 → terraform's stop-modify-start cycle killed SSM-running deploy-prod.sh with SIGTERM exit 143. Per `incident-to-rule-pipeline.md` 5-stage applied: Detect ✓ (user-flagged "sai quyết định trigger parallel" + asked file rule to prevent recurrence) → Classify ✓ (no existing rule covers concurrent mutation ops on shared production resource; `terraform-apply-retry-reconfirm.md` covers retry not pre-trigger; `agent-aws-access.md` §4.3 covers per-op Tier 3 ban not concurrency) → Rule+Enforce ✓ (this file + `pre-mutation-state-check.md` §3 extension via "Pending" section template + memory `feedback_concurrent_mutation_ops_conflict.md` + Wave 65 audit artifact extension paired same-PR per `rule-change-process.md` §6.5) → Self-Test ✓ (§8 worked example on the originating 2026-05-12 incident — rule fires correctly + counterfactual cost-save demonstrated) → Retro Log ✓ (this entry). Reviewer: @nguyenvankiet (solo-dev MINOR self-approve per `rule-change-process.md` §5 — new constraint adding previously-uncovered concurrency class; no constraint loosening; existing serial deploy patterns grandfathered; rule applies prospectively from this PR). Detector wiring deferred per premature-rule guard ≥7 days.
