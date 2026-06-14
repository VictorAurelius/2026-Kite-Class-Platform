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

## Planned: GET /api/v1/instance/config (GAP-1334 — feature gap, chưa implement)

FE feature-detection hook `kiteclass-frontend/src/hooks/useFeatureDetection.ts` query
`GET /api/v1/instance/config` (số **ít**) để lấy `InstanceConfig { tier, features }` cho feature-gating theo subscription tier. **Endpoint này CHƯA tồn tại** trên `InstanceController` (base `/api/v1/instances`, số **nhiều** — không có sub-path `/config`) → 404 → hook fallback im lặng (`hasFeature` luôn `false`).

**Trạng thái:** GAP-1334 **PARTIAL** — đây là **feature gap** (cần implement BE endpoint), không phải FE call-site sai. Không có endpoint BE hiện hữu nào trả đúng shape `{ tier, features }` để FE chuyển hướng tới. Phương án (deferred):
- (a) Implement `GET /api/v1/instance/config` trên kiteclass-core (hoặc gateway proxy) trả `InstanceConfig` (tier + feature flags) — tier có thể đọc từ gateway header `X-Subscription-Tier` (ADR-039); HOẶC
- (b) FE đọc tier từ JWT claim / gateway header thay vì gọi endpoint riêng.

Khi implement xong: document shape `InstanceConfig` tại đây + `check-fe-be-api-contract.sh` sẽ hết flag path này.

## Log
- 2026-06-14 — GAP-1334: ghi nhận planned `GET /api/v1/instance/config` (feature gap, PARTIAL).
- 2026-04-14 — Initial API contract
