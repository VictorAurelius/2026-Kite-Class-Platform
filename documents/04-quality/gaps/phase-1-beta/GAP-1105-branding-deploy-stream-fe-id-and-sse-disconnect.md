# GAP-1105: AI Branding deploy-stream FE — jobId-as-instanceId + SSE STREAM_DISCONNECTED

**Status:** 🔵 OPEN
**Priority:** 🟠 P1
**Domain:** Frontend
**Found:** 2026-06-09 (G2 browser-walk wizard Step 6 deploy — instance dd5d0f56)
**Affects:** kitehub-frontend `components/branding/wizard/` (DeployingStep / LifecycleInline / useLifecycleEvents / useDeployStream), Step6Preview id wiring

## Problem

Browser-walk wizard AI Branding Step 6 (approve → deploy) lộ 2 bug FE mà curl-walk D (GAP-1021) miss (bài học `g1-browser-walk-before-flip`):

1. **jobId truyền nhầm chỗ instanceId** — FE gọi `GET /api/v1/branding/instances/{instanceId}/lifecycle/events` với **jobId** `dd5d0f56` (UI label "Instance dd5d0f56") thay vì instanceId thật `7862ab7e`. Empirical: lifecycle/events với jobId → **0 events**; với instanceId thật → 10 events. → panel "Tiến trình" kẹt "Đang khởi tạo", không bao giờ thấy DEPLOYED. Wiring: `Step6Preview/DeployingStep` truyền jobId vào prop `instanceId`. instanceId thật có trong `BrandingJobResponse.tenantId` (= tenant claim) → nên dùng giá trị đó.

2. **SSE deploy-stream STREAM_DISCONNECTED + UNKNOWN** — `useDeployStream` EventSource fire `error` lúc connect (17:22:03, TRƯỚC provision 17:22:04) → emit `{errorCode:'STREAM_DISCONNECTED'}`. Nghi: token-in-query `?token=` auth qua gateway SSE route fail HOẶC EventSource immediate error. Cần browser Network tab inspect (status SSE request) để xác định root cause.

Backend provision THÀNH CÔNG (lifecycle → DEPLOYED `toan-master.kitehub.me` 17:22:08) — chỉ FE display kẹt. Bug 1 (42P18 500) đã fix riêng + verified (commit branding repo).

## Proposed Fix

1. Truyền `instanceId` thật (từ `BrandingJobResponse.tenantId` / wizard state) vào DeployingStep prop, KHÔNG dùng jobId. Deploy-stream SSE giữ jobId (đúng route `/jobs/{jobId}/deploy-stream`).
2. Browser Network inspect SSE `/jobs/{jobId}/deploy-stream?token=...` → xác định 401/timeout/route → fix gateway SSE auth (token-in-query) hoặc EventSource retry.

## Acceptance Criteria

- [ ] Browser-walk Step 6: lifecycle "Tiến trình" hiển thị NOT_STARTED→INITIALIZING→GENERATING→DEPLOYED (không kẹt)
- [ ] SSE deploy-stream connect 200, nhận progress + complete event (không STREAM_DISCONNECTED)
- [ ] Browser network: lifecycle/events gọi với instanceId thật (không jobId)
- [ ] Re-walk full wizard → DEPLOYED + quality gate, no error toast

## Related

- Bug 1 (42P18) fixed: branding `BrandingLifecycleEventRepository` (commit this session)
- GAP-1021 (Agent D deploy pipeline) — flagged needs-rework: curl-walk PASS nhưng browser-walk FAIL
- Rule `g1-browser-walk-before-flip.md` (browser-real walk điều kiện cần G1)
