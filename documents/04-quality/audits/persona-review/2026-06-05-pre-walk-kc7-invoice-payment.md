---
title: "Pre-Walk Persona Simulation — KC-7 Invoice → Payment Record → Reconcile"
audience: dev
created: 2026-06-05
scope: Flow Verification Campaign KC-7 pre-walk
flow: KC-7
risk_verdict: MED
---

# Pre-Walk KC-7 — Invoice → Payment record → Reconcile

Mục tiêu: liệt kê failure mode mà người walk KC-7 trên stack Docker production-equivalent (Postgres + Flyway V1..V88) **rất có khả năng** đụng, để check trước khi walk thay vì phát hiện giữa luồng.

## Tóm tắt phương pháp + state-check đã chạy

Đã đọc thực tế (không đoán): entity `Invoice` / `InvoiceItem` / `PaymentRecord` / `Payment`, enum `InvoiceStatus` / `PaymentRecordMethod`, listener `EnrollmentEventListener`, service `InvoiceServiceImpl` + `PaymentRecordServiceImpl`, controller `InvoiceController` + `PaymentRecordController`, migrations `V1` / `V69` / `V79` / `V86`.

**Kết luận schema-drift (hypothesis #1) — phần lớn ĐÃ được vá, KHÁC KC-5/KC-6:**
- `invoices.status` CHECK: V1 có 6 giá trị lowercase (`draft, pending, partially_paid, paid, overdue, cancelled`) lệch enum 7 UPPERCASE. **Nhưng V86 (dòng 16-30 + 103-108) đã DROP CHECK cũ → UPDATE backfill lowercase→UPPERCASE → ADD CHECK mới `('DRAFT','SENT','PARTIAL','PAID','OVERDUE','CANCELLED','REFUNDED')` + default `'DRAFT'`.** Khớp enum hoàn toàn → GAP-882 phần `invoices.status` thực tế ĐÃ RESOLVED trên schema V88. Đây là điểm khác KC-5/KC-6 (chưa có migration vá tại thời điểm walk).
- `invoice_items.item_type`: VARCHAR(50), **KHÔNG có CHECK constraint** (V1 dòng 559 chỉ là comment). Entity ghi UPPERCASE `InvoiceItemType.TUITION` → không vi phạm gì (no CHECK to violate). GAP-882 phần item_type benign.
- `payments` (bảng V1 legacy): entity drift NẶNG ở V1 nhưng **V79 (dòng 113-150) đã ADD COLUMN IF NOT EXISTS** đủ bộ (`payment_status`, `installment_id`, `gateway_*`, `initiated_at NOT NULL`, `transaction_id SET NOT NULL` + unique index). V86 thêm UPPERCASE CHECK cho `payments.status`. → `payments` không phải write-blocker cho KC-7 (KC-7 dùng `payment_records`, không dùng `payments`).
- `payment_records` (V69): entity ↔ DDL khớp 1-1 (instance_id, invoice_id, method CHECK 4 giá trị khớp enum, amount NUMERIC(19,2), paid_at, note, recorded_by, BaseEntity cols). Sạch.

→ **Hypothesis schema-drift write-blocking LARGELY NEGATIVE cho KC-7.** Risk dịch chuyển sang FUNCTIONAL/business-logic (đặc biệt #1 zero-out bug + #2 silent invoice missing + #3 GAP-879 dual-system reconcile).

---

## Bảng failure mode (xếp write-blocking / nghiêm trọng trước)

| # | Tiêu đề | (a) Where | (b) Symptom người walk thấy | (c) Pre-walk check (lệnh + kỳ vọng vs drift) | Severity |
|---|---|---|---|---|---|
| **1** | **`@PreUpdate` zero-out invoice khi record payment** (logic, write-corrupting) | `Invoice.java:258-283` `calculateTotals()` chạy mỗi `@PreUpdate`; gọi từ `PaymentRecordServiceImpl.java:87` `invoiceRepository.save(invoice)`. Invoice được load tại dòng 53 bằng `findById(invoiceId)` — **items KHÔNG được fetch/khởi tạo trong cùng persistence flow của save này**; `@PreUpdate` recompute `subtotal = items.stream()...` → nếu collection rỗng/lazy chưa init đúng → `subtotal=0`, `total=0`, rồi `updateStatus()` set sai trạng thái | Sau khi record 1 payment: `GET /api/v1/invoices/{id}` trả `total` về 0 (hoặc total bị tính lại sai), `status` nhảy sai (vd `PAID` dù mới trả 1 phần vì `balanceDue = 0 - amountPaid <= 0`), `balance_due` (generated column) âm | Walk 2 bước: tạo invoice (qua enrollment) → đọc `total`. Sau đó record payment 1 phần → đọc lại. `psql -c "SELECT id,subtotal,total,amount_paid,status,balance_due FROM invoices WHERE id=<id>"`. **Drift indicator:** total bị giảm/về 0 hoặc status=PAID khi mới trả 1 phần. **Expected nếu đúng:** total giữ nguyên, status=PARTIAL. Cũng grep: `PaymentRecordServiceImpl` KHÔNG re-load items trước save → khả năng cao tái hiện | 🔴 P0 — write-corrupting (data integrity) |
| **2** | **Invoice không xuất hiện sau ENROLLMENT_CREATED — listener nuốt lỗi** | `EnrollmentEventListener.java:58-77` — `@TransactionalEventListener(AFTER_COMMIT)` + `@Transactional(REQUIRES_NEW)` + `try/catch` nuốt mọi exception (dòng 71-77, không re-throw) | Người walk tạo enrollment, kỳ vọng invoice tự sinh → invoice KHÔNG có. Không lỗi HTTP (enrollment trả 201 OK), chỉ log ERROR ở server. Người walk tưởng flow hỏng nhưng không có tín hiệu | Trước walk: kiểm tra path tạo invoice không vỡ. `psql -c "SELECT id, course_id, start_date, end_date FROM classes WHERE id=<classId>"` — nếu `start_date`/`end_date` NULL → `InvoiceServiceImpl:99-100` set `periodStart/periodEnd` NULL nhưng cột `period_start/period_end NOT NULL` (V1:524-525) → INSERT 500 → bị nuốt → invoice missing. Cũng kiểm enrollment có `tuition_amount` + `discount_percent` non-null (`InvoiceServiceImpl:109,115`). **Expected:** class có start/end date + enrollment có tuition. **Drift:** bất kỳ field NULL → silent invoice-missing | 🔴 P0 — write-blocking (invoice never created), bị che giấu |
| **3** | **GAP-879 dual-system: record ghi `payment_records`, nhưng reconcile/“mark paid” không liên thông** | `PaymentRecordServiceImpl:82` ghi `payment_records` + update `invoices.amount_paid`. `InvoiceServiceImpl.markInvoiceAsPaid:330-332` set `amount_paid = total` + status PAID **độc lập**, không đọc tổng `payment_records`. 2 đường cập nhật `amount_paid` không nhất quán; `payments` (V1) là hệ thứ 3 không liên kết FK | Người walk record payment một phần qua `record-payment` → rồi gọi `mark-paid` → `amount_paid` bị GHI ĐÈ thành `total` (mất lịch sử partial); hoặc record đủ tiền nhưng status invoice vẫn không tự chuyển PAID nếu chỉ đi qua record-payment (vì bug #1 hoặc vì `updateStatus` không chạy đúng) | `grep -n "amount_paid\|amountPaid" InvoiceServiceImpl.java PaymentRecordServiceImpl.java` — xác nhận 2 nơi set `amountPaid` khác cơ chế. Walk: record-payment đủ số tiền → `GET invoice` xem status có tự lên PAID không. **Expected (mong muốn):** status PARTIAL→PAID khi tổng record = total. **Drift:** status đứng yên SENT/PARTIAL dù đã đủ tiền (reconcile không tự động) | 🟠 P1 — reconcile ambiguity (GAP-879) |
| **4** | **`amount_paid` không bị clamp — over-payment + record âm có thể lệch** | `PaymentRecordServiceImpl:85-86` cộng dồn `amountPaid += request.amount` không check vượt `total`; `RecordPaymentRequest` chỉ validate (cần verify) amount>0 | Record số tiền > total → `amount_paid > total` → `balance_due` (generated `total - amount_paid`) âm; UI hiển thị “còn nợ -500.000đ”. Record nhiều lần cùng key (no idempotency) → cộng dồn trùng | `psql -c "\d+ invoices"` xem `balance_due` generated. Walk: record amount > total → đọc `balance_due` âm. Cũng đọc `RecordPaymentRequest.java` xem có `@DecimalMin`/`@Positive` + ràng buộc trần. **Expected:** reject hoặc clamp. **Drift:** balance_due âm chấp nhận | 🟠 P1 — money math |
| **5** | **Idempotency không thực thi DB-side — double-record khi FE retry** | `PaymentRecordServiceImpl:62-68` — Idempotency-Key chỉ `log.debug`, KHÔNG check bảng `idempotency_keys` (V66) dù javadoc BR-PAYMENT-METHOD-004 nói có. Comment thừa nhận “deferred” | Người walk submit record-payment 2 lần (hoặc FE double-click) cùng Idempotency-Key → tạo 2 `payment_records` → `amount_paid` cộng đôi → invoice “thừa tiền” | Walk: POST record-payment 2 lần liên tiếp cùng header `Idempotency-Key: <uuid>` → `psql -c "SELECT count(*) FROM payment_records WHERE invoice_id=<id>"`. **Expected:** 1 row. **Drift:** 2 rows (idempotency không chặn) | 🟠 P1 — duplicate write |
| **6** | **`recordedBy` hardcode `1L` — audit trail sai + có thể vi phạm FK người dùng** | `PaymentRecordController.java:76` `Long recordedByUserId = 1L;` (placeholder GAP-526). Entity `PaymentRecord.recordedBy NOT NULL` (V69:41) | Record payment thành công nhưng cột `recorded_by` luôn = 1 bất kể ai đăng nhập → audit “ai thu tiền” vô nghĩa. Nếu walk kỳ vọng thấy tên teacher thật → sai | `grep -n "recordedByUserId = 1L\|@AuthenticationPrincipal" PaymentRecordController.java`. **Expected (mong muốn):** lấy từ principal. **Drift:** hardcode 1L (hiện trạng). Không write-block (vì 1L hợp lệ), nhưng audit sai | 🟡 P2 — audit integrity, không block |
| **7** | **Cross-tenant: read-by-id invoice ở `InvoiceController` KHÔNG check tenant** | `InvoiceController.java:45-50` `getInvoiceById` → `InvoiceServiceImpl.getInvoiceById:144-151` chỉ `findByIdAndDeletedFalse(id)` — KHÔNG so `instance_id` với `TenantContext` (khác hẳn `PaymentRecordServiceImpl:56` có check). Dựa hoàn toàn vào RLS Postgres (V58/V59) | Nếu RLS session var `app.current_tenant_id` không được set đúng ở connection của request này → tenant A đọc được invoice tenant B (IDOR). KC security-1 wave từng bắt class lỗi này | Walk 2 tenant: login tenant A, `GET /api/v1/invoices/{id-của-B}`. **Expected:** 404/403. **Drift:** 200 + data tenant B. Cũng `grep -n "instanceId\|TenantContext\|current_tenant" InvoiceServiceImpl.java` → xác nhận read path KHÔNG có app-layer tenant guard (chỉ dựa RLS) | 🟠 P1 — cross-tenant leak (nếu RLS gap) |
| **8** | **Authz: `InvoiceController` thiếu `@PreAuthorize` toàn bộ** | `InvoiceController.java` — KHÔNG có `@PreAuthorize` trên bất kỳ method nào (kể cả `mark-paid:167`, `cancel:180`, `adjustments:89`). Trái ngược `PaymentRecordController` có `@PreAuthorize` đầy đủ | Bất kỳ role nào (kể cả STUDENT/PARENT nếu lọt gateway) gọi được `mark-paid` / `cancel` / `apply-adjustment` → thao túng tài chính. Người walk với role thấp vẫn mark-paid được | `grep -n "@PreAuthorize" InvoiceController.java` → **Expected:** mỗi mutation có authz. **Drift (hiện trạng):** 0 hits. Walk: login role TEACHER (hoặc thấp hơn) → POST `mark-paid` → xem có chặn không | 🟠 P1 — broken access control (OWASP A01) |
| **9** | **`InvoiceController` mutation endpoint không validate input đúng / status guard mỏng** | `markInvoiceAsPaid` set `amount_paid = total` bỏ qua `payment_records` thực; `applyAdjustment` cho phép chỉnh khi không PAID/CANCELLED nhưng `@PreUpdate` lại re-tính status → tương tác với bug #1 | Apply adjustment → `@PreUpdate calculateTotals` chạy → cùng nguy cơ zero-out items như #1 (apply-adjustment có add adjustment vào collection nhưng items collection có được load không?). Late-fee tương tự | Walk: tạo invoice → `POST /{id}/adjustments` (giảm giá) → đọc lại total/subtotal. **Expected:** subtotal giữ nguyên, total = subtotal + adjustment. **Drift:** subtotal về 0 (items mất khi re-persist) | 🟡 P2 — phụ thuộc #1 |

---

## Walk order recommendation (de-risk nhanh nhất)

1. **Trước tiên (de-risk #2 — silent invoice missing):** `psql` kiểm `classes.start_date/end_date NOT NULL` + `enrollments.tuition_amount/discount_percent NOT NULL` cho data fixture sẽ walk. Đây là điều kiện invoice sinh được; nếu fail thì cả luồng KC-7 đứng ngay bước 1 mà KHÔNG có lỗi HTTP (listener nuốt). Tail server log `Failed to create invoice for enrollment` trong khi walk.
2. **Bước 1 walk:** tạo enrollment → đọc DB xác nhận có invoice row + `total` đúng (`SELECT id,total,status FROM invoices WHERE enrollment_id=<id>`). Trước khi vui mừng — đây mới là half flow.
3. **Bước 2 walk (de-risk #1 — bug nghiêm trọng nhất):** record payment 1 phần qua `POST /{invoiceId}/record-payment` → ĐỌC LẠI `SELECT subtotal,total,amount_paid,status,balance_due`. Nếu `total` đổi/về 0 hoặc status nhảy PAID → trúng bug #1 P0, dừng + file gap ngay.
4. **Bước 3 (reconcile — #3):** record thêm cho đủ tiền → xem status có tự lên PAID không (mong muốn) vs phải gọi `mark-paid` thủ công (hiện trạng GAP-879).
5. **Sad-path nhanh:** record amount > total (#4), record 2 lần cùng Idempotency-Key (#5), login role thấp gọi mark-paid (#8), cross-tenant GET invoice (#7).

## Verdict tổng thể: **MED**

Lý do: schema-drift write-blocking (rủi ro #1 của KC-5/KC-6) phần lớn ĐÃ được vá bởi V79 + V86 — đây là khác biệt quan trọng, KC-7 KHÔNG ở rủi ro schema cao như KC-5/KC-6 tại thời điểm walk. **Nhưng** risk dịch sang functional: bug #1 (`@PreUpdate` zero-out khi record payment) là P0 write-corrupting rất dễ trúng, bug #2 (silent invoice-missing do listener nuốt lỗi + cột NOT NULL) cũng P0 nhưng bị che giấu, cộng GAP-879 reconcile ambiguity (#3) + authz hổng `InvoiceController` (#8). Tổng: 2× P0 + 4× P1 → MED-cao. Walk phải đọc DB sau mỗi mutation (không tin HTTP 201) vì nhiều bug im lặng.

---

## G1 Walk results (2026-06-05, production-equivalent stack — kiteclass_shared DB, Flyway V88)

Walk thực hiện trên stack Docker healthy (kiteclass-core @ :8088, gateway header contract). Tenant sky `0edaee10-2d13-44be-9151-12b78b7c5fd4`, invoice 28 (enrollment 32, student 4, 1.5M, SENT, amount_paid 0, 1 invoice_item=1.5M).

### 🔴 P0 BLOCKER (NEW — pre-walk agent MISSED): `hasRole`/`hasAnyRole` @PreAuthorize dead-deny toàn bộ core

**Triệu chứng (empirical):**
- `GET /api/v1/invoices/28` (InvoiceController, KHÔNG có @PreAuthorize) → **HTTP 200** ✓
- `POST /api/v1/invoices/28/record-payment` (`@PreAuthorize("hasAnyRole('TEACHER','ADMIN','OWNER','PLATFORM_ADMIN')")`) → **HTTP 403 ACCESS_DENIED** với MỌI format header `X-User-Roles` (OWNER / ROLE_OWNER / TEACHER / ROLE_TEACHER / "OWNER,ADMIN").

**Root cause:** kiteclass-core KHÔNG có filter nào convert gateway header `X-User-Roles` → Spring Security `GrantedAuthority`. `SecurityConfig` = `.anyRequest().permitAll()` + `@EnableMethodSecurity`, nhưng `SecurityContextHolder` luôn rỗng (grep toàn core main: 0 site dựng `Authentication`/`GrantedAuthority`/`setAuthentication`). Gateway (`JwtAuthenticationGatewayFilter`) chỉ forward header `X-User-Roles=<role>`; core không đọc nó cho Spring auth. → mọi `hasRole`/`hasAnyRole` deny.

**Blast radius:** 24 `hasRole`/`hasAnyRole` @PreAuthorize trên 10 controller dead-deny: payment-record (record-payment + list-payments), marketing (LandingPage/Lead/ContactMessage), document-gen, report, payroll, settings/BrandingVersion, parent/ParentConsentAdmin. **PLUS** `AuthorizationBean.isAdmin()` (đọc `SecurityContextHolder` authorities) cũng dead → trên `@authz.hasAccessToX` (KC-5 attendance/KC-6 grade) đường admin/owner override gãy thầm lặng; chỉ teacher-ownership DB path chạy (lý do KC-5/KC-6 PASS với persona TEACHER).

**Tại sao IT mù:** `SecurityConfig` `@Profile("!test")`; test dùng `TestSecurityConfig` + `@WithMockUser` (set authorities) → @PreAuthorize PASS trong IT, 403 trên production gateway-headers. Cùng class với bài học KC-5/KC-6 (IT mù schema-drift) — lần này IT mù auth-context-drift.

**Đề xuất fix (1 filter unblock cả 24 endpoint + isAdmin):** thêm `OncePerRequestFilter` trong core đọc `X-User-Roles` → set `PreAuthenticatedAuthenticationToken` với authorities `ROLE_<role>` vào `SecurityContextHolder` (sau gateway, trước method-security). Map role thô (OWNER) → `ROLE_OWNER`. Đồng bộ với `TenantFilterInterceptor` ordering.

**KC-7 G1 verdict:** ❌ BLOCKED tại bước 2 (record payment). Functional bug #1 (@PreUpdate zero-out) + #3 (reconcile dual-system) CHƯA walk được qua API cho đến khi auth bridge fix + rebuild. Lưu ý từ pre-walk DB check: invoice 28 có invoice_item (1.5M) → @PreUpdate recompute subtotal từ items lazy-load trong txn → **bug #1 zero-out nhiều khả năng KHÔNG manifest** cho invoice có items (cần confirm sau khi unblock auth).

### Trạng thái các pre-walk hypothesis sau G1
| # | Pre-walk risk | G1 status |
|---|---|---|
| NEW | hasRole @PreAuthorize dead-deny (24 endpoints) | 🔴 CONFIRMED P0 — blocker |
| 1 | @PreUpdate zero-out | ⏸️ chưa walk được (auth blocked); DB cho thấy items tồn tại → likely không manifest |
| 2 | silent invoice-missing (NULL class dates) | ✅ không trigger — class dates non-NULL; invoice pipeline ENROLLMENT_CREATED→invoice hoạt động (13 invoices linked enrollment) |
| 3 | GAP-879 dual-system reconcile | ⏸️ chưa walk được (auth blocked) |
| 7 | cross-tenant read-by-id InvoiceController | ⏸️ cần 2-tenant test (auth-independent vì InvoiceController không @PreAuthorize) |
| 8 | InvoiceController thiếu @PreAuthorize | ✅ CONFIRMED — GET invoice 28 trả 200 không cần role (đáng lẽ phải gate) |

### G1 Walk POST-FIX (sau khi land GAP-1003 auth bridge + rebuild kiteclass-core)

| Bước | Kết quả |
|---|---|
| record-payment OWNER 500k (invoice 28) | ✅ 201; status SENT→PARTIAL; total giữ 1.5M (bug #1 zero-out KHÔNG manifest — invoice có items) |
| record-payment +1M (invoice 28) | ✅ 201; status PARTIAL→PAID; balance_due 0 (bug #3 reconcile: record-payment path auto-transition đúng) |
| GET invoice 28 (no role) | ⚠️ 200 — bug #8 InvoiceController thiếu @PreAuthorize → GAP-1005 |
| over-payment 4M trên 3.5M (invoice 15) | ⚠️ 201; balance_due -500,000 (no clamp) → GAP-1004 |
| idempotency: 2× same key (invoice 14) | ⚠️ 2 payment_records → GAP-1004 |
| cross-tenant GET invoice tenant khác | ✅ 404 (RLS giữ tenant scope — bug #7 KHÔNG manifest) |
| recordedBy | ⚠️ hardcode 1L → tracked GAP-526 (existing) |

**KC-7 G1 verdict: PASS** cho core flow invoice→payment→reconcile (sau fix GAP-1003 P0). Findings filed: GAP-1003 (DONE — auth bridge fix), GAP-1004 (P1 over-payment+idempotency), GAP-1005 (P1 InvoiceController authz). Schema-drift (giả thuyết #1 KC-5/KC-6) confirmed RESOLVED cho KC-7 (V79+V86+V88).

**Walk fixtures consumed (sky tenant dev DB):** invoice 28 PAID; invoice 15 over-paid (-500k); invoice 14 có 2 payment_records. G2 human re-walk dùng invoice khác (9-13) hoặc tạo enrollment mới.

---

## G3 Production-parity verification (2026-06-05)

Per `local-fix-production-parity-check.md` + campaign §1 G3 definition. G1 walk dùng core-direct (port 8088) với hand-crafted `X-User-Roles` header. G3 verify **chuỗi auth thật qua gateway** (port 9000) chỉ bằng minted JWT — KHÔNG header thủ công.

**Walk:** mint HS256 JWT (`sub` + `role=OWNER` + `email` + `tenantId=<sky>` + `exp`) ký bằng gateway `JWT_SECRET` → request qua gateway :9000 chỉ với `Authorization: Bearer <JWT>`:
- `GET /api/v1/invoices/13` → **200**
- `POST /api/v1/invoices/13/record-payment` `{"method":"CASH","amount":500000}` → **201** (payment_records id=6)

→ Chứng minh chuỗi end-to-end: gateway validate JWT → forward `X-User-Roles=OWNER` + `X-User-Id` + `X-Tenant-Id` (resolve từ `tenantId` claim, TenantResolver fallback localhost) → core `GatewayHeaderAuthenticationFilter` dựng `ROLE_OWNER` → `@PreAuthorize hasAnyRole` PASS. Gateway thật sản xuất đúng header mà filter tiêu thụ.

**Parity matrix:**

| Dimension | Verdict |
|---|---|
| Same image tag (`kiteclass-core:latest` có fix, trên main) | ✅ |
| Real Postgres + Flyway V88 + RLS (không H2) | ✅ (cross-tenant 404) |
| Gateway JWT→header auth | ✅ verified end-to-end (this walk) |
| prod-profile config (`SecurityConfig @Profile("!test")` + filter active prod, no profile dep) | ✅ |
| env-vars (filter zero env dep; `JWT_SECRET` đã có ở prod) | ✅ |

**G3 verdict: PASS.** Lưu ý deploy-time: fix ở `main` — production cần rebuild kiteclass-core image từ main (ECR) khi deploy AWS stack (deploy step, không phải code gap). Walk fixture consumed: invoice 13 có 1 payment 500k.
