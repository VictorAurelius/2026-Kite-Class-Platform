# Pre-Walk Persona Simulation — KH-5 Subscription downgrade / cancel / renew

**Date:** 2026-06-06
**Flow:** KH-5 (Owner quản lý vòng đời subscription: downgrade / cancel / renew)
**Mandate:** `.claude/rules/pre-walk-persona-simulation-mandate.md`
**Mode:** Prediction-only (KHÔNG fix) — dự đoán failure modes TRƯỚC khi human/agent walk trên local Docker stack
**Persona:** Owner của 1 KiteHub instance (role `OWNER`), đã có subscription ACTIVE (KH-3 create + KH-4 upgrade đã G1-verified)

## Bối cảnh kỹ thuật (đã đọc source)

- 3 endpoints: `PATCH /{id}/downgrade`, `DELETE /{id}`, `POST /{id}/renew` trong `SubscriptionController.java` (line 130/147/164), tất cả `@PreAuthorize("hasAnyRole('OWNER','PLATFORM_ADMIN','ADMIN')")`.
- `SubscriptionService.downgradeSubscription` (line 224), `cancelSubscription` (line 257); `SubscriptionRenewalService.manualRenewal` (line 105) + `processRenewal` (line 51, auto-renew cron).
- Gateway `JwtAuthenticationGatewayFilter` đọc claim `role` (single) → inject `X-User-Roles`; subscription `XUserRolesHeaderFilter` map → `ROLE_<role>`.
- Status enum: ACTIVE / CANCELLED / EXPIRED / SUSPENDED / PENDING (V62 thêm PENDING + drop NOT NULL trên started_at/expires_at).
- `@Version` optimistic lock thêm V59. Entity `Subscription` vẫn khai báo `@Column(nullable=false)` cho started_at/expires_at (line 61/67) — drift với V62.
- `GlobalExceptionHandler`: `IllegalArgumentException` → 400; `AuthorizationDeniedException` → 403; `EntityNotFoundException` → 404; catch-all `Exception` → 500.

---

## Ranked failure modes (cao → thấp yield)

### FM-1 (P0, A01 Broken Access Control / IDOR) — không có ownership check
- **(a) Where:** `SubscriptionController` cả 3 endpoint (line 130/147/164) + `SubscriptionService.downgrade/cancel` + `SubscriptionRenewalService.manualRenewal`. Service methods chỉ nhận `UUID id`, KHÔNG đọc `X-User-Id` để verify subscription.instanceId thuộc owner gọi.
- **(b) Symptom:** Owner A (role OWNER) `DELETE /api/platform/subscriptions/{B's id}` → HTTP 204, cancel subscription của Owner B. Tương tự downgrade/renew/GET cross-tenant. `@PreAuthorize` chỉ chặn ROLE, không chặn cross-instance.
- **(c) Pre-walk check:** `grep -n "X-User-Id\|getCurrentTenant\|instanceId.*equals\|ownership\|@PreAuthorize" SubscriptionController.java SubscriptionService.java` → xác nhận KHÔNG có ownership binding. Walk: tạo 2 instance, lấy 2 subscription id, dùng JWT của instance A gọi `DELETE {B id}`.

### FM-2 (P0, NPE → 500) — renew một subscription PENDING (expiresAt null)
- **(a) Where:** `SubscriptionRenewalService.manualRenewal` line 116: `subscription.getExpiresAt().plusMonths(1)`.
- **(b) Symptom:** Sub mới tạo (KH-3 create-flow) ở status PENDING có `expiresAt = null` (V62 cho phép null). `POST /{id}/renew` → NullPointerException → catch-all → **HTTP 500 "An unexpected error occurred"** thay vì 400/409. `manualRenewal` chỉ chặn CANCELLED (line 111), KHÔNG chặn PENDING.
- **(c) Pre-walk check:** Đọc `manualRenewal` line 111-116 — xác nhận chỉ guard CANCELLED + deref `getExpiresAt()` không null-check. Walk: tạo sub mới (PENDING, chưa confirm payment) → renew.

