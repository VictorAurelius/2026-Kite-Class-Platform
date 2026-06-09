# GAP-1097: `TwoFactorController.signAccessToken` thiếu `tier` claim (+ `tenantId`) — divergence với AuthService/TokenService

**Status:** 🔵 OPEN
**Priority:** 🟡 P2
**Domain:** Backend
**Found:** 2026-06-09 (tier-enforcement wave — cross-flow sweep DEFER)
**Affects:** `kitehub-subscription` `TwoFactorController.signAccessToken` (`auth/twofactor/TwoFactorController.java:77-89`) — token builder thứ 3 trong post-2FA login completion path

## Problem

ADR-039 thêm `tier` claim vào 2 access-token builder (`AuthService.generateAccessToken` + `TokenService.generateAccessToken`) để gateway inject trusted `X-Subscription-Tier` header (server-side resolution, không client-trust per GAP-1020). NHƯNG builder thứ 3 `TwoFactorController.signAccessToken` (`TwoFactorController.java:77-89`, dùng cho user hoàn tất login qua 2FA) KHÔNG có `tier` claim — và đã thiếu sẵn `tenantId` claim từ trước (cùng class divergence với GAP-704 đã fix cho 2 builder kia).

Hiện token builder chỉ set 3 claim: `email`, `role`, `type=access` (`TwoFactorController.java:82-84`) — không có `tier`, không có `tenantId`.

Hệ quả: user đăng nhập qua đường 2FA → access token KHÔNG có `tier` claim → gateway inject `X-Subscription-Tier` mặc định FREE dù tier thật của instance có thể cao hơn (BASIC/PREMIUM/ENTERPRISE) → branding/tier-gated features thấy FREE sai. Tương tự, thiếu `tenantId` claim có thể chặn các endpoint yêu cầu cross-check `X-Tenant-Id` (cùng triệu chứng GAP-704).

Hiện admin-2FA path chưa block production (admin → tier=FREE anyway; OWNER test-8 không bật 2FA), nhưng cần parity để tránh latent bug khi OWNER bật 2FA. Cùng bug-class GAP-1020 (tier trust) + GAP-704 (tenantId claim divergence).

## Root Cause

3 access-token builder tồn tại độc lập trong `kitehub-subscription` (`AuthService` + `TokenService` + `TwoFactorController`). ADR-039 SUB-22 chỉ patch 2 builder đầu cho `tier` claim; builder thứ 3 (2FA completion) bị bỏ sót. Không có shared helper → mỗi lần thêm claim mới phải sync thủ công 3 nơi → drift.

## Proposed Fix

1. Inject `InstanceRepository` vào `TwoFactorController` + resolve `tier` từ `instances.tier` (theo cùng logic `AuthService.resolveTierForRole`) → thêm `.claim("tier", ...)`.
2. Thêm `.claim("tenantId", ...)` (resolve qua `users.tenant_id` HOẶC `instances.owner_id` binding như GAP-704 fix cho 2 builder kia).
3. Ưu tiên hơn: refactor 3 builder dùng chung 1 helper (vd `AccessTokenBuilder`) → eliminate drift class vĩnh viễn.
4. Sweep callers + run tests per `api-contract-change-caller-sweep.md` nếu đổi signature.

## Acceptance Criteria

- [ ] `TwoFactorController.signAccessToken` token có `tier` claim khớp `instances.tier`
- [ ] Token có `tenantId` claim (parity với AuthService/TokenService post-GAP-704)
- [ ] 3 access-token builder parity claim set (hoặc refactor về shared helper)
- [ ] Test login 2FA → token tier + tenantId đúng

## Related

- Discovered in: tier-enforcement wave 2026-06-09 (cross-flow sweep DEFER finding)
- Same bug-class tier trust: GAP-1020 (X-Subscription-Tier client-controlled + RLS GUC)
- Same divergence class tenantId claim: GAP-704 (JWT thiếu tenantId post-beta-signup — DONE cho 2 builder, builder 2FA bỏ sót)
- ADR-039 SUB-22 (cross-service subscription-tier propagation) — `tier` claim mandate cho access token
