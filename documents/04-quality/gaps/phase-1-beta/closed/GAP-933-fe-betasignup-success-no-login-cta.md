# GAP-933: BetaSignupForm success state lacks "Đăng nhập" CTA — user dead-ends after signup

**Status:** 🟢 DONE 2026-06-04 — anchor button shipped Wave flow-kh1 (user-flagged during g2test-an-8 re-walk)
**Priority:** 🟡 P2 (UX miss — user can navigate to /login manually but the flow surface stops short of guiding them)
**Domain:** Frontend
**Found:** 2026-06-04 (Wave flow-kh1 G2 re-walk session, user feedback right after seeing "Tạo tài khoản thành công" for g2test-an-8: "không có nút quay lại trang login à")
**Affects:** `kitehub/kitehub-frontend/src/components/auth/BetaSignupForm.tsx` lines 157-164 (`submitted` branch)

## Problem

After a successful beta signup the form renders only a "Tạo tài khoản thành công" panel + "Bạn có thể đăng nhập với email <X>". No call-to-action — the invitee must manually navigate to `/login`. For a beta program where the next step is always "log in and start using the app", this dead-end is a small but real UX miss.

## Fix

Add an explicit `<a href="/login">Đăng nhập</a>` button under the success message styled to match the project's primary CTA. Anchor (not button) so it works pre-hydration too.

## Acceptance Criteria

- [x] Success state renders a primary-style "Đăng nhập" button linking to `/login`
- [x] Button visible and keyboard-accessible (focusable anchor with primary styling)
- [x] Dark mode contrast verified via `dark:` Tailwind variants

## Related

- Discovered in: Wave flow-kh1 G2 re-walk 2026-06-04 (g2test-an-8 signup PASS, user noted missing CTA)
- Sister UX miss class: future audit candidate per `cross-flow-bug-class-sweep.md` §3 — sweep other "success-state-dead-end" patterns (verify-email confirm page, password-reset success, 2FA-enroll success) in a follow-up walk
