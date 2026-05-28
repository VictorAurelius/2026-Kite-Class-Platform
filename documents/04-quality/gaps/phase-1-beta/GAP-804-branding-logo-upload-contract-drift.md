---
audience: dev
---

# GAP-804 — Branding logo upload FE↔BE contract drift (multipart vs @RequestParam String)

**Status:** 🔵 OPEN
**Priority:** 🟠 P1
**Domain:** Mixed (KiteClass FE branding + BE BrandingController)
**Found:** 2026-05-28 (Persona-simulation outside-in agent — demo-tenant planning)
**Phase:** phase-1-beta
**Affects:** Branding logo upload qua UI (tính năng tùy biến UI cốt lõi của KiteClass) — must-have demo-tenant

## Problem

FE và BE bất đồng contract khi upload logo trường — đúng class BE↔FE contract drift (như GAP-801) nhưng ở **request-shape/content-type** (không phải URL-path nên GAP-802 #2 không bắt):

- **FE** `brandingApi.uploadLogo` (`kiteclass/kiteclass-frontend/src/lib/api/branding.ts:33`) gửi `multipart/form-data` với field `logo` (file) tới `POST /api/v1/settings/branding/logo`.
- **BE** `BrandingController.uploadLogo` (`kiteclass-core` `:67`) nhận `@RequestParam("fileUrl") String` — mong đợi một presigned URL / path string, KHÔNG nhận multipart file.

→ Upload logo qua UI **fail (400/415)** — FE gửi file, BE đòi string `fileUrl`. Tính năng "tải logo trường" (nhận diện thương hiệu mạnh nhất, must-have demo #7) không dùng được qua giao diện.

Phân biệt: KHÔNG phải GAP-798b (StorageController fail-closed) — `BrandingController` tách hẳn khỏi StorageController, là OWNER-scoped admin path. Đây là contract mismatch thuần FE↔BE.

## Proposed Fix

Chọn 1 trong 2 hướng (cần xác định luồng upload thật):
- **Hướng A (FE→BE upload trực tiếp):** đổi BE `uploadLogo` nhận `@RequestPart MultipartFile logo`, lưu MinIO, trả `logoUrl`. FE giữ nguyên multipart.
- **Hướng B (presigned URL 2 bước):** FE xin presigned URL trước (BE issue), PUT file lên MinIO, rồi gọi `/branding/logo` với `fileUrl` string. FE phải đổi sang flow 2 bước.

Kèm: caller sweep theo `api-contract-change-caller-sweep.md` + test (FE component test + BE MVC/IT) + VN diacritic-safe filename.

**Workaround demo (GAP-805 wave):** seed `logoUrl` thẳng DB (pattern `BrandingDataSeeder` dùng `/mocks/assets/...`) — KHÔNG chặn demo trong khi chờ fix này.

## Acceptance Criteria

- [ ] Xác định luồng upload chính thức (A hay B) + sửa contract cho khớp
- [ ] Caller sweep + test FE + test BE (per `api-contract-change-caller-sweep.md`)
- [ ] Live walk: owner upload logo qua UI → 200 + logo hiển thị (per `feature-ship-runtime-walk-mandate.md`)

## Related

- **GAP-802 / GAP-801** — BE↔FE contract drift class (URL-path); GAP-804 là sub-class request-shape mà #2 detector không cover
- `api-contract-change-caller-sweep.md` — sweep callers khi đổi contract
- `feature-ship-runtime-walk-mandate.md` — walk owner logo upload trước DONE
- Demo-tenant wave (GAP-805) — workaround seed logoUrl direct

## Log

- **2026-05-28:** Filed từ Persona-simulation outside-in agent (demo-tenant planning). Agent verify GAP-798b KHÔNG chặn branding (public no-auth getBranding/getThemeConfig), nhưng phát hiện logo upload contract drift riêng. Class request-shape mismatch — GAP-802 #2 (URL-path checker) không cover → gợi ý mở rộng detector tương lai (content-type/param-shape drift).
