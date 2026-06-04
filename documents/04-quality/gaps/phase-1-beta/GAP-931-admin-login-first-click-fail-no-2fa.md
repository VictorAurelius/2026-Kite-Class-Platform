# GAP-931: Admin login first-click fails (no 2FA prompt); second click succeeds

**Status:** 🔵 OPEN
**Priority:** 🟡 P2 (UX bug — works on retry, doesn't block admin from accessing system; cosmetic per re-walk standards)
**Domain:** Frontend (likely; needs investigation to confirm)
**Found:** 2026-06-04 (Wave flow-kh1 G2 re-walk session, user feedback during 5-bug-fix verify cycle: "khi tôi bấm đăng nhập lần đầu thì lỗi không 2FA, bấm lần nữa thì được")
**Affects:**
- `kitehub/kitehub-frontend/src/app/(auth)/login/page.tsx` (login form submit)
- Possibly `kitehub/kitehub-frontend/src/lib/api/client.ts` (interceptor / token storage)
- Possibly subscription `AuthService` CSRF double-submit if BE-side

## Problem

On the admin login page (`/login`), the user enters `admin@kitehub.com` + password, clicks "Đăng nhập":
- **First click:** an error appears (no 2FA challenge surfaces) — user did not capture the exact error text in the report
- **Second click:** login proceeds normally → redirects to `/2fa-challenge?token=...` → user completes 2FA → admin home

This was observed AFTER the GAP-924 fix shipped (Wave flow-kh1 commit `2b1b3791`). It is NOT a regression of GAP-924 — GAP-924 covered the 2FA-verify step (after credentials accepted). This sits one step earlier, on the initial credentials submit.

## Root Cause Hypotheses (needs investigation)

1. **Stale token in sessionStorage** from a prior session. `apiClient` request interceptor (per GAP-599 Wave 92 Bucket B) attaches `Authorization: Bearer <stale-access-token>` on every request, including `/api/auth/login`. BE may reject the request with 401 because the stale token is malformed or signed by a key that has been rotated since. After the 401 fires, the response interceptor's auth-flow passthrough (per GAP-924 fix) returns the error to the component. The component's catch block shows a generic error. The user clicks again — by then the stale token may have been cleared by `clearLegacyLocalStorageTokens` useEffect on mount, so the second submit succeeds.
2. **CSRF double-submit cookie not set on first request**. If `DoubleSubmitCsrfTokenProvider` requires the CSRF cookie + header to match and the first request lacks the cookie, BE returns 403. The response sets the cookie, so the second request has it and succeeds.
3. **Gateway circuit breaker** trips on `/api/auth/login` for the very first call after a long idle period, but not on the second (per GAP-928 Phase 1 thresholds). The user would not see "circuit triggered" in the UI — they would see "Đăng nhập thất bại" or similar generic message.
4. **Race condition** between `clearLegacyLocalStorageTokens` useEffect and the form's onSubmit handler. First submit fires while sessionStorage still has stale data; useEffect runs synchronously after, clearing it; second submit has clean state.

Quickest empirical test:
- Hard refresh the login page → open DevTools Network → click "Đăng nhập" once → capture (a) request headers (Authorization, Cookie, X-CSRF-*) (b) response status + body. Click "Đăng nhập" a second time without further navigation → capture same. Diff.

## Proposed Fix

Pending root cause — when confirmed, fix shape depends on which hypothesis wins:

- **Hypothesis 1 (stale token)** — `login/page.tsx` useEffect should clear sessionStorage BEFORE the form renders, or `apiClient` should NOT attach `Authorization` header on `/api/auth/login`, `/api/auth/register`, `/api/auth/refresh` requests (those endpoints establish the session — they should never need an existing token).
- **Hypothesis 2 (CSRF)** — ensure the CSRF cookie is fetched on mount (GET /api/auth/csrf or similar) before any login submit; OR change BE to issue the CSRF cookie on first POST and accept the resubmission.
- **Hypothesis 3 (circuit breaker)** — already addressed in GAP-928 Phase 1 + 2. If this persists, file as follow-up to GAP-928 Phase 1 thresholds for login route specifically.
- **Hypothesis 4 (race)** — restructure `login/page.tsx` to gate form submit until clearLegacy useEffect has run, e.g., set a "ready" state.

## Acceptance Criteria

- [ ] Empirical root cause confirmed (DevTools capture in PR body)
- [ ] First-click submit succeeds end-to-end (admin reaches 2FA challenge on first click, no error)
- [ ] Cross-flow sweep per `cross-flow-bug-class-sweep.md` §3: confirm `/api/auth/register` and other auth-form-submit pages don't exhibit the same first-click-fail pattern

## Related

- Discovered in: Wave flow-kh1 G2 re-walk session 2026-06-04 (user feedback after GAP-924 verified)
- Sister: GAP-924 (FE 2FA verify silent UI) — separate flow stage, but same family of "FE auth form needs per-status diagnostics"
- Possibly related to: GAP-599 Wave 92 Bucket B (sessionStorage per-tab JWT isolation)
- Per `pre-handoff-self-test-completeness.md` §2.4 (admin-flow checklist) — login UI works (a)+(b) must succeed on first click, not second
