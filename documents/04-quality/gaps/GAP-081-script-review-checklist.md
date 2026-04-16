# GAP-081: Script Review Checklist & Skill

**Status:** 🔵 OPEN
**Priority:** 🔴 P0
**Domain:** DevOps / Quality Gate
**Found:** 2026-04-16 (skills gap simulation)
**Affects:** All bash/Python scripts (~20+ files across project)

## Problem

`output-review-mandate.md` flags scripts as VIOLATION — no review standard exists. Scripts (bash/Python) in `scripts/`, `kiteclass/scripts/`, `kitehub/scripts/` are reviewed only as general code. No checklist for: error handling, security, dry-run mode, documentation.

Rủi ro: scripts chạy Docker, DB, CI pipelines — 1 bug = data loss hoặc security breach.

## Proposed Fix

1. Tạo skill `quality/script-review/SKILL.md` với checklist:
   - [ ] `set -euo pipefail` (bash) hoặc equivalent error handling
   - [ ] No `eval`, no hardcoded secrets, no `rm -rf /` without guard
   - [ ] `--dry-run` mode supported (hoặc documented tại sao không cần)
   - [ ] Usage comment ở đầu file
   - [ ] shellcheck passes (bash) / ruff passes (Python)
   - [ ] Exit codes meaningful (0=success, 1=error, 2=usage)
2. Thêm shellcheck vào pre-commit hook
3. Thêm vào PR template: "[ ] Scripts reviewed per script-review skill"

## Acceptance Criteria

- [ ] Skill file tồn tại với checklist
- [ ] shellcheck chạy trong CI cho `*.sh` files
- [ ] Existing scripts pass checklist (hoặc exceptions documented)
