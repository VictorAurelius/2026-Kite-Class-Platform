# GAP-899: CHECK constraint coverage không nhất quán cross-table

**Status:** 🔵 OPEN
**Priority:** 🟡 P2
**Domain:** Backend / DB
**Found:** 2026-06-03 (Wave 13 cluster docs writing — KH branding + KH subscription)
**Affects:** Multiple tables — enum/status columns thiếu DB CHECK

## Problem

CHECK enforce enum + app enum giữ trace là policy. Nhưng coverage không nhất quán:

KH branding cluster (cluster 03 §A3):
- `branding_jobs.status` ✅
- `branding_jobs.brand_personality` ✅
- `branding_instance_state.state` ✅
- `branding_lifecycle_events.from_state/to_state/actor_kind` ✅
- `backup_records.status` ❌ KHÔNG
- `branding_regenerate_usage.tier` ❌ KHÔNG
- `branding_regenerate_usage.window_end > window_start` ❌ KHÔNG semantic CHECK

KH subscription cluster (cluster 02 §A2): `pending_tier` (V6) thêm CHECK riêng `chk_subscription_pending_tier` cùng tập với `chk_subscription_tier` — drift risk khi thêm tier mới (vd `EDU`) phải update đồng thời 2 CHECK.

## Proposed Fix

Migration V## add CHECK missing:
- `backup_records.status` (IN_PROGRESS/COMPLETED/FAILED/RESTORED)
- `branding_regenerate_usage.tier` + window order semantic
- Consolidate `chk_subscription_tier`/`chk_subscription_pending_tier` qua Postgres DOMAIN type hoặc keep checklist invariant

## Acceptance Criteria

- [ ] Migration V## add 3 CHECK constraints
- [ ] Subscription tier consolidation decision documented
- [ ] Reference cluster docs KH 02 §A2 + KH 03 §A3

## Discovered in

KH `02-subscription-billing.md` §A2 + KH `03-branding.md` §A3
