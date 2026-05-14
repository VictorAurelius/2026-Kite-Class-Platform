---
title: Wave 74 Outside-In Benchmark — Hook Coverage Industry Standards
status: complete
created: 2026-05-14
phase: post-wave-74-fix
wave: 74-outside-in
---

# Wave 74 Outside-In Benchmark — Hook Coverage Industry Standards

## Scope

Wave 74 đang ship một `hook-review` skill với 8-point rubric được build inside-out (dev tự nghĩ ra dựa vào kiến thức về 6 hooks của project). Per `.claude/rules/outside-in-coverage-trigger.md` §3 Bước 2, audit này so sánh rubric với industry-standard hook/lint-rule/policy testing patterns từ external projects để identify gaps.

**Project context:** KiteHub/KiteClass ships 6 hooks tại `.claude/hooks/*.py`:
- `audit-gate.py`
- `inject-rule-digest.py`
- `post-tool-guard.py`
- `pre-tool-guard.py`
- `session-lock-guard.py`
- `stop-handoff-check.py`

Tích hợp với Claude Code lifecycle events (PreToolUse / PostToolUse / UserPromptSubmit / Stop) qua `settings.json` matcher pattern.

**Inside-out 8-point rubric Wave 74 đề xuất:**
1. Event matcher correctness
2. BLOCK vs WARN gradient
3. Override trailer recognition (whitespace/case/multi-line)
4. Fail-safe degradation
5. settings.local.json wiring verification
6. False-positive testing (keyword in commit body)
7. Idempotency
8. Performance budget (<500ms)

## Methodology

**Industry projects benchmarked:**

| Tier | Project | Relevance |
|---|---|---|
| 1 (Direct equivalent) | pre-commit framework | Python hook framework, mature conventions |
| 1 | Lefthook | Fast Go-based hook manager |
| 1 | Husky | Node-based git hook standard |
| 2 (Lint rule framework) | ESLint RuleTester | Industry-standard rule unit testing |
| 2 | Semgrep | Rule testing với true/false positive annotations |
| 2 | TypeScript-ESLint RuleTester | Extended ESLint for TypeScript |
| 3 (Policy framework) | OPA/Rego | Policy testing với coverage measurement |
| 3 | Conftest | Policy testing CLI integration |
| Extra | Claude Code Hooks Docs | Official Anthropic specification |

**Search queries used (8):**
- "pre-commit framework testing hooks unit test patterns 2026"
- "ESLint custom rule RuleTester valid invalid test conventions"
- "Semgrep rule testing true positive false positive YAML test fixture"
- "OPA Rego policy testing conventions test-as-code"
- "git hook performance budget benchmark milliseconds lefthook husky"
- "Claude Code hooks settings.json PreToolUse PostToolUse matcher precedence ordering"
- "hook exit code semantics deterministic policy enforcement 0 1 2 convention"
- "mutation testing lint rule policy verify false negative coverage"

**WebFetch budget used:** 7 fetches (within 8-12 target).

---

## Findings per project

### 1. ESLint RuleTester (Tier 2 — gold standard for rule unit testing)

**Key patterns ánh xạ về KiteHub hooks:**

| ESLint pattern | KiteHub hook equivalent |
|---|---|
| `valid` array (code that should pass) + `invalid` array (code that should fail) | True-negative fixtures (no BLOCK expected) + True-positive fixtures (BLOCK expected) |
| `messageId` validation — verify EXACT error identifier matches | Verify hook stderr / blocked message contains exact rule-name token |
| `errors[].line/column/endLine/endColumn` position validation | Verify hook reports CORRECT line in commit body, not just "somewhere matched" |
| `suggestions` testing — different from `output` (autofix) | If hook suggests fix, test the SUGGESTION text separately from the BLOCK action |
| Multi-pass `output: ['v1', 'v2']` array for sequential transformations | Test multi-turn idempotency: hook fires N times, state converges |
| `dependencyConstraints` — skip tests when versions don't match | Test hook skips gracefully when dependency CLI (`gh`, `jq`) not installed |
| Default test file names (`file.ts`, `react.tsx`) | Test hook works regardless of caller's file context |
| `parserOptions` / `languageOptions` per-test | Test hook with different `$CLAUDE_PROJECT_DIR` / `$CLAUDE_EFFORT` values |

