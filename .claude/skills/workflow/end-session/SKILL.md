---
name: end-session
description: "Dùng khi user nói 'end session', 'đóng session', 'kết thúc session', '/end-session', 'finish work', hoặc trước khi /clear cho session quan trọng. Archive session-lock file vào .claude/session-locks/archived/YYYY-MM-DD/ + output 1-line summary (PRs merged, gaps closed, elapsed time, turn count). Phase 2 của GAP-193."
user-invocable: true
argument-hint: "[--keep-lock] [--summary-only]"
---

# /end-session — Explicit Session Lock Release + Retro Archive

Đóng session hiện tại bằng cách archive lock file để retro analysis (GAP-199 sister), in 1-line summary cho user.

Phase 2 của GAP-193 — pair với `/start-session` (Phase 1).

## When to use

- Trước khi `/clear` cho session đã chạy ≥30 turns hoặc có critical work
- Khi switch sang task khác hoàn toàn (different wave / repo)
- Trước khi handoff cho session khác
- Khi parallel-agent xong task (tự release lock thay vì chờ stale-purge 4h)

## Process

### Step 1 — Resolve session id + active lock

```bash
SESSION_ID="${CLAUDE_SESSION_ID:-$(whoami)@$(hostname):ppid-$$}"
LOCK_DIR=".claude/session-locks"
```

Tìm lock file matching `session_id: $SESSION_ID`. Nếu không có lock → output "no active lock for this session" và exit (vẫn in summary).

### Step 2 — Build summary

Best-effort gather:

| Field | Source |
|-------|--------|
| Branch | `git branch --show-current` |
| PRs merged this session | `git log --since="$started" --grep="Merge pull request" main --oneline` (hoặc parse PR-{N}.json events) |
| Gaps touched | `git log --since="$started" --name-only -- documents/04-quality/gaps/ \| grep GAP-` |
| Turn count | `$CLAUDE_TURN_COUNT` nếu có |
| Elapsed | `now() - lock.started` |

### Step 3 — Archive the lock

```bash
DATE=$(date +%Y-%m-%d)
ARCHIVE_DIR="$LOCK_DIR/archived/$DATE"
mkdir -p "$ARCHIVE_DIR"
```

Append summary block vào cuối lock content (preserve original YAML), then `mv` vào `$ARCHIVE_DIR/`.

Skip archive nếu `--keep-lock` (rare — chỉ khi user muốn manual cleanup sau).

### Step 4 — Output 1-line summary (Vietnamese, per CLAUDE.md §CRITICAL)

```
✓ Session {SID} archived → {archive-path}. {N} PRs merged, {M} gaps touched, {turns} turns, {elapsed} elapsed.
```

Nếu thiếu data → omit field gracefully.

## Helper script

Có thể dùng `scripts/end-session.sh` (kèm theo skill này) để one-shot — nhưng skill có thể compose từ shell commands trực tiếp nếu cần.

## Rules

- **TUYỆT ĐỐI tiếng Việt** trong output user-facing — per `CLAUDE.md` §CRITICAL
- Archive directory `.claude/session-locks/archived/` đã được `.gitignore` cover (parent path) — verify bằng `git check-ignore .claude/session-locks/archived/foo.lock`
- Retention: archived locks giữ 30 ngày, sau đó prune. Manual prune: `find .claude/session-locks/archived -type d -mtime +30 -exec rm -rf {} +`
- Nếu lock file không tồn tại (đã stale-purged hoặc never created với `--no-lock`) → KHÔNG báo lỗi, chỉ in summary

## Gotchas

- `$CLAUDE_TURN_COUNT` chưa expose qua harness mặc định — best-effort, omit nếu không có
- `git log --since` cần ISO date format; lock `started:` field phải parse đúng
- WSL2: `hostname` có thể trả về khác nhau giữa shells — luôn dùng `$CLAUDE_SESSION_ID` nếu set
- Không archive nếu lock file đã bị session khác overwrite (race condition rare) — detect bằng compare session_id sau khi đọc
- Archive directory path phải RELATIVE từ repo root — agent worktree paths khác nhau

## Skill contents

- `SKILL.md` — this file
- `scripts/end-session.sh` — helper one-shot (optional convenience)

## Related

- `/start-session` (sister Phase 1) — `.claude/skills/workflow/start-session/SKILL.md`
- `.claude/hooks/session-lock-guard.py` — Phase 2 enforcement that this skill complements
- `.claude/session-locks/README.md` — lock convention + retention policy
- GAP-193 — parent gap (Phase 1 + Phase 2 closed by Wave Meta Phase-2 Cleanup)
- GAP-199 — rework audit (consumes archived locks for retrospective analysis)
- Memory: `feedback_parallel_agent_strategy.md`
