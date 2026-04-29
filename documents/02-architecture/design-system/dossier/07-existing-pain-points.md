# 07 — Existing Pain Points (UI Audit Findings)

Top issues from the most recent UI audit baseline (2026-04-19, KC 81/128 + KH 59/128 averages). Round 2 should target these specifically — fix what's broken before adding new screens.

**Use this when:** prioritizing which screens Round 2 redesigns first. Match Pain Point # against `03-screen-inventory.md` Round-2 priority list.

**Source:** `documents/04-quality/audits/ui-review/2026-04-19/`. Some issues marked UNVERIFIED need fresh Playwright capture post-fix.

---

## Top 10 lowest-scoring screens (cross-app)

| # | App | Screen | Score | Severity | Root cause |
|:-:|-----|--------|:-----:|:--------:|------------|
| 1 | KH | `/billing/payment/[id]` | **33/128** 🔴 | P0 | Mock data: empty/not loading; minimal UX; placeholder layout |
| 2 | KH | `/branding` (hub) | **33/128** 🔴 | P0 | Incomplete feature state; navigation broken to sub-routes |
| 3 | KH | `/branding/wizard` | **33/128** 🔴 | P0 | Wizard steps unclear, no preview, no quality gate visible |
| 4 | KH | `/instances/[id]` | **33/128** 🔴 | P0 | Mock auth fails, no lifecycle UI, status pill only |
| 5 | KH | `/billing` | **39/128** 🔴 | P1 | Sparse content, no invoice list pattern, table empty |
| 6 | KH | `/billing/upgrade` | **39/128** 🔴 | P0 | Conversion paywall — 4 tiers shown but no comparison feature, no CTA hierarchy |
| 7 | KH | `/admin` | 43/128 🔴 | P1 | Admin dashboard placeholder, no real KPIs |
| 8 | KH | `/admin/instances/[id]` | 45/128 🔴 | P0 | Mock data, no lifecycle controls, no logs |
| 9 | KH | `/billing/history` | 50/128 🟠 | P2 | List works but no filters, no period picker |
| 10 | KH | `/admin/instances` | 51/128 🟠 | P0 | List works but no bulk actions, status filter, no search |

**KiteClass lowest:**

| # | App | Screen | Score | Severity | Root cause |
|:-:|-----|--------|:-----:|:--------:|------------|
| 11 | KC | `/settings` | **74/128** 🟠 | P1 | Black color pickers (no styling), unstyled file input |
| 12 | KC | `/parent` | 76/128 🟡 | P0 | MVP placeholder only — children card + "coming soon" message |
| 13 | KC | `/reset-password` | 76/128 🟡 | P2 | Minimal styling, no brand presence |
| 14 | KC | `/students` | 78/128 🟡 | P0 | Missing bulk import button (GAP-137) |

---

## Cross-app issues (prioritized P0–P3)

| ID | Severity | App | Status | Issue | Recommendation |
|----|:--------:|-----|--------|-------|----------------|
| **U-1** | P0 | KH | OPEN | No `not-found.tsx` / `error.tsx` / `global-error.tsx` — English fallback shown for blog-detail, admin routes, payment routes | Mirror KiteClass error pages (Vietnamese custom 404). One-time hit, fixes ~6 screens |
| **U-2** | P0 | KC | OPEN | `/students` has no bulk-import entry point — backend ready (GAP-137) | Add CTA button to `/students` page header |
| **H-1** | P0 | KH | PARTIAL | Mock auth not working on dashboard/admin routes (GAP-076) | Fix MSW handlers — dashboard captures all show error states |
| **K-5 / U-4** | P1 | KC | OPEN | Landing hero has duplicated text — hardcoded `<span>Chuyên nghiệp & Hiệu quả</span>` in HeroSection | Remove hardcoded span, rely on i18n key |
| **K-4 / U-5** | P2 | KC | PARTIAL | Form-select fallback `'Select an option'` still English (line 60) | Localize to `'Chọn một tùy chọn'` |
| **H-2** | P1 | KH | OPEN | `/blog/[slug]` no custom 404 — English Next.js default | Custom 404 with Vietnamese copy |
| **H-3** | P1 | KH | OPEN | `/blog` has Vietnamese diacritics rendering issue (UNVERIFIED) | Verify with fresh capture; likely font subset issue |
| **H-5** | P1 | KH | UNVERIFIED | `/login` missing forgot-password link | Add link |
| **U-3** | P1 | KC | OPEN | Parent dashboard MVP placeholder only — Wave 2 widgets missing (GAP-139) | **Direction D pivot — kiteclass-parent kit covers this** |
| **U-7** | P2 | KC | OPEN | `/parent-invite/[token]` uses native `<select>` instead of shadcn | Replace with shadcn Select |

