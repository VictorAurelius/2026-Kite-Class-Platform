---
title: Wave 66 — Phase 1 BETA P0 cluster (CI fix + deploy preflight + docs flip sweep)
status: complete
created: 2026-05-12
updated: 2026-05-12
waves: [66]
gaps: [GAP-494, GAP-493, GAP-482, GAP-447, GAP-369, GAP-398, GAP-399]
---

<!-- wave-plan-completeness-exempt: Pre-Wave-76 legacy plan — predates current _TEMPLATE.md structure -->

# Wave 66 — Phase 1 BETA P0 cluster

**Goal:** Discharge 7 Phase 1 BETA P0/P2 gaps — 2 real implementation buckets (Lighthouse CI fix + GAP-493 Path B preflight) + 1 coordinator-driven docs-flip sweep for 5 gaps that state-check confirms are DONE / PARTIAL-with-§3-exit-ramp.

**Trigger:** Wave 65b shipped infra LIVE. Per `feedback_wave_plan_before_serial_prs.md`: candidate list has ≥3 disjoint sub-tasks → wave-pack. Per `audit-to-gap-pipeline.md` §2.6 hardened protocol (pre-flight state-check), 7 gap files read end-to-end + all referenced symbols verified BEFORE scope finalized.

**Estimated wall-clock:** ~2-3h total. Longest agent ~75min (Bucket A); coordinator docs sweep ~45min parallel.

---

## 1. Brainstorm

**Q1 (alignment):** Phase 1 BETA path-to-invite per `mvp-launch-plan-2026.md` §3 Phase 1. Each bucket discharges a P0 BLOCKING or P2 obligation. No persona expansion; pure infra+CI hardening before inviting 5 beta tenants (Wave 67-69).

**Q2 (trade-offs):**
- 6-agent fan-out (plan v1) vs 2-agent + 1-coordinator (plan v2): state-check revealed 5 of 7 gaps are docs-flip only (already DONE in prior waves but Log/CSV not synced). Spawning 5 agents for docs-flip work wastes agent-startup cost + creates 5 PR review burden. Coordinator-driven sweep cleaner.
- Plan v2 chose: 2 background agents (Bucket 0 + A) parallel + 1 coordinator-PR (Bucket Z bundling 5 gap flips). 3 PRs total vs 6.

**Q3 (risks):**
- Bucket A IAM change → could break deploy if mis-scoped. Mitigation: extend existing `github_deploy_inline` policy (single Sid addition), not new role. Audit artifact per `pre-mutation-state-check.md`.
- Bucket 0 root cause hypothesis in GAP-494 already disproven by state-check (lighthouse.yml has correct order). Real fix likely needs `cache: 'pnpm'` removal + manual cache pattern matching `frontend-ci.yml`. Investigation bucket, not mechanical.
- Bucket Z docs flip — risk = flipping gap DONE when AC not actually verified. Mitigation: each gap requires §4 state-check evidence cited in Log entry per `gap-done-discipline.md` §2.

---

## 2. Task Breakdown

| Bucket | Gap(s) | Owner | Effort | Disjoint? |
|--------|--------|-------|--------|-----------|
| 0 | GAP-494 | bg-agent (Opus medium) | ~45min | ✅ `.github/workflows/lighthouse.yml` only |
| A | GAP-493 Path B | bg-agent (Opus 4.7) | ~75min | ✅ `.github/workflows/deploy-production.yml` + `infrastructure/terraform-aws/iam.tf` (github_deploy_inline only) |
| Z | GAP-369, 398, 399, 447, 482 | coordinator (this session) | ~45min | ✅ docs only — 5 gap files + CSV + ROADMAP |

**Stake tier per `wave-pack-planner` Step 4.6:**
- Bucket 0: LOW (single workflow file edit, advisory gate, revertable)
- Bucket A: MEDIUM-HIGH (IAM + production deploy workflow; mitigated by Tier 1 state-check + audit artifact + workflow_dispatch human-trigger per `release-deploy-standard.md` §9)
- Bucket Z: LOW (docs only; CSV validator catches schema errors)

