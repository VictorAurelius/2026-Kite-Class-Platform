package com.kiteclass.core.module.invoice.entity;

import com.kiteclass.core.common.constant.InvoiceItemType;
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
 * Invoice item (line item) representing a charge on an invoice.
 *
 * <p>Each invoice can have multiple items such as:
 * <ul>
 *   <li>Tuition fees</li>
 *   <li>Course materials</li>
 *   <li>Registration fees</li>
 *   <li>Exam fees</li>
 * </ul>
 *
 * @author KiteClass Team
 * @since 2.8.0
 */
@Entity
@Table(
        name = "invoice_items",
        indexes = {
                @Index(name = "idx_invoice_items_invoice", columnList = "invoice_id")
        }
)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InvoiceItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Parent invoice.
     * Required, invoice item cannot exist without invoice.
     */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "invoice_id", nullable = false)
    private Invoice invoice;

    /**
     * Item type (TUITION, MATERIALS, etc.).
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "item_type", length = 50)
    private InvoiceItemType type;

    /**
     * Item description.
     * Required, displayed on invoice.
     */
    @Column(name = "description", nullable = false, length = 255)
    private String description;

    /**
     * Quantity of items.
     * Defaults to 1.
     */
    @Column(name = "quantity")
    @Builder.Default
    private Integer quantity = 1;

    /**
     * Unit price per item.
     * Required, must be non-negative.
     */
    @Column(name = "unit_price", nullable = false, precision = 12, scale = 2)
    private BigDecimal unitPrice;

    /**
     * Total amount for this line item.
     * Typically quantity * unit_price.
     * Required, must be non-negative.
     */
    @Column(name = "amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal amount;

    /**
     * Reference ID for tracking.
     * Optional, can link to class_id, session_id, etc.
     */
    @Column(name = "reference_id")
    private Long referenceId;

    /**
     * Timestamp when item was created.
     */
    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;
}
