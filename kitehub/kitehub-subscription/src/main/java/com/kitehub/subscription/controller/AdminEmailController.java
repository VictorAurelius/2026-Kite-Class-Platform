package com.kitehub.subscription.controller;

import com.kitehub.subscription.dto.EmailConfigResponse;
import com.kitehub.subscription.dto.EmailHistoryResponse;
import com.kitehub.subscription.dto.EmailStatsResponse;
import com.kitehub.subscription.dto.TriggerEmailRequest;
import com.kitehub.subscription.service.EmailAdminService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

/**
 * Admin REST controller for email monitoring and control.
 * <p>
 * Provides endpoints for:
 * - Browsing email send history with filters
 * - Viewing aggregate email statistics
 * - Toggling email types on/off
 * - Manually triggering email sends
 *
 * <p><strong>Auth</strong>: all routes require a JWT with role {@code PLATFORM_ADMIN}.
 * The gateway forwards the role as {@code X-User-Roles} header; Spring Security maps it
 * to {@code ROLE_PLATFORM_ADMIN} and the class-level {@link PreAuthorize} enforces access
 * (GAP-938, Wave flow-kh3 — supersedes the legacy {@code X-Admin-Key} interceptor).</p>
 *
 * @since 1.0.0
 */
@Slf4j
@RestController
@RequestMapping("/api/platform/admin/emails")
@RequiredArgsConstructor
@PreAuthorize("hasRole('PLATFORM_ADMIN')")
@Tag(name = "Admin Email", description = "Email monitoring and control for platform admins")
public class AdminEmailController {

    private final EmailAdminService emailAdminService;

    /**
     * Get paginated email send history with optional filters.
     *
     * @param instanceId filter by instance (optional)
     * @param emailType  filter by email type pattern (optional)
     * @param from       start of time range (optional, defaults to 30 days ago)
     * @param to         end of time range (optional, defaults to now)
     * @param page       page number (0-based, default 0)
     * @param size       page size (default 20)
     * @return paginated email history
     */
    @GetMapping("/history")
    @Operation(summary = "Get email send history", description = "Paginated email logs with optional instance/type/date filters")
    public ResponseEntity<Page<EmailHistoryResponse>> getHistory(
            @RequestParam(required = false) UUID instanceId,
            @RequestParam(required = false) String emailType,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        log.info("Admin requesting email history: instanceId={}, emailType={}, from={}, to={}, page={}, size={}",
            instanceId, emailType, from, to, page, size);

        Page<EmailHistoryResponse> history = emailAdminService.getEmailHistory(
            instanceId, emailType, from, to, page, size);
        return ResponseEntity.ok(history);
    }

    /**
     * Get aggregate email statistics (sent counts, failures, by type).
     *
     * @return email statistics for today and this week
     */
    @GetMapping("/stats")
    @Operation(summary = "Get email statistics", description = "Aggregate counts: sent today, this week, failures, by type")
    public ResponseEntity<EmailStatsResponse> getStats() {
        log.info("Admin requesting email stats");
        EmailStatsResponse stats = emailAdminService.getEmailStats();
        return ResponseEntity.ok(stats);
    }

    /**
     * Get current email configuration (queue mode + type toggles).
     *
     * @return email config state
     */
    @GetMapping("/config")
    @Operation(summary = "Get email config", description = "Current email toggle states and queue mode")
    public ResponseEntity<EmailConfigResponse> getConfig() {
        log.info("Admin requesting email config");
        EmailConfigResponse config = emailAdminService.getEmailConfig();
        return ResponseEntity.ok(config);
    }

    /**
     * Update email type toggles (enable/disable specific email types).
     * Updates in-memory only; to persist, update application.yml.
     *
     * @param toggles map of email type to enabled/disabled boolean
     * @return updated email config
     */
    @PutMapping("/config")
    @Operation(summary = "Update email config", description = "Toggle email types on/off at runtime")
    public ResponseEntity<EmailConfigResponse> updateConfig(
            @RequestBody Map<String, Boolean> toggles) {
        log.info("Admin updating email config: {}", toggles);
        EmailConfigResponse config = emailAdminService.updateEmailConfig(toggles);
        return ResponseEntity.ok(config);
    }

    /**
     * Manually trigger a specific email for an instance.
     * Bypasses the daily idempotency check only if the email wasn't already sent today.
     *
     * @param request trigger request with instanceId and emailType
     * @return 200 OK on success
     */
    @PostMapping("/trigger")
    @Operation(summary = "Trigger email manually", description = "Admin-triggered email send for a specific instance and type")
    public ResponseEntity<Void> triggerEmail(@Valid @RequestBody TriggerEmailRequest request) {
        log.info("Admin triggering email: instanceId={}, emailType={}",
            request.getInstanceId(), request.getEmailType());
        emailAdminService.triggerEmail(request.getInstanceId(), request.getEmailType());
        return ResponseEntity.ok().build();
    }
}
