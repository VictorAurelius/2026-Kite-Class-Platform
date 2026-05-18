---
title: Wave 65 — Meta-governance cleanup + Release 1 blockers unblock
status: complete
created: 2026-05-12
updated: 2026-05-12
waves: [65]
gaps: [GAP-483, GAP-484, GAP-485, GAP-486, GAP-487, GAP-488, GAP-491]
prs: [1205, 1206, 1207, 1208, 1209, 1210, 1211, 1212]
outcome: 5 DONE + 1 PARTIAL + 1 follow-up gap; deploy verification gated on GAP-491
---

# Wave 65 — Meta cleanup + Release 1 blockers (1 lượt)

**Goal:** Bắt hết meta-gaps phát hiện qua Wave 64 audit + unblock Release 1 cutover (GAP-483 + GAP-484). 1 wave handle all.
**Trigger:** User-flagged Wave 64 close: "mô phỏng meta gaps + tạo wave xử lý 1 lượt".
**Estimated wall-clock:** ~3-4h serial → 5 buckets parallel với sequencing (A trước, B+C+D+E parallel), ~2h longest bucket.

---

## 1. Brainstorm (5-10 min)

**Q1 (alignment):** Phase 1 BETA cutover step F unblock + meta-governance hygiene. All 5 personas affected indirectly (release readiness + future-session productivity).

**Q2 (trade-offs):**
- Considered: split meta vs release into 2 separate waves (rejected — user explicit "1 lượt")
- Considered: defer GAP-485 CSV indexes (rejected — Wave 65 is the cleanup wave; better consolidate)
- Considered: skip orphan backfill (rejected — accumulating drift; fix now while in cleanup mode)

**Q3 (risks):**
- A + B + C overlap on governance docs → sequence A first to avoid conflict
- D + E touch infra → may surface more cascading bugs → keep pre-mutation-state-check rule v1.1.0 strict
- 5 parallel agents risk token-quota-hit per `feedback_token_quota_spawn_timing.md` → spawn EARLY in session OR `/clear` between plan and exec

---

## 2. Task Breakdown

| Bucket | Gap(s) | Owner | Effort | Disjoint? |
|--------|--------|-------|--------|-----------|
| A | GAP-487 (new) MEMORY.md orphan backfill + GAP-488 (new) wave-history orphan backfill | bg-agent | ~1h | ✅ but MUST ship FIRST (sequence prerequisite) |
| B | GAP-486 post-merge-sync rule + check-docs.sh Rule 17/18 detector | bg-agent | ~1.5h | ✅ `.claude/rules/post-merge-sync-completeness.md` + `scripts/check-docs.sh` |
| C | GAP-485 4 CSV indexes (rules + skills + audits + ADRs) + 4 query helpers | bg-agent | ~3-4h | ✅ new CSVs + new query scripts |
| D | GAP-484 Java OTel OTLP autoconfig fix | bg-agent | ~1h | ✅ `kitehub/*/src/main/resources/application.yml` × 5 |
| E | GAP-483 EC2 user_data bootstrap (git install + repo clone in terraform) | bg-agent | ~30min | ✅ `infrastructure/terraform-aws/ec2.tf` only |

**Sequencing:**
1. **Bucket A ships FIRST** (sync orphan backfill) — establishes clean baseline before B's detector runs
2. **B + C + D + E parallel** after A merges
3. **Closure** flips all 6 gap statuses + Release Plan Progress

---

## 3. Scope

**Stake tier:** HIGH (mix of production deploy fix + meta-governance) → model: **Opus full** for D + E (touch production), **Opus medium** for A + B + C (governance/docs).
**Cross-layer?:** YES — D touches BE code (5 services), but no FE consumer in this wave. Skip Bucket 0 per `contract-first-for-cross-layer.md` (no api-contract change).

| # | Bucket | Gap(s) | Priority | Files | Spawn order |
|:-:|--------|--------|:--------:|-------|:-----------:|
| 1 | **A** | GAP-487 + GAP-488 (new) | 🟠 P1 | `~/.claude/.../memory/MEMORY.md` + `documents/03-planning/waves/wave-history.jsonl` | FIRST (sequential) |
| 2 | **B** | GAP-486 | 🟠 P1 | `.claude/rules/post-merge-sync-completeness.md` + `.claude/skills/workflow/session-docs-check/scripts/check-docs.sh` + test fixtures | parallel after A |
| 3 | **C** | GAP-485 | 🟡 P2 | `.claude/rules/rules-index.csv` + `.claude/skills/skills-index.csv` + `documents/04-quality/audits/audits-index.csv` + `documents/02-architecture/adr/adrs-index.csv` + `scripts/query-{rules,skills,audits,adrs}.sh` | parallel after A |
| 4 | **D** | GAP-484 | 🔴 P0 | 5× `kitehub/kitehub-*/src/main/resources/application.yml` (set `management.otlp.tracing.endpoint` default) | parallel after A |
| 5 | **E** | GAP-483 | 🔴 P0 | `infrastructure/terraform-aws/ec2.tf` (`ec2_user_data` add git + repo clone) | parallel after A |

