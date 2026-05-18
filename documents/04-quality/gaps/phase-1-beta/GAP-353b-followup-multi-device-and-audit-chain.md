# GAP-353b-followup: Multi-device cross-browser test + hash-chain audit log + TestContainers IT

**Status:** 🔵 OPEN
**Priority:** 🟡 P2 (PDPL Phase 2 hardening — server consent API itself shipped Wave 25 Bucket A; these items deepen audit guarantees + verification rigour but don't block PDPL Art 11+13 compliance which the LocalStorage MVP + server API together already satisfy)
**Domain:** Backend / Compliance / Testing infrastructure
**Found:** 2026-05-06 (filed at Wave 25 Bucket A closure per `gap-done-discipline.md` §3 PARTIAL exit-ramp for GAP-353b)
**Affects:** `kitehub-subscription/consent/**` + cross-browser test infra

## Problem

GAP-353b shipped the server-side consent API + 36-month retention cron + idempotent upsert in
Wave 25 Bucket A. Three Acceptance Criteria items shipped at *partial* depth and warrant a
follow-up gap so the deferred work has an explicit home (per `gap-done-discipline.md` §1):

1. **Multi-device sync verified — same visitor_id + cross-browser test** — the implementation
   wires the visitor_id through `kite_visitor_id` LocalStorage and the API correctly idempotents
   by visitor_id, but a *live* cross-browser regression test (Chrome + Firefox + Safari sharing
   the same visitor_id from two devices) requires Playwright + multi-browser context infra not
   available in the current solo-dev WSL2 setup. Unit tests + IT cover the contract; live cross-
   device drift can still occur silently.
2. **Audit-log entries on consent write/revoke** — currently logged via SLF4J INFO entries only.
   The gap originally referenced reusing
   `kiteclass/kiteclass-core/.../ChildProtectionAuditServiceImpl` hash-chain pattern; that
   pattern depends on `TenantContext` which is unavailable for pseudonymous pre-tenant visitors.
   The pattern needs adaptation (chain keyed on `visitor_id` rather than `tenant_id` +
   `entity_type`, separate audit table in `kitehub-subscription`).
3. **TestContainers Postgres IT** — `ConsentControllerIT` ships against the existing project
   convention (H2 in-memory + JPA `create-drop` per `application-test.yml`). Adding
   TestContainers Postgres to kitehub-subscription introduces project-wide testing infra
   work (Postgres 17 image pin + WaitStrategy + Testcontainers JUnit5 dep) that crosses Wave 25
   scope.

## Acceptance Criteria

- [ ] Playwright cross-browser test covering: same `kite_visitor_id` shared between Chromium +
      Firefox sessions → both see the same server-side state via GET endpoint after one writes
- [ ] Hash-chain audit-log table for consent events (`consent_audit_log`) + service writing one
      entry per record/revoke + verification API endpoint
- [ ] Decision recorded (ADR or rule update): TestContainers Postgres adoption for
      kitehub-subscription module ITs OR explicit "stay on H2 + run Flyway compatibility check
      separately" decision

## Related

- Parent: GAP-353b (Wave 25 Bucket A — DONE except for these 3 items, status PARTIAL)
- Reference pattern: `kiteclass/kiteclass-core/.../module/childprotection/service/ChildProtectionAuditServiceImpl.java` (hash-chain)
- Wave plan: `documents/03-planning/waves/wave-2026-05-06-25-pdpl-phase-2-infra.md` §3 Bucket A acceptance notes mentioned PARTIAL exit-ramp specifically for cross-browser verification

## Effort estimate

~6-8h (Playwright setup the bulk; hash-chain table + service ~3h; ADR ~1h).

## Log

- **2026-05-06:** Filed at GAP-353b closure to keep `gap-done-discipline.md` §1 honoured —
  parent gap stays PARTIAL until these 3 items land.
