# GAP-924: FE 2FA verify form không show error khi 401 + Authorization header có thể missing

**Status:** 🔵 OPEN
**Priority:** 🟠 P1 (user-facing — silent fail blocks admin login flow)
**Domain:** Frontend
**Found:** 2026-06-04 (Wave flow-kh1 G2 handoff — user-flagged "ko login được admin bằng mã" + "UI ko trả ra thông báo lỗi")
**Affects:**
- `kitehub-frontend/src/app/(auth)/login/2fa-verify/**` hoặc tương đương (2FA verify form FE)
- Indirect: KH-2a admin auth flow G2 test (blocking user G2)

## Problem

User G2 test 2026-06-04 reported login admin với 6-digit TOTP fail 2 vấn đề concatenated:

1. **API request 401**: User curl từ browser DevTools "Copy as cURL" cho thấy `POST /api/auth/2fa/verify` với body `{challenge_token, totp_code}` → 401 Unauthorized
2. **UI silent**: FE form KHÔNG render error message khi 401 → user không biết tại sao fail, không có hint về expired token / wrong code / missing header

## Root Cause (partial — investigation needed)

### Issue 1: Authorization header behavior

`ChallengeTokenAuthenticationFilter` (subscription-side) yêu cầu `Authorization: Bearer <challenge_token>` HEADER trên `/api/auth/2fa/verify` để set Spring SecurityContext (per filter source line 78-87). Without Bearer header → context empty → Spring Security entry point returns 401.

User's captured curl shows:
- ✅ Body contains `challenge_token` field
- ❌ Authorization header **missing**

Possible causes:
- (a) FE actually doesn't include Authorization header — bug — fix: FE add `Authorization: Bearer ${challengeToken}` before submitting
- (b) Browser DevTools "Copy as cURL" stripped Authorization (less likely)
- (c) Session storage cleared between login + verify — FE lost challenge_token reference

### Issue 2: Challenge token TTL (cosmetic UX)

Challenge tokens have 5-minute TTL (verified empirically: iat=03:22:15 exp=03:27:15 = 5 min). If user delays entering TOTP code > 5 min, token expires → 401. Currently FE doesn't:
- Display countdown timer cho 5-min window
- Detect expired token + trigger "session expired, login again" flow
- Re-issue fresh challenge automatically

### Issue 3: Silent UI failure (PRIMARY bug)

When 401 returns, FE form should display tiếng Việt error:
- 401 với `purpose:TWO_FACTOR_VERIFY` not found → "Phiên xác thực đã hết hạn, vui lòng đăng nhập lại"
- 401 với invalid TOTP code → "Mã xác thực không đúng. Vui lòng thử lại với mã mới"
- 401 generic → "Xác thực thất bại"

Per `pre-handoff-self-test-completeness.md` §2.4 admin auth checklist:
- (b) Login API works (curl) — verifies BE
- (c) Login UI works — verifies FE shows actual state

Issue 3 violates (c) — UI silent on failure = user blind.

## Proposed Fix

### Phase 1 (immediate — Issue 3):
- FE 2FA verify form catch 401 response → display error toast/banner với Vietnamese message
- Per-error-code mapping (challenge expired / invalid code / generic)

### Phase 2 (verify Issue 1):
- Empirical: FE log Network tab → confirm if Authorization header sent
- If missing → add header `Authorization: Bearer ${challengeToken}` to verify request
- Document FE auth flow contract trong `documents/01-business/kitehub/auth-2fa/`

### Phase 3 (Issue 2 — UX):
- Display countdown timer (4:59 → 0:00) cho TOTP entry window
- Auto-redirect to login khi token expires + show "Session expired" message

## Acceptance Criteria

- [ ] Phase 1: FE 2FA verify form renders Vietnamese error message on 401 (challenge expired / invalid code / generic)
- [ ] Phase 2: FE always sends `Authorization: Bearer <challenge_token>` header along with body
- [ ] Phase 3: Countdown timer + auto-redirect on expiry
- [ ] Re-walk G2 admin login → user can complete TOTP entry + see error UI on fail
- [ ] Cross-flow sweep: check `/api/auth/2fa/enroll-init` + `/api/auth/2fa/enroll-confirm` for same FE silent-401 pattern

## Related

- Discovered in: Wave flow-kh1 G2 handoff session 2026-06-04 (user "ko login được admin bằng mã" + "UI ko trả ra thông báo lỗi")
- BE works correctly: subscription `ChallengeTokenAuthenticationFilter.java` source + endpoint verified empirical
- Sister: GAP-917 (login sad path 400 vs 401 spec drift — different endpoint similar UI pattern)
- Per `pre-handoff-self-test-completeness.md` §2.4 admin-flow checklist (c) FAIL
- Memory implication: 2FA testing in subsequent G2 sessions should include UI error-rendering verification
