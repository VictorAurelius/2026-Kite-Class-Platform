# Hook Edge Case Catalog — known patterns

Living catalog của false-positive / false-negative / divergence patterns đã từng surface trong project. Mỗi entry: **class** + **reproduction** + **detection signal** + **fix pattern** + **cross-ref**.

Reviewer cross-check khi 1+ rubric point ❌ trong `rubric-checklist.md` — entry trùng class → áp dụng fix recipe sẵn có.

Mới phát hiện edge case → append entry mới với date + worked example.

**Version:**
- v1.0 (Wave 74) — 9 entries EC-001 → EC-009 covering original 8 rubric points
- v1.1 (Wave 75 Bucket B) — added 6 entries EC-010 → EC-015 covering new 13-point rubric (Points 9-13) per Wave 74 outside-in benchmark fold-in

---

## EC-001 — Rule text says X, hook checks Y (divergence)

**Rubric point affected:** 3 (override trailer recognition), 6 (false-positive testing)

**Class:** Rule documentation và hook implementation describe slightly different behaviors → hook BLOCK đúng theo code nhưng user vận dụng rule theo doc → confusion.

**Reproduction (Wave 74, 2026-05-14):**

1. `admin-merge-discipline.md` §4 nói:
   > "Trailer applied to the SQUASH commit (so it lands on main, not PR feature branch)."

2. `pre-tool-guard.py` `check_admin_merge()` calls `_commit_body()` = `git log -1 --format=%B` = HEAD commit message body.

3. Pre-merge, HEAD commit là feature-branch commit (vd `feat(wave-74-A): hook-review skill`); squash commit chỉ tồn tại post-merge.

4. User adds trailer trong PR body (UI) hoặc plans to add trong squash commit message. Trigger `gh pr merge --admin` → hook BLOCK vì HEAD commit không có trailer.

**Detection signal:**
- Reviewer thấy rule §Override mention "squash commit" / "PR body" / "merge commit" → cross-check hook code: hook đọc gì?
- Hook test fixture only covers commit-body trailer; không test PR-body case.

**Fix patterns:**

| Fix | Trade-off |
|---|---|
| A. Amend HEAD commit với trailer + force-push, then merge | Quick fix; user must remember |
| B. Update rule §Override to clarify "commit body (HEAD pre-merge OR squash post-merge — wherever you put it, hook reads HEAD)" | Documentation truth; no code change |
| C. Hook reads PR body via `gh pr view --json body` | Adds network dep, +200ms; PostToolUse pattern instead of PreToolUse |
| D. Hook reads multiple commits trong PR branch (git log main..HEAD) | More flexible; some perf cost |

Wave 74 took fix A as immediate workaround; B+C+D deferred per `incident-to-rule-pipeline.md` premature-rule guard.

**Cross-ref:** `admin-merge-discipline.md` §4 + `pre-tool-guard.py` `_has_trailer()` + this skill creation incident.

---

## EC-002 — `--admin` flag triggers BLOCK even on tooling-failure retry

**Rubric point affected:** 2 (BLOCK vs WARN gradient)

**Class:** Hook BLOCK appropriate cho normal flow, nhưng user gặp tooling failure outside their control (CI rate-limited, vendor outage) và cần admin-merge để unstick → BLOCK becomes obstacle without recourse.

**Reproduction (Wave 74, 2026-05-14):**

1. PR #1320 (wave-74 plan doc only) submitted; CI runs.
2. Vercel deploy preview fails với rate-limit error (not code issue).
3. User cần merge để clear queue; `gh pr merge 1320 --squash --admin` → BLOCK by hook.
4. Per `release-fix-retry-budget.md`, tooling-failure retry là exception class; but `admin-merge-discipline.md` chưa map override trailer.

**Detection signal:**
- User reports BLOCK trên admin-merge sau khi đã verify CI failure root cause = tooling not code
- Reviewer check `release-fix-retry-budget.md` exception classes vs `admin-merge-discipline.md` trailer options → mismatch

