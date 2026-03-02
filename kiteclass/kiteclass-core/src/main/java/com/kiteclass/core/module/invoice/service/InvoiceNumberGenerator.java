package com.kiteclass.core.module.invoice.service;

import com.kiteclass.core.module.invoice.repository.InvoiceRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Year;
import java.util.List;
import java.util.UUID;

/**
 * Thread-safe invoice number generator.
 *
 * <p>Generates invoice numbers in format: INV-YYYY-NNNNNN
 * <ul>
 *   <li>INV: Prefix</li>
 *   <li>YYYY: Current year (4 digits)</li>
 *   <li>NNNNNN: Sequential number (6 digits, zero-padded)</li>
 * </ul>
 *
 * <p>Example: INV-2026-000001, INV-2026-000002, ...
 *
 * <p>Thread-safety is ensured by @Transactional annotation
 * which provides database-level locking.
 *
 * @author KiteClass Team
 * @since 2.8.0
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class InvoiceNumberGenerator {

    private final InvoiceRepository invoiceRepository;

    /**
     * Generates next invoice number for given tenant.
     *
     * <p>Thread-safe implementation:
     * <ul>
     *   <li>Transaction ensures atomicity</li>
     *   <li>Query latest number from database</li>
     *   <li>Increment and return next number</li>
     * </ul>
     *
     * @param instanceId the tenant ID
     * @return next invoice number (e.g., "INV-2026-000001")
     */
    @Transactional
    public String generate(UUID instanceId) {
        String year = String.valueOf(Year.now().getValue());
        String pattern = "INV-" + year + "-%";

        // Query latest invoice number for this year and tenant
        List<String> latest = invoiceRepository.findLatestInvoiceNumber(
                instanceId, pattern, PageRequest.of(0, 1));

        int nextNumber = 1;
        if (!latest.isEmpty()) {
            // Extract sequence number from "INV-2026-000123" -> "000123"
            String lastNumber = latest.get(0).substring(9); // Skip "INV-YYYY-"
            nextNumber = Integer.parseInt(lastNumber) + 1;
        }

        String invoiceNumber = String.format("INV-%s-%06d", year, nextNumber);
        log.debug("Generated invoice number {} for tenant {}", invoiceNumber, instanceId);

        return invoiceNumber;
    }
}