**Critical observation:** ESLint mandates **AT LEAST ONE valid + ONE invalid test case** per rule. Wave 74 rubric doesn't make this minimum explicit — it lists "False-positive testing" as Point #6 but doesn't mandate paired true-positive fixtures.

**Gap identified:** Wave 74 rubric implicit; should be **explicit minimum: ≥1 true-positive fixture + ≥1 true-negative fixture per BLOCK condition**.

### 2. Semgrep Rule Testing (Tier 2)

**Annotation system rất valuable:**

| Annotation | Purpose | Hook equivalent |
|---|---|---|
| `# ruleid: <rule-id>` | Mark line where rule MUST fire | Fixture with comment "BLOCKED: <rule-name>" |
| `# ok: <rule-id>` | Mark line where rule MUST NOT fire | Fixture with "ALLOWED: <reason>" |
| `# todoruleid: <rule-id>` | Known false negative (rule misses this; future fix) | Fixture flagged "FUTURE: should block but currently passes" |
| `# todook: <rule-id>` | Known false positive (rule fires here; future fix) | Fixture flagged "FUTURE: should pass but currently blocks" |

**Critical observation:** Semgrep's `todoruleid` / `todook` pattern là **explicit way to track known limitations** that current rule version doesn't catch. Wave 74 rubric không có equivalent — missing this means hook drift goes silently unreported.

**Gap identified:** Need fixture annotation convention for KNOWN false negatives + false positives, mapped to follow-up gaps.

**Autofix testing pattern:** Semgrep uses `.fixed.py` suffix sister-file. Hooks generally don't auto-fix, BUT `inject-rule-digest.py` DOES inject context — testing the INJECTED output is structurally similar.

### 3. OPA/Rego Policy Testing (Tier 3)

**Conventions:**

| OPA pattern | KiteHub hook equivalent |
|---|---|
| `test_<name>` rule naming convention | Test fixture filenames: `test_<hook-name>_<scenario>.<ext>` |
| `_test` package suffix (recommended, not mandatory) | `.claude/hooks/tests/` directory |
| `with input as ...` keyword for mocking input | Mock stdin JSON: `echo '{...}' \| python3 hook.py` |
| `with data.X as ...` for mocking external data | Mock `$CLAUDE_PROJECT_DIR` / git state / file system state |
| `opa test --coverage --format=json` | Measure code coverage of hook Python files |
| `--fail-on-empty` flag | Detect typo in test names (no tests actually ran) |
| `todo_` prefix — auto-skipped tests | Skip known-broken tests explicitly |
| Table-driven tests (multiple cases per rule head reference) | Parameterized fixtures for similar test variations |

**Critical observation 1:** **Coverage measurement.** OPA bakes `--coverage` into the test runner. Wave 74 rubric has zero mention of code coverage for hook implementations.

**Critical observation 2:** **`--fail-on-empty` flag.** Wave 74 rubric doesn't check whether test fixtures actually executed — a typo in test discovery means tests silently pass with zero coverage.

**Critical observation 3:** **Mock support via `with` keyword.** Hook tests need explicit mocking of `$CLAUDE_PROJECT_DIR`, `git status` output, file existence. Wave 74 Point #4 mentions "fail-safe degradation" but doesn't structurally mandate mock injection points.

**Gap identified (4 sub-gaps):** Coverage measurement, fail-on-empty, mock injection, todo_ skip pattern.

### 4. Pre-commit Framework

**Hook config fields (`.pre-commit-hooks.yaml`):**

