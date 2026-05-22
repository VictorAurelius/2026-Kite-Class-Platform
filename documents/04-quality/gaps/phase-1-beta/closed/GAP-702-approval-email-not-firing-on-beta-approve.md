---
gap_id: GAP-702
title: Approval email NOT firing on POST /admin/beta-requests/{id}/approve
status: OPEN
priority: P0
domain: Backend
phase: phase-1-beta
completion_pct: 0
filed_date: 2026-05-22
last_updated: 2026-05-22
filed_by: Wave 103 Bucket D live verify
---

# GAP-702 — Approval email NOT firing on beta-request approve

## Problem

`POST /api/v1/admin/beta-requests/{id}/approve` returns HTTP 200 + status flips PENDING → APPROVED — BUT downstream email service does NOT receive any send request.

**Evidence (Wave 103 Bucket D live verify 2026-05-22 03:48-03:50 UTC):**
- Admin approve curl: HTTP 200 response with full BetaRequestResponse JSON, `status=APPROVED, approvedAt=...`
- `docker logs kitehub-email --since 5m` after approve: **0 lines matching "Sending"** — email service quiet
- Mailhog total messages count unchanged after approve (only password-reset emails arrived)
- Sister log: `admin_audit_log` INSERT errored with Hibernate type conversion mismatch (`Could not convert 'java.lang.String' to '[B'`) — SEPARATE bug, but might indicate broken transaction path that also skips email-send

**Impact (P0 — blocks beta tenant signup):**
- Beta tenant cannot complete signup flow (no invite token email arrives)
- Manual workaround: admin gives invite token via Zalo/SMS (defeats automation purpose)
- Affects 100% of beta cohort onboarding velocity

## Context

- Wave 33 GAP-372 shipped `BetaAccessController.approve()` — should trigger invite-token email per documented flow
- Wave 102.9 Bucket B state-check claimed code-AC for GAP-531 shipped Wave 78; reality 2026-05-22 = email-send not wired OR conditionally skipped in test env
- Bucket D used `EMAIL_PROVIDER=smtp` env (Mailhog target) — confirmed working for password-reset path; failure isolated to approve-flow not generic email service breakage

## Proposed Fix

1. **Diagnose** `BetaAccessService.approveRequest()` for missing `notificationService.sendInviteEmail()` call OR feature-flag conditional that disables in non-prod
2. **Wire approval email send** post-status-flip + post-token-generation
3. **Add integration test** `BetaAccessControllerIT.shouldSendInviteEmailOnApprove()` to lock behavior
4. **Verify** via Wave 103 Bucket D pattern: trigger curl approve → assert Mailhog receives 1 message with invite token + VN content per `vn-localization-audit-checklist.md`

## Acceptance Criteria

- [ ] `BetaAccessService.approveRequest()` calls notification path post-token-generation
- [ ] IT `BetaAccessControllerIT.shouldSendInviteEmailOnApprove()` PASS
- [ ] Live verify: approve curl → Mailhog 1 new message within 5s + subject Vietnamese + body has invite token URL
- [ ] Sister bug filed separately: `admin_audit_log` Hibernate type conversion (Could not convert String to [B)
- [ ] GAP-531 status revised based on this fix landing (currently 70% — flip to higher when fix lands)

## Related

- [[GAP-531]] Tenant init handoff (closely coupled — approval email is part of init flow)
- [[GAP-543]] 5 email types VN content (approval is 1 of the 5)
- [[GAP-657]] Email layer hardening (any send goes through hardened pipeline)
- Wave 103 audit: `documents/04-quality/audits/local-stack/2026-05-22-wave-103-email-mailhog-verify.md`
- Wave 103 sister audit: `documents/04-quality/audits/local-stack/2026-05-22-wave-103-owner-persona-walk.md`
