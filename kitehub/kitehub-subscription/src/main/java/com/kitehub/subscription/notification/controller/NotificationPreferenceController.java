package com.kitehub.subscription.notification.controller;

import com.kitehub.subscription.notification.dto.NotificationPreferenceDto;
import com.kitehub.subscription.notification.dto.NotificationPreferenceListResponse;
import com.kitehub.subscription.notification.dto.UpdateNotificationPreferenceRequest;
import com.kitehub.subscription.notification.enums.NotificationType;
import com.kitehub.subscription.notification.service.MandatoryTypeCannotBeDisabledException;
import com.kitehub.subscription.notification.service.NotificationPreferenceService;
import io.micrometer.core.annotation.Timed;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

/**
 * REST controller for user-level notification preferences.
 *
 * <p>Phase 1 surface (Wave 18a Bucket B — GAP-063 Phase 1): list + upsert via
 * PATCH. Auth: caller-supplied {@code X-User-Id} header (gateway-injected from
 * the JWT {@code sub} claim — same pattern as existing webhook controller).
 * Cross-user reads enforced by service-level filter (BR-NOTIF-009).</p>
 *
 * @since 1.0 (Wave 18a Bucket B — GAP-063 Phase 1)
 */
@RestController
@RequestMapping("/api/v1/notification-preferences")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Notification Preferences",
        description = "User-level notification preferences (GAP-063 Phase 1)")
@Timed(value = "http.server.requests", percentiles = {0.5, 0.95, 0.99},
        extraTags = {"slo", "tier-b", "controller", "notification-preferences"})
public class NotificationPreferenceController {

    private final NotificationPreferenceService preferenceService;

    /**
     * List all preferences for the authenticated user.
     */
    @GetMapping
    public ResponseEntity<NotificationPreferenceListResponse> list(
            @RequestHeader("X-User-Id") UUID userId) {
        return ResponseEntity.ok(preferenceService.list(userId));
    }

    /**
     * Upsert the preference for one notification type.
     */
    @PatchMapping("/{notificationType}")
    public ResponseEntity<NotificationPreferenceDto> update(
            @RequestHeader("X-User-Id") UUID userId,
            @PathVariable NotificationType notificationType,
            @Valid @RequestBody UpdateNotificationPreferenceRequest request) {
        NotificationPreferenceDto dto = preferenceService.update(userId, notificationType, request);
        return ResponseEntity.ok(dto);
    }

    // ----- Local exception handlers (small surface; avoid cross-controller spillover) -----

    @ExceptionHandler(MandatoryTypeCannotBeDisabledException.class)
    public ResponseEntity<Map<String, Object>> handleMandatory(MandatoryTypeCannotBeDisabledException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of(
                "errorCode", MandatoryTypeCannotBeDisabledException.ERROR_CODE,
                "message", "Loại thông báo bắt buộc không thể tắt.",
                "timestamp", LocalDateTime.now().toString()
        ));
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<Map<String, Object>> handlePathTypeMismatch(MethodArgumentTypeMismatchException ex) {
        // Triggered when path var "notificationType" doesn't bind to NotificationType enum.
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of(
                "errorCode", "INVALID_NOTIFICATION_TYPE",
                "message", "Loại thông báo không hợp lệ.",
                "timestamp", LocalDateTime.now().toString()
        ));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, Object>> handleIllegal(IllegalArgumentException ex) {
        log.debug("notification.preference.bad-request: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of(
                "errorCode", "INVALID_REQUEST",
                "message", ex.getMessage() != null ? ex.getMessage() : "Yêu cầu không hợp lệ.",
                "timestamp", LocalDateTime.now().toString()
        ));
    }
}
