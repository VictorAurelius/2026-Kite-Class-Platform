---
title: Wave aws-restore-1 — Production stack restore post-GAP-612 Day 8 UNBLOCK
status: draft
created: 2026-05-26
updated: 2026-05-26
audience: dev
tag_primary: aws-restore
tags_secondary: [phase-1-beta, gap-612, rst-prereq]
counter: 1
date_launch: 2026-05-26
waves: [aws-restore-1]
gaps: [GAP-612, GAP-693]
---

# Wave aws-restore-1 — Production stack restore post-GAP-612 Day 8 UNBLOCK

## 1. Brainstorm

### Q1 — Inside-out scope (per `inside-out-completeness-trigger.md`)

- **From session-handoff canonical + Wave audit-stale-sweep-1 recommendation:** GAP-612 single-root unblock cascades 13 PARTIAL→DONE via live walkthrough. Wave aws-restore-1 = pre-requisite for Wave rst-cascade-1 + 4 hard-blocker waves (security-1 / ops-1 / compliance-1 / perf-1).
- **From GAP-612 Phase 2 (post-restore) Proposed Fix:** `aws sts get-caller-identity` confirm + `bash scripts/aws/start-stack.sh` restart EC2 + RDS.
- **From GAP-693 SOP (deferred):** 13-step rebuild playbook NOT YET shipped — this wave executes restore using existing terraform + scripts WITHOUT the SOP runbook. GAP-693 SOP creation deferred follow-up wave.
- **From empirical AWS state-check 2026-05-26** (per `pre-mutation-state-check.md`):
  - ✅ Account ACTIVE (sts get-caller-identity succeeds; suspension lifted Day 8 2026-05-25)
  - ✅ CloudTrail IsLogging=True (`aws-observability-first.md` baseline satisfied)
  - 🟡 EC2: 3 STOPPED (kh_backend i-05d7af46d01436b96 + kc_app i-01ad56b0067d0213b + kc_app_fe i-05cfda7c6c60b683f — last LaunchTime 2026-05-17, EBS volumes intact)
  - 🔴 RDS: 0 instances (kitehub-postgres DELETED post-suspension; 2 snapshots available — manual `final-kitehub-postgresa9068e7e-9e0c-4c36-973b-d6f7800c3af3` 2026-05-21 + auto `rds:kitehub-postgres-2026-05-17-17-08` 2026-05-17)
  - 🔴 ALB: 0 load balancers (kitehub-alb deleted Wave beta-readiness-8 closure cleanup ~$27/mo save — needs terraform recreate)

### Q2 — Outside-in (per `outside-in-coverage-trigger.md`)

**SKIPPED** per §4 exception "Wave 100% internal scope — ops/refactor/tech debt". AWS restore = execute existing terraform IaC + scripts, no architecture rethink. User-facing impact = restore previous state, not new feature.

### Q3 — Risks + tradeoffs

| Risk | Severity | Mitigation |
|---|---|---|
| RDS restore picks wrong snapshot (data loss) | 🔴 HIGH | Use latest MANUAL snapshot `final-kitehub-postgresa9068e7e-...` 2026-05-21 (more recent + intentional backup); verify snapshot integrity pre-apply |
| Terraform plan shows unexpected drift (resources changed during suspension) | 🟠 MEDIUM | Pre-apply `terraform plan` dry-run + reconciliation table per `pre-mutation-state-check.md` §3.5 |
| Concurrent ops on shared EC2 resources (per `concurrent-production-mutation-ops.md`) | 🟠 MEDIUM | Strict serialization Phase A → B → C; verify each phase complete before next |
| EC2 fails to start (corrupted EBS, AMI issue) | 🟡 LOW | Stopped instances = data preserved; AWS auto-recovery handles most failures; fallback = terraform replace |
| ALB DNS propagation latency post-recreate | 🟡 LOW | CloudFlare apex CNAME → ALB; expect 5-15min DNS warm-up post-recreate |
| Wave 37 backlog terraform drift surfaces in plan (per Wave 86 precedent) | 🟠 MEDIUM | `targets` workflow input apply only ALB+RDS subset; defer backlog drift to separate audit |
| GAP-693 SOP not shipped — manual orchestration | 🟠 MEDIUM | Wave plan §3 documents exact steps + gates; serves as ad-hoc SOP for this restore (informs GAP-693 future deliverable) |

