# GAP-071: Branding Migration on Tier Upgrade / Downgrade

**Status:** 🔵 OPEN
**Priority:** 🟡 P2
**Domain:** AI Branding / Product / Billing
**Detected:** 2026-04-14 (simulation-gap-finder on Wave 3 scope)
**Matrix cell:** Owner × Evolution × C10 Evolution

## Problem

Khi tenant upgrade (FREE → PRO → PREMIUM → ENTERPRISE) hoặc downgrade, current branding KHÔNG tự động:

- **Upgrade:** unlock premium templates — có nên auto-migrate sang template cao cấp hơn? Hay giữ current để không disrupt? GAP-036 cover UX **reveal** nhưng không define migration logic.
- **Downgrade:** tenant đã dùng PREMIUM template → downgrade về FREE — template có bị lock? Branding hiện tại vẫn hiển thị? Legal angle: user đã "mua" → ownership debated.

Hiện system không có policy → kỳ vọng tenant conflict.

## Evidence

- GAP-036 Tier Upgrade UX scope: reveal cards + unlock animation, không phải migration runbook
- `BrandingResource.category` (Wave 2 GAP-007) phân loại STATIC/TEMPLATE/FULL_AI, không theo tier
- Không có `tier_locked` field trên template entity
- Billing downgrade event không trigger branding check

## Proposed Fix

### 1. Template tier-locking metadata

```java
@Entity
class ImageTemplate {
  // ... existing
  SubscriptionTier minimumTier;  // FREE, PRO, PREMIUM, ENTERPRISE
}
```

### 2. Upgrade migration policy

Config `branding.upgrade.migration-policy`:
- `NONE` (default) — current branding stays; tenant must rebrand manually
- `SUGGEST` — banner trong dashboard "Your tier unlocks better templates. Rebrand?"
- `AUTO` — auto-enqueue rebrand job với new tier's template library

Most tenants expect SUGGEST. AUTO rủi ro vì branding là visual identity — auto-change sẽ hoang mang.

### 3. Downgrade retention policy

Config `branding.downgrade.retention-policy`:
- `GRACE_PERIOD` (default) — current branding giữ nguyên 30 ngày, warn via email
- `IMMEDIATE_FALLBACK` — immediately rebrand với FREE-tier template
- `LOCK_PREMIUM_ASSETS` — keep display nhưng disable rebrand / regenerate (read-only)

Recommended: GRACE_PERIOD + conversion-attempt before rebrand.

### 4. Saga on tier change

```
tenant.tier-changed event (từ billing)
  → BrandingTierChangeSaga
    → If upgrade + SUGGEST: create notification + dashboard banner
    → If upgrade + AUTO:    enqueue PlanExecutor (full rebrand)
    → If downgrade + GRACE: schedule warn-email + 30d-later fallback-rebrand
    → If downgrade + IMMEDIATE: enqueue rebrand now với free-tier templates
    → Write audit + outbox event
```

## Acceptance Criteria

- [ ] `minimum_tier` column on `image_templates` + migration
- [ ] Policy config keys documented in `rules.md`
- [ ] `BrandingTierChangeSaga` listens `tenant.tier-changed` event
- [ ] Admin UI per-tenant override of policy
- [ ] Tenant email notification cho each migration trigger
- [ ] E2E: upgrade FREE → PRO with AUTO policy → branding re-generates
- [ ] E2E: downgrade PREMIUM → FREE with GRACE → email + 30d grace

## Dependencies

- GAP-036 (tier upgrade UX) — reveal cards link here
- GAP-026 (trial/freemium mechanics) — tier change events
- Wave 3 GAP-008 (agent workflow) — execution path
- Wave 3 Outbox (Sub-PR 3.1) — event reliability

## Target Wave

**Wave 7 UX Polish** (Sprint 5) — ties to tier-related UX.

Does NOT block Wave 3.

## Log

- 2026-04-14 — Detected via simulation-gap-finder (evolution stage, tier change policy missing)
