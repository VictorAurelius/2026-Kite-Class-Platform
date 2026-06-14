# GAP-1361: Email/Zalo/VietQR external client thiếu @CircuitBreaker

**Status:** 🟢 DONE
**Priority:** 🟡 P2
**Domain:** Backend
**Found:** 2026-06-14 (Performance full audit post wave-p0-closeout-1, sub-check 5.3)
**Affects:** `kitehub-email` (ResendEmailService, ZaloOAClient, BrandingClient), `kitehub-subscription` (VietQRService, EmailServiceClient)

## Problem

Các external client sau KHÔNG có `@CircuitBreaker`:
- `kitehub-email`: `ResendEmailService` (Resend API), `ZaloOAClient` (Zalo OA), `BrandingClient` (cross-service WebClient)
- `kitehub-subscription`: `VietQRService` (VietQR generation), `EmailServiceClient`

Chỉ AI client (core + branding) + gateway có circuit breaker. Nếu Resend/Zalo/VietQR down/chậm, không có CB để fail-fast → request thread chờ tới timeout, có thể tích lũy.

**Giảm nhẹ:** email phần lớn đi ASYNC qua RabbitMQ broker (EmailConsumer) → isolation kiểu bulkhead; các client có timeout WebClient. → severity P2 (không P0).

## Proposed Fix

Thêm `@CircuitBreaker` + fallback cho các external call đồng bộ (đặc biệt VietQR vì là path payment user-facing). Đăng ký instance trong resilience4j config. Email async-broker path có thể chỉ cần document isolation, không bắt buộc CB.

## Acceptance Criteria

- [x] VietQRService có `@CircuitBreaker` + fallback
- [x] Synchronous external client (BrandingClient/Resend nếu sync) có CB hoặc documented async-isolation
- [x] resilience4j config khai báo instance tương ứng

## Resolution (2026-06-15, branch fix/audit-fixE-perf-2026-06-14)

- **VietQRService** (path payment user-facing): thêm `@CircuitBreaker(name="vietqr",
  fallbackMethod="generateQRCodeFallback")` lên `generateQRCode(UUID,Long,String)`. Fallback trả
  public VietQR image URL (cùng shape catch-block hiện có) khi CB OPEN hoặc lỗi escape. Thêm
  dependency `resilience4j-spring-boot3:2.4.0` vào `kitehub-subscription` pom + khối
  `resilience4j.circuitbreaker.instances.vietqr` vào application.yml.
- **Documented async-isolation** (không cần CB) cho các sync client còn lại:
  - `kitehub-email` `BrandingClient` — Netty connect+response timeout (GAP-131) + graceful
    fallback `defaultBranding()`; email đi async qua RabbitMQ → không nằm trên request thread.
  - `kitehub-email` `ResendEmailService` — invoke off `EmailConsumer` (broker = bulkhead-style
    isolation) + RestTemplate timeout + broker retry/DLQ.
  - `kitehub-subscription` `EmailServiceClient` — outbox→RabbitMQ async (queue mode default);
    direct-HTTP chỉ dev/test với bounded timeout.
  - `ZaloOAClient` — interface scaffold (mock impl Phase 1), chưa có live HTTP call để CB.
- Test: `VietQRServiceTest.generateQRCode_hasCircuitBreaker` + `generateQRCodeFallback_returnsPublicImageUrl`.
  Existing VietQR tests (RestClientException→fallback, error-code→RuntimeException) vẫn PASS
  (CB inactive trong Mockito unit test, internal logic giữ nguyên). Subscription module BUILD SUCCESS.

## Related

- Discovered in: 2026-06-14 performance audit (F-005)
- KHÁC GAP-776/918 (gateway CB cold-start — các client này thiếu CB hoàn toàn)
