# GAP-357: Deprecated exception-ctor migration + IDE warning sweep (kiteclass-core)

**Status:** 🟡 PARTIAL — Phase 1a SHIPPED 2026-05-06 (childprotection + attendance modules, ~26 ctor sites + 5 housekeeping fixes + 20 messages.properties keys en+vi). Phase 1 remaining (~17 other modules, ~150 keys) eligible post-Wave-23 wave-pack.
**Priority:** 🟡 P3 (tech-debt — non-blocking; future-risk when deprecated ctors removed)
**Domain:** Backend / Tech-debt
**Found:** 2026-05-05 (IDE diagnostics during Wave 19 wait window)
**Affects:** 43 source files in `kiteclass-core`; potential compile break on deprecated removal
**Wave eligibility:** ❌ NOT eligible until Wave 19 closes (heavy overlap with Bucket A childprotection module)

## Problem

`kiteclass-core` ships exception classes with NEW MessageSource-integrated constructors (`ValidationException(String errorCode, Object... args)` + `EntityNotFoundException(String errorCode, Object... args)`) but ~43 source files still use the deprecated `(String message)` / `(String, Long)` legacy ctors. IDE flags severity-4 (Info), not blocking compile, but:

- Future deprecated removal will compile-break ~150+ call sites
- Inconsistent error code coverage (some sites use codes already, most use ad-hoc strings → no i18n / no MessageSource resolution)
- Mixed pattern across modules confuses new contributors

Plus minor housekeeping: 1 MapStruct unmapped target + 4 unused imports flagged by LSP.

## Root Cause

`ValidationException.java` lines 28-56 + `EntityNotFoundException.java` lines 31-56 were marked `@Deprecated` when the new error-code ctor was added (`(String errorCode, Object... args)` at line 76) — likely as part of MessageSource integration earlier in 2026. Migration was not landed because each call site needs:

1. An error code string (e.g. `INCIDENT_INVALID_TRANSITION`)
2. Matching key in `messages.properties` + `messages_vi.properties` (currently 345 keys → likely ~150 new keys to add)
3. Replacing call site

This is mechanical but high-volume + cross-module.

## Current State (verified 2026-05-05 per `audit-to-gap-pipeline.md` §2.5 hardened)

| Symbol | Verification command | Match count | Verdict |
|--------|---------------------|-------------|---------|
| Deprecated `ValidationException(String)` ctor | `grep -rln "new ValidationException(\"" kiteclass/kiteclass-core/src/main/java` | 18 source files | ❌ to-be-migrated |
| Deprecated `EntityNotFoundException(String, Long)` / `(String)` ctor | `grep -rln "new EntityNotFoundException(" kiteclass/kiteclass-core/src/main/java` | 25 source files | ❌ to-be-migrated |
| New ctor signature `ValidationException(String, Object...)` | `kiteclass-core/.../exception/ValidationException.java:76` | 1 (target) | ✅ exists |
| New ctor signature `EntityNotFoundException(String, Object...)` | `kiteclass-core/.../exception/EntityNotFoundException.java:76` | 1 (target) | ✅ exists |
| `messages.properties` baseline | `wc -l kiteclass-core/src/main/resources/messages.properties` | 345 keys | ✅ exists |
| `messages_vi.properties` baseline | `ls kiteclass-core/src/main/resources/messages_vi.properties` | 1 file | ✅ exists |

Affected modules (high-level): `attendance`, `assignment`, `childprotection`, `clazz`, `course`, `enrollment`, `grade`, `invoice`, `lms`, `marketing`, `parent`, `payment`, `payroll`, `reportcard`, `storage`, `student`, `teacher`.

## Out-of-scope housekeeping (also flagged by LSP)

- `kiteclass-core/.../clazz/mapper/ClassMapper.java:42` — Unmapped MapStruct target `recurrenceRule`. Fix: `@Mapping(target = "recurrenceRule", ignore = true)` OR add field to target DTO. Decide based on intent (likely ignore).
- 4 unused imports (cosmetic):
  - `kiteclass-core/.../childprotection/service/IncidentServiceTest.java:10` — `BeforeEach`
  - `kiteclass-core/.../clazz/service/ClassRecurrenceServiceTest.java:42` — `anyLong`
  - `kiteclass-core/.../parent/service/ParentTranscriptServiceTest.java:4` — `EntityNotFoundException`
  - `kitehub-subscription/.../NotificationPreferenceServiceTest.java:31` — `eq`

## Proposed Fix (post-Wave-19; do NOT start during active wave)

### Phase 1 — Per-module migration PRs (parallel-eligible after Wave 19)

Split into one PR per top-level module. ~17 modules → ~17 small PRs. Each PR:

1. Identify all deprecated ctor call sites in `<module>/`
2. Define error codes + add to `messages.properties` + `messages_vi.properties`
3. Replace call sites with new ctor
4. Verify mvn green
5. Update business docs `<module>/rules.md` if any error code is user-visible per `business-logic-review.md` §2

