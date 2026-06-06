# Pre-Walk Persona Simulation — KC-10 Per-Tenant Branding Wizard → Approval

**Flow:** KC-10 — OWNER/ADMIN của 1 tenant KiteClass chỉnh branding (màu/theme/logo/favicon) qua wizard `/api/v1/settings/branding`, snapshot version history `/api/v1/branding/{instanceId}/versions`, manual rollback (apply), serve theme cho trang login qua `/api/v1/branding/public`, composite package `/api/v1/branding/{instanceId}/package`, cache-evict webhook `/internal/notify/instance-deployed`. Asset side-effect verify qua MinIO (`kite-branding-assets`).
**Date:** 2026-06-06
**Mandate:** `.claude/rules/pre-walk-persona-simulation-mandate.md` (prediction-only, KHÔNG fix).
**Stack:** gateway `:9000` (JWT HS512 → strip client headers `X-Tenant-Id`/`X-User-Id` default-filter GAP-814 → inject từ JWT verified) → `kiteclass-core:8080` (multi-tenant per-school DB, Hibernate `tenantFilter` + `TenantContext` ThreadLocal). KiteClass auth = tenant-scoped `/api/v1/tenant-auth/login`. MinIO `kite-branding-assets` cho logo/favicon. **CHÚ Ý: KC-10 ≠ KH-6** — KH-6 là `kitehub-branding` (AI wizard), KC-10 là per-tenant branding trong `kiteclass-core`. Sự nhầm lẫn này là gốc rễ FM-1.

---

## Câu trả lời 2 câu hỏi headline

### (i) Gateway route `/api/v1/branding/**` → kitehub-branding, KHÔNG phải kiteclass-core ❌ (THE walk-blocker)

`kitehub/kitehub-gateway/src/main/resources/application.yml:593-601` route `kitehub-branding-v1` predicate `Path=/api/v1/branding/**` → `uri: http://kitehub-branding:8080`. Route này được khai báo TRƯỚC catch-all `instance-apis` (`application.yml:746-755`, `Path=/api/v1/**` → kiteclass-core). Spring Cloud Gateway match theo thứ tự khai báo, first-match-wins → **MỌI** request `/api/v1/branding/**` đi tới `kitehub-branding` (KH-6 AI service), KHÔNG bao giờ tới 3 controller KiteClass:

```
/api/v1/branding/public            (PublicBrandingController)    ─┐
/api/v1/branding/{id}/versions     (BrandingVersionController)   ─┼─► SHADOWED bởi kitehub-branding-v1
/api/v1/branding/{id}/versions/{n}/rollback                      ─┤   → kitehub-branding:8080
/api/v1/branding/{id}/package      (BrandingPackageController)   ─┘   (KHÔNG tới kiteclass-core)
```

→ 3/5 controller KC-10 (version history / rollback "approval" / public theme / package) **UNREACHABLE qua gateway**. Chỉ `BrandingController` (`/api/v1/settings/branding`, KHÔNG khớp `/api/v1/branding/**`) rơi xuống catch-all `instance-apis` → kiteclass-core. FE thực tế chỉ gọi `/api/v1/settings/branding` (`kiteclass-frontend/src/lib/api/branding.ts:12`) cho wizard chính + `/api/v1/branding/public` (`src/lib/api/public-branding.ts:38`) cho login page — cái thứ hai bị shadow.

### (ii) Role bridge kiteclass-core CÓ — version endpoints KHÔNG dead-deny (nhưng shadowed bởi routing) ✅/moot

`SecurityConfig.java:52-59` `.anyRequest().permitAll()` URL-layer + `GatewayHeaderAuthenticationFilter` (`config/GatewayHeaderAuthenticationFilter.java:64-93`) bridge `X-User-Roles` → `ROLE_OWNER`/`ROLE_ADMIN` authorities → `@PreAuthorize("hasAnyRole('ADMIN','OWNER')")` (`BrandingVersionController.java:47,61`) resolve đúng. Đây là KC-7 G1 fix 2026-06-05 (`SecurityConfig.java:56` comment) — KHÔNG còn 24-endpoint dead-deny. Nên nếu version endpoints reachable (chúng KHÔNG, do FM-1), authz sẽ hoạt động. `BrandingController` (`/settings/branding`) KHÔNG có `@PreAuthorize` ở method (chỉ javadoc "Requires admin role") → bất kỳ authenticated tenant user nào PUT được branding (xem FM-7).

