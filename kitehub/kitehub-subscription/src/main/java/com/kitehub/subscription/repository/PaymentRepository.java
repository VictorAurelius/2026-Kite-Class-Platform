package com.kitehub.subscription.repository;

import com.kitehub.platform.domain.entity.Payment;
import com.kitehub.platform.domain.enums.PaymentStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
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

    @Query("SELECT p FROM Payment p WHERE p.id = :id AND p.deleted = false")
    Optional<Payment> findById(@Param("id") UUID id);

    @Query("SELECT p FROM Payment p WHERE p.subscriptionId = :subscriptionId AND p.deleted = false ORDER BY p.createdAt DESC")
    List<Payment> findBySubscriptionId(@Param("subscriptionId") UUID subscriptionId);

    @Query("SELECT p FROM Payment p WHERE p.transactionId = :transactionId AND p.deleted = false")
    Optional<Payment> findByTransactionId(@Param("transactionId") String transactionId);

    @Query("SELECT p FROM Payment p WHERE p.status = :status AND p.deleted = false ORDER BY p.createdAt DESC")
    List<Payment> findByStatus(@Param("status") PaymentStatus status);

    @Query("SELECT p FROM Payment p WHERE p.status = 'PENDING' AND p.deleted = false ORDER BY p.createdAt ASC")
    List<Payment> findPendingPayments();

    // =========================================================
    // GAP-432 Wave 41 Bucket C: bounded admin payment listing
    // (replace prior unbounded findAll() in PaymentService.getAllPayments).
    // =========================================================

    /**
     * Page through all non-deleted payments. Soft-delete filter pushed into the
     * WHERE clause so deleted rows are not streamed back.
     */
    @Query(
        value = "SELECT p FROM Payment p WHERE p.deleted = false",
        countQuery = "SELECT COUNT(p) FROM Payment p WHERE p.deleted = false"
    )
    Page<Payment> findAllNotDeleted(Pageable pageable);

    /** Page through non-deleted payments matching the given status. */
    @Query(
        value = "SELECT p FROM Payment p WHERE p.status = :status AND p.deleted = false",
        countQuery = "SELECT COUNT(p) FROM Payment p WHERE p.status = :status AND p.deleted = false"
    )
    Page<Payment> findByStatusNotDeleted(@Param("status") PaymentStatus status, Pageable pageable);
}
