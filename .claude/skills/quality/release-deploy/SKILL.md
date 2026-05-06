---
name: release-deploy
description: "Use when user mentions 'deploy version', 'release v1.0', 'go-live', 'tag release', 'production cutover', 'release checklist', 'deploy plan', or before tagging any production release. Guides through `release-deploy-standard.md` per-bump-type checklist + go-live runbook + smoke test + rollback procedure."
user-invocable: true
---

# Skill: Release Deploy Guide

**Version:** 1.0.0
**Created:** 2026-05-06
**Pairs with rule:** `.claude/rules/release-deploy-standard.md`

---

## When to Use

- User mentions: "deploy version X", "release v1.0", "go-live", "tag release", "production cutover", "release checklist", "deploy plan ready?"
- Before tagging any git tag matching `v[0-9]+.[0-9]+.[0-9]+*`
- After receiving production incident requiring hotfix release
- Quarterly release audit cycle

## Trigger conditions

```
User: "Let's deploy v1.0.0"
User: "Tag release v0.9.0-beta"
User: "Ready to go-live?"
User: "Run release checklist for v1.0.0"
User: "Production deploy plan?"
User: "Hotfix release v1.0.1"
```

## What this skill does

1. Identify release type (PRE-RELEASE / PATCH / MINOR / MAJOR) from version tag
2. Load applicable per-bump-type checklist from rule §3
3. Check each artifact status (✅ done / ⏳ pending / ❌ missing)
4. For pending/missing: link to gap or propose new gap
5. Run pre-deploy verification commands
6. Generate deploy command sequence (Helm + Terraform + kubectl)
7. Run smoke test post-deploy
8. Capture deploy outcome → wave-history.jsonl + GitHub Release

## Methodology — 7 stages

### Stage 1: Identify release type

Parse version tag per `versioning-policy.md` §3:

```
v0.9.0-beta       → PRE-RELEASE (beta)
v1.0.0-rc.1       → PRE-RELEASE (rc)
v1.0.0            → MAJOR + first PRODUCTION
v1.0.1            → PATCH
v1.1.0            → MINOR
v2.0.0            → MAJOR (persona expansion)
```

### Stage 2: Load applicable checklist

From `.claude/rules/release-deploy-standard.md` §3:
- §3.1 PRE-RELEASE — subset
- §3.2 PATCH — subset (extends §3.1)
- §3.3 MINOR — most items (extends §3.2)
- §3.4 MAJOR + first PROD — full

### Stage 3: Status check each artifact

For each item in checklist:

```bash
# Examples
[deploy plan]       → check parent doc link in PR description
[smoke test]        → check scripts/smoke-test.sh exists
[rollback]          → check documents/05-guides/operations/runbooks/rollback-runbook.md
[secrets manager]   → check AWS Secrets Manager configured
[DNS]               → dig kitehub.vn +short
[CDN]               → curl -I https://kitehub.vn | grep CF-Ray
[email]             → send test email
[health check]      → curl <url>/actuator/health
[smoke test]        → ./scripts/smoke-test.sh
```

Mark each: ✅ done / ⏳ pending (link gap) / ❌ missing (file new gap)

### Stage 4: Pre-deploy verification

Run automated checks:
- CI green on tag commit
- Database backup taken
- Quality audit /100 ≥ threshold (PRE-RELEASE 80; PROD MAJOR 85)
- Staging deploy successful (if MAJOR)
- Migration plan documented (if schema change)

### Stage 5: Deploy execution

Per parent deploy plan steps. **Human-in-the-loop required** for critical commands (per `release-deploy-standard.md` §9):

```bash
# Agent CAN do (prep/observation):
- Generate deploy command sequence
- Suggest Helm values
- Parse logs
- Run smoke tests

# Agent SHOULD NOT do (production execution):
- terraform apply
- helm upgrade --install (production namespace)
- kubectl apply (production)
- DNS cutover
- Database migrations on prod
```

User executes commands manually OR via separately-authenticated CI/CD workflow.

