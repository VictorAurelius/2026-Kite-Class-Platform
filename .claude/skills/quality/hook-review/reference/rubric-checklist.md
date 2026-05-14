# Hook Review — 13-Point Rubric (full checklist)

Detailed criteria cho mỗi điểm trong rubric. Reviewer dùng tài liệu này khi check PR thêm/sửa `.claude/hooks/*.py`.

Mỗi point có: **What** (định nghĩa), **Why** (blast radius), **How to verify** (grep / test command), **Edge cases**, **Bonus**.

**Version history:**
- v1.0 (Wave 74) — 8 points: matcher / BLOCK-WARN / override trailer / fail-safe / wiring / false-positive / idempotency / perf budget
- v1.1 (Wave 75 Bucket B, this version) — added 5 points (9-13) + sharpened 5 existing (per Wave 74 outside-in benchmark vs ESLint RuleTester / Semgrep / OPA / pre-commit / Claude Code official docs)

---

## Point 1 — Event matcher correctness

### What
`matcher: "..."` trong `.claude/settings.local.json` quyết định khi nào hook chạy. Regex pattern KHÔNG cover đủ tool calls = enforcement gap; quá rộng = performance + false-positive cost.

### Why
Sai matcher = rule không enforce hoặc enforce sai phạm vi. Vd `matcher: "Bash"` bỏ qua `Edit`/`Write` → rule như `aws-sg-description-ascii` (chỉ trigger trên `.tf` file edits) sẽ miss hoàn toàn.

### How to verify
```bash
# Check current matcher trong settings.local.json
grep -A 3 '"matcher":' .claude/settings.local.json

# Verify regex syntax bằng Python
python3 -c "import re; print(re.compile('Bash|Edit|Write'))"
```

Sau đó cross-check với hook code: hook handle những `tool_name` nào trong `main()`?

### Edge cases

- **Matcher empty `""`** — matches all tool calls; performance cost mọi turn. Chỉ dùng cho hook generic (logging) không phải gate.
- **Tool name case mismatch** — `Bash` (correct) vs `bash` (wrong); harness gửi PascalCase.
- **Tool not in Claude's tool list** — `matcher: "Glob"` valid, nhưng nếu user disable Glob trong allowlist thì hook không chạy.
- **PostToolUse vs PreToolUse mismatch** — admin-merge guard phải là PreToolUse (BLOCK trước khi merge); audit log có thể PostToolUse.

### Bonus
- Hook code có handle `tool_name == ""` (edge case khi harness gửi malformed payload) → fail-safe allow?
- Mọi `tool_name` mà hook code check (vd `if tool_name in ("Edit", "Write"):`) phải xuất hiện trong `matcher` regex; mismatch = hook never runs cho case đó.

### v1.1 sharpening — Anthropic event types enumerated

Hook event types (per Claude Code official docs):

| Event type | When fires | Matcher syntax | Example |
|---|---|---|---|
| `PreToolUse` | Before tool invocation; can BLOCK | `"matcher": "Bash"` hoặc `"matcher": "Bash\|Edit\|Write"` | admin-merge guard, terraform retry guard |
| `PostToolUse` | After tool returns; observe + side-effect | Same matcher syntax | post-tool audit log, drift detection |
| `UserPromptSubmit` | When user submits prompt; can inject context | NO matcher field (fires on every prompt) | `inject-rule-digest.py` injects rule context |
| `Stop` | When Claude session stops; cleanup hook | NO matcher field | `stop-handoff-check.py` session lock release |
| `SubagentStop` | When subagent (Task tool) finishes | Implicit | rare; cleanup parent state |
| `PreCompact` | Before context compaction | Implicit | save state before compact |

**Cross-cell coverage table:** every hook × every event combination tested OR explicitly N/A.

Wave 74 KiteHub hook inventory uses 4 event types: `PreToolUse` (`pre-tool-guard`, `audit-gate`), `PostToolUse` (`post-tool-guard`), `UserPromptSubmit` (`inject-rule-digest`), `Stop` (`stop-handoff-check`, `session-lock-guard`). Verify wiring per event type per hook trong settings.json.