---

## Ranked failure modes (confidence × impact)

### FM-1 🔴 Routing collision — `/api/v1/branding/**` → kitehub-branding shadows KiteClass public/version/package endpoints
- **(a) Where:** `kitehub/kitehub-gateway/src/main/resources/application.yml:593-601` (route `kitehub-branding-v1`, declared trước catch-all `instance-apis` `:746-755`). KiteClass controllers bị shadow: `PublicBrandingController.java:33` (`/api/v1/branding/public`), `BrandingVersionController.java:31,45,59`, `BrandingPackageController.java:24,31`.
- **(b) Symptom walker thấy:** Login page KiteClass gọi `GET /api/v1/branding/public?tenantId=X` (`public-branding.ts:38`) → gateway forward tới `kitehub-branding:8080` → kitehub-branding KHÔNG có handler `/api/v1/branding/public` (hoặc có handler khác) → 404 / payload sai service → login page render default theme thay vì tenant branding. Version history + rollback ("approval/apply") + package: curl qua gateway trả response của kitehub-branding (404/wrong) → walker tưởng KC-10 endpoint hỏng. **Walk các sub-flow version/rollback/public/package KHÔNG chạy được qua gateway.**
- **(c) Pre-walk check:**
  ```bash
  # 1) Xác nhận route collision
  grep -nE "id: kitehub-branding-v1|Path=/api/v1/branding|id: instance-apis|Path=/api/v1/\*\*" \
    kitehub/kitehub-gateway/src/main/resources/application.yml
  # 2) Curl public branding qua gateway — xem service nào trả lời (kiteclass vs kitehub-branding)
  curl -s -i "http://localhost:9000/api/v1/branding/public?tenantId=<tenant-uuid>" | head -20
  # 3) So sánh: gọi THẲNG kiteclass-core (bypass gateway) — nếu container expose 8080
  curl -s -i "http://localhost:8080/api/v1/branding/public?tenantId=<tenant-uuid>" | head -20
  # Nếu (2) ≠ (3) → routing shadow confirmed. Version/rollback/package chỉ walk được qua direct curl.
  ```

### FM-2 🟠 `/api/v1/settings/branding` cần TenantResolver từ Host header — 400 nếu thiếu, default-branding nếu TenantContext null
- **(a) Where:** Catch-all route `instance-apis` (`application.yml:746-755`) áp `TenantResolver` filter → resolve `X-Tenant-Id` từ Host/subdomain. `TenantFilterInterceptor.java:77-97` đọc `X-Tenant-Id` → `TenantContext.setCurrentTenant()`. `BrandingServiceImpl.getBranding():95` + `updateBranding():112` đọc `TenantContext.getCurrentTenant()`.
- **(b) Symptom:** Walk wizard mà KHÔNG đi đúng tenant subdomain/Host → `TenantResolver` reject 400 (precedent GAP-539 beta-status comment `application.yml:603-608` "TenantResolver rejected as 400 no tenant header"). Nếu request lọt nhưng `X-Tenant-Id` rỗng → `TenantContext.getCurrentTenant()` = null → `getBranding()` `findByInstanceIdAndDeletedFalse(null)` → empty → `createDefaultBranding(null)` (`BrandingServiceImpl.java:267`) → trả default branding "KiteClass" SILENT (không 500). Walker tưởng tenant chưa set branding nhưng thực ra tenant context bị mất. `@Cacheable key = TenantContext.getCurrentTenant()` = null cache-key.
- **(c) Pre-walk check:**
  ```bash
  # JWT tenant login → xem token có claim tenant + gateway map Host→X-Tenant-Id không
  curl -s -X POST http://localhost:9000/api/v1/tenant-auth/login \
    -H "Content-Type: application/json" -d '{"email":"<owner>","password":"<pw>","tenantSlug":"<slug>"}'
  # GET settings/branding qua gateway VỚI Host header tenant đúng
  curl -s -i -H "Host: <tenant-slug>.localhost" -H "Authorization: Bearer $OWNER_JWT" \
    http://localhost:9000/api/v1/settings/branding | head -20   # Expect 200 + tenant branding, NOT 400/default
  ```

