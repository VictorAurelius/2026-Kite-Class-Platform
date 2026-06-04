# GAP-957: Slug conflict không có user-facing recovery path — orphan owner

**Status:** 🔵 OPEN
**Priority:** 🟠 P1
**Domain:** Backend
**Found:** 2026-06-04 (Wave flow-kh3 KC-1 pre-walk audit — 3-agent outside-in consensus)
**Affects:** KC-1 (Tenant provisioning recovery) — user trust + data integrity
**Defer-to:** After Wave flow-kh3 finish

## Problem

UC-PROV-02 "Initiate Fails (Slug Conflict)" → saga rethrow → "Caller sees 400 from API layer". NHƯNG caller là KH-2b auto-trigger (no human UI). Email + DB row đã commit ở KH-1 chain. Now KC provisioning fails → Owner đã có tài khoản nhưng KHÔNG có tenant. Owner Tuấn login `kc-an-8.kitehub.me` → 404 (slug đã bị Linh xài 2 phút trước) → không có "slug conflict, vui lòng chọn slug khác" recovery flow → support ticket. BR-PROV-004 "initiate failure does NOT trigger compensation" → orphan owner. Surfaced: persona Finding 1.5.

## Proposed Fix

Wire slug-conflict detect → trigger compensation rollback owner row OR redirect Owner to "chọn slug khác" page với suggested alternatives (e.g., `an-tutoring-1`, `an-tutoring-2`). Loop until unique slug accepted hoặc Owner abort. Plus DB unique constraint enforcement guard.

## Acceptance Criteria

- [ ] Slug conflict → FE redirect "Chọn slug khác" page với 3 suggested alternatives
- [ ] Owner row compensated nếu Owner abort
- [ ] Walk: 2 concurrent same-slug signup → 1 succeeds, 1 receives friendly conflict page

## Related

- Discovered in: 3-agent outside-in audit 2026-06-04
- Audit artifact: persona-review/2026-06-04-pre-walk-kc1-tenant-provisioning.md Finding 1.5
- Sister: GAP-535 (slug normalize DONE), matrix A1×E1×EC1 (race) + EC4 (idempotency)
- Flow Verification Campaign §4 row KC-1