**Cross-layer per Step 4.5:** NO — zero FE+BE shared contract. Bucket 0 Foundation skipped.

**Disjoint check:** 3 buckets touch disjoint path sets (workflow file / iam.tf section / docs+csv). No collision.

---

## 3. Scope

> Gap referencing convention per `gap-architecture-v2.md`: canonical ids from `gap-status.csv` verified via `bash scripts/query-gaps.sh <prefix>`. All 7 gap files read end-to-end before this scope was finalized.

### Bucket 0 — Lighthouse CI cache fix (GAP-494)

- File: `.github/workflows/lighthouse.yml` (RELATIVE)
- **State-check finding (this plan §4):** workflow ALREADY has `pnpm/action-setup@v6` BEFORE `actions/setup-node@v6`. GAP-494 proposed fix (add pnpm/action-setup BEFORE setup-node) is already in place. Yet workflow fails twice consecutively (runs 2026-05-12 17:01 + 2026-05-12 02:34). Real root cause: `setup-node@v6` uses built-in `cache: 'pnpm'` which expects pnpm in PATH at cache-setup time. `frontend-ci.yml` (which PASSES on the same PRs) uses **inverse order** (setup-node FIRST, pnpm AFTER) and **no built-in cache** (manual cache step) — see §4 evidence.
- Acceptance:
  - Align lighthouse.yml to frontend-ci.yml pattern: remove `cache: 'pnpm'` from setup-node; add explicit `pnpm install` (no cache) OR add manual `actions/cache` step keyed on pnpm-lock.yaml hash
  - Test PR (edit `kitehub/kitehub-frontend/README.md` trailing whitespace) → Lighthouse runs to completion past Setup Node step
  - GAP-494 file: filename correction (`lighthouse-ci.yml` → `lighthouse.yml`) + AC checked + flip DONE
  - 4-target sync per `post-merge-sync-completeness.md`: gap file + CSV row + ROADMAP §🚀 + (no wave-history since not wave-scoped overflow, just bucket-scoped)

### Bucket A — Deploy preflight + IAM RDS describe (GAP-493 Path B)

- Files (RELATIVE):
  - `.github/workflows/deploy-production.yml` — add preflight job per gap §"Path B"
  - `infrastructure/terraform-aws/iam.tf` — extend `github_deploy_inline` policy with `rds:DescribeDBInstances`
- **State-check finding (§4):** `iam.tf` ALREADY has `rds:DescribeDBInstances` but in `github_tier_3_cutover_inline` policy (line 575), NOT in `github_deploy_inline`. Bucket A needs to add to `github_deploy_inline` (deploy role used by deploy-production.yml).
- Tests:
  - `terraform fmt` + `terraform validate` in `infrastructure/terraform-aws/`
  - `python3 -c "import yaml; yaml.safe_load(open('.github/workflows/deploy-production.yml'))"`
- Acceptance:
  - Preflight job ships per gap §"Path B" YAML template — fails fast (<30s) with actionable message if RDS not available
  - `iam.tf` `github_deploy_inline` gains `rds:DescribeDBInstances` (existing `Ec2Describe` Sid OR new `RdsDescribe` Sid)
  - Pre-mutation audit artifact `documents/04-quality/audits/aws-verification/2026-05-12-gap-493-path-b-preflight.md` per `pre-mutation-state-check.md` §3
  - Apply via `workflow_dispatch terraform-apply.yml` with `confirm=APPLY` (human-triggered per `release-deploy-standard.md` §9; NOT agent-initiated)
  - Verify: trigger deploy when RDS stopped → preflight fails clean; restart RDS → deploy succeeds
  - GAP-493 file + CSV flip DONE (Path A + B both complete); 4-target sync

### Bucket Z — Coordinator-driven docs-flip sweep (5 gaps)

Single PR covering 5 gaps whose state-check confirms work is DONE or PARTIAL-with-§3-exit-ramp. Coordinator drives because each flip is ≤10min mechanical work + cross-file consistency easier in one diff. NOT spawned as agent (per `feedback_coordinator_ci_fix_pattern.md` mechanical-only ≤30 LOC ≤3 files extended here to docs).

