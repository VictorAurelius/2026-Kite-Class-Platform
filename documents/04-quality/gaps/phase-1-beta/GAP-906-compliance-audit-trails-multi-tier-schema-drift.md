# GAP-906: 5 audit trails compliance schema khác nhau — integrity model bất nhất

**Status:** 🔵 OPEN
**Priority:** 🟠 P1
**Domain:** Backend / DB / Compliance
**Found:** 2026-06-03 (Wave 13 cluster docs writing — KC compliance)
**Affects:** `kiteclass-core` 5 audit tables compliance cluster

## Problem

5 audit trails mục đích chồng chéo, schema khác nhau, integrity model khác nhau:

| Bảng | V# | Integrity | DELETE block |
|---|---|---|---|
| `audit_log` | V35 | App-layer | ❌ |
| `parent_read_audit_log` | V53 | App-layer | ❌ |
| `child_protection_audit_log` | V54 | SHA-256 chain + REVOKE DELETE | ✅ |
| `admin_audit_logs` | V60 | RLS UPDATE/DELETE = false | ✅ |
| `quality_reports` | V39 | App-layer | ❌ |

Compliance reviewer phải biết bảng nào dùng integrity model nào. Bất nhất phản ánh bar tăng dần V35 < V53 < V54 < V60.

## Proposed Fix

Architecture doc document audit trail layer matrix. Cân nhắc upgrade `audit_log` + `parent_read_audit_log` lên hash-chain hoặc RLS UPDATE/DELETE block per `feature-ship-runtime-walk-mandate.md` compliance bar. Tracked separately via GAP-889 (audit_log) — gap này cover cross-table architecture.

## Acceptance Criteria

- [ ] Architecture doc audit-trail-layers.md
- [ ] Migration roadmap upgrade audit_log + parent_read_audit_log (linked GAP-889)
- [ ] Reference cluster doc 07-compliance-audit §A1

## Discovered in

`documents/02-architecture/database/kiteclass/07-compliance-audit.md` §A1
