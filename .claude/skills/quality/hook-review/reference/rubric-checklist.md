# Hook Review — 8-Point Rubric (full checklist)

Detailed criteria cho mỗi điểm trong rubric. Reviewer dùng tài liệu này khi check PR thêm/sửa `.claude/hooks/*.py`.

Mỗi point có: **What** (định nghĩa), **Why** (blast radius), **How to verify** (grep / test command), **Edge cases**, **Bonus**.

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

## Summary table — reviewer sign-off template

Khi review PR thêm/sửa hook, fill checklist this:

```markdown
## Hook Review per `quality/hook-review/SKILL.md`

| # | Point | Verdict | Notes |
|---|---|---|---|
| 1 | Event matcher correctness | ✅/❌/N/A | <link to matcher line in settings.local.json> |
| 2 | BLOCK vs WARN gradient | ✅/❌/N/A | <blast radius justification> |
| 3 | Override trailer recognition | ✅/❌/N/A | <trailer name + regex test result> |
| 4 | Fail-safe degradation | ✅/❌/N/A | <exit code on crash test> |
| 5 | `settings.local.json` wiring | ✅/❌/N/A | <grep result> |
| 6 | False-positive testing | ✅/❌/N/A | <count negative test cases> |
| 7 | Idempotency | ✅/❌/N/A | <state file inventory + repeat-run test> |
| 8 | Performance budget | ✅/❌/N/A | <hook timing ms> |
```

All 8 ✅ or N/A → approve. Any ❌ → request changes citing rubric anchor + edge-case-catalog reference nếu pattern đã từng surface.

---

## Related

- `reference/edge-case-catalog.md` — Known patterns; cross-reference khi 1+ point ❌
- `.claude/hooks/pre-tool-guard.py` — Reference implementation cho 5 rules deterministic enforcement
- `.claude/hooks/tests/test-pre-tool-guard.py` — Reference test pattern
- `.claude/rules/admin-merge-discipline.md` — Worked case cho hook implementation + Wave 74 trailer-on-squash incident
