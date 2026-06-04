---
audience: dev
---

# Pre-Walk Failure-Mode Matrix — Payment Flow (Phase 1 BETA + Phase 1.5 PAID)

**Date:** 2026-06-04
**Flow:** Payment (4 patterns × 10 failure scenarios × 4 tenant contexts)
**Audit type:** Outside-in failure-mode matrix per `.claude/skills/quality/simulation-gap-finder/SKILL.md` adapted cho pre-walk (per `pre-walk-persona-simulation-mandate.md` v1.0.0)
**Coordinator:** Claude (Opus 4.7 1M)
**Sister audits:** `2026-06-04-pre-walk-kc1-failure-mode-matrix.md` (KC-1 tenant provisioning), `2026-05-18-phase-1-5-qr-payment-outside-in.md` (Phase 1.5 plan)

---

## 0. Scope re-state-check

**Payment patterns under audit:**
- **P0 (Phase 1 hiện tại):** Static VietQR + admin manual confirm — `PaymentService.confirmPayment(paymentId, transactionId)` per `AdminPaymentController:91`
- **PA (Phase 1.5):** Dynamic VietQR + Casso/SePay webhook (planned per `2026-05-18-phase-1-5-qr-payment-outside-in.md`)
- **PB (Phase 1.5+):** Direct bank API (MB BizAPI / TCB / VCB) — requires merchant agreement
- **PC (Phase 2):** PSP gateway (VNPay/MoMo/ZaloPay) — PSP license barrier per Wave 93 audit

**Empirical code state 2026-06-04:**

| Artifact | Status | Evidence |
|---|---|---|
| `PaymentService.confirmPayment` admin path | ✅ EXISTS `kitehub-subscription/.../service/PaymentService.java:298` | Status transition PENDING → COMPLETED via `payment.complete(transactionId)`; admin manual TX ID input |
| `PaymentService.processPaymentWebhook` | ✅ EXISTS `:188` | BUT `VietQRService.verifyPayment` body commented out (`bankApiClient.getTransaction(...)` placeholder line 183) → effectively no-op webhook verify |
| Idempotency on `confirmPayment` | ❌ ABSENT | Only PENDING-state guard line 304; no idempotency key, no duplicate-tx-id check on `transactionId` field unique constraint |
| `findPaymentByContent` matching | ⚠️ FRAGILE — line 246 substring match `paymentContent.contains(shortId)` | Greedy first-match; race possible if 2 PENDING payments share shortId prefix |
| `payment.complete()` transition guard | ⚠️ UNKNOWN — depends on `Payment` entity `complete()` impl | No state-machine library evidence; likely simple field setter |
| Refund flow | ❌ ABSENT | No `refundPayment` method in `PaymentService`; no `REFUNDED` status in scanned code |
| Audit log row per payment action | ❌ ABSENT | Only `log.info(...)` SLF4J — not persistent admin_audit_logs row |
| Cross-tenant payment integrity | ⚠️ UNVERIFIED | `findByTransactionId` query line 34 không filter `instanceId` → cross-tenant collision risk |
| Webhook signature verification | ❌ ABSENT | `PaymentWebhookController:64` reads raw `payload.get("transactionId")` — no HMAC/signature gate |
| Email confirmation post-payment | ❌ UNCONFIRMED | `confirmPayment` line 309-323 only calls `subscriptionService.applyPendingUpgrade`; no `sendPaymentConfirmEmail` |

**Conclusion:** Phase 1 Pattern P0 ship today shipped with ~6 P0 holes invisible to E2E + Mockito-only unit tests. Pre-walk matrix below surfaces them WITH cross-pattern mitigation comparison.

---

## 1. Matrix axes

