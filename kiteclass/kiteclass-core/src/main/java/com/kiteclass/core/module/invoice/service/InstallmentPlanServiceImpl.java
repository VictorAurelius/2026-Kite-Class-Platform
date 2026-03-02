package com.kiteclass.core.module.invoice.service;

import com.kiteclass.core.common.constant.InstallmentPlanStatus;
import com.kiteclass.core.common.constant.InstallmentStatus;
import com.kiteclass.core.common.exception.EntityNotFoundException;
import com.kiteclass.core.common.exception.ValidationException;
import com.kiteclass.core.module.invoice.dto.CreateInstallmentPlanRequest;
import com.kiteclass.core.module.invoice.dto.InstallmentPlanResponse;
import com.kiteclass.core.module.invoice.entity.Installment;
import com.kiteclass.core.module.invoice.entity.InstallmentPlan;
import com.kiteclass.core.module.invoice.entity.Invoice;
import com.kiteclass.core.module.invoice.mapper.InvoiceMapper;
import com.kiteclass.core.module.invoice.repository.InstallmentPlanRepository;
import com.kiteclass.core.module.invoice.repository.InvoiceRepository;
import jakarta.persistence.EntityManager;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;

/**
 * Service implementation for installment plan management.
 *
 * @author KiteClass Team
 * @since 2.8.0
 */
@Service
@RequiredArgsConstructor
@Validated
@Slf4j
public class InstallmentPlanServiceImpl implements InstallmentPlanService {

    private final InstallmentPlanRepository installmentPlanRepository;
    private final InvoiceRepository invoiceRepository;
    private final InvoiceMapper invoiceMapper;
    private final EntityManager entityManager;

    @Override
    @Transactional
    public InstallmentPlanResponse requestInstallmentPlan(@Valid CreateInstallmentPlanRequest request) {
        log.info("Requesting installment plan for invoice {}: {} installments",
                request.getInvoiceId(), request.getNumberOfInstallments());

        // Validate invoice exists
        Invoice invoice = invoiceRepository.findByIdAndDeletedFalse(request.getInvoiceId())
                .orElseThrow(() -> new EntityNotFoundException("INVOICE_NOT_FOUND", request.getInvoiceId()));

        // Check plan not already exists
        if (installmentPlanRepository.existsByInvoiceIdAndDeletedFalse(request.getInvoiceId())) {
            throw new ValidationException("INSTALLMENT_PLAN_ALREADY_EXISTS", request.getInvoiceId());
        }

        // Validate invoice is not paid
        if (invoice.getStatus().isFinal()) {
            throw new ValidationException("INVOICE_ALREADY_PAID", request.getInvoiceId());
        }

        // Create plan
        InstallmentPlan plan = InstallmentPlan.builder()
                .invoiceId(request.getInvoiceId())
                .numberOfInstallments(request.getNumberOfInstallments())
                .status(InstallmentPlanStatus.PENDING)
                .build();
        plan.setInstanceId(invoice.getInstanceId());

        // Calculate installment amounts
        BigDecimal totalAmount = invoice.getBalanceDue();
        BigDecimal installmentAmount = totalAmount
                .divide(BigDecimal.valueOf(request.getNumberOfInstallments()), 2, RoundingMode.HALF_UP);

        // Create installments (30 days apart)
        LocalDate currentDueDate = invoice.getDueDate();
        for (int i = 1; i <= request.getNumberOfInstallments(); i++) {
            BigDecimal amount = installmentAmount;

            // Last installment gets the remainder to handle rounding
            if (i == request.getNumberOfInstallments()) {
                BigDecimal sumPrevious = installmentAmount.multiply(
                        BigDecimal.valueOf(request.getNumberOfInstallments() - 1));
                amount = totalAmount.subtract(sumPrevious).setScale(2, RoundingMode.HALF_UP);
            }

            Installment installment = Installment.builder()
                    .installmentNumber(i)
                    .amount(amount)
                    .dueDate(currentDueDate.plusMonths(i - 1)) // First installment uses original due date
                    .status(InstallmentStatus.PENDING)
                    .build();

            plan.addInstallment(installment);
        }

        // Save
        InstallmentPlan saved = installmentPlanRepository.save(plan);

        log.info("Created installment plan {} for invoice {}, total: {}",
                saved.getId(), request.getInvoiceId(), totalAmount);

        return invoiceMapper.toPlanResponse(saved);
    }

