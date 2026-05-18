# GAP-321c: Parent Portal Phase 1C — PDPL granular consent + write actions

**Status:** 🟡 PARTIAL — Wave 19 Bucket C v1 SHIPPED 2026-05-05; Phase 1C remainder follow-up: [GAP-360-parent-portal-phase-1c-remainder](GAP-360-parent-portal-phase-1c-remainder.md)
**Priority:** 🟠 P1 (sister of GAP-321 Phase 1A SHIPPED Wave 18b1; sister of GAP-321b Phase 1B)
**Domain:** Backend + Frontend + Compliance
**Detected:** 2026-05-04 (Wave 18b1 Bucket D closure)
**Affects:** P5 K-12 + secondary parents-in-P5

## Context

Phase 1A (Wave 18b1) shipped read-only transcript. Phase 1B (GAP-321b) adds 5 read-only facets + Zalo OTP + audit log. This gap (1C) adds **write actions** + **PDPL Decree 13/2023 Art 16 granular consent tracking**.

## Problem

PDPL Art 16 children-data special protection requires:
- Per-field parental consent flag (not just blanket "all visible")
- Consent versioning (parent re-consents on policy change)
- Granular field minimization (only fields needed per use case)

Plus parents need write actions per Đ.83 K2 implicit communication right:
- File complaint (GAP-339)
- Confirm receipt of monthly conduct report
- RSVP parent-teacher meeting (GAP-338)
- Submit absence excuse with evidence upload

## Proposed Fix

### 1C.1 — PDPL granular consent
- Add `parental_consent` JSONB column to `parent_student_links` (per-field visible flags + consent version + timestamp)
- Migration V<N> kiteclass-core (extends existing `parent_student_links` from V42 — additive, backward compat)
- Service: ConsentService.checkConsent(parentId, childId, field) gate before facet returns data
- UI: parent settings page to toggle per-field consent per child
- Re-consent prompt on policy version bump

### 1C.2 — Write actions (4 endpoints)
- `POST /api/v1/parent/complaints` (depends GAP-339)
- `POST /api/v1/parent/conduct-reports/{id}/confirm` (acknowledge receipt)
- `POST /api/v1/parent/meetings/{id}/rsvp` (depends GAP-338)
- `POST /api/v1/parent/absence-excuses` with evidence upload (MinIO encrypted bucket per Phase 1B GAP-322b pattern)
- All require ParentStudentLink scope guard + audit log entry

### 1C.3 — i18n EN + zh-CN (international schools)
- Phase 1A/1B Vietnamese-only; international K-12 schools (FIS, BIS, etc.) need EN/zh-CN
- Add i18n catalog files for parent-facing strings

## Acceptance Criteria

- [x] `parental_consent` JSONB column added with V56 migration (backward compat — `DEFAULT '{"fields":{}, "version":1, "updatedAt":null}'::jsonb`; existing V42 rows backfilled by clause)
- [x] ConsentService gates `fees` facet end-to-end (`ParentFeesFacetConsentGateIT` proves SECONDARY parent without consent for `fees` returns 403 `PARENT_CONSENT_REQUIRED`); remaining 4 facets → [GAP-360](GAP-360-parent-portal-phase-1c-remainder.md) sub-task B
- [ ] Settings page UI: per-field consent toggle + version display → [GAP-360](GAP-360-parent-portal-phase-1c-remainder.md) sub-task E (endpoints shipped, FE consumer separate)
- [ ] Re-consent flow on policy bump → [GAP-360](GAP-360-parent-portal-phase-1c-remainder.md) sub-task C (mechanic shipped via `bumpConsent`/`getConsentVersion`; admin bulk-bump tooling + FE modal pending)
- [x] 1 of 4 write endpoints with scope guard (`POST /api/v1/parent/complaints` v1); 3 remaining (conduct-confirm, RSVP, absence-excuse) → [GAP-360](GAP-360-parent-portal-phase-1c-remainder.md) sub-task A
- [ ] EN + zh-CN i18n catalogs → [GAP-360](GAP-360-parent-portal-phase-1c-remainder.md) sub-task D (feature-flag if no international tenants)
- [x] Business docs updated: BR-PARENT-PORTAL-011..013 with 5-attribute frontmatter (`Source` PDPL Decree 13/2023 Art 16 + `Compliance` Compliant) + UC-PARENT-CONSENT-MANAGE + UC-PARENT-COMPLAINT-FILE in use-cases.md; remaining BR-014..018 → [GAP-360](GAP-360-parent-portal-phase-1c-remainder.md)
- [x] Tests: `ConsentServiceImplTest` (unit) + `ParentConsentControllerIT` + `ParentComplaintControllerIT` + `ParentFeesFacetConsentGateIT` (consent gate end-to-end) + `ParentFeesFacetServiceImplTest` updated with consent assertions; remaining facet/i18n tests → [GAP-360](GAP-360-parent-portal-phase-1c-remainder.md)