---

## Pattern issues (multiple screens affected)

### A. Mock data missing on KH dashboard routes

KH 8 routes show error states because mock auth fails. Root cause: MSW v2 not wired correctly for KH (GAP-076). Until fixed, ANY redesign of KH dashboard will look broken in capture.

**Round 2 instruction:** design with mock data assumed-working. Document expected data shape per screen so MSW handlers can be authored once during port to production.

### B. No "empty state" pattern across the app

Most screens show a skeleton or table with `[]` (empty array) — no friendly empty state with CTA.

**Round 2 instruction:** every list screen designs MUST include empty state with:
- Icon (lucide)
- Vietnamese copy explaining situation
- Primary CTA to populate (`Thêm học sinh đầu tiên`, `Tạo khóa học`)
- Secondary CTA if applicable (`Nhập từ Excel`)

### C. Loading state inconsistency

- KH uses skeleton in some places, spinner in others
- KC uses `<Skeleton>` shadcn primitive but inconsistently

**Round 2 instruction:** unify on shadcn Skeleton. Spec which areas show skeleton (data zones) vs spinner (full-page transition).

### D. Vietnamese diacritics in Inter font

Inter Google Font with `subsets: ['latin', 'vietnamese']` generally renders OK, but some preview screens show diacritic stacking issues (`ư`, `ờ`, `ụ`).

**Round 2 instruction:** specify `font-feature-settings: 'liga', 'calt'` and test rendering with Vietnamese stress test sentence: `Trường Trung học Phổ thông Lê Quý Đôn — Quận 3, TP.HCM`.

### E. Dark mode untested on tenant-themed screens

KC has theme overlay system (`--theme-primary`, `--theme-secondary`, ...) — but dark mode + tenant theme combinations not tested. Likely WCAG fails for some tenant brand colors on dark backgrounds.

**Round 2 instruction:** Direction C AI Branding wizard quality gate MUST measure WCAG AA in BOTH light and dark mode before approving.

### F. Mobile responsive gaps

Some KC routes (`/attendance/reports` 417 LOC, `/dashboard` 363 LOC) have desktop-first layouts that break at <768px.

**Round 2 instruction:** test 320 / 768 / 1440 viewports for every screen. Direction D (parent) MUST design mobile-first; Direction B owner-dashboard CAN be desktop-first but tablet must work.

---

## Already-fixed (don't redesign)

These were lifted in recent waves; Round 2 should NOT touch them unless adding new features:

| Screen | Score | Recent fix | Wave |
|--------|:-----:|------------|------|
| KH `/` (marketing) | 95 | Sticky nav + hero polish | Wave 4 |
| KH `/pricing` | 98 | 3-tier comparison + ribbon | Wave 4 |
| KC `/login` | 97 | Branding injection + skeleton | Wave 4 |
| KC `/catalog/[id]` 404 | 36→72 | Custom Vietnamese 404 (K-1 FIXED) | Wave 5 |
| KH `/settings` | 39→62 | Tab expansion fix (GAP-098 #354) | Wave 4 |

---

## Audit cadence reminder

Per `post-wave-audit-mandate.md` §2.2: UI audit must run within 3 days after FE wave merges. Round 2 deliverables, when ported to production, will trigger:

- UI `/128` re-audit (per-screen)
- Quality `/100` refresh
- Performance audit (bundle budget)

Plan port PRs accordingly — single big-bang port is harder to audit than per-direction PR series.