Cross-link: [EC-013](edge-case-catalog.md#ec-013-block-condition-tested-only-positively) for sharpening fixture coverage; [EC-015](edge-case-catalog.md#ec-015-settings-precedence-override-silently-disables-hook) for matcher syntax precedence drift.

---

## Point 2 — BLOCK vs WARN gradient

### What
Hook decision có 3 mức: `{}` (allow), `{"systemMessage": "..."}` (WARN — Claude thấy nhưng action chạy), `{"hookSpecificOutput": {"permissionDecision": "deny", ...}}` (BLOCK — action không chạy).

### Why
BLOCK = irreversible commitment. User phải override trailer hoặc fix root cause. WARN = informational; phù hợp khi rule advisory. Mismatch:
- BLOCK cho advisory case → user frustration, override fatigue
- WARN cho destructive case → rule không có teeth, user proceed

### How to verify
```bash
# Find all _deny() / _warn() / _block() helpers in hook
grep -nE "permissionDecision.*deny|systemMessage" .claude/hooks/*.py
```

Cross-check với rule's §1 The Rule + §Override mechanism. Blast radius tiers:

| Blast radius | Recommended decision |
|---|---|
| Irreversible (data loss, prod resource delete, force-push to main) | BLOCK |
| Shared-state mutation (terraform apply prod, AWS create) | BLOCK |
| Workflow violation reversible (admin merge — can revert PR) | BLOCK với clear override |
| Doc drift / convention violation | WARN |
| Performance suggestion / stylistic | WARN or no hook |

### Edge cases

- **BLOCK without override mechanism** = trap; user phải edit hook source để bypass. Rule + hook ship đồng thời → đảm bảo §Override section của rule có trailer name + hook check trailer đó.
- **WARN cho rule có Priority CRITICAL** = mismatch. CRITICAL rules cần BLOCK; nếu không có teeth, downgrade priority.
- **`systemMessage` không reach user nếu Claude không surface** — WARN hữu ích chủ yếu cho Claude (not user); user-facing alerts cần messaging trên Claude side.

### Bonus
- Rule §1 nói "BLOCK" nhưng hook return `systemMessage` → divergence. Reviewer cite.
- Hook có path branching (vd BLOCK cho `--force`, WARN cho `--no-verify`) → document trong rule + skill catalog.

### v1.1 sharpening — Exit code → decision mapping table

Inheriting from Point 9 (Exit code matrix), the BLOCK/WARN gradient maps DIRECTLY to exit code:

| Intent | Exit code | Stdout | Stderr | Hook event allowed |
|---|---|---|---|---|
| Silent allow (no message) | `0` | empty OR `"{}"` | empty | All |
| Allow with informational message | `0` | `{"systemMessage": "..."}` | (optional log) | All |
| Block (with reason shown to Claude) | `2` | (any JSON) | reason text | PreToolUse, PreCompact |
| Allow but non-blocking warn (Claude sees stderr) | `1` | empty | warn text | PreToolUse (warn), PostToolUse (log) |

**HARD WARNING:** `exit 1` in Claude Code is **NON-BLOCKING** for PreToolUse (per Point 9). If author intends BLOCK and uses `exit 1`, the gate silently fails-open. Reviewer MUST cross-check intent vs exit code.

Cross-link to [EC-013](edge-case-catalog.md#ec-013-block-condition-tested-only-positively): BLOCK condition tested only with positive cases (valid block) but not negative (verify the BLOCK actually fires) = false confidence.

---

## Point 3 — Override trailer recognition

### What
Override trailer là git commit body line như `ADMIN_MERGE_OVERRIDE: <reason>`. Hook regex parse trailer; mismatch = user override không hoạt động → user bị stuck.

### Why
Trailer là user escape-hatch khi rule không cover edge case của họ. Regex sai = trap. Reference: `.claude/hooks/pre-tool-guard.py` `_has_trailer()`:

```python
def _has_trailer(trailer: str, body: str = "") -> bool:
    if not body:
        body = _commit_body()
    return bool(re.search(rf"^{re.escape(trailer)}:\s+\S", body, re.MULTILINE))
```

### How to verify

Test fixture với 6 variant commit bodies:

```python
bodies = [
    "ADMIN_MERGE_OVERRIDE: valid reason",                    # ✅ should match
    "ADMIN_MERGE_OVERRIDE:valid reason",                      # ❌ no space after colon
    "  ADMIN_MERGE_OVERRIDE: valid reason",                   # ❌ leading whitespace
    "admin_merge_override: lowercase",                        # ❌ case mismatch
    "Some text\nADMIN_MERGE_OVERRIDE: multi-line body",      # ✅ should match (re.MULTILINE)
    "ADMIN_MERGE_OVERRIDE: reason\nNext-Trailer: foo",        # ✅ match first line
]
```

Reference regex `^{trailer}:\s+\S`:
- `^` requires line start (no leading whitespace)
- `\s+` requires ≥1 whitespace after colon
- `\S` requires non-whitespace value (rejects `TRAILER: ` empty)

### Edge cases

- **PR body vs commit body** — hook đọc `git log -1 --format=%B` = HEAD commit message body. Trailer trong PR description (GitHub UI) KHÔNG count. Rule §Override phải nói rõ "commit body" (not "PR body").
- **Squash commit not yet exists** — pre-merge, HEAD commit là feature-branch commit; squash commit chỉ tồn tại post-merge. Trailer-trên-squash mandate (như `admin-merge-discipline.md` §4) clash với hook check on HEAD. Resolution: amend HEAD với trailer OR rule chấp nhận trailer trên bất kỳ commit trong PR branch.
- **`re.MULTILINE` essential** — không có flag, `^` chỉ match start of string; trailer ở line 2+ bị miss.
- **Trailer value empty** — `ADMIN_MERGE_OVERRIDE: ` (chỉ space) phải BLOCK (giả-override không có lý do); current regex `\S` requires non-whitespace OK.
- **Trailer in code/log/quote** — git body có thể chứa `\`\`\`<paste log>\`\`\`` với fake trailer text; current regex match anywhere. Reviewer cân nhắc có cần fence-aware parsing không (thường over-engineering cho solo-dev).

### Bonus
- Hook log trailer detected (vd to stderr) để debug → giúp user thấy override actually recognized.
- Multiple trailers cho cùng rule (vd `ADMIN_MERGE_OVERRIDE:` + `ADMIN_MERGE_OVERRIDE_2:` nested) — hook chỉ check first; quarterly retro detect pattern.

### v1.1 sharpening — Semgrep-style fixture annotations

Per Wave 74 outside-in benchmark vs Semgrep test conventions, test fixtures SHOULD use annotation comments để mark expectations:

| Annotation | Purpose | Hook fixture equivalent |
|---|---|---|
| `# ruleid: <rule-name>` | Mark line/case where rule MUST fire (true positive) | Fixture commit body với expected BLOCK |
| `# ok: <rule-name>` | Mark line/case where rule MUST NOT fire (true negative) | Fixture với expected allow |
| `# todoruleid: <rule-name>` | Known false negative (rule misses; future fix tracked) | Fixture flagged FUTURE_BLOCK + gap link |
| `# todook: <rule-name>` | Known false positive (rule fires wrongly; future fix tracked) | Fixture flagged FUTURE_ALLOW + gap link |

Example fixture file `tests/fixtures/admin-merge.py`:

```python
# ruleid: admin-merge-discipline
body_1 = "feat(wave): something\n"  # should BLOCK — no override trailer

# ok: admin-merge-discipline
body_2 = "feat(wave): something\nADMIN_MERGE_OVERRIDE: tooling failure"  # should allow

# todoruleid: admin-merge-discipline
body_3 = "feat(wave): something\nADMIN_MERGE_OVERRIDE:"  # should BLOCK (empty value); current rule allows — track GAP-XXX
```

Cross-link to [EC-013](edge-case-catalog.md#ec-013-block-condition-tested-only-positively): without negative-case parity, test passes when rule is broken.

---

## Point 4 — Fail-safe degradation

### What
Hook crash, missing dependency, malformed input, network timeout, file system error → hook PHẢI exit 0 (allow), không exit 1 hoặc raise exception. BLOCK on dep error = user workflow broken without recourse.

### Why
Hook là gate trên user action; nếu hook bug crash → user không thể làm gì. Fail-safe = "deny rule enforcement when uncertain, allow tool call".

Reference: `.claude/hooks/pre-tool-guard.py` `main()` wrapper:
```python
if __name__ == "__main__":
    try:
        main()
    except SystemExit:
        raise
    except Exception:
        os.write(2, b"pre-tool-guard: internal error, allowing tool call\n")
        print("{}")
        sys.exit(0)
```

### How to verify

```bash
# Test 1: Hook receives malformed JSON
echo "not json" | python3 .claude/hooks/pre-tool-guard.py ; echo "exit=$?"
# Expected: exit=0 + stdout "{}"

# Test 2: Hook receives empty input
echo "" | python3 .claude/hooks/pre-tool-guard.py ; echo "exit=$?"
# Expected: exit=0

# Test 3: Hook with missing optional dep (vd `gh` not installed)
PATH="" echo '{"tool_name":"Bash","tool_input":{"command":"gh pr list"}}' | python3 .claude/hooks/pre-tool-guard.py ; echo "exit=$?"
# Expected: exit=0 (subprocess.run returns non-zero, hook handles)
```

### Edge cases

- **`SystemExit` re-raise** — `_deny()` does `sys.exit(0)`, fail-safe wrapper must NOT swallow SystemExit (else BLOCK turns into allow). Reference code re-raises SystemExit explicitly.
- **`subprocess.run` timeout** — must specify `timeout=N` arg + handle `TimeoutExpired`. Bare `subprocess.run()` can hang indefinitely.
- **`json.loads` on non-JSON** — wrap trong try/except; return empty dict.
- **File I/O on missing file** — use `Path.exists()` check OR `try/except FileNotFoundError`.
- **Network call inside hook** — strongly discouraged (latency + offline failure); use cached state file instead.
- **`contextlib.suppress(Exception)` for state writes** — write may fail but rule check already complete; suppress and continue.

### Bonus
- Hook write to stderr (not stdout) when fail-safe path triggers → reviewer/operator can grep logs for hook crash patterns.
- Hook has explicit `_allow()` helper used uniformly → easier to audit fail-safe coverage.

### v1.1 sharpening — 6 specific degradation cases enumerated

Test fixture suite SHOULD cover these explicit degradation paths (per Wave 74 outside-in vs Claude Code official env-var docs):

| # | Case | Test command | Expected behavior |
|---|---|---|---|
| 1 | Missing CLI dependency (`git`/`gh`/`jq` not in PATH) | `PATH="" echo '{...}' \| python3 hook.py; echo $?` | exit 0 + stdout `"{}"` |
| 2 | Malformed stdin JSON | `echo "not json" \| python3 hook.py; echo $?` | exit 0 (see Point 11 for full coverage) |
| 3 | Subprocess timeout | Inject stub subprocess that sleeps > hook timeout budget | Hook exit 0 + stderr log "timeout, allowing" |
| 4 | Permission denied (read state file) | `chmod 000 .claude/hooks/data/state.txt; echo '{...}' \| hook.py` | exit 0 (cannot persist state ≠ block) |
| 5 | Required env var unset (`CLAUDE_PROJECT_DIR`) | `unset CLAUDE_PROJECT_DIR; echo '{...}' \| hook.py` | exit 0 + fallback to `pwd` |
| 6 | Working directory != repo root | `cd /tmp; python3 /full/path/hook.py < fixture` | exit 0 + absolute-path resolution OR clean fail-safe |

Each case → fixture in `tests/test-<hook>.py::test_failsafe_<case>`.

Cross-link to [EC-005](edge-case-catalog.md#ec-005-hook-crash-on-missing-git-binary-ci-sandbox) (missing git binary worked example) + [EC-011](edge-case-catalog.md#ec-011-exit-1-trap-from-un-trapped-subprocess) (un-trapped subprocess raising exit 1).

---

## Point 5 — `settings.local.json` wiring verification

### What
Hook file in `.claude/hooks/*.py` but NOT referenced in `.claude/settings.local.json` `hooks` section = dead code. Enforcement = 0%.

### Why
Easy to miss when adding new hook. Without wiring, hook never runs; rule's enforcement claim is fiction.

### How to verify

```bash
# List all hook files
ls .claude/hooks/*.py

# Verify each is wired
for hook in .claude/hooks/*.py; do
  name=$(basename "$hook")
  if grep -q "hooks/$name" .claude/settings.local.json; then
    echo "✓ $name wired"
  else
    echo "✗ $name NOT WIRED"
  fi
done
```

### Edge cases

- **`settings.json` vs `settings.local.json`** — project repo có `.claude/settings.json` (committed) AND `.claude/settings.local.json` (gitignored, per-user). Hook wiring có thể nằm ở 1 trong 2; reviewer check cả 2.
- **Matcher includes hook nhưng `command` typo** — vd `python3 $CLAUDE_PROJECT_DIR/.claude/hooks/pre-tool-gard.py` (typo `gard` thay `guard`) → hook never runs, silent failure.
- **Hook wired nhưng `disabled: true`** — JSON schema có cho phép disable flag? Check matcher block cho disabled state.
- **Multiple hooks cùng matcher** — settings.local.json cho phép array; verify order không matter (hooks independent) hoặc nếu matter (vd first hook BLOCK → second không chạy).
- **Stop/UserPromptSubmit hooks không có matcher** — wiring khác PreToolUse/PostToolUse; verify event type đúng.

### Bonus
- New hook PR ship với `settings.local.json` diff in same commit; reviewer xác nhận diff không miss wiring step.
- CI gate (deferred per `incident-to-rule-pipeline.md` premature-rule guard) tự động verify mọi `.claude/hooks/*.py` có wiring.

### v1.1 sharpening — Settings precedence chain test

Claude Code documented precedence (last write wins):

```
managed policy (highest) > .claude/settings.local.json > .claude/settings.json > ~/.claude/settings.json (lowest)
```

Test the full chain để verify effective wiring matches expectation at each layer:

| Layer | File | Scope | Common use |
|---|---|---|---|
| User-level | `~/.claude/settings.json` | All projects, all sessions | Personal preferences (theme, model) |
| Project | `.claude/settings.json` | This repo, all users | Hooks shared across team (committed) |
| Project local | `.claude/settings.local.json` | This repo, this user (gitignored) | Per-user overrides + per-user hook tweaks |
| Managed (org) | OS-specific managed config path | Org-wide policy | Rare in solo-dev mode |

**Drift risk:** if user adds hook in `.claude/settings.local.json` to disable a project hook (vd `"disabled": true` flag), the project's intended enforcement silently dies. Wave 75 audit recommends listing all hook wiring across all layers for any PR adding/modifying hooks.

Cross-link to [EC-015](edge-case-catalog.md#ec-015-settings-precedence-override-silently-disables-hook): settings precedence override silently disables hook.

---

## Point 6 — False-positive testing

### What
Hook regex pattern phải đủ specific để KHÔNG match khi banned keyword xuất hiện trong context vô hại (commit body, PR description, code comment, log paste).

### Why
False-positive = user friction; user mất thời gian figure out tại sao bị BLOCK rồi phải override. Pattern repeated → user disable hook entirely.

Real example: commit body chứa text `"gh pr merge --admin BANNED"` (documentation về rule) — hook KHÔNG được BLOCK vì user không actually run command.

Reference: `pre-tool-guard.py` `ADMIN_MERGE_RE` anchored với `(?:^|[;&|(\n])` = command must start at command boundary (newline / semicolon / pipe / paren), không match arbitrary text.

### How to verify

Test fixtures (negative cases):
```python
# Should ALLOW (false-positive prevention)
benign_cases = [
    'echo "gh pr merge --admin is banned"',           # in echo string
    'cat README.md | grep "gh pr merge --admin"',     # in grep pattern
    '# Comment: gh pr merge --admin disabled',        # in comment
    'history | grep "gh pr merge"',                   # history search
]
for cmd in benign_cases:
    out = run_hook({"tool_name": "Bash", "tool_input": {"command": cmd}})
    assert is_allowed(out), f"False positive on: {cmd}"
```

### Edge cases

- **Banned keyword trong file content (Write/Edit)** — vd write `.md` file documenting banned command; hook không được BLOCK Write tool. Check hook only matches on `tool_name == "Bash"` for command-based rules.
- **Banned keyword in tool_input field other than `command`** — vd `tool_input.file_path` chứa path "admin-merge.md"; hook regex chỉ search `command` field.
- **Shell-quoted command** — `bash -c "gh pr merge --admin"` (nested quote) — phải match. Test variant.
- **`&&` chain** — `git push && gh pr merge --admin` — pattern phải match second command (anchor `[;&|(\n]` covers).
- **Environment prefix** — `DEBUG=1 gh pr merge --admin` — pattern phải match. Reference regex covers via `(?:[A-Z_][A-Z0-9_]*=\S+\s+)*`.
- **Mention trong rule docs / skill files** — `.claude/rules/*.md`, `.claude/skills/**/*.md` chứa banned-command examples. Hook Edit/Write của những file này không được BLOCK (hook check `tool_name == "Bash"` for command-based rules).

### Bonus
- Test suite phải cover ≥3 negative cases (false-positive prevention) per positive case (true-positive). Imbalance → likely under-tested.

---

## Point 7 — Idempotency

### What
Same input → same output mỗi lần. Không log spam (write log mỗi invocation), không state mutation ngoài intent, không side-effect cross-invocation.

### Why
Hook chạy mỗi tool call (potentially hundreds/session); non-idempotent = log file bloat / state corruption / unpredictable BLOCK behavior.

Reference: `pre-tool-guard.py` `check_terraform_retry()` writes state file mỗi `terraform apply` call (legitimate state for retry detection). Đây là **intentional** state mutation; idempotent vì cùng input → cùng state transition.

### How to verify

```bash
# Run hook same input 3 times; verify identical output
INPUT='{"tool_name":"Bash","tool_input":{"command":"ls"}}'
for i in 1 2 3; do
  echo "$INPUT" | python3 .claude/hooks/pre-tool-guard.py
done
# Expected: identical output 3 times

# Verify no unexpected side-effect file created
ls .claude/hooks/data/ # before
echo "$INPUT" | python3 .claude/hooks/pre-tool-guard.py
ls .claude/hooks/data/ # after — should be unchanged for `ls` command
```

### Edge cases

- **State file write per invocation** — OK nếu state intent (retry timestamps, last-run ID); NOT OK nếu append-only log mỗi invocation (file grows unbounded).
- **Random sampling / fuzzing** — hook decision based trên `random.random() < 0.1` = non-idempotent; reject pattern entirely (hook must be deterministic).
- **Time-based check** — `time.time() - last_ts < N` = depends on wall clock; same input at T+0 vs T+1 different decision. Acceptable nếu intent (retry window) nhưng test với mocked clock.
- **Network call cached vs uncached** — first call hits network, subsequent uses cache; idempotent nếu cache stable, non-idempotent nếu cache TTL.
- **Subprocess output parsing** — `gh run list` output có thể vary theo concurrent workflow state; hook decision phụ thuộc → test với mock `gh` output.

### Bonus
- Hook explicitly document state files in module docstring (vd `STATE_DIR = ...`); easy to audit side-effect surface.
- Test suite include "run hook 100× check state file size bounded" sanity check.

---

## Point 8 — Performance budget

### What
PreToolUse hook < 500ms (blocks user action). PostToolUse < 1s (delays next prompt). UserPromptSubmit < 500ms (blocks user input).

### Why
Hook latency = user perceived latency. >500ms PreToolUse = sluggish; >2s = user thinks Claude stuck.

### How to verify

```bash
# Time hook on representative input
time echo '{"tool_name":"Bash","tool_input":{"command":"ls"}}' | python3 .claude/hooks/pre-tool-guard.py
# Expected: real < 0.5s

# Time hook on worst-case input (triggers all checks + subprocess calls)
time echo '{"tool_name":"Bash","tool_input":{"command":"gh workflow run terraform-apply.yml"}}' | python3 .claude/hooks/pre-tool-guard.py
# Expected: real < 0.5s (concurrent-mutation check spawns `gh run list` subprocess)
```

### Edge cases

- **Subprocess timeout** — `subprocess.run(..., timeout=N)` — `N` must be < remaining hook budget. Reference uses `timeout=4` cho `gh run list`; with 10s hook timeout → 6s margin for other work.
- **Cold-start Python** — `python3` startup ~50-100ms. Hook PreToolUse có 400-450ms effective budget cho work.
- **Regex compilation** — `re.compile()` cached at module load = free per-call. Inline `re.search(pattern, string)` = compiles every call; OK cho small patterns, costly cho complex multi-line regex.
- **File I/O** — reading state file ~1ms; reading large file (git log full history) = expensive. Use `git log -1` not `git log`.
- **`git` subprocess** — `git log -1 --format=%B` ~10-30ms; multiple git calls compound.
- **Network call (`gh run list`)** — 200-500ms typical; can spike >1s on slow network. Set short timeout + fail-safe allow on timeout.

### Bonus
- CI step time hooks against fixture inputs; FAIL on regression >50% baseline.
- Hook profile data persisted to `.claude/hooks/data/*-timing.log` (sampled, not every invocation, để avoid violating idempotency point 7).

---

## Point 9 — Exit code matrix + `exit 1` trap callout

### What
Claude Code hook execution model có 3 exit code semantics, NHƯNG semantics khác với Unix convention. `exit 1` trong Claude Code là **NON-BLOCKING WARN**, KHÔNG phải fail — counterintuitive với Unix convention nơi exit 1 thường nghĩa là error/fail.

### Why
**Đây là #1 hook bug class** per `ranjankumar.in/hooks-policy-as-code-agent-enforcement`: "Every security-critical hook must use exit 2 to actually enforce its gate." Hook author intends BLOCK nhưng dùng `exit 1` (theo Unix instinct) → gate silently fails-open → enforcement = 0%.

### Exit code matrix

| Exit code | Claude Code semantics | When to use | Visibility |
|---|---|---|---|
| `0` | Pass / silent allow (default) | Hook ran, no objection | stdout JSON optional |
| `1` | **NON-BLOCKING warn** (Unix convention TRAP!) | Hook saw issue but allows tool call; stderr surfaces to Claude | stderr text |
| `2` | BLOCK — tool call rejected | Hook actively denies; reason in stderr | stderr text + stdout JSON |
| `≥3` | Implementation-defined / treated like exit 1 | Generally avoid | Rarely meaningful |

### How to verify

```bash
# Verify each BLOCK condition in hook → exit 2 (not exit 1)
grep -nE "sys\.exit\([12]\)|exit \"?[12]\"?" .claude/hooks/*.py

# Common bug: `if violation: sys.exit(1)` — should be exit(2)
```

Cross-check: hook's BLOCK message in stderr SHOULD be paired with `sys.exit(2)`, not `sys.exit(1)`. Reviewer reads each branch of hook to verify intent matches exit code.

### Edge cases

- **Un-trapped subprocess `exit 1`** — if hook calls `subprocess.run(["git", "..."], check=True)` and git returns non-zero, `CalledProcessError` raised. Without try/except, hook process inherits exit 1 silently. Fix: wrap with try/except OR explicit exit code handling.
- **`sys.exit()` without arg** — defaults to exit 0, which is SAFE but may obscure intent. Always specify code explicitly.
- **`raise SystemExit(N)`** — equivalent to `sys.exit(N)`; check both syntaxes when grepping.
- **`return 1` from `main()` then `sys.exit(main())`** — propagates correctly but easy to miss in code review.
- **Conditional exit code based on tool result** — `sys.exit(0 if ok else 2)` works; just verify both branches tested.

### Bonus
- Test fixture explicitly asserts exit code via `subprocess.run(...).returncode == 2` for BLOCK cases.
- Hook code uses named constants `EXIT_PASS = 0; EXIT_WARN = 1; EXIT_BLOCK = 2` for clarity.

Cross-link to [EC-011](edge-case-catalog.md#ec-011-exit-1-trap-from-un-trapped-subprocess).

---

## Point 10 — True-positive + true-negative fixture parity per BLOCK condition

### What
Per ESLint RuleTester mandate (gold standard for rule unit testing), every rule MUST have AT LEAST ONE `valid` test case (should pass) AND ONE `invalid` test case (should fail). One-sided testing produces false confidence.

### Why
Test suite with only positive cases (verify hook DOES block when it should) misses the inverse failure mode: hook silently allows things it should block (false negatives). Without paired negative cases, hook can drift to fail-open after refactor without test failure.

Real example: ESLint requires `tests = { valid: [...], invalid: [...] }`. Semgrep requires both `# ruleid:` (positive) and `# ok:` (negative) annotated fixtures. KiteHub hook tests SHOULD mirror.

### How to verify

```bash
# Count valid + invalid fixture pairs per rule
grep -nE "# ruleid:|# ok:" .claude/hooks/tests/fixtures/*.py | sort | uniq -c
```

Expected: each `ruleid: <rule>` annotation paired with at least 1 `ok: <rule>` annotation. Imbalance → likely under-tested.

### Implementation pattern

`.claude/hooks/tests/test-pre-tool-guard.py` structure:

```python
class TestAdminMerge(unittest.TestCase):
    # True positive: should BLOCK
    def test_block_no_override_trailer(self):
        result = run_hook_with(stdin='{"tool_name":"Bash","tool_input":{"command":"gh pr merge --admin"}}',
                              git_log_body="feat(wave): something\n")
        self.assertEqual(result.returncode, 2)  # BLOCK
        self.assertIn("ADMIN_MERGE_OVERRIDE", result.stderr)

    # True negative: should ALLOW with valid override
    def test_allow_with_override_trailer(self):
        result = run_hook_with(stdin='{"tool_name":"Bash","tool_input":{"command":"gh pr merge --admin"}}',
                              git_log_body="feat(wave): something\nADMIN_MERGE_OVERRIDE: tooling failure\n")
        self.assertEqual(result.returncode, 0)  # ALLOW

    # True negative: unrelated command should ALLOW
    def test_allow_normal_merge(self):
        result = run_hook_with(stdin='{"tool_name":"Bash","tool_input":{"command":"gh pr merge"}}',
                              git_log_body="feat(wave): something\n")
        self.assertEqual(result.returncode, 0)  # ALLOW (no --admin flag)
```

### Edge cases

- **Only true-positive tests** → cannot catch fail-open regression where hook stops firing
- **Only true-negative tests** → cannot catch hook over-firing on benign cases
- **One pair total** → minimum, but ideally ≥3 negatives per positive (cover variant benign cases)
- **Parameterized tests** — pytest fixtures with table-driven cases reduce boilerplate
- **Shared fixture setup** — extract common setup (mock git, mock stdin) into helper functions

### Bonus
- Test suite include ≥3 negative cases per BLOCK condition (mirrors §6 ratio).
- Coverage measurement (e.g., `pytest --cov=.claude/hooks/`) reports both branches of BLOCK condition tested.

Cross-link to [EC-013](edge-case-catalog.md#ec-013-block-condition-tested-only-positively).

---

## Point 11 — stdin malformed JSON handling test

### What
Claude Code feeds JSON to hook stdin. If stdin contains invalid JSON, empty string, EOF, or partial JSON, hook MUST fail-safe (silent allow, exit 0). Hook crash on stdin parse error → harness treats as BLOCK (defensive) → user workflow broken.

### Why
Sub-class of Point 4 (fail-safe) but specifically about stdin parsing — the FIRST thing every hook does. Bug here = hook never runs correctly → enforcement = 0% silently.

### How to verify

```bash
# Test 1: malformed JSON
echo "{invalid" | python3 .claude/hooks/pre-tool-guard.py; echo "exit=$?"
# Expected: exit=0 + stdout "{}"

# Test 2: empty stdin
echo "" | python3 .claude/hooks/pre-tool-guard.py; echo "exit=$?"
# Expected: exit=0

# Test 3: EOF before complete JSON
printf '{"tool_' | python3 .claude/hooks/pre-tool-guard.py; echo "exit=$?"
# Expected: exit=0

# Test 4: valid JSON but missing expected fields
echo '{}' | python3 .claude/hooks/pre-tool-guard.py; echo "exit=$?"
# Expected: exit=0 (treat as no tool call to inspect)

# Test 5: deeply nested malformed JSON
echo '{"tool_input": {"command": "test", "nested": "{invalid"}}' | python3 .claude/hooks/pre-tool-guard.py; echo "exit=$?"
# Expected: exit=0 (outer JSON valid; nested string is just data)

# Test 6: very large stdin (DoS attempt)
yes "x" | head -c 10000000 | python3 .claude/hooks/pre-tool-guard.py; echo "exit=$?"
# Expected: exit=0 + bounded memory (hook shouldn't OOM)
```

### Implementation pattern

```python
def parse_stdin():
    try:
        raw = sys.stdin.read()
        if not raw.strip():
            return {}
        return json.loads(raw)
    except (json.JSONDecodeError, OSError):
        return {}  # fail-safe: empty dict → no rules fire

payload = parse_stdin()
tool_name = payload.get("tool_name", "")
```

### Edge cases

- **`json.loads` raises `json.JSONDecodeError`** — catch this specifically; not bare `Exception`
- **stdin pipe broken** — `OSError` / `IOError`; treat same as empty
- **stdin contains BOM** — strip via `raw.lstrip("﻿")` if needed
- **stdin in non-UTF8 encoding** — `UnicodeDecodeError`; treat as malformed → exit 0
- **Concurrent stdin reads** — not relevant for single-process hooks but worth noting

### Bonus
- Hook stderr logs malformed-stdin event for debugging without blocking.
- Test fixture includes `null` stdin case (`echo "null"` valid JSON parses to Python `None`).

Cross-link to [EC-010](edge-case-catalog.md#ec-010-stdin-malformed-json-crashes-hook).

---

## Point 12 — JSON stdout contract schema compliance

### What
When hook emits JSON on stdout (typically for `systemMessage` injection or `hookSpecificOutput` extension), the output MUST conform to Anthropic's documented schema. Non-compliant output → Claude Code may ignore the hook's intended effect silently.

### Why
Spec drift between hook author's mental model and actual Claude Code parser = invisible bugs. Hook returns `{"message": "..."}` but spec requires `{"systemMessage": "..."}` → message never surfaces.

### Documented schema (per Claude Code official docs)

PreToolUse hook JSON stdout (when allowing):

```json
{
  "continue": true,
  "hookSpecificOutput": {
    "hookEventName": "PreToolUse",
    "permissionDecision": "allow"
  },
  "systemMessage": "<optional informational message Claude sees>"
}
```

PreToolUse hook BLOCK:

```json
{
  "continue": false,
  "hookSpecificOutput": {
    "hookEventName": "PreToolUse",
    "permissionDecision": "deny",
    "permissionDecisionReason": "<reason>"
  }
}
```

UserPromptSubmit injection:

```json
{
  "hookSpecificOutput": {
    "hookEventName": "UserPromptSubmit",
    "additionalContext": "<context to inject before user prompt>"
  }
}
```

### How to verify

```bash
# Validate hook stdout against jq schema check
echo '{"tool_name":"Bash","tool_input":{"command":"ls"}}' \
  | python3 .claude/hooks/pre-tool-guard.py \
  | jq -e 'has("continue") or has("hookSpecificOutput") or . == {}' > /dev/null
echo "schema-conformant=$?"

# Verify required nested keys present when hookSpecificOutput exists
echo '...' | python3 hook.py \
  | jq -e '.hookSpecificOutput | has("hookEventName") and (has("permissionDecision") or has("additionalContext"))' > /dev/null
```

### Edge cases

- **Empty stdout `""` or `"{}"`** — both valid (default = allow, no extra effect)
- **Trailing newline / whitespace** — stripped by parser; OK
- **Non-JSON stdout (vd plain text "ok")** — Claude Code likely treats as empty + emits parse warning; hook intent lost
- **Mixed JSON + non-JSON** (vd `{"continue": true} extra text`) — parser strict; likely fails
- **Missing `hookEventName` inside `hookSpecificOutput`** — Anthropic spec requires this key
- **Wrong `hookEventName` value** (vd `"PreTool"` instead of `"PreToolUse"`) — silently ignored
- **Stale field names** (vd legacy `"approve": true` from older spec) — drift; verify against current docs

### Bonus
- Test fixture pipes hook output through `jq` schema validator.
- Hook uses dataclasses / Pydantic models to construct output (compile-time schema check).
- Document hook's stdout JSON shape in module docstring with worked example.

Cross-link to [EC-014](edge-case-catalog.md#ec-014-stdout-schema-mismatch-with-anthropic-spec).

---

## Point 13 — Hardware-pinned performance baseline (cold-start vs steady-state)

### What
Point 8 says "<500ms" but doesn't pin hardware/runner/measurement methodology. A hook fast on a workstation may be slow on CI runner. Cold-start (first invocation, JIT/imports) vs steady-state (Nth invocation, cached) can differ by 10×.

### Why
"<500ms" without baseline → drift over time as deps grow. Author tests on M2 Mac (50ms hook), CI runs on x86 GitHub runner (500ms hook), production runs slow → user latency complaint with no diagnostic trail.

### Cold-start vs steady-state breakdown

| Phase | Typical cost | Source |
|---|---|---|
| Python interpreter startup | 50-100ms | OS + Python bootstrapping |
| Import overhead | 20-200ms | Imports of `subprocess`, `json`, `re`, custom modules |
| First regex compile | 5-20ms per pattern | `re.compile` JIT |
| First file I/O | 10-50ms | OS file cache miss |
| First subprocess call | 100-500ms | Spawn + setup git/gh CLI process |
| **Cold-start total** | **~200ms-1s** | First invocation per session |
| Cached regex match | <1ms per pattern | `re.compile` cached |
| Cached file I/O | 1-5ms | File in OS page cache |
| Cached subprocess | 50-200ms | Process spawn still costs |
| **Steady-state total** | **~50-300ms** | Nth invocation per session |

### Required documentation

Hook PR description SHOULD include:

```
## Performance baseline

Hardware: <dev machine OR CI runner spec>
Measurement: `time python3 hook.py < fixture` averaged over N=10 runs

Cold-start (1st invocation): <Xms> p50, <Yms> p95
Steady-state (10th invocation, warm cache): <Ams> p50, <Bms> p95
Target: PreToolUse < 500ms p95 cold; < 100ms p95 warm
```

### How to verify

```bash
# Cold-start measurement (fresh Python invocation each time)
for i in $(seq 1 10); do
  time echo '{...}' | python3 .claude/hooks/pre-tool-guard.py > /dev/null
done 2>&1 | grep real

# Steady-state measurement (Python sub-interpreter cached — approximate via pyinstrument or pytest-benchmark)
python3 -m pytest tests/test-perf.py --benchmark-only
```

### Edge cases

- **Cold-start dominates for short hooks** — total may be 80% startup + 20% logic
- **Lazy imports** — `import gh_cli only when needed` reduces cold-start
- **Bytecode cache (`__pycache__`)** — second run faster; CI may delete pycache → always cold
- **CI vs local Python version** — CI Python 3.10 may differ from local 3.12 in import speed
- **Concurrent hook runs** — multiple PreToolUse hooks fire; latencies compound
- **Network in steady-state** — even cached, network calls vary (cache TTL, network jitter)

### Bonus
- CI step asserts p95 < target (`pytest-benchmark --benchmark-fail-on=mean>500ms`).
- Hook profile data tracked in `.claude/hooks/data/timing-baseline.json` (committed, updated quarterly).
- Hardware spec recorded in baseline: CPU model, RAM, Python version, OS.

Cross-link to [EC-012](edge-case-catalog.md#ec-012-cold-start-vs-steady-state-perf-drift).

---

## Summary table — reviewer sign-off template

Khi review PR thêm/sửa hook, fill checklist this:

```markdown
## Hook Review per `quality/hook-review/SKILL.md` (v1.1, 13-point rubric)

| # | Point | Verdict | Notes |
|---|---|---|---|
| 1 | Event matcher correctness | ✅/❌/N/A | <link to matcher line + event type per v1.1 enumeration> |
| 2 | BLOCK vs WARN gradient | ✅/❌/N/A | <blast radius justification + exit code mapping> |
| 3 | Override trailer recognition | ✅/❌/N/A | <trailer name + regex test + Semgrep-style annotations> |
| 4 | Fail-safe degradation | ✅/❌/N/A | <6 cases covered: missing dep/malformed/timeout/perm/env/pwd> |
| 5 | `settings.local.json` wiring | ✅/❌/N/A | <grep result + precedence chain verified> |
| 6 | False-positive testing | ✅/❌/N/A | <count negative test cases> |
| 7 | Idempotency | ✅/❌/N/A | <state file inventory + repeat-run test> |
| 8 | Performance budget | ✅/❌/N/A | <hook timing ms> |
| 9 | Exit code matrix + exit 1 trap | ✅/❌/N/A | <grep sys.exit calls; intent matches exit code?> |
| 10 | True-pos + true-neg fixture parity | ✅/❌/N/A | <count: positive cases / negative cases per BLOCK> |
| 11 | stdin malformed JSON handling | ✅/❌/N/A | <6 fixtures: malformed/empty/EOF/missing-fields/nested/large> |
| 12 | stdout JSON schema compliance | ✅/❌/N/A | <jq schema validate against Anthropic spec> |
| 13 | Hardware-pinned perf baseline | ✅/❌/N/A | <cold-start vs steady-state p50/p95 documented> |
```

All 13 ✅ or N/A → approve. Any ❌ → request changes citing rubric anchor + edge-case-catalog reference nếu pattern đã từng surface.

---

## Related

- `reference/edge-case-catalog.md` — 15 known patterns (9 v1.0 + 6 v1.1 covering Points 9-13); cross-reference khi 1+ point ❌
- `.claude/hooks/pre-tool-guard.py` — Reference implementation cho 5 rules deterministic enforcement
- `.claude/hooks/tests/test-pre-tool-guard.py` — Reference test pattern
- `.claude/rules/admin-merge-discipline.md` — Worked case cho hook implementation + Wave 74 trailer-on-squash incident
- `documents/04-quality/audits/meta/2026-05-14-wave-74-outside-in-benchmark.md` — v1.1 fold-in source (Wave 74 outside-in benchmark vs ESLint/Semgrep/OPA/pre-commit/Claude Code docs)