Per `gap-done-discipline.md` §2: each flip cites §4 state-check evidence in Log entry.

| Gap | Current CSV | Target | Evidence basis |
|-----|-------------|--------|----------------|
| GAP-369 (DNS Phase 1 BETA) | PARTIAL 70% | DONE 100% (Phase 2 `.vn` deferred as separate concern) | §4: ALB HTTPS:443 + HTTP:80→HTTPS redirect + ACM `*.kitehub.me` ISSUED + CF proxy live (verified `curl -sI` returns Cloudflare server) |
| GAP-398 (Docker build) | PARTIAL 50% | DONE 100% | §4: ECR has 10 `kite/<service>` repos; per GAP-482 Wave 64 Log "Docker images pushed v0.9.0-beta-staging.9 (10 services)" |
| GAP-399 (ECR region) | PARTIAL 50% | DONE 100% | §4: zero `us-east-1` config references (only 2 in `cloudtrail.tf` COMMENTS); ECR repos confirmed in `ap-southeast-1` |
| GAP-447 (EC2 right-size) | PARTIAL 50% | PARTIAL 75% — kh_backend ✅ DONE, kc_app drift tracked GAP-450 (separate), CWAgent install deferred to user (manual SSM = `agent-aws-access.md` §4.3 Tier 3) | §4: EC2 `kitehub-kh-backend` running on t3.medium per snapshot; kc_app drift per GAP-447 Log + GAP-450 |
| GAP-482 (Deploy IAM tag + hardcoded ID) | PARTIAL 70% | PARTIAL 95% — code fixes PR #1199+#1200 shipped, E2E verification gated on Bucket A merge (next session triggers deploy-production.yml dry_run=false) | §4: deploy E2E pending Bucket A; can't verify without first running |

- Files (RELATIVE):
  - `documents/04-quality/gaps/GAP-369-production-dns-domain-setup.md` (status + Log + AC checks)
  - `documents/04-quality/gaps/GAP-398-docker-build-kitehub-services-6-modules.md` (status + Log)
  - `documents/04-quality/gaps/GAP-399-ecr-region-pin-ap-southeast-1.md` (status + Log)
  - `documents/04-quality/gaps/GAP-447-right-size-ec2-post-vercel-pivot.md` (status + Log; PARTIAL exit-ramp)
  - `documents/04-quality/gaps/GAP-482-deploy-workflow-iam-tag-and-hardcoded-instance.md` (status + Log; AC partial check)
  - `documents/04-quality/gaps/gap-status.csv` (5 rows)
  - `documents/04-quality/gaps/ROADMAP.md` §🚀 Next Action (post-Wave-66 outlook)
- Acceptance per `post-merge-sync-completeness.md` Rule 17:
  - Each Status flip in gap file matches `gap-status.csv` row update in same diff
  - State-check evidence cited in each Log entry per `gap-done-discipline.md` §2
  - PARTIAL flips name follow-up gap (kc_app drift = GAP-450; CWAgent = user-action no gap needed)

---

## 4. State-Check Evidence (BẮT BUỘC per `audit-to-gap-pipeline.md` §2.6 hardened protocol)

All commands run before scope finalized. NO `| head` truncation per hardened protocol.

