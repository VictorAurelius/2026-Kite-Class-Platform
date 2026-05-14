---
title: Wave 43 — Cost discipline (EventBridge stop/start + right-size + admin sweep)
status: complete
created: 2026-05-08
updated: 2026-05-08
waves: [43]
gaps: [GAP-446, GAP-447, GAP-448, GAP-411, GAP-373, GAP-369, GAP-377, GAP-378, GAP-413]
audit_cluster: release-deploy-artifacts
---

<!-- wave-plan-completeness-exempt: Pre-Wave-76 legacy plan — predates current _TEMPLATE.md structure -->

# Wave 43 — Cost Discipline (Phase 1 BETA)

**Goal:** Giảm AWS burn rate $157/mo → ~$45-55/mo để $200 credit kéo đủ Phase 1 BETA 9-12 tuần.
**Trigger:** User-flagged 2026-05-08 "ALB/EC2/RDS chạy liên tục là không cần thiết, cần cơ chế tắt bật". State-check phát hiện kc-app vẫn `running` mâu thuẫn GAP-445 + cả 2 EC2 over-provisioned m7i-flex.large 8GB.
**Estimated wall-clock:** ~30min agent work, longest-bucket Bucket B ~25min.

---

## 1. Brainstorm

**Q1 (alignment):**
- Persona: Solo dev Phase 1 BETA pre-launch — chưa có beta tenants live, không cần 24/7 availability
- Domain: AWS Infrastructure cost optimization (`infrastructure/terraform-aws/`)
- Wave: standalone — không block work khác; output cải thiện credit longevity

**Q2 (trade-offs):**
- **Stop/start vs delete-recreate ALB:** ALB không stop được (chỉ delete), nhưng ALB chỉ ~$22/mo và DNS reuse quan trọng → giữ chạy. Delete-recreate sẽ thay đổi DNS + complex.
- **Right-size vs giữ m7i-flex.large:** m7i-flex.large cho kh-backend là over-correction từ OOM #1031 (deploy nhầm t3.micro). Compose budget evidence chứng minh t3.medium đủ. Saving 50% đáng làm.
- **3 buckets vs 4:** thêm Bucket D AWS Budgets provisioning hấp dẫn, nhưng GAP-413 đã reference GAP-395 Bucket A → defer Wave 44 để tránh duplicate work với GAP-395.
- **EC2 timezone:** ICT (Asia/Ho_Chi_Minh) phù hợp solo-dev Vietnam working hours, không UTC.

**Q3 (rủi ro):**
- **Right-size OOM:** t3.medium có thể tight nếu peak memory cao hơn compose budget → safety net = CloudWatch memory alarm + rollback path documented (JVM tune → t3.large → m7i-flex.large escalation).
- **Stop/start downtime impact:** beta tenants chưa active → safe. Nếu sau này có tenants, terraform variable `enable_cost_scheduling=false` để override.
- **Admin sweep status drift:** GAP-373 (status page) đã DONE thực tế (Better Stack live) nhưng gap chưa flip — Bucket C sweep dễ → low risk.
- **kh-backend stress test:** post-downsize 1h monitor memory trước khi downsize kc-app — sequential, không parallel.

---

## 2. Task Breakdown

| Bucket | Gap(s) | Owner | Effort | Disjoint? |
|--------|--------|-------|--------|-----------|
| A | GAP-446 | bg-agent | ~25 min | ✅ scheduler.tf + IAM + runbook |
| B | GAP-447 + GAP-411 update | bg-agent | ~25 min | ✅ ec2.tf + CloudWatch + sizing matrix doc |
| C | GAP-373/369/377/378/413 admin sweep | bg-agent | ~15 min | ✅ docs only — gap status flips |

Disjoint check: A touches `scheduler.tf` (NEW) + `iam.tf`; B touches `ec2.tf` + `cloudwatch.tf` (NEW or amend); C touches `documents/04-quality/gaps/*.md`. Zero overlap.

---

## 3. Scope (compact schema)

