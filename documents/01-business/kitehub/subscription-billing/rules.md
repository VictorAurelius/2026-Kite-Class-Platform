# Subscription & Billing — Business Rules

**Last verified:** 2026-03-24
**Config prefix:** `kitehub.subscription`

## Rules

| ID | Rule | Value | Config Key |
|----|------|-------|-----------|
| SUB-01 | FREE tier không được tạo subscription | N/A | hardcoded SubscriptionService |
| SUB-02 | Billing cycles | MONTHLY (30 ngày), ANNUALLY (365 ngày) | BillingCycle enum |
| SUB-03 | Auto-renew mặc định | true | request.getAutoRenew() |
| SUB-04 | Grace period sau hết hạn | 3 ngày | `kitehub.subscription.grace-period-days` |
| SUB-05 | Warning days | 7, 3, 1 ngày trước hết hạn | `kitehub.subscription.warning-days` |
| SUB-06 | Upgrade: chỉ lên tier cao hơn | ordinal comparison | hardcoded |
| SUB-07 | Upgrade timing Phase 1 BETA | **Không nâng tier ngay**; set `pendingTier` + tạo/reuse `Payment PENDING`; tier chỉ apply sau admin confirm payment | `kitehub.subscription.upgrade.apply-after-payment: true` |
| SUB-08 | Downgrade: chỉ xuống tier thấp hơn | ordinal comparison | hardcoded |
| SUB-09 | Downgrade timing | Cuối chu kỳ hiện tại | pendingTier field |
| SUB-10 | Prorated formula | `(newPrice - oldPrice) / cycleDays * max(daysLeft, 0)`; minimum payable amount = 0 means no payment required | hardcoded |
| SUB-11 | Default payment method Phase 1 BETA | `VIETQR`/manual bank transfer; admin đối soát rồi confirm; MoMo/VNPay gateway deferred Phase 2+ | PaymentMethod.VIETQR |
| SUB-12 | Cancel immediate | expiresAt = now, autoRenew=false | hardcoded |
| SUB-13 | Cancel end-of-cycle | giữ expiresAt, autoRenew=false | hardcoded |
| SUB-14 | 1 subscription active per instance | validate on create | hardcoded |
| SUB-15 | Currency | VND minor unit (integer đồng, không decimal) | payment.setCurrency("VND") |
| SUB-16 | Expiring query window | 30 ngày tới | hardcoded |
| SUB-17 | Upgrade payment idempotency | Nếu subscription đã có `pendingPaymentId` trỏ tới `Payment PENDING`, retry upgrade cùng pending tier trả lại payment đó; không tạo payment thứ hai | `payments.status=PENDING` + `subscriptions.pending_payment_id` |
| SUB-18 | Payment content uniqueness | Nội dung chuyển khoản phải chứa short subscription id/payment marker đủ để admin đối soát trong bảng pending payments | VietQRService.generatePaymentContent |
| SUB-19 | Admin confirm is payment capture source | `POST /admin/payments/{id}/confirm` là nguồn capture chính Phase 1 BETA; automated webhook/bank API chỉ future enhancement | PaymentService.confirmPayment |
| SUB-20 | Create-first-paid phải qua cổng VietQR thủ công | `POST /api/platform/subscriptions` với tier != FREE tạo subscription với `status=PENDING, tier=FREE, pendingTier=<requested>, pendingPaymentId=<new>` + Payment PENDING. Tier chỉ flip sang `requested` + status flip `ACTIVE` khi `PaymentService.confirmPayment` gọi `applyPendingUpgrade`. Mirror UC-SUB-02 manual VietQR pattern. Phát hiện qua Wave flow-kh3 G1 walk 2026-06-04 — pre-rule create flow tự ý mark `status=ACTIVE` mà không có payment gate. | hardcoded SubscriptionService.createSubscription |
| SUB-21 | `instances.tier` phải mirror tier của subscription ACTIVE | `subscriptions.tier` = source-of-truth; `instances.tier` = denormalized synced current-effective-tier. Sync khi tier thực sự apply: `applyPendingUpgrade` (create-flow activation + upgrade-flow apply) và `processRenewal` (end-of-cycle downgrade apply). `instances.tier` là load-bearing — connection-pool size (`MultiTenantDataSourceConfig`), custom-domain eligibility (`DomainService`), data-retention window (`DataRetentionService`). Phát hiện qua GAP-1090 — pre-rule chỉ flip `subscriptions.tier`, để `instances.tier` kẹt FREE/pre-change tier. | hardcoded SubscriptionService.applyPendingUpgrade + SubscriptionRenewalService.processRenewal; backfill V68__sync_instance_tier_to_active_subscription.sql |
| SUB-22 | Entitlement matrix per tier (canonical) | Bảng caps/quota/giá theo 4 tier `FREE/BASIC/PREMIUM/ENTERPRISE` — xem section `## Entitlement matrix` bên dưới. TRIAL = entitlement FREE + time-box 14 ngày (subscription STATE, KHÔNG phải tier riêng). | `PricingTier` enum + `ai.input.*` + `RateLimitConfig.tierMultiplier`; propagation per ADR-039 |
| SUB-23 | Pending-payment TTL + dunning | `Payment PENDING` có TTL `kitehub.subscription.pending-payment-ttl-days: 7` → quá hạn auto-FAILED (PaymentStatus chưa có `EXPIRED`; payments.status CHECK chỉ cho PENDING/COMPLETED/FAILED/REFUNDED/CANCELLED → FAILED = timeout documented) + giải phóng `pendingPaymentId`. Trong khi chờ admin confirm VietQR: grace-period dunning reminder "còn X ngày trước suspend" (reuse renewal-reminder email, dedup `alreadySentToday`). Wave kitehub-biz-100. | GAP-1259; `SubscriptionExpirationChecker.processStalePendingPayments` + grace dunning trong `processExpiredSubscriptions` |
| SUB-24 | Involuntary-churn (PAID hết grace) | Subscription ACTIVE hết hạn + qua grace 3 ngày (SUB-04) mà chưa thanh toán → auto-suspend instance + đánh dấu involuntary churn (phân biệt voluntary cancel SUB-12/13). Phase 1: WARN-log classification (BE-1, `SubscriptionRenewalService.suspendExpiredSubscription`). Queryable `subscriptions.churn_type` (VOLUNTARY/INVOLUNTARY) column **DEFERRED Phase 1.5** — set-points nằm ở suspend paths thuộc `SubscriptionRenewalService` + `SubscriptionService` (cross-owner), nên cần wave riêng để wire atomic; reserved migration V74. Wave kitehub-biz-100. | GAP-1260; WARN-log shipped, queryable column deferred |
| SUB-25 | `suspended_at` = nguồn deterministic cho retention clock | Khi suspend (trial / involuntary / cancel) set cột `instances.suspended_at`; `DataRetentionService` tính retention window từ `suspended_at`, KHÔNG dùng `updated_at` (tránh reset clock khi row update khác — PDPL determinism). Wave kitehub-biz-100. | GAP-1264; V73 `suspended_at` + DataRetentionService |
| SUB-26 | Downgrade over-cap warning + confirm | Trước khi áp downgrade (SUB-08/09) mà tier mới có cap thấp hơn usage hiện tại (students / storage / custom-domain) → hiển thị impact summary + bắt owner confirm chủ động + soft-lock excess (KHÔNG xóa data). Wave kitehub-biz-100. | GAP-1261; downgrade flow FE+BE |

