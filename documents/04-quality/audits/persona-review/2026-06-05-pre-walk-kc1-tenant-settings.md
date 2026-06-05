# Pre-walk persona simulation — KC-1 Tenant provisioning + settings (2026-06-05)

> Audit type: pre-walk static analysis (read-only). Stack KHÔNG chạy. Mục tiêu: dự đoán failure mode mà Owner persona sẽ gặp TRƯỚC khi walk thật trên local Docker stack, per `.claude/rules/pre-walk-persona-simulation-mandate.md` §1.
>
> Persona: Owner trung tâm, vừa xong onboarding wizard (KH-2c), non-technical, nói tiếng Việt. Tâm lý: "Tôi vừa làm xong wizard — giờ vào settings thấy gì? Dữ liệu của tôi có đó không? Sửa được không?"

## Scope inspected

### BE files (kiteclass-core)
- `kiteclass/kiteclass-core/src/main/java/com/kiteclass/core/module/settings/controller/BrandingController.java` — `@RequestMapping("/api/v1/settings/branding")`, GET/PUT/POST logo/favicon/theme
- `kiteclass/kiteclass-core/src/main/java/com/kiteclass/core/module/settings/controller/UserPreferencesController.java` — preferences endpoint
- `kiteclass/kiteclass-core/src/main/java/com/kiteclass/core/module/settings/service/BrandingServiceImpl.java` — `getBranding()` line 94-98, `createDefaultBranding()` line 267-276
- `kiteclass/kiteclass-core/src/main/java/com/kiteclass/core/module/settings/entity/Branding.java` — entity columns (line 43-64)
- `kiteclass/kiteclass-core/src/main/java/com/kiteclass/core/module/provisioning/TenantProvisioningSaga.java` — `provisionInfrastructure()` line 83 (placeholder)
- `kiteclass/kiteclass-core/src/main/java/com/kiteclass/core/config/TenantFilterInterceptor.java` — resolves `X-Tenant-Id` header → `TenantContext` (line 77-82)
- `kiteclass/kiteclass-core/src/main/java/com/kiteclass/core/config/SecurityConfig.java` — `.anyRequest().permitAll()` (line 49)
- `kitehub/kitehub-gateway/src/main/resources/application.yml` — route `instance-apis` line 722-728 (`Path=/api/v1/**` → kiteclass-core via `TenantResolver` filter)

### FE files (kiteclass-frontend)
- `kiteclass/kiteclass-frontend/src/app/(dashboard)/settings/page.tsx` — Tabbed page (Branding / Theme preview / Tùy chọn)
- `kiteclass/kiteclass-frontend/src/components/settings/branding-settings.tsx`
- `kiteclass/kiteclass-frontend/src/components/settings/preferences-settings.tsx`
- `kiteclass/kiteclass-frontend/src/lib/api/branding.ts` — `BASE_URL = '/api/v1/settings/branding'`
- `kiteclass/kiteclass-frontend/src/lib/api-client.ts` — request interceptor gắn `Authorization` + `X-Tenant-Id` (line 82-95)
- `kiteclass/kiteclass-frontend/src/lib/auth/jwt-storage.ts` — `getTenantId()` line 75-78 (đọc từ `sessionStorage`)

### Business docs
- `documents/01-business/kiteclass/tenant-settings/api-contract.md` — KHÔNG có `/tenant/settings`; chỉ branding + user-preferences + landing
- `documents/01-business/kiteclass/tenant-settings/rules.md` — BR-SET-01..18 (chỉ branding); BR-SET-02 "default branding KHÔNG persist"
- `documents/01-business/kiteclass/tenant-settings/use-cases.md` — UC-TNT-01..06
- `documents/01-business/kiteclass/tenant-provisioning/rules.md` — BR-PROV-020 "provisionInfrastructure là placeholder (chỉ log)"

---

## Predicted failure modes (8)

### FM-1: Endpoint `/api/v1/tenant/settings` KHÔNG TỒN TẠI — walk check (b) sẽ 404

