# GAP-321b.1 — Conduct facet wiring against `Incident.visibilityScope`

**Status:** 🟢 DONE 2026-05-05
**Priority:** 🟠 P1 (parent-portal completeness — Đ.83 K2 "đầy đủ thông tin")
**Domain:** Backend (kiteclass-core parent + kiteclass-core childprotection)
**Detected:** 2026-05-04 (Wave 18b3 Bucket C state-check)
**Affects:** Pa. Parent (P5 K-12) — conduct/hạnh kiểm facet returns empty in v1 stub
**Blocks:** True closure of GAP-321b — parent-portal facet completeness for Đ.83 K2 mandate

## Context

Wave 18b2 Bucket C (PR #773) shipped `ParentConductFacetServiceImpl` as a v1 stub returning empty list. Wave 18b3 Bucket C plan §3 originally proposed wiring it to `Incident` filtered by `visibilityScope IN (PARENT_VISIBLE, PUBLIC)` per BR-CHILD-PROTECT-005. State-check 2026-05-04 (per `audit-to-gap-pipeline.md` Step 2.5 hardened protocol) found:

- `grep -rn "visibilityScope\|visibility_scope" kiteclass/kiteclass-core/src/main/java kiteclass/kiteclass-core/src/main/resources/db/migration documents/01-business/kiteclass/child-protection` → **0 matches**.
- `grep -rn "BR-CHILD-PROTECT-005" documents/01-business/kiteclass/child-protection/rules.md` → **0 matches**.

The aspirational scope cannot ship without first authoring the visibility column + enum + business rule — out of scope for Wave 18b3 Bucket C's allowlist (which excluded `kiteclass-core/src/main/java/com/kiteclass/core/module/childprotection/**`).

## Problem

Conduct facet endpoint (`GET /api/v1/parent/children/{id}/conduct`) returns empty list. Parents using K-12 portal don't see hạnh kiểm rating/period data, even when the school has marked it. Đ.83 K2 mandates "đầy đủ thông tin về quá trình học tập, rèn luyện" — partial coverage breaches "đầy đủ" promise once visibility schema lands.

## Root Cause

Two upstream prerequisites missing:

1. `Incident` entity has no `visibilityScope` column / enum (`PARENT_VISIBLE` / `PUBLIC` / `STAFF_ONLY` / etc.) — querying without it risks PDPL Decree 13/2023 Art 16 violation (special protection for children's data; surfacing unverified `REPORTED` incidents to parents could leak unfounded accusations).
2. `documents/01-business/kiteclass/child-protection/rules.md` does not define `BR-CHILD-PROTECT-005` — the enumerated visibility-scope rule that the JPQL filter would reference.

Plus: even with the schema, conduct-as-Incident is a semantic stretch — hạnh kiểm rating is closer to a per-period grade/rubric than a child-protection ticket. May need a dedicated `conduct_record` table or an extension to `report_cards`. Decision deferred to this gap's design phase.

## Proposed Fix

### Phase 1 — schema authoring (precondition)
- Add `visibility_scope` enum column to `incidents` table (V54 migration); seed historical rows to `STAFF_ONLY` default.
- Add `IncidentVisibilityScope` Java enum (`PARENT_VISIBLE`, `PUBLIC`, `STAFF_ONLY`, `RESTRICTED`).
- Author BR-CHILD-PROTECT-005 in `documents/01-business/kiteclass/child-protection/rules.md` with 5-attribute frontmatter per `business-logic-review.md` §2.

### Phase 2 — design choice for hạnh kiểm storage
- Decide: extend `Incident` (semantic stretch, but already encrypted) OR add `conduct_record` table OR extend `report_cards`.
- Document decision via ADR.

### Phase 3 — wire `ParentConductFacetServiceImpl`
- Replace stub with JPQL: `SELECT i FROM Incident i WHERE i.subjectStudentId = :childId AND i.visibilityScope IN ('PARENT_VISIBLE', 'PUBLIC') AND i.deleted = false` (or analog against new conduct_record table).
- Add `@EntityGraph` on the query for any nested sensitive-decryption paths.
- Reuse `ParentConductFacetServiceImpl` test class — flip the `staffOnlyIncidentEquivalent_notExposedToParent` regression test from passing-trivially (empty list) to passing-against-real-data (real STAFF_ONLY row in fixture, must not appear in result).
- Hibernate Statistics test (mirror `ParentFeesFacetEntityGraphIT`) asserts `assertSelectCount ≤3`.

## Acceptance Criteria

- [x] V54 migration adds `visibility_scope` column to `incidents` (shipped Wave 19 Bucket A — `V54__add_incident_visibility_scope_and_audit_log.sql`)
- [x] BR-CHILD-PROTECT-005 authored in `documents/01-business/kiteclass/child-protection/rules.md` with 5-attribute frontmatter (shipped Wave 19 Bucket A)
- [x] Storage decision documented inline in `parent-portal/rules.md` BR-PARENT-FACET-CONDUCT-002 (extend `Incident` — same encryption + audit story; dedicated `conduct_record` table deferred until digital hạnh kiểm rating store ships separately)
- [x] `ParentConductFacetServiceImpl` returns real data filtered by `IncidentVisibilityScope IN (PARENT_VISIBLE, PUBLIC)` via `IncidentRepository.findVisibleForParentList`
- [x] `staffOnlyIncidentEquivalent_notExposedToParent` flipped from passes-trivially-against-empty-stub to passes-against-real-fixture-with-STAFF_ONLY-row + ArgumentCaptor verifies service requested only PARENT_VISIBLE + PUBLIC scopes
- [x] N+1 protection: `ParentConductFacetEntityGraphIT` asserts `assertSelectCount ≤3` prepared statements; STAFF_ONLY row in fixture must not appear in result
- [x] Sonar coverage ≥80% on changed Service code (5 unit tests cover all branches: 401 / 400 / 403 / linked-with-rows / linked-empty / scope-filter; toResponse + ratingFromSeverity exercised by linked-with-rows)
- [x] All 4 layers covered per `design-layer-coverage.md` §2.1: 要件定義 (BR-PARENT-FACET-CONDUCT-002 + BR-CHILD-PROTECT-005 + Pa. Parent persona) / 基本設計 (parent conduct screen wired to existing `/api/v1/parent/children/{id}/conduct`) / 詳細設計 (Service + Repository state + scope filter ADR-equivalent inline rules.md) / コンポーネント設計 (DTO `ParentConductFacetResponse` + Repository method)

## Out of Scope

- FE conduct-page polish (drill-down already wired Wave 18b1)
- Discipline (kỷ luật) facet — separate (deferred to GAP-321c)

## Estimated Effort

~3-5 days (schema migration + ADR review + JPQL wiring + regression-test flip + N+1 IT)

## Related

- **Parent gap:** GAP-321b (Phase 1B umbrella)
- **Sister sub-gaps:** GAP-321b.1-notifications-engine-wiring, GAP-321b.1-fees-instalment-payment-history
- **Depends on:** Authoring of BR-CHILD-PROTECT-005 in child-protection/rules.md (precedes the JPQL filter)
- **Wave plan:** `documents/03-planning/waves/wave-2026-05-04-18b3-k12-legal-phase-1b-remainder.md` §3 Bucket C
- **Source code touched (when filed):** `kiteclass/kiteclass-core/src/main/java/com/kiteclass/core/module/parent/service/impl/ParentConductFacetServiceImpl.java`, `documents/01-business/kiteclass/parent-portal/rules.md` §13.4

## Log

- **2026-05-04** Filed by Wave 18b3 Bucket C agent. State-check (per `audit-to-gap-pipeline.md` Step 2.5 hardened) confirmed `Incident.visibilityScope` does not exist anywhere in code/schema/business-rules. Bucket C scope-cut: keep v1 stub (with explicit BR-PARENT-FACET-CONDUCT-002 documenting the stub-stay reason) + file this sub-gap per `gap-done-discipline.md` §3 PARTIAL exit-ramp.

- **2026-05-05** Shipped Wave 19 Bucket D (stacked on Bucket A PR #793). `ParentConductFacetServiceImpl` rewired to query `IncidentRepository.findVisibleForParentList(childId, [PARENT_VISIBLE, PUBLIC])`. New repository method carries `@EntityGraph(attributePaths = {})` so query plan is single-SELECT. Hạnh kiểm rating projected coarsely from `Incident.severity` until digital rating store ships (LOW→TỐT / MEDIUM→KHÁ / HIGH→TRUNG_BÌNH / CRITICAL→YẾU). Encrypted `description` never projected — `title` surfaces as `remark`. Unit test flipped: `staffOnlyIncidentEquivalent_notExposedToParent` now uses ArgumentCaptor to verify the service passes only PARENT_VISIBLE + PUBLIC scopes, with a real STAFF_ONLY incident in the mocked-repo fixture. New IT `ParentConductFacetEntityGraphIT` asserts assertSelectCount ≤3 + STAFF_ONLY exclusion against TestContainers Postgres. BR-PARENT-FACET-CONDUCT-002 in `documents/01-business/kiteclass/parent-portal/rules.md` flipped from "stub stays" → "real wiring with visibility-scope filter" with full citation of BR-CHILD-PROTECT-005. Verification: `./mvnw -pl kiteclass-core clean verify -Dcheckstyle.skip=true` green. Reviewer: @nguyenvankiet (acting Product Owner + acting Legal scout, solo-dev). Compliance: Compliant — Luật GD 2019 Đ.83 K2 (right-to-information served) + PDPL Decree 13/2023 Art 16 (children's data minimization preserved via STAFF_ONLY default + scope filter).
