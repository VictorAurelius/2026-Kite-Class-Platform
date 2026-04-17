package com.kiteclass.core.module.invoice.dto;

import com.kiteclass.core.common.constant.InvoiceStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Response DTO for invoice.
 *
 * @author KiteClass Team
 * @since 2.8.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InvoiceResponse {

    private Long id;
    private String invoiceNumber;
    private Long studentId;
    private Long classId;
    private Long enrollmentId;
    private InvoiceStatus status;
    private LocalDate issueDate;
    private LocalDate dueDate;
    private LocalDate periodStart;
    private LocalDate periodEnd;
    private BigDecimal subtotal;
    private BigDecimal discount;
    private BigDecimal total;
    private BigDecimal amountPaid;
    private BigDecimal balanceDue;
    private LocalDateTime paidAt;
    private String notes;
    private List<InvoiceItemResponse> items;
    private List<InvoiceAdjustmentResponse> adjustments;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
