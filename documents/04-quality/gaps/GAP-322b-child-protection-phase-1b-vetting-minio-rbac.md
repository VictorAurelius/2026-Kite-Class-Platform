# GAP-322b: Child Protection Phase 1B — Staff vetting workflow + MinIO encrypted bucket + RBAC gate

**Status:** 🔵 OPEN
**Priority:** 🔴 P0 LEGAL (criminal liability — sister of GAP-322 Phase 1A SHIPPED Wave 18b1)
**Domain:** Backend + Frontend + DevOps (MinIO config)
**Detected:** 2026-05-04 (Wave 18b1 Bucket E closure)
**Affects:** P5 K-12 (Luật Trẻ em 2016 Đ.25 + Decree 56/2017 vetting mandate)

## Context

Phase 1A SHIPPED Wave 18b1 (PR #767): module/childprotection/, Incident entity with AES-256-GCM encryption, AesGcmAttributeConverter (33 tests), 3 enums, IncidentService skeleton CRUD, V49 migration with SAFEGUARDING_OFFICER role + 3 permissions seeded.

This gap covers Phase 1B — staff vetting workflow + MinIO encrypted bucket + RBAC gate that prevents teacher access to student data until verified.

## Problem

Luật Trẻ em 2016 Đ.25 + Decree 56/2017 mandate background check (LLTP) on every adult who works with minors. Without enforcement:
- AC-ONBOARD-005 FAIL (50 GV vetted ≤7d)
- Criminal liability for school + platform (data processor)
- Cannot enable K12_ENTERPRISE tier

## Proposed Fix

### 1B.1 — Vetting workflow
- HR/admin uploads xlsx + zip per teacher (CCCD scan + bằng tốt nghiệp + LLTP số 2 ≤6 tháng + ảnh 3×4)
- New entity `StaffVettingRecord` (teacher_id, status, uploaded_files JSONB metadata, verified_by, verified_at, expires_at, instance_id)
- New service `StaffVettingService` with state machine `PENDING → UNDER_REVIEW → VERIFIED | REJECTED → EXPIRED`
- Migration V<N> kiteclass-core (next free after V50)

### 1B.2 — MinIO encrypted bucket
- Configure `staff-vetting-evidence/{tenantId}/{userId}/` bucket
- AES-256 server-side encryption at rest (MinIO SSE-S3)
- File-level access via signed URLs (15-min TTL, safeguarding officer + Hiệu trưởng only)
- Lifecycle: 7-year retention, no manual delete
- DevOps: MinIO config update, helm chart

### 1B.3 — RBAC gate teacher access
- Spring Security filter or aspect: TEACHER role with `vetting_status != VERIFIED` → 403 on student data endpoints
- Banner in admin panel: "{N} teachers pending vetting"
- Email + in-app reminder to admin per pending vetting
- Annual re-vetting cadence reminder; LLTP refresh ≤2 years

### 1B.4 — Verify queue UI
- Admin/safeguarding-officer page: list vetting records by status
- Click record → preview encrypted files (signed URL render) → approve/reject with reason
- Audit: every preview emits log entry (parent of GAP-322c hash-chained log)

## Acceptance Criteria

- [ ] `StaffVettingRecord` entity + repository + service + state machine
- [ ] V<N> migration (after V50) with status + audit columns
- [ ] MinIO bucket configured with SSE-S3 + signed URL helper
- [ ] RBAC filter: unverified teacher → 403 on student data endpoints
- [ ] Admin verify queue UI (list + preview + approve/reject)
- [ ] Annual re-vetting cron job + reminder
- [ ] Tests: vetting state machine unit + RBAC filter IT + signed URL TTL test
- [ ] Business docs updated: BR-CHILD-PROTECTION-{vetting} + UC-VET-UPLOAD/VERIFY/EXPIRE
- [ ] mvn + pnpm green

## Estimated Effort

~2-3 weeks:
- 322b.1: Vetting workflow entity + service (~5 days)
- 322b.2: MinIO config + signed URL (~3 days)
- 322b.3: RBAC gate + filter (~5 days)
- 322b.4: Admin verify queue UI (~5 days)
- 322b.5: Re-vetting cron + reminders (~2 days)

## Related

- **Sister of:** GAP-322 Phase 1A (PR #767) + GAP-322c Phase 1C
- **Cross-cuts:** GAP-321b/c (similar audit log pattern), GAP-322c (hash-chained audit log infrastructure)
- **DevOps dependency:** MinIO bucket creation in helm + terraform

## Log

- **2026-05-04** — Filed by Wave 18b1 closure coordinator. Per `gap-done-discipline.md` §3 PARTIAL exit ramp.
