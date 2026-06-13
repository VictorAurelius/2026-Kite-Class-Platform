# Trial Lifecycle — Business Rules

**Last verified:** 2026-03-24
**Config prefix:** `kitehub.trial`

## Rules

| ID | Rule | Value | Config Key | Code Location |
|----|------|-------|-----------|---------------|
| TR-01 | Trial duration | 14 days | `kitehub.trial.duration-days` | Instance.startTrial() |
| TR-02 | Max trial per owner | 1 | `kitehub.trial.max-per-owner` | InstanceService |
| TR-03 | Warning emails | Day 11, 13 | `kitehub.trial.warning-days: [3,1]` | TrialExpirationChecker |
| TR-04 | Auto-suspend on expire | Yes | — | TrialExpirationChecker |
| TR-05 | Data retention after suspend | 7 days | `kitehub.data-retention.trial: 7` | DataRetentionService |
| TR-06 | Retention warnings | Day 3, 6 after suspend | `kitehub.data-retention.warning-count: 2` | DataRetentionScheduler |
| TR-07 | Re-trial prevention | Block if ever had trial | — | InstanceService |
| TR-08 | Trial conversion cadence + extension | Tăng touch-points nhắc convert (ngành 5-7 email vs hiện 3 day 7/11/13); thêm cơ chế trial extension `kitehub.trial.extension-days` (admin/auto grant rescue). Wave kitehub-biz-100. | GAP-1270; TrialExpirationChecker + email cadence |

## Config

```yaml
kitehub:
  trial:
    duration-days: 14
    max-per-owner: 1
    warning-days: [3, 1]       # ngày trước khi hết
    midpoint-day: 7            # gửi email ngày 7
  data-retention:
    trial: 7                   # ngày giữ data sau suspend
    warning-count: 2           # số lần cảnh báo trước xóa
```

## Five-attribute review per `business-logic-review.md`

Per-rule attributes (Source / Rationale / Reviewer / Compliance check / Review cadence) backfilled at file-level placeholder per Phase 1 of GAP-433. Per-rule granularity tracked via GAP-156 Phase 2 stakeholder sign-offs.

- **Source:** Existing rules in this file derive from a mix of: feature gaps cited inline (where present), ADRs, persona reviews, and informed-gut estimates from Wave 1-30 work. Rules without inline citation default to `informed gut` per `business-logic-review.md` §2.1 and inherit quarterly re-review obligation below.
- **Rationale:** Rule values reflect product judgment + (where applicable) competitor benchmarks + VN regulatory minimums. Detailed per-rule rationale to be backfilled during GAP-156 Phase 2 stakeholder review; until then, treat values as `informed gut` subject to next quarterly review.
- **Reviewer:** @nguyenvankiet (acting Product Owner, solo-dev, 2026-05-08). Formal stakeholder + legal counsel sign-off queued via GAP-156. Solo-dev exemption per `business-logic-review.md` §2.3 — the Reviewer line documents which hat is being worn AND obligation is attached for team-growth or pre-launch trigger.
- **Compliance check:** **N/A** — trial mechanics not regulated (free trial, no commitment, no auto-renewal). Cross-reference `subscription-billing/rules.md` for paid-conversion compliance.
- **Review cadence:** Quarterly (default per `business-logic-review.md` §2.5). **Next review:** 2026-08-08. Event triggers: Competitor pricing change, ≥5% trial-to-paid conversion movement.

## Log

- **2026-05-08** Backfill 5-attribute review section per GAP-433 Phase 1 (`business-logic-review.md` §2 standard). Placeholder Reviewer + Quarterly cadence + domain-specific Compliance check. GAP-156 Phase 2 will replace placeholders with stakeholder sign-offs.
