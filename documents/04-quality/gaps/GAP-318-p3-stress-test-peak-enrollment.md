# GAP-318: Stress Test Framework for Peak Enrollment Scenarios

**Status:** 🔵 OPEN
**Priority:** 🟡 P2
**Domain:** DevOps (k6 / Gatling stress test scripts) + observability
**Found:** 2026-05-04 (Persona Review Round 1 — P3 Bucket C)
**Affects:** 2 ACs — tenant + admin

---

## Problem

P3 đầu kỳ (tháng 8 / tháng 1) có peak: 50 enrollments/giờ × 3 lễ tân concurrent. System phải:
- No 5xx errors
- Response time <2s p95
- No duplicate parent accounts khi siblings cùng đăng ký
- Payment processing không lost transactions
- UI không freeze

## Root Cause

Không có stress test framework. Performance audit chưa cover persona scale (peak enrollment).

## Current State (verified 2026-05-04)

| Component | Path | State |
|-----------|------|-------|
| Stress test scripts | — | ❌ missing |
| Performance audit cover persona scale | `documents/04-quality/audits/performance/` | ⚠️ baseline 63/100 but no peak scenarios |
| Idempotency on enrollment endpoint | — | ⚠️ unknown |
| Duplicate detection cho parent account | — | ⚠️ unknown |

## Proposed Fix

1. k6 (or Gatling) script simulating 3 concurrent users × 17 enrollments/giờ × 2 giờ
2. Test invariants: no 5xx, p95 <2s, no duplicate parent, payment integrity
3. CI workflow scheduled weekly stress test
4. Performance audit add "peak enrollment" scenario per persona
5. Optimization triggers: if test fails, identify bottleneck (DB query, lock contention, etc.)

## Acceptance Criteria

- [ ] k6 script committed in `tools/stress-tests/peak-enrollment.js`
- [ ] Test passes baseline criteria on local stack
- [ ] CI scheduled weekly stress test workflow
- [ ] Performance audit report updated with peak enrollment scenario
- [ ] Idempotency keys on enrollment endpoint
- [ ] Duplicate parent detection logic verified

## Linked ACs

| AC ID | Persona | Doc |
|-------|---------|-----|
| AC-EDGE-002 | Tenant Director | `P3-medium-center.md` |
| AC-EDGE-001 | Admin | `secondary/admin-in-P3.md` |

## Related

- Existing: post-wave performance audit (Wave 5 baseline 63/100)
- Persona review: §2 (Tenant AC-EDGE-002), §4 (Admin AC-EDGE-001)

## Log

- **2026-05-04** Created from Persona Review Round 1 P3 Bucket C.
