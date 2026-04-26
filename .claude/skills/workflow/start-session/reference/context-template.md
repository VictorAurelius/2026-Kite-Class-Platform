# Context Template — Output Format + Lock Schema

Reference cho `/start-session` skill. Đọc khi format output HOẶC manage locks.

**Quy tắc ngôn ngữ:** Field labels + prose phải tiếng Việt per `CLAUDE.md` §CRITICAL Communication Language + GAP-207. Giữ English cho: technical terms (CI, CVE, PR, gap, wave, branch, main, merge — loanwords trong project context), file paths, command output, code.

## 1. Summary Block Format

Target: single fenced markdown block, ≤15 dòng. Skip field nào empty.

```
## Ngữ cảnh session ({ISO_TIMESTAMP})
**Wave:** {active wave ID + mô tả ngắn}
**Nhánh:** {current git branch} ({clean|dirty|ahead-of-origin})
**PRs đang mở:** {count} ({top 3 #ids với 1-word status})
**CI:** main {green|red|unknown} — {N} failure gần đây
**MCP servers:** {connected}/{total}{ — FAILED: {list}}
**Gaps blocker:** {top 3 P0/P1 gap IDs + 1-từ topic}
**Sức khỏe context:** {fresh|warm|degraded} (lượt ~{N}, lần compact gần nhất {time})
**Session locks đang active:** {N} ({list nếu conflict với intended work})
**Đề xuất tiếp theo:** {1-line action — /continue | /gap-triage | etc.}
```

> **MCP row** (added 2026-04-26 sau Wave 6 anti-pattern): nếu báo failed servers hoặc 0/N connected → suggest user fix trước critical work. GitHub MCP thiếu → mọi PR/merge/check ops fallback `gh` CLI per `.claude/rules/mcp-first-with-fallback.md` §3 (vẫn work nhưng tốn parsing layer + bỏ qua structured output advantage). Khi có failed → nêu rõ tên server thay vì im lặng fallback.

### Vocabulary status

| Field | Values |
|-------|--------|
| nhánh | `clean` / `dirty` / `ahead` / `behind` / `diverged` |
| CI | `green` / `red` / `pending` / `unknown` |
| sức khỏe context | `fresh` (<20 lượt) / `warm` (20-40) / `degraded` (>40 hoặc >2h kể từ compact) |
| PR status | `review` / `draft` / `ci-fail` / `approved` / `conflicts` |
| MCP servers | `N/M` connected (vd `1/1` clean, `0/1 — FAILED: github` cần fix) |

## 2. Lock File Schema

**Path:** `.claude/session-locks/session-{YYYYMMDD-HHMMSS}-{hostname}.lock`

**Format:** YAML (đọc được trong editor, parse được với `yq`).

```yaml
session_id: 20260420-143200-wsl-victor
started: 2026-04-20T14:32:00+07:00
hostname: wsl-victor
pid: 12345                    # optional, cho stale-lock cleanup
branch: feat/wave-8b-E-gap-193-199-session-rework
worktree: /home/user/projects/repo/.claude/worktrees/agent-a8fc490c
gaps:
  - GAP-193
  - GAP-199
intent: |
  Closing GAP-193 + GAP-199 Phase 1 — session orchestration + rework audit skills.
estimated_turns: 20
last_heartbeat: 2026-04-20T14:55:00+07:00   # update mỗi ~10 lượt
```

**Required fields:** `session_id`, `started`, `branch`, `gaps` (có thể empty list).
**Optional:** `pid`, `worktree`, `intent`, `estimated_turns`, `last_heartbeat`.

## 3. Overlap Detection

Khi `/start-session` chạy, scan existing locks:

| Điều kiện | Action |
|-----------|--------|
| Lock khác có cùng `branch` | **BLOCK mặc định** — warn user, hỏi confirm |
| Lock khác có overlapping `gaps` | **WARN** — show intent của session khác, để user quyết |
| Lock khác share `worktree` path | **BLOCK** — worktree single-session only |
| Lock cũ hơn 4h (no heartbeat update) | **Auto-purge** — stale, remove file |
| Không overlap | Tạo own lock, proceed |

## 4. Ví dụ Output

### 4.1 Fresh session, repo green
```
## Ngữ cảnh session (2026-04-20 09:00)
**Wave:** 8b meta cluster (6 agents planned, 0 in flight)
**Nhánh:** main (clean, up-to-date)
**PRs đang mở:** 0
**CI:** main green
**MCP servers:** 1/1 connected (github ✓)
**Gaps blocker:** GAP-193 (session skill), GAP-199 (rework audit), GAP-185 (persona AC)
**Sức khỏe context:** fresh (lượt 1)
**Đề xuất tiếp theo:** /continue hoặc pick Wave 8b agent task
```

### 4.2 Mid-wave, 3 parallel agents, 1 CI red
```
## Ngữ cảnh session (2026-04-20 15:40)
**Wave:** 8b meta cluster (6 agents in flight)
**Nhánh:** main (clean)
**PRs đang mở:** 4 (#395 approved, #396 ci-fail, #397 review, #398 draft)
**CI:** main green, #396 red (E2E flake)
**Gaps blocker:** GAP-199 (rework audit), GAP-196 (skill index stale)
**Sức khỏe context:** warm (lượt ~25)
**Session locks đang active:** 3 (agent-B trên wave-8b-B, agent-C trên wave-8b-C — không overlap với intent)
**Đề xuất tiếp theo:** /fix-pr 396 để unblock HOẶC tiếp tục agent task
```

### 4.3 Degraded session, user cần clear
```
## Ngữ cảnh session (2026-04-20 22:10)
**Wave:** 8b meta cluster
**Nhánh:** feat/wave-8b-X (dirty, uncommitted)
**PRs đang mở:** 2
**CI:** main green
**MCP servers:** 0/1 — FAILED: github (Docker daemon down?)
**Sức khỏe context:** DEGRADED (lượt ~55, last compact 3h trước, quality drift likely)
**Đề xuất tiếp theo:** Fix MCP (`docker ps` rồi `claude mcp list`) + commit WIP + /clear + re-run /start-session TRƯỚC critical merge
```

## 5. Minimal Output Mode (`--quick`)

Single line:

```
Wave 8b · main clean · 4 PRs (1 red) · 3 blockers · fresh
```

Dùng khi user chỉ cần sanity check, không phải full digest.

## 6. Integration với skills khác

- `/continue` đọc summary để pick top priority — nếu degraded, `/continue` phải abort with warning
- `/repo-status` cho richer signal; `/start-session` nhanh hơn cho routine entry
- `/rework-audit` consumes lock history + context-health log để detect PR nào built dưới degraded conditions

## 7. Anti-patterns

| ❌ Đừng | ✅ Nên |
|---------|------|
| Dump full CLAUDE.md vào session context | Digest — chỉ header rules |
| List tất cả 150+ gaps trong blockers | Top 3 P0/P1 only |
| Skip lock check vì "solo session" | Check kể cả solo — cheap, future-proof |
| Để stale locks | Auto-purge >4h mỗi run |
| Treat lock as hard enforcement | Hint thôi — git worktree isolation mới là barrier thật |