### FM-3 (P0, side-effect integrity / revenue) — manual renew KHÔNG tạo payment, miễn phí
- **(a) Where:** `manualRenewal` line 104-132 — extend expiry + set ACTIVE + reactivate SUSPENDED instance, KHÔNG có `createRenewalPayment` / Payment record (so với `processRenewal` line 78 có tạo payment).
- **(b) Symptom:** Owner gọi `POST /{id}/renew` → HTTP 204, subscription +1 tháng, instance SUSPENDED → ACTIVE, **không hề có payment**. Owner tự gia hạn vô hạn miễn phí; instance không trả tiền tự reactivate. Revenue leak Phase 1 paid.
- **(c) Pre-walk check:** `grep -n "createRenewalPayment\|paymentRepository.save\|Payment" SubscriptionRenewalService.java` — thấy `processRenewal` có, `manualRenewal` KHÔNG. Walk: renew → query `SELECT * FROM payments WHERE subscription_id=...` → 0 row mới.

### FM-4 (P1, side-effect integrity) — cancel KHÔNG suspend/deprovision instance
- **(a) Where:** `SubscriptionService.cancelSubscription` line 257-283 — chỉ set status CANCELLED + expiresAt + autoRenew=false; KHÔNG đụng Instance. Scheduler `suspendExpiredSubscription` chỉ chạy khi status==EXPIRED (line 163); `findExpiredSubscriptions` query target ACTIVE/EXPIRED, **loại CANCELLED**.
- **(b) Symptom:** `DELETE /{id}?immediate=true` → 204, sub CANCELLED, expiresAt=now, nhưng Instance vẫn ACTIVE mãi mãi (không path nào suspend CANCELLED sub). Owner cancel nhưng vẫn dùng được dịch vụ.
- **(c) Pre-walk check:** Đọc `cancelSubscription` — xác nhận không có `instanceRepository.save` / `instance.suspend()`. Walk: cancel immediate → query `SELECT status FROM instances WHERE id=...` → vẫn ACTIVE.

### FM-5 (P1, state corruption) — downgrade trong khi pending upgrade làm hỏng tier
- **(a) Where:** `downgradeSubscription` line 224-248 chỉ check `status==ACTIVE` (line 235), KHÔNG check `pendingPaymentId != null`. Trong lúc pending upgrade, status vẫn ACTIVE + `pendingPaymentId` set.
- **(b) Symptom:** Owner upgrade (pendingTier=PREMIUM, pendingPaymentId set) → rồi downgrade (overwrite pendingTier=BASIC, pendingPaymentId vẫn trỏ payment upgrade). Admin confirm payment upgrade → `applyPendingUpgrade` set tier = pendingTier (= BASIC). Owner trả tiền upgrade nhưng nhận tier thấp.
- **(c) Pre-walk check:** So `upgradeSubscription` line 188 (CÓ guard `pendingPaymentId != null`) vs `downgradeSubscription` (KHÔNG có guard). Walk: upgrade → downgrade → confirm payment → kiểm tier.

### FM-6 (P1, business logic) — renew bỏ qua billing cycle, luôn +1 tháng
- **(a) Where:** `manualRenewal` line 116 + `processRenewal` line 82: `expiresAt.plusMonths(1)` hardcoded, không phân biệt ANNUALLY (so với `SubscriptionService.calculateExpiryDate` line 557 có check ANNUALLY → plusYears(1)).
- **(b) Symptom:** Subscriber gói ANNUALLY renew → chỉ +1 tháng thay vì +1 năm. Sai hạn nghiêm trọng cho gói năm.
- **(c) Pre-walk check:** `grep -n "plusMonths\|plusYears\|billingCycle" SubscriptionRenewalService.java` — thấy không tham chiếu billingCycle. Walk: tạo sub ANNUALLY → renew → kiểm `expires_at` chỉ +1 tháng.

### FM-7 (P1, state-machine gap) — manual renew KHÔNG apply pending downgrade
- **(a) Where:** `manualRenewal` line 104-132 KHÔNG có block apply `pendingTier` (chỉ `processRenewal` auto-renew line 69-75 có).
- **(b) Symptom:** Owner downgrade (set pendingTier=BASIC) → rồi manual renew → renew bỏ qua pendingTier → tier giữ nguyên PREMIUM, pendingTier=BASIC vẫn treo. Downgrade âm thầm mất / không bao giờ apply nếu owner luôn manual renew.
- **(c) Pre-walk check:** `grep -n "pendingTier" SubscriptionRenewalService.java` — thấy chỉ `processRenewal` xử lý. Walk: downgrade → manual renew → query `SELECT tier, pending_tier`.

