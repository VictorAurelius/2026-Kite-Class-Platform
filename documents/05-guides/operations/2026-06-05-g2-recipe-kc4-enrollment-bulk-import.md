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
- **🔄 Data REFRESHED 2026-06-15** (tenant `sky-education` cũ `0edaee10` đã re-provision; data cũ stale). Tenant walk-ready mới seed qua real pipeline:
  - **Tenant:** `sky-education-171900` (instance_id `271dc912-fae2-4197-823b-8eec56fc7931`)
  - **Owner login (KC `:3000`):** `owner+171900@skyedu.vn` / `SkyEdu@2026`
  - **Students:** 171 Phạm Thị Mai, 172 Hoàng Văn Nam (đã ghi danh class 31); **173 Đỗ Thị Lan, 174 Vũ Minh Quang (CHƯA ghi danh class 31 — dùng cho Bước 2)**
  - **Class 31** "Lớp IELTS 6.5 - Tối Thứ 246" — **SCHEDULED, max 15, hiện 2** (còn chỗ ghi danh)
  - **Class 33** "Lớp đã hủy (KC-4 G2 fixture)" — **CANCELLED** (dùng cho Bước 6 GAP-989)
  - **Isolation (Bước 7):** dùng resource id của tenant KHÁC — vd class `2` / student `3` (thuộc tenant `e8ff87e1` cũ) → owner `171900` không thấy → 404.

**Thời lượng:** ~12-15 phút.

**⚠️ Lưu ý quan trọng:** Bulk-import nhận **XLSX** (Excel), KHÔNG phải CSV. Header bắt buộc: `name`, `email`; tùy chọn: `phone`, `date_of_birth` (dd/MM/yyyy), `gender` (MALE/FEMALE), `address`, `note`.

## 2. Setup

**🔴 Truy cập production-accurate qua subdomain Host (KHÔNG `localhost`)** — per `g1-browser-walk-before-flip.md` §3.1/§3.2. Production truy cập KiteClass tenant qua `{slug}.kitehub.me`; gateway resolve tenant **từ Host** (GAP-814: client `X-Tenant-Id` bị strip), FE `auth.ts` gọi gateway cùng hostname + loại trừ `localhost`. Mở `localhost:3000` → Host=localhost → không có subdomain → bypass đường resolution thật.

- **URL walk:** `http://sky-education-171900.127.0.0.1.nip.io:3000` (nip.io wildcard resolve về 127.0.0.1, Host header mang subdomain thật, no sudo — verified 2026-06-15).
- FE tự đọc `window.location.hostname` → gọi gateway tại `sky-education-171900.127.0.0.1.nip.io:9000` với Host subdomain → tenant resolve đúng production path.

Walk ưu tiên qua UI (frontend); mỗi bước có **curl fallback** (gọi gateway `:9000` với owner JWT — tenant từ JWT claim). Body enroll cần `tuitionAmount`.

## 3. Các bước

### Bước 1 — Owner login + mở khu vực ghi danh
- **Hành động:** Mở browser `http://sky-education-171900.127.0.0.1.nip.io:3000` (subdomain Host production-accurate — KHÔNG `localhost`, xem §2) → đăng nhập `owner+171900@skyedu.vn` / `SkyEdu@2026` → vào lớp **class 31** "Lớp IELTS 6.5 - Tối Thứ 246" → tab "Học sinh / Ghi danh".
- **Kỳ vọng:** Thấy danh sách học sinh đã ghi danh của lớp (tenant-scoped) — 2 học sinh (171, 172).
- **Verify (curl):** `GET /api/v1/enrollments/class/31` -H tenant sky → 200.

### Bước 2 — Ghi danh học sinh vào lớp
- **Hành động:** Chọn học sinh **"Đỗ Thị Lan" id 173** (chưa ghi danh class 31) → nhập học phí → "Ghi danh".
- **Kỳ vọng:** HTTP 201, enrollment status `PENDING_PAYMENT`, `finalAmount` = tuition − discount; sĩ số lớp (`currentEnrolled`) tăng 1 (2 → 3).
- **Sad path:** Lớp đầy (currentEnrolled = maxStudents) → 400 capacity error.
- **Verify (curl):** `POST /api/v1/enrollments` body `{"studentId":173,"classId":31,"tuitionAmount":1500000}` → 201.

### Bước 3 — Ghi danh trùng (duplicate guard)
- **Hành động:** Ghi danh lại học sinh **171** (đã ở class 31) vào cùng class 31.
- **Kỳ vọng:** **HTTP 409** (đã ghi danh rồi) — không tạo bản ghi trùng.
- **Verify (curl):** `POST /api/v1/enrollments` body `{"studentId":171,"classId":31,"tuitionAmount":1500000}` → 409.

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
- **Hành động:** Thử ghi danh học sinh (vd 174) vào **class 33** "Lớp đã hủy (KC-4 G2 fixture)" (status **CANCELLED**).
- **Kỳ vọng:** **HTTP 400** `CLASS_NOT_ENROLLABLE` "Lớp ... không thể nhận ghi danh mới (status: Đã hủy)". (Trước fix: 201 sai.)
- **Verify (curl):** `POST /api/v1/enrollments` body `{"studentId":174,"classId":33,"tuitionAmount":1500000}` → **400** `CLASS_NOT_ENROLLABLE`.

### Bước 7 — Cách ly đa tenant 🔒
- **Hành động:** Là owner `171900`, thử ghi danh vào **class 2 / student 3** (thuộc tenant `e8ff87e1` khác — không thuộc tenant `171900`).
- **Kỳ vọng:** **HTTP 404** (filter chặn — không thấy resource tenant khác).
- **Verify (curl):** `POST /api/v1/enrollments` -H tenant `271dc912...` body `{"studentId":3,"classId":2,"tuitionAmount":1500000}` → **404**.

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
