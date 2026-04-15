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
| PRO | No |
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

## Log
- 2026-04-14 — Initial rules (Wave 3 Sub-PR 3.5, GAP-070)
