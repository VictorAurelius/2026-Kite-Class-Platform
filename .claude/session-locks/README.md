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
3. Session end (explicit or stale) → delete lock

## MVP Limitations

- Advisory only — no hook enforcement yet (Phase 2, post-pilot)
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
