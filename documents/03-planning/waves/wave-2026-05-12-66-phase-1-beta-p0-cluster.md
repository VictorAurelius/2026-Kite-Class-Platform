---
title: Wave 66 — Phase 1 BETA P0 cluster (deploy preflight + workflow unblock + right-size + DNS cleanup + docs flip)
status: draft
created: 2026-05-12
updated: 2026-05-12
waves: [66]
gaps: [GAP-494, GAP-493, GAP-482, GAP-447, GAP-369, GAP-398, GAP-399]
---

# Wave 66 — Phase 1 BETA P0 cluster

**Goal:** Discharge 6 Phase 1 BETA P0/P2 buckets in parallel — push remaining PARTIAL gaps toward DONE, unblock invite-readiness path Wave 67-69.
**Trigger:** Wave 65b shipped infra LIVE (api.kitehub.me HTTP 200). Now 6 disjoint P0 gaps remain on Phase 1 BETA critical path; per `feedback_wave_plan_before_serial_prs.md` ≥3 disjoint sub-tasks → wave-pack instead of serial.
**Estimated wall-clock:** ~3-4h agent work, longest-bucket ~90min (Bucket A GAP-493 Path B).

---

## 1. Brainstorm

**Q1 (alignment):** Phase 1 BETA path-to-invite per `mvp-launch-plan-2026.md` §3 Phase 1 — every bucket closes a P0 BLOCKING / hardening gap. No persona scope expansion; pure infra+deploy hardening before inviting 5 beta tenants (Wave 68-69).

**Q2 (trade-offs):**
- Wave 66 cluster vs serial-PR queue: cluster (~3-4h) vs ~6-8h serial → 2x saving per Wave Observability retro.
- Bucket B GAP-482 close-gate depends on Bucket E GAP-484 OTel fix ALREADY DONE 2026-05-12 → no intra-wave dependency.
- Bucket A GAP-493 Path B (preflight job + V34 audit) deferred from Wave 65b because Path A already restored HTTP 200; Path B is hardening, can ship parallel.
- Bucket 0 GAP-494 originally tagged "~5min mechanical" but state-check reveals `lighthouse.yml` already has correct ordering yet still fails → investigation bucket, NOT mechanical.

**Q3 (risks):**
- GAP-493 Path B IAM change → risk of breaking deploy. Mitigation: test on staging first, document state-check.
- GAP-447 t3.medium stress test → may discover memory pressure on real workload. Mitigation: rollback to m7i-flex.large documented; AWS Activate D+14 cutover (Wave 67) re-evaluates.
- GAP-398/399 "docs flip" assumption may be wrong if state-check shows incomplete. Each bucket includes state-check as first step.

---

## 2. Task Breakdown

| Bucket | Gap(s) | Owner | Effort | Disjoint? |
|--------|--------|-------|--------|-----------|
| 0 | GAP-494 | bg-agent (Opus medium) | ~30min investigation + 15min fix | ✅ `.github/workflows/lighthouse.yml` only |
| A | GAP-493 Path B | bg-agent (Opus 4.7) | ~90min | ✅ `.github/workflows/deploy-production.yml` + IAM terraform + Flyway audit |
| B | GAP-482 close | bg-agent (Opus medium) | ~30min | ✅ docs + verify E2E (depends on Bucket A staging clean) |
| C | GAP-447 | bg-agent (Opus 4.7) | ~60min | ✅ terraform `ec2.tf` + stress test |
| D | GAP-369 Phase 2 | bg-agent (Opus medium) | ~30min | ✅ Cloudflare DNS + docs only |
| E | GAP-398 + GAP-399 docs flip | bg-agent (Opus medium) | ~20min | ✅ docs + CSV flip after state-check |

**Disjoint check:** Buckets touch separate files (workflow / terraform / docs / cloudflare). Bucket B verification depends on Bucket A completion → spawn B AFTER A green.

