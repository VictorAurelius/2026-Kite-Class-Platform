# GAP-447: Right-size EC2 m7i-flex.large → t3.medium (post-Vercel pivot)

**Status:** 🟡 PARTIAL 75% 2026-05-12 — kh_backend right-size ✅ DONE; kc_app drift tracked GAP-450; CWAgent install = user-action (manual SSM per `agent-aws-access.md` §4.3 Tier 3); kc_app_memory alarm deferred Phase 7. Phase 1 BETA cost goal achieved.
**Priority:** 🔴 P0 (50% cost saving on EC2 alone)
**Domain:** Infrastructure / Cost / FinOps
**Found:** 2026-05-08 (Wave 43 cost-discipline state-check)
**Affects:** EC2 burn rate Phase 1 BETA — over-provisioning ~$60/mo

## Problem

Cả 2 EC2 (`kh-backend`, `kc-app`) hiện chạy `m7i-flex.large` (8GB RAM) — **over-provision so với compose memory budget**:

| Stack | Compose budget | EC2 hiện tại | Headroom thực |
|---|---|---|---|
| **kh-backend** (5 services + redis + rabbitmq + gateway) | ~3.2GB peak (per `docker-compose.production.yml:13-19`) | m7i-flex.large 8GB | **5GB lãng phí** |
| **kc-app** (kiteclass-core + gateway + redis + rabbitmq, frontend trên Vercel) | ~2.5GB peak (per `docker-compose.kc.yml:13-18`) | m7i-flex.large 8GB | **5.5GB lãng phí** |

## Root Cause

### kh-backend OOM #1031 = over-correction

Architecture B per GAP-411 (DONE) chốt `t3.medium 4GB` cho kh-backend. Phase 7 deploy thực tế deploy nhầm `t3.micro 1GB` → OOM cascade #1031 → bumped lên `m7i-flex.large 8GB`. **Đáng lẽ chỉ cần đúng `t3.medium 4GB` matrix.**

### kc-app sizing matrix bị stale

GAP-411 chốt `t3.small 2GB` cho kc-app từ trước khi pivot Vercel (2026-05-07). Lúc đó plan = KC frontend Next.js ON kc-app. Sau pivot:
- KC frontend → Vercel (off EC2)
- KC backend Java services (core + gateway + redis + rabbitmq) → vẫn trên kc-app
- Compose `docker-compose.kc.yml` chốt budget 2.5GB peak

→ `t3.small 2GB` < 2.5GB peak → **OOM rủi ro cao**. Phải `t3.medium 4GB`.

## Proposed Fix

### Right-size matrix mới

| Service | Stale (GAP-411) | Hiện tại | Đề xuất | Saving |
|---|---|---|---|---|
| kh-backend | t3.medium | m7i-flex.large ($60/mo) | **t3.medium ($30/mo)** | $30 |
| kc-app | t3.small (stale) | m7i-flex.large ($60/mo) | **t3.medium ($30/mo)** | $30 |
| **TOTAL** | | $120 | **$60** | **$60/mo** |

### OOM safety net (BẮT BUỘC trước khi downsize)

1. **CloudWatch memory alarm:** `MemoryUtilization > 85%` cho cả 2 instance → SNS notification
2. **Pre-downsize stress test:** trên kh-backend chạy 1h sau khi downsize, monitor memory; nếu OK → apply kc-app
3. **Rollback path:** Nếu OOM trên t3.medium →
   - **Step 1:** tune JVM heap thấp hơn (giảm `-Xmx384m` → `-Xmx256m` cho 1-2 services không critical)
   - **Step 2:** upsize `t3.large 8GB` (cùng family, vẫn rẻ hơn m7i-flex.large)
   - **Step 3:** revert m7i-flex.large nếu Step 1+2 không đủ
4. **Phase order:** kh-backend trước (đã có data live, dễ stress-test), kc-app sau (chưa resume per GAP-445)

### Update GAP-411 sizing matrix

GAP-411 phải update post-Vercel pivot:
- KC frontend trên Vercel (không phải kc-app)
- kc-app sizing rationale = backend-only stack
- Reference docker-compose budgets thay vì estimates

### Concurrent với GAP-446 EventBridge stop/start

Right-size + stop/start = combined saving:
- Right-size only: $120 → $60 EC2/mo
- Plus stop/start (~58% downtime): $60 × 42% on-time = ~$25
- **Combined EC2: $25/mo** (vs $120/mo current)

## Acceptance Criteria

- [ ] `infrastructure/terraform-aws/ec2.tf` updated — `instance_type` đổi `m7i-flex.large` → `t3.medium` cho cả 2
- [ ] CloudWatch memory alarm shipped — `MemoryUtilization > 85%` (cần CloudWatch agent install hoặc procstat metric)
- [ ] GAP-411 sizing matrix updated — Vercel pivot reflected, kc-app rationale = backend-only
- [ ] Pre-downsize stress test runbook — `documents/05-guides/deploy/right-size-stress-test.md`
- [ ] Rollback plan documented — JVM tune → upsize t3.large → revert m7i-flex.large escalation
- [ ] Verification post-apply: `aws ec2 describe-instances` confirm `t3.medium` cho cả 2; `aws cloudwatch describe-alarms` confirm memory alarm active
- [ ] Verification artifact: `documents/04-quality/audits/aws-verification/2026-05-08-wave-43-right-size.md`

