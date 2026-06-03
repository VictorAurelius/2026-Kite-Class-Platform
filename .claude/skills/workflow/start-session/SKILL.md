---
name: start-session
description: "Dùng khi user nói 'start new session', 'bắt đầu session mới', '/start-session', 'what's the state?', 'tình trạng hiện tại', hoặc khi cần load context sau khi /clear. Load: CLAUDE.md digest, current wave, open PRs, failing CI, active blockers. Output: single summary block. Cũng check session-lock conflicts khi chạy parallel agents."
user-invocable: true
argument-hint: "[--quick] [--no-lock] [--no-aws|--refresh-aws] [--audit-snapshot]"
---

# /start-session — Session Orchestration

Prepare fresh session bằng cách load đúng context + detect conflicts với concurrent sessions.

## When to use

- Bắt đầu conversation mới trong repo này
- Sau khi `/clear` hoặc `/compact`
- Trước critical work (wave merge, audit, release)
- Khi unsure "làm gì tiếp?" → chạy skill này thay vì đọc mò

## ⚠️ CURRENT MODE — Flow Verification Campaign (2026-06-04→)

**Active:** `documents/03-planning/roadmap/flow-verification-campaign.md` — thông toàn bộ ~22 user-facing flow KH+KC cho Phase 1 BETA TRƯỚC, rồi mới quay lại fix-gap-theo-wave.

**Override default "Recommended next":** khi mode active, KHÔNG recommend "pick P0/blocker gap để fix". Thay vào đó:
1. Đọc campaign §4 table → flow chưa thông (⬜) kế tiếp theo §3 dependency order (KH-2→KH-1→KH-3→KC-1→KC-2→KC-3→KC-4→{KC-5/6/7}→{KC-8/9}).
2. Point tới wave plan flow đó (tạo lazy nếu chưa, campaign §5).
3. Loop per `feature-ship-runtime-walk-mandate` §3.4 — chỉ fix **blocker do walk lòi ra**; gap non-flow-blocking defer.
4. Flow THÔNG = 3 gate: G1 agent walk + G2 human local test + G3 production-parity (campaign §1).

Gỡ mode khi campaign §4 all ✅ → quay lại wave-based gap-fix.

## Process

### Step 1 — Run state collector

```bash
./.claude/skills/workflow/start-session/scripts/collect-state.sh
```

Script output: active branch, open PRs count, failing CI runs, top 3 blocker gaps, current wave, last /compact timestamp (if logged).

### Step 2 — Read digest sources (CONTEXT-AWARE — minimize path-trigger rules)

**Fix 2026-05-21 context bloat:** mỗi file read vào `documents/04-quality/gaps/**` HOẶC `documents/03-planning/**` HOẶC `documents/**` generic kích auto-load 10+ path-scoped rules (~150-240k bytes per session). Default = CSV/script queries thay vì narrative reads.

Default flow (skip file reads):
1. **Wave / blockers / Phase 1 BETA P0:** đã có trong collect-state.sh output. Không cần read thêm.
2. **Detail gap inspection:** `bash scripts/query-gaps.sh <GAP-prefix>` thay vì Read gap file
3. **Recent waves:** `tail -3 .claude/skills/quality/wave-pack-planner/data/wave-history.jsonl | jq -r '.wave + " — " + .outcome'`

Chỉ Read files khi user explicit ask OR script output không đủ cho current task:
1. `CLAUDE.md` — auto-load mọi session, KHÔNG cần Read lại
2. `documents/03-planning/waves/<specific-wave>.md` — chỉ khi cần plan detail (touches `documents/03-planning/waves/**` path-scoped rules)
3. `documents/04-quality/gaps/ROADMAP.md` — **AVOID** (triggers ~10 gap-* rules); query CSV instead
4. `documents/03-planning/MASTER-GAPS-FIX-PLAN.md` — chỉ khi cần multi-quarter horizon

KHÔNG đọc toàn bộ ROADMAP/plan — chỉ header + current section khi unavoidable.

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
| **AWS Phase 1 BETA** | Tier 1 read-only commands per `.claude/rules/agent-aws-access.md` §2.1: `sts get-caller-identity` / `ec2 describe-instances` / `rds describe-db-instances` / `elbv2 describe-load-balancers` / `cloudtrail describe-trails` + `get-trail-status` / `cloudwatch describe-alarms --state-value ALARM`. Cached 30 min in `.claude/session-aws-cache/snapshot.json` (gitignored). Added 2026-05-09 after user-flagged miss "session report thiếu trạng thái AWS" — Phase 1 BETA stack on account 906286017800 (ap-southeast-1) needs visibility at session start so drift / alarms / EC2 stop don't surprise mid-wave. |

Ví dụ output (tiếng Việt per CLAUDE.md §CRITICAL — GAP-207):

