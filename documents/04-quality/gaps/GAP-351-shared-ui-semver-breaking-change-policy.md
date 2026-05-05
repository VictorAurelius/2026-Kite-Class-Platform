# GAP-351: `@kite/shared-ui` Semver + Breaking-Change Policy

**Status:** 🔵 OPEN
**Priority:** 🟠 P1 (gates Phase 2 component churn — without policy, every breaking change risks downstream consumer breakage)
**Domain:** Frontend / Design System / Governance
**Found:** 2026-05-05 (simulation-gap-finder — Persona: Developer × Stage: Evolution × Category: C10)
**Affects:** `packages/shared-ui/` (npm package consumed by `kiteclass-frontend`, `kitehub-frontend`)

## Problem

`@kite/shared-ui` package shipped Phase 1 (PR #713) without versioning policy. Once Phase 2 (GAP-349) lands 5 components and Phase 3 lands 7 more + 10 dialogs, downstream consumers (`kiteclass-frontend` + `kitehub-frontend`) will pin to specific versions. Without semver + breaking-change policy:

- Component v1 → v2 with prop rename breaks both consumers silently if version not bumped major
- No deprecation window for removed components
- No migration guide standard
- Downstream cannot decide safe-to-bump vs needs-coordination

## Current State (verified 2026-05-05)

| Artifact | Status |
|---|---|
| `packages/shared-ui/package.json` version | ✅ `0.0.0` (initial scaffold) |
| Semver policy doc | ❌ 0 hits "semver"/"breaking change" in `packages/shared-ui` |
| Component deprecation playbook | ❌ none |
| ADR for design-system versioning | ❌ ADR-024 covers strategy, not lifecycle |
| Consumer pin strategy | ⚠️ workspace `*` (auto-latest) — fine for monorepo, fragile when external |

## Proposed Fix

ADR + governance doc covering:

**Versioning rules:**
- MAJOR — component removal, prop rename without alias, theme token rename, render-output regression in default mode
- MINOR — new component, new optional prop, additional variant
- PATCH — internal refactor, bug fix, doc update

**Breaking-change discipline:**
- 1 release deprecation window (deprecated prop emits console warn)
- Migration guide in `packages/shared-ui/MIGRATIONS.md` per major
- Coordinate via `audit-gate.py` rule "shared-ui-major-bump-requires-migration-guide"
- Both consumers (kiteclass-frontend + kitehub-frontend) bump in same wave or pin previous major

**Per-component changelog:**
- `packages/shared-ui/src/components/{Name}/CHANGELOG.md` — newest-first
- Linked from package-level CHANGELOG.md aggregator

**Visual regression as breaking-change detector:**
- Playwright snapshot diff > threshold = MAJOR per default-output rule above
- Enforced in CI on Phase 2+

## Acceptance Criteria

- [ ] `documents/02-architecture/adr/ADR-025-shared-ui-versioning-strategy.md` written + ACCEPTED
- [ ] `packages/shared-ui/MIGRATIONS.md` template created
- [ ] `packages/shared-ui/CHANGELOG.md` started with entry for current scaffold (v0.0.0 → v0.1.0 on first component)
- [ ] `audit-gate.py` rule added: PR touching `packages/shared-ui/src/components/**` with version bump major must also touch `MIGRATIONS.md`
- [ ] Self-test fixture: synthetic PR removing a prop without major bump → audit-gate flags
- [ ] Cross-link added to GAP-349 (Phase 2 must include CHANGELOG entry per component bucket)

## Why P1

Per `meta-gap-priority.md` §3 — Meta tier (governance/process). Force-multiplier: 1 versioning rule × every future component PR. Skipping until Phase 4 means 12+ components shipped with no policy, then retroactive enforcement = pain.

## Related

- ADR-024 shared-ui strategy (`02-architecture/adr/ADR-024-shared-ui-lib-strategy.md`)
- GAP-273 Phase 1 (DONE PR #713) + GAP-349 Phase 2 (OPEN)
- Sister: `rule-change-process.md` — semver pattern proven on rules, applies to code packages similarly
- Audit hook precedent: `audit-gate.py` AUDIT_RULES

## Effort estimate

~1 day. ADR-025 draft (~3 hr) + MIGRATIONS template (~1 hr) + audit-gate rule + self-test (~2 hr) + closure PR.

## Log

- **2026-05-05:** Filed via simulation-gap-finder 3-axis matrix sweep. Discovered at Developer × Evolution × C10 cell. State-check: 0 mentions in repo for "semver" or "breaking change" in `packages/shared-ui`. P1 because gates Phase 2 (GAP-349) component churn discipline.
