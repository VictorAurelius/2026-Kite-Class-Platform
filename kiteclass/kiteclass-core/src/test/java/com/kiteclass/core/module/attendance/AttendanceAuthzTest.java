package com.kiteclass.core.module.attendance;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kiteclass.core.common.constant.AttendanceStatus;
import com.kiteclass.core.common.context.UserContext;
import com.kiteclass.core.common.security.AuthorizationBean;
import com.kiteclass.core.module.attendance.controller.AttendancePeriodController;
import com.kiteclass.core.module.attendance.dto.AttendancePeriodBatchCreateRequest;
import com.kiteclass.core.module.attendance.dto.AttendancePeriodCreateRequest;
import com.kiteclass.core.module.attendance.dto.AttendancePeriodResponse;
import com.kiteclass.core.module.attendance.service.AttendancePeriodService;
import org.junit.jupiter.api.AfterEach;
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
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.util.Collections;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * GAP-1300 — Per-period attendance recording authz closure (BE authz, web slice).
 *
 * <p>Cross-flow sweep of GAP-1299: {@code AttendancePeriodController} read the client-supplied
 * {@code X-Teacher-Id} header as the recording-teacher identity ({@code recordedBy}). The gateway
 * does NOT control that header (GAP-814), so a caller could attribute records to an arbitrary
 * teacher, and any authenticated tenant user (incl. STUDENT/PARENT) could record at all.
 *
 * <p>This {@code @WebMvcTest} slice verifies the controller authz contract with real method
 * security + a mocked service:
 * <ol>
 *   <li><strong>Role gate</strong> — {@code @PreAuthorize("hasAnyRole('TEACHER','STAFF','OWNER','ADMIN')")}
 *       blocks STUDENT/PARENT (403); the service is never invoked.</li>
 *   <li><strong>Identity from token</strong> — {@code recordedBy} is derived from the authenticated
 *       principal ({@code X-User-Reference-Id} → {@code UserContext}), NOT the client
 *       {@code X-Teacher-Id} header. A spoofed {@code X-Teacher-Id} is ignored: the service is
 *       invoked with the token reference id, not the spoofed value.</li>
 * </ol>
 *
 * <p>The ADMIN/OWNER service-layer bypass (mark/update paths) uses {@code AuthorizationBean.isAdmin()}
 * and is covered by unit tests in {@code AttendanceServiceTest}. {@code recorded_by} is NOT NULL, so
 * the realistic per-period recorder is a TEACHER carrying a numeric reference id (no privilege
 * escalation — the spoof is closed).
 */
@WebMvcTest(AttendancePeriodController.class)
@AutoConfigureMockMvc
@Import({AttendanceAuthzTest.TestSecurityConfig.class, AttendanceAuthzTest.MockConfig.class})
@ActiveProfiles("test")
@DisplayName("GAP-1300 — AttendancePeriodController authz (role gate + X-Teacher-Id spoof closure)")
class AttendanceAuthzTest {

    @TestConfiguration
    @EnableMethodSecurity(prePostEnabled = true)
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
        @Bean @Primary
        AttendancePeriodService service() {
            return Mockito.mock(AttendancePeriodService.class);
        }
    }

    @Autowired private MockMvc mockMvc;
    @Autowired private AttendancePeriodService service;
    @Autowired private ObjectMapper objectMapper;

    /** Bean-override mock for the {@code @authz.hasAccessToClass} SpEL on sibling read endpoints. */
    @MockitoBean(name = "authz")
    private AuthorizationBean authz;

    private static final Long TEACHER_ID = 909L;
    private static final String DATE = "2026-09-05";

    @BeforeEach
    void seedContext() {
        Mockito.reset(service, authz);
        // GAP-1300: recording teacher derived from the authenticated principal
        // (X-User-Reference-Id → UserContext), NOT the dropped client X-Teacher-Id header. This
        // @WebMvcTest slice does not run the real TenantFilterInterceptor → seed the thread-local.
        UserContext.setCurrentReferenceId(TEACHER_ID);
    }

    @AfterEach
    void clearContext() {
        UserContext.clear();
    }

    private String batchBody() throws Exception {
        AttendancePeriodCreateRequest entry = AttendancePeriodCreateRequest.builder()
                .studentId(102L)
                .classId(202L)
                .subjectSectionId(303L)
                .periodNo(2)
                .date(LocalDate.parse(DATE))
                .status(AttendanceStatus.PRESENT)
                .build();
        return objectMapper.writeValueAsString(AttendancePeriodBatchCreateRequest.builder()
                .entries(List.of(entry))
                .build());
    }

    // ── Layer 1: role gate — STUDENT / PARENT blocked entirely ──────────────

    @Test
    @DisplayName("STUDENT cannot record attendance — 403; service NOT invoked")
    @WithMockUser(roles = "STUDENT")
    void student_cannotRecordAttendance() throws Exception {
        mockMvc.perform(post("/api/v1/attendance/periods")
                        .header("X-Tenant-Id", "00000000-0000-0000-0000-000000000001")
                        // spoofed header — must not matter
                        .header("X-Teacher-Id", "111")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(batchBody()))
                .andExpect(status().isForbidden());
        Mockito.verifyNoInteractions(service);
    }

    @Test
    @DisplayName("PARENT cannot record attendance — 403; service NOT invoked")
    @WithMockUser(roles = "PARENT")
    void parent_cannotRecordAttendance() throws Exception {
        mockMvc.perform(post("/api/v1/attendance/periods")
                        .header("X-Tenant-Id", "00000000-0000-0000-0000-000000000001")
                        .header("X-Teacher-Id", "111")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(batchBody()))
                .andExpect(status().isForbidden());
        Mockito.verifyNoInteractions(service);
    }

    // ── Layer 2: identity from token — X-Teacher-Id spoof ignored ───────────

    @Test
    @DisplayName("GAP-1300 — TEACHER records; spoofed X-Teacher-Id ignored, recordedBy = token id")
    @WithMockUser(roles = "TEACHER")
    void teacher_recordedBy_isTokenIdentity_notSpoofedHeader() throws Exception {
        when(service.upsertBatch(any(), eq(TEACHER_ID)))
                .thenReturn(Collections.<AttendancePeriodResponse>emptyList());

        // UserContext = TEACHER_ID (token). The request spoofs X-Teacher-Id = 111 (different
        // teacher). The controller no longer reads it → service must be invoked with TEACHER_ID.
        mockMvc.perform(post("/api/v1/attendance/periods")
                        .header("X-Tenant-Id", "00000000-0000-0000-0000-000000000001")
                        .header("X-Teacher-Id", "111")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(batchBody()))
                .andExpect(status().isCreated());

        verify(service).upsertBatch(any(), eq(TEACHER_ID));
        Mockito.verify(service, Mockito.never()).upsertBatch(any(), eq(111L));
    }
}
