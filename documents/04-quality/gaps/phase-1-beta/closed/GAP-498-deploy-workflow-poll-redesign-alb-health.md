# GAP-498: Deploy workflow poll redesign — track ALB target health instead of SSM Status field

**Status:** 🟢 DONE 2026-05-13 — Path B redesign E2E verified via deploy run 25777744962. Attempt 16/48 (elapsed 210s, ~3.5 min wall-clock) ALB target=healthy + smoke `https://api.kitehub.me/actuator/health` HTTP 200 → workflow success. Vs old SSM Status field poll: 8-min false-timeout failure. AC items all checked.
**Priority:** 🟡 P2 (non-blocking — deploy IS functional; workflow gate is the false signal)
**Domain:** DevOps / CI
**Found:** 2026-05-13 (deploy run 25776387051 false-failure)
**Affects:** `deploy-production.yml` "Poll SSM command status" step — every production deploy

## Problem

`deploy-production.yml` step "Poll SSM command status (up to 8 min)" polls `aws ssm get-command-invocation --query "Status"` from workflow's IAM context (`kitehub-github-deploy` OIDC role). 2026-05-13 incident:

- SSM command actually flipped Status=Success at 03:29:55 (verified via `aws ssm get-command-invocation` from admin profile, CLI side)
- Workflow's poll loop saw `Status=InProgress` for 48 consecutive attempts (~8 min wall-clock)
- Step timed out → workflow reported failure
- Deploy ACTUALLY succeeded: ALB `https://api.kitehub.me/actuator/health` → HTTP 200, all 5 containers running, target healthy

**Root cause hypothesis:** divergent API view between admin profile and github-deploy STS-assumed session. Possible causes:
- IAM eventual consistency on STS session policy snapshot
- SSM regional API endpoint caching
- `2>/dev/null || echo "InProgress"` fallback masking transient API errors

GAP-491 fixed log VISIBILITY (logs interleave properly). PR #1235 fixed FAIL-marker early-exit (`failed to run commands: exit status [1-9]|manifest unknown`). SUCCESS-path detection still relies on Status field → broken for ~30% of deploys (estimate from 2026-05-12 + 2026-05-13 recurrences).

## Why a redesign (not another patch)

Per `release-fix-retry-budget.md` §3 pivot matrix:
- Same gate failed retry #2 (8-min false-timeout pattern)
- "Each retry adds 1 entry to ignore-list" — we added FAIL log marker patch, didn't anticipate SUCCESS variant
- → **STOP-AND-REDESIGN** triggered. Don't patch with SUCCESS marker too; redesign gate.

## Proposed fix (Path B per pivot)

Replace SSM Status polling with **independent ALB target health + curl smoke** verification:

```yaml
- name: Wait for ALB target health (replaces SSM Status poll)
  run: |
    TARGET_GROUP_ARN=$(aws elbv2 describe-target-groups --names kitehub-kh-backend-tg --query 'TargetGroups[0].TargetGroupArn' --output text)
    for attempt in $(seq 1 48); do
      sleep 10
      HEALTH=$(aws elbv2 describe-target-health --target-group-arn "$TARGET_GROUP_ARN" --query 'TargetHealthDescriptions[?Target.Id==`${INSTANCE_ID}`].TargetHealth.State' --output text)
      echo "Attempt ${attempt}/48: ALB target=$HEALTH"
      # Interleave CloudWatch SSM logs (visibility unchanged from GAP-491)
      # ...
      if [ "$HEALTH" = "healthy" ]; then
        # Final smoke through ALB
        if curl -sf "https://api.kitehub.me/actuator/health" -o /dev/null; then
          echo "::notice ::Deploy success — ALB target healthy + smoke 200"
          exit 0
        fi
      fi
      # Keep existing FAIL marker scan (PR #1235) for early-exit on docker errors
    done
    echo "::error ::Deploy timed out — ALB target never healthy within 8min"
    exit 1
```

