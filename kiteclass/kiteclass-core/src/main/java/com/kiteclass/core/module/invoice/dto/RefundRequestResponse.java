package com.kiteclass.core.module.invoice.dto;

import com.kiteclass.core.common.constant.RefundStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Response DTO for refund request.
 *
 * @author KiteClass Team
 * @since 2.8.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RefundRequestResponse {

    private Long id;
    private Long invoiceId;
    private BigDecimal refundAmount;
    private String reason;
    private RefundStatus status;
    private Long requestedBy;
    private LocalDateTime requestedAt;
    private Long approvedBy;
    private LocalDateTime approvedAt;
    private Long rejectedBy;
    private LocalDateTime rejectedAt;
    private String rejectionReason;
    private LocalDateTime processedAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
