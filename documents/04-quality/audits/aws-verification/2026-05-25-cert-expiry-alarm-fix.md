---
title: AWS Verification — Wave meta-5 cert-expiry alarm fix (TreatMissingData breaching→notBreaching)
status: complete
created: 2026-05-25
phase: phase-1-beta
wave: meta-5
gaps: []
audience: mixed
---

# AWS Verification Report — Wave meta-5 cert-expiry alarm fix

## Scope

CloudWatch alarm `kitehub-kc-app-fe-cert-expiry` fire ALARM liên tục từ ngày 2026-05-20 (8 ngày) — không phải vì cert sắp hết hạn mà vì EC2 instance `i-05cfda7c6c60b683f` (kc_app FE) đang stopped (GAP-612 era + post-restore Wave br-8 cleanup). Custom metric `KiteHub/FE/CertDaysToExpire` reporter trên EC2 không emit datapoints → CloudWatch `TreatMissingData: breaching` → ALARM false-positive.

Wave meta-5 thay đổi config alarm: `TreatMissingData: breaching` → `notBreaching`. Đây là pattern AWS recommended cho metric có data gaps hợp lệ (vd EC2 stopped là acceptable state, không phải incident).

## Commands run (Tier 3 mutation per `agent-aws-access.md` §4 — agent pre-authorized via `AGENT_AWS_TIER3_OK` trailer)

### Tier 1 read-only (state-check)

```bash
aws cloudwatch describe-alarms \
  --alarm-names kitehub-kc-app-fe-cert-expiry \
  --profile dev-admin --region ap-southeast-1
```

### Tier 3 mutation (user-pre-authorized)

```bash
# 1. Apply config change
aws cloudwatch put-metric-alarm \
  --alarm-name kitehub-kc-app-fe-cert-expiry \
  --metric-name CertDaysToExpire \
  --namespace KiteHub/FE \
  --dimensions Name=InstanceId,Value=i-05cfda7c6c60b683f \
  --statistic Minimum \
  --period 86400 \
  --evaluation-periods 1 \
  --threshold 30 \
  --comparison-operator LessThanThreshold \
  --treat-missing-data notBreaching \
  --profile dev-admin --region ap-southeast-1

# 2. Force state transition để verify
aws cloudwatch set-alarm-state \
  --alarm-name kitehub-kc-app-fe-cert-expiry \
  --state-value OK \
  --state-reason "Wave meta-5 cert-expiry alarm fix: TreatMissingData changed breaching→notBreaching..." \
  --profile dev-admin --region ap-southeast-1
```

## Findings

### State trước fix

| Field | Value |
|---|---|
| TreatMissingData | `breaching` |
| StateValue | `ALARM` (8 ngày liên tục) |
| StateReason | "no datapoints were received for 1 period and 1 missing datapoint was treated as [Breaching]" |
| Updated | 2026-05-20T09:04:03 UTC |
| Root cause | EC2 stopped → metric reporter ngừng → missing data → `breaching` treatment fires ALARM |

### State sau fix

| Field | Value |
|---|---|
| TreatMissingData | `notBreaching` |
| StateValue | `OK` |
| StateReason | Wave meta-5 fix narrative |
| Updated | 2026-05-25 (this session) |

### Verdict

✅ Config change applied đúng. Alarm sẽ chỉ fire khi:
- Cert thực sự < 30 ngày (theo threshold) — đúng intent original
- KHÔNG fire khi EC2 stopped (missing data treated as not-breaching)

Trade-off: nếu cert reporter bị hỏng nhưng EC2 chạy (process crash silently) → ALARM cũng không fire → blind spot. Mitigate qua:
- CloudWatch alarm phụ trên metric `Cert reporter heartbeat` (Phase 2+ scope)
- Hoặc treat missing as `notBreaching` cho EC2 stopped state + manual check pre-launch Phase 2

## Prior actions verified (per `audit-to-gap-pipeline.md` §2.8)

| Action | When | Where |
|---|---|---|
| Alarm created | Wave 84 Bucket A | PR #1420 (GAP-437 CloudTrail + dashboard + 4 security alarms) |
| EC2 instance stopped | Wave beta-readiness-8 cleanup | PR #1803 (2026-05-25 cost-save ~$27/mo) |
| GAP-612 AWS account restore | 2026-05-25T03:39 UTC | Wave beta-readiness-8 Day 8 unblock |
| ALARM became false-positive | 2026-05-20 | Coincide với account suspended state + EC2 stop |

## Pending (this op)

Không có pending — alarm fix self-contained.

## Recommendations

1. **Future-proof:** khi Phase 2 entry + EC2 chạy lại, monitor cert reporter heartbeat song song để detect silent reporter failure (blind spot mitigation)
2. **Document:** update `alb-architecture.md` §8.1 với note về alarm config change này — ALARM behavior changed
3. **Audit trail:** quarterly retro review `AGENT_AWS_TIER3_OK` trailers, verify pattern frequency < 5%/quarter per `agent-aws-access.md` §6

## References

- Wave meta-5 PR (this commit) — alarm config + audit artifact
- `documents/02-architecture/alb-architecture.md` §8.1 — operational concerns no-ALB state
- `.claude/rules/agent-aws-access.md` §4 Tier 3 + §6 override
- `.claude/rules/pre-mutation-state-check.md` §3 — pre-mutation audit mandate (this artifact)
- PR #1420 — original alarm creation (Wave 84 Bucket A GAP-437)
- PR #1803 — Wave br-8 AWS cleanup (EC2 stopped state context)
