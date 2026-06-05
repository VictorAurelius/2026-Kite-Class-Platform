---
title: "Pre-Walk Persona Simulation — KC-4 Enrollment + Bulk Import"
audience: dev
created: 2026-06-05
scope: "Flow Verification Campaign KC-4 — Student enrollment (POST /api/v1/enrollments) + Bulk import students (POST /api/v1/students/bulk-import/{preview,commit}) trong kiteclass-core. Pre-walk static persona simulation per pre-walk-persona-simulation-mandate.md §3 — surface failure modes TRƯỚC khi coordinator walk local Docker stack."
---

# Pre-Walk Persona Simulation — KC-4 Enrollment + Bulk Import

**Mục tiêu:** Mô phỏng tâm lý 2 persona (STAFF ghi danh học sinh + STAFF import hàng loạt) → liệt kê failure mode LIKELY trước khi walk, để batch-fix các finding HIGH trước.

**Phạm vi đã state-check (endpoints EXIST):**
- Enroll: `EnrollmentController` @ `/api/v1/enrollments` — POST enroll + GET by-id/student/class + PUT status/withdraw.
- Bulk import: `BulkImportController` @ `/api/v1/students/bulk-import` — POST `/preview` + `/commit` (multipart) + POST `/jobs/{id}/errors`.

**Context quan trọng — GAP-983 (Wave security-1, vừa fix):** cross-tenant by-id read leak đã được vá bằng cách enable Hibernate `tenantFilter` trên transaction-bound session (`TenantAwareDataSourceInterceptor.enableTenantFilter`), cộng với Postgres RLS qua GUC `app.current_tenant_id`. Hai lớp này CHỈ active khi `TenantContext.isSet()` + transaction active. Đây là trục cần verify kỹ nhất cho enroll.

---

## Phát hiện chính trước khi đọc chi tiết

- **Bulk-import là XLSX-ONLY, KHÔNG phải CSV.** Parser dùng `XSSFWorkbook` (Apache POI). Prompt + bất kỳ UI nào ghi "CSV" sẽ misled — upload `.csv` thật → exception. Đây là contract surprise lớn nhất.
- **Không có validation MIME/content-type.** File `.png` đổi tên `.xlsx`, file `.csv`, file rỗng → POI ném `RuntimeException` (không phải `IOException`) → escape `parseSafely` → generic handler → **HTTP 500** (không phải 400 graceful).
- **Enroll KHÔNG check Class status.** Ghi danh được vào lớp đã `COMPLETED`/`CANCELLED`.

---

## Các failure mode (12) — theo confidence

### 1. [HIGH] Bulk-import từ chối file CSV thật — parser chỉ đọc XLSX
- **(a) Where:** `XlsxParser.parse()` line 65 `new XSSFWorkbook(inputStream)` — `StudentBulkImportService.preview/commit`.
- **(b) Symptom:** Persona "import từ file CSV" tải file `.csv` lên → POI ném `NotOfficeXmlFileException` (RuntimeException). `parseSafely` (line 202-208) chỉ catch `IOException` → exception escape → generic `Exception` handler → **HTTP 500 `SYSTEM_INTERNAL_ERROR`**. User thấy "lỗi hệ thống" thay vì "file phải là .xlsx".
- **(c) Pre-walk check:** `grep -n "XSSFWorkbook\|catch (IOException" XlsxParser.java StudentBulkImportService.java` → xác nhận chỉ catch IOException; POI bad-format exception là RuntimeException không được wrap. Walk: chuẩn bị file `.xlsx` THẬT (Excel/LibreOffice), KHÔNG phải `.csv` đổi đuôi.

### 2. [HIGH] Không validate MIME → file sai định dạng trả 500 thay vì 400/415
- **(a) Where:** `BulkImportController.preview/commit` — không có check `file.getContentType()` / extension; `StudentBulkImportService.assertFilePresent` chỉ check `isEmpty()`.
- **(b) Symptom:** Upload `.png` rename `.xlsx`, hoặc PDF, hoặc binary rác → POI `POIXMLException`/`NotOfficeXmlFileException` (RuntimeException) → **HTTP 500**. Vi phạm `pre-handoff-self-test-completeness.md` §2.5 (a) MIME validation enforced server-side.
- **(c) Pre-walk check:** `grep -n "getContentType\|MediaType\|endsWith" bulkimport/` → 0 hit ngoài `consumes=MULTIPART`. Walk sad-path: upload 1 file ảnh đổi đuôi → quan sát status code.

