# GAP-347: JaCoCo surefire+failsafe coverage merge so SonarCloud sees IT coverage

**Status:** 🟡 PARTIAL — meta fix PR in flight (`meta/jacoco-surefire-failsafe-merge`)
**Priority:** 🟠 MANDATORY (Meta — affects every future PR's Sonar gate)
**Domain:** DevOps / Maven config (`kiteclass/kiteclass-core/pom.xml`)
**Detected:** 2026-05-04 (Wave 18b2 closure — PR #773 Sonar 78.2% gate fail)
**Affects:** Every PR that tests via `*IT` (`@SpringBootTest` + MockMvc) instead of `@WebMvcTest` slice tests

## Problem

PR #773 (Wave 18b2 Bucket C — GAP-321b parent portal facets foundation) shipped 1230/1230 functional tests passing and 100% line + branch coverage on all 9 production classes per local JaCoCo. SonarCloud reported **78.2%** on new code (gate ≥80%) and the PR was admin-merged with the gap as residual debt.

Root cause confirmed: `kiteclass/kiteclass-core/pom.xml` JaCoCo plugin only attaches a single agent to surefire (unit tests). Failsafe (`*IT` integration tests under `@SpringBootTest`) generates no JaCoCo `.exec` data, so any class covered ONLY by `*IT` appears uncovered in the merged report Sonar consumes. PR #773 went heavy on full-stack `@SpringBootTest` MockMvc IT for controllers — Sonar saw those controllers at 10-11% (annotation/wiring instructions only) despite 100% behavioural coverage from IT.

Confirmed against #772 (Agent B vetting) which used `@WebMvcTest` slice tests for controllers (surefire) + passed Sonar 7/7. The split is real: surefire-counted vs failsafe-uncounted. This is a config issue, not a test-quality issue — the IT tests are correct; Sonar simply can't see them.

## Why this is meta-priority

Per `meta-gap-priority.md` §3 — fixing a build/CI config that affects every future PR is force-multiplied. Without this fix:
- Every PR shipping IT-heavy controller coverage will need extra `@WebMvcTest` slice duplication purely to satisfy Sonar
- Sonar threshold becomes a perverse incentive to favour shallow unit tests over realistic full-stack ITs
- Future agents will keep hitting this gate and treating it as a per-PR coverage problem (incorrect framing)

## Proposed Fix

1. Add `prepare-agent-integration` JaCoCo execution bound to `pre-integration-test` phase, writing `target/jacoco-it.exec`
2. Add `merge` JaCoCo execution in `verify` phase combining `target/jacoco.exec` + `target/jacoco-it.exec` → `target/jacoco-merged.exec`
3. Reconfigure `report` execution (or add `report-aggregate`) to read the merged `.exec` and write `target/site/jacoco/jacoco.xml` (Sonar's default lookup path)
4. Verify failsafe is configured and bound to `integration-test` phase. If failsafe is missing (ITs running under surefire by name convention), evaluate option (b) — keep ITs in surefire and ensure both JaCoCo + Sonar see them.

The fix lives in `kiteclass/kiteclass-core/pom.xml` and possibly `.github/workflows/core-ci.yml` (ensure `mvn verify` runs before `sonar:sonar`).

## Acceptance Criteria

- [ ] `kiteclass/kiteclass-core/pom.xml` JaCoCo plugin merges surefire + failsafe `.exec`
- [ ] `./mvnw clean verify` produces `target/site/jacoco/jacoco.xml` containing IT-driven line + branch coverage
- [ ] Pre/post coverage delta documented for ≥2 IT-only-tested classes (e.g. `ParentAttendanceFacetController` should jump from ~10% → ~100%)
- [ ] CI workflow `core-ci.yml` confirms `mvn verify` runs before `sonar:sonar` (likely already does — verify only)
- [ ] No production source changes — config only
- [ ] Sonar on next PR with IT-heavy testing passes ≥80% gate

## Related

- Triggered by: PR #773 (Wave 18b2 Bucket C, GAP-321b foundation) Sonar 78.2% fail
- Sister PR: `meta/jacoco-surefire-failsafe-merge` (in flight)
- Cross-cuts: any gap testing controllers via `*IT` instead of `@WebMvcTest` slice — too many to enumerate
- Wave: Wave 18b2 closure follow-up

## Log

- **2026-05-04** — Filed during Wave 18b2 closure. Solo-dev override on PR #773 with this gap as the systemic-fix follow-up. Meta fix agent dispatched against branch `meta/jacoco-surefire-failsafe-merge`; PR will land separately for review.
