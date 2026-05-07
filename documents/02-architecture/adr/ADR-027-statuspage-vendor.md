# ADR-027: Status Page Vendor — Instatus Free Tier (Phase 1 BETA)

**Status:** ACCEPTED
**Date:** 2026-05-07
**Deciders:** @nguyenvankiet (solo-dev, acting CTO + acting Product Owner)
**Reviewers:** N/A (solo-dev mode per CLAUDE.md decision context locked 2026-05-06)
**Related Gap(s):** GAP-373 (this ADR closes vendor decision); GAP-370 (email infra — subscriber notifications integration)
**Related Rule(s):** `.claude/rules/release-deploy-standard.md` §3 (per-bump-type artifact checklist)

---

## Context

Phase 1 BETA invite-only launch yêu cầu official incident communication channel để:
- Tenants biết có downtime (without email-blast 1-on-1)
- Public history of incidents → SLA tracking
- Professional appearance vs ad-hoc Slack/email

Decision drivers (priority order):
1. **Cost cap:** <$30/mo Phase 1 BETA (matches Architecture B ~$72/mo total budget)
2. **Setup time:** ≤half day (Phase 1 BETA timeline ~9-12 weeks)
3. **Subscriber notifications:** must integrate với GAP-370 email infra OR self-managed
4. **VN-friendly:** UI Vietnamese OR vendor accepts custom domain `status.kitehub.vn`
5. **Component model:** support 5+ components (KiteHub API, KiteClass API, Marketing site, Auth, Email delivery, AI Branding)
6. **Public history:** ≥90 days
7. **Vendor lock-in mitigation:** export-able data history if migrate self-hosted Phase 2

---

## Considered Options

### Option A — Atlassian Statuspage.io
- **Cost:** Hobby $29/mo (Pro $99/mo, Business $249/mo)
- **Pros:** Industry standard, Atlassian-backed, robust uptime, mature feature set, well-documented
- **Cons:** Cost ceiling Phase 1 BETA tight; no free tier; UI English-only
- **Setup:** ~half day

### Option B — Instatus
- **Cost:** Free tier (5 components, 100 subscribers, 90-day history) → $20/mo Pro (unlimited)
- **Pros:** Free tier covers Phase 1 BETA scope (5 components × 100 subscribers × 90 days exactly fits); fast modern UI; custom domain support free; export to JSON; good API
- **Cons:** Smaller vendor (acquisition risk, but data export mitigates); UI English-only
- **Setup:** ~2-3 hours

### Option C — Cachet (self-hosted)
- **Cost:** $0 software + ~$5/mo small VPS
- **Pros:** Full control, open-source, no vendor lock-in, customizable
- **Cons:** Project maintenance gap (last release 2023); needs ops time; subscribers email = self-managed; ~1-2 ngày setup
- **Setup:** ~1-2 days

### Option D — Statping (self-hosted)
- **Cost:** $0 software + ~$5/mo small VPS
- **Pros:** Active OSS project, monitoring + status combined, subscriber notifications, custom branding
- **Cons:** Setup overhead; another service to maintain; ops debt Phase 1
- **Setup:** ~1-2 days

### Option E — Uptime-Kuma (self-hosted, monitoring + status combined)
- **Cost:** $0 software + ~$5/mo small VPS
- **Pros:** Combine với Phase 1 monitoring needs; very active OSS; nice UI
- **Cons:** Public status page mode less polished than dedicated vendors; subscriber model basic
- **Setup:** ~1 day

---

## Decision

**Chọn Option B — Instatus Free tier** cho Phase 1 BETA.

Rationale:
1. **Cost = $0** Phase 1 BETA — fits budget với 0 marginal expense
2. **Free tier coverage exactly matches Phase 1 BETA scope:** 5 components (KH-API, KC-API, Marketing, Auth, Email + spare slot for AI Branding), 100 subscribers (≥10-20 invite tenants với buffer), 90-day history
3. **Setup time 2-3 hours** — fastest to-launch
4. **Custom domain free** — `status.kitehub.vn` works on free tier
5. **Export-to-JSON** mitigates vendor lock-in — if migrate self-hosted Phase 2, history portable
6. **API support** — automation integration possible với GAP-370 email infra (post-mortem auto-email)
7. **Phase 2 escape hatch:** if cost/scale concerns post Phase 1 BETA, migrate to Cachet/Statping với JSON export

Trade-offs accepted:
- Vendor lock-in (mitigated by export); UI English-only (acceptable Phase 1 BETA technical audience)
- Free tier 100 subscriber cap (Phase 1 BETA invite-only ~10-20 → comfortable)

Phase 2 trigger gate to re-evaluate:
- Subscriber count > 80 (approaching free tier cap)
- Need custom workflows (status automation tightly coupled with internal alerting)
- Annual cost projection $20/mo×12 = $240/yr beats self-hosted $5/mo×12 = $60/yr (4× cost difference) → consider migration

---

## Consequences

### Positive
- Zero-cost professional status page Phase 1 BETA
- Fast time-to-launch (~2-3 hours)
- Public domain `status.kitehub.vn` available
- Email subscriber model native (no integration với GAP-370 email infra needed)
- Export-able data history → migration path Phase 2

### Negative
- Vendor dependency (acceptance risk Phase 1 BETA scope)
- UI English-only (technical audience acceptable; tenant-facing communications in Vietnamese via incident message body)
- Free tier 100 subscriber + 5 component caps (sufficient Phase 1 BETA scope)

### Neutral
- Self-hosted alternatives deferred Phase 2 evaluation (Cachet/Statping/Uptime-Kuma if cost/scale cross over)

---

## Implementation steps (post-this-ADR)

Per GAP-373 Acceptance Criteria:
1. User-action: Create Instatus account at https://instatus.com (Phase 1 BETA: free tier)
2. User-action: Configure custom domain `status.kitehub.vn` (DNS CNAME record per GAP-369 DNS runbook)
3. User-action: Define 5 components (KH-API, KC-API, Marketing, Auth, Email; AI Branding optional 6th slot)
4. User-action: Configure incident severity levels per `incident-comms-runbook.md` §3
5. User-action: Email subscriber integration via Instatus native (NOT GAP-370 email infra Phase 1 — simpler)
6. Smoke test: create test incident → mark resolved (cleanup) → verify subscriber email delivered

---

## References

- GAP-373: Status Page + Incident Communication
- `documents/05-guides/operations/incident-comms-runbook.md` (sister doc, this PR)
- `documents/05-guides/operations/post-mortem-template.md` (sister doc, this PR)
- `documents/03-planning/roadmap/release-1-deploy-plan.md`
- `.claude/rules/release-deploy-standard.md` §3 (per-bump-type artifact checklist)

---

## Log

- **2026-05-07:** ADR created Wave 38 Bucket C (coordinator-applied sau Sonnet agent autocompact-thrash). Vendor decision: Instatus Free tier Phase 1 BETA. Phase 2 evaluation trigger gate documented above.
