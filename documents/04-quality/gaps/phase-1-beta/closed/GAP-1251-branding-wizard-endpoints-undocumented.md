# GAP-1251: Branding-100 wizard + legacy endpoints undocumented trong api-contract.md

**Status:** 🟢 DONE
**Priority:** 🟠 P1
**Domain:** Backend (API contract drift)
**Found:** 2026-06-12 (post-wave audit suite — api-contract-audit, cadence ui-kits-100 + landing-100, base SHA `1f6baea26`)
**Affects:** `documents/01-business/kitehub/ai-branding/api-contract.md` + `kitehub/kitehub-branding/src/main/**`

## Problem

Wave branding-100 ship/expand bộ endpoint branding wizard nhưng api-contract.md (`ai-branding/` + `kiteclass/branding-api/`) KHÔNG document đầy đủ. Endpoint coverage diff (rubric §2.1.1 P0 — "controller endpoint set − doc endpoint set = 0") FAIL: ~13 endpoint code KHÔNG có entry trong bất kỳ api-contract.md nào.

Endpoint chưa documented (state-check `grep -rln "<path>" documents/01-business/` → 0 file):

| # | Endpoint (code) | Controller:line | Doc hits |
|---|---|---|:---:|
| 1 | `POST /api/v1/branding/jobs` (submit wizard job) | `BrandingJobV1Controller.java:111` | 0 (chỉ có GET `/{jobId}`) |
| 2 | `POST /api/v1/branding/jobs/preview-banner` | `BrandingJobV1Controller.java:169` | 0 |
| 3 | `POST /api/v1/branding/jobs/{jobId}/approve` | `BrandingJobV1Controller.java:251` | 0 |
| 4 | `POST /api/platform/branding/content/generate` | `ContentGenerationController.java:44` | 0 |
| 5 | `GET /api/platform/branding/content/{instanceId}` | `ContentGenerationController.java:69` | 0 |
| 6-10 | `/api/platform/branding/jobs/**` (POST, GET, GET/{id}, GET/{id}/assets, DELETE/{id}) | `BrandingJobController.java:75-168` | 0 |
| 11-13 | `/api/platform/branding/assets/**` (POST upload, GET, DELETE) | `AssetStorageController.java:62-165` | 0 |

Endpoint 1-3 là **lõi luồng wizard branding-100** (submit job → preview banner → approve) — luồng user-facing chính của wave, ship mà không có contract entry → consumer (FE wizard, gateway, third-party) không có spec.

Secondary: `kiteclass/branding-wizard/api-contract.md` chỉ document `POST /api/v1/instances` + `GET /api/v1/instances/{id}` — KHÔNG khớp bộ endpoint wizard thật `/api/v1/branding/jobs/**` ở `kitehub-branding`. Doc-location/ownership confusion (branding-wizard domain doc ở KiteClass nhưng wizard controller ở KiteHub).

## State-check evidence

```
grep -rln "preview-banner|/jobs/{jobId}/approve|branding/content/generate|branding/assets|platform/branding/jobs" documents/01-business/  → 0 files mỗi pattern
grep -rn "@PostMapping" BrandingJobV1Controller.java  → line 111 (root), 169 (preview-banner), 251 ({jobId}/approve)
```

Base SHA `1f6baea26` (đã gồm branding-100 Bucket A/F #2356/2357). Lưu ý: PR #2358/#2359 (BE branding) đang merge mid-audit — reviewer verify lại coverage trên final main; gap này có thể được partial-address nếu các PR đó ship docs.

## Proposed Fix

Document 13 endpoint vào api-contract.md đúng domain: wizard `/api/v1/branding/jobs/**` + `/api/platform/branding/{content,jobs,assets}/**`. Mỗi entry: method + path + request/response schema + error codes + auth model. Reconcile `kiteclass/branding-wizard/api-contract.md` `/api/v1/instances` claim với endpoint thật (hoặc cross-link sang ai-branding nếu wizard thuộc KiteHub).

## Acceptance Criteria

- [x] 13 endpoint trong bảng có entry trong api-contract.md với schema + error codes
- [x] `kiteclass/branding-wizard/api-contract.md` reconcile với wizard controller thật (KiteHub `/api/v1/branding/jobs`)
- [x] api-contract-audit §2.1.1 endpoint-coverage diff = 0 cho branding domain (kitehub-branding)

## Resolution (2026-06-21)

Đóng qua 2 đợt:

**Đợt 1 (branding-100 Bucket B+E, PR mid-audit #2358/#2359 — `1f6baea26` về sau):** 3 core wizard endpoints + bộ wizard lifecycle (slug/quota/regenerate/sse-token/deploy-stream/quality-score/preview/deploy-status/lifecycle-events/GET jobs) đã được document trong `ai-branding/api-contract.md` §"Wave branding-100" + §"Wave 34".

**Đợt 2 (this PR — GAP-1251 closeout):** document 5 endpoint còn lại + reconcile:
- `POST /api/platform/branding/content/generate` (`ContentGenerationController`) — schema `ContentGenerationRequest` → `LandingPageContent`
- `GET /api/platform/branding/content/{instanceId}` — honest 404 (persistence hoãn PR 4.9)
- `POST /api/platform/branding/assets/{instanceId}/{assetType}` (`AssetStorageController`) — multipart → `BrandingAsset` + dedup policy
- `GET /api/platform/branding/assets/{instanceId}` — `BrandingAsset[]`
- `DELETE /api/platform/branding/assets/{instanceId}` — `{status, message}`
- Legacy `BrandingJobController` (`@Deprecated`, GAP-1252): chuyển narrative → 5 entry method+path+auth+error tường minh
- `kiteclass/branding-wizard/api-contract.md`: reconcile Backend-calls → controller THẬT ở `kitehub-branding`, cross-link doc canonical, phân biệt khỏi saga `/api/v1/instances` (`kiteclass-core`)

Verify: mọi endpoint của 9 controller `kitehub-branding` (AIBranding/ContentGeneration/AssetStorage/BrandingJob[legacy]/TemplateGallery/BrandingJobV1/BrandingWizard/DeployStream/Preview/QualityScore/LifecycleEvents) đều có ≥1 entry → endpoint-coverage diff = 0.

**Out-of-scope (not GAP-1251):** `/api/v1/settings/branding` (`kiteclass-core` settings module `BrandingController`) — khác domain (settings, không phải wizard); follow-up riêng nếu audit flag.

## Related

- Discovered in: post-wave audit suite 2026-06-12 (`documents/04-quality/audits/api-contract/2026-06-12-api-contract-audit.md`)
- Sister: GAP-1252 (legacy BrandingJobController dual-mount no @Deprecated)
- Cross-rule: `contract-first-for-cross-layer.md` §3 cross-layer drift; `api-contract-audit/SKILL.md` §2.1.1
