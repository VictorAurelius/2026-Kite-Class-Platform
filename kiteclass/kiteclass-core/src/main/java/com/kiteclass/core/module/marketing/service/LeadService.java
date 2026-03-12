package com.kiteclass.core.module.marketing.service;

import com.kiteclass.core.common.dto.PageResponse;
import com.kiteclass.core.module.marketing.dto.request.CreateLeadRequest;
import com.kiteclass.core.module.marketing.dto.request.UpdateLeadRequest;
import com.kiteclass.core.module.marketing.dto.response.LeadResponse;
import com.kiteclass.core.module.marketing.enums.LeadStatus;
import jakarta.validation.Valid;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

/**
 * Service interface for Lead business logic.
 *
 * <p>Business Rules:
 * <ul>
 *   <li>BR-MKT-002: Lead email must be unique per tenant</li>
 *   <li>BR-MKT-004: Lead creation sends confirmation email</li>
 * </ul>
 *
 * @since 2.10
 */
public interface LeadService {

    /**
     * Creates a new lead from trial registration.
     *
     * <p>Implements BR-MKT-002: Validates email uniqueness within tenant.
     * Sets initial status to NEW.
     * Sends confirmation email to lead (BR-MKT-004).
     *
     * @param request  the create request with lead details
     * @param tenantId the tenant ID (instance ID) for multi-tenant isolation
     * @return LeadResponse with created lead data
     * @throws com.kiteclass.core.common.exception.ValidationException if email already exists
     */
    LeadResponse createLead(@Valid CreateLeadRequest request, UUID tenantId);

    /**
     * Retrieves a lead by ID.
     *
     * @param id the lead ID
     * @return LeadResponse with lead data
     * @throws com.kiteclass.core.common.exception.EntityNotFoundException if lead not found or deleted
     */
    LeadResponse getLeadById(Long id);

    /**
     * Searches leads with filters and pagination.
     *
     * <p>Filters by status (optional). Returns all leads if status is null.
     *
     * @param status   the lead status filter (can be null for all statuses)
     * @param pageable pagination parameters
     * @return PageResponse with matching leads
     */
    PageResponse<LeadResponse> getLeads(LeadStatus status, Pageable pageable);

    /**
     * Updates lead status (e.g., NEW → CONTACTED → CONVERTED).
     *
     * @param id        the lead ID
     * @param newStatus the new status
     * @return LeadResponse with updated lead data
     * @throws com.kiteclass.core.common.exception.EntityNotFoundException if lead not found
     */
    LeadResponse updateLeadStatus(Long id, LeadStatus newStatus);

    /**
     * Updates an existing lead.
     *
     * <p>Only updates non-null fields from request.
     * Validates email uniqueness if changed.
     *
     * @param id      the lead ID
     * @param request the update request with new values
     * @return LeadResponse with updated lead data
     * @throws com.kiteclass.core.common.exception.EntityNotFoundException if lead not found
     * @throws com.kiteclass.core.common.exception.ValidationException       if new email already exists
     */
    LeadResponse updateLead(Long id, @Valid UpdateLeadRequest request);

    /**
     * Soft-deletes a lead.
     *
     * <p>Sets deleted flag to true instead of physically removing the record.
     * Deleted leads are excluded from normal queries.
     *
     * @param id the lead ID
     * @throws com.kiteclass.core.common.exception.EntityNotFoundException if lead not found
     */
    void deleteLead(Long id);
}