**Fix patterns:**
- Cross-link two rules: admin-merge §Override accepts trailer `ADMIN_MERGE_OVERRIDE: tooling-failure — <vendor + evidence link>`
- Update hook test fixture với tooling-failure case as positive (allowed with trailer)

**Cross-ref:** `release-fix-retry-budget.md` §5 + `admin-merge-discipline.md` §4

---

## EC-003 — Matcher includes Edit/Write but hook only handles Bash

**Rubric point affected:** 1 (event matcher correctness)

**Class:** `matcher: "Bash|Edit|Write"` causes hook to run on Edit/Write tool calls, but hook's `main()` only handles `tool_name == "Bash"` → Edit/Write tool calls invoke hook (cost ~100ms each) without any rule actually applying.

**Reproduction:**

1. PreToolUse hook wired với `matcher: "Bash|Edit|Write"`.
2. Hook code:
   ```python
   if tool_name == "Bash" and command:
       check_admin_merge(command)
       check_aws_tier3(command)
       # ... 5 rules
   # No Edit/Write handling
   ```
3. Mỗi Edit call → hook runs → no rule fires → exit 0. Cost: ~100ms × N edits/session = real latency.

**Detection signal:**
- Reviewer cross-check matcher regex vs hook code `if tool_name == ...` branches
- If hook handles only subset of matcher → either narrow matcher OR add handlers

**Fix patterns:**

| Fix | When |
|---|---|
| A. Narrow matcher to exact tool types handled (`matcher: "Bash"`) | Hook only checks Bash commands |
| B. Add Edit/Write rule handlers in hook | If new rule (vd `aws-sg-description-ascii`) needs Edit/Write check |
| C. Keep broad matcher + fast-path early exit | If anticipate adding more handlers soon |

Reference: `pre-tool-guard.py` currently uses option C (matcher Bash|Edit|Write; handles Bash for 4 rules + Edit/Write for `check_sg_ascii`).

**Cross-ref:** `pre-tool-guard.py` `main()` + `aws-sg-description-ascii.md` Rule 3 implementation

---

## EC-004 — Trailer regex misses when trailer-value is empty whitespace

**Rubric point affected:** 3 (override trailer recognition)

**Class:** Regex `^{trailer}:\s+\S` requires non-whitespace value after colon+spaces. User writes `ADMIN_MERGE_OVERRIDE:   ` (trailing spaces only) thinking trailer empty = "no specific reason" → hook does NOT recognize trailer → BLOCK still fires. Intended behavior, but user may not realize.

**Reproduction:**

1. User adds commit body trailer `ADMIN_MERGE_OVERRIDE:` (no value).
2. `gh pr merge --admin` → BLOCK; user surprised "I added the trailer!"
3. Hook output cites `ADMIN_MERGE_OVERRIDE: <reason>` format requirement; user missed `<reason>` is mandatory.

**Detection signal:**
- User reports trailer added but BLOCK persists
- Reviewer check commit body via `git log -1 --format=%B` → trailer present but value empty

**Fix patterns:**
- Hook error message MUST explicitly say `<reason>` is required value (current `pre-tool-guard.py` deny message does this for admin-merge ✓)
- Rule §Override section template `<trailer>: <reason — be specific>` placeholder, not just `<trailer>:` (with explanation)

**Cross-ref:** `pre-tool-guard.py` `_has_trailer()` regex `\S` enforcement + rule §Override examples

---

## EC-005 — Hook crash on missing `git` binary (CI sandbox)

**Rubric point affected:** 4 (fail-safe degradation)

**Class:** Hook calls `git log` via `subprocess.run`; in CI sandbox or container without git, `FileNotFoundError`. Without fail-safe wrapper, hook process raises exception → harness treats as BLOCK (defensive).

**Reproduction:**

1. Hook test runs trong CI container without git binary preinstalled.
2. `subprocess.run(["git", "log", ...])` raises `FileNotFoundError`.
3. Without try/except wrapper → unhandled exception → exit 1.
4. Harness interprets non-zero exit as BLOCK → entire test suite fails với misleading reason.

**Detection signal:**
- CI failure with stderr showing `FileNotFoundError: 'git'`
- Hook test PASS locally (git installed) but FAIL in fresh container

