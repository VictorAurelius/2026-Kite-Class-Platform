# GAP-473: AWS Stack On-Demand Automation (Start/Stop Scripts + Optional Cron Scheduler)

**Status:** 🟡 PARTIAL
**Priority:** 🟠 P1
**Domain:** DevOps
**Detected:** 2026-05-11
**Related PRs:** Wave 61 Bucket D (this PR)
**Related Docs:**
- `documents/05-guides/operations/stack-on-demand-runbook.md`
- `scripts/aws/start-stack.sh`
- `scripts/aws/stop-stack.sh`
- `documents/03-planning/waves/wave-2026-05-12-61-stop-when-idle-cutover.md` §3 Bucket D
- `.claude/rules/agent-aws-access.md` §4

## Current State (verified 2026-05-11)

| Piece | File / Path | Status |
|-------|-------------|--------|
| Start script (resume EC2 + RDS, wait healthy) | `scripts/aws/start-stack.sh` (~210 LOC) | ✅ shipped Wave 61 Bucket D |
| Stop script (graceful drain + stop EC2 + RDS) | `scripts/aws/stop-stack.sh` (~210 LOC) | ✅ shipped Wave 61 Bucket D |
| Operations runbook (manual + cron template) | `documents/05-guides/operations/stack-on-demand-runbook.md` | ✅ shipped Wave 61 Bucket D |
| Session ledger schema (`.aws-stack-state.json`) | gitignored; populated by scripts | ✅ shipped Wave 61 Bucket D |
| Dry-run mode (CI-safe) | both scripts support `--dry-run` | ✅ shipped Wave 61 Bucket D |
| EventBridge cron schedule (M-F 9-17 ICT auto-stop) | runbook §6 template only | ❌ NOT WIRED (deferred Phase 1.5) |
| Lambda function `kitehub-stack-control` | runbook §6.2 skeleton only | ❌ NOT DEPLOYED (deferred Phase 1.5) |
| IAM role for scheduler Lambda | runbook §6.3 policy template | ❌ NOT CREATED (deferred Phase 1.5) |
| CloudWatch dashboard for session ledger analytics | none | ❌ deferred Phase 2 |

**Grep commands run:**
```bash
ls scripts/aws/                                                  # confirmed absent at start; now populated
find documents/05-guides/operations -iname "*on-demand*"         # confirmed absent at start; now populated
grep -rn "start-instances\|stop-instances" scripts/              # confirmed no prior scripts
aws ec2 describe-instances --filters "Name=tag:Name,..." ...     # confirmed 2 EC2 stopped
aws rds describe-db-instances --db-instance-identifier kitehub-postgres ...  # confirmed RDS stopped
```

## Problem

Phase 1 BETA stack (2× EC2 t3.medium + RDS db.t3.micro) ran always-on default at ~$30/mo. Invite-only beta cohort is sparse — most days zero traffic. Always-on burns compute budget while AWS Activate Founder application status uncertain (D+14 = 2026-05-25).

Without automation, manual `aws ec2 stop-instances ...` / `aws rds stop-db-instance ...` per cycle is error-prone:
- Forgetting one resource leaks cost
- Re-typing instance IDs invites typos
- No usage ledger → no cost forecast data
- No grace warning → stops mid-demo by accident

Need: idempotent, dry-run-safe, audit-logged scripts user invokes per session — agent CANNOT touch lifecycle (per `agent-aws-access.md` §4 BANNED list).

Phase 1.5 (post-AWS-Activate-decision, post-beta-volume-justification): wire EventBridge cron + Lambda for automatic M-F 9-17 ICT cycles. Out of scope cho Wave 61 vì:
- Beta thưa → manual cycles còn ổn
- Activate evaluation period cần thấy thực-tế cost pattern, không bị mask bởi cron
- Lambda+IAM+EventBridge wiring requires user-side Console interaction (agent KHÔNG create autonomously per `agent-aws-access.md`)

## Context

