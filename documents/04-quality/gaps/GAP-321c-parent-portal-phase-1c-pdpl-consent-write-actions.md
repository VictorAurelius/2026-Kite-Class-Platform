# GAP-321c: Parent Portal Phase 1C — PDPL granular consent + write actions

**Status:** 🔵 OPEN
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

- [ ] `parental_consent` JSONB column added with V<N> migration (backward compat)
- [ ] ConsentService gates all parent facet APIs (test: SECONDARY parent without consent for `fees` field returns 403)
- [ ] Settings page UI: per-field consent toggle + version display
- [ ] Re-consent flow on policy bump (mock test sufficient)
- [ ] 4 write endpoints with scope guard + audit log
- [ ] EN + zh-CN i18n catalogs (if international school tenants exist; else feature-flag)
- [ ] Business docs updated: BR-PARENT-PORTAL-011..018 + 4 new UCs
- [ ] Tests: ConsentService unit + 4 write IT + i18n smoke

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

- **2026-05-04** — Filed by Wave 18b1 closure coordinator. Per `gap-done-discipline.md` §3 PARTIAL exit ramp.