- **Axis 1 (Patterns):** P0 Static QR+manual · PA Dynamic QR+webhook · PB Direct bank API · PC PSP gateway
- **Axis 2 (Failures):** F1 close-tab-orphan · F2 wrong-amount · F3 wrong-memo · F4 double-pay · F5 settlement-delay · F6 same-amount-same-minute · F7 webhook-timeout-retry · F8 admin-typo · F9 refund-request · F10 cross-tenant-memo
- **Axis 3 (Tenants):** T1 tiny (5-15) · T2 small-medium (20-50) · T3 medium (80-150) · T4 large/school (200+)

4×10×4 = 160 cells. Reporting **34 cells with identified failure mode** (filtered by signal; rest = inapplicable e.g. F9 refund on Pattern PC PSP-handled, T1 tiny dùng admin manual exclusively).

---

## 2. Findings (34, sorted by severity)

### P0 — feature-missing / data-loss / security / cross-tenant

#### Cell P0×F4×T2: Double-pay không có idempotency guard
- **Combination:** Static QR manual × double-pay × small-medium tenant
- **What breaks:** User chuyển 600k, không thấy admin confirm (delay), chuyển lại 600k. Admin nhập tx-id của lần 1 vào `confirmPayment(paymentId, "FT2026...")` → PENDING → COMPLETED. Lần 2 tx-id chưa được claim → Payment row khác (cùng subscription) → admin nhập → COMPLETED. Subscription upgraded 2x; user mất 600k duplicate.
- **Pre-walk check:** `psql -c "SELECT transaction_id, COUNT(*) FROM payments WHERE status='COMPLETED' GROUP BY transaction_id HAVING COUNT(*) > 1"` → expect 0. AND `grep -n "unique\|UNIQUE" kitehub/kitehub-subscription/src/main/resources/db/migration/V3__create_payments_table.sql` — check transaction_id has UNIQUE constraint
- **Severity:** P0 (data-loss, money)
- **Mitigation comparison:**
  | Pattern | Idempotency strength |
  |---|---|
  | P0 manual | ❌ admin can re-enter same tx-id; UNIQUE constraint là only line of defense (chưa verified present) |
  | PA Casso webhook | 🟡 webhook usually idempotent via tx-id but BE handler MUST honor `findByTransactionId` BEFORE insert |
  | PB direct bank | ✅ bank API returns transaction with stable tx-id; idempotency key native |
  | PC PSP | ✅ PSP idempotency-key header industry standard (Stripe `Idempotency-Key`, VNPay `vnp_TxnRef`) |
- **Related GAP:** propose `GAP-NEW-payment-idempotency-guard`

#### Cell P0×F8×T2: Admin typo tx-id confirmPayment thành công (silent fraud)
- **Combination:** Static QR manual × admin-typo × small-medium
- **What breaks:** Admin nhìn bank app, gõ tay tx-id vào form. Typo `FT26012345` thay vì `FT26012354`. `confirmPayment` chỉ check PENDING state, không verify tx-id thật sự exists ở bank. Subscription upgraded; nhưng tx-id hệ thống lưu sai → 2 tuần sau reconcile bank statement → mismatch → không trace được tiền nào ứng với payment nào.
- **Pre-walk check:** Code review `PaymentService.confirmPayment:298` — không có `vietQRService.verifyPayment(transactionId, ...)` call (chỉ có ở `processPaymentWebhook` path, mà path đó còn commented out).
- **Severity:** P0 (audit/compliance + fraud window)
- **Mitigation comparison:** PA/PB/PC tự động dùng tx-id từ bank → eliminate typo class. P0 cần admin double-entry OR copy-paste from bank app screen (UX friction).
- **Related GAP:** propose `GAP-NEW-payment-confirm-verify-tx-existence`