- **Discovered:** Wave 61 plan §3 Bucket D — user chose path (e) "stop-when-idle" sau khi xác nhận Architecture C 2GB không đủ RAM
- **Scope:** Phase 1 BETA only. Production v1.0.0 (Phase 1.5+ PAID) sẽ chuyển sang always-on hoặc K8s auto-scaling — different pattern, không tái-dụng scripts này
- **Cost data point (manual ledger projection):**
  - Always-on: ~$30/mo
  - Stop-when-idle (~4h/day × 5 day/week): ~$8-10/mo (66-75% savings)
- **Decoupling benefit:** Wave 61 cutover (DNS + SES + seed + automation + security headers) ship được bất kể AWS Activate D+14 verdict. Activate approved → upgrade lên always-on miễn phí; Activate denied → stop-when-idle ~$5-10/mo manageable

## Evidence

- AWS CLI describe (Tier 1 read-only, agent-aws-access.md §2.1) confirmed 2 EC2 + RDS all stopped at session start: `i-0b65c3947d36cae61`, `i-07f6de54544162124`, `kitehub-postgres`
- Dry-run verification (no AWS calls): `bash scripts/aws/start-stack.sh --dry-run` exit 0 + `bash scripts/aws/stop-stack.sh --dry-run` exit 0
- Wave 61 plan §3 Bucket D acceptance: "1-command start (5-10 min total) + 1-command stop (2-3 min); state.json log per session" — satisfied by scripts shipped this PR

## Proposed Fix

### Phase 1 (Wave 61 Bucket D, this PR — DONE)

- [x] `scripts/aws/start-stack.sh` — resume EC2 + RDS + wait healthy + ledger entry
- [x] `scripts/aws/stop-stack.sh` — 60s grace + stop EC2 + RDS + ledger close
- [x] `documents/05-guides/operations/stack-on-demand-runbook.md` — manual cycle + cron template + troubleshooting
- [x] `.gitignore` add `.aws-stack-state.json`
- [x] Shellcheck clean (verified via CI `script-quality.yml`)
- [x] Dry-run mode for both scripts (CI-safe)

### Phase 2 (deferred Phase 1.5 — when beta volume justifies)

- [ ] Trigger condition: ≥5 active beta tenants OR sustained M-F traffic pattern OR AWS Activate denial confirmed
- [ ] Deploy Lambda `kitehub-stack-control` (runtime python3.11) per runbook §6.2 template
- [ ] Create IAM role with policy per runbook §6.3 (minimum-scope; instance ARN + RDS ARN only)
- [ ] Wire EventBridge rules: `kitehub-stack-start` (cron M-F 09:00 ICT), `kitehub-stack-stop` (cron M-F 17:00 ICT)
- [ ] Test cycle: enable rules 1 week → verify CloudWatch Logs → measure actual cost vs manual baseline
- [ ] Disable rules during AWS Activate evaluation if approval pending

### Phase 3 (deferred Phase 2 — production scale)

- [ ] CloudWatch dashboard reading `.aws-stack-state.json` equivalent (or migrate ledger to CloudWatch Logs Insights)
- [ ] Cost-anomaly alarms (compute cost > $15/mo threshold)
- [ ] Sunset stop-when-idle pattern when migrating to K8s auto-scaling (Phase 2 + Phase 3 production)

## Acceptance Criteria

- [x] **Phase 1:** `bash scripts/aws/start-stack.sh --dry-run` exits 0
- [x] **Phase 1:** `bash scripts/aws/stop-stack.sh --dry-run` exits 0
- [x] **Phase 1:** Shellcheck clean on both scripts (CI gate)
- [x] **Phase 1:** Runbook covers when-to-start, when-to-stop, pre-checks, manual cycle, dry-run, optional cron, ledger analysis, troubleshooting (8 sections + boundaries)
- [x] **Phase 1:** GAP file conforms `_TEMPLATE.md` (Current State + Problem + Evidence + Proposed Fix + AC + Related + Log)
- [x] **Phase 1:** 5 business-rule attributes documented (§Business Rule Attributes below per `business-logic-review.md` §2)
- [x] **Phase 1:** CSV row added to `documents/04-quality/gaps/gap-status.csv`
- [x] **Phase 1:** `.gitignore` updated cho `.aws-stack-state.json`
- [ ] **Phase 2:** EventBridge cron wired + 1 week stable cycle (deferred Phase 1.5; depends on AWS Activate verdict + beta volume)
- [ ] **Phase 2:** Actual cost ≤ $10/mo verified via AWS Cost Explorer MTD (deferred Phase 1.5)
- [ ] **Phase 3:** Dashboard + alarms (deferred Phase 2)

