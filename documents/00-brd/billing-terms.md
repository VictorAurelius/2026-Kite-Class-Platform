# Billing Terms + VAT/TCT Invoice Compliance — KiteHub/KiteClass

**Trạng thái:** 🔵 SKELETON (Phase 1 — section structure + payment matrix + tax calc examples + late fee config link; Phase 2 legal counsel + Tax advisor + TCT e-invoice provider selection via GAP-154 + GAP-108)
**Owner:** Legal + Finance
**Reviewer:** Legal counsel + Tax advisor + TCT-registered e-invoice provider liaison (VNPT/Viettel/Misa) + Finance Lead (Phase 2)
**Last-Updated:** 2026-04-29
**Tracking:** GAP-185 (Phase 1, Wave Legal-BRD Phase 1.5 2026-04-29) → GAP-154 (Phase 2 content + legal sign-off) → GAP-108 (config externalization for late fee + grace period)
**Legal basis:** **Circular 78/2021/TT-BTC** (e-invoice mandate + XML schema), **Decree 123/2020/NĐ-CP** (invoice + tax records 10 năm retention), **VAT Law 2008** + amendments (10% standard rate, 5% education exemption analysis), **Luật Quản lý Thuế 2019** (tax management framework), Commercial Law 2005 (commercial billing terms)
**Cross-cuts:** [terms-of-service.md](terms-of-service.md) (GAP-180 — references billing terms), [refund-dispute-resolution-policy.md](refund-dispute-resolution-policy.md) (GAP-183 sibling Agent A this wave — refund calculation basis), [privacy-policy.md](privacy-policy.md) (GAP-182 — payment data handling per PDPL), [data-retention-deletion-policy.md](data-retention-deletion-policy.md) (GAP-184 — financial records 10y per Tax Law), GAP-108 (planned — config externalization, drift root cause: hardcoded `LATE_FEE_RATE=0.001`), [pricing-model.md](pricing-model.md) (tier alignment — FREE/BASIC/PREMIUM/ENTERPRISE)

---

## 1. Phạm vi & nguyên tắc

Tài liệu này định nghĩa các điều khoản thanh toán, xử lý thuế (VAT), và phát hành hóa đơn điện tử cho cả nền tảng KiteHub (SaaS quản lý) và KiteClass (multi-tenant giáo dục). **Skeleton Phase 1** chỉ thiết lập khung 9 sections + 3 bảng matrix với placeholder values; nội dung pháp lý đầy đủ + sign-off từ legal counsel + Tax advisor + TCT-registered e-invoice provider thuộc Phase 2 (GAP-154 + GAP-108).

Nguyên tắc cốt lõi:
- **Compliance-first:** mọi hóa đơn phát hành cho khách hàng Việt Nam PHẢI tuân thủ **Circular 78/2021/TT-BTC** (e-invoice mandatory) và **Decree 123/2020/NĐ-CP** (XML schema + 10 năm retention).
- **Transparency:** giá hiển thị PHẢI rõ ràng tax-inclusive vs tax-exclusive (theo Luật Bảo vệ Quyền lợi Người tiêu dùng 2023).
- **Tier-aware:** payment methods + billing cycles + currency phụ thuộc tier (FREE/BASIC/PREMIUM/ENTERPRISE — xem `pricing-model.md`).
- **Config externalization:** mọi giá trị business (late fee rate, grace period, notice period) PHẢI ở `application.yml`, KHÔNG hardcode trong Java/TS code (anti-pattern hiện tại tracked qua GAP-108).
- **MST collection mandatory** cho mọi tenant Việt Nam request VAT invoice (VAT deduction yêu cầu MST hợp lệ trên hóa đơn).

Phạm vi áp dụng: KiteHub subscription billing (SaaS plan), KiteClass per-tenant billing (school operations), enterprise procurement (PO/net-30/net-60), promotions + discounts, refunds (link → GAP-183), late payment, currency conversion (international tenants).

---