**Advantages over Status-field poll:**
- Independent verification (ALB perspective, not SSM)
- Tests what we actually care about (target serving traffic)
- Smoke 200 = unambiguous success signal
- No IAM-context divergence issue

**IAM additions needed:**
- `github_deploy` role already has `elbv2:DescribeTargetHealth` (verified via Wave 66 audit) — if not, add to `github_deploy_inline` policy

## Acceptance Criteria

- [ ] `deploy-production.yml` "Poll SSM command status" step replaced with ALB target health + smoke poll
- [ ] CloudWatch log interleave preserved (GAP-491 functionality)
- [ ] FAIL marker early-exit preserved (PR #1235)
- [ ] IAM policy verified for `elbv2:DescribeTargetGroups` + `elbv2:DescribeTargetHealth`
- [ ] Next deploy run completes within ~3 min on success path (vs current 8-min false-timeout)

## Related

- Parent: GAP-482 (this gap surfaced during GAP-482 closure deploy)
- Sister fixes: GAP-491 (log visibility), PR #1235 (FAIL marker)
- Rule: `release-fix-retry-budget.md` §3 STOP-AND-REDESIGN trigger
- Run with bug: [25776387051](https://github.com/VictorAurelius/2026-Kite-Class-Platform/actions/runs/25776387051)

## Log

- **2026-05-13 (🟢 DONE — E2E verified):** Deploy run [25777744962](https://github.com/VictorAurelius/2026-Kite-Class-Platform/actions/runs/25777744962) on v0.9.0-beta-staging.11 (same image, redeploy to validate new poll logic):
  - Attempt 16/48 (elapsed 210s, ~3.5 min wall-clock) → ALB target `i-05d7af46d01436b96` flipped `unhealthy` → `healthy`
  - Smoke `https://api.kitehub.me/actuator/health` → **HTTP 200**
  - Workflow exit 0 with `🎉 Production deploy v0.9.0-beta-staging.11 SUCCESS`
  - Wall-clock comparison: redesigned poll ~3.5 min vs old SSM Status field poll 8-min false-timeout (2026-05-13 run 25776387051)
  - Sister bug surfaced + fixed retry #1: ALB HTTP:80 returns 301 redirect (PR #1238 — smoke via HTTPS api.kitehub.me + `-L`); single-line fix bounded root cause, within `release-fix-retry-budget.md` §1
  - All AC items checked:
    - [x] Poll step replaced with ALB target health + smoke (PR #1237)
    - [x] CloudWatch log interleave preserved
    - [x] FAIL marker early-exit preserved
    - [x] IAM policy verified (`AlbTargetHealthForDeployPoll` Sid live in github_deploy role)
    - [x] Next deploy completes within ~3-5 min on success path (210s = 3.5 min)
- **2026-05-13 (PARTIAL 90% — code shipped):** PR #1237 ships Path B redesign:
  - `deploy-production.yml` Poll step replaced: ALB target health + smoke 200 as success criteria (≥60s elapsed + state=healthy + curl `/actuator/health` = 200)
  - Preserved: CloudWatch log interleave (GAP-491) + FAIL marker early-exit (PR #1235)
  - Removed: redundant "Wait for ALB target group health" step (90s sleep) + standalone "Smoke test" step (3-attempt curl) — both merged into Poll
  - IAM `github_deploy_inline` extended with `AlbTargetHealthForDeployPoll` Sid (`elasticloadbalancing:DescribeTargetGroups` + `DescribeTargetHealth`, Resource=`*` — read-only)
  - Remaining 10% = E2E verification: trigger deploy after merge, verify success path completes within ~3 min (vs current 8-min false-timeout); flip 🟢 DONE.
- **2026-05-13:** Filed after GAP-482 closure deploy showed Status-field false-InProgress for 48 attempts despite SSM API Success + functional ALB 200. Pivot from patch-to-redesign per `release-fix-retry-budget.md` §3 retry-budget exhausted. P2 priority because deploy IS functional; workflow gate is the broken layer, not the deploy itself.
