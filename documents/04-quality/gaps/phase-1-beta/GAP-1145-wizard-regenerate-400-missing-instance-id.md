# GAP-1145: Wizard "Tạo lại" (regenerate) → 400 do FE thiếu header `X-Instance-Id`

**Status:** 🟡 PARTIAL — fix shipped PR #2289, pending G2 browser re-walk
**Priority:** 🟡 P2
**Domain:** Frontend (+ gateway header contract)
**Found:** 2026-06-10 (Wizard Step 7 G2 browser-walk — enhancement wave-wizard-step7, PR #2289)
**Affects:** `kitehub-frontend` `useRegenerateQuota.ts` · `kitehub-branding` `BrandingWizardController.regenerate`

## Problem

Trong G2-walk Bước 7 (AI Branding wizard), nhấn nút **"Tạo lại"** (RegenerateCounter) → `POST :9000/api/v1/branding/jobs/{jobId}/regenerate` trả **HTTP 400** (console lặp lại nhiều lần).

Root cause (state-check):
- `BrandingWizardController.regenerate` (`controller/BrandingWizardController.java:108-119`) trả `400 MISSING_INSTANCE_ID` khi header `X-Instance-Id` null. (`Idempotency-Key` ĐÃ được gửi — `useRegenerateQuota.ts:76`.)
- `useRegenerateQuota.ts:69-79` regenerate mutation chỉ gửi `Idempotency-Key`, **KHÔNG gửi `X-Instance-Id`**.
- `kitehub-gateway` map `tenantId → X-Tenant-Id` nhưng **KHÔNG set `X-Instance-Id`** → header này luôn null nếu FE không tự gửi → 400 cho mọi caller.

Lỗi PRE-EXISTING (Wave 34 GAP-272d), không phát sinh từ enhancement wave-wizard-step7 (`useRegenerateQuota` + `RegenerateCounter` được giữ nguyên).

**Tầng 2 (sâu hơn):** kể cả khi thêm `X-Instance-Id`, job wizard Phase-1 là **mock** (status QUEUED→INITIALIZING, không bao giờ DEPLOYED mid-flow) → regenerate sẽ tiếp tục `409 INVALID_JOB_STATE` ("regenerate only allowed from DEPLOYED/FAILED"). Tức regenerate concept không khớp giai đoạn preview-trước-deploy của wizard. Cần product decision: (a) ẩn nút regenerate ở Bước 7 (live banner preview mới đã tự re-generate khi đổi input), hoặc (b) cho regenerate apply lên template/preview thay vì job.

## Proposed Fix

1. **Tầng 1 (header):** `useRegenerateQuota` gửi `X-Instance-Id: <instanceId>` (đổi mutation signature `{ jobId, instanceId }`, caller `Step6Preview.handleRegenerateClick` truyền `wizardState.instanceId`). HOẶC gateway `JwtAuthenticationGatewayFilter` inject `X-Instance-Id` từ JWT claim.
2. **Tầng 2 (state):** quyết định scope regenerate trong wizard (ẩn nút mid-flow vs cho regenerate preview). Live banner preview (GAP-1143) đã thay phần lớn nhu cầu "tạo lại banner".

## Acceptance Criteria

- [ ] Regenerate không còn 400 MISSING_INSTANCE_ID (header gửi đúng HOẶC gateway inject).
- [ ] Quyết định + xử lý 409 state mismatch trong wizard mock-flow (ẩn nút / re-scope).
- [ ] Runtime-walk Bước 7 regenerate không lỗi.

## Fix (PR #2289, 2026-06-10)

- **Tầng 1 (header):** `useRegenerateQuota` mutation signature đổi `{ jobId, instanceId }`, gửi header `X-Instance-Id` (+ test `sends the X-Instance-Id header on regenerate`). `Step6Preview.handleRegenerateClick` truyền `wizardState.instanceId`; nếu chưa có → toast hướng dẫn (live preview đã thay).
- **Tầng 2 (409 state):** mock job mid-wizard luôn QUEUED → 409. `handleRegenerateClick` thêm `onError` toast friendly thay vì lỗi thô ("Bản xem trước trực tiếp tự cập nhật… Tạo lại áp dụng sau khi triển khai"). RegenerateCounter giữ lại để hiển thị quota tier.
- FE tests 10/10 pass (`useRegenerateQuota.test.tsx` + `Step6Preview-orchestrator-wiring.test.tsx`).
- **Pending:** G2 browser re-walk Bước 7 xác nhận không còn 400/409 lỗi thô.

## Related

- Discovered in: PR #2289 (wave-wizard-step7 G2 walk 2026-06-10)
- GAP-272n (regenerate response-shape, phase-2) — sister regenerate concern
- GAP-1107 (mock-provision rollback + "Runtime-walk REGENERATE x5 pending") — regenerate walk được flag trước đó
