# Wave Coordinator Runbook

**Use when:** You (parent Claude session) are running a wave-pack — orchestrating 3-5 isolated agents from foundation PR through closure.

**Format:** Checklist/runbook for human-in-the-loop coordinator (NOT a prompt to feed Agent tool). Coordinator is the parent session; subagents are spawned via `Agent` tool with templates from siblings in this folder.

**Branch naming convention enforced:** all sub-PRs `feat|chore|test|docs/wave-{theme}-gap-{id-slug}[-suffix]`

## Coordinator phase checklist

### Phase 0 — Pre-flight (5 min)
- [ ] `claude mcp list` — verify GitHub MCP connected (per `feedback_mcp_check_at_session_start.md`)
- [ ] `git checkout main && git pull --ff-only` (per `feedback_git_pull_ff_only.md`)
- [ ] Confirm wave plan exists: `documents/03-planning/waves/wave-{date}-{theme}.md`
- [ ] Confirm ROADMAP §"Active wave queue" lists this cluster as IN_PROGRESS
- [ ] Verify reserved migration slots noted in wave plan (if applicable)
- [ ] Verify GAP IDs in scope have Status `🔵 OPEN` or `🟡 PARTIAL` (not already DONE)

### Phase 1 — Foundation PR (10 min) — wave plan itself
- [ ] Branch: `wave/{theme}-plan` or `docs/wave-{theme}-plan` (NOT direct push, per `feedback_wave_plan_through_pr.md`)
- [ ] PR title: `wave: {theme} cluster plan ({n} gaps)`
- [ ] PR body: link gaps, file-overlap matrix, deferred items
- [ ] Squash merge after CI green
- [ ] `git pull --ff-only` after merge

