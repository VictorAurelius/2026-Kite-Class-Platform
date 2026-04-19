# GAP-140: `form-select` Default Placeholder Hardcoded English "Select an option"

**Status:** 🔵 OPEN
**Priority:** 🟡 P2
**Domain:** Frontend / i18n
**Found:** 2026-04-19 (UI audit catch-up — ui-review-2026-04-19.md §New Issues U-5)
**Affects:** `kiteclass-frontend` — every consumer of `<FormSelect>` that omits explicit `placeholder` prop

## Problem

`kiteclass-frontend/src/components/forms/form-select.tsx:60` contains:

```tsx
<SelectValue placeholder={placeholder || 'Select an option'} />
```

The fallback English string leaks to the UI whenever a consumer does not pass a `placeholder` prop. Previous i18n sweep (#329 GAP-079) translated wizard labels, status badges, data-table strings — but missed this default.

**Evidence (2026-04-19):**
```bash
$ grep -n "Select an option" kiteclass-frontend/src/components/forms/form-select.tsx
60:            <SelectValue placeholder={placeholder || 'Select an option'} />
```

Also referenced by test expecting the English default:
```
kiteclass-frontend/src/components/forms/__tests__/form-select.test.tsx:87:
  expect(screen.getByText('Select an option')).toBeInTheDocument();
```

## Root Cause

i18n sweep scoped to visible UI surfaces; a lib-internal fallback was overlooked because tests explicitly assert on it.

## Proposed Fix

Change the fallback to Vietnamese:

```tsx
<SelectValue placeholder={placeholder || 'Chọn một tùy chọn'} />
```

Update matching test to assert Vietnamese string. Encourage consumers to always pass a contextual placeholder (e.g. "Chọn lớp", "Chọn giới tính") — but fallback must still be Vietnamese.

Optional hardening: accept `placeholder?: string` required-by-convention via lint rule if team prefers explicit labels.

## Acceptance Criteria

- [ ] `form-select.tsx:60` fallback = Vietnamese
- [ ] Test updated to match
- [ ] Full-repo grep for `'Select an option'` returns 0 matches (excluding test fixtures)
- [ ] Full-repo grep for remaining untranslated strings in `components/forms/` returns 0

## Related

- Audit: `documents/04-quality/audits/ui/ui-review-2026-04-19.md` §New Issues U-5
- Prior i18n sweep: GAP-079 (closed PR #329) — this gap is a residual oversight
- Design: Vietnamese-first i18n requirement per CLAUDE.md Language rule

## Log

- 2026-04-19 — Identified during UI audit catch-up.
