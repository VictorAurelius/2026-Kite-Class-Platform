# GAP-902: `admin_audit_log` (V36) vs `admin_audit_logs` (V50) — 2 bảng tên gần giống

**Status:** 🔵 OPEN
**Priority:** 🟡 P2
**Domain:** Backend / DB / Compliance
**Found:** 2026-06-03 (Wave 13 cluster docs writing — KH email/compliance/admin)
**Affects:** `kitehub` tables `admin_audit_log` + `admin_audit_logs`

## Problem

2 bảng tên gần giống mục đích khác:

| | `admin_audit_log` (V36) | `admin_audit_logs` (V50) |
|---|---|---|
| Dùng | Generic @Auditable aspect (kh-sub local) | PDPL Art 11 federated immutable |
| PK | BIGSERIAL | UUID |
| Mutability | App-layer | RLS FORCE + UPDATE/DELETE blocked |
| RLS | ❌ | ✅ FORCE |
| Timestamps | TIMESTAMPTZ | TIMESTAMP (drift) |

Cả hai canonical nhưng dễ nhầm — query "tất cả admin actions" phải union cả 2 + kc-core admin_audit_logs.

## Proposed Fix

Document trong architecture doc + business doc compliance. Cân nhắc rename `admin_audit_log` (V36) → `admin_action_log` để giảm collision. Federated query helper utility.

## Acceptance Criteria

- [ ] Architecture doc explain 2 audit log layers
- [ ] Rename decision (rename vs document only)
- [ ] Federated query helper utility
- [ ] Reference cluster doc KH 04-email-compliance-admin §A1

## Discovered in

`documents/02-architecture/database/kitehub/04-email-compliance-admin.md` §A1
