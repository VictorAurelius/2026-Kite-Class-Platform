# ADR-017: School MIS Sync Strategy (One-Shot vs Live Sync)

**Status:** PROPOSED
**Date:** 2026-04-21
**Deciders:** @tech-lead, @business-lead
**Reviewers:** @security, @dba, @pm-k12
**Related Gap(s):** GAP-200

## Context

KiteClass onboards K-12 schools that already run an existing MIS/SMS (VNEDU,
SMAS, Base.vn). Re-keying 300-2,000 students + 50-150 teachers by hand is a
**Tier-1 blocker** for the K-12 Principal persona (BRD §4.2). We need an import
path from the legacy MIS on Day 1.

Two realistic strategies exist:

1. **One-shot roster import at onboarding** — pull students/teachers/classes
   once, during provisioning. Subsequent changes managed inside KiteClass.
2. **Live bi-directional sync** — scheduled sync pulls changes from MIS every
   N minutes; optionally pushes KiteClass changes back.

Constraints shaping the decision:

- **No public VNEDU / SMAS API.** Partner tier pull-only, no webhooks.
  Live sync = polling = operational cost + risk of missing intra-poll changes.
- **Ownership of truth unclear.** If teacher adds a grade in KiteClass and the
  principal edits the same student in VNEDU, which wins?
- **Staff training cost.** Schools already train staff on VNEDU. Asking them to
  also double-enter everything into KiteClass defeats the value prop.
- **Pilot timeline.** We need to ship Phase 1 in this wave; partnership MoUs
  for live sync take 3-6 months.
- **Design-patterns rule §2** mandates Adapter pattern for vendor isolation
  and Strategy pattern for swappable sync modes.
- **Data-residency / PDPL** — every sync round is a "processing" event under
  PDPL. Fewer syncs = cleaner audit story.

## Decision

**We will implement one-shot roster import at onboarding, with MIS-wins
conflict resolution by default, configurable per tenant.**

Concretely:

1. Define a neutral `MisRosterSource` interface (Adapter pattern) that every
   MIS adapter implements. Adapters return a `RosterImport` DTO — vendor types
   do NOT leak into core.
2. Ship `VneduAdapter` as pilot skeleton this wave. SMAS + Base.vn + OneRoster
   CSV follow in Phase 2.
3. Import is triggered once during tenant provisioning (Wizard Step "Import
   from existing MIS"). Retry allowed with a cooldown. No scheduled polling
   in Phase 1.
4. Conflict resolution: when roster data overlaps existing KiteClass records
   (rare on Day 1; common during manual re-import), the **MIS is the winner**
   by default. Tenant admin may override to `KITECLASS_WINS` or
   `MANUAL_REVIEW` via `kiteclass.mis.conflict-strategy` setting.
5. Live sync (pull every N min) is explicitly **deferred to Phase 2** and
   requires an ADR amendment with partnership agreements in hand.

## Consequences

### Positive
- Faster time-to-value: schools onboard in 1 day instead of 2 weeks.
- Smaller PDPL surface: one import = one consent event, one audit record.
- No dual-source-of-truth headaches during pilot.
- Teachers use KiteClass as daily system; MIS becomes a one-way "parent" for
  historical reference only (matches how most schools actually use VNEDU).
- Adapter interface set now means Phase 2 live sync is a subclass addition,
  not a core refactor.

### Negative
- Staff must re-enter new students/teachers in BOTH KiteClass and MIS until
  Phase 2 sync ships. Mitigated by "export to VNEDU CSV" feature (Phase 2).
- If school switches back to MIS-only, roster drift over time. Mitigated
  because Phase 1 is explicitly a pilot — schools accept this.
- Conflict logic stays unused until first re-import; risk of bit-rot.
  Mitigation: integration test covers re-import path in Phase 2.

### Neutral
- Adapter-per-vendor code footprint: ~5 adapters × ~300 LOC each over Phase 1+2.
- Requires `integration/` package in `kiteclass-core` (new peer to `module/`).
- New config key `kiteclass.mis.conflict-strategy` lives in `application.yml`.

## Alternatives Considered

### Alternative A: Skip MIS integration, CSV only
Pros: No partnership fees, simple to ship.
Cons: K-12 principals repeatedly request "stop typing data twice"; CSV UX is
painful for 2,000-student schools; blocks Tier-1 persona objective.
Rejected because: a bulk-import CSV path already exists (`StudentBulkImportIT`)
— the missing piece is the **MIS-native** integration that justifies switching
to KiteClass. CSV alone is status quo.

### Alternative B: Live bi-directional sync from day one
Pros: "Real-time" sounds nice to buyers.
Cons: Polling-only APIs mean we'd claim "real-time" we can't deliver. Requires
partnership agreements (3-6 mo). Dual-source-of-truth introduces complex
conflict UX. Operational cost of per-tenant polling jobs.
Rejected because: no API surface supports true live sync today. Over-promising
hurts trust; under-delivering hurts retention.

### Alternative C: Build our own MIS (replace VNEDU)
Pros: Full control; no third-party dependency.
Cons: Years of regulatory work; competes with state-backed incumbent; outside
business scope.
Rejected because: KiteClass is a **complement** to VNEDU, not a replacement.
Trying to replace it would alienate 60%+ of the K-12 market.

## Implementation Notes

**Phase 1 (this wave — GAP-200):**
- `MisRosterSource` interface + `RosterImport` DTO
- `VneduAdapter` skeleton (no live API calls; structure only)
- Unit test validating interface contract
- 3-layer business docs + catalog + this ADR

**Phase 2 (deferred):**
- `VneduAdapter` live implementation once partner API credentials provisioned
- `SmasAdapter`, `BaseVnAdapter`, `OneRosterCsvAdapter`
- `MisImportOrchestrator` service (pull → validate → dedupe → commit)
- Onboarding wizard step "Import from MIS"
- Conflict-resolution admin UI
- Feature flag `kiteclass.mis.enabled` (default false until partner sign-off)
- Integration tests with MIS sandbox environments

**Rollback plan:** Phase 1 ships docs + interface + skeleton only — no runtime
behavior changes. Rollback = delete `integration/mis/` package; core unaffected.

**Monitoring (Phase 2):**
- Prometheus metric `mis_import_duration_seconds{provider, tenant}`
- Log event `mis.import.completed` per tenant
- Alert on >20% records rejected by validation

## References

- Design pattern used: `.claude/rules/design-patterns.md` §2 (Adapter, Strategy)
- Related ADRs: ADR-001 (K-12 data model), ADR-014 (async jobs queue)
- Related rules: `.claude/rules/ai-branding-guidelines.md` (consent wizard precedent)
- Related gap: `documents/04-quality/gaps/GAP-200-school-mis-integration.md`
- Catalog: `documents/02-architecture/integrations/school-mis-catalog.md`
- Bulk-import precedent: `kiteclass/kiteclass-core/src/main/java/com/kiteclass/core/module/student/bulkimport/`

## Log

- 2026-04-21 — Initial proposal (GAP-200 Phase 1)
