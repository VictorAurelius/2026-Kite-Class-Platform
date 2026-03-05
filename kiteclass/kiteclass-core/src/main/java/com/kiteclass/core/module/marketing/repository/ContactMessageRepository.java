package com.kiteclass.core.module.marketing.repository;

import com.kiteclass.core.module.marketing.entity.ContactMessage;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

/**
 * Repository for ContactMessage entity.
 *
 * @since 2.10
 */
@Repository
public interface ContactMessageRepository extends JpaRepository<ContactMessage, Long> {

    /**
     * Find all contact messages for tenant with pagination.
     *
     * @param instanceId tenant instance ID
     * @param pageable   pagination parameters
     * @return page of contact messages
     */
    @Query("SELECT cm FROM ContactMessage cm WHERE cm.instanceId = :instanceId AND cm.deleted = false")
    Page<ContactMessage> findByInstanceIdAndDeletedFalse(@Param("instanceId") UUID instanceId, Pageable pageable);

    /**
     * Find unread contact messages for tenant.
     *
     * @param instanceId tenant instance ID
     * @param pageable   pagination parameters
     * @return page of unread messages
     */
    @Query("SELECT cm FROM ContactMessage cm WHERE cm.instanceId = :instanceId AND cm.isRead = false AND cm.deleted = false")
    Page<ContactMessage> findUnreadByInstanceId(@Param("instanceId") UUID instanceId, Pageable pageable);

    /**
     * Find contact message by ID (bypass Hibernate filter issue).
     *
     * @param id         message ID
     * @param instanceId tenant instance ID
     * @return contact message if exists
     */
    @Query("SELECT cm FROM ContactMessage cm WHERE cm.id = :id AND cm.instanceId = :instanceId AND cm.deleted = false")
    Optional<ContactMessage> findByIdAndInstanceIdAndDeletedFalse(@Param("id") Long id, @Param("instanceId") UUID instanceId);

    /**
     * Count unread messages for tenant.
     *
     * @param instanceId tenant instance ID
     * @return number of unread messages
     */
    @Query("SELECT COUNT(cm) FROM ContactMessage cm WHERE cm.instanceId = :instanceId AND cm.isRead = false AND cm.deleted = false")
    long countUnreadByInstanceId(@Param("instanceId") UUID instanceId);
}