## 2. Payment Terms

### 2.1 Billing cycle

Ba chu kỳ chính, lựa chọn theo tier + use-case:

- **Monthly:** mặc định cho FREE/BASIC/PREMIUM (auto-renewal). Charge ngày tenant signup mỗi tháng.
- **Annual:** discount TODO 15-25% (xem `pricing-model.md` §2 Annual discount). Phù hợp PREMIUM/ENTERPRISE muốn lock-in giá + giảm friction renewal.
- **Per-enrollment:** dành cho KiteClass tenant nhỏ (P1 Solo Teacher, P2 Small Center) charge per student-active hoặc per-class. <!-- Phase 2: per-enrollment pricing model — informed gut Q3 2026, GAP-154 -->

### 2.2 Due date

| Tier | Default due date | Enterprise option |
|------|------------------|-------------------|
| FREE | N/A (no charge) | — |
| BASIC | Upfront (charge khi start period) | — |
| PREMIUM | Upfront | — |
| ENTERPRISE | Upfront OR Net-30 / Net-60 (negotiated) | Net-30 default qua PO process (xem §8) |

Mid-tier (BASIC/PREMIUM) KHÔNG hỗ trợ end-of-period billing — tránh dunning cost + dispute risk.

### 2.3 Currency

- **VND** mặc định cho mọi tenant Việt Nam (Luật Quản lý Thuế 2019 yêu cầu hóa đơn VAT bằng VND).
- **USD** chỉ available cho ENTERPRISE tier với international tenant (P9 International School). Hóa đơn vẫn issue VND nếu tenant có MST Việt Nam; USD invoice chỉ issue cho foreign-entity tenant (export of services — xem §3.3).
- Currency conversion (USD ↔ VND) — xem §9.

### 2.4 Payment methods per tier

Xem **Payment Method Matrix** §2.6 dưới.

### 2.5 Auto-renewal vs manual

- **Auto-renewal mặc định** cho BASIC/PREMIUM monthly. Tenant có thể opt-out qua dashboard.
- **Manual renewal** mặc định cho ENTERPRISE annual + multi-year contract (xem §8.3). Notice 30 days trước expiry (Sales-led).
- **Auto-renewal disclosure:** PHẢI hiển thị rõ tại checkout + email confirmation (Luật Bảo vệ Quyền lợi Người tiêu dùng 2023 yêu cầu transparency cho recurring charge). <!-- Phase 2: auto-renewal disclosure UI copy + opt-out flow — informed gut Q3 2026, GAP-154 -->

### 2.6 Payment Method Matrix

Bảng dưới là **placeholder Phase 1** — Phase 2 sẽ confirm processor fees với mỗi gateway sau khi negotiate rate.

| Tier | VNPay | MoMo | ZaloPay | Bank transfer (NAPAS) | Credit card (Stripe/Onepay) | PO + Bank transfer |
|------|:-----:|:----:|:-------:|:---------------------:|:---------------------------:|:------------------:|
| FREE | — | — | — | — | — | — |
| BASIC | ✅ TODO ~1.5% | ✅ TODO ~1.5% | ✅ TODO ~1.5% | ✅ free | ✅ TODO ~3.5% | ❌ |
| PREMIUM | ✅ TODO ~1.5% | ✅ TODO ~1.5% | ✅ TODO ~1.5% | ✅ free | ✅ TODO ~3.5% | ❌ |
| ENTERPRISE | ✅ | ✅ | ✅ | ✅ free | ✅ | ✅ Net-30/60 (xem §8) |

<!-- Phase 2: confirm processor fees per gateway — informed gut Q3 2026, GAP-154 -->

**Anti-pattern:** KHÔNG hardcode payment processor fee % trong invoice display logic. Externalize qua config keys `payment.processor.{vnpay,momo,...}.fee-pct` (GAP-108 tracking).

---

## 3. Tax Treatment

### 3.1 VAT rate

