# Subscription & Billing — Use Cases

### UC-SUB-01: Tạo Subscription mới
- **Actor:** Owner (sau khi trial expired hoặc lần đầu chọn gói)
- **Precondition:** Instance tồn tại, không có active subscription, tier != FREE
- **Steps:**
  1. FE: hiển thị pricing page với các tier (BASIC/PREMIUM/ENTERPRISE)
  2. User: chọn tier và billing cycle (MONTHLY/ANNUALLY)
  3. FE: POST `/api/platform/subscriptions` với payload `{instanceId, tier, billingCycle, autoRenew}`
  4. System: validate không có active subscription trùng (SUB-14)
  5. System: reject nếu tier = FREE (SUB-01)
  6. System: tính price từ tier + billing cycle
  7. System: tạo subscription với `status=PENDING, tier=FREE, pendingTier=<requested>, billingCycle=<requested>, priceVnd=<calculated>, autoRenew=<requested>` (SUB-20 — KHÔNG tự ý mark ACTIVE)
  8. System: gọi VietQRService tạo `Payment PENDING` cho `priceVnd` đầy đủ với method `VIETQR`, nội dung chuyển khoản unique (SUB-11, SUB-18); gán `pendingPaymentId`
  9. API: trả `SubscriptionResponse` với `status=PENDING, tier=FREE, pendingTier, pendingPaymentId`
  10. FE: redirect tới `/billing/payment/{pendingPaymentId}` hiển thị QR/thông tin chuyển khoản
  11. User: chuyển khoản ngoài hệ thống theo QR/thông tin ngân hàng
  12. Admin: đối soát statement ngân hàng rồi confirm payment (UC-SUB-07)
  13. System (sau admin confirm): `PaymentService.confirmPayment` → `applyPendingUpgrade(subscriptionId, paymentId)` → flip `tier=<requested>`, `status=ACTIVE`, clear `pendingTier`+`pendingPaymentId`, update instance status = ACTIVE, gửi subscription-created email
- **Postcondition (sau Bước 9):** Subscription `PENDING` với `pendingTier` + `pendingPaymentId` set; Payment `PENDING` chờ admin xử lý. Instance KHÔNG được activate trước admin confirm.
- **Postcondition (sau Bước 13):** Subscription `ACTIVE` tier `<requested>`, instance `ACTIVE`, email đã gửi.
- **Errors:**
  - 409: already has active subscription
  - 400: FREE tier cannot have subscription
- **FE Behavior:** Toast "Đã tạo đơn đăng ký gói, vui lòng thanh toán" + redirect sang payment page. Không hiển thị gói đã active cho đến khi admin confirm payment (polling thấy payment `COMPLETED` → reload subscription thấy `status=ACTIVE`).

### UC-SUB-02: Upgrade Subscription bằng chuyển khoản thủ công/VietQR
- **Actor:** Owner
- **Precondition:** Subscription status = ACTIVE, không có payment upgrade pending khác hoặc retry cùng pendingTier/payment còn hiệu lực
- **Steps:**
  1. FE: hiển thị upgrade options (chỉ tiers cao hơn hiện tại)
  2. User: chọn tier mới và submit
  3. System: validate newTier ordinal > currentTier ordinal (SUB-06)
  4. System: tính prorated charge (SUB-10)
  5. System: set `pendingTier = newTier`; **không đổi `tier` hiện tại trước khi thanh toán được confirm** (SUB-07)
  6. System: tạo hoặc reuse `Payment PENDING` với method `VIETQR`, amount VND, nội dung chuyển khoản unique (SUB-11, SUB-17, SUB-18)
  7. API: trả `SubscriptionResponse.pendingPaymentId`
  8. FE: redirect user tới `/billing/payment/{pendingPaymentId}`
  9. User: chuyển khoản ngoài hệ thống theo QR/thông tin ngân hàng
  10. Admin: đối soát statement ngân hàng rồi confirm payment (UC-SUB-07)
- **Postcondition:** Subscription vẫn giữ tier hiện tại, `pendingTier` + `pendingPaymentId` được set; payment ở trạng thái PENDING cho admin xử lý.
- **Errors:**
  - 400: cannot upgrade to same/lower tier hoặc prorated amount invalid
  - 404: subscription not found
  - 409: subscription đang có pending payment cho tier khác; FE phải điều hướng user tới payment hiện tại hoặc yêu cầu admin xử lý
