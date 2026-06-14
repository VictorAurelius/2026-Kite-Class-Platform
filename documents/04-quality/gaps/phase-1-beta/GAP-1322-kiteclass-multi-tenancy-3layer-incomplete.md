# GAP-1322: kiteclass/multi-tenancy domain missing use-cases.md + api-contract.md (3-layer incomplete)

**Status:** 🔵 OPEN
**Priority:** 🟡 P2
**Domain:** Meta (business docs 3-layer completeness — KiteClass)
**Found:** 2026-06-14 (Business Logic full audit post wave-p0-closeout-1)
**Affects:** `documents/01-business/kiteclass/multi-tenancy/`

## Problem

`ls documents/01-business/kiteclass/multi-tenancy/` → CHỈ `rules.md` (thiếu `use-cases.md` + `api-contract.md`).

Multi-tenancy là cross-cutting domain load-bearing — tenant isolation enforce qua RLS + `instance_id` scoping (cited bởi BR-STU-006 student-enrollment / BR-ATT-009 attendance / BR-AUTH-007 tenant-auth). Thiếu Layer-2 (use-cases) + Layer-3 (api-contract) → verification chain `BR-xxx → UC-xxx → endpoint → @Mapping → @Test` không trace được cho domain nền tảng nhất.

Đây là cùng class với GAP-664 (3-layer drift) nhưng GAP-664 scope = **kitehub** preferences/email; gap này = **kiteclass** multi-tenancy (đường nhánh riêng, không duplicate). CLAUDE.md §"3-Layer Structure" mandate 3 file/domain; pre-commit hook warn nếu thiếu.

## Root Cause

multi-tenancy rules.md được tạo sớm (Wave 1-30 foundation) trước khi 3-layer mandate enforce; use-cases + api-contract không backfill khi mandate landed (giống GAP-664 pattern).

## Proposed Fix

Tạo 2 layer còn thiếu cho `kiteclass/multi-tenancy/`:
- `use-cases.md` — UC tenant resolution (subdomain → instance_id), RLS GUC set per request, cross-tenant access denial.
- `api-contract.md` — tenant header contract (`X-Tenant-Id` resolution qua gateway TenantResolver GAP-711) + RLS-scoped query behavior.

Cross-ref existing architecture: `ADR-040` (cross-product SSO), `multi-tenant-architecture.md`, GAP-711 (TenantResolver).

## Acceptance Criteria

- [ ] `documents/01-business/kiteclass/multi-tenancy/use-cases.md` tồn tại (UC actor/steps/errors).
- [ ] `documents/01-business/kiteclass/multi-tenancy/api-contract.md` tồn tại (tenant header + RLS contract).
- [ ] `bash scripts/verify-business-docs.sh` không còn warn multi-tenancy thiếu layer.

## Related

- **Parent audit:** `documents/04-quality/audits/business-logic/2026-06-14-business-logic-full-audit.md` (Finding 3)
- **Sibling class:** GAP-664 (kitehub preferences/email 3-layer drift)
- **Rule:** CLAUDE.md §"Business Logic Documents 3-Layer Structure"
