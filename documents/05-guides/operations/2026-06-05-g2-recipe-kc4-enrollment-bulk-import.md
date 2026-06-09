---
title: G2 Human Test Recipe — KC-4 Student enrollment + bulk import
audience: dev
product: KiteClass (KC) — FE kiteclass-frontend :3000, backend kiteclass-core qua gateway :9000 (per kitehub-kiteclass-boundary.md §2)
created: 2026-06-05
flow: KC-4
scope: Flow Verification Campaign G2 handoff cho luồng KC-4 (Owner ghi danh học sinh vào lớp + import hàng loạt qua XLSX)
---

# G2 Recipe — KC-4 Student enrollment + bulk import

## 1. Mục tiêu + prereq + thời lượng

**Mục tiêu:** Owner ghi danh học sinh vào lớp (đơn lẻ) + import hàng loạt học sinh từ file **XLSX**, verify capacity/duplicate/cross-tenant + 2 fix vừa ship (GAP-988 bulk-import bad-format, GAP-989 enroll lớp đã đóng).

**Prereq:**
- Stack local UP (`curl -s -o /dev/null -w '%{http_code}' http://localhost:8088/actuator/health` → 200).
- KC-2 (đã có học sinh) + KC-3 (đã có lớp). Data sẵn: tenant `sky-education` (`0edaee10-...`) có students id 4-9, class 14 "Lớp A1 - Ca tối T2-T4" (SCHEDULED, max 20). Tenant `khanh-phapluat` (`126eaa8c-...`) để test isolation.

**Thời lượng:** ~12-15 phút.

**⚠️ Lưu ý quan trọng:** Bulk-import nhận **XLSX** (Excel), KHÔNG phải CSV. Header bắt buộc: `name`, `email`; tùy chọn: `phone`, `date_of_birth` (dd/MM/yyyy), `gender` (MALE/FEMALE), `address`, `note`.

## 2. Setup
Walk ưu tiên qua UI (frontend); mỗi bước có **curl fallback** (gọi thẳng core 8088 với header `X-Tenant-Id`). Body enroll cần `tuitionAmount`.

## 3. Các bước

### Bước 1 — Owner login + mở khu vực ghi danh
- **Hành động:** Đăng nhập Owner sky-education → vào lớp (class 14) → tab "Học sinh / Ghi danh".
- **Kỳ vọng:** Thấy danh sách học sinh đã ghi danh của lớp (tenant-scoped).
- **Verify (curl):** `GET /api/v1/enrollments/class/14` -H tenant sky → 200.

### Bước 2 — Ghi danh học sinh vào lớp
- **Hành động:** Chọn học sinh (vd "Bùi Văn Dũng" id 4) → nhập học phí → "Ghi danh".
- **Kỳ vọng:** HTTP 201, enrollment status `PENDING_PAYMENT`, `finalAmount` = tuition − discount; sĩ số lớp (`currentEnrolled`) tăng 1.
- **Sad path:** Lớp đầy (currentEnrolled = maxStudents) → 400 capacity error.
- **Verify (curl):** `POST /api/v1/enrollments` body `{"studentId":4,"classId":14,"tuitionAmount":1500000}` → 201.

### Bước 3 — Ghi danh trùng (duplicate guard)
- **Hành động:** Ghi danh lại cùng học sinh vào cùng lớp.
- **Kỳ vọng:** **HTTP 409** (đã ghi danh rồi) — không tạo bản ghi trùng.

### Bước 4 — Import hàng loạt XLSX (preview → commit)
- **Hành động:** "Import học sinh" → tải template XLSX (nếu UI có) → điền vài dòng (name + email + phone tùy chọn) → upload **Preview**.
- **Kỳ vọng:** Preview hiển thị các dòng parse được + đánh dấu dòng lỗi/trùng. Sau đó **Commit** → tạo học sinh mới (tenant-scoped), báo số tạo thành công / bỏ qua.
- **Sad path:** Dòng trùng email/phone với học sinh đã tồn tại → preview đánh dấu skip (không all-or-nothing — commit phần hợp lệ).
- **Verify (curl):** `POST /api/v1/students/bulk-import/preview` -F `file=@students.xlsx` → 200 + preview JSON; `POST .../commit` → 200.

### Bước 5 — Upload sai định dạng (GAP-988 fix) 🛡️
- **Hành động:** Upload file CSV (hoặc ảnh đổi tên `.xlsx`, file rỗng) vào ô import.
- **Kỳ vọng:** **HTTP 415** (sai loại file) hoặc **400** (nội dung hỏng) — KHÔNG còn 500. UI báo lỗi rõ "file phải là XLSX".
- **Verify (curl):** `POST .../bulk-import/preview` -F `file=@bad.csv` → **415**; file ảnh đổi tên `.xlsx` → **400**. (Trước fix: 500.)

### Bước 6 — Ghi danh vào lớp đã đóng (GAP-989 fix) 🛡️
- **Hành động:** Thử ghi danh học sinh vào lớp có trạng thái **Đã hoàn thành (COMPLETED)** hoặc **Đã hủy (CANCELLED)**.
- **Kỳ vọng:** **HTTP 400** `CLASS_NOT_ENROLLABLE` "Lớp ... không thể nhận ghi danh mới (status: Đã hoàn thành)". (Trước fix: 201 sai.)

### Bước 7 — Cách ly đa tenant 🔒
- **Hành động:** Đổi sang tenant `khanh-phapluat`, thử ghi danh vào student/class của sky (id 4 / 14).
- **Kỳ vọng:** **HTTP 404** (filter chặn — không thấy resource tenant khác).
- **Verify (curl):** `POST /api/v1/enrollments` -H tenant khanh body `{"studentId":4,"classId":14,"tuitionAmount":1500000}` → **404**.

## 4. Sad path quick checks
- Duplicate enroll → 409 (Bước 3).
- Lớp đầy → 400 capacity.
- Bad-format upload → 415/400 không 500 (Bước 5, GAP-988).
- Enroll lớp COMPLETED/CANCELLED → 400 (Bước 6, GAP-989).
- Cross-tenant → 404 (Bước 7).

## 5. Báo kết quả (4 outcome)
- ✅ **FULL PASS** — Bước 1-7 đúng kỳ vọng → flip KC-4 campaign row `✅ THÔNG (G1+G2)`.
- ⚠️ **PASS-with-note** — flow chính OK nhưng UI import chưa hoàn chỉnh (vd thiếu template download) → catalog gap polish.
- 🔴 **FAIL-functional/isolation** — enroll/import lỗi HOẶC Bước 7 không 404 → ghi bước + HTTP code → file gap (isolation fail = P0).
- ⛔ **BLOCKED** — FE chưa có trang enrollment/import → walk curl fallback + note FE gap.

## 6. Troubleshooting + G3 preview
- **Enroll 500 trên course/teacher đọc:** Redis cache poisoning (GAP-986) — flush `courses*`/`teachers*` keys.
- **Bulk-import 200 nhưng 0 student tạo:** kiểm tra header XLSX đúng `name`,`email` + sheet đầu tiên.
- **G3 preview (production parity):** sau G2 PASS local, G3 verify trên AWS (post GAP-612 restore) — enrollment isolation + file storage XLSX qua MinIO/S3 + RLS layer (GAP-985). Bulk-import XLSX storage location + virus scan = mục cần verify production (per `pre-handoff-self-test-completeness.md` §2.5 file-upload checklist).
