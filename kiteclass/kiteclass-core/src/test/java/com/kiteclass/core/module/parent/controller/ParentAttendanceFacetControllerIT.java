package com.kiteclass.core.module.parent.controller;

import com.kiteclass.core.common.constant.AttendanceStatus;
import com.kiteclass.core.common.exception.BusinessException;
import com.kiteclass.core.module.attendance.dto.AttendancePeriodResponse;
import com.kiteclass.core.module.parent.service.ParentAttendanceFacetService;
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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Web-slice IT for {@link ParentAttendanceFacetController}.
 *
 * <p>Asserts the controller honours the documented contract:
 * <ul>
 *   <li>200 happy path with linked parent + valid range.</li>
 *   <li>403 when {@code ParentStudentLink} missing (service throws
 *       {@code PARENT_FACET_FORBIDDEN}).</li>
 * </ul>
 *
 * @since 2.18.1 (Wave 18b2 — GAP-321b Phase 1B foundation)
 */
@WebMvcTest(ParentAttendanceFacetController.class)
@AutoConfigureMockMvc
@Import({ParentAttendanceFacetControllerIT.TestSecurityConfig.class,
        ParentAttendanceFacetControllerIT.MockConfig.class})
@ActiveProfiles("test")
@DisplayName("ParentAttendanceFacetController IT")
class ParentAttendanceFacetControllerIT {

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
        ParentAttendanceFacetService service() {
            return Mockito.mock(ParentAttendanceFacetService.class);
        }
    }

    @Autowired private MockMvc mockMvc;
    @Autowired private ParentAttendanceFacetService service;

    private static final Long PARENT_ID = 10L;
    private static final Long CHILD_ID = 100L;

    @Test
    @DisplayName("GET /api/v1/parent/children/{id}/attendance — 200 with linked parent")
    void linked_returns200() throws Exception {
        AttendancePeriodResponse a = AttendancePeriodResponse.builder()
                .id(1L).studentId(CHILD_ID).classId(7L).subjectSectionId(11L)
                .periodNo(1).date(LocalDate.parse("2026-04-15"))
                .status(AttendanceStatus.PRESENT).recordedBy(99L)
                .build();
        Page<AttendancePeriodResponse> page = new PageImpl<>(List.of(a), Pageable.unpaged(), 1);
        when(service.getAttendanceForChild(eq(PARENT_ID), eq(CHILD_ID), any(), any(), any()))
                .thenReturn(page);

        mockMvc.perform(get("/api/v1/parent/children/{childId}/attendance", CHILD_ID)
                        .param("from", "2026-04-01")
                        .param("to", "2026-04-30")
                        .header("X-User-Reference-Id", PARENT_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.content[0].studentId").value(CHILD_ID))
                .andExpect(jsonPath("$.data.content[0].periodNo").value(1));
    }

    @Test
    @DisplayName("returns 403 when ParentStudentLink missing for the (parent, child) pair")
    void unlinkedParent_returns403() throws Exception {
        when(service.getAttendanceForChild(eq(PARENT_ID), eq(CHILD_ID), any(), any(), any()))
                .thenThrow(new BusinessException("PARENT_FACET_FORBIDDEN", HttpStatus.FORBIDDEN));

        mockMvc.perform(get("/api/v1/parent/children/{childId}/attendance", CHILD_ID)
                        .param("from", "2026-04-01")
                        .param("to", "2026-04-30")
                        .header("X-User-Reference-Id", PARENT_ID))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("returns 401 when X-User-Reference-Id header is missing")
    void missingParentHeader_returns401() throws Exception {
        mockMvc.perform(get("/api/v1/parent/children/{childId}/attendance", CHILD_ID)
                        .param("from", "2026-04-01")
                        .param("to", "2026-04-30"))
                .andExpect(status().isUnauthorized());
    }
}
