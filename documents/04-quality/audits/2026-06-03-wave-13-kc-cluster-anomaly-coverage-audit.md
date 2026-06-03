---
title: "Wave 13 META — KiteClass cluster pre-session anomaly coverage audit"
audience: mixed
created: 2026-06-03
last-reviewed: 2026-06-03
---

# Wave 13 META — Audit anomaly coverage 3 cluster KC merge trước session

## TL;DR

User flagged 5 cluster KiteClass database docs merge TRƯỚC session hôm nay (cluster 01/02/03/05/06) không được kiểm soát chất lượng anomaly coverage. Quick grep cho thấy 2 cluster (01 + 02) thiếu hẳn `## Ghi chú schema (anomalies)` section, 1 cluster (06) có section nhưng cần verify depth so với baseline (04-finance.md = 10 anomalies A1-A10 structured).

| Cluster | Section anomaly | Anomaly depth | Verdict |
|---|:---:|:---:|:---|
| **01-academic-structure** | ❌ thiếu hẳn | scattered inline drift notes | 🟠 **NEEDS FIX (P1)** — phải gom thành `## Ghi chú schema (anomalies)` rời |
| **02-people-enrollment** | ❌ thiếu hẳn | scattered inline drift notes | 🟠 **NEEDS FIX (P1)** — phải gom thành `## Ghi chú schema (anomalies)` rời |
| **06-gamification** | ✅ có (A-E, 5 anomalies) | structured + depth OK | 🟢 **PASS** — nhưng có thể bổ sung 1-2 anomaly cross-reference |

**Aggregate verdict**: 2/3 cluster cần follow-up rewrite append `## Ghi chú schema` section, model theo 04-finance.md baseline (A1-A10 structured). Cluster 06 đạt chuẩn (5 anomalies covered + cross-reference).

**Recommended follow-up**: 1 bundled fix PR cho cả cluster 01 + 02 (cùng class fix: append anomaly section). Cluster 06 nice-to-have polish (1-2 cross-link bổ sung) — defer hoặc skip.

**Cluster nằm ngoài scope audit này (do user task explicit chỉ định 01/02/06)**: cluster 03-attendance-grading (29 mentions, 1 section) + cluster 05-rbac (49 mentions, 1 section) — nên audit follow-up cùng class.

---

## Baseline reference — 04-finance.md (Wave 13 batch hôm nay)

Cluster 04-finance.md baseline đạt chuẩn cao nhất trong batch hôm nay với `## Ghi chú schema (anomalies)` section gồm **10 anomalies A1-A10**:

| ID | Anomaly class |
|---|---|
| A1 | Two parallel payment systems (`payments` V1 vs `payment_records` V69) |
| A2 | Entity↔table drift NẶNG (`Payment` entity ↔ `payments` table — cột entity không có migration) |
| A3 | Entity↔table drift (`Invoice` ↔ `invoices` — thiếu `deleted`/`enrollment_id`) |
| A4 | Enum↔CHECK constraint drift (`invoices.status` 6 lowercase vs entity 7 UPPERCASE; `invoice_items.item_type`) |
| A5 | Money type inconsistency (DECIMAL(12,2) / NUMERIC(19,2) / DECIMAL(15,2) / BIGINT minor-unit comment) |
| A6 | Actor BIGINT bị V73 sweep BỎ SÓT (`payments.received_by`, `payments.payer_id`, `payment_records.recorded_by`, `payment_idempotency_keys.user_id`) |
| A7 | `version` thiếu DEFAULT 0 trên `invoices`, `payments`, `payment_records` |
| A8 | TIMESTAMP vs TIMESTAMPTZ không nhất quán (V1 + V69 dùng TZ; V48 + V61 dùng naive) |
| A9 | RLS coverage gap (`payment_records` V69 + `payment_idempotency_keys` V61 — tạo SAU V58/V59 → CHƯA bật RLS DB-level) |
| A10 | Idempotency table neighbor (`idempotency_keys` V66 shared cross-domain — không thuộc cluster nhưng phục vụ luồng PAYMENT) |

→ 10 anomaly class = checklist gold standard. Audit dưới đây compare 3 cluster target với checklist này + verify anomalies SHOULD have been documented.

---

## Cluster 01 — Academic Structure

### Structural completeness checklist

