---
title: Wave 70 — GAP-502 production thrashing fix (RC1 cred sync + RC2 sizing/JVM)
status: complete
created: 2026-05-13
updated: 2026-05-13
waves: [70]
gaps: [GAP-502, GAP-447]
---

# Wave 70 — GAP-502 production thrashing fix

**Goal:** Stop kh_backend production thrashing — RC1 RabbitMQ auth fail + RC2 container OOM — và unblock Plan 1 self-test execution.
**Trigger:** Wave 69 audit-of-trust pass surfaced 11 container die/1h + Spring `AmqpAuthenticationException` loops + cgroup OOM kills; user chốt path A (cred sync) + chấp nhận t3.medium → t3.large upsize (+$30/mo, downsize evaluation post-release).
**Estimated wall-clock:** ~3-4h total. Code-prep parallel ~30 min (4 agents background); live ops sequential ~2-3h coordinator-managed (terraform apply → SSM cred sync → SSM compose deploy).

---

## 1. Brainstorm (5-10 min)

**Q1 (alignment):** Phase 1 BETA launch blocker — Plan 1 self-test cannot execute, cohort onboarding impossible. Serves all personas (P2 Owner first-touch trên `api.kitehub.me`). Anchor wave cho path-to-beta sequence Wave 60→63 plan.

**Q2 (trade-offs):**
- **RC1 Option B (defer rabbit autoStartup=false) considered + rejected** — user chốt Option A (cred sync). Lý do: defer = symptom hide; Option A diagnose actual config drift + đảm bảo rabbit listeners hoạt động khi cohort onboard.
- **RC2 Sub-C (fixed `-Xmx` lower) considered + rejected** — GC thrash risk + perf degrade khi cohort onboard. Sub-A (JVM `MaxRAMPercentage=50.0`) + Sub-B (mem_limit bump + t3.medium → t3.large) combo: ergonomic + cost +$30/mo acceptable.
- **Single-bucket "fix everything at once" considered + rejected** — concurrency constraint per `concurrent-production-mutation-ops.md`: D (terraform stop-modify-start EC2) + A/C (SSM SendCommand on same EC2) → terraform stop kills SSM mid-run. Phải sequential.

**Q3 (risks):**
- **Bucket D terraform apply timing** — `instance_type` change → stop-modify-start window ~3-5 min. Production unavailable during window. Mitigation: Plan 1 BETA chưa có real users; window acceptable.
- **Bucket A cred sync wrong target** — sync sai chiều có thể lock rabbit out hoàn toàn. Mitigation: Phase 1 audit list users + read .env FIRST (Tier 1 read-only); decide direction sau evidence.
- **Bucket C compose deploy partial** — nếu deploy-production.yml fail mid-stream → services có thể up với mixed config. Mitigation: rollback runbook (GAP-378 DONE) + workflow_dispatch confirm "APPLY" gate.
- **t3.large state-modify cascade** — per `concurrent-production-mutation-ops.md` §6, AWS implements instance_type change as stop→ModifyInstanceAttribute→start; user_data NOT re-executed (data persists); container state lost (acceptable, will restart).
- **Recurrence #3 scaffold pattern** — per `feedback_e2e_scaffold_pattern_universal.md`: AC `[x]` không bằng production-verified. Wave 70 closure phải execute live verification (10 consecutive `/api/v1/beta-access/request` 2xx + 30 min stability gate) trước khi flip GAP-502 DONE.

---

## 2. Task Breakdown

| Bucket | Gap(s) | Type | Owner | Effort | Disjoint? |
|--------|--------|------|-------|--------|-----------|
| A | GAP-502 RC1 | Runbook + live SSM ops | bg-agent (drafts) + coordinator (exec) | ~30 min draft + ~30 min exec | ✅ runbook file + live ops |
| C | GAP-502 RC2 Sub-A+B (compose) | Code edit + deploy | bg-agent (code) + coordinator (deploy) | ~20 min code + ~20 min deploy | ✅ `docker-compose.production.yml` only |
| D | GAP-502 RC2 Sub-B (terraform) + GAP-447 sizing revisit | Code edit + workflow_dispatch | bg-agent (code) + coordinator (apply) | ~15 min code + ~10 min apply | ✅ `variables.tf` + tf-apply |
| E | GAP-502 Phase 3 prevention | Code + ADR | bg-agent | ~30 min | ✅ `cloudwatch.tf` + docs |

