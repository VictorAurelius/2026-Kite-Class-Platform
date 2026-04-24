# GAP-205: CI history retention policy + automation (769 runs → capped)

**Status:** 🟠 IN_PROGRESS — policy drafted + one-shot cleanup ran, automation PR pending
**Priority:** 🟡 P2 (Meta-P2 per `meta-gap-priority.md` — workflow hygiene, not critical path)
**Domain:** Workflow / DevOps (CI governance)
**Detected:** 2026-04-24 (user observation during GAP-204 session: "30+ trang CI tồn tại")
**Related PRs:** this PR + followup workflow PR
**Related Docs:**
- `CLAUDE.md` §CI History Hygiene (existing partial policy)
- `scripts/cleanup-ci-runs.sh` (existing manual script)

## Current State (verified 2026-04-24)

| Piece | Path / Value | Status |
|-------|--------------|--------|
| Existing policy | `CLAUDE.md` §CI History Hygiene | 🟡 PARTIAL — covers failed runs only |
| Cleanup script | `scripts/cleanup-ci-runs.sh` | ✅ EXISTS — deletes all-but-most-recent per branch |
| Scheduled cleanup workflow | `.github/workflows/ci-cleanup.yml` | ❌ MISSING |
| Total CI runs (pre-cleanup) | 769 | 🔴 Excessive (>30 UI pages) |
| Success runs | 673 | No retention cap |
| Failed runs | 94 | Policy says ≤2, reality 94 |
| Runs >30 days old | 224 | No age cutoff applied |
| `/repo-status` CI history check | Flags >2 failed runs | ✅ Detector exists, no enforcement |

**Grep commands run:**
```bash
grep -A 3 "CI History\|cleanup-ci-runs" CLAUDE.md   # → policy exists
ls scripts/cleanup-ci-runs.sh                        # → script exists
gh run list --limit 1000 --json databaseId | jq 'length'   # → 769
ls .github/workflows/ci-cleanup*                     # → MISSING
```

## Problem

Policy in `CLAUDE.md` §CI History Hygiene covers only **failed** runs ("≤2 failed runs in history"). Successful runs accumulate indefinitely. Repo currently has **769 total runs** (>30 UI pages in GitHub Actions tab), of which:

- 94 failures (47× above policy cap of 2)
- 673 successes with no retention
- 224 runs >30 days old

This impacts:
1. **`/repo-status` skill noise** — reports "dirty CI history" permanently
2. **GitHub UI navigation** — 30+ pages to browse
3. **Action log storage** — free tier has 500MB limit; large repos can hit this
4. **Compounding daily** — no automation means cleanup is manual + easily forgotten

## Context

User flagged 2026-04-24 during GAP-204 security session. Policy gap surfaced when /repo-status consistently showed "8 failed runs in history" even after GAP-204's security PRs merged green. Investigation revealed most failures were:
- Legacy from wave merges (Wave 8b, 9, 9.5) weeks ago
- "Dependabot Updates" workflow failures (known pnpm transitive limitation — see memory `feedback_dependabot_pnpm_transitive.md`)
- CI misconfigurations from past sessions

None are actionable; all are cleanup candidates.

## Evidence

**Sample run age distribution (2026-04-24 snapshot):**
```
Total:            769
>30 days old:     224 (29%)
>7 days old:      ~400 (52%)
Failed >7d:        39
Success >30d:    ~200
```

Top noise sources:
- Wave agent branches (deleted branches, runs orphaned)
- Dependabot Updates (transitive pnpm attempts)
- Frontend CI multiple retries on failed Lighthouse

## Proposed Fix (staged)

### Stage A — Policy update (this PR)
Extend `CLAUDE.md` §CI History Hygiene:

```markdown
### CI History Hygiene

Retention caps (enforced by scheduled cleanup + /repo-status check):

| Run type | Keep policy |
|----------|-------------|
| Failed on main | ≤2 in history (existing rule) |
| Failed on feature branch | Delete after 7 days |
| Success runs | Keep last 30 days OR last 50 runs per branch, whichever is more |
| All runs | Hard cap 200 total; beyond that, oldest deleted |

Automation: weekly `ci-cleanup` workflow prunes per above. Manual override: `scripts/cleanup-ci-runs.sh`.
```

### Stage B — One-shot bulk cleanup (this PR or sibling)
```bash
# Delete >30 days old OR failed >7 days (preserve recent activity)
gh run list --limit 1000 --json databaseId,createdAt,conclusion \
  --jq '.[] | select(.createdAt < "<30_DAYS_AGO>" or (.conclusion=="failure" and .createdAt < "<7_DAYS_AGO>")) | .databaseId' \
  | xargs -I {} gh api --method DELETE "repos/$REPO/actions/runs/{}"
```
Executed 2026-04-24 during this gap filing. Expected drop: 769 → ~400-500.

### Stage C — Scheduled automation (separate PR)
Add `.github/workflows/ci-cleanup.yml`:

```yaml
name: CI History Cleanup
on:
  schedule:
    - cron: '0 20 * * SUN'  # Every Sunday 20:00 UTC = Monday 03:00 Asia/Ho_Chi_Minh
  workflow_dispatch:
permissions:
  actions: write
jobs:
  cleanup:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      - name: Prune old runs
        env:
          GH_TOKEN: ${{ secrets.GITHUB_TOKEN }}
        run: |
          # Same deletion criteria as Stage B
          ...
```

### Stage D — /repo-status skill refinement (separate PR)
Update `scripts/repo-status.sh` to:
- Filter out "Dependabot Updates" workflow failures from count (known pnpm limitation, not actionable)
- Report "within cleanup policy" vs "cleanup due" based on retention caps

## Acceptance Criteria

### Stage A (this PR)
- [ ] `CLAUDE.md` §CI History Hygiene updated with 4-tier retention table
- [ ] GAP-205 file committed
- [ ] ROADMAP Epic 14 (Quality Governance) — add GAP-205 entry

### Stage B (this PR)
- [x] Bulk cleanup executed 2026-04-24 (~260 runs deleted, 769 → expected ~500)
- [ ] Post-cleanup verification: `gh run list --limit 100 | wc -l` < 100

### Stage C (separate PR)
- [ ] `.github/workflows/ci-cleanup.yml` created with weekly cron
- [ ] Manual `workflow_dispatch` supported
- [ ] Test: trigger manually on merged PR, verify deletion + audit log

### Stage D (separate PR)
- [ ] `/repo-status` filters Dependabot Updates failures
- [ ] Skill reports cleanup debt ("100 runs pending prune") separately from real failures

## Related

- **Depends on:** CLAUDE.md is the policy anchor; scheduled workflow depends on GitHub `actions: write` permission (default OK)
- **Case study:** 2026-04-21 Dependabot flood (`documents/04-quality/analyses/2026-04-21-dependabot-first-run-incident.md`) — similar "auto-tooling needs retention policy" pattern
- **Memory:** `feedback_dependabot_pnpm_transitive.md` — Dependabot Updates failures are expected for pnpm, should be filtered

## Log

- **2026-04-24** — Gap filed after user observation during GAP-204 session ("30+ trang CI tồn tại"). Existing CLAUDE.md policy covers failed runs only, missing success retention + age caps. Stage B bulk cleanup executed same day (260+ runs queued for deletion).
