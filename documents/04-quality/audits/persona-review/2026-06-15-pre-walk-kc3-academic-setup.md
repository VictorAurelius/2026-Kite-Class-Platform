---
title: "Pre-walk persona simulation — KC-3 Academic setup (niên khóa → khóa học → lớp → lịch tuần → auto-gen sessions → cross-tenant isolation)"
date: 2026-06-15
flow: KC-3
type: pre-walk-persona-simulation
rule: pre-walk-persona-simulation-mandate.md
stack: kiteclass-core + kite-gateway:9000 + kiteclass-frontend:3000
walk_url: "http://sky-education-074901.127.0.0.1.nip.io:3000"
tenant_instance_id: 5b3ef1ae-39e7-4088-888f-941fca67f410
persona: Owner (owner+074901@skyedu.vn)
---

# Pre-walk persona simulation — KC-3 Academic setup

## Mục tiêu

Front-load failure mode TRƯỚC khi human walk flow KC-3 trên stack local production-equivalent. Owner đăng nhập → tạo niên khóa → tạo khóa học → tạo lớp → xếp lịch tuần → auto-gen buổi học → kiểm tra cô lập cross-tenant.

## Bối cảnh code đã đọc (design-first, không đoán)

- **Course:** `CourseController` `POST /api/v1/courses`; `CreateCourseRequest` — `teacherId` **@NotNull bắt buộc**, `code` @Pattern `^[A-Z0-9-]+$` + unique. KHÔNG có field `academic_year_id`.
- **Class:** `ClassController` `POST /api/v1/courses/{courseId}/classes`; `CreateClassRequest` — `startDate`/`endDate` **optional**, `maxStudents` bắt buộc. KHÔNG có `academic_year_id`. `createClass` bind `teacher_id = X-User-Id` (người tạo).
- **Schedule auto-gen:** 2 đường:
  - `POST /api/v1/classes/{classId}/schedule` (`CreateScheduleRequest`) — **ném `CLASS_NO_DATES` nếu class thiếu startDate/endDate**.
  - `POST /api/v1/classes/{classId}/sessions/generate-from-recurrence` (`RecurrenceRuleDto`) — dùng `planStart = max(today, class.startDate)` → KHÔNG cần class dates. FE class-new page dùng đường recurrence này (toggle "Lặp lại theo lịch (tuần)").
  - Cả hai `@PreAuthorize("@authz.hasAccessToClass(#classId)")`.
- **Academic year:** module `academicyear` có entity/service/repository nhưng **ZERO @RestController** → KHÔNG có REST endpoint tạo niên khóa. AcademicYearService chỉ dùng nội bộ (VnHolidayProvider/recurrence). Seed = **0 academic_years**.
- **FE routes:** `courses/new`, `courses/[id]/classes/new` (có recurrence panel), `classes/page.tsx` (`useClasses(selectedCourseId!)` — cần chọn course trước), `classes/[id]` (xem sessions). **KHÔNG có trang owner xếp lịch độc lập** — chỉ `/(teacher)/teacher/schedule`.
- **Seed tenant 5b3ef1ae:** 2 teachers, 0 academic_years, 2 courses, 2 classes, 0 class_schedules, 4 students, 4 enrollments.

## Bảng failure mode

