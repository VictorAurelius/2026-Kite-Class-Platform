# RBAC Roles — API Contract

**Domain:** Role-Based Access Control role definitions (Wave 79 Bucket B — GAP-562 Phase 1 MVP 2-role)
**Source-of-truth:** Implementation across multiple modules — role values used trong JWT claims + `@PreAuthorize` annotations cross-cutting domain.
**Last verified:** 2026-05-14 (Wave 79 Bucket 0 Foundation)

This contract documents role-related contract surface across endpoints (NOT a single controller — role taxonomy is cross-cutting). Wave 79 Bucket B target: staff invitation endpoints (separate contract dưới) + role enforcement annotations.

> **Note:** Role taxonomy chưa có dedicated `RoleController`. Phase 1 BETA role assignment qua existing auth endpoints (registration → OWNER for tenant creator; staff invitation → STAFF). Future Phase 2+ Manager + Teacher + Accountant + Receptionist roles sẽ cần dedicated role-management endpoints — file này serves as scaffold cho future expansion.

---

## Role taxonomy reference

Per BR-ROLE-001 (Phase 1 MVP):

| Role | Scope | Source | JWT claim format |
|------|-------|--------|------------------|
| `OWNER` | Tenant-scoped Customer (full access) | Registration; migration from `PLATFORM_ADMIN` (tenant_id ≠ null) | `{ "role": "OWNER", "tenantId": "uuid", ... }` |
| `STAFF` | Tenant-scoped Staff (restricted; no billing/branding) | Staff invitation flow | `{ "role": "STAFF", "tenantId": "uuid", ... }` |
| `PLATFORM_ADMIN` | Cross-tenant superuser | Manual DB seed (admin@kitehub.me) | `{ "role": "PLATFORM_ADMIN", "tenantId": null, ... }` |

Backward-compat aliases until 2026-06-14 (BR-ROLE-002):
- Existing JWT với `role=PLATFORM_ADMIN` + `tenant_id ≠ null` → treat as `OWNER` for tenant-scoped checks.
- FE `RoleGuard` accept `['PLATFORM_ADMIN', 'ADMIN', 'OWNER']` cho Owner UI surface.

---

## Endpoints related to roles (cross-reference)

### Staff invitation endpoints — Wave 79 Bucket B target

Full contract documented separately tại implementation time. UC reference: UC-ROLE-STAFF-INVITE trong `use-cases.md`. Endpoints overview:

| Method | Path | Use case |
|--------|------|----------|
| POST | `/api/v1/staff-invitations` | Owner create invitation |
| GET | `/api/v1/staff-invitations/{token}` | Recipient fetch invite details |
| POST | `/api/v1/staff-invitations/{token}/accept` | Recipient accept + set password |
| GET | `/api/v1/staff-invitations` | Owner list pending + active staff |
| POST | `/api/v1/staff-invitations/{id}/resend` | Owner resend invitation email |
| DELETE | `/api/v1/staff-invitations/{id}` | Owner revoke pending OR disable active staff |

MSW handler: `kitehub-frontend/src/test/msw/handlers/staff-invitations.ts` (Wave 79 Bucket 0 Foundation — this PR).

**Auth model:**
- POST / GET list / DELETE: require `@PreAuthorize("hasRole('OWNER')")` + access token.
- GET `/{token}` / POST `/{token}/accept`: public (token là auth proof).

**Schema:** documented in MSW handler implementation (Bucket 0); typed TS schema co-located với handler.

### Auth endpoints relevant to role (existing)

- `POST /api/auth/login` — issue JWT với `role` claim.
- `POST /api/auth/register` — assign `OWNER` cho tenant creator (Phase 1; future Phase 2 self-service registration without tenant creation).

### Migration endpoint — Wave 79 Bucket B target

None — migration là Flyway V46 (DB-only). Runtime alias xử lý trong `RoleHierarchy` Spring Security bean (config, not endpoint).

---

## Role enforcement matrix (BE)

Bucket B implement `@PreAuthorize` annotation theo BR-ROLE-006:

| Endpoint pattern | Required role | Annotation |
|------------------|---------------|------------|
| `/api/v1/billing/**` | OWNER | `@PreAuthorize("hasRole('OWNER')")` |
| `/api/v1/branding/**` | OWNER | `@PreAuthorize("hasRole('OWNER')")` |
| `/api/v1/ai-branding/**` | OWNER | `@PreAuthorize("hasRole('OWNER')")` |
| `/api/v1/staff-invitations/**` (mgmt) | OWNER | `@PreAuthorize("hasRole('OWNER')")` |
| `/api/v1/students/**` | OWNER, STAFF | `@PreAuthorize("hasAnyRole('OWNER','STAFF')")` |
| `/api/v1/classes/**` | OWNER, STAFF | `@PreAuthorize("hasAnyRole('OWNER','STAFF')")` |
| `/api/v1/admin/**` (cross-tenant) | PLATFORM_ADMIN | `@PreAuthorize("hasRole('PLATFORM_ADMIN')")` |
| `/api/v1/auth/2fa/disable` | not PLATFORM_ADMIN | Service-level check (per BR-AUTH-2FA-005) |

---

## Role enforcement (FE)

Bucket B FE `RoleGuard` component:

```tsx
<RoleGuard allowedRoles={['OWNER']}>
  <BillingPage />
</RoleGuard>
```

Implementation:
- Read JWT claim `role` from access token (decoded client-side).
- If role không match allowed list → redirect `/dashboard` + toast "Bạn không có quyền truy cập trang này".
- Backward-compat alias map: `'PLATFORM_ADMIN' ↔ 'ADMIN' ↔ 'OWNER'` (until 2026-06-14).
- Sidebar nav: conditional render based on role (per BR-ROLE-003 matrix).

---

## Related

- BR-ROLE-001..006: `documents/01-business/roles/rules.md`
- UC-ROLE-{OWNER-SCOPED-DASHBOARD, STAFF-SCOPED-DASHBOARD, STAFF-INVITE, MIGRATION}: `documents/01-business/roles/use-cases.md`
- Wave 79 plan: `documents/03-planning/waves/wave-2026-05-14-79-beta-invite-close-out.md`
- Cross-layer rule: `.claude/rules/contract-first-for-cross-layer.md`
- OWASP A01 enforcement: `.claude/rules/pre-launch-owasp-rest-hardening-checklist.md` §2.1
- Auth hardening (admin 2FA mandatory): `.claude/rules/pre-launch-auth-hardening-checklist.md` §2.4
- 2FA contract: `documents/01-business/kitehub/auth-2fa/api-contract.md` BR-AUTH-2FA-005 (cannot disable cho admin)
- MSW handler (staff invitations): `kitehub/kitehub-frontend/src/test/msw/handlers/staff-invitations.ts`
- Gap: GAP-562 (RBAC OWNER/STAFF role separation P0)
- Wave 78 GAP-518 (admin role mismatch BE+FE — closed; this contract codifies the long-term solution)
