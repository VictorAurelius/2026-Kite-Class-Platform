---
audience: dev
---

# GAP-788 — META Wave 80+ retro-walk batch (apply feature-ship-runtime-walk-mandate retroactively)

**Status:** 🔵 OPEN
**Priority:** 🔴 P0
**Domain:** Meta
**Found:** 2026-05-28 (User strategic decision post Wave meta-6 Bucket A walk shutdown — see `documents/04-quality/audits/retro/2026-05-28-wave-80-plus-done-features-walk-evidence-audit.md`)
**Phase:** phase-1-beta

## Problem

Wave meta-6 Bucket A RST walk 2026-05-28 surfaced **17 bugs** in shipped-DONE feature. 2 P0 paths COMPLETELY MISSING (Bug #14 email, Bug #17 user provision). Audit suite 76-94/100 + 25 Mockito tests PASS — all bugs invisible until human walk.

Audit retro Opus agent (`AUDIT-2026-05-28-wave-80-plus-done-features-walk-evidence-audit`) enumerated 46 Wave 80+ DONE features + sampled 10 for walk-evidence check:
- **5 NONE** (50%) — no runtime walk evidence
- 3 PARTIAL (30%)
- 2 HAS_RUNTIME_WALK (20%)

**Projection:** 50% × 46 = ~23 features likely have similar walk-evidence gaps as Wave meta-6 Bucket A. Conservative bug estimate: 5-15 bugs per feature × 23 = **115-345 latent bugs** in shipped Phase 1 BETA scope.

Trust-pass anti-pattern recurrence **≥7 lần** quantified. `feature-ship-runtime-walk-mandate.md` v1.0.0 shipped same session (commit `b6539bab`) closes class at original-ship moment **prospectively**. This META gap tracks retroactive application.

## Scope

Apply `feature-ship-runtime-walk-mandate.md` v1.0.0 retroactively to all 46 Wave 80+ DONE features. Re-classify DONE → PARTIAL where walk evidence missing AND bugs surface. Per `gap-done-discipline.md` §3 grandfather convention — DO NOT mass-flip; flip per-feature only when retro-walk confirms bugs.

### User strategic decision 2026-05-28 (locked)

1. **STOP per-feature walks** (don't accumulate more isolated walk sessions)
2. **Audit suite RETRO** — apply rule retroactively, file 1 META gap (this one) tracking batch
3. **Time-box ~10 days** — Wave A (@PreAuthorize sweep) + Wave B (email/event binding) only — defer Wave C/D/E to Phase 3
4. **Accept 50% NONE projection** as baseline — no expansion sample to 100%

## Phase 2 BETA Wave A — @PreAuthorize ghost-guards sweep (~3 days)

**Hypothesis (per audit retro §6.3 top 3 patterns):** ALL `@PreAuthorize` annotations in kiteclass-core are no-op because `SecurityConfig.anyRequest().permitAll()` means no Spring Authentication object exists.

### Acceptance criteria

- [ ] Grep all `@PreAuthorize` annotations across `kiteclass/kiteclass-core/src/main/java/**/controller/*Controller.java`
- [ ] Estimate: ~10-20 controllers affected
- [ ] Refactor pattern (per Wave meta-6 walk-fix Bug #8 — see `StaffInvitationController` for canonical example):
  - Remove `@PreAuthorize` annotation
  - Add `@RequestHeader(value = "X-User-Roles", required = false) String roles` param
  - Call `requireXyzRole(roles)` helper (mirror `VettingController.requireSafeguardingOfficer` pattern)
- [ ] Sweep PR shipped reviewing each controller's allowed roles per `pre-launch-auth-hardening-checklist.md` §2
- [ ] No regression: existing IT tests with `X-User-Roles` header pass
- [ ] Per-controller walk evidence per `feature-ship-runtime-walk-mandate.md` §3 (sample 3-5 walks, not all controllers)

## Phase 2 BETA Wave B — Email/event/outbox binding missing (~7 days)

**Hypothesis (per audit retro §6.3):** 8-12 features emit event/email but downstream consumer/template/binding broken silently.

### Sub-features to audit

Likely candidates (grep `outboxEvent` + `EmailService` + `@RabbitListener` cross-reference per audit retro):

1. **Staff invitation email** (Bug #14 / GAP-787) — paired with this batch
2. **Parent invitation email** — Wave 80 era (similar pattern likely missing)
3. **Welcome email after signup** (Owner first login)
4. **Beta access approval email** (admin approval flow)
5. **Password reset email** (auth flow)
6. **Payment receipt email** (billing flow)
7. **Invoice issued email** (billing flow)
8. **Class reschedule notification** (Owner → enrolled students)
9. **Assignment due reminder** (scheduled cron — outbox cron pattern)
10. **Vetting status change notification** (childprotection)

### Acceptance criteria per affected feature

- [ ] Service method emits outbox event (`OutboxEventWriter.enqueue(...)` per `design-patterns.md` §3.5)
- [ ] kitehub-email consumer listens for routing key
- [ ] Email template Vietnamese narrative + persona-appropriate (per `vn-localization-audit-checklist.md` v1.0.0)
- [ ] Accept/action URL correct
- [ ] MailHog dev verify
- [ ] RabbitMQ queue auto-declared (no manual `rabbitmqadmin` per Wave 6 Bug #6 recurrence)
- [ ] Walk evidence per feature per `feature-ship-runtime-walk-mandate.md` §3

## Phase 2 BETA scope DEFERRED to Phase 3 (per time-box decision)

- **Wave C: FE ApiResponse `.map()` unwrap sweep** — 5-10 FE pages defensive unwrap OR global axios interceptor architecture-decision (estimate 1-2 days)
- **Wave D: 5 retro-walk waves** — signup chain / compliance+auth / email cluster / payment+audit / vendor+FE re-verify (estimate 8-10 days)
- **Wave E: Audit suite extensions** — api-contract Cat 2 ext (FE call sites cross-ref) + ops-readiness probe each mutation produces side effects + business-logic AC walk (estimate 3-4 days)

Re-evaluate Phase 3 inclusion sau Wave A+B ship + retroactive walk evidence collected từ those PRs.

## Acceptance Criteria (META gap)

- [ ] Wave A shipped (single sweep PR replacing all `@PreAuthorize` với header-RBAC)
- [ ] Wave B shipped (~10 features email/event binding fixed)
- [ ] Per-feature re-classification: DONE → PARTIAL nếu retro-walk confirms bugs, OR stay DONE if walk passes
- [ ] `audits-index.csv` annotations cho features re-classified
- [ ] ROADMAP updated với Wave A+B shipping summary
- [ ] Audit retro doc (`retro/2026-05-28-wave-80-plus-done-features-walk-evidence-audit.md`) updated với actual results vs projection (validates 50% NONE estimate)
- [ ] Decision logged: continue Phase 3 Wave C/D/E OR sufficient ground covered

## Related

- Walk shutdown findings: `documents/04-quality/audits/rst-html/2026-05-28-wave-meta-6-bucket-a-walk-shutdown-findings.md` (17-bug catalog Wave meta-6 Bucket A origin)
- Audit retro doc: `documents/04-quality/audits/retro/2026-05-28-wave-80-plus-done-features-walk-evidence-audit.md` (46-feat enumerated, 10 sampled, projection)
- META rule shipped same session: `.claude/rules/feature-ship-runtime-walk-mandate.md` v1.0.0
- Sister gaps Wave meta-6 Bucket A walk:
  - GAP-786 (Bug #17 user provision on accept — paired)
  - GAP-787 (Bug #14 email never sent — paired Wave B candidate #1)
  - GAP-783 (Wave 71b/meta-6 JWT authority class — PR #1917 OPEN)
  - GAP-784 (FE invite role drift — PR #1917 OPEN)
  - GAP-785 (RabbitMQ queue auto-declare — PR #1917 OPEN, recurrence concern Wave B Sub-feature 1)

## Log

- **2026-05-28** — Filed per user strategic decision 2026-05-28 (4-question batch answered Recommended for all 4):
  - GAP-779 phantom reference: file new GAP-786 (Bug #17) — done
  - GAP-772 Wave meta-6 Bucket A re-class: file META gap (this one) instead of mass-flip
  - Phase 2 BETA scope ~10 days time-box: Wave A + Wave B only
  - Audit retro sample: accept 50% projection, no expansion

  Wave A+B scoped + acceptance criteria defined. Wave C/D/E deferred Phase 3. META P0 priority due trust-pass anti-pattern recurrence ≥7 — eliminates root class at original-ship moment + retroactively applies to existing shipped scope.

  Next session: wave plan trigger for Wave A (@PreAuthorize sweep) start.
