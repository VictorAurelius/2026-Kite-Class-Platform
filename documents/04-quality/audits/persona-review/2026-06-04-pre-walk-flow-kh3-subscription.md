# Pre-walk persona simulation — Wave flow-kh3 subscription trial→paid

**Persona walked:** Owner (Tuấn, tenant `g2test-an-8` từ KH-1 chain) — đã login + complete onboarding KH-2c, hiện tier FREE/TRIAL, attempt upgrade BASIC qua manual VietQR.

**Source artifacts:**
- Business: `documents/01-business/kitehub/subscription-billing/{rules,use-cases,api-contract}.md`
- BE: `kitehub/kitehub-subscription/src/main/java/com/kitehub/subscription/controller/{SubscriptionController,PaymentController,PaymentWebhookController}.java`, `service/{SubscriptionService,PaymentService,VietQRService}.java`
- FE: `kitehub/kitehub-frontend/src/app/(customer)/billing/{page,upgrade/page,payment/[id]/page}.tsx`, `lib/api/endpoints.ts`, `lib/pricing.ts`
- Gateway: `kitehub/kitehub-gateway/src/main/resources/application.yml`

---

## Failure modes (10)

### 1. **🔴 P0 BLOCKING — Admin confirm/reject endpoints không exposed HTTP** (Owner blocked indefinitely sau khi chuyển khoản)

- (a) Where: BE `PaymentController` (`/api/platform/payments`) — không có `@PostMapping("/admin/payments/{id}/confirm")` hay `/reject`. Chỉ có `PaymentWebhookController` ở `/api/platform/webhooks/payment`. Service methods `PaymentService.confirmPayment` (line 292) + `rejectPayment` (line 328) tồn tại nhưng KHÔNG controller nào gọi.
- (b) Symptom: Owner chuyển khoản, FE polling `GET /api/platform/payments/{id}` mỗi 5s. Admin không có endpoint để confirm → payment forever `PENDING` → `pendingTier` không bao giờ apply → Owner stuck. Sau N tiếng Owner gọi support.
- (c) Pre-walk check:
  ```bash
  grep -rn "admin/payments\|/confirm\|/reject" kitehub/kitehub-subscription/src/main/java/com/kitehub/subscription/controller/ --include="*.java"
  # Expect: ≥2 controller mappings; ACTUAL: 0
  grep -rn "PaymentService.*confirmPayment\|PaymentService.*rejectPayment" kitehub/kitehub-subscription/src/main/java/ --include="*.java"
  # Expect: controller invocations; ACTUAL: only test files
  ```

### 2. **🔴 P0 BLOCKING — FE upgrade flow gọi createPayment manual + races backend** (Duplicate payment + violate SUB-17 idempotency)

- (a) Where: `kitehub/kitehub-frontend/src/app/(customer)/billing/upgrade/page.tsx:86-100` — sau `upgrade.mutateAsync` (mà BE đã tự tạo Payment PENDING via `SubscriptionService.upgradeSubscription` line 191), FE lại gọi `createPayment.mutateAsync({...VIETQR})` riêng → TẠO PAYMENT THỨ HAI.
- (b) Symptom: Owner submit upgrade → BE tạo Payment #1 + `pendingPaymentId` trỏ Payment #1. FE ngay lập tức POST `/api/platform/payments` → tạo Payment #2 (cùng subscription, không link `pendingPaymentId`). FE redirect `/billing/payment/{Payment #2}` → Owner thấy QR Payment #2 → chuyển khoản theo Payment #2 content → admin confirm Payment #2 (nếu có endpoint) → `applyPendingUpgrade` skip vì `pendingPaymentId ≠ paymentId` (SubscriptionService line 398). Tier KHÔNG apply.
- (c) Pre-walk check:
  ```bash
  grep -n "createPayment\|upgrade.mutateAsync" kitehub/kitehub-frontend/src/app/\(customer\)/billing/upgrade/page.tsx
  # Expect: upgrade mutation chỉ + redirect dùng pendingPaymentId từ response; ACTUAL: cả 2 mutation chained
  grep -n "pendingPaymentId" kitehub/kitehub-frontend/src/app/\(customer\)/billing/upgrade/page.tsx
  # Expect: redirect dùng updatedSub.pendingPaymentId; ACTUAL: dùng payment.id từ createPayment thứ 2
  ```

