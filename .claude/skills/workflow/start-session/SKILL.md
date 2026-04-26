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

Format theo `reference/context-template.md`. Data sources (from `collect-state.sh`):

| Field | Source |
|-------|--------|
| Wave | `ROADMAP.md` "Next recommended wave" line (per GAP-206 fix 2026-04-24) |
| Blocker gaps | `ROADMAP.md` "GA Blockers remaining" table (per GAP-206) |
| Repo level | `scripts/repo-status.sh --json` → `.level` (4 factors) |
| CVE / stale branches | Same `--json` → `.security`, `.branches` |
| Recent merges | `git log main --since='3 days ago' --oneline` (last 5) |
| Branch state | `git diff --name-only`; `documents/action-2.md` alone = "scratchpad only" |
| **MCP servers** | `claude mcp list` → connected/total + failed names. Added 2026-04-26 after Wave 6 anti-pattern: session defaulted `gh` CLI all wave because MCP availability never checked. Per `.claude/rules/mcp-first-with-fallback.md` §3 must verify at session start. |

Ví dụ output (tiếng Việt per CLAUDE.md §CRITICAL — GAP-207):

```
## Ngữ cảnh session (2026-04-24 04:45)
**Wave:** Wave 5 — GAP-047 document generation (theo ROADMAP §Next recommended wave)
**Nhánh:** main (clean, scratchpad: action-2.md) / worktrees: 0
**Mức repo:** GREEN (CI green, 0 CVE, 0 branches cũ, 0 audit P0)
**PRs đang mở:** 0
**Gaps blocker (top 6):** GAP-047, GAP-046, GAP-016, GAP-011, GAP-014, GAP-005
**Merges gần đây (3 ngày):** #468 start-session accuracy, #467 solo-dev CI, #466 Dependabot guide, #465 CI retention, #464 size limit
**Sức khỏe context:** fresh session
**Đề xuất tiếp theo:** bắt đầu Wave 5 sub-PR 5.1 (PDF+Excel doc generation)
```

**KHÔNG** infer wave từ `ls -t` mtime hoặc blockers alphabetical grep (bugs cũ pre-GAP-206). Luôn parse `ROADMAP.md`.

**KHÔNG** output bằng English — vi phạm CLAUDE.md §CRITICAL. Field labels + prose phải tiếng Việt. Giữ English cho: technical terms (CI, CVE, PR, gap, wave, branch — đã là loanwords trong context project), file paths, command output, code.

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

## Rules

- **TUYỆT ĐỐI giao tiếp bằng tiếng Việt** per `CLAUDE.md` §CRITICAL Communication Language. Field labels, prose, recommendations trong output — tất cả tiếng Việt. Chỉ giữ English cho: technical terms (CI, CVE, PR, gap, wave, branch, main, merge — đã là loanwords trong project context), file paths, command output, code.
- LUÔN chạy `collect-state.sh` trước — không tự suy diễn status
- Nếu script fail (gh unauthed, hook missing) → báo rõ, không đoán
- Wave + blockers LUÔN parse từ `ROADMAP.md`, không dùng `ls -t` mtime hoặc alphabetical grep (pre-GAP-206 bugs)
- Output format tuân `reference/context-template.md` (có ví dụ VN trong Step 4)
- **MCP status PHẢI nêu trong summary** — nếu line "MCP servers" báo failed servers hoặc 0/N, suggest user fix trước critical work (GitHub MCP cần cho merge/PR ops); per `.claude/rules/mcp-first-with-fallback.md` §3 default sang CLI fallback nhưng phải biết để swap khi fix xong. Anti-pattern bắt nguồn 2026-04-26 (Wave 6 session default `gh` CLI suốt 4 sub-PR vì không check MCP đầu session).

## Gotchas

- `collect-state.sh` needs `gh` CLI authenticated — fall back to manual `gh pr list` nếu script fails
- `.claude/session-locks/` KHÔNG commit (add to `.gitignore` nếu chưa có)
- Stale locks (>4h) auto-purged — nhưng crash mid-session có thể leave orphan; `--no-lock` hoặc manual `rm` clear
- Hostname trên WSL = `wsl-name`; multiple Claude Code windows cùng host vẫn tạo unique lock (timestamp differs)
- KHÔNG rely solely trên lock để prevent conflict — lock là hint, git branch isolation (worktree) mới là enforcement thật
- **MCP transient connect failure**: `claude mcp list` có thể báo `✗ Failed` rồi retry trong cùng session báo `✓ Connected` — lý do thường là Docker daemon vừa khởi động hoặc image đang pull. Khi gặp failed: `docker ps` (verify daemon) → `docker pull ghcr.io/github/github-mcp-server` → `claude mcp list` lại. Nếu vẫn failed, báo user, fallback CLI per `mcp-first-with-fallback.md` §3.

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