| # | Check | Result |
|---|---|:---:|
| 1 | `## ... [Aa]nomalies` section header structured | ❌ **MISSING** |
| 2 | Mỗi bảng có per-table column detail (kiểu/null/default/index/ý nghĩa) | ✅ tất cả 12 bảng đầy đủ |
| 3 | ERD Mermaid `erDiagram` có / đủ entity | ✅ Mermaid ERD bao 12 bảng |
| 4 | "Nguồn đọc" section (migrations + entities + enums) | ✅ TL;DR + cite migrations V1→V77 |
| 5 | TL;DR front-matter | ✅ đầy đủ |

### Anomalies found inline nhưng KHÔNG gom — should be promoted to `## Ghi chú schema` section

Đọc kỹ 514 dòng, anomalies SCATTERED inline nhiều, chưa gom thành section riêng. Liệt kê 10 anomaly classes SHOULD have been documented:

#### A1 — `class_schedules` + `class_sessions` + `course_prerequisites` thiếu `instance_id` → KHÔNG bật RLS (tương tự A9 baseline)

**Evidence**: Verified với V58 — 3 bảng này KHÔNG nằm trong danh sách RLS (V58 list không include `class_schedules`, `class_sessions`, `course_prerequisites`).

Doc đã đề cập inline (cluster doc line 21+ TL;DR + bảng-by-bảng), nhưng KHÔNG gom thành section anomaly. Reader phải scan từng bảng để biết bảng nào RLS skip.

**Fix**: Promote thành dedicated `## A1 — Bảng thiếu instance_id → ngoài scope RLS V58` ở section anomaly cuối doc.

#### A2 — Entity↔DB drift nặng cho `ClassSession`

**Evidence**: cluster doc line 418 ghi rõ — `ClassSession` extends `BaseEntity` ⇒ KHAI BÁO `instance_id`, `deleted`, `location`, `attendance_taken` NHƯNG **KHÔNG có migration nào thêm các cột này vào bảng**. Runtime có thể lệch schema. Đây là drift cùng class với A3 baseline (`Invoice` ↔ `invoices` thiếu `deleted`/`enrollment_id`).

**Fix**: Promote inline note thành `## A2 — Entity ClassSession ↔ bảng class_sessions drift` standalone.

#### A3 — `courses` Entity↔DB drift (cột legacy V1 không map + cột entity dùng tên khác)

**Evidence**: cluster doc line 271 — entity dùng `cover_image_url` KHÔNG `thumbnail_url`; `suggested_tuition`, `default_sessions` là cột V1 không còn map; `price` deprecate. Pattern giống A2 baseline cho `Payment` entity drift.

**Fix**: Standalone `## A3 — Entity Course ↔ bảng courses drift` (legacy V1 columns + entity name divergence).

#### A4 — `classes` Entity↔DB drift (cột V1 legacy `code`, `tuition_amount`, `tuition_type` không map)

**Evidence**: cluster doc line 365 inline note "Drift entity↔DB". Pattern lặp lại A2/A3.

**Fix**: gom chung với A3 trong section `## A3 — Entity ↔ DB drift cluster summary` (3 bảng `courses`, `classes`, `class_sessions`).

#### A5 — `courses.teacher_id` BIGINT bị V73 sweep BỎ SÓT (tương tự A6 baseline)

**Evidence**: cluster doc line 254 inline note "V73 chỉ đổi `classes.teacher_id` sang UUID, không đổi `courses.teacher_id`". Đồng thời `subject_sections.teacher_id`, `homeroom_classes.homeroom_teacher_id` cũng BIGINT.

**Fix**: Standalone `## A5 — Actor BIGINT bị V73 sweep BỎ SÓT` liệt kê 3 cột:
- `courses.teacher_id` BIGINT (V27 thêm)
- `subject_sections.teacher_id` BIGINT (V29)
- `homeroom_classes.homeroom_teacher_id` BIGINT (V29 soft ref)

Đây là risk class identical với A6 baseline — nếu code ghi từ `X-User-Id` JWT (UUID) → parse fail.

#### A6 — `class_schedule_slots` có cột `deleted_at` không khai báo trong BaseEntity

**Evidence**: cluster doc line 507 inline note "drift nhỏ — entity không map `deletedAt`". Cùng class A3 baseline.

**Fix**: nhập chung A2 (Entity drift cluster summary).

