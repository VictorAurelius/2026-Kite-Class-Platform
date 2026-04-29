# GAP-261: Werror flip-day — burndown existing IDE warnings then enforce -Werror in CI

**Status:** 🔵 OPEN
**Priority:** 🟡 P2 (Meta — process hardening, force-multiplier)
**Domain:** DevOps / CI / Test infrastructure
**Created:** 2026-04-29 (Wave Meta-Gov 2 Cluster 6 — filed alongside GAP-245 Phase 1 closure)
**Affects:** All Java modules (`kiteclass-core`, `kiteclass-gateway`, `kitehub-*`); every PR that touches Java code post-flip-day

## Problem

GAP-245 Phase 1 (PR Wave Meta-Gov 2 — same wave as this gap) shipped the Maven `strict-warnings` profile that surfaces deprecation, unchecked, raw-type, cast, and overrides warnings prominently in CI compile output. **Phase 1 deliberately does NOT add `-Werror`** — flipping that flag on existing code would break CI for dozens of pre-existing warnings unrelated to the in-flight PR (deprecated `@MockBean` in Spring Boot 3.4+, raw `ArgumentCaptor.forClass` patterns, etc.).

This gap tracks Phase 2: the burndown + flip work needed to reach the original GAP-245 acceptance criterion *"a deprecated import or raw generic in a new PR fails CI"*.

## Root Cause

Memory rule `feedback_ide_warnings_check.md` reminds the AI to check IDE diagnostics before commit, but reminders are not enforcement. PR #605 closed 8 IDE warnings that had silently shipped to main across 3 files. Without `-Werror`, the compiler emits warnings but exits 0, and reviewers/AI commonly miss the noisy compile output.

GAP-245 Phase 1 makes the warnings VISIBLE in CI logs (mandatory profile activation). GAP-261 Phase 2 makes them BLOCKING.

## Proposed Fix — Burndown + Flip

### Step 1: Inventory (per-module)

For each module, run with the new profile and capture warning counts:

```bash
# kiteclass-core
cd kiteclass/kiteclass-core
./mvnw clean compile -P strict-warnings 2>&1 | tee /tmp/core-warnings.log
grep -E "WARNING|warning:" /tmp/core-warnings.log | wc -l

# kiteclass-gateway
cd kiteclass/kiteclass-gateway
./mvnw clean compile -P strict-warnings 2>&1 | tee /tmp/gateway-warnings.log

# kitehub-*
cd kitehub
./mvnw clean compile -P strict-warnings -pl kitehub-platform 2>&1 | tee /tmp/kh-platform.log
./mvnw clean compile -P strict-warnings -pl kitehub-subscription -am 2>&1 | tee /tmp/kh-sub.log
./mvnw clean compile -P strict-warnings -pl kitehub-branding -am 2>&1 | tee /tmp/kh-brand.log
./mvnw clean compile -P strict-warnings -pl kitehub-admin -am 2>&1 | tee /tmp/kh-admin.log
./mvnw clean compile -P strict-warnings -pl kitehub-email -am 2>&1 | tee /tmp/kh-email.log
./mvnw clean compile -P strict-warnings -pl kitehub-gateway -am 2>&1 | tee /tmp/kh-gw.log
```

Record total warnings per module + per category (deprecation / unchecked / rawtypes / cast / overrides) in this gap's "Warning Inventory" section once Step 1 is run.

### Step 2: Triage

For each warning, choose one path:

| Warning category | Common fix | Suppression alternative |
|------------------|-----------|-------------------------|
| Deprecation (e.g. `@MockBean` in Spring Boot 3.4+) | Migrate to replacement (e.g. `@MockitoBean`) | `@SuppressWarnings("deprecation")` on the test class WITH javadoc explaining why migration deferred + tracking link |
| Unchecked (raw generics in mock setup) | Use parameterized type or Mockito 5 `mock(Class<X>)` form | `@SuppressWarnings("unchecked")` on narrowest scope possible |
| Raw types (`ArgumentCaptor.forClass(MyDto.class)` returning raw) | Use `ArgumentCaptor.forClass(MyDto.class)` with explicit type witness | `@SuppressWarnings("rawtypes")` |
| Cast | Use generic methods to avoid cast | `@SuppressWarnings("cast")` |
| Overrides | Add `@Override` annotation | — (almost always fixable) |

