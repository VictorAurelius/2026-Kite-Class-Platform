# GAP-1090: `instances.tier` không sync với `subscriptions.tier` khi activation/upgrade — `applyPendingUpgrade` thiếu `instance.setTier()`

**Status:** 🟢 DONE
**Priority:** 🟠 P1
**Domain:** Backend
**Found:** 2026-06-09 (state-check tier-entitlement UI session)
**Affects:** `kitehub-subscription` `SubscriptionService.applyPendingUpgrade` + denormalized `instances.tier` consumers (HikariCP pool / domain eligibility / retention / `InstanceResponse`) + `kitehub-frontend` dashboard & trial-countdown bar — root cause của bug "đã nâng PREMIUM nhưng UI vẫn hiển thị trial"

## Problem

Root cause chính của cluster bug user báo "nâng gói rồi mà thanh thời gian vẫn trial".

1. **`applyPendingUpgrade` không gọi `instance.setTier()`** — `SubscriptionService.applyPendingUpgrade` (`kitehub/kitehub-subscription/src/main/java/com/kitehub/subscription/service/SubscriptionService.java:481-563`) flip `subscription.setTier(targetTier)` + `subscription.setStatus(ACTIVE)` (line 502/510) và set `instance.setStatus(ACTIVE)` + `setSubscriptionId` + `setSubscriptionExpiresAt` (create-flow line 517-525) NHƯNG **không bao giờ gọi `instance.setTier(targetTier)`**. Upgrade-else-branch (line 542-562) chỉ load instance để gửi email, không đụng tier.

2. **State drift reachable bình thường** — hệ quả: `instances.tier` kẹt `FREE` dù `subscriptions.tier=PREMIUM` + `status=ACTIVE`. Verified trong DB 2026-06-09 (tenant `test-8` + `sky-test`): `subscription.tier=PREMIUM` nhưng `instance.tier=FREE`.

3. **`instance.tier` là load-bearing denormalized "current effective tier"** — drive nhiều nơi:
   - `MultiTenantDataSourceConfig.java:75` — HikariCP pool size per tier
   - `DomainService.java:71` — custom-domain eligibility
   - `DataRetentionService.java:46/68/125` — retention window
   - `InstanceResponse.java:33` — expose tier ra FE instance-detail

4. **FE split-brain (triệu chứng user thấy)** — các surface đọc `instance.tier`/`isOnTrial` hiển thị **trial dù đã PREMIUM**:
   - Dashboard (`kitehub-frontend/src/app/(customer)/dashboard/page.tsx:71-73,250`) đọc `instance.tier`/`isOnTrial`
   - Trial-countdown bar (`(customer)/instances/[id]/page.tsx:25-27`) gate bằng `instance.isOnTrial`
   - Billing page (`(customer)/billing/page.tsx:422`) đọc `subscription.tier` → **đúng PREMIUM** (chỉ surface này đúng → confirm split-brain giữa instance.tier vs subscription.tier)

5. **Contributing (FE cache stale)** — subscription mutation (`use-subscriptions.ts:72/99/136/156`) + payment-confirm (`use-payments.ts:85`) chỉ invalidate `['subscriptions']`/`['payments']`, KHÔNG invalidate `['instances']` (`refetchOnWindowFocus:false`) → dashboard giữ stale tier tới khi reload/60s GC.

## Root Cause

`applyPendingUpgrade` được thiết kế cập nhật subscription state nhưng bỏ sót việc đồng bộ giá trị denormalized `instances.tier`. `subscription.tier` là source-of-truth, `instance.tier` là cache phải mirror nó — nhưng invariant này chưa được code enforce ở cả 3 path (create-flow / upgrade-else / downgrade-apply) và chưa được document trong `rules.md`. Doc drift bổ sung: GAP-974 narrative (line 22) claim SAI rằng `applyPendingUpgrade` "flips Instance tier/status" — thực tế chỉ flip status, không flip tier.

## Proposed Fix

1. Thêm `instance.setTier(targetTier)` vào `applyPendingUpgrade` ở cả 3 path: create-flow block + upgrade-else-branch + downgrade-apply path.
2. Flyway backfill migration resync rows lệch: `UPDATE instances SET tier = (active subscription.tier) WHERE instance.tier != active sub.tier`.
3. FE: invalidate `['instances']` trong subscription mutation hooks (`use-subscriptions.ts` + `use-payments.ts`) sau mutation.
4. Document invariant trong `documents/01-business/kitehub/subscription-billing/rules.md`: rule mới `instances.tier` mirror active `subscriptions.tier`; `subscription.tier` = source-of-truth. Sửa GAP-974 narrative drift (line 22).