### FM-3 🟠 SVG XSS — logo/favicon upload chấp nhận `image/svg+xml`, MIME validate CHỈ từ client header (không magic-byte)
- **(a) Where:** `BrandingServiceImpl.java:47-49` `ALLOWED_CONTENT_TYPES` chứa `"image/svg+xml"`. `storeAsset():222-225` validate bằng `file.getContentType()` (multipart header do client gửi, KHÔNG sniff magic-byte). `MinIOBrandingAssetStorageImpl.store():88-90` lưu với chính `contentType` đó → presigned GET URL (`:101-105`) trả về với `Content-Type: image/svg+xml`.
- **(b) Symptom:** Upload SVG chứa `<script>alert(document.cookie)</script>` → lưu MinIO + presigned URL. Logo render qua `<img src>` KHÔNG execute script (an toàn). NHƯNG nếu user/nạn nhân navigate TRỰC TIẾP tới presigned URL hoặc nhúng qua `<object>`/`<iframe>` → SVG script execute trong origin MinIO (`kite-minio`). Stored XSS moderate (origin khác app nên cookie app không leak trực tiếp, nhưng phishing/UI-redress). Thêm: client có thể gắn label `image/png` cho file HTML → lưu contentType png → browser respect png → KHÔNG execute (HTML vector chặn bởi declared Content-Type). SVG là vector thật.
- **(c) Pre-walk check:**
  ```bash
  printf '<svg xmlns="http://www.w3.org/2000/svg"><script>alert(1)</script></svg>' > /tmp/xss.svg
  curl -s -i -H "Authorization: Bearer $OWNER_JWT" -H "Host: <tenant-slug>.localhost" \
    -F "logo=@/tmp/xss.svg;type=image/svg+xml" http://localhost:9000/api/v1/settings/branding/logo | head
  # Nếu 200 → lấy logoUrl presigned → curl -i <url> → kiểm tra Content-Type + Content-Disposition (inline?)
  grep -n "svg" kiteclass/kiteclass-core/src/main/java/com/kiteclass/core/module/settings/service/BrandingServiceImpl.java
  ```

### FM-4 🟠 BrandingPackageController IDOR latent — `Long instanceId`, KHÔNG `@PreAuthorize`, KHÔNG tenant check
- **(a) Where:** `BrandingPackageController.java:34-37` `@GetMapping("/{instanceId}/package")` `@PathVariable Long instanceId` — KHÔNG có `@PreAuthorize`, KHÔNG có `assertCurrentTenant` (khác `BrandingVersionController.java:51` có check). `BrandingPackageServiceImpl.getByInstanceId():30-31` → `instanceRepository.findById(instanceId)` (FrontendInstance numeric PK) → scope resource theo `instance.getInstanceId()` của CHÍNH instance đó.
- **(b) Symptom:** Nếu reachable (hiện KHÔNG do FM-1 shadow), bất kỳ caller enumerate `Long instanceId` (1,2,3...) → đọc branding package của tenant khác → cross-tenant IDOR (recurrence class GAP-1015/1019/1023/1031). Thêm: **inconsistency Long vs UUID** — version controller dùng `UUID instanceId`, package controller dùng `Long instanceId` (FrontendInstance PK) → 2 endpoint cùng prefix `/api/v1/branding/{instanceId}/*` nhưng kiểu khác nhau → confuse + nếu routing fix sau này, IDOR sẽ live.
- **(c) Pre-walk check:**
  ```bash
  grep -nE "@PreAuthorize|assertCurrentTenant|PathVariable (Long|UUID)" \
    kiteclass/kiteclass-core/src/main/java/com/kiteclass/core/module/branding/controller/BrandingPackageController.java \
    kiteclass/kiteclass-core/src/main/java/com/kiteclass/core/module/settings/controller/BrandingVersionController.java
  # Latent đến khi FM-1 routing được sửa; ghi vào gap để fix CÙNG khi mở route.
  ```