- **(a) WHERE:** Không có controller nào map `/api/v1/tenant/settings`. Controller thật là `BrandingController.java:27` (`/api/v1/settings/branding`) + `UserPreferencesController` (`/api/v1/users/{userId}/preferences`) + `LandingPageController` (`/api/v1/tenants/{tenantId}/landing`). Settings là 3 concern RỜI NHAU, không có unified endpoint.
- **(b) SYMPTOM:** Owner/walker gọi `GET /api/v1/tenant/settings` → 404 (route catch-all `/api/v1/**` forward tới kiteclass-core, nhưng kiteclass-core không có handler → Spring 404). Walk check (b) "returns 200 + full JSON" FAIL ngay.
- **(c) PRE-WALK CHECK:**
  ```bash
  grep -rn '"/api/v1/tenant/settings"\|tenant/settings' kiteclass/kiteclass-core/src/main/java --include=*.java
  # Expected: 0 hits → endpoint không tồn tại → walk dùng SAI path
  # Đúng path: curl http://localhost:8080/api/v1/settings/branding (qua gateway, cần X-Tenant-Id)
  ```
- **SEVERITY: P0 (blocks walk)** — toàn bộ premise của flow (unified tenant-settings) không match code. Walk phải re-scope sang 3 endpoint rời (branding + preferences + landing).

### FM-2: KHÔNG có DB tenant-settings row được seed lúc onboarding — walk check (a) sẽ thấy bảng rỗng

- **(a) WHERE:** `TenantProvisioningSaga.provisionInfrastructure()` line 83 là placeholder — javadoc line 23 ghi rõ "DB schema, MinIO bucket, DNS — placeholder (logs only)". `rules.md` BR-PROV-020 xác nhận. KHÔNG có code seed branding/settings/academic-year lúc tạo tenant.
- **(b) SYMPTOM:** Owner vừa xong wizard, vào settings → BE `getBranding()` (`BrandingServiceImpl.java:94`) tìm `findByInstanceIdAndDeletedFalse(instanceId)` → KHÔNG có row → `orElseGet(createDefaultBranding)` trả về object in-memory (line 98). Walk check (a) "DB tenant settings row exists with all default fields" FAIL — bảng `branding` rỗng cho tới khi Owner PUT lần đầu (BR-SET-02: "default KHÔNG persist cho tới khi update").
- **(c) PRE-WALK CHECK:**
  ```bash
  # Sau khi tạo tenant qua onboarding, kiểm tra DB:
  docker exec kite-postgres psql -U kite -d kiteclass -c \
    "SELECT instance_id, display_name FROM branding;"
  # Expected: 0 rows cho tenant mới → check (a) sai giả định (default in-memory, not persisted)
  ```
- **SEVERITY: P1 (blocks G1 PASS)** — check (a) như viết sẽ luôn FAIL cho tenant fresh. Walk phải re-phrase: "GET trả default in-memory" (đúng BR-SET-02), KHÔNG phải "DB row exists".

### FM-3: Defaults về academic year / week Mon-Sat / locale vi-VN / currency VND KHÔNG TỒN TẠI trong settings

- **(a) WHERE:** `Branding.java:43-64` chỉ có columns: `displayName`, `tagline`, `primaryColor`, `secondaryColor`, `accentColor` (+ contact/social ở phần dưới). KHÔNG có `academicYear`, `weekStart`, `locale`, `currency`. Academic year là module RIÊNG (`module/academicyear/entity/AcademicYear.java`), không thuộc tenant-settings. Currency/locale/week config không tồn tại ở đâu trong settings scope.
- **(b) SYMPTOM:** Owner kỳ vọng (theo flow premise) thấy "academic year 2025-2026, week Mon-Sat, locale vi-VN, currency VND". Thực tế settings page chỉ có Branding (tên/tagline/màu) + Theme preview + Tùy chọn (language/theme/notification của USER, không phải tenant). Owner bối rối: "Niên khóa tôi chọn ở wizard đâu? Cài đặt tuần học đâu?"
- **(c) PRE-WALK CHECK:**
  ```bash
  grep -niE "academicYear|weekStart|currency|locale|timezone" \
    kiteclass/kiteclass-core/src/main/java/com/kiteclass/core/module/settings/entity/Branding.java
  # Expected: 0 hits → các field này KHÔNG ở settings → flow premise quá rộng so với code
  ```
