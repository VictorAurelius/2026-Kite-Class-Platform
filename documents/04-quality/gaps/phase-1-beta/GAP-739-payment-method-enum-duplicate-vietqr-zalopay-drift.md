# GAP-739: PaymentMethod enum DUPLICATE + 3-way drift VIETQR vs ZALOPAY

**Status:** 🔵 OPEN
**Priority:** 🔴 P0
**Domain:** Backend
**Found:** 2026-05-25 (Wave audit-1 Bucket B Business Logic audit)
**Affects:** Payment flow; consumer integration; FE display logic

## Problem

Per `documents/04-quality/audits/business-logic/2026-05-25-wave-br-4-business-logic-audit.md` §P0-2:

`PaymentMethod` enum được declare ở 2 nơi khác nhau (likely `kiteclass-core` + `kitehub-subscription` hoặc tương đương) → DUPLICATE source of truth. Worse: 2 enum lists DIFFER:
- One has `VIETQR` value
- One has `ZALOPAY` value
- Some have both, some neither

3-way drift: enum values vs `documents/01-business/.../api-contract.md` vs FE TypeScript type union.

## Root Cause

Wave br-4 + earlier waves ship payment code piecemeal without central enum source. Test isolation gap (GAP-735) makes refactor risky.

## Proposed Fix

1. Identify all `PaymentMethod` enum declarations (Java + TypeScript) — `grep -rn "PaymentMethod\|VIETQR\|ZALOPAY" kiteclass kitehub`
2. Establish canonical declaration: 1 Java enum + shared via api-contract.md
3. Refactor duplicates to import canonical
4. Update FE TypeScript union to match
5. Update api-contract.md per affected domain
6. IT test enum coverage (all values handled)

## Acceptance Criteria

- [ ] Single canonical `PaymentMethod` enum (e.g., `kiteclass-core` per BR-PAYMENT-NNN)
- [ ] Duplicate declarations removed/replaced với import
- [ ] FE TypeScript union match canonical
- [ ] api-contract.md updated per affected domain
- [ ] `scripts/check-cross-layer-contract-drift.sh` PASS post-refactor
- [ ] IT test all PaymentMethod values produce valid payment record

## Related

- Audit: `documents/04-quality/audits/business-logic/2026-05-25-wave-br-4-business-logic-audit.md` §P0-2
- Sister gap GAP-738 (3-layer docs cho payment-record domain)
- Wave: planned `wave-beta-readiness-8`

## Log

- **2026-05-25 (created):** Filed per Wave audit-1 Business Logic audit P0-2. Wave beta-readiness-8 scope.