**Disjoint check:**
- A touches `/etc/kite/.env` (live) + new runbook file `documents/05-guides/deploy/rabbitmq-cred-sync-runbook.md` — no overlap with B/C/D/E source paths
- C touches `docker-compose.production.yml` only (root) — no other bucket touches it
- D touches `infrastructure/terraform-aws/variables.tf` only — E touches `cloudwatch.tf` (different file)
- E touches `infrastructure/terraform-aws/cloudwatch.tf` + new ADR `documents/02-architecture/adr/ADR-NNN-jvm-container-memory-budget.md`

Code-prep agents (A draft + C + D + E) can spawn PARALLEL (background, worktree isolation). Live ops sequential: **D apply → A cred sync → C compose deploy → E apply**.

---

## 3. Scope (compact schema)

**Stake tier (per `wave-pack-planner/SKILL.md` §Step 4.6):** **HIGH** (production thrashing fix, BETA blocker, mutation ops on production EC2) → model: **Opus 4.7 full** for all buckets
**Cross-layer? (per `wave-pack-planner/SKILL.md` §Step 4.5):** **NO** — pure infra/devops scope, no FE+BE contract. Skip Bucket 0 Foundation.

> Gap referencing convention per `gap-architecture-v2.md`: canonical id from `gap-status.csv`. GAP-502 verified: `bash scripts/query-gaps.sh GAP-502` → P0 BLOCKING, OPEN, phase-1-beta. GAP-447 (PARTIAL) referenced for sizing decision revisit.

| # | Bucket | Gap(s) | Priority | Files (glob) | Spawn order |
|:-:|--------|--------|:--------:|--------------|:-----------:|
| 1 | **A — RC1 cred sync runbook** | GAP-502 RC1 | 🔴 P0 | `documents/05-guides/deploy/rabbitmq-cred-sync-runbook.md` (NEW) | parallel (code-prep) |
| 2 | **C — RC2 JVM + mem_limit (compose)** | GAP-502 RC2 Sub-A+B | 🔴 P0 | `docker-compose.production.yml` | parallel (code-prep) |
| 3 | **D — RC2 EC2 upsize (terraform)** | GAP-502 RC2 Sub-B + GAP-447 | 🔴 P0 | `infrastructure/terraform-aws/variables.tf` | parallel (code-prep) |
| 4 | **E — Phase 3 prevention** | GAP-502 Phase 3 | 🟠 P1 | `infrastructure/terraform-aws/cloudwatch.tf` + `documents/02-architecture/adr/ADR-029-jvm-container-memory-budget.md` (NEW) | parallel (code-prep) |

**Live ops sequencing (post-merge, coordinator-managed per `concurrent-production-mutation-ops.md`):**
1. Bucket D apply via `terraform-apply.yml workflow_dispatch dry_run=false confirm=APPLY` → wait EC2 `running` (~5 min stop-modify-start window)
2. Bucket A exec: SSM diagnose (Tier 1: `aws ssm send-command rabbitmqctl list_users` borderline → confirm với user) → cred sync (Tier 3 mutation, user-trigger)
3. Bucket C exec: `deploy-production.yml workflow_dispatch confirm=APPLY` for new compose deploy
4. Bucket E apply: terraform-apply.yml for CloudWatch alarms (read-only adds, low risk; can bundle with closure)

### Bucket A — RC1 RabbitMQ cred sync runbook

- Files: `documents/05-guides/deploy/rabbitmq-cred-sync-runbook.md` (NEW per `deployment-naming-convention.md` §2 — pre-deploy one-time live ops procedure)
- Acceptance:
  - Runbook §1 Pre-flight: SSM commands to list rabbit users + read `/etc/kite/.env` (Tier 1/2 per `agent-aws-access.md`)
  - §2 Diagnose: decision tree — creds match? mismatch direction? rabbit user missing?
  - §3 Sync (Option A path): either `rabbitmqctl change_password` OR update `/etc/kite/.env` to match rabbit (decide based on §2 evidence)
  - §4 Restart + verify: `docker compose restart` + 10-min stability check
  - §5 Audit artifact: `documents/04-quality/audits/aws-verification/2026-05-13-gap-502-rc1-cred-sync.md` template per `pre-mutation-state-check.md` §3
  - §6 Rollback: revert .env from backup OR rabbit user password reset