**Stake tier:** MEDIUM (deploy-touching but hardening, no new feature) → default Opus medium, escalate Bucket A+C to Opus 4.7 (terraform mutation + stress test high-risk).

**Cross-layer?** NO — pure DevOps/infra. Bucket 0 Foundation skipped per `contract-first-for-cross-layer.md` (no FE+BE shared contract).

---

## 3. Scope

> **Gap referencing:** canonical ids from `gap-status.csv` confirmed via `bash scripts/query-gaps.sh <prefix>`.

| # | Bucket | Gap(s) | Priority | Files (glob) | Spawn order |
|:-:|--------|--------|:--------:|--------------|:-----------:|
| 0 | **Lighthouse CI fix** | GAP-494 | 🟡 P2 | `.github/workflows/lighthouse.yml` | parallel with A/C/D/E |
| A | **Deploy preflight + V34 audit** | GAP-493 Path B | 🔴 P0 | `.github/workflows/deploy-production.yml` + `infrastructure/terraform-aws/iam-deploy.tf` + `kitehub/kitehub-platform/src/main/resources/db/migration/` | spawn first (longest) |
| B | **GAP-482 close** | GAP-482 | 🔴 P0 | gap file + ROADMAP + CSV + smoke E2E verify | AFTER A green |
| C | **EC2 right-size** | GAP-447 | 🔴 P0 | `infrastructure/terraform-aws/ec2.tf` + `documents/04-quality/audits/aws-verification/` + GAP file | parallel with A/0/D/E |
| D | **DNS Phase 2 cleanup** | GAP-369 | 🔴 P0 | Cloudflare DNS records + GAP file + ROADMAP | parallel with A/0/C/E |
| E | **Docker + ECR docs flip** | GAP-398, GAP-399 | 🔴 P0 | gap files + CSV + state-check evidence | parallel with A/0/C/D |

### Bucket 0 — Lighthouse CI investigation + fix

- Files: `.github/workflows/lighthouse.yml` (RELATIVE)
- **State-check finding (this plan):** workflow ALREADY has `pnpm/action-setup@v6` BEFORE `actions/setup-node@v6`, yet fails. GAP-494 root-cause hypothesis WRONG. Investigation needed:
  - Compare `lighthouse.yml` vs `frontend-ci.yml` (which passes)
  - Verify `pnpm/action-setup@v6` version match with `frontend-ci.yml`
  - Check `cache-dependency-path` resolution
- Acceptance: workflow runs Setup Node step clean; gap renamed to actual file (`lighthouse.yml` not `lighthouse-ci.yml`); AC checked + GAP flipped DONE.

### Bucket A — Deploy preflight + V34 audit (GAP-493 Path B)

- Files: `.github/workflows/deploy-production.yml` + `infrastructure/terraform-aws/iam-deploy.tf` + migration audit
- Tests: terraform plan clean; deploy dry-run shows preflight job; V34 file audit documented
- Acceptance:
  - Preflight job added before SSM step (checks RDS `available` via `aws rds describe-db-instances`)
  - IAM role gains `rds:DescribeDBInstances` permission
  - V34 file audit: find which migration drifted causing Flyway checksum mismatch on previous deploy
  - Audit artifact `documents/04-quality/audits/aws-verification/2026-05-12-gap-493-path-b-preflight.md` per `pre-mutation-state-check.md`
- Apply via `workflow_dispatch terraform-apply.yml` with `confirm=APPLY` (human-triggered per `release-deploy-standard.md` §9)

### Bucket B — GAP-482 close

- Files: `documents/04-quality/gaps/GAP-482-*.md` + `gap-status.csv` + `ROADMAP.md`
- Acceptance:
  - Spawn AFTER Bucket A merges (so deploy E2E with new preflight is testable)
  - Run `gh workflow run deploy-production.yml -f confirm=APPLY -f dry_run=false` end-to-end → success
  - Gap §AC checked; CSV flip DONE; Log entry citing PR
