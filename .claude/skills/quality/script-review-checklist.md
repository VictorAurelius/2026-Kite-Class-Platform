---
name: script-review
description: "Dùng khi PR có .sh/.py files, 'review script', 'check script', 'kiểm tra script'. Checklist cho bash/python scripts."
user-invocable: true
---

# Script Review Checklist

Dùng khi PR thêm/sửa `.sh` hoặc `.py` files trong `scripts/`, `kitehub/scripts/`, `kiteclass/scripts/`.

## Bash Scripts

- [ ] `set -euo pipefail` ở đầu file
- [ ] shellcheck passes — **CI enforces `-S error`** via `.github/workflows/quality-code.yml` (GAP-194). Warnings reported non-blocking but should be fixed before merge.
- [ ] No `eval`, no `curl | bash`, no `rm -rf /` without guard
- [ ] No hardcoded secrets (passwords, tokens, API keys)
- [ ] Input sanitized — quote all `"$variables"`, validate args
- [ ] Timeout on external calls (`timeout 30 curl ...`)
- [ ] Usage comment at top: purpose, args, examples
- [ ] `--help` flag prints usage
- [ ] `--dry-run` mode for destructive operations (or documented why not needed)
- [ ] Exit codes meaningful: 0=success, 1=error, 2=usage
- [ ] Idempotent — safe to run twice without side effects
- [ ] Works on Linux + macOS + Git Bash (no GNU-only flags without check)

## Python Scripts

- [ ] ruff passes — **CI enforces `ruff check`** via `.github/workflows/quality-code.yml` (GAP-194). Config in `ruff.toml` (root). Format diffs reported non-blocking.
- [ ] No `eval()`, no `subprocess.shell=True` with user input
- [ ] No hardcoded secrets
- [ ] Type hints on public functions
- [ ] `argparse` with `--help`
- [ ] Error handling: try/except with meaningful messages
- [ ] `if __name__ == "__main__"` guard

## Gotchas

- `kitehub/scripts/` — Docker scripts MUST use `docker compose` (not `docker-compose`)
- Port conflicts — verify port availability before binding (`lsof -i :PORT`)
- Windows Git Bash — avoid `realpath`, use `$(cd "$(dirname "$0")" && pwd)` instead
- CI scripts — must exit non-zero on failure (CI treats 0 as success)

## Related

- **CI gate:** `.github/workflows/quality-code.yml` enforces shellcheck + ruff on every PR (GAP-194 Phase 1, shipped 2026-04-20).
- **Local pre-commit gate:** `lefthook.yml` mirrors the CI checks via `lefthook install`. See [`documents/05-guides/local-dev/local-dev-pre-commit.md`](../../../documents/05-guides/local-dev/local-dev-pre-commit.md) for install + bypass + fallback (GAP-194 Phase 2, shipped 2026-04-29).