    @Override
    @Transactional
    public InstallmentPlanResponse approveInstallmentPlan(Long planId, Long approvedBy) {
        log.info("Approving installment plan {}", planId);

        InstallmentPlan plan = installmentPlanRepository.findByIdAndDeletedFalse(planId)
                .orElseThrow(() -> new EntityNotFoundException("INSTALLMENT_PLAN_NOT_FOUND", planId));

        plan.approve(approvedBy);
        plan.activate(); // Transition to ACTIVE

        InstallmentPlan saved = installmentPlanRepository.save(plan);

        log.info("Approved installment plan {}", planId);

        return invoiceMapper.toPlanResponse(saved);
    }

    @Override
    @Transactional
    public InstallmentPlanResponse rejectInstallmentPlan(Long planId, String reason) {
        log.info("Rejecting installment plan {}: {}", planId, reason);

        InstallmentPlan plan = installmentPlanRepository.findByIdAndDeletedFalse(planId)
                .orElseThrow(() -> new EntityNotFoundException("INSTALLMENT_PLAN_NOT_FOUND", planId));

        plan.reject(reason);

        InstallmentPlan saved = installmentPlanRepository.save(plan);

        log.info("Rejected installment plan {}", planId);

        return invoiceMapper.toPlanResponse(saved);
    }

    @Override
    @Transactional
    public InstallmentPlanResponse recordInstallmentPayment(Long installmentId, BigDecimal amount) {
        log.info("Recording payment for installment {}: amount={}", installmentId, amount);

        // Find installment (need to fetch plan first)
        InstallmentPlan plan = installmentPlanRepository.findAll().stream()
                .filter(p -> p.getInstallments().stream()
                        .anyMatch(i -> i.getId().equals(installmentId)))
                .findFirst()
                .orElseThrow(() -> new EntityNotFoundException("INSTALLMENT_NOT_FOUND", installmentId));

        Installment installment = plan.getInstallments().stream()
                .filter(i -> i.getId().equals(installmentId))
                .findFirst()
                .orElseThrow(() -> new EntityNotFoundException("INSTALLMENT_NOT_FOUND", installmentId));

        // Record payment
        installment.recordPayment(amount);

        // Check if all installments paid
        boolean allPaid = plan.getInstallments().stream()
                .allMatch(Installment::isFullyPaid);

        if (allPaid) {
            plan.setStatus(InstallmentPlanStatus.COMPLETED);
        }

        InstallmentPlan saved = installmentPlanRepository.save(plan);

        log.info("Recorded payment for installment {}, plan status: {}",
                installmentId, saved.getStatus());

        return invoiceMapper.toPlanResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public InstallmentPlanResponse getInstallmentPlanById(Long id) {
        log.debug("Fetching installment plan with ID: {}", id);

        InstallmentPlan plan = installmentPlanRepository.findByIdAndDeletedFalse(id)
                .orElseThrow(() -> new EntityNotFoundException("INSTALLMENT_PLAN_NOT_FOUND", id));

        return invoiceMapper.toPlanResponse(plan);
    }

    @Override
    @Transactional(readOnly = true)
    public InstallmentPlanResponse getInstallmentPlanByInvoiceId(Long invoiceId) {
        log.debug("Fetching installment plan for invoice ID: {}", invoiceId);

        InstallmentPlan plan = installmentPlanRepository.findByInvoiceIdAndDeletedFalse(invoiceId)
                .orElseThrow(() -> new EntityNotFoundException("INSTALLMENT_PLAN_NOT_FOUND"));

        return invoiceMapper.toPlanResponse(plan);
    }
}
