# GAP-268a: `/teacher/attendance/[classId]` overview-by-class save endpoint

**Status:** 🟡 PARTIAL — BE endpoint + tests + business docs shipped Wave 51 Bucket B; FE wiring + outbox emission deferred to follow-ups
**Priority:** 🟡 P2 (existing per-tiết save works; class-overview is convenience layer)
**Domain:** Backend (kiteclass-core attendance API extension)
**Found:** 2026-05-10 (Wave 49 Bucket B PARTIAL exit-ramp per `gap-done-discipline.md` §3)
**Parent:** [GAP-268](GAP-268-track-2-port-kiteclass-teacher.md)
**Affects:** `kiteclass-core/.../attendance/**` controllers + service + Flyway migration if schema gaps

## Problem

Wave 49 Bucket B (PR #1094) shipped 11 routes under canonical `(teacher)/teacher/*`. The new `/teacher/attendance/[classId]` overview-by-class route renders correctly + saves locally, but the FE save call hits a **stub TODO** because the backend has no batch save-by-class endpoint. Existing `(teacher)/attendance/period/[classId]/[periodNo]/[date]` route consumes `attendancePeriodApi` (per-tiết save, working since Wave 18b2).

The class-overview UI lets teacher see today's roster across ALL periods at once — a workflow shortcut over the existing per-tiết flow. Without backend support, save action falls back to per-tiết loop (n round-trips), which is what the new UI was designed to avoid.

## Current State (verified 2026-05-10)

| Artifact | Status |
|---|---|
| `attendancePeriodApi.savePeriod(classId, periodNo, date, ...)` | ✅ exists since Wave 18b2 |
| `/teacher/attendance/[classId]` FE route | ✅ shipped Wave 49 Bucket B |
| `attendancePeriodApi.saveOverviewByClass(classId, date, ...)` batch endpoint | ❌ does not exist |
| Backend controller `AttendancePeriodController.saveBatchByClass` | ❌ does not exist |

## Proposed Fix

1. State-check `kiteclass-core` attendance domain for existing batch patterns
2. Add `POST /api/v1/attendance/class/{classId}/batch?date=YYYY-MM-DD` accepting array of `{periodNo, studentId, status}` records
3. Service: validate teacher has access to class → upsert per (class × period × student × date) tuple → emit outbox event per `design-patterns.md` §3.5.1
4. Add integration test verifying single round-trip saves all periods
5. Wire FE in `attendancePeriodApi.saveOverviewByClass` to consume new endpoint
6. Update `documents/01-business/kiteclass/attendance/api-contract.md` + `use-cases.md`

## Acceptance Criteria

- [x] `POST /api/v1/attendance/class/{classId}/batch` documented in api-contract.md (extended `documents/01-business/kiteclass/attendance/api-contract.md` + `use-cases.md` UC-ATT-09 + `rules.md` BR-ATT-CLASS-BATCH-001/002)
- [x] Backend integration test passes (`AttendanceClassBatchControllerIT` — 4 tests: happy path captures classId+date+entries forwarded to service, empty entries → 400, periodNo > 10 → 400, missing X-Teacher-Id → error)
- [ ] FE `attendancePeriodApi.saveOverviewByClass` calls new endpoint (no stub TODO) — **deferred to FE follow-up gap; out of scope of Wave 51 Bucket B per plan §3**
- [ ] Outbox event published per attendance domain pattern — **deferred; existing `upsertBatch` path uses `ApplicationEventPublisher` only without outbox emission, new endpoint mirrors that behaviour to keep upsert-path uniform; cross-cutting outbox refactor tracked separately**
- [ ] GAP-268 parent gap "Daily attendance saves to backend" AC ✅ verifiable — pending FE wiring follow-up

## Related

- Parent: GAP-268
- Sibling: GAP-268b (Playwright E2E flow that exercises this endpoint)
- Wave 49 Bucket B PR #1094

## Log

- **2026-05-10**: Filed at Wave 49 closure as named follow-up promised in GAP-268 Log entry. Per `audit-to-gap-pipeline.md` §3 + `gap-done-discipline.md` §3, deferred items get real gap files.
- **2026-05-10**: Wave 51 Bucket B shipped backend portion. `POST /api/v1/attendance/class/{classId}/batch` endpoint live via `AttendanceClassBatchController` + `AttendancePeriodService.upsertClassBatch` (thin adapter that folds classId+date into existing per-row upsert). DTOs `ClassBatchAttendanceRequest` + `ClassBatchAttendanceEntry` capped at 200 cells. Integration test `AttendanceClassBatchControllerIT` covers happy path + 400 validation. Business docs extended (api-contract.md + use-cases.md UC-ATT-09 + rules.md BR-ATT-CLASS-BATCH-001/002 with 5-attribute review). Status flips OPEN → PARTIAL per `gap-done-discipline.md` §3 — FE wiring + outbox emission named-deferred.
