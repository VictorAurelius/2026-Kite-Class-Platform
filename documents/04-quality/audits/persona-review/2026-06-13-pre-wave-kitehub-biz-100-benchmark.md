---
audience: mixed
type: outside-in-benchmark
domain: kitehub-subscription-lifecycle
date: 2026-06-13
scope: pre-wave-kitehub-biz-100
method: external SaaS benchmark (Stripe Billing / Chargebee / Recurly / VN edu context)
---

# Outside-In Benchmark — KiteHub Subscription Lifecycle vs Industry SaaS Billing

**Auditor:** External SaaS benchmark agent (outside-in)
**Ngày:** 2026-06-13
**Scope:** So sánh thiết kế nghiệp vụ subscription-lifecycle của KiteHub (Phase 1 BETA, thanh toán VietQR thủ công, không gateway tự động) với chuẩn ngành (Stripe Billing, Chargebee, Recurly) + bối cảnh edu Việt Nam.
**Investigation order:** DESIGN trước (rules.md SUB/TR/T2P) → GAPS dedup (`query-gaps.sh`) → benchmark.

> **Lưu ý phạm vi:** KiteHub Phase 1 BETA chủ động chọn **VietQR/chuyển khoản thủ công + admin đối soát** (SUB-11/19). Nhiều pattern ngành (auto card-charge, smart-retry ML, payment-method-updater) **không áp dụng trực tiếp** — báo cáo này adapt chúng sang ngữ cảnh manual-VietQR thay vì copy nguyên. Các deliverable PSP/VAT/refund-engine đã được defer đúng đắn (xem §4 "Correctly deferred").

---

## 1. Tóm tắt thiết kế hiện tại (DESIGN đã đọc)

| Vùng | Rule liên quan | Nội dung |
|---|---|---|
| Grace period | SUB-04 | 3 ngày sau hết hạn |
| Warning trước hết hạn | SUB-05 | 7, 3, 1 ngày |
| Upgrade | SUB-07, SUB-17, SUB-20 | `pendingTier` + Payment PENDING; tier flip chỉ sau admin confirm; idempotent |
| Downgrade | SUB-08, SUB-09 | Chỉ xuống tier thấp hơn; áp dụng cuối chu kỳ |
| Proration | SUB-10 | `(newPrice - oldPrice)/cycleDays * max(daysLeft,0)` |
| Thanh toán | SUB-11, SUB-18, SUB-19 | VietQR thủ công + admin đối soát + confirm là capture source |
| Cancel | SUB-12, SUB-13 | Immediate (expiresAt=now) / end-of-cycle |
| Entitlement | SUB-22 | FREE/BASIC/PREMIUM/ENTERPRISE caps (students/teachers/storage/AI) |
| Trial | TR-01, TR-03, TR-04, TR-05, TR-06 | 14 ngày; warning day 7/11/13; auto-suspend; retention 7 ngày; retention warning day 3/6 |
| Trial→Paid | T2P-01..14 | Flip-in-place state machine, rollback window 24h, outbox events |

---

## 2. Benchmark table

