# GAP-1336: FE→BE method drift — generate-theme FE GET vs BE POST (405)

**Status:** 🟢 DONE
**Priority:** 🟡 P2
**Domain:** Mixed
**Found:** 2026-06-14 (API-contract full audit, AUDIT-2026-06-14-api-contract-full)
**Affects:** `use-theme-generation.ts` (kitehub-frontend) ↔ kitehub-branding AIBrandingController

## Problem

`check-fe-be-api-contract.sh` flag: FE `use-theme-generation.ts:26` gọi `GET /api/platform/branding/ai/generate-theme` nhưng BE `AIBrandingController.java:185` chỉ expose `@PostMapping("/generate-theme")` (base `/api/platform/branding/ai`). Method mismatch GET vs POST → 405 Method Not Allowed.

Lưu ý: AI branding Phase 1 = TEMPLATE-first by design (mock generation), nên có thể FE hook chưa được dùng hoặc đang ở trạng thái stub. Severity P2 (chưa chắc breakage runtime trong Phase 1).

## Root Cause

FE dùng `apiClient.get` cho endpoint generation vốn là POST (generation có payload + side-effect).

## Proposed Fix

Sửa FE `use-theme-generation.ts` dùng `apiClient.post` với body (logo/brand input) khớp BE `@PostMapping("/generate-theme")`; document trong `ai-branding/api-contract.md`.

## Acceptance Criteria

- [ ] FE generate-theme dùng POST khớp BE — không 405
- [ ] Endpoint documented trong `ai-branding/api-contract.md` (request/response)
- [ ] `check-fe-be-api-contract.sh` không còn flag

## Resolution

🟢 DONE (2026-06-15, branch `fix/audit-fixC-apidocs-2026-06-14`). Root cause kép: (1) raw `fetch` với `method: 'POST'` ở dòng KHÁC dòng `fetch(` → static detector đọc default GET → flag GET-vs-POST; (2) raw fetch KHÔNG gắn `Authorization`/`X-Tenant-Id` → endpoint `@PreAuthorize(OWNER_AUTHZ)` sẽ 401 runtime.

Sửa FE `kitehub-frontend/src/hooks/use-theme-generation.ts`: chuyển sang `apiClient.post('/api/platform/branding/ai/generate-theme', analysis)` (shared client tự gắn `Authorization` + `X-Tenant-Id`; khớp BE `@PostMapping("/generate-theme")`).

Verify: `check-fe-be-api-contract.sh` hết flag (POST khớp BE POST); `pnpm --filter kitehub-frontend build` PASS. AC: cả 3 ✅.

## Related

- Discovered in: `documents/04-quality/audits/api-contract/2026-06-14-api-contract-full-audit.md` B6
- Detector: `scripts/check-fe-be-api-contract.sh`
- Related: AI branding generation model (TEMPLATE-first Phase 1, ADR-037/026)
