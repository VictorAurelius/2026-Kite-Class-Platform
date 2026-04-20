---
name: start-session
description: "Dùng khi user nói 'start new session', 'bắt đầu session mới', '/start-session', 'what's the state?', 'tình trạng hiện tại', hoặc khi cần load context sau khi /clear. Load: CLAUDE.md digest, current wave, open PRs, failing CI, active blockers. Output: single summary block. Cũng check session-lock conflicts khi chạy parallel agents."
user-invocable: true
argument-hint: "[--quick] [--no-lock]"
---

# /start-session — Session Orchestration

Prepare fresh session bằng cách load đúng context + detect conflicts với concurrent sessions.

## When to use

- Bắt đầu conversation mới trong repo này
- Sau khi `/clear` hoặc `/compact`
- Trước critical work (wave merge, audit, release)
- Khi unsure "làm gì tiếp?" → chạy skill này thay vì đọc mò

## Process

### Step 1 — Run state collector

```bash
./.claude/skills/workflow/start-session/scripts/collect-state.sh
```

Script output: active branch, open PRs count, failing CI runs, top 3 blocker gaps, current wave, last /compact timestamp (if logged).

### Step 2 — Read digest sources

Nếu script không đủ context, đọc theo thứ tự:

1. `CLAUDE.md` (top 50 lines — rules, wave strategy, naming)
2. `documents/03-planning/waves/` → file newest (current wave plan)
3. `documents/04-quality/gaps/ROADMAP.md` (top 30 lines — current status snapshot)
4. `documents/03-planning/MASTER-GAPS-FIX-PLAN.md` (if planning horizon needed)

KHÔNG đọc toàn bộ ROADMAP/plan — chỉ header + current section.

### Step 3 — Session-lock check (optional, `--no-lock` skips)

1. `ls .claude/session-locks/` — list active locks
2. Nếu có lock match branch/gap user định touch → **WARN** user: "Session X đang edit branch Y / GAP-Z, continue = risk conflict"
3. User confirm → create own lock: `session-{timestamp}-{hostname}.lock`
4. Lock content: YAML với `branch`, `gaps`, `started`, `turn_estimate`

Chi tiết: `reference/context-template.md` §Session Locks.

### Step 4 — Output summary block

Format theo `reference/context-template.md`. Ví dụ:

```
## Session Context (2026-04-20 14:32)
**Wave:** 8b (meta cluster, 6 parallel agents in flight)
**Branch:** main (clean) / agent worktrees: 6
**Open PRs:** 3 (PR #394 merged, PR #395-397 pending review)
**CI:** main green, 1 PR red (#396 — flaky E2E)
**Blocker gaps:** GAP-193 (this skill), GAP-199 (rework audit), GAP-185 (BRD persona AC)
**Context health:** fresh session (turn 1, cache warm)
**Recommended next:** continue Wave 8b agent work OR /continue for top priority
```

### Step 5 — Handoff to next skill

User decides:
- `/continue` — execute top priority from plan
- `/repo-status` — deeper health check
- `/gap-triage` — review gap backlog
- `/pr-health 290-300` — audit recent merges

## Context-degradation heuristic

Flag session as **degraded** if ANY:

- Turn count > 40 (manual count — harness doesn't expose; estimate from conversation length)
- Last `/compact` > 2 hours ago
- User reported output quality drop
- Multiple retries on simple tasks

**Action khi degraded:** nudge user to `/clear` + re-run `/start-session`. Critical work (wave merge, production deploy) → DON'T proceed in degraded session.

## Session lock convention

Directory: `.claude/session-locks/` (git-ignored, per-machine).

File naming: `session-{YYYYMMDD-HHMMSS}-{hostname}.lock`

Schema: see `reference/context-template.md` §Lock File Schema.

Lock lifecycle:
- Created on `/start-session` (unless `--no-lock`)
- Updated when switching branch/gap mid-session
- Removed on explicit `/end-session` OR stale after 4h (any new session cleans stale locks)

**MVP:** lock read + warn only, no enforcement hook. Phase 2 (post-pilot): add pre-commit hook to block commits on locked branch from different session.

## Gotchas

- `collect-state.sh` needs `gh` CLI authenticated — fall back to manual `gh pr list` nếu script fails
- `.claude/session-locks/` KHÔNG commit (add to `.gitignore` nếu chưa có)
- Stale locks (>4h) auto-purged — nhưng crash mid-session có thể leave orphan; `--no-lock` hoặc manual `rm` clear
- Hostname trên WSL = `wsl-name`; multiple Claude Code windows cùng host vẫn tạo unique lock (timestamp differs)
- KHÔNG rely solely trên lock để prevent conflict — lock là hint, git branch isolation (worktree) mới là enforcement thật

## Skill contents

- `SKILL.md` — this file
- `reference/context-template.md` — output format + lock schema + example
- `scripts/collect-state.sh` — state collector
- `data/` — session history log (optional, append-only)

## Related

- `.claude/skills/workflow/continue/SKILL.md` — use after /start-session to act on top priority
- `.claude/skills/workflow/repo-status/SKILL.md` — deeper remote health
- `.claude/skills/quality/rework-audit/SKILL.md` — consumes degradation signal for retrospective audit
- Rule: `.claude/rules/output-review-mandate.md` (sessions produce outputs)
- Memory: `feedback_parallel_agent_strategy.md` (multi-agent safety)
