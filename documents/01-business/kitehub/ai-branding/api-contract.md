# AI Branding — API Contract

> **Note on identifiers:** Path params `instanceId` / `jobId` and any `id` field
> exposed in responses are UUID v4 strings (e.g.
> `550e8400-e29b-41d4-a716-446655440000`), not numeric. Earlier revisions of
> this document used numeric placeholders for illustration; all examples are
> now aligned to the actual wire format (GAP-390-C).

> **AI provider abstraction (ADR-038, 2026-06-02):** Tất cả endpoints `POST /api/platform/branding/ai/*` đều route qua `AIClient` interface (provider-agnostic) — NotificationChannel-style abstraction per `design-patterns.md` §3.10. Provider selection (Gemini Free Tier primary / OpenAI fallback) config-driven qua `ai.provider.primary` + `ai.provider.fallback`. Response shape vendor-neutral (no `GeminiResponse` / `OpenAIResponse` leak vào domain layer). Cost + latency + token usage emit qua Micrometer metrics per `documents/02-architecture/ai-external-observability-plan.md`. Khi cả 2 provider Circuit Breaker OPEN → fallback to template-only output per `ai-branding-guidelines.md` taxonomy.

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
    "id": "550e8400-e29b-41d4-a716-446655440000",
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
  "instanceId": "550e8400-e29b-41d4-a716-446655440000",
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
  "message": "branding-package cache evicted for instance 550e8400-e29b-41d4-a716-446655440000",
  "data": "evicted"
}
```

---

# Wave 34 — AI Branding Wizard endpoints

> **Source-of-truth (when shipped):** Future `BrandingWizardController` + `BrandingJobController` in `kitehub/kitehub-branding/src/main/java/com/kite/hub/branding/controller/`. These endpoints back the FE wizard refactor (Wave 34 Bucket D) and replace inline mocks (`MOCK_TAKEN_SLUGS`, `STUB_JOB_ID`, `TEMPLATE_TO_COLORS`) shipped in Wave 32 v1. Bucket 0 (this PR) ships the contract; Buckets A/B/C implement controllers + DTOs matching the schemas below; Bucket D consumes via MSW handlers (`kitehub-frontend/src/test/msw/handlers/branding.ts`).
>
> **Error envelope (project convention):** all error responses follow `{ "error": "<CODE>", "message": "<human-readable>", ...optional context }`. Error codes are SCREAMING_SNAKE_CASE matching the v1 endpoints above (`AI_RATE_LIMIT_EXCEEDED`, `AI_INPUT_TOO_LONG`, etc.).

## GET /api/v1/branding/slug-availability
**Use case:** Wizard step "choose tenant slug"; replaces Wave 32 v1 inline `MOCK_TAKEN_SLUGS` (sub-GAP-272i)
**Auth:** Bearer token
**Request params:** `?slug={slug}` (required; 3–63 chars, lowercase alphanumeric + hyphens, no leading/trailing hyphen)
**Response 200:**
```json
{
  "available": false,
  "suggestions": ["abc-school-2", "abc-school-vn", "abc-school-edu"]
}
```
- `available` — `true` nếu slug chưa tồn tại trong `frontend_instances` và pass reserved-words filter
- `suggestions` — 0..5 alternates khi `available=false`; rỗng `[]` khi `available=true`
**Errors:**
- 400: `{ "error": "INVALID_SLUG_FORMAT", "message": "...", "slug": "..." }`
- 401: unauthenticated

---

## GET /api/v1/branding/regenerate-quota
**Use case:** Wizard preview screen + dashboard regenerate counter; replaces Wave 32 v1 inline tier-quota logic (sub-GAP-272d)
**Auth:** Bearer token
**Headers:** `X-Subscription-Tier: FREE|BASIC|PREMIUM|ENTERPRISE` (gateway-injected; alias cũ `PRO` được BE canonical hoá → BASIC per GAP-1228)
**Response 200:**
```json
{
  "tier": "BASIC",
  "used": 4,
  "limit": 10,
  "resetAt": "2026-05-08T00:00:00Z"
}
```
- `tier` — current subscription tier
- `used` — số regenerate đã consume trong current window
- `limit` — tier cap per `ai-branding-guidelines.md` §4.3 (FREE=3, BASIC=10, PREMIUM=30, ENTERPRISE=−1 = unlimited; alias JWT cũ PRO → BASIC per GAP-1228)
- `resetAt` — ISO-8601 UTC khi quota reset (daily window). Khi `limit=-1`, `resetAt` = `null`.
**Errors:**
- 401: unauthenticated
- 403: tier not recognized → fallback to FREE per `AIInputCapService` fail-safe convention

---

## POST /api/v1/branding/jobs/{jobId}/regenerate
**Use case:** User clicks "Regenerate" sau preview; replaces Wave 32 v1 inline regenerate stub (sub-GAP-272d)
**Auth:** Bearer token
**Headers:** `Idempotency-Key: <uuid>` (required — same key returns same job within 10 min window)
**Request:** empty body
**Response 200:** Updated `BrandingJob` (xem schema GET `/api/v1/branding/jobs/{jobId}` bên dưới)
- Server consumes 1 quota (atomic) trước khi enqueue
- Returns the existing job updated với `status=REGENERATING`, `regenerateCount++`
**Errors:**
- 400: `{ "error": "MISSING_IDEMPOTENCY_KEY", "message": "..." }`
- 401: unauthenticated
- 403: `{ "error": "AI_REGENERATE_QUOTA_EXCEEDED", "tier": "FREE", "used": 3, "limit": 3, "resetAt": "..." }` — quota cap hit (per `ai-branding-guidelines.md` §4.3)
- 404: `{ "error": "JOB_NOT_FOUND", "jobId": "..." }`
- 409: `{ "error": "INVALID_JOB_STATE", "currentStatus": "GENERATING", "message": "regenerate only allowed from DEPLOYED" }` — state-machine reject per BR-LIFE-002

---

## POST /api/v1/branding/jobs/{jobId}/sse-token
**Use case:** Mint a short-lived HMAC token cho browser EventSource (GAP-1021). EventSource KHÔNG set được `Authorization` header → FE gọi endpoint này (Bearer JWT) để lấy token ngắn hạn, rồi mở deploy-stream với `?access_token=<token>`.
**Auth:** Bearer token
**Response 200:** `{ "token": "<hmac-token>", "expiresInSeconds": <ttl> }`
**Errors:** 401 unauthenticated · 404 job not found

---

## GET /api/v1/branding/jobs/{jobId}/deploy-stream
**Use case:** Live wizard "deploy" step UI streams progress; replaces Wave 32 v1 inline simulated progress (sub-GAP-272e)
**Auth:** SSE EventSource → `?access_token=<minted-token>` query param (GAP-1021, `SseQueryTokenAuthFilter`). FE chốt 1 đường = mint sse-token (KHÔNG dùng raw `?token=<JWT>` legacy gateway path — JWT-in-URL dài + dễ hết hạn giữa walk). Mint qua `POST .../sse-token` trước, FE `useDeployStream` carry token vào `?access_token=`.
**Content-Type:** `text/event-stream` (SSE)
**Response 200:** SSE stream — events terminated by `\n\n`, heartbeat ~30s.

Event types (line `event: <name>` followed by `data: <json>`):

| Event | Payload | Emitted when |
|---|---|---|
| `log` | `{ "ts": "ISO8601", "level": "INFO\|WARN\|ERROR", "message": "..." }` | Each step log line |
| `progress` | `{ "step": "ANALYZE\|PLAN\|GENERATE\|REVIEW\|DEPLOY", "percent": 0-100 }` | Per-step progress tick |
| `state-change` | `{ "from": "GENERATING", "to": "DEPLOYED", "ts": "ISO8601" }` | Lifecycle state transition (BR-LIFE-002) |
| `complete` | `{ "jobId": "...", "finalStatus": "DEPLOYED", "ts": "ISO8601" }` | Stream terminates successfully |
| `error` | `{ "errorCode": "...", "message": "...", "retryable": true }` | Stream terminates with error |
| `heartbeat` | `{}` | Every ~30s while idle (proxy keepalive) |

Stream closes after `complete` or `error`. FE reconnect via `Last-Event-ID` header optional (server-side cursor not yet implemented; v1 = full replay on reconnect).

**Errors (initial response):**
- 401: unauthenticated
- 404: job not found
- 409: job already in terminal state (FAILED/DEPLOYED + no active stream) — FE should fetch GET `/api/v1/branding/jobs/{jobId}` instead

---

## GET /api/v1/branding/jobs/{jobId}/quality-score
**Use case:** Wizard preview screen displays quality breakdown before user approves DEPLOY; replaces Wave 32 v1 inline placeholder (sub-GAP-272c). Tied to `InstanceQualityReviewer.review()` per `ai-branding-guidelines.md` §5.
**Auth:** Bearer token
**Response 200:**
```json
{
  "jobId": "job-abc-123",
  "score": 82,
  "passed": true,
  "threshold": 70,
  "subscores": {
    "contrast": 90,
    "brokenLinks": 100,
    "cssVarsApplied": 85,
    "visualRegression": 75,
    "logoPlacement": 80
  },
  "issues": [
    { "severity": "WARN", "check": "visualRegression", "message": "..." }
  ],
  "computedAt": "2026-05-07T10:23:00Z"
}
```
- `score` — composite 0–100 (weighted average of subscores)
- `passed` — `score >= threshold` (default 70 per §5)
- `subscores` — 5 dimensions per `ai-branding-guidelines.md` §5; each 0–100
- `issues` — array of severity-tagged findings; empty `[]` khi all subscores ≥ threshold
**Errors:**
- 401: unauthenticated
- 404: job not found
- 409: `{ "error": "QUALITY_SCORE_NOT_READY", "currentStatus": "GENERATING", "message": "score available only after REVIEW step completes" }`

---

## GET /api/v1/branding/jobs/{jobId}/preview
**Use case:** iframe-safe HTML preview rendered trong wizard preview step; replaces Wave 32 v1 inline placeholder (sub-GAP-272j)
**Auth:** Bearer token (passed via `Authorization` header — preview is per-user, not public)
**Content-Type:** `text/html; charset=utf-8`
**Response Headers:**
- `X-Frame-Options: SAMEORIGIN`
- `Content-Security-Policy: default-src 'self'; img-src 'self' https://cdn.kiteclass.vn data:; style-src 'self' 'unsafe-inline'; frame-ancestors 'self'`
- `Cache-Control: private, max-age=60`
**Response 200:** Standalone HTML document — fully rendered with brand colors, logo, sample copy. Safe to embed via `<iframe sandbox="allow-same-origin">`.
**Errors:**
- 401: unauthenticated
- 404: job not found
- 409: preview not yet available (job still in `INITIALIZING`/`GENERATING` early phase) — body = HTML 503-style placeholder; status 200 maintained for iframe-friendly UX. Distinct from 404.

