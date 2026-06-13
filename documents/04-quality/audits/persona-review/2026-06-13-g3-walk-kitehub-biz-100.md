---
title: G3 Production-Parity Walk — wave-kitehub-biz-100 (KH-3 + KH-5 billing lifecycle)
audience: mixed
status: complete
created: 2026-06-13
wave: wave-kitehub-biz-100
gate: G3 (production-parity, gateway :9000 + Postgres/Flyway + JWT→header)
---

# G3 Walk — wave-kitehub-biz-100

**Phạm vi:** Walk lifecycle billing KiteHub (KH-3 nâng/hạ gói, KH-5 gia hạn/hủy/kích hoạt lại + win-back) qua **gateway :9000** (JWT HS512 → header thật, KHÔNG BE-direct curl), trên Postgres + Flyway V73 (`suspended_at`) + V74 (`in_app_notifications`) đã apply. Code đang chạy = `wave/kitehub-biz-100-final` (HEAD `9ebf2ca41`).

**Setup:** Seed 2 instance TRIAL/FREE — A (`aaaa1111-…0001`, owner A) là walk target, B (`bbbb2222-…0002`, owner B) làm IDOR attacker. Mint 3 JWT (OWNER-A / OWNER-B / PLATFORM_ADMIN) ký bằng gateway `JWT_SECRET`; gateway `TenantHeaderGuardFilter` inject `X-Tenant-Id` từ claim `tenantId`. Tất cả call qua `http://localhost:9000`.

## Verdict table

| # | Flow | Verdict | HTTP | Bằng chứng |
|---|---|---|---|---|
| 1 | KH-3 upgrade — create PENDING (SUB-20) | ✅ PASS | 201 | `status=PENDING tier=FREE pendingTier=PREMIUM priceVnd=1500000 pendingPaymentId` set |
| 2 | KH-3 upgrade — admin confirm → ACTIVE + SUB-21 tier sync | ⚠️ PASS-with-P1 | 200 | Khi `contact_email` có: confirm → sub `ACTIVE/PREMIUM` + `instances.tier=PREMIUM` + `/active`=200 ✓. **Nhưng khi `contact_email` NULL → upgrade rollback ngầm, confirm vẫn trả 200 + payment COMPLETED (split-brain).** Xem Bug P1 |
| 3 | KH-3 downgrade-preview (SUB-26 / GAP-1261) | ✅ PASS | 200 | `currentTier=PREMIUM→BASIC`, caps 200→50 hs / 20→5 gv / 10240→2048 MB, custom-domain disabled, 4 cảnh báo tiếng Việt + `usageDataNote` |
| 4 | KH-3 edge — no-active-sub → 404 (GAP-1079) | ✅ PASS | 404 | `/instance/B/active` → 404 RFC-7807 "No active subscription" (đúng 404 không phải 400) |
| 5 | KH-3 edge — idempotent double-create (GAP-1080) | ✅ PASS | 201×2 | Create B PREMIUM 2 lần → **cùng** `subscriptionId` + **cùng** `pendingPaymentId`; DB chỉ 1 row PENDING (no-dup) |
| 6 | KH-5 renew (SUB-23 / GAP-1016 no-free-extend) | ✅ PASS | 204 | `expiresAt` GIỮ NGUYÊN (2026-07-13) + tạo Payment PENDING mới (`pending_payment_id` set) — không gia hạn miễn phí |
| 7 | KH-5 cancel immediate + suspended_at (GAP-1264) | ✅ PASS | 204 | `DELETE ?immediate=true` → instance `SUSPENDED` + **`instances.suspended_at` được stamp** (2026-06-13 16:53:04) qua `Instance.setStatus()` override + sub `CANCELLED` |
| 8 | Win-back IN_APP + email on cancel (GAP-1263) | ⚠️ DEFERRED (by-design) | — | KHÔNG fire inline trên immediate-cancel. Win-back = scheduler-driven (`SubscriptionExpirationChecker.sendWinBackBestEffort` → `OwnerNotificationDispatcher.sendWinBack`, IN_APP+EMAIL) khi instance đã lapsed. Không verify được đồng bộ trong walk. Xem ghi chú |
| 9 | Reactivate → PAYMENT_REQUIRED (GAP-1263) | ✅ PASS | 200 | `outcome=PAYMENT_REQUIRED`, `churnType=VOLUNTARY`, reuse pending payment, message tiếng Việt "Vui lòng thanh toán để kích hoạt lại…" |
| 10 | IDOR cross-tenant | ✅ PASS | 403 | OWNER-B → `/instance/A/active` → 403 "Access denied" (TenantOwnershipGuard) |