- Bucket-level local verify: docs only — `bash scripts/check-docs.sh` PASS

### Bucket C — RC2 JVM + mem_limit (docker-compose.production.yml)

- Files: `docker-compose.production.yml` (root)
- Edits per `ai-branding-guidelines.md` §11.4 + `release-deploy-standard.md` §3.1:
  - Replace fixed `-Xmx256m -Xms128m -XX:+UseSerialGC` (email, gateway) → `JAVA_OPTS=-XX:MaxRAMPercentage=50.0 -XX:+UseContainerSupport -XX:+UseSerialGC`
  - Replace fixed `-Xmx384m -Xms192m` (admin, branding, subscription) → same `JAVA_OPTS` pattern
  - Bump `mem_limit`: email + gateway 320 MiB → 512 MiB; admin + branding + subscription 480 MiB → 640 MiB
  - Add `healthcheck.start_period: 120s` cho all 5 kitehub-* services (currently default 0s — triggers preemptive restart per GAP-502 §3 Phase 3)
- Tests: `docker compose config -f docker-compose.production.yml` parse clean; YAML valid
- Acceptance:
  - Compose parses without error
  - JAVA_OPTS pattern consistent 5 services
  - mem_limit total: 512×2 + 640×3 = 2944 MiB (vs current 320×2 + 480×3 = 2080 MiB; +864 MiB) — fits t3.large 8 GiB với rabbit 320 + redis 320 + OS 1 GiB
- Bucket-level local verify: `docker compose -f docker-compose.production.yml config -q` (just parse, không up)

### Bucket D — RC2 EC2 upsize + GAP-447 sizing revisit (terraform)

- Files: `infrastructure/terraform-aws/variables.tf` (one variable + description update)
- Edits:
  - `kh_backend_instance_type` default `"t3.medium"` → `"t3.large"`
  - Update description to reflect Wave 70 sizing decision: Phase 1 BETA upsize per GAP-502 OOM evidence; downsize evaluation post-release per user direction
  - **NOT touching kc_app_instance_type** — KC stack pre-Vercel-pivot footprint OK
- Pre-mutation audit artifact (REQUIRED per `pre-mutation-state-check.md` §3): `documents/04-quality/audits/aws-verification/2026-05-13-wave-70-pre-apply-d-terraform.md`
  - Section: Real vs phantom changes from `terraform plan` output
  - Section: Concurrency check per `concurrent-production-mutation-ops.md` — confirm no deploy-production.yml in flight
  - Section: Cost delta ($30 → $60/mo kh_backend)
- Acceptance:
  - Variable update merges
  - terraform-apply.yml dry_run=true output shows `1 to change` (aws_instance.kh_backend instance_type)
  - GAP-447 status flipped 🟡 PARTIAL → log entry "Wave 70 revisit: t3.medium insufficient per GAP-502 OOM evidence; upsize t3.large; downsize evaluation deferred post-Phase-1-BETA-launch"
- Bucket-level local verify: `cd infrastructure/terraform-aws && terraform validate && terraform fmt -check`

### Bucket E — Phase 3 prevention (CloudWatch alarms + ADR)

- Files:
  - `infrastructure/terraform-aws/cloudwatch.tf` — add `aws_cloudwatch_metric_alarm` "kh_backend_memory_high" (MemoryUtilization >85% 3 datapoints out of 5 minutes — already mentioned in GAP-447 §"OOM safety net" but not yet implemented)
  - `documents/02-architecture/adr/ADR-029-jvm-container-memory-budget.md` (NEW per `deployment-naming-convention.md` ADR scope) — JVM-in-container budget calculation rule per service tier
- Acceptance:
  - CloudWatch alarm terraform validates + `terraform plan` shows `1 to add`
  - ADR-029 cites GAP-502 + GAP-447 + Wave 70 decision; status DRAFT (ACCEPTED post-apply)
  - Update `documents/02-architecture/adr/adrs-index.csv` per `meta-csv-index-pattern.md`