## Config

```yaml
kitehub:
  subscription:
    grace-period-days: 3
    warning-days: 7,3,1
    pending-payment-ttl-days: 7              # SUB-23 (GAP-1259) — stale PENDING payment auto-FAILED + release pendingPaymentId
    orphan-pending-subscription-ttl-days: 7  # GAP-1080 AC#2 — orphan PENDING subscription cleanup sweep

payment:
  vietqr:
    api-url: ${VIETQR_API_URL:https://api.vietqr.io/v2/generate}
    api-key: ${VIETQR_API_KEY:}
    mock-mode: ${PAYMENT_MOCK_MODE:true}
    bank-code: ${VIETQR_BANK_CODE:VCB}
    account-number: ${VIETQR_ACCOUNT_NUMBER:}
    account-name: ${VIETQR_ACCOUNT_NAME:}
    template: ${VIETQR_TEMPLATE:compact}
```

## Phase 1 BETA payment policy

KiteHub subscription billing dùng **chuyển khoản ngân hàng thủ công/VietQR** trong Phase 1 BETA:

1. Owner chọn upgrade → backend tính prorated charge và tạo `Payment PENDING`.
2. FE hiển thị QR/thông tin chuyển khoản cho user, kèm nội dung chuyển khoản bắt buộc.
3. User chuyển khoản ngoài hệ thống.
4. Platform admin đối soát statement ngân hàng, nhập `transactionId`, rồi confirm payment.
5. Chỉ sau confirm, backend mới apply `pendingTier` vào subscription.

