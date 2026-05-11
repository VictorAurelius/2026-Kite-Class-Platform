# GAP-361: Parent Portal Phase 1C remainder — 3 write actions + multi-facet consent gate + i18n + settings UI + re-consent flow

**Status:** 🔵 OPEN
**Priority:** 🟠 P1 (Phase 1C v1 sister gap; sibling of GAP-321c which shipped 1 of 4 write actions + 1 of 5 facet gates v1)
**Domain:** Backend + Frontend + Compliance + i18n
**Detected:** 2026-05-05 (Wave 19 Bucket C closure — GAP-321c PARTIAL flip)
**Affects:** P5 K-12 + secondary parents-in-P5; international K-12 schools (FIS, BIS) for i18n

## Context

Wave 19 Bucket C shipped Phase 1C **v1** (GAP-321c PARTIAL):
- ✅ V56 migration (additive `parent_student_links.parental_consent` JSONB + `parent_complaint_queue` table)
- ✅ ConsentService (checkConsent / getConsent / getConsentVersion / bumpConsent)
- ✅ ParentConsentController (`GET` + `PUT /api/v1/parent/consent`)
- ✅ ParentComplaintController (`POST /api/v1/parent/complaints` v1 — minimal write surface)
- ✅ Fees facet wired end-to-end with consent gate (`PARENT_CONSENT_REQUIRED` 403 when consent missing)
- ✅ BR-PARENT-PORTAL-011..013 with 5-attribute frontmatter (`business-logic-review.md` §2)

This follow-up tracks the **remainder** explicitly per `gap-done-discipline.md` §3 PARTIAL exit ramp.

## Problem (deferred items from GAP-321c v1)

### A. 3 remaining write actions (depend GAP-338/339 + MinIO encrypted bucket)

- `POST /api/v1/parent/conduct-reports/{id}/confirm` — acknowledge receipt of monthly conduct report
- `POST /api/v1/parent/meetings/{id}/rsvp` — depends `GAP-338` (parent-teacher meeting entity)
- `POST /api/v1/parent/absence-excuses` with evidence upload — depends MinIO encrypted bucket per Phase 1B `GAP-322b` LLTP pattern

### B. Apply consent gate to remaining 4 facets

GAP-321c v1 wired `fees` facet only as proof-of-pattern. Remaining facets need same wiring + IT updates per facet:

- `transcript` (GAP-321 Phase 1A facet)
- `attendance` (Phase 1B GAP-321b facet)
- `conduct` (Phase 1B GAP-321b facet — currently v1 stub per Wave 18b3 Bucket C)
- `notifications` (Phase 1B GAP-321b facet — currently v1 stub, hard-blocked by GAP-063b notification engine)

### C. Re-consent flow on policy version bump

PDPL Decree 13/2023 Art 16 K2 d implies parents re-consent when scope changes. Mechanic shipped in v1 (`bumpConsent` exists; `getConsentVersion` returns current); UX flow + admin tooling for bulk-bumps NOT shipped.

- Admin tool to bulk-bump `parental_consent.version` for all parents (e.g., when adding a new facet → all existing consents stale)
- FE prompt UX: when `getConsentVersion(parent, child)` < current policy version → modal "Privacy policy updated, please re-confirm consent for child X"
- Mock test exists in unit; wiring to real session/middleware pending

### D. Settings page UI (consent toggles per child)

GAP-321c v1 ships endpoints; FE consumer ships separately:

- `/parent/privacy` page listing children + per-field toggle
- Optimistic UI on PUT consent
- Visual diff of "what's visible vs hidden"
- Mobile-responsive (P5 K-12 parent persona uses mobile)

### E. i18n EN + zh-CN catalogs

International K-12 schools (FIS, BIS, etc.) need EN + zh-CN. Phase 1A/1B/1C v1 = Vietnamese-only. Strings to translate:

- All `PARENT_*` error codes (already in `messages.properties`)
- Settings page UI strings
- Email/notification templates that mention consent + complaint workflow
- Feature-flag if international tenants exist (skip if zero zh-CN/EN tenants signed up)

## Proposed Fix

### Phase 1C v2 (this gap) — split into 4 sub-tasks

| Sub | Scope | Estimated effort | Depends |
|-----|-------|------------------|---------|
| 360.A | Conduct-confirm + meeting-RSVP + absence-excuse upload (3 endpoints) | ~5 days | GAP-338, GAP-339, MinIO encrypted bucket |
| 360.B | Apply consent gate to remaining 4 facets + IT per facet | ~3 days | None (GAP-321c shipped pattern) |
| 360.C | Re-consent flow (admin bulk-bump + FE modal + middleware integration) | ~3 days | None |
| 360.D | i18n EN + zh-CN catalogs + feature-flag | ~2 days | International tenant signup — feature-flag default false |
| 360.E | Settings page UI (`/parent/privacy`) + tests | ~3 days | 360.B (multi-facet for full picture) |

