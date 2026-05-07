# GAP-373: Status Page + Incident Communication

**Status:** 🟡 PARTIAL 2026-05-07 (Wave 38 Bucket C — vendor decided + 3 docs shipped; account signup + DNS subdomain = user-action)
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

- [x] Status page vendor chosen — **Instatus Free tier** per ADR-027 (`documents/02-architecture/adr/ADR-027-statuspage-vendor.md`)
- [ ] Account created — user-action (Instatus signup)
- [ ] DNS subdomain `status.kitehub.vn` configured — user-action (CNAME per GAP-369 DNS runbook)
- [x] Components defined (5+ minimum) — KH-API, KC-API, Marketing, Auth, Email, AI Branding (6) per ADR-027 §Implementation
- [x] Incident severity levels documented — 4 levels (Critical/Major/Minor/Maintenance) per `incident-comms-runbook.md` §3
- [x] Subscriber email integration — Instatus native (Phase 1); GAP-370 email infra integration deferred Phase 2 per ADR-027 §Decision
- [x] Incident comms runbook shipped — `documents/05-guides/operations/incident-comms-runbook.md` 9 sections (bối cảnh, roles, severity, 6-step procedure, message templates, subscriber flow, SLA targets, cross-references, log)
- [x] Post-mortem template shipped — `documents/05-guides/operations/post-mortem-template.md` 10 sections (header, summary, timeline, RCA 5-Whys, impact, what-went-well/poorly, action items, lessons learned, references, distribution checklist)
- [ ] Smoke test (test incident → resolve flow) — user-action post-account-creation

## Log

- **2026-05-07 (Wave 38 Bucket C — coordinator-applied):** Sonnet agent autocompact-thrashed 2 times (same pattern as Wave 37 Bucket D). Coordinator-applied directly: 3 NEW files (incident-comms-runbook + post-mortem-template + ADR-027). Vendor decision Instatus Free tier (cost $0 Phase 1 + setup ~2-3h + 5 components × 100 subscribers × 90 days fits Phase 1 BETA scope exactly + custom domain free + JSON export Phase 2 escape hatch). Status flipped 🟡 PARTIAL per `gap-done-discipline.md` §3 — 6/9 AC done; 3 deferred user-action (account signup + DNS CNAME + smoke test).

## Open decisions

- Vendor pick (Statuspage vs Instatus vs free tier of either)
- Self-hosted migration timeline (if any)
- SLA targets (uptime 99.9%? 99.5% beta?)

## Effort estimate

~half day setup + ~half day docs.

## Related

- Parent plan: `documents/03-planning/roadmap/release-1-deploy-plan.md`
- Sister: GAP-369 (DNS), GAP-370 (email)

## Standards reference (added 2026-05-06)

Per `.claude/rules/release-deploy-standard.md` §3 — this gap satisfies a checklist item from one of the per-bump-type artifact requirements. Grounded in:

- **AWS Well-Architected Framework** (Operational Excellence / Security / Reliability pillars)
- **The Twelve-Factor App** (config + deploy patterns where applicable)
- **Project source-of-truth:** `documents/02-architecture/deployment-strategy.md` (GAP-103 DONE 2026-04-18)
- **ADR-015** (AWS Agent Plugins evaluation = DEFER Q3 2026)
- **GAP-381** (Claude agent deploy framework — agent role per phase)

## Log

- **2026-05-06:** Filed by Release 1 deploy plan PR. Phase 1 BETA strongly recommend — professionalism + reduce 1-on-1 incident comms overhead.
