package com.kiteclass.core.module.marketing.service.impl;

import com.kiteclass.core.common.context.TenantContext;
import com.kiteclass.core.common.dto.PageResponse;
import com.kiteclass.core.common.exception.EntityNotFoundException;
import com.kiteclass.core.common.service.email.EmailService;
import com.kiteclass.core.module.marketing.dto.request.CreateContactMessageRequest;
import com.kiteclass.core.module.marketing.dto.response.ContactMessageResponse;
import com.kiteclass.core.module.marketing.entity.ContactMessage;
import com.kiteclass.core.module.marketing.mapper.ContactMessageMapper;
import com.kiteclass.core.module.marketing.repository.ContactMessageRepository;
import com.kiteclass.core.module.marketing.service.ContactMessageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

/**
 * Implementation of ContactMessageService interface.
 *
 * <p>Handles contact message management with multi-tenant isolation.
 *
 * @since 2.10
 */
@Slf4j
@Service
@RequiredArgsConstructor
@org.springframework.validation.annotation.Validated
public class ContactMessageServiceImpl implements ContactMessageService {

    private final ContactMessageRepository contactMessageRepository;
    private final ContactMessageMapper contactMessageMapper;
    private final EmailService emailService;

    @Value("${contact.admin-email:admin@kiteclass.com}")
    private String adminEmail;

    /**
     * Creates a new contact message from website visitor.
     *
     * @param request  the create request with message details
     * @param tenantId the tenant ID
     * @return ContactMessageResponse with created message data
     */
    @Override
    @Transactional
    @CacheEvict(value = "contactMessages", allEntries = true)
    public ContactMessageResponse createContactMessage(CreateContactMessageRequest request, UUID tenantId) {
        log.info("Creating contact message from: {}, tenantId: {}", request.getEmail(), tenantId);

        // GAP-1221: subject optional trên form public — default server-side trước khi map
        if (request.getSubject() == null || request.getSubject().isBlank()) {
            request.setSubject("Liên hệ từ " + request.getName());
        }

        ContactMessage contactMessage = contactMessageMapper.toEntity(request);

        // CRITICAL: Set instanceId for multi-tenant isolation
        contactMessage.setInstanceId(tenantId);

        ContactMessage saved = contactMessageRepository.save(contactMessage);

        // BR-MKT-003: Send notification email to teacher/admin
        try {
            // GAP-1221: email optional — placeholder để template notify không render null
            String senderEmail = (request.getEmail() == null || request.getEmail().isBlank())
                    ? "(không cung cấp email)"
                    : request.getEmail();
            emailService.sendContactNotification(
                    adminEmail,
                    request.getName(),
                    senderEmail,
                    request.getSubject(),
                    request.getMessage()
            );
            log.info("Sent contact notification email to: {}", adminEmail);
        } catch (Exception e) {
            // Don't fail contact message creation if email fails
            log.error("Failed to send contact notification email: {}", e.getMessage(), e);
        }

        log.info("Created contact message with ID: {}, instanceId: {}", saved.getId(), saved.getInstanceId());
        return contactMessageMapper.toResponse(saved);
    }

    /**
     * Searches contact messages with filters and pagination.
     *
     * @param isRead   filter by read status (can be null)
     * @param pageable pagination parameters
     * @return PageResponse with matching contact messages
     */
    @Override
    @Transactional(readOnly = true)
    public PageResponse<ContactMessageResponse> getContactMessages(Boolean isRead, Pageable pageable) {
        UUID tenantId = TenantContext.getCurrentTenant();

        log.debug("Searching contact messages with isRead='{}', tenantId={}, page={}", isRead, tenantId, pageable.getPageNumber());

        Page<ContactMessage> messagePage;

        if (Boolean.FALSE.equals(isRead)) {
            messagePage = contactMessageRepository.findUnreadByInstanceId(tenantId, pageable);
        } else {
            messagePage = contactMessageRepository.findByInstanceIdAndDeletedFalse(tenantId, pageable);
        }

        Page<ContactMessageResponse> responsePage = messagePage.map(contactMessageMapper::toResponse);

        return PageResponse.from(responsePage);
    }

    /**
     * Marks a contact message as read.
     *
     * @param id     the contact message ID
     * @param readBy the username/email of person who read the message
     * @return ContactMessageResponse with updated message data
     * @throws EntityNotFoundException if message not found
     */
    @Override
    @Transactional
    @CacheEvict(value = "contactMessages", key = "#id")
    public ContactMessageResponse markAsRead(Long id, String readBy) {
        log.info("Marking contact message as read: {}, by: {}", id, readBy);

        UUID tenantId = TenantContext.getCurrentTenant();

        ContactMessage contactMessage = contactMessageRepository.findByIdAndInstanceIdAndDeletedFalse(id, tenantId)
                .orElseThrow(() -> {
                    log.warn("Contact message not found with ID: {}", id);
                    return new EntityNotFoundException("CONTACT_MESSAGE_NOT_FOUND", (Object) id);
                });

        contactMessage.setIsRead(true);
        contactMessage.setReadAt(Instant.now());
        contactMessage.setReadBy(readBy);

        ContactMessage updated = contactMessageRepository.save(contactMessage);

        log.info("Marked contact message as read: {}", id);
        return contactMessageMapper.toResponse(updated);
    }

    /**
     * Soft-deletes a contact message.
     *
     * @param id the contact message ID
     * @throws EntityNotFoundException if message not found
     */
    @Override
    @Transactional
    @CacheEvict(value = "contactMessages", key = "#id")
    public void deleteContactMessage(Long id) {
        log.info("Deleting contact message with ID: {}", id);

        UUID tenantId = TenantContext.getCurrentTenant();

        ContactMessage contactMessage = contactMessageRepository.findByIdAndInstanceIdAndDeletedFalse(id, tenantId)
                .orElseThrow(() -> {
                    log.warn("Contact message not found with ID: {}", id);
                    return new EntityNotFoundException("CONTACT_MESSAGE_NOT_FOUND", (Object) id);
                });

        contactMessage.setDeleted(true);
        contactMessageRepository.save(contactMessage);

        log.info("Deleted contact message with ID: {}", id);
    }

    /**
     * Counts unread messages for current tenant.
     *
     * @param tenantId the tenant ID
     * @return number of unread messages
     */
    @Override
    @Transactional(readOnly = true)
    public long countUnread(UUID tenantId) {
        log.debug("Counting unread messages for tenant: {}", tenantId);

        return contactMessageRepository.countUnreadByInstanceId(tenantId);
    }
}
