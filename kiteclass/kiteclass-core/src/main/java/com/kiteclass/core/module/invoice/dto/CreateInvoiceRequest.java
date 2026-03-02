package com.kiteclass.core.module.invoice.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

/**
 * Request DTO for manual invoice creation.
 *
 * <p>Used when creating invoices manually (not via enrollment event).
 *
 * @author KiteClass Team
 * @since 2.8.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateInvoiceRequest {

    /**
     * Student ID (required).
     */
    @NotNull(message = "Student ID is required")
    @Positive(message = "Student ID must be positive")
    private Long studentId;

    /**
     * Class ID (optional).
     */
    private Long classId;

    /**
     * Issue date (defaults to today if not provided).
     */
    private LocalDate issueDate;

    /**
     * Due date (required).
     */
    @NotNull(message = "Due date is required")
    private LocalDate dueDate;

    /**
     * Billing period start date (required).
     */
    @NotNull(message = "Period start date is required")
    private LocalDate periodStart;

    /**
     * Billing period end date (required).
     */
    @NotNull(message = "Period end date is required")
    private LocalDate periodEnd;

    /**
     * Additional notes (optional).
     */
    private String notes;
}
