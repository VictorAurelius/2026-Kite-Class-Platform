---
audience: dev
domain: seed
layer: 1-rules
last-updated: 2026-05-18
version: 1.0
---

# Seed Worker — Layer 1: Business Rules

**Domain:** Sample/demo data seeding cho default tenant onboarding (KiteHub instance).
**Related:** GAP-538 (onboarding checklist sample-data seed) — GAP-658 (VN sample seed worker, Wave 98 Bucket B2).
**Rules referenced:** `user-manual-content-standard.md` §2 row 7 (VN sample data mandate), `dev-readable-doc-language.md` §2/§3.

---

## BR-SEED-001 — Locale-driven sample data source

Seed worker PHẢI dùng VN-friendly sample data làm default khi seed default tenant (P2 Center Owner persona walkthrough — chị Hằng, Q.1 TP.HCM English center).

**Rationale:** First-touch trust signal. English Lorem-Ipsum placeholders (`John Doe`, `Class A1`, `Example Center`, `$60.00`, `Mon May 14, 2026`) trigger bounce: "đây là phần mềm Mỹ, không phù hợp VN".

**Config key:** `seed.locale`
- Default: `vi-VN`
- Allowed values: `vi-VN` | `en-US`
- Source: `application.yml` per env

**Acceptance:** mọi sample row trong default seed phải khớp VN convention (xem BR-SEED-002..BR-SEED-007).

---

## BR-SEED-002 — Student names VN-friendly

Sample student names PHẢI:
- Sử dụng real Vietnamese names (Trần/Nguyễn/Lê/Phạm/Hoàng/Vũ/Đỗ family names)
- Bao gồm diacritics (Hồng / Mai / Hương — KHÔNG strip thành Hong/Mai/Huong)
- Cân bằng regional (Bắc/Trung/Nam ~ 1/3 each)
- Cân bằng gender (F/M)
- Tối thiểu 300 distinct rows trong pool

**Source:** `kitehub/kitehub-platform/src/main/resources/seed-data/vn-friendly/student-names.csv`
**Schema:** `first_name,last_name,full_name,gender,region`

---

## BR-SEED-003 — Teacher names VN-friendly với specialty

Sample teacher names PHẢI:
- Tuân BR-SEED-002 (real VN names + diacritics + regional)
- Đính kèm specialty: `Anh ngữ | Toán | Lý | Hóa | Văn | Sử | Địa | Tin học | Sinh học | Tiếng Nhật | Tiếng Hàn | Tiếng Trung`
- Tối thiểu 100 rows

**Source:** `seed-data/vn-friendly/teacher-names.csv`
**Schema:** `first_name,last_name,full_name,gender,region,specialty`

---

## BR-SEED-004 — Class names theo VN edu convention

Sample class names PHẢI follow VN edu naming convention:
- Prefix `Lớp` (KHÔNG `Class`)
- Format `Lớp <subject> <grade><section>` (e.g., `Lớp Anh ngữ 5A1`, `Lớp Toán 9B`, `Lớp Tin học 11C`)
- Grade level 0-12 (0 = non-grade-bound classes như IELTS/TOEIC/N5)
- Tối thiểu 50 rows

**Source:** `seed-data/vn-friendly/class-names.csv`
**Schema:** `name,grade_level,subject`

---

## BR-SEED-005 — Center names theo VN edu pattern

Sample center names PHẢI:
- Prefix `Trung tâm` (KHÔNG `Center` hoặc `School`)
- Bao gồm subject hoặc focus area (e.g., `Trung tâm Anh ngữ Sky Education`, `Trung tâm Toán Quang Minh`)
- City field PHẢI là VN city/province (TP.HCM | Hà Nội | Đà Nẵng | Cần Thơ | Hải Phòng | Nha Trang | etc.)
- Tối thiểu 50 rows

**Source:** `seed-data/vn-friendly/center-names.csv`
**Schema:** `name,short_name,city`

---

## BR-SEED-006 — Addresses theo VN format

Sample addresses PHẢI:
- Real district names (Q.1 | Q.3 | Q.5 | Q.7 | Q.10 | Q.Bình Thạnh | Q.Phú Nhuận | Q.Tân Bình | Q.Gò Vấp | TP.Thủ Đức | Q.Hoàn Kiếm | Q.Hai Bà Trưng | Q.Cầu Giấy | Q.Đống Đa | Q.Tây Hồ | Q.Ba Đình | Q.Hà Đông | Q.Hải Châu | Q.Ngô Quyền | etc.)
- Real Vietnamese street names (Lê Lợi | Nguyễn Huệ | Hai Bà Trưng | Trần Hưng Đạo | etc.)
- Format: `<street_number> <street_name>` (e.g., `123 Lê Lợi`)
- Distribution across major cities (TP.HCM weighted 40%, Hà Nội 30%, Đà Nẵng 15%, others 15%)
- Tối thiểu 100 rows

