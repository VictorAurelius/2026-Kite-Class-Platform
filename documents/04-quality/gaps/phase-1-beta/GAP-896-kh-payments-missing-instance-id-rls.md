# GAP-896: KH `payments` thiếu `instance_id` → RLS bypass + cross-tenant leak risk

**Status:** 🔵 OPEN
**Priority:** 🟠 P1
**Domain:** Backend / DB / Security
**Found:** 2026-06-03 (Wave 13 cluster docs writing — KH subscription/billing)
**Affects:** `kitehub-subscription` `payments` table

## Problem

KH `payments` KHÔNG có cột `instance_id` — V34 explicit skip. Cô lập tenant **gián tiếp** qua FK `subscription_id → subscriptions.instance_id`.

Trade-off:
- ❌ Repository method `findBySubscriptionId(uuid)` KHÔNG check tenant ownership — caller pass subscription_id của tenant khác (URL param không validate) → leak.
- ❌ Future kh-subscription gain TenantContext + FORCE RLS sẽ KHÔNG cover `payments` cho tới khi denormalize `instance_id` từ subscription.

Cross-tenant leak risk medium — cần audit code path bằng tay.

## Proposed Fix

Migration V## denormalize `instance_id UUID NOT NULL` cho `payments` (backfill từ subscription FK) + add RLS policy. Update repository method enforce tenant filter.

## Acceptance Criteria

- [ ] Migration V## add `instance_id` + backfill + RLS
- [ ] Repository method tenant-aware
- [ ] IT test cross-tenant leak prevented
- [ ] Reference cluster doc KH 02-subscription-billing §A7

## Discovered in

`documents/02-architecture/database/kitehub/02-subscription-billing.md` §A7
