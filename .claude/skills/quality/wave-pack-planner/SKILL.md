---
name: wave-pack-planner
description: "Dùng khi user nói 'plan wave', 'cluster gaps', 'wave-pack', 'next wave', hoặc khi cần group ≥3 disjoint gaps thành 1 wave-pack chạy parallel agents. Output: wave plan markdown (foundation PR-ready) + file-overlap matrix + 3-5 agent prompts. Codify pattern từ Wave Observability 2026-04-28 (3 gaps trong ~75 min vs ~6h serial = 5x speedup)."
user-invocable: true
argument-hint: "[gap-id ...] [--theme=<topic>]"
---

# /wave-pack-planner — Cluster-then-Spawn Methodology

Group các disjoint gaps thành wave-pack rồi spawn 3-5 worktree-isolated agents parallel. Đóng nhiều gaps trong 1 session, không serial PR queue.

## When to use

- Sau `/start-session` báo "Wave-eligible: YES" (≥3 sub-tasks/gaps disjoint)
- Khi backlog có ≥5 gaps cùng theme (DR/Backup, Observability, Admin, Frontend bundle...)
- Trước khi "pick 1 gap rồi serial" — luôn check cluster-eligible trước
- Khi muốn validate pattern thêm 1 data point

## When NOT to use

- 1-2 gaps độc lập (overhead wave plan > save time)
- Gaps share migration version slot (V_n collide khi parallel)
- Gaps share `application.yml` / single service file (rebase sequential)
- Foundation chưa ship (cần PR-0 trước)

## Process (5 steps, ~30-45 min planning + spawn)

### Step 1 — Identify candidate cluster (5 min)

Đọc `documents/04-quality/gaps/ROADMAP.md` §"Active wave queue (clustered)". Cluster định sẵn → chọn cluster đầu queue. Nếu không có:
- `grep -l "Domain.*<theme>" documents/04-quality/gaps/GAP-*.md` để find gaps cùng theme
- Output: 3-7 gap IDs candidate

### Step 2 — File overlap analysis (10 min)

```bash
./.claude/skills/quality/wave-pack-planner/scripts/analyze-overlap.sh GAP-117 GAP-118 GAP-119
```

Script parse "Files" / "Affects" / "Proposed Fix" sections → output matrix:

| File | Touched by | Conflict risk |
|------|-----------|:-------------:|
| ... | A only | None |
| `shared.yaml` | B + C | **SOFT** — different sections |
| `migration/V47__*.sql` | A + B | **HARD** — version collide → SERIALIZE |

**Decision rule:** ≥1 HARD conflict → either re-bucket gaps OR ship foundation PR first to defuse. Đọc `reference/file-overlap-algorithm.md` cho edge cases.

### Step 3 — Bucket assignment (5 min)

Mỗi disjoint subset → 1 agent. Choose agent template:
- Pure markdown/runbook gap → `assets/agents/docs-only-agent.md`
- Backfill tests/fixtures → `assets/agents/test-only-agent.md`
- Dead code/unused/cleanup → `assets/agents/p3-cleanup-agent.md`
- Code change with TDD → `assets/agents/feature-tdd-agent.md`

Wave plan tự viết bằng `wave-coordinator-agent.md` template.

### Step 4 — Foundation PR (10 min)

Tạo `documents/03-planning/waves/wave-{date}-{theme}.md` theo template `reference/wave-plan-template.md`. Ship qua PR (PR-first per `feedback_wave_plan_through_pr.md`).

ROADMAP entry: thêm cluster vào §"Active wave queue", mark IN_PROGRESS.

### Step 5 — Spawn agents (1 message, multiple Agent calls)

**BẮT BUỘC** single message với N Agent tool uses parallel. KHÔNG sequential spawn (mất parallel benefit).

```
Agent A: isolation=worktree, prompt từ docs-only-agent.md + GAP-XXX context
Agent B: isolation=worktree, prompt từ feature-tdd-agent.md + GAP-YYY context
Agent C: isolation=worktree, prompt từ docs-only-agent.md + GAP-ZZZ context
```

Sau khi agents xong → coordinator merge sequential (A → B → C), resolve SOFT conflicts manually, close wave per `reference/retrospective-checklist.md`.

## Skill contents

- `SKILL.md` — this file (entry point)
- `reference/cluster-pattern.md` — cluster-then-spawn methodology + decision tree
- `reference/file-overlap-algorithm.md` — overlap classification + edge cases
- `reference/agent-spawning-template.md` — how to write isolation:worktree prompts
- `reference/retrospective-checklist.md` — lessons-learned capture sau wave merge
- `reference/wave-plan-template.md` — markdown template cho wave plan
- `reference/background-loop-fleet.md` — documented `/loop` commands cho doc-sync, p3-sweeper, audit-cadence
- `assets/agents/` — 5 agent prompt templates (docs-only, test-only, p3-cleanup, feature-tdd, wave-coordinator)
- `scripts/analyze-overlap.sh` — file overlap detector
- `data/wave-history.jsonl` — append-only log: wave + gaps + wall-clock + lessons

