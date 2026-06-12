# Instance Lifecycle — API Contract

> Endpoints scaffolded at service layer; REST controllers added in a follow-up PR.
> Contract documented here to lock semantics for consumers.

## POST /api/v1/instances
Initiate provisioning.
**Request:**
```json
{ "tenantId": "t-abc", "slug": "acme" }
```
**Response 201:**
```json
{ "id": 1, "tenantId": "t-abc", "slug": "acme", "status": "INITIALIZING" }
```
**Errors:** 400 slug already in use.

## POST /api/v1/instances/{id}/infrastructure-ready
Mark infrastructure ready → status=GENERATING.
**Response 200:** FrontendInstance

## POST /api/v1/instances/{id}/branding-completed
**Request:** `{ "frontendUrl": "https://acme.kitehub.me" }` (optional)
**Response 200:** FrontendInstance (status=DEPLOYED, brandingVersion++)

## POST /api/v1/instances/{id}/rebrand
Trigger rebrand (status=DEPLOYED → REGENERATING).

## POST /api/v1/instances/{id}/failed
**Request:** `{ "reason": "AI provider 500" }`
**Response 200:** FrontendInstance (status=FAILED, retryCount++)

## POST /api/v1/instances/{id}/retry
Retry failed instance (status=FAILED → INITIALIZING).
**Errors:** 409 MAX_RETRIES exceeded.

## GET /api/v1/instances/{id}
Returns FrontendInstance.

## GET /api/v1/instances?status=FAILED
List instances by status (admin dashboard).

## Error model

| Code | Meaning |
|------|---------|
| 400 | Bad input (slug in use) |
| 404 | Instance not found |
| 409 | Invalid state transition OR MAX_RETRIES |

## Log
- 2026-04-14 — Initial API contract
