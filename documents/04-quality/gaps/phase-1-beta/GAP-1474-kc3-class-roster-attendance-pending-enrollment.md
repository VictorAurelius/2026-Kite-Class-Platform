# GAP-1474: KC-3 — Owner không xem được danh sách học sinh trong lớp + điểm danh không fetch (enrollment PENDING_PAYMENT)

**Status:** 🟡 PARTIAL (80% — code shipped wave-flow-kc3, pending human G2 re-walk per `feature-ship-runtime-walk-mandate.md`)
**Priority:** 🟠 P2
**Domain:** Frontend+Backend+Seed
**Found:** 2026-06-17 (KC-3 G2 walk — class detail "Generic H2 (Embedded)")
**Affects:** `kiteclass/kiteclass-frontend/src/app/(dashboard)/classes/[id]/**`, `.../attendance/**`, `kiteclass/kiteclass-core/.../module/enrollment`, `kitehub/scripts/seed-walk-tenant.sh`

## Problem

Khi owner mở chi tiết lớp (KC-3, lớp `Generic H2 (Embedded)` = class id 27, tenant `d1d3e28e`), gặp 2 vấn đề:

### A. Không có UX xem danh sách học sinh trong lớp (role Owner)
Trang chi tiết lớp hiển thị **"Sĩ số 1/30"** nhưng KHÔNG có affordance để owner xem *ai* đang ở trong lớp (roster). Có nút "Thêm học sinh vào lớp" + "Import hàng loạt" (bulk-enroll) nhưng không có view danh sách học sinh đã ghi danh. Owner muốn biết lớp gồm những học sinh nào mà không có chỗ xem.

### B. Điểm danh không fetch được danh sách học sinh
Vào điểm danh → danh sách học sinh trống. Root cause (đã verify):

- BE roster cho điểm danh = `EnrollmentRepository.findActiveEnrollmentsByClassId` (`AND e.status = 'ACTIVE'`) + **BR-ATTEND-001: "Enrollment must exist and be ACTIVE"** → chỉ enrollment **ACTIVE** mới hiện trong điểm danh.
- Enrollment duy nhất của class 27 (enr id 109, student 224) có status **`PENDING_PAYMENT`** → bị loại → điểm danh trống.
- **Mâu thuẫn đếm:** `classes.current_enrolled` (Sĩ số 1/30) ĐẾM cả PENDING_PAYMENT, nhưng điểm danh thì KHÔNG → user thấy "1 học sinh" ở header nhưng 0 ở điểm danh, không có thông báo giải thích.

### C. Seed data không đầy đủ (root của B trong walk)
`kitehub/scripts/seed-walk-tenant.sh:120` tạo enrollment bằng `POST /api/v1/enrollments {studentId, classId, tuitionAmount:1200000}` **không xác nhận thanh toán** → enrollment mặc định `PENDING_PAYMENT` và không bao giờ được activate (`EnrollmentService`: `PENDING_PAYMENT → ACTIVE` chỉ khi payment confirmed). Toàn DB `kiteclass_shared`: **44/103 enrollment ở `PENDING_PAYMENT`** → mọi walk điểm danh đều trống.

**Phụ:** class walk (id 27) thuộc tenant `d1d3e28e`, KHÁC tenant committed-seed g2walk (`0edaee10`) → seed (nếu chạy) không tác động đúng tenant user đang walk.

## Proposed Fix

1. **A (FE UX):** thêm view "Danh sách học sinh" trong chi tiết lớp (role Owner/Admin/Staff) — list học sinh đã ghi danh + trạng thái enrollment (ACTIVE / Chờ thanh toán). Có thể dùng `GET /api/v1/classes/{id}/enrollments` (verify endpoint) hoặc `findByClassIdAndDeletedFalse`.
2. **B (UX rõ ràng):** khi điểm danh trống vì enrollment PENDING_PAYMENT → hiện empty-state giải thích ("N học sinh đang chờ thanh toán, chưa thể điểm danh") thay vì list rỗng im lặng. Cân nhắc đồng bộ cách đếm Sĩ số (đếm ACTIVE, hoặc tách "ACTIVE / chờ thanh toán").
3. **C (Seed):** `seed-walk-tenant.sh` sau khi enroll PHẢI confirm payment để enrollment → ACTIVE (committed idempotent), để walk điểm danh có học sinh. Verify endpoint activate enrollment (`PUT /api/v1/enrollments/{id}/confirm-payment` hoặc tương đương). Per `walk-data-committed-seed.md` — seed phải tạo baseline điểm danh dùng được.

## Acceptance Criteria

- [ ] Owner mở chi tiết lớp thấy danh sách học sinh + trạng thái enrollment.
- [ ] Điểm danh: nếu có enrollment ACTIVE → hiện học sinh; nếu chỉ PENDING_PAYMENT → empty-state giải thích (không im lặng).
- [ ] `seed-walk-tenant.sh` tạo ≥1 enrollment ACTIVE (payment-confirmed) cho lớp walk → điểm danh có học sinh sau seed.
- [ ] Sĩ số và roster điểm danh nhất quán (hoặc tách rõ ACTIVE vs chờ thanh toán).
- [ ] Human G2 re-walk KC-3 xác nhận xem roster + điểm danh có học sinh.

## Fix shipped (wave-flow-kc3, branch `docs/gap-1474-kc3-roster-attendance` — PR #2469)

- **A:** `components/enrollment/class-roster-section.tsx` (mới) — card "Danh sách học sinh" trong chi tiết lớp, list mọi enrollment + badge trạng thái (Đang học / Chờ thanh toán / …). Tên resolve client-side từ `useStudents` (roster endpoint chỉ trả `studentId`). Có loading / empty ("Lớp chưa có học sinh nào") / error states.
- **B:** `attendance/page.tsx` + `attendance-form-list.tsx` — khi roster ACTIVE-only trống nhưng có enrollment PENDING_PAYMENT → empty-state giải thích "N học sinh đang chờ xác nhận thanh toán…" thay vì im lặng. BR-ATTEND-001 GIỮ NGUYÊN (attendance vẫn ACTIVE-only). Cũng resolve `studentName` cho hàng điểm danh từ `useStudents`.
- **C:** `kitehub/scripts/seed-walk-tenant.sh` — sau enroll, `PUT /api/v1/enrollments/{id}/status` `{status:ACTIVE}` cho mọi enrollment chưa ACTIVE (idempotent, owner=ROLE_OWNER bypass) → walk có ≥1 ACTIVE → điểm danh có học sinh.
- BE: **KHÔNG đổi** (endpoint + DTO đã có sẵn). Sister surface gradebook (tên trống do `EnrollmentResponse` thiếu `studentName`) → DEFER **GAP-1475** (root fix BE enrichment).
- Verify: FE `pnpm build` + lint + 7 test mới PASS; 188 affected test PASS; seed `shellcheck --severity=warning` clean.
- Còn lại 20%: **human G2 re-walk KC-3** xác nhận xem roster + điểm danh có học sinh.

## Related

- Discovered in: KC-3 G2 walk 2026-06-17 (class 27 `Generic H2 (Embedded)`, tenant d1d3e28e, enrollment 109 PENDING_PAYMENT)
- Sister (DEFER from cross-flow sweep): GAP-1475 (teacher gradebook blank studentName — BE EnrollmentResponse enrichment)
- BR-ATTEND-001 (attendance requires ACTIVE enrollment) — `kiteclass-core/.../module/attendance`
- Seed: `kitehub/scripts/seed-walk-tenant.sh:120`
- Rule: `walk-data-committed-seed.md` (seed phải reproducible + usable baseline)
