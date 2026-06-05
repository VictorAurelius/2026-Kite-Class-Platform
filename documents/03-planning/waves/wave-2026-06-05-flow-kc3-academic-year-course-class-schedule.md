---
title: Wave flow-kc3 — Academic: year → course → class → schedule
status: draft
created: 2026-06-05
updated: 2026-06-05
waves: [flow-kc3]
tag_primary: flow
tags_secondary: [kc3, academic, course, class, schedule, kiteclass, campaign]
counter: 3
gaps: [GAP-909]
campaign: flow-verification-campaign
---

# Wave flow-kc3 — Academic: year → course → class → schedule

**Goal:** Walk end-to-end flow KC-3 (Owner/STAFF setup cấu trúc học thuật: tạo niên khóa → tạo khóa học → tạo lớp → xếp lịch tuần) trên stack production-equivalent, đạt **G1 PASS**. Xác minh chuỗi tạo cấu trúc nghiệp vụ trường hoạt động — nền tảng cho enrollment (KC-4) + attendance (KC-5) + grade (KC-6).

**Trigger:** KC-3 đứng sau KC-1 (tenant configured) + KC-2 (staff/teacher tồn tại để gán lớp). Mọi flow nghiệp vụ sau (enrollment/attendance/grade) giả định course+class+schedule đã tạo. Nếu KC-3 chưa thông → KC-4..9 thiếu context.

## 1. Brainstorm

**Bối cảnh state-check (2026-06-05):** KC-3 là kiteclass-core flow (không platform-side). Phát hiện quan trọng:
- ✅ **academicyear** module tồn tại (`module/academicyear`)
- ✅ **course** module + `CourseController` @ `/api/v1/courses`
- ⚠️ **class + schedule controller KHÔNG tìm thấy** qua quick grep (chỉ `/api/v1/courses`) — phần class→schedule có thể **chưa implement** hoặc tên module khác. **Cần verify-at-session-start** (state-check hardened per `audit-to-gap-pipeline.md` §2.5, no `| head`).
- Docs: `course-class` + `academic-year` + `reschedule` + `student-enrollment` 3-layer tồn tại.

**Pre-walk persona sim per `pre-walk-persona-simulation-mandate.md` BẮT BUỘC** (multi-step setup flow): persona "Owner/STAFF setup học thuật lần đầu" — tạo year → course → class → assign teacher → schedule. Likely failure modes: class/schedule endpoint thiếu (partial impl) / academic-year required cho course / teacher assign cross-tenant / schedule slot conflict.

**Blocker:** GAP-909 🟡 PARTIAL P2 — KC courses entity vs migration drift (`cover_image_url` + `suggested_tuition`). Low priority, không block walk.

## 2. Task Breakdown

| Bucket | Scope | Owner | Walk class | Effort |
|---|---|---|---|---|
| 0 (Pre-walk) | State-check class/schedule impl (verify controller tồn tại không) + spawn Opus pre-walk persona sim per `pre-walk-persona-simulation-mandate.md` §1 | Coordinator | n/a | ~10 phút |
| A | Loop walk: tạo academic-year → course → class → schedule slot (curl + DB verify mỗi bước) | claude (session-pick) | user-facing ✅ pre-walk required | 30-60 phút |
| B | Batch-fix blocker (class/schedule endpoint thiếu → có thể là feature-build, không chỉ walk; academic-year linkage; teacher assign) | claude | n/a | varies (có thể lớn nếu class/schedule chưa impl) |
| C | Re-walk + G1 verdict + G2 handoff MD per `g2-handoff-md-mandate.md` | claude | n/a | 15-30 phút |

## 3. Scope

Scope re-locked sau hardened state-check 2026-06-05 (verdict §4):
- **Walk chain (G1):** `course` → `class` → `schedule` → `sessions` — chuỗi cốt lõi, FULLY IMPL, walkable ngay. Endpoints: `POST /api/v1/courses` → `POST /api/v1/courses/{courseId}/classes` → `POST /api/v1/classes/{classId}/schedule` → `POST /api/v1/classes/{classId}/sessions/generate-from-recurrence` + `GET .../sessions`.
- **DROPPED từ walk:** `academic-year` (niên khóa) — module orphan (service không controller/caller), filed GAP-982 P1, defer build sang wave riêng. `Class` không FK academic_year → walk độc lập OK.
- **BE (kiteclass-core):** `course` module (`CourseController`) + `clazz` module (`ClassController` + `ClassServiceImpl` + `RecurrenceServiceImpl` + `ClassSession` entity).
- **FE (kiteclass-frontend):** course/class pages — verify khi G2 (teacher area).
- **Walk target:** tenant `sky-education` (reuse KC-1/KC-2), Owner `owner@skyedu.vn`; teacher từ KC-2 STAFF (gán lớp).
- **Dependency:** KC-1 (tenant) + KC-2 (staff/teacher) — STAFF tenant resolution đã fix (GAP-981).
- **Pre-walk (Bucket 0):** Opus persona sim agent spawned 2026-06-05 per `pre-walk-persona-simulation-mandate.md` §1.

