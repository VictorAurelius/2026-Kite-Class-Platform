# GAP-1435: BASIC owner không có đường downgrade hợp lệ — tier thấp hơn duy nhất là FREE bị BE từ chối 400

**Status:** 🟢 DONE
**Priority:** 🟡 P2
**Domain:** Mixed
**Found:** 2026-06-16 (Phase-2 browser walk flow KH-5)
**Affects:** KH-5 subscription lifecycle — `kitehub-frontend (customer)/billing/upgrade` + `TierSelector` + `SubscriptionService.downgradeSubscription`

## Problem
Discovered Phase-2 browser walk KH-5. Với owner đang ở tier BASIC, tier thấp hơn duy nhất là FREE — nhưng BE cố ý từ chối downgrade→FREE (`PATCH /downgrade` trả 400 "Cancel to drop to FREE"). FE `TierSelector` vẫn cho chọn FREE làm target downgrade, khiến lỗi 400 hiện ra dưới dạng toast generic, owner BASIC không có đường downgrade-hoặc-cancel rõ ràng.

## Proposed Fix
FE `TierSelector` ẩn FREE khỏi danh sách target downgrade cho owner trả phí, HOẶC hiển thị guidance "hủy đăng ký để về FREE" thay vì để PATCH /downgrade trả 400 generic. Reconcile recipe + FE để owner BASIC có path downgrade-or-cancel xác định.

## Acceptance Criteria
- [x] Owner BASIC không thấy FREE là một downgrade target trong `TierSelector` (hoặc thấy guidance "hủy đăng ký" rõ ràng)
- [x] Không còn 400 generic toast khi owner thử về FREE
- [ ] Recipe KH-5 phản ánh path downgrade-or-cancel đúng

## Fix implemented (2026-06-16, pending re-walk)
FE-only fix — design-first confirmed BE downgrade→FREE rejection là intended (SUB-01/GAP-1018: cancel ends a subscription, downgrade only moves between paid tiers).
- `kitehub/kitehub-frontend/src/components/billing/TierSelector.tsx`: FREE không còn là selectable downgrade target cho paid owner (`isDowngradeToFreeForPaidOwner` → card disabled); thay vào đó hiển thị guidance "Để chuyển về gói Miễn phí, vui lòng hủy đăng ký hiện tại" + nút "Hủy đăng ký" route `/settings` (Danger Zone). Clicking FREE không gọi `downgrade.mutateAsync` → không còn 400 generic toast.
- Test: `src/components/billing/__tests__/TierSelector.test.tsx` (4 cases PASS) — paid owner FREE guidance + no onSelect('FREE') + cancel→/settings + FREE owner paid-tier selectable.
- `pnpm --filter kitehub-frontend build` PASS.
- AC #3 (recipe KH-5) defer — doc/recipe scope ngoài code fix; chờ re-walk.

## Related
- Discovered in: Phase-2 browser walk (flow KH-5), 2026-06-16
