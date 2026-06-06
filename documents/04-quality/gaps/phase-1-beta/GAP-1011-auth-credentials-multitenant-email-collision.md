# GAP-1011: auth_credentials global email-unique collides with multi-tenant (auth-1)

**Status:** 🔵 OPEN
**Priority:** 🟠 P1
**Domain:** Backend
**Found:** 2026-06-06 (Wave auth-1 post-wave audit suite — business-logic P1 + ops-readiness P2)
**Affects:** `kiteclass-core` `V89__create_auth_credentials.sql:20` + `AuthCredentialProvisioningService.java:50-55`

## Problem

`V89` declares `CONSTRAINT uk_auth_credentials_email UNIQUE (email)` — GLOBALLY unique across all tenants. This contradicts BR-PARENT-001 (`parent-portal/rules.md:27`) which allows the same email to be a valid parent in multiple tenants (one person, multiple schools). Consequence: `AuthCredentialProvisioningService` provision is idempotent-on-email → a parent already provisioned in tenant A who is invited to tenant B gets the tenant-A credential returned → login resolves to the WRONG tenant (instance_id from the first row). RLS-disabled (intentional pre-auth lookup) means global email scan is by design, but the uniqueness scope is the bug.

## Proposed Fix

Decide one:
- **(A) Document "1 email = 1 tenant" Phase 1 BETA limitation** — accept constraint, note in BR-PARENT-001 + tenant-auth rules.md, defer multi-tenant-email to Phase 1.5. (cheapest)
- **(B) Change to `UNIQUE (instance_id, email)`** + lookup by `email + instance_id` — but login is pre-auth (no tenant context); requires tenant disambiguation at login (subdomain/explicit tenant pick). Larger scope.

Pick (A) for Phase 1 BETA unless multi-school-parent is a launch requirement.

## Acceptance Criteria

- [ ] Decision recorded (A or B) in tenant-auth rules.md + BR-PARENT-001 reconciled
- [ ] If (A): provisioning rejects/flags cross-tenant email reuse with clear error (not silent wrong-tenant)
- [ ] If (B): unique(instance_id,email) migration + login tenant-disambiguation + IT proving cross-tenant isolation

## Related

- Audit reports: `documents/04-quality/audits/business-logic/2026-06-06-wave-auth-1-business-logic.md` + `../ops-readiness/2026-06-06-wave-auth-1-ops-readiness.md`
- BR-PARENT-001; V89 RLS-disable rationale (pre-auth lookup)
