package com.kitehub.admin.controller;

import com.kitehub.admin.dto.AuditLogSummary;
import com.kitehub.subscription.audit.AdminAuditLog;
import com.kitehub.subscription.audit.AdminAuditLogRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Admin audit-log v1 REST API — exposes the privileged-action audit trail at the canonical
 * {@code /api/v1/admin/audit-logs} path for PLATFORM_ADMIN review (GAP-774).
 *
 * <p>Closes the Mảng D4 blocker (Wave 106 RST): the {@code admin_audit_log} table is populated
 * (V36 baseline + V54 enrichment in kitehub-subscription, written by {@code AdminAuditAspect})
 * but had no read endpoint — admins could not view the audit trail. This controller provides
 * a read-only, filtered, paginated viewer over that data.</p>
 *
 * <p>Read-only by design: audit logs are immutable (V50 RLS append-only enforcement). No
 * mutation endpoints. Mirrors the thin read-stub pattern of {@link AdminInstancesController}
 * and {@link AdminPaymentsController} — injects the subscription-layer repository directly
 * (monolith classpath) and maps to a DTO for response-shape stability.</p>
 *
 * @since 1.0 (GAP-774)
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/admin/audit-logs")
@RequiredArgsConstructor
@PreAuthorize("hasRole('PLATFORM_ADMIN')")
@Tag(name = "Admin v1 - Audit Logs", description = "Admin privileged-action audit trail viewer (GAP-774 — Wave 106 D4 fix)")
public class AdminAuditLogController {

    private final AdminAuditLogRepository auditLogRepository;

    /**
     * Default page size for the list endpoint (mirrors {@link AdminInstancesController#DEFAULT_PAGE_SIZE}).
     */
    static final int DEFAULT_PAGE_SIZE = 20;

    /**
     * Hard cap for page size to prevent unbounded scans (mirrors {@link AdminInstancesController#MAX_PAGE_SIZE}).
     */
    static final int MAX_PAGE_SIZE = 100;

    /**
     * Get a paginated, filtered list of admin audit-log entries (newest first).
     *
     * @param action      optional exact action filter (e.g. {@code BETA_REQUEST_APPROVE})
     * @param adminUserId optional filter by admin who performed the action
     * @param from        optional inclusive lower bound on {@code createdAt} (ISO date-time)
     * @param to          optional inclusive upper bound on {@code createdAt} (ISO date-time)
     * @param pageable    pagination (default size 20, max 100; sort fixed to createdAt DESC)
     * @return page of audit-log summaries
     */
    @GetMapping
    @Operation(summary = "List audit logs", description = "Paginated, filterable admin audit trail (newest first)")
    public ResponseEntity<Page<AuditLogSummary>> listAuditLogs(
            @RequestParam(required = false) String action,
            @RequestParam(required = false) UUID adminUserId,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to,
            @PageableDefault(size = DEFAULT_PAGE_SIZE) Pageable pageable) {

        Pageable safe = clampPageable(pageable);
        log.info("Admin v1 list audit-logs action={} adminUserId={} from={} to={} page={} size={}",
                action, adminUserId, from, to, safe.getPageNumber(), safe.getPageSize());

        Page<AdminAuditLog> page = auditLogRepository.search(action, adminUserId, from, to, safe);
        Page<AuditLogSummary> summaries = page.map(this::convertToSummary);

        return ResponseEntity.ok(summaries);
    }

    /**
     * Get a single audit-log entry by id.
     *
     * @param id audit-log row id
     * @return audit-log summary
     */
    @GetMapping("/{id}")
    @Operation(summary = "Get audit log detail", description = "Get a single audit-log entry by id")
    public ResponseEntity<AuditLogSummary> getAuditLog(@PathVariable Long id) {
        log.info("Admin v1 get audit-log detail: {}", id);

        AdminAuditLog entry = auditLogRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Audit log not found: " + id));

        return ResponseEntity.ok(convertToSummary(entry));
    }

    /**
     * Enforce page size ceiling — same pattern as {@link AdminInstancesController#clampPageable}.
     */
    static Pageable clampPageable(Pageable pageable) {
        if (pageable == null) {
            return PageRequest.of(0, DEFAULT_PAGE_SIZE);
        }
        if (pageable.getPageSize() > MAX_PAGE_SIZE) {
            return PageRequest.of(pageable.getPageNumber(), MAX_PAGE_SIZE, pageable.getSort());
        }
        return pageable;
    }

    /**
     * Map {@link AdminAuditLog} entity to {@link AuditLogSummary} DTO.
     */
    private AuditLogSummary convertToSummary(AdminAuditLog entry) {
        return AuditLogSummary.builder()
                .id(entry.getId())
                .adminUserId(entry.getAdminUserId())
                .action(entry.getAction())
                .targetEntityType(entry.getTargetEntityType())
                .targetEntityId(entry.getTargetEntityId())
                .targetResourceType(entry.getTargetResourceType())
                .targetResourceId(entry.getTargetResourceId())
                .requestIp(entry.getRequestIp())
                .userAgent(entry.getUserAgent())
                .requestId(entry.getRequestId())
                .payloadJson(entry.getPayloadJson())
                .beforeState(entry.getBeforeState())
                .afterState(entry.getAfterState())
                .success(entry.isSuccess())
                .errorMessage(entry.getErrorMessage())
                .createdAt(entry.getCreatedAt())
                .build();
    }
}
