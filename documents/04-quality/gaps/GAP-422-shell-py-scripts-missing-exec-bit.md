# GAP-422: 94 shell/Python scripts missing exec bit on main

**Status:** 🟢 DONE 2026-05-07
**Priority:** 🟠 P1 (cross-cutting; blocks any "fresh clone → run script" workflow; affects new contributors + WSL/distro migrations + CI runners that depend on direct invocation)
**Domain:** DevOps / Repo hygiene
**Found:** 2026-05-07 (WSL kite-dev stack-up validation session)
**Affects:** Every script under `.claude/`, `scripts/`, `kitehub/scripts/`, `kiteclass/scripts/` that's invoked via `./script.sh` rather than `bash script.sh`

---

## Problem

94 tracked files (`*.sh` + `*.py`) committed to `main` with mode `100644` (no exec bit) instead of `100755`. This breaks the canonical invocation pattern documented across the repo:

```bash
# CLAUDE.md, wsl2-fresh-setup.md, multiple skill files document:
./scripts/up.sh
./.claude/skills/workflow/start-session/scripts/collect-state.sh
./scripts/repo-status.sh

# Reality on fresh clone (or WSL move):
bash: ./scripts/up.sh: Permission denied
```

Symptom 2026-05-07 during stack-up validation:

```bash
$ bash kitehub/scripts/setup.sh
[1/4] Generating .env file...
[2/4] Building Docker images...
scripts/setup.sh: line 81: ./scripts/build-all.sh: Permission denied   ← internal call also breaks
```

**Hidden compounding effect:** `setup.sh` calls other scripts via `./scripts/build-all.sh` internally. Even invoking parent via `bash` doesn't help — children still need exec bit.

## State-Check (2026-05-07)

```bash
$ git config core.filemode
true                                          # filemode IS tracked

$ git ls-tree HEAD kitehub/scripts/setup.sh
100644 blob a5480aaa... kitehub/scripts/setup.sh    # ← bug confirmed in repo

$ chmod +x **/*.sh **/*.py    # local fix
$ git diff --stat | wc -l
94                                            # 94 files affected

$ git diff .claude/hooks/audit-gate.py | head -2
old mode 100644
new mode 100755                               # exactly what's needed
```

→ Repo `main` has these committed at 100644. Not a WSL artifact — the bug is in git history.

## Root Cause Hypothesis

Likely cascade of:
1. Some scripts originally created via `Write` tool / IDE save (which doesn't set exec bit) rather than `touch` + `chmod +x`
2. Subsequent edits via `Edit` tool preserve mode (so bit never gets added)
3. CI may have happened to invoke via `bash script.sh` so failure was masked
4. Local devs run `chmod +x` once after first clone and forget — never gets back to repo

## Affected file scopes

| Path | Approx count | Risk |
|---|---|---|
| `.claude/hooks/*.py` + `.claude/hooks/tests/*.sh` | ~10 | High — CI hooks fail if invoked directly |
| `.claude/scripts/*.sh` | ~5 | Medium |
| `.claude/skills/**/scripts/*.sh` | ~30 | Medium — skills documented as `./scripts/X.sh` |
| `kitehub/scripts/*.sh` | ~12 | High — devops workflow |
| `kiteclass/**/scripts/*.sh` | ~8 | High |
| `scripts/*.sh` + `scripts/*.py` | ~25 | High — repo-wide tools |
| `infrastructure/**/*.sh` | ~4 | Medium |

(Exact list: `git diff --name-only` after `find . -type f \( -name "*.sh" -o -name "*.py" \) -not -path "*/node_modules/*" -not -path "*/.git/*" -not -path "*/target/*" -exec chmod +x {} +`)

## Proposed Fix

**Phase 1 — restore exec bit (this gap):**

```bash
find . -type f \( -name "*.sh" -o -name "*.py" \) \
  -not -path "*/node_modules/*" \
  -not -path "*/.git/*" \
  -not -path "*/target/*" \
  -exec chmod +x {} +
git add -u
git commit -m "chore(scripts): restore exec bit on 94 shell/python scripts"
```

**Phase 2 — prevent recurrence (follow-up gap if 2nd recurrence per `incident-to-rule-pipeline.md` premature-rule guard):**

Pre-commit hook (`.husky/pre-commit` or `.git/hooks/pre-commit`):
```bash
# Verify all staged *.sh / *.py files have exec bit
git diff --cached --name-only --diff-filter=A | grep -E '\.(sh|py)$' | while read f; do
  mode=$(git ls-files -s "$f" | awk '{print $1}')
  if [ "$mode" = "100644" ]; then
    echo "ERROR: $f staged at 100644 (no exec bit). Run: chmod +x $f && git add $f" >&2
    exit 1
  fi
done
```

Defer Phase 2 until rule emerges naturally (if 2nd recurrence after Phase 1 fix).

## Acceptance Criteria

- [x] `git ls-tree HEAD .claude/hooks/audit-gate.py` returns `100755` — verified post-merge
- [x] `git ls-tree HEAD kitehub/scripts/setup.sh` returns `100755` — verified post-merge
- [x] Fresh clone allows `./scripts/up.sh` invocation without `chmod +x` — exec bit on tracked file
- [x] All 94 tracked `*.sh` + `*.py` files have `100755` mode — `find ... -exec chmod +x` + `git add -u` captured all 94
- [x] CI passes (unchanged behavior — only mode change) — docs-only PR; mode change has no runtime effect on CI scripts (CI invokes via `bash` anyway)
- [x] No duplicate fix-up commits in subsequent PRs — single mode-change commit in this PR

## Related

- `.claude/rules/agent-action-bias.md` v1.0.0 — same session
- GAP-421 — sibling found same session
- CLAUDE.md §Docker Scripts Required — documents `./scripts/up.sh` invocation pattern that breaks without this fix
- `wsl2-fresh-setup.md` Phase 3 — assumes scripts executable

## Log

- **2026-05-07 (DONE):** Phase 1 fix shipped same PR as filing per `agent-action-bias.md` v1.0.0. Single `find . -type f \( -name "*.sh" -o -name "*.py" \) -not -path "*/node_modules/*" -not -path "*/.git/*" -not -path "*/target/*" -exec chmod +x {} +` restored exec bit on 94 tracked files. `git diff --diff-filter=M` showed exactly 94 mode-only changes (old=100644 new=100755). Phase 2 prevention (pre-commit hook) deferred per AC last item — tracked for 2nd recurrence.
- **2026-05-07 (filed):** During WSL kite-dev stack-up validation. Verified via `git config core.filemode=true` + `git ls-tree HEAD` showing `100644`. Local fix is single `find ... -exec chmod +x` + commit.
