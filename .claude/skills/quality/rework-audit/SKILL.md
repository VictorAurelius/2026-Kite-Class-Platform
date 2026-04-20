---
name: rework-audit
description: "Dùng khi user nói 'rework audit', 'PR nào cần rework', 'context-degraded PRs', 'chất lượng PR gần đây', 'retroactive audit', 'audit lại wave X'. Re-audit PRs merged during high-context-pressure sessions (GAP-199). Output: prioritized rework backlog (P0/P1/P2) feeding audit-to-gap-pipeline."
user-invocable: true
argument-hint: "<PR-range | wave-N> [--phase1|--phase2]"
---

# /rework-audit — Retroactive Audit for Context-Degraded PRs

Detect + re-audit PRs likely produced under context degradation, output rework backlog.

## When to use

- Post-wave retro: "did any merged PRs have quality drift?"
- After `/repo-status` flags scoring inconsistency
- When user reports "PR X looks off, was it the tired-session one?"
- Quarterly sweep of last 50-100 merged PRs

## Inputs

- **PR list**: range (`290-310`), wave label (`wave-8b`), or `--recent N`
- **Context signals**: `documents/03-planning/pr-logs/PR-*.json`, session-lock history (if present), audit evidence files

## Process

### Step 1 — Detection (heuristic filter)

Run heuristics from `reference/heuristics.md`:

1. PR log analysis — missing audits, low compliance score, late-night merge timestamps
2. Merge clustering — 3+ PRs merged within 30 min = fatigue window
3. Session-lock cross-reference — PRs merged during lock with `estimated_turns > 40`
4. Audit evidence gaps — PRs without required UI/business/security audit within 7-day window

Output: candidate PR list with **context-pressure score** 0-10 (higher = more suspect).

### Step 2 — Triage (per candidate PR)

For each candidate score ≥5:

1. Pull PR details: `gh pr view {N} --json files,body,mergedAt,additions,deletions`
2. Spot-check against original gap/rule the PR claimed to close
3. Re-run relevant audit skill narrowly:
   - Frontend changes → `/ui-review` on touched screens only
   - Business logic → `/business-logic-audit` on touched rules
   - API changes → `/api-contract-audit`
4. Compare re-audit score vs PR-log compliance score — delta >15 pts = rework signal

### Step 3 — Score rework severity

Use rubric from `reference/scoring-rubric.md`:

| Severity | Trigger | Action |
|----------|---------|--------|
| **P0** | Functional regression, broken AC, security/compliance miss | Immediate gap + hotfix PR |
| **P1** | Missing tests, docs drift, incomplete audit, rule-doc desync | Gap next sprint |
| **P2** | Style, minor naming, comment gaps | Batch into quarterly cleanup |

### Step 4 — Emit gaps via pipeline

Each rework item → gap file per `.claude/rules/audit-to-gap-pipeline.md`:

1. **Duplicate check** — grep `documents/04-quality/gaps/` cho overlapping topic
2. **New gap** — format: `GAP-XXX-rework-PR{N}-{short-topic}.md`
3. **Memory save** — ONLY if pattern repeats (e.g., "3+ degraded PRs missed API contract audit")
4. **ROADMAP update** — assign to sprint by severity

### Step 5 — Output backlog

Single markdown table, sortable by severity:

```markdown
| Rework-ID | PR | Severity | Pattern | Suggested fix | Gap file |
|-----------|----|----|---------|--------------|----------|
| RW-01 | #362 | P0 | Missing quality gate run | Re-run + patch | GAP-XXX |
| RW-02 | #367 | P1 | Docs desync rules.md | Update rules | GAP-XXX |
```

Also emit summary: "N PRs audited, M rework items found, X P0 / Y P1 / Z P2."

## Phase 1 vs Phase 2

- **Phase 1 (this skill delivery):** heuristics + rubric documented, manual PR selection, manual audit execution. Pilot run on Wave 6-8 **deferred** (too heavy for single PR). See `reference/heuristics.md` §Phase-2 queue.
- **Phase 2 (deferred):** automate detection via `scripts/detect-candidates.sh`, integrate into `/repo-status`, add `post-wave-audit-mandate.md` §4 checklist item for rework sweep.

## Detection heuristics (quick summary)

Full detail: `reference/heuristics.md`

| Signal | Weight | Source |
|--------|:------:|--------|
| PR log compliance score <3/5 | 3 | `pr-logs/PR-*.json` |
| Merged in fatigue cluster (3+ in 30min) | 2 | `mergedAt` timestamps |
| Missing required audit evidence | 3 | `documents/04-quality/audits/` dates |
| Session-lock >40 turns at merge | 2 | `.claude/session-locks/*.lock` (if archived) |
| Late-night merge (22:00-06:00) | 1 | timezone-adjusted `mergedAt` |
| Post-compact context degradation flag | 2 | session log (if captured) |
| Rule-doc desync (code vs rules.md differ) | 3 | grep-based check |

Score ≥5 = candidate for deep re-audit.

## Gotchas

- PR logs missing turn-count today → heuristic relies more on compliance score + merge clustering. Add turn logging in GAP-193 skill later (Phase 2).
- Session-lock history NOT persisted by default — Phase 2 adds archival hook
- `gh pr view --json` mergedAt is UTC — convert to local for fatigue detection
- Don't over-flag: a 3-PR cluster of docs-only PRs is fine (low risk surface)
- **Every rework item becomes a gap** — never fix PR directly (per `audit-to-gap-pipeline.md`)
- Rework audit ITSELF is an audit output — subject to `output-review-mandate.md`: record session ID, heuristics version, reviewer

## Related

- GAP-199 (this skill's origin)
- GAP-193 (sister — session orchestration provides degradation signals)
- `.claude/rules/audit-to-gap-pipeline.md` (how gaps flow)
- `.claude/rules/post-wave-audit-mandate.md` (audit cadence)
- `.claude/skills/workflow/pr-health.md` (overlapping PR compliance check)
- Memory `feedback_audit_calibration` (self-audit overstates 15-20 pts)

## Skill contents

- `SKILL.md` — this file
- `reference/heuristics.md` — detection criteria + Phase 2 queue
- `reference/scoring-rubric.md` — P0/P1/P2 severity definitions
- `scripts/` — (Phase 2) `detect-candidates.sh` auto-filter

## Example runs (scenarios, not pilot)

### Scenario A — Single PR suspect
```
User: "PR #362 looks off, was it rework?"
→ /rework-audit 362
→ heuristic score 7 (no UI audit, merged after 3 other PRs in 20min, turn ~50)
→ re-audit UI narrowly → finds 2 screens under-scored
→ emits GAP-XXX-rework-PR362-ui-rescore (P1)
```

### Scenario B — Wave sweep
```
User: "audit lại wave 7"
→ /rework-audit wave-7
→ filter PRs with wave/07 label, 12 candidates, 4 score ≥5
→ deep audit 4 → 2 rework items (1 P0, 1 P1)
→ emits 2 gap files, updates ROADMAP
```
