# GAP-319: WORM Audit Log for 10-Year Tax Compliance

**Status:** 🔵 OPEN
**Priority:** 🔴 P0 — legal compliance (Luật Quản lý Thuế 2019 + ND-13/2023/NĐ-CP)
**Domain:** Backend (kiteclass-core retention + new audit-log module) + Storage
**Found:** 2026-05-04 (Persona Review Round 1 — P3 Bucket C)
**Affects:** 3 ACs across tenant + admin

---

## Problem

P3 phải retain financial records 10 năm (Luật Quản lý Thuế 2019 + ND-13/2023/NĐ-CP). Records phải:
1. Immutable (write-once read-many — WORM) — không deletable/editable post-creation
2. Queryable for 10 năm (e.g. Tax authority audit 2017-2026)
3. Old records archived to cold storage but retrievable trong ≤24h
4. Hash integrity verifiable

Without WORM, records mutable → Tax authority reject → fine + license revoke risk.

## Root Cause

`module/legal` + `module/retention` exists nhưng:
- Không có WORM-immutable storage layer
- Không có 10-year retention policy enforcement
- Không có cold storage tiering (hot/warm/cold per `logs-format-standard.md` retention tiers)
- Không có hash integrity verification

## Current State (verified 2026-05-04)

| Component | Path | State |
|-----------|------|-------|
| Legal module | `kiteclass-core/.../module/legal` | ✅ scaffold |
| Retention module | `kiteclass-core/.../module/retention` | ✅ scaffold |
| WORM storage adapter | — | ❌ missing |
| 10-year retention policy enforcement | — | ❌ missing |
| Cold storage archival | — | ❌ missing |
| Hash integrity verification | — | ❌ missing |
| Tax authority export package generator | — | ❌ missing |

## Proposed Fix

**Phase 1 — WORM storage adapter:**
1. `WormStorageService` interface; implementations: PostgreSQL append-only table + S3 Object Lock cold tier
2. Append-only constraint (DB trigger blocking UPDATE/DELETE)
3. SHA-256 hash per row; chain hash for tamper detection

**Phase 2 — Tier-based retention:**
1. Hot (0-30d) — primary DB
2. Warm (31d-2yr) — read-only DB partition
3. Cold (2yr-10yr) — S3 Object Lock with retrieval ≤24h

**Phase 3 — Tax export tooling:**
1. Tax authority export wizard: trigger → encrypted ZIP package + hash manifest
2. Audit log tracks who triggered + downloaded
3. Bàn giao via portal TCT (manual or API)

## Acceptance Criteria

- [ ] WormStorageService blocks UPDATE/DELETE on financial records
- [ ] Hash chain validated; tamper detection test passes
- [ ] Cold storage archival cron job ships records >2yr
- [ ] Retrieval from cold ≤24h (test with sample query)
- [ ] Tax export package generates encrypted ZIP + hash manifest
- [ ] Audit log captures export with who/when/hash
- [ ] Performance: 10-year query returns within ≤30s for sample tenant
- [ ] Business rules in `documents/01-business/kiteclass/audit-retention/rules.md` per 5-attribute standard

## Linked ACs

| AC ID | Persona | Doc |
|-------|---------|-----|
| AC-EDGE-004 | Tenant Director | `P3-medium-center.md` |
| AC-EDGE-003 | Admin (kế toán) | `secondary/admin-in-P3.md` |
| AC-ONBOARD-002 (audit log on 403) | Admin | `secondary/admin-in-P3.md` (linked GAP-308) |

## Related

- Existing: GAP-184 (data retention — extends with WORM tier)
- Compliance: Luật Quản lý Thuế 2019 (10-năm), ND-13/2023/NĐ-CP, Luật Kế toán 2015
- Persona review: §2 (Tenant AC-EDGE-004), §4 (Admin AC-EDGE-003)
- Logs format: `.claude/rules/logs-format-standard.md` §4 retention tiers

## Log

- **2026-05-04** Created from Persona Review Round 1 P3 Bucket C.
