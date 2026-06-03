# GAP-884: `version` column missing DEFAULT 0 on multiple tables — NPE risk seed/raw INSERT

**Status:** 🔵 OPEN
**Priority:** 🟡 P2
**Domain:** Backend / DB
**Found:** 2026-06-03 (Wave 13 cluster docs writing — KC finance + KC branding)
**Affects:** `invoices`, `payments`, `payment_records`, `landing_pages` — not in V62/V63 sweep

## Problem

V62/V63 set `version` DEFAULT 0 cho 19+ bảng nhưng bỏ sót:

- KC `invoices.version` (V26) + `payments.version` (V26) — không default
- KC `payment_records.version` (V69) — không default
- KC branding `landing_pages.version` (V75 nullable, no default)

Raw INSERT (seed/test fixture/migration script) không bind `version` → NULL → JPA `@Version` NPE ở flush. Service path qua JPA bind default OK; rủi ro thực tế thấp nhưng bất nhất với 19 bảng đã chuẩn hóa.

## Proposed Fix

Migration ALTER COLUMN ... SET DEFAULT 0 + backfill NULL→0 cho 4 bảng. Add to V62/V63 follow-up.

## Acceptance Criteria

- [ ] Migration V## SET DEFAULT 0 cho 4 bảng + backfill
- [ ] Reference cluster doc 04-finance §A7 + 08-branding-marketing §A3

## Discovered in

KC `04-finance.md` §A7 + `08-branding-marketing.md` §A3
