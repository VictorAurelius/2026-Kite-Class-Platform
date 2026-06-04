# GAP-973: check-entity-mapper-consistency.sh self-test reports drift=1 on Widget PASS fixture

**Status:** 🔵 OPEN
**Priority:** P2
**Domain:** DevOps (CI script)
**Found:** 2026-06-04 (Wave flow-kh3 PR #2161 cluster diagnosis surfaced detector regression)
**Affects:** `scripts/check-entity-mapper-consistency.sh --self-test` CI job in `quality-code.yml`; every PR touching entity/migration/mapper triad scope

## Problem

`scripts/check-entity-mapper-consistency.sh --self-test` reports `pass_drift=1 expected 0` on the Widget PASS fixture (inline test fixture validating "no drift" baseline). Detector was shipped Wave meta-1 Bucket D 2026-05-25 with 2 inline fixtures (PASS + FAIL) both verified green at landing.

Originally surfaced PR #2161 CI as "Entity ↔ Migration ↔ Mapper triad drift (GAP-743)" job failure. Initial diagnosis attributed it to GAP-743 rule scope (entity-mapper drift class) — but actual cause is detector script bug, not real triad drift on the touched code.

Likely root cause (per CI fix agent diagnosis 2026-06-04): recent migration commits touched Payment entity fields that the detector now flags despite Widget fixture being designed as PASS baseline. Suspect commit: PR #2153 (`fix(gap-939): snapshot account_number + account_name into Payment from VietQR defaults`) — added new entity fields requiring inline fixture regeneration OR allowlist update.

Sister rule: `.claude/rules/design-patterns.md` §3.12 documents the detector limitations:
> grep-based, no AST. False positives expected on:
> - Inherited `BaseEntity` fields (filtered via allowlist: id, created_at, updated_at, ...)
> - Relations (`@OneToMany` / `List<X>` excluded)
> - Custom `@Column(name = "...")` overrides (parsed)
> - Migration columns added in later V file than entity (acceptable when same PR ships both)

Suggests detector v1 heuristic surface inherently fragile across entity additions.

## Root Cause (Investigation needed per `release-fix-retry-budget.md` §3.5)

Empirical state-check required:
1. Read `scripts/check-entity-mapper-consistency.sh` Widget PASS fixture inline definition (search for "Widget" inside script)
2. Run `bash scripts/check-entity-mapper-consistency.sh --self-test` locally + capture verbose output to identify which field triggered drift=1
3. Cross-reference with recent commits touching entity OR migration sources

Hypothesis A: Widget PASS fixture references field that recent migration commit removed/renamed
Hypothesis B: Allowlist in detector hasn't been updated for new BaseEntity sister field
Hypothesis C: Detector regex captures spurious match from inline string literal recently added

## Proposed Fix

Phase 1: Identify root cause (1 session, 15-30min Read tool work).

Phase 2 fix candidates:
- (a) Update inline Widget fixture to match current entity baseline shape
- (b) Extend detector allowlist to ignore newly-added fields
- (c) Tighten detector regex to avoid spurious match

CI job `entity-mapper-consistency` currently WARN-mode per `design-patterns.md` §3.12 Log "WARN-mode initially exit 0; HARD STOP deferred until 2nd recurrence". This GAP-973 IS the 2nd recurrence signal (first was Wave br-4 hotfixes #1784/#1787 driving original GAP-743 rule creation). Either fix detector OR keep WARN-only forever (lower-bar enforcement).

## Acceptance Criteria

- [ ] `bash scripts/check-entity-mapper-consistency.sh --self-test` exits 0 on main HEAD
- [ ] Detector job `entity-mapper-consistency` returns green on subsequent PRs touching kitehub-subscription entity/migration scope
- [ ] If fix is "expand allowlist" — document allowed-field rationale in `design-patterns.md` §3.12 sister Log entry

## Related

- Discovered in: PR #2162 CI fix agent diagnosis 2026-06-04 (originally misdiagnosed as triad drift; actual root = detector self-test bug)
- Parent rule: `design-patterns.md` §3.12 Entity-Migration-Mapper triad drift (Wave meta-1 Bucket D)
- Sister gap: GAP-743 (parent triad drift class — closed Wave meta-1)
- Likely triggering commit: PR #2153 GAP-939 (Payment entity field additions)
- Reference: `release-fix-retry-budget.md` §3.5 — investigation-first mandate before fix
- WARN-mode persistence option per `incident-to-rule-pipeline.md` §3.1 cost-benefit (detector complexity > value alternative)

## Log

- **2026-06-04** Gap filed during Wave flow-kh3 CI cluster post-mortem. PR #2161 (now closed superseded by #2162) hit "Entity ↔ Migration ↔ Mapper triad drift (GAP-743)" job failure; CI fix agent verified the failure is detector self-test bug, not real triad drift on PR diff. Investigation deferred per `release-fix-retry-budget.md` §3.5; tracked here for follow-up wave.
