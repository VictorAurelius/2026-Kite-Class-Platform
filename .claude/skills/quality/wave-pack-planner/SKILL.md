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

### Step 4.5 — Cross-layer check (api-contract first) — BẮT BUỘC

Per `.claude/rules/contract-first-for-cross-layer.md` v1.0.0 — wave plan có FE+BE scope (cross-layer) PHẢI có Bucket 0 Foundation ship `documents/01-business/{domain}/api-contract.md` TRƯỚC khi spawn FE bucket.

**Decision flow:**

1. Wave có cross-layer scope? Check rule §2 definition:
   - ≥1 bucket touch `*-frontend/**` AND ≥1 bucket touch `*-core/**` → CROSS-LAYER
   - Single bucket touch CẢ FE+BE → CROSS-LAYER
   - FE bucket consume API mà BE bucket cùng wave tạo → CROSS-LAYER
   - FE bucket dùng mock/fixture cho endpoint chưa tồn tại → CROSS-LAYER (contract violation)
2. Nếu CROSS-LAYER:
   - `ls documents/01-business/{domain}/api-contract.md` — tồn tại + đủ endpoint? Skip foundation. Thiếu/missing → add Bucket 0 Foundation vào §3 Scope FIRST
   - §4 State-Check Evidence: thêm row cho api-contract.md
   - FE bucket AC: "Endpoint consumption tuân thủ schema trong api-contract.md (Bucket 0 ship trước)"
   - BE bucket AC: "Controller signature + DTO match api-contract.md schema"
3. Spawn order: Bucket 0 merge FIRST → THEN FE+BE buckets parallel

**Anti-pattern signal:** wave plan có FE bucket + BE bucket nhưng KHÔNG có Bucket 0 Foundation row trong §3 Scope → STOP, add Bucket 0 hoặc justify exception qua commit trailer `CONTRACT_FIRST_OVERRIDE: <reason>`.

**Worked example:** Wave 32 v1 violated rule (no Bucket 0) → 8 sub-gap follow-up (GAP-272c/d/e/h/i/j/k/l). Wave 34 (Phase D) sẽ satisfy rule (Bucket 0 Foundation ships api-contract + MSW infra first).

Memory: `feedback_fe_first_endpoint_proliferation.md` (auto-loaded).

### Step 4.6 — Quality calibration (model tier per stake)

Per `feedback_sonnet_parallel_agent_crash.md` + `feedback_opus_rework_validation.md` — classify wave stake → pick model tier cho parallel agents. Document choice trong wave plan §1 Brainstorm.

**Stake matrix:**

| Wave stake | Model tier | Rationale |
|---|---|---|
| **HIGH** — Phase 1 BETA blocking, AI Branding feature, payment/lifecycle/security, multi-file refactor với strict compliance gates | **Opus 4.7 full effort** | Sonnet crashes mid-stream + skip verification trên multi-file refactor (Wave 32 v1 = 0/3 ship clean) |
| **MEDIUM** — FE component ports theo established patterns, doc-heavy, single-file additions | **Opus medium effort** (experiment first) | Measure wall-clock + audit findings vs full Opus baseline; adopt as default chỉ khi ship-clean parity |
| **LOW** — typo fix, doc sync, dep bump, cleanup | **Sonnet/Haiku OK** | Single-file low-complexity work; no compliance-gate burden |

**Model-agnostic gates (mandatory regardless of tier):**
- §3.4 Verification gate: PR body paste output `pnpm test --run <new-file>` + `mvn test -pl <module>` xanh
- §3.5 AC Coverage table: PR body có table mapping mỗi AC line → file/test/verification
- §5.5 Anti-pattern grep: PR body confirm grep checks (e.g. no `console.log`, no `TODO`, no scaffold strings)

**Anti-pattern signal:** wave plan §1 Brainstorm không document model tier choice → reviewer hỏi "stake assessment?" trước approve. Default fallback: Opus 4.7 full effort (safer than under-spec).

