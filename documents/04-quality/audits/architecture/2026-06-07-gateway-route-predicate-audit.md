# Gateway Route-Predicate Audit — toàn bộ routing collision map (GAP-1042)

**Ngày:** 2026-06-07
**Loại:** Architecture audit — READ-ONLY (không sửa code)
**Phạm vi:** `kitehub/kitehub-gateway/src/main/resources/application.yml` (Spring Cloud Gateway) ↔ toàn bộ controller `@RequestMapping` trong 5 service đích (kitehub-subscription, kiteclass-core, kitehub-branding, kitehub-admin, kitehub-email)
**Trigger:** Flow Verification Campaign G3 production-parity. Routing collision lặp ≥3 lần (GAP-1034 KC-10 branding shadow, GAP-1041 KC-12 payroll). Audit 1 lần toàn bộ thay vì per-flow.
**Cơ chế routing xác nhận:** Spring Cloud Gateway thuần — route matching theo **declaration order** (route đầu tiên khớp predicate thắng). Không có Nginx/custom filter routing.

---

## 1. Tóm tắt (Executive Summary)

Gateway có **56 route** (script đếm), map tới 5 backend service. Routing dựa trên 3 nhóm catch-all rộng đặt CUỐI, với các route hẹp (specific path) carve-out đặt TRƯỚC theo đúng order semantics:

| Catch-all (đặt cuối nhóm) | Predicate | Target service |
|---|---|---|
| `instance-apis` | `Path=/api/v1/**` | `kiteclass-core` (+ TenantResolver) |
| `kitehub-admin-v1` | `Path=/api/v1/admin/**` | `kitehub-admin` |
| `kitehub-branding-v1` | `Path=/api/v1/branding/**` | `kitehub-branding` |
| `platform-admin` | `Path=/api/platform/admin/**` | `kitehub-admin` |

**Kết quả:** phát hiện **5 collision** (tất cả cùng class "controller bị shadow bởi catch-all sai service" HOẶC "public endpoint kẹt sau TenantResolver"):

| # | Endpoint | Service thật | Bị route sai về | Severity | Audit script bắt được? |
|---|---|---|---|:---:|:---:|
| C1 | `/api/platform/admin/payments/**` | kitehub-subscription | kitehub-admin (404) | P1 | ✅ CÓ (WRONG_SERVICE) |
| C2 | `/api/v1/preferences/dismiss-banner-state` | kitehub-subscription | kiteclass-core (404/400) | P2 | ✅ CÓ (WRONG_SERVICE) |
| C3 | `/api/v1/admin/parent/consent/**` | kiteclass-core | kitehub-admin (404) | P1 | ❌ KHÔNG (blind spot) |
| C4 | `/api/v1/payments/webhook/**` | kiteclass-core | TenantResolver 400 | P1 | ❌ KHÔNG (blind spot) |
| C5 | `/api/v1/parent-invitations/redeem/{token}` | kiteclass-core | TenantResolver 400 | P1 | ❌ KHÔNG (blind spot) |

**GAP-1034 (branding) + GAP-1041 (payroll): root cause XÁC NHẬN + fix HIỆN CÓ và ĐÚNG.**

**META finding quan trọng nhất:** `scripts/audit-gateway-routes.sh` có **blind spot** — chỉ scan controller của 4 kitehub module, **KHÔNG scan kiteclass-core**. Đây là lý do GAP-1041 (payroll, kiteclass-core) phải tìm bằng manual walk thay vì detector. 3/5 collision (C3/C4/C5) hiện vô hình với detector. Xem §5.

---

## 2. Route table (theo declaration order — nhóm collision-relevant)

Chỉ liệt kê các route ảnh hưởng collision + nhóm catch-all. Auth/docs routes (rate-limit) liệt kê gọn cuối.

### 2.1 Nhóm `/api/platform/**` (KHÔNG có `/api/platform/**` catch-all — chỉ có catch-all con `/api/platform/admin/**`)

