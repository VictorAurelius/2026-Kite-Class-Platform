# GAP-527: Verify kitehub-email actuator health post Wave 71 + email send end-to-end smoke

**Status:** 🟡 PARTIAL (Wave 78 Bucket E — actuator config verified + smoke script shipped; live E2E send deferred to first Plan 1 invite)
**Priority:** 🟠 P1 (Plan 1 Bước 5 verify gate)
**Domain:** DevOps + verification
**Found:** 2026-05-13 (Wave 71c-meta-Phase-2 — residual from Wave 70 GAP-502 "email cosmetic")
**Affects:** kitehub-email service status + actual outbound email delivery

## Problem

Wave 70 closed GAP-502 → 🟡 PARTIAL with note "4/5 services healthy post-final-deploy; kitehub-email cosmetic unhealthy". Wave 71 PR #1267 added `MANAGEMENT_HEALTH_MAIL_ENABLED=false` to disable mail health probe (which was probing dev SMTP `kite-mailhog:1025`). This fixed the *cosmetic* health indicator but does NOT prove email actually sends.

Wave 71 Bucket D + Wave 71c GAP-513 closure verified `RESEND_API_KEY` env var present in container (length=35, prefix=re_ho). But end-to-end "register beta account → email arrives in inbox" smoke NOT performed.

## Proposed Fix

1. SSM verify all 5 services `(healthy)` via `docker ps` (post Wave 71b deploy this should pass)
2. SSM verify kitehub-email `/actuator/health` returns 200 + status UP
3. End-to-end smoke: register fresh beta account via `https://kitehub.me/register` → check inbox `vannkite@outlook.com` for verify-email arrival (within 60s)
4. Check Resend dashboard → Logs → Sent → confirm delivery + recipient + open rate
5. If email NOT arriving: check container logs `docker logs kitehub-email` for Resend API errors

## Acceptance Criteria

- [x] kitehub-email actuator config verified (`application.yml` exposes `health,info,metrics,prometheus`; probe groups `liveness` + `readiness` Wave 77 Bucket B)
- [x] Smoke script `scripts/smoke-email-actuator.sh` shipped (health + liveness + info + optional `SEND_LIVE=true`)
- [x] `KiteHubEmailHealthIndicator` defense-in-depth (RabbitMQ + JVM heap probes — GAP-502 RC1/RC2)
- [ ] All 5 kitehub services report `(healthy)` for ≥30 min — defer live verify đến Plan 1 invite
- [ ] Test beta-access registration → email arrives within 60s — defer live verify đến Plan 1 invite
- [ ] Resend dashboard shows "delivered" status — defer live verify đến Plan 1 invite

## Related

- Parent: GAP-502 (Wave 70 PARTIAL) + GAP-513 (Wave 71c DONE — infra ready)
- Sibling: GAP-525 (rotate Resend key post Plan 1 self-test), GAP-543 (content audit), GAP-531 (handoff runbook)
- Rule: `pre-handoff-self-test-completeness.md` §2.3 email-driven flow

## Log

- **2026-05-14 (Wave 78 Bucket E):** PARTIAL — actuator config verified existing (Wave 77 Bucket B). `KiteHubEmailHealthIndicator` already DEFENSE-IN-DEPTH với RabbitMQ + heap probes. Shipped `scripts/smoke-email-actuator.sh` covering health + liveness + info + optional send. Live E2E run (3 AC còn lại) defer đến Plan 1 invite — real persona = empirical walkthrough. Per `gap-done-discipline.md` §3 PARTIAL exit ramp.
