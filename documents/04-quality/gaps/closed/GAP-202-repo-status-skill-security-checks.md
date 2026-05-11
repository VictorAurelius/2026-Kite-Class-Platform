# GAP-202: `/repo-status` skill blind to GitHub Security & Code Scanning

**Status:** 🟢 DONE 2026-04-21
**Priority:** 🟠 P1 (Meta — per `meta-gap-priority.md` §3: skills/rules/workflow gap → boost above feature-P1)
**Domain:** Workflow / Meta (skill)
**Detected:** 2026-04-21 (user observation during `/repo-status` run — skill reported GREEN while 3 HIGH CVEs were open on main)
**Related PRs:** (none yet)
**Related Docs:**
- `.claude/skills/workflow/repo-status/SKILL.md`
- `scripts/repo-status.sh`
- `.claude/rules/meta-gap-priority.md`

## Current State (verified 2026-04-21)

| Piece | File / Path | Status |
|-------|-------------|--------|
| Skill body (3 factors: CI, PRs/branches, audit gaps) | `.claude/skills/workflow/repo-status/SKILL.md:1-132` | ✅ shipped |
| Collector script (3 same factors) | `scripts/repo-status.sh` | ✅ shipped |
| Dependabot alert check | — | ❌ missing |
| Code scanning alert check (`/code-scanning/alerts`) | — | ❌ missing |
| Secret scanning alert check (`/secret-scanning/alerts`) | — | ❌ missing |
| Dependabot **enabled** at repo settings | GitHub repo config | ❌ DISABLED (HTTP 403 on alerts API) |
| Level gate (GREEN/YELLOW/.../BLACK) considers security | — | ❌ no security input to gate |

**Grep commands run:**
```bash
# Skill + script — confirmed 0 hits for security-related checks
grep -E "dependabot|code-scanning|secret-scanning|code_scanning|secret_scanning" scripts/repo-status.sh
grep -E "dependabot|code-scanning|secret-scanning" .claude/skills/workflow/repo-status/SKILL.md

# Live GitHub state
gh api repos/VictorAurelius/2026-Kite-Class-Platform/dependabot/alerts   # → 403 disabled
gh api repos/VictorAurelius/2026-Kite-Class-Platform/code-scanning/alerts?state=open  # → 7 open (3 HIGH)
gh api repos/VictorAurelius/2026-Kite-Class-Platform/secret-scanning/alerts?state=open  # → 0
```

## Problem

`/repo-status` claims repo is **GREEN** when there are **3 HIGH-severity CVEs** open on main (see GAP-203) and **Dependabot is disabled** (no auto-tracking). The skill's 3-factor model (CI / PRs / audit gaps) **misses the entire GitHub Security tab**. A security regression on main is invisible to the skill.

Blast radius: every session that runs `/repo-status` to decide "safe to ship?" gets a false-positive GREEN. Per `output-review-mandate.md` §4 — this is a health-check with missing standard.

## Context

- User ran `/repo-status` at 2026-04-21 07:14 → ORANGE (stale branches only). Cleaned up 23 branches → GREEN.
- User then flagged: "skill check repo health chưa check Security and quality github báo"
- Manual `gh api` revealed 3 HIGH CVEs (commons-beanutils + 2× tomcat-embed-core) + 4 warnings that skill never surfaced.
- Dependabot was silently disabled — skill cannot detect this today.

## Evidence

- Skill source: `.claude/skills/workflow/repo-status/SKILL.md:12` — "Đánh giá nhanh sức khỏe remote repo qua **3 nhân tố**" (CI, PRs, gaps only)
- Script source: `scripts/repo-status.sh` — grep for any security API → **0 matches**
- Live alerts surfaced via `gh api .../code-scanning/alerts` → 7 open (3 HIGH severity ERROR + 4 WARNING)
- Dependabot API returns `403 "Dependabot alerts are disabled for this repository"`

## Proposed Fix

### A. Extend `scripts/repo-status.sh` — add Factor 4: Security

Add 3 probes via `gh api` (with graceful degradation if API returns 403):

```bash
# Dependabot
DEPENDABOT=$(gh api "repos/$REPO/dependabot/alerts?state=open&per_page=100" 2>/dev/null)
if echo "$DEPENDABOT" | jq -e 'type=="object" and .message' > /dev/null 2>&1; then
  DEPENDABOT_STATUS="disabled"
else
  DEPENDABOT_CRITICAL=$(echo "$DEPENDABOT" | jq '[.[] | select(.security_advisory.severity=="critical")] | length')
  DEPENDABOT_HIGH=$(echo "$DEPENDABOT" | jq '[.[] | select(.security_advisory.severity=="high")] | length')
fi

# Code scanning
CODE_SCAN=$(gh api "repos/$REPO/code-scanning/alerts?state=open&per_page=100" 2>/dev/null || echo '[]')
CODE_SCAN_ERRORS=$(echo "$CODE_SCAN" | jq '[.[] | select(.rule.severity=="error")] | length')
CODE_SCAN_WARNINGS=$(echo "$CODE_SCAN" | jq '[.[] | select(.rule.severity=="warning")] | length')

# Secret scanning (enterprise / public repo only)
SECRET_SCAN=$(gh api "repos/$REPO/secret-scanning/alerts?state=open" 2>/dev/null || echo '[]')
SECRET_SCAN_COUNT=$(echo "$SECRET_SCAN" | jq 'if type=="array" then length else 0 end')
```