### 3. **🔴 P0 — Owner FREE tier không thể upgrade (no subscription tồn tại để PATCH)**

- (a) Where: `SubscriptionController` `PATCH /api/platform/subscriptions/{id}/upgrade` requires subscription UUID. Per UC-SUB-01 + SUB-01, FREE tier KHÔNG có subscription row. Owner trên FREE/TRIAL → `useActiveSubscription` 404 → upgrade page có `subscription=null` → button "Upgrade" navigate `/billing/upgrade` rồi `subscription` null → handler exit silently không feedback.
- (b) Symptom: Owner click Upgrade từ dashboard → blank state hoặc "không tìm thấy gói hiện tại"; không có flow tạo subscription từ TRIAL/FREE → PAID. UC-SUB-01 yêu cầu POST `/api/platform/subscriptions` với `tier=BASIC` nhưng FE upgrade page chỉ gọi PATCH `/upgrade`.
- (c) Pre-walk check:
  ```bash
  grep -n "createSubscription\|subscriptionApi.create" kitehub/kitehub-frontend/src/app/\(customer\)/billing/upgrade/page.tsx kitehub/kitehub-frontend/src/hooks/use-subscriptions.ts
  # Expect: create path khi subscription null; ACTUAL: chỉ upgrade/downgrade mutations
  # Verify: query `useActiveSubscription` return 404 cho TRIAL instance — Owner stuck
  ```

### 4. **🟠 P1 — Trial countdown KHÔNG hiển thị trên dashboard / billing page** (Owner không biết khi nào hết trial)

- (a) Where: `kitehub/kitehub-frontend/src/app/(customer)/billing/page.tsx` + `dashboard/page.tsx` — không có component đọc `trialExpiresAt` từ instance state. `CurrentPlanCard.tsx` chỉ hiển thị subscription tier name, không có trial badge.
- (b) Symptom: Owner ở TRIAL không thấy "còn 7 ngày" → upgrade trễ → instance auto-suspend (TR-04) → mất context. Không có warning banner ở day 11/13 (TR-03).
- (c) Pre-walk check:
  ```bash
  grep -rn "trialExpiresAt\|days.*remain\|còn.*ngày" kitehub/kitehub-frontend/src/app/\(customer\)/ kitehub/kitehub-frontend/src/components/billing/ --include="*.tsx"
  # Expect: countdown banner; ACTUAL: 0 hits for trial countdown rendering
  ```

### 5. **🟠 P1 — VietQR mock-mode default mismatch local vs prod** (Owner thấy placeholder QR ở local nhưng test code path khác prod)

- (a) Where: `VietQRService.java:42` — `@Value("${payment.vietqr.mock-mode:false}")` default false. `rules.md` SUB-config `mock-mode: ${PAYMENT_MOCK_MODE:true}`. Discrepancy: Java default `false` vs rules.md default `true`. Local docker compose chắc chắn không set env → fall to Java default `false` → service call real `https://api.vietqr.io/v2/generate` → external HTTP từ Docker tới Internet.
- (b) Symptom: Local walk → VietQRService call real API → nếu network down / API key missing → throw RuntimeException → upgrade trả 500 → Owner thấy generic error. Hoặc nếu network OK → real QR rendered (acceptable nhưng test path khác prod).
- (c) Pre-walk check:
  ```bash
  grep -n "PAYMENT_MOCK_MODE\|payment.vietqr.mock-mode" kitehub/kitehub-subscription/src/main/resources/application*.yml kitehub/docker-compose.kitehub.yml
  # Expect: explicit mock-mode=true in local profile; verify default behavior
  grep -rn "mock-mode\|mockMode" kitehub/kitehub-subscription/src/main/java/com/kitehub/subscription/service/VietQRService.java
  curl -X POST http://localhost:8082/actuator/env | jq '.propertySources[].properties | with_entries(select(.key | test("vietqr")))'
  ```