---

## GET /api/v1/branding/instances/{instanceId}/lifecycle/events
**Use case:** Admin/Ops debug timeline for instance + dashboard recent-activity feed; replaces Wave 32 v1 inline simulated events (sub-GAP-272l)
**Auth:** Bearer token (admin scope for cross-tenant; tenant scope for own instance)
**Request params:** `?since=ISO8601` (optional — default: last 30 days), `?limit=N` (optional, default 50, max 200), `?cursor=<opaque>` (optional pagination)
**Response 200:**
```json
{
  "instanceId": "550e8400-e29b-41d4-a716-446655440000",
  "events": [
    {
      "id": "evt-2026-0507-001",
      "ts": "2026-05-07T10:00:00Z",
      "type": "state-change",
      "fromState": "INITIALIZING",
      "toState": "GENERATING",
      "actor": { "kind": "system", "id": "saga:provisioning" },
      "metadata": { "brandingVersion": 1 }
    },
    {
      "id": "evt-2026-0507-002",
      "ts": "2026-05-07T10:05:00Z",
      "type": "regenerate-requested",
      "actor": { "kind": "user", "id": "usr-9f2e8d" },
      "metadata": { "regenerateCount": 2 }
    }
  ],
  "nextCursor": null
}
```
- Events sorted descending by `ts` (newest first)
- `type` ∈ `state-change | regenerate-requested | quality-score-computed | deploy-completed | failed | manual-override`
- `actor.kind` ∈ `user | system | admin`
- `nextCursor` — pass back as `?cursor=` for next page; `null` khi no more events
**Errors:**
- 401: unauthenticated
- 403: tenant scope mismatch
- 404: instance not found

