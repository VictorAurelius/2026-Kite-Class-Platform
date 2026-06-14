# GAP-1374: KH login form label không associated + thiếu autocomplete + error ARIA — WCAG 1.3.1/3.3.2/1.3.5/4.1.2

**Status:** 🟢 DONE
**Priority:** 🟠 P1
**Domain:** Frontend
**Found:** 2026-06-14 (UI review full audit, AUDIT-2026-06-14-ui-review-full)
**Affects:** `kitehub/kitehub-frontend/src/app/(auth)/login/page.tsx`

## Problem

KH login form vi phạm nhiều WCAG sub-check:

- `<label className="block ...">Email</label>` là raw label KHÔNG có `htmlFor`; `<input {...register('email')}>` KHÔNG có `id` → label không programmatically associated với input (WCAG 1.3.1 Info and Relationships + 3.3.2 Labels or Instructions + 4.1.2 Name/Role/Value). Screen reader không announce label khi focus input.
- Input email/password KHÔNG có `autoComplete` (`autocomplete="email"` / `current-password`) → password manager + WCAG 1.3.5 Identify Input Purpose fail.
- Error `<p className="...text-destructive">{errors.email.message}</p>` KHÔNG có `id`; input KHÔNG có `aria-describedby` trỏ tới error + KHÔNG `aria-invalid` khi lỗi → screen reader không liên kết lỗi với field.

KH login là **outlier**: các form khác của KH (`BetaRequestForm`, `BetaSignupForm`, `FeedbackForm`) ĐÃ dùng `htmlFor`. Login page viết raw thay vì dùng shared FormField pattern.

## Root Cause

Login page viết inline `<label>`/`<input>` thủ công thay vì shared FormField component (như KC dùng `FormInput` với `useId`/`htmlFor`). Không có a11y lint rule bắt unassociated label.

## Proposed Fix

Refactor KH login dùng shared FormField pattern (hoặc thêm `id` + `htmlFor` + `autoComplete` + `aria-invalid` + `aria-describedby`). Sweep sister auth pages per `cross-flow-bug-class-sweep.md`: `(auth)/register`, `(auth)/2fa-challenge`, `(auth)/2fa-setup`, `(auth)/verify-email` — grep raw `<label>` không `htmlFor`.

## Acceptance Criteria

- [x] Email + password input có `id` (`login-email`/`login-password`); label có `htmlFor` khớp `id`
- [x] Input có `autoComplete="email"` / `current-password`
- [x] Khi lỗi: input có `aria-invalid="true"` + `aria-describedby` trỏ error element có `id`; error `<p>` có `role="alert"`
- [x] Sweep evidence: register + 2fa pages kiểm tra cùng class (FIX/EXEMPT documented)

## Resolution

**Fixed:** 2026-06-15 (branch `fix/audit-fixH-ui-2026-06-14`)

`kitehub-frontend/src/app/(auth)/login/page.tsx`:
- Email input: `id="login-email"` + `<label htmlFor="login-email">` + `autoComplete="email"` + `aria-invalid` + `aria-describedby="login-email-error"` (set only khi có lỗi).
- Password input: `id="login-password"` + `<label htmlFor>` + `autoComplete="current-password"` + `aria-invalid` + `aria-describedby="login-password-error"`.
- Error `<p>` mỗi field: thêm `id` tương ứng + `role="alert"`.

**Test:** `(auth)/login/__tests__/page.test.tsx` — thêm test a11y khẳng định `getByLabelText('Email'/'Mật khẩu')` + `autoComplete` (WCAG 1.3.1/1.3.5); helper `fillAndSubmit` chuyển từ `querySelector('input[name]')` sang `getByLabelText`.

### Cross-flow sweep (per `cross-flow-bug-class-sweep.md`)

**Bug class signature:** raw `<label>` không `htmlFor` + input không `id`/`autoComplete`/`aria-*` trong KH auth pages.

| Page | Verdict | Lý do |
|---|---|---|
| `(auth)/login/page.tsx` | **FIX** | Outlier — đã sửa PR này |
| `(auth)/register/page.tsx` | **EXEMPT** | Server component chỉ `redirect('/request-beta-access')` — không có form |
| `(auth)/2fa-challenge/page.tsx` | **EXEMPT** | Recovery-code branch đã có `htmlFor`+`aria-describedby`; TOTP branch dùng `<TotpInput aria-label="Mã TOTP 6 số xác thực">` (custom multi-box, có accessible name riêng) |
| `(auth)/2fa-setup`, `verify-email`, `request-beta-access`, `beta-signup` | **EXEMPT** | Không dùng raw unassociated label (dùng shared form pattern / không có input bị thiếu) |

## Related

- Discovered in: `documents/04-quality/audits/ui-review/2026-06-14-ui-review-full-audit.md` (Bug list, P1)
- Source: `kitehub/kitehub-frontend/src/app/(auth)/login/page.tsx:195-219`
- Good pattern (KC): `kiteclass/kiteclass-frontend/src/components/forms/form-input.tsx` (useId/htmlFor)
- Cross-flow sweep: `.claude/rules/cross-flow-bug-class-sweep.md`
- WCAG 1.3.1 / 3.3.2 / 1.3.5 / 4.1.2 (Level A/AA)