- **10% standard rate** áp dụng cho hầu hết SaaS service per Luật Thuế GTGT 2008 + amendments.
- **5% reduced rate** cần phân tích Phase 2 — VAT exemption khả năng áp dụng cho **education service** (per Luật Thuế GTGT Art 5 — "dịch vụ dạy học, dạy nghề" có thể exempt). KiteClass core (school operations) khả năng exempt; KiteHub (SaaS platform billing) khả năng phải chịu 10%. <!-- Phase 2: VAT exemption analysis — Tax advisor sign-off required, GAP-154 + GAP-156 -->

### 3.2 VAT invoice (e-invoice per TT 78/2021)

Mọi tenant Việt Nam có MST PHẢI nhận hóa đơn điện tử (e-invoice) theo **Circular 78/2021/TT-BTC** (mandatory từ 2022-07-01 cho tất cả doanh nghiệp).

Format:
- XML schema theo Tổng Cục Thuế (TCT) chuẩn
- Digital signature từ TCT-registered CA provider
- Issuance qua TCT-registered e-invoice provider (VNPT, Viettel, Misa, EFY, hoặc tương đương) <!-- Phase 2: e-invoice provider selection — Finance + IT Lead, GAP-154 -->
- Mã số hóa đơn (invoice number) tuân thủ Decree 123/2020/NĐ-CP định dạng

Chi tiết — xem §4 Invoicing.

### 3.3 Foreign tenant handling (export of services)

Tenant entity nước ngoài (không có MST Việt Nam) được xử lý theo "export of services" rule:
- VAT 0% áp dụng (per Luật Thuế GTGT Art 8 — service exports)
- Yêu cầu evidence: contract + payment confirmation từ foreign bank + proof of service delivery to foreign entity
- Hóa đơn vẫn issue qua TCT system với VAT rate 0%
- USD billing được phép

<!-- Phase 2: export-of-services workflow + evidence collection — Tax advisor sign-off, GAP-154 -->

### 3.4 Tax holiday / startup incentives

KiteHub/KiteClass có thể đủ điều kiện:
- **High-tech startup incentives** (Nghị định 13/2019/NĐ-CP) — corporate income tax giảm/exempt giai đoạn đầu
- **Software service incentives** — VAT 5% nếu được công nhận là sản phẩm phần mềm (Luật CNTT 2006 + amendments)

<!-- Phase 2: startup incentive eligibility assessment — Tax advisor + Legal counsel, GAP-154 -->

---

## 4. Invoicing

### 4.1 e-Invoice format (XML schema per TCT)

Hóa đơn điện tử PHẢI tuân thủ:
- **Decree 123/2020/NĐ-CP** Art 10 — invoice content (seller, buyer, item, VAT, total)
- **Circular 78/2021/TT-BTC** Art 4 — XML schema (TCT publishes XSD)
- Mã số hóa đơn (invoice serial + number) duy nhất, không trùng

Provider integration (Phase 2): VNPT-Invoice, Viettel S-Invoice, Misa MeInvoice, EFY-eINVOICE — chọn 1 sau RFP. <!-- Phase 2: e-invoice provider RFP + selection — Finance + IT Lead, GAP-154 -->

### 4.2 Digital signature

CA provider tích hợp với e-invoice provider:
- VNPT-CA, Viettel-CA, BKAV-CA, FPT-CA — tùy provider
- Token-based hoặc cloud-based signature (USB token vs HSM)
- Signature applied trước khi submit lên TCT system

### 4.3 Issuance timing

- **Charge upfront tier (BASIC/PREMIUM):** issue hóa đơn ngay sau payment confirmation (within 24h per Decree 123 Art 9)
- **Net-30 enterprise (PO):** issue hóa đơn at service commencement (start of period), payment due Net-30
- **Per-enrollment:** issue hóa đơn cuối tháng aggregating tất cả enrollments tháng đó

Decree 123/2020/NĐ-CP Art 9 yêu cầu issuance tại thời điểm chuyển giao dịch vụ — trễ nhất 7 ngày sau khi cung cấp dịch vụ trọn vẹn.