### FM-5 🟠 BrandingVersionController IDOR soft-spot — `assertCurrentTenant` chỉ chặn khi `current != null`
- **(a) Where:** `BrandingVersionController.java:71-76` `assertCurrentTenant(UUID instanceId)`: `if (current != null && !current.equals(instanceId)) throw`. Nếu `TenantContext.getCurrentTenant()` = null (X-Tenant-Id absent — xem FM-2) → KHÔNG throw → `listVersions(instanceId)` / `rollback(instanceId, n)` chạy với instanceId từ path (chỉ còn Hibernate `tenantFilter` + `@PreAuthorize` làm rào).
- **(b) Symptom:** Defense-in-depth yếu: tin tuyệt đối gateway luôn set X-Tenant-Id. Nếu một route nào đó bỏ TenantResolver (public token paths, landing) HOẶC tenant header bị strip mà chưa re-inject → null context → IDOR mở. Hiện shadowed bởi FM-1 nên moot, nhưng nếu mở route phải vá `current == null` → reject thay vì pass.
- **(c) Pre-walk check:**
  ```bash
  sed -n '71,76p' kiteclass/kiteclass-core/src/main/java/com/kiteclass/core/module/settings/controller/BrandingVersionController.java
  # Verdict: latent; fix cùng FM-1 routing + FM-4.
  ```

### FM-6 🟡 KHÔNG có approval workflow — "→ approval" là misnomer; updateBranding apply + snapshot NGAY, rollback apply NGAY
- **(a) Where:** `BrandingServiceImpl.updateBranding():139-144` save branding RỒI `brandingVersionService.snapshot(branding, null)` → version mới active ngay. `BrandingVersionServiceImpl.rollback():81-90` restore + tạo version mới active ngay. KHÔNG có trạng thái PENDING/APPROVED. (Lưu ý: `V34__create_rebrand_approvals_table.sql` tồn tại nhưng KHÔNG được wire vào BrandingService — orphan table?)
- **(b) Symptom:** Walker kỳ vọng luồng "submit → pending → approve → apply" sẽ KHÔNG tìm thấy. Thực tế: mọi PUT `/settings/branding` apply tức thì + auto-version; "rollback" CHÍNH LÀ cơ chế apply lại version cũ. Đừng coi thiếu approval-gate là bug — đó là design Wave 4 MVP (`BrandingVersionService.java:13-21` "Out of scope: automated rollback triggers"). Verify xem `rebrand_approvals` table (V34) có consumer nào không.
- **(c) Pre-walk check:**
  ```bash
  grep -rn "rebrand_approval\|RebrandApproval\|PENDING\|APPROVED" kiteclass/kiteclass-core/src/main/java/com/kiteclass/core/module/ | head
  # Nếu 0 consumer → V34 orphan table; ghi note (không block walk).
  ```

### FM-7 🟡 `PUT /api/v1/settings/branding` KHÔNG có `@PreAuthorize` method-level — mọi authenticated tenant user PUT được
- **(a) Where:** `BrandingController.java:54-59` `updateBranding` chỉ có javadoc "Requires admin role" (`:49`) NHƯNG KHÔNG `@PreAuthorize`. `SecurityConfig.java:53` `.anyRequest().permitAll()` URL-layer → không có rào method → bất kỳ user có tenant context (kể cả non-OWNER: teacher/staff) PUT được branding tenant.
- **(b) Symptom:** Walk như TEACHER (không phải OWNER) → PUT `/settings/branding` đổi màu/tagline → 200 (không 403). Broken Access Control A01 — branding nên OWNER/ADMIN-only. So sánh: version endpoints CÓ `@PreAuthorize` (`BrandingVersionController.java:47,61`) nhưng wizard chính thì KHÔNG. Logo/favicon upload (`:74,92`) cũng thiếu `@PreAuthorize`.
- **(c) Pre-walk check:**
  ```bash
  grep -nE "@PreAuthorize|@PutMapping|@PostMapping" \
    kiteclass/kiteclass-core/src/main/java/com/kiteclass/core/module/settings/controller/BrandingController.java
  # Walk: login persona NON-owner → PUT /settings/branding → expect 403 (likely 200 = bug A01).
  ```

