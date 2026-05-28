package com.kiteclass.core.module.marketing.service.impl;

import com.kiteclass.core.common.context.TenantContext;
import com.kiteclass.core.common.dto.PageResponse;
import com.kiteclass.core.common.exception.EntityNotFoundException;
import com.kiteclass.core.common.exception.ValidationException;
import com.kiteclass.core.common.service.email.EmailService;
import com.kiteclass.core.module.marketing.dto.request.CreateLeadRequest;
import com.kiteclass.core.module.marketing.dto.request.UpdateLeadRequest;
import com.kiteclass.core.module.marketing.dto.response.LeadResponse;
import com.kiteclass.core.module.marketing.entity.Lead;
import com.kiteclass.core.module.marketing.enums.LeadStatus;
import com.kiteclass.core.module.marketing.mapper.LeadMapper;
import com.kiteclass.core.module.marketing.repository.LeadRepository;
import com.kiteclass.core.module.marketing.service.LeadService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * Implementation of LeadService interface.
 *
 * <p>Handles lead management with multi-tenant isolation and business rules.
 *
 * @since 2.10
 */
@Slf4j
@Service
@RequiredArgsConstructor
@org.springframework.validation.annotation.Validated
public class LeadServiceImpl implements LeadService {

    private final LeadRepository leadRepository;
    private final LeadMapper leadMapper;
    private final EmailService emailService;

    /**
     * Creates a new lead from trial registration.
     *
     * <p>Implements BR-MKT-002: Validates email uniqueness within tenant.
     *
     * @param request  the create request with lead details
     * @param tenantId the tenant ID
     * @return LeadResponse with created lead data
     * @throws ValidationException if email already exists within tenant
     */
    @Override
    @Transactional
    @CacheEvict(value = "leads", allEntries = true)
    public LeadResponse createLead(CreateLeadRequest request, UUID tenantId) {
        log.info("Creating lead with email: {}, tenantId: {}", request.getEmail(), tenantId);

        // BR-MKT-002: Validate email uniqueness within tenant
        if (leadRepository.findByEmailAndInstanceIdAndDeletedFalse(request.getEmail(), tenantId).isPresent()) {
            log.warn("Duplicate lead email within tenant: {}, tenantId: {}", request.getEmail(), tenantId);
            throw new ValidationException("LEAD_EMAIL_ALREADY_EXISTS", (Object) request.getEmail());
        }

        Lead lead = leadMapper.toEntity(request);

        // CRITICAL: Set instanceId for multi-tenant isolation
        lead.setInstanceId(tenantId);

        Lead saved = leadRepository.save(lead);

        // BR-MKT-004: Send confirmation email to lead
        try {
            emailService.sendLeadConfirmation(request.getEmail(), request.getName());
            log.info("Sent confirmation email to lead: {}", request.getEmail());
        } catch (Exception e) {
            // Don't fail lead creation if email fails
            log.error("Failed to send lead confirmation email: {}", e.getMessage(), e);
        }

        log.info("Created lead with ID: {}, instanceId: {}", saved.getId(), saved.getInstanceId());
        return leadMapper.toResponse(saved);
    }

    /**
     * Retrieves a lead by ID.
     *
     * <p>Result is cached in Redis with key "leads::{id}".
     *
     * @param id the lead ID
     * @return LeadResponse with lead data
     * @throws EntityNotFoundException if lead not found
     */
    @Override
    @Transactional(readOnly = true)
    // GAP-792 cross-flow sweep — cache key MUST include tenant. Lead PKs come from a shared
    // global sequence; key="#id" alone causes cross-tenant cache pollution (tenant B fetching
    // lead 5 gets tenant A's cached payload before the tenant-scoped DB query runs). The
    // matching @CacheEvict keys below use the same tenant-scoped expression.
    @Cacheable(value = "leads", key = "T(com.kiteclass.core.common.context.TenantContext).getCurrentTenant() + ':' + #id")
    public LeadResponse getLeadById(Long id) {
        log.debug("Fetching lead with ID: {}", id);

        UUID tenantId = TenantContext.getCurrentTenant();

        Lead lead = leadRepository.findByIdAndInstanceIdAndDeletedFalse(id, tenantId)
                .orElseThrow(() -> {
                    log.warn("Lead not found with ID: {}", id);
                    return new EntityNotFoundException("LEAD_NOT_FOUND", (Object) id);
                });

        return leadMapper.toResponse(lead);
    }

