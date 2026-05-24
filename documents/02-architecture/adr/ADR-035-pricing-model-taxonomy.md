---
adr_number: 035
title: Pricing Model Taxonomy — PER_HOUR primary + MONTHLY + COURSE_PACKAGE + FREE
status: ACCEPTED
date: 2026-05-24
deciders: ["@nguyenvankiet"]
wave: beta-readiness-4
gap: GAP-292
supersedes: none
audience: mixed
---

# ADR-035: Pricing Model Taxonomy — PER_HOUR primary

> Note: Wave plan `wave-2026-05-24-beta-readiness-4-meta-pdpl-pricing-reschedule-tone.md` §3.6 originally referenced "ADR-027"; ADR-027 đã taken bởi statuspage-vendor decision. Bucket C ADR shipped với next sequential ADR-035 (canonical numbering per `meta-csv-index-pattern.md`).

## Status

ACCEPTED — 2026-05-24, Wave beta-readiness-4 Bucket C.

## Context

Bài toán: Trước Wave beta-readiness-4, Course entity có 1 field duy nhất `BigDecimal price` (legacy flat-fee). Field này KHÔNG capture được sự đa dạng mô hình tính học phí trong VN edu market — đặc biệt cho TT Anh ngữ + STEM dạy thêm dùng PER_HOUR là dominant pattern (Apollo English 257.000-344.000đ/giờ, ILA 195.000-368.000đ/giờ).

Phụ thuộc:
- Invoice generation (InvoiceService) cần biết model để tính amount đúng
- Tax compliance: Thông tư 78/2021/TT-BTC eInvoice requires `giờ` unit cho dịch vụ giáo dục
- Cross-bucket Bucket D reschedule: nếu session date mutate cross period boundary, PER_HOUR invoice period recalc cần thiết
- 3-layer business docs (rules.md / use-cases.md / api-contract.md) cần update đồng thời

PM scan trung tâm pilot KiteClass (2026-05-20, 3 trung tâm contacted):
- 3/4 confirm PER_HOUR là model primary
- 1/4 (TT âm nhạc trẻ em) dùng MONTHLY cho lớp Piano 4-6 tuổi
- 0/4 muốn HYBRID model (vd PER_HOUR + monthly cap)

## Decision

Adopt **4-enum PricingModel taxonomy** với PER_HOUR primary:

| Enum | Semantics | unit_price interpretation |
|------|-----------|----------------------------|
| `PER_HOUR` | Tính theo giờ học thực tế | đ/giờ |
| `MONTHLY` | Phí tháng cố định | đ/tháng |
| `COURSE_PACKAGE` | Trọn gói 1 lần | đ/khoá |
| `FREE` | Miễn phí | phải = 0 |

**Implementation:**
1. Course entity adds `pricing_model VARCHAR(30) NOT NULL` + `unit_price NUMERIC(19,2)`
2. Legacy `price BigDecimal` marked `@Deprecated(since="V67")` — preserved BUT new code MUST NOT write
3. Migration V67 ALTER TABLE + DEFAULT 'PER_HOUR' (per VN market norm); pre-migration audit script `scripts/audit-pre-pricing-model.sql` guides reclassification cho tenants existing với `price > 0`
4. `PricingCalculator` Spring component decouples math từ InvoiceService (future-proof for PER_SESSION addition)
5. CHECK constraints enforce: pricing_model ∈ whitelist; unit_price ≥ 0; FREE → unit_price = 0
6. Rollback path: `R67__undo_pricing_model.sql` manual execution

## Alternatives considered + Rejected

### Alt 1: PER_SESSION primary (rejected)

**Pros:** Buổi học là đơn vị tự nhiên user (phụ huynh) hiểu ("Lớp này còn 8 buổi").

**Cons:**
- VN TT Anh ngữ market benchmark: 100% major chains dùng PER_HOUR (Apollo/ILA/Yola/Wall Street). PER_SESSION không exist dưới dạng pricing tag (chỉ display tag).
- VAT eInvoice Thông tư 78/2021/TT-BTC sử dụng `giờ` chuẩn cho dịch vụ giáo dục — PER_SESSION không match tax unit.
- PER_SESSION derivable = `PER_HOUR × session_duration_hours` — không cần enum riêng.