| Order | Route id | Predicate | StripPrefix/Filter | Target | Controller cover |
|---|---|---|---|---|---|
| ~ | `platform-instances` | `/api/platform/instances/**` | CircuitBreaker | subscription | InstanceController + TrialToPaidController `/instances/{id}/upgrade` ✓ |
| ~ | `kitehub-instance-domain-verify` | `/api/instances/**` | CircuitBreaker | subscription | DomainController `/api/instances/{id}/domain` ✓ |
| ~ | `platform-config` | `/api/platform/config/**` | CircuitBreaker | subscription | PublicConfigController ✓ |
| ~ | `platform-subscription` | `/api/platform/subscriptions/**` | CircuitBreaker | subscription | SubscriptionController ✓ |
| ~ | `platform-payment` | `/api/platform/payments/**` | CircuitBreaker | subscription | PaymentController ✓ |
| ~ | `platform-webhooks` | `/api/platform/webhooks/**` | CircuitBreaker | subscription | PaymentWebhookController + MigrationWebhookController ✓ |
| ~ | `platform-branding` | `/api/platform/branding/**` | RateLimiter+CB | **branding** | ContentGen/TemplateGallery/AssetStorage/BrandingJob/AIBranding ✓ |
| ~ | `platform-admin-emails-subscription` | `/api/platform/admin/emails/**` | CircuitBreaker | subscription | AdminEmailController ✓ |
| ~ | `platform-admin-instances-force-convert-subscription` | `/api/platform/admin/instances/{id}/force-convert` | CircuitBreaker | subscription | AdminMigrationController ✓ |
| ~ | `platform-admin-instances-rollback-migration-subscription` | `/api/platform/admin/instances/{id}/rollback-migration` | CircuitBreaker | subscription | AdminMigrationController ✓ |
| ~ | `platform-admin-instances-retry-provisioning-subscription` | `/api/platform/admin/instances/{id}/retry-provisioning` | CircuitBreaker | subscription | AdminTenantProvisioningController ✓ |
| **CUỐI** | `platform-admin` | `/api/platform/admin/**` | CircuitBreaker | **admin** | AdminController (`/dashboard,/instances,/revenue,/subscriptions`) ✓ — **NHƯNG shadow AdminPaymentController → C1** |

### 2.2 Nhóm `/api/v1/**` (catch-all `instance-apis` → kiteclass-core đặt CUỐI)

| Order | Route id | Predicate | Filter | Target | Ghi chú |
|---|---|---|---|---|---|
| ~ | `kitehub-auth-v1-*` (beta-access/2fa/beta-signup) | `/api/v1/auth/...` | RateLimiter+CB | subscription | carve ✓ |
| ~ | `kitehub-feedback-v1` | `/api/v1/feedback` | RateLimiter+CB | subscription | ✓ |
| ~ | `kitehub-auth-v1` | `/api/v1/auth/**` | CircuitBreaker | subscription | ✓ |
| ~ | `kitehub-admin-beta-requests-v1` | `/api/v1/admin/beta-requests/**` | CircuitBreaker | subscription | carve trước admin catch-all ✓ |
| ~ | `kitehub-admin-impersonate` | `/api/v1/admin/impersonate/**` | CircuitBreaker | subscription | carve ✓ (Wave 82 F4) |
| ~ | `kiteclass-payroll` | `/api/v1/admin/payroll/**` | **TenantResolver**+CB | **kiteclass-core** | **GAP-1041 fix ✓** |
| **(con)** | `kitehub-admin-v1` | `/api/v1/admin/**` | CircuitBreaker | **admin** | AdminRevenue/Instances/AuditLog/Payments ✓ — **NHƯNG shadow ParentConsentAdminController → C3** |
| ~ | `kitehub-consent-v1` | `/api/v1/consent/**` | CircuitBreaker | subscription | cover cả ImmutableConsent `/consent/v2` ✓ |
| ~ | `kitehub-dsar-v1` | `/api/v1/dsar/**` | CircuitBreaker | subscription | ✓ |
| ~ | `kitehub-notification-preferences-v1` | `/api/v1/notification-preferences/**` | CircuitBreaker | subscription | ✓ (KHÔNG khớp `/api/v1/preferences` → C2) |
| ~ | `kiteclass-branding-public` | `/api/v1/branding/public` | CB (skip TenantResolver) | kiteclass-core | **GAP-1034 fix ✓** |
| ~ | `kiteclass-branding-versions` | `/api/v1/branding/*/versions/**` | TenantResolver+CB | kiteclass-core | **GAP-1034 fix ✓** |
| ~ | `kiteclass-branding-package` | `/api/v1/branding/*/package` | TenantResolver+CB | kiteclass-core | **GAP-1034 fix ✓** |
| **(con)** | `kitehub-branding-v1` | `/api/v1/branding/**` | CircuitBreaker | **branding** | Wizard/Job/Preview/Quality/DeployStream/Lifecycle ✓ |
| ~ | `kitehub-beta-status` | `/api/v1/beta-status` | CircuitBreaker | subscription | ✓ |
| ~ | `staff-invitations-public-token` | `/api/v1/staff-invitations/by-token/**,/*/accept` | CB (skip TenantResolver) | subscription | **GAP-790 pattern ✓** |
| ~ | `staff-invitations` | `/api/v1/staff-invitations/**` | TenantResolver+CB | subscription | ✓ |
| ~ | `kitehub-onboarding-progress` | `/api/v1/onboarding-progress(/**)` | TenantResolver+CB | subscription | ✓ |
| ~ | `public-tenant-landing` | `/api/v1/tenants/*/landing` | CB (skip TenantResolver) | kiteclass-core | ✓ |
| ~ | `public-tenant-resolve` | `/api/v1/public/tenants/**` | RateLimiter+CB | subscription | ✓ |
| ~ | `kc-tenant-auth` | `/api/v1/tenant-auth/**` | RateLimiter+CB (skip TenantResolver) | kiteclass-core | ✓ (Wave auth-1) |
| **CUỐI** | `instance-apis` | `/api/v1/**` | **TenantResolver**+CB | **kiteclass-core** | catch-all — **shadow C4 (payment webhook) + C5 (parent redeem) qua TenantResolver 400** |

