# GAP-824 — `BetaAccessService.exchangeClaimCode` collapses lifecycle states vào `CODE_NOT_FOUND` (cluster sister của GAP-610)

**Status:** 🔵 OPEN
**Priority:** 🟠 P1
**Domain:** Backend
**Found:** 2026-06-01 (Wave onboarding-polish-2 Bucket E investigation IT)
**Affects:** Beta signup claim-code (GAP-388 388-B 2FA) — invitee submits 6-digit code → operator cannot distinguish "code never issued" vs "code in wrong lifecycle state"

## Problem

`BetaAccessService.exchangeClaimCode(String claimCode)` (lines 364-382) collapses 2 semantically distinct failure modes vào SAME response code (`CODE_NOT_FOUND`):

```java
public BetaClaimCodeExchangeResponse exchangeClaimCode(String claimCode) {
    Optional<BetaAccessRequest> opt = repository.findByClaimCode(claimCode);
    if (opt.isEmpty()) {
        return BetaClaimCodeExchangeResponse.invalid("CODE_NOT_FOUND");  // (A) code truly missing
    }
    BetaAccessRequest entity = opt.get();
    if (entity.getStatus() == BetaAccessRequestStatus.SIGNED_UP) {
        return BetaClaimCodeExchangeResponse.invalid("ALREADY_USED");
    }
    if (entity.getStatus() != BetaAccessRequestStatus.APPROVED) {
        return BetaClaimCodeExchangeResponse.invalid("CODE_NOT_FOUND");  // (B) row exists but PENDING/REJECTED/ABORTED
    }
    if (entity.isTokenExpired()) {
        return BetaClaimCodeExchangeResponse.invalid("CODE_EXPIRED");
    }
    return BetaClaimCodeExchangeResponse.ok(...);
}
```

**Anti-pattern identical to GAP-610** (sister `validateToken` method same service class) — invitee receives same `CODE_NOT_FOUND` regardless of whether claim code was never issued OR row exists in pre-APPROVAL / REJECTED / ABORTED state.

## Root Cause

Same diagnostic-clarity gap as GAP-610. Cluster confirmed via Wave onboarding-polish-2 Bucket E `BetaSignupTokenReproIT` investigation:

- H4 (lifecycle-collapse) anti-pattern verified at `validateToken` (lines 540-551)
- Sweep cùng class `BetaAccessService` surfaces `exchangeClaimCode` (lines 367+374) với same shape
- Per `cross-flow-bug-class-sweep.md` §1 — "Sau khi fix bug trong 1 flow, MUST grep + audit other flows cho same bug class signature"

Bug class signature: `service.invalid("X_NOT_FOUND")` returned for BOTH `Optional.isEmpty()` path AND `entity.getStatus() != APPROVED` path. Conflates "row missing" vs "row wrong state".

## Acceptance Criteria

- [ ] Split `CODE_NOT_FOUND` (line 374) → new code `CODE_NOT_APPROVED` HOẶC reuse separate semantic
- [ ] Update `BetaClaimCodeExchangeResponse.errorCode` allowed values + FE `BetaClaimCodeForm.tsx` UI message handling
- [ ] Cluster fix shipped same PR as GAP-610 fix wave (sister gap)
- [ ] Integration test cover: PENDING claim code → returns `CODE_NOT_APPROVED` NOT `CODE_NOT_FOUND`
- [ ] VN-localization: error messages comply `vn-localization-audit-checklist.md` §2 (Vietnamese label)

## Proposed Fix

Same fix wave as GAP-610. Batch both `validateToken` + `exchangeClaimCode` lifecycle-collapse fixes in one PR — same domain, same code class, same FE downstream surface.

**Effort:** ~15 min additional on top of GAP-610 fix (already grokking the service class).

## Related

- **GAP-610** — sister cluster (validateToken collapse); root cause investigation @ Wave onboarding-polish-2 Bucket E
- **`cross-flow-bug-class-sweep.md`** v1.0.1 §1 — sweep rule that surfaced this cluster
- **`BetaAccessService.exchangeClaimCode`** — lines 364-382
- **`BetaClaimCodeExchangeResponse`** — DTO error code enum
- **GAP-388** — 388-B 2FA claim code feature (parent feature)

## Log

- **2026-06-01:** Gap filed during Wave onboarding-polish-2 Bucket E investigation. IT `BetaSignupTokenReproIT` confirmed H4 lifecycle-collapse in `validateToken`; cross-flow sweep per `cross-flow-bug-class-sweep.md` §1 identified identical pattern in `exchangeClaimCode`. Same class, same domain → file as cluster sister. Fix wave should batch both methods in single PR (~15min additional work on top of GAP-610 fix scope).
