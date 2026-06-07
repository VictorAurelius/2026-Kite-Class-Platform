package com.kiteclass.core.module.invoice.service;

import com.kiteclass.core.common.constant.EnrollmentStatus;
import com.kiteclass.core.common.constant.InvoiceAdjustmentType;
import com.kiteclass.core.common.constant.InvoiceItemType;
import com.kiteclass.core.common.constant.InvoiceStatus;
import com.kiteclass.core.common.context.TenantContext;
import com.kiteclass.core.common.exception.ValidationException;
import com.kiteclass.core.module.clazz.entity.Class;
import com.kiteclass.core.module.clazz.repository.ClassRepository;
import com.kiteclass.core.module.enrollment.entity.Enrollment;
import com.kiteclass.core.module.enrollment.repository.EnrollmentRepository;
import com.kiteclass.core.module.invoice.dto.BatchInvoiceConfirmResponse;
import com.kiteclass.core.module.invoice.dto.BatchInvoiceLineItem;
import com.kiteclass.core.module.invoice.dto.BatchInvoicePreviewResponse;
import com.kiteclass.core.module.invoice.entity.Invoice;
import com.kiteclass.core.module.invoice.entity.InvoiceAdjustment;
import com.kiteclass.core.module.invoice.entity.InvoiceItem;
import com.kiteclass.core.module.invoice.event.InvoiceCreatedEvent;
import com.kiteclass.core.module.invoice.repository.InvoiceRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.DateTimeException;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Implementation of batch monthly invoice generation (GAP-297).
 *
 * <p>Enumerates active enrollments for the current tenant, applies mid-month
 * pro-rata + enrollment discount, and persists one invoice per enrollment per
 * month. Idempotency is enforced both at the service layer (skip already-invoiced
 * enrollments) and at the DB layer (unique constraint
 * {@code uk_invoices_enrollment_month}).
 *
 * @author KiteClass Team
 * @since GAP-297
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class InvoiceBatchServiceImpl implements InvoiceBatchService {

    private static final BigDecimal HUNDRED = new BigDecimal("100");
    /** Number of days after the billing month start that the invoice is due. */
    private static final int DUE_DAYS_FROM_MONTH_START = 7;

    private final EnrollmentRepository enrollmentRepository;
    private final InvoiceRepository invoiceRepository;
    private final ClassRepository classRepository;
    private final InvoiceNumberGenerator invoiceNumberGenerator;
    private final ApplicationEventPublisher eventPublisher;

    @Override
    @Transactional(readOnly = true)
    public BatchInvoicePreviewResponse generatePreview(String month) {
        UUID tenantId = TenantContext.getCurrentTenant();
        YearMonth ym = parseMonth(month);

        List<Enrollment> enrollments = billableEnrollments(tenantId, ym);
        Map<Long, String> classNames = classNames(enrollments);

        List<BatchInvoiceLineItem> lines = enrollments.stream()
                .map(e -> computeLineItem(e, ym, classNames))
                .toList();

        BigDecimal totalRevenue = lines.stream()
                .map(BatchInvoiceLineItem::total)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(2, RoundingMode.HALF_UP);

        log.info("Batch invoice preview for tenant {} month {}: {} invoices, total {}",
                tenantId, ym, lines.size(), totalRevenue);

        return new BatchInvoicePreviewResponse(ym.toString(), lines.size(), totalRevenue, lines);
    }

    @Override
    @Transactional
    public BatchInvoiceConfirmResponse confirm(String month) {
        UUID tenantId = TenantContext.getCurrentTenant();
        YearMonth ym = parseMonth(month);
        LocalDate monthStart = ym.atDay(1);
        LocalDate monthEnd = ym.atEndOfMonth();

        List<Enrollment> enrollments = billableEnrollments(tenantId, ym);
        Map<Long, String> classNames = classNames(enrollments);

        List<Long> createdIds = new ArrayList<>();
        BigDecimal totalRevenue = BigDecimal.ZERO;
        int skipped = 0;

        for (Enrollment enrollment : enrollments) {
            // Idempotency: skip enrollments already invoiced for this month.
            if (invoiceRepository.existsByInstanceIdAndEnrollmentIdAndBillingMonthAndDeletedFalse(
                    tenantId, enrollment.getId(), monthStart)) {
                skipped++;
                continue;
            }

            BatchInvoiceLineItem line = computeLineItem(enrollment, ym, classNames);

            Invoice invoice = Invoice.builder()
                    .invoiceNumber(invoiceNumberGenerator.generate(tenantId))
                    .studentId(enrollment.getStudentId())
                    .classId(enrollment.getClassId())
                    .enrollmentId(enrollment.getId())
                    .billingMonth(monthStart)
                    .status(InvoiceStatus.SENT)
                    .issueDate(LocalDate.now())
                    .dueDate(monthStart.plusDays(DUE_DAYS_FROM_MONTH_START))
                    .periodStart(monthStart)
                    .periodEnd(monthEnd)
                    .build();
            invoice.setInstanceId(tenantId);

            InvoiceItem tuitionItem = InvoiceItem.builder()
                    .type(InvoiceItemType.TUITION)
                    .description(itemDescription(line.classNameVi(), ym))
                    .quantity(1)
                    .unitPrice(line.proratedTuition())
                    .amount(line.proratedTuition())
                    .build();
            invoice.addItem(tuitionItem);

            if (line.discountAmount().compareTo(BigDecimal.ZERO) > 0) {
                InvoiceAdjustment discount = InvoiceAdjustment.builder()
                        .type(InvoiceAdjustmentType.DISCOUNT)
                        .description("Giảm giá " + line.discountPercent() + "%")
                        .amount(line.discountAmount().negate()) // negative reduces total
                        .reason("Enrollment discount")
                        .build();
                invoice.addAdjustment(discount);
            }

            Invoice saved = invoiceRepository.save(invoice);
            eventPublisher.publishEvent(new InvoiceCreatedEvent(this, saved));

            createdIds.add(saved.getId());
            totalRevenue = totalRevenue.add(saved.getTotal());
        }

        totalRevenue = totalRevenue.setScale(2, RoundingMode.HALF_UP);
        log.info("Batch invoice confirm for tenant {} month {}: {} created, {} skipped, total {}",
                tenantId, ym, createdIds.size(), skipped, totalRevenue);

        return new BatchInvoiceConfirmResponse(
                ym.toString(), createdIds.size(), skipped, totalRevenue, createdIds);
    }

    // ===================== helpers =====================

    /**
     * Parses {@code yyyy-MM} into a {@link YearMonth}, throwing a friendly
     * validation error on bad input.
     */
    private YearMonth parseMonth(String month) {
        if (month == null || month.isBlank()) {
            throw new ValidationException("INVALID_MONTH_FORMAT", "<blank>");
        }
        try {
            return YearMonth.parse(month.trim());
        } catch (DateTimeException ex) {
            throw new ValidationException("INVALID_MONTH_FORMAT", month);
        }
    }

    /**
     * Active enrollments for the tenant that existed during the billed month
     * (enrolled on or before month end).
     */
    private List<Enrollment> billableEnrollments(UUID tenantId, YearMonth ym) {
        LocalDate monthEnd = ym.atEndOfMonth();
        return enrollmentRepository
                .findByInstanceIdAndStatusAndDeletedFalse(tenantId, EnrollmentStatus.ACTIVE)
                .stream()
                .filter(e -> e.getEnrollmentDate() != null
                        && !e.getEnrollmentDate().toLocalDate().isAfter(monthEnd))
                .toList();
    }

    /** Batch-fetch class names for the given enrollments to avoid N+1 lookups. */
    private Map<Long, String> classNames(List<Enrollment> enrollments) {
        List<Long> classIds = enrollments.stream()
                .map(Enrollment::getClassId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        if (classIds.isEmpty()) {
            return Map.of();
        }
        return classRepository.findAllById(classIds).stream()
                .collect(Collectors.toMap(Class::getId, Class::getName, (a, b) -> a));
    }

    /**
     * Computes the projected invoice line for one enrollment in the given month.
     *
     * <p>Pro-rata: enrollments that started mid-month are billed for the remaining
     * days inclusive of the enrollment day —
     * {@code prorated = tuition * billableDays / daysInMonth}. The enrollment
     * discount is then applied on the prorated tuition.
     */
    private BatchInvoiceLineItem computeLineItem(Enrollment e, YearMonth ym, Map<Long, String> classNames) {
        LocalDate monthStart = ym.atDay(1);
        int daysInMonth = ym.lengthOfMonth();
        LocalDate enrollDate = e.getEnrollmentDate().toLocalDate();

        boolean prorated;
        int billableDays;
        if (!enrollDate.isAfter(monthStart)) {
            // Enrolled on/before the first of the month → full month.
            prorated = false;
            billableDays = daysInMonth;
        } else {
            // Enrolled mid-month → bill remaining days (inclusive of enrollment day).
            prorated = true;
            billableDays = daysInMonth - enrollDate.getDayOfMonth() + 1;
        }

        BigDecimal tuition = e.getTuitionAmount().setScale(2, RoundingMode.HALF_UP);
        BigDecimal proratedTuition = prorated
                ? tuition.multiply(BigDecimal.valueOf(billableDays))
                        .divide(BigDecimal.valueOf(daysInMonth), 2, RoundingMode.HALF_UP)
                : tuition;

        BigDecimal discountPercent = e.getDiscountPercent() == null
                ? BigDecimal.ZERO : e.getDiscountPercent();
        BigDecimal discountAmount = proratedTuition.multiply(discountPercent)
                .divide(HUNDRED, 2, RoundingMode.HALF_UP);
        BigDecimal total = proratedTuition.subtract(discountAmount).setScale(2, RoundingMode.HALF_UP);

        return new BatchInvoiceLineItem(
                e.getId(),
                e.getStudentId(),
                e.getClassId(),
                e.getClassId() == null ? null : classNames.get(e.getClassId()),
                tuition,
                discountPercent,
                proratedTuition,
                discountAmount,
                total,
                prorated,
                billableDays,
                daysInMonth);
    }

    /** Vietnamese tuition line description, e.g. "Học phí khóa Toán 6A - tháng 05/2026". */
    private String itemDescription(String classNameVi, YearMonth ym) {
        String monthLabel = String.format("%02d/%d", ym.getMonthValue(), ym.getYear());
        return classNameVi != null
                ? "Học phí khóa " + classNameVi + " - tháng " + monthLabel
                : "Học phí tháng " + monthLabel;
    }
}
