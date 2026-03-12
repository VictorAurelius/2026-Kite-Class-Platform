# TODO Action Plan - KiteClass Platform

**Generated from:** TODO analysis report (12 TODOs from recent test fixes + invoice module)
**Last Updated:** 2026-03-12
**Current Branch:** main (after PR #49 merge)

---

## 📊 Tổng Quan

**Tổng số TODOs đang active:** 12 items
**Phân loại:**
- 🔥 CRITICAL (immediate value): 3 TODOs
- 🟡 HIGH (unblock features): 3 TODOs
- 🟢 MEDIUM-LOW (improvements): 6 TODOs

**Chiến lược:** Implement CRITICAL trước (PR 2.14 - 3 invoice methods) → HIGH (caching + async events) → MEDIUM-LOW (khi có thời gian)

---

## 🔥 CRITICAL: Invoice Controller Methods (PR 2.14)

**Priority:** 1 (Highest)
**Effort:** 3-4 hours
**Blockers:** None ✅
**Branch:** `feature/PR-2.14-invoice-payment-methods`
**Target Date:** 2026-03-12 (Today)

### Tasks Overview

| TODO | Line | Description | Effort |
|------|------|-------------|--------|
| 1 | 140 | Filter unpaid invoices | 1h |
| 2 | 158 | Filter overdue invoices | 1h |
| 3 | 172 | Mark invoice as paid | 1.5h |

### Task 1: Filter Unpaid Invoices

**File:** `InvoiceController.java:140`
**Endpoint:** `GET /api/v1/invoices/student/{studentId}/unpaid`

**Implementation:**

1. **Service method** (`InvoiceService.java`):
```java
/**
 * Gets unpaid invoices for a student.
 *
 * @param studentId the student ID
 * @param pageable pagination parameters
 * @return page of unpaid invoice response DTOs
 * @since 2.14
 */
Page<InvoiceResponse> getUnpaidInvoicesByStudent(Long studentId, Pageable pageable);
```

2. **Repository query** (`InvoiceRepository.java`):
```java
@Query("""
    SELECT i FROM Invoice i
    WHERE i.student.id = :studentId
      AND i.paymentStatus != 'PAID'
      AND i.deleted = false
    ORDER BY i.dueDate ASC
    """)
Page<Invoice> findUnpaidByStudentId(@Param("studentId") Long studentId, Pageable pageable);
```

3. **Update controller:**
```java
Page<InvoiceResponse> invoices = invoiceService.getUnpaidInvoicesByStudent(studentId, pageable);
```

**Tests:**
- Integration test: Create 5 invoices (3 PAID, 2 PENDING), expect 2 results
- Edge case: All paid → empty page
- Multi-tenant: Tenant A cannot see Tenant B's unpaid invoices

---

### Task 2: Filter Overdue Invoices

**File:** `InvoiceController.java:158`
**Endpoint:** `GET /api/v1/invoices/student/{studentId}/overdue`

**Implementation:**

1. **Service method:**
```java
/**
 * Gets overdue invoices for a student (dueDate < today AND not paid).
 *
 * @param studentId the student ID
 * @param pageable pagination parameters
 * @return page of overdue invoice response DTOs
 * @since 2.14
 */
Page<InvoiceResponse> getOverdueInvoicesByStudent(Long studentId, Pageable pageable);
```

2. **Repository query:**
```java
@Query("""
    SELECT i FROM Invoice i
    WHERE i.student.id = :studentId
      AND i.dueDate < :today
      AND i.paymentStatus != 'PAID'
      AND i.deleted = false
    ORDER BY i.dueDate ASC
    """)
Page<Invoice> findOverdueByStudentId(
    @Param("studentId") Long studentId,
    @Param("today") LocalDate today,
    Pageable pageable
);
```

3. **Service implementation:**
```java
@Override
@Transactional(readOnly = true)
public Page<InvoiceResponse> getOverdueInvoicesByStudent(Long studentId, Pageable pageable) {
    LocalDate today = LocalDate.now();
    Page<Invoice> invoices = invoiceRepository.findOverdueByStudentId(studentId, today, pageable);
    return invoices.map(invoiceMapper::toResponse);
}
```

**Tests:**
- Create invoices with past/future due dates (3 overdue, 2 upcoming, 1 paid overdue)
- Expect 3 results (exclude paid even if overdue)
- Time-sensitive: Use fixed `LocalDate.of(2026, 3, 12)` in tests

---

### Task 3: Mark Invoice as Paid

**File:** `InvoiceController.java:172`
**Endpoint:** `POST /api/v1/invoices/{id}/mark-paid`

**Implementation:**

1. **DTO for request** (optional, can add later):
```java
public record MarkAsPaidRequest(
    @NotNull LocalDate paidDate,
    @Size(max = 100) String transactionId,
    @Size(max = 500) String notes
) {
    public MarkAsPaidRequest {
        if (paidDate == null) {
            paidDate = LocalDate.now();
        }
    }
}
```

2. **Service method:**
```java
/**
 * Marks invoice as paid (manual payment recording).
 *
 * @param id the invoice ID
 * @param paidDate the payment date (default: today)
 * @param transactionId optional transaction reference
 * @return updated invoice response DTO
 * @throws EntityNotFoundException if invoice not found
 * @throws ValidationException if invoice already paid
 * @since 2.14
 */
InvoiceResponse markInvoiceAsPaid(Long id, LocalDate paidDate, String transactionId);
```

3. **Service implementation:**
```java
@Override
@Transactional
public InvoiceResponse markInvoiceAsPaid(Long id, LocalDate paidDate, String transactionId) {
    Invoice invoice = invoiceRepository.findByIdAndDeletedFalse(id)
        .orElseThrow(() -> new EntityNotFoundException("INVOICE_NOT_FOUND", id));

    if (invoice.getPaymentStatus() == PaymentStatus.PAID) {
        throw new ValidationException("INVOICE_ALREADY_PAID", id);
    }

    invoice.setPaymentStatus(PaymentStatus.PAID);
    invoice.setPaidDate(paidDate != null ? paidDate : LocalDate.now());
    invoice.setTransactionId(transactionId);

    Invoice saved = invoiceRepository.save(invoice);
    return invoiceMapper.toResponse(saved);
}
```

**Controller update:**
```java
@PostMapping("/{id}/mark-paid")
public ResponseEntity<ApiResponse<InvoiceResponse>> markInvoiceAsPaid(
        @PathVariable Long id,
        @RequestBody(required = false) MarkAsPaidRequest request) {

    LocalDate paidDate = request != null ? request.paidDate() : LocalDate.now();
    String transactionId = request != null ? request.transactionId() : null;

    InvoiceResponse invoice = invoiceService.markInvoiceAsPaid(id, paidDate, transactionId);
    return ResponseEntity.ok(ApiResponse.success(invoice));
}
```

**Tests:**
- Happy path: Mark PENDING → PAID
- Validation: Already PAID → 400 error
- Multi-tenant: Cannot mark other tenant's invoice
- Audit: Check paidDate, transactionId saved correctly

---

### Success Criteria (PR 2.14)

- [ ] 3 methods implemented in service layer
- [ ] 2 repository queries added (unpaid, overdue)
- [ ] Controller TODOs removed (replace with actual calls)
- [ ] 9+ integration tests passing
- [ ] No breaking changes to existing invoice endpoints
- [ ] Messages.properties updated (2 new error codes)

**Commit Message:**
```
feat(invoice): Add payment status filtering methods

Changes:
- Filter unpaid invoices (paymentStatus != PAID)
- Filter overdue invoices (dueDate < today AND unpaid)
- Mark invoice as paid (manual payment recording)
- New repository queries with multi-tenant support
- Integration tests for all 3 methods

Resolves: 3 TODOs in InvoiceController.java (lines 140, 158, 172)
```

---

## 🟡 HIGH PRIORITY: Performance & Async Events

### 1. Student Service Caching (1 TODO)

**File:** `StudentServiceImpl.java:94`
**Priority:** 2 (High)
**Effort:** 2-3 hours
**Blockers:** None ✅

**Problem:**
- Caching disabled due to multi-tenant key conflict
- Current: `@Cacheable(value = "students", key = "#id")` causes cross-tenant cache hits
- Risk: Tenant A could get Tenant B's student data from cache

**Solution:**
Create custom `KeyGenerator` that includes `tenantId`:

```java
@Component("multiTenantKeyGenerator")
public class MultiTenantKeyGenerator implements KeyGenerator {
    @Override
    public Object generate(Object target, Method method, Object... params) {
        UUID tenantId = TenantContext.getCurrentTenantId();
        return tenantId + ":" + Arrays.toString(params);
    }
}
```

**Usage:**
```java
@Cacheable(value = "students", keyGenerator = "multiTenantKeyGenerator")
public StudentResponse getStudentById(Long id) {
    // Key format: "UUID:id" (e.g., "123e4567-...:42")
}
```

**Tests:**
- Create student in Tenant A, cache hit for Tenant A
- Switch to Tenant B, cache miss (different key)
- Verify cache eviction on update/delete

**PR:** `feature/PR-2.14.1-student-caching`

---

### 2. Async Event: Enrollment → Grade Auto-Init (1 TODO)

**File:** `EnrollmentFlowIntegrationTest.java:198`
**Priority:** 2 (High)
**Effort:** 4-5 hours
**Blockers:** None ✅

**Problem:**
- When student enrolls in course, grade record must be created
- Current: Manual creation, easy to forget
- Need: Auto-init grade on `ENROLLMENT_CREATED` event

**Solution:**

1. **Create event** (`EnrollmentEvent.java`):
```java
public record EnrollmentCreatedEvent(
    UUID eventId,
    Long enrollmentId,
    Long studentId,
    Long courseId,
    UUID tenantId,
    LocalDateTime timestamp
) implements DomainEvent {}
```

2. **Publish event** in `EnrollmentService.createEnrollment()`:
```java
Enrollment saved = enrollmentRepository.save(enrollment);
eventPublisher.publishEvent(new EnrollmentCreatedEvent(
    UUID.randomUUID(),
    saved.getId(),
    saved.getStudent().getId(),
    saved.getCourse().getId(),
    TenantContext.getCurrentTenantId(),
    LocalDateTime.now()
));
```

3. **Event listener** (`GradebookEventListener.java`):
```java
@Component
@Slf4j
public class GradebookEventListener {

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleEnrollmentCreated(EnrollmentCreatedEvent event) {
        log.info("Auto-creating grade for enrollment: {}", event.enrollmentId());

        TenantContext.setCurrentTenantId(event.tenantId());
        try {
            gradeService.initializeGradeForEnrollment(event.enrollmentId());
        } finally {
            TenantContext.clear();
        }
    }
}
```

**Tests:**
- Enroll student → verify grade auto-created
- Async verification (use `@Await` or `Thread.sleep(500)`)
- Multi-tenant: Event includes tenantId, grade created in correct tenant

**PR:** `feature/PR-2.15-async-gradebook-events`

---

### 3. Async Event: Assignment → Grade Auto-Init (1 TODO)

**File:** `AssignmentFlowIntegrationTest.java:257`
**Priority:** 2 (High)
**Effort:** 3-4 hours (similar to #2)

**Problem:**
- When assignment created, grade entries for all enrolled students needed
- Current: Manual batch creation
- Need: Auto-create on `ASSIGNMENT_CREATED` event

**Solution:**

1. **Event:**
```java
public record AssignmentCreatedEvent(
    UUID eventId,
    Long assignmentId,
    Long courseId,
    UUID tenantId,
    LocalDateTime timestamp
) implements DomainEvent {}
```

2. **Listener:**
```java
@Async
@TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
public void handleAssignmentCreated(AssignmentCreatedEvent event) {
    log.info("Auto-creating grades for assignment: {}", event.assignmentId());

    TenantContext.setCurrentTenantId(event.tenantId());
    try {
        gradeService.initializeGradesForAssignment(event.assignmentId(), event.courseId());
    } finally {
        TenantContext.clear();
    }
}
```

3. **Service method:**
```java
@Transactional
public void initializeGradesForAssignment(Long assignmentId, Long courseId) {
    List<Enrollment> enrollments = enrollmentRepository.findByCourseId(courseId);

    List<Grade> grades = enrollments.stream()
        .map(e -> Grade.builder()
            .student(e.getStudent())
            .assignment(assignmentRepository.getReferenceById(assignmentId))
            .score(BigDecimal.ZERO)
            .status(GradeStatus.NOT_SUBMITTED)
            .build())
        .toList();

    gradeRepository.saveAll(grades);
}
```

**PR:** `feature/PR-2.15-async-gradebook-events` (same as #2, combined)

---

## 📅 Execution Schedule

### Week 1 (Current - 2026-03-12)
- [x] PR 2.13: Integration test improvements (DONE ✅)
- [x] PR 2.15: Fix remaining test failures (DONE ✅)
- [x] PR #49: Merged to main (DONE ✅)
- [x] **PR 2.15: Async gradebook events** (2 HIGH TODOs) - DONE ✅
  - [x] AssignmentCreatedEvent domain event
  - [x] GradeEventListener with 2 handlers
  - [x] Event publishing in AssignmentService
  - [x] Auto-create Grade on enrollment (idempotent)
  - [x] Auto-create GradeComponents on assignment (batch)
  - [x] All tests passing (555 tests, 0 failures)
  - [x] PR #53 merged to main
- [x] **PR 2.14: Invoice payment methods** (3 CRITICAL TODOs) - DONE ✅
  - [x] Task 1: Filter unpaid invoices (findUnpaidByStudentId)
  - [x] Task 2: Filter overdue invoices (findOverdueByStudentId)
  - [x] Task 3: Mark invoice as paid (markInvoiceAsPaid)
  - [x] Integration tests (558 tests, 0 failures)
  - [x] PR #50 merged to main

### Week 2 (2026-03-13 → 2026-03-19)
- [ ] **PR 2.14.1: Student caching** (1 HIGH TODO)
  - [ ] Custom KeyGenerator with tenantId
  - [ ] Re-enable `@Cacheable` annotations
  - [ ] Tests (cache hit/miss per tenant)

### Week 3+ (Backlog - MEDIUM Priority)
- [ ] Test fixtures setup (6 TODOs) - Value: High, Effort: 3h
- [ ] Frontend data fetching (6 TODOs) - Value: Medium, Effort: 4h

---

## 📊 Progress Tracking

| Week | Resolved | Remaining | % Complete |
|------|----------|-----------|------------|
| Week 1 (Current) | 5 | 7 | 42% (PR 2.14 ✅ + PR 2.15 ✅) |
| Week 2 End (Target) | 6 | 6 | 50% (PR 2.14.1) |
| Week 3+ | 12 | 0 | 100% |

**Current Focus:** 🟡 PR 2.14.1 - Student caching (1 HIGH TODO)
**Just Completed:**
- ✅ PR 2.15 - Async gradebook events (2 TODOs)
- ✅ PR 2.14 - Invoice payment methods (3 TODOs)

---

## 🎯 Success Metrics

### PR 2.14 (Invoice Methods)
- **Metric 1:** 3 TODOs resolved → 0 TODOs in `InvoiceController.java`
- **Metric 2:** Test coverage ≥ 85% for new methods
- **Metric 3:** No breaking changes to existing invoice endpoints
- **Metric 4:** CI passes (all 555+ tests green)

### PR 2.14.1 (Student Caching)
- **Metric 1:** 1 TODO resolved → Caching re-enabled
- **Metric 2:** Cache hit rate ≥ 70% in load tests
- **Metric 3:** No cross-tenant cache leaks (security test)

### PR 2.15 (Async Events)
- **Metric 1:** 2 TODOs resolved → Events auto-create grades
- **Metric 2:** Event delivery ≤ 500ms (async performance)
- **Metric 3:** 100% tenant isolation in async context

---

## 🔗 References

### Internal Docs
- **TODO Analysis:** `documents/05-qa-and-best-practices/todo-comments-analysis-report.md`
- **Master Plan:** `documents/03-planning/implementation/kiteclass-implementation-plan.md`
- **PR Dependency Graph:** `documents/03-planning/implementation/pr-dependency-graph-v2.md`

### Source Files
- **Invoice Controller:** `kiteclass/kiteclass-core/src/main/java/com/kiteclass/core/module/invoice/controller/InvoiceController.java`
- **Student Service:** `kiteclass/kiteclass-core/src/main/java/com/kiteclass/core/module/student/service/impl/StudentServiceImpl.java`
- **Integration Tests:** `kiteclass/kiteclass-core/src/test/java/com/kiteclass/core/integration/flow/`

---

## 📝 Notes

### Why PR 2.14 First?
1. **Immediate user value:** Invoice filtering is customer-facing feature
2. **No blockers:** All dependencies ready (repository, service, mapper exist)
3. **Quick win:** 3-4 hours vs 2-3 days for async events
4. **Test foundation:** Invoice tests already mature (18 tests passing)

### Why Async Events After Caching?
- **Complexity:** Events require careful design (rollback, multi-tenant context)
- **Testing:** Async tests harder to debug (timing issues)
- **Impact:** Lower priority than caching (performance > automation)

### Risk Mitigation
- **Risk 1:** PR 2.14 takes longer than 4h
  - Mitigation: Timebox to 4h, move Task 3 (mark paid) to PR 2.14.2 if needed
- **Risk 2:** Async events break existing flow
  - Mitigation: Feature flag `@ConditionalOnProperty("features.async-gradebook")`
- **Risk 3:** Caching causes cross-tenant leaks
  - Mitigation: Dedicated security test suite for cache isolation

---

**Last Updated:** 2026-03-12 (After PR 2.14 + PR 2.15 completion)
**Next Review:** After PR 2.14.1 completion (week 2)
**Status:** ✅ In progress (5/12 TODOs resolved, 42% complete)

---

## ✅ Completed PRs

### PR 2.14: Invoice Payment Methods (MERGED - 2026-03-12)

**TODOs Resolved:** 3 CRITICAL (unpaid filter, overdue filter, mark as paid)
**Implementation:**
- Added `findUnpaidByStudentId` repository query (status NOT IN PAID/CANCELLED/REFUNDED)
- Added `findOverdueByStudentId` repository query (dueDate < today AND unpaid)
- Implemented `getUnpaidInvoicesByStudent` service method
- Implemented `getOverdueInvoicesByStudent` service method
- Implemented `markInvoiceAsPaid` service method (set status=PAID, amountPaid, paidAt)
- 3 controller TODOs resolved (lines 140, 158, 172)

**Technical Details:**
- Multi-tenant support in repository queries
- Validation: Cannot mark already-paid invoice
- Pagination support for all filter endpoints
- All 558 tests passing, 0 failures
- PR #50 merged via squash commit

**Test Coverage:**
- `testFilterUnpaidInvoices`: Creates 2 invoices, marks 1 paid, expects 1 unpaid
- `testMarkInvoiceAsPaid_ValidationTests`: Success + validation tests
- `testMultiTenantIsolation_InvoiceFilters`: Tenant isolation verification

**Files Modified:**
- `InvoiceController.java` (3 TODOs resolved)
- `InvoiceRepository.java` (2 new queries)
- `InvoiceService.java` (3 new method signatures)
- `InvoiceServiceImpl.java` (3 method implementations)
- `InvoiceFlowIntegrationTest.java` (3 new tests)

---

### PR 2.15: Async Gradebook Events (MERGED - 2026-03-12)

**TODOs Resolved:** 2 (Enrollment → Grade, Assignment → GradeComponents)
**Implementation:**
- Created `AssignmentCreatedEvent` domain event
- Added `GradeEventListener` with 2 event handlers
- Auto-create Grade when student enrolls (idempotent operation)
- Auto-create GradeComponents when assignment created (batch processing)
- Event-driven architecture with silent error handling
- Multi-tenant context preserved in async handlers

**Technical Details:**
- Used sync events (not async) to match existing pattern
- Fixed compilation errors (use ID fields not entity relationships)
- All 555 tests passing, 0 failures
- PR #53 merged via squash commit

**Commits:**
- `62ad405` - feat(grade): Add assignment created event
- `8bb9eb1` - feat(grade): Publish assignment event
- `a03f3a8` - feat(grade): Add event-driven grade init
- `9f96a21` - feat(grade): Add grade event listener
- `e838322` - fix(grade): Use ID fields not relationships

**Files Modified:**
- `AssignmentCreatedEvent.java` (NEW)
- `AssignmentServiceImpl.java` (event publishing)
- `GradeEventListener.java` (NEW)
- `GradeService.java` (2 new method signatures)
- `GradeServiceImpl.java` (2 method implementations)
- `messages_vi.properties` (ASSIGNMENT_NOT_FOUND)
