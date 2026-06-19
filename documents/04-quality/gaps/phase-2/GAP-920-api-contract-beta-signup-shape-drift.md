# GAP-920: api-contract.md beta-signup body drift — docs `{token, password, acceptTos}` vs code `{token, ownerPassword, subdomain}`

**Status:** 🔵 OPEN
**Priority:** 🟡 P2
**Domain:** Backend (API contract drift)
**Found:** 2026-06-04 (Wave flow-kh1 walk S5)
**Affects:**
- `documents/01-business/kitehub/beta-access/api-contract.md` line ~140 (POST /api/v1/auth/beta-signup request body schema)
- `kitehub/kitehub-subscription/src/main/java/com/kitehub/subscription/beta/dto/BetaSignupCommand.java`

## Problem

api-contract.md spec POST /api/v1/auth/beta-signup request body:
```json
{
  "token": "uuid-v4",
  "password": "...",
  "acceptTos": true
}
```

Actual code `BetaSignupCommand` requires:
```java
public record BetaSignupCommand(
    @NotNull UUID token,
    @NotBlank @Size(min = 8, max = 200) String ownerPassword,
    @NotBlank @Size(max = 100) String subdomain
) {}
```

Discrepancies:
- `password` (docs) vs **`ownerPassword`** (code) — field rename
- `acceptTos: true` (docs) — **không tồn tại trong code** (PDPL consent có ở S1 request-beta-access, không repeat ở S5; OR removed mid-development)
- **`subdomain`** field MANDATORY trong code — không có trong docs (tenant subdomain prefill từ orgName? OR user chooses?)

Impact:
- FE form likely already handles correct shape (since production has been working for existing tenants); docs lag behind code
- Cross-layer contract drift per `contract-first-for-cross-layer.md` v1.0.3 — docs and code MUST stay synced

Severity P2 — không phải bug, drift docs vs code; FE clients hard-code correct shape; agent walks bị blocked nhưng có code as source-of-truth.

## Proposed Fix

1. Update `api-contract.md` POST /api/v1/auth/beta-signup body schema:
   - `password` → `ownerPassword`
   - Remove `acceptTos` (PDPL consent ở S1 only)
   - Add `subdomain` mandatory (kebab-case tenant subdomain)
2. Run `scripts/check-cross-layer-contract-drift.sh` để verify other drift in same area
3. Cross-flow sweep per `cross-flow-bug-class-sweep.md`: check other beta-access endpoints (exchange-claim-code, validate, request-beta-access) for similar docs vs code drift

## Acceptance Criteria

- [ ] api-contract.md beta-signup request body schema matches BetaSignupCommand fields exactly
- [ ] FE TypeScript types match canonical schema (verify `kitehub-frontend/src/types/beta-access.ts` if exists)
- [ ] Cross-layer drift detector (`check-cross-layer-contract-drift.sh`) passes
- [ ] Walk regression: agent walk Wave flow-kh1 S5 succeeds first-try với updated docs

## Related

- Discovered in: Wave flow-kh1 walk (`documents/03-planning/waves/wave-2026-06-04-flow-kh1-beta-funnel.md`) S5
- Sister rule: `contract-first-for-cross-layer.md` Cross-layer contract drift
- Sister gap: GAP-919 (KH-2 register FE gate — different layer)