Total: ~2-3 weeks if sequential; ~1.5 weeks with parallelism (360.B + 360.D parallel; 360.E after 360.B).

## Acceptance Criteria

- [ ] 3 remaining write actions shipped with scope guard + audit log entry
- [x] All 5 parent-portal facets gate consent (transcript + attendance + conduct + fees + notifications) — Wave 24 Bucket C 2026-05-06
- [x] Re-consent flow wired: admin bulk-bump endpoint + middleware check on facet calls — Wave 24 Bucket C 2026-05-06 (FE modal still pending in §C continuation)
- [ ] EN + zh-CN i18n catalogs (or feature-flagged skip if no international tenants)
- [ ] `/parent/privacy` settings page (consent toggles per child + per field; optimistic UI; mobile responsive)
- [x] BR-PARENT-PORTAL-014..016 + 5-attribute frontmatter — Wave 24 Bucket C 2026-05-06 (BR-017..018 deferred to §A/§D/§E follow-ups when scope lands)
- [ ] Tests: 4 write IT (conduct-confirm + RSVP + absence-excuse) — backend consent multi-facet gate + admin bulk-bump tests SHIPPED Wave 24 Bucket C 2026-05-06; write IT + i18n smoke + FE component tests still pending
- [ ] Migration to encrypted MinIO bucket for absence-excuse uploads

## Estimated Effort

~1.5-3 weeks (single dev solo-dev mode, parallelizable into 2 buckets if Wave 20 wave-pack).

## Related

- **Sister of:** GAP-321c (Phase 1C v1 PARTIAL — Wave 19 Bucket C 2026-05-05)
- **Depends on:** GAP-338 (parent-teacher meeting entity), GAP-339 (full complaint workflow), GAP-063b (notification engine) for facet C wiring, MinIO encrypted bucket per GAP-322b pattern
- **Cross-cuts:** PDPL Decree 13/2023 Art 16 (granular consent + re-consent on policy bump); Luật Giáo dục 2019 Đ.83 K2 (3 remaining write actions complete the implicit communication right)
- **Memory cross-link:** `feedback_post_merge_doc_sync.md` (closure PR pattern verified Wave 19 Bucket C)

## Log

- **2026-05-06** Wave 24 Bucket C (GAP-361 Phase 1C v1.5 §B + §C) — partial closure of remainder:
  - 361.B (consent gate × 4 remaining facets): SHIPPED. `ParentTranscriptServiceImpl` + `ParentAttendanceFacetServiceImpl` + `ParentConductFacetServiceImpl` + `ParentNotificationsFacetServiceImpl` now call `ConsentService.checkConsent(parentId, childId, "<facetName>")` after the existing scope guard; missing per-field consent → 403 `PARENT_CONSENT_REQUIRED`. Each impl exposes `public static final String CONSENT_FIELD_*` constant matching the JSONB field name.
  - 361.C (re-consent flow): SHIPPED (backend). All 5 facet impls (fees + 4 above) check `consentService.getConsentVersion(...) >= consentService.getRequiredVersion()`; stale → 403 `RECONSENT_REQUIRED`. Admin endpoint `POST /api/v1/admin/parent/consent/bulk-bump` ships with `@PreAuthorize("hasAnyRole('ADMIN','PRINCIPAL','OWNER')")`. ConsentService extended with `getRequiredVersion()` (reads `kite.parent.consent.required-version`, default 1) + `bulkBumpVersion(instanceId, newVersion, reason)` backed by native PostgreSQL `jsonb_set` UPDATE for single-round-trip bulk update.
  - 3 new BR rules (BR-PARENT-PORTAL-014/015/016) with full 5-attribute frontmatter added to `documents/01-business/kiteclass/parent-portal/rules.md` §15.
  - 3 new properties keys (`PARENT_CONSENT_REQUIRED`, `RECONSENT_REQUIRED`, `PARENT_CONSENT_BULK_BUMP_OK`) in `messages.properties` + `messages_vi.properties`.
  - Tests: 4 facet `consentMissing_throws403` + 4 facet `consentStale_throwsReconsentRequired` + `ConsentServiceImplTest` re-consent suite (6 new) + `ParentConsentAdminControllerTest` (2). Local mvn run: 107 tests PASS, 0 fail.
  - Out of scope this PR (deferred to follow-ups, gap stays 🔵 OPEN at coordinator closure):
    - 361.A (3 remaining write actions: conduct-confirm + meeting RSVP + absence-excuse upload) — depends GAP-338 + GAP-339 + MinIO encrypted bucket
    - 361.D (i18n EN + zh-CN catalogs)
    - 361.E (Settings page UI `/parent/privacy`) — Wave 25 FE wave
    - FE re-consent modal UX — Wave 25 FE wave
- **2026-05-05** Filed by Wave 19 Bucket C closure agent. Per `gap-done-discipline.md` §3 PARTIAL exit ramp, GAP-321c v1 ships PARTIAL not DONE — fees facet gated end-to-end + 1 write action live, remaining work tracked here.
