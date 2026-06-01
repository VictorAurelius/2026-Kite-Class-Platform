# GAP-823 — `instances` table triad drift + trust-pass anti-pattern (META P0)

**Status:** 🔵 OPEN
**Priority:** P0 (META force-multiplier)
**Phase:** phase-1-beta
**Domain:** Meta/Backend
**Found:** 2026-06-01 (Wave onboarding-polish-2-execute state-check)
**Last-Verified:** 2026-06-01

## Problem

Wave onboarding-polish-1 plan §3 Bucket B proposed "wire `TenantSlugNormalizer` vào `InstanceService.createInstance`" assuming 1h scope. State-check 2026-06-01 (Wave onboarding-polish-2 pre-flight) revealed **deep architectural triad drift**:

| Symbol | Expected (per GAP-535 + V40 + Wave 77 outside-in failure-mode matrix F2) | Actual state | Verdict |
|---|---|---|---|
| `V40__tenant_slug_normalize.sql` | Migration shipped → `instances.slug VARCHAR(120)` column + UNIQUE INDEX + subdomain backfill | ✅ Shipped trong `kitehub-subscription/src/main/resources/db/migration/` | ✅ |
| `Instance` entity `slug` field | Field mapped via `@Column(name="slug")` | ❌ **MISSING** — entity only has `subdomain` field | 🔴 DRIFT |
| `InstanceRepository` (with `existsBySlugStartingWith`-class methods) | Repository exists for collision-suffix recovery loop | ❌ **không tồn tại** — no `InstanceRepository.java` in any package | 🔴 DRIFT |
| `InstanceService.createInstance` (normalizer call site) | Service calls `TenantSlugNormalizer.normalize()` + collision recovery 10-retry → 409 | ❌ **không tồn tại** — no `InstanceService.java` either package | 🔴 DRIFT |
| `TenantSlugNormalizer` production callers | At least 1 call site in production code (`InstanceService` per plan) | ❌ **ZERO** production callers — dead class (only test invokes it) | 🔴 DEAD |
| Service ownership of `instances` table | Single service owns CRUD + schema migration | ⚠️ Cross-service: V40 in `kitehub-subscription` migration dir, `Instance.java` in `kitehub-platform` package | 🟠 ARCH |

**Trust-pass anti-pattern recurrence (per `feedback_audit_of_trust_pass.md` memory):** Wave meta-7 Bucket A audit-catalog (2026-06-01) flipped GAP-535 → DONE based on "class + migration shipped" criteria WITHOUT verifying wiring AC. The original GAP-535 §Acceptance Criteria explicit included:
- [ ] `InstanceService.createInstance` calls `TenantSlugNormalizer.normalize()` before persist
- [ ] Collision recovery 10-retry loop with `existsBySlug*` repository method
- [ ] IT verifies VN diacritic case → DB row slug normalized

→ All 3 wiring AC unchecked at flip time. Per `gap-done-discipline.md` §2 — flip was premature. Per `feature-ship-runtime-walk-mandate.md` §3 — no RST walk evidence. Per `cross-flow-bug-class-sweep.md` §3 — no sweep cho similar "class shipped without callers" pattern across other Wave 77 deliverables.

## Root Cause

**Layer 1 — Wave 77 ship discipline:**
- TenantSlugNormalizer class + V40 migration shipped Wave 77 Bucket D as "infrastructure"
- Bucket D did NOT include entity field add / repo create / service wire / IT
- GAP-535 left as PARTIAL pending Bucket B wiring follow-up

**Layer 2 — Wave meta-7 catalog flip miss:**
- Bucket A "catalog apply" mass-flipped 19 SHIPPED-DONE based on "class file + migration file exists" heuristic
- Heuristic missed wiring AC verification (Service call site, Repository methods, Entity field mapping)
- Per `gap-done-discipline.md` §2 — banned for DONE flip without all AC ticked
- META P0 surface: audit-catalog detector needs cross-flow wiring sweep before bulk flip (see Proposed Fix §3)

**Layer 3 — Architectural ownership ambiguity:**
- `instances` table lives in subscription DB schema (V40 in subscription migration dir)
- But `Instance.java` entity lives in `kitehub-platform` package
- Cross-service entity/migration ownership creates ambiguity about who handles CRUD + slug normalization
- Need ADR: single-service ownership of `instances` table (probably platform owns entity + Service + Repository; subscription stops managing the migration OR moves V40 → platform migration dir)

## Proposed Fix (META P0 — 2-phase)

### Phase 1: Architectural decision + entity-side fix (Wave meta-9 candidate, ~3h)

1. **ADR creation:** `documents/02-architecture/adr/ADR-NNN-instances-table-ownership.md`
   - Decision: which service owns `instances` table CRUD + schema migration
   - Options: (a) platform owns all, subscription read-only via JPA shared model; (b) subscription owns all, platform reads via API; (c) hybrid với clear boundary
   - Recommend (a) per existing `Instance.java` in platform + existing platform domain enums