- Bucket-level local verify: `terraform validate` + `bash scripts/check-adrs-index-csv.sh`

---

## 4. State-Check Evidence (per `audit-to-gap-pipeline.md` §2.6)

| Symbol | Type | Verification command | Evidence | Verdict |
|--------|------|----------------------|----------|---------|
| `GAP-502` | Canonical gap | `bash scripts/query-gaps.sh GAP-502` | Found OPEN P0 phase-1-beta | ✅ exists |
| `GAP-447` | Canonical gap | `bash scripts/query-gaps.sh GAP-447` | Found PARTIAL P0 phase-1-beta | ✅ exists |
| `kh_backend_instance_type` | Terraform variable | `grep -n kh_backend_instance_type infrastructure/terraform-aws/variables.tf` | Line 48-52, default `"t3.medium"` | ✅ exists |
| `aws_instance.kh_backend` | Terraform resource | `grep -n kh_backend infrastructure/terraform-aws/ec2.tf` | Line 70 | ✅ exists |
| `docker-compose.production.yml` | Compose file | `ls docker-compose.production.yml` | Exists at repo root | ✅ exists |
| `kitehub-email` JAVA_OPTS fixed `-Xmx256m` | Current compose state | `grep -n "Xmx256m" docker-compose.production.yml` | Line 122, 152 (email, gateway) | ✅ current state confirmed |
| `infrastructure/terraform-aws/cloudwatch.tf` | Alarms file | `ls infrastructure/terraform-aws/cloudwatch.tf` | Exists 5.4K | ✅ exists |
| `kh_backend_memory_high` CloudWatch alarm | Terraform resource | `grep -n memory_high infrastructure/terraform-aws/cloudwatch.tf` | 0 matches | 🆕 to-be-created (Bucket E) |
| `documents/05-guides/deploy/rabbitmq-cred-sync-runbook.md` | New runbook | `ls documents/05-guides/deploy/rabbitmq-cred-sync-runbook.md` | Does not exist | 🆕 to-be-created (Bucket A) |
| `documents/02-architecture/adr/ADR-029-jvm-container-memory-budget.md` | New ADR | `ls documents/02-architecture/adr/ADR-029*` | Does not exist; ADR-028 latest per CSV | 🆕 to-be-created (Bucket E) |
| `terraform-apply.yml` workflow | CI workflow | `ls .github/workflows/terraform-apply.yml` | Exists (per Wave 44 GAP-449 Bucket B) | ✅ exists |
| `deploy-production.yml` workflow | CI workflow | `ls .github/workflows/deploy-production.yml` | Exists | ✅ exists |

---

## 5. Verification Gates (per bucket)

| Bucket | Local verify command | CI gate | Live verify (post-apply) |
|--------|---------------------|---------|--------------------------|
| A | `bash scripts/check-docs.sh` | script-quality | Post-cred-sync: kitehub-email Spring context init clean ≥10 min; no `AmqpAuthenticationException` |
| C | `docker compose -f docker-compose.production.yml config -q` | (none — yaml parse only) | Post-deploy: 5 kitehub-* services Up ≥30 min; container memory usage <80% limit |
| D | `cd infrastructure/terraform-aws && terraform validate && terraform fmt -check` | terraform-plan workflow | Post-apply: EC2 i-05d7af46d01436b96 instance_type `t3.large`; LaunchTime updated; CloudWatch host memory headroom >1 GiB |
| E | `terraform validate && bash scripts/check-adrs-index-csv.sh` | terraform-plan + script-quality | Post-apply: alarm ARN active; ADR-029 status ACCEPTED post-merge |

**Wave-level stability gate (per GAP-502 AC):**
- 10 consecutive POST `/api/v1/beta-access/request` valid payload → 2xx/4xx (NOT 502 nor 400-empty)
- All 5 kitehub-* services Up ≥30 min continuous
- No `dmesg` OOM events for kitehub-* containers trong 1h sliding window
- Plan 1 self-test §3 Bước 2/3/5/7 re-runnable

---

## 6. Agent Spawn Pattern

