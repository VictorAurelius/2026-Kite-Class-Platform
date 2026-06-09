package com.kiteclass.core.module.invoice.service;

import com.kiteclass.core.common.constant.InvoiceAdjustmentType;
import com.kiteclass.core.common.constant.InvoiceItemType;
import com.kiteclass.core.common.constant.InvoiceStatus;
import com.kiteclass.core.common.dto.PageResponse;
import com.kiteclass.core.common.exception.EntityNotFoundException;
import com.kiteclass.core.common.exception.ValidationException;
import com.kiteclass.core.module.clazz.entity.Class;
import com.kiteclass.core.module.clazz.repository.ClassRepository;
import com.kiteclass.core.module.course.entity.Course;
import com.kiteclass.core.module.course.repository.CourseRepository;
import com.kiteclass.core.module.enrollment.entity.Enrollment;
import com.kiteclass.core.module.enrollment.repository.EnrollmentRepository;
import com.kiteclass.core.module.invoice.dto.ApplyAdjustmentRequest;
import com.kiteclass.core.module.invoice.dto.InvoiceItemResponse;
import com.kiteclass.core.module.invoice.dto.InvoiceResponse;
import com.kiteclass.core.module.invoice.entity.Invoice;
import com.kiteclass.core.module.invoice.entity.InvoiceAdjustment;
import com.kiteclass.core.module.invoice.entity.InvoiceItem;
import com.kiteclass.core.module.invoice.event.InvoiceCreatedEvent;
import com.kiteclass.core.module.invoice.mapper.InvoiceMapper;
import com.kiteclass.core.module.invoice.repository.InvoiceRepository;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;


/**
 * Service implementation for invoice management.
 *
 * @author KiteClass Team
 * @since 2.8.0
 */
@Service
@RequiredArgsConstructor
@Validated
@Slf4j
public class InvoiceServiceImpl implements InvoiceService {

    private final InvoiceRepository invoiceRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final ClassRepository classRepository;
    private final CourseRepository courseRepository;
    private final InvoiceNumberGenerator invoiceNumberGenerator;
    private final InvoiceMapper invoiceMapper;
    private final ApplicationEventPublisher eventPublisher;

    /**
     * Late fee rate: 0.1% per day.
     */
    private static final BigDecimal LATE_FEE_RATE = new BigDecimal("0.001");

    @Override
    @Transactional
    public Invoice createInvoiceForEnrollment(Long enrollmentId) {
        log.info("Creating invoice for enrollment ID: {}", enrollmentId);

        // 1. Validate enrollment exists
        Enrollment enrollment = enrollmentRepository.findByIdAndDeletedFalse(enrollmentId)
                .orElseThrow(() -> new EntityNotFoundException("ENROLLMENT_NOT_FOUND", (Object) enrollmentId));

        // 2. Check invoice not already exists (BR-INV-005)
        if (invoiceRepository.existsByEnrollmentIdAndDeletedFalse(enrollmentId)) {
            throw new ValidationException("INVOICE_ALREADY_EXISTS", enrollmentId);
        }

        // 3. Get class and course info for item description
        Class clazz = classRepository.findByIdAndDeletedFalse(enrollment.getClassId())
                .orElseThrow(() -> new EntityNotFoundException("CLASS_NOT_FOUND", (Object) enrollment.getClassId()));

        Course course = courseRepository.findByIdAndDeletedFalse(clazz.getCourseId())
                .orElseThrow(() -> new EntityNotFoundException("COURSE_NOT_FOUND", (Object) clazz.getCourseId()));

        // 4. Generate invoice number (thread-safe)
        String invoiceNumber = invoiceNumberGenerator.generate(enrollment.getInstanceId());

        // 5. Build invoice
        Invoice invoice = Invoice.builder()
                .invoiceNumber(invoiceNumber)
                .studentId(enrollment.getStudentId())
                .classId(enrollment.getClassId())
                .enrollmentId(enrollment.getId())
                .status(InvoiceStatus.SENT)
                .issueDate(LocalDate.now())
                .dueDate(LocalDate.now().plusDays(7)) // 7 days payment deadline
                .periodStart(clazz.getStartDate())
                .periodEnd(clazz.getEndDate())
                .build();
        invoice.setInstanceId(enrollment.getInstanceId());

        // 6. Create tuition item
        InvoiceItem tuitionItem = InvoiceItem.builder()
                .type(InvoiceItemType.TUITION)
                .description("Học phí khóa " + course.getName())
                .quantity(1)
                .unitPrice(enrollment.getTuitionAmount())
                .amount(enrollment.getTuitionAmount())
                .build();
        invoice.addItem(tuitionItem);

        // 7. Apply discount if exists
        if (enrollment.getDiscountPercent().compareTo(BigDecimal.ZERO) > 0) {
            BigDecimal discountAmount = enrollment.getTuitionAmount()
                    .multiply(enrollment.getDiscountPercent())
                    .divide(new BigDecimal("100"), 2, RoundingMode.HALF_UP)
                    .negate(); // Negative for discount

            InvoiceAdjustment discount = InvoiceAdjustment.builder()
                    .type(InvoiceAdjustmentType.DISCOUNT)
                    .description("Giảm giá " + enrollment.getDiscountPercent() + "%")
                    .amount(discountAmount)
                    .reason("Enrollment discount")
                    .build();
            invoice.addAdjustment(discount);
        }

        // 8. Save (cascade saves items/adjustments, @PrePersist calculates totals)
        Invoice saved = invoiceRepository.save(invoice);

        log.info("Auto-created invoice {} for enrollment {}, total: {}",
                saved.getInvoiceNumber(), enrollmentId, saved.getTotal());

        // 9. Publish event for future Payment Module
        eventPublisher.publishEvent(new InvoiceCreatedEvent(this, saved));

        return saved;
    }