| Symbol | Type | Verification command | Evidence | Verdict |
|--------|------|----------------------|----------|---------|
| `.github/workflows/lighthouse.yml` | CI workflow | `ls .github/workflows/lighthouse.yml` | 1 file, 3.0K, 100 lines | ✅ exists |
| `.github/workflows/lighthouse-ci.yml` | CI workflow (gap references this name) | `ls .github/workflows/lighthouse-ci.yml` | NOT FOUND | ❌ name in GAP-494 wrong (filename correction part of Bucket 0 AC) |
| `pnpm/action-setup` order in lighthouse.yml | YAML step order | `grep -nE "pnpm/action-setup\|setup-node\|cache:" .github/workflows/lighthouse.yml` | line 37 pnpm-setup@v6 BEFORE line 42 setup-node@v6 (with `cache: 'pnpm'` line 45) | ✅ already correctly ordered — GAP-494 root-cause hypothesis wrong; real fix = remove `cache: 'pnpm'` |
| `pnpm/action-setup` order in frontend-ci.yml | YAML step order (control) | same grep on frontend-ci.yml | line 32 setup-node@v6 BEFORE line 37 pnpm-setup@v6, NO `cache:` option, manual cache step | ✅ control case — Lighthouse should match this pattern |
| Lighthouse recent runs | gh CLI | `gh run list --workflow=lighthouse.yml --limit 3 --json conclusion,createdAt` | 2 of 3 most recent = failure (2026-05-12T17:01, 2026-05-12T02:34) | ✅ failure confirmed (Bucket 0 needed) |
| `.github/workflows/deploy-production.yml` | CI workflow | `ls .github/workflows/deploy-production.yml` | 1 file, 9.9K | ✅ exists |
| Preflight job in deploy-production.yml | grep | `grep -c "preflight\|kitehub-postgres\|rds:DescribeDB" .github/workflows/deploy-production.yml` | 0 matches | 🆕 to-be-created (Bucket A) |
| `infrastructure/terraform-aws/iam.tf` | Terraform | `ls infrastructure/terraform-aws/iam.tf` | 26.3K | ✅ exists |
| `github_deploy_inline` policy | IAM resource | `grep -n "github_deploy_inline" infrastructure/terraform-aws/iam.tf` | line 286 | ✅ exists |
| `rds:DescribeDBInstances` in github_deploy_inline | grep + role mapping | `awk` Sid-to-role mapping (see §scope) | `rds:DescribeDBInstances` exists ONLY in `github_tier_3_cutover_inline` (line 575), NOT in `github_deploy_inline` | 🆕 to-add (Bucket A) |
| V34 migration files | Flyway | `find . -path ./node_modules -prune -o -name "V34*" -print` | 2 files: `kitehub-subscription/V34__enable_rls_tenant_scoped_tables.sql` (Wave 56 GAP-466) + `kiteclass-core/V34__create_rebrand_approvals_table.sql` | ✅ exists (V34 checksum mismatch from GAP-493 was RESOLVED in Path A by schema drop — NOT relevant to Bucket A scope) |
| EC2 `kitehub-kh-backend` t3.medium | AWS describe | session-start snapshot | running, type per GAP-447 Log = t3.medium | ✅ exists (GAP-447 kh_backend already right-sized) |
| EC2 `kitehub-kc-app` | AWS describe | session-start snapshot | running but `i-007b72fffc6dcad22` replaced 2026-05-12 04:11 → drift per GAP-450 | ⚠️ drift tracked separately |
| RDS `kitehub-postgres` available | AWS describe | session-start snapshot | available | ✅ exists |
| ALB `kitehub-alb` HTTPS:443 | AWS elbv2 describe-listeners | `aws elbv2 describe-listeners` | HTTPS:443 + HTTP:80 listeners both present | ✅ GAP-369 SSL listener confirmed live |
| ACM cert `*.kitehub.me` | AWS acm list | `aws acm list-certificates` | `*.kitehub.me` ISSUED | ✅ GAP-369 cert confirmed |
| `curl -sI http://api.kitehub.me` | HTTP redirect | shell | 301 → https://api.kitehub.me/ via Cloudflare | ✅ GAP-369 HTTPS redirect confirmed |
| ECR repos in ap-southeast-1 | AWS ecr describe | `aws ecr describe-repositories --region ap-southeast-1` | 10 `kite/<service>` repos | ✅ GAP-398 + GAP-399 confirmed (5 KH + 3 KC + 2 frontend) |
| `us-east-1` references in workflows + terraform | grep | `grep -l "us-east-1" .github/workflows/*.yml infrastructure/terraform-aws/*.tf` | 1 file (`cloudtrail.tf` lines 9+130 — COMMENTS only, zero config) | ✅ GAP-399 confirmed clean |

