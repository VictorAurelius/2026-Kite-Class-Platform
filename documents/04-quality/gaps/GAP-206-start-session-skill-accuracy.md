# GAP-206: `/start-session` skill reports wrong wave + blockers + missing recent context

**Status:** 🟢 DONE 2026-04-24
**Priority:** 🟠 P1 (Meta — skill blindspot per `.claude/rules/meta-gap-priority.md` §3)
**Domain:** Workflow / Meta (skill `/start-session`)
**Detected:** 2026-04-24 during user /start-session run after Dependabot security session close
**Related PRs:** fix PR incoming same session
**Related Docs:**
- `.claude/skills/workflow/start-session/SKILL.md`
- `.claude/skills/workflow/start-session/scripts/collect-state.sh`
- `documents/04-quality/gaps/ROADMAP.md` (source of truth for wave + blockers)

## Current State (verified 2026-04-24)

| Piece | File / Logic | Status |
|-------|--------------|--------|
| `collect-state.sh` wave detection | Line 41 — `ls -t waves/*.md \| head -1` | ❌ Wrong — mtime ≠ current active wave |
| Blocker gaps detection | Line 60-62 — `grep OPEN + P0/P1 \| head -3` | ❌ Wrong — alphabetical not priority |
| Recent context (merged PRs, commits) | — | ❌ Missing — skill doesn't surface |
| `/repo-status` integration | — | ❌ Missing — skill reruns minimal CI check instead |
| Scratchpad awareness | — | ❌ Missing — `action-2.md` flagged as "uncommitted" generic |
| ROADMAP parsing | — | ❌ Missing — skill ignores ROADMAP.md snapshot section |

**Sample wrong output (2026-04-24 user session):**
```
Wave: wave-05-decision-guide (newest plan in documents/03-planning/waves/)
Blocker gaps: GAP-006 (Gemma-4 upgrade), GAP-017 (AI billing), GAP-019 (AI observability)
```

**What ROADMAP.md says is actually current (§Current Status Snapshot):**
- Next recommended wave: **GAP-047** document generation
- Block GA blockers (priority order): GAP-047 → GAP-046 → GAP-016 → GAP-011 → GAP-014 → GAP-005
- Wave 5 "decision-guide" is an OLD plan (wave-05 file edited recently for bookkeeping → got `ls -t` top rank)

**Grep commands verifying bugs:**
```bash
# Bug 1: ls -t picks mtime, not semantic "current"
ls -t documents/03-planning/waves/*.md | head -1
# → wave-05-decision-guide.md (edited recently, not current work)

# Bug 2: head -3 without sort = alphabetical
grep -l "P0\|P1" documents/04-quality/gaps/GAP-*.md | head -3
# → GAP-006, GAP-017, GAP-019 (alphabetical first 3, not priority)

# What SHOULD drive output (ROADMAP.md):
grep -A 10 "Block GA" documents/04-quality/gaps/ROADMAP.md | head -15
```

## Problem

`/start-session` skill output misleads users about:
1. **Which wave is active** — reports file by mtime instead of ROADMAP-designated next wave
2. **Which gaps block progress** — reports alphabetical first 3 instead of top-priority blockers
3. **Recent session context** — no mention of merged PRs / commits from last 1-3 days → user re-discovers work already done
4. **Real repo health** — ignores `/repo-status` factors (Security, Audit Gaps, Stale Branches)

Impact: session kickoff wastes tokens re-briefing Claude about context that exists in files. User had to manually critique output + ask "không hợp lý lắm nhỉ?" → exposes skill is force-multiplier DEBT (per `meta-gap-priority.md` §5.1).

## Context

Session flow 2026-04-23 → 2026-04-24 closed 11 PRs (GAP-202/203/204/205 + axios bumps + CI policy + Dependabot guide + solo-dev CI mode). Significant context for Claude. User `/clear` + `/start-session` lost all of it because skill doesn't surface recent merges or link to memories.

## Evidence

### Bug 1 — wave detection
```bash
ls -t documents/03-planning/waves/*.md | head -3
# wave-05-decision-guide.md       ← returned (wrong)
# wave-04-security-compliance.md
# wave-saas-data-safety.md
```
Meanwhile `documents/04-quality/gaps/ROADMAP.md` line ~32:
> **Next recommended wave:** Wave 5 **GAP-047** document generation

Coincidence for this case (Wave 5 is correct name), but **wrong file** (`wave-05-decision-guide` ≠ document generation). Detection by filename is broken.

### Bug 2 — blocker detection
```bash
$ grep -l '^\*\*Status:\*\* 🔵 OPEN' documents/04-quality/gaps/GAP-*.md \
  | xargs grep -l '^\*\*Priority:\*\* 🔴 P0\|^\*\*Priority:\*\* 🟠 P1' \
  | head -3
documents/04-quality/gaps/GAP-006-upgrade-to-gemma-4.md
documents/04-quality/gaps/GAP-017-ai-usage-billing-integration.md
documents/04-quality/gaps/GAP-019-ai-observability-cost-monitoring.md
```
These are alphabetical first 3. Real blockers per ROADMAP meta-priority: GAP-047, -046, -016, -011, -014, -005.

