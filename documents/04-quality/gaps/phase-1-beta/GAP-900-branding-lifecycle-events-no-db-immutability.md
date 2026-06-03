# GAP-900: `branding_lifecycle_events` append-only chỉ ở app layer

**Status:** 🔵 OPEN
**Priority:** 🟠 P1
**Domain:** Backend / DB / Compliance
**Found:** 2026-06-03 (Wave 13 cluster docs writing — KH branding)
**Affects:** `kitehub-branding` `branding_lifecycle_events` table

## Problem

`branding_lifecycle_events` mô tả append-only audit trail trong comment + entity javadoc, nhưng V30 KHÔNG có:
- Trigger BEFORE UPDATE/DELETE (kiểu `consent_record_immutable` V56)
- REVOKE UPDATE/DELETE GRANT cho role app

App code phải tự discipline INSERT-only. Bug ngẫu nhiên (vd `eventRepository.saveAll(...)` sau modify entity in-place) sẽ ghi đè event mà không bị DB từ chối.

## Proposed Fix

Apply pattern V56 (trigger + GRANT revoke) hoặc V60 (RLS UPDATE/DELETE = false) cho `branding_lifecycle_events`. Test profile bypass mechanism documented.

## Acceptance Criteria

- [ ] Migration V## block UPDATE/DELETE
- [ ] IT test verify append-only invariant
- [ ] Reference cluster doc KH 03-branding §A4

## Discovered in

`documents/02-architecture/database/kitehub/03-branding.md` §A4