- **SEVERITY: P1 (blocks G1 PASS)** — check (a) "all default fields reasonable" giả định fields không tồn tại. G1 phải giới hạn scope về đúng những gì Branding entity có (displayName + 3 màu + tagline).

### FM-4: FE `X-Tenant-Id` đọc từ `sessionStorage` — nếu onboarding không set → BE 400/tenant filter không kích hoạt

- **(a) WHERE:** `api-client.ts:92-94` gắn `X-Tenant-Id` từ `getTenantId()` (`jwt-storage.ts:75` đọc `sessionStorage.getItem(TENANT_ID_KEY)`). Nếu sau onboarding wizard, `sessionStorage` chưa có `tenantId` (vd onboarding ở KiteHub FE domain khác, hoặc redirect sang KiteClass FE chưa set), header sẽ vắng.
- **(b) SYMPTOM:** Request `GET /api/v1/settings/branding` thiếu `X-Tenant-Id` → `TenantFilterInterceptor.java:99` log "No X-Tenant-Id header, tenant filter not enabled" → `TenantContext.getCurrentTenant()` trong `BrandingServiceImpl:95` trả null/throw → 500 hoặc cross-tenant leak. Owner thấy settings page lỗi tải (`isError`) hoặc spinner mãi.
- **(c) PRE-WALK CHECK:**
  ```bash
  # Trong browser DevTools sau onboarding, Console:
  #   sessionStorage.getItem('kiteclass_tenant_id')   (verify key name trong jwt-storage.ts)
  grep -n "TENANT_ID_KEY" kiteclass/kiteclass-frontend/src/lib/auth/jwt-storage.ts
  # Walk: mở Network tab → request settings/branding → verify header X-Tenant-Id present
  ```
- **SEVERITY: P0 (blocks walk)** — đây là contract-drift cross-domain (KiteHub onboarding → KiteClass settings). Nếu tenant context không bắc cầu, mọi tenant-scoped call fail. Đây là recurring bug class (GAP-920 api-contract drift).

### FM-5: Gateway `TenantResolver` filter có thể 400 nếu không resolve được tenant từ subdomain/header

- **(a) WHERE:** Gateway route `instance-apis` (`application.yml:722-728`) áp `TenantResolver` filter cho mọi `/api/v1/**`. Comment line 686-688 (route `public-tenant-landing`) ghi rõ `TenantResolver` "would 400 on localhost/SSR with no subdomain". Walk trên local Docker (`localhost:8080`) không có subdomain.
- **(b) SYMPTOM:** Trên local, gọi `/api/v1/settings/branding` qua gateway → `TenantResolver` cố resolve tenant từ Host subdomain → localhost không có subdomain → 400 trước khi tới kiteclass-core. Owner/walker thấy 400 "tenant không xác định" dù JWT hợp lệ.
- **(c) PRE-WALK CHECK:**
  ```bash
  # Đọc TenantResolver filter logic — nó fallback X-Tenant-Id header hay chỉ subdomain?
  grep -rln "TenantResolver" kitehub/kitehub-gateway/src/main/java
  grep -rn "subdomain\|X-Tenant-Id\|400\|Host" \
    $(grep -rln "class TenantResolver" kitehub/kitehub-gateway/src/main/java)
  # Verify: filter có chấp nhận X-Tenant-Id header khi không có subdomain không?
  ```
- **SEVERITY: P0 (blocks walk)** — nếu `TenantResolver` chỉ resolve qua subdomain, mọi local walk qua gateway sẽ 400. Phải hoặc (a) walk trực tiếp kiteclass-core:8080 bypass gateway, hoặc (b) set Host header subdomain giả.

### FM-6: Walk check (d) "PATCH 1 field" — BrandingController dùng PUT, không phải PATCH; semantics partial-update ở body

