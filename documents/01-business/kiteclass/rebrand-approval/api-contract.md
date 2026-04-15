# Rebrand Approval — API Contract

> REST layer lands in a follow-up Sub-PR. This document fixes the service-level contract.

## RebrandApprovalService

### request(instanceId, initiatorUserId, expectedVersion, reason) → RebrandApproval
- Returns PENDING approval on success.
- Throws ConcurrentRebrandException (HTTP 409) on:
  - version mismatch (stale UI)
  - existing PENDING approval for the same instance

### approve(approvalId, approverUserId) → RebrandApproval
- Transitions PENDING → APPROVED.
- Throws ConcurrentRebrandException when approver == initiator.
- Throws IllegalStateException when current status is terminal.

### reject(approvalId, approverUserId, rejectionReason) → RebrandApproval
- Transitions PENDING → REJECTED.
- Same validation as approve().

### expireDueApprovals() → int
- Scheduler entrypoint. Returns count of rows expired.
- Idempotent per row (already-terminal rows are skipped by the WHERE clause).

## Future REST surface (follow-up Sub-PR)

| Method | Path | Purpose |
|--------|------|---------|
| POST | /api/v1/instances/{id}/rebrand-approvals | Request (requires If-Match) |
| POST | /api/v1/rebrand-approvals/{approvalId}/approve | Approve |
| POST | /api/v1/rebrand-approvals/{approvalId}/reject | Reject (reason required) |
| GET | /api/v1/rebrand-approvals?status=PENDING | Admin queue |

**Error codes:**
- 400: validation (missing reason, invalid TTL)
- 404: approval id not found
- 409: version mismatch / pending duplicate / approver-equals-initiator
- 422: attempt to mutate terminal approval

## Log
- 2026-04-14 — Initial contract
