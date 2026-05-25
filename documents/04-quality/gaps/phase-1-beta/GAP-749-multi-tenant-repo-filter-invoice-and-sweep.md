# GAP-749: Invoice multi-tenant filter + audit sweep 15 repositories cross-tenant leak class

**Status:** 🔵 OPEN
**Priority:** 🟠 P1
**Domain:** Backend (service layer + repository pattern)
**Found:** 2026-05-25 (Wave gap-746 inline salvage deferred remainder)
**Affects:** Invoice multi-tenant isolation + 15 repositories `findByIdAndDeletedFalse` pattern không có tenant filter explicit

## Problem

Wave gap-746 inline salvage 2026-05-25 evening session shipped Path A1 Enrollment fix only (`findByIdAndInstanceIdAndDeletedFalse` explicit tenant param). 2 phần scope còn lại defer:

### Phần 1 — Invoice Path C (InvoiceServiceImpl tenant filter)

`InvoiceFlowIT.testMultiTenantIsolation_InvoiceFilters` vẫn fail — `InvoiceServiceImpl.getUnpaidInvoices` own-tenant filter trả empty data. Cần:
- Investigation phase deeper — read `InvoiceServiceImpl` + `InvoiceRepository` query
- Identify `@PostConstruct` / `@Async` event listener commits invoice ngoài test transaction window
- Fix: tương tự Path A1 — explicit tenant param HOẶC Hibernate filter session-wide

### Phần 2 — Audit sweep 15 repositories cross-tenant leak class

~15 repositories trong `kiteclass/kiteclass-core/src/main/java/com/kiteclass/core/module/*/repository/` dùng pattern `findByIdAndDeletedFalse(Long id)` không có tenant filter:

```
GradeComponentRepository, SubmissionRepository, GradingScaleRepository,
AttendanceRepository, AttendancePeriodRepository, AssignmentRepository,
SubjectGradeRepository, LearningResourceRepository, VettingRepository,
RefundRequestRepository, UploadedFileRepository, IncidentRepository,
CourseModuleRepository, TranscriptRepository, (+ EnrollmentRepository
đã fixed Wave gap-746 inline salvage)
```

Cho mỗi repository:
- Check service consumer có set TenantContext + filter trước query không
- Nếu KHÔNG → vulnerable cross-tenant leak (return entity của tenant khác → 500 thay vì 404)
- Apply Path A1 pattern: add `findByIdAndInstanceIdAndDeletedFalse(Long id, UUID instanceId)` + update service callers

## Proposed Fix

### Wave dedicated (2-3h)

1. Investigation phase per `release-fix-retry-budget.md` §3.5 — read InvoiceServiceImpl + 14 còn lại repositories + service consumers
2. Apply Path A1 pattern repo + service callers atomically
3. Verify 2 residual tests PASS (`EnrollmentIT.enrollStudent_shouldIsolate_multiTenantData` + `InvoiceFlowIT.testMultiTenantIsolation_InvoiceFilters`)
4. No regression in 1480+ tests

## Acceptance Criteria

- [ ] InvoiceServiceImpl Path C fix shipped — `InvoiceFlowIT.testMultiTenantIsolation_InvoiceFilters` PASS
- [ ] 14 repositories audit sweep: identify which need Path A1 vs which already have tenant filter via Hibernate session
- [ ] All identified vulnerable repos fixed với explicit tenant param method + service caller update
- [ ] `./mvnw verify -P strict-warnings` PASS
- [ ] GAP-746 flip DONE 100% sau khi Invoice + sweep DONE

## Related

- **GAP-746** — Enrollment Path A1 fix shipped Wave gap-746 inline salvage 2026-05-25 (PARTIAL 60%)
- `EnrollmentRepository.findByIdAndInstanceIdAndDeletedFalse` — reference pattern
- `release-fix-retry-budget.md` §3.5 — investigation phase mandate
- `audit-to-gap-pipeline.md` §2.8 — fix-time state-check

## Log

- **2026-05-25 (filed):** Wave gap-746 inline salvage shipped Enrollment Path A1 only; Invoice Path C + 14 repo sweep defer dedicated future wave.
