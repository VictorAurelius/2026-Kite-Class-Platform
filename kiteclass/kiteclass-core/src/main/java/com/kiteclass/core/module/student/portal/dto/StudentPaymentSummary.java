package com.kiteclass.core.module.student.portal.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Invoice summary row for the kc-student payments screen (GAP-269b).
 *
 * <p>Read-only projection of {@code Invoice} scoped to the calling student's
 * enrollments. Settlement is delegated to the existing owner-side payment
 * flow — students see status only.
 *
 * @since GAP-269b (Wave 51 Bucket B)
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StudentPaymentSummary {

    private Long invoiceId;
    private String invoiceNumber;
    private String description;
    private BigDecimal amountDue;
    private BigDecimal amountPaid;
    private LocalDate dueDate;
    private String status; // PENDING / PARTIALLY_PAID / PAID / OVERDUE / CANCELLED
    private String currency; // VND
}