2. **Per ADR decision, ship triad:**
   - Add `@Column(name="slug")` field to `Instance.java` entity (if platform-owns)
   - Move V40 migration to platform service migration dir (if subscription was wrong location)
   - Create `InstanceRepository` interface + `existsBySlugStartingWith(String prefix)` method
   - Create `InstanceService` với `createInstance()` method calling `TenantSlugNormalizer.normalize()` + collision loop (10-retry → 409)
   - IT verifies: VN diacritic "Trường Mầm Non 'Hoa Mai'" → DB row `slug=truong-mam-non-hoa-mai` + collision case `truong-mam-non-hoa-mai-1`
3. **GAP-535 re-open from `phase-1-beta/closed/` → `phase-1-beta/` per `gap-folder-organization.md` v2.0.0:**
   - Re-open Status PARTIAL với completion_pct=70 (V40 + Normalizer shipped; wiring pending)
   - Sync CSV row + git mv file

### Phase 2: META detector + audit-catalog trust-pass elimination (Wave meta-9 candidate, ~2h)

Per `meta-gap-priority.md` §3 META P0 force-multiplier:

1. **Audit-catalog trust-pass detector:** `scripts/check-audit-catalog-trust-pass.sh`
   - Input: list of gaps proposed for DONE flip via audit-catalog bulk operation
   - For each gap, verify ALL `### Acceptance Criteria` checkboxes are `[x]` BEFORE flip
   - For each gap với "wire/integrate/connect X into Y" AC pattern, verify Y file's `grep` returns X-related call sites
   - Exit 1 on any unchecked AC OR missing wiring evidence
2. **Wire vào audit-catalog skill** workflow: pre-flight check before bulk DONE flip
3. **Cross-flow wiring sweep** of Wave 77 deliverables: enumerate all "class shipped" + grep for production callers; surface any other dead-class triad drift (suspected candidates: any Wave 77 Bucket D class with zero production import)

## Acceptance Criteria

### Phase 1 (Wave meta-9 Bucket A)
- [ ] ADR-NNN ships với architectural decision (single-service ownership of `instances`)
- [ ] `Instance.java` entity has `slug` field với `@Column(name="slug")` + `@Size(min=3, max=120)` + NULL allowed (matches V40 partial unique index)
- [ ] `InstanceRepository` exists với `existsBySlugStartingWith(String)` + `findBySlug(String)` methods
- [ ] `InstanceService.createInstance(CreateInstanceRequest)` calls `TenantSlugNormalizer.normalize()` + collision-recovery 10-retry loop → 409 IllegalStateException after exhaust
- [ ] IT: Testcontainers VN diacritic input → 201 + DB row `slug` normalized + collision case generates `-1` suffix
- [ ] GAP-535 re-opened từ closed/ → re-flip DONE only after this Phase complete + RST walk evidence per `feature-ship-runtime-walk-mandate.md`

### Phase 2 (Wave meta-9 Bucket B — META detector)
- [ ] `scripts/check-audit-catalog-trust-pass.sh` shipped với self-test fixtures (3 fixtures: clean DONE flip / unchecked AC / dead-class wiring)
- [ ] CI job wired trong `quality-docs.yml` WARN-mode initially
- [ ] Wave 77 cross-flow sweep complete — list of any sister dead-class triad drift surfaced + gap files filed
- [ ] `feedback_audit_of_trust_pass.md` memory entry updated với this recurrence cited

## Related

- Wave onboarding-polish-1 plan §3 Bucket B (defer rationale) — `documents/03-planning/waves/wave-2026-06-01-onboarding-polish-1-cluster-close.md`
- Wave onboarding-polish-2-execute plan §3 Bucket B punt — `documents/03-planning/waves/wave-2026-06-01-onboarding-polish-2-execute.md`
- GAP-535 (closed, re-open candidate) — `documents/04-quality/gaps/phase-1-beta/closed/GAP-535-tenant-slug-normalize-vn-diacritics.md`
- `feedback_audit_of_trust_pass.md` memory — trust-pass anti-pattern recurrence tracking
- `meta-gap-priority.md` §3 — META P0 force-multiplier rationale
- `gap-done-discipline.md` §2 — AC verification required before DONE flip
- `feature-ship-runtime-walk-mandate.md` §3 — RST walk required for user-facing features
- `cross-flow-bug-class-sweep.md` §3 — sister flow sweep mandate
- Wave 77 Bucket D (TenantSlugNormalizer + V40 ship) — Wave 77 plan reference
- Wave meta-7 PR #2007 (catalog flip incident) — `git show 71a0f880`

## Log

- **2026-06-01** (Wave onboarding-polish-2-execute state-check): Gap filed. State-check Wave onboarding-polish-2 pre-flight surfaced triad drift TRƯỚC khi commit code: `Instance.java` no slug field + no `InstanceRepository` + no `InstanceService` + `TenantSlugNormalizer` zero production callers + cross-service V40-vs-entity ownership ambiguity. Per `incident-to-rule-pipeline.md` Detect+Classify stages applied. Bucket B (originally ~1h in Wave onboarding-polish-1 plan) revised to ~3h Phase 1 (architectural decision + triad fix) + ~2h Phase 2 (META detector + sweep). Punted from Wave onboarding-polish-2-execute to Wave meta-9 candidate. META P0 priority per `meta-gap-priority.md` §3 force-multiplier — fix once → audit-catalog trust-pass eliminated permanently. Cross-link với recurrence ≥7 trust-pass pattern per `feedback_audit_of_trust_pass.md` memory.