### FM-8 🟡 rollback version không tồn tại → IllegalArgumentException → global handler có thể 400 (không 404); no branding row → 500
- **(a) Where:** `BrandingVersionServiceImpl.rollback():83-90` — version không tồn tại → `IllegalArgumentException` (`:84`); không có branding row → `IllegalStateException` (`:89`); serialize fail → `IllegalStateException` (`:116`). `BrandingVersionController` KHÔNG có local `@ExceptionHandler` → phụ thuộc global handler kiteclass-core.
- **(b) Symptom:** Rollback tới versionNumber vô lý → kỳ vọng 404 nhưng global handler nhiều khả năng map `IllegalArgumentException` → 400. `IllegalStateException` (no branding row) → có thể 500 (leak). (Shadowed bởi FM-1 — chỉ test được qua direct curl). Verify mapping global handler.
- **(c) Pre-walk check:**
  ```bash
  grep -rn "IllegalArgumentException\|IllegalStateException\|@RestControllerAdvice\|@ExceptionHandler" \
    kiteclass/kiteclass-core/src/main/java/com/kiteclass/core/common/exception/ 2>/dev/null | head
  ```

### FM-9 🟡 BrandingVersion.snapshot_json JSONB — ĐÃ AN TOÀN qua `@JdbcTypeCode(SqlTypes.JSON)` (đừng chase)
- **(a) Where:** `entity/BrandingVersion.java:66-68` `@Column(columnDefinition = "jsonb") @JdbcTypeCode(SqlTypes.JSON) private String snapshotJson`. Migration `V43__create_branding_versions.sql` `snapshot_json JSONB NOT NULL`. JDBC URL (`application.yml:26`) KHÔNG có `stringtype=unspecified` — nhưng GAP-220 đã vá bằng `@JdbcTypeCode(SqlTypes.JSON)` → Hibernate 6.6 bind JSON đúng, KHÔNG còn SQLState 42804.
- **(b) Symptom:** PUT `/settings/branding` → `snapshot()` insert BrandingVersion → KHÔNG 500 (binding đúng). `Branding.theme_config_json` là `TEXT` (`entity/Branding.java:68`) → cũng an toàn. **Đây là note để walker KHÔNG nhầm 0-error là may mắn** — binding class (INET/JSONB per `postgres-specific-type-testcontainers.md`) đã được vá ở entity này. Verify schema khớp.
- **(c) Pre-walk check:**
  ```bash
  docker exec kite-postgres psql -U <user> -d <tenant-db> -c "\d branding_versions" -c "\d branding"
  grep -n "JdbcTypeCode\|columnDefinition" kiteclass/kiteclass-core/src/main/java/com/kiteclass/core/module/settings/entity/BrandingVersion.java
  ```

### FM-10 🟢 InternalWebhookController `/internal/notify/instance-deployed` — GATED đúng (contrast KH-10 GAP-1031)
- **(a) Where:** `InternalWebhookController.java:26,38` `/internal/notify` + `/instance-deployed`. `InternalRequestFilter.java:47` `@Order(1)` + `:104-115` chặn mọi `/internal/**` thiếu HMAC-SHA256 signature (constant-time compare `:151`, 5-min replay window `:163-173`, fail-fast secret validation `:87-93`). Gateway KHÔNG có route `/internal/**` (vắng trong route list `application.yml`) → KHÔNG expose ra ngoài.
- **(b) Symptom:** KHÔNG reachable qua gateway; nếu reach nội bộ phải có HMAC. An toàn — KHÁC KH-10 email hole (GAP-1031). Side-effect chỉ là cache evict (`packageProxy.evict()`), low impact. Walker KHÔNG cần test (không qua gateway).
- **(c) Pre-walk check:**
  ```bash
  grep -nE "internal|/internal" kitehub/kitehub-gateway/src/main/resources/application.yml   # expect 0 route
  curl -s -o /dev/null -w "%{http_code}\n" -X POST "http://localhost:9000/internal/notify/instance-deployed?instanceId=1"  # expect 404/403
  ```