| # | Nghiệp vụ lifecycle | Stripe / Chargebee / Recurly pattern | VN edu context | KiteHub design hiện tại | Verdict | Fix Phase-1-adapted (manual VietQR) |
|---|---|---|---|---|:---:|---|
| 1 | **Dunning cho pending payment** (chuỗi nhắc CK chưa tới) | Stripe dunning flow: chuỗi email sau khi payment "fail" + nhắc + exit khi paid; Chargebee reminder emails theo lịch | Trung tâm nhắc phụ huynh đóng học phí qua Zalo/SMS nhiều lần | Owner upgrade/create → tạo `Payment PENDING` → **KHÔNG có** chuỗi nhắc "hãy hoàn tất CK"; admin không có alert pending tồn đọng | ❌ **THIẾU** | Scheduled job nhắc Owner (email + Zalo stub GAP-721) "bạn có payment PENDING, hoàn tất CK trong N ngày"; admin dashboard list pending quá hạn |
| 2 | **Pending payment timeout/expiry** | Checkout Session tự expire (default 24h); Invoice auto-void sau recovery window | Báo giá/giữ chỗ có hạn | `Payment PENDING` sống **vô thời hạn**; SUB-17 trả lại pending cũ → Owner kẹt nếu đã bỏ ý định | ❌ **THIẾU** | TTL cho PENDING (vd 7 ngày) → auto-EXPIRED + giải phóng `pendingPaymentId` + notify Owner để tạo lại |
| 3 | **Dunning trong grace period** (sau hết hạn, trước suspend) | Recovery-window messaging: "payment failed, X ngày tới suspend"; Stripe dừng khi paid hoặc hết window | Nhắc gia hạn dồn dập khi sắp cắt lớp | SUB-05 chỉ nhắc **TRƯỚC** hết hạn (7/3/1); SUB-04 grace 3 ngày nhưng **KHÔNG có** rule nhắc trong grace | ⚠️ **PARTIAL** | Thêm reminder trong 3-ngày grace: "subscription đã hết hạn, còn X ngày trước khi suspend — gia hạn ngay" |
| 4 | **Involuntary churn path** (hết hạn không thanh toán → suspend) | Phân biệt involuntary (payment fail) vs voluntary (cancel); recovery rồi mới suspend | Học viên ngừng đóng → bảo lưu rồi mới cắt | SUB-04 grace 3 ngày tồn tại nhưng **KHÔNG có rule** mô tả hành động cuối grace cho **paid** sub (suspend instance? retention?); GAP-1016/1017 chỉ cover manual-renewal + cancel | ⚠️ **PARTIAL** | Rule rõ: paid sub expired → grace 3d → suspend instance (involuntary), tách path voluntary cancel; reactivate khi confirm renewal (đã có GAP-1016) |
| 5 | **Downgrade data/feature-loss warning** | Kinde: cảnh báo feature/data sẽ mất TRƯỚC khi áp; preserve data 30-90 ngày; UI giải thích feature bị khóa | Hạ gói → mất quyền lợi cần thông báo rõ | SUB-08/09 chỉ kiểm tra ordinal + cuối-chu-kỳ; **KHÔNG có** rule xử lý vượt-cap khi hạ tier (PREMIUM 200 students → BASIC 50) + không cảnh báo Owner; GAP-071 chỉ là branding (Phase 2) | ❌ **THIẾU** | Pre-downgrade summary: "150 học sinh vượt cap BASIC 50 → sẽ bị soft-lock/read-only"; Owner xác nhận trước khi schedule downgrade |
| 6 | **Biên nhận sau confirm payment** (non-VAT receipt) | Mọi payment sinh receipt/invoice; self-serve download | NĐ 123/2020: business có MST phải xuất hóa đơn VAT; nhưng biên nhận đơn giản phổ biến cho giao dịch nhỏ | Sau admin confirm **KHÔNG có** biên nhận Owner-facing; GAP-185 (VAT) PENDING Phase-1.5; GAP-630 chỉ là evidence nội bộ | ⚠️ **PARTIAL** | Sinh **biên nhận non-VAT** đơn giản (PDF/email) sau confirm: số tiền, tier, kỳ, txnId — **VAT e-invoice đúng là defer** (GAP-185/315/634) |
| 7 | **Win-back / reactivation sau cancel/expiry** | Reactivation sequence 1-2 ngày sau expiry; "work của bạn chưa mất"; offer trial extension / fresh trial | Nhắc tái đăng ký khóa mới | GAP-1016 cover **kỹ thuật** reactivate-on-renewal; **KHÔNG có** email win-back hay reactivation CTA | ❌ **THIẾU** | Win-back email sau cancel/expiry (Phase-1.5) + nút "Kích hoạt lại" trên billing page khi instance SUSPENDED còn trong retention |
| 8 | **Self-serve billing portal** | Chargebee/Recurly portal: xem/đổi plan, lịch sử invoice, download receipt, manage payment | Phụ huynh muốn xem lịch sử đóng tiền | Owner upgrade/cancel được nhưng **billing history / next-renewal / invoice list / receipt download** chưa hoàn chỉnh; GAP-1079 billing page crash khi no-active-sub; GAP-1093 renew không có FE caller | ⚠️ **PARTIAL** | Billing portal: trạng thái sub, ngày gia hạn kế, lịch sử payment, list/download biên nhận; fix GAP-1079 trước (crash) |
| 9 | **Trial conversion cadence + extension** | 5-7 email (welcome/midpoint/expiry-warning/last-chance/winback); start 5-7d trước hết hạn cho trial 14d; offer trial extension | — | TR-03 = day 11 + 13 (+ midpoint day 7) = ~3 touch (ngành khuyến nghị 5-7); **KHÔNG có** cơ chế trial extension | ⚠️ **PARTIAL** | Enrich cadence (thêm welcome day-0 + last-chance day-14) + cơ chế one-time trial extension (Owner request / admin grant) |
| 10 | **Paid post-suspend retention + soft vs hard suspend** | Soft-suspend (disable access, preserve data 30-90d) → hard-suspend (deprovision) | Bảo lưu data trước khi xóa | TR-05 retention **7 ngày chỉ cho TRIAL**; **KHÔNG có** retention window riêng cho PAID sub sau suspend; suspend = cắt full access (không có read-only intermediate); GAP-1026 off-boarding cận | ⚠️ **PARTIAL** | Rule retention paid-sub sau suspend (vd 30 ngày) trước purge + cân nhắc read-only soft-suspend; align PDPL pre-deletion notice (GAP-1026) |
| 11 | **Proration khi đổi billing cycle** | Proration khi MONTHLY↔ANNUALLY switch | — | SUB-10 prorate theo tier-price nhưng **KHÔNG cover** đổi độ-dài chu kỳ (MONTHLY→ANNUALLY) | ⚠️ **PARTIAL (edge)** | Rule prorate/credit khi đổi BillingCycle; thấp ưu tiên (ít xảy ra Phase-1 beta) |

