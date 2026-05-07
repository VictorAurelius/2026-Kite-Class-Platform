# Monthly Cost Report — Template / 2026-06 Skeleton

**Report month:** 2026-06 (template — first actual report ~30 ngày sau Phase 1 BETA deploy)
**Status:** TEMPLATE
**Last-Reviewed:** 2026-05-07
**Reviewer:** @nguyenvankiet (solo-dev, acting CTO + acting FinOps)
**Closes:** GAP-414 (EC2 right-sizing monthly review template)
**Related:** GAP-411 (sizing matrix), GAP-413 (cost monitoring alarm data feeds), ADR-025 (AWS Singapore)

---

## 1. Mục đích

Monthly cost report template áp dụng từ Phase 1 BETA tháng đầu tiên (deploy ~2026-05-09 → first report ~2026-06-09). Mỗi tháng:

1. Capture actual cost vs estimate (per `aws-architecture-sizing-matrix.md` Architecture B target)
2. Run AWS Compute Optimizer + Cost Explorer
3. Document right-sizing recommendations + actions
4. Track Activate credit balance trend
5. Surface anomalies cho follow-up gap

**Cadence:**
- Phase 1 BETA: Monthly manual review (template-driven)
- Phase 1.5+: Quarterly (less drift expected; automate cron Phase 2+)

---

## 2. Report Skeleton (copy + fill each month)

### 2.1 Executive Summary

- **Month:** YYYY-MM
- **Phase:** Phase 1 BETA / Phase 1.5 PAID early / etc.
- **Tenants live:** N (delta vs prior month: +N / -N)
- **Total spend actual:** $X.XX
- **Total spend estimate (sizing matrix target):** $X.XX
- **Variance:** ±X% (within target / over / under)
- **AWS Activate credit balance:** $X.XX remaining (delta vs prior month)
- **Top 3 cost drivers:** 1. ... 2. ... 3. ...
- **Action items captured:** N (see §5 below)

### 2.2 Cost Breakdown (Actual vs Estimate)

| Category | Estimate $/mo (sizing matrix) | Actual $/mo | Variance | Notes |
|---|---|---|---|---|
| EC2 — kitehub services (t3.medium) | 0 (Free Tier Yr1) | | | |
| EC2 — kiteclass + frontends (t3.small) | 15 | | | |
| RDS Postgres (db.t3.micro) | 0 (Free Tier Yr1) | | | |
| EBS storage | 3 | | | |
| ECR | 0 | | | |
| S3 | 0.5 | | | |
| ALB | 18 | | | |
| Route 53 | 0.5 | | | |
| CloudWatch | 0 (Free Tier Yr1) | | | |
| Data transfer egress | 5 | | | |
| Secrets Manager | 2 | | | |
| SES email | 1 | | | |
| **TOTAL** | **45-50** | | | |

Estimate baseline: `aws-architecture-sizing-matrix.md` §3.1 Phase 1 BETA Yr1.

### 2.3 AWS Compute Optimizer Recommendations

**Source:** AWS Console → Compute Optimizer → EC2 instances tab

| Instance | Current type | Utilization (CPU avg / max) | Optimizer recommendation | Action decision |
|---|---|---|---|---|
| kitehub-main | t3.medium | X% / Y% | <Optimized / Under-provisioned / Over-provisioned> | Keep / Upsize / Downsize / RI commit |
| kiteclass-main | t3.small | X% / Y% | <...> | <...> |

**RDS recommendations:** AWS Console → RDS → Recommendations tab (separate from Compute Optimizer)

| RDS instance | Current | Recommendation | Decision |
|---|---|---|---|
| kite-postgres | db.t3.micro | <...> | <...> |

### 2.4 CloudWatch Utilization Summary (30-day)

**EC2 metrics per instance:**
- CPU Utilization: avg / p50 / p95 / max
- Network Out: GB total
- EBSReadOps + EBSWriteOps: avg / max
- StatusCheckFailed: count

**RDS metrics:**
- CPUUtilization: avg / p95
- DatabaseConnections: avg / max
- ReadIOPS + WriteIOPS: avg / max
- FreeableMemory: avg / min
- FreeStorageSpace: end-of-month value

**ALB metrics:**
- RequestCount: total
- TargetResponseTime: avg / p95
- HTTPCode_Target_4XX_Count + 5XX_Count
- ActiveConnectionCount: avg

### 2.5 Activate Credit Trend

| Date | Credit balance | Monthly burn | Runway @ current rate |
|---|---|---|---|
| Start of month | $X | | |
| End of month | $Y | $X-Y | (Y / monthly burn) tháng |

**Alarm B status (per GAP-413 §2.2):** OK / Triggered (date / threshold)

---

## 3. Anomaly Detection

| Anomaly | Description | Investigation | Resolution |
|---|---|---|---|
| | | | |

**Anomaly types to look for:**
- Cost spike >20% vs prior month không có growth correlate
- Single service consume >40% total (per Tag.Service alarm C)
- Data transfer egress unusual (potential security incident — exfiltration)
- CloudWatch logs ingest spike (chatty service / log level mis-set)

---

## 4. Right-Sizing Decision Log

| Date | Resource | Change | Reason | Effect (next month) |
|---|---|---|---|---|
| | | (e.g., t3.medium → t3.small) | | (estimate $ saved) |

---

## 5. Action Items for Next Month

| # | Action | Owner | Due | Reference |
|---|---|---|---|---|
| 1 | | @nguyenvankiet | YYYY-MM-DD | GAP-XXX (if applicable) |

---

## 6. Phase Transition Check

**Current phase target:** Phase 1 BETA (per ADR-025)
**Phase 1.5 trigger gate (per `sizing-matrix.md` §2):** 30 paying tenants OR daily signup >5/day
**This month:** N tenants total (X paying, Y trial), Z signup/day average → trigger NOT yet / ALREADY MET / WITHIN 30%

If trigger gate met → file follow-up gap "Phase 1.5 architecture transition" + plan upgrade Architecture A.

---

## 7. References

- `documents/05-guides/deploy/aws-architecture-sizing-matrix.md` — estimate baseline
- `documents/05-guides/deploy/aws-cost-monitoring.md` — alarm thresholds + runbook
- `documents/05-guides/deploy/aws-activate-credit-policy.md` — credit allocation rules
- GAP-414 — right-sizing review cadence
- AWS Compute Optimizer: https://aws.amazon.com/compute-optimizer/
- AWS Cost Explorer: https://docs.aws.amazon.com/cost-management/latest/userguide/ce-what-is.html

---

## 8. Log

- **2026-05-07** — Template created. First actual fill ~2026-06-09 sau Phase 1 BETA deploy + 30 days operational. Closes GAP-414 acceptance criterion partial (template + cadence doc; first actual report deferred ~30 days post-deploy).

---

## 9. Acceptance Criteria mapping

| GAP-414 AC | Status |
|---|---|
| First report `documents/04-quality/cost-reports/2026-06.md` after 1 month Phase 1 BETA running | 🟡 PARTIAL — template ready, actual fill ~2026-06-09 |
| Compute Optimizer recommendation captured | ✅ §2.3 template |
| CloudWatch utilization summary | ✅ §2.4 template |
| Action items actioned next month (downsize/upsize/RI) | ✅ §4 + §5 template |
| Document review cadence (monthly Phase 1, quarterly Phase 1.5+) | ✅ §1 + this row |

**Status flip:** GAP-414 → 🟡 PARTIAL (template + cadence ship; first actual report deferred 30 days post-deploy + filed as routine monthly task per cadence).
