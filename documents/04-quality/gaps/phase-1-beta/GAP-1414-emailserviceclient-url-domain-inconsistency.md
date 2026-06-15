# GAP-1414: EmailServiceClient hardcodes ~25 URLs with prod-domain inconsistency (.com/.me/.vn)

**Status:** 🔵 OPEN
**Priority:** 🟠 P1
**Domain:** Backend
**Found:** 2026-06-15 (hardcode-mock state-check, BE agent — largest functional hardcode cluster)
**Affects:** `kitehub-subscription/.../client/EmailServiceClient.java` + `OwnerNotificationDispatcher.java` + `DomainService.java`

## Problem

`EmailServiceClient.java` hardcodes ~25 URLs across customer email templates with **3 conflicting production domains in ONE file**: 9× `https://kitehub.com`, 9× `https://kitehub.me`, 3× `https://kitehub.vn` (e.g. `:124 upgradeUrl=kitehub.com/pricing`, `:510 loginUrl=kitehub.vn/login`, `:333 https://%s.kitehub.me/dashboard`). Real domain = `kitehub.me` (per `kitehub-kiteclass-boundary` §2) → emails point to ≥2 WRONG domains → broken/dead links in customer-facing emails. HARDCODE (extract to config), functional P1. Sister: `OwnerNotificationDispatcher.java:91-139` billing URLs + `DomainService.java:285` backup URL domain hardcoded.

## Proposed Fix

Externalize email link base URL to a single config key (`@Value("${kitehub.email.base-url:https://kitehub.me}")` or env). Replace all `.com`/`.vn`/`.me` literals with the config-driven base + path. Sweep `OwnerNotificationDispatcher` + `DomainService` same fix (per `cross-flow-bug-class-sweep`). Distinct from GAP-692 (docs/scripts/terraform env-hardcode) — this is Java email-link domain drift.

## Acceptance Criteria

- [ ] Single config key drives all email link domains (no `.com`/`.vn` literals)
- [ ] `grep -E "kitehub\.(com|vn)" kitehub-subscription/.../EmailServiceClient.java` = 0
- [ ] OwnerNotificationDispatcher + DomainService swept same
- [ ] Email link smoke (per `smoke-email-links.sh` GAP-802) → non-404 + correct domain

## Related

- Umbrella: GAP-1410 · Audit: `2026-06-15-hardcode-mock-state-check.md`
- GAP-692 (env hardcode docs/scripts/tf — different scope); `kitehub-kiteclass-boundary` §2 (domain canonical); GAP-802 email-link smoke
