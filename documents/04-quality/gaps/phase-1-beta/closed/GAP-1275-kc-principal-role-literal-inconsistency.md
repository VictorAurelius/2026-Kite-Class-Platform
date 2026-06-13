# GAP-1275: `PRINCIPAL` `@PreAuthorize` literal (K-12) inconsistent with Phase 1 BETA 5-template RBAC

**Status:** 🟢 DONE
**Priority:** 🟡 P2
**Domain:** Backend
**Found:** 2026-06-14 (Wave rbac-lms-be-foundation — PART 3 `@PreAuthorize` role-literal audit)
**Resolved:** 2026-06-14 (Wave rbac-lms-kc9-staff — decision recorded: keep as Phase 3 K-12 forward-ref)
**Affects:** `kiteclass/kiteclass-core/**` — `PRINCIPAL` `@PreAuthorize` annotations (3 sites confirmed)

## Problem

The `@PreAuthorize` audit found `PRINCIPAL` used in 4 annotations:
- `hasAnyRole('OWNER','ADMIN','PRINCIPAL')` ×2
- `hasAnyRole('ADMIN','PRINCIPAL','OWNER')` ×1
- (one more PRINCIPAL combination)

`PRINCIPAL` is a K-12 role-hierarchy concept (ADR-003 illustrative names TENANT_OWNER/PRINCIPAL/VICE_PRINCIPAL) but is **NOT** one of the Phase 1 BETA 5 RBAC templates (OWNER/STAFF/TEACHER/PARENT/STUDENT), and **NOT** in `AuthorizationBean.isAdmin()` (which recognizes ROLE_PLATFORM_ADMIN / ROLE_ADMIN / ROLE_OWNER). For Phase 1 BETA (non-K-12), `PRINCIPAL` is never granted, so this is additive / harmless (an extra OR-clause that no user matches) — NOT a Wave-78-GAP-518-style breaking mismatch. But it is an inconsistency: the literal set drifts from the canonical 5-template + admin-bypass model.

## Proposed Fix

Phase 1 BETA: either drop `PRINCIPAL` from the annotations (fold into OWNER/ADMIN) OR document it as a deliberate Phase 3 K-12 forward-reference. Decide as part of the K-12 role-hierarchy activation (Phase 3). Low priority — no functional impact in Phase 1.

## Decision (2026-06-14, Wave rbac-lms-kc9-staff)

**KEEP `PRINCIPAL` as an intentional Phase 3 K-12 forward-reference.** Rationale:

- `PRINCIPAL` is a real role in the K-12 hierarchy per **ADR-003** (`Role.java` level 2: `OWNER(10) > PRINCIPAL(2) > VICE_PRINCIPAL(3)` illustrative names). It is NOT noise — it is the school-principal role activated in Phase 3 (K-12 P5 cohort).
- Additive-harmless in Phase 1 BETA: `PRINCIPAL` is never granted (not a seeded `SystemRoleTemplate`, not in `AuthorizationBean.isAdmin()`), so the extra OR-clause matches no user → zero functional impact (NOT a Wave-78-GAP-518-style breaking mismatch).
- The 3 confirmed sites are **already mutually consistent** — each pairs `PRINCIPAL` with `OWNER`+`ADMIN` (owner-level admin surfaces). No drift between them to reconcile.
- Removing it would lose the forward-reference and require re-adding at Phase 3 K-12 activation — net churn for no Phase 1 benefit (per `thesis-as-future-state-mandate.md` spirit: keep the goal-state forward commitment).

**Correction:** the gap title said "4 sites"; empirical grep (`grep -rn PRINCIPAL .../src/main/java`) confirms **3** `@PreAuthorize` sites use `PRINCIPAL`.

### Canonical role-literal list (Phase 1 BETA, kiteclass-core `@PreAuthorize`)

| Literal | Phase 1 granted? | Source |
|---|---|---|
| `OWNER` | ✅ | `SystemRoleTemplate.OWNER` + `isAdmin()` bypass |
| `STAFF` | ✅ (this wave GAP-1274) | `SystemRoleTemplate.STAFF` |
| `TEACHER` | ✅ | `SystemRoleTemplate.TEACHER` |
| `PARENT` | ✅ | `SystemRoleTemplate.PARENT` (reference-id authz) |
| `STUDENT` | ✅ (KC-9 GAP-1277) | `SystemRoleTemplate.STUDENT` |
| `ADMIN` | ✅ | platform/`isAdmin()` (gateway `ROLE_ADMIN`) |
| `PLATFORM_ADMIN` | ✅ | platform/`isAdmin()` |
| `PRINCIPAL` | ❌ Phase 3 K-12 forward-ref | ADR-003 (additive-harmless until K-12 activation) |

### 3 confirmed PRINCIPAL sites (all `OWNER`+`ADMIN`+`PRINCIPAL`, consistent)

- `onboarding/controller/OnboardingController.java:48` — `hasAnyRole('OWNER','ADMIN','PRINCIPAL')`
- `teacher/controller/TeacherController.java:72` — `hasAnyRole('OWNER','ADMIN','PRINCIPAL')`
- `parent/controller/ParentConsentAdminController.java:63` — `hasAnyRole('ADMIN','PRINCIPAL','OWNER')`

## Acceptance Criteria

- [x] Decision recorded: keep PRINCIPAL as Phase 3 K-12 forward-ref (see Decision section above)
- [x] Canonical role-literal list documented (table above; cross-ref PR body audit)

## Related

- Discovered in: branch `wave/rbac-lms-be-foundation` (this PR, PART 3 audit)
- ADR-003 role-hierarchy; GAP-1119 (5-template fixed-curated RBAC)
- Sister finding: GAP-1274 (STAFF missing coverage)
- Precedent: Wave 78 GAP-518 (PLATFORM_ADMIN vs ADMIN drift)