## Discoveries filed (per discovery-to-gap-inline-filing.md §3.4)

- **GAP-982** (P1 Backend): Academic-year module orphan — service full logic but no controller/caller. Surfaced trong KC-3 hardened state-check.
- **GAP-983** (🔴 P0 Backend): LIVE cross-tenant by-id read leak — course/class/session/teacher. KC-3 walk empirical proof (tenant khanh-phapluat đọc data sky-education qua GET-by-id). OWASP A01 IDOR. Escalates GAP-746 bug class. **Blocks KC-3 THÔNG.**
- **GAP-984** (P2 Backend): Per-tenant DB provisioned nhưng core dùng shared DB — isolation model mismatch.

## 4. State-Check Evidence

Verified 2026-06-05 (coordinator quick state-check — full verify at session start):

| Symbol | Type | Verify command | Verdict |
|---|---|---|---|
| `module/academicyear` | BE module | `find .../module/academicyear` | ✅ exists BUT 🔴 **ORPHAN** — service full CRUD logic, **no controller, no caller** (GAP-982) |
| `CourseController` @ `/api/v1/courses` | BE controller | `grep -rn CourseController` | ✅ exists |
| class controller | BE controller | `grep -rnE "@(Post\|Get)Mapping.*classes"` | ✅ **EXISTS** — module `clazz` (Java reserved word → stub `class` grep false-negative). `ClassController` full CRUD + lifecycle (start/complete/cancel/reschedule) + generate-code |
| schedule | BE controller | `grep -rn "classes/.*schedule"` | ✅ **EXISTS as sub-resource** — `POST /api/v1/classes/{classId}/schedule` (CreateScheduleRequest startTime/endTime) + `GET .../sessions` + `POST .../sessions/generate-from-recurrence` (RecurrenceService + ClassSession) |
| `documents/01-business/kiteclass/course-class/` | 3-layer docs | `ls` | ✅ exists |
| `documents/01-business/kiteclass/academic-year/` | 3-layer docs | `ls` | ✅ exists |

**✅ State-check verdict (2026-06-05, hardened, no `| head`):** Giả định "partial-impl risk" của stub **SAI** — course → class → schedule → sessions **FULLY IMPLEMENTED + walkable ngay**. Stub quick grep false-negative vì module tên `clazz` + schedule là sub-resource dưới class.

**🔴 Phát hiện mới (orphan):** `AcademicYearService` đủ logic (`createAcademicYear`/`setCurrent`/`getCurrent`/`listAll`) NHƯNG `grep -rnl AcademicYearService` chỉ trả repo+self → **không controller, không caller**. Niên khóa không tạo được qua API → filed **GAP-982** (P1 Backend, defer build sang wave riêng). `Class` entity FK chỉ `course_id`+`teacher_id` (no `academic_year_id`) → orphan KHÔNG block walk.

**Re-scope quyết định (user-approved 2026-06-05):** KC-3 walk = chuỗi cốt lõi **course → class → schedule → sessions** (DROP academic-year). Bucket B feature-build **KHÔNG cần** (class/schedule done). Academic-year orphan → GAP-982 defer.

## 5. Verification Gates

| Gate | Owner | Criteria | Status |
|---|---|---|---|
| G1 — agent runtime walk | Claude | (a) ~~academic-year~~ DROPPED (orphan GAP-982); (b) course `POST /api/v1/courses` ✅ 201 (id=10, teacherId=10); (c) class `POST /courses/10/classes` ✅ 201 (id=14, after locationType enum fix IN_PERSON); (d) schedule `POST /classes/14/schedule` ✅ 201 + **27 sessions auto-gen** đúng daysOfWeek MON+WED; (e) GET chain ✅ + DB persist confirmed | ⚠️ **FUNCTIONAL PASS nhưng P0 blocker GAP-983 (cross-tenant leak) → NOT THÔNG** |
| G2 — human local test | User | Login Owner → UI tạo year/course/class/schedule → verify hiển thị | ⬜ |
| G3 — production parity | Claude + User | Production: academic schema migrate sạch RDS + multi-tenant isolation + schedule timezone đúng | ⬜ |

