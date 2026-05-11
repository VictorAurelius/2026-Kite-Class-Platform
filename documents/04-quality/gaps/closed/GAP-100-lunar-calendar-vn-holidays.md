# GAP-100: Lunar Calendar for Vietnamese Holidays

**Status:** 🟢 DONE (2026-04-18, PR #353)
**Priority:** 🟢 P3
**Domain:** KiteClass Core / Academic Year
**Found:** 2026-04-18 (TODO audit post Wave 4)
**Affects:** Academic year holiday calendar accuracy for Tết, Giỗ tổ, etc.

## Problem

`kiteclass/kiteclass-core/src/main/java/com/kiteclass/core/module/academicyear/service/VnHolidayProvider.java`:

Lines 30, 42, 84:
- Line 30: `// For MVP, uses approximate dates (TODO: lunar calc)`
- Line 42: `// TODO: Use lunar calendar library for accuracy`
- Line 84: `// TODO: Replace với proper lunar calendar lookup.`

Current implementation hardcodes Solar-calendar approximate dates for:
- Tết Nguyên Đán (Lunar New Year) — varies Jan 21 to Feb 20
- Giỗ tổ Hùng Vương (10th day of 3rd lunar month) — varies Mar-Apr
- Tết Trung Thu (15th day of 8th lunar month) — varies Sep-Oct

**Accuracy drift per year:** ±7-14 days. MVP uses fixed Feb 10 for Tết — wrong for most years.

## Impact

- Wrong holiday dates auto-scheduled → attendance false absences
- Report cards generated during Tết include exam days that don't exist
- Parent notifications sent on holidays
- Academic year boundaries miscalculated for K-12 semester

## Proposed Fix

**Option A: Add lunar calendar library** (recommended)
- Add dep: `com.github.isohuynh:vietnamese-lunar-calendar` or `org.mabb:lunar-calendar`
- Replace hardcoded dates with `LunarDate.toSolar()` conversion
- Cache results per-year (static table)

**Option B: CSV lookup table** (simpler, 10-year window)
- Pre-computed CSV: `year, tet_start, giotoonggia, trungthu`
- For 2025-2035 (10 years) — maintainer adds row yearly
- No runtime dependency, zero CPU

## Recommendation

Option B for MVP (P3 priority, 1-day work). Revisit Option A when Vietnamese compliance becomes critical (e.g., K-12 MoET audit).

## Acceptance Criteria

- [x] 11-year CSV (2025-2035) committed at `src/main/resources/data/vn-lunar-holidays.csv`
- [x] Service reads CSV via `@PostConstruct`, falls back to approximate dates if year out of range (Feb 1 / Apr 15 / Sep 20)
- [x] Unit tests: 10 tests covering 3+ known years (2026, 2027) + out-of-range fallback (2050) + Trung Thu inclusion
- [x] TODO comments removed from VnHolidayProvider.java
- [x] Added Tết Trung Thu (15/8 lunar) as national holiday (was missing)
- [x] Fixed Giỗ tổ Hùng Vương (was hardcoded solar Apr 18 → now lunar-computed)

## Dependencies

- None. Standalone fix.

## Related

- GAP-072 Scheduled rebrand + academic-year-tied branding refresh
