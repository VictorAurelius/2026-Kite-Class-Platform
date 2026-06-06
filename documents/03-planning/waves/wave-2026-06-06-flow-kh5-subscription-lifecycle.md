---
title: Wave flow-kh5 — KH-5 Subscription lifecycle G1 walk
status: complete
created: 2026-06-06
updated: 2026-06-06
waves: [flow-kh5]
wave: wave-2026-06-06-flow-kh5
tag_primary: flow-kh5
tags_secondary: [subscription, lifecycle, idor, billing]
date: 2026-06-06
flow: KH-5 (Subscription downgrade / cancel / renew)
gaps: [GAP-1015, GAP-1016, GAP-1017, GAP-1018]
---

# Wave flow-kh5 — KH-5 Subscription lifecycle G1 walk

**Mục tiêu:** G1 (agent runtime walk) cho flow KH-5 — Owner quản lý vòng đời subscription: downgrade / cancel / renew. Flow thứ cấp đầu tiên trong đợt G1-cho-tất-cả-flow (sequencing chốt 2026-06-06).

## 1. Brainstorm

KH-5 là 1 trong 9 flow secondary (⬜ chưa walk). Endpoint surface: `PATCH /{id}/downgrade`, `DELETE /{id}` (cancel), `POST /{id}/renew` — tất cả qua gateway :9000 với Owner JWT. Stack-up + gateway auth chain đã known-good từ KH-3/KH-4. Pre-walk persona simulation (Opus, per `pre-walk-persona-simulation-mandate.md`) trả về 10 failure mode; FM-1..FM-4 cao nhất.

## 2. Task Breakdown

1. Stack up (đã sẵn — services healthy) + login owner.test qua gateway.
2. Walk happy path 3 endpoint + capture HTTP + DB + side effect.
3. Walk ≥1 sad path mỗi endpoint + confirm/refute 10 failure mode.
4. Batch-fix blocker-class bug (per `feature-ship-runtime-walk-mandate.md` §3.4) → 1 rebuild → re-walk.
5. File gap cho finding còn lại + flip campaign row.

## 3. Scope

Walk-only G1 cho 3 endpoint lifecycle (downgrade/cancel/renew) qua gateway. Trong scope: fix inline blocker-class (NPE 500, state corruption). Ngoài scope (→ gap): IDOR ownership binding (cần gateway change), payment-gate redesign, instance suspend side-effect, billing-cycle correctness. Walk solo (coordinator), KHÔNG parallel agent.

## 4. State-Check Evidence

| Symbol | Verdict | Evidence |
|---|---|---|
| `SubscriptionController.downgradeSubscription/cancelSubscription/renewSubscription` | ✅ present | controller line 130/147/164 |
| `SubscriptionRenewalService.manualRenewal` | ✅ present | line 105-132 |
| `SubscriptionService.downgradeSubscription/cancelSubscription` | ✅ present | line 224/257 |
| Schema `subscriptions` (version, pending_tier, expires_at nullable) | ✅ present | `information_schema` — no drift (entity stricter than DB, safe direction) |

## 5. Verification Gates

### Pre-walk
Pre-walk Opus persona simulation → 10 failure mode, artifact `documents/04-quality/audits/persona-review/2026-06-06-pre-walk-kh5-subscription-lifecycle.md`.

### G1 walk — evidence

Credential: `owner.test@test.vn / Test@1234` (tenant `22003e3c…`), login `/api/auth/login` via gateway :9000. Subscription `81cf38cd…` (BASIC/ACTIVE).

**Happy paths (PASS):**

| Endpoint | Kết quả | Side effect verified |
|---|---|---|
| `POST /{id}/renew` | HTTP 204 | `expiresAt` +1 tháng, `version`++ |
| `PATCH /{id}/downgrade` `{newTier:FREE}` | HTTP 200 | `pendingTier=FREE` set |
| `DELETE /{id}?immediate=true` | HTTP 204 | status CANCELLED, `expiresAt=now`, autoRenew=false |

**Sad paths (PASS — hành vi đúng):**

