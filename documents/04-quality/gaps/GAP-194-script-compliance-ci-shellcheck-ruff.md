# GAP-194: Bash / Python Script Compliance (shellcheck / ruff in CI)

**Status:** 🔵 OPEN
**Priority:** 🟠 P1 (meta tier — enforcement of existing standard)
**Domain:** Meta / DevOps / CI
**Found:** 2026-04-20 (action-1 §9 + §15.E; flagged by user as "improvement riêng")
**Wave:** Wave 8b (meta)
**Affects:** All bash scripts (~20+) in `scripts/`, `kiteclass/scripts/`, `kitehub/scripts/`; Python scripts (hooks, capture tools)

## Problem

GAP-081 (script-review-checklist — DONE) + `output-review-mandate.md` §5.5 both list shellcheck / ruff as acceptance criteria, but neither is enforced in CI:

- Bash scripts not gated by shellcheck — regressions slip through
- Python scripts not gated by ruff — lint drift over time
- Pre-commit hook not wired
- Skill `quality/script-review/` references the tools but no executable gate

User mention: action-1 line 544–546 "known limitation, improvement riêng".

## Context

This is enforcement of an already-agreed standard, not a new policy. Low-risk, high-leverage.

## Proposed Fix

1. **Pre-commit hook** (`.husky/pre-commit` or equivalent)
   - `shellcheck -S warning $changed_sh_files`
   - `ruff check $changed_py_files`
2. **GitHub Actions job** — `.github/workflows/script-quality.yml`
   - Runs on push + PR affecting `**/*.sh`, `**/*.py`
   - shellcheck (with `.shellcheckrc` allowing known project conventions)
   - ruff (config in `pyproject.toml` or `ruff.toml`)
3. **Baseline pass** — run once, fix or add inline `# shellcheck disable=` / `# noqa` with justification
4. **Update skill** — add "CI enforces this" note to `quality/script-review/SKILL.md`
5. **Update `output-review-mandate.md`** §5.5 — move Scripts from ✅ DONE (standard exists) to ✅ DONE (standard exists + enforced)

## Acceptance Criteria

- [ ] `.shellcheckrc` + ruff config committed
- [ ] Pre-commit hook blocks on script violations locally
- [ ] CI job blocks PR merge on script violations
- [ ] Baseline: all existing scripts green (disables justified in code)
- [ ] Documentation updated in `output-review-mandate.md` §5.5
- [ ] `quality/script-review/SKILL.md` references CI gate

## Related

- action-1 §9 + §15.E
- GAP-081 script review checklist (DONE, this is enforcement phase)
- GAP-048 output review standards coverage
- `output-review-mandate.md` §5.5
- Rule: `.claude/rules/meta-gap-priority.md` §3 (Meta P1)

## Log

- 2026-04-20 — Created from action-1 §15.E.
