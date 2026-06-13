# GAP-1275: `PRINCIPAL` `@PreAuthorize` literal (K-12) inconsistent with Phase 1 BETA 5-template RBAC

**Status:** 🔵 OPEN
**Priority:** 🟡 P2
**Domain:** Backend
**Found:** 2026-06-14 (Wave rbac-lms-be-foundation — PART 3 `@PreAuthorize` role-literal audit)
**Affects:** `kiteclass/kiteclass-core/**` — 4 `@PreAuthorize` annotations using `PRINCIPAL`

## Problem

The `@PreAuthorize` audit found `PRINCIPAL` used in 4 annotations:
- `hasAnyRole('OWNER','ADMIN','PRINCIPAL')` ×2
- `hasAnyRole('ADMIN','PRINCIPAL','OWNER')` ×1
- (one more PRINCIPAL combination)

`PRINCIPAL` is a K-12 role-hierarchy concept (ADR-003 illustrative names TENANT_OWNER/PRINCIPAL/VICE_PRINCIPAL) but is **NOT** one of the Phase 1 BETA 5 RBAC templates (OWNER/STAFF/TEACHER/PARENT/STUDENT), and **NOT** in `AuthorizationBean.isAdmin()` (which recognizes ROLE_PLATFORM_ADMIN / ROLE_ADMIN / ROLE_OWNER). For Phase 1 BETA (non-K-12), `PRINCIPAL` is never granted, so this is additive / harmless (an extra OR-clause that no user matches) — NOT a Wave-78-GAP-518-style breaking mismatch. But it is an inconsistency: the literal set drifts from the canonical 5-template + admin-bypass model.

## Proposed Fix

Phase 1 BETA: either drop `PRINCIPAL` from the 4 annotations (fold into OWNER/ADMIN) OR document it as a deliberate Phase 3 K-12 forward-reference. Decide as part of the K-12 role-hierarchy activation (Phase 3). Low priority — no functional impact in Phase 1.

## Acceptance Criteria

- [ ] Decision recorded: keep PRINCIPAL as Phase 3 forward-ref OR remove from Phase 1 annotations
- [ ] Canonical role-literal list documented (cross-ref this wave's PR body audit)

## Related

- Discovered in: branch `wave/rbac-lms-be-foundation` (this PR, PART 3 audit)
- ADR-003 role-hierarchy; GAP-1119 (5-template fixed-curated RBAC)
- Sister finding: GAP-1274 (STAFF missing coverage)
- Precedent: Wave 78 GAP-518 (PLATFORM_ADMIN vs ADMIN drift)