### Q4 — Authorization required

Per `release-deploy-standard.md` §9 + `agent-aws-access.md` §4 + `dev-authorized-terraform-trigger.md`:
- Phase A (EC2 start via `start-stack.sh`): Tier 2 always-confirm — requires user explicit "tôi cho phép" / "claude trigger"
- Phase B (RDS terraform apply): Tier 3 banned-default — requires user explicit override trailer `AGENT_AWS_TIER3_OK` per `agent-aws-access.md` §6 OR `claude trigger` per `dev-authorized-terraform-trigger.md`
- Phase C (ALB terraform apply): Tier 3 banned-default — same requirement
- Phase D (smoke verify): Tier 1 read-only — no special authorization

**Plan stops at end of Phase 0 (this PR draft) — user reviews + authorizes Phase A-C executions step-by-step.**

## 2. Task Breakdown

| # | Phase | Task | Est | Risk | Authorization |
|---|---|---|---|---|---|
| T1 | Phase 0 | Wave plan + draft PR for user review | 15min | None | None (this PR) |
| T2 | Phase A | `bash scripts/aws/start-stack.sh` — start 3 EC2 | 5min | Low | Tier 2 confirm |
| T3 | Phase A | Verify 3 EC2 `running` + SSM reachable + internal connectivity | 10min | Low | Tier 1 |
| T4 | Phase B | Pre-apply audit artifact (per `pre-mutation-state-check.md` §3) | 15min | None | Tier 1 |
| T5 | Phase B | Trigger `terraform-apply.yml` with `targets='aws_db_instance.kitehub'` + `dry_run=true` | 5min | Low | Tier 2 |
| T6 | Phase B | Review terraform plan output + verify RDS restore from `final-kitehub-postgresa9068e7e-...` snapshot | 10min | Medium | Tier 1 |
| T7 | Phase B | Trigger `terraform-apply.yml` with `dry_run=false` + `confirm=APPLY` | 5min | **HIGH** | Tier 3 + `claude trigger` |
| T8 | Phase B | Wait RDS available state (~10-15min cold start from snapshot) | 15min | Medium | Tier 1 monitor |
| T9 | Phase B | Verify RDS reachable from EC2 (psql connectivity test via SSM) | 10min | Medium | Tier 1 |
| T10 | Phase C | Pre-apply audit for ALB recreate | 10min | None | Tier 1 |
| T11 | Phase C | Trigger `terraform-apply.yml` with `targets='aws_lb.kitehub*,aws_lb_listener.*,aws_lb_target_group.*'` + `dry_run=true` | 5min | Low | Tier 2 |
| T12 | Phase C | Review plan + `dry_run=false` | 5min | **HIGH** | Tier 3 + `claude trigger` |
| T13 | Phase C | Wait ALB Active state (~3-5min) + 2 EIP allocations | 10min | Low | Tier 1 monitor |
| T14 | Phase D | Verify CloudFlare apex DNS → ALB chain | 5min | Low | Tier 1 |
| T15 | Phase D | Live smoke: curl kitehub.me + api.kitehub.me /actuator/health | 5min | Low | Tier 1 |
| T16 | Phase D | Verify admin login flow (per `pre-handoff-self-test-completeness.md` §2.4) | 15min | Low | Tier 1 |
| T17 | Phase E | Closure PR — Scope-Completeness Reconciliation + 5-target sync | 15min | None | None |

**Total estimate:** ~160min (~2.5h) — but heavily gated by user authorization checkpoints between phases.

## 3. Scope

### Phase A — EC2 restart (LOW risk, Tier 2)

**Goal:** 3 stopped EC2 instances → `running` state with data preserved.

**Action:**
```bash
bash scripts/aws/start-stack.sh
```

**Pre-conditions:**
- AWS account active (verified ✅)
- CloudTrail logging (verified ✅)
- Tag `Name=kitehub-{kh-backend,kc-app,kc-app-fe}` present (verified — 3 EC2 found)

**Verify post:**
- `aws ec2 describe-instances` shows all 3 `State.Name=running`
- SSM `aws ssm start-session --target i-...` succeeds (proves IAM + SSM agent healthy)
- Internal connectivity: SSM → curl localhost:8080/actuator/health on kh_backend EC2