### 2.3 Default filters (áp dụng mọi route)

```
RemoveRequestHeader=X-Tenant-Id          # anti-spoof (GAP-814)
RemoveRequestHeader=X-User-Id
RemoveRequestHeader=X-User-Reference-Id  # Wave auth-1
DedupeResponseHeader / AddResponseHeader=X-Gateway-Version,1.0
```

Gateway là cơ quan DUY NHẤT set `X-Tenant-Id`/`X-User-Id`. `JwtAuthenticationGatewayFilter` re-inject `X-User-Id` từ JWT; `TenantHeaderGuardFilter` re-inject `X-Tenant-Id` từ JWT claim; `TenantResolver` set lại từ Host trên instance-api routes. **Hệ quả routing:** route nào SKIP TenantResolver mà controller cần tenant scope → phải tự derive tenant server-side; route nào dính TenantResolver mà request không có tenant context → **400 "Cannot resolve tenant"** (xác nhận tại `TenantResolverGatewayFilterFactory.java:101`).

---

## 3. Collision findings (chi tiết)

### C1 — `/api/platform/admin/payments/**` route sai service (P1)

- **Controller thật:** `AdminPaymentController` (kitehub-subscription) `@RequestMapping("/api/platform/admin/payments")` — `GET /pending`, `POST /{id}/confirm`, `POST /{id}/reject`.
- **Routing thực tế:** không có route hẹp → khớp `platform-admin` `/api/platform/admin/**` → **kitehub-admin**. kitehub-admin `AdminController` chỉ có `/dashboard,/instances,/revenue,/subscriptions` → **404**.
- **Ảnh hưởng:** xác nhận/từ chối thanh toán thủ công (SePay) ở admin DEAD. Phase 1.5 payment.
- **Fix:** thêm route `platform-admin-payments-subscription` `Path=/api/platform/admin/payments/**` → kitehub-subscription, đặt **TRƯỚC** `platform-admin` catch-all.

### C2 — `/api/v1/preferences/dismiss-banner-state` route sai service (P2)