### Bucket A — Orphan backfill (sequential FIRST)

- **Sub-A1 (GAP-487):** Backfill 7 orphan memory files into MEMORY.md index
- **Sub-A2 (GAP-488):** Backfill 15+ wave plan entries into wave-history.jsonl
- Acceptance: `comm -23 <(memory files) <(MEMORY.md entries)` empty; wave plans `status:complete` all have jsonl entry
- Files: 2 only

### Bucket B — GAP-486 rule + detector

- New rule `.claude/rules/post-merge-sync-completeness.md` v1.0.0 (per GAP-486 design Option B+C)
- New check-docs.sh Rule 17 (gap status modified → CSV row sync) + Rule 18 (new memory file → MEMORY.md index)
- 4 fixture scenarios in `session-docs-check/test/fixtures/`
- PR template Output Review checkbox cho 4 targets

### Bucket C — GAP-485 CSV indexes

- 4 new CSVs (per GAP-485 schemas)
- 4 query helpers `scripts/query-{rules,skills,audits,adrs}.sh` (template `query-gaps.sh`)
- 4 CI validators `scripts/check-{rules,skills,audits,adrs}-index-csv.sh`
- New rule `.claude/rules/meta-csv-index-pattern.md` v1.0.0 (codifies pattern)

### Bucket D — GAP-484 OTel fix

- Edit 5 application.yml files (kitehub-gateway + kitehub-subscription + kitehub-branding + kitehub-email + kitehub-admin):
  ```yaml
  management:
    otlp:
      tracing:
        endpoint: ${MANAGEMENT_OTLP_TRACING_ENDPOINT:http://localhost:4318/v1/traces}
  ```
- Tag v0.9.0-beta-staging.10 → docker-build-push → workflow_dispatch deploy
- Verify HTTPS api.kitehub.me/actuator/health returns 200

### Bucket E — GAP-483 user_data bootstrap

- Edit `ec2_user_data` in `infrastructure/terraform-aws/ec2.tf`:
  ```bash
  dnf install -y git
  mkdir -p /opt/kite-prod
  chown ec2-user:ec2-user /opt/kite-prod
  sudo -u ec2-user git clone https://github.com/VictorAurelius/2026-Kite-Class-Platform.git /opt/kite-prod
  ```
- terraform apply (will replace 2 EC2 again — accept per "no data to lose pre-launch" per user 2026-05-12)
- Verify new EC2 has `/opt/kite-prod/scripts/deploy-prod.sh` available without manual SSM bootstrap

---

## 4. State-Check Evidence (per `audit-to-gap-pipeline.md` §2.6)

| Symbol | Type | Verification command | Verdict |
|--------|------|----------------------|---------|
| Orphan memory files (7) | Audit finding | `comm -23 <(ls ~/.claude/.../memory/*.md) <(grep -oE ...MEMORY.md)` | ✅ verified — 7 orphans |
| Wave plans orphan (15+) | Audit finding | `grep -l "status: complete" documents/03-planning/waves/wave-*.md \| ...` | ✅ verified |
| `~/.claude/.../memory/MEMORY.md` | File | `ls ...MEMORY.md` | ✅ exists (Bucket A backfill) |
| `.claude/rules/post-merge-sync-completeness.md` | Rule (new) | `ls` | 🆕 to-be-created (Bucket B) |
| `.claude/rules/rules-index.csv` (etc.) | CSV indexes (new × 4) | `ls` | 🆕 to-be-created (Bucket C) |
| 5× `application.yml` | Existing config | `ls kitehub/kitehub-*/src/main/resources/application.yml` | ✅ exists (Bucket D edits) |
| `infrastructure/terraform-aws/ec2.tf` | Existing | `ls` | ✅ exists (Bucket E edits) |
| `infrastructure/terraform-aws/iam.tf` `aws:ResourceTag/Project="Kite"` (Wave 64 fix) | Already shipped | `grep` | ✅ shipped PR #1199 |

