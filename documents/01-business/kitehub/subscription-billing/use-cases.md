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

### UC-SUB-05: Gia hạn thủ công
- **Actor:** Owner (sau khi subscription expired)
- **Precondition:** Subscription tồn tại
- **Steps:**
  1. FE: hiển thị "Renew" button
  2. User: click renew
  3. System: tạo billing cycle mới, reactivate instance nếu suspended
- **Postcondition:** Instance ACTIVE, subscription ACTIVE

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