- Tier-1 read-only verification per `agent-aws-access.md` §2.1

### Bucket C — EC2 right-size m7i-flex.large → t3.medium

- Files: `infrastructure/terraform-aws/ec2.tf` (RELATIVE)
- State-check current: both EC2 instances `running` (per AWS snapshot session-start)
- Acceptance:
  - terraform plan shows `aws_instance.kh_backend` + `aws_instance.kc_app` instance_type update only (no replace)
  - Pre-mutation audit artifact per `pre-mutation-state-check.md` §3 — must include real-vs-phantom analysis
  - Per `concurrent-production-mutation-ops.md`: this Bucket MUST NOT run concurrently with Bucket A workflow_dispatch — serialize: A complete → verify EC2 running → C apply
  - Stress test: `curl` /api/v1/health 50x parallel; monitor CloudWatch CPU/memory; document baseline
  - Audit artifact `documents/04-quality/audits/aws-verification/2026-05-12-gap-447-right-size.md`

### Bucket D — DNS Phase 2 cleanup (GAP-369)

- Files: Cloudflare DNS via wrangler/MCP + `documents/04-quality/gaps/GAP-369-*.md`
- State-check current: GAP-369 70% PARTIAL — DNS bind live; SSL strict + Always HTTPS pending per CSV notes
- Acceptance:
  - Enable Cloudflare SSL strict mode + Always Use HTTPS via API per `pre-mutation-state-check.md`
  - Verify `curl -sI https://api.kitehub.me` shows HTTPS-only + HSTS
  - Audit artifact `documents/04-quality/audits/cloudflare-verification/2026-05-12-gap-369-ssl-strict.md`
  - CSV flip → DONE 100%

### Bucket E — GAP-398/399 state-check + docs flip

- Files: gap files + `gap-status.csv` only
- State-check first (per `audit-to-gap-pipeline.md` §2.8):
  - GAP-398 Docker build status: query ECR repo + verify 5 modules pushed
  - GAP-399 region pin: grep `us-east-1` in all `.tf` + `.github/workflows/*`; verify zero matches OR documented exception
- Acceptance:
  - If state-check shows fully implemented → flip DONE with state-check evidence in Log
  - If still partial → file follow-up sub-gap, keep PARTIAL per `gap-done-discipline.md` §3

---

## 4. State-Check Evidence

| Symbol | Type | Verification command | Evidence | Verdict |
|--------|------|----------------------|----------|---------|
| `.github/workflows/lighthouse.yml` | CI workflow | `ls .github/workflows/lighthouse.yml` | 1 file (3.0K) | ✅ exists (GAP-494 references wrong name `lighthouse-ci.yml` — investigation bucket 0) |
| `pnpm/action-setup@v6` in lighthouse.yml | Action step | `grep -n "pnpm/action-setup" .github/workflows/lighthouse.yml` | line 35-37 (already present) | ✅ exists (root cause hypothesis WRONG → Bucket 0 investigation) |
| `.github/workflows/deploy-production.yml` | CI workflow | `ls .github/workflows/deploy-production.yml` | 1 file (9.9K) | ✅ exists |
| `infrastructure/terraform-aws/iam-deploy.tf` | Terraform | `ls infrastructure/terraform-aws/iam-deploy.tf` | (to verify by agent A — paired with `iam-roles.tf` family) | ⚠️ to-verify (Bucket A first step) |
| `infrastructure/terraform-aws/ec2.tf` | Terraform | `ls infrastructure/terraform-aws/ec2.tf` | (to verify by agent C) | ⚠️ to-verify (Bucket C first step) |
| `V34__*.sql` migration | Flyway file | `find kitehub kiteclass -path "*/db/migration/V34*"` | **0 matches** (latest is V33 in subscription + core) | ❌ absent (Bucket A audit must find which file/checksum drifted) |
| EC2 `kitehub-kh-backend` running | AWS resource | session-start AWS snapshot | `running` | ✅ exists |
| EC2 `kitehub-kc-app` running | AWS resource | session-start AWS snapshot | `running` | ✅ exists |
| RDS `kitehub-postgres` available | AWS resource | session-start snapshot | `available` | ✅ exists |
| Cloudflare zone `kitehub.me` | DNS zone | wrangler/MCP query at exec | (Bucket D state-check first) | ⚠️ to-verify |
| `GAP-484` OTel fix | Predecessor gap | CSV query | DONE 100% | ✅ exists — unblocks Bucket B |