### 4.4 Tenant tax info required (MST — Mã Số Thuế)

Tenant Việt Nam muốn nhận VAT invoice PHẢI cung cấp:
- **MST** (Mã Số Thuế — 10 hoặc 13 ký tự)
- Tên doanh nghiệp đăng ký với cơ quan thuế
- Địa chỉ đăng ký
- Email + người liên hệ tài chính

Tenant cá nhân (không doanh nghiệp): có thể nhận hóa đơn cá nhân với CMND/CCCD nhưng không được khấu trừ VAT (Decree 123/2020/NĐ-CP Art 4).

<!-- Phase 2: MST collection UI flow at signup + checkout + dashboard — separate frontend gap (out of scope GAP-185), GAP-154 -->

### 4.5 Credit note / adjustment invoice process

Khi cần điều chỉnh hóa đơn đã phát hành (refund per GAP-183, dispute resolution, billing error):
- **Credit note (hóa đơn điều chỉnh giảm)** issue qua TCT system theo Decree 123/2020/NĐ-CP Art 19
- Reference invoice number gốc + lý do điều chỉnh
- Submit lên TCT trước cuối kỳ kê khai
- Tenant nhận credit note + refund processing (xem §7 + GAP-183)

---

## 5. Late Payment

### 5.1 Grace period

Khoảng thời gian từ due date đến bắt đầu áp dụng late fee:
- **Default: TODO 7 ngày** <!-- Phase 2: grace period — informed gut Q3 2026, GAP-154 -->
- ENTERPRISE Net-30: grace period thêm 14 days post-due (Sales-led negotiation)

Config key planned: `late-fee.grace-days` (GAP-108 externalization).

### 5.2 Late fee calculation

**Hiện trạng (anti-pattern):** `LATE_FEE_RATE = 0.001` hardcoded — vi phạm `business-logic-review.md` 5-attribute requirement. GAP-108 tracking externalization.

Công thức skeleton:
```
late_fee = outstanding_amount × late_fee.rate × overdue_days
```

Config keys planned (GAP-108):
- `late-fee.rate` — daily rate (default placeholder 0.001 = 0.1%/day; **Phase 2 Tax + Legal review** để align với Commercial Law 2005 max 150% of base interest rate)
- `late-fee.grace-days` — default 7
- `late-fee.max-cap-pct` — late fee không vượt quá X% of outstanding (default placeholder 30%)
- `late-fee.compounding` — boolean, simple vs compound (default simple)

<!-- Phase 2: late fee rate + cap + compounding rules — Legal counsel sign-off (Commercial Law 2005 max rate compliance), GAP-154 + GAP-108 -->

Xem **Late Fee Calculation Examples** §5.6 dưới.

### 5.3 Service suspension trigger

| Days overdue | Action |
|--------------|--------|
| 0 → grace_period (7d) | Soft reminder email Day 1 + Day 5 |
| grace_period + 1 → +14d | Late fee starts accruing; warning email Day 8 + Day 14 |
| +15d → +30d | **Suspend service** (read-only mode); CTA upgrade/pay |
| +31d → +60d | **Lockout** (no read access); manual recovery only |
| +61d → +90d | **Termination notice** (30 days advance per TOS GAP-180) |
| +91d+ | **Account terminated** + write-off process (xem §5.5) |

<!-- Phase 2: confirm thresholds with Customer Success + Finance — informed gut Q3 2026, GAP-154 -->

### 5.4 Reactivation process

Tenant pay outstanding (principal + late fee) → service reactivation within 1 business day:
- Auto-reactivate BASIC/PREMIUM upon payment confirmation
- ENTERPRISE: manual reactivation by CS (verify PO + payment received)
- Reactivation fee: TODO N/A (default no fee) <!-- Phase 2: reactivation fee policy — informed gut Q3 2026, GAP-154 -->

