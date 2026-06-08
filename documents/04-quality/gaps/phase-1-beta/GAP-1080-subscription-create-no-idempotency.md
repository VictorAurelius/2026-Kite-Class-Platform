# GAP-1080: POST /subscriptions không idempotent — tạo PENDING row trùng mỗi lần gọi

**Status:** 🔵 OPEN
**Priority:** 🟡 P2
**Domain:** Backend
**Found:** 2026-06-09 (KH-3 G2 human walk — surfaced khi debug GAP-1079)
**Affects:** `kitehub-subscription` SubscriptionService.createSubscription

## Problem

`POST /api/platform/subscriptions` tạo 1 subscription PENDING row MỚI mỗi lần gọi — không idempotency, không guard "đã có PENDING subscription cho instance này". KH-3 G2 walk 2026-06-09: instance `7862ab7e` có 3 PENDING rows (1 từ user click + 2 từ curl debug). Mỗi click "Nâng cấp" / mỗi retry FE = 1 row rác.

Repro:
```
POST :9000/api/platform/subscriptions {instanceId, tier:BASIC, billingCycle:MONTHLY}
→ 201 (lần 1) + 201 (lần 2) → 2 PENDING rows cùng instance
```

## Proposed Fix

Idempotency: trước khi tạo, check instance đã có PENDING subscription chưa → nếu có, return existing (200) hoặc reject (409 Conflict) thay vì tạo trùng. Cân nhắc idempotency-key header (per GAP-1004 KC-7 sister pattern).

## Acceptance Criteria

- [ ] POST /subscriptions lần 2 cho instance đã có PENDING → return existing OR 409, KHÔNG tạo row mới
- [ ] Cleanup orphan PENDING rows (retention/abort scheduler hoặc unique constraint)

## Related

- Discovered in: KH-3 G2 human walk 2026-06-09
- Sister: GAP-1004 (KC-7 invoice over-payment + idempotency — same class); GAP-1079 (the primary blocker same walk)