**Wave-eligible:** YES once Wave 19 merges. Modules disjoint = parallel-safe in worktree per `feedback_parallel_agent_strategy.md`.

### Phase 2 — Housekeeping single PR

After Phase 1: 1 PR for ClassMapper unmapped property + 4 unused imports.

### Phase 3 — Lock-in (optional, follow-up)

Add ArchUnit test or Checkstyle rule banning `new ValidationException(String)` / `new EntityNotFoundException(String, Long)` to prevent regression. Track separately if useful.

## Acceptance Criteria

- [ ] All 43 source files migrated to new error-code ctor (6/43 done — Phase 1a 2026-05-06: attendance/AttendancePeriodServiceImpl + childprotection/{VettingController, IncidentService, VettingServiceImpl, storage/MinIOVettingDocumentStorageImpl})
- [ ] ~150 new error-code keys added to `messages.properties` + `messages_vi.properties` (20/150 done — Phase 1a 2026-05-06: INCIDENT_* x8 + VETTING_* x12)
- [ ] mvn `./mvnw -pl kiteclass-core clean verify -Dcheckstyle.skip=true` green
- [x] `ClassMapper.java` recurrenceRule mapping resolved — `@Mapping(target = "recurrenceRule", ignore = true)` (Phase 1a — Wave 23 wait window 2026-05-06)
- [x] 4 unused imports removed — IncidentServiceTest (`BeforeEach`), ClassRecurrenceServiceTest (`anyLong`), ParentTranscriptServiceTest (`EntityNotFoundException`), NotificationPreferenceServiceTest (`eq`) (Phase 1a — Wave 23 wait window 2026-05-06)
- [ ] LSP/IDE clean: 0 deprecated-ctor warnings on touched files — Phase 1a clean for attendance + childprotection 5 files; remaining ~17 modules pending
- [ ] Business docs updated for any user-visible error codes (Phase 1 remaining)

## Out-of-scope

- Other deprecated APIs (Spring Boot, Hibernate, etc.) — separate sweep
- Removing the `@Deprecated` ctors themselves — defer until ALL call sites migrated + ≥1 release cycle for safety
- Refactoring `BusinessException` hierarchy — separate scope

## Estimated Effort

- Phase 1: ~3-5 days × 17 modules ÷ 4 parallel agents ≈ 1 wave-pack run (≤90min agent each)
- Phase 2: ~1h
- Phase 3: optional, defer

## Related

- `kiteclass-core/.../common/exception/ValidationException.java` — deprecated source
- `kiteclass-core/.../common/exception/EntityNotFoundException.java` — deprecated source
- `feedback_ide_warnings_check.md` — memory rule "check deprecated APIs before commit"
- `meta-gap-priority.md` — this is Feature P3 (tech-debt), runs AFTER Wave 19 LEGAL P0
- `audit-to-gap-pipeline.md` §2.5 hardened — state-check used to find full scope (43 files vs IDE's 6)

## Log

- **2026-05-06 (Wave 23 wait window — Phase 1a SHIPPED expanded scope)** Coordinator-only PR (no agent) covering attendance + childprotection + 5 housekeeping. User pasted IDE diagnostics for childprotection module + asked "fix toàn bộ trong 1 PR thôi" → expanded scope from initial 3-fix to 31-fix consolidated PR. Migrations: attendance/AttendancePeriodServiceImpl x2 (cast `(Object) id`); childprotection/VettingController x3 (`new Object[0]` no-args); childprotection/IncidentService x8 (full migration with new error codes `INCIDENT_*` + Test assertion updates from `hasMessageContaining("Title")` → `hasMessageContaining("INCIDENT_TITLE_REQUIRED")`); childprotection/VettingServiceImpl x5 (mix of `new Object[0]` + `(Object) id` cast); childprotection/storage/MinIOVettingDocumentStorageImpl x8 (`new Object[0]`). Properties: 20 new keys (INCIDENT_* x8 + VETTING_* x12) added to both messages.properties + messages_vi.properties. Housekeeping (5 fixes from earlier scope): ClassMapper recurrenceRule ignore + 4 unused imports. Verification: 35 + 6 = 41 targeted tests green, mvnw test-compile clean both modules. Status flip 🔵 OPEN → 🟡 PARTIAL per `gap-done-discipline.md` §3 (~17 remaining modules pending Phase 1 wave-pack). Memory `feedback_deprecated_ctor_overload_resolution.md` saved with Java overload-ambiguity gotcha pattern. PR #817 (Phase 2 housekeeping) + PR #820 (initial Phase 1a-attendance) SUBSUMED by this expanded PR — both closed at coordinator merge.
- **2026-05-05** Filed during Wave 19 wait window after IDE diagnostics surfaced ~30 warnings. State-check expanded scope from IDE-flagged 6 files → full 43. Filed instead of fixed because heavy overlap with active Bucket A (childprotection — 4 files, 23 call sites). Per agent-tool guidance "do not duplicate this agent's work — avoid working with the same files." Migration deferred to post-Wave-19 wave-pack.