### Step 5 — Spawn agents (1 message, multiple Agent calls)

**BẮT BUỘC** single message với N Agent tool uses parallel. KHÔNG sequential spawn (mất parallel benefit).

```
Agent A: isolation=worktree, prompt từ docs-only-agent.md + GAP-XXX context
Agent B: isolation=worktree, prompt từ feature-tdd-agent.md + GAP-YYY context
Agent C: isolation=worktree, prompt từ docs-only-agent.md + GAP-ZZZ context
```

Sau khi agents xong → coordinator merge sequential (A → B → C), resolve SOFT conflicts manually, close wave per `reference/retrospective-checklist.md`.

### Step 5.5 — Pipeline next wave plan (during agent wait — eliminate dead-time)

**Khi nào:** ngay sau Step 5 (agents spawned background), trước khi merge starts.

**What:** Coordinator KHÔNG idle wait — propose Wave N+1 candidate (2-3 từ ROADMAP §🚀 Next Action) → user pick → draft Wave N+1 plan PR (state-check + brainstorm + scope + bucket allocation). Plan PR is docs-only, low conflict risk.

**Constraints:**
- Wave N+1 plan PR drafted but **NOT merged** until Wave N closure ships (per `feedback_wave_plan_through_pr.md`).
- Skip if coordinator already at ~150k+ context (per `feedback_token_quota_spawn_timing.md`).
- Skip if Wave N has high failure risk (untested infra) — finish Wave N + lessons first.
- Skip if user wants to pause between waves — respect pacing.

**Math:** sequential 2-wave block ~140min; pipelined ~110min = ~20-30% saving. See `feedback_pipelined_wave_planning.md` (auto-loaded memory).

**Anti-pattern signal:** if coordinator says "agents working background, anything else?" instead of "while agents run, pick Wave N+1?" — pipeline missed.

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

## Wave numbering (added 2026-05-23 — per `.claude/rules/wave-tag-numbering-convention.md` v1.0.0)

Từ Wave thesis-1 (2026-05-23) trở đi, wave mới dùng **tag-based format** thay vì sequential counter (Wave 108, Wave 109, ...). Wave 01-107 cũ giữ sequential — KHÔNG backfill.

### Format

```
wave-{tag_primary}-{counter}[-{descriptor}]
```

- `tag_primary` lowercase-kebab semantic (thesis / beta / security / meta / ops / feature-{domain} / hotfix / release-{N})
- `counter` monotonic integer per tag (thesis-1, thesis-2, thesis-3, ...)
- `descriptor` optional kebab-case (closure / hardening / fix-bugs)

### Examples

| Artifact | Format |
|---|---|
| Wave identifier | `wave-thesis-1-closure` |
| Plan filename | `documents/03-planning/waves/wave-2026-05-23-thesis-1-closure.md` |
| Branch | `wave/thesis-1-closure` |
| Plan commit | `plan(wave-thesis-1): closure 6 bucket parallel` |
| Bucket commit | `feat(wave-thesis-1-bucket-A): citation-extract skill` |
| wave-history.jsonl entry | `{"wave":"thesis-1","tag_primary":"thesis","tags_secondary":["doc","beta-prep","meta"],"counter":1,...}` |

### Frontmatter trong wave plan file

```yaml
---
wave: 1
tag_primary: thesis
tags_secondary: [doc, beta-prep, meta]
counter: 1
date_launch: 2026-05-23
status: planning | in-progress | shipped | abandoned
---
```

### Multi-tag

1 wave có **1 primary tag** (drives counter) + **N secondary tags** (descriptive only). Query "tất cả wave touching beta-prep":

```bash
jq 'select(.tag_primary == "beta-prep" or ((.tags_secondary // []) | index("beta-prep")))' wave-history.jsonl
```

### Counter rules

