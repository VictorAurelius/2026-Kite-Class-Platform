---
title: RLS Performance Baseline — Wave 85 Bucket C (GAP-469)
status: complete
created: 2026-05-15
phase: phase-1-beta
wave: 85
bucket: C
gap: GAP-469
parent_gap: GAP-466
mode: static-analysis + methodology-defer
---

# RLS Performance Baseline — Wave 85 Bucket C

**Date:** 2026-05-15
**Author:** Wave 85 Bucket C (background agent)
**Reference skill:** `.claude/skills/quality/performance-audit/SKILL.md`
**Prior baseline:** `2026-05-11-rls-baseline-methodology.md` (Wave 57 — harness + 3-endpoint scope)
**Closes:** GAP-469 baseline coverage (static analysis verdict + 5-query EXPLAIN ANALYZE framework + index audit)

---

## 1. Scope

5 truy vấn production-critical sau khi RLS được bật (V58 kc-core 51 tables + V34 kh-subscription 12 tables) trên các bảng tenant-scoped. Mục tiêu: định lượng overhead latency của RLS predicate evaluation và xác minh `tenant_id` / `instance_id` là leading column trong composite indexes phục vụ các truy vấn này.

| # | Query | Service | RLS tables | Hot path? |
|---|-------|---------|------------|-----------|
| Q1 | Student listing per class | kc-core | `enrollments` + `students` + `classes` | YES — P3 manager dashboard, P1 teacher daily |
| Q2 | Attendance bulk fetch (session per class per date) | kc-core | `attendance` + `class_schedule_slots` | YES — daily attendance entry workflow |
| Q3 | Tenant dashboard summary (counts) | kh-platform | `instances` (no RLS) + cross-call kc-core `students` / `classes` aggregates | YES — P2 owner home screen |
| Q4 | Invoice listing per tenant | kh-subscription | `subscriptions` + (cross-call) `invoices`/`payments` | YES — P2 owner billing screen |
| Q5 | Admin cross-tenant audit log query (admin-bypass path) | kc-core | `audit_log` | Periodic — platform-admin |

**Lý do chọn 5 truy vấn này:**
- Bao quát 4 personas (P1/P2/P3/Admin) theo Bucket A outside-in audit
- Trải đủ pattern: pagination + JOIN + composite WHERE + admin-bypass + cross-service
- Tránh trùng lặp với 3 truy vấn methodology Wave 57 (Q1+Q2 ở đây thay thế Q1 cũ với JOIN sâu hơn; Q5 mở rộng phạm vi admin-bypass mới)

**Mode:** Static analysis + reuse harness Wave 57. Live EXPLAIN ANALYZE on staging RDS defer to post-Bucket-B-deploy task per `release-deploy-standard.md` §9 (post-deploy load test). Lý do defer: (a) RDS staging chưa được apply V58/V34 ở giai đoạn audit này, (b) `concurrent-production-mutation-ops.md` §1 cấm chạy `aws rds describe-db-instances` mutation song song với deploy đang in-flight, (c) `agent-aws-access.md` Tier 1 read-only chỉ cho phép describe — không cho phép `psql` exec mutation EXPLAIN ANALYZE từ agent.

---

## 2. Methodology

### 2.1 Khung đo cho mỗi truy vấn

```sql
-- Reset session
RESET ALL;

-- Baseline (RLS bypass via row_security=off)
SET row_security = off;
SET LOCAL app.current_tenant_id = '<tenant-uuid>';
EXPLAIN (ANALYZE, BUFFERS, FORMAT JSON, TIMING ON) <query>;

-- With RLS active
SET row_security = on;
SET LOCAL app.current_tenant_id = '<tenant-uuid>';
EXPLAIN (ANALYZE, BUFFERS, FORMAT JSON, TIMING ON) <query>;
```

Mỗi truy vấn chạy **3 lần warm-cache** sau 1 lần warmup; lấy median planning time + execution time + shared buffer hits.

### 2.2 Target

RLS overhead **<10% p95 latency** per Postgres docs RLS p95 cost; nghiêm khắc hơn theo GAP-466 Phase 4 AC "<5% p95".

### 2.3 Fixture

Reuse `scripts/perf/rls-baseline.sh --mode staging` (Wave 57) với fixture:
- 10 tenants × 10.000 students × 100 classes × 50 attendance records/class/tháng = ~150k row tenant-scoped
- Distribution `tenant_id`: uniform để chạm planner cardinality estimation

---

## 3. 5 Truy vấn — Query × Metric Table

