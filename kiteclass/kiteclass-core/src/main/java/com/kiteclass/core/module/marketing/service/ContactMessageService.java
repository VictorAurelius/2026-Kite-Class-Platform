package com.kiteclass.core.module.marketing.service;

import com.kiteclass.core.common.dto.PageResponse;
import com.kiteclass.core.module.marketing.dto.request.CreateContactMessageRequest;
import com.kiteclass.core.module.marketing.dto.response.ContactMessageResponse;
import jakarta.validation.Valid;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

/**
 * Service interface for ContactMessage business logic.
 *
 * <p>Business Rule: BR-MKT-003 - Contact message triggers email to teacher (TODO).
 *
 * @since 2.10
 */
public interface ContactMessageService {

    /**
     * Creates a new contact message from website visitor.
     *
     * <p>Sets isRead to false initially.
     *
     * TODO: BR-MKT-003 - Integrate with EmailService to notify teacher
     *
     * @param request  the create request with message details
     * @param tenantId the tenant ID (instance ID) for multi-tenant isolation
     * @return ContactMessageResponse with created message data
     */
    ContactMessageResponse createContactMessage(@Valid CreateContactMessageRequest request, UUID tenantId);

    /**
     * Searches contact messages with filters and pagination.
     *
     * <p>Filters by read status (optional). Returns all messages if isRead is null.
     *
     * @param isRead   filter by read status (can be null for all messages)
     * @param pageable pagination parameters
     * @return PageResponse with matching contact messages
     */
    PageResponse<ContactMessageResponse> getContactMessages(Boolean isRead, Pageable pageable);

    /**
     * Marks a contact message as read.
     *
     * <p>Sets isRead to true, readAt to current time, and readBy to current user.
     *
     * @param id     the contact message ID
     * @param readBy the username/email of person who read the message
     * @return ContactMessageResponse with updated message data
     * @throws com.kiteclass.core.common.exception.EntityNotFoundException if message not found
     */
    ContactMessageResponse markAsRead(Long id, String readBy);

    /**
     * Soft-deletes a contact message.
     *
     * <p>Sets deleted flag to true instead of physically removing the record.
     * Deleted messages are excluded from normal queries.
     *
     * @param id the contact message ID
     * @throws com.kiteclass.core.common.exception.EntityNotFoundException if message not found
     */
    void deleteContactMessage(Long id);

    /**
     * Counts unread messages for current tenant.
     *
     * <p>Used for notification badges in admin UI.
     *
     * @param tenantId the tenant ID
     * @return number of unread messages
     */
    long countUnread(UUID tenantId);
}
