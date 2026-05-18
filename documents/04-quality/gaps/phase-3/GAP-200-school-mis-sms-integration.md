# GAP-200: School MIS / SMS Integration (VNEDU, SMAS, Base.vn)

**Status:** 🔵 OPEN
**Priority:** 🟠 P1 (business-logic tier — P5 K-12 onboarding blocker)
**Domain:** Integration / Backend / KiteClass / BRD
**Found:** 2026-04-20 (simulation-action-1 Part C — P5 × Onboard × Integration)
**Wave:** Wave 9 or 10
**Affects:** P5 K-12 School persona onboarding, student/class roster import, parent account creation

## Problem

K-12 schools already use one of the Vietnamese MIS/SMS systems for roster, grade book, attendance. No gap covers importing from them:

- VNEDU (Vietnamese Education Ministry — most common public-school MIS)
- SMAS (School Management System, widespread in urban schools)
- Base.vn (Base HRM — includes school-module product line)
- Microsoft School Data Sync, Google Classroom rosters (international-leaning)

Without integration:
- Onboarding a P5 school = manual retype of every student/class (dealbreaker)
- Parent accounts cannot be pre-created from MIS household data
- Risk of data divergence (MIS = source of truth at school, platform = copy)

## Context

Discovered via system-simulation axis P5 × Onboard × Integration (simulation-action-1 Part C). P5 (K-12 School) is user's strategic priority persona per BRD.

## Proposed Fix

1. **Integration catalog doc** — `documents/02-architecture/integrations/school-mis-catalog.md`
   - Per-MIS: API availability, auth mode, data model, update cadence, licensing
2. **Unified import contract** — domain `RosterImport` DTO neutral to source MIS
3. **Adapter pattern (per design-patterns.md)** — `VneduAdapter`, `SmasAdapter`, `BaseVnAdapter` all implementing `MisRosterSource`
4. **Bulk-import reuse** — piggyback on GAP-051 bulk import users XLSX (already DONE) as fallback when API unavailable
5. **Sync strategy**
   - One-shot: import once at onboarding
   - Ongoing: scheduled poll vs webhook (per MIS capability)
   - Conflict resolution: MIS-wins vs platform-wins, configurable per tenant
6. **Pilot**: VNEDU first (highest K-12 coverage), SMAS second, Base.vn third
7. **3-layer docs** — `documents/01-business/kiteclass/mis-integration/`

## Acceptance Criteria

### Phase 1 — Discovery + ADR
- [ ] Integration catalog with API details for VNEDU / SMAS / Base.vn
- [ ] ADR on sync strategy (one-shot vs live sync) per MIS
- [ ] Legal review: data processor agreement requirements

### Phase 2 — Pilot (VNEDU)
- [ ] VneduAdapter implements `MisRosterSource`
- [ ] Import wizard step in onboarding
- [ ] Conflict-resolution UI for duplicate students (email / national-id match)
- [ ] E2E test: import 100 students + 5 classes from VNEDU sandbox
- [ ] Tenant can re-run import on demand

### Phase 3 — Expansion
- [ ] SMAS + Base.vn adapters
- [ ] Scheduled sync worker (configurable frequency)

## Out of Scope

- Google Classroom / MS School Data Sync — international-leaning, separate gap if Tier 2 expansion happens
- Grade-book sync — separate integration scope (student roster only here)

## Related

- simulation-action-1-2026-04-20.md Part C (P5 × Onboard × Integration)
- GAP-051 bulk import users XLSX (fallback path)
- GAP-052 parent portal (consumes parent roster from MIS)
- GAP-150 BRD docs completion (K-12 persona)
- GAP-186 child protection policy (data-processor agreement requirements)
- Rule: `.claude/rules/meta-gap-priority.md` §3 (BL-P1, persona-coverage impact)
- Rule: `.claude/rules/design-patterns.md` (Adapter pattern mandatory)

## Log

- 2026-04-20 — Created from simulation Part C.
