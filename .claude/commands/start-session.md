---
name: start-session
description: "Dùng khi user nói 'start new session', 'bắt đầu session mới', '/start-session', 'what's the state?', 'tình trạng hiện tại', hoặc khi cần load context sau khi /clear. Load: CLAUDE.md digest, current wave, open PRs, failing CI, active blockers. Output: single summary block. Cũng check session-lock conflicts khi chạy parallel agents."
user-invocable: true
argument-hint: "[--quick] [--no-lock]"
---

# /start-session — Session Orchestration

Slash-command entry point. Full spec lives in the skill:
**`.claude/skills/workflow/start-session/SKILL.md`**

## Quick flow

1. Run state collector:
   ```bash
   ./.claude/skills/workflow/start-session/scripts/collect-state.sh
   ```

2. If collector output insufficient, prefer script queries (skip file reads to minimize path-trigger rules):
   - **Gaps:** `bash scripts/query-gaps.sh <prefix>` (NOT Read ROADMAP — triggers ~10 gap-* rules ~150k bytes)
   - **Recent waves:** `tail -3 .claude/skills/quality/wave-pack-planner/data/wave-history.jsonl | jq -r '.wave + " — " + .outcome'`
   - **CLAUDE.md** auto-loads — NEVER Read explicitly
   - Read `documents/03-planning/waves/<specific>.md` ONLY khi cần plan detail for current task

3. **Session-lock check** (skip if `--no-lock`):
   - `ls .claude/session-locks/` — look for active locks
   - Warn if branch/gap conflict with another session
   - Create lock `session-{YYYYMMDD-HHMMSS}-{hostname}.lock`

4. **Output single summary block** per `reference/context-template.md`:
   ```
   ## Session Context (YYYY-MM-DD HH:MM)
   **Wave:** ...
   **Branch:** ... / worktrees: N
   **Open PRs:** N
   **CI:** main green/red
   **Blocker gaps:** GAP-XXX, GAP-YYY, ...
   **Context health:** fresh / degraded
   **Recommended next:** ...
   ```

5. **Handoff** — suggest next skill: `/continue`, `/repo-status`, `/gap-triage`, or task user requested.

## Context-degradation heuristic

Flag session as **degraded** if ANY:
- Turn count > 40
- Last `/compact` > 2h ago
- User reported output quality drop
- Multiple retries on simple tasks

**Action when degraded:** nudge user toward `/clear` + re-run `/start-session`. Critical work (wave merge, production deploy) MUST NOT proceed in degraded session.

## Gotchas

- `collect-state.sh` needs `gh` CLI authenticated — falls back gracefully if not
- `.claude/session-locks/` is git-ignored (per `.gitignore:23`)
- Stale locks (>4h) auto-purged by next `/start-session` invocation
- Lock is a HINT, not enforcement — git branch/worktree isolation is real barrier

## Reference docs

- Full process: `.claude/skills/workflow/start-session/SKILL.md`
- Output format + lock schema + 4 examples: `.claude/skills/workflow/start-session/reference/context-template.md`
- Origin gap: `documents/04-quality/gaps/closed/GAP-193-session-orchestration-skill.md`