**Lưu ý — downgrade mid-cycle KHÔNG refund:** SUB-09 (downgrade end-of-cycle, không hoàn tiền giữa kỳ) **khớp chuẩn ngành** (Kinde/Stripe thường áp downgrade cuối kỳ, không refund) → ✅ OK, không phải gap.

---

## 3. Findings list (NET-NEW vs overlap)

### Finding 1 — Pending-payment dunning chain (manual VietQR adapt) — P1 — NET-NEW
Owner tạo `Payment PENDING` (create/upgrade/renewal) nhưng không có chuỗi nhắc hoàn tất CK + admin không thấy pending tồn đọng. Đây là adapt của failed-payment dunning (Stripe/Chargebee) cho ngữ cảnh thủ công. NET-NEW (GAP-1080 chỉ cover idempotency duplicate, không cover reminder).

### Finding 2 — Stale pending-payment TTL/expiry — P2 — NET-NEW
`Payment PENDING` sống vô thời hạn; SUB-17 trả lại pending cũ → Owner kẹt nếu đã bỏ ý định upgrade. Cần TTL auto-expire + cleanup. Partial overlap GAP-1080 (idempotency) nhưng TTL là concern riêng.

### Finding 3 — Grace-period dunning reminders — P1 — NET-NEW (sister của Finding 1)
SUB-05 chỉ nhắc TRƯỚC hết hạn; trong 3-ngày grace (SUB-04) không có reminder. Industry messaging recovery-window. NET-NEW.

### Finding 4 — Involuntary churn lifecycle spec (expired-unpaid → suspend) — P1 — maps-to-GAP-1016/1017 (partial, spec gap)
SUB-04 grace tồn tại nhưng không rule mô tả hành động cuối grace cho PAID sub (suspend? retention?). GAP-1016 (manual renewal reactivate) + GAP-1017 (cancel→suspend) cover phần liền kề nhưng **auto involuntary path "hết-hạn-không-trả → suspend"** chưa được spec hóa. Overlap một phần — nên bổ sung rule vào cluster GAP-1016/1017 hoặc gap mới.

### Finding 5 — Downgrade over-cap data/feature-loss warning — P1 — NET-NEW
Entitlement caps tụt khi hạ tier (PREMIUM 200 → BASIC 50 students); không rule xử lý vượt-cap + không cảnh báo Owner trước. GAP-071 chỉ branding (Phase 2). NET-NEW: pre-downgrade summary + confirm + soft-lock excess.

### Finding 6 — Owner-facing payment confirmation receipt (non-VAT biên nhận) — P2 — maps-to-GAP-185 (partial; lighter Phase-1 slice)
Sau admin confirm không sinh biên nhận Owner-facing. VAT e-invoice **đúng là defer** (GAP-185 PENDING / GAP-315 Phase-3 / GAP-634 MISA). Nhưng **biên nhận non-VAT đơn giản** (số tiền/tier/kỳ/txnId) là kỳ vọng Phase-1 hợp lý, tách khỏi VAT. Partial overlap GAP-185.

### Finding 7 — Win-back / reactivation outreach sau cancel/expiry — P2 — NET-NEW (phase-1.5)
GAP-1016 cover kỹ thuật reactivate; không có email win-back / reactivation CTA. NET-NEW. Hợp với phase-1.5.

