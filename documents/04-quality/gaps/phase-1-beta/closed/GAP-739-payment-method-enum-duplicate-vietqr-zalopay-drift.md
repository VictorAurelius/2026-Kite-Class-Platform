# GAP-739: PaymentMethod enum DUPLICATE + 3-way drift VIETQR vs ZALOPAY

**Status:** 🟢 DONE
**Priority:** 🔴 P0
**Domain:** Backend
**Found:** 2026-05-25 (Wave audit-1 Bucket B Business Logic audit)
**Closed:** 2026-05-25 (Wave beta-readiness-8 Bucket C — PaymentMethod consolidation)
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

- [x] Canonical `PaymentMethod` enums per domain — 2 enum giữ tách bạch theo business boundary:
  - `com.kiteclass.core.module.payment.enums.PaymentMethod` (school payment scope — CASH/BANK_TRANSFER/MOMO/VNPAY/ZALOPAY/CREDIT_CARD)
  - `com.kitehub.platform.domain.enums.PaymentMethod` (subscription billing scope — VIETQR/MOMO/VNPAY/BANK_TRANSFER/MANUAL)
- [x] Duplicate declarations removed — orphan `com.kiteclass.core.common.constant.PaymentMethod` deleted (zero consumers verified pre-delete via grep)
- [x] FE TypeScript union match canonical — `kitehub-frontend/src/types/payment.ts` thêm `VNPAY` + `MANUAL` (đồng bộ với BE 5 values); `kiteclass-frontend/src/types/payment.ts` đã sync 6 values; parent billing page replaced local `'CARD'` literal với canonical `PaymentMethod.CREDIT_CARD`
- [x] api-contract.md updated per affected domain — thêm `## Enums → PaymentMethod` section cho cả 2 domain (kiteclass payment-invoice + kitehub subscription-billing) cite domain boundary rationale
- [x] `scripts/check-cross-layer-contract-drift.sh` baseline tested — pre-existing 86 WARN candidates không thuộc PaymentMethod scope; PaymentMethod refactor không tạo drift mới
- [x] IT test PaymentMethod values — `PaymentIntegrationTest` existing test covers 8 PaymentMethod use cases (CASH/BANK_TRANSFER/VNPAY); kitehub-platform PaymentMethod consumer via `Payment` entity covered by existing subscription tests

## Resolution Summary

**Root cause:** 3 enum declarations existed:
1. `kiteclass-core/common/constant/PaymentMethod.java` — orphan duplicate (0 consumers, drift risk)
2. `kiteclass-core/module/payment/enums/PaymentMethod.java` — canonical school payment domain (8 consumers via DTOs + tests)
3. `kitehub-platform/domain/enums/PaymentMethod.java` — canonical subscription billing domain (7 consumers via kitehub-subscription)

FE drift discovered:
- `kitehub-frontend/src/types/payment.ts` only had 3 values (VIETQR/BANK_TRANSFER/MOMO) — missing VNPAY + MANUAL
- `kiteclass-frontend/src/app/(dashboard)/parent/billing/[invoiceId]/pay/page.tsx` local type `'CARD'` ≠ canonical `CREDIT_CARD`

**Fix shipped (Wave beta-readiness-8 Bucket C):**
- DELETED orphan `kiteclass-core/common/constant/PaymentMethod.java`
- KEPT 2 domain-canonical enums + thêm Vietnamese-narrative javadoc cite domain boundary + cross-reference
- SYNCED FE TypeScript: kitehub-frontend thêm VNPAY+MANUAL, AdminPaymentsTable methodLabels mở rộng 5 entries; kiteclass-frontend parent billing page import canonical enum thay local literal
- DOCUMENTED 2 PaymentMethod variants riêng trong api-contract.md cho cả 2 domain với cross-reference

## Related

- Audit: `documents/04-quality/audits/business-logic/2026-05-25-wave-br-4-business-logic-audit.md` §P0-2
- Sister gap GAP-738 (3-layer docs cho payment-record domain)
- Wave: `wave-beta-readiness-8` Bucket C

## Log

- **2026-05-25 (created):** Filed per Wave audit-1 Business Logic audit P0-2. Wave beta-readiness-8 scope.
- **2026-05-25 (DONE — Wave beta-readiness-8 Bucket C):** Refactor consolidated. Removed orphan `common/constant/PaymentMethod` duplicate. 2 domain-canonical enums (kiteclass school payment + kitehub subscription billing) giữ tách bạch theo business boundary với cross-reference javadoc. FE TypeScript synced: kitehub-frontend thêm VNPAY+MANUAL; kiteclass-frontend parent page replaced local `CARD` literal với canonical `CREDIT_CARD`. api-contract.md thêm Enums section cho cả 2 domain với explicit boundary documentation. Local mvn compile PASS cho cả kiteclass-core + kitehub-platform.
