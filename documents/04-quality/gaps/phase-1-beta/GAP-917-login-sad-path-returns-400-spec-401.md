# GAP-917: Login sad path (wrong password) trả HTTP 400 thay vì 401 INVALID_CREDENTIALS

**Status:** 🔵 OPEN
**Priority:** 🟡 P2
**Domain:** Backend (Auth)
**Found:** 2026-06-03 (Wave flow-kh2 walk Bucket A)
**Affects:** `kitehub-subscription/src/main/java/com/kitehub/subscription/service/AuthService.java` login() + global exception handler

## Problem

Walk KH-2 S3 sub-step sad path: `POST /api/auth/login` với password sai → HTTP 400 `{"type":"about:blank","title":"Bad Request","status":400,"detail":"Invalid email or password","instance":"/api/auth/login"}`.

Spec mismatch:
- `documents/01-business/kitehub/auth/use-cases.md` UC-AUTH-001 §"Failure paths" yêu cầu **HTTP 401** `{error: "INVALID_CREDENTIALS"}` cho wrong password + user not found (uniform để chống username enumeration)
- `documents/01-business/kitehub/auth/rules.md` BR-AUTH-001/002 spec 401 cho wrong credentials, 423 LOCKED cho lockout

Actual code thrown `IllegalArgumentException("Invalid email or password")` → Spring's default exception handler render thành HTTP 400 (Bad Request). Chưa map sang 401.

Impact:
- FE phân biệt "field validation error" (400) vs "auth fail" (401) sai → có thể trigger wrong UX (vd hiển thị "form invalid" thay vì "email/mật khẩu sai")
- Audit trail + security monitoring rule based on 401 count cho brute-force detection có thể miss

Severity P2 (không phải P0/P1): functional flow OK (user nhận error message rõ), nhưng spec drift cần align cho production-readiness.

## Proposed Fix

- Tạo dedicated exception (`InvalidCredentialsException` extends `RuntimeException`)
- Throw từ `AuthService.login()` thay vì `IllegalArgumentException`
- Global exception handler map → HTTP 401 + body `{error: "INVALID_CREDENTIALS"}` per RFC 7807 ProblemDetail format
- Cùng class cho user-not-found path (uniform response prevent enumeration)
- Same fix cho captcha-fail trong register: 400 OK (validation) → giữ
- Update integration tests: verify 401 vs 400

## Acceptance Criteria

- [ ] `POST /api/auth/login` wrong password → HTTP 401 + `{error: "INVALID_CREDENTIALS"}`
- [ ] `POST /api/auth/login` user not found → HTTP 401 + same body (no enumeration leak)
- [ ] BR-AUTH-001 lockout case vẫn 423 LOCKED (không degrade)
- [ ] Integration test cover 3 cases (wrong pw, no user, locked)
- [ ] FE auth error handling vẫn render đúng tone tiếng Việt ("Email hoặc mật khẩu không đúng")

## Related

- Discovered in: Wave flow-kh2 walk (Blocker #3)
- BR-AUTH-001 / BR-AUTH-002 / UC-AUTH-001 reference
- Sister: GAP-916 (gateway onboarding 401 — different layer)