### Phase B — RDS restore from snapshot (HIGH risk, Tier 3)

**Goal:** Recreate `kitehub-postgres` RDS instance from latest manual snapshot.

**Snapshot selected:** `final-kitehub-postgresa9068e7e-9e0c-4c36-973b-d6f7800c3af3` (manual, 2026-05-21, more recent + intentional backup vs auto 2026-05-17)

**Action sequence:**
1. Pre-apply audit artifact `documents/04-quality/audits/aws-verification/2026-05-26-wave-aws-restore-1-phase-b-rds-restore-preapply.md`
2. `gh workflow run terraform-apply.yml -f targets='aws_db_instance.kitehub' -f dry_run=true` — plan dry-run
3. Review terraform plan: expect `aws_db_instance.kitehub` create with `snapshot_identifier=final-kitehub-postgresa9068e7e-...`
4. `gh workflow run terraform-apply.yml -f targets='aws_db_instance.kitehub' -f dry_run=false -f confirm=APPLY`
5. Wait `DBInstanceStatus=available` (~10-15min cold start from snapshot)
6. Test connectivity via SSM: `aws ssm start-session --target i-05d7af46d01436b96` → `psql -h <rds-endpoint> -U kitehub` → `\dt kitehub.*` lists tables

**Pre-condition:**
- Phase A complete (EC2 running)
- Snapshot integrity check: `aws rds describe-db-snapshots --db-snapshot-identifier final-kitehub-postgresa9068e7e-...` → Status=available + EngineVersion match terraform code

**Verify post:**
- RDS endpoint reachable from EC2 SG
- Schema migrations applied (Flyway V1-V60+ table presence check)
- Sample data present (verify production data restored, not test data)

### Phase C — ALB recreate (HIGH risk, Tier 3)

**Goal:** Recreate `kitehub-alb` + listeners + target groups + 2 EIPs.

**Action sequence:**
1. Pre-apply audit artifact
2. `gh workflow run terraform-apply.yml -f targets='aws_lb.kitehub_alb,aws_lb_listener.kitehub_https,aws_lb_listener.kitehub_http,aws_lb_target_group.*,aws_eip.kitehub_alb*' -f dry_run=true`
3. Review plan + `dry_run=false confirm=APPLY`
4. Wait ALB `State.Code=active` (~3-5min)
5. Verify 2 EIP allocated + attached to ALB

**Per `concurrent-production-mutation-ops.md` §1:** strict serialization — Phase B RDS apply MUST complete + verified healthy before Phase C ALB apply begins.

**Verify post:**
- ALB DNS name resolvable
- Target group health checks (initially unhealthy — EC2 needs deploy fresh image OR rejoin target group)
- HTTPS listener :443 has valid ACM cert attached

### Phase D — Live smoke (LOW risk, Tier 1)

**Goal:** End-to-end smoke kitehub.me + api.kitehub.me HTTP 200.

**Action:**
1. Verify CloudFlare apex `kitehub.me` DNS resolves to ALB DNS name (curl -I kitehub.me → 200 OR 503)
2. Verify `api.kitehub.me/actuator/health` → 200 (may need EC2 deploy fresh image if 503)
3. Apply admin-login smoke per `release-deploy-standard.md` §3.1 + `pre-handoff-self-test-completeness.md` §2.4
4. If smoke fails on application layer (not infrastructure) — file follow-up gap, document deferred

### Phase E — Wave closure

**Goal:** Wave plan flip `status: complete` + 5-target sync.

**Acceptance criteria for Wave aws-restore-1 DONE:**
- [ ] EC2 3/3 running
- [ ] RDS available + schema verified
- [ ] ALB active + target group healthy (or documented blocker if app-layer issue)
- [ ] kitehub.me HTTP 200
- [ ] api.kitehub.me /actuator/health HTTP 200
- [ ] GAP-612 flipped → DONE (CSV + file + git mv to closed/)
- [ ] GAP-693 stays PARTIAL (SOP runbook deferred — follow-up wave per GAP-693 v2 scope refinement)
- [ ] 5-target sync (gap-status.csv + ROADMAP + wave-history.jsonl + MEMORY + session-handoff)

