# Hook Edge Case Catalog — known patterns

Living catalog của false-positive / false-negative / divergence patterns đã từng surface trong project. Mỗi entry: **class** + **reproduction** + **detection signal** + **fix pattern** + **cross-ref**.

Reviewer cross-check khi 1+ rubric point ❌ trong `rubric-checklist.md` — entry trùng class → áp dụng fix recipe sẵn có.

Mới phát hiện edge case → append entry mới với date + worked example.

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

## Catalog coverage matrix

| Rubric point | Entries with test |
|---|---|
| 1. Event matcher correctness | EC-003 |
| 2. BLOCK vs WARN gradient | EC-002 |
| 3. Override trailer recognition | EC-001, EC-004 |
| 4. Fail-safe degradation | EC-005 |
| 5. `settings.local.json` wiring | EC-006 |
| 6. False-positive testing | EC-007 |
| 7. Idempotency | EC-008 |
| 8. Performance budget | EC-009 |

All 8 points covered ✅ (per skill creation Wave 74 acceptance criteria).

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

- `rubric-checklist.md` — Full 8-point criteria; this catalog supports per-point edge case awareness
- `SKILL.md` — Entry point; guides reviewer to this catalog after rubric run
- `.claude/rules/incident-to-rule-pipeline.md` — 5-stage pipeline; each EC entry typically corresponds to a Detect→Classify outcome