## Business Rule Attributes (per `.claude/rules/business-logic-review.md` §2)

This is operational tooling not business logic per se, but the **stop-when-idle pattern** itself is a business decision:

- **Source:** User decision 2026-05-11 path (e) stop-when-idle, recorded in `wave-2026-05-12-61-stop-when-idle-cutover.md` §1 + `release-1-deploy-plan.md` Phase 1 BETA cost section
- **Rationale:** Invite-only beta thưa khách (≤5 active tenants expected Q3 2026) → 4h/day × 5 day/week realistic usage; always-on $30/mo wastes 66-75% of compute when AWS Activate Founder status uncertain (D+14 = 2026-05-25); decoupling deploy cutover from Activate verdict reduces single-point-of-failure
- **Reviewer:** @nguyenvankiet (solo-dev, acting CTO + DevOps lead, 2026-05-11). Sign-off paired with Wave 61 plan PR #1172 merge
- **Compliance check:** N/A — no PDPL / Consumer Protection / Cybersecurity Law trigger (operational pattern, no user data movement; ALB stays always-on so users never see "service down" UX bug; outage windows scheduled outside business hours)
- **Review cadence:** Quarterly OR event-driven. **Next review:** 2026-08-11. Triggers: (a) ≥5 active beta tenants weekly, (b) AWS Activate decision (approved → revisit always-on; denied → continue), (c) sustained complaint about cold-start latency from beta cohort, (d) Phase 1.5 PAID launch (then stop-when-idle sunsets in favor of always-on or K8s auto-scaling)

## Related

- **Wave 61 plan:** `documents/03-planning/waves/wave-2026-05-12-61-stop-when-idle-cutover.md` §3 Bucket D
- **Sister buckets:**
  - Bucket A (DNS + SSL) — independent
  - Bucket B (SES production) — independent
  - Bucket C (Production seed runbook) — invokes start-stack.sh as Step 1
  - Bucket E (Security headers) — independent
- **Rules invoked:**
  - `.claude/rules/agent-aws-access.md` §4 — BANNED autonomous `start-*` / `stop-*` (defines agent vs user boundary)
  - `.claude/rules/release-deploy-standard.md` §9 — agent deploy execution matrix (this gap = "post-deploy verification" agent-tooled, user-executed)
  - `.claude/rules/business-logic-review.md` §2 — 5 attributes (above)
- **AWS resources:** EC2 `i-0b65c3947d36cae61` (kitehub-kh-backend), `i-07f6de54544162124` (kitehub-kc-app); RDS `kitehub-postgres`; account 906286017800 region `ap-southeast-1`
- **Cost context:** `documents/03-planning/roadmap/release-1-deploy-plan.md` Phase 1 BETA architecture cost; AWS Activate Founder $1k credit pending (D+14 verdict 2026-05-25)
- **Audit trail:** Session ledger `.aws-stack-state.json` (gitignored, per-operator local); monthly cost forecast formula in runbook §7

## Log


- 2026-06-14: phase re-triage — phase-1-beta→phase-1.5-paid (notes 'EventBridge cron defer Phase 1.5').
- **2026-05-11** — Initial write-up (state-check completed; gap filed during Wave 61 Bucket D execution). Phase 1 DONE (scripts + runbook + ledger schema); Phase 2 (EventBridge cron + Lambda) deferred Phase 1.5; Phase 3 (dashboard + alarms) deferred Phase 2. Status 🟡 PARTIAL because Phase 1 scope shipped fully but full automation vision spans 3 phases; aligns with `gap-done-discipline.md` §3 PARTIAL exit ramp (deferred slices have explicit AC + trigger conditions).
