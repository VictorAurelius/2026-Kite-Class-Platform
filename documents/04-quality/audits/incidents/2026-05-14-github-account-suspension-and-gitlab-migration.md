---
title: 2026-05-14 — GitHub account suspension + GitLab mirror migration
status: complete
created: 2026-05-14
incident_date: 2026-05-14
resolved: 2026-05-14 (same-day GitHub restore + GitLab mirror established)
severity: P0 — full repo remote access blocked
---

# 2026-05-14 — GitHub Account Suspension + GitLab Mirror Migration

## Timeline (UTC)

| Time | Event |
|------|-------|
| ~04:08 | PR #1301 (Wave 72b Bucket A 2FA TOTP) created — last successful GitHub op |
| ~10:00 | Wave 77 plan PR #1339 MERGED |
| ~10:05 | 4 Wave 77 execution agents spawned via `Agent` tool (Buckets A/B/C/D in worktrees) |
| ~10:08 | Meta PR #1340 (inside-out-completeness-trigger rule) OPEN |
| ~10:11 | Wave 78 plan agent completed → PR #1341 DRAFT opened |
| ~10:15 | First HTTP 403 detected — `gh pr view 1301 --json statusCheckRollup` → `Sorry. Your account was suspended` |
| ~10:18 | Confirmed full suspension: `gh api user` 403 + `git fetch` 403 |
| ~10:20 | User attempts GitHub web login → redirects to `/suspended` page |
| ~10:25 | Public profile `github.com/VictorAurelius` returns HTTP 404 |
| ~10:30 | Public repo `github.com/VictorAurelius/2026-Kite-Class-Platform` returns HTTP 404 — confirms hard suspension |
| ~10:35 | User checks Gmail for suspension notification — none found |
| ~10:40 | Backup script `scripts/backup-repo-snapshot.sh` created + ran (458MB tarball, SHA-256 verified) |
| ~10:45 | User decision: migrate to GitLab parallel with appeal |
| ~10:50 | GitLab account `victoraurelius` created (existing account via email Google OAuth) |
| ~10:55 | GitLab project `kite-class-platform` created (private) + SSH key added |
| ~11:00 | `git push -u origin main` to GitLab succeeded |
| ~11:05 | `git push origin --all --tags` — 112 branches + 13 tags mirrored to GitLab |
| ~11:10 | `gitlab-runner` installed + registered as `kite-dev-wsl2-shell` (shell executor, user nguyenvankiet) |
| ~11:25 | First smoke `.gitlab-ci.yml` shipped (commit a172903a, branch `ci/gitlab-smoke-test`) |
| ~12:55 | GitHub account RESTORED — appeal resolved same-day; public profile + repo back to HTTP 200 |
| ~13:00 | Remote config restored: `origin` = GitHub (primary fetch + push); `gitlab` = mirror; pushurl multi-target both |
| ~13:05 | Weekly tarball cron added (Sunday 02:00 UTC) |
| ~13:10 | This incident log written + committed |

**Total downtime:** ~2h 40min (10:15 → 12:55) wall-clock.
**Actual work blocked:** ~2h (backup + GitLab setup ran parallel, partial productive).

## Root cause (suspected — never confirmed by GitHub)

GitHub did not provide reason via email or `/suspended` page detail. Hypothesis based on activity pattern:

| Hypothesis | Likelihood | Evidence |
|------------|:----------:|----------|
| Rate abuse detection (rapid PR + auto-merge bursts) | 🟡 Medium | Wave 76/77 day shipped 5+ PRs in ~6h via Claude Code automation; multiple worktree branch pushes |
| AI/bot pattern heuristic | 🟡 Medium | Consistent commit cadence + automated rebase + force-push patterns |
| False positive on Trust & Safety scan | 🟡 Medium | Account restored same day without policy clarification = likely heuristic false positive |
| TOS violation | 🟢 Low | No spam/abuse content; account restored quickly without remediation requirement |
| Compromised account suspicion | 🟢 Low | No password reset enforced post-restore; access continuity preserved |

**Most likely:** anti-abuse heuristic triggered by burst pattern. Resolved via appeal review.

## What was preserved (zero data loss)

- ✅ Full git history all branches all tags
- ✅ 4 Wave 77 execution agent commits in worktrees (a172903a-style local commits)
- ✅ Meta PR #1340 branch state
- ✅ Wave 78 plan PR #1341 branch state
- ✅ Local backup tarball (off-device copy recommended but local SSD intact)
- ✅ AWS infrastructure unaffected (Secrets Manager, RDS, ECR, ALB)
- ✅ Vercel deployment unaffected (independent platform)

## What was lost (recovered post-restore)

- ✅ GitHub PR/issue history (restored when account back)
- ✅ GitHub Actions run history (restored)
- ✅ Star count / repo metadata (restored)

Net result: **zero permanent loss**.

## Counterfactual — what if appeal had failed?

- ~7-9h migration overhead to GitLab full setup (~3h done; ~4-6h remaining at suspension time)
- Lose ~1300 PR/issue threads (recoverable via export only if account briefly restored before re-ban)
- Workflows need rewrite GitHub Actions → GitLab CI (~2-4h)
- Secrets recreate (~1h) + AWS OIDC trust policy reconfig (~30min)
- Vercel/Cloudflare reconnect (~30min)
- Repo refs sweep (~1h)

**Migration was the right call** — even with quick resolution, GitLab mirror is now permanent resilience.

## Permanent changes shipped post-incident

### 1. Always-on GitLab mirror

