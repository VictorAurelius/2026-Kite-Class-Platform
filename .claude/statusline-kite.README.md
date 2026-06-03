# Statusline — Kite

Custom Claude Code statusline (model · context bar · tokens · cost · rate-limits).

## Output preview

```
Opus 4.7 (1M context) [██░░░░░░░░] 27% 273.9k/1000.0k $97.9598 5h:42% 7d:18%
```

Segments:
- **Model** — `.model.display_name` (cyan)
- **Context bar + %** — 10-segment bar, colored by `used_perc`: <70 green, 70-89 yellow, ≥90 red
- **Tokens used/total** — derived from last `usage` record in `transcript_path` JSONL (`input + cache_creation + cache_read`); `1m` model id → 1M total, else 200k
- **Cost** — `.cost.total_cost_usd`
- **5h / 7d** — `.rate_limits.{five_hour,seven_day}.used_percentage` (color-coded same threshold). Segment skipped if both absent — backward-compat with older Claude Code releases that don't populate the field.

## Install for Claude Code

Two options:

### Option A — symlink (recommended; auto-syncs with repo updates)

```bash
ln -sfn "$(pwd)/.claude/statusline-kite.sh" ~/.claude/statusline-kite.sh
```

Then in `~/.claude/settings.json`:
```json
{
  "statusLine": {
    "type": "command",
    "command": "bash ~/.claude/statusline-kite.sh"
  }
}
```

### Option B — copy (no auto-sync)

```bash
cp .claude/statusline-kite.sh ~/.claude/
chmod +x ~/.claude/statusline-kite.sh
```

Then same `settings.json` snippet as Option A.

## Required tools

`jq`, `awk`, `tac` — standard on Linux/macOS. WSL2 ✅.

## Why the transcript-derived token count?

Claude Code's statusline JSON payload does NOT carry context-window usage natively (only `cost.total_cost_usd`, `model.*`, `transcript_path`, etc.). Reading the last `usage` record from `transcript_path` is the most reliable signal — matches what the API actually sent.

## Backward-compat with older Claude Code

- If `rate_limits` field absent → 5h/7d segment is silently omitted (no broken output)
- If `transcript_path` empty/unreadable → falls back to 0%/0 tokens (bar empty but still renders)

## Cost

Statusline runs as a subprocess outside Claude's context window. Zero token cost per turn — just local CPU/IO (~10ms).

## Update flow

1. Edit `.claude/statusline-kite.sh` in the repo
2. PR + merge
3. On each environment: if Option A symlink → done automatically. If Option B copy → re-run `cp`.
