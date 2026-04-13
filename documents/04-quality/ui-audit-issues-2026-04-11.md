# UI Audit Issues Report — 2026-04-11

**Nguồn:** 144 screenshots (36 pages × 4 variants: light/dark × desktop/mobile)  
**Label:** `after-pr-259-darkfix`  
**Người đánh giá:** Claude Code

---

## Tóm tắt Executive

| Severity | Total | Fixed | Open | Fixed by |
|----------|-------|-------|------|----------|
| 🔴 P0 Critical | 4 | 4 | 0 | PR #261, #262 |
| 🟠 P1 High | 5 | 5 | 0 | PR #263 |
| 🟡 P2 Medium | 4 | 2 | 2 | PR #263 (partial) |
| 🟢 P3 Low | 3 | 1 | 2 | P3-1 auto-resolved |

**Updated 2026-04-13:** 12/16 issues fixed. 4 remaining (2 P2 + 2 P3, all minor).
**Re-audit screenshots needed** to visually confirm fixes.

---

## 🔴 P0 — Critical (Production Bugs) — ✅ ALL FIXED

### P0-1: ReactQueryDevtools hiển thị ở Production — ✅ Fixed PR #261

**Trang bị ảnh hưởng:** TẤT CẢ 36 trang  
**Visible:** Badge đỏ "2 errors" ở bottom-left MỌI trang  
**Root cause:**

```tsx
// src/providers/ReactQueryProvider.tsx:35
<ReactQueryDevtools initialIsOpen={false} />  // ← không có NODE_ENV check
```

`ReactQueryDevtools` được render không điều kiện — hiển thị ở production, expose API error count, gây confusion cho end users.

**Fix:**
```tsx
{process.env.NODE_ENV === 'development' && <ReactQueryDevtools initialIsOpen={false} />}
```

---

### P0-2: Mobile Dashboard Layout Broken — ✅ Fixed PR #262

**Trang bị ảnh hưởng:** Tất cả 21+ dashboard pages trên mobile  
**Triệu chứng:** Sidebar + content render side-by-side → content area cực hẹp, text bị wordwrap từng chữ ("Lớp\nhọc", "Quản lý\ndanh\nsách...")  
**Root cause:**

```tsx
// src/components/layout/dashboard-layout.tsx:26
<div className="flex flex-1 flex-col overflow-hidden pl-64">
//                                                   ^^^^ hardcoded 256px, không responsive
```

Sidebar không có `hidden md:flex`, content area không có `md:pl-64` — trên mobile sidebar luôn hiển thị và chiếm không gian.

**Fix cần:**
1. `Sidebar`: thêm `hidden md:flex` (ẩn trên mobile)
2. Content wrapper: `pl-0 md:pl-64`
3. Mobile: thêm hamburger button + drawer/sheet nav
4. Header: thêm hamburger icon trên mobile

---

### P0-3: `billing-pay` Page Hoàn Toàn Trắng — ✅ Fixed PR #261

**Route:** `/billing/:id/pay`  
**Triệu chứng:** Chỉ hiện "Đang tải..." — không render form thanh toán  
**Root cause:**

```tsx
// billing/[id]/pay/page.tsx:64
if (!invoice) return <div>Đang tải...</div>;
```

Khi API trả 404 (không tìm thấy invoice), `invoice` mãi là `null` → page kẹt ở loading state vĩnh viễn. Không có error state.

**Fix:** Phân biệt `isLoading` vs `error` vs `data === null`:
```tsx
if (isLoading) return <Skeleton />;
if (!invoice) return <ErrorState message="Không tìm thấy hóa đơn" />;
```

---

### P0-4: `settings` Page Hoàn Toàn Trắng — ✅ Fixed PR #261

**Route:** `/settings`  
**Triệu chứng:** Header + tabs hiển thị, content area chỉ "Đang tải..."  
**Root cause:**

```tsx
// src/components/settings/branding-settings.tsx:85-86
if (isLoading) return <div>Đang tải...</div>;
```

Khi API branding fail (no backend), `isLoading` chuyển sang `isError` nhưng component không handle `error` state → hiển thị sai.