```
## Ngữ cảnh session (2026-05-09 09:30)
**Wave:** Wave 48 SHIPPED — chưa có wave kế hoạch tiếp theo (theo ROADMAP §Next Action)
**Nhánh:** main (clean) / worktrees: 0
**Mức repo:** GREEN (CI green, 0 CVE, 0 branches cũ, 0 audit P0)
**PRs đang mở:** 0
**MCP servers:** 1/1 connected (github)
**AWS Phase 1 BETA (cache 5m):**
  · Account/Region: 906286017800 / ap-southeast-1
  · EC2: 2 running, 0 stopped (kh_backend=running, kc_backend=running)
  · RDS: 1 — kite-postgres-prod=available
  · ALB: 1 — kite-alb=active
  · CloudTrail: kitehub-main=IsLogging:True
  · Alarms ALARM: 0
**Gaps blocker (top 6):** GAP-453, GAP-223, GAP-222a, GAP-016, GAP-011, GAP-014
**Sức khỏe context:** fresh session
**Đề xuất tiếp theo:** GAP-453 (PR-time E2E gate, B.2 narrow-subset) hoặc /repo-status để triage CVE
```

### Cờ collect-state.sh

| Cờ | Tác dụng |
|---|---|
| `--quick` | One-line summary; AWS thu gọn còn `EC2-running / alarms` |
| `--json` | Structured JSON cho hooks/agents khác consume |
| `--no-aws` | Skip phần AWS hoàn toàn (offline / no-creds / không quan tâm) |
| `--refresh-aws` | Bỏ qua cache 30m, gọi AWS thật ngay |
| `--audit-snapshot` | Ghi `documents/04-quality/audits/aws-verification/<date>-session-start-snapshot.md` per `agent-aws-access.md` §5 |
| `--no-lock` | Bỏ qua session-lock check / create |

**KHÔNG** infer wave từ `ls -t` mtime hoặc blockers alphabetical grep (bugs cũ pre-GAP-206). Luôn parse `ROADMAP.md`.

**KHÔNG** output bằng English — vi phạm CLAUDE.md §CRITICAL. Field labels + prose phải tiếng Việt. Giữ English cho: technical terms (CI, CVE, PR, gap, wave, branch — đã là loanwords trong context project), file paths, command output, code.

### Step 4.5 — Parallel-eligibility hint (BẮT BUỘC sau khi xác định "Đề xuất tiếp theo")

**Mỗi khi đề xuất next action**, scan xem nó có wave-eligible không. Nếu yes → nhắc user dùng wave + parallel agents thay vì serial PRs.

**Heuristic — gap/feature là wave-eligible khi:**

1. Có ≥3 sub-tasks được phân chia rõ (Phase 1/2/3, Sub-PR a/b/c, Layer 1/2/3...)
2. Files mỗi sub-task touch là disjoint (không share `application.yml`, không share migration version, không share service class)
3. Mỗi sub-task self-contained TDD/build cycle (không cần wait sub-task khác xong)

**Heuristic — KHÔNG wave-eligible khi:**

- Chỉ 1-2 sub-tasks
- Sub-tasks share migration version slot (V_n và V_n+1 collide nếu chạy parallel)
- Sub-tasks share `application.yml` (rebases sẽ phải sequential)
- Foundation chưa ship (cần PR-0 ship interfaces/shared lib trước, sau đó parallel)

**Nếu wave-eligible → output line:**

```
**Wave-eligible:** YES — 5 sub-tasks disjoint files. Đề xuất: tạo wave plan + spawn 4-5 agents parallel (`isolation:worktree`, max 5).
Reference: `feedback_parallel_agent_strategy.md` + `feedback_wave_plan_before_serial_prs.md` + skill `quality-plan` / `priority-pr-planning` / `gap-to-pr-converter`.
```

**Nếu KHÔNG wave-eligible → output line:**

```
**Wave-eligible:** NO — {lý do, e.g. "shared application.yml", "1 sub-task only"}. Tiếp tục serial PR là OK.
```

**Anti-pattern (phát hiện 2026-04-26 GAP-229):** Pick gap có "Phase 1 / Phase 2 / Phase 3" rồi nhảy thẳng Phase 1 trên branch riêng, ship serial PRs liên tiếp. ~90min serial vs ~30min nếu parallel. Memory `feedback_wave_plan_before_serial_prs.md` chứa case study + checklist 3-câu.

### Step 5 — Handoff to next skill

