# GAP-895: KH subscription cluster không có `version` (optimistic lock) toàn cluster

**Status:** 🔵 OPEN
**Priority:** 🟡 P2
**Domain:** Backend / DB
**Found:** 2026-06-03 (Wave 13 cluster docs writing — KH subscription/billing)
**Affects:** `kitehub-subscription` `BaseEntity` + `subscriptions`/`payments` tables

## Problem

`BaseEntity` (`kitehub-platform`) không khai báo `@Version`. So với KiteClass `BaseEntity` có `@Version Long version` + V62/V63 set DEFAULT 0.

Hệ quả: 2 caller race trên cùng subscription (auto-renew cron + admin manual extend) → ghi đè không có optimistic lock. Rủi ro đặc biệt `pending_payment_id` (admin upgrade flow vs cron expire).

## Proposed Fix

Migration V## thêm `version BIGINT NOT NULL DEFAULT 0` cho `subscriptions` + `payments`. Refactor `BaseEntity` (kitehub-platform) add `@Version`. Apply pattern xuyên suốt KH future tables.

## Acceptance Criteria

- [ ] Migration V## add version column
- [ ] BaseEntity (kh-platform) extends @Version
- [ ] IT test verify optimistic locking
- [ ] Reference cluster doc KH 02-subscription-billing §A9

## Discovered in

`documents/02-architecture/database/kitehub/02-subscription-billing.md` §A9
