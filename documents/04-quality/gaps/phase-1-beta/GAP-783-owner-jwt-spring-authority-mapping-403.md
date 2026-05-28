---
audience: dev
---

# GAP-783 — Owner JWT → Spring Security authority mapping 403 ACCESS_DENIED

**Status:** 🔵 OPEN
**Priority:** 🔴 P0
**Domain:** Backend
**Found:** 2026-05-28 (Wave meta-6 human walk RST cycle)
**Phase:** phase-1-beta

## Problem

Owner JWT (`role: OWNER`, `tenantId: 22003e3c-...`) → POST `/api/v1/staff-invitations` qua gateway → `kiteclass-core/StaffInvitationController` → **HTTP 403 ACCESS_DENIED**.

Controller declares: `@PreAuthorize("hasAnyRole('ADMIN','OWNER','PLATFORM_ADMIN')")` — listet OWNER nhưng check fails.

```bash
curl -X POST http://localhost:9000/api/v1/staff-invitations \
  -H "Authorization: Bearer <OWNER_JWT>" \
  -H 'Content-Type: application/json' \
  -d '{"email":"staff.test@test.vn","fullName":"Thầy Tâm","role":"TEACHER"}'
# → 403 {"code":"ACCESS_DENIED","message":"You do not have permission..."}
```

## Root Cause

Spring Security `hasRole('X')` requires `GrantedAuthority` value `ROLE_X` (prefix added implicitly). When JWT filter converts `role` claim → authority, the `ROLE_` prefix may be missing:
- JWT contains: `"role": "OWNER"`
- Spring authority created: `OWNER` (no prefix)
- `hasRole('OWNER')` checks for `ROLE_OWNER` → not found → 403

**Recurrence (2nd occurrence — same class):**
- Wave 71b admin login 500 (2026-05-13): seeded BE role `PLATFORM_ADMIN` vs FE guard literal `'ADMIN'` mismatch
- Wave meta-6 walk (2026-05-28): JWT role `OWNER` vs Spring authority `ROLE_OWNER` mismatch

Per `incident-to-rule-pipeline.md` 5-stage Stage 1 (Detect) ✓ + Stage 2 (Classify) ✓ — recurrence threshold ≥2 met. Rule candidate filed trong follow-up META gap.

## Proposed Fix

**Option A (recommended — controller-level)**: change `@PreAuthorize` syntax to `hasAnyAuthority('OWNER','ADMIN','PLATFORM_ADMIN')` — bypasses Spring's implicit `ROLE_` prefix requirement. 3 lines change trong `StaffInvitationController.java:70+93+106`.

**Option B (JWT filter-level)**: update gateway/kiteclass-core JWT-to-authority converter để add `ROLE_` prefix consistently. Broader scope nhưng fixes recurring pattern. Pair với meta rule "JWT role → Spring authority mapping audit at controller addition time".

**Option C (both)**: Option A short-term fix Wave meta-6 walk unblock, Option B long-term consistent mapping.

## Acceptance Criteria

- [ ] Owner JWT POST `/api/v1/staff-invitations` returns 201 (not 403)
- [ ] Owner JWT POST + DB row inserted với correct tenant + role + token
- [ ] Owner JWT POST + email sent to MailHog
- [ ] Existing PLATFORM_ADMIN access unchanged
- [ ] Controller IT extension: mock JWT role=OWNER → @PreAuthorize pass
- [ ] Decision Option A/B/C logged

## Related

- Sister incident: 2026-05-13 admin login 500 (Wave 71b GAP-518/519)
- Sister rule: `pre-handoff-self-test-completeness.md` §2.4 admin-flow (a) role match BE seed vs FE guard
- RST artifact: `documents/04-quality/audits/rst-html/2026-05-28-wave-meta-6-human-walk-rst.md` Finding #8
- Wave meta-6 BE PR #1904 ship Status: shipped but functional Owner walk path broken

## Log

- **2026-05-28** — Found qua Wave meta-6 RST human walk. P0 blocks Owner walk + staff-invite feature end-to-end. Fix scope ~10 phút (Option A) → unblocks walk completion. Per `meta-gap-priority.md` §3 — META P0 force-multiplier if Option B chosen (mapping audit rule eliminates class).
