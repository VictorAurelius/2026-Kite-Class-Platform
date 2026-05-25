# GAP-743: Entity-Migration-Mapper triad CI gate — prevent hotfix #1784/#1787 class

**Status:** 🔵 OPEN
**Priority:** 🟠 P1 META
**Domain:** Meta (CI tooling)
**Found:** 2026-05-25 (Wave audit-1 Bucket D Ops Readiness audit)
**Affects:** Deployment pipeline quality; hotfix iteration count

## Problem

Per `documents/04-quality/audits/ops-readiness/2026-05-25-wave-br-4-ops-readiness-audit.md` §OPS-BR4-002:

Wave beta-readiness-4 ship 3 post-merge hotfixes; 2/3 preventable tại CI time:
- **Hotfix #1784** (`5e3ceebe`): Course entity `pricingModel` field thiếu — bucket C ship PricingCalculator + tests nhưng forgot entity field. Surface qua IDE post-merge.
- **Hotfix #1787** (`9ce75c17`): ClassMapper `@Mapping` ignore 6 reschedule audit columns thiếu — surface qua CI strict-warnings.

Root cause systemic: entity field ↔ migration column ↔ MapStruct `@Mapping` triad consistency chưa được verify tại CI time. Hiện chỉ catch ở runtime/IDE level.

Force-multiplier: 60% bucket hotfix rate Wave br-4 = signal pipeline quality cần improve. Future code waves sẽ tiếp tục hit hotfix pattern nếu không file CI gate.

## Proposed Fix

CI check script `scripts/check-entity-mapper-consistency.sh` (hoặc Java `@Test` integration test) verify:
1. Entity field declared in `*.java` → matching column in `db/migration/V*.sql`
2. Entity field declared → MapStruct `@Mapping` coverage (no silent ignore unless `@Mapping(target=..., ignore=true)` explicit)
3. Migration columns → entity field declared (catch DB-only changes that lack JPA mapping)

Approach option A: Bash script parse Java + SQL via regex (fast, fragile)
Approach option B: Java integration test load entity metadata + compare to migration SQL parse (more robust)

## Acceptance Criteria

- [ ] CI script OR integration test cover entity-migration-mapper triad
- [ ] Self-test: hotfix #1784 scenario caught (Course entity missing field)
- [ ] Self-test: hotfix #1787 scenario caught (ClassMapper missing @Mapping for audit column)
- [ ] CI job wired in `script-quality.yml` hoặc dedicated workflow
- [ ] WARN mode initially; HARD STOP after 7-day stabilization
- [ ] Documentation in rule update (`docs-folder-structure.md` hoặc new META rule)

## Related

- Audit: `documents/04-quality/audits/ops-readiness/2026-05-25-wave-br-4-ops-readiness-audit.md` §OPS-BR4-002
- Wave br-4 hotfix PRs: #1784 + #1787
- Rule: `release-fix-retry-budget.md` (hotfix pattern detection)
- Sister rule: `local-self-test-before-aws-deploy.md` (local verify mandate)
- Meta context: session handoff `2026-05-24-wave-beta-readiness-4-closure.md` lesson #7
- Wave: planned `wave-meta-1` (Bucket D addition — fit META force-multiplier scope)

## Log

- **2026-05-25 (created):** Filed per Wave audit-1 Ops Readiness audit OPS-BR4-002. Wave meta-1 Bucket D scope (paired GAP-735 test isolation META cleanup).
