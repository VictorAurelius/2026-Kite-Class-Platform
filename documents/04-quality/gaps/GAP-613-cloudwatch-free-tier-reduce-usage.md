# GAP-613 — CloudWatch Free Tier 85% threshold — reduce alarms + log retention

**Status:** 🔵 OPEN
**Priority:** 🟠 P1
**Domain:** DevOps + Cost
**Found:** 2026-05-17 (Wave 90 AWS Free Tier alert email 04:09 UTC, ~12h before account suspension)
**Affects:** AWS account billing + verification risk; potentially contributing factor to GAP-612 suspension trigger

## Problem

AWS Free Tier limit alert (2026-05-17 04:09 UTC):
> Your AWS account 906286017800 has exceeded 85% of the usage limit for one or more AWS services with a Free Tier offer for the month of May.

Email doesn't specify which service(s); requires AWS console login to check Bills page (blocked by GAP-612 suspension).

## Suspect services (cost-benefit analysis pending AWS restore)

| Service | Free Tier monthly limit | Suspect actual usage | Action candidate |
|---|---|---|---|
| **CloudWatch Alarms** | 10 alarms | Wave 84-85 added ~10 alarms (4 security GAP-437 + 3 cost GAP-414 + 3 perf Wave 85) — likely AT/OVER limit | Review + disable duplicates; target ≤8 |
| **CloudWatch Logs ingestion** | 5GB/month | CloudTrail multi-region + container stdout + admin-login-alert poison spam (GAP-606 ~864K msg over 24h) | Fix GAP-606 + shorten retention |
| **CloudWatch API requests** | 1M/month | Alarm polling + dashboard refresh | Reduce dashboard auto-refresh interval |
| **CloudTrail data events** | $0 free tier (mgmt events only) | Verify no data events enabled | Disable if any |
| **EC2 hours t3.micro/small** | 750h × 1 instance | 3 instances × 24h × 10 days = 720h | Already AT limit; cannot reduce without stopping |

## Root cause hypothesis

Wave 84-85 added significant CloudWatch resources without Free Tier budget review:
- GAP-437 CloudTrail observability (4 metric filters + 4 alarms + SNS)
- GAP-414 EC2 cost monitoring (3 low-CPU alarms)
- Wave 85 Bucket H (3 performance alarms)
- Cumulative count likely exceeds 10-alarm Free Tier limit

GAP-606 admin-new-login-alert template missing → consumer infinite retry → ~10 log entries/sec spamming CloudWatch Logs → fast burn through 5GB/month ingestion limit.

## Production impact

🟠 (current) Free Tier overage typically $1-3/month — small. BUT:
🔴 (verification risk) Pattern "new account hits 85% in 10 days" looks anomalous to AWS billing risk system → may have contributed to GAP-612 account suspension trigger.

## Proposed Fix (POST-AWS-RESTORE — Wave 92 candidate)

### Phase 1 (immediate, ~30 min post-restore)
1. Login AWS Billing console → Free Tier dashboard → identify exact services + usage %
2. Review CloudWatch Alarms list → identify duplicates / non-critical
3. Disable non-critical alarms (target: keep 5 core security + 3 cost = 8 total)
4. Shorten CloudWatch Logs retention: 30d → 7d (where business value < cost)
5. Verify CloudTrail no data events enabled (mgmt events only stay free)

### Phase 2 (sustained, ~2h)
1. File AWS Budget alarm at $5/month threshold (alert before overage compounds)
2. Add CloudWatch usage to weekly ops dashboard
3. Document Free Tier budget per service in `documents/05-guides/operations/aws-cost-monitoring.md`
4. Cross-reference với `release-deploy-standard.md` §3.1 PRE-RELEASE checklist — add cost review row

### Phase 3 (long-term)
- Migrate verbose container logs from CloudWatch Logs to S3 archive (per Wave 84 GAP-414 baseline)
- Consider self-hosted Prometheus + Grafana for non-AWS-native metrics (Wave 92+)

## Cross-reference với GAP-606 (cascading benefit)

GAP-606 fix (admin-new-login-alert template ship Wave 91 Bucket C) eliminates ~864K wasted log messages/24h → automatically reduces CloudWatch Logs ingestion by ~30-40% baseline. May resolve overage without alarm reduction.

→ Execute Phase 1 AFTER GAP-606 deploy + observe 24h log volume reduction. If still overage → proceed with alarm review.

## Acceptance Criteria

- [ ] AWS Billing dashboard accessed (requires GAP-612 resolved)
- [ ] Exact overage service(s) identified
- [ ] If CloudWatch Alarms count >10: reduce to ≤8 (5 core security + 3 cost)
- [ ] CloudWatch Logs retention shortened (30d → 7d) where appropriate
- [ ] Budget alarm $5/month set
- [ ] Cost review row added to `release-deploy-standard.md` §3.1
- [ ] Runbook `documents/05-guides/operations/aws-cost-monitoring.md` created
- [ ] Free Tier dashboard < 70% baseline 30 days post-fix

## Related

- GAP-612 (sister — suspension; this gap may be contributing trigger)
- GAP-606 (Wave 91 Bucket C — fixes log spam cascade source)
- GAP-437 (Wave 84 — added 4 CloudTrail alarms)
- GAP-414 (Wave 84 — added 3 cost alarms)
- Wave 85 Bucket H (added 3 perf alarms)
- `release-deploy-standard.md` §3.1 — extend với cost review checklist (Phase 2 follow-up)

## Log

- **2026-05-17:** Gap filed Wave 90 closure docs. Free Tier alert preceded suspension by ~12h — hypothesis: contributing trigger for GAP-612. Execution blocked until AWS restored. Cross-reference Wave 91 Bucket C (GAP-606 fix) may auto-resolve log overage without alarm reduction.
