# GAP-961: Subdomain concurrent race + registerFromBetaInvite no idempotency

**Status:** 🔵 OPEN
**Priority:** 🟠 P1
**Domain:** Backend
**Found:** 2026-06-04 (Wave flow-kh3 KC-1 pre-walk audit — 3-agent outside-in consensus)
**Affects:** KC-1 (Tenant provisioning concurrency) — race + idempotency
**Defer-to:** After Wave flow-kh3 finish

## Problem

(1) `AuthService.registerFromBetaInvite:230` check `existsBySubdomainAndDeletedFalse(subdomain)` rồi save. KHÔNG có `SELECT FOR UPDATE` / DB unique constraint enforcement guarantee. 2 concurrent calls với same subdomain → both pass check → 2nd save throws unique-constraint violation 500 (should map to 409). (2) Beta signup POST không idempotent (no `Idempotency-Key` header check). User double-clicks submit → 2 attempts; 1st succeeds (tenant created), 2nd fails on `existsByEmail`/`existsBySubdomain` → user confused. GAP-536 history shows POST `/tenants` had idempotency fix; beta-signup variant likely lacks same protection. Surfaced: matrix A1×E1×EC1 + A1×E1×EC4.

## Proposed Fix

Add `Idempotency-Key` header check trong `AuthService.registerFromBetaInvite`. Map `DataIntegrityViolationException` (subdomain unique constraint) → 409 Conflict with friendly message + 3 slug suggestions. Verify DB unique constraint exists on `instances.subdomain` (lowercase).

## Acceptance Criteria

- [ ] `grep -n "Idempotency\|@IdempotencyKey" kitehub/kitehub-subscription/.../service/AuthService.java` ≥1 hit
- [ ] 2 concurrent same-subdomain POST → 1 success (201), 1 conflict (409, NOT 500)
- [ ] Double-click submit → 1 tenant created (idempotency key matches)

## Related

- Discovered in: 3-agent outside-in audit 2026-06-04
- Audit artifact: persona-review/2026-06-04-pre-walk-kc1-failure-mode-matrix.md A1×E1×EC1 + EC4
- Sister: GAP-536 (POST /tenants idempotency — extend coverage), GAP-535 (slug normalize)
- Flow Verification Campaign §4 row KC-1
