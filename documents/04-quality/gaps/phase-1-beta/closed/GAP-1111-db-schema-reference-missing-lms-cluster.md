# GAP-1111: DB schema-reference KiteClass thiếu cluster LMS (course_modules / lessons / learning_resources / lesson_progress)

**Status:** 🟢 DONE
**Priority:** 🟠 P1
**Domain:** Architecture/Docs
**Found:** 2026-06-10 (audit cluster doc DB schema-reference KiteClass — phát hiện 4 bảng LMS chưa được document)
**Affects:** `documents/02-architecture/database/kiteclass/` (bộ schema-reference cluster) — 4 bảng `course_modules` / `lessons` / `learning_resources` / `lesson_progress`

## Problem

Module LMS ở backend `kiteclass-core` đã hoàn chỉnh: 4 entity (`module/lms/entity/CourseModule` / `Lesson` / `LearningResource` / `LessonProgress`), 2 controller (`LmsController` + `LessonProgressController`, tổng 15 endpoint), service layer, business rules `documents/01-business/kiteclass/lms/rules.md` (BR-LMS-001..020). 4 bảng tương ứng tạo ở migration `V79__entity_schema_sync.sql` (dòng ~445-527).

NHƯNG bộ DB schema-reference docs (`documents/02-architecture/database/kiteclass/`) chỉ có **8 cluster** (01-academic-structure … 08-branding-marketing) và **KHÔNG có cluster nào** cho 4 bảng LMS này. Dev đọc schema-reference để viết query/migration sẽ không thấy phân cấp Course > Module > Lesson, không biết các anomaly (đặc biệt lỗ RLS DB-level chưa bật cho cụm V79).

Đây là **doc gap** (drift giữa code reality và schema-reference docs), không phải code gap.

## Proposed Fix

Tạo cluster doc thứ 9 `09-lms.md` theo đúng cấu trúc các cluster hiện có (TL;DR → ERD Mermaid `erDiagram` → mỗi bảng có Mục đích / Bảng cột đầy đủ / Quan hệ / RLS + ghi chú → Ghi chú schema anomalies → Liên kết). Lấy cột chính xác từ V79 + entity files + cross-ref BR-LMS-001..020. Sync README cluster index (thêm dòng 09-lms + cập nhật số bảng/cluster).

## Acceptance Criteria

- [x] Tạo `documents/02-architecture/database/kiteclass/09-lms.md` đầy đủ 4 bảng theo cấu trúc cluster chuẩn (ERD Mermaid erDiagram + bảng cột từ V79 + anomalies + liên kết)
- [x] Document trung thực trạng thái RLS: `instance_id NOT NULL` + Hibernate `@Filter("tenantFilter")` code-level CÓ, nhưng RLS DB-level THIẾU (cross-ref GAP-1112 + anomaly A)
- [x] README cluster index synced: thêm dòng `[09-lms.md] | LMS / Nội dung học tập`; sửa "~65 bảng, 8 cluster" → "~67 bảng, 9 cluster" (đếm thực tế)

## Related

- Discovered in: PR `feature/gap-1111-lms-db-doc-cluster` (cluster DB docs audit 2026-06-10)
- Cluster doc tạo: `documents/02-architecture/database/kiteclass/09-lms.md`
- RLS fix đang tiến hành (anomaly A): **GAP-1112** (enable RLS DB-level cho cụm LMS V79)
- FE LMS headless (defer): **GAP-1113**
- Schema source: `kiteclass/kiteclass-core/src/main/resources/db/migration/V79__entity_schema_sync.sql` (dòng ~445-527) + `module/lms/entity/*.java`
- Business rules: `documents/01-business/kiteclass/lms/rules.md` (BR-LMS-001..020)
- Cùng class anomaly RLS: cluster 03 `03-attendance-grading.md` anomaly J
