---
name: hook-review
version: 1.1.0
description: "Dùng khi PR thêm/sửa `.claude/hooks/*.py`, 'review hook', 'kiểm tra hook coverage', 'hook test missing', 'audit hook', 'hook BLOCK false-positive'. 13-point rubric (Wave 75 outside-in fold-in): event matcher + BLOCK/WARN gradient + override trailer + fail-safe + wiring + false-positive + idempotency + performance budget + exit code matrix + fixture parity + stdin malformed JSON + stdout JSON schema + cold-start vs steady-state. Reference rubric-checklist.md cho full criteria + edge-case-catalog.md cho 15 known patterns."
user-invocable: true
---

# Hook Review Skill

Reviewer checklist cho mọi PR thêm hoặc sửa `.claude/hooks/*.py`. Specialization của `quality/script-review-checklist.md` cho deterministic enforcement hooks — hooks không chỉ là Python scripts, mà là **gates** chạy mỗi tool call → blast radius lớn hơn nhiều so với một script ad-hoc.

**Version 1.1.0 (Wave 75 Bucket B):** rubric mở rộng từ 8 → 13 points dựa trên outside-in benchmark vs ESLint RuleTester / Semgrep / OPA / pre-commit / Claude Code official docs. 5 NEW points cover exit code matrix (counterintuitive `exit 1` trap), fixture parity, stdin malformed handling, stdout JSON schema, hardware-pinned perf baseline. 5 existing points sharpened. 6 NEW EC entries (EC-010 → EC-015).

## When to use

- PR touches any file trong `.claude/hooks/` (Python source OR test fixtures)
- PR thêm rule mới trong `.claude/rules/` mà rule có enforcement = hook (per `rule-change-process.md` §6.5 Enforcement Parity)
- User báo hook BLOCK sai cảnh (false-positive) hoặc miss case (false-negative)
- Quarterly retro audit toàn bộ hook coverage

## Process

1. **Identify scope** — đọc diff, classify: hook mới / sửa rule existing / sửa test / wiring change (`settings.local.json`).
2. **Run 8-point rubric** — xem `reference/rubric-checklist.md` cho criteria chi tiết per point.
3. **Cross-check edge case catalog** — xem `reference/edge-case-catalog.md` cho known false-positive/false-negative patterns; bonus check nếu diff trùng class đã từng surface.
4. **Verify tests + wiring** — chạy `python3 -m unittest .claude/hooks/tests/test-<hook>.py` PASS; verify hook được wire trong `.claude/settings.local.json` (nếu hook mới).
5. **Sign off** — PR review comment cite per-point verdict (PASS/FAIL/N/A) + rubric anchor.

## 13-point rubric summary

| # | Point | Quick test |
|---|---|---|
| 1 | Event matcher correctness | Regex covers đủ tool calls intended? + 4 Anthropic event types enumerated (PreToolUse/PostToolUse/UserPromptSubmit/Stop) |
| 2 | BLOCK vs WARN gradient | Severity match blast radius? + map to exit code matrix (Point 9) |
| 3 | Override trailer recognition | Regex parses trailer chính xác (case, whitespace, multi-line) + Semgrep-style fixture annotations |
| 4 | Fail-safe degradation | Hook crash → silent allow; 6 specific cases enumerated (missing dep / malformed input / timeout / permission / env unset / wrong pwd) |
| 5 | `settings.local.json` wiring | Hook wired? + settings precedence chain (project → local → user) |
| 6 | False-positive testing | Banned keyword in benign context KHÔNG trigger BLOCK |
| 7 | Idempotency | Cùng input → cùng output, không log spam |
| 8 | Performance budget | PreToolUse < 500ms; PostToolUse < 1s; UserPromptSubmit < 500ms |
| **9** | **Exit code matrix + `exit 1` trap** | exit 0 / 1 / 2 semantics explicit; `exit 1` counterintuitive (non-blocking trong Claude Code!) |
| **10** | **True-pos + true-neg fixture parity** | Mỗi BLOCK condition có ≥1 valid fixture + ≥1 invalid fixture (ESLint RuleTester mandate) |
| **11** | **stdin malformed JSON handling** | Hook fail-safe silent allow trên malformed stdin, không crash |
| **12** | **stdout JSON contract schema** | Output keys match Anthropic spec (`hookSpecificOutput.permissionDecision`, `systemMessage`, etc.) |
| **13** | **Hardware-pinned perf baseline (cold vs warm)** | Cold-start tách khỏi steady-state; target ms per dev hardware documented |

Đọc `reference/rubric-checklist.md` cho từng point chi tiết với edge cases + grep snippets.

## Gotchas

- **Rule docs nói X, hook implement Y** — case study Wave 74: `admin-merge-discipline.md` §4 mandate trailer trên SQUASH commit, nhưng `check_admin_merge` đọc `git log -1 --format=%B` (= HEAD commit). Squash commit chưa tồn tại pre-merge → hook BLOCK chính đáng nhưng UX mismatch. Resolution: amend HEAD với trailer + force-push. Catalog class "Rule text vs hook implementation divergence".
- **Trailer regex case-sensitive** — `ADMIN_MERGE_OVERRIDE:` chứ không phải `admin_merge_override:`. Reviewer grep diff cho exact-case match.
- **Hook không trong `settings.local.json` ≡ không tồn tại** — file in `.claude/hooks/` mà không wired = dead code. Mọi hook PR phải verify wiring bằng `grep hooks/<name>.py .claude/settings.local.json`.
- **`subprocess.run(timeout=N)` MUST < hook timeout** — nếu hook timeout 5s nhưng nested `subprocess.run(timeout=10)` → hook process killed mid-call, fail-safe path không chạy → user thấy hook treo. Set inner timeout < outer.
- **PR body ≠ HEAD commit body** — hook đọc git commit message; trailer trong PR description không count. Reviewer document rõ trong rule §Override.
- **Test fixture isolation** — test phải mock `_commit_body()` hoặc set up tmp git repo; nếu test inherit caller's git HEAD → flaky.

## Skill Contents

- `reference/rubric-checklist.md` — Full 13-point rubric với grep snippets + per-point examples + bonus checks (8 original + 5 v1.1.0 additions; 5 existing sharpened per Wave 74 outside-in benchmark)
- `reference/edge-case-catalog.md` — 15 known patterns (9 original + 6 v1.1.0 additions covering Points 9-13)

## Related

- `.claude/rules/admin-merge-discipline.md` — Rule đầu tiên có hook implementation (Wave 73 Bucket B); concrete worked case xuyên suốt skill này
- `.claude/skills/quality/script-review-checklist.md` — Parent generic script checklist; hook-review specializes cho `.claude/hooks/*.py`
- `.claude/rules/rule-change-process.md` §6.5 Enforcement Parity Mandate — hooks là 1 trong các enforcement types phải ship cùng PR với rule
- `.claude/hooks/pre-tool-guard.py` — Reference implementation (5 rules deterministic enforcement)
- `.claude/hooks/tests/test-pre-tool-guard.py` — Reference test pattern (subprocess + JSON contract assertions)