- **Controller thật:** `PreferencesController` (kitehub-subscription) `@PostMapping("/api/v1/preferences/dismiss-banner-state")` (full path ở method, không có class-level @RequestMapping).
- **Routing thực tế:** không khớp `/api/v1/notification-preferences/**` → rơi xuống `instance-apis` `/api/v1/**` → **kiteclass-core** (+ TenantResolver). kiteclass-core không có handler `/api/v1/preferences` → 404 (hoặc 400 nếu thiếu tenant).
- **Ảnh hưởng:** lưu trạng thái dismiss banner (httpOnly cookie, onboarding UX) hỏng. UX nhẹ.
- **Fix:** thêm route `kitehub-preferences-v1` `Path=/api/v1/preferences/**` → kitehub-subscription, đặt **TRƯỚC** `instance-apis`.

### C3 — `/api/v1/admin/parent/consent/**` (kiteclass-core) bị shadow (P1) — VÔ HÌNH với detector

- **Controller thật:** `ParentConsentAdminController` (kiteclass-core) `@RequestMapping("/api/v1/admin/parent/consent")` — `POST /bulk-bump`.
- **Routing thực tế:** không có carve `/api/v1/admin/parent/**` → khớp `kitehub-admin-v1` `/api/v1/admin/**` → **kitehub-admin** → 404.
- **Cùng class GAP-1041 (payroll).** Vì controller nằm ở kiteclass-core nên `audit-gateway-routes.sh` không scan → không bắt.
- **Ảnh hưởng:** bulk-bump consent version cho parent (child-protection / PDPL) DEAD.
- **Fix:** thêm route `kiteclass-admin-parent-consent` `Path=/api/v1/admin/parent/**` → kiteclass-core (**+ TenantResolver**, mirror `kiteclass-payroll`), đặt **TRƯỚC** `kitehub-admin-v1`.

### C4 — `/api/v1/payments/webhook/**` (kiteclass-core) kẹt sau TenantResolver (P1) — VÔ HÌNH

- **Controller thật:** `PaymentWebhookController` (kiteclass-core) `@RequestMapping("/api/v1/payments/webhook")` — `GET /vnpay`, `POST /momo`, `POST /zalopay` (callback từ payment gateway bên ngoài, KHÔNG có JWT / KHÔNG có subdomain).
- **Routing thực tế:** rơi xuống `instance-apis` `/api/v1/**` → kiteclass-core **+ TenantResolver** → TenantResolver không resolve được tenant → **400 "Cannot resolve tenant"**.
- **Cùng class GAP-790** (public endpoint kẹt sau TenantResolver).
- **Ảnh hưởng:** webhook xác nhận thanh toán từ VNPay/MoMo/ZaloPay bị reject → payment không bao giờ confirm. Phase 1.5.
- **Fix:** thêm route `kiteclass-payment-webhook` `Path=/api/v1/payments/webhook/**` → kiteclass-core **SKIP TenantResolver** (tenant derive server-side từ transaction ref), đặt **TRƯỚC** `instance-apis`. Mirror `staff-invitations-public-token`.

### C5 — `/api/v1/parent-invitations/redeem/{token}` (kiteclass-core) kẹt sau TenantResolver (P1) — VÔ HÌNH

