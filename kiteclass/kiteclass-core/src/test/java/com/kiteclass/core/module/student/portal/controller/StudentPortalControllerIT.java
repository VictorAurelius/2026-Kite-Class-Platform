package com.kiteclass.core.module.student.portal.controller;

import com.kiteclass.core.module.student.portal.dto.StudentGradeDetailResponse;
import com.kiteclass.core.module.student.portal.dto.StudentNotificationFeedResponse;
import com.kiteclass.core.module.student.portal.dto.StudentTodayResponse;
import com.kiteclass.core.module.student.portal.service.StudentPortalService;
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
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.util.Collections;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Web slice tests covering the 5 student-portal read endpoints (GAP-269b).
 *
 * <p>Verifies (1) auth guard rejects requests without
 * {@code X-User-Reference-Id} with HTTP 401 and (2) happy-path returns the
 * stable empty payload Phase 1 v1 ships.
 */
@WebMvcTest(StudentPortalController.class)
@AutoConfigureMockMvc
@Import({StudentPortalControllerIT.TestSecurityConfig.class,
        StudentPortalControllerIT.MockConfig.class})
@ActiveProfiles("test")
@DisplayName("StudentPortalController IT")
class StudentPortalControllerIT {

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
        @Bean @Primary
        StudentPortalService service() {
            return Mockito.mock(StudentPortalService.class);
        }
    }

    @Autowired private MockMvc mockMvc;
    @Autowired private StudentPortalService service;

    private static final Long STUDENT_ID = 42L;

    @Test
    @DisplayName("GET /me/today — 200 with auth header (empty payload v1)")
    void today_authenticated_returns200() throws Exception {
        when(service.getToday(eq(STUDENT_ID))).thenReturn(
                StudentTodayResponse.builder()
                        .date(LocalDate.now())
                        .schedulePeriods(Collections.emptyList())
                        .assignmentsDueToday(Collections.emptyList())
                        .build());

        mockMvc.perform(get("/api/v1/students/me/today")
                        .header("X-User-Reference-Id", STUDENT_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.schedulePeriods").isArray())
                .andExpect(jsonPath("$.data.schedulePeriods.length()").value(0))
                .andExpect(jsonPath("$.data.assignmentsDueToday").isArray());
    }

    @Test
    @DisplayName("GET /me/today — 401 when X-User-Reference-Id is missing")
    void today_missingHeader_returns401() throws Exception {
        mockMvc.perform(get("/api/v1/students/me/today"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("GET /me/grades — 200 returns empty list v1")
    void gradesOverview_returns200() throws Exception {
        when(service.getGradesOverview(eq(STUDENT_ID))).thenReturn(Collections.emptyList());

        mockMvc.perform(get("/api/v1/students/me/grades")
                        .header("X-User-Reference-Id", STUDENT_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data.length()").value(0));
    }

    @Test
    @DisplayName("GET /me/grades/{subjectId} — 200 with subjectId echo")
    void subjectDetail_returns200() throws Exception {
        when(service.getSubjectDetail(eq(STUDENT_ID), eq(7L))).thenReturn(
                StudentGradeDetailResponse.builder()
                        .subjectId(7L)
                        .entries(Collections.emptyList())
                        .build());

        mockMvc.perform(get("/api/v1/students/me/grades/{subjectId}", 7L)
                        .header("X-User-Reference-Id", STUDENT_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.subjectId").value(7))
                .andExpect(jsonPath("$.data.entries").isArray());
    }

    @Test
    @DisplayName("GET /me/payments — 200 returns empty list v1")
    void payments_returns200() throws Exception {
        when(service.getPayments(eq(STUDENT_ID))).thenReturn(Collections.emptyList());

        mockMvc.perform(get("/api/v1/students/me/payments")
                        .header("X-User-Reference-Id", STUDENT_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data.length()").value(0));
    }

    @Test
    @DisplayName("GET /me/notifications — 200 with cursor & limit, empty feed v1")
    void notifications_returns200() throws Exception {
        when(service.getNotifications(eq(STUDENT_ID), any(), any())).thenReturn(
                StudentNotificationFeedResponse.builder()
                        .items(Collections.emptyList())
                        .nextCursor(null)
                        .build());

        mockMvc.perform(get("/api/v1/students/me/notifications")
                        .param("limit", "20")
                        .header("X-User-Reference-Id", STUDENT_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items").isArray())
                .andExpect(jsonPath("$.data.items.length()").value(0));
    }

    @Test
    @DisplayName("GET /me/notifications — 401 when auth header missing")
    void notifications_missingHeader_returns401() throws Exception {
        mockMvc.perform(get("/api/v1/students/me/notifications"))
                .andExpect(status().isUnauthorized());
    }
}
