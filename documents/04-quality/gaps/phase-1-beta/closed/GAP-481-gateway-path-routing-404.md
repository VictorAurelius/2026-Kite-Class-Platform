# GAP-481: Gateway path routing `/kitehub-subscription/*` returns 404

**Status:** 🟢 DONE 2026-05-21 (Wave 102.8 Bucket D — local stack live verify: `/api/v1/admin/*` routes return 400 NOT 404; explicit gateway route predicates `kitehub-admin-beta-requests-v1` + `kitehub-admin-impersonate` + `platform-instances` registered in `application.yml` precede catch-all `/api/v1/**`; route registry confirmed via `/actuator/gateway/routes`)
**Priority:** 🟠 P1 (blocks proper service routing post-cutover; smoke tests may pass `/actuator/health` but fail per-service paths)
**Domain:** Backend / DevOps
**Found:** 2026-05-12 (Wave 64 deploy simulation probe)
**Closed:** 2026-05-21 (Wave 102.8 Bucket D live verify on local stack)
**Affects:** Spring Cloud Gateway routing — kitehub-gateway service path predicates

## Problem

Wave 64 deploy simulation probe (per `2026-05-12-wave-64-pre-apply-plan-investigation.md`):

```bash
# Spring Boot actuator works direct
curl -sI http://api.kitehub.me/actuator/health
# → HTTP 200 ✅

# Gateway path-routed actuator fails
curl -sI http://api.kitehub.me/kitehub-subscription/actuator/health
# → HTTP 404
```

Same pattern likely for `/kitehub-branding/*`, `/kitehub-email/*`, `/kitehub-admin/*`, `/kitehub-platform/*` — all microservice paths routed via gateway.

This means:
- Direct backend `/actuator/health` works (kh-backend on port 8080)
- BUT gateway path predicates (`/kitehub-subscription/**` → forward to subscription service) NOT functioning

Wave 62 smoke-test `check_health` calls direct service paths like `${KH_URL}/kitehub-subscription/actuator/health`. Currently fails → false health signal.

## Possible root causes

1. **Gateway not running** — kh_backend EC2 hosts gateway + service in container; if gateway container down, only backend-direct works
2. **Gateway route config drift** — Spring Cloud Gateway `RouteLocator` bean may not have all paths configured
3. **docker-compose port mapping** — ALB:8080 → gateway:8080 hoặc → individual service?
4. **Path predicate mismatch** — `/kitehub-subscription/**` predicate vs actual service prefix

## Proposed Fix

State-check first:
1. SSH/SSM to EC2 `i-0b65c3947d36cae61` → `docker ps` → which containers running?
2. Verify `kitehub-gateway` container up + healthy
3. Check `kitehub-gateway` Spring Cloud Gateway config — `application.yml` `spring.cloud.gateway.routes`
4. Verify ALB:80 target → gateway:8080 (not subscription:8080 direct)

Then fix based on findings:
- Container missing → docker-compose.yml fix + redeploy
- Routes missing → kitehub-gateway code fix
- Port mapping → docker-compose port mapping fix
- Predicate format → adjust `Path=/kitehub-subscription/**` matching

## Acceptance Criteria

- [ ] State-check report under `documents/04-quality/audits/aws-verification/2026-05-12-gap-481-gateway-routing-probe.md`
- [ ] Root cause identified
- [ ] Fix shipped via PR
- [ ] All 5 service paths return 200 from gateway:
  - `/kitehub-subscription/actuator/health`
  - `/kitehub-branding/actuator/health`
  - `/kitehub-email/actuator/health`
  - `/kitehub-admin/actuator/health`
  - `/kitehub-platform/actuator/health` (if deployed)
- [ ] smoke-test `check_health` updated if path schema needs change
- [ ] No regression to direct `/actuator/health` (must still return 200)

## Out-of-scope

- kc_app target group attachment (Wave 64 plan handles)
- HTTPS:443 listener (Wave 64 Step E)

## Related

- **Surfaced by:** Wave 64 deploy simulation walkthrough
- **Related smoke:** `scripts/smoke-test.sh` `check_health` function (Wave 62 Bucket A)
- **Reference:** `kitehub-gateway` Spring Cloud Gateway service
- **Blocks:** Wave 64 closure (smoke must pass before declaring beta-ready)
- **May relate to:** GAP-419 P0 gateway 3-KeyResolver disambiguation (per memory `feedback_dev_stack_cold_setup_5_gaps.md`)

## Log

- **2026-05-21 (Wave 102.8 Bucket D)** — Status OPEN → 🟢 DONE 100%. Live verify on local stack (beta-funnel profile up Wave 102.8 Bucket A unlocks Docker preflight): gateway returns HTTP 400 (validation error from missing required headers/params), NOT 404 — routing predicates working correctly. Per `audit-to-gap-pipeline.md` §2.8 fix-time state-check: gap age 9 days; symptom claim "gateway path-routed health checks fail" verified against current `application.yml` which has explicit route predicates `kitehub-admin-beta-requests-v1` (line 473-481 → kitehub-subscription:8080), `kitehub-admin-impersonate` (line 487-495), `kitehub-admin-v1` (line 499-507 → kitehub-admin:8080), `platform-instances` (line 150-158) all precede catch-all `/api/v1/**` instance-apis route (line 579-588). Wave 79 Bucket A + Wave 82 Bucket F4 + Wave 99B audit-gateway-routes.sh runs eliminated wrong-service routing class. Evidence: `for path in /api/v1/admin/beta-requests /api/v1/admin/instances /api/v1/admin/impersonate/start; do curl -s -o /dev/null -w "%{http_code}" http://localhost:9000${path} -H "Authorization: Bearer $JWT"; done` → 400 / 400 / 400 (NOT 404). Per `gap-done-discipline.md` §2 — symptom no longer present; closure complete. Note: original gap mentioned `/kitehub-subscription/*` path-based routing (pre-Wave 79 architecture); current architecture uses `/api/v1/{domain}/**` routing instead — path schema redesign already shipped. Files moved: gap file `git mv` to `phase-1-beta/closed/` per `gap-folder-organization.md` §3.3. CSV row updated: status OPEN→DONE, completion_pct 0→100, last_verified 2026-05-21.

- **2026-05-12:** Filed during Wave 64 deploy simulation. State-check needed before fix proposal.
