# GAP-477: rollback.yml workflow missing — required for smoke-rollback-cycle exec

**Status:** 🔵 OPEN
**Priority:** 🟠 P1
**Domain:** DevOps
**Found:** 2026-05-11 (Wave 62 Bucket C state-check)
**Affects:** Sub-6 of GAP-475 + rollback procedure per `release-deploy-standard.md` §3.1 / GAP-378

## Problem

Wave 62 Bucket C (PR #1185) shipped `scripts/smoke-rollback-cycle.sh` for Sub-6 of GAP-475 (rollback cycle test). State-check confirmed:

```bash
ls .github/workflows/ | grep -iE "rollback|revert"
# → empty
```

**No `rollback.yml` workflow exists** despite `release-deploy-standard.md` §3.1 PRE-RELEASE checklist mandating "Rollback procedure documented (per GAP-378) — at minimum one-command rollback" and GAP-378 (rollback runbook baseline) being DONE.

Sub-6 script ships PARTIAL with `[DEFER]` path: `workflow_exists()` detects absence + skips real `gh workflow run` when absent. Dry-run still useful for SHA resolution + pre-flight smoke + JSON scaffold validation, but full rollback cycle E2E impossible without workflow.

## Root Cause

GAP-378 closed 2026-05-06 baseline rollback runbook (documentation only). Workflow automation deferred. Stop-when-idle model (Wave 61) + imminent first beta tenant invitation make this gap blocking — rollback runbook untested without exec workflow.

## Proposed Fix

Create `.github/workflows/rollback.yml`:
- `workflow_dispatch` with input `target_sha` (required) + `confirm=APPLY` (verbatim gate per `release-deploy-standard.md` §9 human-in-the-loop)
- Steps:
  1. Validate `target_sha` exists on main + not too old (e.g. <30 days)
  2. Trigger ECR image retag → previous SHA's image becomes `latest`
  3. Trigger k8s deployment rollback OR re-apply with old image tag
  4. Wait health probes via `kubectl wait` or smoke endpoint poll
  5. Post deployment status to PR/issue
- Uses ephemeral OIDC creds per `release-deploy-standard.md` §9 + `agent-aws-access.md`
- Approval gate via `environment: production` GitHub setting

## Acceptance Criteria

- [ ] `.github/workflows/rollback.yml` shipped with `workflow_dispatch` + `confirm=APPLY` input
- [ ] OIDC role for rollback ops created via terraform (separate from apply role per least-privilege)
- [ ] Dry-run path: workflow can be invoked with `--field dry_run=true` outputting plan only
- [ ] Documentation cross-link in `incident-response-runbook.md` §8 + `release-deploy-standard.md` §4.3
- [ ] `scripts/smoke-rollback-cycle.sh` updated to remove `[DEFER]` path, exercise real workflow
- [ ] First `--execute` dry-run in staging records baseline TTR (time-to-recovery)

## Related

- **Parent:** GAP-475 Sub-6 (Wave 62 deferral)
- **Sibling:** GAP-476 (Flyway HTTP endpoint, also Wave 62 deferral)
- **Predecessor:** GAP-378 (rollback runbook docs baseline, DONE 2026-05-06)
- **References:**
  - `release-deploy-standard.md` §3.1 + §4.3 + §9 (human-in-the-loop apply)
  - `agent-aws-access.md` §4.3 (workflow_dispatch carve-out)
  - `scripts/smoke-rollback-cycle.sh` (Wave 62 Bucket C scaffold)

## Log

- **2026-05-11:** Filed as Wave 62 Bucket C deferral. Sub-6 of GAP-475 PARTIAL pending this workflow. P1 because blocks beta-launch rollback readiness.
