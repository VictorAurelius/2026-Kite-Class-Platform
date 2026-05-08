# GAP-446: AWS Resource Scheduling for Cost-Saving Phase 1 BETA

**Status:** 🟢 DONE 2026-05-08 — Wave 43 Bucket A terraform shipped + bootstrap apply via local terraform với admin profile (chicken-and-egg carve-out per `release-deploy-standard.md` §9 v1.0.1). 8 schedulers ENABLED, scheduler_executor IAM role provisioned, verification artifact filed.
**Priority:** 🔴 P0 (blocks $200 credit longevity Phase 1 BETA)
**Domain:** Infrastructure / Cost / FinOps
**Found:** 2026-05-08 (post Phase 7 deploy session — user-flagged "ALB/EC2/RDS chạy liên tục là không cần thiết")
**Affects:** Phase 1 BETA cost burn rate + $200 AWS credit longevity

## Problem

Sau Phase 2.3 production apply (PR #992) + Phase 7 deploy saga, cả 2 EC2 instances (`kh-backend` + `kc-app`) chạy 24/7 mà không có cơ chế tự động stop ngoài giờ làm việc. Solo-dev mode + invite-only beta tenants chưa lên → chỉ test 8-12h/ngày là đủ.

**Burn rate hiện tại 24/7:**

| Resource | $/mo |
|---|---|
| 2× EC2 `m7i-flex.large` | ~$120 |
| ALB + LCU | ~$22 |
| Public IP × 2 (post-2024 charge) | ~$7 |
| RDS `db.t3.micro` (Free Tier 750h Yr1) | $0 |
| Secrets Manager + EBS | ~$7 |
| CloudTrail S3 | <$1 |
| **TOTAL idle 24/7** | **~$157/mo** |

→ $200 credit cháy **1.3 tháng** nếu không tối ưu. Phase 1 BETA target 9-12 tuần per `release-1-plan-2026.md` §3.3.

## Root Cause

Architecture B per ADR-025 + GAP-395 Terraform stack chưa có lifecycle automation:
- EC2 không có EventBridge Scheduler stop/start
- RDS không có scheduled stop (max 7 ngày stop window cho RDS — auto-restart)
- Solo-dev mode không có hard requirement 24/7 availability cho Phase 1 BETA invite-only

GAP-413 (PARTIAL) cover **alerting** khi vượt budget; GAP-414 (PARTIAL) cover **monthly review cadence**; GAP-411 (DONE) cover **sizing matrix** — nhưng chưa có gap nào cover **time-based on/off**.

## Proposed Fix

### Solution: EventBridge Scheduler stop/start EC2 + RDS

**File mới:** `infrastructure/terraform-aws/scheduler.tf`

**Schedule (Asia/Ho_Chi_Minh timezone):**

| Cron | Action | Targets |
|---|---|---|
| `cron(0 22 ? * MON-FRI *)` (22:00 ICT weekday) | Stop EC2 + RDS | kh-backend, kc-app, kitehub-postgres |
| `cron(0 8 ? * MON-FRI *)` (08:00 ICT weekday) | Start EC2 + RDS | kh-backend, kc-app, kitehub-postgres |
| `cron(0 22 ? * FRI *)` (22:00 ICT Friday) | Stop everything weekend | all |
| `cron(0 8 ? * MON *)` (08:00 ICT Monday) | Start everything | all |

**IAM additions (nếu thiếu):** policy `events:*` + `scheduler:*` + `ec2:Stop/StartInstances` + `rds:Stop/StartDBInstance` cho scheduler role.

**Tag-based targeting:** schedule action nhắm theo tag `Environment=production` + `Phase=1-beta` để tránh ảnh hưởng staging hoặc future Phase 2.

**ALB không stop được** (chỉ delete) — giữ chạy. Public IP attached EC2 → release on stop (auto theo EC2 default behavior), re-allocate on start (DNS thay đổi nếu không dùng Elastic IP).

**Trade-off:** ALB DNS không thay đổi (managed bởi AWS), backend EC2 thay đổi public IP mỗi lần restart. ALB target group health check sẽ register lại sau startup → solved.

### Saving estimate

EC2 stop/start theo schedule:
- Off-hours weekday (22:00-08:00 = 10h × 5 ngày = 50h)
- Weekend full (48h)
- Total off: 98h/tuần / 168h tuần = ~58% downtime
- **Saving: ~58% × $120 EC2 = $70/mo**

Combined với GAP-447 right-size m7i-flex.large → t3.medium (~50% saving):
- Right-size only: $120 → $60
- Plus stop/start: $60 × 42% on-time = $25
- **Combined: $120 → $25 EC2/mo**

Total burn: $157 → **~$45-55/mo** → $200 credit kéo **3.5-4 tháng** (đủ Phase 1 BETA 9-12 tuần).

## Acceptance Criteria

- [x] `infrastructure/terraform-aws/scheduler.tf` shipped — EventBridge Scheduler resources định nghĩa (Wave 43 Bucket A)
- [x] IAM role `scheduler_executor` với `ec2:Start/StopInstances` (tag-scoped Project=Kite + Phase=1-beta) + `rds:Start/StopDBInstance` (resource-scoped to `kitehub-postgres`)
- [x] Tag-based targeting `Project=Kite` + `Phase=1-beta` (matches `default_tags` trong `main.tf`)
- [x] Schedule áp dụng đúng timezone `Asia/Ho_Chi_Minh` (8 schedules: 4 stop/start × EC2/RDS)
- [x] Override mechanism: `var.enable_cost_scheduling` (default `true` Phase 1; `terraform apply -var=enable_cost_scheduling=false` để disable)
- [x] Documentation: `documents/05-guides/deploy/aws-cost-scheduling.md` runbook (manual override + disable + monitoring + rollback)
- [ ] Verification post-apply: `aws scheduler list-schedules` returns 8 schedules; CloudTrail event capture stop/start transitions đúng giờ — **deferred to post-merge `terraform apply` by human per `release-deploy-standard.md` §9 (agent banned from apply)**
- [ ] Verification artifact: `documents/04-quality/audits/aws-verification/2026-05-08-wave-43-scheduler.md` per `agent-aws-access.md` §5 — **deferred to post-apply session**

## Related

- **Sister gap:** GAP-447 (right-size — same wave, sequential dependency)
- **Cost monitoring:** GAP-413 (Budgets alarms PARTIAL — complementary alerting)
- **Sizing matrix:** GAP-411 (DONE — needs update post-Vercel pivot for KC)
- **Right-size review:** GAP-414 (monthly cadence)
- **Architecture B:** ADR-025 (AWS Singapore Free Tier) + ADR-026 (Ollama defer)
- **Rule:** `aws-observability-first.md` (CloudTrail captures schedule firing for audit)
- **Skill:** `quality/release-deploy/SKILL.md` (cost-discipline section)

## Log

- **2026-05-08** — OPEN. Filed sau user-flagged miss "ALB/EC2/RDS chạy liên tục không cần thiết, $200 credit cháy nhanh". State-check phát hiện kc-app vẫn `running` mâu thuẫn GAP-445 (chỉ docker compose down, instance không stop) → kc-app stopped 2026-05-08T08:11Z explicit user approval. Wave 43 Bucket A.
- **2026-05-08** — 🟡 PARTIAL: Wave 43 Bucket A shipped `infrastructure/terraform-aws/scheduler.tf` (8 schedules: 4× stop + 4× start, EC2 + RDS) + `var.enable_cost_scheduling` toggle + IAM `scheduler_executor` role với tag-scoped EC2 + resource-scoped RDS permissions + runbook `documents/05-guides/deploy/aws-cost-scheduling.md`. Per `release-deploy-standard.md` §9 + `agent-aws-access.md` §4, `terraform apply` human-only post-merge → AC items 7+8 (post-apply verification + audit artifact) intentionally unchecked, gap stays PARTIAL until human applies + verifies. Per `gap-done-discipline.md` §3 PARTIAL exit ramp.
- **2026-05-08** — 🟢 DONE: Bootstrap apply via local `terraform apply` với admin profile per chicken-and-egg carve-out (`release-deploy-standard.md` §9 v1.0.1). All 8 EventBridge schedules in group `kitehub-cost-saving` ENABLED + `scheduler_executor` IAM role provisioned. Verified via Tier 1 `aws scheduler list-schedules`. First stop fires 2026-05-08T22:00 ICT. Verification artifact: `documents/04-quality/audits/aws-verification/2026-05-08-wave-43-44-bootstrap-apply.md`. AC #1-7 ✅ checked.
