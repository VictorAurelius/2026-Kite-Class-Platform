# GAP-817: Wave tenant-domain-1 post-wave audit suite (≤3 days mandate)

**Status:** 🔵 OPEN
**Priority:** 🟡 P2
**Domain:** Meta
**Found:** 2026-06-01 (Wave tenant-domain-1 closure)
**Affects:** Wave tenant-domain-1 audit trail compliance per `post-wave-audit-mandate.md` §2.2

## Problem

Wave tenant-domain-1 (5/5 buckets merged 2026-06-01) shipped tenant→domain→landing cluster fix. Per `.claude/rules/post-wave-audit-mandate.md` §2.2, mọi wave merge PHẢI có audit suite chạy ≤3 ngày sau closure (business-logic + api-contract + ops-readiness baseline). Audit chưa run khi wave closure PR ship → mandate compliance pending until 2026-06-04.

## Proposed Fix

Run 3 audit skills against Wave tenant-domain-1 scope:
1. `business-logic-audit /100` — verify tenant/domain/landing 3-layer doc ↔ code sync (kitehub-platform DomainController/TenantController/PublicTenantController + use-cases.md + api-contract.md)
2. `api-contract-audit /100` — verify new endpoints (POST /api/v1/public/tenants/resolve + DomainService endpoints) match api-contract.md với 11 standard error handlers
3. `ops-readiness-audit /100` — verify gateway TenantHeaderGuardFilter + ACM cert provisioning runbook deltas

## Acceptance Criteria

- [ ] business-logic audit shipped → `audits-index.csv` row added
- [ ] api-contract audit shipped → `audits-index.csv` row
- [ ] ops-readiness audit shipped → `audits-index.csv` row
- [ ] Any P0/P1 finding files spawn child gap per `audit-to-gap-pipeline.md`
- [ ] Deadline 2026-06-04 honored

## Related

- Wave plan: `documents/03-planning/waves/wave-tenant-domain-1.md`
- Rule: `.claude/rules/post-wave-audit-mandate.md` §2.2
- Sister gaps: GAP-811/812/813/814/816
