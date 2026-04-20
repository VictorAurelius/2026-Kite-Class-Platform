---
title: Trial → Paid Conversion — Business Requirements Document
status: draft
created: 2026-04-20
updated: 2026-04-20
owner: Product / Business Lead
related_gaps: [GAP-192, GAP-026, GAP-108, GAP-150]
---

# BRD: Trial → Paid Conversion

## 1. Why this document exists

The trial-to-paid conversion is the core SaaS revenue path for KiteHub. Any downtime, data-loss risk, or confusing UX at upgrade moment = lost revenue + churn. GAP-192 (filed 2026-04-20, Business-Logic-P0 per `.claude/rules/meta-gap-priority.md` §3) identified that while the basic status flip exists (`TrialService.convertTrialToSubscription()`), the project has no formal design for:
- Zero-downtime guarantee
- Rollback on payment reversal
- State machine formalization
- Event-driven downstream updates (branding, cache, email)

This BRD captures the business-level requirements; the technical realization lives in `documents/01-business/kitehub/trial-to-paid-migration/` (3-layer docs).

## 2. Business Outcomes

| Outcome | Metric | Target |
|---------|--------|--------|
| Conversion funnel health | Trial → Paid conversion rate | ≥ 15% (baseline, industry 5–20%) |
| Zero-trust at upgrade | User-visible downtime during upgrade | 0 seconds |
| Backend efficiency | P95 backend migration time | ≤ 5 seconds |
| Rollback safety | Successful rollback within 24h window | 100% |
| Support ticket rate | Upgrade-related tickets / 100 upgrades | ≤ 2 |
| AI-branding integrity | Tier-appropriate branding refreshed post-upgrade | 100% |

## 3. Stakeholders

- **Primary user personas affected:** P1 Solo Teacher, P2 Small Center, P3 Mid Center, P5 K-12 School (all Tier 1 from `personas-catalog.md`)
- **Business roles:** Finance (subscription revenue recognition), Support (handles ticket fallback), Legal (refund/dispute policy alignment)
- **Technical roles:** Platform engineering (Instance service), Billing (payment gateway integration), AI branding (post-upgrade template refresh)

## 4. Business Constraints

- **Vietnamese Consumer Protection Law** — refund/reversal must complete within 7 days of customer request; our 24h reversal window exceeds statutory minimum.
- **Tax regulations (VAT/TCT Circular 78/2021)** — once an invoice is issued (on PAYMENT_CAPTURED), audit-retention is 7 years. Rollback does not delete the invoice record; it issues a credit note.
- **Data residency (Decree 53/2022)** — all conversion data stays in-country; this rules out cross-region shadow provisioning for cross-tier upgrades.

## 5. Upgrade Journey (Business Perspective)

| Stage | User experience | Business intent |
|-------|-----------------|-----------------|
| Trial countdown visible | Days-left badge, warning emails (from `trial-lifecycle/`) | Prompt conversion before expiry |
| Upgrade prompt | Modal / banner with tier comparison + pricing | Convert before trial expires |
| Payment capture | Stripe-like flow with tier + cycle selection | Collect revenue immediately |
| Backend migration | Modal spinner ≤ 5s | Hide complexity from user |
| Dashboard post-upgrade | Welcome banner, tier-appropriate branding, AI-budget update | Reinforce value of paid tier |
| Failure recovery | Support banner if migration fails; ops paged | Safety net, non-zero but rare |

## 6. Key Business Rules Referenced

See `documents/01-business/kitehub/trial-to-paid-migration/rules.md` for the authoritative T2P-01 … T2P-14 rule list. Business-priority rules:
- **T2P-02** (0 downtime) — must not regress
- **T2P-04** (24h reversal window) — aligns with consumer protection spirit
- **T2P-12** (AI-budget carryover) — no perceived loss when upgrading
- **T2P-13** (7-year audit retention) — tax law compliance

## 7. Out of Scope (this BRD)

- **Trial mechanics:** owned by `trial-lifecycle/` (GAP filed separately if needed)
- **AI-budget policy at upgrade:** owned by GAP-026 (referenced)
- **Subscription pricing tiers:** owned by `subscription-billing/`
- **Off-boarding / cancellation after ACTIVE:** owned by GAP-201
- **Multi-region deployment:** not yet supported; VN-only

## 8. Dependencies

- **GAP-108** — trial config hardcoded in call sites; must close before T2P config is enforceable
- **GAP-026** — trial/freemium AI mechanics must cover carryover semantics
- **GAP-150** — BRD strategic skeletons (pricing-model, business-objectives) provide context for target conversion rate

## 9. Open Questions

1. **Cross-tier upgrade (FREE → ENTERPRISE) strategy:** current design = flip-in-place; should Enterprise-specific assets (custom domains, SSO) be provisioned in a shadow pattern instead? Flag-gated, deferred.
2. **Reversal window SLA:** 24h chosen by judgment; review with Finance + Support to confirm rate of legitimate reversals.
3. **MIGRATION_FAILED recovery playbook:** ops runbook drafting is out-of-scope for this BRD; track as follow-up.

## 10. Log

- 2026-04-20 — Created under GAP-192 Phase 3 to provide business-level context for the 3-layer technical docs. Will be absorbed into GAP-150 Phase 2 BRD collection if `pricing-model.md` or `business-objectives.md` later covers conversion funnel.