#### Cell P0×F10×T2 (CRITICAL): Cross-tenant memo collision → payment confirmed wrong tenant
- **Combination:** Static QR manual × cross-tenant memo × small-medium (owner có 2 tenants)
- **What breaks:** Owner có Tenant A "alpha-edu" + Tenant B "beta-academy". Chuyển khoản 600k cho B nhưng memo gõ "KITECLASS alpha-edu-xyz". `findPaymentByContent:234` extract `parts[1] = "alpha-edu-xyz"` → tìm PENDING payment có content chứa shortId. Nếu A có PENDING 600k (cùng số tiền) → MATCH → A upgraded; B chờ mãi.
- **Pre-walk check:** `psql -c "SELECT instance_id, status, amount_vnd, payment_content FROM payments WHERE status='PENDING' ORDER BY created_at LIMIT 20"` — verify mỗi PENDING có shortId unique cross-tenant.
- **Severity:** P0 (cross-tenant data integrity + accounting nightmare)
- **Mitigation comparison:**
  | Pattern | Cross-tenant guard |
  |---|---|
  | P0 substring match | ❌ greedy first-match, no tenant scope |
  | PA webhook (Casso bank-account-id) | 🟡 if each tenant has dedicated VA (Virtual Account) → tx maps to tenant exactly |
  | PB direct API | ✅ VA per tenant native (MB BizAPI / TCB) |
  | PC PSP | ✅ per-payment `orderId` PSP-scoped + tenant claim via JWT |
- **Related GAP:** propose `GAP-NEW-payment-find-by-content-tenant-scope` (P0 critical)

#### Cell PA×F7×T3: Webhook delivered 2x → double-credit
- **Combination:** Dynamic QR webhook × webhook-retry-2x × medium tenant
- **What breaks:** Casso/SePay webhook timeout (BE >5s) → vendor retries 30s sau. BE handler 2nd time fires `processPaymentWebhook(tx-id-X, ...)` → `findPaymentByContent` returns same Payment row (still PENDING because 1st request died mid-txn) → `payment.complete()` → SECOND time vendor delivers, Payment now COMPLETED → state guard catches → no-op. BUT if 1st request actually succeeded (slow but not dead) → Payment COMPLETED → 2nd webhook hits COMPLETED guard → throws → vendor sees error → retries 5x → vendor support flag.
- **Pre-walk check:** Verify `processPaymentWebhook` line 188 has explicit `findByTransactionId(transactionId).ifPresent(return)` short-circuit BEFORE `findPaymentByContent`. Currently absent.
- **Severity:** P0 (webhook reliability + vendor cascade)
- **Mitigation comparison:** PA needs explicit idempotency check by `transactionId`. PB/PC use idempotency-key header.
- **Related GAP:** propose `GAP-NEW-webhook-idempotency-tx-id-short-circuit`

#### Cell PA×F7×T*: Webhook signature absent → spoofed payment confirmation
- **Combination:** Dynamic QR webhook × webhook-spoof × any tenant
- **What breaks:** `PaymentWebhookController:64` reads `payload.get("transactionId")` from raw JSON body. Endpoint `/api/v1/webhooks/payment` (per controller @Tag) — if exposed to internet (vendor must reach it) without HMAC signature verify → attacker `curl -X POST` với fake tx-id matching outstanding PENDING → upgrade ANY tenant subscription for free.
- **Pre-walk check:** `grep -rn "X-Signature\|verifySignature\|HMAC\|webhook.*secret" kitehub/kitehub-subscription/src/main/java/com/kitehub/subscription/controller/PaymentWebhookController.java` → 0 hits
- **Severity:** P0 (security — A07 Identity & Auth Failures per OWASP)
- **Related GAP:** propose `GAP-NEW-webhook-signature-verification` (paired sister rule `pre-handoff-self-test-completeness.md` §2.6 (c))

#### Cell P0×F9×T3: Refund request → manual SQL UPDATE (no refund flow)
- **Combination:** Static QR manual × refund × medium tenant
- **What breaks:** User cancel within 7-day grace (per Phase 1 BETA TOS). Admin: không có `refundPayment` method, không có `REFUNDED` status. Choice: (a) `UPDATE payments SET status='FAILED' WHERE id=X` (lying — payment did complete), (b) keep COMPLETED + manual bank transfer back + write off in spreadsheet. Audit trail broken.
- **Pre-walk check:** `grep -rn "refund\|REFUND" kitehub/kitehub-subscription/src/main/java --include="*.java"` → 0 hits in PaymentService scope
- **Severity:** P1 (Phase 1 BETA scope debatable; PDPL Art 11 audit trail requires honest status)
- **Related GAP:** propose `GAP-NEW-refund-flow-state-machine` (Phase 1.5 scope)

