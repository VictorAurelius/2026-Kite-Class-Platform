---
title: Wave LMS-FE 1 — KiteClass LMS content-delivery FE (lean MVP)
status: draft
created: 2026-06-10
updated: 2026-06-10
tag_primary: lms-fe
gaps: [GAP-1113]
---

# Wave LMS-FE 1 — KC LMS content-delivery (lean MVP)

## TL;DR
FE LMS surfaces cắm lên role-shell (Wave RBAC-Shell 1). MVP **LEAN** per benchmark (trung tâm dạy thêm VN = operations-first, không content-LMS-first). Đóng **GAP-1113**. Chạy SAU Wave RBAC-Shell 1.

## Brainstorm
- **Outside-in:** ĐÃ audit 3-lens (`documents/04-quality/audits/persona-review/2026-06-10-pre-wave-lms-fe-outside-in.md`).
- **Deps CỨNG (phải xong trước):**
  1. **Wave RBAC-Shell 1** (teacher-shell + student-shell) — surfaces cắm vào.
  2. **F1 BE-fix GAP-1115..1118** merged (paywall bypass / enrollment enforce / missing-header 500 / tenant-context leak).
  3. **Phase0-BE gap-fill** (bucket dưới).
  - Student surfaces chờ thêm **KC-9** student-auth.

## Buckets
| Bucket | Scope | Dep |
|---|---|---|
| Phase0-BE — gap-fill | course **list/search** endpoint + **publish/unpublish** + **reorder** atomic + resource **upload** (MinIO/S3) + completion-roster (teacher) | F1 merged |
| A — Teacher authoring UI | tab "Nội dung" trong course-detail: CRUD module/lesson/resource (drag-drop reorder, auto orderNumber ẩn); cắm **teacher-shell** | Phase0-BE + RBAC-Shell teacher |
| B — Guest catalog + paywall UI | public catalog + trial preview + paywall (khóa bài paid + CTA đăng ký, KHÔNG raw 403) | Phase0-BE |
| C — Student lesson player + progress | lesson view (markdown + **video embed** YouTube/Vimeo, không tự host) + mark-complete + progress% + gamification toast; cắm **student-shell** | RBAC-Shell student (**KC-9**) |
| D — Surface `assignment` (BE đã có) | giao/nộp/chấm/trả theo class — ROI cao vì BE xong | RBAC-Shell |

## Defer / Cut (per benchmark)
- **Defer:** quiz auto-grade có-giờ (kéo lên **P0 nếu beta cohort = trung tâm luyện thi THPT**), học bạ/analytics, **Zalo-notify** (tách track operations).
- **Cut khỏi MVP:** SCORM / certificate / DRM-video / discussion forum / self-checkout marketplace.

## Sequencing
Phase0-BE → A/B/D song song (**Increment A** = teacher+guest, KHÔNG cần student-auth) → C (**Increment B** = student, chờ KC-9).

## Acceptance
Theo GAP-1113 AC: teacher tạo nội dung + guest xem trial/paywall + student học+progress (Increment B) + assignment surfaced; mỗi feature runtime-walk per `feature-ship-runtime-walk-mandate` trước DONE.
