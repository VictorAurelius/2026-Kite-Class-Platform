package com.kiteclass.core.module.payment.repository;

import com.kiteclass.core.module.payment.entity.Payment;
import com.kiteclass.core.module.payment.enums.PaymentStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository for Payment entity operations.
 *
 * @since 1.0.0
 */
@Repository
public interface PaymentRepository extends JpaRepository<Payment, Long> {

    /**
     * Finds payment by ID (soft delete aware).
     *
     * @param id payment ID
     * @return payment if found and not deleted
     */
    Optional<Payment> findByIdAndDeletedFalse(Long id);

    /**
     * Finds payment by transaction ID (for webhook processing).
     *
     * @param transactionId unique transaction ID
     * @return payment if found and not deleted
     */
    Optional<Payment> findByTransactionIdAndDeletedFalse(String transactionId);

    /**
     * Finds all payments for an invoice.
     *
     * @param invoiceId invoice ID
     * @return list of payments for invoice
     */
    List<Payment> findByInvoiceIdAndDeletedFalse(Long invoiceId);

    /**
     * Finds all payments for an installment.
     *
     * @param installmentId installment ID
     * @return list of payments for installment
     */
    List<Payment> findByInstallmentIdAndDeletedFalse(Long installmentId);

    /**
     * Finds payments by status with pagination.
     *
     * @param status payment status
     * @param pageable pagination parameters
     * @return page of payments with given status
     */
    Page<Payment> findByPaymentStatusAndDeletedFalse(
        PaymentStatus status, Pageable pageable);

    /**
     * Finds latest payment number for sequence generation.
     *
     * @param tenantId tenant ID
     * @param pattern payment number pattern (e.g., "PAY-2026-%")
     * @param pageable pagination (limit 1)
     * @return list with latest payment number
     */
    @Query("SELECT p.paymentNumber FROM Payment p WHERE p.instanceId = :tenantId " +
           "AND p.paymentNumber LIKE :pattern ORDER BY p.paymentNumber DESC")
    List<String> findLatestPaymentNumber(
        @Param("tenantId") UUID tenantId,
        @Param("pattern") String pattern,
        Pageable pageable);

    /**
     * Expires pending payments that have passed expiry time.
     *
     * @param now current timestamp
     * @param status target status (FAILED)
     * @return number of payments expired
     */
    @Modifying
    @Query("UPDATE Payment p SET p.paymentStatus = :status, p.failedAt = :now, " +
           "p.failureReason = 'Payment expired' " +
           "WHERE p.expiresAt < :now AND p.paymentStatus = 'PENDING'")
    int expirePendingPayments(@Param("now") LocalDateTime now,
                               @Param("status") PaymentStatus status);
}