**Source:** `seed-data/vn-friendly/addresses.csv`
**Schema:** `street,district,city`

---

## BR-SEED-007 — Subject names mixed VN + EN abbreviation

Sample subject pool PHẢI:
- Name = Vietnamese (`Anh ngữ`, `Toán`, `Lý`, `Hóa`, `Văn`)
- Abbreviation = English code (`EN`, `MATH`, `PHY`, `CHEM`, `LIT`) — cross-locale stable per `dev-readable-doc-language.md` §3
- Tối thiểu 30 rows

**Source:** `seed-data/vn-friendly/subject-names.csv`
**Schema:** `name,abbreviation`

---

## BR-SEED-008 — Currency formatting VND

Tất cả currency values trong sample seed PHẢI dùng `VietnamSampleDataGenerator.formatVND(BigDecimal)`:
- Output format: `1.500.000 ₫` hoặc `1.500.000đ` (per JDK Locale data)
- BANNED: `$60.00`, `60 USD`, `60.00 USD`
- Locale: `Locale.forLanguageTag("vi-VN")`

---

## BR-SEED-009 — Date formatting VN convention

Tất cả date display trong sample seed PHẢI dùng `VietnamSampleDataGenerator.formatVNDate(LocalDate)`:
- Output format: `Thứ Hai, 14/05/2026` (day-of-week + dd/MM/yyyy)
- BANNED: `Mon May 14, 2026`, `2026-05-14` (ISO ok trong code/frontmatter only, không user-facing)
- Pattern: `EEEE, dd/MM/yyyy` với `Locale.forLanguageTag("vi-VN")`

---

## BR-SEED-010 — Time formatting 24-hour

Tất cả time display trong sample seed PHẢI dùng `VietnamSampleDataGenerator.formatVNTime(LocalTime)`:
- Output format: `09:30`, `14:00` (24-hour `HH:mm`)
- Acceptable narrative: `9 giờ 30 sáng` (manual conversion for natural prose)
- BANNED: `9:30 AM`, `2:00 PM`

---

## Config keys (canonical reference)

| Key | Default | Description |
|---|---|---|
| `seed.locale` | `vi-VN` | Sample data source locale (`vi-VN` \| `en-US`) |

---

## Override mechanism

Khi test fixture cần English placeholders (cross-locale test coverage):

```yaml
# kitehub-platform/src/test/resources/application-test.yml
seed:
  locale: en-US
```

Locale=`en-US` returns placeholder constants (`Student Sample`, `Example Center`, `Class A1`, etc.) cho test compat.

---

## Five-attribute review per `business-logic-review.md` §2

Seed-worker rule values (VN-name pools, class/center naming, VND/date/time formats) govern **synthetic sample/demo data** for default-tenant onboarding — a localization/engineering fixture, not a production business rule. No real personal data.

- **Source:** Informed gut + VN edu localization norms — `user-manual-content-standard.md` §2 row 7 (VN sample-data mandate) + `dev-readable-doc-language.md` §2/§3 + P2 Center Owner persona walkthrough (chị Hằng, Q.1 TP.HCM). Wave 98 Bucket B2 (GAP-658, closes GAP-538 AC7).
- **Rationale:** First-touch trust signal — English Lorem-Ipsum placeholders (`John Doe`, `$60.00`, `Mon May 14`) trigger bounce ("đây là phần mềm Mỹ"). VN-friendly sample data (real diacritic names, `Trung tâm`/`Lớp` prefixes, `1.500.000đ`, `Thứ Hai, 14/05/2026`) signals a VN-built product. `en-US` override retained for cross-locale test fixtures only.
- **Reviewer:** @nguyenvankiet (acting Tech Lead, solo-dev, 2026-06-21). VN-localization correctness benefits from a native-VN reviewer (deferred, paired Wave 98 Bucket B4 i18n). No business-value sign-off required — synthetic fixture. Review queued — GAP-156 AC-D.
- **Compliance check:** **N/A** — synthetic/sample seed data only (no real student/parent/teacher PII; production tenants generate their own data). Dev fixture, not a production data-processing rule, so no PDPL/tax trigger per `documents/00-brd/compliance-checklist.md`.
- **Review cadence:** **Annual** + event-driven when VN sample pools expand or a new locale is added. **Next review:** 2026-09-21 (next audit checkpoint), then Annual.

---

## Log

- **2026-05-18 (v1.0):** Domain `seed/` created Wave 98 Bucket B2 per GAP-658. Closes GAP-538 AC7 (VN sample seed worker). 10 business rules codify VN-friendly sample data mandate per `user-manual-content-standard.md` §2 row 7 + `dev-readable-doc-language.md` §2/§3. 6 CSV sources shipped trong `seed-data/vn-friendly/`. `VietnamSampleDataGenerator` Spring component implements rules. Native VN copywriter review pass deferred — paired với Wave 98 Bucket B4 i18n bucket (parallel execution).
