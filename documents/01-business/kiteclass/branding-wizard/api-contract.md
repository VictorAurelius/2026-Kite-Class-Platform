# Branding Wizard — API Contract

## Frontend SPI

### `BrandingWizard` component

```tsx
<BrandingWizard
  tier="PRO"            // Tier enum from session
  tenantId="t-abc"
  slug="my-school"
/>
```

Mounts at route `/(dashboard)/branding/wizard`.

### `useBrandingWizard(tier, tenantId, slug)` hook

Returns:
```ts
{ state: WizardState; send: (e: WizardEvent) => void; submit: () => Promise<void> }
```

### Pure reducer

```ts
reducer(state: WizardState, event: WizardEvent): WizardState
```

100% pure; unit-tested via `__tests__/wizard-machine.test.ts`.

## Backend calls

> **⚠️ Ownership note (GAP-1251):** Controller wizard THẬT nằm ở **KiteHub** service
> `kitehub-branding` (`BrandingJobV1Controller` + `BrandingWizardController` + `DeployStreamController`
> tại `/api/v1/branding/jobs/**` và `/api/v1/branding/**`), KHÔNG phải `kiteclass-core`.
> Hợp đồng đầy đủ (request/response/error/SSE event) là **source-of-truth duy nhất** tại
> [`documents/01-business/kitehub/ai-branding/api-contract.md`](../../kitehub/ai-branding/api-contract.md)
> §"Wave 34 — AI Branding Wizard endpoints" + §"Wave branding-100 — Wizard job lifecycle endpoints".
> File này (KiteClass branding-wizard) chỉ mô tả **FE SPI** (component/hook/reducer) — không
> duplicate spec endpoint để tránh drift 2 nguồn.

Luồng wizard (submit → preview → deploy) gọi các endpoint KiteHub branding sau (chi tiết schema xem doc canonical ở trên):

| Bước | Method | Path | Ghi chú |
|------|--------|------|---------|
| tạo job | POST | /api/v1/branding/jobs | `CreateWizardJobRequest` → `BrandingJobResponse` (201) |
| live preview banner | POST | /api/v1/branding/jobs/preview-banner | TEMPLATE/FULL_AI, stateless |
| poll job | GET | /api/v1/branding/jobs/{jobId} | `BrandingJobResponse` (brandColors) |
| approve/deploy | POST | /api/v1/branding/jobs/{jobId}/approve | quality-gate ≥70 → 202 |
| live progress (SSE) | GET | /api/v1/branding/jobs/{jobId}/deploy-stream | EventSource `?access_token=` |
| check slug | GET | /api/v1/branding/slug-availability | own-subdomain exempt |
| regenerate quota | GET | /api/v1/branding/regenerate-quota | tier cap |

> **Legacy lưu ý:** `POST /api/v1/instances` + `GET /api/v1/instances/{id}` (Wave 3 Sub-PR 3.4,
> `kiteclass-core` `InstanceController`) là saga provisioning lifecycle — KHÁC wizard job flow ở trên.
> Spec của chúng nằm tại [`kiteclass/branding-api/api-contract.md`](../branding-api/api-contract.md).

## Types

See `src/components/branding/wizard/types.ts` — complete discriminated-union for
`WizardState`, `WizardEvent`, `Tier`, `Segment`, `BrandInputs`.

## Log
- 2026-06-21 — GAP-1251: reconcile Backend calls section với wizard controller THẬT ở `kitehub-branding` (`/api/v1/branding/jobs/**`); cross-link doc canonical `kitehub/ai-branding/api-contract.md`; phân biệt khỏi saga `/api/v1/instances` (`kiteclass-core`).
- 2026-04-14 — Initial contract
