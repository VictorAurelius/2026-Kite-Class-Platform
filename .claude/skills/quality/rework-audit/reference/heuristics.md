# Rework Detection Heuristics

Reference for `/rework-audit` skill. Read when filtering candidate PRs.

## 1. Signal Catalog

Each signal scores 1-3 points. Sum signals per PR → **context-pressure score**.

### 1.1 Primary signals (weight 3)

**S1 — Low PR compliance score**
- Source: `documents/03-planning/pr-logs/PR-{N}.json` → `score` field (format "N/5")
- Trigger: score `<3/5` OR any `checks.*` value = `fail` for required check
- Rationale: hook-generated score = objective at merge time

**S2 — Missing required audit evidence**
- Source: `documents/04-quality/audits/` — look for audit reports within ±7 days of `mergedAt`
- Required audits by file pattern (per `post-wave-audit-mandate.md` §2.1):
  - Frontend changes → UI /128 audit
  - `rules.md` / business logic → business-logic /100
  - Controller / DTO → api-contract /100
  - `pom.xml` / `package.json` → security /100
  - Infra files → ops-readiness /100
- Trigger: PR touches pattern X, no audit X within 7 days → add 3

**S3 — Rule-doc desync**
- Source: grep code for constants/keys, cross-check `documents/01-business/*/rules.md`
- Trigger: PR changed logic, rules.md not updated in same PR → add 3
- Shortcut: check `git log --name-only {PR_merge_commit}` — if Java/TS files changed without corresponding `rules.md` edit

### 1.2 Secondary signals (weight 2)

**S4 — Fatigue cluster**
- Source: `gh pr list --state merged --json number,mergedAt,author`
- Trigger: ≥3 PRs merged by same author within 30 minutes → all PRs in cluster +2
- Rationale: rapid-fire merges suggest automation drift OR tired reviewer

**S5 — Session-lock turn count**
- Source: `.claude/session-locks/archive/*.lock` (archived locks — Phase 2)
- Trigger: PR merged during lock with `estimated_turns > 40` OR `last_heartbeat > 2h after started`
- Rationale: degraded context per GAP-193 heuristic
- Note: Phase 1 — session-lock archive not yet captured; this signal activates Phase 2

**S6 — Post-compact context flag**
- Source: session log entries (if harness captures compact events)
- Trigger: merge within 15 min after `/compact` → +2 (context reload churn)
- Phase 2: requires harness capture

### 1.3 Tertiary signals (weight 1)

**S7 — Late-night merge**
- Source: `mergedAt` → convert UTC → Asia/Ho_Chi_Minh (UTC+7)
- Trigger: 22:00-06:00 local → +1
- Rationale: weak signal alone; combined with others = stronger

**S8 — Large diff + fast turnaround**
- Trigger: `additions + deletions > 500` AND `(mergedAt - createdAt) < 1h` → +1

## 2. Scoring Threshold

| Score | Classification | Action |
|:-----:|---------------|--------|
| 0-2 | Clean | No rework audit needed |
| 3-4 | Watch | Spot-check if user flags; no action by default |
| 5-6 | Candidate | Deep re-audit via appropriate skill |
| 7-10 | High-risk | Mandatory re-audit + rework severity assessment |

## 3. Detection Commands

### 3.1 PR compliance score scan

```bash
# List PRs with score <3/5 or any fail
for f in documents/03-planning/pr-logs/PR-*.json; do
  score=$(jq -r '.score // "0/5"' "$f")
  fails=$(jq -r '[.checks | to_entries[] | select(.value=="fail") | .key] | join(",")' "$f")
  num=$(jq -r '.pr' "$f")
  if [[ "$score" < "3/5" ]] || [[ -n "$fails" ]]; then
    echo "PR #$num score=$score fails=$fails"
  fi
done
```

### 3.2 Fatigue cluster detection

```bash
# Group merged PRs by 30-min window
gh pr list --state merged --limit 100 \
  --json number,author,mergedAt \
  --jq '.[] | [.author.login, (.mergedAt | fromdate | floor / 1800 | floor), .number] | @tsv' \
  | sort | awk '{ key=$1"_"$2; bucket[key]=bucket[key]" "$3 } END { for (k in bucket) { n=split(bucket[k], a); if (n>=3) print k": "bucket[k] } }'
```

### 3.3 Audit evidence gap

```bash
# For PR #N, check if audit of type T ran within ±7 days
PR_MERGE_DATE=$(gh pr view $N --json mergedAt --jq '.mergedAt' | cut -c1-10)
find documents/04-quality/audits/ui -name '*.md' -newermt "$PR_MERGE_DATE -7 days" ! -newermt "$PR_MERGE_DATE +7 days"
# Empty result = audit missing
```

## 4. Phase-2 Queue (deferred pilot work)

NOT part of this PR. Future work:

1. **Pilot on Wave 6-8 PRs** — user-suspected degraded window. Run detection + deep audit on all merges in that wave range. Expected output: 5-15 rework items per wave.
2. **`scripts/detect-candidates.sh`** — automate §3 commands, output ranked TSV
3. **Session-lock archival** — hook to move `.claude/session-locks/*.lock` to `archive/` on session-end, preserves for retro analysis
4. **Turn-count telemetry** — extend `audit-gate.py` to record session turn estimate in `PR-{N}.json`
5. **Integration into `/repo-status`** — add "rework suspicion" factor to health levels
6. **Post-wave checklist** — add "run /rework-audit wave-N" to `post-wave-audit-mandate.md` §4

## 5. False-Positive Guards

Before flagging, check:

- **Docs-only PRs** — touching only `.md` files → skip most signals (no rule desync, no code audits needed)
- **Revert PRs** — exempt per `post-wave-audit-mandate.md` §8
- **Hotfix PRs** — labeled `hotfix` or `AUDIT_OVERRIDE` trailer → lower threshold but still audit within 24h
- **Bot-merged PRs** (Dependabot, Renovate) — different quality bar, separate heuristic (not covered here)

## 6. Versioning

- **v1 (this doc):** 2026-04-20 — initial heuristic set
- Review quarterly; tune weights based on false-positive / false-negative rate in pilot runs
