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

    @Query("SELECT p FROM Payment p WHERE p.subscriptionId = :subscriptionId AND p.status = 'PENDING' AND p.deleted = false ORDER BY p.createdAt DESC")
    Optional<Payment> findLatestPendingBySubscriptionId(@Param("subscriptionId") UUID subscriptionId);

    @Query("SELECT p FROM Payment p WHERE p.transactionId = :transactionId AND p.deleted = false")
    Optional<Payment> findByTransactionId(@Param("transactionId") String transactionId);

    /**
     * Exact-match lookup of a payment by its SePay reference (Wave flow-kh3-2,
     * GAP-975/GAP-976). Exact equality — NOT a {@code LIKE} substring scan — so a
     * memo collision across tenants can never resolve to the wrong payment.
     */
    @Query("SELECT p FROM Payment p WHERE p.txnRef = :txnRef AND p.deleted = false")
    Optional<Payment> findByTxnRef(@Param("txnRef") String txnRef);

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

    // =========================================================
    // Wave 85 Bucket D D-AC1: cursor-based (keyset) pagination
    // for datasets >1M rows. Avoids OFFSET cliff cost.
    // Order is fixed id ASC for stable keyset traversal.
    //
    // GAP-1106: split each prior `(:cursorId IS NULL OR p.id > :cursorId)` JPQL
    // into a first-page query (no cursor param) + an after-cursor query (typed
    // cursor param), branched by the default methods below. Postgres rejected the
    // untyped null `:cursorId` in the `IS NULL` position with 42P18 ("could not
    // determine data type of parameter"); H2 (test) hid it. Same class as
    // GAP-1028 (AdminAuditLogRepository) + GAP-1105 (branding lifecycle-events).
    // =========================================================

    /** First keyset page of non-deleted payments (no cursor). Order id ASC. */
    @Query("SELECT p FROM Payment p WHERE p.deleted = false ORDER BY p.id ASC")
    List<Payment> findFirstPage(Pageable pageable);

    /** Keyset page of non-deleted payments strictly AFTER {@code cursorId}. Order id ASC. */
    @Query("SELECT p FROM Payment p WHERE p.deleted = false "
        + "AND p.id > :cursorId ORDER BY p.id ASC")
    List<Payment> findAfterCursorId(@Param("cursorId") UUID cursorId, Pageable pageable);

    /**
     * Keyset-paginate non-deleted payments starting AFTER the given cursor id.
     * Pass {@code cursorId = null} for the first page; branches to a cursor-free
     * query so no untyped null param is bound (GAP-1106).
     */
    default List<Payment> findAfterCursor(UUID cursorId, Pageable pageable) {
        return cursorId == null
            ? findFirstPage(pageable)
            : findAfterCursorId(cursorId, pageable);
    }

    /** First keyset page filtered by status (no cursor). Order id ASC. */
    @Query("SELECT p FROM Payment p WHERE p.deleted = false AND p.status = :status "
        + "ORDER BY p.id ASC")
    List<Payment> findByStatusFirstPage(@Param("status") PaymentStatus status, Pageable pageable);

    /** Keyset page filtered by status, strictly AFTER {@code cursorId}. Order id ASC. */
    @Query("SELECT p FROM Payment p WHERE p.deleted = false AND p.status = :status "
        + "AND p.id > :cursorId ORDER BY p.id ASC")
    List<Payment> findByStatusAfterCursorId(@Param("status") PaymentStatus status,
                                            @Param("cursorId") UUID cursorId,
                                            Pageable pageable);

    /**
     * Keyset variant with status filter. Pass {@code cursorId = null} for the
     * first page; branches to a cursor-free query so no untyped null param is
     * bound (GAP-1106).
     */
    default List<Payment> findByStatusAfterCursor(PaymentStatus status,
                                                  UUID cursorId,
                                                  Pageable pageable) {
        return cursorId == null
            ? findByStatusFirstPage(status, pageable)
            : findByStatusAfterCursorId(status, cursorId, pageable);
    }
}