**Fix:** Handle error state trong `BrandingSettings` và `PreferencesSettings`.

---

## 🟠 P1 — High (Dead-end UX) — ✅ ALL FIXED

### P1-1: 5 List Pages Spinner Vô Hạn — ✅ Fixed PR #263

**Trang:** Students, Teachers, Courses, Classes, Billing  
**Triệu chứng:** Spinner xoay vô tận — không có timeout, không có empty state  
**Root cause:** `useQuery` với mock auth → API 401 → `retry: 1` → vẫn fail → spinner forever  
**Fix cần:** Sau khi query fail (`isError`), render proper empty/error state thay vì spinner

---

### P1-2: Dashboard Teacher Stats Cards — ✅ Fixed PR #263

**Route:** `/teacher/dashboard`  
**Triệu chứng:** 4 stat cards (tổng học viên, lớp học, etc.) là blank skeleton indefinitely  
**Root cause:** Tương tự P1-1 — API fails, nhưng component chỉ check `isLoading` không check `isError`  
**Fix:** Khi `isError`, show `"--"` hoặc `"N/A"` thay vì skeleton trắng

---

### P1-3: `billing-detail` Error State Minimal — ✅ Fixed PR #263

**Route:** `/billing/:id`  
**Triệu chứng:** Chỉ text thuần "Không tìm thấy hóa đơn" — không có navigation, không có back button, không có layout dashboard  
**Fix:** Dùng proper `ErrorState` component với "Quay lại" button

---

### P1-4: Catalog Infinite Loading Spinner — ✅ Fixed PR #263

**Route:** `/catalog`  
**Triệu chứng:** Spinner loading ở giữa trang — không có timeout fallback  
**Vấn đề:** Public page không có auth nhưng vẫn cần backend. Nên có graceful fallback.  
**Fix:** Sau `retry: 1` thất bại → show empty state "Chưa có khóa học" thay vì spinner

---

### P1-5: Catalog Detail 404 Minimal — ✅ Fixed PR #263

**Route:** `/catalog/:id`  
**Triệu chứng:** Page "Không tìm thấy trang" — đúng nhưng UX kém (không breadcrumb, không suggest navigation)  
**Fix:** Thêm breadcrumb "Khóa học / Không tìm thấy" + link "Xem tất cả khóa học"

---

## 🟡 P2 — Medium (Placeholder & Polish) — 2/4 Fixed

### P2-1: Contact Page — ✅ Fixed PR #263 (env vars)

**Route:** `/contact`  
**Visible data:**
- Email: `support@kiteclass.com` ← fake
- Hotline: `1900 xxxx` ← placeholder
- Địa chỉ: "Hà Nội, Việt Nam" ← generic

**Fix:** Thay bằng config-driven values hoặc real info. Minimum: dùng `NEXT_PUBLIC_CONTACT_EMAIL`, `NEXT_PUBLIC_CONTACT_PHONE` env vars.

---

### P2-2: Footer — ✅ Fixed PR #263 (env vars)

**Trang bị ảnh hưởng:** TẤT CẢ public pages (landing, about, catalog, contact)  
**Visible:** `Email: support@kiteclass.com` + `Hotline: 1900 xxxx`  
**Fix:** Cùng env vars như P2-1

---

### P2-3: Attendance Stats — Skeleton Cards Không Có Label — ⬜ Open

**Route:** `/admin/attendance/stats`  
**Triệu chứng:** 4 skeleton cards loading nhưng không có label text → user không biết stats là gì  
**Fix:** Giữ label text khi loading, chỉ skeleton value

---

### P2-4: `class-attendance`, `student-attendance` — Error "Lỗi" Bare Text — ⬜ Open

**Triệu chứng:** Box đỏ nhỏ "Lỗi / Không tìm thấy lớp học" — không có context, không có navigation  
**Fix:** Dùng consistent `ErrorState` component với back button

---

## 🟢 P3 — Low (Polish) — 1/3 Fixed

### P3-1: Mobile — "2 errors" overlay — ✅ Auto-resolved (P0-1 fix)

Sau khi fix P0-1 (devtools), issue này tự resolve.

### P3-2: Landing Page — Nav Active State — ⬜ Open

