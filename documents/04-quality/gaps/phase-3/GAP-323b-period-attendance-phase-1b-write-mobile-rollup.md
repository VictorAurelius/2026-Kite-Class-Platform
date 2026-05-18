# GAP-323b: Period Attendance Phase 1B — Write API + GVCN mobile UI ≤2min + daily roll-up + load test

**Status:** 🟡 PARTIAL — Phase 1B v1 (Write API + on-demand daily rollup + V51 CHECK) shipped 2026-05-04
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

- [x] Write API + idempotency + optimistic lock — POST batch + PATCH single, V50 unique-tuple upsert + JPA `@Version`
- [ ] GVCN mobile UI tap-grid; Playwright perf test ≤ 2 min for 42 HS — deferred to follow-up PR
- [ ] Offline queue + retry mechanism — deferred to follow-up PR
- [x] Daily roll-up endpoint — Phase 1B v1: on-demand SQL aggregation. Materialized-view variant deferred to follow-up PR (BR-PERIOD-ATT-010 §note)
- [ ] Concurrent load test 30 GVCN passes (P99 ≤ 2s) — deferred to follow-up PR
- [ ] Parent portal /attendance facet exposed (coordinate GAP-321b) — deferred to follow-up PR
- [x] DB CHECK constraint on `period_no` range — V51 narrows to BETWEEN 1 AND 10 (cross-DB `vertical_type` pairing dropped as un-expressible; service-layer only)
- [x] Tests: idempotency unit + write API IT (9 unit + 10 IT green); mobile UI Playwright + concurrent load test deferred to follow-up
- [x] Business docs updated: BR-PERIOD-ATT-{008..011} + UC-PERIOD-ATT-W-001/002 + UC-PERIOD-ATT-R-005
- [x] mvn green (kiteclass-core); pnpm N/A (no FE in this PR)

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

- **2026-05-04** (Phase 1B v1 mobile UI, Wave 18b2 Bucket A) — Shipped GVCN
  per-tiết mobile UI v1 on top of the backend foundation: route shell at
  `/attendance/period/{classId}/{periodNo}/{date}` ((teacher) route group),
  `PeriodTapGrid` (4 status buttons × ≤42 students), `PeriodBulkActions`
  ("mark all present" / "reset" / "save"), `attendancePeriodApi` client
  (POST batch + PATCH + GET roster), `useDailyRoster` +
  `useUpsertAttendancePeriod` TanStack hooks. Extended UC-PERIOD-ATT-UI-001
  from placeholder to full UC; added FE-behaviour subsections in W-001 +
  W-002. `pnpm test` + `pnpm build` both green. Status stays 🟡 PARTIAL —
  offline queue (UC-PERIOD-ATT-UI-002), Playwright ≤2-min perf assertion,
  multi-period quick-switch, single-row PATCH merge dialog, and the rest of
  the §1B follow-up scope all remain deferred per the existing AC checklist.
- **2026-05-04** (Phase 1B v1, this PR) — Shipped backend foundation: idempotent batch upsert (BR-PERIOD-ATT-008), optimistic-lock PATCH (BR-PERIOD-ATT-009), on-demand daily roll-up (BR-PERIOD-ATT-010, matview deferred), `period_no` range CHECK V51 (BR-PERIOD-ATT-002 tightened from `>0` to `BETWEEN 1 AND 10`), recording-header contract (BR-PERIOD-ATT-011 — fine-grained RBAC deferred), 3-layer business docs synced. New error code `OPTIMISTIC_LOCK_CONFLICT` on `GlobalExceptionHandler`. 19 tests green (9 unit + 10 IT TestContainers Postgres). Status flips OPEN → PARTIAL per `gap-done-discipline.md` §3; mobile UI / offline queue / matview / concurrent load test / parent-portal facet / fine-grained RBAC explicitly deferred to follow-up PRs (each tracked in `documents/01-business/kiteclass/period-attendance/rules.md` §4).
- **2026-05-04** — Wave 18b3 Bucket A (PR #780) shipped IndexedDB-backed offline queue + k6 perf test for the period-attendance batch upsert endpoint. New `src/lib/offline/{attendance-queue.ts, use-offline-attendance-queue.ts, sync-status-badge.tsx}` using `idb` v8: `enqueue / drain / retry` API + `online` event auto-drain + colour-coded `OfflineSyncStatusBadge` (queued 🟡 / syncing 🔵 / synced 🟢 / failed 🔴 with retry button) + persistence across page reload. Hook wired into existing `(teacher)/attendance/period/[classId]/[periodNo]/[date]/page.tsx` mobile route from Wave 18b2. 14 new tests (612/612 FE suite passing, was 598; 0 regressions). `pnpm build` strict-mode green (Next.js 15 production build clean). k6 perf script `tests/perf/attendance-period-concurrent.k6.ts` committed: 30 VUs ramping (30s→4min soak→30s ramp-down), `http_req_duration` threshold `p(95)<2000`, `http_req_failed<0.01`, `checks>0.99` — assertions match BR-PERIOD-ATT-008 §note "30 GVCN concurrent ≤2min" target. Live k6 run pending backend stack availability (documented in `tests/perf/README.md` §"Live-run status" — runbook for operator with stack up). Coordinator inline fix: agent's k6 script tripped Next.js typecheck in CI (`(r) => r.status` implicit any in `check()` callback) — fixed via `// @ts-nocheck` since k6 has its own runtime + type system, not part of Next.js compile graph. Status stays 🟡 PARTIAL — PWA service-worker background-sync API, conflict-resolution UI when remote `version` advances during offline window, queue size cap + LRU eviction routed to follow-up sub-gap.
- **2026-05-04** — Filed by Wave 18b1 closure coordinator. Per `gap-done-discipline.md` §3 PARTIAL exit ramp.
