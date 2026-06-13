# ADR-041: Instance Tier-Sync Centralization (SUB-21)

**Status:** ACCEPTED
**Date:** 2026-06-13
**Deciders:** @nguyenvankiet (solo-dev — acting architect)
**Reviewers:** @nguyenvankiet (solo-dev — data-consistency angle)
**Related Gap(s):** GAP-1090 (`instances.tier` không mirror), GAP-1256 (tier desync ở rollback / suspend / cancel / expiry), GAP-1095/1096 (tier carry qua migration), GAP-1264 (retention-clock determinism)

## Context

`subscriptions.tier` là **source-of-truth** cho tier hiện hành của tenant, nhưng nhiều consumer đọc `instances.tier` (cột denormalized) trực tiếp thay vì join sang subscription mỗi request:

- `MultiTenantDataSourceConfig` — connection-pool size theo tier
- `DomainService.canUseCustomDomain()` — custom-domain eligibility (PREMIUM/ENTERPRISE)
- `DataRetentionService` — retention window theo tier

GAP-1090 phát hiện: `SubscriptionService.applyPendingUpgrade` + `SubscriptionRenewalService.processRenewal` flip `subscriptions.tier` nhưng **không bao giờ** gọi `instance.setTier(...)` → `instances.tier` kẹt ở FREE (hoặc tier trước thay đổi). Tenant trả tiền PREMIUM nhưng pool/domain/retention vẫn enforce như FREE.

GAP-1256 phát hiện cùng class drift ở các path khác: rollback migration (TRIAL→ACTIVE→TRIAL), suspend (involuntary-churn / cancel / trial-expiry). Mỗi path tự gọi `instance.setTier(...)` rải rác → dễ quên một nhánh → silent desync tái diễn.

Song song, một cột load-bearing thứ hai có cùng vấn đề "set rải rác dễ quên": `instances.suspended_at` — mốc deterministic cho retention clock (GAP-1264, SUB-25). Trước đây không có cột này; `DataRetentionService` tính window từ `updated_at`, nên một row-update không liên quan (tier sync, sửa contact-email) vô tình **reset** retention clock — vi phạm tính xác định PDPL.

**Ràng buộc:**
- Solo-dev Phase 1 BETA — ưu tiên giải pháp đọc-trực-tiếp (không thêm join runtime mỗi request).
- Denormalization phải giữ invariant rẻ + đúng tại MỌI path thay đổi tier/status.

## Decision

**Tập trung hai mutation rải-rác về hai điểm canonical duy nhất:**

### 1. `InstanceTierSyncService.syncInstanceTier(instance, effectiveTier)` — điểm sync tier duy nhất (SUB-21)

Một service stateless (`@Service`) với một method: set `instances.tier` = effective tier của subscription ACTIVE, no-op nếu đã đồng bộ. MỌI path thay đổi effective tier PHẢI route qua đây thay vì gọi `instance.setTier(...)` trực tiếp:

- BE-1: `SubscriptionService.applyPendingUpgrade` (create-flow activation + upgrade apply) + `SubscriptionRenewalService.processRenewal` (end-of-cycle downgrade apply)
- BE-2: `TrialToPaidService` rollback (GAP-1256 rollback desync) + migration flip mang tier requested (GAP-1095, qua `trialService.convertTrialToSubscription(instanceId, instance.getTier())`)
- BE-3: suspend / cancel / expiry paths (GAP-1256 lapse desync)

Caller chịu trách nhiệm persist `Instance` — service chỉ mutate entity trong txn của caller.

### 2. `Instance.setStatus(InstanceStatus)` — điểm stamp `suspended_at` duy nhất (SUB-25)

Override `setStatus` (thay cho Lombok `@Setter` thuần) để **mỗi** transition trạng thái tự quản lý `suspended_at`:

- → `SUSPENDED` (khi đang KHÔNG suspended): stamp `suspended_at = now()`
- → `ACTIVE`: clear `suspended_at = null`
- → `DELETED` / `PURGED`: **giữ nguyên** `suspended_at` cho audit

Mọi suspend path (trial-expiry / involuntary-churn / cancel) gọi `instance.suspend()` hoặc `instance.setStatus(SUSPENDED)` → tự động có mốc retention đúng. `DataRetentionService.retentionClockStart()` đọc `suspended_at` (fallback `updated_at` chỉ cho legacy row pre-V73).

Backfill: `V68__sync_instance_tier_to_active_subscription.sql` (idempotent UPDATE các row đã drift); `V73` thêm cột `suspended_at`.

## Consequences

