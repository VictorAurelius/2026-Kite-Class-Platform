# GAP-897: `system_config` index `idx_system_config_key` redundant với PRIMARY KEY

**Status:** 🔵 OPEN
**Priority:** 🟢 P3
**Domain:** DB
**Found:** 2026-06-03 (Wave 13 cluster docs writing — KH subscription/billing)
**Affects:** `kitehub-subscription` `system_config` table

## Problem

`CREATE INDEX IF NOT EXISTS idx_system_config_key ON system_config(config_key)` (V27) — `config_key` đã là PRIMARY KEY (Postgres tự tạo unique B-tree index trên PK). Tạo thêm `idx_system_config_key` cùng cột = duplicate index. Tốn disk + slow INSERT/UPDATE.

Minor anomaly không ảnh hưởng correctness.

## Proposed Fix

Cleanup migration `DROP INDEX IF EXISTS idx_system_config_key`.

## Acceptance Criteria

- [ ] Migration V## drop redundant index
- [ ] Reference cluster doc KH 02-subscription-billing §A4

## Discovered in

`documents/02-architecture/database/kitehub/02-subscription-billing.md` §A4