### Finding 8 — Self-serve billing portal completeness — P2 — maps-to-GAP-1079/1093 (partial)
Thiếu view billing history / next-renewal / invoice list / receipt download hoàn chỉnh. GAP-1079 (billing crash no-active-sub) + GAP-1093 (renew no FE caller) là mảnh; portal-level completeness là NET-NEW umbrella.

### Finding 9 — Trial conversion cadence enrichment + trial extension — P2 — NET-NEW
TR-03 ~3 touch vs ngành 5-7; không cơ chế trial extension. NET-NEW enhancement. (Không over-scope: cadence là email config, extension là feature nhỏ.)

### Finding 10 — Paid-sub post-suspend retention + soft-suspend — P2 — maps-to-GAP-1026 (partial)
TR-05 retention 7 ngày chỉ TRIAL; PAID sub sau suspend không có retention window riêng + không soft-suspend (read-only). GAP-1026 (off-boarding/retention) cận; cần rule retention paid-sub + soft vs hard.

### Finding 11 — Proration khi đổi billing cycle — P3 — NET-NEW (edge)
SUB-10 không cover MONTHLY↔ANNUALLY switch proration. Edge case, ưu tiên thấp.

---

## 4. Correctly Phase-2/1.5-deferred (KHÔNG over-scope Phase 1)

| Pattern ngành | Trạng thái KiteHub | Verdict |
|---|---|---|
| Auto card-charge + smart-retry ML (Stripe 8x/2wk) | N/A — VietQR thủ công | ✅ Đúng là defer (Phase 2 khi có PSP) |
| Card-on-file expiry / Account Updater | N/A — không lưu thẻ | ✅ Đúng là defer |
| VAT e-invoice + TCT API + HSM signature | GAP-185 PENDING / GAP-315 Phase-3 / GAP-634 MISA partnership | ✅ Đúng là defer (chỉ thiếu biên nhận non-VAT — Finding 6) |
| Refund engine | GAP-183 Phase-1.5 PARTIAL → manual SOP GAP-629/630 | ✅ Đúng là defer (manual SOP đúng hướng) |
| Payment gateway MoMo/VNPay | SUB-11 defer Phase 2 | ✅ Đúng là defer |
| QR account verification refresh | GAP-631 Phase-1.5 | ✅ Đã có kế hoạch |
| Late-cancel charge workflow | GAP-295 Phase-2 | ✅ Đúng là defer |

---

## 5. Sources (industry claims)

- Stripe Smart Retries / dunning flow: [Stripe — Automate payment retries](https://docs.stripe.com/billing/revenue-recovery/smart-retries) · [Stripe Revenue recovery](https://docs.stripe.com/billing/revenue-recovery) · [Triggla — Stripe dunning flow](https://triggla.com/blog/stripe-dunning-flow-automate-recovery-after-failed-payments)
- Chargebee dunning / involuntary churn: [Chargebee — Dunning best practices](https://www.chargebee.com/blog/dunning-process-best-practices/) · [Chargebee — Failed payments & involuntary churn guide](https://www.chargebee.com/resources/guides/involuntary-churn-payment-failed/) · [Chargebee — 23 ways to reduce involuntary churn](https://www.chargebee.com/blog/reduce-involuntary-churn-23-ways/)
- Downgrade / data-loss warning: [Kinde — Best practices for plan upgrades and downgrades](https://www.kinde.com/learn/billing/plans/best-practices-for-handling-plan-upgrades-and-downgrades/)
- Trial conversion / win-back cadence: [FluentCRM — SaaS free trial email sequence](https://fluentcrm.com/blog/saas-free-trial-email-sequence/) · [Sequenzy — Trial expiration email sequences](https://www.sequenzy.com/blog/how-to-set-up-trial-expiration-emails)
- Self-serve portal: [Chargebee — Self-Serve Portal docs](https://www.chargebee.com/docs/billing/2.0/hosted-capabilities/self-serve-portal)
- VN edu context: [ILA — học phí các trung tâm tiếng Anh](https://ila.edu.vn/hoc-phi-cac-trung-tam-tieng-anh-cho-tre-em) · [Apollo — trả góp học phí](https://apollo.edu.vn/tieng-anh-tre-em/tra-gop-hoc-phi) (học phí 2.8-5M VND/tháng, trả góp 0% qua thẻ tín dụng → kỳ vọng biên nhận + linh hoạt thanh toán)
