---
title: GAP-708 — Wave 104 follow-up cluster (live verify + test harness fix + dead-field cleanup)
status: OPEN
priority: P2
phase: phase-1-beta
created: 2026-05-22
found_by: Wave 104 closure inline (coordinator)
related_gaps: [GAP-702, GAP-703, GAP-704, GAP-705, GAP-706, GAP-707, GAP-516, GAP-531, GAP-543, GAP-657, GAP-659]
related_waves: [104, 104.5]
audience: dev
---

# GAP-708 — Wave 104 follow-up cluster

## Problem

Wave 104 shipped 4 production fix buckets (A/B/C/D — GAP-702..707) trên `wave/104-fix-followup-bugs` với 100% unit test coverage. **3 items defer khỏi Wave 104 closure scope** (per `wave-closure-scope-completeness.md` §3 PARTIAL exit ramp):

### Item 1 — Bucket E live verify (post-rebuild)
Plan §3 Bucket E required re-trigger Wave 103 patterns against post-fix Docker images:
- Owner walk: signup → approve → Owner login → `/api/v1/onboarding-progress` Bearer (NO X-Tenant-Id) → expect 200 (was 400)
- Email verify: trigger 5 types → Mailhog inspect multipart + List-Unsubscribe (was 0/5)
- 2FA via gateway: admin → enroll-init port 9000 Bearer challenge (NO spoofed) → expect 200 (was 401)
- GAP-707 verify: 5 logins → log scan absence of "unique result" WARN
- Audit doc: `documents/04-quality/audits/local-stack/2026-05-22-wave-104-post-fix-verify.md`

**Deferred reason:** Wave 104 closure session had WSL RAM constraint (Docker stack ~6GB / 7.6GB total); rebuild 3 services (kitehub-subscription + kitehub-email + kite-gateway) + live verify ~1h coordinator + RAM bursts not safe.

### Item 2 — `EmailHardeningTest` re-enable
Bucket B agent shipped `EmailHardeningTest` (GAP-703 regression coverage cho multipart/alternative + List-Unsubscribe headers) nhưng test harness's standalone Thymeleaf resolver chain (`htmlResolver .html` + `textResolver ""` empty suffix) does not match production `EmailTemplateResolverConfig` dual-mode wiring. Rendered multipart drops HTML part → assertion fails "multipart/alternative should contain text/html part".

**Current state:** 2 test methods @Disabled với reason citing this gap. Production code (B1+B2 commits `c7d916e6` + `b8a784fc`) verified correct via direct source review — only test plumbing broken.

**Fix options:**
- (a) Refactor test to `@SpringBootTest` slice using production `EmailTemplateResolverConfig` bean
- (b) Fix standalone resolver suffix conflict (textResolver should target `.txt` specifically not `""`)

### Item 3 — `SESEmailService.templateEngine` dead field cleanup
Wave 104 inline removed `renderTemplate()` private method (orphan dead code), cascading `org.thymeleaf.context.Context` import + `private final TemplateEngine templateEngine` field to unused state. Import removed; field retained với `@SuppressWarnings("unused")` to avoid breaking 3 test constructor calls (NotificationChannelContractTest + EmailHardeningTest + SESEmailServiceTest) — scope creep mid-closure.

**Fix:** Remove `templateEngine` field + constructor param + update 3 test fixtures.

## Acceptance Criteria

- [ ] Item 1 — Wave 104 post-rebuild live verify (Bucket E):
  - [ ] Rebuild 3 services (kitehub-subscription, kitehub-email, kite-gateway) post Wave 104 merge
  - [ ] Re-trigger Wave 103 4 patterns; pre/post comparison audit doc shipped
  - [ ] No NEW bugs surfaced (else file Wave 105 gap)
  - [ ] Wave 103 PARTIAL gaps revised: GAP-531 70%→100%, GAP-516 90%→100%, GAP-543 65%→95%, GAP-657 40%→100%, GAP-659 50%→80%
- [ ] Item 2 — `EmailHardeningTest` re-enabled:
  - [ ] @Disabled removed; both methods PASS
  - [ ] Either `@SpringBootTest` slice OR standalone resolver suffix fix
- [ ] Item 3 — `SESEmailService.templateEngine` removed:
  - [ ] Field + constructor param removed
  - [ ] 3 test fixtures updated (NotificationChannelContractTest, EmailHardeningTest, SESEmailServiceTest)
  - [ ] `mvn verify -P strict-warnings` PASS no regression

## Recommended Wave

**Wave 104.5** (LOCAL-only follow-up post Wave 104 merge) — bundle all 3 items in 1 cleanup wave. Estimated: 1.5-2h coordinator (Items 1+2+3 sequential).

## Related

- Wave 104 plan: `documents/03-planning/waves/wave-2026-05-22-104-fix-followup-bugs.md`
- Wave 104 closure PR: TBD (Wave 104 squash-merge)
- Sister rules: `wave-closure-scope-completeness.md`, `pre-handoff-self-test-completeness.md` §2.4

## Log

- **2026-05-22:** Gap filed during Wave 104 inline closure (Bucket D coordinator). 3 deferred items bundled per `wave-closure-scope-completeness.md` §3 PARTIAL exit ramp.
