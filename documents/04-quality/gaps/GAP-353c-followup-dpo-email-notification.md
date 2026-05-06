# GAP-353c-followup-dpo-email-notification: DSAR DPO email notification flow

**Status:** 🔵 OPEN
**Priority:** 🟡 P2 (DSAR ticket persists + structured log emits regardless; missing only proactive DPO email)
**Domain:** Backend (kitehub-subscription ↔ kitehub-email integration)
**Found:** 2026-05-06 (Wave 26 Bucket A GAP-353c PARTIAL exit-ramp)
**Affects:** `kitehub-subscription/dsar/service/DsarServiceImpl.notifyDpo`

## Problem

Wave 26 Bucket A shipped `DsarServiceImpl.submitRequest` which currently emits a structured
`dsar.ticket.created` log line in lieu of a proper async DPO email dispatch. The log line is
captured by aggregation and gives DPO an audit trail when grepping logs, but it is NOT a
push notification — DPO must proactively pull. For a 20-day SLA queue this is acceptable
short-term (DPO already runs daily ticket review), but inferior to push-based delivery.

## Current State (verified 2026-05-06)

| Artifact | Status |
|---|---|
| `DsarServiceImpl.notifyDpo(...)` | ✅ exists — emits `log.info("dsar.ticket.created event=... ...")` |
| `kitehub-email` async API surface | ❌ not yet exposed cross-module |
| `EmailServiceClient` adapter in `kitehub-subscription` | ❌ missing |

## Proposed Fix

When `kitehub-email` exposes an async dispatch API (REST or RabbitMQ event):

1. Replace `notifyDpo` log line with `emailServiceClient.dispatch(EmailDispatchRequest...)`
2. Add tier-routed template: `dsar-new-ticket-dpo` (DPO inbox) + `dsar-acknowledgement-requester` (requester confirmation)
3. Wire failure-tolerance: email failure must NOT roll back ticket creation — outbox pattern recommended (see `design-patterns.md` §3.5.1 Exception D dispatcher rules)
4. Update `DsarServiceImplTest` with mock `EmailServiceClient` verification

## Acceptance Criteria

- [ ] `EmailServiceClient` injected into `DsarServiceImpl`
- [ ] DPO email template `dsar-new-ticket-dpo` rendered + dispatched on submit
- [ ] Requester acknowledgement email rendered + dispatched on submit
- [ ] Email dispatch failure does not block ticket persistence (outbox or try/catch envelope)
- [ ] Unit test verifies email client invoked with correct payload
- [ ] `notifyDpo` log line preserved as audit fallback (defense in depth)

## Related

- Parent gap: GAP-353c (Wave 26 Bucket A)
- Cross-module: kitehub-email service API surface
- Pattern reference: `design-patterns.md` §3.5.1 (Outbox bypass policy)

## Effort estimate

~2-4h once `kitehub-email` API ready.

## Log

- **2026-05-06:** Filed as PARTIAL exit-ramp deferral from GAP-353c Wave 26 Bucket A per `gap-done-discipline.md` §3.
