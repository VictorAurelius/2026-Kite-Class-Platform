---
title: Wave meta-1 — Test isolation GAP-735 (remove AUDIT_OVERRIDE blocker)
status: draft
created: 2026-05-25
updated: 2026-05-25
wave: 1
tag_primary: meta
tags_secondary: [test-isolation, force-multiplier, gap-735, kiteclass-core]
counter: 1
date_launch: 2026-05-25
waves: [meta-1]
gaps: [GAP-735]
---

# Wave meta-1 — Test isolation GAP-735 (remove AUDIT_OVERRIDE blocker)

**Goal:** Loại bỏ deterministic Testcontainer pollution để mọi code PR kiteclass-core merge non-admin (không cần AUDIT_OVERRIDE trailer). Flip GAP-735 OPEN → DONE 100%.
**Trigger:** Session handoff `2026-05-24-wave-beta-readiness-4-closure.md` §"Wave 2/5" — META P1 force-multiplier; current state mọi PR cần AUDIT_OVERRIDE = friction cao cho future code waves.
**Estimated wall-clock:** ~4-6h Opus 1M (per Sonnet 200k thrash pattern Wave br-4 lesson); 3-bucket parallel ~2-3h longest.

---

## 1. Brainstorm (5-10 min)

**Q1 (alignment — inside-out 4-bucket per `inside-out-completeness-trigger.md` §3):**

- **Inside-out từ session handoff** `2026-05-24-wave-beta-readiness-4-closure.md` §"Wave 2/5": Test isolation GAP-735 — 3-bucket parallel (annotation + fixture isolation + CI workflow update)
- **Inside-out từ queue file** `documents/03-planning/inside-out-queue.md`: confirm GAP-735 trong queue status=queued (consume khi wave launch)
- **Inside-out từ audit:** N/A (meta wave, không trigger outside audit)
- **Outside-in NEW:** SKIP per `outside-in-coverage-trigger.md` §4 row 4 (wave 100% internal scope — test infrastructure refactor, không user-facing change)

Persona phục vụ: Author (solo-dev) + mọi future code PR kiteclass-core (force-multiplier). Domain: test infrastructure trong `kiteclass/kiteclass-core/src/test/`.

**Q2 (trade-offs):**

| Rejected option | Reason |
|---|---|
| Defer Wave meta-2 sau khi ship Wave 3/5 beta-signup | Friction compound — mọi PR Wave 3 cần AUDIT_OVERRIDE, accumulating tech debt |
| @Transactional@Rollback alone (skip fixture isolation) | 3 IT classes có shared state ngoài DB (cache + JVM static) — @Rollback không đủ |
| Add per-test Testcontainer (1 container per test) | Too slow (1-2s startup × 50 tests = ~2 min overhead); fixture cleanup approach phù hợp hơn |
| Skip CI workflow update | Documentation drift — `admin-merge-discipline.md` §2 exception list cần đồng bộ |

**Q3 (risks):**

| Risk | Recovery |
|---|---|
| @Transactional@Rollback breaks JPA cascade test verifications | Bucket A include verify pattern test; nếu fail → use `@DirtiesContext` fallback |
| Test fixture isolation hook conflicts với Spring lifecycle | Bucket B agent test fixture cleanup pattern proven trong Spring docs; verify với `--rerun-tests` |
| Removing AUDIT_OVERRIDE precedent breaks legitimate use cases | Bucket C check current AUDIT_OVERRIDE entries — preserve genuine non-test exceptions |
| 6 deterministic test failures (CourseSecurityTest 4× + EnrollmentIT + InvoiceFlowIT) still flake post-fix | Bucket A + B agents verify ALL 6 PASS với `./mvnw verify -P strict-warnings` 3 consecutive runs |
| Sonnet 200k autocompact thrash (Wave br-4 lesson) | Default Opus 1M cho implementation agents; coordinator coordinate via SendMessage |

---

## 2. Task Breakdown

