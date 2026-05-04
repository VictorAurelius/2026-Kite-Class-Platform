# GAP-286: Mobile OTP signup via Zalo/SMS

**Status:** 🔵 OPEN
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

- [ ] Mobile signup flow ≤10 phút wall-clock từ landing → dashboard ready
- [ ] OTP gửi qua Zalo OA (primary) với SMS fallback (secondary)
- [ ] Phone number format VN validated (`0\d{9,10}`)
- [ ] Rate limit 3 OTP requests / 15 phút / phone
- [ ] Tenant TRIAL provisioning sub-30s từ OTP verify → first login redirect
- [ ] E2E test: Playwright mobile viewport simulating iPhone 13 Safari
- [ ] Cost telemetry: track Zalo ZNS vs SMS unit cost per signup

## Related

- AC-ONBOARD-001 (P1 review report 2026-05-04)
- GAP-063 (SMS/Zalo notification — class context, not signup; this gap is signup-specific)
- GAP-287 (Skip wizard — paired UX fix)
- Wave 17 Bucket A — P1 Solo Teacher review

## Log

- **2026-05-04** — Filed by Wave 17 Bucket A Agent during P1 Solo Teacher persona review Round 1. State-check confirmed no existing implementation. Reserved range GAP-286..295.
