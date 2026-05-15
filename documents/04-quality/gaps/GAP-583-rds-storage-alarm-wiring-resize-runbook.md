# GAP-583: RDS storage alarm wiring + resize runbook

**Status:** 🔵 OPEN
**Priority:** 🟠 P1
**Domain:** DevOps
**Phase:** phase-1-beta
**Found:** 2026-05-15 (Wave 86 Bucket A simulation-3axis audit cell 8)
**Affects:** Production RDS db.t3.micro (kitehub-postgres)

## Problem

RDS db.t3.micro = 20 GiB storage; storage autoscaling DISABLED (cost saving t3.micro tier). Với 5 tenants × 100 students × audit logs + email_send_audit (Wave 85 G-AC4) → ~12 GiB after 30 days (60% threshold). Wave 84 ops audit không wire CloudWatch alarm cho FreeStorageSpace → first detection = service down.

## Root Cause

Wave 84 Bucket H CloudWatch alarms scope chỉ cover CPU/mem/restart, không wire storage alarm. RDS storage growth silent.

## Proposed Fix

1. Terraform `infrastructure/terraform-aws/cloudwatch-alarms.tf`:
   - Add `aws_cloudwatch_metric_alarm.rds_free_storage_low`
   - Threshold: `RDSFreeStorageSpace < 5GB` (25% remaining)
   - Action: SNS topic `production-alerts` → email support@kitehub.me
2. Runbook `documents/05-guides/operations/rds-storage-runbook.md`:
   - When alarm fires: assess growth rate, identify top tables, execute resize procedure
   - Resize: modify-db-instance allocated_storage 20→40 GiB (online, ~5min)
   - Cost impact documented ($0.115/GB/month delta)
3. Self-test: simulate alarm via `aws cloudwatch set-alarm-state` → verify SNS email delivery

## Acceptance Criteria

- [ ] Terraform applied production; alarm visible CloudWatch
- [ ] SNS email delivery verified self-test
- [ ] Runbook reviewed + linked Wave 86 Bucket H runbook
- [ ] Audit log retention policy paired (H-AC5 / GAP-XXX defer)

## Related

- Audit: `documents/04-quality/audits/persona-review/2026-05-15-pre-wave-86-simulation-3axis.md` §3 cell 8 + §5 H-AC2
- Wave 86 plan §3 Bucket H AC H-AC2
- Wave 84 Bucket H ops audit baseline 78/100 (storage alarm gap surfaced)
