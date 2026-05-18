# GAP-578: P2 owner 2FA mandatory + new-device email alert

**Status:** 🔵 OPEN
**Priority:** 🔴 P0 (chặn GA Phase 2 — owner role đụng tiền + danh sách HS = high-value)
**Domain:** Backend / Frontend / Security
**Found:** 2026-05-15 (Wave 85 Bucket A persona outside-in audit cell 2.3)
**Affects:** P2 Center Owner authentication flow

## Problem

Wave 85 inside-out scope không cover 2FA cho P2 Center Owner role. Bucket A persona audit cell 2.3 (chị Hằng — Center Owner Sky Education):

- P2 owner expect 2FA mandatory cho owner role — owner đụng tiền (học phí thu) + danh sách học sinh (tài sản cạnh tranh) = high-value target.
- Expect login-from-new-device email alert (industry standard — Google / banking).
- Expect password complexity rule (min 12 chars + mixed case + digit + symbol).

Hiện tại P2 owner login chỉ password — trust gap với paying tenants. Owner credential leak → tenant data + revenue records exposed.

## Root Cause

- Wave 80 RBAC shipped role distinction nhưng KHÔNG enforce per-role auth tier (P1 teacher OK chỉ password; P2 owner cần 2FA).
- Wave 85 scope inside-out focused defense-in-depth data layer (RLS); auth tier hardening = blind spot caught by Bucket A outside-in.

## Proposed Fix

Wave 86 scope (3 sub-tasks):

1. **2FA mandatory cho P2 owner role** — TOTP enroll required trong first login; backup codes; cannot dismiss.
2. **New-device email alert** — track `device_fingerprint` (user_agent + IP CIDR); first-time fingerprint → email alert "Đăng nhập từ thiết bị mới: {device} {city} {time}".
3. **Password complexity rule** — config `kitehub.auth.password-policy` per role; P2 owner: min 12 chars + mixed case + digit + symbol; reject weak passwords on signup/change.

## Acceptance Criteria

- [ ] 2FA mandatory cho P2 owner role — TOTP setup flow trên first login
- [ ] Backup codes generation + recovery flow tested
- [ ] New-device email alert template Vietnamese + tested với Mailtrap (Phase 1 BETA) / Resend (production)
- [ ] Device fingerprinting cookie + IP geo lookup wired
- [ ] Password complexity rule enforce per-role (P2 owner stricter than P1)
- [ ] Integration test: P2 owner login first time → TOTP enrollment forced
- [ ] Integration test: P2 owner login từ new device fingerprint → email alert delivered
- [ ] Pre-handoff verify per `pre-handoff-self-test-completeness.md` §2.4 (login flow)

## Related

- Wave 85 Bucket A persona audit: `documents/04-quality/audits/persona-review/2026-05-15-pre-wave-85-persona-outside-in.md` cell 2.3
- Wave 86 scope (planned)
- `pre-launch-auth-hardening-checklist.md` (parent rule)
- GAP-577 (sister P0 — admin hardening MFA)
- Wave 80 RBAC PARTIAL (role distinction foundation)

## Log

- **2026-05-15** Filed via Wave 85 Bucket A persona outside-in audit integration. Defer Wave 86 — P2 owner trust gap critical pre GA Phase 2 nhưng Wave 85 scope locked. Status OPEN.
- **2026-05-18 — Cross-ref Wave 93 GAP-625 KYC dependency** per Wave 93 re-triage audit (`documents/04-quality/audits/persona-review/2026-05-18-phase-1-5-26-gaps-re-triage.md`). GAP-625 (Phase 1.5a P0 — Owner KYC + bank account ownership verification at QR setup) is **Owner identity verification baseline**. GAP-578 (P2 Owner 2FA + new-device email alert) builds on identity-verified baseline. Recommended sequential ordering: GAP-625 KYC infrastructure ships Phase 1.5a (Wave 31-32) → GAP-578 2FA layer leverages identity-verified Owner record + extends with TOTP/SMS factor. Cross-ref complementary, NOT duplicate (KYC = identity proof; 2FA = continuous auth strengthening). User decision Wave 86 vs paired Phase 1.5a continued in wave plan §6 follow-up.
