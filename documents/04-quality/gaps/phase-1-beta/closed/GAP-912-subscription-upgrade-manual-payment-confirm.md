---
id: GAP-912
title: Subscription tier upgrade gated behind manual VietQR payment + admin confirm (Phase 1 BETA)
status: DONE
priority: P1
phase: phase-1-beta
audience: dev
found: 2026-06-04
last_verified: 2026-06-04
completion_pct: 100
related: [GAP-298, GAP-625, GAP-722, GAP-739, GAP-544, GAP-913]
---

# GAP-912 — Subscription upgrade gated behind manual VietQR payment confirm

## Problem

Trước đây `SubscriptionService.upgradeSubscription` áp tier mới **ngay lập tức** (SUB-07 cũ: "Immediate + prorated charge") rồi mới tạo PENDING payment. Với Phase 1 BETA dùng **chuyển khoản ngân hàng thủ công/VietQR** (User decision 2026-06-03 — giảm scope giấy phép/merchant/KYC, phù hợp beta nhỏ + solo-dev), hành vi này sai: tenant được nâng tier mà chưa thực sự thanh toán; không có cơ chế admin đối soát statement trước khi apply.

Feature này được làm như **WIP working-tree thuần** trên branch `feature/PR-billing-upgrade-manual-payment` (không gắn gap khi bắt đầu) → filed retroactively để vào CSV-canonical fix pipeline (per `discovery-to-gap-inline-filing.md` + `output-review-mandate.md` §3).

Khác domain với các gap payment hiện có: GAP-298 (KiteClass học phí phụ huynh→owner), GAP-625/626/627/722 (QR payment học phí KiteClass, phase-1.5-paid). Đây là **KiteHub subscription tier billing**.

## Proposed Fix

Gate upgrade sau admin confirm:
1. `upgradeSubscription` set `pendingTier` + tạo/reuse `Payment PENDING` (VietQR), KHÔNG đổi `tier`; idempotent qua `findLatestPendingBySubscriptionId`.
2. `applyPendingUpgrade` apply tier khi payment confirmed/completed; `clearPendingUpgrade` khi reject.
3. Admin confirm/reject qua `kitehub-admin` (`AdminController` `/api/platform/admin/payments/...` + `AdminPaymentsController` `/api/v1/admin/payments`).
4. Docs 3-layer: rules SUB-07/10/11/17/18/19, UC-SUB-02/07, api-contract admin endpoints.

## Acceptance Criteria

- [x] `upgradeSubscription` set pendingTier + PENDING payment, không flip tier
- [x] `applyPendingUpgrade` / `clearPendingUpgrade` + wiring confirm/reject
- [x] Idempotency (reuse pending payment; 400 nếu pending tier khác)
- [x] Docs 3-layer cập nhật cùng PR (Living Docs)
- [x] Unit + service tests xanh (`mvnw verify -P strict-warnings` → 769/0)
- [x] **Runtime walk** upgrade → admin confirm → tier applied trên stack chạy (per `feature-ship-runtime-walk-mandate`) — DONE 2026-06-04 (xem §Walk evidence; bắt + fix P0 GAP-913 instance_id drift)

## Out-of-scope (tracked separately)

| Item | Where |
|---|---|
| `SubscriptionBillingIT` chạy trên Postgres (H2 `SET_CONFIG` + `*IT` ngoài surefire path) | [[GAP-544]] |
| Cleanup `SubscriptionService.activateSubscription` dead code | Minor follow-up (non-functional; 0 caller verified) |
| `payment_content` "KITECLASS {id}" prefix (KH subscription nên "KITEHUB"?) + dev account_number rỗng | Minor — VietQR config + branding polish |
| api-contract ghi 409 cho "pending khác tier" nhưng impl trả 400 | Minor contract-vs-impl drift |

## Walk evidence (per feature-ship-runtime-walk-mandate §3)

Stack local: kite-postgres + kitehub-subscription(8081) + kitehub-admin(8085) + redis + rabbitmq (rebuild từ code mới). Auth qua gateway-header pattern (X-User-Id/X-User-Roles). Fixture: ACTIVE BASIC sub `81cf38cd` (sky-test, owner.test@test.vn).

| Path | HTTP | DB verify |
|---|---|---|
| OWNER upgrade BASIC→PREMIUM | 200 | tier=BASIC giữ, pendingTier=PREMIUM, PENDING payment instance_id populated, amount 633333 |
| Admin confirm payment | 200 | payment COMPLETED + txnId + paidAt; **subscription tier=PREMIUM** (1.5M), pending cleared |
| Admin reject (sad) | 200 | payment FAILED, tier giữ BASIC, pending cleared |
| Idempotency (upgrade 2x same tier) | 200 | same paymentId reused, 1 PENDING row (no dup) |
| Conflict (upgrade khác tier khi pending) | 400 | "already has a pending upgrade payment" |

**Blocker phát hiện + fixed cùng walk:** [[GAP-913]] — Payment entity thiếu `instanceId` (V58 NOT NULL) → payment insert vỡ trên Postgres (409). Fixed entity + 3 site → re-walk PASS.

## Related

- Shipped in: **PR #2140** (commit `e0d74e27`)
- [[GAP-544]] — subscription IT requires Postgres :5433 (lý do SubscriptionBillingIT không chạy CI)
- [[GAP-722]] — VietQR live payment Phase 1.5+ (auto-capture thay manual confirm)
- [[GAP-739]] — PaymentMethod enum cleanup (đã DONE)
- [[GAP-298]] — KiteClass manual bank-transfer reconciliation (domain khác, pattern tương tự)