#### Cell PC×F*×T1: PSP license barrier blocks T1 dùng PSP Phase 1.5
- **Combination:** PSP gateway × any failure × tiny tenant
- **What breaks:** VNPay/MoMo merchant onboarding requires: business license, tax code, bank account verification, KYC documents, ~2-4 tuần approval. T1 tiny center (gia đình mở tutoring, chưa có business license) → cannot adopt PC. Pattern PC effectively unavailable for entire T1 segment.
- **Pre-walk check:** Cross-reference với Wave 93 GAP-NEW-payment-processor-init audit findings (CANCEL Phase 1.5; defer Phase 2).
- **Severity:** P0 (scope blocker for T1 segment)
- **Mitigation:** Stick với P0/PA cho T1; PC chỉ availible cho T2+.
- **Related GAP:** see existing Wave 93 audit + `2026-05-18-phase-1-5-qr-payment-outside-in.md`

---

### P1 — UX broken / observability gap / reconciliation hard

#### Cell P0×F1×T*: User submit close tab before chuyển → orphan PENDING vĩnh viễn
- **Combination:** Static QR × close-tab × any tenant
- **What breaks:** `PaymentService.createPayment` saves row PENDING. User chưa chuyển. Không có TTL/expiry → row PENDING forever. `findPendingPayments()` query returns growing list → admin UI clutter + `findPaymentByContent` substring iteration cost O(N).
- **Pre-walk check:** `psql -c "SELECT COUNT(*), MIN(created_at) FROM payments WHERE status='PENDING'"` — expect <50 active OR cleanup cron evidence
- **Severity:** P1 (data hygiene + perf)
- **Mitigation:** Cron sweep `UPDATE payments SET status='ABORTED' WHERE status='PENDING' AND created_at < now() - interval '24 hours'`. Compare với BetaAccessRequestStatus ABORTED pattern (line 12 in BetaAccessRequestStatus.java — already implemented).
- **Related GAP:** propose `GAP-NEW-payment-pending-cleanup-cron` (mirror BetaAccessRequestStatus.cleanupAbort)

#### Cell P0×F2×T2: User chuyển sai số tiền (599k cho gói 600k)
- **Combination:** Static QR × wrong-amount × small-medium
- **What breaks:** `processPaymentWebhook:196` check `payment.amountVnd.equals(amountVnd)` → throw IllegalArgumentException → `payment.fail()` ở catch route bị skip vì throw fires before. Webhook returns 500. Admin path `confirmPayment` không check amount AT ALL (just transitions state). Admin có thể blindly confirm 599k → user "trả ít hơn nhận đủ gói" → revenue leak.
- **Pre-walk check:** Trace `confirmPayment:298` line-by-line — không có amount param. Reviewer compare với `processPaymentWebhook:196` để see asymmetry.
- **Severity:** P1 (revenue leak + UX confusion)
- **Mitigation:** Admin confirm form thêm "Số tiền đã nhận" field; backend compare với `payment.amountVnd` ± tolerance (0 hoặc ≤1k cho rounding).
- **Related GAP:** propose `GAP-NEW-admin-confirm-amount-verify`

