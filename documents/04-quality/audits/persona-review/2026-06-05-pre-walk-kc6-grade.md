---
title: "Pre-Walk Persona Simulation — KC-6 Grade"
audience: dev
created: 2026-06-05
scope: "Flow Verification Campaign KC-6 — Grade entry (component CRUD) → calculate weighted final → finalize/unfinalize → transcript/report card → gradebook class statistics trong kiteclass-core. Pre-walk static persona simulation per pre-walk-persona-simulation-mandate.md §3 — surface failure mode TRƯỚC khi coordinator walk local Docker stack. Bao gồm schema↔entity drift check (rút kinh nghiệm KC-5 attendance P0)."
---

# Pre-Walk Persona Simulation — KC-6 Grade

**Mục tiêu:** Mô phỏng tâm lý 3 persona (Teacher nhập grade component + calculate + finalize; Teacher/Admin generate transcript + gradebook statistics; GVCN K12 multi-subject) → liệt kê failure mode LIKELY trước khi walk, batch-fix các finding HIGH trước. Trục #1 ưu tiên = schema↔entity drift (rút từ KC-5 attendance HTTP 500 P0 vô hình với IT vì test profile `spring.flyway.enabled=false` + `ddl-auto=create-drop`).

**Phạm vi đã state-check (endpoints EXIST):**
- `GradeController` @ `/api/v1/grades` — POST `/initialize`, GET `/{id}`, GET `/student/{studentId}/class/{classId}`, GET `/student/{studentId}`, GET `/class/{classId}`, POST `/components`, PUT `/components/{id}`, DELETE `/components/{id}`, POST `/{id}/calculate`, POST `/{id}/finalize`, POST `/{id}/unfinalize`, POST `/transcripts/generate`, GET `/transcripts/student/{studentId}/semester/{semester}`, GET `/transcripts/student/{studentId}`, GET `/class/{classId}/statistics`.
- `SubjectGradeController` @ `/api/v1/grades/subjects` — K12 multi-subject (submit-for-review / review / publish / bulk-publish). **K12-only, secondary scope — flag.**
- `ReportController` @ `/api/v1/reports` — `/revenue` + `/attendance`, **KHÔNG có report card/transcript endpoint ở đây** (finance/attendance summary only, `@PreAuthorize("hasRole('ADMIN')")`). "Report card" thực chất = `/transcripts/*` của GradeController.

---

## Phát hiện chính trước khi đọc chi tiết

- **🔴 SCHEMA↔ENTITY DRIFT GIỐNG HỆT KC-5 — TÌM THẤY, bảng `grading_scales`.** GAP-875 (DONE) chỉ ADD cột entity (`scale_name`/`letter_grade`/`min_score`/`max_score`/`gpa_value`/`is_default`/`is_passing`) **mà KHÔNG xử lý 4 cột legacy `grade`/`min_percentage`/`max_percentage`/`gpa` vẫn `NOT NULL` + KHÔNG có default + entity `GradingScale.java` KHÔNG map**. Bất kỳ entity INSERT nào vào `grading_scales` sẽ ném `NOT NULL violation` (Postgres `23502`). GAP-875 scaffold-closed một phần — live schema là ground truth, không phải gap status.
- **🔴 CONTRACT SURPRISE LỚN NHẤT: calculate + finalize SẼ 500/404 vì `grading_scales` table RỖNG (count=0) + KHÔNG có seed.** `mapGradeToLetterAndGpa` (`GradeServiceImpl:579-598`) gọi `findByInstanceIdAndScoreRange` → fallback `findDefaultGradingScales()` (instance_id IS NULL) → cả hai rỗng → ném `EntityNotFoundException("GRADING_SCALE_NOT_FOUND")` → **404**. Không có migration seed default scale, không có code `gradingScaleRepository.save(...)` ở đâu cả. → POST `/{id}/calculate` và POST `/{id}/finalize` (path không-finalScore) sẽ fail ở bước map letter grade. Đây là blocker walk nghiêm trọng nhất.
- **CONTRACT SURPRISE #2: finalize/calculate KHÔNG có `@PreAuthorize`.** Chỉ `initialize` / `getStudentGrade` / `getGradesByClass` / `statistics` có `@authz.hasAccessToClass`. `calculate` / `finalize` / `unfinalize` / `addOrUpdateComponent` / `updateComponent` / `getGradeById` / `getGradesByStudent` KHÔNG có authz annotation — giống lỗ hổng single-mark của KC-5 (GAP-729 miss). finalize chỉ check teacher qua `request.getTeacherId()` body (self-asserted, không qua JWT/header).
- **CONTRACT SURPRISE #3: addOrUpdateComponent + updateComponent KHÔNG check permission.** Chỉ `deleteComponent` (nhận header `X-Teacher-Id`) + `finalize` gọi `validateTeacherPermission`. Thêm/sửa component bất kỳ → không guard → bất kỳ authenticated user nào sửa điểm.
- **Auth của grade dùng `request.getTeacherId()` (body) cho finalize + `X-Teacher-Id` header cho deleteComponent — KHÔNG nhất quán.** initialize/calculate KHÔNG có teacher concept.
- **`getGradeById` / read paths KHÔNG dùng `instanceId` trong query** → cross-tenant dựa 100% vào tenantFilter + RLS (GAP-983 re-walk axis, giống KC-5).

