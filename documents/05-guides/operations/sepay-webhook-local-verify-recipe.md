# SePay Webhook Local Verify Recipe — GAP-975/976 logic (decoupled khỏi AWS/vendor)

**Mục tiêu:** Verify toàn bộ logic webhook SePay (`PaymentWebhookController` + `PaymentService.processSepayWebhook`) trên **local Docker stack**, KHÔNG cần SePay dashboard / tunnel / production HTTPS / AWS restore (GAP-612). Đóng phần "logic-verify" của GAP-975 (dynamic VietQR txnRef) + GAP-976 (Apikey auth + idempotency) theo GAP-1058.

**Nguyên lý:** Cái cần test là *handler của mình*, không phải SePay. Nên tự craft payload SePay giả rồi POST thẳng vào endpoint với Apikey → exercise đủ mọi nhánh. SePay Test Mode (runbook §4.5) chỉ cần khi muốn test *end-to-end qua SePay simulator* (defer).

**Thời lượng:** ~10 phút (stack đã chạy sẵn).

---

## 0. Prerequisites

| Mục | Lệnh / Trạng thái |
|---|---|
| Stack local chạy | `bash kitehub/scripts/up.sh --profile full` → 14 container healthy |
| `SEPAY_API_KEY` set trong subscription | compose `kitehub-subscription` env có `SEPAY_API_KEY: ${SEPAY_API_KEY:-dev-sepay-test-key-local}` (wired 2026-06-08) |
| Webhook path whitelisted | subscription SecurityConfig có `.requestMatchers("/api/platform/webhooks/**").permitAll()` (GAP-1061 fix) |
| txnRef format | regex `KH3SUB[A-F0-9]{8}` (uppercase hex), vd `KH3SUBDEAD1234` |

Verify nhanh:
```bash
docker exec kitehub-subscription printenv SEPAY_API_KEY    # → dev-sepay-test-key-local
```

---

## 1. Seed 1 payment PENDING

Payment cần subscription_id **tồn tại + chưa soft-deleted** (nếu trỏ subscription `deleted=true` → `applyPendingUpgrade` throw → GAP-1062 rollback). Lấy 1 subscription ACTIVE rồi seed:

```bash
SUB=$(docker exec kite-postgres psql -U kitehub -d kitehub -t -A -c \
  "SELECT id FROM subscriptions WHERE deleted=false AND status='ACTIVE' LIMIT 1;")
INST=$(docker exec kite-postgres psql -U kitehub -d kitehub -t -A -c \
  "SELECT instance_id FROM payments LIMIT 1;")
docker exec kite-postgres psql -U kitehub -d kitehub -c "
INSERT INTO payments (id, subscription_id, amount_vnd, currency, payment_method, status, txn_ref, payment_content, created_at, updated_at, deleted, instance_id, version)
VALUES (gen_random_uuid(), '$SUB', 10000, 'VND', 'VIETQR', 'PENDING', 'KH3SUBDEAD1234', 'local verify', now(), now(), false, '$INST', 0);"
```

---

## 2. Chạy 8 nhánh — POST trực tiếp subscription :8081

> Direct `:8081` bypass gateway để isolate handler. (Production: SePay → gateway `/api/platform/webhooks/payment` → subscription; gateway whitelist `isPublicPath` đã có.)

