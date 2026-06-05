# GAP-990: K12 HomeroomClassService.enrollStudent thiếu status guard (cross-flow sweep của GAP-989)

**Status:** 🔵 OPEN
**Priority:** 🟢 P3
**Domain:** Backend (K12 module — Phase 3)
**Found:** 2026-06-05 (Wave flow-kc4 GAP-989 cross-flow sweep)
**Affects:** `HomeroomClassService.enrollStudent` (K12 module)

## Problem

Cross-flow sweep của GAP-989 (per `cross-flow-bug-class-sweep.md`) phát hiện `HomeroomClassService.enrollStudent` (K12 homeroom) cũng thiếu class-status guard. KHÁC `EnrollmentServiceImpl`: entity khác (`HomeroomClass`, school-year homeroom, không có lifecycle SCHEDULED/COMPLETED/CANCELLED), error model khác (`IllegalStateException`). K12 = Phase 3 scope (ngoài Phase 1 BETA) → defer.

## Proposed Fix

Khi K12 vào scope (Phase 3): thêm status/lifecycle guard cho HomeroomClass enrollment phù hợp với model homeroom (active school-year check).

## Acceptance Criteria
- [ ] Enroll vào homeroom không active → reject (Phase 3)

## Related
- Sweep parent: [[GAP-989]] (Phase 1 enrollment guard — fixed)
- Phase 3 K12 scope
