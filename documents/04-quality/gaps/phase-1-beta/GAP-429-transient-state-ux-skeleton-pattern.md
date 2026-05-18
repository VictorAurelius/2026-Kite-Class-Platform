# GAP-429: Transient-State UX Skeleton Pattern — Loading/Empty/Error States Below 105/128

**Status:** 🔵 OPEN
**Priority:** 🟠 P1
**Domain:** Frontend — KiteClass + KiteHub UI Kits
**Found:** 2026-05-08 (UI Review /128 Wave 40 Bucket A milestone audit)
**Affects:** 7 screens across 3 kits; teacher and center-owner flows during data fetch + empty periods

---

## Problem

7 HTML kit screens score below the 105/128 threshold, all sharing the same root cause: **transient-state UX (loading/empty/error) has weaker Motion & Interaction and Content & Copy dimensions** compared to primary-flow screens in the same kits.

**Affected screens:**

| Screen | Kit | Score | Dimension weakness |
|--------|-----|-------|--------------------|
| `reports-loading.html` | kiteclass-teacher | **100/128** | Loading message uses spinner text (`Đang tải báo cáo · vui lòng chờ`) rather than progressive skeleton reveal; KPI card skeletons lack animate-pulse; table skeleton uses flat boxes instead of row-shaped cells |
| `branding-hub-loading.html` | kitehub-pro-v2 | **100/128** | Skeleton content sparse; AI generation context not communicated; no progress indicator for async job |
| `attendance-empty.html` | kiteclass-teacher | **102/128** | Empty state illustration is icon-only; CTA (`Thêm học sinh`) buried below fold on mobile; copy doesn't explain how to resolve |
| `reports-empty.html` | kiteclass-teacher | **102/128** | Period navigation good (prev/next month) but empty state icon is generic calendar-x; month context in description only |
| `billing-loading.html` | kitehub-pro-v2 | **102/128** | Loading skeleton minimal; billing summary card not preserved during fetch; no indication of what is loading |
| `dashboard-error.html` | kitehub-pro-v2 | **102/128** | Error recovery path unclear; "Thử lại" button is generic; no distinction between network error vs service error |
| `dashboard-error.html` | kiteclass-pro-v2 | **102/128** | Same pattern — generic error message with no context-specific recovery action |

---

## Root Cause

Transient-state screens were designed to meet the minimum viable standard for their use cases. Two patterns are systematically weaker:

1. **Loading states**: Use static text spinners (`animate-spin` icon + text) where skeleton rows with `animate-pulse` would better preserve layout context and reduce perceived wait time.
2. **Empty/error states**: Copy is descriptive but not actionable. Primary CTA is not visually prominent. Illustrations are generic Lucide icons rather than purpose-built SVG scenes.

These patterns were noted in Wave 34-35 kit builds as "known limitation, P2" — this audit escalates to P1 given the score gap from target (100-102 vs target ≥105).

---

## Proposed Fix

### Phase 1 — Kit prototype updates (HTML)

For each of the 7 screens:

**Loading screens (reports-loading, branding-hub-loading, billing-loading):**
- Replace `animate-spin` spinner text with staggered `animate-pulse` skeleton rows matching the loaded layout exactly (shape fidelity)
- For AI-generating screens: add step indicator (`Bước 2/5 · Phân tích thương hiệu...`)
- Keep `aria-busy="true" aria-live="polite"` wrapper

**Empty state screens (attendance-empty, reports-empty):**
- Replace Lucide icon with purpose-built inline SVG (class-calendar, empty-chart motifs)
- Make CTA button full-width on mobile, top-positioned in the empty state card
- Copy: add "Đây là lần đầu" context hint or "Tháng X/Y không có dữ liệu — chọn tháng khác" pattern

**Error state screens (dashboard-error × 2):**
- Distinguish network error vs service error vs auth-expired visually
- Add specific recovery action per error type: `Thử lại` (network), `Liên hệ hỗ trợ` (service), `Đăng nhập lại` (auth)
- Copy: surface the error code/type for support reference

### Phase 2 — Production implementation

Corresponding production files in `kiteclass-frontend` and `kitehub-frontend`:
- `attendance/reports/page.tsx` — Suspense boundary with skeleton component
- `attendance/page.tsx` — empty state component update
- `kitehub-frontend/app/(dashboard)/branding/page.tsx` — loading state for AI generation jobs

---

## Acceptance Criteria

- [ ] `reports-loading.html` score raised to ≥108/128 (Motion/Interaction +4 via animate-pulse, Content/Copy +4 via step indicator)
- [ ] `branding-hub-loading.html` score raised to ≥108/128
- [ ] `billing-loading.html` score raised to ≥108/128
- [ ] `attendance-empty.html` score raised to ≥108/128 (inline SVG illustration + prominent CTA)
- [ ] `reports-empty.html` score raised to ≥108/128
- [ ] `dashboard-error.html` (kiteclass-pro-v2 + kitehub-pro-v2) scores raised to ≥108/128
- [ ] All 7 screens have updated `Score self-estimate:` annotation in HTML comment
- [ ] No regression on any currently ≥105 screen in same kits

---

## Related

- `documents/04-quality/audits/ui/2026-05-08-wave-40-milestone.md` — source audit
- `documents/02-architecture/design-system/ui_kits/kiteclass-teacher/screens/reports-loading.html` (100/128)
- `documents/02-architecture/design-system/ui_kits/kiteclass-teacher/screens/attendance-empty.html` (102/128)
- `documents/02-architecture/design-system/ui_kits/kiteclass-teacher/screens/reports-empty.html` (102/128)
- `documents/02-architecture/design-system/ui_kits/kitehub-pro-v2/screens/branding-hub-loading.html` (100/128)
- `documents/02-architecture/design-system/ui_kits/kitehub-pro-v2/screens/billing-loading.html` (102/128)
- `documents/02-architecture/design-system/ui_kits/kitehub-pro-v2/screens/dashboard-error.html` (102/128)
- `documents/02-architecture/design-system/ui_kits/kiteclass-pro-v2/screens/dashboard-error.html` (102/128)

## Log

- **2026-05-08:** Filed from Wave 40 Bucket A UI Review /128 milestone audit. 7 screens below 105/128 threshold, all transient states. Root cause: skeleton + empty/error UX pattern weakness. Phase 1 = kit HTML updates; Phase 2 = production Suspense/skeleton implementation.