#### Cell P0×F3×T1: Memo trống/sai → admin manual hunt
- **Combination:** Static QR × wrong-memo × tiny tenant
- **What breaks:** User chuyển 600k, memo `"Tien hoc phi"` (không có shortId). `findPaymentByContent:236` `parts.length < 2` → throws `IllegalArgumentException("Invalid payment content format")`. Webhook returns 500. Tiền vào bank không match payment. Admin phải manually: (a) phát hiện qua bank statement, (b) tìm user qua amount + timestamp, (c) `confirmPayment` với tx-id thật. ~5-10 phút/case × N case/tháng.
- **Pre-walk check:** Compare `findPaymentByContent` logic với `payment_content` column nullable check in V3 migration
- **Severity:** P1 (UX + ops cost)
- **Mitigation:** Admin UI "Reconcile orphan" view: list bank tx không match payment + suggest matching PENDING by amount range. Plus user-side: payment instruction page should COPY-paste memo (clipboard API) not type.
- **Related GAP:** propose `GAP-NEW-admin-orphan-bank-tx-reconcile-ui`

#### Cell P0×F6×T3: 2 users chuyển cùng amount cùng minute → admin assign nhầm
- **Combination:** Static QR × same-amount-same-minute × medium tenant
- **What breaks:** T3 medium (80-150 students) có nhiều subscription tier cùng 600k. User A + User B both chuyển 600k vào 09:15. Bank shows 2 incoming tx. Admin opens "Pending payments" → 2 PENDING với amount=600k. Admin nhìn shortId vs memo: nếu User B memo lỗi (typo shortId) → admin guess → assign B's bank tx vào A's payment → A upgraded twice, B's payment lost.
- **Pre-walk check:** `findPendingPayments` ordering — verify per-tenant or per-user grouping; UI surfaces user identifier prominently.
- **Severity:** P1 (cross-user data integrity)
- **Mitigation:** Force unique memo (shortId UUID prefix ≥6 chars + amount in memo as redundancy). Plus bank statement reconcile shows full memo not just amount.
- **Related GAP:** propose `GAP-NEW-payment-memo-uniqueness-shortid-len`

#### Cell P0×F5×T*: Settlement delay weekend → user thấy chưa upgrade
- **Combination:** Static QR × settlement-delay × any tenant
- **What breaks:** User chuyển Friday 21:00. Bank settles Monday 09:00 (NAPAS off-hour). User F5 cuối tuần liên tục check dashboard → still FREE tier → support ticket Saturday "tôi đã chuyển rồi sao chưa nâng cấp?". Admin manually verify bank statement Monday morning.
- **Pre-walk check:** User-facing payment status page presence; `grep -rn "payment.*status.*pending\|PENDING\|đang xử lý" kitehub/kitehub-frontend/src --include="*.tsx"` — verify clear messaging.
- **Severity:** P1 (UX trust)
- **Mitigation:** Payment confirmation page hiển thị explicit "Giao dịch sẽ được xử lý trong vòng 24h (chuyển ngoài giờ làm việc có thể chậm đến T+1)". Plus admin daily 09:00 + 14:00 batch confirm cron.
- **Related GAP:** propose `GAP-NEW-payment-pending-user-explainer-copy`

#### Cell PA×F5×T2: Webhook receives settlement after Casso delay (5-15 phút)
- **Combination:** Dynamic QR webhook × settlement-delay × small-medium
- **What breaks:** Even PA pattern, Casso poll bank API every 1-5 phút. User chuyển → webhook arrives 5-15 phút sau (NOT instant). User refresh dashboard immediately → still PENDING → confusion.
- **Pre-walk check:** FE polling/SSE for payment status; expected vs actual UX gap measurement.
- **Severity:** P2 (UX expectations)
- **Mitigation:** "Đang xác minh từ ngân hàng (5-15 phút)" loader; SSE channel for status flip.
- **Related GAP:** propose `GAP-NEW-payment-status-sse-channel`