### Out-of-scope

- GAP-693 SOP runbook creation (13-step playbook + 4 scripts + meta-rule — defer follow-up wave aws-rebuild-sop-1)
- 13 cascade live walkthroughs (657/658/659/543/530/370/608/684/508/514/534/538/599/502) — defer Wave rst-cascade-1
- GAP-727 hasAccessToClass fix + GAP-730 idempotency port — defer Waves class-teacher-fix-1 + idempotency-finish-1
- GAP-533 Resend warm-up Day 1-7 user-action — defer parallel background

## 4. State-Check Evidence (per `audit-to-gap-pipeline.md` §2.6)

| Symbol | Type | Verification | Verdict |
|---|---|---|---|
| `documents/04-quality/gaps/phase-1-beta/GAP-612-aws-account-suspension-recovery.md` | Gap file | `ls` returns 1 file; Status PARTIAL 30% | ✅ exists |
| `documents/04-quality/gaps/phase-1-beta/GAP-693-aws-rebuild-sop-playbook.md` | Gap file | `ls` returns 1 file; Status OPEN BLOCKED | ✅ exists |
| `scripts/aws/start-stack.sh` | Restart script | `ls` returns 1 file executable | ✅ exists |
| `scripts/aws/stop-stack.sh` | Stop script (for rollback) | `ls` returns 1 file executable | ✅ exists |
| `infrastructure/terraform-aws/ec2.tf` + `ec2-kc-app.tf` | EC2 IaC | `ls` returns files | ✅ exists |
| `infrastructure/terraform-aws/rds.tf` | RDS IaC (need to verify snapshot_identifier setup) | Will read in Phase B pre-apply | 🆕 to-verify |
| `infrastructure/terraform-aws/alb.tf` (or similar) | ALB IaC | Will read in Phase C pre-apply | 🆕 to-verify |
| `.github/workflows/terraform-apply.yml` | Apply workflow | Used in Wave 63+ ; supports `targets` + `confirm=APPLY` | ✅ exists per `release-deploy-standard.md` §9 |
| AWS empirical state — account active | `aws sts get-caller-identity` | UserId AIDA5GAW3FUEDJ4ZZLVRK / Account 906286017800 | ✅ verified 2026-05-26 |
| AWS empirical — CloudTrail logging | `aws cloudtrail get-trail-status --name kitehub-main` | IsLogging=True | ✅ verified |
| AWS empirical — RDS snapshots available | `aws rds describe-db-snapshots` | 2 snapshots (final manual + auto) | ✅ verified |
| AWS empirical — EC2 stopped state | `aws ec2 describe-instances` | 3 stopped, EBS preserved | ✅ verified |

All blocking symbols verified. 2 `🆕 to-verify` items deferred to Phase B/C pre-apply audits — non-blocking for plan PR.

## 5. Verification Gates

Per `concurrent-production-mutation-ops.md` strict serialization + `pre-mutation-state-check.md`:

| Gate | Before | Pass criteria |
|---|---|---|
| Gate 1 | Phase A start | User explicit "tôi cho phép" / "claude trigger" Phase A; CloudTrail confirmed active; AWS sts OK |
| Gate 2 | Phase A → Phase B transition | 3 EC2 running + SSM reachable + internal HTTP 200 on localhost:8080/actuator/health |
| Gate 3 | Phase B `dry_run=true` review | Plan shows only RDS create (no unexpected drift); snapshot_identifier correct |
| Gate 4 | Phase B `dry_run=false` apply | User explicit Tier 3 authorization (per `dev-authorized-terraform-trigger.md`) |
| Gate 5 | Phase B → Phase C transition | RDS DBInstanceStatus=available + psql connectivity via SSM verified |
| Gate 6 | Phase C `dry_run=true` review | Plan shows only ALB+listeners+TG+EIP create (no unexpected drift) |
| Gate 7 | Phase C `dry_run=false` apply | User explicit Tier 3 authorization |
| Gate 8 | Phase C → Phase D transition | ALB State.Code=active + 2 EIP allocated |
| Gate 9 | Phase D closure | kitehub.me HTTP 200 + api.kitehub.me /actuator/health HTTP 200 + admin-login smoke pass |

