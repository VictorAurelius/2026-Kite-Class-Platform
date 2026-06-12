# GAP-1021: Branding job assets không persist thành active theme + SSE preview/deploy auth qua EventSource

**Status:** 🔵 OPEN
**Priority:** 🟠 P1
**Domain:** Mixed
**Found:** 2026-06-06 (KH-6 AI Branding wizard G1 walk)
**Affects:** kitehub-branding (job apply persistence + SSE controllers) + FE wizard

## Problem

KH-6 G1 walk surface 2 "apply/approval" gap:

1. **Job assets không persist thành active theme (FM-7):** Branding job (`POST /jobs`) generate đầy đủ assets (`GET /jobs/{id}/assets` trả marketingCopy + logos + hero + og + profile variants) NHƯNG không có endpoint approve/apply để persist assets job thành theme active của instance. Chỉ `POST /templates/{id}/apply` persist (template-based, không phải AI-generated job output). Wizard flow "generate (job) → preview → approve per resource" (per `ai-branding-guidelines.md` §4.2) dead-end ở bước approve — job COMPLETED nhưng không có đường activate kết quả. `generate-theme` cũng chỉ trả JSON, không persist.

2. **SSE preview/deploy auth (FM-4):** `PreviewController` + `DeployStreamController` dưới `/api/v1/branding/jobs/**` require auth, nhưng browser `EventSource`/iframe KHÔNG gửi được `Authorization`/`X-User-*` header → gateway 401, stream không kết nối. (Walk chưa test SSE trực tiếp — predicted; cần token-in-query hoặc cookie auth cho SSE.)

## Root Cause

(1) Thiếu job-approval endpoint persist `BrandingJob` assets → instance active theme + lifecycle DEPLOYED. (2) SSE endpoint dùng header-auth không tương thích EventSource (không set custom header).

## Proposed Fix

1. Thêm `POST /jobs/{id}/approve` (hoặc per-resource approve per §4.2) persist assets → instance theme + transition lifecycle GENERATING→DEPLOYED qua `InstanceLifecycleService` + quality gate §5.
2. SSE auth: token-in-query-param (short-lived) hoặc cookie-based cho `/jobs/{id}/preview` + `deploy-stream`; gateway whitelist SSE path với query-token verify.

## Acceptance Criteria

- [ ] Job COMPLETED → approve → instance active theme = job assets + lifecycle DEPLOYED
- [ ] SSE preview/deploy stream kết nối được từ browser EventSource (auth qua query-token/cookie)
- [ ] Quality gate §5 chạy trước DEPLOYED (score ≥70)

## Related

- Discovered in: KH-6 G1 walk — `documents/04-quality/audits/persona-review/2026-06-06-pre-walk-kh6-ai-branding-wizard.md` (FM-7 + FM-4)
- Related: `ai-branding-guidelines.md` §4.2 preview+approve + §5 quality gate + §6 lifecycle

## Log

- **2026-06-07** (Wave g2-blockers-1 Bucket B — investigation, NOT fixed): Để OPEN — security-config surgery + new endpoint, không rush vào high-context. Findings cho next session:
  - **Part 1 (job approve/apply persist theme):** `BrandingJobV1Controller` (`wizard/BrandingJobV1Controller.java`) hiện chỉ có `getJob` (GET /{jobId}) — KHÔNG có approve/apply endpoint → wizard approve dead-end. Cần thêm POST approve/apply persist generated theme thành instance active branding (study `BrandingJobService` + cách template-apply persist hiện tại để mirror persistence path).
  - **Part 2 (SSE token-in-query):** SSE endpoint `DeployStreamController:81` `GET /{jobId}/deploy-stream` produces `text/event-stream`, dùng `@RequestHeader Last-Event-ID`. Auth qua gateway JWT→header (SecurityConfig); EventSource KHÔNG gửi được Authorization header → 401. Fix = permit SSE path trong `SecurityConfig` (line 71+ chain) + validate JWT từ query param `?token=` trong 1 filter. **RISK:** sửa SecurityConfig sai = mở endpoint không auth → cần cẩn thận + IT.
  - **Status 🔵 OPEN** — deferred Bucket B; SecurityConfig surgery + new endpoint design.
- **2026-06-11:** Design source: `ui_kits/ai-branding-wizard-v2/v3/screens/step8-banner-generating.html` + `step9-preview.html` (GAP-1212 DONE 2026-06-11, Wave ui-kits-100 Bucket D) — SSE preview/deploy progress log realtime làm 基本設計 layer.
- **2026-06-12** (gateway SSE whitelist — coordinator inline): `JwtAuthenticationGatewayFilter.isPublicPath` thêm match `GET /api/v1/branding/jobs/*/deploy-stream` — EventSource không set được Authorization header; gateway pass-through, auth enforced SERVICE-SIDE bởi `SseQueryTokenAuthFilter` (HMAC `?access_token=`). Mint endpoint + mọi path branding khác giữ JWT. TDD: 4 assertion mới (`deploy-stream` true / `sse-token` false / job-path false / suffix-extra false), suite 14/14 PASS. Còn: per-resource approve FE (Bucket E) + runtime SSE browser-walk.