### 5.5 Write-off criteria

Sau 90 days overdue + termination, account moves to write-off:
- Outstanding amount written off as bad debt (accounting per VAS 18 / IFRS 9)
- Tenant data follow `data-retention-deletion-policy.md` (GAP-184) post-termination retention
- Legal collection action threshold: outstanding > TODO 50M VND (cost-benefit analysis) <!-- Phase 2: collection action threshold — Legal counsel sign-off, GAP-154 -->

### 5.6 Late Fee Calculation Examples

Bảng dưới là **placeholder Phase 1** dùng config key `late-fee.rate = 0.001` (0.1%/day, GAP-108 hiện trạng) và `late-fee.grace-days = 7`. Phase 2 sẽ confirm rate/cap với Legal counsel.

| Outstanding | Days overdue (after grace) | Daily rate | Accrued late fee | Capped at 30% of outstanding | Total due |
|------------:|:--------------------------:|:----------:|-----------------:|:----------------------------:|----------:|
| 500,000 VND | 5 | 0.001 | 2,500 VND | 150,000 VND cap | 502,500 VND |
| 500,000 VND | 30 | 0.001 | 15,000 VND | 150,000 VND cap | 515,000 VND |
| 500,000 VND | 365 | 0.001 | 182,500 VND | **150,000 VND cap** (hit) | 650,000 VND |
| 5,000,000 VND | 60 | 0.001 | 300,000 VND | 1,500,000 VND cap | 5,300,000 VND |
| 50,000,000 VND | 90 | 0.001 | 4,500,000 VND | 15,000,000 VND cap | 54,500,000 VND |

<!-- Phase 2: confirm cap + compounding + Commercial Law 2005 rate compliance — Legal counsel + Tax advisor, GAP-154 + GAP-108 -->

**Anti-pattern reminder:** mọi giá trị trong cột "Daily rate", "Cap %" PHẢI đến từ `application.yml` config keys, không hardcode. GAP-108 theo dõi.

---

## 6. Pricing Changes

### 6.1 Notice period

- **Default: TODO 30-60 ngày** trước khi price change effective <!-- Phase 2: notice period — Legal counsel align với Consumer Protection Law 2023, GAP-154 -->
- Notice qua: in-app banner + email (mọi billing contact) + dashboard banner sticky
- ENTERPRISE multi-year contract: price locked qua contract term (xem §8.3) — change chỉ apply at renewal

### 6.2 Grandfathering

Tenant active tại thời điểm price change announcement có quyền:
- Giữ giá cũ đến hết current billing period
- Annual subscriber: giữ giá cũ đến hết annual term
- Multi-year ENTERPRISE: giữ giá theo contract

Grandfathering KHÔNG áp dụng nếu tenant downgrade hoặc add-on (giá add-on = giá mới).

### 6.3 Mid-term price change prohibition

KHÔNG được tăng giá giữa kỳ billing đã pay (per Luật Bảo vệ Quyền lợi Người tiêu dùng 2023 — không được thay đổi điều khoản đã ký).

Trường hợp đặc biệt:
- VAT rate change do quy định pháp luật → mid-term adjustment được phép (compliance-driven, không phải commercial decision)
- Currency conversion drift > X% (USD ↔ VND) — chỉ áp dụng cho USD invoice, threshold TODO <!-- Phase 2: currency drift threshold — Finance, GAP-154 -->

---

## 7. Promotions + Discounts

### 7.1 Discount types

- **Percentage discount:** "Giảm 20% 3 tháng đầu" (capped at 50% off)
- **Fixed discount:** "Giảm 100K VND tháng đầu"
- **Free months:** "Annual subscription được tặng 2 tháng" (= ~17% effective discount)
- **Tier upgrade trial:** BASIC tenant trial PREMIUM 14 days free

### 7.2 Referral credits

- Existing tenant refer new tenant → cả 2 nhận credit:
  - Referrer: TODO 1 tháng free hoặc 200K VND credit <!-- Phase 2: referral economics — Finance + Marketing, GAP-154 -->
  - Referee: TODO 20% off tháng đầu
