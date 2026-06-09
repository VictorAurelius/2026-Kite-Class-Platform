# ADR-039: Cross-Service Subscription Tier Propagation

**Status:** ACCEPTED
**Date:** 2026-06-09
**Deciders:** @nguyenvankiet (solo-dev — acting architect)
**Reviewers:** @nguyenvankiet (solo-dev — security angle)
**Related Gap(s):** GAP-1020 (X-Subscription-Tier client-controlled spoof + default FREE), GAP-1089 (tier entitlement unenforced cross-service), GAP-1090 (instances.tier mirror — SUB-21)

## Context

Một số service cần biết subscription tier của tenant để enforce entitlement (branding regenerate quota, AI input cap, custom-domain eligibility, rate-limit multiplier). Hiện tại `kitehub-branding` (và tương lai `kiteclass-core`) đọc tier qua header `X-Subscription-Tier` với `defaultValue = "FREE"` — header này **client-controlled**:

- **Spoof risk:** client tự gắn `X-Subscription-Tier: ENTERPRISE` để bypass quota/entitlement (GAP-1020). Không có trust boundary nào strip header này từ request ngoài.
- **Default-FREE silent downgrade:** khi header vắng, service mặc định FREE → tenant trả tiền PREMIUM nhưng bị enforce như FREE (entitlement không propagate đúng — GAP-1089).

Per [`multi-tenant-architecture.md`](../multi-tenant-architecture.md) §3, **gateway là single trust boundary** cho JWT validation: gateway verify JWT signature một lần rồi inject trusted header (`X-Tenant-Id`, `X-User-Id`, `X-User-Roles`) cho downstream; service KHÔNG được tự parse JWT body (tránh duplicate validation + key distribution). Tier propagation phải theo cùng mô hình — tier là attribute của verified principal, không phải input client tự khai.

Ràng buộc:
- `subscriptions.tier` = source-of-truth; `instances.tier` = denormalized mirror đã sync qua SUB-21 + Flyway `V68__sync_instance_tier_to_active_subscription.sql` (GAP-1090).
- Phase 1 BETA solo-dev: ưu tiên giải pháp ít hop nhất, không thêm hot dependency runtime giữa gateway và subscription.

## Decision

**Option A — JWT `tier` claim.** Tier được embed vào access token và gateway inject thành trusted header, mirror đúng pattern `X-User-Roles`:

1. **Issue-time (kitehub-subscription `TokenService`):** khi phát hành access token, đọc `instances.tier` (đã mirror `subscriptions.tier` ACTIVE per SUB-21) và embed claim `tier` vào JWT payload.
2. **Gateway (`JwtAuthenticationGatewayFilter`):** sau khi verify JWT signature, đọc claim `tier` và inject trusted header `X-Subscription-Tier` cho downstream — y hệt cách inject `X-User-Roles` từ claim `roles`.
3. **Anti-spoof (`default-filters`):** gateway `default-filters` PHẢI strip mọi `X-Subscription-Tier` client gửi vào TRƯỚC khi inject giá trị trusted (request-header `RemoveRequestHeader=X-Subscription-Tier` rồi mới `AddRequestHeader` từ claim). Client không thể forge.
4. **Downstream trust:** `kitehub-branding` (+ tương lai `kiteclass-core`) trust header `X-Subscription-Tier` do gateway inject; bỏ `defaultValue = "FREE"` client-controlled. Unknown/missing tier → fail-safe FREE (chỉ xảy ra khi route không qua gateway — dev local direct call).

Entitlement matrix (giá trị cap/quota theo từng tier) là canonical tại [`subscription-billing/rules.md`](../../01-business/kitehub/subscription-billing/rules.md) **SUB-22** — ADR này quy định cơ chế *propagation*, không quy định *giá trị* entitlement.

## Consequences

### Positive
- **Không forge được:** tier nằm trong JWT server-signed; client sửa header → bị strip; client sửa JWT → fail signature verify. Đóng GAP-1020 spoof class.
- **Zero extra hop runtime:** tier đi kèm token đã verify; gateway không gọi thêm subscription mỗi request (khác Option B).
- **Đúng trust-boundary model:** mirror `X-User-Roles` — service chỉ trust gateway-injected header, không tự parse JWT (consistent với `multi-tenant-architecture.md` §3).
- **Entitlement propagate đúng:** PREMIUM tenant được enforce PREMIUM quota, không silent-downgrade FREE (đóng GAP-1089 propagation gap).

