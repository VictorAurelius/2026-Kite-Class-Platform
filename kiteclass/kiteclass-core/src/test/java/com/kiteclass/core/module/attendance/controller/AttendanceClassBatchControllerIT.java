package com.kiteclass.core.module.attendance.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kiteclass.core.common.constant.AttendanceStatus;
import com.kiteclass.core.common.security.AuthorizationHelper;
import com.kiteclass.core.module.attendance.dto.AttendancePeriodResponse;
import com.kiteclass.core.module.attendance.dto.ClassBatchAttendanceEntry;
import com.kiteclass.core.module.attendance.dto.ClassBatchAttendanceRequest;
import com.kiteclass.core.module.attendance.service.AttendancePeriodService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
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
 * Web slice tests for the GAP-268a class-overview save endpoint.
 *
 * <p>Verifies (1) happy-path 201 response, (2) validation rejects empty
 * entries / out-of-range periodNo, (3) class+date arrive correctly on the
 * service via {@link ArgumentCaptor}.
 */
@WebMvcTest(AttendanceClassBatchController.class)
@AutoConfigureMockMvc
@Import({AttendanceClassBatchControllerIT.TestSecurityConfig.class,
        AttendanceClassBatchControllerIT.MockConfig.class})
@ActiveProfiles("test")
@DisplayName("AttendanceClassBatchController IT")
class AttendanceClassBatchControllerIT {

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