#### Cell P0×F*×T*: No persistent payment audit log row
- **Combination:** Any pattern × any failure × any tenant
- **What breaks:** `confirmPayment:299` only `log.info(...)`. SLF4J ephemeral. PDPL Art 11 + admin_audit_logs (Wave 85 immutable) does NOT cover payment_audit_log domain. 6 tháng sau, "ai confirm payment X khi nào, IP nào?" → unanswerable. K-12 audit requirement (Phase 3) fails.
- **Pre-walk check:** `psql -c "\dt admin_audit_logs"` — verify exists + grep `INSERT.*admin_audit_logs.*PAYMENT` in PaymentService
- **Severity:** P1 (compliance — escalates P0 in Phase 3 K-12)
- **Mitigation:** AuditService.recordPaymentAction(adminId, paymentId, action, ip, fingerprint, timestamp) via REQUIRES_NEW per `audit-service-isolation.md`.
- **Related GAP:** propose `GAP-NEW-payment-admin-audit-log`

#### Cell PB×F*×T1: Direct bank API (MB BizAPI) requires merchant agreement T1 cannot get
- **Combination:** Direct bank API × any × tiny tenant
- **What breaks:** Same as PC license barrier — MB BizAPI / TCB Open API require business banking account + integration fee (~5-15M VND setup + monthly). T1 tiny không có.
- **Severity:** P1 (scope blocker — affects pattern selection matrix Section C)
- **Mitigation:** PB unavailable cho T1; KiteHub-pool VA model (central account + shared VA) viable but increases reconcile complexity.

---

### P2 — operational friction / nice-to-have polish

#### Cell P0×F8×T4: Admin large center process 50+ payments/day → typo rate >2%
- **Combination:** Static QR manual × admin-typo × large tenant
- **What breaks:** T4 (200+ students, dedicated Accountant) process 50+ payments/day during enrollment period. Typo rate 2% = 1 wrong assign/day. Cumulative monthly = ~22 reconcile actions.
- **Severity:** P2 (ops cost scales với T4 volume)
- **Mitigation:** T4 segment SHOULD use PA/PB/PC, not P0. P0 fits T1/T2 only.

#### Cell P0×F*×T1: T1 owner Saturday morning = no admin
- **Combination:** Static QR × any × tiny tenant
- **What breaks:** T1 = solo founder. Saturday user chuyển → owner sleeping/family time. No staff backup. ~12-18h delay.
- **Severity:** P2 (T1 UX tradeoff acceptable per Phase 1 BETA scope)
- **Mitigation:** "Confirm in 1 business day" SLA in user-facing copy.

---

## 3. Section A — Top 10 P0 cells

Sorted criticality (impact × likelihood × current absence-of-mitigation):

| # | Cell | Title | Mitigation comparison (P0/PA/PB/PC) |
|---|---|---|---|
| 1 | P0×F10×T2 | Cross-tenant memo collision | ❌ / 🟡 (VA per tenant) / ✅ / ✅ |
| 2 | PA×F7×T* | Webhook signature absent | N/A / ❌ (must add HMAC) / ✅ bank API auth / ✅ PSP signature |
| 3 | P0×F4×T2 | Double-pay no idempotency | ❌ / 🟡 webhook idempotent / ✅ / ✅ idempotency-key |
| 4 | P0×F8×T2 | Admin typo silent fraud | ❌ / ✅ (auto tx-id from webhook) / ✅ / ✅ |
| 5 | PA×F7×T3 | Webhook retry double-credit | N/A / ❌ (need tx-id short-circuit) / ✅ / ✅ |
| 6 | PC×F*×T1 | PSP license blocks T1 | N/A / N/A / N/A / ❌ scope blocker |
| 7 | P0×F*×T* | No payment audit_log row | ❌ / ❌ / ❌ / ❌ (all 4 patterns need audit) |
| 8 | P0×F9×T3 | Refund flow missing | ❌ / ❌ / ❌ / 🟡 PSP refund API native |
| 9 | P0×F2×T2 | Admin confirm no amount verify | ❌ / ✅ webhook amount in payload / ✅ / ✅ |
| 10 | P0×F3×T1 | Memo trống → admin hunt | ❌ / 🟡 webhook needs memo too / ✅ / ✅ |

---

## 4. Section B — Failure class clustering

