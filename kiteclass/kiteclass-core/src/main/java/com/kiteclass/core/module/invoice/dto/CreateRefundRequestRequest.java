package com.kiteclass.core.module.invoice.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * Request DTO for creating refund request.
 *
 * @author KiteClass Team
 * @since 2.8.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateRefundRequestRequest {

    /**
     * Invoice ID (required).
     */
    @NotNull(message = "Invoice ID is required")
    @Positive(message = "Invoice ID must be positive")
    private Long invoiceId;

    /**
     * Refund amount (required, must be positive).
     */
    @NotNull(message = "Refund amount is required")
    @Positive(message = "Refund amount must be positive")
    private BigDecimal refundAmount;

    /**
     * Reason for refund (required).
     */
    @NotBlank(message = "Reason is required")
    private String reason;
}