**Stake tier:** **MEDIUM** (production AWS resources, but reversible via terraform variable + rollback path) → model: **Opus medium effort** (per `feedback_sonnet_baseline_context_thrash.md` — Sonnet thrash on infrastructure files với 16 rules auto-load)
**Cross-layer?** **NO** (infrastructure only, không có FE+BE) → skip Bucket 0 Foundation per `contract-first-for-cross-layer.md`

| # | Bucket | Gap(s) | Priority | Files (glob) | Spawn order |
|:-:|--------|--------|:--------:|--------------|:-----------:|
| 1 | **A** | GAP-446 | 🔴 P0 | `infrastructure/terraform-aws/scheduler.tf` (NEW) + `iam.tf` (amend) + `documents/05-guides/deploy/aws-cost-scheduling.md` (NEW) | parallel |
| 2 | **B** | GAP-447 + GAP-411 update | 🔴 P0 | `infrastructure/terraform-aws/ec2.tf` (amend) + `cloudwatch.tf` (NEW or amend) + `documents/04-quality/gaps/closed/GAP-411-aws-architecture-b-sizing-matrix.md` (update) + `documents/05-guides/deploy/right-size-stress-test.md` (NEW) | parallel |
| 3 | **C** | GAP-373/369/377/378/413 sweep | 🟠 P1 | `documents/04-quality/gaps/GAP-373*.md` + `GAP-369*.md` + `GAP-377*.md` + `GAP-378*.md` + `GAP-413*.md` | parallel |

### Bucket A — EventBridge Scheduler stop/start

- Files: `infrastructure/terraform-aws/scheduler.tf` (NEW) + `iam.tf` (amend permissions) + runbook `documents/05-guides/deploy/aws-cost-scheduling.md`
- Tests: terraform validate + plan output included in PR description
- Acceptance: GAP-446 AC items #1-7
- Implementation: aws_scheduler_schedule_group + 4 aws_scheduler_schedule resources (stop weekday + start weekday + stop weekend + start Monday) targeting EC2/RDS via tag-filter, ICT timezone

### Bucket B — Right-size + OOM safety net

- Files: `infrastructure/terraform-aws/ec2.tf` (instance_type m7i-flex.large → t3.medium) + `cloudwatch.tf` (memory alarm) + GAP-411 update doc + stress-test runbook
- Tests: terraform validate + plan + memory alarm verify command
- Acceptance: GAP-447 AC items #1-7
- Sequential ordering: kh-backend right-size first → 1h stress test → kc-app right-size after kh OK
- Note: kc-app currently `stopped` per Action 1 này turn → resize trên stopped instance OK, no service interruption

### Bucket C — Admin sweep gap status flips

- Files: 5 gap files docs only
- GAP-373 (status page) → DONE (Better Stack `kite-platform.betteruptime.com` live per `release-1-deploy-session-2026-05-07.md` §6)
- GAP-369 (DNS production) → rescope Phase 2 (custom domain decision deferred per session log)
- GAP-377 (smoke test) → check Wave 42 Bucket B coverage; nếu DONE flip, không thì stay PARTIAL
- GAP-378 (rollback) → check Wave 42 Bucket D coverage; flip status accordingly
- GAP-413 (AWS Budgets) → cập nhật Log với reference Wave 43 (no scope change)
- Acceptance: 5 gap files với updated Status + Log entry; ROADMAP updated nếu có status flip

---

## 4. State-Check Evidence (per `audit-to-gap-pipeline.md` §2.6)

