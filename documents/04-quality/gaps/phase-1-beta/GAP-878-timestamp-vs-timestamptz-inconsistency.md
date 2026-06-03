# GAP-878: TIMESTAMP vs TIMESTAMPTZ inconsistency cross-cluster

**Status:** 🔵 OPEN
**Priority:** 🟡 P2
**Domain:** Backend / DB
**Found:** 2026-06-03 (Wave 13 cluster docs writing — cross-cluster pattern)
**Affects:** Mọi cluster có bảng cũ V1-V24 mix với bảng mới V35+

## Problem

Trộn `TIMESTAMP` (không timezone) và `TIMESTAMP WITH TIME ZONE` (timestamptz) trong cùng cluster + cross-cluster:

- KC attendance-grading 03 §F: bảng V1 timestamptz, V29/V50 TIMESTAMP
- KC finance 04 §A8: `invoices`/`payments`/`payment_records` timestamptz; `payroll_*`/`payment_idempotency_keys` TIMESTAMP
- KC compliance 07 §A5: cả 11 bảng đều TIMESTAMP — bao gồm `deletion_requests.grace_ends_at` (race condition cutover) + `child_protection_audit_log.occurred_at` (mandatory-reporting evidence)
- KC branding 08 §A5: `landing_pages`/`idempotency_keys` timestamptz; `branding`/`branding_resources`/`branding_versions`/`rebrand_approvals`/`frontend_instances`/`outbox_events` TIMESTAMP
- KH auth 01 §A5: trộn V9/V1-V24 TIMESTAMP với V35+ timestamptz
- KH subscription 02 §A8: 100% TIMESTAMP — cross-DB query với KC sẽ drift
- KH branding 03: TIMESTAMP

**Risks:** lệch giờ khi cross-table/cross-cluster compare; `deletion_requests.grace_ends_at` poll precision; mandatory-reporting evidence dispute với MOLISA.

## Proposed Fix

Migration ALTER TYPE TIMESTAMPTZ cho cluster compliance (07 — high legal risk) + đồng nhất per cluster trong follow-up waves. Document Spring `hibernate.jdbc.time_zone=UTC` invariant.

## Acceptance Criteria

- [ ] Compliance cluster 07 ALTER → TIMESTAMPTZ (priority 1)
- [ ] Document timezone convention (UTC) trong architecture doc
- [ ] Other clusters batch-fix in future waves
- [ ] Reference cluster docs 03/04/07/08 + KH 01/02

## Discovered in

7 cluster docs Wave 13.
