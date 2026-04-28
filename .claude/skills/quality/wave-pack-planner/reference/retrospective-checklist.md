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
