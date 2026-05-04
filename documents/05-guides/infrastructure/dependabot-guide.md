# Dependabot — Comprehensive Guide

**Audience:** Devs + Claude sessions
**Last reviewed:** 2026-04-24
**Source of truth:** `.github/dependabot.yml` + repo Settings → Code security and analysis
**Related:** GAP-202/203/204/205, case study `documents/04-quality/analyses/2026-04-21-dependabot-first-run-incident.md`

---

## TL;DR

| Question | Answer |
|----------|--------|
| Dependabot đang check những gì? | **Security CVEs cho 6 ecosystems** (3 Maven backend + 2 npm frontend + 1 GitHub Actions) |
| Có tự mở PR không? | **CHỈ cho security updates**, KHÔNG cho version-updates (suppressed `limit: 0`) |
| Backend có được cover không? | ✅ **CÓ** — kitehub/pom.xml + kiteclass-core/pom.xml + kiteclass-gateway/pom.xml |
| Tần suất scan? | **Weekly** Monday 04:00 Asia/Ho_Chi_Minh + on-demand khi push to main |
| Transitive deps có auto-fix không? | ❌ **Chỉ direct deps cho pnpm** (known limitation); dùng `pnpm.overrides` cho transitive |

---

## 1. Current Configuration

### 1.1 File: `.github/dependabot.yml`

```yaml
version: 2
updates:
  - package-ecosystem: maven
    directory: "/kitehub"
    schedule:
      interval: weekly
    open-pull-requests-limit: 0      # ← SUPPRESSES version-update PRs
    labels: [dependencies, security, kitehub]

  - package-ecosystem: maven
    directory: "/kiteclass/kiteclass-core"
    schedule: { interval: weekly }
    open-pull-requests-limit: 0
    labels: [dependencies, security, kiteclass]

  # ... (3 more ecosystems — see actual file)
```