## Estimated Effort

~1-2 weeks:
- 321c.1: PDPL consent (~5 days)
- 321c.2: 4 write actions (~5-7 days, depends GAP-338/339)
- 321c.3: i18n (~2 days)

## Related

- **Sister of:** GAP-321 Phase 1A (PR #766) + GAP-321b Phase 1B
- **Depends on:** GAP-338 (parent meeting), GAP-339 (complaint workflow)
- **Cross-cuts:** PDPL Decree 13/2023 Art 16; GAP-322b/c (audit log pattern, MinIO encrypted bucket)

## Log

- **2026-05-05** Wave 19 Bucket C v1 SHIPPED — Status flipped 🔵 OPEN → 🟡 PARTIAL. Salvaged from pre-WSL-restart agent work + verified `mvn test` BUILD SUCCESS for 6 targeted tests. Ships: V56 migration (additive `parental_consent` JSONB on `parent_student_links` + `parent_complaint_queue` table) + ParentalConsent DTO record (typed `fields` map + version + updatedAt; `@JdbcTypeCode(SqlTypes.JSON)` paired with `columnDefinition = "jsonb"` per `feedback_jpa_jsonb_jdbctypecode.md`) + ConsentService (checkConsent / getConsent / getConsentVersion / bumpConsent — fail-safe deny on missing key) + ParentConsentController (GET + PUT `/api/v1/parent/consent`) + ParentComplaintController + ParentComplaintService + ParentComplaint entity + repository (`POST /api/v1/parent/complaints` v1 with scope guard) + ParentFeesFacetServiceImpl gated by `consentService.checkConsent` (returns 403 `PARENT_CONSENT_REQUIRED` when consent missing) + BR-PARENT-PORTAL-011..013 with 5-attribute frontmatter (Source = PDPL Decree 13/2023 Art 16 statute citation + Đ.83 K2 statute citation; Compliance = **Compliant**; Cadence = Annual + event-driven on Decree 13/2023 implementing-decree publication) + UC-PARENT-CONSENT-MANAGE + UC-PARENT-COMPLAINT-FILE in use-cases.md + 4 new tests (`ConsentServiceImplTest` + `ParentConsentControllerIT` + `ParentComplaintControllerIT` + `ParentFeesFacetConsentGateIT`) + 2 updated tests (`ParentFeesFacetServiceImplTest` + `ParentReadAuditLogIntegrationTest`). Reviewer: @nguyenvankiet (acting Product Owner + acting Legal scout, solo-dev). Verification artifact: `mvn test -Dtest='ConsentServiceImplTest,ParentFeesFacetServiceImplTest,ParentReadAuditLogIntegrationTest,ParentConsentControllerIT,ParentComplaintControllerIT,ParentFeesFacetConsentGateIT' -Dcheckstyle.skip=true` → `[INFO] BUILD SUCCESS`. Remaining Phase 1C work tracked in [GAP-360](GAP-360-parent-portal-phase-1c-remainder.md): 3 remaining write actions (depends GAP-338/339/MinIO encrypted bucket) + 4 remaining facet consent gates + i18n EN/zh-CN + settings UI + re-consent flow admin bulk-bump tooling + FE modal. Per `gap-done-discipline.md` §3 PARTIAL exit ramp + `incident-to-rule-pipeline.md` 5-stage retro: filing follow-up gap with concrete sub-tasks satisfies discipline (no banned phrases without paired follow-up).
- **2026-05-04** — Filed by Wave 18b1 closure coordinator. Per `gap-done-discipline.md` §3 PARTIAL exit ramp.