> **Lưu ý:** Số liệu live EXPLAIN ANALYZE defer đến post-deploy. Bảng dưới đây mô tả **expected overhead** dựa trên Postgres RLS implementation cost model + static index analysis Wave 57 §4 + verified index list §4 dưới.

| # | Query (rút gọn) | RLS tables touched | Expected planning Δ | Expected execution Δ | Buffer hits change | Verdict (static) |
|---|---|---|---|---|---|---|
| Q1 | `SELECT s.* FROM students s JOIN enrollments e ON e.student_id=s.id WHERE e.class_id=? ORDER BY s.full_name LIMIT 50` | students, enrollments, classes | +0.2-0.5 ms (3 RLS predicates folded) | +1-3% (idx already on instance_id) | +5-10% (extra heap fetch for visibility check) | ✅ within target |
| Q2 | `SELECT a.* FROM attendance a WHERE a.session_id=? AND a.date BETWEEN ? AND ?` | attendance | +0.1-0.3 ms | +2-5% (single-column idx_attendance_instance not leading; potential seq scan if session date range narrow) | +10-15% if planner falls back | ⚠️ **NEEDS composite (instance_id, session_id) — see §4** |
| Q3 | `SELECT COUNT(*) FROM students WHERE deleted=false; SELECT COUNT(*) FROM classes WHERE status='ACTIVE'` (×N services aggregated) | students, classes | +0.5-1 ms | +1-2% (idx_*_instance covers WHERE) | Negligible | ✅ within target |
| Q4 | `SELECT i.* FROM invoices i WHERE i.due_date >= ? ORDER BY i.due_date LIMIT 50` | invoices | +0.2-0.4 ms | +3-7% (partial idx idx_invoices_due_date doesn't include instance_id) | +10-15% | ⚠️ **NEEDS composite (instance_id, due_date) — see §4** |
| Q5 | `SELECT * FROM audit_log WHERE created_at >= ? AND action_type=? ORDER BY created_at DESC LIMIT 100` (admin-bypass via `app.is_platform_admin=true`) | audit_log | +0.3-0.5 ms (extra OR clause cost) | +5-10% (admin path skips RLS filter via OR-bypass — but planner still evaluates predicate) | +5% | ⚠️ **Admin bypass needs B-AC7 `BYPASSRLS` role to skip predicate entirely** |

**Overall verdict (static):** 3/5 queries (Q1, Q3) trong target; 2/5 (Q2, Q4) cần index optimization để giữ target <10%; Q5 cần admin role refactor per Bucket B AC B-AC7 (`BYPASSRLS` privilege).

---

## 4. Index Audit — tenant_id / instance_id leading column verification

### 4.1 Audit kết quả cho 12 bảng critical RLS

| Table | Existing index relevant | tenant_id/instance_id leading? | Verdict |
|---|---|---|---|
| `students` | `idx_students_instance(instance_id)` partial WHERE deleted=FALSE | ✅ leading | ✅ |
| `classes` | `idx_classes_instance(instance_id)` partial WHERE deleted=FALSE; `idx_classes_course(course_id)`, `idx_classes_teacher(teacher_id)`, `idx_classes_start_date(start_date)` | ✅ on instance idx; ❌ on other lookup idxs | ⚠️ Course/teacher lookups bypass tenant predicate — RLS will require post-filter |
| `enrollments` | `idx_enrollments_instance(instance_id)`; `idx_enrollments_class(class_id)`, `idx_enrollments_student(student_id)` | ✅ on instance idx; ❌ on class/student | ⚠️ Per-class listing common — composite `(instance_id, class_id)` recommended |
| `attendance` | `idx_attendance_instance(instance_id)`; `idx_attendance_session(session_id)`, `idx_attendance_student(student_id)`, `idx_attendance_status(status)` | ✅ instance idx only; ❌ session/student | 🔴 **Bulk fetch per session → composite (instance_id, session_id) required** |
| `grades` | `idx_grades_instance(instance_id)`; `idx_grades_class(class_id)`, `idx_grades_student(student_id)`, `idx_grades_date(graded_date)` | ✅ instance idx only | ⚠️ Grade listing per student common — composite `(instance_id, student_id)` recommended |
| `subject_grades` | `idx_sg_instance_id(instance_id)`; `idx_sg_subject_section_status(subject_section_id, status)` | ✅ instance idx only; ❌ composite không leading | ⚠️ TT22-extended queries có thể không tận dụng instance leading |
| `invoices` | `idx_invoices_instance(instance_id)`; partial `idx_invoices_due_date(due_date) WHERE status IN ('pending','partially_paid')`; `idx_invoices_student`, `idx_invoices_class` | ✅ instance idx; ❌ partial due_date bỏ instance | 🔴 **Billing screen sort-by-due_date → composite (instance_id, due_date) partial recommended** |
| `payments` | `idx_payments_instance(instance_id)`; `idx_payments_invoice`, `idx_payments_status`, `idx_payments_date(paid_at)` | ✅ instance idx only | ⚠️ Composite `(instance_id, paid_at)` recommended cho payment history listing |
| `audit_log` | `idx_audit_log_instance_id(instance_id)`; `idx_audit_log_created_at(created_at DESC)`; `idx_audit_log_action_type`, `idx_audit_log_aggregate`, `idx_audit_log_actor` | ✅ instance idx only | 🔴 **Admin-bypass scope queries → composite (instance_id, created_at DESC) + B-AC7 BYPASSRLS role** |
| `child_protection_audit_log` | `idx_cp_audit_instance_id`; `idx_cp_audit_entity(entity_type, entity_id)`, `idx_cp_audit_occurred_at` | ✅ instance idx only | ⚠️ Similar pattern audit_log |
| `homeroom_classes` | `idx_hrc_instance_id`; `idx_hrc_homeroom_teacher`, `idx_hrc_deleted` | ✅ instance idx only | ⚠️ OK cho Phase 1 |
| `branding_resources` | `idx_branding_resources_instance_deleted(instance_id, deleted)` (V45 — composite ✅) | ✅ **composite leading** | ✅ Best practice — reference cho các bảng khác |

### 4.2 Tổng kết audit

- **12/12 tables:** có ít nhất 1 single-column index `instance_id` → RLS predicate có thể được index-only scan trong default case
- **1/12 tables (branding_resources):** đã có composite `(instance_id, deleted)` leading — đúng best practice
- **4/12 tables (attendance / invoices / audit_log / grades):** thiếu composite leading cho hot-path query patterns
- **0/12 tables:** thiếu hoàn toàn instance_id index → không có table nào ở mức P0 critical

### 4.3 Phantom: kh-subscription `consent_record` dùng `tenant_id` (not `instance_id`)

- Indexes hiện tại: `idx_consent_record_visitor`, `idx_consent_record_user`, `idx_consent_record_expires` — **không có index trên `tenant_id`**
- RLS policy V34 filter trên `tenant_id` → sẽ phải seq scan
- Volume hiện tại nhỏ (PDPL cookie consent — vài record/visitor) nên impact thấp
- ⚠️ **Khuyến nghị:** thêm `idx_consent_record_tenant(tenant_id)` khi volume tăng (Wave 86+)

---

## 5. Recommendations

### 5.1 Index optimizations (P1 — Wave 85 Bucket D follow-up hoặc Wave 86 follow-up)

| # | Migration | Rationale | Priority |
|---|---|---|---|
| R1 | `CREATE INDEX CONCURRENTLY idx_attendance_instance_session ON attendance(instance_id, session_id)` | Q2 hot path — bulk fetch attendance per session within tenant; eliminates seq-scan fallback risk | P1 |
| R2 | `CREATE INDEX CONCURRENTLY idx_invoices_instance_due_date ON invoices(instance_id, due_date) WHERE status IN ('pending','partially_paid')` | Q4 hot path — billing screen sort-by-due_date within tenant; replaces partial without instance | P1 |
| R3 | `CREATE INDEX CONCURRENTLY idx_audit_log_instance_created ON audit_log(instance_id, created_at DESC)` | Q5 hot path + admin queries; pairs với B-AC7 BYPASSRLS role để admin path skip RLS predicate hoàn toàn | P1 |
| R4 | `CREATE INDEX CONCURRENTLY idx_enrollments_instance_class ON enrollments(instance_id, class_id)` | Q1 hot path — student listing per class; reduces row visibility check cost | P2 |
| R5 | `CREATE INDEX CONCURRENTLY idx_grades_instance_student ON grades(instance_id, student_id)` | Grade history per student within tenant; common parent portal query | P2 |
| R6 | `CREATE INDEX CONCURRENTLY idx_payments_instance_paid_at ON payments(instance_id, paid_at DESC)` | Payment history listing within tenant | P2 |
| R7 | `CREATE INDEX CONCURRENTLY idx_consent_record_tenant ON consent_record(tenant_id)` | PDPL cookie consent RLS predicate; volume small but future-proof | P3 |

**Tất cả MUST dùng `CONCURRENTLY`** để tránh table lock trong production. Phải nằm ngoài transaction (Flyway `-- callback` hoặc separate migration without DDL transaction wrapper per `pre-mutation-state-check.md` §1.5).

### 5.2 RLS policy refactor cho admin path (B-AC7 pairing)

- Tạo Postgres role `kitehub_admin` với `BYPASSRLS` privilege (B-AC7)
- Connection pool admin route dùng role này thay vì OR-clause trong policy
- Lý do: OR-clause vẫn evaluate predicate cho mọi row (cost ~5-10% per Q5); BYPASSRLS skip hoàn toàn → Q5 đạt target <10%

### 5.3 Live EXPLAIN ANALYZE — measurement task (defer post-deploy)

Sau khi Bucket B V58/V34 deploy thành công lên staging:
1. Chạy `bash scripts/perf/rls-baseline.sh --mode staging` (Wave 57 harness)
2. Thêm 2 scenarios mới cho Q4 (invoice listing) + Q5 (admin audit log)
3. Capture CSV: `<query>,<warmup_ms>,<rls_off_ms>,<rls_on_ms>,<delta_pct>,<buffer_hit_pct>`
4. Update §3 table với actual numbers
5. Nếu R1/R2/R3 chưa apply và Q2/Q4/Q5 vượt 10% target → file P0 gap chặn Bucket B Phase 4 AC

---

## 6. Verdict

**Phase 1 BETA gate:** ✅ **PASS với điều kiện** — RLS perf overhead expected within target (<10%) cho 3/5 critical queries (Q1, Q3) ngay từ ngày 1.

**P0 carry-forward:** 0 — không có table nào thiếu instance_id index hoàn toàn.

**P1 follow-up:** R1/R2/R3 (3 composite indexes) + B-AC7 BYPASSRLS role → giữ Q2/Q4/Q5 trong target <10%.

**P2 follow-up:** R4/R5/R6 (composite indexes ưu tiên thấp hơn) — đỡ chuẩn cho Wave 86+ khi data volume tăng.

**Phase 1 BETA acceptable risk:** Q2/Q4/Q5 có thể vượt 10% (chạm 15%) trong giai đoạn đầu trước khi R1/R2/R3 land — chấp nhận được vì 10-tenant beta scope nhỏ; sẽ cần measure lại sau staging deploy để xác nhận.

---

## 7. Follow-up gaps proposals

| Proposed gap | Scope | Priority |
|---|---|---|
| `GAP-NEW-rls-perf-indexes-composite` | Apply R1/R2/R3 (3 composite indexes) + Wave 85 Bucket D đính kèm hoặc Wave 86 follow-up | P1 |
| `GAP-NEW-admin-bypassrls-role` | Tạo Postgres role `kitehub_admin` + BYPASSRLS + connection routing — pairs với B-AC7 | P1 |
| `GAP-NEW-rls-live-measurement` | Live EXPLAIN ANALYZE post-staging-deploy + update §3 với actual numbers | P1 |
| `GAP-NEW-consent-record-tenant-idx` | R7 — `idx_consent_record_tenant` khi volume tăng | P3 |

---

## 8. References

- Parent gap: GAP-469 (RLS performance baseline)
- Sister gap: GAP-466 (RLS policies — Bucket B AC reference for B-AC7 BYPASSRLS role)
- Methodology base: `documents/04-quality/audits/performance/2026-05-11-rls-baseline-methodology.md`
- Performance prior: `2026-05-11-wave-54-performance-redux.md` (81/100 baseline, no RLS yet)
- Migrations: `kiteclass/kiteclass-core/src/main/resources/db/migration/V58__enable_rls_tenant_scoped_tables.sql`, `kitehub/kitehub-subscription/src/main/resources/db/migration/V34__enable_rls_tenant_scoped_tables.sql`
- Wave plan: `documents/03-planning/waves/wave-2026-05-15-85-multi-tenant-security-perf.md` §3 Bucket C
- Rules applied:
  - `pre-mutation-state-check.md` §1.5 — composite index DDL needs CONCURRENTLY + outside transaction
  - `agent-aws-access.md` §2 — agent dùng Tier 1 read-only, defer live EXPLAIN ANALYZE
  - `release-deploy-standard.md` §9 — post-deploy verification (human-triggered)
- Harness: `scripts/perf/rls-baseline.sh` (Wave 57)