- Credit auto-apply at next invoice
- Anti-fraud: max TODO 10 referrals/tenant/year

### 7.3 Educational discounts (registered schools)

Tenant là trường học chính thức (có giấy phép MoET, mã số trường) đủ điều kiện:
- Discount permanent TODO 20% on PREMIUM/ENTERPRISE
- Verification: upload giấy phép kinh doanh / quyết định thành lập trường <!-- Phase 2: educational verification flow — separate frontend gap, GAP-154 -->
- KHÔNG stack với promotional discount

### 7.4 Promotion terms

Mọi promotion PHẢI hiển thị rõ:
- Giá trước + sau discount (transparency per Consumer Protection Law 2023)
- Thời hạn promotion (start + end date)
- Điều kiện áp dụng (new tenant only? BASIC+ only?)
- Auto-cancellation rule (recurring promo expires after N billing cycles)

<!-- Phase 2: promotion approval workflow + caps — Marketing + Finance + Legal, GAP-154 -->

---

## 8. Refunds

Toàn bộ refund logic — bao gồm money-back guarantee, dispute resolution, prorated refund, chargeback handling — được định nghĩa tại **[refund-dispute-resolution-policy.md](refund-dispute-resolution-policy.md)** (GAP-183, sibling Agent A this wave).

Tóm tắt cross-link:
- Refund window per tier (xem GAP-183 §refund-window)
- Refund calculation basis (full vs prorated) ties với invoice issued tại §4 ở đây
- Credit note issuance per §4.5 ở đây
- Dispute resolution timeline (xem GAP-183 §dispute-resolution)
- Chargeback handling (xem GAP-183 §chargeback)

Khi tenant refund: hóa đơn gốc KHÔNG xóa (per Decree 123/2020/NĐ-CP), thay vào đó issue credit note (hóa đơn điều chỉnh giảm) — xem §4.5.

---

## 9. Enterprise Payment

### 9.1 PO process (Purchase Order)

ENTERPRISE tenant có thể procure qua PO:
- Sales team negotiate quote (custom pricing per `pricing-model.md`)
- Tenant issue PO với amount + terms (Net-30/60)
- KiteHub raise sales order → service provisioning starts at PO acceptance
- Hóa đơn issue tại service commencement (per §4.3) với reference PO number
- Payment due Net-30 hoặc Net-60 sau invoice date

<!-- Phase 2: PO workflow + sales order template + ERP integration — Finance + Sales, GAP-154 -->

### 9.2 Annual prepay discount

ENTERPRISE annual prepay nhận:
- Discount TODO 15-25% so với monthly (xem `pricing-model.md` §2)
- Locked-in price suốt 12 tháng (no mid-term increase per §6.3)
- Single annual invoice (vs 12 monthly invoices) — giảm AP overhead cho tenant

### 9.3 Multi-year contracts

Contract 2-3 năm:
- Discount stack với annual prepay (TODO additional 5-10% per year extension)
- Price escalation clause: TODO max 5%/year để align inflation <!-- Phase 2: escalation clause — Legal counsel, GAP-154 -->
- Termination for convenience: TODO 90 days notice + early termination fee <!-- Phase 2: ETF formula — Legal counsel, GAP-154 -->

### 9.4 Net-30 / Net-60 terms

Default Net-30 cho ENTERPRISE; Net-60 negotiable cho large account (TODO threshold > 1B VND/year).

Late payment trên Net-30/60: late fee + suspension threshold reset relative to Net term (Net-30 + grace 7 = total 37 days before late fee starts).

---

## 10. Currency Conversion (international tenants)

International tenant (P9) billed USD nhưng KiteHub operations + tax filing bằng VND:

### 10.1 Exchange rate timing

