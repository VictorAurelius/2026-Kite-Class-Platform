package com.kiteclass.core.testutil;

import com.kiteclass.core.common.constant.InvoiceAdjustmentType;
import com.kiteclass.core.common.constant.InvoiceItemType;
import com.kiteclass.core.common.constant.InvoiceStatus;
import com.kiteclass.core.module.invoice.entity.Invoice;
import com.kiteclass.core.module.invoice.entity.InvoiceAdjustment;
import com.kiteclass.core.module.invoice.entity.InvoiceItem;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Test data builder for Invoice-related objects.
 *
 * <p>Provides factory methods to create test data for:
 * <ul>
 *   <li>Invoice entities</li>
 *   <li>InvoiceItem entities</li>
 *   <li>InvoiceAdjustment entities</li>
 * </ul>
 *
 * <p>Closes GAP-745: invoice number + id are generated per-call from a static
 * counter so that @SpringBootTest integration tests sharing the same Testcontainer
 * DB don't violate the {@code uk_invoices_instance_number} unique constraint when
 * multiple tests in the same suite persist invoices.
 *
 * @author KiteClass Team
 * @since 2.8.0
 */
public class InvoiceTestDataBuilder {

    public static final UUID DEFAULT_TENANT = ClassTestDataBuilder.DEFAULT_TENANT;

    private static final AtomicLong INVOICE_COUNTER = new AtomicLong(0);

    private static String nextInvoiceNumber() {
        return String.format("INV-2026-%06d", INVOICE_COUNTER.incrementAndGet());
    }

    private static long nextInvoiceId() {
        return INVOICE_COUNTER.get();
    }

    /**
     * Creates a default Invoice entity for testing.
     *
     * @return Invoice with default test data
     */
    public static Invoice createDefaultInvoice() {
        String number = nextInvoiceNumber();
        long id = nextInvoiceId();
        Invoice invoice = Invoice.builder()
                .invoiceNumber(number)
                .studentId(1L)
                .classId(1L)
                .enrollmentId(1L)
                .status(InvoiceStatus.SENT)
                .issueDate(LocalDate.now())
                .dueDate(LocalDate.now().plusDays(7))
                .periodStart(LocalDate.now())
                .periodEnd(LocalDate.now().plusMonths(3))
                .build();
        invoice.setId(id);
        invoice.setInstanceId(DEFAULT_TENANT);
        invoice.setDeleted(false);

        // Add default tuition item
        InvoiceItem item = createDefaultItem();
        invoice.addItem(item);

        return invoice;
    }

    /**
     * Creates an Invoice entity with custom student and enrollment IDs.
     *
     * @param studentId the student ID
     * @param enrollmentId the enrollment ID
     * @return Invoice with specified IDs
     */
    public static Invoice createInvoice(Long studentId, Long enrollmentId) {
        Invoice invoice = createDefaultInvoice();
        invoice.setStudentId(studentId);
        invoice.setEnrollmentId(enrollmentId);
        return invoice;
    }

    /**
     * Creates an Invoice entity with discount applied.
     *
     * @param tuition the tuition amount
     * @param discountPercent the discount percentage (0-100)
     * @return Invoice with discount
     */
    public static Invoice createInvoiceWithDiscount(BigDecimal tuition, BigDecimal discountPercent) {
        String number = nextInvoiceNumber();
        long id = nextInvoiceId();
        Invoice invoice = Invoice.builder()
                .invoiceNumber(number)
                .studentId(1L)
                .classId(1L)
                .enrollmentId(1L)
                .status(InvoiceStatus.SENT)
                .issueDate(LocalDate.now())
                .dueDate(LocalDate.now().plusDays(7))
                .periodStart(LocalDate.now())
                .periodEnd(LocalDate.now().plusMonths(3))
                .build();
        invoice.setId(id);
        invoice.setInstanceId(DEFAULT_TENANT);

        // Add tuition item
        InvoiceItem item = InvoiceItem.builder()
                .type(InvoiceItemType.TUITION)
                .description("Học phí khóa học")
                .quantity(1)
                .unitPrice(tuition)
                .amount(tuition)
                .build();
        invoice.addItem(item);

        // Add discount adjustment
        BigDecimal discountAmount = tuition
                .multiply(discountPercent)
                .divide(new BigDecimal("100"), 2, RoundingMode.HALF_UP)
                .negate(); // Negative for discount

        InvoiceAdjustment discount = InvoiceAdjustment.builder()
                .type(InvoiceAdjustmentType.DISCOUNT)
                .description("Giảm giá " + discountPercent + "%")
                .amount(discountAmount)
                .reason("Enrollment discount")
                .build();
        invoice.addAdjustment(discount);

        return invoice;
    }