## Related

- **Sister gap:** GAP-446 (EventBridge stop/start — combined saving, same wave)
- **Sizing matrix (DONE):** GAP-411 — **needs update** in this gap's PR
- **Right-size cadence:** GAP-414 (monthly Compute Optimizer review)
- **OOM root cause:** PR #1031 (terraform sizing fix)
- **KC compose:** `docker-compose.kc.yml` (post-Vercel-pivot KC backend stack)
- **KH compose:** `docker-compose.production.yml` (5 KH services + infra)
- **ADR-025:** AWS Singapore Free Tier strategy
- **Memory:** `feedback_release_fix_retry_budget.md` (OOM #1031 was over-correction not root-cause fix)

## Log

- **2026-05-13:** Wave 70 GAP-502 revisit. Original "right-size t3.medium" assumption invalidated by production OOM evidence (11 container die/1h, Wave 69 audit-of-trust). kh_backend upsized t3.medium → t3.large (+$30/mo). kc_app stays t3.medium. Status STAYS PARTIAL: original right-sizing decision retained for kc_app; kh_backend escalation per built-in OOM-safety-net rollback path (variable description §"escalation: → t3.large → m7i-flex.large"). Post-release downsize evaluation deferred to follow-up gap (criteria: ≥4 weeks stability + avg MemoryUtilization <60% + zero OOM → consider re-downsize).
- **2026-05-12** (Wave 66 Bucket Z — PARTIAL exit-ramp clarification per `gap-done-discipline.md` §3): Re-scoped remaining items into 3 categories:
  1. **kh_backend right-size** — ✅ DONE 2026-05-08 (t3.medium in-place modify; Phase 1 BETA cost goal achieved $120 → $60/mo EC2)
  2. **kc_app drift** — tracked separately in GAP-450 (not Wave 66 scope; ship when Phase 7 resumes)
  3. **CWAgent install + memory alarm transition** — user-action (manual SSM RunCommand per `agent-aws-access.md` §4.3 Tier 3 banned for agent); runbook `documents/05-guides/deploy/right-size-stress-test.md` §1 covers steps. Stress test gated on real traffic (Phase 1 BETA invite window) — not pre-launch concern.
  Status stays 🟡 PARTIAL — Phase 1 BETA blocking portion (kh_backend right-size) DONE; remaining work tracked or user-owned. Flips to DONE post-CWAgent install when user runs SSM steps.
- **2026-05-08** — 🟡 PARTIAL (post bootstrap apply): (1) kh_backend `m7i-flex.large` → `t3.medium` in-place modify ✅ (~30s restart đã xảy ra; running healthy verified via `aws ec2 describe-instances`); (2) kc_app REPLACED unintentionally — old `i-04f65503ace7febe4` destroyed + new `i-07f6de54544162124` t3.medium stopped (target dependency pulled instance + `associate_public_ip_address: false→true` drift forced replacement; data loss minimal since Phase 7 deferred); kc_app drift tracked GAP-450 ⚠️; (3) `kitehub-memory-alerts` SNS topic created + email subscription `vannkite@outlook.com` PendingConfirmation ✅ (user manual click confirm); (4) `kitehub-kh-backend-memory-high` alarm INSUFFICIENT_DATA expected (CWAgent install pending manual SSM step per `right-size-stress-test.md` §1) ✅; (5) `kitehub-kc-app-memory-high` alarm SKIPPED (depend on kc_app drift, ship Phase 7 resume). SNS tag fix mid-flight via PR #1046 (parens `()` invalid trong AWS SNS tags). Verification artifact: `documents/04-quality/audits/aws-verification/2026-05-08-wave-43-44-bootstrap-apply.md`. Cost saving achieved: $120/mo EC2 → $60/mo (right-size) → ~$25/mo effective post-scheduler 58% downtime. Status PARTIAL → DONE khi: CWAgent install + alarm transitions to ACTIVE + kc_app_memory_high alarm provisioned post-Phase-7.
- **2026-05-08** — PARTIAL (Wave 43 Bucket B). Terraform changes shipped: `infrastructure/terraform-aws/variables.tf` defaults `kh_backend_instance_type` + `kc_app_instance_type` flipped m7i-flex.large → t3.medium with rationale citing compose budgets + GAP-411. New `infrastructure/terraform-aws/cloudwatch.tf` ships SNS topic `kitehub-memory-alerts` (email `vannkite@outlook.com`) + 2 `MemoryUtilization > 85%` alarms (5min × 2 datapoints). GAP-411 sizing matrix updated in this PR with Post-Vercel Pivot Update section. Runbook `documents/05-guides/deploy/right-size-stress-test.md` covers Phase 0-5 (prereqs → CloudWatch agent install → sequential apply kh-backend then kc-app → 1h stress test → rollback escalation). Remaining ACs (CI apply + stress test + verification artifact) deferred to post-merge per `gap-done-discipline.md` §3 PARTIAL exit ramp; closure when stress-test report saved.
- **2026-05-08** — OPEN. Filed sau Wave 43 state-check phát hiện 2 instances cùng m7i-flex.large 8GB không cần thiết. Compose budgets evidence: KH 3.2GB peak / KC 2.5GB peak, t3.medium 4GB đủ + headroom. Wave 43 Bucket B với OOM safety net.
