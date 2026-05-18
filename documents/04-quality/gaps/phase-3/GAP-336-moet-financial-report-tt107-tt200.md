# GAP-336: MOET Financial Report TT 107/2017 + TT 200/2014 with E-Signature

**Status:** 🔵 OPEN
**Priority:** 🟠 P1
**Domain:** Backend + Compliance
**Detected:** 2026-05-04 (Wave 17 Bucket D)
**Related:** P5-k12-school.md AC-FIN-004

## Current State (verified 2026-05-04)

```bash
grep -rl "TT.107\|TT.200\|moet.financial" kitehub/ --include="*.java"
```
Result: zero. No MOET financial report template; no e-signature integration.

## Problem

Quarterly + annual reports phải đúng format Bộ Tài Chính (TT 107/2017 hành chính sự nghiệp công lập, TT 200/2014 DN cho tư thục), có HT + Kế toán trưởng e-signature, submit qua hệ thống MOET online.

## Proposed Fix

1. **Report templates:** Excel templates per TT 107/2017 + TT 200/2014 with formula bindings
2. **Aggregation service:** pull số HS + thu/chi/miễn giảm/scholarship from billing + payroll
3. **E-signature integration:** support common VN e-signature providers (VNPT-CA, FPT-CA, BkavCA)
4. **MOET online submission:** PDF + XML format if MOET API available, else download for manual upload

## Acceptance Criteria

- [ ] Templates implemented + tested with sample data
- [ ] E-signature workflow (HT + Kế toán trưởng dual-sign)
- [ ] Quarterly + annual schedule auto-prep + reminder
- [ ] Test: Q4/2026 report → all fields correct + e-signed PDF generated
- [ ] business-logic-review.md 5-attribute (Source: TT 107/2017 + TT 200/2014; Compliance: Compliant)

## Related

- **Depends on:** GAP-334 (multi-fee), GAP-062 (payroll)
- **Wave plan:** Bucket D Stage 4

## Log

- **2026-05-04** — Filed Wave 17 Bucket D.