User decides:
- **Wave plan** (nếu Step 4.5 báo YES) — ưu tiên cao nhất; dùng `quality-plan` HOẶC viết wave plan ngắn (5-10 dòng) rồi single-message multi-Agent dispatch
- `/continue` — execute top priority from plan (single sub-task scope)
- `/session-docs-check` — Living Docs completeness gate (chạy trước commit/PR/merge để bắt status flip thiếu, ROADMAP entry thiếu, skill index chưa update)
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
- **Step 4.5 BẮT BUỘC** — luôn output line "Wave-eligible: YES/NO" sau "Đề xuất tiếp theo". Anti-pattern bắt nguồn 2026-04-26 (GAP-229 3 phases serial = ~90min vs ~30min nếu parallel). Per memory `feedback_wave_plan_before_serial_prs.md` + `feedback_parallel_agent_strategy.md`.
- **AWS section trong summary**: nếu `AWS Phase 1 BETA` line báo `no-auth`, `no-cli`, hoặc bất kỳ alarm ALARM > 0, PHẢI surface trong "Đề xuất tiếp theo" (vd: "Triage alarm X trước infra work" hoặc "Fix AWS auth trước deploy work"). Anti-pattern: silently swallow AWS state, ship infra change rồi mới phát hiện EC2 đã stopped sáng nay. Per `.claude/rules/agent-aws-access.md` §1 + `aws-observability-first.md`.

## Gotchas

- `collect-state.sh` needs `gh` CLI authenticated — fall back to manual `gh pr list` nếu script fails
- `.claude/session-locks/` KHÔNG commit (add to `.gitignore` nếu chưa có)
- Stale locks (>4h) auto-purged — nhưng crash mid-session có thể leave orphan; `--no-lock` hoặc manual `rm` clear
- Hostname trên WSL = `wsl-name`; multiple Claude Code windows cùng host vẫn tạo unique lock (timestamp differs)
- KHÔNG rely solely trên lock để prevent conflict — lock là hint, git branch isolation (worktree) mới là enforcement thật
- **MCP transient connect failure**: `claude mcp list` có thể báo `✗ Failed` rồi retry trong cùng session báo `✓ Connected` — lý do thường là Docker daemon vừa khởi động hoặc image đang pull. Khi gặp failed: `docker ps` (verify daemon) → `docker pull ghcr.io/github/github-mcp-server` → `claude mcp list` lại. Nếu vẫn failed, báo user, fallback CLI per `mcp-first-with-fallback.md` §3.
- **AWS auth states**: `no-cli` (CLI chưa cài), `no-auth` (sts get-caller-identity fail — kiểm `aws configure list` + key valid), `cached` (đọc snapshot 30m), `fresh` (vừa fetch lại), `skipped` (`--no-aws`). Khi `no-auth` xuất hiện trên Phase 1 BETA branch, cảnh báo user trước khi làm bất kỳ infra/deploy work — drift visibility = 0.
- **AWS cost**: Tier 1 describe/list calls đều free-tier within reason (vài chục ngàn requests/month per service). Cache 30m + opt-out `--no-aws` cover heavy session pattern. KHÔNG add `aws cloudtrail get-trail-status` data-events query — đó là Tier 2 + tốn `$0.10/100k` (per `agent-aws-access.md` §3).
- **AWS section bypasses agent-aws-access.md §5 logging by default**: theo §5.3 single ad-hoc no-log; multi-command verification logs only when `--audit-snapshot`. Cache file is gitignored, contains no secret material (Tier 1 commands return metadata only, never `get-secret-value`).
- **AWS region**: lấy từ `aws configure get region` (default profile) → fallback `$AWS_REGION` env → `?`. Phase 1 BETA dùng `ap-southeast-1`; nếu profile shifted region, snapshot sẽ phản ánh region đó (KHÔNG cross-region multi-call để tiết kiệm time).

## Skill contents

- `SKILL.md` — this file
- `reference/context-template.md` — output format + lock schema + example
- `scripts/collect-state.sh` — state collector (full / `--quick` / `--json` modes; AWS flags: `--no-aws` / `--refresh-aws` / `--audit-snapshot`)
- `data/` — session history log (optional, append-only)
- `.claude/session-aws-cache/snapshot.json` — AWS Tier 1 snapshot, 30m TTL, gitignored (created lazily on first AWS-enabled run)

## Related

- `.claude/skills/workflow/continue/SKILL.md` — use after /start-session to act on top priority
- `.claude/skills/workflow/repo-status/SKILL.md` — deeper remote health
- `.claude/skills/quality/rework-audit/SKILL.md` — consumes degradation signal for retrospective audit
- Rule: `.claude/rules/output-review-mandate.md` (sessions produce outputs)
- Rule: `.claude/rules/agent-aws-access.md` (Tier 1 read-only commands powering AWS section; `--audit-snapshot` artifact format)
- Rule: `.claude/rules/aws-observability-first.md` (CloudTrail IsLogging line in AWS section closes the audit-baseline gap surfaced by this rule)
- Memory: `feedback_parallel_agent_strategy.md` (multi-agent safety)