**Key flag:** `open-pull-requests-limit: 0` → DOES NOT suppress security updates (unaffected by this limit per [Dependabot docs](https://docs.github.com/en/code-security/dependabot/dependabot-version-updates/configuration-options-for-the-dependabot.yml-file#open-pull-requests-limit)). Suppresses only **version-update** PRs.

### 1.2 Repo Settings (UI or API)

| Setting | Endpoint / UI path | Current state |
|---------|--------------------|---------------|
| Dependabot alerts | Settings → Code security → Dependabot alerts<br>API: `PUT /repos/{owner}/{repo}/vulnerability-alerts` | ✅ Enabled |
| Dependabot automated security fixes | Settings → Code security → Dependabot security updates<br>API: `PUT /repos/{owner}/{repo}/automated-security-fixes` | ✅ Enabled |
| Dependabot version updates | Via `.github/dependabot.yml` existence | ✅ Configured (but `limit: 0`) |
| Secret scanning | Settings → Code security → Secret scanning | Auto-enabled for public repos |
| Code scanning (CodeQL / Trivy) | CI workflow uploads SARIF | ✅ Enabled (Trivy in `core-ci.yml` + `docker-build-push.yml`) |

### 1.3 What Dependabot does automatically NOW

1. **Scans weekly** + on manifest file changes to main
2. **Creates alerts** in Security tab for ANY CVE (direct or transitive, all 6 ecosystems)
3. **Opens security PRs** ONLY for:
   - Direct dependencies (npm + Maven direct deps)
   - Transitive deps when package_manager supports it (maven supports; **pnpm does NOT**)
4. **Does NOT open** version-update PRs (suppressed by config)

---

## 2. Ecosystem Coverage Matrix

| Ecosystem | Path | CVE scan | Auto-PR | Notes |
|-----------|------|:--------:|:-------:|-------|
| Maven | `/kitehub` | ✅ | ✅ direct + transitive | 6 modules inherit (subscription, branding, email, admin, gateway, platform) |
| Maven | `/kiteclass/kiteclass-core` | ✅ | ✅ direct + transitive | poi-ooxml, spring-data-jpa, etc. |
| Maven | `/kiteclass/kiteclass-gateway` | ✅ | ✅ direct + transitive | spring-cloud-gateway, jjwt, etc. |
| npm | `/kitehub/kitehub-frontend` | ✅ | ⚠️ direct only (pnpm) | Transitive CVEs need `pnpm.overrides` manual |
| npm | `/kiteclass/kiteclass-frontend` | ✅ | ⚠️ direct only (pnpm) | Same |
| GitHub Actions | `/` | ✅ | ✅ | Scans `.github/workflows/*.yml` action versions |

**Gap:** pnpm transitive auto-fix is a known Dependabot limitation ([dependabot-core#1736](https://github.com/dependabot/dependabot-core/issues/1736)). For pnpm transitive CVEs, the `Dependabot Updates` workflow fails and we must manually override via `package.json.pnpm.overrides`.

---

## 3. Workflow for Devs

### 3.1 Daily / weekly flow

```
Monday ~04:00 Asia/Ho_Chi_Minh:
  Dependabot scans all 6 ecosystems
  ↓
  New CVEs found?
  ├─ Yes (direct dep, supported ecosystem):
  │     Dependabot opens PR → labels "dependencies, security"
  │     → review, test locally, merge
  │
  └─ Yes (transitive dep, pnpm):
        "Dependabot Updates" workflow fails
        Alert appears in Security tab BUT no PR
        → /repo-status skill flags → manual triage per §3.3
```

### 3.2 When Dependabot opens a security PR

Review checklist:
- [ ] CVE severity — CRITICAL / HIGH always fix; medium/low defer if breaking changes
- [ ] Version bump delta — patch/minor safe, major needs test
- [ ] CI status — fails often means breaking change in consumers
- [ ] Check `pnpm build` locally if frontend, `mvn test` if backend
- [ ] If CI fails on image size → bump `.github/workflows/kitehub-frontend-ci.yml` threshold (current 220MB, policy max 230MB before re-optimizing)
- [ ] Merge via squash, delete branch

Example: PR #462 axios 1.13.6 → 1.15.0 — 1 CI failure (image size 206MB > 200MB threshold), fixed by PR #464 bumping limit to 220MB.

### 3.3 When Dependabot CAN'T fix (pnpm transitive)

1. Alert visible in Security tab
2. No auto-PR
3. `/repo-status` reports HIGH/CRITICAL security

**Fix flow:**
```bash
# 1. Identify affected packages
gh api "repos/$(gh repo view --json nameWithOwner --jq .nameWithOwner)/dependabot/alerts?state=open" \
  --jq '.[] | select(.dependency.relationship=="transitive") | {
    pkg: .dependency.package.name,
    fix: .security_vulnerability.first_patched_version.identifier,
    path: .dependency.manifest_path
  }'

# 2. Add override to pnpm.overrides in package.json
#    {
#      "pnpm": {
#        "overrides": {
#          "picomatch": "^4.0.4",
#          ...
#        }
#      }
#    }

# 3. Regenerate lockfile
cd kitehub/kitehub-frontend && pnpm install --lockfile-only
cd ../../kiteclass/kiteclass-frontend && pnpm install --lockfile-only

# 4. Test build
pnpm build   # in each frontend

# 5. Commit + PR
```

### 3.4 Dismissing a false-positive alert

Conditions for dismissal (`dismissed_reason`):
- `inaccurate` — advisory citation wrong (e.g., fix version doesn't exist on registry)
- `not_used` — dep not actually imported by code
- `tolerable_risk` — advisory accurate but risk accepted for this project
- `fix_started` — fix PR in progress (prefer to leave OPEN until merge)
- `no_bandwidth` — will defer; prefer not to use this reason long-term

Command:
```bash
gh api --method PATCH "repos/$REPO/dependabot/alerts/<NUMBER>" \
  -f state="dismissed" \
  -f dismissed_reason="inaccurate"
```

**Historical dismissals:**
- 2026-04-24 lodash alerts #91, #92 — dismissed `inaccurate` because advisory cited fix=4.18.0 which doesn't exist on npm (latest 4.17.23). See memory `feedback_dependabot_alert_query.md`.

---

## 4. Guide for Claude Sessions

### 4.1 Priority rules

- **Any CRITICAL CVE** → `/repo-status` BLACK → session priority: fix before ANY feature work
- **Any HIGH CVE** → RED → fix before next wave merge
- **Medium/low** → YELLOW → absorb into weekly review
- **Dependabot disabled** → ORANGE → enable immediately (silent drift)

See `reference/level-definitions.md` in `.claude/skills/workflow/repo-status/`.

### 4.2 Queries Claude should use

**Get accurate alert scope:**
```bash
REPO=$(gh repo view --json nameWithOwner --jq .nameWithOwner)

# CORRECT — uses security_vulnerability (Dependabot-computed applicability)
gh api "repos/$REPO/dependabot/alerts?state=open&per_page=100" \
  --jq '.[] | {
    n: .number,
    pkg: .dependency.package.name,
    sev: .security_advisory.severity,
    range: .security_vulnerability.vulnerable_version_range,
    fix: .security_vulnerability.first_patched_version.identifier,
    rel: .dependency.relationship,
    mfst: .dependency.manifest_path
  }'

# WRONG — first range only, misses multi-line advisories
gh api "repos/$REPO/dependabot/alerts?state=open" \
  --jq '.[] | .vulnerabilities[0].vulnerable_version_range'   # ← do NOT use
```

### 4.3 Check automation state

```bash
# Dependabot alerts enabled?
gh api "repos/$REPO/vulnerability-alerts" 2>&1 | head -1
# → 204 No Content if enabled, 404 if disabled

# Automated security fixes?
gh api "repos/$REPO/automated-security-fixes"
# → {"enabled": true, "paused": false}
```

### 4.4 Common Claude workflows

| User says | Claude action |
|-----------|---------------|
| "check security" / "any CVE?" | Run queries above, report breakdown by severity |
| "update deps" | Explain current policy: version updates disabled; if user insists, edit `.github/dependabot.yml` to raise `open-pull-requests-limit` |
| "enable Dependabot" | Check `vulnerability-alerts` + `automated-security-fixes` endpoints; enable if disabled |
| "why no auto PR for X" | Check if X is transitive in pnpm — if yes, explain limitation + propose `pnpm.overrides` |
| "clean up CI runs" | See CLAUDE.md §CI History Hygiene (GAP-205) — retention policy governs |

### 4.5 Memories Claude must reference

- `feedback_repo_status_security_coverage` — Health-check skills MUST probe Dependabot
- `feedback_dependabot_first_run` — Enable-in-stages pattern (avoid 28-PR flood)
- `feedback_dependabot_alert_query` — Use `security_vulnerability.vulnerable_version_range`
- `feedback_dependabot_pnpm_transitive` — pnpm transitive limitation
- `feedback_nextjs_rsc_array_regression` — Next.js 15.1.7+ `/pricing` break pattern (relevant when bumping next)

---

## 5. Re-enabling Version Updates (future decision)

Currently disabled after 28-PR flood incident. To re-enable:

### Prerequisites
- Team has capacity for weekly dep review (1 person × ~30 min every Monday)
- Review rotation documented in `documents/05-guides/operations/` or similar
- Catch-all `groups` pattern in config to bundle per ecosystem

### Revised config pattern
```yaml
- package-ecosystem: maven
  directory: "/kitehub"
  schedule: { interval: weekly }
  open-pull-requests-limit: 1          # Single grouped PR per ecosystem
  groups:
    all:                               # Catch-all pattern
      patterns: ["*"]
```

This opens **1 PR per ecosystem per week** = 6 PRs total, manageable.

### File a gap first
Per `.claude/rules/audit-to-gap-pipeline.md`, re-enabling version updates = significant policy change → file gap (extends GAP-205 or new) + get reviewer approval.

---

## 6. Troubleshooting

### 6.1 "Dependabot Updates" workflow fails on main

**Symptom:** CI shows failed "Dependabot Updates" runs periodically.
**Cause:** Dependabot attempted to fix transitive pnpm CVE — not supported upstream.
**Action:** Delete failed runs (policy: always deletable per CLAUDE.md §CI History). Fix underlying CVE via manual `pnpm.overrides` if HIGH/CRITICAL.

### 6.2 First-run flood after enabling version updates

**Symptom:** 20+ PRs open within minutes of enabling version updates.
**Cause:** First-run scan detects all outdated deps, opens individual PRs per dep.
**Action:** Close all PRs with bulk comment, reconfigure to security-only OR with catch-all `groups`. See case study `documents/04-quality/analyses/2026-04-21-dependabot-first-run-incident.md`.

### 6.3 Lockfile mismatch on CI

**Symptom:** `ERR_PNPM_OUTDATED_LOCKFILE` in CI install step.
**Cause:** `pnpm.overrides` conflicts with direct dep spec (e.g., override says `^8.0.5` but devDep says `^8.0.10`).
**Action:** Unify — either remove override (use direct dep) or align versions. See GAP-204 Log 2026-04-24.

### 6.4 Image size check fails after bump

**Symptom:** "❌ Image size exceeds 220MB limit" in Docker Build & Verify.
**Cause:** Cumulative dep bumps grew image.
**Action:** If <230MB, bump threshold in `.github/workflows/kitehub-frontend-ci.yml`. If >230MB, optimize Dockerfile (multi-stage + `pnpm prune --prod`).

---

## 7. Quick Commands Reference

```bash
REPO=$(gh repo view --json nameWithOwner --jq .nameWithOwner)

# Alert summary
gh api "repos/$REPO/dependabot/alerts?state=open&per_page=100" \
  --jq '{
    total: length,
    by_severity: [.[] | .security_advisory.severity] | group_by(.) | map({severity: .[0], count: length}),
    by_ecosystem: [.[] | .dependency.package.ecosystem] | group_by(.) | map({eco: .[0], count: length}),
    by_relationship: [.[] | .dependency.relationship] | group_by(.) | map({rel: .[0], count: length})
  }'

# Dismiss stale advisory
gh api --method PATCH "repos/$REPO/dependabot/alerts/<NUM>" \
  -f state="dismissed" -f dismissed_reason="inaccurate"

# Enable / disable automation
gh api --method PUT "repos/$REPO/vulnerability-alerts"          # enable alerts
gh api --method DELETE "repos/$REPO/vulnerability-alerts"       # disable
gh api --method PUT "repos/$REPO/automated-security-fixes"      # enable auto-PRs
gh api --method DELETE "repos/$REPO/automated-security-fixes"   # disable auto-PRs

# Run /repo-status locally
bash ./scripts/repo-status.sh          # full
bash ./scripts/repo-status.sh --level  # level only
bash ./scripts/repo-status.sh --json   # JSON

# Cleanup CI runs (per CLAUDE.md §CI History Hygiene)
bash ./scripts/cleanup-ci-runs.sh [--dry-run] [--branch <name>] [--merged-only]
```

---

## 8. Related Documents

| Document | Purpose |
|----------|---------|
| `documents/04-quality/gaps/GAP-202-*.md` | Skill blindspot for GitHub Security (origin gap) |
| `documents/04-quality/gaps/GAP-203-*.md` | First Maven CVE fix (commons-beanutils, tomcat-embed) |
| `documents/04-quality/gaps/GAP-204-*.md` | npm security backlog — 89 → 0 alerts |
| `documents/04-quality/gaps/GAP-205-*.md` | CI history retention policy |
| `documents/04-quality/analyses/2026-04-21-dependabot-first-run-incident.md` | 28-PR flood case study + enable-in-stages lesson |
| `.claude/skills/workflow/repo-status/SKILL.md` | Skill that detects Dependabot state |
| `.claude/rules/mcp-first-with-fallback.md` | Why `gh api` is preferred over `gh` CLI for programmatic queries |
| `CLAUDE.md` §CI History Hygiene | Retention policy for CI runs (relevant because Dependabot creates them) |

---

## 9. Changelog

- **2026-04-24** — Guide created after GAP-204 session close + user request for comprehensive reference. Consolidates scattered info from 4 GAP files + case study + 5 memories.
