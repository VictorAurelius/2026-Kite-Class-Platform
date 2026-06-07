package com.kiteclass.core.integration;

import com.kiteclass.core.common.constant.ClassStatus;
import com.kiteclass.core.common.constant.EnrollmentStatus;
import com.kiteclass.core.common.context.TenantContext;
import com.kiteclass.core.common.exception.ValidationException;
import com.kiteclass.core.config.TestContainersConfiguration;
import com.kiteclass.core.module.clazz.entity.Class;
import com.kiteclass.core.module.clazz.repository.ClassRepository;
import com.kiteclass.core.module.enrollment.entity.Enrollment;
import com.kiteclass.core.module.enrollment.repository.EnrollmentRepository;
import com.kiteclass.core.module.invoice.dto.BatchInvoiceConfirmResponse;
import com.kiteclass.core.module.invoice.dto.BatchInvoiceLineItem;
import com.kiteclass.core.module.invoice.dto.BatchInvoicePreviewResponse;
import com.kiteclass.core.module.invoice.event.InvoiceCreatedEvent;
import com.kiteclass.core.module.invoice.repository.InvoiceRepository;
import com.kiteclass.core.module.invoice.service.InvoiceBatchService;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.annotation.Rollback;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.event.ApplicationEvents;
import org.springframework.test.context.event.RecordApplicationEvents;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Integration test for batch monthly invoice generation (GAP-297).
 *
 * <p>Runs against real PostgreSQL via Testcontainers (schema from JPA entities,
 * Flyway disabled in the test profile). Verifies:
 * <ol>
 *   <li>batch-generate previews the correct count + total WITHOUT persisting;</li>
 *   <li>batch-confirm persists one invoice per active enrollment + emits one
 *       {@code InvoiceCreatedEvent} per invoice;</li>
 *   <li>re-running batch-confirm for the same (tenant, month) is idempotent —
 *       no duplicate invoices;</li>
 *   <li>mid-month enrollments are pro-rated;</li>
 *   <li>enrollment discount is applied;</li>
 *   <li>an invalid month string is rejected.</li>
 * </ol>
 *
 * @author KiteClass Team
 * @since GAP-297
 */
@SpringBootTest
@ActiveProfiles("test")
@Import(TestContainersConfiguration.class)
@ContextConfiguration(initializers = TestContainersConfiguration.Initializer.class)
@Transactional
@Rollback(true)
@RecordApplicationEvents
class BatchInvoiceGenerationIT {

    private static final String MONTH = "2026-05"; // 31-day month → clean pro-rata maths

    @Autowired
    private InvoiceBatchService invoiceBatchService;

    @Autowired
    private EnrollmentRepository enrollmentRepository;

    @Autowired
    private InvoiceRepository invoiceRepository;

    @Autowired
    private ClassRepository classRepository;

    @Autowired
    private ApplicationEvents events;

    @PersistenceContext
    private EntityManager entityManager;

    private UUID tenantId;

