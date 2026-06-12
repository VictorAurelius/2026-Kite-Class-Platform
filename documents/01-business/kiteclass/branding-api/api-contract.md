# Branding API — Contract

## Instance Lifecycle

### POST /api/v1/instances
**Request:**
```json
{ "tenantId": "t-abc", "slug": "acme" }
```
**Response 201:** `ApiResponse<InstanceResponse>`
**Errors:** 400 validation; 400 slug already in use

### GET /api/v1/instances/{id}
**Response 200:** `ApiResponse<InstanceResponse>`
**Errors:** 400 not found

### GET /api/v1/instances?status={STATUS}
**Response 200:** `ApiResponse<List<InstanceResponse>>`

### POST /api/v1/instances/{id}/infrastructure-ready
**Response 200:** `ApiResponse<InstanceResponse>` (status=GENERATING)

### POST /api/v1/instances/{id}/branding-completed
**Request (optional):**
```json
{ "frontendUrl": "https://acme.kitehub.me" }
```
**Response 200:** `ApiResponse<InstanceResponse>` (status=DEPLOYED, brandingVersion++)

### POST /api/v1/instances/{id}/rebrand
**Response 200:** `ApiResponse<InstanceResponse>` (status=REGENERATING)

### POST /api/v1/instances/{id}/failed
**Request:**
```json
{ "reason": "AI timeout" }
```
**Response 200:** `ApiResponse<InstanceResponse>` (status=FAILED, retryCount++)

### POST /api/v1/instances/{id}/retry
**Response 200:** `ApiResponse<InstanceResponse>` (status=INITIALIZING)
**Errors:** 409 when retryCount ≥ MAX_RETRIES

## Branding Package (Composite + ETag)

### GET /api/v1/branding/{instanceId}/package
**Headers (optional):**
- `If-None-Match: W/"v7-abc"` → may return 304

**Response 200:**
```json
{
  "instanceId": 10,
  "tenantId": "t-abc",
  "slug": "acme",
  "frontendUrl": "https://acme.kitehub.me",
  "brandingVersion": 7,
  "deployedAt": "2026-04-14T00:00:00Z",
  "assets": [
    { "type": "LOGO", "category": "STATIC", "url": "...", "alt": null }
  ]
}
```
**Response headers:** `ETag: W/"v7-<hash>"`, `Content-Type: application/json`

**Response 304:** no body (FE uses cached copy)

## Internal Webhooks

### POST /internal/notify/instance-deployed?instanceId={id}
**Headers:** `X-Internal-Caller` (optional; for audit log)
**Response 200:** `ApiResponse<String>` `"evicted"`
**Errors:** 400 missing instanceId

## Error Model

| Code | Reason |
|------|--------|
| 400 | Validation failure (invalid slug, missing fields) OR instance not found |
| 404 | Route not found |
| 409 | Invalid state transition OR retry exhausted |
| 500 | Unexpected server error |

## InstanceResponse schema

```java
record InstanceResponse(
    Long id,
    String tenantId,
    String slug,
    String frontendUrl,
    FrontendInstanceStatus status,
    Integer retryCount,
    String failureReason,
    Integer brandingVersion,
    Instant initializingAt,
    Instant generatingAt,
    Instant deployedAt,
    Instant lastRegenerateAt,
    Instant failedAt);
```

## Log
- 2026-04-14 — Initial contract
