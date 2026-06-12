package com.kiteclass.core.module.marketing.controller;

import com.kiteclass.core.common.dto.ApiResponse;
import com.kiteclass.core.common.dto.PageResponse;
import com.kiteclass.core.module.marketing.dto.request.CreateLeadRequest;
import com.kiteclass.core.module.marketing.dto.request.UpdateLeadRequest;
import com.kiteclass.core.module.marketing.dto.response.LeadResponse;
import com.kiteclass.core.module.marketing.enums.LeadStatus;
import com.kiteclass.core.module.marketing.service.LeadService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

/**
 * REST controller for Lead operations.
 *
 * <p>Provides endpoints for:
 * <ul>
 *   <li>POST /api/v1/leads - Create lead (public)</li>
 *   <li>GET /api/v1/leads - Search leads (admin/teacher)</li>
 *   <li>GET /api/v1/leads/{id} - Get lead by ID (admin/teacher)</li>
 *   <li>PUT /api/v1/leads/{id}/status - Update lead status (admin/teacher)</li>
 *   <li>PUT /api/v1/leads/{id} - Update lead (admin/teacher)</li>
 *   <li>DELETE /api/v1/leads/{id} - Delete lead (admin/teacher)</li>
 * </ul>
 *
 * @since 2.10
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/leads")
@RequiredArgsConstructor
@Tag(name = "Marketing - Leads", description = "Lead management APIs")
public class LeadController {

    private final LeadService leadService;

    /**
     * Creates a new lead from trial registration.
     * Public endpoint - no authentication required.
     *
     * @param request  the create request with lead details
     * @param tenantId the tenant ID from X-Tenant-Id header
     * @return ApiResponse with created lead data and HTTP 201
     */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Create a new lead", description = "Creates a new lead from trial registration (public access)")
    public ApiResponse<LeadResponse> createLead(
            @Valid @RequestBody CreateLeadRequest request,
            @RequestHeader(value = "X-Tenant-Id", required = true) UUID tenantId) {
        log.info("REST request to create lead: {}, tenantId: {}", request.getEmail(), tenantId);
        LeadResponse response = leadService.createLead(request, tenantId);
        return ApiResponse.success(response, "Lead created successfully");
    }

    /**
     * Searches leads with filters and pagination.
     * Requires ADMIN or TEACHER role.
     *
     * @param status the lead status filter
     * @param page   page number (0-indexed)
     * @param size   page size
     * @param sort   sort field (default: createdAt)
     * @return ApiResponse with page of leads and HTTP 200
     */
    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'TEACHER', 'OWNER')")
    @Operation(summary = "Search leads",
            description = "Searches leads with optional filters and pagination")
    public ApiResponse<PageResponse<LeadResponse>> getLeads(
            @Parameter(description = "Lead status filter") @RequestParam(required = false) LeadStatus status,
            @Parameter(description = "Page number (0-indexed)") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Page size") @RequestParam(defaultValue = "20") int size,
            @Parameter(description = "Sort criteria (e.g., 'createdAt,desc')") @RequestParam(defaultValue = "createdAt,desc") String sort) {
        log.debug("REST request to search leads: status='{}', page={}, size={}", status, page, size);

        // Parse sort string (format: "field,direction")
        String[] sortParts = sort.split(",");
        String sortField = sortParts[0];
        Sort.Direction direction = sortParts.length > 1 && "desc".equalsIgnoreCase(sortParts[1]) ?
                Sort.Direction.DESC : Sort.Direction.ASC;

        // Convert camelCase to snake_case for native SQL queries
        String dbColumnName = toSnakeCase(sortField);

        Pageable pageable = PageRequest.of(page, size, Sort.by(direction, dbColumnName));
        PageResponse<LeadResponse> response = leadService.getLeads(status, pageable);

        return ApiResponse.success(response);
    }

    /**
     * Retrieves a lead by ID.
     * Requires ADMIN or TEACHER role.
     *
     * @param id the lead ID
     * @return ApiResponse with lead data and HTTP 200
     */
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'TEACHER', 'OWNER')")
    @Operation(summary = "Get lead by ID", description = "Retrieves a lead's information by their ID (requires ADMIN or TEACHER role)")
    public ApiResponse<LeadResponse> getLeadById(
            @Parameter(description = "Lead ID") @PathVariable Long id) {
        log.debug("REST request to get lead with ID: {}", id);
        LeadResponse response = leadService.getLeadById(id);
        return ApiResponse.success(response);
    }

    /**
     * Updates lead status.
     * Requires ADMIN or TEACHER role.
     *
     * @param id        the lead ID
     * @param newStatus the new status
     * @return ApiResponse with updated lead data and HTTP 200
     */
    @PutMapping("/{id}/status")
    @PreAuthorize("hasAnyRole('ADMIN', 'TEACHER', 'OWNER')")
    @Operation(summary = "Update lead status", description = "Updates lead status (e.g., NEW → CONTACTED → CONVERTED)")
    public ApiResponse<LeadResponse> updateLeadStatus(
            @Parameter(description = "Lead ID") @PathVariable Long id,
            @Parameter(description = "New status") @RequestParam LeadStatus newStatus) {
        log.info("REST request to update lead status: ID={}, newStatus={}", id, newStatus);
        LeadResponse response = leadService.updateLeadStatus(id, newStatus);
        return ApiResponse.success(response, "Lead status updated successfully");
    }

    /**
     * Updates an existing lead.
     * Requires ADMIN or TEACHER role.
     *
     * @param id      the lead ID
     * @param request the update request with new values
     * @return ApiResponse with updated lead data and HTTP 200
     */
    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'TEACHER', 'OWNER')")
    @Operation(summary = "Update lead", description = "Updates an existing lead's information (requires ADMIN or TEACHER role)")
    public ApiResponse<LeadResponse> updateLead(
            @Parameter(description = "Lead ID") @PathVariable Long id,
            @Valid @RequestBody UpdateLeadRequest request) {
        log.info("REST request to update lead with ID: {}", id);
        LeadResponse response = leadService.updateLead(id, request);
        return ApiResponse.success(response, "Lead updated successfully");
    }

    /**
     * Soft-deletes a lead.
     * Requires ADMIN or TEACHER role.
     *
     * @param id the lead ID
     * @return HTTP 204 No Content
     */
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasAnyRole('ADMIN', 'TEACHER', 'OWNER')")
    @Operation(summary = "Delete lead", description = "Soft-deletes a lead (sets deleted flag)")
    public void deleteLead(@Parameter(description = "Lead ID") @PathVariable Long id) {
        log.info("REST request to delete lead with ID: {}", id);
        leadService.deleteLead(id);
    }

    /**
     * Converts camelCase field name to snake_case database column name.
     *
     * @param camelCase the camelCase field name
     * @return the snake_case column name
     */
    private String toSnakeCase(String camelCase) {
        return camelCase.replaceAll("([a-z])([A-Z])", "$1_$2").toLowerCase();
    }
}