| Case | Kết quả |
|---|---|
| Double-cancel | HTTP 204 idempotent no-op |
| Renew sub đã CANCELLED | HTTP 400 + message rõ ràng |

**Bugs surfaced (6) — 2 fix inline, 4 file gap:**

| FM | Severity | Verdict | Trạng thái |
|---|---|---|---|
| FM-2 NPE renew PENDING (null expiry) → 500 | P1 | **FIXED inline** | guard PENDING/null → 400; re-walk PASS |
| FM-5 downgrade ghi đè pending upgrade payment → corruption | P1 | **FIXED inline** | guard pendingPaymentId → 400; re-walk PASS (pending_tier giữ PREMIUM) |
| FM-1 IDOR cross-tenant (GET/cancel/downgrade/renew sub tenant khác) | **P0** | GAP-1015 | cần gateway forward tenantId + ownership guard |
| FM-3 manual renew miễn phí (không tạo payment) | P1 | GAP-1016 | |
| FM-4 cancel không suspend instance | P1 | GAP-1017 | |
| FM-6/7/10 billing cycle + pending-downgrade + idempotency + downgrade-FREE | P2 | GAP-1018 | |

**Gate G1: ✅ PASS** — 3 endpoint reachable + happy/sad đúng + 2 blocker fix + re-walk verify.
**Gate G2 (human local walk): ⬜ pending** — chờ đợt G2 tập trung (sau khi G1 đủ 22 flow).
**Gate G3 (production parity): ⬜ pending.**

### Inline fixes (this wave)

1. `SubscriptionRenewalService.manualRenewal()` — guard `status==PENDING || expiresAt==null` → IllegalArgumentException (400) thay vì NPE (500). Test `shouldRejectManualRenewOfPendingSubscription`.
2. `SubscriptionService.downgradeSubscription()` — guard `pendingPaymentId != null` → reject downgrade khi đang có pending tier-change payment (mirror upgrade guard). Test `shouldRejectDowngradeWhenPendingPaymentExists`.

`./mvnw test` (SubscriptionServiceTest + SubscriptionRenewalServiceTest): 23/23 PASS. Re-walk live sau rebuild: FM-2 → 400 ✓, FM-5 → 400 + no corruption ✓.

## 6. Agent Spawn Pattern

N/A — walk solo (coordinator). Chỉ 1 Opus background agent cho pre-walk persona simulation (per `pre-walk-persona-simulation-mandate.md` + `agent-model-opus-default.md`); không spawn parallel bucket agent vì G1 walk là sequential single-stack.

## 7. Closure Protocol

### Discoveries filed (per `discovery-to-gap-inline-filing.md` §3)

- GAP-1015: Subscription lifecycle IDOR cross-tenant (P0, Backend)
- GAP-1016: Manual renewal miễn phí — không tạo payment (P1, Backend)
- GAP-1017: Cancel không suspend instance (P1, Backend)
- GAP-1018: Manual renewal hardening cluster — billing cycle/pending-downgrade/idempotency/downgrade-FREE (P2, Backend)

### Sync targets

- Campaign `flow-verification-campaign.md` §4 KH-5 row → `🔄 walk-pass-pending-human`
- `wave-history.jsonl` flow-kh5 entry appended
- `gap-status.csv` 4 rows (GAP-1015..1018)
- `audits-index.csv` pre-walk artifact row
- Seeded subscription `81cf38cd…` restore về BASIC/ACTIVE sau walk

### Outcome

KH-5 **G1 ✅ PASS**. **Lưu ý cho người review/G2:** FM-1 IDOR (GAP-1015) là P0 cross-tenant security — nên ưu tiên fix sớm (cùng class KC-7 GAP-1005 / KC-8 GAP-1007); ảnh hưởng cả KH-4 upgrade.

## 8. Log

- **2026-06-06:** Wave flow-kh5 — KH-5 G1 walk complete. 2 inline fixes (FM-2 NPE→400, FM-5 downgrade corruption→400) + 4 gaps filed (GAP-1015 P0 IDOR + GAP-1016/1017 P1 + GAP-1018 P2). PR #2196. Campaign row → walk-pass-pending-human.