- **Spot rate at invoice issuance** dùng cho USD-billed invoice (Vietcombank trung tâm rate ngày issue)
- Conversion locked at issue time — không adjust khi rate biến động
- Hóa đơn VAT (nếu áp dụng cho tenant có MST VN) issue VND amount converted at issue rate; USD amount ghi reference

### 10.2 Conversion fees

- Bank conversion fee (incoming USD → VND): pass-through (tenant chịu) hoặc absorb (KiteHub chịu) <!-- Phase 2: conversion fee policy — Finance, GAP-154 -->
- Stripe/Wise/PingPong fee: ~1-2.5% — included in pricing markup hoặc invoice line item separate

### 10.3 Exchange rate disclosure

PHẢI disclose tại checkout + invoice:
- Source rate (Vietcombank trung tâm)
- Conversion timestamp
- Final VND amount equivalent
- Any conversion fee separately

(Per Luật Bảo vệ Quyền lợi Người tiêu dùng 2023 transparency.)

---

## 11. Tax Calculation Examples

Bảng dưới là **placeholder Phase 1** với VAT 10% standard rate. Phase 2 confirm 5% education exemption applicability (xem §3.1).

### 11.1 VAT-exclusive pricing (tenant với MST, B2B)

Giá hiển thị KHÔNG bao gồm VAT; VAT thêm vào tại checkout.

| Plan | List price (VAT-exclusive) | VAT 10% | Total (VAT-inclusive) | Notes |
|------|-------------------------:|--------:|----------------------:|-------|
| BASIC monthly | TODO 500,000 VND | 50,000 VND | 550,000 VND | MST tenant deducts 50K input VAT |
| PREMIUM monthly | TODO 2,000,000 VND | 200,000 VND | 2,200,000 VND | |
| ENTERPRISE annual | TODO 50,000,000 VND | 5,000,000 VND | 55,000,000 VND | Single annual invoice |
| PREMIUM annual (20% disc) | TODO 19,200,000 VND | 1,920,000 VND | 21,120,000 VND | Annual prepay with 20% discount |

### 11.2 VAT-inclusive pricing (tenant cá nhân, không MST, B2C)

Giá hiển thị bao gồm VAT (per Consumer Protection Law 2023 transparency cho cá nhân).

| Plan | List price (VAT-inclusive) | VAT 10% backed-out | Net price | Notes |
|------|-------------------------:|-------------------:|----------:|-------|
| BASIC monthly | TODO 550,000 VND | 50,000 VND | 500,000 VND | Cá nhân không deduct VAT |
| PREMIUM monthly | TODO 2,200,000 VND | 200,000 VND | 2,000,000 VND | |

Backed-out formula: `vat = price × 10 / 110` (price đã VAT-inclusive).

### 11.3 Foreign tenant (export of services, VAT 0%)

| Plan | USD price | VAT 0% | Total USD | VND equivalent (rate 25,000) |
|------|----------:|-------:|----------:|-----------------------------:|
| ENTERPRISE annual | TODO $2,000 | $0 | $2,000 | 50,000,000 VND |

<!-- Phase 2: confirm VAT exemption applicability for education category — Tax advisor, GAP-154 -->

**Anti-pattern reminder:** mọi VAT rate trong invoice generation logic PHẢI đọc từ `application.yml` (`tax.vat.rate.standard = 0.10`, `tax.vat.rate.education = 0.05`, `tax.vat.rate.export = 0.00`), KHÔNG hardcode trong service code. GAP-108 tracking.

---

## 12. Open Items / Phase 2 Deliverables (GAP-154 + GAP-108)

