---
wave_id: flow-kh3
tag_primary: flow-kh3
tags_secondary: [phase-1-beta, subscription, billing]
status: planned
created: 2026-06-04
owner: claude+nguyenvankiet
---

# Wave flow-kh3 — Subscription create + trial→paid migration

## §1 3-gate (per `flow-verification-campaign.md` §1)

| Gate | Owner | Criteria | Status |
|---|---|---|---|
| G1 — agent runtime walk | Claude | Subscription create on tenant Owner from KH-1 chain; trial countdown 14d → manual VietQR upgrade per `subscription/trial-to-paid.md` use-case; admin confirm payment → status flip PAID | ⬜ |
| G2 — human local test | User | Login as Owner (g2test-an-8 or similar from KH-1 closure) → create subscription → verify trial state → simulate trial expiry OR manual upgrade flow | ⬜ |
| G3 — production parity | Claude + User | Production: VietQR QR code generation reachable + payment confirm webhook (or admin manual path per Phase 1 BETA scope) + email notification sent via SES | ⬜ |

## §2 Loop protocol

Per `feature-ship-runtime-walk-mandate.md` §3.4:
1. State-check current Subscription entity + service code (BetaAccessService → SubscriptionService chain)
2. State-check VietQR provider config (subscription module)
3. Walk happy path: new tenant from KH-1 → POST create-subscription → DB row PAID/TRIAL
4. Walk trial→paid: admin manual confirm → status flip → notification email
5. Catalog blockers per `feature-ship-runtime-walk-mandate.md` §3.4 catalog-then-batch
6. Fix iterate to G1 PASS
7. Hand to user G2 + G3 parity

## §3 Scope — TBD at session start

Read these before scoping:
- `documents/01-business/kitehub/subscription/rules.md`
- `documents/01-business/kitehub/subscription/use-cases.md` (trial-to-paid)
- `documents/01-business/kitehub/subscription/api-contract.md`
- Recent commit `ac54a419 feat(subscription): gate tier upgrade behind manual VietQR payment confirm`

Dependency check: Owner from KH-1+KH-2c chain ✅ (g2test-an-8 tenant exists per Wave flow-kh1 closure)

## §4 Test data

Use existing tenant from Wave flow-kh1 closure (g2test-an-8) — Owner already logged in via KH-2c chain; subscription create is next step.

## §5 Log

- **2026-06-04 (plan ship)**: Stub plan filed at Wave flow-kh1 session-end as KH-3 is logically next per campaign §3 dependency graph (KH-2c → KH-3). Owner exists from KH-1 closure; subscription module ready (recent commit ac54a419 manual VietQR gate). Next session: `/start-session` → expand §3 Scope via state-check → walk G1.