Không tích hợp MoMo/VNPay/Stripe tự động trong Phase 1 BETA. Các enum `MOMO`/`VNPAY` được giữ để tương thích domain tương lai nhưng không là default path cho soft launch. Quyết định này giảm scope giấy phép/merchant/KYC, phù hợp beta cohort nhỏ và solo-dev operation.

**Source:** User decision 2026-06-03 + outside-in payment scope lessons Wave 93 (partnership/PSP licensing complexity) + existing KiteClass manual-transfer pattern.
**Rationale:** Manual transfer đủ dùng cho beta volume nhỏ; admin confirm giúp kiểm soát fraud/nhầm nội dung chuyển khoản; PSP auto-capture deferred đến Phase 2 khi có legal/counsel và merchant readiness.
**Reviewer:** @nguyenvankiet (acting Product Owner, solo-dev, 2026-06-04). Formal legal/tax review remains queued via GAP-156.
**Compliance check:** **Considered** — Consumer Protection Law (clear price/payment instruction), Luật Giao dịch điện tử 2023, tax/e-invoice obligations. No auto-renew card capture in Phase 1 BETA.
**Review cadence:** Quarterly. **Next review:** 2026-09-04 or when PSP integration/paid cohort scale >5 beta tenants.

## Entitlement matrix

**SUB-22** — canonical entitlement per tier. `PricingTier` enum (`PricingTier.java`) là source-of-truth cho caps/price/custom-domain; AI input cap + branding regenerate + rate-limit multiplier cite source riêng (cột dưới). Propagation cross-service per [ADR-039](../../../02-architecture/adr/ADR-039-cross-service-subscription-tier-propagation.md).

| Tier | maxStudents | maxTeachers | storageMB | priceVND/mo | branding regen/ngày | AI banner mode (FULL_AI/tháng) | AI input cap (tokens) | custom domain | rate-limit multiplier |
|------|------------:|------------:|----------:|------------:|--------------------:|:------------------------------:|----------------------:|:-------------:|:---------------------:|
| FREE | 10 | 1 | 500 | 0 | 3 | TEMPLATE | 2000 | ❌ | 1× |
| BASIC | 50 | 5 | 2048 | 500.000 | 10 | TEMPLATE | 4000 | ❌ | 1× |
| PREMIUM | 200 | 20 | 10240 | 1.500.000 | 30 | FULL_AI (5/tháng) | 8000 | ✅ | 3× |
| ENTERPRISE | ∞ | ∞ | ∞ | custom | ∞ (-1) | FULL_AI (∞) | 16000 | ✅ | 10× |

**TRIAL clarification:** TRIAL KHÔNG phải tier riêng — là **subscription STATE** (time-box 14 ngày) với entitlement = FREE. Khi trial hết hạn không upgrade → tenant ở lại entitlement FREE. AI input cap §2.5 (`ai-branding-guidelines.md`) ghi rõ `FREE / TRIAL = 2000`.

**Sources (verified):**
- `maxStudents` / `maxTeachers` / `storageMB` / `priceVND`: `kitehub/kitehub-platform/src/main/java/com/kitehub/platform/domain/enums/PricingTier.java:16-31` (enum constructor args) + `:36` (`priceVND` field; ENTERPRISE `0L` = custom pricing).
- `custom domain`: `PricingTier.java:50-52` (`allowsCustomDomain()` → `this == PREMIUM || this == ENTERPRISE`).
- `AI input cap (tokens)`: `.claude/rules/ai-branding-guidelines.md` §2.5 (GAP-258 `AIInputCapService`; `ai.input.*` keys; chars/4 heuristic; `-1` = unlimited).
- `branding regen/ngày`: `.claude/rules/ai-branding-guidelines.md` §4.3 (counter visible + decremented; hết quota → disabled button + upgrade CTA). PREMIUM = **30** (canonical; GAP-1137 đồng bộ code `application.yml branding.rate-limit.premium-per-day` 50→30).
- `AI banner mode (FULL_AI/tháng)`: GAP-1137 — banner TEMPLATE (HTML+Gemini→Playwright, $0) mặc định mọi tier; **FULL_AI (GPT-5.5 image-gen, có phí) chỉ PREMIUM + ENTERPRISE** (per `ai-branding-guidelines.md` §2.4 + ADR-037 Amendment 2026-06-10). PREMIUM giới hạn cost quota riêng (`ai.rate-limit.fullai-premium-per-month`, mặc định 5/tháng → hết thì fallback TEMPLATE); ENTERPRISE unlimited. Enforce: `GenerationMode.forTier` (eligibility) + `FullAiQuotaService` (PREMIUM monthly cap) + cost metric `ai.fullai.call{tier,outcome}`. FREE/BASIC không eligible.
- `rate-limit multiplier`: GAP-260 (`gateway-tier-multiplier-enforcement` — `RateLimitConfig.tierMultiplier` FREE 1× / BASIC 1× / PREMIUM 3× / ENTERPRISE 10×).

