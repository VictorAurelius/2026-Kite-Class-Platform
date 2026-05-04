# GAP-321: MOET License Verification at Tenant Onboarding

**Status:** 🔵 OPEN
**Priority:** 🔴 P0 LEGAL
**Domain:** Backend (KiteHub) + Compliance
**Detected:** 2026-05-04 (P5 K-12 persona review Round 1, Wave 17 Bucket D)
**Related Docs:** `documents/00-brd/persona-criteria/P5-k12-school.md` AC-ONBOARD-003

## Current State (verified 2026-05-04)

| Piece | File / Path | Status |
|-------|-------------|--------|
| Tenant provisioning workflow | `kitehub/kitehub-platform/src/main/java/com/kitehub/platform/...` (skeleton) | 🟡 partial |
| `mã số trường` MOET field | nowhere | ❌ missing |
| `giấy phép thành lập` upload + admin verify | nowhere | ❌ missing |
| Hiệu trưởng CCCD + chứng chỉ quản lý | nowhere | ❌ missing |
| TRIAL → ACTIVE gate based on verify | nowhere | ❌ missing |

**Grep commands run:**
```bash
grep -rli "mã số trường\|maSoTruong\|giấy phép\|MoetLicense" kitehub/ kiteclass/ documents/01-business/
# returns: nothing
```

## Problem

K-12 tenants (P5) require MOET school registration code + license + Hiệu trưởng credentials before being legally allowed to operate as a school on the platform. Without this verification, anyone can register a "trường" tenant and process minor data — directly violating Luật Giáo dục 2019 + Luật Trẻ em 2016 staff vetting requirements.

## Proposed Fix

1. Add `K12_ENTERPRISE` tier path in tenant onboarding wizard with required fields:
   - `moet_school_code` (mã số trường)
   - `establishment_license_url` (PDF upload to MinIO encrypted bucket)
   - `principal_cccd_url` (encrypted upload, GDPR/PDPL minor-adjacent — Hiệu trưởng PII)
   - `principal_management_cert_url` (encrypted)
2. New entity `TenantVerification` (tenantId, status enum PENDING/APPROVED/REJECTED, verifier_id, evidence_urls, decision_at, audit_trail JSONB)
3. KiteHub admin queue endpoint to verify / reject with reason
4. Tenant remains in TRIAL status; unable to provision learners until APPROVED
5. Audit log every step (preserve 7 years per legal compliance)

## Acceptance Criteria

- [ ] Wizard rejects K12_ENTERPRISE registration without 4 required fields
- [ ] Encrypted upload to MinIO with 2x replicas
- [ ] Admin queue list + approve/reject endpoint
- [ ] Tenant status transition gated on verification (`TRIAL`→`ACTIVE` requires `TenantVerification.status = APPROVED`)
- [ ] Audit log preserved 7 years (per `documents/00-brd/data-retention-deletion-policy.md`)

## Related

- Parent persona: `documents/00-brd/persona-criteria/P5-k12-school.md` AC-ONBOARD-003
- Existing gaps: GAP-058 (role hierarchy — Hiệu trưởng), GAP-186 (child protection)
- Legal: Luật Giáo dục 2019, Luật Trẻ em 2016 Đ.25 + Decree 56/2017

## Log

- 2026-05-04 — Filed by Wave 17 Bucket D persona review. State-check confirmed nothing exists.
