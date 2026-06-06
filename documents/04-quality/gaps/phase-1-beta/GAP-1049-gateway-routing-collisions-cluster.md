# GAP-1049: 5 gateway route collision (shadowed/public paths route to wrong service or 400)

**Status:** 🟡 PARTIAL
**Priority:** 🟠 P1
**Domain:** Backend (gateway)
**Found:** 2026-06-07 (G3 production-parity P2 cluster A — gateway route predicate audit, child of GAP-1042 META)
**Affects:** `kitehub-gateway` route table (`application.yml`) — 5 collisions across 3 backend services

## Problem

GAP-1042 META gateway route-predicate audit surfaced 5 concrete route collisions remaining after Wave security-1 closed GAP-1031/1034/1041. Each is a broad catch-all (`/api/platform/admin/**` → kitehub-admin, `/api/v1/admin/**` → kitehub-admin, `/api/v1/**` → kiteclass-core +TenantResolver) shadowing a narrower path the catch-all's target service does NOT own, OR a public path forced through TenantResolver → 400.

| # | Path | Real service | Was routing to | Class | Severity |
|---|---|---|---|---|---|
| C1 | `/api/platform/admin/payments/**` (AdminPaymentController) | kitehub-subscription | kitehub-admin (404) | wrong-service shadow | P1 |
| C2 | `/api/v1/preferences/**` (PreferencesController `dismiss-banner-state`) | kitehub-subscription | kiteclass-core (404) | wrong-service shadow | P2 |
| C3 | `/api/v1/admin/parent/consent/**` (ParentConsentAdminController) | kiteclass-core | kitehub-admin (404) | wrong-service shadow | P1 (active Phase 1) |
| C4 | `/api/v1/payments/webhook/**` (PaymentWebhookController vnpay/momo/zalopay) | kiteclass-core | kiteclass-core +TenantResolver (400) | TenantResolver-400 (public path) | P1 |
| C5 | `/api/v1/parent-invitations/redeem/{token}` (ParentInvitationController) | kiteclass-core | kiteclass-core +TenantResolver (400) | TenantResolver-400 (public path) | P1 (active Phase 1) |

C4/C5 are public endpoints (payment provider callback / email-link recipient) that carry no tenant context — the `/api/v1/**` catch-all's TenantResolver rejects them with 400 even though the target service is correct.

## Root Cause

Same systemic root as GAP-1042: gateway route table lacks predicate discipline — narrow paths owned by service B are shadowed by a `/**` catch-all targeting service A declared earlier. Plus a second collision class (C4/C5): public-by-design paths inheriting TenantResolver from the catch-all.

**Detector blind spots (BS) that let C3/C4/C5 escape `audit-gateway-routes.sh`:**
- **BS#1** — the detector scanned only the 4 kitehub modules, so kiteclass-core controllers shadowed by a kitehub catch-all (C3/C4/C5) were invisible.
- **BS#2** — the detector only modeled wrong-service routing; it did not model the TenantResolver-400 case (C4/C5 route to the correct service kiteclass-core but get a 400 from TenantResolver on a public path).

## Proposed Fix

1. 5 carve-out routes in `application.yml`, each declared BEFORE its shadowing catch-all (route order = Spring Cloud Gateway match order):
   - C1 `platform-admin-payments-subscription` → kitehub-subscription, before `platform-admin`.
   - C2 `kitehub-preferences-v1` → kitehub-subscription (no TenantResolver, user-scoped), before `instance-apis`.
   - C3 `kiteclass-parent-consent-admin` → kiteclass-core (+TenantResolver, tenant-scoped admin), before `kitehub-admin-v1`.
   - C4 `kiteclass-payments-webhook` → kiteclass-core (NO TenantResolver, public), before `instance-apis`.
   - C5 `kiteclass-parent-invitation-redeem` → kiteclass-core (NO TenantResolver, public), before `instance-apis`. Owner-scoped create stays on the catch-all WITH TenantResolver.
2. META detector `audit-gateway-routes.sh`: add kiteclass-core to scan scope (BS#1); model TenantResolver-400 collision class for public-by-design paths (BS#2); harden the path matcher for mid-path `*` + `{var}` URI-template vars (else BS#1 false-positives); exempt `/internal/**` service-to-service endpoints.

## Acceptance Criteria

- [x] 5 carve-out routes added, each before its catch-all (declaration-order verified)
- [x] C4/C5 public routes SKIP TenantResolver
- [x] GAP-1034 (branding) + GAP-1041 (payroll) carve-outs untouched
- [x] `audit-gateway-routes.sh` scans kiteclass-core (BS#1) + models TenantResolver collision (BS#2)
- [x] detector PASS (exit 0) with carve-outs present; catches all 5 collisions when carve-outs absent (before/after evidence in PR)
- [x] YAML valid + shellcheck clean
- [ ] Runtime gateway rebuild + route smoke (C1-C5 reach correct service, public paths no 400) — P3 G3 re-walk, coordinator

## Related

- Parent META: GAP-1042 (gateway route-predicate audit)
- Sister closed: GAP-1031 (email expose) / GAP-1034 (branding shadow) / GAP-1041 (payroll shadow)
- `production-env-config-registry.md` §11 — `audit-gateway-routes.sh` ownership
- Discovered in: G3 production-parity P2 cluster A
