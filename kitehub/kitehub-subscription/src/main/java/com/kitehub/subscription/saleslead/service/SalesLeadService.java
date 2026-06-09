package com.kitehub.subscription.saleslead.service;

import com.kitehub.subscription.saleslead.dto.CreateSalesLeadRequest;
import com.kitehub.subscription.saleslead.entity.SalesLead;
import com.kitehub.subscription.saleslead.repository.SalesLeadRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Domain service for KiteHub PLATFORM sales lead persistence (GAP-1101).
 *
 * <p>Single responsibility: validate-then-persist a {@link SalesLead}. Honeypot
 * + Bean-Validation gate at the controller layer; this service trusts the DTO
 * has already been validated. Mirrors {@code FeedbackService} precedent
 * (log-only side-effect — no separate audit transaction, so no
 * {@code audit-service-isolation.md} REQUIRES_NEW concern).</p>
 *
 * @since GAP-1101
 */
@Service
@Slf4j
public class SalesLeadService {

    static final String DEFAULT_PLAN_INTEREST = "ENTERPRISE";

    private final SalesLeadRepository repository;

    public SalesLeadService(SalesLeadRepository repository) {
        this.repository = repository;
    }

    /**
     * Persist a submitted sales lead. Trims input + defaults
     * {@code planInterest} to ENTERPRISE when absent.
     *
     * @param request  validated submit payload
     * @param clientIp originating IP (forensic audit), nullable
     * @return persisted entity
     */
    @Transactional
    public SalesLead submit(CreateSalesLeadRequest request, String clientIp) {
        String fullName = request.fullName() == null ? null : request.fullName().trim();
        String email = request.email() == null ? null : request.email().trim();
        String phone = request.phone() == null ? null : request.phone().trim();
        String organizationName = request.organizationName() == null
                ? null
                : request.organizationName().trim();
        String message = (request.message() == null || request.message().isBlank())
                ? null
                : request.message().trim();
        String planInterest = (request.planInterest() == null || request.planInterest().isBlank())
                ? DEFAULT_PLAN_INTEREST
                : request.planInterest().trim();

        SalesLead entity = SalesLead.builder()
                .publicId(UUID.randomUUID())
                .fullName(fullName)
                .email(email)
                .phone(phone)
                .organizationName(organizationName)
                .message(message)
                .planInterest(planInterest)
                .status("NEW")
                .clientIp(clientIp)
                .createdAt(OffsetDateTime.now())
                .updatedAt(OffsetDateTime.now())
                .build();

        SalesLead saved = repository.save(entity);
        log.info("Sales lead submitted publicId={} planInterest={} hasMessage={}",
                saved.getPublicId(),
                saved.getPlanInterest(),
                message != null);
        return saved;
    }
}