### Bug 3 — recent context missing
```bash
$ git log --oneline main --since="2 days ago" | head -5
0716deae ci: solo-dev mode — remove push:main trigger (#467)
8ed8b83b docs(guide): comprehensive Dependabot guide (#466)
29290347 docs(workflow): GAP-205 CI history retention (#465)
d20a2054 Merge PR #462 axios bump
1303b6bc ci: raise image size limit 220MB (#464)
```
None of this appears in `/start-session` output. User who `/clear`'d has to re-discover via `git log` or remembering.

### Bug 4 — /repo-status not integrated
Skill reports `CI main: ✅ green` (1-dim). `/repo-status` has 4 factors (CI + PRs + Gaps + Security) + level output. Inclusion would give fuller health.

## Proposed Fix

### Stage A — this PR
Update `collect-state.sh` + `SKILL.md`:

1. **Wave detection** → parse ROADMAP:
```bash
CURRENT_WAVE=$(grep -oP '(?<=Next recommended wave:).*' documents/04-quality/gaps/ROADMAP.md \
  | head -1 | sed 's/[*_]//g' | xargs)
# fallback to ls -t if grep returns empty
```

2. **Blocker gaps** → parse ROADMAP "Block GA" table:
```bash
BLOCKERS=$(awk '/GA Blockers remaining/,/Priority rule/' documents/04-quality/gaps/ROADMAP.md \
  | grep -oE '\*\*GAP-[0-9]+\*\*|GAP-[0-9]+' | sed 's/\*//g' | sort -u | head -5 | tr '\n' ';')
```

3. **Recent context** → last 5 merged commits on main:
```bash
RECENT=$(git log main --since="3 days ago" --oneline --merges | head -5 | tr '\n' '§')
```

4. **Integrate /repo-status** — call `scripts/repo-status.sh --json` and extract level + security summary:
```bash
RS_JSON=$(bash scripts/repo-status.sh --json 2>/dev/null)
RS_LEVEL=$(echo "$RS_JSON" | jq -r '.level')
RS_CVE=$(echo "$RS_JSON" | jq -r '.security.high + .security.critical // 0')
```

5. **Scratchpad awareness** — if `documents/action-2.md` is the ONLY dirty file, tag as `scratchpad`:
```bash
DIRTY_FILES=$(git diff --name-only)
if [ "$DIRTY_FILES" = "documents/action-2.md" ]; then
  BRANCH_STATE="clean (scratchpad only)"
fi
```

6. **Update SKILL.md output template** to include the new fields.

### Stage B — follow-up (future PR)
- Add links to relevant memory files in output (e.g., "recent work → see memory: feedback_dependabot_first_run")
- Surface open GitHub security alerts count in blockers if security factor != GREEN
- Per-wave progress indicator (N of M sub-PRs done)

## Acceptance Criteria

- [x] `collect-state.sh` parses ROADMAP.md for wave + blockers (not mtime/alphabetical)
- [x] Output includes "Recent merges" section with last 5 merged PRs
- [x] Output includes `/repo-status` level + security summary
- [x] `documents/action-2.md` alone = "clean (scratchpad only)", not generic "dirty"
- [x] `SKILL.md` output template updated to reflect new fields
- [x] Test: re-run `/start-session` post-fix — correct wave (GAP-047) + correct top blockers (GAP-047/-046/-016/-011/-014/-005) + recent PR context

## Related

- `meta-gap-priority.md` §5.1 — skill blindspot is force-multiplier debt
- `feedback_repo_status_security_coverage.md` — health-checks must integrate Security
- `feedback_dependabot_first_run.md` — recent auto-tooling context should surface in session kickoff
- GAP-202 — similar pattern (`/repo-status` skill had security blindspot, fixed in PR #423)

## Log

- **2026-04-24** — Gap filed after user ran `/start-session` post-Dependabot-session-close and received misleading output. Asked "không hợp lý lắm nhỉ?" — triggered triage. 4 bugs identified: wave mtime, blockers alphabetical, no recent context, no `/repo-status` integration. Fix in same session.
- **2026-04-29 (status sync)** — Truth-up: PR #468 merged 2026-04-24 closing this gap (collect-state.sh now parses ROADMAP, surfaces recent merges, integrates repo-status, scratchpad-aware; SKILL.md template updated). Status header drifted from reality. Per memory feedback_post_merge_doc_sync.md, gap closure doc-sync should happen in same PR as the closing merge — backfilled here under Wave Meta-Gov 2 Agent C housekeeping. All 6 ACs verified shipped via live script run.