#### A7 — `R67__undo_pricing_model.sql` là rollback script THỦ CÔNG

**Evidence**: cluster doc line 271 inline note "R67 KHÔNG tự áp dụng bởi Flyway". Đây là operational anomaly cần Standalone — flag rõ cho người vận hành.

**Fix**: Standalone `## A7 — R67 rollback script thủ công không tự apply`.

#### A8 — Cột `version` thiếu DEFAULT 0 trên bảng V1 cũ (`classes`, `courses`, `enrollments`...)

**Evidence**: cluster doc line 162+ ghi `version YES DEFAULT 0 (V62/V63)` — implicit thừa nhận V62/V63 set DEFAULT 0 SAU khi V26 thêm cột thiếu DEFAULT.

**Fix**: Standalone `## A8 — version DEFAULT 0 backfill batched V62/V63`. Risk: raw INSERT vào snapshot test giữa V26→V62 sẽ NPE flush.

#### A9 — TIMESTAMP vs TIMESTAMPTZ không nhất quán trong cluster

**Evidence**: cluster doc inline — `courses`, `classes`, `class_sessions`, `class_schedules` dùng `TIMESTAMP WITH TIME ZONE`; `academic_years`, `semesters`, `holidays`, `curricula`, `homeroom_classes`, `subject_sections`, `class_schedule_slots` dùng `TIMESTAMP` (naive). Đây là pattern identical A8 baseline 04-finance.md.

**Fix**: Standalone `## A9 — TIMESTAMP vs TIMESTAMPTZ mismatch`. 12 bảng split ~half-half — risk khi compare timestamps cross-table.

#### A10 — Hai mô hình song song (center model `classes` vs K-12 `homeroom_classes` + `subject_sections`) — Strangler Fig

**Evidence**: cluster doc TL;DR + line 424 ADR-001 Strangler Fig. Khá quan trọng để đặt ở §anomaly vì FK domain phân nhánh + service code có thể nhầm 2 mô hình.

**Fix**: Standalone `## A10 — Strangler Fig — 2 mô hình lớp song song (`classes` vs `homeroom_classes`/`subject_sections`)`. Match pattern A1 baseline (`payments` vs `payment_records`).

### Gap delta vs 04-finance.md baseline (10 anomaly classes)

| Baseline A class | Cluster 01 inline equivalent | Should be promoted to §anomaly? |
|---|---|:---:|
| A1 Two parallel systems | ✅ center vs K-12 model (A10 above) | ✅ |
| A2 Entity ↔ Table drift NẶNG | ✅ ClassSession, Course, Classes — 3 drift cases | ✅ |
| A3 Entity ↔ Table drift cột thiếu | ✅ (gom với A2) | ✅ |
| A4 Enum↔CHECK drift | partial — cluster có CHECK cho `status` enum nhưng KHÔNG có drift documented | (not applicable hoặc verify thêm) |
| A5 Money type inconsistency | N/A — cluster KHÔNG có cột tiền | ⬛ N/A |
| A6 Actor BIGINT V73 miss | ✅ courses.teacher_id, subject_sections.teacher_id, homeroom_teacher_id | ✅ |
| A7 version DEFAULT 0 missing | ✅ inline acknowledge V62/V63 backfill | ✅ |
| A8 TIMESTAMP vs TIMESTAMPTZ | ✅ 12 bảng split half-half | ✅ |
| A9 RLS coverage gap | ✅ 3 bảng thiếu instance_id (class_schedules/class_sessions/course_prerequisites) | ✅ |
| A10 Idempotency neighbor | ⬛ N/A | ⬛ N/A |

**Coverage**: 8/10 baseline anomaly classes APPLY cho cluster 01 — tất cả đã được mention inline nhưng KHÔNG gom thành section. Reader phải open file + scan ~514 dòng để biết.

### Recommendation cho cluster 01

🟠 **NEEDS FIX (P1)** — file gap GAP-NNN-rewrite-01-academic-structure-anomaly-section. Append `## Ghi chú schema (anomalies)` với 8 anomaly classes structured (A1-A10 model). Effort ~1.5h read + write. NO migration / NO code change (chỉ docs).

---

## Cluster 02 — People & Enrollment

### Structural completeness checklist

