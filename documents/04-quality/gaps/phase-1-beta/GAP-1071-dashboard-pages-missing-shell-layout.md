# GAP-1071: (dashboard) page thiếu wrap DashboardLayout shell → mất header/sidebar/footer

**Status:** 🟡 PARTIAL
**Priority:** 🟠 P1
**Domain:** Frontend
**Found:** 2026-06-08 (KC-1 G2 Bước 2 — user báo /settings "vỡ layout hoàn toàn, không header footer")
**Affects:** `kiteclass-frontend` (dashboard) route group — pages không tự wrap `<DashboardLayout>`

## Problem

Route `(dashboard)/layout.tsx` **KHÔNG render shell nav** (chỉ `<BrandingThemeApplier/> {children} <CommandPalette/>`). Convention codebase: **mỗi page tự wrap `<DashboardLayout>`** (`components/layout/dashboard-layout.tsx` = Sidebar + Header + `container mx-auto p-6`). Pages như classes/courses/students/billing/admin đều wrap.

**`settings/page.tsx` QUÊN wrap** → render bare `<div className="space-y-6">` → không header/sidebar/footer → "vỡ layout". **FIXED** PR này: import `DashboardLayout` + wrap return.

### Cross-flow sweep (per cross-flow-bug-class-sweep + campaign §4.5)

11 (dashboard) page.tsx KHÔNG dùng DashboardLayout/role-shell:

| Page | Verdict |
|---|---|
| `settings/page.tsx` | ✅ FIXED PR này |
| `reports/page.tsx` | ⏳ TRIAGE (KC-11 — likely FIX) |
| `overview/page.tsx` | ⏳ TRIAGE (alt dashboard home — likely FIX) |
| `branding/page.tsx` + `branding/wizard/page.tsx` | ⏳ TRIAGE (KC-10; wizard có thể cố ý full-screen → EXEMPT) |
| `admin/payroll/page.tsx` | ⏳ TRIAGE (KC-12) |
| `admin/attendance/stats/page.tsx` | ⏳ TRIAGE |
| `students/[id]/attendance/page.tsx` | ⏳ TRIAGE |
| `parent/transcript/[childId]/page.tsx` | ⏳ TRIAGE (print view → có thể EXEMPT) |
| `admin/vetting/[vettingId]/upload/page.tsx` | ⏳ TRIAGE (upload modal → có thể EXEMPT) |
| `student/page.tsx` | ⏳ TRIAGE (student dùng mobile-shell?) |

Mỗi page thuộc flow riêng (KC-10/11/12...) → triage FIX/EXEMPT khi G2 flow đó, HOẶC batch-fix dedicated wave.

### Observations phụ (cùng /settings, không chặn)
- **Logo hiện tại không render**: `branding.logo_url` CÓ (sky-logo.png) nhưng UI ghi "Không có tệp" → BrandingSettings không hiển thị logo hiện tại. Thêm: presigned URL hết hạn (X-Amz-Date 0529 + 7d → ~0605). → GAP-1072.
- **contact_email/phone/address rỗng trong seed** — UI hiện trống là ĐÚNG (không phải bug); seed test tenant nên enrich VN contact (per vn-localization-audit §3) cho demo thật.

## Proposed Fix

Đã wrap settings. Cân nhắc fix gốc hơn: chuyển shell vào `(dashboard)/layout.tsx` 1 lần (wrap `{children}` trong DashboardLayout) thay vì per-page wrap — eliminate class này vĩnh viễn (per-page wrap = anti-pattern dễ quên). Nhưng cần verify pages cố ý full-screen (wizard/print/upload) opt-out được. Triage 10 page còn lại.

## Acceptance Criteria

- [x] settings/page.tsx wrap DashboardLayout → /settings có header/sidebar/footer
- [ ] Triage 10 page còn lại: FIX (thiếu shell thật) vs EXEMPT (cố ý full-screen)
- [ ] Cân nhắc move shell vào (dashboard)/layout.tsx (root fix) + opt-out cho full-screen pages
- [ ] Re-walk /settings browser → chrome hiển thị (per g1-browser-walk-before-flip)

## Related

- Discovered in: KC-1 G2 Bước 2 walk 2026-06-08
- GAP-1072 (logo hiện tại không render — sister observation)
- `cross-flow-bug-class-sweep` + `flow-verification-campaign §4.5` (cross-flow class → sweep)
- `g1-browser-walk-before-flip` (browser-walk bắt class này — curl không thấy layout)
