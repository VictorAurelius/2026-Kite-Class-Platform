package com.kiteclass.core.module.invoice.entity;

import com.kiteclass.core.common.constant.InvoiceAdjustmentType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.CreatedDate;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Invoice adjustment entity (discounts, fees, refunds).
 *
 * <p>Adjustments modify the invoice total:
 * <ul>
 *   <li>DISCOUNT: Negative amount (reduces total)</li>
 *   <li>LATE_FEE: Positive amount (increases total)</li>
 *   <li>ADDITIONAL_CHARGE: Positive amount (increases total)</li>
 *   <li>REFUND: Negative amount (reduces total)</li>
 * </ul>
 *
 * @author KiteClass Team
 * @since 2.8.0
 */
@Entity
@Table(
        name = "invoice_adjustments",
        indexes = {
                @Index(name = "idx_adjustments_invoice", columnList = "invoice_id"),
                @Index(name = "idx_adjustments_type", columnList = "type")
        }
)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InvoiceAdjustment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Parent invoice.
     * Required, adjustment cannot exist without invoice.
     */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "invoice_id", nullable = false)
    private Invoice invoice;

    /**
     * Adjustment type.
     * Determines if amount is positive (fee) or negative (discount).
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false, length = 50)
    private InvoiceAdjustmentType type;

    /**
     * Adjustment description.
     * Required, displayed on invoice.
     */
    @Column(name = "description", nullable = false, length = 255)
    private String description;

    /**
     * Adjustment amount.
     * Positive for fees/charges, negative for discounts/refunds.
     */
    @Column(name = "amount", nullable = false, precision = 10, scale = 2)
    private BigDecimal amount;

    /**
     * Reason for adjustment.
     * Optional, for audit trail.
     */
    @Column(name = "reason", columnDefinition = "TEXT")
    private String reason;

    /**
     * Timestamp when adjustment was created.
     */
    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;
}
