# GAP-286: Mobile OTP signup via Zalo/SMS

**Status:** 🟡 PARTIAL (~40%) — backend OTP core + 3-layer docs DONE 2026-06-21; FE mobile form + live ZNS/SMS + fast-provisioning + E2E remain
**Priority:** 🔴 P0 — blocks P1 Solo Teacher onboarding (AC-ONBOARD-001 FAIL)
**Domain:** Backend (auth) + Frontend (register flow) + KiteHub provisioning
**Found:** 2026-05-04 (Wave 17 P1 Solo Teacher persona review — Round 1)
**Affects:** P1 Solo Teacher signup; P2 Small Center owner signup (likely shared finding); all mobile-first signup flows

## Problem

Solo gia sư signup PHẢI mobile-only (iPhone Safari / Android Chrome). Theo AC-ONBOARD-001:
- ≤10 phút từ click "Sign up" → dashboard ready
- OTP gửi qua Zalo hoặc SMS (không phải email — phụ huynh + học sinh KHÔNG dùng email VN context)
- Phone-based signup (không phải email-only)

**State-check (verified 2026-05-04):**
- `kiteclass-frontend/src/app/(auth)/register/page.tsx` exists nhưng chỉ email + password form (no OTP step)
- Grep `OTP|sms.?verify|zalo.*verify|phone.?verify` ở `kiteclass-core/src/main/java` = 0 hits ngoài `ParentInvitationController` (parent invite cho K-12, không phải tenant signup)
- KHÔNG có Zalo OA / SMS gateway integration ở `kitehub-subscription`
- Tenant provisioning trong `kitehub-subscription/scheduler/TrialExpirationChecker.java` async — instance ready time không guaranteed <10 phút wall-clock

## Root Cause

Signup flow ban đầu thiết kế cho web-first email/password (B2B SaaS desktop). Mobile-first persona (P1, P2) emerged trong Round 1 review nhưng auth flow chưa adapt. Zalo/SMS OTP integration là feature gap cross-cutting với GAP-063 (SMS/Zalo notification) nhưng signup OTP là USE CASE riêng (KHÔNG phải class notification).

## Proposed Fix

1. **Backend (kitehub-subscription):**
   - Add SMS gateway adapter (eSMS.vn / VNotify / Twilio fallback) per `design-patterns.md` Strategy
   - Add Zalo OA messaging adapter (Zalo OA Notification Service template ZNS)
   - `OtpService` với 6-digit code, 5-min TTL, rate-limit 3/15min per phone
   - New endpoint `POST /api/v1/auth/signup/request-otp` + `POST /api/v1/auth/signup/verify-otp`
2. **Backend (kiteclass-core):**
   - Tenant fast-provisioning path: TRIAL state với pre-warmed schema → ready <10s
3. **Frontend (kiteclass-frontend / kitehub-frontend):**
   - `app/(auth)/register/mobile/page.tsx` — phone-first form
   - OTP input component (6-digit, autofill from SMS web-otp API)
   - Show wizard branding skip button (xem GAP-287)

## Acceptance Criteria

### Backend OTP core ✅ DONE 2026-06-21 (kitehub-subscription)
- [x] Phone number format VN validated (`0\d{9,10}`) — `OtpService`
- [x] Rate limit 3 OTP requests / 15 phút / phone — sliding-window, typed 429 result
- [x] `OtpService` 6-digit + bcrypt hash + 300s TTL + max-5-verify-attempts + single-use
- [x] `POST /signup/request-otp` + `POST /signup/verify-otp` + `signupToken` (10-min, HS256, mirror twofactor) + ProblemDetail errors
- [x] 3-layer business docs — `documents/01-business/kitehub/signup-otp/{rules,use-cases,api-contract}.md`
- [x] Tests: 7 `OtpServiceTest` + 5 `OtpControllerTest` green; `mvnw -pl kitehub-subscription test-compile` EXIT 0
- [~] OTP gửi qua Zalo OA (primary) + SMS fallback — **mock delivery** (`OtpDeliveryService` logs `[OTP-MOCK]`); live ZNS/SMS = Phase 2 (GAP-063 vendor-blocked)

### Remaining (follow-up — FE / vendor / infra)
- [ ] FE `app/(auth)/register/mobile/page.tsx` phone-first form + 6-digit OTP input (Web-OTP autofill) — wires to new endpoints
- [ ] Mobile signup flow ≤10 phút wall-clock landing → dashboard (needs FE + fast-provisioning)
- [ ] Tenant TRIAL provisioning sub-30s từ OTP verify → first login redirect (separate sub-task)
- [ ] E2E Playwright mobile viewport (iPhone 13 Safari) — needs FE
- [ ] Live ZNS/SMS delivery + cost telemetry (Zalo ZNS vs SMS unit cost) — Phase 2 vendor-blocked

## Related

- AC-ONBOARD-001 (P1 review report 2026-05-04)
- GAP-063 (SMS/Zalo notification — class context, not signup; this gap is signup-specific)
- GAP-287 (Skip wizard — paired UX fix)
- Wave 17 Bucket A — P1 Solo Teacher review

## Log

- **2026-06-21** — Status OPEN → 🟡 PARTIAL (~40%). **Backend OTP core shipped** (kitehub-subscription, mirror `passwordreset` pattern): `OtpService` (6-digit, bcrypt-hashed `ConcurrentHashMap` store + `// TODO Phase 2: Redis`, 300s TTL, 3-req/15min sliding-window rate-limit, max-5-verify-attempts, single-use, VN phone `^0\d{9,10}$`, injectable `Clock`) + `SignupTokenService` (10-min HS256, mirror `twofactor/ChallengeTokenService`) + `OtpDeliveryService` (MOCK — logs `[OTP-MOCK]`, no vendor call) + `OtpController` (`POST /api/v1/auth/signup/request-otp` + `verify-otp`, ProblemDetail) + config `kitehub.auth.signup-otp.*`. Tests: 7 `OtpServiceTest` + 5 `OtpControllerTest` green; `mvnw -pl kitehub-subscription test-compile` EXIT 0. **3-layer business docs** written (`01-business/kitehub/signup-otp/`, rules.md born-compliant 5-attribute). Notification dispatch = mock-log fallback (`OwnerNotificationDispatcher` email-centric, `ZALO` channel unwired stub → Phase 2 wire). **Remaining:** FE mobile signup page + 6-digit input, live ZNS/SMS (GAP-063 Phase 2 vendor), fast-provisioning sub-30s, Playwright E2E, cost telemetry. Built via 1 Opus agent + coordinator inline (3-layer docs) per `agent-concurrency-budget-inline-hybrid`.
- **2026-05-04** — Filed by Wave 17 Bucket A Agent during P1 Solo Teacher persona review Round 1. State-check confirmed no existing implementation. Reserved range GAP-286..295.
