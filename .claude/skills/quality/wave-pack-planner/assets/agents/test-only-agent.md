# Test-Only Agent Template

**Use when:** Backfill missing tests, mock data, fixtures, snapshot tests after a feature shipped test-light. No production code change.

**Spawn config:** `isolation=worktree`, `subagent_type=general-purpose`
**Branch naming:** `feat/wave-{theme}-gap-{id-slug}-tests`

## Prompt template

```
You are Agent {LETTER} of wave-pack {THEME}. Your scope: {GAP_ID} — {GAP_TITLE}.

## Wave context
Wave plan: documents/03-planning/waves/wave-{DATE}-{THEME}.md
Worktree root: {WORKTREE_ROOT} (you are isolated; do NOT cd to main repo)
Branch: feat/wave-{THEME}-gap-{GAP_ID_SLUG}-tests (already created on your worktree)

## Your task
Read first:
- documents/04-quality/gaps/{GAP_ID}.md (Acceptance Criteria + coverage gap evidence)
- .claude/skills/testing/testing-standards.md (project test conventions)
- {EXISTING_TEST_FILE} (mimic structure)

Backfill the following test layers per the test pyramid:

| Layer | Where | Target |
|-------|-------|--------|
| Unit | {UNIT_TEST_PATHS} | Cover {UNIT_TARGETS — e.g. "5 service methods, error paths"} |
| Integration | {INTEGRATION_TEST_PATHS} | {INTEGRATION_TARGETS — e.g. "@SpringBootTest CRUD round-trip"} |
| E2E | {E2E_TEST_PATHS} | {E2E_TARGETS — only if AC requires; usually skip} |

## Rules
- ZERO production code changes. If you discover a bug while writing tests:
  STOP, write a one-line note in PR body, escalate via {ESCALATION — usually "respond to coordinator, do not fix"}.
- Test paths only: {ALLOWED_PATHS}. Adding fixtures under `src/test/resources/` OK if path stated.
- Spring Boot tests: use `@SpringBootTest` only when integration; prefer `@WebMvcTest` / `@DataJpaTest` slices for speed
- Vitest (frontend): use `describe`/`it` blocks; mock via `vi.mock()`; snapshot tests only for stable UI
- ObjectMapper in tests: use `findAndRegisterModules()` (per `feedback_objectmapper_test_jsr310.md`)
- Test hostnames: use `.invalid` / `.test` / IP literal per `feedback_test_hostnames_rfc2606.md`
- Mock data: realistic, not "foo/bar"; reflect VN context where relevant (tenant names, dates Asia/Ho_Chi_Minh)

## Coverage delta reporting (mandatory in PR body)

Before commits:
- Run baseline: `mvn test -pl {MODULE} -Dtest={EXISTING_TESTS} jacoco:report` (or `pnpm test -- --coverage`)
- Capture line/branch coverage % for {TARGET_CLASSES}

After commits:
- Re-run with new tests
- Report delta: `before X% → after Y% (+Δ)`

If delta <{MIN_DELTA}% → escalate to coordinator (test scope insufficient).

## Deliverable format
After commits, report back:
1. Branch name + commit SHAs
2. Test files added (path list, count)
3. Coverage delta table per class
4. PR URL (`gh pr create --base main --title "test({SCOPE}): backfill {GAP_ID}"`)
5. CI green confirmation (`gh pr checks <PR>` passing)
6. Note: do NOT flip {GAP_ID} Status to DONE — coordinator handles.
```

## Required placeholders

| Placeholder | Example | Notes |
|---|---|---|
| {GAP_ID} | GAP-217 | Single gap |
| {GAP_TITLE} | Backfill RenewService tests | |
| {UNIT_TEST_PATHS} | `kitehub-subscription/src/test/java/.../RenewServiceTest.java` | Concrete paths |
| {UNIT_TARGETS} | "5 happy paths + 3 error paths in `RenewService`" | Measurable |
| {INTEGRATION_TEST_PATHS} | `kitehub-subscription/src/test/java/.../RenewIntegrationTest.java` | Or "N/A" |
| {EXISTING_TEST_FILE} | `kitehub-subscription/src/test/java/.../CreateSubscriptionServiceTest.java` | Pattern to copy |
| {MODULE} | `kitehub/kitehub-subscription` | Maven module path |
| {EXISTING_TESTS} | `*Test` | Surefire pattern |
| {TARGET_CLASSES} | `com.kite.hub.subscription.RenewService` | FQNs |
| {MIN_DELTA} | 20 | Coverage % delta threshold |
| {ALLOWED_PATHS} | `kitehub-subscription/src/test/**` | Restrict scope |
| {ESCALATION} | "open follow-up GAP-XXX, do not fix" | What to do on bug discovery |

## Gotchas

- **Surefire negation pattern includes IT tests** — per `feedback_surefire_negation_pattern.md`, `-Dtest='!Pattern'` matches integration tests too. Prefer explicit list or `*Test` pattern.
- **`@SpringBootTest` slow + brittle** — use slice annotations (`@WebMvcTest`, `@DataJpaTest`, `@JsonTest`) when possible.
- **Mocking RabbitTemplate** — set `spring.rabbitmq.listener.simple.auto-startup=false` in test profile to avoid broker dependency.
- **JPA jsonb columns** — per `feedback_jpa_jsonb_jdbctypecode.md`, `@JdbcTypeCode(SqlTypes.JSON)` required; tests will fail with cryptic VARCHAR error if missing in entity.
- **Snapshot tests churn** — only snapshot stable UI; avoid for components with dates/IDs.
- **Coverage tool config drift** — verify `pom.xml` has jacoco plugin in target module BEFORE running coverage; if missing, escalate (config change = production change, out of scope).
- **Bug discovery temptation** — easy to "just fix it while I'm here." Don't. File a follow-up gap, let coordinator triage. Mixing test-add + bug-fix in 1 PR makes review hard.

## When NOT to use this template

- Gap requires NEW feature code → use `feature-tdd-agent.md` (TDD writes tests + code together)
- Tests need test-infrastructure change (new fixture loader, new test starter) → escalate to feature-tdd
- Gap is "fix flaky test" → that IS a code change to test infra, use `feature-tdd-agent.md`
- E2E-only scope without unit baseline → flag to coordinator first; usually wrong layer

## Reference

- Methodology: [`../../SKILL.md`](../../SKILL.md) Step 3
- Spawn pattern: [`../../reference/agent-spawning-template.md`](../../reference/agent-spawning-template.md)
- Test conventions: `.claude/skills/testing/testing-standards.md`
- Wave closure: [`../../reference/retrospective-checklist.md`](../../reference/retrospective-checklist.md)