    /**
     * Creates an overdue Invoice entity.
     *
     * @return Invoice with past due date
     */
    public static Invoice createOverdueInvoice() {
        Invoice invoice = createDefaultInvoice();
        // Note: createDefaultInvoice already assigned unique number + id; no override needed.
        invoice.setDueDate(LocalDate.now().minusDays(10)); // 10 days overdue
        invoice.setStatus(InvoiceStatus.OVERDUE);
        return invoice;
    }

    /**
     * Creates a paid Invoice entity.
     *
     * @return Invoice with PAID status
     */
    public static Invoice createPaidInvoice() {
        Invoice invoice = createDefaultInvoice();
        // Note: createDefaultInvoice already assigned unique number + id; no override needed.
        invoice.setStatus(InvoiceStatus.PAID);
        invoice.setAmountPaid(new BigDecimal("1000.00"));
        return invoice;
    }

    /**
     * Creates a default InvoiceItem entity.
     *
     * @return InvoiceItem with default test data
     */
    public static InvoiceItem createDefaultItem() {
        return InvoiceItem.builder()
                .type(InvoiceItemType.TUITION)
                .description("Học phí khóa học")
                .quantity(1)
                .unitPrice(new BigDecimal("1000.00"))
                .amount(new BigDecimal("1000.00"))
                .build();
    }

    /**
     * Creates an InvoiceItem with custom amount.
     *
     * @param amount the item amount
     * @return InvoiceItem with specified amount
     */
    public static InvoiceItem createItem(BigDecimal amount) {
        return InvoiceItem.builder()
                .type(InvoiceItemType.TUITION)
                .description("Học phí")
                .quantity(1)
                .unitPrice(amount)
                .amount(amount)
                .build();
    }

    /**
     * Creates a materials fee InvoiceItem.
     *
     * @return InvoiceItem for materials
     */
    public static InvoiceItem createMaterialsItem() {
        return InvoiceItem.builder()
                .type(InvoiceItemType.MATERIALS)
                .description("Tài liệu học tập")
                .quantity(1)
                .unitPrice(new BigDecimal("100.00"))
                .amount(new BigDecimal("100.00"))
                .build();
    }

    /**
     * Creates a discount InvoiceAdjustment.
     *
     * @param discountPercent the discount percentage
     * @param baseAmount the amount to apply discount to
     * @return InvoiceAdjustment for discount
     */
    public static InvoiceAdjustment createDiscountAdjustment(
            BigDecimal discountPercent, BigDecimal baseAmount) {

        BigDecimal discountAmount = baseAmount
                .multiply(discountPercent)
                .divide(new BigDecimal("100"), 2, RoundingMode.HALF_UP)
                .negate(); // Negative

        return InvoiceAdjustment.builder()
                .type(InvoiceAdjustmentType.DISCOUNT)
                .description("Giảm giá " + discountPercent + "%")
                .amount(discountAmount)
                .reason("Test discount")
                .build();
    }

    /**
     * Creates a late fee InvoiceAdjustment.
     *
     * @param lateFeeAmount the late fee amount
     * @return InvoiceAdjustment for late fee
     */
    public static InvoiceAdjustment createLateFeeAdjustment(BigDecimal lateFeeAmount) {
        return InvoiceAdjustment.builder()
                .type(InvoiceAdjustmentType.LATE_FEE)
                .description("Phí trễ hạn")
                .amount(lateFeeAmount)
                .reason("Late payment")
                .build();
    }
}
