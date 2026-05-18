# GAP-530: Email-driven flow end-to-end live verify per `pre-handoff-self-test-completeness.md` §2.3

**Status:** 🟡 PARTIAL 10% — Wave 77 Bucket A shipped verification automation scripts; 5-email-type live verify per §2.3 + audit artifact remain user-action post GAP-370/533 dashboard verify + apply + warm-up
**Priority:** 🔴 P0 — BLOCKING Plan 1 BETA invite
**Domain:** Backend + DevOps
**Found:** 2026-05-14 (Wave 76 — Phase 1 BETA persona audit)
**Affects:** All 5 beta-tenant personas (P1 Trial / P2 Center Owner / P3 Manager / P5 K-12 / Power User)
**Phase:** phase-1-beta

## Problem

Phase 1 BETA persona audit (`documents/04-quality/audits/meta/2026-05-14-phase-1-beta-blockers-re-audit-persona.md`) discovered that **email-driven flows have no end-to-end live verification** per `pre-handoff-self-test-completeness.md` §2.3.

Existing email infrastructure gaps (GAP-370 SES + GAP-508 Phase 2 RESEND_API_KEY + production-env-config-registry rule) address **CONFIGURATION** but NOT empirical end-to-end verification: real email actually sent → real recipient inbox receives → real link clickable → real subsequent state advances.

**Symptom:** 5/5 beta personas hit email touchpoints (invite + verification + approval notification + password reset + welcome). Without §2.3 verification, ANY email infrastructure misconfiguration silently blocks all personas.

## Root Cause

`pre-handoff-self-test-completeness.md` §2.3 (Email-driven flow gap) checklist:
- [ ] (a) Email actually sent (not queued+dropped) — provider dashboard shows "delivered" OR check inbox
- [ ] (b) Link in email points to live URL — curl that URL → 200, NOT 404/dev-domain
- [ ] (c) Clicking link advances state — token validates, downstream action completes

Plan 1 deploy plan has §2.4 manual checklist "Beta invite email received + signup token validates" but:
- Marked manual checklist, NOT automated
- "Beta invite" only; doesn't cover verification / approval-notification / welcome / password-reset emails
- No artifact format documented — verification ad-hoc, no audit trail

## Proposed Fix

### Phase 1 — Pre-invite blocker (must close before any Plan 1 invite)

1. Wire `EMAIL_PROVIDER=resend` in production (per GAP-508)
2. Real test email send from production env (admin@kitehub.me to known test inbox)
3. Run §2.3 checklist explicitly for 5 email types:
   - Request Beta Access acknowledgment (Wave 33 Bucket C)
   - Admin approval notification (post-admin-approve)
   - Invite email với signup token (post-approve)
   - Email verification link (post-signup)
   - Welcome email (post-tenant-provision)
4. Ship audit artifact `documents/04-quality/audits/email/2026-05-14-phase-1-beta-email-flow-e2e.md` documenting:
   - 5 email types tested
   - Provider dashboard delivery confirmed (or screenshot)
   - Link URL validated against production (curl 200)
   - State advance verified (login persisted / approval flipped / etc.)
5. Update `release-1-deploy-plan.md` §2.4 with structured §2.3 checklist (replace ad-hoc 1-line item)

### Phase 2 — Automation (Wave 77+)

- Automated smoke test extending `scripts/smoke-test.sh` với email send + delivery polling
- CI gate cho email e2e (deferred ≥7 days per `incident-to-rule-pipeline.md` premature-rule guard)

## Acceptance Criteria

- [ ] EMAIL_PROVIDER=resend wired production
- [ ] 5 email types tested live with §2.3 checklist verified
- [ ] Audit artifact filed under `documents/04-quality/audits/email/`
- [ ] release-1-deploy-plan.md §2.4 updated với structured checklist
- [ ] Provider dashboard screenshot OR equivalent delivery confirmation
- [ ] Subsequent state advance verified per email (token validate / login / approval)

## Related

- Wave 76 closure: `2026-05-14-wave-76-closure-meta-hygiene.md`
- Phase 1 audit: `2026-05-14-phase-1-beta-blockers-re-audit-persona.md` (verdict: 5/5 personas affected by email infra)
- Companion gaps: GAP-370 (SES setup), GAP-508 (RESEND_API_KEY Phase 2), GAP-372 (beta invite mechanism)
- Rule: `pre-handoff-self-test-completeness.md` §2.3 Email-driven flow class
- Deploy plan: `documents/03-planning/roadmap/release-1-deploy-plan.md` §2.4 smoke checklist

## Log

- **2026-05-14** (Wave 77 Bucket A code-side context): Verification automation `scripts/verify-email-deliverability.sh` + `scripts/smoke-resend.sh` ship in same wave Bucket A. Once GAP-533 user-action follow-on (Resend dashboard verified + DNS applied + warm-up Day 5+ spam-score green) completes, operator runs §2.3 Phase 1 5-email-type live verify per `pre-handoff-self-test-completeness.md` §2.3 to flip this gap. Status stays 🔵 OPEN — code shipped to enable verification, but the verification itself remains user-action and the audit artifact `documents/04-quality/audits/email/2026-05-14-phase-1-beta-email-flow-e2e.md` has not yet been authored.
- **2026-05-14:** Gap filed Wave 76 Bucket F closure from Phase 1 BETA persona audit. Surfaced as NEW-001 (P0) — not previously in P0 list. Email infrastructure is cross-cutting risk affecting 5/5 personas; must close before Plan 1 invite per audit verdict "tightly-controlled handful (2-3 trusted users)".