**Banned shortcuts respected:** zero `| head` truncation on grep/find in this plan. Bucket 0 finding (V34 absent + lighthouse.yml already correctly ordered) explicitly surfaced — agents must investigate, not assume.

---

## 5. Verification Gates

| Bucket | Local verify | CI gate |
|--------|--------------|---------|
| 0 | edit lighthouse.yml → test PR with FE README touch → workflow runs clean | `lighthouse` workflow green |
| A | `terraform fmt && terraform validate` in `infrastructure/terraform-aws/`; `terraform-plan.yml` workflow_dispatch clean | terraform-plan job green |
| B | `gh workflow run deploy-production.yml -f confirm=APPLY -f dry_run=false` → success; smoke `curl https://api.kitehub.me` | deploy-production workflow green |
| C | `terraform plan` shows instance_type update only; stress test documented | terraform-plan green + audit artifact present |
| D | `curl -sI https://api.kitehub.me` shows HTTPS-only + HSTS | n/a (Cloudflare side) |
| E | grep state-check evidence committed; CSV validator green | `gap-status-csv` CI green |

---

## 6. Agent Spawn Pattern

- All buckets `run_in_background: true` per `agent-background-spawn-default.md`
- `isolation: worktree` for parallel safety
- RELATIVE paths in prompts per `feedback_worktree_absolute_path_contamination.md`
- Bucket A spawns FIRST (longest); 0/C/D/E spawn parallel with A; B spawns AFTER A merges (deploy E2E verification needs new preflight)
- Per `concurrent-production-mutation-ops.md`: serialize Bucket A workflow_dispatch and Bucket C terraform apply (both touch EC2 — see Bucket C acceptance)

---

## 7. Closure Protocol

Per `gap-done-discipline.md` + `feedback_post_merge_doc_sync.md` + `post-merge-sync-completeness.md` + `feedback_wave_history_append_required.md` + `post-wave-cleanup.md`:

- Each bucket PR: gap file Log + CSV row + ROADMAP §🚀 in same diff (4-target sync rule 17)
- Wave closure PR: `wave-history.jsonl` append + wave plan `status: complete` flip
- Run `bash scripts/prune-merged-worktrees.sh --yes` before drafting closure PR
- Audit artifacts: 3 expected (Bucket A preflight + Bucket C right-size + Bucket D Cloudflare SSL)
- **`## Release Plan Progress` section in closure PR** — Phase 1 BETA path-to-invite progress + Waves Remaining table (Wave 67-69 per `feedback_wave_closure_release_progress_report.md`)

### Path-to-invite — Waves Remaining

| Wave | Strict-min v0.9.0-beta | Practical v0.9.0-beta | v1.0.0 PROD |
|------|------------------------|----------------------|-------------|
| 66 (this) | P0 cluster discharge | P0 cluster discharge | — |
| 67 | GAP-376 data seed + GAP-412 AWS Activate D+14 cutover | + dashboard polish | — |
| 68 | GAP-370 SES production approval + GAP-372 invite mechanism | + smoke E2E | + audit /100 ≥80 |
| 69 | rollback drill | pre-launch acceptance | final audit + counsel review (Phase 3 K-12 only — N/A Phase 1 BETA) |

Phase 1 → 2 trigger gates per CLAUDE.md: audit /100 ≥80 + 5 beta tenants live + 0 P0 incidents 2 weeks.