```bash
URL="http://localhost:8081/api/platform/webhooks/payment"
KEY="dev-sepay-test-key-local"; CT="Content-Type: application/json"
t() { code=$(curl -s -o /tmp/b -w '%{http_code}' -X POST "$URL" "${@:2}"); printf "%-40s → HTTP %s | %s\n" "$1" "$code" "$(cat /tmp/b)"; }

t "B5a wrong Apikey"   -H "Authorization: Apikey WRONG" -H "$CT" -d '{"id":"X1","transferType":"in","transferAmount":10000,"description":"KH3SUBDEAD1234"}'
t "B5b no Apikey"      -H "$CT" -d '{"id":"X2","transferType":"in","transferAmount":10000,"description":"KH3SUBDEAD1234"}'
t "B4 transferType=out" -H "Authorization: Apikey $KEY" -H "$CT" -d '{"id":"X3","transferType":"out","transferAmount":10000,"description":"KH3SUBDEAD1234"}'
t "B2a orphan txnRef"  -H "Authorization: Apikey $KEY" -H "$CT" -d '{"id":"X4","transferType":"in","transferAmount":10000,"description":"KH3SUBFFFFFFFF"}'
t "B2b no txnRef"      -H "Authorization: Apikey $KEY" -H "$CT" -d '{"id":"X5","transferType":"in","transferAmount":10000,"description":"random memo"}'
t "B1 happy"           -H "Authorization: Apikey $KEY" -H "$CT" -d '{"id":"SEPAY-TXN-001","transferType":"in","transferAmount":10000,"description":"Thanh toan KH3SUBDEAD1234"}'
t "B3 replay sepayId"  -H "Authorization: Apikey $KEY" -H "$CT" -d '{"id":"SEPAY-TXN-001","transferType":"in","transferAmount":10000,"description":"Thanh toan KH3SUBDEAD1234"}'
t "B6 already-completed" -H "Authorization: Apikey $KEY" -H "$CT" -d '{"id":"SEPAY-TXN-999","transferType":"in","transferAmount":99999,"description":"Thanh toan KH3SUBDEAD1234"}'
```

## 3. Expected results (đã verify 2026-06-08)

| # | Nhánh | HTTP | Body | Logic verified |
|---|---|---|---|---|
| B5a | Sai Apikey | 401 | `{"error":"Invalid API key"}` | GAP-976 Apikey constant-time auth |
| B5b | Thiếu Apikey | 401 | `{"error":"Invalid API key"}` | GAP-976 auth required |
| B4 | `transferType=out` | 200 | `{"status":"ignored"}` | filter chỉ tiền vào |
| B2a | txnRef không khớp payment | 400 | `No payment found for txnRef: ...` | GAP-975 exact-match lookup |
| B2b | description không có txnRef | 400 | `No txnRef found in SePay description: ...` | GAP-975 regex extract |
| B1 | Happy (txnRef + amount khớp) | 200 | `{"status":"success"}` | GAP-975 PAID flip |
| B3 | Replay cùng sepayId | 200 | `{"status":"success"}` | GAP-976 idempotency-by-`transaction_id` |
| B6 | Payload mới trên payment đã COMPLETED | 200 | `{"status":"success"}` | guard `isCompleted()` |

**DB assertion sau B1:**
```bash
docker exec kite-postgres psql -U kitehub -d kitehub -c \
  "SELECT status, transaction_id, paid_at IS NOT NULL FROM payments WHERE txn_ref='KH3SUBDEAD1234';"
# → COMPLETED | SEPAY-TXN-001 | t
```
Sau B3+B6: `transaction_id` vẫn `SEPAY-TXN-001` (KHÔNG bị ghi đè bởi SEPAY-TXN-999) = no double-process.

---

## 4. Cleanup

```bash
docker exec kite-postgres psql -U kitehub -d kitehub -c "DELETE FROM payments WHERE txn_ref='KH3SUBDEAD1234';"
```

---

## 5. Còn lại (defer)

| Việc | Lý do defer |
|---|---|
| Real-money smoke qua SePay dashboard + bank thật | Cần tài khoản SePay merchant (KYC) + production HTTPS (GAP-612 AWS restore) |
| End-to-end qua SePay Test Mode simulator + tunnel | Cần SePay account + `cloudflared tunnel` — runbook §4.5; logic đã verify ở recipe này nên ưu tiên thấp |
| `applyPendingUpgrade` rollback poisoning | GAP-1062 — fix `REQUIRES_NEW` + TDD (riêng) |

---

## Related

- GAP-975 (dynamic VietQR txnRef) + GAP-976 (Apikey + idempotency) — logic verified qua recipe này
- GAP-1058 (Test Mode verify path) — recipe này = thực thi
- GAP-1061 (SecurityConfig whitelist `/api/platform/webhooks/**`) — prereq fix, surfaced bởi recipe này
- GAP-1062 (applyPendingUpgrade rollback poisoning) — surfaced bởi recipe này
- Runbook: `documents/05-guides/account-prep/sepay-account-setup-runbook.md` §4.5 (SePay Test Mode setup, cho end-to-end)
