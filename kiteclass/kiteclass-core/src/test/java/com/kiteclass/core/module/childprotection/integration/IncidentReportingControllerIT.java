package com.kiteclass.core.module.childprotection.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kiteclass.core.common.exception.GlobalExceptionHandler;
import com.kiteclass.core.module.childprotection.controller.IncidentReportingController;
import com.kiteclass.core.module.childprotection.dto.MandatoryReportAckRequest;
import com.kiteclass.core.module.childprotection.entity.ChildProtectionAuditLog;
import com.kiteclass.core.module.childprotection.entity.Incident;
import com.kiteclass.core.module.childprotection.enums.IncidentCategory;
import com.kiteclass.core.module.childprotection.enums.IncidentSeverity;
import com.kiteclass.core.module.childprotection.enums.IncidentStatus;
import com.kiteclass.core.module.childprotection.listener.IncidentTransitionListener;
import com.kiteclass.core.module.childprotection.service.ChildProtectionAuditService;
import com.kiteclass.core.module.childprotection.service.IncidentService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Web-slice integration test for {@link IncidentReportingController} —
 * verifies the mandatory-report ack endpoint + RBAC gating
 * (SAFEGUARDING_OFFICER only) per BR-CHILD-PROTECT-006.
 *
 * <p>Hash-chain semantics + service correctness are exhaustively covered
 * in {@code ChildProtectionAuditServiceImplTest}; this IT focuses on the
 * HTTP boundary.
 *
 * @since Wave 19 Bucket A — GAP-322c Phase 1C v1
 */
@WebMvcTest(IncidentReportingController.class)
@AutoConfigureMockMvc
@Import({
        IncidentReportingControllerIT.TestSecurityConfig.class,
        IncidentReportingControllerIT.MockConfig.class,
        GlobalExceptionHandler.class
})
@ActiveProfiles("test")
@DisplayName("IncidentReporting Integration — Phase 1C v1")
class IncidentReportingControllerIT {

