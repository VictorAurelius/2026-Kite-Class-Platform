package com.kiteclass.core.module.invoice.service;

import com.kiteclass.core.config.TestContainersConfiguration;
import com.kiteclass.core.config.TestSecurityConfig;
import com.kiteclass.core.config.TestTenantContextFilter;
import com.kiteclass.core.module.invoice.repository.InvoiceRepository;
import com.kiteclass.core.testutil.InvoiceTestDataBuilder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link InvoiceNumberGenerator}.
 *
 * @author KiteClass Team
 * @since 2.8.0
 */
@SpringBootTest
@ActiveProfiles("test")
@Import({TestContainersConfiguration.class, TestSecurityConfig.class, TestTenantContextFilter.class})
@ContextConfiguration(initializers = TestContainersConfiguration.Initializer.class)
@Transactional
class InvoiceNumberGeneratorTest {

    @Autowired
    private InvoiceNumberGenerator invoiceNumberGenerator;

    @Autowired
    private InvoiceRepository invoiceRepository;

    private UUID tenantId;

    @BeforeEach
    void setUp() {
        tenantId = UUID.randomUUID();
    }

    @Test
    void generate_firstInvoice_returnsInv2026000001() {
        // When
        String invoiceNumber = invoiceNumberGenerator.generate(tenantId);

        // Then
        assertThat(invoiceNumber).matches("INV-2026-\\d{6}");
        assertThat(invoiceNumber).endsWith("000001");
    }

    @Test
    void generate_secondInvoice_incrementsSequence() {
        // Given: First invoice created
        String first = invoiceNumberGenerator.generate(tenantId);

        // When: Generate second invoice
        String second = invoiceNumberGenerator.generate(tenantId);

        // Then: Sequence incremented
        assertThat(second).isEqualTo("INV-2026-000002");
    }

    @Test
    void generate_multipleInvoices_incrementsCorrectly() {
        // When: Generate 5 invoices
        String inv1 = invoiceNumberGenerator.generate(tenantId);
        String inv2 = invoiceNumberGenerator.generate(tenantId);
        String inv3 = invoiceNumberGenerator.generate(tenantId);
        String inv4 = invoiceNumberGenerator.generate(tenantId);
        String inv5 = invoiceNumberGenerator.generate(tenantId);

        // Then: All sequential
        assertThat(inv1).endsWith("000001");
        assertThat(inv2).endsWith("000002");
        assertThat(inv3).endsWith("000003");
        assertThat(inv4).endsWith("000004");
        assertThat(inv5).endsWith("000005");
    }
}