- **FE Behavior:** Hiển thị toast "Đã tạo yêu cầu nâng cấp, vui lòng thanh toán" và chuyển sang payment page. Không hiển thị tier mới là active cho đến khi payment COMPLETED.

### UC-SUB-03: Downgrade Subscription
- **Actor:** Owner
- **Precondition:** Subscription status = ACTIVE
- **Steps:**
  1. FE: hiển thị downgrade options (tiers thấp hơn)
  2. User: chọn tier mới
  3. System: validate newTier ordinal < currentTier ordinal (SUB-08)
  4. System: set pendingTier = newTier (không đổi ngay)
  5. System: tier thay đổi khi chu kỳ hiện tại hết (SUB-09)
- **Postcondition:** pendingTier được set, tier hiện tại không đổi
- **FE Behavior:** Hiển thị "Will downgrade on {expiresAt}" banner

### UC-SUB-04: Hủy Subscription
- **Actor:** Owner
- **Precondition:** Subscription active
- **Steps:**
  1. FE: confirm dialog "Hủy ngay" hoặc "Hủy cuối chu kỳ"
  2. System (immediate): set expiresAt=now, autoRenew=false, status=CANCELLED (SUB-12)
  3. System (end-of-cycle): giữ expiresAt, set autoRenew=false (SUB-13)
- **Errors:**
  - 409: already CANCELLED (idempotent, no error)

### UC-SUB-05: Gia hạn thủ công (hardened)
- **Actor:** Owner (sau khi subscription expired hoặc trong grace window)
- **Precondition:** Subscription tồn tại; KHÔNG có `pendingPaymentId` treo (nếu có → owner phải hoàn tất/chờ TTL release trước — SUB-23)
- **Steps:**
  1. FE: hiển thị "Renew" button
  2. User: click renew → tạo `Payment PENDING` mới (VietQR flow, SUB-11) nếu chưa có pending hợp lệ
  3. User chuyển khoản → admin confirm (UC-SUB-07) hoặc SePay auto-confirm (UC-SUB-08)
  4. System: tạo billing cycle mới, reactivate instance nếu suspended → `instance.setStatus(ACTIVE)` clear `suspended_at` (dừng retention clock — SUB-25)
- **Postcondition:** Instance ACTIVE, subscription ACTIVE; `pendingPaymentId` cleared; retention clock reset nếu trước đó suspended.
- **Hardening note (SUB-23):** Nếu owner để pending treo quá TTL `pending-payment-ttl-days: 7` → scheduler mark `FAILED` + release `pendingPaymentId` → unblock renewal attempt mới. Reactivate instance SUSPENDED dùng UC-SUB-12 (`reactivate` endpoint, phân biệt tombstone PURGED/DELETED)

### UC-SUB-06: Tự động xử lý hết hạn (Scheduler)
- **Actor:** System (daily 9 AM + 10 AM)
- **Steps:**
  1. 9 AM: scan ACTIVE subscriptions sắp hết hạn, gửi renewal-reminder (7/3/1 ngày)
  2. 10 AM: mark ACTIVE subscriptions đã hết hạn → EXPIRED
  3. 10 AM: suspend instances nếu grace period (3 ngày) đã hết (SUB-04)

### UC-SUB-07: Admin confirm/reject payment nâng cấp
- **Actor:** Platform Admin
- **Precondition:** Có `Payment PENDING` từ UC-SUB-02; admin đã đối soát ngân hàng ngoài hệ thống
- **Steps — Confirm:**
  1. Admin mở trang pending payments
  2. System hiển thị amount, payment content, subscription/instance/owner để đối soát
  3. Admin nhập `transactionId`/mã giao dịch ngân hàng và click Confirm
  4. System mark payment `COMPLETED`, set `paidAt`, lưu `transactionId`
  5. System apply `pendingTier` vào subscription, update `priceVnd`, clear `pendingTier`/`pendingPaymentId`
  6. FE owner payment page polling thấy `COMPLETED` và redirect về billing
- **Steps — Reject:**
  1. Admin nhập lý do reject khi payment không khớp statement hoặc sai amount/content
  2. System mark payment `FAILED`
  3. System giữ tier hiện tại, clear hoặc giữ pending state theo policy xử lý lại; Phase 1 BETA ưu tiên clear để owner tạo yêu cầu mới sạch
