package com.kiteclass.core.module.invoice.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request DTO for creating installment plan.
 *
 * @author KiteClass Team
 * @since 2.8.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateInstallmentPlanRequest {

    /**
     * Invoice ID (required).
     */
    @NotNull(message = "Invoice ID is required")
    @Positive(message = "Invoice ID must be positive")
    private Long invoiceId;

    /**
     * Number of installments (2-12).
     */
    @NotNull(message = "Number of installments is required")
    @Min(value = 2, message = "Minimum 2 installments required")
    @Max(value = 12, message = "Maximum 12 installments allowed")
    private Integer numberOfInstallments;
}