    /**
     * Searches leads with filters and pagination.
     *
     * @param status   the lead status filter (can be null)
     * @param pageable pagination parameters
     * @return PageResponse with matching leads
     */
    @Override
    @Transactional(readOnly = true)
    public PageResponse<LeadResponse> getLeads(LeadStatus status, Pageable pageable) {
        UUID tenantId = TenantContext.getCurrentTenant();

        log.debug("Searching leads with status='{}', tenantId={}, page={}", status, tenantId, pageable.getPageNumber());

        Page<Lead> leadPage;

        if (status != null) {
            leadPage = leadRepository.findByInstanceIdAndStatusAndDeletedFalse(tenantId, status, pageable);
        } else {
            leadPage = leadRepository.findByInstanceIdAndDeletedFalse(tenantId, pageable);
        }

        Page<LeadResponse> responsePage = leadPage.map(leadMapper::toResponse);

        return PageResponse.from(responsePage);
    }

    /**
     * Updates lead status.
     *
     * @param id        the lead ID
     * @param newStatus the new status
     * @return LeadResponse with updated lead data
     * @throws EntityNotFoundException if lead not found
     */
    @Override
    @Transactional
    // GAP-792 cross-flow sweep — evict key includes tenant to match tenant-scoped @Cacheable key.
    @CacheEvict(value = "leads", key = "T(com.kiteclass.core.common.context.TenantContext).getCurrentTenant() + ':' + #id")
    public LeadResponse updateLeadStatus(Long id, LeadStatus newStatus) {
        log.info("Updating lead status for ID: {} to {}", id, newStatus);

        UUID tenantId = TenantContext.getCurrentTenant();

        Lead lead = leadRepository.findByIdAndInstanceIdAndDeletedFalse(id, tenantId)
                .orElseThrow(() -> {
                    log.warn("Lead not found with ID: {}", id);
                    return new EntityNotFoundException("LEAD_NOT_FOUND", (Object) id);
                });

        lead.setStatus(newStatus);
        Lead updated = leadRepository.save(lead);

        log.info("Updated lead status for ID: {}", id);
        return leadMapper.toResponse(updated);
    }

    /**
     * Updates an existing lead.
     *
     * <p>Only updates non-null fields from request.
     * Validates email uniqueness if changed.
     *
     * @param id      the lead ID
     * @param request the update request
     * @return LeadResponse with updated lead data
     * @throws EntityNotFoundException if lead not found
     * @throws ValidationException     if new email already exists
     */
    @Override
    @Transactional
    // GAP-792 cross-flow sweep — evict key includes tenant to match tenant-scoped @Cacheable key.
    @CacheEvict(value = "leads", key = "T(com.kiteclass.core.common.context.TenantContext).getCurrentTenant() + ':' + #id")
    public LeadResponse updateLead(Long id, UpdateLeadRequest request) {
        log.info("Updating lead with ID: {}", id);

        UUID tenantId = TenantContext.getCurrentTenant();

        Lead lead = leadRepository.findByIdAndInstanceIdAndDeletedFalse(id, tenantId)
                .orElseThrow(() -> {
                    log.warn("Lead not found with ID: {}", id);
                    return new EntityNotFoundException("LEAD_NOT_FOUND", (Object) id);
                });

        // BR-MKT-002: Validate email uniqueness within tenant if changed
        if (request.getEmail() != null && !request.getEmail().equals(lead.getEmail())) {
            if (leadRepository.findByEmailAndInstanceIdAndDeletedFalse(request.getEmail(), tenantId).isPresent()) {
                log.warn("Duplicate lead email within tenant: {}, tenantId: {}", request.getEmail(), tenantId);
                throw new ValidationException("LEAD_EMAIL_ALREADY_EXISTS", (Object) request.getEmail());
            }
        }

        leadMapper.updateEntity(lead, request);
        Lead updated = leadRepository.save(lead);

        log.info("Updated lead with ID: {}", id);
        return leadMapper.toResponse(updated);
    }

    /**
     * Soft-deletes a lead.
     *
     * <p>Marks the lead as deleted without physically removing from database.
     *
     * @param id the lead ID
     * @throws EntityNotFoundException if lead not found
     */
    @Override
    @Transactional
    // GAP-792 cross-flow sweep — evict key includes tenant to match tenant-scoped @Cacheable key.
    @CacheEvict(value = "leads", key = "T(com.kiteclass.core.common.context.TenantContext).getCurrentTenant() + ':' + #id")
    public void deleteLead(Long id) {
        log.info("Deleting lead with ID: {}", id);

        UUID tenantId = TenantContext.getCurrentTenant();

        Lead lead = leadRepository.findByIdAndInstanceIdAndDeletedFalse(id, tenantId)
                .orElseThrow(() -> {
                    log.warn("Lead not found with ID: {}", id);
                    return new EntityNotFoundException("LEAD_NOT_FOUND", (Object) id);
                });

        lead.setDeleted(true);
        leadRepository.save(lead);

        log.info("Deleted lead with ID: {}", id);
    }
}
