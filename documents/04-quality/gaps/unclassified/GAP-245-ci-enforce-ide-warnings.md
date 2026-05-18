# GAP-245: CI does not enforce IDE warnings (deprecation, unused, raw types)

**Status:** 🟡 PARTIAL — Phase 1 shipped 2026-04-29 (Wave Meta-Gov 2 Cluster 6); Phase 2 (Werror flip-day) tracked in GAP-261
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

## Proposed Fix — Phased

### Phase 1: Maven `strict-warnings` profile + CI activation (THIS PR — DONE)

Add `<profile id="strict-warnings">` to root POMs with `-Xlint:deprecation -Xlint:unchecked -Xlint:rawtypes -Xlint:cast -Xlint:overrides`. CI workflows pass `-P strict-warnings` to mvn compile/test steps so warnings are surfaced prominently in CI logs.

**No `-Werror` in Phase 1** — flipping that flag on existing code would break CI for dozens of pre-existing warnings unrelated to the in-flight PR.

Files touched in Phase 1:
- `kitehub/pom.xml` — added `<profiles><profile id="strict-warnings">` after `<build>`
- `kiteclass/kiteclass-core/pom.xml` — same (no parent POM in kiteclass; per-module declaration)
- `kiteclass/kiteclass-gateway/pom.xml` — same
- `.github/workflows/kitehub-ci.yml` — added `-P strict-warnings` to 6 mvn steps (platform + 5 service tests)
- `.github/workflows/core-ci.yml` — added `-P strict-warnings` to compile + test steps
- `.github/workflows/gateway-ci.yml` — added `-P strict-warnings` to test + compile-warnings step

### Phase 2: Werror flip-day → tracked in GAP-261

Burndown all existing warnings (fix or annotate `@SuppressWarnings(...)` with rationale), then flip the profile to add `-Werror`. Once flipped, a deprecated import or raw generic in a new PR fails CI.

See `documents/04-quality/gaps/GAP-261-werror-flipday.md` for the full burndown procedure.

## Acceptance Criteria

### Phase 1 (this PR — DONE)
- [x] CI profile `strict-warnings` defined in root POMs (`kitehub/pom.xml` + `kiteclass-core/pom.xml` + `kiteclass-gateway/pom.xml`)
- [x] CI workflow steps activate the profile (`-P strict-warnings` on `mvn compile` / `mvn test`) — `kitehub-ci.yml`, `core-ci.yml`, `gateway-ci.yml`
- [x] No `-Werror` accidentally introduced in Phase 1 (verified via grep — only present in code comments documenting Phase 2)
- [x] XML well-formed (validated via Python ElementTree)
- [x] YAML well-formed (validated via Python `yaml.safe_load`)
- [x] Phase 2 follow-up gap filed (GAP-261)

### Phase 2 (GAP-261 — separate PR)
- [ ] Existing warnings inventoried + triaged before flip-day → see GAP-261 §Step 1-2
- [ ] `<arg>-Werror</arg>` added to `strict-warnings` profile → see GAP-261 §Step 3
- [ ] Once flipped, a deprecated import or raw generic in a new PR fails CI → see GAP-261 §Step 4
- [ ] Synthetic regression test confirms CI blocks → see GAP-261 §Step 4

## Related

- **Phase 2 follow-up:** GAP-261-werror-flipday — burndown + `-Werror` flip
- **Memory:** `feedback_ide_warnings_check.md` (manual reminder rule, now reinforced by this gap + GAP-261)
- **PR #605** — example of warnings that slipped past memory rule
- **Memory:** `feedback_incident_to_rule_pipeline.md` §5: incident → rule → enforcement → self-test (this gap is the enforcement step)
- **Rule:** `output-review-mandate.md` — every output (test code, Java source) must have review standard + process; CI lint is the missing process
- **Rule:** `gap-done-discipline.md` §3 — Status stays 🟡 PARTIAL because Phase 2 ACs are mapped to GAP-261, no banned phrases in Log

## Log

- **2026-04-29** — Phase 1 shipped (Wave Meta-Gov 2 Cluster 6 — Agent A). Maven `strict-warnings` profile added to 3 root POMs (`kitehub/pom.xml`, `kiteclass-core/pom.xml`, `kiteclass-gateway/pom.xml`) with `-Xlint:deprecation -Xlint:unchecked -Xlint:rawtypes -Xlint:cast -Xlint:overrides`. CI workflows (`kitehub-ci.yml` 6 mvn steps, `core-ci.yml` 2 steps, `gateway-ci.yml` 2 steps) updated to pass `-P strict-warnings`. No `-Werror` in Phase 1 by design (would break CI on pre-existing warnings). Status flips 🔵 OPEN → 🟡 PARTIAL per `gap-done-discipline.md` §3 — Phase 2 (warnings burndown + `-Werror` flip) mapped to GAP-261-werror-flipday. Verification: XML well-formed via `xml.etree.ElementTree`; YAML well-formed via `yaml.safe_load`; grep confirms `-Werror` only present in code comments (not in compilerArgs).
- **2026-04-28** — Filed after PR #605 closed 8 shipped IDE warnings. State check confirmed no existing CI lint enforcement (workflows + POMs grepped). Memory rule alone is insufficient per `feedback_incident_to_rule_pipeline.md` 5-stage pipeline.