| Item | Section ref | Owner | Phase 2 deliverable |
|------|-------------|-------|---------------------|
| VAT 5% education exemption analysis | §3.1, §11 | Tax advisor + Legal | Eligibility memo + ruling request to TCT |
| TCT e-invoice provider RFP + selection | §3.2, §4.1 | Finance + IT Lead | Vendor signed (VNPT/Viettel/Misa/EFY) |
| Late fee rate + cap + compounding | §5.2, §5.6 | Legal counsel | Commercial Law 2005 compliant rate + config externalization (GAP-108) |
| Grace period | §5.1 | Finance + CS | Number confirmed + config externalization (GAP-108) |
| Notice period for pricing changes | §6.1 | Legal counsel | Consumer Protection Law 2023 alignment |
| MST collection UI flow | §4.4 | Frontend (separate gap) | Signup + dashboard MST capture flow |
| Invoice numbering format | §4.1 | Finance | TCT-compliant format per Decree 123 Art 4 |
| Auto-renewal disclosure UI copy | §2.5 | Legal + Marketing | Consumer Protection Law 2023 compliance |
| Late fee config externalization | §5.2, §5.6, §11 | Engineering | Close GAP-108 (move LATE_FEE_RATE → application.yml) |
| Service suspension thresholds | §5.3 | Customer Success + Finance | Confirmed threshold matrix |
| Educational verification flow | §7.3 | Frontend (separate gap) | School license upload + verification |
| Currency conversion fee policy | §10.2 | Finance | Pass-through vs absorb decision |
| Per-enrollment pricing model | §2.1 | Product + Finance | KiteClass tenant per-enrollment economics |
| Multi-year escalation clause + ETF | §9.3 | Legal counsel | Contract template clauses |
| Startup tax incentive eligibility | §3.4 | Tax advisor + Legal | Eligibility memo + filing if applicable |
| Refund + credit note workflow | §4.5, §8 | Cross-link với GAP-183 | Coordinate với Agent A refund-dispute-resolution-policy.md |

---

## 13. Out of scope (explicit)

- **TCT e-invoice integration code** — separate feature gap (provider SDK integration, Phase 2)
- **MST collection UI** — separate frontend gap
- **Accounting system integration** (Misa Amis, FAST, BRAVO) — enterprise feature, Phase 3+
- **PCI-DSS Level 1 self-assessment** — only required nếu KiteHub directly handle card data (currently relies on processor tokenization — Stripe/Onepay)
- **International tax (US sales tax, EU VAT MOSS)** — out of scope cho v1; KiteClass Việt Nam-first (per `compliance-scope.md`)
- **Cryptocurrency payment** — KHÔNG accept (regulatory risk per State Bank of Vietnam guidance)

---

## 14. Log

- **2026-04-29 (Phase 1 SKELETON):** Skeleton shipped Wave Legal-BRD Phase 1.5 Agent B (GAP-185). 9 sections per §Scope: Payment Terms / Tax Treatment / Invoicing / Late Payment / Pricing Changes / Promotions+Discounts / Refunds (link → GAP-183) / Enterprise Payment / Currency Conversion. 3 tables: Payment Method Matrix (§2.6), Late Fee Calculation Examples (§5.6), Tax Calculation Examples (§11). Legal basis: Circular 78/2021/TT-BTC (e-invoice mandate), Decree 123/2020/NĐ-CP (10y retention), VAT Law 2008, Luật Quản lý Thuế 2019, Commercial Law 2005, Consumer Protection Law 2023. Cross-cuts wired: GAP-180 TOS, GAP-183 refund (Agent A sibling), GAP-182 privacy (PDPL payment data), GAP-184 retention (10y financial), GAP-108 (late fee + grace period config externalization), pricing-model.md tier alignment. Phase 2 deliverables tracked §12: VAT exemption analysis, e-invoice provider RFP, late fee Commercial Law 2005 compliance, MST collection UI flow, multi-year contract clauses — all under GAP-154 umbrella + GAP-108 config drift root cause. **Status: 🔵 SKELETON — placeholder values throughout; legal counsel + Tax advisor + TCT-registered e-invoice provider sign-off mandatory before activation.** Reviewer: @nguyenvankiet (acting Legal scout + Finance + Compliance, solo-dev, 2026-04-29). Formal Legal counsel + Tax advisor + Finance Lead review queued — GAP-154 + GAP-156 acceptance criteria.
