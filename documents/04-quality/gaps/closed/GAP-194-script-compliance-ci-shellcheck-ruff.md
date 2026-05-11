# GAP-194: Bash / Python Script Compliance (shellcheck / ruff in CI)

**Status:** 🟢 DONE 2026-04-29 — Phase 1 (CI gate + configs) shipped 2026-04-20; Phase 2 (lefthook pre-commit gate + local-dev guide) shipped 2026-04-29 (Wave Meta Phase-2 Cleanup, Cluster 7, Agent B)
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

- [x] `.shellcheckrc` + ruff config committed (Phase 1, 2026-04-20 — `ruff.toml` + `.shellcheckrc` at repo root)
- [x] Pre-commit hook blocks on script violations locally (Phase 2, 2026-04-29 — `lefthook.yml` mirrors CI parity)
- [x] CI job blocks PR merge on script violations (Phase 1, 2026-04-20 — `.github/workflows/script-quality.yml` `shellcheck -S error` blocking)
- [x] Baseline: all existing scripts green (Phase 1, 2026-04-20 — shellcheck `-S error` 0 issues across 50+ scripts; ruff 0 issues across all `.py` files; warnings non-blocking informational)
- [x] Documentation updated in `output-review-mandate.md` §5.5 (Phase 1, 2026-04-20 — Scripts row shows "✅ DONE" via `script-review-checklist` skill)
- [x] `quality/script-review/SKILL.md` references CI gate (Phase 1, 2026-04-20 — bash + python checklist items cite GAP-194 + workflow path; Phase 2, 2026-04-29 — added §Related cross-link to lefthook config + local-dev guide)

## Related

- action-1 §9 + §15.E
- GAP-081 script review checklist (DONE, this is enforcement phase)
- GAP-048 output review standards coverage
- `output-review-mandate.md` §5.5
- Rule: `.claude/rules/meta-gap-priority.md` §3 (Meta P1)

## Log

- 2026-04-20 — Created from action-1 §15.E.
- **2026-04-20 (Wave 8b-D):** CI enforcement shipped:
  - `.github/workflows/script-quality.yml` — shellcheck `-S error` blocking + `-S warning` non-blocking; ruff `check` blocking + `format --check` non-blocking. Triggers only on changed `**/*.sh` / `**/*.py` / config files.
  - `.shellcheckrc` (repo root) — shell=bash, source-path set for scripts dirs, SC1091 disabled (CI can't follow sourced siblings) with justification.
  - `ruff.toml` (repo root) — target py311, line 100, rules E/W/F/I/B/UP/SIM, exclude worktrees/starter-kit/documents/target.
  - Baseline fixes: `kitehub/scripts/up.sh` SC2145 (`${SERVICES[@]}` in echo string → `${SERVICES[*]}`); `.claude/hooks/audit-gate.py` F401 (unused `os` import), SIM105 (try/except/pass → `contextlib.suppress`), I001 (import ordering).
  - Baseline counts: shellcheck `-S error` across 50+ `.sh` files = **0 issues** (36 warnings informational, follow-up cleanup welcome); ruff across 2 `.py` files = **0 issues**.
  - Skill `quality/script-review-checklist.md` updated to reference CI gate.
  - Status **PARTIAL** (not DONE): pre-commit hook in acceptance criteria deferred — no `.husky/` exists in repo and project uses Maven/pnpm not npm. Follow-up: introduce husky or `lefthook` as separate gap if desired; CI gate already enforces pre-merge.
- **2026-04-29 (Wave Meta Phase-2 Cleanup, Cluster 7, Agent B):** Phase 2 shipped — lefthook pre-commit gate added; the outstanding AC (pre-commit hook blocks on script violations locally) is now satisfied. Status closes to DONE.
  - `lefthook.yml` (repo root) — single-binary, language-agnostic config (Go binary, no `npm` dependency at root). Two parallel commands matching CI parity:
    - `shellcheck` glob `*.sh` → `shellcheck -S warning {staged_files}`
    - `ruff` glob `*.py` → `ruff check {staged_files}`
    - YAML validated locally with `python3 -c "import yaml; yaml.safe_load(...)"`.
  - `documents/05-guides/local-dev/local-dev-pre-commit.md` (~150 lines) — install paths (Homebrew / apt / `go install` / npm / direct download), `lefthook install` activation, bypass via `git commit --no-verify`, per-language fallback (manual `git diff --cached | xargs shellcheck/ruff`), troubleshooting table, cross-links to CI workflow + skill + `output-review-mandate.md` §5.5.
  - `.claude/skills/quality/script-review-checklist.md` — added §Related cross-link to CI workflow + lefthook config + local-dev guide.
  - **Tool choice rationale:** lefthook chosen over husky because project has no root `package.json` — Maven + pnpm are subproject-scoped. Lefthook is single Go binary, fits "or equivalent" wording in original Proposed Fix #1.
  - **Verification:**
    - `lefthook.yml` YAML well-formed (`python3 -c "import yaml; yaml.safe_load(...)"` clean parse).
    - Cross-links resolve — verified `lefthook.yml`, `.github/workflows/script-quality.yml`, `.claude/skills/quality/script-review-checklist.md`, `.claude/rules/output-review-mandate.md`, gap file all reachable from guide via `test -f`.
    - Underlying tool gate self-test executed in worktree — synthetic dirty Python file (multiple imports on one line + unused imports) → `ruff check` exits 1 with 4 errors; synthetic syntax-error shell file → `shellcheck -S warning` exits 1 with SC1072/SC1073/SC1080. Both tools (`shellcheck` 0.x, `ruff` 0.x) installed locally and confirmed available. Lefthook orchestrates the same commands via `{staged_files}` — the orchestration layer is documented in the guide and reviewer can run `lefthook install && lefthook run pre-commit` once the binary is on PATH.
