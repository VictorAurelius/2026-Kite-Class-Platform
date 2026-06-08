package com.kiteclass.core.module.invoice.repository;

import com.kiteclass.core.module.invoice.entity.Invoice;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository for {@link Invoice} entity.
 *
 * @author KiteClass Team
 * @since 2.8.0
 */
@Repository
public interface InvoiceRepository extends JpaRepository<Invoice, Long> {

    /**
     * Finds invoice by ID (excluding soft-deleted).
     *
     * @param id the invoice ID
     * @return Optional containing invoice if found
     */
    Optional<Invoice> findByIdAndDeletedFalse(Long id);

    /**
     * Finds invoice by ID with {@code items} and {@code adjustments} prefetched
     * in a single round-trip — GAP-134 anti-N+1 path for callers that need the
     * full totals breakdown (InvoiceServiceImpl.calculateTotal, payment handlers,
     * receipt rendering).
     *
     * <p>Only one of the two {@code @OneToMany} collections is added to the entity
     * graph at a time to avoid a Cartesian product. Pair lookups (need both items
     * and adjustments) may still trigger one extra SELECT for the collection not
     * included here; callers needing true single-SQL should call {@link #findByIdWithAdjustments}
     * followed by {@code invoice.getItems()} (which triggers at most one collection fetch).
     *
     * @param id the invoice ID
     * @return Optional containing invoice with items prefetched
     * @since 2.9.1 (GAP-134)
     */
    @EntityGraph(attributePaths = {"items"})
    @Query("SELECT i FROM Invoice i WHERE i.id = :id AND i.deleted = false")
    Optional<Invoice> findByIdWithItems(@Param("id") Long id);

    /**
     * Finds invoice by ID with {@code adjustments} prefetched (GAP-134 anti-N+1).
     *
     * @param id the invoice ID
     * @return Optional containing invoice with adjustments prefetched
     * @since 2.9.1 (GAP-134)
     */
    @EntityGraph(attributePaths = {"adjustments"})
    @Query("SELECT i FROM Invoice i WHERE i.id = :id AND i.deleted = false")
    Optional<Invoice> findByIdWithAdjustments(@Param("id") Long id);

    /**
     * Finds invoice by enrollment ID (excluding soft-deleted).
     * One invoice per enrollment constraint.
     *
     * @param enrollmentId the enrollment ID
     * @return Optional containing invoice if found
     */
    Optional<Invoice> findByEnrollmentIdAndDeletedFalse(Long enrollmentId);

    /**
     * Checks if invoice exists for enrollment (excluding soft-deleted).
     *
     * @param enrollmentId the enrollment ID
     * @return true if invoice exists
     */
    boolean existsByEnrollmentIdAndDeletedFalse(Long enrollmentId);

    /**
     * Checks if a recurring monthly invoice already exists for the given
     * (tenant, enrollment, billing month) — the idempotency guard for batch
     * monthly invoice generation (GAP-297). Excludes soft-deleted rows.
     *
     * @param instanceId the tenant ID
     * @param enrollmentId the enrollment ID
     * @param billingMonth first day of the billed month
     * @return true if a monthly invoice already exists for that key
     */
    boolean existsByInstanceIdAndEnrollmentIdAndBillingMonthAndDeletedFalse(
            UUID instanceId, Long enrollmentId, LocalDate billingMonth);

    /**
     * Finds all invoices for a student (excluding soft-deleted), paginated.
     *
     * @param studentId the student ID
     * @param pageable pagination parameters
     * @return Page of invoices
     */
    Page<Invoice> findByStudentIdAndDeletedFalse(Long studentId, Pageable pageable);

    /**
     * Finds all invoices for a class (excluding soft-deleted), paginated.
     *
     * @param classId the class ID
     * @param pageable pagination parameters
     * @return Page of invoices
     */
    Page<Invoice> findByClassIdAndDeletedFalse(Long classId, Pageable pageable);

    /**
     * Finds all invoices for the current tenant (excluding soft-deleted), paginated.
     *
     * <p>Backs the Owner dashboard flat list {@code GET /api/v1/invoices}. Tenant
     * isolation is enforced automatically by the Hibernate {@code tenantFilter}
     * (from {@link com.kiteclass.core.common.entity.BaseEntity}) — this derived
     * query compiles to HQL, so the filter condition {@code instance_id = :tenantId}
     * is appended transparently and no cross-tenant leak is possible.
     *
     * @param pageable pagination + sort params
     * @return Page of invoices scoped to the current tenant
     */
    Page<Invoice> findAllByDeletedFalse(Pageable pageable);

    /**
     * Finds all overdue invoices (past due date and not paid), paginated.
     *
     * @param currentDate the current date for comparison
     * @param pageable pagination parameters
     * @return Page of overdue invoices
     */
    @Query("SELECT i FROM Invoice i WHERE i.deleted = false " +
           "AND i.dueDate < :currentDate " +
           "AND i.status IN ('SENT', 'PARTIAL', 'OVERDUE')")
    Page<Invoice> findOverdueInvoices(@Param("currentDate") LocalDate currentDate,
                                       Pageable pageable);