    @BeforeEach
    void setUp() {
        tenantId = UUID.randomUUID();
        TenantContext.setCurrentTenant(tenantId);
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    @Test
    @DisplayName("batch-generate previews 60 invoices, batch-confirm persists 60 + 60 events, re-confirm is idempotent")
    void batchGenerateThenConfirm_creates60Invoices_andIsIdempotent() {
        Long classId = seedClass("Toán 6A");
        for (int i = 1; i <= 60; i++) {
            // All enrolled before the month → full-month tuition of 1,000,000 each.
            seedEnrollment((long) i, classId, new BigDecimal("1000000"),
                    BigDecimal.ZERO, LocalDate.of(2026, 4, 1));
        }
        entityManager.flush();

        long invoicesBefore = invoiceRepository.count();

        // ===== batch-generate (preview, no persistence) =====
        BatchInvoicePreviewResponse preview = invoiceBatchService.generatePreview(MONTH);
        assertThat(preview.invoiceCount()).isEqualTo(60);
        assertThat(preview.invoices()).hasSize(60);
        assertThat(preview.totalRevenue()).isEqualByComparingTo(new BigDecimal("60000000.00"));
        // No rows persisted by preview.
        assertThat(invoiceRepository.count()).isEqualTo(invoicesBefore);

        // ===== batch-confirm (persist + events) =====
        BatchInvoiceConfirmResponse confirm = invoiceBatchService.confirm(MONTH);
        entityManager.flush();
        assertThat(confirm.createdCount()).isEqualTo(60);
        assertThat(confirm.skippedCount()).isZero();
        assertThat(confirm.totalRevenue()).isEqualByComparingTo(new BigDecimal("60000000.00"));
        assertThat(confirm.createdInvoiceIds()).hasSize(60);

        // 60 invoice rows persisted (delta over any pre-existing rows from the shared container).
        assertThat(invoiceRepository.count() - invoicesBefore).isEqualTo(60);
        // 60 outbox/application events emitted (one per created invoice).
        assertThat(events.stream(InvoiceCreatedEvent.class).count()).isEqualTo(60);

        // ===== idempotency: re-run confirm for the same (tenant, month) =====
        BatchInvoiceConfirmResponse rerun = invoiceBatchService.confirm(MONTH);
        entityManager.flush();
        assertThat(rerun.createdCount()).isZero();
        assertThat(rerun.skippedCount()).isEqualTo(60);
        // No duplicate invoices created.
        assertThat(invoiceRepository.count() - invoicesBefore).isEqualTo(60);
    }

    @Test
    @DisplayName("Mid-month enrollment is pro-rated by remaining days")
    void confirm_proratesMidMonthEnrollment() {
        Long classId = seedClass("Lý 7B");
        // Full-month enrollment.
        seedEnrollment(101L, classId, new BigDecimal("2000000"),
                BigDecimal.ZERO, LocalDate.of(2026, 4, 1));
        // Mid-month enrollment on 2026-05-16 → 31 - 16 + 1 = 16 billable days.
        // 3,100,000 / 31 * 16 = 1,600,000.00 exactly.
        Enrollment mid = seedEnrollment(102L, classId, new BigDecimal("3100000"),
                BigDecimal.ZERO, LocalDate.of(2026, 5, 16));
        entityManager.flush();

        BatchInvoicePreviewResponse preview = invoiceBatchService.generatePreview(MONTH);
        BatchInvoiceLineItem midLine = preview.invoices().stream()
                .filter(l -> l.enrollmentId().equals(mid.getId()))
                .findFirst()
                .orElseThrow();
        assertThat(midLine.prorated()).isTrue();
        assertThat(midLine.billableDays()).isEqualTo(16);
        assertThat(midLine.daysInMonth()).isEqualTo(31);
        assertThat(midLine.proratedTuition()).isEqualByComparingTo(new BigDecimal("1600000.00"));
        assertThat(midLine.total()).isEqualByComparingTo(new BigDecimal("1600000.00"));

        BatchInvoiceConfirmResponse confirm = invoiceBatchService.confirm(MONTH);
        assertThat(confirm.createdCount()).isEqualTo(2);
        // 2,000,000 (full) + 1,600,000 (prorated)
        assertThat(confirm.totalRevenue()).isEqualByComparingTo(new BigDecimal("3600000.00"));
    }

    @Test
    @DisplayName("Enrollment discount is applied on the (prorated) tuition")
    void confirm_appliesEnrollmentDiscount() {
        Long classId = seedClass("Hóa 8C");
        seedEnrollment(201L, classId, new BigDecimal("1000000"),
                new BigDecimal("10.00"), LocalDate.of(2026, 4, 1));
        entityManager.flush();

        BatchInvoicePreviewResponse preview = invoiceBatchService.generatePreview(MONTH);
        BatchInvoiceLineItem line = preview.invoices().get(0);
        assertThat(line.discountAmount()).isEqualByComparingTo(new BigDecimal("100000.00"));
        assertThat(line.total()).isEqualByComparingTo(new BigDecimal("900000.00"));

        BatchInvoiceConfirmResponse confirm = invoiceBatchService.confirm(MONTH);
        assertThat(confirm.totalRevenue()).isEqualByComparingTo(new BigDecimal("900000.00"));
    }

    @Test
    @DisplayName("Invalid month string is rejected with a validation error")
    void generatePreview_rejectsInvalidMonth() {
        assertThatThrownBy(() -> invoiceBatchService.generatePreview("2026/05"))
                .isInstanceOf(ValidationException.class);
    }

    // ===================== seeding helpers =====================

    private Long seedClass(String name) {
        Class clazz = Class.builder()
                .courseId(1L)
                .name(name)
                .maxStudents(200)
                .status(ClassStatus.SCHEDULED)
                .startDate(LocalDate.of(2026, 1, 1))
                .endDate(LocalDate.of(2026, 12, 31))
                .build();
        return classRepository.save(clazz).getId();
    }

    private Enrollment seedEnrollment(Long studentId, Long classId, BigDecimal tuition,
                                      BigDecimal discountPercent, LocalDate enrollDate) {
        Enrollment enrollment = Enrollment.builder()
                .studentId(studentId)
                .classId(classId)
                .tuitionAmount(tuition)
                .discountPercent(discountPercent)
                .status(EnrollmentStatus.ACTIVE)
                .enrollmentDate(enrollDate.atStartOfDay())
                .build();
        return enrollmentRepository.save(enrollment);
    }
}