### Positive
- **Single point of change** — thêm tier-changing path mới chỉ cần gọi `syncInstanceTier`; quên = lỗi rõ ràng tại review, không silent drift.
- **Invariant đúng tại mọi path** — đóng GAP-1090 (upgrade) + GAP-1256 (rollback/suspend/cancel/expiry) + GAP-1095 (migration tier carry) cùng một cơ chế.
- **Retention determinism (PDPL)** — `suspended_at` immune với row-update không liên quan; retention window tính từ thời điểm suspend thực, không phải `updated_at` trôi.
- **Zero extra runtime hop** — consumer vẫn đọc `instances.tier` / `instances.suspended_at` trực tiếp; không thêm join mỗi request.

### Negative
- **Denormalization burden** — `instances.tier` vẫn là bản sao; nếu một path tương lai bypass `InstanceTierSyncService` thì drift tái diễn. Mitigate: reviewer-checklist + `instances-table-triad-discipline.md`.
- **`setStatus` không còn là pure setter** — có side-effect (stamp/clear `suspended_at`); developer phải biết `setStatus` ≠ Lombok-generated. Mitigate: javadoc rõ trên method.

### Neutral
- `InstanceTierSyncService` là helper mỏng (1 method) — không phải Facade/Strategy; chấp nhận theo YAGNI vì chỉ cần một điểm tập trung.
- Cần V68 backfill chạy một lần để sửa các row đã drift trước khi rule có hiệu lực.

## Alternatives Considered

### Alternative A: Bỏ `instances.tier`, luôn join `subscriptions` runtime
- Pros: không có denormalization → không thể drift.
- Cons: mỗi pool-sizing / domain-eligibility / retention check thêm 1 join + subscription thành hot dependency trên data-path. Trái nguyên tắc Phase 1 BETA (ít hop).
- **Rejected:** chi phí runtime + coupling vượt lợi ích; tier đổi hiếm (upgrade/downgrade) nên denormalize + sync-on-change rẻ hơn join-mỗi-request.

### Alternative B: Giữ `instance.setTier(...)` rải rác, chỉ thêm test coverage
- Pros: ít refactor.
- Cons: không loại được class "quên một nhánh" — GAP-1256 chứng minh path rải rác sinh drift lặp lại; test chỉ bắt path đã biết.
- **Rejected:** không phải force-multiplier; recurrence chứng minh cần single point.

### Alternative C: Stamp `suspended_at` tại từng suspend call-site (không override `setStatus`)
- Pros: `setStatus` giữ pure setter.
- Cons: cùng class "quên một call-site" như tier — GAP-1256 nguyên nhân gốc. `setStatus` là choke-point tự nhiên cho mọi status transition.
- **Rejected:** override `setStatus` đảm bảo MỌI transition (kể cả tương lai) đều stamp đúng.

## Implementation Notes

- **Code:** `InstanceTierSyncService.syncInstanceTier`; `Instance.setStatus(InstanceStatus)` override (kitehub-platform entity); call-sites BE-1/2/3.
- **Migration:** `V68__sync_instance_tier_to_active_subscription.sql` (backfill drift), `V73` (cột `suspended_at`).
- **Rollback:** drift là under-grant-safe (consumer fallback FREE); revert = đọc subscription join.
- **Monitoring:** counter khi consumer gặp `instances.tier` ≠ subscription ACTIVE tier (phát hiện bypass).
- **Enforcement:** `instances-table-triad-discipline.md` (entity ↔ migration ↔ caller atomic ship) bao trùm mọi thay đổi schema/field của bảng `instances`.

## References

- Rule canonical: [`subscription-billing/rules.md`](../../01-business/kitehub/subscription-billing/rules.md) SUB-21 + SUB-25
- Tier propagation cross-service: [ADR-039](ADR-039-cross-service-subscription-tier-propagation.md) (JWT tier claim — đọc `instances.tier` đã sync làm nguồn issue token)
- Migration atomicity: [ADR-042](ADR-042-trial-to-paid-migration-atomicity.md) (tier carry GAP-1095 qua sync point)
- Dunning + retention clock: [ADR-043](ADR-043-manual-vietqr-dunning-involuntary-churn-lifecycle.md) (suspended_at là mốc SUB-25)
- Design pattern: `.claude/rules/design-patterns.md` §3.12 + `.claude/rules/instances-table-triad-discipline.md`
- Related gaps: GAP-1090, GAP-1256, GAP-1095/1096, GAP-1264

## Log

- 2026-06-13 — Initial proposal + ACCEPTED same day (solo-dev). Codifies tier-sync + suspended_at centralization shipped trong wave kitehub-biz-100 (BE-1/2/3). Reviewer: @nguyenvankiet (solo-dev acting architect).
