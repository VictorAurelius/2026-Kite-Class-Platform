# Rebrand Approval — Business Rules

**Domain:** rebrand-approval
**Source:** GAP-070, Wave 3 Sub-PR 3.5

## Rules

### Approval lifecycle (State Machine)
| ID | Rule |
|----|------|
| BR-APRV-001 | Status transitions enforced by ApprovalStatus machine; APPROVED/REJECTED/EXPIRED terminal |
| BR-APRV-002 | Approver MUST be different from initiator (two-person rule) |
| BR-APRV-003 | Auto-expires after `DEFAULT_TTL` (24h); scheduler calls expireDueApprovals() |
| BR-APRV-004 | At most ONE PENDING approval per target instance at any time |
| BR-APRV-005 | Only PENDING approvals are mutable; terminal rows audit-only |

### Concurrent rebrand protection (optimistic locking)
| ID | Rule |
|----|------|
| BR-APRV-010 | request() rejects when caller-supplied expectedVersion ≠ instance.@Version → 409 |
| BR-APRV-011 | request() rejects when another PENDING approval exists for the instance → 409 |
| BR-APRV-012 | Controller passes `If-Match: <version>` header; service extracts into expectedVersion |

### Tier gating (applied by caller, not service)
| Tier | Approval required? |
|------|:------------------:|
| FREE | No |
| BASIC | No |
| PREMIUM | No |
| ENTERPRISE | Yes (default true; config-overridable per instance) |

## Config keys

| Key | Default | Purpose |
|-----|---------|---------|
| `approval.rebrand.ttl-hours` | 24 | TTL for PENDING requests |
| `approval.rebrand.enterprise-required` | true | Enterprise-default gate |

## Event catalogue (outbox)

| Event | Trigger |
|-------|---------|
| rebrand.requested | request() persists PENDING row |
| rebrand.approved | approve() transitions to APPROVED |
| rebrand.rejected | reject() transitions to REJECTED |
| rebrand.expired | expireDueApprovals() terminates overdue PENDING rows |

## Five-attribute review per `business-logic-review.md`

Per-rule attributes (Source / Rationale / Reviewer / Compliance check / Review cadence) backfilled at file-level placeholder per Phase 1 of GAP-433. Per-rule granularity tracked via GAP-156 Phase 2 stakeholder sign-offs.

- **Source:** Existing rules in this file derive from a mix of: feature gaps cited inline (where present), ADRs, persona reviews, and informed-gut estimates from Wave 1-30 work. Rules without inline citation default to `informed gut` per `business-logic-review.md` §2.1 and inherit quarterly re-review obligation below.
- **Rationale:** Rule values reflect product judgment + (where applicable) competitor benchmarks + VN regulatory minimums. Detailed per-rule rationale to be backfilled during GAP-156 Phase 2 stakeholder review; until then, treat values as `informed gut` subject to next quarterly review.
- **Reviewer:** @nguyenvankiet (acting Product Owner, solo-dev, 2026-05-08). Formal stakeholder + legal counsel sign-off queued via GAP-156. Solo-dev exemption per `business-logic-review.md` §2.3 — the Reviewer line documents which hat is being worn AND obligation is attached for team-growth or pre-launch trigger.
- **Compliance check:** **Considered** — Luật Quảng cáo 2012 (brand claim review); content-moderation cross-check.
- **Review cadence:** Quarterly (default per `business-logic-review.md` §2.5). **Next review:** 2026-08-08. Event triggers: Approval workflow SLA change, ≥10 false-positive rejections.

## Log
- 2026-04-14 — Initial rules (Wave 3 Sub-PR 3.5, GAP-070)
