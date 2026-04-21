package com.kiteclass.core.module.invoice.repository;

import com.kiteclass.core.module.invoice.entity.Invoice;
import com.kiteclass.core.module.invoice.entity.InvoiceItem;
import com.kiteclass.core.testutil.IntegrationTestBase;
import com.kiteclass.core.testutil.InvoiceTestDataBuilder;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.hibernate.SessionFactory;
import org.hibernate.stat.Statistics;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * GAP-134 — Verifies that the {@code @EntityGraph} paths load {@code Invoice} +
 * {@code items} (or {@code adjustments}) in a single SELECT round-trip, preventing
 * the N+1 that existed before Wave 9.
 *
 * <p>Uses Hibernate {@link Statistics} to count emitted SELECTs. On the baseline
 * ({@code findByIdAndDeletedFalse} followed by {@code invoice.getItems()}) we expect
 * 2 queries (parent + collection). On the new {@code findByIdWithItems} we expect 1
 * query — the collection is prefetched via LEFT JOIN in the same SELECT.
 *
 * <p>Guarded by {@code ENABLE_INTEGRATION_TESTS=true} like the other repository
 * slice tests, because it needs Testcontainers + PostgreSQL.
 */
@EnabledIfEnvironmentVariable(named = "ENABLE_INTEGRATION_TESTS", matches = "true")
class InvoiceRepositoryEntityGraphTest extends IntegrationTestBase {

    @Autowired
    private InvoiceRepository invoiceRepository;

    @PersistenceContext
    private EntityManager entityManager;

    @Test
    void findByIdWithItems_runsSingleSelect_whenCollectionAccessed() {
        // Given — save an invoice with 3 line items
        Invoice invoice = InvoiceTestDataBuilder.createDefaultInvoice();
        invoice.setId(null); // let DB allocate
        invoice.setInvoiceNumber("INV-GAP134-001");
        invoice.addItem(InvoiceTestDataBuilder.createMaterialsItem());
        invoice.addItem(InvoiceTestDataBuilder.createMaterialsItem());
        Invoice saved = invoiceRepository.save(invoice);
        entityManager.flush();
        entityManager.clear();

        Statistics stats = entityManager.getEntityManagerFactory()
                .unwrap(SessionFactory.class)
                .getStatistics();
        stats.setStatisticsEnabled(true);
        stats.clear();

        // When — fetch with EntityGraph and iterate the collection
        Optional<Invoice> loaded = invoiceRepository.findByIdWithItems(saved.getId());
        assertThat(loaded).isPresent();
        int itemsSize = loaded.get().getItems().size();
        assertThat(itemsSize).isGreaterThanOrEqualTo(3);

        long selectCount = stats.getPrepareStatementCount();

        // Then — a single SELECT suffices (parent JOIN items via EntityGraph)
        assertThat(selectCount)
                .as("findByIdWithItems must not trigger N+1 when items are accessed — "
                        + "expected 1 prepared statement, got %d", selectCount)
                .isEqualTo(1L);
    }

    @Test
    void findByIdAndDeletedFalse_triggersExtraSelect_whenItemsAccessed_demonstratingBaseline() {
        // Baseline verification — without @EntityGraph, touching the lazy collection
        // after initial load triggers an extra SELECT. This documents the problem
        // GAP-134 solves; it is NOT a regression test we expect to fail.
        Invoice invoice = InvoiceTestDataBuilder.createDefaultInvoice();
        invoice.setId(null);
        invoice.setInvoiceNumber("INV-GAP134-002");
        Invoice saved = invoiceRepository.save(invoice);
        entityManager.flush();
        entityManager.clear();

        Statistics stats = entityManager.getEntityManagerFactory()
                .unwrap(SessionFactory.class)
                .getStatistics();
        stats.setStatisticsEnabled(true);
        stats.clear();

        Optional<Invoice> loaded = invoiceRepository.findByIdAndDeletedFalse(saved.getId());
        assertThat(loaded).isPresent();
        // Trigger collection init
        for (InvoiceItem ignored : loaded.get().getItems()) {
            // no-op — just force init
        }

        long selectCount = stats.getPrepareStatementCount();
        assertThat(selectCount)
                .as("baseline: legacy method must emit ≥2 statements when touching "
                        + "the lazy items collection; got %d", selectCount)
                .isGreaterThanOrEqualTo(2L);
    }
}
