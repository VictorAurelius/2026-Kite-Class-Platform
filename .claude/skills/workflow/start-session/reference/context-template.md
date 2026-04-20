# Context Template — Output Format + Lock Schema

Reference for `/start-session` skill. Read when formatting output OR managing locks.

## 1. Summary Block Format

Target: single fenced markdown block, ≤15 lines. Skip fields that are empty.

```
## Session Context ({ISO_TIMESTAMP})
**Wave:** {active wave ID + short description}
**Branch:** {current git branch} ({clean|dirty|ahead-of-origin})
**Open PRs:** {count} ({top 3 #ids with 1-word status})
**CI:** main {green|red|unknown} — {N} recent failures
**Blocker gaps:** {top 3 P0/P1 gap IDs + 1-word topic}
**Context health:** {fresh|warm|degraded} (turn ~{N}, last compact {time})
**Active session locks:** {N} ({list if conflict with intended work})
**Recommended next:** {one-line action — /continue | /gap-triage | etc.}
```

### Status vocabulary

| Field | Values |
|-------|--------|
| branch | `clean` / `dirty` / `ahead` / `behind` / `diverged` |
| CI | `green` / `red` / `pending` / `unknown` |
| context health | `fresh` (<20 turns) / `warm` (20-40) / `degraded` (>40 or >2h since compact) |
| PR status | `review` / `draft` / `ci-fail` / `approved` / `conflicts` |

## 2. Lock File Schema

**Path:** `.claude/session-locks/session-{YYYYMMDD-HHMMSS}-{hostname}.lock`

**Format:** YAML (readable in editors, parseable with `yq`).

```yaml
session_id: 20260420-143200-wsl-victor
started: 2026-04-20T14:32:00+07:00
hostname: wsl-victor
pid: 12345                    # optional, for stale-lock cleanup
branch: feat/wave-8b-E-gap-193-199-session-rework
worktree: /home/user/projects/repo/.claude/worktrees/agent-a8fc490c
gaps:
  - GAP-193
  - GAP-199
intent: |
  Closing GAP-193 + GAP-199 Phase 1 — session orchestration + rework audit skills.
estimated_turns: 20
last_heartbeat: 2026-04-20T14:55:00+07:00   # update every ~10 turns
```

**Required fields:** `session_id`, `started`, `branch`, `gaps` (can be empty list).
**Optional:** `pid`, `worktree`, `intent`, `estimated_turns`, `last_heartbeat`.

## 3. Overlap Detection

When `/start-session` runs, scan existing locks:

| Condition | Action |
|-----------|--------|
| Another lock has same `branch` | **BLOCK by default** — warn user, ask confirm |
| Another lock has overlapping `gaps` | **WARN** — show other session's intent, let user decide |
| Another lock shares `worktree` path | **BLOCK** — worktree single-session only |
| Lock older than 4h (no heartbeat update) | **Auto-purge** — stale, remove file |
| No overlap | Create own lock, proceed |

## 4. Example Output Scenarios

### 4.1 Fresh session, green repo
```
## Session Context (2026-04-20 09:00)
**Wave:** 8b meta cluster (6 agents planned, 0 in flight yet)
**Branch:** main (clean, up-to-date)
**Open PRs:** 0
**CI:** main green
**Blocker gaps:** GAP-193 (session skill), GAP-199 (rework audit), GAP-185 (persona AC)
**Context health:** fresh (turn 1)
**Recommended next:** /continue or pick Wave 8b agent task
```

### 4.2 Mid-wave, 3 parallel agents, 1 CI red
```
## Session Context (2026-04-20 15:40)
**Wave:** 8b meta cluster (6 agents in flight)
**Branch:** main (clean)
**Open PRs:** 4 (#395 approved, #396 ci-fail, #397 review, #398 draft)
**CI:** main green, #396 red (E2E flake)
**Blocker gaps:** GAP-199 (rework audit), GAP-196 (skill index stale)
**Context health:** warm (turn ~25)
**Active session locks:** 3 (agent-B on wave-8b-B, agent-C on wave-8b-C — no overlap with your intent)
**Recommended next:** /fix-pr 396 to unblock OR continue your agent task
```

### 4.3 Degraded session, user needs to clear
```
## Session Context (2026-04-20 22:10)
**Wave:** 8b meta cluster
**Branch:** feat/wave-8b-X (dirty, uncommitted)
**Open PRs:** 2
**CI:** main green
**Context health:** DEGRADED (turn ~55, last compact 3h ago, quality drift likely)
**Recommended next:** Commit WIP, /clear, re-run /start-session BEFORE critical merge
```

## 5. Minimal Output Mode (`--quick`)

Single line:

```
Wave 8b · main clean · 4 PRs (1 red) · 3 blockers · fresh
```

Use when user just wants sanity check, not full digest.

## 6. Integration with other skills

- `/continue` reads summary to pick top priority — if degraded, `/continue` should abort with warning
- `/repo-status` provides richer signal; `/start-session` is faster for routine entry
- `/rework-audit` consumes the lock history + context-health log to detect which past PRs were built under degraded conditions

## 7. Anti-patterns

| ❌ Don't | ✅ Do |
|---------|------|
| Dump full CLAUDE.md into session context | Digest — header rules only |
| List all 150+ gaps in blockers | Top 3 P0/P1 only |
| Skip lock check because "solo session" | Check even if solo — cheap, future-proof |
| Leave stale locks | Auto-purge >4h on each run |
| Treat lock as hard enforcement | It's a hint — git worktree isolation is real barrier |
