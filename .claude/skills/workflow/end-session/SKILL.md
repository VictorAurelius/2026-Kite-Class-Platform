---
name: end-session
description: "Dùng khi user nói 'end session', 'đóng session', 'kết thúc session', '/end-session', 'finish work', 'wrap up', hoặc trước khi /clear cho session quan trọng. Working-tree clean + sync gate (Step 0a: no dirty/uncommitted, main synced OR in-flight branch pushed+handed-off) + docs-sync 5-target (CSV/ROADMAP/wave-history/MEMORY/session-handoff per session-end-context-check.md §4.5) + auto-write session-handoff note + archive session-lock + 1-line summary. Symmetric counterpart cho /start-session."
user-invocable: true
argument-hint: "[--check-only] [--allow-dirty] [--keep-lock] [--summary-only] [--skip-handoff] [--no-pr]"
---

# /end-session — Docs-Sync + Handoff + Lock Release

Đóng session hiện tại: docs-sync 5-target verify → auto-write handoff note → archive lock → 1-line summary. Symmetric counterpart cho `/start-session` (Phase 1).

Đảm bảo next session pickup clean state (không miss wave/gap/memory drift).

## When to use

- Trước khi `/clear` cho session đã chạy ≥30 turns hoặc có critical work
- Khi switch sang task khác hoàn toàn (different wave / repo)
- Trước khi handoff cho session khác
- Khi parallel-agent xong task (tự release lock thay vì chờ stale-purge 4h)

## Process

### Step 0a — Working-tree clean + sync gate (NEW v1.2.0 — BLOCKING precondition)

Chạy TRƯỚC mọi bước khác. Mục tiêu: next session KHÔNG pickup vào dirty / uncommitted / unsynced state → không mất phương hướng. Skip chỉ khi `--allow-dirty` (rare, document lý do).

```bash
bash .claude/skills/workflow/end-session/scripts/end-session.sh --check-only   # gate report
```

1. **Main tree clean** — `git status --porcelain` PHẢI empty. Nếu dirty:
   - File thuộc task hiện tại → commit vào branch (worktree per `worktree-only-branch-work.md`) + push + PR. KHÔNG để dangling uncommitted.
   - File rác/experiment → revert HOẶC `git stash push -m "<lý do>"` (note rõ để next session biết).
2. **Orphan worktrees** — `git worktree list`; mỗi worktree ≠ main tree: `git -C <wt> status --porcelain` PHẢI empty. Dirty worktree = landmine cho next session → commit+push, HOẶC `git worktree remove <wt>` nếu branch đã merged.
3. **Sync state** — `git fetch origin main`; `git rev-list --count main..origin/main`. Nếu >0 → main tree behind (fast-forward nếu đang trên main + clean; note nếu đang trên branch khác).
4. **Decision — end CHỈ được phép khi working tree clean AND một trong:**
   - **(a) Clean-slate:** tất cả session branch đã merged + main synced (rev-list 0). HOẶC
   - **(b) In-flight-handed-off:** branch dở dang đã `commit` + `push` lên remote + session-handoff note (Step 2.5) §Pickup reference đúng branch name + §Start-next-session có lệnh `git worktree add ../kite-wt-<slug> <branch>` để next session tiếp tục.

   Nếu KHÔNG thỏa (a) hoặc (b) → **KHÔNG propose end**; resolve trước (commit/push/PR hoặc viết handoff trỏ branch). Đây là gate cứng: dirty/unsynced + no-handoff = session sau mất phương hướng.

### Step 0 — Docs-sync 5-target verify (NEW v1.1.0)

Per `session-end-context-check.md` §4.5, verify TẤT CẢ 5 targets sync trước khi propose end:

| # | Target | Verify command |
|---|---|---|
| 1 | `gap-status.csv` | `bash scripts/check-gap-status-csv.sh` PASS + recent gap flips reflected |
| 2 | `ROADMAP.md` §Current Status Snapshot | `grep "Wave X\|GAP-Y\|PR #N"` returns recent session ships |
| 3 | `wave-history.jsonl` | `tail -3` shows session's waves |
| 4 | `MEMORY.md` index | `tail -5` shows new memory entries (if any created) |
| 5 | `session-handoffs/YYYY-MM-DD-*.md` | `ls -t documents/03-planning/session-handoffs/ \| head -1` = today's date |

**Decision:** ANY stale → fix BEFORE propose end (bundle vào docs-only PR per `docs-only-pr-auto-merge.md` auto-merge eligible). KHÔNG defer "next session" — context flush = lose track.

