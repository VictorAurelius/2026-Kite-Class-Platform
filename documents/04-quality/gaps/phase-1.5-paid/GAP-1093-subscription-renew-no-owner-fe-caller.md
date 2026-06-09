# GAP-1093: Subscription `renew` endpoint không có owner-facing FE caller — confirm intended vs FE-wiring gap

**Status:** 🔵 OPEN
**Priority:** 🟢 P3
**Domain:** Frontend
**Found:** 2026-06-09 (tách từ GAP-1092 — KH-5 G2 recipe affordance audit)
**Affects:** `kitehub-subscription` `SubscriptionController.renewSubscription` (`POST /api/platform/subscriptions/{id}/renew`) + `kitehub-frontend` (KH-5 subscription lifecycle)

## Problem

Khi audit affordance FE-wired cho KH-5 (per GAP-1092), phát hiện `renew` endpoint **0 owner-facing FE caller**:
- BE `SubscriptionController.java:53` (comment) + `:209` `@PostMapping("/{id}/renew")` tự ghi "no owner-facing FE — operational/renewal-reminder view".
- Khác downgrade/cancel (CÓ FE: `use-subscriptions.ts` + `(customer)/billing/upgrade/page.tsx` + `(customer)/settings/components/DangerZone.tsx`).

Câu hỏi cần chốt: `renew` không có FE owner-facing là **intended** (auto-renewal job + manual operational/admin) hay **FE-wiring gap** (owner nên tự renew từ billing UI)?

Nhiều khả năng intended: renewal Phase 1 BETA = auto (scheduled) + manual operational; owner-facing self-renew có thể defer Phase 1.5 paid (cùng cluster billing UI maturity). BE comment "no owner-facing FE" gợi ý decision đã có.

## Proposed Fix

Clarify intended vs gap:
- Nếu intended (auto-renew + operational-only) → document trong `documents/01-business/.../subscription/use-cases.md` rằng owner self-renew out-of-scope Phase 1 BETA; close WONTFIX.
- Nếu FE-wiring gap → file Phase 1.5 work thêm `useRenewSubscription` + nút renew trong billing UI.

## Acceptance Criteria

- [ ] Chốt: `renew` owner-facing FE = intended-absent HOẶC gap
- [ ] Nếu intended → document trong use-cases.md + close WONTFIX
- [ ] Nếu gap → file Phase 1.5 FE renew wiring task

## Related

- Parent: GAP-1092 (G2 curl-only affordance miss)
- BE: `SubscriptionController.java:53,209` (comment "no owner-facing FE")
- KH-5 flow: `flow-verification-campaign.md` §4 row KH-5
- Sibling FE-wired affordance: downgrade/cancel (have FE)