### Phase 2 — Spawn agents (1 message, multiple Agent calls)
- [ ] Pre-create worktrees: `git worktree add /tmp/worktrees/wave-{theme}-{role} main`
- [ ] Pre-create branches in each worktree
- [ ] **Single message** with N Agent tool uses in parallel (NOT sequential)
- [ ] Each Agent: `isolation=worktree`, `subagent_type=general-purpose`
- [ ] Pass concrete worktree path in prompt (per rule #3 — agents drift to main repo otherwise)
- [ ] Cap: max 5 concurrent agents (per `feedback_parallel_agent_strategy.md` rule #9)

### Phase 3 — Monitor + collect (variable, ~30-60 min)
- [ ] Each agent reports back: branch, PR URL, scope summary
- [ ] If agent silent + transcript mtime stale → respawn with same prompt (per rule #7)
- [ ] If agent reports SOFT conflict expected (shared config file section) → note for Phase 4
- [ ] If agent escalates (e.g. p3-cleanup found non-trivial issue) → triage: defer to next wave OR re-scope

### Phase 4 — Sequential merge (NOT batch — CI race risk per rule #5)

For each agent's PR (in dependency order, typically A → B → C):
- [ ] `gh pr checks <PR>` — green
- [ ] Verify branch is up-to-date with main; if not, `gh pr update-branch <PR>`
- [ ] Resolve SOFT conflicts manually:
  - Whole-file reformats: prefer the agent's section, drop reformat
  - Shared config sections (values.yaml, application.yml): merge both sections, keep alphabetical
  - ROADMAP races: coordinator owns; agents must NOT touch ROADMAP
- [ ] `gh pr merge <PR> --squash` (NOT `--delete-branch` — see Gotchas)
- [ ] `git checkout main && git pull --ff-only`
- [ ] Verify merge clean (`git log --oneline -3`)

### Phase 5 — Per-gap status flips (per `gap-done-discipline.md` §2)

For each closed gap:
- [ ] Verify ALL Acceptance Criteria checkboxes `- [x]` in gap file
- [ ] Verify NO banned phrases in DONE-flip Log entry: `deferred`, `defer to`, `manual run`, `out of scope`, `infra block`, `local can't`, `chưa boot được`, `partial` (when status is DONE)
- [ ] If any AC unchecked → status stays `🟡 PARTIAL`, file follow-up gap
- [ ] If all AC checked → flip Status to `🟢 DONE` with PR refs in Log
- [ ] Commit: `chore(gap): {GAP_ID} → DONE post-wave-{theme}`

### Phase 6 — Wave closure
- [ ] ROADMAP §"Current Status Snapshot" — append wave-closure entry:
  - Wave name + date
  - Gaps closed (with PR refs)
  - Wall-clock metric (foundation PR → final merge)
  - Lessons-learned 1-liner
- [ ] ROADMAP §"Active wave queue" — rotate cluster out, advance next
- [ ] Append to `data/wave-history.jsonl`:
  ```json
  {"wave":"{theme}","date":"YYYY-MM-DD","gaps":["GAP-X","GAP-Y"],"prs":[N1,N2],"wall_clock_min":75,"agent_count":3,"soft_conflicts":1,"hard_conflicts":0,"lessons":"..."}
  ```
- [ ] Commit: `chore(wave): close {theme} — N gaps, ~M min wall-clock`

### Phase 7 — Worktree cleanup (mandatory per rule #6)

Manual sequence (each `gh pr merge --delete-branch` would fail with worktree referencing branch):
- [ ] `git worktree remove /tmp/worktrees/wave-{theme}-A` (repeat per agent)
- [ ] `git branch -D feat/wave-{theme}-gap-{id}-A` (repeat)
- [ ] `git push origin --delete feat/wave-{theme}-gap-{id}-A` (repeat)
- [ ] `git stash list` — drop any agent-stash entries (verify `git diff stash@{N}` matches what shipped per rule #10)
- [ ] `git worktree prune`

### Phase 8 — Post-wave audit (per `post-wave-audit-mandate.md`)
- [ ] Identify required audits per file patterns changed (UI / Business Logic / Security / Ops / Performance / API Contract)
- [ ] Schedule within 3-day window (per rule)
- [ ] If touching `pom.xml`/`package.json` → security audit MUST run
- [ ] If touching `infrastructure/` → ops-readiness MUST run

## Gotchas

- **`--delete-branch` trap** (per `feedback_stacked_pr_delete_branch.md`): merging stacked PRs with `--delete-branch` auto-closes child PRs unrecoverable. NEVER use `--delete-branch` in wave merge. Manual cleanup in Phase 7.
- **Parent cwd drift** (per `feedback_parallel_agent_strategy.md` rule #8): after agent push + `gh pr merge`, parent cwd may end on agent's branch. Always prefix `cd /home/nguyenvankiet/projects/2026-Kite-Class-Platform && git checkout main`.
- **Stash dance** (per rule #10): if agents leak into main repo working copy, `git stash push -m "agent-X-stash" -- <files>` to preserve before switching context. Drop after verifying ship.
- **Sequential merge timing**: if Agent A's PR fails CI, do NOT skip to merge B/C — breaks dependency chain. Either fix A or abort wave.
- **YAML validation** (per `feedback_yaml_validate_before_push.md`): if foundation PR or any agent touches `.github/workflows/*.yml`, validate locally before push.
- **Banned-phrase check** is the most-missed step. Build a habit: open gap file BEFORE flipping Status, scan Log entry text against `gap-done-discipline.md` §2 banned list.
- **`data/wave-history.jsonl` append** is easy to forget — it's the only persistent record of wave wall-clock for tuning future waves.

## When NOT to use this runbook

- Single-gap PR (not a wave) — use `workflow/start-pr` skill instead
- 1-2 disjoint gaps — overhead of wave > benefit (per SKILL.md "When NOT to use")
- Hot-fix wave (production incident) — skip Phases 0-1, jump to Phase 2; document override
- Audit-driven wave (catch-up audits) — different methodology, see `post-wave-audit-mandate.md` §4 runbook

## Reference

- Methodology: [`../../SKILL.md`](../../SKILL.md)
- Spawn pattern: [`../../reference/agent-spawning-template.md`](../../reference/agent-spawning-template.md)
- Wave closure detail: [`../../reference/retrospective-checklist.md`](../../reference/retrospective-checklist.md)
- File overlap analysis: [`../../reference/file-overlap-algorithm.md`](../../reference/file-overlap-algorithm.md)
- Worked example: Wave Observability 2026-04-28 (3 gaps, 3 agents, ~75 min wall-clock — see `documents/03-planning/waves/wave-2026-04-29-observability.md`)
- Memory: `feedback_parallel_agent_strategy.md` (rules #1-10)
- Memory: `feedback_wave_plan_through_pr.md` (foundation PR-first)
- Memory: `feedback_wave_pack_cross_gap_clustering.md` (cluster motivation)
- Rule: `.claude/rules/gap-done-discipline.md` (status-flip discipline)
- Rule: `.claude/rules/post-wave-audit-mandate.md` (post-wave audit cadence)
