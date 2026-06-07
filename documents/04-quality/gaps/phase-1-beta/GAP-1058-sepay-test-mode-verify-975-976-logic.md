# GAP-1058: Verify GAP-975/976 SePay webhook logic via Test Mode (decouple from AWS restore)

**Status:** 🔵 OPEN
**Priority:** 🟠 P1
**Domain:** Backend
**Found:** 2026-06-08 (SePay runbook Test Mode discovery — verification path)
**Affects:** `kitehub-subscription` — `PaymentWebhookController` + `PaymentService.processSepayWebhook` (verification of GAP-975 + GAP-976)

## Problem

GAP-975 (dynamic VietQR txnRef + beta-amount override) + GAP-976 (SePay webhook Apikey auth + idempotency) đều ở 85% — phần "live verify 15% còn lại" bị treo vì verify path hiện tại (runbook §5) cần: tài khoản ngân hàng thật + tiền thật + production HTTPS domain live (chờ AWS restore — GAP-612). Cả 3 đều là blocker AWS/vendor-gated.

SePay có **Test Mode** (tài khoản giả lập + mô phỏng giao dịch + webhook test) — dữ liệu tách biệt, không tiền thật, không tài khoản thật, không đốt quota. Test Mode cho phép verify TOÀN BỘ logic webhook (txnRef matching `KH3SUB[A-F0-9]{8}`, idempotency theo `sepayId`, orphan → 400, PAID flip, `transferType=in` filter) **mà không phụ thuộc GAP-612 AWS restore**.

→ Phần logic-verify của GAP-975/976 có thể đóng qua Test Mode NGAY; chỉ còn lại 1 lần "real-money smoke" thật sự cần production (defer hợp lý tới GAP-612 unblock).

## Proposed Fix

1. Thiết lập SePay Test Mode (runbook §4.5 — 8 bước): tài khoản giả lập + API key test mode + webhook test trỏ về local stack qua tunnel (`cloudflared tunnel --url http://localhost:9000`) HOẶC staging deployed.
2. Set `SEPAY_API_KEY` = test-mode key trong local/staging env.
3. Mô phỏng giao dịch trên SePay Test Mode dashboard với nội dung chứa `txnRef` → trigger webhook.
4. Verify từng nhánh logic:
   - txnRef khớp invoice → PAID flip + `payment_records` row
   - txnRef không khớp → 400 orphan
   - resend cùng `sepayId` → idempotent (200 sớm, không double-process)
   - `transferType != in` → ignored 200
   - sai Apikey → 401
5. Cập nhật GAP-975 + GAP-976 completion_pct theo nhánh logic verified; chỉ giữ "real-money smoke" defer GAP-612.

## Acceptance Criteria

- [ ] SePay Test Mode thiết lập + webhook test reachable (local tunnel hoặc staging)
- [ ] 5 nhánh logic verified qua mô phỏng giao dịch (PAID flip / orphan 400 / idempotent / ignored / 401)
- [ ] GAP-975 + GAP-976 completion_pct cập nhật phản ánh logic-verify done (chỉ còn real-money smoke defer)
- [ ] Walk evidence ghi vào gap closure (per `feature-ship-runtime-walk-mandate.md` §3 — Test Mode = production-equivalent cho webhook logic)

## Related

- Verifies: GAP-975 (dynamic VietQR txnRef) + GAP-976 (webhook Apikey + idempotency) — both 85% PARTIAL
- Unblocks: logic-verify slice WITHOUT GAP-612 (AWS account restore)
- Runbook: `documents/05-guides/account-prep/sepay-account-setup-runbook.md` §4.5 (Test Mode 8-step + dual-credential config)
- Discovered in: SePay runbook Test Mode design review 2026-06-08