**Banned shortcuts respected:** zero `| head` truncation on any state-check command. All verifications surface FULL output for inspection.

**Honest framing:** This v2 plan corrects 3 errors in v1 (V34 absent, iam-deploy.tf nonexistent, GAP-399 misclassified) by reading actual gap files first instead of relying on closure-PR summaries. Per `audit-to-gap-pipeline.md` §2.6: wave plan state-check MUST verify every symbol BEFORE plan ships, not after PR opened.

---

## 5. Verification Gates

| Bucket | Local verify | CI gate |
|--------|--------------|---------|
| 0 | edit lighthouse.yml → trigger via test PR (whitespace edit in `kitehub/kitehub-frontend/README.md`) → `gh run watch` Lighthouse | Lighthouse workflow runs past Setup Node step (whether passes scores = separate concern, gate is advisory per workflow comment) |
| A | `terraform fmt` + `terraform validate` clean; `terraform-plan.yml workflow_dispatch` shows IAM-only update; YAML lint clean | terraform-plan job green; deploy-production smoke after Bucket A merge (separate session) |
| Z | `bash scripts/check-gap-status-csv.sh` green (Phase 2 mode `GAP_FILES_OPTIONAL=false`); all 5 Log entries cite §4 evidence | `gap-status-csv` CI green; `meta-csv-indexes` validator green |

---

## 6. Agent Spawn Pattern

Per `feedback_parallel_agent_strategy.md` + `agent-background-spawn-default.md`:

| Bucket | Spawn mode | Model | Timing |
|--------|-----------|-------|--------|
| 0 | `Agent(isolation:worktree, run_in_background:true, subagent_type:general-purpose)` | Opus medium | T+0 (parallel with A) |
| A | `Agent(isolation:worktree, run_in_background:true, subagent_type:general-purpose)` | Opus 4.7 (default — MEDIUM-HIGH stake) | T+0 (parallel with 0) |
| Z | Coordinator (this session) — NO agent spawn | n/a | T+0 (parallel; coordinator can work while 0+A run background) |

RELATIVE paths in agent prompts per `feedback_worktree_absolute_path_contamination.md`.

Concurrency guard (per `concurrent-production-mutation-ops.md`): Bucket A's `terraform-apply.yml workflow_dispatch` is post-bucket-A-PR-merge user-action — NOT triggered during agent run. Zero concurrent-mutation risk.

---

## 7. Closure Protocol

Per `gap-done-discipline.md` + `post-merge-sync-completeness.md` Rule 17 + `feedback_wave_history_append_required.md` + `post-wave-cleanup.md`:

- Each PR (0, A, Z): gap file + CSV row + ROADMAP §🚀 sync in same diff
- Wave closure PR (separate, after all 3 merge):
  - Wave plan frontmatter `status: complete`
  - `wave-history.jsonl` append (this wave entry)
  - `bash scripts/prune-merged-worktrees.sh --yes` before drafting
  - `## Release Plan Progress` section per `feedback_wave_closure_release_progress_report.md` with Waves Remaining table

### Path-to-invite — Waves Remaining

| Wave | Strict-min v0.9.0-beta | Practical v0.9.0-beta | v1.0.0 PROD |
|------|------------------------|----------------------|-------------|
| 66 (this) | GAP-493/494 close + 5 docs flips | + Bucket A E2E verify (sets up Wave 67) | — |
| 67 | GAP-376 prod data seed + GAP-412 AWS Activate D+14 cutover + GAP-482 final E2E | + dashboard polish | — |
| 68 | GAP-370 SES production approval + GAP-372 invite mechanism | + smoke E2E | + audit /100 ≥80 |
| 69 | rollback drill + GAP-447 final (CWAgent install user-action) | pre-launch acceptance + 5 beta tenant invites | final audit /100 ≥85 |

Phase 1 → 2 trigger gates per CLAUDE.md: audit /100 ≥80 + 5 beta tenants live + 0 P0 incidents 2 weeks.