    /**
     * Finds latest invoice number matching pattern (for sequence generation).
     * Used by InvoiceNumberGenerator for thread-safe number generation.
     *
     * @param tenantId the tenant ID
     * @param pattern the pattern to match (e.g., "INV-2026-%")
     * @param pageable limit to first result
     * @return List containing latest invoice number (or empty if none found)
     */
    @Query("SELECT i.invoiceNumber FROM Invoice i WHERE i.instanceId = :tenantId " +
           "AND i.invoiceNumber LIKE :pattern ORDER BY i.invoiceNumber DESC")
    List<String> findLatestInvoiceNumber(@Param("tenantId") UUID tenantId,
                                          @Param("pattern") String pattern,
                                          Pageable pageable);

    /**
     * Finds unpaid invoices for a student (excluding soft-deleted), paginated.
     * Unpaid = status not in (PAID, CANCELLED, REFUNDED).
     *
     * @param studentId the student ID
     * @param pageable pagination parameters
     * @return Page of unpaid invoices
     * @since 2.14
     */
    @Query("SELECT i FROM Invoice i WHERE i.deleted = false " +
           "AND i.studentId = :studentId " +
           "AND i.status NOT IN ('PAID', 'CANCELLED', 'REFUNDED') " +
           "ORDER BY i.dueDate ASC")
    Page<Invoice> findUnpaidByStudentId(@Param("studentId") Long studentId, Pageable pageable);

    /**
     * Finds overdue unpaid invoices for a student (excluding soft-deleted), paginated.
     * Overdue = dueDate < today AND status not in (PAID, CANCELLED, REFUNDED).
     *
     * @param studentId the student ID
     * @param currentDate the current date for comparison
     * @param pageable pagination parameters
     * @return Page of overdue unpaid invoices
     * @since 2.14
     */
    @Query("SELECT i FROM Invoice i WHERE i.deleted = false " +
           "AND i.studentId = :studentId " +
           "AND i.dueDate < :currentDate " +
           "AND i.status NOT IN ('PAID', 'CANCELLED', 'REFUNDED') " +
           "ORDER BY i.dueDate ASC")
    Page<Invoice> findOverdueByStudentId(@Param("studentId") Long studentId,
                                          @Param("currentDate") LocalDate currentDate,
                                          Pageable pageable);

    /**
     * Finds invoices eligible for overdue marking (SENT or PARTIAL with dueDate before given date).
     * Used by {@link com.kiteclass.core.module.invoice.scheduler.InvoiceOverdueScheduler}.
     *
     * @param currentDate the current date for comparison
     * @return List of invoices that should be marked OVERDUE
     * @since 2026-03-24
     */
    @Query("SELECT i FROM Invoice i WHERE i.deleted = false " +
           "AND i.dueDate < :currentDate " +
           "AND i.status IN ('SENT', 'PARTIAL')")
    List<Invoice> findInvoicesEligibleForOverdue(@Param("currentDate") LocalDate currentDate);

    /**
     * Page of invoices for a student narrowed by {@code dueDate} range, with
     * {@code items} and {@code adjustments} prefetched via {@link EntityGraph}
     * to prevent N+1. Used by {@code ParentFeesFacetServiceImpl} for the
     * parent-portal fee drill-down.
     *
     * <p><b>BR-PARENT-FACET-FEES-002:</b> The parent-portal fees facet narrows
     * by {@code dueDate BETWEEN :from AND :to} (inclusive on both ends) and
     * pulls items + adjustments in a single round-trip. Hibernate Statistics
     * test asserts ≤3 prepared statements per facet call (1 count + 1 page +
     * ≤1 collection coalesce).
     *
     * <p>Date-range semantics: inclusive on both bounds — a parent asking
     * "fees due in April" passes {@code from=2026-04-01, to=2026-04-30} and
     * gets all invoices with {@code dueDate} in that closed interval.
     *
     * @param studentId the child's id (already verified by the service-layer
     *                  scope guard)
     * @param from inclusive lower bound on {@code dueDate}
     * @param to inclusive upper bound on {@code dueDate}
     * @param pageable pagination parameters; service-layer caller defaults
     *                 to {@code dueDate DESC}
     * @return page of invoices; items + adjustments batch-loaded lazily via
     *         {@code @BatchSize(20)} on the collections (GAP-1006 — cannot
     *         {@code @EntityGraph} both List bags simultaneously without
     *         {@code MultipleBagFetchException}; batch-load also avoids
     *         the in-memory pagination Hibernate applies when a {@code Page}
     *         query JOIN-FETCHes a collection).
     * @since 2.18.2 (Wave 18b3 Bucket C — GAP-321b Phase 1B remainder)
     */
    @Query("SELECT i FROM Invoice i WHERE i.deleted = false " +
           "AND i.studentId = :studentId " +
           "AND i.dueDate BETWEEN :from AND :to")
    Page<Invoice> findByStudentIdAndDueDateRange(@Param("studentId") Long studentId,
                                                  @Param("from") LocalDate from,
                                                  @Param("to") LocalDate to,
                                                  Pageable pageable);
}
