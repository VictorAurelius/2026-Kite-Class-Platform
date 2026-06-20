# GAP-1500: OTP signup endpoints thiếu gateway IP rate-limit → SMS/Zalo bombing + cost-amplification DoS (latent)

**Status:** 🔵 OPEN
**Priority:** 🟡 P2
**Domain:** Backend
**Found:** 2026-06-21 (security full audit — AUTH-007 / F-005)
**Affects:** `kitehub-gateway` route layer, mobile-OTP signup flow (GAP-286)

## Problem

2 endpoint OTP signup mới (GAP-286, shipped #2515) — `POST /api/v1/auth/signup/request-otp` + `POST /api/v1/auth/signup/verify-otp` — KHÔNG có gateway `RequestRateLimiter` filter. Grep `kitehub/kitehub-gateway/src/main/resources/application.yml` cho `signup/request-otp` = 0 hit; gateway chỉ rate-limit auth-register/login/refresh/verify-email/resend + beta-signup (GAP-514/509), chưa thêm OTP route vào matrix.

Bảo vệ duy nhất hiện tại = in-app rate-limit **per-PHONE** trong `OtpService.requestOtp` (`OtpService.java:121-131`): 3 request/15min/phone, lưu trong `ConcurrentHashMap` **in-memory per-instance** (`// TODO Phase 2: Redis`, L68-71).

Lỗ hổng:
- **Phone rotation:** attacker xoay vòng số điện thoại (mỗi số mới = 3 OTP delivery miễn phí) → spam OTP dispatch không bị giới hạn per-IP.
- **Cost-amplification DoS (latent):** khi live ZNS/SMS delivery wired (Phase 2 — GAP-063 vendor-blocked), mỗi OTP gửi tốn tiền (Zalo ZNS / SMS unit cost) → phone-rotation = bill bombing. Hiện delivery = MOCK (`OtpDeliveryService` chỉ log `[OTP-MOCK]`, không call vendor) → cost-DoS **latent**, chưa exploitable về chi phí.
- **Multi-instance bypass:** in-memory store không shared → nếu scale >1 kitehub-subscription instance, per-phone limit bypass-able (Phase 1 = single-instance per ADR-025 → chấp nhận hiện tại; là rủi ro Phase 1.5+ khi scale).

`pre-launch-auth-hardening-checklist.md` §2.1 mandate gateway `RequestRateLimiter` trên MỌI `/api/v1/auth/**` endpoint — 2 OTP route mới chưa được thêm vào matrix nên là coverage gap (regression của checklist khi GAP-286 ship).

OTP **brute-force** thì đã được chặn tốt (6-digit BCrypt-hashed, max-5-verify-attempts, 300s TTL, single-use, SecureRandom) → space 1M, ≤15 guess/15min = negligible. Gap này CHỈ về OTP-dispatch flooding (delivery spam / cost), KHÔNG về code-guessing.

## Root Cause

GAP-286 mobile-OTP ship in-app per-phone rate-limit nhưng bỏ qua gateway IP rate-limit layer (defense-in-depth standard cho mọi auth endpoint per checklist §2.1). Route mới không được thêm vào gateway rate-limit matrix khi GAP-286 merge.

## Proposed Fix

1. Thêm gateway route `signup-otp` trong `kitehub-gateway/application.yml` với `RequestRateLimiter` (ipKeyResolver, replenishRate 1-2/sec, burstCapacity 3-5), đặt TRƯỚC catch-all `kitehub-auth-v1` (giống pattern beta-signup GAP-509/514).
2. (Phase 2, đồng bộ live delivery) back OTP store + rate-limit bằng Redis (native TTL + multi-instance shared + survive restart) per `OtpService` TODO.
3. (Optional Phase 1.5) cân nhắc CAPTCHA / per-IP cap riêng cho `request-otp` khi live ZNS/SMS wired.

## Acceptance Criteria

- [ ] `/api/v1/auth/signup/request-otp` + `/verify-otp` có gateway `RequestRateLimiter` (ipKeyResolver) trong `application.yml`, precede catch-all
- [ ] Gateway rate-limit fire 429 khi vượt ngưỡng per-IP (verify qua smoke/RST)
- [ ] (Phase 2) Redis-backed OTP store khi live delivery wired — tracked riêng dưới GAP-286 Phase 2 hoặc GAP-063

## Related

- Discovered in: security full audit 2026-06-21 (`documents/04-quality/audits/security/2026-06-21-security-full-audit.md` §5 AUTH-007, F-005)
- Parent surface: GAP-286 (mobile-OTP signup — PARTIAL ~60%)
- Vendor dependency: GAP-063 (Zalo notification — Phase 2 vendor-blocked; live delivery makes cost-DoS exploitable)
- Standard: `.claude/rules/pre-launch-auth-hardening-checklist.md` §2.1 (gateway RequestRateLimiter mandate cho mọi /api/v1/auth/**)
- Precedent: GAP-514 (auth endpoint rate-limit), GAP-509 (beta-signup dedicated route)
