---
name: hook-review
description: "Dùng khi PR thêm/sửa `.claude/hooks/*.py`, 'review hook', 'kiểm tra hook coverage', 'hook test missing', 'audit hook', 'hook BLOCK false-positive'. 8-point rubric covering event matcher + BLOCK/WARN gradient + override trailer + fail-safe + wiring + false-positive + idempotency + performance budget. Reference rubric-checklist.md cho full criteria + edge-case-catalog.md cho known patterns."
user-invocable: true
---

# Hook Review Skill

Reviewer checklist cho mọi PR thêm hoặc sửa `.claude/hooks/*.py`. Specialization của `quality/script-review-checklist.md` cho deterministic enforcement hooks — hooks không chỉ là Python scripts, mà là **gates** chạy mỗi tool call → blast radius lớn hơn nhiều so với một script ad-hoc.

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

## 8-point rubric summary

| # | Point | Quick test |
|---|---|---|
| 1 | Event matcher correctness | Regex covers đủ tool calls intended? |
| 2 | BLOCK vs WARN gradient | Severity match blast radius? BLOCK reserved for irreversible/shared-state |
| 3 | Override trailer recognition | Regex parses trailer trong commit body chính xác (case, whitespace, multi-line) |
| 4 | Fail-safe degradation | Hook crash → silent allow (exit 0), không BLOCK trên dep error |
| 5 | `settings.local.json` wiring | Hook file tồn tại NHƯNG không wired = 0% enforcement; grep verify |
| 6 | False-positive testing | Commit body / PR body chứa banned keyword KHÔNG được trigger BLOCK |
| 7 | Idempotency | Cùng input → cùng output, không log spam, không state mutation ngoài intent |
| 8 | Performance budget | PreToolUse < 500ms; PostToolUse < 1s; UserPromptSubmit < 500ms |

Đọc `reference/rubric-checklist.md` cho từng point chi tiết với edge cases + grep snippets.

## Gotchas

- **Rule docs nói X, hook implement Y** — case study Wave 74: `admin-merge-discipline.md` §4 mandate trailer trên SQUASH commit, nhưng `check_admin_merge` đọc `git log -1 --format=%B` (= HEAD commit). Squash commit chưa tồn tại pre-merge → hook BLOCK chính đáng nhưng UX mismatch. Resolution: amend HEAD với trailer + force-push. Catalog class "Rule text vs hook implementation divergence".
- **Trailer regex case-sensitive** — `ADMIN_MERGE_OVERRIDE:` chứ không phải `admin_merge_override:`. Reviewer grep diff cho exact-case match.
- **Hook không trong `settings.local.json` ≡ không tồn tại** — file in `.claude/hooks/` mà không wired = dead code. Mọi hook PR phải verify wiring bằng `grep hooks/<name>.py .claude/settings.local.json`.
- **`subprocess.run(timeout=N)` MUST < hook timeout** — nếu hook timeout 5s nhưng nested `subprocess.run(timeout=10)` → hook process killed mid-call, fail-safe path không chạy → user thấy hook treo. Set inner timeout < outer.
- **PR body ≠ HEAD commit body** — hook đọc git commit message; trailer trong PR description không count. Reviewer document rõ trong rule §Override.
- **Test fixture isolation** — test phải mock `_commit_body()` hoặc set up tmp git repo; nếu test inherit caller's git HEAD → flaky.

## Skill Contents

- `reference/rubric-checklist.md` — Full 8-point rubric với grep snippets + per-point examples + bonus checks
- `reference/edge-case-catalog.md` — Known false-positive/false-negative patterns; each entry tracks 1 incident class with reproduction recipe + recommended fix

## Related

- `.claude/rules/admin-merge-discipline.md` — Rule đầu tiên có hook implementation (Wave 73 Bucket B); concrete worked case xuyên suốt skill này
- `.claude/skills/quality/script-review-checklist.md` — Parent generic script checklist; hook-review specializes cho `.claude/hooks/*.py`
- `.claude/rules/rule-change-process.md` §6.5 Enforcement Parity Mandate — hooks là 1 trong các enforcement types phải ship cùng PR với rule
- `.claude/hooks/pre-tool-guard.py` — Reference implementation (5 rules deterministic enforcement)
- `.claude/hooks/tests/test-pre-tool-guard.py` — Reference test pattern (subprocess + JSON contract assertions)