**Tổng: 8 PASS, 1 PASS-with-P1 (KH-3 upgrade confirm), 1 DEFERRED-by-design (win-back inline).**

## 🔴 Bug P1 — Upgrade-apply rollback bị nuốt → split-brain payment COMPLETED / sub PENDING

**Triệu chứng (runtime, lần walk đầu, instance A chưa có `contact_email`):**
- `POST /admin/payments/{id}/confirm` (ADMIN) trả **HTTP 200** + payment `status=COMPLETED`.
- Nhưng `subscriptions`: vẫn `tier=FREE status=PENDING pending_tier=PREMIUM`; `instances.tier=FREE status=TRIAL`; `/instance/A/active` → **404**.
- → DB split-brain: tiền đã "COMPLETED" nhưng gói KHÔNG được nâng, không có tín hiệu lỗi nào trả về admin/FE.

**Root cause (xác định qua log container `kitehub-subscription` lúc 16:47):**
```
ERROR PaymentService - Failed to apply pending upgrade for subscription: ee32c9c0-…
org.springframework.dao.DataIntegrityViolationException: … insert into email_sent_log
  … null value in column "recipient" of relation "email_sent_log" violates not-null constraint
  at JpaTransactionManager.doCommit(…)   ← lỗi nổ tại COMMIT, ngoài try/catch
```

Chuỗi lỗi:
1. `SubscriptionService.applyPendingUpgrade` (create-flow) gọi `emailServiceClient.sendSubscriptionCreatedEmail(instance.getId(), instance.getContactEmail()=NULL, …)` → persist `EmailSentLog(recipient=NULL)` vào persistence context.
2. `try/catch` quanh lời gọi email **không bắt được** vì INSERT thật bị defer tới flush/commit (sau khi catch đã đi qua).
3. Tại commit, Hibernate flush → `email_sent_log.recipient` NOT-NULL violation → **toàn bộ transaction `applyPendingUpgrade` rollback** (tier-flip + `instances.tier` sync đều bị revert).
4. Exception trồi lên `PaymentService.confirmPayment` → bị **try/catch nuốt (chỉ log.error)** → vẫn `return 200` + payment đã COMPLETED (commit ở transaction trước đó).

**Hai khiếm khuyết cấu trúc (latent, độc lập với seed):**
- **D1 — coupling:** việc ghi `EmailSentLog` (audit/notification) nằm CÙNG transaction với tier-flip nghiệp vụ → bất kỳ lỗi nào ở nhánh email-log (null recipient / constraint / DB hiccup) đều rollback state nghiệp vụ cốt lõi. `try/catch` quanh `sendSubscriptionCreatedEmail` cho cảm giác an toàn GIẢ vì INSERT defer tới commit.
- **D2 — swallow + lie:** `confirmPayment` nuốt exception của `applyPendingUpgrade` rồi trả 200 + payment COMPLETED trong khi gói chưa nâng → split-brain, không retry path, không tín hiệu cho admin.

