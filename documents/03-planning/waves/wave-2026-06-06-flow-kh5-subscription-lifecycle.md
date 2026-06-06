---
wave: wave-2026-06-06-flow-kh5
tag_primary: flow-kh5
tags_secondary: [subscription, lifecycle, idor, billing]
status: complete
date: 2026-06-06
flow: KH-5 (Subscription downgrade / cancel / renew)
---

# Wave flow-kh5 — KH-5 Subscription lifecycle G1 walk

**Mục tiêu:** G1 (agent runtime walk) cho flow KH-5 — Owner quản lý vòng đời subscription: downgrade / cancel / renew. Flow thứ cấp đầu tiên trong đợt G1-cho-tất-cả-flow (sequencing chốt 2026-06-06).

## 1. Brainstorm

KH-5 là 1 trong 9 flow secondary (⬜ chưa walk). Endpoint surface: `PATCH /{id}/downgrade`, `DELETE /{id}` (cancel), `POST /{id}/renew` — tất cả qua gateway :9000 với Owner JWT. Stack-up + gateway auth chain đã known-good từ KH-3/KH-4. Pre-walk persona simulation (Opus, per `pre-walk-persona-simulation-mandate.md`) trả về 10 failure mode; FM-1..FM-4 cao nhất.

## 2. State-Check Evidence

| Symbol | Verdict | Evidence |
|---|---|---|
| `SubscriptionController.downgradeSubscription/cancelSubscription/renewSubscription` | ✅ present | controller line 130/147/164 |
| `SubscriptionRenewalService.manualRenewal` | ✅ present | line 105-132 |
| `SubscriptionService.downgradeSubscription/cancelSubscription` | ✅ present | line 224/257 |
| Schema `subscriptions` (version, pending_tier, expires_at nullable) | ✅ present | `information_schema` — no drift (entity stricter than DB, safe direction) |

## 3. G1 walk — evidence

Credential: `owner.test@test.vn / Test@1234` (tenant `22003e3c…`), login `/api/auth/login` via gateway :9000. Subscription `81cf38cd…` (BASIC/ACTIVE).

### Happy paths (PASS)
| Endpoint | Kết quả | Side effect verified |
|---|---|---|
| `POST /{id}/renew` | HTTP 204 | `expiresAt` +1 tháng, `version`++ |
| `PATCH /{id}/downgrade` `{newTier:FREE}` | HTTP 200 | `pendingTier=FREE` set |
| `DELETE /{id}?immediate=true` | HTTP 204 | status CANCELLED, `expiresAt=now`, autoRenew=false |

### Sad paths (PASS — hành vi đúng)
| Case | Kết quả |
|---|---|
| Double-cancel | HTTP 204 idempotent no-op |
| Renew sub đã CANCELLED | HTTP 400 + message rõ ràng |

### Bugs surfaced (6) — 2 fix inline, 4 file gap
| FM | Severity | Verdict | Trạng thái |
|---|---|---|---|
| FM-2 NPE renew PENDING (null expiry) → 500 | P1 | **FIXED inline** | guard PENDING/null → 400; re-walk PASS |
| FM-5 downgrade ghi đè pending upgrade payment → corruption | P1 | **FIXED inline** | guard pendingPaymentId → 400; re-walk PASS (pending_tier giữ PREMIUM) |
| FM-1 IDOR cross-tenant (GET/cancel/downgrade/renew sub tenant khác) | **P0** | GAP-1015 | cần gateway forward tenantId + ownership guard |
| FM-3 manual renew miễn phí (không tạo payment) | P1 | GAP-1016 | |
| FM-4 cancel không suspend instance | P1 | GAP-1017 | |
| FM-6/7/10 billing cycle + pending-downgrade + idempotency + downgrade-FREE | P2 | GAP-1018 | |

## 4. Inline fixes (this wave)

1. `SubscriptionRenewalService.manualRenewal()` — guard `status==PENDING || expiresAt==null` → IllegalArgumentException (400) thay vì NPE (500). Test `shouldRejectManualRenewOfPendingSubscription`.
2. `SubscriptionService.downgradeSubscription()` — guard `pendingPaymentId != null` → reject downgrade khi đang có pending tier-change payment (mirror upgrade guard). Test `shouldRejectDowngradeWhenPendingPaymentExists`.

`./mvnw test` (SubscriptionServiceTest + SubscriptionRenewalServiceTest): 23/23 PASS. Re-walk live sau rebuild: FM-2 → 400 ✓, FM-5 → 400 + no corruption ✓.

## 5. Discoveries filed (per discovery-to-gap-inline-filing.md §3)

- GAP-1015: Subscription lifecycle IDOR cross-tenant (P0, Backend)
- GAP-1016: Manual renewal miễn phí — không tạo payment (P1, Backend)
- GAP-1017: Cancel không suspend instance (P1, Backend)
- GAP-1018: Manual renewal hardening cluster — billing cycle/pending-downgrade/idempotency/downgrade-FREE (P2, Backend)

## 6. Outcome

KH-5 **G1 ✅ PASS** — cả 3 endpoint reachable + happy + sad path đúng; 2 blocker-class bug (500 NPE + state corruption) đã fix inline + re-walk verify; 4 gap (1 P0 security + 3 P1/P2) filed cho follow-up. Campaign row KH-5 → `🔄 walk-pass-pending-human`.

**Lưu ý cho người review/G2:** FM-1 IDOR (GAP-1015) là P0 cross-tenant security — nên ưu tiên fix sớm (cùng class KC-7 GAP-1005 / KC-8 GAP-1007); ảnh hưởng cả KH-4 upgrade. Seeded subscription `81cf38cd…` đã restore về BASIC/ACTIVE sau walk.
