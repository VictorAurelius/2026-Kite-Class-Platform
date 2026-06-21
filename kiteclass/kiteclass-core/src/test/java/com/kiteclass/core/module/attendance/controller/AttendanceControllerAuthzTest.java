package com.kiteclass.core.module.attendance.controller;

import com.kiteclass.core.common.security.AuthorizationBean;
import com.kiteclass.core.module.attendance.service.AttendanceService;
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
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.security.test.context.support.WithAnonymousUser;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Web-slice authorization tests for {@link AttendanceController} (GAP-1527, OWASP A01).
 *
 * <p>Before GAP-1527 three endpoints had NO {@code @PreAuthorize}: GET {@code /{id}},
 * GET {@code /enrollment/{enrollmentId}}, DELETE {@code /{id}} — so an unauthenticated /
 * STUDENT / PARENT caller could read or delete attendance records. The fix guards all
 * three with {@code hasAnyRole('TEACHER','STAFF','OWNER','ADMIN')}.
 */
@WebMvcTest(AttendanceController.class)
@AutoConfigureMockMvc
@Import({AttendanceControllerAuthzTest.TestSecurityConfig.class,
        AttendanceControllerAuthzTest.MockConfig.class})
@ActiveProfiles("test")
@DisplayName("AttendanceController @PreAuthorize role gate (GAP-1527, OWASP A01)")
class AttendanceControllerAuthzTest {

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
        @Bean
        @Primary
        AttendanceService attendanceService() {
            return Mockito.mock(AttendanceService.class);
        }
    }

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private AttendanceService attendanceService;

    /** Bean-override mock for {@code @authz.*} SpEL on the sibling write/read endpoints. */
    @MockitoBean(name = "authz")
    private AuthorizationBean authz;

    private static final Long ID = 42L;

    @BeforeEach
    void resetMocks() {
        Mockito.reset(attendanceService, authz);
    }

    @Test
    @DisplayName("TEACHER → 200 on GET /{id} (read tier)")
    @WithMockUser(roles = "TEACHER")
    void getById_teacher_allowed() throws Exception {
        mockMvc.perform(get("/api/v1/attendance/{id}", ID))
                .andExpect(status().isOk());
        verify(attendanceService).getAttendanceById(ID);
    }

    @Test
    @DisplayName("OWASP A01: STUDENT → denied GET /{id} (service NOT invoked)")
    @WithMockUser(roles = "STUDENT")
    void getById_student_denied() throws Exception {
        mockMvc.perform(get("/api/v1/attendance/{id}", ID))
                .andExpect(result -> assertDenied(result.getResponse().getStatus(), "STUDENT getById"));
        verifyNoInteractions(attendanceService);
    }

    @Test
    @DisplayName("OWASP A01: anonymous → denied GET /enrollment/{id} (service NOT invoked)")
    @WithAnonymousUser
    void getByEnrollment_anonymous_denied() throws Exception {
        mockMvc.perform(get("/api/v1/attendance/enrollment/{id}", 7L))
                .andExpect(result -> assertDenied(result.getResponse().getStatus(), "anonymous getByEnrollment"));
        verifyNoInteractions(attendanceService);
    }

    @Test
    @DisplayName("OWASP A01: PARENT → denied DELETE /{id} (service NOT invoked)")
    @WithMockUser(roles = "PARENT")
    void delete_parent_denied() throws Exception {
        mockMvc.perform(delete("/api/v1/attendance/{id}", ID))
                .andExpect(result -> assertDenied(result.getResponse().getStatus(), "PARENT delete"));
        verifyNoInteractions(attendanceService);
    }

    @Test
    @DisplayName("STAFF → 204 on DELETE /{id} (admin tier, service invoked)")
    @WithMockUser(roles = "STAFF")
    void delete_staff_allowed() throws Exception {
        mockMvc.perform(delete("/api/v1/attendance/{id}", ID))
                .andExpect(status().isNoContent());
        verify(attendanceService).deleteAttendance(ID);
    }

    private static void assertDenied(int statusCode, String label) {
        if (statusCode >= 200 && statusCode < 300) {
            throw new AssertionError(label + " must be denied by @PreAuthorize, got " + statusCode);
        }
    }
}
