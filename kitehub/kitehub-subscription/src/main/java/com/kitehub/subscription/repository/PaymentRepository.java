package com.kitehub.subscription.repository;

import com.kitehub.platform.domain.entity.Payment;
import com.kitehub.platform.domain.enums.PaymentStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository for Payment entity.
 *
 * @author KiteHub Team
 * @since 1.0.0
 */
@Repository
public interface PaymentRepository extends JpaRepository<Payment, UUID> {

    /**
     * Find payment by ID (excluding soft deleted).
     *
     * @param id Payment UUID
     * @return Optional payment
     */
    @Query("SELECT p FROM Payment p WHERE p.id = :id AND p.deleted = false")
    Optional<Payment> findById(@Param("id") UUID id);

    /**
     * Find all payments for subscription.
     *
     * @param subscriptionId Subscription UUID
     * @return List of payments
     */
    @Query("SELECT p FROM Payment p WHERE p.subscriptionId = :subscriptionId AND p.deleted = false ORDER BY p.createdAt DESC")
    List<Payment> findBySubscriptionId(@Param("subscriptionId") UUID subscriptionId);

    /**
     * Find payment by transaction ID.
     *
     * @param transactionId Transaction ID
     * @return Optional payment
     */
    @Query("SELECT p FROM Payment p WHERE p.transactionId = :transactionId AND p.deleted = false")
    Optional<Payment> findByTransactionId(@Param("transactionId") String transactionId);

    /**
     * Find payments by status.
     *
     * @param status Payment status
     * @return List of payments
     */
    @Query("SELECT p FROM Payment p WHERE p.status = :status AND p.deleted = false ORDER BY p.createdAt DESC")
    List<Payment> findByStatus(@Param("status") PaymentStatus status);

    /**
     * Find pending payments (for timeout checking).
     *
     * @return List of pending payments
     */
    @Query("SELECT p FROM Payment p WHERE p.status = 'PENDING' AND p.deleted = false ORDER BY p.createdAt ASC")
    List<Payment> findPendingPayments();
}