| Field | Purpose | Wave 74 coverage |
|---|---|---|
| `id` | Unique identifier | Implicit |
| `name` | Display name | Implicit |
| `entry` | Executable command | Implicit |
| `language` | How to install | N/A for Claude hooks |
| `files` | Pattern matching for activation | **Equivalent to matcher pattern — point #1** |
| `exclude` | Exclude pattern | **Gap: no exclude testing** |
| `types` (AND) / `types_or` (OR) | File type filters | **Gap: not tested** |
| `args` | Additional CLI args | Hook scripts have implicit args via stdin |
| `stages` | Which git hook event(s) | Claude event types (PreToolUse, etc.) |
| `always_run` | Run regardless of file matches | **Gap: no testing for always-fire hooks** |
| `pass_filenames` | Whether to pass filenames | Hook stdin JSON contract equivalent |
| `require_serial` | Serial vs parallel | **Critical gap — see below** |
| `fail_fast` | Stop on first failure | **Critical gap — multi-hook ordering** |

**Critical observation:** **Pre-commit explicitly supports `require_serial: true`** for hooks that cannot safely run concurrently. Wave 74 rubric ignores concurrency entirely. With parallel agents (per `feedback_parallel_agent_strategy.md`), KiteHub hooks fire concurrently — **untested race condition territory**.

**Testing pattern:** Pre-commit ships `pre-commit try-repo` — interactive hook development without committing. Wave 74 rubric should mandate equivalent dry-run mode for testing hooks against simulated stdin.

**Gap identified:** Concurrent/parallel execution race conditions; exclude pattern testing; `always_run` semantics; dry-run mode for development.

### 5. Lefthook (Performance benchmark gold standard)

**Performance findings từ benchmark research:**

| Metric | Lefthook | Husky | KiteHub target |
|---|---|---|---|
| Small project pre-commit | 0.9s | 1.2s | Wave 74 says <500ms |
| Large monorepo (100+ packages) | 2.7× faster than Husky | baseline | TBD |
| Cold start (Go binary vs Node.js) | Near-instant | Node.js startup overhead | Python startup ~50-100ms |
| Parallel execution | Native | Manual | Unclear in Claude Code |

**Critical observation 1:** **Lefthook benchmark uses 16-core AMD Ryzen 9 7950X**. Wave 74 Point #8 says "<500ms" but doesn't specify HARDWARE BASELINE. A hook fast on dev workstation might be slow on CI runner.

**Critical observation 2:** **Lefthook supports `lefthook-local.yml`** for env-specific overrides. Wave 74 doesn't structurally test the `settings.json` + `settings.local.json` interaction beyond "wiring verification" (Point #5).

**Critical observation 3:** **`fail_text` field** — custom error message shown when hook fails. Wave 74 rubric ignores message clarity testing.

**Gap identified:** Hardware-baseline pinning for performance, message clarity testing, local vs project config interaction.

### 6. Husky (Node-based)

**Failure mode learned:** Husky 9.0 has Node.js startup overhead — relevant because KiteHub hooks are Python. Python startup is faster than Node but still ~50-100ms cold. Performance budget should account for cold-start tax.

**Gap identified:** Cold-start measurement separate from steady-state.

### 7. Claude Code Hooks Documentation (Official)

**Hard data từ Anthropic docs:**

| Topic | Documented | Wave 74 rubric coverage |
|---|---|---|
| Exit code 0/1/2 semantics | ✅ Clear — **1 is non-blocking** (counterintuitive!) | Point #2 mentions BLOCK vs WARN; doesn't explicitly enumerate exit codes |
| stdin JSON contract | ✅ Complete | Not in rubric |
| stdout JSON output (`continue`, `hookSpecificOutput`, etc.) | ✅ Complete | Not in rubric |
| Environment vars (`$CLAUDE_PROJECT_DIR`, `$CLAUDE_PLUGIN_ROOT`, etc.) | ✅ Complete list | **Gap — Point #4 mentions fail-safe but not these specific vars** |
| Timeout defaults | Command 600s / Prompt 30s / Agent 60s / HTTP 30s | **Gap — Point #8 mentions <500ms but not timeout boundaries** |
| Hook execution order (multi-hook, same event+matcher) | ❌ **NOT DOCUMENTED — CRITICAL GAP** | **Gap — Wave 74 doesn't test ordering** |
| Parallel vs sequential | ⚠️ Ambiguous — "All matching hooks run in parallel" but scope unclear | **Gap** |
| settings.json precedence (`~/.claude/` < `.claude/` < `.claude/settings.local.json` < managed) | ✅ Clear hierarchy | Point #5 covers wiring; not precedence rules |
| Deduplication (command+args, HTTP URL) | ✅ Documented | **Gap** |
| Exec vs shell form (`args` array vs single command string) | ✅ Documented; Windows caveat | **Gap** |
| Hook receives stdin even when not parsed | ✅ Implicit | **Gap — should test hook handles malformed stdin gracefully** |