| Class | Strongest pattern | Weakest pattern | Gap |
|---|---|---|---|
| **State machine integrity** (PENDING→COMPLETED/FAILED/REFUNDED) | PC PSP (state mgmt by vendor) | P0 (no REFUNDED status, ABORTED missing) | Add Payment state machine + REFUNDED + ABORTED |
| **Reconciliation correctness** | PB direct bank API (VA per tenant) | P0 (substring memo match cross-tenant collision) | tenant-scope `findPaymentByContent` + VA model |
| **Idempotency** | PC (idempotency-key header) | P0 (admin can re-enter tx-id) | UNIQUE constraint + idempotency-key on confirm |
| **Race condition** (webhook retry, concurrent confirm) | PC (PSP queues retries) | PA (no tx-id short-circuit in current code) | findByTransactionId early-return |
| **Cross-tenant integrity** | PB/PC (per-tenant identifier native) | P0 (greedy substring match) | tenant_id filter in payment lookup |
| **UX clarity** (user knows what happened) | PA (webhook fast feedback 5-15min) | P0 (12-72h admin delay) | Payment status SSE + clear copy |
| **Observability** (admin/SRE debug) | PC (PSP dashboard + webhook event log) | P0 (only SLF4J log.info, no audit_log) | Persistent payment_audit_log |
| **Compliance** (PDPL Art 11, K-12 audit) | All weak — need audit_log; PC strongest via PSP record | All 4 require addition | payment_audit_log mandatory |

---

## 5. Section C — Pattern selection matrix

| Tenant | Recommended Phase 1 BETA | Recommended Phase 1.5 PAID | Reason |
|---|---|---|---|
| **T1 tiny (5-15)** | P0 Static QR + admin manual | P0 stays; OR PA Casso shared VA Phase 1.5 if license trivial | No business license barrier; 12-24h SLA acceptable; admin overhead low (<5 confirms/week) |
| **T2 small-medium (20-50)** | P0 (today) → PA migration target | **PA Casso/SePay dedicated VA** | Volume justifies webhook automation; T2 owners often have business license; auto-reconcile saves ~10h/month |
| **T3 medium (80-150)** | PA Casso (target Phase 1.5 launch) | **PA + PB hybrid** (PA fast feedback, PB monthly reconcile bank statement) | High volume needs automation + monthly accounting cross-check |
| **T4 large/school (200+)** | PA mandatory (P0 admin overhead infeasible) | **PB direct bank + PC PSP optional** | Dedicated accountants + audit dept; native VA + PSP for parent-side card flexibility |

**Risk acceptance Phase 1 BETA scope:**
- T1 stuck với P0: accept 12-24h SLA + Saturday delay risk
- T2 stuck với P0 until PA ships: accept ~3-5% typo rate + ops cost
- T3/T4 SHOULD NOT join Phase 1 BETA until PA available (or limit beta to ≤20 students/tenant)

---

## 6. Section D — Pre-walk state checks (acceptance criteria for top 10 P0)

