# GAP-1096: `SubscriptionService.activateSubscription` set status không sync tier — dead-code 0 caller (cùng class GAP-1090)

**Status:** 🟢 DONE 2026-06-14
**Priority:** 🟢 P3
**Domain:** Backend
**Found:** 2026-06-09 (tier-UI fix session — browser verify + BE sweep)
**Affects:** `kitehub-subscription` `SubscriptionService.activateSubscription(UUID subscriptionId)` (`service/SubscriptionService.java:332`)

## Problem

Trong BE sweep cùng class GAP-1090, `SubscriptionService.activateSubscription` (line 332) flip `subscription.setStatus(ACTIVE)` + `instance.setStatus(InstanceStatus.ACTIVE)` (line 355) + `setSubscriptionExpiresAt` NHƯNG **không gọi `instance.setTier(...)`** → cùng desync class "set status không sync tier" như GAP-1090.

Khác biệt quan trọng: method này hiện **0 production caller** — `grep -rn "activateSubscription" kitehub/kitehub-subscription/src/main/java` (2026-06-09) chỉ trả về dòng định nghĩa (line 332), không có call-site. Path activation hiện tại được phục vụ bởi `applyPendingUpgrade` create-flow (SUB-20). `activateSubscription` là dead-code残 (legacy activation entry chưa xoá).

→ Đây là latent bug: nếu method được tái dùng trong tương lai mà không sync tier, sẽ tái tạo split-brain GAP-1090. P3 vì hiện không reachable.

## Root Cause

Legacy activation method giữ lại sau khi SUB-20 `applyPendingUpgrade` create-flow thay thế đường activation chính; không được dọn → dead-code mang sẵn mầm desync (set status không set tier) + chưa tuân invariant rules.md SUB-21.

## Proposed Fix

Một trong hai hướng:
1. **Remove dead method** `activateSubscription` (xác nhận 0 caller toàn repo gồm test/controller trước khi xoá), HOẶC
2. Nếu giữ để tái dùng → thêm `instance.setTier(targetTier)` cùng `setStatus(ACTIVE)` để tuân invariant SUB-21 (cùng pattern fix GAP-1090).

## Acceptance Criteria

- [x] Xác nhận 0 caller toàn repo (main + test + controller) cho `activateSubscription`
- [x] Method được remove HOẶC thêm `instance.setTier` sync (tuân rules.md SUB-21)
- [x] Không còn dead-code set-status-không-sync-tier trong `SubscriptionService`

## Related

- Discovered in: tier-UI fix session 2026-06-09 (BE sweep cùng class GAP-1090)
- Root cause / pattern: GAP-1090 (`applyPendingUpgrade` setTier 3 path + rules.md SUB-21 invariant) — DONE
- Cùng class non-fixed path: GAP-1095 (`convertTrialToSubscription` trial→paid convert set status không sync tier — cần signature change)

## Closure — wave-kitehub-biz-100 (2026-06-14)

🟢 DONE — engineering-complete + G3 production-parity walk verified. BE-1 SUB-21 sweep — activateSubscription tier-sync resolved (dead-code path).

- G3 walk: `documents/04-quality/audits/persona-review/2026-06-13-g3-walk-kitehub-biz-100.md` (8 PASS / 1 PASS-with-P1 closed via GAP-1273 / 1 win-back async by-design).
- Tests: 963 backend (kitehub-subscription) + 906 frontend green.
- Consolidated into PR branch `wave/kitehub-biz-100`.
