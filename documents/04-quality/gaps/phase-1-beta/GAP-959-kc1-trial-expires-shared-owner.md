# GAP-959: Trial expiry shared per owner — tenant 2 chỉ còn 3 ngày khi vừa tạo

**Status:** 🔵 OPEN
**Priority:** 🟠 P1
**Domain:** Backend
**Found:** 2026-06-04 (Wave flow-kh3 KC-1 pre-walk audit — 3-agent outside-in consensus)
**Affects:** KC-1 (Trial lifecycle) — business-rule clarity + user trust
**Defer-to:** After Wave flow-kh3 finish

## Problem

Trial subscription thường tied to Owner-level (not Instance-level) per `kitehub/trial-lifecycle/rules.md`. Owner có 2 tenants share 1 trial expiry → tenant 2 trial chỉ còn 3 ngày khi vừa tạo. Linh tạo tenant B day 11/14 trial → tenant B chỉ còn 3 ngày → cảm giác bị "cheated". Plus `InstanceService:131` rejects 2nd trial per owner: `existsByOwnerIdAndTrialStartedAtIsNotNull(ownerId)` → throws `IllegalArgumentException` → conflicts với "Owner owns N tenants" claim in matrix Axis 2 E3. Surfaced: persona Finding 2.4 + matrix A8×E3×EC4.

## Proposed Fix

Decision (business): trial scope = per-instance (each tenant gets own 14-day trial) OR per-owner (shared). Update `trial-lifecycle/rules.md` + schema accordingly. If per-instance: drop `existsByOwnerIdAndTrialStartedAtIsNotNull` check; if per-owner: communicate clearly trong signup UI + tenant create UI ("Trial expires DD/MM/YYYY for all your tenants").

## Acceptance Criteria

- [ ] `trial-lifecycle/rules.md` declares per-instance OR per-owner explicitly với rationale
- [ ] Schema reflects decision (`subscriptions` table scope + `instances.trial_expires_at` if per-instance)
- [ ] FE clearly displays trial expiry policy at tenant create page

## Related

- Discovered in: 3-agent outside-in audit 2026-06-04
- Audit artifact: persona-review/2026-06-04-pre-walk-kc1-{tenant-provisioning,failure-mode-matrix}.md Finding 2.4 + A8×E3×EC4
- Sister: GAP-532 (multi-tenant tenant-switch flow) — same root question Owner-N-tenants
- Flow Verification Campaign §4 row KC-1