---

## GET /api/v1/branding/instances/{instanceId}/deploy-status
**Use case:** Post-deploy `/branding` page (GAP-1108) — deploy-success card hiển thị trạng thái `DEPLOYED` + link landing (`frontendUrl`) + summary lần deploy gần nhất, KHÔNG cần parse full lifecycle-events feed.
**Auth:** Bearer token (tenant scope cho own instance; admin scope cross-tenant)
**Response 200:** `DeployStatusResponse` — instance lifecycle state + latest `deploy-completed` marker metadata:
```json
{
  "instanceId": "550e8400-e29b-41d4-a716-446655440000",
  "state": "DEPLOYED",
  "deployed": true,
  "frontendUrl": "https://toan-master.kiteclass.vn",
  "templateId": "sky-wave",
  "slug": "toan-master",
  "brandingVersion": 1,
  "deployedAt": "2026-06-09T09:57:59"
}
```
- `state` ∈ `NOT_STARTED | INITIALIZING | GENERATING | DEPLOYED | REGENERATING | FAILED` — `null` khi chưa có state row
- `deployed` = `state == DEPLOYED`
- `frontendUrl` / `templateId` / `slug` / `deployedAt` — lấy từ marker `deploy-completed` mới nhất; `null` khi instance chưa từng deploy
- `brandingVersion` — counter từ `branding_instance_state`; `null` khi chưa có state row
- MOCK boundary (Phase 1, GAP-1055): `frontendUrl` là placeholder `https://{slug}.kiteclass.vn`, chưa serve subdomain thật (GAP-811/1077)
**Errors:**
- 401: unauthenticated
- 403: tenant scope mismatch

