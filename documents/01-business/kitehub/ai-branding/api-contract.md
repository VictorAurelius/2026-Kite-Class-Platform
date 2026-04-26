# AI Branding — API Contract

## POST /api/platform/branding/ai/analyze-logo
**Use case:** UC-AIB-01
**Auth:** Bearer token
**Headers:** `X-Instance-Id: {uuid}`, `X-Subscription-Tier: BASIC`
**Request:**
```json
{
  "logoUrl": "https://example.com/logo.png",
  "organizationName": "Trường ABC"
}
```
**Response 200:**
```json
{
  "primaryColor": "#1a73e8",
  "secondaryColor": "#fbbc04",
  "fontFamily": "Roboto",
  "brandIdentity": "professional, education-focused"
}
```
**Errors:**
- 429: `{ "error": "AI_RATE_LIMIT_EXCEEDED", "dailyLimit": 3, "tier": "FREE" }`

---

## POST /api/platform/branding/ai/generate-image
**Use case:** UC-AIB-02
**Auth:** Bearer token
**Headers:** `X-Instance-Id: {uuid}`, `X-Subscription-Tier: BASIC`
**Request:**
```json
{
  "organizationName": "Trường ABC",
  "theme": "modern education",
  "colors": "#1a73e8, #fbbc04"
}
```
**Response 200:**
```json
{ "imageUrl": "https://cdn.example.com/generated/hero-abc.png" }
```
**Errors:** 429 rate limit exceeded

---

## POST /api/platform/branding/ai/generate-text
**Use case:** UC-AIB-03
**Auth:** Bearer token
**Headers:** `X-Instance-Id: {uuid}`, `X-Subscription-Tier: BASIC`
**Request:**
```json
{
  "organizationName": "Trường ABC",
  "theme": "modern education",
  "targetAudience": "học sinh THPT"
}
```
**Response 200:**
```json
{ "text": "Nâng tầm học tập với công nghệ hiện đại..." }
```
**Errors:** 429 rate limit exceeded

---

## POST /api/platform/branding/ai/generate-theme
**Use case:** UC-AIB-04
**Auth:** Bearer token
**Headers:** `X-Instance-Id: {uuid}`, `X-Subscription-Tier: BASIC`
**Request:** LogoAnalysis object
```json
{
  "primaryColor": "#1a73e8",
  "secondaryColor": "#fbbc04",
  "fontFamily": "Roboto",
  "brandIdentity": "professional"
}
```
**Response 200:** ThemeConfig object
```json
{
  "primary": "#1a73e8",
  "secondary": "#fbbc04",
  "fontFamily": "Roboto",
  "borderRadius": "8px",
  "spacing": "comfortable"
}
```
**Errors:** 429 rate limit exceeded

---

## GET /api/platform/branding/templates
**Use case:** UC-AIB-05
**Auth:** Bearer token
**Request params:** `?category=education` (optional)
**Response 200:**
```json
[
  {
    "id": "uuid",
    "name": "Education Modern",
    "category": "education",
    "active": true,
    "themeConfig": "{ ... }"
  }
]
```

---

## GET /api/platform/branding/templates/{id}
**Auth:** Bearer token
**Response 200:** Single BrandingTemplate
**Errors:** 404 not found

---

## POST /api/platform/branding/templates/{id}/apply
**Use case:** UC-AIB-06
**Auth:** Bearer token
**Headers:** `X-Instance-Id: {uuid}` (required)
**Response 200:**
```json
{
  "themeConfig": "{ \"primary\": \"#1a73e8\", ... }",
  "status": "applied"
}
```
**Errors:** 404 template not found

---

# v2 Endpoints (Waves 2-4 — kiteclass-core implementation)

> **Source-of-truth:** Real Controllers in `kiteclass/kiteclass-core/src/main/java/com/kiteclass/core/module/{instance,branding}/controller/`. Schema below derived directly from `InstanceController`, `BrandingPackageController`, `PublicBrandingController`, `InternalWebhookController`. Use `InstanceResponse` record for response shape (id is `Long`, not UUID).

## Lifecycle endpoints (`/api/v1/instances`)

All write endpoints delegate to `InstanceLifecycleService` (BR-LIFE-003); state-machine validation per BR-LIFE-002. Response shape:

```json
{
  "success": true,
  "message": "...",
  "data": {
    "id": 12345,
    "tenantId": "...",
    "slug": "...",
    "frontendUrl": "https://...",
    "status": "DEPLOYED",
    "retryCount": 0,
    "failureReason": null,
    "brandingVersion": 3,
    "initializingAt": "2026-04-26T10:00:00Z",
    "generatingAt": "2026-04-26T10:01:00Z",
    "deployedAt": "2026-04-26T10:05:00Z",
    "lastRegenerateAt": null,
    "failedAt": null
  }
}
```