**Tính chất seed vs prod:** Trigger tức thời (`contact_email` NULL) là **seed artifact** — beta-signup thật populate `contact_email` (`AuthService.java:223 setContactEmail(ownerEmail)`). Khi set `contact_email` cho instance A rồi re-walk: confirm → sub `ACTIVE/PREMIUM` + `instances.tier=PREMIUM` ✓ (SUB-21 sync chạy đúng). **Nhưng D1+D2 vẫn là rủi ro production thật** — bất kỳ lỗi nào trong nhánh email-log lúc upgrade-apply đều silent-rollback gói trong khi báo 200 cho admin. Có thể liên quan GAP-1256 (`tier-desync-rollback-suspend-cancel-expiry-sub21-sweep`).

**Đề xuất fix (không thực hiện trong walk này):** tách `EmailSentLog`/notification ra khỏi transaction tier-flip (after-commit / best-effort / separate tx), HOẶC làm `confirmPayment + applyPendingUpgrade` atomic (rollback cả payment nếu upgrade fail) thay vì nuốt exception trả 200; guard recipient NULL trước khi persist `EmailSentLog`.

> **Fix follow-up:** GAP-1273 (`phase-1-beta/GAP-1273-confirm-payment-split-brain-email-log-tx-coupling.md`) — D1 (REQUIRES_NEW + null-recipient guard) + D2 (confirmPayment surface genuine tier-flip failure) shipped trên `wave/kitehub-biz-100-p1fix`.

## Ghi chú — Win-back (GAP-1263) là scheduler-driven, không inline

`OwnerNotificationDispatcher.sendWinBack` (IN_APP + EMAIL) chỉ được gọi từ `SubscriptionExpirationChecker.sendWinBackBestEffort` (cron, nhánh involuntary line 125 / voluntary-lapsed line 149) khi instance đã lapsed — KHÔNG gọi inline trong `cancelSubscription`. Vì immediate-cancel set `expiresAt=now()`, win-back SẼ fire ở lần scheduler chạy kế (nhánh voluntary-lapsed), nhưng không đồng bộ trong walk.

- 2 row `in_app_notifications` của A thực chất title "Thanh toán đã được xác nhận" (từ payment-confirmed lúc 16:47/16:49) — **không phải** win-back.
- MailHog có "Gói đăng ký đã kích hoạt" + "Thanh toán đã được xác nhận" — **không có** win-back email.
- → Hạ tầng channel IN_APP + EMAIL đã hoạt động (chứng minh qua payment-confirmed); riêng trigger win-back-on-cancel là async/scheduler. Recipe kỳ vọng inline → mismatch, cần trigger scheduler hoặc chờ cron để verify đầy đủ. Reactivate (nửa còn lại của GAP-1263) chạy đúng tại runtime.

## Điểm mạnh quan sát được
- Gateway JWT→header + `TenantOwnershipGuard`/`requireOwnedSubscription` chặn IDOR đúng (403) trên mọi endpoint scoped.
- SUB-20 → SUB-21 chain (pending payment → admin confirm → tier flip + `instances.tier` sync qua `InstanceTierSyncService`) đúng khi prerequisite data đủ.
- `suspended_at` stamp qua override `Instance.setStatus()` (stamp khi vào SUSPENDED, clear khi ra, idempotent re-suspend) — thiết kế xác định-thời-điểm tốt cho retention clock (GAP-1264).
- downgrade-preview + reactivate message + win-back đều localized tiếng Việt đúng (VND, cap warnings).
- Idempotency double-submit (GAP-1080) + 404-not-400 (GAP-1079) đều đúng.

## Artifacts
- Seed/helpers (read-only reuse): `.claude/worktrees/agent-a307d767fd106a4ae/.g3-scratch/{seed-g3-biz100.sql, walk-helpers.sh}` + mint `.claude/g3-walk-scratch/mint.py`.
- Walk instances A=`aaaa1111-0000-0000-0000-000000000001`, B=`bbbb2222-0000-0000-0000-000000000002` (TRIAL/FREE seed, no FK/RLS).
- **Copied onto `wave/kitehub-biz-100-p1fix`** by the P1-FIX agent (GAP-1273) so the fix PR carries its originating walk report.
