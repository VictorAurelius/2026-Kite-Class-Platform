# GAP-343: Học bạ + Bằng tốt nghiệp Sealed PDF + E-Signature + Dấu Digital + QR Verification

**Status:** 🔵 OPEN
**Priority:** 🟠 P1
**Domain:** Backend + Compliance
**Detected:** 2026-05-04 (Wave 17 Bucket D)
**Related:** P5-k12-school.md AC-EXIT-001; GAP-055

## Current State (verified 2026-05-04)

```bash
grep -rl "hocba\|graduation.cert\|qr.verify" kiteclass/ --include="*.java"
```
Result: zero. No sealed PDF + e-signature + dấu digital workflow.

## Problem

End of grade 9 / 12 → học bạ chính thức + bằng tốt nghiệp THCS/THPT theo MOET format Phụ lục I TT 22/2021. Lưu 5 năm. Trường mới verify bằng QR. Without:
- Học bạ editable post-publish (forgery risk)
- Trường mới phải gọi điện confirm
- AC-EXIT-001 FAIL

## Proposed Fix

1. **Sealed PDF:** generate học bạ + bằng PDF với e-signature HT (VNPT-CA/FPT-CA) + dấu digital trường
2. **QR verification endpoint:** `GET /api/v1/verify/{certId}` returns sealed metadata
3. **5-year retention** integration with GAP-184
4. **Format:** Phụ lục I TT 22/2021 exact compliance

## Acceptance Criteria

- [ ] Sealed PDF generation with e-signature
- [ ] QR code embedded with verification URL
- [ ] Public verify endpoint (rate-limited)
- [ ] 5y retention enforced
- [ ] Test: generate học bạ for HS A → QR verify returns valid
- [ ] business-logic-review.md 5-attribute (Source: TT 22/2021 Phụ lục I + TT 32/2020 Đ.40; Compliance: Compliant)

## Related

- **Depends on:** GAP-055 (báo cáo MOET), GAP-184 (retention), GAP-323 (gradebook for ĐTBmCN)
- **Wave plan:** Bucket D Stage 2

## Log

- **2026-05-04** — Filed Wave 17 Bucket D.
