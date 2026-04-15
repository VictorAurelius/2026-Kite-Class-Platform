# Rebrand Approval — Use Cases

### UC-APRV-01: Request Rebrand (Enterprise)
- **Actor:** Tenant admin
- **Call:** POST /api/v1/instances/{id}/rebrand-approvals (REST wiring in follow-up)
- **Headers:** `If-Match: <instance.version>`
- **Service:** RebrandApprovalService.request(instanceId, initiatorUserId, expectedVersion, reason)
- **Steps:**
  1. Load FrontendInstance
  2. Compare expectedVersion to instance.@Version
  3. If mismatch → ConcurrentRebrandException (HTTP 409)
  4. Check no PENDING approval exists for this instance
  5. Create RebrandApproval(status=PENDING, expiresAt=now+24h)
  6. Emit rebrand.requested outbox event
- **Postcondition:** Approval PENDING; admin-B notified (email in later wave)

### UC-APRV-02: Approve
- **Actor:** Second admin (ADMIN role, different user)
- **Service:** RebrandApprovalService.approve(approvalId, approverUserId)
- **Steps:**
  1. Load approval
  2. Reject if approverUserId == initiatorUserId (BR-APRV-002)
  3. Transition PENDING → APPROVED
  4. Record approverUserId + approvedAt
  5. Emit rebrand.approved
- **Next:** subscriber invokes InstanceLifecycleService.rebrand → transitions DEPLOYED → REGENERATING

### UC-APRV-03: Reject
- **Actor:** Second admin
- **Service:** RebrandApprovalService.reject(approvalId, approverUserId, rejectionReason)
- **Steps:**
  1. Load + validate approver ≠ initiator
  2. Transition PENDING → REJECTED
  3. Record rejectionReason
  4. Emit rebrand.rejected
- **Postcondition:** Instance stays DEPLOYED; initiator notified

### UC-APRV-04: Auto-Expire Stale Requests
- **Actor:** Scheduled job (Spring @Scheduled, follow-up PR)
- **Service:** RebrandApprovalService.expireDueApprovals()
- **Steps:**
  1. Query PENDING with expiresAt < now
  2. Transition each to EXPIRED
  3. Emit rebrand.expired per row
- **Cadence:** every 15 minutes (config tunable)

### UC-APRV-05: Stale UI Click (Race)
- **Actor:** Admin who opened the form before another admin mutated the instance
- **Trigger:** expectedVersion (from If-Match header) doesn't match current @Version
- **Result:** 409 ConcurrentRebrandException with message "version changed since you opened the rebrand form — refresh and retry"
- **UX:** front-end catches 409, prompts user to reload

### UC-APRV-06: Duplicate Request (Race)
- **Actor:** Two admins clicking "request rebrand" simultaneously
- **Result:** First one wins; second gets 409 "already pending for instance X"
- **Resolution:** second admin must wait for the first to be actioned (or expire)

## Log
- 2026-04-14 — Initial UCs