### 3. [HIGH] Enroll vào lớp COMPLETED/CANCELLED — không check Class status
- **(a) Where:** `EnrollmentServiceImpl.enrollStudent` line 49-111 — load `Class` qua `findByIdForEnrollmentWithLock`, check duplicate + capacity, NHƯNG không đọc `clazz.getStatus()`.
- **(b) Symptom:** Persona ghi danh học sinh vào lớp `id=14` đã `COMPLETED` hoặc `CANCELLED` → enroll thành công (HTTP 201), invoice event publish, `currentEnrolled` tăng. Business-invalid: ghi danh vào lớp đã kết thúc.
- **(c) Pre-walk check:** `grep -n "getStatus\|ClassStatus\|COMPLETED\|CANCELLED" EnrollmentServiceImpl.java` → 0 hit trong `enrollStudent`. Walk: set class 14 sang COMPLETED (psql UPDATE) → POST enroll → kỳ vọng 201 (bug) thay vì 400.

### 4. [MEDIUM] Tên tiếng Việt có dấu — encoding round-trip (UTF-8) trong XLSX + DB
- **(a) Where:** `XlsxParser.formatCell` (STRING branch line 191-193) → `RowValidator` → `StudentService.createStudent` → DB.
- **(b) Symptom:** XLSX lưu string dạng UTF-8 nội bộ (POI handle tốt), NHƯNG nếu DB column collation / connection charset không UTF-8, tên "Trần Thị Hồng" có thể lưu sai (mojibake). Ít rủi ro hơn CSV vì XLSX không có BOM issue, nhưng vẫn cần verify DB charset.
- **(c) Pre-walk check:** Walk: import 1 row tên có dấu đầy đủ → `psql -c "SELECT name FROM students WHERE ... "` xác nhận đúng dấu. (XLSX nên OK; flag MEDIUM vì DB-layer chưa verify trong scope này.)

### 5. [MEDIUM] Enroll yêu cầu `tuitionAmount` bắt buộc — contract surprise cho persona
- **(a) Where:** `CreateEnrollmentRequest.tuitionAmount` line 48-51 `@NotNull` + `@Digits(integer=8, fraction=2)`.
- **(b) Symptom:** Persona "chỉ muốn ghi danh học sinh vào lớp" gửi `{studentId, classId}` → **HTTP 400** `MethodArgumentNotValidException` "Tuition amount is required". Không obvious từ tên flow; FE phải gửi tuitionAmount. Ngoài ra `@Digits(integer=8)` → học phí ≥ 100,000,000đ → 400.
- **(c) Pre-walk check:** Đã xác nhận DTO. Walk: POST `/enrollments` với body tối thiểu `{studentId, classId, tuitionAmount}`; KHÔNG quên tuitionAmount.

### 6. [MEDIUM] Enroll cross-tenant phụ thuộc HOÀN TOÀN vào tenantFilter + RLS (GAP-983) — cần TenantContext set
- **(a) Where:** `enrollStudent` dùng `studentRepository.findByIdAndDeletedFalse(studentId)` (line 53) + `classRepository.findByIdForEnrollmentWithLock(classId)` (line 61) — CẢ HAI KHÔNG có `instanceId` trong query; chỉ dựa `@Filter tenantFilter` + RLS GUC.
- **(b) Symptom:** Nếu `TenantContext` chưa set (gateway không truyền tenant cho POST enroll, hoặc filter chạy trước transaction), `tenantFilter` không enable → student/class của tenant KHÁC bị load → enroll cross-tenant (HTTP 201 thay vì 404). Đây chính là class lỗi GAP-983 vừa fix — cần re-walk verify fix giữ vững cho enroll path (per `pre-handoff-self-test-completeness.md` §3 post-fix re-walk).
- **(c) Pre-walk check:** `grep -n "findByIdAndInstanceId\|findByIdForEnrollmentWithLock\|findByIdAndDeletedFalse" EnrollmentServiceImpl.java` (xác nhận không có instanceId param trên enroll path). Walk sad-path: login tenant A, POST enroll với `studentId`/`classId` thuộc tenant B → kỳ vọng 404 (fix giữ) NOT 201.