### POST /api/v1/instances
**Use case:** UC-AIB-07 (saga's first step; can also be called directly by ops/admin)
**Auth:** Internal / admin only (gateway filter)
**Status:** 201 Created
**Request:**
```json
{
  "tenantId": "kitehub-tenant-uuid",
  "slug": "abc-school"
}
```
**Response 201:** `InstanceResponse` (status `INITIALIZING`)
**Errors:**
- 400: slug already in use → `IllegalArgumentException`
- 400: validation error (tenantId/slug missing)

### GET /api/v1/instances/{id}
**Auth:** Bearer token (admin)
**Response 200:** `InstanceResponse`
**Errors:** 404 instance not found

### GET /api/v1/instances?status=DEPLOYED
**Auth:** Bearer token (admin)
**Request params:** `?status=NOT_STARTED|INITIALIZING|GENERATING|DEPLOYED|REGENERATING|FAILED` (optional — omit for all)
**Response 200:** Array of `InstanceResponse`

### POST /api/v1/instances/{id}/infrastructure-ready
**Use case:** UC-AIB-07 step 5
**Auth:** Internal (called by `TenantProvisioningSaga` or Infrastructure service)
**Transition:** INITIALIZING → GENERATING
**Response 200:** `InstanceResponse`
**Errors:** 409 invalid transition (e.g. instance ở DEPLOYED)

### POST /api/v1/instances/{id}/branding-completed
**Use case:** UC-AIB-07 step 7 / UC-AIB-09 step 4
**Auth:** Internal (called by `PublishPackageStep`)
**Transition:** GENERATING|REGENERATING → DEPLOYED; `brandingVersion++`
**Request (optional):**
```json
{ "frontendUrl": "https://abc-school.kiteclass.vn" }
```
**Response 200:** `InstanceResponse`

### POST /api/v1/instances/{id}/rebrand
**Use case:** UC-AIB-09 (lower tiers) hoặc step 5 của UC-AIB-10 (Enterprise sau APPROVED)
**Auth:** Bearer token
**Transition:** DEPLOYED → REGENERATING
**Response 200:** `InstanceResponse`
**Errors:** 409 invalid transition

### POST /api/v1/instances/{id}/failed
**Use case:** Saga compensation (UC-AIB-07 errors), manual ops force-fail
**Auth:** Internal / admin
**Transition:** * → FAILED; `retryCount++`
**Request:**
```json
{ "reason": "step PickTemplate threw: no template for audience=K-12 tone=energetic" }
```
**Response 200:** `InstanceResponse`

### POST /api/v1/instances/{id}/retry
**Use case:** Recovery sau FAILED
**Auth:** Bearer token (admin)
**Transition:** FAILED → INITIALIZING (chỉ khi `retryCount < MAX_RETRIES = 3` per BR-LIFE-005)
**Response 200:** `InstanceResponse`
**Errors:** 409 if `retryCount ≥ 3` (instance abandoned, manual intervention required)

---

## Branding package endpoints (`/api/v1/branding`)

### GET /api/v1/branding/{instanceId}/package
**Use case:** UC-AIB-11
**Auth:** Bearer token
**Headers (optional):** `If-None-Match: W/"v3-a1b2c3d4"` (ETag từ previous fetch)
**Response 200:**
```json
{
  "instanceId": 12345,
  "tenantId": "kitehub-tenant-uuid",
  "slug": "abc-school",
  "frontendUrl": "https://abc-school.kiteclass.vn",
  "brandingVersion": 3,
  "deployedAt": "2026-04-26T10:05:00Z",
  "assets": [
    { "type": "logo", "category": "STATIC", "url": "https://cdn/.../logo.svg", "alt": "ABC School logo" },
    { "type": "hero", "category": "TEMPLATE", "url": "https://cdn/.../hero.png", "alt": "..." }
  ]
}
```
**Response Header:** `ETag: W/"v3-a1b2c3d4"` (per BR-PKG-002)
**Response 304 Not Modified:** body rỗng, header `ETag` giữ nguyên (cache hit FE-side)
**Cache (server):** Per BR-PKG-003, evict on outbox events `instance.deployed` / `instance.regenerating`

### GET /api/v1/branding/public?tenantId={uuid-or-slug}
**Use case:** UC-AIB-12
**Auth:** None (unauthenticated)
**Request params:** `tenantId` accepts either UUID hoặc slug — slug được resolve qua `FrontendInstanceRepository.findBySlugAndDeletedFalse`
**Response 200:**
```json
{
  "displayName": "ABC School",
  "logoUrl": "https://cdn/.../logo.svg",
  "primaryColor": "#1a73e8",
  "secondaryColor": "#fbbc04",
  "accentColor": "#10B981",
  "tagline": "Học cho tương lai"
}
```
**Defaults (tenant không tồn tại / chưa có branding):**
```json
{
  "displayName": "KiteClass",
  "logoUrl": "",
  "primaryColor": "#3B82F6",
  "secondaryColor": "#8B5CF6",
  "accentColor": "#10B981",
  "tagline": ""
}
```
**Lưu ý bảo mật:** Endpoint này KHÔNG leak admin fields — chỉ 6 fields công khai an toàn

---

## Internal endpoints (`/internal/notify`)

### POST /internal/notify/instance-deployed?instanceId={id}
**Use case:** Outbox dispatcher hoặc ops manual cache invalidation
**Auth:** Internal-only (gateway `InternalRequestFilter`)
**Headers (optional):** `X-Internal-Caller: outbox-dispatcher`
**Response 200:**
```json
{
  "success": true,
  "message": "branding-package cache evicted for instance 12345",
  "data": "evicted"
}
```

---

## Approval endpoints (TBD — UC-AIB-10)

> **Note:** `RebrandApprovalService` exists (Wave 3 Sub-PR 3.5, GAP-070) but REST controller chưa được landed. Khi landed sẽ thêm vào doc:
>
> - `POST /api/v1/instances/{id}/rebrand-approval` — Admin 1 request approval (BR-APRV)
> - `POST /api/v1/rebrand-approvals/{approvalId}/approve` — Admin 2 approve (BR-APRV-002)
> - `POST /api/v1/rebrand-approvals/{approvalId}/reject`
> - `GET /api/v1/instances/{id}/rebrand-approvals` — list approvals for instance

Tracker: see GAP-070 follow-up controller PR.
