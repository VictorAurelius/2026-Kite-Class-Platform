# GAP-353c-followup-dpo-email-notification: DSAR DPO email notification flow

**Status:** 🟢 DONE 2026-05-09 (Wave 48 Bucket A)
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

- [x] `EmailServiceClient` injected into `DsarServiceImpl`
- [x] DPO email template `dsar-new-ticket-dpo` rendered + dispatched on submit
- [x] Requester acknowledgement email rendered + dispatched on submit
- [x] Email dispatch failure does not block ticket persistence (outbox or try/catch envelope)
- [x] Unit test verifies email client invoked with correct payload
- [x] `notifyDpo` log line preserved as audit fallback (defense in depth)

## Related

- Parent gap: GAP-353c (Wave 26 Bucket A)
- Cross-module: kitehub-email service API surface
- Pattern reference: `design-patterns.md` §3.5.1 (Outbox bypass policy)

## Effort estimate

~2-4h once `kitehub-email` API ready.

## Log

- **2026-05-09 (Wave 48 Bucket A shipped):** `EmailServiceClient` extended with `sendDsarNewTicketDpoEmail` + `sendDsarAcknowledgementEmail` (re-using `dispatchEmail` outbox-first pipeline per `design-patterns.md` §3.5.1 Exception A — outbox is the reliability net via `EmailServiceClient.publishToQueue` line 595). 2 Thymeleaf templates created: `dsar-new-ticket-dpo.html` (DPO ops alert with ticket fields + admin queue CTA) + `dsar-acknowledgement-requester.html` (requester confirmation with 20-day SLA reassurance + status check link). `DsarServiceImpl.notifyDpo` now wires both dispatches inside try/catch envelopes; ticket transaction commits before notification step + failure does NOT roll back ticket. Audit log line preserved verbatim as defense-in-depth fallback. `DsarServiceImplTest` extended with 3 new tests (`submitRequestDispatchesDpoEmail`, `submitRequestDispatchesRequesterAcknowledgement`, `submitRequestEmailFailureDoesNotRollbackTicket`) — all 7 DsarServiceImplTest tests pass + `mvn -pl kitehub-subscription verify -P strict-warnings` BUILD SUCCESS (455 tests total, 0 failures). `BR-PDPL-DSAR-006` (DPO email notification address `dpo@kitehub.vn`, config key `kitehub.dsar.dpo-email`) added to `documents/01-business/kitehub/marketing/rules.md` with full 5-attribute review per `business-logic-review.md` v1.0.0. Cascade: GAP-353c parent flips 🟡 PARTIAL → 🟢 DONE per `gap-done-discipline.md` §2 (last unchecked AC item satisfied — all 11 ACs verified).
- **2026-05-06:** Filed as PARTIAL exit-ramp deferral from GAP-353c Wave 26 Bucket A per `gap-done-discipline.md` §3.