### 7. [MEDIUM] File quá lớn / multipart malformed → 500 thay vì 413
- **(a) Where:** `BulkImportController` — không có `@ExceptionHandler(MaxUploadSizeExceededException)` / `MultipartException` trong `GlobalExceptionHandler` (đã verify 0 hit). Row-limit 413 chỉ check SAU khi parse (`assertRowLimit` MAX_ROWS=1000).
- **(b) Symptom:** File vượt `spring.servlet.multipart.max-file-size` (default Spring) → container ném `MaxUploadSizeExceededException` TRƯỚC khi vào service → generic handler → **HTTP 500**, không phải 413. (Lưu ý: 413 cho >1000 ROWS vẫn hoạt động đúng vì check ở service-layer.)
- **(c) Pre-walk check:** `grep -rn "max-file-size\|MaxUpload" application*.yml GlobalExceptionHandler.java` → 0 hit core-level. Walk: upload file >cấu hình multipart → quan sát status.

### 8. [MEDIUM] Học sinh soft-deleted vẫn enroll được? — student lookup dùng `deletedFalse`, OK; nhưng cross-check class
- **(a) Where:** `enrollStudent` line 53 `findByIdAndDeletedFalse(studentId)` → student deleted → 404 đúng. Class line 61 `findByIdForEnrollmentWithLock` cũng `deleted=false`. OK.
- **(b) Symptom:** Đây là path ĐÚNG — flag MEDIUM để confirm trong walk: student/class đã soft-delete → enroll 404 `STUDENT_NOT_FOUND`/`CLASS_NOT_FOUND` (không phải 500).
- **(c) Pre-walk check:** Đã xác nhận cả 2 query có `deleted=false`. Walk happy verify: soft-delete student → enroll → kỳ vọng 404.

### 9. [LOW] Duplicate enrollment — đã có guard (BR-ENROLL-002)
- **(a) Where:** `enrollStudent` line 64-71 `findByStudentIdAndClassIdAndDeletedFalse` → `DuplicateResourceException("ENROLLMENT_DUPLICATE")` → handler → 409.
- **(b) Symptom:** Enroll cùng student vào cùng class lần 2 → 409 graceful (đúng). Lưu ý: check "regardless of status" — kể cả enrollment WITHDRAWN cũng coi là duplicate (có thể là intent, nhưng persona muốn re-enroll sau withdraw sẽ bị chặn). Verify trong walk.
- **(c) Pre-walk check:** Đã xác nhận. Walk: enroll → withdraw → enroll lại cùng class → kỳ vọng 409 (re-enroll bị chặn — có thể là UX gap).

### 10. [LOW] In-file duplicate email/phone trong bulk — đã có guard
- **(a) Where:** `StudentBulkImportService.detectInFileDuplicates` line 230-253 — email case-insensitive, phone exact; first occurrence valid, sau đó RowError.
- **(b) Symptom:** File có 2 row cùng email → row sau bị skip + báo "Email trùng với dòng N trong cùng file". OK graceful. Verify message hiển thị đúng.
- **(c) Pre-walk check:** Đã xác nhận. Walk: file 3 row, row 2+3 trùng email row 1 → preview báo 2 lỗi, commit chỉ tạo 1.

### 11. [LOW] Preview vs Commit state drift — duplicate-existing không bắt ở preview
- **(a) Where:** `preview` (line 76-101) chạy `rowValidator.validate` + `detectInFileDuplicates` (in-file) NHƯNG KHÔNG check duplicate-với-DB-existing (vì preview không gọi `createStudent`). `commit` → `chunkExecutor.processChunk` → `createStudent` → bắt `STUDENT_EMAIL_EXISTS` khi insert.
- **(b) Symptom:** Preview báo "5 success" (email chưa có trong file), nhưng commit chỉ 3 success vì 2 email đã tồn tại trong DB từ trước → preview-commit drift. Persona ngạc nhiên "preview nói OK mà commit fail". Đây là by-design (preview rẻ, không hit DB uniqueness) nhưng cần document/verify expectation.
- **(c) Pre-walk check:** Đã xác nhận: `preview` không gọi `createStudent`. Walk: seed 1 student email X → upload file có email X → preview success=1, commit success=0 (email đã tồn tại) → confirm drift.

