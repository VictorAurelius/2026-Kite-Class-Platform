# GAP-1063: SePay webhook 200 response thiếu `{"success": true}` → SePay đánh "failed" + retry 7×

**Status:** 🟢 DONE
**Priority:** 🟠 P1 (production — mọi payment dù xử lý đúng vẫn bị SePay đánh failed + 7× retry + dashboard "chưa xác nhận")
**Domain:** Backend
**Found:** 2026-06-08 (SePay Test Mode end-to-end live verify — Approach B tunnel)
**Closed:** 2026-06-08 (fix + re-verified live via SePay simulator)
**Affects:** `kitehub-subscription` — `PaymentWebhookController.handlePaymentWebhook`

## Problem

End-to-end live test qua SePay Test Mode (real simulator → cloudflared tunnel → gateway → subscription): giao dịch `KH3SUBCAFE0001` xử lý đúng phía KiteHub (payment → COMPLETED, transaction_id=7066, PAID flip), HTTP **200**. NHƯNG **SePay delivery log báo "Thất bại — Response không đúng quy cách — Body thiếu `{"success": true}`"**.

Root cause: SePay ACK contract yêu cầu webhook receiver trả body chứa `{"success": true}` (HTTP 2xx) để xác nhận đã nhận. Handler trả `{"status": "success"}` → SePay không thấy `success: true` → đánh delivery failed → **auto-retry tối đa 7 lần** (theo webhook config) → SePay dashboard hiển thị giao dịch "Hệ thống của bạn chưa xác nhận giao dịch".

Hệ quả production: mọi payment thành công vẫn show "failed webhook" trên SePay + 7× redundant calls/giao dịch + admin không tin được trạng thái đối soát SePay.

**Vì sao chỉ lộ khi test với SePay thật:** Approach A (craft payload + self-assert) KHÔNG bao giờ bắt được — vì assert do mình kiểm soát, không theo contract SePay. Chỉ real-vendor test (Approach B) mới surface contract mismatch này. Validate giá trị của việc test thật.

## Fix (shipped + re-verified)

`PaymentWebhookController` — 4 response paths:
- success → `{"success": true, "status": "success"}`
- ignored (non-incoming) → `{"success": true, "status": "ignored"}`
- 401 (bad Apikey) → `{"success": false, "error": ...}`
- 400 (orphan/no-txnRef) → `{"success": false, "error": ...}`
- Method signature `ResponseEntity<Map<String,String>>` → `Map<String,Object>` (value giờ có Boolean)

## Acceptance Criteria

- [x] 200 ACK response body chứa `{"success": true}`
- [x] Local compile PASS (mvnw compile exit 0)
- [x] Re-verify live qua SePay simulator → delivery log SePay = **success** (không còn "Response không đúng quy cách")

## Related

- Surfaced by: SePay Test Mode Approach B (tunnel) live verify 2026-06-08, recipe `documents/05-guides/operations/sepay-webhook-local-verify-recipe.md`
- Sibling: GAP-1061 (SecurityConfig whitelist), GAP-1062 (applyPendingUpgrade rollback)
- SePay payload shape (verified): `{gateway, transactionDate, accountNumber, code, content, transferType, description, transferAmount, referenceCode, id}`
