# GAP-939: Payment.account_number rỗng dù VietQRService có default `1234567890`

**Status:** 🔵 OPEN
**Priority:** 🟡 P2
**Domain:** Backend
**Found:** 2026-06-04 (Wave flow-kh3 G1 walk verify Payment record state sau UC-SUB-02 upgrade)
**Affects:** Mọi Payment record sinh ra qua `SubscriptionService.upgradeSubscription` + `createSubscription` (sau fix UC-SUB-01) — Owner thấy QR thiếu số tài khoản, không biết chuyển tiền vào đâu.

## Problem

Walk evidence 2026-06-04 G1:

```sql
SELECT id, subscription_id, bank_code, account_number, payment_content
FROM payments
WHERE subscription_id='3ea37b40-e36d-4472-8e4e-f8cc4ac86572';
```

```
 bank_code | account_number |  payment_content
-----------+----------------+--------------------
 VCB       |                | KITECLASS 3EA37B40
```

`bank_code=VCB` được snapshot đúng default từ `VietQRService.@Value("${payment.vietqr.bank-code:VCB}")`.

`account_number` RỖNG — không lấy default `1234567890` từ `VietQRService.@Value("${payment.vietqr.account-number:1234567890}")`.

Container env verify:
```bash
docker exec kitehub-subscription printenv | grep -i vietqr
# (rỗng — không có PAYMENT_VIETQR_ACCOUNT_NUMBER)
```

→ Bug ở chỗ binding hoặc snapshot logic. 2 giả thuyết:
1. `VietQRService.accountNumber` field load default `1234567890` đúng nhưng khi snapshot vào Payment entity → null/empty (có thể logic ghi `accountNumber = paymentInfo.getAccountNumber()` nhưng `paymentInfo` không set trường này).
2. `application.yml` binding key sai (vd `payment.vietqr.account-number` vs `payment.vietqr.accountNumber` kebab vs camel).

## Root Cause

Cần state-check `VietQRService.generatePaymentInfo` (hoặc tương đương) + `PaymentService.createPaymentForUpgrade` (chỗ tạo Payment entity từ VietQR response) để xem field `accountNumber` được pipe như nào:

```bash
grep -n "accountNumber\|getAccountNumber\|setAccountNumber" kitehub/kitehub-subscription/src/main/java/com/kitehub/subscription/service/{VietQRService,PaymentService}.java
```

Giả thuyết: VietQRService dùng `accountNumber` cho `acqId(bankCode).accountNo(accountNumber).accountName(accountName)` khi gọi VietQR API, nhưng response từ VietQR API (`https://api.vietqr.io/v2/generate`) chỉ trả `qrCode` URL, không trả lại bank info. Code có thể wrap response thành DTO + lúc snapshot vào Payment chỉ copy `qrCodeUrl` + `bankCode` mà quên `accountNumber` + `accountName`.

(Verify: walk evidence cũng cho thấy `account_name` rỗng tương tự.)

## Proposed Fix

1. Đọc `VietQRService.generatePaymentInfo` (hoặc tương đương) — verify hàm có trả về object chứa cả bankCode, accountNumber, accountName.
2. Đọc nơi tạo Payment entity (`PaymentService.createPaymentForUpgrade` hoặc nơi tương đương) — verify mọi 3 trường được set trên Payment entity trước khi save.
3. Fix mapping nếu thiếu.
4. Thêm IT test: `PaymentServiceIT.create_payment_snapshots_bank_info_from_vietqr_service` assert account_number + account_name không rỗng.
5. Cân nhắc thêm DB constraint NOT NULL cho `account_number` nếu Phase 1 BETA không cho phép Payment thiếu thông tin chuyển khoản (sẽ break existing data — defer Phase 2).

## Acceptance Criteria

- [ ] Payment record mới (sau upgrade hoặc create) có `account_number` + `account_name` không rỗng, lấy từ `VietQRService` default hoặc env override.
- [ ] IT test PASS verify snapshot.
- [ ] Owner trên trang `/billing/payment/{id}` thấy đủ "Số tài khoản: 1234567890" + "Tên chủ tài khoản: CONG TY KITECLASS" (hoặc env override).

## Related

- Tiền lệ binding default: GAP-A trong session note user-flagged 2026-06-04 (default account_number=1234567890 dangerous in production — separate concern)
- Discovered via: Wave flow-kh3 G1 walk 2026-06-04
- Rule cite: `.claude/rules/local-fix-production-parity-check.md` (env binding misalignment class)
- Cross-ref: `documents/01-business/kitehub/subscription-billing/rules.md` SUB-18 Payment content uniqueness (sister concern — content có rồi, account_number bị miss)