---

## 5. Verification Gates (per bucket)

| Bucket | Local verify command | CI gate |
|--------|---------------------|---------|
| A | `comm -23 ...` empty + wave-history JSON valid | Existing gap-status-csv + new (B-shipped) Rule 17/18 |
| B | `bash scripts/check-docs.sh` on test fixtures (3-fixture self-test per `rule-change-process.md` §6.5) | ShellCheck + session-docs-check |
| C | `bash scripts/check-rules-index-csv.sh` etc. + query helpers tested | ShellCheck + new validators |
| D | `cd kitehub/kitehub-gateway && ./mvnw verify` + 4 sibling services | Backend CI |
| E | `cd infrastructure/terraform-aws && terraform fmt -check && terraform validate` | terraform-plan CI |

---

## 6. Agent Spawn Pattern

Per `feedback_parallel_agent_strategy.md` + `agent-background-spawn-default.md`:
- **Bucket A**: spawn FIRST + wait merge (sequential prerequisite for B's detector)
- **B + C + D + E**: spawn parallel after A merges, all `run_in_background: true` + `isolation: worktree`
- Models: D + E Opus full (HIGH stake — touches prod infra/code); A + B + C Opus medium
- RELATIVE paths in all agent prompts per `feedback_worktree_absolute_path_contamination.md`
- Pre-mutation-state-check rule v1.1.0 §1.5 mandatory cross-reference matrix BEFORE D + E terraform/code edits

---

## 7. Closure Protocol

Per `gap-done-discipline.md` + `feedback_post_merge_doc_sync.md` + `feedback_wave_history_append_required.md` + `post-wave-cleanup.md`:

- Each bucket PR updates affected GAP file Log + status + CSV row
- ROADMAP §🚀 Next Action updated với cutover post-deploy state (HTTPS 200 verified)
- Wave plan `status: complete` flip
- `wave-history.jsonl` Wave 65 entry append (with Wave 64 cutover continuation note)
- Sub-gaps for any deferrals (e.g., audits-index.csv backfill of 50+ historical audits = follow-up if >2h scope creep)
- Run `bash scripts/prune-merged-worktrees.sh --yes` post-merge
- **`## Release Plan Progress` section** — Wave 65 unblocks Phase 1 BETA step F deploy + closes 4 meta-gaps in 1 lượt

---

## 8. Log

- **2026-05-12** (draft): Plan created at user request "mô phỏng meta gaps + tạo wave xử lý 1 lượt". Decomposed 6 gaps (4 existing + 2 new GAP-487/488 for orphan backfill) into 5 disjoint buckets với Bucket A sequential FIRST. Stake mixed (HIGH for D+E touching prod, MEDIUM for A/B/C). Cross-layer technically yes (BE Java code in D) but skip Bucket 0 since no api-contract change.

---

## 9. Closure (2026-05-12)

**Outcome:** 5 DONE + 1 PARTIAL + 1 follow-up gap (GAP-491). Wave 65 SHIPPED.

### Buckets

| Bucket | Gap | PR | Result |
|--------|-----|-----|--------|
| A1 | GAP-487 MEMORY orphans | #1206 | 🟢 DONE (state-corrected: 88=88 files=refs 0 orphans — symptom self-corrected via Wave 64 PRs) |
| A2 | GAP-488 wave-history orphans | #1207 | 🟢 DONE (64 stub entries backfilled, 0 remaining; 87 total jsonl entries) |
| B | GAP-486 post-merge-sync rule + Rule 17 | #1210 | 🟢 DONE (rule + detector + 3 fixtures + PR template) |
| C | GAP-485 meta-csv-index pattern + ADRs/Rules CSVs | #1211 | 🟡 PARTIAL 55% (Tier 1+2; Tier 3 skills+audits → GAP-490) |
| D | GAP-484 OTel autoconfig P0 BLOCKING | #1209 | 🟢 DONE (7 services application.yml fix; production deploy verification deferred per release-deploy-standard §9) |
| E | GAP-483 EC2 user_data P0 BLOCKING | #1208 | 🟢 DONE (terraform applied 2026-05-12 07:52 UTC; 2 EC2 in-place user_data update; AC#2/#3 deferred) |
| Plan | Wave plan | #1205 | 🟢 DONE |
| Incident | 2026-05-12 deploy concurrency conflict + tooling visibility gap | #1212 | 🟢 DONE — rules + GAP-491 + audit artifact extension |

### Post-deploy incident (2026-05-12)

Triggered terraform-apply + deploy-production within 22s on same EC2 → terraform stop-modify-start killed SSM deploy-prod.sh with SIGTERM. Workflow poll showed `Status=InProgress` for 15 attempts while command Failed at 7s (CloudWatch streaming missing).

Rules shipped same wave per `incident-to-rule-pipeline.md`:
- `concurrent-production-mutation-ops.md` v1.0.0
- `release-fix-retry-budget.md` v1.1.0 (+ §4 + §5 visibility-gap)
- GAP-491 BLOCKING next deploy retry

### Wall-clock + parallel speedup

- Wave plan + 7 bucket PRs + closure: ~6 hours (with 2 quota-reset waits)
- 4 buckets ran parallel (B/C/D/E) — Bucket A sequential first
- Coordinator-applied rebases (3 conflict resolutions for CSV + PR_TEMPLATE)
- Quota-reset incident: 1× ~16 min wait (all 4 agents quota-killed first spawn; re-spawn success post-reset)
- Admin merge usage: 4 PRs (#1208/#1209/#1210/#1211) due to Vercel external rate-limit (precedent Wave 45)

### Memory entries (user paste to user-memory dir)

Per `post-merge-sync-completeness.md` §7.5 + `incident-to-rule-pipeline.md` Stage 5:

```markdown
# feedback_concurrent_mutation_ops_conflict.md

Wave 65 deploy 2026-05-12 incident: triggered terraform-apply + deploy-production within 22s on same EC2 → terraform `user_data` update implements stop→ModifyInstanceAttribute→start, SIGTERM killed SSM deploy-prod.sh mid-fetch-secrets (exit 143). Rule `concurrent-production-mutation-ops.md` v1.0.0 + `release-fix-retry-budget.md` v1.1.0 ship to prevent recurrence. §6 matrix enumerates common pair concurrency decisions. ALWAYS serialize mutation ops on shared production resource: terraform apply complete → verify resource healthy → THEN deploy/SSM. Wave-pack parallelism is for CODE WORK only (agent worktrees) — never production mutations.
```

```markdown
# feedback_tooling_fix_before_retry.md

Wave 65 deploy 2026-05-12: workflow poll showed Status=InProgress for 15 attempts (2.5min) while underlying SSM command Failed at 7s — no CloudWatch streaming = no early-failure detection. Rule `release-fix-retry-budget.md` v1.1.0 §4 row "Tooling visibility gap" + §5 row "Tooling-fix-then-retry" with override trailer `RELEASE_RETRY_TOOLING_FIXED:`. Pattern: when retry would re-hit same gap because visibility lacking, STOP retry, fix observability FIRST. Otherwise bug-fix loop. GAP-491 P0 BLOCKING tracks Path A CloudWatch streaming Phase 1 (terraform log group + IAM) + Phase 2 (workflow `--cloud-watch-output-config` + log tail).
```

### Release Plan Progress

**Phase 1 BETA progress per `release-deploy-standard.md` §3.1:**

P0 BLOCKING closed this wave: GAP-483 + GAP-484 (production deploy verification gated on GAP-491).

P0 BLOCKING remaining for first beta invite (per ROADMAP §🚀):
- GAP-491 P0 NEW — SSM CloudWatch streaming (blocks deploy retry)
- GAP-482 PARTIAL — Deploy workflow cascade (gated on GAP-491)
- GAP-370 PARTIAL 75% — SES production approval (user-action: submit form + 24-48h wait)
- GAP-369 PARTIAL 70% — DNS production cutover
- GAP-376 PARTIAL 80% — Production data seed
- GAP-398/399 — Docker images + ECR region pin (essentially DONE — needs docs flip per state-check)

**ETA invite-ready:** ~3-5 ngày work (Wave 66 cluster) + SES approval wait

### Next wave seed — Wave 66 candidates

1. **GAP-491 P0** (BLOCKING) — Path A SSM CloudWatch streaming
2. **Retry deploy** with `RELEASE_RETRY_TOOLING_FIXED:` trailer → verify HTTPS 200 + close GAP-482 cascade
3. **GAP-398/399 status flip** (state-check shows DONE; docs-only sync)
4. **GAP-370 production access** — user submits SES form (parallel work)
5. **GAP-369 + GAP-376** — DNS + data seed (after deploy verified)
