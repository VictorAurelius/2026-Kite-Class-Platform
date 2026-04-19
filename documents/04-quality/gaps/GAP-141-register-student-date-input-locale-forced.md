# GAP-141: Register-Student Date Input Locale-Forced (dd/mm/yyyy not enforced)

**Status:** 🔵 OPEN
**Priority:** 🟡 P2
**Domain:** Frontend / i18n / UX
**Found:** 2026-04-19 (UI audit catch-up — ui-review-2026-04-19.md §New Issues U-6)
**Affects:** `kiteclass-frontend` `(auth)/register/student/page.tsx` — student self-registration form

## Problem

The date-of-birth input on the student registration form uses a native HTML5 `type="date"` (`page.tsx:246`). Browser display format for this input type is **locale-determined**, not author-controllable — an English-locale Chromium will show mm/dd/yyyy, an Vietnamese-locale may show dd/mm/yyyy. Prior audit flagged this as K-3 (P1). #263 added helper text "Định dạng: ngày/tháng/năm" (`page.tsx:251`) but the actual **displayed** format still depends on OS/browser locale, not on page intent.

Vietnamese users on browsers launched with English locale (common — many preinstalled Chromium builds, Edge on VN Windows, corporate-imaged Chrome) will see mm/dd/yyyy and be confused. Hint text helps but does not fix the root UX inconsistency.

**Evidence (2026-04-19):**
```tsx
// kiteclass-frontend/src/app/(auth)/register/student/page.tsx:243-252
<Input
  id="dateOfBirth"
  name="dateOfBirth"
  type="date"
  value={formData.dateOfBirth}
  onChange={handleChange}
  disabled={isLoading}
/>
<p className="text-xs text-muted-foreground">Định dạng: ngày/tháng/năm</p>
```

## Root Cause

HTML5 date input intentionally hides format from author. Only way to enforce dd/mm/yyyy is:
1. Custom date picker (Shadcn has `Calendar` + `Popover` based `DatePicker` recipe).
2. Three separate numeric inputs (day / month / year) with explicit labels.
3. Text input with regex `^\d{2}/\d{2}/\d{4}$` + client-side validation.

Hint text alone is insufficient for users who've already typed the wrong format.

## Proposed Fix

Replace `type="date"` with Shadcn `DatePicker` (Calendar + Popover), locale set to `vi-VN`:

```tsx
import { DatePicker } from '@/components/ui/date-picker';
import { vi } from 'date-fns/locale';

<DatePicker
  value={formData.dateOfBirth}
  onChange={(date) => update('dateOfBirth', format(date, 'yyyy-MM-dd'))}
  locale={vi}
  displayFormat="dd/MM/yyyy"
/>
```

This gives:
- Consistent dd/MM/yyyy display regardless of browser locale
- Vietnamese month/weekday labels
- Calendar picker with correct week-start (Monday for vi-VN)
- Still submits ISO string to backend

Apply same fix to other date inputs (student-new, teacher-new, class-edit).

## Acceptance Criteria

- [ ] `DatePicker` component replaces native `type="date"` on register-student
- [ ] Vietnamese locale (month/weekday labels)
- [ ] Displayed format = dd/MM/yyyy regardless of browser locale
- [ ] Backend still receives ISO yyyy-MM-dd
- [ ] Same pattern applied to: student-new, teacher-new, and any other date input forms
- [ ] E2E test: submit date → verify backend receives correct ISO

## Related

- Audit: `documents/04-quality/audits/ui/ui-review-2026-04-19.md` §New Issues U-6
- Previous audit: K-3 (2026-04-11, marked P1, fixed PARTIAL via hint text)
- i18n sweep: GAP-079 (closed PR #329) — this gap is residual (date widget, not text)

## Log

- 2026-04-19 — Re-verified. K-3 partial fix via hint text insufficient; needs actual widget swap.