G2 handoff MD recipe per `g2-handoff-md-mandate.md` §3 — ship same PR as G1 PASS flip.

## 6. Agent Spawn Pattern

- KHÔNG parallel-spawn cho walk (state-continuous).
- Pre-walk Opus agent BACKGROUND per `agent-background-spawn-default.md` + `agent-model-opus-default.md`.
- Nếu class/schedule chưa impl → Bucket B = feature-build (spawn agent ship controller+entity+migration TRƯỚC re-walk) — quyết định tại session start sau state-check.

## 7. Closure Protocol

1. Flip `gap-status.csv` rows DONE cho gaps closed wave này + git mv → `phase-1-beta/closed/`.
2. ROADMAP §🎯 Current Status Snapshot — thêm Wave flow-kc3 closure entry.
3. `bash scripts/prune-merged-worktrees.sh --yes` per `post-wave-cleanup.md`.
4. Wave plan frontmatter `status: draft → complete` + Scope-Completeness Reconciliation table per `wave-closure-scope-completeness.md` §3.
5. Campaign §4 row KC-3 flip → `🔄 walk-pass-pending-human` (G1 ✅) + ship G2 handoff MD recipe.
6. wave-history.jsonl append tại full closure.

## 8. Log

- **2026-06-05 (G1 walk — functional PASS + P0 blocker found):** Pre-walk Opus persona sim returned 10 failure modes (artifact `2026-06-05-pre-walk-kc3-course-class-schedule.md`). Walk course→class→schedule→sessions trên kiteclass-core (direct core:8088, owner headers, gateway-equivalent per SecurityConfig permitAll + header-trust): teacher 201 → course 201 → class 201 (1 catalog: locationType OFFLINE→IN_PERSON enum fix) → schedule 201 + 27 sessions đúng MON+WED. **Functional chain PASS.** BUT walk Step 5 isolation test surfaced 🔴 **P0 cross-tenant by-id read leak** (GAP-983): tenant khanh-phapluat đọc sky-education classes/14 + 27 sessions + teachers/10 qua GET-by-id (200). Root cause: Hibernate @Filter ineffective trên findById (LIST safe via Specification) + RLS OFF. Also found GAP-984 (per-tenant DB unused). Architecture: core dùng kiteclass_shared single-DB + instance_id, NOT per-tenant DB. **KC-3 G1 functional PASS nhưng NOT campaign-THÔNG — blocked GAP-983.** Pre-walk failure mode #3 (RLS @PreAuthorize) KHÔNG fire (RLS off + owner=teacher_id); #1/#6 (DTO daysOfWeek/dates) verified correct; #8/#9 (overlap/idempotency) chưa test (defer).
- **2026-06-05 (hardened state-check + re-scope):** Session-start hardened state-check (no `| head` per `audit-to-gap-pipeline.md` §2.5) **đảo ngược** giả định stub: course → class → schedule → sessions **FULLY IMPL + walkable** (stub false-negative: module `clazz` Java-reserved-word + schedule sub-resource). Phát hiện academic-year orphan (service no controller/caller) → filed **GAP-982** P1, defer. Re-scope (user-approved): walk chuỗi cốt lõi DROP academic-year; Bucket B feature-build KHÔNG cần. Pre-walk Opus persona sim agent spawned per `pre-walk-persona-simulation-mandate.md`. §4 evidence + §3 scope cập nhật.
- **2026-06-05 (plan stub ship):** Filed sau KC-2 G1 PASS (PR #2172). KC-3 = next-in-chain per campaign §3 (KC-2 → KC-3 → KC-4..9). State-check: academicyear + course module ✅; **class/schedule controller NOT found quick grep → verify-at-session-start (partial-impl risk)**. GAP-909 P2 blocker (courses entity drift, không block walk). Stub thỏa `check-wave-plan-completeness.sh` (8 sections + 4 frontmatter). Full §3 scope + pre-walk persona sim + class/schedule impl verification happens tại session start. KC-3 có thể là partial feature-build nếu class/schedule chưa impl.
