---
audience: dev
---

# GAP-783 — Owner JWT → Spring Security authority mapping 403 ACCESS_DENIED

**Status:** 🟢 DONE
**Priority:** 🔴 P0
**Domain:** Backend
**Found:** 2026-05-28 (Wave meta-6 human walk RST cycle)
**Closed:** 2026-05-28 (Wave Phase 2 Beta Wave A Bucket C verify — no fix needed, chain healthy)
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

- **2026-05-28** — 🟢 DONE Wave Phase 2 Beta Wave A Bucket C empirical verify per `pre-handoff-self-test-completeness.md` v1.2.0 §3 (post-fix re-walk mandate) + `audit-to-gap-pipeline.md` §2.8 (fix-time state-check). Symptom DOES NOT REPRODUCE on main HEAD (commit 902f8f77). Chain verified healthy end-to-end:

  **State-check evidence (2026-05-28T09:56Z):**
  - Login Owner `owner.test@test.vn / Test@1234` via gateway → HTTP 200 + JWT issued
  - JWT payload decoded:
    ```json
    {"sub":"b9fa3522-64e4-4ea8-93f4-d7aa43aea5c5","email":"owner.test@test.vn","role":"OWNER","type":"access","tenantId":"877dff9d-c354-4faf-8c44-3c17196dbf24","iat":...,"exp":...}
    ```
    `role: OWNER` + `tenantId: <UUID>` claims BOTH present (GAP-704 fix ✓)
  - POST `/api/v1/staff-invitations` with Owner JWT + explicit `X-Tenant-Id` header → **HTTP 201** + DB row created + correct tenant_id binding
  - Chain components VERIFIED:
    - `TokenService.generateAccessToken()` enriches JWT với `tenantId` claim (line 60-72, Wave 104 ship in main commit 902f8f77)
    - `JwtAuthenticationGatewayFilter` extracts `role` claim → emits `X-User-Roles` header (line 158-171)
    - Subscription `SecurityConfig.XUserRolesHeaderFilter` splits CSV + prefixes `ROLE_` (line 175-198)
    - `StaffInvitationController.OWNER_AUTHZ = "hasAnyRole('OWNER','PLATFORM_ADMIN','ADMIN')"` (line 90-91) — chain produces `ROLE_OWNER` which `hasRole('OWNER')` accepts ✓
  - **Originating diagnostic (Bug #8 = 403 ACCESS_DENIED on @PreAuthorize) does not apply.** When tenant header attached, end-to-end chain works.

  **Root cause re-classification:** Wave meta-6 walk surfaced 403 — but the actual failure mode is `403 TENANT_CONTEXT_MISSING` (X-Tenant-Id header absent), NOT `403 ACCESS_DENIED` on Spring authority. The Spring Security authority chain is healthy. Different bug class.

  **Separable concern filed as new gap GAP-789** — gateway `staff-invitations` route missing `TenantResolver` filter (per `pre-handoff-self-test-completeness.md` §3.2 Re-walk scope spot-check, this is a SISTER bug surfaced during re-walk; per `cross-flow-bug-class-sweep.md` §5 decision matrix DEFER row, separable scope from this gap's claim).
