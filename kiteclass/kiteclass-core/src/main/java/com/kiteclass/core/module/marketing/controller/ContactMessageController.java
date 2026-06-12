package com.kiteclass.core.module.marketing.controller;

import com.kiteclass.core.common.dto.ApiResponse;
import com.kiteclass.core.common.dto.PageResponse;
import com.kiteclass.core.module.marketing.dto.request.CreateContactMessageRequest;
import com.kiteclass.core.module.marketing.dto.response.ContactMessageResponse;
import com.kiteclass.core.module.marketing.service.ContactMessageService;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

/**
 * REST controller for Contact Message operations.
 *
 * <p>Provides endpoints for:
 * <ul>
 *   <li>POST /api/v1/contact - Create contact message (public)</li>
 *   <li>GET /api/v1/contact-messages - Search messages (admin/teacher)</li>
 *   <li>GET /api/v1/contact-messages/unread-count - Count unread messages (admin/teacher)</li>
 *   <li>PUT /api/v1/contact-messages/{id}/read - Mark message as read (admin/teacher)</li>
 *   <li>DELETE /api/v1/contact-messages/{id} - Delete message (admin/teacher)</li>
 * </ul>
 *
 * @since 2.10
 */
@Slf4j
@RestController
@RequiredArgsConstructor
@Tag(name = "Marketing - Contact Messages", description = "Contact message management APIs")
public class ContactMessageController {

    private final ContactMessageService contactMessageService;

    /**
     * Creates a new contact message from website visitor.
     * Public endpoint - no authentication required.
     *
     * @param request  the create request with message details
     * @param tenantId the tenant ID from X-Tenant-Id header
     * @return ApiResponse with created message data and HTTP 201
     */
    @PostMapping("/api/v1/contact")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Create contact message", description = "Creates a contact message from website visitor (public access)")
    public ApiResponse<ContactMessageResponse> createContactMessage(
            @Valid @RequestBody CreateContactMessageRequest request,
            @RequestHeader(value = "X-Tenant-Id", required = true) UUID tenantId) {
        log.info("REST request to create contact message from: {}, tenantId: {}", request.getEmail(), tenantId);
        ContactMessageResponse response = contactMessageService.createContactMessage(request, tenantId);
        return ApiResponse.success(response, "Contact message sent successfully");
    }

    /**
     * Searches contact messages with filters and pagination.
     * Requires ADMIN or TEACHER role.
     *
     * @param isRead filter by read status
     * @param page   page number (0-indexed)
     * @param size   page size
     * @param sort   sort field (default: createdAt)
     * @return ApiResponse with page of contact messages and HTTP 200
     */
    @GetMapping("/api/v1/contact-messages")
    @PreAuthorize("hasAnyRole('ADMIN', 'TEACHER', 'OWNER')")
    @Operation(summary = "Search contact messages",
            description = "Searches contact messages with optional filters and pagination")
    public ApiResponse<PageResponse<ContactMessageResponse>> getContactMessages(
            @Parameter(description = "Filter by read status") @RequestParam(required = false) Boolean isRead,
            @Parameter(description = "Page number (0-indexed)") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Page size") @RequestParam(defaultValue = "20") int size,
            @Parameter(description = "Sort criteria (e.g., 'createdAt,desc')") @RequestParam(defaultValue = "createdAt,desc") String sort) {
        log.debug("REST request to search contact messages: isRead='{}', page={}, size={}", isRead, page, size);

        // Parse sort string (format: "field,direction")
        String[] sortParts = sort.split(",");
        String sortField = sortParts[0];
        Sort.Direction direction = sortParts.length > 1 && "desc".equalsIgnoreCase(sortParts[1]) ?
                Sort.Direction.DESC : Sort.Direction.ASC;

        // Convert camelCase to snake_case for native SQL queries
        String dbColumnName = toSnakeCase(sortField);

        Pageable pageable = PageRequest.of(page, size, Sort.by(direction, dbColumnName));
        PageResponse<ContactMessageResponse> response = contactMessageService.getContactMessages(isRead, pageable);

        return ApiResponse.success(response);
    }

    /**
     * Counts unread messages for current tenant.
     * Requires ADMIN or TEACHER role.
     *
     * @param tenantId the tenant ID from X-Tenant-Id header
     * @return ApiResponse with unread count and HTTP 200
     */
    @GetMapping("/api/v1/contact-messages/unread-count")
    @PreAuthorize("hasAnyRole('ADMIN', 'TEACHER', 'OWNER')")
    @Operation(summary = "Count unread messages",
            description = "Counts unread contact messages for a tenant")
    public ApiResponse<Long> getUnreadCount(
            @RequestHeader(value = "X-Tenant-Id", required = true) UUID tenantId) {
        log.debug("REST request to count unread messages for tenant: {}", tenantId);
        long count = contactMessageService.countUnread(tenantId);
        return ApiResponse.success(count);
    }

    /**
     * Marks a contact message as read.
     * Requires ADMIN or TEACHER role.
     *
     * @param id     the contact message ID
     * @param readBy the username/email of person who read the message
     * @return ApiResponse with updated message data and HTTP 200
     */
    @PutMapping("/api/v1/contact-messages/{id}/read")
    @PreAuthorize("hasAnyRole('ADMIN', 'TEACHER', 'OWNER')")
    @Operation(summary = "Mark message as read", description = "Marks a contact message as read (requires ADMIN or TEACHER role)")
    public ApiResponse<ContactMessageResponse> markAsRead(
            @Parameter(description = "Contact message ID") @PathVariable Long id,
            @Parameter(description = "Username/email of reader") @RequestParam String readBy) {
        log.info("REST request to mark contact message as read: ID={}, readBy={}", id, readBy);
        ContactMessageResponse response = contactMessageService.markAsRead(id, readBy);
        return ApiResponse.success(response, "Contact message marked as read");
    }

    /**
     * Soft-deletes a contact message.
     * Requires ADMIN or TEACHER role.
     *
     * @param id the contact message ID
     * @return HTTP 204 No Content
     */
    @DeleteMapping("/api/v1/contact-messages/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasAnyRole('ADMIN', 'TEACHER', 'OWNER')")
    @Operation(summary = "Delete contact message", description = "Soft-deletes a contact message (sets deleted flag)")
    public void deleteContactMessage(@Parameter(description = "Contact message ID") @PathVariable Long id) {
        log.info("REST request to delete contact message with ID: {}", id);
        contactMessageService.deleteContactMessage(id);
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
