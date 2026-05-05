package com.kiteclass.core.module.childprotection.controller;

import com.kiteclass.core.common.dto.ApiResponse;
import com.kiteclass.core.common.exception.BusinessException;
import com.kiteclass.core.module.childprotection.dto.MandatoryReportAckRequest;
import com.kiteclass.core.module.childprotection.dto.MandatoryReportAckResponse;
import com.kiteclass.core.module.childprotection.entity.ChildProtectionAuditLog;
import com.kiteclass.core.module.childprotection.entity.Incident;
import com.kiteclass.core.module.childprotection.listener.IncidentTransitionListener;
import com.kiteclass.core.module.childprotection.service.ChildProtectionAuditService;
import com.kiteclass.core.module.childprotection.service.IncidentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * IncidentReportingController — endpoints for the Đ.51 mandatory-reporting
 * workflow (GAP-322c Phase 1C v1, BR-CHILD-PROTECT-006).
 *
 * <p>Phase 1C v1 ships a single endpoint:
 * {@code POST /api/v1/incidents/{id}/mandatory-report-ack} — the
 * safeguarding officer submits the external reference number + timestamp
 * after they have actually reported the case to Tổng đài 111 + công an
 * địa phương. The endpoint persists a hash-chain audit log entry; the
 * banner state on the FE side then drops the "must report" cue.
 *
 * <p>RBAC mirrors {@code VettingController}: {@code SAFEGUARDING_OFFICER}
 * role on the {@code X-User-Roles} header (forwarded by the Gateway). Anyone
 * else receives 403.
 *
 * <p>Out-of-scope for v1 (deferred to Phase 1C remainder follow-up gap):
 * <ul>
 *   <li>Listing reported acks per incident (read-side timeline)</li>
 *   <li>Tổng đài 111 webhook delivery (Stage 2 Q4 2026)</li>
 *   <li>Full UC-INCIDENT-CRITICAL-REPORT page UI</li>
 * </ul>
 *
 * @since Wave 19 Bucket A — GAP-322c Phase 1C v1
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/incidents")
@RequiredArgsConstructor
@Tag(name = "IncidentReporting",
        description = "Mandatory-reporting acknowledgements (GAP-322c Phase 1C v1)")
public class IncidentReportingController {

    /** Role name authorised to ack mandatory reports. */
    static final String SAFEGUARDING_OFFICER = "SAFEGUARDING_OFFICER";

    /** Audit-log action recorded on a mandatory-report ack. */
    public static final String ACTION_MANDATORY_REPORT_ACK = "MANDATORY_REPORT_ACK";

    private final IncidentService incidentService;
    private final ChildProtectionAuditService auditService;

    @PostMapping("/{id}/mandatory-report-ack")
    @Operation(
            summary = "Acknowledge that a mandatory report (Đ.51) has been filed",
            description = "RBAC: SAFEGUARDING_OFFICER only. Persists a hash-chain audit log entry."
    )
    public ResponseEntity<ApiResponse<MandatoryReportAckResponse>> ackMandatoryReport(
            @PathVariable Long id,
            @Valid @RequestBody MandatoryReportAckRequest req,
            @RequestHeader(value = "X-User-Roles", required = false) String roles,
            @RequestHeader(value = "X-User-Reference-Id", required = false) Long actorUserId) {

        requireSafeguardingOfficer(roles);

        // Verify incident exists (404 surfaces). We don't expose any
        // sensitive narrative through this endpoint — only the id +
        // surface category/severity for the audit payload.
        Incident incident = incidentService.findById(id);

        // Build canonical payload for the chain (TreeMap inside service
        // re-orders so insertion sequence here is irrelevant for hash
        // determinism — but we keep a stable map type for log clarity).
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("referenceNumber", req.referenceNumber());
        payload.put("reportedAt", req.reportedAt().toString());
        payload.put("severity", incident.getSeverity().name());
        payload.put("category", incident.getCategory().name());
        if (req.notes() != null && !req.notes().isBlank()) {
            payload.put("notes", req.notes());
        }

        ChildProtectionAuditLog entry = auditService.append(
                IncidentTransitionListener.INCIDENT_ENTITY_TYPE,
                incident.getId(),
                ACTION_MANDATORY_REPORT_ACK,
                actorUserId,
                payload);

        log.info("Mandatory-report ack persisted incidentId={} ref={} auditId={}",
                incident.getId(), req.referenceNumber(), entry.getId());

        MandatoryReportAckResponse resp = new MandatoryReportAckResponse(
                incident.getId(),
                req.referenceNumber(),
                req.reportedAt(),
                entry.getId(),
                entry.getContentHash());

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(resp));
    }

    /**
     * RBAC gate — duplicate of {@code VettingController.requireSafeguardingOfficer}
     * intentionally kept local to avoid a cross-controller helper class for
     * a single check. Unified into a {@code @PreAuthorize}/Spring Security
     * gate when role-based wiring lands across all child-protection endpoints
     * (Phase 1C remainder).
     */
    static void requireSafeguardingOfficer(String rolesHeader) {
        if (rolesHeader == null || rolesHeader.isBlank()) {
            throw new BusinessException("INCIDENT_REPORT_RBAC_DENIED", HttpStatus.FORBIDDEN);
        }
        List<String> roles = Arrays.stream(rolesHeader.split("[,\\s]+"))
                .map(s -> s.trim().toUpperCase(Locale.ROOT))
                .toList();
        if (!roles.contains(SAFEGUARDING_OFFICER)) {
            throw new BusinessException("INCIDENT_REPORT_RBAC_DENIED", HttpStatus.FORBIDDEN);
        }
    }
}