- **Postcondition:** Confirm → subscription tier mới active. Reject → không đổi tier hiện tại.
- **Errors:**
  - 400: transactionId/reason missing
  - 404: payment not found
  - 409: payment không còn PENDING
- **FE Behavior:** Admin thấy toast rõ ràng; pending table refresh; owner payment page chuyển trạng thái qua polling.

### UC-SUB-08: Tự động xác nhận thanh toán qua SePay webhook
- **Actor:** System (SePay merchant gateway webhook)
- **Precondition:** Có `Payment PENDING` với `txnRef`; owner đã chuyển khoản; SePay gửi notification
- **Steps:**
  1. SePay POST `/api/platform/webhooks/payment` với `Authorization: Apikey` header
  2. System verify `transferType == "in"`, extract `txnRef` từ `description` (regex `KH3SUB[A-F0-9]{8}`)
  3. System `findPaymentByTxnRef(txnRef)` exact-match (KHÔNG `LIKE` — cross-tenant collision guard), verify `transferAmount == payment.amountVnd`
  4. Idempotency: nếu `payment.transaction_id == sepay.id` → HTTP 200 early-return (no double-process)
  5. System `payment.complete(sepay.id)` + `applyPendingUpgrade` (cùng state machine UC-SUB-07) → tier flip + instance activate
  6. FE WebSocket `/topic/payments/{paymentId}` nhận `paymentCompleted` event
- **Postcondition:** Subscription ACTIVE tier requested; in-app + email `payment-confirmed` notification gửi kèm biên nhận (UC-SUB-13).
- **Errors:** 401 invalid Apikey; 400 malformed/orphan/amount-mismatch (logged, không surfaced — SePay retry trên non-200 → idempotency bắt buộc)
- **Note:** Chi tiết payload + matching logic trong `api-contract.md` §POST /webhooks/payment. Đây là path auto-confirm bổ trợ cho admin-confirm thủ công (UC-SUB-07) Phase 1 BETA.

### UC-SUB-09: Owner theo dõi trạng thái "đang chờ xác nhận" + SLA admin confirm
- **Actor:** Owner (sau khi tạo create-first-paid SUB-20 hoặc upgrade SUB-07, đã chuyển khoản)
- **Precondition:** Subscription có `pendingPaymentId` trỏ tới `Payment PENDING`
- **Steps:**
  1. FE: GET `/api/platform/subscriptions/instance/{instanceId}/pending-payment-status`
  2. System trả trạng thái pending payment hiện tại: `PENDING` (chờ admin đối soát) / `COMPLETED` (đã confirm) / `FAILED` (reject hoặc quá TTL 7 ngày — SUB-23)
  3. Owner thấy banner "Đang chờ xác nhận thanh toán" + thời gian còn lại trước khi pending hết hạn (TTL 7 ngày)
  4. Khi admin confirm (UC-SUB-07) hoặc SePay auto-confirm (UC-SUB-08) → status `COMPLETED` → FE reload thấy subscription `ACTIVE`
  5. Nếu quá TTL chưa thanh toán → scheduler mark payment `FAILED` + release `pendingPaymentId` (SUB-23) → owner tạo yêu cầu mới sạch
- **Postcondition:** Owner luôn biết trạng thái đối soát; không bị treo "đăng ký không rõ tiến độ".
- **Errors:** 403 cross-tenant; 404 no active pending payment / instance not found
- **FE Behavior:** Persistent banner trạng thái pending; polling/WebSocket cập nhật khi flip COMPLETED/FAILED.

### UC-SUB-10: Downgrade với cảnh báo vượt cap + xác nhận (SUB-26)
- **Actor:** Owner
- **Precondition:** Subscription ACTIVE; chọn tier thấp hơn (SUB-08)
- **Steps:**
  1. FE: GET `/api/platform/subscriptions/instance/{instanceId}/downgrade-preview?targetTier=BASIC`
  2. System tính impact summary: usage hiện tại vs cap tier mới (students / storage / custom-domain eligibility)
  3. Nếu usage HIỆN VƯỢT cap tier mới → FE hiển thị cảnh báo cụ thể ("Bạn đang có 180 học sinh, gói BASIC giới hạn 50") + yêu cầu owner xác nhận chủ động
  4. Owner confirm → System set `pendingTier = BASIC` (áp dụng cuối chu kỳ — SUB-09); KHÔNG xóa data
  5. Cuối chu kỳ: `processRenewal` apply downgrade + `instances.tier` sync (SUB-21); excess data **soft-lock** (read-only) chứ KHÔNG xóa
