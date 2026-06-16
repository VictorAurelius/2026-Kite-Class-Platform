# GAP-1464: instances.subscription_id NULL khi sub seeded/created ngoài flow → BE trả subscriptionId null → FE DangerZone tưởng không có sub

**Status:** 🟡 PARTIAL
**Priority:** 🟠 P2
**Domain:** Backend
**Found:** 2026-06-16 (KH-5 human G2 walk)
**Affects:** kitehub-subscription InstanceService.toResponse + seed paths

## Problem

KH-5 walk: DangerZone báo "Bạn chưa có gói đăng ký để hủy" + disable nút dù owner có PREMIUM ACTIVE sub. Root: InstanceService.toResponse:649 đọc instance.getSubscriptionId() (cột denormalized instances.subscription_id) = NULL. SubscriptionService:543 CÓ set cột này khi tạo sub qua flow thường, nhưng sub seeded trực tiếp (vd sky-education) bỏ qua → cột NULL → BE trả subscriptionId:null → FE Boolean(instance.subscriptionId)=false → disable cancel.

Backfill data dev DB đã fix tạm (UPDATE instances SET subscription_id từ active sub). PARTIAL: root robustness chưa fix.

## Acceptance Criteria

- [x] Data backfill dev DB (instances có active sub + NULL → set)
- [ ] Robustness: InstanceService.toResponse derive subscriptionId từ subscriptionRepository.findActiveByInstanceId nếu cột NULL (defensive) HOẶC seed scripts set back-link
- [ ] Re-walk DangerZone enable confirm

## Related
- Discovered in: 2026-06-16 KH-5 G2 walk · class GAP-823 (triad/denorm sync)
