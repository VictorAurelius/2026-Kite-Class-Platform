package com.kiteclass.core.module.invoice.dto;

import com.kiteclass.core.common.constant.InvoiceAdjustmentType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * Request DTO for applying adjustment to invoice.
 *
 * @author KiteClass Team
 * @since 2.8.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ApplyAdjustmentRequest {

    /**
     * Adjustment type (required).
     */
    @NotNull(message = "Adjustment type is required")
    private InvoiceAdjustmentType type;

    /**
     * Adjustment description (required).
     */
    @NotBlank(message = "Description is required")
    @Size(max = 255, message = "Description must not exceed 255 characters")
    private String description;

    /**
     * Adjustment amount (required).
     * Positive for fees/charges, negative for discounts/refunds.
     */
    @NotNull(message = "Amount is required")
    private BigDecimal amount;

    /**
     * Reason for adjustment (optional).
     */
    private String reason;
}
