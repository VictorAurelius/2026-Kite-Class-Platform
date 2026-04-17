package com.kiteclass.core.module.invoice.dto;

import com.kiteclass.core.common.constant.InvoiceAdjustmentType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * Response DTO for invoice adjustment.
 *
 * @author KiteClass Team
 * @since 2.8.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InvoiceAdjustmentResponse {

    private Long id;
    private InvoiceAdjustmentType type;
    private String description;
    private BigDecimal amount;
    private String reason;
}
