# GAP-888: Gamification 5/6 tables không có JPA entity — schema-trước-code drift

**Status:** 🔵 OPEN
**Priority:** 🟡 P2
**Domain:** Backend
**Found:** 2026-06-03 (Wave 13 cluster docs writing — KC gamification)
**Affects:** `kiteclass-core` module gamification — 5 bảng không entity

## Problem

Trong code `kiteclass-core/.../module/gamification/`, chỉ có `StudentPoint` entity/repo/service. 5 bảng còn lại (`point_rules`, `badges`, `student_badges`, `rewards`, `reward_redemptions`) tồn tại DB (V1+V26+V58/59+V62/63+V73) nhưng chưa có entity/repository/service JPA.

Drift schema-trước-code: tính năng badge/reward chưa cài đặt tầng app. Khi mở rộng feature cần: tạo entity + ensure `created_by`/`updated_by` UUID (V73 khớp), `version` optimistic lock, `BaseEntity` extension.

Plus: `StudentPoint` entity hiện tại không kế thừa `BaseEntity` (khai cột thủ công, thiếu `updated_at`/`deleted`/`created_by`/`updated_by`/`version`) — drift entity-vs-DB.

## Proposed Fix

Phase 1.5+ feature wave: tạo 5 entity + repo + service. Refactor `StudentPoint` extends `BaseEntity`. Document gamification feature scope trong business doc.

## Acceptance Criteria

- [ ] 5 entity created với BaseEntity extension
- [ ] `StudentPoint` refactor extends BaseEntity
- [ ] Business doc gamification scope
- [ ] Reference cluster doc 06-gamification §C

## Discovered in

`documents/02-architecture/database/kiteclass/06-gamification.md` §C
