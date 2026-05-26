---
status: OPEN
priority: P1
category: Backend
phase: phase-1-beta
progress: 0
created: 2026-05-26
updated: 2026-05-26
wave: beta-prep-1-bucket-A-followup
---

# GAP-755 — PDPL consent BE persistence integration (Wave beta-prep-1 Bucket A follow-up)

## Problem

Wave beta-prep-1 Bucket A (2026-05-26) shipped FE consent capture (3 granular checkboxes in `BetaSignupForm.tsx`) + PDPL legal docs (privacy-notice, ToS, data-retention-policy, breach-notification-sop). FE submits `consent: { tosPrivacy, marketing, analytics, version, signedAt }` trong payload `/api/v1/auth/beta-signup` request.

BE persistence integration NOT shipped same-PR vì:
- V56 `consent_record_immutable` table đã có Wave br-4 với immutable hash chain
- ConsentService API/integration logic chưa wire vào BetaSignupController
- Bucket A scope hard-bounded ~45min — out-of-scope BE Spring service implementation

Without BE integration:
- FE consent state collected but DROPPED tại BE controller (`completeBetaSignup` ignore `consent` payload field)
- PDPL Art 11 informed-consent audit trail GAP — vi phạm "consent persist immutable" requirement

## Acceptance Criteria

- [ ] `BetaSignupController.completeBetaSignup` accept + validate `consent` payload field per DTO
- [ ] Validate `tosPrivacy == true` ELSE 400 (BE-side defense — FE already enforces but BE must validate independently)
- [ ] On success: insert row vào `consent_record_immutable` table với:
  - `user_id` = newly-created user ID
  - `tenant_id` = newly-provisioned tenant ID
  - `granted` JSONB: `{"essential":true, "tos_privacy":true, "marketing":<bool>, "analytics":<bool>, "version":"v0.9.0-beta", "signed_at":"<iso>"}`
  - `ip_address`, `user_agent` từ `HttpServletRequest`
  - `prev_hash`, `current_hash` per existing ConsentService hash chain logic
- [ ] Audit log entry trong `admin_audit_log` cho consent grant event
- [ ] Integration test PostgresIT verify row insert + hash chain integrity
- [ ] Update `documents/01-business/cookie-consent/api-contract.md` với consent_record endpoints if new

## Proposed Fix

**Path A — Reuse existing ConsentService:**
1. Inspect existing `kitehub-subscription/.../consent/ConsentService.java` (Wave br-4)
2. Add method `recordSignupConsent(SignupConsentDto consent, Long userId, Long tenantId, HttpServletRequest req)`
3. Wire vào `BetaSignupServiceImpl.complete(...)` after tenant provisioning success
4. PostgresIT (NOT Mockito — per `postgres-specific-type-testcontainers.md`) cho INET binding + JSONB serialization + hash chain

**Path B — Sister rule fire `local-fix-production-parity-check.md`:**
- FE consent payload added → BE controller MUST accept hoặc reject explicitly (no silent drop)
- This gap covers BE parity

## References

- Wave beta-prep-1 Bucket A PR (this wave) — FE checkboxes + legal docs shipped
- `kitehub/kitehub-subscription/src/main/resources/db/migration/V56__create_consent_record_immutable.sql` — immutable consent table (Wave br-4 GAP-353b)
- `documents/01-business/cookie-consent/rules.md` BR-COOKIE-001..004 — cookie consent BRs (related but different domain — cookie vs signup consent)
- `documents/01-business/legal/privacy-notice.md` §4 — Quyền PDPL Art 11 truy cập đồng ý
- `.claude/rules/local-fix-production-parity-check.md` §2 row 3 — FE consent capture → BE persistence parity
- PDPL Art 11 — informed consent + persist immutable

## Wave context

- **Wave beta-prep-1 Bucket A:** shipped 4 items (privacy + ToS + retention + breach SOP + FE consent checkboxes)
- **PDPL deadline:** 2026-07-01 (~5 tuần from Bucket A)
- **Phase 1 BETA gate:** Quality ≥80/100 — consent persistence required for compliance ≥80 path

## Effort estimate

~2-3 giờ:
- 30min inspect ConsentService Wave br-4 implementation
- 60min wire consent recording vào BetaSignupServiceImpl
- 60min PostgresIT + Mockito unit test
- 30min update api-contract.md + verify integration

## Cascade

- Future: ConsentWithdrawalEndpoint cho right-to-be-forgotten (PDPL Art 11) — separate gap
- Future: Admin dashboard consent history view — separate gap
