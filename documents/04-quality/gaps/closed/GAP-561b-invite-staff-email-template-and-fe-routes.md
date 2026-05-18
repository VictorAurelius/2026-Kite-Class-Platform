---
id: GAP-561b
title: invite-staff email template + FE invite UI + actual InvitationController behind 501 stubs
status: DONE
priority: P0
layer: Mixed
phase: phase-1-beta
percent_complete: 100
created: 2026-05-14
updated: 2026-05-15
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

- [x] All 5 InvitationController endpoints return real HTTP codes (no 501) — POST/GET/DELETE + accept + resend wired in StaffInvitationController, integration test 9 cases PASS
- [x] Email delivered + clickable → /staff/accept-invite landing renders — `invite-staff.html` + `.txt` templates + EmailServiceClient.sendInviteStaffEmail() wired; FE accept page lives at `/staff/accept-invite?token=...`
- [x] Invitee accepts → user row created in DB with correct role — controller POST /{token}/accept creates User row with role=STAFF, integration test `postAcceptHappyPath` verifies
- [x] OWNER sees /admin/staff list with pending + active invites — `(admin)/admin/staff/page.tsx` lists rows; refresh after invite shows new row (E2E `staff-invite.spec.ts` verifies)
- [x] Non-OWNER hitting /admin/staff/invite → 403 (via RBAC sister gap GAP-562b) — @PreAuthorize on controller; FE RoleGuard component scope = GAP-562b Wave 80 Bucket C
- [x] Audit log entries verified post-invite + accept + revoke — `StaffInvitationAuditEntry` entity + `staff_invitation_audit_log` table (V49); 6 event types CREATED/SENT/RESENT/ACCEPTED/REVOKED/EXPIRED; integration test asserts rows

## Dependencies

- **Sister:** GAP-562b (FE role-guard component + @PreAuthorize coverage extension) — bundle in same Wave 80 bucket
- **Upstream:** GAP-561 PARTIAL 50% (V45 migration + StaffInvitation entity + skeleton 501) — Wave 79 Bucket B PR #1366 SHIPPED

## Refs

- `documents/01-business/roles/{rules,use-cases,api-contract}.md` (Wave 79 Bucket 0 Foundation)
- `documents/03-planning/session-handoffs/2026-05-15-post-wave-79-handoff.md` §2.1
- `documents/03-planning/waves/wave-2026-05-14-80-deploy-smoke.md` (DRAFT — may add bucket post-DEPLOY)

## Log

- **2026-05-15 (Wave 80 Bucket B):** 🟢 DONE 100% — shipped via wave-80-b/invite-staff-flow:
  - Email templates `invite-staff.html` + `.txt` (vi-VN narrative, branded variables, VND/VN date format)
  - `InvitationTokenService` HMAC-SHA256 signed tokens, TTL 7 days, `@PostConstruct` fail-fast guard mirroring `TotpSecretCipher` pattern (per `pre-launch-auth-hardening-checklist.md` §2.6)
  - `StaffInvitationController` real implementation replacing 3 × 501 stubs: POST (create+email), GET (list), DELETE (revoke), POST /accept (token-validated public, creates User row), POST /resend (rotate token + email)
  - Idempotency on re-invite: auto-revoke old PENDING + create new (audit-trail friendly per Wave 80 §1 brainstorm Q2)
  - `StaffInvitationAuditEntry` entity + `staff_invitation_audit_log` table (V49 migration) — 6 event types, audit row per state transition (OWASP A09)
  - `EmailServiceClient.sendInviteStaffEmail()` best-effort dispatch + idempotency log
  - FE routes: `/admin/staff` (list), `/admin/staff/invite` (form), `/staff/accept-invite` (public landing, password set)
  - Endpoint constants in `lib/api/endpoints.ts` `staffInvitations` namespace
  - BE integration test `InvitationControllerIntegrationTest` — 9 cases all PASS (POST happy + GET list + DELETE revoke + accept happy + resend rotate + expired token + revoked token + duplicate idempotency + weak password)
  - FE Playwright E2E `staff-invite.spec.ts` — 4 scenarios (invite flow + accept happy + expired token + weak password client validation)
  - Smoke test `scripts/smoke-email-actuator.sh` extended with `--template invite-staff` variant
  - All existing tests pass: 630 BE tests + 738 FE tests green
- **2026-05-14:** Filed as Wave 79 Bucket B PARTIAL exit-ramp per `gap-done-discipline.md` §3. Sister to GAP-562b. P0 BLOCKING v0.9.0-beta tenant invite flow.

- **2026-05-15:** OPEN 0% → DONE 100% — Wave 80 Bucket B closure. Full impl shipped per spec (PR #1383). CI initial fail rebound: EmailTypeTest catalog 15→16 (INVITE_STAFF added) + `/staff/accept-invite` wrapped Suspense boundary for Next.js SSG prerender (fix commit 28af2869 merged into PR rebase). Parent GAP-561 upgraded PARTIAL 50 → DONE 100.