### Negative
- **Staleness ≤ access-token TTL (~15 phút):** khi tier đổi (upgrade confirm / downgrade apply), token cũ còn mang tier cũ tới khi hết TTL. Mitigate: **force token-refresh ngay sau `applyPendingUpgrade`** (revoke/rotate access token) để tier mới có hiệu lực tức thì. Downgrade end-of-cycle ít nhạy cảm về thời điểm nên TTL drift chấp nhận được.
- **Token payload tăng nhẹ:** thêm 1 claim `tier` (~10 bytes) — không đáng kể.

### Neutral
- TokenService phải đọc `instances.tier` lúc issue → phụ thuộc SUB-21 mirror đã đúng (GAP-1090 đã đóng invariant đó).
- Gateway `default-filters` gain thêm 1 cặp Remove/Add header — cùng pattern đang dùng cho các trusted header khác.

## Alternatives Considered

### Alternative B: Gateway → subscription lookup per-request
Gateway gọi `kitehub-subscription` mỗi request để resolve tier theo tenant.
- Pros: zero staleness (luôn đọc tier hiện tại).
- Cons: +1 network hop mỗi request; biến subscription thành **hot dependency** trên data-path của mọi service — subscription down = toàn bộ gateway routing degrade. Trái nguyên tắc Phase 1 BETA (ít hop, không hot dependency runtime).
- **Rejected:** chi phí latency + coupling vượt lợi ích zero-staleness; staleness ≤15min đã đủ tốt với force-refresh-on-upgrade.

### Alternative C: Gateway đọc `instances.tier` trực tiếp
Gateway tự query `instances.tier` (DB read) khi resolve route.
- Pros: zero staleness cho route resolve theo subdomain (gateway đã đọc `instances` để resolve tenant từ subdomain).
- Cons: gateway thêm DB coupling cho mọi JWT-authenticated route (đa số route không cần đọc instances).
- **Kept as fallback (zero-staleness) cho subdomain-resolved routes:** với route mà gateway ĐÃ đọc `instances` để resolve tenant theo subdomain, có thể đọc luôn `instances.tier` (free, không thêm query) → dùng làm trusted tier cho riêng các route đó. Option A (JWT claim) là path chính cho JWT-authenticated API; Option C là tối ưu cục bộ khi gateway đã chạm `instances` rồi.

## Implementation Notes

- **Migration strategy:** ship theo thứ tự — (1) TokenService embed claim `tier` (backward-compatible: token cũ không có claim → gateway fallback đọc Option C hoặc FREE); (2) gateway inject `X-Subscription-Tier` + strip client header; (3) downstream bỏ `defaultValue=FREE` client-controlled. Tier-enforcement wave này code 3 lớp song song (agent khác).
- **Force-refresh wiring:** `applyPendingUpgrade` (create-flow activation + upgrade apply) gọi token revoke/rotate để tier mới có hiệu lực < 15 phút.
- **Monitoring:** emit counter khi downstream nhận tier mismatch (header tier ≠ DB tier) để phát hiện staleness drift bất thường.
- **Rollback:** nếu propagation lỗi, downstream fail-safe FREE (an toàn — under-grant, không over-grant); revert gateway filter về pass-through không inject.

## References

- Trust boundary: [`multi-tenant-architecture.md`](../multi-tenant-architecture.md) §3 (Tenant ID propagation chain — single trust boundary)
- Entitlement matrix (canonical giá trị): [`subscription-billing/rules.md`](../../01-business/kitehub/subscription-billing/rules.md) §Entitlement matrix SUB-22
- Tier source-of-truth + mirror: `subscription-billing/rules.md` SUB-21 + `V68__sync_instance_tier_to_active_subscription.sql`
- Tier caps source: `PricingTier.java` (FREE/BASIC/PREMIUM/ENTERPRISE)
- Related rules: `.claude/rules/design-patterns.md` §3.4 (Adapter — header injection mirror pattern)
- Related gaps: GAP-1020 (spoof), GAP-1089 (cross-service tier unenforced), GAP-1090 (instances.tier mirror)
- Related ADRs: ADR-023 (Gateway Rate-Limit Key Resolver), ADR-035 (Pricing Model Taxonomy)

## Log

- 2026-06-09 — Initial proposal + ACCEPTED same day (solo-dev — decision đã chốt từ design investigation tier-enforcement wave; closes propagation design portion of GAP-1089 + addresses GAP-1020 spoof). Reviewer: @nguyenvankiet (solo-dev acting architect + security scout).
