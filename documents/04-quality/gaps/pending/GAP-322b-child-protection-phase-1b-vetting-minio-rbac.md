# GAP-322b: Child Protection Phase 1B — Staff vetting workflow + MinIO encrypted bucket + RBAC gate

**Status:** 🟡 PARTIAL — Phase 1B foundation shipped (Wave 18b2 Bucket B)
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

- [x] `Vetting` entity + repository + service + state machine (Phase 1B foundation — `Vetting.java`, `VettingRepository.java`, `VettingService` interface + `VettingServiceImpl`, `VettingStatus` enum)
- [x] V52 migration (after V51) with status + audit columns + CHECK constraint on enum
- [x] `VettingDocumentStorage` contract pinned + `MinIOVettingDocumentStorageImpl` stub (concrete MinIO SDK wiring with SSE-S3 + signed URL helper deferred to Phase 1B follow-up)
- [x] RBAC gate at controller level: SAFEGUARDING_OFFICER only on `/api/v1/vettings/*`, anyone else 403 `VETTING_RBAC_DENIED`
- [ ] RBAC filter on student-data endpoints: unverified teacher → 403 (separate filter aspect — Phase 1B follow-up)
- [ ] Admin verify queue UI (list + preview + approve/reject) — Phase 1B follow-up
- [ ] Annual re-vetting cron job + reminder — Phase 1B follow-up
- [x] Tests: vetting state machine unit (10 transition tests + create/find/softDelete = 30 unit) + storage stub contract test (9) + web-slice integration test (9 RBAC + happy-path)
- [x] Business docs updated: BR-VETTING-001..005 (`rules.md` v0.2) + UC-VETTING-001..005 (`use-cases.md` v0.2) + 5 endpoints + 4 schemas + 7 error codes (`api-contract.md` v0.2)
- [x] mvn green (`./mvnw test -Dtest='VettingServiceTest,VettingDocumentStorageStubTest,VettingIntegrationTest'` — 48 tests pass)

## Phase 1B follow-up scope (deferred — separate sister PRs)

- LLTP file-upload endpoint + UI form
- Verify-queue admin UI (list + preview signed URLs + approve/reject buttons)
- Concrete MinIO SDK wiring (server-side AES-256, signed URLs, 7-year retention bucket lifecycle)
- RBAC filter aspect: teacher → 403 on student-PII endpoints unless an APPROVED Vetting exists
- Annual re-vetting cron + reminder emails
- Tổng đài 111 webhook (also tracked under GAP-322c Phase 1C)

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

- **2026-05-04** — Phase 1B foundation shipped (this PR — Wave 18b2 Bucket B). `Vetting` entity + AES-256 on `lltp_number` / `police_check_details` (BR-VETTING-002, reuses Wave 18b1 `AesGcmAttributeConverter`); state-machine guard PENDING→SUBMITTED→INTERVIEW_DONE→APPROVED|REJECTED + APPROVED→EXPIRED at service layer (BR-VETTING-001); `VettingDocumentStorage` contract pinned + `MinIOVettingDocumentStorageImpl` stub returning deterministic URLs (BR-VETTING-004); `VettingController` RBAC gate restricting `/api/v1/vettings/*` to `SAFEGUARDING_OFFICER` only (BR-VETTING-003); V52 migration with CHECK enum + indexes; 48 tests green (30 unit + 9 storage contract + 9 web-slice integration). Business docs v0.2 (`rules.md` BR-VETTING-001..005 with 5-attribute frontmatter, `use-cases.md` UC-VETTING-001..005, `api-contract.md` 5 endpoints + 4 schemas + 7 error codes). Status: PARTIAL per `gap-done-discipline.md` §3 — file-upload UI, verify-queue UI, concrete MinIO SDK, RBAC filter aspect on student-PII endpoints, annual re-vetting cron all deferred to Phase 1B follow-up sister PRs.
- **2026-05-04** — Wave 18b3 Bucket B (PR #782) shipped LLTP upload UI + concrete AWS SDK v2 `MinIOVettingDocumentStorageImpl` (replaces 18b2 stub). Real `S3Client.putObject` with config-driven endpoint via `childprotection.minio.{endpoint, bucket, access-key, secret-key, region}` in `application.yml` (no hardcoded credentials). New `POST /api/v1/vettings/{vettingId}/documents` multipart endpoint accepting PDF + image/* up to 10MB; returns `{vettingId, storageKey, sizeBytes, contentType}` (V54 NOT used — file metadata lives in response, separate `vetting_document` table deferred to Phase 1C). FE upload form at `(dashboard)/admin/vetting/[vettingId]/upload/page.tsx` — chose existing `(dashboard)/admin/` route over new `(dashboard)/safeguarding/` to preserve dashboard structure (RBAC enforced server-side via existing controller-level role gate). Tests: 28 BE additions (11 `MinIOVettingDocumentStorageImplTest` unit + 5 `VettingDocumentUploadControllerTest` slice + 3 `MinIOVettingDocumentStorageIT` LocalStack/MinIO testcontainer round-trip + 9 `VettingIntegrationTest` bean wiring updates) + 5 FE component tests (603/603 FE suite green, no regressions). Jacoco coverage on new code: `MinIOVettingDocumentStorageImpl` 93% instructions / 75% branches / 7-of-7 methods; `VettingController` 79% instructions / 75% branches (controller defensive IOException paths not realistic to mock); `VettingDocumentResponse` 79%. Sonar gate ≥80% on new code via merged jacoco.xml from GAP-347 #775 fix. Status stays 🟡 PARTIAL — resumable multipart upload, virus scan webhook, document deletion/replacement workflow, audit-log entries on upload, hash-chain audit infrastructure all routed to Phase 1C (GAP-322c).
- **2026-05-04** — Filed by Wave 18b1 closure coordinator. Per `gap-done-discipline.md` §3 PARTIAL exit ramp.
