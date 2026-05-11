# GAP-070: Concurrent Rebrand Race + Approval Workflow

**Status:** 🟢 DONE (Wave 3 Sub-PR 3.5, merged 2026-04-14; ApprovalStatus state machine + RebrandApproval entity + RebrandApprovalService with optimistic lock + two-person rule + auto-expire. REST endpoints + @Scheduled expiry job deferred to follow-up.)
**Priority:** 🟠 P1 (data integrity + multi-admin correctness)
**Domain:** Backend / AI Branding / Safety
**Detected:** 2026-04-14 (simulation-gap-finder on Wave 3 scope)
**Matrix cell:** Owner × Edge/Error × C3 Data + C5 Security

## Problem

`InstanceLifecycleService` state machine (Wave 2 GAP-009) enforces DEPLOYED → REGENERATING → DEPLOYED transitions — **per-instance**. Nhưng không có:

1. **Concurrent-rebrand protection:** 2 admins đồng thời click "Rebrand" tại thời điểm khác nhau của flow → latest write wins, one admin's prompt/preset silently overwritten.
2. **Approval workflow:** rebrand đi vào GENERATING ngay khi 1 admin click — không có second-eye review. Rủi ro cho enterprise tier khi rebrand ảnh hưởng hàng trăm instance-user.

## Evidence

- `FrontendInstance.transitionTo()` chỉ check state machine, không check optimistic lock `@Version` concurrency vs rebrand intent
- `InstanceLifecycleService.rebrand(Long instanceId)` không nhận reason / initiator field
- Không có `RebrandRequest` entity với pending/approved/rejected status
- GAP-023 (moderation) focus post-generation, không phải pre-generation approval
- GAP-035 (team collaboration) focus collaborative editing UX, không phải approval gate

## Proposed Fix

### Part A: Optimistic locking on rebrand trigger

```java
@Transactional
public FrontendInstance rebrand(Long instanceId, RebrandRequest req, Long expectedVersion) {
  FrontendInstance i = repository.findById(instanceId).orElseThrow();
  if (!i.getVersion().equals(expectedVersion)) {
    throw new ConcurrentRebrandException(
      "Instance version changed since you opened the rebrand form — refresh and retry");
  }
  // ... existing logic
}
```

Controller passes `If-Match: <version>` header to detect stale UI.

### Part B: RebrandApproval entity (enterprise tier only)

```java
@Entity
class RebrandApproval {
  Long id;
  Long instanceId;
  RebrandRequest request;
  ApprovalStatus status;   // PENDING / APPROVED / REJECTED / EXPIRED
  Long initiatorUserId;
  Long approverUserId;     // null until approved
  Instant requestedAt;
  Instant approvedAt;
  Instant expiresAt;       // auto-expire after 24h
  String rejectionReason;
}
```

Flow:
1. Admin A initiates → `RebrandApproval(PENDING)` — instance stays DEPLOYED
2. Admin B (different user, role ADMIN+) reviews
3. B approves → lifecycle transitions DEPLOYED → REGENERATING, approval marked APPROVED
4. B rejects → approval marked REJECTED, no state change
5. Auto-expire after 24h → notify initiator

### Part C: Tier gating

| Tier | Approval required? |
|------|:------------------:|
| FREE | No (solo tenant) |
| PRO | No |
| PREMIUM | No |
| ENTERPRISE | Yes (configurable per instance) |

`branding.rebrand.approval.required` config key, enterprise-default true.

### Part D: Anti-pattern notice

Implementation MUST use **Saga + State Pattern** per `design-patterns.md`:
- Saga: rebrand touches lifecycle + approval + audit + outbox
- State Pattern: ApprovalStatus enum với `allowedTransitions()`

## Acceptance Criteria

- [ ] `ConcurrentRebrandException` thrown on stale version
- [ ] `If-Match` version header required cho `POST /instances/{id}/rebrand`
- [ ] `RebrandApproval` entity + migration + API endpoints
- [ ] Tier-gated: enterprise requires approval, others skip
- [ ] Second admin cannot be same as initiator (role + user check)
- [ ] Expired approvals auto-rejected via `@Scheduled` job
- [ ] Outbox events: `rebrand.requested`, `rebrand.approved`, `rebrand.rejected`, `rebrand.expired`
- [ ] 3-layer docs: `01-business/kiteclass/rebrand-approval/`
- [ ] Unit tests: race condition (2 threads calling rebrand with same version — one wins)
- [ ] Integration test: full approval flow

## Dependencies

- Wave 2 GAP-009 — lifecycle state machine (DONE)
- Wave 3 GAP-008 — agent workflow consumes approval result
- Wave 3 Sub-PR 3.1 — Outbox for approval events

## Target Wave

**Wave 3 Sub-PR 3.5** (AI Agent workflow) — integrate approval gate trước `PlanExecutor.execute`. Effort +1 day.

## Log

- 2026-04-14 — Detected via simulation-gap-finder (race condition in rebrand) on Wave 3 scope
