# Local pre-commit hook — script-quality gate (lefthook)

**Last Updated:** 2026-04-29
**Closes:** GAP-194 Phase 2 (Wave Meta Phase-2 Cleanup, Cluster 7, Agent B)
**Related:**
- [`.github/workflows/quality-code.yml`](../../.github/workflows/quality-code.yml) — CI gate (Phase 1, shipped 2026-04-20)
- [`.claude/skills/quality/script-review-checklist.md`](../../.claude/skills/quality/script-review-checklist.md) — review skill
- [`lefthook.yml`](../../lefthook.yml) — config consumed by this guide

---

## Why

CI already enforces shellcheck (`-S error`) and ruff (`check`) on every PR via `quality-code.yml`. The pre-commit hook adds a **second, earlier gate** that runs locally before commit, so:

- Failures show up in seconds on your machine, not minutes after pushing to CI
- You don't burn a CI run on a typo a linter would have caught
- Solo-dev round-trip stays tight (avoid the push → wait → red CI → fix → push loop)

The pre-commit hook is **complementary**, not a replacement — CI remains the source of truth (someone could `--no-verify` past the local hook). Both gates run the same tools with parity flags so behavior matches.

## Tool: lefthook (single-binary, language-agnostic)

We use [`lefthook`](https://github.com/evilmartians/lefthook) instead of husky because:

- Project has **no `npm`/`pnpm` dependency at repo root** — Maven + pnpm are scoped to subprojects
- lefthook is a **single Go binary** — no Node runtime needed
- Config is one file (`lefthook.yml`) checked in at root
- Bypass mechanism (`git commit --no-verify`) is identical to other Git hook frameworks

Existing tools in the repo already match this profile (`shellcheck`, `ruff`, `python3`, `bash`).

## Install

Pick the install path for your OS — lefthook ships a single binary, no other deps:

```bash
# macOS (Homebrew)
brew install lefthook

# Debian/Ubuntu (apt)
sudo apt update && sudo apt install -y lefthook  # available on Ubuntu 24.04+; otherwise use Go path below

# Go install (any OS with Go ≥ 1.22 installed)
go install github.com/evilmartians/lefthook@latest

# npm (if you happen to already have Node available)
npm install -g lefthook

# Direct download (Linux, no package manager)
# See https://github.com/evilmartians/lefthook/releases for binaries
```

Verify:

```bash
lefthook version  # expect e.g. "1.7.x" or newer
```

## Activate in this repo

From repo root, run once:

```bash
lefthook install
```

This wires `.git/hooks/pre-commit` to invoke lefthook with the config in `lefthook.yml`. Subsequent `git commit` calls run the gate automatically against staged `.sh` and `.py` files.

To verify the install:

```bash
lefthook run pre-commit
```

(Runs the configured commands against all currently staged files; shows tool output.)

## What it checks

`lefthook.yml` defines two parallel commands that mirror the CI gate:

| Command | Glob | Tool | Mirrors CI step |
|---------|------|------|-----------------|
| `shellcheck` | `*.sh` | `shellcheck -S warning {staged_files}` | `quality-code.yml` job `shellcheck` |
| `ruff` | `*.py` | `ruff check {staged_files}` | `quality-code.yml` job `ruff` |

Notes:

- `-S warning` is slightly stricter than CI's `-S error` blocking gate (CI surfaces warnings non-blocking; the local hook fails on warnings to keep the staged tree clean — see `script-review-checklist.md`).
- `{staged_files}` is a lefthook template that expands to only files matching the `glob` AND added to the staging area; unmodified files are not re-linted.
- `parallel: true` runs both commands at once (typically <1 s on a small staged set).

Tool config remains the project root files:
- `.shellcheckrc` (project conventions, SC1091 disabled)
- `ruff.toml` (target py311, line 100, rules E/W/F/I/B/UP/SIM)

## Bypass for emergencies

```bash
git commit --no-verify -m "hotfix: ..."
```

Use sparingly — bypassing means CI is the only safety net. Acceptable when:

- Mid-rebase / staged conflict resolution where lint state is transient
- Documentation-only commits where the regex still matches (e.g., a `.sh` block embedded in markdown — the file isn't a `.sh` file but lefthook's glob may still trigger; rare)
- Hot-path incident response where CI will re-run the same checks anyway

`output-review-mandate.md` exception clauses still apply — bypassing is not a license to merge unreviewed code.

## Per-language fallback (no lefthook binary available)

If you can't install lefthook (e.g., locked-down dev VM), run the same checks manually before every commit:

```bash
# Shell scripts (changed files only)
git diff --cached --name-only --diff-filter=ACM | grep -E '\.sh$' \
  | xargs -r shellcheck -S warning

# Python scripts (changed files only)
git diff --cached --name-only --diff-filter=ACM | grep -E '\.py$' \
  | xargs -r ruff check
```

You can drop these two commands into a `pre-commit` shell script under `.git/hooks/pre-commit` (set `+x`) — that's effectively what lefthook does, just without the orchestration. Per-developer; not checked in.

## Troubleshooting

| Symptom | Cause | Fix |
|---------|-------|-----|
| `lefthook: command not found` after install | PATH missing Go bin / brew bin | Add `$(go env GOPATH)/bin` or `/opt/homebrew/bin` to PATH |
| Hook doesn't run on commit | `lefthook install` not run after clone | `lefthook install` once per fresh checkout |
| `shellcheck: not found` | Tool missing | `brew install shellcheck` / `sudo apt install shellcheck` |
| `ruff: not found` | Tool missing | `pip install ruff` / `pipx install ruff` |
| Hook fails on files I didn't touch | Stale `lefthook.yml` cache | `lefthook install --force` |

## Reference

- Gap: [`GAP-194-script-compliance-ci-shellcheck-ruff.md`](../04-quality/gaps/GAP-194-script-compliance-ci-shellcheck-ruff.md) — closes Phase 2 AC
- CI workflow: [`.github/workflows/quality-code.yml`](../../.github/workflows/quality-code.yml)
- Skill: [`.claude/skills/quality/script-review-checklist.md`](../../.claude/skills/quality/script-review-checklist.md)
- Rule: [`.claude/rules/output-review-mandate.md`](../../.claude/rules/output-review-mandate.md) §5.5
- lefthook docs: <https://lefthook.dev/>