<!-- TODO Wave 12+ GAP-868 Phase 2 — replace manual 5-target verify với `scripts/check-post-merge-sync.sh` invocation. Script wraps `post-merge-sync-completeness.md` §2 4-target framework + session-handoff (target #5) — emit PASS/FAIL/WARN deterministic exit. -->


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

### Step 2.5 — Auto-write session-handoff note (NEW v1.1.0)

Skip nếu `--skip-handoff` OR target #5 already present today.

Template: `.claude/skills/workflow/end-session/reference/handoff-template.md` (load on activation).

<!-- TODO Wave 12+ GAP-868 Phase 2 — audit ≥10 shipped handoff notes in `documents/03-planning/session-handoffs/` → categorize template gaps (multi-wave / agent-only / partial-close / blocker-cascade) → refine handoff-template.md với ≥4 new edge-case placeholders. -->


Path: `documents/03-planning/session-handoffs/YYYY-MM-DD-{wave-or-scope-slug}.md`

Required sections (6):
1. `## Scope shipped` — PRs + waves table
2. `## Gaps DONE / improved / NEW filed`
3. `## Lessons captured` — session-internal patterns (non-rule-class)
4. `## Stack state` — local Docker + AWS + known bugs
5. `## Pickup for next session` — wave-N+1 queue + active blockers
6. `## Start next session` — concrete commands

### Step 2.6 — Append wave-history.jsonl if waves shipped

```bash
cat >> .claude/skills/quality/wave-pack-planner/data/wave-history.jsonl <<EOF
{"wave":"<name>","completed_at":"$(date +%Y-%m-%d)","outcome":"<1-line summary + PR refs>"}
EOF
```

One entry per wave completed this session. Skip nếu no wave-level work shipped (gap-only fixes).

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

<!-- TODO Wave 12+ GAP-868 Phase 2 — add Step 4.5 invoking `scripts/check-context-budget.sh --session-delta` (read transcript start vs end size) + emit summary line "Context budget X% → Y% (+Δ tokens)" trước Step 5 PR step. Helps recalibrate context-budget mandate compliance pre `/clear`. -->


### Step 5 — Open docs-only PR + propose /clear (NEW v1.1.0)

Skip nếu `--no-pr` OR no docs changes made (Steps 0/2.5/2.6 all clean OR skipped).

Branch + commit + push per `docs-only-pr-auto-merge.md` — auto-merge eligible khi CI green (chỉ docs jobs run).

Sau khi PR merged → propose `/clear` cho user (clean handoff confirmed). KHÔNG auto-execute `/clear`.

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

- `SKILL.md` — this file (process entry-point)
- `scripts/end-session.sh` — helper one-shot lock archive
- `reference/handoff-template.md` — 6-section session-handoff template (loaded when Step 2.5 fires)

## Related

- `/start-session` (sister Phase 1) — `.claude/skills/workflow/start-session/SKILL.md`
- `.claude/hooks/session-lock-guard.py` — Phase 2 enforcement that this skill complements
- `.claude/rules/session-end-context-check.md` §4.5 — 5-target docs-sync mandate (Step 0 source)
- `.claude/rules/post-merge-sync-completeness.md` §2 — 4-target framework (handoff = target #5 extension)
- `.claude/rules/docs-only-pr-auto-merge.md` — handoff PR auto-merge eligibility
- `.claude/session-locks/README.md` — lock convention + retention policy
- GAP-193 — parent gap (Phase 1 + Phase 2 closed by Wave Meta Phase-2 Cleanup)
- GAP-199 — rework audit (consumes archived locks for retrospective analysis)
- GAP-868 — META formalization (3 Wave 12+ scaffold extensions: handoff template iteration + post-merge sync sub-script + context-budget delta surface) — filed Wave local-doable-11 Bucket D
- Memory: `feedback_parallel_agent_strategy.md`

## Log

- **2026-06-11 (v1.2.0):** MINOR — added Step 0a Working-tree clean + sync gate (BLOCKING precondition before docs-sync) + `end-session.sh` `--check-only`/`--allow-dirty` flags + gate-check function (`git status --porcelain` main tree + orphan-worktree dirty scan + `main..origin/main` behind-count). Triggered by user-flagged 2026-06-11: end-session phải xử lý hết file đang changing + sync main HOẶC check sang nhánh có session-handoff để session sau không mất phương hướng. Skill trước chỉ verify docs-sync + handoff + lock, KHÔNG check uncommitted working files / orphan dirty worktrees / main-behind. Decision gate: end CHỈ khi working tree clean AND (clean-slate merged+synced OR in-flight-branch pushed + handoff §Pickup trỏ branch). Especially relevant với worktree workflow (orphan dirty worktree = landmine). Reviewer: @nguyenvankiet (solo-dev MINOR self-approve — additive precondition, no constraint loosening; existing Steps 0-5 preserved, Step 0a prepended).
- **2026-06-02 (Wave local-doable-11 Bucket D):** Inline `<!-- TODO Wave 12+ GAP-868 -->` markers added tại 3 scaffold candidate positions (Step 0 post-merge sync sub-script + Step 2.5 handoff template iteration + Step 4.5 context-budget delta surface). Filed GAP-868 P1 META tracking Phase 2 concrete implementations. No skill behavior change này wave; markers signal Wave 12+ scope cho future readers.
- **2026-06-02 (v1.1.0):** MINOR — extended scope to include docs-sync 5-target verify (Step 0) + auto-write session-handoff note (Step 2.5) + wave-history.jsonl append (Step 2.6) + docs-only PR open (Step 5). Reference template `handoff-template.md` added. Triggered by user-flagged miss Wave local-doable-6 closure 2026-06-02: session ended without handoff doc; `session-end-context-check.md` §4.5 mandates 5-target sync (target #5 = handoff) but rule §2 exception bypasses §4 sequence when user explicit "end session". Skill now enforces docs-sync regardless of trigger. Per `incident-to-rule-pipeline.md` 5-stage applied (extension, not new rule). Reviewer: @nguyenvankiet (solo-dev MINOR self-approve — additive scope, no constraint loosening; existing v1.0.0 lock-archive flow preserved as Steps 1-4).
- **2026-04-20 (v1.0.0):** Skill created — Phase 2 of GAP-193 (lock archive + 1-line summary).