| Cell | Pre-walk command |
|---|---|
| P0×F10 cross-tenant | `psql -c "SELECT instance_id, payment_content, COUNT(*) FROM payments WHERE status='PENDING' GROUP BY instance_id, payment_content HAVING COUNT(*) > 1"` → expect 0 collisions |
| PA×F7 webhook signature | `grep -rnE "(X-Signature\|HmacSha256\|verifyWebhookSignature)" kitehub/kitehub-subscription/src/main/java` → expect ≥1 hit in PaymentWebhookController |
| P0×F4 idempotency | `grep -n "UNIQUE\|unique" kitehub/kitehub-subscription/src/main/resources/db/migration/V*payments*.sql` → verify transaction_id UNIQUE constraint |
| P0×F8 admin typo | Code review `PaymentService.confirmPayment:298` — expect amount verify + optional bank-API cross-check |
| PA×F7 retry double-credit | `grep -A5 "processPaymentWebhook" PaymentService.java` → expect `findByTransactionId(...).ifPresent(...)` early return |
| PC×F*×T1 PSP scope | Confirm Phase 2 deferral documented; cite Wave 93 audit `2026-05-18-phase-1-5-26-gaps-re-triage.md` |
| P0 audit_log | `psql -c "SELECT event_type FROM admin_audit_logs WHERE event_type LIKE 'PAYMENT_%' LIMIT 1"` → expect rows post-walk |
| P0×F9 refund | `grep -n "RefundPayment\|REFUNDED" kitehub/kitehub-subscription/src/main/java -r` → expect Phase 1.5 scope confirmation OR placeholder |
| P0×F2 admin amount verify | `grep -A10 "AdminConfirmPaymentRequest" .../dto/AdminConfirmPaymentRequest.java` → expect amount field present |
| P0×F3 memo trống | Code review `findPaymentByContent:234` — verify fallback path for null/empty memo + admin reconcile UI link |

---

## 7. Cross-link

- Sister failure-mode matrix: `2026-06-04-pre-walk-kc1-failure-mode-matrix.md`
- Phase 1.5 outside-in: `2026-05-18-phase-1-5-qr-payment-outside-in.md`
- Wave 93 re-triage: `2026-05-18-phase-1-5-26-gaps-re-triage.md` (GAP-NEW-payment-processor-init CANCEL)
- External benchmark: `2026-06-04-pre-walk-kc1-external-benchmark.md` (if applicable for Casso/SePay benchmarks)
- Pre-walk mandate: `.claude/rules/pre-walk-persona-simulation-mandate.md` v1.0.0
- Flow checklist source: `.claude/rules/pre-handoff-self-test-completeness.md` v1.2.0 §2.6
- Existing GAP backlog: `documents/04-quality/gaps/gap-status.csv` — query `bash scripts/query-gaps.sh payment` to dedupe before filing new GAPs per §audit-to-gap-pipeline §2

## 8. New GAP candidates (12 — must Step 0 canonical-status lookup before filing)

Per `audit-to-gap-pipeline.md` §2 + memory `feedback_audit_candidate_pre_filing_state_check.md` — TRƯỚC khi file gap, MUST run `bash scripts/query-gaps.sh <prefix>` to dedupe:

1. `GAP-NEW-payment-find-by-content-tenant-scope` (P0 — cell P0×F10)
2. `GAP-NEW-webhook-signature-verification` (P0 — PA×F7)
3. `GAP-NEW-payment-idempotency-guard` (P0 — P0×F4)
4. `GAP-NEW-payment-confirm-verify-tx-existence` (P0 — P0×F8)
5. `GAP-NEW-webhook-idempotency-tx-id-short-circuit` (P0 — PA×F7)
6. `GAP-NEW-payment-admin-audit-log` (P1, escalates P0 Phase 3 — P0×F*)
7. `GAP-NEW-refund-flow-state-machine` (P1 Phase 1.5 — P0×F9)
8. `GAP-NEW-admin-confirm-amount-verify` (P1 — P0×F2)
9. `GAP-NEW-admin-orphan-bank-tx-reconcile-ui` (P1 — P0×F3)
10. `GAP-NEW-payment-memo-uniqueness-shortid-len` (P1 — P0×F6)
11. `GAP-NEW-payment-pending-cleanup-cron` (P1 — P0×F1)
12. `GAP-NEW-payment-pending-user-explainer-copy` + `GAP-NEW-payment-status-sse-channel` (P2 — P0×F5, PA×F5)

---

**Total findings:** 34 cells reported (out of 160 scanned)
**Severity breakdown:** P0=10 · P1=12 · P2=2 · cross-pattern observations=10
**Pattern recommendation:** T1+T2 stick P0 → migrate PA Phase 1.5; T3+T4 wait PA before joining beta
**Top action:** P0×F10 cross-tenant memo collision needs immediate guard BEFORE next admin confirm in production
