package com.kiteclass.core.module.parent.controller;

import com.kiteclass.core.common.exception.BusinessException;
import com.kiteclass.core.module.parent.dto.ParentNotificationFacetResponse;
import com.kiteclass.core.module.parent.service.ParentNotificationsFacetService;
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
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ParentNotificationsFacetController.class)
@AutoConfigureMockMvc
@Import({ParentNotificationsFacetControllerIT.TestSecurityConfig.class,
        ParentNotificationsFacetControllerIT.MockConfig.class})
@ActiveProfiles("test")
@DisplayName("ParentNotificationsFacetController IT")
class ParentNotificationsFacetControllerIT {

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
        ParentNotificationsFacetService service() {
            return Mockito.mock(ParentNotificationsFacetService.class);
        }
    }

    @Autowired private MockMvc mockMvc;
    @Autowired private ParentNotificationsFacetService service;

    private static final Long PARENT_ID = 10L;
    private static final Long CHILD_ID = 100L;

    @Test
    @DisplayName("GET /api/v1/parent/children/{id}/notifications — 200 with linked parent (empty page v1 stub)")
    void linked_returns200() throws Exception {
        when(service.getNotificationsForChild(eq(PARENT_ID), eq(CHILD_ID), any(), any(), any()))
                .thenReturn(new PageImpl<ParentNotificationFacetResponse>(List.of(), Pageable.unpaged(), 0));

        mockMvc.perform(get("/api/v1/parent/children/{childId}/notifications", CHILD_ID)
                        .param("from", "2026-04-01")
                        .param("to", "2026-04-30")
                        .header("X-User-Reference-Id", PARENT_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.content").isArray())
                .andExpect(jsonPath("$.data.content.length()").value(0));
    }

    @Test
    @DisplayName("returns 403 when ParentStudentLink missing")
    void unlinkedParent_returns403() throws Exception {
        when(service.getNotificationsForChild(eq(PARENT_ID), eq(CHILD_ID), any(), any(), any()))
                .thenThrow(new BusinessException("PARENT_FACET_FORBIDDEN", HttpStatus.FORBIDDEN));

        mockMvc.perform(get("/api/v1/parent/children/{childId}/notifications", CHILD_ID)
                        .param("from", "2026-04-01")
                        .param("to", "2026-04-30")
                        .header("X-User-Reference-Id", PARENT_ID))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("returns 401 when X-User-Reference-Id header is missing")
    void missingParentHeader_returns401() throws Exception {
        mockMvc.perform(get("/api/v1/parent/children/{childId}/notifications", CHILD_ID)
                        .param("from", "2026-04-01")
                        .param("to", "2026-04-30"))
                .andExpect(status().isUnauthorized());
    }
}