| Bucket | Gap(s) | Owner | Effort | Disjoint? |
|--------|--------|-------|--------|-----------|
| A | GAP-735 annotation | bg-agent Opus | ~2-3h | ✅ kiteclass-core/src/test/integration/ |
| B | GAP-735 fixture isolation | bg-agent Opus | ~3h | ✅ kiteclass-core/src/test/util/ (new) + JPA listener |
| C | GAP-735 CI + admin-merge-discipline.md update | bg-agent Sonnet | ~1h | ✅ .github/workflows/ + .claude/rules/ |
| Closure | 4-target sync + GAP-735 DONE flip | coordinator inline | ~30-45 min | After A/B verify PASS |

Disjoint check:
- Bucket A modifies @Transactional@Rollback annotations trong 3 IT classes (EnrollmentIntegrationTest + InvoiceFlowIntegrationTest + CourseSecurityTest)
- Bucket B creates new test fixture utility class + JPA EntityManager cleanup hook
- Bucket C modifies workflow YAML + rules markdown
- Read overlap: Bucket A + B đều đọc IT files để hiểu state — read OK, write paths disjoint

---

## 3. Scope

**Stake tier (per `wave-pack-planner/SKILL.md` §Step 4.6):** HIGH → model Opus 4.7 (1M) cho Bucket A+B (test infrastructure thrash risk Wave br-4 precedent); Sonnet OK cho Bucket C (docs/workflow update).
**Cross-layer? (per `wave-pack-planner/SKILL.md` §Step 4.5):** NO — pure BE test infrastructure scope.

| # | Bucket | Gap(s) | Priority | Files (glob) | Spawn order |
|:-:|--------|--------|:--------:|--------------|:-----------:|
| 1 | **A** | GAP-735 @Transactional@Rollback annotation | 🔴 P0 | `kiteclass/kiteclass-core/src/test/java/.../integration/{EnrollmentIntegrationTest,InvoiceFlowIntegrationTest,CourseSecurityTest}.java` | parallel batch 1 |
| 2 | **B** | GAP-735 test fixture isolation | 🔴 P0 | `kiteclass/kiteclass-core/src/test/java/.../util/TestFixtureCleanup.java` (new) + JPA EntityManager listener | parallel batch 1 |
| 3 | **C** | GAP-735 CI + admin-merge-discipline.md update | 🟠 P1 | `.github/workflows/*.yml` + `.claude/rules/admin-merge-discipline.md` §2 exception list | parallel batch 1 |
| 4 | **Closure** | 4-target sync + GAP-735 DONE | 🔴 P0 | gap file + ROADMAP + audits-index + handoff | After A+B verify |

### Bucket A — @Transactional@Rollback annotation

- Files: `kiteclass-core/src/test/java/.../integration/EnrollmentIntegrationTest.java` + `InvoiceFlowIntegrationTest.java` + `CourseSecurityTest.java`
- Pattern: Add `@Transactional` + `@Rollback` annotation to test classes hoặc per-test methods; verify với Spring Boot Testcontainer docs
- Acceptance: All 6 deterministic failures PASS (CourseSecurityTest 4× + EnrollmentIT + InvoiceFlowIT) trong full suite run `./mvnw verify -P strict-warnings` 3 consecutive runs

### Bucket B — Test fixture isolation

- Files: `kiteclass-core/src/test/java/.../util/TestFixtureCleanup.java` (NEW) + JPA EntityManager listener hook
- Pattern: Per-test tenant context cleanup + DB reset hook (truncate test tables OR Liquibase rollback)
- Acceptance: Test fixture utility integrates với Spring TestExecutionListener; isolation verified với cross-test state leak detection

### Bucket C — CI + rule update

- Files: `.github/workflows/core-ci.yml` (verify no AUDIT_OVERRIDE workaround needed) + `.claude/rules/admin-merge-discipline.md` §2 exception list (remove GAP-735 row)
- Pattern: Document GAP-735 closure trong rule Log entry + PATCH bump
- Acceptance: rule v-bump + Log entry + removed GAP-735 từ documented flake list

---

## 4. State-Check Evidence