    @Override
    @Transactional(readOnly = true)
    public InvoiceResponse getInvoiceById(Long id) {
        log.debug("Fetching invoice with ID: {}", id);

        Invoice invoice = invoiceRepository.findByIdAndDeletedFalse(id)
                .orElseThrow(() -> new EntityNotFoundException("INVOICE_NOT_FOUND", (Object) id));

        return invoiceMapper.toResponse(invoice);
    }

    @Override
    @Transactional(readOnly = true)
    public List<InvoiceItemResponse> getInvoiceItems(Long invoiceId) {
        log.debug("Fetching items for invoice ID: {}", invoiceId);

        // Validate invoice exists and tenant access
        Invoice invoice = invoiceRepository.findByIdAndDeletedFalse(invoiceId)
                .orElseThrow(() -> new EntityNotFoundException("INVOICE_NOT_FOUND", (Object) invoiceId));

        // Map items to DTOs
        return invoice.getItems().stream()
                .map(invoiceMapper::toItemResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public Page<InvoiceResponse> getInvoicesByStudent(Long studentId, Pageable pageable) {
        log.debug("Fetching invoices for student ID: {}", studentId);

        Page<Invoice> invoices = invoiceRepository.findByStudentIdAndDeletedFalse(studentId, pageable);
        return invoices.map(invoiceMapper::toResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<InvoiceResponse> getInvoices(Pageable pageable) {
        log.debug("Listing all invoices (tenant-scoped): pageable={}", pageable);

        Page<Invoice> invoices = invoiceRepository.findAllByDeletedFalse(pageable);

        List<InvoiceResponse> content = invoices.getContent()
                .stream()
                .map(invoiceMapper::toResponse)
                .toList();

        return PageResponse.of(
                content,
                invoices.getNumber(),
                invoices.getSize(),
                invoices.getTotalElements());
    }

    @Override
    @Transactional
    public InvoiceResponse applyAdjustment(Long invoiceId, @Valid ApplyAdjustmentRequest request) {
        log.info("Applying adjustment to invoice {}: type={}, amount={}",
                invoiceId, request.getType(), request.getAmount());

        // Validate invoice exists and can be modified
        Invoice invoice = invoiceRepository.findByIdAndDeletedFalse(invoiceId)
                .orElseThrow(() -> new EntityNotFoundException("INVOICE_NOT_FOUND", (Object) invoiceId));

        if (invoice.getStatus() == InvoiceStatus.PAID) {
            throw new ValidationException("INVOICE_ALREADY_PAID", invoiceId);
        }

        if (invoice.getStatus() == InvoiceStatus.CANCELLED) {
            throw new ValidationException("INVOICE_CANCELLED", invoiceId);
        }

        // Create adjustment
        InvoiceAdjustment adjustment = InvoiceAdjustment.builder()
                .type(request.getType())
                .description(request.getDescription())
                .amount(request.getAmount())
                .reason(request.getReason())
                .build();

        // Add to invoice (triggers @PreUpdate calculation)
        invoice.addAdjustment(adjustment);

        // Save
        Invoice saved = invoiceRepository.save(invoice);

        log.info("Applied adjustment to invoice {}, new total: {}", invoiceId, saved.getTotal());

        return invoiceMapper.toResponse(saved);
    }

    @Override
    @Transactional
    public InvoiceResponse calculateLateFees(Long invoiceId) {
        log.info("Calculating late fees for invoice {}", invoiceId);

        // Validate invoice exists and is overdue
        Invoice invoice = invoiceRepository.findByIdAndDeletedFalse(invoiceId)
                .orElseThrow(() -> new EntityNotFoundException("INVOICE_NOT_FOUND", (Object) invoiceId));

        if (invoice.getStatus() == InvoiceStatus.PAID) {
            throw new ValidationException("INVOICE_ALREADY_PAID", invoiceId);
        }

        if (invoice.getDueDate() == null || !LocalDate.now().isAfter(invoice.getDueDate())) {
            throw new ValidationException("INVOICE_NOT_OVERDUE", invoiceId);
        }

        // Calculate days overdue
        long daysOverdue = ChronoUnit.DAYS.between(invoice.getDueDate(), LocalDate.now());

        // Calculate late fee: 0.1% per day * original total
        BigDecimal lateFee = invoice.getTotal()
                .multiply(LATE_FEE_RATE)
                .multiply(BigDecimal.valueOf(daysOverdue))
                .setScale(2, RoundingMode.HALF_UP);

        // Create late fee adjustment
        InvoiceAdjustment adjustment = InvoiceAdjustment.builder()
                .type(InvoiceAdjustmentType.LATE_FEE)
                .description("Phí trễ hạn " + daysOverdue + " ngày")
                .amount(lateFee)
                .reason("Late payment fee: " + daysOverdue + " days overdue")
                .build();

        invoice.addAdjustment(adjustment);

        // Status will be updated to OVERDUE by @PreUpdate calculateTotals()
        Invoice saved = invoiceRepository.save(invoice);

        log.info("Applied late fee {} to invoice {}, new total: {}",
                lateFee, invoiceId, saved.getTotal());

        return invoiceMapper.toResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<InvoiceResponse> getOverdueInvoices(Pageable pageable) {
        log.debug("Fetching overdue invoices");

        Page<Invoice> overdueInvoices = invoiceRepository.findOverdueInvoices(
                LocalDate.now(), pageable);

        return overdueInvoices.map(invoiceMapper::toResponse);
    }

    @Override
    @Transactional
    public InvoiceResponse cancelInvoice(Long id) {
        log.info("Cancelling invoice {}", id);

        Invoice invoice = invoiceRepository.findByIdAndDeletedFalse(id)
                .orElseThrow(() -> new EntityNotFoundException("INVOICE_NOT_FOUND", (Object) id));

        if (!invoice.canCancel()) {
            throw new ValidationException("INVOICE_CANNOT_CANCEL", id);
        }

        invoice.setStatus(InvoiceStatus.CANCELLED);
        Invoice saved = invoiceRepository.save(invoice);

        log.info("Cancelled invoice {}", id);

        return invoiceMapper.toResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<InvoiceResponse> getUnpaidInvoicesByStudent(Long studentId, Pageable pageable) {
        log.debug("Fetching unpaid invoices for student ID: {}", studentId);

        Page<Invoice> unpaidInvoices = invoiceRepository.findUnpaidByStudentId(studentId, pageable);
        return unpaidInvoices.map(invoiceMapper::toResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<InvoiceResponse> getOverdueInvoicesByStudent(Long studentId, Pageable pageable) {
        log.debug("Fetching overdue invoices for student ID: {}", studentId);

        Page<Invoice> overdueInvoices = invoiceRepository.findOverdueByStudentId(
                studentId, LocalDate.now(), pageable);

        return overdueInvoices.map(invoiceMapper::toResponse);
    }

    @Override
    @Transactional
    public InvoiceResponse markInvoiceAsPaid(Long id) {
        log.info("Marking invoice {} as paid", id);

        // 1. Validate invoice exists
        Invoice invoice = invoiceRepository.findByIdAndDeletedFalse(id)
                .orElseThrow(() -> new EntityNotFoundException("INVOICE_NOT_FOUND", (Object) id));

        // 2. Check not already paid
        if (invoice.getStatus() == InvoiceStatus.PAID) {
            throw new ValidationException("INVOICE_ALREADY_PAID", id);
        }

        // 3. Check not cancelled
        if (invoice.getStatus() == InvoiceStatus.CANCELLED) {
            throw new ValidationException("INVOICE_CANCELLED", id);
        }

        // 4. Update status and payment tracking
        invoice.setStatus(InvoiceStatus.PAID);
        invoice.setAmountPaid(invoice.getTotal());
        invoice.setPaidAt(LocalDateTime.now());

        // 5. Save
        Invoice saved = invoiceRepository.save(invoice);

        log.info("Marked invoice {} as paid, total: {}", id, saved.getTotal());

        return invoiceMapper.toResponse(saved);
    }
}