Per `feedback_parallel_agent_strategy.md` + `agent-background-spawn-default.md`:
- 4 buckets (A draft, C, D, E) spawned with `run_in_background: true` + `isolation: worktree`
- All HIGH stake → Opus 4.7 (full) for all 4 agents
- RELATIVE paths in agent prompts per `feedback_worktree_absolute_path_contamination.md`
- Coordinator waits all 4 background completions → merges sequentially A→C→D→E
- **Live ops sequencing post-merge (coordinator-managed, user-triggered)**: D apply → A SSM exec → C deploy → E apply. NO parallel mutations per `concurrent-production-mutation-ops.md`.

---

## 7. Closure Protocol

Per `gap-done-discipline.md` + `feedback_post_merge_doc_sync.md` + `feedback_wave_history_append_required.md` + `post-wave-cleanup.md` + `feedback_wave_closure_release_progress_report.md`:
- Each bucket PR updates GAP-502 + GAP-447 Log + status appropriately
- Wave-level stability gate (§5) MUST pass live BEFORE flipping GAP-502 🟢 DONE (per `feedback_e2e_scaffold_pattern_universal.md` — recurrence #3 prevention)
- ROADMAP §🚀 Next Action updated in closure PR — point to Wave 71 (likely Plan 1 self-test execution OR Plan 2 cohort outreach)
- Wave plan frontmatter `status: complete` flip in closure PR
- `wave-history.jsonl` append in closure PR (Rule 15 enforcement)
- Sub-gaps filed for any deferral; PARTIAL exit-ramp per `gap-done-discipline.md` §3 (e.g., if RC1 cred sync reveals deeper secret-rotation issue → file follow-up)
- Run `bash scripts/prune-merged-worktrees.sh --yes` post-merge per `post-wave-cleanup.md`
- **`## Release Plan Progress` section in closure PR body** — current Phase 1 BETA progress + GAP-502 closure unlocks Plan 1 → updated Waves Remaining table

**Post-release downsize evaluation (user-directed):**
- File follow-up gap `GAP-NNN-post-release-kh-backend-downsize-evaluation` to track decision after Phase 1 BETA stabilizes (≥4 weeks production stability + memory metrics baseline)
- Criteria: avg MemoryUtilization <60% + zero OOM events → consider downsize back t3.large → t3.medium; else stay t3.large

---

## 8. Log

- **2026-05-13 (draft):** Plan created post Wave 69 audit-of-trust closure. User chốt RC1 Option A (cred sync) + RC2 Sub-A+B (JVM tune + t3.large upsize) + post-release downsize evaluation. 4 buckets parallel code-prep + sequential live ops per `concurrent-production-mutation-ops.md`.

- **2026-05-13 (complete):** Wave 70 SHIPPED. 6 PRs merged total: plan #1258, code-prep A/C/D/E (#1259/1260/1261/1262), follow-up fix #1263 (GAP-504+505). Live ops executed end-to-end this session: terraform-apply.yml run 25788727856 (t3.medium → t3.large success) + 3 SSM cred sync rounds (5 ephemeral rabbit users generated due to GAP-506 chicken-and-egg) + deploy-production.yml runs 25789336481 + 25790657079 + 25791611463 (3rd ran Step 6.5 self-heal correctly per logs). RC1 (rabbit auth) ✅ RESOLVED. RC2 (OOM) ✅ RESOLVED (host mem 7.8GB / 5.6GB free post-upsize). 4/5 services healthy + zero auth + zero OOM + 5/5 API valid responses. Outcomes:
  - GAP-502 → 🟡 PARTIAL (email service unhealthy cosmetic; functional)
  - GAP-447 → revisited (ADR-029 + variable description)
  - GAP-504 (rabbit self-heal in deploy-prod.sh) → DONE
  - GAP-505 (per-service healthcheck port override) → DONE
  - GAP-506 (deploy-prod tech debt: chicken-and-egg + ephemeral cred + start_period + email port) → FILED OPEN P1
  - ADR-029 ACCEPTED (JVM-in-container memory budget rule)
  - Rule applied first time end-to-end: `agent-aws-access.md` Tier 3 carve-out via workflow_dispatch + ad-hoc Step 6.5 in scripted location
  - Rule violations observed during live ops (raw `docker` via SSM) → tracked GAP-506 Phase 3
  - Post-release downsize evaluation deferred (criteria: ≥4 weeks stable + avg MemoryUtilization <60% → consider t3.large → t3.medium)
