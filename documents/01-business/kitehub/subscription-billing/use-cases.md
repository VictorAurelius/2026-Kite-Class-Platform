# Subscription & Billing — Use Cases

### UC-SUB-01: Tạo Subscription mới
- **Actor:** Owner (sau khi trial expired hoặc upgrade)
- **Precondition:** Instance tồn tại, không có active subscription, tier != FREE
- **Steps:**
  1. FE: hiển thị pricing page với các tier (BASIC/PREMIUM/ENTERPRISE)
  2. User: chọn tier và billing cycle (MONTHLY/ANNUALLY)
  3. System: validate không có active subscription trùng (SUB-14)
  4. System: reject nếu tier = FREE (SUB-01)
  5. System: tính price từ tier + billing cycle
  6. System: tạo subscription (status=ACTIVE, autoRenew=true)
  7. System: update instance status = ACTIVE
  8. System: gửi subscription-created email
- **Postcondition:** Subscription ACTIVE, instance ACTIVE
- **Errors:**
  - 409: already has active subscription
  - 400: FREE tier cannot have subscription
- **FE Behavior:** Redirect về dashboard sau thanh toán thành công

### UC-SUB-02: Upgrade Subscription
- **Actor:** Owner
- **Precondition:** Subscription status = ACTIVE
- **Steps:**
  1. FE: hiển thị upgrade options (chỉ tiers cao hơn hiện tại)
  2. User: chọn tier mới
  3. System: validate newTier ordinal > currentTier ordinal (SUB-06)
  4. System: tính prorated charge (SUB-10)
  5. System: update tier ngay lập tức (SUB-07)
  6. System: tạo PENDING payment record (VietQR)
- **Postcondition:** Tier đã được nâng cấp ngay, có pending payment
- **Errors:**
  - 400: cannot upgrade to same or lower tier
  - 404: subscription not found

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