**Banned:** triggering Phase B + Phase C in parallel (per `concurrent-production-mutation-ops.md` §1 — shared EC2 + RDS resources serialize mandatory).

## 6. Agent Spawn Pattern

**Coordinator-inline.** Per `agent-model-opus-default.md` §3 exception + per nature of work:
- Production mutation requires human-in-loop authorization per phase
- No parallelizable sub-tasks (each phase serializes)
- Coordinator owns state across phases (RDS endpoint → ALB target group → DNS)

NO agent spawn this wave.

## 7. Closure Protocol

1. Audit artifacts written per phase (B + C) → `documents/04-quality/audits/aws-verification/2026-05-26-wave-aws-restore-1-phase-{B,C}-{rds,alb}-{pre,post}-apply.md`
2. Audits-index.csv rows appended
3. Wave-history.jsonl entry appended với tag-based schema (`tag_primary: aws-restore`, `counter: 1`)
4. GAP-612 file Status: PARTIAL 30% → 🟢 DONE 100% (if all AC met) OR PARTIAL → higher % (if app-layer follow-up)
5. GAP-693 stays PARTIAL (SOP creation deferred — file follow-up scope refinement note in gap Log)
6. CSV row updates synced
7. ROADMAP §🎯 Current Status Snapshot — add "Wave aws-restore-1 SHIPPED 2026-05-26"
8. Session-handoff `documents/03-planning/session-handoffs/2026-05-26-wave-aws-restore-1-shipped-rst-cascade-queued.md`
9. Frontmatter `status: draft → complete` flip same closure PR
10. Worktree husk cleanup: N/A (coordinator-inline)

### Scope-Completeness Reconciliation (per `wave-closure-scope-completeness.md` §3)

Will populate at Phase E closure. Template:

| # | Plan §3 Scope item | Verdict | Follow-up |
|---|---|---|---|
| 1 | Phase A EC2 restart | TBD | — |
| 2 | Phase B RDS restore | TBD | — |
| 3 | Phase C ALB recreate | TBD | — |
| 4 | Phase D smoke verify | TBD | — |
| 5 | GAP-612 DONE flip | TBD | — |
| 6 | GAP-693 SOP creation | ❌ NOT-IMPLEMENTED | Defer Wave aws-rebuild-sop-1 (P1, ~3 days estimate) |

## 8. Log

- **2026-05-26 (status: draft):** Wave plan created. Source = Wave audit-stale-sweep-1 recommendation (file Wave aws-restore-1 BEFORE 4 hard-blocker waves to unblock 13 cascade flips). Empirical AWS state verified 2026-05-26: account ACTIVE + CloudTrail logging + EC2 3 stopped (data preserved) + RDS deleted (2 snapshots available) + ALB deleted (terraform recreate needed). Approach = coordinator-inline 4-phase serialization (A EC2 / B RDS / C ALB / D smoke) per `concurrent-production-mutation-ops.md`. User authorization required per phase per `release-deploy-standard.md` §9 + `dev-authorized-terraform-trigger.md` solo-dev override. GAP-693 SOP runbook deferred follow-up wave aws-rebuild-sop-1. Per `wave-tag-numbering-convention.md` v1.0.0 tag-based schema: `tag_primary: aws-restore`, `counter: 1`.

---

## 🛑 PHASE 0 STOP — AWAITING USER AUTHORIZATION

**Coordinator does NOT proceed to Phase A without explicit user direction.**

### User decision points

1. **Authorize Phase A?** (Tier 2 confirm — `bash scripts/aws/start-stack.sh` to restart 3 EC2)
2. **Snapshot choice for RDS restore?** Recommend `final-kitehub-postgresa9068e7e-...` (manual 2026-05-21). User confirms OR picks auto snapshot 2026-05-17.
3. **Single PR or separate PRs per phase?** Recommend single closure PR after all phases pass smoke (atomic restoration record).
4. **Defer GAP-693 SOP creation to follow-up wave?** Recommend YES (3-day scope vs ~2h restore execution).
5. **GAP-733 + GAP-727 + GAP-730 work parallel or post-aws-restore-1?** Per session-handoff recommendation — wait for aws-restore-1 done first to allow live cascade verification.