| Symbol | Type | Verification command | Evidence | Verdict |
|--------|------|----------------------|----------|---------|
| `kiteclass-core/src/test/java/.../integration/EnrollmentIntegrationTest.java` | IT class | `find kiteclass/kiteclass-core/src/test -name "EnrollmentIntegrationTest.java"` | (verify pre-spawn) | ✅ expected to exist |
| `kiteclass-core/src/test/java/.../integration/InvoiceFlowIntegrationTest.java` | IT class | `find kiteclass/kiteclass-core/src/test -name "InvoiceFlowIntegrationTest.java"` | (verify pre-spawn) | ✅ expected to exist |
| `kiteclass-core/src/test/java/.../integration/CourseSecurityTest.java` | IT class | `find kiteclass/kiteclass-core/src/test -name "CourseSecurityTest.java"` | (verify pre-spawn) | ✅ expected to exist |
| `documents/04-quality/gaps/GAP-735*.md` | Gap file | `ls documents/04-quality/gaps/GAP-735*.md` | (verify pre-spawn) | ✅ exists |
| `.claude/rules/admin-merge-discipline.md` §2 GAP-735 row | Exception list entry | `grep "GAP-735" .claude/rules/admin-merge-discipline.md` | (verify pre-spawn) | ✅ exists |
| `kiteclass-core/src/test/java/.../util/TestFixtureCleanup.java` | New fixture utility | (post-spawn) | not yet created | 🆕 to-be-created (Bucket B) |

---

## 5. Verification Gates

| Bucket | Local verify command | CI gate |
|--------|---------------------|---------|
| A | `cd kiteclass/kiteclass-core && ./mvnw verify -P strict-warnings` 3 consecutive runs ALL PASS | core-ci |
| B | `cd kiteclass/kiteclass-core && ./mvnw verify -P strict-warnings -Dtest=*IntegrationTest -Dsurefire.rerunFailingTestsCount=0` | core-ci |
| C | `bash .github/workflows/.../validation` + grep verify GAP-735 row removed from rule | script-quality |
| Closure | `bash scripts/query-gaps.sh GAP-735 DONE phase-1-beta` returns 1 row | None |

---

## 6. Agent Spawn Pattern

3 agents parallel batch 1 + closure inline:

```
Bucket A: subagent_type=general-purpose, model=opus, isolation=worktree, run_in_background=true
Bucket B: subagent_type=general-purpose, model=opus, isolation=worktree, run_in_background=true
Bucket C: subagent_type=general-purpose, model=sonnet, isolation=worktree, run_in_background=true

After A+B verify PASS:
  - Coordinator inline: cherry-pick test fixes + run full suite 3× to confirm zero flake
  - Flip GAP-735 OPEN → DONE
  - Update admin-merge-discipline.md §2 exception list
  - 5-target sync per closure protocol
```

---

## 7. Closure Protocol

1. All 3 buckets SHIPPED + 6 deterministic tests PASS 3 consecutive runs
2. GAP-735 flipped DONE 100% per `gap-done-discipline.md` §2
3. `admin-merge-discipline.md` §2 exception list row "GAP-735" REMOVED + rule v-bump
4. 5-target sync per `post-merge-sync-completeness.md` §2 + session handoff
5. Wave plan status: complete + closed_at: 2026-05-XX
6. Worktree cleanup per `post-wave-cleanup.md`
7. Next wave (Wave 3/5 beta-signup-unblock) becomes non-AUDIT_OVERRIDE eligible

---

## 8. Log

- **2026-05-25 (status: draft):** Wave plan drafted per session handoff `2026-05-24-wave-beta-readiness-4-closure.md` §"Wave 2/5" pickup. Drafted parallel với 3 wave plans khác (Wave 3/5/4/5/5/5) trong combined PR. Counter `meta-1` = first wave với tag_primary=meta (Wave 73 retroactive label per `wave-tag-numbering-convention.md` §5 KHÔNG count). Outside-in audit SKIP per §4 row 4 (internal scope). Author: @nguyenvankiet (solo-dev coordinator).
