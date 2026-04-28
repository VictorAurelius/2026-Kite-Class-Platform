# Wave Plan Template

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

## Scope

| # | Gap | Title | Priority | Agent | Disjoint files |
|:-:|-----|-------|:--------:|:-----:|----------------|
| 1 | **{GAP-XXX}** | {gap title} | {🔴 P0 / 🟠 P1 / 🟡 P2 / 🟢 P3} | A | {file paths or globs} |
| 2 | **{GAP-YYY}** | {gap title} | {priority} | B | {file paths or globs} |
| 3 | **{GAP-ZZZ}** | {gap title} | {priority} | C | {file paths or globs} |

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