Configured `origin` with multi-pushurl:
- Fetch: `https://github.com/VictorAurelius/2026-Kite-Class-Platform.git`
- Push targets: GitHub + GitLab simultaneously

Every `git push origin <branch>` now pushes to both. Zero overhead beyond +1-3s/push.

```bash
$ git remote get-url --all --push origin
git@gitlab.com:victoraurelius/kite-class-platform.git
https://github.com/VictorAurelius/2026-Kite-Class-Platform.git
```

### 2. Weekly tarball cron

```cron
0 2 * * 0 BACKUP_DIR=/home/nguyenvankiet/backups bash scripts/backup-repo-snapshot.sh
```

WSL2 caveat: only runs if WSL2 instance up Sunday 2am. Fallback: weekly Monday morning manual run.

### 3. GitLab self-hosted runner

`kite-dev-wsl2-shell` — shell executor as nguyenvankiet user. Unlimited free CI (no minute cap). Defers GitHub Actions dependency.

Config: `/etc/gitlab-runner/config.toml`. Service: `gitlab-runner.service`.

### 4. Backup script + procedure

`scripts/backup-repo-snapshot.sh` — full tarball + SHA-256 + metadata. Excludes node_modules/target/.next.

Usage:
```bash
bash scripts/backup-repo-snapshot.sh                       # default ~/backups
BACKUP_DIR=/mnt/external/... bash scripts/...              # custom dir
VERIFY=1 bash scripts/...                                  # extract + verify integrity
```

## Lessons learned

### LL-1: Always-on mirror is cheap insurance

**Cost:** +1-3s per push (negligible).
**Benefit:** Zero downtime if one platform suspends. Both GitHub + GitLab would need simultaneous ban to lose access.

Adopted permanently.

### LL-2: Local backup + off-device copy is mandatory

Local-only backup = single SSD failure = total loss. Off-device (cloud / external drive / second machine) is required.

User completed local backup but did NOT off-device-copy in this incident (resolved before needed). Action: add to wave-closure checklist.

### LL-3: Burst commit/push patterns trigger heuristics

5+ PRs in 6h with auto-merge + force-push patterns = bot-like signal.

**Mitigation:**
- Throttle batch operations: 30-60s sleep between bulk PR merges
- Avoid `--admin` merge (already banned per `admin-merge-discipline.md`)
- Spread wave-pack agent pushes over time (vs simultaneous burst)

Consider adding to `agent-background-spawn-default.md` v1.1.0 — "Pace burst push patterns to <3 PRs per 10min on shared infra to avoid trust-and-safety false positives."

### LL-4: Backup before destructive remote ops

Even though git is distributed, certain ops compound risk:
- `git remote remove origin` mid-suspension scare
- `git remote rename` chain (github → github-suspended → github → origin)

Always backup BEFORE touching remote config when remote state uncertain.

### LL-5: GitHub appeal process is opaque but fast

- No email notification of suspension
- No reason given via web UI `/suspended` page
- No reason given via support response
- BUT restoration was same-day after appeal submitted

Suggests: appeal queue prioritizes obvious false positives. Submit appeal immediately when suspended — don't wait/investigate first.

## Recommendations for future

### Backup cadence going forward

- ✅ **Always-on mirror** to GitLab (configured)
- ✅ **Weekly tarball cron** (configured)
- ⚠️ **Manual off-device copy** of latest tarball — Sunday morning check, copy to cloud
- ❌ **Per-wave manual mirror** — unnecessary ceremony given always-on already covers

### Migration readiness baseline

In case of permanent GitHub loss, post-this-incident migration time estimate **reduced from 7-9h to ~2-3h**:
- ✅ GitLab project exists + SSH configured
- ✅ Self-hosted runner registered + tested
- ⚠️ Workflows still GitHub Actions YAML — translation pending (~2-4h)
- ⚠️ AWS OIDC trust policy still GitHub-only — needs add GitLab issuer if migrate
- ✅ Repo content fully mirrored

Track residual translation work as follow-up gap (file when scoping next Wave 79+).

### Account hygiene

- GitHub: enable 2FA (if not already)
- GitLab: enable 2FA (mandatory per security best practice)
- Both: verify recovery email + phone
- Both: rotate Personal Access Tokens quarterly

## Cross-references

- `scripts/backup-repo-snapshot.sh` — backup automation
- `.claude/rules/release-fix-retry-budget.md` — retry budget rule (informs sequencing strategy)
- `.claude/rules/concurrent-production-mutation-ops.md` — burst-mutation governance
- `.claude/rules/incident-to-rule-pipeline.md` — 5-stage pipeline (this incident applied: Detect ✓ Classify ✓ Rule+Enforce — TBD via LL-3 follow-up Self-Test ✓ Retro Log ✓ via this file)

## Open items (follow-up gap candidates)

- [ ] **Off-device backup automation** — rclone or similar to push tarball to cloud (Drive/B2/S3) weekly
- [ ] **Workflow translation GitHub Actions → GitLab CI** — keep mirror ready as failover (Wave 79 candidate)
- [ ] **AWS OIDC dual-issuer config** — allow both GitHub + GitLab OIDC for runners (defensive)
- [ ] **Throttle burst push pattern rule** — codify LL-3 into `agent-background-spawn-default.md` or new rule
- [ ] **Migration runbook** — document the GitLab migration steps in `documents/05-guides/operations/migration-to-gitlab-runbook.md` for fast execution if needed again

## Log

- **2026-05-14** — Incident occurred + resolved same-day. Document written end-of-day capturing full timeline + lessons + permanent changes shipped (mirror, cron, runner, backup script).
