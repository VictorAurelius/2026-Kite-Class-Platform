# GAP-323b: Period Attendance Phase 1B — Write API + GVCN mobile UI ≤2min + daily roll-up + load test

**Status:** 🔵 OPEN
**Priority:** 🔴 P0 (sister of GAP-323 Phase 1A SHIPPED Wave 18b1)
**Domain:** Backend + Frontend (mobile)
**Detected:** 2026-05-04 (Wave 18b1 Bucket F closure)
**Affects:** P5 K-12 daily ops (AC-OPS-001..003); P3/P4 future K-12 conversion

## Context

Phase 1A SHIPPED Wave 18b1 (PR #765): AttendancePeriod entity + 4 read-only GET endpoints + V50 migration + tenant.vertical_type discriminator + 3-layer business docs. Read-only Phase 1A — no write yet.

This gap covers Phase 1B — write API + GVCN mobile UI + daily aggregation. Phase 1B = the actual daily-ops feature; Phase 1A laid foundation.

## Problem

GVCN (homeroom teacher) needs to record attendance for 30-42 students per period × 5-10 periods/day. AC-OPS-001 mandates ≤2 min per period. Without:
- Mobile-optimized tap-grid UI
- Idempotent write API
- Concurrent load handling (30 GVCN tiết 1 simultaneously 07:00-08:00)
- Daily roll-up view (vắng cả ngày = ≥7 tiết vắng)

K-12 daily ops fail.

## Proposed Fix

### 1B.1 — Write API
- `POST /api/v1/attendance/periods` — create attendance entries (batch)
- `PATCH /api/v1/attendance/periods/{id}` — update single entry
- Idempotency: composite UK on (student_id, class_id, subject_section_id, period_no, date) — already in V50; ON CONFLICT DO UPDATE pattern in service
- Optimistic locking via existing version column
- RBAC: TEACHER assigned to subject_section can write; admin can override

### 1B.2 — GVCN mobile UI
- Mobile-optimized React route: `/teacher/attendance/period/[classId]/[periodNo]/[date]`
- Tap-grid: 42 students × 4 status buttons (P / A-excused / A-unexcused / Late)
- Bulk actions: "Mark all present" + "Reset"
- Submit ≤ 2 min (Playwright performance test target)
- Offline-tolerant: queue submissions if network drops; retry on reconnect
- Inherit from previous period (tiết bộ môn inherits GVCN tiết 1 status; deltas only)

### 1B.3 — Daily roll-up view
- New materialized view `daily_attendance_roll_up`: per (student_id, date) → period_count, present_count, absent_count, late_count, all_day_absent (boolean: absent_count ≥ 7)
- Refresh trigger: on attendance_period insert/update (debounced)
- Endpoint: `GET /api/v1/attendance/daily-rollup` for GVCN dashboard

### 1B.4 — Concurrent load test
- Playwright + Gatling test: 30 concurrent GVCN sessions submit attendance for 42-HS class within 5min
- Verify no DB lock contention, no duplicate entries, P99 latency ≤ 2s per submission
- Baseline metrics committed to `documents/04-quality/audits/performance/`

### 1B.5 — Parent portal exposure
- Period attendance visible on `/parent/children/{id}/attendance` (depends GAP-321b 1B.1)

### 1B.6 — Per-table CHECK constraint
- Phase 1A enforces period_no required when K12_SCHOOL in service layer (TODO comment); Phase 1B add DB-level CHECK constraint: `vertical_type = 'K12_SCHOOL' IMPLIES period_no IS NOT NULL`

## Acceptance Criteria

- [ ] Write API + idempotency + optimistic lock
- [ ] GVCN mobile UI tap-grid; Playwright perf test ≤ 2 min for 42 HS
- [ ] Offline queue + retry mechanism
- [ ] Daily roll-up materialized view + endpoint
- [ ] Concurrent load test 30 GVCN passes (P99 ≤ 2s)
- [ ] Parent portal /attendance facet exposed (coordinate GAP-321b)
- [ ] DB CHECK constraint on vertical_type↔period_no relationship
- [ ] Tests: idempotency unit + write API IT + mobile UI Playwright + load test
- [ ] Business docs updated: BR-PERIOD-ATT-{write/rollup/concurrency} + UC-GVCN-DIEM-DANH
- [ ] mvn + pnpm green

## Estimated Effort

~2-3 weeks:
- 323b.1: Write API + idempotency (~5 days)
- 323b.2: GVCN mobile UI (~7 days; mobile UX is finicky)
- 323b.3: Offline-tolerant queue (~3 days)
- 323b.4: Daily roll-up + concurrent load test (~5 days)
- 323b.5: Parent portal exposure (~2 days, coord GAP-321b)
- 323b.6: DB CHECK constraint (~1 day)

## Related

- **Sister of:** GAP-323 Phase 1A (PR #765) + GAP-323c Phase 1C
- **Cross-cuts:** GAP-321b (parent portal /attendance facet), GAP-322 (incident link if attendance pattern triggers safeguarding)
- **Wave plan:** `documents/03-planning/waves/wave-2026-05-04-18b1-k12-legal-phase-1a.md`

## Log

- **2026-05-04** — Filed by Wave 18b1 closure coordinator. Per `gap-done-discipline.md` §3 PARTIAL exit ramp.