- Monotonic per tag, no skip, no reset (thesis-1, thesis-2, thesis-3...)
- Sub-wave dùng descriptor (`wave-thesis-1-fix-bundle`) thay vì decimal (`wave-thesis-1.1` BANNED)
- Tags độc lập (`thesis-3` không liên quan `beta-3`)

### Hybrid history query

`wave-history.jsonl` chứa 2 format coexist (legacy sequential Wave 01-107 + new tag-based từ Wave thesis-1). Query script PHẢI handle cả 2:

```bash
# Match by theme keyword (fuzzy) for legacy + by tag_primary for new
jq 'select(
  .tag_primary == "thesis" or
  (.tags_secondary // [] | index("thesis")) or
  (.theme | ascii_downcase | test("thesis|khóa luận"))
)' wave-history.jsonl
```

Reference đầy đủ: `.claude/rules/wave-tag-numbering-convention.md`.

## Related

- Memory: `feedback_wave_pack_cross_gap_clustering.md` (cluster pattern motivation)
- Memory: `feedback_wave_plan_before_serial_prs.md` (anti-pattern serial PR)
- Memory: `feedback_parallel_agent_strategy.md` (worktree isolation rules)
- Memory: `feedback_wave_plan_through_pr.md` (PR-first cho wave plan)
- Skill: `workflow/start-session/SKILL.md` (Step 4.5 wave-eligibility hint)
- Skill: `workflow/quality-plan/SKILL.md` (alternative planning skill)
- Skill: `workflow/gap-to-pr-converter.md` (single-gap-to-PR; this skill = N-gap-to-wave)
- Rule: `.claude/rules/post-wave-audit-mandate.md` §4 (post-wave audit cadence)
- Rule: `.claude/rules/contract-first-for-cross-layer.md` (Step 4.5 reference)
- Memory: `feedback_fe_first_endpoint_proliferation.md` (Step 4.5 motivation)
- Memory: `feedback_sonnet_parallel_agent_crash.md` (Step 4.6 motivation)
- Memory: `feedback_opus_rework_validation.md` (Step 4.6 working solution)

## Log

- **2026-05-07 (v1.2):** Added §Step 4.5 Cross-layer check (api-contract first) per new `.claude/rules/contract-first-for-cross-layer.md` v1.0.0 + §Step 4.6 Quality calibration (model tier per stake) per `feedback_sonnet_parallel_agent_crash.md` + `feedback_opus_rework_validation.md`. Wave 32 v1 (Sonnet 4.6, no contract foundation) → 0/3 ship clean + 8 sub-gap follow-up; Wave 32 rework (Opus 4.7 + verification gates) → 4/4 ship clean. Both lessons codify into skill steps. Reviewer: @nguyenvankiet (solo-dev MINOR — additive workflow guidance, no constraint loosening).
- **2026-05-06 (v1.1):** Added §Step 5.5 — Pipeline next wave plan (during agent wait). Codifies pattern user requested after Wave 27 retro: coordinator drafts Wave N+1 plan PR while Wave N agents run background → 0 dead-time between waves (~20-30% saving per 2-wave block). Paired with memory `feedback_pipelined_wave_planning.md` (auto-loaded). Reviewer: @nguyenvankiet (solo-dev MINOR — additive workflow guidance, no constraint loosening).
- **2026-04-28 (v1.0):** Skill created. Codifies methodology demonstrated by Wave Observability 2026-04-28 (3 gaps GAP-121/143/144 closed in ~75 min wall-clock = ~5x speedup vs serial). Wave Obs lessons-learned (worktree cross-contamination Phase 2b, values.yaml shared-section soft-merge) captured in §Gotchas. Day 2 deliverable per "Option C hybrid" strategy (execute then codify) decided in 2026-04-28 morning session. Reviewer: @nguyenvankiet (solo-dev MINOR, new skill paired with check-skill-conventions PASS + self-test green).
