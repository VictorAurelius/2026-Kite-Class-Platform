# GAP-1361: Email/Zalo/VietQR external client thiếu @CircuitBreaker

**Status:** 🔵 OPEN
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

- [ ] VietQRService có `@CircuitBreaker` + fallback
- [ ] Synchronous external client (BrandingClient/Resend nếu sync) có CB hoặc documented async-isolation
- [ ] resilience4j config khai báo instance tương ứng

## Related

- Discovered in: 2026-06-14 performance audit (F-005)
- KHÁC GAP-776/918 (gateway CB cold-start — các client này thiếu CB hoàn toàn)
