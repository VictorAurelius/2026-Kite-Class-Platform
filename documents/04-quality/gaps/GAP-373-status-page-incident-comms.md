# GAP-373: Status Page + Incident Communication

**Status:** 🔵 OPEN
**Priority:** 🟠 P1 STRONGLY recommend (Phase 1 BETA — không block launch nhưng professionalism)
**Domain:** Infrastructure / Operations / Customer Support
**Found:** 2026-05-06 (Release 1 deploy plan)
**Affects:** Tenant trust during incidents, professional appearance

## Problem

KHÔNG có status page hoặc official incident communication channel. Trong incident:
- Tenants không biết có downtime
- Coordinator phải email từng tenant manually
- No public history of incidents → no SLA tracking
- Less professional appearance

## Proposed Fix

### Option A — Hosted (recommend cho beta)
- **Vendor:** Statuspage.io / Atlassian Statuspage / Instatus
- Free tier limits ~50 subscribers
- Cost: $0-29/mo
- Setup: ~half day

### Option B — Self-hosted
- **Tools:** Cachet / Statping / Uptime-Kuma
- Free, but need infra
- Setup: ~1-2 ngày

**Recommend Option A (hosted)** for beta period to minimize ops overhead. Migrate to self-hosted post-Release 1 if cost concerns.

### Setup

- Domain: `status.kitehub.vn`
- Components: KiteHub API, KiteClass API, Marketing site, Auth, Email delivery, AI Branding
- Incident severity: Critical / Major / Minor / Maintenance
- Subscriber notifications: email + RSS
- Public history (last 90 days)
- Manual incident creation flow

### Incident comms procedure

1. **Detect** — alert fires (Grafana / Sentry)
2. **Triage** — coordinator confirms severity
3. **Post incident** — create on status page với initial message
4. **Update** — at least every 30 min trong active incident
5. **Resolve** — mark resolved + RCA summary
6. **Post-mortem** — email subscribers within 48h with details

## Acceptance Criteria

- [ ] Status page vendor chosen + account created
- [ ] DNS subdomain `status.kitehub.vn` configured
- [ ] Components defined (5+ minimum)
- [ ] Incident severity levels documented
- [ ] Subscriber email integration (use GAP-370 email infra)
- [ ] Incident comms runbook drafted
- [ ] Post-mortem template
- [ ] Smoke test: create test incident → resolve flow

## Open decisions

- Vendor pick (Statuspage vs Instatus vs free tier of either)
- Self-hosted migration timeline (if any)
- SLA targets (uptime 99.9%? 99.5% beta?)

## Effort estimate

~half day setup + ~half day docs.

## Related

- Parent plan: `documents/03-planning/roadmap/release-1-deploy-plan.md`
- Sister: GAP-369 (DNS), GAP-370 (email)

## Log

- **2026-05-06:** Filed by Release 1 deploy plan PR. Phase 1 BETA strongly recommend — professionalism + reduce 1-on-1 incident comms overhead.
