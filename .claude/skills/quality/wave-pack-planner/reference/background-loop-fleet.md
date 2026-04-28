# Background Loop Fleet — Documented `/loop` Commands

3 fleet roles user CAN run (not auto-configured). Documented vì `/loop` skill có sẵn, nhưng auto-config hooks là user decision per `update-config` skill warning.

**Per user instruction Q1=A on 2026-04-28 session: doc-only, no auto-config.** Khi user ready, copy-paste invocations dưới.

## Why not auto-config

- Hooks ghi vào `settings.json` = behavioral commitment cross-session
- `/loop` runs cron-like background tasks; tokens/cost compound silently
- User authority decision: enable when ready, disable easily

If user later wants auto-config → use `update-config` skill, NOT this doc.

## Fleet role 1: `doc-sync` (daily)

**Purpose:** Auto-update ROADMAP gap counts, surface stale OPEN gaps (>30d no log entry), flag drift between gap files and ROADMAP queue.

**Cadence:** 1 day (daily morning sweep)

**Invocation:**
```
/loop 1d /doc-sync
```

(User types this into Claude Code; `/loop` skill schedules + re-fires every 1d.)

**What `/doc-sync` does (target skill, separate from this skill):**

1. Count OPEN/PARTIAL/DONE gaps in `documents/04-quality/gaps/GAP-*.md`
2. Compare to ROADMAP `## 🎯 Current Status Snapshot` headline counts
3. Flag any gap file with `mtime > 30 days ago` AND Status = OPEN AND no `## Log` entry in last 30d → "stale OPEN gap"
4. Surface to user as summary block with action items
5. Do NOT auto-edit anything — user decides next step

**Exit conditions:**
- User stops manually (`/loop list` → cancel)
- 5 consecutive runs find zero drift + zero stale → suggest extending cadence to 3d
- User invokes `/doc-sync --once` for ad-hoc check

**Why daily:** ROADMAP drifts within 1-2 days of active wave work; weekly cadence misses too much.

## Fleet role 2: `p3-sweeper` (weekly)

**Purpose:** Pick 3-5 P3 gaps with disjoint files, batch into 1 wave-pack via `/wave-pack-planner` invocation, ship as P3-cleanup wave with parallel agents.

**Cadence:** 1 week (Sunday/Monday batch)

**Invocation:**
```
/loop 7d /p3-sweeper
```

**What `/p3-sweeper` does (separate skill, calls this skill):**

1. `grep -l "Priority.*P3" documents/04-quality/gaps/GAP-*.md | head -20` → P3 candidate list
2. Run `[file-overlap-algorithm.md](file-overlap-algorithm.md)` on first 5 candidates
3. If ≥3 disjoint → propose wave-pack to user (cluster preview)
4. User approves → `/wave-pack-planner` spawns `p3-cleanup-agent.md` × N
5. If <3 disjoint → defer to next week, log "P3 backlog has no disjoint cluster"

**Exit conditions:**
- User stops manually
- 3 consecutive runs find no disjoint cluster → suggest reviewing P3 backlog quality (maybe gaps need re-classification)
- P3 backlog drops below 5 → cadence becomes redundant; pause loop

**Why weekly:** P3 gaps low priority; daily sweep wastes cycles. Weekly batch fits "Sunday cleanup" mental model.

## Fleet role 3: `audit-cadence` (per `post-wave-audit-mandate.md` §4)

**Purpose:** Auto-trigger required audit suite within 3 days post-wave merge, per file-pattern rules.

**Cadence:** Triggered by wave merge events, not pure time-based. Recommended `/loop 1d` polling.

**Invocation:**
```
/loop 1d /audit-cadence
```

**What `/audit-cadence` does (separate skill):**

1. Check `git log main --since='3 days ago'` for merge commits with wave plan reference
2. For each wave merged ≥1 day ago AND <3 days ago, parse the changed files
3. Cross-reference against `audit-gate.py` AUDIT_RULES file patterns
4. For required-but-missing audits, prompt user to run them (or auto-spawn audit agents if pre-approved)
5. Logs audits run / pending to `documents/04-quality/audits/{category}/`

**Exit conditions:**
- User stops manually
- No wave merges in last 7 days → loop is no-op; pause until next wave
- All required audits up to date → silent pass, next run

**Why daily:** 3-day audit window from `post-wave-audit-mandate.md` §4 — daily check ensures audits run on day 1-2, not day 3 panic.

## Summary table

| Loop | Cadence | What | When to stop |
|------|---------|------|--------------|
| `doc-sync` | 1d | ROADMAP drift + stale gap detection | 5 consecutive zero-drift runs |
| `p3-sweeper` | 7d | Cluster + ship P3 backlog | 3 consecutive no-disjoint runs OR P3 backlog <5 |
| `audit-cadence` | 1d | Trigger required post-wave audits | No wave merges in 7d (pause, resume on next wave) |

## Manual cancellation

```
/loop list                    # see active loops
/loop cancel <loop-id>        # stop specific loop
```

## Combined fleet recommendation

Khi user ready full fleet:
```
/loop 1d /doc-sync
/loop 7d /p3-sweeper
/loop 1d /audit-cadence
```

Total background overhead: ~3 fleet runs/day, each ~1-3 min agent time. Token cost compounds — monitor first 1-2 weeks to gauge.

## Related

- [SKILL.md](../SKILL.md) — entry point; this skill IS what `/p3-sweeper` calls
- Skill: `loop` (built-in `/loop` command)
- Skill: `update-config` (if user wants auto-trigger via hooks instead of `/loop`)
- Rule: `.claude/rules/post-wave-audit-mandate.md` §4 (audit cadence basis)
- Skill (target, not yet created): `/doc-sync`, `/p3-sweeper`, `/audit-cadence` — these are SEPARATE skill files user can request
- Memory `feedback_wave_pack_cross_gap_clustering.md` — p3-sweeper rationale (cluster patterns scale to P3 too)
