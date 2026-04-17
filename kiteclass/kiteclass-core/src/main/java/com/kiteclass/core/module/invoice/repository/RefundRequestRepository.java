package com.kiteclass.core.module.invoice.repository;

import com.kiteclass.core.module.invoice.entity.RefundRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Repository for {@link RefundRequest} entity.
 *
 * @author KiteClass Team
 * @since 2.8.0
 */
@Repository
public interface RefundRequestRepository extends JpaRepository<RefundRequest, Long> {

    /**
     * Finds refund request by ID (excluding soft-deleted).
     *
     * @param id the refund request ID
     * @return Optional containing request if found
     */
    Optional<RefundRequest> findByIdAndDeletedFalse(Long id);

    /**
     * Finds all refund requests for an invoice (excluding soft-deleted), paginated.
     *
     * @param invoiceId the invoice ID
     * @param pageable pagination parameters
     * @return Page of refund requests
     */
    Page<RefundRequest> findByInvoiceIdAndDeletedFalse(Long invoiceId, Pageable pageable);
}