| # | WHERE | SYMPTOM (human thấy khi walk) | PRE-WALK CHECK |
|---|---|---|---|
| **FM-1** 🔴 HIGH | Bước "tạo niên khóa" — module `academicyear` (0 @RestController) + FE không có route niên khóa | KHÔNG có nút/trang nào để tạo niên khóa. Human kẹt ngay bước đầu, đi tìm UI không tồn tại. Decoupled: course/class KHÔNG cần academic_year_id nên các bước sau vẫn chạy. | `grep -rln '@RestController' kiteclass-core/.../academicyear` → 0. `grep -rn 'niên khóa\|academic' FE sidebar` → 0 entry. Xác nhận bước này không executable → bỏ khỏi walk hoặc đánh dấu N/A. |
| **FM-2** 🔴 HIGH | Bước "xếp lịch tuần" — không có trang owner độc lập; chỉ panel recurrence trong `courses/[id]/classes/new` | Human kỳ vọng 1 trang "Lịch tuần" riêng → không tìm thấy. Thực tế phải bật toggle "Lặp lại theo lịch (tuần)" NGAY trong form tạo lớp. Nếu tạo lớp xong mới muốn xếp lịch → không có entry point UI (chỉ teacher schedule). | Mở `:3000/courses/<id>/classes/new` → xác nhận có toggle "Lặp lại theo lịch (tuần)" + RecurrenceForm. Xác nhận KHÔNG có `/(dashboard)` schedule page. Map walk step "xếp lịch" = recurrence toggle, không phải trang riêng. |
| **FM-3** 🔴 HIGH | `CreateCourseRequest.teacherId @NotNull` + CourseForm `useTeachers(status=ACTIVE)` dropdown | Nếu dropdown giảng viên rỗng (gateway strip header / 0 teacher ACTIVE / API teachers lỗi) → owner không chọn được giảng viên → submit 400 "ID giảng viên là bắt buộc". | `curl :9000/api/v1/teachers?status=ACTIVE` với header tenant+auth → ≥1 teacher. DB: 2 teachers seed cho tenant; xác nhận `status=ACTIVE` (không phải PENDING/INACTIVE). |
| **FM-4** 🟠 MED | `classes/page.tsx` — `useClasses(selectedCourseId!)` non-null assertion | Owner vào `/classes` mà chưa chọn course → `selectedCourseId` null → query gọi với `undefined` → list rỗng/lỗi/crash. Human tưởng "không có lớp nào". | Mở `:3000/classes` chưa chọn course → quan sát có guard chọn course trước hay crash. Verify cần chọn course → mới thấy lớp. |
| **FM-5** 🟠 MED | `CreateCourseRequest.code` @Pattern `^[A-Z0-9-]+$` + unique; 2 course seed sẵn | Human gõ mã khóa học chữ thường (vd `anh-van-01`) → 400 "Mã khóa học chỉ được chứa chữ in hoa...". Hoặc trùng mã course cũ → 409/duplicate. | Xem 2 course code seed sẵn (`select code from courses where instance_id=...`). Hướng dẫn human dùng mã IN HOA mới, vd `ANH-VAN-2026`. |
| **FM-6** 🟠 MED | Cross-tenant isolation — `instance_id` auto-set bởi `EntityPersistenceListener` từ `TenantContext`; phụ thuộc `X-Tenant-Id` do gateway resolve từ Host nip.io | Nếu gateway strip/không resolve `X-Tenant-Id` (lớp lỗi GAP-1068) → write 400 hoặc ghi sai tenant. Cross-tenant check (login tenant khác, không thấy course của 5b3ef1ae) sai lệch nếu header không đúng. | `curl -H "Host: sky-education-074901.127.0.0.1.nip.io" :9000/api/v1/courses` → quan sát response gắn đúng tenant. Verify gateway resolve Host→X-Tenant-Id (không cần `?tenant=`). |
| **FM-7** 🟡 LOW | `@PreAuthorize("@authz.hasAccessToClass(#classId)")` trên createSchedule + recurrence | Owner tạo lớp → `teacher_id = X-User-Id` (id owner). Nếu `hasAccessToClass` chỉ cho teacher gán HOẶC platform-admin mà KHÔNG cho owner-role → 403 khi xếp lịch. Owner-tạo-lớp thường pass (teacher_id=owner id) nhưng cần xác nhận logic. | Đọc `@authz.hasAccessToClass` impl: có cho phép owner role / creator không. Walk: owner tạo lớp → xếp lịch ngay → quan sát 403 hay 201. |
| **FM-8** 🟡 LOW | RecurrenceForm — `until` bắt buộc + phải > hôm nay; `byDay` ≥1 ngày | Human để trống ngày kết thúc lặp hoặc chọn ngày quá khứ → inline error "Ngày kết thúc lặp phải sau ngày hôm nay". Quên chọn ngày trong tuần → "Phải chọn ít nhất 1 ngày". | Mở recurrence panel → xác nhận field "Ngày kết thúc lặp" + chọn day-of-week. Hướng dẫn human chọn until tương lai + ≥1 ngày. |

## 3 failure mode HIGH-confidence nhất

1. **FM-1 — "tạo niên khóa" KHÔNG executable:** module academicyear có 0 @RestController, FE không có route/nav niên khóa, seed 0 academic_years. Human kẹt bước đầu tìm UI không tồn tại. Quan trọng: niên khóa DECOUPLED — course/class không tham chiếu `academic_year_id` → các bước còn lại vẫn chạy độc lập.

2. **FM-2 — không có trang xếp lịch tuần độc lập cho owner:** scheduling nhúng trong recurrence panel của form tạo lớp (`courses/[id]/classes/new`), không phải trang riêng. Chỉ teacher có `/(teacher)/teacher/schedule`. Walk step "xếp lịch tuần" phải map = toggle "Lặp lại theo lịch (tuần)" khi tạo lớp.

3. **FM-3 — createCourse cần teacherId, phụ thuộc dropdown useTeachers:** nếu API teachers rỗng/lỗi (gateway header / status filter) → owner không tạo được course. Seed có 2 teacher nhưng cần verify status ACTIVE.

## Verdict

**KC-3 WALKABLE VỚI CAVEAT — KHÔNG phải full happy-path như mô tả.**

- ❌ **Bước "tạo niên khóa" KHÔNG thực hiện được** (FM-1): không có endpoint/UI. Đề xuất: bỏ bước này khỏi walk HOẶC đánh dấu N/A + file gap nếu niên khóa là AC bắt buộc của KC-3. Niên khóa decoupled khỏi phần còn lại.
- ⚠️ **Bước "xếp lịch tuần" map lại** (FM-2): thực hiện qua recurrence toggle trong form tạo lớp, không phải trang riêng. Human cần được hướng dẫn đúng entry point.
- ✅ **Chuỗi tạo khóa học → tạo lớp → recurrence auto-gen sessions → xem sessions → cross-tenant** WALKABLE qua FE thật, với caveat: chọn course trước khi vào /classes (FM-4), mã course IN HOA mới (FM-5), gateway resolve tenant đúng (FM-6), owner có quyền xếp lịch (FM-7), điền until tương lai (FM-8).
- 🔧 **Pre-walk bắt buộc:** xác nhận 2 teacher ACTIVE (FM-3) + gateway Host→X-Tenant-Id (FM-6) trước khi human walk.

Artifact: `documents/04-quality/audits/persona-review/2026-06-15-pre-walk-kc3-academic-setup.md`
