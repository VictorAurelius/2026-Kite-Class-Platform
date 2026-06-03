# GAP-907: Compliance cluster cột tham chiếu logic vs FK thật bất nhất

**Status:** 🔵 OPEN
**Priority:** 🟡 P2
**Domain:** Backend / DB
**Found:** 2026-06-03 (Wave 13 cluster docs writing — KC compliance)
**Affects:** `kiteclass-core` compliance cluster tables — `audit_log`, `incidents`, etc.

## Problem

Chỉ duy nhất `parent_complaint_queue` (V56) có FK thật (`parent_id → parents`, `student_id → students`). Toàn bộ các bảng compliance còn lại dùng logical references (string discriminator + ID không-FK):

- `audit_log.aggregate_type/aggregate_id` — string discriminator + string ID cross-aggregate
- (cluster doc cut off but pattern continues)

Lý do logical: cross-aggregate type không thể single FK. Trade-off: integrity check phải thủ công khi cleanup.

## Proposed Fix

Document logical reference convention trong architecture doc. Add integrity check script chạy định kỳ (orphan reference detection). Cân nhắc add FK partial cho subset known (vd `incidents.tenant_id`).

## Acceptance Criteria

- [ ] Architecture doc logical-reference-convention.md
- [ ] Integrity check script
- [ ] Reference cluster doc 07-compliance-audit §A6

## Discovered in

`documents/02-architecture/database/kiteclass/07-compliance-audit.md` §A6
