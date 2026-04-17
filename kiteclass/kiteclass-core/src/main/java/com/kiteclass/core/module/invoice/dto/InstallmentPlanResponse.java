package com.kiteclass.core.module.invoice.dto;

import com.kiteclass.core.common.constant.InstallmentPlanStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Response DTO for installment plan.
 *
 * @author KiteClass Team
 * @since 2.8.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InstallmentPlanResponse {

    private Long id;
    private Long invoiceId;
    private Integer numberOfInstallments;
    private InstallmentPlanStatus status;
    private LocalDateTime requestedAt;
    private LocalDateTime approvedAt;
    private Long approvedBy;
    private LocalDateTime rejectedAt;
    private String rejectionReason;
    private List<InstallmentResponse> installments;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
