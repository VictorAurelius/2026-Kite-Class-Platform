# GAP-982: Academic-year module orphan — service đầy đủ logic nhưng KHÔNG controller, KHÔNG caller

**Status:** 🔵 OPEN
**Priority:** 🟠 P1
**Domain:** Backend
**Found:** 2026-06-05 (Wave flow-kc3 KC-3 hardened state-check, non-audit work)
**Affects:** `kiteclass/kiteclass-core/.../module/academicyear/**`

## Problem

Trong KC-3 hardened state-check (verify class/schedule controller tồn tại), phát hiện module `academicyear` là **orphan**: business logic đầy đủ nhưng KHÔNG có REST endpoint nào expose, và KHÔNG service nào khác wire vào.

Evidence:
- `AcademicYearService.java:49-125` có đủ public methods: `createAcademicYear(name, startDate, endDate)`, `setCurrent(id)`, `getCurrent()`, `getById(id)`, `listAll()`, `isHoliday(date)`.
- Module có entity (`AcademicYear`, `Semester`, `Holiday`, `HolidayType`, `SemesterType`, `AcademicYearStatus`) + 3 repository + `VnHolidayProvider`.
- `grep -rnl "AcademicYearService" kiteclass-core/src/main/java` → CHỈ trả về repository + chính file service. **Không controller, không provisioning, không bootstrap caller.**
- `grep` toàn bộ `@RequestMapping`/`@*Mapping` trong kiteclass-core → **không có endpoint academic-year/semester** nào (chỉ `GradeController` có path segment `/semester/{semester}` không liên quan tạo niên khóa).

Hệ quả: persona Owner/STAFF **không thể tạo niên khóa qua API** — step 1 của journey KC-3 "niên khóa → khóa học → lớp → lịch" bị đứt. Niên khóa hiện là dead code, không trigger được runtime. Liên quan grade rollup ambiguity (sister [[GAP-960]] — VN Năm học default missing, P1).

**Lưu ý decoupling:** `Class` entity chỉ FK `course_id` (required) + `teacher_id` (nullable UUID), KHÔNG có `academic_year_id`. Chuỗi course→class→schedule chạy độc lập, KHÔNG bị block bởi orphan này — đó là lý do KC-3 walk vẫn proceed được cho chuỗi cốt lõi.

## Proposed Fix

Ship `AcademicYearController` expose CRUD đã có sẵn trong service (`POST /api/v1/academic-years` tạo + `GET` list/current + `PATCH .../set-current`), wire vào setup flow Owner/STAFF. Cân nhắc gộp với [[GAP-960]] (VN năm học default) thành 1 wave academic-year vì cùng surface. Defer sang wave riêng (không block KC-3 walk).

## Acceptance Criteria

- [ ] `POST /api/v1/academic-years` (Owner/STAFF) trả 201 + DB row trong `academic_years`
- [ ] `GET /api/v1/academic-years` list + `getCurrent` endpoint hoạt động
- [ ] Multi-tenant scope: niên khóa của tenant A không rò sang tenant B
- [ ] Cross-reference [[GAP-960]] grade rollup khi wire default năm học

## Related

- Discovered in: Wave flow-kc3 KC-3 hardened state-check (session 2026-06-05), commit pending
- Sister: GAP-960 (VN Năm học default missing — grade rollup), GAP-072 (academic-year-tied branding refresh, P2)
- Per `discovery-to-gap-inline-filing.md` §3 (non-audit work discovery → file inline)