### Five-attribute review per `business-logic-review.md` §2 (SUB-22)

- **Source:** Code-derived canonical (`PricingTier.java` caps/price/domain) + rule-derived (`ai-branding-guidelines.md` §2.5/§4.3) + GAP-260 (multiplier). Đây là entitlement *aggregation* từ các source đã review, không phải giá trị mới. Per-value pricing rationale (vì sao 500k/1.5M) inherit GAP-156 Phase 2 stakeholder review.
- **Rationale:** Bảng tập trung mọi entitlement vào MỘT canonical reference để cross-service enforcement (per ADR-039) đọc nhất quán — tránh drift giữa code enum, AI cap rule, regen rule, multiplier gap (mỗi nơi định nghĩa rời rạc → silent divergence). Tier-laddering (FREE eval → BASIC small → PREMIUM mid → ENTERPRISE custom) phản ánh quy mô trung tâm + willingness-to-pay.
- **Reviewer:** @nguyenvankiet (acting Product Owner, solo-dev, 2026-06-09). Self-approval cho business-value bị BANNED per `business-logic-review.md` §2.3 — formal PO + Business Stakeholder sign-off cho pricing/tier-quota queued via GAP-156 Phase 2. Solo-dev exemption documented (hat = acting PO).
- **Compliance check:** **Considered** — Consumer Protection Law 2023 (hiển thị giá rõ ràng per tier); giá VND minor-unit integer per SUB-15. Không trigger PDPL (entitlement không phải PII). Tax/e-invoice obligation áp dụng tại payment confirm (SUB-19), không tại matrix.
- **Review cadence:** Quarterly. **Next review:** 2026-09-09. Event triggers: thêm/bớt tier, đổi giá, đổi cap/quota, competitor pricing change, GAP-156 Phase 2 stakeholder review lands.

## Five-attribute review per `business-logic-review.md`

Per-rule attributes (Source / Rationale / Reviewer / Compliance check / Review cadence) backfilled at file-level placeholder per Phase 1 of GAP-433. Per-rule granularity tracked via GAP-156 Phase 2 stakeholder sign-offs.

- **Source:** Existing rules in this file derive from a mix of: feature gaps cited inline (where present), ADRs, persona reviews, and informed-gut estimates from Wave 1-30 work. Rules without inline citation default to `informed gut` per `business-logic-review.md` §2.1 and inherit quarterly re-review obligation below.
- **Rationale:** Rule values reflect product judgment + (where applicable) competitor benchmarks + VN regulatory minimums. Detailed per-rule rationale to be backfilled during GAP-156 Phase 2 stakeholder review; until then, treat values as `informed gut` subject to next quarterly review.
- **Reviewer:** @nguyenvankiet (acting Product Owner, solo-dev, 2026-05-08). Formal stakeholder + legal counsel sign-off queued via GAP-156. Solo-dev exemption per `business-logic-review.md` §2.3 — the Reviewer line documents which hat is being worn AND obligation is attached for team-growth or pre-launch trigger.
- **Compliance check:** **Compliant** — Luật Quản lý Thuế 2019; Nghị định 123/2020/NĐ-CP (e-invoice); Consumer Protection Law (refund + dispute window 24mo); Luật Giao dịch điện tử 2023.
- **Review cadence:** Quarterly (default per `business-logic-review.md` §2.5). **Next review:** 2026-08-08. Event triggers: Tax law amendment, e-invoice regulation, payment-gateway swap, tier pricing change.

## Log

