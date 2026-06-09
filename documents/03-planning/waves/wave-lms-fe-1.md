---
title: Wave LMS-FE 1 — KiteClass LMS content-delivery FE (lean MVP)
status: draft
created: 2026-06-10
updated: 2026-06-09
tag_primary: lms-fe
tags_secondary: [kiteclass, content-delivery, beta-prep]
counter: 1
gaps: [GAP-1113, GAP-1115, GAP-1116, GAP-1117, GAP-1118]
audience: dev
---

# Wave LMS-FE 1 — KC LMS content-delivery (lean MVP)

> **Trạng thái:** DRAFT thảo luận — chờ user review/chốt scope. KHÔNG auto-merge.
> **Tiền đề:** chạy SAU `wave-rbac-shell-1` (role-shell foundation) + SAU F1 BE-fix (GAP-1115..1118 PR #2284) merge.

## TL;DR
FE LMS surfaces cắm lên role-shell (Wave RBAC-Shell 1). MVP **LEAN** theo benchmark (trung tâm dạy thêm VN = operations-first, không content-LMS-first). Đóng **GAP-1113** (FE LMS headless). BE LMS đã có 4 bảng + RLS + service nhưng cần Phase0-BE gap-fill (course list/publish/upload) + F1 security fix.

## Bối cảnh điều tra (đã làm session 2026-06-10, design-first)

### Trạng thái BE LMS hiện tại (đã verify code)
- **4 bảng LMS shipped:** `course_modules` / `lessons` / `learning_resources` / `lesson_progress` (V14 migration KC-core, V79 entity sync). Doc cluster: `documents/02-architecture/database/kiteclass/09-lms.md` (PR #2280).
- **RLS present 2 lớp** (đính chính — KHÔNG thiếu như giả định ban đầu): V79 dòng 577-613 `DO $$` áp ENABLE+FORCE+policy `tenant_isolation` hardened V59 cho cả 4 bảng + Hibernate `@Filter`. GAP-1112 reframe = test-guard (`LmsRlsIsolationIT`, PR #2281), KHÔNG phải security gap.
- **Service + controller có sẵn:** `LmsController` / `LessonProgressController` + `LmsServiceImpl` / `LessonProgressServiceImpl` + DTO/mapper/repository đầy đủ (module/lesson/resource/progress CRUD).
- **`assignment` BE đã có** (giao/nộp/chấm/trả theo class) — ROI cao vì BE xong, chỉ thiếu FE surface.

### F1 BE security/correctness (PR #2284 — GAP-1115..1118, 46/46 test PASS, PARTIAL chờ walk)
| Gap | Sev | Bug | Fix |
|---|---|---|---|
| **GAP-1115** | 🔴 P0 | Paywall bypass — `getCourseStructureForStudent` rò `content`+`videoUrl` bài trả phí cho student CHƯA enroll | Strip `content`/`videoUrl` cho bài paid khi non-enrolled |
| **GAP-1116** | 🟠 P1 | `completeLesson` không enforce enrollment cho bài trả phí (BR-LMS-019 no-op) | Inject Enrollment/Class repo; bài paid → verify enrollment (403) |
| **GAP-1117** | 🟠 P1 | Thiếu `X-User-Id`/`X-Teacher-Id` → 500 thay vì 400 | Global `@ExceptionHandler(MissingRequestHeaderException)` → 400 + `MISSING_HEADER` |
| **GAP-1118** | 🟡 P2 | `getLessonPublic` set TenantContext không restore → rò cross-tenant pooled thread | try/finally capture+restore previousTenant |

Audit report 3-lens: `documents/04-quality/audits/persona-review/2026-06-10-pre-wave-lms-fe-outside-in.md` (trong PR #2284).

### Endpoint còn THIẾU ở BE (Phase0-BE gap-fill — surface trong audit)
- Course **list/search** endpoint (FE catalog cần) — hiện chỉ có get-by-id.
- **Publish/unpublish** course state transition.
- **Reorder** module/lesson atomic (drag-drop FE cần orderNumber update batch).
- Resource **upload** (MinIO/S3) — hiện chỉ có metadata CRUD, chưa có upload pipeline.
- Completion-roster (teacher xem ai hoàn thành) — aggregate query.

## §1. Brainstorm

### Outside-in (ĐÃ audit 3-lens)
`documents/04-quality/audits/persona-review/2026-06-10-pre-wave-lms-fe-outside-in.md` (persona + benchmark + failure-matrix). Benchmark verdict: **trung tâm dạy thêm VN = operations-first** (điểm danh + học phí + báo phụ huynh quan trọng hơn content-LMS đầy đủ). → MVP LEAN, defer/cut nhiều content-LMS feature.

### Pre-walk persona simulation (BẮT BUỘC trước G2 walk per `pre-walk-persona-simulation-mandate.md`)
LMS = user-facing flow (teacher authoring / student player / guest paywall) → §2 trigger fires. PHẢI spawn Opus pre-walk agent return ≥5 failure modes TRƯỚC mỗi bucket walk. Bucket 0 (pre-walk) thêm vào §3.

### Deps CỨNG (phải xong trước)
1. **Wave RBAC-Shell 1** (teacher-shell + student-shell) — surfaces cắm vào shell đúng role.
2. **F1 BE-fix GAP-1115..1118** merged (PR #2284) — paywall/enrollment/header/tenant-context.
3. **Phase0-BE gap-fill** (bucket dưới) — course list/publish/reorder/upload/roster.
4. Student surfaces chờ thêm **KC-9** student-auth (memory `project_parent_student_portal_phase2_gated`).

## §2. Buckets (disjoint, worktree-parallel, Opus per `agent-model-opus-default.md`)

| Bucket | Scope | Layer | Dep | Walk class |
|---|---|---|---|---|
| **Phase0-BE** — gap-fill | course **list/search** endpoint + **publish/unpublish** state + **reorder** atomic (batch orderNumber) + resource **upload** (MinIO/S3 pipeline) + completion-roster aggregate (teacher) + 3-layer doc `lms/api-contract.md` update | BE (kc-core) | F1 merged | n/a (API contract per `contract-first-for-cross-layer.md`) |
| **A** — Teacher authoring UI | tab "Nội dung" trong course-detail: CRUD module/lesson/resource (drag-drop reorder, auto orderNumber ẩn); cắm **teacher-shell** | FE | Phase0-BE + RBAC-Shell teacher | user-facing flow ✅ pre-walk required |
| **B** — Guest catalog + paywall UI | public catalog (list endpoint) + trial preview + paywall lock bài paid + CTA đăng ký (KHÔNG raw 403) | FE | Phase0-BE | user-facing flow ✅ pre-walk required |
| **C** — Student lesson player + progress | lesson view (markdown + **video embed** YouTube/Vimeo, không tự host) + mark-complete + progress% + gamification toast; cắm **student-shell** | FE | RBAC-Shell student (**KC-9 gated**) | user-facing flow ✅ pre-walk required |
| **D** — Surface `assignment` (BE đã có) | giao/nộp/chấm/trả theo class — ROI cao vì BE xong, chỉ thiếu FE | FE | RBAC-Shell teacher+student | user-facing flow ✅ pre-walk required |

**Quy ước cross-layer (per `contract-first-for-cross-layer.md`):** Phase0-BE = Bucket 0 Foundation, ship api-contract + endpoint FIRST → FE bucket reference contract, KHÔNG tự design endpoint shape.

## §3. Scope-completeness (per `wave-closure-scope-completeness.md` — fill at closure)
| # | Plan §2 item | Verdict | Follow-up |
|---|---|---|---|
| _(điền tại closure)_ | | | |

## §4. Defer / Cut (per benchmark — operations-first)
- **Defer (Wave LMS-FE 2+):** quiz auto-grade có-giờ (kéo lên **P0 nếu beta cohort = trung tâm luyện thi THPT**), học bạ/analytics LMS, **Zalo-notify** (tách track operations Wave Zalo).
- **Cut khỏi MVP:** SCORM / certificate / DRM-video / discussion forum / self-checkout marketplace / live-class video conferencing.

## §5. Sequencing (2 Increment)
```
Phase0-BE (Bucket 0 Foundation, merge first)
   ↓
Increment A (teacher + guest — KHÔNG cần student-auth):
   Bucket A (teacher authoring) ∥ Bucket B (guest paywall) ∥ Bucket D (assignment)
   ↓
Increment B (student — chờ KC-9 student-auth unblock):
   Bucket C (student player + progress)
```
Increment A shippable ngay sau RBAC-Shell teacher-shell + Phase0-BE. Increment B gated KC-9.

## §6. Acceptance
Theo GAP-1113 AC: teacher tạo nội dung + guest xem trial/paywall (KHÔNG rò content bài paid) + student học+progress (Increment B) + assignment surfaced. Mỗi feature:
- Pre-walk persona simulation (§1) TRƯỚC walk.
- Runtime-walk per `feature-ship-runtime-walk-mandate.md` §3 (Walk evidence section) trước DONE.
- G1 browser-walk per `g1-browser-walk-before-flip.md` cho FE flow.

## §7. Risk
- **Paywall correctness (P0):** F1 GAP-1115 fix PARTIAL chờ walk — KHÔNG flip LMS-FE DONE trước khi paywall walk live-verify (non-enrolled student → content=null; enrolled → full).
- **KC-9 student-auth blocker:** Increment B treo cho tới khi student login shipped. Scaffold student-shell only (per RBAC-Shell Bucket B).
- **Upload pipeline (Phase0-BE):** MinIO/S3 resource upload = file-upload flow → verify per `pre-handoff-self-test-completeness.md` §2.5 (MIME/size/scan/storage/retrieval).

## §8. Log
- **2026-06-09:** Draft enrich từ session 2026-06-10 investigation (5 PR open #2280/#2281/#2284 + GAP-1119) — thêm F1 BE-fix context, Phase0-BE endpoint gap-fill, cross-layer contract-first, pre-walk mandate, 2-Increment sequencing, risk. EXTEND draft gốc (PR #2283 branch `feature/gap-1119-kc-role-shell`).
