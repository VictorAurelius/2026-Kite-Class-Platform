# GAP-998: grading_scales RỖNG (no seed) → calculate/finalize 404 + residual legacy NOT-NULL drift (GAP-875 scaffold-close)

**Status:** 🟢 DONE (Wave flow-kc6 G1 walk PASS, 2026-06-05)
**Priority:** 🔴 P0
**Domain:** Backend (schema/migration — KC-6)
**Found:** 2026-06-05 (Wave flow-kc6 pre-walk — schema-drift check, HIGH #1+#2)
**Affects:** `grading_scales` table + `GradeServiceImpl.mapGradeToLetterAndGpa` (calculate + finalize)

## Problem

Hai lỗi cộng dồn trên `grading_scales`:

1. **EMPTY (count=0) + không seed:** `mapGradeToLetterAndGpa` → `findByInstanceIdAndScoreRange` → fallback `findDefaultGradingScales()` (instance_id IS NULL) → cả hai rỗng → `EntityNotFoundException("GRADING_SCALE_NOT_FOUND")` → **404**. KHÔNG có migration seed, KHÔNG có `gradingScaleRepository.save()` path nào. → POST `/{id}/calculate` + `/{id}/finalize` fail ở bước map letter grade. **Blocker chính của KC-6 walk.**
2. **Residual schema↔entity drift (GAP-875 scaffold-close):** cột legacy `grade`/`min_percentage`/`max_percentage`/`gpa` vẫn `NOT NULL` + no default, entity `GradingScale.java` KHÔNG map (chỉ map cột V79 `scale_name`/`letter_grade`/`min_score`/`max_score`/`gpa_value`/`is_default`/`is_passing`). Bất kỳ entity-save scale → `23502` NOT NULL violation (chưa nổ vì 0 save path; Phase 1.5 tenant-custom-scale UI sẽ nổ). Cùng class KC-5 attendance.student_id.

**GAP-875 (DONE) là scaffold-close:** V79 chỉ `ADD COLUMN` cột entity mới, KHÔNG reconcile legacy NOT-NULL. `Wave14EntityDriftMigrationsIT` PASS nhưng chỉ check 1 chiều (entity-cols-exist-in-schema), KHÔNG check "schema NOT-NULL cols đều được entity map". Live schema = ground truth.

## Proposed Fix

`V88__seed_default_grading_scales_fix_drift.sql`:
- `ALTER COLUMN grade/min_percentage/max_percentage/gpa DROP NOT NULL` (legacy, entity không map).
- Seed 8 default scale (instance_id NULL) per BR-GRD-005/006 (A+ 95-100 gpa 4.0 … F 0-59.99 gpa 0.0, F is_passing=false), bands `.99` upper để khớp query `score>=min AND score<=max` với final_score 2-decimal.

## Acceptance Criteria
- [x] calculate/finalize grade với final score → map đúng letter+gpa (W3 calculate → 88.0/B+/3.3; W4 finalize OK; was 404)
- [x] V88 applies clean; scales seeded **per-tenant** (8 × tenant-có-classes; sky=8). NULL-default unreachable by design → GAP-1002
- [x] legacy cols nullable (grade/min_percentage/max_percentage/gpa DROP NOT NULL)

## Related
- Supersedes residual của GAP-875 (DONE scaffold-close — DONE one-way, gap mới reference)
- Class: KC-5 GAP-996 (attendance schema drift) + `postgres-specific-type-testcontainers.md` + `design-patterns.md` §3.12
- Meta follow-up: `Wave14EntityDriftMigrationsIT` drift-check 1 chiều (không bắt unmapped-NOT-NULL) — broad test-infra improvement

## Log

- **2026-06-05 (Wave flow-kc6 — DONE):** V88 ban đầu seed instance_id NULL → FAIL (NOT NULL + tenantFilter/RLS kill NULL default, GAP-1002). Revised → seed per-tenant (INSERT...SELECT DISTINCT classes.instance_id). G1 walk: calculate → 88.0/B+/3.3 (was 404), finalize OK. Legacy cols nullable.