### FM-11 🟢 PublicBranding enumeration — chỉ leak public theme fields (intended), shadowed bởi FM-1
- **(a) Where:** `PublicBrandingController.java:43-53` `GET /api/v1/branding/public?tenantId=X` unauthenticated, resolve UUID hoặc slug (`:55-76`), trả `toPublicPayload()` (`:78-87`) CHỈ displayName/logoUrl/primaryColor/secondaryColor/accentColor/tagline — KHÔNG leak contact/social (javadoc `:26-28`). KHÔNG có Hibernate tenantFilter (public path không set X-Tenant-Id) → `findByInstanceIdAndDeletedFalse(tenantUuid)` trả branding theo param.
- **(b) Symptom:** Enumerable cross-tenant theme (login page cần vậy — by design). Chỉ public fields → low sensitivity. Hiện shadowed bởi FM-1 nên FE login page KiteClass KHÔNG nhận được payload này → render default. Walker: nếu sửa FM-1 → endpoint live + enumeration acceptable.
- **(c) Pre-walk check:** verify `toPublicPayload()` KHÔNG thêm field nhạy cảm; confirm login page fallback default khi shadowed.

---

## Tóm tắt cho walker

| # | Severity | 1-dòng | Loại |
|---|---|---|---|
| FM-1 | 🔴 | `/api/v1/branding/**` → kitehub-branding shadows public/version/rollback/package KC-10 | Routing walk-blocker |
| FM-2 | 🟠 | `/settings/branding` cần Host→TenantResolver; null context → silent default-branding | Auth/tenant binding |
| FM-3 | 🟠 | SVG XSS — svg+xml allowed, MIME từ client header (no magic-byte sniff) | File-upload security |
| FM-4 | 🟠 | BrandingPackageController IDOR latent (Long id, no @PreAuthorize/tenant check) | Trust-boundary (latent) |
| FM-5 | 🟠 | BrandingVersionController `assertCurrentTenant` chỉ chặn khi current != null | Trust-boundary soft-spot |
| FM-6 | 🟡 | KHÔNG có approval workflow — update apply ngay; rollback = apply; "→approval" misnomer | State machine clarify |
| FM-7 | 🟡 | PUT/logo/favicon `/settings/branding` thiếu @PreAuthorize → non-owner PUT được | A01 access control |
| FM-8 | 🟡 | rollback bad version → 400 không 404; no branding row → 500 | Validation (latent) |
| FM-9 | 🟡 | snapshot_json JSONB ĐÃ vá @JdbcTypeCode — đừng chase 0-error là bug | Schema ✅ verified-safe |
| FM-10 | 🟢 | InternalWebhook HMAC-gated + no gateway route — secure (khác KH-10) | Internal ✅ |
| FM-11 | 🟢 | PublicBranding enumerable nhưng chỉ public theme fields | Info |

**Pre-walk MUST-run trước khi mở flow (quyết định walk chạy được không + đâu là risk thật):**

1. **FM-1 routing** — `grep -nE "id: kitehub-branding-v1|Path=/api/v1/branding|id: instance-apis" kitehub/kitehub-gateway/src/main/resources/application.yml:593` + `curl -i http://localhost:9000/api/v1/branding/public?tenantId=<uuid>` so với direct `http://localhost:8080/...`. **Quyết định: version/rollback/public/package có walk được qua gateway không (KHÔNG — phải direct curl kiteclass-core:8080).** Đây là blocker lớn nhất.
2. **FM-2 tenant Host** — `curl -i -H "Host:<slug>.localhost" -H "Authorization: Bearer $OWNER_JWT" http://localhost:9000/api/v1/settings/branding`. **Quyết định: wizard chính (`/settings/branding`) walk được không — expect 200 + tenant branding, NOT 400/silent-default** (`BrandingServiceImpl.java:95,112` đọc TenantContext).
3. **FM-7 + FM-3** — `grep -nE "@PreAuthorize" .../settings/controller/BrandingController.java` (expect 0 → A01 bug) + thử upload `/tmp/xss.svg` type `image/svg+xml` (`BrandingServiceImpl.java:47-49` ALLOWED_CONTENT_TYPES). **Quyết định severity thật: non-owner PUT được branding? SVG XSS execute trên presigned URL?**

3 cái này quyết định: (a) sub-flow nào walk được qua gateway vs phải direct-curl, (b) wizard chính có chạy với đúng tenant không, (c) đâu là security bug thật (A01 + SVG) vs latent (IDOR shadowed).