- **2026-06-09** Tier-enforcement wave — added SUB-22 (Entitlement matrix canonical) + new `## Entitlement matrix` section. Tập trung caps/quota/giá 4 tier `FREE/BASIC/PREMIUM/ENTERPRISE` vào MỘT canonical reference cho cross-service enforcement per ADR-039. Verified sources: `PricingTier.java:16-31,50-52` (caps/price/custom-domain) + `ai-branding-guidelines.md` §2.5 (AI input cap 2000/4000/8000/16000) + §4.3 (regen 3/10/30/∞) + GAP-260 (rate-limit multiplier 1×/1×/3×/10×). TRIAL clarified = entitlement FREE + time-box 14 ngày (subscription STATE, không phải tier riêng). **Five-attribute review per `business-logic-review.md` §2:** **Source:** code-derived (`PricingTier.java`) + rule-derived (`ai-branding-guidelines.md`) + GAP-260; aggregation từ source đã review. **Rationale:** single canonical reference tránh drift giữa enum/AI-cap/regen/multiplier rời rạc → cross-service đọc nhất quán. **Reviewer:** @nguyenvankiet (acting PO, solo-dev) — pricing/tier-quota formal sign-off BANNED self-approve per §2.3, queued GAP-156 Phase 2. **Compliance check:** Considered — Consumer Protection Law (hiển thị giá rõ); không trigger PDPL (entitlement ≠ PII). **Review cadence:** Quarterly. **Next review:** 2026-09-09 hoặc khi thêm/đổi tier/giá. Same-wave: ADR-039 (propagation) + tier-name drift sweep (multi-tenant §6 + ai-branding-guidelines §4.3).
- **2026-06-09** GAP-1090 discovery — added SUB-21 (`instances.tier` mirror active `subscriptions.tier`). `SubscriptionService.applyPendingUpgrade` (create-flow activation + upgrade-flow apply) và `SubscriptionRenewalService.processRenewal` (end-of-cycle downgrade apply) flip `subscriptions.tier` nhưng không bao giờ gọi `instance.setTier(...)` → `instances.tier` kẹt FREE (hoặc pre-change tier) dù subscription ACTIVE ở tier cao/thấp hơn. `instances.tier` là load-bearing (pool size + custom-domain eligibility + retention window). Same-PR: code fix 3 path (create-flow + upgrade-else + downgrade-apply) + Flyway backfill `V68__sync_instance_tier_to_active_subscription.sql` (idempotent UPDATE rows drifted) + tests updated. **Five-attribute review per `business-logic-review.md` §2:** **Source:** GAP-1090 cross-service tier-propagation analysis (denormalization invariant — không phải pricing/market value). **Rationale:** `subscriptions.tier` đã là source-of-truth; `instances.tier` denormalized để các consumer (pool sizing, domain, retention) đọc trực tiếp khỏi join subscription mỗi request → sync-on-apply giữ invariant rẻ + đúng. **Reviewer:** @nguyenvankiet (acting architect, solo-dev, 2026-06-09) — đây là data-consistency invariant không phải business-value nên không cần PO/legal sign-off; formal review queued GAP-156. **Compliance check:** N/A — không chạm vùng regulated (không đổi giá, retention window, hay PII; chỉ đồng bộ tier giữa 2 bảng). **Review cadence:** Quarterly cùng phần còn lại file. **Next review:** 2026-09-09 hoặc khi thêm tier mới / đổi denormalization strategy.
- **2026-06-04** Wave flow-kh3 G1 walk discovery — added SUB-20 (Create-first-paid manual VietQR gate). UC-SUB-01 pre-rule code mark `status=ACTIVE` immediately on POST `/api/platform/subscriptions` without payment, allowing Owner self-grant BASIC/PREMIUM/ENTERPRISE for free. Rule mirrors SUB-07/SUB-11/SUB-17 upgrade pattern: tạo PENDING subscription + PENDING Payment, tier+status flip chỉ sau admin confirm. Same-PR: code fix `SubscriptionService.createSubscription` + `applyPendingUpgrade` extended to handle create-case, `SubscriptionStatus.PENDING` enum value added, tests updated.
- **2026-05-08** Backfill 5-attribute review section per GAP-433 Phase 1 (`business-logic-review.md` §2 standard). Placeholder Reviewer + Quarterly cadence + domain-specific Compliance check. GAP-156 Phase 2 will replace placeholders with stakeholder sign-offs.
