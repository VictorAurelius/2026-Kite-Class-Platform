# GAP-901: KH Branding cluster FK coverage mỏng — 1/9 bảng có FK thật

**Status:** 🔵 OPEN
**Priority:** 🟢 P3
**Domain:** Backend / DB
**Found:** 2026-06-03 (Wave 13 cluster docs writing — KH branding)
**Affects:** `kitehub-branding` 9 bảng cluster

## Problem

Chỉ 1/9 bảng có FK thật tới `instances(id)`:
- ✅ `branding_jobs` (FK CASCADE V4) — duy nhất
- ❌ logic `ai_usage_log`, `branding_regenerate_usage`, `branding_instance_state`, `branding_lifecycle_events`, `backup_records`, `branding_outbox`, `subscription_outbox`

Lý do hợp lý cho audit/outbox (survive delete forensic), nhưng `branding_instance_state` + `branding_regenerate_usage` + `ai_usage_log` có thể safely add FK CASCADE (như `branding_jobs`) mà không mất tính năng.

## Proposed Fix

Migration V## add FK CASCADE cho 3 bảng counter/state. Audit + outbox giữ logic ref (forensic survive purge).

## Acceptance Criteria

- [ ] Migration V## add 3 FK CASCADE
- [ ] Document design intent: audit logical ref vs counter FK
- [ ] Reference cluster doc KH 03-branding §A1

## Discovered in

`documents/02-architecture/database/kitehub/03-branding.md` §A1
