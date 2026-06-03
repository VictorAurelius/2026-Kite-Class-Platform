# GAP-876: `assignments` + `submissions` entity ↔ DB drift (missing `deleted`)

**Status:** 🟡 PARTIAL (80%)
**Priority:** 🟠 P1
**Domain:** Backend / DB
**Found:** 2026-06-03 (Wave 13 cluster docs writing — KiteClass attendance-grading)
**Affects:** `kiteclass-core` module assessment; entities `Assignment.java`, `Submission.java` vs `assignments`/`submissions` tables

## Problem

`Assignment.java` map: `weight_percent`, `allow_late_submission`, `late_penalty_percent`, `deleted` (BaseEntity) — migration KHÔNG có cột nào trong số này. Migration có `attachments` (JSONB), `assigned_date`, `instructions` mà entity không map.

`Submission.java` map: `submission_date`, `content_url`, `notes`, `adjusted_score`, `deleted` — migration KHÔNG có. Migration có `content`, `attachments`, `submitted_at` mà entity không map.

Cả 2 bảng thiếu `deleted` trong DB nhưng entity kế thừa `BaseEntity.deleted` (NOT NULL) → soft-delete query sẽ lỗi.

## Proposed Fix

Migration add `deleted BOOLEAN NOT NULL DEFAULT FALSE` cho cả 2 bảng + decide cột legacy DB (drop vs keep). Sync entity fields với migration.

## Acceptance Criteria

- [ ] Migration V## add `deleted` cho `assignments` + `submissions`
- [ ] Entity-DB drift documented hoặc resolved (drop legacy vs map entity)
- [ ] Reference cluster doc 03-attendance-grading §D

## Discovered in

`documents/02-architecture/database/kiteclass/03-attendance-grading.md` §D
