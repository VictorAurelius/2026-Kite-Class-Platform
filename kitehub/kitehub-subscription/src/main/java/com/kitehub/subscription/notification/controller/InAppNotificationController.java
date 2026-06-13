package com.kitehub.subscription.notification.controller;

import com.kitehub.subscription.notification.dto.InAppNotificationResponse;
import com.kitehub.subscription.notification.service.InAppNotificationService;
import com.kitehub.subscription.security.TenantOwnershipGuard;
import io.micrometer.core.annotation.Timed;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * Owner-facing read/dismiss for persistent in-app notifications (GAP-1265 banner store).
 *
 * <p>The instance path id IS the tenant scope — bound to the caller via
 * {@code TenantOwnershipGuard} (same pattern as {@code SubscriptionController}). This is the read
 * path for the persistent-banner fallback channel; the dispatch (write) path is server-side.</p>
 *
 * @author KiteHub Team
 * @since wave-kitehub-biz-100
 */
@RestController
@RequestMapping("/api/platform/notifications/in-app")
@RequiredArgsConstructor
@Tag(name = "In-App Notifications", description = "Persistent owner notification banners (GAP-1265)")
@Timed(value = "http.server.requests", percentiles = {0.5, 0.95, 0.99},
       extraTags = {"slo", "tier-b", "controller", "in-app-notifications"})
public class InAppNotificationController {

    static final String OWNER_OR_STAFF_AUTHZ =
            "hasAnyRole('OWNER','STAFF','PLATFORM_ADMIN','ADMIN')";

    private final InAppNotificationService service;

    /**
     * List banners for the instance.
     *
     * @param instanceId instance UUID
     * @param unreadOnly when true, return only undismissed banners (default false)
     * @return banners newest-first
     */
    @GetMapping("/instance/{instanceId}")
    @PreAuthorize(OWNER_OR_STAFF_AUTHZ)
    public ResponseEntity<List<InAppNotificationResponse>> list(
        @RequestHeader(value = "X-Tenant-Id", required = false) String tenantHeader,
        @PathVariable UUID instanceId,
        @RequestParam(defaultValue = "false") boolean unreadOnly
    ) {
        TenantOwnershipGuard.requireOwnership(instanceId, tenantHeader);
        return ResponseEntity.ok(service.list(instanceId, unreadOnly));
    }

    /**
     * Mark one banner read/dismissed.
     *
     * @param instanceId     instance UUID (tenant scope)
     * @param notificationId banner UUID
     * @return updated banner
     */
    @PatchMapping("/instance/{instanceId}/{notificationId}/read")
    @PreAuthorize(OWNER_OR_STAFF_AUTHZ)
    public ResponseEntity<InAppNotificationResponse> markRead(
        @RequestHeader(value = "X-Tenant-Id", required = false) String tenantHeader,
        @PathVariable UUID instanceId,
        @PathVariable UUID notificationId
    ) {
        TenantOwnershipGuard.requireOwnership(instanceId, tenantHeader);
        return ResponseEntity.ok(service.markRead(instanceId, notificationId));
    }
}