**Critical observation 1 (highest priority):** Exit code 1 is **NON-BLOCKING** in Claude Code, contrary to Unix convention. This is **the #1 hook bug** per `ranjankumar.in/hooks-policy-as-code-agent-enforcement`: "Every security-critical hook must use exit 2 to actually enforce its gate." Wave 74 Point #2 (BLOCK vs WARN) doesn't make this **counterintuitive exit code semantics** explicit. Hooks that should BLOCK but use `exit 1` will silently fail-open.

**Critical observation 2:** **Hook ordering is undocumented**. With 6 hooks across multiple matchers, KiteHub has untested behavior when 2 hooks match the same `PreToolUse` event for `Bash` matcher.

**Critical observation 3:** **Deduplication by command+args.** Multiple sessions / plugins / settings files can install the SAME hook command. Wave 74 doesn't test what happens when hook is registered twice.

### 8. Mutation Testing (cross-cutting research)

**Concept:** Deliberately introduce bugs into hook code, run test suite, verify tests catch the mutation. "Surviving mutants" = tests that pass when code is broken = false confidence.

**Application to hooks:**
- Mutate `if grep -q "OVERRIDE:" "$file"` → `if grep "OVERRIDE:" "$file"` — does the test catch missing `-q` (case where command output appears)?
- Mutate `exit 2` → `exit 1` — does the test catch the silent fail-open?
- Mutate regex `[A-Z]+_OVERRIDE:` → `[a-z]+_OVERRIDE:` — does the test catch case-sensitivity flip?