| Symbol | Type | Verification command | Evidence | Verdict |
|--------|------|----------------------|----------|---------|
| `infrastructure/terraform-aws/scheduler.tf` | Terraform file | `ls infrastructure/terraform-aws/scheduler.tf` | not found | 🆕 to-be-created (Bucket A) |
| `infrastructure/terraform-aws/cloudwatch.tf` | Terraform file | `ls infrastructure/terraform-aws/cloudwatch*.tf` | not found (only `cloudwatch-dashboard.tf`) | 🆕 to-be-created or amend dashboard.tf (Bucket B) |
| `infrastructure/terraform-aws/ec2.tf` | Terraform file | `ls infrastructure/terraform-aws/ec2.tf` | exists 7.9K | ✅ exists (Bucket B amend) |
| `infrastructure/terraform-aws/iam.tf` | Terraform file | `ls infrastructure/terraform-aws/iam.tf` | exists 12.5K | ✅ exists (Bucket A amend) |
| `m7i-flex.large` (current instance type) | EC2 verification | `aws ec2 describe-instances --query 'Reservations[].Instances[].InstanceType'` | `["m7i-flex.large", "m7i-flex.large"]` | ✅ confirmed both EC2 |
| `kitehub-postgres` RDS | RDS verification | `aws rds describe-db-instances --query 'DBInstances[].DBInstanceIdentifier'` | `["kitehub-postgres"]` | ✅ exists |
| `kitehub-alb` ALB | ALB verification | `aws elbv2 describe-load-balancers --query 'LoadBalancers[].LoadBalancerName'` | `["kitehub-alb"]` | ✅ exists |
| `kitehub-main` CloudTrail | Trail verification | `aws cloudtrail get-trail-status --name kitehub-main --query IsLogging` | `True` | ✅ logging active per `aws-observability-first.md` |
| `documents/05-guides/deploy/aws-cost-scheduling.md` | Runbook | `ls documents/05-guides/deploy/aws-cost-scheduling.md` | not found | 🆕 to-be-created (Bucket A) |
| `documents/05-guides/deploy/right-size-stress-test.md` | Runbook | `ls documents/05-guides/deploy/right-size-stress-test.md` | not found | 🆕 to-be-created (Bucket B) |
| `GAP-411` sizing matrix | Existing gap | `ls documents/04-quality/gaps/GAP-411*.md` | exists DONE 2026-05-07 | ✅ exists (Bucket B updates post-Vercel pivot) |
| `GAP-373/369/377/378/413` | Existing gaps | `ls documents/04-quality/gaps/GAP-{373,369,377,378,413}*.md` | all 5 exist | ✅ exists (Bucket C sweep) |
| Better Stack status page `kite-platform.betteruptime.com` | External vendor | per `release-1-deploy-session-2026-05-07.md:86` | LIVE 2026-05-07T22:15Z | ✅ confirmed (Bucket C flips GAP-373 → DONE) |

Banned shortcuts: zero `| head` truncation used; zero aspirational refs without 🆕 flag.

---

## 5. Verification Gates (per bucket)

| Bucket | Local verify command | CI gate |
|--------|---------------------|---------|
| A | `cd infrastructure/terraform-aws && terraform fmt -check && terraform validate` | terraform-plan workflow on PR |
| B | `cd infrastructure/terraform-aws && terraform fmt -check && terraform validate` | terraform-plan workflow + `aws ec2 describe-instances` post-apply |
| C | docs only — manual reviewer check + `scripts/check-docs.sh` | session-docs-check Rule 13 (gap-done-discipline) + Rule 15 (wave-history) |

**Apply ordering (post-merge):**
1. Bucket A apply qua CI OIDC → verify schedules created (Tier 1: `aws scheduler list-schedules`)
2. Bucket B apply qua CI OIDC → kh-backend resize + stress test 1h → kc-app resize
3. Bucket C docs only — no AWS apply

---

## 6. Agent Spawn Pattern

Per `feedback_parallel_agent_strategy.md` + `agent-background-spawn-default.md`:
- All 3 buckets spawned với `run_in_background: true`
- Worktree isolation (`isolation: worktree`)
- RELATIVE paths in agent prompts per `feedback_worktree_absolute_path_contamination.md`
- Coordinator merges sequentially after all background completions
- Stake tier MEDIUM → Opus medium effort default per `feedback_sonnet_baseline_context_thrash.md`

---

## 7. Closure Protocol