---

## GET /api/v1/branding/jobs/{jobId}
**Use case:** FE polls khi không dùng SSE; also returned by POST `/regenerate`. New endpoint Wave 34 — replaces Wave 32 v1 inline `STUB_JOB_ID` patterns and adds `brandColors` field (sub-GAP-272k).
**Auth:** Bearer token
**Response 200:** `BrandingJob` schema:
```json
{
  "jobId": "job-abc-123",
  "instanceId": "550e8400-e29b-41d4-a716-446655440000",
  "tenantId": "kitehub-tenant-uuid",
  "status": "DEPLOYED",
  "regenerateCount": 1,
  "brandingVersion": 2,
  "createdAt": "2026-05-07T09:00:00Z",
  "updatedAt": "2026-05-07T10:05:00Z",
  "brandColors": {
    "primary": "#1a73e8",
    "secondary": "#fbbc04",
    "accent": "#10B981",
    "neutral": "#1f2937",
    "background": "#ffffff",
    "source": "TEMPLATE"
  },
  "templateId": "tpl-edu-modern-001",
  "audience": "K-12",
  "tone": "FRIENDLY",
  "previewUrl": "/api/v1/branding/jobs/job-abc-123/preview"
}
```
- `status` ∈ `INITIALIZING | GENERATING | REVIEWING | DEPLOYED | REGENERATING | FAILED` (per `ai-branding-guidelines.md` §6 lifecycle state machine)
- **`brandColors`** (NEW Wave 34, sub-GAP-272k):
  - `primary`, `secondary`, `accent`, `neutral`, `background` — hex strings (`#RRGGBB`); validated via `ThemeColor` value object per `design-patterns.md` §3.2
  - `source` ∈ `TEMPLATE | AI_ANALYSIS | USER_OVERRIDE` — surfaces which path produced the colors (Resource Classification per `ai-branding-guidelines.md` §1)
- `previewUrl` — relative path; FE composes full URL với gateway base
**Errors:**
- 401: unauthenticated
- 404: `{ "error": "JOB_NOT_FOUND", "jobId": "..." }`

---

# Wave branding-100 — Wizard job lifecycle endpoints (GAP-1251)

Core wizard flow served by `BrandingJobV1Controller` tại `/api/v1/branding/jobs`:
**submit job → preview banner → approve/deploy**. Write endpoints chỉ owner-tier
(`OWNER`/`PLATFORM_ADMIN`/`ADMIN`).

## POST /api/v1/branding/jobs
**Use case:** FE vào bước "Generate" → tạo `BrandingJob` thật (status `QUEUED`) gắn với instance tenant của caller (JWT `tenantId` claim), để `jobId` non-empty và preview + deploy-stream hook enable (GAP-1021). Không enqueue heavy AI pipeline (Phase 1 MOCK provisioning).
**Auth:** Bearer token (owner-tier write)
**Request body** (`CreateWizardJobRequest`, mọi field nullable-tolerant):
```json
{
  "organizationName": "Trung tâm Anh ngữ Sky Education",
  "slug": "sky-education",
  "language": "vi",
  "logoUrl": "https://cdn.../logo.png",
  "orgType": "SMALL_CENTER",
  "tone": "FRIENDLY",
  "templateId": "tpl-edu-modern-001"
}
```
**Response 201:** `BrandingJobResponse` (cùng shape với `GET /jobs/{jobId}` — gồm `jobId` + `brandColors`).
**Errors:**
- 400: `{ "error": "TENANT_CONTEXT_REQUIRED", "message": "Không xác định được trung tâm từ phiên đăng nhập" }` — thiếu/invalid JWT tenant claim
- 401/403: unauthenticated / non-owner role

