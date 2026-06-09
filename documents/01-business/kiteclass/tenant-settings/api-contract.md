# Tenant Settings — API Contract

## Endpoints — BrandingController

### GET /api/v1/settings/branding

**Use Case:** UC-TNT-01 | **Auth:** Bearer token | **Role:** ADMIN, TEACHER

**Response 200:**
```json
{ "success": true, "data": { "id": 1, "logoUrl": "/uploads/logo.png", "faviconUrl": "/uploads/favicon.ico", "displayName": "Trung tâm ABC", "tagline": "Học mà vui", "primaryColor": "#1976D2", "secondaryColor": "#424242", "accentColor": "#FF5722", "themeConfigJson": "{...}", "contactEmail": "info@abc.edu.vn", "contactPhone": "0901234567", "address": "123 Nguyen Hue, HCM", "facebookUrl": "https://fb.com/abc", "zaloUrl": "https://zalo.me/abc", "websiteUrl": "https://abc.edu.vn" } }
```

> **Note (GAP-1072):** `logoUrl` / `faviconUrl` được lưu dạng presigned MinIO URL (hết hạn sau 7 ngày). Mỗi lần đọc, BE tự regenerate presigned URL mới từ object key nên FE luôn nhận URL còn hạn — KHÔNG cần re-upload. Giá trị non-presigned / external giữ nguyên.

### PUT /api/v1/settings/branding

**Use Case:** UC-TNT-02 | **Auth:** Bearer token | **Role:** ADMIN

**Request:**
```json
{ "displayName": "string", "tagline": "string?", "primaryColor": "#hex", "secondaryColor": "#hex", "accentColor": "#hex", "themeConfigJson": "string?", "contactEmail": "string?", "contactPhone": "string?", "address": "string?", "facebookUrl": "string?", "zaloUrl": "string?", "websiteUrl": "string?" }
```

**Response 200:** BrandingResponse (same as GET)

### POST /api/v1/settings/branding/logo

**Use Case:** UC-TNT-02 | **Auth:** Bearer token | **Role:** ADMIN

**Request:** `{ "fileUrl": "string" }` | **Response 200:** BrandingResponse

### POST /api/v1/settings/branding/favicon

**Auth:** Bearer token | **Role:** ADMIN

**Request:** `{ "fileUrl": "string" }` | **Response 200:** BrandingResponse

### GET /api/v1/settings/branding/theme

**Auth:** Public (for tenant frontend rendering)

**Response 200:** `{ "success": true, "data": "{\"primaryColor\":\"#1976D2\",...}" }`

---

## Endpoints — UserPreferencesController

### GET /api/v1/users/{userId}/preferences

**Use Case:** UC-TNT-03 | **Auth:** Bearer token | **Role:** Owner hoặc ADMIN

**Response 200:**
```json
{ "success": true, "data": { "id": 1, "userId": 42, "language": "vi", "timezone": "Asia/Ho_Chi_Minh", "theme": "LIGHT", "notificationPreferences": { "email": true, "sms": false, "push": true } } }
```

### PATCH /api/v1/users/{userId}/preferences

**Use Case:** UC-TNT-04 | **Auth:** Bearer token | **Role:** Owner hoặc ADMIN

**Request:**
```json
{ "language": "vi|en", "timezone": "string?", "theme": "LIGHT|DARK|SYSTEM", "notificationPreferences": { "email": true, "sms": false } }
```

**Response 200:** UserPreferencesResponse

### POST /api/v1/users/{userId}/preferences/initialize

**Use Case:** UC-TNT-03 | **Auth:** Internal/System

**Response 201:** UserPreferencesResponse with defaults

---

## Endpoints — LandingPageController

### GET /api/v1/tenants/{tenantId}/landing

**Use Case:** UC-TNT-05 | **Auth:** Public

**Response 200:**
```json
{ "success": true, "data": { "id": 1, "heroTitle": "Chào mừng đến ABC", "heroSubtitle": "Nơi kiến thức bắt đầu", "heroImageUrl": "/uploads/hero.jpg", "teacherBio": "10 năm kinh nghiệm...", "logoUrl": "/uploads/logo.png", "tagline": "Học mà vui", "primaryColor": "#1976D2", "secondaryColor": "#424242", "contactEmail": "info@abc.edu.vn", "contactPhone": "0901234567", "address": "123 Nguyen Hue", "facebookUrl": "https://fb.com/abc", "youtubeUrl": null, "instagramUrl": null } }
```

### PUT /api/v1/tenants/{tenantId}/landing

**Use Case:** UC-TNT-06 | **Auth:** Bearer token | **Role:** ADMIN

**Request:** Same fields as LandingPageResponse (all optional for partial update)

**Response 200:** LandingPageResponse

---

## Errors (shared)

| Status | Code | Message |
|--------|------|---------|
| 404 | BRANDING_NOT_FOUND | "Branding config not found" |
| 404 | PREFERENCES_NOT_FOUND | "User preferences not found" |
| 404 | LANDING_PAGE_NOT_FOUND | "Landing page not found" |
| 400 | INVALID_COLOR_FORMAT | "Color must be hex format" |
| 403 | ACCESS_DENIED | "Not authorized for this tenant" |

---

## Endpoints — TenantSettingsController (GAP-947)

> Per-tenant config (timezone / locale / Năm học / ...). `{id}` = tenant (instance) UUID, PHẢI khớp `X-Tenant-Id` (tenant isolation).
> Code: `kiteclass-core/module/tenantsettings/`

### GET /api/v1/tenants/{id}/settings

**Use Case:** UC-TSET-01 | **Auth:** Bearer token + `X-Tenant-Id` | **Behavior:** auto-create default (Năm học auto-fill) nếu chưa có.

**Response 200:**
```json
{ "success": true, "data": { "id": 1, "timezone": "Asia/Ho_Chi_Minh", "locale": "vi", "academicYear": "2026-2027", "fiscalYear": null, "schoolType": "CENTER", "address": null, "phone": null, "logoUrl": null, "themeConfig": null } }
```

### PUT /api/v1/tenants/{id}/settings

**Use Case:** UC-TSET-02 | **Auth:** Bearer token + `X-Tenant-Id` | **Behavior:** upsert — provided-field-wins (null giữ giá trị cũ).

**Request:** (tất cả field optional)
```json
{ "timezone": "Asia/Ho_Chi_Minh", "locale": "vi", "academicYear": "2026-2027", "fiscalYear": "2026", "schoolType": "K12", "address": "123 Đường Láng, Hà Nội", "phone": "0241234567", "logoUrl": "https://cdn.example.com/logo.png", "themeConfig": { "primaryColor": "#2563eb" } }
```

**Response 200:** TenantSettingsResponse (same shape as GET `data`).

**Field constraints:** `academicYear` regex `^\d{4}-\d{4}$`; `schoolType` ∈ `CENTER|K12|UNIVERSITY|OTHER`; `timezone`≤50, `locale`≤10, `fiscalYear`≤20, `address`≤500, `phone`≤30, `logoUrl`≤1000.

### Errors (TenantSettings)

| Status | Code | Message |
|--------|------|---------|
| 403 | TENANT_ACCESS_DENIED | "{id} ≠ tenant đăng nhập (cross-tenant IDOR)" |
| 400 | (validation) | "academicYear/schoolType/length validation" |
