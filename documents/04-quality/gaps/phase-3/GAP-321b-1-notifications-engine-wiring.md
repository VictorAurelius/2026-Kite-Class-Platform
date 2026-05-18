# GAP-321b.1 — Notifications facet wiring against parent-audience-scoped notification engine

**Status:** 🔵 OPEN
**Priority:** 🟠 P1 (parent-portal completeness — Đ.83 K2 "đầy đủ thông tin")
**Domain:** Backend (kiteclass-core parent + cross-cutting notification engine)
**Detected:** 2026-05-04 (Wave 18b3 Bucket C state-check)
**Affects:** Pa. Parent (P5 K-12) — notifications facet returns empty in v1 stub
**Blocks:** True closure of GAP-321b — parent-portal facet completeness for Đ.83 K2 mandate
**Depends on:** **GAP-063b** (cross-cutting notification engine — Wave 18a Bucket B, not yet shipped)

## Context

Wave 18b2 Bucket C (PR #773) shipped `ParentNotificationsFacetServiceImpl` as a v1 stub returning empty page. Wave 18b3 Bucket C plan §3 originally proposed wiring it to `Notification` filtered by `audienceScope ∋ {PARENT, ALL_PARENTS}`. State-check 2026-05-04 (per `audit-to-gap-pipeline.md` Step 2.5 hardened protocol) found:

- `find kiteclass/kiteclass-core/src/main/java -name "*Notification*.java"` (full output) → **0 hits in module domain scope**.
- `grep -rn "audienceScope\|audience_scope" kiteclass/kiteclass-core/src/main/java` → **0 matches**.

Per BR-PARENT-FACET-NOTIFY-001 (Wave 18b2 foundation), the cross-cutting notification engine ships in **Wave 18a Bucket B (GAP-063b)** which has not yet shipped.

## Problem

Notifications facet endpoint (`GET /api/v1/parent/children/{id}/notifications`) returns empty page. Parents using K-12 portal don't see school broadcasts, attendance alerts, fee reminders, or any other parent-targeted notification. Đ.83 K2 mandates "đầy đủ thông tin" — once GAP-063b lands, parent visibility into notifications is the highest-frequency expected interaction.

## Root Cause

Two upstream prerequisites missing:

1. **No `Notification` entity in `kiteclass-core`** — KiteHub's `NotificationPreference` (subscription tier preferences) is unrelated tier-preference data, not parent-targeted notifications.
2. **No `audienceScope` enum / column** — without it, JPQL cannot prevent leaking staff-targeted notifications to parents.
3. **GAP-063b parent dependency** — until the cross-cutting engine ships its parent-audience-scoped read API, this gap is blocked.

## Proposed Fix

### Phase 1 — wait for GAP-063b
- Track GAP-063b shipping; until it lands, notifications facet stays v1 stub.
- Coordinate with GAP-063b PR to ensure the engine exposes a parent-readable query surface (return only `audienceScope ∈ {PARENT, ALL_PARENTS}`).

### Phase 2 — wire `ParentNotificationsFacetServiceImpl`
- Replace stub with JPQL filtering by `studentId` + `audienceScope` + `tenant filter` + `from/to` date range.
- Add `@EntityGraph` on the query if the notification entity has nested rich-text or attachment collections.
- N+1 protection: assertSelectCount ≤3 (mirror `ParentFeesFacetEntityGraphIT`).
- Reuse the `ParentNotificationsFacetServiceImplTest` regression test `staffOnlyAudienceEquivalent_notExposedToParent` — flip from passing-trivially (empty page) to passing-against-real-data (real STAFF audience row in fixture must not appear in result).
- Map to `ParentNotificationFacetResponse` (already published Wave 18b2).

### Phase 3 — read-status (out of scope here)
- `readAt` field in DTO already exists. Marking-as-read is a write action — defer to **GAP-321c** (Phase 1C write actions).

## Acceptance Criteria

- [ ] GAP-063b shipped (precondition)
- [ ] `ParentNotificationsFacetServiceImpl` returns real data filtered by `audienceScope ∈ {PARENT, ALL_PARENTS}` + tenant + linked-child + date range
- [ ] Existing v1-stub regression test (`staffOnlyAudienceEquivalent_notExposedToParent`) flipped to use real fixture data + still passes
- [ ] N+1 protection: assertSelectCount ≤3 prepared statements per facet call
- [ ] Sonar coverage ≥80% on changed Service code
- [ ] All 4 layers covered per `design-layer-coverage.md` §2.1

## Out of Scope

- Read-status write action (mark as read) — deferred to GAP-321c
- Push notifications / email / SMS dispatch — covered by GAP-063b itself
- Per-tenant audience-scope rules customization

## Estimated Effort

~2-3 days once GAP-063b lands (JPQL wiring + regression-test flip + N+1 IT)

## Related

- **Parent gap:** GAP-321b (Phase 1B umbrella)
- **Sister sub-gaps:** GAP-321b.1-conduct-incident-visibility, GAP-321b.1-fees-instalment-payment-history
- **Hard-blocked by:** GAP-063b (cross-cutting notification engine — Wave 18a Bucket B)
- **Wave plan:** `documents/03-planning/waves/wave-2026-05-04-18b3-k12-legal-phase-1b-remainder.md` §3 Bucket C
- **Source code touched (when filed):** `kiteclass/kiteclass-core/src/main/java/com/kiteclass/core/module/parent/service/impl/ParentNotificationsFacetServiceImpl.java`, `documents/01-business/kiteclass/parent-portal/rules.md` §13.4

## Log

- **2026-05-04** Filed by Wave 18b3 Bucket C agent. State-check (per `audit-to-gap-pipeline.md` Step 2.5 hardened) confirmed `Notification` entity + `audienceScope` field do not exist anywhere in kiteclass-core. Bucket C scope-cut: keep v1 stub (with explicit BR-PARENT-FACET-NOTIFY-002 documenting the stub-stay reason) + file this sub-gap per `gap-done-discipline.md` §3 PARTIAL exit-ramp.
