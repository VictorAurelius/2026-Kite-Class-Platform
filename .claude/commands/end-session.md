---
name: end-session
description: "Dùng khi user nói 'end session', 'đóng session', 'kết thúc session', '/end-session', 'finish work', 'wrap up', hoặc trước khi /clear cho session quan trọng. Working-tree clean + sync gate + docs-sync 5-target (CSV/ROADMAP/wave-history/MEMORY/session-handoff) + auto-write handoff note + archive session-lock + 1-line summary. Symmetric counterpart cho /start-session."
user-invocable: true
argument-hint: "[--check-only] [--allow-dirty] [--keep-lock] [--summary-only] [--skip-handoff] [--no-pr]"
---

# /end-session — Session Close-out

Slash-command entry point. Full spec: **`.claude/skills/workflow/end-session/SKILL.md`**.
Governed by **`.claude/rules/session-end-context-check.md`** (§3 threshold + §4.5 5-target docs-sync).

Đóng session sạch để phiên sau `/start-session` pick up không miss state.

## Quick flow

1. **Step 0a — clean + sync gate** (BLOCKING; skip chỉ với `--allow-dirty`):
   ```bash
   bash .claude/skills/workflow/end-session/scripts/end-session.sh --check-only
   ```
   Block nếu main tree / worktree có uncommitted, hoặc main chưa sync / in-flight branch chưa push+handoff. Fix trước khi end.

2. **Context % check** (per `session-end-context-check.md` §3) — run `.claude/statusline-kite.sh` (JSON stdin per §4), report `X%` → threshold <50% / 70-84% / ≥85% / ≥95%.

3. **Docs-sync 5-target** (per §4.5 — fix stale, bundle 1 docs-only PR trừ khi `--no-pr`):
   gap-status.csv · ROADMAP §Current Status Snapshot · wave-history.jsonl · MEMORY.md · session-handoff note.

4. **Auto-write handoff** (skip nếu `--skip-handoff`) → `documents/03-planning/session-handoffs/YYYY-MM-DD-<scope>.md` (template `reference/handoff-template.md`): scope shipped + open PRs + pickup (việc đầu tiên) + background services + known issues.

5. **Archive lock + summary**:
   ```bash
   bash .claude/skills/workflow/end-session/scripts/end-session.sh   # gate → archive lock → 1-line summary
   ```
   (`--keep-lock`/`--summary-only` để giữ lock; `--check-only` chạy gate report only.)

6. **Recommend** `/clear` nếu context ≥70% (background tasks + detached servers + Docker stack **survive /clear**).

## Gotchas
- User explicit "end" = override §3 threshold, NHƯNG vẫn chạy Step 0a gate + docs-sync + handoff cho clean pickup.
- `statusline-kite.sh` đọc JSON stdin (`transcript_path`) — KHÔNG chạy standalone.
- KHÔNG merge PR pending-CI lúc end — để open, note ở pickup.
- Liệt kê background `run_in_background` tasks / detached servers / Docker stack ở handoff — chúng không mất khi `/clear`.

## Reference docs
- Full process: `.claude/skills/workflow/end-session/SKILL.md`
- Handoff template: `.claude/skills/workflow/end-session/reference/handoff-template.md`
- Rule: `.claude/rules/session-end-context-check.md` (§3 threshold + §4.5 5-target)
- Symmetric: `.claude/commands/start-session.md`
