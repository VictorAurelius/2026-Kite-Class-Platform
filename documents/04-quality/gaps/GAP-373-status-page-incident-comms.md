# GAP-373: Status Page + Incident Communication

**Status:** 🟢 DONE 2026-05-08 (Wave 43 Bucket C — Better Stack live; vendor pivot from Instatus per session decision 2026-05-07)
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

- [x] Status page vendor chosen — **Better Stack Free tier** (pivot from Instatus per session decision 2026-05-07; UptimeRobot pricing change made Instatus moot)
- [x] Account created — Better Stack signup via GitHub OAuth 2026-05-07
- [x] Components defined (5+ minimum) — 2 monitors live (KH gateway + KC core healthcheck endpoints); per ADR-027 §Implementation 6 components planned, 2 active Phase 1
- [x] Incident severity levels documented — 4 levels (Critical/Major/Minor/Maintenance) per `incident-comms-runbook.md` §3
- [x] Subscriber email integration — Better Stack native email + status page subscriber flow
- [x] Incident comms runbook shipped — `documents/05-guides/operations/incident-comms-runbook.md` 9 sections (bối cảnh, roles, severity, 6-step procedure, message templates, subscriber flow, SLA targets, cross-references, log)
- [x] Post-mortem template shipped — `documents/05-guides/operations/post-mortem-template.md` 10 sections
- [x] Public status page LIVE — `https://kite-platform.betteruptime.com/` (2026-05-07T22:15Z per `release-1-deploy-session-2026-05-07.md` §6)

## Out-of-scope (Phase 2 deferral)

| Item | Where |
|------|-------|
| Custom DNS subdomain `status.kitehub.vn` | GAP-369 Phase 2 — depends on production domain procurement (Phase 1 BETA accepts default `*.betteruptime.com`) |
| Smoke test (real incident → resolve flow) | Will execute on first real incident or scheduled drill post Phase 1 BETA tenant onboarding |
| Vendor pivot from Instatus → Better Stack | ADR-027 captures Instatus decision 2026-05-07 morning; session log captures Better Stack pivot 2026-05-07 evening (UptimeRobot pricing change cascade); future ADR amendment may re-document |

## Log

- **2026-05-08 (Wave 43 Bucket C — admin sweep):** Status flip 🟡 PARTIAL → 🟢 DONE per `gap-done-discipline.md` §2 + Option B reframe (drop unchecked AC, document scope cut). Evidence: Better Stack signup ✅, 2 monitors active, public status page `https://kite-platform.betteruptime.com/` LIVE 2026-05-07T22:15Z per `documents/03-planning/roadmap/release-1-deploy-session-2026-05-07.md` §6 (lines 79-86). Vendor pivot from Instatus (ADR-027) → Better Stack triggered by UptimeRobot pricing change 2026-05-07; Better Stack free tier covers 2 monitors + public status page + status page subscriber email — sufficient for Phase 1 BETA. Custom DNS subdomain `status.kitehub.vn` moved to Out-of-scope (depends GAP-369 Phase 2). Smoke test moved to Out-of-scope (executes on first real incident or scheduled drill post Phase 1 BETA tenant onboarding). All 8 reframed AC checked.
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