**Verdict:** ❌ Rejected — VN market + tax compliance đều favor PER_HOUR.

### Alt 2: 5-enum hybrid (PER_HOUR + PER_SESSION + MONTHLY + COURSE_PACKAGE + FREE) (rejected)

**Pros:** Cover thêm 1 use case (trung tâm muốn tag explicit PER_SESSION).

**Cons:**
- Cognitive load tăng cho UI (radio 5 option vs 4)
- PER_SESSION derivable từ PER_HOUR (không add value functionally)
- Đa enum → ambiguity InvoiceGenerationService logic (more branches to test)

**Verdict:** ❌ Rejected — YAGNI (per `design-patterns.md` §1.1).

### Alt 3: Defer pricing taxonomy → Wave 5+ (rejected)

**Pros:** Tránh migration risk, ship Bucket C nhanh hơn.

**Cons:**
- BLOCKS Phase 1 BETA gate — Phase 1 personas (P2 Center Owner) cần đúng pricing model cho invoice (current `price` flat-fee gây miscalculation cho TT Anh ngữ dùng giờ)
- Compounds technical debt — mỗi sprint thêm enrollment + invoice = thêm legacy `price` data
- PER_HOUR pivot decision (vs PER_SESSION) đã made; defer chỉ delay implementation

**Verdict:** ❌ Rejected — Phase 1 BETA blocker.

## Consequences

### Positive

- VN TT Anh ngữ market fit ngay từ Phase 1 BETA
- VAT eInvoice compliance ready (Thông tư 78/2021 unit `giờ`)
- PricingCalculator extensibility (Phase 2+ có thể add PER_SESSION enum nếu market signal change)
- 5-attribute business rules documented (BR-COURSE-PRICING-001..004)
- Cross-bucket Bucket D edge case (reschedule period recalc) explicit per BR-COURSE-PRICING-004

### Negative

- Migration risk: existing Course.price data cần reclassify thủ công (3-5 tenants currently pilot) — pre-migration audit script + email runbook mitigate
- Legacy `price` column preserved trong DB schema → minor schema bloat (cleanup deferred V70+ post production validation)
- Pricing model immutability post-enrollment (BR-COURSE-PRICING-003) hạn chế flexibility — accepted tradeoff để avoid bait-and-switch disputes

### Neutral

- FE form radio "Hình thức tính học phí" mới — 4 option, intuitive (deferred Phase 1.5+ form integration; types + labels shipped Bucket C)
- Invoice display amount unchanged user-facing (vẫn hiển thị VND total)

## Compliance

- **Thông tư 78/2021/TT-BTC (eInvoice):** ✅ PER_HOUR unit `giờ` đáp ứng tax reporting
- **Luật Bảo vệ Quyền lợi Người tiêu dùng:** ✅ Pricing immutability post-enrollment (BR-COURSE-PRICING-003) prevents bait-and-switch
- **PDPL 2023:** N/A — pricing data không PII
- **VN edu market norm:** ✅ Match Apollo/ILA dominant pattern

## References

- **Wave plan:** `documents/03-planning/waves/wave-2026-05-24-beta-readiness-4-meta-pdpl-pricing-reschedule-tone.md` §3.3 Bucket C
- **Gap:** `documents/04-quality/gaps/GAP-292-pricing-model-taxonomy.md`
- **Business rules:** `documents/01-business/kiteclass/course-class/rules.md` §6
- **API contract:** `documents/01-business/kiteclass/course-class/api-contract.md` §Pricing
- **Migration:** V67 (add columns), R67 (rollback), V69 (payment_records)
- **Code:** `PricingModel.java`, `PricingCalculator.java`, `Course.java` (extended)
- **Pre-migration audit:** `scripts/audit-pre-pricing-model.sql`
- **VN market benchmark sources:** Apollo English pricing 2024; ILA Vietnam pricing 2024; PM scan 3 TT pilot 2026-05-20 (internal notes)

## Log

- **2026-05-24:** ADR created và ACCEPTED. Reviewer: @nguyenvankiet (solo-dev acting Product Owner + Tech Lead). Self-approve per `business-logic-review.md` §2.3 solo-dev exemption; formal PM consultant sign-off queued Phase 1.5+ per GAP-156.