- **Controller thật:** `ParentInvitationController` (kiteclass-core) `@RequestMapping("/api/v1/parent-invitations")` — `POST /redeem/{token}` (javadoc ghi rõ "public endpoint consumed by the Gateway during parent signup", parent click email link, không tenant context). `POST` (invite, owner-scoped) thì OK qua JWT tenant claim.
- **Routing thực tế:** không có carve cho redeem → `instance-apis` `/api/v1/**` → kiteclass-core **+ TenantResolver** → **400**.
- **Cùng class GAP-790.** Parent auth đã pull-forward Phase 1 (Wave auth-1, PR #2186) → đây là P1 active, không phải Phase 2.
- **Fix:** thêm route `parent-invitations-public-redeem` `Path=/api/v1/parent-invitations/redeem/**` → kiteclass-core **SKIP TenantResolver**, đặt **TRƯỚC** `instance-apis`. Owner-scoped `/api/v1/parent-invitations` (create/list) vẫn dùng instance-apis TenantResolver (resolve từ JWT) — OK, không cần thêm.

### Caveat (LOW, không phải collision cứng) — `/api/v1/leads` POST public

`LeadController` (kiteclass-core) `POST /api/v1/leads` là public (lead capture form). Đi qua `instance-apis` + TenantResolver. Trên production form đặt ở subdomain trường (`school.kiteclass.com`) → TenantResolver resolve từ Host → **chạy được**. Chỉ 400 trên localhost/apex. Không carve riêng → chấp nhận được khi truy cập qua subdomain. Ghi nhận để theo dõi nếu lead form đặt ở apex domain.

### Internal-only (đúng thiết kế, KHÔNG cần route)

`/internal/notify`, `/internal/students`, `/internal/teachers`, `/internal/parents`, `/public/dmca` (kiteclass-core) + `/api/platform/emails` (kitehub-email) — nằm ngoài `/api/...` gateway-exposed family, gọi trực tiếp service-to-service trên docker network. Không có route gateway là CỐ Ý (giống GAP-1031 email). Không phải collision.

---

## 4. Xác nhận GAP-1034 + GAP-1041 root cause

### GAP-1034 (KC-10 per-tenant branding) — ✅ root cause XÁC NHẬN + fix HOÀN CHỈNH

- **Root cause:** `kitehub-branding-v1` `/api/v1/branding/**` → kitehub-branding sẽ shadow 3 controller kiteclass-core (`PublicBrandingController /public`, `BrandingVersionController /{id}/versions`, `BrandingPackageController /{id}/package`).
- **Fix hiện có:** 3 route carve (`kiteclass-branding-public`, `kiteclass-branding-versions` `/api/v1/branding/*/versions/**`, `kiteclass-branding-package` `/api/v1/branding/*/package`) đặt TRƯỚC `kitehub-branding-v1`.
- **Verify completeness:** method-level kiteclass branding = `GET /{instanceId}/package`, `GET /{instanceId}/versions`, `POST /{instanceId}/versions/{n}/rollback`, `GET /public` — TẤT CẢ khớp 3 carve route. kitehub-branding sub-paths (`/slug-availability`, `/regenerate-quota`, `/jobs/**`, `/instances`) KHÔNG khớp `/public`,`*/versions`,`*/package` → không regression. **Fix đúng + đủ.** ✓

### GAP-1041 (KC-12 payroll) — ✅ root cause XÁC NHẬN + fix HIỆN CÓ

- **Root cause:** `PayrollController` (kiteclass-core) `/api/v1/admin/payroll` bị `kitehub-admin-v1` `/api/v1/admin/**` → kitehub-admin → 404.
- **Fix hiện có:** route `kiteclass-payroll` `/api/v1/admin/payroll/**` → kiteclass-core + TenantResolver, đặt TRƯỚC `kitehub-admin-v1`. **Đúng.** ✓
- **Lưu ý:** C3 (ParentConsentAdminController `/api/v1/admin/parent/consent`) là chính xác cùng class này nhưng CHƯA được carve → recurrence chưa đóng hết.

---

## 5. META finding — blind spot của `scripts/audit-gateway-routes.sh`

Đây là nguyên nhân gốc tại sao collision class này LẶP LẠI (GAP-1041 + C3/C4/C5 phải tìm bằng manual walk).

| Blind spot | Mô tả | Hệ quả |
|---|---|---|
| **BS#1 — không scan kiteclass-core** | Script (line 70) chỉ loop `kitehub-{subscription,branding,admin,email}`. KHÔNG có `kiteclass-core`. | MỌI controller kiteclass-core bị kitehub catch-all shadow (`/api/v1/admin/**`→admin, `/api/v1/branding/**`→branding) đều VÔ HÌNH. GAP-1041 + C3 thuộc đây. |
| **BS#2 — không model TenantResolver 400** | Script chỉ check "controller có route khớp không" + "route đúng service không". Không biết public endpoint kẹt sau `instance-apis` TenantResolver sẽ 400. | C4 (payment webhook) + C5 (parent redeem) VÔ HÌNH dù chúng "khớp" route `/api/v1/**` đúng service kiteclass-core. |

**Ground truth khi chạy script hôm nay (`bash scripts/audit-gateway-routes.sh`):** exit 1, bắt 4 WRONG_SERVICE — tất cả đều là **kitehub-side** (C1 ×3 sub-path + C2). KHÔNG bắt C3/C4/C5 (kiteclass-side). Xác nhận BS#1+BS#2.

**Đề xuất fix detector (cho fix wave P2):**
1. **BS#1:** thêm `kiteclass-core` vào danh sách module scan (line 70) với `MODULE_EXPECTED_HOST["kiteclass-core"]="kiteclass-core"`. Sẽ bắt C3 (và ngăn tái diễn GAP-1041 class). Cần điều chỉnh `INTERNAL_ONLY_PATTERNS` để bỏ qua `/internal/**` + `/public/dmca` của kiteclass.
2. **BS#2:** thêm check phụ — endpoint public (controller có comment/annotation "public" hoặc nằm trong allowlist webhook/redeem/by-token) mà khớp route có filter `TenantResolver` → WARN "public endpoint behind TenantResolver → 400 risk". Heuristic, để WARN-only.
3. Wire vào CI BLOCKING sau khi C1–C5 fix xong (hiện `production-env-config-registry.md` §3.3 để CI gate deferred → đó là lý do C1/C2 detectable nhưng chưa bị chặn merge).

---

## 6. P2 cluster scope — route cần fix (cho parallel fix wave)

Tất cả fix là thêm route carve-out đặt TRƯỚC catch-all tương ứng trong `application.yml`. Không sửa controller.

| # | Route mới (id đề xuất) | Predicate | Target / Filter | Đặt TRƯỚC route | Mirror pattern |
|---|---|---|---|---|---|
| C1 | `platform-admin-payments-subscription` | `Path=/api/platform/admin/payments/**` | kitehub-subscription / CB(subscription) | `platform-admin` | `platform-admin-emails-subscription` |
| C2 | `kitehub-preferences-v1` | `Path=/api/v1/preferences/**` | kitehub-subscription / CB(subscription) | `instance-apis` | `kitehub-notification-preferences-v1` |
| C3 | `kiteclass-admin-parent-consent` | `Path=/api/v1/admin/parent/**` | kiteclass-core / **TenantResolver**+CB(instance) | `kitehub-admin-v1` | `kiteclass-payroll` |
| C4 | `kiteclass-payment-webhook` | `Path=/api/v1/payments/webhook/**` | kiteclass-core / **SKIP TenantResolver**, CB(instance) | `instance-apis` | `staff-invitations-public-token` |
| C5 | `parent-invitations-public-redeem` | `Path=/api/v1/parent-invitations/redeem/**` | kiteclass-core / **SKIP TenantResolver**, CB(instance) | `instance-apis` | `staff-invitations-public-token` |
| META | fix `audit-gateway-routes.sh` BS#1+BS#2 | — | thêm kiteclass-core scan + TenantResolver-400 WARN | — | §5 |

**Lưu ý thứ tự (CRITICAL):** mọi route carve PHẢI đặt TRƯỚC catch-all tương ứng vì Spring Cloud Gateway match theo declaration order. C3 dùng TenantResolver (admin scope, tenant từ JWT); C4/C5 SKIP TenantResolver (public, tenant derive server-side). C1/C2 thuần CircuitBreaker (không tenant-scoped ở gateway level).

**Severity ưu tiên fix:** C3 + C5 (P1, KC parent/consent + parent onboarding ĐANG active Phase 1) > C1 + C4 (P1, payment Phase 1.5) > C2 (P2 UX) > META (P1 force-multiplier — ngăn recurrence).

---

## 7. Phụ lục — route đếm + verify state

- Tổng route: **56** (script đếm), 102 backend controller endpoint (chỉ kitehub, chưa gồm ~52 kiteclass-core controller).
- `bash scripts/audit-gateway-routes.sh` → exit 1, 4 WRONG_SERVICE (C1 ×3 + C2). 0 ORPHAN. 0 UNKNOWN_HOSTS.
- TenantResolver 400 path: `TenantResolverGatewayFilterFactory.java:101` `respondWithError(BAD_REQUEST, "Cannot resolve tenant")`.
- kiteclass-core SecurityConfig: `.anyRequest().permitAll()` — service trust gateway-enforced auth (JWT filter + @PreAuthorize). TenantResolver tại gateway là cơ chế tenant-scoping → đó là lý do public path kẹt sau nó bị 400.
