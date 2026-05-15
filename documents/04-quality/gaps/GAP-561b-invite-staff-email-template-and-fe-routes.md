---
id: GAP-561b
title: invite-staff email template + FE invite UI + actual InvitationController behind 501 stubs
status: OPEN
priority: P0
layer: Mixed
phase: phase-1-beta
percent_complete: 0
created: 2026-05-14
updated: 2026-05-14
parent: GAP-561
wave_target: 80
---

# GAP-561b — invite-staff email + FE UI + behind-501 implementation

**Type:** Sister/follow-up gap (split from GAP-561 PARTIAL 50% Wave 79 Bucket B)
**Priority:** 🔴 P0 — Manager flow blocker for v0.9.0-beta tenant invite
**Wave target:** 80 DEPLOY+SMOKE (bundle with GAP-562b — sister RBAC gap)
**Estimate:** ~2-3h single bucket (FE+BE+email coupled)

---

## Problem

Wave 79 Bucket B (PR #1366) shipped V45 migration + StaffInvitation entity + InvitationController skeleton stubs returning **HTTP 501 NOT_IMPLEMENTED** for 3 of 5 staff endpoints. Email template + FE invite-staff UI route + actual business logic deferred to follow-up per gap-done-discipline.md §3 PARTIAL exit ramp.

Result: P3 Center Manager invite flow not functional — Owner cannot send staff invitations from production beta.

---

## Scope (this follow-up)

### Email template (kitehub-email)

- [ ] HTML + plain-text `invite-staff.html` + `invite-staff.txt` templates
- [ ] Vietnamese narrative (per `dev-readable-doc-language.md`)
- [ ] Variables: `{{ownerName}}`, `{{tenantName}}`, `{{inviteUrl}}`, `{{expiresAt}}`, `{{role}}` (OWNER/STAFF)
- [ ] Smoke test post-deploy: `scripts/smoke-email-actuator.sh` extends with invite-staff template variant

### FE route (kitehub-frontend)

- [ ] `/admin/staff/invite` route with form (email + role + permissions checkbox grid)
- [ ] `/admin/staff` list route showing pending/active invitations
- [ ] Token-link landing page `/staff/accept-invite?token=...` for invitee acceptance
- [ ] RBAC guard — only OWNER role can access /admin/staff/invite (sister to GAP-562b)

### BE implementation (kitehub-subscription InvitationController)

- [ ] Replace 501 stubs with actual logic:
  - POST /api/v1/staff/invitations — create + send email
  - GET /api/v1/staff/invitations — list pending/active for tenant
  - DELETE /api/v1/staff/invitations/{id} — revoke
  - POST /api/v1/staff/invitations/{id}/accept (public, token-validated) — convert to user
  - POST /api/v1/staff/invitations/{id}/resend — re-send email
- [ ] Token: HMAC-signed JWT TTL 7 days per `pre-launch-auth-hardening-checklist.md`
- [ ] Idempotency: re-invite same email → revoke old + create new
- [ ] Audit log row per invite created/accepted/revoked

### Tests

- [ ] InvitationControllerIntegrationTest (testcontainers) — 5 endpoints + 4 edge cases (expired token + revoked + duplicate email + non-owner caller)
- [ ] FE Playwright E2E: invite → email link → accept flow
- [ ] Email template smoke: render with sample data, verify vi-VN locale

## Acceptance Criteria

- [ ] All 5 InvitationController endpoints return real HTTP codes (no 501)
- [ ] Email delivered + clickable → /staff/accept-invite landing renders
- [ ] Invitee accepts → user row created in DB with correct role
- [ ] OWNER sees /admin/staff list with pending + active invites
- [ ] Non-OWNER hitting /admin/staff/invite → 403 (via RBAC sister gap GAP-562b)
- [ ] Audit log entries verified post-invite + accept + revoke

## Dependencies

- **Sister:** GAP-562b (FE role-guard component + @PreAuthorize coverage extension) — bundle in same Wave 80 bucket
- **Upstream:** GAP-561 PARTIAL 50% (V45 migration + StaffInvitation entity + skeleton 501) — Wave 79 Bucket B PR #1366 SHIPPED

## Refs

- `documents/01-business/roles/{rules,use-cases,api-contract}.md` (Wave 79 Bucket 0 Foundation)
- `documents/03-planning/session-handoffs/2026-05-15-post-wave-79-handoff.md` §2.1
- `documents/03-planning/waves/wave-2026-05-14-80-deploy-smoke.md` (DRAFT — may add bucket post-DEPLOY)

## Log

- **2026-05-14:** Filed as Wave 79 Bucket B PARTIAL exit-ramp per `gap-done-discipline.md` §3. Sister to GAP-562b. P0 BLOCKING v0.9.0-beta tenant invite flow.