---

## Schema drift check (psql results per grade table)

Chạy live trên `kiteclass_shared` (kite-postgres Up healthy):

### `grades` — ✅ NO DRIFT
- NOT-NULL cols: `id, instance_id, class_id, student_id, created_at, updated_at, deleted, status, pass_threshold`. Tất cả đều được entity `Grade.java` set (status/pass_threshold có `@Builder.Default`; instance_id set qua `grade.setInstanceId(...)`; class/student_id từ request). `grade_type` nullable (mapped, V64/V74).
- Constraints: chỉ `chk_grades_final_score CHECK (final_score IS NULL OR 0<=final_score<=100)` — khớp entity validation (`calculateFinalScore` clamp [0,100]).
- count = **25** (đã có data từ trước — flow từng chạy).
- **Verdict: clean.** V64 align + V62 + V74 đã sync tốt.

### `grade_components` — ✅ NO DRIFT
- NOT-NULL cols: `id, instance_id, grade_id, component_type, component_name, score, max_score, weight_percent, created_at, deleted`. Entity `GradeComponent.java` set tất cả (`grade_id` qua `grade.addComponent` / builder; `instance_id` set ở `initializeGradeComponentsForAssignment:521` NHƯNG **KHÔNG set ở `addOrUpdateComponent`** — xem finding #8).
- Constraint: `uk_grade_components_ref UNIQUE (grade_id, component_type, component_ref_id)` — khớp `@UniqueConstraint`. component_ref_id nullable (PostgreSQL UNIQUE treats NULL distinct → cẩn thận multiple manual components cùng type với ref_id=NULL, finding #7).
- count = **0** — write table chưa chạy thật. Signal yếu là flow component CRUD chưa từng walk trên real schema.
- **Verdict: clean** (column-level), nhưng count=0 + instance_id không set ở manual-add path = risk runtime.

### `transcripts` — ✅ NO DRIFT
- NOT-NULL cols: `id, instance_id, student_id, total_credits, total_courses, passed_courses, failed_courses, created_at, deleted`. Entity `Transcript.java` set tất cả (`total_*`/`passed`/`failed` có `@Builder.Default`; instance_id set `generateTranscript:382`).
- Constraint: `uk_transcripts_student_semester UNIQUE (student_id, semester, academic_year)`. ⚠️ `semester` + `academic_year` nullable → nếu `extractAcademicYear` trả null (semester string không có năm) → UNIQUE (student, NULL, NULL) cho phép nhiều dòng (NULL distinct).
- count = **0** — chưa generate transcript thật bao giờ.
- **Verdict: clean** column-level; count=0 + nullable semester/year = edge risk (finding #11).

### `grading_scales` — 🔴 DRIFT (NOT-NULL legacy cols entity không map)
```
column        | nullable | default
--------------+----------+---------
grade         | NO       | (none)     ← entity KHÔNG map, NOT NULL no default
min_percentage| NO       | (none)     ← entity KHÔNG map, NOT NULL no default
max_percentage| NO       | (none)     ← entity KHÔNG map, NOT NULL no default
gpa           | NO       | (none)     ← entity KHÔNG map, NOT NULL no default
scale_name    | NO       | 'Default'  ← entity map, có default (V79)
letter_grade  | NO       | 'F'        ← entity map, có default (V79)
min_score     | NO       | 0          ← entity map (V79)
max_score     | NO       | 100        ← entity map (V79)
gpa_value     | NO       | 0          ← entity map (V79)
is_default    | NO       | false      ← entity map (V79)
is_passing    | NO       | true       ← entity map (V79)
```
- Constraints: `uk_grading_scales_instance_grade UNIQUE (instance_id, grade)` — UNIQUE trên cột **legacy `grade`** mà entity không biết đến.
- count = **0**; instance_id IS NULL (default scale) count = **0**.
- **Verdict: 🔴 DRIFT + EMPTY.** Hai lỗi cộng dồn: (a) entity INSERT sẽ vi phạm NOT NULL của `grade`/`min_percentage`/`max_percentage`/`gpa` (no default) — GIỐNG HỆT class lỗi `attendance` KC-5; (b) table rỗng + không seed → mọi calculate/finalize 404 GRADING_SCALE_NOT_FOUND. Khác KC-5 ở chỗ: hiện KHÔNG có code path nào `gradingScaleRepository.save()` (chỉ read), nên lỗi (a) chưa nổ runtime — nhưng (b) chắc chắn nổ ở calculate/finalize. Nếu sau này thêm "tenant custom scale" UI → lỗi (a) nổ ngay.

---

## Các failure mode (12) — theo confidence

### 1. [HIGH] `grading_scales` rỗng + không seed → calculate/finalize ném 404 GRADING_SCALE_NOT_FOUND
- **(a) Where:** `GradeServiceImpl.mapGradeToLetterAndGpa:579-598` → `gradingScaleRepository.findByInstanceIdAndScoreRange(instanceId, finalScore)` (`GradingScaleRepository:48-53`) `.orElseGet` → `findDefaultGradingScales()` (`:98-100`, instance_id IS NULL) → `.orElseThrow(EntityNotFoundException("GRADING_SCALE_NOT_FOUND"))`. Live `grading_scales` count=0; instance_id IS NULL count=0. KHÔNG có migration `INSERT INTO grading_scales`, KHÔNG có `gradingScaleRepository.save(...)` ở bất kỳ đâu, KHÔNG có provisioning listener tạo scale.
- **(b) Symptom:** POST `/{id}/calculate` → `calculateFinalScore` → `mapGradeToLetterAndGpa` → throw → handler map `EntityNotFoundException` → **404 GRADING_SCALE_NOT_FOUND**. POST `/{id}/finalize` (khi finalScore==null, path `:278-281`) cũng gọi `mapGradeToLetterAndGpa` → 404. Persona Teacher "tính điểm xong nhấn Tính tổng kết → lỗi không tìm thấy thang điểm". **Đây là blocker chính của walk** — calculate + finalize happy-path không thể PASS nếu chưa seed scale.
- **(c) Pre-walk check:** `docker exec kite-postgres psql -U kitehub -d kiteclass_shared -tA -c "SELECT count(*) FROM grading_scales WHERE deleted=false;"` → 0 (confirmed). Pre-walk PHẢI seed default grading_scales (9 dòng A+..F instance_id NULL) HOẶC fix code để có embedded default fallback. Walk: POST calculate trên grade có components → kỳ vọng 404 (bug) cho tới khi seed.

### 2. [HIGH] `grading_scales` schema↔entity drift — 4 cột legacy NOT NULL no-default entity không map (KC-5 class lỗi)
- **(a) Where:** Live `grading_scales` có `grade`, `min_percentage`, `max_percentage`, `gpa` = `NOT NULL` + KHÔNG default. Entity `GradingScale.java:51-103` chỉ map `scale_name/letter_grade/min_score/max_score/gpa_value/is_default/is_passing` + BaseEntity. V79 (GAP-875 fix) chỉ `ADD COLUMN IF NOT EXISTS` cột entity, KHÔNG `ALTER ... DROP`/`SET DEFAULT`/`ALTER ... DROP NOT NULL` cho 4 cột legacy. `uk_grading_scales_instance_grade` UNIQUE trên cột legacy `grade`.
- **(b) Symptom:** Bất kỳ `gradingScaleRepository.save(new GradingScale(...))` → Hibernate INSERT chỉ điền cột entity → Postgres `ERROR: null value in column "grade" violates not-null constraint` (SQLState 23502) → **HTTP 500**. Hiện CHƯA nổ vì 0 code path save scale, NHƯNG: (1) nếu walk/seed dùng entity-save thì nổ; (2) Phase 1.5 "tenant custom grading scale" UI sẽ nổ. Giống hệt `attendance` KC-5 (NOT NULL student_id entity bỏ map). GAP-875 DONE là scaffold-close — chỉ thêm cột mới, không reconcile legacy.
- **(c) Pre-walk check:** `docker exec kite-postgres psql -U kitehub -d kiteclass_shared -tA -c "SELECT column_name,is_nullable,column_default FROM information_schema.columns WHERE table_name='grading_scales' AND is_nullable='NO' AND column_default IS NULL;"` → confirm `grade/min_percentage/max_percentage/gpa` (no default). Pre-walk fix: migration V## `ALTER TABLE grading_scales ALTER COLUMN grade DROP NOT NULL` (+ 3 cột) HOẶC seed default qua raw SQL điền cả cột legacy. Re-open GAP-875 (verify-then-fix, không trust DONE). Walk seed nên dùng raw SQL INSERT điền đủ legacy + entity cols để tránh 23502.

### 3. [HIGH] calculate/finalize/component-CRUD/read KHÔNG có `@PreAuthorize` — bất kỳ tenant user sửa/finalize điểm bất kỳ
- **(a) Where:** `GradeController` — chỉ `initialize:54`, `getStudentGrade:77`, `getGradesByClass:104`, `statistics:219` có `@PreAuthorize("@authz.hasAccessToClass(#classId)")`. KHÔNG có annotation: `getGradeById:67`, `getGradesByStudent:89`, `addOrUpdateComponent:115`, `updateComponent:127`, `calculateFinalScore:152`, `finalizeGrade:161`, `unfinalizeGrade:173`, `generateTranscript:182`, `getTranscript*`. Service `addOrUpdateComponent:138` + `updateComponent:181` + `calculateFinalScore:240` + `unfinalizeGrade:301` KHÔNG gọi `validateTeacherPermission`. Chỉ `deleteComponent:224` + `finalizeGrade:265` có check.
- **(b) Symptom:** Authenticated tenant user (vai trò bất kỳ, kể cả không phải teacher lớp) POST `/components` / `/{id}/calculate` / `/{id}/unfinalize` với gradeId hợp lệ → 200, sửa/tính/mở khoá điểm thành công. Vi phạm BR-ASG-005 (chỉ class teacher) + OWASP A01. unfinalize đặc biệt nguy hiểm: bất kỳ user mở khoá grade đã finalize → sửa → re-finalize. Cross-flow sweep: GAP-729/GAP-983 đã guard một số endpoint nhưng MISS grade write/calculate paths (giống single-mark KC-5).
- **(c) Pre-walk check:** `grep -n "PreAuthorize\|validateTeacherPermission\|hasAccessToClass" GradeController.java GradeServiceImpl.java` → confirm calculate/unfinalize/addComponent/updateComponent có 0 guard. Walk sad-path: login user KHÔNG phải teacher → POST `/{id}/unfinalize` → kỳ vọng 403 nhưng thực tế 200 (bug).

### 4. [HIGH] finalize teacher permission dựa `request.getTeacherId()` (body, self-asserted) — không qua JWT/header
- **(a) Where:** `FinalizeGradeRequest.teacherId` (`:22-23`, `@NotNull`) — client tự gửi. `finalizeGrade:265` → `validateTeacherPermission(grade, request.getTeacherId())` → `teacherClassRepository.findByTeacherIdAndClassId(teacherId, classId)` → check `role == MAIN_TEACHER`. Caller tự khai teacherId. deleteComponent dùng header `X-Teacher-Id` (cũng self-asserted). Không lấy từ JWT/SecurityContext.
- **(b) Symptom:** Attacker biết teacherId của MAIN_TEACHER lớp → gửi `{"teacherId": <main_teacher_id>}` → finalize thành công dù không phải người đó. IDOR / privilege spoofing. Đồng thời ADMIN không có TeacherClass row → `findByTeacherIdAndClassId` empty → `PermissionDeniedException("TEACHER_NOT_IN_CLASS")` → 403 (ADMIN bị chặn finalize, giống KC-5 finding #10). Persona Admin "tôi finalize hộ giáo viên nghỉ → 403".
- **(c) Pre-walk check:** Confirmed `FinalizeGradeRequest` có `teacherId` body field; `validateTeacherPermission` chỉ MAIN_TEACHER via TeacherClass. Walk: finalize với teacherId của teacher khác (spoof) → kỳ vọng 403 nhưng thực tế 200 (bug). ADMIN finalize → kỳ vọng 200 (BR-GRD-007) nhưng thực tế 403.

### 5. [MEDIUM] calculate trước khi đủ component / weights chưa = 100% → final_score sai âm thầm, KHÔNG validate
- **(a) Where:** `calculateFinalScore:240-256` gọi `grade.calculateFinalScore()` (`Grade.java:216`) = `Σ weighted_score` clamp [0,100] — KHÔNG check `isWeightsSumValid()`. Chỉ `finalize` (`:273`) mới check weights==100. `addOrUpdateComponent` không chặn calculate khi mới có 1 component. BR-GRD-002 (weights sum 100%) chỉ enforce ở finalize, không ở calculate.
- **(b) Symptom:** Teacher thêm 1 component (midterm 30%), nhấn Tính → final_score = 30% của midterm (ví dụ 85→25.5) → hiển thị "final_score=25.5, letter F" gây hiểu nhầm "học sinh trượt" dù mới nhập 1 phần. Không lỗi HTTP nhưng business-misleading. 0 components → `calculateFinalScore` reduce ZERO → final_score=0 → letter F. div-by-zero an toàn (`GradeComponent.calculateWeightedScore:158` guard maxScore==0 → ZERO).
- **(c) Pre-walk check:** Confirmed calculate không check weights. Walk: initialize grade → add 1 component weight 30 → POST calculate → kỳ vọng final_score partial (gây hiểu nhầm) hoặc cảnh báo; thực tế tính luôn không cảnh báo. 0 components → calculate → final_score=0 (cần grading_scale cho 0 → finding #1 sẽ 404 trước).

### 6. [MEDIUM] addOrUpdateComponent KHÔNG set `instance_id` → INSERT có thể null instance hoặc rớt tenantFilter
- **(a) Where:** `addOrUpdateComponent:138-177` — nhánh create (`:164-168`) `gradeMapper.toEntity(request)` + `grade.addComponent(component)` rồi `gradeComponentRepository.save`. Mapper `toEntity` (`GradeMapper:38-40`) ignore grade + expression weightedScore — KHÔNG set instanceId. Service KHÔNG `component.setInstanceId(...)`. Tương phản: `initializeGradeComponentsForAssignment:521` CÓ `component.setInstanceId(TenantContext.getCurrentTenant())`. Live `grade_components.instance_id` = NOT NULL.
- **(b) Symptom:** POST `/components` (manual midterm/final) → entity instance_id = null → Postgres `NOT NULL violation` instance_id → **HTTP 500** (nếu BaseEntity/JPA listener không tự fill). Nếu có @PrePersist fill từ TenantContext thì OK — cần verify BaseEntity listener. Đây là lý do khả dĩ count=0 trên grade_components (manual-add path chưa bao giờ chạy thành công). KC-5-class drift gián tiếp.
- **(c) Pre-walk check:** `grep -n "setInstanceId\|@PrePersist\|instanceId" GradeServiceImpl.java common/entity/BaseEntity.java` → confirm manual-add không set + check BaseEntity có auto-fill không. Walk: POST `/components` tạo midterm → nếu 500 instance_id null → bug; nếu 201 → BaseEntity tự fill (OK).

### 7. [MEDIUM] component_ref_id NULL cho manual components → UNIQUE không chặn duplicate type
- **(a) Where:** `uk_grade_components_ref UNIQUE (grade_id, component_type, component_ref_id)`. Manual components (MIDTERM/FINAL/PARTICIPATION) có `component_ref_id` = null (`CreateGradeComponentRequest` không bắt buộc). Postgres UNIQUE coi NULL distinct → (grade, MIDTERM, NULL) + (grade, MIDTERM, NULL) đều insert được. `addOrUpdateComponent:148` `findByGradeIdAndComponentTypeAndComponentRefId(gradeId, type, null)` — query `= null` (JPQL) trả empty (NULL không = NULL) → luôn create mới.
- **(b) Symptom:** Teacher thêm MIDTERM lần 2 (sửa điểm) → vì query `componentRefId=null` không match → tạo component MIDTERM thứ 2 thay vì update → weights cộng dồn (30+30=60) → `isWeightsSumValid` sai → calculate double-count. Persona "tôi sửa điểm giữa kỳ nhưng nó thêm dòng mới". Data integrity hole cho manual entry.
- **(c) Pre-walk check:** Confirmed `findByGradeIdAndComponentTypeAndComponentRefId` với null ref_id không match (JPQL `= :componentRefId` với null). Walk: POST `/components` MIDTERM 2 lần (ref_id null) → kỳ vọng update (1 dòng) nhưng thực tế 2 dòng (bug); GET grade → totalWeight nhân đôi.

### 8. [MEDIUM] cross-tenant grade read/finalize phụ thuộc HOÀN TOÀN tenantFilter + RLS (GAP-983 re-walk)
- **(a) Where:** Mọi grade lookup KHÔNG dùng instanceId: `findGradeById:551` `findByIdAndDeletedFalse(id)`; `getStudentGrade:115` `findByStudentIdAndClassIdAndDeletedFalse`; component `findByIdAndDeletedFalse:183`; transcript `findByStudentIdAndSemesterAndDeletedFalse`. Chỉ dựa `@Filter tenantFilter` (BaseEntity, Grade extends BaseEntity) + RLS GUC `app.current_tenant_id`. Hai lớp chỉ active khi `TenantContext.isSet()` + transaction.
- **(b) Symptom:** Nếu `TenantContext` chưa set (gateway không truyền tenant cho grade route, hoặc filter ngoài transaction), tenantFilter không enable → grade/transcript của tenant KHÁC bị load → cross-tenant đọc điểm/finalize điểm (200 thay vì 404). Đây CHÍNH là class lỗi GAP-983 vừa fix — cần re-walk verify fix giữ cho grade path (per `pre-handoff-self-test-completeness.md` §3 post-fix re-walk). Lưu ý: read paths KHÔNG có `@PreAuthorize` (finding #3) làm trục này nguy hiểm hơn — không có lớp authz backup.
- **(c) Pre-walk check:** `grep -n "findByIdAndInstanceId\|findByIdAndDeletedFalse\|instanceId" GradeRepository.java` → confirm 0 instanceId param read path. Walk sad-path: login tenant A, GET `/grades/{id}` với id thuộc tenant B → kỳ vọng 404 NOT 200; finalize grade tenant B → kỳ vọng 404.

### 9. [MEDIUM] generateTranscript KHÔNG có authz + dùng giả định "tất cả grade = học kỳ này" + credit hardcode 3.0
- **(a) Where:** `generateTranscript:323-389` — KHÔNG `@PreAuthorize`, KHÔNG teacher check. Lấy `findFinalizedGradesByStudentId(studentId)` (TẤT CẢ finalized grades, không lọc semester) → tính GPA giả định all thuộc semester truyền vào. Credit hardcode `BigDecimal.valueOf(3.0)` mỗi course (`:353`). `extractAcademicYear:607` parse chữ cuối semester string.
- **(b) Symptom:** (1) Bất kỳ user generate transcript cho studentId bất kỳ → leak GPA/grade toàn bộ (PII nhạy cảm per rules.md Compliance PDPL). (2) Transcript "Spring 2026" gộp cả grade Fall 2025 vì không lọc semester → GPA sai. (3) Credit luôn 3.0 → totalCredits = số course × 3 (không phản ánh credit thật). Persona Admin "transcript học kỳ này sao có cả môn kỳ trước". (4) `extractAcademicYear("Học kỳ 1")` → parse "1" → academicYear=1 (sai); semester tiếng Việt không có năm → null.
- **(c) Pre-walk check:** Confirmed transcript dùng all-finalized + credit 3.0 + no authz. Walk: finalize 2 grades khác semester → generateTranscript "Spring 2026" → kỳ vọng chỉ 1 môn nhưng gộp cả 2 (bug); login non-teacher → generate transcript → kỳ vọng 403 nhưng 201 (PII leak).

### 10. [MEDIUM] NO_FINALIZED_GRADES → ValidationException 400; transcript happy-path phụ thuộc finding #1+#4 (calculate/finalize phải chạy được trước)
- **(a) Where:** `generateTranscript:340-342` — nếu `findFinalizedGradesByStudentId` rỗng → `throw new ValidationException("NO_FINALIZED_GRADES_FOR_STUDENT")` → 400. Mà để có finalized grade phải qua finalize (finding #4 ADMIN 403 / spoof) → cần `grading_scale` (finding #1 404). → Transcript walk happy-path bị chặn bởi chuỗi phụ thuộc: seed grading_scale → calculate OK → finalize OK → mới generate transcript được.
- **(b) Symptom:** Walk transcript "no finalized grades" → 400 NO_FINALIZED_GRADES (path đúng — flag để confirm graceful). NHƯNG happy-path transcript KHÔNG thể test nếu chưa fix finding #1 (grading_scale empty). Walker cần thứ tự: (1) seed scale → (2) add components weights=100 → (3) calculate → (4) finalize → (5) generate transcript.
- **(c) Pre-walk check:** Confirmed NO_FINALIZED → 400 graceful. Walk: generate transcript cho student chưa finalize → kỳ vọng 400 NO_FINALIZED_GRADES (đúng). Happy-path: chỉ test được sau khi seed grading_scale.

### 11. [LOW] transcript studentName/studentEmail null + semester nullable UNIQUE edge + i18n placeholder
- **(a) Where:** `GradeMapper.toTranscriptResponse:113-116` `@Mapping ignore` cho `grades`/`studentName`/`studentEmail` → response 3 field này luôn null. `transcripts.semester` + `academic_year` nullable → `uk_transcripts_student_semester` cho phép nhiều (student, NULL, NULL) (NULL distinct). `GradeResponse` không có studentName (chỉ studentId).
- **(b) Symptom:** Report card/transcript UI hiển thị `studentName=null` (không fetch tên thật) → "Bảng điểm của: (trống)" thay vì "Trần Thị Hồng". Tên/notes tiếng Việt có dấu trong `comments` field (`grades.comments TEXT`) round-trip OK nếu UTF-8. Generate transcript 2 lần với semester null → 2 dòng (UNIQUE không chặn). i18n: letter grade A+/F là English token (OK); không có VN label.
- **(c) Pre-walk check:** Confirmed mapper ignore studentName. Walk: generate transcript → response `studentName=null` (UI placeholder bug); comments tiếng Việt round-trip OK; semester="" → academic_year null → có thể tạo trùng.

### 12. [LOW] SubjectGradeController (K12) authz qua header tùy chọn + invalid input graceful — flag K12-only + verify path đúng
- **(a) Where:** `SubjectGradeController` @ `/api/v1/grades/subjects` — submit-for-review / review / publish / bulk-publish dùng `@RequestHeader(value="X-User-Reference-Id", required=false)` → submitterId/reviewerId nullable, KHÔNG `@PreAuthorize`. K12 multi-subject TT22/2021 flow riêng (V55 extend subject_grades). Invalid enum status / missing field → handler chuẩn (400 MALFORMED/VALIDATION); gradeId không tồn tại → 404.
- **(b) Symptom:** (K12-only, secondary) submit-for-review header optional → submitterId=null → audit gap ai submit. KHÔNG có per-resource authz → bất kỳ user publish điểm K12. Flag K12-only — walk chính tập trung core GradeController, K12 secondary. Path đúng cho invalid input: status="FOO" → 400; gradeId=99999 → 404 graceful (verify).
- **(c) Pre-walk check:** Confirmed SubjectGradeController header optional + no authz. Walk K12 (nếu test): submit-for-review không header → submitterId=null (audit gap). Core: invalid status → 400; gradeId không tồn tại → 404.

---

## Recommended pre-walk batch fix (theo confidence × impact)

### 🔴 HIGH — fix/seed TRƯỚC khi walk

1. **Finding #1 (grading_scales rỗng → calculate/finalize 404).** BẮT BUỘC seed 9 dòng default grading_scale (instance_id NULL, A+..F per BR-GRD-005/006) TRƯỚC khi walk — nếu không calculate + finalize happy-path không thể PASS. Seed bằng **raw SQL** điền cả cột legacy (`grade`/`min_percentage`/`max_percentage`/`gpa`) + cột entity để tránh finding #2.
2. **Finding #2 (grading_scales drift NOT NULL legacy).** Re-open GAP-875 (verify-then-fix — DONE là scaffold-close). Migration V## `ALTER COLUMN grade/min_percentage/max_percentage/gpa DROP NOT NULL` HOẶC backfill. Bắt buộc trước khi có bất kỳ entity-save scale path (Phase 1.5 custom scale).
3. **Finding #3 (calculate/finalize/component-CRUD/unfinalize no authz).** Thêm `@PreAuthorize @authz.hasAccessToClass` + service-level teacher check cho calculate/unfinalize/addComponent/updateComponent — đối xứng initialize. OWASP A01, cross-flow sweep miss của GAP-729/983 (giống single-mark KC-5).
4. **Finding #4 (finalize teacherId body self-asserted + ADMIN 403).** Lấy teacherId từ JWT/SecurityContext không từ body; cho ADMIN bypass TeacherClass check (BR-GRD-007).

### 🟡 MEDIUM — spot-check trong walk (fix nếu confirm)

- **#5** calculate không validate weights==100 → final_score sai gây hiểu nhầm.
- **#6** addOrUpdateComponent không set instance_id → khả năng 500 (verify BaseEntity @PrePersist).
- **#7** component_ref_id NULL → manual component duplicate (weights cộng dồn).
- **#8** cross-tenant grade re-walk GAP-983 (bắt buộc sad-path tenant A đọc grade tenant B → 404).
- **#9** generateTranscript no authz (PII leak) + semester không lọc + credit hardcode 3.0.
- **#10** transcript happy-path phụ thuộc chuỗi seed→calculate→finalize.

### 🟢 LOW — defer to walk observation

- **#11** transcript studentName null (UI placeholder) + semester nullable UNIQUE edge.
- **#12** SubjectGradeController K12-only header optional authz (secondary scope).

---

## Endpoint contract surprises (cho coordinator)

1. **calculate + finalize SẼ 404 GRADING_SCALE_NOT_FOUND vì `grading_scales` rỗng + không seed.** PHẢI seed default scale (raw SQL, điền cả cột legacy) TRƯỚC khi walk calculate/finalize. Đây là blocker #1.
2. **`grading_scales` schema drift (4 cột legacy NOT NULL no-default entity không map) = KC-5 class lỗi → YES, tìm thấy.** GAP-875 DONE scaffold-close, chưa reconcile legacy. Entity-save scale sẽ 500 (chưa nổ vì 0 save path hiện tại).
3. **"Report card" KHÔNG ở ReportController.** `ReportController` @ `/api/v1/reports` chỉ `/revenue` + `/attendance` (ADMIN role). Transcript/report card thực = `/api/v1/grades/transcripts/*` của GradeController.
4. **finalize body = `{teacherId (required), comments?}` — teacherId self-asserted, không qua JWT.** deleteComponent dùng header `X-Teacher-Id`. calculate/initialize không có teacher concept. Inconsistent auth.
5. **calculate/unfinalize/addComponent/updateComponent/getGradeById/getGradesByStudent/generateTranscript KHÔNG có `@PreAuthorize`.** Chỉ initialize/getStudentGrade/getGradesByClass/statistics có `@authz.hasAccessToClass`.
6. **Thứ tự walk happy-path bắt buộc:** seed grading_scale → POST `/initialize` (studentId+classId qua @RequestParam) → POST `/components` (weights cộng đủ 100%) → POST `/{id}/calculate` → POST `/{id}/finalize` (body teacherId là MAIN_TEACHER) → POST `/transcripts/generate`. Bỏ qua seed → kẹt ở calculate.
7. **initialize dùng @RequestParam `studentId` + `classId` (query param), KHÔNG body.** finalize/calculate dùng `{id}` path = gradeId (không phải studentId). Walker resolve gradeId từ initialize response trước.
8. **ADMIN bị chặn finalize** (không có TeacherClass row → TEACHER_NOT_IN_CLASS 403) — giống ADMIN PATCH bị chặn ở KC-5.
9. **SubjectGradeController (`/api/v1/grades/subjects`) = K12 multi-subject TT22, secondary scope** — header `X-User-Reference-Id` optional, no per-resource authz.
