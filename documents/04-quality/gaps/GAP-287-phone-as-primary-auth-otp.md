# GAP-287: Phone as primary auth + OTP via SMS/Zalo

**Status:** 🔵 OPEN
**Priority:** 🔴 P0
**Domain:** Backend (auth) + Frontend (signup) + Integration (SMS/Zalo)
**Found:** 2026-05-04 (Wave 17 Bucket A — P1 Solo Teacher Round 1 review)
**Affects:** P1 Solo Teacher (mobile-first signup), Pa Parent (Zalo-first communication), eventually all VN-market personas

## Problem

P1 AC-ONBOARD-001 requires: "Teacher có thể signup nhập email + phone + tên + chọn role → nhận OTP Zalo/SMS → confirm → vào dashboard." Current `register/page.tsx` only collects email + password + hCaptcha; no phone field, no OTP flow. Email-as-primary-identifier is a desktop-era pattern; VN solo tutor / parent users primarily on mobile with phone numbers as their stable identifier (email often inactive or shared).

## Root Cause

Auth originally designed for center owner persona (where email is professionally maintained). Phone + OTP infrastructure (SMS gateway / Zalo OA) never integrated.

## Proposed Fix

1. Backend: `kitehub-platform` extend User entity with `phone` (E164 format) + `phoneVerified` columns; migration.
2. Backend: implement OTP service — generate 6-digit code, store in Redis with TTL 5 min, expose `/auth/otp/request` + `/auth/otp/verify` endpoints.
3. Integration: SMS gateway (suggest StringeeAPI or eSMS for VN market); Zalo OA (Official Account) for free push to Zalo users (per Zalo Mini App / OA TOS).
4. Frontend: `register/page.tsx` adds phone field + OTP step; `login/page.tsx` adds "Login with phone + OTP" alternative.
5. Per `feedback_phone_otp_dispatch_strategy.md` (if exists; if not, file as memory entry post-implementation): try Zalo first (free if user is Zalo OA follower), fallback to SMS (paid).

## Acceptance Criteria

- [ ] User entity has `phone` + `phoneVerified` columns + migration
- [ ] `/auth/otp/request` (rate-limited 1/min/phone, 5/hour/phone) + `/auth/otp/verify` endpoints
- [ ] SMS gateway integration (StringeeAPI or equivalent) — production credentials secured per `business-logic-review.md` §2.4
- [ ] Zalo OA integration (best-effort, fallback SMS)
- [ ] Frontend signup includes phone field + OTP step (≤5 form fields total per AC)
- [ ] Frontend login alternative "phone + OTP" available
- [ ] OTP delivery success rate ≥95% in 30s (SLO documented)
- [ ] Tests: rate-limit, OTP expiry, retry logic, fallback path

## Related

- Review: [`documents/00-brd/persona-reviews/P1-solo-teacher-round-1-2026-05-04.md`](../../00-brd/persona-reviews/P1-solo-teacher-round-1-2026-05-04.md) §1
- AC: AC-ONBOARD-001
- Sibling: GAP-063 (SMS/Zalo notification — overlapping infrastructure; share gateway dependency)
- Sibling: GAP-286 (solo onboarding flow — consumes this OTP step)

## Log

- 2026-05-04 — Created from Wave 17 Bucket A. State-check: `registerSchema` has no phone, `endpoints.auth.register` accepts only ownerEmail+ownerPassword (`register/page.tsx:39-44`). No SMS/Zalo gateway code in `kitehub-platform` or `kiteclass-core`. Build-from-scratch.