### 12. [LOW] Partial failure trong commit — skip-and-report (KHÔNG rollback all)
- **(a) Where:** `commit` chunk loop line 131-141 + `BulkImportChunkExecutor.processChunk` (`REQUIRES_NEW` per chunk). Row lỗi → RowError, row OK → vẫn tạo. Không rollback toàn bộ.
- **(b) Symptom:** File 5 row, row 3 lỗi validate → row 1,2,4,5 vẫn được tạo; row 3 báo lỗi. Persona kỳ vọng "all-or-nothing"? Đây là chính sách skip-and-report by-design (đã document trong javadoc). Verify: chunk-level `REQUIRES_NEW` nghĩa là chunk fail không rollback chunk trước. Confirm với file nhỏ <500 row (1 chunk) vs >500 (multi-chunk).
- **(c) Pre-walk check:** Đã xác nhận policy. Walk: file 5 row với row 3 invalid → commit `success=4, failedRows=1`, 4 student được tạo trong DB.

---

## Recommended pre-walk batch fix (theo confidence × impact)

### 🔴 HIGH — fix/clarify TRƯỚC khi walk

1. **Finding #1 + #2 (cùng root): non-xlsx upload → 500.** Đây là class lỗi `pre-walk-static-audit-bundle` / `pre-handoff §2.5(a)`. Đề xuất: wrap POI `RuntimeException` (`NotOfficeXmlFileException`, `POIXMLException`, `EmptyFileException`, `OLE2NotOfficeXmlFileException`) trong `parseSafely` → `BulkImportParseException` (400), VÀ thêm pre-check MIME/extension ở controller hoặc service (`.xlsx` only, reject `.csv`/khác với 415/400). Cập nhật mọi UI/recipe ghi "CSV" → "XLSX". **Đây là contract surprise quan trọng nhất — coordinator PHẢI dùng file `.xlsx` thật khi walk.**

2. **Finding #3: enroll vào lớp COMPLETED/CANCELLED.** Thêm guard `clazz.getStatus()` trong `enrollStudent` sau khi load class — reject với `ValidationException("CLASS_NOT_ENROLLABLE")` nếu status ∉ {ACTIVE/SCHEDULED/...}. Business-correctness gap.

### 🟡 MEDIUM — spot-check trong walk (fix nếu confirm)

- **#6 cross-tenant enroll** (GAP-983 re-walk): bắt buộc walk sad-path login tenant A + enroll student/class tenant B → verify 404. Đây là post-fix re-walk per `pre-handoff §3`.
- **#7 file quá lớn → 500 thay vì 413**: thêm `@ExceptionHandler(MaxUploadSizeExceededException.class)` → 413.
- **#5 tuitionAmount bắt buộc**: confirm FE gửi field; nếu không → 400 (đúng nhưng cần UX message rõ).
- **#4 encoding tên có dấu**: spot-check DB charset.

### 🟢 LOW — defer to walk observation

- #9 (re-enroll sau withdraw bị 409 — UX gap?), #11 (preview-commit drift by-design), #12 (skip-and-report by-design). Quan sát + document, không cần fix gấp.

---

## Endpoint contract surprises (cho coordinator)

1. **Bulk-import = XLSX-only (Apache POI XSSF), KHÔNG CSV.** Bất kỳ tài liệu/UI nào ghi "CSV" là sai. Upload file `.xlsx` thật.
2. **XLSX header schema (case-insensitive, thứ tự cột linh hoạt):** bắt buộc `name`, `email`; optional `phone`, `date_of_birth`, `gender`, `address`, `note`. Date format `dd/MM/yyyy`. Gender chỉ `MALE`/`FEMALE` (enum không có OTHER → reject). Phone `^0\d{9}$`.
3. **Enroll body bắt buộc:** `studentId` (Positive), `classId` (Positive), `tuitionAmount` (`@NotNull`, ≤8 integer digits + 2 decimals). Optional: `discountPercent` (0-100), `notes` (≤2000). Header `Idempotency-Key` optional (GAP-730 dedupe).
4. **Multipart field name = `file`**; header `X-Tenant-Id: <UUID>` BẮT BUỘC cho preview/commit (controller `@RequestHeader("X-Tenant-Id")` — thiếu → 400). Lưu ý: bulk-import truyền tenantId NỖI TƯỜNG MINH (không qua TenantContext) → `createStudent(req, tenantId)` dùng explicit `existsByEmailAndInstanceIdAndDeletedFalse` → path này tenant-safe (khác enroll path dựa @Filter).
5. **Row cap 1000** (HTTP 413 nếu >1000 ROWS — check sau parse, hoạt động đúng); chunk 500/transaction. Error report qua **POST** `/jobs/{id}/errors` (re-upload file), GET trả 405.
