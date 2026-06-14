# GAP-1334: FE→BE drift — `GET /api/v1/instance/config` không có BE mapping (feature-detection 404)

**Status:** 🟡 PARTIAL
**Priority:** 🟠 P1
**Domain:** Mixed
**Found:** 2026-06-14 (API-contract full audit, AUDIT-2026-06-14-api-contract-full)
**Affects:** `useFeatureDetection.ts` (kiteclass-frontend) ↔ kiteclass-core InstanceController

## Problem

`check-fe-be-api-contract.sh` flag (drift thật): FE feature-detection hook query `GET /api/v1/instance/config` (`useFeatureDetection.ts:17`) nhưng kiteclass `InstanceController` base path = `/api/v1/instances` (số nhiều, `InstanceController.java:40`) và KHÔNG có endpoint `/config`. Kết quả → 404; hook có `retry: 2` + `staleTime` → feature-gating fallback im lặng (`hasFeature` luôn false khi `config` null) → tính năng theo tier có thể bị ẩn sai.

## Root Cause

FE call-site path không khớp BE mapping nào — endpoint `/api/v1/instance/config` chưa được implement, hoặc FE phải gọi endpoint khác (vd subscription tier config qua gateway).

## Proposed Fix

Xác định nguồn config đúng: (a) implement `GET /api/v1/instance/config` trên kiteclass-core (hoặc gateway proxy) trả `InstanceConfig` (tier + feature flags); HOẶC (b) sửa FE gọi endpoint hiện có. Document endpoint trong `instance-lifecycle/api-contract.md` hoặc `tenant-settings/api-contract.md`.

## Acceptance Criteria

- [ ] FE `/api/v1/instance/config` resolve 200 với `InstanceConfig` shape (tier + features) — KHÔNG 404
- [ ] Endpoint documented trong api-contract.md tương ứng
- [ ] `check-fe-be-api-contract.sh` không còn flag path này

## Resolution

🟡 PARTIAL (2026-06-15, branch `fix/audit-fixC-apidocs-2026-06-14`). Điều tra: `GET /api/v1/instance/config` KHÔNG khớp BE mapping nào — kiteclass `InstanceController` base `/api/v1/instances` (số nhiều), không có sub-path `/config`; và KHÔNG có endpoint BE hiện hữu nào trả đúng shape `InstanceConfig { tier, features }`. Đây là **feature gap** (cần implement BE endpoint), KHÔNG phải FE call-site sai → **không sửa FE** (chuyển FE sang endpoint khác sẽ phá `config.features[...]` access pattern).

Đã làm (doc): document drift + planned endpoint + 2 phương án (implement BE `GET /api/v1/instance/config` trả `InstanceConfig`, hoặc FE đọc tier từ JWT claim / gateway `X-Subscription-Tier`) trong `kiteclass/instance-lifecycle/api-contract.md` §"Planned".

Còn lại (defer — feature, ngoài scope api-contract docs PR): implement BE endpoint → AC #1 (200 `InstanceConfig`) + AC #3 (detector hết flag) chưa đạt. `check-fe-be-api-contract.sh` vẫn flag path này (đúng — BE chưa expose). Vì là feature mới (không phải minor mapping), defer sang wave feature riêng.

## Related

- Discovered in: `documents/04-quality/audits/api-contract/2026-06-14-api-contract-full-audit.md` B3
- Detector: `scripts/check-fe-be-api-contract.sh`
- Related: AI branding generation model (tier propagate qua gateway X-Subscription-Tier, ADR-039)