### B. Update level-gate logic

Augment `reference/level-definitions.md`:

| Level | Security addition |
|-------|-------------------|
| GREEN | 0 critical/HIGH CVE, 0 secrets, Dependabot enabled |
| YELLOW | Only WARNING code-scanning alerts, Dependabot enabled |
| ORANGE | Dependabot disabled OR ≥3 WARNING / ≥5 medium |
| RED | ≥1 HIGH CVE OR ≥1 secret scanning alert |
| BLACK | ≥1 CRITICAL CVE OR secret actively exposed >24h |

Also: if Dependabot **disabled** → never GREEN, minimum YELLOW, nudge user to enable.

### C. Update SKILL.md

- Rename §12 "Factor N" sections: CI → PRs → Audit Gaps → **Security (new)**
- Add §Gotchas entries: Dependabot can be disabled (silent), `secret-scanning/alerts` requires repo to be public or enterprise tier
- Update `description:` trigger string to mention "security health" / "CVE" / "vuln"

### D. Repo settings — enable Dependabot

This is **infrastructure** not skill, but required as part of fix:
- Settings → Code security and analysis → Enable Dependabot alerts + Dependabot security updates
- Also enable secret scanning if repo goes public
- Track in AC below; doing it manually is acceptable

## Acceptance Criteria

- [x] `scripts/repo-status.sh` emits Factor 4 section (Security) in full + JSON output
- [x] `--level` exit code reflects security state (RED / BLACK for HIGH/CRITICAL)
- [x] `reference/level-definitions.md` documents security thresholds
- [x] SKILL.md updated: §Instructions, §Level Definitions, §Gotchas
- [x] Dependabot enabled at repo settings (screenshot or config link in PR description)
- [x] Re-run `/repo-status` after fixing GAP-203 → shows GREEN with "Security: 0 CVE, 0 secrets, Dependabot enabled"
- [x] Starter-kit sync: if skill exists in starter-kit repo, apply same update there (per `skill-conventions.md` §Starter-Kit Version Management)

## Related

- **Blocks** GAP-203 exposure → actually, parallel: GAP-203 is the concrete CVE-fix; GAP-202 is the detector that would have caught it
- `meta-gap-priority.md` §3 — meta-P1 precedes feature-P1; this gap qualifies for meta-boost
- `output-review-mandate.md` §4 row "PRs" → review standard exists but misses repo-level health dimension; extending
- `quality/security-audit/SKILL.md` — runs periodic; `/repo-status` is the continuous layer that should flag between audits
- `.claude/rules/audit-to-gap-pipeline.md` Step 2.5 — state-check completed above

## Log

- **2026-04-21** — Initial write-up (state-check completed). Triggered by user observation during `/repo-status` session after Wave 9.5 merge cleanup. Paired with GAP-203 (CVE fixes).
- **2026-04-21 (later)** — PR #423 merged. Skill now reports Factor 4 (Security) with Dependabot / code-scanning / secret-scanning probes. Validated on live repo: script correctly detects 3 HIGH CodeQL errors + Dependabot disabled → RED level. Detection side of the detection+fix pair is **DONE**. Status stays IN_PROGRESS until full AC verified (including Dependabot alerts enabled at repo Settings — manual reviewer step from GAP-203 PR #424).
- **2026-04-21 (later)** — Side-effect incident: enabling Dependabot in PR #424 triggered 28-PR first-run flood (#425-#452). Captured in case study [`documents/04-quality/analyses/2026-04-21-dependabot-first-run-incident.md`](../analyses/2026-04-21-dependabot-first-run-incident.md). Does not change this gap's AC but documents that the skill's "Dependabot disabled" detection was what surfaced the original gap — the skill worked as designed. Config hardened to security-only in PR #453.
- **2026-04-29 (status sync)** — Truth-up: PR #423 merged 2026-04-21 closing this gap (Factor 4 Security shipped in `scripts/repo-status.sh`, security thresholds in `level-definitions.md`, SKILL.md updated, Dependabot enabled via PR #424). Status header drifted from reality. Per memory feedback_post_merge_doc_sync.md, gap closure doc-sync should happen in same PR as the closing merge — backfilled here under Wave Meta-Gov 2 Agent C housekeeping. All 7 ACs verified as shipped.
