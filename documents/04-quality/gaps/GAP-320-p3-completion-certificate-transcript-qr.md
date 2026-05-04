# GAP-320: Completion Certificate + Student Transcript with QR Verify

**Status:** 🔵 OPEN
**Priority:** 🟡 P2
**Domain:** Backend (kiteclass-core document module) + Frontend
**Found:** 2026-05-04 (Persona Review Round 1 — P3 Bucket C)
**Affects:** 1 AC tenant + indirectly student exit

---

## Problem

P3 issue completion certificate khi student hoàn thành lớp (e.g. Anh-Adv với CEFR alignment). Yêu cầu:
1. Template với header trung tâm + ký giám đốc + QR verify
2. PDF generator with center-specific branding
3. Student transcript exportable (all grades + attendance) PDF
4. QR verify endpoint cho parent/employer scan validate authenticity

## Root Cause

`module/document` có scaffold templates nhưng:
- Certificate template chưa có
- QR verify chưa có
- Transcript generator chưa có

## Current State (verified 2026-05-04)

| Component | Path | State |
|-----------|------|-------|
| Document module | `kiteclass-core/.../module/document/` | ✅ scaffold |
| Certificate template | — | ❌ missing |
| Transcript template | — | ❌ missing |
| QR verify service + endpoint | — | ❌ missing |
| Frontend "Issue certificate" wizard | — | ❌ missing |

## Proposed Fix

1. Certificate + transcript PDF templates (Thymeleaf or React-PDF)
2. QR verify endpoint `/verify/{certId}` returns issuance metadata
3. Center branding integration (depends on existing branding module)
4. Frontend wizard: select student + class → preview → issue
5. Bulk issuance for class graduations

## Acceptance Criteria

- [ ] Certificate PDF template with center logo + director signature image + QR code
- [ ] QR scans to verify endpoint returns issuance + student + class + date
- [ ] Transcript PDF includes all grades + attendance % + curriculum progress
- [ ] Bulk issuance for full class with progress indicator
- [ ] RBAC: only giám đốc + admin can issue

## Linked ACs

| AC ID | Persona | Doc |
|-------|---------|-----|
| AC-EXIT-001 | Tenant Director | `P3-medium-center.md` |

## Related

- Existing: `module/branding` (for logo/center identity)
- Persona review: §2 (Tenant AC-EXIT-001)

## Log

- **2026-05-04** Created from Persona Review Round 1 P3 Bucket C.
