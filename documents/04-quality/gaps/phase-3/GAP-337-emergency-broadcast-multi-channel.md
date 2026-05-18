# GAP-337: Emergency Broadcast Workflow (1500+ PH ≤5 min Multi-channel + Failover)

**Status:** 🔵 OPEN
**Priority:** 🟠 P1
**Domain:** Backend
**Detected:** 2026-05-04 (Wave 17 Bucket D)
**Related:** P5-k12-school.md AC-COMM-003; GAP-063

## Current State (verified 2026-05-04)

No emergency broadcast workflow; no multi-channel parallel send + failover.

## Problem

Hiệu trưởng cần broadcast khẩn (bão / dịch / lockdown) cho 1500+ PH trong ≤5 phút qua SMS+Zalo+email+push parallel. Single-channel + sequential infeasible at scale.

## Proposed Fix

1. **EmergencyBroadcast entity:** with channel-status tracking
2. **Parallel dispatch:** workers per channel (SMS, Zalo, email, push)
3. **Failover:** when 1 channel fails > threshold → escalate via remaining channels
4. **Real-time dashboard:** delivery stats live (1490/1500 SMS success, 1480/1500 Zalo)

## Acceptance Criteria

- [ ] EmergencyBroadcast entity + state machine
- [ ] Multi-channel parallel dispatch ≤5 min for 1500 PH
- [ ] Failover logic tested
- [ ] Real-time dashboard
- [ ] Test: simulate 1500 PH broadcast → ≤5 min completion + dashboard accurate
- [ ] business-logic-review.md 5-attribute

## Related

- **Depends on:** GAP-063 (multi-channel infra), GAP-321 (parent contact data)
- **Wave plan:** Bucket D Stage 3

## Log

- **2026-05-04** — Filed Wave 17 Bucket D.
