# Wave Retrospective Checklist

Sau wave merge, capture lessons + log data trước khi đóng wave. ~10 min process.

Run AFTER:
- All N agent PRs merged
- ROADMAP closure entry written
- Worktrees cleaned

## Required questions (markdown checklist)

Copy block dưới vào wave plan `## Lessons-learned` section, fill in answers.

```markdown
## Lessons-learned ({wave-name}, completed {YYYY-MM-DD})

### Worktree isolation
- [ ] Did `isolation: "worktree"` hold? (no agent saw another's uncommitted state)
- [ ] If contamination: which agents, which files, recovery steps applied?
  (per feedback_parallel_agent_strategy.md rule #10 stash-dance)

### File-overlap analysis accuracy
- [ ] Predicted SOFT conflicts: {list}; actual SOFT conflicts at merge: {list}
- [ ] Predicted HARD conflicts: {list}; actual HARD conflicts: {list}
- [ ] Any UNPREDICTED conflicts? (calibration miss → update file-overlap-algorithm.md)
- [ ] False-positive conflicts predicted but didn't materialize?

### Wall-clock vs estimate
- [ ] Estimated wall-clock: {min}; actual: {min}
- [ ] Variance source if >20% off: agent rework / CI flake / merge friction / other?
- [ ] Agent-only time (excluding user response delay): {min} — separate metric per SKILL.md gotcha

### Agent prompt quality
- [ ] Each agent's clarification rounds: A={n}, B={n}, C={n}
- [ ] Rounds=0 → prompt self-contained ✅
- [ ] Rounds≥1 → which agent template needs update? (`assets/agents/*.md`)

### Token cost
- [ ] Total tokens consumed (sum of agent transcripts + coordinator)
- [ ] Tokens per gap closed: {total / N}
- [ ] Compare to last wave: trending up/down/stable?

### Gap closure quality (per gap-done-discipline.md)
- [ ] Each gap Status flip honored discipline (no "deferred" in DONE log)?
- [ ] Any gap should've been PARTIAL but flipped DONE? (anti-pattern)
- [ ] Follow-up gaps filed for any PARTIAL items?

### Cleanup hygiene (per feedback_parallel_agent_strategy.md rule #6)
- [ ] All N worktrees removed: `git worktree list` shows main only
- [ ] All N local branches deleted: `git branch | grep -c feat/wave-` = 0
- [ ] All N remote branches deleted: `git ls-remote --heads origin | grep -c feat/wave-` = 0
- [ ] Stale stashes cleaned: `git stash list | grep -c agent-` = 0

### Wave-pack data point
- [ ] `data/wave-history.jsonl` entry appended (schema below)
- [ ] ROADMAP "Current Status Snapshot" updated with cluster outcome
- [ ] If novel pattern emerged → memory entry filed
```

## Where to log: `data/wave-history.jsonl`

Append-only JSON-lines file. **One JSON object per line** (no array wrapper, no pretty-print).

Schema:

```json
{
  "wave": "observability-1",
  "date": "2026-04-28",
  "theme": "observability",
  "gaps": ["GAP-121", "GAP-143", "GAP-144"],
  "agents": 3,
  "agent_roles": ["docs-only", "feature-tdd", "feature-tdd"],
  "wall_clock_min": 75,
  "estimated_serial_min": 360,
  "speedup_ratio": 4.8,
  "tokens_total": 0,
  "tokens_per_gap": 0,
  "clarification_rounds": [0, 0, 1],
  "predicted_conflicts": {"soft": ["values.yaml"], "hard": []},
  "actual_conflicts": {"soft": [], "hard": []},
  "lessons": [
    "values.yaml SOFT predicted but auto-merged cleanly",
    "Agent C had 1 clarification round — feature-tdd template missing 'wait for green CI' criterion"
  ],
  "follow_up_gaps_filed": [],
  "novel_pattern_memory": null
}
```

**Append example:**

```bash
cat >> .claude/skills/quality/wave-pack-planner/data/wave-history.jsonl <<'EOF'
{"wave":"observability-1","date":"2026-04-28","theme":"observability","gaps":["GAP-121","GAP-143","GAP-144"],"agents":3,"wall_clock_min":75,"estimated_serial_min":360,"speedup_ratio":4.8,"clarification_rounds":[0,0,1],"predicted_conflicts":{"soft":["values.yaml"],"hard":[]},"actual_conflicts":{"soft":[],"hard":[]},"lessons":["values.yaml SOFT auto-merged","Agent C 1 clarification round"]}
EOF
```

## Where to surface: ROADMAP

Add entry to top of `documents/04-quality/gaps/ROADMAP.md` `## 🎯 Current Status Snapshot`:

```markdown
**YYYY-MM-DD (Wave {Theme} SHIPPED — {N}-agent parallel cluster pack, {M} PRs merged ~{X} min wall-clock):**
{1-2 sentences} PRs: foundation #{N1} → Agent A #{N2} ({GAP-XXX status}) → Agent B #{N3} → Agent C #{N4}.
Cluster status: {GAP-XXX → 🟢 DONE; GAP-YYY → ...}.
Counts: {N OPEN → M OPEN}.
**Cadence:** {N} gaps closed in {X} min vs {Y} min serial = ~{Z}x speedup.
{Lessons-learned headline if novel}.
Worktrees + branches cleaned post-merge per `feedback_parallel_agent_strategy.md` rule #6.
```

Then rotate `### Active wave queue (clustered)` table:
- Strikethrough shipped wave (`~~Theme — Wave N~~`), mark `✅ SHIPPED YYYY-MM-DD`
- Promote next cluster to `**bold (next)**`

## 4+-agent local-state hazards (added 2026-04-29 from Wave 13 lessons)

When wave-pack uses 4 or more parallel worktree-isolated agents (Wave Legal-BRD Phase 1 was first to observe these), three local-state hazards surface that don't appear in 3-agent waves. The cluster of merged branches + still-active worktrees + invisible coordinator `cd` interactions creates failure modes that need explicit recovery procedures.

Apply this checklist when reviewing wave closure for any wave with N>=4 agents.

### Hazard 1 — Worktree-held branches block `gh pr merge --delete-branch`

**Symptom:** running `gh pr merge <N> --squash --delete-branch` for the *last* PR in a 4-agent wave succeeds upstream, but the post-merge `git checkout main` step fails locally with:

```
fatal: 'main' is already used by worktree at /home/.../worktrees/agent-XXXX
```

**Why:** the 4 agent worktrees are still on detached HEADs of their (now-merged) feature branches. `gh pr merge` post-merge tries to switch the *main* repo's working copy to `main`, but main is "claimed" by a worktree somewhere on the filesystem.

**Recovery (inside main repo, NOT inside a worktree):**

```bash
git fetch origin main
git reset --hard origin/main
```

This forces main repo to track `origin/main` regardless of worktree claims.

**Mitigation:**
- **Option A (preferred):** prune worktrees BEFORE merging the final PR of a 4+-agent wave:
  ```bash
  git worktree list                     # inspect
  git worktree remove --force <path>    # for each agent worktree
  git worktree prune
  ```
- **Option B (acceptable):** accept local stale state until the dedicated cleanup task; coordinator runs Recovery sequence above as task #N in queue
- **Option C (avoid):** trying to checkout main from inside a worktree — re-triggers the same error

**Worked example:** Wave Legal-BRD Phase 1 (2026-04-29) — `gh pr merge 691` (the 4th agent's PR) post-merge checkout failed because all 4 agent worktrees still held HEAD refs of merged branches. Recovery via `git fetch origin main && git reset --hard origin/main` from main repo path took ~30 seconds.

### Hazard 2 — Coordinator-side `cd` contamination

**Symptom:** coordinator-issued `git checkout -b <branch>` for the closure / cleanup PR creates the branch INSIDE one of the agent worktrees instead of the main repo. Subsequent commits on that branch land referenced from the wrong worktree, and post-merge inspection shows commits on a branch with the right name but wrong workspace lineage.

**Why:** at some earlier point in the session, a `cd <agent-worktree>` ran (often invisibly — embedded in a tool call's `cwd` or a chained command). Subsequent shell commands inherit that cwd until something explicitly changes it. The coordinator's mental model says "I'm in main repo" but the shell's `pwd` says otherwise.

**Recovery:**

```bash
# 1. Get back to main repo explicitly (use absolute path or known-good relative)
cd /home/.../2026-Kite-Class-Platform   # main repo path, NOT a worktree path
pwd | grep -v worktrees/                 # MUST be empty match (i.e. NOT in worktree)

# 2. The mis-created branch name is now held by the worktree — pick a different name
git checkout -b <new-branch-name>        # NOT the original contaminated name
```

If the contaminated branch already has commits you want to keep, cherry-pick from it:

```bash
git log <contaminated-branch> --oneline | head -5    # find commits to rescue
git cherry-pick <SHA1> <SHA2>                         # apply onto new branch
```

**Mitigation:**
- **Verify before every branch op:** `pwd | grep -v worktrees/` (must NOT match `worktrees/`)
- **Use explicit `git -C`:** `git -C /home/.../2026-Kite-Class-Platform checkout -b <branch>` makes the working dir explicit and bypasses cwd entirely
- **Audit trail:** record `pwd` output in coordinator notes before / after each major branch operation in 4+-agent waves

**Worked example:** Wave Legal-BRD Phase 1 (2026-04-29) — coordinator's closure-branch creation landed inside Agent C's worktree because an earlier `cd` (during the privacy-skeleton contamination recovery — different incident, see also `feedback_worktree_absolute_path_contamination.md`) was never reverted. Recovery via explicit `cd <main-repo-path>` + new branch name took ~5 minutes including git log archeology.

### Hazard 3 — `git reset --hard origin/main` recovery NUKES uncommitted dirty files

**Symptom:** while applying Hazard 1's recovery (`git reset --hard origin/main`), pre-existing dirty modifications in the main repo working tree (NOT from this session — could be from prior sessions, IDE auto-fixes, manual edits not yet staged) get silently wiped. Reflog only tracks committed history; uncommitted changes are unrecoverable post-reset.

**Why:** `git reset --hard` is by design destructive to working-tree state. Reflog (`git reflog`) tracks `HEAD` movements, NOT working-tree contents. `git fsck --lost-found` finds blobs only if they were ever staged; truly uncommitted files leave no trace.

**Mitigation (run BEFORE the `git reset --hard`):**

```bash
# Capture EVERYTHING — committed dirty + untracked + ignored-but-tracked
git stash push --include-untracked --message "pre-reset-recovery $(date -Iseconds)"

# Now safe to reset
git fetch origin main
git reset --hard origin/main

# Restore captured state
git stash pop                            # if no conflicts
# OR if conflicts: git stash apply + manual conflict resolution
```

If `git stash pop` fails with conflicts, the stash is preserved (`git stash list` shows it) — restore manually file-by-file via `git checkout stash@{0} -- <path>`.

**Worked example:** No incident in Wave 13 itself (coordinator's main repo was clean), but the hazard is real for any solo-dev session that has been running >1 day with accumulated unsaved IDE state. Documented preemptively because Reflog-cannot-help users typically discover this only AFTER losing work.

### Cross-references

- Sister memory: `feedback_worktree_absolute_path_contamination.md` (in `/home/nguyenvankiet/.claude/projects/-home-nguyenvankiet-projects-2026-Kite-Class-Platform/memory/`) — covers Wave DR/Backup 2026-04-28 (Agent B commit landed on Agent C's branch via absolute-path bypass) AND Wave Legal-BRD Phase 1 2026-04-29 (Agent C Write tool landed at main worktree path, caught pre-commit by `pwd` grep). Hazards 1-3 above are the same family of failures viewed from coordinator side; the memory is from agent side.
- Wave 13 worked examples: see `data/wave-history.jsonl` `wave-2026-04-29-legal-brd-phase1` entry (`worktree_contamination: true, worktree_contamination_caught_pre_commit: true`)
- Mitigation rule: `feedback_parallel_agent_strategy.md` rule #6 (cleanup hygiene) + rule #10 (stash-dance) — both pre-date 4-agent threshold; Hazards 1-3 extend them for 4+-agent specifics

### When to escalate from this section to a rule

If Hazards 1-3 recur in >=2 waves AFTER mitigation is documented, escalate per §"When to escalate" matrix below. Likely escalation: new rule `.claude/rules/parallel-agent-discipline.md` formalizing pre-final-merge worktree pruning + explicit `git -C <main>` for branch ops in 4+-agent waves.

---

## When to escalate

If patterns repeat across waves:

| Pattern (≥2 waves) | Escalation |
|--------------------|-----------|
| Same anti-pattern in agent prompts | Update `agent-spawning-template.md` + relevant `assets/agents/*.md` |
| Same file-overlap miscall | Update `file-overlap-algorithm.md` risk classification rules |
| Worktree contamination keeps happening | Strengthen agent prompt language; consider hook OR raise to `.claude/rules/parallel-agent-discipline.md` (new rule) |
| Wall-clock estimate consistently off >30% | Recalibrate cluster heuristics in `cluster-pattern.md` |
| Cluster theme always > 5 agents | Lower agent cap from 5 to 4; document in `cluster-pattern.md` |

Escalation path → propose rule update via [`.claude/rules/rule-change-process.md`](../../../../rules/rule-change-process.md):
1. Branch `rule/{slug}`
2. Cite ≥2 wave-history.jsonl entries as evidence
3. Bump version per semver (PATCH/MINOR/MAJOR)
4. Update `## Log` of impacted rule with motivation

## Memory entry trigger

File new memory entry under `feedback_*` if:
- Novel anti-pattern discovered (e.g. new contamination mode, agent role mismatch)
- Tool / process bug found (e.g. CI flake correlated with parallel merges)
- Cross-cutting insight (e.g. "K-12 themed waves always have 4 agents not 3")

Memory dir: `/home/nguyenvankiet/.claude/projects/-home-nguyenvankiet-projects-2026-Kite-Class-Platform/memory/`. Update `MEMORY.md` index.

## Related

- [SKILL.md](../SKILL.md) — entry point
- [agent-spawning-template.md](agent-spawning-template.md) — calibration source
- [file-overlap-algorithm.md](file-overlap-algorithm.md) — calibration source
- [cluster-pattern.md](cluster-pattern.md) — heuristic recalibration
- Memory `feedback_parallel_agent_strategy.md` — 10 hard rules
- Rule `.claude/rules/rule-change-process.md` — escalation pathway

## Log

- 2026-04-29 — Added §4+-agent local-state hazards section (3 hazards from Wave 13 retro). Reviewer: Agent D (Wave Legal-BRD Phase 1.5 meta track).
