# GAP-1071: (dashboard) page thiếu wrap DashboardLayout shell → mất header/sidebar/footer

**Status:** 🟡 PARTIAL
**Priority:** 🟠 P1
**Domain:** Frontend
**Found:** 2026-06-08 (KC-1 G2 Bước 2 — user báo /settings "vỡ layout hoàn toàn, không header footer")
**Affects:** `kiteclass-frontend` (dashboard) route group — pages không tự wrap `<DashboardLayout>`

## Problem

Route `(dashboard)/layout.tsx` **KHÔNG render shell nav** (chỉ `<BrandingThemeApplier/> {children} <CommandPalette/>`). Convention codebase: **mỗi page tự wrap `<DashboardLayout>`** (`components/layout/dashboard-layout.tsx` = Sidebar + Header + `container mx-auto p-6`). Pages như classes/courses/students/billing/admin đều wrap.

**`settings/page.tsx` QUÊN wrap** → render bare `<div className="space-y-6">` → không header/sidebar/footer → "vỡ layout". **FIXED** PR này: import `DashboardLayout` + wrap return.

### Cross-flow sweep — TRIAGE 10 page DONE (per cross-flow-bug-class-sweep §5 FIX/EXEMPT)

11 (dashboard) page.tsx ban đầu KHÔNG dùng shell. `settings` đã fix trước. 10 page còn lại triage:

| # | Page | Verdict | Shell dùng | Lý do |
|---|---|---|---|---|
| 0 | `settings/page.tsx` | ✅ FIXED (trước PR này) | DashboardLayout | Owner settings dashboard bình thường |
| 1 | `reports/page.tsx` | ✅ **FIX** | DashboardLayout | Owner revenue/attendance analytics dashboard; wrap cả 2 return (no-permission + main) |
| 2 | `overview/page.tsx` | ✅ **FIX** | DashboardLayout | Alt dashboard home (KPI cards), cần sidebar+header |
| 3 | `branding/page.tsx` | ✅ **FIX** | DashboardLayout | Owner branding landing, dashboard chrome bình thường |
| 4 | `admin/payroll/page.tsx` | ✅ **FIX** | DashboardLayout | Admin payroll management page |
| 5 | `admin/attendance/stats/page.tsx` | ✅ **FIX** | DashboardLayout | Admin attendance stats page |
| 6 | `students/[id]/attendance/page.tsx` | ✅ **FIX** | DashboardLayout | Student attendance detail (admin/teacher view); wrap cả 3 return (loading/error/main) |
| 7 | `admin/vetting/[vettingId]/upload/page.tsx` | ✅ **FIX** | DashboardLayout | Admin upload form page (full route, KHÔNG phải modal); wrap cả 2 return |
| 8 | `student/page.tsx` | 🟦 **EXEMPT** | — | Redirect-only (`redirect('/student/today')`) — không render UI, không cần chrome. Marker `// shell-exempt:` |
| 9 | `branding/wizard/page.tsx` | 🟦 **EXEMPT** | — | Full-screen multi-step branding wizard, focused flow by design. Marker `// shell-exempt:` |
| 10 | `parent/transcript/[childId]/page.tsx` | 🟦 **EXEMPT** | — | Full-width học bạ document/print view (`max-w-5xl`) với self-contained header + back-nav; ParentShell mobile-480px incompatible (DashboardLayout sai persona). Marker `// shell-exempt:` |

**Kết quả:** 7 FIX (wrap DashboardLayout) + 3 EXEMPT (marker `// shell-exempt:`). Lưu ý role-shell: parent pages bình thường dùng `ParentShell`, student pages dùng `StudentMobileShell` (per-page, layout.tsx chỉ persona-guard); transcript desktop-width nên EXEMPT thay vì ép ParentShell mobile shell.

`pnpm --filter kiteclass-frontend build` PASS (EXIT=0, 61/61 static pages) sau khi wrap — verify per fe-build-local-verify.

### Detector

`scripts/check-dashboard-shell-wrapper.sh` (NEW) — scan `(dashboard)/**/page.tsx` không wrap DashboardLayout/ParentShell/StudentMobileShell/TeacherShell/MobileShell và không có marker `// shell-exempt:` → WARN finding (WARN-mode exit 0). Self-test sau triage: `scanned=55 shell-exempt=3 findings=0`. CI wiring deferred (coordinator).

### Observations phụ (cùng /settings, không chặn)
- **Logo hiện tại không render**: `branding.logo_url` CÓ (sky-logo.png) nhưng UI ghi "Không có tệp" → BrandingSettings không hiển thị logo hiện tại. Thêm: presigned URL hết hạn (X-Amz-Date 0529 + 7d → ~0605). → GAP-1072.
- **contact_email/phone/address rỗng trong seed** — UI hiện trống là ĐÚNG (không phải bug); seed test tenant nên enrich VN contact (per vn-localization-audit §3) cho demo thật.

## Proposed Fix

Đã wrap settings. Cân nhắc fix gốc hơn: chuyển shell vào `(dashboard)/layout.tsx` 1 lần (wrap `{children}` trong DashboardLayout) thay vì per-page wrap — eliminate class này vĩnh viễn (per-page wrap = anti-pattern dễ quên). Nhưng cần verify pages cố ý full-screen (wizard/print/upload) opt-out được. Triage 10 page còn lại.

## Acceptance Criteria

- [x] settings/page.tsx wrap DashboardLayout → /settings có header/sidebar/footer
- [x] Triage 10 page còn lại: FIX (thiếu shell thật) vs EXEMPT (cố ý full-screen) — 7 FIX + 3 EXEMPT, build PASS
- [ ] Cân nhắc move shell vào (dashboard)/layout.tsx (root fix) + opt-out cho full-screen pages — SPLIT defer (cross-cutting root-fix per small-gap-inline-fix §3 SPLIT; fix-site per-page inline DONE)
- [ ] Re-walk /settings browser → chrome hiển thị (per g1-browser-walk-before-flip)

## Related

- Discovered in: KC-1 G2 Bước 2 walk 2026-06-08
- GAP-1072 (logo hiện tại không render — sister observation)
- `cross-flow-bug-class-sweep` + `flow-verification-campaign §4.5` (cross-flow class → sweep)
- `g1-browser-walk-before-flip` (browser-walk bắt class này — curl không thấy layout)