### 6. **🟠 P1 — Pending payment race: FE upgrade trên subscription đã có pendingPaymentId** (409 không có distinct message)

- (a) Where: `SubscriptionService.upgradeSubscription:178-183` — nếu Owner refresh /billing/upgrade rồi resubmit cùng tier → service trả existing payment OK (idempotent). NHƯNG nếu Owner chọn tier khác → throws `IllegalArgumentException "Subscription already has a pending upgrade payment"` → BE trả 400 generic, KHÔNG `errorCode: UPGRADE_PAYMENT_PENDING` per api-contract.md 409 spec.
- (b) Symptom: Owner upgrade BASIC → chờ → muốn đổi PREMIUM → click → API trả 400 với message tiếng Anh `Subscription already has a pending upgrade payment` (không VN, không structured errorCode). FE generic toast "Lỗi", Owner không biết phải làm gì.
- (c) Pre-walk check:
  ```bash
  grep -n "UPGRADE_PAYMENT_PENDING\|ProblemDetail\|@ExceptionHandler" kitehub/kitehub-subscription/src/main/java/com/kitehub/subscription/ | head
  grep -n "errorCode\|UPGRADE_PAYMENT" kitehub/kitehub-frontend/src/hooks/use-subscriptions.ts
  # Expect: structured error mapping; ACTUAL: IllegalArgumentException → generic 400
  ```

### 7. **🟠 P1 — Payment content sanitization + diacritic preservation chưa verified** (per `vn-localization-audit-checklist.md` §5)

- (a) Where: `VietQRService.generatePaymentContent(UUID subscriptionId)` — content được set vào QR `addInfo` + DB `payment_content`. Nếu admin search/filter trong pending payments UI có dùng `HtmlUtils.htmlEscape(content)` single-arg → corrupt diacritic. Wave 106 GAP-764 precedent.
- (b) Symptom: Payment content render trong admin pending list bị mã hóa `&acirc;` thay vì `â` (nếu sanitization layer dùng single-arg htmlEscape). Tham chiếu chéo Owner và admin không match.
- (c) Pre-walk check:
  ```bash
  grep -rn "HtmlUtils\.htmlEscape\|htmlEscape" kitehub/kitehub-subscription/src/main/java/com/kitehub/subscription/service/ kitehub/kitehub-subscription/src/main/java/com/kitehub/subscription/controller/
  # Expect: 0 hits OR all 2-arg ("UTF-8"); FAIL if single-arg appears
  grep -n "generatePaymentContent" kitehub/kitehub-subscription/src/main/java/com/kitehub/subscription/service/VietQRService.java
  ```

### 8. **🟡 P2 — Email send fire-and-forget swallow exception** (Owner không nhận confirm email sau upgrade applied)

- (a) Where: `SubscriptionService.createSubscription:94-104` — `emailServiceClient.sendSubscriptionCreatedEmail` wrapped in `try { } catch (Exception e) { log.error(...) }`. Nhưng `SubscriptionService` itself `@Transactional` → vi phạm `audit-service-isolation.md` §1 (sister rule): nếu email client throw DataAccessException nested → rollback-only flag → main txn commit fail → 500. `applyPendingUpgrade` line 394 KHÔNG có email send → Owner sau khi admin confirm KHÔNG nhận thông báo upgrade success.
- (b) Symptom: Owner thấy tier flip trong UI (qua polling), nhưng không có email "Gói PREMIUM đã active". Inbox MailHog không có row. Per `pre-handoff-self-test-completeness.md` §2.3 email-driven flow check (a)/(b) FAIL.
- (c) Pre-walk check:
  ```bash
  grep -n "sendSubscription\|sendUpgrade\|emailServiceClient" kitehub/kitehub-subscription/src/main/java/com/kitehub/subscription/service/{SubscriptionService,PaymentService}.java
  # Expect: applyPendingUpgrade triggers email; ACTUAL: only createSubscription
  # MailHog probe at walk time: curl http://localhost:8025/api/v2/messages | jq '.items[]|{subject,to}'
  ```