    @TestConfiguration
    static class TestSecurityConfig {
        @Bean
        SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
            http.csrf(AbstractHttpConfigurer::disable)
                    .authorizeHttpRequests(auth -> auth.anyRequest().permitAll());
            return http.build();
        }
    }

    @TestConfiguration
    static class MockConfig {
        @Bean
        @Primary
        IncidentService incidentService() {
            return Mockito.mock(IncidentService.class);
        }

        @Bean
        @Primary
        ChildProtectionAuditService childProtectionAuditService() {
            return Mockito.mock(ChildProtectionAuditService.class);
        }
    }

    @Autowired private MockMvc mockMvc;
    @Autowired private IncidentService incidentService;
    @Autowired private ChildProtectionAuditService auditService;
    @Autowired private ObjectMapper objectMapper;

    private static final String OFFICER_ROLE = "SAFEGUARDING_OFFICER";
    private static final String TEACHER_ROLE = "TEACHER";

    @BeforeEach
    void resetMocks() {
        // @WebMvcTest reuses mock-bean across tests; reset interactions so
        // verify(..., never()) sees only the current test (per
        // feedback_webmvctest_mock_reset.md).
        Mockito.reset(incidentService, auditService);
    }

    @Test
    @DisplayName("POST /api/v1/incidents/{id}/mandatory-report-ack — SAFEGUARDING_OFFICER → 201")
    void ack_authorised_returns201() throws Exception {
        Incident incident = sampleCriticalIncident(7L);
        ChildProtectionAuditLog entry = sampleAuditEntry(99L, "abcdef".repeat(10) + "abcd");

        when(incidentService.findById(eq(7L))).thenReturn(incident);
        when(auditService.append(
                eq(IncidentTransitionListener.INCIDENT_ENTITY_TYPE),
                eq(7L),
                eq(IncidentReportingController.ACTION_MANDATORY_REPORT_ACK),
                eq(50L),
                anyMap()))
                .thenReturn(entry);

        MandatoryReportAckRequest req = new MandatoryReportAckRequest(
                "TĐ111-2026-0001",
                Instant.parse("2026-05-05T10:00:00Z"),
                "Đã liên hệ công an phường");

        mockMvc.perform(post("/api/v1/incidents/{id}/mandatory-report-ack", 7L)
                        .header("X-User-Roles", OFFICER_ROLE)
                        .header("X-User-Reference-Id", 50L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.incidentId").value(7))
                .andExpect(jsonPath("$.data.referenceNumber").value("TĐ111-2026-0001"))
                .andExpect(jsonPath("$.data.auditLogId").value(99));

        verify(auditService).append(
                eq(IncidentTransitionListener.INCIDENT_ENTITY_TYPE),
                eq(7L),
                eq(IncidentReportingController.ACTION_MANDATORY_REPORT_ACK),
                eq(50L),
                anyMap());
    }

    @Test
    @DisplayName("POST .../mandatory-report-ack — TEACHER role → 403, services never called")
    void ack_teacher_returns403() throws Exception {
        MandatoryReportAckRequest req = new MandatoryReportAckRequest(
                "TĐ111-X", Instant.now(), null);

        mockMvc.perform(post("/api/v1/incidents/{id}/mandatory-report-ack", 7L)
                        .header("X-User-Roles", TEACHER_ROLE)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isForbidden());

        verify(incidentService, never()).findById(any());
        verify(auditService, never()).append(any(), any(), any(), any(), any());
    }

    @Test
    @DisplayName("POST .../mandatory-report-ack — missing X-User-Roles → 403")
    void ack_missingRoles_returns403() throws Exception {
        MandatoryReportAckRequest req = new MandatoryReportAckRequest(
                "TĐ111-X", Instant.now(), null);

        mockMvc.perform(post("/api/v1/incidents/{id}/mandatory-report-ack", 7L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isForbidden());

        verify(auditService, never()).append(any(), any(), any(), any(), any());
    }

    @Test
    @DisplayName("POST .../mandatory-report-ack — missing referenceNumber → 400 (validation)")
    void ack_missingReference_returns400() throws Exception {
        // The request validation @NotBlank handles reference-number.
        MandatoryReportAckRequest req = new MandatoryReportAckRequest(
                "  ",
                Instant.now(),
                null);

        mockMvc.perform(post("/api/v1/incidents/{id}/mandatory-report-ack", 7L)
                        .header("X-User-Roles", OFFICER_ROLE)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest());

        verify(auditService, never()).append(any(), any(), any(), any(), any());
    }

    @Test
    @DisplayName("RBAC: multi-role header containing SAFEGUARDING_OFFICER → 201")
    void ack_multiRole_includesOfficer_allowed() throws Exception {
        Incident incident = sampleCriticalIncident(8L);
        when(incidentService.findById(eq(8L))).thenReturn(incident);
        when(auditService.append(any(), any(), any(), any(), anyMap()))
                .thenReturn(sampleAuditEntry(100L, "f".repeat(64)));

        MandatoryReportAckRequest req = new MandatoryReportAckRequest(
                "TĐ111-2026-0002",
                Instant.parse("2026-05-05T10:00:00Z"),
                null);

        mockMvc.perform(post("/api/v1/incidents/{id}/mandatory-report-ack", 8L)
                        .header("X-User-Roles", "TEACHER, SAFEGUARDING_OFFICER, COUNSELOR")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated());
    }

    private static Incident sampleCriticalIncident(Long id) {
        Incident i = Incident.builder()
                .title("Sự cố nghi xâm hại")
                .severity(IncidentSeverity.CRITICAL)
                .category(IncidentCategory.ABUSE)
                .status(IncidentStatus.INVESTIGATING)
                .reporterUserId(10L)
                .build();
        i.setId(id);
        return i;
    }

    private static ChildProtectionAuditLog sampleAuditEntry(Long id, String contentHash) {
        ChildProtectionAuditLog e = ChildProtectionAuditLog.builder()
                .entityType("Incident")
                .entityId(7L)
                .action(IncidentReportingController.ACTION_MANDATORY_REPORT_ACK)
                .actorId(50L)
                .occurredAt(Instant.now())
                .prevHash("0".repeat(64))
                .contentHash(contentHash)
                .payloadJson("{}")
                .build();
        e.setId(id);
        return e;
    }
}