- **(a) WHERE:** `BrandingController.java:54` là `@PutMapping` (PUT), không có `@PatchMapping`. `rules.md` BR-SET-14 ghi "PATCH — only provided fields updated" nhưng đó là PATCH-semantics qua PUT (BrandingMapper bỏ qua null field), KHÔNG phải HTTP PATCH verb.
- **(b) SYMPTOM:** Walker gọi `PATCH /api/v1/settings/branding` (theo check (d) literal) → 405 Method Not Allowed (chỉ PUT registered). Nếu gọi PUT với chỉ 1 field `displayName`, các field required khác (`primaryColor` nullable=false) trong `UpdateBrandingRequest` có thể fail validation → 400 nếu request không gửi đủ màu.
- **(c) PRE-WALK CHECK:**
  ```bash
  grep -n "@PutMapping\|@PatchMapping" \
    kiteclass/kiteclass-core/src/main/java/com/kiteclass/core/module/settings/controller/BrandingController.java
  # Đọc UpdateBrandingRequest: field nào @NotBlank/required?
  grep -n "@NotBlank\|@NotNull\|required" \
    kiteclass/kiteclass-core/src/main/java/com/kiteclass/core/module/settings/dto/request/UpdateBrandingRequest.java
  ```
- **SEVERITY: P1 (blocks G1 PASS)** — check (d) "PATCH 1 field returns 200" sai verb. Walk phải dùng PUT + gửi full valid body (hoặc verify partial-update semantics qua PUT). Nếu chỉ gửi displayName mà thiếu required color → 400 thay vì 200+persist.

### FM-7: FE settings page tab "Tùy chọn" gọi user preferences cần `userId` — có thể chưa resolve cho Owner fresh

- **(a) WHERE:** `settings/page.tsx:91` render `<PreferencesSettings />` → `preferences-settings.tsx:54` dùng `usePreferences()` hook → gọi `/api/v1/users/{userId}/preferences`. `use-preferences.ts` cần userId. UC-TNT-05 ghi "preferences record có thể chưa tồn tại" → cần `POST .../initialize` trước. Owner vừa tạo có thể chưa có preferences row.
- **(b) SYMPTOM:** Owner click tab "Tùy chọn" → `usePreferences` → 404 PREFERENCES_NOT_FOUND (UC-TNT-04 error) → `isError` → tab hiển thị lỗi thay vì form. Owner thấy "không tải được tùy chọn".
- **(c) PRE-WALK CHECK:**
  ```bash
  grep -n "userId\|initialize\|/preferences" kiteclass/kiteclass-frontend/src/lib/api/preferences.ts
  grep -n "PREFERENCES_NOT_FOUND\|initialize\|orElse" \
    kiteclass/kiteclass-core/src/main/java/com/kiteclass/core/module/settings/controller/UserPreferencesController.java
  # Verify: preferences auto-init lúc first GET, hay cần POST initialize riêng?
  ```
- **SEVERITY: P2 (cosmetic/secondary tab)** — không block check (a)-(d) chính (branding), nhưng nếu walk mở tab Tùy chọn sẽ thấy lỗi. Owner mất niềm tin nếu 1 trong 3 tab lỗi.

### FM-8: SecurityConfig `.anyRequest().permitAll()` — Owner KHÔNG bị chặn, nhưng GET branding public ⇒ check role không được verify

- **(a) WHERE:** `SecurityConfig.java:49` `.anyRequest().permitAll()` — kiteclass-core TIN gateway đã auth (line 45 comment "Gateway handles authentication, Core trusts X-User-Id/X-User-Roles headers"). BrandingController GET (line 41) javadoc ghi "Public endpoint (no authentication required)". PUT (line 54) ghi "Requires admin role" NHƯNG không có `@PreAuthorize` — chỉ dựa header trust.
- **(b) SYMPTOM:** Walk sẽ PASS check (b) GET 200 (public OK). NHƯNG nếu walk muốn verify "Owner role enforced cho PUT", sẽ thấy KHÔNG có guard ở core — bất kỳ request nào tới core với header đúng đều PUT được. Đây không phải lỗi Owner gặp khi walk happy-path, nhưng là gap an ninh (OWASP A01 per `pre-launch-owasp-rest-hardening-checklist.md` §2.1: thiếu `@PreAuthorize`).
- **(c) PRE-WALK CHECK:**
  ```bash
  grep -n "@PreAuthorize\|@Secured\|hasRole\|ADMIN" \
    kiteclass/kiteclass-core/src/main/java/com/kiteclass/core/module/settings/controller/BrandingController.java
  # Expected: 0 hits → no per-resource authz ở core (relies on gateway). Walk happy-path PASS,
  # nhưng note security gap cho follow-up gap.
  ```
