# GAP-1092: G2 gate cho phép curl-only bỏ lọt affordance FE-wired — campaign §1 "(UI/API)" loophole + no rule bind browser-UI vào G2

**Status:** 🟢 DONE
**Priority:** 🟠 P1
**Domain:** Mixed
**Found:** 2026-06-09 (Flow Verification Campaign — KH-5/KH-6/KC-8 G2 recipe curl-only miss)
**Affects:** `flow-verification-campaign.md` §1 G2 gate + `g2-handoff-md-mandate.md` §3.4 + 22-flow G2 recipe (`documents/05-guides/operations/*-g2-recipe-*.md`)

## Problem

G2 = human walk UI thật, nhưng recipe KH-5 (subscription downgrade/cancel/renew) + KH-6 (AI branding wizard) + KC-8 (parent portal) viết **curl-only** qua gateway `:9000` gắn header tay → bỏ lọt affordance FE-wired (user G2 không test đúng surface FE).

Bằng chứng FE-wired bị bỏ lọt (KH-5):
- downgrade CÓ FE: `kitehub-frontend/src/.../use-subscriptions.ts:81` `useDowngradeSubscription` + `(customer)/billing/upgrade/page.tsx:72,98`
- cancel CÓ FE: `(customer)/settings/components/DangerZone.tsx:14`
- renew KHÔNG có FE: BE `SubscriptionController.java:53` comment "no owner-facing FE" (operational/renewal-reminder view) → curl hợp lệ (tách thành GAP-1093)

Root cause (coverage gap, 3 lớp):
1. `flow-verification-campaign.md` §1 G2 gate ghi "Con người tự test flow trên local stack thành công **(UI/API)**" — "(UI/API)" = loophole cho phép curl thay browser.
2. `g1-browser-walk-before-flip.md` chỉ cover **G1** (flip → `🔄 walk-pass-pending-human`), KHÔNG cover G2 recipe format.
3. `g2-handoff-md-mandate.md` v1.0.0/v1.0.1 mandate FORMAT (7 sections) — KHÔNG mandate browser-vs-curl per affordance.

So sánh: KH-1/KH-2c/KH-3/KC-1 (✅ THÔNG) đều browser-walk → trải nghiệm đúng; KH-5..10/KC-8 curl-only → lệch surface.

## Root Cause

Không có rule nào bind "browser-UI cho affordance FE-wired" vào G2 recipe. Campaign §1 "(UI/API)" cho phép curl-only; sister rule `g1-browser-walk-before-flip` chỉ fire ở boundary G1, không phủ G2.

## Proposed Fix (shipped same session)

1. `flow-verification-campaign.md` §1 G2 gate: đổi "(UI/API)" → mandate browser-UI thật (đúng FE port per `kitehub-kiteclass-boundary` §2: KH `:3001`, KC `:3000`) cho MỌI affordance FE-wired; curl CHỈ cho BE-only (label rõ).
2. `g2-handoff-md-mandate.md` v1.0.2 §3.4: hard-requirement browser-walk-per-affordance FE-wired; curl supplement cho BE-only (mirror `g1-browser-walk-before-flip` §2 API-only carve-out) + §8.1 reviewer-checklist row + §7.5 worked self-test.
3. `output-review-mandate.md` §3 matrix row "G2 handoff recipe MD" extend cite v1.0.2 browser-walk mandate.

## Acceptance Criteria

- [x] `flow-verification-campaign.md` §1 G2 gate mandate browser-UI cho FE-wired affordance, curl chỉ BE-only
- [x] `g2-handoff-md-mandate.md` §3.4 hard-requirement browser-walk-per-affordance + §8.1 checklist row + §7.5 self-test (v1.0.2 MINOR)
- [x] `output-review-mandate.md` §3 row extend + Log entry
- [x] `rules-index.csv` g2-handoff version → 1.0.2

## Out-of-scope (track separately)

| Item | Where |
|---|---|
| Existing recipe KH-5/KH-6/KC-8 (+ KH-7..10/KC-10..12 curl-only secondary) re-verified browser-walk steps | Grandfathered per `g2-handoff-md-mandate.md` v1.0.2 prospective clause — re-verified naturally qua G2 walk loop (`flow-verification-campaign.md` §2 step 7); KH-5 G1 flip 2026-06-06 (trước `g1-browser-walk-before-flip` 2026-06-08) grandfathered |
| `renew` endpoint owner-facing FE confirm intended-absent vs gap | GAP-1093 |

## Related

- Rule fix: `g2-handoff-md-mandate.md` v1.0.2 + `flow-verification-campaign.md` §1
- Sister rule: `g1-browser-walk-before-flip.md` (G1 boundary), `kitehub-kiteclass-boundary.md` §2 (FE port)
- Spawned: GAP-1093 (renew BE-only no FE — confirm intended)
- Recipe affected: KH-5/KH-6/KC-8 (+ KH-7..10/KC-10..12 curl-only secondary)
- Pipeline: `incident-to-rule-pipeline.md` 5-stage; `discovery-to-gap-inline-filing.md`