| # | Check | Result |
|---|---|:---:|
| 1 | `## ... [Aa]nomalies` section header structured | ❌ **MISSING** |
| 2 | Mỗi bảng có per-table column detail | ✅ tất cả 8 bảng đầy đủ |
| 3 | ERD Mermaid `erDiagram` có / đủ entity | ✅ |
| 4 | "Nguồn đọc" cite migrations | ✅ inline trong mỗi bảng |
| 5 | TL;DR front-matter | ✅ structured table |

### Anomalies found inline — should be promoted to `## Ghi chú schema` section

Đọc 369 dòng, identify 7 anomaly classes scattered inline:

#### A1 — `teacher_courses` thiếu `instance_id` + KHÔNG kế thừa BaseEntity (RLS skip)

**Evidence**: cluster doc line 328 ghi rõ "KHÔNG tenant-scoped (không có `instance_id`)" — cô lập tenant qua FK gián tiếp giống `student_badges` cluster 06 anomaly A. Risk class identical A9 baseline.

**Fix**: Standalone `## A1 — teacher_courses thiếu instance_id → ngoài RLS V58`.

#### A2 — `teachers` legacy V1 columns vs entity V27 columns (drift)

**Evidence**: cluster doc line 163 — DB giữ cả `phone`/`department`/`qualifications` (V1) lẫn `phone_number`/`qualification` (V27). Entity chỉ map subset. Pattern giống A2/A3 baseline.

**Fix**: Standalone `## A2 — Entity Teacher ↔ bảng teachers drift V1 legacy vs V27 columns`.

#### A3 — `enrollments` UNIQUE constraint mismatch (DB 2-cột vs entity 4-cột annotation)

**Evidence**: cluster doc line 295 ghi rõ "DB constraint thực tế là `uk_enrollments (class_id, student_id)` (V1). Entity khai báo annotation `(student_id, class_id, instance_id, deleted)` 4-cột — không có migration tạo".

Pattern unique drift là 1 sub-class A4 baseline (Enum ↔ CHECK), expanded thành "Index/UNIQUE drift entity ↔ DB".

**Fix**: Standalone `## A3 — Enrollments UNIQUE constraint drift entity vs DB`.

#### A4 — Enrollments có 2 cột thời gian song song (`enrolled_at` V1 vs `enrollment_date` V27)

**Evidence**: cluster doc line 304 inline note. Cùng class A2 baseline (legacy + new column tồn tại đồng thời).

**Fix**: gom chung A2 (Entity drift cluster summary).

#### A5 — `classes.teacher_id` V73 đổi sang UUID + DROP FK tới teachers(id)

**Evidence**: cluster doc line 167 inline note — V73 drop FK `classes.teacher_id → teachers(id)`. Đây là intentional change nhưng risk cho code legacy giả định FK domain.

**Fix**: Standalone `## A5 — classes.teacher_id V73 UUID conversion + FK drop` (link cross-cluster với cluster 01 + cluster 06 A6 baseline).

#### A6 — Actor BIGINT bị V73 sweep BỎ SÓT (`teacher_courses.assigned_by`)

**Evidence**: cluster doc line 328 inline note "`assigned_by` vẫn là BIGINT (không nằm trong sweep V73 vì không tên `created_by`/`updated_by`)". Identical pattern A6 baseline.

**Fix**: Standalone `## A6 — Actor BIGINT bị V73 sweep BỎ SÓT` (cùng class với cluster 01 A5 + cluster 04 A6 + cluster 06 B).

#### A7 — TIMESTAMP vs TIMESTAMPTZ không nhất quán

**Evidence**: cluster doc inline — `students`, `teachers`, `enrollments` dùng TIMESTAMPTZ; `parents`, `parent_student_links`, `parent_invitations`, `student_bulk_import_jobs` (V42/V41) dùng TIMESTAMP naive. Identical A8 baseline pattern.

**Fix**: Standalone `## A7 — TIMESTAMP vs TIMESTAMPTZ mismatch` (3 bảng TZ vs 4 bảng naive).

### Gap delta vs 04-finance.md baseline

