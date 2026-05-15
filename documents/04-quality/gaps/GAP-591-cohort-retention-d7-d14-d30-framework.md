# GAP-591: Cohort retention tracking framework — D7 / D14 / D30 activation milestones

**Status:** 🔵 OPEN
**Priority:** 🟠 P1
**Domain:** Backend / Analytics / Ops
**Phase:** phase-1-beta
**Found:** 2026-05-15 (Wave 86 Bucket A benchmark-vn-saas-edu Q2)
**Affects:** Wave 86 first 5 beta cohort monitoring + Phase 1.5 scaling baseline

## Problem

Industry benchmark Q2:
- **70% of churn xảy ra 90 ngày đầu** (Userpilot 2026)
- Time-to-first-value <7 days → 50% lower churn
- 20%+ voluntary churn linked to poor onboarding
- B2B SMB monthly churn 3-5% (best-in-class <1%)

Wave 86 Bucket H plan hiện tại chỉ track "first-day churn rate" — thiếu D7 / D14 / D30 cohort activation framework. Critical period 90 ngày đầu không có dashboard → first cohort lost insights → Phase 1.5 scaling không có baseline data để optimize.

## Root Cause

Wave 86 inside-out scope focus on tag + invite mechanics, không scope retention analytics framework. Forced manual tracking khi chỉ 5 tenants — nhưng pattern phải codify trước khi >20 tenants Phase 1.5.

## Proposed Fix

1. **Activation milestone definition**:
   - **D7**: % của 5 tenants đã tạo ≥1 lớp / mời ≑1 student
   - **D14**: % vẫn active login ≥1× trong tuần qua
   - **D30**: % vẫn active OR có churn reason captured
2. **Tracking implementation** (Phase 1 BETA scale allows manual):
   - Spreadsheet `documents/01-business/kitehub/beta-cohort/p1-retention-tracker.csv`
   - Columns: tenant_id, tenant_name, invited_at, signup_at, first_class_at, last_login_at, D7_status, D14_status, D30_status, churn_reason
   - Update weekly manually từ admin dashboard query
3. **Automated activation trigger**:
   - Cron job `kitehub-analytics/jobs/CohortActivationJob.java` weekly
   - Query DB cho activation events per tenant
   - Emit metric `CohortActivation/D7`, `D14`, `D30` → CloudWatch
4. **Proactive outreach trigger**:
   - Nếu D7 activation < 50% → trigger Zalo OA outreach (defer Phase 1.5+ khi Zalo active) OR manual email reminder
5. **Churn reason capture form**:
   - Lightweight survey trigger trong dashboard khi tenant không login > 14 days
   - 3 options: "Quá phức tạp" / "Thiếu feature" / "Không phù hợp" + freetext

## Acceptance Criteria

- [ ] Activation milestone definitions documented
- [ ] Tracker spreadsheet shipped + initial 5-tenant rows
- [ ] Cron job CohortActivationJob shipped + emits CloudWatch metric
- [ ] Manual outreach process documented Bucket H runbook
- [ ] Churn reason capture form FE shipped
- [ ] D7 dashboard tile live (CloudWatch hoặc Grafana)

## Related

- Audit: `documents/04-quality/audits/persona-review/2026-05-15-pre-wave-86-benchmark-vn-saas-edu.md` §3 Q2 + §6 GAP-NEW-3
- Wave 86 plan §3 Bucket H AC H-AC8
- Phase 1.5 force-multiplier (scale to >5 cohort)