### Stage 6: Smoke test post-deploy

```bash
./scripts/smoke-test.sh <prod-url>
# Expect: all 15+ assertions pass

# If fail: trigger rollback procedure (per GAP-378)
```

### Stage 7: Capture outcome

- Append `wave-history.jsonl` entry per `feedback_wave_history_append_required.md`
- Update `documents/03-planning/changelogs/CHANGELOG-vX.Y.Z.md`
- Create GitHub Release với changelog body
- Update status page (resolved if pre-existing maintenance)

---

## Skill output format

```markdown
# Release Deploy Status — vX.Y.Z

**Release type:** PRE-RELEASE / PATCH / MINOR / MAJOR
**Target environment:** beta / staging / production
**Standard reference:** `.claude/rules/release-deploy-standard.md` §3.X

## Artifact checklist (N/M done)

### Operational Excellence
- [✅] <item> — <reference>
- [⏳] <item> — pending GAP-XXX
- [❌] <item> — MISSING; recommend file new gap

### Security
- [✅] ...
- ...

### Reliability
- ...

## Pre-deploy verification

- [✅/❌] CI green on tag
- [✅/❌] Backup taken
- [✅/❌] Quality audit /100 ≥ threshold
- ...

## Deploy plan

Per parent doc: `documents/03-planning/roadmap/release-1-deploy-plan.md` §X

Commands ready for execution (human-in-the-loop):
1. ...
2. ...

## Smoke test

Run: `./scripts/smoke-test.sh <url>`
Expected: 15+ assertions pass

## Rollback ready?

Per: `documents/05-guides/operations/runbooks/rollback-runbook.md`

## Decision

🟢 GO — All blocking items satisfied
🟡 GO WITH CAVEATS — N items pending; override trailer required citing follow-up gaps
🔴 NO-GO — N blocking items missing; close gaps before retry
```

---

## Skill contents

- This `SKILL.md` — methodology
- `reference/per-bump-checklist.md` — detailed checklist tables (mirror rule §3)
- `reference/standards-cheatsheet.md` — Well-Architected/Twelve-Factor/DORA quick refs
- `scripts/check-release-readiness.sh` — automated artifact check script (defer follow-up)

---

## Gotchas

- **Don't run terraform apply or helm upgrade on production from agent** — per `release-deploy-standard.md` §9 + `output-review-mandate.md` Section 6, production execution = human-required
- **Smoke test failure → trigger rollback procedure**, not "ignore and proceed"
- **Override trailer must reference follow-up gap** — bare override blocked per rule §5
- **Skip if user just asks "what's deploy status"** without intent to actually deploy — reply with current state, don't run full skill
- **Beta release ≠ production release** — different checklists; verify version tag pattern carefully
- **DNS cutover irreversible without prep** — prep rollback DNS records BEFORE cutover

---

## Related

- Rule: `.claude/rules/release-deploy-standard.md` — the standard
- Rule: `.claude/rules/output-review-mandate.md` §3 — production deployment review row
- Plan: `documents/03-planning/roadmap/release-1-deploy-plan.md` — Release 1 specific instance
- Plan: `documents/03-planning/roadmap/versioning-policy.md` — version convention
- ADR: `documents/02-architecture/adr/ADR-015-aws-agent-plugins-evaluation.md` — agent role evaluation
- GAP: `GAP-381` — Claude agent deploy framework evaluation
- Gaps: `GAP-369..380` — Release 1 deploy artifact gaps

---

## Log

- **2026-05-06 (v1.0.0):** Skill created paired same-PR with `release-deploy-standard.md` per `rule-change-process.md` §6.5 Enforcement Parity Mandate. Triggered by user feedback to codify deploy standard via rule + skill + workflow thay vì ad-hoc gap files. Standards grounded: AWS Well-Architected + Twelve-Factor + DORA + OWASP. Self-test on Release 1 v1.0.0 (rule §11) verifies skill activates correctly + maps existing gap state.
