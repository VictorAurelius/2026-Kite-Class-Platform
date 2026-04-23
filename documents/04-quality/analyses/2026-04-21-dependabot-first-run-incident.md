# Case Study: Dependabot First-Run 28-PR Flood

**Date:** 2026-04-21
**Duration:** ~1 hour (discovery → containment)
**Severity:** 🟡 Low (noise, not outage)
**Triggered by:** PR #424 merge adding `.github/dependabot.yml`
**Closed by:** Reconfiguration PR (security-only) + 28 PRs closed by policy
**Owner:** @nguyenvankiet
**Related gaps:** GAP-202, GAP-203
**Related rules:** `.claude/rules/mcp-first-with-fallback.md`, `.claude/rules/output-review-mandate.md`

---

## 1. Timeline

| Time (UTC+7) | Event |
|--------------|-------|
| 07:13 | `/start-session` → GREEN state assumed |
| 07:14 | `/repo-status` → ORANGE (23 stale branches), cleanup → GREEN |
| 07:18 | User flags: skill missing GitHub Security checks |
| 07:25 | Manual `gh api` probe → 7 open CVEs (3 HIGH) + Dependabot disabled |
| 07:30 | Filed GAP-202 (skill) + GAP-203 (CVEs), ROADMAP + memory updated |
| 07:35 | PR #423 (GAP-202 skill fix) opened |
| 07:45 | PR #424 (GAP-203 CVE fix + `.github/dependabot.yml`) opened |
| 07:54 | PR #423 merged |
| 07:56 | PR #424 merged |
| 07:57 | **Dependabot first-run triggered → 28 PRs opened in 3 minutes (#425–#452)** |
| 08:00 | User flagged: "28 PR là quá nhiều, free tier?" + "dồn 1 PR không được sao?" |
| 08:10 | Classification complete, 3 response options proposed |
| 08:15 | Decision: Option A — close 28 + case study + reconfigure |

---

## 2. What Happened

`.github/dependabot.yml` committed in PR #424 enabled **Dependabot version updates** for 6 ecosystems (kitehub maven, kiteclass-core maven, kiteclass-gateway maven, kitehub-frontend npm, kiteclass-frontend npm, github-actions). On first run, Dependabot scanned all declared deps and opened a separate PR per outdated dep that didn't fall into a configured `groups` block.

**28 PRs breakdown:**
- 3 GitHub Actions (major bumps — setup-java 4→5, setup-node 4→6, buildx-action 3→4)
- 15 Maven (mix: 4 jjwt, 2 commons-compress, 2 group rollups, 7 individual)
- 10 npm (mix: 4 group rollups, 6 individual)

Two PRs (#433, #435, #441) specifically superseded the CVE fixes I had just merged in PR #424 with even newer versions (commons-compress 1.26→1.28, poi-ooxml 5.4→5.5.1). That's not wrong — but signals the scope of drift Dependabot was trying to resolve.

---

## 3. Root Causes

### 3.1 Config-level (primary)

`.github/dependabot.yml` as written enabled **version updates** with `open-pull-requests-limit: 5` per ecosystem. Math: 6 ecosystems × ~5 PRs each = up to 30 on first run. The limit is per-ecosystem, not global.

### 3.2 Intent-level (what I actually wanted)

The reason we enabled Dependabot at all: close the detection gap surfaced by GAP-202 — auto-track **new CVEs** so main doesn't silently accumulate HIGH severity alerts. We did NOT need weekly "your deps are outdated by 3 patches" PRs; those are version-update noise.

### 3.3 Process-level

- No team policy on Dependabot update frequency before enabling
- `groups` config covered Spring / React / testing / dev-tooling but missed long-tail deps (jjwt, poi, jsoup, lombok, opencsv, springdoc) → each opened individually
- No staged rollout (enable security only first, add version updates later when there's capacity)

---

## 4. Impact

**What was harmed:**
- 28 CI runs queued/completed on first-run PRs (~100-150 Linux-minutes across workflows) — repo is PUBLIC so **$0 cost** on GitHub Actions
- Reviewer cognitive load: 28 PRs in PR list makes `/repo-status` + `gh pr list` noisy
- Time spent on classification + response: ~30 minutes

**What was NOT harmed:**
- No production impact (branch PRs, no auto-merge)
- No repo security posture change (CVE fix still shipped in PR #424)
- No lost work (Dependabot PRs preserved as closed history if we want patterns later)

---

## 5. Response

Per `audit-to-gap-pipeline.md` we don't fix-direct in incident discovery — we create gap/docs, then execute. This case study came first, reconfiguration PR second, then PR-closure.

**Option A chosen:** Close all 28 PRs by policy, reconfigure Dependabot to security-only, document in this case study. Reasoning:
- Product pre-GA, dep version churn is not a high-value signal
- CVE detection (the original goal) is achieved by security-updates alone
- Team has no policy yet for weekly dep review; don't create obligation we can't fulfill

**Alternatives considered:**
- Option B (selective merge): would have merged ~8 low-risk patch PRs. Rejected — effort (2-3h testing) exceeds benefit for pre-GA; major bumps still deferred so most PRs close anyway.
- Option C (mixed): similar trade-off, more inconsistent policy.

---

## 6. Lessons Learned

### 6.1 Enable-in-stages pattern for auto-tooling

When enabling any automation (Dependabot, Renovate, auto-formatters, security scanners) on an existing repo with accumulated state, **stage the rollout**:

1. **Stage 1 — detection only**: surface findings without acting (`open-pull-requests-limit: 0`, `allow: security-updates`, read-only scanners)
2. **Stage 2 — security-critical auto-action**: enable PRs for CVE-only
3. **Stage 3 — routine auto-action**: enable version updates when team has capacity for weekly review

Adding Stage 3 before team has capacity = incident.

### 6.2 Global limits beat per-ecosystem limits for first-run safety

Dependabot's `open-pull-requests-limit` is per-ecosystem. For first-run safety, prefer:
```yaml
updates:
  - package-ecosystem: maven
    allow:
      - dependency-type: "direct"       # only direct deps, not transitive
    open-pull-requests-limit: 1          # cap hard
```
Or use `groups` aggressively with catch-all patterns (`"*"` per ecosystem) to fold everything into 1 PR/eco.

### 6.3 `groups` syntax supports wildcard catch-all

Better config pattern (if keeping version updates):
```yaml
groups:
  all-dependencies:
    patterns:
      - "*"
```
One PR per ecosystem per week instead of one per dep.

### 6.4 Auto-tooling changes need their own PR separate from other fixes

In PR #424 I bundled:
- pom.xml CVE bumps (concrete security fix)
- `.github/dependabot.yml` (new automation)

These have different risk profiles. The pom.xml changes are reversible; the Dependabot config triggers **side effects** (28 PRs) that outlast the PR itself. Next time: auto-tooling config goes in its own PR so side effects are isolated.

### 6.5 Check automation side-effects before merge

Before merging PRs that enable automation (Dependabot, CI workflows that scan repo, scheduled jobs), mentally simulate: "what side effects will fire on first run?". The answer isn't in the diff — it's in the tool's behavior spec.

---

## 7. Preventive Actions

| # | Action | Owner | Status |
|---|--------|-------|--------|
| 1 | Reconfigure `dependabot.yml` → security-only via separate PR | This session | 🟠 IN_PROGRESS |
| 2 | Close 28 Dependabot PRs with standardized comment linking this case study | This session | 🟠 IN_PROGRESS |
| 3 | Save memory `feedback_dependabot_first_run` with the enable-in-stages pattern | This session | 🟠 IN_PROGRESS |
| 4 | Update GAP-202 + GAP-203 Log entries to reference this case study | This session | 🟠 IN_PROGRESS |
| 5 | Future: if team adds weekly dep review capacity, re-enable version-updates with catch-all groups (file as a gap, not a TODO) | Future | 📋 Deferred |

---

## 8. Policy Decisions (binding from 2026-04-21 forward)

1. **Dependabot scope:** security-updates only until explicit team decision reverses this
2. **Auto-tooling PRs:** single-concern PRs (config changes separate from code changes that benefit from that config)
3. **First-run simulation:** before merging automation config, document expected side effects in PR body
4. **Case studies:** meaningful auto-tooling incidents (≥10 PRs opened/closed in under 1 day) warrant a case study doc in `documents/04-quality/analyses/`

---

## 9. References

- **GAP-202** — `/repo-status` skill security checks (detection gap that started this session)
- **GAP-203** — pom.xml CVE dep upgrades (concrete fix, shipped PR #424)
- **PR #423** — `feat(skill): GAP-202 — /repo-status adds GitHub Security factor`
- **PR #424** — `fix(security): GAP-203 — bump transitive deps to close 7 open CVEs`
- **Dependabot config docs** — https://docs.github.com/en/code-security/dependabot/dependabot-version-updates/configuration-options-for-the-dependabot.yml-file
- **Rule:** `.claude/rules/output-review-mandate.md` — health checks must have review standards (this case study IS the standard for auto-tooling)
- **Rule:** `.claude/rules/meta-gap-priority.md` — skill/workflow gaps = force multiplier; first-run flood demonstrates why (1 config line → 28 PRs affected)

---

## 10. Log

- **2026-04-21 08:15** — Case study written as Step 1 of Option A response. User direction: "đã mở PR rồi thì phải làm đúng quy trình và lưu lại case study."