## Rules

- **Parallel spawn = 1 message, multiple Agent calls.** Sequential Agent spawns = anti-pattern (mất parallel benefit)
- **Foundation PR phải merge TRƯỚC khi spawn agents** — agents branch off main, không off feature branch
- Mỗi agent dùng `isolation: "worktree"` — không share working dir
- Branch naming: `feat/wave-{theme}-gap-{id-slug}` — coordinator dễ track
- Coordinator merge sequential (A → B → C) — KHÔNG batch merge (CI race)
- Sau merge, **bắt buộc** clean worktrees + remote branches per `feedback_parallel_agent_strategy.md` rule #6
- Update `data/wave-history.jsonl` với wall-clock + lessons (data points cho future tuning)

## Gotchas

- **Worktree cross-contamination** (Wave Obs Phase 2b): 2 agents sửa cùng 1 file (vd. `adr/README.md`) trong worktrees riêng → cuối khi merge git stash dance complex. Mitigation: file-overlap analysis (Step 2) phải catch — nếu HARD conflict, re-bucket
- **Worktree absolute-path bug** (Wave DR/Backup 2026-04-28): nếu prompt cite absolute paths (`/home/.../scripts/foo.sh`), agent có thể `cd` ra khỏi worktree vào main checkout → Write/commits land SAI branch. Wave DR/Backup case: Agent B's commit landed trên Agent C's branch → coordinator phải rebase + force-push để recover. Mitigation: prompt template (`assets/agents/*-agent.md`) bắt buộc dùng RELATIVE paths + agent verify `pwd | grep worktrees` trước Write/commit. Memory: `feedback_worktree_absolute_path_contamination.md`
- **`values.yaml` shared section** (Wave Obs Agent B + C): cùng file nhưng different YAML sections → git auto-merge thường OK. Nhưng nếu 1 agent reformats whole file → conflict. Mitigation: agent prompt instruct "chỉ touch section X, đừng reformat file"
- **Stale wave plan trong `waves/`**: collect-state.sh dùng mtime fallback → có thể chỉ vào wave đã ship. Mitigation: ROADMAP §"Active wave queue" là source of truth, không phải mtime
- **Sequential merge timing**: nếu Agent A's PR fail CI, đừng skip merge A để merge B trước — sẽ break dependency chain. Mitigation: wave plan ghi rõ merge order + rollback path
- **Agent prompt drift**: mỗi lần edit template → nhớ bump version + log entry. Otherwise next wave dùng stale template không match real workflow
- **Overlapping gap closure timing**: nếu 2 agents close 2 gaps trong cùng PR description → ROADMAP entry race. Mitigation: 1 PR = 1 gap closure, coordinator wave-closure entry batch
- **Wall-clock metric noise**: wall-clock include user response delay (approve permissions, answer Qs). Đo agent-time riêng nếu cần benchmark thực

## Self-test

```bash
./scripts/analyze-overlap.sh GAP-121 GAP-143 GAP-144
# Expect output match documents/03-planning/waves/wave-2026-04-29-observability.md "File overlap analysis" section
```

Nếu output diverge → script bug, fix trước khi dùng skill cho wave mới.

## Related

- Memory: `feedback_wave_pack_cross_gap_clustering.md` (cluster pattern motivation)
- Memory: `feedback_wave_plan_before_serial_prs.md` (anti-pattern serial PR)
- Memory: `feedback_parallel_agent_strategy.md` (worktree isolation rules)
- Memory: `feedback_wave_plan_through_pr.md` (PR-first cho wave plan)
- Skill: `workflow/start-session/SKILL.md` (Step 4.5 wave-eligibility hint)
- Skill: `workflow/quality-plan/SKILL.md` (alternative planning skill)
- Skill: `workflow/gap-to-pr-converter.md` (single-gap-to-PR; this skill = N-gap-to-wave)
- Rule: `.claude/rules/post-wave-audit-mandate.md` §4 (post-wave audit cadence)

## Log

- **2026-04-28 (v1.0):** Skill created. Codifies methodology demonstrated by Wave Observability 2026-04-28 (3 gaps GAP-121/143/144 closed in ~75 min wall-clock = ~5x speedup vs serial). Wave Obs lessons-learned (worktree cross-contamination Phase 2b, values.yaml shared-section soft-merge) captured in §Gotchas. Day 2 deliverable per "Option C hybrid" strategy (execute then codify) decided in 2026-04-28 morning session. Reviewer: @nguyenvankiet (solo-dev MINOR, new skill paired with check-skill-conventions PASS + self-test green).
