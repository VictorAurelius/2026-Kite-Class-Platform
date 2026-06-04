# Wave Plan Template

> **⚠️ CANONICAL SOURCE = [`documents/03-planning/waves/_TEMPLATE.md`](../../../../documents/03-planning/waves/_TEMPLATE.md)** — đó là file mà `scripts/check-wave-plan-completeness.sh` validator đọc required sections từ. Drift giữa 2 file → canonical thắng. Reference này là mirror + thêm File-overlap matrix + Lessons-learned scaffolding cho rich plans.
>
> **Recurrence note 2026-06-04** (Wave flow-kh3 stub + Wave 14 PR #2141): cả 2 lần stub/plan ship thiếu canonical `## N. Heading` numbered structure → CI fail + revision-PR cost. **Stubs cũng phải có đủ 8 sections + 4 frontmatter fields** (`title`/`status`/`created`/`waves`) ngay cả khi `## 3. Scope` để TBD. Pre-commit hook `scripts/hooks/pre-commit` (opt-in via `git config core.hooksPath scripts/hooks`) chạy validator local trước commit.

Markdown template cho `documents/03-planning/waves/wave-{date}-{theme}.md`. Source: structure of `wave-2026-04-29-observability.md` (canonical example).

Copy block dưới, fill `{placeholders}`. Ship qua PR per [feedback_wave_plan_through_pr.md](../../../../projects/-home-nguyenvankiet-projects-2026-Kite-Class-Platform/memory/feedback_wave_plan_through_pr.md) BEFORE spawning agents.

## Frontmatter (required)

```yaml
---
title: Wave {Theme} — {1-line summary}
status: active
created: {YYYY-MM-DD}
updated: {YYYY-MM-DD}
gaps: [GAP-XXX, GAP-YYY, GAP-ZZZ]
deferred_to_next_wave: [GAP-AAA]
deferred_separate_track: [GAP-BBB]
---
```

Fields:
- `status`: `active` (during wave) → `complete` (after merge) → `superseded` (if rewritten)
- `gaps`: list of gap IDs IN this wave
- `deferred_to_next_wave`: gaps in same theme but defered for race-risk reasons
- `deferred_separate_track`: gaps in different scope (multi-PR migration etc.)

## Body template

```markdown
# Wave {Theme} — Cluster Pack {N}

**Wave date:** {YYYY-MM-DD} (kicked off {YYYY-MM-DD HH:MM})
**Cluster theme:** {1-2 sentence theme description}
**Strategy reference:** {link to memory or earlier wave + rationale}
**Stake tier (per SKILL.md §Step 4.6):** {HIGH | MEDIUM | LOW} → model tier: {Opus 4.7 full | Opus medium | Sonnet/Haiku}
**Cross-layer? (per SKILL.md §Step 4.5):** {YES → Bucket 0 Foundation required | NO → skip foundation}

## Scope (compact §3 schema — Strategy B+C proven Wave 33)

Only one row per bucket. Gap details live in their gap files. Strategy:
- B (compact): one row per bucket, files glob-only
- C (cross-layer): if cross-layer=YES, Bucket 0 Foundation row FIRST per `contract-first-for-cross-layer.md`

| # | Bucket | Gap(s) | Priority | Files (glob) | Spawn order |
|:-:|--------|--------|:--------:|--------------|:-----------:|
| 0 | **Foundation** | (contract + mock infra) | 🟠 P1 | `documents/01-business/{domain}/api-contract.md` + `{frontend}/src/test/msw/handlers/{domain}.ts` | **MERGE FIRST** |
| 1 | **A** | {GAP-XXX} | {priority} | {file glob} | parallel after Bucket 0 |
| 2 | **B** | {GAP-YYY} | {priority} | {file glob} | parallel after Bucket 0 |
| 3 | **C** | {GAP-ZZZ} | {priority} | {file glob} | parallel after Bucket 0 |

**Cross-layer foundation bucket pattern** (skip if cross-layer=NO):

### Bucket 0 — Foundation (Contract + Mock Infrastructure)

- **Files:** `documents/01-business/{domain}/api-contract.md` (CREATE/UPDATE)
  - List mọi endpoint mà FE+BE buckets trong wave consume
  - Mỗi endpoint: method + path + request/response schema + error codes
- **Mock infra (nếu wave dùng MSW handlers):** `{frontend}/src/test/msw/handlers/{domain}.ts` setup
- **Acceptance:** api-contract.md tồn tại + list đủ endpoints; MSW handlers consumable
- **Spawn order:** MERGE FIRST trước FE+BE buckets

## Deferred (next wave)

- **{GAP-AAA}** — {1-line title}. Deferred because {race-risk reason: same file as Agent X / shared migration version / dependency on GAP-Y}.

## Deferred (separate track)

- **{GAP-BBB}** — {1-line title}. Tracked separately because {multi-service migration / multi-PR scope per rule X.md / etc.}.

## File overlap analysis

Run via `./.claude/skills/quality/wave-pack-planner/scripts/analyze-overlap.sh {GAP-XXX} {GAP-YYY} {GAP-ZZZ}`.

| File | Touched by | Conflict risk |
|------|-----------|:-------------:|
| `{path/to/file-1}` | A only | None |
| `{path/to/file-2}` (NEW) | B only | None |
| `{path/to/shared-file}` | B + C | **SOFT** — {section/key disjoint, git auto-merges} |
| `{path/to/another-shared}` | A + C | **HARD** — {reason} → SERIALIZE A→C OR re-bucket |

Net: {summary of overlap state}.

## Agent workflow

Per `feedback_parallel_agent_strategy.md`:

1. Each agent gets `isolation: "worktree"` (separate git checkout)
2. Branches off main (after this foundation PR merges)
3. Commits + creates own PR — branch naming: `feat/wave-{theme}-{gap-id-slug}`
4. Reports back PR number + scope summary
5. Coordinator merges sequentially: A → B → C
6. Conflict resolution: {who resolves which file at merge}
7. Wave closure ROADMAP entry after all {N} merge

## Acceptance criteria (wave-level)

- [ ] {N} PRs merged (one per gap) with green CI
- [ ] All {N} gap files transitioned per `gap-done-discipline.md` §2
- [ ] ROADMAP "Current Status Snapshot" gets wave-closure entry (counts updated, queue rotated)
- [ ] No conflicts left unresolved on main
- [ ] Worktrees + branches cleaned post-merge per `feedback_parallel_agent_strategy.md` rule #6
- [ ] `data/wave-history.jsonl` entry appended
- [ ] Lessons-learned section filled below

## Wall-clock target

- Foundation PR (this doc + ROADMAP entry): ~10 min
- {N} parallel agents: ~{X-Y} min wall (each ~{P-Q} min agent-time, parallel)
- Sequential merge + conflict resolution: ~{Z} min
- Closure (ROADMAP + cleanup + retrospective): ~10 min
- **Total wave: ~{TOTAL} min**

## Lessons-learned ({wave-name}, completed {YYYY-MM-DD})

(Filled AFTER wave merges — copy template from `reference/retrospective-checklist.md`)

### Worktree isolation
- [ ] Did `isolation: "worktree"` hold?
- [ ] Contamination details if any: {agents, files, recovery steps}

### File-overlap accuracy
- [ ] Predicted SOFT: {list}; actual: {list}
- [ ] Predicted HARD: {list}; actual: {list}
- [ ] Unpredicted conflicts: {list}

### Wall-clock
- [ ] Estimated: {min}; actual: {min}; variance source: {reason}

### Agent prompt quality
- [ ] Clarification rounds: A={n}, B={n}, C={n}
- [ ] Template updates needed: {which agents/templates}

### Token cost
- [ ] Total tokens: {sum}; per gap: {avg}

### Cleanup
- [ ] Worktrees removed
- [ ] Local branches deleted
- [ ] Remote branches deleted
- [ ] Stale stashes cleaned

### Novel patterns
- [ ] New memory entry filed? {path or N/A}
- [ ] Rule update proposed? {link or N/A}

## Log

- {YYYY-MM-DD} — Wave plan created. Foundation PR will land this doc + ROADMAP active-wave callout. After merge, {N} agents spawn from main.
- {YYYY-MM-DD} — {Status update entry per stage}.
- {YYYY-MM-DD} — Wave SHIPPED: {summary}, lessons-learned filled.
```

## Naming convention

Filename: `wave-{YYYY-MM-DD}-{theme-slug}.md`
- `theme-slug`: lowercase, kebab-case, ≤3 words (e.g. `observability`, `dr-backup`, `kh-admin`)
- Date = wave KICKOFF date, not merge date

Branch for foundation PR: `wave/{date}-{theme}-plan`

## Related

- [SKILL.md](../SKILL.md) — entry point Step 4
- [cluster-pattern.md](cluster-pattern.md) — eligibility before drafting plan
- [file-overlap-algorithm.md](file-overlap-algorithm.md) — fills overlap matrix
- [agent-spawning-template.md](agent-spawning-template.md) — agent prompts post-merge
- [retrospective-checklist.md](retrospective-checklist.md) — fills Lessons-learned section
- Canonical example: `documents/03-planning/waves/wave-2026-04-29-observability.md`
- Rule `.claude/rules/planning-docs-structure.md` — frontmatter + placement
- Memory `feedback_wave_plan_through_pr.md` — PR-first mandate