Wave 74 rubric (Point #3 mentions override trailer whitespace/case/multi-line) covers some mutations but doesn't formalize **mutation coverage** as a metric.

**Gap identified:** No mutation testing dimension — false-negative tests can themselves be buggy.

---

## Coverage gap analysis — Wave 74 8-point rubric vs industry

| # | Industry pattern | Wave 74 rubric | Gap / observation |
|---|---|---|---|
| 1 | **Exit code 0/1/2 explicit semantics + 1=non-blocking trap** | ⚠️ Partial (Point #2 BLOCK/WARN abstract) | **Sharpen Point #2 to explicit exit code matrix + reject exit 1 for BLOCK intent** |
| 2 | **Minimum ≥1 true-positive + ≥1 true-negative fixture per condition** | ⚠️ Implicit (Point #6 false-positive only) | **Sharpen Point #6: pair with mandatory true-positive minimum** |
| 3 | **Test annotation convention (ruleid / ok / todoruleid / todook)** | ❌ MISS | **NEW point: fixture annotation + known-limitation tracking** |
| 4 | **Coverage measurement (`opa test --coverage`)** | ❌ MISS | **NEW point: branch/line coverage measurement of hook source** |
| 5 | **`--fail-on-empty` — detect tests that didn't actually run** | ❌ MISS | **NEW point: test discovery verification (no silent zero-test runs)** |
| 6 | **Mock injection points (`with input as ...`)** | ⚠️ Partial (Point #4 fail-safe degradation) | **Sharpen Point #4 to enumerate specific mocks needed** |
| 7 | **Hook ordering when multi-hook matches same event** | ❌ MISS | **NEW point: ordering test — fire order documented + tested** |
| 8 | **Concurrent execution race conditions** | ❌ MISS (Point #7 idempotency is single-process) | **NEW point: concurrent fire (N parallel) → state convergence** |
| 9 | **Hardware-baseline pinning for performance budget** | ⚠️ Partial (Point #8 <500ms) | **Sharpen Point #8: specify baseline (CI runner spec, cold vs warm)** |
| 10 | **Cold-start vs steady-state separation** | ❌ MISS | **NEW dimension under Point #8** |
| 11 | **Mutation testing for test suite quality** | ❌ MISS | **NEW point: mutation coverage — at least N=5 mutations attempted** |
| 12 | **`stages` / event-type matrix coverage** | ⚠️ Partial (Point #1 event matcher correctness) | **Sharpen Point #1 to enumerate event types tested** |
| 13 | **`exclude` pattern testing** | ❌ MISS | **NEW point: negative scope testing** |
| 14 | **`always_run` / `pass_filenames` semantics** | ❌ MISS | **NEW dimension under Point #1** |
| 15 | **Environment variable presence/absence (`$CLAUDE_PROJECT_DIR` unset)** | ⚠️ Partial (Point #4) | **Sharpen Point #4 with specific env var enumeration** |
| 16 | **Working directory dependencies** | ❌ MISS | **NEW point: hook called from `pwd != repo root` — does it work?** |
| 17 | **Settings precedence (local > project > user > managed)** | ⚠️ Partial (Point #5) | **Sharpen Point #5: test full precedence chain** |
| 18 | **Deduplication semantics (same command+args)** | ❌ MISS | **NEW point: deduplication behavior verified** |
| 19 | **Exec vs shell form (`args` array vs command string)** | ❌ MISS | **NEW point: invocation form tested both ways** |
| 20 | **Malformed stdin JSON handling** | ❌ MISS | **NEW point: hook handles invalid stdin without crash** |
| 21 | **Timeout boundary (hook exceeds default 600s/30s/60s)** | ❌ MISS | **NEW point: timeout fallback behavior** |
| 22 | **`fail_text` / error message clarity** | ❌ MISS | **NEW point: stderr message readability + reviewer-friendly** |
| 23 | **Hook discoverability (which hooks fire on action X?)** | ❌ MISS | **NEW point: self-doc / dry-run / introspection** |
| 24 | **Test isolation (each test sets up + tears down state)** | ❌ MISS | **NEW point: fixture lifecycle (no test bleeds state)** |
| 25 | **JSON stdout contract compliance (`continue` / `hookSpecificOutput` fields)** | ❌ MISS | **NEW point: stdout JSON validates against schema** |

**Score:** Wave 74 rubric covers ~32% of industry patterns (8 points, ~3 fully + ~5 partial / 25 patterns identified). **17 NEW points** + **8 SHARPEN existing** recommended.

---

## Recommendations

### NEW rubric points to add (HIGH confidence — Bucket D closure absorbs)

These ship with low risk — they extend Wave 74 rubric without contradicting it:

1. **Exit code matrix explicit:** rubric MUST enumerate exit 0 = pass, exit 1 = **non-blocking warn (Unix convention TRAP)**, exit 2 = block. Reject hooks using `exit 1` for BLOCK intent.

2. **Minimum fixture parity:** every BLOCK condition needs ≥1 true-positive fixture + ≥1 true-negative fixture. Mirrors ESLint RuleTester mandate.

3. **stdin malformed JSON handling:** hook must not crash on `echo "" | hook.py` or `echo "{invalid" | hook.py`. Stderr message + exit code defined.

4. **JSON stdout contract compliance:** if hook emits JSON on exit 0, validate against Claude Code documented schema (`continue`, `hookSpecificOutput.hookEventName`, etc.).

5. **Hardware baseline + cold vs warm:** Point #8 "<500ms" extended to specify (a) hardware/runner spec; (b) cold-start measurement separate from warm; (c) p50/p95 distribution not single sample.

### EXISTING rubric points to sharpen (HIGH confidence)

1. **Point #1 (Event matcher correctness):** ENUMERATE event types tested (PreToolUse / PostToolUse / UserPromptSubmit / Stop / SubagentStop / PreCompact). Cross-cell coverage: each hook × each fired event combination tested or explicitly N/A.

2. **Point #2 (BLOCK vs WARN gradient):** map to exit code matrix from new point #1 above. Reject ambiguity ("WARN" must map to exit 0 + stderr OR exit 1 explicitly).

3. **Point #3 (Override trailer recognition):** add SEMGREP-style fixture annotations:
   - `# BLOCKED: <hook-name> — <reason>` (true positive expected)
   - `# ALLOWED: <hook-name> — <reason>` (true negative expected)
   - `# FUTURE_BLOCK: <hook-name>` (known false negative, gap-linked)
   - `# FUTURE_ALLOW: <hook-name>` (known false positive, gap-linked)

4. **Point #4 (Fail-safe degradation):** enumerate SPECIFIC degradation cases:
   - `$CLAUDE_PROJECT_DIR` unset
   - `$CLAUDE_PLUGIN_ROOT` unset
   - `pwd` != repo root
   - `git` / `jq` / `gh` CLI not in PATH
   - `.claude/settings.json` not readable
   - Network unavailable (HTTP hooks)
   - File system read-only

5. **Point #5 (settings.local.json wiring):** EXPAND to full precedence chain test:
   - User-level (`~/.claude/settings.json`) only
   - + Project (`.claude/settings.json`)
   - + Local (`.claude/settings.local.json`) overrides
   - Managed policy (if applicable)
   - Verify final effective config matches expectation at each layer

### Medium-confidence additions (file as Bucket A skill follow-up PR)

6. **Hook ordering test:** when 2+ hooks match same event+matcher, document fire order. Recommend: alphabetical by hook filename (deterministic) OR settings.json declaration order. Test that ordering matches doc.

7. **Concurrent fire (race condition) test:** simulate parallel agents → 5 concurrent fires of same hook → assert end-state converges. Critical for hooks that mutate `.claude/hooks/data/*` state files.

8. **Coverage measurement:** `pytest --cov=.claude/hooks/` or equivalent. Branch coverage target ≥80% per hook.

9. **Mutation coverage:** at least 5 deliberate code mutations per hook; verify test suite catches each.

10. **Deduplication semantics:** install same hook command twice (different settings files) → verify Claude Code dedups and fires once.

11. **Timeout fallback:** hook that sleeps past timeout → verify non-blocking behavior + clean kill.

12. **Working directory independence:** hook called from `/tmp` with no `$CLAUDE_PROJECT_DIR` → fails gracefully or absolute-paths everything.

### Test category structural recommendations (file as Wave 75+ gap)

13. **Test discovery verification:** equivalent to `opa test --fail-on-empty`. If `.claude/hooks/tests/` has zero files matching pattern, CI fails.

14. **Self-doc / introspection:** each hook supports `--help` or `--dry-run` flag returning what it would do for sample stdin.

15. **Fixture lifecycle (setup/teardown):** every fixture creates state in isolated temp dir; teardown removes; no test bleeds.

16. **`exclude` / `always_run` semantic coverage:** if hook has activation pattern, test BOTH activation AND non-activation cases.

17. **Exec vs shell form:** test hook invocation in both `args` array form and single command string form (Claude Code supports both).

### Architecture / structural recommendations (Wave 75+ scope)

18. **Hook framework abstraction:** consider extracting common patterns (stdin JSON parsing, exit code semantics, env var validation) into `.claude/hooks/_common.py` shared module. Reduces per-hook test surface.

19. **Hook registry CSV:** per `meta-csv-index-pattern.md`, create `.claude/hooks/hooks-index.csv` with columns: name / event_type / matcher_pattern / exit_codes_used / blocking_intent / paired_skill. Single source of truth for hook inventory.

20. **Performance regression baseline:** track hook latency per commit; alert if p95 grows >20% from baseline.

21. **Hook contract test (golden file):** capture expected stdin → expected stdout/stderr/exit for each hook into golden files. Regression detection via diff.

---

## Fold-in strategy

| Recommendation # | Confidence | Target | Owner |
|---|---|---|---|
| 1-5 (NEW high-confidence additions) | HIGH | Bucket D closure (this wave) | Coordinator |
| Sharpening 1-5 (extend existing points) | HIGH | Bucket D closure (this wave) | Coordinator |
| 6-12 (medium-confidence additions) | MEDIUM | Bucket A `hook-review` skill follow-up PR | Bucket A author |
| 13-17 (test category structural) | MEDIUM-LOW | Wave 75 follow-up gap | New gap GAP-XXX |
| 18-21 (architecture) | LOW (scope expansion) | Wave 75+ tracked gap | New gap GAP-XXX |

**Risk-managed approach:**
- Bucket D absorbs ONLY items #1-5 + sharpening — they extend rubric without re-architecting
- Bucket A skill PR (after Bucket A merges) extends rubric with items #6-12
- New Wave 75 gap files capture items #13-21 for backlog

---

## Verdict

**Wave 74 rubric covers ~32% of industry patterns** (8 points partially / fully cover ~8 of 25 industry-identified concerns).

**5 high-priority additions surface immediately:**
1. Exit code matrix (counterintuitive `exit 1` trap — security-critical)
2. Minimum true-positive + true-negative fixture parity
3. stdin malformed JSON handling
4. JSON stdout contract validation
5. Hardware-pinned performance baseline (cold vs warm)

**3 critical untested classes Wave 74 missed entirely:**
- **Hook ordering** when multi-hook matches same event (undocumented even by Anthropic — KiteHub at high risk)
- **Concurrent race conditions** with parallel agents (KiteHub uses parallel agents per wave-pack pattern)
- **Coverage measurement** (no current way to know which hook code paths are untested)

**Inside-out vs outside-in pattern confirmation:** this audit demonstrates the value codified in `.claude/rules/outside-in-coverage-trigger.md` — Wave 74's inside-out rubric was useful starting point but missed 17 industry-standard concerns that benchmark research surfaced in 60 minutes of WebSearch + WebFetch budget. Per `meta-gap-priority.md` §3 force-multiplier — investing 1 hour outside-in upfront saves multiple wave retrospectives later.

---

## References

### Industry standards consulted
- ESLint RuleTester: https://eslint.org/docs/latest/extend/custom-rules
- TypeScript-ESLint RuleTester: https://typescript-eslint.io/packages/rule-tester/
- Semgrep test rules: https://semgrep.dev/docs/writing-rules/testing-rules
- OPA Policy Testing: https://www.openpolicyagent.org/docs/policy-testing
- Conftest: https://www.conftest.dev/
- pre-commit framework: https://pre-commit.com/
- Lefthook: https://github.com/evilmartians/lefthook
- Lefthook docs: https://lefthook.dev/
- Husky: https://typicode.github.io/husky/

### Claude Code official
- Claude Code Hooks: https://code.claude.com/docs/en/hooks
- Hook anti-patterns: https://ranjankumar.in/hooks-policy-as-code-agent-enforcement

### Cross-cutting concepts
- Mutation testing: Stryker Mutator (https://stryker-mutator.io/docs/)
- Code coverage vs mutation testing comparison (Codecov blog)

### Related project rules
- `.claude/rules/outside-in-coverage-trigger.md` — codifies this audit's necessity
- `.claude/rules/meta-gap-priority.md` §3 — force-multiplier rationale
- `.claude/rules/rule-change-process.md` §6.5 — Enforcement Parity Mandate (rubric + tests ship same PR)
- `.claude/rules/feedback_parallel_agent_strategy.md` — explains why concurrent race conditions matter
- `.claude/rules/meta-csv-index-pattern.md` — pattern for proposed `hooks-index.csv`

### Cross-wave links
- Wave 73 — Meta Context Optimization (context budget mandate)
- Wave 74 — Hook Coverage (this benchmark closes outside-in gap)
- Wave 75 — proposed follow-up for items #13-21
