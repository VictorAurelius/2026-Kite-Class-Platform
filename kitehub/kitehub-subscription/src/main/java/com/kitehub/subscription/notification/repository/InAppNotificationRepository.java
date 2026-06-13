package com.kitehub.subscription.notification.repository;

import com.kitehub.subscription.notification.entity.InAppNotification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository for persistent in-app notifications (GAP-1265 persistent-banner fallback).
 *
 * <p>All queries are instance-scoped — the controller binds {@code instanceId} to the caller's
 * tenant via {@code TenantOwnershipGuard} before reaching the repository.</p>
 *
 * @author KiteHub Team
 * @since wave-kitehub-biz-100
 */
@Repository
public interface InAppNotificationRepository extends JpaRepository<InAppNotification, UUID> {

    /** Unread banners for an instance, newest first. */
    List<InAppNotification> findByInstanceIdAndReadFalseAndDeletedFalseOrderByCreatedAtDesc(UUID instanceId);

    /** All (read + unread) banners for an instance, newest first. */
    List<InAppNotification> findByInstanceIdAndDeletedFalseOrderByCreatedAtDesc(UUID instanceId);

    /** Tenant-scoped lookup for mark-read (guards against cross-tenant id guessing). */
    Optional<InAppNotification> findByIdAndInstanceIdAndDeletedFalse(UUID id, UUID instanceId);
}