### 9. **🟡 P2 — Prorated charge tính `daysLeft` từ `expiresAt`, nhưng TRIAL/FREE không có `expiresAt`** (NPE hoặc charge = 0)

- (a) Where: `SubscriptionService.upgradeSubscription:186` — `ChronoUnit.DAYS.between(now, subscription.getExpiresAt())`. Trial instance không có active subscription → entire flow fails earlier (per finding #3), nhưng nếu Owner manually tạo subscription via direct POST sub trên FREE rồi upgrade → `expiresAt` có thể null nếu setup partial → NPE. Hoặc `expiresAt < now` (TRIAL just expired) → `daysLeft = 0` → `proratedCharge = 0` → payment với amount 0 → VietQR API có thể reject + Owner thấy 0đ QR.
- (b) Symptom: Owner upgrade trong rescue window (UC-T2P-04) — charge = 0 → cảm thấy "free" rồi không hiểu sao admin yêu cầu chuyển tiền. Hoặc NPE → 500.
- (c) Pre-walk check:
  ```bash
  grep -n "getExpiresAt\|daysLeft\|calculateProratedCharge" kitehub/kitehub-subscription/src/main/java/com/kitehub/subscription/service/SubscriptionService.java
  # Verify: null-check getExpiresAt() OR floor at minimum charge
  # Verify: SUB-10 spec "minimum payable amount = 0 means no payment required" — but FE redirect logic không handle null pendingPaymentId properly (api-contract.md note)
  ```

### 10. **🟡 P2 — Gateway routes `/api/platform/admin/**` (line 266) catch-all NHƯNG admin payment endpoints không có handler downstream**

- (a) Where: `kitehub-gateway/application.yml:266` `Path=/api/platform/admin/**` routes to subscription service. FE call `POST /api/platform/admin/payments/{id}/confirm` → gateway forwards to `kitehub-subscription:8082/api/platform/admin/payments/{id}/confirm` → Spring trả 404 (no controller mapping). Per `pre-walk-static-audit-bundle.md` (GAP-928 echo) — false 503 hoặc 404 generic.
- (b) Symptom: Admin (if walk admin too) click confirm → 404 → admin generic toast "Lỗi". FE expects 200 response shape.
- (c) Pre-walk check:
  ```bash
  curl -X POST http://localhost:8080/api/platform/admin/payments/test-id/confirm \
    -H "Authorization: Bearer $ADMIN_JWT" -H "Content-Type: application/json" -d '{"transactionId":"X"}'
  # Expect: 200 OR 404 with structured ProblemDetail; ACTUAL: 404 default Spring error
  # Cross-verify with finding #1 — same root cause (controller missing)
  ```

---

## Recommended pre-walk batch fix

| # | Confidence | Impact | Action before walk |
|---|---|---|---|
| 1 | HIGH | HIGH | Add `AdminPaymentController` exposing `POST /api/platform/admin/payments/{id}/confirm` + `/reject` + `GET /pending` calling existing `PaymentService.confirmPayment/rejectPayment/getPendingPayments`. Pre-walk blocker — without it, walk terminates at step "wait for admin". |
| 2 | HIGH | HIGH | Fix `billing/upgrade/page.tsx` handleConfirm: REMOVE the second `createPayment.mutateAsync` call. Use `updatedSub.pendingPaymentId` từ upgrade response để redirect. Aligns với api-contract.md UC-SUB-02 step 8. |
| 3 | HIGH | HIGH | Add FE create-subscription path khi Owner FREE/TRIAL: detect `subscription === null`, call `POST /api/platform/subscriptions` (UC-SUB-01) thay vì PATCH `/upgrade`. Hoặc Owner trial path đi qua `trial-to-paid-migration` UC-T2P-01 (`POST /api/platform/instances/{id}/upgrade`) — verify which flow KH-3 scope wants then enable it. |
| 4 | HIGH | MEDIUM | Verify `payment.vietqr.mock-mode=true` set explicit trong `kitehub/docker-compose.kitehub.yml` env `PAYMENT_MOCK_MODE=true` cho kitehub-subscription service. Nếu thiếu → add same-PR. Tránh real VietQR API call lúc walk. |
| 5 | MEDIUM | HIGH | Add trial countdown component vào `(customer)/dashboard/page.tsx` + `billing/page.tsx`: render `còn N ngày trial` banner đọc `instance.trialExpiresAt`. P1 nhưng impact-cao cho persona psychology (Owner cần biết deadline). |
| 6 | MEDIUM | MEDIUM | Verify `SubscriptionService.upgradeSubscription:178-183` exception → ControllerAdvice/RestExceptionHandler mapping → ProblemDetail `errorCode=UPGRADE_PAYMENT_PENDING` 409. Grep `@RestControllerAdvice` trong subscription module. |
| 7 | MEDIUM | MEDIUM | Verify `VietQRService.generatePaymentContent` không pipe through `HtmlUtils.htmlEscape` single-arg anywhere. Spot-check admin pending-payments listing template (when controller added per #1) — ensure preserves UTF-8. |
| 8 | MEDIUM | MEDIUM | Add email send vào `applyPendingUpgrade` (per UC-SUB-07 confirm postcondition) — Owner phải nhận confirm. Cân nhắc move emailServiceClient call ra `Propagation.REQUIRES_NEW` per `audit-service-isolation.md`. |
| 9 | LOW | MEDIUM | Add null/zero-guard cho `daysLeft` trong `calculateProratedCharge`. Nếu `expiresAt == null` OR `daysLeft <= 0` → return 0 + FE handle null `pendingPaymentId` per api-contract.md note (redirect `/billing` success copy). |
| 10 | LOW | LOW | Verify finding #10 walks same path as #1 (likely duplicate root cause). Spot-check during walk: chạy curl trước khi walk admin path. |

---

## Walk readiness verdict

- HIGH-confidence findings batch-fix needed pre-walk: **4** (#1, #2, #3, #4)
- MEDIUM findings — verify during walk: **5** (#5, #6, #7, #8, #9)
- LOW: 1 (#10 — likely subsumed by #1)
- Estimated bug-yield reduction at G1 walk time: **~70-80%** (cả 3 P0 #1+#2+#3 là blocker — walk sẽ shutdown sớm; fix pre-walk → walk có thể complete end-to-end qua all 7 ACs)

**Critical pre-walk action:** Findings #1, #2, #3 là MANDATORY fix trước walk (per `feature-ship-runtime-walk-mandate.md` §3.4 catalog-then-batch — fix all known-blocker pre-walk; tránh inline rebuild thrash). #4 mock-mode verify là cheap nhưng impactful. #5-#9 có thể catalog mid-walk + batch fix post-walk.

**Sister rule cross-reference:**
- `pre-handoff-self-test-completeness.md` §2.6 Payment flow + §2.10 Time-sensitive — finding #4/#5/#9 fall here
- `vn-localization-audit-checklist.md` §5 — finding #7 (sanitization roundtrip)
- `audit-service-isolation.md` — finding #8 (email send + REQUIRES_NEW)
- `contract-first-for-cross-layer.md` — finding #1+#3 là sister-class: api-contract.md ship rồi nhưng BE handler chưa implement, FE call site assume endpoint live