## Acceptance Criteria

- [x] `applyPendingUpgrade` set `instance.setTier(targetTier)` ở cả 3 path (create-flow / upgrade-else / downgrade-apply)
- [x] Backfill migration verify `test-8` instance.tier → PREMIUM (rows lệch resynced)
- [x] FE invalidate `['instances']` sau subscription/payment mutation
- [x] Dashboard + trial-countdown bar hiển thị đúng PREMIUM (no trial) sau upgrade
- [x] Invariant documented trong `rules.md` (instance.tier mirror subscription.tier)

## Walk evidence (per feature-ship-runtime-walk-mandate.md §3 + pre-handoff-self-test-completeness §3)

**Source:** state-check tier-entitlement UI fix session 2026-06-09. Verify trên local Docker stack (`:3001` kitehub-frontend), tenant `test-8` đã upgrade PREMIUM (owner `g2test-an-8@example.com`). Headless browser Playwright + DB + API + build — 7/7 assertion PASS.

**DB:**
- V68 backfill migration applied `success=true` → `test-8` `instances.tier` FREE → PREMIUM (resync rows lệch khớp active subscription).

**API:**
- `GET /api/platform/instances/owner/{ownerId}` → `tier: PREMIUM, isOnTrial: false, trialDaysLeft: 0` (`instance.tier` source đã đúng).
- `GET /subscriptions/instance/{id}/active` → `tier: PREMIUM, status: ACTIVE` (subscription source-of-truth khớp).

**Browser (Playwright headless `:3001`, login `g2test-an-8@example.com`):**
- Dashboard: hiển thị **PREMIUM** + **KHÔNG còn dòng "gói thử nghiệm"** — residual fix `buildHealthSnapshot` gate bằng `isOnTrial` (trial-countdown bar tắt khi `isOnTrial=false`).

**Build / test:**
- `pnpm --filter kitehub-frontend build` PASS 89/89.
- BE `SubscriptionServiceTest` 13/0/0 + `SubscriptionRenewalServiceTest` 10/0/0 (3 path `instance.setTier` + renewal path covered).

**Code shipped (working tree session 2026-06-09):**
- BE `SubscriptionService.java` (3 path `instance.setTier`) + `SubscriptionRenewalService.java` (renewal path tier sync) + Flyway `V68__sync_instance_tier_to_active_subscription.sql` (backfill) + `rules.md` SUB-21 invariant.
- FE `use-subscriptions.ts` + `use-payments.ts` (invalidate `['instances']` sau mutation) + `dashboard/page.tsx` (`buildHealthSnapshot` `isOnTrial` gate).

## Related

- Discovered in: state-check tier-entitlement UI session 2026-06-09 (2 Opus agent state-check)
- Same method `applyPendingUpgrade`: GAP-974 — narrative line 22 claim SAI "flips Instance tier/status" (doc-vs-code drift, sửa cùng PR fix này)
- Cluster sibling: GAP-1091 (AI branding hiển thị trial/locked dù PREMIUM — cùng cluster "PREMIUM shows trial UI")
- Tier theme: GAP-1089 (cross-service tier entitlement không enforce cho core product), GAP-1020 (branding tier RLS, server-side không client-trust)

## Log

- **2026-06-09** — 🟢 DONE. Fix shipped tier-UI session: `SubscriptionService.applyPendingUpgrade` set `instance.setTier(targetTier)` ở 3 path (create-flow / upgrade-else / downgrade-apply) + `SubscriptionRenewalService` renewal path sync + Flyway `V68` backfill (test-8 FREE→PREMIUM) + FE `use-subscriptions.ts`/`use-payments.ts` invalidate `['instances']` + `dashboard/page.tsx` `buildHealthSnapshot` gate `isOnTrial` + `rules.md` SUB-21 invariant + GAP-974 narrative line 22 drift đã sửa. Verify đầy đủ per `## Walk evidence` (Playwright headless `:3001` tenant test-8 PREMIUM 7/7 PASS + DB V68 + API tier=PREMIUM + build 89/89 + BE 13/0/0 + 10/0/0). Status flip per `gap-done-discipline.md` §2 — all 5 AC verified; `git mv` → `phase-1-beta/closed/` + CSV row sync per `gap-folder-organization.md` v2.0.0 + `post-merge-sync-completeness.md`.