### FM-8 (P1, OptimisticLock → 500) — concurrent renew/downgrade map sai status
- **(a) Where:** `@Version` (entity line 94, V59) → `ObjectOptimisticLockingFailureException` là `DataAccessException` nhưng KHÔNG phải `DataIntegrityViolationException` → rơi vào `handleGenericException` → 500.
- **(b) Symptom:** Auto-renew cron + manual renew/downgrade đồng thời cùng sub → lần thua optimistic lock trả **HTTP 500** thay vì 409 Conflict. Ý nghĩa @Version mất (user thấy lỗi server).
- **(c) Pre-walk check:** `grep -n "OptimisticLock\|ObjectOptimisticLock\|StaleObject" GlobalExceptionHandler.java` → 0 hit (không có handler). Walk: 2 request renew song song (hoặc đọc code đủ).

### FM-9 (P2, stale-image / schema drift) — entity nullable mismatch + image cũ
- **(a) Where:** `Subscription.java` line 61/67 `@Column(nullable=false)` cho started_at/expires_at vs V62 drop NOT NULL; thêm version column V59. Nếu Docker image `kitehub-subscription` cũ hơn commit V59/V62 → schema chạy lệch.
- **(b) Symptom:** Nếu image cũ thiếu cột `version` → mọi ghi subscription fail `column "version" does not exist` → 500 (giống lớp KC-5/6/7). Nếu thiếu V62 → create PENDING fail 23502 → 409 sai nghĩa.
- **(c) Pre-walk check:** `bash scripts/check-stale-images.sh`; trong DB: `SELECT version FROM flyway_schema_history ORDER BY installed_rank DESC LIMIT 5;` xác nhận V59+V62 applied; `\d subscriptions` xác nhận có cột `version` + started_at/expires_at nullable.

### FM-10 (P2, validation/state) — downgrade về FREE tạo state mâu thuẫn + double-renew không idempotent
- **(a) Where:** `downgradeSubscription` cho phép newTier FREE (ordinal 0 < current, line 231 chỉ chặn `>=`). Khi auto-renew apply → `processRenewal` set priceVnd=0 + tạo Payment 0 VND cho FREE — trong khi `createSubscription` cấm FREE (line 83). Đồng thời `manualRenewal` không có idempotency key → double POST = +2 tháng.
- **(b) Symptom:** Downgrade về FREE: lịch hẹn tier=FREE giá 0, payment 0đ rác. Double-click renew: cộng dồn 2 chu kỳ.
- **(c) Pre-walk check:** Đọc `downgradeSubscription` line 231 (không cấm FREE) + `manualRenewal` (không có idempotency-key param/check). Walk: downgrade BASIC→FREE; bấm renew 2 lần nhanh → kiểm expires_at cộng dồn.

---

## Bonus check — authority bridge (KC-7 GAP-1003 class)
Gateway inject single `role` claim → `X-User-Roles`. Controller dùng `hasAnyRole('OWNER','PLATFORM_ADMIN','ADMIN')`. Nhưng SecurityConfig 2FA matchers tham chiếu `TENANT_OWNER`/`TENANT_STAFF`/`TENANT_USER`. **Nếu JWT của KiteHub Owner mang role claim `TENANT_OWNER` (không phải `OWNER`)** → `@PreAuthorize(OWNER_AUTHZ)` FAIL → **403** trên cả 3 endpoint KH-5.
- **Pre-walk check:** Decode JWT của Owner (jwt.io hoặc `echo <token> | cut -d. -f2 | base64 -d`) → xem field `role`. Nếu = `TENANT_OWNER` mà KH-3 lại verified bằng credential PLATFORM_ADMIN → gap còn ẩn. `grep -rn "OWNER\|TENANT_OWNER" auth/role/PlatformRole.java` để xem canonical literal.

---

**Verdict:** 10 failure modes + 1 bonus. Yield cao nhất: FM-1 (IDOR), FM-2 (NPE renew PENDING), FM-3 (renew miễn phí không payment), FM-4 (cancel không suspend instance). Walker nên chạy 4 pre-walk check này trước, vì khả năng manifest ngay ở bước đầu.