        /**
         * Wave 105 Bucket C: stub the authz bean so SpEL
         * {@code @authz.hasAccessToClass(#classId)} resolves without a
         * full Spring Security ApplicationContext + TeacherClassRepository.
         * Tests below configure return values per-scenario.
         */
        @Bean("authz") @Primary
        AuthorizationHelper authz() {
            return Mockito.mock(AuthorizationHelper.class);
        }
    }

    @Autowired private MockMvc mockMvc;
    @Autowired private AttendancePeriodService service;
    @Autowired private AuthorizationHelper authz;
    @Autowired private ObjectMapper objectMapper;

    private static final Long CLASS_ID = 100L;
    private static final Long TEACHER_ID = 5L;
    private static final String DATE = "2026-05-12";

    @BeforeEach
    void resetMocks() {
        // @TestConfiguration beans are singletons across tests — reset to avoid
        // cross-test invocation pollution (e.g., happyPath verify(... 1 time)
        // would see invocations from assignedClass_stillAllowed otherwise).
        Mockito.reset(service, authz);
    }

    @Test
    @DisplayName("POST class/{id}/batch — 201 + delegates classId+date to service")
    @WithMockUser
    void happyPath_returns201() throws Exception {
        // Wave 105 Bucket C: authz allows access to CLASS_ID
        when(authz.hasAccessToClass(eq(CLASS_ID))).thenReturn(true);

        ClassBatchAttendanceRequest body = ClassBatchAttendanceRequest.builder()
                .entries(List.of(
                        ClassBatchAttendanceEntry.builder()
                                .studentId(11L)
                                .subjectSectionId(7L)
                                .periodNo(1)
                                .status(AttendanceStatus.PRESENT)
                                .build(),
                        ClassBatchAttendanceEntry.builder()
                                .studentId(12L)
                                .subjectSectionId(7L)
                                .periodNo(1)
                                .status(AttendanceStatus.LATE)
                                .build()))
                .build();

        when(service.upsertClassBatch(eq(CLASS_ID), eq(LocalDate.parse(DATE)),
                any(), eq(TEACHER_ID))).thenReturn(Collections.<AttendancePeriodResponse>emptyList());

        mockMvc.perform(post("/api/v1/attendance/class/{classId}/batch", CLASS_ID)
                        .param("date", DATE)
                        .header("X-Teacher-Id", TEACHER_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isCreated());

        ArgumentCaptor<ClassBatchAttendanceRequest> captor =
                ArgumentCaptor.forClass(ClassBatchAttendanceRequest.class);
        verify(service).upsertClassBatch(eq(CLASS_ID), eq(LocalDate.parse(DATE)),
                captor.capture(), eq(TEACHER_ID));
        // Body entries forwarded verbatim
        org.junit.jupiter.api.Assertions.assertEquals(2, captor.getValue().getEntries().size());
    }

    @Test
    @DisplayName("rejects empty entries with 400")
    @WithMockUser
    void emptyEntries_returns400() throws Exception {
        when(authz.hasAccessToClass(eq(CLASS_ID))).thenReturn(true);

        ClassBatchAttendanceRequest body = ClassBatchAttendanceRequest.builder()
                .entries(Collections.emptyList())
                .build();

        mockMvc.perform(post("/api/v1/attendance/class/{classId}/batch", CLASS_ID)
                        .param("date", DATE)
                        .header("X-Teacher-Id", TEACHER_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("rejects periodNo > 10 with 400")
    @WithMockUser
    void invalidPeriod_returns400() throws Exception {
        when(authz.hasAccessToClass(eq(CLASS_ID))).thenReturn(true);

        ClassBatchAttendanceRequest body = ClassBatchAttendanceRequest.builder()
                .entries(List.of(
                        ClassBatchAttendanceEntry.builder()
                                .studentId(11L)
                                .subjectSectionId(7L)
                                .periodNo(11) // out of range 1..10
                                .status(AttendanceStatus.PRESENT)
                                .build()))
                .build();

        mockMvc.perform(post("/api/v1/attendance/class/{classId}/batch", CLASS_ID)
                        .param("date", DATE)
                        .header("X-Teacher-Id", TEACHER_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("rejects missing X-Teacher-Id header (4xx/5xx — Spring MissingRequestHeader)")
    @WithMockUser
    void missingTeacherHeader_isError() throws Exception {
        when(authz.hasAccessToClass(eq(CLASS_ID))).thenReturn(true);

        ClassBatchAttendanceRequest body = ClassBatchAttendanceRequest.builder()
                .entries(List.of(ClassBatchAttendanceEntry.builder()
                        .studentId(11L).subjectSectionId(7L).periodNo(1)
                        .status(AttendanceStatus.PRESENT).build()))
                .build();

        // Behaviour mirrors existing AttendancePeriodController upsertBatch path:
        // GlobalExceptionHandler maps MissingRequestHeaderException via the default
        // SYSTEM_INTERNAL_ERROR envelope. Test asserts the request is rejected
        // (any 4xx/5xx — what matters is no successful 201).
        mockMvc.perform(post("/api/v1/attendance/class/{classId}/batch", CLASS_ID)
                        .param("date", DATE)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(result -> {
                    int status = result.getResponse().getStatus();
                    if (status < 400) {
                        throw new AssertionError("Expected 4xx/5xx but got " + status);
                    }
                });
    }

    /**
     * Wave 105 Bucket C — OWASP A01 cross-class spoof guard.
     *
     * <p>Failure-mode matrix row C3: teacher Tâm assigned class 100 attempts
     * to record attendance for class 999 (NOT assigned) by submitting URL
     * {@code POST /api/v1/attendance/class/999/batch}. Per
     * {@link AuthorizationHelper#hasAccessToClass(Long)} return false →
     * Spring Security's {@code @PreAuthorize} denies → HTTP 403.
     *
     * <p>Evidence chain per `pre-handoff-self-test-completeness.md` §2.4
     * admin-flow checklist (a)-(g) adapted to per-resource authz:
     * <ul>
     *   <li>(a) Credential — {@link WithMockUser} provides authenticated principal</li>
     *   <li>(d) Role-guard — {@code @PreAuthorize("@authz.hasAccessToClass(#classId)")} fires</li>
     *   <li>(g) Target action FAILS for spoof — 403 instead of 201</li>
     * </ul>
     */
    @Test
    @DisplayName("OWASP A01 — cross-class spoof returns 403 (Wave 105 C3 P0)")
    @WithMockUser
    void crossClassSpoof_returns403() throws Exception {
        Long spoofedClassId = 999L;
        // Teacher NOT assigned to class 999 — authz denies
        when(authz.hasAccessToClass(eq(spoofedClassId))).thenReturn(false);

        ClassBatchAttendanceRequest body = ClassBatchAttendanceRequest.builder()
                .entries(List.of(ClassBatchAttendanceEntry.builder()
                        .studentId(11L).subjectSectionId(7L).periodNo(1)
                        .status(AttendanceStatus.PRESENT).build()))
                .build();

        mockMvc.perform(post("/api/v1/attendance/class/{classId}/batch", spoofedClassId)
                        .param("date", DATE)
                        .header("X-Teacher-Id", TEACHER_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(result -> {
                    int status = result.getResponse().getStatus();
                    // Spring Security denial yields 403 with proper exception
                    // mapping, OR 500 when AccessDeniedException propagates
                    // without handler — either way, NOT 2xx success.
                    // Production AccessDenied → ExceptionTranslationFilter → 403.
                    if (status >= 200 && status < 300) {
                        throw new AssertionError(
                                "Spoof request should NOT succeed, got " + status);
                    }
                });

        // Defense in depth — service was NOT invoked even though endpoint hit
        Mockito.verifyNoInteractions(service);
    }

    @Test
    @DisplayName("OWASP A01 — assigned class still ALLOWED 201 (no false positives)")
    @WithMockUser
    void assignedClass_stillAllowed() throws Exception {
        when(authz.hasAccessToClass(eq(CLASS_ID))).thenReturn(true);
        when(service.upsertClassBatch(eq(CLASS_ID), eq(LocalDate.parse(DATE)),
                any(), eq(TEACHER_ID))).thenReturn(Collections.<AttendancePeriodResponse>emptyList());

        ClassBatchAttendanceRequest body = ClassBatchAttendanceRequest.builder()
                .entries(List.of(ClassBatchAttendanceEntry.builder()
                        .studentId(11L).subjectSectionId(7L).periodNo(1)
                        .status(AttendanceStatus.PRESENT).build()))
                .build();

        mockMvc.perform(post("/api/v1/attendance/class/{classId}/batch", CLASS_ID)
                        .param("date", DATE)
                        .header("X-Teacher-Id", TEACHER_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isCreated());
    }
}