| Baseline A class | Cluster 02 inline equivalent | Promote? |
|---|---|:---:|
| A1 Two parallel systems | ⬛ N/A | ⬛ |
| A2 Entity ↔ Table drift | ✅ Teacher legacy V1 vs V27 + Enrollment 2 timestamps | ✅ |
| A3 Entity ↔ Table cột thiếu | ✅ enrollments unique constraint drift | ✅ |
| A4 Enum↔CHECK drift | partial — có CHECK cho status, không drift docs | (verify) |
| A5 Money type | ✅ enrollments tuition_amount/final_amount DECIMAL(10,2) — `decimal(12,2)` `invoices` khác — possible drift across clusters | (cross-cluster anomaly) |
| A6 Actor BIGINT V73 miss | ✅ `teacher_courses.assigned_by` BIGINT | ✅ |
| A7 version DEFAULT 0 | ✅ acknowledge inline V62/V63 | ✅ |
| A8 TIMESTAMP vs TIMESTAMPTZ | ✅ 3 vs 4 bảng | ✅ |
| A9 RLS coverage gap | ✅ `teacher_courses` skip RLS | ✅ |
| A10 Idempotency neighbor | ⬛ N/A | ⬛ |

**Coverage**: 7/10 baseline anomaly classes APPLY cho cluster 02 — tất cả mention inline nhưng KHÔNG gom thành section.

### Recommendation cho cluster 02

🟠 **NEEDS FIX (P1)** — file gap GAP-NNN-rewrite-02-people-enrollment-anomaly-section. Append `## Ghi chú schema (anomalies)` với 7 anomaly classes A1-A7 structured. Effort ~1h (cluster 02 ngắn hơn 01). NO migration / NO code change.

---

## Cluster 06 — Gamification

### Structural completeness checklist

| # | Check | Result |
|---|---|:---:|
| 1 | `## Ghi chú schema (anomalies)` section header structured | ✅ **PRESENT** với 5 anomaly A-E |
| 2 | Mỗi bảng có per-table column detail | ✅ tất cả 6 bảng đầy đủ |
| 3 | ERD Mermaid `erDiagram` có / đủ entity | ✅ |
| 4 | "Nguồn đọc" cite migrations | ✅ TL;DR + line cite V1+V26+V58/59+V62/63+V73 |
| 5 | TL;DR front-matter | ✅ structured table |

### Anomaly depth verification (vs baseline 04-finance.md)

Cluster 06 có section `## Ghi chú schema (anomalies)` với 5 anomaly classes:

| ID | Cluster 06 anomaly | Equivalent baseline A class |
|---|---|---|
| A | `student_badges` thiếu `instance_id` → RLS skip | A9 baseline (RLS coverage gap) ✅ |
| B | Actor kiểu bất nhất (`created_by/updated_by` UUID vs `approved_by` BIGINT) | A6 baseline (Actor BIGINT V73 miss) ✅ |
| C | Entity↔DB drift — chỉ 1/6 bảng có entity JPA | A2/A3 baseline (Entity drift) ✅ |
| D | Point ledger cumulative không snapshot | (NEW class — performance anomaly không có trong baseline) |
| E | Không soft-delete toàn cluster | (NEW class — design anomaly) |

### Gap delta vs baseline

| Baseline A class | Cluster 06 coverage | Verdict |
|---|---|:---:|
| A1 Two parallel systems | ⬛ N/A | ⬛ |
| A2 Entity ↔ Table drift | ✅ Cluster 06 C | ✅ |
| A3 Entity ↔ Table cột thiếu | ✅ (gom với C) | ✅ |
| A4 Enum↔CHECK drift | ⚠️ partial — cluster 06 nói `status` workflow `pending/approved/delivered` VARCHAR không enum DB; chưa drift docs | ⚠️ verify |
| A5 Money type | ⬛ N/A — không cột tiền | ⬛ |
| A6 Actor BIGINT V73 miss | ✅ Cluster 06 B | ✅ |
| A7 version DEFAULT 0 batched | ⚠️ implicit trong TL;DR nhưng KHÔNG standalone anomaly | ⚠️ nhỏ |
| A8 TIMESTAMP vs TIMESTAMPTZ | ⚠️ cluster 06 dùng TIMESTAMPTZ thuần (V1) — KHÔNG drift | ⬛ N/A |
| A9 RLS coverage gap | ✅ Cluster 06 A | ✅ |
| A10 Idempotency neighbor | ⬛ N/A | ⬛ |