- **Postcondition:** Downgrade scheduled cuối chu kỳ; owner đã thấy + chấp nhận impact; data bảo toàn (soft-lock excess).
- **Errors:** 400 `targetTier` missing hoặc không thấp hơn current; 403 cross-tenant; 404 instance not found
- **FE Behavior:** Modal impact summary + nút xác nhận; banner "Sẽ hạ gói xuống {tier} vào {expiresAt}".

### UC-SUB-11: Involuntary churn — tự động suspend khi hết grace chưa trả (SUB-24)
- **Actor:** System (scheduler `processExpiredSubscriptions`, daily 10:00)
- **Precondition:** Subscription ACTIVE hết hạn → EXPIRED; chưa thanh toán gia hạn
- **Steps:**
  1. Trong grace window (3 ngày — SUB-04): gửi dunning reminder "còn X ngày trước suspend" (SUB-23, reuse `renewal-reminder` email, dedup `alreadySentToday`)
  2. Hết grace mà vẫn chưa trả → `SubscriptionRenewalService.suspendExpiredSubscription` auto-suspend instance
  3. System WARN-log phân loại **involuntary churn** (phân biệt voluntary cancel UC-SUB-04) — cột queryable `churn_type` DEFERRED Phase 1.5
  4. `instance.setStatus(SUSPENDED)` stamp `suspended_at` → khởi động retention clock (SUB-25)
  5. System dispatch win-back notification (UC-SUB-12, `voluntary=false`)
- **Postcondition:** Instance SUSPENDED do non-payment; retention clock chạy từ `suspended_at`; owner nhận win-back CTA.
- **FE Behavior:** Owner login thấy banner "Trung tâm tạm ngưng do chưa thanh toán" + CTA kích hoạt lại.

### UC-SUB-12: Win-back / reactivate trung tâm bị suspend (GAP-1263)
- **Actor:** Owner (instance SUSPENDED do involuntary churn UC-SUB-11 hoặc voluntary cancel UC-SUB-04)
- **Precondition:** Instance status SUSPENDED (chưa qua retention window → chưa DELETED/PURGED)
- **Steps:**
  1. Owner nhận win-back email/in-app banner (`winback-reactivate`, CTA → reactivate)
  2. FE: POST `/api/platform/subscriptions/instance/{instanceId}/reactivate`
  3. System validate: SUSPENDED → reactivatable; PURGED (data đã xóa) → 409 tạo mới; DELETED (fraud-block / admin tombstone) → 409 liên hệ support; PENDING (chưa từng activate) → 409
  4. Reactivate → tạo billing cycle mới (thanh toán theo VietQR flow); `instance.setStatus(ACTIVE)` clear `suspended_at` (dừng retention clock)
- **Postcondition:** Instance ACTIVE trở lại; retention clock reset; phân biệt rõ tombstone (PURGED/DELETED fraud-block) vs reactivatable (SUSPENDED voluntary/involuntary).
- **Errors:** 403 cross-tenant; 404 instance not found; 409 tombstone (PURGED/DELETED) HOẶC never-activated (PENDING)
- **FE Behavior:** Idempotency của reactivation xử lý ở `OwnerBillingService.reactivate`; toast kết quả + redirect billing.

### UC-SUB-13: Owner xem biên nhận thanh toán (non-VAT, GAP-1266)
- **Actor:** Owner | Staff
- **Precondition:** Payment đã `COMPLETED`
- **Steps:**
  1. FE: GET `/api/platform/payments/{id}/receipt`
  2. System derive on-demand từ payment row: `receiptNumber = BN-<year>-<8 hex uppercase của payment id>` (deterministic, không lưu riêng)
  3. Trả biên nhận: tier, billing cycle, amount VND, transactionId, paidAt, note "Đây là biên nhận thanh toán (không phải hóa đơn GTGT)"
- **Postcondition:** Owner có biên nhận; cũng được email kèm khi payment confirm (template `payment-confirmed`).
- **Errors:** 400 payment chưa COMPLETED; 404 payment not found
- **Note:** Phase 1 BETA chỉ **biên nhận non-VAT** — hóa đơn GTGT (e-invoice) deferred MISA MeInvoice partnership (GAP-185/634).