Trên landing, không có nav item nào được highlight là active. Minor UX issue.

### P3-3: Auth Pages Mobile — Left Panel Ẩn — ⬜ Open

Trên mobile auth (login, register), left panel màu xanh với KiteClass branding bị ẩn hoàn toàn. UX chấp nhận được nhưng branding bị mất. Có thể thêm logo nhỏ ở top-center thay thế.

---

## Tổng Hợp Theo Trang

| Trang | Trạng thái | Vấn đề chính |
|-------|-----------|--------------|
| landing | ✅ Good | Footer placeholder |
| about | ✅ Good | Footer placeholder |
| catalog | ⚠️ P1 | Infinite spinner, footer placeholder |
| catalog-detail | ⚠️ P1 | 404 minimal UI |
| contact | ⚠️ P2 | Placeholder email/phone |
| login | ✅ Good | Devtools badge (P0) |
| register | ✅ Good | Devtools badge (P0) |
| register-student | ✅ Good | Devtools badge (P0) |
| forgot-password | ✅ Good | — |
| reset-password | ✅ Good | Error state intentional |
| dashboard-teacher | ⚠️ P1 | Skeleton cards, mobile broken |
| classes | ⚠️ P1 | Infinite spinner, **mobile broken** |
| students | ⚠️ P1 | Infinite spinner, **mobile broken** |
| teachers | ⚠️ P1 | Infinite spinner, **mobile broken** |
| courses | ⚠️ P1 | Infinite spinner, **mobile broken** |
| class-detail | ⚠️ P1 | Error UI minimal |
| student-detail | ⚠️ P1 | Error UI minimal |
| attendance | ✅ Good | Empty state OK (no data) |
| attendance-reports | ✅ Good | Filter-first UX intentional |
| attendance-stats | ⚠️ P2 | Skeleton labels missing |
| billing | ⚠️ P1 | Infinite spinner |
| billing-detail | 🔴 P0 | Blank page |
| billing-pay | 🔴 P0 | Blank page |
| settings | 🔴 P0 | Blank page |
| *ALL dashboard mobile* | 🔴 P0 | Sidebar layout broken |

---

## Kế Hoạch Fix: 3 PRs

### PR #261 — Quick P0 Fixes (Ưu tiên cao nhất)
**Scope:** 3 isolated file changes
- `ReactQueryProvider.tsx`: Wrap `ReactQueryDevtools` trong NODE_ENV check
- `billing/[id]/pay/page.tsx`: Phân biệt isLoading vs isError
- `components/settings/branding-settings.tsx`: Handle error state

**Estimate:** 1-2 giờ | **Branch:** `fix/p0-dev-overlay-error-states`

### PR #262 — Mobile Dashboard Layout (Phức tạp nhất)
**Scope:** Responsive sidebar + hamburger navigation
- `dashboard-layout.tsx`: `pl-0 md:pl-64`
- `sidebar.tsx`: `hidden md:flex`, thêm overlay mode cho mobile
- `header.tsx`: Thêm hamburger button + Sheet/Drawer nav

**Estimate:** 4-6 giờ | **Branch:** `fix/mobile-dashboard-responsive`

### PR #263 — Empty States + Placeholder Data
**Scope:** UX improvements
- Tất cả list pages: `isError` → empty state component
- Dashboard stats: skeleton → `"--"` khi error
- Contact/footer: env vars cho email/phone
- Catalog: timeout → empty state
- Error pages: thêm back button + context

**Estimate:** 2-3 giờ | **Branch:** `fix/empty-states-and-placeholders`

---

## Điểm Tốt (Không Cần Sửa)

- ✅ Dark mode: hoạt động đúng sau PR #260
- ✅ Auth pages: i18n hoàn chỉnh, layout 2-panel đẹp
- ✅ Landing page: nội dung đầy đủ sau PR #258
- ✅ ARIA attributes: form errors announce đúng sau PR #259
- ✅ About page: nội dung đầy đủ, responsive tốt
- ✅ Attendance page: empty state + filter UX hợp lý
- ✅ Register student form: date hint, validation rõ ràng
- ✅ Course-new, student-new forms: placeholder data phù hợp