## POST /api/v1/branding/jobs/preview-banner
**Use case:** Bước "Generate & Live Preview" — compose + rasterise banner WebP cho owner xem trước deploy. Stateless: KHÔNG ghi DB. Mode mặc định `TEMPLATE` (HTML compose → renderer; miễn phí, không bao giờ trừ quota). Opt-in `mode:"FULL_AI"` (GAP-1147/1135) cho PREMIUM/ENTERPRISE — gate **server-side**.
**Auth:** Bearer token (owner-tier write) + header `X-Subscription-Tier` (gateway-inject; mặc định `FREE`)
**Request body** (`PreviewBannerRequest`):
```json
{
  "organizationName": "Trung tâm Sky",
  "copy": "Học giỏi cùng Sky",
  "logoUrl": "https://cdn.../logo.png",
  "portraitUrls": ["https://cdn.../teacher1.png"],
  "themeIcon": "📚",
  "colours": { "primary": "#1E40AF", "secondary": "#F59E0B", "accent": "#F59E0B", "neutral": "#0F172A", "background": "#FFFFFF", "source": "TEMPLATE" },
  "mode": "TEMPLATE"
}
```
**Response 200:**
```json
{ "bannerUrl": "https://.../banner.webp", "mode": "TEMPLATE", "fallbackReason": null }
```
- `mode` ∈ `TEMPLATE | FULL_AI` — mode HIỆU LỰC resolve server-side (không bao giờ tin mode client gửi).
- `bannerUrl` — nullable (FE fallback logo/placeholder). Khi `mode=FULL_AI` được grant, đây là URL ảnh GPT image-gen thật (GAP-1135); ngược lại là output TEMPLATE composer.
- `fallbackReason` (chỉ xuất hiện khi `FULL_AI` được request nhưng fallback về `TEMPLATE`):

  | `fallbackReason` | Nghĩa | Trừ quota? |
  |---|---|:---:|
  | `TIER_NOT_ELIGIBLE` | tier FREE/BASIC — FULL_AI không khả dụng | Không |
  | `NOT_AVAILABLE` | flag image-gen tắt HOẶC provider mock-mode (chưa có key thật) — guard consumer-trust GAP-1218 | Không |
  | `QUOTA_EXHAUSTED` | hết quota FULL_AI tháng của PREMIUM | Không |
  | `GENERATION_FAILED` | đã grant + gọi nhưng image-gen không trả output dùng được | Không |

  Quota FULL_AI (`FullAiQuotaService`) CHỈ bị trừ khi banner thật được sinh (`mode=FULL_AI` + không có `fallbackReason`).

## POST /api/v1/branding/jobs/{jobId}/approve
**Use case:** Bước "Deploy" — chạy quality gate (≥70, GAP-1217) rồi trigger async MOCK deploy provisioning; SSE `deploy-stream` surface tiến trình live. Trả `202` ngay.
**Auth:** Bearer token (owner-tier write)
**Request body** (`ApproveDeployRequest`): `{ "slug": "sky-education", "templateId": "tpl-...", "approvedResources": ["logo","colors","banner"] }`
**Response 202:**
```json
{ "jobId": "...", "status": "INITIALIZING", "frontendUrl": "https://sky-education.kiteclass.vn", "qualityScore": 88, "message": "Đang triển khai (mock provisioning)" }
```
**Errors:**
- 404: `{ "error": "JOB_NOT_FOUND", "jobId": "..." }`
- 422: `{ "error": "QUALITY_GATE_FAILED", "jobId": "...", "score": 55, "threshold": 70, "issues": [...], "message": "Điểm chất lượng 55/100 dưới ngưỡng 70 — không thể triển khai tự động" }` — asset score dưới gate; job mark FAILED, không provisioning.

## DEPRECATED — `/api/platform/branding/jobs/**` (legacy `BrandingJobController`, GAP-1252)

Legacy `BrandingJobController` (`@Deprecated`, GAP-1252) trả raw entity `BrandingJob` tại `/api/platform/branding/jobs` (POST/GET/GET `{id}`/GET `{id}/assets`/DELETE `{id}`). **Caller mới PHẢI dùng path v1 ở trên** (`/api/v1/branding/jobs`) trả DTO contract-stable `BrandingJobResponse`. Path legacy chỉ giữ cho client legacy đang chạy, sẽ remove sau khi migrate xong.

---

## Approval endpoints (TBD — UC-AIB-10)

> **Note:** `RebrandApprovalService` exists (Wave 3 Sub-PR 3.5, GAP-070) but REST controller chưa được landed. Khi landed sẽ thêm vào doc:
>
> - `POST /api/v1/instances/{id}/rebrand-approval` — Admin 1 request approval (BR-APRV)
> - `POST /api/v1/rebrand-approvals/{approvalId}/approve` — Admin 2 approve (BR-APRV-002)
> - `POST /api/v1/rebrand-approvals/{approvalId}/reject`
> - `GET /api/v1/instances/{id}/rebrand-approvals` — list approvals for instance

Tracker: see GAP-070 follow-up controller PR.
