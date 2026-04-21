# GAP-200: School MIS/SMS Integration (VNEDU, SMAS, Base.vn)

**Status:** 🟡 PARTIAL (Phase 1 shipped — Phase 2 deferred)
**Priority:** 🟠 P1 (Business-Logic)
**Domain:** KiteClass Core / Integration
**Found:** 2026-04-20 (Wave 9 persona review — K-12 Principal blocker)
**Affects:** K-12 tenant onboarding (Tier-1 persona), retention, perceived time-to-value

---

## Problem

K-12 schools in Vietnam already operate an existing MIS/SMS (typically VNEDU
~60%, SMAS ~22%, Base.vn ~6%). Re-typing 300-2,000 students + 50-150 teachers
into KiteClass during onboarding is a **hard blocker** per persona interviews
with principals and IT managers.

Without an MIS import path, KiteClass onboarding takes 1-2 weeks per school
instead of 1 day. This kills K-12 sales velocity and is the #1 objection
raised in BRD §4.2 persona feedback.

Additionally the codebase had zero abstraction for MIS integration — any
Phase 2 work would have to invent the interface + docs + ADR from scratch
under time pressure, leading to leaky vendor types in the core module (bad
outcome already warned against in `.claude/rules/design-patterns.md` §3.10).

## Root Cause

1. MIS integration was not part of Wave 1-4 MVP scope (AI branding + core
   data model took priority).
2. No partnership channel was opened with VNEDU / SMAS / Base.vn vendors.
3. No neutral adapter interface existed, so any ad-hoc implementation would
   couple core code to vendor types.

## Proposed Fix

Two-phase delivery:

### Phase 1 (THIS PR — wave 9)

Structure + skeleton + docs. No live API calls. Locks the contract so Phase 2
is a pure additive effort.

Deliverables:
1. Integration catalog doc comparing VNEDU / SMAS / Base.vn / MS SDS /
   Google Classroom on API, auth, data model, cost, residency.
2. ADR-017 selecting one-shot import at onboarding + MIS-wins default
   conflict strategy.
3. 3-layer business docs (`rules.md`, `use-cases.md`, `api-contract.md`) in
   `documents/01-business/kiteclass/mis-integration/`.
4. `MisRosterSource` interface + `RosterImport` neutral DTO + `MisProvider`
   enum + `MisConnectionStatus` + `MisIntegrationException` in
   `kiteclass-core/src/main/java/com/kiteclass/core/integration/mis/`.
5. `VneduAdapter` pilot skeleton (no HTTP calls; `ping()` returns failed,
   `fetchRoster()` throws `MisIntegrationException` to fail loud).
6. `VneduAdapterTest` unit test validating interface contract + fail-loud
   behavior (10 test methods).

### Phase 2 (deferred — next wave, successor gap)

- Partnership MoU with Viettel (VNEDU + SMAS) — BUSINESS BLOCKER
- Partnership / API keys with Base.vn
- Live HTTP client per adapter (WireMock-based integration tests)
- `MisImportOrchestrator` service + async RabbitMQ worker (per ADR-014)
- `MisImportController` + `MisConflictController`
- `MisCredentialsService` (encrypted-at-rest per BR-MIS-SEC-001)
- Onboarding wizard step "Import from existing MIS"
- Conflict-resolution admin UI
- `SmasAdapter`, `BaseVnAdapter`, `OneRosterCsvAdapter`
- Feature flag flip to `true` on partnership sign-off
- E2E tests against VNEDU sandbox

## Acceptance Criteria

### Phase 1 (this PR)
- [x] Catalog doc comparing ≥5 MIS providers on 6+ dimensions
- [x] ADR-017 proposed with ≥2 alternatives considered
- [x] 3-layer business docs complete (rules + use-cases + api-contract)
- [x] `MisRosterSource` interface documented + immutable
- [x] `RosterImport` DTO neutral (no vendor types)
- [x] `VneduAdapter` skeleton implements interface
- [x] Unit test validates contract + fail-loud behavior
- [x] No vendor SDKs added to `pom.xml` (structure-only PR)
- [x] ADR README index updated (016 + 017 added)

### Phase 2 (deferred successor gap)
- [ ] VNEDU sandbox integration test green
- [ ] Tenant can import real roster via wizard in <10 min for 1,000-student school
- [ ] Conflict resolution UI covers MIS_WINS / KITECLASS_WINS / MANUAL_REVIEW
- [ ] PDPL DPA template published + linked from wizard
- [ ] 3 adapters shipped (VNEDU live, SMAS, OneRoster CSV minimum)

## Related

- ADR: `documents/02-architecture/adr/ADR-017-mis-sync-strategy.md`
- Catalog: `documents/02-architecture/integrations/school-mis-catalog.md`
- Rules: `documents/01-business/kiteclass/mis-integration/rules.md`
- Use cases: `documents/01-business/kiteclass/mis-integration/use-cases.md`
- API contract: `documents/01-business/kiteclass/mis-integration/api-contract.md`
- Design patterns rule: `.claude/rules/design-patterns.md` §2 (Adapter, Strategy)
- Meta-gap priority rule: `.claude/rules/meta-gap-priority.md` (business-logic tier)
- Bulk import precedent: `kiteclass/kiteclass-core/src/main/java/com/kiteclass/core/module/student/bulkimport/`
- Async queue precedent: ADR-014 (RabbitMQ over batch)

## Log

- 2026-04-21 — 🟡 PARTIAL — Phase 1 shipped (wave 9 agent C). Interface,
  neutral DTO, VNEDU skeleton, catalog, ADR-017 (PROPOSED), 3-layer docs,
  10-test unit test landed. Zero runtime behavior change. Phase 2 requires
  partnership MoU (business blocker) — tracked for next wave after Legal
  + Partnership engagement with Viettel.
- 2026-04-20 — 🔵 OPEN — gap filed during Wave 9 persona review; K-12
  principal persona flagged as Tier-1 onboarding blocker.