**Coverage**: 5/8 baseline anomaly classes (excluding N/A) đã documented. 2 NEW classes (D ledger performance, E no-soft-delete design) là bổ sung giá trị — không có trong baseline nhưng đáng note. 1 missing nhỏ (A7 version DEFAULT 0 không standalone).

### Recommendation cho cluster 06

🟢 **PASS** với 1 minor polish: có thể bổ sung anomaly F về workflow `status` VARCHAR không enforce state machine ở DB level (đối chiếu A4 baseline pattern + cluster 04 baseline workflow drift). Effort ~15 min. P3 nice-to-have, không P0/P1.

---

## Aggregate verdict + recommended follow-up

### Summary

| Cluster | Verdict | Effort fix | Class fix |
|---|---|---|---|
| 01-academic-structure | 🟠 NEEDS FIX P1 | ~1.5h | Append `## Ghi chú schema (anomalies)` 10 classes structured |
| 02-people-enrollment | 🟠 NEEDS FIX P1 | ~1h | Append `## Ghi chú schema (anomalies)` 7 classes structured |
| 06-gamification | 🟢 PASS (P3 polish optional) | ~15min | Bổ sung 1 anomaly F state-machine VARCHAR |

**Total missing anomalies**: 8 (cluster 01) + 7 (cluster 02) = **15 anomalies** SHOULD have been documented trong dedicated section nhưng hiện scattered inline (reader phải scan ~880 dòng total để biết).

**Total promoted anomaly classes follow-up** (cross-cluster sweep): ~15-17 (P3 cluster 06 + 15 P1)

### Recommended follow-up PR strategy

**Option A — 1 bundled PR cho cả 01 + 02 (RECOMMENDED)**:
- PR title: `docs(db): KiteClass clusters 01 + 02 append anomaly coverage section`
- Effort ~2.5h
- Cùng class fix, cùng review pattern → 1 PR efficient hơn 2 PR riêng.

**Option B — 2 PR riêng** (nếu effort scope độc lập): không recommend vì duplicate review overhead.

**Cluster 06 polish**: defer hoặc skip (nice-to-have, không phải gap blocking).

### Out-of-scope nhưng nên follow-up cùng class

User task explicit chỉ audit 3 cluster (01/02/06). Tuy nhiên 2 cluster còn lại cũng merge trước session:
- **03-attendance-grading** (29 mentions, 1 section) — verify depth
- **05-rbac** (49 mentions, 1 section) — verify depth

Recommended Wave 14 follow-up audit cùng class cho 03 + 05 (1 audit report 2 cluster, ~1h effort).

---

## Methodology notes (cho future audit cùng class)

### Tools used
- `wc -l` cho line count baseline
- `Read` full file cho 3 target cluster (514 + 369 + 371 dòng = ~1250 dòng)
- `Read` full file 04-finance.md baseline (417 dòng)
- `grep` V58 RLS table list để verify A1/A9 anomaly empirically

### Audit criteria
- **Structural completeness**: 5 checks (section header / per-table column / ERD / migrations cite / TL;DR)
- **Anomaly depth**: compare với 04-finance.md baseline 10 classes A1-A10 + identify novel anomalies trong cluster scope
- **Gap delta table**: per cluster cho transparency

### Boundary calls
- Anomaly counting: vài anomaly thuộc multiple classes (vd `class_sessions` drift là cả Entity↔DB drift A2 + RLS skip A9). Đếm primary class.
- 06 P3 polish optional vì rule không mandate ≥10 anomaly — chỉ require dedicated section + sufficient depth (5 anomaly + 2 novel classes OK).

---

## Liên kết

- Baseline: [`documents/02-architecture/database/kiteclass/04-finance.md`](../../02-architecture/database/kiteclass/04-finance.md)
- Cluster 01: [`documents/02-architecture/database/kiteclass/01-academic-structure.md`](../../02-architecture/database/kiteclass/01-academic-structure.md)
- Cluster 02: [`documents/02-architecture/database/kiteclass/02-people-enrollment.md`](../../02-architecture/database/kiteclass/02-people-enrollment.md)
- Cluster 06: [`documents/02-architecture/database/kiteclass/06-gamification.md`](../../02-architecture/database/kiteclass/06-gamification.md)
- KH baseline reference: [`documents/02-architecture/database/kitehub/01-auth-user-instance.md`](../../02-architecture/database/kitehub/01-auth-user-instance.md) (12 anomalies, 617 dòng)