**Fix patterns:**
- Wrap subprocess calls in try/except returning empty string default
- Top-level `try/except Exception: print("{}")` fallback (reference `pre-tool-guard.py` main block)
- Test fixture explicitly run hook with `PATH=""` to simulate missing binaries; assert exit 0

**Cross-ref:** `pre-tool-guard.py` `_commit_body()` already implements try/except returning ""

---

## EC-006 — Wiring drift after hook file rename

**Rubric point affected:** 5 (`settings.local.json` wiring verification)

**Class:** Rename hook file (vd `pre-tool-guard.py` → `tool-guard-pre.py`) without updating `settings.local.json` → wiring points to old name → hook silently never runs.

**Reproduction:**

1. PR renames `.claude/hooks/pre-tool-guard.py` → `.claude/hooks/tool-guard-pre.py` for clarity.
2. Update SKILL.md cross-references to new name. Update test file import.
3. Forget `settings.local.json` `command: python3 $CLAUDE_PROJECT_DIR/.claude/hooks/pre-tool-guard.py`.
4. Hook entry references missing file; harness likely silent (`python3` fails with `[Errno 2] No such file or directory` but tool call proceeds).
5. Result: enforcement = 0% but no visible alarm.

**Detection signal:**
- Reviewer grep `settings.local.json` for hook filenames → any reference to non-existent file
- CI gate (deferred): script validates every hook in `settings.local.json` exists; every file in `.claude/hooks/` is referenced (or has explicit `disabled: true` flag)

**Fix patterns:**
- Rename hook PR MUST include `settings.local.json` diff in same commit
- Pre-merge checklist: `bash -c 'for f in .claude/hooks/*.py; do grep -q "$(basename $f)" .claude/settings.local.json || echo "MISS: $f"; done'`

**Cross-ref:** none yet (preventive; documented before incident)

---

## EC-007 — False-positive on banned keyword inside echo / cat / grep

**Rubric point affected:** 6 (false-positive testing)

**Class:** Banned command pattern (vd `aws delete-bucket`) appears inside `echo "..."` or `cat << EOF ... EOF` or `grep -E "aws delete-"` — hook regex matches text without realizing it's quoted string, not command invocation.

**Reproduction (anticipated, not yet incident):**

1. User documenting AWS Tier 3 rule writes: `echo "WARN: aws delete-bucket is BANNED"`
2. Hook `check_aws_tier3()` regex `(?:^|[;&|(\n])\s*(?:...)aws\s+(?:[a-z0-9-]+\s+)?(?:delete-|...)`
3. Anchor `^|[;&|(\n]` requires command boundary. `echo "...aws delete-bucket..."` — the `aws` token is inside quoted argument, but anchor `^` doesn't differentiate quoted vs unquoted.
4. False-positive likely if regex too permissive.

**Detection signal:**
- Test fixture missing case: `echo "...banned-command..."`
- User reports BLOCK on documentation/echo statement

**Fix patterns:**
- Tighten regex to require command syntax (no leading quote): e.g., negative lookbehind `(?<![\"'])` (Python re supports fixed-width)
- Add positive test fixture: `bash -c "echo 'aws delete-bucket is banned'"` → assert allowed

Reference `pre-tool-guard.py` `AWS_TIER3_RE`: currently uses command-boundary anchor (`^|[;&|(\n]`); does NOT include negative-lookbehind for quotes. Acceptable for v1.0 (low false-positive rate in practice); revisit if 2nd incident.

**Cross-ref:** `pre-tool-guard.py` `AWS_TIER3_RE` + `pre-handoff-self-test-completeness.md` (similar pattern coverage)

---

## EC-008 — Hook state file grows unbounded (idempotency violation)

**Rubric point affected:** 7 (idempotency)

**Class:** Hook writes state file on every invocation (vd appending log line "rule X checked at T"); over time file grows to MB+ → next read slow → hook latency degrades → eventually hits performance budget.

**Reproduction (anticipated):**

