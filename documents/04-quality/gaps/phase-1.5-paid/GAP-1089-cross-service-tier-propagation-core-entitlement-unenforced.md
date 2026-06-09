# GAP-1089: Tier entitlement không enforce cho core product — kiteclass-core không nhận subscription tier (PricingTier caps dead)

**Status:** 🔵 OPEN
**Priority:** 🟠 P1
**Domain:** Mixed (kitehub-subscription + gateway + kiteclass-core)
**Found:** 2026-06-09 (KH-3 G2 entitlement investigation — user hỏi "nâng gói vs trial khác biệt đã đầy đủ chưa")
**Affects:** `kitehub-platform` `PricingTier` enum (maxStudents/maxTeachers/storageLimitMB) + `kiteclass-core` (course/class/student/attendance/grade — toàn bộ nghiệp vụ trường) + gateway header propagation

## Problem

KH-3 (subscription upgrade) verify **cơ chế** nâng gói (state flip FREE/TRIAL → PREMIUM/ACTIVE + payment gate + email) — đúng scope. Nhưng **quyền lợi gói (entitlement)** giữa TRIAL và PAID **chưa thể hiện ở core product**:

- `PricingTier` enum (`kitehub-platform/.../enums/PricingTier.java`) định nghĩa `maxStudents`, `maxTeachers`, `storageLimitMB`, `priceVND` per tier (FREE/BASIC/PREMIUM/ENTERPRISE).
- **Nhưng `kiteclass-core` (app nghiệp vụ trường) KHÔNG nhận subscription tier** — grep `kiteclass-core/src/main/java` cho `subscriptionTier|getTier|instance.*tier|X-*-Tier|fetchSubscription` → **0 reference**. Subscription tier sống ở kitehub-subscription DB (`instances` table); kiteclass-core là service riêng + DB riêng, không có cơ chế propagate tier qua (gateway header / event / API).
- Hệ quả: `maxStudents` trong kiteclass-core là field **per-class** (mỗi `Class.maxStudents=30`, set lúc tạo lớp), KHÔNG phải cap tenant-wide theo gói. `PricingTier.maxStudents/maxTeachers` = **dead/cosmetic** cho core product.

→ **TRIAL và PREMIUM tạo lớp / enroll học sinh / điểm danh / chấm điểm y như nhau.** Khác biệt hiện tại chỉ là **thời gian** (trial 14 ngày → hết hạn → suspend) + AI quota (KH-6), KHÔNG phải tính năng/dung lượng core. Người trả tiền không nhận thêm gì ở sản phẩm chính.

## Root Cause

Multi-service architecture: subscription tier (SaaS lifecycle, kitehub) chưa được propagate sang education service (kiteclass-core) để enforce entitlement. Thiếu cơ chế tier-resolution server-side mà kiteclass-core tin được (per GAP-1020 bài học: KHÔNG dùng client-sent `X-Subscription-Tier`).

## Proposed Fix (Phase 1.5 — khi paid tier có ý nghĩa thực)

1. **Tier propagation cross-service:** gateway resolve subscription tier server-side (từ kitehub-subscription) → inject trusted header (vd `X-Subscription-Tier`, server-set NOT client) cho kiteclass-core; HOẶC kiteclass-core fetch/cache tier qua API/event. KHÔNG trust client-sent (per GAP-1020).
2. **Core entitlement enforcement:** kiteclass-core check tenant-wide caps (maxStudents/maxTeachers/storage) theo tier ở enroll/add-teacher/upload time → reject khi vượt + thông báo nâng gói.
3. **Định nghĩa entitlement matrix:** 1 nguồn canonical (rules.md hoặc ADR) liệt kê mỗi tier mở khoá gì (caps + features), thay vì rải rác enum + AI quota + gateway multiplier.

## Acceptance Criteria

- [ ] Cơ chế tier-resolution server-side cho kiteclass-core (không client-trust)
- [ ] kiteclass-core enforce ≥1 tier-cap thực (vd maxStudents tenant-wide) → reject + upgrade-prompt khi vượt
- [ ] Entitlement matrix canonical trong `subscription-billing/rules.md` (tier → caps + features)
- [ ] Test: TRIAL/FREE tenant bị chặn ở cap, PREMIUM tenant vượt được

## Related

- Discovered in: KH-3 G2 entitlement investigation 2026-06-09 (post Bug E/F/D re-walk)
- Slice gaps đã có: GAP-260 (gateway tier-multiplier enforcement, phase-1.5), GAP-1078 (AI provider tier-routing, phase-2), GAP-1020 (branding X-Subscription-Tier client-spoof, phase-1-beta P1)
- Business rules: `documents/01-business/kitehub/subscription-billing/rules.md` (SUB-01..20 = mechanics; thiếu entitlement matrix)
- Phase rationale: Phase 1 BETA gần như mọi tenant trial/beta → entitlement defer Phase 1.5 paid launch (paid tier phải "có teeth")