**Suppression policy:** every `@SuppressWarnings(...)` MUST include a javadoc comment with rationale. Bare suppressions WITHOUT explanation are a separate sub-issue and tracked by the audit skill.

### Step 3: Flip CI profile to `-Werror`

Once warning count is 0 (or 100% pinned with documented suppressions), add `-Werror` to the strict-warnings profile in:

- `kitehub/pom.xml` (parent — applies to all 6 kitehub modules)
- `kiteclass/kiteclass-core/pom.xml`
- `kiteclass/kiteclass-gateway/pom.xml`

Add `<arg>-Werror</arg>` after `<arg>-Xlint:overrides</arg>` in each module's `strict-warnings` profile.

### Step 4: Verify with synthetic regression

In a feature branch, deliberately introduce a deprecated import (e.g. `@MockBean` if not already migrated) and confirm CI fails on the relevant CI workflow (kitehub-ci, core-ci, or gateway-ci). Then revert before merging.

### Step 5: Update memory + post-merge audit

- Memory entry: `feedback_werror_active.md` — note that CI now blocks IDE warnings in Java compile.
- Post-merge: run security audit + ops-readiness audit per `post-wave-audit-mandate.md` (Java compile chain change).

## Acceptance Criteria

- [ ] Per-module warning inventory captured in this gap (count + category breakdown)
- [ ] All warnings either fixed or pinned with annotated `@SuppressWarnings(...)` (javadoc rationale required)
- [ ] `<arg>-Werror</arg>` added to `strict-warnings` profile in all 3 root POMs
- [ ] Synthetic regression test confirms CI blocks a new deprecated import / raw generic
- [ ] Memory `feedback_werror_active.md` saved
- [ ] CI green on at least one wave-cycle (5 PRs minimum) post-flip — no false-positive blocks

## Risks / Considerations

- **CI churn during burndown** — long-running PRs may need rebase + warning fixes when this lands. Communicate flip-day in advance via wave plan.
- **Suppression abuse** — bare `@SuppressWarnings("unchecked")` without javadoc is the new anti-pattern. Audit skill `quality/business-logic-audit/` or a new `quality/suppression-discipline/` skill (file follow-up gap if abuse pattern emerges) should detect undocumented suppressions.
- **Spring Boot upgrades** — every minor version bump tends to deprecate APIs. After flip-day, Spring Boot upgrade PRs MUST migrate deprecations same-PR (no "deferred to follow-up" — that's the regression we're trying to prevent).

## Related

- **Parent:** GAP-245 — Phase 1 strict-warnings profile shipped same wave (Wave Meta-Gov 2 Cluster 6)
- **Memory:** `feedback_ide_warnings_check.md` — origin reminder rule that GAP-245 + this gap reinforce
- **Memory:** `feedback_incident_to_rule_pipeline.md` §5 — rule + enforcement + self-test pattern; this gap completes the enforcement layer
- **Rule:** `output-review-mandate.md` — Java compile = "Code" output type; CI lint is the missing process
- **Rule:** `gap-done-discipline.md` §3 — GAP-245 stays 🟡 PARTIAL until this gap (GAP-261) lands

## Log

- **2026-04-29** — Filed alongside GAP-245 Phase 1 (Maven `strict-warnings` profile + CI profile activation). Phase 2 = warnings burndown + `-Werror` flip-day. Tracked as P2 because Phase 1 already provides visibility (warnings now appear prominently in CI compile logs); flip-day is hardening, not blocker. Re-check priority when Spring Boot 3.5+ deprecation pressure increases.