1. New hook adds debug-style logging: `with open(STATE_DIR/"checks.log", "a") as f: f.write(f"{timestamp} {rule}\n")`.
2. 1000 tool calls/day × 1 line/call × 30 days = 30k lines, ~3MB.
3. Hook latency baseline 50ms → 200ms+ as log read on each invocation.

**Detection signal:**
- `ls -lh .claude/hooks/data/` shows file > 1MB
- Performance regression in hook timing tests

**Fix patterns:**
- State file must be **bounded** in size: rotate (vd keep last N entries) OR overwrite (not append) OR no-write at all
- Reference: `pre-tool-guard.py` `TF_APPLY_STATE` writes single timestamp (overwrite, not append) → bounded ~10 bytes
- If logging needed for debug, use stderr (captured by harness, not project file) OR rotate via `logging.handlers.RotatingFileHandler`

**Cross-ref:** `pre-tool-guard.py` state file pattern (bounded) — anti-pattern would be log accumulation

---

## EC-009 — Hook performance regression from added subprocess call

**Rubric point affected:** 8 (performance budget)

**Class:** Adding new rule check requires `subprocess.run(["gh", ...])` for context; cumulative subprocess calls push hook latency over 500ms budget.

**Reproduction (anticipated based on `concurrent-production-mutation-ops` rule):**

1. `pre-tool-guard.py` Rule 5 calls `gh run list --status in_progress` to check active workflows.
2. `gh` CLI cold-start ~300ms; network call to GitHub API ~200ms; total ~500ms per check.
3. Hook PreToolUse budget 500ms; this single subprocess consumes entire budget.
4. Adding 2nd similar check → exceeds budget.

**Detection signal:**
- `time python3 .claude/hooks/pre-tool-guard.py < fixture` shows real > 0.5s on representative input
- User reports "Claude feels sluggish" after recent hook change

