package com.kiteclass.core.module.invoice.repository;

import com.kiteclass.core.module.invoice.entity.Invoice;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
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
}
