# Agent Spawning Template — `isolation: "worktree"` Prompts

Detail cho Step 5 trong [SKILL.md](../SKILL.md). How to write agent prompts that don't need clarification rounds.

## Self-contained rule

Agent có **zero conversation history**. Prompt phải include EVERYTHING:
- Context (theme, gap IDs, why this exists)
- Concrete file paths to touch
- Do-not-touch list (mitigation per [file-overlap-algorithm.md](file-overlap-algorithm.md) HARD/SOFT findings)
- Branch name to use
- Commit message format
- Success criteria (measurable, not "do it correctly")
- Report-back format

If prompt is missing ANY → agent will guess + rework rounds eat parallel budget.

## Worktree contract

| Step | Owner | Action |
|------|-------|--------|
| 1 | Coordinator | Spawn agent with `isolation: "worktree"` flag (separate git checkout) |
| 2 | Agent | Branch off main HEAD inside its worktree (`git checkout -b feat/wave-{theme}-{gap-slug}`) |
| 3 | Agent | TDD/edit/commit cycle, all confined to worktree |
| 4 | Agent | Push branch + open PR via `gh pr create` |
| 5 | Agent | Report PR number + scope summary back to coordinator |
| 6 | Coordinator | Sequential merge in plan order (A → B → C); resolve SOFT conflicts |
| 7 | Coordinator | Worktree cleanup post-merge ([retrospective-checklist.md](retrospective-checklist.md)) |

**Agent does NOT merge.** Merge ordering depends on cross-agent context coordinator owns.

## Required prompt sections

```
## 1. Context
- Wave: {wave-name}
- Theme: {theme}
- Gap(s) you own: {GAP-XXX, GAP-YYY}
- Wave plan path: {documents/03-planning/waves/wave-...md}
- Why this exists: {1-2 sentences from gap problem statement}

## 2. Files you MUST touch
- {abs-path-1}
- {abs-path-2}
- (NEW) {abs-path-3}

## 3. Files you MUST NOT touch (other agents own them)
- {abs-path-other-agent}
- shared `application.yml` (lead owns per feedback_parallel_agent_strategy rule #2)
- ROADMAP.md (lead owns; you describe what changed, lead applies)

## 4. Branch + commit
- Branch name: feat/wave-{theme}-{gap-slug}
- Commit message format: `{type}({scope}): GAP-XXX — {what}`
- Conventional commits per CLAUDE.md
- DO NOT add Co-Authored-By trailer

## 5. Success criteria (measurable)
- [ ] Tests added/updated and green: {specific test class or path}
- [ ] {gap AC item 1 from gap file}
- [ ] {gap AC item 2}
- [ ] CI green on the branch (you wait for green before reporting back)
- [ ] Gap file Status updated per gap-done-discipline.md (DONE if all AC met, else PARTIAL)

## 6. Report back format
When done, reply with:
- PR number + URL
- Files changed (list)
- Test results (pass count / total)
- Any HARD blockers encountered (if blocked, do NOT silently skip — report it)
- Worktree path (so coordinator can clean up)
```

## Anti-patterns (BANNED)

| Anti-pattern | Why bad | Fix |
|--------------|---------|-----|
| "Based on your findings, fix the bug" | Delegates understanding; agent will diverge from coordinator's mental model | Pre-analyze; tell agent the diagnosis + the fix scope |
| "Do X correctly" / "make it nice" | No measurable success criteria; rework loop | Ship explicit AC checkboxes from gap file |
| Missing branch name | Agents create random branches → coordinator can't track / merge | Always specify exact branch name in prompt |
| "If you have time, also do Y" | Scope creep; agent goes wide instead of deep | One agent = one disjoint scope; Y goes to another agent OR next wave |
| "Update ROADMAP when done" | Race condition with other agents | Lead owns ROADMAP per rule #2; agent describes change, lead applies |
| Generic "follow project conventions" | Agent has no convo history; doesn't know what convention | Cite exact rule files: `.claude/rules/X.md §Y` |
| "Read the codebase first" | Wastes tokens; agent re-discovers what coordinator knows | Cite specific files agent should read (max 3-5) |
| Multi-paragraph prose with no structure | Agent skims and misses | Use sections + checkboxes per template above |

## Sample prompt skeleton (copy + fill `{placeholders}`)

```
You are Agent {LETTER} in Wave {WAVE-NAME}.

## Context
- Wave plan: {documents/03-planning/waves/wave-{date}-{theme}.md}
- Your gap(s): {GAP-XXX} ({1-line title})
- Theme: {theme}
- Project root: /home/nguyenvankiet/projects/2026-Kite-Class-Platform
- Worktree path: (assigned by isolation:worktree flag — verify with `pwd`)

## Files you MUST touch
- {abs-path-1} — {what change}
- (NEW) {abs-path-2} — {purpose}

## Files you MUST NOT touch
- {abs-path-other-agent-1}
- `documents/04-quality/gaps/ROADMAP.md` (lead owns)
- `application.yml` shared keys

## Branch + commits
- Branch: `feat/wave-{theme}-gap-{xxx}`
- Conventional commits, no Co-Authored-By trailer
- Commit message: `{type}({scope}): GAP-{XXX} — {what}`

## Success criteria
- [ ] {AC checkbox 1 from gap file}
- [ ] {AC checkbox 2}
- [ ] Tests green: `{maven/pnpm command}`
- [ ] CI green on branch (wait for green before reporting)
- [ ] Gap file `Status` flipped per `gap-done-discipline.md` §2

## Conventions to follow
- TDD per `.claude/skills/core/tdd-enforcement.md`
- `.claude/rules/design-patterns.md` if touching backend
- `.claude/rules/{relevant-rule}.md`
- Vietnamese commit body OK, English title

## Constraints
- DO NOT modify files outside your "MUST touch" list
- DO NOT push to main directly
- DO NOT merge your own PR
- DO NOT delete worktree (lead does post-merge)

## Report back
When CI is green and PR is open, reply with:
- PR number + URL
- Branch name
- Files changed (list)
- Tests: {N pass / M total}
- Worktree path
- Any blockers or follow-up gaps you'd file
```

## Prompt-quality calibration signals

Track per [retrospective-checklist.md](retrospective-checklist.md):
- **Clarification rounds = 0:** prompt was self-contained ✅
- **Rounds = 1:** minor missing context, fix template
- **Rounds ≥2:** prompt failure mode, root-cause + update agent template

Each agent template (`assets/agents/*.md`) versioned + dated; bump version when template gains new section based on retro lessons.

## Related

- [SKILL.md](../SKILL.md) — entry point Step 5
- [retrospective-checklist.md](retrospective-checklist.md) — capture clarification-round count
- `assets/agents/wave-coordinator-agent.md` — coordinator self-prompt
- `assets/agents/{docs-only,test-only,p3-cleanup,feature-tdd}-agent.md` — role-specific templates
- Memory `feedback_parallel_agent_strategy.md` rule #3 (worktree path emphasis), rule #10 (stash-dance recovery)