**Fix patterns:**
- **Cache results** with short TTL (vd 10s) — multiple checks in same session reuse fetch
- **Async / lazy** — only call `gh run list` if command actually triggers a workflow (already implemented; early-return if regex doesn't match)
- **Shorter `gh` timeout** with fail-safe allow on timeout (reference: `timeout=4`)
- **Move check to PostToolUse** if BLOCK isn't strictly necessary pre-action

**Cross-ref:** `pre-tool-guard.py` `check_concurrent_mutation` uses fail-safe (return on error) + early-return (regex match gate) → bounded

---

## EC-010 — stdin malformed JSON crashes hook

**Rubric point affected:** 11 (stdin malformed JSON handling test), 4 (fail-safe degradation)

**Class:** Hook reads stdin and calls `json.loads(raw)` without try/except. When Claude Code (or test fixture) feeds malformed JSON, empty string, partial JSON, or non-UTF-8 bytes, hook raises `JSONDecodeError` / `UnicodeDecodeError` → exit non-zero → harness treats as BLOCK → user workflow broken silently.

**Reproduction (anticipated based on Wave 74 outside-in benchmark — Claude Code docs note "hook receives stdin even when not parsed"):**

1. Hook implements: `payload = json.loads(sys.stdin.read())` (no try/except).
2. Test fixture or Claude Code edge case feeds `"{invalid"` or empty string or EOF mid-stream.
3. `json.JSONDecodeError` raised → uncaught → Python exits 1.
4. Harness interprets exit 1 as non-blocking warn (per Point 9 trap) OR as BLOCK depending on event type → either way, hook intent broken.

**Detection signal:**
- CI hook test logs show `json.JSONDecodeError: Expecting value` traceback
- Hook never seems to fire on certain tool calls — investigation reveals stdin format edge case
- New deployment of Claude Code version changes stdin format → hook crashes silently

**Fix patterns:**
- Wrap `json.loads` in try/except returning `{}` (fail-safe per `pre-tool-guard.py` pattern)
- Test fixture suite include 6 stdin variants: malformed / empty / EOF-mid-stream / missing-fields / nested-malformed / very-large
- Catch specifically `json.JSONDecodeError` + `UnicodeDecodeError` + `OSError`; avoid bare `except Exception` if possible

**Cross-ref:** `pre-tool-guard.py` `parse_stdin()` (if implemented per Point 11) + rubric Point 11 §How to verify 6 test commands

---

## EC-011 — `exit 1` trap from un-trapped subprocess

**Rubric point affected:** 9 (exit code matrix + `exit 1` trap callout)

**Class:** Hook author uses `subprocess.run([...], check=True)` which raises `CalledProcessError` on non-zero exit. Without try/except, Python script inherits the non-zero exit code → hook process exits 1 → Claude Code treats as NON-BLOCKING WARN (per documented exit code semantics) → BLOCK intent silently fails open.

**Reproduction (anticipated):**

1. Hook calls `subprocess.run(["git", "log", "-1"], check=True, capture_output=True)`.
2. In CI sandbox or worktree without git initialized, `git log` returns non-zero.
3. `check=True` raises `CalledProcessError`.
4. Uncaught → Python's default exception handler prints traceback to stderr → exits 1.
5. Author intended BLOCK on rule violation but actual exit is 1 (warn, non-blocking) → user proceeds with violating action.

**Detection signal:**
- Hook stderr shows `CalledProcessError: Command ... returned non-zero exit status N`
- Reviewer notices `sys.exit(1)` in BLOCK code path
- Audit: `grep -nE "sys\.exit\(1\)|exit \"?1\"?" .claude/hooks/*.py` finds intent mismatch

**Fix patterns:**
- Use `check=False` and inspect `.returncode` explicitly
- Wrap subprocess calls in try/except handling `CalledProcessError` + `FileNotFoundError` + `TimeoutExpired`
- For BLOCK intent, use `sys.exit(2)` explicitly — never `exit(1)`
- Named constants: `EXIT_PASS = 0; EXIT_WARN = 1; EXIT_BLOCK = 2`

**Cross-ref:** rubric Point 9 §Edge cases + `pre-tool-guard.py` `_commit_body()` try/except pattern (correct)

---

## EC-012 — Cold-start vs steady-state perf drift

**Rubric point affected:** 13 (hardware-pinned performance baseline)

**Class:** Hook PR ships with timing benchmark "~80ms" measured on author's M2 Mac steady-state. Production / CI sees 500ms cold-start. Author has no diagnostic when user complains "Claude feels sluggish" — looks identical to author's local measurement.

**Reproduction (anticipated based on Lefthook benchmark research + Husky/Python cold-start data):**

1. Author runs `time python3 hook.py < fixture` once after some warm-up → 80ms result.
2. PR ships claiming "<500ms target met."
3. CI runs hook for the first time on each fresh runner image → 600ms (Python startup ~100ms + imports ~150ms + first regex compile ~30ms + first subprocess ~250ms + actual work ~70ms).
4. User on slow workstation also sees ~600ms cold-start; complaint surfaces.
5. Author cannot reproduce because their cache is warm.

**Detection signal:**
- User reports latency that author cannot reproduce
- CI hook timing job (if exists) shows variance >2× between p50 and p95
- New environment (fresh container, new dev machine) consistently slow on first hook invocation

**Fix patterns:**
- Document baseline with EXPLICIT hardware + cold-start separation per Point 13
- Use `pytest-benchmark` or equivalent for hot-path timing (multiple iterations, statistical summary)
- Measure both: `for i in {1..10}; do time python3 hook.py < fixture; done` capture distribution
- If cold-start dominates: optimize imports (lazy import expensive deps; skip unused imports)
- Track baseline in `.claude/hooks/data/timing-baseline.json` — committed, updated when significant change

**Cross-ref:** rubric Point 13 + Lefthook/Husky benchmark data + Wave 74 outside-in benchmark Section 5

---

## EC-013 — BLOCK condition tested only positively

**Rubric point affected:** 10 (true-positive + true-negative fixture parity), 2 (BLOCK vs WARN), 6 (false-positive)

**Class:** Hook test suite verifies "hook BLOCKs when input X is provided" (true positive) but lacks paired test "hook ALLOWs when input Y is similar but safe" (true negative). Without negative cases, hook can drift to fail-open without test failure: refactor causes `sys.exit(2)` to be skipped, all positive tests still pass.

**Reproduction (anticipated based on ESLint RuleTester mandate + Semgrep test conventions):**

1. PR adds new rule + hook check + test:
   ```python
   def test_block_admin_merge():
       result = run_hook(stdin='{...gh pr merge --admin...}')
       self.assertEqual(result.returncode, 2)  # ✅ BLOCK
   ```
2. Test passes; PR ships.
3. Later refactor: someone changes `if violation: sys.exit(2)` to `if violation: sys.exit(1)` (mistakenly thinking it's "warning").
4. Test STILL passes because `assertEqual(returncode, 2)` was the original assertion — but new code returns 1 which... wait, that would fail. Let me reframe:
5. Refactor: someone changes early-return logic such that hook never reaches the BLOCK condition for the test input. Test fails.
6. BUT if refactor changes regex slightly such that the test input no longer matches → no BLOCK → test FAILS (caught). However: if author updates the positive test to match new behavior but doesn't add a NEGATIVE test, then hook may now over-block other inputs without any test catching it.

**Detection signal:**
- Test file has many `assertEqual(returncode, 2)` lines but few `assertEqual(returncode, 0)` lines
- Per Point 10 grep: `grep -c "returncode, 2" tests/*.py` >> `grep -c "returncode, 0" tests/*.py`
- Code coverage tool shows BLOCK branch covered but ALLOW branch underexposed

**Fix patterns:**
- Mandate per-rule fixture parity: ≥1 positive + ≥1 negative per BLOCK condition
- Use Semgrep-style annotations (`# ruleid:` / `# ok:`) to make intent explicit
- ESLint RuleTester convention: `tests = { valid: [...], invalid: [...] }`
- Coverage measurement: aim for ≥80% branch coverage per hook (Point 13 bonus)

**Cross-ref:** rubric Point 10 §Implementation pattern + Wave 74 outside-in benchmark Section 1 (ESLint RuleTester)

---

## EC-014 — stdout schema mismatch with Anthropic spec

**Rubric point affected:** 12 (JSON stdout contract schema compliance)

**Class:** Hook author writes `print(json.dumps({"message": "warn text"}))` based on intuition. Anthropic spec requires `{"systemMessage": "warn text"}` (different key) for the message to surface to Claude. Hook output is valid JSON but semantic field name wrong → Claude Code parser silently ignores the intended effect → hook intent lost.

**Reproduction (anticipated based on Wave 74 outside-in vs Claude Code official docs Section 7):**

1. Hook intends to inject context: `print(json.dumps({"context": "Rule X applies: ..."}))`.
2. Anthropic spec requires (for UserPromptSubmit): `{"hookSpecificOutput": {"hookEventName": "UserPromptSubmit", "additionalContext": "..."}}`.
3. Hook stdout is valid JSON, exit 0 → parser doesn't error.
4. But `context` is not a recognized key → parser drops it → Claude never sees the context.
5. Author and reviewer think hook works ("no errors!"); actual injection silently fails.

**Detection signal:**
- Hook intent not surfacing (Claude doesn't reference injected context, doesn't show warn message)
- jq schema validate fails: `jq -e '.hookSpecificOutput.hookEventName' < hook-output.json` returns null
- Claude Code spec changes (vd new fields added) — older hooks may have stale field names

**Fix patterns:**
- Pipe hook stdout through `jq` schema validator in test fixture
- Use dataclasses / Pydantic models to construct output (compile-time schema check)
- Reference Anthropic docs Section 7 keys: `continue` / `hookSpecificOutput.hookEventName` / `permissionDecision` / `permissionDecisionReason` / `systemMessage` / `additionalContext`
- Document hook's stdout shape in module docstring with worked example
- Test against actual Claude Code parser if possible (golden file pattern)

**Cross-ref:** rubric Point 12 §Documented schema + Wave 74 outside-in benchmark Section 7 (Claude Code docs)

---

## EC-015 — Settings precedence override silently disables hook

**Rubric point affected:** 5 (`settings.local.json` wiring verification)

**Class:** Project ships `.claude/settings.json` (committed) wiring critical hook `pre-tool-guard.py`. User adds personal `.claude/settings.local.json` for their preferences (vd custom matcher pattern, disabled flag, or override of hooks array). Per Claude Code documented precedence chain, local settings OVERRIDE project settings → critical enforcement disabled per-user with no team visibility.

**Reproduction (anticipated based on Wave 74 outside-in vs pre-commit / Lefthook precedence patterns):**

1. Project `.claude/settings.json` (committed):
   ```json
   {
     "hooks": {
       "PreToolUse": [{"matcher": "Bash", "hooks": [{"type": "command", "command": "python3 .claude/hooks/pre-tool-guard.py"}]}]
     }
   }
   ```
2. User creates `.claude/settings.local.json` (gitignored) to customize:
   ```json
   {
     "hooks": {
       "PreToolUse": []  // ← user accidentally overrides with empty
     }
   }
   ```
3. Per precedence: `settings.local.json` > `settings.json` → effective hooks for user = empty.
4. User's PreToolUse hooks never fire → admin-merge guard / terraform retry guard / aws Tier3 BLOCK all silent.
5. Team unaware until production incident where rule violation slips through.

**Detection signal:**
- Audit: every PR adding/modifying hooks should list effective wiring at all 4 layers (user / project / local / managed)
- User reports hook "didn't fire on my machine" but works for others — likely local override
- Code review of `.claude/settings.local.json` (when shared in PR description) reveals override

**Fix patterns:**
- Document hook wiring intent in module docstring (so override is intentional, not accidental)
- Reviewer checklist line: "Settings precedence verified — local override absent OR explicitly intended?"
- Per Wave 75 audit recommend listing all hook wiring across layers for any hook PR
- For critical hooks (security, audit), consider using managed policy layer (highest precedence) if org supports
- CI script (deferred) that diffs effective settings vs project settings → flags local-only overrides for review

**Cross-ref:** rubric Point 5 §v1.1 sharpening — Settings precedence chain test + Lefthook `lefthook-local.yml` pattern (similar override mechanism)

---

## Catalog coverage matrix

| Rubric point | Entries with test |
|---|---|
| 1. Event matcher correctness | EC-003 |
| 2. BLOCK vs WARN gradient | EC-002 |
| 3. Override trailer recognition | EC-001, EC-004 |
| 4. Fail-safe degradation | EC-005, EC-010 |
| 5. `settings.local.json` wiring | EC-006, EC-015 |
| 6. False-positive testing | EC-007 |
| 7. Idempotency | EC-008 |
| 8. Performance budget | EC-009 |
| **9. Exit code matrix + exit 1 trap (v1.1)** | EC-011 |
| **10. True-pos + true-neg fixture parity (v1.1)** | EC-013 |
| **11. stdin malformed JSON handling (v1.1)** | EC-010 |
| **12. stdout JSON schema compliance (v1.1)** | EC-014 |
| **13. Hardware-pinned perf baseline (v1.1)** | EC-012 |

All 13 points covered ✅ (per skill v1.1 Wave 75 Bucket B acceptance criteria).

---

## How to add new entry

When new incident surfaces, append entry với schema:

```markdown
## EC-NNN — <one-line summary>

**Rubric point affected:** <number(s)>

**Class:** <general pattern>

**Reproduction (Wave NN, YYYY-MM-DD):**
1. <step>
2. ...

**Detection signal:**
- <reviewer / CI signal>

**Fix patterns:**
- <patterns>

**Cross-ref:** <rule + hook + skill paths>
```

Then update §"Catalog coverage matrix" if new point covered.

---

## Related

- `rubric-checklist.md` — Full 13-point criteria (v1.1); this catalog supports per-point edge case awareness
- `SKILL.md` — Entry point; guides reviewer to this catalog after rubric run
- `.claude/rules/incident-to-rule-pipeline.md` — 5-stage pipeline; each EC entry typically corresponds to a Detect→Classify outcome
- `documents/04-quality/audits/meta/2026-05-14-wave-74-outside-in-benchmark.md` — Wave 74 outside-in benchmark source for v1.1 additions (EC-010 → EC-015)
