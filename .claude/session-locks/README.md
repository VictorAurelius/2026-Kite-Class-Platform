# Session Locks

Per-session lock files for `/start-session` skill (GAP-193).

**Purpose:** advisory hint to detect concurrent-session conflicts on the same branch / gap.

## Rules

- Created when `/start-session` starts (unless `--no-lock`)
- File naming: `session-{YYYYMMDD-HHMMSS}-{hostname}.lock`
- YAML format — see `.claude/skills/workflow/start-session/reference/context-template.md` §Lock File Schema
- **NOT** committed to git — add to `.gitignore` (per-machine)
- Auto-purged after 4h via `collect-state.sh`

## Lifecycle

1. Session start → create lock
2. Every ~10 turns → update `last_heartbeat`
3. Session end → `/end-session` skill (Phase 2, GAP-193) MOVES lock to
   `.claude/session-locks/archived/YYYY-MM-DD/{lock-name}` for retro analysis
4. Stale (>4h) — `.claude/hooks/session-lock-guard.py` + `collect-state.sh`
   auto-purge (no archive — stale = orphaned crash, low retro value)

## Archival

- Path: `.claude/session-locks/archived/YYYY-MM-DD/`
- Retention: 30 days; then prune. Manual prune:
  `find .claude/session-locks/archived -type d -mtime +30 -exec rm -rf {} +`
- NOT git-committed — `.gitignore` covers `.claude/session-locks/` (parent path).
  Archived locks are local-only retro material; GAP-199 rework-audit reads them
  for context-degradation pattern analysis.

## Phase 2 enforcement (GAP-193, 2026-04-29)

- `.claude/hooks/session-lock-guard.py` — invoked by `audit-gate.py` at merge
  events; exits 1 (block) when current branch is locked by a different
  session id. Auto-purges stale (>4h) locks.
- Session telemetry recorded in `documents/03-planning/pr-logs/PR-{N}.json`
  under key `session_telemetry` (best-effort: `session_id` always;
  `turn_count` + `session_started_at` when detectable).
- `/end-session` skill (`.claude/skills/workflow/end-session/SKILL.md`) —
  explicit lock release with retro summary block appended to lock content.

## MVP Limitations (Phase 1, still applicable)

- Single-machine awareness only (not distributed)
- Real safety for parallel agents = git worktree isolation

## Example Lock

```yaml
session_id: 20260420-143200-wsl-victor
started: 2026-04-20T14:32:00+07:00
branch: feat/wave-8b-E-gap-193-199-session-rework
gaps: [GAP-193, GAP-199]
intent: "Phase 1 session + rework-audit skills"
estimated_turns: 20
last_heartbeat: 2026-04-20T14:55:00+07:00
```

Related: GAP-193, `feedback_parallel_agent_strategy.md` memory.