- **SEVERITY: P2 (không block walk happy-path; security follow-up)** — Owner walk sẽ qua, nhưng surface authz-gap cần file gap riêng.

---

## Summary table

| FM | Title | Layer | Severity |
|----|-------|-------|----------|
| FM-1 | `/api/v1/tenant/settings` không tồn tại (đúng: `/settings/branding`) | BE controller / contract | P0 |
| FM-2 | Không seed DB settings row lúc onboarding (provisionInfrastructure placeholder) | BE saga / persistence | P1 |
| FM-3 | Academic year / week / locale / currency không thuộc settings entity | BE entity / scope | P1 |
| FM-4 | FE `X-Tenant-Id` từ sessionStorage có thể vắng sau onboarding cross-domain | FE→BE contract drift | P0 |
| FM-5 | Gateway `TenantResolver` 400 trên localhost (no subdomain) | Gateway filter | P0 |
| FM-6 | Walk dùng PATCH nhưng controller chỉ có PUT; partial-update là body-semantics | BE controller / verb | P1 |
| FM-7 | Tab "Tùy chọn" cần userId preferences có thể chưa init → 404 | FE/BE preferences | P2 |
| FM-8 | Không `@PreAuthorize` ở core (trust gateway header) — authz gap | BE security | P2 |

---

## Top-3 to verify first

1. **FM-1 + FM-3 (re-scope premise) — P0/P1.** Toàn bộ flow premise "unified tenant-settings với academic year + locale + currency + week" KHÔNG match code. Thực tế: settings = 3 concern rời (Branding entity 5 field màu/tên + UserPreferences + LandingPage). **Trước khi walk, coordinator/user phải re-scope G1 checks (a)-(d) về đúng `/api/v1/settings/branding` + Branding entity columns thật.** Đây là gap lớn nhất — walk theo premise hiện tại sẽ FAIL 100% vì test sai endpoint + sai field.

2. **FM-5 (gateway TenantResolver localhost 400) — P0.** Nếu walk qua gateway trên `localhost:8080`, `TenantResolver` filter có thể 400 do không có subdomain. Verify TenantResolver fallback X-Tenant-Id header trước khi walk; nếu không, walk trực tiếp `kiteclass-core:8080` bypass gateway HOẶC set Host subdomain giả. Quyết định route TRƯỚC walk.

3. **FM-4 (X-Tenant-Id sessionStorage vắng sau onboarding) — P0.** Verify sau onboarding wizard, KiteClass FE `sessionStorage` đã có `tenantId` chưa. Đây là cross-domain handoff (KiteHub onboarding → KiteClass settings) — recurring contract-drift class (GAP-920). Nếu vắng, mọi tenant-scoped call (gồm settings) fail ngay từ request đầu.

---

## Khuyến nghị cho walk session

- **Re-scope G1 trước khi walk** (per FM-1/FM-3): đổi check (b) thành `GET /api/v1/settings/branding`, check (a) thành "GET trả default in-memory đúng BR-SET-02 (displayName='KiteClass', 3 màu)", bỏ academic-year/locale/currency/week khỏi scope KC-1 (chúng thuộc module khác — file gap riêng nếu Owner thực sự cần chúng ở settings).
- **Chọn route path trước** (per FM-5): gateway-with-subdomain vs direct-core. Document trong G2 recipe.
- **Verify tenant-context bridge** (per FM-4) ngay bước đầu walk: Network tab → confirm `X-Tenant-Id` present.
- 5/8 FM (FM-1/2/3/4/6) sẽ làm walk theo premise hiện tại FAIL ngay → fix scope/route TRƯỚC khi walk tiết kiệm ~1 vòng walk-rebuild.