Per `gap-done-discipline.md` + `feedback_post_merge_doc_sync.md` + `feedback_wave_history_append_required.md` + `post-wave-cleanup.md` + `feedback_wave_closure_release_progress_report.md`:
- Each bucket PR updates affected GAP file Log + status
- ROADMAP §🚀 Next Action updated trong closure PR
- Wave plan frontmatter `status: complete` flip trong closure PR
- `wave-history.jsonl` append trong closure PR (Rule 15 enforcement) — **lưu ý:** file chưa tồn tại per state-check; cần tạo trong closure PR
- Sub-gaps filed nếu có deferral (e.g., AWS Budgets provisioning vẫn defer GAP-395)
- `bash scripts/prune-merged-worktrees.sh --yes` post-merge
- **`## Release Plan Progress` section** trong closure PR body — Phase 1 BETA cost-discipline contribution + credit longevity update

---

## 8. Release Plan Progress (will be filled at closure)

**Current Phase:** Phase 1 BETA P1+P2 Soft Launch (chốt 2026-05-06, target 9-12 tuần)
**Wave 43 contribution:** Cost-discipline foundation — burn rate $157→~$50/mo, $200 credit longevity 1.3 tháng → 3.5-4 tháng
**Phase 1 trigger gates progress:** unchanged — Wave 43 không touch quality audit/beta tenants/P0 incidents (cost optimization parallel scope)
**Waves Remaining:** unchanged — Wave 43 không trên critical path Phase 1 trigger; standalone

---

## 9. Log

- **2026-05-08** (draft): Plan created sau user-flagged miss "ALB/EC2/RDS chạy liên tục lãng phí". State-check phát hiện 2 cost leaks: (1) kc-app vẫn running mâu thuẫn GAP-445 — stopped explicit user approval 2026-05-08T08:11Z; (2) cả 2 EC2 m7i-flex.large 8GB over-provisioned vs compose budget 3.2GB/2.5GB. 3 buckets parallel, ~30min wall-clock estimate. MEDIUM stake tier, Opus medium effort.
- **2026-05-08** (in-progress): Inline fix GAP-448 added — Vercel `ignoreCommand` shipped trong plan PR (2× `vercel.json` cho kiteclass + kitehub FE). User-flagged khi check CI #1035 thấy Vercel build trigger trên docs-only PR — spirit-violation với CI Trigger Policy 2026-04-24. Fix nhỏ (~10 LOC) ship cùng plan PR thay vì Wave 43 Bucket C để có hiệu quả ngay từ commit tiếp theo.
- **2026-05-08** (complete): Wave 43 SHIPPED. 4 PRs merged sequential A→B→C→bonus: #1036 (A GAP-446 EventBridge Scheduler 270 LOC + IAM + runbook, GAP-446 🟡 PARTIAL terraform shipped CI apply pending), #1038 (B GAP-447 right-size m7i-flex.large→t3.medium + CloudWatch memory alarm + stress-test runbook + GAP-411 post-Vercel matrix update, GAP-447 🟡 PARTIAL), #1037 (C admin sweep — GAP-373 → 🟢 DONE Better Stack evidence, GAP-369 Phase 2 rescope, GAP-377/378 verified DONE Wave 25/26, GAP-413 Log update, ROADMAP updated), #1039 (bonus — Java unchecked varargs warning fix BetaAccessServiceTest:467). Plus inline GAP-448 Vercel ignoreCommand DONE in plan PR #1035. Pre-spawn cost saving: kc-app instance stopped 08:11Z (-$60/mo started). Vercel ignoreCommand active confirmed via #1036/1037/1038 SUCCESS = Skipped. Vercel quota saving validated. Wall-clock ~6-8min agent work (vs 30min estimate, 4× faster — Opus medium effort efficient on terraform). Stake MEDIUM, 0 clarification rounds, 0 CI fails. **Combined burn rate impact:** $157/mo → ~$45-55/mo target (post-CI-apply right-size + scheduler), $200 credit longevity 1.3 tháng → 3.5-4 tháng. **Post-merge follow-ups (human-action):** terraform apply both stacks → run §3 verify commands from `aws-cost-scheduling.md` + stress test per `right-size-stress-test.md` → file 2× verification artifacts under `documents/04-quality/audits/aws-verification/2026-05-08-wave-43-*.md` → flip GAP-446 + GAP-447 → 🟢 DONE. Closure PR per `feedback_post_merge_doc_sync.md` + `feedback_wave_history_append_required.md` + `post-wave-cleanup.md`.
