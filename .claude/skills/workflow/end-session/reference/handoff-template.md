# Session Handoff Template

Template cho `documents/03-planning/session-handoffs/YYYY-MM-DD-{wave-or-scope}.md` per `/end-session` Step 2.5.

Loaded khi skill Step 2.5 fires. SKILL.md body giữ tight (<150 lines); detailed template ở đây.

---

## Template (copy + fill)

```markdown
---
title: Session handoff — <Wave name OR scope summary>
date: YYYY-MM-DD
session_scope: ~Xh
context_at_end: NN% Opus 4.7 1M
session_type: <fresh / continuation / hotfix / planning>
---

# Session handoff — YYYY-MM-DD <Wave/scope>

## Scope shipped

| Wave / Bucket | PRs merged | Status |
|---|---|---|
| <wave-1> | #NNNN, #NNNN | ✅ done / ❌ aborted / 🟡 partial |
| ... | ... | ... |

## Gaps DONE (N)

- **GAP-XXX** Title — `phase-X/closed/`
- ...

## Gaps improved (PARTIAL bumps)

- **GAP-XXX** Title: X% → Y% + reason

## Gaps NEW filed

- **GAP-XXX** Title (Priority) — root cause / context
- ...

## Lessons captured (session-internal)

1. Pattern lesson — not rule-class (rule-class = file via `incident-to-rule-pipeline.md`)
2. ...

## Stack state

- Local Docker: <N/N healthy> via `<command>`
- ⚠️ Known bugs (manual workarounds documented):
  - **GAP-XXX:** brief description + workaround command
- AWS: <stopped/running per CLAUDE.md mode>

## Pickup for next session

**Wave-N+1 ready (M buckets parallel — per Plan X locked):**

| Bucket | Gap | Scope | Module |
|---|---|---|---|
| A | GAP-XXX | scope summary | module |
| ... | ... | ... | ... |

**Active blockers:** <list with gap refs>
**Wave-N+2 queued:** <gap list>

## Start next session

\`\`\`
/start-session
# Then fire wave-N+1 manually OR /loop continue per plan
\`\`\`

## References

- Plan: `documents/03-planning/plans/<active-plan>.md`
- Prior handoff: <link to previous session-handoff if continuation>
- Wave history: `.claude/skills/quality/wave-pack-planner/data/wave-history.jsonl` (entries appended this session)
```

---

## Section requirements

| Section | Mandatory? | Skip when |
|---|---|---|
| Scope shipped (PR/wave table) | YES | Session = read-only / planning-only |
| Gaps DONE / improved / NEW | YES | No gap-state changes (rare) |
| Lessons captured | YES | Use "(none)" if truly nothing learned |
| Stack state | YES | Always — even if "no change" |
| Pickup for next session | YES | Even if "fresh slate" — say so |
| Start next session commands | YES | Concrete commands save next-session time |

---

## Filename slug convention

- `YYYY-MM-DD-wave-{name}-{closure|continuation}.md` — wave-scoped session
- `YYYY-MM-DD-{topic}.md` — non-wave session (vd `hotfix-prod-incident-Y`, `audit-suite-Z`)
- `YYYY-MM-DD-eod-{N}-wave-shipped.md` — multi-wave end-of-day summary

---

## Examples

See `documents/03-planning/session-handoffs/` for prior sessions. Most-recent samples:

- `2026-06-02-wave-5-6-local-doable-harvest.md` — multi-wave continuation
- `2026-06-01-wave-meta-8-catalog-apply-closure.md` — wave closure
- `2026-06-01-eod-4-wave-shipped.md` — end-of-day rollup
