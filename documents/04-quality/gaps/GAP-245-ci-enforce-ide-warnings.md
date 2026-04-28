# GAP-245: CI does not enforce IDE warnings (deprecation, unused, raw types)

**Status:** 🔵 OPEN
**Priority:** 🟠 P1 (Meta — process gap; force-multiplier per `meta-gap-priority.md`)
**Domain:** DevOps / CI / Test infrastructure
**Detected:** 2026-04-28 (user-flagged IDE diagnostics surfaced 8 warnings shipped to main across 3 files)
**Affects:** All Java modules (`kiteclass/**`, `kitehub/**`); every PR that touches Java code

## Problem

Memory rule `feedback_ide_warnings_check.md` instructs the AI to verify IDE diagnostics before commit. PR #605 closed 8 such warnings shipped to main (deprecated `@MockBean` ×3, raw `ArgumentCaptor.forClass` ×2, unused `@InjectMocks` ×1, plus stale TODOs that reviewers similarly missed). Memory is a reminder, not enforcement — the check was skipped in multiple sessions before the user manually noticed.

State-check 2026-04-28: no `-Werror`, `-Xlint`, or deprecation-fail-on-warning configuration exists in:
- `.github/workflows/*.yml`
- `kiteclass/pom.xml`, `kitehub/pom.xml` (parent POMs), or any `*-service/pom.xml` child POM

Result: deprecated APIs (Spring Boot 3.4.0+ `@MockBean` deprecated since Q4 2024) and raw type usage cross the merge gate silently.

## Root Cause

Memory + manual reminder is the only layer of defense. Maven compiler plugin defaults are permissive (warnings logged, build green). CI inherits permissive defaults.

## Proposed Fix

Two viable layers — likely both:

### Layer 1: Maven compiler `-Werror` for deprecation + unchecked

Add to parent POMs (or shared compiler-plugin config) a profile activated in CI:

```xml
<plugin>
  <artifactId>maven-compiler-plugin</artifactId>
  <configuration>
    <compilerArgs>
      <arg>-Xlint:deprecation</arg>
      <arg>-Xlint:unchecked</arg>
      <arg>-Xlint:rawtypes</arg>
      <arg>-Werror</arg>  <!-- only when CI profile active -->
    </compilerArgs>
  </configuration>
</plugin>
```

Gate behind a `<profile id="strict-warnings">` so local dev iteration stays unblocked but CI fails on new warnings.

### Layer 2: Burndown of existing warnings before turning Werror on

A flag-day approach risks blocking dozens of unrelated PRs. Steps:
1. Run `mvn compile -Xlint:all` once across all modules; record current warning count
2. Triage: deprecation warnings = fix or document `@SuppressWarnings("deprecation")` with reason; unchecked = fix per Mockito 5 patterns (this PR's example); rawtypes = parameterize
3. Once count is 0 (or pinned with documented suppressions), flip CI profile to `-Werror`

## Acceptance Criteria

- [ ] CI profile `strict-warnings` defined in parent POMs
- [ ] CI workflow steps activate the profile (`-P strict-warnings` on `mvn verify` / `mvn test`)
- [ ] Existing warnings inventoried + triaged before flip-day
- [ ] Once flipped, a deprecated import or raw generic in a new PR fails CI

## Related

- Memory: `feedback_ide_warnings_check.md` (manual reminder rule, now reinforced by this gap)
- PR #605 — example of warnings that slipped past memory rule
- `feedback_incident_to_rule_pipeline.md` §5: incident → rule → enforcement → self-test (this gap is the enforcement step)
- `output-review-mandate.md` — every output (test code, Java source) must have review standard + process; CI lint is the missing process

## Log

- **2026-04-28** — Filed after PR #605 closed 8 shipped IDE warnings. State check confirmed no existing CI lint enforcement (workflows + POMs grepped). Memory rule alone is insufficient per `feedback_incident_to_rule_pipeline.md` 5-stage pipeline.
