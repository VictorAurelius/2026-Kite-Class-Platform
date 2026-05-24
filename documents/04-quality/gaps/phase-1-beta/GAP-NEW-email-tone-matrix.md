---
gap-id: GAP-NEW-email-tone-matrix
title: META cross-cut email tone matrix Mustache/Thymeleaf helper + VN sample fixture audit
priority: P1 META
status: 🟢 DONE
phase: phase-1-beta
created: 2026-05-24
closed: 2026-05-25
owner: Wave beta-readiness-4 Bucket E
wave: wave-beta-readiness-4
---

# GAP-NEW-email-tone-matrix — Email tone matrix shared helper + VN sample audit

## Problem

Wave beta-readiness-4 Bucket D shipped `class-rescheduled.html` email template với inline hardcoded greeting `"Kính gửi quý phụ huynh,"` — đúng cho parent persona NHƯNG vi phạm `vn-localization-audit-checklist.md` §2 Section 2 persona tone matrix (6 persona × 6 greeting). Mọi email template subsequent (Bucket A welcome / Bucket B PDPL consent / future operational notifications) sẽ inline greeting riêng → drift theo thời gian → 6+ source-of-truth → reviewer burden + inconsistency.

Bucket D inline comment explicit "Bucket E sẽ refactor consume `_shared/persona-tone` partial cho persona-specific greeting matrix" — this GAP closes that scope.

Additional scope: Wave 100 Bucket D shipped VN-localization audit checklist `.claude/rules/vn-localization-audit-checklist.md` but không có detector cho test fixtures + email previews. Cần audit script grep English placeholder (John Doe / Class A1 / Example Center / Lorem Ipsum / USD currency) trong test code + docs để prevent VN-sample violation slip qua reviewer.

## Acceptance Criteria

- [x] Thymeleaf shared fragment `_shared/persona-tone.html` ship với 5+ persona greeting variants (P1/P2/P3/PARENT/STUDENT + PLATFORM_ADMIN default)
- [x] `PersonaToneResolver.java` Java service inject `personaGreeting` variable based on recipient persona
- [x] Unit test `PersonaToneResolverTest` 5+ persona greeting matrix mappings PASS
- [x] Bucket D `class-rescheduled.html` template refactored consume `_shared/persona-tone :: greeting` fragment (no inline greeting)
- [x] Bucket D `ClassRescheduledEmailTemplateTest` STILL PASSES post-refactor (regression check)
- [x] `ClassRescheduledEmailService.buildTemplateContext()` updated inject `personaGreeting` via `PersonaToneResolver.resolveGreeting(Persona.PARENT)`
- [x] `scripts/audit-vn-sample-fixtures.sh` detect English placeholder patterns + USD currency
- [x] `scripts/tests/test-audit-vn-sample-fixtures.sh` 3/3 fixture scenarios PASS (clean / known-bad / acceptable-english)
- [x] CI job `vn-sample-fixtures` WARN-mode wired trong `quality-rules-skills.yml`
- [x] `./mvnw -pl kitehub-email verify -P strict-warnings` PASS (Bucket D test still 4/4 + new PersonaToneResolverTest 12/12)

## Implementation Notes

**Tech stack pivot:** Wave plan §3.5 specified Mustache partial; actual template stack is Thymeleaf (`xmlns:th` + `th:text` + `th:fragment`). Shipped Thymeleaf fragment instead — semantic equivalent (single source of truth + per-persona greeting injection), correct tech stack.

**Persona enum scope:** 6 personas covering Phase 1 BETA tenant-facing scope:
- P1_SOLO_TEACHER (casual "Chào em,")
- P2_CENTER_OWNER (formal "Em chào chị,")
- P3_CENTER_MANAGER (formal-neutral "Em chào chị/anh,")
- PARENT (very formal "Kính gửi quý phụ huynh,")
- STUDENT (friendly "Chào em,")
- PLATFORM_ADMIN (default formal "Em chào anh/chị,")

**Safe-default:** Unknown / null persona → returns `PLATFORM_ADMIN` formal-neutral greeting. Avoid casual greeting leak to unknown recipient (vd: spam pipeline, mis-routed email, JWT claim mismatch).

**VN audit WARN-mode:** Per `incident-to-rule-pipeline.md` §3.1 tightened legitimate-deferral conditions — heuristic regex FP risk on legitimate code-switching (JWT/HTTP/AWS English tokens natural per `dev-readable-doc-language.md` §4); HARD STOP target 2026-06-23 (30-day grace). Override via commit trailer `VN_LOCALIZATION_OVERRIDE:` per checklist §4.5.

**Self-test 3/3 PASS:** clean fixture (0 findings), known-bad (13 findings — exceeds ≥4 threshold), acceptable-english (0 findings — JWT/HTTP/OAuth/brand names exempt).

## Files Changed

- `kitehub/kitehub-email/src/main/resources/templates/_shared/persona-tone.html` (NEW — Thymeleaf fragment)
- `kitehub/kitehub-email/src/main/java/com/kitehub/email/template/PersonaToneResolver.java` (NEW)
- `kitehub/kitehub-email/src/test/java/com/kitehub/email/template/PersonaToneResolverTest.java` (NEW)
- `kitehub/kitehub-email/src/main/resources/templates/emails/class-rescheduled.html` (MODIFIED — consume fragment)
- `kitehub/kitehub-email/src/main/java/com/kitehub/email/service/ClassRescheduledEmailService.java` (MODIFIED — inject `personaGreeting`)
- `kitehub/kitehub-email/src/test/java/com/kitehub/email/service/ClassRescheduledEmailTemplateTest.java` (MODIFIED — add `personaGreeting` context)
- `scripts/audit-vn-sample-fixtures.sh` (NEW — VN sample detector WARN-mode)
- `scripts/tests/test-audit-vn-sample-fixtures.sh` (NEW — 3-fixture self-test)
- `scripts/tests/fixtures/audit-vn-sample-fixtures/{clean,known-bad,acceptable-english}/*` (NEW fixtures)
- `.github/workflows/quality-rules-skills.yml` (MODIFIED — wire `vn-sample-fixtures` job + path triggers)
- `documents/04-quality/gaps/phase-1-beta/GAP-NEW-email-tone-matrix.md` (NEW — this file)

## Related

- Wave plan: `documents/03-planning/waves/wave-2026-05-24-beta-readiness-4-meta-pdpl-pricing-reschedule-tone.md` §3.5
- Bucket D shipped: commit `883f43b8` `feat(wave-beta-readiness-4-bucket-d): reschedule + email fallback`
- Rule cross-link: `.claude/rules/vn-localization-audit-checklist.md` §2 Section 2 + §4.5
- Rule cross-link: `.claude/rules/dev-readable-doc-language.md` §4 (code-switching exception)
- Rule cross-link: `.claude/rules/incident-to-rule-pipeline.md` §3.1 (premature-rule guard)
