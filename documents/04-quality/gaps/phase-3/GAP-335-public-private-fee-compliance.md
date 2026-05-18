# GAP-335: Public vs Private School Fee Compliance Enforcement

**Status:** 🔵 OPEN
**Priority:** 🟠 P1 LEGAL
**Domain:** Backend + Compliance
**Detected:** 2026-05-04 (Wave 17 Bucket D)
**Related:** P5-k12-school.md AC-FIN-002

## Current State (verified 2026-05-04)

No fee-cap enforcement; admin can set any fee. Public school MUST comply UBND quy định fee cap; private school flexible.

## Problem

Public school học phí cap by Nghị quyết HĐND tỉnh/thành (vd Hà Nội THCS 0-155k/tháng). Without enforcement: school may charge over cap → consumer protection violation + Phòng GD audit fail.

## Proposed Fix

1. **Tenant.school_type:** PUBLIC | PRIVATE
2. **FeeCapTable:** seeded với UBND quy định per tỉnh / cấp / năm
3. **Validation:** PUBLIC tenants → fee changes validated against cap; over-cap → require Phòng GD approval evidence upload
4. **Audit log** of every fee config change

## Acceptance Criteria

- [ ] FeeCapTable seeded for major provinces
- [ ] Validation enforced for PUBLIC tenants
- [ ] Audit log with evidence requirement when over-cap
- [ ] Private school: no cap, flexible pricing + tier discounts
- [ ] Test: public tenant set HP 200k > cap 155k → reject; private tenant set HP 5tr → allow
- [ ] business-logic-review.md 5-attribute (Source: Nghị quyết HĐND tỉnh + Consumer Protection 2023; Compliance: Compliant)

## Related

- **Depends on:** GAP-334 (multi-fee structure)
- **Wave plan:** Bucket D Stage 4

## Log

- **2026-05-04** — Filed Wave 17 Bucket D.
