package com.kiteclass.core.module.parent.repository;

import com.kiteclass.core.common.constant.InvoiceStatus;
import com.kiteclass.core.config.TestContainersConfiguration;
import com.kiteclass.core.config.TestSecurityConfig;
import com.kiteclass.core.config.TestTenantContextFilter;
import com.kiteclass.core.module.invoice.entity.Invoice;
import com.kiteclass.core.module.invoice.entity.InvoiceItem;
import com.kiteclass.core.module.invoice.repository.InvoiceRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.hibernate.SessionFactory;
import org.hibernate.stat.Statistics;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * GAP-321b Phase 1B remainder — verifies that
 * {@link InvoiceRepository#findByStudentIdAndDueDateRange} loads
 * {@code Invoice} + {@code items} + {@code adjustments} with ≤3 prepared
 * statements per parent-facet call (BR-PARENT-FACET-FEES-002).
 *
 * <p>Why ≤3? Spring Data {@code Page} composition emits 1 count query +
 * 1 page select. With {@code @EntityGraph(items, adjustments)} both
 * collections coalesce into the page select via LEFT JOINs, so iterating
 * the items + adjustments collections triggers no extra round-trip. A
 * conservative ceiling of 3 leaves headroom for Hibernate's internal
 * collection-init heuristics (some versions emit a single coalesce-init
 * statement).
 *
 * <p>Uses {@link SpringBootTest} with the project's {@link
 * TestContainersConfiguration} so JPA auditing populates audit columns
 * (matches the pattern used by {@code InvoiceFlowIntegrationTest} et al.).
 *
 * <p>Guarded by {@code ENABLE_INTEGRATION_TESTS=true} per the existing
 * project convention.
 *
 * @since 2.18.2 (Wave 18b3 Bucket C — GAP-321b Phase 1B remainder)
 */
@EnabledIfEnvironmentVariable(named = "ENABLE_INTEGRATION_TESTS", matches = "true")
@SpringBootTest
@ActiveProfiles("test")
@Import({TestContainersConfiguration.class, TestSecurityConfig.class, TestTenantContextFilter.class})
@ContextConfiguration(initializers = TestContainersConfiguration.Initializer.class)
@Transactional
@DisplayName("ParentFeesFacet — N+1 protection (BR-PARENT-FACET-FEES-002)")
class ParentFeesFacetEntityGraphIT {

    @Autowired private InvoiceRepository invoiceRepository;
    @PersistenceContext private EntityManager entityManager;

    private static final Long CHILD_ID = 999_001L;

    @Test
    @DisplayName("findByStudentIdAndDueDateRange runs ≤3 prepared statements when items + adjustments accessed")
    void rangeQueryDoesNotTriggerN1OnCollections() {
        UUID tenant = UUID.randomUUID();

        // Given: 3 invoices in range, each with 1 item.
        for (int n = 0; n < 3; n++) {
            Invoice inv = Invoice.builder()
                    .invoiceNumber("INV-RNG-N1-" + n)
                    .studentId(CHILD_ID)
                    .classId(1L)
                    .enrollmentId(900L + n)
                    .status(InvoiceStatus.SENT)
                    .issueDate(LocalDate.parse("2026-04-15"))
                    .dueDate(LocalDate.parse("2026-04-15").plusDays(n))
                    .periodStart(LocalDate.parse("2026-04-01"))
                    .periodEnd(LocalDate.parse("2026-04-30"))
                    .build();
            inv.setInstanceId(tenant);

            InvoiceItem item = InvoiceItem.builder()
                    .description("Tuition — month " + n)
                    .quantity(1)
                    .unitPrice(new BigDecimal("1000000.00"))
                    .amount(new BigDecimal("1000000.00"))
                    .build();
            inv.addItem(item);

            invoiceRepository.save(inv);
        }
        entityManager.flush();
        entityManager.clear();

        Statistics stats = entityManager.getEntityManagerFactory()
                .unwrap(SessionFactory.class)
                .getStatistics();
        stats.setStatisticsEnabled(true);
        stats.clear();

        // When: query parent-facet method + iterate both collections per row.
        Pageable pageable = PageRequest.of(0, 10, Sort.by(Sort.Direction.DESC, "dueDate"));
        Page<Invoice> page = invoiceRepository.findByStudentIdAndDueDateRange(
                CHILD_ID,
                LocalDate.parse("2026-04-01"),
                LocalDate.parse("2026-04-30"),
                pageable);

        int totalItems = 0;
        int totalAdjustments = 0;
        for (Invoice inv : page.getContent()) {
            totalItems += inv.getItems().size();
            totalAdjustments += inv.getAdjustments().size();
        }

        long selectCount = stats.getPrepareStatementCount();

        // Then: ≤3 prepared statements (1 count + 1 page-select-with-joins +
        // ≤1 coalesce). Asserting the upper bound — Hibernate collection
        // init heuristics may legitimately emit a single coalesce statement
        // even with EntityGraph, but never per-row N+1.
        assertThat(page.getContent()).hasSize(3);
        assertThat(totalItems).isGreaterThanOrEqualTo(3);
        assertThat(totalAdjustments).isGreaterThanOrEqualTo(0);
        assertThat(selectCount)
                .as("findByStudentIdAndDueDateRange must not trigger N+1 — "
                        + "expected ≤3 prepared statements, got %d", selectCount)
                .isLessThanOrEqualTo(3L);
    }
}
